// src/main/java/com/yourname/thermalvision/registry/ModArmorMaterials.java
package com.yourname.thermalvision.registry;

import com.yourname.thermalvision.ThermalVision;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.Util;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

public enum ModArmorMaterials implements ArmorMaterial, StringRepresentable {
    THERMAL_GOGGLES(
        "thermal_goggles",
        Util.make(new EnumMap<>(ArmorItem.Type.class), defenses -> {
            defenses.put(ArmorItem.Type.BOOTS, 0);
            defenses.put(ArmorItem.Type.LEGGINGS, 0);
            defenses.put(ArmorItem.Type.CHESTPLATE, 0);
            defenses.put(ArmorItem.Type.HELMET, 2);
        }),
        9,
        SoundEvents.ARMOR_EQUIP_IRON,
        0.0F,
        0.0F,
        () -> Ingredient.of(ModItems.THERMAL_BATTERY.get())
    );

    private static final int THERMAL_GOGGLES_DURABILITY = 5;

    private final String name;
    private final Map<ArmorItem.Type, Integer> defenseByType;
    private final int enchantmentValue;
    private final SoundEvent equipSound;
    private final float toughness;
    private final float knockbackResistance;
    private final Supplier<Ingredient> repairIngredient;

    ModArmorMaterials(
        String name,
        Map<ArmorItem.Type, Integer> defenseByType,
        int enchantmentValue,
        SoundEvent equipSound,
        float toughness,
        float knockbackResistance,
        Supplier<Ingredient> repairIngredient
    ) {
        this.name = name;
        this.defenseByType = defenseByType;
        this.enchantmentValue = enchantmentValue;
        this.equipSound = equipSound;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
        this.repairIngredient = repairIngredient;
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return THERMAL_GOGGLES_DURABILITY;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return defenseByType.get(type);
    }

    @Override
    public int getEnchantmentValue() {
        return enchantmentValue;
    }

    @Override
    public SoundEvent getEquipSound() {
        return equipSound;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return repairIngredient.get();
    }

    @Override
    public String getName() {
        return ThermalVision.MOD_ID + ":" + name;
    }

    @Override
    public float getToughness() {
        return toughness;
    }

    @Override
    public float getKnockbackResistance() {
        return knockbackResistance;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
