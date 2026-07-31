package xd.harm;

import xd.harm.command.feature.*;
import xd.harm.utils.client.BotMode;
import xd.harm.utils.client.DiscordRPC;
import xd.harm.utils.rotation.RotationUtility;
import xd.harm.utils.waveycapes.WaveyCapesBase;
import com.google.common.eventbus.EventBus;
import xd.harm.bot.BotSessionManager;

import xd.harm.command.api.ConsoleLogger;
import xd.harm.command.api.MinecraftLogger;
import xd.harm.command.api.MultiLogger;
import xd.harm.command.api.ParametersFactoryImpl;
import xd.harm.command.api.PrefixImpl;
import xd.harm.command.api.StandaloneCommandDispatcher;
import xd.harm.command.interfaces.Command;
import xd.harm.command.interfaces.CommandDispatcher;
import xd.harm.command.interfaces.Logger;
import xd.harm.command.interfaces.ParametersFactory;
import xd.harm.command.interfaces.Prefix;
import xd.harm.config.FriendStorage;
import xd.harm.config.StaffStorage;
import xd.harm.config.ConfigStorage;
import xd.harm.config.CloudBootstrapLoader;
import xd.harm.config.MacroManager;
import xd.harm.events.input.EventKey;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleManager;
import xd.harm.modules.api.Notify;
import xd.harm.modules.impl.render.ClickGui;
import xd.harm.ui.notify.NotificationManager;

import xd.harm.config.AltConfig;
import xd.harm.ui.clickgui.MenuPanel;
import xd.harm.ui.mainmenu.AltScreen;

