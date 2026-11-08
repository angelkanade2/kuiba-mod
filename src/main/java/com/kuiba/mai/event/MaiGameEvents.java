package com.kuiba.mai.event;

import com.kuiba.core.GateHostData;
import com.kuiba.core.KuibaServerConfig;
import com.kuiba.core.MaiAttachments;
import com.kuiba.core.MaiGateHosts;
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
import net.minecraft.world.entity.Mob;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class MaiGameEvents {
    private MaiGameEvents() {}

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(MaiTiers.SERVER);
        event.addListener(MaiSkills.SERVER);
        event.addListener(WeaponArts.SERVER);
    }

    @SubscribeEvent
    public static void onReloadComplete(OnDatapackSyncEvent event) {
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
     * Forge does not copy capabilities on its own. Copy the full mai attachment first, then apply the
     * same death trim the 1.21.1 build applied after NeoForge's copyOnDeath.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        event.getOriginal().reviveCaps();
        MaiData previous = MaiAttachments.data(event.getOriginal());
        MaiData mai = MaiApi.data(player);
        mai.deserializeNBT(previous.serializeNBT());
        event.getOriginal().invalidateCaps();
        if (event.isWasDeath()) {
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
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (event.player instanceof ServerPlayer player) {
            MaiRitual.tick(player);
            MaiCombo.tick(player);
            MaiRegen.tick(player);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (event.level instanceof ServerLevel level) {
            MaiWaves.tick(level);
            com.kuiba.mai.MaiFxSequencer.tick(level);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        MaiWaves.clear();
        com.kuiba.mai.MaiFxSequencer.clear();
    }

    /** A player came into view: give the viewer the subject's 脉门 aura so the rings show right away. */
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer viewer)) {
            return;
        }
        if (event.getTarget() instanceof ServerPlayer subject) {
            MaiApi.sendAuraTo(viewer, subject);
            return;
        }
        // 特殊怪: replay its 开脉 pulse so a player walking in sees the same rings and sound.
        if (event.getTarget() instanceof Mob mob && MaiAttachments.isHost(mob)) {
            GateHostData data = MaiAttachments.host(mob);
            if (data.isHost()) {
                MaiGateHosts.pulseToOne(viewer, mob, data.gates());
            }
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
    public static void onDamaged(LivingDamageEvent event) {
        if (event.getAmount() <= 0.0F) {
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
