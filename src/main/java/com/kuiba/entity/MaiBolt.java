package com.kuiba.entity;

import com.kuiba.mai.MaiSkillFx;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 脉弹: a condensed knot of mai that flies straight, trails cyan motes, and bursts on whatever it hits.
 * 倍率 is synced so the client draws a bigger, deeper-coloured orb.
 */
public class MaiBolt extends ThrowableProjectile {
    private static final EntityDataAccessor<Integer> DATA_CHARGE =
            SynchedEntityData.defineId(MaiBolt.class, EntityDataSerializers.INT);

    private float damage = 5.0F;
    private int lifeTicks = 40;

    public MaiBolt(EntityType<? extends MaiBolt> type, Level level) {
        super(type, level);
    }

    public MaiBolt(Level level, LivingEntity owner, float damage, int charge, int lifeTicks) {
        super(KuibaEntities.MAI_BOLT.get(), owner, level);
        this.damage = damage;
        this.lifeTicks = lifeTicks;
        this.entityData.set(DATA_CHARGE, Mth.clamp(charge, 1, 10));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_CHARGE, 1);
    }

    public int charge() {
        return this.entityData.get(DATA_CHARGE);
    }

    /** Orb radius in blocks, 0.35 up to 0.55; capped so ×10 does not become a giant ball. */
    public float visualSize() {
        return 0.30F + 0.05F * Math.min(charge(), 5);
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0D;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            MaiSkillFx.boltTrail(level(), this);
        } else if (this.tickCount > this.lifeTicks) {
            if (level() instanceof ServerLevel server) {
                MaiSkillFx.boltBurst(server, position(), charge(), 0.6F);
            }
            discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity target = result.getEntity();
        Entity owner = getOwner();
        target.hurt(damageSources().thrown(this, owner == null ? this : owner), this.damage);
        if (target instanceof LivingEntity living) {
            Vec3 push = getDeltaMovement().normalize();
            living.knockback(0.35D + 0.15D * Math.min(charge(), 6), -push.x, -push.z);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level() instanceof ServerLevel server) {
            MaiSkillFx.boltBurst(server, result.getLocation(), charge(), 1.0F);
            server.playSound(null, getX(), getY(), getZ(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS,
                    0.9F, Math.max(0.5F, 1.4F - 0.15F * charge()));
            discard();
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", this.damage);
        tag.putInt("Life", this.lifeTicks);
        tag.putInt("Charge", charge());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.damage = tag.getFloat("Damage");
        this.lifeTicks = tag.getInt("Life");
        this.entityData.set(DATA_CHARGE, Mth.clamp(tag.getInt("Charge"), 1, 10));
    }
}
