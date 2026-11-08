package com.kuiba.mai.payload;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.Optional;

import com.kuiba.mai.MaiApi;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public record EquipSkillPayload(int slot, Optional<ResourceLocation> skillId) {
    public static void encode(EquipSkillPayload msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.slot());
        buf.writeBoolean(msg.skillId().isPresent());
        msg.skillId().ifPresent(buf::writeResourceLocation);
    }

    public static EquipSkillPayload decode(FriendlyByteBuf buf) {
        int slot = buf.readVarInt();
        Optional<ResourceLocation> skill = buf.readBoolean() ? Optional.of(buf.readResourceLocation()) : Optional.empty();
        return new EquipSkillPayload(slot, skill);
    }

    public static void handle(EquipSkillPayload msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                MaiApi.tryEquip(player, msg.slot(), msg.skillId().orElse(null));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
