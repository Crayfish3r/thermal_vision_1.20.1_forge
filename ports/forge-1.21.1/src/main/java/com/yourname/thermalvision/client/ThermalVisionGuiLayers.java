package com.yourname.thermalvision.client;

import com.yourname.thermalvision.ThermalVision;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.gui.overlay.ForgeLayeredDraw;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ThermalVision.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ThermalVisionGuiLayers {
    private static final ResourceLocation WORLD_EFFECT =
        ResourceLocation.fromNamespaceAndPath(ThermalVision.MOD_ID, "world_effect");
    private static final ResourceLocation HUD =
        ResourceLocation.fromNamespaceAndPath(ThermalVision.MOD_ID, "hud");

    private ThermalVisionGuiLayers() {
    }

    @SubscribeEvent
    public static void addLayers(AddGuiOverlayLayersEvent event) {
        ForgeLayeredDraw layers = event.getLayeredDraw();
        layers.add(ForgeLayeredDraw.VANILLA_ROOT, WORLD_EFFECT, (graphics, delta) -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (!minecraft.options.hideGui || minecraft.screen != null) {
                ThermalVisionClientEvents.renderFinalThermalFrame(
                    delta.getGameTimeDeltaPartialTick(false));
            }
        });
        layers.putBelow(ForgeLayeredDraw.VANILLA_ROOT, WORLD_EFFECT,
            ForgeLayeredDraw.PRE_SLEEP_STACK);
        layers.add(ForgeLayeredDraw.PRE_SLEEP_STACK, HUD,
            (graphics, delta) -> ThermalVisionHudOverlay.render(graphics));
        layers.putAbove(ForgeLayeredDraw.PRE_SLEEP_STACK, HUD, ForgeLayeredDraw.HOTBAR);
    }
}
