package com.kuiba.mai.client;

import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import com.kuiba.Kuiba;
import com.kuiba.mai.MaiTiers;
import com.kuiba.mai.Meridian;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;

/**
 * Draws the glowing 脉门 rings on a player's body, like the anime's 开脉 shot: a halo over the crown,
 * discs on chest, back, shoulders, hands, knees, and the back of the head. Each ring is a flat
 * textured quad anchored to a model part, so it follows arm swing, sneaking, swimming, and sleeping.
 */
public final class MaiGateLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation RING = Kuiba.id("textures/fx/gate_ring.png");
    private static final ResourceLocation CORE = Kuiba.id("textures/fx/gate_core.png");
    /** Small ring size and its distance off the skin, both as fractions of the large ring's radius. */
    private static final float CORE_SCALE = 0.36F;
    private static final float CORE_LIFT = 0.55F;
    private static final ResourceLocation RING_GLOW = Kuiba.id("textures/fx/gate_ring_glow.png");
    private static final ResourceLocation CORE_GLOW = Kuiba.id("textures/fx/gate_core_glow.png");
    /** Bloom strength of a fully lit ring outside any flash. */
    private static final float GLOW_BASE = 0.55F;
    /** Lift off the skin so the quads do not z-fight with the model or its armour. */
    private static final float SURFACE_LIFT = 0.9F;
    private static final int LIGHT = LightTexture.FULL_BRIGHT;
    private static final boolean DEBUG_TRACE = Boolean.getBoolean("kuiba.traceLayer");

    public MaiGateLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
            float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (player.isInvisible() || player.isSpectator()) {
            return;
        }
        MaiFx.View view = MaiFx.view(player);
        if (view == null) {
            return;
        }
        float time = MaiFx.time(partialTick);
        if (DEBUG_TRACE && (MaiClientState.now() % 40L) == 0L) {
            Kuiba.LOGGER.info("[mai-layer] {} gate={} intensity={} fx={}", player.getScoreboardName(), view.gate(), view.intensity(), view.fx() == null ? "-" : view.fx().kind);
        }
        PlayerModel<AbstractClientPlayer> model = getParentModel();
        boolean slim = player.getSkin().model() == PlayerSkin.Model.SLIM;
        for (int gate = 1; gate <= MaiFx.MAX_GATE; gate++) {
            Meridian meridian = MaiTiers.meridian(gate);
            if (meridian == null) {
                continue;
            }
            MaiFx.Ring ring = MaiFx.ring(view, gate, time);
            if (ring == null) {
                continue;
            }
            Anchor anchor = anchor(meridian, model, slim);
            if (anchor == null || !anchor.part().visible) {
                continue;
            }
            draw(poseStack, buffer, anchor, ring);
        }
    }

    private static void draw(PoseStack poseStack, MultiBufferSource buffer, Anchor anchor, MaiFx.Ring ring) {
        poseStack.pushPose();
        anchor.part().translateAndRotate(poseStack);
        float nx = anchor.nx();
        float ny = anchor.ny();
        float nz = anchor.nz();
        poseStack.translate(
                (anchor.x() + nx * SURFACE_LIFT) / 16.0F,
                (anchor.y() + ny * SURFACE_LIFT) / 16.0F,
                (anchor.z() + nz * SURFACE_LIFT) / 16.0F);
        // Quads are built in the XY plane facing +Z; turn +Z onto the anchor normal.
        poseStack.mulPose(new Quaternionf().rotationTo(0.0F, 0.0F, 1.0F, nx, ny, nz));

        float radius = anchor.radius() / 16.0F * ring.scale();
        int alpha = Math.round(255.0F * ring.alpha());
        // Steady bloom while lit, boosted by the 开脉/冲脉 flashes.
        int glow = Math.round(255.0F * Math.min(1.0F, ring.alpha() * (GLOW_BASE + ring.glow() * 0.6F)));

        // Large ring flat on the skin.
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, 0.002F);
        glow(buffer, poseStack.last(), RING_GLOW, radius * 1.1F, glow);
        VertexConsumer main = buffer.getBuffer(RenderType.entityTranslucentEmissive(RING));
        quad(main, poseStack.last(), radius, 255, alpha, false);
        poseStack.popPose();

        // Small ring floating out along the normal, so the pair reads as 3D from the side.
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, radius * CORE_LIFT);
        float coreRadius = radius * CORE_SCALE;
        glow(buffer, poseStack.last(), CORE_GLOW, coreRadius * 1.6F, glow);
        VertexConsumer core = buffer.getBuffer(RenderType.entityTranslucentEmissive(CORE));
        quad(core, poseStack.last(), coreRadius, 255, alpha, false);
        poseStack.popPose();

        poseStack.popPose();
    }

    /**
     * Additive bloom. The eyes type ignores alpha, so the textures are black where empty and the
     * strength goes through the vertex colour; both faces because the type culls.
     */
    private static void glow(MultiBufferSource buffer, PoseStack.Pose pose, ResourceLocation texture, float radius, int strength) {
        if (strength <= 2) {
            return;
        }
        VertexConsumer consumer = buffer.getBuffer(RenderType.eyes(texture));
        quad(consumer, pose, radius, strength, 255, false);
        quad(consumer, pose, radius, strength, 255, true);
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose pose, float s, int shade, int alpha, boolean flip) {
        if (!flip) {
            vertex(consumer, pose, -s, -s, 0.0F, 0.0F, shade, alpha, 1.0F);
            vertex(consumer, pose, -s, s, 0.0F, 1.0F, shade, alpha, 1.0F);
            vertex(consumer, pose, s, s, 1.0F, 1.0F, shade, alpha, 1.0F);
            vertex(consumer, pose, s, -s, 1.0F, 0.0F, shade, alpha, 1.0F);
        } else {
            vertex(consumer, pose, s, -s, 1.0F, 0.0F, shade, alpha, -1.0F);
            vertex(consumer, pose, s, s, 1.0F, 1.0F, shade, alpha, -1.0F);
            vertex(consumer, pose, -s, s, 0.0F, 1.0F, shade, alpha, -1.0F);
            vertex(consumer, pose, -s, -s, 0.0F, 0.0F, shade, alpha, -1.0F);
        }
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float u, float v, int shade, int alpha, float nz) {
        consumer.addVertex(pose, x, y, 0.0F)
                .setColor(shade, shade, shade, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LIGHT)
                .setNormal(pose, 0.0F, 0.0F, nz);
    }

    /**
     * Where each meridian sits, in model-part space (1/16 blocks, +Y down, -Z is the front).
     * Positions match the reference sheet: 比圆 halo over the crown; 暮还松/临缺 over the shoulders;
     * 朝迎风 on the chest; 玄骨/仑泉 on the backs of the hands; 身破虚/承暗 on the knees;
     * 心断空/启明 on the back of the head; 昆海 on the back; 幻血 at the small of the back.
     */
    private static @Nullable Anchor anchor(Meridian meridian, PlayerModel<AbstractClientPlayer> model, boolean slim) {
        float armOuter = slim ? 2.0F : 3.0F;
        return switch (meridian) {
            case BI_YUAN -> new Anchor(model.head, 0.0F, -10.0F, 0.0F, 0.0F, -1.0F, 0.0F, 5.2F);
            case XIN_DUAN_KONG -> new Anchor(model.head, 2.6F, -4.6F, 4.4F, 0.0F, 0.0F, 1.0F, 1.7F);
            case QI_MING -> new Anchor(model.head, -2.6F, -4.6F, 4.4F, 0.0F, 0.0F, 1.0F, 1.7F);
            case CHAO_YING_FENG -> new Anchor(model.body, 0.0F, 4.2F, -2.6F, 0.0F, 0.0F, -1.0F, 4.0F);
            case KUN_HAI -> new Anchor(model.body, 0.0F, 4.6F, 2.6F, 0.0F, 0.0F, 1.0F, 3.8F);
            case HUAN_XUE -> new Anchor(model.body, 0.0F, 10.3F, 2.5F, 0.0F, 0.0F, 1.0F, 1.6F);
            // Shoulder discs hover over the arm tops, tilted outward like the shoulder plates.
            case MU_HUAN_SONG -> new Anchor(model.body, -6.3F, -0.6F, 0.0F, -0.42F, -0.91F, 0.0F, 2.6F);
            case LIN_QUE -> new Anchor(model.body, 6.3F, -0.6F, 0.0F, 0.42F, -0.91F, 0.0F, 2.6F);
            case XUAN_GU -> new Anchor(model.rightArm, -armOuter - 0.2F, 8.3F, 0.0F, -1.0F, 0.0F, 0.0F, 1.6F);
            case LUN_QUAN -> new Anchor(model.leftArm, armOuter + 0.2F, 8.3F, 0.0F, 1.0F, 0.0F, 0.0F, 1.6F);
            case SHEN_PO_XU -> new Anchor(model.rightLeg, 0.0F, 5.6F, -2.6F, 0.0F, 0.0F, -1.0F, 1.9F);
            case CHENG_AN -> new Anchor(model.leftLeg, 0.0F, 5.6F, -2.6F, 0.0F, 0.0F, -1.0F, 1.9F);
        };
    }

    /** Model part, offset in part space, unit normal, and ring radius (all 1/16 units). */
    private record Anchor(ModelPart part, float x, float y, float z, float nx, float ny, float nz, float radius) {}
}
