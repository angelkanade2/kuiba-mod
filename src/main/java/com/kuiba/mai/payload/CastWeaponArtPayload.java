package com.kuiba.mai.payload;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import com.kuiba.wenyao.WeaponArts;

import net.minecraft.server.level.ServerPlayer;

/** Optional client request for the same cast as main-hand right-click. */
public record CastWeaponArtPayload() {
    public static void encode(CastWeaponArtPayload msg, FriendlyByteBuf buf) {
    }

    public static CastWeaponArtPayload decode(FriendlyByteBuf buf) {
        return new CastWeaponArtPayload();
    }

    public static void handle(CastWeaponArtPayload msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                WeaponArts.tryCast(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
