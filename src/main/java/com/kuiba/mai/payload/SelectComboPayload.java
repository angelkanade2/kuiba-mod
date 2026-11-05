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
 * C→S: the 技能圆盘 was released over {@code comboIndex} (or closed on nothing, sent as
 * {@link MaiCombo#NO_COMBO}). The server stores the pick as the active combo.
 */
public record SelectComboPayload(int comboIndex) implements CustomPacketPayload {
    public static final Type<SelectComboPayload> TYPE = new Type<>(Kuiba.id("select_combo"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SelectComboPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            SelectComboPayload::comboIndex,
            SelectComboPayload::new);

    public static void handle(SelectComboPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                MaiCombo.select(player, payload.comboIndex());
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
