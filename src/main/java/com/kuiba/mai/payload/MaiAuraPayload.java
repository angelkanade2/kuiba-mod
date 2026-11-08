package com.kuiba.mai.payload;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import com.kuiba.mai.client.MaiClientState;

import net.minecraft.util.Mth;

/**
 * S→C: what other clients need to draw a player's 脉门 rings.
 * {@code level} is the pool ratio quantised to 0..{@link #LEVELS}.
 */
public record MaiAuraPayload(int entityId, int gate, int level) {
    public static final int LEVELS = 16;

    public static int quantize(float ratio) {
        if (!(ratio > 0.0F)) {
            return 0;
        }
        return Mth.clamp(Math.round(ratio * LEVELS), 1, LEVELS);
    }

    public static void encode(MaiAuraPayload msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId());
        buf.writeVarInt(msg.gate());
        buf.writeVarInt(msg.level());
    }

    public static MaiAuraPayload decode(FriendlyByteBuf buf) {
        return new MaiAuraPayload(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(MaiAuraPayload msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> MaiClientState.onAura(msg.entityId(), msg.gate(), msg.level() / (float) LEVELS));
        ctx.get().setPacketHandled(true);
    }
}
