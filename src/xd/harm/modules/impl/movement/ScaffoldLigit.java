package xd.harm.modules.impl.movement;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.Getter;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.FlowingFluidBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CAnimateHandPacket;
import net.minecraft.network.play.client.CHeldItemChangePacket;
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
import xd.harm.events.input.EventInput;
import xd.harm.events.movement.EventMotion;
import xd.harm.events.movement.SprintEvent;
import xd.harm.events.network.EventPacket;
import xd.harm.events.render.EventDisplay;
import xd.harm.events.world.EventUpdate;
import xd.harm.events.movement.StrafeEvent;
import xd.harm.Harmony;
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

@ModuleRegister(name = "Scaffold", category = Category.Movement, desc = "Автоматически ставит под вас блоки")
public class ScaffoldLigit extends Module {

    private static final List<Block> INVALID_BLOCKS = Arrays.asList(
            Blocks.ENCHANTING_TABLE, Blocks.FURNACE, Blocks.CRAFTING_TABLE,
            Blocks.TRAPPED_CHEST, Blocks.CHEST, Blocks.DISPENSER, Blocks.AIR, Blocks.WATER,
            Blocks.LAVA, Blocks.SAND, Blocks.SNOW,
            Blocks.TORCH, Blocks.ANVIL, Blocks.JUKEBOX, Blocks.STONE_BUTTON, Blocks.OAK_BUTTON,
            Blocks.LEVER, Blocks.NOTE_BLOCK, Blocks.STONE_PRESSURE_PLATE, Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE,
            Blocks.OAK_PRESSURE_PLATE, Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE, Blocks.RED_MUSHROOM,
            Blocks.BROWN_MUSHROOM, Blocks.DANDELION, Blocks.POPPY, Blocks.GLASS_PANE,
            Blocks.IRON_BARS, Blocks.CACTUS, Blocks.LADDER, Blocks.COBWEB, Blocks.PUMPKIN
    );

    // === Категории (заголовки группировки) ===

    private final CategorySetting catRotation = new CategorySetting("РОТАЦИИ");
    private final CategorySetting catMovement = new CategorySetting("ДВИЖЕНИЕ");
    private final CategorySetting catTower = new CategorySetting("ТАУЭР");
    private final CategorySetting catPlace = new CategorySetting("УСТАНОВКА");
    private final CategorySetting catTimer = new CategorySetting("ТАЙМЕР");
    private final CategorySetting catAdvanced = new CategorySetting("ДОПОЛНИТЕЛЬНО");

    // === РОТАЦИИ ===

    private final ModeSetting rotations = new ModeSetting("Ротации", "Static",
            "Static yaw", "Static yaw god", "Static god", "Static", "Polar", "Intave", "God", "Keep",
            "Snap", "Direct", "Rewinside", "Выкл");

    private final BooleanSetting moveFix = new BooleanSetting("Исправление движения", false)
            .setVisible(() -> !rotations.is("Выкл"));

    // === ДВИЖЕНИЕ ===

    private final ModeSetting scaffoldMode = (ModeSetting) new ModeSetting("Режим", "Normal",
            "Normal", "Telly", "GodBridge", "Down", "Expand").setSticky(true);

    // GodBridge sub-settings (только для режима GodBridge в Режиме строительства)
    private final SliderSetting godBridgePitch = new SliderSetting("Питч год бриджа", 75f, 60f, 90f, 1f)
            .setVisible(() -> scaffoldMode.is("GodBridge"));
    private final BooleanSetting customGodPitch = new BooleanSetting("Свой питч", false)
            .setVisible(() -> scaffoldMode.is("GodBridge"));

    private final ModeSetting sprintMode = new ModeSetting("Спринт", "Всегда",
            "Всегда", "Выкл", "Легит", "Скорость в воздухе", "Переключение", "Переключение без пакета", "Без пакета");

    private final BooleanSetting safeWalk = new BooleanSetting("Безопасная ходьба", true);
    private final BooleanSetting sneak = new BooleanSetting("Скрытие", false)
            .setVisible(() -> !safeWalk.get());
    private final SliderSetting sneakDelay = new SliderSetting("Задержка скрытия", 4f, 1f, 20f, 1f)
            .setVisible(() -> sneak.get());
    private final SliderSetting sneakTime = new SliderSetting("Время скрытия", 2f, 1f, 6f, 1f)
            .setVisible(() -> sneak.get());
    private final BooleanSetting jump = new BooleanSetting("Прыжок", false);
    private final BooleanSetting adStrafe = new BooleanSetting("A D стрейф", false);
    private final SliderSetting expand = new SliderSetting("Расширение", 0f, 0f, 8f, 1f)
            .setVisible(() -> scaffoldMode.is("Expand"));

    // === ТАУЭР ===

    private final ModeSetting towerMode = new ModeSetting("Тауэр", "Выкл", "Выкл", "Таймер", "Intave");

    // === УСТАНОВКА ===

    private final ModeSetting spoof = new ModeSetting("Спуф слот", "Выкл", "Выкл", "Обычный", "Фейк");
    private final BooleanSetting swing = new BooleanSetting("Размах руки", true);
    private final BooleanSetting switchBack = new BooleanSetting("Вернуть слот", true);
    private final BooleanSetting dragClick = new BooleanSetting("Drag клик", false);
    private final BooleanSetting sneakWhenPlace = new BooleanSetting("Скрываться при установке", false);

    // === ТАЙМЕР (встроенный) ===

    private final BooleanSetting timerEnabled = new BooleanSetting("Включить таймер", false);
    private final ModeSetting timerMode = new ModeSetting("Режим", "Постоянный",
            "Постоянный", "Прерывистый", "Движение", "Прыжок")
            .setVisible(() -> timerEnabled.get());
    private final SliderSetting timerSpeed = new SliderSetting("Скорость", 1.5f, 0.1f, 10f, 0.1f)
            .setVisible(() -> timerEnabled.get());
    private final SliderSetting timerSlowSpeed = new SliderSetting("Медленная скорость", 0.5f, 0.1f, 1f, 0.05f)
            .setVisible(() -> timerEnabled.get() && timerMode.is("Прерывистый"));
    private final SliderSetting timerFastTime = new SliderSetting("Время ускорения", 200f, 50f, 1000f, 50f)
            .setVisible(() -> timerEnabled.get() && timerMode.is("Прерывистый"));
    private final SliderSetting timerSlowTime = new SliderSetting("Время замедления", 200f, 50f, 1000f, 50f)
            .setVisible(() -> timerEnabled.get() && timerMode.is("Прерывистый"));
    private final BooleanSetting timerOnGround = new BooleanSetting("Только на земле", false)
            .setVisible(() -> timerEnabled.get() && timerMode.is("Прыжок"));
    private final BooleanSetting timerStopHurt = new BooleanSetting("Стоп при уроне", false)
            .setVisible(() -> timerEnabled.get());
    private final BooleanSetting timerStopWater = new BooleanSetting("Стоп в воде", false)
            .setVisible(() -> timerEnabled.get());

