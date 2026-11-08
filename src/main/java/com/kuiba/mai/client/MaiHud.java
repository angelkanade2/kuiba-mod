package com.kuiba.mai.client;

import net.minecraft.util.Mth;
import com.kuiba.Kuiba;
import com.kuiba.mai.MaiApi;
import com.kuiba.mai.MaiData;
import com.kuiba.mai.MaiTiers;
import com.kuiba.mai.Meridian;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;

/**
 * Mai HUD above the hunger row, bottom to top: the pool bar, a row of twelve 脉门 marks that light with
 * the body rings, the gate label, and the 冲脉 progress bar. 开脉 and 冲脉 also tint the screen edges for
 * the local player, who cannot see their own body rings in first person.
 */
public final class MaiHud {
    private static final ResourceLocation GATE_ON = Kuiba.id("textures/hud/gate_on.png");
    private static final ResourceLocation GATE_OFF = Kuiba.id("textures/hud/gate_off.png");
    private static final ResourceLocation VIGNETTE = Kuiba.id("textures/hud/mai_vignette.png");

    private static final int BAR_W = 96;
    private static final int BAR_H = 7;
    private static final int DOT = 8;

    private static final int CYAN = 0xFF4FE3FF;
    private static final int CYAN_TEXT = 0xFFCFF6FF;
    private static final int PANEL = 0xC0061018;
    private static final int PANEL_LINE = 0xFF10303C;
    private static final int LOW = 0xFFFF7A5C;
    private static final int LOW_DARK = 0xFFB83A2A;
    private static final int GOLD = 0xFFFFD36A;

    private MaiHud() {}

