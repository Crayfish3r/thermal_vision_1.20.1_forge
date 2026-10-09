package com.yourname.thermalvision.common;

public record ThermalVisionState(boolean active, int remainingTicks, int cooldownTicks) {
    public static final ThermalVisionState INACTIVE = new ThermalVisionState(false, 0, 0);

    public ThermalVisionState {
        remainingTicks = Math.max(remainingTicks, 0);
        cooldownTicks = Math.max(cooldownTicks, 0);
    }
}
