package com.yourname.thermalvision.client.model;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
/** Generated from the supplied modded_entity bbmodel by export_variants.py. */
public final class NightVisionGeometry {
    private NightVisionGeometry() {}
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
        PartDefinition helmet_shell = fixed_mount.addOrReplaceChild("helmet_shell", CubeListBuilder.create()
            // helmet_crown
            .texOffs(3, 65).addBox(-5.55F, -9.0F, -5.35F, 11.1F, 0.85F, 10.85F, new CubeDeformation(0.0F))
            // helmet_left_shell
            .texOffs(51, 65).addBox(-5.8F, -8.15F, -5.12F, 0.68F, 2.65F, 10.52F, new CubeDeformation(0.0F))
            // helmet_right_shell
            .texOffs(78, 65).addBox(5.12F, -8.15F, -5.12F, 0.68F, 2.65F, 10.52F, new CubeDeformation(0.0F))
            // helmet_rear_shell
            .texOffs(105, 65).addBox(-5.12F, -8.15F, 5.12F, 10.24F, 3.35F, 0.68F, new CubeDeformation(0.0F))
            // helmet_front_brow
            .texOffs(131, 65).addBox(-5.12F, -8.15F, -5.7F, 10.24F, 1.3F, 0.58F, new CubeDeformation(0.0F))
            // helmet_crown_rail_left
            .texOffs(157, 65).addBox(-4.15F, -9.18F, -3.8F, 0.75F, 0.18F, 7.3F, new CubeDeformation(0.0F))
            // helmet_crown_rail_right
            .texOffs(178, 65).addBox(3.4F, -9.18F, -3.8F, 0.75F, 0.18F, 7.3F, new CubeDeformation(0.0F))
            , PartPose.offset(0.0F, 0.0F, 0.0F));
        PartDefinition goggles_hinge = head.addOrReplaceChild("goggles_hinge", CubeListBuilder.create()
            , PartPose.offset(0.0F, -7.25F, -5.6F));
        PartDefinition visor_frame = goggles_hinge.addOrReplaceChild("visor_frame", CubeListBuilder.create()
            // hinge_moving_link
            .texOffs(30, 43).addBox(-0.63F, 0.3F, -0.92F, 1.26F, 1.15F, 0.34F, new CubeDeformation(0.0F))
            // visor_brow_frame
            .texOffs(199, 65).addBox(-4.65F, 0.6F, -2.85F, 9.3F, 0.55F, 2.35F, new CubeDeformation(0.0F))
            // visor_lower_frame
            .texOffs(227, 65).addBox(-4.65F, 4.35F, -2.85F, 9.3F, 0.55F, 2.25F, new CubeDeformation(0.0F))
            // visor_left_frame
            .texOffs(3, 83).addBox(-5.2F, 1.6F, -2.85F, 0.55F, 2.3F, 2.4F, new CubeDeformation(0.0F))
            // visor_right_frame
            .texOffs(13, 83).addBox(4.65F, 1.6F, -2.85F, 0.55F, 2.3F, 2.4F, new CubeDeformation(0.0F))
            // visor_top_left_corner
            .texOffs(23, 83).addBox(-4.98F, 0.95F, -2.85F, 0.88F, 0.65F, 2.25F, new CubeDeformation(0.0F))
            // visor_top_right_corner
            .texOffs(34, 83).addBox(4.1F, 0.95F, -2.85F, 0.88F, 0.65F, 2.25F, new CubeDeformation(0.0F))
            // visor_bottom_left_corner
            .texOffs(45, 83).addBox(-4.98F, 3.9F, -2.85F, 0.88F, 0.65F, 2.25F, new CubeDeformation(0.0F))
            // visor_bottom_right_corner
            .texOffs(56, 83).addBox(4.1F, 3.9F, -2.85F, 0.88F, 0.65F, 2.25F, new CubeDeformation(0.0F))
            // visor_left_hinge
            .texOffs(67, 83).addBox(-5.65F, 2.0F, -1.65F, 0.45F, 1.05F, 1.05F, new CubeDeformation(0.0F))
            // visor_right_hinge
            .texOffs(74, 83).addBox(5.2F, 2.0F, -1.65F, 0.45F, 1.05F, 1.05F, new CubeDeformation(0.0F))
            // visor_control_cap
            .texOffs(81, 83).addBox(-0.8F, 0.15F, -1.5F, 1.6F, 0.45F, 1.0F, new CubeDeformation(0.0F))
            , PartPose.offset(-0.0F, 0.0F, 0.0F));
        PartDefinition continuous_green_visor = goggles_hinge.addOrReplaceChild("continuous_green_visor", CubeListBuilder.create()
            // visor_single_pane
            .texOffs(91, 83).addBox(-4.12F, 1.15F, -2.7F, 8.24F, 3.2F, 0.38F, new CubeDeformation(0.0F))
            // visor_left_glass_corner
            .texOffs(113, 83).addBox(-4.65F, 1.6F, -2.7F, 0.53F, 2.3F, 0.38F, new CubeDeformation(0.0F))
            // visor_right_glass_corner
            .texOffs(119, 83).addBox(4.12F, 1.6F, -2.7F, 0.53F, 2.3F, 0.38F, new CubeDeformation(0.0F))
            , PartPose.offset(-0.0F, 0.0F, 0.0F));
        for (String name : new String[]{"hat", "body", "left_arm", "right_arm", "left_leg", "right_leg"})
            root.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.ZERO);
        return LayerDefinition.create(mesh, 256, 256);
    }
}
