package com.kuiba.mai.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.kuiba.mai.MaiApi;
import com.kuiba.mai.MaiCombo;
import com.kuiba.mai.MaiData;
import com.kuiba.mai.MaiSkill;
import com.kuiba.mai.MaiSkills;
import com.kuiba.mai.payload.FixedComboPayload;
import com.kuiba.mai.payload.SaveComboPayload;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import com.kuiba.core.KuibaNetwork;

/**
 * Combo editor. Left column: the eight named presets, each with a 圆盘 marker. Right column: the
 * selected preset's name, 倍率 / 序列 mode, up to three steps with their own charge, the gap between
 * steps, and a 放入圆盘 toggle. Edits stay local until 保存 sends one preset; the server re-checks the
 * gate, unlocks, charge limits, and trims the name.
 */
public class MaiComboScreen extends Screen {
    private static final int PANEL_W = 400;
    private static final int PANEL_H = 264;
    private static final int ROW_W = 132;
    private static final int ROW_H = 20;
    private static final int ROW_PITCH = 22;
    private static final int BOX_W = 44;
    private static final int BOX_H = 40;
    private static final int BOX_PITCH = 46;
    private static final int CHARGE_W = 30;
    /** Fixed-combo selector chips (which preset the X key casts). */
    private static final int FIXED_CHIP_W = 26;
    private static final int FIXED_CHIP_PITCH = 28;
    private static final int CHARGE_PITCH = 32;
    private static final int CELL = 20;
    private static final int GRID_COLS = 11;
    private static final int GRID_ROWS = 2;
    private static final int GAP_STEP = 2;
    private static final int GAP_MAX = 60;
    private static final int BUTTON_H = 18;

    private static final int CYAN_TEXT = 0xFFCFF6FF;
    private static final int MUTED = 0xFF7C8A92;
    private static final int GOLD = 0xFFFFD36A;
    private static final int LOW = 0xFFFF7A5C;
    private static final int ROW_IDLE = 0xFF182028;
    private static final int ROW_HOVER = 0xFF243446;
    private static final int ROW_ACTIVE = 0xFF244868;

    private final List<List<MaiCombo.Step>> work = new ArrayList<>();
    private final int[] gaps = new int[MaiCombo.COMBO_COUNT];
    private final String[] names = new String[MaiCombo.COMBO_COUNT];
    private final boolean[] onWheel = new boolean[MaiCombo.COMBO_COUNT];
    private int slot;
    /** Step box the 倍率 chips act on. */
    private int activeStep;
    @Nullable
    private EditBox nameBox;
    @Nullable
    private Component status;
    private int statusColor = MUTED;

    public MaiComboScreen() {
        super(Component.translatable("screen.kuiba.combo.title"));
    }

