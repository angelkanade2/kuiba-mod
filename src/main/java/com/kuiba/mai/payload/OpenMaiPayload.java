package com.kuiba.mai.payload;

import com.kuiba.Kuiba;
import com.kuiba.mai.MaiApi;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client request to fill the mai pool. The server checks gate, ritual, hunger, and cooldown. */
public record OpenMaiPayload() implements CustomPacketPayload {
    public static final Type<OpenMaiPayload> TYPE = new Type<>(Kuiba.id("open_mai"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenMaiPayload> STREAM_CODEC = StreamCodec.unit(new OpenMaiPayload());

    public static void handle(OpenMaiPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                MaiApi.tryOpenMai(player);
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
