package com.kuiba.mai.payload;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import com.kuiba.core.ClientDispatch;

/**
 * S→C one-shot visual cue on 脉门 rings.
 */
public record MaiFxPayload(int entityId, int kind, int gate) {
    public static final int KIND_OPEN = 0;
    public static final int KIND_UNLOCK = 1;
    public static final int KIND_CAST = 2;

    public static void encode(MaiFxPayload msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId());
        buf.writeVarInt(msg.kind());
        buf.writeVarInt(msg.gate());
    }

    public static MaiFxPayload decode(FriendlyByteBuf buf) {
        return new MaiFxPayload(buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(MaiFxPayload msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientDispatch.run("fx", msg));
        ctx.get().setPacketHandled(true);
    }
}
