// src/main/java/com/yourname/thermalvision/network/SetThermalVisionS2CPacket.java
package com.yourname.thermalvision.network;

import com.yourname.thermalvision.common.VisionMode;

import com.yourname.thermalvision.common.ThermalVisionState;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public record SetThermalVisionS2CPacket(boolean active, int remainingTicks, int cooldownTicks, VisionMode mode) {
    public SetThermalVisionS2CPacket(ThermalVisionState state, VisionMode mode) {
        this(state.active(), state.remainingTicks(), state.cooldownTicks(), mode);
    }

    public static void encode(SetThermalVisionS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.active);
        buffer.writeVarInt(packet.remainingTicks);
        buffer.writeVarInt(packet.cooldownTicks);
        buffer.writeEnum(packet.mode);
    }

    public static SetThermalVisionS2CPacket decode(FriendlyByteBuf buffer) {
        return new SetThermalVisionS2CPacket(
            buffer.readBoolean(),
            buffer.readVarInt(),
            buffer.readVarInt(),
            buffer.readEnum(VisionMode.class)
        );
    }

    public static void handle(
        SetThermalVisionS2CPacket packet,
        Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();

        context.enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(
                Dist.CLIENT,
                () -> () -> com.yourname.thermalvision.client.ClientNetworkHandlers.handleThermalState(packet)
            )
        );

        context.setPacketHandled(true);
    }
}
