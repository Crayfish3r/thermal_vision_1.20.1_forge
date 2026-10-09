package com.yourname.thermalvision.audit;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.yourname.thermalvision.ThermalVision;
import com.yourname.thermalvision.client.ClientThermalVisionState;
import com.yourname.thermalvision.client.ThermalVisionPostProcessor;
import com.yourname.thermalvision.client.render.ThermalSignatureResolver;
import com.yourname.thermalvision.common.ServerThermalVisionStates;
import com.yourname.thermalvision.common.ThermalVisionAccess;
import com.yourname.thermalvision.common.ThermalVisionConfig;
import com.yourname.thermalvision.item.ThermalImagerAccessoryItem;
import com.yourname.thermalvision.network.ModNetworking;
import com.yourname.thermalvision.network.ToggleThermalVisionC2SPacket;
import com.yourname.thermalvision.registry.ModItems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;

/** Dev-only integration checks. Enable with -PauditRuntime; clean build before distributing. */
@Mod.EventBusSubscriber(modid = ThermalVision.MOD_ID, value = Dist.CLIENT)
public final class AuditRuntimeChecks {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static int tick;
    private static int phase;
    private static boolean started;
    private static boolean failed;
    private static int pausedCharge;
    private static float pausedFade;
    private static int pauseFrames;
    private static CompletableFuture<Void> serverWork;

    private static void require(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
        LOGGER.info("AUDIT PASS: {}", description);
    }

