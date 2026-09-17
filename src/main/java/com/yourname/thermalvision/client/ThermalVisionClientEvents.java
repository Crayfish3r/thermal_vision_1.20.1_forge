// src/main/java/com/yourname/thermalvision/client/ThermalVisionClientEvents.java
package com.yourname.thermalvision.client;

import com.yourname.thermalvision.ThermalVision;
import com.yourname.thermalvision.client.render.ThermalVisionRenderPipeline;
import com.yourname.thermalvision.network.ModNetworking;
import com.yourname.thermalvision.network.ToggleThermalVisionC2SPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
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

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            ClientThermalVisionState.reset();
            return;
        }

        ClientThermalVisionState.tick();

        while (ThermalVisionKeybinds.TOGGLE_THERMAL_VISION.get().consumeClick()) {
            ModNetworking.sendToServer(new ToggleThermalVisionC2SPacket());
        }
    }

    @SubscribeEvent
    public static void onClientLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        ClientThermalVisionState.reset();
    }

    @SubscribeEvent
    public static void onClientLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientThermalVisionState.reset();
    }

    @SubscribeEvent
    public static void onClientPlayerClone(ClientPlayerNetworkEvent.Clone event) {
        ClientThermalVisionState.reset();
    }

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiOverlayEvent.Post event) {
        ThermalVisionHudOverlay.render(event);
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Pre event) {
        renderFinalThermalFrame(event.getPartialTick());
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        ThermalVisionRenderPipeline.prepareFrame(event);
        ThermalVisionWorldRenderer.render(event);

        Minecraft minecraft = Minecraft.getInstance();
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL
            && minecraft.options.hideGui
            && minecraft.screen == null) {
            renderFinalThermalFrame(event.getPartialTick());
        }
    }

    private static void renderFinalThermalFrame(float partialTick) {
        ThermalVisionPostProcessor.render(partialTick);
        ThermalVisionRenderPipeline.composite(partialTick);
    }
}
