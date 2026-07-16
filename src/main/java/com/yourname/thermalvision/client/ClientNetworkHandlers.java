// src/main/java/com/yourname/thermalvision/client/ClientNetworkHandlers.java
package com.yourname.thermalvision.client;

import com.yourname.thermalvision.network.SetThermalVisionS2CPacket;

public final class ClientNetworkHandlers {
    private ClientNetworkHandlers() {
    }

    public static void handleThermalState(SetThermalVisionS2CPacket packet) {
        ClientThermalVisionState.set(packet.active(), packet.remainingTicks(), packet.cooldownTicks());
    }
}
