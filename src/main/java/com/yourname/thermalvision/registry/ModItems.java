// src/main/java/com/yourname/thermalvision/registry/ModItems.java
package com.yourname.thermalvision.registry;

import com.yourname.thermalvision.common.VisionMode;

import com.yourname.thermalvision.ThermalVision;
import com.yourname.thermalvision.item.ThermalBatteryItem;
import com.yourname.thermalvision.item.ThermalImagerAccessoryItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, ThermalVision.MOD_ID);

    public static final RegistryObject<Item> THERMAL_BATTERY =
        ITEMS.register("thermal_battery", () -> new ThermalBatteryItem(
            new Item.Properties()
                .stacksTo(16)
                .rarity(Rarity.UNCOMMON)
        ));

    public static final RegistryObject<Item> THERMAL_GOGGLES =
        ITEMS.register("thermal_goggles", () -> new ThermalImagerAccessoryItem(
            ModArmorMaterials.THERMAL_GOGGLES,
            ArmorItem.Type.HELMET,
            new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.UNCOMMON)
        ));

    public static final RegistryObject<Item> WHITE_HOT_GOGGLES = variant("thermal_goggles_white_hot", VisionMode.WHITE_HOT);
    public static final RegistryObject<Item> NIGHT_VISION_GOGGLES = variant("thermal_goggles_night_vision", VisionMode.NIGHT_VISION);

    private static RegistryObject<Item> variant(String id, VisionMode mode) {
        return ITEMS.register(id, () -> new ThermalImagerAccessoryItem(ModArmorMaterials.THERMAL_GOGGLES,
            ArmorItem.Type.HELMET, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON), mode));
    }

    public static boolean isGoggles(net.minecraft.world.item.ItemStack stack) {
        return stack.getItem() instanceof ThermalImagerAccessoryItem;
    }

    private ModItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