    /**
     * Sits above air and vehicle health so it can stack on {@code Gui.rightHeight}, then reserves its own
     * height there. Forge lifts the held-item name above that height.
     */
    public static void register(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.AIR_LEVEL.id(), "mai_hud", MaiHud::render);
    }

    /**
     * Bottom edge for a stack that starts at {@code stackHeight} ({@code Gui.leftHeight} or {@code rightHeight}).
     * Never lower than one row above the food bar, so the experience level stays clear in creative.
     */
    static int stackBottom(int screenHeight, int stackHeight) {
        return screenHeight - Math.max(stackHeight, 49) + 6;
    }

    /**
     * New {@code Gui.leftHeight}/{@code rightHeight} after drawing down from {@code top}. Without hearts the
     * held-item name drops 14 px, so that case reserves 14 px more.
     */
    static int reserve(Minecraft minecraft, int screenHeight, int top) {
        int height = screenHeight - top + 10;
        if (minecraft.gameMode != null && !minecraft.gameMode.canHurtPlayer()) {
            height += 14;
        }
        return height;
    }

    private static void render(net.minecraftforge.client.gui.overlay.ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || minecraft.player.isSpectator()) {
            return;
        }
        MaiData mai = MaiApi.data(minecraft.player);
        float time = MaiFx.time(partialTick);
        MaiClientState.Fx fx = MaiClientState.fx(minecraft.player.getId());
        MaiFx.View view = MaiFx.view(minecraft.player);

        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();

        renderFlash(graphics, fx, time, width, height);

        int right = width / 2 + 91;
        int barX = right - BAR_W;
        net.minecraftforge.client.gui.overlay.ForgeGui forgeGui = (net.minecraftforge.client.gui.overlay.ForgeGui) minecraft.gui;
        int barY = stackBottom(height, forgeGui.rightHeight) - BAR_H - 1;
        int dotsY = barY - DOT - 1;
        int labelY = dotsY - 10;
        int ritualY = labelY - 10;
        int top = MaiClientState.ritualActive() ? ritualY - 11 : labelY - 1;
        forgeGui.rightHeight = reserve(minecraft, height, top);

        float max = Math.max(1.0F, mai.shownMax());
        float ratio = Mth.clamp(mai.current() / max, 0.0F, 1.0F);
        boolean closed = ratio <= 0.001F;
        boolean low = !closed && ratio < 0.25F;

        renderBar(graphics, barX, barY, ratio, closed, low, time);
        renderGateDots(graphics, view, mai.gate(), barX, dotsY, time);
        renderValue(graphics, minecraft, mai, barX, labelY, closed, low, max);

        if (MaiClientState.ritualActive()) {
            int fill = Math.round(BAR_W * Mth.clamp(MaiClientState.progress(), 0.0F, 1.0F));
            graphics.fill(barX - 1, ritualY - 1, barX + BAR_W + 1, ritualY + 4, PANEL);
            graphics.fill(barX, ritualY, barX + fill, ritualY + 3, GOLD);
            Meridian next = MaiTiers.meridian(mai.gate() + 1);
            Component text = next == null
                    ? Component.translatable("hud.kuiba.ritual")
                    : Component.translatable("hud.kuiba.ritual_target", Component.translatable(next.nameKey()));
            graphics.drawString(minecraft.font, text, barX, ritualY - 10, GOLD, true);
        }
    }

    private static void renderFlash(GuiGraphics graphics, MaiClientState.Fx fx, float time, int width, int height) {
        float flash = MaiFx.screenFlash(fx, time);
        float cine = MaiFx.cinematic(fx, time);
        if (flash <= 0.01F && cine <= 0.01F) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        // Dark edges under the flash: the world closes in, then the cyan flash blooms through.
        if (cine > 0.01F) {
            graphics.setColor(0.0F, 0.0F, 0.0F, 0.5F * cine);
            graphics.blit(VIGNETTE, 0, 0, width, height, 0.0F, 0.0F, 64, 64, 64, 64);
        }
        if (flash > 0.01F) {
            graphics.setColor(1.0F, 1.0F, 1.0F, flash);
            graphics.blit(VIGNETTE, 0, 0, width, height, 0.0F, 0.0F, 64, 64, 64, 64);
        }
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void renderBar(GuiGraphics graphics, int x, int y, float ratio, boolean closed, boolean low, float time) {
        graphics.fill(x - 1, y - 1, x + BAR_W + 1, y + BAR_H + 1, PANEL);
        graphics.fill(x, y, x + BAR_W, y + BAR_H, 0xFF0B1A24);
        int inner = BAR_W - 2;
        int fill = Math.round(inner * ratio);
        if (fill > 0) {
            int top = low ? LOW : 0xFFA6F7FF;
            int bottom = low ? LOW_DARK : 0xFF1FB8E8;
            graphics.fillGradient(x + 1, y + 1, x + 1 + fill, y + BAR_H - 1, top, bottom);
            // Thin highlight along the top edge and a drifting shimmer.
            graphics.fill(x + 1, y + 1, x + 1 + fill, y + 2, 0x80FFFFFF);
            if (fill > 6) {
                int shimmer = (int) ((time * 1.6F) % fill);
                int sx = x + 1 + shimmer;
                graphics.fill(sx, y + 1, Math.min(sx + 3, x + 1 + fill), y + BAR_H - 1, 0x55FFFFFF);
            }
        }
        int frame = closed ? PANEL_LINE : (low ? LOW : withAlpha(CYAN, 0.55F + 0.45F * ratio));
        frame(graphics, x, y, BAR_W, BAR_H, frame);
    }

    private static void renderGateDots(GuiGraphics graphics, MaiFx.View view, int gate, int x, int y, float time) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        for (int g = 1; g <= MaiFx.MAX_GATE; g++) {
            int dx = x + (g - 1) * DOT;
            boolean opened = g <= gate;
            MaiFx.Ring ring = view == null ? null : MaiFx.ring(view, g, time);
            if (ring != null) {
                float a = ring.alpha();
                float boost = Math.min(1.0F, ring.glow() * 0.5F);
                graphics.setColor(0.31F + 0.69F * boost, 0.89F + 0.11F * boost, 1.0F, a);
                graphics.blit(GATE_ON, dx, y, 0.0F, 0.0F, DOT, DOT, DOT, DOT);
                if (ring.scale() > 1.15F) {
                    // Flash: echo a larger ghost ring around the mark.
                    graphics.setColor(1.0F, 1.0F, 1.0F, a * 0.5F);
                    graphics.blit(GATE_OFF, dx - 1, y - 1, DOT + 2, DOT + 2, 0.0F, 0.0F, DOT, DOT, DOT, DOT);
                }
            } else if (opened) {
                // Opened but dormant (pool closed).
                graphics.setColor(0.12F, 0.44F, 0.53F, 0.9F);
                graphics.blit(GATE_ON, dx, y, 0.0F, 0.0F, DOT, DOT, DOT, DOT);
            } else {
                float a = g > 6 ? 0.18F : 0.3F;
                graphics.setColor(1.0F, 1.0F, 1.0F, a);
                graphics.blit(GATE_OFF, dx, y, 0.0F, 0.0F, DOT, DOT, DOT, DOT);
            }
        }
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** 脉值 current/max，画在脉门点上方一行（门阶文字已去掉）. */
    private static void renderValue(GuiGraphics graphics, Minecraft minecraft, MaiData mai, int x, int y, boolean closed, boolean low, float max) {
        Component text = closed
                ? Component.translatable("hud.kuiba.closed")
                : Component.literal(Math.round(mai.current()) + "/" + Math.round(max));
        int color = closed ? 0xFF7C8A92 : (low ? LOW : 0xFF8CF3FF);
        int w = minecraft.font.width(text);
        graphics.drawString(minecraft.font, text, x + BAR_W - w, y, color, true);
    }

    static void frame(GuiGraphics graphics, int x, int y, int w, int h, int color) {
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y + 1, x + 1, y + h - 1, color);
        graphics.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    static int withAlpha(int rgb, float alpha) {
        int a = Math.round(255.0F * Mth.clamp(alpha, 0.0F, 1.0F));
        return (a << 24) | (rgb & 0x00FFFFFF);
    }
}
