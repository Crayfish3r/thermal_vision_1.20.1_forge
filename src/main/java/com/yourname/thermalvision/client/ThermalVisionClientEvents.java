// src/main/java/com/yourname/thermalvision/client/ThermalVisionClientEvents.java
package com.yourname.thermalvision.client;

import com.yourname.thermalvision.ThermalVision;
import com.yourname.thermalvision.network.ModNetworking;
import com.yourname.thermalvision.network.ToggleThermalVisionC2SPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
    modid = ThermalVision.MOD_ID,
    bus = Mod.EventBusSubscriber.Bus.FORGE,
    value = Dist.CLIENT
)
public final class ThermalVisionClientEvents {
    private ThermalVisionClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        ClientThermalVisionState.tick();

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        while (ThermalVisionKeybinds.TOGGLE_THERMAL_VISION.get().consumeClick()) {
            ModNetworking.sendToServer(new ToggleThermalVisionC2SPacket());
        }
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        ThermalVisionHudOverlay.render(event);
    }

    @SubscribeEvent
    public static void onRenderGuiOverlayPre(RenderGuiOverlayEvent.Pre event) {
        ThermalVisionPostProcessor.render(event);
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        ThermalVisionWorldRenderer.render(event);
    }
}
