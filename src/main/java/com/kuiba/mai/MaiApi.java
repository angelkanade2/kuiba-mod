package com.kuiba.mai;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.kuiba.Kuiba;
import com.kuiba.core.KuibaServerConfig;
import com.kuiba.core.MaiAttachments;
import com.kuiba.item.KuibaItems;
import com.kuiba.mai.advancement.GateReachedTrigger;
import com.kuiba.mai.event.MaiGateChangedEvent;
import com.kuiba.mai.payload.MaiAuraPayload;
import com.kuiba.mai.payload.MaiFxPayload;
import com.kuiba.mai.payload.SyncMaiPayload;
import com.kuiba.wenyao.WenyaoApi;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

public final class MaiApi {
    public static final float EMPTY_EPSILON = 1.0E-3F;
    public static final int OPEN_MAI_COOLDOWN_TICKS = 20;
    /** Unused drain: full bar empties in ~90 s (1/90 of max per second). */
    public static final float DECAY_FRACTION_PER_SECOND = 1.0F / 90.0F;
    /** Body 脉门 FX after 开脉 lasts this long, then dissipates. Mai points keep decaying slowly. */
    public static final int OPEN_MAI_VISUAL_TICKS = 40;

    private MaiApi() {}

    public static MaiData data(Player player) {
        return player.getData(MaiAttachments.MAI.get());
    }

    public static int getGate(Player player) {
        return data(player).gate();
    }

    public static float getPoints(Player player) {
        return data(player).current();
    }

    /** {@code tiers[gate].max_points + WenyaoApi.bonusMax}. Bonus is 0 until wenyao exists. */
    public static float getMax(Player player) {
        return data(player).tierMax() + WenyaoApi.bonusMax(player);
    }

    /** Default 1. Passed through to {@link WenyaoApi#costMultiplier}. */
    public static float getCostMultiplier(Player player, @Nullable ResourceLocation skillOrArt) {
        float multiplier = WenyaoApi.costMultiplier(player, skillOrArt);
        return multiplier > 0.0F ? multiplier : 1.0F;
    }

    /**
     * Scale applied to per-second decay ({@code max / 90}).
     * Two weave pieces return 0.90 so the pool drains slower.
     * {@link WenyaoApi#oocRegenMultiplier} is the same value; regen was removed.
     */
    public static float getDecayMultiplier(Player player) {
        float multiplier = WenyaoApi.decayMultiplier(player);
        return multiplier > 0.0F ? multiplier : 1.0F;
    }

    /** Use {@link #getDecayMultiplier}. Kept so older callers do not restore regen. */
    @Deprecated
    public static float getOocRegenMultiplier(Player player) {
        return getDecayMultiplier(player);
    }

    public static boolean isMaiClosed(Player player) {
        return !player.getAbilities().instabuild && data(player).current() <= EMPTY_EPSILON;
    }

    public static boolean meetsGate(Player player, int required) {
        return getGate(player) >= required;
    }

    /**
     * Spend points. Creative ({@code instabuild}) pays nothing.
     * Fails when the pool is short. Overdraw is not implemented.
     */
    public static boolean tryConsume(Player player, float cost) {
        return tryConsume(player, cost, null);
    }

    /**
     * Spend {@code cost * getCostMultiplier(player, skillOrArt)}.
     * Creative pays nothing. Fails when the pool is short. No overdraw.
     */
    public static boolean tryConsume(Player player, float cost, @Nullable ResourceLocation skillOrArt) {
        float paid = cost * getCostMultiplier(player, skillOrArt);
        if (paid <= 0.0F) {
            return true;
        }
        if (player.getAbilities().instabuild) {
            return true;
        }
        if (!(player instanceof ServerPlayer server)) {
            return false;
        }
        MaiData mai = data(server);
        if (mai.current() <= EMPTY_EPSILON || mai.current() + EMPTY_EPSILON < paid) {
            return false;
        }
        mai.setCurrent(mai.current() - paid, getMax(server));
        sync(server);
        return true;
    }

