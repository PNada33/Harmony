package net.fabricmc.fabric.api.networking.v1;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ServerPlayNetworking {
    private static final Map<ResourceLocation, PlayChannelHandler> HANDLERS = new ConcurrentHashMap<>();

    private ServerPlayNetworking() {
    }

    public static boolean registerGlobalReceiver(ResourceLocation channelName, PlayChannelHandler channelHandler) {
        return HANDLERS.put(channelName, channelHandler) == null;
    }

    public static boolean unregisterGlobalReceiver(ResourceLocation channelName) {
        return HANDLERS.remove(channelName) != null;
    }

    public static void send(ServerPlayerEntity player, ResourceLocation channelName, PacketBuffer packetBuffer) {
    }

    public interface PlayChannelHandler {
        void receive(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetHandler handler, PacketBuffer buf, PacketSender responseSender);
    }
}
