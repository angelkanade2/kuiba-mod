package com.kuiba.core;

import org.jetbrains.annotations.Nullable;

import com.kuiba.Kuiba;
import com.kuiba.mai.MaiData;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Player {@code kuiba:mai} and living-entity {@code kuiba:gate_host} capabilities.
 * NeoForge attachments are not available on Forge 1.20.1, so the same data is stored here and
 * copied by hand on player clone (death and End return).
 */
public final class MaiAttachments {
    public static final Capability<MaiData> MAI = CapabilityManager.get(new CapabilityToken<>() {});
    public static final Capability<GateHostData> GATE_HOST = CapabilityManager.get(new CapabilityToken<>() {});

    public static final ResourceLocation MAI_ID = new ResourceLocation(Kuiba.MOD_ID, "mai");
    public static final ResourceLocation GATE_HOST_ID = new ResourceLocation(Kuiba.MOD_ID, "gate_host");

    private MaiAttachments() {}

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(MaiData.class);
        event.register(GateHostData.class);
    }

    public static MaiData data(Player player) {
        return player.getCapability(MAI).orElseGet(MaiData::new);
    }

    public static GateHostData host(LivingEntity entity) {
        return entity.getCapability(GATE_HOST).orElseGet(GateHostData::new);
    }

    public static boolean isHost(LivingEntity entity) {
        GateHostData data = entity.getCapability(GATE_HOST).orElse(null);
        return data != null && data.isHost();
    }

    @SubscribeEvent
    public static void attach(AttachCapabilitiesEvent<Entity> event) {
        Entity entity = event.getObject();
        if (entity instanceof Player) {
            MaiProvider provider = new MaiProvider();
            event.addCapability(MAI_ID, provider);
            event.addListener(provider::invalidate);
        }
        if (entity instanceof LivingEntity) {
            GateHostProvider provider = new GateHostProvider();
            event.addCapability(GATE_HOST_ID, provider);
            event.addListener(provider::invalidate);
        }
    }

    private static final class MaiProvider implements ICapabilitySerializable<CompoundTag> {
        private final MaiData data = new MaiData();
        private final LazyOptional<MaiData> optional = LazyOptional.of(() -> data);

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
            return cap == MAI ? optional.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            return data.serializeNBT();
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            data.deserializeNBT(tag);
        }

        private void invalidate() {
            optional.invalidate();
        }
    }

    private static final class GateHostProvider implements ICapabilitySerializable<CompoundTag> {
        private final GateHostData data = new GateHostData();
        private final LazyOptional<GateHostData> optional = LazyOptional.of(() -> data);

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
            return cap == GATE_HOST ? optional.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            return data.serializeNBT();
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            data.deserializeNBT(tag);
        }

        private void invalidate() {
            optional.invalidate();
        }
    }
}
