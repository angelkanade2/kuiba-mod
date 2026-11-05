package com.kuiba.core;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server config file: {@code config/kuiba-server.toml}.
 * P0 ritual is the pill flow ({@link RitualMode#SIMPLE}). Altar is reserved.
 */
public final class KuibaServerConfig {
    public enum RitualMode {
        SIMPLE,
        ALTAR
    }

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.EnumValue<RitualMode> RITUAL_MODE = BUILDER
            .comment("Ritual entry. simple = 冲脉丹 (P0). altar is not implemented yet and falls back to the same FSM.")
            .defineEnum("ritualMode", RitualMode.SIMPLE);

    public static final ModConfigSpec.BooleanValue KEEP_GATE_ON_DEATH = BUILDER
            .comment("On death, keep gate, unlocked skills, and equipped slots. Current points become 30% of max either way.")
            .define("keepGateOnDeath", true);

    public static final ModConfigSpec.BooleanValue KEEP_WENYAO_ON_DEATH = BUILDER
            .comment("On death, keep the wenyaoSlot stack. The slot is empty until wenyao equip exists.")
            .define("keepWenyaoOnDeath", true);

    public static final ModConfigSpec.DoubleValue COMBAT_REGEN_PER_SECOND = BUILDER
            .comment("Fraction of max points restored per second while in combat (damage dealt or taken within 100 ticks).")
            .defineInRange("combatRegenPerSecond", 0.02D, 0.0D, 1.0D);

    public static final ModConfigSpec.DoubleValue OOC_REGEN_PER_SECOND = BUILDER
            .comment("Fraction of max points restored per second out of combat.")
            .defineInRange("oocRegenPerSecond", 0.08D, 0.0D, 1.0D);

    public static final ModConfigSpec.IntValue STARTING_CRYSTALS = BUILDER
            .comment("脉晶 a brand-new player starts with. 6 is exactly the gate 1 ritual, so it opens one gate. 0 disables the kit.")
            .defineInRange("startingCrystals", 6, 0, 64);

    public static final ModConfigSpec.DoubleValue ZOMBIE_CRYSTAL_CHANCE = BUILDER
            .comment("Chance (0-1) that a player-killed zombie drops one extra 脉晶. 0 disables it. Husks, drowned and zombie villagers count; zombified piglins do not.")
            .defineInRange("zombieCrystalChance", 0.15D, 0.0D, 1.0D);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private KuibaServerConfig() {}
}
