// src/main/java/com/yourname/thermalvision/network/ModNetworking.java
package com.yourname.thermalvision.network;

import com.yourname.thermalvision.common.ThermalVisionAccess;

import com.yourname.thermalvision.common.VisionMode;

import com.yourname.thermalvision.ThermalVision;
import com.yourname.thermalvision.common.ThermalVisionState;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetworking {
    private static final String PROTOCOL_VERSION = "3";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(ThermalVision.MOD_ID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );

    private static int packetId;

    private ModNetworking() {
    }

    public static void register() {
        CHANNEL.registerMessage(
            nextPacketId(),
            ToggleThermalVisionC2SPacket.class,
            ToggleThermalVisionC2SPacket::encode,
            ToggleThermalVisionC2SPacket::decode,
            ToggleThermalVisionC2SPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );

        CHANNEL.registerMessage(
            nextPacketId(),
            SetThermalVisionS2CPacket.class,
            SetThermalVisionS2CPacket::encode,
            SetThermalVisionS2CPacket::decode,
            SetThermalVisionS2CPacket::handle,
            Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(nextPacketId(), GogglesVisualS2CPacket.class,
            GogglesVisualS2CPacket::encode, GogglesVisualS2CPacket::decode,
            GogglesVisualS2CPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void sendToServer(ToggleThermalVisionC2SPacket packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void sendToPlayer(ServerPlayer player, SetThermalVisionS2CPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToPlayer(ServerPlayer player, ThermalVisionState state) {
        sendToPlayer(player, new SetThermalVisionS2CPacket(state, VisionMode.of(ThermalVisionAccess.getWorkingGoggles(player))));
    }

    public static void sendVisual(ServerPlayer wearer, GogglesVisualS2CPacket packet) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> wearer), packet);
    }

    public static void sendVisualTo(ServerPlayer observer, GogglesVisualS2CPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> observer), packet);
    }

    private static int nextPacketId() {
        return packetId++;
    }
}
