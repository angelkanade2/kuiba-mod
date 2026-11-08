package com.kuiba.mai.payload;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import com.kuiba.mai.client.MaiClientState;

/** S→C ritual bar. */
public record RitualProgressPayload(float progress, boolean cancelled) {
    public static void encode(RitualProgressPayload msg, FriendlyByteBuf buf) {
        buf.writeFloat(msg.progress());
        buf.writeBoolean(msg.cancelled());
    }

    public static RitualProgressPayload decode(FriendlyByteBuf buf) {
        return new RitualProgressPayload(buf.readFloat(), buf.readBoolean());
    }

    public static void handle(RitualProgressPayload msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> MaiClientState.onProgress(msg.progress(), msg.cancelled()));
        ctx.get().setPacketHandled(true);
    }
}
