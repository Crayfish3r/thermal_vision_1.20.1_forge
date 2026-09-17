// src/main/java/com/yourname/thermalvision/common/ServerThermalVisionStates.java
package com.yourname.thermalvision.common;

import com.yourname.thermalvision.ThermalVision;
import com.yourname.thermalvision.item.ThermalImagerAccessoryItem;
import com.yourname.thermalvision.network.ModNetworking;
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
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ThermalVision.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ServerThermalVisionStates {
    private static final Map<UUID, ThermalVisionState> STATES = new HashMap<>();

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

        if (remainingTicks <= 0) {
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

    private static ThermalVisionState set(ServerPlayer player, ThermalVisionState state) {
        if (!state.active() && state.cooldownTicks() <= 0) {
            STATES.remove(player.getUUID());
        } else {
            STATES.put(player.getUUID(), state);
        }

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
                state.active() && remainingTicks > 0,
                remainingTicks,
                state.cooldownTicks()
            )
        );
    }

    private static void syncState(ServerPlayer player) {
        ModNetworking.sendToPlayer(player, refreshState(player));
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }

        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        ThermalVisionState state = STATES.get(player.getUUID());
        if (state == null) {
            return;
        }

        if (!state.active()) {
            tickCooldown(player, state);
            return;
        }

        ItemStack goggles = ThermalVisionAccess.getThermalHelmet(player);
        if (goggles.isEmpty()) {
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

        if (!event.getOriginal().is(ModItems.THERMAL_GOGGLES.get())) {
            return;
        }

        playGogglesBreakSound(player);
        ThermalVisionState disabled = set(player, new ThermalVisionState(false, 0, 0));
        ModNetworking.sendToPlayer(player, disabled);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
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
