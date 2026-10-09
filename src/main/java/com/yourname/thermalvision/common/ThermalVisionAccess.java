// src/main/java/com/yourname/thermalvision/common/ThermalVisionAccess.java
package com.yourname.thermalvision.common;

import com.yourname.thermalvision.item.ThermalImagerAccessoryItem;
import com.yourname.thermalvision.compat.ThermalEquipment;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class ThermalVisionAccess {
    private ThermalVisionAccess() {
    }

    public static boolean canUseThermalVision(ServerPlayer player) {
        ItemStack goggles = getWorkingGoggles(player);
        return player.isAlive() && !player.isSpectator()
            && !goggles.isEmpty() && ThermalImagerAccessoryItem.hasThermalUseRemaining(goggles);
    }

    public static ItemStack getThermalHelmet(ServerPlayer player) {
        return getWorkingGoggles(player);
    }

    public static ItemStack getWorkingGoggles(LivingEntity wearer) {
        return ThermalEquipment.getWorkingGoggles(wearer);
    }

    public static int getRemainingUseTicks(ServerPlayer player) {
        ItemStack goggles = getWorkingGoggles(player);
        return goggles.isEmpty() ? 0 : ThermalImagerAccessoryItem.getThermalUseTicks(goggles);
    }
}
