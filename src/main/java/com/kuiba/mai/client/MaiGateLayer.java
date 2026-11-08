package com.kuiba.mai.client;

import com.kuiba.Kuiba;
import com.kuiba.mai.MaiTiers;
import com.kuiba.mai.Meridian;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

/**
 * Draws the glowing 脉门 rings on a player's body, like the anime's 开脉 shot: a halo over the crown,
 * discs on chest, back, shoulders, hands, knees, and the back of the head. Each ring is a flat
 * textured quad anchored to a model part, so it follows arm swing, sneaking, swimming, and sleeping.
 * <p>
 * The geometry lives in {@link MaiGateGeometry} so 特殊怪 can draw at the exact same body positions.
 */
public final class MaiGateLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
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
        boolean slim = "slim".equals(player.getModelName());
        for (int gate = 1; gate <= MaiFx.MAX_GATE; gate++) {
            Meridian meridian = MaiTiers.meridian(gate);
            if (meridian == null) {
                continue;
            }
            MaiFx.Ring ring = MaiFx.ring(view, gate, time);
            if (ring == null) {
                continue;
            }
            MaiGateGeometry.Anchor anchor = MaiGateGeometry.anchor(meridian, model, slim);
            if (anchor == null || !anchor.part().visible) {
                continue;
            }
            MaiGateGeometry.draw(poseStack, buffer, anchor, ring);
        }
    }
}
