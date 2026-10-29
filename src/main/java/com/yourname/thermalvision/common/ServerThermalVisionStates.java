// src/main/java/com/yourname/thermalvision/common/ServerThermalVisionStates.java
package com.yourname.thermalvision.common;

import com.yourname.thermalvision.ThermalVision;
import com.yourname.thermalvision.item.ThermalImagerAccessoryItem;
import com.yourname.thermalvision.network.ModNetworking;
import com.yourname.thermalvision.network.GogglesVisualS2CPacket;
import com.yourname.thermalvision.compat.ThermalEquipment;
import com.yourname.thermalvision.registry.ModItems;
import com.yourname.thermalvision.registry.ModSounds;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ThermalVision.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ServerThermalVisionStates {
    private static final Map<UUID, ThermalVisionState> STATES = new HashMap<>();
    private static final Map<UUID, Integer> LAST_TOGGLE_TICKS = new HashMap<>();
    private static final Map<UUID, ItemStack> ACTIVE_GOGGLES = new HashMap<>();
    private static final Map<UUID, GogglesVisualS2CPacket> LAST_VISUALS = new HashMap<>();

    private ServerThermalVisionStates() {
    }

    public static ThermalVisionState get(ServerPlayer player) {
        ThermalVisionState state = STATES.get(player.getUUID());
        return state != null
            ? state
            : new ThermalVisionState(false, ThermalVisionAccess.getRemainingUseTicks(player), 0);
    }

    public static ThermalVisionState toggle(ServerPlayer player) {
        ThermalVisionState state = get(player);
        int remainingTicks = ThermalVisionAccess.getRemainingUseTicks(player);

        if (state.active()) {
            return forceDisable(player, ThermalVisionConfig.COOLDOWN_TICKS);
        }

        if (state.cooldownTicks() > 0) {
            return set(
                player,
                new ThermalVisionState(false, remainingTicks, state.cooldownTicks())
            );
        }

        if (!ThermalVisionAccess.canUseThermalVision(player)) {
            return forceDisable(player, 0);
        }

        return set(player, new ThermalVisionState(true, remainingTicks, 0));
    }

    public static ThermalVisionState forceDisable(ServerPlayer player, int cooldownTicks) {
        return set(
            player,
            new ThermalVisionState(
                false,
                ThermalVisionAccess.getRemainingUseTicks(player),
                cooldownTicks
            )
        );
    }

    /** Independent of the gameplay cooldown: reject packet floods before replying or changing state. */
    public static boolean acceptToggle(ServerPlayer player) {
        int tick = player.server.getTickCount();
        Integer previous = LAST_TOGGLE_TICKS.get(player.getUUID());
        if (previous != null && tick >= previous && tick - previous < 4) {
            return false;
        }
        LAST_TOGGLE_TICKS.put(player.getUUID(), tick);
        return true;
    }

    private static ThermalVisionState set(ServerPlayer player, ThermalVisionState state) {
        ItemStack goggles = ThermalVisionAccess.getWorkingGoggles(player);
        ItemStack bound = ACTIVE_GOGGLES.get(player.getUUID());
        if (state.active() && (goggles.isEmpty() || (bound != null && bound != goggles))) {
            // Replacing an active pair must not activate or drain a different pair.
            state = new ThermalVisionState(false, ThermalVisionAccess.getRemainingUseTicks(player),
                ThermalVisionConfig.COOLDOWN_TICKS);
        }
        if (state.active()) ACTIVE_GOGGLES.put(player.getUUID(), goggles);
        else ACTIVE_GOGGLES.remove(player.getUUID());
        if (!state.active() && state.cooldownTicks() <= 0) {
            STATES.remove(player.getUUID());
        } else {
            STATES.put(player.getUUID(), state);
        }

        publishVisual(player);
        return state;
    }

    private static ThermalVisionState refreshState(ServerPlayer player) {
        ThermalVisionState state = STATES.get(player.getUUID());
        int remainingTicks = ThermalVisionAccess.getRemainingUseTicks(player);

        if (state == null) {
            return new ThermalVisionState(false, remainingTicks, 0);
        }

        return set(
            player,
            new ThermalVisionState(
                state.active() && ThermalVisionAccess.canUseThermalVision(player)
                    && ACTIVE_GOGGLES.get(player.getUUID()) == ThermalVisionAccess.getWorkingGoggles(player),
                remainingTicks,
                state.cooldownTicks()
            )
        );
    }

    public static void syncState(ServerPlayer player) {
        ModNetworking.sendToPlayer(player, refreshState(player));
        ModNetworking.sendVisualTo(player, visual(player, true));
    }

    private static GogglesVisualS2CPacket visual(ServerPlayer player, boolean snapshot) {
        ItemStack goggles = ThermalVisionAccess.getWorkingGoggles(player);
        ThermalVisionState state = STATES.get(player.getUUID());
        boolean active = state != null && state.active() && player.isAlive() && !player.isSpectator()
            && !goggles.isEmpty() && ACTIVE_GOGGLES.get(player.getUUID()) == goggles;
        return new GogglesVisualS2CPacket(player.getId(), player.getUUID(), !goggles.isEmpty(),
            active, ThermalEquipment.usesCurios(), ThermalEquipment.getRenderIndex(player), snapshot);
    }

    private static void publishVisual(ServerPlayer player) {
        GogglesVisualS2CPacket next = visual(player, false);
        GogglesVisualS2CPacket previous = LAST_VISUALS.put(player.getUUID(), next);
        if (!next.equals(previous)) ModNetworking.sendVisual(player, next);
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer observer
            && event.getTarget() instanceof ServerPlayer wearer) {
            ModNetworking.sendVisualTo(observer, visual(wearer, true));
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }

        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        ThermalEquipment.migrateLegacyHelmet(player);
        ThermalVisionState state = STATES.get(player.getUUID());
        publishVisual(player);
        if (state == null) {
            return;
        }

        if (!state.active()) {
            tickCooldown(player, state);
            return;
        }

        ItemStack goggles = ThermalVisionAccess.getWorkingGoggles(player);
        if (!player.isAlive() || player.isSpectator() || goggles.isEmpty()
            || ACTIVE_GOGGLES.get(player.getUUID()) != goggles) {
            ThermalVisionState disabled =
                forceDisable(player, ThermalVisionConfig.COOLDOWN_TICKS);
            ModNetworking.sendToPlayer(player, disabled);
            return;
        }

        int remainingTicks = ThermalImagerAccessoryItem.consumeThermalUseTick(goggles);
        int cooldownTicks = remainingTicks > 0 ? 0 : ThermalVisionConfig.COOLDOWN_TICKS;
        ThermalVisionState nextState =
            new ThermalVisionState(remainingTicks > 0, remainingTicks, cooldownTicks);
        set(player, nextState);

        if (!nextState.active()) {
            playGogglesBreakSound(player);
            player.displayClientMessage(
                Component.translatable("message.thermal_vision.goggles_depleted"),
                true
            );
            ModNetworking.sendToPlayer(player, nextState);
            return;
        }

        if (player.tickCount % 20 == 0) {
            ModNetworking.sendToPlayer(player, nextState);
        }
    }

    private static void tickCooldown(ServerPlayer player, ThermalVisionState state) {
        if (state.cooldownTicks() <= 0) {
            set(player, state);
            return;
        }

        int nextCooldownTicks = state.cooldownTicks() - 1;
        ThermalVisionState nextState = set(
            player,
            new ThermalVisionState(
                false,
                ThermalVisionAccess.getRemainingUseTicks(player),
                nextCooldownTicks
            )
        );

        if (nextCooldownTicks == 0 || player.tickCount % 20 == 0) {
            ModNetworking.sendToPlayer(player, nextState);
        }
    }

    @SubscribeEvent
    public static void onPlayerDestroyItem(PlayerDestroyItemEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (!ModItems.isGoggles(event.getOriginal())) {
            return;
        }

        playGogglesBreakSound(player);
        // A spare pair breaking in a hand must not disable the equipped goggles.
        // Armor destruction can notify before the slot is cleared; the server tick handles that case.
        if (ThermalVisionAccess.getWorkingGoggles(player).isEmpty()) {
            ModNetworking.sendToPlayer(player, forceDisable(player, ThermalVisionConfig.COOLDOWN_TICKS));
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ThermalEquipment.migrateLegacyHelmet(player);
            syncState(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ThermalVisionState disabled = forceDisable(player, 0);
            ModNetworking.sendToPlayer(player, disabled);
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            syncState(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        STATES.remove(event.getEntity().getUUID());
        LAST_TOGGLE_TICKS.remove(event.getEntity().getUUID());
        ACTIVE_GOGGLES.remove(event.getEntity().getUUID());
        LAST_VISUALS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        STATES.clear();
        LAST_TOGGLE_TICKS.clear();
        ACTIVE_GOGGLES.clear();
        LAST_VISUALS.clear();
    }

    private static void playGogglesBreakSound(ServerPlayer player) {
        player.level().playSound(
            null,
            player.getX(),
            player.getY() + player.getEyeHeight() * 0.75D,
            player.getZ(),
            ModSounds.THERMAL_GOGGLES_BREAK.get(),
            SoundSource.PLAYERS,
            ThermalVisionConfig.BREAK_SOUND_VOLUME,
            ThermalVisionConfig.BREAK_SOUND_PITCH
        );
    }
}
