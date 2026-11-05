package com.kuiba.mai.client;

import org.jetbrains.annotations.Nullable;

import com.kuiba.KuibaSounds;
import com.kuiba.mai.MaiApi;
import com.kuiba.mai.MaiData;
import com.kuiba.mai.payload.MaiFxPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Animation model for the 脉门 rings. Pure math over {@link MaiClientState}; the body layer and the
 * HUD both read it so they pulse together. Every opened gate stays visible: bright and glowing while
 * the pool is full, dimming to a dormant ring as it drains. 开脉 lights them in sequence, 冲脉 bursts the new one.
 */
public final class MaiFx {
    /** Ticks between one meridian lighting and the next during 开脉. Was 3; 12 gates now run in ~1.1 s. */
    public static final int OPEN_STEP_TICKS = 2;
    public static final int OPEN_FLASH_TICKS = 12;
    /** Body rings from 开脉 last this many ticks, then dissipate while mai keeps draining. */
    public static final int OPEN_VISUAL_TICKS = 40;
    private static final int OPEN_VISUAL_FADE_TICKS = 8;
    public static final int UNLOCK_TICKS = 32;
    public static final int CAST_TICKS = 9;
    public static final int MAX_GATE = 12;
    /** Opened gates never disappear; with an empty pool they sit at this alpha without glow. */
    public static final float DORMANT_ALPHA = 0.4F;

    private MaiFx() {}

    /** What is known about one player's 脉门 for drawing. {@code ritualProgress} is negative when idle. */
    public record View(int gate, float intensity, @Nullable MaiClientState.Fx fx, float ritualProgress, boolean self) {
        public boolean opened(int gate) {
            return gate >= 1 && gate <= this.gate;
        }
    }

    /** Per-ring draw parameters. {@code spin} in degrees; {@code glow} is extra additive halo strength. */
    public record Ring(float alpha, float scale, float spin, float glow) {}

    public static float time(float partialTick) {
        return MaiClientState.now() + partialTick;
    }

