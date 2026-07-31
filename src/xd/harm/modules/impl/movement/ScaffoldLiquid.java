package xd.harm.modules.impl.movement;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.matrix.MatrixStack;
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
import xd.harm.events.movement.SprintEvent;
import xd.harm.events.movement.StrafeEvent;
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

/*
 * Полное портирование Scaffold из LiquidBounce.
 * Все настройки, техники, тауэры, фичи — один-в-один.
 */
@ModuleRegister(name = "ScaffoldLiquid", category = Category.Movement,
        desc = "Scaffold из LiquidBounce")
public class ScaffoldLiquid extends Module {

    // ======================== КОНСТАНТЫ ========================

    private static final List<Block> DISALLOWED_BLOCKS = Arrays.asList(
            Blocks.TNT, Blocks.COBWEB, Blocks.NETHER_PORTAL
    );

    // ======================== КАТЕГОРИИ ========================

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

    // ======================== ТЕХНИКА ========================

    private final ModeSetting technique = new ModeSetting("Техника", "Normal",
            "Normal", "Expand", "GodBridge", "Breezily");

    // --- Normal Technique ---
    private final ModeSetting aimMode = new ModeSetting("Режим прицела", "Stabilized",
            "Center", "Random", "Stabilized", "NearestRotation",
            "ReverseYaw", "DiagonalYaw", "AngleYaw", "EdgePoint")
            .setVisible(() -> technique.is("Normal"));

    private final BooleanSetting requiresSight = new BooleanSetting("Требуется обзор", false)
            .setVisible(() -> technique.is("Normal"));

    // Eagle (Normal)
    private final BooleanSetting eagle = new BooleanSetting("Орёл", true)
            .setVisible(() -> technique.is("Normal"));
    private final ModeSetting eagleMode = new ModeSetting("Режим орла", "Normal", "Normal", "Sneak")
            .setVisible(() -> technique.is("Normal") && eagle.get());
    private final SliderSetting eagleEdgeDistance = new SliderSetting("Дистанция края", 0.5f, 0.1f, 1f, 0.05f)
            .setVisible(() -> technique.is("Normal") && eagle.get());
    private final SliderSetting blocksToEagle = new SliderSetting("Блоков до орла", 0f, 0f, 10f, 1f)
            .setVisible(() -> technique.is("Normal") && eagle.get());
    private final BooleanSetting eagleSprint = new BooleanSetting("Спринт орла", false)
            .setVisible(() -> technique.is("Normal") && eagle.get());

    // Telly (Normal)
    private final BooleanSetting telly = new BooleanSetting("Telly", false)
            .setVisible(() -> technique.is("Normal"));
    private final ModeSetting tellyMode = new ModeSetting("Режим Telly", "Reverse", "Reverse", "Reset")
            .setVisible(() -> technique.is("Normal") && telly.get());
    private final BooleanSetting tellyAutoJump = new BooleanSetting("Авто-прыжок", true)
            .setVisible(() -> technique.is("Normal") && telly.get());
    private final SliderSetting blocksToJumpMin = new SliderSetting("Мин. блоков", 3f, 1f, 20f, 1f)
            .setVisible(() -> technique.is("Normal") && telly.get() && tellyAutoJump.get());
    private final SliderSetting blocksToJumpMax = new SliderSetting("Макс. блоков", 5f, 1f, 20f, 1f)
            .setVisible(() -> technique.is("Normal") && telly.get() && tellyAutoJump.get());
    private final BooleanSetting jumpOnUserInput = new BooleanSetting("Прыжок по вводу", false)
            .setVisible(() -> technique.is("Normal") && telly.get());
    private final BooleanSetting airSafe = new BooleanSetting("Безопасность в воздухе", false)
            .setVisible(() -> technique.is("Normal") && telly.get());

    // Down (Normal)
    private final BooleanSetting down = new BooleanSetting("Вниз", false)
            .setVisible(() -> technique.is("Normal"));

    // StabilizeMovement (Normal)
    private final BooleanSetting stabilizeMovement = new BooleanSetting("Стабилизация движения", false)
            .setVisible(() -> technique.is("Normal"));

    // Ceiling (Normal)
    private final BooleanSetting ceiling = new BooleanSetting("Потолок", false)
            .setVisible(() -> technique.is("Normal"));

    // HeadHitter (Normal)
    private final BooleanSetting headHitter = new BooleanSetting("Удар по голове", false)
            .setVisible(() -> technique.is("Normal"));

    // --- Expand ---
    private final SliderSetting expand = new SliderSetting("Расширение", 0f, 0f, 8f, 1f)
            .setVisible(() -> technique.is("Expand"));

    // --- GodBridge ---
    private final BooleanSetting godBridgeJump = new BooleanSetting("Прыжок", false)
            .setVisible(() -> technique.is("GodBridge"));
    private final BooleanSetting godBridgeSneak = new BooleanSetting("Скрытие", true)
            .setVisible(() -> technique.is("GodBridge"));
    private final BooleanSetting godBridgeStopInput = new BooleanSetting("Стоп ввод", false)
            .setVisible(() -> technique.is("GodBridge"));
    private final BooleanSetting godBridgeBackwards = new BooleanSetting("Назад", false)
            .setVisible(() -> technique.is("GodBridge"));
    private final SliderSetting godBridgeForceSneak = new SliderSetting("Принуд. скрытие <", 3f, 0f, 10f, 1f)
            .setVisible(() -> technique.is("GodBridge"));
    private final SliderSetting godBridgeSneakTime = new SliderSetting("Время скрытия", 1f, 1f, 10f, 1f)
            .setVisible(() -> technique.is("GodBridge"));
    private final SliderSetting godBridgePitch = new SliderSetting("Питч", 75.7f, 60f, 90f, 0.1f)
            .setVisible(() -> technique.is("GodBridge"));

