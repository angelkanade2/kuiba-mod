package com.kuiba;

import java.util.function.Supplier;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class KuibaSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Kuiba.MOD_ID);

    /** 开脉 composite sfx: one clip per opened gate count (1..12). */
    @SuppressWarnings("unchecked")
    private static final Supplier<SoundEvent>[] OPEN_MAI_GATES = new Supplier[13];

    /**
     * Audible radius of a 开脉 clip, in blocks. A variable-range event only reaches the default 16 blocks,
     * which made 特殊怪 pulses useless for 听声辩位; a fixed range lets the sound attenuate over a wider
     * area while staying fully positional.
     */
    public static final float OPEN_MAI_RANGE = 48.0F;

    static {
        for (int i = 1; i <= 12; i++) {
            String name = "open_mai_gate_x" + i;
            OPEN_MAI_GATES[i] = register(name);
        }
    }

    private KuibaSounds() {}

    public static SoundEvent openMaiGate(int gateCount) {
        int n = Mth.clamp(gateCount, 1, 12);
        return OPEN_MAI_GATES[n].get();
    }

    private static Supplier<SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createFixedRangeEvent(Kuiba.id(name), OPEN_MAI_RANGE));
    }
}
