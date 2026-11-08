package com.kuiba.mai.client;

import com.kuiba.Kuiba;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.event.ComputeFovModifierEvent;

/**
 * First-person 开脉 / 冲脉 staging: a FOV kick plus a short camera shake, both driven by
 * {@link MaiFx#cinematic} so they land in sync with the body rings, the HUD flash and the sound.
 * Only the local player is affected; other players see the body rings and particle burst instead.
 */
@Mod.EventBusSubscriber(modid = Kuiba.MOD_ID, value = Dist.CLIENT)
public final class MaiCinematic {
    /** Peak FOV widening, applied on top of the current modifier. ~6 FOV degrees at 90. */
    private static final float FOV_KICK = 0.065F;
    /** Peak camera shake in degrees. */
    private static final float SHAKE_DEG = 0.45F;

    private MaiCinematic() {}

    private static float intensity() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.isSpectator() || minecraft.screen != null) {
            return 0.0F;
        }
        MaiClientState.Fx fx = MaiClientState.fx(minecraft.player.getId());
        float time = MaiFx.time(minecraft.getFrameTime());
        return MaiFx.cinematic(fx, time);
    }

    @SubscribeEvent
    public static void onFov(ComputeFovModifierEvent event) {
        float cine = intensity();
        if (cine <= 0.0F) {
            return;
        }
        // Widen fast, then settle back: the world "breaths out" as the gates light.
        event.setNewFovModifier(event.getFovModifier() * (1.0F + FOV_KICK * cine));
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        float cine = intensity();
        if (cine <= 0.0F) {
            return;
        }
        float time = MaiFx.time(Minecraft.getInstance().getFrameTime());
        float shake = SHAKE_DEG * cine * cine; // square it: tremor, not wobble
        event.setPitch(event.getPitch() + shake * (Mth.sin(time * 53.1F) * 0.6F + Mth.sin(time * 23.7F) * 0.4F));
        event.setYaw(event.getYaw() + shake * (Mth.sin(time * 47.3F + 1.3F) * 0.6F + Mth.sin(time * 19.1F + 0.7F) * 0.4F));
        event.setRoll(event.getRoll() + shake * 0.8F * Mth.sin(time * 41.9F + 2.1F));
    }
}
