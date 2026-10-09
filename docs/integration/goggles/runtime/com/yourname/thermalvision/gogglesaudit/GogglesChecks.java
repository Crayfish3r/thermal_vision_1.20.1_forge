package com.yourname.thermalvision.gogglesaudit;

import com.mojang.logging.LogUtils;
import com.yourname.thermalvision.common.ServerThermalVisionStates;
import com.yourname.thermalvision.common.ThermalVisionAccess;
import com.yourname.thermalvision.compat.ThermalEquipment;
import com.yourname.thermalvision.item.ThermalImagerAccessoryItem;
import com.yourname.thermalvision.network.GogglesVisualS2CPacket;
import com.yourname.thermalvision.registry.ModItems;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

public final class GogglesChecks {
    private GogglesChecks() {}
    public static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        LogUtils.getLogger().info("GOGGLES PASS: {}", message);
    }

    public static void clear(ServerPlayer player) {
        ServerThermalVisionStates.forceDisable(player,0);
        player.getInventory().clearContent();
        if (ThermalEquipment.usesCurios()) CuriosChecks.clear(player);
        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
    }

    public static ItemStack equip(ServerPlayer player) {
        ItemStack stack = new ItemStack(ModItems.THERMAL_GOGGLES.get());
        if (ThermalEquipment.usesCurios()) CuriosChecks.equip(player,stack);
        else player.setItemSlot(EquipmentSlot.HEAD,stack);
        return ThermalVisionAccess.getWorkingGoggles(player);
    }

    public static void tick(ServerPlayer player) {
        ServerThermalVisionStates.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,player));
    }

    public static void run(ServerPlayer player) {
        player.setGameMode(GameType.SURVIVAL);
        clear(player);
        ItemStack sample=new ItemStack(ModItems.THERMAL_GOGGLES.get());
        require(player.inventoryMenu.getSlot(5).mayPlace(sample)==!ThermalEquipment.usesCurios(),"manual armor insertion follows mode");
        require(sample.canEquip(EquipmentSlot.HEAD,player)==!ThermalEquipment.usesCurios(),"Forge canEquip follows mode");
        if (ThermalEquipment.usesCurios()) {
            require(sample.getAttributeModifiers(EquipmentSlot.HEAD).isEmpty(),"no accessory-mode armor attributes");
            CuriosChecks.initial(player);
            clear(player);
        }
        player.setItemInHand(InteractionHand.MAIN_HAND,sample);
        var click=new PlayerInteractEvent.RightClickItem(player,InteractionHand.MAIN_HAND);
        MinecraftForge.EVENT_BUS.post(click);
        if (!click.isCanceled()) sample.getItem().use(player.level(),player,InteractionHand.MAIN_HAND);
        require(!ThermalVisionAccess.getWorkingGoggles(player).isEmpty(),"right click equips the working slot");
        require(!ThermalEquipment.usesCurios() || player.getItemBySlot(EquipmentSlot.HEAD).isEmpty(),"right click leaves armor head free in Curios mode");
        clear(player);
        player.getInventory().setItem(9,new ItemStack(ModItems.THERMAL_GOGGLES.get()));
        player.inventoryMenu.quickMoveStack(player,9);
        require(player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()==ThermalEquipment.usesCurios(),"vanilla shift-click does not bypass mode");
        clear(player);
        ItemStack goggles=equip(player);
        ThermalImagerAccessoryItem.setThermalUseTicks(goggles,501);
        require(ThermalVisionAccess.getWorkingGoggles(player)==goggles,"accessor returns live equipped stack");
        require(ServerThermalVisionStates.toggle(player).active(),"server accepts activation");
        tick(player);
        require(ThermalImagerAccessoryItem.getThermalUseTicks(goggles)==500,"one server tick consumes exactly one charge");
        ItemStack battery=new ItemStack(ModItems.THERMAL_BATTERY.get(),2);
        player.setItemInHand(InteractionHand.MAIN_HAND,battery);
        battery.getItem().use(player.level(),player,InteractionHand.MAIN_HAND);
        require(battery.getCount()==1 && ThermalImagerAccessoryItem.getThermalUseTicks(goggles)==1200,"battery recharges actual working stack once");
        ThermalImagerAccessoryItem.setThermalUseTicks(goggles,1);
        tick(player);
        require(!ServerThermalVisionStates.get(player).active() && ThermalImagerAccessoryItem.getThermalUseTicks(goggles)==0,"depletion disables at zero");
        ThermalImagerAccessoryItem.setThermalUseTicks(goggles,100);
        ServerThermalVisionStates.toggle(player);
        ItemStack replacement=equip(player);
        tick(player);
        require(!ServerThermalVisionStates.get(player).active() && ThermalImagerAccessoryItem.getThermalUseTicks(replacement)==1200,"replacing active goggles disables without draining replacement");
        require(ServerThermalVisionStates.toggle(player).active(),"replacement can activate after confirmation");
        if (ThermalEquipment.usesCurios()) CuriosChecks.clear(player);
        else player.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);
        tick(player);
        require(!ServerThermalVisionStates.get(player).active(),"removal disables active state");
        goggles=equip(player);
        ServerThermalVisionStates.toggle(player);
        ServerThermalVisionStates.onPlayerRespawn(new PlayerEvent.PlayerRespawnEvent(player,false));
        require(!ServerThermalVisionStates.get(player).active(),"respawn disables");
        ServerThermalVisionStates.toggle(player);
        ServerThermalVisionStates.onPlayerChangedDimension(new PlayerEvent.PlayerChangedDimensionEvent(player,player.level().dimension(),player.level().dimension()));
        require(ServerThermalVisionStates.get(player).active(),"dimension state resynchronizes");
        ServerThermalVisionStates.onPlayerLoggedOut(new PlayerEvent.PlayerLoggedOutEvent(player));
        require(!ServerThermalVisionStates.get(player).active(),"logout clears active state");
        ServerThermalVisionStates.onPlayerLoggedIn(new PlayerEvent.PlayerLoggedInEvent(player));
        require(!ServerThermalVisionStates.get(player).active(),"login begins inactive");

        var packet=new GogglesVisualS2CPacket(player.getId(),player.getUUID(),true,true,ThermalEquipment.usesCurios(),0,true);
        FriendlyByteBuf bytes=new FriendlyByteBuf(Unpooled.buffer());
        try { GogglesVisualS2CPacket.encode(packet,bytes); require(packet.equals(GogglesVisualS2CPacket.decode(bytes)),"public visual packet round trip"); }
        finally { bytes.release(); }

        clear(player);
        BlockPos pos=player.blockPosition().offset(0,0,-1);
        player.serverLevel().setBlockAndUpdate(pos,Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING,Direction.SOUTH));
        player.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+1.5);
        ItemStack dispensed=new ItemStack(ModItems.THERMAL_GOGGLES.get());
        var dispenser=(DispenserBlockEntity)player.serverLevel().getBlockEntity(pos);
        dispenser.setItem(0,dispensed);
        ((DispenserBlock)Blocks.DISPENSER).tick(player.serverLevel().getBlockState(pos),
            player.serverLevel(),pos,player.serverLevel().random);
        require(player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()==ThermalEquipment.usesCurios(),"dispenser follows armor/accessory mode");
        clear(player);
        equip(player);
        if (ThermalEquipment.usesCurios()) player.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.IRON_HELMET));
        player.setGameMode(GameType.CREATIVE);
        ServerThermalVisionStates.syncState(player);
    }
}
