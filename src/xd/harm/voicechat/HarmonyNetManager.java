package xd.harm.voicechat;

import de.maxhenkel.voicechat.Voicechat;
import de.maxhenkel.voicechat.net.Channel;
import de.maxhenkel.voicechat.net.ClientServerChannel;
import de.maxhenkel.voicechat.net.ClientServerNetManager;
import de.maxhenkel.voicechat.net.Packet;
import de.maxhenkel.voicechat.net.RequestSecretPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CCustomPayloadPacket;
import net.minecraft.network.play.server.SCustomPayloadPlayPacket;
import net.minecraft.util.ResourceLocation;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class HarmonyNetManager extends ClientServerNetManager {
    private static final ResourceLocation REGISTER_CHANNEL = new ResourceLocation("minecraft", "register");

    private final Map<ResourceLocation, ClientPacketHandler<?>> clientReceivers = new ConcurrentHashMap<>();
    private final Set<ResourceLocation> channels = ConcurrentHashMap.newKeySet();

    @Override
    public <T extends Packet<T>> Channel<T> registerReceiver(Class<T> packetClass, boolean clientbound, boolean serverbound) {
        ClientServerChannel<T> channel = new ClientServerChannel<>();

        try {
            T packet = packetClass.getDeclaredConstructor().newInstance();
            channels.add(packet.getIdentifier());

            if (clientbound) {
                clientReceivers.put(packet.getIdentifier(), new ClientPacketHandler<>(packetClass, channel));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to register voice chat packet " + packetClass.getName(), e);
        }

        return channel;
    }

    public void sendChannelRegistration() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientPlayNetHandler connection = minecraft.getConnection();

        if (connection == null || connection.getWorld() == null || channels.isEmpty()) {
            return;
        }

        String payload = channels.stream()
                .map(ResourceLocation::toString)
                .sorted()
                .collect(Collectors.joining("\u0000"));
        PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
        buffer.writeBytes(payload.getBytes(StandardCharsets.UTF_8));
        connection.sendPacket(new CCustomPayloadPacket(REGISTER_CHANNEL, buffer));
        System.out.println("[voicechat] Registered plugin channels: " + payload.replace('\u0000', ','));
    }

    public void requestSecret(String reason) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientPlayNetHandler connection = minecraft.getConnection();

        if (connection == null || connection.getWorld() == null) {
            return;
        }

        System.out.println("[voicechat] Sending extra secret request (" + reason + ")");
        ClientServerNetManager.sendToServer(new RequestSecretPacket(Voicechat.COMPATIBILITY_VERSION));
    }

    public boolean handleClientbound(SCustomPayloadPlayPacket vanillaPacket) {
        if (REGISTER_CHANNEL.equals(vanillaPacket.getChannelName())) {
            handleServerChannelRegistration(vanillaPacket.getBufferData());
            return false;
        }

        ClientPacketHandler<?> handler = clientReceivers.get(vanillaPacket.getChannelName());

        if (handler == null) {
            return false;
        }

        handler.handle(vanillaPacket.getBufferData());
        return true;
    }

    private void handleServerChannelRegistration(PacketBuffer buffer) {
        PacketBuffer copy = new PacketBuffer(buffer.copy());

        try {
            byte[] bytes = new byte[copy.readableBytes()];
            copy.readBytes(bytes);
            Set<String> serverChannels = Arrays.stream(new String(bytes, StandardCharsets.UTF_8).split("\u0000"))
                    .filter(channel -> !channel.isEmpty())
                    .collect(Collectors.toSet());

            if (serverChannels.isEmpty()) {
                return;
            }

            System.out.println("[voicechat] Server plugin channels: " + serverChannels.stream().sorted().collect(Collectors.joining(",")));

            boolean hasVoicechat = serverChannels.stream().anyMatch(channel -> channel.startsWith("voicechat:"));

            if (hasVoicechat) {
                sendChannelRegistration();
                requestSecret("server registered voicechat channels");
            } else {
                System.out.println("[voicechat] Server did not register Simple Voice Chat channels");
            }
        } finally {
            copy.release();
        }
    }

    private static class ClientPacketHandler<T extends Packet<T>> {
        private final Class<T> packetClass;
        private final ClientServerChannel<T> channel;

        private ClientPacketHandler(Class<T> packetClass, ClientServerChannel<T> channel) {
            this.packetClass = packetClass;
            this.channel = channel;
        }

        private void handle(PacketBuffer buffer) {
            try {
                T packet = packetClass.getDeclaredConstructor().newInstance().fromBytes(buffer);
                Minecraft minecraft = Minecraft.getInstance();
                ClientPlayNetHandler connection = minecraft.getConnection();

                if (connection != null) {
                    channel.onClientPacket(minecraft, connection, packet);
                }
            } catch (Exception e) {
                throw new IllegalStateException("Failed to handle voice chat packet " + packetClass.getName(), e);
            }
        }
    }
}
