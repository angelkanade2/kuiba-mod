package com.kuiba.mai.client;

import com.kuiba.Kuiba;
import com.kuiba.core.KuibaNetwork;
import com.kuiba.entity.KuibaEntities;
import com.kuiba.mai.MaiApi;
import com.kuiba.mai.MaiCombo;
import com.kuiba.mai.payload.CastComboPayload;
import com.kuiba.mai.payload.CastSkillPayload;
import com.kuiba.mai.payload.MaiInputPayload;
import com.kuiba.mai.payload.OpenMaiPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Kuiba.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class KuibaClient {
    private KuibaClient() {}

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        MaiKeys.register(event);
    }

    @SubscribeEvent
    public static void registerHud(RegisterGuiOverlaysEvent event) {
        MaiHud.register(event);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(KuibaEntities.MAI_BOLT.get(), MaiBoltRenderer::new);
    }

    /** Attach the 脉门 ring layer to both player model variants. */
    @SubscribeEvent
    public static void addPlayerLayers(EntityRenderersEvent.AddLayers event) {
        int added = 0;
        for (String skin : event.getSkins()) {
            var renderer = event.getSkin(skin);
            if (renderer instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer playerRenderer
                    && playerRenderer.addLayer(new MaiGateLayer(playerRenderer))) {
                added++;
            }
        }
        Kuiba.LOGGER.debug("Attached the mai gate layer to {} player renderer(s)", added);
    }

    @Mod.EventBusSubscriber(modid = Kuiba.MOD_ID, value = Dist.CLIENT)
    public static final class ForgeBus {
        private ForgeBus() {}

        @SubscribeEvent
        public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
            MaiClientState.clear();
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
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
            while (MaiKeys.COMBO_WHEEL.consumeClick()) {
                if (minecraft.screen == null) {
                    minecraft.setScreen(new ComboWheelScreen());
                }
            }
            if (minecraft.screen != null) {
                return;
            }
            while (MaiKeys.OPEN_GATE.consumeClick()) {
                KuibaNetwork.sendToServer(new OpenMaiPayload());
            }
            int forward = (minecraft.options.keyUp.isDown() ? 1 : 0) - (minecraft.options.keyDown.isDown() ? 1 : 0);
            int strafe = (minecraft.options.keyLeft.isDown() ? 1 : 0) - (minecraft.options.keyRight.isDown() ? 1 : 0);
            if (MaiClientState.shouldSendInput(forward, strafe)) {
                KuibaNetwork.sendToServer(new MaiInputPayload(forward, strafe));
            }
            while (MaiKeys.SLOT_1.consumeClick()) {
                KuibaNetwork.sendToServer(new CastSkillPayload(0));
            }
            while (MaiKeys.SLOT_2.consumeClick()) {
                KuibaNetwork.sendToServer(new CastSkillPayload(1));
            }
            while (MaiKeys.SLOT_3.consumeClick()) {
                KuibaNetwork.sendToServer(new CastSkillPayload(2));
            }
            while (MaiKeys.COMBO_1.consumeClick()) {
                KuibaNetwork.sendToServer(new CastComboPayload(MaiCombo.NO_COMBO));
            }
            while (MaiKeys.COMBO_2.consumeClick()) {
                LocalPlayer fixedPlayer = minecraft.player;
                int fixed = fixedPlayer == null ? 1 : MaiApi.data(fixedPlayer).fixedCombo();
                KuibaNetwork.sendToServer(new CastComboPayload(fixed));
            }
            while (MaiKeys.COMBO_OPTIONAL.consumeClick()) {
                // Reserved. Two P0 combo slots are Z and X.
            }
        }

    }
}
