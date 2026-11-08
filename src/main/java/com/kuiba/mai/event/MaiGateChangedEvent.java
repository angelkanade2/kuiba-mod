package com.kuiba.mai.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

public class MaiGateChangedEvent extends Event {
    private final ServerPlayer player;
    private final int from;
    private final int to;

    public MaiGateChangedEvent(ServerPlayer player, int from, int to) {
        this.player = player;
        this.from = from;
        this.to = to;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public int getFrom() {
        return from;
    }

    public int getTo() {
        return to;
    }
}