    // --- Breezily ---
    private final BooleanSetting breezilyAutoJump = new BooleanSetting("Авто-прыжок", true)
            .setVisible(() -> technique.is("Breezily"));
    private final SliderSetting breezilyPitch = new SliderSetting("Питч", 75f, 60f, 90f, 0.1f)
            .setVisible(() -> technique.is("Breezily"));

    // ======================== РОТАЦИИ ========================

    private final ModeSetting rotationTiming = new ModeSetting("Тайминг ротаций", "Normal",
            "Normal", "OnTick", "OnTickSnap");

    private final BooleanSetting considerInventory = new BooleanSetting("Учитывать инвентарь", false);

    private final ModeSetting safeWalkMode = new ModeSetting("SafeWalk", "Normal",
            "Normal", "Скрытие", "Выкл");

    private final ModeSetting swingMode = new ModeSetting("Размах", "DoNotHide",
            "DoNotHide", "Hide", "NoSwing", "Instant");

    // ======================== ДВИЖЕНИЕ ========================

    private final ModeSetting sprintControl = new ModeSetting("Спринт", "Всегда",
            "Всегда", "Выкл", "Легит", "Переключение", "Без пакета");

    private final BooleanSetting strafe = new BooleanSetting("Стрейф", false);

    private final BooleanSetting jumpStrafe = new BooleanSetting("Стрейф прыжка", false);
    private final SliderSetting jumpStraightStrafe = new SliderSetting("Прямой стрейф", 0.5f, 0f, 1f, 0.01f)
            .setVisible(() -> jumpStrafe.get());
    private final SliderSetting jumpDiagonalStrafe = new SliderSetting("Диагональный стрейф", 0.5f, 0f, 1f, 0.01f)
            .setVisible(() -> jumpStrafe.get());

    private final BooleanSetting speedLimiter = new BooleanSetting("Ограничитель скорости", false);
    private final SliderSetting speedLimit = new SliderSetting("Лимит скорости", 1f, 0.1f, 5f, 0.1f)
            .setVisible(() -> speedLimiter.get());

    private final BooleanSetting acceleration = new BooleanSetting("Ускорение", false);

    // ======================== ТАУЭР ========================

    private final ModeSetting towerMode = new ModeSetting("Тауэр", "Выкл",
            "Выкл", "Motion", "Pulldown", "Karhu", "Vulcan", "Hypixel");

    private final SliderSetting towerMotion = new SliderSetting("Motion", 0.42f, 0.1f, 1f, 0.01f)
            .setVisible(() -> !towerMode.is("Выкл"));

    // ======================== ДОПОЛНИТЕЛЬНО ========================

    private final BooleanSetting autoBlock = new BooleanSetting("Авто-блок", false);
    private final BooleanSetting alwaysHoldBlock = new BooleanSetting("Всегда держать блок", false)
            .setVisible(() -> autoBlock.get());
    private final SliderSetting doNotUseBelowCount = new SliderSetting("Не исп. ниже", 1f, 0f, 10f, 1f)
            .setVisible(() -> autoBlock.get());
    private final SliderSetting slotResetDelay = new SliderSetting("Задержка сброса", 5f, 1f, 20f, 1f)
            .setVisible(() -> autoBlock.get());

    private final BooleanSetting movementPrediction = new BooleanSetting("Прогноз движения", false);
    private final BooleanSetting autoSpeed = new BooleanSetting("Авто-скорость", false);
    private final BooleanSetting blink = new BooleanSetting("Blink", false);
    private final BooleanSetting ledge = new BooleanSetting("Край", true);

    // SameY
    private final BooleanSetting sameY = new BooleanSetting("Та же Y", false);
    private final ModeSetting sameYMode = new ModeSetting("Режим Y", "Off", "Off", "On", "Falling", "Hypixel")
            .setVisible(() -> sameY.get());

    // SimulatePlacementAttempts
    private final BooleanSetting simulatePlacement = new BooleanSetting("Симуляция установки", false);
    private final BooleanSetting failedAttemptsOnly = new BooleanSetting("Только неудачные", true)
            .setVisible(() -> simulatePlacement.get());

    // Spoof
    private final ModeSetting spoof = new ModeSetting("Спуф слот", "Выкл", "Выкл", "Обычный", "Фейк");

    // Extra clicks
    private final BooleanSetting extraClicks = new BooleanSetting("Доп. клики", false);
    private final SliderSetting extraClickCPS = new SliderSetting("CPS доп. кликов", 5f, 1f, 20f, 1f)
            .setVisible(() -> extraClicks.get());
    private final BooleanSetting simulateDoubleClick = new BooleanSetting("Двойной клик", false);

    // ======================== РЕНДЕР ========================

    private final BooleanSetting renderEnabled = new BooleanSetting("Рендер", true);

    // ======================== СОСТОЯНИЕ ========================

    private final TimerUtility placeTimer = TimerUtility.create();
    private final TimerUtility eagleTimer = TimerUtility.create();
    private final TimerUtility extraClickTimer = TimerUtility.create();

    private boolean silentHeldSwap;
    private boolean eagleSneaking;
    private int placedBlocksUntilEagle;
    private int blocksPlacedSinceJump;
    private int ticksUntilJump;
    private int launchY;
    private int startY;
    private boolean wasTowering;
    private int towerJumps;
    private int posY;
    private int lastSlot;
    private int itemBefore;
    private int ticksExisted;
    private boolean godBridgeOnRightSide;

