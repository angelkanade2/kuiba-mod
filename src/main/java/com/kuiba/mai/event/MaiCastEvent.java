package com.kuiba.mai.event;

import com.kuiba.mai.MaiSkill;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/** Posted before points are spent. Cancel to refuse the cast. */
public class MaiCastEvent extends Event implements ICancellableEvent {
    private final ServerPlayer player;
    private final MaiSkill skill;
    private final int slot;

    public MaiCastEvent(ServerPlayer player, MaiSkill skill, int slot) {
        this.player = player;
        this.skill = skill;
        this.slot = slot;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public MaiSkill getSkill() {
        return skill;
    }

    public int getSlot() {
        return slot;
    }
}