    // === ДОПОЛНИТЕЛЬНО (перенесённые из LiquidBounce / rinbounce) ===

    // --- Eagle (присед на краю) ---
    private final BooleanSetting eagle = new BooleanSetting("Орёл", false);
    private final ModeSetting eagleMode = new ModeSetting("Режим орла", "Normal", "Normal", "Sneak")
            .setVisible(() -> eagle.get());
    private final SliderSetting eagleSpeed = new SliderSetting("Скорость орла", 1f, 0.1f, 3f, 0.1f)
            .setVisible(() -> eagle.get());
    private final SliderSetting eagleEdgeDistance = new SliderSetting("Дистанция края", 0.5f, 0.1f, 1f, 0.05f)
            .setVisible(() -> eagle.get());
    private final SliderSetting blocksToEagle = new SliderSetting("Блоков до орла", 0f, 0f, 10f, 1f)
            .setVisible(() -> eagle.get());
    private final BooleanSetting eagleSprint = new BooleanSetting("Спринт орла", false)
            .setVisible(() -> eagle.get());

    // --- Telly-настройки (видны только в режиме Telly) ---
    private final BooleanSetting jumpAutomatically = new BooleanSetting("Авто-прыжок", true)
            .setVisible(() -> scaffoldMode.is("Telly"));
    private final SliderSetting blocksToJumpMin = new SliderSetting("Мин. блоков до прыжка", 3f, 1f, 20f, 1f)
            .setVisible(() -> scaffoldMode.is("Telly") && jumpAutomatically.get());
    private final SliderSetting blocksToJumpMax = new SliderSetting("Макс. блоков до прыжка", 5f, 1f, 20f, 1f)
            .setVisible(() -> scaffoldMode.is("Telly") && jumpAutomatically.get());
    private final BooleanSetting jumpOnUserInput = new BooleanSetting("Прыжок по вводу", false)
            .setVisible(() -> scaffoldMode.is("Telly"));
    private final BooleanSetting airSafe = new BooleanSetting("Безопасность в воздухе", false)
            .setVisible(() -> scaffoldMode.is("Telly"));

    // --- SameY (сохранение высоты) ---
    private final BooleanSetting sameY = new BooleanSetting("Та же Y", false);
    private final ModeSetting sameYMode = new ModeSetting("Режим Y", "On", "On", "Off", "Falling", "Hypixel")
            .setVisible(() -> sameY.get());

    // --- Zitter (анти-аим дрожание) ---
    private final BooleanSetting zitter = new BooleanSetting("Дрожание", false);
    private final ModeSetting zitterMode = new ModeSetting("Режим дрожания", "Smooth", "Smooth", "Teleport", "Off")
            .setVisible(() -> zitter.get());
    private final SliderSetting zitterSpeed = new SliderSetting("Скорость дрожания", 0.1f, 0.05f, 1f, 0.05f)
            .setVisible(() -> zitter.get() && !zitterMode.is("Off"));
    private final SliderSetting zitterStrength = new SliderSetting("Сила дрожания", 0.05f, 0.01f, 0.5f, 0.01f)
            .setVisible(() -> zitter.get() && !zitterMode.is("Off"));

    // --- AutoBlock / Silent расширения ---
    private final BooleanSetting earlySwitch = new BooleanSetting("Ранняя смена", false);
    private final SliderSetting switchAmount = new SliderSetting("Кол-во смен", 1f, 1f, 10f, 1f)
            .setVisible(() -> earlySwitch.get());
    private final SliderSetting slotResetDelay = new SliderSetting("Задержка сброса слота", 5f, 1f, 20f, 1f)
            .setVisible(() -> !spoof.is("Выкл"));
    private final BooleanSetting trackCPS = new BooleanSetting("Считать CPS", false);
    private final BooleanSetting extraClicks = new BooleanSetting("Доп клики", false);
    private final SliderSetting extraClickCPS = new SliderSetting("CPS доп кликов", 5f, 1f, 20f, 1f)
            .setVisible(() -> extraClicks.get());
    private final BooleanSetting simulateDoubleClick = new BooleanSetting("Двойной клик", false);

    // --- SpeedLimit / Slow ---
    private final BooleanSetting speedLimiter = new BooleanSetting("Ограничитель скорости", false);
    private final SliderSetting speedLimit = new SliderSetting("Лимит скорости", 1f, 0.1f, 5f, 0.1f)
            .setVisible(() -> speedLimiter.get());
    private final BooleanSetting slow = new BooleanSetting("Замедление", false);
    private final SliderSetting slowSpeed = new SliderSetting("Скорость замедления", 0.5f, 0.1f, 1f, 0.05f)
            .setVisible(() -> slow.get());
    private final BooleanSetting slowOnlyGround = new BooleanSetting("Замедл. только на земле", true)
            .setVisible(() -> slow.get());

    // --- JumpStrafe ---
    private final BooleanSetting jumpStrafe = new BooleanSetting("Стрейф прыжка", false);
    private final SliderSetting jumpStraightStrafe = new SliderSetting("Прямой стрейф", 0.5f, 0f, 1f, 0.01f)
            .setVisible(() -> jumpStrafe.get());
    private final SliderSetting jumpDiagonalStrafe = new SliderSetting("Диагональный стрейф", 0.5f, 0f, 1f, 0.01f)
            .setVisible(() -> jumpStrafe.get());
    private final SliderSetting jumpTicks = new SliderSetting("Тики прыжка", 3f, 1f, 10f, 1f)
            .setVisible(() -> jumpStrafe.get());

    // --- Прочие ротации/установка ---
    private final BooleanSetting useOptimizedPitch = new BooleanSetting("Оптим. питч", false);
    private final BooleanSetting waitForRotations = new BooleanSetting("Ждать ротации", false);
    private final SliderSetting minDist = new SliderSetting("Мин. дистанция", 0f, 0f, 5f, 0.1f);
    private final BooleanSetting allowClutching = new BooleanSetting("Клатч", false);
    private final BooleanSetting stabilized = new BooleanSetting("Стабилизация", false);
    private final BooleanSetting smoothPredictive = new BooleanSetting("Плавный прогноз", false);

    // --- rinbounce уникальное ---
    private final BooleanSetting reverseYaw = new BooleanSetting("Обратный yaw", false);
    private final BooleanSetting hmcBlink = new BooleanSetting("HMC Blink", false);

    // === Состояние ===

