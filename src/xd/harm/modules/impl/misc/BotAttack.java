package xd.harm.modules.impl.misc;

import com.google.common.eventbus.Subscribe;
import xd.harm.events.network.EventPacket;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.CategorySetting;
import xd.harm.modules.settings.impl.ModeSetting;
import xd.harm.modules.settings.impl.SliderSetting;
import xd.harm.modules.settings.impl.StringSetting;
import xd.harm.utils.math.TimerHelper;
import xd.harm.utils.client.BotMode;

import net.minecraft.util.text.StringTextComponent;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import xd.harm.config.Config;
import net.minecraft.network.play.client.CChatMessagePacket;

@ModuleRegister(name = "BotAttack", category = Category.Misc, desc = "Запускает ботов Harmony")
public class BotAttack extends Module {

    private final CategorySetting catMain = new CategorySetting("ГЛАВНОЕ");
    private final SliderSetting botCount = new SliderSetting("Bot Count", 5f, 1f, 50f, 1f);
    private final SliderSetting launchDelay = new SliderSetting("Launch Delay", 5f, 1f, 30f, 1f);
    private final ModeSetting nicknameMode = new ModeSetting("Nickname Mode", "Random", "Random", "Custom", "Custom List");
    private final StringSetting customNick = new StringSetting("Custom Nick", "Mishka", "Base nickname for Custom mode")
            .setVisible(() -> nicknameMode.is("Custom"));
    private final StringSetting nicknames = new StringSetting("Nicknames", "Bot1,Bot2,Bot3", "Nicknames (comma-separated)")
            .setVisible(() -> nicknameMode.is("Custom List"));
    private final StringSetting serverIP = new StringSetting("Server IP", "localhost:25565", "IP:Port for bots to join");
    private final BooleanSetting autoConnect = new BooleanSetting("Auto Connect", true);
    private final StringSetting proxyList = new StringSetting("Proxies", "", "host:port,host:port,... (SOCKS5)");

    private final CategorySetting catCommands = new CategorySetting("КОМАНДЫ");
    private final ModeSetting afterSpawn = new ModeSetting("After Spawn", "Nothing", "Nothing", "Follow Me");
    private final SliderSetting followDistance = new SliderSetting("Follow Distance", 0.5f, 0f, 5f, 0.1f)
            .setVisible(() -> afterSpawn.is("Follow Me"));
    private final BooleanSetting botBWEnter = new BooleanSetting("BW Auto Enter", false);
    private final BooleanSetting botBWEnterLeave = new BooleanSetting("BWAutoEnterLeave", false);

    private final CategorySetting catCombat = new CategorySetting("БОЙ");
    private final BooleanSetting botHitAura = new BooleanSetting("HitAura", true);

    private final CategorySetting catMovement = new CategorySetting("ДВИЖЕНИЕ");
    private final BooleanSetting botSprint = new BooleanSetting("Sprint", true);
    private final BooleanSetting botScaffold = new BooleanSetting("Scaffold", false);
    private final BooleanSetting botVelocity = new BooleanSetting("Velocity", false);
    private final BooleanSetting botInvManager = new BooleanSetting("InvManager после закупки", true);
    private final ModeSetting botInvMode = new ModeSetting("Inv режим", "Оба", "Мусор", "Сортировка", "Оба");
    private final SliderSetting botInvMinDelay = new SliderSetting("Inv мин. задержка", 50f, 0f, 500f, 10f)
            .setVisible(() -> botInvManager.get());
    private final SliderSetting botInvMaxDelay = new SliderSetting("Inv макс. задержка", 150f, 0f, 500f, 10f)
            .setVisible(() -> botInvManager.get());
    private final BooleanSetting botInvAutoArmor = new BooleanSetting("Inv авто-броня", true)
            .setVisible(() -> botInvManager.get());
    private final BooleanSetting botInvArmorHotbar = new BooleanSetting("Inv броня HotBar", true)
            .setVisible(() -> botInvManager.get() && botInvAutoArmor.get());
    private final BooleanSetting botInvDropGarbage = new BooleanSetting("Inv выбрасывать мусор", false)
            .setVisible(() -> botInvManager.get() && !botInvMode.is("Сортировка"));
    private final BooleanSetting botInvGarbageHotbar = new BooleanSetting("Inv мусор HotBar", false)
            .setVisible(() -> botInvManager.get() && botInvDropGarbage.get());
    private final ModeSetting botInvBlockOrder = new ModeSetting("Inv блоки", "Уменьшение", "Увеличение", "Уменьшение");
    private final ModeSetting botInvSlot1 = new ModeSetting("Inv слот 1", "Меч", "Меч", "Лучший предмет", "Кирка", "Топор", "Блоки", "Еда", "Ничего");
    private final ModeSetting botInvSlot2 = new ModeSetting("Inv слот 2", "Кирка", "Меч", "Лучший предмет", "Кирка", "Топор", "Блоки", "Еда", "Ничего");
    private final ModeSetting botInvSlot3 = new ModeSetting("Inv слот 3", "Ничего", "Меч", "Лучший предмет", "Кирка", "Топор", "Блоки", "Еда", "Ничего");
    private final ModeSetting botInvSlot4 = new ModeSetting("Inv слот 4", "Ничего", "Меч", "Лучший предмет", "Кирка", "Топор", "Блоки", "Еда", "Ничего");
    private final ModeSetting botInvSlot5 = new ModeSetting("Inv слот 5", "Ничего", "Меч", "Лучший предмет", "Кирка", "Топор", "Блоки", "Еда", "Ничего");
    private final ModeSetting botInvSlot6 = new ModeSetting("Inv слот 6", "Блоки", "Меч", "Лучший предмет", "Кирка", "Топор", "Блоки", "Еда", "Ничего");
    private final ModeSetting botInvSlot7 = new ModeSetting("Inv слот 7", "Ничего", "Меч", "Лучший предмет", "Кирка", "Топор", "Блоки", "Еда", "Ничего");
    private final ModeSetting botInvSlot8 = new ModeSetting("Inv слот 8", "Ничего", "Меч", "Лучший предмет", "Кирка", "Топор", "Блоки", "Еда", "Ничего");
    private final ModeSetting botInvSlot9 = new ModeSetting("Inv слот 9", "Ничего", "Меч", "Лучший предмет", "Кирка", "Топор", "Блоки", "Еда", "Ничего");

