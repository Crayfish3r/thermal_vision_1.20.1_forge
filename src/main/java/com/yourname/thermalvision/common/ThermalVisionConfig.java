package com.yourname.thermalvision.common;

public final class ThermalVisionConfig {
    public static final int CHUNKS_IN_RANGE = 6;
    public static final int BLOCKS_PER_CHUNK = 16;
    public static final int DEFAULT_RANGE_BLOCKS = CHUNKS_IN_RANGE * BLOCKS_PER_CHUNK;

    public static final int TOTAL_USE_TICKS = 20 * 60;
    public static final int COOLDOWN_TICKS = 0;
    public static final int DENIED_COOLDOWN_TICKS = 0;

    public static final boolean IGNORE_INVISIBLE_ENTITIES = true;
    public static final boolean IGNORE_SPECTATORS = true;

    public static final float OUTER_AURA_WIDTH_SCALE = 5.6F;
    public static final float OUTER_AURA_HEIGHT_SCALE = 2.55F;
    public static final float BODY_GLOW_WIDTH_SCALE = 3.15F;
    public static final float BODY_GLOW_HEIGHT_SCALE = 1.62F;
    public static final float HOTSPOT_WIDTH_SCALE = 1.82F;
    public static final float HOTSPOT_HEIGHT_SCALE = 0.88F;
    public static final float HEAD_GLOW_WIDTH_SCALE = 1.40F;
    public static final float HEAD_GLOW_HEIGHT_SCALE = 0.66F;
    public static final float GROUND_GLOW_SCALE = 4.15F;
    public static final int LIGHT_SOURCE_RANGE_BLOCKS = 32;
    public static final int LIGHT_SOURCE_VERTICAL_RANGE_BLOCKS = 18;
    public static final int LIGHT_SOURCE_MIN_EMISSION = 10;
    public static final int LIGHT_SOURCE_MAX_TARGETS = 96;
    public static final int LIGHT_SOURCE_SCAN_INTERVAL_TICKS = 8;
    public static final float LIGHT_SOURCE_GLOW_SCALE = 2.55F;
    public static final float LIGHT_SOURCE_CORE_SCALE = 1.05F;
    public static final float LIGHT_SOURCE_GROUND_GLOW_SCALE = 2.75F;

    public static final float OUTER_AURA_ALPHA = 0.40F;
    public static final float BODY_GLOW_ALPHA = 0.40F;
    public static final float HOTSPOT_ALPHA = 0.40F;
    public static final float HEAD_GLOW_ALPHA = 0.40F;
    public static final float GROUND_GLOW_ALPHA = 0.40F;

    public static final float BODY_OUTLINE_WIDTH_SCALE = 2.30F;
    public static final float BODY_OUTLINE_HEIGHT_SCALE = 1.26F;
    public static final float HEAD_OUTLINE_WIDTH_SCALE = 1.38F;
    public static final float HEAD_OUTLINE_HEIGHT_SCALE = 0.48F;
    public static final float BODY_OUTLINE_ALPHA = 1.0F;
    public static final float HEAD_OUTLINE_ALPHA = 0.98F;

    public static final float TOGGLE_ON_SOUND_VOLUME = 0.14875F;
    public static final float TOGGLE_ON_SOUND_PITCH = 1.0F;
    public static final float LOOP_SOUND_VOLUME = 0.0595F;
    public static final float LOOP_SOUND_PITCH = 1.0F;
    public static final float BREAK_SOUND_VOLUME = 0.95F;
    public static final float BREAK_SOUND_PITCH = 1.0F;

    private ThermalVisionConfig() {
    }
}
