package com.yourname.thermalvision.item;

import com.yourname.thermalvision.common.ThermalVisionConfig;
import com.yourname.thermalvision.common.ServerThermalVisionStates;
import com.yourname.thermalvision.common.ThermalVisionAccess;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public final class ThermalBatteryItem extends Item {
    public ThermalBatteryItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack battery = player.getItemInHand(hand);
        ItemStack goggles = ThermalVisionAccess.getWorkingGoggles(player);

        if (goggles.isEmpty()) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.thermal_vision.battery_no_goggles"), true);
            }
            return InteractionResultHolder.fail(battery);
        }

        if (ThermalImagerAccessoryItem.getThermalUseTicks(goggles) >= ThermalVisionConfig.TOTAL_USE_TICKS) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.thermal_vision.battery_full"), true);
            }
            return InteractionResultHolder.fail(battery);
        }

        if (!level.isClientSide) {
            ThermalImagerAccessoryItem.setThermalUseTicks(goggles, ThermalVisionConfig.TOTAL_USE_TICKS);
            if (player instanceof ServerPlayer serverPlayer) {
                ServerThermalVisionStates.syncState(serverPlayer);
            }
            if (!player.getAbilities().instabuild) {
                battery.shrink(1);
            }
            player.displayClientMessage(Component.translatable("message.thermal_vision.battery_recharged"), true);
        }

        return InteractionResultHolder.sidedSuccess(battery, level.isClientSide);
    }

    @Override
    public void appendHoverText(
        ItemStack stack,
        @Nullable Level level,
        List<Component> tooltip,
        TooltipFlag flag
    ) {
        tooltip.add(Component.translatable("tooltip.thermal_vision.thermal_battery")
            .withStyle(ChatFormatting.GRAY));
    }
}
