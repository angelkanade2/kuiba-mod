package com.kuiba.mai.payload;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.kuiba.Kuiba;
import com.kuiba.mai.MaiApi;
import com.kuiba.mai.MaiCombo;
import com.kuiba.mai.MaiData;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SyncMaiPayload(
        int gate,
        float current,
        float max,
        int backlashStacks,
        ResourceLocation[] equipped,
        Map<ResourceLocation, Integer> cooldowns,
        Set<ResourceLocation> unlocked,
        MaiCombo.Preset[] combos,
        int selectedCombo) implements CustomPacketPayload {
    public static final Type<SyncMaiPayload> TYPE = new Type<>(Kuiba.id("sync_mai"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncMaiPayload> STREAM_CODEC = StreamCodec.ofMember(
            SyncMaiPayload::write,
            SyncMaiPayload::decode);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(gate);
        buf.writeFloat(current);
        buf.writeFloat(max);
        buf.writeVarInt(backlashStacks);
        for (int i = 0; i < MaiData.EQUIPPED_SLOTS; i++) {
            ResourceLocation id = i < equipped.length ? equipped[i] : null;
            buf.writeBoolean(id != null);
            if (id != null) {
                buf.writeResourceLocation(id);
            }
        }
        buf.writeVarInt(cooldowns.size());
        for (Map.Entry<ResourceLocation, Integer> entry : cooldowns.entrySet()) {
            buf.writeResourceLocation(entry.getKey());
            buf.writeVarInt(entry.getValue());
        }
        buf.writeVarInt(unlocked.size());
        for (ResourceLocation id : unlocked) {
            buf.writeResourceLocation(id);
        }
        for (int i = 0; i < MaiCombo.COMBO_COUNT; i++) {
            SaveComboPayload.writePreset(buf, i < combos.length ? combos[i] : null);
        }
        buf.writeVarInt(selectedCombo);
    }

    private static SyncMaiPayload decode(RegistryFriendlyByteBuf buf) {
        int gate = buf.readVarInt();
        float current = buf.readFloat();
        float max = buf.readFloat();
        int backlash = buf.readVarInt();
        ResourceLocation[] equipped = new ResourceLocation[MaiData.EQUIPPED_SLOTS];
        for (int i = 0; i < MaiData.EQUIPPED_SLOTS; i++) {
            if (buf.readBoolean()) {
                equipped[i] = buf.readResourceLocation();
            }
        }
        int cooldownCount = buf.readVarInt();
        Map<ResourceLocation, Integer> cooldowns = new LinkedHashMap<>();
        for (int i = 0; i < cooldownCount; i++) {
            cooldowns.put(buf.readResourceLocation(), buf.readVarInt());
        }
        int unlockedCount = buf.readVarInt();
        Set<ResourceLocation> unlocked = new LinkedHashSet<>();
        for (int i = 0; i < unlockedCount; i++) {
            unlocked.add(buf.readResourceLocation());
        }
        MaiCombo.Preset[] combos = new MaiCombo.Preset[MaiCombo.COMBO_COUNT];
        for (int i = 0; i < MaiCombo.COMBO_COUNT; i++) {
            combos[i] = SaveComboPayload.readPreset(buf);
        }
        int selectedCombo = buf.readVarInt();
        return new SyncMaiPayload(gate, current, max, backlash, equipped, cooldowns, unlocked, combos, selectedCombo);
    }

    public static void handle(SyncMaiPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            MaiData mai = MaiApi.data(player);
            mai.applySync(
                    payload.gate(),
                    payload.current(),
                    payload.max(),
                    payload.backlashStacks(),
                    payload.equipped(),
                    payload.cooldowns(),
                    payload.unlocked());
            for (int i = 0; i < payload.combos().length; i++) {
                mai.setCombo(i, payload.combos()[i]);
            }
            mai.setSelectedCombo(payload.selectedCombo());
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