    /**
     * Fill the mai pool to max. Not the gate ritual. Costs 2 hunger unless creative or spectator.
     */
    public static boolean tryOpenMai(ServerPlayer player) {
        MaiData mai = data(player);
        if (mai.gate() < 1) {
            player.displayClientMessage(Component.translatable("message.kuiba.open_mai.gate"), true);
            return false;
        }
        if (mai.ritual() != null) {
            player.displayClientMessage(Component.translatable("message.kuiba.open_mai.busy"), true);
            return false;
        }
        if (mai.openMaiCooldown() > 0) {
            player.displayClientMessage(Component.translatable("message.kuiba.open_mai.cooldown"), true);
            return false;
        }
        boolean skipFood = player.isSpectator() || player.getAbilities().instabuild;
        if (!skipFood && player.getFoodData().getFoodLevel() < 2) {
            player.displayClientMessage(Component.translatable("message.kuiba.open_mai.hungry"), true);
            return false;
        }
        if (!skipFood) {
            player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - 2);
        }
        float max = getMax(player);
        mai.setCurrent(max, max);
        mai.setOpenMaiCooldown(OPEN_MAI_COOLDOWN_TICKS);
        MaiRegen.markOpenVisual(player);
        sync(player);
        sendFx(player, MaiFxPayload.KIND_OPEN, mai.gate());
        player.displayClientMessage(Component.translatable("message.kuiba.open_mai.done"), true);
        return true;
    }

    /**
     * Fill the pool and play the 开脉 effect for free: no hunger, no cooldown, no ritual checks.
     * Used by the 冲脉 success path, which replaces the old level-up sound with an automatic 开脉.
     */
    public static void forceOpenMai(ServerPlayer player) {
        MaiData mai = data(player);
        float max = getMax(player);
        mai.setCurrent(max, max);
        MaiRegen.markOpenVisual(player);
        sync(player);
        sendFx(player, MaiFxPayload.KIND_OPEN, mai.gate());
    }

    /** One-shot ring effect, shown to the player and everyone who can see them. */
    public static void sendFx(ServerPlayer player, int kind, int gate) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new MaiFxPayload(player.getId(), kind, gate));
    }

    /** Aura packet for one viewer. Used when a player starts tracking another. */
    public static void sendAuraTo(ServerPlayer viewer, ServerPlayer subject) {
        MaiData mai = data(subject);
        float max = Math.max(1.0F, getMax(subject));
        int level = MaiRegen.isOpenVisualActive(subject) ? MaiAuraPayload.quantize(mai.current() / max) : 0;
        PacketDistributor.sendToPlayer(viewer, new MaiAuraPayload(subject.getId(), mai.gate(), level));
    }

    private static void broadcastAura(ServerPlayer player, MaiData mai, float max) {
        int level = MaiRegen.isOpenVisualActive(player) ? MaiAuraPayload.quantize(mai.current() / Math.max(1.0F, max)) : 0;
        if (mai.updateAura(mai.gate(), level)) {
            PacketDistributor.sendToPlayersTrackingEntity(player, new MaiAuraPayload(player.getId(), mai.gate(), level));
        }
    }

    /** Force trackers to drop body glow when the open visual window ends. */
    public static void syncOpenVisualClosed(ServerPlayer player) {
        MaiData mai = data(player);
        if (mai.updateAura(mai.gate(), 0)) {
            PacketDistributor.sendToPlayersTrackingEntity(player, new MaiAuraPayload(player.getId(), mai.gate(), 0));
        }
    }

    public static void addBacklash(Player player, int stacks) {
        Kuiba.LOGGER.debug("addBacklash stub ignored ({} stacks) for {}", stacks, player.getScoreboardName());
    }

    public static void ensureInitialized(ServerPlayer player) {
        MaiData mai = data(player);
        if (!mai.initialized()) {
            mai.markInitialized();
            float max = getMax(player);
            mai.setCurrent(max, max);
            grantStartingCrystals(player);
        }
        MaiSkills.unlockFor(mai);
        mai.trimEquipped(MaiTiers.slots(mai.gate()));
        applyModifiers(player);
    }

    /**
     * 开局礼包: enough 脉晶 to run the gate 1 ritual. Granted once, tracked by the persisted
     * {@code initialized} flag, so dying or relogging never re-grants it.
     */
    private static void grantStartingCrystals(ServerPlayer player) {
        int count = KuibaServerConfig.STARTING_CRYSTALS.get();
        if (count <= 0) {
            return;
        }
        ItemStack stack = new ItemStack(KuibaItems.MAI_CRYSTAL.get(), count);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        player.displayClientMessage(Component.translatable("message.kuiba.start.crystals", count), true);
    }

    public static void changeGate(ServerPlayer player, int newGate) {
        MaiData mai = data(player);
        int from = mai.gate();
        mai.setGate(newGate);
        MaiSkills.unlockFor(mai);
        mai.trimEquipped(MaiTiers.slots(mai.gate()));
        float max = getMax(player);
        mai.setCurrent(Math.min(mai.current(), max), max);
        applyModifiers(player);
        NeoForge.EVENT_BUS.post(new MaiGateChangedEvent(player, from, mai.gate()));
        if (mai.gate() > from) {
            GateReachedTrigger.GATE_REACHED.get().trigger(player, mai.gate());
        }
        sync(player);
        if (mai.gate() > from) {
            sendFx(player, MaiFxPayload.KIND_UNLOCK, mai.gate());
        }
    }

    public static void applyModifiers(Player player) {
        for (MaiTiers.Tier tier : MaiTiers.all()) {
            for (MaiTiers.ModifierSpec modifier : tier.modifiers()) {
                AttributeInstance instance = player.getAttribute(modifier.attribute());
                if (instance != null) {
                    instance.removeModifier(modifier.modifierId());
                }
            }
        }
        MaiTiers.Tier current = MaiTiers.get(data(player).gate());
        if (current != null) {
            for (MaiTiers.ModifierSpec modifier : current.modifiers()) {
                AttributeInstance instance = player.getAttribute(modifier.attribute());
                if (instance == null) {
                    continue;
                }
                instance.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                        modifier.modifierId(),
                        modifier.amount(),
                        modifier.operation()));
            }
        }
        WenyaoApi.applyMeleeBonus(player);
    }

    public static void refreshFromWenyao(ServerPlayer player) {
        float max = getMax(player);
        data(player).setCurrent(Math.min(data(player).current(), max), max);
        applyModifiers(player);
        sync(player);
    }

    public static boolean tryCast(ServerPlayer player, int slot) {
        if (slot < 0 || slot >= MaiData.EQUIPPED_SLOTS) {
            return false;
        }
        MaiData mai = data(player);
        if (slot >= MaiTiers.slots(mai.gate())) {
            return false;
        }
        ResourceLocation skillId = mai.equipped(slot);
        if (skillId == null) {
            return false;
        }
        MaiSkill skill = MaiSkills.get(skillId);
        if (skill == null || !mai.isUnlocked(skillId) || !meetsGate(player, skill.unlockGate())) {
            return false;
        }
        return MaiCast.execute(player, skillId, 1, slot);
    }

    public static boolean tryEquip(ServerPlayer player, int slot, ResourceLocation skillId) {
        if (slot < 0 || slot >= MaiData.EQUIPPED_SLOTS) {
            return false;
        }
        MaiData mai = data(player);
        if (skillId == null) {
            mai.setEquipped(slot, null);
            sync(player);
            return true;
        }
        if (slot >= MaiTiers.slots(mai.gate())) {
            player.displayClientMessage(Component.translatable("message.kuiba.equip.locked"), true);
            return false;
        }
        MaiSkill skill = MaiSkills.get(skillId);
        if (skill == null || !mai.isUnlocked(skillId) || !meetsGate(player, skill.unlockGate())) {
            player.displayClientMessage(Component.translatable("message.kuiba.equip.locked_skill"), true);
            return false;
        }
        for (int i = 0; i < MaiData.EQUIPPED_SLOTS; i++) {
            if (skillId.equals(mai.equipped(i))) {
                mai.setEquipped(i, null);
            }
        }
        mai.setEquipped(slot, skillId);
        sync(player);
        return true;
    }

    public static void sync(ServerPlayer player) {
        MaiData mai = data(player);
        Map<ResourceLocation, Integer> cooldowns = new LinkedHashMap<>();
        for (Map.Entry<ResourceLocation, Integer> entry : mai.cooldownsView().entrySet()) {
            if (entry.getValue() > 0) {
                cooldowns.put(entry.getKey(), entry.getValue());
            }
        }
        ResourceLocation[] equipped = new ResourceLocation[MaiData.EQUIPPED_SLOTS];
        System.arraycopy(mai.equipped(), 0, equipped, 0, MaiData.EQUIPPED_SLOTS);
        float max = getMax(player);
        MaiCombo.Preset[] combos = new MaiCombo.Preset[MaiCombo.COMBO_COUNT];
        for (int i = 0; i < MaiCombo.COMBO_COUNT; i++) {
            combos[i] = mai.combo(i);
        }
        SyncMaiPayload payload = new SyncMaiPayload(
                mai.gate(),
                mai.current(),
                max,
                mai.backlashStacks(),
                equipped,
                cooldowns,
                Set.copyOf(mai.unlocked()),
                combos,
                mai.selectedCombo());
        player.connection.send(new ClientboundCustomPayloadPacket(payload));
        broadcastAura(player, mai, max);
    }

    public static void onResourcesReloaded() {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            MaiData mai = data(player);
            float max = getMax(player);
            mai.setCurrent(Math.min(mai.current(), max), max);
            MaiSkills.unlockFor(mai);
            mai.trimEquipped(MaiTiers.slots(mai.gate()));
            applyModifiers(player);
            sync(player);
        }
    }
}
