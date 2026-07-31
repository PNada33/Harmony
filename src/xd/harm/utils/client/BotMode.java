package xd.harm.utils.client;

import com.google.common.eventbus.Subscribe;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.server.SChatPacket;
import net.minecraft.network.play.server.SWindowItemsPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import xd.harm.Harmony;
import xd.harm.events.input.EventInput;
import xd.harm.events.input.EventKey;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.impl.misc.AutoBuyMLegacy;
import xd.harm.events.movement.EventMotion;
import xd.harm.events.network.EventPacket;
import net.minecraft.network.play.server.STitlePacket;
import xd.harm.events.world.EventChangeWorld;
import xd.harm.events.world.TickEvent;
import xd.harm.utils.player.MoveUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static xd.harm.utils.client.IMinecraft.mc;

import xd.harm.bot.brain.BotBrain;
import xd.harm.bot.brain.BotBrainChat;
import xd.harm.bot.brain.BotBrainDecision;
import xd.harm.bot.brain.BotBrainState;

import xd.harm.baritone.api.BaritoneAPI;
import xd.harm.baritone.api.Settings;
import xd.harm.baritone.api.pathing.goals.GoalNear;
import xd.harm.baritone.api.behavior.IPathingBehavior;

public class BotMode {

    private static final Path CONTROL_FILE = Paths.get("E:\\Мои Сурсы\\harmony\\bot_control.txt");
    private static final Path MODULE_CONFIG_PATH = Paths.get("E:\\Мои Сурсы\\harmony\\bot_module_config.json");
    private static long moduleConfigLastModified;
    private static boolean applied;
    private static boolean isFollowing;
    private static String followTarget;
    private static double followDistance = 0.5;
    private static boolean sprintEnabled = false;
    private static boolean autoSkinEnabled = false;
    private static String autoSkinName = "";
    private static int skinTickCounter;
    private static boolean skinSent;
    private static boolean autoRegisterEnabled = false;
    private static String registerPassword = "";
    private static String lastBWJoinNumber = "";
    private static boolean bwEnterEnabled = false;
    private static boolean bwEntered = false;
    // 0 = idle, 2 = waiting for chest GUI after right-clicking compass
    private static int bwEnterState = 0;
    private static int bwEnterTimer = 0;

    // BW AutoEnterLeave: separate flag — writes /leave, then clicks green glass.
    private static boolean bwEnterLeaveEnabled = false;
    private static int bwEnterLeaveTimer = 0;
    private static int bwEnterLeaveStopDelay = 0;

    // BedWars AI
    private static boolean bwAIEnabled = false;
    private static String bwAIStrategy = "Balanced";
    private static boolean bwLlmStrategistEnabled = false;
    private static int bwLlmTick = 0;
    private static boolean bwTeamAIEnabled = false;
    private static String bwTeamId = System.getProperty("bot.team", "LionsTempleTeam");
    private static String bwTeamRole = System.getProperty("bot.role", "Auto");
    private static int bwTeamDecisionSeconds = 5;
    private static String bwTeamLastRole = "";
    private static String bwAIMaxTarget = "30i 6g";
    private static boolean bwAIExtraIronEnabled = false;
    private static int bwAIExtraIron = 1;
    private static boolean bwAIExtraGoldEnabled = false;
    private static int bwAIExtraGold = 1;
    private static int bwAIBuyDelay = 10;
    private static int bwAIBridgeBlocks = 64;
    private static float bwAIFightRange = 4.0f;
    private static float bwAICollectRadius = 8.0f;
    private static boolean bwAIAutoDefendBed = false;
    private static boolean bwAIBuyArmor = true;
    private static boolean bwAIBuySword = true;
    private static boolean bwAIBuyPickaxe = true;
    private static boolean bwAIBuyBlocks = true;
    private static boolean bwAIOnlyOneBlock = false;

    // v10: локальный порт InvManager для ботов. Он запускается строго после закупки.
    private static boolean bwInvEnabled = true;
    private static String bwInvMode = "Оба";
    private static int bwInvMinDelay = 50;
    private static int bwInvMaxDelay = 150;
    private static boolean bwInvAutoArmor = true;
    private static boolean bwInvArmorHotbar = true;
    private static boolean bwInvDropGarbage = false;
    private static boolean bwInvGarbageHotbar = false;
    private static String bwInvBlockOrder = "Уменьшение";
    private static final String[] bwInvSlots = {"Меч", "Кирка", "Ничего", "Ничего", "Ничего", "Блоки", "Ничего", "Ничего", "Ничего"};
    private static long bwInvLastAction = 0L;
    private static int bwInvQuietTicks = 0;
    private static int bwInvNextPhase = 2;

    // v9: несколько точек края базы + жёсткий гейт строительства
    private static java.util.Map<String, java.util.List<double[]>> mapBridgeStartsMulti = new java.util.HashMap<>();
    private static net.minecraft.util.math.BlockPos bwAISelectedBridgeStart = null;
    private static boolean bwAIBridgeStartReached = false;
    private static net.minecraft.util.math.BlockPos bwAIBridgeAxisTarget = null;
    // v12: подтверждаем каждый блок серверным обновлением мира до следующего шага.
    private static net.minecraft.util.math.BlockPos bwBridgePendingSupport = null;
    private static int bwBridgeSupportStableTicks = 0;
    private static int bwBridgeLastPlaceTick = -100;
    private static int bwBridgeStartReachedTick = -100;
    // v33: зафиксированная ось моста: 0 = нет, 1 = X, 2 = Z
    private static int bwBridgeLockedAxis = 0;
    // v34: сколько тиков стоим у края, а Scaffold так и не поставил опору
    private static int bwBridgeScaffoldStuckTicks = 0;
    // v27: следование по уже построенному мосту союзника и самостоятельный обгон,
    // если союзник остановился/у него закончились блоки.
    private static int bwBridgeLeaderNoStepTicks = 0;
    private static net.minecraft.util.math.BlockPos bwBridgeTakeoverTarget = null;
    private static boolean bwBridgeFollowingExisting = false;

    // v9: состояние закупки (дословный порт AutoBuyMLegacy)
    private static long botBuyLastAction = 0L;
    private static boolean botBuySessionActive = false;
    private static int botBuyWindowId = -1;
    private static int botBuyIndex = 0;
    private static int botBuyStep = 0;
    private static int botBuyAttempts = 0;
    private static boolean botBuyWaitingBlocks = false;
    private static int botBuyIronBefore = 0;
    private static int botBuyDetectedCost = 0;
    private static long botBuyClickTime = 0L;
    private static java.util.List<Object[]> botBuyPlan = new java.util.ArrayList<>();
    private static boolean bwScaffoldEnabled = false;

    // v21: отдельный подъём на центральный остров. На LionsTemple прямой мост
    // приходит под земляной выступ, поэтому сначала даём боту место для лестницы.
    private static boolean bwMidClimbActive = false;
    private static int bwMidClimbStage = 0; // 0 = отойти от стены, 1 = лестница к центру
    private static net.minecraft.util.math.BlockPos bwMidClimbAnchor = null;
    private static double bwMidClimbBestY = 0.0;
    private static int bwMidClimbLastRiseTick = 0;
    private static int bwMidClimbStartedTick = 0;

    // Baritone-навигация для ботов
    private static boolean bwBaritoneNav = false;
    private static boolean bwBaritoneInited = false;
    private static net.minecraft.util.math.BlockPos bwBaritoneCurGoal = null;
    // Под-режим фазы моста: false = ведёт Baritone (земля), true = мостим Scaffold (бездна)
    private static boolean bwBridgeScaffoldMode = false;
    // Устойчивость: тик, когда выдали цель Baritone, и флаг «Baritone не справился»
    private static int bwBaritoneGoalTick = 0;
    private static boolean bwBaritoneFailed = false;
    private static int bwBaritoneDiagTick = 0;

    // Neural Brain (BotBrain)
    private static boolean bwAIBrainEnabled = false;
    private static boolean bwAIChatEnabled = false;
    private static boolean bwAIRecordEnabled = false;
    private static xd.harm.bot.brain.BotBrainDecision lastBrainDecision = null;
    private static long lastBrainChatTick = 0;
    private static int bwAIBrainEmeraldWait = 0;
    private static int bwAIBrainDiamondWait = 0;

    // Сохранение генераторов по картам
    private static final String GENERATORS_FILE = "E:\\Мои Сурсы\\harmony\\bw_generators.json";
    private static java.util.Map<String, double[]> generatorPositions = new java.util.HashMap<>();
    private static java.util.Map<String, double[]> mapCenters = new java.util.HashMap<>();
    private static java.util.Map<String, double[]> mapBridgeStarts = new java.util.HashMap<>();
    private static String currentMapName = "";
    private static double[] currentGeneratorPos = null;
    private static boolean bwAIGoingToGenerator = false;
    private static String chatMapName = "";
    private static boolean bwGameStarted = false;
    private static int bwGameStartTick = 0;
    private static boolean spawnDetected = false;
    private static float bwGenDistance = 1.0f;
    private static float bwAISpawnYaw = 0f;

    private static final int BWAI_IDLE = 0;
    private static final int BWAI_BUY_BLOCKS = 1;
    private static final int BWAI_BRIDGE = 2;
    private static final int BWAI_COLLECT = 3;
    private static final int BWAI_BUY_GEAR = 4;
    private static final int BWAI_FIGHT = 5;
    private static final int BWAI_DEFEND_BED = 6;
    private static final int BWAI_RETURN_BASE = 7;
    private static final int BWAI_BUY_GEAR_MID = 8;
    private static final int BWAI_COLLECT_SPAWN = 9;
    private static final int BWAI_BUY_SETUP = 10;
    private static final int BWAI_INVENTORY = 11;

    private static int bwAIPhase = BWAI_IDLE;
    private static int bwAITickCount = 0;
    private static int bwAIPhaseTicks = 0;
    private static net.minecraft.util.math.BlockPos bwAIBasePos = null;
    private static net.minecraft.util.math.BlockPos bwAIMidPos = null;
    private static net.minecraft.util.math.BlockPos bwAIBedPos = null;
    private static boolean bwAIShopOpen = false;
    private static int bwAIShopTicks = 0;
    private static int bwAIBuyAttempts = 0;
    private static int bwAIIronCount = 0;
    private static int bwAIGoldCount = 0;
    private static int bwAIEmeraldCount = 0;
    private static int bwAIDiamondCount = 0;
    private static int bwAIWoolCount = 0;
    private static net.minecraft.entity.player.PlayerEntity bwAITargetEnemy = null;
    private static int bwAIFightTicks = 0;
    private static int bwAIBridgeBlocksPlaced = 0;
    private static boolean bwAIStuck = false;
    private static int bwAISpawnWaitTicks = 0;
    private static boolean bwAIAutoBuyStarted = false;

    // v26: вход на центр подтверждается пересечением сохранённого края со стороны своей базы.
    // После этого бот идёт к изумруду по цепочке точек, а не одной прямой через рельеф/листву.
    private static boolean bwCenterConfirmed = false;
    // 0 = маршрут к изумруду, 1 = ждём изумруд, 2 = обратный маршрут, 3 = ждём алмазный маршрут
    private static int bwCenterResourceStage = 0;
    private static int bwEmeraldCountOnArrival = 0;
    private static int bwEmeraldWaitTicks = 0;
    private static String bwCenterEntrySide = "";
    private static java.util.List<net.minecraft.util.math.BlockPos> bwCenterRoute = new java.util.ArrayList<>();
    private static int bwCenterRouteIndex = 0;
    private static int bwCenterWaypointStartTick = 0;

    // === Загрузка/сохранение генераторов по картам ===

    private static void loadGenerators() {
        try {
            java.io.File f = new java.io.File(GENERATORS_FILE);
            if (!f.exists()) return;
            String content = new String(java.nio.file.Files.readAllBytes(f.toPath()), java.nio.charset.StandardCharsets.UTF_8);
            com.google.gson.JsonObject json = new com.google.gson.JsonParser().parse(content).getAsJsonObject();
            for (java.util.Map.Entry<String, com.google.gson.JsonElement> entry : json.entrySet()) {
                String key = entry.getKey();
                if (!entry.getValue().isJsonArray()) continue;
                com.google.gson.JsonArray arr = entry.getValue().getAsJsonArray();
                if (arr.size() >= 3) {
                    double[] pos = new double[]{
                        arr.get(0).getAsDouble(),
                        arr.get(1).getAsDouble(),
                        arr.get(2).getAsDouble()
                    };
                    if (key.endsWith("_center")) {
                        mapCenters.put(key.replace("_center", ""), pos);
                    } else if (key.matches(".*_bridge\\d+$")) {
                        String baseKey = key.replaceAll("_bridge\\d+$", "");
                        mapBridgeStartsMulti.computeIfAbsent(baseKey, k -> new java.util.ArrayList<>()).add(pos);
                    } else if (key.endsWith("_bridge")) {
                        mapBridgeStarts.put(key.replace("_bridge", ""), pos);
                    } else {
                        generatorPositions.put(key, pos);
                    }
                }
            }
            System.out.println("[BotMode] Loaded " + generatorPositions.size() + " generators, " + mapCenters.size() + " centers");
        } catch (Exception e) {
            System.out.println("[BotMode] Failed to load generators: " + e.getMessage());
        }
    }

