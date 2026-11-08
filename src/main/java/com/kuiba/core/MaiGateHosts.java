package com.kuiba.core;

import com.kuiba.Kuiba;
import com.kuiba.core.KuibaIds;
import com.kuiba.core.KuibaNetwork;
import com.kuiba.item.KuibaItems;
import com.kuiba.mai.payload.MaiFxPayload;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 特殊怪. Each mob rolls a fixed 脉门 count when it spawns — 1–4 for zombies/skeletons, 1–6 for
 * pigs, 6–12 for End endermen — and keeps it for life. It pulses the same 开脉 animation a player gets
 * (rings on the body plus the {@code open_mai_gate_xN} sound) every
 * {@code gateHostOpenIntervalTicks}, which is purely visual; the damage bonus is always on and scales
 * with the fixed count. Drops also scale with it: 脉碎片, 脉晶 or 脉核.
 */
public final class MaiGateHosts {
    /** Attribute modifier id for the per-gate health bonus. Shared with the client renderer. */
    public static final java.util.UUID HEALTH_MODIFIER_ID = KuibaIds.GATE_HOST_HEALTH;
    /** How far the 开脉 pulse packet is broadcast, in blocks. */
    private static final double PULSE_RADIUS = 64.0D;

    private MaiGateHosts() {}

    /** Roll the fixed gate count once, when the mob first joins the level. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }
        // Already a host (loaded from disk, or re-entering the level): make sure the health buff is there.
        GateHostData existing = MaiAttachments.host(mob);
        if (existing.isHost()) {
            ensureHealth(mob, existing.gates());
            return;
        }
        if (event.loadedFromDisk()) {
            return;
        }
        // 末影人只在末地开脉门，6–12 门.
        if (mob instanceof EnderMan) {
            if (mob.level().dimension() != Level.END) {
                return;
            }
            GateHostData data = MaiAttachments.host(mob);
            int low = Math.max(1, KuibaServerConfig.ENDERMAN_MIN_GATES.get());
            int high = Math.max(low, KuibaServerConfig.ENDERMAN_MAX_GATES.get());
            makeHost(mob, data, GateHostData.Kind.CORE, low, high);
            return;
        }
        GateHostData.Kind kind = kindFor(mob);
        if (kind == GateHostData.Kind.NONE) {
            return;
        }
        RandomSource random = mob.getRandom();
        if (random.nextDouble() >= KuibaServerConfig.GATE_HOST_CHANCE.get()) {
            return;
        }
        // 僵尸 / 骷髅 1–4 门，猪 1–6 门.
        GateHostData data = MaiAttachments.host(mob);
        makeHost(mob, data, kind, 1, kind == GateHostData.Kind.SHARD ? 4 : 6);
    }

    /** Used by the spawn eggs: spawn a mob already marked as a 特殊怪 with a rolled gate count. */
    public static void forceHost(Mob mob, GateHostData.Kind kind, int minGates, int maxGates) {
        if (kind == GateHostData.Kind.NONE || mob.level().isClientSide()) {
            return;
        }
        makeHost(mob, MaiAttachments.host(mob), kind, minGates, maxGates);
    }

    private static void makeHost(Mob mob, GateHostData data, GateHostData.Kind kind, int minGates, int maxGates) {
        int low = Math.max(1, Math.min(12, minGates));
        int high = Math.max(low, Math.min(12, maxGates));
        data.setKind(kind);
        data.setGates(low + mob.getRandom().nextInt(high - low + 1));
        // Pulse right away, then on the timer.
        data.setNextPulseTick(mob.level().getGameTime());
        applyHealth(mob, data.gates(), true);
        Kuiba.LOGGER.debug("特殊怪: {} -> {} 门, kind={}", mob.getType(), data.gates(), kind);
    }

    /** Adds the health buff only if it is missing, so a damaged mob does not heal when a chunk reloads. */
    private static void ensureHealth(Mob mob, int gates) {
        AttributeInstance health = mob.getAttribute(Attributes.MAX_HEALTH);
        if (health == null || health.getModifier(HEALTH_MODIFIER_ID) != null) {
            return;
        }
        applyHealth(mob, gates, false);
    }

    /**
     * 每门 +50% 血量. Uses an attribute modifier so it survives a save/load with the mob; the mob is then
     * healed to its new maximum so a fresh host does not spawn at full-old-health.
     */
    private static void applyHealth(Mob mob, int gates, boolean heal) {
        if (gates <= 0) {
            return;
        }
        AttributeInstance health = mob.getAttribute(Attributes.MAX_HEALTH);
        if (health == null) {
            return;
        }
        health.removeModifier(HEALTH_MODIFIER_ID);
        double perGate = KuibaServerConfig.GATE_HOST_HEALTH_PER_GATE.get();
        if (perGate <= 0.0D) {
            return;
        }
        health.addPermanentModifier(new AttributeModifier(
                HEALTH_MODIFIER_ID, "kuiba:gate_host_health", perGate * gates, AttributeModifier.Operation.MULTIPLY_BASE));
        if (heal) {
            mob.setHealth(mob.getMaxHealth());
        }
    }

