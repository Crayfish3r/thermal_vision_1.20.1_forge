// src/main/java/com/yourname/thermalvision/client/ThermalVisionKeybinds.java
package com.yourname.thermalvision.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.yourname.thermalvision.ThermalVision;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(
    modid = ThermalVision.MOD_ID,
    bus = Mod.EventBusSubscriber.Bus.MOD,
    value = Dist.CLIENT
)
public final class ThermalVisionKeybinds {
    public static final Lazy<KeyMapping> TOGGLE_THERMAL_VISION = Lazy.of(() -> new KeyMapping(
        "key.thermal_vision.toggle_thermal_vision",
        KeyConflictContext.IN_GAME,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_V,
        "key.categories.thermal_vision"
    ));

    private ThermalVisionKeybinds() {
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(TOGGLE_THERMAL_VISION.get());
    }
}
