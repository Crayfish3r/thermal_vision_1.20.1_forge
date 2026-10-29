// src/main/java/com/yourname/thermalvision/item/ThermalImagerAccessoryItem.java
package com.yourname.thermalvision.item;

import com.yourname.thermalvision.common.VisionMode;

import com.yourname.thermalvision.common.ThermalVisionConfig;
import com.yourname.thermalvision.compat.ThermalEquipment;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public final class ThermalImagerAccessoryItem extends ArmorItem {
    private static final String THERMAL_USE_TICKS_TAG = "ThermalUseTicks";

    private final VisionMode mode;
    public VisionMode mode() { return mode; }

    public ThermalImagerAccessoryItem(ArmorMaterial material, Type type, Properties properties,
            VisionMode mode) {
        super(material, type, properties);
        this.mode = mode;
    }

    public ThermalImagerAccessoryItem(ArmorMaterial material, Type type, Properties properties) {
        this(material, type, properties, VisionMode.THERMAL);
    }

    public static int getThermalUseTicks(ItemStack stack) {
        CompoundTag tag = stack.getTag();
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
        stack.getOrCreateTag().putInt(THERMAL_USE_TICKS_TAG, clampThermalUseTicks(ticks));
    }

    private static int clampThermalUseTicks(int ticks) {
        return Math.min(ThermalVisionConfig.TOTAL_USE_TICKS, Math.max(0, ticks));
    }

    @Override
    public boolean canEquip(ItemStack stack, EquipmentSlot slot, Entity entity) {
        return !ThermalEquipment.usesCurios() && slot == EquipmentSlot.HEAD;
    }

    @Override
    public EquipmentSlot getEquipmentSlot(ItemStack stack) {
        // Prevent vanilla shift-click from choosing the armor slot in accessory mode.
        return ThermalEquipment.usesCurios() ? EquipmentSlot.MAINHAND : EquipmentSlot.HEAD;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        // Curios handles successful accessory auto-equip in its RightClickItem event.
        return ThermalEquipment.usesCurios()
            ? InteractionResultHolder.pass(player.getItemInHand(hand))
            : super.use(level, player, hand);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return ThermalEquipment.usesCurios() ? ImmutableMultimap.of() : super.getDefaultAttributeModifiers(slot);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        return ThermalEquipment.usesCurios() ? ImmutableMultimap.of() : super.getAttributeModifiers(slot, stack);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        com.yourname.thermalvision.client.model.ThermalGogglesClient.initialize(consumer);
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return entity.level().isClientSide
            ? com.yourname.thermalvision.client.model.ThermalGogglesClient.texturePath(entity, stack)
            : "thermal_vision:textures/models/goggles/thermal_goggles_off.png";
    }

    @Override
    public void appendHoverText(
        ItemStack stack,
        @Nullable Level level,
        List<Component> tooltip,
        TooltipFlag flag
    ) {
        int seconds = Math.max(0, (getThermalUseTicks(stack) + 19) / 20);

        tooltip.add(Component.translatable(ThermalEquipment.usesCurios()
            ? "tooltip.thermal_vision.thermal_goggles.curios" : "tooltip.thermal_vision.thermal_goggles")
            .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(mode.thermal() ? "tooltip.thermal_vision.thermal_goggles.range" : "tooltip.thermal_vision.night_vision")
            .withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.translatable("tooltip.thermal_vision.thermal_goggles.use_time", seconds)
            .withStyle(seconds > 0 ? ChatFormatting.GOLD : ChatFormatting.RED));
    }
}