    private final CategorySetting catAuto = new CategorySetting("АВТОМАТИЗАЦИЯ");
    private final BooleanSetting botAutoSkin = new BooleanSetting("Auto Skin", false);
    private final StringSetting botSkinName = new StringSetting("Skin Name", "", "Skin username");
    private final BooleanSetting botAutoRegister = new BooleanSetting("Auto Register", false);
    private final StringSetting botRegisterPassword = new StringSetting("Register Password", "", "Password for /register");

    private final CategorySetting catBw = new CategorySetting("БВ-НАСТРОЙКИ");
    private final BooleanSetting botBWJoin = new BooleanSetting("BW Auto Join", false);
    private final BooleanSetting botBWJoinHelper = new BooleanSetting("BWJoinHelper", false);
    private final BooleanSetting botBWAutoLeave = new BooleanSetting("BWAutoLeave", false);

    private final CategorySetting catAI = new CategorySetting("AI-НАСТРОЙКИ");
    private final BooleanSetting botBedWarsAI = new BooleanSetting("BedWars", false);
    private final BooleanSetting bwLlmStrategist = new BooleanSetting("LLM Стратег", false)
            .setVisible(() -> botBedWarsAI.get());
    private final CategorySetting catTeamAI = new CategorySetting("КОМАНДНЫЙ ИИ");
    private final BooleanSetting bwTeamAI = new BooleanSetting("Team LLM Strategy", false).setVisible(() -> botBedWarsAI.get());
    private final StringSetting bwTeamId = new StringSetting("Team ID", "LionsTempleTeam", "Общий ID команды").setVisible(() -> botBedWarsAI.get() && bwTeamAI.get());
    private final StringSetting bwTeamRoles = new StringSetting("Role Layout", "Bridger,Defender,Collector,Fighter", "Роли по порядку запуска").setVisible(() -> botBedWarsAI.get() && bwTeamAI.get());
    private final SliderSetting bwTeamDecisionSeconds = new SliderSetting("Team Decision Sec", 5f, 2f, 20f, 1f).setVisible(() -> botBedWarsAI.get() && bwTeamAI.get());
    private final ModeSetting bwStrategy = new ModeSetting("Strategy", "Balanced", "Balanced", "Rush Mid", "Defensive", "Aggressive", "AggressiveMax")
            .setVisible(() -> botBedWarsAI.get());
    private final ModeSetting bwMaxTarget = new ModeSetting("Resource Target", "30i 6g", "30i 6g", "35i 7g")
            .setVisible(() -> botBedWarsAI.get() && bwStrategy.is("AggressiveMax"));
    private final BooleanSetting bwExtraIronEnabled = new BooleanSetting("Доп-Железо", false)
            .setVisible(() -> botBedWarsAI.get() && bwStrategy.is("AggressiveMax"));
    private final SliderSetting bwExtraIron = new SliderSetting("Доп-Железо Кол-во", 1f, 1f, 64f, 1f)
            .setVisible(() -> botBedWarsAI.get() && bwStrategy.is("AggressiveMax") && bwExtraIronEnabled.get());
    private final BooleanSetting bwExtraGoldEnabled = new BooleanSetting("Доп-Золото", false)
            .setVisible(() -> botBedWarsAI.get() && bwStrategy.is("AggressiveMax"));
    private final SliderSetting bwExtraGold = new SliderSetting("Доп-Золото Кол-во", 1f, 1f, 64f, 1f)
            .setVisible(() -> botBedWarsAI.get() && bwStrategy.is("AggressiveMax") && bwExtraGoldEnabled.get());
    private final SliderSetting bwBuyDelay = new SliderSetting("Buy Delay", 10f, 5f, 30f, 1f)
            .setVisible(() -> botBedWarsAI.get());
    private final SliderSetting bwBridgeBlocks = new SliderSetting("Bridge Blocks", 64f, 16f, 128f, 8f)
            .setVisible(() -> botBedWarsAI.get());
    private final SliderSetting bwFightRange = new SliderSetting("Fight Range", 4.0f, 2.0f, 6.0f, 0.5f)
            .setVisible(() -> botBedWarsAI.get());
    private final SliderSetting bwCollectRadius = new SliderSetting("Collect Radius", 8.0f, 3.0f, 16.0f, 1f)
            .setVisible(() -> botBedWarsAI.get());
    private final SliderSetting bwGenDistance = new SliderSetting("Gen Distance", 1.0f, 0.5f, 5.0f, 0.5f)
            .setVisible(() -> botBedWarsAI.get());
    private final BooleanSetting bwAutoDefendBed = new BooleanSetting("Auto Defend Bed", true)
            .setVisible(() -> botBedWarsAI.get());
    private final BooleanSetting bwBuyArmor = new BooleanSetting("Buy Armor", true)
            .setVisible(() -> botBedWarsAI.get());
    private final BooleanSetting bwBuySword = new BooleanSetting("Buy Sword", true)
            .setVisible(() -> botBedWarsAI.get());
    private final BooleanSetting bwBuyPickaxe = new BooleanSetting("Buy Pickaxe", true)
            .setVisible(() -> botBedWarsAI.get());
    private final BooleanSetting bwBuyBlocks = new BooleanSetting("Buy Blocks", true)
            .setVisible(() -> botBedWarsAI.get());
    private final BooleanSetting bwOnlyOneBlock = new BooleanSetting("Только 1 Блок", false)
            .setVisible(() -> botBedWarsAI.get() && bwBuyBlocks.get());
    private final BooleanSetting bwSaveGenerator = new BooleanSetting("Save Generator", false)
            .setVisible(() -> botBedWarsAI.get());
    private final BooleanSetting bwBaritoneNav = new BooleanSetting("Baritone Nav", false)
            .setVisible(() -> botBedWarsAI.get());

