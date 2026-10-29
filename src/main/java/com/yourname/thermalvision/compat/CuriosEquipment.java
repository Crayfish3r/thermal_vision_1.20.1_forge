package com.yourname.thermalvision.compat;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.yourname.thermalvision.registry.ModItems;
import java.util.UUID;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

/** Loaded only after the curios mod-presence guard in ThermalEquipment. */
public final class CuriosEquipment {
    private CuriosEquipment() {}

    public static void register() {
        for (var item : java.util.List.of(ModItems.THERMAL_GOGGLES, ModItems.WHITE_HOT_GOGGLES, ModItems.NIGHT_VISION_GOGGLES)) {
        CuriosApi.registerCurio(item.get(), new ICurioItem() {
            @Override
            public boolean canEquip(SlotContext context, ItemStack stack) {
                return "head".equals(context.identifier());
            }

            @Override
            public boolean canEquipFromUse(SlotContext context, ItemStack stack) {
                return "head".equals(context.identifier()) && !context.cosmetic();
            }

            @Override
            public Multimap<Attribute, AttributeModifier> getAttributeModifiers(
                SlotContext context, UUID uuid, ItemStack stack
            ) {
                // Accessories provide no armor or toughness, including custom attribute NBT.
                return ImmutableMultimap.of();
            }
        });
        // Vanilla ArmorItem registers an armor dispenser behavior in its constructor.
        // In accessory mode the dispenser drops the item instead of putting it in armor.
        DispenserBlock.registerBehavior(item.get(), new DefaultDispenseItemBehavior());
        }
    }

    private static ICurioStacksHandler head(LivingEntity wearer) {
        var inventory = CuriosApi.getCuriosInventory(wearer).orElse(null);
        return inventory == null ? null : inventory.getCurios().get("head");
    }

    public static ItemStack getWorkingGoggles(LivingEntity wearer) {
        var head = head(wearer);
        if (head != null) {
            var stacks = head.getStacks();
            for (int i = 0; i < stacks.getSlots(); i++) {
                ItemStack stack = stacks.getStackInSlot(i);
                if (ModItems.isGoggles(stack)) return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /** Single displayed pair, following cosmetic replacement and the render toggle. */
    public static int getRenderIndex(LivingEntity wearer) {
        var head = head(wearer);
        if (head == null) return -1;
        for (int i = 0; i < head.getSlots(); i++) {
            if (i >= head.getRenders().size() || !head.getRenders().get(i)) continue;
            ItemStack cosmetic = head.getCosmeticStacks().getStackInSlot(i);
            ItemStack displayed = cosmetic.isEmpty() ? head.getStacks().getStackInSlot(i) : cosmetic;
            if (ModItems.isGoggles(displayed)) return i;
        }
        return -1;
    }

    public static void migrateLegacyHelmet(ServerPlayer player) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!ModItems.isGoggles(helmet)) return;
        var head = head(player);
        // Curios may not have received slot data yet; retry on a later server tick.
        if (head == null) return;
        var stacks = head.getStacks();
        for (int i = 0; i < stacks.getSlots(); i++) {
            if (!stacks.getStackInSlot(i).isEmpty() || !stacks.isItemValid(i, helmet)) continue;
            ItemStack transfer = helmet.copyWithCount(1);
            if (!stacks.insertItem(i, transfer, true).isEmpty()) continue;
            if (stacks.insertItem(i, transfer, false).isEmpty()) {
                helmet.shrink(1);
                if (helmet.isEmpty()) player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                player.inventoryMenu.broadcastChanges();
                return;
            }
        }
        // Do not evict an accessory or drop a charged item. Transfer to inventory only if
        // the entire stack fits; otherwise leave the legacy helmet intact and inactive.
        if (player.getInventory().getFreeSlot() >= 0) {
            ItemStack transfer = helmet.copy();
            player.getInventory().add(transfer);
            if (transfer.getCount() < helmet.getCount()) {
                player.setItemSlot(EquipmentSlot.HEAD, transfer.isEmpty() ? ItemStack.EMPTY : transfer);
                player.inventoryMenu.broadcastChanges();
            }
        }
    }
}
