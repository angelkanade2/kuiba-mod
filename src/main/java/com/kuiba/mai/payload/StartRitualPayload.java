package com.kuiba.mai.payload;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;

import com.kuiba.mai.MaiRitual;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

public record StartRitualPayload(boolean start, Optional<BlockPos> altar) {
    public static void encode(StartRitualPayload msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.start());
        buf.writeBoolean(msg.altar().isPresent());
        msg.altar().ifPresent(buf::writeBlockPos);
    }

    public static StartRitualPayload decode(FriendlyByteBuf buf) {
        boolean start = buf.readBoolean();
        Optional<BlockPos> altar = buf.readBoolean() ? Optional.of(buf.readBlockPos()) : Optional.empty();
        return new StartRitualPayload(start, altar);
    }

    public static void handle(StartRitualPayload msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            if (msg.start()) {
                MaiRitual.tryStart(player, msg.altar().orElse(null));
            } else {
                MaiRitual.cancel(player, "packet");
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
