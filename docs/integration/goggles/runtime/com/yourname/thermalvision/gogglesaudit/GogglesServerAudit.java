package com.yourname.thermalvision.gogglesaudit;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import com.yourname.thermalvision.ThermalVision;
import java.util.UUID;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber(modid=ThermalVision.MOD_ID)
public final class GogglesServerAudit {
    private static boolean checked;
    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (!Boolean.getBoolean("thermal_vision.gogglesAudit") || checked || event.phase!=TickEvent.Phase.END) return;
        var server=ServerLifecycleHooks.getCurrentServer();
        if (server==null || !server.isDedicatedServer() || server.getTickCount()<40) return;
        checked=true;
        try {
            var player=FakePlayerFactory.get(server.overworld(),new GameProfile(UUID.fromString("55da58d3-3f92-43f8-b82b-2a6fe94ab2f4"),"GogglesAudit"));
            server.overworld().addNewPlayer(player);
            GogglesChecks.run(player);
            player.discard();
            LogUtils.getLogger().info("GOGGLES SERVER AUDIT COMPLETE");
        } catch(Throwable error) {
            LogUtils.getLogger().error("GOGGLES SERVER AUDIT FAILED",error);
        } finally {
            server.halt(false);
        }
    }
}
