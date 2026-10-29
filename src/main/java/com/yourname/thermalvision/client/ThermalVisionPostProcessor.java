package com.yourname.thermalvision.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.logging.LogUtils;
import com.yourname.thermalvision.ThermalVision;
import com.yourname.thermalvision.client.render.ThermalPostRenderState;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.slf4j.Logger;

public final class ThermalVisionPostProcessor {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation THERMAL_POST_EFFECT =
        new ResourceLocation(ThermalVision.MOD_ID, "shaders/post/thermal_vision.json");

    private static final String SWAP_TARGET = "thermal_background_swap";
    private static final String OUTPUT_TARGET = "thermal_background_output";
    private static final ThermalPostRenderState RENDER_STATE = new ThermalPostRenderState();
    private static final String FADE_UNIFORM = "Fade";
    private static final float FADE_IN_STEP = 1.0F / 23.0F;
    private static final float FADE_OUT_STEP = 1.0F / 18.0F;

    private static boolean loaded;
    private static boolean failed;
    private static boolean desiredActive;
    private static float fade;
    private static PostChain postChain;
    private static PostPass backgroundPass;
    private static PostPass copyPass;
    private static RenderTarget chainMainTarget;
    private static Uniform fadeUniform;
    private static int lastWidth = -1;
    private static int lastHeight = -1;

    private ThermalVisionPostProcessor() {
    }

    public static void setDesiredActive(boolean shouldBeActive) {
        desiredActive = shouldBeActive;
    }

    public static float effectStrength() {
        return fade;
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        boolean canRender = minecraft.level != null && minecraft.player != null;

        if (!canRender) {
            reset();
            return;
        }

        updateFadeValue();
    }

    public static void render(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (failed || minecraft.level == null || minecraft.player == null) {
            return;
        }
        if (!desiredActive && fade <= 0.001F && postChain == null) {
            return;
        }

        RENDER_STATE.capture();

        try {
            if (!desiredActive && fade <= 0.001F) {
                disable(minecraft);
                return;
            }
            // Create/resize immediately before use, including while game ticks are paused.
            enable(minecraft);
            applyFadeUniform(minecraft);
            if (!loaded) {
                return;
            }
            RenderTarget mainTarget = minecraft.getMainRenderTarget();
            ThermalPostRenderState.requireTarget(mainTarget, lastWidth, lastHeight);
            ThermalPostRenderState.requireTarget(backgroundPass.outTarget, lastWidth, lastHeight);
            ThermalPostRenderState.requireTarget(copyPass.outTarget, lastWidth, lastHeight);
            ThermalPostRenderState.requireNoGlError("Before thermal background pass");
            ThermalPostRenderState.prepareFullscreenPass();
            postChain.process(partialTick);
            ThermalPostRenderState.requireNoGlError("Thermal background/copy pass");
            ThermalPostRenderState.copyColor(copyPass.outTarget, mainTarget);
        } catch (RuntimeException exception) {
            fail(minecraft, "[ThermalVision] Thermal background/copy pass failed; keeping the scene", exception);
        } finally {
            RENDER_STATE.restore();
        }
    }

    public static void resetFailureState() {
        failed = false;
    }

    public static void reset() {
        Minecraft minecraft = Minecraft.getInstance();
        desiredActive = false;
        fade = 0.0F;
        failed = false;

        RENDER_STATE.capture();
        try {
            disable(minecraft);
        } finally {
            RENDER_STATE.restore();
        }
    }

    public static void onResourceReload() {
        Minecraft minecraft = Minecraft.getInstance();
        RENDER_STATE.capture();

        try {
            failed = false;
            disable(minecraft);
        } finally {
            RENDER_STATE.restore();
        }
    }

    private static void updateFadeValue() {
        float target = desiredActive ? 1.0F : 0.0F;

        if (fade < target) {
            fade = Math.min(target, fade + FADE_IN_STEP);
            return;
        }

        if (fade > target) {
            fade = Math.max(target, fade - FADE_OUT_STEP);
        }
    }

    private static void enable(Minecraft minecraft) {
        if (failed) {
            return;
        }

        if (ensurePostChain(minecraft)) {
            loaded = true;
        }
    }

