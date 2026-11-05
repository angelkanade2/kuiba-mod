package com.kuiba.mai.payload;

import com.kuiba.Kuiba;
import com.kuiba.mai.client.MaiClientState;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RitualProgressPayload(float progress, boolean cancelled) implements CustomPacketPayload {
    public static final Type<RitualProgressPayload> TYPE = new Type<>(Kuiba.id("ritual_progress"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RitualProgressPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT,
            RitualProgressPayload::progress,
            ByteBufCodecs.BOOL,
            RitualProgressPayload::cancelled,
            RitualProgressPayload::new);

    public static void handle(RitualProgressPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> MaiClientState.onProgress(payload.progress(), payload.cancelled()));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
