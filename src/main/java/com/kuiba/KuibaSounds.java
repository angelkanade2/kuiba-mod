package com.kuiba;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class KuibaSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, Kuiba.MOD_ID);

    /** 开脉 composite sfx: one clip per opened gate count (1..12). */
    @SuppressWarnings("unchecked")
    private static final Supplier<SoundEvent>[] OPEN_MAI_GATES = new Supplier[13];

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
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(Kuiba.id(name)));
    }
}
