package com.kuiba.mai;

import org.joml.Vector3f;

import com.kuiba.entity.MaiBolt;

import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Particle and sound work for every 脉 skill. All server-side so every nearby player sees it.
 * Each skill has its own wuxia palette — vermilion-gold for killing moves, jade for defense,
 * indigo for control, warm gold for healing — that darkens with 倍率. Sounds are layered pairs
 * (a wind/impact body plus a bell / flute / plucked-string top) whose pitch sinks with 倍率.
 * Every cast is staged over several ticks through {@link MaiFxSequencer}: a wind-up, a moving
 * body, and a release — a single static frame reads as a screenshot, not a skill.
 */
public final class MaiSkillFx {
    private static final Vector3f WHITE = new Vector3f(0.96F, 0.98F, 1.0F);

    /** 脉闪: 月白入青碧 — light-footed, stays closest to the 脉 base cyan. */
    private static final Vector3f[] PALETTE_DASH = ramp(0xEAF9FF, 0x2FB9E8);
    /** 脉击: 赤金 — the killing stroke, gold flash into vermilion. */
    private static final Vector3f[] PALETTE_STRIKE = ramp(0xFFD36A, 0xC8391F);
    /** 脉弹: 紫电 — pale violet into deep indigo. */
    private static final Vector3f[] PALETTE_BOLT = ramp(0xD9C8FF, 0x5A2FD6);
    /** 脉冲: 玄青 — misty steel-blue into ink. */
    private static final Vector3f[] PALETTE_WAVE = ramp(0x9CC4E4, 0x123C6E);
    /** 脉护: 玉 — pale jade into deep verdant. */
    private static final Vector3f[] PALETTE_GUARD = ramp(0xD8FFE8, 0x2E9E7B);
    /** 脉愈: 暖金化翠 — warm candlelight into jade green. */
    private static final Vector3f[] PALETTE_MEND = ramp(0xFFF3B0, 0x58B368);
    /** 脉刃: 纯金 — blade light, pale gold into old bronze-gold. */
    private static final Vector3f[] PALETTE_EDGE = ramp(0xFFF0A0, 0xE8961E);
    /** 脉缚: 玄紫 — pale orchid into deep violet. */
    private static final Vector3f[] PALETTE_BIND = ramp(0xE3C6FF, 0x6A1FB8);
    /** 脉障: 琥珀 — pale amber into bronze. */
    private static final Vector3f[] PALETTE_BARRIER = ramp(0xFFE0A3, 0xA8641E);
    /** Generic 脉 burst (aoe_zone): the base cyan ramp, kept for data-pack skills. */
    private static final Vector3f[] PALETTE_MAI = ramp(0x8CF2FF, 0x0B5F8C);

    private MaiSkillFx() {}

    /** Five tones from a bright first step to a deep fifth, for the ×1–×10 倍率 ladder. */
    private static Vector3f[] ramp(int bright, int deep) {
        Vector3f a = rgb(bright);
        Vector3f b = rgb(deep);
        Vector3f[] tones = new Vector3f[5];
        for (int i = 0; i < tones.length; i++) {
            tones[i] = new Vector3f(a).lerp(b, i / 4.0F);
        }
        return tones;
    }

    private static Vector3f rgb(int hex) {
        return new Vector3f(((hex >> 16) & 0xFF) / 255.0F, ((hex >> 8) & 0xFF) / 255.0F, (hex & 0xFF) / 255.0F);
    }

    public static DustParticleOptions dust(Vector3f[] palette, int charge, float scale) {
        return new DustParticleOptions(tone(palette, charge), scale);
    }

    /** Bright core fading into the charge tone. */
    public static DustColorTransitionOptions spark(Vector3f[] palette, int charge, float scale) {
        return new DustColorTransitionOptions(WHITE, tone(palette, charge), scale);
    }

    /** 倍率 step index, 0-based. */
    private static int step(int charge) {
        return MaiCombo.chargeIndex(charge);
    }

    private static Vector3f tone(Vector3f[] palette, int charge) {
        return palette[Math.min(palette.length - 1, step(charge))];
    }

    private static int density(int charge, int base) {
        return Math.round(base * (1.0F + 0.5F * step(charge)));
    }

    // ---- per skill -------------------------------------------------------------------------