    private final TimerUtility placeTimer = TimerUtility.create();
    private final TimerUtility sneakTimer = TimerUtility.create();
    private final TimerUtility adStrafeTimer = TimerUtility.create();
    private final TimerUtility pulseTimer = TimerUtility.create();
    private final TimerUtility eagleTimer = TimerUtility.create();
    private final TimerUtility zitterTimer = TimerUtility.create();
    private final TimerUtility extraClickTimer = TimerUtility.create();
    private final TimerUtility slotResetTimer = TimerUtility.create();
    private boolean silentHeldSwap = false;
    private boolean pulseFast = true;

    // Eagle-состояние
    private boolean eagleSneaking;
    private int placedBlocksUntilEagle;

    // Zitter-состояние
    private boolean zitterDirection;

    // Telly-состояние (как в LiquidBounce)
    private int blocksPlacedSinceJump;
    private int ticksUntilJump;

    // SameY-состояние
    private int launchY;

    // Slot reset
    private boolean waitingSlotReset;

    private Vector2f rotationVector = new Vector2f(0, 0);
    private float lastYaw = 0;
    private float lastPitch = 0;

    @Getter
    private int blockCount = 0;

    private int posY, spoofSlot, lastSlot, itemBefore, ticksExisted, ticks;
    private boolean adStrafeDirection;
    private float scaffoldYaw, scaffoldPitch;
    private BlockPos blockPos;
    private Direction facing;

    // === Конструктор ===

    public ScaffoldLigit() {
        addSettings(
                // ===== РОТАЦИИ =====
                catRotation, rotations, moveFix, godBridgePitch, customGodPitch,
                // ===== ДВИЖЕНИЕ =====
                catMovement, scaffoldMode, sprintMode, safeWalk, sneak, sneakDelay, sneakTime, jump, adStrafe, expand,
                // ===== ТАУЭР =====
                catTower, towerMode,
                // ===== УСТАНОВКА =====
                catPlace, spoof, swing, switchBack, dragClick, sneakWhenPlace,
                // ===== ТАЙМЕР =====
                catTimer, timerEnabled, timerMode, timerSpeed, timerSlowSpeed, timerFastTime,
                timerSlowTime, timerOnGround, timerStopHurt, timerStopWater,
                // ===== ДОПОЛНИТЕЛЬНО =====
                catAdvanced,
                // Eagle
                eagle, eagleMode, eagleSpeed, eagleEdgeDistance, blocksToEagle, eagleSprint,
                // Telly (настройки режима Telly)
                jumpAutomatically, blocksToJumpMin, blocksToJumpMax, jumpOnUserInput, airSafe,
                // SameY
                sameY, sameYMode,
                // Zitter
                zitter, zitterMode, zitterSpeed, zitterStrength,
                // AutoBlock расширения
                earlySwitch, switchAmount, slotResetDelay, trackCPS, extraClicks, extraClickCPS, simulateDoubleClick,
                // SpeedLimit / Slow
                speedLimiter, speedLimit, slow, slowSpeed, slowOnlyGround,
                // JumpStrafe
                jumpStrafe, jumpStraightStrafe, jumpDiagonalStrafe, jumpTicks,
                // Прочие ротации/установка
                useOptimizedPitch, waitForRotations, minDist, allowClutching, stabilized, smoothPredictive,
                // rinbounce
                reverseYaw, hmcBlink
        );
    }

    // === OnEnable / OnDisable ===

    @Override
    public boolean onEnable() {
        super.onEnable();
        lastSlot = mc.player.inventory.currentItem;
        itemBefore = mc.player.inventory.currentItem;
        lastYaw = mc.player.rotationYaw - 180f;
        lastPitch = rotations.is("Polar") ? 82f : 90f;
        posY = (int) (mc.player.getPosY() - 1);
        spoofSlot = mc.player.inventory.currentItem;
        scaffoldYaw = mc.player.rotationYaw - 180f;
        scaffoldPitch = rotations.is("Polar") ? 82f : 90f;
        rotationVector = new Vector2f(mc.player.rotationYaw, mc.player.rotationPitch);
        // Инициализация новых состояний
        launchY = (int) mc.player.getPosY();
        eagleSneaking = false;
        placedBlocksUntilEagle = 0;
        blocksPlacedSinceJump = 0;
        ticksUntilJump = randomBlocksToJump();
        zitterDirection = false;
        waitingSlotReset = false;
        eagleTimer.reset();
        zitterTimer.reset();
        extraClickTimer.reset();
        slotResetTimer.reset();
        updateBlockCount();
        placeTimer.reset();
        pulseTimer.reset();
        pulseFast = true;
        sprint();
        processBlockData();
        rotations();
        return false;
    }

    @Override
    public boolean onDisable() {
        if (switchBack.get() && spoof.is("Выкл")) {
            mc.player.inventory.currentItem = itemBefore;
        }
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
        // Сброс встроенного таймера (если отдельный модуль Timer выключен)
        TimerModule standaloneTimer = Harmony.getInstance().getModuleManager().getTimer();
        if (!standaloneTimer.isState()) {
            mc.timer.timerSpeed = 1.0f;
        }
        return super.onDisable();
    }

    // === Event Handlers ===

    @Subscribe
    public void onMotion(EventMotion e) {
        if (!rotations.is("Выкл")) {
            e.setYaw(rotationVector.x);
            e.setPitch(rotationVector.y);
            mc.player.renderYawOffset = rotationVector.x;
            mc.player.rotationYawHead = rotationVector.x;
            mc.player.rotationPitchHead = rotationVector.y;
        }
        // Tower
        if (towerMode.is("Таймер")) {
            if (mc.gameSettings.keyBindJump.isKeyDown()) {
                placeTimer.reset();
            }
        } else if (towerMode.is("Intave")) {
            if (mc.gameSettings.keyBindJump.isKeyDown() && mc.player.isOnGround()) {
                mc.player.setMotion(mc.player.getMotion().x, 0.41D, mc.player.getMotion().z);
            }
        }
        sprint();
    }

    @Subscribe
    public void onDisplay(EventDisplay e) {
        if (e.getType() != EventDisplay.Type.POST) return;
        renderBlockCounter(e.getMatrixStack());
    }

