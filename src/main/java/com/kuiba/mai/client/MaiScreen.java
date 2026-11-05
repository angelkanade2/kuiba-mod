package com.kuiba.mai.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.kuiba.Kuiba;
import com.kuiba.mai.MaiApi;
import com.kuiba.mai.MaiData;
import com.kuiba.mai.MaiRitual;
import com.kuiba.mai.MaiSkill;
import com.kuiba.mai.MaiSkills;
import com.kuiba.mai.MaiTiers;
import com.kuiba.mai.Meridian;
import com.kuiba.mai.payload.EquipSkillPayload;
import com.kuiba.mai.payload.OpenMaiPayload;
import com.kuiba.mai.payload.StartRitualPayload;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Mai gate screen. Click or drag an unlocked skill onto an open slot.
 * Wenyao equip stays out of this screen.
 */
public class MaiScreen extends Screen {
    private static final int PANEL_W = 372;
    private static final int PANEL_H = 268;
    private static final int SLOT_W = 150;
    private static final int SLOT_H = 24;
    private static final int ROW_H = 18;
    private static final int VISIBLE_ROWS = 6;
    private static final ResourceLocation GATE_ON = Kuiba.id("textures/hud/gate_on.png");
    private static final ResourceLocation GATE_OFF = Kuiba.id("textures/hud/gate_off.png");

    @Nullable
    private ResourceLocation selected;
    @Nullable
    private ResourceLocation dragSkill;
    private boolean dragMoved;
    private int dragX;
    private int dragY;
    private int scroll;

