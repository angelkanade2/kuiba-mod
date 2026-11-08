package com.kuiba.mai.client;

import org.joml.Quaternionf;

import com.kuiba.Kuiba;
import com.kuiba.entity.MaiBolt;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * 脉弹. With a weapon in hand the bolt renders as that item, tip-first along the flight path; empty
 * hand falls back to the mai orb — a bright core with additive bloom facing the camera.
 */
public class MaiBoltRenderer extends EntityRenderer<MaiBolt> {
    private static final ResourceLocation ORB = Kuiba.id("textures/fx/mai_orb.png");
    private static final ResourceLocation ORB_GLOW = Kuiba.id("textures/fx/mai_orb_glow.png");
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

        ItemStack stack = bolt.visualItem();
        if (!stack.isEmpty()) {
            // 御器: the weapon flies tip-first along its path; on the circle it stands upright, turning.
            poseStack.pushPose();
            Vec3 motion = bolt.getDeltaMovement();
            if (motion.lengthSqr() > 1.0E-6D) {
                Vec3 dir = motion.normalize();
                // Item models render pointing up-right in FIXED; the -45° straightens them to +Y,
                // which the outer rotation then lays onto the flight direction.
                poseStack.mulPose(new Quaternionf().rotationTo(0.0F, 1.0F, 0.0F, (float) dir.x, (float) dir.y, (float) dir.z));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-45.0F));
            } else {
                poseStack.mulPose(Axis.YP.rotationDegrees(time * 20.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-45.0F));
            }
            poseStack.scale(0.7F, 0.7F, 0.7F);
            Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED,
                    LIGHT, OverlayTexture.NO_OVERLAY, poseStack, buffer, bolt.level(), 0);
            poseStack.popPose();
        } else {
            // Plain mai orb: bright core with additive bloom facing the camera.
            poseStack.pushPose();
            poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
            quad(buffer.getBuffer(RenderType.eyes(ORB_GLOW)), poseStack.last(), size * 2.6F, glow, 255, true);
            quad(buffer.getBuffer(RenderType.entityTranslucentEmissive(ORB)), poseStack.last(), size, 255, 255, false);
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
            consumer.vertex(pose.pose(), c[0], c[1], 0.0F)
                    .color(shade, shade, shade, alpha)
                    .uv(c[2], c[3])
                    .overlayCoords(OverlayTexture.NO_OVERLAY)
                    .uv2(LIGHT)
                    .normal(pose.normal(), 0.0F, 0.0F, nz)
                    .endVertex();
        }
    }
}
