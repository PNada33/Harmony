package xd.harm.modules.impl.movement;

import com.google.common.eventbus.Subscribe;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.FallingBlock;
import net.minecraft.block.FlowingFluidBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CAnimateHandPacket;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.network.play.client.CPlayerPacket;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;

import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import xd.harm.Harmony;
import xd.harm.events.input.EventInput;
import xd.harm.events.movement.EventMotion;
import xd.harm.events.network.EventPacket;
import xd.harm.events.render.EventDisplay;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.CategorySetting;
import xd.harm.modules.settings.impl.ModeSetting;
import xd.harm.modules.settings.impl.SliderSetting;
import xd.harm.utils.client.TimerUtility;
import xd.harm.utils.player.MoveUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

@ModuleRegister(name = "ScaffoldLiquidTwo", category = Category.Movement,
        desc = "Scaffold из LiquidBounce (v2)")
public class ScaffoldLiquidTwo extends Module {

    private static final List<Block> DISALLOWED_BLOCKS = Arrays.asList(
            Blocks.TNT, Blocks.COBWEB, Blocks.NETHER_PORTAL
    );

    private final CategorySetting catBasic = new CategorySetting("ОСНОВНЫЕ");
    private final CategorySetting catTechnique = new CategorySetting("ТЕХНИКА");
    private final CategorySetting catRotation = new CategorySetting("РОТАЦИИ");
    private final CategorySetting catMovement = new CategorySetting("ДВИЖЕНИЕ");
    private final CategorySetting catTower = new CategorySetting("ТАУЭР");
    private final CategorySetting catAdvanced = new CategorySetting("ДОПОЛНИТЕЛЬНО");
    private final CategorySetting catRender = new CategorySetting("РЕНДЕР");

    // ======================== ОСНОВНЫЕ ========================

    private final SliderSetting delay = new SliderSetting("Задержка", 0f, 0f, 40f, 1f);
    private final SliderSetting minDist = new SliderSetting("Мин. дистанция", 0f, 0f, 0.25f, 0.01f);
    private final SliderSetting timer = new SliderSetting("Таймер", 1f, 0.01f, 10f, 0.01f);
    private final BooleanSetting placeDelay = new BooleanSetting("Задержка размещения", true);
    private final BooleanSetting sprint = new BooleanSetting("Спринт", true);
    private final BooleanSetting swing = new BooleanSetting("Размах", true);
    private final BooleanSetting down = new BooleanSetting("Вниз", true);
    private final BooleanSetting sameY = new BooleanSetting("Одна Y", false);

    // ======================== ТЕХНИКА ========================

    private final ModeSetting technique = new ModeSetting("Техника", "Normal",
            "Normal", "Rewinside", "Expand", "Telly", "GodBridge", "Breezily");

    // --- Normal ---
    private final ModeSetting aimMode = new ModeSetting("Режим прицела", "Stabilized",
            "Center", "Random", "Stabilized", "NearestRotation", "ReverseYaw", "DiagonalYaw", "AngleYaw", "EdgePoint")
            .setVisible(() -> technique.is("Normal"));
    private final BooleanSetting requiresSight = new BooleanSetting("Требуется обзор", false)
            .setVisible(() -> technique.is("Normal"));
    private final BooleanSetting eagle = new BooleanSetting("Орёл", true)
            .setVisible(() -> technique.is("Normal"));
    private final ModeSetting eagleMode = new ModeSetting("Режим орла", "Normal", "Normal", "Sneak")
            .setVisible(() -> technique.is("Normal") && eagle.get());
    private final SliderSetting eagleEdgeDistance = new SliderSetting("Дистанция края", 0.5f, 0.1f, 1f, 0.05f)
            .setVisible(() -> technique.is("Normal") && eagle.get());
    private final BooleanSetting eagleSprint = new BooleanSetting("Орёл спринт", false)
            .setVisible(() -> technique.is("Normal") && eagle.get());

    // --- Expand ---
    private final BooleanSetting omniDirectionalExpand = new BooleanSetting("Omni-расширение", false)
            .setVisible(() -> technique.is("Expand"));
    private final SliderSetting expandLength = new SliderSetting("Длина расширения", 1f, 1f, 6f, 1f)
            .setVisible(() -> technique.is("Expand"));

    // --- Telly ---
    private final ModeSetting tellyRotationMode = new ModeSetting("Режим ротации Telly", "Stabilized",
            "Center", "Random", "Stabilized", "AngleYaw")
            .setVisible(() -> technique.is("Telly"));
    private final BooleanSetting startHorizontally = new BooleanSetting("Начать горизонтально", false)
            .setVisible(() -> technique.is("Telly"));
    private final SliderSetting horizontalPlacementsMin = new SliderSetting("Мин. гор. размещений", 1f, 1f, 10f, 1f)
            .setVisible(() -> technique.is("Telly"));
    private final SliderSetting horizontalPlacementsMax = new SliderSetting("Макс. гор. размещений", 5f, 1f, 10f, 1f)
            .setVisible(() -> technique.is("Telly"));
    private final SliderSetting verticalPlacementsMin = new SliderSetting("Мин. верт. размещений", 1f, 1f, 10f, 1f)
            .setVisible(() -> technique.is("Telly"));
    private final SliderSetting verticalPlacementsMax = new SliderSetting("Макс. верт. размещений", 3f, 1f, 10f, 1f)
            .setVisible(() -> technique.is("Telly"));
    private final SliderSetting jumpTicksMin = new SliderSetting("Мин. тиков прыжка", 1f, 1f, 20f, 1f)
            .setVisible(() -> technique.is("Telly"));
    private final SliderSetting jumpTicksMax = new SliderSetting("Макс. тиков прыжка", 3f, 1f, 20f, 1f)
            .setVisible(() -> technique.is("Telly"));

    // --- GodBridge ---
    private final BooleanSetting waitForRots = new BooleanSetting("Ждать ротации", true)
            .setVisible(() -> technique.is("GodBridge"));
    private final BooleanSetting useOptimizedPitch = new BooleanSetting("Оптим. питч", true)
            .setVisible(() -> technique.is("GodBridge"));
    private final SliderSetting customGodPitch = new SliderSetting("GodBridge питч", 75f, 60f, 90f, 0.5f)
            .setVisible(() -> technique.is("GodBridge") && !useOptimizedPitch.get());
    private final BooleanSetting jumpAutomatically = new BooleanSetting("Авто-прыжок", true)
            .setVisible(() -> technique.is("GodBridge"));
    private final SliderSetting blocksToJumpMin = new SliderSetting("Мин. блоков до прыжка", 1f, 1f, 10f, 1f)
            .setVisible(() -> technique.is("GodBridge") && !jumpAutomatically.get());
    private final SliderSetting blocksToJumpMax = new SliderSetting("Макс. блоков до прыжка", 3f, 1f, 10f, 1f)
            .setVisible(() -> technique.is("GodBridge") && !jumpAutomatically.get());