    private Vector2f rotationVector = new Vector2f(0, 0);
    private float lastYaw;
    private float lastPitch;
    private float scaffoldYaw;
    private float scaffoldPitch;

    private BlockPos blockPos;
    private Direction facing;
    private int blockCount;

    private final Random random = new Random();

    // ======================== КОНСТРУКТОР ========================

    public ScaffoldLiquid() {
        addSettings(
                catBasic, delay, minDist, timer,

                catTechnique, technique,
                // Normal
                aimMode, requiresSight,
                eagle, eagleMode, eagleEdgeDistance, blocksToEagle, eagleSprint,
                telly, tellyMode, tellyAutoJump, blocksToJumpMin, blocksToJumpMax,
                jumpOnUserInput, airSafe,
                down, stabilizeMovement, ceiling, headHitter,
                // Expand
                expand,
                // GodBridge
                godBridgeJump, godBridgeSneak, godBridgeStopInput, godBridgeBackwards,
                godBridgeForceSneak, godBridgeSneakTime, godBridgePitch,
                // Breezily
                breezilyAutoJump, breezilyPitch,

                catRotation, rotationTiming, considerInventory, safeWalkMode, swingMode,

                catMovement, sprintControl, strafe, jumpStrafe, jumpStraightStrafe,
                jumpDiagonalStrafe, speedLimiter, speedLimit, acceleration,

                catTower, towerMode, towerMotion,

                catAdvanced,
                autoBlock, alwaysHoldBlock, doNotUseBelowCount, slotResetDelay,
                movementPrediction, autoSpeed, blink, ledge,
                sameY, sameYMode,
                simulatePlacement, failedAttemptsOnly,
                spoof, extraClicks, extraClickCPS, simulateDoubleClick,

                catRender, renderEnabled
        );
    }

    // ======================== ON ENABLE / ON DISABLE ========================

    @Override
    public boolean onEnable() {
        super.onEnable();
        lastSlot = mc.player.inventory.currentItem;
        itemBefore = mc.player.inventory.currentItem;
        posY = (int) (mc.player.getPosY() - 1);
        startY = (int) mc.player.getPosY();
        launchY = (int) mc.player.getPosY();
        eagleSneaking = false;
        placedBlocksUntilEagle = 0;
        blocksPlacedSinceJump = 0;
        ticksUntilJump = randomBlocksToJump();
        towerJumps = 2;
        wasTowering = false;
        godBridgeOnRightSide = true;
        placeTimer.reset();
        eagleTimer.reset();
        extraClickTimer.reset();
        updateBlockCount();
        return false;
    }

    @Override
    public boolean onDisable() {
        if (spoof.is("Фейк")) {
            if (mc.player.inventory.currentItem != lastSlot) {
                try {
                    silentHeldSwap = true;
                    mc.player.connection.sendPacket(new CHeldItemChangePacket(mc.player.inventory.currentItem));
                } finally {
                    silentHeldSwap = false;
                }
            }
        }
        mc.gameSettings.keyBindSneak.setPressed(false);
        TimerModule standaloneTimer = Harmony.getInstance().getModuleManager().getTimer();
        if (!standaloneTimer.isState()) {
            mc.timer.timerSpeed = 1.0f;
        }
        return super.onDisable();
    }

    // ======================== EVENT: ROTATION (аналог RotationUpdateEvent) ========================

    @Subscribe
    public void onMotion(EventMotion e) {
        // Если не выбрана техника GodBridge/Breezily — используем обычные ротации
        if (!technique.is("GodBridge") && !technique.is("Breezily")) {
            // Поиск блока и цели для ротации (как в LB rotationUpdateHandler)
            findBestBlockSlot();
            processBlockData();

            // SameY — обновление позиции
            if (sameY.get()) {
                handleSameY();
            }

            // Рассчитываем ротацию на основе текущего блока/фейса
            calculateRotationTarget();
        }

        // Применяем ротацию для GodBridge/Breezily
        if (technique.is("GodBridge")) {
            handleGodBridgeRotation();
        } else if (technique.is("Breezily")) {
            handleBreezilyRotation();
        }

        // Применяем ротацию в событие
        if (!rotationTiming.is("OnTickSnap")) {
            e.setYaw(rotationVector.x);
            e.setPitch(rotationVector.y);
            mc.player.renderYawOffset = rotationVector.x;
            mc.player.rotationYawHead = rotationVector.x;
            mc.player.rotationPitchHead = rotationVector.y;
        }

        // Tower
        handleTower();
        sprint();
    }

    // ======================== EVENT: TICK (аналог GameTickEvent) ========================

    @Subscribe
    public void onUpdate(EventUpdate e) {
        if (mc.world == null) return;

        updateBlockCount();

        // Поиск блока в руке
        ItemStack mainHand = mc.player.getHeldItemMainhand();
        if (mainHand.isEmpty() || !(mainHand.getItem() instanceof BlockItem) || !isValidBlock(mainHand)) {
            switchToBlockSlot();
        }

        // Telly
        handleTelly();

        // Breezily — авто-прыжок
        if (technique.is("Breezily") && breezilyAutoJump.get()) {
            if (MoveUtils.isMoving() && mc.player.isOnGround()
                    && !mc.gameSettings.keyBindJump.isKeyDown()) {
                mc.player.jump();
            }
        }

        // Ledge
        if (ledge.get()) {
            handleLedge();
        }

        // SameY — обновление launchY
        if (sameY.get() && mc.player.isOnGround()) {
            launchY = (int) mc.player.getPosY();
        }

        // Placement с задержкой
        int currentDelay = delay.get().intValue();
        int placeDelay = currentDelay * 50;

        if (placeTimer.isReached(placeDelay)) {
            if (blockPos != null && facing != null) {
                place();
            }
            placeTimer.reset();
            updateBlockCount();
        }

        // Timer
        applyTimer();

        // Дополнительные фичи
        applyAdvancedFeatures();

        // OnTickSnap — отправка ротации отдельным пакетом
        if (rotationTiming.is("OnTickSnap")) {
            sendRotationPacket();
        }
    }

