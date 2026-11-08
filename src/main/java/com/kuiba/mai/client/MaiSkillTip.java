package com.kuiba.mai.client;

import java.util.ArrayList;
import java.util.List;

import com.kuiba.mai.MaiSkill;

import net.minecraft.client.gui.Font;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

/** Hover text for a skill: name, cost and cooldown, then the wrapped description. */
final class MaiSkillTip {
    private static final int WIDTH = 190;
    private static final int NAME = 0xFF8CF3FF;
    private static final int STATS = 0xFF7C8A92;
    private static final int DESC = 0xFFCFE8EE;

    private MaiSkillTip() {}

    static List<Component> lines(Font font, MaiSkill skill) {
        List<Component> tip = new ArrayList<>();
        tip.add(Component.translatable(skill.nameKey()).withStyle(style -> style.withColor(NAME)));
        tip.add(Component.translatable("screen.kuiba.combo.skill_tip", Math.round(skill.cost())).withStyle(style -> style.withColor(STATS)));
        if (I18n.exists(skill.descKey())) {
            for (FormattedText line : font.getSplitter().splitLines(Component.translatable(skill.descKey()), WIDTH, Style.EMPTY)) {
                tip.add(Component.literal(line.getString()).withStyle(style -> style.withColor(DESC)));
            }
        }
        return tip;
    }
}
