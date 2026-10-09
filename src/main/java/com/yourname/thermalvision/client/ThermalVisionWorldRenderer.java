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
import com.yourname.thermalvision.client.render.ThermalPostRenderState;
import com.yourname.thermalvision.common.ThermalVisionConfig;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.ShaderInstance;
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
        new ResourceLocation(ThermalVision.MOD_ID, "textures/effect/heat_aura.png");

    private static final List<BlockHeatSource> CACHED_LIGHT_SOURCES = new ArrayList<>();
    private static final List<BlockHeatSource> VISIBLE_LIGHT_SOURCES = new ArrayList<>();
    private static final PriorityQueue<ScoredBlockHeatSource> SCAN_CANDIDATES =
        new PriorityQueue<>(Comparator.comparingInt(ScoredBlockHeatSource::distanceSqr).reversed());
    private static final BlockPos.MutableBlockPos SCAN_POS = new BlockPos.MutableBlockPos();
    private static final ThermalPostRenderState RENDER_STATE = new ThermalPostRenderState();
    private static ClientLevel cachedLightSourceLevel;
    private static BlockPos lastLightScanCenter = BlockPos.ZERO;
    private static long lastLightScanTick = Long.MIN_VALUE;
    private static int scanIndex = -1;

    private ThermalVisionWorldRenderer() {
    }

    public static void render(RenderLevelStageEvent event) {
        if (ThermalVisionPostProcessor.effectStrength() <= 0.001F
            || event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
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
        if (cachedLightSourceLevel != level || VISIBLE_LIGHT_SOURCES.isEmpty()) {
            return;
        }

        ShaderInstance previousShader = RenderSystem.getShader();
        int previousTexture = RenderSystem.getShaderTexture(0);
        float[] color = RenderSystem.getShaderColor();
        float red = color[0], green = color[1], blue = color[2], alpha = color[3];
        RENDER_STATE.capture();
        poseStack.pushPose();
        try {
            poseStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z);
            applyThermalRenderState();
            renderLightSourcePass(poseStack, VISIBLE_LIGHT_SOURCES, camera, event.getPartialTick());
        } finally {
            RenderSystem.setShader(() -> previousShader);
            RenderSystem.setShaderTexture(0, previousTexture);
            RenderSystem.setShaderColor(red, green, blue, alpha);
            RENDER_STATE.restore();
            poseStack.popPose();
        }
    }

    public static void reset() {
        CACHED_LIGHT_SOURCES.clear();
        VISIBLE_LIGHT_SOURCES.clear();
        SCAN_CANDIDATES.clear();
        scanIndex = -1;
        cachedLightSourceLevel = null;
        lastLightScanCenter = BlockPos.ZERO;
        lastLightScanTick = Long.MIN_VALUE;
    }

    private static void renderLightSourcePass(
        PoseStack poseStack,
        List<BlockHeatSource> targets,
        Camera camera,
        float partialTick
    ) {
        RenderSystem.setShaderTexture(0, HEAT_AURA_TEXTURE);

        BufferBuilder builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP);

        long gameTime = Minecraft.getInstance().level == null
            ? 0L
            : Minecraft.getInstance().level.getGameTime();

        for (BlockHeatSource target : targets) {
            renderLightSourceAura(
                poseStack,
                builder,
                target,
                camera.rotation(),
                gameTime,
                partialTick
            );
        }

        BufferUploader.drawWithShader(builder.end());
    }

    private static void applyThermalRenderState() {
        RenderSystem.setShader(GameRenderer::getPositionColorTexLightmapShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, ThermalVisionPostProcessor.effectStrength());
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(
            GlStateManager.SourceFactor.SRC_ALPHA,
            GlStateManager.DestFactor.ONE
        );
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
    }

    /** World queries run at most once per game tick, never once per rendered frame. */
    public static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        Player localPlayer = minecraft.player;
        if (level == null || localPlayer == null) {
            reset();
            return;
        }
        if (cachedLightSourceLevel != level) {
            reset();
            cachedLightSourceLevel = level;
        }
        if (!ClientThermalVisionState.isActive()) {
            if (ThermalVisionPostProcessor.effectStrength() <= 0.001F) reset();
            return;
        }
        updateLightSourceCache(level, localPlayer);

        Vec3 cameraPosition = minecraft.gameRenderer.getMainCamera().getPosition();
        double maxDistanceSqr =
            ThermalVisionConfig.LIGHT_SOURCE_RANGE_BLOCKS
                * ThermalVisionConfig.LIGHT_SOURCE_RANGE_BLOCKS;

        VISIBLE_LIGHT_SOURCES.clear();

        for (BlockHeatSource source : CACHED_LIGHT_SOURCES) {
            Vec3 center = source.center();
            if (center.distanceToSqr(cameraPosition) > maxDistanceSqr) {
                continue;
            }
            SCAN_POS.set(Mth.floor(center.x), Mth.floor(center.y), Mth.floor(center.z));
            if (!level.hasChunkAt(SCAN_POS)
                || level.getBlockState(SCAN_POS).getLightEmission(level, SCAN_POS)
                    < ThermalVisionConfig.LIGHT_SOURCE_MIN_EMISSION) {
                continue;
            }

            if (!hasUnblockedThermalLine(level, cameraPosition, center)) {
                continue;
            }

            VISIBLE_LIGHT_SOURCES.add(source);
        }
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

        if (scanIndex < 0 && !movedFarEnough && !scanExpired) {
            return;
        }

        if (scanIndex < 0) {
            lastLightScanCenter = center.immutable();
            lastLightScanTick = gameTime;
            SCAN_CANDIDATES.clear();
            scanIndex = 0;
        }

        int range = ThermalVisionConfig.LIGHT_SOURCE_RANGE_BLOCKS;
        int verticalRange = ThermalVisionConfig.LIGHT_SOURCE_VERTICAL_RANGE_BLOCKS;
        int rangeSqr = range * range;
        int diameter = range * 2 + 1;
        int volume = diameter * diameter * (verticalRange * 2 + 1);
        int endIndex = Math.min(volume, scanIndex + ThermalVisionConfig.LIGHT_SOURCE_SCAN_BUDGET);
        BlockPos.MutableBlockPos pos = SCAN_POS;
        for (; scanIndex < endIndex; scanIndex++) {
            int y = scanIndex / (diameter * diameter) - verticalRange;
            int x = (scanIndex / diameter) % diameter - range;
            int z = scanIndex % diameter - range;
            int distanceSqr = x * x + y * y + z * z;
            if (distanceSqr > rangeSqr) continue;

            pos.set(lastLightScanCenter.getX() + x, lastLightScanCenter.getY() + y,
                lastLightScanCenter.getZ() + z);
            if (level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos)) continue;

            BlockState state = level.getBlockState(pos);
            int emission = state.getLightEmission(level, pos);
            if (emission < ThermalVisionConfig.LIGHT_SOURCE_MIN_EMISSION) continue;
            if (SCAN_CANDIDATES.size() >= ThermalVisionConfig.LIGHT_SOURCE_MAX_TARGETS) {
                if (distanceSqr >= SCAN_CANDIDATES.peek().distanceSqr()) continue;
                SCAN_CANDIDATES.poll();
            }

            float intensity = Mth.clamp(emission / 15.0F, 0.35F, 1.0F);
            float pulseOffset = (Math.floorMod(pos.asLong(), 997L) / 997.0F) * Mth.TWO_PI;
            SCAN_CANDIDATES.add(new ScoredBlockHeatSource(
                new BlockHeatSource(Vec3.atCenterOf(pos), intensity, pulseOffset), distanceSqr));
        }
        if (scanIndex == volume) {
            CACHED_LIGHT_SOURCES.clear();
            while (!SCAN_CANDIDATES.isEmpty()) {
                CACHED_LIGHT_SOURCES.add(SCAN_CANDIDATES.poll().source());
            }
            scanIndex = -1;
        }
    }

    private static void renderLightSourceAura(
        PoseStack poseStack,
        BufferBuilder builder,
        BlockHeatSource target,
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
        builder.vertex(matrix, x, y, z)
            .color(red, green, blue, alpha)
            .uv(u, v)
            .uv2(LightTexture.FULL_BRIGHT)
            .endVertex();
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

}
