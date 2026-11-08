package com.kuiba.item;

import java.util.function.Supplier;

import com.kuiba.Kuiba;
import com.kuiba.core.GateHostData;
import com.kuiba.wenyao.WenyaoTier;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class KuibaItems {
    private static final int WEAVE_DURABILITY = 18;
    private static final int[] SLOT_DURABILITY = new int[] {13, 15, 16, 11};

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Kuiba.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB, Kuiba.MOD_ID);

    public static final ArmorMaterial MAI_WEAVE = new ArmorMaterial() {
        @Override
        public int getDurabilityForType(ArmorItem.Type type) {
            return SLOT_DURABILITY[type.getSlot().getIndex()] * WEAVE_DURABILITY;
        }

        @Override
        public int getDefenseForType(ArmorItem.Type type) {
            return switch (type) {
                case HELMET -> 3;
                case CHESTPLATE -> 6;
                case LEGGINGS -> 5;
                case BOOTS -> 2;
            };
        }

        @Override
        public int getEnchantmentValue() {
            return 12;
        }

        @Override
        public SoundEvent getEquipSound() {
            return SoundEvents.ARMOR_EQUIP_LEATHER;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(MAI_INGOT.get());
        }

        @Override
        public String getName() {
            return Kuiba.MOD_ID + ":mai_weave";
        }

        @Override
        public float getToughness() {
            return 0.0F;
        }

        @Override
        public float getKnockbackResistance() {
            return 0.0F;
        }
    };

    public static final RegistryObject<Item> MAI_CRYSTAL = simple("mai_crystal");
    public static final RegistryObject<Item> MAI_SHARD = simple("mai_shard");
    public static final RegistryObject<Item> MAI_CORE = simple("mai_core");
    public static final RegistryObject<Item> MAI_INGOT = simple("mai_ingot");
    public static final RegistryObject<Item> WENYAO_DUST = simple("wenyao_dust");
    public static final RegistryObject<Item> WENYAO_BLANK = simple("wenyao_blank");
    public static final RegistryObject<ChongMaiDanItem> CHONG_MAI_DAN = ITEMS.register(
            "chong_mai_dan",
            () -> new ChongMaiDanItem(new Item.Properties()));

    public static final RegistryObject<Item> WENYAO_COMMON = pendant("wenyao_common", WenyaoTier.COMMON);
    public static final RegistryObject<Item> WENYAO_XIA = pendant("wenyao_xia", WenyaoTier.XIA);
    public static final RegistryObject<Item> WENYAO_JIANG = pendant("wenyao_jiang", WenyaoTier.JIANG);
    public static final RegistryObject<Item> WENYAO_WANG = pendant("wenyao_wang", WenyaoTier.WANG);

    public static final RegistryObject<ArmorItem> MAI_WEAVE_HELMET = armor("mai_weave_helmet", ArmorItem.Type.HELMET);
    public static final RegistryObject<ArmorItem> MAI_WEAVE_CHESTPLATE = armor("mai_weave_chestplate", ArmorItem.Type.CHESTPLATE);
    public static final RegistryObject<ArmorItem> MAI_WEAVE_LEGGINGS = armor("mai_weave_leggings", ArmorItem.Type.LEGGINGS);
    public static final RegistryObject<ArmorItem> MAI_WEAVE_BOOTS = armor("mai_weave_boots", ArmorItem.Type.BOOTS);

    public static final RegistryObject<MaiWeaponItem> BLADE_PULSE_RIFT = ITEMS.register("blade_pulse_rift", () -> new MaiWeaponItem(
            MaiWeaponItem.Kind.BLADE,
            Kuiba.id("art_rift_flash"),
            5.5D,
            -2.4D,
            0.0D,
            new Item.Properties().durability(312)));

    public static final RegistryObject<MaiWeaponItem> SPEAR_MAI_CONDENSE = ITEMS.register("spear_mai_condense", () -> new MaiWeaponItem(
            MaiWeaponItem.Kind.SPEAR,
            Kuiba.id("art_pierce_mai"),
            4.5D,
            -2.8D,
            0.8D,
            new Item.Properties().durability(280)));

    public static final RegistryObject<GateHostSpawnEgg> SPECIAL_ZOMBIE_EGG = egg(
            "special_zombie_spawn_egg", EntityType.ZOMBIE, GateHostData.Kind.SHARD, 1, 4);
    public static final RegistryObject<GateHostSpawnEgg> SPECIAL_SKELETON_EGG = egg(
            "special_skeleton_spawn_egg", EntityType.SKELETON, GateHostData.Kind.SHARD, 1, 4);
    public static final RegistryObject<GateHostSpawnEgg> SPECIAL_ZOMBIFIED_PIGLIN_EGG = egg(
            "special_zombified_piglin_spawn_egg", EntityType.ZOMBIFIED_PIGLIN, GateHostData.Kind.CRYSTAL, 1, 6);
    public static final RegistryObject<GateHostSpawnEgg> SPECIAL_PIGLIN_EGG = egg(
            "special_piglin_spawn_egg", EntityType.PIGLIN, GateHostData.Kind.CRYSTAL, 1, 6);
    public static final RegistryObject<GateHostSpawnEgg> SPECIAL_ENDERMAN_EGG = egg(
            "special_enderman_spawn_egg", EntityType.ENDERMAN, GateHostData.Kind.CORE, 6, 12);

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("kuiba", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.kuiba"))
            .icon(() -> new ItemStack(MAI_CRYSTAL.get()))
            .displayItems((parameters, output) -> {
                accept(output, MAI_CRYSTAL);
                accept(output, MAI_SHARD);
                accept(output, MAI_CORE);
                accept(output, MAI_INGOT);
                accept(output, CHONG_MAI_DAN);
                accept(output, WENYAO_DUST);
                accept(output, WENYAO_BLANK);
                accept(output, WENYAO_COMMON);
                accept(output, WENYAO_XIA);
                accept(output, WENYAO_JIANG);
                accept(output, WENYAO_WANG);
                accept(output, BLADE_PULSE_RIFT);
                accept(output, SPEAR_MAI_CONDENSE);
                accept(output, MAI_WEAVE_HELMET);
                accept(output, MAI_WEAVE_CHESTPLATE);
                accept(output, MAI_WEAVE_LEGGINGS);
                accept(output, MAI_WEAVE_BOOTS);
                accept(output, SPECIAL_ZOMBIE_EGG);
                accept(output, SPECIAL_SKELETON_EGG);
                accept(output, SPECIAL_ZOMBIFIED_PIGLIN_EGG);
                accept(output, SPECIAL_PIGLIN_EGG);
                accept(output, SPECIAL_ENDERMAN_EGG);
            })
            .build());

    private KuibaItems() {}

    private static RegistryObject<Item> simple(String name) {
        return ITEMS.register(name, () -> new Item(new Item.Properties()));
    }

    private static RegistryObject<Item> pendant(String name, WenyaoTier tier) {
        return ITEMS.register(name, () -> new WenyaoPendantItem(tier, new Item.Properties()));
    }

    private static RegistryObject<ArmorItem> armor(String name, ArmorItem.Type type) {
        return ITEMS.register(name, () -> new ArmorItem(MAI_WEAVE, type, new Item.Properties()));
    }

    private static RegistryObject<GateHostSpawnEgg> egg(String name, EntityType<? extends net.minecraft.world.entity.Mob> type,
            GateHostData.Kind kind, int minGates, int maxGates) {
        return ITEMS.register(name, () -> new GateHostSpawnEgg(type, kind, minGates, maxGates, new Item.Properties()));
    }

    private static void accept(CreativeModeTab.Output output, Supplier<? extends Item> item) {
        output.accept(item.get());
    }

    /** Kept so armor slot math stays next to the material. Unused by callers. */
    @SuppressWarnings("unused")
    private static int durability(EquipmentSlot slot) {
        return SLOT_DURABILITY[slot.getIndex()] * WEAVE_DURABILITY;
    }
}
