package net.fabricmc.fabric.api.networking.v1;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

public interface PacketSender {
    void sendPacket(ResourceLocation channelName, PacketBuffer packetBuffer);
}
