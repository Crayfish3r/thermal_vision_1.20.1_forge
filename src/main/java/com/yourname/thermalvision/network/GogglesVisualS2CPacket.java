package com.yourname.thermalvision.network;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Public pose state only. Charge and cooldown still go exclusively to the owner. */
public record GogglesVisualS2CPacket(
    int entityId, UUID wearer, boolean equipped, boolean active, boolean curios,
    int renderIndex, boolean snapshot
) {
    public static void encode(GogglesVisualS2CPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.entityId);
        buffer.writeUUID(packet.wearer);
        buffer.writeBoolean(packet.equipped);
        buffer.writeBoolean(packet.active);
        buffer.writeBoolean(packet.curios);
        buffer.writeVarInt(packet.renderIndex);
        buffer.writeBoolean(packet.snapshot);
    }

    public static GogglesVisualS2CPacket decode(FriendlyByteBuf buffer) {
        return new GogglesVisualS2CPacket(buffer.readVarInt(), buffer.readUUID(),
            buffer.readBoolean(), buffer.readBoolean(), buffer.readBoolean(),
            buffer.readVarInt(), buffer.readBoolean());
    }

    public static void handle(GogglesVisualS2CPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
            () -> () -> com.yourname.thermalvision.client.model.ClientGogglesVisuals.accept(packet)));
        context.setPacketHandled(true);
    }
}