    // --- Breezily ---
    private final SliderSetting breezilyPitch = new SliderSetting("Breezily питч", 80f, 60f, 90f, 0.5f)
            .setVisible(() -> technique.is("Breezily"));

    // ======================== РОТАЦИИ (общие) ========================

    private final ModeSetting rotationMode = new ModeSetting("Режим ротации", "Normal",
            "Normal", "Stabilized", "ReverseYaw");
    private final BooleanSetting keepRotation = new BooleanSetting("Удерж. ротацию", false);
    private final SliderSetting resetTicks = new SliderSetting("Тики сброса", 15f, 1f, 40f, 1f);
    private final BooleanSetting legitimize = new BooleanSetting("Легитимно", false);
    private final BooleanSetting applyServerSide = new BooleanSetting("Серверная ротация", false);

    // ======================== ДВИЖЕНИЕ ========================

    private final BooleanSetting strafe = new BooleanSetting("Стрейф", true);
    private final BooleanSetting jumpStrafe = new BooleanSetting("Прыжок-стрейф", true);
    private final SliderSetting jumpStraightStrafe = new SliderSetting("Стрейф прямо", 1f, 0f, 1f, 0.01f)
            .setVisible(() -> jumpStrafe.get());
    private final SliderSetting jumpDiagonalStrafe = new SliderSetting("Стрейф диагональ", 0.8f, 0f, 1f, 0.01f)
            .setVisible(() -> jumpStrafe.get());
    private final SliderSetting speedModifier = new SliderSetting("Модиф. скорости", 1f, 0f, 2f, 0.01f);
    private final BooleanSetting speedLimiter = new BooleanSetting("Лимитер скорости", false);
    private final SliderSetting speedLimit = new SliderSetting("Лимит скорости", 0.5f, 0f, 2f, 0.01f)
            .setVisible(() -> speedLimiter.get());
    private final BooleanSetting slow = new BooleanSetting("Замедление", false);
    private final BooleanSetting slowGround = new BooleanSetting("Замедл. на земле", false)
            .setVisible(() -> slow.get());
    private final SliderSetting slowSpeed = new SliderSetting("Скорость замедл.", 0.6f, 0f, 1f, 0.01f)
            .setVisible(() -> slow.get());

    // ======================== ZITTER ========================

    private final ModeSetting zitterMode = new ModeSetting("Zitter режим", "Off",
            "Off", "Smooth", "Teleport");
    private final SliderSetting zitterSpeed = new SliderSetting("Zitter скорость", 1f, 0.1f, 5f, 0.1f)
            .setVisible(() -> zitterMode.is("Teleport"));
    private final SliderSetting zitterStrength = new SliderSetting("Zitter сила", 0.3f, 0f, 1f, 0.01f)
            .setVisible(() -> zitterMode.is("Teleport"));
    private final SliderSetting zitterTicks = new SliderSetting("Zitter тики", 1f, 1f, 10f, 1f)
            .setVisible(() -> zitterMode.is("Smooth"));
    private final BooleanSetting useSneakMidAir = new BooleanSetting("Красться в воздухе", false)
            .setVisible(() -> zitterMode.is("Smooth"));

    // ======================== ТАУЭР ========================

    private final ModeSetting towerMode = new ModeSetting("Тауэр", "None",
            "None", "Jump", "MotionJump", "Motion", "ConstantMotion",
            "MotionTP", "Packet", "Teleport", "AAC3.3.9", "AAC3.6.4",
            "Vulcan2.9.0", "Pulldown");
    private final BooleanSetting stopWhenBlockAbove = new BooleanSetting("Стоп при блоке сверху", false)
            .setVisible(() -> !towerMode.is("None"));
    private final BooleanSetting towerOnJump = new BooleanSetting("Тауэр на прыжок", true)
            .setVisible(() -> !towerMode.is("None"));
    private final BooleanSetting towerNotOnMove = new BooleanSetting("Тауэр не при движении", false)
            .setVisible(() -> !towerMode.is("None"));
    private final SliderSetting jumpMotion = new SliderSetting("Движение прыжка", 0.42f, 0.36f, 0.79f, 0.01f)
            .setVisible(() -> towerMode.is("MotionJump"));
    private final SliderSetting jumpDelay = new SliderSetting("Задержка прыжка", 0f, 0f, 20f, 1f)
            .setVisible(() -> towerMode.is("MotionJump") || towerMode.is("Jump"));
    private final SliderSetting constantMotion = new SliderSetting("Пост. движение", 0.42f, 0.1f, 1f, 0.01f)
            .setVisible(() -> towerMode.is("ConstantMotion"));
    private final SliderSetting constantMotionJumpGround = new SliderSetting("Прыжок ConstantMotion", 0.79f, 0.76f, 1f, 0.01f)
            .setVisible(() -> towerMode.is("ConstantMotion"));
    private final BooleanSetting jumpPacket = new BooleanSetting("Пакет прыжка", true)
            .setVisible(() -> towerMode.is("ConstantMotion"));
    private final SliderSetting triggerMotion = new SliderSetting("TriggerMotion", 0.1f, 0f, 0.2f, 0.01f)
            .setVisible(() -> towerMode.is("Pulldown"));
    private final SliderSetting dragMotion = new SliderSetting("DragMotion", 1f, 0.1f, 1f, 0.01f)
            .setVisible(() -> towerMode.is("Pulldown"));
    private final SliderSetting teleportHeight = new SliderSetting("Высота телепорта", 1.15f, 0.1f, 5f, 0.05f)
            .setVisible(() -> towerMode.is("Teleport"));
    private final SliderSetting teleportDelaySlots = new SliderSetting("Задержка телепорта", 0f, 0f, 20f, 1f)
            .setVisible(() -> towerMode.is("Teleport"));
    private final BooleanSetting teleportGround = new BooleanSetting("Телепорт на земле", true)
            .setVisible(() -> towerMode.is("Teleport"));
    private final BooleanSetting teleportNoMotion = new BooleanSetting("Телепорт без движения", false)
            .setVisible(() -> towerMode.is("Teleport"));

    // ======================== ДОПОЛНИТЕЛЬНО ========================

