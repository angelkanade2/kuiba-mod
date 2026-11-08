package com.kuiba.mai.client;

import com.kuiba.Kuiba;
import com.kuiba.mai.MaiTiers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import com.kuiba.mai.Meridian;
import com.kuiba.mai.payload.MaiFxPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.RenderLivingEvent;

/**
 * Draws the 脉门 rings on 特殊怪 with the exact same anchors, textures and animation a player gets.
 * <p>
 * {@code RenderLivingEvent.Post} fires after {@code LivingEntityRenderer} has already popped its pose
 * stack, so the model-space transform is rebuilt here before handing over to {@link MaiGateGeometry}.
 * Only humanoid models are handled — every gate host (zombie, skeleton, piglin, enderman) is one.
 */
@Mod.EventBusSubscriber(modid = Kuiba.MOD_ID, value = Dist.CLIENT)
public final class MaiGateHostRenderer {

    /** Same id the server-side health buff uses; attributes are synced, so the client can spot a host. */
    private static final java.util.UUID HEALTH_MODIFIER_ID = com.kuiba.core.MaiGateHosts.HEALTH_MODIFIER_ID;

    private static boolean isGateHost(LivingEntity entity) {
        var health = entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
        return health != null && health.getModifier(HEALTH_MODIFIER_ID) != null;
    }

    private MaiGateHostRenderer() {}

    @SubscribeEvent
    public static void onRender(RenderLivingEvent.Post<LivingEntity, EntityModel<LivingEntity>> event) {
        LivingEntity entity = event.getEntity();
        if (entity.isInvisible()) {
            return;
        }
        if (!isGateHost(entity)) {
            return;
        }
        MaiClientState.Fx fx = MaiClientState.fx(entity.getId());
        if (fx == null || fx.kind != MaiFxPayload.KIND_OPEN || fx.gate <= 0) {
            return;
        }
        if (!(event.getRenderer().getModel() instanceof HumanoidModel<?> model)) {
            return;
        }

        float partial = event.getPartialTick();
        float time = MaiFx.time(partial);
        // The mob's own opening animation: every gate it owns lights up in sequence, then fades out.
        MaiFx.View view = new MaiFx.View(fx.gate, 1.0F, fx, -1.0F, false);

        PoseStack pose = event.getPoseStack();
        MultiBufferSource buffer = event.getMultiBufferSource();
        pose.pushPose();
        // Rebuild what LivingEntityRenderer applied before rendering the model.
        float bodyRot = Mth.rotLerp(partial, entity.yBodyRotO, entity.yBodyRot);
        float scale = 1.0F;
        pose.scale(scale, scale, scale);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - bodyRot));
        pose.scale(-1.0F, -1.0F, 1.0F);
        if (entity.isBaby()) {
            pose.scale(0.5F, 0.5F, 0.5F);
        }
        pose.translate(0.0F, -1.501F, 0.0F);

        for (int gate = 1; gate <= fx.gate; gate++) {
            Meridian meridian = MaiTiers.meridian(gate);
            if (meridian == null) {
                continue;
            }
            MaiFx.Ring ring = MaiFx.ring(view, gate, time);
            if (ring == null || ring.alpha() <= 0.01F) {
                continue;
            }
            MaiGateGeometry.Anchor anchor = MaiGateGeometry.anchor(meridian, model, false);
            if (anchor == null || !anchor.part().visible) {
                continue;
            }
            MaiGateGeometry.draw(pose, buffer, anchor, ring);
        }
        pose.popPose();
    }
}
