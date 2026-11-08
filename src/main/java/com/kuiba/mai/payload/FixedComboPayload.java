package com.kuiba.mai.payload;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import com.kuiba.mai.MaiApi;
import com.kuiba.mai.MaiData;

import net.minecraft.server.level.ServerPlayer;

/** C→S: pick which preset the fixed combo key (X) casts. */
public record FixedComboPayload(int slot) {
    public static void encode(FixedComboPayload msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.slot());
    }

    public static FixedComboPayload decode(FriendlyByteBuf buf) {
        return new FixedComboPayload(buf.readVarInt());
    }

    public static void handle(FixedComboPayload msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                MaiData mai = MaiApi.data(player);
                if (mai.fixedCombo() == msg.slot()) {
                    return;
                }
                mai.setFixedCombo(msg.slot());
                MaiApi.sync(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
