package com.kuiba;

import org.slf4j.Logger;

import com.kuiba.core.KuibaDrops;
import com.kuiba.core.KuibaNetwork;
import com.kuiba.core.KuibaServerConfig;
import com.kuiba.core.MaiAttachments;
import com.kuiba.entity.KuibaEntities;
import com.kuiba.item.KuibaComponents;
import com.kuiba.item.KuibaItems;
import com.kuiba.mai.advancement.GateReachedTrigger;
import com.kuiba.mai.event.MaiGameEvents;
import com.kuiba.wenyao.KuibaCommands;
import com.mojang.logging.LogUtils;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

@Mod(Kuiba.MOD_ID)
public class Kuiba {
    public static final String MOD_ID = "kuiba";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Kuiba(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, KuibaServerConfig.SPEC, "kuiba-server.toml");

        KuibaComponents.COMPONENTS.register(modBus);
        KuibaItems.ARMOR_MATERIALS.register(modBus);
        KuibaItems.ITEMS.register(modBus);
        KuibaItems.TABS.register(modBus);
        MaiAttachments.ATTACHMENT_TYPES.register(modBus);
        GateReachedTrigger.TRIGGERS.register(modBus);
        KuibaSounds.SOUND_EVENTS.register(modBus);
        KuibaEntities.ENTITY_TYPES.register(modBus);

        modBus.addListener(KuibaNetwork::register);
        NeoForge.EVENT_BUS.register(MaiGameEvents.class);
        NeoForge.EVENT_BUS.register(KuibaDrops.class);
        NeoForge.EVENT_BUS.addListener(KuibaCommands::register);

        LOGGER.info("魁拔 (kuiba) M1 脉门骨架已加载");
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