    @Subscribe
    public void onInput(EventInput eventInput) {
        // Скрытие (sneak) по таймеру
        if (sneak.get() && !safeWalk.get()) {
            if (sneakTimer.isReached((long) (sneakDelay.get() * 100))) {
                eventInput.setSneak(true);
                if (sneakTimer.isReached((long) ((sneakDelay.get() * 100) + (sneakTime.get() * 50)))) {
                    sneakTimer.reset();
                }
            }
        }
        // Безопасная ходьба (safe walk) — нажимает Shift на краю
        if (safeWalk.get() && mc.player.isOnGround()) {
            BlockPos below = new BlockPos(mc.player.getPosX() + mc.player.getMotion().x,
                    mc.player.getPosY() - 1.0, mc.player.getPosZ() + mc.player.getMotion().z);
            if (mc.world.getBlockState(below).getBlock() instanceof AirBlock) {
                eventInput.setSneak(true);
            }
        }
        // A/D стрейф — автоматически переключает A/D для лучшего обхода
        if (adStrafe.get()) {
            if (adStrafeTimer.isReached(125)) {
                adStrafeDirection = !adStrafeDirection;
                adStrafeTimer.reset();
            }
            if (MoveUtils.isMoving() && !mc.gameSettings.keyBindLeft.isKeyDown() && !mc.gameSettings.keyBindRight.isKeyDown()) {
                eventInput.setStrafe(adStrafeDirection ? 1.0f : -1.0f);
            }
        }

        // Eagle (Орёл) — приседание на краю блока
        if (eagle.get() && mc.player.isOnGround() && MoveUtils.isMoving()) {
            // Не орлим, пока ждем нужного кол-ва блоков
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
    }

    // Проверка, стоит ли игрок на краю блока
    private boolean isOnEdge() {
        double playerX = mc.player.getPosX();
        double playerZ = mc.player.getPosZ();
        double minX = Math.floor(playerX) + eagleEdgeDistance.get();
        double maxX = Math.floor(playerX) + 1 - eagleEdgeDistance.get();
        double minZ = Math.floor(playerZ) + eagleEdgeDistance.get();
        double maxZ = Math.floor(playerZ) + 1 - eagleEdgeDistance.get();
        return playerX <= minX || playerX >= maxX || playerZ <= minZ || playerZ >= maxZ;
    }

    // === Telly — как в LiquidBounce: строит блоки + автоматически прыгает каждые N блоков ===
    // Логика: после каждой установки блока blocksPlacedSinceJump++.
    // Когда счётчик >= выбранного случайного значения из диапазона → tryJump().
    // blocksToJump — диапазон (min/max), новое случайное значение выбирается после каждого прыжка.

    // Генерирует случайное число блоков между прыжками из диапазона
    private int randomBlocksToJump() {
        int min = blocksToJumpMin.getInt();
        int max = blocksToJumpMax.getInt();
        if (max < min) max = min;
        return min + (int) (Math.random() * (max - min + 1));
    }

    // Пытается совершить прыжок (как LiquidBounce tryJump)
    private void tryJump() {
        if (!mc.player.isOnGround()) return;

        // AirSafe — не прыгать если под нами пустота (проверяем блок прямо под ногами)
        if (airSafe.get()) {
            BlockPos below = new BlockPos(mc.player.getPosX(), mc.player.getPosY() - 1, mc.player.getPosZ());
            if (mc.world.getBlockState(below).getBlock() instanceof AirBlock) {
                return;
            }
        }

        mc.player.jump();
        blocksPlacedSinceJump = 0;
        // Новое случайное число блоков до следующего прыжка
        ticksUntilJump = randomBlocksToJump();
    }

    // Вызывается после успешной установки блока — увеличивает счётчик
    private void updatePlacedBlocksForTelly() {
        if (!scaffoldMode.is("Telly")) return;

        blocksPlacedSinceJump++;

        // Авто-прыжок: когда поставлено достаточно блоков — прыгаем
        if (jumpAutomatically.get() && blocksPlacedSinceJump >= ticksUntilJump) {
            tryJump();
        }
    }

    // Вызывается в onUpdate каждый тик (как LiquidBounce onUpdate)
    private void handleTelly() {
        if (!scaffoldMode.is("Telly")) return;

        // Обновляем posY для Telly — чтобы корректно ставить блоки после прыжка
        if (mc.player.isOnGround()) {
            posY = (int) (mc.player.getPosY() - 1);
        }

        // Прыжок по вводу: прыжок когда игрок нажимает прыжок (проверяем каждый тик)
        if (jumpOnUserInput.get() && mc.gameSettings.keyBindJump.isKeyDown()) {
            if (mc.player.isOnGround()) {
                mc.player.jump();
            }
        }
    }

    @Subscribe
    public void onStrafe(StrafeEvent e) {
        if (moveFix.get() && !rotations.is("Выкл")) {
            // Используем fixMovement из harmony вместо silentMoveFix
            float yaw = rotationVector.x;
            float forward = mc.player.movementInput.moveForward;
            float strafe = mc.player.movementInput.moveStrafe;
            if (forward != 0 || strafe != 0) {
                float closestForward = 0.0F;
                float closestStrafe = 0.0F;
                float closestDifference = Float.MAX_VALUE;
                float clientYaw = mc.player.rotationYaw;
                double angle = MathHelper.wrapDegrees(Math.toDegrees(MoveUtils.direction(clientYaw, forward, strafe)));
                for (float predictedForward = -1.0F; predictedForward <= 1.0F; predictedForward += 1.0F) {
                    for (float predictedStrafe = -1.0F; predictedStrafe <= 1.0F; predictedStrafe += 1.0F) {
                        if (predictedStrafe == 0 && predictedForward == 0) continue;
                        double predictedAngle = MathHelper.wrapDegrees(Math.toDegrees(MoveUtils.direction(yaw, predictedForward, predictedStrafe)));
                        double difference = Math.abs(angle - predictedAngle);
                        if (difference < closestDifference) {
                            closestDifference = (float) difference;
                            closestForward = predictedForward;
                            closestStrafe = predictedStrafe;
                        }
                    }
                }
                mc.player.movementInput.moveForward = closestForward;
                mc.player.movementInput.moveStrafe = closestStrafe;
            }
        }
    }

    @Subscribe
    public void onSendPacket(EventPacket e) {
        if (e.isSend() && e.getPacket() instanceof CHeldItemChangePacket && spoof.is("Фейк") && !silentHeldSwap) {
            e.cancel();
        }
    }

    @Subscribe
    public void onSprint(SprintEvent e) {
        // Режим "Выкл" — гасим любой запуск спринта (включая авто-спринт Minecraft),
        // чтобы Scaffold реально держал игрока без спринта.
        if (sprintMode.is("Выкл")) {
            e.cancel();
        }
    }

    @Subscribe
    public void onUpdate(EventUpdate e) {
        // Поиск блока в hotbar
        ItemStack mainHand = mc.player.getHeldItemMainhand();
        if (mainHand.isEmpty() || !(mainHand.getItem() instanceof BlockItem) || !isValidBlock(mainHand)) {
            switchToBlockSlot();
        }
        updateBlockCount();

        // Отслеживание posY
        if (jump.get()) {
            if (mc.gameSettings.keyBindJump.isKeyDown()) {
                posY = (int) (mc.player.getPosY() - 1);
            }
        } else {
            posY = (int) (mc.player.getPosY() - 1);
        }

        // Авто-прыжок
        if (jump.get()) {
            if (MoveUtils.isMoving() && mc.player.isOnGround() && !mc.gameSettings.keyBindJump.isKeyDown()) {
                mc.player.jump();
            }
        }

        // Telly — обработка авто-прыжка каждые N блоков
        handleTelly();

        // Обработка и ротация
        ticksExisted = mc.player.ticksExisted;
        processBlockData();
        rotations();

        if (dragClick.get()) {
            fakeClick();
        }

        // WaitForRotations — не ставить блок пока ротации не достигнут цели
        if (waitForRotations.get() && !rotations.is("Выкл") && blockPos != null) {
            if (!lookingAtBlock(blockPos, facing, rotationVector)) {
                return;
            }
        }

        // MinDist — не ставить если позиция блока слишком далеко
        if (minDist.get() > 0 && blockPos != null) {
            double dist = Math.sqrt(mc.player.getDistanceSq(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5));
            if (dist < minDist.get()) {
                return;
            }
        }

        int placeDelay = mc.gameSettings.keyBindJump.isKeyDown() && towerMode.is("Таймер") ? 0 : 50;
        if (placeTimer.isReached(placeDelay)) {
            place();
            placeTimer.reset();
            updateBlockCount();
        }

        // Встроенный таймер — применяем только если отдельный модуль Timer выключен,
        // иначе модули будут конфликтовать за mc.timer.timerSpeed.
        applyScaffoldTimer();

        // Дополнительные функции (Eagle, Zitter, SameY, SpeedLimiter и т.д.)
        applyAdvancedFeatures();
    }

    private void applyScaffoldTimer() {
        // Отдельный модуль Timer имеет приоритет — если он включен, не вмешиваемся.
        TimerModule standaloneTimer = Harmony.getInstance().getModuleManager().getTimer();
        if (standaloneTimer.isState()) return;

        if (!timerEnabled.get()) {
            mc.timer.timerSpeed = 1.0f;
            return;
        }

        // Условия полного отключения встроенного таймера
        if (timerStopHurt.get() && mc.player.hurtTime > 0) {
            mc.timer.timerSpeed = 1.0f;
            return;
        }
        if (timerStopWater.get() && (mc.player.isInWater() || mc.player.isInLava())) {
            mc.timer.timerSpeed = 1.0f;
            return;
        }

        switch (timerMode.get()) {
            case "Постоянный" -> mc.timer.timerSpeed = timerSpeed.get();
            case "Прерывистый" -> {
                long fast = timerFastTime.getInt();
                long slow = timerSlowTime.getInt();
                if (pulseFast) {
                    mc.timer.timerSpeed = timerSpeed.get();
                    if (pulseTimer.isReached(fast)) {
                        pulseFast = false;
                        pulseTimer.reset();
                    }
                } else {
                    mc.timer.timerSpeed = timerSlowSpeed.get();
                    if (pulseTimer.isReached(slow)) {
                        pulseFast = true;
                        pulseTimer.reset();
                    }
                }
            }
            case "Движение" -> mc.timer.timerSpeed = MoveUtils.isMoving() ? timerSpeed.get() : 1.0f;
            case "Прыжок" -> {
                boolean condition = timerOnGround.get() ? mc.player.isOnGround() : !mc.player.isOnGround();
                mc.timer.timerSpeed = condition ? timerSpeed.get() : 1.0f;
            }
        }
    }

    // === Дополнительная логика (вызывается из onUpdate) ===

    private void applyAdvancedFeatures() {
        // --- SpeedLimiter (ограничение горизонтальной скорости) ---
        if (speedLimiter.get() && MoveUtils.isMoving()) {
            double horizontalSpeed = Math.sqrt(mc.player.getMotion().x * mc.player.getMotion().x
                    + mc.player.getMotion().z * mc.player.getMotion().z);
            if (horizontalSpeed > speedLimit.get()) {
                float factor = speedLimit.get() / (float) horizontalSpeed;
                mc.player.setMotion(mc.player.getMotion().x * factor, mc.player.getMotion().y, mc.player.getMotion().z * factor);
            }
        }

        // --- Slow (замедление) ---
        if (slow.get() && MoveUtils.isMoving()) {
            if (slowOnlyGround.get() && !mc.player.isOnGround()) return;
            double horizontalSpeed = Math.sqrt(mc.player.getMotion().x * mc.player.getMotion().x
                    + mc.player.getMotion().z * mc.player.getMotion().z);
            if (horizontalSpeed > slowSpeed.get()) {
                float factor = slowSpeed.get() / (float) horizontalSpeed;
                mc.player.setMotion(mc.player.getMotion().x * factor, mc.player.getMotion().y, mc.player.getMotion().z * factor);
            }
        }

        // --- Zitter (дрожание / анти-аим jitter) ---
        if (zitter.get() && !zitterMode.is("Off") && MoveUtils.isMoving()) {
            if (zitterMode.is("Smooth")) {
                if (zitterTimer.isReached((long) (zitterSpeed.get() * 100))) {
                    zitterDirection = !zitterDirection;
                    zitterTimer.reset();
                }
                mc.player.setMotion(mc.player.getMotion().x + (zitterDirection ? zitterStrength.get() : -zitterStrength.get()),
                        mc.player.getMotion().y,
                        mc.player.getMotion().z);
            } else if (zitterMode.is("Teleport")) {
                double direction = Math.toRadians(MoveUtils.getDirection());
                double offset = zitterStrength.get() * (zitterDirection ? 1 : -1);
                mc.player.setPosition(mc.player.getPosX() + (-Math.sin(direction) * offset),
                        mc.player.getPosY(),
                        mc.player.getPosZ() + (Math.cos(direction) * offset));
                if (zitterTimer.isReached((long) (zitterSpeed.get() * 100))) {
                    zitterDirection = !zitterDirection;
                    zitterTimer.reset();
                }
            }
        }

        // --- SameY (сохранение высоты) ---
        if (sameY.get()) {
            switch (sameYMode.get()) {
                case "On" -> {
                    if (mc.player.getPosY() < launchY - 0.5) {
                        // Teleport back to launch Y
                        mc.player.setPosition(mc.player.getPosX(), launchY + 0.1, mc.player.getPosZ());
                        mc.player.setMotion(mc.player.getMotion().x, 0, mc.player.getMotion().z);
                    }
                }
                case "Falling" -> {
                    if (mc.player.getPosY() > launchY + 0.5 && mc.player.getMotion().y < 0) {
                        mc.player.setMotion(mc.player.getMotion().x, 0, mc.player.getMotion().z);
                    }
                }
                case "Hypixel" -> {
                    if (mc.player.getPosY() > launchY + 0.1 && mc.player.getMotion().y > 0) {
                        mc.player.setMotion(mc.player.getMotion().x, -0.5, mc.player.getMotion().z);
                    }
                }
            }
        }

        // --- JumpStrafe (стрейф во время прыжка) ---
        if (jumpStrafe.get() && !mc.player.isOnGround()) {
            double yawRad = Math.toRadians(mc.player.rotationYaw);
            boolean straight = mc.player.movementInput.moveStrafe == 0;
            float strafeMult = straight ? jumpStraightStrafe.get() : jumpDiagonalStrafe.get();
            double motionX = -Math.sin(yawRad) * strafeMult;
            double motionZ = Math.cos(yawRad) * strafeMult;
            if (mc.player.movementInput.moveForward != 0) {
                mc.player.setMotion(motionX, mc.player.getMotion().y, motionZ);
            }
        }

        // --- ExtraClicks (дополнительные клики для анти-чита) ---
        if (extraClicks.get()) {
            if (extraClickTimer.isReached((long) (1000 / extraClickCPS.get()))) {
                mc.player.connection.sendPacket(new CAnimateHandPacket(Hand.MAIN_HAND));
                extraClickTimer.reset();
            }
        }
    }

    // === Рендер счетчика блоков ===

    private void renderBlockCounter(MatrixStack matrixStack) {
        int scaledWidth = mc.getMainWindow().getScaledWidth();
        int scaledHeight = mc.getMainWindow().getScaledHeight();
        String text = blockCount + " blocks";
        float textWidth = mc.fontRenderer.getStringWidth(text);
        float x = scaledWidth / 2f - textWidth / 2f;
        float y = scaledHeight - 70;
        int color = blockCount > 0 ? 0xFFFFFF : 0xFF4040;
        mc.fontRenderer.drawStringWithShadow(matrixStack, text, x, y, color);
    }

    // === Утилиты блоков ===

    private boolean isValidBlock(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) return false;
        Block block = ((BlockItem) stack.getItem()).getBlock();
        return !INVALID_BLOCKS.contains(block);
    }

