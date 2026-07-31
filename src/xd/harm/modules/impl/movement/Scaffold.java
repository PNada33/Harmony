package xd.harm.modules.impl.movement;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.FlowingFluidBlock;
import net.minecraft.client.settings.PointOfView;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.client.CAnimateHandPacket;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.network.play.client.CPlayerPacket;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import xd.harm.events.input.EventInput;
import xd.harm.events.movement.EventMotion;
import xd.harm.events.movement.SprintEvent;
import xd.harm.events.network.EventPacket;
import xd.harm.events.render.EventDisplay;
import xd.harm.events.render.EventRender3D;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.CategorySetting;
import xd.harm.modules.settings.impl.ColorSetting;
import xd.harm.modules.settings.impl.ModeSetting;
import xd.harm.modules.settings.impl.SliderSetting;
import xd.harm.modules.impl.render.Theme;
import xd.harm.utils.client.TimerUtility;
import xd.harm.utils.render.Render3D;
import xd.harm.utils.render.RectUtility;
import xd.harm.utils.render.color.ColorUtils;
import xd.harm.utils.text.font.ClientFonts;

import xd.harm.utils.player.MoveUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

@ModuleRegister(name = "Scaffold", category = Category.Movement)
public class Scaffold extends Module {

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

    // === CATEGORIES ===
    private final CategorySetting catBasic = new CategorySetting("ОСНОВНЫЕ");
    private final CategorySetting catTechnique = new CategorySetting("ТЕХНИКА");
    private final CategorySetting catRotation = new CategorySetting("РОТАЦИИ");
    private final CategorySetting catMovement = new CategorySetting("ДВИЖЕНИЕ");
    private final CategorySetting catTower = new CategorySetting("ТАУЭР");
    private final CategorySetting catAdvanced = new CategorySetting("ДОПОЛНИТЕЛЬНО");
    private final CategorySetting catRender = new CategorySetting("РЕНДЕР");

    // === PLACEMENT MODE (Normal / RiseTelly / Telly) ===
    private final ModeSetting placementMode = new ModeSetting("Placement", "Normal", "Normal", "RiseTelly", "Telly");

    // === ROTATION ===
    private final ModeSetting rotationMode = new ModeSetting("Rotation", "Default",
            "Default", "None",
            "Static yaw", "Static yaw god", "Static god", "Static", "Polar", "Intave", "God", "Keep", "Snap", "Direct",
            // FDP 5.4.0
            "Better", "AAC", "Vanilla", "Static1", "Static2", "Custom", "Advanced", "Backwards", "Snap FDP", "BackSnap",
            // LB b100
            "Normal lb100", "Stabilized", "ReverseYaw", "Off",
            // LB NextGen
            "Normal lbNext", "OnTick", "OnTickSnap",
            // Opal
            "Organic");

    // === SPRINT ===
    // === TOWER ===
    private final ModeSetting towerMode = new ModeSetting("Tower", "None",
            "None", "Jump", "Matrix", "Packet", "MotionTP", "Timer", "Intave");

    // === SPOOF ===
    private final ModeSetting spoof = new ModeSetting("Spoof slot", "None", "None", "Normal", "Fake", "Pick", "Spoof", "Switch", "Silent");

    // === SWITCH MODE (Opal) ===
    private final ModeSetting switchMode = new ModeSetting("Switch Mode", "Normal", "Normal", "Hotbar", "Full");

    // === OLD TELLY (backward bridge) ===
    private final SliderSetting tellyJumpDelay = new SliderSetting("Telly задержка", 6f, 1f, 20f, 1f)
            .setVisible(() -> placementMode.is("Telly"));
    private final SliderSetting tellyBlockCount = new SliderSetting("Telly блоков", 4f, 1f, 10f, 1f)
            .setVisible(() -> placementMode.is("Telly"));

    // === RAY CAST (Rise) ===
    private final ModeSetting rayCast = new ModeSetting("RayCast", "Normal", "Off", "Normal", "Strict");

    // === Opal: Override Raycast ===
    private final BooleanSetting overrideRaycast = new BooleanSetting("Override Raycast", false);

    // === ROTATION SPEED (Rise) ===
    private final SliderSetting rotationSpeedMin = new SliderSetting("Rotation Speed Min", 15f, 1f, 100f, 1f);
    private final SliderSetting rotationSpeedMax = new SliderSetting("Rotation Speed Max", 25f, 1f, 100f, 1f);

    // === Opal: Organic Rotation ===
    private final SliderSetting organicDrift = new SliderSetting("Organic Drift", 1.2f, 0.5f, 2f, 0.1f);
    private final SliderSetting organicJitter = new SliderSetting("Organic Jitter", 0.12f, 0f, 0.3f, 0.01f);

    // === PLACE DELAY (Rise) ===
    private final SliderSetting placeDelayMin = new SliderSetting("Place Delay Min", 0f, 0f, 500f, 1f);
    private final SliderSetting placeDelayMax = new SliderSetting("Place Delay Max", 0f, 0f, 500f, 1f);

    // === DELAYED RISE TELLY (прыжок через N блоков) ===
    private final BooleanSetting delayedRiseTelly = new BooleanSetting("Delayed RiseTelly", false);
    private final SliderSetting delayedRiseTellyBlocks = new SliderSetting("Delay блоков", 3f, 1f, 10f, 1f)
            .setVisible(() -> delayedRiseTelly.get() && placementMode.is("RiseTelly"));

    // === BOOLEANS (старые + новые) ===
    private final ModeSetting yMode = new ModeSetting("Режим Y", "Off", "Off", "On", "Falling", "Hypixel");
    private final ModeSetting swingMode = new ModeSetting("Размах", "DoNotHide", "DoNotHide", "Hide", "NoSwing", "Instant");
    private final BooleanSetting search = new BooleanSetting("Search", true);
    private final BooleanSetting moveFix = new BooleanSetting("MoveFix", false);
    private final ModeSetting safeWalkMode = new ModeSetting("SafeWalk", "Legit", "Legit", "Выкл");
    private final BooleanSetting switchBack = new BooleanSetting("Switch Back", true);
    private final BooleanSetting sneak = new BooleanSetting("Sneak", false);

    private final BooleanSetting dragClick = new BooleanSetting("Drag Click", false);
    private final BooleanSetting sneakWhenPlace = new BooleanSetting("Sneak when place", false);
    private final ModeSetting eagleMode = new ModeSetting("Орел", "Normal", "Normal", "Sneak", "Выкл");
    private final BooleanSetting jumpStrafe = new BooleanSetting("Стрейф прыжка", false);
    private final SliderSetting jumpStraightStrafe = new SliderSetting("Прямой стрейф", 0.5f, 0f, 1f, 0.01f)
            .setVisible(() -> jumpStrafe.get());
    private final SliderSetting jumpDiagonalStrafe = new SliderSetting("Диагональный стрейф", 0.5f, 0f, 1f, 0.01f)
            .setVisible(() -> jumpStrafe.get());

    // === ZITTER ===
    private final ModeSetting zitterMode = new ModeSetting("Zitter", "OFF", "OFF", "Teleport", "Smooth");
    private final SliderSetting zitterSpeed = new SliderSetting("ZitterSpeed", 0.13f, 0.1f, 0.9f, 0.01f)
            .setVisible(() -> !zitterMode.is("OFF"));
    private final SliderSetting zitterStrength = new SliderSetting("ZitterStrength", 0.072f, 0.01f, 0.2f, 0.001f)
            .setVisible(() -> zitterMode.is("Teleport"));
    private final BooleanSetting zitterPro = new BooleanSetting("Zitter Pro", false)
            .setVisible(() -> zitterMode.is("Teleport"));
    private final SliderSetting zitterSpeedStart = new SliderSetting("ZitterSpeedStart", 0.13f, 0.1f, 0.9f, 0.01f)
            .setVisible(() -> zitterMode.is("Teleport") && zitterPro.get());
    private final SliderSetting zitterSpeedOver = new SliderSetting("ZitterSpeedOver", 0.13f, 0.1f, 0.9f, 0.01f)
            .setVisible(() -> zitterMode.is("Teleport") && zitterPro.get());
    private final SliderSetting zitterStrengthStart = new SliderSetting("ZitterStrengthStart", 0.072f, 0.01f, 0.2f, 0.001f)
            .setVisible(() -> zitterMode.is("Teleport") && zitterPro.get());
    private final SliderSetting zitterStrengthOver = new SliderSetting("ZitterStrengthOver", 0.072f, 0.01f, 0.2f, 0.001f)
            .setVisible(() -> zitterMode.is("Teleport") && zitterPro.get());
    private final SliderSetting zitterStartBlocks = new SliderSetting("ZitterStartBlocks", 3f, 1f, 10f, 1f)
            .setVisible(() -> zitterMode.is("Teleport") && zitterPro.get());

    // === CPS & RANGE ===
    private final SliderSetting cps = new SliderSetting("CPS", 20f, 1f, 100f, 1f);
    private final SliderSetting range = new SliderSetting("Range", 4.5f, 1f, 6f, 0.1f);

    // === TIMER ===
    private final ModeSetting timerMode = new ModeSetting("Timer", "Выкл", "Выкл",
            "Постоянный", "Прерывистый", "Движение", "Прыжок");
    private final SliderSetting timerSpeed = new SliderSetting("T скорость", 2f, 0.1f, 2.5f, 0.01f)
            .setVisible(() -> !timerMode.is("Выкл"));
    private final SliderSetting timerSlowSpeed = new SliderSetting("T медленная", 0.5f, 0.1f, 1f, 0.05f)
            .setVisible(() -> timerMode.is("Прерывистый"));
    private final SliderSetting timerFastTime = new SliderSetting("T время ускор", 200f, 50f, 1000f, 50f)
            .setVisible(() -> timerMode.is("Прерывистый"));
    private final SliderSetting timerSlowTime = new SliderSetting("T время замедл", 200f, 50f, 1000f, 50f)
            .setVisible(() -> timerMode.is("Прерывистый"));
    private final BooleanSetting timerOnlyOnGround = new BooleanSetting("T только земля", false)
            .setVisible(() -> timerMode.is("Прыжок"));
    private final BooleanSetting timerStopOnHurt = new BooleanSetting("T стоп урон", false)
            .setVisible(() -> !timerMode.is("Выкл"));
    private final BooleanSetting timerStopInWater = new BooleanSetting("T стоп вода", false)
            .setVisible(() -> !timerMode.is("Выкл"));

    // === SLIDERS ===
    private final SliderSetting expand = new SliderSetting("Expand", 0f, 0f, 8f, 1f);
    private final SliderSetting sneakDelay = new SliderSetting("Sneak Delay", 4f, 1f, 20f, 1f)
            .setVisible(() -> sneak.get());
    private final SliderSetting sneakTime = new SliderSetting("Sneak Time", 2f, 1f, 6f, 1f)
            .setVisible(() -> sneak.get());

    // === NEW: Eagle扩展 (FDP, LB100) ===
    private final SliderSetting blocksToEagle = new SliderSetting("Blocks to Eagle", 0f, 0f, 20f, 1f)
            .setVisible(() -> !eagleMode.is("Выкл"));
    private final SliderSetting edgeDistance = new SliderSetting("Edge Distance", 0.3f, 0.1f, 0.5f, 0.01f)
            .setVisible(() -> !eagleMode.is("Выкл"));

    // === NEW: Search (LB100) ===
    private final ModeSetting searchMode = new ModeSetting("Search Mode", "Center", "Center", "Lazy", "Nearest");
    private final SliderSetting minDist = new SliderSetting("Min Dist", 0f, 0f, 2f, 0.1f);

    // === NEW: Rotation扩展 (FDP, LB100) ===
    private final SliderSetting keepRotationTick = new SliderSetting("Keep Rotation Tick", 0f, 0f, 20f, 1f);
    private final BooleanSetting waitForRotations = new BooleanSetting("Wait For Rotations", false);
    private final BooleanSetting useOptimizedPitch = new BooleanSetting("Optimized Pitch", false);
    private final SliderSetting godBridgePitch = new SliderSetting("GodBridge Pitch", 75f, 60f, 90f, 0.5f)
            .setVisible(() -> rotationMode.is("God") || rotationMode.is("Static yaw god") || rotationMode.is("Static god"));
    private final SliderSetting customYaw = new SliderSetting("Custom Yaw", 0f, -180f, 180f, 0.5f)
            .setVisible(() -> rotationMode.is("Custom"));
    private final SliderSetting customPitch = new SliderSetting("Custom Pitch", 90f, -180f, 180f, 0.5f)
            .setVisible(() -> rotationMode.is("Custom"));

