package com.kuiba;

import org.slf4j.Logger;

import com.kuiba.core.KuibaDrops;
import com.kuiba.core.KuibaNetwork;
import com.kuiba.core.KuibaServerConfig;
import com.kuiba.core.MaiAttachments;
import com.kuiba.core.MaiGateHosts;
import com.kuiba.entity.KuibaEntities;
import com.kuiba.item.KuibaItems;
import com.kuiba.mai.advancement.GateReachedTrigger;
import com.kuiba.mai.event.MaiGameEvents;
import com.kuiba.test.KuibaGameTests;
import com.kuiba.wenyao.KuibaCommands;
import com.mojang.logging.LogUtils;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.ModLoadingContext;

@Mod(Kuiba.MOD_ID)
public class Kuiba {
    public static final String MOD_ID = "kuiba";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Kuiba() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, KuibaServerConfig.SPEC, "kuiba-server.toml");

        KuibaItems.ITEMS.register(modBus);
        KuibaItems.TABS.register(modBus);
        KuibaSounds.SOUND_EVENTS.register(modBus);
        KuibaEntities.ENTITY_TYPES.register(modBus);
        modBus.addListener(MaiAttachments::registerCapabilities);
        modBus.addListener(KuibaGameTests::register);

        KuibaNetwork.register();
        GateReachedTrigger.register();

        MinecraftForge.EVENT_BUS.register(MaiAttachments.class);
        MinecraftForge.EVENT_BUS.register(MaiGameEvents.class);
        MinecraftForge.EVENT_BUS.register(MaiGateHosts.class);
        MinecraftForge.EVENT_BUS.register(KuibaDrops.class);
        MinecraftForge.EVENT_BUS.addListener(KuibaCommands::register);

        LOGGER.info("魁拔 (kuiba) 1.20.1 Forge 已加载");
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    /** {@code null} when {@code raw} is not a valid resource location. */
    public static ResourceLocation parse(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            return new ResourceLocation(raw);
        } catch (RuntimeException exception) {
            return null;
        }
    }
}
