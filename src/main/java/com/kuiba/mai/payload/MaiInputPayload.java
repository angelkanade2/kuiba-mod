package com.kuiba.mai.payload;

import com.kuiba.Kuiba;
import com.kuiba.mai.MaiApi;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C→S: the movement keys changed. The server cannot read normal WASD input for a standing player, so
 * 脉闪 needs this pushed live to build its direction on every dash, including the later steps of a combo.
 * <p>
 * {@code forward} is W(+1) / S(-1), {@code strafe} is A(+1, left) / D(-1, right), both in {@code -1..1}.
 * Sent only when the pair changes, so it is one packet per key press/release, not per tick.
 */
public record MaiInputPayload(int forward, int strafe) implements CustomPacketPayload {
    public static final Type<MaiInputPayload> TYPE = new Type<>(Kuiba.id("mai_input"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MaiInputPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            MaiInputPayload::forward,
            ByteBufCodecs.VAR_INT,
            MaiInputPayload::strafe,
            MaiInputPayload::new);

    public static void handle(MaiInputPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                MaiApi.data(player).setInput(payload.forward(), payload.strafe());
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
