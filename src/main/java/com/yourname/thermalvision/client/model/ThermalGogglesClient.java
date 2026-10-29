package com.yourname.thermalvision.client.model;

import com.yourname.thermalvision.ThermalVision;
import com.yourname.thermalvision.compat.ThermalEquipment;
import java.util.function.Consumer;
import com.yourname.thermalvision.common.VisionMode;
import net.minecraft.client.model.geom.ModelLayerLocation;
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
    private static final ThermalGogglesModel[] MODELS = new ThermalGogglesModel[3];
    private static final ModelLayerLocation[] LAYERS = {ThermalGogglesModel.LAYER,
        new ModelLayerLocation(new ResourceLocation(ThermalVision.MOD_ID, "thermal_goggles_white_hot"), "main"),
        new ModelLayerLocation(new ResourceLocation(ThermalVision.MOD_ID, "thermal_goggles_night_vision"), "main")};
    private static final ResourceLocation[] ON_TEXTURES = {ON,
        new ResourceLocation(ThermalVision.MOD_ID, "textures/models/goggles/thermal_goggles_white_hot_on.png"),
        new ResourceLocation(ThermalVision.MOD_ID, "textures/models/goggles/thermal_goggles_night_vision_on.png")};
    private static final ResourceLocation[] OFF_TEXTURES = {OFF,
        new ResourceLocation(ThermalVision.MOD_ID, "textures/models/goggles/thermal_goggles_white_hot_off.png"),
        new ResourceLocation(ThermalVision.MOD_ID, "textures/models/goggles/thermal_goggles_night_vision_off.png")};
    private ThermalGogglesClient() {}
    public static ThermalGogglesModel model() { return model(ItemStack.EMPTY); }
    public static ThermalGogglesModel model(ItemStack stack) {
        int index = VisionMode.of(stack).ordinal();
        if (MODELS[index] == null) MODELS[index] = new ThermalGogglesModel(Minecraft.getInstance().getEntityModels().bakeLayer(LAYERS[index]));
        return MODELS[index];
    }
    public static void invalidateModel() { java.util.Arrays.fill(MODELS, null); }
    public static ResourceLocation texture(Entity wearer, ItemStack stack) {
        return (ClientGogglesVisuals.isActive(wearer) ? ON_TEXTURES : OFF_TEXTURES)[VisionMode.of(stack).ordinal()];
    }
    public static ResourceLocation texture(Entity wearer) { return texture(wearer, ItemStack.EMPTY); }
    public static ResourceLocation offTexture() { return OFF; }
    public static ResourceLocation offTexture(ItemStack stack) { return OFF_TEXTURES[VisionMode.of(stack).ordinal()]; }
    public static String texturePath(Entity wearer, ItemStack stack) { return texture(wearer, stack).toString(); }

    public static void initialize(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity wearer, ItemStack stack,
                EquipmentSlot slot, HumanoidModel<?> original) {
                ThermalGogglesModel goggles = model(stack);
                goggles.applyPose(original, ClientGogglesVisuals.angle(wearer),
                    !ThermalEquipment.usesCurios() && slot == EquipmentSlot.HEAD && !wearer.isSpectator());
                return goggles;
            }
        });
    }

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ThermalGogglesModel.LAYER, ThermalGogglesModel::createBodyLayer);
        event.registerLayerDefinition(LAYERS[1], WhiteHotGeometry::createBodyLayer);
        event.registerLayerDefinition(LAYERS[2], NightVisionGeometry::createBodyLayer);
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        if (ThermalEquipment.usesCurios()) event.enqueueWork(com.yourname.thermalvision.compat.client.CuriosGogglesRenderer::register);
    }
}
