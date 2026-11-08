package com.kuiba.mai;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.server.level.ServerLevel;

/**
 * Server-side frame-by-frame particle player. Vanilla particles are fire-and-forget, so anything
 * animated (a sweeping arc, a closing shell, a rising wall) is cut into per-tick frames here.
 * Everything is emitted through {@code ServerLevel#sendParticles}, so every nearby player sees
 * the same staging. Entries live at most a handful of ticks and die with the server.
 */
public final class MaiFxSequencer {
    /** Plays one frame of an effect. {@code frame} is 0-based and counts up to {@code frames - 1}. */
    public interface Emitter {
        void tick(int frame);
    }

    private static final class Entry {
        final ServerLevel level;
        final Emitter emitter;
        final int frames;
        int frame;

        Entry(ServerLevel level, Emitter emitter, int frames) {
            this.level = level;
            this.emitter = emitter;
            this.frames = frames;
        }
    }

    private static final List<Entry> RUNNING = new ArrayList<>();

    private MaiFxSequencer() {}

    /** Play {@code emitter} once per tick for {@code frames} ticks in {@code level}, starting next tick. */
    public static void schedule(ServerLevel level, int frames, Emitter emitter) {
        if (frames <= 0) {
            return;
        }
        RUNNING.add(new Entry(level, emitter, frames));
    }

    /** Advance every effect belonging to {@code level}. Called from the level tick event. */
    public static void tick(ServerLevel level) {
        if (RUNNING.isEmpty()) {
            return;
        }
        Iterator<Entry> it = RUNNING.iterator();
        while (it.hasNext()) {
            Entry entry = it.next();
            if (entry.level != level) {
                continue;
            }
            entry.emitter.tick(entry.frame);
            entry.frame++;
            if (entry.frame >= entry.frames) {
                it.remove();
            }
        }
    }

    /** Drop everything (server stopping). Levels are gone anyway; this just releases the list. */
    public static void clear() {
        RUNNING.clear();
    }
}
