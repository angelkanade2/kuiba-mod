package com.kuiba.mai.client;

import com.kuiba.mai.MaiApi;
import com.kuiba.mai.MaiData;
import com.kuiba.mai.payload.MaiFxPayload;
import com.kuiba.mai.payload.SyncMaiPayload;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/** Client half of packets that must not be referenced from common code. */
public final class ClientPayloads {
    private ClientPayloads() {}

    public static void sync(SyncMaiPayload msg) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        MaiData mai = MaiApi.data(player);
        mai.applySync(
                msg.gate(),
                msg.current(),
                msg.max(),
                msg.backlashStacks(),
                msg.equipped(),
                msg.cooldowns(),
                msg.unlocked());
        for (int i = 0; i < msg.combos().length; i++) {
            mai.setCombo(i, msg.combos()[i]);
        }
        mai.setSelectedCombo(msg.selectedCombo());
        mai.setFixedCombo(msg.fixedCombo());
    }

    public static void fx(MaiFxPayload msg) {
        MaiClientState.onFx(msg.entityId(), msg.kind(), msg.gate());
        if (msg.kind() == MaiFxPayload.KIND_OPEN && msg.gate() > 0) {
            MaiFx.playOpenSound(msg.entityId(), msg.gate());
            MaiFx.openBurst(msg.entityId(), msg.gate());
        } else if (msg.kind() == MaiFxPayload.KIND_UNLOCK && msg.gate() > 0) {
            MaiFx.openBurst(msg.entityId(), msg.gate());
        }
    }
}
