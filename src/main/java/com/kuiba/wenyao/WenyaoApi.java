package com.kuiba.wenyao;

import org.jetbrains.annotations.Nullable;

import com.kuiba.core.KuibaIds;
import com.kuiba.item.KuibaComponents;
import com.kuiba.item.KuibaItems;
import com.kuiba.item.MaiWeaponItem;
import com.kuiba.mai.MaiApi;
import com.kuiba.mai.MaiData;
import com.kuiba.mai.MaiSkill;
import com.kuiba.mai.MaiSkills;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Numbers for the equipped {@code wenyaoSlot}. Below the tier's gate the pendant sits idle.
 * Weapon arts are cast by right-clicking a mod weapon in the main hand.
 */
public final class WenyaoApi {
    public static final java.util.UUID MELEE_MODIFIER = KuibaIds.WENYAO_MELEE;
    private static final float PULSE_RESONANCE = 0.97F;
    private static final float ART_POWER = 1.1F;
    /** Two weave pieces slow decay to 90% of max/90. Not a regen bonus. */
    private static final float WEAVE_DECAY = 0.90F;
    private static final float FOUR_PIECE_ON_HIT = 0.3F;
    private static final float BLADE_HIT = 0.4F;
    private static final float BLADE_HIT_WENYAO = 0.6F;

    private WenyaoApi() {}

    public static @Nullable WenyaoTier slottedTier(Player player) {
        ItemStack stack = MaiApi.data(player).wenyaoSlot();
        if (stack.isEmpty()) {
            return null;
        }
        return KuibaComponents.wenyaoTier(stack);
    }

    /** Tier that is both slotted and allowed by the player's gate. */
    public static @Nullable WenyaoTier activeTier(Player player) {
        WenyaoTier tier = slottedTier(player);
        if (tier == null || !MaiApi.meetsGate(player, tier.minGate())) {
            return null;
        }
        return tier;
    }

    public static float bonusMax(Player player) {
        WenyaoTier tier = activeTier(player);
        return tier == null ? 0.0F : tier.bonusMax();
    }

    public static float costMultiplier(Player player, @Nullable ResourceLocation skillOrArt) {
        float multiplier = 1.0F;
        WenyaoTier tier = activeTier(player);
        if (tier != null) {
            multiplier *= tier.costMultiplier();
        }
        if (skillOrArt != null && isPulseSkill(skillOrArt) && resonating(player)) {
            multiplier *= PULSE_RESONANCE;
        }
        return multiplier;
    }

    /**
     * Decay scale for {@code MaiRegen}. Two-piece weave is {@code 0.90}.
     * The old out-of-combat regen ×1.10 hook now means slower drain, not healing.
     */
    public static float decayMultiplier(Player player) {
        return weavePieces(player) >= 2 ? WEAVE_DECAY : 1.0F;
    }

    /** Name kept. Returns {@link #decayMultiplier}, not a regen rate. */
    @Deprecated
    public static float oocRegenMultiplier(Player player) {
        return decayMultiplier(player);
    }

    /** Xia or higher, a mod weapon in the main hand, and gate at least 4. */
    public static boolean resonating(Player player) {
        WenyaoTier tier = activeTier(player);
        return tier != null
                && tier.atLeast(WenyaoTier.XIA)
                && MaiApi.meetsGate(player, 4)
                && isModWeapon(player.getMainHandItem());
    }

    public static float artPowerScale(Player player) {
        return resonating(player) ? ART_POWER : 1.0F;
    }

    public static void applyMeleeBonus(Player player) {
        AttributeInstance attack = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack == null) {
            return;
        }
        attack.removeModifier(MELEE_MODIFIER);
        WenyaoTier tier = activeTier(player);
        if (tier == null || tier.meleeBonus() <= 0.0F) {
            return;
        }
        attack.addTransientModifier(new AttributeModifier(
                MELEE_MODIFIER,
                "kuiba:wenyao_melee",
                tier.meleeBonus(),
                AttributeModifier.Operation.MULTIPLY_BASE));
    }

    public static void onBladeHit(ServerPlayer player) {
        float amount = slottedTier(player) != null ? BLADE_HIT_WENYAO : BLADE_HIT;
        grant(player, amount);
    }

    public static void onFourPieceHit(ServerPlayer player) {
        if (weavePieces(player) < 4) {
            return;
        }
        grant(player, FOUR_PIECE_ON_HIT);
    }

    public static boolean isModWeapon(ItemStack stack) {
        return stack.getItem() instanceof MaiWeaponItem;
    }

    public static int weavePieces(Player player) {
        int count = 0;
        for (ItemStack stack : player.getArmorSlots()) {
            if (stack.is(KuibaItems.MAI_WEAVE_HELMET.get())
                    || stack.is(KuibaItems.MAI_WEAVE_CHESTPLATE.get())
                    || stack.is(KuibaItems.MAI_WEAVE_LEGGINGS.get())
                    || stack.is(KuibaItems.MAI_WEAVE_BOOTS.get())) {
                count++;
            }
        }
        return count;
    }

    private static boolean isPulseSkill(ResourceLocation id) {
        if (id.getPath().startsWith("pulse_")) {
            return true;
        }
        MaiSkill skill = MaiSkills.get(id);
        return skill != null && "pulse".equals(skill.category());
    }

    private static void grant(ServerPlayer player, float amount) {
        MaiData data = MaiApi.data(player);
        data.setCurrent(data.current() + amount, MaiApi.getMax(player));
        MaiApi.sync(player);
    }
}
