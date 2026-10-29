package com.yourname.thermalvision.compat;

import com.yourname.thermalvision.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

/** The only common entry point into optional Curios classes; no Curios types in this API. */
public final class ThermalEquipment {
    private static final boolean CURIOS = ModList.get().isLoaded("curios");

    private ThermalEquipment() {}

    public static boolean usesCurios() { return CURIOS; }

    public static void register() {
        if (CURIOS) CuriosEquipment.register();
    }

    /** Returns the real equipped stack, never a copy and never a cosmetic stack. */
    public static ItemStack getWorkingGoggles(LivingEntity wearer) {
        if (CURIOS) return CuriosEquipment.getWorkingGoggles(wearer);
        ItemStack stack = wearer.getItemBySlot(EquipmentSlot.HEAD);
        return ModItems.isGoggles(stack) ? stack : ItemStack.EMPTY;
    }

    public static void migrateLegacyHelmet(ServerPlayer player) {
        if (CURIOS) CuriosEquipment.migrateLegacyHelmet(player);
    }

    public static int getRenderIndex(LivingEntity wearer) {
        return CURIOS ? CuriosEquipment.getRenderIndex(wearer) : -1;
    }
}