    private static void disable(Minecraft minecraft) {
        if (!loaded && postChain == null) {
            return;
        }

        if (postChain != null) {
            // Also reset EffectInstance's cached program after an interrupted PostPass.
            if (backgroundPass != null) backgroundPass.getEffect().clear();
            if (copyPass != null) copyPass.getEffect().clear();
            postChain.close();
            postChain = null;
        }

        backgroundPass = null;
        copyPass = null;
        chainMainTarget = null;
        fadeUniform = null;
        loaded = false;
        lastWidth = -1;
        lastHeight = -1;
    }

    private static boolean ensurePostChain(Minecraft minecraft) {
        try {
            RenderTarget mainTarget = minecraft.getMainRenderTarget();
            ThermalPostRenderState.requireTarget(mainTarget, mainTarget.width, mainTarget.height);
            if (postChain != null && chainMainTarget != mainTarget) {
                disable(minecraft);
            }
            if (postChain == null) {
                chainMainTarget = mainTarget;
                postChain = new PostChain(
                    minecraft.getTextureManager(),
                    minecraft.getResourceManager(),
                    mainTarget,
                    THERMAL_POST_EFFECT
                );

                postChain.addTempTarget(SWAP_TARGET, mainTarget.width, mainTarget.height);
                postChain.addTempTarget(OUTPUT_TARGET, mainTarget.width, mainTarget.height);
                RenderTarget swapTarget = postChain.getTempTarget(SWAP_TARGET);
                backgroundPass = postChain.addPass(
                    "thermal_vision:thermal_vision",
                    mainTarget,
                    swapTarget
                );
                // PostPass clears its output. Keep every shader output away from main.
                copyPass = postChain.addPass("thermal_vision:thermal_copy", swapTarget,
                    postChain.getTempTarget(OUTPUT_TARGET));
                ThermalPostRenderState.requireUniforms(backgroundPass, "ProjMat", "InSize", "OutSize", "Time", "Fade", "VisionMode");
                ThermalPostRenderState.requireSampler(backgroundPass, "DiffuseSampler");
                ThermalPostRenderState.requireUniforms(copyPass, "ProjMat", "OutSize");
                ThermalPostRenderState.requireSampler(copyPass, "DiffuseSampler");

                fadeUniform = backgroundPass.getEffect().getUniform(FADE_UNIFORM);
                if (fadeUniform == null) {
                    fail(
                        minecraft,
                        "Thermal vision shader does not expose a writable Fade uniform",
                        null
                    );
                    return false;
                }

                LOGGER.debug(
                    "Created thermal vision background post-processing chain at {}x{}",
                    mainTarget.width,
                    mainTarget.height
                );
            }

            int width = mainTarget.width;
            int height = mainTarget.height;
            if (width != lastWidth || height != lastHeight) {
                postChain.resize(width, height);
                lastWidth = width;
                lastHeight = height;
            }

            return true;
        } catch (IOException | RuntimeException exception) {
            fail(minecraft, "[ThermalVision] Failed to initialize thermal post chain", exception);
            return false;
        }
    }

    private static void applyFadeUniform(Minecraft minecraft) {
        if (!loaded) {
            return;
        }

        if (backgroundPass == null || fadeUniform == null) {
            fail(
                minecraft,
                "Thermal vision background pass is unavailable while the effect is active",
                null
            );
            return;
        }

        try {
            backgroundPass.getEffect().getUniform("VisionMode").set((float) ClientThermalVisionState.mode().ordinal());
            fadeUniform.set(fade);
        } catch (RuntimeException exception) {
            fail(minecraft, "Failed to update thermal vision Fade uniform", exception);
        }
    }

    private static void fail(Minecraft minecraft, String message, Throwable exception) {
        if (!failed) {
            String diagnosticMessage = message + " " + describeFramebufferState();
            if (exception == null) {
                LOGGER.error(diagnosticMessage);
            } else {
                LOGGER.error(diagnosticMessage, exception);
            }
        }

        failed = true;
        desiredActive = false;
        fade = 0.0F;
        disable(minecraft);
    }

    private static String describeFramebufferState() {
        FramebufferBinding binding = captureFramebufferBinding();
        return "[draw=" + binding.drawFramebuffer()
            + ", read=" + binding.readFramebuffer()
            + ", size=" + lastWidth + "x" + lastHeight + "]";
    }

    private static FramebufferBinding captureFramebufferBinding() {
        return new FramebufferBinding(
            GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING),
            GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING)
        );
    }

    private record FramebufferBinding(int drawFramebuffer, int readFramebuffer) {
    }

}
