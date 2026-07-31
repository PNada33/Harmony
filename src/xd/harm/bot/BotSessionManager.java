package xd.harm.bot;

import com.google.common.eventbus.Subscribe;
import net.minecraft.client.gui.ClientBossInfo;
import net.minecraft.client.gui.screen.ConnectingScreen;
import net.minecraft.client.gui.screen.MultiplayerScreen;
import net.minecraft.client.multiplayer.PlayerController;
import net.minecraft.client.multiplayer.ServerAddress;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.ProtocolType;
import net.minecraft.network.handshake.client.CHandshakePacket;
import net.minecraft.network.login.client.CLoginStartPacket;
import net.minecraft.network.play.client.CPlayerPacket;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.DefaultUncaughtExceptionHandler;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;
import xd.harm.baritone.api.BaritoneAPI;
import xd.harm.baritone.api.behavior.IPathingBehavior;
import xd.harm.Harmony;
import xd.harm.events.EventManager;
import xd.harm.events.render.EventRender3D;
import xd.harm.events.world.EventUpdate;
import xd.harm.events.world.TickEvent;
import xd.harm.ui.mainmenu.MainScreen;
import xd.harm.utils.client.IMinecraft;

import com.mojang.authlib.GameProfile;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public final class BotSessionManager implements IMinecraft {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Pattern NICK_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,16}$");
    private static final int GHOST_ENTITY_ID_THRESHOLD = -1_000_000_000;

    private static final Field PLAY_HANDLER_WORLD_FIELD = resolveClientPlayField("world");
    private static final Field PLAY_HANDLER_WORLD_INFO_FIELD = resolveClientPlayField("field_239161_g_");
    private static BotSessionManager instance;

    private static Field resolveClientPlayField(String name) {
        try {
            Field field = ClientPlayNetHandler.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (Exception e) {
            LOGGER.warn("Failed to resolve ClientPlayNetHandler field {}: {}", name, e.getMessage());
            return null;
        }
    }

    private static final class ParkedClientContext {
        private final String key;
        private final String nick;
        private final ClientPlayNetHandler playHandler;
        private final ClientWorld world;
        private final ClientPlayerEntity player;
        private final PlayerController playerController;
        private final ServerData serverData;

        private ParkedClientContext(
                String key,
                String nick,
                ClientPlayNetHandler playHandler,
                ClientWorld world,
                ClientPlayerEntity player,
                PlayerController playerController,
                ServerData serverData
        ) {
            this.key = key;
            this.nick = nick;
            this.playHandler = playHandler;
            this.world = world;
            this.player = player;
            this.playerController = playerController;
            this.serverData = serverData;
        }
    }

    private static final class PositionSnapshot {
        private final double x;
        private final double y;
        private final double z;
        private final float yaw;
        private final float pitch;
        private final boolean onGround;

        private PositionSnapshot(double x, double y, double z, float yaw, float pitch, boolean onGround) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
            this.onGround = onGround;
        }
    }

    private String makeUniqueLocalNick(String baseNick, String suffix) {
        String base = baseNick == null || baseNick.trim().isEmpty() ? "session" : baseNick;
        String suf = suffix == null ? "_x" : suffix;
        String candidate = base + suf;
        int i = 2;
        while (sessions.containsKey(candidate.toLowerCase(Locale.ROOT))) {
            candidate = base + suf + i;
            i++;
        }
        return candidate;
    }

    private final Map<String, BotSession> sessions = new ConcurrentHashMap<>();
    private final Map<java.util.UUID, ClientBossInfo> bossBackup = new LinkedHashMap<>();
    private final Map<String, Integer> ghostEntityIds = new ConcurrentHashMap<>();
    private final Map<String, ParkedClientContext> parkedSessionContexts = new ConcurrentHashMap<>();

    private volatile String controlledKey;
    private volatile boolean hudOverridden;
    private volatile ITextComponent tabHeaderBackup;
    private volatile ITextComponent tabFooterBackup;
    private volatile boolean foregroundMode;
    private volatile String foregroundNick;
    private volatile ServerData mainServerBackup;
    private volatile NetworkManager foregroundNetworkManager;
    private volatile boolean foregroundCrossServer;
    private volatile String foregroundHost;
    private volatile int foregroundPort;
    private volatile ParkedClientContext mainParkedContext;
    private volatile PositionSnapshot pendingForegroundSync;
    private volatile int pendingForegroundSyncTicks;

    private BotSessionManager() {
        Harmony.getInstance().getEventBus().register(this);
        EventManager.register(this);
    }

    public static synchronized BotSessionManager getInstance() {
        if (instance == null) {
            instance = new BotSessionManager();
        }
        return instance;
    }

    public String connectBot(String nick, String host, int port) {
        if (nick == null || !NICK_PATTERN.matcher(nick).matches()) {
            return "Nick must match [A-Za-z0-9_] and be 3..16 chars";
        }
        if (host == null || host.trim().isEmpty()) {
            return "Host is empty";
        }
        if (port < 1 || port > 65535) {
            return "Port must be 1..65535";
        }

        String key = nick.toLowerCase(Locale.ROOT);
        BotSession existing = sessions.get(key);
        if (existing != null && existing.getState() != BotSession.State.DISCONNECTED && existing.getState() != BotSession.State.FAILED) {
            return "Сессия с этим ником уже активна на серве";
        }
        if (existing != null) {
            sessions.remove(key);
            parkedSessionContexts.remove(key);
            if (key.equals(controlledKey)) {
                controlledKey = null;
                restoreHudState();
            }
        }

        BotSession session = new BotSession(nick, host, port);
        sessions.put(key, session);

        Thread connector = new Thread(() -> connectInternal(session), "BotConnect-" + nick);
        connector.setDaemon(true);
        connector.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER));
        connector.start();
        return null;
    }

    private void connectInternal(BotSession session) {
        try {
            // Авто-применяем дефолтный бот-конфиг "Bot" (HitAura/Scaffold/Velocity/AutoSprint)
            // в bot_module_config.json при каждом подключении бота.
            String botApplyErr = xd.harm.command.feature.BotCommand.applyBotConfig("Bot");
            if (botApplyErr != null) {
                LOGGER.warn("Bot default config apply skipped: {}", botApplyErr);
            }

            ServerAddress address = ServerAddress.fromString(session.getHost() + ":" + session.getPort());
            if (address == null || address.getIP().isEmpty()) {
                markFailed(session, "Хуйня серв бро");
                return;
            }

            InetAddress inetAddress = resolve(address.getIP());
            NetworkManager networkManager = NetworkManager.createNetworkManagerAndConnect(
                    inetAddress,
                    address.getPort(),
                    mc.gameSettings.isUsingNativeTransport()
            );
            session.setNetworkManager(networkManager);
            session.setState(BotSession.State.LOGIN, "Заходит");

            networkManager.setNetHandler(new BotLoginNetHandler(this, session, networkManager));
            networkManager.sendPacket(new CHandshakePacket(address.getIP(), address.getPort(), ProtocolType.LOGIN));
            networkManager.sendPacket(new CLoginStartPacket(session.getProfile()));
        } catch (UnknownHostException e) {
            markFailed(session, "Хз чо за серв");
        } catch (Exception e) {
            markFailed(session, e.getMessage() == null ? "Ошибка при подключении" : e.getMessage());
        }
    }

    private InetAddress resolve(String host) throws UnknownHostException {
        return InetAddress.getByName(host);
    }

    public String controlBot(String nick) {
        if (nick == null || nick.trim().isEmpty()) {
            return "Пусто нах";
        }
        BotSession session = sessions.get(nick.toLowerCase(Locale.ROOT));
        if (session == null) {
            return "Сессия сэбалась нах";
        }
        if (session.getState() != BotSession.State.PLAY) {
            return "Сессия не готова по причине: " + session.getStatus();
        }
        return switchToForeground(session);
    }

    public String rerunMainSession() {
        if (foregroundMode) {
            return rerunToMainFromForeground();
        }

        if (controlledKey == null) {
            return "И чо тут";
        }
        controlledKey = null;
        restoreHudState();
        return null;
    }

    public String disconnectBot(String nickOrNull) {
        if (nickOrNull == null || nickOrNull.trim().isEmpty()) {
            if (foregroundMode) {
                disconnectForegroundToMenu();
                return null;
            }
            if (controlledKey == null) {
                return "Ник хуета бро меняй";
            }
            return disconnectByKey(controlledKey);
        }
        if ("all".equalsIgnoreCase(nickOrNull)) {
            if (foregroundMode) {
                disconnectForegroundToMenu();
            }
            disconnectAll();
            return null;
        }
        if (foregroundMode && nickOrNull.equalsIgnoreCase(foregroundNick)) {
            disconnectForegroundToMenu();
            return null;
        }
        return disconnectByKey(nickOrNull.toLowerCase(Locale.ROOT));
    }

    private String disconnectByKey(String key) {
        BotSession session = sessions.get(key);
        if (session == null) {
            return "Сессия сэбалась";
        }

        session.disconnect("Disconnected by client");
        session.markDisconnected("Disconnected");
        sessions.remove(key);
        parkedSessionContexts.remove(key);
        if (key.equals(controlledKey)) {
            controlledKey = null;
            restoreHudState();
        }
        return null;
    }

    public void disconnectAll() {
        for (BotSession session : sessions.values()) {
            session.disconnect("Disconnected by client");
            session.markDisconnected("Disconnected");
        }
        sessions.clear();
        parkedSessionContexts.clear();
        controlledKey = null;
        mainParkedContext = null;
        foregroundCrossServer = false;
        foregroundHost = null;
        foregroundPort = 0;
        pendingForegroundSync = null;
        pendingForegroundSyncTicks = 0;
        clearGhosts();
        restoreHudState();
    }

    public boolean routeChatToControlled(String message) {
        BotSession session = getControlledSession();
        if (session == null) {
            return false;
        }
        boolean sent = session.sendChat(message);
        if (!sent) {
            print(TextFormatting.RED + "Сессия " + session.getNick() + " не готова (" + session.getStatus() + ")");
        }
        return true;
    }

    public List<BotSession> snapshotSessions() {
        return new ArrayList<>(sessions.values());
    }

    public boolean isForegroundMode() {
        return foregroundMode;
    }

    public String getForegroundNick() {
        return foregroundNick;
    }

    public static boolean isManagedGhostEntityId(int entityId) {
        return entityId <= GHOST_ENTITY_ID_THRESHOLD;
    }

    public boolean isManagedSessionNick(String nick) {
        if (nick == null || nick.trim().isEmpty()) {
            return false;
        }
        return sessions.containsKey(nick.toLowerCase(Locale.ROOT));
    }

    public Scoreboard getRenderScoreboard(Scoreboard fallback) {
        BotSession session = getControlledSession();
        if (session == null || session.getState() != BotSession.State.PLAY) {
            return fallback;
        }
        return session.getScoreboard();
    }

    public int getRenderTabSize(int fallback) {
        BotSession session = getControlledSession();
        if (session == null || session.getState() != BotSession.State.PLAY) {
            return fallback;
        }
        return session.copyPlayerInfos().size();
    }

    public List<NetworkPlayerInfo> getRenderTabEntries() {
        BotSession session = getControlledSession();
        if (session == null || session.getState() != BotSession.State.PLAY) {
            return null;
        }
        return session.copyPlayerInfos();
    }

    public void onSessionDisconnected(BotSession session, String reason) {
        onClientThread(() -> {
            session.markDisconnected(reason);
            session.setNetworkManager(null);
            parkedSessionContexts.remove(session.getKey());
            if (session.getKey().equals(controlledKey)) {
                controlledKey = null;
                restoreHudState();
                print(TextFormatting.RED + "Сэбался нах: " + reason);
            }
        });
    }

    public void onForegroundConnectFailed(String reason) {
        onClientThread(() -> {
            String failedNick = foregroundNick;
            String failedHost = foregroundHost;
            int failedPort = foregroundPort;

            foregroundMode = false;
            foregroundNick = null;
            foregroundNetworkManager = null;
            pendingForegroundSync = null;
            pendingForegroundSyncTicks = 0;
            print(TextFormatting.RED + "Ошибка нах: " + reason);
            foregroundCrossServer = false;
            foregroundHost = null;
            foregroundPort = 0;

             String restoreError = restoreMainFromParked();
             if (restoreError == null) {
                 print(TextFormatting.YELLOW + "Вернулся на основную сессию");
                 return;
             }
             LOGGER.warn("Хз нах: {}", restoreError);

            if (mc.world == null) {
                mc.displayGuiScreen(new MultiplayerScreen(new MainScreen()));
            }

            if (failedNick != null && failedHost != null && !failedHost.trim().isEmpty() && failedPort >= 1 && failedPort <= 65535) {
                BotSession current = sessions.get(failedNick.toLowerCase(Locale.ROOT));
                boolean connected = current != null
                        && current.getNetworkManager() != null
                        && current.getNetworkManager().isChannelOpen()
                        && current.getState() == BotSession.State.PLAY;
                if (!connected) {
                    String reconnectError = connectBot(failedNick, failedHost, failedPort);
                    if (reconnectError == null) {
                        print(TextFormatting.YELLOW + "Перезахожу нах " + failedNick + "...");
                    }
                }
            }
        });
    }

    public void onForegroundPlayDisconnected(ITextComponent reason) {
        onClientThread(() -> {
            if (!foregroundMode) {
                return;
            }
            stopPrimaryBaritonePathing();

            String reasonText = reason == null ? "Disconnected" : reason.getString();
            LOGGER.warn("Ошибка: {}", reasonText);

            foregroundMode = false;
            foregroundNick = null;
            foregroundNetworkManager = null;
            foregroundCrossServer = false;
            foregroundHost = null;
            foregroundPort = 0;
            pendingForegroundSync = null;
            pendingForegroundSyncTicks = 0;

            String restoreError = restoreMainFromParked();
            if (restoreError == null) {
                mainServerBackup = null;
                clearGhosts();
                print(TextFormatting.YELLOW + "Ошибка нах");
                return;
            }

            LOGGER.warn("Ошибка нах: {}", restoreError);
            mainParkedContext = null;
            parkedSessionContexts.clear();
            clearGhosts();
            if (mc.world == null) {
                mc.displayGuiScreen(new MultiplayerScreen(new MainScreen()));
            }
        });
    }

    public void onBotChat(BotSession session, ITextComponent message) {
        onClientThread(() -> {
            if (message == null) {
                return;
            }
            if (!session.getKey().equals(controlledKey)) {
                return;
            }
            ITextComponent component = new StringTextComponent(TextFormatting.DARK_GRAY + "[Сессия " + session.getNick() + "] " + TextFormatting.RESET)
                    .append(message.deepCopy());
            if (mc.player != null) {
                mc.ingameGUI.getChatGUI().printChatMessage(component);
            }
        });
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        syncHudState();
    }

    @Subscribe
    public void onRender3D(EventRender3D event) {
    }

    public void onClientTick(TickEvent event) {
        for (BotSession session : sessions.values()) {
            session.tickNetwork();
        }

        NetworkManager pendingForeground = foregroundNetworkManager;
        if (pendingForeground != null) {
            if (pendingForeground.isChannelOpen()) {
                pendingForeground.tick();
            } else {
                pendingForeground.handleDisconnection();
                foregroundNetworkManager = null;
            }
        }

        PositionSnapshot sync = pendingForegroundSync;
        if (sync != null && foregroundMode && mc.player != null && mc.getConnection() != null) {
            if (pendingForegroundSyncTicks > 4) {
                pendingForegroundSync = null;
                pendingForegroundSyncTicks = 0;
            } else {
                double dx = mc.player.getPosX() - sync.x;
                double dy = mc.player.getPosY() - sync.y;
                double dz = mc.player.getPosZ() - sync.z;
                double distSq = dx * dx + dy * dy + dz * dz;

                if (distSq > 0.25D || pendingForegroundSyncTicks == 0) {
                    mc.player.setPositionAndRotation(sync.x, sync.y, sync.z, sync.yaw, sync.pitch);
                    mc.getConnection().sendPacket(new net.minecraft.network.play.client.CPlayerPacket.PositionRotationPacket(
                            sync.x, sync.y, sync.z, sync.yaw, sync.pitch, sync.onGround
                    ));
                }

                pendingForegroundSyncTicks++;
            }
        }
        syncGhostPlayers();
    }


    private void syncHudState() {
        if (mc.ingameGUI == null) {
            return;
        }
        BotSession controlled = getControlledSession();
        if (controlled == null || controlled.getState() != BotSession.State.PLAY) {
            restoreHudState();
            return;
        }

        if (!hudOverridden) {
            hudOverridden = true;
            tabHeaderBackup = mc.ingameGUI.getTabList().header;
            tabFooterBackup = mc.ingameGUI.getTabList().footer;
            bossBackup.clear();
            bossBackup.putAll(mc.ingameGUI.getBossOverlay().mapBossInfos);
        }

        mc.ingameGUI.getTabList().setHeader(controlled.getTabHeader());
        mc.ingameGUI.getTabList().setFooter(controlled.getTabFooter());
        mc.ingameGUI.getBossOverlay().mapBossInfos.clear();
        mc.ingameGUI.getBossOverlay().mapBossInfos.putAll(controlled.copyBossInfos());
    }

    private void restoreHudState() {
        if (!hudOverridden || mc.ingameGUI == null) {
            return;
        }

        hudOverridden = false;
        mc.ingameGUI.getTabList().setHeader(tabHeaderBackup);
        mc.ingameGUI.getTabList().setFooter(tabFooterBackup);
        mc.ingameGUI.getBossOverlay().mapBossInfos.clear();
        mc.ingameGUI.getBossOverlay().mapBossInfos.putAll(bossBackup);
        bossBackup.clear();
    }

    private BotSession getControlledSession() {
        String key = controlledKey;
        if (key == null) {
            return null;
        }
        return sessions.get(key);
    }

    private void onClientThread(Runnable runnable) {
        if (mc.isOnExecutionThread()) {
            runnable.run();
        } else {
            mc.execute(runnable);
        }
    }

    private void markFailed(BotSession session, String reason) {
        session.markFailed(reason);
        onClientThread(() -> print(TextFormatting.RED + "Сессия " + session.getNick() + " ошибка: " + reason));
    }

    private void stopPrimaryBaritonePathing() {
        try {
            IPathingBehavior pathing = BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior();
            if (pathing == null) {
                return;
            }
            if (pathing.isPathing() || pathing.hasPath() || pathing.getGoal() != null) {
                pathing.cancelEverything();
                pathing.forceCancel();
            }
        } catch (Throwable t) {
        }
    }

    private PositionSnapshot snapshotPosition(BotSession session) {
        if (session == null || !session.hasPosition()) {
            return null;
        }
        return new PositionSnapshot(
                session.getPosX(),
                session.getPosY(),
                session.getPosZ(),
                session.getYaw(),
                session.getPitch(),
                session.isOnGround()
        );
    }

    private void flushSessionPosition(BotSession session) {
        if (session == null || !session.hasPosition()) {
            return;
        }
        NetworkManager networkManager = session.getNetworkManager();
        if (networkManager == null || !networkManager.isChannelOpen()) {
            return;
        }
        networkManager.sendPacket(new CPlayerPacket.PositionRotationPacket(
                session.getPosX(),
                session.getPosY(),
                session.getPosZ(),
                session.getYaw(),
                session.getPitch(),
                session.isOnGround()
        ));
    }


    private boolean isSessionOnServer(BotSession session, ServerData serverData) {
        if (session == null || serverData == null || serverData.serverIP == null || serverData.serverIP.trim().isEmpty()) {
            return false;
        }
        ServerAddress address = ServerAddress.fromString(serverData.serverIP);
        if (address == null || address.getIP().isEmpty()) {
            return false;
        }
        return session.getPort() == address.getPort()
                && session.getHost().equalsIgnoreCase(address.getIP());
    }


    private AbstractClientPlayerEntity findWorldPlayerForSession(
            BotSession session,
            Map<UUID, AbstractClientPlayerEntity> playersById,
            Map<String, AbstractClientPlayerEntity> playersByName
    ) {
        if (session == null) {
            return null;
        }
        GameProfile sessionProfile = session.getProfile();
        if (sessionProfile != null && sessionProfile.getId() != null) {
            AbstractClientPlayerEntity byId = playersById.get(sessionProfile.getId());
            if (byId != null) {
                return byId;
            }
        }
        AbstractClientPlayerEntity byNick = playersByName.get(session.getNick().toLowerCase(Locale.ROOT));
        if (byNick != null) {
            return byNick;
        }
        if (sessionProfile != null && sessionProfile.getName() != null) {
            AbstractClientPlayerEntity byProfileName = playersByName.get(sessionProfile.getName().toLowerCase(Locale.ROOT));
            if (byProfileName != null) {
                return byProfileName;
            }
        }
        return null;
    }

    private void removeGhost(String key) {
        Integer entityId = ghostEntityIds.remove(key);
        if (entityId == null || mc.world == null) {
            return;
        }
        mc.world.removeEntityFromWorld(entityId);
    }

    private void clearGhosts() {
        if (mc.world != null) {
            for (Integer entityId : ghostEntityIds.values()) {
                mc.world.removeEntityFromWorld(entityId);
            }
        }
        ghostEntityIds.clear();
    }

    private void syncGhostPlayers() {
        ghostEntityIds.clear();
    }

    private String switchToForeground(BotSession session) {
        stopPrimaryBaritonePathing();
        if (!foregroundMode) {
            mainServerBackup = copyServerData(mc.getCurrentServerData());
            parkCurrentMainSessionToBackground(session);
            foregroundCrossServer = !isSessionOnServer(session, mainServerBackup);
        } else {
            parkForegroundSessionToBackground();
            foregroundCrossServer = false;
        }

        controlledKey = null;
        restoreHudState();

        if (restoreForegroundFromParked(session)) {
            return null;
        }

        pendingForegroundSync = snapshotPosition(session);
        pendingForegroundSyncTicks = 0;
        session.disconnect("Switching to foreground");

        mc.unloadWorld();

        foregroundMode = true;
        foregroundNick = session.getNick();
        foregroundHost = session.getHost();
        foregroundPort = session.getPort();
        connectForeground(session.getNick(), session.getHost(), session.getPort());
        return null;
    }

    private ParkedClientContext captureCurrentMainContext(ClientPlayNetHandler connection, String parkedKey) {
        if (connection == null || mc.world == null || mc.player == null || mc.playerController == null) {
            return null;
        }

        String nick = connection.getGameProfile().getName();
        if (nick == null || nick.trim().isEmpty()) {
            return null;
        }

        return new ParkedClientContext(
                parkedKey,
                nick,
                connection,
                mc.world,
                mc.player,
                mc.playerController,
                copyServerData(mc.getCurrentServerData())
        );
    }

    private void parkCurrentMainSessionToBackground(BotSession preserveSession) {
        ClientPlayNetHandler connection = mc.getConnection();
        if (connection == null) {
            return;
        }

        NetworkManager networkManager = connection.getNetworkManager();
        if (networkManager == null || !networkManager.isChannelOpen()) {
            return;
        }

        ServerData current = mc.getCurrentServerData();
        if (current == null || current.serverIP == null || current.serverIP.trim().isEmpty()) {
            return;
        }

        ServerAddress parsed = ServerAddress.fromString(current.serverIP);
        if (parsed == null || parsed.getIP().isEmpty()) {
            return;
        }

        String nick = connection.getGameProfile().getName();
        String parkedNick = nick;
        String mainKey = nick.toLowerCase(Locale.ROOT);
        boolean collisionWithPreserve = preserveSession != null && preserveSession.getKey().equals(mainKey);

        if (collisionWithPreserve) {
            parkedNick = makeUniqueLocalNick(nick, "_main");
        } else {
            BotSession old = sessions.remove(mainKey);
            if (old != null && old.getNetworkManager() != networkManager) {
                old.disconnect("Replaced by main parking");
                old.markDisconnected("Replaced");
            }
        }

        String parkedKey = parkedNick.toLowerCase(Locale.ROOT);
        mainParkedContext = null;
        mainParkedContext = captureCurrentMainContext(connection, parkedKey);
        parkedSessionContexts.remove(parkedKey);

        BotSession parked = new BotSession(parkedNick, parsed.getIP(), parsed.getPort());
        parked.setProfile(connection.getGameProfile());
        parked.setNetworkManager(networkManager);
        parked.setState(BotSession.State.PLAY, "Background");
        if (mc.player != null) {
            parked.setPlayerEntityId(mc.player.getEntityId());
            parked.updatePosition(
                    mc.player.getPosX(),
                    mc.player.getPosY(),
                    mc.player.getPosZ(),
                    mc.player.rotationYaw,
                    mc.player.rotationPitch,
                    mc.player.isOnGround()
            );
        }
        if (mc.ingameGUI != null) {
            parked.setTabHeader(mc.ingameGUI.getTabList().header);
            parked.setTabFooter(mc.ingameGUI.getTabList().footer);
            synchronized (parked.getLock()) {
                parked.getBossInfos().clear();
                parked.getBossInfos().putAll(mc.ingameGUI.getBossOverlay().mapBossInfos);
            }
        }

        networkManager.setNetHandler(new BotPlayNetHandler(this, parked, networkManager));
        flushSessionPosition(parked);
        sessions.put(parked.getKey(), parked);
    }

    private void connectForeground(String nick, String host, int port) {
        Thread connector = new Thread(() -> {
            InetAddress inetAddress = null;
            try {
                Thread.sleep(2800L);

                ServerAddress address = ServerAddress.fromString(host + ":" + port);
                if (address == null || address.getIP().isEmpty()) {
                    onForegroundConnectFailed("Invalid server address");
                    return;
                }

                inetAddress = resolve(address.getIP());

                ServerData targetServer = new ServerData("Bot " + nick, address.getIP() + ":" + address.getPort(), false);
                onClientThread(() -> mc.setServerData(targetServer));

                NetworkManager networkManager = NetworkManager.createNetworkManagerAndConnect(
                        inetAddress,
                        address.getPort(),
                        mc.gameSettings.isUsingNativeTransport()
                );
                foregroundNetworkManager = networkManager;
                networkManager.setNetHandler(new ForegroundBotLoginNetHandler(
                        this,
                        networkManager,
                        new MultiplayerScreen(new MainScreen()),
                        nick
                ));
                networkManager.sendPacket(new CHandshakePacket(address.getIP(), address.getPort(), ProtocolType.LOGIN));
                networkManager.sendPacket(new CLoginStartPacket(new GameProfile((java.util.UUID) null, nick)));
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                foregroundNetworkManager = null;
                onForegroundConnectFailed("Ошибка нах");
            } catch (UnknownHostException e) {
                foregroundNetworkManager = null;
                onForegroundConnectFailed("Хз чо за хост");
            } catch (Exception e) {
                foregroundNetworkManager = null;
                String reason = e.getMessage() == null ? "Не смог зайти нах" : e.getMessage();
                if (inetAddress != null) {
                    reason = reason.replace(inetAddress + ":" + port, "");
                }
                onForegroundConnectFailed(reason);
            }
        }, "BotForeground-" + nick);
        connector.setDaemon(true);
        connector.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER));
        connector.start();
    }

    private String rerunToMainFromForeground() {
        stopPrimaryBaritonePathing();
        parkForegroundSessionToBackground();
        foregroundMode = false;
        foregroundNick = null;
        foregroundNetworkManager = null;
        foregroundCrossServer = false;
        foregroundHost = null;
        foregroundPort = 0;
        pendingForegroundSync = null;
        pendingForegroundSyncTicks = 0;
        String restoreError = restoreMainFromParked();
        if (restoreError != null) {
            return restoreError;
        }
        mainServerBackup = null;
        clearGhosts();
        return null;
    }

    private String restoreMainFromParked() {
        ParkedClientContext context = mainParkedContext;
        if (context == null) {
            return "No parked main context";
        }

        BotSession session = sessions.get(context.key);
        if (session == null) {
            return "Parked main session not found";
        }

        NetworkManager networkManager = session.getNetworkManager();
        if (networkManager == null || !networkManager.isChannelOpen()) {
            return "Parked main session is disconnected";
        }
        if (context.playHandler == null || context.world == null || context.player == null || context.playerController == null) {
            return "Parked main context is incomplete";
        }

        mc.unloadWorld();

        if (!rebindClientPlayWorld(context.playHandler, context.world)) {
            return "Failed to restore main play handler state";
        }

        networkManager.setNetHandler(context.playHandler);

        mc.loadWorld(context.world);
        mc.playerController = context.playerController;
        mc.player = context.player;
        mc.setRenderViewEntity(context.player);
        mc.networkManager = networkManager;
        if (context.serverData != null) {
            mc.setServerData(copyServerData(context.serverData));
        }
        if (mc.currentScreen != null) {
            mc.displayGuiScreen(null);
        }

        session.setNetworkManager(null);
        sessions.remove(context.key);
        mainParkedContext = null;
        return null;
    }

    private boolean restoreForegroundFromParked(BotSession session) {
        if (session == null) {
            return false;
        }

        String key = session.getKey();
        ParkedClientContext context = parkedSessionContexts.get(key);
        if (context == null) {
            return false;
        }

        NetworkManager networkManager = session.getNetworkManager();
        if (networkManager == null || !networkManager.isChannelOpen()) {
            parkedSessionContexts.remove(key);
            return false;
        }
        if (context.playHandler == null || context.world == null || context.player == null || context.playerController == null) {
            parkedSessionContexts.remove(key);
            return false;
        }

        mc.unloadWorld();
        if (!rebindClientPlayWorld(context.playHandler, context.world)) {
            return false;
        }

        networkManager.setNetHandler(context.playHandler);
        mc.loadWorld(context.world);
        mc.playerController = context.playerController;
        mc.player = context.player;
        mc.setRenderViewEntity(context.player);
        mc.networkManager = networkManager;
        if (context.serverData != null) {
            mc.setServerData(copyServerData(context.serverData));
        }
        if (mc.currentScreen != null) {
            mc.displayGuiScreen(null);
        }

        sessions.remove(key);
        session.setNetworkManager(null);
        parkedSessionContexts.remove(key);
        removeGhost(key);

        foregroundMode = true;
        foregroundNick = session.getNick();
        foregroundHost = session.getHost();
        foregroundPort = session.getPort();
        foregroundCrossServer = false;
        foregroundNetworkManager = null;
        pendingForegroundSync = null;
        pendingForegroundSyncTicks = 0;
        return true;
    }

    private boolean rebindClientPlayWorld(ClientPlayNetHandler playHandler, ClientWorld world) {
        try {
            if (PLAY_HANDLER_WORLD_FIELD == null || PLAY_HANDLER_WORLD_INFO_FIELD == null) {
                return false;
            }
            PLAY_HANDLER_WORLD_FIELD.set(playHandler, world);
            PLAY_HANDLER_WORLD_INFO_FIELD.set(playHandler, world.getWorldInfo());
            return true;
        } catch (Exception e) {
            LOGGER.warn("Failed to rebind play handler world: {}", e.getMessage());
            return false;
        }
    }

    private void parkForegroundSessionToBackground() {
        if (!foregroundMode) {
            return;
        }

        ClientPlayNetHandler connection = mc.getConnection();
        if (connection == null) {
            return;
        }

        NetworkManager networkManager = connection.getNetworkManager();
        if (networkManager == null || !networkManager.isChannelOpen()) {
            return;
        }

        String host = null;
        int port = 0;
        ServerData current = mc.getCurrentServerData();
        if (current != null && current.serverIP != null && !current.serverIP.trim().isEmpty()) {
            ServerAddress parsed = ServerAddress.fromString(current.serverIP);
            if (parsed != null && !parsed.getIP().isEmpty()) {
                host = parsed.getIP();
                port = parsed.getPort();
            }
        }
        if (host == null || host.trim().isEmpty() || port < 1 || port > 65535) {
            if (foregroundHost != null && !foregroundHost.trim().isEmpty() && foregroundPort >= 1 && foregroundPort <= 65535) {
                host = foregroundHost;
                port = foregroundPort;
            } else {
                return;
            }
        }

        String profileNick = connection.getGameProfile().getName();
        String nick = foregroundNick;
        if (nick == null || nick.trim().isEmpty()) {
            nick = profileNick;
        }
        String parkedNick = nick;
        if (mainParkedContext != null && profileNick != null && profileNick.equalsIgnoreCase(mainParkedContext.nick)) {
            parkedNick = makeUniqueLocalNick(nick, "_fg");
        }
        String parkedKey = parkedNick.toLowerCase(Locale.ROOT);

        BotSession old = sessions.remove(parkedKey);
        if (old != null && old.getNetworkManager() != networkManager) {
            old.disconnect("Replaced by foreground parking");
            old.markDisconnected("Replaced");
        }
        ParkedClientContext parkedContext = captureCurrentMainContext(connection, parkedKey);
        if (parkedContext != null) {
            parkedSessionContexts.put(parkedKey, parkedContext);
        } else {
            parkedSessionContexts.remove(parkedKey);
        }

        BotSession parked = new BotSession(parkedNick, host, port);
        parked.setProfile(connection.getGameProfile());
        parked.setNetworkManager(networkManager);
        parked.setState(BotSession.State.PLAY, "Background");
        if (mc.player != null) {
            parked.setPlayerEntityId(mc.player.getEntityId());
            parked.updatePosition(
                    mc.player.getPosX(),
                    mc.player.getPosY(),
                    mc.player.getPosZ(),
                    mc.player.rotationYaw,
                    mc.player.rotationPitch,
                    mc.player.isOnGround()
            );
        }
        if (mc.ingameGUI != null) {
            parked.setTabHeader(mc.ingameGUI.getTabList().header);
            parked.setTabFooter(mc.ingameGUI.getTabList().footer);
            synchronized (parked.getLock()) {
                parked.getBossInfos().clear();
                parked.getBossInfos().putAll(mc.ingameGUI.getBossOverlay().mapBossInfos);
            }
        }

        networkManager.setNetHandler(new BotPlayNetHandler(this, parked, networkManager));
        flushSessionPosition(parked);
        sessions.put(parked.getKey(), parked);
    }

    private void disconnectForegroundToMenu() {
        stopPrimaryBaritonePathing();
        foregroundMode = false;
        foregroundNick = null;
        foregroundNetworkManager = null;
        foregroundCrossServer = false;
        parkedSessionContexts.clear();
        foregroundHost = null;
        foregroundPort = 0;
        pendingForegroundSync = null;
        pendingForegroundSyncTicks = 0;
        if (mc.world != null) {
            mc.world.sendQuittingDisconnectingPacket();
        }
        String restoreError = restoreMainFromParked();
        if (restoreError == null) {
            mainServerBackup = null;
            clearGhosts();
            return;
        }
        LOGGER.warn("Failed to restore main after foreground manual disconnect: {}", restoreError);
        mainParkedContext = null;
        clearGhosts();
        mc.unloadWorld();
        mc.displayGuiScreen(new MultiplayerScreen(new MainScreen()));
    }

    private ServerData copyServerData(ServerData source) {
        if (source == null) {
            return null;
        }
        ServerData copy = new ServerData(source.serverName, source.serverIP, source.isOnLAN());
        copy.copyFrom(source);
        return copy;
    }

    public void onForegroundJoinedPlay(NetworkManager networkManager) {
        if (foregroundNetworkManager == networkManager) {
            foregroundNetworkManager = null;
        }
        foregroundCrossServer = false;
        ServerData current = mc.getCurrentServerData();
        if (current != null && current.serverIP != null && !current.serverIP.trim().isEmpty()) {
            ServerAddress parsed = ServerAddress.fromString(current.serverIP);
            if (parsed != null && !parsed.getIP().isEmpty()) {
                foregroundHost = parsed.getIP();
                foregroundPort = parsed.getPort();
            }
        }
    }
}