    /**
     * 脉闪: a kick ring at launch, then the streak lights segment by segment from start to end
     * over 6 ticks — the trail "catches up" with the player — and a burst where the dash ends.
     * {@code dir} is the dash heading.
     */
    public static void dash(ServerPlayer player, int charge, double distance, Vec3 dir) {
        ServerLevel level = player.serverLevel();
        Vec3 look = dir.lengthSqr() < 1.0E-6D ? player.getLookAngle() : dir.normalize();
        Vec3 start = player.position().add(0.0D, 0.9D, 0.0D);
        Vec3 end = start.add(look.scale(distance));
        ring(level, start, look, 0.9D, density(charge, 18), spark(PALETTE_DASH, charge, 1.1F));
        point(level, spark(PALETTE_DASH, charge, 1.5F), start);
        sprinkle(level, ParticleTypes.END_ROD, start, 0.3D, density(charge, 4), 0.06D);
        sound(level, player, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.6F, charge);
        int frames = 6;
        MaiFxSequencer.schedule(level, frames, frame -> {
            // Each frame lights the next slice of the line; head frames are brighter.
            int n = density(charge, 4);
            for (int i = 0; i <= n; i++) {
                double t = Mth.lerp(i / (double) n, frame / (double) frames, (frame + 1) / (double) frames);
                point(level, frame >= frames - 2 ? spark(PALETTE_DASH, charge, 1.2F) : dust(PALETTE_DASH, charge, 0.9F),
                        start.lerp(end, t));
            }
            if (frame == frames - 1) {
                burst(level, PALETTE_DASH, end, 0.35D, density(charge, 10), charge);
                sprinkle(level, ParticleTypes.END_ROD, end, 0.25D, density(charge, 5), 0.08D);
            }
        });
    }

    /**
     * 脉击: a wind-up flash at the hand, then the 140° arc sweeps across over 4 ticks like a blade
     * pass, and the target takes a starburst plus a ground ring that rolls out from under it.
     * Sound: the sweep over a war-drum hit — 一刀一鼓.
     */
    public static void strike(ServerPlayer player, int charge, LivingEntity target) {
        ServerLevel level = player.serverLevel();
        Vec3 look = horizontal(player.getLookAngle());
        Vec3 side = new Vec3(-look.z, 0.0D, look.x);
        Vec3 center = player.position().add(0.0D, 1.1D, 0.0D);
        burst(level, PALETTE_STRIKE, center.add(look.scale(0.8D)), 0.15D, density(charge, 5), charge);
        chord(level, player, SoundEvents.PLAYER_ATTACK_SWEEP, SoundEvents.NOTE_BLOCK_BASEDRUM, 1.0F, 1.2F, charge);
        int sweeps = 4;
        MaiFxSequencer.schedule(level, sweeps, frame -> {
            double a0 = -70.0D + 140.0D * frame / sweeps;
            double a1 = -70.0D + 140.0D * (frame + 1) / sweeps;
            int n = Math.max(3, density(charge, 16) / sweeps);
            for (int i = 0; i <= n; i++) {
                double a = Math.toRadians(Mth.lerp(i / (double) n, a0, a1));
                Vec3 p = center.add(look.scale(Math.cos(a) * 1.6D)).add(side.scale(Math.sin(a) * 1.6D)).add(0.0D, Math.sin(a) * -0.25D, 0.0D);
                point(level, spark(PALETTE_STRIKE, charge, 1.0F), p);
            }
        });
        if (target != null) {
            Vec3 hit = target.position().add(0.0D, target.getBbHeight() * 0.6D, 0.0D);
            Vec3 feet = target.position();
            burst(level, PALETTE_STRIKE, hit, 0.45D, density(charge, 14), charge);
            sprinkle(level, ParticleTypes.ELECTRIC_SPARK, hit, 0.3D, density(charge, 6), 0.25D);
            // Aftermath: a shock ring rolls out along the ground from the point of impact.
            MaiFxSequencer.schedule(level, 3, frame -> {
                ring(level, feet.add(0.0D, 0.12D, 0.0D), new Vec3(0, 1, 0), 0.5D + 0.5D * frame,
                        density(charge, 10), dust(PALETTE_STRIKE, charge, 1.0F));
                sprinkle(level, ParticleTypes.END_ROD, hit, 0.2D, density(charge, 2), 0.05D);
            });
        }
    }

