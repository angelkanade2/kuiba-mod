package com.kuiba.mai.payload;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import com.kuiba.mai.client.MaiClientState;

/** S→C combo cursor for the HUD. */
public record ComboStatePayload(int slot, int done, int total, int state) {
    public static final int STATE_RUNNING = 0;
    public static final int STATE_DONE = 1;
    public static final int STATE_FAILED = 2;

    public static void encode(ComboStatePayload msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.slot());
        buf.writeVarInt(msg.done());
        buf.writeVarInt(msg.total());
        buf.writeVarInt(msg.state());
    }

    public static ComboStatePayload decode(FriendlyByteBuf buf) {
        return new ComboStatePayload(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(ComboStatePayload msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> MaiClientState.onCombo(msg.slot(), msg.done(), msg.total(), msg.state()));
        ctx.get().setPacketHandled(true);
    }
}
