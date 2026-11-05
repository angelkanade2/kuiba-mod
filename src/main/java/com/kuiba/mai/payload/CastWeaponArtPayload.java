package com.kuiba.mai.payload;

import com.kuiba.Kuiba;
import com.kuiba.wenyao.WeaponArts;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Optional client request for the same cast as main-hand right-click.
 * No extra key is bound. G + right-click is not a gesture.
 */
public record CastWeaponArtPayload() implements CustomPacketPayload {
    public static final Type<CastWeaponArtPayload> TYPE = new Type<>(Kuiba.id("cast_weapon_art"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CastWeaponArtPayload> STREAM_CODEC = StreamCodec.unit(new CastWeaponArtPayload());

    public static void handle(CastWeaponArtPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                WeaponArts.tryCast(player);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
