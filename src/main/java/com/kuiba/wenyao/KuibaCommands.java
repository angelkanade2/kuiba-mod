package com.kuiba.wenyao;

import com.kuiba.item.KuibaComponents;
import com.kuiba.mai.MaiApi;
import com.kuiba.mai.MaiData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;

/** Temporary equip path until the mai screen grows a wenyao slot. */
public final class KuibaCommands {
    private KuibaCommands() {}

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("kuiba")
                .then(Commands.literal("wenyao")
                        .then(Commands.literal("equip").executes(context -> equip(context.getSource().getPlayerOrException())))
                        .then(Commands.literal("clear").executes(context -> clear(context.getSource().getPlayerOrException()))))
                .then(Commands.literal("gate")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("gate", IntegerArgumentType.integer(0, 12))
                                .executes(context -> setGate(
                                        context.getSource().getPlayerOrException(),
                                        IntegerArgumentType.getInteger(context, "gate"))))));
    }

    /** Debug / preview: jump straight to a gate. Fills the pool so the rings show at once. */
    private static int setGate(ServerPlayer player, int gate) {
        MaiApi.changeGate(player, gate);
        float max = MaiApi.getMax(player);
        MaiApi.data(player).setCurrent(max, max);
        MaiApi.sync(player);
        player.displayClientMessage(Component.translatable("message.kuiba.ritual.success", gate), true);
        return 1;
    }

    private static int equip(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty() || KuibaComponents.wenyaoTier(held) == null) {
            player.displayClientMessage(Component.translatable("message.kuiba.wenyao.invalid"), true);
            return 0;
        }
        MaiData data = MaiApi.data(player);
        ItemStack previous = data.wenyaoSlot();
        ItemStack equipped = held.copy();
        equipped.setCount(1);
        held.shrink(1);
        data.setWenyaoSlot(equipped);
        if (!previous.isEmpty() && !player.getInventory().add(previous)) {
            player.drop(previous, false);
        }
        MaiApi.refreshFromWenyao(player);
        player.displayClientMessage(Component.translatable("message.kuiba.wenyao.equipped", equipped.getHoverName()), true);
        return 1;
    }

    private static int clear(ServerPlayer player) {
        MaiData data = MaiApi.data(player);
        ItemStack previous = data.wenyaoSlot();
        if (previous.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.kuiba.wenyao.empty"), true);
            return 0;
        }
        data.clearWenyaoSlot();
        if (!player.getInventory().add(previous)) {
            player.drop(previous, false);
        }
        MaiApi.refreshFromWenyao(player);
        player.displayClientMessage(Component.translatable("message.kuiba.wenyao.cleared"), true);
        return 1;
    }
}
