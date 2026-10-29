package com.yourname.thermalvision.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.BlendMode;
import com.mojang.blaze3d.systems.RenderSystem;
import com.yourname.thermalvision.client.ClientThermalVisionState;
import com.yourname.thermalvision.client.ThermalVisionPostProcessor;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

public final class ThermalVisionRenderPipeline {
    private static final ResourceLocation COMPOSITE_PIPELINE =
        new ResourceLocation("thermal_vision", "shaders/post/thermal_entity_composite_pipeline.json");
    private static final String SCENE_COPY_TARGET = "thermal_scene_copy";
    private static final String OUTPUT_TARGET = "thermal_composite_output";
    private static final ThermalPostRenderState RENDER_STATE = new ThermalPostRenderState();

    private static final Deque<FramebufferBinding> FRAMEBUFFER_STACK = new ArrayDeque<>();

    private static TextureTarget maskTarget;
    private static PostChain compositeChain;
    private static PostPass compositePass;
    private static PostPass sceneCopyPass;
    private static RenderTarget chainMainTarget;
    private static int targetWidth = -1;
    private static int targetHeight = -1;
    private static boolean framePrepared;
    private static boolean failed;
    private static ClientLevel preparedLevel;

    private ThermalVisionRenderPipeline() {
    }

    public static void prepareFrame(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) {
            return;
        }

        restoreLeakedFramebufferBindings("the start of a new frame");

        if (!shouldRenderMask()) {
            framePrepared = false;
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (preparedLevel != minecraft.level) {
            reset();
            preparedLevel = minecraft.level;
        }
        if (minecraft.level == null || minecraft.player == null || failed) {
            framePrepared = false;
            return;
        }

        RenderTarget mainTarget = minecraft.getMainRenderTarget();
        RENDER_STATE.capture();

        try {
            ThermalPostRenderState.requireTarget(mainTarget, mainTarget.width, mainTarget.height);
            ensureTargets(minecraft, mainTarget.width, mainTarget.height);

            ThermalPostRenderState.prepareFullscreenPass();
            // Unlike fullscreen color passes, the mask needs a cleared, writable depth attachment.
            RenderSystem.depthMask(true);
            maskTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            maskTarget.clear(Minecraft.ON_OSX);
            maskTarget.copyDepthFrom(mainTarget);
            ThermalPostRenderState.requireNoGlError("Thermal mask preparation/depth copy");

            framePrepared = true;
        } catch (IOException | RuntimeException exception) {
            failed = true;
            framePrepared = false;
            ThermalVisionRenderLog.error(
                "Failed to prepare thermal entity mask pipeline " + describeFramebufferState(),
                exception
            );
            destroyTargets();
        } finally {
            RENDER_STATE.restore();
        }
    }

    public static void composite(float partialTick) {
        restoreLeakedFramebufferBindings("the entity-mask composite pass");

        if (!framePrepared
            || maskTarget == null
            || compositeChain == null
            || failed) {
            return;
        }

        RENDER_STATE.capture();

        try {
            RenderTarget mainTarget = Minecraft.getInstance().getMainRenderTarget();
            if (mainTarget != chainMainTarget) {
                throw new IllegalStateException("Main framebuffer changed after thermal mask preparation");
            }
            ThermalPostRenderState.requireTarget(mainTarget, targetWidth, targetHeight);
            ThermalPostRenderState.requireTarget(maskTarget, targetWidth, targetHeight);
            ThermalPostRenderState.requireTarget(sceneCopyPass.outTarget, targetWidth, targetHeight);
            ThermalPostRenderState.requireTarget(compositePass.outTarget, targetWidth, targetHeight);
            ThermalPostRenderState.requireNoGlError("Before thermal entity composite");
            ThermalPostRenderState.prepareFullscreenPass();
            compositePass.getEffect().getUniform("VisionMode").set((float) ClientThermalVisionState.mode().ordinal());
            compositeChain.process(partialTick);
            ThermalPostRenderState.requireNoGlError("Thermal entity composite");
            ThermalPostRenderState.copyColor(compositePass.outTarget, mainTarget);
        } catch (RuntimeException exception) {
            failed = true;
            ThermalVisionRenderLog.error(
                "[ThermalVision] Thermal copy/composite pass failed; keeping the background " + describeFramebufferState(),
                exception
            );
            destroyTargets();
        } finally {
            framePrepared = false;
            RENDER_STATE.restore();
        }
    }

    public static boolean isMaskRenderingEnabled() {
        return framePrepared
            && maskTarget != null
            && ThermalVisionShaders.getEntityMaskShader() != null
            && shouldRenderMask();
    }

    public static void bindMaskTarget() {
        if (!isMaskRenderingEnabled()) {
            return;
        }

        FramebufferBinding binding = captureFramebufferBinding();
        try {
            maskTarget.bindWrite(false);
            FRAMEBUFFER_STACK.push(binding);
        } catch (RuntimeException exception) {
            restoreFramebufferBinding(binding);
            ThermalVisionRenderLog.error(
                "Failed to bind thermal entity mask target " + describeFramebufferState(),
                exception
            );
            throw exception;
        }
    }

    public static void restorePreviousTarget() {
        if (!FRAMEBUFFER_STACK.isEmpty()) {
            restoreFramebufferBinding(FRAMEBUFFER_STACK.pop());
        }
    }