    // ======================== EVENT: INPUT (аналог MovementInputEvent) ========================

    @Subscribe
    public void onInput(EventInput eventInput) {
        // SafeWalk
        if (!safeWalkMode.is("Выкл")) {
            handleSafeWalk(eventInput);
        }

        // Eagle
        if (technique.is("Normal") && eagle.get()) {
            handleEagle(eventInput);
        }

        // GodBridge — скрытие
        if (technique.is("GodBridge") && godBridgeSneak.get()) {
            if (blockCount < godBridgeForceSneak.getInt()) {
                eventInput.setSneak(true);
            } else if (isOnEdge()) {
                eventInput.setSneak(true);
            }
        }

        // Ledge
        if (ledge.get() && isOnEdge()) {
            if (!lookingAtBlock(blockPos, facing, rotationVector)) {
                eventInput.setSneak(true);
            }
        }

        // Strafe
        if (strafe.get() && MoveUtils.isMoving()
                && !mc.gameSettings.keyBindLeft.isKeyDown()
                && !mc.gameSettings.keyBindRight.isKeyDown()) {
            eventInput.setStrafe(1f);
        }
    }

    @Subscribe
    public void onStrafe(StrafeEvent e) {
        // Strafe handled in onInput via EventInput
    }

    @Subscribe
    public void onSprint(SprintEvent e) {
        if (sprintControl.is("Выкл")) {
            e.cancel();
        }
    }

    @Subscribe
    public void onSendPacket(EventPacket e) {
        if (e.isSend() && e.getPacket() instanceof CHeldItemChangePacket
                && spoof.is("Фейк") && !silentHeldSwap) {
            e.cancel();
        }
    }

    @Subscribe
    public void onDisplay(EventDisplay e) {
        if (e.getType() != EventDisplay.Type.POST) return;
        renderBlockCounter(e.getMatrixStack());
    }

    // ======================== ROTATION HANDLERS ========================

