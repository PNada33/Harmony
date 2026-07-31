package net.fabricmc.fabric.api.client.networking.v1;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientPlayNetworking {
    private static final Map<ResourceLocation, PlayChannelHandler> HANDLERS = new ConcurrentHashMap<>();
    private static final PacketSender NOOP_SENDER = (channelName, packetBuffer) -> {
    };

    private ClientPlayNetworking() {
    }

    public static boolean registerGlobalReceiver(ResourceLocation channelName, PlayChannelHandler channelHandler) {
        return HANDLERS.put(channelName, channelHandler) == null;
    }

    public static boolean unregisterGlobalReceiver(ResourceLocation channelName) {
        return HANDLERS.remove(channelName) != null;
    }

    public static void send(ResourceLocation channelName, PacketBuffer packetBuffer) {
    }

    public static void receive(ResourceLocation channelName, Minecraft client, ClientPlayNetHandler handler, PacketBuffer packetBuffer) {
        PlayChannelHandler channelHandler = HANDLERS.get(channelName);

        if (channelHandler != null) {
            channelHandler.receive(client, handler, packetBuffer, NOOP_SENDER);
        }
    }

    public interface PlayChannelHandler {
        void receive(Minecraft client, ClientPlayNetHandler handler, PacketBuffer buf, PacketSender responseSender);
    }
}