    private final BooleanSetting autoBlockToggle = new BooleanSetting("Авто-блок", false);
    private final ModeSetting autoBlockMode = new ModeSetting("Режим авто-блока", "Spoof", "Pick", "Spoof", "Switch")
            .setVisible(() -> autoBlockToggle.get());
    private final BooleanSetting sortByHighestAmount = new BooleanSetting("Сорт. по кол-ву", false)
            .setVisible(() -> autoBlockToggle.get());
    private final BooleanSetting earlySwitch = new BooleanSetting("Ранняя смена", false)
            .setVisible(() -> autoBlockToggle.get() && !sortByHighestAmount.get());
    private final SliderSetting amountBeforeSwitch = new SliderSetting("Кол-во до смены", 3f, 1f, 10f, 1f)
            .setVisible(() -> earlySwitch.get() && !sortByHighestAmount.get());
    private final BooleanSetting extraClicks = new BooleanSetting("Доп. клики", true);
    private final BooleanSetting simulateDoubleClicking = new BooleanSetting("Симуляция двойного клика", false)
            .setVisible(() -> extraClicks.get());
    private final SliderSetting extraClickCPSMin = new SliderSetting("Мин. CPS доп. кликов", 3f, 0f, 50f, 1f)
            .setVisible(() -> extraClicks.get());
    private final SliderSetting extraClickCPSMax = new SliderSetting("Макс. CPS доп. кликов", 7f, 0f, 50f, 1f)
            .setVisible(() -> extraClicks.get());
    private final ModeSetting placementAttempt = new ModeSetting("Попыток размещения", "Fail", "Fail", "Independent")
            .setVisible(() -> extraClicks.get());
    private final BooleanSetting safeWalk = new BooleanSetting("Безопасная ходьба", true);
    private final BooleanSetting airSafe = new BooleanSetting("В воздухе", true)
            .setVisible(() -> safeWalk.get());
    private final BooleanSetting autoF5 = new BooleanSetting("Авто-F5", false);

    // Eagle (общие настройки для всех техник кроме GodBridge)
    private final ModeSetting eagleOption = new ModeSetting("Eagle", "Off",
            "Off", "Normal", "Silent", "Reverse")
            .setVisible(() -> !technique.is("GodBridge"));
    private final ModeSetting eagleModeOption = new ModeSetting("Eagle режим", "Normal",
            "Normal", "Strict", "OnGround")
            .setVisible(() -> !technique.is("GodBridge") && !eagleOption.is("Off"));
    private final BooleanSetting adjustedSneakSpeed = new BooleanSetting("Рег. скорость красться", false)
            .setVisible(() -> !technique.is("GodBridge") && eagleOption.is("Silent"));
    private final SliderSetting eagleSpeed = new SliderSetting("Eagle скорость", 0.3f, 0.01f, 1f, 0.01f)
            .setVisible(() -> !technique.is("GodBridge") && !eagleOption.is("Off"));
    private final SliderSetting blocksToEagleMin = new SliderSetting("Мин. блоков до Eagle", 1f, 1f, 10f, 1f)
            .setVisible(() -> !technique.is("GodBridge") && !eagleOption.is("Off"));
    private final SliderSetting blocksToEagleMax = new SliderSetting("Макс. блоков до Eagle", 3f, 1f, 10f, 1f)
            .setVisible(() -> !technique.is("GodBridge") && !eagleOption.is("Off"));
    private final SliderSetting edgeDistance = new SliderSetting("Дист. края", 0.5f, 0.1f, 1f, 0.05f)
            .setVisible(() -> !technique.is("GodBridge") && !eagleOption.is("Off"));
    private final BooleanSetting useMaxSneakTime = new BooleanSetting("Макс. время красться", false)
            .setVisible(() -> !technique.is("GodBridge") && !eagleOption.is("Off"));
    private final SliderSetting maxSneakTicksMin = new SliderSetting("Мин. тиков красться", 1f, 1f, 40f, 1f)
            .setVisible(() -> useMaxSneakTime.get());
    private final SliderSetting maxSneakTicksMax = new SliderSetting("Макс. тиков красться", 10f, 1f, 40f, 1f)
            .setVisible(() -> useMaxSneakTime.get());
    private final BooleanSetting blockSneakingAgainUntilOnGround = new BooleanSetting("Блок. повтор до земли", false)
            .setVisible(() -> useMaxSneakTime.get() && !eagleModeOption.is("OnGround"));

    private final BooleanSetting allowClutching = new BooleanSetting("Разрешить клатч", true)
            .setVisible(() -> !technique.is("Telly") && !technique.is("Expand"));
    private final SliderSetting horizontalClutchBlocks = new SliderSetting("Гор. блоки клатча", 2f, 1f, 10f, 1f)
            .setVisible(() -> allowClutching.get() && !technique.is("Telly") && !technique.is("Expand"));
    private final SliderSetting verticalClutchBlocks = new SliderSetting("Верт. блоки клатча", 1f, 1f, 10f, 1f)
            .setVisible(() -> allowClutching.get() && !technique.is("Telly") && !technique.is("Expand"));
    private final BooleanSetting blockSafe = new BooleanSetting("Блок безопасно", true)
            .setVisible(() -> !technique.is("GodBridge"));
    private final BooleanSetting trackCPS = new BooleanSetting("Отслеживать CPS", false);
    private final BooleanSetting jumpOnUserInput = new BooleanSetting("Прыжок по вводу", false)
            .setVisible(() -> sameY.get() && !technique.is("GodBridge"));

    // ======================== РЕНДЕР ========================

    private final BooleanSetting mark = new BooleanSetting("Метка", true);
    private final BooleanSetting blockESP = new BooleanSetting("ESP блоков", true);

    // ======================== СОСТОЯНИЕ ========================

    private Random random = new Random();
    private final TimerUtility delayTimer = new TimerUtility();

    private BlockPos blockPos;
    private Direction facing;
    private Vector2f rotationVector;
    private float scaffoldYaw;
    private float scaffoldPitch;

    private int launchY;
    private int ticks;
    private int placedBlocksWithoutEagle;
    private boolean eagleSneaking;
    private boolean requestedStopSneak;
    private int blocksPlacedUntilJump;
    private int blocksToJump;
    private int horizontalPlacements;
    private int verticalPlacements;
    private int blocksUntilAxisChange;
    private int jumpTicks;
    private int ticksUntilJump;
    private boolean isOnRightSide;
    private boolean zitterDirection;
    private int extraClicksCounter;
    private long lastExtraClick;
    private int lastHotbarSlot = -1;

    // ======================== СТАТИКА ========================

    private static final double[] OFFSETS = {0.301, 0.0, -0.301};

    // ======================== КОНСТРУКТОР ========================

