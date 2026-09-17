package com.yourname.thermalvision.client.render;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.util.function.Function;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public final class ThermalRenderTypes extends RenderType {
    private static final ShaderStateShard THERMAL_ENTITY_MASK_SHADER =
        new ShaderStateShard(ThermalVisionShaders::getEntityMaskShader);

    private static final OutputStateShard THERMAL_MASK_TARGET =
        new OutputStateShard(
            "thermal_vision_mask_target",
            ThermalVisionRenderPipeline::bindMaskTarget,
            ThermalVisionRenderPipeline::restorePreviousTarget
        );

    private static final Function<ResourceLocation, RenderType> ENTITY_MASK =
        Util.memoize(texture -> create(
            "thermal_vision_entity_mask",
            DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS,
            256,
            true,
            false,
            CompositeState.builder()
                .setShaderState(THERMAL_ENTITY_MASK_SHADER)
                .setTextureState(new TextureStateShard(texture, false, false))
                .setTransparencyState(NO_TRANSPARENCY)
                .setDepthTestState(LEQUAL_DEPTH_TEST)
                .setCullState(NO_CULL)
                .setLightmapState(NO_LIGHTMAP)
                .setOverlayState(NO_OVERLAY)
                .setOutputState(THERMAL_MASK_TARGET)
                .setWriteMaskState(COLOR_DEPTH_WRITE)
                .createCompositeState(false)
        ));

    private ThermalRenderTypes(
        String name,
        VertexFormat format,
        VertexFormat.Mode mode,
        int bufferSize,
        boolean affectsCrumbling,
        boolean sortOnUpload,
        Runnable setupState,
        Runnable clearState
    ) {
        super(
            name,
            format,
            mode,
            bufferSize,
            affectsCrumbling,
            sortOnUpload,
            setupState,
            clearState
        );
    }

    public static RenderType entityMask(ResourceLocation texture) {
        return ENTITY_MASK.apply(texture);
    }
}
