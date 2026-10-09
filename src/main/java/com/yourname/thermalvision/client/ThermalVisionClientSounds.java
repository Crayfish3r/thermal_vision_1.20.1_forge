// src/main/java/com/yourname/thermalvision/client/ThermalVisionClientSounds.java
package com.yourname.thermalvision.client;

import com.yourname.thermalvision.common.ThermalVisionConfig;
import com.yourname.thermalvision.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;

public final class ThermalVisionClientSounds {
    private static ThermalLoopSound loopSound;

    private ThermalVisionClientSounds() {
    }

    public static void playToggleOn() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        player.playSound(
            ModSounds.THERMAL_GOGGLES_ON.get(),
            ThermalVisionConfig.TOGGLE_ON_SOUND_VOLUME,
            ThermalVisionConfig.TOGGLE_ON_SOUND_PITCH
        );
    }

    public static void ensureLoopPlaying() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (player == null || !ClientThermalVisionState.isActive()) {
            stopLoop();
            return;
        }

        if (loopSound != null && !loopSound.isStopped()) {
            return;
        }

        loopSound = new ThermalLoopSound(player);
        minecraft.getSoundManager().play(loopSound);
    }

    public static void stopLoop() {
        if (loopSound == null) {
            return;
        }

        loopSound.requestStop();
        Minecraft.getInstance().getSoundManager().stop(loopSound);
        loopSound = null;
    }

    private static final class ThermalLoopSound extends AbstractTickableSoundInstance {
        private final LocalPlayer player;

        private ThermalLoopSound(LocalPlayer player) {
            super(ModSounds.THERMAL_GOGGLES_LOOP.get(), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            this.player = player;
            this.looping = true;
            this.delay = 0;
            this.volume = ThermalVisionConfig.LOOP_SOUND_VOLUME;
            this.pitch = ThermalVisionConfig.LOOP_SOUND_PITCH;
            this.relative = false;
            updatePosition();
        }

        @Override
        public void tick() {
            if (player.isRemoved() || !ClientThermalVisionState.isActive()) {
                requestStop();
                return;
            }

            updatePosition();
        }

        private void updatePosition() {
            this.x = player.getX();
            this.y = player.getY();
            this.z = player.getZ();
        }

        private void requestStop() {
            stop();
        }
    }
}
