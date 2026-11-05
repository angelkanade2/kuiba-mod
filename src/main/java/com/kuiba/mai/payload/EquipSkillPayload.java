package com.kuiba.mai.payload;

import java.util.Optional;

import com.kuiba.Kuiba;
import com.kuiba.mai.MaiApi;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record EquipSkillPayload(int slot, Optional<ResourceLocation> skillId) implements CustomPacketPayload {
    public static final Type<EquipSkillPayload> TYPE = new Type<>(Kuiba.id("equip_skill"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EquipSkillPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            EquipSkillPayload::slot,
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC),
            EquipSkillPayload::skillId,
            EquipSkillPayload::new);

    public static void handle(EquipSkillPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                MaiApi.tryEquip(player, payload.slot(), payload.skillId().orElse(null));
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
