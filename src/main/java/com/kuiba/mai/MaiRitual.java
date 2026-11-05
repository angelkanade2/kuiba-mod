package com.kuiba.mai;

import org.jetbrains.annotations.Nullable;

import com.kuiba.Kuiba;
import com.kuiba.core.KuibaServerConfig;
import com.kuiba.mai.payload.RitualProgressPayload;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Shared ritual FSM for the 冲脉丹 (and the later altar). Ruling A:
 * interrupt from moving more than 2 blocks, being hurt, or disconnecting does not
 * consume ingredients, does not raise the gate, and does not add backlash.
 */
public final class MaiRitual {
    public static final double INTERRUPT_DISTANCE_SQR = 4.0D;

    private static boolean warnedAltar;

    private MaiRitual() {}

    public static void tryStart(ServerPlayer player, @Nullable BlockPos altar) {
        if (KuibaServerConfig.RITUAL_MODE.get() == KuibaServerConfig.RitualMode.ALTAR && !warnedAltar) {
            warnedAltar = true;
            Kuiba.LOGGER.warn("ritualMode=altar is not implemented in P0; using the 冲脉丹 FSM");
        }
        MaiData mai = MaiApi.data(player);
        if (mai.ritual() != null) {
            player.displayClientMessage(Component.translatable("message.kuiba.ritual.busy"), true);
            return;
        }
        int target = mai.gate() + 1;
        MaiTiers.Tier tier = MaiTiers.get(target);
        if (tier == null || tier.ritual() == null) {
            player.displayClientMessage(Component.translatable("message.kuiba.ritual.no_next"), true);
            return;
        }
        if (!player.getAbilities().instabuild && !hasIngredients(player, tier.ritual())) {
            player.displayClientMessage(Component.translatable("message.kuiba.ritual.missing"), true);
            return;
        }
        mai.setRitual(new MaiData.RitualState(target, player.level().getGameTime(), altar, player.getX(), player.getY(), player.getZ()));
        player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.7F, 1.0F);
        player.displayClientMessage(Component.translatable("message.kuiba.ritual.start", target), true);
        sendProgress(player, 0.0F, false);
        MaiApi.sync(player);
    }

    public static void cancel(ServerPlayer player, String reason) {
        cancel(player, reason, true);
    }

    /**
     * Ruling A. {@code notify} is false on disconnect, when the client is already gone.
     * Nothing is consumed and backlash is not applied.
     */
    public static void cancel(ServerPlayer player, String reason, boolean notify) {
        MaiData mai = MaiApi.data(player);
        if (mai.ritual() == null) {
            return;
        }
        mai.setRitual(null);
        Kuiba.LOGGER.debug("Ritual cancelled for {} ({})", player.getScoreboardName(), reason);
        if (!notify) {
            return;
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.45F, 1.2F);
        player.displayClientMessage(Component.translatable("message.kuiba.ritual.interrupt"), true);
        sendProgress(player, 0.0F, true);
        MaiApi.sync(player);
    }

    public static void onHurt(ServerPlayer player) {
        if (MaiApi.data(player).ritual() != null) {
            cancel(player, "hurt");
        }
    }

    public static void tick(ServerPlayer player) {
        MaiData mai = MaiApi.data(player);
        MaiData.RitualState state = mai.ritual();
        if (state == null) {
            return;
        }
        double dx = player.getX() - state.x();
        double dy = player.getY() - state.y();
        double dz = player.getZ() - state.z();
        if (dx * dx + dy * dy + dz * dz > INTERRUPT_DISTANCE_SQR) {
            cancel(player, "move");
            return;
        }
        MaiTiers.Tier tier = MaiTiers.get(state.targetGate());
        if (tier == null || tier.ritual() == null) {
            cancel(player, "missing_tier");
            return;
        }
        long elapsed = player.level().getGameTime() - state.startTick();
        int duration = tier.ritual().durationTicks();
        if (elapsed < duration) {
            if (elapsed % 10L == 0L) {
                sendProgress(player, duration == 0 ? 1.0F : (float) elapsed / (float) duration, false);
            }
            return;
        }
        if (!player.getAbilities().instabuild && !hasIngredients(player, tier.ritual())) {
            mai.setRitual(null);
            player.displayClientMessage(Component.translatable("message.kuiba.ritual.missing"), true);
            sendProgress(player, 0.0F, true);
            MaiApi.sync(player);
            return;
        }
        if (!player.getAbilities().instabuild) {
            consume(player, tier.ritual());
        }
        mai.setRitual(null);
        MaiApi.changeGate(player, state.targetGate());
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0D, player.getZ(), 18, 0.4D, 0.5D, 0.4D, 0.02D);
        }
        player.displayClientMessage(Component.translatable("message.kuiba.ritual.success", state.targetGate()), true);
        sendProgress(player, 1.0F, false);
        // 冲脉成功不再放升级音效，改为自动开一次脉门：脉力补满并播放开脉声与身上脉环的连环点亮。
        MaiApi.forceOpenMai(player);
    }

    public static int countMatching(Player player, MaiTiers.IngredientSpec ingredient) {
        return count(player, ingredient);
    }

    private static void sendProgress(ServerPlayer player, float progress, boolean cancelled) {
        player.connection.send(new ClientboundCustomPayloadPacket(new RitualProgressPayload(progress, cancelled)));
    }

    private static boolean hasIngredients(ServerPlayer player, MaiTiers.RitualDef ritual) {
        for (MaiTiers.IngredientSpec ingredient : ritual.ingredients()) {
            if (count(player, ingredient) < ingredient.count()) {
                return false;
            }
        }
        return true;
    }

    private static void consume(ServerPlayer player, MaiTiers.RitualDef ritual) {
        for (MaiTiers.IngredientSpec ingredient : ritual.ingredients()) {
            int remaining = ingredient.count();
            Inventory inventory = player.getInventory();
            for (int slot = 0; slot < inventory.items.size() && remaining > 0; slot++) {
                ItemStack stack = inventory.items.get(slot);
                if (!matches(stack, ingredient)) {
                    continue;
                }
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
        }
    }

    private static int count(Player player, MaiTiers.IngredientSpec ingredient) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (matches(stack, ingredient)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static boolean matches(ItemStack stack, MaiTiers.IngredientSpec ingredient) {
        if (stack.isEmpty()) {
            return false;
        }
        if (ingredient.item() != null) {
            Item item = BuiltInRegistries.ITEM.get(ingredient.item());
            return item != null && stack.is(item);
        }
        if (ingredient.tag() != null) {
            TagKey<Item> tag = TagKey.create(Registries.ITEM, ingredient.tag());
            return stack.is(tag);
        }
        return false;
    }
}
