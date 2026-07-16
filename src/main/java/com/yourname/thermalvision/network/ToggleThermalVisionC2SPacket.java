// src/main/java/com/yourname/thermalvision/network/ToggleThermalVisionC2SPacket.java
package com.yourname.thermalvision.network;

import com.yourname.thermalvision.common.ServerThermalVisionStates;
import com.yourname.thermalvision.common.ThermalVisionAccess;
import com.yourname.thermalvision.common.ThermalVisionConfig;
import com.yourname.thermalvision.common.ThermalVisionState;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class ToggleThermalVisionC2SPacket {
    public ToggleThermalVisionC2SPacket() {
    }

    public static void encode(ToggleThermalVisionC2SPacket packet, FriendlyByteBuf buffer) {
    }

    public static ToggleThermalVisionC2SPacket decode(FriendlyByteBuf buffer) {
        return new ToggleThermalVisionC2SPacket();
    }

    public static void handle(
        ToggleThermalVisionC2SPacket packet,
        Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();

        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            ThermalVisionState currentState = ServerThermalVisionStates.get(player);
            ThermalVisionState state;
            if (currentState.active() || ThermalVisionAccess.canUseThermalVision(player)) {
                state = ServerThermalVisionStates.toggle(player);
            } else {
                state = ServerThermalVisionStates.forceDisable(player, ThermalVisionConfig.DENIED_COOLDOWN_TICKS);
                player.displayClientMessage(Component.translatable("message.thermal_vision.goggles_unavailable"), true);
            }

            ModNetworking.sendToPlayer(player, state);
        });

        context.setPacketHandled(true);
    }
}
