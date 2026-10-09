package com.yourname.thermalvision.client.model;

import com.yourname.thermalvision.ThermalVision;
import com.yourname.thermalvision.network.GogglesVisualS2CPacket;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ThermalVision.MOD_ID, value = Dist.CLIENT)
public final class ClientGogglesVisuals {
    private static final Map<UUID, Visual> STATES = new HashMap<>();
    private static ClientLevel level;

    private static final class Visual {
        private int entityId;
        private boolean active;
        private int renderIndex;
        private final ThermalGogglesMotion motion = new ThermalGogglesMotion();
    }

    private ClientGogglesVisuals() {}

    private static void checkLevel() {
        ClientLevel current = Minecraft.getInstance().level;
        if (current != level) {
            STATES.clear();
            level = current;
        }
    }

    public static double now() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null ? 0.0 : (minecraft.level.getGameTime() + minecraft.getFrameTime()) / 20.0;
    }

    public static void accept(GogglesVisualS2CPacket packet) {
        checkLevel();
        if (level == null) return;
        Visual visual = STATES.get(packet.wearer());
        boolean fresh = visual == null || visual.entityId != packet.entityId();
        if (fresh) {
            visual = new Visual();
            visual.entityId = packet.entityId();
            STATES.put(packet.wearer(), visual);
            visual.motion.snap(false, now());
        }
        boolean active = packet.equipped() && packet.active();
        if (fresh && packet.snapshot()) visual.motion.snap(active, now());
        else visual.motion.setActive(active, now());
        visual.active = active;
        visual.renderIndex = packet.curios() ? packet.renderIndex() : -1;
    }

    private static Visual get(Entity wearer) {
        Visual visual = STATES.get(wearer.getUUID());
        return visual != null && visual.entityId == wearer.getId() ? visual : null;
    }

    public static boolean isActive(Entity wearer) {
        Visual visual = get(wearer);
        return visual != null && visual.active;
    }

    public static float angle(Entity wearer) {
        Visual visual = get(wearer);
        return visual == null ? ThermalGogglesMotion.RAISED : visual.motion.sample(now());
    }

    public static int renderIndex(Entity wearer) {
        Visual visual = get(wearer);
        return visual == null ? -1 : visual.renderIndex;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) checkLevel();
    }

    @SubscribeEvent
    public static void leave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide) {
            Visual visual = get(event.getEntity());
            if (visual != null) STATES.remove(event.getEntity().getUUID());
        }
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        STATES.clear();
        level = null;
    }
}