    public ScaffoldLiquidTwo() {
        addSettings(
                catBasic, delay, minDist, timer, placeDelay, sprint, swing, down, sameY,

                catTechnique, technique,
                // Normal
                aimMode, requiresSight,
                eagle, eagleMode, eagleEdgeDistance, eagleSprint,
                // Expand
                omniDirectionalExpand, expandLength,
                // Telly
                tellyRotationMode, startHorizontally,
                horizontalPlacementsMin, horizontalPlacementsMax,
                verticalPlacementsMin, verticalPlacementsMax,
                jumpTicksMin, jumpTicksMax,
                // GodBridge
                waitForRots, useOptimizedPitch, customGodPitch,
                jumpAutomatically, blocksToJumpMin, blocksToJumpMax,
                // Breezily
                breezilyPitch,

                catRotation, rotationMode, keepRotation, resetTicks, legitimize, applyServerSide,

                catMovement, strafe, jumpStrafe, jumpStraightStrafe, jumpDiagonalStrafe,
                speedModifier, speedLimiter, speedLimit,
                slow, slowGround, slowSpeed,

                // Zitter
                zitterMode, zitterSpeed, zitterStrength, zitterTicks, useSneakMidAir,

                catTower, towerMode, stopWhenBlockAbove, towerOnJump, towerNotOnMove,
                jumpMotion, jumpDelay, constantMotion, constantMotionJumpGround, jumpPacket,
                triggerMotion, dragMotion, teleportHeight, teleportDelaySlots,
                teleportGround, teleportNoMotion,

                catAdvanced,
                autoBlockToggle, autoBlockMode, sortByHighestAmount, earlySwitch, amountBeforeSwitch,
                extraClicks, simulateDoubleClicking, extraClickCPSMin, extraClickCPSMax, placementAttempt,
                safeWalk, airSafe, autoF5,
                eagleOption, eagleModeOption, adjustedSneakSpeed, eagleSpeed,
                blocksToEagleMin, blocksToEagleMax, edgeDistance,
                useMaxSneakTime, maxSneakTicksMin, maxSneakTicksMax,
                blockSneakingAgainUntilOnGround,
                allowClutching, horizontalClutchBlocks, verticalClutchBlocks,
                blockSafe, trackCPS, jumpOnUserInput,

                catRender, mark, blockESP
        );
    }

    // ======================== ON ENABLE / DISABLE ========================

    @Override
    public boolean onEnable() {
        if (!super.onEnable()) return false;
        delayTimer.reset();
        ticks = 0;
        scaffoldYaw = mc.player.rotationYaw;
        scaffoldPitch = mc.player.rotationPitch;
        launchY = (int) mc.player.getPosY();
        isOnRightSide = false;
        zitterDirection = false;
        blocksPlacedUntilJump = 0;
        blocksToJump = randomInt(blocksToJumpMin.get().intValue(), blocksToJumpMax.get().intValue());
        horizontalPlacements = randomInt(horizontalPlacementsMin.get().intValue(), horizontalPlacementsMax.get().intValue());
        verticalPlacements = randomInt(verticalPlacementsMin.get().intValue(), verticalPlacementsMax.get().intValue());
        blocksUntilAxisChange = 0;
        jumpTicks = randomInt(jumpTicksMin.get().intValue(), jumpTicksMax.get().intValue());
        ticksUntilJump = 0;
        lastHotbarSlot = mc.player.inventory.currentItem;
        return true;
    }

    @Override
    public boolean onDisable() {
        if (!super.onDisable()) return false;
        return true;
    }

    // ======================== EVENT: MOTION (поиск блока + ротация) ========================

    @Subscribe
    public void onMotion(EventMotion e) {
        if (mc.player == null || mc.world == null) return;

        ticks++;
        if (ticks == 1) launchY = (int) mc.player.getPosY();

        // Поиск блока и подбор слота
        findBestBlockSlot();
        findBlock();

        if (blockPos != null && facing != null) {
            // Расчёт ротации на основе блока
            calculateRotationFromBlock();
        }

        // Применение ротации
        if (rotationVector != null) {
            e.setYaw(rotationVector.x);
            e.setPitch(rotationVector.y);
        } else {
            e.setYaw(mc.player.rotationYaw);
            e.setPitch(mc.player.rotationPitch);
        }

        // Tower
        handleTower();
    }

    // ======================== EVENT: UPDATE (размещение блока) ========================

    @Subscribe
    public void onUpdate(EventUpdate e) {
        if (mc.player == null || mc.world == null) return;

        if (blockPos == null) {
            if (placeDelay.get()) delayTimer.reset();
            return;
        }

        int placeDelayMs = delay.get().intValue() * 50;
        if (!delayTimer.hasReached(placeDelayMs)) return;

        placeBlock();
    }

    // ======================== ROTATION ========================

    private void calculateRotationFromBlock() {
        if (blockPos == null || facing == null) return;

        String tech = technique.get();

        switch (tech) {
            case "GodBridge":
                handleGodBridgeRotation();
                return;
            case "Breezily":
                handleBreezilyRotation();
                return;
            case "Rewinside":
                handleRewinsideRotation();
                return;
        }

        // Normal / Expand / Telly — рассчитываем через hitVec
        Vector3d hitVec = getHitVec(blockPos, facing);
        Vector3d eyes = mc.player.getEyePosition(1.0f);
        Vector3d diff = hitVec.subtract(eyes);

        float yaw = (float) Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90f;
        float pitch = (float) -Math.toDegrees(Math.atan2(diff.y, Math.sqrt(diff.x * diff.x + diff.z * diff.z)));

        if ("Normal".equals(tech)) {
            String mode = aimMode.get();
            switch (mode) {
                case "Random":
                    yaw += (random.nextFloat() - 0.5f) * 6f;
                    pitch += (random.nextFloat() - 0.5f) * 6f;
                    break;
                case "Stabilized": {
                    float yawDiff = MathHelper.wrapDegrees(yaw - scaffoldYaw);
                    scaffoldYaw += yawDiff * 0.5f;
                    scaffoldPitch += (pitch - scaffoldPitch) * 0.5f;
                    yaw = scaffoldYaw;
                    pitch = scaffoldPitch;
                    break;
                }
                case "NearestRotation": {
                    float roundedYaw = (float) Math.rint(yaw / 45f) * 45f;
                    if (Math.abs(yaw - roundedYaw) < 22.5f) yaw = roundedYaw;
                    break;
                }
                case "ReverseYaw": {
                    float snappedYaw = (float) Math.rint(yaw / 45f) * 45f;
                    if (snappedYaw % 90f != 0f) {
                        yaw = snappedYaw;
                    } else if (!isOnCenter(blockPos)) {
                        yaw = (float) Math.rint((yaw + 180f) / 45f) * 45f;
                    }
                    break;
                }
                case "AngleYaw":
                    yaw = (float) Math.rint(yaw / 45f) * 45f;
                    pitch = 85f;
                    break;
            }
        } else if ("Telly".equals(tech)) {
            String tellyRot = tellyRotationMode.get();
            if ("Stabilized".equals(tellyRot)) {
                scaffoldYaw += MathHelper.wrapDegrees(yaw - scaffoldYaw) * 0.5f;
                scaffoldPitch += (85f - scaffoldPitch) * 0.5f;
                yaw = scaffoldYaw;
                pitch = scaffoldPitch;
            } else if ("AngleYaw".equals(tellyRot)) {
                yaw = (float) Math.rint(yaw / 45f) * 45f;
                pitch = 85f;
            }
        } else if ("Expand".equals(tech)) {
            pitch = 85f;
        }

        scaffoldYaw = yaw;
        scaffoldPitch = pitch;
        rotationVector = new Vector2f(scaffoldYaw, scaffoldPitch);
    }

