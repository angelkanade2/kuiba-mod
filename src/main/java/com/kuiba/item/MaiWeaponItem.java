package com.kuiba.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.kuiba.core.KuibaIds;
import com.kuiba.wenyao.WeaponArts;
import com.kuiba.wenyao.WenyaoApi;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeMod;

/** Mod weapon. Right-click in the main hand casts its weapon art. There is no second key. */
public class MaiWeaponItem extends Item {
    public enum Kind {
        BLADE,
        SPEAR
    }

    private final Kind kind;
    private final ResourceLocation defaultArt;
    private final double damageBonus;
    private final double attackSpeed;
    private final double reach;

    public MaiWeaponItem(Kind kind, ResourceLocation defaultArt, double damageBonus, double attackSpeed, double reach, Properties properties) {
        super(properties);
        this.kind = kind;
        this.defaultArt = defaultArt;
        this.damageBonus = damageBonus;
        this.attackSpeed = attackSpeed;
        this.reach = reach;
    }

    public Kind kind() {
        return kind;
    }

    public ResourceLocation defaultArt() {
        return defaultArt;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(stack);
        }
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        if (player instanceof ServerPlayer server) {
            WeaponArts.tryCast(server);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (kind == Kind.BLADE && attacker instanceof ServerPlayer player) {
            WenyaoApi.onBladeHit(player);
        }
        stack.hurtAndBreak(1, attacker, living -> living.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        return true;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot != EquipmentSlot.MAINHAND) {
            return super.getAttributeModifiers(slot, stack);
        }
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", damageBonus, AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(
                BASE_ATTACK_SPEED_UUID, "Weapon modifier", attackSpeed, AttributeModifier.Operation.ADDITION));
        if (reach != 0.0D) {
            builder.put(ForgeMod.ENTITY_REACH.get(), new AttributeModifier(
                    KuibaIds.SPEAR_REACH, "Spear reach", reach, AttributeModifier.Operation.ADDITION));
        }
        return builder.build();
    }
}
