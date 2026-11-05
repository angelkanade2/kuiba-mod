package com.kuiba.item;

import com.kuiba.Kuiba;
import com.kuiba.wenyao.WenyaoTier;
import com.mojang.serialization.Codec;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class KuibaComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Kuiba.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<WenyaoTier>> WENYAO_TIER = COMPONENTS.register(
            "wenyao_tier",
            () -> DataComponentType.<WenyaoTier>builder()
                    .persistent(Codec.STRING.xmap(WenyaoTier::byName, WenyaoTier::getSerializedName))
                    .networkSynchronized(WenyaoTier.STREAM_CODEC)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> WEAPON_ART = COMPONENTS.register(
            "weapon_art",
            () -> DataComponentType.<ResourceLocation>builder()
                    .persistent(ResourceLocation.CODEC)
                    .networkSynchronized(ResourceLocation.STREAM_CODEC)
                    .build());

    private KuibaComponents() {}
}