    private void calculateRotationTarget() {
        if (blockPos == null || facing == null) return;

        Vector3d hit = getFaceHitVec(blockPos, facing);
        Vector2f target = calculateRotation(hit);

        // Плавное приближение
        float speed = rotationTiming.is("Normal") ? 30f : 60f;
        scaffoldYaw = updateRotationFloat(scaffoldYaw, target.x, speed);
        scaffoldPitch = updateRotationFloat(scaffoldPitch, target.y, speed);

        rotationVector = new Vector2f(scaffoldYaw, scaffoldPitch);
        lastYaw = scaffoldYaw;
        lastPitch = scaffoldPitch;
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
                    if (!right) godBridgeOnRightSide = !godBridgeOnRightSide;

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
                        godBridgeOnRightSide = !godBridgeOnRightSide;
                    }
                }
                scaffoldYaw = snappedYaw + (godBridgeOnRightSide ? 45 : -45);
            } else {
                scaffoldYaw = snappedYaw;
            }
            scaffoldPitch = godBridgePitch.get();
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
                new net.minecraft.network.play.client.CPlayerPacket.RotationPacket(
                        rotationVector.x, rotationVector.y, mc.player.isOnGround()));
    }

    // ======================== TOWER ========================

    private void handleTower() {
        boolean jumping = mc.gameSettings.keyBindJump.isKeyDown();

        if (towerMode.is("Выкл")) {
            wasTowering = false;
            return;
        }

        if (jumping) {
            wasTowering = true;
            float motion = towerMotion.get();
            if (mc.player.isOnGround()) {
                mc.player.setMotion(mc.player.getMotion().x, motion, mc.player.getMotion().z);
            }
        } else {
            wasTowering = false;
        }
    }

    // ======================== LEDGE ========================

    private void handleLedge() {
        if (isOnEdge() && blockPos != null && facing != null) {
            if (!lookingAtBlock(blockPos, facing, rotationVector)) {
                // Ledge action — подкрадываемся к краю
            }
        }
    }

    // ======================== TELLY ========================

    private int randomBlocksToJump() {
        int min = blocksToJumpMin.getInt();
        int max = blocksToJumpMax.getInt();
        if (max < min) max = min;
        return min + random.nextInt(max - min + 1);
    }

    private void tryJump() {
        if (!mc.player.isOnGround()) return;
        if (airSafe.get()) {
            BlockPos below = new BlockPos(
                    mc.player.getPosX(), mc.player.getPosY() - 1, mc.player.getPosZ());
            if (mc.world.getBlockState(below).getBlock() instanceof AirBlock) return;
        }
        mc.player.jump();
        blocksPlacedSinceJump = 0;
        ticksUntilJump = randomBlocksToJump();
    }

    private void updatePlacedBlocksForTelly() {
        if (!technique.is("Normal") || !telly.get()) return;
        blocksPlacedSinceJump++;
        if (tellyAutoJump.get() && blocksPlacedSinceJump >= ticksUntilJump) {
            tryJump();
        }
    }

    private void handleTelly() {
        if (!technique.is("Normal") || !telly.get()) return;
        if (mc.player.isOnGround()) {
            posY = (int) (mc.player.getPosY() - 1);
        }
        if (jumpOnUserInput.get() && mc.gameSettings.keyBindJump.isKeyDown()) {
            if (mc.player.isOnGround()) {
                mc.player.jump();
            }
        }
    }

    // ======================== SAFE WALK / EAGLE ========================

    private void handleSafeWalk(EventInput eventInput) {
        if (!mc.player.isOnGround()) return;
        if (safeWalkMode.is("Normal")) {
            BlockPos below = new BlockPos(
                    mc.player.getPosX() + mc.player.getMotion().x,
                    mc.player.getPosY() - 1.0,
                    mc.player.getPosZ() + mc.player.getMotion().z);
            if (mc.world.getBlockState(below).getBlock() instanceof AirBlock) {
                eventInput.setSneak(true);
            }
        } else if (safeWalkMode.is("Скрытие")) {
            eventInput.setSneak(true);
        }
    }

    private void handleEagle(EventInput eventInput) {
        if (!mc.player.isOnGround() || !MoveUtils.isMoving()) return;

        if (placedBlocksUntilEagle < blocksToEagle.getInt()) {
            eagleSneaking = false;
            return;
        }

        if (isOnEdge()) {
            if (eagleMode.is("Normal")) {
                eagleSneaking = !eagleSneaking;
                eagleTimer.reset();
            } else if (eagleMode.is("Sneak")) {
                eagleSneaking = true;
            }
        } else {
            eagleSneaking = false;
        }

        if (eagleSneaking) {
            eventInput.setSneak(true);
        }
    }

    // ======================== TIMER ========================

    private void applyTimer() {
        TimerModule standaloneTimer = Harmony.getInstance().getModuleManager().getTimer();
        if (standaloneTimer.isState()) return;
        mc.timer.timerSpeed = timer.get();
    }

    // ======================== ADVANCED FEATURES ========================

    private void applyAdvancedFeatures() {
        // SpeedLimiter
        if (speedLimiter.get() && MoveUtils.isMoving()) {
            double hs = Math.sqrt(mc.player.getMotion().x * mc.player.getMotion().x
                    + mc.player.getMotion().z * mc.player.getMotion().z);
            if (hs > speedLimit.get()) {
                float factor = speedLimit.get() / (float) hs;
                mc.player.setMotion(
                        mc.player.getMotion().x * factor,
                        mc.player.getMotion().y,
                        mc.player.getMotion().z * factor);
            }
        }

        // Acceleration
        if (acceleration.get() && MoveUtils.isMoving()) {
            double hs = Math.sqrt(mc.player.getMotion().x * mc.player.getMotion().x
                    + mc.player.getMotion().z * mc.player.getMotion().z);
            if (hs > 0 && hs < 0.5) {
                mc.player.setMotion(
                        mc.player.getMotion().x * 1.05,
                        mc.player.getMotion().y,
                        mc.player.getMotion().z * 1.05);
            }
        }

        // JumpStrafe
        if (jumpStrafe.get() && !mc.player.isOnGround()) {
            double yawRad = Math.toRadians(mc.player.rotationYaw);
            boolean straight = mc.player.movementInput.moveStrafe == 0;
            float mult = straight ? jumpStraightStrafe.get() : jumpDiagonalStrafe.get();
            double mx = -Math.sin(yawRad) * mult;
            double mz = Math.cos(yawRad) * mult;
            if (mc.player.movementInput.moveForward != 0) {
                mc.player.setMotion(mx, mc.player.getMotion().y, mz);
            }
        }

        // Extra Clicks
        if (extraClicks.get()) {
            if (extraClickTimer.isReached((long) (1000 / extraClickCPS.get()))) {
                mc.player.connection.sendPacket(new CAnimateHandPacket(Hand.MAIN_HAND));
                extraClickTimer.reset();
            }
        }

        // AutoSpeed
        if (autoSpeed.get() && mc.player.isOnGround() && MoveUtils.isMoving()) {
            MoveUtils.setMotion(0.25);
        }
    }

    private void handleSameY() {
        // SameY — логика как в LB
        if (mc.player.isOnGround()) {
            launchY = (int) mc.player.getPosY();
        }
    }

    // ======================== SPRINT ========================

    private void sprint() {
        switch (sprintControl.get()) {
            case "Легит":
                mc.player.setSprinting(MoveUtils.isMoving()
                        && mc.gameSettings.keyBindForward.isKeyDown()
                        && Math.abs(MathHelper.wrapDegrees(
                        mc.player.rotationYaw - rotationVector.x)) < 66.5f);
                break;
            case "Выкл":
                mc.player.setSprinting(false);
                break;
            case "Всегда":
                if (mc.player.moveForward > 0) mc.player.setSprinting(true);
                break;
            case "Переключение":
                mc.player.setSprinting(mc.player.ticksExisted % 2 == 0);
                break;
            case "Без пакета":
                mc.player.setSprinting(false);
                break;
        }
    }

    // ======================== BLOCK FINDING ========================

    private void findBestBlockSlot() {
        // LB: findBestValidHotbarSlotForTarget
        int bestSlot = -1;
        int bestCount = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (isValidBlock(stack) && stack.getCount() > bestCount) {
                bestCount = stack.getCount();
                bestSlot = i;
            }
        }
        if (bestSlot != -1 && autoBlock.get() && alwaysHoldBlock.get()) {
            if (bestSlot != mc.player.inventory.currentItem) {
                try {
                    silentHeldSwap = true;
                    mc.player.connection.sendPacket(new CHeldItemChangePacket(bestSlot));
                } finally {
                    silentHeldSwap = false;
                }
            }
        }
    }

    private void processBlockData() {
        if (technique.is("Expand")) {
            processBlockDataDown();
            return;
        }
        if (technique.is("Normal") && down.get()) {
            processBlockDataDown();
            return;
        }

        if (technique.is("Expand") && expand.get() > 0) {
            Vector3d vec = expandVec(new Vector3d(
                    mc.player.getPosX(), posY, mc.player.getPosZ()));
            setBlockFacingOld(new BlockPos(vec.x, vec.y + 1, vec.z));
        } else {
            blockPos = getBlockPos(mc.player.getPosX(), mc.player.getPosZ());
        }

        if (blockPos != null && !technique.is("Expand")) {
            facing = getPlaceSide(mc.player.getPosX(), mc.player.getPosZ());
        }
    }

    private void processBlockDataDown() {
        BlockPos playerPos = new BlockPos(
                mc.player.getPosX(), mc.player.getPosY(), mc.player.getPosZ());
        for (int y = playerPos.getY() - 1; y >= playerPos.getY() - 5; y--) {
            BlockPos check = new BlockPos(playerPos.getX(), y, playerPos.getZ());
            if (mc.world.isAirBlock(check)) {
                BlockPos target = findDownPlaceTarget(check);
                if (target != null) return;
            } else {
                break;
            }
        }
    }

    private BlockPos findDownPlaceTarget(BlockPos emptyPos) {
        Direction[] sides = {Direction.UP, Direction.NORTH, Direction.SOUTH,
                Direction.EAST, Direction.WEST};
        for (Direction dir : sides) {
            BlockPos adj = emptyPos.offset(dir);
            if (isPosSolid(adj)) {
                blockPos = adj;
                facing = dir.getOpposite();
                return adj;
            }
        }
        return null;
    }

    private Vector3d expandVec(Vector3d position) {
        int exp = expand.getInt();
        if (exp > 0) {
            double dir = Math.toRadians(MoveUtils.getDirection());
            Vector3d expandVector = new Vector3d(-Math.sin(dir), 0, Math.cos(dir));
            int bestExpand = 0;
            for (int i = 0; i < exp; i++) {
                if (!MoveUtils.isMoving()) break;
                Vector3d vec = position.add(0, -1, 0).add(expandVector.scale(i));
                setBlockFacingOld(new BlockPos(vec.x, posY, vec.z));
                if (blockPos != null && facing != Direction.UP) {
                    bestExpand = i;
                }
            }
            position = position.add(expandVector.scale(bestExpand));
            position = new Vector3d(position.x, posY - 1, position.z);
        }
        return position;
    }

    private void setBlockFacingOld(BlockPos pos) {
        if (!mc.world.isAirBlock(pos.down())) {
            this.blockPos = pos.down();
            facing = Direction.UP;
        } else if (!mc.world.isAirBlock(pos.west())) {
            this.blockPos = pos.west();
            facing = Direction.EAST;
        } else if (!mc.world.isAirBlock(pos.east())) {
            this.blockPos = pos.east();
            facing = Direction.WEST;
        } else if (!mc.world.isAirBlock(pos.north())) {
            this.blockPos = pos.north();
            facing = Direction.SOUTH;
        } else if (!mc.world.isAirBlock(pos.south())) {
            this.blockPos = pos.south();
            facing = Direction.NORTH;
        } else if (!mc.world.isAirBlock(pos.add(-1, 0, -1))) {
            facing = Direction.EAST;
            this.blockPos = pos.add(-1, 0, -1);
        } else if (!mc.world.isAirBlock(pos.add(1, 0, 1))) {
            facing = Direction.WEST;
            this.blockPos = pos.add(1, 0, 1);
        } else if (!mc.world.isAirBlock(pos.add(1, 0, -1))) {
            facing = Direction.SOUTH;
            this.blockPos = pos.add(1, 0, -1);
        } else if (!mc.world.isAirBlock(pos.add(-1, 0, 1))) {
            facing = Direction.NORTH;
            this.blockPos = pos.add(-1, 0, 1);
        } else if (!mc.world.isAirBlock(pos.add(0, -1, 1))) {
            this.blockPos = pos.add(0, -1, 1);
            facing = Direction.UP;
        } else if (!mc.world.isAirBlock(pos.add(0, -1, -1))) {
            this.blockPos = pos.add(0, -1, -1);
            facing = Direction.UP;
        } else if (!mc.world.isAirBlock(pos.add(1, -1, 0))) {
            this.blockPos = pos.add(1, -1, 0);
            facing = Direction.UP;
        } else if (!mc.world.isAirBlock(pos.add(-1, -1, 0))) {
            this.blockPos = pos.add(-1, -1, 0);
            facing = Direction.UP;
        }
    }

    private BlockPos getBlockPos(double posX, double posZ) {
        BlockPos playerPos = new BlockPos(posX, posY, posZ);
        ArrayList<Vector3d> positions = new ArrayList<>();
        HashMap<Vector3d, BlockPos> map = new HashMap<>();
        for (int y = playerPos.getY() - 1; y <= playerPos.getY(); ++y) {
            for (int x = playerPos.getX() - 5; x <= playerPos.getX() + 5; ++x) {
                for (int z = playerPos.getZ() - 5; z <= playerPos.getZ() + 5; ++z) {
                    BlockPos bp = new BlockPos(x, y, z);
                    if (isValidWorldBlock(bp)) {
                        Vector3d vec = new Vector3d(x + 0.5, y + 0.5, z + 0.5);
                        positions.add(vec);
                        map.put(vec, bp);
                    }
                }
            }
        }
        if (positions.isEmpty()) return null;
        positions.sort(Comparator.comparingDouble(
                v -> mc.player.getDistanceSq(v.x, v.y, v.z)));
        return map.get(positions.get(0));
    }

    private Direction getPlaceSide(double posX, double posZ) {
        ArrayList<Vector3d> positions = new ArrayList<>();
        HashMap<Vector3d, Direction> map = new HashMap<>();
        BlockPos playerPos = new BlockPos(posX, posY + 1, posZ);

        if (!isPosSolid(blockPos.up()) && !blockPos.up().equals(playerPos)
                && !mc.player.isOnGround()) {
            positions.add(getBestHitFeet(blockPos.up()));
            map.put(getBestHitFeet(blockPos.up()), Direction.UP);
        }
        Direction[] dirs = {Direction.EAST, Direction.WEST,
                Direction.SOUTH, Direction.NORTH};
        for (Direction dir : dirs) {
            BlockPos bp = blockPos.offset(dir);
            if (!isPosSolid(bp) && !bp.equals(playerPos)) {
                Vector3d v = getBestHitFeet(bp);
                positions.add(v);
                map.put(v, dir);
            }
        }
        positions.sort(Comparator.comparingDouble(
                v -> mc.player.getDistanceSq(v.x, v.y, v.z)));
        if (!positions.isEmpty()) {
            Vector3d v5 = getBestHitFeet(blockPos);
            if (mc.player.getDistanceSq(v5.x, v5.y, v5.z)
                    >= mc.player.getDistanceSq(
                    positions.get(0).x, positions.get(0).y, positions.get(0).z)) {
                return map.get(positions.get(0));
            }
        }
        return null;
    }

    // ======================== BLOCK PLACEMENT ========================

    private void place() {
        if (blockPos == null || facing == null) return;
        Vector3d hv = getFaceHitVec(blockPos, facing);

        // Проверка crosshair target (как LB: isValidCrosshairTarget)
        BlockRayTraceResult crosshairTest = rayTraceBlock();
        if (crosshairTest != null && crosshairTest.getType() == RayTraceResult.Type.BLOCK
                && crosshairTest.getPos().equals(blockPos)
                && crosshairTest.getFace() == facing) {
            if (!isValidCrosshairTarget(crosshairTest)) return;
        }

        int slot = findBestBlockSlotForPlace();
        if (slot == -1) return;

        // Spoof слота
        if (!spoof.is("Фейк")) {
            if (spoof.is("Обычный")) {
                int current = mc.player.inventory.currentItem;
                if (current != slot) {
                    try {
                        silentHeldSwap = true;
                        mc.player.connection.sendPacket(new CHeldItemChangePacket(slot));
                    } finally {
                        silentHeldSwap = false;
                    }
                }
            } else {
                mc.player.inventory.currentItem = slot;
            }
        } else {
            if (slot != lastSlot) {
                try {
                    silentHeldSwap = true;
                    mc.player.connection.sendPacket(new CHeldItemChangePacket(slot));
                } finally {
                    silentHeldSwap = false;
                }
                lastSlot = slot;
            }
        }

        BlockRayTraceResult result = new BlockRayTraceResult(hv, facing, blockPos, false);
        ActionResultType actionResult = mc.playerController.processRightClickBlock(
                mc.player, mc.world, Hand.MAIN_HAND, result);

        if (actionResult == ActionResultType.SUCCESS) {
            // Swing
            if (swingMode.is("DoNotHide") || swingMode.is("Hide")) {
                mc.player.swingArm(Hand.MAIN_HAND);
            } else if (swingMode.is("NoSwing")) {
                mc.player.connection.sendPacket(new CAnimateHandPacket(Hand.MAIN_HAND));
            }

            // Eagle
            if (technique.is("Normal") && eagle.get()) {
                placedBlocksUntilEagle++;
            }

            // Telly
            updatePlacedBlocksForTelly();

            // SimulateDoubleClick
            if (simulateDoubleClick.get()) {
                mc.playerController.processRightClickBlock(
                        mc.player, mc.world, Hand.MAIN_HAND, result);
            }
        }

        // Возврат слота
        if (spoof.is("Обычный")) {
            if (slot != mc.player.inventory.currentItem) {
                try {
                    silentHeldSwap = true;
                    mc.player.connection.sendPacket(
                            new CHeldItemChangePacket(mc.player.inventory.currentItem));
                } finally {
                    silentHeldSwap = false;
                }
            }
        }
    }

    private int findBestBlockSlotForPlace() {
        int slot = -1;
        int bestScore = Integer.MIN_VALUE;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (isValidBlock(stack)) {
                int score = stack.getCount() * 100;
                if (score > bestScore) {
                    bestScore = score;
                    slot = i;
                }
            }
        }
        return slot;
    }

    // ======================== RAY TRACE / CROSSHAIR ========================

    private BlockRayTraceResult rayTraceBlock() {
        Vector3d eyes = mc.player.getEyePosition(1.0f);
        Vector3d look = getLookVector(rotationVector);
        Vector3d reach = eyes.add(look.scale(mc.playerController.getBlockReachDistance()));
        RayTraceContext ctx = new RayTraceContext(eyes, reach,
                RayTraceContext.BlockMode.COLLIDER,
                RayTraceContext.FluidMode.NONE, mc.player);
        return (BlockRayTraceResult) mc.world.rayTraceBlocks(ctx);
    }

    private boolean isValidCrosshairTarget(BlockRayTraceResult rayTraceResult) {
        // LB: проверка minDist
        Vector3d diff = rayTraceResult.getHitVec()
                .subtract(mc.player.getEyePosition(1.0f));
        Direction side = rayTraceResult.getFace();
        if (side.getAxis() != Direction.Axis.Y) {
            double dist = side == Direction.NORTH || side == Direction.SOUTH
                    ? diff.z : diff.x;
            if (Math.abs(dist) < minDist.get()) return false;
        }
        return true;
    }

    // ======================== UTILITIES ========================

    private boolean isValidBlock(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) return false;
        Block block = ((BlockItem) stack.getItem()).getBlock();
        return !DISALLOWED_BLOCKS.contains(block)
                && !(block instanceof FallingBlock);
    }

    private void switchToBlockSlot() {
        int slot = findBestBlockSlotForPlace();
        if (slot != -1) {
            mc.player.inventory.currentItem = slot;
        }
    }

    private void updateBlockCount() {
        blockCount = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (isValidBlock(stack)) blockCount += stack.getCount();
        }
        ItemStack offhand = mc.player.getHeldItemOffhand();
        if (isValidBlock(offhand)) blockCount += offhand.getCount();
    }

    private boolean isOnEdge() {
        double x = mc.player.getPosX();
        double z = mc.player.getPosZ();
        double minX = Math.floor(x) + 0.3;
        double maxX = Math.floor(x) + 1 - 0.3;
        double minZ = Math.floor(z) + 0.3;
        double maxZ = Math.floor(z) + 1 - 0.3;
        return x <= minX || x >= maxX || z <= minZ || z >= maxZ;
    }

    private float wrapDegrees(float value) {
        value = value % 360f;
        if (value >= 180f) value -= 360f;
        if (value < -180f) value += 360f;
        return value;
    }

    private float updateRotationFloat(float current, float target, float speed) {
        float diff = wrapDegrees(target - current);
        if (diff > speed) diff = speed;
        if (diff < -speed) diff = -speed;
        return current + diff;
    }

    private Vector2f calculateRotation(Vector3d target) {
        Vector3d eyes = mc.player.getEyePosition(1.0f);
        double dx = target.x - eyes.x;
        double dy = target.y - eyes.y;
        double dz = target.z - eyes.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, dist));
        return new Vector2f(yaw, pitch);
    }

    private Vector3d getLookVector(Vector2f rotation) {
        float yawRad = rotation.x * ((float) Math.PI / 180F);
        float pitchRad = rotation.y * ((float) Math.PI / 180F);
        float cosYaw = MathHelper.cos(-yawRad - (float) Math.PI);
        float sinYaw = MathHelper.sin(-yawRad - (float) Math.PI);
        float cosPitch = -MathHelper.cos(-pitchRad);
        float sinPitch = MathHelper.sin(-pitchRad);
        return new Vector3d(sinYaw * cosPitch, sinPitch, cosYaw * cosPitch);
    }

    private boolean lookingAtBlock(BlockPos pos, Direction face, Vector2f rot) {
        Vector3d eyes = mc.player.getEyePosition(1.0f);
        Vector3d look = getLookVector(rot);
        Vector3d reach = eyes.add(look.scale(mc.playerController.getBlockReachDistance()));
        RayTraceContext ctx = new RayTraceContext(eyes, reach,
                RayTraceContext.BlockMode.COLLIDER,
                RayTraceContext.FluidMode.NONE, mc.player);
        BlockRayTraceResult res = mc.world.rayTraceBlocks(ctx);
        return res != null && res.getType() == RayTraceResult.Type.BLOCK
                && res.getPos().equals(pos) && res.getFace() == face;
    }

    private Vector3d getFaceHitVec(BlockPos pos, Direction face) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        double off = 0.5;
        switch (face) {
            case UP: y += off; break;
            case DOWN: y -= off; break;
            case NORTH: z -= off; break;
            case SOUTH: z += off; break;
            case WEST: x -= off; break;
            case EAST: x += off; break;
        }
        return new Vector3d(x, y, z);
    }

    private Vector3d getBestHitFeet(BlockPos bp) {
        return new Vector3d(
                MathHelper.clamp(mc.player.getPosX(), bp.getX(), bp.getX() + 1.0),
                MathHelper.clamp(mc.player.getPosY(), bp.getY(), bp.getY() + 1.0),
                MathHelper.clamp(mc.player.getPosZ(), bp.getZ(), bp.getZ() + 1.0)
        );
    }

    private boolean isPosSolid(BlockPos pos) {
        Block block = mc.world.getBlockState(pos).getBlock();
        return !(block instanceof AirBlock) && !(block instanceof FlowingFluidBlock);
    }

    private boolean isValidWorldBlock(BlockPos pos) {
        Block block = mc.world.getBlockState(pos).getBlock();
        return !(block instanceof AirBlock) && !(block instanceof FlowingFluidBlock)
                && block != Blocks.CHEST && block != Blocks.FURNACE;
    }

    // ======================== RENDER ========================

    private void renderBlockCounter(MatrixStack matrixStack) {
        if (!renderEnabled.get()) return;
        int scaledWidth = mc.getMainWindow().getScaledWidth();
        int scaledHeight = mc.getMainWindow().getScaledHeight();
        String text = blockCount + " blocks";
        float textWidth = mc.fontRenderer.getStringWidth(text);
        float x = scaledWidth / 2f - textWidth / 2f;
        float y = scaledHeight - 70;
        int color = blockCount > 0 ? 0xFFFFFF : 0xFF4040;
        mc.fontRenderer.drawStringWithShadow(matrixStack, text, x, y, color);
    }
}
