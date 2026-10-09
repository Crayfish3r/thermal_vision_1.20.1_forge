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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public final class ThermalVisionWorldRenderer {
    private static final ResourceLocation HEAT_AURA_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(ThermalVision.MOD_ID, "textures/effect/heat_aura.png");

    private static final List<BlockHeatSource> CACHED_LIGHT_SOURCES = new ArrayList<>();
    private static ClientLevel cachedLightSourceLevel;
    private static BlockPos lastLightScanCenter = BlockPos.ZERO;
    private static long lastLightScanTick = Long.MIN_VALUE;

    private ThermalVisionWorldRenderer() {
    }

    public static void render(RenderLevelStageEvent event) {
        if (!ClientThermalVisionState.isActive()
            || event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        Player localPlayer = minecraft.player;
        if (level == null || localPlayer == null) {
            return;
        }

        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(event.getPoseStack());
        Camera camera = event.getCamera();
        Vec3 cameraPosition = camera.getPosition();
        List<LightSourceTarget> lightTargets = getVisibleLightSources(level, localPlayer, camera);
        if (lightTargets.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        try {
            poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
            applyThermalRenderState();
            try {
                renderLightSourcePass(poseStack, lightTargets, camera, event.getPartialTick());
            } finally {
                restoreThermalRenderState();
            }
        } finally {
            poseStack.popPose();
        }
    }

    public static void reset() {
        CACHED_LIGHT_SOURCES.clear();
        cachedLightSourceLevel = null;
        lastLightScanCenter = BlockPos.ZERO;
        lastLightScanTick = Long.MIN_VALUE;
    }

    private static void renderLightSourcePass(
        PoseStack poseStack,
        List<LightSourceTarget> targets,
        Camera camera,
        float partialTick
    ) {
        RenderSystem.setShaderTexture(0, HEAT_AURA_TEXTURE);

        BufferBuilder builder = Tesselator.getInstance().begin(
            VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP);

        long gameTime = Minecraft.getInstance().level == null
            ? 0L
            : Minecraft.getInstance().level.getGameTime();

        for (LightSourceTarget target : targets) {
            renderLightSourceAura(
                poseStack,
                builder,
                target,
                camera.rotation(),
                gameTime,
                partialTick
            );
        }

        BufferUploader.drawWithShader(builder.buildOrThrow());
    }

    private static void applyThermalRenderState() {
        RenderSystem.setShader(GameRenderer::getPositionColorTexLightmapShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE
        );
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

    private static List<LightSourceTarget> getVisibleLightSources(
        ClientLevel level,
        Player localPlayer,
        Camera camera
    ) {
        updateLightSourceCache(level, localPlayer);

        Vec3 cameraPosition = camera.getPosition();
        double maxDistanceSqr =
            ThermalVisionConfig.LIGHT_SOURCE_RANGE_BLOCKS
                * ThermalVisionConfig.LIGHT_SOURCE_RANGE_BLOCKS;

        List<LightSourceTarget> targets =
            new ArrayList<>(CACHED_LIGHT_SOURCES.size());

        for (BlockHeatSource source : CACHED_LIGHT_SOURCES) {
            Vec3 center = source.center();
            if (center.distanceToSqr(cameraPosition) > maxDistanceSqr) {
                continue;
            }

            if (!hasUnblockedThermalLine(level, cameraPosition, center)) {
                continue;
            }

            targets.add(
                new LightSourceTarget(
                    center,
                    source.intensity(),
                    source.pulseOffset()
                )
            );
        }

        return targets;
    }

    private static void updateLightSourceCache(
        ClientLevel level,
        Player localPlayer
    ) {
        if (cachedLightSourceLevel != level) {
            reset();
            cachedLightSourceLevel = level;
        }

        BlockPos center = localPlayer.blockPosition();
        long gameTime = level.getGameTime();
        boolean movedFarEnough = center.distSqr(lastLightScanCenter) > 16.0D;
        boolean scanExpired =
            lastLightScanTick == Long.MIN_VALUE
                || gameTime < lastLightScanTick
                || gameTime - lastLightScanTick
                    >= ThermalVisionConfig.LIGHT_SOURCE_SCAN_INTERVAL_TICKS;

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

                    pos.set(
                        center.getX() + x,
                        center.getY() + y,
                        center.getZ() + z
                    );

                    BlockState state = level.getBlockState(pos);
                    int emission = state.getLightEmission(level, pos);
                    if (emission < ThermalVisionConfig.LIGHT_SOURCE_MIN_EMISSION) {
                        continue;
                    }

                    float intensity =
                        Mth.clamp(emission / 15.0F, 0.35F, 1.0F);
                    float pulseOffset =
                        (Math.floorMod(pos.asLong(), 997L) / 997.0F)
                            * Mth.TWO_PI;

                    candidates.add(
                        new ScoredBlockHeatSource(
                            new BlockHeatSource(
                                Vec3.atCenterOf(pos),
                                intensity,
                                pulseOffset
                            ),
                            distanceSqr
                        )
                    );
                }
            }
        }

        candidates.sort(
            (first, second) ->
                Integer.compare(first.distanceSqr(), second.distanceSqr())
        );

        int count = Math.min(
            candidates.size(),
            ThermalVisionConfig.LIGHT_SOURCE_MAX_TARGETS
        );

        for (int index = 0; index < count; index++) {
            CACHED_LIGHT_SOURCES.add(candidates.get(index).source());
        }
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
        float pulse =
            0.88F
                + 0.12F
                    * Mth.sin(
                        (gameTime + partialTick) * 0.20F
                            + target.pulseOffset()
                    );
        float intensity = target.intensity();
        float glowSize =
            ThermalVisionConfig.LIGHT_SOURCE_GLOW_SCALE
                * (0.70F + intensity * 0.45F)
                * pulse;
        float coreSize =
            ThermalVisionConfig.LIGHT_SOURCE_CORE_SCALE
                * (0.75F + intensity * 0.35F);

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
            ThermalVisionConfig.LIGHT_SOURCE_GROUND_GLOW_SCALE
                * (0.75F + intensity * 0.25F),
            1.0F,
            0.28F + intensity * 0.20F,
            0.02F,
            0.20F + intensity * 0.12F
        );
    }

    private static boolean hasUnblockedThermalLine(
        ClientLevel level,
        Vec3 start,
        Vec3 end
    ) {
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
            (int) Math.ceil(
                Math.abs(end.x - start.x)
                    + Math.abs(end.y - start.y)
                    + Math.abs(end.z - start.z)
            ) + 8
        );

        BlockPos.MutableBlockPos currentPos =
            new BlockPos.MutableBlockPos();

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

            if (
                x == endPos.getX()
                    && y == endPos.getY()
                    && z == endPos.getZ()
            ) {
                return true;
            }

            currentPos.set(x, y, z);
            if (blocksThermal(level, currentPos)) {
                return false;
            }
        }

        return true;
    }

    private static boolean blocksThermal(
        ClientLevel level,
        BlockPos pos
    ) {
        BlockState state = level.getBlockState(pos);

        if (state.isAir() || !state.getFluidState().isEmpty()) {
            return false;
        }

        if (!state.canOcclude()) {
            return false;
        }

        return Block.isShapeFullBlock(
            state.getOcclusionShape(level, pos)
        );
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

    private static double firstBoundaryDistance(
        double coordinate,
        double direction,
        int step
    ) {
        if (step > 0) {
            return (Math.floor(coordinate) + 1.0D - coordinate)
                / direction;
        }

        if (step < 0) {
            return (coordinate - Math.floor(coordinate))
                / -direction;
        }

        return Double.POSITIVE_INFINITY;
    }

    private static double stepDistance(
        double direction,
        int step
    ) {
        return step == 0
            ? Double.POSITIVE_INFINITY
            : Math.abs(1.0D / direction);
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

        addThermalVertex(
            builder,
            matrix,
            -halfWidth,
            -halfHeight,
            0.0F,
            0.0F,
            1.0F,
            red,
            green,
            blue,
            alpha
        );
        addThermalVertex(
            builder,
            matrix,
            halfWidth,
            -halfHeight,
            0.0F,
            1.0F,
            1.0F,
            red,
            green,
            blue,
            alpha
        );
        addThermalVertex(
            builder,
            matrix,
            halfWidth,
            halfHeight,
            0.0F,
            1.0F,
            0.0F,
            red,
            green,
            blue,
            alpha
        );
        addThermalVertex(
            builder,
            matrix,
            -halfWidth,
            halfHeight,
            0.0F,
            0.0F,
            0.0F,
            red,
            green,
            blue,
            alpha
        );

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

        addThermalVertex(
            builder,
            matrix,
            localX - halfSize,
            localY,
            localZ - halfSize,
            0.0F,
            0.0F,
            red,
            green,
            blue,
            alpha
        );
        addThermalVertex(
            builder,
            matrix,
            localX + halfSize,
            localY,
            localZ - halfSize,
            1.0F,
            0.0F,
            red,
            green,
            blue,
            alpha
        );
        addThermalVertex(
            builder,
            matrix,
            localX + halfSize,
            localY,
            localZ + halfSize,
            1.0F,
            1.0F,
            red,
            green,
            blue,
            alpha
        );
        addThermalVertex(
            builder,
            matrix,
            localX - halfSize,
            localY,
            localZ + halfSize,
            0.0F,
            1.0F,
            red,
            green,
            blue,
            alpha
        );
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
        builder.addVertex(matrix, x, y, z)
            .setColor(red, green, blue, alpha)
            .setUv(u, v)
            .setUv2(LightTexture.FULL_BRIGHT & 0xFFFF, LightTexture.FULL_BRIGHT >>> 16);
    }

    private record BlockHeatSource(
        Vec3 center,
        float intensity,
        float pulseOffset
    ) {
    }

    private record ScoredBlockHeatSource(
        BlockHeatSource source,
        int distanceSqr
    ) {
    }

    private record LightSourceTarget(
        Vec3 center,
        float intensity,
        float pulseOffset
    ) {
    }
}
