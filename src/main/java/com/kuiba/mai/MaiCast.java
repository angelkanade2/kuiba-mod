package com.kuiba.mai;

import java.util.Locale;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.kuiba.entity.MaiBolt;
import com.kuiba.mai.event.MaiCastEvent;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;

/**
 * Single cast entry for hotbar slots and combo steps.
 * 倍率 is ×1 / ×2 / ×3 / ×5 / ×10: cost scales with the multiplier, effect scales sub-linearly
 * ({@code 1 / 1.75 / 2.75 / 4 / 5.5}). No gate requirement; only the pool matters.
 * The cast happens only when {@link MaiApi#tryConsume} can pay. No overdraw.
 */
public final class MaiCast {
    /** Ticks of fall-damage grace granted by 脉闪, counted from the dash. */
    public static final int FALL_GRACE_TICKS = 60;
    /** How much of the look pitch survives when a direction key decides the heading, 0 = flat, 1 = full. */
    private static final double DIRECTIONAL_PITCH = 0.5D;

    public static final Set<String> KNOWN = Set.of(
            "dash",
            "buff",
            "aoe_zone",
            "melee",
            "projectile",
            "wave",
            "heal",
            "self_buff",
            "shield",
            "debuff",
            "aoe");

    private MaiCast() {}

    public static boolean isKnown(String cast) {
        return cast != null && KNOWN.contains(cast.toLowerCase(Locale.ROOT));
    }

    public static boolean execute(ServerPlayer player, ResourceLocation skillId, int charge) {
        return execute(player, skillId, charge, -1);
    }

