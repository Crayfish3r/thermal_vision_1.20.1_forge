package com.yourname.thermalvision.client.render;

import com.yourname.thermalvision.ThermalVision;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(
    modid = ThermalVision.MOD_ID,
    bus = Mod.EventBusSubscriber.Bus.MOD,
    value = Dist.CLIENT
)
public final class ThermalEntityLayerRegistration {
    private ThermalEntityLayerRegistration() {
    }

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (EntityType<?> entityType : ForgeRegistries.ENTITY_TYPES.getValues()) {
            if (entityType == EntityType.PLAYER) {
                continue;
            }

            addEntityLayer(event, entityType);
        }

        for (PlayerSkin.Model skin : event.getSkins()) {
            addPlayerLayer(event.getPlayerSkin(skin));
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void addEntityLayer(
        EntityRenderersEvent.AddLayers event,
        EntityType<?> entityType
    ) {
        EntityRenderer renderer = event.getEntityRenderer((EntityType) entityType);
        if (renderer instanceof LivingEntityRenderer livingRenderer) {
            livingRenderer.addLayer(new ThermalEntityLayer(livingRenderer));
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void addPlayerLayer(LivingEntityRenderer renderer) {
        if (renderer != null) {
            renderer.addLayer(new ThermalEntityLayer(renderer));
        }
    }
}
