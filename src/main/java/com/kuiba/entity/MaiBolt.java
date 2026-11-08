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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 脉弹: condensed mai shaped like the caster's weapon. The volley first circles the caster for
 * {@link #ORBIT_TICKS} ticks — 绕体一周 — then releases along the look direction, locking onto the
 * best-centered living entity within 20 blocks and homing in on it. With an empty hand the bolt
 * falls back to the old mai orb. 倍率 decides how many bolts fly; the client sees it for size and colour.
 */
public class MaiBolt extends ThrowableProjectile {
    private static final EntityDataAccessor<Integer> DATA_CHARGE =
            SynchedEntityData.defineId(MaiBolt.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<ItemStack> DATA_ITEM =
            SynchedEntityData.defineId(MaiBolt.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Boolean> DATA_ORBIT =
            SynchedEntityData.defineId(MaiBolt.class, EntityDataSerializers.BOOLEAN);

    /** Ticks spent circling the caster before release — exactly one full revolution. */
    public static final int ORBIT_TICKS = 12;
    public static final float FLIGHT_SPEED = 1.6F;
    private static final double ORBIT_RADIUS = 1.15D;
    private static final double ORBIT_HEIGHT = 1.2D;
    /** Lock-on cone along the shooter's look direction and its range. */
    private static final double LOCK_RANGE = 20.0D;
    private static final double LOCK_DOT = 0.80D;
    /** Homing turn rate per tick: 1 = snap to the target, 0 = straight. */
    private static final double STEER = 0.35D;

    private float damage = 5.0F;
    private int lifeTicks = 40;
    /** Starting angle on the circle, so the volley spreads evenly. */
    private double orbitPhase;
    private int targetId = -1;
    private boolean launched;

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
    protected void defineSynchedData() {
        this.entityData.define(DATA_CHARGE, 1);
        this.entityData.define(DATA_ITEM, ItemStack.EMPTY);
        this.entityData.define(DATA_ORBIT, false);
    }

    public int charge() {
        return this.entityData.get(DATA_CHARGE);
    }

    /** The weapon this bolt is shaped as; empty means the plain mai orb. */
    public ItemStack visualItem() {
        return this.entityData.get(DATA_ITEM);
    }

    public void setVisualItem(ItemStack stack) {
        ItemStack visual = ItemStack.EMPTY;
        if (!stack.isEmpty()) {
            visual = stack.copy();
            visual.setCount(1);
        }
        this.entityData.set(DATA_ITEM, visual);
    }

    /** Start on the circle around the caster instead of flying off immediately. */
    public void beginOrbit(double phase) {
        this.orbitPhase = phase;
        this.lifeTicks += ORBIT_TICKS;
        this.entityData.set(DATA_ORBIT, true);
    }

    private boolean orbiting() {
        return this.entityData.get(DATA_ORBIT) && this.tickCount < ORBIT_TICKS;
    }

    /** Orb radius in blocks, 0.35 up to 0.55; capped so ×10 does not become a giant ball. */
    public float visualSize() {
        return 0.30F + 0.05F * Math.min(charge(), 5);
    }

    @Override
    protected float getGravity() {
        return 0.0F;
    }

    @Override
    public void tick() {
        if (!level().isClientSide) {
            if (orbiting()) {
                Entity owner = getOwner();
                if (owner == null) {
                    launch();
                } else {
                    double a = orbitPhase + this.tickCount * (Math.PI * 2.0D / ORBIT_TICKS);
                    Vec3 center = owner.position().add(0.0D, ORBIT_HEIGHT, 0.0D);
                    setPos(center.x + Math.cos(a) * ORBIT_RADIUS, center.y, center.z + Math.sin(a) * ORBIT_RADIUS);
                    setDeltaMovement(Vec3.ZERO);
                }
            } else if (!launched) {
                launch();
            } else {
                steer();
            }
        }
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

    /** Release: lock the best target in the look cone, or fly straight along the look. */
    private void launch() {
        this.launched = true;
        Entity owner = getOwner();
        LivingEntity target = owner instanceof LivingEntity shooter ? findLockTarget(shooter) : null;
        Vec3 dir;
        if (target != null) {
            this.targetId = target.getId();
            dir = target.getBoundingBox().getCenter().subtract(position()).normalize();
        } else {
            dir = owner != null ? owner.getLookAngle() : new Vec3(0.0D, 0.0D, 1.0D);
        }
        setDeltaMovement(dir.normalize().scale(FLIGHT_SPEED));
        this.hasImpulse = true;
    }

    /** Bend the flight path toward the locked target while it lives. */
    private void steer() {
        if (this.targetId < 0 || !(level() instanceof ServerLevel server)) {
            return;
        }
        Entity target = server.getEntity(this.targetId);
        if (!(target instanceof LivingEntity living) || !living.isAlive()) {
            this.targetId = -1;
            return;
        }
        Vec3 want = living.getBoundingBox().getCenter().subtract(position());
        if (want.lengthSqr() < 1.0E-6D) {
            return;
        }
        Vec3 velocity = getDeltaMovement();
        double speed = Math.max(velocity.length(), FLIGHT_SPEED * 0.5D);
        setDeltaMovement(velocity.normalize().lerp(want.normalize(), STEER).normalize().scale(speed));
        this.hasImpulse = true;
    }

    /** Nearest living entity inside a ~37° cone along the shooter's look, within {@link #LOCK_RANGE}. */
    private LivingEntity findLockTarget(LivingEntity shooter) {
        Vec3 look = shooter.getLookAngle().normalize();
        Vec3 eye = shooter.getEyePosition();
        LivingEntity best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (LivingEntity entity : level().getEntitiesOfClass(LivingEntity.class,
                shooter.getBoundingBox().inflate(LOCK_RANGE), e -> e != shooter && e.isAlive())) {
            Vec3 to = entity.getBoundingBox().getCenter().subtract(eye);
            double distance = to.length();
            if (distance > LOCK_RANGE || distance < 0.5D) {
                continue;
            }
            double dot = to.normalize().dot(look);
            if (dot < LOCK_DOT) {
                continue;
            }
            // Prefer entities close to the crosshair, then close to the shooter.
            double score = dot * 2.0D - distance / LOCK_RANGE;
            if (score > bestScore) {
                bestScore = score;
                best = entity;
            }
        }
        return best;
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        // Orbiting bolts are a wind-up, not an attack.
        return !orbiting() && super.canHitEntity(target);
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
        tag.putDouble("OrbitPhase", this.orbitPhase);
        tag.putInt("Target", this.targetId);
        tag.putBoolean("Launched", this.launched);
        tag.putBoolean("Orbit", this.entityData.get(DATA_ORBIT));
        ItemStack stack = visualItem();
        if (!stack.isEmpty()) {
            CompoundTag saved = new CompoundTag();
            stack.save(saved);
            tag.put("VisualItem", saved);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.damage = tag.getFloat("Damage");
        this.lifeTicks = tag.getInt("Life");
        this.entityData.set(DATA_CHARGE, Mth.clamp(tag.getInt("Charge"), 1, 10));
        this.orbitPhase = tag.getDouble("OrbitPhase");
        this.targetId = tag.getInt("Target");
        this.launched = tag.getBoolean("Launched");
        this.entityData.set(DATA_ORBIT, tag.getBoolean("Orbit"));
        if (tag.contains("VisualItem")) {
            this.entityData.set(DATA_ITEM, ItemStack.of(tag.getCompound("VisualItem")));
        }
    }
}