import xd.harm.ui.styles.Style;
import xd.harm.ui.styles.StyleFactory;
import xd.harm.ui.styles.StyleFactoryImpl;
import xd.harm.ui.styles.StyleManager;
import xd.harm.utils.TPSCalc;
import xd.harm.utils.client.CommandRetryService;
import xd.harm.utils.client.ServerTPS;
import xd.harm.utils.client.UserPublic;
import xd.harm.utils.drag.DragManager;
import xd.harm.utils.drag.Dragging;
import xd.harm.utils.rotation.FreeLookHandler;
import xd.harm.utils.rotation.RotationHandler;
import xd.harm.utils.text.font.ClientFonts;
import xd.harm.utils.recording.RecordingManager;
import xd.harm.chesttracker.HarmonyChestTrackerBootstrap;
import xd.harm.voicechat.HarmonyVoicechatBootstrap;
import lombok.*;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.Minecraft;
import via.ViaMCP;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Harmony {

    public static UserData userData;
    public boolean playerOnServer = false;
    public static final String name = UserPublic.getClientname;
    public static final String version = UserPublic.getVers;
    public static final String build = UserPublic.getVers;

    private static Harmony instance;

    public static Harmony getInstance() { return instance; }
    public ModuleManager getModuleManager() { return moduleManager; }
    public ConfigStorage getConfigStorage() { return configStorage; }
    public RotationUtility getRotationUtility() { return rotationUtility; }
    public CommandDispatcher getCommandDispatcher() { return commandDispatcher; }
    public ServerTPS getServerTPS() { return serverTPS; }
    public MacroManager getMacroManager() { return macroManager; }
    public StyleManager getStyleManager() { return styleManager; }
    public WaveyCapesBase getWaveyCapesBase() { return waveyCapesBase; }
    public EventBus getEventBus() { return eventBus; }
    public File getClientDir() { return clientDir; }
    public File getFilesDir() { return filesDir; }
    public AltConfig getAltConfig() { return altConfig; }
    public ViaMCP getViaMCP() { return viaMCP; }
    public TPSCalc getTpsCalc() { return tpsCalc; }
    public FreeLookHandler getFreeLookHandler() { return freeLookHandler; }
    public RotationHandler getRotationHandler() { return rotationHandler; }
    public AltScreen getAltScreen() { return altScreen; }
    public RecordingManager getRecordingManager() { return recordingManager; }
    public CommandRetryService getCommandRetryService() { return commandRetryService; }
    public MenuPanel getMenuPanel() { return menuPanel; }
    public xd.harm.ui.clickgui.minidrop.ClickGuiScreen getMiniDropDownScreen() { return miniDropDownScreen; }

    private ModuleManager moduleManager;
    private ConfigStorage configStorage;
    private RotationUtility rotationUtility;
    private CommandDispatcher commandDispatcher;
    private ServerTPS serverTPS;
    private MacroManager macroManager;
    private StyleManager styleManager;
    private WaveyCapesBase waveyCapesBase;

    private final EventBus eventBus = new EventBus();

    private final File clientDir = new File(Minecraft.getInstance().gameDir + "\\harmony\\files");
    private final File filesDir = new File(Minecraft.getInstance().gameDir + "\\harmony\\files\\other");

    private AltConfig altConfig;

    private MenuPanel menuPanel;
    private xd.harm.ui.clickgui.minidrop.ClickGuiScreen miniDropDownScreen;


    private ViaMCP viaMCP;
    private TPSCalc tpsCalc;

    private FreeLookHandler freeLookHandler;
    private RotationHandler rotationHandler;
    private AltScreen altScreen;
    private RecordingManager recordingManager;
    private CommandRetryService commandRetryService;

    public Harmony() {
        instance = this;

        if (!clientDir.exists()) {
            clientDir.mkdirs();
        }
        if (!filesDir.exists()) {
            filesDir.mkdirs();
        }

        clientLoad();
        FriendStorage.load();
        StaffStorage.load();
        DiscordRPC.startRPC();
        BotMode.init();
    }

    public static Object getInst() {
        return null;
    }

    public Dragging createDrag(Module module, String name, float x, float y) {
        DragManager.draggables.put(name, new Dragging(module, name, x, y));
        return DragManager.draggables.get(name);
    }

    @SneakyThrows
    private void clientLoad() {
        try {
            if (!Boolean.getBoolean("bot.mode")) {
                ClientFonts.init();
            } else {
                System.out.println("[BotMode] Skipping ClientFonts.init()");
            }
            viaMCP = new ViaMCP();
            serverTPS = new ServerTPS();
            commandRetryService = new CommandRetryService();
            BotSessionManager.getInstance();
            CloudBootstrapLoader.preload();
            macroManager = new MacroManager();
            configStorage = new ConfigStorage();
            Notify.NOTIFICATION_MANAGER = new NotificationManager();
            moduleManager = new ModuleManager();
            moduleManager.init();
            rotationUtility = new RotationUtility();

            initCommands();

            if (!Boolean.getBoolean("bot.mode")) {
                initStyles();
                altScreen = new AltScreen();
                altConfig = new AltConfig();
                DragManager.load();
                menuPanel = new MenuPanel();
                freeLookHandler = new FreeLookHandler();
                rotationHandler = new RotationHandler();
            }

            waveyCapesBase = new WaveyCapesBase();
            waveyCapesBase.init();

            tpsCalc = new TPSCalc();
            recordingManager = new RecordingManager();

            userData = new UserData("HarmonyClient", 1);

            try {
                configStorage.init();
            } catch (IOException e) {
                System.err.println("Ошибка при загрузке конфига: " + e.getMessage());
            }

            try {
                macroManager.init();
            } catch (IOException e) {
                System.err.println("Ошибка при загрузке макросов: " + e.getMessage());
            }

            eventBus.register(this);

            if (!Boolean.getBoolean("bot.mode")) {
                try {
                    HarmonyVoicechatBootstrap.init();
                } catch (Throwable throwable) {
                    System.err.println("VoiceChat bootstrap failed: " + throwable.getMessage());
                    throwable.printStackTrace();
                }
                try {
                    HarmonyChestTrackerBootstrap.init();
                } catch (Throwable throwable) {
                    System.err.println("ChestTracker bootstrap failed: " + throwable.getMessage());
                    throwable.printStackTrace();
                }
            }

            if (!Boolean.getBoolean("bot.mode")) {
                try {
                    altConfig.init();
                } catch (Exception e) {
                    System.err.println("Ошибка инициализации AltConfig: " + e.getMessage());
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            if (isCloudBootstrapFailure(e)) {
                throw new HarmonyStartupException("Cloud bootstrap unavailable");
            }
            System.err.println("Критическая ошибка при загрузке клиента: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Не удалось загрузить Harmony", e);
        }
    }

    private boolean isCloudBootstrapFailure(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.contains("Cloud bootstrap failed")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    public String randomNickname() {
        String[] names = new String[]{
                "Vexis", "Krux", "Zynta", "Phaze", "Glyph", "Nerve", "Kyro", "Axis", "Dox", "Salo", "Vanta", "Raze", "Mura", "Koda", "Lume", "Siren", "Exo", "Oriz", "Yugen", "Kira",
                "Aestix", "Kynto", "Veno", "Sway", "Rift", "Flux", "Zale", "Eonx", "Haze", "Daze", "Lyth", "Vorn", "Xylo", "Ethe", "Kaelo", "Nyxar", "Zoryn", "Vesta", "Rhys", "Thale",
                "Myre", "Kova", "Sola", "Fen", "Bryn", "Jora", "Kaelu", "Nym", "Vael", "Sura", "Zek", "Vyn", "Kox", "Grit", "Lux", "Ryn", "Jax", "Tev", "Nox", "Dra",
                "Cyn", "Vort", "Skax", "Mev", "Zon", "Pura", "Yul", "Kev", "Bax", "Zro", "Neo", "Zero", "Void", "Static", "Cinder", "Prism", "Echo", "Orbit", "Aura", "Zen",
                "Vector", "Delta", "Sigma", "Helix", "Ion", "Nova", "Cyber", "Logic", "Pulse", "Code", "Krow", "Xero", "Vyx", "Talon", "Mord", "Sintra", "Dusk", "Kael", "Ruin", "Vile"
        };
        String[] titles = new String[]{
                "EXE", "RAW", "LOG", "SYS", "CORE", "NULL", "V3", "MK1", "OS", "NET", "AI", "DEV", "BOT", "ROOT", "USER", "LINK", "HUB", "DATA", "FLOW", "GRID",
                "MVN", "MAIN", "WAVE", "VIBE", "ZONE", "MIND", "X", "Y", "Z", "FRAG", "DRIP", "SZN", "MODE", "SYNC", "CLAN", "TEAM", "PRO", "GOD", "ACE", "MVP",
                "SOFT", "DARK", "LITE", "COLD", "PURE", "GLITCH", "MIST", "RARE", "REAL", "FAKE", "LOST", "FOUND", "LAST", "FAST", "SLOW", "NEON", "DUST", "SILK", "BOLD", "DEAF",
                "MD", "FX", "ID", "0X", "V8", "RX", "DX", "LX", "UX", "UI", "VR", "AR", "HD", "4K", "SH", "JS", "TS", "GO", "RS", "PY", "DB", "FS", "MAX", "MIN", "XENO", "ZOD", "VOID", "NOX", "ULTRA", "LTD"
        };
        String name = names[(int)(((float)names.length - 1.0F) * (float)Math.random() * (((float)names.length - 1.0F) / (float)names.length))]; String title = titles[(int)(((float)titles.length - 1.0F) * (float)Math.random() * (((float)titles.length - 1.0F) / (float)titles.length))]; int size = (name + "_").length(); return name + "_" + (16 - size == 0 ? "" : title); }

    private final EventKey eventKey = new EventKey(-1);

    public void onKeyPressed(int key) {
        eventKey.setKey(key);
        eventBus.post(eventKey);
        macroManager.onKeyPressed(key);

        if (key == ClickGui.bind.get()) {
            if (ClickGui.guiStyle.get().equals("MiniDropDown")) {
                openMiniDropDown();
            } else {
                openMenuPanel();
            }
        }
    }

    private void openMenuPanel() {
        if (menuPanel == null) {
            menuPanel = new MenuPanel();
        }
        Minecraft.getInstance().displayGuiScreen(menuPanel);
    }

    public void openMenuPanelOnTab(int tab) {
        if (menuPanel == null) {
            menuPanel = new MenuPanel();
        }
        switch (tab) {
            case 1: menuPanel.openConfigsTab(); break;
            case 2: menuPanel.openAutoBuyTab(); break;
            case 3: menuPanel.openThemeTab(); break;
            case 4: menuPanel.openBotConfigsTab(); break;
        }
        Minecraft.getInstance().displayGuiScreen(menuPanel);
    }

    public void openMiniDropDown() {
        if (miniDropDownScreen == null) {
            miniDropDownScreen = new xd.harm.ui.clickgui.minidrop.ClickGuiScreen(
                    new net.minecraft.util.text.StringTextComponent("MiniDropDown"));
        }
        Minecraft.getInstance().displayGuiScreen(miniDropDownScreen);
    }


    private void initCommands() {
        Minecraft mc = Minecraft.getInstance();
        Logger logger = new MultiLogger(List.of(new ConsoleLogger(), new MinecraftLogger()));
        List<Command> commands = new ArrayList<>();
        Prefix prefix = new PrefixImpl();
        commands.add(new ListCommand(commands, logger));
        commands.add(new FriendCommand(prefix, logger, mc));
        commands.add(new BindCommand(prefix, logger));
        commands.add(new WayCommand(prefix, logger));
        commands.add(new ConfigCommand(configStorage, prefix, logger));
        commands.add(new MacroCommand(macroManager, prefix, logger));
        commands.add(new RecordingCommand(prefix, logger));
        commands.add(new VClipCommand(prefix, logger, mc));
        commands.add(new HClipCommand(prefix, logger, mc));
        commands.add(new StaffCommand(prefix, logger));
        commands.add(new TeleportCommand(prefix, logger));
        commands.add(new MemoryCommand(logger));
        commands.add(new ReconnectCommand(logger));
        commands.add(new BotCommand(prefix, logger));

        ParametersFactory parametersFactory = new ParametersFactoryImpl();

        commandDispatcher = new StandaloneCommandDispatcher(commands, prefix, parametersFactory, logger);
    }

    private void initStyles() {
        StyleFactory styleFactory = new StyleFactoryImpl();
        List<Style> styles = new ArrayList<>();
        styles.add(styleFactory.createStyle("Альфа?", new Color(126, 94, 141), new Color(0, 178, 253)));
        styleManager = new StyleManager(styles, styles.get(0));
    }

    public static class UserData {
        final String user;
        final int uid;
        public UserData(String user, int uid) { this.user = user; this.uid = uid; }
        public String getUser() { return user; }
        public int getUid() { return uid; }
    }
}
