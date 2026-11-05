package com.kuiba.mai;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.server.level.ServerPlayer;

/**
 * Unused mai drains at {@code max / 90} per second (weave ×0.90 ≈ 100 s).
 * The 2 s open-mai timer only clears body FX / aura, not the pool.
 */
public final class MaiRegen {
    private MaiRegen() {}

    /** Server-only: game time when the current open-mai body FX ends. */
    private static final Map<UUID, Long> OPEN_VISUAL_UNTIL = new ConcurrentHashMap<>();

    public static void markOpenVisual(ServerPlayer player) {
        OPEN_VISUAL_UNTIL.put(player.getUUID(), player.level().getGameTime() + MaiApi.OPEN_MAI_VISUAL_TICKS);
    }

    public static boolean isOpenVisualActive(ServerPlayer player) {
        Long until = OPEN_VISUAL_UNTIL.get(player.getUUID());
        return until != null && player.level().getGameTime() < until;
    }

    public static void tick(ServerPlayer player) {
        MaiData mai = MaiApi.data(player);
        boolean cooldownChanged = mai.tickCooldowns();
        mai.tickArtCooldowns();
        mai.tickOpenMaiCooldown();
        boolean cooling = mai.cooldownsView().values().stream().anyMatch(ticks -> ticks > 0);
        float before = mai.current();

        Long until = OPEN_VISUAL_UNTIL.get(player.getUUID());
        if (until != null && player.level().getGameTime() >= until) {
            OPEN_VISUAL_UNTIL.remove(player.getUUID());
            MaiApi.syncOpenVisualClosed(player);
        }

        if (player.tickCount % 20 == 0) {
            float max = MaiApi.getMax(player);
            float loss = max * MaiApi.DECAY_FRACTION_PER_SECOND * MaiApi.getDecayMultiplier(player);
            if (loss > 0.0F && mai.current() > 0.0F && !player.getAbilities().instabuild) {
                mai.setCurrent(mai.current() - loss, max);
            }
        }

        boolean pointsChanged = Math.abs(mai.current() - before) > 0.01F;
        if (cooldownChanged || pointsChanged || (cooling && player.tickCount % 10 == 0)) {
            MaiApi.sync(player);
        }
    }

    public static void markHurt(ServerPlayer player) {
        MaiApi.data(player).setLastCombatTick(player.level().getGameTime());
    }

    public static void markDealt(ServerPlayer player) {
        MaiApi.data(player).setLastCombatTick(player.level().getGameTime());
    }
}
