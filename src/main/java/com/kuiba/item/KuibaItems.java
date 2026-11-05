package com.kuiba.item;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.kuiba.Kuiba;
import com.kuiba.wenyao.WenyaoTier;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class KuibaItems {
    private static final int WEAVE_DURABILITY = 18;

    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, Kuiba.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Kuiba.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Kuiba.MOD_ID);

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> MAI_WEAVE = ARMOR_MATERIALS.register("mai_weave", () -> new ArmorMaterial(
            weaveDefense(),
            12,
            SoundEvents.ARMOR_EQUIP_LEATHER,
            () -> {
                var item = BuiltInRegistries.ITEM.get(Kuiba.id("mai_ingot"));
                return item == null ? Ingredient.EMPTY : Ingredient.of(item);
            },
            List.of(new ArmorMaterial.Layer(Kuiba.id("mai_weave"))),
            0.0F,
            0.0F));

    public static final DeferredItem<Item> MAI_CRYSTAL = ITEMS.registerSimpleItem("mai_crystal");
    public static final DeferredItem<Item> MAI_SHARD = ITEMS.registerSimpleItem("mai_shard");
    public static final DeferredItem<Item> MAI_CORE = ITEMS.registerSimpleItem("mai_core");
    public static final DeferredItem<Item> MAI_INGOT = ITEMS.registerSimpleItem("mai_ingot");
    public static final DeferredItem<Item> WENYAO_DUST = ITEMS.registerSimpleItem("wenyao_dust");
    public static final DeferredItem<Item> WENYAO_BLANK = ITEMS.registerSimpleItem("wenyao_blank");
    public static final DeferredItem<ChongMaiDanItem> CHONG_MAI_DAN = ITEMS.register(
            "chong_mai_dan",
            () -> new ChongMaiDanItem(new Item.Properties()));

    public static final DeferredItem<Item> WENYAO_COMMON = pendant("wenyao_common", WenyaoTier.COMMON);
    public static final DeferredItem<Item> WENYAO_XIA = pendant("wenyao_xia", WenyaoTier.XIA);
    public static final DeferredItem<Item> WENYAO_JIANG = pendant("wenyao_jiang", WenyaoTier.JIANG);
    public static final DeferredItem<Item> WENYAO_WANG = pendant("wenyao_wang", WenyaoTier.WANG);

    public static final DeferredItem<ArmorItem> MAI_WEAVE_HELMET = armor("mai_weave_helmet", ArmorItem.Type.HELMET);
    public static final DeferredItem<ArmorItem> MAI_WEAVE_CHESTPLATE = armor("mai_weave_chestplate", ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<ArmorItem> MAI_WEAVE_LEGGINGS = armor("mai_weave_leggings", ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<ArmorItem> MAI_WEAVE_BOOTS = armor("mai_weave_boots", ArmorItem.Type.BOOTS);

    public static final DeferredItem<MaiWeaponItem> BLADE_PULSE_RIFT = ITEMS.register("blade_pulse_rift", () -> new MaiWeaponItem(
            MaiWeaponItem.Kind.BLADE,
            new Item.Properties()
                    .durability(312)
                    .component(KuibaComponents.WEAPON_ART.get(), Kuiba.id("art_rift_flash"))
                    .attributes(weaponAttributes(5.5D, -2.4D, 0.0D))));

    public static final DeferredItem<MaiWeaponItem> SPEAR_MAI_CONDENSE = ITEMS.register("spear_mai_condense", () -> new MaiWeaponItem(
            MaiWeaponItem.Kind.SPEAR,
            new Item.Properties()
                    .durability(280)
                    .component(KuibaComponents.WEAPON_ART.get(), Kuiba.id("art_pierce_mai"))
                    .attributes(weaponAttributes(4.5D, -2.8D, 0.8D))));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("kuiba", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.kuiba"))
            .icon(() -> MAI_CRYSTAL.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(MAI_CRYSTAL);
                output.accept(MAI_SHARD);
                output.accept(MAI_CORE);
                output.accept(MAI_INGOT);
                output.accept(CHONG_MAI_DAN);
                output.accept(WENYAO_DUST);
                output.accept(WENYAO_BLANK);
                output.accept(WENYAO_COMMON);
                output.accept(WENYAO_XIA);
                output.accept(WENYAO_JIANG);
                output.accept(WENYAO_WANG);
                output.accept(BLADE_PULSE_RIFT);
                output.accept(SPEAR_MAI_CONDENSE);
                output.accept(MAI_WEAVE_HELMET);
                output.accept(MAI_WEAVE_CHESTPLATE);
                output.accept(MAI_WEAVE_LEGGINGS);
                output.accept(MAI_WEAVE_BOOTS);
            })
            .build());

    private KuibaItems() {}

    private static DeferredItem<Item> pendant(String name, WenyaoTier tier) {
        return ITEMS.register(name, () -> new Item(new Item.Properties().component(KuibaComponents.WENYAO_TIER.get(), tier)));
    }

    private static DeferredItem<ArmorItem> armor(String name, ArmorItem.Type type) {
        return ITEMS.register(name, () -> new ArmorItem(
                MAI_WEAVE,
                type,
                new Item.Properties().durability(type.getDurability(WEAVE_DURABILITY))));
    }

    /** Displayed damage is modifier + the player's base 1. Speeds are the vanilla attack-speed modifier. */
    private static ItemAttributeModifiers weaponAttributes(double damageBonus, double speed, double reach) {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, damageBonus, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, speed, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
        if (reach != 0.0D) {
            builder.add(
                    Attributes.ENTITY_INTERACTION_RANGE,
                    new AttributeModifier(Kuiba.id("spear_reach"), reach, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND);
        }
        return builder.build();
    }

    private static Map<ArmorItem.Type, Integer> weaveDefense() {
        EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        defense.put(ArmorItem.Type.HELMET, 3);
        defense.put(ArmorItem.Type.CHESTPLATE, 6);
        defense.put(ArmorItem.Type.LEGGINGS, 5);
        defense.put(ArmorItem.Type.BOOTS, 2);
        defense.put(ArmorItem.Type.BODY, 6);
        return defense;
    }
}
