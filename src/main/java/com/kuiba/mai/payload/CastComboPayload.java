package com.kuiba.mai.payload;

import com.kuiba.Kuiba;
import com.kuiba.mai.MaiCombo;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C→S: run a combo. {@code slot} is a preset index, or {@link MaiCombo#NO_COMBO} for the wheel pick.
 * Direction hints are not carried here: {@link MaiInputPayload} keeps the server's view of the
 * backward key live, so each 脉闪 step decides its own direction.
 */
public record CastComboPayload(int slot) implements CustomPacketPayload {
    public static final Type<CastComboPayload> TYPE = new Type<>(Kuiba.id("cast_combo"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CastComboPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            CastComboPayload::slot,
            CastComboPayload::new);

    public static void handle(CastComboPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                MaiCombo.start(player, payload.slot());
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
