package com.kuiba.mai;

import net.minecraft.resources.ResourceLocation;

/**
 * One loaded {@code data/kuiba/mai_skill} entry. Unknown {@code cast} values are dropped at load time.
 */
public record MaiSkill(
        ResourceLocation id,
        String category,
        int unlockGate,
        float cost,
        int cooldownTicks,
        String cast,
        float power,
        float range,
        int durationTicks,
        ResourceLocation icon) {
    public String nameKey() {
        return "skill.kuiba." + id.getPath();
    }

    public String descKey() {
        return nameKey() + ".desc";
    }
}
