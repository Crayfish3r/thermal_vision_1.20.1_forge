package com.yourname.thermalvision.client.model;

import com.yourname.thermalvision.ThermalVision;
import com.yourname.thermalvision.compat.ThermalEquipment;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = ThermalVision.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ThermalGogglesClient {
    private static final String ON_PATH = "thermal_vision:textures/models/goggles/thermal_goggles_on.png";
    private static final String OFF_PATH = "thermal_vision:textures/models/goggles/thermal_goggles_off.png";
    private static final ResourceLocation ON = new ResourceLocation(ON_PATH);
    private static final ResourceLocation OFF = new ResourceLocation(OFF_PATH);
    private static ThermalGogglesModel model;

    private ThermalGogglesClient() {}

    public static ThermalGogglesModel model() {
        if (model == null) model = new ThermalGogglesModel(Minecraft.getInstance().getEntityModels().bakeLayer(ThermalGogglesModel.LAYER));
        return model;
    }

    public static void invalidateModel() { model = null; }

    public static ResourceLocation texture(Entity wearer) { return ClientGogglesVisuals.isActive(wearer) ? ON : OFF; }
    public static ResourceLocation offTexture() { return OFF; }
    public static String texturePath(Entity wearer) { return ClientGogglesVisuals.isActive(wearer) ? ON_PATH : OFF_PATH; }

    public static void initialize(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity wearer, ItemStack stack,
                EquipmentSlot slot, HumanoidModel<?> original) {
                ThermalGogglesModel goggles = model();
                goggles.applyPose(original, ClientGogglesVisuals.angle(wearer),
                    !ThermalEquipment.usesCurios() && slot == EquipmentSlot.HEAD && !wearer.isSpectator());
                return goggles;
            }
        });
    }

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ThermalGogglesModel.LAYER, ThermalGogglesModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        if (ThermalEquipment.usesCurios()) event.enqueueWork(com.yourname.thermalvision.compat.client.CuriosGogglesRenderer::register);
    }
}
