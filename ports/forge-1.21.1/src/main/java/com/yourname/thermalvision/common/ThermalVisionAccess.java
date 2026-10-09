// src/main/java/com/yourname/thermalvision/common/ThermalVisionAccess.java
package com.yourname.thermalvision.common;

import com.yourname.thermalvision.item.ThermalImagerAccessoryItem;
import com.yourname.thermalvision.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public final class ThermalVisionAccess {
    private ThermalVisionAccess() {
    }

    public static boolean canUseThermalVision(ServerPlayer player) {
        ItemStack goggles = getThermalHelmet(player);
        return !goggles.isEmpty() && ThermalImagerAccessoryItem.hasThermalUseRemaining(goggles);
    }

    public static ItemStack getThermalHelmet(ServerPlayer player) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        return helmet.is(ModItems.THERMAL_GOGGLES.get()) ? helmet : ItemStack.EMPTY;
    }

    public static int getRemainingUseTicks(ServerPlayer player) {
        ItemStack goggles = getThermalHelmet(player);
        return goggles.isEmpty() ? 0 : ThermalImagerAccessoryItem.getThermalUseTicks(goggles);
    }
}