    // === NEW: BlockSafe (LB100) ===
    private final BooleanSetting blockSafe = new BooleanSetting("Block Safe", false);

    // === NEW: ExtraClick (FDP, LB100) ===
    private final BooleanSetting extraClick = new BooleanSetting("Extra Click", false);
    private final SliderSetting extraClickMinDelay = new SliderSetting("Extra Click Min Delay", 0f, 0f, 500f, 50f)
            .setVisible(() -> extraClick.get());
    private final SliderSetting extraClickMaxDelay = new SliderSetting("Extra Click Max Delay", 0f, 0f, 500f, 50f)
            .setVisible(() -> extraClick.get());

    // === NEW: Ledge (LB NextGen) ===
    private final BooleanSetting ledge = new BooleanSetting("Ledge", false);

    // === NEW: Blink (LB NextGen) ===
    private final BooleanSetting blink = new BooleanSetting("Blink", false);
    private final SliderSetting blinkPulseTime = new SliderSetting("Blink Pulse Time", 20f, 1f, 100f, 1f)
            .setVisible(() -> blink.get());

    // === NEW: Down (FDP, LB100) ===
    private final BooleanSetting down = new BooleanSetting("Down", false);

    // === NEW: Slow (LB100) ===
    private final BooleanSetting slow = new BooleanSetting("Slow", false);
    private final BooleanSetting slowGround = new BooleanSetting("Slow Only Ground", false)
            .setVisible(() -> slow.get());
    private final SliderSetting slowSpeed = new SliderSetting("Slow Speed", 0.2f, 0.1f, 1f, 0.01f)
            .setVisible(() -> slow.get());

    // === NEW: SpeedLimiter (LB100) ===
    private final BooleanSetting speedLimiter = new BooleanSetting("Speed Limiter", false);
    private final SliderSetting speedLimit = new SliderSetting("Speed Limit", 0.6f, 0.1f, 2f, 0.01f)
            .setVisible(() -> speedLimiter.get());

    // === NEW: AirSafe (LB100) ===
    private final BooleanSetting airSafe = new BooleanSetting("Air Safe", false);

    // === NEW: Jump Automatically (LB100) ===
    private final BooleanSetting jumpAutomatically = new BooleanSetting("Jump Automatically", false);
    private final SliderSetting blocksToJumpMin = new SliderSetting("Blocks to Jump Min", 2f, 1f, 10f, 1f)
            .setVisible(() -> jumpAutomatically.get());
    private final SliderSetting blocksToJumpMax = new SliderSetting("Blocks to Jump Max", 4f, 1f, 10f, 1f)
            .setVisible(() -> jumpAutomatically.get());

    // === NEW: Mark (FDP, LB100) ===
    private final BooleanSetting mark = new BooleanSetting("Mark", false);
    private final ModeSetting markColorMode = new ModeSetting("Mark Color", "Клиент", "Клиент", "Свой")
            .setVisible(() -> mark.get());
    private final ColorSetting markCustomColor = new ColorSetting("Mark Custom Color", ColorUtils.rgb(255, 0, 0))
            .setVisible(() -> mark.get() && markColorMode.is("Свой"));

    // === NEW: Counter (FDP) ===
    private final ModeSetting counterDisplay = new ModeSetting("Counter", "Off", "Off", "Default", "Rise", "RiseESP", "Island");

    // === Opal: Fading Overlay ===
    private final BooleanSetting markFade = new BooleanSetting("Mark Fade", false)
            .setVisible(() -> mark.get());
    private final SliderSetting markFadeTime = new SliderSetting("Mark Fade Time", 300f, 50f, 1000f, 50f)
            .setVisible(() -> mark.get() && markFade.get());



    // === NEW: Tower扩展 (FDP) ===
    private final BooleanSetting towerStopWhenBlockAbove = new BooleanSetting("Tower Stop Block Above", false);
    private final SliderSetting towerJumpMotion = new SliderSetting("Tower Jump Motion", 0.42f, 0.1f, 1f, 0.01f)
            .setVisible(() -> towerMode.is("Jump"));
    private final SliderSetting towerStableMotion = new SliderSetting("Tower Stable Motion", 0.19f, 0.01f, 0.5f, 0.01f)
            .setVisible(() -> towerMode.is("Jump"));

    // === NEW: Sneak扩展 (LB100) ===
    private final BooleanSetting autoF5 = new BooleanSetting("Auto F5", false);

    // === NEW: Clutching (LB100) ===
    private final BooleanSetting allowClutching = new BooleanSetting("Allow Clutching", false);
    private final SliderSetting horizontalClutchBlocks = new SliderSetting("Horizontal Clutch", 1f, 0f, 3f, 1f)
            .setVisible(() -> allowClutching.get());
    private final SliderSetting verticalClutchBlocks = new SliderSetting("Vertical Clutch", 1f, 0f, 3f, 1f)
            .setVisible(() -> allowClutching.get());

    // === LB NextGen: Acceleration ===
    private final BooleanSetting acceleration = new BooleanSetting("Acceleration", false);
    private final SliderSetting accelerationSpeed = new SliderSetting("Accel Speed", 1.2f, 1.0f, 5.0f, 0.1f)
            .setVisible(() -> acceleration.get());
    private final BooleanSetting accelerationOnlyGround = new BooleanSetting("Accel Only Ground", true)
            .setVisible(() -> acceleration.get());

    // === LB NextGen: AutoBlock ===
    private final BooleanSetting autoBlock = new BooleanSetting("AutoBlock", false);
    private final BooleanSetting autoBlockAlways = new BooleanSetting("AB Always Hold", false)
            .setVisible(() -> autoBlock.get());
    private final SliderSetting autoBlockSlotResetDelay = new SliderSetting("AB SlotReset Delay", 4f, 0f, 20f, 1f)
            .setVisible(() -> autoBlock.get());
    private final SliderSetting autoBlockDoNotUseBelow = new SliderSetting("AB DoNotUse Below", 0f, 0f, 10f, 1f)
            .setVisible(() -> autoBlock.get());

    // === Opal: Modern Delay ===
    private final BooleanSetting modernDelay = new BooleanSetting("Modern Delay", false);

    // === Opal: Jump Mode ===
    private final ModeSetting jumpMode = new ModeSetting("Jump Mode", "Vanilla", "Vanilla", "AntiGamingChair", "Watchdog", "Bloxd");

    // === Opal: Real Stack Size ===
    private final BooleanSetting syncStackSize = new BooleanSetting("Sync Stack Size", false);

    // === LB NextGen: Ceiling ===
    private final BooleanSetting ceiling = new BooleanSetting("Ceiling", false);

    // === LB NextGen: HeadHitter ===
    private final BooleanSetting headHitter = new BooleanSetting("HeadHitter", false);

    // === LB NextGen: SprintControl ===
    private final BooleanSetting sprintControl = new BooleanSetting("SprintControl", false);
    private final ModeSetting sprintClientMode = new ModeSetting("SC Client", "DO_NOT_CHANGE",
            "DO_NOT_CHANGE", "FORCE_SPRINT", "FORCE_NO_SPRINT", "NO_SPRINT_ON_PLACE", "NO_SPRINT_ON_GROUND")
            .setVisible(() -> sprintControl.get());
    private final ModeSetting sprintServerMode = new ModeSetting("SC Server", "DO_NOT_CHANGE",
            "DO_NOT_CHANGE", "FORCE_SPRINT", "FORCE_NO_SPRINT", "NO_SPRINT_ON_PLACE", "NO_SPRINT_ON_GROUND")
            .setVisible(() -> sprintControl.get());


    // === LB NextGen: Strafe ===
    private final BooleanSetting strafe = new BooleanSetting("Strafe", false);
    private final SliderSetting strafeSpeed = new SliderSetting("Strafe Speed", 0.3f, 0.1f, 2.0f, 0.01f)
            .setVisible(() -> strafe.get());
    private final BooleanSetting strafeHypixel = new BooleanSetting("Strafe Hypixel", false)
            .setVisible(() -> strafe.get());
    private final BooleanSetting strafeOnlyGround = new BooleanSetting("Strafe Only Ground", true)
            .setVisible(() -> strafe.get());

    // === LB NextGen: GodBridge technique ===
    private final BooleanSetting godBridge = new BooleanSetting("GodBridge", false);
    private final BooleanSetting godBridgeJump = new BooleanSetting("GB Jump", true)
            .setVisible(() -> godBridge.get());
    private final BooleanSetting godBridgeSneakAct = new BooleanSetting("GB Sneak", true)
            .setVisible(() -> godBridge.get());
    private final BooleanSetting godBridgeStopInput = new BooleanSetting("GB StopInput", false)
            .setVisible(() -> godBridge.get());
    private final BooleanSetting godBridgeBackwards = new BooleanSetting("GB Backwards", false)
            .setVisible(() -> godBridge.get());
    private final BooleanSetting godBridgeForceSneakBelow = new BooleanSetting("GB ForceSneak Below", false)
            .setVisible(() -> godBridge.get());
    private final SliderSetting godBridgeSneakTime = new SliderSetting("GB Sneak Time", 2f, 1f, 10f, 1f)
            .setVisible(() -> godBridge.get());

    // === LB NextGen: Breezily technique ===
    private final BooleanSetting breezily = new BooleanSetting("Breezily", false);
    private final SliderSetting breezilyEdgeDistance = new SliderSetting("Breezily Edge", 0.3f, 0.05f, 0.6f, 0.01f)
            .setVisible(() -> breezily.get());

    // === Opal: Movement Intelligence (вместо StabilizeMove) ===
    private final BooleanSetting movementIntelligence = new BooleanSetting("MI Enabled", false);
    private final BooleanSetting miDiagonal = new BooleanSetting("MI Diagonal", false)
            .setVisible(() -> movementIntelligence.get());
    private final BooleanSetting miSnap = new BooleanSetting("MI Snap", true)
            .setVisible(() -> movementIntelligence.get());
    private final SliderSetting miSteps = new SliderSetting("MI Steps", 3f, 1f, 3f, 1f)
            .setVisible(() -> movementIntelligence.get() && miSnap.get());

    // === Opal: Player Simulation ===
    private final BooleanSetting playerSimulation = new BooleanSetting("Predict", false);
    private final SliderSetting predictTicks = new SliderSetting("Predict Ticks", 5f, 1f, 10f, 1f)
            .setVisible(() -> playerSimulation.get());

    // === LB NextGen: ConsiderInventory (rotation) ===
    private final BooleanSetting considerInventory = new BooleanSetting("Consider Inventory", false);

    // === LB NextGen: Blink FlushOn + Time range ===
    private final BooleanSetting blinkFlushOnPlace = new BooleanSetting("Blink Flush Place", true)
            .setVisible(() -> blink.get());
    private final BooleanSetting blinkFlushOnTowering = new BooleanSetting("Blink Flush Towering", false)
            .setVisible(() -> blink.get());
    private final BooleanSetting blinkFlushOnSneaking = new BooleanSetting("Blink Flush Sneaking", false)
            .setVisible(() -> blink.get());
    private final BooleanSetting blinkFlushOnNotSneaking = new BooleanSetting("Blink Flush NotSneak", false)
            .setVisible(() -> blink.get());
    private final BooleanSetting blinkFlushOnGround = new BooleanSetting("Blink Flush Ground", false)
            .setVisible(() -> blink.get());
    private final BooleanSetting blinkFlushOnAir = new BooleanSetting("Blink Flush Air", false)
            .setVisible(() -> blink.get());
    private final SliderSetting blinkTimeMin = new SliderSetting("Blink Time Min", 10f, 1f, 500f, 5f)
            .setVisible(() -> blink.get());
    private final SliderSetting blinkTimeMax = new SliderSetting("Blink Time Max", 30f, 1f, 500f, 5f)
            .setVisible(() -> blink.get());

    // === LB NextGen: Telly расширенный ===
    private final ModeSetting tellyResetMode = new ModeSetting("Telly ResetMode", "RESET", "RESET", "REVERSE")
            .setVisible(() -> placementMode.is("Telly") || placementMode.is("RiseTelly"));
    private final SliderSetting tellyStraightTicks = new SliderSetting("Telly StraightTicks", 3f, 1f, 20f, 1f)
            .setVisible(() -> placementMode.is("Telly") || placementMode.is("RiseTelly"));
    private final SliderSetting tellyJumpTicksMin = new SliderSetting("Telly JumpTicks Min", 1f, 1f, 10f, 1f)
            .setVisible(() -> placementMode.is("Telly") || placementMode.is("RiseTelly"));
    private final SliderSetting tellyJumpTicksMax = new SliderSetting("Telly JumpTicks Max", 2f, 1f, 10f, 1f)
            .setVisible(() -> placementMode.is("Telly") || placementMode.is("RiseTelly"));
    private final BooleanSetting tellyAimOnTower = new BooleanSetting("Telly AimOnTower", false)
            .setVisible(() -> placementMode.is("Telly") || placementMode.is("RiseTelly"));

