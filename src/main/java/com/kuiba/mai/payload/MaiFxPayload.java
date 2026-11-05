package com.kuiba.mai.payload;

import com.kuiba.Kuiba;
import com.kuiba.mai.client.MaiClientState;
import com.kuiba.mai.client.MaiFx;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * S→C one-shot visual cue on a player's 脉门 rings.
 * {@code kind} is one of the {@code KIND_*} constants; {@code gate} is the gate count for
 * {@link #KIND_OPEN}, the newly opened gate for {@link #KIND_UNLOCK}, unused for {@link #KIND_CAST}.
 */
public record MaiFxPayload(int entityId, int kind, int gate) implements CustomPacketPayload {
    /** 开脉: every opened meridian lights up in sequence. */
    public static final int KIND_OPEN = 0;
    /** 冲脉 success: the new meridian bursts open. */
    public static final int KIND_UNLOCK = 1;
    /** A skill was cast: short pulse on all rings. */
    public static final int KIND_CAST = 2;

    public static final Type<MaiFxPayload> TYPE = new Type<>(Kuiba.id("mai_fx"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MaiFxPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            MaiFxPayload::entityId,
            ByteBufCodecs.VAR_INT,
            MaiFxPayload::kind,
            ByteBufCodecs.VAR_INT,
            MaiFxPayload::gate,
            MaiFxPayload::new);

    public static void handle(MaiFxPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            MaiClientState.onFx(payload.entityId(), payload.kind(), payload.gate());
            if (payload.kind() == KIND_OPEN && payload.gate() > 0) {
                MaiFx.playOpenSound(payload.entityId(), payload.gate());
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
