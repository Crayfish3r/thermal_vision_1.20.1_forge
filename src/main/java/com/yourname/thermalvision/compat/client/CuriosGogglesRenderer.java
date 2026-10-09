package com.yourname.thermalvision.compat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.yourname.thermalvision.client.model.ClientGogglesVisuals;
import com.yourname.thermalvision.client.model.ThermalGogglesClient;
import com.yourname.thermalvision.client.model.ThermalGogglesMotion;
import com.yourname.thermalvision.registry.ModItems;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;
import top.theillusivec4.curios.api.client.ICurioRenderer;

public final class CuriosGogglesRenderer implements ICurioRenderer {
    public static void register() {
        CuriosRendererRegistry.register(ModItems.THERMAL_GOGGLES.get(), CuriosGogglesRenderer::new);
    }

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(
        ItemStack stack, SlotContext context, PoseStack poses, RenderLayerParent<T, M> parent,
        MultiBufferSource buffers, int light, float limbSwing, float limbSwingAmount,
        float partialTicks, float ageInTicks, float netHeadYaw, float headPitch
    ) {
        LivingEntity wearer = context.entity();
        if (!"head".equals(context.identifier()) || !context.visible() || wearer.isSpectator()
            || wearer.isInvisible() || context.index() != ClientGogglesVisuals.renderIndex(wearer)
            || !(parent.getModel() instanceof HumanoidModel<?> humanoid)) return;
        var model = ThermalGogglesClient.model();
        model.applyPose(humanoid, context.cosmetic() ? ThermalGogglesMotion.RAISED : ClientGogglesVisuals.angle(wearer), true);
        var texture = context.cosmetic() ? ThermalGogglesClient.offTexture() : ThermalGogglesClient.texture(wearer);
        poses.pushPose();
        try {
            model.renderToBuffer(poses, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)),
                light, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1);
        } finally {
            poses.popPose();
        }
    }
}
