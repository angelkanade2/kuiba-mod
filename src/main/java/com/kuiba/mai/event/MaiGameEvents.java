package com.kuiba.mai.event;

import com.kuiba.core.KuibaServerConfig;
import com.kuiba.mai.MaiApi;
import com.kuiba.mai.MaiCombo;
import com.kuiba.mai.MaiData;
import com.kuiba.mai.MaiRegen;
import com.kuiba.mai.MaiRitual;
import com.kuiba.mai.MaiSkills;
import com.kuiba.mai.MaiTiers;
import com.kuiba.mai.MaiWaves;
import com.kuiba.wenyao.WeaponArts;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class MaiGameEvents {
    private MaiGameEvents() {}

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(MaiTiers.SERVER);
        event.addListener(MaiSkills.SERVER);
        event.addListener(WeaponArts.SERVER);
    }

    @SubscribeEvent
    public static void onReloadComplete(net.neoforged.neoforge.event.OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) {
            MaiApi.onResourcesReloaded();
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MaiApi.ensureInitialized(player);
            MaiApi.sync(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MaiApi.applyModifiers(player);
            MaiApi.sync(player);
        }
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MaiApi.applyModifiers(player);
            MaiApi.sync(player);
        }
    }

    /**
     * Runs after NeoForge's own clone copy ({@code copyOnDeath}).
     * Death keeps gate, unlocked, and equipped, then sets current to 30% of max.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (event.isWasDeath()) {
            MaiData mai = MaiApi.data(player);
            if (KuibaServerConfig.KEEP_GATE_ON_DEATH.get()) {
                mai.retainProgressionOnly();
            } else {
                mai.resetProgression();
                MaiSkills.unlockFor(mai);
            }
            if (!KuibaServerConfig.KEEP_WENYAO_ON_DEATH.get()) {
                mai.clearWenyaoSlot();
            }
            mai.clearComboRuntime();
            float max = MaiApi.getMax(player);
            mai.setCurrent(0.0F, max);
            mai.markInitialized();
        }
        MaiApi.applyModifiers(player);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MaiRitual.tick(player);
            MaiCombo.tick(player);
            MaiRegen.tick(player);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            MaiWaves.tick(level);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        MaiWaves.clear();
    }

    /** A player came into view: give the viewer the subject's 脉门 aura so the rings show right away. */
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer viewer && event.getTarget() instanceof ServerPlayer subject) {
            MaiApi.sendAuraTo(viewer, subject);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MaiRitual.cancel(player, "disconnect", false);
            MaiApi.data(player).clearComboRuntime();
        }
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        if (event.getNewDamage() <= 0.0F) {
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            MaiRegen.markHurt(player);
            MaiRitual.onHurt(player);
        }
        if (event.getSource().getEntity() instanceof ServerPlayer attacker && event.getEntity() != attacker) {
            MaiRegen.markDealt(attacker);
            com.kuiba.wenyao.WenyaoApi.onFourPieceHit(attacker);
        }
    }

    /**
     * 脉闪 grants a short window where landing costs nothing. Only the first landing inside the window
     * is waived, so a later ordinary fall still hurts.
     */
    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        MaiData mai = MaiApi.data(player);
        long until = mai.noFallUntil();
        if (until > 0L && player.level().getGameTime() < until) {
            event.setCanceled(true);
            mai.setNoFallUntil(0L);
        }
    }
}
