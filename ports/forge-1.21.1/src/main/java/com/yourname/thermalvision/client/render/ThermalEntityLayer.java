package com.yourname.thermalvision.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.yourname.thermalvision.client.ThermalVisionPostProcessor;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

public final class ThermalEntityLayer<
    T extends LivingEntity,
    M extends EntityModel<T>
> extends RenderLayer<T, M> {
    public ThermalEntityLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(
        PoseStack poseStack,
        MultiBufferSource bufferSource,
        int packedLight,
        T entity,
        float limbSwing,
        float limbSwingAmount,
        float partialTick,
        float ageInTicks,
        float netHeadYaw,
        float headPitch
    ) {
        if (!ThermalVisionRenderPipeline.isMaskRenderingEnabled()
            || !ThermalSignatureResolver.shouldRender(entity)) {
            return;
        }

        float heat = ThermalSignatureResolver.getHeat(entity, partialTick);
        float strength = ThermalVisionPostProcessor.effectStrength();
        if (heat <= 0.001F || strength <= 0.001F) {
            return;
        }

        ResourceLocation texture = getTextureLocation(entity);
        VertexConsumer consumer = bufferSource.getBuffer(ThermalRenderTypes.entityMask(texture));

        int color = ((int) (strength * 255.0F) << 24)
            | ((int) (heat * 255.0F) << 16);
        getParentModel().renderToBuffer(
            poseStack, consumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, color
        );
    }
}
