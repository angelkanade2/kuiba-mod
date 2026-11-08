package com.kuiba.core;

import net.minecraft.util.Mth;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;

/**
 * Per-mob 脉门 state, attached to a {@code LivingEntity}. A plain mob has {@code kind == NONE} and
 * {@code gates == 0}.
 * <p>
 * {@code gates} is rolled once when the mob first spawns and then never changes: this particular mob can
 * open that many 脉门. It pulses its 开脉 animation on a timer, which is purely visual; the damage bonus
 * is always derived from {@code gates}.
 */
public final class GateHostData implements INBTSerializable<CompoundTag> {
    /** What the mob drops once it is a gate host. */
    public enum Kind {
        /** Not a gate host. */
        NONE,
        /** 特殊僵尸 / 骷髅: drops 脉碎片. */
        SHARD,
        /** 特殊 zombified piglin / piglin: drops 脉晶. */
        CRYSTAL,
        /** 末影人: drops 脉核. */
        CORE
    }

    private Kind kind = Kind.NONE;
    /** Fixed 脉门 count for this mob, rolled at spawn. */
    private int gates;
    /** Game time of the next 开脉 pulse (visual only). */
    private long nextPulseTick;

    public Kind kind() {
        return kind;
    }

    public void setKind(Kind kind) {
        this.kind = kind == null ? Kind.NONE : kind;
    }

    public boolean isHost() {
        return kind != Kind.NONE && gates > 0;
    }

    public int gates() {
        return gates;
    }

    public void setGates(int gates) {
        this.gates = Mth.clamp(gates, 0, 12);
    }

    public long nextPulseTick() {
        return nextPulseTick;
    }

    public void setNextPulseTick(long tick) {
        this.nextPulseTick = tick;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Kind", kind.name());
        tag.putInt("Gates", gates);
        tag.putLong("NextPulse", nextPulseTick);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag) {
        try {
            kind = Kind.valueOf(tag.getString("Kind"));
        } catch (IllegalArgumentException exception) {
            kind = Kind.NONE;
        }
        gates = Mth.clamp(tag.getInt("Gates"), 0, 12);
        nextPulseTick = tag.getLong("NextPulse");
    }
}