    public MaiScreen() {
        super(Component.translatable("screen.kuiba.mai"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * Blur the world only. Vanilla {@code renderBackground} also paints a full-screen menu
     * texture, and {@code Screen.render} blurs again after custom draws, which smears the panel.
     */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (this.minecraft == null) {
            return;
        }
        if (this.minecraft.level == null) {
            this.renderPanorama(graphics, partialTick);
        }
        this.renderBlurredBackground(partialTick);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        int left = left();
        int top = top();
        graphics.fill(left, top, left + PANEL_W, top + PANEL_H, 0xFF10141C);
        graphics.fill(left, top, left + PANEL_W, top + 2, 0xFF3A6EA5);
        graphics.drawString(this.font, this.title, left + 12, top + 8, 0xFFE7F6FF, false);

        if (this.minecraft == null || this.minecraft.player == null) {
            for (var renderable : this.renderables) {
                renderable.render(graphics, mouseX, mouseY, partialTick);
            }
            return;
        }
        MaiData mai = MaiApi.data(this.minecraft.player);
        int openSlots = MaiTiers.slots(mai.gate());
        graphics.drawString(this.font, Component.translatable(
                "hud.kuiba.points",
                Component.translatable(MaiTiers.nameKey(mai.gate())),
                Math.round(mai.current()),
                Math.round(mai.shownMax())), left + 12, top + 22, mai.gate() > 6 ? 0xFF9A9A9A : 0xFFB7F0D8, false);

        Component gateTip = null;
        MaiFx.View view = MaiFx.view(this.minecraft.player);
        float time = MaiFx.time(partialTick);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        for (int gate = 1; gate <= 12; gate++) {
            int px = left + 12 + (gate - 1) * 14;
            int py = top + 35;
            boolean opened = gate <= mai.gate();
            MaiFx.Ring ring = view == null ? null : MaiFx.ring(view, gate, time);
            if (ring != null) {
                graphics.setColor(0.31F, 0.89F, 1.0F, ring.alpha());
                graphics.blit(GATE_ON, px, py, 0.0F, 0.0F, 8, 8, 8, 8);
            } else if (opened) {
                graphics.setColor(0.12F, 0.44F, 0.53F, 1.0F);
                graphics.blit(GATE_ON, px, py, 0.0F, 0.0F, 8, 8, 8, 8);
            } else {
                graphics.setColor(1.0F, 1.0F, 1.0F, gate > 6 ? 0.2F : 0.35F);
                graphics.blit(GATE_OFF, px, py, 0.0F, 0.0F, 8, 8, 8, 8);
            }
            if (gate == mai.gate()) {
                graphics.setColor(1.0F, 0.83F, 0.42F, 1.0F);
                graphics.blit(GATE_OFF, px - 1, py - 1, 10, 10, 0.0F, 0.0F, 8, 8, 8, 8);
            }
            if (inside(mouseX, mouseY, px, py, 8, 8)) {
                Meridian meridian = MaiTiers.meridian(gate);
                Component name = meridian == null ? Component.translatable(MaiTiers.nameKey(gate)) : Component.translatable(meridian.nameKey());
                gateTip = Component.translatable("screen.kuiba.gate_tip", gate, name)
                        .append(" · ")
                        .append(Component.translatable(opened ? "screen.kuiba.gate_tip_open" : "screen.kuiba.gate_tip_closed"));
            }
        }
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.drawString(this.font, Component.translatable("screen.kuiba.later_gates"), left + 12, top + 48, 0xFF8A8A8A, false);
        graphics.drawString(this.font, Component.translatable("screen.kuiba.equipped"), left + 12, top + 62, 0xFF9AD0FF, false);
        graphics.drawString(this.font, Component.translatable("screen.kuiba.unlocked"), left + 176, top + 62, 0xFF9AD0FF, false);

        ItemStack hovered = ItemStack.EMPTY;
        MaiSkill hoveredSkill = null;
        for (int i = 0; i < MaiData.EQUIPPED_SLOTS; i++) {
            int sx = slotX();
            int sy = slotY(i);
            boolean open = i < openSlots;
            boolean hover = inside(mouseX, mouseY, sx, sy, SLOT_W, SLOT_H);
            graphics.fill(sx, sy, sx + SLOT_W, sy + SLOT_H, !open ? 0xFF2A2A2A : (hover ? 0xFF243446 : 0xFF1C2430));
            ResourceLocation equipped = mai.equipped(i);
            MaiSkill skill = equipped == null ? null : MaiSkills.get(equipped);
            if (skill != null) {
                graphics.blit(skill.icon(), sx + 4, sy + 4, 0.0F, 0.0F, 16, 16, 16, 16);
                if (hover) {
                    hoveredSkill = skill;
                }
            }
            String keyName = switch (i) {
                case 0 -> "V";
                case 1 -> "B";
                case 2 -> "N";
                default -> "-";
            };
            Component line = skill == null
                    ? Component.translatable("screen.kuiba.empty_slot", i + 1, keyName)
                    : Component.translatable(skill.nameKey());
            graphics.drawString(this.font, line, sx + 24, sy + 8, open ? 0xFFE7F6FF : 0xFF777777, false);
        }

        List<MaiSkill> unlocked = unlocked(mai);
        int maxScroll = Math.max(0, unlocked.size() - VISIBLE_ROWS);
        scroll = Math.clamp(scroll, 0, maxScroll);
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int index = scroll + row;
            if (index >= unlocked.size()) {
                break;
            }
            MaiSkill skill = unlocked.get(index);
            int sx = poolX();
            int sy = poolY(row);
            boolean hover = inside(mouseX, mouseY, sx, sy, 180, ROW_H);
            if (hover) {
                hoveredSkill = skill;
            }
            boolean isSelected = skill.id().equals(selected) || skill.id().equals(dragSkill);
            graphics.fill(sx, sy, sx + 180, sy + ROW_H - 1, isSelected ? 0xFF2E6B4F : (hover ? 0xFF243446 : 0xFF182028));
            graphics.blit(skill.icon(), sx + 2, sy + 1, 0.0F, 0.0F, 16, 16, 16, 16);
            graphics.drawString(this.font, Component.translatable(skill.nameKey()), sx + 22, sy + 5, 0xFFE7F6FF, false);
            graphics.drawString(this.font, Integer.toString(Math.round(skill.cost())), sx + 150, sy + 5, 0xFF7CFFD4, false);
        }
        int locked = MaiSkills.all().size() - unlocked.size();
        if (locked > 0) {
            graphics.drawString(this.font, Component.translatable("screen.kuiba.locked_count", locked), poolX(), poolY(VISIBLE_ROWS) + 2, 0xFF777777, false);
        }

        MaiTiers.Tier next = MaiTiers.get(mai.gate() + 1);
        int ingredientY = top + PANEL_H - 62;
        if (next == null || next.ritual() == null) {
            graphics.drawString(this.font, Component.translatable("screen.kuiba.no_next"), left + 12, ingredientY + 8, 0xFF8A8A8A, false);
        } else {
            graphics.drawString(this.font, Component.translatable("screen.kuiba.next_gate", next.gate(), next.ritual().durationTicks() / 20), left + 12, ingredientY, 0xFFFFD36A, false);
            int ix = left + 12;
            for (MaiTiers.IngredientSpec ingredient : next.ritual().ingredients()) {
                ItemStack stack = iconFor(ingredient);
                int have = MaiRitual.countMatching(this.minecraft.player, ingredient);
                boolean enough = have >= ingredient.count() || this.minecraft.player.getAbilities().instabuild;
                graphics.renderItem(stack, ix, ingredientY + 12);
                graphics.drawString(this.font, have + "/" + ingredient.count(), ix + 18, ingredientY + 16, enough ? 0xFF7CFFD4 : 0xFFE25B4A, false);
                if (inside(mouseX, mouseY, ix, ingredientY + 12, 54, 16) && !stack.isEmpty()) {
                    hovered = stack;
                }
                ix += 78;
            }
        }

        int buttonY = top + PANEL_H - 26;
        boolean openHover = inside(mouseX, mouseY, left + 12, buttonY, 64, 18);
        graphics.fill(left + 12, buttonY, left + 76, buttonY + 18, openHover ? 0xFF2E8B57 : 0xFF1E5C3A);
        graphics.drawString(this.font, Component.translatable("screen.kuiba.open_mai"), left + 24, buttonY + 5, 0xFFFFFFFF, false);
        boolean ritualHover = inside(mouseX, mouseY, left + 84, buttonY, 100, 18);
        graphics.fill(left + 84, buttonY, left + 184, buttonY + 18, ritualHover ? 0xFF3A6EA5 : 0xFF244868);
        graphics.drawString(this.font, Component.translatable(MaiClientState.ritualActive() ? "screen.kuiba.ritual_cancel" : "screen.kuiba.ritual"), left + 96, buttonY + 5, 0xFFFFFFFF, false);
        boolean comboHover = inside(mouseX, mouseY, left + 192, buttonY, 64, 18);
        graphics.fill(left + 192, buttonY, left + 256, buttonY + 18, comboHover ? 0xFF8A6A1E : 0xFF5C4614);
        graphics.drawCenteredString(this.font, Component.translatable("screen.kuiba.combo.open"), left + 224, buttonY + 5, 0xFFFFFFFF);
        boolean closeHover = inside(mouseX, mouseY, left + PANEL_W - 72, buttonY, 60, 18);
        graphics.fill(left + PANEL_W - 72, buttonY, left + PANEL_W - 12, buttonY + 18, closeHover ? 0xFF5A3A3A : 0xFF3A2A2A);
        graphics.drawString(this.font, Component.translatable("screen.kuiba.close"), left + PANEL_W - 60, buttonY + 5, 0xFFFFFFFF, false);

        if (dragMoved && dragSkill != null) {
            MaiSkill skill = MaiSkills.get(dragSkill);
            if (skill != null) {
                graphics.blit(skill.icon(), dragX - 8, dragY - 8, 0.0F, 0.0F, 16, 16, 16, 16);
            }
        }
        if (!hovered.isEmpty()) {
            graphics.renderTooltip(this.font, hovered, mouseX, mouseY);
        } else if (hoveredSkill != null && !dragMoved) {
            graphics.renderComponentTooltip(this.font, MaiSkillTip.lines(this.font, hoveredSkill), mouseX, mouseY);
        } else if (gateTip != null) {
            graphics.renderTooltip(this.font, gateTip, mouseX, mouseY);
        }
        for (var renderable : this.renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.minecraft == null || this.minecraft.player == null) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        int left = left();
        int top = top();
        int buttonY = top + PANEL_H - 26;
        if (inside(mouseX, mouseY, left + 12, buttonY, 64, 18)) {
            PacketDistributor.sendToServer(new OpenMaiPayload());
            return true;
        }
        if (inside(mouseX, mouseY, left + 84, buttonY, 100, 18)) {
            PacketDistributor.sendToServer(new StartRitualPayload(!MaiClientState.ritualActive(), Optional.empty()));
            return true;
        }
        if (inside(mouseX, mouseY, left + 192, buttonY, 64, 18)) {
            this.minecraft.setScreen(new MaiComboScreen());
            return true;
        }
        if (inside(mouseX, mouseY, left + PANEL_W - 72, buttonY, 60, 18)) {
            onClose();
            return true;
        }

        MaiData mai = MaiApi.data(this.minecraft.player);
        int openSlots = MaiTiers.slots(mai.gate());
        int slot = slotAt(mouseX, mouseY);
        if (slot >= 0) {
            if (button == 1) {
                PacketDistributor.sendToServer(new EquipSkillPayload(slot, Optional.empty()));
                selected = null;
                return true;
            }
            if (selected != null && slot < openSlots) {
                PacketDistributor.sendToServer(new EquipSkillPayload(slot, Optional.of(selected)));
                return true;
            }
            ResourceLocation equipped = mai.equipped(slot);
            if (equipped != null && slot < openSlots) {
                selected = equipped;
            }
            return true;
        }

        MaiSkill skill = skillAt(mouseX, mouseY, mai);
        if (skill != null && mai.isUnlocked(skill.id())) {
            dragSkill = skill.id();
            dragMoved = false;
            dragX = (int) mouseX;
            dragY = (int) mouseY;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragXAmount, double dragYAmount) {
        if (dragSkill != null) {
            dragMoved = true;
            dragX = (int) mouseX;
            dragY = (int) mouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragXAmount, dragYAmount);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (dragSkill != null) {
            ResourceLocation skill = dragSkill;
            boolean moved = dragMoved;
            dragSkill = null;
            dragMoved = false;
            if (!moved) {
                selected = skill;
                return true;
            }
            if (this.minecraft != null && this.minecraft.player != null) {
                int openSlots = MaiTiers.slots(MaiApi.data(this.minecraft.player).gate());
                int slot = slotAt(mouseX, mouseY);
                if (slot >= 0 && slot < openSlots) {
                    PacketDistributor.sendToServer(new EquipSkillPayload(slot, Optional.of(skill)));
                    selected = skill;
                }
            }
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.minecraft != null && this.minecraft.player != null && inside(mouseX, mouseY, poolX(), poolY(0), 180, VISIBLE_ROWS * ROW_H)) {
            int maxScroll = Math.max(0, unlocked(MaiApi.data(this.minecraft.player)).size() - VISIBLE_ROWS);
            scroll = Math.clamp(scroll - (int) Math.signum(scrollY), 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private List<MaiSkill> unlocked(MaiData mai) {
        List<MaiSkill> list = new ArrayList<>();
        for (MaiSkill skill : MaiSkills.all()) {
            if (mai.isUnlocked(skill.id())) {
                list.add(skill);
            }
        }
        return list;
    }

    private int slotAt(double mouseX, double mouseY) {
        for (int i = 0; i < MaiData.EQUIPPED_SLOTS; i++) {
            if (inside(mouseX, mouseY, slotX(), slotY(i), SLOT_W, SLOT_H)) {
                return i;
            }
        }
        return -1;
    }

    private @Nullable MaiSkill skillAt(double mouseX, double mouseY, MaiData mai) {
        List<MaiSkill> unlocked = unlocked(mai);
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int index = scroll + row;
            if (index >= unlocked.size()) {
                break;
            }
            if (inside(mouseX, mouseY, poolX(), poolY(row), 180, ROW_H)) {
                return unlocked.get(index);
            }
        }
        return null;
    }

    private ItemStack iconFor(MaiTiers.IngredientSpec ingredient) {
        if (ingredient.item() != null) {
            Item item = BuiltInRegistries.ITEM.get(ingredient.item());
            return item == null ? ItemStack.EMPTY : new ItemStack(item);
        }
        if (ingredient.tag() != null) {
            TagKey<Item> key = TagKey.create(Registries.ITEM, ingredient.tag());
            var named = BuiltInRegistries.ITEM.getTag(key);
            if (named.isPresent()) {
                var first = named.get().stream().findFirst();
                if (first.isPresent()) {
                    return new ItemStack(first.get());
                }
            }
        }
        return ItemStack.EMPTY;
    }

    private int left() {
        return (this.width - PANEL_W) / 2;
    }

    private int top() {
        return (this.height - PANEL_H) / 2;
    }

    private int slotX() {
        return left() + 12;
    }

    private int slotY(int index) {
        return top() + 76 + index * (SLOT_H + 2);
    }

    private int poolX() {
        return left() + 176;
    }

    private int poolY(int row) {
        return top() + 76 + row * ROW_H;
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }
}