    public static void reset() {
        framePrepared = false;
        failed = false;
        preparedLevel = null;
        restoreLeakedFramebufferBindings("the thermal pipeline reset");

        RENDER_STATE.capture();
        try {
            destroyTargets();
        } finally {
            RENDER_STATE.restore();
        }
    }

    public static void onResourceReload() {
        reset();
        ThermalVisionRenderLog.debug(
            "Invalidated thermal entity mask targets after client resource reload"
        );
    }

    public static void resetFailureState() {
        failed = false;
    }

    private static boolean shouldRenderMask() {
        return ClientThermalVisionState.mode().thermal() && (ClientThermalVisionState.isActive()
            || ThermalVisionPostProcessor.effectStrength() > 0.001F);
    }

    private static void ensureTargets(Minecraft minecraft, int width, int height) throws IOException {
        boolean sizeChanged = width != targetWidth || height != targetHeight;
        if (maskTarget != null
            && maskTarget.isStencilEnabled() != minecraft.getMainRenderTarget().isStencilEnabled()) {
            destroyTargets();
            sizeChanged = true;
        }

        if (maskTarget == null) {
            maskTarget = new TextureTarget(width, height, true, Minecraft.ON_OSX);
            if (minecraft.getMainRenderTarget().isStencilEnabled()) {
                maskTarget.enableStencil();
            }
            targetWidth = width;
            targetHeight = height;
            sizeChanged = true;
        } else if (sizeChanged) {
            maskTarget.resize(width, height, Minecraft.ON_OSX);
            targetWidth = width;
            targetHeight = height;
        }

        if (compositeChain != null && (sizeChanged || chainMainTarget != minecraft.getMainRenderTarget())) {
            closeCompositeChain();
        }

        if (compositeChain == null) {
            chainMainTarget = minecraft.getMainRenderTarget();
            compositeChain = new PostChain(
                minecraft.getTextureManager(),
                minecraft.getResourceManager(),
                minecraft.getMainRenderTarget(),
                COMPOSITE_PIPELINE
            );
            compositeChain.addTempTarget(SCENE_COPY_TARGET, width, height);
            compositeChain.addTempTarget(OUTPUT_TARGET, width, height);

            RenderTarget sceneCopy = compositeChain.getTempTarget(SCENE_COPY_TARGET);
            sceneCopyPass = compositeChain.addPass("thermal_vision:thermal_copy", chainMainTarget, sceneCopy);
            compositePass = compositeChain.addPass(
                "thermal_vision:thermal_entity_composite",
                sceneCopy,
                compositeChain.getTempTarget(OUTPUT_TARGET)
            );
            ThermalPostRenderState.requireUniforms(sceneCopyPass, "ProjMat", "OutSize");
            ThermalPostRenderState.requireSampler(sceneCopyPass, "DiffuseSampler");
            ThermalPostRenderState.requireUniforms(compositePass, "ProjMat", "OutSize", "VisionMode");
            ThermalPostRenderState.requireSampler(compositePass, "DiffuseSampler");
            ThermalPostRenderState.requireSampler(compositePass, "ThermalMaskSampler");
            compositePass.addAuxAsset(
                "ThermalMaskSampler",
                maskTarget::getColorTextureId,
                width,
                height
            );
            // addPass does not set the projection matrix; resize initializes it for every pass.
            compositeChain.resize(width, height);
        }
    }

    private static void closeCompositeChain() {
        if (compositeChain != null) {
            if (sceneCopyPass != null) sceneCopyPass.getEffect().clear();
            if (compositePass != null) compositePass.getEffect().clear();
            compositeChain.close();
            compositeChain = null;
            compositePass = null;
            sceneCopyPass = null;
            chainMainTarget = null;
        }
    }

    private static void destroyTargets() {
        closeCompositeChain();
        if (maskTarget != null) {
            maskTarget.destroyBuffers();
            maskTarget = null;
        }

        targetWidth = -1;
        targetHeight = -1;
    }

    private static void restoreLeakedFramebufferBindings(String context) {
        if (FRAMEBUFFER_STACK.isEmpty()) {
            return;
        }

        int leakedBindings = FRAMEBUFFER_STACK.size();
        while (!FRAMEBUFFER_STACK.isEmpty()) {
            restoreFramebufferBinding(FRAMEBUFFER_STACK.pop());
        }

        ThermalVisionRenderLog.warn(
            "Recovered {} leaked thermal framebuffer binding(s) before {}",
            leakedBindings,
            context
        );
    }

    private static String describeFramebufferState() {
        FramebufferBinding binding = captureFramebufferBinding();
        int maskFramebuffer = maskTarget == null ? -1 : maskTarget.frameBufferId;

        return "[draw=" + binding.drawFramebuffer()
            + ", read=" + binding.readFramebuffer()
            + ", mask=" + maskFramebuffer
            + ", size=" + targetWidth + "x" + targetHeight + "]";
    }

    private static FramebufferBinding captureFramebufferBinding() {
        return new FramebufferBinding(
            GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING),
            GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING),
            BlendMode.lastApplied
        );
    }

    private static void restoreFramebufferBinding(FramebufferBinding binding) {
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, binding.drawFramebuffer());
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, binding.readFramebuffer());
        // The opaque mask shader must not change how the following vanilla shader applies blending.
        BlendMode.lastApplied = binding.shaderBlendMode();
    }

    private record FramebufferBinding(int drawFramebuffer, int readFramebuffer, BlendMode shaderBlendMode) {
    }
}
