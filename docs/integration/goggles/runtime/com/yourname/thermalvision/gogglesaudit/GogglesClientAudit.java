package com.yourname.thermalvision.gogglesaudit;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.yourname.thermalvision.ThermalVision;
import com.yourname.thermalvision.client.ClientThermalVisionState;
import com.yourname.thermalvision.client.ThermalVisionPostProcessor;
import com.yourname.thermalvision.client.model.ClientGogglesVisuals;
import com.yourname.thermalvision.client.model.ThermalGogglesClient;
import com.yourname.thermalvision.client.model.ThermalGogglesModel;
import com.yourname.thermalvision.compat.ThermalEquipment;
import com.yourname.thermalvision.common.ServerThermalVisionStates;
import com.yourname.thermalvision.network.ModNetworking;
import com.yourname.thermalvision.network.ToggleThermalVisionC2SPacket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL11;

/** Dev-only game/API audit. It creates an isolated world and captures real game frames. */
@Mod.EventBusSubscriber(modid=ThermalVision.MOD_ID,value=Dist.CLIENT)
public final class GogglesClientAudit {
    private static boolean started,failed;
    private static int tick,phase;
    private static CompletableFuture<Void> work;
    private static ThermalGogglesModel beforeReload;
    private static String world;
    private static boolean capture;
    private static int frame;
    private static double lastFrame;
    private static final StringBuilder frameTimes=new StringBuilder("frame,game_seconds,angle_degrees,active\n");
    private static final java.util.List<CompletableFuture<Void>> frameWrites=new java.util.ArrayList<>();
    private static ServerPlayer remote;
    private static net.minecraft.world.entity.animal.Cow glassCow;
    private static final java.util.UUID REMOTE_ID=java.util.UUID.fromString("bc443c6a-edc6-48c6-ad22-e9a0e53985ab");
    private static net.minecraft.client.player.AbstractClientPlayer remoteClient() {
        return Minecraft.getInstance().level.players().stream().filter(p->p.getUUID().equals(REMOTE_ID)).findFirst().orElseThrow(()->new AssertionError("tracked remote player missing"));
    }
    @SubscribeEvent public static void tickRemote(TickEvent.ServerTickEvent event) {
        // FakePlayer intentionally skips normal player ticking. Drive the real inherited
        // doTick path so vanilla equipment and Curios send their own inventory packets.
        if(Boolean.getBoolean("thermal_vision.gogglesAudit") && event.phase==TickEvent.Phase.END
            && remote!=null && !remote.isRemoved()) remote.doTick();
    }
    private static final class ModelPreview extends Screen {
        private ModelPreview() { super(Component.literal("Goggles runtime model preview")); }
        @Override public boolean isPauseScreen() { return false; }
        @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partial) {
            // Keep every recorded preview frame free of tutorial/advancement popups.
            Minecraft.getInstance().getToasts().clear();
            graphics.fill(0,0,width,height,0xFF27313D);
            InventoryScreen.renderEntityInInventoryFollowsMouse(graphics,width/2,height+30,
                (int)(height*.45),0,0,Minecraft.getInstance().player);
            graphics.drawCenteredString(font,"Actual Forge / Curios renderer",width/2,10,0xFFFFFF);
        }
    }
    @SubscribeEvent public static void frames(TickEvent.RenderTickEvent event) {
        if(!capture || event.phase!=TickEvent.Phase.END) return;
        double now=ClientGogglesVisuals.now();
        if(now-lastFrame<.02) return;
        lastFrame=now;
        try {
            frameTimes.append(frame).append(',').append(now).append(',')
                .append(Math.toDegrees(ClientGogglesVisuals.angle(Minecraft.getInstance().player))).append(',')
                .append(ClientGogglesVisuals.isActive(Minecraft.getInstance().player)).append('\n');
            var mc=Minecraft.getInstance();
            Path path=Path.of(mc.gameDirectory.getAbsolutePath(),"goggles-evidence",String.format("motion-%03d.png",frame++));
            NativeImage captured=Screenshot.takeScreenshot(mc.getMainRenderTarget());
            frameWrites.add(CompletableFuture.runAsync(()->{
                try(captured) { captured.writeToFile(path); }
                catch(Exception error) { throw new java.util.concurrent.CompletionException(error); }
            }));
        }
        catch(Exception error) { failed=true;capture=false;LogUtils.getLogger().error("GOGGLES CLIENT AUDIT FAILED",error);Minecraft.getInstance().stop(); }
    }
    private static void server(Consumer<ServerPlayer> action) {
        var mc=Minecraft.getInstance();
        work=CompletableFuture.runAsync(()->action.accept(mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID())),mc.getSingleplayerServer());
    }
    private static void screenshot(String name) throws Exception {
        var mc=Minecraft.getInstance();
        Path directory=Path.of(mc.gameDirectory.getAbsolutePath(),"goggles-evidence");
        Files.createDirectories(directory);
        try(NativeImage image=Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(directory.resolve(name)); }
        GogglesChecks.require(GL11.glGetError()==GL11.GL_NO_ERROR,name+": no OpenGL error");
    }
    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (!Boolean.getBoolean("thermal_vision.gogglesAudit") || failed || event.phase!=TickEvent.Phase.END) return;
        Minecraft mc=Minecraft.getInstance();
        try {
            if(!started && mc.getOverlay()==null && (mc.screen instanceof TitleScreen || mc.screen instanceof AccessibilityOnboardingScreen)) {
                started=true;world="goggles-"+System.currentTimeMillis();
                mc.options.pauseOnLostFocus=false;mc.options.fov().set(35);
                mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
                mc.getToasts().clear();
                mc.createWorldOpenFlows().createFreshLevel(world,
                    new LevelSettings("Goggles integration audit",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),
                    new WorldOptions(82314,false,false),r->r.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).createWorldDimensions());
                return;
            }
            if(started && phase==29 && mc.screen instanceof TitleScreen && mc.getOverlay()==null) {
                mc.createWorldOpenFlows().loadLevel(new TitleScreen(),world);
                phase=30;tick=0;return;
            }
            if(mc.player==null || mc.level==null || mc.isPaused() || mc.getOverlay()!=null) return;
            if(work!=null) { if(!work.isDone()) return;work.join();work=null; }
            if(++tick<((phase==18 || phase==19)?4:30)) return;tick=0;
            switch(phase++) {
                case 0 -> server(player->{
                    player.teleportTo(.5,100,.5);
                    for(int x=-5;x<=5;x++) for(int z=-5;z<=5;z++) player.serverLevel().setBlockAndUpdate(new BlockPos(x,99,z),Blocks.STONE.defaultBlockState());
                    player.serverLevel().setBlockAndUpdate(new BlockPos(2,100,3),Blocks.GLOWSTONE.defaultBlockState());
                    GogglesChecks.run(player);
                });
                case 1 -> {
                    var model=ThermalGogglesClient.model();
                    var hinge=model.head.getChild("goggles_hinge");
                    GogglesChecks.require(Math.abs(hinge.y+7.25)<1e-6 && Math.abs(hinge.z+5.6)<1e-6,"baked ModelPart pivot matches input project");
                    mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                    mc.player.setYRot(0);mc.player.setXRot(0);
                }
                case 2 -> { screenshot("01-raised-front.png");ModNetworking.sendToServer(new ToggleThermalVisionC2SPacket()); }
                case 3 -> {
                    GogglesChecks.require(ClientThermalVisionState.isActive() && ClientGogglesVisuals.isActive(mc.player),"owner thermal state and confirmed visual state activate");
                    GogglesChecks.require(Math.abs(ClientGogglesVisuals.angle(mc.player))<1e-6,"lowering finishes in the running client");
                    GogglesChecks.require(ThermalVisionPostProcessor.effectStrength()>.99,"existing thermal grading reaches full fade");
                    screenshot("02-active-front.png");
                    mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);mc.player.setXRot(25);mc.options.keyShift.setDown(true);
                }
                case 4 -> { screenshot("03-crouched-back.png");mc.options.keyShift.setDown(false);mc.options.setCameraType(CameraType.FIRST_PERSON); }
                case 5 -> { screenshot("04-first-person.png");beforeReload=ThermalGogglesClient.model();work=mc.reloadResourcePacks(); }
                case 6 -> {
                    GogglesChecks.require(beforeReload!=ThermalGogglesClient.model(),"resource reload recreates baked goggles model");
                    mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
                    screenshot("05-resource-reload.png");
                    mc.getWindow().setWindowed(1024,720);
                }
                case 7 -> { screenshot("06-resized.png");mc.getWindow().toggleFullScreen(); }
                case 8 -> { screenshot("07-fullscreen.png");mc.getWindow().toggleFullScreen();mc.options.hideGui=true; }
                case 9 -> { screenshot("08-hidden-gui.png");mc.options.hideGui=false;ModNetworking.sendToServer(new ToggleThermalVisionC2SPacket()); }
                case 10 -> {
                    GogglesChecks.require(!ClientThermalVisionState.isActive() && !ClientGogglesVisuals.isActive(mc.player),"confirmed deactivation updates both visual and thermal state");
                    GogglesChecks.require(ThermalVisionPostProcessor.effectStrength()<.001,"existing deactivation fade completes");
                    screenshot("09-raised-after-off.png");
                    if(ThermalEquipment.usesCurios()) server(player->CuriosChecks.visibility(player,false));
                }
                case 11 -> {
                    if(ThermalEquipment.usesCurios()) {
                        GogglesChecks.require(ClientGogglesVisuals.renderIndex(mc.player)==-1,"hidden Curios accessory reaches client");
                        screenshot("10-curios-hidden.png");server(player->CuriosChecks.visibility(player,true));
                    }
                }
                case 12 -> {
                    if(ThermalEquipment.usesCurios()) GogglesChecks.require(ClientGogglesVisuals.renderIndex(mc.player)==0,"Curios visibility restored");
                    server(player->{
                        var nether=player.server.getLevel(Level.NETHER);
                        player.teleportTo(nether,.5,100,.5,0,0);
                    });
                }
                case 13 -> {
                    GogglesChecks.require(mc.level.dimension().equals(Level.NETHER),"actual dimension transfer reaches client");
                    GogglesChecks.require(!ClientGogglesVisuals.isActive(mc.player),"dimension snapshot remains inactive");
                    screenshot("11-nether.png");
                    server(player-> {
                        player.level().getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(true,player.server);
                        player.setGameMode(GameType.SURVIVAL);
                        player.hurt(player.damageSources().genericKill(),Float.MAX_VALUE);
                    });
                }
                case 14 -> { GogglesChecks.require(!mc.player.isAlive(),"actual player death");mc.player.respawn(); }
                case 15 -> {
                    GogglesChecks.require(mc.player.isAlive() && !ClientGogglesVisuals.isActive(mc.player),"actual respawn clears visual activation");
                    screenshot("12-respawn.png");
                    server(player->{player.setGameMode(GameType.CREATIVE);player.setHealth(20);player.getInventory().clearContent();GogglesChecks.equip(player);ServerThermalVisionStates.syncState(player);});
                    mc.setScreen(new ModelPreview());
                }
                case 16 -> { screenshot("13-model-raised.png");capture=true;frame=0;lastFrame=0;ModNetworking.sendToServer(new ToggleThermalVisionC2SPacket()); }
                case 17 -> { screenshot("14-model-lowered.png");ModNetworking.sendToServer(new ToggleThermalVisionC2SPacket()); }
                case 18 -> ModNetworking.sendToServer(new ToggleThermalVisionC2SPacket());
                case 19 -> ModNetworking.sendToServer(new ToggleThermalVisionC2SPacket());
                case 20 -> {
                    capture=false;
                    work=CompletableFuture.allOf(frameWrites.toArray(CompletableFuture[]::new));
                    Files.writeString(Path.of(mc.gameDirectory.getAbsolutePath(),"goggles-evidence","motion.csv"),frameTimes);
                    GogglesChecks.require(ClientGogglesVisuals.isActive(mc.player)==ClientThermalVisionState.isActive(),"rapid toggles respect confirmed server state and packet rate limit");
                    if(ClientGogglesVisuals.isActive(mc.player)) ModNetworking.sendToServer(new ToggleThermalVisionC2SPacket());
                    mc.setScreen(null);
                }
                case 21 -> {
                    GogglesChecks.require(!ClientGogglesVisuals.isActive(mc.player)
                        && Math.abs(ClientGogglesVisuals.angle(mc.player)-com.yourname.thermalvision.client.model.ThermalGogglesMotion.RAISED)<1e-6,"settles at confirmed raised endpoint after rapid toggles");
                    screenshot("15-after-rapid-toggle.png");
                    mc.options.setCameraType(CameraType.FIRST_PERSON);
                    server(player->{
                        player.teleportTo(.5,100,4.5);player.connection.teleport(.5,100,4.5,180,0);
                        remote=net.minecraftforge.common.util.FakePlayerFactory.get(player.serverLevel(),new com.mojang.authlib.GameProfile(REMOTE_ID,"GogglesRemote"));
                        remote.setGameMode(GameType.CREATIVE);remote.setInvulnerable(true);
                        remote.moveTo(.5,100,.5,0,0);remote.yBodyRot=0;remote.yHeadRot=0;
                        player.connection.send(net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(java.util.List.of(remote)));
                        player.serverLevel().addNewPlayer(remote);
                        GogglesChecks.equip(remote);
                        if(ThermalEquipment.usesCurios()) remote.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_HELMET));
                        ServerThermalVisionStates.syncState(remote);
                    });
                }
                case 22 -> {
                    GogglesChecks.require(!com.yourname.thermalvision.common.ThermalVisionAccess.getWorkingGoggles(remoteClient()).isEmpty(),"remote equipment inventory reaches observer through native packets");
                    GogglesChecks.require(!ClientGogglesVisuals.isActive(remoteClient()),"remote equipped player initially raised");
                    screenshot("17-remote-raised.png");
                    server(player->{ServerThermalVisionStates.toggle(remote);ServerThermalVisionStates.syncState(remote);});
                }
                case 23 -> {
                    GogglesChecks.require(ClientGogglesVisuals.isActive(remoteClient()) && Math.abs(ClientGogglesVisuals.angle(remoteClient()))<1e-6
                        && !ClientThermalVisionState.isActive(),"remote activation reaches observer without enabling observer thermal effect");
                    screenshot("18-remote-lowered.png");
                    server(player->player.connection.teleport(512.5,100,512.5,180,0));
                }
                case 24 -> {
                    GogglesChecks.require(mc.level.players().stream().noneMatch(p->p.getUUID().equals(REMOTE_ID)),"remote player leaves tracking range");
                    server(player->player.connection.teleport(.5,100,4.5,180,0));
                }
                case 25 -> {
                    GogglesChecks.require(ClientGogglesVisuals.isActive(remoteClient()) && Math.abs(ClientGogglesVisuals.angle(remoteClient()))<1e-6,"StartTracking snapshot restores already active remote goggles");
                    screenshot("19-remote-retracked.png");
                    server(player->{
                        if(ThermalEquipment.usesCurios()) CuriosChecks.clear(remote);
                        else remote.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,net.minecraft.world.item.ItemStack.EMPTY);
                        GogglesChecks.tick(remote);
                    });
                }
                case 26 -> {
                    GogglesChecks.require(!ClientGogglesVisuals.isActive(remoteClient()),"remote removal disables visual state on observer");
                    screenshot("20-remote-removed.png");
                    server(player->{remote.discard();ServerThermalVisionStates.onPlayerLoggedOut(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(remote));player.connection.send(new net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket(java.util.List.of(REMOTE_ID)));});
                }
                case 27 -> GogglesChecks.require(mc.level.players().stream().noneMatch(p->p.getUUID().equals(REMOTE_ID)),"removed remote entity leaves client");
                case 28 -> { mc.level.disconnect();mc.clearLevel(new TitleScreen()); }
                case 30 -> {
                    GogglesChecks.require(!ClientGogglesVisuals.isActive(mc.player) && !ClientThermalVisionState.isActive(),"actual reconnect begins inactive");
                    screenshot("16-reconnected.png");
                    server(player->{
                        glassCow=net.minecraft.world.entity.EntityType.COW.create(player.serverLevel());
                        glassCow.setNoAi(true);glassCow.moveTo(.5,100,.5,0,0);player.serverLevel().addFreshEntity(glassCow);
                        for(int x=-1;x<=1;x++) for(int y=100;y<=102;y++) player.serverLevel().setBlockAndUpdate(new BlockPos(x,y,2),Blocks.GLASS.defaultBlockState());
                        ServerThermalVisionStates.toggle(player);ServerThermalVisionStates.syncState(player);
                    });
                }
                case 31 -> {
                    GogglesChecks.require(ClientThermalVisionState.isActive(),"thermal effect activates in scene with translucent blocks");
                    screenshot("21-thermal-through-glass.png");server(player->glassCow.setInvisible(true));
                }
                case 32 -> {
                    var cow=(net.minecraft.world.entity.LivingEntity)mc.level.getEntity(glassCow.getId());
                    GogglesChecks.require(cow!=null && cow.isInvisible()
                        && !com.yourname.thermalvision.client.render.ThermalSignatureResolver.shouldRender(cow),"server-synchronized invisible living entity is excluded from heat mask");
                    screenshot("22-invisible-through-glass.png");server(player->player.setGameMode(GameType.SPECTATOR));
                }
                case 33 -> {
                    GogglesChecks.require(!ClientThermalVisionState.isActive() && !ClientGogglesVisuals.isActive(mc.player)
                        && ThermalVisionPostProcessor.effectStrength()<.001,"actual spectator transition disables goggles and thermal effect");
                    screenshot("23-spectator.png");
                    LogUtils.getLogger().info("GOGGLES CLIENT AUDIT COMPLETE");mc.stop();
                }
                default -> {}
            }
        } catch(Throwable error) {
            failed=true;LogUtils.getLogger().error("GOGGLES CLIENT AUDIT FAILED",error);mc.stop();
        }
    }
}
