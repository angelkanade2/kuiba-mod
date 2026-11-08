package com.kuiba.mai.payload;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import com.kuiba.core.ClientDispatch;
import com.kuiba.mai.MaiCombo;
import com.kuiba.mai.MaiData;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

public record SyncMaiPayload(
        int gate,
        float current,
        float max,
        int backlashStacks,
        ResourceLocation[] equipped,
        Map<ResourceLocation, Integer> cooldowns,
        Set<ResourceLocation> unlocked,
        MaiCombo.Preset[] combos,
        int selectedCombo,
        int fixedCombo) {

    public static void encode(SyncMaiPayload msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.gate());
        buf.writeFloat(msg.current());
        buf.writeFloat(msg.max());
        buf.writeVarInt(msg.backlashStacks());
        for (int i = 0; i < MaiData.EQUIPPED_SLOTS; i++) {
            ResourceLocation id = i < msg.equipped().length ? msg.equipped()[i] : null;
            buf.writeBoolean(id != null);
            if (id != null) {
                buf.writeResourceLocation(id);
            }
        }
        buf.writeVarInt(msg.cooldowns().size());
        for (Map.Entry<ResourceLocation, Integer> entry : msg.cooldowns().entrySet()) {
            buf.writeResourceLocation(entry.getKey());
            buf.writeVarInt(entry.getValue());
        }
        buf.writeVarInt(msg.unlocked().size());
        for (ResourceLocation id : msg.unlocked()) {
            buf.writeResourceLocation(id);
        }
        for (int i = 0; i < MaiCombo.COMBO_COUNT; i++) {
            SaveComboPayload.writePreset(buf, i < msg.combos().length ? msg.combos()[i] : null);
        }
        buf.writeVarInt(msg.selectedCombo());
        buf.writeVarInt(msg.fixedCombo());
    }

    public static SyncMaiPayload decode(FriendlyByteBuf buf) {
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
        int fixedCombo = buf.readVarInt();
        return new SyncMaiPayload(gate, current, max, backlash, equipped, cooldowns, unlocked, combos, selectedCombo, fixedCombo);
    }

    public static void handle(SyncMaiPayload msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientDispatch.run("sync", msg));
        ctx.get().setPacketHandled(true);
    }
}