    private static void saveGenerators() {
        try {
            com.google.gson.JsonObject json = new com.google.gson.JsonObject();
            for (java.util.Map.Entry<String, double[]> entry : generatorPositions.entrySet()) {
                com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
                arr.add(entry.getValue()[0]);
                arr.add(entry.getValue()[1]);
                arr.add(entry.getValue()[2]);
                json.add(entry.getKey(), arr);
            }
            // Не теряем центр и нижнюю точку моста при повторном сохранении генератора.
            for (java.util.Map.Entry<String, double[]> entry : mapCenters.entrySet()) {
                com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
                arr.add(entry.getValue()[0]);
                arr.add(entry.getValue()[1]);
                arr.add(entry.getValue()[2]);
                json.add(entry.getKey() + "_center", arr);
            }
            for (java.util.Map.Entry<String, double[]> entry : mapBridgeStarts.entrySet()) {
                com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
                arr.add(entry.getValue()[0]);
                arr.add(entry.getValue()[1]);
                arr.add(entry.getValue()[2]);
                json.add(entry.getKey() + "_bridge", arr);
            }
            for (java.util.Map.Entry<String, java.util.List<double[]>> entry : mapBridgeStartsMulti.entrySet()) {
                int i = 1;
                for (double[] pos : entry.getValue()) {
                    com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
                    arr.add(pos[0]); arr.add(pos[1]); arr.add(pos[2]);
                    json.add(entry.getKey() + "_bridge" + i++, arr);
                }
            }
            java.nio.file.Files.write(
                new java.io.File(GENERATORS_FILE).toPath(),
                json.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)
            );
            System.out.println("[BotMode] Saved " + generatorPositions.size() + " generator positions");
        } catch (Exception e) {
            System.out.println("[BotMode] Failed to save generators: " + e.getMessage());
        }
    }

    // Сохранить текущую позицию как генератор для текущей карты
    public static void saveCurrentGenerator() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        String mapName = detectMapName();
        if (mapName.isEmpty()) {
            System.out.println("[BotMode] Cannot detect map name");
            return;
        }
        double[] pos = new double[]{
            mc.player.getPosX(),
            mc.player.getPosY(),
            mc.player.getPosZ()
        };
        generatorPositions.put(mapName, pos);
        saveGenerators();
        System.out.println("[BotMode] Saved generator for " + mapName + ": " + (int)pos[0] + ", " + (int)pos[1] + ", " + (int)pos[2]);
    }

    // Определить имя текущей карты из scoreboard
    // LionsTemple: red spawn ~(14397,34,14475), generator ~(14398,33,14478)
    // Проверяем координаты спавна — если близко к известным точкам, ставим генератор
    private static void detectMapBySpawn(Minecraft mc) {
        if (bwAIBasePos == null) return;
        double x = bwAIBasePos.getX();
        double y = bwAIBasePos.getY();
        double z = bwAIBasePos.getZ();

        // LionsTemple: spawn зона ~14390-14420, Y=34, Z=14460-14500
        if (x > 14380 && x < 14420 && y > 25 && y < 50 && z > 14460 && z < 14500) {
            currentGeneratorPos = new double[]{14398, 33, 14478};
            currentMapName = "LionsTemple";
            spawnDetected = true;
            System.out.println("[BotMode] Spawn-based map: LionsTemple! Generator at 14398,33,14478");
            return;
        }

        // Координаты неизвестны — сервер телепортирует в отдельный мир
        // Генератор всегда прямо перед спавном (~3-5 блоков по yaw)
        System.out.println("[BotMode] Unknown spawn: " + (int)x + "," + (int)y + "," + (int)z + " — using forward walk");
        spawnDetected = true;
    }

    private static String detectMapName() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.world == null) return "";
        try {
            net.minecraft.scoreboard.Scoreboard scoreboard = mc.world.getScoreboard();
            net.minecraft.scoreboard.ScoreObjective sidebar = scoreboard.getObjectiveInDisplaySlot(1);
            if (sidebar == null) {
                if (bwAITickCount % 100 == 0) System.out.println("[BotMode] detectMap: no sidebar objective");
                return "";
            }

            String objName = sidebar.getDisplayName().getString();
            if (bwAITickCount % 100 == 0) System.out.println("[BotMode] detectMap: objective=[" + objName + "]");

            // Проверяем — BEDWARS в objective?
            String objLower = objName.toLowerCase(java.util.Locale.ROOT);
            boolean isBedwars = objLower.contains("bedwars") || objLower.contains("bed war");
            if (!isBedwars) {
                // Фолбэк: проверяем чат — может уже видели "Карта:" в чате
                return chatMapName;
            }

            // Ищем "Карта:" в строках сайдбара
            java.util.Collection<net.minecraft.scoreboard.Score> scores = scoreboard.getSortedScores(sidebar);
            if (bwAITickCount % 100 == 0) System.out.println("[BotMode] detectMap: sidebar scores count=" + scores.size());
            int lineNum = 0;
            for (net.minecraft.scoreboard.Score score : scores) {
                String raw = score.getPlayerName();
                lineNum++;
                if (bwAITickCount % 100 == 0 && lineNum <= 5) {
                    System.out.println("[BotMode] sidebar[" + lineNum + "] raw=[" + raw + "] lower=[" + raw.toLowerCase(java.util.Locale.ROOT) + "]");
                }
                if (raw.toLowerCase(java.util.Locale.ROOT).contains("карт") || raw.toLowerCase(java.util.Locale.ROOT).contains("map")) {
                    String map = raw.replaceAll("(?i).*?(?:карта|map)[:\\s]+", "").replaceAll("[^\\x20-\\x7E\\u0400-\\u04FF]", "").trim();
                    if (!map.isEmpty()) {
                        System.out.println("[BotMode] Detected map: [" + map + "]");
                        return map;
                    }
                }
            }
        } catch (Exception e) { System.out.println("[BotMode] detectMapName error: " + e.getMessage()); }
        return "";
    }

    // Загрузить позицию генератора для текущей карты
    private static void loadGeneratorForCurrentMap() {
        String mapName = detectMapName();
        // Фолбэк: имя из чата
        if (mapName.isEmpty() && !chatMapName.isEmpty()) {
            mapName = chatMapName;
            System.out.println("[BotMode] Using chat map name: " + mapName);
        }
        if (mapName.isEmpty()) return;

        currentMapName = mapName;
        if (generatorPositions.containsKey(mapName)) {
            currentGeneratorPos = generatorPositions.get(mapName);
        } else {
            for (java.util.Map.Entry<String, double[]> entry : generatorPositions.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(mapName)) {
                    currentGeneratorPos = entry.getValue();
                    break;
                }
            }
        }
        if (currentGeneratorPos != null) {
            System.out.println("[BotMode] Generator for " + mapName + ": " + (int)currentGeneratorPos[0] + ", " + (int)currentGeneratorPos[1] + ", " + (int)currentGeneratorPos[2]);
        } else {
            System.out.println("[BotMode] No saved generator for " + mapName + " — use /savegen to save one");
        }
    }

    public static void init() {
        if (!Boolean.getBoolean("bot.mode")) {
            return;
        }
        System.out.println("[BotMode] Initializing bot mode...");

        apply();
        disableRenderModules();
        loadGenerators();
        Harmony.getInstance().getEventBus().register(new BotMode());
    }

    private static void disableRenderModules() {
        try {
            Harmony.getInstance().getModuleManager().getModules().stream()
                .filter(m -> m.getCategory() == Category.Render)
                .forEach(m -> {
                    if (m.isState()) {
                        m.setState(false, true);
                        System.out.println("[BotMode] Disabled render module: " + m.getName());
                    }
                });
        } catch (Exception e) {
            System.out.println("[BotMode] disableRenderModules failed: " + e.getMessage());
        }
    }

    @Subscribe
    public void onKey(EventKey e) {
        if (!Boolean.getBoolean("bot.mode")) return;
        if (e.getKey() == 344) {
            System.out.println("[BotMode] RShift pressed — shutting down bot");
            Minecraft.getInstance().shutdown();
        }
    }

    @Subscribe
    public void onWorldChange(EventChangeWorld e) {
        if (!Boolean.getBoolean("bot.mode")) return;
        setFps(30);
        skinTickCounter = 0;
        skinSent = false;
        bwEntered = false;
        bwEnterState = 0;
        bwEnterTimer = 0;
        // Сброс BedWars AI
        bwAIPhase = BWAI_IDLE;
        bwAIPhaseTicks = 0;
        bwAISetupStep = 0;
        bwAIShopOpen = false;
        bwAIBedPos = null;
        bwAIMidPos = null;
        currentGeneratorPos = null;
        currentMapName = "";
        chatMapName = "";
        bwGameStarted = false;
        spawnDetected = false;
        bwAIBasePos = null;
        bwAISpawnYaw = 0f;
        bwAIBridgeBlocksPlaced = 0;
        bwCenterConfirmed = false;
        bwCenterResourceStage = 0;
        bwEmeraldCountOnArrival = 0;
        bwEmeraldWaitTicks = 0;
        bwCenterEntrySide = "";
        bwCenterRoute.clear();
        bwCenterRouteIndex = 0;
        bwCenterWaypointStartTick = 0;
        bwBridgePendingSupport = null;
        bwBridgeSupportStableTicks = 0;
        bwBridgeLastPlaceTick = -100;
        bwBridgeStartReachedTick = -100;
        bwBridgeLockedAxis = 0;
        bwBridgeScaffoldStuckTicks = 0;
        bwBridgeLeaderNoStepTicks = 0;
        bwBridgeTakeoverTarget = null;
        bwBridgeFollowingExisting = false;
        resetMidClimb();
        mc.gameSettings.keyBindSneak.setPressed(false);
        mc.gameSettings.keyBindUseItem.setPressed(false);
        bwInvQuietTicks = 0;
        bwInvLastAction = 0L;

        // ВСЕГДА выключаем мешающие модули при смене мира — без проверки bwAIEnabled
        // потому что bwAIEnabled может быть ещё false на первых тиках
        String[] disableOnSpawn = {
            "HitAura", "TriggerBot", "KillAura", "AimAssist", "AutoClicker",
            "TargetStrafe", "Reach", "Scaffold",
            "NoFall", "Fly", "Speed", "Jesus", "Spider", "NoClip",
            "AutoArmor", "ChestStealer", "InventoryCleaner",
            "AntiKnockback", "FastBow", "AutoTotem", "AutoSprint",
            "AutoTool", "CivBreak", "Nuker", "AutoBuild", "LootAura",
            "ChestAura", "Eagle", "SafeWalk", "NoSlow", "Sprint"
        };
        for (String modName : disableOnSpawn) {
            setModuleState(modName, false);
        }
        // Если AI активен — помечаем что мы уже в катке, чтобы tickBWEnter не кликал компас
        if (bwAIEnabled) {
            bwEntered = true;
        }
        // Сбрасываем конфиг чтобы модули не включались обратно
        moduleConfigLastModified = 0;
        System.out.println("[BotMode] World change — disabled ALL interfering modules");
        System.out.println("[BotMode] World change, BedWarsAI reset");
    }

    @Subscribe
    public void onTick(TickEvent e) {
        if (!Boolean.getBoolean("bot.mode")) return;
        Minecraft mc = Minecraft.getInstance();

        readControlFile();

        if (!bwAIEnabled) {
            mc.gameSettings.keyBindSneak.setPressed(false);
            mc.gameSettings.keyBindUseItem.setPressed(false);
        }

        if (bwAIEnabled && mc.player != null) {
            disableAllInterferingModules();
        }

        reloadModuleConfig();

        if (bwAIEnabled && mc.player != null && mc.world != null) {
            tickBotBrain(mc);
        }

        if (mc.player != null && mc.world != null) {
            tickAutoSkin(mc);
            if (!bwAIEnabled) {
                tickBWEnter(mc);
                tickBWEnterLeave(mc);
            }
            if (bwAIEnabled) {
                tickTeamCoordination(mc);
                tickLlmStrategist(mc);
                tickBedWarsAI(mc);
            }
        }
    }

    // Обработка команды /savegen — сохраняет текущую позицию как генератор
    private static boolean saveGenPending = false;

    // Жёсткое отключение ВСЕХ модулей кроме разрешённых
    private static void disableAllInterferingModules() {
        try {
            // Разрешённые модули — НЕ трогаем
            java.util.Set<String> allowed = new java.util.HashSet<>(java.util.Arrays.asList(
                "BotAttack", "BWAutoLeave", "BWJoinHelper", "AutoSkin",
                "AutoRegister", "Notifications", "HUD", "ClickGui",
                "StreamerMode", "ChatHelper", "InvManager",
                "Scaffold"
            ));

            // Выключаем ВСЕ combat и movement модули
            for (Module m : Harmony.getInstance().getModuleManager().getModules()) {
                if (allowed.contains(m.getName())) continue;

                Category cat = m.getCategory();
                if (cat == Category.Combat || cat == Category.Movement || cat == Category.Player) {
                    if (m.isState()) {
                        m.setState(false, true);
                    }
                }
            }
        } catch (Exception e) {}
    }

    private static void tickAutoSkin(Minecraft mc) {
        if (!autoSkinEnabled || skinSent || autoSkinName.isEmpty()) return;
        if (skinTickCounter < 100) {
            skinTickCounter++;
            return;
        }
        mc.player.sendChatMessage("/skin " + autoSkinName);
        skinSent = true;
        System.out.println("[BotMode] AutoSkin: /skin " + autoSkinName);
    }

    private static void tickBWEnter(Minecraft mc) {
        // Only auto-enter from the lobby (no open container).
        if (mc.player.openContainer != null && mc.player.openContainer.windowId != 0) {
            bwEnterTimer = 0;
            return;
        }

        if (!bwEnterEnabled) {
            bwEnterState = 0;
            bwEnterTimer = 0;
            return;
        }

        // Already on a BedWars game — don't spam compass in-game.
        if (bwEntered) return;

        if (bwEnterState == 0) {
            int slot = findItemInHotbar(Items.COMPASS);
            if (slot >= 0) {
                mc.player.inventory.currentItem = slot;
                mc.playerController.processRightClick(mc.player, mc.world, Hand.MAIN_HAND);
                bwEnterState = 2;
                bwEnterTimer = 0;
                System.out.println("[BotMode] BWEnter: right-clicked compass");
            }
        } else if (bwEnterState == 2) {
            // Waiting for the chest GUI to open. Give up after ~3s (60 ticks).
            bwEnterTimer++;
            if (bwEnterTimer > 60) {
                System.out.println("[BotMode] BWEnter: chest GUI didn't open, retrying");
                bwEnterState = 0;
                bwEnterTimer = 0;
            }
        }
    }

    private static void tickBWEnterLeave(Minecraft mc) {
        if (!bwEnterLeaveEnabled) {
            bwEnterLeaveTimer = 0;
            bwEnterLeaveStopDelay = 0;
            return;
        }

        if (bwEnterLeaveStopDelay > 0) {
            bwEnterLeaveStopDelay--;
        }

        if (mc.player.openContainer == null || mc.player.openContainer.windowId == 0) {
            bwEnterLeaveTimer++;
            if (bwEnterLeaveTimer > 100) {
                mc.player.sendChatMessage("/leave");
                bwEnterLeaveTimer = 0;
                bwEnterLeaveStopDelay = 100;
                System.out.println("[BotMode] BWAutoEnterLeave: sent /leave");
            }
        }
    }

    // ==================== LLM-стратег (локальная нейросеть через LM Studio) ====================
    private static void tickTeamCoordination(Minecraft mc) {
        xd.harm.bot.team.TeamCoordinator.configure(bwTeamAIEnabled, bwTeamId, bwTeamRole);
        if (!bwTeamAIEnabled) return;
        xd.harm.bot.team.TeamCoordinator.publishState(mc.player.getName().getString(), currentMapName, bwAIPhase,
                mc.player.getPosX(), mc.player.getPosY(), mc.player.getPosZ(), mc.player.getHealth(), mc.player.getTotalArmorValue(),
                bwAIIronCount, bwAIGoldCount, bwAIEmeraldCount, bwAIDiamondCount, bwAIWoolCount, bwAIBedPos != null);
        String role=xd.harm.bot.team.TeamCoordinator.getAssignedRole();
        if(!role.equals(bwTeamLastRole)){bwTeamLastRole=role;System.out.println("[TeamAI] assigned role="+role);}
    }

    private static void tickLlmStrategist(Minecraft mc) {
        if (!bwLlmStrategistEnabled || mc.player == null || mc.world == null) return;
        if(bwTeamAIEnabled){String shared=xd.harm.bot.team.TeamCoordinator.readSharedStrategy();if(shared!=null)bwAIStrategy=shared;}
        String fresh=xd.harm.bot.brain.LlmStrategist.consumeStrategy();
        if(fresh!=null){if(bwTeamAIEnabled)xd.harm.bot.team.TeamCoordinator.publishStrategy(fresh);bwAIStrategy=fresh;}
        if(bwTeamAIEnabled && !xd.harm.bot.team.TeamCoordinator.isCommander())return;
        bwLlmTick++; int interval=Math.max(40,bwTeamDecisionSeconds*20); if(bwLlmTick%interval!=0)return;
        double nearest=999.0;for(net.minecraft.entity.player.PlayerEntity p:mc.world.getPlayers()){if(p!=mc.player)nearest=Math.min(nearest,p.getDistance(mc.player));}
        String state="здоровье="+(int)mc.player.getHealth()+"/20, фаза="+bwAIPhase+", роль="+(bwTeamAIEnabled?xd.harm.bot.team.TeamCoordinator.getAssignedRole():"solo")
                +", ближайший="+(nearest>900?"нет":(int)nearest)+", стратегия="+bwAIStrategy;
        if(bwTeamAIEnabled)state+="\nКОМАНДА:\n"+xd.harm.bot.team.TeamCoordinator.buildTeamSummary();
        xd.harm.bot.brain.LlmStrategist.requestAsync(state);
    }

    // ==================== BedWars AI ====================

    private static void tickBedWarsAI(Minecraft mc) {
        bwAITickCount++;
        bwAIPhaseTicks++;

        // v9: не паузимся при потере фокуса и сами закрываем Game Menu
        mc.gameSettings.pauseOnLostFocus = false;
        if (mc.currentScreen instanceof net.minecraft.client.gui.screen.IngameMenuScreen) {
            mc.displayGuiScreen(null);
        }

        // v9: сбрасываем выбор края базы вне фазы моста
        if (bwAIPhase != BWAI_BRIDGE) {
            bwAISelectedBridgeStart = null;
            bwAIBridgeStartReached = false;
            bwAIBridgeAxisTarget = null;
            bwBridgePendingSupport = null;
            bwBridgeSupportStableTicks = 0;
            bwBridgeStartReachedTick = -100;
            bwBridgeLockedAxis = 0;
            bwBridgeScaffoldStuckTicks = 0;
            bwBridgeLeaderNoStepTicks = 0;
            bwBridgeTakeoverTarget = null;
            bwBridgeFollowingExisting = false;
            resetMidClimb();
        }

        bwAIIronCount = countItemInInventory(Items.IRON_INGOT);
        bwAIGoldCount = countItemInInventory(Items.GOLD_INGOT);
        bwAIEmeraldCount = countItemInInventory(Items.EMERALD);
        bwAIDiamondCount = countItemInInventory(Items.DIAMOND);
        bwAIWoolCount = countAllWool();

        if (bwAITickCount % 20 == 0) {
            if (bwAIBedPos == null) findBedForAI(mc);
            // Позицию базы ловим ТОЛЬКО когда игра началась и бот стоит на земле —
            // иначе поймаем координаты лобби (напр. Y=110), и Baritone не найдёт путь.
            if (bwAIBasePos == null && bwGameStarted && mc.player.isOnGround()) {
                bwAIBasePos = mc.player.getPosition();
                bwAISpawnYaw = mc.player.rotationYaw;
                System.out.println("[BotMode] Spawn pos: " + bwAIBasePos.getX() + "," + bwAIBasePos.getY() + "," + bwAIBasePos.getZ() + " yaw=" + bwAISpawnYaw);
            }
            System.out.println("[BotMode] BedWarsAI: phase=" + bwAIPhase + " iron=" + bwAIIronCount + " gold=" + bwAIGoldCount + " ticks=" + bwAISpawnWaitTicks);
        }

        detectEnemiesForAI(mc);
        syncBedWarsModules();

        // Baritone: если сейчас не навигационная фаза, но путь активен — отменяем.
        if (bwBaritoneNav && !isBaritoneNavPhase()) {
            cancelBaritone();
        }

        // Диагностика Baritone: раз в секунду в навигационной фазе печатаем состояние пути.
        if (bwBaritoneNav && isBaritoneNavPhase() && bwAITickCount - bwBaritoneDiagTick >= 20) {
            bwBaritoneDiagTick = bwAITickCount;
            try {
                IPathingBehavior pb = BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior();
                boolean pathing = pb != null && pb.isPathing();
                boolean hasPath = pb != null && pb.hasPath();
                boolean hasGoal = pb != null && pb.getGoal() != null;
                System.out.println("[BotMode] Baritone diag: phase=" + bwAIPhase
                        + " goal=" + (bwBaritoneCurGoal != null)
                        + " pathing=" + pathing + " hasPath=" + hasPath + " hasGoalObj=" + hasGoal
                        + " failed=" + bwBaritoneFailed
                        + " controlling=" + isBaritoneControlling());
            } catch (Throwable t) {
                System.out.println("[BotMode] Baritone diag error: " + t.getMessage());
            }
        }

        // Вне моста отпускаем клавиши, которые мог зажать бот.
        if (bwAIPhase != BWAI_BRIDGE) {
            mc.gameSettings.keyBindSneak.setPressed(false);
            mc.gameSettings.keyBindUseItem.setPressed(false);
            mc.player.setSneaking(false);
        }

        // Принудительная остановка — бот не двигается пока AI не решит куда идти
        // НО если идём к генератору — НЕ обнуляем движение
        if (bwAIPhase == BWAI_IDLE && (currentGeneratorPos == null || mc.player.getDistance(
            new net.minecraft.util.math.BlockPos(currentGeneratorPos[0], currentGeneratorPos[1], currentGeneratorPos[2])) < bwGenDistance)) {
            mc.player.moveForward = 0;
            mc.player.moveStrafing = 0;
            mc.player.setSprinting(false);
        }

        switch (bwAIPhase) {
            case BWAI_IDLE -> aiHandleIdle(mc);
            case BWAI_BUY_SETUP -> aiHandleBuySetup(mc);
            case BWAI_BUY_BLOCKS -> aiHandleBuyBlocks(mc);
            case BWAI_BRIDGE -> aiHandleBridge(mc);
            case BWAI_COLLECT -> aiHandleCollect(mc);
            case BWAI_BUY_GEAR -> aiHandleBuyGear(mc);
            case BWAI_FIGHT -> aiHandleFight(mc);
            case BWAI_DEFEND_BED -> aiHandleDefendBed(mc);
            case BWAI_RETURN_BASE -> aiHandleReturnBase(mc);
            case BWAI_BUY_GEAR_MID -> aiHandleBuyGearMid(mc);
            case BWAI_INVENTORY -> aiHandleInventory(mc);
        }
    }

    private static void aiHandleIdle(Minecraft mc) {
        if (bwAIBasePos == null && bwGameStarted && mc.player.isOnGround()) {
            bwAIBasePos = mc.player.getPosition();
            bwAISpawnYaw = mc.player.rotationYaw;
        }

        // Загружаем позицию генератора для текущей карты (один раз)
        if (currentGeneratorPos == null) {
            loadGeneratorForCurrentMap();
        }

        // Отключаем модули которые мешают
        disableCombatModules();

        bwAISetupStep = 0;
        bwAISpawnWaitTicks = 0;

        // Проверяем достаточно ли ресурсов для покупки
        if (bwAIStrategy.equals("AggressiveMax")) {
            int[] effectiveTarget = getEffectiveResourceTarget();
            int needIron = effectiveTarget[0];
            int needGold = effectiveTarget[1];

            if (bwAIIronCount < needIron || bwAIGoldCount < needGold) {
                // Недостаточно ресурсов — идём к генератору если знаем где он
                if (currentGeneratorPos != null) {
                    net.minecraft.util.math.BlockPos genPos = new net.minecraft.util.math.BlockPos(
                        currentGeneratorPos[0], currentGeneratorPos[1], currentGeneratorPos[2]);
                    double distToGen = horizontalDistanceTo(mc, genPos);
                    if (distToGen > bwGenDistance) {
                        // Идём к генератору
                        moveTowardsForAI(mc, genPos);
                        if (bwAITickCount % 20 == 0) {
                            System.out.println("[BotMode] BedWarsAI: walking to generator iron=" + bwAIIronCount + "/" + needIron + " gold=" + bwAIGoldCount + "/" + needGold);
                        }
                        return;
                    }
                }
                // Ждём у генератора пока не наберём ресурсы
                if (bwAITickCount % 40 == 0) {
                    System.out.println("[BotMode] BedWarsAI: waiting for resources iron=" + bwAIIronCount + "/" + needIron + " gold=" + bwAIGoldCount + "/" + needGold);
                }
                return;
            }

            bwAIPhase = BWAI_BUY_SETUP;
        } else if (bwAIStrategy.equals("Aggressive")) {
            bwAIPhase = BWAI_BUY_GEAR;
        } else if (bwAIAutoDefendBed) {
            bwAIPhase = BWAI_DEFEND_BED;
        } else {
            bwAIPhase = BWAI_BUY_BLOCKS;
        }
        bwAIPhaseTicks = 0;
        System.out.println("[BotMode] BedWarsAI: idle → phase " + bwAIPhase + " iron=" + bwAIIronCount + " gold=" + bwAIGoldCount);
    }

    private static void disableCombatModules() {
        try {
            Harmony.getInstance().getModuleManager().getModules().stream()
                .filter(m -> m.getName().equals("HitAura") || m.getName().equals("TriggerBot"))
                .forEach(m -> {
                    if (m.isState()) {
                        m.setState(false, true);
                        System.out.println("[BotMode] BedWarsAI: disabled " + m.getName());
                    }
                });
        } catch (Exception e) {}
    }

    // Управление модулями во время BedWars AI
    private static void syncBedWarsModules() {
        try {
            // ВСЕ модули которые могут мешать — ВЫКЛЮЧАЕМ
            String[] disableList = {
                "HitAura", "TriggerBot", "KillAura", "AimAssist", "AutoClicker",
                "TargetStrafe", "Reach",
                "NoFall", "Fly", "Speed", "Jesus", "Spider", "NoClip",
                "AutoArmor", "ChestStealer", "InventoryCleaner",
                "AntiKnockback", "FastBow", "AutoTotem",
                "AutoSprint", "Sprint"
            };
            for (String modName : disableList) {
                // HitAura не трогаем в фазе боя — иначе toggle-флаппинг каждый тик (выкл→вкл)
                if (modName.equals("HitAura") && bwAIPhase == BWAI_FIGHT) continue;
                setModuleState(modName, false);
            }

            // Velocity — всегда включен (ПОСЛЕ disableList, чтобы не выключился)
            setModuleState("Velocity", true);
            // Sprint НЕ включаем — чтобы античит не флагал

            // Горизонтальный мост и Tower используют один пользовательский Scaffold.
            // В Tower-режиме BotMode только зажимает Space — блок под игроком ставит сам Scaffold.
            // v27: Scaffold включён ВСЮ фазу моста после достижения края.
            // Раньше проверка isVoidAhead использовала yaw прошлого тика: BotMode успевал
            // повернуть и дать forward, а Scaffold ещё оставался выключенным — причина падения.
            boolean scaffoldOn = bwAIEnabled && bwAIPhase == BWAI_BRIDGE
                    && bwAIBridgeStartReached && bwScaffoldEnabled;
            setModuleState("Scaffold", scaffoldOn);

            // HitAura — только в фазе боя
            if (bwAIPhase == BWAI_FIGHT) {
                setModuleState("HitAura", true);
            }

            // Обычный модуль выключен: BotMode использует локальный порт InvManager
            // и может точно дождаться окончания сортировки перед выходом с базы.
            setModuleState("InvManager", false);
        } catch (Exception e) {}
    }

    private static void setModuleState(String name, boolean state) {
        Harmony.getInstance().getModuleManager().getModules().stream()
            .filter(m -> m.getName().equals(name))
            .findFirst()
            .ifPresent(m -> {
                if (m.isState() != state) {
                    m.setState(state, true);
                }
            });
    }

    // Есть ли бездна впереди в пределах N блоков по направлению взгляда
    // (проверяем, что под передними клетками пусто вниз на 2 блока).
    private static boolean isVoidAhead(Minecraft mc, int blocksAhead) {
        net.minecraft.util.Direction facing = net.minecraft.util.Direction.fromAngle(mc.player.rotationYaw);
        net.minecraft.util.math.BlockPos feet = mc.player.getPosition();
        for (int i = 1; i <= blocksAhead; i++) {
            net.minecraft.util.math.BlockPos ahead = feet.offset(facing, i);
            boolean gap = mc.world.getBlockState(ahead.down()).getMaterial().isReplaceable()
                    && mc.world.getBlockState(ahead.down().down()).getMaterial().isReplaceable();
            if (gap) return true;
        }
        return false;
    }

    private static boolean isBridgingZone(Minecraft mc) {
        if (bwAIMidPos == null) return false;
        if (!currentMapName.isEmpty() && mapBridgeStarts.containsKey(currentMapName)) {
            double[] bs = mapBridgeStarts.get(currentMapName);
            net.minecraft.util.math.BlockPos bridgeStart = new net.minecraft.util.math.BlockPos(bs[0], bs[1], bs[2]);
            double distToStart = mc.player.getDistance(bridgeStart);
            double distToMid = mc.player.getDistance(bwAIMidPos);
            double startToMid = bridgeStart.distanceSq(bwAIMidPos);
            return distToStart <= 3.0 || mc.player.getPosition().distanceSq(bwAIMidPos) <= startToMid;
        }
        return true;
    }

    private static void aiHandleCollectSpawn(Minecraft mc) {
        // Если есть сохранённая позиция генератора — идём туда
        if (currentGeneratorPos != null) {
            net.minecraft.util.math.BlockPos genTarget = new net.minecraft.util.math.BlockPos(
                currentGeneratorPos[0], currentGeneratorPos[1], currentGeneratorPos[2]);
            double distToGen = horizontalDistanceTo(mc, genTarget);

            // Если ещё не дошли — идём к генератору
            if (distToGen > bwGenDistance) {
                moveTowardsForAI(mc, genTarget);
                bwAISpawnWaitTicks = 0;
                return;
            }

            // Дошли до генератора — ищем предметы рядом
            java.util.List<net.minecraft.entity.Entity> items = findNearbyItemsForAI(mc, 4.0);
            if (!items.isEmpty()) {
                net.minecraft.entity.Entity nearest = items.get(0);
                double dist = mc.player.getDistance(nearest);
                if (dist > 1.5) {
                    moveTowardsForAI(mc, nearest.getPosition());
                }
                return;
            }
        } else {
            // Нет сохранённой позиции — ищем предметы рядом
            java.util.List<net.minecraft.entity.Entity> items = findNearbyItemsForAI(mc, 4.0);
            if (!items.isEmpty()) {
                net.minecraft.entity.Entity nearest = items.get(0);
                double dist = mc.player.getDistance(nearest);
                if (dist > 1.5) {
                    moveTowardsForAI(mc, nearest.getPosition());
                }
                return;
            }
        }

        // Нет предметов — просто стоим и ждём
        bwAISpawnWaitTicks++;

        // AggressiveMax: время больше не может закончить ожидание раньше цели.
        // В��ходим только после фактического набора base + Доп-Железо/Доп-Золото.
        if (bwAIStrategy.equals("AggressiveMax")) {
            int[] effectiveTarget = getEffectiveResourceTarget();
            if (bwAIIronCount < effectiveTarget[0] || bwAIGoldCount < effectiveTarget[1]) {
                if (bwAITickCount % 40 == 0) {
                    System.out.println("[BotMode] Extra resources: iron=" + bwAIIronCount + "/" + effectiveTarget[0]
                            + " gold=" + bwAIGoldCount + "/" + effectiveTarget[1]);
                }
                return;
            }
            bwAIPhase = BWAI_BUY_SETUP;
            bwAIPhaseTicks = 0;
            System.out.println("[BotMode] BedWarsAI: resource target reached (" + bwAIIronCount + "/" + effectiveTarget[0]
                    + " iron, " + bwAIGoldCount + "/" + effectiveTarget[1] + " gold)");
            return;
        }

        // Остальные стратегии: ждём 5 сек
        if (bwAISpawnWaitTicks > 100) {
            if (bwAIStrategy.equals("Aggressive")) {
                bwAIPhase = BWAI_BUY_GEAR;
            } else if (bwAIAutoDefendBed) {
                bwAIPhase = BWAI_DEFEND_BED;
            } else {
                bwAIPhase = BWAI_BUY_BLOCKS;
            }
            bwAIPhaseTicks = 0;
            System.out.println("[BotMode] BedWarsAI: collected (" + bwAIIronCount + " iron, " + bwAIGoldCount + " gold) → " + bwAIPhase);
        }
    }


    // AggressiveMax: покупка по таблице
    private static int bwAISetupStep = 0;

    // Состояния для навигации по магазину
    private static final int SHOP_STATE_IDLE = 0;
    private static final int SHOP_STATE_OPENING = 1;
    private static final int SHOP_STATE_IN_CATEGORY = 2;
    private static final int SHOP_STATE_GOING_BACK = 3;

    private static int shopState = SHOP_STATE_IDLE;
    private static int shopCategorySlot = -1;
    private static String shopItemRu = "";
    private static String shopItemEn = "";
    private static int shopItemCost = 0;

    // === v9: закупка — дословный порт модуля AutoBuyMLegacy ===

    private static boolean isBotShopScreen(Minecraft mc) {
        if (mc.currentScreen == null || mc.player == null || mc.player.openContainer == null) return false;
        String title = mc.currentScreen.getTitle().getString()
                .replaceAll("§[0-9a-fk-or]", "")
                .toLowerCase(java.util.Locale.ROOT);
        return title.contains("магазин") || title.contains("shop");
    }

    private static void resetBotBuySession() {
        botBuySessionActive = false;
        botBuyWindowId = -1;
        botBuyIndex = 0;
        botBuyStep = 0;
        botBuyAttempts = 0;
        botBuyWaitingBlocks = false;
        botBuyIronBefore = 0;
        botBuyDetectedCost = 0;
        botBuyClickTime = 0L;
        botBuyPlan.clear();
    }

    private static void beginBotBuySession(int windowId) {
        botBuyWindowId = windowId;
        botBuySessionActive = true;
        botBuyIndex = 0;
        botBuyStep = 0;
        botBuyAttempts = 0;
        botBuyWaitingBlocks = false;
        botBuyIronBefore = 0;
        botBuyDetectedCost = 0;
        botBuyClickTime = 0L;
        buildBotBuyPlan();
        botBuyLastAction = System.currentTimeMillis();
        System.out.println("[BotMode] BotBuy: сессия начата, пунктов=" + botBuyPlan.size());
    }

    // {categorySlot, ruKey, enKey, ruKey2, enKey2, currency(Item), cost(int), skipWhenOwned(bool), repeatBlocks(bool)}
    private static void buildBotBuyPlan() {
        botBuyPlan.clear();
        boolean is35 = bwAIMaxTarget.equals("35i 7g");
        if (is35) {
            if (bwAIBuySword) {
                botBuyPlan.add(new Object[]{SHOP_CAT_WEAPONS, "меч", "sword", "железн", "iron", Items.GOLD_INGOT, 7, true, false});
            }
            if (bwAIBuyArmor) {
                botBuyPlan.add(new Object[]{SHOP_CAT_ARMOR, "шлем", "helmet", "кольчужн", "chain", Items.IRON_INGOT, 5, true, false});
                botBuyPlan.add(new Object[]{SHOP_CAT_ARMOR, "понож", "legging", "кольчужн", "chain", Items.IRON_INGOT, 5, true, false});
                botBuyPlan.add(new Object[]{SHOP_CAT_ARMOR, "нагрудник", "chest", "кольчужн", "chain", Items.IRON_INGOT, 5, true, false});
                botBuyPlan.add(new Object[]{SHOP_CAT_ARMOR, "ботин", "boot", "кольчужн", "chain", Items.IRON_INGOT, 5, true, false});
            }
        } else {
            if (bwAIBuyArmor) {
                botBuyPlan.add(new Object[]{SHOP_CAT_ARMOR, "нагрудник", "chest", "железн", "iron", Items.GOLD_INGOT, 3, true, false});
                botBuyPlan.add(new Object[]{SHOP_CAT_ARMOR, "понож", "legging", "железн", "iron", Items.GOLD_INGOT, 3, true, false});
                botBuyPlan.add(new Object[]{SHOP_CAT_ARMOR, "шлем", "helmet", "кольчужн", "chain", Items.IRON_INGOT, 5, true, false});
                botBuyPlan.add(new Object[]{SHOP_CAT_ARMOR, "ботин", "boot", "кольчужн", "chain", Items.IRON_INGOT, 5, true, false});
            }
            if (bwAIBuySword) {
                botBuyPlan.add(new Object[]{SHOP_CAT_WEAPONS, "меч", "sword", "камен", "stone", Items.IRON_INGOT, 10, true, false});
            }
        }
        if (bwAIBuyPickaxe) {
            botBuyPlan.add(new Object[]{SHOP_CAT_TOOLS, "кирк", "pickaxe", null, null, null, 0, true, false});
        }
        if (bwAIBuyBlocks) {
            if (bwAIOnlyOneBlock) {
                botBuyPlan.add(new Object[]{SHOP_CAT_BLOCKS, "шерст", "wool", null, null, null, 0, false, false});
            } else {
                botBuyPlan.add(new Object[]{SHOP_CAT_BLOCKS, "шерст", "wool", null, null, null, 0, false, true});
            }
        }
    }

    private static boolean botBuyHasCurrency(Object[] p) {
        net.minecraft.item.Item currency = (net.minecraft.item.Item) p[5];
        int cost = (Integer) p[6];
        return currency == null || cost <= 0 || countItemInInventory(currency) >= cost;
    }

    private static void skipBotBuyUnavailable() {
        while (botBuyIndex < botBuyPlan.size()) {
            Object[] p = botBuyPlan.get(botBuyIndex);
            boolean skipWhenOwned = (Boolean) p[7];
            if (skipWhenOwned && hasItemByName((String) p[1], (String) p[2])) {
                botBuyIndex++; botBuyStep = 0; botBuyAttempts = 0; continue;
            }
            if (!botBuyHasCurrency(p)) {
                botBuyIndex++; botBuyStep = 0; botBuyAttempts = 0; continue;
            }
            break;
        }
    }

    private static boolean handleAutoBuyMLegacy(Minecraft mc, int nextPhase) {
        if (!isBotShopScreen(mc)) {
            resetBotBuySession();
            bwAIShopOpen = false;
            if (mc.currentScreen != null) return true; // чужой GUI не трогаем
            net.minecraft.entity.merchant.villager.VillagerEntity shop = findNearestShopForAI(mc);
            if (shop == null) return true;
            if (mc.player.getDistance(shop) > 3.5) {
                moveTowardsForAI(mc, shop.getPosition());
            } else {
                mc.player.moveForward = 0f;
                mc.player.moveStrafing = 0f;
                mc.playerController.interactWithEntity(mc.player, shop, net.minecraft.util.Hand.MAIN_HAND);
            }
            return true;
        }

        bwAIShopOpen = true;
        mc.player.moveForward = 0f;
        mc.player.moveStrafing = 0f;

        net.minecraft.inventory.container.Container container = mc.player.openContainer;
        if (container == null || container.windowId == 0) return true;

        if (!botBuySessionActive || botBuyWindowId != container.windowId) {
            beginBotBuySession(container.windowId);
            return true;
        }

        long delay = Math.max(50L, bwAIBuyDelay * 50L);
        long now = System.currentTimeMillis();
        if (now - botBuyLastAction < delay) return true;
        botBuyLastAction = now;

        skipBotBuyUnavailable();
        if (botBuyIndex >= botBuyPlan.size()) {
            mc.player.closeScreen();
            resetBotBuySession();
            bwAIShopOpen = false;
            shopState = SHOP_STATE_IDLE;
            bwAIWoolCount = countAllWool();
            startBotInventory(nextPhase);
            System.out.println("[BotMode] BotBuy: закупка завершена, blocks=" + bwAIWoolCount + ", запускаю InvManager");
            return true;
        }

        Object[] p = botBuyPlan.get(botBuyIndex);
        int categorySlot = (Integer) p[0];
        boolean repeatBlocks = (Boolean) p[8];

        if (botBuyStep == 0) {
            mc.playerController.windowClick(container.windowId, categorySlot, 0, ClickType.PICKUP, mc.player);
            botBuyStep = 1;
            botBuyAttempts = 0;
            return true;
        }

        if (repeatBlocks && botBuyWaitingBlocks) {
            int currentIron = countItemInInventory(Items.IRON_INGOT);
            if (currentIron < botBuyIronBefore) {
                int spent = botBuyIronBefore - currentIron;
                if (botBuyDetectedCost <= 0) botBuyDetectedCost = spent;
                botBuyWaitingBlocks = false;
                botBuyClickTime = 0L;
                if (botBuyDetectedCost > 0 && currentIron < botBuyDetectedCost) {
                    botBuyIndex++;
                    botBuyStep = 0;
                }
                return true;
            }
            if (System.currentTimeMillis() - botBuyClickTime >= 1500L) {
                botBuyWaitingBlocks = false;
                botBuyClickTime = 0L;
                botBuyIndex++;
                botBuyStep = 0;
            }
            return true;
        }

        boolean bought = buyItemInCategory(mc, container, (String) p[1], (String) p[2], (String) p[3], (String) p[4]);
        botBuyAttempts++;

        if (bought && repeatBlocks) {
            botBuyWaitingBlocks = true;
            botBuyIronBefore = countItemInInventory(Items.IRON_INGOT);
            botBuyClickTime = System.currentTimeMillis();
            botBuyAttempts = 0;
            return true;
        }

        if (bought || botBuyAttempts > 15) {
            if (!bought) System.out.println("[BotMode] BotBuy: пункт " + botBuyIndex + " не найден, пропускаю");
            botBuyIndex++;
            botBuyStep = 0;
            botBuyAttempts = 0;
        }
        return true;
    }

    private static void aiHandleBuySetup(Minecraft mc) {
        handleAutoBuyMLegacy(mc, BWAI_BRIDGE);
    }

    private static void aiHandleBuyBlocks(Minecraft mc) {
        handleAutoBuyMLegacy(mc, BWAI_BRIDGE);
    }

    private static void startBotInventory(int nextPhase) {
        bwInvNextPhase = nextPhase;
        bwInvQuietTicks = 0;
        bwInvLastAction = 0L;
        bwAIPhaseTicks = 0;
        bwAIPhase = bwInvEnabled ? BWAI_INVENTORY : nextPhase;
        mc.player.moveForward = 0f;
        mc.player.moveStrafing = 0f;
    }

    private static void aiHandleInventory(Minecraft mc) {
        mc.player.moveForward = 0f;
        mc.player.moveStrafing = 0f;
        mc.player.setSprinting(false);
        mc.player.setSneaking(false);
        if (!bwInvEnabled) {
            finishBotInventory();
            return;
        }
        if (mc.currentScreen != null) {
            mc.player.closeScreen();
            return;
        }
        long now = System.currentTimeMillis();
        int min = Math.max(0, Math.min(bwInvMinDelay, bwInvMaxDelay));
        int max = Math.max(min, Math.max(bwInvMinDelay, bwInvMaxDelay));
        long delay = min + (max > min ? java.util.concurrent.ThreadLocalRandom.current().nextInt(max - min + 1) : 0);
        if (now - bwInvLastAction < delay) return;

        if (runBotInventoryAction(mc)) {
            bwInvLastAction = now;
            bwInvQuietTicks = 0;
            return;
        }
        // Несколько чистых тиков защищают от перехода до прихода серверного обновления инвентаря.
        if (++bwInvQuietTicks >= 10) finishBotInventory();
    }

    private static void finishBotInventory() {
        bwInvQuietTicks = 0;
        bwInvLastAction = 0L;
        bwAIPhase = bwInvNextPhase;
        bwAIPhaseTicks = 0;
        bwAISelectedBridgeStart = null;
        bwAIBridgeStartReached = false;
        bwAIBridgeAxisTarget = null;
        System.out.println("[BotMode] InvManager: экипировка и сортировка завершены, phase -> " + bwInvNextPhase);
    }

    private static boolean runBotInventoryAction(Minecraft mc) {
        if (("Мусор".equals(bwInvMode) || "Оба".equals(bwInvMode)) && bwInvDropGarbage && botDropGarbage(mc)) return true;
        if (("Сортировка".equals(bwInvMode) || "Оба".equals(bwInvMode)) && botSortHotbar(mc)) return true;
        if (bwInvAutoArmor && botEquipArmor(mc)) return true;
        return ("Сортировка".equals(bwInvMode) || "Оба".equals(bwInvMode)) && botReorderBlocks(mc);
    }

    private static boolean botDropGarbage(Minecraft mc) {
        int start = bwInvGarbageHotbar ? 0 : 9;
        for (int i = start; i < 36; i++) {
            ItemStack st = mc.player.inventory.getStackInSlot(i);
            if (st.isEmpty() || !isBotGarbage(st)) continue;
            int containerSlot = i < 9 ? i + 36 : i;
            mc.playerController.windowClick(0, containerSlot, 0, ClickType.THROW, mc.player);
            return true;
        }
        return false;
    }

    private static boolean isBotGarbage(ItemStack st) {
        net.minecraft.item.Item it = st.getItem();
        if (it instanceof net.minecraft.item.SwordItem || it instanceof net.minecraft.item.ArmorItem
                || it instanceof net.minecraft.item.PickaxeItem || it instanceof net.minecraft.item.AxeItem
                || it instanceof net.minecraft.item.ShovelItem || it instanceof net.minecraft.item.BlockItem
                || it.isFood()) return false;
        if (it == Items.IRON_INGOT || it == Items.GOLD_INGOT || it == Items.DIAMOND || it == Items.EMERALD
                || it == Items.ENDER_PEARL || it == Items.TOTEM_OF_UNDYING) return false;
        return it == Items.STICK || it == Items.PAPER || it == Items.FEATHER || it == Items.BONE
                || it == Items.SNOWBALL || it == Items.COMPASS || it == Items.MAP || it == Items.FILLED_MAP;
    }

    private static boolean botSortHotbar(Minecraft mc) {
        for (int target = 0; target < 9; target++) {
            String desired = bwInvSlots[target];
            if ("Ничего".equals(desired)) continue;
            ItemStack current = mc.player.inventory.getStackInSlot(target);
            if (botMatchesType(current, desired)) continue;
            int found = botFindBestItem(mc, desired, target);
            if (found >= 0 && found != target) {
                int containerSlot = found < 9 ? found + 36 : found;
                mc.playerController.windowClick(0, containerSlot, target, ClickType.SWAP, mc.player);
                return true;
            }
        }
        return false;
    }

    private static int botFindBestItem(Minecraft mc, String type, int currentSlot) {
        int best = -1, score = Integer.MIN_VALUE;
        for (int i = 0; i < 36; i++) {
            ItemStack st = mc.player.inventory.getStackInSlot(i);
            if (!botMatchesType(st, type)) continue;
            if (i < 9 && i != currentSlot && type.equals(bwInvSlots[i])) continue;
            int value = botItemScore(st);
            if (value > score) { score = value; best = i; }
        }
        return best;
    }

    private static boolean botMatchesType(ItemStack st, String type) {
        if (st == null || st.isEmpty()) return false;
        net.minecraft.item.Item it = st.getItem();
        switch (type) {
            case "Меч": return it instanceof net.minecraft.item.SwordItem;
            case "Кирка": return it instanceof net.minecraft.item.PickaxeItem;
            case "Топор": return it instanceof net.minecraft.item.AxeItem;
            case "Лук": return it instanceof net.minecraft.item.BowItem || it instanceof net.minecraft.item.CrossbowItem;
            case "Блоки": return it instanceof net.minecraft.item.BlockItem;
            case "Еда": return it.isFood();
            case "Лучший предмет": return true;
            default: return false;
        }
    }

    private static int botItemScore(ItemStack st) {
        net.minecraft.item.Item it = st.getItem();
        int score = 10;
        if (it == Items.NETHERITE_SWORD || it == Items.NETHERITE_AXE || it == Items.NETHERITE_PICKAXE) score = 100;
        else if (it == Items.DIAMOND_SWORD || it == Items.DIAMOND_AXE || it == Items.DIAMOND_PICKAXE) score = 80;
        else if (it == Items.IRON_SWORD || it == Items.IRON_AXE || it == Items.IRON_PICKAXE) score = 60;
        else if (it == Items.STONE_SWORD || it == Items.STONE_AXE || it == Items.STONE_PICKAXE) score = 40;
        else if (it == Items.WOODEN_SWORD || it == Items.WOODEN_AXE || it == Items.WOODEN_PICKAXE) score = 20;
        if (st.isEnchanted()) score += 30;
        if (st.isDamaged()) score -= st.getDamage();
        if (it instanceof net.minecraft.item.BlockItem) score += st.getCount();
        return score;
    }

    private static boolean botEquipArmor(Minecraft mc) {
        int start = bwInvArmorHotbar ? 0 : 9;
        for (int armorSlot = 0; armorSlot < 4; armorSlot++) {
            int best = -1;
            int protection = botArmorScore(mc.player.inventory.getStackInSlot(36 + armorSlot));
            for (int i = start; i < 36; i++) {
                ItemStack st = mc.player.inventory.getStackInSlot(i);
                if (!(st.getItem() instanceof net.minecraft.item.ArmorItem)) continue;
                net.minecraft.item.ArmorItem armor = (net.minecraft.item.ArmorItem) st.getItem();
                if (armor.getEquipmentSlot().getIndex() == armorSlot && botArmorScore(st) > protection) {
                    protection = botArmorScore(st);
                    best = i;
                }
            }
            if (best >= 0) {
                int containerSlot = best < 9 ? best + 36 : best;
                mc.playerController.windowClick(0, containerSlot, 0, ClickType.QUICK_MOVE, mc.player);
                return true;
            }
        }
        return false;
    }

    private static int botArmorScore(ItemStack st) {
        if (st == null || st.isEmpty() || !(st.getItem() instanceof net.minecraft.item.ArmorItem)) return -1;
        net.minecraft.item.Item it = st.getItem();
        int score = 0;
        if (it == Items.NETHERITE_CHESTPLATE || it == Items.NETHERITE_LEGGINGS || it == Items.NETHERITE_HELMET || it == Items.NETHERITE_BOOTS) score = 4;
        else if (it == Items.DIAMOND_CHESTPLATE || it == Items.DIAMOND_LEGGINGS || it == Items.DIAMOND_HELMET || it == Items.DIAMOND_BOOTS) score = 3;
        else if (it == Items.IRON_CHESTPLATE || it == Items.IRON_LEGGINGS || it == Items.IRON_HELMET || it == Items.IRON_BOOTS) score = 2;
        else if (it == Items.CHAINMAIL_CHESTPLATE || it == Items.CHAINMAIL_LEGGINGS || it == Items.CHAINMAIL_HELMET || it == Items.CHAINMAIL_BOOTS) score = 1;
        if (st.isEnchanted()) score += 2;
        return score;
    }

    private static boolean botReorderBlocks(Minecraft mc) {
        boolean ascending = "Увеличение".equals(bwInvBlockOrder);
        for (int i = 0; i < 8; i++) {
            ItemStack a = mc.player.inventory.getStackInSlot(i), b = mc.player.inventory.getStackInSlot(i + 1);
            if (a.isEmpty() || b.isEmpty() || !(a.getItem() instanceof net.minecraft.item.BlockItem) || !(b.getItem() instanceof net.minecraft.item.BlockItem)) continue;
            boolean wrong = ascending ? a.getCount() > b.getCount() : a.getCount() < b.getCount();
            if (wrong) {
                mc.playerController.windowClick(0, i + 36, i + 1, ClickType.SWAP, mc.player);
                return true;
            }
        }
        return false;
    }

    private static void aiHandleBridge(Minecraft mc) {
        if (bwAIMidPos == null) bwAIMidPos = findMidPosForAI(mc);

        // v26: как только бот реально пересёк край центра со стороны своей базы,
        // переключаемся на многошаговый маршрут к изумруду. До пересечения Scaffold
        // продолжает вести его по мосту; после пересечения блоки больше не ставятся.
        if (hasCrossedCenterEntryEdge(mc)) {
            bwCenterConfirmed = true;
            bwCenterResourceStage = 0;
            bwEmeraldCountOnArrival = bwAIEmeraldCount;
            bwEmeraldWaitTicks = 0;
            initializeCenterRoute(mc);
            bwAIPhase = BWAI_COLLECT;
            bwAIPhaseTicks = 0;
            cancelBaritone();
            System.out.println("[BotMode] Center gate: пересечён " + bwCenterEntrySide
                    + " край, точек маршрута=" + bwCenterRoute.size());
            return;
        }

        if (bwAIWoolCount <= 0) {
            bwAIPhase = BWAI_BUY_BLOCKS;
            bwAIPhaseTicks = 0;
            return;
        }

        // Лимит BridgeBlocks больше не может преждевременно перевести бота к ресурсам:
        // переход выполняется только через Center gate выше.

        net.minecraft.util.math.BlockPos bridgeStart = getBridgeStartForCurrentMap();
        if (bwAITickCount % 40 == 0) {
            System.out.println("[BotMode] Bridge: map='" + currentMapName + "' start="
                    + (bridgeStart != null ? bridgeStart.getX() + "," + bridgeStart.getY() + "," + bridgeStart.getZ() : "null")
                    + " reached=" + bwAIBridgeStartReached);
        }

        // Baritone рулит только до края базы.
        if (isBaritoneControlling() && !bwAIBridgeStartReached) {
            return;
        }

        // v9: ЖЁСТКОЕ ПРАВИЛО — до края базы ни одного блока
        if (!hasReachedBridgeStart(mc, bridgeStart)) {
            mc.player.setSneaking(false);
            if (bridgeStart != null) {
                moveTowardsForAI(mc, bridgeStart);
            } else {
                moveTowardsForAI(mc, bwAIMidPos); // идём к краю без блоков
            }
            return;
        }

        cancelBaritone();
        net.minecraft.util.math.BlockPos axisTarget = getBridgeAxisTarget(mc);
        safeBridgeStep(mc, axisTarget != null ? axisTarget : bwAIMidPos);
    }

    private static void aiHandleCollect(Minecraft mc) {
        if (bwAIMidPos == null) bwAIMidPos = findMidPosForAI(mc);

        // Любой внешний переход в COLLECT всё равно обязан пройти через нужный край центра.
        if (!bwCenterConfirmed) {
            net.minecraft.util.math.BlockPos edge = getCenterEntryEdge(mc);
            if (!hasCrossedCenterEntryEdge(mc)) {
                if (edge != null) moveTowardsForAI(mc, edge);
                else moveTowardsForAI(mc, bwAIMidPos);
                return;
            }
            bwCenterConfirmed = true;
            bwCenterResourceStage = 0;
            bwEmeraldCountOnArrival = bwAIEmeraldCount;
            bwEmeraldWaitTicks = 0;
            initializeCenterRoute(mc);
            cancelBaritone();
            System.out.println("[BotMode] Center gate: край подтверждён в COLLECT, side=" + bwCenterEntrySide);
        }

        net.minecraft.util.math.BlockPos emeraldTarget = getNearestSavedResource(mc, "emerald");
        if (emeraldTarget == null) {
            mc.player.moveForward = 0f;
            mc.player.moveStrafing = 0f;
            if (bwAITickCount % 100 == 0) System.out.println("[BotMode] Center resources: координата emerald не найдена");
            return;
        }

        if (bwCenterResourceStage == 0) {
            net.minecraft.util.math.BlockPos routeTarget = getCurrentCenterResourceTarget(mc);
            boolean onWaypoint = bwCenterRouteIndex < bwCenterRoute.size();
            double reach = onWaypoint ? 2.6 : 1.85;
            if (routeTarget != null && mc.player.getDistance(routeTarget) > reach) {
                // Если отдельная точка недостижима 7 секунд, пропускаем только её,
                // а не весь маршрут. Это защищает от временно поставленных блоков.
                if (onWaypoint && bwAITickCount - bwCenterWaypointStartTick > 140) {
                    System.out.println("[BotMode] Center route: timeout waypoint " + (bwCenterRouteIndex + 1));
                    bwCenterRouteIndex++;
                    bwCenterWaypointStartTick = bwAITickCount;
                    cancelBaritone();
                } else {
                    moveTowardsForAI(mc, routeTarget);
                }
                return;
            }
            if (onWaypoint) {
                bwCenterRouteIndex++;
                bwCenterWaypointStartTick = bwAITickCount;
                cancelBaritone();
                if (bwCenterRouteIndex < bwCenterRoute.size()) {
                    System.out.println("[BotMode] Center route: next " + (bwCenterRouteIndex + 1) + "/" + bwCenterRoute.size());
                    return;
                }
                System.out.println("[BotMode] Center route: края пройдены, идём к emerald");
                return;
            }

            cancelBaritone();
            bwCenterResourceStage = 1;
            bwEmeraldCountOnArrival = bwAIEmeraldCount;
            bwEmeraldWaitTicks = 0;
            mc.player.moveForward = 0f;
            mc.player.moveStrafing = 0f;
            System.out.println("[BotMode] Center resources: изумрудный генератор достигнут");
            return;
        }

        if (bwCenterResourceStage == 1) {
            bwEmeraldWaitTicks++;
            java.util.List<net.minecraft.entity.Entity> items = findNearbyItemsForAI(mc, 4.0f);
            if (!items.isEmpty()) moveTowardsForAI(mc, items.get(0).getPosition());
            else { mc.player.moveForward = 0f; mc.player.moveStrafing = 0f; }
            if (bwAIEmeraldCount > bwEmeraldCountOnArrival || bwEmeraldWaitTicks >= 300) {
                bwCenterResourceStage = 2;
                bwCenterRouteIndex = bwCenterRoute.size() - 1;
                bwCenterWaypointStartTick = bwAITickCount;
                bwAIPhaseTicks = 0;
                cancelBaritone();
                System.out.println("[BotMode] Center resources: изумруд собран/таймаут, обратный маршрут");
            }
            return;
        }

        if (bwCenterResourceStage == 2) {
            net.minecraft.util.math.BlockPos target = getCurrentCenterResourceTarget(mc);
            if (target != null && mc.player.getDistance(target) > 2.6) {
                if (bwAITickCount - bwCenterWaypointStartTick > 140) {
                    System.out.println("[BotMode] Center return: timeout waypoint " + (bwCenterRouteIndex + 1));
                    bwCenterRouteIndex--;
                    bwCenterWaypointStartTick = bwAITickCount;
                    cancelBaritone();
                } else moveTowardsForAI(mc, target);
                return;
            }
            if (bwCenterRouteIndex >= 0) {
                bwCenterRouteIndex--;
                bwCenterWaypointStartTick = bwAITickCount;
                cancelBaritone();
                return;
            }
            bwCenterResourceStage = 3;
            mc.player.moveForward = 0f;
            mc.player.moveStrafing = 0f;
            System.out.println("[BotMode] Center resources: вернулись к краю центра, ожидаем алмазный маршрут");
            return;
        }

        mc.player.moveForward = 0f;
        mc.player.moveStrafing = 0f;
        bwAIPhaseTicks = 0;
    }

    private static net.minecraft.util.math.BlockPos getNearestSavedResource(Minecraft mc, String keySub) {
        net.minecraft.util.math.BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        String mapPrefix = currentMapName == null ? "" : currentMapName.toLowerCase(java.util.Locale.ROOT);
        String needle = keySub.toLowerCase(java.util.Locale.ROOT);
        for (java.util.Map.Entry<String, double[]> e : generatorPositions.entrySet()) {
            String key = e.getKey().toLowerCase(java.util.Locale.ROOT);
            if (!key.contains(needle)) continue;
            if (!mapPrefix.isEmpty() && !key.startsWith(mapPrefix)) continue;
            double[] a = e.getValue();
            net.minecraft.util.math.BlockPos pos = new net.minecraft.util.math.BlockPos(a[0], a[1], a[2]);
            double d = mc.player.getDistance(pos);
            if (d < bestDist) { bestDist = d; best = pos; }
        }
        return best;
    }

    private static net.minecraft.util.math.BlockPos getSavedMapPoint(String suffix) {
        String wanted = ((currentMapName == null ? "" : currentMapName) + "_" + suffix).toLowerCase(java.util.Locale.ROOT);
        for (java.util.Map.Entry<String, double[]> e : generatorPositions.entrySet()) {
            if (!e.getKey().toLowerCase(java.util.Locale.ROOT).equals(wanted)) continue;
            double[] a = e.getValue();
            return new net.minecraft.util.math.BlockPos(a[0], a[1], a[2]);
        }
        return null;
    }

    private static String determineCenterEntrySide(Minecraft mc) {
        net.minecraft.util.math.BlockPos blue = getSavedMapPoint("center_blue_edge");
        net.minecraft.util.math.BlockPos red = getSavedMapPoint("center_red_edge");
        net.minecraft.util.math.BlockPos reference = bwAIBasePos != null ? bwAIBasePos : mc.player.getPosition();
        if (blue == null) return red == null ? "" : "red";
        if (red == null) return "blue";
        return reference.distanceSq(blue) <= reference.distanceSq(red) ? "blue" : "red";
    }

    private static net.minecraft.util.math.BlockPos getCenterEntryEdge(Minecraft mc) {
        if (bwCenterEntrySide.isEmpty()) bwCenterEntrySide = determineCenterEntrySide(mc);
        net.minecraft.util.math.BlockPos edge = getSavedMapPoint("center_" + bwCenterEntrySide + "_edge");
        return edge != null ? edge : bwAIMidPos;
    }

    private static boolean hasCrossedCenterEntryEdge(Minecraft mc) {
        if (bwAIMidPos == null) bwAIMidPos = findMidPosForAI(mc);
        net.minecraft.util.math.BlockPos edge = getCenterEntryEdge(mc);
        if (edge == null || bwAIMidPos == null) return mc.player.getDistance(bwAIMidPos) < 2.25;
        double vx = bwAIMidPos.getX() - edge.getX();
        double vz = bwAIMidPos.getZ() - edge.getZ();
        double len = Math.sqrt(vx * vx + vz * vz);
        if (len < 0.1) return mc.player.getDistance(edge) < 3.0;
        double px = mc.player.getPosX() - (edge.getX() + 0.5);
        double pz = mc.player.getPosZ() - (edge.getZ() + 0.5);
        double inward = (px * vx + pz * vz) / len;
        double lateral = Math.abs(px * vz - pz * vx) / len;
        return inward >= 0.0 && lateral <= 9.0 && mc.player.getPosY() >= edge.getY() - 2.5;
    }

    private static void initializeCenterRoute(Minecraft mc) {
        if (bwCenterEntrySide.isEmpty()) bwCenterEntrySide = determineCenterEntrySide(mc);
        bwCenterRoute.clear();
        String prefix = ((currentMapName == null ? "" : currentMapName) + "_center_route_" + bwCenterEntrySide + "_").toLowerCase(java.util.Locale.ROOT);
        java.util.List<java.util.Map.Entry<String, double[]>> points = new java.util.ArrayList<>();
        for (java.util.Map.Entry<String, double[]> e : generatorPositions.entrySet()) {
            if (e.getKey().toLowerCase(java.util.Locale.ROOT).startsWith(prefix)) points.add(e);
        }
        points.sort(java.util.Comparator.comparing(java.util.Map.Entry::getKey));
        for (java.util.Map.Entry<String, double[]> e : points) {
            double[] a = e.getValue();
            bwCenterRoute.add(new net.minecraft.util.math.BlockPos(a[0], a[1], a[2]));
        }
        bwCenterRouteIndex = 0;
        // Уже пересечённые точки пропускаем: не заставляем бота разворачиваться назад.
        while (bwCenterRouteIndex < bwCenterRoute.size()
                && mc.player.getDistance(bwCenterRoute.get(bwCenterRouteIndex)) <= 3.0) bwCenterRouteIndex++;
        bwCenterWaypointStartTick = bwAITickCount;
    }

    private static net.minecraft.util.math.BlockPos getCurrentCenterResourceTarget(Minecraft mc) {
        if (bwAIMidPos == null) bwAIMidPos = findMidPosForAI(mc);
        if (!bwCenterConfirmed) return getCenterEntryEdge(mc);
        if (bwCenterResourceStage == 0) {
            if (bwCenterRouteIndex < bwCenterRoute.size()) return bwCenterRoute.get(bwCenterRouteIndex);
            return getNearestSavedResource(mc, "emerald");
        }
        if (bwCenterResourceStage == 2 && bwCenterRouteIndex >= 0 && bwCenterRouteIndex < bwCenterRoute.size()) {
            return bwCenterRoute.get(bwCenterRouteIndex);
        }
        return null;
    }

    private static void aiHandleBuyGear(Minecraft mc) {
        handleAutoBuyMLegacy(mc, BWAI_BRIDGE);
    }

    private static void aiHandleBuyGearMid(Minecraft mc) {
        handleAutoBuyMLegacy(mc, BWAI_COLLECT);
    }

    private static void aiHandleFight(Minecraft mc) {
        if (bwAITargetEnemy == null || bwAITargetEnemy.removed || bwAITargetEnemy.getDistance(mc.player) > 20) {
            bwAITargetEnemy = null;
            bwAIPhase = (bwAIIronCount >= 24 || bwAIEmeraldCount >= 4) ? BWAI_BUY_GEAR_MID : BWAI_COLLECT;
            bwAIPhaseTicks = 0;
            return;
        }

        double dist = mc.player.getDistance(bwAITargetEnemy);
        if (dist > bwAIFightRange) {
            moveTowardsForAI(mc, bwAITargetEnemy.getPosition());
        }
        // Атакует HitAura (включается в syncBedWarsModules для BWAI_FIGHT).
        // Ручной attackEntity каждый тик = 20 CPS = мгновенный кик античитом.
    }

    private static void aiHandleDefendBed(Minecraft mc) {
        if (bwAIBedPos == null) {
            findBedForAI(mc);
            if (bwAIBedPos == null) { bwAIPhase = BWAI_IDLE; bwAIPhaseTicks = 0; return; }
        }

        if (bwAIWoolCount <= 0) {
            bwAIPhase = BWAI_BUY_BLOCKS;
            bwAIPhaseTicks = 0;
            return;
        }

        double dist = mc.player.getDistance(bwAIBedPos);
        if (dist > 4.0) {
            moveTowardsForAI(mc, bwAIBedPos);
        } else {
            defendBedForAI(mc);
        }
    }

    private static void aiHandleReturnBase(Minecraft mc) {
        if (bwAIBasePos == null) { bwAIPhase = BWAI_IDLE; bwAIPhaseTicks = 0; return; }
        if (mc.player.getDistance(bwAIBasePos) < 5.0) {
            bwAIPhase = BWAI_BUY_GEAR;
            bwAIPhaseTicks = 0;
        } else {
            moveTowardsForAI(mc, bwAIBasePos);
        }
    }

    // === BedWars AI helpers ===

    private static void findBedForAI(Minecraft mc) {
        if (mc.world == null) return;
        net.minecraft.util.math.BlockPos playerPos = mc.player.getPosition();
        for (int x = -20; x <= 20; x++) {
            for (int y = -5; y <= 5; y++) {
                for (int z = -20; z <= 20; z++) {
                    net.minecraft.util.math.BlockPos pos = playerPos.add(x, y, z);
                    if (mc.world.getBlockState(pos).getBlock() instanceof net.minecraft.block.BedBlock) {
                        if (bwAIBedPos == null || mc.player.getDistance(pos) < mc.player.getDistance(bwAIBedPos)) {
                            bwAIBedPos = pos;
                        }
                    }
                }
            }
        }
    }

    private static net.minecraft.util.math.BlockPos findMidPosForAI(Minecraft mc) {
        if (mc.world == null) return mc.player.getPosition().add(0, 0, 50);
        // Используем точный центр карты из bw_generators.json (если задан)
        String mapKey = currentMapName;
        if (mapKey.isEmpty() && currentGeneratorPos != null) {
            // Подбираем карту по известной позиции генератора
            for (java.util.Map.Entry<String, double[]> e : generatorPositions.entrySet()) {
                double[] g = e.getValue();
                if (Math.abs(g[0] - currentGeneratorPos[0]) < 2
                        && Math.abs(g[1] - currentGeneratorPos[1]) < 2
                        && Math.abs(g[2] - currentGeneratorPos[2]) < 2) {
                    mapKey = e.getKey();
                    break;
                }
            }
        }
        if (!mapKey.isEmpty() && mapCenters.containsKey(mapKey)) {
            double[] center = mapCenters.get(mapKey);
            return new net.minecraft.util.math.BlockPos(center[0], center[1], center[2]);
        }
        // Центр карты — ищем через генератор ресурсов (самый дальний от кровати)
        // Или просто идём от кровати в противоположную сторону
        if (bwAIBedPos != null) {
            // Направление от кровати через текущую позицию игрока — это направление к центру
            double dx = mc.player.getPosX() - bwAIBedPos.getX();
            double dz = mc.player.getPosZ() - bwAIBedPos.getZ();
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len < 1) len = 1;
            // Идём в 3 раза дальше от кровати чем сейчас
            double scale = 3.0;
            if (len * scale < 10) scale = 10.0 / len;
            int targetX = (int) (mc.player.getPosX() + (dx / len) * scale * len);
            int targetZ = (int) (mc.player.getPosZ() + (dz / len) * scale * len);
            return new net.minecraft.util.math.BlockPos(targetX, mc.player.getPosition().getY(), targetZ);
        }
        // Фоллбэк: идём по Z вперёд
        return mc.player.getPosition().add(0, 0, 50);
    }

    private static void detectEnemiesForAI(Minecraft mc) {
        bwAITargetEnemy = null;
        double closest = 20.0;
        for (net.minecraft.entity.player.PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player) continue;
            double d = mc.player.getDistance(p);
            if (d < closest) { closest = d; bwAITargetEnemy = p; }
        }
    }

    private static net.minecraft.entity.merchant.villager.VillagerEntity findNearestShopForAI(Minecraft mc) {
        if (mc.world == null) return null;
        net.minecraft.entity.merchant.villager.VillagerEntity nearest = null;
        double closest = 15.0;
        for (net.minecraft.entity.Entity e : mc.world.getAllEntities()) {
            if (e instanceof net.minecraft.entity.merchant.villager.VillagerEntity v) {
                // Проверяем имя — только "Магазин", пропускаем "Командные улучшения" и других
                String name = v.getDisplayName().getString();
                String cleanName = name.replaceAll("§[0-9a-fk-or]", "").toLowerCase(java.util.Locale.ROOT);
                if (!cleanName.contains("магазин") && !cleanName.contains("shop")) continue;
                double d = mc.player.getDistance(v);
                if (d < closest) { closest = d; nearest = v; }
            }
        }
        return nearest;
    }

    private static java.util.List<net.minecraft.entity.Entity> findNearbyItemsForAI(Minecraft mc, double radius) {
        if (mc.world == null) return java.util.Collections.emptyList();
        net.minecraft.util.math.AxisAlignedBB box = mc.player.getBoundingBox().grow(radius);
        java.util.List<net.minecraft.entity.Entity> items = new java.util.ArrayList<>();
        for (net.minecraft.entity.Entity e : mc.world.getEntitiesInAABBexcluding(mc.player, box,
                e2 -> e2 instanceof net.minecraft.entity.item.ItemEntity)) {
            items.add(e);
        }
        items.sort(java.util.Comparator.comparingDouble(e -> mc.player.getDistance(e)));
        return items;
    }

    // ==================== Магазин MineLegacy (двухуровневый) ====================
    // Категории в строке 1: 0=Блоки, 1=Оружие, 2=Броня, 3=Инструменты, 4=Луки, 5=Зелья, 6=Другое
    // Товары в строке 2+

    private static final int SHOP_CAT_BLOCKS = 0;
    private static final int SHOP_CAT_WEAPONS = 1;
    private static final int SHOP_CAT_ARMOR = 2;
    private static final int SHOP_CAT_TOOLS = 3;

    // Открыть магазин и перейти в категорию
    private static boolean openShopCategory(Minecraft mc, net.minecraft.entity.merchant.villager.VillagerEntity shop, int categorySlot) {
        if (!bwAIShopOpen) {
            double dist = mc.player.getDistance(shop);
            if (dist > 3.5) {
                moveTowardsForAI(mc, shop.getPosition());
                return false;
            }
            mc.playerController.interactWithEntity(mc.player, shop, net.minecraft.util.Hand.MAIN_HAND);
            bwAIShopOpen = true;
            bwAIShopTicks = 0;
            return false;
        }

        bwAIShopTicks++;
        if (bwAIShopTicks < bwAIBuyDelay) return false;

        net.minecraft.inventory.container.Container container = mc.player.openContainer;
        if (container == null || mc.currentScreen == null) { bwAIShopOpen = false; return false; }

        // Проверяем что мы на главном экране (категории в строке 1)
        net.minecraft.item.ItemStack slot0 = container.getSlot(0).getStack();
        if (!slot0.isEmpty() && slot0.hasDisplayName()) {
            String name = slot0.getDisplayName().getString().toLowerCase(java.util.Locale.ROOT);
            if (name.contains("блок") || name.contains("terracotta") || name.contains("wool")) {
                // Мы на главном экране — кликаем категорию
                mc.playerController.windowClick(container.windowId, categorySlot, 0, ClickType.PICKUP, mc.player);
                bwAIShopTicks = 0;
                return true;
            }
        }

        // Если зелёное стекло — значит мы внутри категории, возвращаемся
        net.minecraft.item.ItemStack slot9 = container.getSlot(9).getStack();
        if (!slot9.isEmpty() && slot9.getItem() == net.minecraft.item.Items.LIME_STAINED_GLASS_PANE) {
            mc.playerController.windowClick(container.windowId, 9, 0, ClickType.PICKUP, mc.player);
            bwAIShopTicks = 0;
            return false;
        }

        return true;
    }

    // Купить предмет в категории по имени
    private static boolean buyItemInCategory(Minecraft mc, net.minecraft.inventory.container.Container container, String ruKey, String enKey) {
        return buyItemInCategory(mc, container, ruKey, enKey, null, null);
    }

    private static boolean buyItemInCategory(Minecraft mc, net.minecraft.inventory.container.Container container, String ruKey, String enKey, String ruKey2, String enKey2) {
        int maxSlot = Math.min(container.inventorySlots.size(), 54);
        for (int i = 9; i < maxSlot; i++) {
            net.minecraft.inventory.container.Slot slot = container.getSlot(i);
            if (slot.getStack().isEmpty()) continue;
            String name = slot.getStack().getDisplayName().getString().toLowerCase(java.util.Locale.ROOT);
            boolean match1 = name.contains(ruKey) || name.contains(enKey);
            boolean match2 = (ruKey2 == null) || name.contains(ruKey2) || name.contains(enKey2);
            if (match1 && match2) {
                mc.playerController.windowClick(container.windowId, i, 0, ClickType.PICKUP, mc.player);
                return true;
            }
        }
        return false;
    }

    // Вернуться на главный экран категорий
    private static void goBackToCategories(Minecraft mc) {
        net.minecraft.inventory.container.Container container = mc.player.openContainer;
        if (container == null) return;
        // Ищем зелёное стекло (кнопка "назад") по слотам 9-17
        for (int i = 9; i < 18 && i < container.inventorySlots.size(); i++) {
            net.minecraft.item.ItemStack slot = container.getSlot(i).getStack();
            if (!slot.isEmpty() && slot.getItem() == net.minecraft.item.Items.LIME_STAINED_GLASS_PANE) {
                mc.playerController.windowClick(container.windowId, i, 0, ClickType.PICKUP, mc.player);
                return;
            }
        }
    }

    private static boolean hasItemInInventory(Class<? extends net.minecraft.item.Item> itemClass) {
        Minecraft mc = Minecraft.getInstance();
        for (int i = 0; i < 36; i++) {
            if (itemClass.isInstance(mc.player.inventory.getStackInSlot(i).getItem())) return true;
        }
        return false;
    }

    // Проверить есть ли в инвентаре или экипировке предмет с нужными ключевыми словами
    private static boolean hasItemByName(String ruKey, String enKey) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        // Проверяем весь инвентарь (0-35)
        for (int i = 0; i < 36; i++) {
            net.minecraft.item.ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            String name = stack.getDisplayName().getString().toLowerCase(java.util.Locale.ROOT);
            if (name.contains(ruKey) || name.contains(enKey)) return true;
        }
        // Проверяем слоты экипировки (шлем, нагрудник, поножи, ботинки)
        net.minecraft.inventory.EquipmentSlotType[] slots = {
            net.minecraft.inventory.EquipmentSlotType.HEAD,
            net.minecraft.inventory.EquipmentSlotType.CHEST,
            net.minecraft.inventory.EquipmentSlotType.LEGS,
            net.minecraft.inventory.EquipmentSlotType.FEET
        };
        for (net.minecraft.inventory.EquipmentSlotType slot : slots) {
            net.minecraft.item.ItemStack stack = mc.player.getItemStackFromSlot(slot);
            if (stack.isEmpty()) continue;
            String name = stack.getDisplayName().getString().toLowerCase(java.util.Locale.ROOT);
            if (name.contains(ruKey) || name.contains(enKey)) return true;
        }
        return false;
    }

    private static int countItemInInventory(net.minecraft.item.Item item) {
        Minecraft mc = Minecraft.getInstance();
        int count = 0;
        for (int i = 0; i < 36; i++) {
            net.minecraft.item.ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (stack.getItem() == item) count += stack.getCount();
        }
        return count;
    }

    private static int countAllWool() {
        Minecraft mc = Minecraft.getInstance();
        int count = 0;
        for (int i = 0; i < 36; i++) {
            net.minecraft.item.ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof net.minecraft.item.BlockItem) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static double horizontalDistanceTo(Minecraft mc, net.minecraft.util.math.BlockPos target) {
        double dx = target.getX() + 0.5 - mc.player.getPosX();
        double dz = target.getZ() + 0.5 - mc.player.getPosZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static net.minecraft.util.math.BlockPos getBridgeStartForCurrentMap() {
        if (bwAISelectedBridgeStart != null) return bwAISelectedBridgeStart;
        String mapKey = currentMapName;
        if (mapKey.isEmpty()) {
            if (mapBridgeStartsMulti.size() == 1) mapKey = mapBridgeStartsMulti.keySet().iterator().next();
            else if (mapBridgeStarts.size() == 1) mapKey = mapBridgeStarts.keySet().iterator().next();
        }
        if (mapKey.isEmpty()) return null;
        java.util.List<double[]> list = mapBridgeStartsMulti.get(mapKey);
        if (list != null && !list.isEmpty()) {
            net.minecraft.util.math.BlockPos ref = bwAIBasePos != null ? bwAIBasePos : Minecraft.getInstance().player.getPosition();
            double min = Double.MAX_VALUE;
            for (double[] p : list) {
                double dx = p[0] - ref.getX(), dz = p[2] - ref.getZ();
                min = Math.min(min, dx * dx + dz * dz);
            }
            java.util.List<double[]> nearby = new java.util.ArrayList<>();
            for (double[] p : list) {
                double dx = p[0] - ref.getX(), dz = p[2] - ref.getZ();
                if (dx * dx + dz * dz <= min + 64.0) nearby.add(p);
            }
            double[] bs = nearby.get(new java.util.Random().nextInt(nearby.size()));
            bwAISelectedBridgeStart = new net.minecraft.util.math.BlockPos(bs[0], bs[1], bs[2]);
            System.out.println("[BotMode] Bridge: выбран ближайший край базы " + bwAISelectedBridgeStart.getX() + "," + bwAISelectedBridgeStart.getY() + "," + bwAISelectedBridgeStart.getZ());
            return bwAISelectedBridgeStart;
        }
        double[] bs = mapBridgeStarts.get(mapKey);
        if (bs == null) return null;
        bwAISelectedBridgeStart = new net.minecraft.util.math.BlockPos(bs[0], bs[1], bs[2]);
        return bwAISelectedBridgeStart;
    }

    // v9: стоит ли бот на краю острова (впереди обрыв)
    private static boolean isAtVoidEdgeForAI(Minecraft mc) {
        net.minecraft.util.Direction fc = net.minecraft.util.Direction.fromAngle(mc.player.rotationYaw);
        net.minecraft.util.math.BlockPos front = mc.player.getPosition().offset(fc);
        net.minecraft.util.math.BlockPos frontBelow = front.down();
        return mc.world.getBlockState(front).getMaterial().isReplaceable()
                && mc.world.getBlockState(frontBelow).getMaterial().isReplaceable()
                && mc.world.getBlockState(frontBelow.down()).getMaterial().isReplaceable();
    }

    private static boolean hasReachedBridgeStart(Minecraft mc, net.minecraft.util.math.BlockPos pos) {
        if (bwAIBridgeStartReached) return true;
        boolean reached;
        if (pos == null) {
            reached = isAtVoidEdgeForAI(mc);
        } else {
            double h = horizontalDistanceTo(mc, pos);
            // v10: точка считается достигнутой только рядом с реальным краем.
            // Старый допуск 6 блоков позволял начать Scaffold ещё в зоне спавна.
            reached = h <= 1.35 || (h <= 2.75 && isAtVoidEdgeForAI(mc));
        }
        if (reached) {
            bwAIBridgeStartReached = true;
            bwBridgeStartReachedTick = bwAITickCount;
            bwBridgePendingSupport = null;
            bwBridgeSupportStableTicks = 0;
            bwBridgeLockedAxis = 0;
            bwBridgeScaffoldStuckTicks = 0;
            System.out.println("[BotMode] Bridge: край базы достигнут, включаем и вооружаем строительство");
        }
        return reached;
    }

    // v9: строим строго по доминирующей оси к центру (не по диагонали)
    private static net.minecraft.util.math.BlockPos getBridgeAxisTarget(Minecraft mc) {
        if (bwAIMidPos == null) return null;
        if (bwAIBridgeAxisTarget != null && horizontalDistanceTo(mc, bwAIBridgeAxisTarget) > 1.5) {
            return bwAIBridgeAxisTarget;
        }
        double dx = bwAIMidPos.getX() + 0.5 - mc.player.getPosX();
        double dz = bwAIMidPos.getZ() + 0.5 - mc.player.getPosZ();
        net.minecraft.util.math.BlockPos cur = mc.player.getPosition();
        if (Math.abs(dx) > Math.abs(dz)) {
            bwAIBridgeAxisTarget = new net.minecraft.util.math.BlockPos(bwAIMidPos.getX(), cur.getY(), cur.getZ());
        } else {
            bwAIBridgeAxisTarget = new net.minecraft.util.math.BlockPos(cur.getX(), cur.getY(), bwAIMidPos.getZ());
        }
        System.out.println("[BotMode] Bridge: строим прямо к " + bwAIBridgeAxisTarget.getX() + "," + bwAIBridgeAxisTarget.getZ());
        return bwAIBridgeAxisTarget;
    }

    private static boolean isGeneratorApproachPhase() {
        return bwAIPhase == BWAI_IDLE || bwAIPhase == BWAI_COLLECT_SPAWN;
    }

    private static void moveTowardsForAI(Minecraft mc, net.minecraft.util.math.BlockPos target) {
        if (isBaritoneControlling()) return; // движением управляет Baritone
        double dx = target.getX() + 0.5 - mc.player.getPosX();
        double dz = target.getZ() + 0.5 - mc.player.getPosZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < 1.5) return;

        mc.player.rotationYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        // Спринт во время передвижения (к генератору/закупке/центру).
        // Не спринтим при строительстве моста над бездной — можно перебежать край.
        boolean bridgingOverVoid = bwAIPhase == BWAI_BRIDGE
                && mc.world.getBlockState(mc.player.getPosition().down()).getMaterial().isReplaceable();
        boolean canSprint = !bridgingOverVoid && mc.player.getFoodStats().getFoodLevel() > 6;
        mc.player.setSprinting(canSprint);
        mc.player.moveForward = 1.0f;
        mc.player.moveStrafing = 0;

        // Обход стенок: если прямо по курсу стена — уходим вбок (strafe), а не упираемся.
        avoidWallForAI(mc);
    }

    // Если перед ботом на уровне ног ИЛИ головы твёрдая стена — стрейфим в свободную сторону.
    private static void avoidWallForAI(Minecraft mc) {
        net.minecraft.util.Direction facing = net.minecraft.util.Direction.fromAngle(mc.player.rotationYaw);
        net.minecraft.util.math.BlockPos feet = mc.player.getPosition();
        net.minecraft.util.math.BlockPos front = feet.offset(facing);
        net.minecraft.util.math.BlockPos frontHead = front.up();

        boolean wallAhead = isSolid(mc, front) || isSolid(mc, frontHead);
        if (!wallAhead) return;

        // Пробуем шагнуть на ступеньку (стена в 1 блок) — прыжок без спринта (античит)
        if (isSolid(mc, front) && !isSolid(mc, frontHead) && !isSolid(mc, front.up().up())) {
            mc.player.setSprinting(false);
            // LionsTemple: генератор ниже спавна. Здесь прыжок возвращал бота
            // наверх и создавал бесконечное подпрыгивание у цели.
            if (isGeneratorApproachPhase()) {
                mc.player.moveForward = 0f;
            } else if (bwAITickCount % 6 == 0) {
                mc.player.jump();
            }
            return;
        }

        // Иначе обходим: выбираем сторону, где свободно
        net.minecraft.util.Direction left = facing.rotateYCCW();
        net.minecraft.util.Direction right = facing.rotateY();
        boolean leftFree = !isSolid(mc, feet.offset(left)) && !isSolid(mc, feet.offset(left).up());
        boolean rightFree = !isSolid(mc, feet.offset(right)) && !isSolid(mc, feet.offset(right).up());

        if (rightFree) {
            mc.player.moveStrafing = -1.0f;
        } else if (leftFree) {
            mc.player.moveStrafing = 1.0f;
        } else {
            // Обе стороны заблокированы — тормозим, чтобы не толкаться в стену (античит)
            mc.player.moveForward = 0f;
            mc.player.setSprinting(false);
        }
    }

    private static boolean isSolid(Minecraft mc, net.minecraft.util.math.BlockPos pos) {
        net.minecraft.block.BlockState st = mc.world.getBlockState(pos);
        return !st.getMaterial().isReplaceable() && st.getMaterial().blocksMovement();
    }

    // Осевое движение для моста: двигаемся строго по X или Z (без диагонали),
    // чтобы античит не флагал диагональный мост.
    private static void moveTowardsAxisAlignedForAI(Minecraft mc, net.minecraft.util.math.BlockPos target) {
        if (isBaritoneControlling()) return; // движением управляет Baritone
        double dx = target.getX() + 0.5 - mc.player.getPosX();
        double dz = target.getZ() + 0.5 - mc.player.getPosZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < 1.0) return;

        // Выбираем доминирующую ось и снапим yaw к 0/90/180/270
        float yaw;
        if (Math.abs(dx) > Math.abs(dz)) {
            yaw = dx > 0 ? -90f : 90f; // движемся вдоль X
        } else {
            yaw = dz > 0 ? 0f : 180f;  // движемся вдоль Z
        }
        mc.player.rotationYaw = yaw;
        mc.player.setSprinting(false); // на прямом мосту не спринтим — стабильнее
        mc.player.moveForward = 1.0f;
        mc.player.moveStrafing = 0;

        // Обход чужих построек на мосту (например, застроенная кровать):
        // если прямо по курсу стена — перепрыгиваем ступеньку или обходим сбоку.
        net.minecraft.util.Direction facing = net.minecraft.util.Direction.fromAngle(mc.player.rotationYaw);
        net.minecraft.util.math.BlockPos feet = mc.player.getPosition();
        net.minecraft.util.math.BlockPos front = feet.offset(facing);
        net.minecraft.util.math.BlockPos frontHead = front.up();
        if (isSolid(mc, front) || isSolid(mc, frontHead)) {
            // Ступенька в 1 блок — прыгаем наверх
            if (isSolid(mc, front) && !isSolid(mc, frontHead) && !isSolid(mc, front.up().up())) {
                if (bwAITickCount % 6 == 0) mc.player.jump();
            } else {
                // Полноценная стена — обходим в свободную сторону
                net.minecraft.util.Direction left = facing.rotateYCCW();
                net.minecraft.util.Direction right = facing.rotateY();
                boolean rightFree = !isSolid(mc, feet.offset(right)) && !isSolid(mc, feet.offset(right).up());
                boolean leftFree = !isSolid(mc, feet.offset(left)) && !isSolid(mc, feet.offset(left).up());
                if (rightFree) mc.player.moveStrafing = -1.0f;
                else if (leftFree) mc.player.moveStrafing = 1.0f;
                else { mc.player.moveForward = 0f; }
            }
        }
    }

    // v12: безопасный мост без гонки с модулем Scaffold.
    // Бот стоит на месте, пока блок впереди-снизу не появится в мире и не
    // останется там несколько тиков. Только после подтверждения делает шаг.
    private static void safeBridgeStep(Minecraft mc, net.minecraft.util.math.BlockPos target) {
        if (target == null || !bwAIBridgeStartReached || !bwScaffoldEnabled) {
            mc.player.moveForward = 0f;
            return;
        }

        // v21/v23: подъём Scaffold Tower у стены центра имеет высший приоритет.
        if (bwMidClimbActive || shouldStartMidClimb(mc, target)) {
            bwBridgeFollowingExisting = false;
            midClimbStep(mc, target);
            return;
        }

        // Если уже начали безопасный уход на соседнюю линию, сначала завершаем его.
        if (bwBridgeTakeoverTarget != null) {
            if (horizontalDistanceTo(mc, bwBridgeTakeoverTarget) <= 0.70) {
                System.out.println("[BotMode] Teammate bridge: соседняя линия достигнута, продолжаем Scaffold");
                bwBridgeTakeoverTarget = null;
                bwBridgeLeaderNoStepTicks = 0;
                bwAIBridgeAxisTarget = null;
            } else {
                confirmedBridgeStep(mc, bwBridgeTakeoverTarget);
                return;
            }
        }

        net.minecraft.entity.player.PlayerEntity leader = findTeammateBridgeLeader(mc, target);
        net.minecraft.util.math.BlockPos existingStep = findSupportedBridgeStep(mc, target, leader);
        if (existingStep != null) {
            bwBridgeLeaderNoStepTicks = 0;
            bwBridgeFollowingExisting = true;
            // Не толкаем союзника с узкого моста: держим примерно два блока дистанции.
            if (leader != null && mc.player.getDistance(leader) < 2.25
                    && leader.getPosition().distanceSq(existingStep) <= 2.25) {
                mc.player.moveForward = 0f;
                mc.player.moveStrafing = 0f;
                return;
            }
            // v35: даже по уже готовому мосту заранее держим Scaffold в рабочем
            // состоянии (блок в руке + взгляд вниз). Иначе переключение в режим
            // строительства происходило только на последнем блоке, когда инерция
            // уже уносила бота в пустоту.
            confirmedBridgeStep(mc, existingStep);
            return;
        }

        bwBridgeFollowingExisting = false;
        if (leader != null && mc.player.getDistance(leader) < 6.0) {
            bwBridgeLeaderNoStepTicks++;
            // Союзник мог просто ставить следующий блок. Даём ему короткое время,
            // затем строим параллельную линию и продолжаем вместо него.
            if (bwBridgeLeaderNoStepTicks <= 12) {
                mc.player.moveForward = 0f;
                mc.player.moveStrafing = 0f;
                return;
            }
            bwBridgeTakeoverTarget = chooseBridgeTakeoverTarget(mc, target, leader);
            bwBridgeLeaderNoStepTicks = 0;
            if (bwBridgeTakeoverTarget != null) {
                System.out.println("[BotMode] Teammate bridge: союзник остановился, начинаем свою линию");
                confirmedBridgeStep(mc, bwBridgeTakeoverTarget);
                return;
            }
        } else {
            bwBridgeLeaderNoStepTicks = 0;
        }

        // Готового безопасного блока нет — ставим опору сами и ждём подтверждения мира.
        // Движение запрещено, пока серверный блок не появился и не прожил два тика.
        net.minecraft.util.math.BlockPos next = getForwardBridgeCell(mc, target);
        confirmedBridgeStep(mc, next);
    }

    private static net.minecraft.entity.player.PlayerEntity findTeammateBridgeLeader(
            Minecraft mc, net.minecraft.util.math.BlockPos target) {
        if (mc.world == null || mc.player == null) return null;
        double tx = target.getX() + 0.5 - mc.player.getPosX();
        double tz = target.getZ() + 0.5 - mc.player.getPosZ();
        double len = Math.sqrt(tx * tx + tz * tz);
        if (len < 0.1) return null;
        tx /= len; tz /= len;
        net.minecraft.entity.player.PlayerEntity best = null;
        double bestScore = -Double.MAX_VALUE;
        for (net.minecraft.entity.player.PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player || p.removed || !mc.player.isOnSameTeam(p)) continue;
            double dist = mc.player.getDistance(p);
            if (dist > 32.0 || Math.abs(p.getPosY() - mc.player.getPosY()) > 4.0) continue;
            double dx = p.getPosX() - mc.player.getPosX();
            double dz = p.getPosZ() - mc.player.getPosZ();
            double progress = dx * tx + dz * tz;
            if (progress < -1.0) continue;
            double lateral = Math.abs(dx * tz - dz * tx);
            if (lateral > 8.0) continue;
            double score = progress * 2.0 - lateral - dist * 0.15;
            if (score > bestScore) { bestScore = score; best = p; }
        }
        return best;
    }

    private static boolean isSafeExistingBridgeCell(Minecraft mc, net.minecraft.util.math.BlockPos cell) {
        if (mc.world.getBlockState(cell.down()).getMaterial().isReplaceable()
                || !mc.world.getBlockState(cell).getMaterial().isReplaceable()
                || !mc.world.getBlockState(cell.up()).getMaterial().isReplaceable()) return false;
        // Нельзя идти на единственный блок, если его уже занимает игрок: на узком мосту
        // коллизия сдвигает бота в пустоту даже при наличии блока под ногами.
        net.minecraft.util.math.AxisAlignedBB box = new net.minecraft.util.math.AxisAlignedBB(cell).grow(0.20);
        for (net.minecraft.entity.player.PlayerEntity p : mc.world.getPlayers()) {
            if (p != mc.player && !p.removed && p.getBoundingBox().intersects(box)) return false;
        }
        return true;
    }

    private static net.minecraft.util.math.BlockPos findSupportedBridgeStep(
            Minecraft mc, net.minecraft.util.math.BlockPos target,
            net.minecraft.entity.player.PlayerEntity leader) {
        net.minecraft.util.math.BlockPos feet = mc.player.getPosition();
        int dx = target.getX() - feet.getX();
        int dz = target.getZ() - feet.getZ();
        int fx = 0, fz = 0;
        if (Math.abs(dx) >= Math.abs(dz)) fx = dx >= 0 ? 1 : -1;
        else fz = dz >= 0 ? 1 : -1;
        int lx = -fz, lz = fx;
        int[][] offsets = {
                {fx, fz}, {fx + lx, fz + lz}, {fx - lx, fz - lz},
                {lx, lz}, {-lx, -lz}
        };
        net.minecraft.util.math.BlockPos best = null;
        double bestScore = -Double.MAX_VALUE;
        for (int[] o : offsets) {
            net.minecraft.util.math.BlockPos c = feet.add(o[0], 0, o[1]);
            if (!isSafeExistingBridgeCell(mc, c)) continue;
            double progress = o[0] * fx + o[1] * fz;
            if (progress < 0.0) continue;
            double score = progress * 12.0;
            if (leader != null) score -= c.distanceSq(leader.getPosition()) * 0.35;
            // Прямой блок предпочтительнее бокового при одинаковой близости.
            score -= Math.abs(o[0] * lx + o[1] * lz) * 0.5;
            if (score > bestScore) { bestScore = score; best = c; }
        }
        return best;
    }

    private static net.minecraft.util.math.BlockPos chooseBridgeTakeoverTarget(
            Minecraft mc, net.minecraft.util.math.BlockPos target,
            net.minecraft.entity.player.PlayerEntity leader) {
        net.minecraft.util.math.BlockPos feet = mc.player.getPosition();
        int dx = target.getX() - feet.getX(), dz = target.getZ() - feet.getZ();
        int fx = 0, fz = 0;
        if (Math.abs(dx) >= Math.abs(dz)) fx = dx >= 0 ? 1 : -1;
        else fz = dz >= 0 ? 1 : -1;
        int lx = -fz, lz = fx;
        double leaderSide = (leader.getPosX() - mc.player.getPosX()) * lx
                + (leader.getPosZ() - mc.player.getPosZ()) * lz;
        // Сначала один боковой блок (он примыкает к текущей опоре), затем обычная
        // логика пересчитает прямую ось и продолжит вперёд.
        int side = leaderSide >= 0 ? -1 : 1;
        return feet.add(lx * side, 0, lz * side);
    }

    private static net.minecraft.util.math.BlockPos getForwardBridgeCell(
            Minecraft mc, net.minecraft.util.math.BlockPos target) {
        net.minecraft.util.math.BlockPos feet = mc.player.getPosition();
        int dx = target.getX() - feet.getX(), dz = target.getZ() - feet.getZ();
        // v33: ось моста фиксируется, чтобы линия шла прямо без зигзага.
        // Ось меняется только если другая дельта больше минимум на 2 блока.
        boolean xAxis;
        if (bwBridgeLockedAxis == 1) xAxis = Math.abs(dx) + 2 >= Math.abs(dz);
        else if (bwBridgeLockedAxis == 2) xAxis = Math.abs(dx) > Math.abs(dz) + 2;
        else xAxis = Math.abs(dx) >= Math.abs(dz);
        bwBridgeLockedAxis = xAxis ? 1 : 2;
        if (xAxis) return feet.add(dx >= 0 ? 1 : -1, 0, 0);
        return feet.add(0, 0, dz >= 0 ? 1 : -1);
    }

    private static int findBestBridgeBlockSlot(Minecraft mc) {
        int best = -1, bestCount = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof net.minecraft.item.BlockItem
                    && stack.getCount() > bestCount) {
                best = i; bestCount = stack.getCount();
            }
        }
        return best;
    }

    private static boolean tryPlaceBridgeSupport(Minecraft mc,
            net.minecraft.util.math.BlockPos placePos) {
        if (!mc.world.getBlockState(placePos).getMaterial().isReplaceable()) return true;
        int slot = findBestBridgeBlockSlot(mc);
        if (slot < 0 || bwAITickCount - bwBridgeLastPlaceTick < 3) return false;
        mc.player.inventory.currentItem = slot;

        // Кликаем по твёрдому соседу и по грани, обращённой к пустой клетке.
        // processRightClick в воздух (старый код) блок не ставил.
        for (net.minecraft.util.Direction dir : net.minecraft.util.Direction.values()) {
            net.minecraft.util.math.BlockPos neighbor = placePos.offset(dir);
            if (mc.world.getBlockState(neighbor).getMaterial().isReplaceable()) continue;
            net.minecraft.util.Direction face = dir.getOpposite();
            net.minecraft.util.math.vector.Vector3d hitVec = new net.minecraft.util.math.vector.Vector3d(
                    neighbor.getX() + 0.5 + face.getXOffset() * 0.5,
                    neighbor.getY() + 0.5 + face.getYOffset() * 0.5,
                    neighbor.getZ() + 0.5 + face.getZOffset() * 0.5);
            net.minecraft.util.math.BlockRayTraceResult hit = new net.minecraft.util.math.BlockRayTraceResult(
                    hitVec, face, neighbor, false);
            mc.playerController.processRightClickBlock(mc.player, mc.world, net.minecraft.util.Hand.MAIN_HAND, hit);
            mc.player.swingArm(net.minecraft.util.Hand.MAIN_HAND);
            bwBridgeLastPlaceTick = bwAITickCount;
            return true;
        }
        return false;
    }

    private static void confirmedBridgeStep(Minecraft mc,
            net.minecraft.util.math.BlockPos nextFeet) {
        if (nextFeet == null) { mc.player.moveForward = 0f; return; }

        // v33: пользовательский Scaffold ставит блоки сам, как при ручной игре.
        // Бот НЕ ставит блоки через processRightClickBlock и НЕ ждёт подтверждений:
        // два установщика мешали друг другу и ломали ровный мост.
        if (bwScaffoldEnabled) {
            setModuleState("Scaffold", true);
            mc.player.setSneaking(false);
            mc.gameSettings.keyBindSneak.setPressed(false);
            // v34: как человек — блоки в руке и взгляд вниз на край последнего блока,
            // чтобы Scaffold видел грань и ставил блоки при горизонтальном ходе.
            int scafSlot = findBestBridgeBlockSlot(mc);
            if (scafSlot >= 0) mc.player.inventory.currentItem = scafSlot;
            double sdx = nextFeet.getX() + 0.5 - mc.player.getPosX();
            double sdz = nextFeet.getZ() + 0.5 - mc.player.getPosZ();
            mc.player.rotationYaw = (float) Math.toDegrees(Math.atan2(-sdx, sdz));
            mc.player.rotationPitch = 78f;
            mc.player.setSprinting(false);
            mc.player.moveStrafing = 0f;
            if (bwAITickCount - bwBridgeStartReachedTick < 2) {
                mc.player.moveForward = 0f;
                return;
            }
            net.minecraft.util.math.BlockPos support = nextFeet.down();
            boolean supportMissing = mc.world.getBlockState(support).getMaterial().isReplaceable();
            boolean feetMissing = mc.world.getBlockState(mc.player.getPosition().down()).getMaterial().isReplaceable();
            // Аварийный тормоз: на самом краю без опоры в воздух не шагаем.
            if (supportMissing && feetMissing) {
                mc.player.moveForward = 0f;
                mc.player.moveStrafing = 0f;
                mc.player.setMotion(mc.player.getMotion().mul(0.0D, 1.0D, 0.0D));
                bwBridgeScaffoldStuckTicks++;
                if (bwBridgeScaffoldStuckTicks > 2) tryPlaceBridgeSupport(mc, support);
                return;
            }
            if (supportMissing && horizontalDistanceTo(mc, nextFeet) < 1.35) {
                // v35: тормозим ДО края и полностью гасим горизонтальную инерцию.
                // В v34 замедление 0.15 включалось слишком поздно и бот всё равно
                // успевал соскользнуть с последнего готового блока.
                bwBridgeScaffoldStuckTicks++;
                mc.player.moveForward = 0f;
                mc.player.moveStrafing = 0f;
                mc.player.setMotion(mc.player.getMotion().mul(0.0D, 1.0D, 0.0D));
                if (bwBridgeScaffoldStuckTicks > 2) tryPlaceBridgeSupport(mc, support);
                return;
            }
            bwBridgeScaffoldStuckTicks = 0;
            mc.player.moveForward = 0.48f;
            return;
        }

        net.minecraft.util.math.BlockPos support = nextFeet.down();

        // v29: горизонтальный мост никогда не использует Sneak. Безопасность обеспечивает
        // полный запрет forward до серверного подтверждения опорного блока.
        mc.player.setSneaking(false);
        mc.gameSettings.keyBindSneak.setPressed(false);

        // В тот же тик, когда достигнут край, Scaffold только включается. Никакого шага.
        if (bwAITickCount - bwBridgeStartReachedTick < 4) {
            setModuleState("Scaffold", true);
            mc.player.moveForward = 0f;
            mc.player.moveStrafing = 0f;
            return;
        }

        if (mc.world.getBlockState(support).getMaterial().isReplaceable()) {
            bwBridgePendingSupport = support;
            bwBridgeSupportStableTicks = 0;
            mc.player.moveForward = 0f;
            mc.player.moveStrafing = 0f;
            mc.player.setSprinting(false);
            tryPlaceBridgeSupport(mc, support);
            return;
        }

        if (bwBridgePendingSupport != null && bwBridgePendingSupport.equals(support)) {
            bwBridgeSupportStableTicks++;
            if (bwBridgeSupportStableTicks < 2) {
                mc.player.moveForward = 0f;
                mc.player.moveStrafing = 0f;
                return;
            }
        } else {
            bwBridgePendingSupport = support;
            bwBridgeSupportStableTicks = 2; // готовый чужой/старый блок уже подтверждён
        }

        mc.player.setSneaking(false);
        mc.gameSettings.keyBindSneak.setPressed(false);
        moveBridgeCarefullyTo(mc, nextFeet, 0.34f);
        if (horizontalDistanceTo(mc, nextFeet) <= 0.58) {
            bwBridgePendingSupport = null;
            bwBridgeSupportStableTicks = 0;
        }
    }

    private static void moveBridgeCarefullyTo(Minecraft mc,
            net.minecraft.util.math.BlockPos target, float speed) {
        double dx = target.getX() + 0.5 - mc.player.getPosX();
        double dz = target.getZ() + 0.5 - mc.player.getPosZ();
        mc.player.rotationYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        mc.player.setSprinting(false);
        mc.player.setSneaking(false);
        mc.gameSettings.keyBindSneak.setPressed(false);
        mc.gameSettings.keyBindUseItem.setPressed(false);
        mc.player.moveStrafing = 0f;
        mc.player.moveForward = speed;
    }

    private static boolean shouldStartMidClimb(Minecraft mc, net.minecraft.util.math.BlockPos target) {
        if (mc.player == null || mc.world == null || bwAIMidPos == null || !mc.player.isOnGround()) return false;
        if (bwAIMidPos.getY() - mc.player.getPosY() < 2.0) return false;

        double dx = target.getX() + 0.5 - mc.player.getPosX();
        double dz = target.getZ() + 0.5 - mc.player.getPosZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        // Не реагируем на стены базы: подъём нужен только на последней части пути к mid.
        if (horizontal > 38.0) return false;

        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        net.minecraft.util.Direction facing = net.minecraft.util.Direction.fromAngle(yaw);
        net.minecraft.util.math.BlockPos feet = mc.player.getPosition();
        net.minecraft.util.math.BlockPos front = feet.offset(facing);
        boolean centerWall = isSolid(mc, front) || isSolid(mc, front.up());
        if (!centerWall) return false;

        bwMidClimbActive = true;
        bwMidClimbStage = 0;
        bwMidClimbAnchor = feet;
        bwMidClimbBestY = mc.player.getPosY();
        bwMidClimbLastRiseTick = bwAITickCount;
        bwMidClimbStartedTick = bwAITickCount;
        mc.gameSettings.keyBindJump.setPressed(false);
        cancelBaritone();
        System.out.println("[BotMode] Mid climb: стена центра обнаружена, отходим для лестницы");
        return true;
    }

    private static void midClimbStep(Minecraft mc, net.minecraft.util.math.BlockPos target) {
        if (!bwMidClimbActive || bwMidClimbAnchor == null) {
            resetMidClimb();
            return;
        }

        double dx = target.getX() + 0.5 - mc.player.getPosX();
        double dz = target.getZ() + 0.5 - mc.player.getPosZ();
        mc.player.rotationYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        mc.player.setSprinting(false);
        mc.player.setSneaking(false);
        mc.gameSettings.keyBindSneak.setPressed(false);
        mc.gameSettings.keyBindUseItem.setPressed(false);
        mc.player.moveStrafing = 0f;

        // Уже поднялись до уровня поверхности — дальше снова идём обычным маршрутом к mid.
        if (bwAIMidPos != null && mc.player.getPosY() >= bwAIMidPos.getY() - 0.25) {
            System.out.println("[BotMode] Mid climb: высота центра достигнута");
            resetMidClimb();
            normalScaffoldStep(mc, target);
            return;
        }

        if (bwMidClimbStage == 0) {
            double ax = mc.player.getPosX() - (bwMidClimbAnchor.getX() + 0.5);
            double az = mc.player.getPosZ() - (bwMidClimbAnchor.getZ() + 0.5);
            double backed = Math.sqrt(ax * ax + az * az);
            if (backed < 3.25) {
                // Смотрим на центр, но идём назад — строго вдоль уже готового моста.
                mc.gameSettings.keyBindJump.setPressed(false);
                mc.player.moveForward = -0.52f;
                return;
            }
            bwMidClimbStage = 1;
            bwMidClimbBestY = mc.player.getPosY();
            bwMidClimbLastRiseTick = bwAITickCount;
            System.out.println("[BotMode] Mid climb: начинаем лестницу");
        }

        // v23: точная копия рабочего ручного Tower из видео — стоим на месте,
        // Scaffold включён, а Space удерживается настоящим keyBindJump.
        mc.player.moveForward = 0f;
        mc.player.moveStrafing = 0f;
        mc.gameSettings.keyBindJump.setPressed(true);
        if (mc.player.getPosY() > bwMidClimbBestY + 0.30) {
            bwMidClimbBestY = mc.player.getPosY();
            bwMidClimbLastRiseTick = bwAITickCount;
        }

        // Если за 3 секунды высота не выросла, значит навес длиннее ожидаемого:
        // снова отходим ещё на три блока и повторяем лестницу с большим разбегом.
        if (bwAITickCount - bwMidClimbLastRiseTick > 60) {
            bwMidClimbStage = 0;
            bwMidClimbAnchor = mc.player.getPosition();
            bwMidClimbLastRiseTick = bwAITickCount;
            System.out.println("[BotMode] Mid climb: мало места, увеличиваем разбег");
        }

        // Аварийный перезапуск, чтобы состояние никогда не зависло навсегда.
        if (bwAITickCount - bwMidClimbStartedTick > 360) {
            System.out.println("[BotMode] Mid climb: таймаут, повторная попытка");
            bwMidClimbStage = 0;
            bwMidClimbAnchor = mc.player.getPosition();
            bwMidClimbStartedTick = bwAITickCount;
            bwMidClimbLastRiseTick = bwAITickCount;
        }
    }

    private static void resetMidClimb() {
        bwMidClimbActive = false;
        bwMidClimbStage = 0;
        bwMidClimbAnchor = null;
        bwMidClimbBestY = 0.0;
        bwMidClimbLastRiseTick = 0;
        bwMidClimbStartedTick = 0;
        try {
            if (mc != null && mc.gameSettings != null) {
                mc.gameSettings.keyBindJump.setPressed(false);
            }
        } catch (Throwable ignored) {}
    }

    // Normal: движение оставляем BotMode, а установку делает модуль Scaffold,
    // загруженный с полным пользовательским конфигом основного клиента.
    private static void normalScaffoldStep(Minecraft mc, net.minecraft.util.math.BlockPos target) {
        double dx = target.getX() + 0.5 - mc.player.getPosX();
        double dz = target.getZ() + 0.5 - mc.player.getPosZ();
        float yaw;
        if (Math.abs(dx) > Math.abs(dz)) yaw = dx > 0 ? -90f : 90f;
        else yaw = dz > 0 ? 0f : 180f;
        mc.player.rotationYaw = yaw;
        mc.gameSettings.keyBindSneak.setPressed(false);
        mc.gameSettings.keyBindUseItem.setPressed(false);
        mc.player.setSprinting(false);
        mc.player.setSneaking(false);
        mc.player.moveStrafing = 0f;
        mc.player.moveForward = 0.48f;
    }

    private static void placeBlockBelowForAI(Minecraft mc) {
        // v11: последний предохранитель для всех путей вызова.
        if (bwAIPhase != BWAI_BRIDGE || !bwAIBridgeStartReached) return;
        net.minecraft.util.math.BlockPos axisTarget = getBridgeAxisTarget(mc);
        safeBridgeStep(mc, axisTarget != null ? axisTarget : bwAIMidPos);
    }

    private static void defendBedForAI(Minecraft mc) {
        if (bwAIBedPos == null || bwAIWoolCount <= 0) return;
        net.minecraft.util.math.BlockPos[] offsets = {
                bwAIBedPos.north(), bwAIBedPos.south(), bwAIBedPos.east(), bwAIBedPos.west(), bwAIBedPos.up()
        };
        for (net.minecraft.util.math.BlockPos offset : offsets) {
            if (mc.world.getBlockState(offset).getMaterial().isReplaceable()) {
                for (int i = 0; i < 9; i++) {
                    net.minecraft.item.ItemStack stack = mc.player.inventory.getStackInSlot(i);
                    if (!stack.isEmpty() && stack.getItem() instanceof net.minecraft.item.BlockItem) {
                        mc.player.inventory.currentItem = i;
                        mc.playerController.processRightClick(mc.player, mc.world, net.minecraft.util.Hand.MAIN_HAND);
                        bwAIWoolCount--;
                        break;
                    }
                }
                break;
            }
        }
    }

    private static int findItemInHotbar(net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = Minecraft.getInstance().player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isBedItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        net.minecraft.item.Item item = stack.getItem();
        return item == Items.RED_BED || item == Items.WHITE_BED || item == Items.ORANGE_BED
                || item == Items.MAGENTA_BED || item == Items.LIGHT_BLUE_BED || item == Items.YELLOW_BED
                || item == Items.LIME_BED || item == Items.PINK_BED || item == Items.GRAY_BED
                || item == Items.LIGHT_GRAY_BED || item == Items.CYAN_BED || item == Items.PURPLE_BED
                || item == Items.BLUE_BED || item == Items.BROWN_BED || item == Items.GREEN_BED
                || item == Items.BLACK_BED;
    }

    private static void readControlFile() {
        followTarget = null;
        try {
            if (Files.exists(CONTROL_FILE)) {
                List<String> lines = Files.readAllLines(CONTROL_FILE);
                if (!lines.isEmpty()) {
                    for (String line : lines) {
                        line = line.trim();
                        if (line.startsWith("follow=")) {
                            String rest = line.substring(7).trim();
                            int colon = rest.indexOf(':');
                            if (colon >= 0) {
                                followTarget = rest.substring(0, colon).trim();
                                try {
                                    followDistance = Double.parseDouble(rest.substring(colon + 1).trim());
                                } catch (NumberFormatException ignored) {}
                            } else {
                                followTarget = rest;
                            }
                            if (followTarget.isEmpty()) followTarget = null;
                        } else if (line.startsWith("autoSkin=")) {
                            autoSkinEnabled = Boolean.parseBoolean(line.substring(9).trim());
                        } else if (line.startsWith("skinName=")) {
                            autoSkinName = line.substring(9).trim();
                        } else if (line.startsWith("autoRegister=")) {
                            autoRegisterEnabled = Boolean.parseBoolean(line.substring(13).trim());
                        } else if (line.startsWith("registerPassword=")) {
                            registerPassword = line.substring(17).trim();
                        } else if (line.startsWith("bwenter=")) {
                            bwEnterEnabled = Boolean.parseBoolean(line.substring(8).trim());
                        } else if (line.startsWith("bwenterleave=")) {
                            bwEnterLeaveEnabled = Boolean.parseBoolean(line.substring(13).trim());
                        } else if (line.startsWith("bwjoin=")) {
                            String bwNum = line.substring(7).trim();
                            if (!bwNum.isEmpty() && !bwNum.equals(lastBWJoinNumber)) {
                                lastBWJoinNumber = bwNum;
                                mc.player.sendChatMessage("/bw rjoin BW-" + bwNum);
                                System.out.println("[BotMode] BWJoin: /bw rjoin BW-" + bwNum);
                            }
                        } else if (line.startsWith("bwai=")) {
                            boolean newBwAI = Boolean.parseBoolean(line.substring(5).trim());
                            if (newBwAI != bwAIEnabled) {
                                // При смене состояния BedWars AI — принудительно перечитываем конфиг модулей,
                                // чтобы пользовательские настройки Scaffold/HitAura/Sprint/Velocity вступили в силу
                                moduleConfigLastModified = 0;
                            }
                            bwAIEnabled = newBwAI;
                        } else if (line.startsWith("llm_strategist=")) {
                            bwLlmStrategistEnabled = Boolean.parseBoolean(line.substring(15).trim());
                        } else if (line.startsWith("team_ai=")) {
                            bwTeamAIEnabled = Boolean.parseBoolean(line.substring(line.indexOf('=') + 1).trim());
                        } else if (line.startsWith("team_id=")) {
                            bwTeamId = line.substring(line.indexOf('=') + 1).trim();
                        } else if (line.startsWith("team_decision_seconds=")) {
                            try { bwTeamDecisionSeconds = Math.max(2, Math.min(20, Integer.parseInt(line.substring(line.indexOf('=') + 1).trim()))); } catch (NumberFormatException ignored) {}
                        } else if (line.startsWith("bwai_strategy=")) {
                            bwAIStrategy = line.substring(14).trim();
                        } else if (line.startsWith("bwai_maxtarget=")) {
                            bwAIMaxTarget = line.substring(15).trim();
                        } else if (line.startsWith("bwai_extrairon_enabled=")) {
                            bwAIExtraIronEnabled = Boolean.parseBoolean(line.substring(line.indexOf('=') + 1).trim());
                        } else if (line.startsWith("bwai_extrairon=")) {
                            try { bwAIExtraIron = Math.max(1, Math.min(64, Integer.parseInt(line.substring(line.indexOf('=') + 1).trim()))); } catch (NumberFormatException ignored) {}
                        } else if (line.startsWith("bwai_extragold_enabled=")) {
                            bwAIExtraGoldEnabled = Boolean.parseBoolean(line.substring(line.indexOf('=') + 1).trim());
                        } else if (line.startsWith("bwai_extragold=")) {
                            try { bwAIExtraGold = Math.max(1, Math.min(64, Integer.parseInt(line.substring(line.indexOf('=') + 1).trim()))); } catch (NumberFormatException ignored) {}
                        } else if (line.startsWith("bwai_buydelay=")) {
                            try { bwAIBuyDelay = Integer.parseInt(line.substring(14).trim()); } catch (NumberFormatException ignored) {}
                        } else if (line.startsWith("bwai_bridgeblocks=")) {
                            try { bwAIBridgeBlocks = Integer.parseInt(line.substring(18).trim()); } catch (NumberFormatException ignored) {}
                        } else if (line.startsWith("bwai_fightrange=")) {
                            try { bwAIFightRange = Float.parseFloat(line.substring(16).trim()); } catch (NumberFormatException ignored) {}
                        } else if (line.startsWith("bwai_collectradius=")) {
                            try { bwAICollectRadius = Float.parseFloat(line.substring(19).trim()); } catch (NumberFormatException ignored) {}
                        } else if (line.startsWith("bwai_defendbed=")) {
                            bwAIAutoDefendBed = Boolean.parseBoolean(line.substring(15).trim());
                        } else if (line.startsWith("bwai_buyarmor=")) {
                            bwAIBuyArmor = Boolean.parseBoolean(line.substring(14).trim());
                        } else if (line.startsWith("bwai_buysword=")) {
                            bwAIBuySword = Boolean.parseBoolean(line.substring(14).trim());
                        } else if (line.startsWith("bwai_buypickaxe=")) {
                            bwAIBuyPickaxe = Boolean.parseBoolean(line.substring(16).trim());
                        } else if (line.startsWith("bwai_buyblocks=")) {
                            bwAIBuyBlocks = Boolean.parseBoolean(line.substring(15).trim());
                        } else if (line.startsWith("bwai_onlyoneblock=")) {
                            bwAIOnlyOneBlock = Boolean.parseBoolean(line.substring(18).trim());
                        } else if (line.startsWith("bwai_gendistance=")) {
                            try { bwGenDistance = Float.parseFloat(line.substring(17).trim()); } catch (NumberFormatException ignored) {}
                        } else if (line.startsWith("bwinv_enabled=")) {
                            bwInvEnabled = Boolean.parseBoolean(line.substring(14).trim());
                        } else if (line.startsWith("bwinv_mode=")) {
                            bwInvMode = line.substring(11).trim();
                        } else if (line.startsWith("bwinv_mindelay=")) {
                            try { bwInvMinDelay = Integer.parseInt(line.substring(15).trim()); } catch (NumberFormatException ignored) {}
                        } else if (line.startsWith("bwinv_maxdelay=")) {
                            try { bwInvMaxDelay = Integer.parseInt(line.substring(15).trim()); } catch (NumberFormatException ignored) {}
                        } else if (line.startsWith("bwinv_autoarmor=")) {
                            bwInvAutoArmor = Boolean.parseBoolean(line.substring(16).trim());
                        } else if (line.startsWith("bwinv_armorhotbar=")) {
                            bwInvArmorHotbar = Boolean.parseBoolean(line.substring(18).trim());
                        } else if (line.startsWith("bwinv_dropgarbage=")) {
                            bwInvDropGarbage = Boolean.parseBoolean(line.substring(18).trim());
                        } else if (line.startsWith("bwinv_garbagehotbar=")) {
                            bwInvGarbageHotbar = Boolean.parseBoolean(line.substring(20).trim());
                        } else if (line.startsWith("bwinv_blockorder=")) {
                            bwInvBlockOrder = line.substring(17).trim();
                        } else if (line.matches("bwinv_slot[1-9]=.*")) {
                            int eq = line.indexOf('=');
                            int slot = Integer.parseInt(line.substring(10, eq)) - 1;
                            if (slot >= 0 && slot < 9) bwInvSlots[slot] = line.substring(eq + 1).trim();
                        } else if (line.startsWith("scaffold=")) {
                            bwScaffoldEnabled = Boolean.parseBoolean(line.substring(9).trim());
                        } else if (line.startsWith("bwai_baritone=")) {
                            bwBaritoneNav = Boolean.parseBoolean(line.substring(14).trim());
                        } else if (line.startsWith("brain=")) {
                            bwAIBrainEnabled = Boolean.parseBoolean(line.substring(6).trim());
                        } else if (line.startsWith("brainchat=")) {
                            bwAIChatEnabled = Boolean.parseBoolean(line.substring(10).trim());
                        } else if (line.startsWith("brainrecord=")) {
                            bwAIRecordEnabled = Boolean.parseBoolean(line.substring(12).trim());
                        }
                    }
                    return;
                }
            }
        } catch (IOException ignored) {
        }
    }

    private static void reloadModuleConfig() {
        try {
            File f = MODULE_CONFIG_PATH.toFile();
            if (!f.exists()) return;
            long modified = f.lastModified();
            if (modified == moduleConfigLastModified && moduleConfigLastModified != 0) return;
            moduleConfigLastModified = modified;

            String content = Files.readString(MODULE_CONFIG_PATH);
            JsonObject json = new com.google.gson.JsonParser().parse(content).getAsJsonObject();
            JsonObject modules = json.getAsJsonObject("modules");
            if (modules == null) return;

            // Разрешённые модули — НЕ трогаем
            java.util.Set<String> allowed = new java.util.HashSet<>(java.util.Arrays.asList(
                "botattack", "bwautoleave", "bwjoinhelper", "autoskin",
                "autoregister", "notifications", "hud", "clickgui",
                "streamermode", "chathelper", "invmanager",
                "scaffold"
            ));

            for (Module m : Harmony.getInstance().getModuleManager().getModules()) {
                String key = m.getName().toLowerCase();
                if (!modules.has(key)) continue;

                // Разрешённые — пропускаем
                if (allowed.contains(key)) continue;

                // Combat/Movement/Player — выключаем ТОЛЬКО если BedWars AI активен.
                // Когда BedWars AI выключен — применяем состояние из конфига модуля,
                // чтобы пользователь мог сам включать Scaffold/HitAura/Sprint/Velocity.
                Category cat = m.getCategory();
                if (cat == Category.Combat || cat == Category.Movement || cat == Category.Player) {
                    if (bwAIEnabled) {
                        if (m.isState()) {
                            m.setState(false, true);
                            System.out.println("[BotMode] Blocked: " + m.getName());
                        }
                        continue;
                    }
                    JsonObject modJson = modules.getAsJsonObject(key);
                    if (!modJson.has("state")) continue;
                    boolean targetState = modJson.get("state").getAsBoolean();
                    if (m.isState() != targetState) {
                        m.setState(targetState, true);
                    }
                    continue;
                }

                // Если BedWars AI активен — не применяем конфиг
                if (bwAIEnabled) continue;

                JsonObject modJson = modules.getAsJsonObject(key);
                if (!modJson.has("state")) continue;
                boolean targetState = modJson.get("state").getAsBoolean();
                if (m.isState() != targetState) {
                    m.setState(targetState, true);
                }
            }
        } catch (Exception e) {
            System.err.println("[BotMode] reloadModuleConfig failed: " + e.getMessage());
        }
    }

    // ===== Baritone-навигация =====

    // Инициализация настроек Baritone (один раз) под задачи бота:
    // навигация по земле с обходом препятствий; мост через бездну — не Baritone, а Scaffold.
    private static void initBaritoneSettings() {
        if (bwBaritoneInited) return;
        try {
            Settings s = BaritoneAPI.getSettings();
            s.allowPlace.value = false;   // Baritone НЕ мостит и НЕ пилларит (мост — Scaffold, tower запрещён)
            s.allowBreak.value = false;   // ничего не ломать
            s.allowParkour.value = true;  // обычные прыжки через щели разрешены (античит их не флагает)
            s.allowSprint.value = true;   // спринт разрешён
            s.allowInventory.value = false;
            bwBaritoneInited = true;
            System.out.println("[BotMode] Baritone settings initialized");
        } catch (Throwable t) {
            System.err.println("[BotMode] initBaritoneSettings failed: " + t.getMessage());
        }
    }

    // Задать цель Baritone (не перепутить, если цель та же).
    private static void baritoneGoto(net.minecraft.util.math.BlockPos target) {
        if (target == null) return;
        try {
            initBaritoneSettings();
            if (bwBaritoneCurGoal != null && bwBaritoneCurGoal.equals(target)) return;
            bwBaritoneCurGoal = target;
            bwBaritoneGoalTick = bwAITickCount;
            bwBaritoneFailed = false;
            BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess()
                    .setGoalAndPath(new GoalNear(target, 1));
            System.out.println("[BotMode] Baritone goto " + target.getX() + "," + target.getY() + "," + target.getZ());
        } catch (Throwable t) {
            System.err.println("[BotMode] baritoneGoto failed: " + t.getMessage());
        }
    }

    // Полностью остановить Baritone.
    private static void cancelBaritone() {
        try {
            bwBaritoneCurGoal = null;
            IPathingBehavior pb = BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior();
            if (pb != null && (pb.isPathing() || pb.hasPath() || pb.getGoal() != null)) {
                pb.cancelEverything();
                pb.forceCancel();
            }
        } catch (Throwable t) {
            System.err.println("[BotMode] cancelBaritone failed: " + t.getMessage());
        }
    }

    // Является ли фаза «навигационной» для Baritone (перемещение по земле).
    private static boolean isBaritoneNavPhase() {
        if (!bwBaritoneNav || !bwAIEnabled) return false;
        switch (bwAIPhase) {
            case BWAI_IDLE:
            case BWAI_COLLECT_SPAWN:
            case BWAI_RETURN_BASE:
            case BWAI_BRIDGE:
            case BWAI_COLLECT:
                return true;
            default:
                return false;
        }
    }

    // Реально ли сейчас Baritone управляет движением: навигационная фаза,
    // есть выданная цель, Baritone не «сдался». Если после выдачи цели за grace-период
    // путь так и не появился — считаем, что Baritone не справился, и отдаём ручное управление.
    private static boolean isBaritoneControlling() {
        if (!isBaritoneNavPhase()) return false;
        if (bwBaritoneCurGoal == null) return false;
        if (bwBaritoneFailed) return false;
        try {
            IPathingBehavior pb = BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior();
            if (pb == null) return false;
            if (pb.isPathing() || pb.hasPath()) return true;
            // Grace-период: 120 тиков (6с) на расчёт пути после выдачи цели
            if (bwAITickCount - bwBaritoneGoalTick <= 120) return true;
            // Путь так и не появился — Baritone не справился
            bwBaritoneFailed = true;
            System.out.println("[BotMode] Baritone: no path, fallback to manual");
            return false;
        } catch (Throwable t) {
            return false;
        }
    }

    // ===== Neural Brain (BotBrain) =====

    private static void tickBotBrain(Minecraft mc) {
        try {
            BotBrainState s = new BotBrainState();
            double px = mc.player.getPosX(), py = mc.player.getPosY(), pz = mc.player.getPosZ();
            s.selfX = (float) px; s.selfY = (float) py; s.selfZ = (float) pz;

            // Центр карты
            double cx = 0, cy = 0, cz = 0; boolean haveCenter = false;
            if (bwAIMidPos != null) { cx = bwAIMidPos.getX(); cy = bwAIMidPos.getY(); cz = bwAIMidPos.getZ(); haveCenter = true; }
            else if (!mapCenters.isEmpty()) {
                double[] c = mapCenters.values().iterator().next();
                cx = c[0]; cy = c[1]; cz = c[2]; haveCenter = true;
            }
            if (haveCenter) s.distToCenter = (float) Math.sqrt(distSq(px, py, pz, cx, cy, cz));

            // Генераторы по подстроке в ключе
            s.distToEmerald = nearestGenDist(px, py, pz, "emerald");
            s.distToDiamond = nearestGenDist(px, py, pz, "diamond");
            s.distToOwnGen = (currentGeneratorPos != null)
                    ? (float) Math.sqrt(distSq(px, py, pz, currentGeneratorPos[0], currentGeneratorPos[1], currentGeneratorPos[2]))
                    : 0f;
            s.distToEnemyBed = (bwAIBedPos != null)
                    ? (float) Math.sqrt(distSq(px, py, pz, bwAIBedPos.getX(), bwAIBedPos.getY(), bwAIBedPos.getZ()))
                    : 0f;

            // Ресурсы (считаются в AI-тике, берём актуальные значения)
            s.iron = bwAIIronCount;
            s.gold = bwAIGoldCount;
            s.emerald = bwAIEmeraldCount;
            s.diamond = bwAIDiamondCount;
            s.blocks = bwAIWoolCount;
            s.health = mc.player.getHealth();
            s.armor = (float) mc.player.getTotalArmorValue();
            s.phase = mapPhase(bwAIPhase);
            s.bridging = (bwAIPhase == BWAI_BRIDGE) ? 1f : 0f;
            // Цель от игрока: стратегия + план закупа (мозг "следует" им)
            s.strategyIndex = strategyToIndex(bwAIStrategy);
            int[] tgt = getEffectiveResourceTarget();
            s.ironTarget = tgt[0];
            s.goldTarget = tgt[1];

            BotBrain brain = BotBrain.getInstance();
            BotBrainDecision d = brain.decide(s);
            lastBrainDecision = d;

            // Чат (только если включён toggle и прошёл порог/кулдаун)
            if (bwAIChatEnabled && d.chatIndex >= 0 && d.chatConfidence >= BotBrainChat.SEND_THRESHOLD
                    && (bwAITickCount - lastBrainChatTick) > 200) {
                BotBrainChat.maybeSend(d, true);
                lastBrainChatTick = bwAITickCount;
            }

            // Запись демо для обучения
            if (bwAIRecordEnabled) {
                recordDemo(s, d);
            }
        } catch (Exception e) {
            // Мозг не должен крашить игру
            System.err.println("[BotMode] tickBotBrain error: " + e.getMessage());
        }
    }

    private static double distSq(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x1 - x2, dy = y1 - y2, dz = z1 - z2;
        return dx * dx + dy * dy + dz * dz;
    }

    private static float nearestGenDist(double px, double py, double pz, String keySub) {
        float best = -1f; boolean found = false;
        for (java.util.Map.Entry<String, double[]> e : generatorPositions.entrySet()) {
            if (e.getKey().toLowerCase(java.util.Locale.ROOT).contains(keySub)) {
                double[] p = e.getValue();
                float d = (float) Math.sqrt(distSq(px, py, pz, p[0], p[1], p[2]));
                if (!found || d < best) { best = d; found = true; }
            }
        }
        return best;
    }

    private static BotBrainState.Phase mapPhase(int phase) {
        switch (phase) {
            case BWAI_BRIDGE: return BotBrainState.Phase.BRIDGE;
            case BWAI_COLLECT:
            case BWAI_COLLECT_SPAWN: return BotBrainState.Phase.COLLECT_IRON;
            default: return BotBrainState.Phase.SPAWN;
        }
    }

    private static int strategyToIndex(String strat) {
        if (strat == null) return 0;
        switch (strat.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "rush mid": return 1;
            case "defensive": return 2;
            case "aggressive": return 3;
            case "aggressivemax": return 4;
            default: return 0; // Balanced
        }
    }

    /** Парсит "30i 6g" -> [ironTarget, goldTarget]. */
    private static int[] parsePurchasePlan(String plan) {
        int iron = 30, gold = 6;
        if (plan != null) {
            for (String tok : plan.toLowerCase(java.util.Locale.ROOT).split("\\s+")) {
                tok = tok.trim();
                if (tok.isEmpty()) continue;
                if (tok.contains("i")) {
                    try { iron = Integer.parseInt(tok.replace("i", "").trim()); } catch (Exception ignored) {}
                } else if (tok.contains("g")) {
                    try { gold = Integer.parseInt(tok.replace("g", "").trim()); } catch (Exception ignored) {}
                }
            }
        }
        return new int[]{iron, gold};
    }

    private static int[] getEffectiveResourceTarget() {
        int[] base = parsePurchasePlan(bwAIMaxTarget);
        int extraIron = bwAIExtraIronEnabled ? Math.max(1, Math.min(64, bwAIExtraIron)) : 0;
        int extraGold = bwAIExtraGoldEnabled ? Math.max(1, Math.min(64, bwAIExtraGold)) : 0;
        return new int[]{base[0] + extraIron, base[1] + extraGold};
    }

    private static void recordDemo(BotBrainState s, BotBrainDecision d) {
        try {
            java.nio.file.Path p = java.nio.file.Paths.get("E:\\Мои Сурсы\\harmony\\bot_demos.jsonl");
            float[] in = s.toVector();
            // Целевой вектор в пространстве tanh (-1..1), как выдаёт forward()
            float[] out = new float[BotBrain.OUTPUT_DIM];
            out[0] = d.bridgeDesire * 2f - 1f;
            out[1] = d.bridgeTargetCenter ? 1f : -1f;
            out[2] = clampT(d.emeraldWaitTicks / 600f) * 2f - 1f;
            out[3] = clampT(d.diamondWaitTicks / 600f) * 2f - 1f;
            for (int i = 0; i < 5; i++) out[4 + i] = (i == d.chatIndex) ? 1f : -1f;

            StringBuilder sb = new StringBuilder();
            sb.append("{\"in\":[");
            for (int i = 0; i < in.length; i++) { if (i > 0) sb.append(','); sb.append(round(in[i])); }
            sb.append("],\"out\":[");
            for (int i = 0; i < out.length; i++) { if (i > 0) sb.append(','); sb.append(round(out[i])); }
            sb.append("]}\n");
            java.nio.file.Files.write(p, sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception ignored) {
        }
    }

    private static float clampT(float v) { return v < 0f ? 0f : (v > 1f ? 1f : v); }
    private static float round(float v) { return Math.round(v * 1000f) / 1000f; }

    /** Возвращает последнее решение мозга (для可能的 интеграции в логику мостов/сбора). */
    public static BotBrainDecision getLastBrainDecision() { return lastBrainDecision; }

    @Subscribe
    public void onInput(EventInput event) {
        if (!bwAIEnabled) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.world == null) return;

        // Автодвижение для всех фаз BedWars AI через EventInput
        {
            // Загружаем генератор — повторяем каждые 20 тиков пока не найдём
            if (currentGeneratorPos == null && bwGameStarted) {
                if (bwAITickCount % 20 == 0) {
                    loadGeneratorForCurrentMap();
                }
                // Фолбэк: определение по координатам спавна
                if (currentGeneratorPos == null && !spawnDetected && bwAIBasePos != null) {
                    detectMapBySpawn(mc);
                }
            }

            // Фолбэк координат генератора — ДО ветки Baritone, чтобы цель могла назначиться.
            if ((bwAIPhase == BWAI_IDLE || bwAIPhase == BWAI_COLLECT_SPAWN)
                    && currentGeneratorPos == null && bwGameStarted && bwAIBasePos != null) {
                currentGeneratorPos = new double[]{14398, 33, 14478};
                currentMapName = "LionsTemple";
                System.out.println("[BotMode] Fallback: using LionsTemple generator coords");
            }

            // === Baritone-навигация: если рулит Baritone — задаём цель и НЕ инжектим ручной ввод ===
            if (isBaritoneNavPhase()) {
                net.minecraft.util.math.BlockPos navTarget = null;
                if ((bwAIPhase == BWAI_IDLE || bwAIPhase == BWAI_COLLECT_SPAWN) && currentGeneratorPos != null) {
                    navTarget = new net.minecraft.util.math.BlockPos(
                            currentGeneratorPos[0], currentGeneratorPos[1], currentGeneratorPos[2]);
                    if (horizontalDistanceTo(mc, navTarget) <= Math.max(1.25, bwGenDistance)) navTarget = null;
                } else if (bwAIPhase == BWAI_RETURN_BASE && bwAIBasePos != null) {
                    navTarget = bwAIBasePos;
                } else if (bwAIPhase == BWAI_COLLECT) {
                    navTarget = getCurrentCenterResourceTarget(mc);
                    if (navTarget != null && mc.player.getDistance(navTarget) <= 1.85) navTarget = null;
                } else if (bwAIPhase == BWAI_BRIDGE) {
                    // Этап 3: Baritone на твёрдой земле, Scaffold над бездной
                    net.minecraft.util.math.BlockPos f = mc.player.getPosition();
                    boolean overVoidHere = mc.world.getBlockState(f.down()).getMaterial().isReplaceable();
                    if (overVoidHere) {
                        // Над бездной — Baritone не может ставить блоки, отменяем, переходим на ручной Scaffold
                        cancelBaritone();
                    } else {
                        // На твёрдой земле сначала спускаемся к нижней точке моста.
                        if (bwAIMidPos == null) bwAIMidPos = findMidPosForAI(mc);
                        net.minecraft.util.math.BlockPos bridgeStart = getBridgeStartForCurrentMap();
                        navTarget = bridgeStart != null && !hasReachedBridgeStart(mc, bridgeStart)
                                ? bridgeStart : bwAIMidPos;
                    }
                }
                if (navTarget != null) {
                    baritoneGoto(navTarget);
                } else if (bwAIPhase != BWAI_BRIDGE) {
                    cancelBaritone();
                }
                // Заглушаем ручной ввод ТОЛЬКО если Baritone реально управляет.
                // Иначе (нет цели / Baritone не справился / над бездной) — падаем в ручной режим ниже.
                if (isBaritoneControlling()) {
                    if (bwAIPhase == BWAI_BRIDGE && bwAIBridgeStartReached) {
                        // У края Baritone больше не должен перезаписывать безопасную скорость.
                        cancelBaritone();
                    } else {
                        return; // Baritone управляет движением сам
                    }
                }
            }

            // v12: на самом мосту EventInput не имеет права выставить forward=1.0.
            // Используем только скорость, которую разрешил safeBridgeStep после
            // подтверждения опоры сервером.
            if (bwAIPhase == BWAI_BRIDGE && bwAIBridgeStartReached) {
                if (bwAIMidPos == null) bwAIMidPos = findMidPosForAI(mc);
                net.minecraft.util.math.BlockPos axisT = getBridgeAxisTarget(mc);
                safeBridgeStep(mc, axisT != null ? axisT : bwAIMidPos);
                event.setForward(mc.player.moveForward);
                event.setStrafe(0f);
                event.setSprintState(false);
                // v23: Scaffold Tower должен видеть тот же Space, который пользователь
                // удерживает вручную. В остальных подрежимах моста прыжок запрещён.
                event.setJump(bwMidClimbActive && bwMidClimbStage == 1);
                MoveUtils.fixMovement(event, mc.player.rotationYaw);
                return;
            }

            net.minecraft.util.math.BlockPos target = null;

            if (bwAITickCount % 40 == 0) {
                System.out.println("[BotMode] onInput: phase=" + bwAIPhase + " genPos=" + (currentGeneratorPos != null ? (int)currentGeneratorPos[0]+","+(int)currentGeneratorPos[1]+","+(int)currentGeneratorPos[2] : "null") + " map=" + currentMapName + " chatMap=" + chatMapName);
            }

            if (bwAIPhase == BWAI_IDLE && currentGeneratorPos != null) {
                // Идём к генератору пока собираем ресурсы
                net.minecraft.util.math.BlockPos genPos = new net.minecraft.util.math.BlockPos(
                    currentGeneratorPos[0], currentGeneratorPos[1], currentGeneratorPos[2]);
                if (horizontalDistanceTo(mc, genPos) > Math.max(1.25, bwGenDistance)) {
                    target = genPos;
                }
            } else if (bwAIPhase == BWAI_IDLE && bwGameStarted && bwAIBasePos != null) {
                // Фолбэк: если генератор не найден, ставим LionsTemple по умолчанию
                if (currentGeneratorPos == null) {
                    currentGeneratorPos = new double[]{14398, 33, 14478};
                    currentMapName = "LionsTemple";
                    System.out.println("[BotMode] Fallback: using LionsTemple generator coords");
                }
                net.minecraft.util.math.BlockPos genPos = new net.minecraft.util.math.BlockPos(
                    currentGeneratorPos[0], currentGeneratorPos[1], currentGeneratorPos[2]);
                if (horizontalDistanceTo(mc, genPos) > Math.max(1.25, bwGenDistance)) {
                    target = genPos;
                } else {
                    // Уже у генератора — смотрим в сторону генератора, не дёргаемся
                    target = genPos;
                    // Логируем ресурсы
                    if (bwAITickCount % 40 == 0) {
                        System.out.println("[BotMode] At generator, iron=" + bwAIIronCount + " gold=" + bwAIGoldCount + " emerald=" + bwAIEmeraldCount);
                    }
                }
            } else if (bwAIPhase == BWAI_BUY_SETUP && !bwAIShopOpen) {
                // Идём к жителю "Магазин" для покупки
                net.minecraft.entity.merchant.villager.VillagerEntity shop = findNearestShopForAI(mc);
                if (shop != null && mc.player.getDistance(shop) > 2.0) {
                    target = shop.getPosition();
                }
            } else if (bwAIPhase == BWAI_BRIDGE || bwAIPhase == BWAI_COLLECT || bwAIPhase == BWAI_RETURN_BASE) {
                // Идём к центру или к цели
                if (bwAIPhase == BWAI_BRIDGE) {
                    if (bwAIMidPos == null) bwAIMidPos = findMidPosForAI(mc);
                    net.minecraft.util.math.BlockPos bridgeStart = getBridgeStartForCurrentMap();
                    if (bridgeStart != null && !hasReachedBridgeStart(mc, bridgeStart)) {
                        target = bridgeStart;
                    } else {
                        net.minecraft.util.math.BlockPos axisT = getBridgeAxisTarget(mc);
                        target = axisT != null ? axisT : bwAIMidPos;
                    }
                } else if (bwAIPhase == BWAI_COLLECT) {
                    target = getCurrentCenterResourceTarget(mc);
                } else if (bwAIPhase == BWAI_RETURN_BASE && bwAIBasePos != null) {
                    target = bwAIBasePos;
                }
                if (target != null && mc.player.getDistance(target) > 2.0) {
                    // Строим мост если нужно
                    if (bwAIPhase == BWAI_BRIDGE && bwAIBridgeStartReached && !(bwScaffoldEnabled && isBridgingZone(mc))) placeBlockBelowForAI(mc);
                } else {
                    target = null;
                }
            } else if (bwAIPhase == BWAI_DEFEND_BED && bwAIBedPos != null) {
                if (mc.player.getDistance(bwAIBedPos) > 4.0) {
                    target = bwAIBedPos;
                }
            }

            if (target != null) {
                double dx = target.getX() + 0.5 - mc.player.getPosX();
                double dz = target.getZ() + 0.5 - mc.player.getPosZ();
                mc.player.rotationYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
                event.setForward(1.0f);
                event.setSprintState(false);

                // Обход стенок: если впереди стена — стрейфим или прыгаем
                net.minecraft.util.Direction facing = net.minecraft.util.Direction.fromAngle(mc.player.rotationYaw);
                net.minecraft.util.math.BlockPos feet = mc.player.getPosition();
                net.minecraft.util.math.BlockPos front = feet.offset(facing);
                net.minecraft.util.math.BlockPos frontHead = front.up();
                boolean wallBottom = isSolid(mc, front);
                boolean wallTop = isSolid(mc, frontHead);
                if (wallBottom || wallTop) {
                    if (wallBottom && !wallTop && !isSolid(mc, front.up().up())) {
                        if (isGeneratorApproachPhase()) {
                            event.setForward(0f);
                            event.setJump(false);
                        } else {
                            event.setJump(true);
                        }
                        event.setSprintState(false);
                    } else {
                        net.minecraft.util.Direction left = facing.rotateYCCW();
                        net.minecraft.util.Direction right = facing.rotateY();
                        boolean rightFree = !isSolid(mc, feet.offset(right)) && !isSolid(mc, feet.offset(right).up());
                        boolean leftFree = !isSolid(mc, feet.offset(left)) && !isSolid(mc, feet.offset(left).up());
                        if (rightFree) event.setStrafe(-1.0f);
                        else if (leftFree) event.setStrafe(1.0f);
                        else { event.setForward(0f); }
                    }
                } else {
                    event.setStrafe(0.0f);
                }

                MoveUtils.fixMovement(event, mc.player.rotationYaw);
                return;
            }
        }

        if (followTarget == null) {
            isFollowing = false;
            return;
        }

        PlayerEntity target = findPlayer(followTarget);
        if (target == null || target.removed) {
            isFollowing = false;
            return;
        }

        double dx = target.getPosX() - mc.player.getPosX();
        double dz = target.getPosZ() - mc.player.getPosZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        isFollowing = true;

        if (dist > followDistance) {
            event.setForward(1.0f);
            event.setStrafe(0.0f);
            event.setSprintState(true);
            MoveUtils.fixMovement(event, mc.player.rotationYaw);

            boolean shouldJump = target.getPosY() > mc.player.getPosY() + 0.5
                    || mc.player.collidedHorizontally;
            event.setJump(shouldJump);
        }
    }

    @Subscribe
    public void onPacket(EventPacket e) {
        if (!Boolean.getBoolean("bot.mode")) return;
        if (mc.player == null) return;

        if (!e.isReceive()) return;
        IPacket<?> packet = e.getPacket();

        // Обнаружение тайтлов "СТАРТ"
        if (packet instanceof net.minecraft.network.play.server.STitlePacket titlePacket) {
            if (titlePacket.getType() == net.minecraft.network.play.server.STitlePacket.Type.TITLE
                    && titlePacket.getMessage() != null) {
                String titleText = titlePacket.getMessage().getString();
                if (titleText.contains("СТАРТ") || titleText.toLowerCase(java.util.Locale.ROOT).contains("start")) {
                    bwGameStarted = true;
                    bwGameStartTick = bwAITickCount;
                    System.out.println("[BotMode] TITLE START detected");
                }
            }
        }

        // AutoRegister
        if (autoRegisterEnabled && registerPassword.length() > 0 && packet instanceof SChatPacket) {
            SChatPacket chatPacket = (SChatPacket) packet;
            String text = chatPacket.getChatComponent().getString().toLowerCase(java.util.Locale.ROOT);
            handleAutoRegister(text);
        }

        // Перехват чата — ищем начало катки и имя карты
        if (packet instanceof SChatPacket) {
            String chatText = ((SChatPacket) packet).getChatComponent().getString();

            // Определяем что катка началась
            if (chatText.contains("уничтожить команды") || chatText.contains("Сломайте её")) {
                bwGameStarted = true;
                bwGameStartTick = bwAITickCount;
                System.out.println("[BotMode] GAME STARTED detected from chat");
            }

            // Ищем "Карта:" в чате
            if (chatText.contains("Карта:") || chatText.toLowerCase(java.util.Locale.ROOT).contains("map:")) {
                String extracted = chatText.replaceAll("(?i).*?(?:карта|map)[:\\s]+", "").replaceAll("[^\\x20-\\x7E\\u0400-\\u04FF]", "").trim();
                if (!extracted.isEmpty()) {
                    chatMapName = extracted;
                    System.out.println("[BotMode] Chat map: [" + chatMapName + "]");
                }
            }

            // Фолбэк: "Вы зашли на арену BW-XX"
            if (chatText.contains("арену BW-") || chatText.contains("arena BW-")) {
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("BW-(\\d+)").matcher(chatText);
                if (m.find()) {
                    String arena = "BW-" + m.group(1);
                    if (chatMapName.isEmpty()) chatMapName = arena;
                    System.out.println("[BotMode] Chat arena: [" + arena + "]");
                }
            }
        }

        // BWAutoEnterLeave: handle GUI after /leave (confirm dialog AND green glass in hub)
        if (bwEnterLeaveEnabled && packet instanceof SWindowItemsPacket) {
            if (bwEnterState != 2) {
                SWindowItemsPacket windowPacket = (SWindowItemsPacket) packet;
                java.util.List<ItemStack> items = windowPacket.getItemStacks();
                int totalItems = items.size();
                int containerSize = totalItems;
                if (containerSize > 36) containerSize = totalItems - 36;
                if (containerSize > 54) containerSize = totalItems;

                // After /leave: first GUI is the confirm dialog (slot 2 = "Подтвердить"/"Confirm").
                // Click it, then wait for the hub GUI with the green glass.
                if (bwEnterLeaveStopDelay > 0 && containerSize >= 3 && containerSize <= 9) {
                    ItemStack slot2 = items.get(2);
                    if (!slot2.isEmpty() && slot2.hasDisplayName()) {
                        String displayName = slot2.getDisplayName().getString();
                        if (displayName.contains("Подтвердить") || displayName.contains("Confirm")) {
                            mc.playerController.windowClick(windowPacket.getWindowId(), 2, 0, ClickType.PICKUP, mc.player);
                            System.out.println("[BotMode] BWAutoEnterLeave: clicked confirm at slot 2 (" + displayName + ")");
                            bwEnterLeaveStopDelay = 100;
                        }
                    }
                }

                // Hub GUI: find green glass and click it.
                System.out.println("[BotMode] BWAutoEnterLeave: scanning " + containerSize + " container slots (total items=" + totalItems + ")");
                for (int i = 0; i < containerSize && i < totalItems; i++) {
                    ItemStack stack = items.get(i);
                    if (stack.isEmpty()) continue;
                    boolean isGreenGlass = stack.getItem() == Items.LIME_STAINED_GLASS_PANE;
                    if (!isGreenGlass && stack.hasDisplayName()) {
                        String name = stack.getDisplayName().getString().toLowerCase(java.util.Locale.ROOT);
                        isGreenGlass = name.contains("играть") || name.contains("play") || name.contains("play again")
                                || name.contains("сыграть") || name.contains("вернуть");
                    }
                    if (isGreenGlass) {
                        mc.playerController.windowClick(windowPacket.getWindowId(), i, 0, ClickType.PICKUP, mc.player);
                        System.out.println("[BotMode] BWAutoEnterLeave: CLICKED green glass at slot " + i + " (" + stack.getDisplayName().getString() + ")");
                        break;
                    }
                }
            }
        }

        // BWEnter: detect bed item in the compass chest GUI
        if (bwEnterState == 2 && packet instanceof SWindowItemsPacket) {
            SWindowItemsPacket windowPacket = (SWindowItemsPacket) packet;
            java.util.List<ItemStack> items = windowPacket.getItemStacks();
            int totalItems = items.size();
            int containerSize = totalItems;
            if (containerSize > 36) containerSize = totalItems - 36;
            if (containerSize > 54) containerSize = totalItems;
            System.out.println("[BotMode] BWEnter: scanning " + containerSize + " container slots (total items=" + totalItems + ")");
            // Log all non-empty container items for debugging
            for (int i = 0; i < containerSize && i < items.size(); i++) {
                ItemStack stack = items.get(i);
                if (stack.isEmpty()) continue;
                System.out.println("[BotMode] BWEnter:   slot " + i + " = " + stack.getDisplayName().getString());
            }
            for (int i = 0; i < containerSize && i < items.size(); i++) {
                ItemStack stack = items.get(i);
                if (stack.isEmpty()) continue;
                // Match by actual item type (any bed) OR by lobby naming.
                boolean isBed = isBedItem(stack);
                if (!isBed && stack.hasDisplayName()) {
                    String name = stack.getDisplayName().getString().toLowerCase(java.util.Locale.ROOT);
                    isBed = name.contains("bedwars") || name.contains("bed wars") || name.contains("bed")
                            || name.contains("кровать") || name.contains("постель");
                }
                if (isBed) {
                    mc.playerController.windowClick(windowPacket.getWindowId(), i, 0, ClickType.PICKUP, mc.player);
                    bwEnterState = 0;
                    bwEnterTimer = 0;
                    bwEntered = true;
                    e.cancel();
                    System.out.println("[BotMode] BWEnter: CLICKED bed at slot " + i + " (" + stack.getDisplayName().getString() + ")");
                    break;
                }
            }
            if (bwEnterState == 2) {
                // If we scanned but didn't find/click a bed, the chest is open but no bed found.
                // Treat this as "chest opened" so we don't retry compass endlessly.
                System.out.println("[BotMode] BWEnter: chest opened but no bed found in container");
                bwEnterState = 0;
                bwEnterTimer = 0;
            }
        }
    }

    private void handleAutoRegister(String text) {
        if (text.contains("/register") || containsAny(text, "зарегистр", "регистрац", "придумайте пароль", "повторите пароль", "repeat password", "create password")) {
            if (!containsAny(text, "already registered", "уже зарегистр", "already logged")) {
                mc.player.sendChatMessage("/register " + registerPassword + " " + registerPassword);
                System.out.println("[BotMode] AutoRegister: /register " + registerPassword);
            }
        } else if (text.contains("/login") || text.contains("/l ") || containsAny(text, "авториз", "войдите", "войти", "залогин", "введите пароль")) {
            if (!containsAny(text, "already registered", "уже зарегистр")) {
                mc.player.sendChatMessage("/login " + registerPassword);
                System.out.println("[BotMode] AutoRegister: /login " + registerPassword);
            }
        }
    }

    private static boolean containsAny(String text, String... words) {
        for (String word : words) {
            if (text.contains(word)) return true;
        }
        return false;
    }

    @Subscribe
    public void onMotion(EventMotion event) {
        if (!Boolean.getBoolean("bot.mode")) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.world == null) return;

        if (followTarget == null) return;
        PlayerEntity target = findPlayer(followTarget);
        if (target == null || target.removed) return;

        double dx = target.getPosX() - mc.player.getPosX();
        double dz = target.getPosZ() - mc.player.getPosZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        double targetEyeY = target instanceof LivingEntity
                ? target.getPosY() + ((LivingEntity) target).getEyeHeight()
                : target.getPosY() + 0.5;
        double dy = targetEyeY - (mc.player.getPosY() + mc.player.getEyeHeight());

        float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, Math.max(dist, 0.01)));

        event.setYaw(targetYaw);
        event.setPitch(MathHelper.clamp(targetPitch, -90.0F, 90.0F));

        mc.player.rotationYaw = targetYaw;
        mc.player.rotationPitch = MathHelper.clamp(targetPitch, -90.0F, 90.0F);
        mc.player.rotationYawHead = targetYaw;
        mc.player.prevRotationYaw = targetYaw;
        mc.player.prevRotationPitch = MathHelper.clamp(targetPitch, -90.0F, 90.0F);
    }

    private static PlayerEntity findPlayer(String name) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.world == null) return null;
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p.getGameProfile().getName().equals(name)) return p;
        }
        return null;
    }

    private static void setFps(int fps) {
        Minecraft mc = Minecraft.getInstance();
        mc.getMainWindow().setFramerateLimit(fps);
    }

    private static void apply() {
        try {
            Minecraft mc = Minecraft.getInstance();
            setFps(30);
            mc.getSoundHandler().stop();

            if (!applied) {
                applied = true;
                System.out.println("[BotMode] Applied: FPS=30, sound=stopped");
            }
        } catch (Exception ex) {
            System.err.println("[BotMode] Failed to apply: " + ex.getMessage());
        }
    }
}
