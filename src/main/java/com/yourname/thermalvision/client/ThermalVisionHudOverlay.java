package com.yourname.thermalvision.client;

import com.yourname.thermalvision.common.ThermalVisionConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;

public final class ThermalVisionHudOverlay {
    private static final int COLD_FALLBACK_DARKEN_ARGB = 0x28000000;
    private static final int COLD_FALLBACK_BLUE_ARGB = 0x2410315E;
    private static final int SCANLINE_ARGB = 0x235A9DDE;
    private static final int TEXT_ARGB = 0xFFB7E6FF;
    private static final int HEADER_ARGB = 0x8A020812;
    private static final int VIGNETTE_ARGB = 0xEA000000;
    private static final int VIGNETTE_CURVE_STEPS = 72;
    private static final Component THERMAL_LABEL = Component.literal("THERMAL");
    private static final Component RANGE_LABEL = Component.literal(ThermalVisionConfig.CHUNKS_IN_RANGE + " chunks");

    private static int cachedSeconds = -1;
    private static Component cachedUseTimeLabel = Component.literal("USE 0s");

    private ThermalVisionHudOverlay() {
    }

    public static void render(RenderGuiOverlayEvent.Post event) {
        if (!ClientThermalVisionState.isActive()) {
            return;
        }

        if (event.getOverlay() != VanillaGuiOverlay.HOTBAR.type()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        GuiGraphics graphics = event.getGuiGraphics();

        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();

        graphics.fill(0, 0, width, height, COLD_FALLBACK_DARKEN_ARGB);
        graphics.fill(0, 0, width, height, COLD_FALLBACK_BLUE_ARGB);

        for (int y = 0; y < height; y += 4) {
            graphics.fill(0, y, width, y + 1, SCANLINE_ARGB);
        }

        drawRoundedVignette(graphics, width, height);
        drawStatusText(minecraft, graphics, width);
    }

    private static void drawRoundedVignette(GuiGraphics graphics, int width, int height) {
        int maxHorizontalInset = Math.max(96, width / 5);
        int maxVerticalInset = Math.max(72, height / 4);

        for (int step = 0; step < VIGNETTE_CURVE_STEPS; step++) {
            float progress = step / (float) (VIGNETTE_CURVE_STEPS - 1);
            float curve = (float) Math.sqrt(progress);
            int alpha = Math.min(255, (int) (((VIGNETTE_ARGB >>> 24) & 0xFF) * (1.0F - progress) * (1.0F - progress)));
            int color = alpha << 24;

            int xInset = Math.round(maxHorizontalInset * curve);
            int yInset = Math.round(maxVerticalInset * curve);

            graphics.fill(xInset, step, width - xInset, step + 1, color);
            graphics.fill(xInset, height - step - 1, width - xInset, height - step, color);
            graphics.fill(step, yInset, step + 1, height - yInset, color);
            graphics.fill(width - step - 1, yInset, width - step, height - yInset, color);
        }
    }

    private static void drawStatusText(Minecraft minecraft, GuiGraphics graphics, int width) {
        int seconds = Math.max(0, (ClientThermalVisionState.remainingTicks() + 19) / 20);
        if (seconds != cachedSeconds) {
            cachedSeconds = seconds;
            cachedUseTimeLabel = Component.literal("USE " + seconds + "s");
        }

        graphics.fill(0, 0, width, 18, HEADER_ARGB);
        graphics.drawString(minecraft.font, THERMAL_LABEL, 8, 6, TEXT_ARGB, false);
        graphics.drawString(minecraft.font, cachedUseTimeLabel, 72, 6, TEXT_ARGB, false);
        graphics.drawString(minecraft.font, RANGE_LABEL, 138, 6, TEXT_ARGB, false);
    }
}
