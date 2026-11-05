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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Cyan particle work for every 脉 skill. All server-side so every nearby player sees it.
 * Higher 倍率 packs more particles and shifts the colour from pale cyan toward deep azure.
 */
public final class MaiSkillFx {
    private static final Vector3f WHITE = new Vector3f(0.86F, 1.0F, 1.0F);
    /** One tone per 倍率 step: ×1, ×2, ×3, ×5, ×10. */
    private static final Vector3f[] TONE = {
            new Vector3f(0.55F, 0.95F, 1.0F),
            new Vector3f(0.30F, 0.85F, 1.0F),
            new Vector3f(0.12F, 0.66F, 1.0F),
            new Vector3f(0.06F, 0.48F, 0.95F),
            new Vector3f(0.02F, 0.34F, 0.86F),
    };

    private MaiSkillFx() {}

    public static DustParticleOptions dust(int charge, float scale) {
        return new DustParticleOptions(tone(charge), scale);
    }

    /** Bright core fading into the charge tone. */
    public static DustColorTransitionOptions spark(int charge, float scale) {
        return new DustColorTransitionOptions(WHITE, tone(charge), scale);
    }

    /** 倍率 step index, 0-based. */
    private static int step(int charge) {
        return MaiCombo.chargeIndex(charge);
    }

    private static Vector3f tone(int charge) {
        return TONE[Math.min(TONE.length - 1, step(charge))];
    }

    private static int density(int charge, int base) {
        return Math.round(base * (1.0F + 0.5F * step(charge)));
    }

    // ---- per skill -------------------------------------------------------------------------

    /** 脉闪: a ring kicked off behind the player and a streak along the dash line. {@code dir} is the dash heading. */
    public static void dash(ServerPlayer player, int charge, double distance, Vec3 dir) {
        ServerLevel level = player.serverLevel();
        Vec3 look = dir.lengthSqr() < 1.0E-6D ? player.getLookAngle() : dir.normalize();
        Vec3 start = player.position().add(0.0D, 0.9D, 0.0D);
        ring(level, start, look, 0.9D, density(charge, 18), spark(charge, 1.1F));
        line(level, start, start.add(look.scale(distance)), density(charge, 20), dust(charge, 0.9F));
        sprinkle(level, ParticleTypes.END_ROD, start, 0.3D, density(charge, 4), 0.06D);
        sound(level, player, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.6F, charge);
    }

    /** 脉击: an arc swept in front of the player and an impact star on the target. */
    public static void strike(ServerPlayer player, int charge, LivingEntity target) {
        ServerLevel level = player.serverLevel();
        Vec3 look = horizontal(player.getLookAngle());
        Vec3 side = new Vec3(-look.z, 0.0D, look.x);
        Vec3 center = player.position().add(0.0D, 1.1D, 0.0D);
        int points = density(charge, 16);
        for (int i = 0; i <= points; i++) {
            double a = Math.toRadians(-70.0D + 140.0D * i / points);
            Vec3 p = center.add(look.scale(Math.cos(a) * 1.6D)).add(side.scale(Math.sin(a) * 1.6D)).add(0.0D, Math.sin(a) * -0.25D, 0.0D);
            point(level, spark(charge, 1.0F), p);
        }
        if (target != null) {
            Vec3 hit = target.position().add(0.0D, target.getBbHeight() * 0.6D, 0.0D);
            burst(level, hit, 0.45D, density(charge, 14), charge);
            sprinkle(level, ParticleTypes.ELECTRIC_SPARK, hit, 0.3D, density(charge, 6), 0.25D);
        }
        sound(level, player, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 1.3F, charge);
    }

    /** 脉弹 cast: a small gate flare at the hand. The orb itself is {@link MaiBolt}. */
    public static void boltCast(ServerPlayer player, int charge) {
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();
        Vec3 hand = player.getEyePosition().add(look.scale(0.8D)).add(0.0D, -0.2D, 0.0D);
        ring(level, hand, look, 0.45D, density(charge, 12), spark(charge, 0.8F));
        sound(level, player, SoundEvents.SHULKER_SHOOT, 0.9F, 1.5F, charge);
    }

