package com.kuiba.mai.payload;

import com.kuiba.Kuiba;
import com.kuiba.mai.client.MaiClientState;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * S→C combo cursor for the HUD. {@code done} counts steps already cast; {@code state} is one of
 * {@code STATE_*}.
 */
public record ComboStatePayload(int slot, int done, int total, int state) implements CustomPacketPayload {
    public static final int STATE_RUNNING = 0;
    public static final int STATE_DONE = 1;
    public static final int STATE_FAILED = 2;

    public static final Type<ComboStatePayload> TYPE = new Type<>(Kuiba.id("combo_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ComboStatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            ComboStatePayload::slot,
            ByteBufCodecs.VAR_INT,
            ComboStatePayload::done,
            ByteBufCodecs.VAR_INT,
            ComboStatePayload::total,
            ByteBufCodecs.VAR_INT,
            ComboStatePayload::state,
            ComboStatePayload::new);

    public static void handle(ComboStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> MaiClientState.onCombo(payload.slot(), payload.done(), payload.total(), payload.state()));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
