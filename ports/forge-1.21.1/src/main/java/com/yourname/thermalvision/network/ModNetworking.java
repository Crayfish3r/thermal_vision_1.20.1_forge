// src/main/java/com/yourname/thermalvision/network/ModNetworking.java
package com.yourname.thermalvision.network;

import com.yourname.thermalvision.ThermalVision;
import com.yourname.thermalvision.common.ThermalVisionState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

public final class ModNetworking {
    public static final SimpleChannel CHANNEL = ChannelBuilder
        .named(ResourceLocation.fromNamespaceAndPath(ThermalVision.MOD_ID, "main"))
        .networkProtocolVersion(1)
        .simpleChannel();

    private ModNetworking() {
    }

    public static void register() {
        CHANNEL.messageBuilder(ToggleThermalVisionC2SPacket.class, NetworkDirection.PLAY_TO_SERVER)
            .encoder(ToggleThermalVisionC2SPacket::encode)
            .decoder(ToggleThermalVisionC2SPacket::decode)
            .consumerMainThread(ToggleThermalVisionC2SPacket::handle)
            .add();
        CHANNEL.messageBuilder(SetThermalVisionS2CPacket.class, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(SetThermalVisionS2CPacket::encode)
            .decoder(SetThermalVisionS2CPacket::decode)
            .consumerMainThread(SetThermalVisionS2CPacket::handle)
            .add();
        CHANNEL.build();
    }

    public static void sendToServer(ToggleThermalVisionC2SPacket packet) {
        CHANNEL.send(packet, PacketDistributor.SERVER.noArg());
    }

    public static void sendToPlayer(ServerPlayer player, SetThermalVisionS2CPacket packet) {
        CHANNEL.send(packet, PacketDistributor.PLAYER.with(player));
    }

    public static void sendToPlayer(ServerPlayer player, ThermalVisionState state) {
        sendToPlayer(player, new SetThermalVisionS2CPacket(state));
    }

}
