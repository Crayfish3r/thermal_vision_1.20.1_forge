// src/main/java/com/yourname/thermalvision/registry/ModSounds.java
package com.yourname.thermalvision.registry;

import com.yourname.thermalvision.ThermalVision;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
        DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ThermalVision.MOD_ID);

    public static final RegistryObject<SoundEvent> THERMAL_GOGGLES_ON =
        register("thermal_goggles_on");

    public static final RegistryObject<SoundEvent> THERMAL_GOGGLES_LOOP =
        register("thermal_goggles_loop");

    public static final RegistryObject<SoundEvent> THERMAL_GOGGLES_BREAK =
        register("thermal_goggles_break");

    private ModSounds() {
    }

    public static void register(IEventBus modBus) {
        SOUND_EVENTS.register(modBus);
    }

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUND_EVENTS.register(
            name,
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(ThermalVision.MOD_ID, name))
        );
    }
}