    // === TIMERS ===
    private final TimerUtility placeTimer = TimerUtility.create();
    private final TimerUtility sneakTimer = TimerUtility.create();
    private final TimerUtility eagleTimer = TimerUtility.create();


    // === STATE ===
    private PlaceInfo placeInfo = null;
    private Vector2f rotationVector = new Vector2f(0, 0);
    private float lastYaw = 0;
    private float lastPitch = 0;
    private double lastY = 0;
    private boolean towerStatus = false;
    private int blockCount = 0;
    private int riseTellyBlocksPlaced = 0;
    private int zitterProBlocksPlaced = 0;
    private int towerState = 0;
    private boolean zitterDirection = false;
    private long lastZitterTime = 0;
    private int towerTicks = 0;

    // Spoof state (FDPClient-style)
    private boolean silentHeldSwap = false;
    private int posY, itemBefore, ticksExisted, ticks;
    private int spoofSlot = -1;
    private final ItemStack barrier = new ItemStack(Blocks.BARRIER);

    private int offGroundTicks;
    private int tellyGroundPosY = -1;
    private int tellyPlacedBlocks;
    private int tellyJumpTicks;
    private float tellyAimYaw;
    private int autoJumpBlocksPlaced;
    private int keepRotationTicks;
    private BlockPos lastPlacedBlockPos;
    private long lastPlaceTime;
    private final List<IPacket<?>> blinkPacketBuffer = new ArrayList<>();
    private final TimerUtility blinkTimer = TimerUtility.create();
    private final TimerUtility timerPulseTimer = TimerUtility.create();
    private boolean timerPulseFast = true;
    private boolean eagleSneaking;
    private final Random random = new Random();
    private int placedBlocksUntilEagle;
    private float scaffoldYaw, scaffoldPitch;
    private BlockPos blockPos;
    private Direction facing;
    private final HashMap<Integer, Integer> realStackMap = new HashMap<>();

    public Scaffold() {
        addSettings(
                catBasic,
                placementMode, cps, range, placeDelayMin, placeDelayMax,
                timerMode, timerSpeed, timerSlowSpeed, timerFastTime, timerSlowTime,
                timerOnlyOnGround, timerStopOnHurt, timerStopInWater,

                catTechnique,
                expand, rayCast, overrideRaycast, search, searchMode, minDist, blockSafe,
                yMode, swingMode, dragClick, switchBack, spoof, switchMode,
                sneakWhenPlace, sneak, sneakDelay, sneakTime,
                delayedRiseTelly, delayedRiseTellyBlocks,
                tellyJumpDelay, tellyBlockCount,
                tellyResetMode, tellyStraightTicks,
                tellyJumpTicksMin, tellyJumpTicksMax, tellyAimOnTower,
                zitterMode, zitterSpeed, zitterStrength,
                zitterPro, zitterSpeedStart, zitterSpeedOver,
                zitterStrengthStart, zitterStrengthOver, zitterStartBlocks,
                eagleMode, blocksToEagle, edgeDistance,
                safeWalkMode, airSafe, moveFix,
                jumpStrafe, jumpStraightStrafe, jumpDiagonalStrafe,
                ledge,
                acceleration, accelerationSpeed, accelerationOnlyGround,
                autoBlock, autoBlockAlways, autoBlockSlotResetDelay, autoBlockDoNotUseBelow,
                modernDelay, syncStackSize,
                ceiling, headHitter,
                godBridge, godBridgeJump, godBridgeSneakAct, godBridgeStopInput, godBridgeBackwards,
                godBridgeForceSneakBelow, godBridgeSneakTime,
                breezily, breezilyEdgeDistance,
                movementIntelligence, miDiagonal, miSnap, miSteps,
                playerSimulation, predictTicks,

                catRotation,
                rotationMode, rotationSpeedMin, rotationSpeedMax,
                organicDrift, organicJitter,
                keepRotationTick, waitForRotations,
                useOptimizedPitch, godBridgePitch,
                customYaw, customPitch,
                considerInventory,

                catMovement,
                slow, slowGround, slowSpeed,
                speedLimiter, speedLimit,
                down, autoF5,
                jumpMode, jumpAutomatically, blocksToJumpMin, blocksToJumpMax,
                sprintControl, sprintClientMode, sprintServerMode,
                strafe, strafeSpeed, strafeHypixel, strafeOnlyGround,

                catTower,
                towerMode, towerStopWhenBlockAbove,
                towerJumpMotion, towerStableMotion,

                catAdvanced,
                extraClick, extraClickMinDelay, extraClickMaxDelay,
                blink, blinkPulseTime,
                blinkFlushOnPlace, blinkFlushOnTowering, blinkFlushOnSneaking,
                blinkFlushOnNotSneaking, blinkFlushOnGround, blinkFlushOnAir,
                blinkTimeMin, blinkTimeMax,
                allowClutching, horizontalClutchBlocks, verticalClutchBlocks,

                catRender,
                mark, markColorMode, markCustomColor, markFade, markFadeTime,
                counterDisplay
        );
    }

    @Override
    public boolean onEnable() {
        super.onEnable();

        // === NEW: Auto F5 (LB100) ===
        if (autoF5.get()) {
            mc.gameSettings.setPointOfView(PointOfView.THIRD_PERSON_BACK);
        }

        itemBefore = mc.player.inventory.currentItem;
        spoofSlot = mc.player.inventory.currentItem;
        if (!spoof.is("Fake")) {
            switchToBlockSlot();
        }

        placeInfo = null;
        rotationVector = new Vector2f(mc.player.rotationYaw, mc.player.rotationPitch);
        lastYaw = mc.player.rotationYaw - 180f;
        lastPitch = rotationMode.is("Polar") ? 82f : 90f;
        posY = (int) (mc.player.getPosY() - 1);
        scaffoldYaw = mc.player.rotationYaw;
        scaffoldPitch = rotationMode.is("Polar") ? 82f : 90f;
        lastY = mc.player.getPosY();
        lastPlacedBlockPos = null;
        lastPlaceTime = 0;
        eagleSneaking = false;
        placedBlocksUntilEagle = 0;
        towerState = 0;
        towerStatus = false;
        zitterDirection = false;
        lastZitterTime = System.currentTimeMillis();
        riseTellyBlocksPlaced = 0;
        zitterProBlocksPlaced = 0;
        towerTicks = 0;
        tellyPlacedBlocks = 0;
        timerPulseTimer.reset();
        timerPulseFast = true;
        mc.timer.timerSpeed = 1.0f;
        blinkPacketBuffer.clear();
        blinkTimer.reset();

        updateBlockCount();
        placeTimer.reset();
        processBlockData();
        handleRotations();

        return false;
    }

    @Override
    public boolean onDisable() {
        if (spoof.is("Fake")) {
            if (spoofSlot != mc.player.inventory.currentItem) {
                try {
                    silentHeldSwap = true;
                    mc.player.connection.sendPacket(new CHeldItemChangePacket(mc.player.inventory.currentItem));
                } finally {
                    silentHeldSwap = false;
                }
            }
        } else if (switchBack.get()) {
            mc.player.inventory.currentItem = itemBefore;
        }
        placeInfo = null;
        eagleSneaking = false;
        mc.gameSettings.keyBindSneak.setPressed(false);

        if (zitterMode.is("Smooth")) {
            mc.gameSettings.keyBindRight.setPressed(false);
            mc.gameSettings.keyBindLeft.setPressed(false);
        }

        mc.timer.timerSpeed = 1.0f;

        // === NEW: Auto F5 restore ===
        if (autoF5.get()) {
            mc.gameSettings.setPointOfView(PointOfView.FIRST_PERSON);
        }

        // === NEW: Flush blink buffer on disable ===
        if (blink.get()) {
            for (IPacket<?> bp : blinkPacketBuffer) {
                mc.player.connection.sendPacket(bp);
            }
            blinkPacketBuffer.clear();
        }

        return super.onDisable();
    }

    @Subscribe
    private void onMotion(EventMotion e) {
        if (!isRotationNone()) {
            e.setYaw(rotationVector.x);
            e.setPitch(rotationVector.y);
            mc.player.renderYawOffset = rotationVector.x;
            mc.player.rotationYawHead = rotationVector.x;
            mc.player.rotationPitchHead = rotationVector.y;
        }

        handleTower(e);
        handleZitter();
    }

    @Subscribe
    private void onSprint(SprintEvent e) {
        // === LB NextGen: SprintControl ===
        if (sprintControl.get()) {
            String serverMode = sprintServerMode.get();
            switch (serverMode) {
                case "FORCE_NO_SPRINT":
                    e.cancel();
                    break;
                case "NO_SPRINT_ON_PLACE":
                    if (lastPlacedBlockPos != null) e.cancel();
                    break;
                case "NO_SPRINT_ON_GROUND":
                    if (mc.player.isOnGround()) e.cancel();
                    break;
            }
        }
    }

    @Subscribe
    private void onDisplay(EventDisplay e) {
        if (e.getType() != EventDisplay.Type.POST) return;
        renderBlockCounter(e.getMatrixStack());
    }

    @Subscribe
    private void onInput(EventInput eventInput) {
        if (moveFix.get() && !isRotationNone()) {
            MoveUtils.fixMovement(eventInput, mc.player.rotationYaw);
        }

        if (sneak.get()) {
            if (sneakTimer.hasTimeElapsed((long) (sneakDelay.get() * 100))) {
                eventInput.setSneak(true);
                if (sneakTimer.hasTimeElapsed((long) ((sneakDelay.get() * 100) + (sneakTime.get() * 50)))) {
                    sneakTimer.reset();
                }
            }
        }

        if (!safeWalkMode.is("Выкл")) {
            handleSafeWalk(eventInput);
        }

        if (!eagleMode.is("Выкл")) {
            handleEagle(eventInput);
        }

        // === NEW: Ledge (LB NextGen) ===
        if (ledge.get()) {
            BlockPos below = new BlockPos(mc.player.getPosX(), mc.player.getPosY() - 1.0, mc.player.getPosZ());
            if (mc.world.isAirBlock(below) || mc.player.fallDistance > 0.5f) {
                eventInput.setForward(0);
                eventInput.setStrafe(0);
                eventInput.setSneak(true);
            }
        }

        // === NEW: Slow (LB100) ===
        if (slow.get()) {
            if (!slowGround.get() || mc.player.isOnGround()) {
                eventInput.setForward(eventInput.getForward() * slowSpeed.get());
                eventInput.setStrafe(eventInput.getStrafe() * slowSpeed.get());
            }
        }
    }

    private void handleSafeWalk(EventInput eventInput) {
        if (!airSafe.get() && !mc.player.isOnGround()) return;
        BlockPos below = new BlockPos(
                mc.player.getPosX() + mc.player.getMotion().x,
                mc.player.getPosY() - 1.0,
                mc.player.getPosZ() + mc.player.getMotion().z);
        if (mc.world.getBlockState(below).getBlock() instanceof AirBlock) {
            eventInput.setSneak(true);
        }
    }

    private void handleEagle(EventInput eventInput) {
        if (!mc.player.isOnGround() || !MoveUtils.isMoving()) return;

        if (isOnEdge()) {
            if (eagleMode.is("Normal")) {
                if (blocksToEagle.get().intValue() <= 0 || placedBlocksUntilEagle >= blocksToEagle.get().intValue()) {
                    eagleSneaking = !eagleSneaking;
                    eagleTimer.reset();
                }
            } else if (eagleMode.is("Sneak")) {
                if (blocksToEagle.get().intValue() <= 0 || placedBlocksUntilEagle >= blocksToEagle.get().intValue()) {
                    eagleSneaking = true;
                }
            }
        } else {
            eagleSneaking = false;
        }

        if (eagleSneaking) {
            eventInput.setSneak(true);
        }
    }

    private boolean isOnEdge() {
        double x = mc.player.getPosX();
        double z = mc.player.getPosZ();
        double edgeDist = edgeDistance.get();
        double minX = Math.floor(x) + edgeDist;
        double maxX = Math.floor(x) + 1 - edgeDist;
        double minZ = Math.floor(z) + edgeDist;
        double maxZ = Math.floor(z) + 1 - edgeDist;
        return x <= minX || x >= maxX || z <= minZ || z >= maxZ;
    }

