package com.kuiba.item;

import com.kuiba.wenyao.WenyaoTier;

import net.minecraft.world.item.Item;

/** Pendant whose rank is part of the item, the 1.20.1 stand-in for the wenyao_tier component. */
public class WenyaoPendantItem extends Item {
    private final WenyaoTier tier;

    public WenyaoPendantItem(WenyaoTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public WenyaoTier tier() {
        return tier;
    }
}
