package com.kuiba.mai.payload;

import com.kuiba.Kuiba;
import com.kuiba.mai.client.MaiClientState;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * S→C: what other clients need to draw a player's 脉门 rings.
 * {@code level} is the pool ratio quantised to 0..{@link #LEVELS}. Only sent to players tracking
 * {@code entityId}, and only when gate or level changed. The local player uses its own attachment.
 */
public record MaiAuraPayload(int entityId, int gate, int level) implements CustomPacketPayload {
    public static final int LEVELS = 16;

    public static final Type<MaiAuraPayload> TYPE = new Type<>(Kuiba.id("mai_aura"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MaiAuraPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            MaiAuraPayload::entityId,
            ByteBufCodecs.VAR_INT,
            MaiAuraPayload::gate,
            ByteBufCodecs.VAR_INT,
            MaiAuraPayload::level,
            MaiAuraPayload::new);

    public static int quantize(float ratio) {
        if (!(ratio > 0.0F)) {
            return 0;
        }
        return Math.clamp(Math.round(ratio * LEVELS), 1, LEVELS);
    }

    public static void handle(MaiAuraPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> MaiClientState.onAura(payload.entityId(), payload.gate(), payload.level() / (float) LEVELS));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
