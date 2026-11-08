package com.kuiba.item;

import org.jetbrains.annotations.Nullable;

import com.kuiba.Kuiba;
import com.kuiba.wenyao.WenyaoTier;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * 1.20.1 has no data components. Wenyao tier and weapon art live on the item type, with an optional
 * NBT override so a stack can still carry a different value.
 */
public final class KuibaComponents {
    public static final String WENYAO_TIER = "kuiba_wenyao_tier";
    public static final String WEAPON_ART = "kuiba_weapon_art";

    private KuibaComponents() {}

    public static @Nullable WenyaoTier wenyaoTier(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(WENYAO_TIER)) {
            return WenyaoTier.byName(tag.getString(WENYAO_TIER));
        }
        if (stack.getItem() instanceof WenyaoPendantItem pendant) {
            return pendant.tier();
        }
        return null;
    }

    public static @Nullable ResourceLocation weaponArt(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(WEAPON_ART)) {
            return Kuiba.parse(tag.getString(WEAPON_ART));
        }
        if (stack.getItem() instanceof MaiWeaponItem weapon) {
            return weapon.defaultArt();
        }
        return null;
    }
}
