package xd.harm.utils.client;

import com.jagrosh.discordipc.IPCClient;
import com.jagrosh.discordipc.IPCListener;
import com.jagrosh.discordipc.entities.RichPresence;
import com.jagrosh.discordipc.entities.User;
import com.jagrosh.discordipc.exceptions.NoDiscordClientException;
import lombok.experimental.UtilityClass;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.screen.MultiplayerScreen;
import net.minecraft.client.gui.screen.OptionsScreen;
import net.minecraft.client.gui.screen.WorldSelectionScreen;
import net.minecraft.network.play.server.SUpdateBossInfoPacket;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import xd.harm.Harmony;
import xd.harm.ui.mainmenu.AltScreen;
import xd.harm.ui.mainmenu.MainScreen;
import org.json.JSONObject;
import org.lwjgl.glfw.GLFW;
import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@UtilityClass
public class ClientUtil implements IMinecraft {
    public static User me;
    private static final IPCClient discordClient = new IPCClient(1367902337380778135L);
    private static volatile boolean rpcInitialized = false;
    private static Thread rpcUpdateThread;

    private static final int MAX_FIELD_LENGTH = 128;

    public static synchronized void startRPC() {
        if (rpcInitialized) {

            return;
        }


        try {

            discordClient.connect();
            rpcInitialized = true;

            startUpdateThread();
        } catch (NoDiscordClientException e) {



        } catch (Exception e) {
            e.printStackTrace();
            rpcInitialized = false;
            startReconnectThread();
        }
    }

    private static void tryConnect() {
        try {
            discordClient.connect();
            rpcInitialized = true;
            startUpdateThread();
        } catch (NoDiscordClientException e) {

        } catch (Exception e) {
            e.printStackTrace();
            rpcInitialized = false;
            startReconnectThread();
        }
    }

    private static void startReconnectThread() {
        new Thread(() -> {
            while (!rpcInitialized) {
                try {
                    Thread.sleep(10000);
                    tryConnect();
                } catch (InterruptedException ignored) {
                    break;
                }
            }
        }, "Discord-RPC-Reconnect").start();
    }

