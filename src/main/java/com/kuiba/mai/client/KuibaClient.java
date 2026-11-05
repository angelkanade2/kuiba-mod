package com.kuiba.mai.client;

import com.kuiba.Kuiba;
import com.kuiba.entity.KuibaEntities;
import com.kuiba.mai.MaiCombo;
import com.kuiba.mai.MaiSkills;
import com.kuiba.mai.MaiTiers;
import com.kuiba.wenyao.WeaponArts;
import com.kuiba.mai.payload.CastComboPayload;
import com.kuiba.mai.payload.CastSkillPayload;
import com.kuiba.mai.payload.MaiInputPayload;
import com.kuiba.mai.payload.OpenMaiPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

@Mod(value = Kuiba.MOD_ID, dist = Dist.CLIENT)
public class KuibaClient {
    public KuibaClient(IEventBus modBus, ModContainer container) {
        modBus.addListener(MaiKeys::register);
        modBus.addListener(MaiHud::register);
        modBus.addListener(KuibaClient::registerClientReloaders);
        modBus.addListener(KuibaClient::addPlayerLayers);
        modBus.addListener(KuibaClient::registerRenderers);
        NeoForge.EVENT_BUS.register(KuibaClient.class);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(KuibaEntities.MAI_BOLT.get(), MaiBoltRenderer::new);
    }

    private static void registerClientReloaders(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(MaiTiers.CLIENT);
        event.registerReloadListener(MaiSkills.CLIENT);
        event.registerReloadListener(WeaponArts.CLIENT);
    }

    /** Attach the 脉门 ring layer to both player model variants. */
    private static void addPlayerLayers(EntityRenderersEvent.AddLayers event) {
        int added = 0;
        for (PlayerSkin.Model skin : event.getSkins()) {
            LivingEntityRenderer<? extends Player, ? extends EntityModel<? extends Player>> renderer = event.getSkin(skin);
            if (renderer instanceof PlayerRenderer playerRenderer && playerRenderer.addLayer(new MaiGateLayer(playerRenderer))) {
                added++;
            }
        }
        Kuiba.LOGGER.debug("Attached the mai gate layer to {} player renderer(s)", added);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        MaiClientState.clear();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        MaiFx.tick();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        while (MaiKeys.OPEN.consumeClick()) {
            if (minecraft.screen instanceof MaiScreen) {
                minecraft.setScreen(null);
            } else if (minecraft.screen == null) {
                minecraft.setScreen(new MaiScreen());
            }
        }
        while (MaiKeys.COMBO_EDITOR.consumeClick()) {
            if (minecraft.screen == null) {
                minecraft.setScreen(new MaiComboScreen());
            }
        }
        // Hold-to-open wheel. The screen itself closes on key release and sends the pick.
        while (MaiKeys.COMBO_WHEEL.consumeClick()) {
            if (minecraft.screen == null) {
                minecraft.setScreen(new ComboWheelScreen());
            }
        }
        if (minecraft.screen != null) {
            return;
        }
        while (MaiKeys.OPEN_GATE.consumeClick()) {
            PacketDistributor.sendToServer(new OpenMaiPayload());
        }
        // Live movement input: 脉闪 builds its direction from W/A/S/D on every dash, including combo steps.
        int forward = (minecraft.options.keyUp.isDown() ? 1 : 0) - (minecraft.options.keyDown.isDown() ? 1 : 0);
        int strafe = (minecraft.options.keyLeft.isDown() ? 1 : 0) - (minecraft.options.keyRight.isDown() ? 1 : 0);
        if (MaiClientState.shouldSendInput(forward, strafe)) {
            PacketDistributor.sendToServer(new MaiInputPayload(forward, strafe));
        }
        while (MaiKeys.SLOT_1.consumeClick()) {
            PacketDistributor.sendToServer(new CastSkillPayload(0));
        }
        while (MaiKeys.SLOT_2.consumeClick()) {
            PacketDistributor.sendToServer(new CastSkillPayload(1));
        }
        while (MaiKeys.SLOT_3.consumeClick()) {
            PacketDistributor.sendToServer(new CastSkillPayload(2));
        }
        while (MaiKeys.COMBO_1.consumeClick()) {
            // 施放脉技: runs whatever the 技能圆盘 has selected.
            PacketDistributor.sendToServer(new CastComboPayload(MaiCombo.NO_COMBO));
        }
        while (MaiKeys.COMBO_2.consumeClick()) {
            PacketDistributor.sendToServer(new CastComboPayload(1));
        }
        while (MaiKeys.COMBO_OPTIONAL.consumeClick()) {
            // Reserved. Two P0 combo slots are Z and X.
        }
    }
}
