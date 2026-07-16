package com.yourname.thermalvision.client;

public final class ClientThermalVisionState {
    private static boolean active;
    private static int remainingTicks;
    private static int cooldownTicks;

    private ClientThermalVisionState() {
    }

    public static boolean isActive() {
        return active;
    }

    public static int remainingTicks() {
        return remainingTicks;
    }

    public static int cooldownTicks() {
        return cooldownTicks;
    }

    public static void set(boolean nextActive, int nextRemainingTicks, int nextCooldownTicks) {
        boolean wasActive = active;

        active = nextActive && nextRemainingTicks > 0;
        remainingTicks = Math.max(nextRemainingTicks, 0);
        cooldownTicks = Math.max(nextCooldownTicks, 0);

        if (!wasActive && active) {
            ThermalVisionPostProcessor.resetFailureState();
            ThermalVisionClientSounds.playToggleOn();
        }

        if (active) {
            ThermalVisionClientSounds.ensureLoopPlaying();
        } else if (wasActive) {
            ThermalVisionClientSounds.stopLoop();
        }

        ThermalVisionPostProcessor.setDesiredActive(active);
        ThermalVisionPostProcessor.tick();
    }

    public static void tick() {
        if (active && remainingTicks > 0) {
            remainingTicks--;
            ThermalVisionClientSounds.ensureLoopPlaying();
        }

        if (cooldownTicks > 0) {
            cooldownTicks--;
        }

        if (active && remainingTicks == 0) {
            active = false;
            ThermalVisionClientSounds.stopLoop();
        }

        if (!active) {
            ThermalVisionClientSounds.stopLoop();
        }

        ThermalVisionPostProcessor.setDesiredActive(active);
        ThermalVisionPostProcessor.tick();
    }
}
