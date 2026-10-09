package com.yourname.thermalvision.gogglesaudit;

import com.yourname.thermalvision.common.ServerThermalVisionStates;
import com.yourname.thermalvision.common.ThermalVisionAccess;
import com.yourname.thermalvision.compat.ThermalEquipment;
import com.yourname.thermalvision.item.ThermalImagerAccessoryItem;
import com.yourname.thermalvision.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;

/** Dev-only API audit; the menu check discovers the installed menu at runtime. */
public final class CuriosChecks {
    private CuriosChecks() {}
    private static ICurioStacksHandler head(ServerPlayer player) {
        var inv=CuriosApi.getCuriosInventory(player).orElseThrow(()->new AssertionError("Curios inventory missing"));
        var head=inv.getCurios().get("head");
        GogglesChecks.require(head!=null && head.getSlots()>=1,"head slot assigned by our data");
        return head;
    }
    public static void clear(ServerPlayer player) {
        var head=head(player);
        for(int i=0;i<head.getSlots();i++) {
            head.getStacks().setStackInSlot(i,ItemStack.EMPTY);
            head.getCosmeticStacks().setStackInSlot(i,ItemStack.EMPTY);
            head.getRenders().set(i,true);
        }
    }
    public static void equip(ServerPlayer player,ItemStack stack) {
        head(player).getStacks().setStackInSlot(0,stack);
    }
    public static void visibility(ServerPlayer player,boolean visible) {
        head(player).getRenders().set(0,visible);
    }
    public static void initial(ServerPlayer player) {
        var head=head(player);
        ItemStack goggles=new ItemStack(ModItems.THERMAL_GOGGLES.get());
        GogglesChecks.require(head.getStacks().isItemValid(0,goggles),"head tag accepts goggles");
        head.getCosmeticStacks().setStackInSlot(0,goggles);
        GogglesChecks.require(ThermalVisionAccess.getWorkingGoggles(player).isEmpty() && !ServerThermalVisionStates.toggle(player).active(),"cosmetic-only goggles cannot activate");
        head.getCosmeticStacks().setStackInSlot(0,ItemStack.EMPTY);
        ThermalImagerAccessoryItem.setThermalUseTicks(goggles,417);
        goggles.getOrCreateTag().putString("AuditExtra","preserved");
        player.setItemSlot(EquipmentSlot.HEAD,goggles);
        ThermalEquipment.migrateLegacyHelmet(player);
        GogglesChecks.require(player.getItemBySlot(EquipmentSlot.HEAD).isEmpty(),"legacy helmet moved out of armor");
        ItemStack migrated=ThermalVisionAccess.getWorkingGoggles(player);
        GogglesChecks.require(ThermalImagerAccessoryItem.getThermalUseTicks(migrated)==417 && "preserved".equals(migrated.getTag().getString("AuditExtra")),"migration preserves charge and other NBT");
        head.getRenders().set(0,false);
        GogglesChecks.require(ThermalEquipment.getRenderIndex(player)==-1,"render toggle respected");
        head.getRenders().set(0,true);
        GogglesChecks.require(ThermalEquipment.getRenderIndex(player)==0,"visible head selects one renderer");
        ItemStack second=new ItemStack(ModItems.THERMAL_GOGGLES.get());
        ThermalImagerAccessoryItem.setThermalUseTicks(second,319);
        player.setItemSlot(EquipmentSlot.HEAD,second);
        for(int i=0;i<36;i++) player.getInventory().setItem(i,new ItemStack(Items.STONE,64));
        ThermalEquipment.migrateLegacyHelmet(player);
        GogglesChecks.require(player.getItemBySlot(EquipmentSlot.HEAD)==second && head.getStacks().getStackInSlot(0)==migrated,"occupied curio and full inventory retain both original stacks");
        player.getInventory().setItem(12,ItemStack.EMPTY);
        ThermalEquipment.migrateLegacyHelmet(player);
        GogglesChecks.require(player.getItemBySlot(EquipmentSlot.HEAD).isEmpty() && ThermalImagerAccessoryItem.getThermalUseTicks(player.getInventory().getItem(12))==319 && head.getStacks().getStackInSlot(0)==migrated,"occupied curio migration moves legacy pair to inventory without eviction");
        player.getInventory().clearContent();
        clear(player);
        player.getInventory().setItem(9,new ItemStack(ModItems.THERMAL_GOGGLES.get()));
        try {
            var type=Class.forName("top.theillusivec4.curios.common.inventory.container.CuriosContainerV2");
            var menu=(net.minecraft.world.inventory.AbstractContainerMenu)type
                .getConstructor(int.class,net.minecraft.world.entity.player.Inventory.class)
                .newInstance(123,player.getInventory());
            GogglesChecks.require(!menu.getSlot(5).mayPlace(new ItemStack(ModItems.THERMAL_GOGGLES.get())),"Curios menu rejects manual armor insertion");
            menu.quickMoveStack(player,9);
            GogglesChecks.require(!ThermalVisionAccess.getWorkingGoggles(player).isEmpty()
                && player.getItemBySlot(EquipmentSlot.HEAD).isEmpty(),"Curios menu shift-click equips functional head instead of armor");
            menu.removed(player);
        } catch (ReflectiveOperationException error) { throw new AssertionError("installed Curios menu audit",error); }
    }
}
