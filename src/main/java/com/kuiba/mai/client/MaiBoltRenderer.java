package com.kuiba.mai.client;

import org.joml.Quaternionf;

import com.kuiba.Kuiba;
import com.kuiba.entity.MaiBolt;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * 脉弹 as a solid knot of mai: a bright orb with additive bloom facing the camera, and a 脉门 ring
 * set square to the flight path, spinning, so the bolt reads as a gate thrown forward.
 */
public class MaiBoltRenderer extends EntityRenderer<MaiBolt> {
    private static final ResourceLocation ORB = Kuiba.id("textures/fx/mai_orb.png");
    private static final ResourceLocation ORB_GLOW = Kuiba.id("textures/fx/mai_orb_glow.png");
    private static final ResourceLocation RING = Kuiba.id("textures/fx/gate_ring.png");
    private static final ResourceLocation RING_GLOW = Kuiba.id("textures/fx/gate_ring_glow.png");
    private static final int LIGHT = LightTexture.FULL_BRIGHT;

    public MaiBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(MaiBolt bolt, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float time = bolt.tickCount + partialTick;
        float size = bolt.visualSize() * (1.0F + 0.08F * Mth.sin(time * 0.9F));
        int glow = 150 + 35 * bolt.charge();

        poseStack.pushPose();
        poseStack.translate(0.0F, bolt.getBbHeight() * 0.5F, 0.0F);

        poseStack.pushPose();
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        quad(buffer.getBuffer(RenderType.eyes(ORB_GLOW)), poseStack.last(), size * 2.6F, glow, 255, true);
        quad(buffer.getBuffer(RenderType.entityTranslucentEmissive(ORB)), poseStack.last(), size, 255, 255, false);
        poseStack.popPose();

        Vec3 motion = bolt.getDeltaMovement();
        if (motion.lengthSqr() > 1.0E-6D) {
            Vec3 dir = motion.normalize();
            poseStack.pushPose();
            poseStack.mulPose(new Quaternionf().rotationTo(0.0F, 0.0F, 1.0F, (float) dir.x, (float) dir.y, (float) dir.z));
            poseStack.mulPose(Axis.ZP.rotationDegrees(time * 24.0F));
            float ringSize = size * 2.0F;
            quad(buffer.getBuffer(RenderType.eyes(RING_GLOW)), poseStack.last(), ringSize * 1.1F, glow, 255, true);
            quad(buffer.getBuffer(RenderType.entityTranslucentEmissive(RING)), poseStack.last(), ringSize, 255, 230, false);
            poseStack.popPose();
        }

        poseStack.popPose();
        super.render(bolt, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(MaiBolt bolt) {
        return ORB;
    }

    /** Centred square; {@code twoSided} adds the back face for culling render types. */
    private static void quad(VertexConsumer consumer, PoseStack.Pose pose, float s, int shade, int alpha, boolean twoSided) {
        face(consumer, pose, s, shade, alpha, 1.0F);
        if (twoSided) {
            face(consumer, pose, s, shade, alpha, -1.0F);
        }
    }

    private static void face(VertexConsumer consumer, PoseStack.Pose pose, float s, int shade, int alpha, float nz) {
        float[][] corners = nz > 0.0F
                ? new float[][] {{-s, -s, 0, 1}, {s, -s, 1, 1}, {s, s, 1, 0}, {-s, s, 0, 0}}
                : new float[][] {{-s, s, 0, 0}, {s, s, 1, 0}, {s, -s, 1, 1}, {-s, -s, 0, 1}};
        for (float[] c : corners) {
            consumer.addVertex(pose, c[0], c[1], 0.0F)
                    .setColor(shade, shade, shade, alpha)
                    .setUv(c[2], c[3])
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(LIGHT)
                    .setNormal(pose, 0.0F, 0.0F, nz);
        }
    }
}
