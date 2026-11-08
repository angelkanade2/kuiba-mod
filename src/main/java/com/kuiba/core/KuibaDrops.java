package com.kuiba.core;

import com.kuiba.item.KuibaItems;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;

/**
 * 掉落规则. A zombie killed by a player has a config-driven chance to drop one extra 脉晶.
 * <p>
 * Only player kills count, so zombies that burn at sunrise or drown do not hand out free crystals.
 * Husks, drowned and zombie villagers are all {@link Zombie}s and count; zombified piglins are excluded
 * on purpose, they are Nether mobs and only inherit from {@code Zombie} for the shared AI.
 */
public final class KuibaDrops {
    private KuibaDrops() {}

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie) || zombie instanceof ZombifiedPiglin) {
            return;
        }
        if (zombie.level().isClientSide()) {
            return;
        }
        double chance = KuibaServerConfig.ZOMBIE_CRYSTAL_CHANCE.get();
        if (chance <= 0.0D) {
            return;
        }
        if (!killedByPlayer(event.getSource())) {
            return;
        }
        if (zombie.getRandom().nextDouble() >= chance) {
            return;
        }
        // Add to the drop list only: the vanilla hook spawns everything in it afterwards.
        event.getDrops().add(new ItemEntity(
                zombie.level(),
                zombie.getX(),
                zombie.getY(0.5D),
                zombie.getZ(),
                new ItemStack(KuibaItems.MAI_CRYSTAL.get())));
    }

    /** Direct player hit, or a projectile the player shot. */
    static boolean killedByPlayer(DamageSource source) {
        if (source.getEntity() instanceof Player) {
            return true;
        }
        return source.getEntity() instanceof Projectile projectile && projectile.getOwner() instanceof Player;
    }
}
