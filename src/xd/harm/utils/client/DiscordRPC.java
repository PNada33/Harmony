package xd.harm.utils.client;

import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.IPCListener;
import com.jagrosh.discordipc.entities.Packet;
import com.jagrosh.discordipc.entities.RichPresence;
import com.jagrosh.discordipc.exceptions.NoDiscordClientException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.screen.MultiplayerScreen;
import net.minecraft.entity.player.PlayerEntity;

import java.time.OffsetDateTime;

public class DiscordRPC {
    private static final Minecraft mc = Minecraft.getInstance();
    private static final IPCClient client = new IPCClient(1426261398236696708L);
    private static volatile boolean rpcInitialized = false;
    private static Thread rpcUpdateThread;

    public static synchronized void startRPC() {
        if (rpcInitialized) {
            return;
        }

        client.setListener(new IPCListener() {
            @Override
            public void onPacketReceived(IPCClient client, Packet packet) {
            }

            @Override
            public void onReady(IPCClient client) {
                rpcInitialized = true;
                startUpdateThread();
            }

            @Override
            public void onDisconnect(IPCClient client, Throwable t) {
                rpcInitialized = false;
                startReconnectThread();
            }

            @Override
            public void onError(IPCClient client, String error) {
            }
        });

        try {
            client.connect();
        } catch (NoDiscordClientException ignored) {
        } catch (Exception e) {
            startReconnectThread();
        }
    }

    private static void startUpdateThread() {
        if (client.getStatus() != com.jagrosh.discordipc.entities.pipe.PipeStatus.CONNECTED) {
            return;
        }

        rpcUpdateThread = new Thread(() -> {
            while (rpcInitialized && client.getStatus() == com.jagrosh.discordipc.entities.pipe.PipeStatus.CONNECTED) {
                try {
                    PlayerEntity player = mc.player;
                    String playerName = player != null ? player.getName().getString() : "Unknown";

                    String state;
                    if (mc.currentScreen instanceof MainMenuScreen) {
                        state = "В главном меню";
                    } else if (mc.currentScreen instanceof MultiplayerScreen) {
                        state = "Выбирает сервер";
                    } else if (mc.isSingleplayer()) {
                        state = "В одиночном мире";
                    } else if (mc.getCurrentServerData() != null) {
                        state = "Играет на " + mc.getCurrentServerData().serverIP;
                    } else {
                        state = "own: danta_mephedronov";
                    }

                    RichPresence.Builder builder = new RichPresence.Builder()
                            .setDetails("Build: v1.0.0 » Type: Freeᵇᵉᵗᵃ")
                            .setState(state)
                            .setStartTimestamp(OffsetDateTime.now())
                            .setLargeImage("logo", "dsc.gg/harmonyclient");

                    client.sendRichPresence(builder.build());
                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {
                    break;
                } catch (Exception ignored) {
                }
            }
        }, "Discord-RPC-Update");
        rpcUpdateThread.start();
    }

    private static void startReconnectThread() {
        new Thread(() -> {
            while (!rpcInitialized) {
                try {
                    Thread.sleep(10000);
                    client.connect();
                    rpcInitialized = true;
                    startUpdateThread();
                } catch (NoDiscordClientException | InterruptedException ignored) {
                } catch (Exception e) {
                }
            }
        }, "Discord-RPC-Reconnect").start();
    }

    public static synchronized void stopRPC() {
        if (rpcInitialized) {
            rpcInitialized = false;
            if (rpcUpdateThread != null) {
                rpcUpdateThread.interrupt();
                rpcUpdateThread = null;
            }
            client.close();
        }
    }
}
