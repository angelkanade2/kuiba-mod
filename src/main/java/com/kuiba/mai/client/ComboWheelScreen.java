package com.kuiba.mai.client;

import java.util.List;

import com.kuiba.mai.MaiApi;
import com.kuiba.mai.MaiCombo;
import com.kuiba.mai.MaiData;
import com.kuiba.mai.MaiSkill;
import com.kuiba.mai.MaiSkills;
import com.kuiba.mai.payload.SelectComboPayload;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 技能圆盘. Opened by holding the wheel key; the wheel lists every combo placed on it, each with its
 * first skill's icon and its name. Slide onto one and release the key to make it the active combo,
 * which 施放脉技 then casts. Esc, or releasing over empty space, keeps whatever was selected before.
 * <p>
 * Opened as a screen on purpose: vanilla releases the cursor and clears key-mapping state when a screen
 * opens, so the release arrives as {@link #keyReleased} and the mouse is free to pick a slot.
 */
public class ComboWheelScreen extends Screen {
    private static final int RING_RADIUS = 78;
    private static final int TILE = 34;
    private static final int ICON = 16;
    /** Radius below which the cursor counts as "no slot". */
    private static final int DEAD_ZONE = 26;
    /** Name is drawn at this scale inside the tile so long names cannot spill onto a neighbour. */
    private static final float NAME_SCALE = 0.65F;

    private static final int PANEL = 0xF00A0F16;
    private static final int TILE_IDLE = 0xE0141C26;
    private static final int TILE_HOVER = 0xE01E3A4E;
    private static final int TILE_SELECTED = 0xE0203A22;
    private static final int CYAN = 0xFF4FE3FF;
    private static final int CYAN_TEXT = 0xFFCFF6FF;
    private static final int GOLD = 0xFFFFD36A;
    private static final int MUTED = 0xFF7C8A92;
    private static final int LOCKED = 0xFF2A3238;

    private int hovered = MaiCombo.NO_COMBO;
    /** Set once the pick is sent, so the fallback in {@link #tick()} cannot fire twice. */
    private boolean committed;
    private int spin;

    public ComboWheelScreen() {
        super(Component.translatable("screen.kuiba.combo.wheel"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** No vanilla blur or panorama: the wheel draws its own scrim so the HUD stays readable behind it. */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void tick() {
        spin++;
        if (committed) {
            return;
        }
        if (!holdKeyDown()) {
            // The key went up before the release reached us (very fast tap, or a rebind we cannot see).
            commit();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        int width = this.width;
        int height = this.height;
        int cx = width / 2;
        int cy = height / 2;

        graphics.fill(0, 0, width, height, 0x8C04070B);

        MaiData mai = MaiApi.data(minecraft.player);
        List<Integer> entries = MaiCombo.wheelEntries(mai);
        hovered = hitTest(cx, cy, mouseX, mouseY, entries);

        if (entries.isEmpty()) {
            Font font = this.font;
            Component line = Component.translatable("screen.kuiba.combo.wheel_empty");
            graphics.drawCenteredString(font, line, cx, cy - 6, CYAN_TEXT);
            Component hint = Component.translatable("screen.kuiba.combo.wheel_empty_hint", editorKeyName());
            graphics.drawCenteredString(font, hint, cx, cy + 8, MUTED);
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        int n = entries.size();
        for (int i = 0; i < n; i++) {
            int comboIndex = entries.get(i);
            double angle = -Math.PI / 2.0 + i * (Math.PI * 2.0 / n);
            int tx = cx + (int) Math.round(Math.cos(angle) * RING_RADIUS) - TILE / 2;
            int ty = cy + (int) Math.round(Math.sin(angle) * RING_RADIUS) - TILE / 2;
            renderTile(graphics, mai, comboIndex, tx, ty, comboIndex == hovered, comboIndex == mai.selectedCombo());
        }
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        renderCentre(graphics, minecraft, mai, cx, cy, entries);
    }

    private void renderTile(GuiGraphics graphics, MaiData mai, int comboIndex, int x, int y, boolean hover, boolean selected) {
        MaiCombo.Preset preset = mai.combo(comboIndex);
        int fill = hover ? TILE_HOVER : (selected ? TILE_SELECTED : TILE_IDLE);
        graphics.fill(x, y, x + TILE, y + TILE, fill);
        int frame = hover ? GOLD : (selected ? 0xFFFFE08A : CYAN);
        MaiHud.frame(graphics, x, y, TILE, TILE, hover ? frame : MaiHud.withAlpha(frame, selected ? 0.9F : 0.5F));

        List<MaiCombo.Step> steps = preset.steps();
        MaiSkill first = steps.isEmpty() ? null : MaiSkills.get(steps.get(0).skillId());
        int iconX = x + (TILE - ICON) / 2;
        if (first != null) {
            graphics.blit(first.icon(), iconX, y + 3, 0.0F, 0.0F, ICON, ICON, ICON, ICON);
        } else {
            graphics.drawCenteredString(this.font, "?", x + TILE / 2, y + 6, LOCKED);
        }

        // Name, clamped into the tile.
        String name = MaiCombo.displayName(preset).getString();
        String trimmed = this.font.plainSubstrByWidth(name, Math.round((TILE - 4) / NAME_SCALE));
        int nameWidth = this.font.width(trimmed);
        graphics.pose().pushPose();
        graphics.pose().translate(x + TILE / 2.0F, y + TILE - 9.0F, 0.0F);
        graphics.pose().scale(NAME_SCALE, NAME_SCALE, 1.0F);
        graphics.drawString(this.font, trimmed, -nameWidth / 2, 0, hover ? 0xFFFFFFFF : MUTED, false);
        graphics.pose().popPose();

        if (steps.size() > 1) {
            String badge = Integer.toString(steps.size());
            graphics.drawString(this.font, badge, x + TILE - 6, y + 2, GOLD, false);
        }
    }

    private void renderCentre(GuiGraphics graphics, Minecraft minecraft, MaiData mai, int cx, int cy, List<Integer> entries) {
        int hoveredIndex = hovered == MaiCombo.NO_COMBO ? MaiCombo.NO_COMBO : hovered;
        Component title;
        int color;
        if (hoveredIndex != MaiCombo.NO_COMBO) {
            MaiCombo.Preset preset = mai.combo(hoveredIndex);
            title = MaiCombo.displayName(preset);
            color = GOLD;
        } else if (mai.selectedCombo() != MaiCombo.NO_COMBO) {
            title = Component.translatable("screen.kuiba.combo.wheel_current",
                    MaiCombo.displayName(mai.combo(mai.selectedCombo())));
            color = CYAN_TEXT;
        } else {
            title = Component.translatable("screen.kuiba.combo.wheel_pick");
            color = MUTED;
        }
        graphics.drawCenteredString(this.font, title, cx, cy - 4, color);

        Component hint = hoveredIndex != MaiCombo.NO_COMBO
                ? Component.translatable("screen.kuiba.combo.wheel_release")
                : Component.translatable("screen.kuiba.combo.wheel_hint");
        graphics.drawCenteredString(this.font, hint, cx, cy + 10, MUTED);

        // Slow sweep so the ring reads as live while held.
        float pulse = 0.5F + 0.5F * Mth.sin(spin * 0.15F);
        int ringColor = MaiHud.withAlpha(CYAN, 0.15F + 0.25F * pulse);
        for (int i = 0; i < entries.size(); i++) {
            double angle = -Math.PI / 2.0 + i * (Math.PI * 2.0 / entries.size()) + spin * 0.004D;
            MaiHud.frame(graphics,
                    cx + (int) Math.round(Math.cos(angle) * (RING_RADIUS + TILE / 2 + 4)) - 1,
                    cy + (int) Math.round(Math.sin(angle) * (RING_RADIUS + TILE / 2 + 4)) - 1,
                    2, 2, ringColor);
        }
    }

    /** Nearest wheel slot to the cursor, or {@link MaiCombo#NO_COMBO} inside the dead zone. */
    private int hitTest(int cx, int cy, double mouseX, double mouseY, List<Integer> entries) {
        if (entries.isEmpty()) {
            return MaiCombo.NO_COMBO;
        }
        double dx = mouseX - cx;
        double dy = mouseY - cy;
        if (Math.sqrt(dx * dx + dy * dy) < DEAD_ZONE) {
            return MaiCombo.NO_COMBO;
        }
        double angle = Math.atan2(dy, dx) + Math.PI / 2.0;
        if (angle < 0.0D) {
            angle += Math.PI * 2.0D;
        }
        int n = entries.size();
        int index = (int) Math.round(angle / (Math.PI * 2.0D / n)) % n;
        return entries.get(index);
    }

    private void commit() {
        if (committed) {
            return;
        }
        committed = true;
        if (hovered != MaiCombo.NO_COMBO) {
            PacketDistributor.sendToServer(new SelectComboPayload(hovered));
        }
        close();
    }

    private void close() {
        this.committed = true;
        if (this.minecraft != null) {
            this.minecraft.setScreen(null);
        }
    }

    @Override
    public void onClose() {
        close();
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (MaiKeys.COMBO_WHEEL.matches(keyCode, scanCode)) {
            commit();
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (MaiKeys.COMBO_WHEEL.matches(keyCode, scanCode)) {
            // Auto-repeat while held: swallow it instead of letting it reach the game.
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return true;
    }

    /** True while the physical wheel key is still held. False for non-keyboard binds we cannot poll. */
    private static boolean holdKeyDown() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getWindow() == null) {
            return false;
        }
        var key = MaiKeys.COMBO_WHEEL.getKey();
        if (key.getType() != com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM) {
            return true;
        }
        return com.mojang.blaze3d.platform.InputConstants.isKeyDown(
                minecraft.getWindow().getWindow(), key.getValue());
    }

    private static String editorKeyName() {
        return MaiKeys.COMBO_EDITOR.getTranslatedKeyMessage().getString();
    }
}