    @Subscribe
    private void onSendPacket(EventPacket e) {
        if (!e.isSend()) return;
        IPacket<?> p = e.getPacket();

        if (blink.get() && (p instanceof CPlayerPacket.PositionPacket || p instanceof CPlayerPacket.PositionRotationPacket || p instanceof CPlayerPacket.RotationPacket)) {
            e.cancel();
            blinkPacketBuffer.add(p);
            return;
        }

        if (p instanceof CHeldItemChangePacket && spoof.is("Fake") && !silentHeldSwap) {
            CHeldItemChangePacket held = (CHeldItemChangePacket) p;
            if (held.getSlotId() == spoofSlot) {
                e.cancel();
            } else {
                spoofSlot = held.getSlotId();
            }
        }

    }

    @Subscribe
    private void onReceivePacket(EventPacket e) {
        if (e.isSend()) return;
        IPacket<?> p = e.getPacket();

        // === Opal: Real Stack Size — track server-side slot updates & item pickups ===
        if (syncStackSize.get()) {
            if (p instanceof net.minecraft.network.play.server.SSetSlotPacket) {
                net.minecraft.network.play.server.SSetSlotPacket slot = (net.minecraft.network.play.server.SSetSlotPacket) p;
                int slotId = slot.getSlot();
                if (slotId >= 36 && slotId <= 44) {
                    realStackMap.put(slotId - 36, slot.getStack().getCount());
                } else if (slotId == 45) {
                    realStackMap.put(-1, slot.getStack().getCount());
                }
            }
            if (p instanceof net.minecraft.network.play.server.SWindowItemsPacket) {
                realStackMap.clear();
            }
            String pkg = p.getClass().getName();
            if (pkg.contains("ItemPickupAnimationS2CPacket") || pkg.contains("SPacketCollectItem")) {
                for (int i = 0; i < 9; i++) {
                    ItemStack stack = mc.player.inventory.getStackInSlot(i);
                    if (isValidBlock(stack)) {
                        int prev = realStackMap.getOrDefault(i, stack.getCount());
                        realStackMap.put(i, prev + 1);
                    }
                }
            }
        }
    }

    @Subscribe
    private void onUpdate(EventUpdate e) {
        // === NEW: Blink flush (LB NextGen) ===
        if (blink.get()) {
            long blinkInterval = (long) (blinkTimeMin.get() + random.nextFloat() * (blinkTimeMax.get() - blinkTimeMin.get()));
            if (blinkTimer.hasTimeElapsed(blinkInterval)) {
                for (IPacket<?> bp : blinkPacketBuffer) {
                    mc.player.connection.sendPacket(bp);
                }
                blinkPacketBuffer.clear();
                blinkTimer.reset();
            }
        }

        towerStatus = mc.gameSettings.keyBindJump.isKeyDown();

        // === LB NextGen: AutoBlock ===
        if (autoBlock.get()) {
            boolean shouldSwitch = autoBlockAlways.get() || blockPos != null;
            if (shouldSwitch) {
                int blockCnt = getBlockCount();
                if (blockCnt > autoBlockDoNotUseBelow.get().intValue()) {
                    ItemStack mainHandStack = mc.player.getHeldItemMainhand();
                    if (mainHandStack.isEmpty() || !(mainHandStack.getItem() instanceof BlockItem) || !isValidBlock(mainHandStack)) {
                        int prev = mc.player.inventory.currentItem;
                        switchToBlockSlot();
                        if (autoBlockSlotResetDelay.get().intValue() > 0 && mc.player.inventory.currentItem != prev) {
                            int delay = autoBlockSlotResetDelay.get().intValue();
                            mc.player.inventory.currentItem = prev;
                            mc.player.connection.sendPacket(new CHeldItemChangePacket(mc.player.inventory.currentItem));
                            ticks = -delay;
                        }
                    }
                }
            }
        }

        // === LB NextGen: SprintControl (client mode) ===
        if (sprintControl.get()) {
            String cm = sprintClientMode.get();
            switch (cm) {
                case "FORCE_SPRINT":
                    mc.player.setSprinting(true);
                    break;
                case "FORCE_NO_SPRINT":
                    mc.player.setSprinting(false);
                    break;
                case "NO_SPRINT_ON_PLACE":
                    if (lastPlacedBlockPos != null) mc.player.setSprinting(false);
                    break;
                case "NO_SPRINT_ON_GROUND":
                    if (mc.player.isOnGround()) mc.player.setSprinting(false);
                    break;
            }
        }

        ItemStack mainHand = mc.player.getHeldItemMainhand();
        if (!spoof.is("Fake") && (mainHand.isEmpty() || !(mainHand.getItem() instanceof BlockItem) || !isValidBlock(mainHand))) {
            switchToBlockSlot();
        }

        updateBlockCount();

        switch (yMode.get()) {
            case "On":
                if (mc.gameSettings.keyBindJump.isKeyDown()) {
                    posY = (int) (mc.player.getPosY() - 1);
                }
                if (MoveUtils.isMoving() && mc.player.isOnGround() && !mc.gameSettings.keyBindJump.isKeyDown()) {
                    performJump();
                }
                break;
            case "Falling":
                posY = (int) (mc.player.getPosY() - 1);
                if (MoveUtils.isMoving() && mc.player.isOnGround()) {
                    performJump();
                }
                break;
            case "Hypixel":
                if (mc.gameSettings.keyBindJump.isKeyDown()) {
                    posY = (int) (mc.player.getPosY() - 1);
                }
                if (MoveUtils.isMoving() && mc.player.isOnGround() && !mc.gameSettings.keyBindJump.isKeyDown()) {
                    performJump();
                }
                break;
            default:
                break;
        }

        offGroundTicks = mc.player.isOnGround() ? 0 : offGroundTicks + 1;

        // === LB NextGen: HeadHitter (авто-прыжок при ударе головой) ===
        if (headHitter.get()) {
            BlockPos headPos = new BlockPos(mc.player.getPosX(), mc.player.getPosY() + 1.0, mc.player.getPosZ());
            if (!mc.world.isAirBlock(headPos) && mc.player.isOnGround() && MoveUtils.isMoving()) {
                performJump();
            }
        }

        // === LB NextGen: Ceiling (блок над головой) ===
        if (ceiling.get() && mc.player.isOnGround()) {
            BlockPos headPos = new BlockPos(mc.player.getPosX(), mc.player.getPosY() + 2.0, mc.player.getPosZ());
            if (mc.world.isAirBlock(headPos)) {
                BlockPos support = headPos.up();
                if (!mc.world.isAirBlock(support)) {
                    Vector3d hv = getFaceHitVec(headPos, Direction.DOWN);
                    BlockRayTraceResult result = new BlockRayTraceResult(hv, Direction.DOWN, headPos, false);
                    mc.playerController.processRightClickBlock(mc.player, mc.world, Hand.MAIN_HAND, result);
                    mc.player.swingArm(Hand.MAIN_HAND);
                }
            }
        }

        // === LB NextGen: Blink FlushOn условия ===
        if (blink.get() && !blinkPacketBuffer.isEmpty()) {
            boolean shouldFlush = blinkFlushOnPlace.get() && lastPlacedBlockPos != null;
            if (blinkFlushOnTowering.get() && towerStatus) shouldFlush = true;
            if (blinkFlushOnSneaking.get() && mc.player.isSneaking()) shouldFlush = true;
            if (blinkFlushOnNotSneaking.get() && !mc.player.isSneaking()) shouldFlush = true;
            if (blinkFlushOnGround.get() && mc.player.isOnGround()) shouldFlush = true;
            if (blinkFlushOnAir.get() && !mc.player.isOnGround()) shouldFlush = true;
            if (shouldFlush) {
                for (IPacket<?> bp : blinkPacketBuffer) {
                    mc.player.connection.sendPacket(bp);
                }
                blinkPacketBuffer.clear();
                blinkTimer.reset();
            }
        }

        // === NEW: Down mode (FDP, LB100) ===
        if (down.get() && mc.gameSettings.keyBindSneak.isKeyDown() && !mc.gameSettings.keyBindJump.isKeyDown()) {
            BlockPos below = new BlockPos(mc.player.getPosX(), mc.player.getPosY() - 1.0, mc.player.getPosZ());
            if (!mc.world.isAirBlock(below)) {
                mc.player.setMotion(mc.player.getMotion().x, -0.5, mc.player.getMotion().z);
            }
        }

        // === NEW: AllowClutching (LB100) ===
        if (allowClutching.get() && !mc.player.isOnGround() && mc.player.getMotion().y < -0.5) {
            BlockPos downPos = new BlockPos(mc.player.getPosX(), mc.player.getPosY() - 2.0, mc.player.getPosZ());
            if (mc.world.isAirBlock(downPos)) {
                int clutchSlot = findBlockSlot();
                if (clutchSlot != -1) {
                    int prevSlot = mc.player.inventory.currentItem;
                    mc.player.inventory.currentItem = clutchSlot;
                    for (int y = 1; y <= verticalClutchBlocks.get(); y++) {
                        for (int hx = -horizontalClutchBlocks.get().intValue(); hx <= horizontalClutchBlocks.get().intValue(); hx++) {
                            for (int hz = -horizontalClutchBlocks.get().intValue(); hz <= horizontalClutchBlocks.get().intValue(); hz++) {
                                BlockPos bp = new BlockPos(mc.player.getPosX() + hx, mc.player.getPosY() - y, mc.player.getPosZ() + hz);
                                BlockPos support = bp.down();
                                if (!mc.world.isAirBlock(support) && mc.world.isAirBlock(bp)) {
                                    Vector3d clutchHv = getFaceHitVec(bp, Direction.UP);
                                    BlockRayTraceResult clutchResult = new BlockRayTraceResult(clutchHv, Direction.UP, bp, false);
                                    mc.playerController.processRightClickBlock(mc.player, mc.world, Hand.MAIN_HAND, clutchResult);
                                    mc.player.swingArm(Hand.MAIN_HAND);
                                }
                            }
                        }
                    }
                    mc.player.inventory.currentItem = prevSlot;
                }
            }
        }

        // === NEW: Jump Automatically (LB100) ===
        if (jumpAutomatically.get() && mc.player.isOnGround() && MoveUtils.isMoving()) {
            int jumpMin = blocksToJumpMin.get().intValue();
            int jumpMax = blocksToJumpMax.get().intValue();
            int jumpBlocks = jumpMin + random.nextInt(Math.max(jumpMax - jumpMin + 1, 1));
            if (autoJumpBlocksPlaced >= jumpBlocks) {
                performJump();
                autoJumpBlocksPlaced = 0;
            }
        }

        if (placementMode.is("Telly") || placementMode.is("RiseTelly")) {
            // === LB NextGen: Telly expanded features ===
            if (mc.player.isOnGround()) {
                // ResetMode: reset blocks count on land
                if (tellyResetMode.is("ON_LAND") && mc.player.isOnGround()) {
                    tellyPlacedBlocks = 0;
                    autoJumpBlocksPlaced = 0;
                }
                // Jump logic
                boolean shouldJump = false;
                if (placementMode.is("Telly")) {
                    tellyGroundPosY = (int) (mc.player.getPosY() - 1);
                    keepRotationTicks = 0;
                    shouldJump = true;
                }
                if (placementMode.is("RiseTelly")) {
                    if (delayedRiseTelly.get()) {
                        if (riseTellyBlocksPlaced >= delayedRiseTellyBlocks.get().intValue()) {
                            shouldJump = true;
                            riseTellyBlocksPlaced = 0;
                        }
                    } else {
                        shouldJump = true;
                    }
                }
                // JumpTicks range
                if (shouldJump && MoveUtils.isMoving()) {
                    int jMin = tellyJumpTicksMin.get().intValue();
                    int jMax = tellyJumpTicksMax.get().intValue();
                    if (jMax < jMin) jMax = jMin;
                    int jumpDelayTicks = jMin + random.nextInt(Math.max(jMax - jMin + 1, 1));
                    if (tellyJumpTicks >= jumpDelayTicks) {
                        performJump();
                        tellyJumpTicks = 0;
                    }
                    tellyJumpTicks++;
                }
                // StraightTicks: only place straight for N ticks after landing
                if (tellyStraightTicks.get().intValue() > 0 && placementMode.is("Telly")) {
                    // straight placement is handled in searchBlock by looking behind
                }
            }
            // AimOnTower: aim yaw at tower position
            if (tellyAimOnTower.get() && !mc.player.isOnGround() && tellyGroundPosY != -1) {
                double tdx = mc.player.getPosX() - (new BlockPos(mc.player.getPosX(), tellyGroundPosY, mc.player.getPosZ())).getX();
                double tdz = mc.player.getPosZ() - (new BlockPos(mc.player.getPosX(), tellyGroundPosY, mc.player.getPosZ())).getZ();
                float towerYaw = (float) Math.toDegrees(Math.atan2(-tdx, tdz));
                tellyAimYaw = towerYaw;
            }
        }

        ticksExisted = mc.player.ticksExisted;

        // === LB NextGen: GodBridge ===
        boolean godBridgeOverride = false;
        if (godBridge.get() && mc.player.isOnGround()) {
            // StopInput (zero out movement)
            if (godBridgeStopInput.get()) {
                mc.player.movementInput.moveForward = 0;
                mc.player.movementInput.moveStrafe = 0;
            }

            // Backwards (invert forward movement)
            if (godBridgeBackwards.get()) {
                mc.player.movementInput.moveForward = -Math.abs(mc.player.movementInput.moveForward);
            }

            // SneakTime: start sneaking after blocks placed
            if (godBridgeSneakTime.get().intValue() > 0 && placedBlocksUntilEagle >= godBridgeSneakTime.get().intValue()) {
                mc.player.setSneaking(true);
            }

            // ForceSneakBelow: sneak when near edge
            if (godBridgeForceSneakBelow.get()) {
                BlockPos gbBelow = new BlockPos(mc.player.getPosX(), mc.player.getPosY() - 1.0, mc.player.getPosZ());
                if (mc.world.isAirBlock(gbBelow)) {
                    mc.player.setSneaking(true);
                }
            }

            // Block placement ahead
            double gbYawRad = Math.toRadians(mc.player.rotationYaw);
            double gbDx = -Math.sin(gbYawRad) * 0.3;
            double gbDz = Math.cos(gbYawRad) * 0.3;
            BlockPos gbTarget = new BlockPos(
                mc.player.getPosX() + gbDx,
                mc.player.getPosY() - 2.0,
                mc.player.getPosZ() + gbDz
            );
            BlockPos gbSupport = new BlockPos(
                mc.player.getPosX() + gbDx,
                mc.player.getPosY() - 1.0,
                mc.player.getPosZ() + gbDz
            );
            if (!mc.world.isAirBlock(gbSupport) && mc.world.isAirBlock(gbTarget)) {
                PlaceInfo gbInfo = findBestPlaceInfo(gbTarget);
                if (gbInfo != null) {
                    placeInfo = gbInfo;
                    godBridgeOverride = true;
                    if (godBridgeJump.get() && mc.player.isOnGround() && MoveUtils.isMoving()) {
                        performJump();
                    }
                    if (godBridgeSneakAct.get() && mc.player.isOnGround()) {
                        mc.player.setSneaking(true);
                    }
                }
            }
        }

        // === Opal: Player Simulation (Predict) ===
        boolean predInUse = false;
        if (playerSimulation.get() && mc.player.isOnGround() && MoveUtils.isMoving()) {
            int predTicks = predictTicks.get().intValue();
            double predX = mc.player.getPosX() + mc.player.getMotion().x * predTicks;
            double predZ = mc.player.getPosZ() + mc.player.getMotion().z * predTicks;
            BlockPos predPos = new BlockPos(predX, mc.player.getPosY(), predZ).down();
            if (!mc.world.isAirBlock(predPos.down()) && mc.world.isAirBlock(predPos)) {
                PlaceInfo predInfo = findBestPlaceInfo(predPos);
                if (predInfo != null) {
                    placeInfo = predInfo;
                    predInUse = true;
                }
            }
        }
        if (!predInUse) {
            if (!godBridgeOverride) {
                searchBlock();
                processBlockData();
            } else {
                processBlockData();
            }
        }

        // === LB NextGen: Breezily edge randomization ===
        if (breezily.get() && blockPos != null && mc.player.isOnGround()) {
            double edgeDist = breezilyEdgeDistance.get();
            if (edgeDist > 0) {
                double bYawRad = Math.toRadians(mc.player.rotationYaw);
                double offsetX = -Math.sin(bYawRad) * random.nextDouble() * edgeDist;
                double offsetZ = Math.cos(bYawRad) * random.nextDouble() * edgeDist;
                BlockPos bzTarget = new BlockPos(
                    blockPos.getX() + (int) Math.round(offsetX),
                    blockPos.getY(),
                    blockPos.getZ() + (int) Math.round(offsetZ)
                );
                if (!mc.world.isAirBlock(bzTarget.down()) && mc.world.isAirBlock(bzTarget)) {
                    PlaceInfo bzInfo = findBestPlaceInfo(bzTarget);
                    if (bzInfo != null) {
                        placeInfo = bzInfo;
                    }
                }
            }
        }
        handleRotations();

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

        // === LB NextGen: Strafe ===
        if (strafe.get() && MoveUtils.isMoving()) {
            if (!strafeOnlyGround.get() || mc.player.isOnGround()) {
                double yawRad = Math.toRadians(mc.player.rotationYaw);
                if (strafeHypixel.get()) {
                    yawRad = Math.toRadians(MoveUtils.getDirection());
                }
                double mx = -Math.sin(yawRad) * strafeSpeed.get();
                double mz = Math.cos(yawRad) * strafeSpeed.get();
                mc.player.setMotion(mx, mc.player.getMotion().y, mz);
            }
        }

        // === Opal: Movement Intelligence (MI) ===
        if (movementIntelligence.get() && mc.player.isOnGround() && MoveUtils.isMoving()) {
            double miYaw = Math.toRadians(mc.player.rotationYaw);
            boolean diag = miDiagonal.get() && mc.player.movementInput.moveStrafe != 0 && mc.player.movementInput.moveForward != 0;

            if (diag) {
                double snapX = Math.round(mc.player.getPosX() * 2) / 2.0;
                double snapZ = Math.round(mc.player.getPosZ() * 2) / 2.0;
                if (miSnap.get()) {
                    mc.player.setPosition(snapX, mc.player.getPosY(), snapZ);
                }
            } else {
                double snapX = Math.round(mc.player.getPosX());
                double snapZ = Math.round(mc.player.getPosZ());
                if (miSnap.get()) {
                    mc.player.setPosition(snapX, mc.player.getPosY(), snapZ);
                }
            }
        }

        // === NEW: SpeedLimiter (перенесён из удалённого sprint()) ===
        if (speedLimiter.get() && MoveUtils.isMoving()) {
            double speed = Math.sqrt(mc.player.getMotion().x * mc.player.getMotion().x + mc.player.getMotion().z * mc.player.getMotion().z);
            if (speed > speedLimit.get()) {
                double ratio = speedLimit.get() / speed;
                mc.player.setMotion(mc.player.getMotion().x * ratio, mc.player.getMotion().y, mc.player.getMotion().z * ratio);
            }
        }

        if (dragClick.get()) {
            fakeClick();
        }

        if (modernDelay.get()) {
            // === Opal: Modern Delay — tick-based placement, independent of CPS ===
            int modernDelayMs = getPlaceDelay();
            if (modernDelayMs < 50) modernDelayMs = 50;
            if (placeTimer.hasTimeElapsed(modernDelayMs)) {
                place();
                placeTimer.reset();
                updateBlockCount();
            }
        } else {
            int baseDelay = mc.gameSettings.keyBindJump.isKeyDown() && towerMode.is("Timer") ? 0
                    : (int) (1000 / cps.get());
            int extraDelay = getPlaceDelay();
            if (placeTimer.hasTimeElapsed(baseDelay + extraDelay)) {
                place();
                placeTimer.reset();
                updateBlockCount();
            }
        }

        handleScaffoldTimer();
    }

