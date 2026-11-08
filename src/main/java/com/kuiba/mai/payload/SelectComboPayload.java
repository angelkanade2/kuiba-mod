package com.kuiba.mai.payload;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import com.kuiba.mai.MaiCombo;

import net.minecraft.server.level.ServerPlayer;

/**
 * C→S: the 技能圆盘 was released over {@code comboIndex} (or closed on nothing, sent as
 * {@link MaiCombo#NO_COMBO}).
 */
public record SelectComboPayload(int comboIndex) {
    public static void encode(SelectComboPayload msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.comboIndex());
    }

    public static SelectComboPayload decode(FriendlyByteBuf buf) {
        return new SelectComboPayload(buf.readVarInt());
    }

    public static void handle(SelectComboPayload msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                MaiCombo.select(player, msg.comboIndex());
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