    private void handleRewinsideRotation() {
        if (blockPos == null) return;

        double rad = Math.toRadians(mc.player.rotationYaw);
        double x = -Math.sin(rad) * 0.5;
        double z = Math.cos(rad) * 0.5;

        BlockPos ahead = new BlockPos(
                mc.player.getPosX() + x,
                mc.player.getPosY() - 1,
                mc.player.getPosZ() + z
        );

        if (!isAir(ahead)) {
            scaffoldYaw = mc.player.rotationYaw;
            scaffoldPitch = 85f;
        } else {
            scaffoldYaw = mc.player.rotationYaw;
            scaffoldPitch = mc.player.rotationPitch;
        }
        rotationVector = new Vector2f(scaffoldYaw, scaffoldPitch);
    }

    private void handleExpandRotation() {
        if (blockPos == null) return;
        float yaw = mc.player.rotationYaw;
        float pitch = 85f;
        scaffoldYaw = yaw;
        scaffoldPitch = pitch;
        rotationVector = new Vector2f(scaffoldYaw, scaffoldPitch);
    }

    private void handleTellyRotation() {
        if (blockPos == null) return;
        float yaw = mc.player.rotationYaw;
        float pitch = 85f;

        String tellyRot = tellyRotationMode.get();
        if ("Stabilized".equals(tellyRot)) {
            scaffoldYaw += MathHelper.wrapDegrees(yaw - scaffoldYaw) * 0.5f;
            scaffoldPitch += (pitch - scaffoldPitch) * 0.5f;
        } else if ("AngleYaw".equals(tellyRot)) {
            scaffoldYaw = (float) Math.rint(yaw / 45f) * 45f;
            scaffoldPitch = 85f;
        } else {
            scaffoldYaw = yaw;
            scaffoldPitch = pitch;
        }
        rotationVector = new Vector2f(scaffoldYaw, scaffoldPitch);
    }

    private void handleGodBridgeRotation() {
        float movingYaw = 0;
        if (MoveUtils.isMoving()) {
            movingYaw = (float) Math.toDegrees(
                    MoveUtils.direction(mc.player.rotationYaw - 180f,
                            mc.player.movementInput.moveForward,
                            mc.player.movementInput.moveStrafe));

            float snappedYaw = (float) Math.rint(movingYaw / 45f) * 45f;
            boolean straight = snappedYaw % 90f == 0f;

            if (straight) {
                if (mc.player.isOnGround()) {
                    double rad = Math.toRadians(snappedYaw);
                    boolean right = Math.floor(mc.player.getPosX()
                            + Math.cos(rad) * 0.5) == Math.floor(mc.player.getPosX())
                            && Math.floor(mc.player.getPosZ()
                            + Math.sin(rad) * 0.5) == Math.floor(mc.player.getPosZ());
                    if (!right) isOnRightSide = !isOnRightSide;

                    Direction dir = Direction.fromAngle(snappedYaw);
                    BlockPos ahead = new BlockPos(
                            mc.player.getPosX() + dir.getXOffset() * 0.6,
                            mc.player.getPosY(),
                            mc.player.getPosZ() + dir.getZOffset() * 0.6);
                    BlockPos belowPlayer = new BlockPos(mc.player.getPosX(), mc.player.getPosY(), mc.player.getPosZ()).down();
                    boolean leaningOff = mc.world.getBlockState(belowPlayer)
                            .getBlock() instanceof AirBlock;
                    boolean nextAir = mc.world.getBlockState(ahead.down())
                            .getBlock() instanceof AirBlock;
                    if (leaningOff && nextAir) {
                        isOnRightSide = !isOnRightSide;
                    }
                }
                scaffoldYaw = snappedYaw + (isOnRightSide ? 45 : -45);
            } else {
                scaffoldYaw = snappedYaw;
            }
            scaffoldPitch = useOptimizedPitch.get() ? 73.5f : customGodPitch.get();
        }
        rotationVector = new Vector2f(scaffoldYaw, scaffoldPitch);
    }

    private void handleBreezilyRotation() {
        float yaw = (float) Math.toDegrees(
                MoveUtils.direction(mc.player.rotationYaw - 180f,
                        mc.player.movementInput.moveForward,
                        mc.player.movementInput.moveStrafe));
        scaffoldYaw = updateRotationFloat(scaffoldYaw, yaw, 30);
        scaffoldPitch = breezilyPitch.get();
        rotationVector = new Vector2f(scaffoldYaw, scaffoldPitch);
    }

    private void sendRotationPacket() {
        mc.player.connection.sendPacket(
                new CPlayerPacket.RotationPacket(
                        rotationVector.x, rotationVector.y, mc.player.isOnGround()));
    }

    // ======================== TOWER ========================

    private boolean isTowering = false;

    private void handleTower() {
        String mode = towerMode.get();
        if ("None".equals(mode)) return;
        if (towerNotOnMove.get() && MoveUtils.isMoving()) return;
        if (!towerOnJump.get() || mc.gameSettings.keyBindJump.isKeyDown()) {
            isTowering = true;
            doTowerMove(mode);
            if (stopWhenBlockAbove.get()) {
                BlockPos above = new BlockPos(mc.player.getPosX(), mc.player.getPosY(), mc.player.getPosZ()).up(2);
                if (!(mc.world.getBlockState(above).getBlock() instanceof AirBlock)) {
                    return;
                }
            }
        } else {
            isTowering = false;
        }
    }

