// src/main/java/com/yourname/thermalvision/ThermalVision.java
package com.yourname.thermalvision;

import com.yourname.thermalvision.network.ModNetworking;
import com.yourname.thermalvision.compat.ThermalEquipment;
import com.yourname.thermalvision.registry.ModItems;
import com.yourname.thermalvision.registry.ModSounds;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ThermalVision.MOD_ID)
public final class ThermalVision {
    public static final String MOD_ID = "thermal_vision";

    public ThermalVision() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModItems.register(modBus);
        ModSounds.register(modBus);
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::addCreativeTabItems);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModNetworking.register();
            ThermalEquipment.register();
        });
    }

    private void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT
            || event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.THERMAL_GOGGLES.get());
        }

        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS
            || event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.THERMAL_BATTERY.get());
        }
    }
}