    // === CHECK IF ROTATION IS DISABLED ===

    private boolean isRotationNone() {
        return rotationMode.is("None");
    }

    // === BLOCK FINDING (im.quantum — точный перебор) ===

    private void searchBlock() {
        if (expand.get() > 0) return;

        // === NEW: Block Safe (LB100) ===
        if (blockSafe.get()) {
            BlockPos below = new BlockPos(mc.player.getPosX(), mc.player.getPosY() - 1.0, mc.player.getPosZ());
            if (mc.world.isAirBlock(below) && !mc.player.isOnGround()) {
                placeInfo = findBestPlaceInfo(new BlockPos(mc.player.getPosX(), mc.player.getPosY(), mc.player.getPosZ()).down());
                if (placeInfo != null) return;
            }
        }

        BlockPos playerPos = getPlayerPos();

        if (!(mc.world.getBlockState(playerPos).getBlock() instanceof AirBlock)) {
            placeInfo = null;
            return;
        }

        // === Telly + RiseTelly ===
        boolean tellyActive = (placementMode.is("Telly") || placementMode.is("RiseTelly")) && tellyGroundPosY != -1;
        if (tellyActive && !mc.player.isOnGround() && offGroundTicks >= tellyJumpDelay.get().intValue()) {
            // StraightTicks: place at feet for first N ticks after landing
            int straightTicks = tellyStraightTicks.get().intValue();
            if (straightTicks > 0 && tellyJumpTicks >= 0 && tellyJumpTicks < straightTicks) {
                BlockPos straightPos = new BlockPos(mc.player.getPosX(), tellyGroundPosY, mc.player.getPosZ());
                placeInfo = findBestPlaceInfo(straightPos);
                if (placeInfo != null) return;
            }
            // Normal behind placement
            double moveRad = MoveUtils.direction(mc.player.rotationYaw,
                    mc.player.movementInput.moveForward, mc.player.movementInput.moveStrafe);
            BlockPos behindPos = new BlockPos(
                    mc.player.getPosX() - Math.sin(moveRad) * 1.0,
                    tellyGroundPosY,
                    mc.player.getPosZ() + Math.cos(moveRad) * 1.0
            );
            placeInfo = findBestPlaceInfo(behindPos);
            if (placeInfo != null) return;
        }

        placeInfo = findBestPlaceInfo(playerPos);

        if (placeInfo == null && search.get()) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && z == 0) continue;
                    PlaceInfo info = findBestPlaceInfo(playerPos.add(x, 0, z));
                    if (info != null) {
                        placeInfo = info;
                        return;
                    }
                }
            }
        }
    }

    private BlockPos getPlayerPos() {
        if ((yMode.is("On") || yMode.is("Hypixel")) && !towerStatus) {
            if (mc.player.isOnGround()) {
                lastY = mc.player.getPosY();
            }
            return new BlockPos(mc.player.getPosX(), lastY, mc.player.getPosZ()).down();
        }
        return new BlockPos(mc.player.getPosX(), mc.player.getPosY(), mc.player.getPosZ()).down();
    }

    private PlaceInfo findBestPlaceInfo(BlockPos pos) {
        PlaceInfo bestInfo = null;
        double bestDistance = Double.MAX_VALUE;

        // 1. Пробуем кликнуть по твёрдому блоку, чтобы поставить НА него (через UP)
        for (Direction dir : Direction.values()) {
            BlockPos offsetPos = pos.offset(dir);
            if (!canPlaceOn(offsetPos)) continue;

            if (searchMode.is("Lazy")) {
                Vector3d hitVec = new Vector3d(offsetPos.getX() + 0.5, offsetPos.getY() + 0.5, offsetPos.getZ() + 0.5);
                PlaceInfo info = new PlaceInfo(offsetPos, dir.getOpposite(), hitVec, calculateRotation(hitVec));
                if (isValidPlacement(info)) {
                    double distance = mc.player.getEyePosition(1.0f).distanceTo(hitVec);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        bestInfo = info;
                    }
                }
            } else {
                double xStart = searchMode.is("Center") ? 0.5 : 0.3;
                double xEnd = searchMode.is("Center") ? 0.5 : 0.7;
                double step = searchMode.is("Center") ? 0.0 : 0.2;
                for (double x = xStart; x <= xEnd; x += (step > 0 ? step : 0.01)) {
                    for (double y = 0.3; y <= 0.7; y += (step > 0 ? step : 0.4)) {
                        for (double z = 0.3; z <= 0.7; z += (step > 0 ? step : 0.01)) {
                            Vector3d hitVec = new Vector3d(offsetPos.getX() + x, offsetPos.getY() + y, offsetPos.getZ() + z);
                            PlaceInfo info = new PlaceInfo(offsetPos, dir.getOpposite(), hitVec, calculateRotation(hitVec));
                            double distance = mc.player.getEyePosition(1.0f).distanceTo(hitVec);
                            if (distance < bestDistance && isValidPlacement(info)) {
                                bestDistance = distance;
                                bestInfo = info;
                            }
                        }
                    }
                }
            }
        }

        // 2. Если pos — solid (игрок на земле), ищем air-блоки рядом (край моста)
        if (canPlaceOn(pos)) {
            for (Direction dir : Direction.values()) {
                if (dir == Direction.UP || dir == Direction.DOWN) continue;
                BlockPos edgePos = pos.offset(dir);
                if (mc.world.isAirBlock(edgePos)) {
                    Vector3d hitVec = getFaceHitVec(pos, dir);
                    PlaceInfo info = new PlaceInfo(pos, dir, hitVec, calculateRotation(hitVec));
                    if (isValidPlacement(info)) {
                        double distance = mc.player.getEyePosition(1.0f).distanceTo(hitVec);
                        if (distance < bestDistance) {
                            bestDistance = distance;
                            bestInfo = info;
                        }
                    }
                }
            }
        }

        return bestInfo;
    }

    private boolean canPlaceOn(BlockPos pos) {
        Block block = mc.world.getBlockState(pos).getBlock();
        return !(block instanceof AirBlock) && !(block instanceof FlowingFluidBlock);
    }

    private boolean isValidPlacement(PlaceInfo info) {
        double dist = mc.player.getEyePosition(1.0f).distanceTo(info.hitVec);
        return dist <= range.get() && dist >= minDist.get();
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

    // === PROCESS BLOCK DATA ===

    private void processBlockData() {
        if (placeInfo != null) {
            blockPos = placeInfo.blockPos;
            facing = placeInfo.facing;
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

    private Vector3d expandVec(Vector3d position) {
        int exp = expand.get().intValue();
        if (exp > 0) {
            final double yawRad = Math.toRadians(mc.player.rotationYaw);
            Vector3d expandVector = new Vector3d(-Math.sin(yawRad), 0, Math.cos(yawRad));
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

    // === FALLBACK BLOCK FINDING ===

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
            BlockPos bp = blockPos.up();
            Vector3d v = getBestHitFeet(bp);
            positions.add(v);
            map.put(v, Direction.UP);
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

    private boolean isPosSolid(BlockPos pos) {
        Block block = mc.world.getBlockState(pos).getBlock();
        return !(block instanceof AirBlock) && !(block instanceof FlowingFluidBlock);
    }

    private boolean isValidWorldBlock(BlockPos pos) {
        Block block = mc.world.getBlockState(pos).getBlock();
        return !(block instanceof AirBlock) && !(block instanceof FlowingFluidBlock)
                && block != Blocks.CHEST && block != Blocks.FURNACE;
    }

    private Vector3d getBestHitFeet(BlockPos bp) {
        return new Vector3d(
                MathHelper.clamp(mc.player.getPosX(), bp.getX(), bp.getX() + 1.0),
                MathHelper.clamp(mc.player.getPosY(), bp.getY(), bp.getY() + 1.0),
                MathHelper.clamp(mc.player.getPosZ(), bp.getZ(), bp.getZ() + 1.0)
        );
    }

    // === ROTATIONS (объединённая: old smooth + new lookingAtBlock) ===

    private void handleRotations() {
        if (placementMode.is("Telly")) {
            if (blockPos == null || facing == null) {
                rotationVector = new Vector2f(mc.player.rotationYaw, mc.player.rotationPitch);
                lastYaw = mc.player.rotationYaw;
                lastPitch = mc.player.rotationPitch;
                return;
            }
            handleNewRotation();
            return;
        }
        if (isRotationNone()) return;
        handleNewRotation();
    }

    private void handleNewRotation() {
        if (blockPos == null || facing == null) return;

        // === LB NextGen: ConsiderInventory ===
        if (considerInventory.get() && mc.currentScreen instanceof ContainerScreen) {
            return;
        }

        if (keepRotationTicks > 0) {
            keepRotationTicks--;
            if (keepRotationTicks > 0) return;
        }

        // === LB NextGen: Telly AimOnTower ===
        if (tellyAimOnTower.get() && (placementMode.is("Telly") || placementMode.is("RiseTelly")) && !mc.player.isOnGround() && tellyGroundPosY != -1) {
            rotationVector = new Vector2f(tellyAimYaw, 90f);
            return;
        }

        boolean stop = false;

        float currentYaw = (float) Math.toDegrees(
                MoveUtils.direction(mc.player.rotationYaw,
                        mc.player.movementInput.moveForward, mc.player.movementInput.moveStrafe));
        float currentPitch = lastPitch;

        Vector3d hit = getFaceHitVec(blockPos, facing);
        Vector2f to = calculateRotation(hit);

        switch (rotationMode.get()) {
            case "Default":
                if (placementMode.is("Telly") && !mc.player.isOnGround()) {
                    int delay = tellyJumpDelay.get().intValue();
                    if (offGroundTicks >= delay) {
                        scanTellyRotations();
                        scaffoldYaw = mc.player.rotationYaw - 180f;
                        currentYaw = scaffoldYaw;
                        currentPitch = scaffoldPitch;
                        break;
                    }
                }
                currentYaw = to.x;
                currentPitch = to.y;
                break;
            case "Direct":
                currentYaw += 180;
                for (float pitch = 90; pitch > 30; pitch -= 1) {
                    if (lookingAtBlock(blockPos, facing, new Vector2f(mc.player.rotationYaw, pitch))) {
                        currentPitch = pitch;
                        break;
                    }
                }
                break;
            case "Snap":
                currentYaw = to.x;
                currentPitch = to.y;
                break;
            case "Keep":
                scaffoldYaw = to.x;
                scaffoldPitch = to.y;
                currentYaw = scaffoldYaw;
                currentPitch = scaffoldPitch;
                break;
            case "Intave":
            case "God": {
                float add = rotationMode.is("God") ? 45f : 0f;
                currentYaw += add;
                float godPitch = useOptimizedPitch.get() ? godBridgePitch.get() : to.y;
                scaffoldPitch = godPitch;
                if (!lookingAtBlock(blockPos, facing, new Vector2f(currentYaw, scaffoldPitch))) {
                    int maxTicks = (int) Math.abs(wrapDegrees(scaffoldYaw - to.x) / 4);
                    int t = 0;
                    while (t <= maxTicks && !stop) {
                        scaffoldYaw = updateRotationFloat(scaffoldYaw, to.x, 5f);
                        scaffoldPitch = godPitch;
                        if (lookingAtBlock(blockPos, facing, new Vector2f(scaffoldYaw, scaffoldPitch))) {
                            stop = true;
                        }
                        t++;
                    }
                }
                if (!stop) {
                    if (ticksExisted == mc.player.ticksExisted) {
                        scaffoldYaw = updateRotationFloat(scaffoldYaw, currentYaw, getRotationSpeed());
                        ticksExisted++;
                    }
                }
                currentYaw = scaffoldYaw;
                currentPitch = scaffoldPitch;
                break;
            }
            case "Static yaw":
            case "Static yaw god": {
                float add = rotationMode.is("Static yaw god") ? 45f : 0f;
                currentYaw += add;
                float staticPitch = (rotationMode.is("Static yaw god") && useOptimizedPitch.get()) ? godBridgePitch.get() : to.y;
                if (!lookingAtBlock(blockPos, facing, new Vector2f(currentYaw, lastPitch))) {
                    scaffoldPitch = staticPitch;
                }
                if (ticksExisted == mc.player.ticksExisted) {
                    scaffoldYaw = updateRotationFloat(scaffoldYaw, currentYaw, getRotationSpeed());
                    ticksExisted++;
                }
                currentYaw = scaffoldYaw;
                currentPitch = scaffoldPitch;
                break;
            }
            case "Static god":
                currentYaw += 45f;
                boolean diagonalG = (mc.player.movementInput.moveForward != 0 && mc.player.movementInput.moveStrafe != 0);
                scaffoldPitch = useOptimizedPitch.get() ? godBridgePitch.get() : (diagonalG ? 75.5f : 77f);
                if (ticksExisted == mc.player.ticksExisted) {
                    scaffoldYaw = updateRotationFloat(scaffoldYaw, currentYaw, getRotationSpeed());
                    ticksExisted++;
                }
                currentYaw = scaffoldYaw;
                currentPitch = scaffoldPitch;
                break;
            case "Static":
                boolean diagonalS = (mc.player.movementInput.moveForward != 0 && mc.player.movementInput.moveStrafe != 0);
                scaffoldPitch = diagonalS ? 77.5f : 79.5f;
                if (ticksExisted == mc.player.ticksExisted) {
                    scaffoldYaw = updateRotationFloat(scaffoldYaw, currentYaw, getRotationSpeed());
                    ticksExisted++;
                }
                currentYaw = scaffoldYaw;
                currentPitch = scaffoldPitch;
                break;
            case "Polar":
                if (!lookingAtBlock(blockPos, facing, new Vector2f(currentYaw, lastPitch))) {
                    scaffoldPitch = to.y;
                }
                if (ticksExisted == mc.player.ticksExisted) {
                    scaffoldYaw = updateRotationFloat(scaffoldYaw, currentYaw, getRotationSpeed());
                    ticksExisted++;
                }
                if (lastPitch != scaffoldPitch) mc.player.rotationYaw += Math.random() - Math.random();
                if (lastYaw != scaffoldYaw) scaffoldPitch += Math.random() - Math.random();
                currentYaw = scaffoldYaw;
                currentPitch = scaffoldPitch;
                break;
            // === FDP 5.4.0 rotations ===
            case "Better":
                currentYaw = to.x;
                if (!lookingAtBlock(blockPos, facing, new Vector2f(currentYaw, lastPitch))) {
                    scaffoldPitch = to.y;
                }
                if (ticksExisted == mc.player.ticksExisted) {
                    scaffoldYaw = updateRotationFloat(scaffoldYaw, currentYaw, getRotationSpeed());
                    ticksExisted++;
                }
                currentYaw = scaffoldYaw;
                currentPitch = scaffoldPitch;
                break;
            case "AAC":
                currentYaw = to.x + 180f;
                currentPitch = to.y;
                break;
            case "Vanilla":
                currentYaw = to.x;
                currentPitch = to.y;
                break;
            case "Static1":
                scaffoldPitch = 83.5f;
                if (ticksExisted == mc.player.ticksExisted) {
                    scaffoldYaw = updateRotationFloat(scaffoldYaw, currentYaw, getRotationSpeed());
                    ticksExisted++;
                }
                currentYaw = scaffoldYaw;
                currentPitch = scaffoldPitch;
                break;
            case "Static2":
                scaffoldPitch = 78f;
                if (ticksExisted == mc.player.ticksExisted) {
                    scaffoldYaw = updateRotationFloat(scaffoldYaw, currentYaw, getRotationSpeed());
                    ticksExisted++;
                }
                currentYaw = scaffoldYaw;
                currentPitch = scaffoldPitch;
                break;
            case "Custom":
                currentYaw = mc.player.rotationYaw + customYaw.get();
                currentPitch = customPitch.get();
                break;
            case "Advanced":
                if (!lookingAtBlock(blockPos, facing, new Vector2f(currentYaw, lastPitch))) {
                    scaffoldPitch = to.y;
                }
                if (ticksExisted == mc.player.ticksExisted) {
                    scaffoldYaw = updateRotationFloat(scaffoldYaw, currentYaw, getRotationSpeed());
                    ticksExisted++;
                }
                currentYaw = scaffoldYaw;
                currentPitch = scaffoldPitch;
                break;
            case "Backwards":
                currentYaw = mc.player.rotationYaw + 180f;
                currentPitch = to.y;
                break;
            case "Snap FDP":
                currentYaw = to.x;
                currentPitch = to.y;
                break;
            case "BackSnap":
                currentYaw = mc.player.rotationYaw + 180f;
                if (!lookingAtBlock(blockPos, facing, new Vector2f(currentYaw, lastPitch))) {
                    scaffoldPitch = to.y;
                }
                currentPitch = scaffoldPitch;
                break;
            // === LB b100 rotations ===
            case "Normal lb100":
                currentYaw = to.x;
                if (!lookingAtBlock(blockPos, facing, new Vector2f(currentYaw, lastPitch))) {
                    scaffoldPitch = to.y;
                }
                if (ticksExisted == mc.player.ticksExisted) {
                    scaffoldYaw = updateRotationFloat(scaffoldYaw, currentYaw, getRotationSpeed());
                    ticksExisted++;
                }
                currentYaw = scaffoldYaw;
                currentPitch = scaffoldPitch;
                break;
            case "Stabilized":
                if (!lookingAtBlock(blockPos, facing, new Vector2f(currentYaw, lastPitch))) {
                    scaffoldPitch = to.y;
                }
                if (ticksExisted == mc.player.ticksExisted) {
                    scaffoldYaw = updateRotationFloat(scaffoldYaw, currentYaw, Math.min(getRotationSpeed(), 2f));
                    ticksExisted++;
                }
                currentYaw = scaffoldYaw;
                currentPitch = scaffoldPitch;
                break;
            case "ReverseYaw":
                currentYaw = mc.player.rotationYaw + 180f;
                if (!lookingAtBlock(blockPos, facing, new Vector2f(currentYaw, lastPitch))) {
                    scaffoldPitch = to.y;
                }
                if (ticksExisted == mc.player.ticksExisted) {
                    scaffoldYaw = updateRotationFloat(scaffoldYaw, currentYaw, getRotationSpeed());
                    ticksExisted++;
                }
                currentYaw = scaffoldYaw;
                currentPitch = scaffoldPitch;
                break;
            case "Off":
                break;
            // === LB NextGen rotations ===
            case "Normal lbNext":
                currentYaw = to.x;
                if (!lookingAtBlock(blockPos, facing, new Vector2f(currentYaw, lastPitch))) {
                    scaffoldPitch = to.y;
                }
                if (ticksExisted == mc.player.ticksExisted) {
                    scaffoldYaw = updateRotationFloat(scaffoldYaw, currentYaw, getRotationSpeed());
                    ticksExisted++;
                }
                currentYaw = scaffoldYaw;
                currentPitch = scaffoldPitch;
                break;
            case "OnTick":
                currentYaw = to.x;
                currentPitch = to.y;
                break;
            case "OnTickSnap":
                currentYaw = to.x;
                currentPitch = to.y;
                break;
            // === Opal: Organic rotation ===
            case "Organic":
                if (ticksExisted == mc.player.ticksExisted) {
                    float drift = organicDrift.get();
                    float jitter = organicJitter.get();
                    float organicSpeed = getRotationSpeed() * drift;
                    float yawTarget = to.x;
                    float pitchTarget = to.y;
                    if (jitter > 0) {
                        yawTarget += (float) ((Math.random() - 0.5) * 2.0 * jitter);
                        pitchTarget += (float) ((Math.random() - 0.5) * 2.0 * jitter);
                    }
                    scaffoldYaw = updateRotationFloat(scaffoldYaw, yawTarget, organicSpeed);
                    scaffoldPitch = updateRotationFloat(scaffoldPitch, pitchTarget, organicSpeed);
                    ticksExisted++;
                }
                currentYaw = scaffoldYaw;
                currentPitch = scaffoldPitch;
                break;
        }

        rotationVector = new Vector2f(currentYaw, currentPitch);
        lastYaw = currentYaw;
        lastPitch = currentPitch;

    }

    private void scanTellyRotations() {
        float yawOffset = 0f;
        boolean found = false;
        for (float yaw = mc.player.rotationYaw - 180f + yawOffset;
             yaw <= mc.player.rotationYaw + 360f - 180f + yawOffset && !found;
             yaw += 45f) {
            for (float pitch = 90f; pitch > 30f && !found;
                 pitch -= pitch > 80f ? 1f : 10f) {
                if (lookingAtBlock(blockPos, facing, new Vector2f(yaw, pitch))) {
                    scaffoldYaw = yaw;
                    scaffoldPitch = pitch;
                    found = true;
                }
            }
        }
        if (!found) {
            Vector3d hit = getFaceHitVec(blockPos, facing);
            Vector2f to = calculateRotation(hit);
            scaffoldYaw = to.x;
            scaffoldPitch = to.y;
        }
    }

    private float updateRotationFloat(float current, float target, float speed) {
        float diff = wrapDegrees(target - current);
        if (diff > speed) diff = speed;
        if (diff < -speed) diff = -speed;
        return current + diff;
    }

    private float getRotationSpeed() {
        float min = rotationSpeedMin.get();
        float max = rotationSpeedMax.get();
        if (max < min) max = min;
        return min + random.nextFloat() * (max - min);
    }

    private int getPlaceDelay() {
        int min = placeDelayMin.get().intValue();
        int max = placeDelayMax.get().intValue();
        if (max < min) max = min;
        return min + (max > min ? random.nextInt(max - min + 1) : 0);
    }

    private boolean lookingAtBlock(BlockPos pos, Direction face, Vector2f rot) {
        Vector3d eyes = mc.player.getEyePosition(1.0f);
        Vector3d look = getLookVector(rot);
        float reachDist = range.get() + expand.get();
        Vector3d reach = eyes.add(look.scale(reachDist));
        RayTraceContext ctx = new RayTraceContext(eyes, reach, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, mc.player);
        BlockRayTraceResult res = mc.world.rayTraceBlocks(ctx);
        return res != null && res.getType() == RayTraceResult.Type.BLOCK && res.getPos().equals(pos) && res.getFace() == face;
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

    // === TOWER ===

    private void handleTower(EventMotion e) {
        String mode = towerMode.get();

        if (mode.equals("None")) {
            if (mc.gameSettings.keyBindJump.isKeyDown()) {
                mc.player.setMotion(mc.player.getMotion().x, 0, mc.player.getMotion().z);
            }
            return;
        }

        if (mode.equals("Timer")) {
            if (mc.gameSettings.keyBindJump.isKeyDown()) {
                placeTimer.reset();
            }
            return;
        }

        if (mode.equals("Intave")) {
            if (mc.gameSettings.keyBindJump.isKeyDown() && mc.player.isOnGround()) {
                mc.player.setMotion(mc.player.getMotion().x, 0.41D, mc.player.getMotion().z);
            }
            return;
        }

        // === NEW: Tower Stop When Block Above (FDP) ===
        if (towerStopWhenBlockAbove.get()) {
            BlockPos above = new BlockPos(mc.player.getPosX(), mc.player.getPosY() + 1.0, mc.player.getPosZ());
            if (!mc.world.isAirBlock(above)) {
                towerState = 0;
                return;
            }
        }

        if (!towerStatus) {
            towerState = 0;
            return;
        }

        switch (mode) {
            case "Jump":
                if (mc.player.isOnGround()) {
                    mc.player.setMotion(mc.player.getMotion().x, towerJumpMotion.get(), mc.player.getMotion().z);
                }
                break;
            case "Matrix":
                handleMatrixTower(e);
                break;
            case "Packet":
                if (mc.player.isOnGround()) {
                    towerTicks++;
                    if (towerTicks >= 2) {
                        mc.player.connection.sendPacket(new CPlayerPacket.PositionPacket(
                                mc.player.getPosX(), mc.player.getPosY() + 0.42, mc.player.getPosZ(), false));
                        mc.player.connection.sendPacket(new CPlayerPacket.PositionPacket(
                                mc.player.getPosX(), mc.player.getPosY() + 0.753, mc.player.getPosZ(), false));
                        mc.player.setPosition(mc.player.getPosX(), mc.player.getPosY() + 1.0, mc.player.getPosZ());
                        towerTicks = 0;
                    }
                }
                break;
            case "MotionTP":
                if (mc.player.isOnGround()) {
                    mc.player.setMotion(mc.player.getMotion().x, 0.42, mc.player.getMotion().z);
                } else if (mc.player.getMotion().y < 0.23) {
                    mc.player.setPosition(mc.player.getPosX(), Math.floor(mc.player.getPosY()), mc.player.getPosZ());
                }
                break;
        }
    }

    private void handleMatrixTower(EventMotion e) {
        double jumpMotion = towerJumpMotion.get();
        double stableMotion = towerStableMotion.get();
        switch (towerState) {
            case 0:
                if (!mc.player.isOnGround()) towerState = 1;
                break;
            case 1:
                if (mc.player.isOnGround()) towerState = 2;
                break;
            case 2:
                if (mc.player.isOnGround()) {
                    mc.player.setMotion(mc.player.getMotion().x, jumpMotion, mc.player.getMotion().z);
                    break;
                }
                if (mc.player.getMotion().y < stableMotion) {
                    e.setOnGround(true);
                    mc.player.setMotion(mc.player.getMotion().x, jumpMotion, mc.player.getMotion().z);
                }
                break;
        }
    }

    // === SPRINT (удалён — используется SprintControl) ===

    // === TIMER ===

    private void handleScaffoldTimer() {
        if (timerMode.is("Выкл")) {
            mc.timer.timerSpeed = 1.0f;
            return;
        }

        if (timerStopOnHurt.get() && mc.player.hurtTime > 0) {
            mc.timer.timerSpeed = 1.0f;
            return;
        }
        if (timerStopInWater.get() && (mc.player.isInWater() || mc.player.isInLava())) {
            mc.timer.timerSpeed = 1.0f;
            return;
        }

        switch (timerMode.get()) {
            case "Постоянный":
                mc.timer.timerSpeed = timerSpeed.get();
                break;
            case "Прерывистый": {
                long fast = timerFastTime.get().intValue();
                long slow = timerSlowTime.get().intValue();
                if (timerPulseFast) {
                    mc.timer.timerSpeed = timerSpeed.get();
                    if (timerPulseTimer.hasTimeElapsed(fast)) {
                        timerPulseFast = false;
                        timerPulseTimer.reset();
                    }
                } else {
                    mc.timer.timerSpeed = timerSlowSpeed.get();
                    if (timerPulseTimer.hasTimeElapsed(slow)) {
                        timerPulseFast = true;
                        timerPulseTimer.reset();
                    }
                }
                break;
            }
            case "Движение":
                if (MoveUtils.isMoving()) {
                    mc.timer.timerSpeed = timerSpeed.get();
                } else {
                    mc.timer.timerSpeed = 1.0f;
                }
                break;
            case "Прыжок":
                boolean condition = timerOnlyOnGround.get()
                        ? mc.player.isOnGround()
                        : !mc.player.isOnGround();
                if (condition) {
                    mc.timer.timerSpeed = timerSpeed.get();
                } else {
                    mc.timer.timerSpeed = 1.0f;
                }
                break;
        }
    }

    // === ZITTER ===

    private void handleZitter() {
        if (!mc.player.isOnGround() || !MoveUtils.isMoving()) return;

        switch (zitterMode.get()) {
            case "Teleport": {
                boolean useOver = zitterPro.get() && zitterProBlocksPlaced >= zitterStartBlocks.get().intValue();
                float speed = useOver ? zitterSpeedOver.get() : (zitterPro.get() ? zitterSpeedStart.get() : zitterSpeed.get());
                float strength = useOver ? zitterStrengthOver.get() : (zitterPro.get() ? zitterStrengthStart.get() : zitterStrength.get());
                MoveUtils.setMotion(speed);
                double yaw = Math.toRadians(mc.player.rotationYaw + (zitterDirection ? 90.0 : -90.0));
                mc.player.setMotion(
                        mc.player.getMotion().x - Math.sin(yaw) * strength,
                        mc.player.getMotion().y,
                        mc.player.getMotion().z + Math.cos(yaw) * strength
                );
                zitterDirection = !zitterDirection;
                break;
            }
            case "Smooth":
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastZitterTime >= 100) {
                    zitterDirection = !zitterDirection;
                    lastZitterTime = currentTime;
                }
                if (zitterDirection) {
                    mc.gameSettings.keyBindRight.setPressed(true);
                    mc.gameSettings.keyBindLeft.setPressed(false);
                } else {
                    mc.gameSettings.keyBindRight.setPressed(false);
                    mc.gameSettings.keyBindLeft.setPressed(true);
                }
                break;
        }
    }

    // === PLACE ===

    // === Opal: Jump Mode ===
    private void performJump() {
        switch (jumpMode.get()) {
            case "Vanilla":
                mc.player.jump();
                break;
            case "AntiGamingChair":
                mc.player.setVelocity(0, 0.42, 0);
                break;
            case "Watchdog":
                // No-op: relies on blink/telly to handle movement
                break;
            case "Bloxd":
                mc.player.jump();
                BlockPos above = new BlockPos(mc.player.getPosX(), mc.player.getPosY() + 1.0, mc.player.getPosZ());
                BlockPos aboveSupport = above.down();
                if (!mc.world.isAirBlock(aboveSupport) && mc.world.isAirBlock(above)) {
                    Direction aboveFace = Direction.UP;
                    Vector3d aboveHv = getFaceHitVec(above, aboveFace);
                    BlockRayTraceResult aboveResult = new BlockRayTraceResult(aboveHv, aboveFace, above, false);
                    mc.playerController.processRightClickBlock(mc.player, mc.world, Hand.MAIN_HAND, aboveResult);
                    mc.player.swingArm(Hand.MAIN_HAND);
                }
                mc.player.setPosition(mc.player.getPosX(), mc.player.getPosY() + 2.0, mc.player.getPosZ());
                break;
        }
    }

    private void place() {
        int tellyDelay = tellyJumpDelay.get().intValue();
        if (placementMode.is("Telly") && !mc.player.isOnGround()) {
            if (offGroundTicks > 0 && offGroundTicks < tellyDelay) return;
            if (tellyPlacedBlocks >= tellyBlockCount.get().intValue()) return;
        }
        if (sneakWhenPlace.get()) {
            mc.gameSettings.keyBindSneak.setPressed(false);
        }
        if (blockPos == null || facing == null) return;

        Vector3d hv = getFaceHitVec(blockPos, facing);

        if ((rayCast.is("Normal") || rayCast.is("Strict")) && expand.get() == 0) {
            if (!lookingAtBlock(blockPos, facing, rotationVector)) return;
        }

        // === NEW: Wait For Rotations (LB100) ===
        if (waitForRotations.get()) {
            float yawDiff = Math.abs(MathHelper.wrapDegrees(rotationVector.x - mc.player.rotationYaw));
            float pitchDiff = Math.abs(rotationVector.y - mc.player.rotationPitch);
            if (yawDiff > 5f || pitchDiff > 5f) return;
        }

        int spoofedSlot = -1;
        int playerSlot = mc.player.inventory.currentItem;

        if (!spoof.is("None")) {
            spoofedSlot = findBlockSlot();
            if (spoofedSlot == -1) return;
        }

        if (spoof.is("Normal")) {
            if (playerSlot != spoofedSlot) {
                try {
                    silentHeldSwap = true;
                    mc.player.connection.sendPacket(new CHeldItemChangePacket(spoofedSlot));
                } finally {
                    silentHeldSwap = false;
                }
            }
        } else if (spoof.is("Fake")) {
            if (spoofedSlot != -1) {
                try {
                    silentHeldSwap = true;
                    mc.player.connection.sendPacket(new CHeldItemChangePacket(spoofedSlot));
                } finally {
                    silentHeldSwap = false;
                }
                mc.player.inventory.currentItem = spoofedSlot;
                spoofSlot = spoofedSlot;
            }
        } else if (spoof.is("Silent") || spoof.is("Switch")) {
            if (playerSlot != spoofedSlot) {
                mc.player.inventory.currentItem = spoofedSlot;
            }
        }

        if (sneakWhenPlace.get()) mc.gameSettings.keyBindSneak.setPressed(true);

        BlockRayTraceResult result = new BlockRayTraceResult(hv, facing, blockPos, false);

        // === Opal: Override Raycast ===
        if (overrideRaycast.get() && mc.objectMouseOver == null) {
            mc.objectMouseOver = result;
        }

        ActionResultType actionResult = mc.playerController.processRightClickBlock(mc.player, mc.world, Hand.MAIN_HAND, result);
        if (actionResult == ActionResultType.SUCCESS) {
            // === Opal: Real Stack Size — decrement ===
            if (syncStackSize.get()) {
                int heldSlot = mc.player.inventory.currentItem;
                int prev = realStackMap.getOrDefault(heldSlot, 0);
                realStackMap.put(heldSlot, Math.max(0, prev - 1));
            }
            if (swingMode.is("DoNotHide")) {
                mc.player.swingArm(Hand.MAIN_HAND);
            } else if (swingMode.is("Hide")) {
                mc.player.swingProgressInt = -1;
                mc.player.isSwingInProgress = true;
                mc.player.swingingHand = Hand.MAIN_HAND;
            } else if (swingMode.is("NoSwing")) {
                mc.player.connection.sendPacket(new CAnimateHandPacket(Hand.MAIN_HAND));
            }

            if (mark.get()) {
                lastPlacedBlockPos = blockPos;
                lastPlaceTime = System.currentTimeMillis();
            }

            if (placementMode.is("Telly")) tellyPlacedBlocks++;
            if (placementMode.is("RiseTelly")) riseTellyBlocksPlaced++;
            if (jumpAutomatically.get()) autoJumpBlocksPlaced++;
            if (keepRotationTick.get().intValue() > 0) keepRotationTicks = keepRotationTick.get().intValue();
            if (zitterPro.get()) {
                BlockPos feetPos = new BlockPos(mc.player.getPosX(), mc.player.getPosY(), mc.player.getPosZ());
                if (blockPos.getX() != feetPos.getX() || blockPos.getZ() != feetPos.getZ()) {
                    zitterProBlocksPlaced++;
                }
            }

            if (yMode.is("Hypixel")) {
                int attempts = random.nextInt(3) + 1;
                for (int i = 0; i < attempts; i++) {
                    BlockPos foot = blockPos.up();
                    int xOff = random.nextInt(3) - 1;
                    int zOff = random.nextInt(3) - 1;
                    BlockPos target = foot.add(xOff, 0, zOff);
                    if (mc.world.isAirBlock(target)) {
                        BlockPos support = target.down();
                        if (isPosSolid(support)) {
                            Vector3d extraHv = getFaceHitVec(support, Direction.UP);
                            BlockRayTraceResult extraResult = new BlockRayTraceResult(extraHv, Direction.UP, support, false);
                            mc.playerController.processRightClickBlock(mc.player, mc.world, Hand.MAIN_HAND, extraResult);
                        }
                    }
                }
            }

            // === NEW: ExtraClick (FDP, LB100) ===
            if (extraClick.get()) {
                int extraMin = extraClickMinDelay.get().intValue();
                int extraMax = extraClickMaxDelay.get().intValue();
                if (extraMax < extraMin) extraMax = extraMin;
                int extraDelay = extraMin + (extraMax > extraMin ? random.nextInt(extraMax - extraMin + 1) : 0);
                if (extraDelay <= 0) {
                    mc.playerController.processRightClickBlock(mc.player, mc.world, Hand.MAIN_HAND, result);
                    mc.player.swingArm(Hand.MAIN_HAND);
                }
            }

            // === LB NextGen: Acceleration ===
            if (acceleration.get()) {
                if (!accelerationOnlyGround.get() || mc.player.isOnGround()) {
                    double yawRad = Math.toRadians(mc.player.rotationYaw);
                    double mx = -Math.sin(yawRad) * accelerationSpeed.get();
                    double mz = Math.cos(yawRad) * accelerationSpeed.get();
                    if (MoveUtils.isMoving()) {
                        mc.player.setMotion(mx, mc.player.getMotion().y, mz);
                    }
                }
            }

            placedBlocksUntilEagle++;
            ticks = 0;
        }

        if (spoof.is("Normal")) {
            int currentSlot = mc.player.inventory.currentItem;
            if (currentSlot != spoofedSlot) {
                try {
                    silentHeldSwap = true;
                    mc.player.connection.sendPacket(new CHeldItemChangePacket(currentSlot));
                } finally {
                    silentHeldSwap = false;
                }
            }
        } else if (spoof.is("Fake")) {
            mc.player.inventory.currentItem = playerSlot;
        } else if (spoof.is("Switch")) {
            mc.player.inventory.currentItem = spoofedSlot;
        } else if (spoof.is("Silent")) {
            try {
                silentHeldSwap = true;
                mc.player.connection.sendPacket(new CHeldItemChangePacket(playerSlot));
            } finally {
                silentHeldSwap = false;
            }
            mc.player.inventory.currentItem = spoofedSlot;
        } else if (spoof.is("Pick") || spoof.is("Spoof")) {
            int currentSlot = mc.player.inventory.currentItem;
            if (currentSlot != spoofedSlot) {
                try {
                    silentHeldSwap = true;
                    mc.player.connection.sendPacket(new CHeldItemChangePacket(spoofedSlot));
                } finally {
                    silentHeldSwap = false;
                }
            }
        }
    }

    // === RENDER ===

    @Subscribe
    private void onRender3D(EventRender3D e) {
        if (!mark.get() || lastPlacedBlockPos == null) return;

        int color;
        if (markColorMode.is("Клиент")) {
            color = Theme.MainColor(0);
        } else {
            color = markCustomColor.get();
        }

        // === Opal: Fading Overlay ===
        int renderAlpha = 128;
        if (markFade.get()) {
            long elapsed = System.currentTimeMillis() - lastPlaceTime;
            float fadeProgress = Math.min(1f, elapsed / markFadeTime.get());
            if (fadeProgress >= 1f) return;
            renderAlpha = (int) ((1f - fadeProgress) * 128);
        }
        int finalAlpha = renderAlpha;

        AxisAlignedBB aabb = new AxisAlignedBB(lastPlacedBlockPos);

        Render3D.setup3dForBlockPos(e, () -> {
            BufferBuilder buffer = Tessellator.getInstance().getBuffer();
            Render3D.drawCanisterBox(e.getMatrixStack(), buffer, Tessellator.getInstance(), aabb, true, false, true, color, 0, (color & 0xFFFFFF) | (finalAlpha << 24));
        }, false, false);
    }

    private void renderBlockCounter(MatrixStack matrixStack) {
        if (counterDisplay.is("Off")) return;

        int scaledWidth = mc.getMainWindow().getScaledWidth();
        int scaledHeight = mc.getMainWindow().getScaledHeight();

        String text = blockCount + " blocks";

        if (counterDisplay.is("Default")) {
            float textWidth = mc.fontRenderer.getStringWidth(text);
            float x = scaledWidth / 2f - textWidth / 2f;
            float y = scaledHeight - 70;
            mc.fontRenderer.drawStringWithShadow(matrixStack, text, x, y, blockCount > 0 ? 0xFFFFFFFF : 0xFFFF4040);
        } else if (counterDisplay.is("Rise")) {
            ClientFonts.nunitoRegular[18].drawStringWithShadow(matrixStack, text,
                    scaledWidth - ClientFonts.nunitoRegular[18].getWidth(text) - 4, 4, Theme.MainColor(0));
        } else if (counterDisplay.is("RiseESP")) {
            int color = blockCount > 0 ? Theme.MainColor(0) : 0xFFFF4040;
            String countStr = String.valueOf(blockCount);

            float labelW = ClientFonts.nunitoRegular[14].getWidth(text);
            float countW = ClientFonts.nunitoBold[28].getWidth(countStr);
            float boxW = Math.max(labelW, countW) + 24;
            float boxH = 38;
            float boxX = scaledWidth / 2f - boxW / 2f;
            float boxY = scaledHeight - 90;

            RectUtility.drawSmoothRect(matrixStack, boxX, boxY, boxX + boxW, boxY + boxH,
                    ColorUtils.rgba(0, 0, 0, 120));

            ClientFonts.nunitoRegular[14].drawCenteredString(matrixStack, text,
                    scaledWidth / 2f, boxY + 5, ColorUtils.rgba(255, 255, 255, 180));

            ClientFonts.nunitoBold[28].drawCenteredString(matrixStack, countStr,
                    scaledWidth / 2f, boxY + 19, color);
        }
    }

    // === UTILITIES ===

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
                if (syncStackSize.get()) {
                    blockCount += realStackMap.getOrDefault(i, stack.getCount());
                } else {
                    blockCount += stack.getCount();
                }
            }
        }

        ItemStack offhand = mc.player.getHeldItemOffhand();
        if (isValidBlock(offhand)) {
            blockCount += offhand.getCount();
        }
    }

    private int findBlockSlot() {
        String sw = switchMode.get();
        if (sw.equals("Hotbar")) {
            for (int i = 0; i < 9; i++) {
                ItemStack stack = mc.player.inventory.getStackInSlot(i);
                if (isValidBlock(stack)) return i;
            }
            return -1;
        }
        if (sw.equals("Full")) {
            for (int i = 0; i < 45; i++) {
                ItemStack stack = mc.player.container.getSlot(i).getStack();
                if (isValidBlock(stack)) return i < 9 ? i : i;
            }
            for (int i = 9; i < 36; i++) {
                ItemStack stack = mc.player.inventory.mainInventory.get(i - 9);
                if (isValidBlock(stack)) return i - 9;
            }
            for (int i = 36; i < 45; i++) {
                ItemStack stack = mc.player.inventory.armorInventory.get(i - 36);
                if (isValidBlock(stack)) {
                    int hotbarSlot = i - 36;
                    if (hotbarSlot < 9) return hotbarSlot;
                }
            }
            return -1;
        }
        // Normal
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (isValidBlock(stack)) return i;
        }
        return -1;
    }

    private boolean isValidBlock(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) return false;
        Block block = ((BlockItem) stack.getItem()).getBlock();
        return !INVALID_BLOCKS.contains(block);
    }

    private float wrapDegrees(float value) {
        value = value % 360.0f;
        if (value >= 180.0f) value -= 360.0f;
        if (value < -180.0f) value += 360.0f;
        return value;
    }

    private float clampPitch(float pitch) {
        return Math.max(-90.0f, Math.min(90.0f, pitch));
    }

    private void fakeClick() {
        if (ticks++ <= 2) {
            mc.player.connection.sendPacket(new CAnimateHandPacket(Hand.MAIN_HAND));
        }
    }

    public int getBlockCount() {
        return blockCount;
    }

    public boolean getTowerStatus() {
        return towerStatus;
    }

    private record PlaceInfo(BlockPos blockPos, Direction facing, Vector3d hitVec, Vector2f rotation) {
    }
}
