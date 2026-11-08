package com.kuiba.mai.payload;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import com.kuiba.mai.MaiApi;

import net.minecraft.server.level.ServerPlayer;

/**
 * C→S: the movement keys changed. {@code forward} is W(+1) / S(-1), {@code strafe} is A(+1) / D(-1).
 */
public record MaiInputPayload(int forward, int strafe) {
    public static void encode(MaiInputPayload msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.forward());
        buf.writeVarInt(msg.strafe());
    }

    public static MaiInputPayload decode(FriendlyByteBuf buf) {
        return new MaiInputPayload(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(MaiInputPayload msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                MaiApi.data(player).setInput(msg.forward(), msg.strafe());
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
