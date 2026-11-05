package com.kuiba.mai.payload;

import java.util.Optional;

import com.kuiba.Kuiba;
import com.kuiba.mai.MaiRitual;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record StartRitualPayload(boolean start, Optional<BlockPos> altar) implements CustomPacketPayload {
    public static final Type<StartRitualPayload> TYPE = new Type<>(Kuiba.id("start_ritual"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StartRitualPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            StartRitualPayload::start,
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC),
            StartRitualPayload::altar,
            StartRitualPayload::new);

    public static void handle(StartRitualPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (payload.start()) {
                MaiRitual.tryStart(player, payload.altar().orElse(null));
            } else {
                MaiRitual.cancel(player, "packet");
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