    private final CategorySetting catBrain = new CategorySetting("МОЗГ-ИНС");
    private final BooleanSetting bwAIBrain = new BooleanSetting("Neural Brain", false);
    private final BooleanSetting bwAIChat = new BooleanSetting("Brain Chat", true);
    private final BooleanSetting bwAIRecord = new BooleanSetting("Record Demos", false);

    private final TimerHelper timer = new TimerHelper();
    private final Random random = new Random();
    private int botsLaunched;
    private boolean launching;

    private static final String PROJECT_DIR = "E:\\Мои Сурсы\\harmony";
    private static final String BOTMODE_DIR = "E:\\Мои Сурсы\\botmode";

    private static final String CONTROL_FILE = "E:\\Мои Сурсы\\harmony\\bot_control.txt";
    private static final String MODULE_CONFIG_FILE = "E:\\Мои Сурсы\\harmony\\bot_module_config.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String KILL_DIAG_FILE = "E:\\Мои Сурсы\\harmony\\bot_kill_diag.txt";

    private static final List<Process> botProcesses = new ArrayList<>();
    private String lastBWNumber = "";
    private String pendingBWJoin = "";

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { java.nio.file.Files.writeString(new java.io.File("E:\\Мои Сурсы\\harmony\\bot_kill_diag.txt").toPath(), "Static shutdown hook at " + System.currentTimeMillis() + "\n", java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND); } catch (Exception e) {}
            synchronized (botProcesses) {
                try { java.nio.file.Files.writeString(new java.io.File("E:\\Мои Сурсы\\harmony\\bot_kill_diag.txt").toPath(), "  botProcesses size: " + botProcesses.size() + "\n", java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND); } catch (Exception e) {}
                for (Process p : new ArrayList<>(botProcesses)) {
                    long pid = p.pid();
                    try { 
                        p.destroy();
                        if (!p.waitFor(2, TimeUnit.SECONDS)) {
                            p.destroyForcibly();
                        }
                        try { java.nio.file.Files.writeString(new java.io.File("E:\\Мои Сурсы\\harmony\\bot_kill_diag.txt").toPath(), "  killed PID " + pid + "\n", java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND); } catch (Exception e) {}
                    } catch (Exception ex) {
                        try { java.nio.file.Files.writeString(new java.io.File("E:\\Мои Сурсы\\harmony\\bot_kill_diag.txt").toPath(), "  ERROR killing PID " + pid + ": " + ex + "\n", java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND); } catch (Exception e) {}
                    }
                }
                botProcesses.clear();
            }
        }, "BotAttack-Shutdown"));
    }

    public static void shutdownAll() {
        try { java.nio.file.Files.writeString(new java.io.File("E:\\Мои Сурсы\\harmony\\bot_kill_diag.txt").toPath(), "shutdownAll called at " + System.currentTimeMillis() + "\n", java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND); } catch (Exception e) {}
        synchronized (botProcesses) {
            try { java.nio.file.Files.writeString(new java.io.File("E:\\Мои Сурсы\\harmony\\bot_kill_diag.txt").toPath(), "  botProcesses size: " + botProcesses.size() + "\n", java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND); } catch (Exception e) {}
            for (Process p : new ArrayList<>(botProcesses)) {
                long pid = p.pid();
                try { 
                    p.destroy();
                    if (!p.waitFor(3, TimeUnit.SECONDS)) {
                        p.destroyForcibly();
                        p.waitFor(1, TimeUnit.SECONDS);
                    }
                    try { java.nio.file.Files.writeString(new java.io.File("E:\\Мои Сурсы\\harmony\\bot_kill_diag.txt").toPath(), "  killed PID " + pid + "\n", java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND); } catch (Exception e) {}
                } catch (Exception ex) {
                    try { java.nio.file.Files.writeString(new java.io.File("E:\\Мои Сурсы\\harmony\\bot_kill_diag.txt").toPath(), "  ERROR killing PID " + pid + ": " + ex + "\n", java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND); } catch (Exception e) {}
                }
            }
            botProcesses.clear();
        }
        // Turn off module if it was on
        xd.harm.Harmony.getInstance().getModuleManager().getModules().stream()
            .filter(m -> m.getName().equals("BotAttack"))
            .findFirst()
            .ifPresent(m -> {
                if (m.isState()) m.toggle();
            });
    }

    public BotAttack() {
        addSettings(catMain, botCount, launchDelay, nicknameMode, customNick, nicknames, serverIP, autoConnect, proxyList,
                catCommands, afterSpawn, followDistance, botBWEnter, botBWEnterLeave,
                catCombat, botHitAura,
                catMovement, botSprint, botScaffold, botVelocity, botInvManager, botInvMode,
                botInvMinDelay, botInvMaxDelay, botInvAutoArmor, botInvArmorHotbar,
                botInvDropGarbage, botInvGarbageHotbar, botInvBlockOrder,
                botInvSlot1, botInvSlot2, botInvSlot3, botInvSlot4, botInvSlot5,
                botInvSlot6, botInvSlot7, botInvSlot8, botInvSlot9,
                catAuto, botAutoSkin, botSkinName, botAutoRegister, botRegisterPassword,
                catBw, botBWJoin, botBWJoinHelper, botBWAutoLeave,
                catAI, botBedWarsAI, bwLlmStrategist, catTeamAI, bwTeamAI, bwTeamId, bwTeamRoles, bwTeamDecisionSeconds, bwStrategy, bwMaxTarget, bwExtraIronEnabled, bwExtraIron, bwExtraGoldEnabled, bwExtraGold, bwBuyDelay, bwBridgeBlocks, bwFightRange, bwCollectRadius, bwGenDistance,
                bwAutoDefendBed, bwBuyArmor, bwBuySword, bwBuyPickaxe, bwBuyBlocks, bwOnlyOneBlock, bwSaveGenerator, bwBaritoneNav,
                catBrain, bwAIBrain, bwAIChat, bwAIRecord);
    }

    @Override
    public boolean onEnable() {
        super.onEnable();
        botsLaunched = 0;
        launching = true;
        timer.reset();
        return false;
    }

    @Override
    public boolean onDisable() {
        super.onDisable();
        launching = false;
        return false;
    }

    @Subscribe
    private void onUpdate(EventUpdate e) {
        // Сохранение генератора — стоять на точке и включить Save Generator
        if (isState() && botBedWarsAI.get() && bwSaveGenerator.get()) {
            xd.harm.utils.client.BotMode.saveCurrentGenerator();
            bwSaveGenerator.set(false);
        }

        syncControlFile();
        cleanupDeadProcesses();

        if (!launching) return;
        if (botsLaunched >= botCount.get().intValue()) {
            launching = false;
            return;
        }

        if (timer.hasReached(launchDelay.getFloat() * 1000)) {
            String nick = generateNick(botsLaunched);

            launchHarmony(nick, botsLaunched);
            botsLaunched++;
            timer.reset();
        }
    }

    @Subscribe
    private void onPacket(EventPacket event) {
        if (!isState() || !botBWJoin.get()) return;
        if (!event.isSend()) return;

        if (event.getPacket() instanceof CChatMessagePacket chatPacket) {
            String msg = chatPacket.getMessage();
            if (msg.startsWith("/") || msg.length() > 6) return;

            String number = "";
            for (int i = msg.length() - 1; i >= 0; i--) {
                if (Character.isDigit(msg.charAt(i))) {
                    number = msg.charAt(i) + number;
                } else break;
            }

            if (number.length() > 0 && number.length() < 3 && !number.equals(lastBWNumber)) {
                lastBWNumber = number;
                pendingBWJoin = number;
            }
        }
    }

    private long lastCleanup;
    private void cleanupDeadProcesses() {
        if (System.currentTimeMillis() - lastCleanup < 1000) return;
        lastCleanup = System.currentTimeMillis();

        synchronized (botProcesses) {
            botProcesses.removeIf(p -> !p.isAlive());
            if (botProcesses.isEmpty() && isState() && botsLaunched > 0) {
                setState(false, false);
            }
        }
    }

    private void launchHarmony(String nick, int botIndex) {
        Thread launcher = new Thread(() -> {
            Process process = null;
            try {
                String javaBin = findJavaExecutable();

                String classpath = buildClasspath();
                String nativesDir = PROJECT_DIR + "\\libraries\\natives";

                java.util.List<String> cmd = new java.util.ArrayList<>();
                cmd.add(javaBin);
                cmd.add("-cp");
                cmd.add(classpath);
                cmd.add("-Xmx1G");
                cmd.add("-Xms512M");
                cmd.add("-Djava.library.path=" + nativesDir);
                cmd.add("-Dbot.mode=true");
                cmd.add("-Dvoicechat.disable=true");
                cmd.add("-Dbot.nick=" + nick);
                cmd.add("-Dbot.id=" + nick);
                cmd.add("-Dbot.team=" + bwTeamId.get().trim());
                cmd.add("-Dbot.index=" + botIndex);
                cmd.add("-Dbot.role=Auto");
                cmd.add("-Dbot.role.layout=" + bwTeamRoles.get().trim());
                cmd.add("-Dbot.team.dir=" + PROJECT_DIR + "\\team_ai");
                cmd.add("-Dbot.config.file=" + MODULE_CONFIG_FILE);

                String proxyLine = proxyList.get().trim();
                if (!proxyLine.isEmpty()) {
                    String[] proxies = proxyLine.split(",");
                    String proxy = proxies[botIndex % proxies.length].trim();
                    int colon = proxy.lastIndexOf(':');
                    if (colon > 0) {
                        String host = proxy.substring(0, colon).trim();
                        String port = proxy.substring(colon + 1).trim();
                        cmd.add("-Dbot.proxyHost=" + host);
                        cmd.add("-Dbot.proxyPort=" + port);
                    }
                }
                cmd.add("Start");

                if (autoConnect.get() && !serverIP.get().trim().isEmpty()) {
                    String ip = serverIP.get().trim();
                    String host = "localhost";
                    int port = 25565;
                    if (ip.contains(":")) {
                        String[] parts = ip.split(":");
                        host = parts[0].trim();
                        port = Integer.parseInt(parts[1].trim());
                    } else {
                        host = ip;
                    }
                    cmd.add("--server");
                    cmd.add(host);
                    cmd.add("--port");
                    cmd.add(String.valueOf(port));
                }

                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.directory(new File(PROJECT_DIR));
                pb.redirectErrorStream(true);
                process = pb.start();
                long pid = process.pid();
                try { java.nio.file.Files.writeString(new java.io.File("E:\\Мои Сурсы\\harmony\\bot_kill_diag.txt").toPath(), "  ADDED PID " + pid + " for " + nick + "\n", java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND); } catch (Exception e) {}
                synchronized (botProcesses) { botProcesses.add(process); }

                File logFile = new File(PROJECT_DIR, "bot_" + nick + "_" + System.currentTimeMillis() + ".log");
                try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
                     java.io.FileWriter writer = new java.io.FileWriter(logFile)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        writer.write(line + System.lineSeparator());
                        writer.flush();
                    }
                }

                process.waitFor();
            } catch (Exception ex) {
                String msg = ex.getMessage() != null ? ex.getMessage() : "Unknown (" + ex.getClass().getSimpleName() + ")";
                String finalMsg = msg;
                net.minecraft.client.Minecraft.getInstance().execute(() -> {
                    if (mc.player != null) {
                        mc.player.sendMessage(new StringTextComponent("§c[BotAttack] Error: " + finalMsg), mc.player.getUniqueID());
                    }
                });
            } finally {
                long pid = process != null ? process.pid() : -1;
                try { java.nio.file.Files.writeString(new java.io.File("E:\\Мои Сурсы\\harmony\\bot_kill_diag.txt").toPath(), "  REMOVED PID " + pid + " in finally\n", java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND); } catch (Exception e) {}
                synchronized (botProcesses) { botProcesses.remove(process); }
            }
        }, "Bot-Launcher-" + nick);
        launcher.setDaemon(true);
        launcher.start();

        net.minecraft.client.Minecraft.getInstance().execute(() -> {
            if (mc.player != null) {
                mc.player.sendMessage(new StringTextComponent("§a[BotAttack] Launched Harmony bot: " + nick), mc.player.getUniqueID());
            }
        });
    }

    private void saveModuleConfig() {
        try {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null) {
                JsonObject config = new Config("bot").saveConfig().getAsJsonObject();
                JsonObject modules = config.getAsJsonObject("modules");
                if (modules != null) {
                    modules.remove("botattack");
                }
                Files.writeString(
                    new File(MODULE_CONFIG_FILE).toPath(),
                    new GsonBuilder().setPrettyPrinting().create().toJson(config),
                    StandardCharsets.UTF_8
                );
            }
        } catch (Exception ignored) {}
    }

    private String findJavaExecutable() {
        String javaHome = System.getProperty("java.home");
        String exe = javaHome + "\\bin\\javaw.exe";
        if (new File(exe).exists()) return exe;
        return "javaw.exe";
    }

    private String buildClasspath() {
        StringBuilder cp = new StringBuilder();
        cp.append(BOTMODE_DIR).append("\\out\\production\\botmode");
        cp.append(";").append(PROJECT_DIR).append("\\out\\production\\client");

        File libDir = new File(PROJECT_DIR + "\\libraries");
        File[] jars = libDir.listFiles((dir, name) -> name.endsWith(".jar") 
                && !name.contains("voicechat") 
                && !name.toLowerCase().contains("optifine"));
        if (jars != null) {
            for (File jar : jars) {
                cp.append(";").append(jar.getAbsolutePath());
            }
        }
        return cp.toString();
    }

    private String generateNick(int index) {
        if (nicknameMode.is("Custom")) {
            return generateCustomNick(index);
        } else if (nicknameMode.is("Custom List")) {
            String[] names = nicknames.get().split(",");
            return index < names.length ? names[index].trim() : generateRandomNick();
        }
        return generateRandomNick();
    }

    private String generateCustomNick(int index) {
        String base = customNick.get().trim();
        if (base.isEmpty()) base = "Bot";
        return base + (index + 1);
    }

    private String generateRandomNick() {
        String[] prefixes = {"Bot", "Player", "Noob", "Pro", "Cool", "xD", "Legend", "Mini", "Ultra", "Super"};
        String[] suffixes = {"_XD", "_Pro", "_123", "_MC", "_BW", "_OP", "_YT", "MC", "PVP", "HD"};
        return prefixes[random.nextInt(prefixes.length)]
                + (100 + random.nextInt(900))
                + suffixes[random.nextInt(suffixes.length)];
    }

    private long lastControlSync;
    private void syncControlFile() {
        if (System.currentTimeMillis() - lastControlSync < 1000) return;
        lastControlSync = System.currentTimeMillis();
        try {
            java.util.List<String> lines = new java.util.ArrayList<>();
            java.io.File f = new java.io.File(CONTROL_FILE);
            if (f.exists()) {
                lines = new java.util.ArrayList<>(java.nio.file.Files.readAllLines(f.toPath()));
            }

            java.util.Map<String, String> settings = new java.util.LinkedHashMap<>();
            settings.put("autoSkin", String.valueOf(botAutoSkin.get()));
            settings.put("skinName", botSkinName.get());
            settings.put("autoRegister", String.valueOf(botAutoRegister.get()));
            settings.put("registerPassword", botRegisterPassword.get());
            // Persistent toggle: bot enters BedWars autonomously via compass whenever enabled.
            settings.put("bwenter", String.valueOf(botBWEnter.get()));
            // BWAutoLeave is handled via module config (like HitAura, Velocity, etc.)
            // Persistent toggle: bot clicks green glass in hub chest to leave the game.
            settings.put("bwenterleave", String.valueOf(botBWEnterLeave.get()));
            // BedWars AI settings
            settings.put("bwai", String.valueOf(botBedWarsAI.get()));
            settings.put("llm_strategist", String.valueOf(bwLlmStrategist.get()));
            settings.put("team_ai", String.valueOf(bwTeamAI.get()));
            settings.put("team_id", bwTeamId.get());
            settings.put("team_roles", bwTeamRoles.get());
            settings.put("team_decision_seconds", String.valueOf(bwTeamDecisionSeconds.get().intValue()));
            settings.put("bwai_strategy", bwStrategy.get());
            settings.put("bwai_maxtarget", bwMaxTarget.get());
            settings.put("bwai_extrairon_enabled", String.valueOf(bwExtraIronEnabled.get()));
            settings.put("bwai_extrairon", String.valueOf(bwExtraIron.get().intValue()));
            settings.put("bwai_extragold_enabled", String.valueOf(bwExtraGoldEnabled.get()));
            settings.put("bwai_extragold", String.valueOf(bwExtraGold.get().intValue()));
            settings.put("bwai_buydelay", String.valueOf(bwBuyDelay.get().intValue()));
            settings.put("bwai_bridgeblocks", String.valueOf(bwBridgeBlocks.get().intValue()));
            settings.put("bwai_fightrange", String.valueOf(bwFightRange.get()));
            settings.put("bwai_collectradius", String.valueOf(bwCollectRadius.get()));
            settings.put("bwai_gendistance", String.valueOf(bwGenDistance.get()));
            settings.put("bwai_defendbed", String.valueOf(bwAutoDefendBed.get()));
            settings.put("bwai_buyarmor", String.valueOf(bwBuyArmor.get()));
            settings.put("bwai_buysword", String.valueOf(bwBuySword.get()));
            settings.put("bwai_buypickaxe", String.valueOf(bwBuyPickaxe.get()));
            settings.put("bwai_buyblocks", String.valueOf(bwBuyBlocks.get()));
            settings.put("bwai_onlyoneblock", String.valueOf(bwOnlyOneBlock.get()));
            settings.put("bwai_baritone", String.valueOf(bwBaritoneNav.get()));
            settings.put("bwinv_enabled", String.valueOf(botInvManager.get()));
            settings.put("bwinv_mode", botInvMode.get());
            settings.put("bwinv_mindelay", String.valueOf(botInvMinDelay.get().intValue()));
            settings.put("bwinv_maxdelay", String.valueOf(botInvMaxDelay.get().intValue()));
            settings.put("bwinv_autoarmor", String.valueOf(botInvAutoArmor.get()));
            settings.put("bwinv_armorhotbar", String.valueOf(botInvArmorHotbar.get()));
            settings.put("bwinv_dropgarbage", String.valueOf(botInvDropGarbage.get()));
            settings.put("bwinv_garbagehotbar", String.valueOf(botInvGarbageHotbar.get()));
            settings.put("bwinv_blockorder", botInvBlockOrder.get());
            settings.put("bwinv_slot1", botInvSlot1.get());
            settings.put("bwinv_slot2", botInvSlot2.get());
            settings.put("bwinv_slot3", botInvSlot3.get());
            settings.put("bwinv_slot4", botInvSlot4.get());
            settings.put("bwinv_slot5", botInvSlot5.get());
            settings.put("bwinv_slot6", botInvSlot6.get());
            settings.put("bwinv_slot7", botInvSlot7.get());
            settings.put("bwinv_slot8", botInvSlot8.get());
            settings.put("bwinv_slot9", botInvSlot9.get());
            settings.put("scaffold", String.valueOf(botScaffold.get()));
            // Neural brain (BotBrain)
            settings.put("brain", String.valueOf(bwAIBrain.get()));
            settings.put("brainchat", String.valueOf(bwAIChat.get()));
            settings.put("brainrecord", String.valueOf(bwAIRecord.get()));
            if (!pendingBWJoin.isEmpty()) {
                settings.put("bwjoin", pendingBWJoin);
                pendingBWJoin = "";
            }

            String followLine = null;
            if (afterSpawn.is("Follow Me") && mc.player != null) {
                followLine = "follow=" + mc.player.getName().getString() + ":" + followDistance.get();
            }

            for (java.util.Map.Entry<String, String> entry : settings.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                boolean found = false;
                for (int i = 0; i < lines.size(); i++) {
                    if (lines.get(i).startsWith(key + "=")) {
                        lines.set(i, key + "=" + value);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    lines.add(key + "=" + value);
                }
            }

            boolean foundFollow = false;
            if (followLine != null) {
                for (int i = 0; i < lines.size(); i++) {
                    if (lines.get(i).startsWith("follow=")) {
                        lines.set(i, followLine);
                        foundFollow = true;
                        break;
                    }
                }
                if (!foundFollow) lines.add(followLine);
            } else {
                lines.removeIf(l -> l.startsWith("follow="));
            }

            java.nio.file.Files.write(
                    java.nio.file.Paths.get(CONTROL_FILE),
                    lines,
                    java.nio.charset.StandardCharsets.UTF_8
            );

            syncModuleConfig();
        } catch (Exception ignored) {
        }
    }

    private void syncModuleConfig() {
        try {
            java.io.File configFile = new java.io.File(MODULE_CONFIG_FILE);
            if (!configFile.exists()) return;

            String content = java.nio.file.Files.readString(configFile.toPath(), StandardCharsets.UTF_8);
            JsonObject json = new JsonParser().parse(content).getAsJsonObject();
            JsonObject modules = json.getAsJsonObject("modules");
            if (modules == null) return;

            JsonObject fullConfig = new Config("bot").saveConfig().getAsJsonObject();
            JsonObject allModules = fullConfig.getAsJsonObject("modules");
            if (allModules == null) return;

            boolean changed = false;

            if (botHitAura.get()) {
                JsonObject src = allModules.getAsJsonObject("hitaura");
                if (src != null) {
                    JsonObject copy = new JsonParser().parse(src.toString()).getAsJsonObject();
                    copy.addProperty("state", true);
                    modules.add("hitaura", copy); changed = true;
                }
            } else {
                JsonObject src = allModules.getAsJsonObject("hitaura");
                if (src != null) {
                    JsonObject copy = new JsonParser().parse(src.toString()).getAsJsonObject();
                    copy.addProperty("state", false);
                    modules.add("hitaura", copy); changed = true;
                }
            }
            if (botSprint.get()) {
                JsonObject src = allModules.getAsJsonObject("autosprint");
                if (src != null) {
                    JsonObject copy = new JsonParser().parse(src.toString()).getAsJsonObject();
                    copy.addProperty("state", true);
                    modules.add("autosprint", copy); changed = true;
                }
            } else {
                JsonObject src = allModules.getAsJsonObject("autosprint");
                if (src != null) {
                    JsonObject copy = new JsonParser().parse(src.toString()).getAsJsonObject();
                    copy.addProperty("state", false);
                    modules.add("autosprint", copy); changed = true;
                }
            }
            if (botScaffold.get()) {
                JsonObject src = allModules.getAsJsonObject("scaffold");
                if (src != null) {
                    JsonObject copy = new JsonParser().parse(src.toString()).getAsJsonObject();
                    copy.addProperty("state", true);
                    modules.add("scaffold", copy); changed = true;
                }
            } else {
                JsonObject src = allModules.getAsJsonObject("scaffold");
                if (src != null) {
                    JsonObject copy = new JsonParser().parse(src.toString()).getAsJsonObject();
                    copy.addProperty("state", false);
                    modules.add("scaffold", copy); changed = true;
                }
            }
            if (botVelocity.get()) {
                JsonObject src = allModules.getAsJsonObject("velocity");
                if (src != null) {
                    JsonObject copy = new JsonParser().parse(src.toString()).getAsJsonObject();
                    copy.addProperty("state", true);
                    modules.add("velocity", copy); changed = true;
                }
            } else {
                JsonObject src = allModules.getAsJsonObject("velocity");
                if (src != null) {
                    JsonObject copy = new JsonParser().parse(src.toString()).getAsJsonObject();
                    copy.addProperty("state", false);
                    modules.add("velocity", copy); changed = true;
                }
            }
            if (botInvManager.get()) {
                JsonObject src = allModules.getAsJsonObject("invmanager");
                if (src != null) {
                    JsonObject copy = new JsonParser().parse(src.toString()).getAsJsonObject();
                    copy.addProperty("state", true);
                    modules.add("invmanager", copy); changed = true;
                }
            } else {
                JsonObject src = allModules.getAsJsonObject("invmanager");
                if (src != null) {
                    JsonObject copy = new JsonParser().parse(src.toString()).getAsJsonObject();
                    copy.addProperty("state", false);
                    modules.add("invmanager", copy); changed = true;
                }
            }
            if (botBWJoinHelper.get()) {
                JsonObject src = allModules.getAsJsonObject("bwjoinhelper");
                if (src != null) {
                    JsonObject copy = new JsonParser().parse(src.toString()).getAsJsonObject();
                    copy.addProperty("state", true);
                    modules.add("bwjoinhelper", copy); changed = true;
                }
            } else {
                JsonObject src = allModules.getAsJsonObject("bwjoinhelper");
                if (src != null) {
                    JsonObject copy = new JsonParser().parse(src.toString()).getAsJsonObject();
                    copy.addProperty("state", false);
                    modules.add("bwjoinhelper", copy); changed = true;
                }
            }
            if (botBWAutoLeave.get()) {
                JsonObject src = allModules.getAsJsonObject("bwautoleave");
                if (src != null) {
                    JsonObject copy = new JsonParser().parse(src.toString()).getAsJsonObject();
                    copy.addProperty("state", true);
                    modules.add("bwautoleave", copy); changed = true;
                }
            } else {
                JsonObject src = allModules.getAsJsonObject("bwautoleave");
                if (src != null) {
                    JsonObject copy = new JsonParser().parse(src.toString()).getAsJsonObject();
                    copy.addProperty("state", false);
                    modules.add("bwautoleave", copy); changed = true;
                }
            }
            if (botBedWarsAI.get()) {
                JsonObject src = allModules.getAsJsonObject("bedwarsai");
                if (src != null) {
                    JsonObject copy = new JsonParser().parse(src.toString()).getAsJsonObject();
                    copy.addProperty("state", true);
                    modules.add("bedwarsai", copy); changed = true;
                }
            } else {
                JsonObject src = allModules.getAsJsonObject("bedwarsai");
                if (src != null) {
                    JsonObject copy = new JsonParser().parse(src.toString()).getAsJsonObject();
                    copy.addProperty("state", false);
                    modules.add("bedwarsai", copy); changed = true;
                }
            }

            if (changed) {
                java.nio.file.Files.writeString(configFile.toPath(), GSON.toJson(json), StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {
        }
    }
}