    /**
     * 脉弹 cast: a few motes spiral in toward the hand while the volley circles the caster, then the
     * gate flare pops as the bolts release. The cast itself is silent; the 咻 comes at release.
     * The volley itself is {@link MaiBolt}.
     */
    public static void boltCast(ServerPlayer player, int charge) {
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();
        Vec3 hand = player.getEyePosition().add(look.scale(0.8D)).add(0.0D, -0.2D, 0.0D);
        RandomSource random = level.random;
        MaiFxSequencer.schedule(level, MaiBolt.ORBIT_TICKS, frame -> {
            if (frame < MaiBolt.ORBIT_TICKS - 2 && frame % 2 == 0) {
                double r = 1.3D - 0.12D * frame;
                int n = density(charge, 2);
                for (int i = 0; i < n; i++) {
                    double a = random.nextDouble() * Math.PI * 2.0D;
                    double y = (random.nextDouble() - 0.5D) * r;
                    double rr = Math.sqrt(Math.max(0.0D, r * r - y * y));
                    // count=1 places the mote exactly and turns the offsets into its velocity,
                    // so each mote drifts inward toward the hand.
                    level.sendParticles(dust(PALETTE_BOLT, charge, 0.7F),
                            hand.x + Math.cos(a) * rr, hand.y + y, hand.z + Math.sin(a) * rr,
                            1, -Math.cos(a) * 0.10D, -y * 0.06D, -Math.sin(a) * 0.10D, 1.0D);
                }
            } else if (frame == MaiBolt.ORBIT_TICKS - 1) {
                ring(level, hand, look, 0.45D, density(charge, 12), spark(PALETTE_BOLT, charge, 0.8F));
                burst(level, PALETTE_BOLT, hand, 0.2D, density(charge, 6), charge);
            }
        });
    }

    /** Client-side trail behind a flying 脉弹. Kept thin so a ×10 volley does not fill the screen. */
    public static void boltTrail(Level level, MaiBolt bolt) {
        if (bolt.tickCount % 2 != 0) {
            return;
        }
        Vec3 pos = bolt.position().add(0.0D, bolt.getBbHeight() * 0.5D, 0.0D);
        Vec3 back = bolt.getDeltaMovement().scale(-0.5D);
        int charge = bolt.charge();
        int motes = 1 + charge / 2;
        for (int i = 0; i < motes; i++) {
            double f = level.random.nextDouble();
            double r = bolt.visualSize() * 0.5D;
            level.addParticle(dust(PALETTE_BOLT, charge, 0.6F + 0.1F * charge),
                    pos.x + back.x * f + (level.random.nextDouble() - 0.5D) * r,
                    pos.y + back.y * f + (level.random.nextDouble() - 0.5D) * r,
                    pos.z + back.z * f + (level.random.nextDouble() - 0.5D) * r,
                    0.0D, 0.0D, 0.0D);
        }
        if (bolt.tickCount % 6 == 0) {
            level.addParticle(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, back.x * 0.05D, back.y * 0.05D, back.z * 0.05D);
        }
    }

    public static void boltBurst(ServerLevel level, Vec3 at, int charge, float scale) {
        burst(level, PALETTE_BOLT, at, 0.6D * scale, density(charge, 8), charge);
        sprinkle(level, ParticleTypes.END_ROD, at, 0.2D, density(charge, 2), 0.12D);
    }

    /**
     * 脉冲 launch: the flat gate flares in front of the player, then a ground-pressure ring rolls
     * outward under the wave for 5 ticks. Sound: the sonic boom over a wind burst — 排山倒海.
     * The wave itself is {@link MaiWaves}.
     */
    public static void waveCast(ServerPlayer player, int charge) {
        ServerLevel level = player.serverLevel();
        Vec3 look = horizontal(player.getLookAngle());
        Vec3 front = player.position().add(0.0D, 1.0D, 0.0D).add(look.scale(0.8D));
        ring(level, front, look, 0.7D, density(charge, 16), spark(PALETTE_WAVE, charge, 1.2F));
        ring(level, front, look, 0.3D, density(charge, 8), spark(PALETTE_WAVE, charge, 0.9F));
        chord(level, player, SoundEvents.WARDEN_SONIC_BOOM, SoundEvents.NOTE_BLOCK_BASEDRUM, 0.8F, 1.3F, charge);
        Vec3 ground = player.position().add(0.0D, 0.1D, 0.0D);
        MaiFxSequencer.schedule(level, 5, frame -> {
            double r = 0.6D + 0.55D * frame;
            ring(level, front.subtract(0.0D, 0.8D, 0.0D), look, r, density(charge, 10), dust(PALETTE_WAVE, charge, 0.8F));
            ring(level, ground, new Vec3(0, 1, 0), r, density(charge, 12), dust(PALETTE_WAVE, charge, 0.7F));
        });
    }

