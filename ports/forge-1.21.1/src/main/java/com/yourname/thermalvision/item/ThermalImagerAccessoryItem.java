// src/main/java/com/yourname/thermalvision/item/ThermalImagerAccessoryItem.java
package com.yourname.thermalvision.item;

import com.yourname.thermalvision.common.ThermalVisionConfig;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

public final class ThermalImagerAccessoryItem extends ArmorItem {
    private static final String THERMAL_USE_TICKS_TAG = "ThermalUseTicks";

    public ThermalImagerAccessoryItem(Holder<ArmorMaterial> material, Type type, Properties properties) {
        super(material, type, properties);
    }

    public static int getThermalUseTicks(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag == null || !tag.contains(THERMAL_USE_TICKS_TAG)) {
            return ThermalVisionConfig.TOTAL_USE_TICKS;
        }

        return clampThermalUseTicks(tag.getInt(THERMAL_USE_TICKS_TAG));
    }

    public static boolean hasThermalUseRemaining(ItemStack stack) {
        return getThermalUseTicks(stack) > 0;
    }

    public static int consumeThermalUseTick(ItemStack stack) {
        int remainingTicks = clampThermalUseTicks(getThermalUseTicks(stack) - 1);
        setThermalUseTicks(stack, remainingTicks);
        return remainingTicks;
    }

    public static void setThermalUseTicks(ItemStack stack, int ticks) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack,
            tag -> tag.putInt(THERMAL_USE_TICKS_TAG, clampThermalUseTicks(ticks)));
    }

    private static int clampThermalUseTicks(int ticks) {
        return Math.min(ThermalVisionConfig.TOTAL_USE_TICKS, Math.max(0, ticks));
    }

    @Override
    public void appendHoverText(
        ItemStack stack,
        Item.TooltipContext context,
        List<Component> tooltip,
        TooltipFlag flag
    ) {
        int seconds = Math.max(0, (getThermalUseTicks(stack) + 19) / 20);

        tooltip.add(Component.translatable("tooltip.thermal_vision.thermal_goggles")
            .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.thermal_vision.thermal_goggles.range")
            .withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.translatable("tooltip.thermal_vision.thermal_goggles.use_time", seconds)
            .withStyle(seconds > 0 ? ChatFormatting.GOLD : ChatFormatting.RED));
    }
}