    /** Client-side trail behind a flying 脉弹. */
    public static void boltTrail(Level level, MaiBolt bolt) {
        Vec3 pos = bolt.position().add(0.0D, bolt.getBbHeight() * 0.5D, 0.0D);
        Vec3 back = bolt.getDeltaMovement().scale(-0.5D);
        int charge = bolt.charge();
        for (int i = 0; i < 1 + charge; i++) {
            double f = level.random.nextDouble();
            double r = bolt.visualSize() * 0.6D;
            level.addParticle(dust(charge, 0.7F + 0.15F * charge),
                    pos.x + back.x * f + (level.random.nextDouble() - 0.5D) * r,
                    pos.y + back.y * f + (level.random.nextDouble() - 0.5D) * r,
                    pos.z + back.z * f + (level.random.nextDouble() - 0.5D) * r,
                    0.0D, 0.0D, 0.0D);
        }
        if (bolt.tickCount % 3 == 0) {
            level.addParticle(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, back.x * 0.05D, back.y * 0.05D, back.z * 0.05D);
        }
    }

    public static void boltBurst(ServerLevel level, Vec3 at, int charge, float scale) {
        burst(level, at, 0.6D * scale, density(charge, 22), charge);
        sprinkle(level, ParticleTypes.END_ROD, at, 0.2D, density(charge, 6), 0.12D);
    }

    /** 脉冲 launch: a flat gate flare in front of the player. The wave itself is {@link MaiWaves}. */
    public static void waveCast(ServerPlayer player, int charge) {
        ServerLevel level = player.serverLevel();
        Vec3 look = horizontal(player.getLookAngle());
        Vec3 front = player.position().add(0.0D, 1.0D, 0.0D).add(look.scale(0.8D));
        ring(level, front, look, 0.7D, density(charge, 16), spark(charge, 1.2F));
        ring(level, front, look, 0.3D, density(charge, 8), spark(charge, 0.9F));
        sound(level, player, SoundEvents.WARDEN_SONIC_BOOM, 0.6F, 1.6F, charge);
    }

    /** One step of a travelling 脉冲 ring. */
    public static void waveFront(ServerLevel level, Vec3 center, Vec3 dir, double radius, int charge) {
        ring(level, center, dir, radius, density(charge, (int) (10 + radius * 10)), spark(charge, 1.3F));
        ring(level, center.subtract(dir.scale(0.35D)), dir, radius * 0.8D, density(charge, 8), dust(charge, 0.9F));
    }

    /** 脉护: a shell of motes closing in around the body. */
    public static void guard(ServerPlayer player, int charge) {
        ServerLevel level = player.serverLevel();
        sphere(level, player.position().add(0.0D, 1.0D, 0.0D), 1.2D, density(charge, 36), dust(charge, 1.0F));
        ring(level, player.position().add(0.0D, 0.1D, 0.0D), new Vec3(0, 1, 0), 1.0D, density(charge, 18), spark(charge, 1.0F));
        sound(level, player, SoundEvents.BEACON_POWER_SELECT, 0.8F, 1.6F, charge);
    }

    /** 脉愈: two strands spiralling up the body. */
    public static void mend(ServerPlayer player, int charge) {
        ServerLevel level = player.serverLevel();
        Vec3 base = player.position();
        int points = density(charge, 24);
        for (int i = 0; i < points; i++) {
            double t = (double) i / points;
            double a = t * Math.PI * 4.0D;
            for (int strand = 0; strand < 2; strand++) {
                double s = a + strand * Math.PI;
                point(level, spark(charge, 0.9F), base.add(Math.cos(s) * 0.7D, t * 2.1D, Math.sin(s) * 0.7D));
            }
        }
        sprinkle(level, ParticleTypes.GLOW, base.add(0.0D, 1.0D, 0.0D), 0.5D, density(charge, 6), 0.02D);
        sound(level, player, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0F, 1.5F, charge);
    }

