package com.yourname.thermalvision.client.render;

import com.yourname.thermalvision.ThermalVision;
import java.io.IOException;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
    modid = ThermalVision.MOD_ID,
    bus = Mod.EventBusSubscriber.Bus.MOD,
    value = Dist.CLIENT
)
public final class ThermalVisionShaders {
    private static ShaderInstance entityMaskShader;

    private ThermalVisionShaders() {
    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(
            new ShaderInstance(
                event.getResourceProvider(),
                ResourceLocation.fromNamespaceAndPath(ThermalVision.MOD_ID, "thermal_entity_mask"),
                DefaultVertexFormat.NEW_ENTITY
            ),
            shader -> entityMaskShader = shader
        );
    }

    public static ShaderInstance getEntityMaskShader() {
        return entityMaskShader;
    }
}
