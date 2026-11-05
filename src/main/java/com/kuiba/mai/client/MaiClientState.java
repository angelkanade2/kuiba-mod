package com.kuiba.mai.client;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

/**
 * Client mirror of ritual progress, other players' 脉门 auras, and one-shot ring effects.
 * No client-only types, so dedicated servers can load the payload classes that call in here.
 */
public final class MaiClientState {
    /** How long a one-shot effect record is kept, in ticks. Longer than any animation. */
    private static final long FX_TTL = 200L;
    /** A running combo with no update for this long is assumed dropped (death, disconnect). Above the max gap. */
    private static final long COMBO_STALE_TICKS = 220L;

    private static boolean ritualActive;
    private static float progress;
    private static long clock;
    /** Last movement input we pushed to the server, so we only send on change. */
    private static int sentForward;
    private static int sentStrafe;
    private static final Map<Integer, Aura> AURAS = new HashMap<>();
    private static final Map<Integer, Fx> EFFECTS = new HashMap<>();
    @Nullable
    private static Combo combo;

    private MaiClientState() {}

    public static void onCombo(int slot, int done, int total, int state) {
        combo = new Combo(slot, done, total, state, clock);
    }

    /** Latest combo cursor for the local player, or null once it has gone stale. */
    public static @Nullable Combo combo() {
        if (combo != null && combo.running() && clock - combo.tick() > COMBO_STALE_TICKS) {
            combo = null;
        }
        return combo;
    }

    public static void onProgress(float next, boolean cancelled) {
        if (cancelled) {
            ritualActive = false;
            progress = 0.0F;
            return;
        }
        progress = next;
        ritualActive = next < 1.0F;
    }

    public static boolean ritualActive() {
        return ritualActive;
    }

    public static float progress() {
        return progress;
    }

    /** Called once per client tick. Drives animation time and expires stale effects. */
    public static void tick() {
        clock++;
        if (EFFECTS.isEmpty()) {
            return;
        }
        Iterator<Fx> it = EFFECTS.values().iterator();
        while (it.hasNext()) {
            if (clock - it.next().startTick > FX_TTL) {
                it.remove();
            }
        }
    }

    /** Monotonic client tick counter. Add the partial tick for smooth animation. */
    public static long now() {
        return clock;
    }

    public static void onAura(int entityId, int gate, float intensity) {
        AURAS.put(entityId, new Aura(gate, Math.clamp(intensity, 0.0F, 1.0F)));
    }

    public static @Nullable Aura aura(int entityId) {
        return AURAS.get(entityId);
    }

    public static void onFx(int entityId, int kind, int gate) {
        EFFECTS.put(entityId, new Fx(kind, gate, clock));
    }

    public static @Nullable Fx fx(int entityId) {
        return EFFECTS.get(entityId);
    }

    /** Snapshot of entity ids that currently have a one-shot effect. */
    public static int[] fxEntityIds() {
        if (EFFECTS.isEmpty()) {
            return new int[0];
        }
        int[] ids = new int[EFFECTS.size()];
        int i = 0;
        for (Integer id : EFFECTS.keySet()) {
            ids[i++] = id;
        }
        return ids;
    }

    /** Disconnect or dimension change: forget everything about other players. */
    public static void clear() {
        AURAS.clear();
        EFFECTS.clear();
        ritualActive = false;
        progress = 0.0F;
        combo = null;
        sentForward = 0;
        sentStrafe = 0;
    }

    /** @return true the first time a new pair shows up, so the caller can push it once. */
    public static boolean shouldSendInput(int forward, int strafe) {
        if (forward != sentForward || strafe != sentStrafe) {
            sentForward = forward;
            sentStrafe = strafe;
            return true;
        }
        return false;
    }

    public record Aura(int gate, float intensity) {}

    /** {@code state} is a {@code ComboStatePayload.STATE_*} value; {@code tick} is the client clock at receipt. */
    public record Combo(int slot, int done, int total, int state, long tick) {
        public boolean running() {
            return state == 0;
        }

        public boolean failed() {
            return state == 2;
        }
    }

    public static final class Fx {
        public final int kind;
        public final int gate;
        public final long startTick;

        Fx(int kind, int gate, long startTick) {
            this.kind = kind;
            this.gate = gate;
            this.startTick = startTick;
        }
    }
}