    private int findBlockSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (isValidBlock(stack)) return i;
        }
        return -1;
    }

    private void switchToBlockSlot() {
        int slot = findBlockSlot();
        if (slot != -1) {
            mc.player.inventory.currentItem = slot;
        }
    }

    private void updateBlockCount() {
        blockCount = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (isValidBlock(stack)) {
                blockCount += stack.getCount();
            }
        }
        ItemStack offhand = mc.player.getHeldItemOffhand();
        if (isValidBlock(offhand)) {
            blockCount += offhand.getCount();
        }
    }

    // === Спринт ===

    private void sprint() {
        switch (sprintMode.get()) {
            case "Легит" -> mc.player.setSprinting(MoveUtils.isMoving() && mc.gameSettings.keyBindForward.isKeyDown()
                    && Math.abs(MathHelper.wrapDegrees(mc.player.rotationYaw - rotationVector.x)) < 66.5f);
            case "Выкл" -> mc.player.setSprinting(false);
            case "Всегда" -> {
                if (mc.player.moveForward > 0) mc.player.setSprinting(true);
            }
            case "Скорость в воздухе" -> {
                if (mc.player.ticksExisted % 3 == 0 && MoveUtils.isMoving() && getSpeed3D() < 0.23f
                        && !mc.player.isOnGround() && !mc.gameSettings.keyBindJump.isKeyDown() && mc.player.hurtTime == 0) {
                    MoveUtils.setStrafe(0.25f);
                }
                mc.player.setSprinting(false);
            }
            case "Без пакета" -> mc.player.setSprinting(false);
            case "Переключение" -> mc.player.setSprinting(mc.player.ticksExisted % 2 == 0);
            case "Переключение без пакета" -> {
                // Без отправки пакетов — ничего не делаем
            }
        }
    }

    private double getSpeed3D() {
        double motionX = mc.player.getMotion().x;
        double motionY = mc.player.isOnGround() ? 0.0 : mc.player.getMotion().y;
        double motionZ = mc.player.getMotion().z;
        return Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
    }

    // === Утилиты углов ===

    private float wrapDegrees(float value) {
        value = value % 360.0f;
        if (value >= 180.0f) value -= 360.0f;
        if (value < -180.0f) value += 360.0f;
        return value;
    }

    private float updateRotationFloat(float current, float target, float speed) {
        float diff = wrapDegrees(target - current);
        if (diff > speed) diff = speed;
        if (diff < -speed) diff = -speed;
        return current + diff;
    }

    // === Ротации ===

    private void rotations() {
        // GodBridge — режим строительства с минимальным jitter для точного бриджинга
        if (scaffoldMode.is("GodBridge")) {
            float targetYaw = (float) Math.toDegrees(
                    MoveUtils.direction(mc.player.rotationYaw - 180f,
                            mc.player.movementInput.moveForward, mc.player.movementInput.moveStrafe));
            float targetPitch = customGodPitch.get() ? godBridgePitch.get() : 75f;
            // Плавное приближение к целевым углам
            scaffoldYaw = updateRotationFloat(scaffoldYaw, targetYaw, 30);
            scaffoldPitch = updateRotationFloat(scaffoldPitch, targetPitch, 30);
            // Небольшой jitter для обхода анти-читов
            scaffoldYaw += (float) (Math.random() - Math.random()) * 0.5f;
            scaffoldPitch += (float) (Math.random() - Math.random()) * 0.5f;
            rotationVector = new Vector2f(scaffoldYaw, scaffoldPitch);
            lastYaw = scaffoldYaw;
            lastPitch = scaffoldPitch;
            return;
        }

        // Rewinside — режим ротации, реверс yaw с наклоном для обхода
        if (rotations.is("Rewinside")) {
            float targetYaw = (float) Math.toDegrees(
                    MoveUtils.direction(mc.player.rotationYaw - 180f,
                            mc.player.movementInput.moveForward, mc.player.movementInput.moveStrafe)) + 180f;
            scaffoldYaw = updateRotationFloat(scaffoldYaw, targetYaw, 25);
            scaffoldPitch = updateRotationFloat(scaffoldPitch, 80f, 25);
            rotationVector = new Vector2f(scaffoldYaw, scaffoldPitch);
            lastYaw = scaffoldYaw;
            lastPitch = scaffoldPitch;
            return;
        }

        // ReverseYaw — инвертируем yaw (дополнительная настройка)
        if (reverseYaw.get()) {
            mc.player.rotationYaw += 180f;
        }

        // UseOptimizedPitch — оптимизированный питч
        float optimizedPitch = useOptimizedPitch.get() ? 79.5f : lastPitch;

        boolean stop = false;
        float currentYaw = (float) Math.toDegrees(
                MoveUtils.direction(mc.player.rotationYaw - 180f,
                        mc.player.movementInput.moveForward, mc.player.movementInput.moveStrafe));
        float currentPitch = optimizedPitch;

        if (blockPos != null && facing != null) {
            Vector3d hit = getFaceHitVec(blockPos, facing);
            Vector2f to = calculateRotation(hit);

            switch (rotations.get()) {
                case "Direct" -> {
                    currentYaw += 180;
                    for (float pitch = 90; pitch > 30; pitch -= 1) {
                        if (lookingAtBlock(blockPos, facing, new Vector2f(mc.player.rotationYaw, pitch))) {
                            currentPitch = pitch;
                            break;
                        }
                    }
                }
                case "Snap" -> {
                    currentYaw = to.x;
                    currentPitch = to.y;
                }
                case "Keep" -> {
                    scaffoldYaw = to.x;
                    scaffoldPitch = to.y;
                    currentYaw = scaffoldYaw;
                    currentPitch = scaffoldPitch;
                }
                case "Intave", "God" -> {
                    float add = rotations.is("God") ? 45f : 0f;
                    currentYaw += add;
                    scaffoldPitch = to.y;
                    if (!lookingAtBlock(blockPos, facing, new Vector2f(currentYaw, scaffoldPitch))) {
                        int maxTicks = (int) Math.abs(wrapDegrees(scaffoldYaw - to.x) / 4);
                        int t = 0;
                        while (t <= maxTicks && !stop) {
                            scaffoldYaw = updateRotationFloat(scaffoldYaw, to.x, 5);
                            scaffoldPitch = to.y;
                            if (lookingAtBlock(blockPos, facing, new Vector2f(scaffoldYaw, scaffoldPitch))) {
                                stop = true;
                            }
                            t++;
                        }
                    }
                    if (!stop) {
                        if (ticksExisted == mc.player.ticksExisted) {
                            scaffoldYaw = updateRotationFloat(scaffoldYaw, currentYaw, 20);
                            ticksExisted++;
                        }
                    }
                    currentYaw = scaffoldYaw;
                    currentPitch = scaffoldPitch;
                }
                case "Static yaw", "Static yaw god" -> {
                    float add = rotations.is("Static yaw god") ? 45f : 0f;
                    currentYaw += add;
                    if (!lookingAtBlock(blockPos, facing, new Vector2f(currentYaw, lastPitch))) {
                        scaffoldPitch = to.y;
                    }
                    if (ticksExisted == mc.player.ticksExisted) {
                        scaffoldYaw = updateRotationFloat(scaffoldYaw, currentYaw, 20);
                        ticksExisted++;
                    }
                    currentYaw = scaffoldYaw;
                    currentPitch = scaffoldPitch;
                }
                case "Static god" -> {
                    currentYaw += 45f;
                    boolean diagonalG = (mc.player.movementInput.moveForward != 0 && mc.player.movementInput.moveStrafe != 0);
                    scaffoldPitch = diagonalG ? 75.5f : 77f;
                    if (ticksExisted == mc.player.ticksExisted) {
                        scaffoldYaw = updateRotationFloat(scaffoldYaw, currentYaw, 20);
                        ticksExisted++;
                    }
                    currentYaw = scaffoldYaw;
                    currentPitch = scaffoldPitch;
                }
                case "Static" -> {
                    boolean diagonalS = (mc.player.movementInput.moveForward != 0 && mc.player.movementInput.moveStrafe != 0);
                    scaffoldPitch = diagonalS ? 77.5f : 79.5f;
                    if (ticksExisted == mc.player.ticksExisted) {
                        scaffoldYaw = updateRotationFloat(scaffoldYaw, currentYaw, 20);
                        ticksExisted++;
                    }
                    currentYaw = scaffoldYaw;
                    currentPitch = scaffoldPitch;
                }
                case "Polar" -> {
                    if (!lookingAtBlock(blockPos, facing, new Vector2f(currentYaw, lastPitch))) {
                        scaffoldPitch = to.y;
                    }
                    if (ticksExisted == mc.player.ticksExisted) {
                        scaffoldYaw = updateRotationFloat(scaffoldYaw, currentYaw, 20);
                        ticksExisted++;
                    }
                    if (lastPitch != scaffoldPitch) mc.player.rotationYaw += (float) (Math.random() - Math.random());
                    if (lastYaw != scaffoldYaw) scaffoldPitch += (float) (Math.random() - Math.random());
                    currentYaw = scaffoldYaw;
                    currentPitch = scaffoldPitch;
                }
                default -> {
                }
            }
        }

        rotationVector = new Vector2f(currentYaw, currentPitch);
        lastYaw = currentYaw;
        lastPitch = currentPitch;
    }

    private boolean lookingAtBlock(BlockPos pos, Direction face, Vector2f rot) {
        Vector3d eyes = mc.player.getEyePosition(1.0f);
        Vector3d look = getLookVector(rot);
        Vector3d reach = eyes.add(look.scale(mc.playerController.getBlockReachDistance()));
        RayTraceContext ctx = new RayTraceContext(eyes, reach, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, mc.player);
        BlockRayTraceResult res = mc.world.rayTraceBlocks(ctx);
        return res != null && res.getType() == RayTraceResult.Type.BLOCK && res.getPos().equals(pos) && res.getFace() == face;
    }

    private Vector2f calculateRotation(Vector3d target) {
        Vector3d eyes = mc.player.getEyePosition(1.0f);
        double diffX = target.x - eyes.x;
        double diffY = target.y - eyes.y;
        double diffZ = target.z - eyes.z;
        double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);
        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0f;
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, dist));
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

    // === Установка блока ===

    private void place() {
        if (sneakWhenPlace.get()) {
            mc.gameSettings.keyBindSneak.setPressed(false);
        }
        if (blockPos == null || facing == null) return;
        Vector3d hv = getFaceHitVec(blockPos, facing);

        if (!rotations.is("Direct") && !scaffoldMode.is("GodBridge") && !rotations.is("Выкл") && expand.get() == 0) {
            if (!lookingAtBlock(blockPos, facing, rotationVector)) return;
        }
        int slot = findBlockSlot();

        // EarlySwitch — сменить слот раньше, если осталось мало блоков
        if (earlySwitch.get() && slot != -1) {
            ItemStack stack = mc.player.inventory.getStackInSlot(slot);
            if (stack.getCount() <= switchAmount.getInt()) {
                int betterSlot = -1;
                for (int i = 0; i < 9; i++) {
                    if (i == slot) continue;
                    ItemStack s = mc.player.inventory.getStackInSlot(i);
                    if (isValidBlock(s) && s.getCount() > stack.getCount()) {
                        betterSlot = i;
                    }
                }
                if (betterSlot != -1) slot = betterSlot;
            }
        }

        if (slot == -1) return;

        if (!spoof.is("Фейк")) {
            if (spoof.is("Обычный")) {
                spoofSlot = mc.player.inventory.currentItem;
                if (spoofSlot != slot) {
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

        if (sneakWhenPlace.get()) mc.gameSettings.keyBindSneak.setPressed(true);

        BlockRayTraceResult result = new BlockRayTraceResult(hv, facing, blockPos, false);
        ActionResultType actionResult = mc.playerController.processRightClickBlock(mc.player, mc.world, Hand.MAIN_HAND, result);
        if (actionResult == ActionResultType.SUCCESS) {
            if (swing.get()) {
                mc.player.swingArm(Hand.MAIN_HAND);
            } else {
                mc.player.connection.sendPacket(new CAnimateHandPacket(Hand.MAIN_HAND));
            }
            ticks = 0;

            // Eagle — увеличиваем счётчик блоков до начала орла
            if (eagle.get()) {
                placedBlocksUntilEagle++;
            }
            // SameY — обновляем launchY при успешной установке на текущей высоте
            if (sameY.get() && mc.player.isOnGround()) {
                launchY = (int) mc.player.getPosY();
            }

            // Telly — увеличиваем счётчик и прыгаем если пора
            updatePlacedBlocksForTelly();

            // SimulateDoubleClick — отправляем дополнительный клик через useItem
            if (simulateDoubleClick.get()) {
                mc.playerController.processRightClickBlock(mc.player, mc.world, Hand.MAIN_HAND, result);
            }
        }

        if (spoof.is("Обычный")) {
            if (slot != spoofSlot) {
                try {
                    silentHeldSwap = true;
                    mc.player.connection.sendPacket(new CHeldItemChangePacket(spoofSlot));
                } finally {
                    silentHeldSwap = false;
                }
            }
        }
    }

    private void fakeClick() {
        if (ticks++ <= 2) {
            mc.player.connection.sendPacket(new CAnimateHandPacket(Hand.MAIN_HAND));
        }
    }

    // === Поиск позиции блока ===

    private void processBlockData() {
        // Down mode — строим блоки вниз под игроком
        if (scaffoldMode.is("Down")) {
            processBlockDataDown();
            return;
        }
        if (expand.get() == 0) {
            blockPos = getBlockPos(mc.player.getPosX(), mc.player.getPosZ());
        } else {
            Vector3d vec = expandVec(new Vector3d(mc.player.getPosX(), posY, mc.player.getPosZ()));
            setBlockFacingOld(new BlockPos(vec.x, vec.y + 1, vec.z));
        }
        if (blockPos != null && expand.get() == 0) {
            facing = getPlaceSide(mc.player.getPosX(), mc.player.getPosZ());
        }
    }

    // Down mode — ищет пустые блоки ниже игрока для установки
    private void processBlockDataDown() {
        BlockPos playerPos = new BlockPos(mc.player.getPosX(), mc.player.getPosY(), mc.player.getPosZ());
        // Ищем первый пустой блок ниже игрока
        for (int y = playerPos.getY() - 1; y >= playerPos.getY() - 5; y--) {
            BlockPos checkPos = new BlockPos(playerPos.getX(), y, playerPos.getZ());
            if (mc.world.isAirBlock(checkPos)) {
                // Нашли пустой блок — ищем соседний твердый блок чтобы поставить на него
                BlockPos target = findDownPlaceTarget(checkPos);
                if (target != null) {
                    blockPos = target;
                    return;
                }
            } else {
                // Твердый блок — проверяем пустоту над ним
                break;
            }
        }
    }

    // Для Down mode — ищет позицию для установки блока рядом с пустой позицией
    private BlockPos findDownPlaceTarget(BlockPos emptyPos) {
        // Сначала проверяем стороны пустой позиции
        Direction[] sides = {Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        for (Direction dir : sides) {
            BlockPos adjacent = emptyPos.offset(dir);
            if (isPosSolid(adjacent)) {
                blockPos = adjacent;
                facing = dir.getOpposite();
                return adjacent;
            }
        }
        return null;
    }

    private Vector3d expandVec(Vector3d position) {
        int exp = expand.getInt();
        if (exp > 0) {
            final double direction = Math.toRadians(MoveUtils.getDirection());
            final Vector3d expandVector = new Vector3d(-Math.sin(direction), 0, Math.cos(direction));
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
                        Vector3d vec3 = new Vector3d(x + 0.5, y + 0.5, z + 0.5);
                        positions.add(vec3);
                        map.put(vec3, bp);
                    }
                }
            }
        }
        if (positions.isEmpty()) return null;
        positions.sort(Comparator.comparingDouble(v -> mc.player.getDistanceSq(v.x, v.y, v.z)));
        return map.get(positions.get(0));
    }

    private Direction getPlaceSide(double posX, double posZ) {
        ArrayList<Vector3d> positions = new ArrayList<>();
        HashMap<Vector3d, Direction> map = new HashMap<>();
        BlockPos playerPos = new BlockPos(posX, posY + 1, posZ);

        if (!isPosSolid(blockPos.up()) && !blockPos.up().equals(playerPos) && !mc.player.isOnGround()) {
            if (jump.get()) {
                if (mc.gameSettings.keyBindJump.isKeyDown()) {
                    BlockPos bp = blockPos.up();
                    Vector3d v = getBestHitFeet(bp);
                    positions.add(v);
                    map.put(v, Direction.UP);
                }
            } else {
                BlockPos bp = blockPos.up();
                Vector3d v = getBestHitFeet(bp);
                positions.add(v);
                map.put(v, Direction.UP);
            }
        }
        if (!isPosSolid(blockPos.east()) && !blockPos.east().equals(playerPos)) {
            BlockPos bp = blockPos.east();
            Vector3d v = getBestHitFeet(bp);
            positions.add(v);
            map.put(v, Direction.EAST);
        }
        if (!isPosSolid(blockPos.west()) && !blockPos.west().equals(playerPos)) {
            BlockPos bp = blockPos.west();
            Vector3d v = getBestHitFeet(bp);
            positions.add(v);
            map.put(v, Direction.WEST);
        }
        if (!isPosSolid(blockPos.south()) && !blockPos.south().equals(playerPos)) {
            BlockPos bp = blockPos.south();
            Vector3d v = getBestHitFeet(bp);
            positions.add(v);
            map.put(v, Direction.SOUTH);
        }
        if (!isPosSolid(blockPos.north()) && !blockPos.north().equals(playerPos)) {
            BlockPos bp = blockPos.north();
            Vector3d v = getBestHitFeet(bp);
            positions.add(v);
            map.put(v, Direction.NORTH);
        }
        positions.sort(Comparator.comparingDouble(v -> mc.player.getDistanceSq(v.x, v.y, v.z)));
        if (!positions.isEmpty()) {
            Vector3d v5 = getBestHitFeet(blockPos);
            if (mc.player.getDistanceSq(v5.x, v5.y, v5.z) >= mc.player.getDistanceSq(positions.get(0).x, positions.get(0).y, positions.get(0).z)) {
                return map.get(positions.get(0));
            }
        }
        return null;
    }

    private Vector3d getFaceHitVec(BlockPos pos, Direction face) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        double off = 0.5;
        switch (face) {
            case UP -> y += off;
            case DOWN -> y -= off;
            case NORTH -> z -= off;
            case SOUTH -> z += off;
            case WEST -> x -= off;
            case EAST -> x += off;
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
}