    private static void server(Consumer<ServerPlayer> action) {
        Minecraft mc = Minecraft.getInstance();
        serverWork = CompletableFuture.runAsync(() -> {
            ServerPlayer player = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
            if (player == null) throw new AssertionError("Server player missing");
            action.accept(player);
        }, mc.getSingleplayerServer());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (!Boolean.getBoolean("thermal_vision.audit") || failed || event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        try {
            if (!started && mc.getOverlay() == null
                && (mc.screen instanceof TitleScreen || mc.screen instanceof AccessibilityOnboardingScreen)) {
                started = true;
                mc.options.pauseOnLostFocus = false;
                mc.createWorldOpenFlows().createFreshLevel("audit-" + System.currentTimeMillis(),
                    new LevelSettings("Thermal audit", GameType.CREATIVE, false, Difficulty.PEACEFUL,
                        true, new GameRules(), WorldDataConfiguration.DEFAULT),
                    new WorldOptions(12345L, false, false), registry -> registry
                        .registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET)
                        .getOrThrow(WorldPresets.FLAT).createWorldDimensions());
                return;
            }
            if (mc.level == null || mc.player == null || mc.isPaused()) return;
            if (serverWork != null) {
                if (!serverWork.isDone()) return;
                serverWork.join();
                serverWork = null;
            }
            tick++;
            if (tick < 40) return;
            tick = 0;
            switch (phase++) {
                case 0 -> server(player -> {
                    player.teleportTo(0.5, 100.0, 0.5);
                    var level = player.serverLevel();
                    for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++) {
                        level.setBlockAndUpdate(new BlockPos(x, 99, z), Blocks.STONE.defaultBlockState());
                        for (int y = 100; y <= 105; y++) {
                            level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
                        }
                    }
                    level.setBlockAndUpdate(new BlockPos(2, 100, 3), Blocks.GLOWSTONE.defaultBlockState());
                    var cow = EntityType.COW.create(level);
                    cow.setNoAi(true);
                    cow.moveTo(0.5, 100.0, 4.5, 180.0F, 0.0F);
                    level.addFreshEntity(cow);
                    player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.THERMAL_GOGGLES.get()));
                    ItemStack goggles = ThermalVisionAccess.getThermalHelmet(player);
                    require(ThermalImagerAccessoryItem.getThermalUseTicks(goggles) == 1200, "fresh NBT charge");
                    ThermalImagerAccessoryItem.setThermalUseTicks(goggles, -100);
                    require(ThermalImagerAccessoryItem.getThermalUseTicks(goggles) == 0, "negative charge clamped");
                    ThermalImagerAccessoryItem.setThermalUseTicks(goggles, Integer.MAX_VALUE);
                    require(ThermalImagerAccessoryItem.getThermalUseTicks(goggles) == 1200, "excess charge clamped");
                    require(ServerThermalVisionStates.acceptToggle(player), "first toggle accepted");
                    require(!ServerThermalVisionStates.acceptToggle(player), "same-tick packet flood rejected");
                });
                case 1 -> ModNetworking.sendToServer(new ToggleThermalVisionC2SPacket());
                case 2 -> {
                    require(ClientThermalVisionState.isActive(), "C2S activation and S2C synchronization");
                    require(ThermalVisionPostProcessor.effectStrength() > 0.99F, "fade reaches full strength");
                    float before = ThermalVisionPostProcessor.effectStrength();
                    ClientThermalVisionState.set(false, 1000, 0);
                    require(ThermalVisionPostProcessor.effectStrength() == before, "state packet does not advance fade");
                    ClientThermalVisionState.set(true, 1000, 0);
                    screenshot("active-first-person.png");
                    mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                }
                case 3 -> {
                    screenshot("active-third-person.png");
                    pausedCharge = ClientThermalVisionState.remainingTicks();
                    pausedFade = ThermalVisionPostProcessor.effectStrength();
                    pauseFrames = 0;
                    mc.setScreen(new PauseScreen(true));
                }
                case 4 -> server(player -> {
                    ItemStack goggles = ThermalVisionAccess.getThermalHelmet(player);
                    ThermalImagerAccessoryItem.setThermalUseTicks(goggles, 600);
                    ItemStack battery = new ItemStack(ModItems.THERMAL_BATTERY.get(), 2);
                    player.setItemInHand(InteractionHand.MAIN_HAND, battery);
                    player.setGameMode(GameType.SURVIVAL);
                    battery.getItem().use(player.level(), player, InteractionHand.MAIN_HAND);
                    require(battery.getCount() == 1, "battery consumed once in survival");
                    require(ThermalImagerAccessoryItem.getThermalUseTicks(goggles) == 1200, "battery restores full charge");
                    ServerThermalVisionStates.onPlayerDestroyItem(new PlayerDestroyItemEvent(player,
                        new ItemStack(ModItems.THERMAL_GOGGLES.get()), InteractionHand.OFF_HAND));
                    require(ServerThermalVisionStates.get(player).active(), "spare goggles destruction preserves activation");
                });
                case 5 -> {
                    require(ClientThermalVisionState.remainingTicks() > 1120, "recharge immediately reaches client");
                    mc.options.hideGui = true;
                }
                case 6 -> {
                    screenshot("active-hidden-gui.png");
                    mc.options.hideGui = false;
                    mc.reloadResourcePacks();
                }
                case 7 -> {
                    screenshot("active-reloaded.png");
                    server(player -> {
                        player.setGameMode(GameType.SPECTATOR);
                        ServerThermalVisionStates.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
                        require(!ServerThermalVisionStates.get(player).active(), "spectator immediately disabled");
                        require(!ServerThermalVisionStates.toggle(player).active(), "spectator activation denied");
                        player.setGameMode(GameType.CREATIVE);
                        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                        require(!ServerThermalVisionStates.toggle(player).active(), "activation without goggles denied");
                        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.THERMAL_GOGGLES.get()));
                        ThermalImagerAccessoryItem.setThermalUseTicks(ThermalVisionAccess.getThermalHelmet(player), 1);
                        ServerThermalVisionStates.toggle(player);
                        ServerThermalVisionStates.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
                        require(!ServerThermalVisionStates.get(player).active(), "last charge tick disables activation");
                        require(ThermalVisionAccess.getRemainingUseTicks(player) == 0, "last charge tick stops at zero");
                    });
                }
                case 8 -> {
                    require(!ClientThermalVisionState.isActive(), "depletion reaches client");
                    require(ThermalVisionPostProcessor.effectStrength() < 0.001F, "deactivation fade completes");
                    for (var entity : mc.level.entitiesForRendering()) {
                        if (entity instanceof LivingEntity living && living != mc.player) {
                            living.setInvisible(true);
                            require(!ThermalSignatureResolver.shouldRender(living), "invisible entities excluded");
                            living.setInvisible(false);
                        }
                    }
                    screenshot("inactive.png");
                    LOGGER.info("AUDIT COMPLETE: all runtime assertions passed");
                    mc.stop();
                }
                default -> { }
            }
        } catch (Throwable exception) {
            failed = true;
            LOGGER.error("AUDIT FAILED", exception);
            mc.stop();
        }
    }

    @SubscribeEvent
    public static void renderGui(RenderGuiEvent.Post event) {
        if (!Boolean.getBoolean("thermal_vision.audit") || failed || phase != 4) return;
        Minecraft mc = Minecraft.getInstance();
        if (!mc.isPaused()) return;
        try {
            if (pauseFrames == 0) {
                pausedCharge = ClientThermalVisionState.remainingTicks();
                pausedFade = ThermalVisionPostProcessor.effectStrength();
            }
            if (++pauseFrames == 60) {
                require(ClientThermalVisionState.remainingTicks() == pausedCharge, "paused charge remains unchanged");
                require(ThermalVisionPostProcessor.effectStrength() == pausedFade, "paused fade remains unchanged");
                mc.setScreen(null);
            }
        } catch (Throwable exception) {
            failed = true;
            LOGGER.error("AUDIT FAILED", exception);
            mc.stop();
        }
    }

    private static void screenshot(String name) throws Exception {
        Minecraft mc = Minecraft.getInstance();
        Path output = Path.of(mc.gameDirectory.getAbsolutePath(), "audit-evidence");
        Files.createDirectories(output);
        try (NativeImage image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            image.writeToFile(output.resolve(name));
        }
        require(GL11.glGetError() == GL11.GL_NO_ERROR, name + ": no pending OpenGL error");
    }
}