    private void doTowerMove(String mode) {
        switch (mode) {
            case "Jump":
                if (mc.player.isOnGround()) {
                    mc.player.jump();
                }
                break;
            case "MotionJump":
                if (mc.player.isOnGround()) {
                    mc.player.jump();
                    mc.player.setMotion(mc.player.getMotion().add(0, jumpMotion.get(), 0));
                }
                break;
            case "Motion":
                if (mc.player.isOnGround()) {
                    mc.player.jump();
                } else if (mc.player.getMotion().y < 0.1) {
                    mc.player.setMotion(mc.player.getMotion().add(0, -0.3, 0));
                }
                break;
            case "ConstantMotion":
                if (mc.player.isOnGround()) {
                    mc.player.setMotion(mc.player.getMotion().add(0, constantMotion.get(), 0));
                }
                if (mc.player.getPosY() > launchY + constantMotionJumpGround.get()) {
                    mc.player.setPosition(mc.player.getPosX(), (int) mc.player.getPosY(), mc.player.getPosZ());
                    mc.player.setMotion(mc.player.getMotion().add(0, constantMotion.get(), 0));
                }
                break;
            case "MotionTP":
                if (mc.player.isOnGround()) {
                    mc.player.jump();
                } else if (mc.player.getMotion().y < 0.23) {
                    mc.player.setPosition(mc.player.getPosX(), (int) mc.player.getPosY(), mc.player.getPosZ());
                }
                break;
            case "Packet":
                if (mc.player.isOnGround()) {
                    mc.player.connection.sendPacket(new CPlayerPacket.PositionRotationPacket(
                            mc.player.getPosX(), mc.player.getPosY() + 0.42, mc.player.getPosZ(),
                            mc.player.rotationYaw, mc.player.rotationPitch, false));
                    mc.player.connection.sendPacket(new CPlayerPacket.PositionRotationPacket(
                            mc.player.getPosX(), mc.player.getPosY() + 0.753, mc.player.getPosZ(),
                            mc.player.rotationYaw, mc.player.rotationPitch, false));
                    mc.player.setPosition(mc.player.getPosX(), mc.player.getPosY() + 1.0, mc.player.getPosZ());
                }
                break;
            case "Teleport":
                if (mc.player.isOnGround() || !teleportGround.get()) {
                    mc.player.setPositionAndUpdate(
                            mc.player.getPosX(),
                            mc.player.getPosY() + teleportHeight.get(),
                            mc.player.getPosZ());
                }
                break;
            case "Vulcan2.9.0":
                if (mc.player.ticksExisted % 10 == 0) {
                    mc.player.setMotion(mc.player.getMotion().add(0, -0.1, 0));
                }
                if (mc.player.ticksExisted % 2 == 0) {
                    mc.player.setMotion(mc.player.getMotion().add(0, 0.7, 0));
                } else {
                    mc.player.setMotion(mc.player.getMotion().add(0, MoveUtils.isMoving() ? 0.42 : 0.6, 0));
                }
                break;
            case "Pulldown":
                if (!mc.player.isOnGround() && mc.player.getMotion().y < triggerMotion.get()) {
                    mc.player.setMotion(mc.player.getMotion().add(0, -dragMotion.get(), 0));
                } else {
                    mc.player.jump();
                }
                break;
            case "AAC3.3.9":
                if (mc.player.isOnGround()) {
                    mc.player.jump();
                    mc.player.setMotion(mc.player.getMotion().add(0, 0.4001, 0));
                }
                if (mc.player.getMotion().y < 0) {
                    mc.player.setMotion(mc.player.getMotion().add(0, -9.45E-6, 0));
                    mc.timer.timerSpeed = 1.6f;
                } else {
                    mc.timer.timerSpeed = 1.0f;
                }
                break;
            case "AAC3.6.4":
                if (mc.player.ticksExisted % 4 == 1) {
                    mc.player.setMotion(mc.player.getMotion().add(0, 0.4195464, 0));
                    mc.player.setPosition(mc.player.getPosX() - 0.035, mc.player.getPosY(), mc.player.getPosZ());
                } else if (mc.player.ticksExisted % 4 == 0) {
                    mc.player.setMotion(mc.player.getMotion().add(0, -0.5, 0));
                    mc.player.setPosition(mc.player.getPosX() + 0.035, mc.player.getPosY(), mc.player.getPosZ());
                }
                break;
        }
    }

    // ======================== FIND BLOCK ========================

    private void findBlock() {
        String tech = technique.get();

        switch (tech) {
            case "Normal":
                blockPos = findNormalBlock();
                break;
            case "Rewinside":
                blockPos = findRewinsideBlock();
                break;
            case "Expand":
                blockPos = findExpandBlock();
                break;
            case "Telly":
                blockPos = findTellyBlock();
                break;
            case "GodBridge":
                blockPos = findGodBridgeBlock();
                break;
            case "Breezily":
                blockPos = findNormalBlock(); // Breezily uses same block finding as Normal
                break;
            default:
                blockPos = findNormalBlock();
        }

        if (blockPos != null) {
            facing = getPlaceSide(blockPos);
            if (facing == null) blockPos = null;
        }
    }

    private BlockPos findNormalBlock() {
        int posY = (int) (mc.player.getPosY() - 1);
        int startY = (int) mc.player.getPosY();
        int range = 5;

        if (requiresSight.get()) {
            BlockPos sightPos = getBlockFromSight();
            if (sightPos != null && isValidBlockPos(sightPos)) {
                return sightPos;
            }
        }

        for (int y = posY; y >= posY - range; y--) {
            for (int x = (int) mc.player.getPosX() - range; x <= (int) mc.player.getPosX() + range; x++) {
                for (int z = (int) mc.player.getPosZ() - range; z <= (int) mc.player.getPosZ() + range; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (isValidBlockPos(pos)) return pos;
                }
            }
        }

        // Clutch fallback
        if (allowClutching.get()) {
            BlockPos clutch = findClutchBlock();
            if (clutch != null) return clutch;
        }

        return null;
    }

    private BlockPos findRewinsideBlock() {
        double rad = Math.toRadians(mc.player.rotationYaw);
        double x = -Math.sin(rad);
        double z = Math.cos(rad);

        BlockPos ahead = new BlockPos(
                mc.player.getPosX() + x,
                mc.player.getPosY() - 1,
                mc.player.getPosZ() + z
        );

        if (isValidBlockPos(ahead)) return ahead;

        // Fallback to block below player
        BlockPos below = new BlockPos(mc.player.getPosX(), mc.player.getPosY() - 1, mc.player.getPosZ());
        if (isValidBlockPos(below)) return below;

        return null;
    }

    private BlockPos findExpandBlock() {
        int length = expandLength.get().intValue();
        float yaw = mc.player.rotationYaw;
        double rad = Math.toRadians(yaw);
        double dirX = -Math.sin(rad);
        double dirZ = Math.cos(rad);

        if (omniDirectionalExpand.get()) {
            int xSign = (int) Math.round(dirX);
            int zSign = (int) Math.round(dirZ);

            for (int i = length; i >= 0; i--) {
                BlockPos pos = new BlockPos(
                        mc.player.getPosX() + xSign * i,
                        mc.player.getPosY() - 1,
                        mc.player.getPosZ() + zSign * i
                );
                if (isValidBlockPos(pos)) return pos;
            }
        } else {
            Direction facingDir = mc.player.getHorizontalFacing();
            for (int i = length; i >= 0; i--) {
                BlockPos pos = new BlockPos(
                        mc.player.getPosX() + facingDir.getXOffset() * i,
                        mc.player.getPosY() - 1,
                        mc.player.getPosZ() + facingDir.getZOffset() * i
                );
                if (isValidBlockPos(pos)) return pos;
            }
        }

        BlockPos below = new BlockPos(mc.player.getPosX(), mc.player.getPosY() - 1, mc.player.getPosZ());
        if (isValidBlockPos(below)) return below;

        return null;
    }

    private BlockPos findTellyBlock() {
        int posY = (int) (mc.player.getPosY() - 1);
        BlockPos below = new BlockPos(mc.player.getPosX(), posY, mc.player.getPosZ());

        if (isValidBlockPos(below)) return below;

        // Find nearest valid block
        for (int y = posY; y >= posY - 3; y--) {
            for (int x = (int) mc.player.getPosX() - 2; x <= (int) mc.player.getPosX() + 2; x++) {
                for (int z = (int) mc.player.getPosZ() - 2; z <= (int) mc.player.getPosZ() + 2; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (isValidBlockPos(pos)) return pos;
                }
            }
        }

        return null;
    }

