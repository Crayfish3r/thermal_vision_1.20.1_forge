package com.yourname.thermalvision.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.yourname.thermalvision.ThermalVision;
import com.yourname.thermalvision.common.ThermalVisionConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public final class ThermalVisionWorldRenderer {
    private static final ResourceLocation HEAT_AURA_TEXTURE =
        new ResourceLocation(ThermalVision.MOD_ID, "textures/effect/heat_aura.png");
    private static final ResourceLocation HEAT_OUTLINE_TEXTURE =
        new ResourceLocation(ThermalVision.MOD_ID, "textures/effect/heat_outline.png");
    private static final List<BlockHeatSource> CACHED_LIGHT_SOURCES = new ArrayList<>();
    private static BlockPos lastLightScanCenter = BlockPos.ZERO;
    private static long lastLightScanTick = Long.MIN_VALUE;

    private ThermalVisionWorldRenderer() {
    }

    public static void render(RenderLevelStageEvent event) {
        if (!ClientThermalVisionState.isActive()) {
            return;
        }

        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        Player localPlayer = minecraft.player;

        if (level == null || localPlayer == null) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Camera camera = event.getCamera();
        Vec3 cameraPosition = camera.getPosition();
        List<FrameTarget> targets = getVisibleTargets(level, localPlayer, camera, event.getPartialTick());
        List<LightSourceTarget> lightTargets = getVisibleLightSources(level, localPlayer, camera);

        poseStack.pushPose();
        poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);

        if (!targets.isEmpty() || !lightTargets.isEmpty()) {
            applyThermalRenderState();
            renderLightSourcePass(poseStack, lightTargets, camera, event.getPartialTick());
            renderAuraPass(poseStack, targets, camera, event.getPartialTick());
            renderOutlinePass(poseStack, targets, camera);
            restoreThermalRenderState();
        }

        poseStack.popPose();
    }

    private static void renderAuraPass(
        PoseStack poseStack,
        List<FrameTarget> targets,
        Camera camera,
        float partialTick
    ) {
        RenderSystem.setShaderTexture(0, HEAT_AURA_TEXTURE);

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP);

        for (FrameTarget target : targets) {
            renderTargetAura(poseStack, builder, target, camera.rotation(), partialTick);
        }

        BufferUploader.drawWithShader(builder.end());
    }

    private static void renderLightSourcePass(
        PoseStack poseStack,
        List<LightSourceTarget> targets,
        Camera camera,
        float partialTick
    ) {
        if (targets.isEmpty()) {
            return;
        }

        RenderSystem.setShaderTexture(0, HEAT_AURA_TEXTURE);

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP);

        long gameTime = Minecraft.getInstance().level == null ? 0L : Minecraft.getInstance().level.getGameTime();
        for (LightSourceTarget target : targets) {
            renderLightSourceAura(poseStack, builder, target, camera.rotation(), gameTime, partialTick);
        }

        BufferUploader.drawWithShader(builder.end());
    }

    private static void renderOutlinePass(
        PoseStack poseStack,
        List<FrameTarget> targets,
        Camera camera
    ) {
        RenderSystem.setShaderTexture(0, HEAT_OUTLINE_TEXTURE);

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP);

        for (FrameTarget target : targets) {
            renderTargetOutline(poseStack, builder, target, camera.rotation());
        }

        BufferUploader.drawWithShader(builder.end());
    }

    private static void applyThermalRenderState() {
        RenderSystem.setShader(GameRenderer::getPositionColorTexLightmapShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
    }

    private static void restoreThermalRenderState() {
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static List<LivingEntity> getTargets(ClientLevel level, Player localPlayer) {
        double range = ThermalVisionConfig.DEFAULT_RANGE_BLOCKS;
        AABB searchArea = localPlayer.getBoundingBox().inflate(range);

        return level.getEntitiesOfClass(
            LivingEntity.class,
            searchArea,
            entity -> isValidTarget(entity, localPlayer, range * range)
        );
    }

    private static boolean isValidTarget(LivingEntity entity, Player localPlayer, double maxDistanceSqr) {
        if (entity == localPlayer && Minecraft.getInstance().options.getCameraType().isFirstPerson()) {
            return false;
        }

        if (!entity.isAlive() || entity.isRemoved()) {
            return false;
        }

        if (ThermalVisionConfig.IGNORE_INVISIBLE_ENTITIES && entity.isInvisible()) {
            return false;
        }

        if (ThermalVisionConfig.IGNORE_SPECTATORS && entity instanceof Player player && player.isSpectator()) {
            return false;
        }

        return entity.distanceToSqr(localPlayer) <= maxDistanceSqr;
    }

    private static List<FrameTarget> getVisibleTargets(
        ClientLevel level,
        Player localPlayer,
        Camera camera,
        float partialTick
    ) {
        Vec3 cameraPosition = camera.getPosition();
        List<LivingEntity> entities = getTargets(level, localPlayer);
        List<FrameTarget> targets = new ArrayList<>(entities.size());

        for (LivingEntity entity : entities) {
            AABB bodyBox = interpolatedBox(entity, partialTick);
            if (!hasThermalLineOfSight(level, cameraPosition, bodyBox)) {
                continue;
            }

            Vec3 center = bodyBox.getCenter();
            double height = Math.max(bodyBox.getYsize(), 0.7D);
            double width = Math.max(Math.max(bodyBox.getXsize(), bodyBox.getZsize()), 0.45D);
            targets.add(new FrameTarget(entity, bodyBox, center, width, height));
        }

        return targets;
    }

    private static List<LightSourceTarget> getVisibleLightSources(
        ClientLevel level,
        Player localPlayer,
        Camera camera
    ) {
        updateLightSourceCache(level, localPlayer);

        Vec3 cameraPosition = camera.getPosition();
        double maxDistanceSqr = ThermalVisionConfig.LIGHT_SOURCE_RANGE_BLOCKS
            * ThermalVisionConfig.LIGHT_SOURCE_RANGE_BLOCKS;
        List<LightSourceTarget> targets = new ArrayList<>(CACHED_LIGHT_SOURCES.size());

        for (BlockHeatSource source : CACHED_LIGHT_SOURCES) {
            Vec3 center = source.center();
            if (center.distanceToSqr(cameraPosition) > maxDistanceSqr) {
                continue;
            }

            if (!hasUnblockedThermalLine(level, cameraPosition, center)) {
                continue;
            }

            targets.add(new LightSourceTarget(center, source.intensity(), source.pulseOffset()));
        }

        return targets;
    }

    private static void updateLightSourceCache(ClientLevel level, Player localPlayer) {
        BlockPos center = localPlayer.blockPosition();
        long gameTime = level.getGameTime();
        boolean movedFarEnough = center.distSqr(lastLightScanCenter) > 16.0D;
        boolean scanExpired = gameTime - lastLightScanTick >= ThermalVisionConfig.LIGHT_SOURCE_SCAN_INTERVAL_TICKS;

        if (!movedFarEnough && !scanExpired) {
            return;
        }

        lastLightScanCenter = center.immutable();
        lastLightScanTick = gameTime;
        CACHED_LIGHT_SOURCES.clear();

        int range = ThermalVisionConfig.LIGHT_SOURCE_RANGE_BLOCKS;
        int verticalRange = ThermalVisionConfig.LIGHT_SOURCE_VERTICAL_RANGE_BLOCKS;
        int rangeSqr = range * range;
        List<ScoredBlockHeatSource> candidates = new ArrayList<>();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int y = -verticalRange; y <= verticalRange; y++) {
            for (int x = -range; x <= range; x++) {
                for (int z = -range; z <= range; z++) {
                    int distanceSqr = x * x + y * y + z * z;
                    if (distanceSqr > rangeSqr) {
                        continue;
                    }

                    pos.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    BlockState state = level.getBlockState(pos);
                    int emission = state.getLightEmission(level, pos);
                    if (emission < ThermalVisionConfig.LIGHT_SOURCE_MIN_EMISSION) {
                        continue;
                    }

                    float intensity = Mth.clamp(emission / 15.0F, 0.35F, 1.0F);
                    float pulseOffset = (Math.floorMod(pos.asLong(), 997L) / 997.0F) * Mth.TWO_PI;
                    candidates.add(new ScoredBlockHeatSource(
                        new BlockHeatSource(pos.immutable(), Vec3.atCenterOf(pos), intensity, pulseOffset),
                        distanceSqr
                    ));
                }
            }
        }

        candidates.sort((first, second) -> Integer.compare(first.distanceSqr(), second.distanceSqr()));
        int count = Math.min(candidates.size(), ThermalVisionConfig.LIGHT_SOURCE_MAX_TARGETS);
        for (int index = 0; index < count; index++) {
            CACHED_LIGHT_SOURCES.add(candidates.get(index).source());
        }
    }

    private static void renderTargetAura(
        PoseStack poseStack,
        BufferBuilder builder,
        FrameTarget target,
        Quaternionf cameraRotation,
        float partialTick
    ) {
        AABB bodyBox = target.bodyBox();
        Vec3 center = target.center();
        double height = target.height();
        double width = target.width();
        float pulse = 0.90F + 0.10F * Mth.sin((target.entity().tickCount + partialTick) * 0.22F);

        renderCameraBillboard(
            poseStack,
            builder,
            cameraRotation,
            center.x,
            bodyBox.minY + height * 0.52D,
            center.z,
            (float) width * ThermalVisionConfig.OUTER_AURA_WIDTH_SCALE * pulse,
            (float) height * ThermalVisionConfig.OUTER_AURA_HEIGHT_SCALE * pulse,
            1.0F,
            0.30F,
            0.02F,
            ThermalVisionConfig.OUTER_AURA_ALPHA
        );

        renderCameraBillboard(
            poseStack,
            builder,
            cameraRotation,
            center.x,
            bodyBox.minY + height * 0.56D,
            center.z,
            (float) width * ThermalVisionConfig.BODY_GLOW_WIDTH_SCALE,
            (float) height * ThermalVisionConfig.BODY_GLOW_HEIGHT_SCALE,
            1.0F,
            0.58F,
            0.04F,
            ThermalVisionConfig.BODY_GLOW_ALPHA
        );

        renderCameraBillboard(
            poseStack,
            builder,
            cameraRotation,
            center.x,
            bodyBox.minY + height * 0.70D,
            center.z,
            (float) width * ThermalVisionConfig.HOTSPOT_WIDTH_SCALE,
            (float) height * ThermalVisionConfig.HOTSPOT_HEIGHT_SCALE,
            1.0F,
            0.92F,
            0.14F,
            ThermalVisionConfig.HOTSPOT_ALPHA
        );

        renderCameraBillboard(
            poseStack,
            builder,
            cameraRotation,
            center.x,
            bodyBox.minY + height * 0.90D,
            center.z,
            (float) width * ThermalVisionConfig.HEAD_GLOW_WIDTH_SCALE,
            (float) height * ThermalVisionConfig.HEAD_GLOW_HEIGHT_SCALE,
            1.0F,
            0.78F,
            0.08F,
            ThermalVisionConfig.HEAD_GLOW_ALPHA
        );

        renderHorizontalGlow(
            poseStack,
            builder,
            center.x,
            bodyBox.minY + 0.035D,
            center.z,
            (float) width * ThermalVisionConfig.GROUND_GLOW_SCALE,
            1.0F,
            0.28F,
            0.02F,
            ThermalVisionConfig.GROUND_GLOW_ALPHA
        );
    }

    private static void renderLightSourceAura(
        PoseStack poseStack,
        BufferBuilder builder,
        LightSourceTarget target,
        Quaternionf cameraRotation,
        long gameTime,
        float partialTick
    ) {
        Vec3 center = target.center();
        float pulse = 0.88F + 0.12F * Mth.sin((gameTime + partialTick) * 0.20F + target.pulseOffset());
        float intensity = target.intensity();
        float glowSize = ThermalVisionConfig.LIGHT_SOURCE_GLOW_SCALE * (0.70F + intensity * 0.45F) * pulse;
        float coreSize = ThermalVisionConfig.LIGHT_SOURCE_CORE_SCALE * (0.75F + intensity * 0.35F);

        renderCameraBillboard(
            poseStack,
            builder,
            cameraRotation,
            center.x,
            center.y,
            center.z,
            glowSize,
            glowSize,
            1.0F,
            0.36F + intensity * 0.28F,
            0.02F,
            0.30F + intensity * 0.18F
        );

        renderCameraBillboard(
            poseStack,
            builder,
            cameraRotation,
            center.x,
            center.y,
            center.z,
            coreSize,
            coreSize,
            1.0F,
            0.90F,
            0.12F,
            0.42F + intensity * 0.22F
        );

        renderHorizontalGlow(
            poseStack,
            builder,
            center.x,
            center.y - 0.48D,
            center.z,
            ThermalVisionConfig.LIGHT_SOURCE_GROUND_GLOW_SCALE * (0.75F + intensity * 0.25F),
            1.0F,
            0.28F + intensity * 0.20F,
            0.02F,
            0.20F + intensity * 0.12F
        );
    }

    private static void renderTargetOutline(
        PoseStack poseStack,
        BufferBuilder builder,
        FrameTarget target,
        Quaternionf cameraRotation
    ) {
        AABB bodyBox = target.bodyBox();
        Vec3 center = target.center();
        double height = target.height();
        double width = target.width();

        renderCameraBillboard(
            poseStack,
            builder,
            cameraRotation,
            center.x,
            bodyBox.minY + height * 0.50D,
            center.z,
            (float) width * ThermalVisionConfig.BODY_OUTLINE_WIDTH_SCALE,
            (float) height * ThermalVisionConfig.BODY_OUTLINE_HEIGHT_SCALE,
            1.0F,
            0.64F,
            0.04F,
            ThermalVisionConfig.BODY_OUTLINE_ALPHA
        );

        renderCameraBillboard(
            poseStack,
            builder,
            cameraRotation,
            center.x,
            bodyBox.minY + height * 0.88D,
            center.z,
            (float) width * ThermalVisionConfig.HEAD_OUTLINE_WIDTH_SCALE,
            (float) height * ThermalVisionConfig.HEAD_OUTLINE_HEIGHT_SCALE,
            1.0F,
            0.96F,
            0.28F,
            ThermalVisionConfig.HEAD_OUTLINE_ALPHA
        );
    }

    private static boolean hasThermalLineOfSight(ClientLevel level, Vec3 cameraPosition, AABB bodyBox) {
        Vec3 center = bodyBox.getCenter();
        double height = Math.max(bodyBox.getYsize(), 0.7D);

        return hasUnblockedThermalLine(level, cameraPosition, new Vec3(center.x, bodyBox.minY + height * 0.50D, center.z))
            || hasUnblockedThermalLine(level, cameraPosition, new Vec3(center.x, bodyBox.minY + height * 0.86D, center.z))
            || hasUnblockedThermalLine(level, cameraPosition, new Vec3(center.x, bodyBox.minY + height * 0.18D, center.z));
    }

    private static boolean hasUnblockedThermalLine(ClientLevel level, Vec3 start, Vec3 end) {
        Vec3 delta = end.subtract(start);
        double distance = delta.length();

        if (distance < 0.001D) {
            return true;
        }

        Vec3 direction = delta.scale(1.0D / distance);
        BlockPos startPos = BlockPos.containing(start);
        BlockPos endPos = BlockPos.containing(end);

        int x = startPos.getX();
        int y = startPos.getY();
        int z = startPos.getZ();

        int stepX = sign(direction.x);
        int stepY = sign(direction.y);
        int stepZ = sign(direction.z);

        double tMaxX = firstBoundaryDistance(start.x, direction.x, stepX);
        double tMaxY = firstBoundaryDistance(start.y, direction.y, stepY);
        double tMaxZ = firstBoundaryDistance(start.z, direction.z, stepZ);

        double tDeltaX = stepDistance(direction.x, stepX);
        double tDeltaY = stepDistance(direction.y, stepY);
        double tDeltaZ = stepDistance(direction.z, stepZ);

        int maxSteps = Math.max(
            1,
            (int) Math.ceil(Math.abs(end.x - start.x) + Math.abs(end.y - start.y) + Math.abs(end.z - start.z)) + 8
        );
        BlockPos.MutableBlockPos currentPos = new BlockPos.MutableBlockPos();

        for (int index = 0; index < maxSteps; index++) {
            double traveled;

            if (tMaxX <= tMaxY && tMaxX <= tMaxZ) {
                x += stepX;
                traveled = tMaxX;
                tMaxX += tDeltaX;
            } else if (tMaxY <= tMaxZ) {
                y += stepY;
                traveled = tMaxY;
                tMaxY += tDeltaY;
            } else {
                z += stepZ;
                traveled = tMaxZ;
                tMaxZ += tDeltaZ;
            }

            if (traveled > distance) {
                return true;
            }

            if (x == endPos.getX() && y == endPos.getY() && z == endPos.getZ()) {
                return true;
            }

            currentPos.set(x, y, z);
            if (blocksThermal(level, currentPos)) {
                return false;
            }
        }

        return true;
    }

    private static boolean blocksThermal(ClientLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);

        if (state.isAir() || !state.getFluidState().isEmpty()) {
            return false;
        }

        if (!state.canOcclude()) {
            return false;
        }

        return Block.isShapeFullBlock(state.getOcclusionShape(level, pos));
    }

    private static int sign(double value) {
        if (value > 0.0D) {
            return 1;
        }

        if (value < 0.0D) {
            return -1;
        }

        return 0;
    }

    private static double firstBoundaryDistance(double coordinate, double direction, int step) {
        if (step > 0) {
            return (Math.floor(coordinate) + 1.0D - coordinate) / direction;
        }

        if (step < 0) {
            return (coordinate - Math.floor(coordinate)) / -direction;
        }

        return Double.POSITIVE_INFINITY;
    }

    private static double stepDistance(double direction, int step) {
        return step == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0D / direction);
    }

    private static void renderCameraBillboard(
        PoseStack poseStack,
        BufferBuilder builder,
        Quaternionf cameraRotation,
        double x,
        double y,
        double z,
        float width,
        float height,
        float red,
        float green,
        float blue,
        float alpha
    ) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(cameraRotation);

        Matrix4f matrix = poseStack.last().pose();
        float halfWidth = width * 0.5F;
        float halfHeight = height * 0.5F;

        addThermalVertex(builder, matrix, -halfWidth, -halfHeight, 0.0F, 0.0F, 1.0F, red, green, blue, alpha);
        addThermalVertex(builder, matrix, halfWidth, -halfHeight, 0.0F, 1.0F, 1.0F, red, green, blue, alpha);
        addThermalVertex(builder, matrix, halfWidth, halfHeight, 0.0F, 1.0F, 0.0F, red, green, blue, alpha);
        addThermalVertex(builder, matrix, -halfWidth, halfHeight, 0.0F, 0.0F, 0.0F, red, green, blue, alpha);

        poseStack.popPose();
    }

    private static void renderHorizontalGlow(
        PoseStack poseStack,
        BufferBuilder builder,
        double x,
        double y,
        double z,
        float size,
        float red,
        float green,
        float blue,
        float alpha
    ) {
        Matrix4f matrix = poseStack.last().pose();
        float halfSize = size * 0.5F;
        float localX = (float) x;
        float localY = (float) y;
        float localZ = (float) z;

        addThermalVertex(builder, matrix, localX - halfSize, localY, localZ - halfSize, 0.0F, 0.0F, red, green, blue, alpha);
        addThermalVertex(builder, matrix, localX + halfSize, localY, localZ - halfSize, 1.0F, 0.0F, red, green, blue, alpha);
        addThermalVertex(builder, matrix, localX + halfSize, localY, localZ + halfSize, 1.0F, 1.0F, red, green, blue, alpha);
        addThermalVertex(builder, matrix, localX - halfSize, localY, localZ + halfSize, 0.0F, 1.0F, red, green, blue, alpha);
    }

    private static void addThermalVertex(
        BufferBuilder builder,
        Matrix4f matrix,
        float x,
        float y,
        float z,
        float u,
        float v,
        float red,
        float green,
        float blue,
        float alpha
    ) {
        builder.vertex(matrix, x, y, z)
            .color(red, green, blue, alpha)
            .uv(u, v)
            .uv2(LightTexture.FULL_BRIGHT)
            .endVertex();
    }

    private static AABB interpolatedBox(Entity entity, float partialTick) {
        double x = Mth.lerp(partialTick, entity.xOld, entity.getX());
        double y = Mth.lerp(partialTick, entity.yOld, entity.getY());
        double z = Mth.lerp(partialTick, entity.zOld, entity.getZ());

        return entity.getBoundingBox().move(
            x - entity.getX(),
            y - entity.getY(),
            z - entity.getZ()
        );
    }

    private record FrameTarget(LivingEntity entity, AABB bodyBox, Vec3 center, double width, double height) {
    }

    private record BlockHeatSource(BlockPos pos, Vec3 center, float intensity, float pulseOffset) {
    }

    private record ScoredBlockHeatSource(BlockHeatSource source, int distanceSqr) {
    }

    private record LightSourceTarget(Vec3 center, float intensity, float pulseOffset) {
    }
}