    static boolean execute(ServerPlayer player, ResourceLocation skillId, int charge, int slot) {
        if (skillId == null) {
            return false;
        }
        // 倍率 is no longer gated by 门阶, only by what the player can pay.
        int level = MaiCombo.clampCharge(charge);
        MaiSkill skill = MaiSkills.get(skillId);
        MaiData mai = MaiApi.data(player);
        if (skill == null || !mai.isUnlocked(skillId) || !MaiApi.meetsGate(player, skill.unlockGate())) {
            return false;
        }
        // Skill casting has no cooldown any more: the mai pool is the only cost. 器艺 keeps its own timer.
        MaiCastEvent event = new MaiCastEvent(player, skill, slot);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            return false;
        }
        float cost = skill.cost() * MaiCombo.costMultiplier(level);
        if (MaiApi.isMaiClosed(player)) {
            player.displayClientMessage(Component.translatable("message.kuiba.open_mai.closed"), true);
            return false;
        }
        if (!MaiApi.tryConsume(player, cost, skillId)) {
            player.displayClientMessage(Component.translatable("message.kuiba.cast.no_points"), true);
            return false;
        }
        apply(player, skill, MaiCombo.effectMultiplier(level), level);
        player.displayClientMessage(Component.translatable("message.kuiba.cast", Component.translatable(skill.nameKey())), true);
        MaiApi.sync(player);
        MaiApi.sendFx(player, com.kuiba.mai.payload.MaiFxPayload.KIND_CAST, level);
        return true;
    }

    private static void apply(ServerPlayer player, MaiSkill skill, float effectScale, int charge) {
        String cast = skill.cast().toLowerCase(Locale.ROOT);
        switch (cast) {
            case "dash" -> dash(player, skill, effectScale, charge);
            case "buff" -> {
                buff(player, skill, effectScale);
                MaiSkillFx.guard(player, charge);
            }
            case "aoe_zone", "aoe" -> aoeZone(player, skill, effectScale, charge);
            case "melee" -> {
                LivingEntity target = hurtNearest(player, skill, skill.power() * effectScale);
                if (target != null) {
                    target.knockback(0.4D * effectScale, player.getX() - target.getX(), player.getZ() - target.getZ());
                }
                MaiSkillFx.strike(player, charge, target);
            }
            case "projectile" -> projectile(player, skill, effectScale, charge);
            case "wave" -> wave(player, skill, effectScale, charge);
            case "heal" -> {
                player.heal(Math.max(1.0F, skill.power() * effectScale));
                MaiSkillFx.mend(player, charge);
            }
            case "self_buff" -> {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, scaledTicks(skill, effectScale), 0));
                MaiSkillFx.edge(player, charge);
            }
            case "shield" -> {
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, scaledTicks(skill, effectScale), Math.max(0, (int) (skill.power() * effectScale) - 1)));
                MaiSkillFx.barrier(player, charge);
            }
            case "debuff" -> debuff(player, skill, effectScale, charge);
            default -> {
                // Unknown casts are dropped at load time.
            }
        }
    }

    private static int scaledTicks(MaiSkill skill, float effectScale) {
        int base = Math.max(20, skill.durationTicks());
        return Math.max(20, Math.round(base * effectScale));
    }

    /**
     * 脉闪: dash along the held movement keys (W/A/S/D, diagonals included), or along the look direction
     * when no movement key is held. With a direction key the dash is mostly horizontal but still keeps
     * {@link #DIRECTIONAL_PITCH} of the look pitch, so looking up gives a little lift. The input is read
     * live from the attachment on every cast, so a combo whose steps happen seconds apart follows whatever
     * the player is holding at that moment. Fall damage is ignored for a short window afterwards.
     */
    private static void dash(ServerPlayer player, MaiSkill skill, float effectScale, int charge) {
        MaiData mai = MaiApi.data(player);
        Vec3 look = player.getLookAngle();
        Vec3 heading = inputHeading(player, mai.inputForward(), mai.inputStrafe());
        double speed = 0.6D + skill.power() * 0.35D * effectScale;

        double vx;
        double vz;
        double vy;
        if (heading.lengthSqr() > 1.0E-6D) {
            vx = heading.x * speed;
            vz = heading.z * speed;
            vy = Math.max(0.12D, look.y * DIRECTIONAL_PITCH * speed);
        } else {
            vx = look.x * speed;
            vz = look.z * speed;
            vy = Math.max(0.12D, look.y * speed);
        }
        player.setDeltaMovement(vx, vy, vz);
        player.hurtMarked = true;
        player.fallDistance = 0.0F;
        mai.setNoFallUntil(player.level().getGameTime() + FALL_GRACE_TICKS);
        // The FX trail follows the actual travel vector, so a pitched dash streaks upward with you.
        MaiSkillFx.dash(player, charge, speed * 4.0D, new Vec3(vx, vy, vz));
    }

    /**
     * Horizontal unit vector for the held keys, using vanilla's own input-to-world rotation so that
     * W/A/S/D map to the same directions the player is actually walking. Returns {@link Vec3#ZERO} when
     * no movement key is held. {@code forward}: W +1 / S -1. {@code strafe}: A +1 (left) / D -1 (right).
     */
    private static Vec3 inputHeading(ServerPlayer player, int forward, int strafe) {
        if (forward == 0 && strafe == 0) {
            return Vec3.ZERO;
        }
        double x = strafe;
        double z = forward;
        double length = Math.sqrt(x * x + z * z);
        x /= length;
        z /= length;
        float facing = player.getYRot() * ((float) Math.PI / 180.0F);
        float sin = (float) Math.sin(facing);
        float cos = (float) Math.cos(facing);
        return new Vec3(x * cos - z * sin, 0.0D, z * cos + x * sin);
    }

    private static void buff(ServerPlayer player, MaiSkill skill, float effectScale) {
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, scaledTicks(skill, effectScale), 0));
    }

    private static void aoeZone(ServerPlayer player, MaiSkill skill, float effectScale, int charge) {
        float range = skill.range() <= 0.0F ? 4.0F : skill.range();
        float damage = skill.power() * effectScale;
        AABB box = player.getBoundingBox().inflate(range);
        for (LivingEntity entity : player.serverLevel().getEntitiesOfClass(LivingEntity.class, box, entity -> entity != player && entity.isAlive())) {
            entity.hurt(player.damageSources().playerAttack(player), damage);
        }
        MaiSkillFx.burst(player, charge, range);
    }

    /**
     * 脉弹: 御器齐射. The volley — one bolt per 倍率 step (×3 fires three, ×10 fires ten) — circles the
     * caster once, then releases along the look direction and locks onto the best-centered entity
     * within 20 blocks. Holding a weapon shapes the bolts after it and its attack damage feeds the
     * hit; empty hand falls back to the plain orb. Total damage is split by √count so a volley is
     * wider, not ten times as lethal.
     */
    private static void projectile(ServerPlayer player, MaiSkill skill, float effectScale, int charge) {
        ServerLevel level = player.serverLevel();
        int count = MaiCombo.clampCharge(charge);
        float range = skill.range() <= 0.0F ? 16.0F : skill.range();
        ItemStack held = player.getMainHandItem();
        float total = skill.power() * effectScale + (float) (weaponDamage(held) * 0.8D);
        float perBolt = (float) (total / Math.sqrt(count));
        int life = Math.round(range / MaiBolt.FLIGHT_SPEED);
        for (int i = 0; i < count; i++) {
            MaiBolt bolt = new MaiBolt(level, player, perBolt, charge, life);
            bolt.setVisualItem(held);
            bolt.setPos(player.getX(), player.getEyeY() - 0.2D, player.getZ());
            bolt.beginOrbit(Math.PI * 2.0D * i / count);
            level.addFreshEntity(bolt);
        }
        MaiSkillFx.boltCast(player, charge);
        // The volley whistles loose as the circle releases: one drawn-out 咻.
        MaiFxSequencer.schedule(level, MaiBolt.ORBIT_TICKS + 1, frame -> {
            if (frame == MaiBolt.ORBIT_TICKS) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.9F, 1.1F);
            }
        });
    }

    /** Sum of the held item's mainhand attack-damage modifiers; 0 for an empty hand or a non-weapon. */
    private static double weaponDamage(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0.0D;
        }
        double bonus = 0.0D;
        Multimap<Attribute, AttributeModifier> modifiers = stack.getAttributeModifiers(EquipmentSlot.MAINHAND);
        for (AttributeModifier modifier : modifiers.get(Attributes.ATTACK_DAMAGE)) {
            if (modifier.getOperation() == AttributeModifier.Operation.ADDITION) {
                bonus += modifier.getAmount();
            }
        }
        return Math.max(0.0D, bonus);
    }

    /** 脉冲: a forward shockwave; {@code range} is how far it rolls. */
    private static void wave(ServerPlayer player, MaiSkill skill, float effectScale, int charge) {
        double distance = (skill.range() <= 0.0F ? 10.0D : skill.range()) * (1.0D + 0.25D * (charge - 1));
        MaiWaves.launch(player, distance, skill.power() * effectScale, 0.6D + 0.3D * effectScale, charge);
        MaiSkillFx.waveCast(player, charge);
    }

    private static void debuff(ServerPlayer player, MaiSkill skill, float effectScale, int charge) {
        LivingEntity target = nearest(player, skill.range() <= 0.0F ? 6.0F : skill.range());
        if (target != null) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, scaledTicks(skill, effectScale), 1));
        }
        MaiSkillFx.bind(player, charge, target);
    }

    private static @Nullable LivingEntity hurtNearest(ServerPlayer player, MaiSkill skill, float damage) {
        LivingEntity target = nearest(player, skill.range() <= 0.0F ? 3.0F : skill.range());
        if (target != null && damage > 0.0F) {
            target.hurt(player.damageSources().playerAttack(player), damage);
        }
        return target;
    }

    private static LivingEntity nearest(ServerPlayer player, float range) {
        AABB box = player.getBoundingBox().inflate(range);
        LivingEntity nearest = null;
        double best = Double.MAX_VALUE;
        for (LivingEntity entity : player.serverLevel().getEntitiesOfClass(LivingEntity.class, box, entity -> entity != player && entity.isAlive())) {
            double distance = entity.distanceToSqr(player);
            if (distance < best) {
                best = distance;
                nearest = entity;
            }
        }
        return nearest;
    }
}
