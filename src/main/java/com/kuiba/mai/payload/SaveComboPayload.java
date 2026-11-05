package com.kuiba.mai.payload;

import java.util.ArrayList;
import java.util.List;

import com.kuiba.Kuiba;
import com.kuiba.mai.MaiCombo;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C→S: save one named preset, including its 技能圆盘 placement.
 * The server re-checks the gate, unlocks, charge limits, and the name.
 */
public record SaveComboPayload(int index, MaiCombo.Preset preset) implements CustomPacketPayload {
    /** Generous read cap; the server clamps the name to {@link MaiCombo#NAME_MAX} anyway. */
    private static final int NAME_WIRE_MAX = 256;

    public static final Type<SaveComboPayload> TYPE = new Type<>(Kuiba.id("save_combo"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SaveComboPayload> STREAM_CODEC = StreamCodec.ofMember(
            SaveComboPayload::write,
            SaveComboPayload::decode);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(index);
        writePreset(buf, preset);
    }

    private static SaveComboPayload decode(RegistryFriendlyByteBuf buf) {
        int index = buf.readVarInt();
        return new SaveComboPayload(index, readPreset(buf));
    }

    public static void writePreset(RegistryFriendlyByteBuf buf, MaiCombo.Preset preset) {
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

    public static MaiCombo.Preset readPreset(RegistryFriendlyByteBuf buf) {
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

    public static void handle(SaveComboPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                MaiCombo.save(player, payload.index(), payload.preset());
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