    private BlockPos findGodBridgeBlock() {
        BlockPos below = new BlockPos(mc.player.getPosX(), mc.player.getPosY() - 1, mc.player.getPosZ());
        if (isValidBlockPos(below)) return below;

        // Check ahead
        float yaw = mc.player.rotationYaw;
        double rad = Math.toRadians(yaw);
        BlockPos ahead = new BlockPos(
                mc.player.getPosX() - Math.sin(rad),
                mc.player.getPosY() - 1,
                mc.player.getPosZ() + Math.cos(rad)
        );
        if (isValidBlockPos(ahead)) return ahead;

        return null;
    }

    private BlockPos findClutchBlock() {
        int hBlocks = horizontalClutchBlocks.get().intValue();
        int vBlocks = verticalClutchBlocks.get().intValue();

        for (int dy = -vBlocks; dy <= vBlocks; dy++) {
            for (int dx = -hBlocks; dx <= hBlocks; dx++) {
                for (int dz = -hBlocks; dz <= hBlocks; dz++) {
                    BlockPos pos = new BlockPos(
                            mc.player.getPosX() + dx,
                            mc.player.getPosY() - 1 + dy,
                            mc.player.getPosZ() + dz
                    );
                    if (isValidBlockPos(pos)) return pos;
                }
            }
        }
        return null;
    }

    private BlockPos getBlockFromSight() {
        Vector3d eyes = mc.player.getEyePosition(1.0f);
        Vector3d look = mc.player.getLookVec();
        Vector3d reach = eyes.add(look.scale(4.5));

        RayTraceContext ctx = new RayTraceContext(
                eyes, reach,
                RayTraceContext.BlockMode.OUTLINE,
                RayTraceContext.FluidMode.NONE,
                mc.player);
        BlockRayTraceResult result = mc.world.rayTraceBlocks(ctx);

        if (result.getType() == RayTraceResult.Type.BLOCK) {
            BlockPos sidePos = result.getPos().offset(result.getFace());
            if (isValidBlockPos(sidePos)) {
                return sidePos;
            }
        }
        return null;
    }

    // ======================== PLACING ========================

    private void placeBlock() {
        if (blockPos == null || facing == null) return;

        if (extraClicks.get()) {
            doExtraClicks();
            if (blockPos == null) return;
        }

        int slot = findBestBlockSlot();
        if (slot == -1) return;

        boolean isDifferentSlot = slot != mc.player.inventory.currentItem;
        int previousSlot = mc.player.inventory.currentItem;

        if (isDifferentSlot) {
            if (autoBlockToggle.get() && autoBlockMode.is("Pick")) {
                mc.player.inventory.currentItem = slot;
            } else if (autoBlockToggle.get() && autoBlockMode.is("Spoof")) {
                mc.player.connection.sendPacket(new CHeldItemChangePacket(slot));
            } else if (autoBlockToggle.get() && autoBlockMode.is("Switch")) {
                mc.player.inventory.currentItem = slot;
            } else {
                mc.player.inventory.currentItem = slot;
            }
            lastHotbarSlot = slot;
        }

        Vector3d hitVec = getHitVec(blockPos, facing);
        if (hitVec == null) return;

        sendRotationPacket();

        BlockRayTraceResult rayTraceResult = new BlockRayTraceResult(hitVec, facing, blockPos, false);
        ActionResultType result = mc.playerController.processRightClickBlock(
                mc.player, mc.world, Hand.MAIN_HAND, rayTraceResult);

        if (result == ActionResultType.SUCCESS) {
            mc.player.swingArm(Hand.MAIN_HAND);
            delayTimer.reset();

            if (speedModifier.get() != 1f && mc.player.isOnGround()) {
                mc.player.setMotion(mc.player.getMotion().mul(speedModifier.get(), 1, speedModifier.get()));
            }

            placedBlocksWithoutEagle++;
            blocksPlacedUntilJump++;

            if (!autoBlockToggle.get() || !autoBlockMode.is("Switch")) {
                // Restore previous slot if switched but not in Switch mode
            }

            updatePlacedBlocksForTelly();
            checkAutoSwitch(slot);
        }
    }

    private void doExtraClicks() {
        int baseClicks = simulateDoubleClicking.get() ? random.nextInt(3) - 1 : 0;
        int clicks = extraClicksCounter + baseClicks;

        long now = System.currentTimeMillis();
        int cpsMin = extraClickCPSMin.get().intValue();
        int cpsMax = extraClickCPSMax.get().intValue();
        int delayMs = 1000 / (cpsMin + random.nextInt(cpsMax - cpsMin + 1));

        if (now - lastExtraClick >= delayMs) {
            for (int i = 0; i < Math.max(1, clicks); i++) {
                extraClicksCounter--;
                lastExtraClick = now;
            }
        }
    }

    private void checkAutoSwitch(int currentSlot) {
        if (!autoBlockToggle.get()) return;

        ItemStack stack = mc.player.inventory.getStackInSlot(currentSlot);
        if (stack.isEmpty()) return;

        if (earlySwitch.get() && stack.getCount() <= amountBeforeSwitch.get().intValue()) {
            int newSlot = sortByHighestAmount.get()
                    ? findBestBlockSlotForPlace()
                    : findBlockInHotbarGreaterThan(amountBeforeSwitch.get().intValue());
            if (newSlot != -1) {
                if (autoBlockMode.is("Spoof")) {
                    mc.player.connection.sendPacket(new CHeldItemChangePacket(newSlot));
                } else {
                    mc.player.inventory.currentItem = newSlot;
                }
            }
        }
    }

    // ======================== EAGLE ========================

    private boolean isEagleEnabled() {
        return !eagleOption.is("Off") && !technique.is("GodBridge");
    }

    @Subscribe
    public void onInput(EventInput eventInput) {
        if (mc.player == null) return;

        // Zitter
        handleZitter(eventInput);

        // Eagle
        String eagleVal = eagleOption.get();
        if (isEagleEnabled()) {
            boolean shouldSneak = false;
            BlockPos below = new BlockPos(mc.player.getPosX(), mc.player.getPosY() - 1, mc.player.getPosZ());

            if (!isSolid(below)) {
                shouldSneak = true;
            }

            if ("Normal".equals(eagleVal) || "Silent".equals(eagleVal)) {
                if (shouldSneak && !isNearEdge() &&                     placedBlocksWithoutEagle >= randomInt(
                        blocksToEagleMin.get().intValue(), blocksToEagleMax.get().intValue())) {
                    eagleSneaking = true;
                    if ("Normal".equals(eagleVal)) {
                        eventInput.setSneak(true);
                    }
                } else if (!shouldSneak) {
                    if (useMaxSneakTime.get()) {
                        // Handled via separate logic
                    }
                    if (blockSneakingAgainUntilOnGround.get() && requestedStopSneak && !mc.player.isOnGround()) {
                        // Keep sneaking
                    } else {
                        eagleSneaking = false;
                        eventInput.setSneak(false);
                    }
                }
            } else if ("Reverse".equals(eagleVal)) {
                if (!shouldSneak) {
                    eventInput.setSneak(true);
                } else {
                    eventInput.setSneak(false);
                }
            }
        }

        // Strafe — handled in onStrafe for jump strafe
        // Do NOT force strafe here — it breaks movement direction

        // SameY / JumpOnUserInput
        if (sameY.get() && jumpOnUserInput.get() && mc.player.isOnGround()
                && eventInput.isJump()
                && !technique.is("GodBridge")) {
            eventInput.setJump(false);
        }

        // Sprint
        if (sprint.get()) {
            eventInput.setSprint(true);
        }
    }

