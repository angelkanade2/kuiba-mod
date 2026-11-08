package com.kuiba.mai.payload;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.kuiba.mai.MaiCombo;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * C→S: save one named preset, including its 技能圆盘 placement.
 * The server re-checks the gate, unlocks, charge limits, and the name.
 */
public record SaveComboPayload(int index, MaiCombo.Preset preset) {
    /** Generous read cap; the server clamps the name to {@link MaiCombo#NAME_MAX} anyway. */
    private static final int NAME_WIRE_MAX = 256;

    public static void encode(SaveComboPayload msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.index());
        writePreset(buf, msg.preset());
    }

    public static SaveComboPayload decode(FriendlyByteBuf buf) {
        return new SaveComboPayload(buf.readVarInt(), readPreset(buf));
    }

    public static void writePreset(FriendlyByteBuf buf, MaiCombo.Preset preset) {
        MaiCombo.Preset safe = preset == null ? MaiCombo.Preset.EMPTY : preset;
        buf.writeUtf(safe.name(), NAME_WIRE_MAX);
        buf.writeVarInt(safe.gapTicks());
        buf.writeBoolean(safe.onWheel());
        buf.writeVarInt(safe.steps().size());
        for (MaiCombo.Step step : safe.steps()) {
            buf.writeResourceLocation(step.skillId());
            buf.writeVarInt(step.charge());
        }
    }

    public static MaiCombo.Preset readPreset(FriendlyByteBuf buf) {
        String name = buf.readUtf(NAME_WIRE_MAX);
        int gap = buf.readVarInt();
        boolean onWheel = buf.readBoolean();
        int count = Math.min(MaiCombo.MAX_STEPS, Math.max(0, buf.readVarInt()));
        List<MaiCombo.Step> steps = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ResourceLocation skill = buf.readResourceLocation();
            int charge = buf.readVarInt();
            steps.add(new MaiCombo.Step(skill, charge));
        }
        return new MaiCombo.Preset(name, gap, onWheel, steps);
    }

    public static void handle(SaveComboPayload msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                MaiCombo.save(player, msg.index(), msg.preset());
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
