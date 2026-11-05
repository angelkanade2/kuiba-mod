package com.kuiba.entity;

import java.util.function.Supplier;

import com.kuiba.Kuiba;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class KuibaEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Kuiba.MOD_ID);

    public static final Supplier<EntityType<MaiBolt>> MAI_BOLT = ENTITY_TYPES.register("mai_bolt",
            () -> EntityType.Builder.<MaiBolt>of(MaiBolt::new, MobCategory.MISC)
                    .sized(0.4F, 0.4F)
                    .clientTrackingRange(6)
                    .updateInterval(1)
                    .build("mai_bolt"));

    private KuibaEntities() {}
}