    @Override
    protected void init() {
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        // Data is loaded once; a resize re-runs init() and must only rebuild the widget.
        if (work.isEmpty()) {
            MaiData mai = MaiApi.data(this.minecraft.player);
            for (int i = 0; i < MaiCombo.COMBO_COUNT; i++) {
                MaiCombo.Preset preset = mai.combo(i);
                work.add(new ArrayList<>(preset.steps()));
                gaps[i] = preset.gapTicks();
                names[i] = preset.name();
                onWheel[i] = preset.onWheel();
            }
        }
        EditBox box = new EditBox(this.font, nameBoxX(), nameBoxY(), 160, 16, Component.translatable("screen.kuiba.combo.name"));
        box.setMaxLength(MaiCombo.NAME_MAX * 2);
        box.setHint(Component.translatable("screen.kuiba.combo.name_hint").withStyle(style -> style.withColor(MUTED)));
        box.setValue(names[slot] == null ? "" : names[slot]);
        box.setResponder(value -> names[slot] = value);
        nameBox = box;
        addRenderableWidget(box);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        graphics.fill(0, 0, this.width, this.height, 0x88080C12);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        if (this.minecraft == null || this.minecraft.player == null || work.isEmpty()) {
            return;
        }
        MaiData mai = MaiApi.data(this.minecraft.player);
        int left = left();
        int top = top();
        graphics.fill(left, top, left + PANEL_W, top + PANEL_H, 0xFF10141C);
        graphics.fill(left, top, left + PANEL_W, top + 2, 0xFF3A6EA5);
        graphics.drawString(this.font, this.title, left + 12, top + 7, 0xFFE7F6FF, false);

        Component gateLine = Component.translatable("screen.kuiba.combo.gate", mai.gate());
        graphics.drawString(this.font, gateLine, left + PANEL_W - 12 - this.font.width(gateLine), top + 7, MUTED, false);

        List<Component> tooltip = null;

        // ---- left: preset list ----
        graphics.drawString(this.font, Component.translatable("screen.kuiba.combo.list"), left + 12, top + 24, 0xFF9AD0FF, false);
        for (int i = 0; i < MaiCombo.COMBO_COUNT; i++) {
            int rx = left + 12;
            int ry = rowY(i);
            boolean active = i == slot;
            boolean hover = inside(mouseX, mouseY, rx, ry, ROW_W, ROW_H);
            graphics.fill(rx, ry, rx + ROW_W, ry + ROW_H, active ? ROW_ACTIVE : (hover ? ROW_HOVER : ROW_IDLE));
            if (active) {
                graphics.fill(rx, ry, rx + 2, ry + ROW_H, 0xFF4FE3FF);
            }
            String name = names[i] == null ? "" : names[i];
            if (name.isEmpty()) {
                MaiCombo.Preset saved = mai.combo(i);
                name = saved.empty()
                        ? Component.translatable("hud.kuiba.combo.empty").getString()
                        : MaiCombo.displayName(saved).getString();
            }
            graphics.drawString(this.font, Integer.toString(i + 1), rx + 5, ry + 6, active ? 0xFFFFFFFF : MUTED, false);
            graphics.drawString(this.font, this.font.plainSubstrByWidth(name, ROW_W - 48), rx + 16, ry + 6,
                    active ? 0xFFFFFFFF : CYAN_TEXT, false);
            // 圆盘 marker
            int dotX = rx + ROW_W - 10;
            int dotColor = onWheel[i] ? (work.get(i).isEmpty() ? LOW : GOLD) : 0xFF2A3238;
            graphics.fill(dotX, ry + 6, dotX + 6, ry + 12, dotColor);
            if (dirty(mai, i)) {
                graphics.drawString(this.font, "*", dotX - 8, ry + 6, GOLD, false);
            }
            if (hover) {
                tooltip = new ArrayList<>();
                tooltip.add(Component.translatable("screen.kuiba.combo.slot", i + 1,
                        i == 1 ? MaiKeys.comboKeyName(1) : MaiKeys.castKeyName()));
                tooltip.add(Component.translatable(work.get(i).isEmpty() ? "hud.kuiba.combo.empty" : "screen.kuiba.combo.preview_sequence",
                        Math.round(MaiCombo.totalCost(preset(i))), Math.round(mai.current())).withStyle(style -> style.withColor(MUTED)));
            }
        }

        // ---- 固定连招（X 键）: 选哪一条被固定键施放 ----
        int fixedY = top() + 212;
        String fixedLabel = Component.translatable("screen.kuiba.combo.fixed", MaiKeys.comboKeyName(1)).getString();
        graphics.drawString(this.font, fixedLabel, left + 12, fixedY + 5, 0xFF9AD0FF, false);
        for (int i = 0; i < MaiCombo.COMBO_COUNT; i++) {
            int cx = fixedChipX(i);
            boolean active = mai.fixedCombo() == i;
            boolean hover = inside(mouseX, mouseY, cx, fixedY, FIXED_CHIP_W, 16);
            graphics.fill(cx, fixedY, cx + FIXED_CHIP_W, fixedY + 16, active ? 0xFF6A4C0F : (hover ? ROW_HOVER : ROW_IDLE));
            if (active) {
                graphics.fill(cx, fixedY, cx + FIXED_CHIP_W, fixedY + 1, GOLD);
            }
            graphics.drawCenteredString(this.font, Integer.toString(i + 1), cx + FIXED_CHIP_W / 2, fixedY + 4,
                    active ? GOLD : CYAN_TEXT);
            if (hover) {
                MaiCombo.Preset candidate = preset(i);
                tooltip = new ArrayList<>();
                tooltip.add(Component.translatable("screen.kuiba.combo.fixed_this", i + 1).withStyle(style -> style.withColor(GOLD)));
                tooltip.add(Component.translatable(candidate.steps().isEmpty() ? "hud.kuiba.combo.empty" : "screen.kuiba.combo.preview_sequence",
                        Math.round(MaiCombo.totalCost(candidate)), Math.round(mai.current())).withStyle(style -> style.withColor(MUTED)));
            }
        }

        // ---- right: editor ----
        // Name box is a widget; frame it so it reads as part of the panel.
        MaiHud.frame(graphics, nameBoxX() - 1, nameBoxY() - 1, 162, 18, 0xFF3A6EA5);

        // 连招只有一种形态：最多 5 步的序列.
        graphics.drawString(this.font, Component.translatable("screen.kuiba.combo.desc_sequence"),
                rightX(), top() + 48, MUTED, false);

        List<MaiCombo.Step> steps = work.get(slot);
        int boxes = MaiCombo.MAX_STEPS;
        if (activeStep >= steps.size()) {
            activeStep = Math.max(0, steps.size() - 1);
        }
        for (int i = 0; i < boxes; i++) {
            List<Component> tip = renderBox(graphics, mai, steps, i, boxX(i), boxY(), mouseX, mouseY);
            if (tip != null) {
                tooltip = tip;
            }
            if (i + 1 < boxes) {
                int ax = boxX(i) + BOX_W;
                int color = i + 1 < steps.size() ? GOLD : 0xFF2A3238;
                graphics.fill(ax + 2, boxY() + 16, ax + 3, boxY() + 24, color);
                graphics.fill(ax + 1, boxY() + 17, ax + 2, boxY() + 23, color);
            }
        }

        // 倍率 chips act on the selected step; no gate check, only the pool limits what saves.
        if (!steps.isEmpty()) {
            int cy = chargeY();
            graphics.drawString(this.font, Component.translatable("screen.kuiba.combo.charge"), rightX(), cy + 4, CYAN_TEXT, false);
            int current = steps.get(Math.min(activeStep, steps.size() - 1)).charge();
            for (int c = 0; c < MaiCombo.CHARGE_LEVELS.length; c++) {
                int level = MaiCombo.CHARGE_LEVELS[c];
                int cx = chargeX(c);
                boolean selected = level == current;
                boolean hover = inside(mouseX, mouseY, cx, cy, CHARGE_W, 14);
                graphics.fill(cx, cy, cx + CHARGE_W, cy + 14, selected ? 0xFF8A6A1E : (hover ? ROW_HOVER : ROW_IDLE));
                graphics.drawCenteredString(this.font, "×" + level, cx + CHARGE_W / 2, cy + 3, selected ? 0xFFFFFFFF : CYAN_TEXT);
                if (hover) {
                    tooltip = new ArrayList<>();
                    tooltip.add(Component.translatable("screen.kuiba.combo.charge_tip", level,
                            String.format(Locale.ROOT, "%.1f", MaiCombo.costMultiplier(level)),
                            String.format(Locale.ROOT, "%.2f", MaiCombo.effectMultiplier(level))));
                }
            }
        }

        int rowY = gapRowY();
        {
            graphics.drawString(this.font, Component.translatable("screen.kuiba.combo.gap"), rightX(), rowY + 3, CYAN_TEXT, false);
            smallButton(graphics, "-", gapMinusX(), rowY, mouseX, mouseY, gaps[slot] > 0);
            String seconds = String.format(Locale.ROOT, "%.2f", gaps[slot] / 20.0F);
            graphics.drawCenteredString(this.font, Component.translatable("screen.kuiba.combo.seconds", seconds), gapMinusX() + 34, rowY + 3, 0xFFFFFFFF);
            smallButton(graphics, "+", gapMinusX() + 56, rowY, mouseX, mouseY, gaps[slot] < GAP_MAX);
        }

        // 放入圆盘 toggle
        int wheelX = left + PANEL_W - 12 - 78;
        boolean wheelHover = inside(mouseX, mouseY, wheelX, rowY, 78, 14);
        boolean placed = onWheel[slot] && !steps.isEmpty();
        graphics.fill(wheelX, rowY, wheelX + 78, rowY + 14, placed ? 0xFF8A6A1E : (wheelHover ? ROW_HOVER : ROW_IDLE));
        Component wheelLabel = Component.translatable(placed ? "screen.kuiba.combo.on_wheel" : "screen.kuiba.combo.put_wheel");
        graphics.drawCenteredString(this.font, wheelLabel, wheelX + 39, rowY + 3, placed ? 0xFFFFFFFF : CYAN_TEXT);
        if (wheelHover) {
            tooltip = new ArrayList<>();
            tooltip.add(Component.translatable("screen.kuiba.combo.put_wheel_tip", MaiKeys.wheelKeyName()));
            if (steps.isEmpty()) {
                tooltip.add(Component.translatable("screen.kuiba.combo.put_wheel_empty").withStyle(style -> style.withColor(LOW)));
            }
        }

        MaiCombo.Preset preview = preset(slot);
        float cost = MaiCombo.totalCost(preview);
        Component costLine;
        if (preview.steps().isEmpty()) {
            costLine = Component.translatable("screen.kuiba.combo.no_steps");
        } else {
            costLine = Component.translatable("screen.kuiba.combo.preview_sequence", Math.round(cost), Math.round(mai.current()));
        }
        int costColor = preview.steps().isEmpty() ? MUTED : (mai.current() + 0.001F < cost ? LOW : 0xFF7CFFD4);
        graphics.drawString(this.font, costLine, left + PANEL_W - 12 - this.font.width(costLine), top() + 146, costColor, false);

        graphics.drawString(this.font, Component.translatable("screen.kuiba.combo.pool"), rightX(), top() + 160, 0xFF9AD0FF, false);
        List<MaiSkill> pool = pool(mai);
        if (pool.isEmpty()) {
            graphics.drawString(this.font, Component.translatable("screen.kuiba.combo.pool_empty"), rightX(), top() + 174, MUTED, false);
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        for (int i = 0; i < Math.min(pool.size(), GRID_COLS * GRID_ROWS); i++) {
            MaiSkill skill = pool.get(i);
            int cx = cellX(i);
            int cy = cellY(i);
            boolean hover = inside(mouseX, mouseY, cx, cy, CELL - 2, CELL - 2);
            boolean used = contains(steps, skill.id());
            graphics.fill(cx, cy, cx + CELL - 2, cy + CELL - 2, hover ? ROW_HOVER : 0xFF182028);
            graphics.blit(skill.icon(), cx + 1, cy + 1, 0.0F, 0.0F, 16, 16, 16, 16);
            if (used) {
                MaiHud.frame(graphics, cx, cy, CELL - 2, CELL - 2, GOLD);
            } else if (hover) {
                MaiHud.frame(graphics, cx, cy, CELL - 2, CELL - 2, 0xFF4FE3FF);
            }
            if (hover) {
                tooltip = MaiSkillTip.lines(this.font, skill);
            }
        }

        int by = buttonY();
        button(graphics, Component.translatable("screen.kuiba.combo.save"), rightX(), by, 56, mouseX, mouseY, 0xFF1E5C3A, 0xFF2E8B57);
        button(graphics, Component.translatable("screen.kuiba.combo.clear"), rightX() + 60, by, 56, mouseX, mouseY, 0xFF3A2A2A, 0xFF5A3A3A);
        button(graphics, Component.translatable("screen.kuiba.close"), left + PANEL_W - 68, by, 56, mouseX, mouseY, 0xFF3A2A2A, 0xFF5A3A3A);
        if (status != null) {
            graphics.drawString(this.font, status, rightX() + 124, by + 5, statusColor, false);
        }

        // Widgets last so the name box sits on top of the panel.
        for (var renderable : this.renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
        if (tooltip != null) {
            graphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    /** Draws one step box and returns its tooltip when hovered. */
    private @Nullable List<Component> renderBox(GuiGraphics graphics, MaiData mai, List<MaiCombo.Step> steps, int index, int x, int y, int mouseX, int mouseY) {
        boolean filled = index < steps.size();
        boolean next = index == steps.size();
        boolean active = filled && index == activeStep;
        graphics.fill(x, y, x + BOX_W, y + BOX_H, filled ? (active ? 0xFF243446 : 0xFF1C2430) : 0xFF141A22);
        if (!filled) {
            MaiHud.frame(graphics, x, y, BOX_W, BOX_H, next ? 0xFF2E5266 : 0xFF1E2830);
            if (next) {
                graphics.drawCenteredString(this.font, "+", x + BOX_W / 2, y + 8, 0xFF4FE3FF);
                graphics.drawCenteredString(this.font, Component.translatable("screen.kuiba.combo.pick"), x + BOX_W / 2, y + 24, MUTED);
            }
            return null;
        }
        MaiCombo.Step step = steps.get(index);
        MaiSkill skill = MaiSkills.get(step.skillId());
        boolean valid = skill != null && mai.isUnlocked(step.skillId());
        MaiHud.frame(graphics, x, y, BOX_W, BOX_H, active ? GOLD : (valid ? 0xFF3A6EA5 : LOW));
        graphics.drawString(this.font, Integer.toString(index + 1), x + 3, y + 2, GOLD, false);
        int iconX = x + (BOX_W - 16) / 2;
        if (skill != null) {
            RenderSystem.enableBlend();
            graphics.blit(skill.icon(), iconX, y + 6, 0.0F, 0.0F, 16, 16, 16, 16);
        } else {
            graphics.drawCenteredString(this.font, "?", x + BOX_W / 2, y + 8, LOW);
        }
        graphics.drawCenteredString(this.font, "×" + step.charge(), x + BOX_W / 2, y + 26, 0xFFFFFFFF);
        // Remove affordance: the little x in the corner.
        boolean removeHover = inside(mouseX, mouseY, removeX(x), y + 1, 8, 8);
        graphics.drawString(this.font, "x", removeX(x), y + 1, removeHover ? LOW : MUTED, false);

        if (skill != null && inside(mouseX, mouseY, x, y, BOX_W, BOX_H)) {
            List<Component> tip = new ArrayList<>(MaiSkillTip.lines(this.font, skill));
            tip.add(Component.translatable("screen.kuiba.combo.step_tip", index + 1,
                    Math.round(skill.cost() * MaiCombo.costMultiplier(step.charge()))).withStyle(style -> style.withColor(MUTED)));
            return tip;
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.minecraft == null || this.minecraft.player == null || work.isEmpty()) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (nameBox != null && nameBox.isMouseOver(mouseX, mouseY)) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (nameBox != null) {
            nameBox.setFocused(false);
        }
        syncNameBox();

        MaiData mai = MaiApi.data(this.minecraft.player);
        int left = left();
        int top = top();

        int fixedY = top() + 212;
        for (int i = 0; i < MaiCombo.COMBO_COUNT; i++) {
            if (inside(mouseX, mouseY, fixedChipX(i), fixedY, FIXED_CHIP_W, 16)) {
                KuibaNetwork.sendToServer(new FixedComboPayload(i));
                return true;
            }
        }
        for (int i = 0; i < MaiCombo.COMBO_COUNT; i++) {
            if (inside(mouseX, mouseY, left + 12, rowY(i), ROW_W, ROW_H)) {
                slot = i;
                syncNameBox();
                return true;
            }
        }

        List<MaiCombo.Step> steps = work.get(slot);
        int boxes = MaiCombo.MAX_STEPS;
        for (int i = 0; i < Math.min(boxes, steps.size()); i++) {
            int bx = boxX(i);
            if (!inside(mouseX, mouseY, bx, boxY(), BOX_W, BOX_H)) {
                continue;
            }
            if (button == 1 || inside(mouseX, mouseY, removeX(bx), boxY() + 1, 8, 8)) {
                steps.remove(i);
                activeStep = Math.max(0, Math.min(activeStep, steps.size() - 1));
                return true;
            }
            activeStep = i;
            return true;
        }
        if (!steps.isEmpty()) {
            int cy = chargeY();
            int active = Math.min(activeStep, steps.size() - 1);
            for (int c = 0; c < MaiCombo.CHARGE_LEVELS.length; c++) {
                if (inside(mouseX, mouseY, chargeX(c), cy, CHARGE_W, 14)) {
                    steps.set(active, new MaiCombo.Step(steps.get(active).skillId(), MaiCombo.CHARGE_LEVELS[c]));
                    return true;
                }
            }
        }

        int rowY = gapRowY();
        {
            if (inside(mouseX, mouseY, gapMinusX(), rowY, 14, 14)) {
                gaps[slot] = Math.max(0, gaps[slot] - GAP_STEP);
                return true;
            }
            if (inside(mouseX, mouseY, gapMinusX() + 56, rowY, 14, 14)) {
                gaps[slot] = Math.min(GAP_MAX, gaps[slot] + GAP_STEP);
                return true;
            }
        }

        int wheelX = left + PANEL_W - 12 - 78;
        if (inside(mouseX, mouseY, wheelX, rowY, 78, 14)) {
            if (steps.isEmpty()) {
                setStatus(Component.translatable("screen.kuiba.combo.put_wheel_empty"), LOW);
            } else {
                onWheel[slot] = !onWheel[slot];
            }
            return true;
        }

        List<MaiSkill> pool = pool(mai);
        for (int i = 0; i < Math.min(pool.size(), GRID_COLS * GRID_ROWS); i++) {
            if (inside(mouseX, mouseY, cellX(i), cellY(i), CELL - 2, CELL - 2)) {
                pick(pool.get(i));
                return true;
            }
        }

        int by = buttonY();
        if (inside(mouseX, mouseY, rightX(), by, 56, BUTTON_H)) {
            save(mai);
            return true;
        }
        if (inside(mouseX, mouseY, rightX() + 60, by, 56, BUTTON_H)) {
            steps.clear();
            return true;
        }
        if (inside(mouseX, mouseY, left + PANEL_W - 68, by, 56, BUTTON_H)) {
            onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (nameBox != null && nameBox.isFocused()) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (MaiKeys.COMBO_EDITOR.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** Reload the name box when the edited preset changes. */
    private void syncNameBox() {
        if (nameBox != null) {
            nameBox.setValue(names[slot] == null ? "" : names[slot]);
        }
    }

    private void pick(MaiSkill skill) {
        List<MaiCombo.Step> steps = work.get(slot);
        if (steps.size() >= MaiCombo.MAX_STEPS) {
            setStatus(Component.translatable("screen.kuiba.combo.full", MaiCombo.MAX_STEPS), LOW);
            return;
        }
        steps.add(new MaiCombo.Step(skill.id(), 1));
        activeStep = steps.size() - 1;
    }

    private void save(MaiData mai) {
        // Send every edited preset, not just the one on screen: switching rows must not silently drop work.
        // The only rule that blocks a save is the mai pool: a combo has to be payable at full charge.
        float max = Math.max(1.0F, mai.shownMax());
        for (int i = 0; i < MaiCombo.COMBO_COUNT; i++) {
            if (!dirty(mai, i)) {
                continue;
            }
            float total = MaiCombo.totalCost(preset(i));
            if (total > max + 0.01F) {
                setStatus(Component.translatable("screen.kuiba.combo.too_expensive",
                        i + 1, Math.round(total), Math.round(max)), LOW);
                return;
            }
        }
        int sent = 0;
        for (int i = 0; i < MaiCombo.COMBO_COUNT; i++) {
            if (!dirty(mai, i)) {
                continue;
            }
            KuibaNetwork.sendToServer(new SaveComboPayload(i, preset(i)));
            sent++;
        }
        if (sent == 0) {
            setStatus(Component.translatable("screen.kuiba.combo.unchanged"), MUTED);
        } else {
            setStatus(Component.translatable("screen.kuiba.combo.sent"), 0xFF7CFFD4);
        }
    }

    private MaiCombo.Preset preset(int index) {
        return new MaiCombo.Preset(names[index] == null ? "" : names[index], gaps[index], onWheel[index], work.get(index));
    }

    private boolean dirty(MaiData mai, int index) {
        MaiCombo.Preset saved = mai.combo(index);
        return !saved.steps().equals(work.get(index))
                || saved.gapTicks() != gaps[index]
                || saved.onWheel() != onWheel[index]
                || !saved.name().equals(names[index] == null ? "" : MaiCombo.clampName(names[index]));
    }

    private List<MaiSkill> pool(MaiData mai) {
        List<MaiSkill> list = new ArrayList<>();
        for (MaiSkill skill : MaiSkills.all()) {
            if (mai.isUnlocked(skill.id())) {
                list.add(skill);
            }
        }
        return list;
    }

    private static boolean contains(List<MaiCombo.Step> steps, ResourceLocation id) {
        for (MaiCombo.Step step : steps) {
            if (step.skillId().equals(id)) {
                return true;
            }
        }
        return false;
    }

    private void setStatus(Component text, int color) {
        status = text;
        statusColor = color;
    }

    private void smallButton(GuiGraphics graphics, String label, int x, int y, int mouseX, int mouseY, boolean enabled) {
        boolean hover = enabled && inside(mouseX, mouseY, x, y, 14, 14);
        graphics.fill(x, y, x + 14, y + 14, !enabled ? 0xFF202428 : (hover ? 0xFF2E4458 : 0xFF243040));
        graphics.drawCenteredString(this.font, label, x + 7, y + 3, enabled ? 0xFFFFFFFF : 0xFF555A60);
    }

    private void button(GuiGraphics graphics, Component label, int x, int y, int w, int mouseX, int mouseY, int color, int hoverColor) {
        boolean hover = inside(mouseX, mouseY, x, y, w, BUTTON_H);
        graphics.fill(x, y, x + w, y + BUTTON_H, hover ? hoverColor : color);
        graphics.drawCenteredString(this.font, label, x + w / 2, y + 5, 0xFFFFFFFF);
    }

    private int left() {
        return (this.width - PANEL_W) / 2;
    }

    private int top() {
        return (this.height - PANEL_H) / 2;
    }

    private int rightX() {
        return left() + 156;
    }


    private int gapRowY() {
        return top() + 128;
    }

    private int nameBoxX() {
        return rightX();
    }

    private int nameBoxY() {
        return top() + 26;
    }

    private int rowY(int index) {
        return top() + 36 + index * ROW_PITCH;
    }

    private int boxX(int index) {
        return rightX() + index * BOX_PITCH;
    }

    private int boxY() {
        return top() + 64;
    }

    private int removeX(int boxX) {
        return boxX + BOX_W - 9;
    }

    private int chargeY() {
        return top() + 108;
    }

    /** Chip {@code index} of {@link MaiCombo#CHARGE_LEVELS}. */
    private int chargeX(int index) {
        return rightX() + 34 + index * CHARGE_PITCH;
    }

    private int gapMinusX() {
        return rightX() + 40;
    }

    private int cellX(int index) {
        return rightX() + (index % GRID_COLS) * CELL;
    }

    private int cellY(int index) {
        return top() + 172 + (index / GRID_COLS) * CELL;
    }

    private int fixedChipX(int index) {
        String label = Component.translatable("screen.kuiba.combo.fixed", MaiKeys.comboKeyName(1)).getString();
        return left() + 12 + this.font.width(label) + 8 + index * FIXED_CHIP_PITCH;
    }

    private int buttonY() {
        return top() + PANEL_H - 26;
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }
}
