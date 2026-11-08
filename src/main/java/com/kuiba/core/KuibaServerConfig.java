package com.kuiba.core;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Server config file: {@code config/kuiba-server.toml}.
 * P0 ritual is the pill flow ({@link RitualMode#SIMPLE}). Altar is reserved.
 */
public final class KuibaServerConfig {
    public enum RitualMode {
        SIMPLE,
        ALTAR
    }

    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.EnumValue<RitualMode> RITUAL_MODE = BUILDER
            .comment("Ritual entry. simple = 冲脉丹 (P0). altar is not implemented yet and falls back to the same FSM.")
            .defineEnum("ritualMode", RitualMode.SIMPLE);

    public static final ForgeConfigSpec.BooleanValue KEEP_GATE_ON_DEATH = BUILDER
            .comment("On death, keep gate, unlocked skills, and equipped slots. Current points become 30% of max either way.")
            .define("keepGateOnDeath", true);

    public static final ForgeConfigSpec.BooleanValue KEEP_WENYAO_ON_DEATH = BUILDER
            .comment("On death, keep the wenyaoSlot stack. The slot is empty until wenyao equip exists.")
            .define("keepWenyaoOnDeath", true);

    public static final ForgeConfigSpec.DoubleValue COMBAT_REGEN_PER_SECOND = BUILDER
            .comment("Fraction of max points restored per second while in combat (damage dealt or taken within 100 ticks).")
            .defineInRange("combatRegenPerSecond", 0.02D, 0.0D, 1.0D);

    public static final ForgeConfigSpec.DoubleValue OOC_REGEN_PER_SECOND = BUILDER
            .comment("Fraction of max points restored per second out of combat.")
            .defineInRange("oocRegenPerSecond", 0.08D, 0.0D, 1.0D);

    public static final ForgeConfigSpec.IntValue STARTING_CRYSTALS = BUILDER
            .comment("脉晶 a brand-new player starts with, handed out once. 0 disables the kit.")
            .defineInRange("startingCrystals", 8, 0, 64);

    public static final ForgeConfigSpec.DoubleValue ZOMBIE_CRYSTAL_CHANCE = BUILDER
            .comment("Chance (0-1) that a player-killed zombie drops one extra 脉晶. 0 disables it. Husks, drowned and zombie villagers count; zombified piglins do not.")
            .defineInRange("zombieCrystalChance", 0.15D, 0.0D, 1.0D);

    public static final ForgeConfigSpec.DoubleValue GATE_HOST_CHANCE = BUILDER
            .comment("Chance (0-1) that a freshly spawned zombie/skeleton (1-4 脉门) or piglin/zombified piglin (1-6 脉门) becomes a 特殊 mob.")
            .defineInRange("gateHostChance", 0.2D, 0.0D, 1.0D);

    public static final ForgeConfigSpec.IntValue GATE_HOST_OPEN_INTERVAL = BUILDER
            .comment("Ticks between 开脉 pulses on a 特殊 mob. The pulse is visual only; its 脉门 count never changes. Default 600 = 30 seconds.")
            .defineInRange("gateHostOpenIntervalTicks", 600, 20, 24000);

    public static final ForgeConfigSpec.DoubleValue GATE_HOST_DAMAGE_PER_GATE = BUILDER
            .comment("Extra outgoing damage per open 脉门, as a fraction. 0.5 means a 10 gate mob hits for 6x.")
            .defineInRange("gateHostDamagePerGate", 0.5D, 0.0D, 1.0D);

    public static final ForgeConfigSpec.DoubleValue GATE_HOST_HEALTH_PER_GATE = BUILDER
            .comment("Extra max health per 脉门, as a fraction of the mob's base health. 0.5 means 4 gates is 3x health.")
            .defineInRange("gateHostHealthPerGate", 0.5D, 0.0D, 1.0D);

    public static final ForgeConfigSpec.IntValue ENDERMAN_MIN_GATES = BUILDER
            .comment("Fewest 脉门 an End enderman opens. Only End endermen are gate hosts and only they drop 脉核.")
            .defineInRange("endermanMinGates", 6, 1, 12);

    public static final ForgeConfigSpec.IntValue ENDERMAN_MAX_GATES = BUILDER
            .comment("Most 脉门 an End enderman opens.")
            .defineInRange("endermanMaxGates", 12, 1, 12);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private KuibaServerConfig() {}
}
