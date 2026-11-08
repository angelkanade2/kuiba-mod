package com.kuiba.item;

import java.util.List;

import com.kuiba.mai.MaiRitual;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** P0 ritual starter. Same FSM as a future altar. Sneak-use cancels. */
public class ChongMaiDanItem extends Item {
    public ChongMaiDanItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        if (player instanceof ServerPlayer server) {
            if (player.isShiftKeyDown()) {
                MaiRitual.cancel(server, "item");
            } else {
                MaiRitual.tryStart(server, null);
            }
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @org.jetbrains.annotations.Nullable net.minecraft.world.level.Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.kuiba.chong_mai_dan.tip").withStyle(ChatFormatting.GRAY));
    }
}
