package com.kuiba.mai.payload;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import com.kuiba.mai.MaiApi;

import net.minecraft.server.level.ServerPlayer;

/** C→S: cast the skill in {@code slot}. */
public record CastSkillPayload(int slot) {
    public static void encode(CastSkillPayload msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.slot());
    }

    public static CastSkillPayload decode(FriendlyByteBuf buf) {
        return new CastSkillPayload(buf.readVarInt());
    }

    public static void handle(CastSkillPayload msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                MaiApi.tryCast(player, msg.slot());
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