    /** One step of a travelling 脉冲 ring. */
    public static void waveFront(ServerLevel level, Vec3 center, Vec3 dir, double radius, int charge) {
        ring(level, center, dir, radius, density(charge, (int) (10 + radius * 10)), spark(PALETTE_WAVE, charge, 1.3F));
        ring(level, center.subtract(dir.scale(0.35D)), dir, radius * 0.8D, density(charge, 8), dust(PALETTE_WAVE, charge, 0.9F));
    }

    /** 脉护: a shell closes in from 1.8 blocks to skin-tight over 7 ticks, then a base ring seals it. Sound: a temple chime. */
    public static void guard(ServerPlayer player, int charge) {
        ServerLevel level = player.serverLevel();
        Vec3 base = player.position();
        chord(level, player, SoundEvents.BEACON_POWER_SELECT, SoundEvents.NOTE_BLOCK_CHIME, 0.9F, 1.2F, charge);
        MaiFxSequencer.schedule(level, 7, frame -> {
            double r = 1.8D - 0.15D * frame;
            Vec3 center = base.add(0.0D, 1.0D, 0.0D);
            sphere(level, center, r, density(charge, 14), frame < 5 ? dust(PALETTE_GUARD, charge, 1.0F) : spark(PALETTE_GUARD, charge, 1.1F));
            if (frame == 6) {
                ring(level, base.add(0.0D, 0.1D, 0.0D), new Vec3(0, 1, 0), 1.0D, density(charge, 18), spark(PALETTE_GUARD, charge, 1.0F));
                sprinkle(level, ParticleTypes.END_ROD, center, 0.5D, density(charge, 5), 0.02D);
            }
        });
    }

    /** 脉愈: the double helix draws itself upward over 8 ticks, then motes float off the shoulders. Sound: a flute over the resonance — 箫声疗伤. */
    public static void mend(ServerPlayer player, int charge) {
        ServerLevel level = player.serverLevel();
        Vec3 base = player.position();
        chord(level, player, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundEvents.NOTE_BLOCK_FLUTE, 1.0F, 1.3F, charge);
        int frames = 8;
        RandomSource random = level.random;
        MaiFxSequencer.schedule(level, frames + 1, frame -> {
            if (frame < frames) {
                int n = density(charge, 4);
                for (int i = 0; i <= n; i++) {
                    double t = Mth.lerp(i / (double) n, frame / (double) frames, (frame + 1) / (double) frames);
                    double a = t * Math.PI * 4.0D;
                    for (int strand = 0; strand < 2; strand++) {
                        double s = a + strand * Math.PI;
                        point(level, spark(PALETTE_MEND, charge, 0.9F), base.add(Math.cos(s) * 0.7D, t * 2.1D, Math.sin(s) * 0.7D));
                    }
                }
            } else {
                for (int i = 0; i < density(charge, 8); i++) {
                    level.sendParticles(ParticleTypes.GLOW,
                            base.x + (random.nextDouble() - 0.5D),
                            base.y + 1.4D + random.nextDouble() * 0.6D,
                            base.z + (random.nextDouble() - 0.5D),
                            1, 0.0D, 0.04D, 0.0D, 1.0D);
                }
            }
        });
    }

    /** 脉刃: energy pours down the arm for 5 ticks, then the blade hand flashes. Sound: a plucked string over the chime, ending in a sweep. */
    public static void edge(ServerPlayer player, int charge) {
        ServerLevel level = player.serverLevel();
        Vec3 look = horizontal(player.getLookAngle());
        Vec3 side = new Vec3(-look.z, 0.0D, look.x);
        Vec3 shoulder = player.position().add(0.0D, 1.4D, 0.0D).add(side.scale(-0.4D));
        Vec3 hand = player.position().add(0.0D, 0.8D, 0.0D).add(side.scale(-0.45D)).add(look.scale(0.6D));
        chord(level, player, SoundEvents.AMETHYST_BLOCK_CHIME, SoundEvents.NOTE_BLOCK_BANJO, 0.9F, 1.2F, charge);
        MaiFxSequencer.schedule(level, 6, frame -> {
            if (frame < 5) {
                double f = frame / 5.0D;
                line(level, shoulder.lerp(hand, f * 0.5D), shoulder.lerp(hand, Math.min(1.0D, f + 0.2D)), density(charge, 3), dust(PALETTE_EDGE, charge, 0.8F));
                sprinkle(level, ParticleTypes.ELECTRIC_SPARK, shoulder.lerp(hand, f), 0.1D, density(charge, 2), 0.1D);
            } else {
                ring(level, hand, look, 0.5D, density(charge, 14), spark(PALETTE_EDGE, charge, 1.1F));
                burst(level, PALETTE_EDGE, hand, 0.25D, density(charge, 8), charge);
                sound(level, player, SoundEvents.PLAYER_ATTACK_SWEEP, 0.7F, 1.6F, charge);
            }
        });
    }

