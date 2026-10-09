package com.yourname.thermalvision.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.yourname.thermalvision.ThermalVision;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/** Geometry exported directly from art/thermal_goggles/thermal_goggles.bbmodel.
 * Empty humanoid parts adapt the same head geometry to Forge armor and Curios.
 * Regenerate with docs/integration/goggles/export_bbmodel.py.
 */
public final class ThermalGogglesModel extends HumanoidModel<LivingEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
        new ResourceLocation(ThermalVision.MOD_ID, "thermal_goggles"), "main");
    private final ModelPart gogglesHinge;
    private boolean renderEnabled;

    public ThermalGogglesModel(ModelPart root) {
        super(root);
        gogglesHinge = head.getChild("goggles_hinge");
    }

    public void applyPose(HumanoidModel<?> wearer, float hingeAngle, boolean visible) {
        head.copyFrom(wearer.head);
        head.visible = true;
        young = wearer.young;
        crouching = wearer.crouching;
        riding = wearer.riding;
        gogglesHinge.xRot = hingeAngle;
        renderEnabled = visible;
    }

    public float hingeAngle() { return gogglesHinge.xRot; }

    @Override
    public void renderToBuffer(PoseStack poses, VertexConsumer vertices, int light, int overlay,
                               float red, float green, float blue, float alpha) {
        if (renderEnabled) super.renderToBuffer(poses, vertices, light, overlay, red, green, blue, alpha);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
            , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition fixed_mount = head.addOrReplaceChild("fixed_mount", CubeListBuilder.create()
            , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition strap = fixed_mount.addOrReplaceChild("strap", CubeListBuilder.create()
            // strap_left
            .texOffs(3, 3).addBox(-5.65F, -6.2F, -5.65F, 0.5F, 1.5F, 11.3F, new CubeDeformation(0.0F))
            // strap_right
            .texOffs(31, 3).addBox(5.15F, -6.2F, -5.65F, 0.5F, 1.5F, 11.3F, new CubeDeformation(0.0F))
            // strap_rear
            .texOffs(65, 36).addBox(-5.15F, -6.2F, 5.15F, 10.3F, 1.5F, 0.5F, new CubeDeformation(0.0F))
            // brow_band
            .texOffs(3, 43).addBox(-5.15F, -7.05F, -5.9F, 10.3F, 0.95F, 0.75F, new CubeDeformation(0.0F))
            // left_band_keeper_0
            .texOffs(17, 36).addBox(-5.9F, -6.35F, -4.88F, 0.25F, 1.8F, 0.56F, new CubeDeformation(0.0F))
            // left_band_keeper_1
            .texOffs(53, 36).addBox(-5.9F, -6.35F, -2.08F, 0.25F, 1.8F, 0.56F, new CubeDeformation(0.0F))
            // left_band_keeper_2
            .texOffs(23, 36).addBox(-5.9F, -6.35F, 1.22F, 0.25F, 1.8F, 0.56F, new CubeDeformation(0.0F))
            // left_band_keeper_3
            .texOffs(29, 36).addBox(-5.9F, -6.35F, 4.12F, 0.25F, 1.8F, 0.56F, new CubeDeformation(0.0F))
            // right_band_keeper_0
            .texOffs(35, 36).addBox(5.65F, -6.35F, -4.88F, 0.25F, 1.8F, 0.56F, new CubeDeformation(0.0F))
            // right_band_keeper_1
            .texOffs(59, 36).addBox(5.65F, -6.35F, -2.08F, 0.25F, 1.8F, 0.56F, new CubeDeformation(0.0F))
            // right_band_keeper_2
            .texOffs(41, 36).addBox(5.65F, -6.35F, 1.22F, 0.25F, 1.8F, 0.56F, new CubeDeformation(0.0F))
            // right_band_keeper_3
            .texOffs(47, 36).addBox(5.65F, -6.35F, 4.12F, 0.25F, 1.8F, 0.56F, new CubeDeformation(0.0F))
            // rear_buckle_frame
            .texOffs(80, 29).addBox(-0.85F, -6.5F, 5.65F, 1.7F, 2.1F, 0.4F, new CubeDeformation(0.0F))
            // rear_buckle_inset
            .texOffs(50, 43).addBox(-0.48F, -6.1F, 6.05F, 0.96F, 1.3F, 0.1F, new CubeDeformation(0.0F))
            // rear_buckle_tab
            .texOffs(41, 55).addBox(-0.26F, -5.7F, 6.15F, 0.52F, 0.5F, 0.09F, new CubeDeformation(0.0F))
            , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition mount_hardware = fixed_mount.addOrReplaceChild("mount_hardware", CubeListBuilder.create()
            // left_mount_plate
            .texOffs(3, 20).addBox(-6.0F, -7.05F, -3.6F, 0.35F, 2.45F, 1.8F, new CubeDeformation(0.0F))
            // left_mount_bolt_0
            .texOffs(69, 49).addBox(-6.13F, -6.7F, -2.95F, 0.13F, 0.4F, 0.4F, new CubeDeformation(0.0F))
            // left_mount_bolt_1
            .texOffs(75, 49).addBox(-6.13F, -5.3F, -2.95F, 0.13F, 0.4F, 0.4F, new CubeDeformation(0.0F))
            // left_brow_anchor
            .texOffs(64, 29).addBox(-5.82F, -7.1F, -6.02F, 0.67F, 1.65F, 0.87F, new CubeDeformation(0.0F))
            // right_mount_plate
            .texOffs(12, 20).addBox(5.65F, -7.05F, -3.6F, 0.35F, 2.45F, 1.8F, new CubeDeformation(0.0F))
            // right_mount_bolt_0
            .texOffs(81, 49).addBox(6.0F, -6.7F, -2.95F, 0.13F, 0.4F, 0.4F, new CubeDeformation(0.0F))
            // right_mount_bolt_1
            .texOffs(87, 49).addBox(6.0F, -5.3F, -2.95F, 0.13F, 0.4F, 0.4F, new CubeDeformation(0.0F))
            // right_brow_anchor
            .texOffs(72, 29).addBox(5.15F, -7.1F, -6.02F, 0.67F, 1.65F, 0.87F, new CubeDeformation(0.0F))
            // brow_rail
            .texOffs(93, 49).addBox(-3.9F, -7.35F, -6.0F, 7.8F, 0.5F, 0.25F, new CubeDeformation(0.0F))
            // fixed_hinge_base
            .texOffs(52, 29).addBox(-1.22F, -7.95F, -6.15F, 2.44F, 1.45F, 1.1F, new CubeDeformation(0.0F))
            // fixed_hinge_top_cap
            .texOffs(3, 49).addBox(-1.0F, -8.15F, -6.12F, 2.0F, 0.2F, 0.92F, new CubeDeformation(0.0F))
            // left_hinge_axis_cap
            .texOffs(38, 43).addBox(-1.48F, -7.62F, -5.97F, 0.26F, 0.74F, 0.74F, new CubeDeformation(0.0F))
            // right_hinge_axis_cap
            .texOffs(44, 43).addBox(1.22F, -7.62F, -5.97F, 0.26F, 0.74F, 0.74F, new CubeDeformation(0.0F))
            // fixed_hinge_front_plate
            .texOffs(61, 49).addBox(-0.75F, -7.65F, -6.3F, 1.5F, 0.7F, 0.15F, new CubeDeformation(0.0F))
            , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition goggles_hinge = head.addOrReplaceChild("goggles_hinge", CubeListBuilder.create()
            , PartPose.offset(0.0F, -7.25F, -5.6F));
        PartDefinition bridge = goggles_hinge.addOrReplaceChild("bridge", CubeListBuilder.create()
            // hinge_moving_link
            .texOffs(30, 43).addBox(-0.63F, 0.3F, -0.92F, 1.26F, 1.15F, 0.34F, new CubeDeformation(0.0F))
            // armored_bridge_core
            .texOffs(21, 20).addBox(-0.72F, 0.9F, -1.95F, 1.44F, 2.75F, 1.47F, new CubeDeformation(0.0F))
            // bridge_front_inset
            .texOffs(57, 43).addBox(-0.45F, 1.65F, -2.15F, 0.9F, 1.05F, 0.2F, new CubeDeformation(0.0F))
            // bridge_lower_fastener
            .texOffs(28, 55).addBox(-0.25F, 2.95F, -2.16F, 0.5F, 0.42F, 0.2F, new CubeDeformation(0.0F))
            // controller_housing
            .texOffs(109, 20).addBox(-1.0F, 0.65F, -2.53F, 2.0F, 1.0F, 2.01F, new CubeDeformation(0.0F))
            // controller_armored_lid
            .texOffs(111, 36).addBox(-0.9F, 0.5F, -2.35F, 1.8F, 0.15F, 1.65F, new CubeDeformation(0.0F))
            // controller_front_rim
            .texOffs(53, 49).addBox(-0.85F, 0.77F, -2.7F, 1.7F, 0.71F, 0.17F, new CubeDeformation(0.0F))
            // controller_face
            .texOffs(34, 55).addBox(-0.61F, 0.9F, -2.83F, 1.22F, 0.49F, 0.13F, new CubeDeformation(0.0F))
            // controller_status_led
            .texOffs(67, 55).addBox(-0.19F, 1.01F, -2.92F, 0.38F, 0.27F, 0.09F, new CubeDeformation(0.0F))
            , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition left_optic = goggles_hinge.addOrReplaceChild("left_optic", CubeListBuilder.create()
            // left_barrel_vertical
            .texOffs(59, 3).addBox(-4.0F, 0.95F, -3.34F, 3.0F, 3.8F, 2.59F, new CubeDeformation(0.0F))
            // left_barrel_horizontal
            .texOffs(91, 3).addBox(-4.45F, 1.5F, -3.34F, 3.9F, 2.7F, 2.59F, new CubeDeformation(0.0F))
            // left_rear_eyecup
            .texOffs(67, 20).addBox(-4.05F, 1.35F, -0.8F, 3.1F, 3.0F, 0.42F, new CubeDeformation(0.0F))
            // left_top_armor
            .texOffs(24, 29).addBox(-3.8F, 0.85F, -2.95F, 2.6F, 0.45F, 2.33F, new CubeDeformation(0.0F))
            // left_bottom_armor
            .texOffs(113, 29).addBox(-3.75F, 4.4F, -2.85F, 2.5F, 0.4F, 2.07F, new CubeDeformation(0.0F))
            // left_left_armor
            .texOffs(31, 20).addBox(-4.52F, 1.8F, -2.82F, 0.42F, 2.1F, 1.87F, new CubeDeformation(0.0F))
            // left_right_armor
            .texOffs(40, 20).addBox(-0.9F, 1.8F, -2.82F, 0.42F, 2.1F, 1.87F, new CubeDeformation(0.0F))
            // left_left_seam
            .texOffs(91, 36).addBox(-4.6F, 2.02F, -2.26F, 0.14F, 1.66F, 0.33F, new CubeDeformation(0.0F))
            // left_right_seam
            .texOffs(96, 36).addBox(-0.54F, 2.02F, -2.26F, 0.14F, 1.66F, 0.33F, new CubeDeformation(0.0F))
            // left_side_bolt_0_0
            .texOffs(8, 55).addBox(-4.61F, 1.95F, -1.72F, 0.09F, 0.36F, 0.38F, new CubeDeformation(0.0F))
            // left_side_bolt_0_1
            .texOffs(114, 49).addBox(-4.61F, 3.39F, -1.72F, 0.09F, 0.36F, 0.38F, new CubeDeformation(0.0F))
            // left_side_bolt_1_0
            .texOffs(13, 55).addBox(-0.48F, 1.95F, -1.72F, 0.09F, 0.36F, 0.38F, new CubeDeformation(0.0F))
            // left_side_bolt_1_1
            .texOffs(119, 49).addBox(-0.48F, 3.39F, -1.72F, 0.09F, 0.36F, 0.38F, new CubeDeformation(0.0F))
            // left_rim_top
            .texOffs(13, 49).addBox(-3.6F, 0.87F, -3.83F, 2.2F, 0.45F, 0.63F, new CubeDeformation(0.0F))
            // left_rim_bottom
            .texOffs(23, 49).addBox(-3.6F, 4.38F, -3.83F, 2.2F, 0.45F, 0.63F, new CubeDeformation(0.0F))
            // left_rim_left
            .texOffs(122, 20).addBox(-4.48F, 1.75F, -3.83F, 0.45F, 2.2F, 0.63F, new CubeDeformation(0.0F))
            // left_rim_right
            .texOffs(3, 29).addBox(-0.97F, 1.75F, -3.83F, 0.45F, 2.2F, 0.63F, new CubeDeformation(0.0F))
            // left_rim_left_upper
            .texOffs(64, 43).addBox(-4.14F, 1.21F, -3.83F, 0.54F, 0.54F, 0.63F, new CubeDeformation(0.0F))
            // left_rim_left_lower
            .texOffs(71, 43).addBox(-4.14F, 3.95F, -3.83F, 0.54F, 0.54F, 0.63F, new CubeDeformation(0.0F))
            // left_rim_right_upper
            .texOffs(78, 43).addBox(-1.4F, 1.21F, -3.83F, 0.54F, 0.54F, 0.63F, new CubeDeformation(0.0F))
            // left_rim_right_lower
            .texOffs(85, 43).addBox(-1.4F, 3.95F, -3.83F, 0.54F, 0.54F, 0.63F, new CubeDeformation(0.0F))
            // left_rim_top_edge
            .texOffs(47, 55).addBox(-3.55F, 0.82F, -3.75F, 2.1F, 0.07F, 0.5F, new CubeDeformation(0.0F))
            // left_lens_center
            .texOffs(91, 20).addBox(-3.58F, 1.34F, -3.66F, 2.16F, 3.02F, 0.32F, new CubeDeformation(0.0F))
            // left_lens_left_shoulder
            .texOffs(89, 29).addBox(-4.01F, 1.77F, -3.66F, 0.43F, 2.16F, 0.32F, new CubeDeformation(0.0F))
            // left_lens_right_shoulder
            .texOffs(95, 29).addBox(-1.42F, 1.77F, -3.66F, 0.43F, 2.16F, 0.32F, new CubeDeformation(0.0F))
            , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition right_optic = goggles_hinge.addOrReplaceChild("right_optic", CubeListBuilder.create()
            // right_barrel_vertical
            .texOffs(75, 3).addBox(1.0F, 0.95F, -3.34F, 3.0F, 3.8F, 2.59F, new CubeDeformation(0.0F))
            // right_barrel_horizontal
            .texOffs(108, 3).addBox(0.55F, 1.5F, -3.34F, 3.9F, 2.7F, 2.59F, new CubeDeformation(0.0F))
            // right_rear_eyecup
            .texOffs(79, 20).addBox(0.95F, 1.35F, -0.8F, 3.1F, 3.0F, 0.42F, new CubeDeformation(0.0F))
            // right_top_armor
            .texOffs(38, 29).addBox(1.2F, 0.85F, -2.95F, 2.6F, 0.45F, 2.33F, new CubeDeformation(0.0F))
            // right_bottom_armor
            .texOffs(3, 36).addBox(1.25F, 4.4F, -2.85F, 2.5F, 0.4F, 2.07F, new CubeDeformation(0.0F))
            // right_left_armor
            .texOffs(49, 20).addBox(0.48F, 1.8F, -2.82F, 0.42F, 2.1F, 1.87F, new CubeDeformation(0.0F))
            // right_right_armor
            .texOffs(58, 20).addBox(4.1F, 1.8F, -2.82F, 0.42F, 2.1F, 1.87F, new CubeDeformation(0.0F))
            // right_left_seam
            .texOffs(101, 36).addBox(0.4F, 2.02F, -2.26F, 0.14F, 1.66F, 0.33F, new CubeDeformation(0.0F))
            // right_right_seam
            .texOffs(106, 36).addBox(4.46F, 2.02F, -2.26F, 0.14F, 1.66F, 0.33F, new CubeDeformation(0.0F))
            // right_side_bolt_0_0
            .texOffs(18, 55).addBox(0.39F, 1.95F, -1.72F, 0.09F, 0.36F, 0.38F, new CubeDeformation(0.0F))
            // right_side_bolt_0_1
            .texOffs(124, 49).addBox(0.39F, 3.39F, -1.72F, 0.09F, 0.36F, 0.38F, new CubeDeformation(0.0F))
            // right_side_bolt_1_0
            .texOffs(23, 55).addBox(4.52F, 1.95F, -1.72F, 0.09F, 0.36F, 0.38F, new CubeDeformation(0.0F))
            // right_side_bolt_1_1
            .texOffs(3, 55).addBox(4.52F, 3.39F, -1.72F, 0.09F, 0.36F, 0.38F, new CubeDeformation(0.0F))
            // right_rim_top
            .texOffs(33, 49).addBox(1.4F, 0.87F, -3.83F, 2.2F, 0.45F, 0.63F, new CubeDeformation(0.0F))
            // right_rim_bottom
            .texOffs(43, 49).addBox(1.4F, 4.38F, -3.83F, 2.2F, 0.45F, 0.63F, new CubeDeformation(0.0F))
            // right_rim_left
            .texOffs(10, 29).addBox(0.52F, 1.75F, -3.83F, 0.45F, 2.2F, 0.63F, new CubeDeformation(0.0F))
            // right_rim_right
            .texOffs(17, 29).addBox(4.03F, 1.75F, -3.83F, 0.45F, 2.2F, 0.63F, new CubeDeformation(0.0F))
            // right_rim_left_upper
            .texOffs(92, 43).addBox(0.86F, 1.21F, -3.83F, 0.54F, 0.54F, 0.63F, new CubeDeformation(0.0F))
            // right_rim_left_lower
            .texOffs(99, 43).addBox(0.86F, 3.95F, -3.83F, 0.54F, 0.54F, 0.63F, new CubeDeformation(0.0F))
            // right_rim_right_upper
            .texOffs(106, 43).addBox(3.6F, 1.21F, -3.83F, 0.54F, 0.54F, 0.63F, new CubeDeformation(0.0F))
            // right_rim_right_lower
            .texOffs(113, 43).addBox(3.6F, 3.95F, -3.83F, 0.54F, 0.54F, 0.63F, new CubeDeformation(0.0F))
            // right_rim_top_edge
            .texOffs(57, 55).addBox(1.45F, 0.82F, -3.75F, 2.1F, 0.07F, 0.5F, new CubeDeformation(0.0F))
            // right_lens_center
            .texOffs(100, 20).addBox(1.42F, 1.34F, -3.66F, 2.16F, 3.02F, 0.32F, new CubeDeformation(0.0F))
            // right_lens_left_shoulder
            .texOffs(101, 29).addBox(0.99F, 1.77F, -3.66F, 0.43F, 2.16F, 0.32F, new CubeDeformation(0.0F))
            // right_lens_right_shoulder
            .texOffs(107, 29).addBox(3.58F, 1.77F, -3.66F, 0.43F, 2.16F, 0.32F, new CubeDeformation(0.0F))
            , PartPose.offset(0.0F, 0.0F, 0.0F));
        // HumanoidModel's constructor requires these nodes; they contain no geometry.
        for (String name : new String[]{"hat", "body", "left_arm", "right_arm", "left_leg", "right_leg"}) {
            root.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.ZERO);
        }
        return LayerDefinition.create(mesh, 128, 128);
    }
}
