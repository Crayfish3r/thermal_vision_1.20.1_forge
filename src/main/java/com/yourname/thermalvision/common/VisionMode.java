package com.yourname.thermalvision.common;

import com.yourname.thermalvision.item.ThermalImagerAccessoryItem;
import net.minecraft.world.item.ItemStack;

/** Stable wire order and shader selector; the server selects this from the equipped item. */
public enum VisionMode {
    THERMAL, WHITE_HOT, NIGHT_VISION;

    public static VisionMode of(ItemStack stack) {
        return stack.getItem() instanceof ThermalImagerAccessoryItem goggles ? goggles.mode() : THERMAL;
    }

    public boolean thermal() { return this != NIGHT_VISION; }
}
