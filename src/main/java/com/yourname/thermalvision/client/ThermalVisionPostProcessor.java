package com.yourname.thermalvision.client;

import com.yourname.thermalvision.ThermalVision;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;

public final class ThermalVisionPostProcessor {
    private static final ResourceLocation THERMAL_POST_EFFECT =
        new ResourceLocation(ThermalVision.MOD_ID, "shaders/post/thermal_vision.json");

    private static final String FADE_UNIFORM = "Fade";
    private static final float FADE_IN_STEP = 1.0F / 23.0F;
    private static final float FADE_OUT_STEP = 1.0F / 18.0F;

    private static boolean loaded;
    private static boolean failed;
    private static boolean desiredActive;
    private static float fade;
    private static PostChain postChain;
    private static Object cachedFadeUniform;
    private static Method cachedFloatSetter;
    private static int lastWidth = -1;
    private static int lastHeight = -1;

    private ThermalVisionPostProcessor() {
    }

    public static void setDesiredActive(boolean shouldBeActive) {
        desiredActive = shouldBeActive;
    }

    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        boolean canRender = minecraft.level != null && minecraft.player != null;

        if (!canRender) {
            desiredActive = false;
            fade = 0.0F;
            disable(minecraft);
            return;
        }

        updateFadeValue();

        if (desiredActive || fade > 0.001F) {
            enable(minecraft);
            applyFadeUniform(minecraft);
            return;
        }

        disable(minecraft);
    }

    public static void render(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type() || !loaded || postChain == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        try {
            postChain.process(event.getPartialTick());
            minecraft.getMainRenderTarget().bindWrite(true);
        } catch (RuntimeException exception) {
            failed = true;
            fade = 0.0F;
            disable(minecraft);
        }
    }

    public static void resetFailureState() {
        failed = false;
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
            postChain.close();
            postChain = null;
        }

        loaded = false;
        lastWidth = -1;
        lastHeight = -1;
        clearCachedPostEffectReflection();
    }

    private static boolean ensurePostChain(Minecraft minecraft) {
        try {
            if (postChain == null) {
                postChain = new PostChain(
                    minecraft.getTextureManager(),
                    minecraft.getResourceManager(),
                    minecraft.getMainRenderTarget(),
                    THERMAL_POST_EFFECT
                );
                clearCachedPostEffectReflection();
            }

            int width = minecraft.getWindow().getWidth();
            int height = minecraft.getWindow().getHeight();
            if (width != lastWidth || height != lastHeight) {
                postChain.resize(width, height);
                lastWidth = width;
                lastHeight = height;
            }

            return true;
        } catch (IOException | RuntimeException exception) {
            failed = true;
            fade = 0.0F;
            disable(minecraft);
            return false;
        }
    }

    private static void applyFadeUniform(Minecraft minecraft) {
        if (!loaded) {
            return;
        }

        if (cachedFadeUniform == null) {
            if (postChain == null) {
                return;
            }

            cachedFadeUniform = findUniform(postChain, FADE_UNIFORM);
            cachedFloatSetter = cachedFadeUniform == null ? null : findFloatSetter(cachedFadeUniform);
        }

        if (cachedFadeUniform == null || cachedFloatSetter == null) {
            return;
        }

        try {
            cachedFloatSetter.invoke(cachedFadeUniform, fade);
        } catch (ReflectiveOperationException exception) {
            clearCachedPostEffectReflection();
        }
    }

    private static void clearCachedPostEffectReflection() {
        cachedFadeUniform = null;
        cachedFloatSetter = null;
    }

    private static Object findUniform(PostChain postChain, String uniformName) {
        for (Object pass : findPostPasses(postChain)) {
            Object effect = findEffectInstance(pass);
            if (effect == null) {
                continue;
            }

            Object uniform = getUniform(effect, uniformName);
            if (uniform != null) {
                return uniform;
            }
        }

        return null;
    }

    private static List<?> findPostPasses(PostChain postChain) {
        for (Field field : postChain.getClass().getDeclaredFields()) {
            if (!List.class.isAssignableFrom(field.getType())) {
                continue;
            }

            try {
                field.setAccessible(true);
                Object value = field.get(postChain);
                if (!(value instanceof List<?> list) || list.isEmpty()) {
                    continue;
                }

                Object first = list.get(0);
                if (first != null && first.getClass().getName().contains("PostPass")) {
                    return list;
                }
            } catch (IllegalAccessException ignored) {
            }
        }

        return List.of();
    }

    private static Object findEffectInstance(Object pass) {
        for (Field field : pass.getClass().getDeclaredFields()) {
            String typeName = field.getType().getName();
            if (!typeName.endsWith("EffectInstance") && !typeName.endsWith("ShaderInstance")) {
                continue;
            }

            try {
                field.setAccessible(true);
                return field.get(pass);
            } catch (IllegalAccessException ignored) {
            }
        }

        return null;
    }

    private static Object getUniform(Object effect, String uniformName) {
        for (Method method : effect.getClass().getMethods()) {
            if (method.getParameterCount() != 1 || method.getParameterTypes()[0] != String.class) {
                continue;
            }

            if (!method.getReturnType().getName().endsWith("Uniform")) {
                continue;
            }

            try {
                Object uniform = method.invoke(effect, uniformName);
                if (uniform != null) {
                    return uniform;
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }

        return null;
    }

    private static Method findFloatSetter(Object uniform) {
        for (Method method : uniform.getClass().getMethods()) {
            if (method.getParameterCount() != 1 || method.getParameterTypes()[0] != float.class) {
                continue;
            }

            if (method.getReturnType() != void.class) {
                continue;
            }

            method.setAccessible(true);
            return method;
        }

        return null;
    }
}
