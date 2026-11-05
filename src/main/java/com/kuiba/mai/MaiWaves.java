package com.kuiba.mai;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 脉冲 shockwaves in flight. Each wave is a ring that rolls forward from the caster, widening as it
 * goes, hitting every living thing it passes once, and breaking up when it meets a solid block.
 */
public final class MaiWaves {
    private static final double SPEED = 0.9D;
    private static final double START_RADIUS = 0.8D;
    private static final double END_RADIUS = 2.4D;

    private static final List<Wave> ACTIVE = new ArrayList<>();

    private MaiWaves() {}

    public static void launch(ServerPlayer player, double distance, float damage, double knockback, int charge) {
        Vec3 look = player.getLookAngle();
        Vec3 dir = new Vec3(look.x, look.y * 0.35D, look.z).normalize();
        Vec3 origin = player.position().add(0.0D, 1.0D, 0.0D).add(dir.scale(0.6D));
        ACTIVE.add(new Wave(player.serverLevel(), player.getUUID(), origin, dir, distance, damage, knockback, charge));
    }

    public static void tick(ServerLevel level) {
        Iterator<Wave> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Wave wave = it.next();
            if (wave.level != level) {
                continue;
            }
            if (!wave.step()) {
                it.remove();
            }
        }
    }

    public static void clear() {
        ACTIVE.clear();
    }

    private static final class Wave {
        final ServerLevel level;
        final UUID owner;
        final Vec3 origin;
        final Vec3 dir;
        final double distance;
        final float damage;
        final double knockback;
        final int charge;
        final Set<UUID> hit = new HashSet<>();
        double travelled;

        Wave(ServerLevel level, UUID owner, Vec3 origin, Vec3 dir, double distance, float damage, double knockback, int charge) {
            this.level = level;
            this.owner = owner;
            this.origin = origin;
            this.dir = dir;
            this.distance = distance;
            this.damage = damage;
            this.knockback = knockback;
            this.charge = charge;
        }

        /** Advance one tick; false once the wave is spent. */
        boolean step() {
            this.travelled += SPEED;
            Vec3 front = this.origin.add(this.dir.scale(this.travelled));
            double radius = START_RADIUS + (END_RADIUS - START_RADIUS) * Math.min(1.0D, this.travelled / this.distance);
            BlockPos pos = BlockPos.containing(front);
            if (!this.level.getBlockState(pos).getCollisionShape(this.level, pos).isEmpty()) {
                MaiSkillFx.boltBurst(this.level, front.subtract(this.dir.scale(0.5D)), this.charge, 1.4F);
                return false;
            }
            MaiSkillFx.waveFront(this.level, front, this.dir, radius, this.charge);

            ServerPlayer caster = this.level.getServer().getPlayerList().getPlayer(this.owner);
            AABB box = new AABB(front, front).inflate(radius, radius * 0.8D, radius);
            for (LivingEntity entity : this.level.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && !e.getUUID().equals(this.owner))) {
                if (!this.hit.add(entity.getUUID())) {
                    continue;
                }
                entity.hurt(caster != null ? this.level.damageSources().playerAttack(caster) : this.level.damageSources().magic(), this.damage);
                entity.knockback(this.knockback, -this.dir.x, -this.dir.z);
                entity.hurtMarked = true;
            }
            return this.travelled < this.distance;
        }
    }
}
