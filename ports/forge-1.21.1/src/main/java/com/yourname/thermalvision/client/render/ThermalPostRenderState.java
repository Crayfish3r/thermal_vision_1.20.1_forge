package com.yourname.thermalvision.client.render;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.shaders.BlendMode;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import net.minecraft.client.renderer.PostPass;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/** Reused on the render thread; no per-frame buffers or changes to Minecraft's shader selection. */
public final class ThermalPostRenderState {
    private final IntBuffer viewport = BufferUtils.createIntBuffer(4);
    private final ByteBuffer colorMask = BufferUtils.createByteBuffer(4);
    private final FloatBuffer clearColor = BufferUtils.createFloatBuffer(4);
    private int drawFramebuffer;
    private int readFramebuffer;
    private int program;
    private int activeTexture;
    private int activeBinding;
    private int texture0;
    private int texture1;
    private int depthFunc;
    private int blendSrcRgb;
    private int blendDstRgb;
    private int blendSrcAlpha;
    private int blendDstAlpha;
    private int blendEquationRgb;
    private int blendEquationAlpha;
    private boolean blend;
    private boolean depthTest;
    private boolean depthMask;
    private boolean cull;
    private boolean scissor;
    private double clearDepth;

    public void capture() {
        RenderSystem.assertOnRenderThread();
        drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, colorMask);
        GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE, clearColor);
        clearDepth = GL11.glGetDouble(GL11.GL_DEPTH_CLEAR_VALUE);
        program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        activeBinding = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        texture0 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        RenderSystem.activeTexture(GL13.GL_TEXTURE1);
        texture1 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        RenderSystem.activeTexture(activeTexture);
        blend = GL11.glIsEnabled(GL11.GL_BLEND);
        depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        blendSrcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB);
        blendDstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        blendSrcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA);
        blendDstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        blendEquationRgb = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB);
        blendEquationAlpha = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA);
    }

    public static void prepareFullscreenPass() {
        // BlendMode caches its last value independently of RenderSystem: establish replacement explicitly.
        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.disableCull();
        RenderSystem.disableScissor();
    }

    public void restore() {
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
        RenderSystem.viewport(viewport.get(0), viewport.get(1), viewport.get(2), viewport.get(3));
        RenderSystem.depthFunc(depthFunc);
        RenderSystem.depthMask(depthMask);
        if (depthTest) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
        RenderSystem.colorMask(colorMask.get(0) != 0, colorMask.get(1) != 0,
            colorMask.get(2) != 0, colorMask.get(3) != 0);
        RenderSystem.blendFuncSeparate(blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha);
        // Rebuild the shader blend cache from the captured GL state; direct cache access is
        // no longer available in 1.21.1.
        if (blend) {
            new BlendMode(blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha,
                blendEquationRgb).apply();
        } else {
            new BlendMode().apply();
        }
        GL20.glBlendEquationSeparate(blendEquationRgb, blendEquationAlpha);
        if (cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
        // The scissor rectangle was never changed.
        if (scissor) GlStateManager._enableScissorTest(); else RenderSystem.disableScissor();
        RenderSystem.clearColor(clearColor.get(0), clearColor.get(1), clearColor.get(2), clearColor.get(3));
        RenderSystem.clearDepth(clearDepth);
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        RenderSystem.bindTexture(texture0);
        RenderSystem.activeTexture(GL13.GL_TEXTURE1);
        RenderSystem.bindTexture(texture1);
        RenderSystem.activeTexture(activeTexture);
        RenderSystem.bindTexture(activeBinding);
        GlStateManager._glUseProgram(program);
    }

    public static void requireTarget(RenderTarget target, int width, int height) {
        if (target == null || width <= 0 || height <= 0 || target.frameBufferId < 0
            || target.getColorTextureId() < 0 || target.width != width || target.height != height
            || target.viewWidth != width || target.viewHeight != height) {
            throw new IllegalStateException("Invalid thermal RenderTarget; expected " + width + "x" + height);
        }
    }

    public static void requireUniforms(PostPass pass, String... names) {
        for (String name : names) {
            Uniform uniform = pass.getEffect().getUniform(name);
            if (uniform == null || uniform.getLocation() < 0) {
                throw new IllegalStateException(pass.getName() + ": missing required uniform " + name);
            }
        }
    }

    public static void requireSampler(PostPass pass, String name) {
        if (GL20.glGetUniformLocation(pass.getEffect().getId(), name) < 0) {
            throw new IllegalStateException(pass.getName() + ": missing required sampler " + name);
        }
    }

    public static void requireNoGlError(String stage) {
        int error = GL11.glGetError();
        if (error != GL11.GL_NO_ERROR) {
            throw new IllegalStateException(stage + ": OpenGL error 0x" + Integer.toHexString(error));
        }
    }

    /** Commit only after all shader passes succeeded. Never clear or copy depth into the main target. */
    public static void copyColor(RenderTarget source, RenderTarget destination) {
        requireTarget(source, destination.width, destination.height);
        requireTarget(destination, source.width, source.height);
        if (source.frameBufferId == destination.frameBufferId) {
            throw new IllegalStateException("Thermal color copy cannot read and write the same framebuffer");
        }
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, source.frameBufferId);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, destination.frameBufferId);
        if (GlStateManager.glCheckFramebufferStatus(GL30.GL_READ_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE
            || GlStateManager.glCheckFramebufferStatus(GL30.GL_DRAW_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) {
            throw new IllegalStateException("Incomplete framebuffer during thermal color copy");
        }
        // A framebuffer blit is a color replacement, independent of alpha and shader/blend state.
        GlStateManager._glBlitFrameBuffer(0, 0, source.width, source.height,
            0, 0, destination.width, destination.height, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
    }
}