    /** Fires the visual 开脉 pulse on the timer. Nothing about the gate count changes. */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }
        if (!isCandidate(mob) || mob.level().isClientSide()) {
            return;
        }
        // 1 Hz per mob, spread across the tick range by tickCount.
        if (mob.tickCount % 20 != 0 || !MaiAttachments.isHost(mob)) {
            return;
        }
        GateHostData data = MaiAttachments.host(mob);
        if (!data.isHost()) {
            return;
        }
        long now = mob.level().getGameTime();
        if (now < data.nextPulseTick()) {
            return;
        }
        data.setNextPulseTick(now + KuibaServerConfig.GATE_HOST_OPEN_INTERVAL.get());
        pulse(mob, data.gates());
    }

    /**
     * Sends the same one-shot cue players get from 开脉: body rings, burst and the gate-count clip.
     * Broadcasts by radius rather than by entity tracking — {@code sendToPlayersTrackingEntity} depends on
     * the mob having a live {@code ChunkMap} tracker entry and fails silently otherwise.
     */
    public static void pulse(Mob mob, int gates) {
        if (gates <= 0) {
            return;
        }
        if (!(mob.level() instanceof ServerLevel level)) {
            return;
        }
        KuibaNetwork.sendNear(level, mob.getX(), mob.getY(), mob.getZ(), PULSE_RADIUS,
                new MaiFxPayload(mob.getId(), MaiFxPayload.KIND_OPEN, gates));
        Kuiba.LOGGER.debug("开脉: {} pulses {} gates (id {})", mob.getType(), gates, mob.getId());
    }

    /** Replay the pulse for one player, used when they start tracking the mob. */
    public static void pulseToOne(net.minecraft.server.level.ServerPlayer viewer, Mob mob, int gates) {
        if (gates <= 0) {
            return;
        }
        KuibaNetwork.sendToPlayer(viewer, new MaiFxPayload(mob.getId(), MaiFxPayload.KIND_OPEN, gates));
    }

    /** Every 脉门 raises the mob's outgoing damage; the count is fixed, so this is a constant buff. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDealDamage(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Mob attacker)) {
            return;
        }
        if (!MaiAttachments.isHost(attacker)) {
            return;
        }
        GateHostData data = MaiAttachments.host(attacker);
        if (!data.isHost()) {
            return;
        }
        float perGate = KuibaServerConfig.GATE_HOST_DAMAGE_PER_GATE.get().floatValue();
        event.setAmount(event.getAmount() * (1.0F + perGate * data.gates()));
    }

    /** More 脉门 means a bigger drop, and only player kills count. */
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }
        if (!MaiAttachments.isHost(mob)) {
            return;
        }
        GateHostData data = MaiAttachments.host(mob);
        if (!data.isHost()) {
            return;
        }
        if (!KuibaDrops.killedByPlayer(event.getSource())) {
            return;
        }
        Item item = switch (data.kind()) {
            case SHARD -> KuibaItems.MAI_SHARD.get();
            case CRYSTAL -> KuibaItems.MAI_CRYSTAL.get();
            default -> KuibaItems.MAI_CORE.get();
        };
        int count = switch (data.kind()) {
            case SHARD -> data.gates();
            case CRYSTAL -> Math.max(1, (data.gates() + 1) / 2);
            default -> data.gates();
        };
        event.getDrops().add(new ItemEntity(
                mob.level(),
                mob.getX(),
                mob.getY(0.5D),
                mob.getZ(),
                new ItemStack(item, count)));
    }

    private static boolean isCandidate(Mob mob) {
        return mob instanceof Zombie
                || mob instanceof AbstractSkeleton
                || mob instanceof Piglin
                || mob instanceof EnderMan;
    }

    /** Pigs first: {@link ZombifiedPiglin} extends {@link Zombie}, so the order matters. */
    private static GateHostData.Kind kindFor(Mob mob) {
        if (mob instanceof ZombifiedPiglin || mob instanceof Piglin) {
            return GateHostData.Kind.CRYSTAL;
        }
        if (mob instanceof Zombie || mob instanceof AbstractSkeleton) {
            return GateHostData.Kind.SHARD;
        }
        return GateHostData.Kind.NONE;
    }
}