    /**
     * 脉缚: the cord lashes out from the player over 4 ticks, then three rings cinch tight around
     * the target. Sound: the chain over a low bass note. No target: a small fizzle at the hand instead of silence.
     */
    public static void bind(ServerPlayer player, int charge, LivingEntity target) {
        ServerLevel level = player.serverLevel();
        if (target == null) {
            Vec3 hand = player.getEyePosition().add(player.getLookAngle().scale(0.6D));
            burst(level, PALETTE_BIND, hand, 0.15D, density(charge, 4), charge);
            sound(level, player, SoundEvents.CHAIN_BREAK, 0.6F, 1.4F, charge);
            return;
        }
        chord(level, player, SoundEvents.CHAIN_PLACE, SoundEvents.NOTE_BLOCK_BASS, 1.0F, 1.1F, charge);
        Vec3 from = player.position().add(0.0D, 1.2D, 0.0D);
        Vec3 to = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        double r = Math.max(0.6D, target.getBbWidth() * 0.9D);
        double height = target.getBbHeight();
        MaiFxSequencer.schedule(level, 6, frame -> {
            if (frame < 4) {
                line(level, from, from.lerp(to, (frame + 1) / 4.0D), density(charge, 4), spark(PALETTE_BIND, charge, 0.8F));
            } else {
                double cinch = 1.35D - 0.45D * (frame - 3) / 3.0D;
                for (int band = 0; band < 3; band++) {
                    ring(level, target.position().add(0.0D, 0.15D + band * height * 0.3D, 0.0D), new Vec3(0, 1, 0),
                            r * cinch, density(charge, 14), dust(PALETTE_BIND, charge, 1.0F));
                }
            }
        });
    }

    /** 脉障: the cylinder wall rises row by row over 8 ticks, then a bright ring seals the top. Sound: a great bell over the beacon hum — 钟鸣立障. */
    public static void barrier(ServerPlayer player, int charge) {
        ServerLevel level = player.serverLevel();
        Vec3 base = player.position();
        chord(level, player, SoundEvents.BEACON_ACTIVATE, SoundEvents.NOTE_BLOCK_BELL, 0.9F, 1.1F, charge);
        int rows = 4;
        MaiFxSequencer.schedule(level, rows * 2 + 1, frame -> {
            if (frame < rows * 2) {
                if (frame % 2 == 0) {
                    int row = frame / 2;
                    ring(level, base.add(0.0D, 0.1D + row * 0.6D, 0.0D), new Vec3(0, 1, 0), 1.3D,
                            density(charge, 20), dust(PALETTE_BARRIER, charge, 1.0F));
                }
            } else {
                ring(level, base.add(0.0D, 0.1D + (rows - 1) * 0.6D, 0.0D), new Vec3(0, 1, 0), 1.3D,
                        density(charge, 20), spark(PALETTE_BARRIER, charge, 1.2F));
                sprinkle(level, ParticleTypes.END_ROD, base.add(0.0D, 1.0D, 0.0D), 1.0D, density(charge, 6), 0.02D);
            }
        });
    }

