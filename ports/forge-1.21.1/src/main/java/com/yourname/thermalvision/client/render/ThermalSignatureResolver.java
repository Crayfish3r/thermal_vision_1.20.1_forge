package com.yourname.thermalvision.client.render;

import com.yourname.thermalvision.ThermalVision;
import com.yourname.thermalvision.common.ThermalVisionConfig;
import net.minecraft.core.registries.Registries;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class ThermalSignatureResolver {
    private static final TagKey<EntityType<?>> NO_THERMAL_SIGNATURE = TagKey.create(
        Registries.ENTITY_TYPE,
        ResourceLocation.fromNamespaceAndPath(ThermalVision.MOD_ID, "no_thermal_signature")
    );

    private ThermalSignatureResolver() {
    }

    public static boolean shouldRender(LivingEntity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        Player localPlayer = minecraft.player;

        if (localPlayer == null || !entity.isAlive() || entity.isRemoved()) {
            return false;
        }

        if (entity.getType().is(NO_THERMAL_SIGNATURE)) {
            return false;
        }

        if (entity == localPlayer && minecraft.options.getCameraType().isFirstPerson()) {
            return false;
        }

        if (ThermalVisionConfig.IGNORE_INVISIBLE_ENTITIES && entity.isInvisible()) {
            return false;
        }

        if (ThermalVisionConfig.IGNORE_SPECTATORS
            && entity instanceof Player player
            && player.isSpectator()) {
            return false;
        }

        double range = ThermalVisionConfig.DEFAULT_RANGE_BLOCKS;
        return entity.distanceToSqr(localPlayer) <= range * range;
    }

    public static float getHeat(LivingEntity entity, float partialTick) {
        return 1.0F;
    }
}