    private static void startUpdateThread() {
        if (discordClient.getStatus() != com.jagrosh.discordipc.entities.pipe.PipeStatus.CONNECTED) {
            return;
        }
        rpcUpdateThread = new Thread(() -> {
            while (rpcInitialized && discordClient.getStatus() == com.jagrosh.discordipc.entities.pipe.PipeStatus.CONNECTED) {
                try {
                    String state;
                    if (mc.currentScreen instanceof MainMenuScreen || mc.currentScreen instanceof MainScreen) {
                        state = "В главном меню";
                    } else if (mc.currentScreen instanceof MultiplayerScreen) {
                        state = "Выбирает сервер";
                    } else if (mc.isSingleplayer()) {
                        state = "В одиночном мире";
                    } else if (mc.getCurrentServerData() != null) {
                        String serverIP = mc.getCurrentServerData().serverIP
                                .replace("mc.", "").replace("play.", "")
                                .replace("gg.", "").replace("go.", "").replace("join.", "").replace("creative.", "")
                                .replace(".top", "").replace(".ru", "").replace(".cc", "").replace(".space", "")
                                .replace(".eu", "").replace(".com", "").replace(".net", "").replace(".xyz", "")
                                .replace(".gg", "").replace(".me", "").replace(".su", "").replace(".fun", "")
                                .replace(".org", "").replace(".host", "").replace("localhost", "LocalServer")
                                .replace(":25565", "");
                        state = "Играет на " + truncateString(serverIP, MAX_FIELD_LENGTH);
                    } else if (mc.currentScreen instanceof OptionsScreen) {
                        state = "В настройках";
                    } else if (mc.currentScreen instanceof WorldSelectionScreen) {
                        state = "Выбирает мир";
                    } else if (mc.currentScreen instanceof AltScreen) {
                        state = "В меню выбора аккаунтов";
                    } else {
                        state = "own: danta_mephedronov";
                    }

                    String modsInfo = "Моды: " + Harmony.getInstance().getModuleManager().countEnabledModules() + "/" +
                            Harmony.getInstance().getModuleManager().getModules().size() + " | Играет на: " +
                            (Minecraft.getInstance().getCurrentServerData() != null ?
                                    Minecraft.getInstance().getCurrentServerData().serverIP : "Локальном мире");
                    modsInfo = truncateString(modsInfo, MAX_FIELD_LENGTH);

                    RichPresence.Builder builder = new RichPresence.Builder()
                            .setDetails(truncateString(state, MAX_FIELD_LENGTH))
                            .setState(modsInfo)
                            .setStartTimestamp(OffsetDateTime.now())
                            .setLargeImage("logo", "Версия: 1.16.5 | Билд: " + Harmony.build);

                    RichPresence presence = builder.build();
                    discordClient.sendRichPresence(presence);

                    Thread.sleep(2000);
                } catch (InterruptedException ignored) {
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, "Discord-RPC-Update");
        rpcUpdateThread.start();
    }

    private static String truncateString(String input, int maxBytes) {
        if (input == null) return "";
        try {
            byte[] bytes = input.getBytes("UTF-8");
            if (bytes.length <= maxBytes) return input;
            int endIndex = input.length();
            while (endIndex > 0 && input.substring(0, endIndex).getBytes("UTF-8").length > maxBytes) {
                endIndex--;
            }
            return input.substring(0, endIndex);
        } catch (Exception e) {
            return input;
        }
    }

    public static String getDiscordUsername() {
        try {
            if (me != null) {
                String username = me.getName();
                return (username != null && !username.isEmpty()) ? username : "Unknown";
            }
            return "Unknown";
        } catch (Exception e) {
            return "Unknown";
        }
    }

    public static synchronized void stopRPC() {
        if (rpcInitialized) {
            rpcInitialized = false;
            if (rpcUpdateThread != null) {
                rpcUpdateThread.interrupt();
                rpcUpdateThread = null;
            }
            discordClient.close();
            me = null;
        }
    }

    public static String getUsername() {
        return System.getProperty("user.name");
    }


    private static Clip currentClip = null;
    private static boolean pvpMode;
    private static UUID uuid;

    public void updateBossInfo(SUpdateBossInfoPacket packet) {
        if (packet.getOperation() == SUpdateBossInfoPacket.Operation.ADD) {
            if (StringUtils.stripControlCodes(packet.getName().getString()).toLowerCase().contains("pvp")) {
                pvpMode = true;
                uuid = packet.getUniqueId();
            }
        } else if (packet.getOperation() == SUpdateBossInfoPacket.Operation.REMOVE) {
            if (packet.getUniqueId().equals(uuid))
                pvpMode = false;
        }
    }


    public boolean isPvP() {
        return pvpMode;
    }

    public void playSound(String sound, float value, boolean nonstop) {
        if (currentClip != null && currentClip.isRunning()) {
            currentClip.stop();
        }
        try {
            currentClip = AudioSystem.getClip();
            InputStream is = mc.getResourceManager().getResource(new ResourceLocation("harmony/sounds/" + sound + ".wav")).getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(bis);
            if (audioInputStream == null) {
                System.out.println("Sound not found!");
                return;
            }

            currentClip.open(audioInputStream);
            currentClip.start();
            FloatControl floatControl = (FloatControl) currentClip.getControl(FloatControl.Type.MASTER_GAIN);
            float min = floatControl.getMinimum();
            float max = floatControl.getMaximum();
            float volumeInDecibels = (float) (min * (1 - (value / 100.0)) + max * (value / 100.0));
            floatControl.setValue(volumeInDecibels);
            if (nonstop) {
                currentClip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        currentClip.setFramePosition(0);
                        currentClip.start();
                    }
                });
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public void stopSound() {
        if (currentClip != null) {
            currentClip.stop();
            currentClip.close();
            currentClip = null;
        }
    }

    public int calc(int value) {
        MainWindow rs = mc.getMainWindow();
        return (int) (value * rs.getGuiScaleFactor() / 2);
    }

    public Vec2i getMouse(int mouseX, int mouseY) {
        return new Vec2i((int) (mouseX * Minecraft.getInstance().getMainWindow().getGuiScaleFactor() / 2),
                (int) (mouseY * Minecraft.getInstance().getMainWindow().getGuiScaleFactor() / 2));
    }

    public static ITextComponent genGradientText(String text, int firstColor, int secondColor) {
        StringTextComponent component = new StringTextComponent("");

        if (text == null || text.isEmpty()) {
            return component;
        }

        char[] chars = text.toCharArray();
        int length = chars.length;

        for (int i = 0; i < length; i++) {
            float ratio = (float) i / (float) Math.max(1, length - 1);

            int r1 = (firstColor >> 16) & 0xFF;
            int g1 = (firstColor >> 8) & 0xFF;
            int b1 = firstColor & 0xFF;

            int r2 = (secondColor >> 16) & 0xFF;
            int g2 = (secondColor >> 8) & 0xFF;
            int b2 = secondColor & 0xFF;

            int red = (int) (r1 * (1 - ratio) + r2 * ratio);
            int green = (int) (g1 * (1 - ratio) + g2 * ratio);
            int blue = (int) (b1 * (1 - ratio) + b2 * ratio);

            red = Math.max(0, Math.min(255, red));
            green = Math.max(0, Math.min(255, green));
            blue = Math.max(0, Math.min(255, blue));

            int color = (red << 16) | (green << 8) | blue;


            StringTextComponent charComponent = new StringTextComponent(String.valueOf(chars[i]));
            charComponent.setStyle(charComponent.getStyle().setColor(net.minecraft.util.text.Color.fromInt(color)));
            component.append(charComponent);
        }

        return component;
    }
}
