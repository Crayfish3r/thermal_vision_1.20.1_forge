package com.yourname.thermalvision.client;

import com.yourname.thermalvision.common.VisionMode;

import com.yourname.thermalvision.client.render.ThermalVisionRenderPipeline;

public final class ClientThermalVisionState {
    private static boolean active;
    private static VisionMode mode = VisionMode.THERMAL;
    public static VisionMode mode() { return mode; }
    public static void set(boolean active, int remaining, int cooldown, VisionMode nextMode) {
        // Keep the old palette during fade-out after removing/replacing the item.
        if (active) {
            if (mode != nextMode) ThermalVisionPostProcessor.reset();
            mode = nextMode;
        }
        set(active, remaining, cooldown);
    }
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
            ThermalVisionRenderPipeline.resetFailureState();
            ThermalVisionClientSounds.playToggleOn();
        }

        if (active) {
            ThermalVisionClientSounds.ensureLoopPlaying();
        } else if (wasActive) {
            ThermalVisionClientSounds.stopLoop();
        }

        ThermalVisionPostProcessor.setDesiredActive(active);
    }

    public static void reset() {
        active = false;
        mode = VisionMode.THERMAL;
        remainingTicks = 0;
        cooldownTicks = 0;

        ThermalVisionClientSounds.stopLoop();
        ThermalVisionPostProcessor.reset();
        ThermalVisionRenderPipeline.reset();
        ThermalVisionWorldRenderer.reset();
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
