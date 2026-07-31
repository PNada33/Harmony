package xd.harm.bot;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.gui.ClientBossInfo;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.client.CChatMessagePacket;
import net.minecraft.network.play.client.CPlayerPacket;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.text.ITextComponent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class BotSession {
    public enum State {
        CONNECTING,
        LOGIN,
        PLAY,
        DISCONNECTED,
        FAILED
    }

    private final String nick;
    private final String key;
    private final String host;
    private final int port;
    private final Scoreboard scoreboard = new Scoreboard();
    private final Map<UUID, ClientBossInfo> bossInfos = new LinkedHashMap<>();
    private final Map<UUID, NetworkPlayerInfo> playerInfoMap = new LinkedHashMap<>();
    private final Object lock = new Object();

    private volatile GameProfile profile;
    private volatile NetworkManager networkManager;
    private volatile State state = State.CONNECTING;
    private volatile String status = "Connecting";
    private volatile long createdAt = System.currentTimeMillis();
    private volatile long lastPacketAt = createdAt;
    private volatile ITextComponent tabHeader;
    private volatile ITextComponent tabFooter;
    private volatile boolean hasPosition;
    private volatile double posX;
    private volatile double posY;
    private volatile double posZ;
    private volatile float yaw;
    private volatile float pitch;
    private volatile boolean onGround = true;
    private volatile int heartbeatTicks;
    private volatile int playerEntityId = -1;

    public BotSession(String nick, String host, int port) {
        this.nick = nick;
        this.key = nick.toLowerCase(Locale.ROOT);
        this.host = host;
        this.port = port;
        this.profile = new GameProfile((UUID) null, nick);
    }

    public String getNick() {
        return nick;
    }

    public String getKey() {
        return key;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public GameProfile getProfile() {
        return profile;
    }

    public void setProfile(GameProfile profile) {
        this.profile = profile;
    }

    public NetworkManager getNetworkManager() {
        return networkManager;
    }

    public void setNetworkManager(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    public State getState() {
        return state;
    }

    public String getStatus() {
        return status;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getLastPacketAt() {
        return lastPacketAt;
    }

    public void touchPacket() {
        this.lastPacketAt = System.currentTimeMillis();
    }

    public void setState(State state, String status) {
        this.state = state;
        this.status = status;
    }

    public void markDisconnected(String reason) {
        this.state = State.DISCONNECTED;
        this.status = reason == null || reason.isEmpty() ? "Disconnected" : reason;
    }

    public void markFailed(String reason) {
        this.state = State.FAILED;
        this.status = reason == null || reason.isEmpty() ? "Failed" : reason;
    }

    public Scoreboard getScoreboard() {
        return scoreboard;
    }

    public void clearHudData() {
        synchronized (lock) {
            bossInfos.clear();
            playerInfoMap.clear();
            tabHeader = null;
            tabFooter = null;
        }
    }

    public void setTabHeader(ITextComponent tabHeader) {
        this.tabHeader = tabHeader;
    }

    public ITextComponent getTabHeader() {
        return tabHeader;
    }

    public void setTabFooter(ITextComponent tabFooter) {
        this.tabFooter = tabFooter;
    }

    public ITextComponent getTabFooter() {
        return tabFooter;
    }

    public Map<UUID, ClientBossInfo> getBossInfos() {
        return bossInfos;
    }

    public Map<UUID, NetworkPlayerInfo> getPlayerInfoMap() {
        return playerInfoMap;
    }

    public Map<UUID, ClientBossInfo> copyBossInfos() {
        synchronized (lock) {
            return new LinkedHashMap<>(bossInfos);
        }
    }

    public List<NetworkPlayerInfo> copyPlayerInfos() {
        synchronized (lock) {
            return new ArrayList<>(playerInfoMap.values());
        }
    }

    public Object getLock() {
        return lock;
    }

    public void updatePosition(double x, double y, double z, float yaw, float pitch, boolean onGround) {
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.onGround = onGround;
        this.hasPosition = true;
    }

    public boolean hasPosition() {
        return hasPosition;
    }

    public double getPosX() {
        return posX;
    }

    public double getPosY() {
        return posY;
    }

    public double getPosZ() {
        return posZ;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public boolean isOnGround() {
        return onGround;
    }

    public int getPlayerEntityId() {
        return playerEntityId;
    }

    public void setPlayerEntityId(int playerEntityId) {
        this.playerEntityId = playerEntityId;
    }

    public void clearPosition() {
        this.hasPosition = false;
        this.posX = 0.0D;
        this.posY = 0.0D;
        this.posZ = 0.0D;
        this.yaw = 0.0F;
        this.pitch = 0.0F;
        this.onGround = true;
    }

    public void tickNetwork() {
        NetworkManager nm = this.networkManager;
        if (nm == null) {
            return;
        }

        if (nm.isChannelOpen()) {
            nm.tick();
            if (state == State.PLAY) {
                heartbeatTicks++;
                if (heartbeatTicks >= 1) {
                    heartbeatTicks = 0;
                    if (hasPosition) {
                        nm.sendPacket(new CPlayerPacket.PositionRotationPacket(posX, posY, posZ, yaw, pitch, onGround));
                    } else {
                        nm.sendPacket(new CPlayerPacket(onGround));
                    }
                }
            }
        } else {
            nm.handleDisconnection();
        }
    }

    public boolean sendChat(String message) {
        NetworkManager nm = this.networkManager;
        if (nm == null || !nm.isChannelOpen() || state != State.PLAY) {
            return false;
        }
        nm.sendPacket(new CChatMessagePacket(message));
        return true;
    }

    public void disconnect(String reason) {
        NetworkManager nm = this.networkManager;
        if (nm != null && nm.isChannelOpen()) {
            nm.closeChannel(new net.minecraft.util.text.StringTextComponent(reason));
        }
    }
}