    /** 脉刃: sparks flowing down the arms into the weapon hand. */
    public static void edge(ServerPlayer player, int charge) {
        ServerLevel level = player.serverLevel();
        Vec3 look = horizontal(player.getLookAngle());
        Vec3 side = new Vec3(-look.z, 0.0D, look.x);
        Vec3 shoulder = player.position().add(0.0D, 1.4D, 0.0D).add(side.scale(-0.4D));
        Vec3 hand = player.position().add(0.0D, 0.8D, 0.0D).add(side.scale(-0.45D)).add(look.scale(0.6D));
        line(level, shoulder, hand, density(charge, 10), spark(charge, 0.8F));
        ring(level, hand, look, 0.35D, density(charge, 10), dust(charge, 0.9F));
        sprinkle(level, ParticleTypes.ELECTRIC_SPARK, hand, 0.2D, density(charge, 5), 0.2D);
        sound(level, player, SoundEvents.AMETHYST_BLOCK_CHIME, 0.9F, 1.1F, charge);
    }

    /** 脉缚: a cord from the player to the target and a ring cinched round its feet. */
    public static void bind(ServerPlayer player, int charge, LivingEntity target) {
        ServerLevel level = player.serverLevel();
        if (target == null) {
            return;
        }
        Vec3 from = player.position().add(0.0D, 1.2D, 0.0D);
        Vec3 to = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        line(level, from, to, density(charge, (int) (8 + from.distanceTo(to) * 3)), spark(charge, 0.8F));
        double r = Math.max(0.6D, target.getBbWidth() * 0.9D);
        for (int band = 0; band < 3; band++) {
            ring(level, target.position().add(0.0D, 0.15D + band * target.getBbHeight() * 0.3D, 0.0D), new Vec3(0, 1, 0), r,
                    density(charge, 14), dust(charge, 1.0F));
        }
        sound(level, player, SoundEvents.CHAIN_PLACE, 1.0F, 1.2F, charge);
    }

    /** 脉障: a standing cylinder wall around the player. */
    public static void barrier(ServerPlayer player, int charge) {
        ServerLevel level = player.serverLevel();
        Vec3 base = player.position();
        int rows = 4;
        for (int row = 0; row < rows; row++) {
            ring(level, base.add(0.0D, 0.1D + row * 0.6D, 0.0D), new Vec3(0, 1, 0), 1.3D, density(charge, 20),
                    row == rows - 1 ? spark(charge, 1.1F) : dust(charge, 1.0F));
        }
        sprinkle(level, ParticleTypes.END_ROD, base.add(0.0D, 1.0D, 0.0D), 1.0D, density(charge, 6), 0.02D);
        sound(level, player, SoundEvents.BEACON_ACTIVATE, 0.7F, 1.8F, charge);
    }

    /** {@code aoe_zone}: ground rings at the blast radius and a flash at the centre. No P0 skill uses this cast today. */
    public static void burst(ServerPlayer player, int charge, float radius) {
        ServerLevel level = player.serverLevel();
        Vec3 base = player.position().add(0.0D, 0.15D, 0.0D);
        for (int i = 1; i <= 3; i++) {
            double r = radius * i / 3.0D;
            ring(level, base, new Vec3(0, 1, 0), r, density(charge, (int) (8 + r * 8)), i == 3 ? spark(charge, 1.4F) : dust(charge, 1.1F));
        }
        burst(level, base.add(0.0D, 1.0D, 0.0D), 1.0D, density(charge, 30), charge);
        sprinkle(level, ParticleTypes.GLOW, base.add(0.0D, 0.5D, 0.0D), radius * 0.5D, density(charge, 12), 0.05D);
        level.sendParticles(ParticleTypes.FLASH, base.x, base.y + 1.0D, base.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        sound(level, player, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.0F, 0.9F, charge);
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

    private static void burst(ServerLevel level, Vec3 at, double spread, int count, int charge) {
        level.sendParticles(spark(charge, 1.2F), at.x, at.y, at.z, count, spread, spread, spread, 0.0D);
        level.sendParticles(dust(charge, 0.8F), at.x, at.y, at.z, count / 2, spread * 1.6D, spread * 1.6D, spread * 1.6D, 0.0D);
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

    /** Higher charge plays lower, per the spec's 高倍率低音脉震. */
    private static void sound(ServerLevel level, Entity at, SoundEvent sound, float volume, float pitch, int charge) {
        level.playSound(null, at.getX(), at.getY(), at.getZ(), sound, SoundSource.PLAYERS, volume,
                Math.max(0.5F, pitch - 0.16F * step(charge)));
    }
}
