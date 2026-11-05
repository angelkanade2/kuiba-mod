package com.kuiba.wenyao;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/** Pendant rank stored in {@code kuiba:wenyao_tier}. Common has no melee bonus. */
public enum WenyaoTier implements StringRepresentable {
    COMMON("common", 1, 4.0F, 0.98F, 0.0F),
    XIA("xia", 4, 10.0F, 0.95F, 0.03F),
    JIANG("jiang", 7, 18.0F, 0.92F, 0.06F),
    WANG("wang", 10, 28.0F, 0.90F, 0.08F);

    public static final StreamCodec<ByteBuf, WenyaoTier> STREAM_CODEC = ByteBufCodecs.STRING_UTF8.map(
            WenyaoTier::byName,
            WenyaoTier::getSerializedName);

    private final String name;
    private final int minGate;
    private final float bonusMax;
    private final float costMultiplier;
    private final float meleeBonus;

    WenyaoTier(String name, int minGate, float bonusMax, float costMultiplier, float meleeBonus) {
        this.name = name;
        this.minGate = minGate;
        this.bonusMax = bonusMax;
        this.costMultiplier = costMultiplier;
        this.meleeBonus = meleeBonus;
    }

    public int minGate() {
        return minGate;
    }

    public float bonusMax() {
        return bonusMax;
    }

    public float costMultiplier() {
        return costMultiplier;
    }

    /** Fraction of attack damage. Common is 0. */
    public float meleeBonus() {
        return meleeBonus;
    }

    public boolean atLeast(WenyaoTier other) {
        return ordinal() >= other.ordinal();
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public static WenyaoTier byName(String raw) {
        for (WenyaoTier tier : values()) {
            if (tier.name.equals(raw)) {
                return tier;
            }
        }
        return COMMON;
    }
}
