package com.kuiba.mai.payload;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import com.kuiba.mai.MaiCombo;

import net.minecraft.server.level.ServerPlayer;

/**
 * C→S: run a combo. {@code slot} is a preset index, or {@link MaiCombo#NO_COMBO} for the wheel pick.
 */
public record CastComboPayload(int slot) {
    public static void encode(CastComboPayload msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.slot());
    }

    public static CastComboPayload decode(FriendlyByteBuf buf) {
        return new CastComboPayload(buf.readVarInt());
    }

    public static void handle(CastComboPayload msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                MaiCombo.start(player, msg.slot());
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
