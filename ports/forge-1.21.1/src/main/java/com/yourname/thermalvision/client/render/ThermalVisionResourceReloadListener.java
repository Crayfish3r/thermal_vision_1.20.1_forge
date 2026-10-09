package com.yourname.thermalvision.client.render;

import com.yourname.thermalvision.ThermalVision;
import com.yourname.thermalvision.client.ThermalVisionPostProcessor;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
    modid = ThermalVision.MOD_ID,
    bus = Mod.EventBusSubscriber.Bus.MOD,
    value = Dist.CLIENT
)
public final class ThermalVisionResourceReloadListener {
    private ThermalVisionResourceReloadListener() {
    }

    @SubscribeEvent
    public static void registerReloadListener(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) resourceManager -> {
            ThermalVisionPostProcessor.onResourceReload();
            ThermalVisionRenderPipeline.onResourceReload();
        });
    }
}
