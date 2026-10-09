package com.yourname.thermalvision.client.render;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

final class ThermalVisionRenderLog {
    private static final Logger LOGGER = LogUtils.getLogger();

    private ThermalVisionRenderLog() {
    }

    static void error(String message, Throwable throwable) {
        LOGGER.error(message, throwable);
    }

    static void warn(String message, Object... arguments) {
        LOGGER.warn(message, arguments);
    }

    static void debug(String message) {
        LOGGER.debug(message);
    }
}
