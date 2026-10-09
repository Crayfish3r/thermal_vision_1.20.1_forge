package com.yourname.thermalvision.registry;

import com.yourname.thermalvision.ThermalVision;
import java.util.EnumMap;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModArmorMaterials {
    private static final DeferredRegister<ArmorMaterial> MATERIALS =
        DeferredRegister.create(Registries.ARMOR_MATERIAL, ThermalVision.MOD_ID);

    public static final RegistryObject<ArmorMaterial> THERMAL_GOGGLES =
        MATERIALS.register("thermal_goggles", () -> new ArmorMaterial(
            Util.make(new EnumMap<>(ArmorItem.Type.class), defense -> {
                defense.put(ArmorItem.Type.BOOTS, 0);
                defense.put(ArmorItem.Type.LEGGINGS, 0);
                defense.put(ArmorItem.Type.CHESTPLATE, 0);
                defense.put(ArmorItem.Type.HELMET, 2);
            }),
            9,
            SoundEvents.ARMOR_EQUIP_IRON,
            () -> Ingredient.of(ModItems.THERMAL_BATTERY.get()),
            List.of(new ArmorMaterial.Layer(
                ResourceLocation.fromNamespaceAndPath(ThermalVision.MOD_ID, "thermal_goggles")
            )),
            0.0F,
            0.0F
        ));

    private ModArmorMaterials() {
    }

    public static void register(IEventBus modBus) {
        MATERIALS.register(modBus);
    }
}