    /** {@code aoe_zone}: ground rings at the blast radius and a flash at the centre. No P0 skill uses this cast today. */
    public static void burst(ServerPlayer player, int charge, float radius) {
        ServerLevel level = player.serverLevel();
        Vec3 base = player.position().add(0.0D, 0.15D, 0.0D);
        burst(level, PALETTE_MAI, base.add(0.0D, 1.0D, 0.0D), 1.0D, density(charge, 30), charge);
        sprinkle(level, ParticleTypes.GLOW, base.add(0.0D, 0.5D, 0.0D), radius * 0.5D, density(charge, 12), 0.05D);
        level.sendParticles(ParticleTypes.FLASH, base.x, base.y + 1.0D, base.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        chord(level, player, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundEvents.NOTE_BLOCK_BASEDRUM, 1.0F, 0.9F, charge);
        // Three shock rings roll out one after another instead of appearing together.
        MaiFxSequencer.schedule(level, 5, frame -> {
            int i = Math.min(3, frame / 2 + 1);
            double r = radius * i / 3.0D;
            ring(level, base, new Vec3(0, 1, 0), r, density(charge, (int) (8 + r * 8)),
                    i == 3 ? spark(PALETTE_MAI, charge, 1.4F) : dust(PALETTE_MAI, charge, 1.1F));
        });
    }

    // ---- shapes ----------------------------------------------------------------------------

    /** Ring of {@code points} around {@code center}, in the plane whose normal is {@code normal}. */
    public static void ring(ServerLevel level, Vec3 center, Vec3 normal, double radius, int points, ParticleOptions particle) {
        Vec3 n = normal.lengthSqr() < 1.0E-6D ? new Vec3(0, 1, 0) : normal.normalize();
        Vec3 helper = Math.abs(n.y) > 0.9D ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 u = n.cross(helper).normalize();
        Vec3 v = n.cross(u).normalize();
        for (int i = 0; i < points; i++) {
            double a = Math.PI * 2.0D * i / points;
            point(level, particle, center.add(u.scale(Math.cos(a) * radius)).add(v.scale(Math.sin(a) * radius)));
        }
    }

    private static void sphere(ServerLevel level, Vec3 center, double radius, int points, ParticleOptions particle) {
        double golden = Math.PI * (3.0D - Math.sqrt(5.0D));
        for (int i = 0; i < points; i++) {
            double y = 1.0D - 2.0D * (i + 0.5D) / points;
            double r = Math.sqrt(1.0D - y * y);
            double a = golden * i;
            point(level, particle, center.add(Math.cos(a) * r * radius, y * radius, Math.sin(a) * r * radius));
        }
    }

    private static void line(ServerLevel level, Vec3 from, Vec3 to, int points, ParticleOptions particle) {
        for (int i = 0; i <= points; i++) {
            point(level, particle, from.lerp(to, (double) i / points));
        }
    }

    private static void burst(ServerLevel level, Vector3f[] palette, Vec3 at, double spread, int count, int charge) {
        level.sendParticles(spark(palette, charge, 1.2F), at.x, at.y, at.z, count, spread, spread, spread, 0.0D);
        level.sendParticles(dust(palette, charge, 0.8F), at.x, at.y, at.z, count / 2, spread * 1.6D, spread * 1.6D, spread * 1.6D, 0.0D);
    }

    private static void sprinkle(ServerLevel level, ParticleOptions particle, Vec3 at, double spread, int count, double speed) {
        level.sendParticles(particle, at.x, at.y, at.z, count, spread, spread, spread, speed);
    }

    private static void point(ServerLevel level, ParticleOptions particle, Vec3 p) {
        level.sendParticles(particle, p.x, p.y, p.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    private static Vec3 horizontal(Vec3 look) {
        Vec3 flat = new Vec3(look.x, 0.0D, look.z);
        return flat.lengthSqr() < 1.0E-6D ? new Vec3(0, 0, 1) : flat.normalize();
    }

    /** A body sound (wind, impact, chain) with a melody sound (bell, flute, string) on top. */
    private static void chord(ServerLevel level, Entity at, SoundEvent body, net.minecraft.core.Holder<SoundEvent> melody, float volume, float pitch, int charge) {
        sound(level, at, body, volume, pitch, charge);
        float melodyPitch = Math.max(0.5F, pitch - 0.16F * step(charge));
        level.playSound(null, at.getX(), at.getY(), at.getZ(), melody.value(), SoundSource.PLAYERS, volume * 0.8F, melodyPitch);
    }

    /** Higher charge plays lower, per the spec's 高倍率低音脉震. */
    private static void sound(ServerLevel level, Entity at, SoundEvent sound, float volume, float pitch, int charge) {
        level.playSound(null, at.getX(), at.getY(), at.getZ(), sound, SoundSource.PLAYERS, volume,
                Math.max(0.5F, pitch - 0.16F * step(charge)));
    }
}
