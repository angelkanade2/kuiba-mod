package com.kuiba.mai.payload;

import com.kuiba.Kuiba;
import com.kuiba.mai.MaiApi;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** C→S: cast the skill in {@code slot}. */
public record CastSkillPayload(int slot) implements CustomPacketPayload {
    public static final Type<CastSkillPayload> TYPE = new Type<>(Kuiba.id("cast_skill"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CastSkillPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            CastSkillPayload::slot,
            CastSkillPayload::new);

    public static void handle(CastSkillPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                MaiApi.tryCast(player, payload.slot());
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