    public static @Nullable View view(Player player) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean self = player == minecraft.player;
        int gate;
        float intensity;
        if (self) {
            MaiData mai = MaiApi.data(player);
            gate = mai.gate();
            intensity = Math.clamp(mai.current() / Math.max(1.0F, mai.shownMax()), 0.0F, 1.0F);
        } else {
            MaiClientState.Aura aura = MaiClientState.aura(player.getId());
            if (aura == null) {
                return null;
            }
            gate = aura.gate();
            intensity = aura.intensity();
        }
        MaiClientState.Fx fx = MaiClientState.fx(player.getId());
        float ritual = self && MaiClientState.ritualActive() ? MaiClientState.progress() : -1.0F;
        if (gate <= 0 && fx == null && ritual < 0.0F) {
            return null;
        }
        return new View(gate, intensity, fx, ritual, self);
    }

    /**
     * Draw state for meridian {@code g} (1..12) at animation time {@code time}, or null when invisible.
     */
    public static @Nullable Ring ring(View view, int g, float time) {
        boolean opened = view.opened(g);
        float alpha = 0.0F;
        float scale = 1.0F;
        float glow = 0.0F;
        float spin = time * 3.5F + g * 37.0F;

        if (opened) {
            // Lingering body glow only while the 开脉 visual window is open (~2 s).
            float intensity = view.intensity() * openVisualFactor(view, time);
            if (intensity > 0.001F) {
                float base = DORMANT_ALPHA + (1.0F - DORMANT_ALPHA) * intensity;
                float pulse = 0.88F + 0.12F * Mth.sin(time * 0.17F + g * 0.9F);
                alpha = base * pulse;
                if (intensity < 0.25F) {
                    alpha *= 0.75F + 0.25F * Mth.sin(time * 1.15F + g * 1.7F);
                }
                glow = 0.45F * alpha * intensity;
            }
        }

        MaiClientState.Fx fx = view.fx();
        if (fx != null) {
            float t = time - fx.startTick;
            switch (fx.kind) {
                case MaiFxPayload.KIND_OPEN -> {
                    if (opened) {
                        float lit = (g - 1) * OPEN_STEP_TICKS;
                        if (t < lit) {
                            alpha = 0.0F;
                            glow = 0.0F;
                        } else if (t < lit + OPEN_FLASH_TICKS) {
                            float f = (t - lit) / OPEN_FLASH_TICKS;
                            float k = easeOut(f);
                            scale = 1.9F - 0.9F * k;
                            alpha = Math.max(alpha, 1.0F - 0.15F * k);
                            glow += 1.3F * (1.0F - k);
                        }
                    }
                }
                case MaiFxPayload.KIND_UNLOCK -> {
                    if (g == fx.gate) {
                        if (t < UNLOCK_TICKS) {
                            float f = t / UNLOCK_TICKS;
                            if (f < 0.4F) {
                                float k = easeOut(f / 0.4F);
                                scale = 0.2F + 1.3F * k;
                                alpha = Math.max(alpha, k);
                                glow = Math.max(glow, 1.6F * k);
                            } else {
                                float k = easeOut((f - 0.4F) / 0.6F);
                                scale = 1.5F - 0.5F * k;
                                alpha = Math.max(alpha, 1.0F - 0.25F * k);
                                glow = Math.max(glow, 1.6F * (1.0F - k));
                            }
                            spin = time * 14.0F;
                        }
                    } else if (opened && t < 14.0F) {
                        float k = 1.0F - t / 14.0F;
                        alpha = Math.max(alpha, 0.85F * k);
                        glow += 0.6F * k;
                    }
                }
                case MaiFxPayload.KIND_CAST -> {
                    if (opened && t < CAST_TICKS) {
                        float k = 1.0F - t / CAST_TICKS;
                        scale *= 1.0F + 0.35F * k;
                        alpha = Math.max(alpha, 0.5F + 0.5F * k);
                        glow += 0.9F * k;
                        spin += 40.0F * k;
                    }
                }
                default -> {
                }
            }
        }

        if (view.ritualProgress() >= 0.0F && g == view.gate() + 1) {
            // 冲脉 in progress: the next meridian slowly takes shape.
            float p = view.ritualProgress();
            float breathe = 0.75F + 0.25F * Mth.sin(time * 0.45F);
            alpha = Math.max(alpha, (0.12F + 0.55F * p) * breathe);
            scale = 0.55F + 0.45F * p;
            spin = time * 10.0F;
            glow = Math.max(glow, 0.5F * p);
        }

        if (alpha <= 0.01F) {
            return null;
        }
        return new Ring(Math.min(1.0F, alpha), scale, spin, Math.min(1.6F, glow));
    }

    /** Full-screen tint for the local player's own 开脉 / 冲脉, 0 when idle. */
    public static float screenFlash(@Nullable MaiClientState.Fx fx, float time) {
        if (fx == null) {
            return 0.0F;
        }
        float t = time - fx.startTick;
        return switch (fx.kind) {
            case MaiFxPayload.KIND_OPEN -> {
                float length = Math.max(1, fx.gate) * OPEN_STEP_TICKS + OPEN_FLASH_TICKS;
                if (t >= length) {
                    yield 0.0F;
                }
                float f = t / length;
                yield 0.5F * (1.0F - f) * (0.7F + 0.3F * Mth.sin(t * 1.2F));
            }
            case MaiFxPayload.KIND_UNLOCK -> t >= UNLOCK_TICKS ? 0.0F : 0.65F * (1.0F - easeOut(t / UNLOCK_TICKS));
            default -> 0.0F;
        };
    }

    /** Play the composite 开脉 clip for {@code gateCount} meridians (1..12). */
    public static void playOpenSound(int entityId, int gateCount) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        int total = Math.clamp(gateCount, 1, MAX_GATE);
        Entity entity = minecraft.level.getEntity(entityId);
        if (entity == null) {
            return;
        }
        minecraft.level.playLocalSound(entity.getX(), entity.getY() + 1.0D, entity.getZ(),
                KuibaSounds.openMaiGate(total), SoundSource.PLAYERS, 1.0F, 1.0F, false);
    }

    public static void tick() {
        MaiClientState.tick();
    }

    /** 1 during the 开脉 body-FX window, fading over the last 8 ticks, else 0. Mai pool is unaffected. */
    public static float openVisualFactor(View view, float time) {
        MaiClientState.Fx fx = view.fx();
        if (fx == null || fx.kind != MaiFxPayload.KIND_OPEN) {
            return 0.0F;
        }
        float age = time - fx.startTick;
        if (age < 0.0F || age >= OPEN_VISUAL_TICKS) {
            return 0.0F;
        }
        float fadeStart = OPEN_VISUAL_TICKS - OPEN_VISUAL_FADE_TICKS;
        return age <= fadeStart ? 1.0F : Math.clamp((OPEN_VISUAL_TICKS - age) / OPEN_VISUAL_FADE_TICKS, 0.0F, 1.0F);
    }

    private static float easeOut(float f) {
        f = Math.clamp(f, 0.0F, 1.0F);
        return 1.0F - (1.0F - f) * (1.0F - f) * (1.0F - f);
    }
}