    private void handleZitter(EventInput input) {
        String mode = zitterMode.get();
        if ("Off".equals(mode)) return;

        if ("Smooth".equals(mode)) {
            if (useSneakMidAir.get() && !mc.player.isOnGround()) {
                input.setSneak(true);
            }
            if (mc.player.isOnGround() && mc.player.collidedVertically) {
                input.setForward(zitterDirection ? 1f : -1f);
                zitterDirection = !zitterDirection;
            }
        } else if ("Teleport".equals(mode)) {
            double yaw = Math.toRadians(mc.player.rotationYaw + (zitterDirection ? 90 : -90));
            mc.player.setMotion(mc.player.getMotion().add(-Math.sin(yaw) * zitterStrength.get(), 0, Math.cos(yaw) * zitterStrength.get()));
            zitterDirection = !zitterDirection;
        }
    }

    // ======================== MOVEMENT EVENTS ========================

    @Subscribe
    public void onPacket(EventPacket e) {
    }

    // ======================== UTILITIES ========================

    private int findBestBlockSlot() {
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (isValidBlock(stack)) {
                slots.add(i);
            }
        }

        if (slots.isEmpty()) return -1;

        if (sortByHighestAmount.get()) {
            slots.sort((a, b) -> Integer.compare(
                    mc.player.inventory.getStackInSlot(b).getCount(),
                    mc.player.inventory.getStackInSlot(a).getCount()));
        }

        return slots.get(0);
    }

    private int findBestBlockSlotForPlace() {
        int bestSlot = -1;
        int bestCount = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (isValidBlock(stack) && stack.getCount() > bestCount) {
                bestCount = stack.getCount();
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    private int findBlockInHotbarGreaterThan(int amount) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (isValidBlock(stack) && stack.getCount() > amount) {
                return i;
            }
        }
        return -1;
    }

    private boolean isValidBlock(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) return false;
        Block block = ((BlockItem) stack.getItem()).getBlock();
        return !DISALLOWED_BLOCKS.contains(block)
                && !(block instanceof FallingBlock);
    }

    private boolean isValidBlockPos(BlockPos pos) {
        if (pos.getY() < 0 || pos.getY() > 255) return false;
        Block block = mc.world.getBlockState(pos).getBlock();
        if (!(block instanceof AirBlock) && !(block instanceof FlowingFluidBlock)) return false;

        // Check if there's a solid block to place against
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.offset(dir);
            Block neighborBlock = mc.world.getBlockState(neighbor).getBlock();
            if (!(neighborBlock instanceof AirBlock) && !(neighborBlock instanceof FlowingFluidBlock)) {
                return blockSafe.get() || isSafeBlock(neighbor);
            }
        }
        return false;
    }

    private boolean isSafeBlock(BlockPos pos) {
        Block block = mc.world.getBlockState(pos).getBlock();
        return block != Blocks.LAVA && !(block instanceof FlowingFluidBlock);
    }

    private Direction getPlaceSide(BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.offset(dir);
            Block block = mc.world.getBlockState(neighbor).getBlock();
            if (!(block instanceof AirBlock) && !(block instanceof FlowingFluidBlock)) {
                return dir.getOpposite();
            }
        }
        return null;
    }

    private boolean isSolid(BlockPos pos) {
        Block block = mc.world.getBlockState(pos).getBlock();
        return !(block instanceof AirBlock);
    }

    private boolean isAir(BlockPos pos) {
        return mc.world.getBlockState(pos).getBlock() instanceof AirBlock;
    }

    private boolean isNearEdge() {
        for (double xOff : OFFSETS) {
            for (double zOff : OFFSETS) {
                BlockPos pos = new BlockPos(
                        mc.player.getPosX() + xOff,
                        mc.player.getPosY() - 1,
                        mc.player.getPosZ() + zOff
                );
                if (isSolid(pos)) return false;
            }
        }
        return true;
    }

    private boolean isOnCenter(BlockPos pos) {
        double x = mc.player.getPosX() % 1;
        double z = mc.player.getPosZ() % 1;
        if (x < 0) x += 1;
        if (z < 0) z += 1;
        return (x >= 0.3 && x <= 0.7) && (z >= 0.3 && z <= 0.7);
    }

    private boolean isLookingDiagonally() {
        float snappedYaw = (float) Math.rint(scaffoldYaw / 45f) * 45f;
        return snappedYaw % 90f != 0f;
    }

    private Vector3d getHitVec(BlockPos pos, Direction facing) {
        Vector3d eyes = mc.player.getEyePosition(1.0f);
        Vector3d blockCenter = new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);

        Vector3d hitVec = blockCenter.add(
                facing.getXOffset() * 0.5,
                facing.getYOffset() * 0.5,
                facing.getZOffset() * 0.5
        );

        // Clamp to block bounds
        hitVec = new Vector3d(
                MathHelper.clamp(hitVec.x, pos.getX(), pos.getX() + 1),
                MathHelper.clamp(hitVec.y, pos.getY(), pos.getY() + 1),
                MathHelper.clamp(hitVec.z, pos.getZ(), pos.getZ() + 1)
        );

        return hitVec;
    }

    private void updatePlacedBlocksForTelly() {
        if (blocksUntilAxisChange > horizontalPlacements + verticalPlacements) {
            blocksUntilAxisChange = 0;
            horizontalPlacements = randomInt(horizontalPlacementsMin.get().intValue(), horizontalPlacementsMax.get().intValue());
            verticalPlacements = randomInt(verticalPlacementsMin.get().intValue(), verticalPlacementsMax.get().intValue());
        } else {
            blocksUntilAxisChange++;
        }
    }

    private int randomInt(int min, int max) {
        if (min >= max) return min;
        return random.nextInt(max - min + 1) + min;
    }

    private float updateRotationFloat(float current, float target, float maxIncrease) {
        float diff = MathHelper.wrapDegrees(target - current);
        if (diff > maxIncrease) diff = maxIncrease;
        if (diff < -maxIncrease) diff = -maxIncrease;
        return current + diff;
    }

    // ======================== RENDER ========================

    @Subscribe
    public void onDisplay(EventDisplay e) {
        if (e.getType() != EventDisplay.Type.POST) return;
        if (!mark.get() && !blockESP.get()) return;
        if (blockPos == null) return;
    }
}
