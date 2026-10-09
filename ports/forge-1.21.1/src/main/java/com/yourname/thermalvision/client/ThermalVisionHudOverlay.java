package com.yourname.thermalvision.client;

import com.yourname.thermalvision.common.ThermalVisionConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class ThermalVisionHudOverlay {
    private static final int SCANLINE_ARGB = 0x12000000;
    private static final int TEXT_ARGB = 0xFFB7E6FF;
    private static final int HEADER_ARGB = 0x8A020812;
    private static final int VIGNETTE_ARGB = 0xEA000000;
    private static final int VIGNETTE_CURVE_STEPS = 72;
    private static final int STATUS_GAP = 16;
    private static final Component THERMAL_LABEL =
        Component.translatable("hud.thermal_vision.label");
    private static final Component RANGE_LABEL =
        Component.translatable("hud.thermal_vision.range", ThermalVisionConfig.CHUNKS_IN_RANGE);

    private static int cachedSeconds = -1;
    private static Component cachedUseTimeLabel =
        Component.translatable("hud.thermal_vision.use_time", 0);

    private ThermalVisionHudOverlay() {
    }

    public static void render(GuiGraphics graphics) {
        if (!ClientThermalVisionState.isActive() || Minecraft.getInstance().options.hideGui) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();

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
            int alpha = Math.min(
                255,
                (int) (((VIGNETTE_ARGB >>> 24) & 0xFF) * (1.0F - progress) * (1.0F - progress))
            );
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
            cachedUseTimeLabel = Component.translatable("hud.thermal_vision.use_time", seconds);
        }

        graphics.fill(0, 0, width, 18, HEADER_ARGB);

        int x = 8;
        graphics.drawString(minecraft.font, THERMAL_LABEL, x, 6, TEXT_ARGB, false);
        x += minecraft.font.width(THERMAL_LABEL) + STATUS_GAP;

        graphics.drawString(minecraft.font, cachedUseTimeLabel, x, 6, TEXT_ARGB, false);
        x += minecraft.font.width(cachedUseTimeLabel) + STATUS_GAP;

        graphics.drawString(minecraft.font, RANGE_LABEL, x, 6, TEXT_ARGB, false);
    }
}
