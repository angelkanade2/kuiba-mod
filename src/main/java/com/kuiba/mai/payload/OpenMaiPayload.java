package com.kuiba.mai.payload;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import com.kuiba.mai.MaiApi;

import net.minecraft.server.level.ServerPlayer;

/** Client request to fill the mai pool. The server checks gate, ritual, hunger, and cooldown. */
public record OpenMaiPayload() {
    public static void encode(OpenMaiPayload msg, FriendlyByteBuf buf) {
    }

    public static OpenMaiPayload decode(FriendlyByteBuf buf) {
        return new OpenMaiPayload();
    }

    public static void handle(OpenMaiPayload msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                MaiApi.tryOpenMai(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
