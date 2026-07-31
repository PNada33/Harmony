package xd.harm.modules.impl.combat;

import com.google.common.eventbus.Subscribe;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.IngameMenuScreen;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.passive.GolemEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.monster.SlimeEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.WaterMobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.UseAction;
import net.minecraft.network.play.client.CPlayerPacket;
import net.minecraft.network.play.client.CAnimateHandPacket;
import net.minecraft.network.play.client.CEntityActionPacket;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import xd.harm.Harmony;
import xd.harm.config.FriendStorage;
import xd.harm.config.HitAuraRotationCloud;
import xd.harm.events.input.EventInput;
import xd.harm.events.movement.EventMotion;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.impl.misc.TestPlayer;
import xd.harm.modules.impl.movement.AirStuck;
import xd.harm.modules.impl.movement.ElytraPredict;
import xd.harm.modules.impl.movement.ElytraTarget;
import xd.harm.modules.settings.Setting;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.CategorySetting;
import xd.harm.modules.settings.impl.ModeListSetting;
import xd.harm.modules.settings.impl.ModeSetting;
import xd.harm.modules.settings.impl.SliderSetting;
import xd.harm.ui.clickgui.MenuPanel;
import xd.harm.utils.math.SensUtils;
import xd.harm.utils.math.StopWatch;
import xd.harm.utils.player.AttackUtil;
import xd.harm.utils.player.MouseUtil;
import xd.harm.utils.player.MoveUtils;
import xd.harm.utils.player.PlayerUtils;
import xd.harm.utils.recording.RecordingManager;

@ModuleRegister(name = "HitAura", category = Category.Combat)
public class HitAura extends Module {
    public ModeSetting gameVersion = new ModeSetting("Версия игры", "1.12.2", new String[]{"1.12.2", "1.8"});
    public ModeSetting type = new ModeSetting("Ротации", HitAuraRotationCloud.getDefaultLabel(), HitAuraRotationCloud.getModeLabels());
    public SliderSetting attackRange = new SliderSetting("Дистанция аттаки", 3.0F, 2.5F, 6.0F, 0.05F);
    public SliderSetting rotationSpeed = new SliderSetting("Скорость ротации", 180.0F, 10.0F, 180.0F, 5.0F)
            .setVisible(() -> !HitAuraRotationCloud.isMode(type.get(), HitAuraRotationCloud.MODE_PACKET));
    public SliderSetting interactTicks = new SliderSetting("Тики интеракта", 3.0F, 1.0F, 10.0F, 1.0F)
            .setVisible(() -> HitAuraRotationCloud.isMode(type.get(), HitAuraRotationCloud.MODE_INTERACT));

    // ===== CategorySetting заголовки для группировки =====
    final CategorySetting catRotation = new CategorySetting("РОТАЦИИ");
    final CategorySetting catDistance = new CategorySetting("ДИСТАНЦИЯ");
    final CategorySetting catAttack = new CategorySetting("АТАКА");
    final CategorySetting catElytra = new CategorySetting("ЭЛИТРА");
    final CategorySetting catTargets = new CategorySetting("ЦЕЛИ");
    final CategorySetting catOptions = new CategorySetting("ОПЦИИ");
    final CategorySetting catMovement = new CategorySetting("ДВИЖЕНИЕ");
    final CategorySetting catCPS = new CategorySetting("CPS (1.8)").setVisible(() -> gameVersion.is("1.8"));

    // --- Перенесено из KillAuraNew (AndroQuantum), переведено на русский ---
    // PerfectHit: бьёт в зоне Дистанции аттаки, игнорируя Дальность клика
    public BooleanSetting perfectHit = new BooleanSetting("Идеальный удар", false);
    // ClickRange: дополнительная дальность, в которой показывается взмах даже без попадания
    public SliderSetting clickRange = new SliderSetting("Дальность клика", 1.0F, 0.0F, 4.0F, 0.001F)
            .setVisible(() -> !perfectHit.get());
    // PreRange: расширяет зону поиска цели и наведения (аттака + ротация готовятся заранее)
    public SliderSetting preRange = new SliderSetting("Дальность наводки", 2.0F, 0.0F, 6.0F, 0.001F);
    // SwingMode: как показывать взмах руки (Default/None/Packet)
    public ModeSetting swingMode = new ModeSetting("Режим взмаха", "По умолчанию", new String[]{"По умолчанию", "Нет", "Пакет"});
    // InteractionHand: какой рукой делать взмах (Правая/Левая), имеет смысл только когда взмах не "Нет"
    public ModeSetting interactionHand = new ModeSetting("Рука взмаха", "Правая", new String[]{"Правая", "Левая"})
            .setVisible(() -> !swingMode.is("Нет"));

    // ===== Перенесено из KillAuraNew =====
    // HitChance: шанс пропустить атаку (0-100% попаданий)
    public SliderSetting hitChance = new SliderSetting("Шанс попадания", 100.0F, 0.0F, 100.0F, 1.0F);
    // MaxHurtTime: не бить цель если hurtTime > этого значения (анти-чит)
    public SliderSetting maxHurtTime = new SliderSetting("Макс HurtTime", 10.0F, 0.0F, 10.0F, 1.0F);
    // FOV: ограничение по углу обзора для поиска целей
    public SliderSetting fov = new SliderSetting("FOV", 180.0F, 1.0F, 180.0F, 1.0F);
    // GroundCondition: условие атаки по состоянию на земле
    public ModeSetting groundCondition = new ModeSetting("Наземное условие", "Всегда", new String[]{"Только на земле", "Только в воздухе", "Умный", "Всегда"});
    // weaponOnly: бить только с оружием в руке
    public BooleanSetting weaponOnly = new BooleanSetting("Только с оружием", false);
    // autoDisable: автовыключение при смерти
    public BooleanSetting autoDisable = new BooleanSetting("Автовыключение", true);
    // ShowSwingNoHit: показать взмах даже без попадания
    public BooleanSetting showSwingNoHit = new BooleanSetting("Показывать взмах", true).setVisible(() -> !perfectHit.get());

    final SliderSetting elytraRange = new SliderSetting("Дистанция на элитре", 6.0F, 0.0F, 16.0F, 0.05F);
    final SliderSetting elytraRotateRange = new SliderSetting("Ротация на элитре", 12.5F, 0.0F, 64.0F, 0.5F);
    final SliderSetting elytraDistReduce = new SliderSetting("Элитра дистанция", 0.7F, 0.0F, 0.7F, 0.05F);
    public BooleanSetting predict = new BooleanSetting("Предикт", false);
    public SliderSetting predictStrength = new SliderSetting("Сила предикта", 0.2F, 0.1F, 0.5F, 0.05F).setVisible(() -> predict.get());
    public SliderSetting rwSnapTicks = new SliderSetting("Тики снапов", 2.0F, 1.0F, 10.0F, 1.0F).setVisible(() -> HitAuraRotationCloud.isMode(type.get(), HitAuraRotationCloud.MODE_RILLIWORLD));
    public SliderSetting rwSnapAimDistance = new SliderSetting("Дист. наводки снапов", 0.3F, 0.0F, 3.0F, 0.05F).setVisible(() -> HitAuraRotationCloud.isMode(type.get(), HitAuraRotationCloud.MODE_RILLIWORLD));
    final ModeListSetting targets = new ModeListSetting("Таргеты", new BooleanSetting[]{new BooleanSetting("Игроки", true), new BooleanSetting("Голые", true), new BooleanSetting("Мобы", false), new BooleanSetting("Животные", false), new BooleanSetting("Друзья", false), new BooleanSetting("Голые невидимки", true), new BooleanSetting("Невидимки", true), new BooleanSetting("Жители", false), new BooleanSetting("Команды", false), new BooleanSetting("Големы", false)});    final ModeListSetting consider = new ModeListSetting("Учитывать", new BooleanSetting[]{new BooleanSetting("Хп", true), new BooleanSetting("Броню", true), new BooleanSetting("Дистанцию", true), new BooleanSetting("Баффы", true)});
    static final ModeListSetting options = new ModeListSetting("Опции", new BooleanSetting[]{new BooleanSetting("Только криты", true), new BooleanSetting("Синхронизировать с TPS", false), new BooleanSetting("Фокусировать одну цель", true), new BooleanSetting("Коррекция движения", true), new BooleanSetting("Оптимальная дистанция атаки", false), new BooleanSetting("Останавливаться при ударе", false), new BooleanSetting("Резольвер", false)});
    public static HitAura instance;
    final ModeListSetting moreOptions = new ModeListSetting("Триггеры", new BooleanSetting[]{new BooleanSetting("Проверка луча", true), new BooleanSetting("Бить через стены", true), new BooleanSetting("Не бить если кушаешь", true), new BooleanSetting("Не бить если в гуи", true), new BooleanSetting("Перебегать противника", false)});
    public BooleanSetting wallBypass = new BooleanSetting("Обход Через Стены", true).setVisible(() -> this.moreOptions.get(1).get());
    public BooleanSetting smartCrits = new BooleanSetting("Умные криты", false).setVisible(() -> options.get(0).get());
    public SliderSetting maxClicksPerTick = new SliderSetting("Макс кликов за тик", 2.0F, 1.0F, 20.0F, 1.0F).setVisible(() -> gameVersion.is("1.8"));
    public SliderSetting minCps = new SliderSetting("Минимум CPS", 12.0F, 1.0F, 40.0F, 1.0F).setVisible(() -> gameVersion.is("1.8"));
    public SliderSetting maxCps = new SliderSetting("Максимум CPS", 15.0F, 1.0F, 40.0F, 1.0F).setVisible(() -> gameVersion.is("1.8"));
    public BooleanSetting doubleClicking = new BooleanSetting("Двойной клик", false).setVisible(() -> gameVersion.is("1.8"));
    public SliderSetting doubleClickChance = new SliderSetting("Шанс двойного клика", 60.0F, 0.0F, 100.0F, 1.0F).setVisible(() -> gameVersion.is("1.8") && doubleClicking.get());
    public static ModeSetting correctionType = new ModeSetting("Тип коррекции", "Свободный", new String[]{"Свободный", "Сфокусированный", "Таргет"}).setVisible(() -> options.get(3).get());
    public ModeSetting sprintMode = new ModeSetting("Режим спринта", "Легитный", new String[]{"Легитный", "Быстрый", "Нет"});
    public BooleanSetting shieldBreaker = new BooleanSetting("Ломать щит", true);
    public SliderSetting runPastBlocks = new SliderSetting("Перебегать на блоков", 2.0F, 1.0F, 5.0F, 1.0F).setVisible(() -> this.moreOptions.get(4).get());
    private final StopWatch stopWatch = new StopWatch();
    public static Vector2f rotateVector = new Vector2f(0.0F, 0.0F);
    public static LivingEntity target;
    private Entity selected;
    int ticks = 0;
    int inputTick = 0;
    boolean isRotated = false;
    boolean canWork = true;
    boolean tpAuraRule = false;
    float lastYaw;
    float lastPitch;
    float acceleration = 0.0F;
    boolean isBack = false;
    private final SecureRandom random = new SecureRandom();
    private long lastJumpTime = 0;
    private int groundTicks = 0;

    // ===== Quantum rotation state =====
    private int quantumTrackTicks = 0;
    private int quantumMatrixIndex = 0;
    private float quantumLastYaw = 0;
    private float quantumLastPitch = 0;
    private Entity quantumSelected = null;

    // ===== 1.8 CPS state =====
    private float nextCps = 12.0F;
    private int completeClicks = 0;
    private final StopWatch cpsTimer = new StopWatch();

    // ===== Rotation state (migrated from ServerHitAuraRotationProvider) =====
    private static final int MORE_OPTION_RUN_PAST_INDEX = 4;
    private static final int OPTION_ONLY_CRITS_INDEX = 0;
    private static final int OPTION_SYNC_TPS_INDEX = 1;
    private static final int OPTION_FOCUS_ONE_INDEX = 2;
    private static final int OPTION_MOVE_FIX_INDEX = 3;
    private static final int OPTION_OPTIMAL_ATTACK_DISTANCE_INDEX = 4;
    private static final int OPTION_STOP_ON_HIT_INDEX = 5;
    private static final int MORE_OPTION_RAY_CHECK_INDEX = 0;
    private static final int MORE_OPTION_WALLS_INDEX = 1;
    private static final int MORE_OPTION_NO_EAT_INDEX = 2;
    private static final int MORE_OPTION_NO_GUI_INDEX = 3;
    private static final int TARGET_PLAYERS_INDEX = 0;
    private static final int TARGET_NAKED_INDEX = 1;
    private static final int TARGET_MOBS_INDEX = 2;
    private static final int TARGET_ANIMALS_INDEX = 3;
    private static final int TARGET_FRIENDS_INDEX = 4;
    private static final int TARGET_NAKED_INVIS_INDEX = 5;
    private static final int TARGET_INVIS_INDEX = 6;
    private static final int TARGET_VILLAGERS_INDEX = 7;
    private static final int TARGET_TEAMS_INDEX = 8;
    private static final int TARGET_GOLEMS_INDEX = 9;
    private static final int CONSIDER_HP_INDEX = 0;
    private static final int CONSIDER_ARMOR_INDEX = 1;
    private static final int CONSIDER_DISTANCE_INDEX = 2;

    private float spookyNoiseYaw = 0;
    private float spookyNoisePitch = 0;
    private float spookyExtraYaw = 0;
    private float spookyExtraPitch = 0;
    private long spookyLastNoiseUpdate = 0;
    private long spookyNextNoiseUpdateDelay = 85L;
    private long spookyLastRotateUpdate = System.currentTimeMillis();
    private long spookyLastAimPointUpdate = 0L;
    private long spookyNextAimPointUpdateDelay = 260L;
    private LivingEntity spookyTrackedTarget;
    private float spookyAimPointX = 0.0F;
    private float spookyAimPointY = 0.0F;
    private float spookyAimPointZ = 0.0F;
    private float spookyYawVelocity = 0.0F;
    private float spookyPitchVelocity = 0.0F;
    private float spookyBurstPower = 0.0F;
    private long spookyBurstTime = 0L;
    private long spookyBurstHoldTime = 0L;
    private int spookyBurstCounter = 0;
    private float spookyHeldYaw = 0.0F;
    private float spookyHeldPitch = 0.0F;
    private float spookyWaveYaw = 0.0F;
    private float spookyWavePitch = 0.0F;
    private long spookyWaveLastUpdate = System.currentTimeMillis();
    private float spookyControlYawSpeed = 27.0F;
    private float spookyControlPitchSpeed = 15.0F;
    private boolean spookyReturnActive = false;
    private long spookyReturnLastUpdate = 0L;
    private int spookyReturnTicks = 0;
    private float spookyReturnYawVelocity = 0.0F;
    private float spookyReturnPitchVelocity = 0.0F;
    private int spookyVisibilityCounter = 0;

    private float funTimeYawMicroShake = 0.0F;
    private float funTimePitchMicroShake = 0.0F;
    private long funTimeLastMicroShakeUpdate = System.currentTimeMillis();
    private float funTimeBaseYawSpeed = randBetween(1608.0F, 1609.0F);
    private float funTimeBasePitchSpeed = randBetween(1.3F, 2.8F);
    private boolean funTimeIsFlickPhase = false;
    private boolean funTimeIsLookingDownPhase = false;
    private long funTimeLastPhaseChange = System.currentTimeMillis();
    private long funTimeNextPhaseChangeDelay = ThreadLocalRandom.current().nextLong(50000L, 100000L);
    private long funTimeFlickStartTime = 0L;
    private int funTimeFlickDurationTicks = 0;
    private int funTimeLastSwingTicks = 0;
    private float funTimeAttackSnapTimer = 0.0F;

    private float rwSnapJitterAmount = 0.0F;

    private float trainingYawMicroJitter = 0.0F;
    private float trainingPitchMicroJitter = 0.0F;
    private long trainingLastMicroUpdate = System.currentTimeMillis();
    private long trainingLastRotateUpdate = System.currentTimeMillis();
    private long trainingLastSpeedRefresh = System.currentTimeMillis();
    private float trainingBaseYawSpeed = randBetween(18.0F, 28.0F);
    private float trainingBasePitchSpeed = randBetween(8.0F, 14.0F);
    private LivingEntity trainingTrackedTarget;
    private float trainingLockedPitch = 0.0F;
    private boolean trainingPitchLocked = false;

    private LivingEntity slotAcTrackedTarget;
    private float slotAcCurrentYaw;
    private float slotAcCurrentPitch;
    private float slotAcVelocityYaw;
    private float slotAcVelocityPitch;
    private double slotAcAimPointX;
    private double slotAcAimPointY;
    private double slotAcAimPointZ;
    private float slotAcNoiseAngle;
    private int slotAcHitPhase;
    private int slotAcHitTimer;
    private float slotAcPitchBeforeHit;
    private long slotAcFirstSeenTime;
    private int slotAcReactionMs;
    private boolean slotAcReactionComplete;
    private float slotAcLastSentYaw;
    private float slotAcLastSentPitch;
    private float slotAcElytraSpeed;
    private boolean slotAcElytraReturning;
    private float slotAcNoiseAmplitude = 1.8F;
    private float slotAcAdjYaw;
    private float slotAcAdjPitch;
    private long slotAcLastAimPointChangeTime;

    public HitAura() {
        instance = this;
        // Скрываем "Только криты" когда выбран режим 1.8
        options.get(OPTION_ONLY_CRITS_INDEX).setVisible(() -> !gameVersion.is("1.8"));
        this.addSettings(new Setting[]{
                // Версия игры — всегда на виду
                this.gameVersion,
                // ===== РОТАЦИИ =====
                catRotation, this.type, this.rotationSpeed, this.interactTicks, this.rwSnapTicks, this.rwSnapAimDistance,
                // ===== ДИСТАНЦИЯ =====
                catDistance, this.attackRange, this.clickRange, this.preRange,
                // ===== АТАКА =====
                catAttack, this.perfectHit, this.swingMode, this.interactionHand, this.showSwingNoHit,
                // ===== ЭЛИТРА =====
                catElytra, this.elytraRange, this.elytraRotateRange, this.elytraDistReduce,
                // ===== ЦЕЛИ =====
                catTargets, this.targets, this.consider, this.hitChance, this.maxHurtTime, this.fov,
                // ===== ОПЦИИ =====
                catOptions, options, this.moreOptions, this.wallBypass, this.smartCrits,
                this.groundCondition, this.weaponOnly, this.autoDisable,
                // ===== ДВИЖЕНИЕ =====
                catMovement, correctionType, this.sprintMode, this.predict, this.predictStrength,
                this.shieldBreaker, this.runPastBlocks,
                // ===== CPS (1.8) =====
                catCPS, this.maxClicksPerTick, this.minCps, this.maxCps,
                this.doubleClicking, this.doubleClickChance
        });
    }

    private static boolean option(int index) {
        return options.get(index).get();
    }

    private boolean moreOption(int index) {
        return this.moreOptions.get(index).get();
    }

    private boolean targetOption(int index) {
        return this.targets.get(index).get();
    }

    private boolean considerOption(int index) {
        return this.consider.get(index).get();
    }

    float maxRange() {
        float elytraRotate = 0.0F;
        if (mc.player.isElytraFlying()) {
            elytraRotate = this.elytraRotateRange.get();
        }
        return this.attackDistance() + (mc.player.isElytraFlying() ? getActiveElytraRange() : 0.0F) + elytraRotate;
    }

    /**
     * Полная зона поиска цели = дистанция атаки + Дальность наводки (preRange).
     * Перенесено из KillAuraNew: цели в радиусе range + preRange ещё можно
     * наводить/крутиться на них, даже если ударить нельзя.
     */
    private float targetSearchRange() {
        return this.attackDistance() + this.preRange.get();
    }

    private static float additionalNavRange() {
        return 7.0F;
    }

@Subscribe
    public void onInput(EventInput eventInput) {
        if (!this.isState()) {
            if (this.spookyReturnActive && mc.player != null && option(OPTION_MOVE_FIX_INDEX)) {
                MoveUtils.fixMovement(eventInput, rotateVector.x);
            }
            return;
        }
        if (inputTick > 0 && option(OPTION_STOP_ON_HIT_INDEX)) {
            eventInput.setForward(0.0F);
            inputTick--;
        } else {
            inputTick = 0;
        }
        handleSprinting(eventInput);
        if (option(OPTION_MOVE_FIX_INDEX) && !correctionType.is("Сфокусированный") && this.canWork) {
            if (HitAuraRotationCloud.isMode(type.get(), HitAuraRotationCloud.MODE_SLOTAC)) {
                float correctionYaw = rotateVector.x;
                if (target != null && mc.player != null) {
                    float yawDelta = MathHelper.wrapDegrees(rotateVector.x - mc.player.rotationYaw);
                    boolean nearTarget = mc.player.getDistanceEyePos(target) <= attackDistance() + 0.35F;
                    if (nearTarget || Math.abs(yawDelta) > 65.0F) {
                        correctionYaw = mc.player.rotationYaw + MathHelper.clamp(yawDelta, -35.0F, 35.0F);
                    }
                }
                MoveUtils.fixMovementNoBack(eventInput, correctionYaw);
            } else {
                MoveUtils.fixMovement(eventInput, rotateVector.x);
            }
        }
    }

    private void handleSprinting(EventInput eventInput) {
        if (mc.player == null) return;
        if (sprintMode.is("Нет")) return;

        // Поддерживаем спринт всегда (и с целью, и без) — иначе после потери
        // цели спринт «ломается» и не восстанавливается, пока не отпустишь W.
        boolean movingForward = mc.gameSettings.keyBindForward.isKeyDown()
                || eventInput.getForward() > 0.0F;

        if (movingForward) {
            eventInput.setSprintState(true);
            if (!mc.player.isSprinting()) {
                mc.player.setSprinting(true);
            }
        }
    }

    private boolean canAutoJump() {
        if (mc.player == null) return false;
        if (!mc.player.isOnGround()) return false;
        if (mc.player.isInWater()) return false;
        if (mc.player.isInLava()) return false;
        if (mc.player.isElytraFlying()) return false;
        if (mc.player.abilities.isFlying) return false;
        if (mc.player.isOnLadder()) return false;
        if (mc.player.isSneaking()) return false;
        if (mc.player.fallDistance > 0.0F) return false;
        long now = System.currentTimeMillis();
        if (now - lastJumpTime < 150) return false;
        return true;
    }

    @Subscribe
    public void onUpdate(EventUpdate e) {
        if (!this.isState()) {
            if (this.spookyReturnActive) {
                this.updateSpookyReturn();
            }
            return;
        }
        // --- autoDisable: выключение при смерти ---
        if (autoDisable.get() && (mc.player == null || !mc.player.isAlive() || mc.player.getHealth() <= 0.0F)) {
            this.toggle();
            return;
        }
        if (this.canWork) {
            boolean focusOneTarget = option(OPTION_FOCUS_ONE_INDEX);
            if (focusOneTarget && (target == null || !this.isValidForNav(target, additionalNavRange())) || !focusOneTarget) {
                this.updateTarget();
            }
            if (mc.player.isOnGround()) {
                groundTicks++;
            } else {
                groundTicks = 0;
            }
            if (target != null) {
                this.isRotated = false;
                this.setRotate();
                if (target instanceof PlayerEntity && shieldBreaker.get() && isTargetBlocking()) {
                    breakShieldAndAttack();
                } else if (gameVersion.is("1.8")) {
                    // 1.8 режим: CPS-based атака без критов
                    this.updateAttack18();
                } else if (this.shouldPlayerFalling() && this.stopWatch.hasTimeElapsed(0)) {
                    this.ticks = 3;
                    this.tpAuraRule = true;
                    AirStuck airStuck = Harmony.getInstance().getModuleManager().getAirStuck();
                    if (airStuck != null && airStuck.isState() && airStuck.critAwaited) {
                        airStuck.startFreeze();
                    }
                    this.updateAttack();
                    this.tpAuraRule = false;
                }
            } else {
                this.stopWatch.setLastMS(0L);
                this.reset();
                acceleration = 0.0F;
                isBack = false;
            }
        }
    }

    private boolean isTargetBlocking() {
        if (target instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) target;
            return player.isActiveItemStackBlocking();
        }
        return false;
    }

    private void breakShieldAndAttack() {
        int axeSlot = findAxeSlot();
        if (axeSlot == -1) return;
        int currentSlot = mc.player.inventory.currentItem;
        mc.player.connection.sendPacket(new CHeldItemChangePacket(axeSlot));
        mc.playerController.attackEntity(mc.player, target);
        mc.player.swingArm(Hand.MAIN_HAND);
        this.onAttackRotation(target);
        mc.player.connection.sendPacket(new CHeldItemChangePacket(currentSlot));
        stopWatch.setLastMS(500L);
    }

    private int findAxeSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (stack.getItem() instanceof AxeItem) {
                return i;
            }
        }
        return -1;
    }

    @Subscribe
    private void onWalking(EventMotion e) {
        if (this.spookyReturnActive && mc.player != null) {
            e.setYaw(rotateVector.x);
            e.setPitch(rotateVector.y);
            mc.player.rotationYawHead = rotateVector.x;
            mc.player.renderYawOffset = PlayerUtils.calculateCorrectYawOffset2(rotateVector.x);
            mc.player.rotationPitchHead = rotateVector.y;
            return;
        }
        if (this.spookyReturnActive) {
            return;
        }
        if (!this.isState()) {
            return;
        }
        if (target != null) {
            e.setYaw(rotateVector.x);
            e.setPitch(rotateVector.y);
            mc.player.rotationYawHead = rotateVector.x;
            mc.player.renderYawOffset = PlayerUtils.calculateCorrectYawOffset2(rotateVector.x);
            mc.player.rotationPitchHead = getVisualRotatePitch();
        }
    }
    public void setRotate() {
        boolean isAttacking = shouldPlayerFalling() && stopWatch.hasTimeElapsed(0);
        this.isRotated = this.rotate(target, isAttacking);

        if (option(OPTION_MOVE_FIX_INDEX)) {
            mc.player.rotationYawOffset = rotateVector.x;
        }
        if (moreOption(MORE_OPTION_NO_EAT_INDEX) && mc.player.isHandActive() && mc.player.getHeldItemOffhand().getUseAction() == UseAction.EAT) {
            rotateVector = new Vector2f(mc.player.rotationYaw, mc.player.rotationPitch);
        }
        if (moreOption(MORE_OPTION_NO_GUI_INDEX) && mc.currentScreen != null && !(mc.currentScreen instanceof MenuPanel) && !(mc.currentScreen instanceof ChatScreen) && !(mc.currentScreen instanceof IngameMenuScreen)) {
            rotateVector = new Vector2f(mc.player.rotationYaw, mc.player.rotationPitch);
        }
    }

    public float attackDistance() {
        if (option(OPTION_OPTIMAL_ATTACK_DISTANCE_INDEX)) {
            return !mc.player.isSwimming() ? 3.6F : 3.0F;
        }
        return this.attackRange.get();
    }

    private float getActiveElytraRange() {
        ElytraTarget elytraTarget = Harmony.getInstance().getModuleManager().getElytraTarget();
        if (elytraTarget != null && elytraTarget.isState()) {
            return elytraTarget.elytraFindRange.get();
        }
        return this.elytraRange.get();
    }

    private void updateTarget() {
        List<LivingEntity> targetsList = new ArrayList<>();
        float navRange = additionalNavRange();
        float searchRange = targetSearchRange();
        for (Entity entity : mc.world.getAllEntities()) {
            if (entity instanceof LivingEntity) {
                LivingEntity living = (LivingEntity) entity;
                // Цель валидна для выбора, если она в зоне навигации ИЛИ в зоне поиска (атака + preRange)
                if (this.isValidForNav(living, navRange) || (this.isValidForNav(living, searchRange) && searchRange > navRange)) {
                    targetsList.add(living);
                }
            }
        }
        if (targetsList.isEmpty()) {
            target = null;
        } else if (targetsList.size() == 1) {
            target = targetsList.get(0);
        } else {
            targetsList.sort(Comparator.comparingDouble(e -> calculatePriority((LivingEntity) e)).reversed());
            target = targetsList.get(0);
        }
    }

    private double calculatePriority(LivingEntity entity) {
        double score = 0.0;
        if (considerOption(CONSIDER_DISTANCE_INDEX)) {
            double distance = mc.player.getDistance(entity);
            score += Math.max(0.0, maxRange() - distance) * 10.0;
        }
        if (considerOption(CONSIDER_HP_INDEX)) {
            double hp = entity.getHealth() + entity.getAbsorptionAmount();
            score += Math.max(0.0, 40.0 - hp) * 2.0;
        }
        if (considerOption(CONSIDER_ARMOR_INDEX) && entity instanceof PlayerEntity) {
            double armorScore = Math.max(0, 20 - getEntityArmor((PlayerEntity) entity));
            score += armorScore * 1.5;
        }
        return score;
    }

    private double getEntityArmor(PlayerEntity player) {
        double armor = 0.0;
        for (int i = 0; i < 4; i++) {
            ItemStack stack = player.inventory.armorInventory.get(i);
            if (stack.getItem() instanceof ArmorItem) {
                ArmorItem armorItem = (ArmorItem) stack.getItem();
                armor += armorItem.getDamageReduceAmount();
                if (stack.isEnchanted()) {
                    armor += EnchantmentHelper.getEnchantmentLevel(Enchantments.PROTECTION, stack) * 0.25;
                }
            }
        }
        return armor;
    }

    /**
     * Вычисляет [yaw, pitch] от глаз игрока до центра сущности.
     * Используется для FOV-проверки.
     */
    private float[] getRotationsToEntity(Entity entity) {
        Vector3d eyePos = mc.player.getEyePosition(1.0F);
        // getEyePosition есть у Entity, работает для всех сущностей
        Vector3d targetPos = entity.getEyePosition(1.0F);
        Vector3d diff = targetPos.subtract(eyePos);
        double dist = Math.hypot(diff.x, diff.z);
        float yaw = (float) Math.toDegrees(Math.atan2(-diff.x, diff.z));
        float pitch = (float) MathHelper.clamp(-Math.toDegrees(Math.atan2(diff.y, dist)), -90.0F, 90.0F);
        return new float[]{yaw, pitch};
    }

    private boolean isValidForNav(LivingEntity entity, float navRange) {
        if (entity instanceof ClientPlayerEntity) return false;
        if (entity.ticksExisted < 3) return false;
        if (mc.player.getDistanceEyePos(entity) > navRange) return false;
        // --- FOV: проверка угла обзора ---
        if (fov.getFloat() < 180.0F) {
            float[] rotations = getRotationsToEntity(entity);
            float yawDiff = MathHelper.wrapDegrees(mc.player.rotationYaw - rotations[0]);
            if (Math.abs(yawDiff) > fov.getFloat() / 2.0F) return false;
        }
        boolean isTestPlayer = TestPlayer.fakePlayer != null && entity.getEntityId() == TestPlayer.fakePlayer.getEntityId();
        if (!isTestPlayer && !moreOption(MORE_OPTION_WALLS_INDEX) && !mc.player.canEntityBeSeen(entity))
            return false;
        if (entity instanceof PlayerEntity) {
            PlayerEntity p = (PlayerEntity) entity;
            if (!isTestPlayer && !entity.getUniqueID().equals(PlayerEntity.getOfflineUUID(p.getGameProfile().getName()))) return false;
            if (FriendStorage.isFriend(p.getName().getString()) && !targetOption(TARGET_FRIENDS_INDEX))
                return false;
            if (p.getName().getString().equalsIgnoreCase(mc.player.getName().getString())) return false;
            if (!isTestPlayer && p.isCreative()) return false;
        }
        if (entity instanceof PlayerEntity && !targetOption(TARGET_PLAYERS_INDEX)) return false;
        // Команды: исключаем тиммейтов из обычных игроков, у них свой тумблер
        if (entity instanceof PlayerEntity && mc.player.getTeam() != null && entity.getTeam() != null
                && entity.getTeam() == mc.player.getTeam() && !targetOption(TARGET_TEAMS_INDEX))
            return false;
        if (entity instanceof PlayerEntity && entity.getTotalArmorValue() == 0 && !targetOption(TARGET_NAKED_INDEX))
            return false;
        if (entity instanceof PlayerEntity && entity.isInvisible() && entity.getTotalArmorValue() == 0 && !targetOption(TARGET_NAKED_INVIS_INDEX))
            return false;
        if (entity instanceof PlayerEntity && entity.isInvisible() && !targetOption(TARGET_INVIS_INDEX))
            return false;
        // Мобы (без жителей — у них свой тумблер)
        if ((entity instanceof MonsterEntity || entity instanceof SlimeEntity) && !targetOption(TARGET_MOBS_INDEX))
            return false;
        // Жители
        if (entity instanceof VillagerEntity && !targetOption(TARGET_VILLAGERS_INDEX))
            return false;
        // Големы
        if (entity instanceof GolemEntity && !targetOption(TARGET_GOLEMS_INDEX))
            return false;
        if ((entity instanceof AnimalEntity || entity instanceof WaterMobEntity) && !targetOption(TARGET_ANIMALS_INDEX)) return false;
        return !entity.isInvulnerable() && entity.isAlive() && !(entity instanceof ArmorStandEntity);
    }

    private boolean isPlayerEating() {
        if (!mc.player.isHandActive()) return false;
        UseAction action = mc.player.getActiveItemStack().getUseAction();
        return action == UseAction.EAT || action == UseAction.DRINK;
    }

    /**
     * Проверяет groundCondition — когда можно атаковать (на земле, в воздухе, умный, всегда).
     */
    private boolean checkGroundCondition() {
        if (groundCondition.is("Всегда")) return true;
        if (groundCondition.is("Только на земле")) return mc.player.isOnGround();
        if (groundCondition.is("Только в воздухе")) return !mc.player.isOnGround();
        // "Умный" — атаковать всегда, но это влияет на криты: криты только на земле
        return true;
    }

    /**
     * Проверяет, держит ли игрок оружие (меч/топор/киркач/ложка) в руке.
     */
    private boolean isHoldingWeapon() {
        ItemStack held = mc.player.getHeldItemMainhand();
        return held.getItem() instanceof net.minecraft.item.SwordItem
                || held.getItem() instanceof AxeItem
                || held.getItem() instanceof net.minecraft.item.PickaxeItem
                || held.getItem() instanceof net.minecraft.item.ShovelItem;
    }

    private void updateAttack() {
        // --- GroundCondition: проверка условия по земле/воздуху ---
        if (!checkGroundCondition()) return;
        // --- weaponOnly: бить только с оружием ---
        if (weaponOnly.get() && !isHoldingWeapon()) return;

        float elytraDistReduce = 0.0F;
        if (mc.player.isElytraFlying()) {
            float elytraSpeed = (float) Math.sqrt(mc.player.getMotion().x * mc.player.getMotion().x + mc.player.getMotion().z * mc.player.getMotion().z);
            elytraDistReduce = this.elytraDistReduce.get() - elytraSpeed * 0.1f;
        }

        double distance = mc.player.getDistanceEyePos(target);
        // --- Перенесено из KillAuraNew:PerfectHit ---
        // PerfectHit=true → бьём строго в зоне attackDistance, иначе выходим.
        // PerfectHit=false → допускаем показ взмаха в расширенной зоне (attackDistance + clickRange).
        boolean inAttackRange = distance <= this.attackDistance() - elytraDistReduce;
        boolean inSwingRange = !perfectHit.get() && distance <= this.attackDistance() + this.clickRange.get();
        if (!inAttackRange && !inSwingRange) return;

        boolean isCloseRange = distance < 1.5;
        boolean isTestPlayerTarget = TestPlayer.fakePlayer != null && target.getEntityId() == TestPlayer.fakePlayer.getEntityId();
        this.selected = MouseUtil.getMouseOver(target, rotateVector.x, rotateVector.y, this.attackDistance());
        if (!isTestPlayerTarget
                && HitAuraRotationCloud.isMode(this.type.get(), HitAuraRotationCloud.MODE_SPOOKY)
                && moreOption(MORE_OPTION_RAY_CHECK_INDEX)
                && !mc.player.isElytraFlying()
                && this.selected == null
                && !isCloseRange) {
            this.alignSpookyBeforeAttack(target);
            this.selected = MouseUtil.getMouseOver(target, rotateVector.x, rotateVector.y, this.attackDistance());
        }
        if (!isTestPlayerTarget && moreOption(MORE_OPTION_RAY_CHECK_INDEX) && !mc.player.isElytraFlying() && this.selected == null && !isCloseRange)
            return;
        if (!isTestPlayerTarget && !moreOption(MORE_OPTION_WALLS_INDEX) && !mc.player.canEntityBeSeen(target) && !isCloseRange)
            return;
        if (moreOption(MORE_OPTION_NO_EAT_INDEX) && this.isPlayerEating()) return;
        if (moreOption(MORE_OPTION_NO_GUI_INDEX) && mc.currentScreen != null && !(mc.currentScreen instanceof MenuPanel) && !(mc.currentScreen instanceof ChatScreen) && !(mc.currentScreen instanceof IngameMenuScreen))
            return;
        inputTick = 1;
        if (mc.player.serverSprintState && !sprintMode.is("Нет")) {
            // Silent unsprint: отправляем серверу STOP_SPRINTING, чтобы удар
            // наносился как неспринтовый (нормальный урон). На следующем тике
            // vanilla-код сам отправит START_SPRINTING, т.к. isSprinting() = true,
            // а serverSprintState станет false. Визуально спринт не прерывается.
            mc.player.connection.sendPacket(new CEntityActionPacket(mc.player, CEntityActionPacket.Action.STOP_SPRINTING));
            mc.player.serverSprintState = false;
        }
        sendAirStuckSilentRotation();
        this.stopWatch.setLastMS(500L);

        // --- HitChance: шанс пропустить атаку ---
        boolean shouldHit = hitChance.getFloat() >= 100.0F || Math.random() * 100.0F < hitChance.getFloat();
        // --- MaxHurtTime: не бить если цель ещё в hurtTime ---
        if (maxHurtTime.getFloat() < 10.0F && target.hurtTime > maxHurtTime.getFloat()) {
            shouldHit = false;
        }

        // Атака только если цель реально в зоне удара
        if (inAttackRange && shouldHit) {
            mc.playerController.attackEntity(mc.player, target);
        }
        // --- ShowSwingNoHit: показать взмах даже без попадания ---
        // Вне зоны удара (PerfectHit off) показываем взмах только если цель в зоне clickRange.
        boolean shouldSwing = inAttackRange || inSwingRange;
        if (shouldSwing && (shouldHit || (showSwingNoHit.get() && !perfectHit.get()))) {
            processSwing();
        }
        if (shouldHit) {
            this.onAttackRotation(target);
        }
    }

    /**
     * Логика взмаха руки — перенесена из KillAuraNew.proccesswing().
     * swingMode: "По умолчанию" (визуальный взмах), "Нет" (без взмаха), "Пакет" (только пакет).
     * interactionHand: "Правая"/"Левая" — какой рукой делать взмах.
     */
    private void processSwing() {
        Hand hand = interactionHand.is("Левая") ? Hand.OFF_HAND : Hand.MAIN_HAND;

        if (swingMode.is("Нет")) {
            return;
        }
        if (swingMode.is("Пакет")) {
            mc.player.connection.sendPacket(new CAnimateHandPacket(hand));
        } else {
            // "По умолчанию"
            mc.player.swingArm(hand);
        }
    }

    private void sendAirStuckSilentRotation() {
        AirStuck airStuck = Harmony.getInstance().getModuleManager().getAirStuck();
        if (airStuck != null && airStuck.isState()) {
            mc.player.connection.sendPacket(new CPlayerPacket.RotationPacket(rotateVector.x, rotateVector.y, mc.player.isOnGround()));
        }
    }

    public boolean shouldPlayerFalling() {
        return AttackUtil.isPlayerFalling(option(OPTION_ONLY_CRITS_INDEX), (Boolean) this.smartCrits.get(), option(OPTION_SYNC_TPS_INDEX));
    }

    // ===== 1.8 CPS logic =====
    private void accumulateCPS() {
        double distance = target != null ? mc.player.getDistance(target) : 999.0;
        if (distance > this.attackDistance()) {
            completeClicks = Math.min(completeClicks, 1);
            return;
        }
        if (cpsTimer.hasTimeElapsed((long) (1000.0 / nextCps), true)) {
            completeClicks++;
            if (doubleClicking.get() && Math.random() * 100 < doubleClickChance.getFloat()) {
                completeClicks++;
            }
            updateNextCps();
        }
        completeClicks = Math.min(completeClicks, maxClicksPerTick.getInt());
    }

    private void updateNextCps() {
        float min = minCps.getFloat();
        float max = maxCps.getFloat();
        nextCps = min + (float) Math.random() * (max - min);
    }

    private void updateAttack18() {
        // --- GroundCondition: проверка условия по земле/воздуху ---
        if (!checkGroundCondition()) return;
        // --- weaponOnly: бить только с оружием ---
        if (weaponOnly.get() && !isHoldingWeapon()) return;

        // Накапливаем клики на основе CPS
        accumulateCPS();
        int clicks = Math.min(completeClicks, maxClicksPerTick.getInt());
        completeClicks -= clicks;
        if (clicks <= 0) return;

        float elytraDistReduce = 0.0F;
        if (mc.player.isElytraFlying()) {
            float elytraSpeed = (float) Math.sqrt(mc.player.getMotion().x * mc.player.getMotion().x + mc.player.getMotion().z * mc.player.getMotion().z);
            elytraDistReduce = this.elytraDistReduce.get() - elytraSpeed * 0.1f;
        }

        double distance = mc.player.getDistanceEyePos(target);
        boolean inAttackRange = distance <= this.attackDistance() - elytraDistReduce;
        boolean inSwingRange = !perfectHit.get() && distance <= this.attackDistance() + this.clickRange.get();
        if (!inAttackRange && !inSwingRange) return;

        boolean isCloseRange = distance < 1.5;
        boolean isTestPlayerTarget = TestPlayer.fakePlayer != null && target.getEntityId() == TestPlayer.fakePlayer.getEntityId();
        this.selected = MouseUtil.getMouseOver(target, rotateVector.x, rotateVector.y, this.attackDistance());
        if (!isTestPlayerTarget
                && HitAuraRotationCloud.isMode(this.type.get(), HitAuraRotationCloud.MODE_SPOOKY)
                && moreOption(MORE_OPTION_RAY_CHECK_INDEX)
                && !mc.player.isElytraFlying()
                && this.selected == null
                && !isCloseRange) {
            this.alignSpookyBeforeAttack(target);
            this.selected = MouseUtil.getMouseOver(target, rotateVector.x, rotateVector.y, this.attackDistance());
        }
        if (!isTestPlayerTarget && moreOption(MORE_OPTION_RAY_CHECK_INDEX) && !mc.player.isElytraFlying() && this.selected == null && !isCloseRange)
            return;
        if (!isTestPlayerTarget && !moreOption(MORE_OPTION_WALLS_INDEX) && !mc.player.canEntityBeSeen(target) && !isCloseRange)
            return;
        if (moreOption(MORE_OPTION_NO_EAT_INDEX) && this.isPlayerEating()) return;
        if (moreOption(MORE_OPTION_NO_GUI_INDEX) && mc.currentScreen != null && !(mc.currentScreen instanceof MenuPanel) && !(mc.currentScreen instanceof ChatScreen) && !(mc.currentScreen instanceof IngameMenuScreen))
            return;
        inputTick = 1;
        if (mc.player.serverSprintState && !sprintMode.is("Нет")) {
            // Silent unsprint: отправляем серверу STOP_SPRINTING, чтобы удар
            // наносился как неспринтовый (нормальный урон). На следующем тике
            // vanilla-код сам отправит START_SPRINTING, т.к. isSprinting() = true,
            // а serverSprintState станет false. Визуально спринт не прерывается.
            mc.player.connection.sendPacket(new CEntityActionPacket(mc.player, CEntityActionPacket.Action.STOP_SPRINTING));
            mc.player.serverSprintState = false;
        }

        // --- HitChance: шанс пропустить атаку ---
        boolean shouldHit = hitChance.getFloat() >= 100.0F || Math.random() * 100.0F < hitChance.getFloat();
        // --- MaxHurtTime: не бить если цель ещё в hurtTime ---
        if (maxHurtTime.getFloat() < 10.0F && target.hurtTime > maxHurtTime.getFloat()) {
            shouldHit = false;
        }

        for (int i = 0; i < clicks; i++) {
            sendAirStuckSilentRotation();
            if (inAttackRange && shouldHit) {
                mc.playerController.attackEntity(mc.player, target);
            }
            // --- ShowSwingNoHit: показать взмах даже без попадания ---
            boolean shouldSwing = inAttackRange || inSwingRange;
            if (shouldSwing && (shouldHit || (showSwingNoHit.get() && !perfectHit.get()))) {
                processSwing();
            }
            if (shouldHit) {
                this.onAttackRotation(target);
            }
        }
        this.stopWatch.setLastMS(500L);
    }

    private void reset() {
        mc.player.rotationYawOffset = Integer.MIN_VALUE;
        rotateVector = new Vector2f(mc.player.rotationYaw, mc.player.rotationPitch);
    }

    private void clearMovementState() {
        this.inputTick = 0;
        this.ticks = 0;
        this.tpAuraRule = false;
        this.canWork = true;
        this.isBack = false;
        this.acceleration = 0.0F;
        if (mc.player != null) {
            mc.player.rotationYawOffset = Integer.MIN_VALUE;
        }
        mc.timer.timerSpeed = 1.0F;
    }

    private float getVisualRotatePitch() {
        if (mc.player == null) {
            return rotateVector.y;
        }
        if (!HitAuraRotationCloud.isMode(type.get(), HitAuraRotationCloud.MODE_SLOTAC)) {
            return rotateVector.y;
        }
        return mc.player.rotationPitch;
    }

    private boolean startSpookyReturn() {
        if (rotateVector == null || mc.player == null) {
            return false;
        }
        float yawDelta = MathHelper.wrapDegrees(rotateVector.x - mc.player.rotationYaw);
        float pitchDelta = rotateVector.y - mc.player.rotationPitch;
        if (Math.abs(yawDelta) < 0.35F && Math.abs(pitchDelta) < 0.35F) {
            return false;
        }
        this.spookyReturnActive = true;
        this.spookyReturnLastUpdate = System.currentTimeMillis() - 50L;
        this.spookyReturnTicks = 0;
        this.spookyReturnYawVelocity = 0.0F;
        this.spookyReturnPitchVelocity = 0.0F;
        this.spookyYawVelocity = 0.0F;
        this.spookyPitchVelocity = 0.0F;
        return true;
    }

    private void updateSpookyReturn() {
        if (mc.player == null) {
            this.finishSpookyReturn();
            return;
        }
        long now = System.currentTimeMillis();
        long dt = now - this.spookyReturnLastUpdate;
        if (dt < 1L || dt > 150L) dt = 50L;
        this.spookyReturnLastUpdate = now;
        ++this.spookyReturnTicks;

        float currentYaw = rotateVector.x;
        float currentPitch = rotateVector.y;
        float yawDelta = MathHelper.wrapDegrees(mc.player.rotationYaw - currentYaw);
        float pitchDelta = mc.player.rotationPitch - currentPitch;
        float absYaw = Math.abs(yawDelta);
        float absPitch = Math.abs(pitchDelta);
        float distanceFactor = Math.min(1.0F, Math.max(0.1F, (float) Math.hypot(absYaw, absPitch) / 45.0F));
        float yawSpeed = Math.max(1.0F, this.spookyControlYawSpeed) * distanceFactor;
        float pitchSpeed = Math.max(1.0F, this.spookyControlPitchSpeed) * distanceFactor;

        float yawStep = MathHelper.clamp(yawDelta, -yawSpeed, yawSpeed);
        float pitchStep = MathHelper.clamp(pitchDelta, -pitchSpeed, pitchSpeed);

        float newYaw = currentYaw;
        float newPitch = currentPitch;
        if (Math.abs(yawStep) > 0.0001F) {
            newYaw += yawStep;
        }
        if (Math.abs(pitchStep) > 0.0001F) {
            newPitch = MathHelper.clamp(newPitch + pitchStep, -90.0F, 90.0F);
        }
        float gcd = SensUtils.getGCDValue();
        if (gcd > 0.0F) {
            newYaw -= (newYaw - currentYaw) % gcd;
            newPitch -= (newPitch - currentPitch) % gcd;
        }
        rotateVector = new Vector2f(newYaw, newPitch);

        if ((absYaw < 1.0F && absPitch < 1.0F) || this.spookyReturnTicks > 32) {
            this.finishSpookyReturn();
        }
    }

    private void finishSpookyReturn() {
        this.spookyReturnActive = false;
        if (mc.player != null) {
            rotateVector = new Vector2f(mc.player.rotationYaw, mc.player.rotationPitch);
        }
        this.clearMovementState();
        this.resetRotationState();
        super.onDisable();
    }

    public boolean onEnable() {
        boolean wasReturning = this.spookyReturnActive;
        if (!wasReturning) {
            super.onEnable();
        }
        this.spookyReturnActive = false;
        type.set(HitAuraRotationCloud.normalizeSelectedLabel(type.get()));
        this.reset();
        target = null;
        acceleration = 0.0F;
        isBack = false;
        lastJumpTime = 0;
        groundTicks = 0;
        this.resetRotationState();
        // Сброс CPS состояния
        completeClicks = 0;
        nextCps = minCps.getFloat();
        cpsTimer.reset();
        return false;
    }

    public boolean onDisable() {
        if (mc.player != null) {
            if (mc.player.isSprinting()) mc.player.setSprinting(false);
            if (HitAuraRotationCloud.isMode(this.type.get(), HitAuraRotationCloud.MODE_SPOOKY) && this.startSpookyReturn()) {
                this.clearMovementState();
                this.stopWatch.setLastMS(0L);
                target = null;
                return false;
            }
        }
        super.onDisable();
        this.reset();
        this.clearMovementState();
        this.stopWatch.setLastMS(0L);
        target = null;
        return false;
    }

    public ModeSetting getType() {
        return this.type;
    }

    public static ModeListSetting getOptions() {
        return options;
    }

    public ModeListSetting getMoreOptions() {
        return this.moreOptions;
    }

    public StopWatch getStopWatch() {
        return this.stopWatch;
    }

    public static LivingEntity getTarget() {
        return target;
    }

    public static void setTarget(LivingEntity t) {
        target = t;
    }

    // ==================== Rotation logic (inlined from ServerHitAuraRotationProvider) ====================

    private void resetRotationState() {
        this.spookyNoiseYaw = 0;
        this.spookyNoisePitch = 0;
        this.spookyExtraYaw = 0;
        this.spookyExtraPitch = 0;
        this.spookyLastNoiseUpdate = 0;
        this.spookyNextNoiseUpdateDelay = 85L;
        this.spookyLastRotateUpdate = System.currentTimeMillis();
        this.spookyLastAimPointUpdate = 0L;
        this.spookyNextAimPointUpdateDelay = 260L;
        this.spookyTrackedTarget = null;
        this.spookyAimPointX = 0.0F;
        this.spookyAimPointY = 0.0F;
        this.spookyAimPointZ = 0.0F;
        this.spookyYawVelocity = 0.0F;
        this.spookyPitchVelocity = 0.0F;
        this.spookyBurstPower = 0.0F;
        this.spookyBurstTime = 0L;
        this.spookyBurstHoldTime = 0L;
        this.spookyBurstCounter = 0;
        this.spookyHeldYaw = rotateVector.x;
        this.spookyHeldPitch = rotateVector.y;
        this.spookyWaveYaw = 0.0F;
        this.spookyWavePitch = 0.0F;
        this.spookyWaveLastUpdate = System.currentTimeMillis();
        this.spookyControlYawSpeed = 27.0F;
        this.spookyControlPitchSpeed = 15.0F;
        this.spookyReturnActive = false;
        this.spookyReturnLastUpdate = 0L;
        this.spookyReturnTicks = 0;
        this.spookyReturnYawVelocity = 0.0F;
        this.spookyReturnPitchVelocity = 0.0F;
        this.spookyVisibilityCounter = 0;
        this.funTimeYawMicroShake = 0.0F;
        this.funTimePitchMicroShake = 0.0F;
        this.funTimeLastMicroShakeUpdate = System.currentTimeMillis();
        this.funTimeBaseYawSpeed = randBetween(1608.0F, 1609.0F);
        this.funTimeBasePitchSpeed = randBetween(1.3F, 2.8F);
        this.funTimeIsFlickPhase = false;
        this.funTimeIsLookingDownPhase = false;
        this.funTimeLastPhaseChange = System.currentTimeMillis();
        this.funTimeNextPhaseChangeDelay = ThreadLocalRandom.current().nextLong(50000L, 100000L);
        this.funTimeFlickStartTime = 0L;
        this.funTimeFlickDurationTicks = 0;
        this.funTimeLastSwingTicks = 0;
        this.funTimeAttackSnapTimer = 0.0F;
        this.rwSnapJitterAmount = 0.0F;
        this.trainingYawMicroJitter = 0.0F;
        this.trainingPitchMicroJitter = 0.0F;
        this.trainingLastMicroUpdate = System.currentTimeMillis();
        this.trainingLastRotateUpdate = System.currentTimeMillis();
        this.trainingLastSpeedRefresh = System.currentTimeMillis();
        this.trainingBaseYawSpeed = randBetween(18.0F, 28.0F);
        this.trainingBasePitchSpeed = randBetween(8.0F, 14.0F);
        this.trainingTrackedTarget = null;
        this.trainingLockedPitch = 0.0F;
        this.trainingPitchLocked = false;
        this.resetSlotAcState();
        this.quantumTrackTicks = 0;
        this.quantumMatrixIndex = 0;
        this.quantumLastYaw = 0;
        this.quantumLastPitch = 0;
        this.quantumSelected = null;
        if (HitAuraRotationCloud.isMode(this.type.get(), HitAuraRotationCloud.MODE_RECORDING)) {
            RecordingManager rm = Harmony.getInstance().getRecordingManager();
            if (rm != null && !rm.isModelLoaded()) {
                rm.loadAllRecordings();
            }
        }
    }

    private boolean rotate(LivingEntity tgt, boolean isAttacking) {
        if (tgt == null || mc.player == null) return false;

        String mode = this.type.get();
        ElytraPredict elytraPredict = Harmony.getInstance().getModuleManager().getElytraPredict();

        if (!isSlotAcMode(mode) && mc.player.isElytraFlying() && tgt.isElytraFlying() && elytraPredict != null && elytraPredict.isState()) {
            rotationAnglesElytra(tgt, elytraPredict);
            return true;
        }
        if (isSlotAcMode(mode)) return rotateSlotAc(tgt, isAttacking);
        if (HitAuraRotationCloud.isMode(mode, HitAuraRotationCloud.MODE_SPOOKY)) return rotateSpooky(tgt, isAttacking);
        if (HitAuraRotationCloud.isMode(mode, HitAuraRotationCloud.MODE_FUNTIME)) return rotateFunTime(tgt, isAttacking);
        if (HitAuraRotationCloud.isMode(mode, HitAuraRotationCloud.MODE_RILLIWORLD)) return rotateRilliWorld(tgt, isAttacking);
        if (HitAuraRotationCloud.isMode(mode, HitAuraRotationCloud.MODE_TRAINING)) return rotateTraining(tgt, isAttacking);
        if (HitAuraRotationCloud.isMode(mode, HitAuraRotationCloud.MODE_GRIM)) return rotateGrim(tgt, isAttacking);
        if (HitAuraRotationCloud.isMode(mode, HitAuraRotationCloud.MODE_RECORDING)) return rotateRecording(tgt, isAttacking);
        if (HitAuraRotationCloud.isMode(mode, HitAuraRotationCloud.MODE_PACKET)) return rotatePacket(tgt, isAttacking);
        if (HitAuraRotationCloud.isMode(mode, HitAuraRotationCloud.MODE_TRACK)) return rotateTrack(tgt, isAttacking);
        if (HitAuraRotationCloud.isMode(mode, HitAuraRotationCloud.MODE_INTERACT)) {
            if (quantumTrackTicks > 0) {
                quantumTrackTicks--;
                return rotateInteract(tgt, isAttacking);
            }
            quantumReset();
            return false;
        }
        if (HitAuraRotationCloud.isMode(mode, HitAuraRotationCloud.MODE_LEGIT)) return rotateLegit(tgt, isAttacking);
        if (HitAuraRotationCloud.isMode(mode, HitAuraRotationCloud.MODE_MATRIX)) return rotateMatrix(tgt, isAttacking);
        return false;
    }

    private void onAttackRotation(LivingEntity tgt) {
        if (tgt == null) return;
        if (isSlotAcMode(this.type.get())) {
            this.slotAcTrackedTarget = tgt;
            this.slotAcHitPhase = 1;
            this.slotAcHitTimer = 0;
            this.slotAcPitchBeforeHit = rotateVector.y;
            this.slotAcCurrentPitch = rotateVector.y;
        }
        if (HitAuraRotationCloud.isMode(this.type.get(), HitAuraRotationCloud.MODE_INTERACT)) {
            quantumTrackTicks = interactTicks.getInt();
        }
    }

    // ===== QUANTUM-STYLE ROTATIONS =====

    private Vector3d quantumTargetVec(LivingEntity tgt) {
        return tgt.getPositionVec().add(0,
                MathHelper.clamp(mc.player.getPosYEye() - tgt.getPosY(),
                        0, tgt.getHeight() * (mc.player.getDistanceEyePos(tgt) / attackDistance())), 0)
                .subtract(mc.player.getEyePosition(1.0F));
    }

    private float quantumYaw(Vector3d vec) {
        return (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(vec.z, vec.x)) - 90);
    }

    private float quantumPitch(Vector3d vec) {
        return (float) (-Math.toDegrees(Math.atan2(vec.y, Math.hypot(vec.x, vec.z))));
    }

    private void quantumApplyGcd(Vector2f target) {
        float gcd = SensUtils.getGCDValue();
        float dY = target.x - rotateVector.x;
        float dP = target.y - rotateVector.y;
        float fixedDY = dY - dY % gcd;
        float fixedDP = dP - dP % gcd;
        rotateVector = new Vector2f(rotateVector.x + fixedDY, rotateVector.y + fixedDP);
    }

    private void quantumReset() {
        rotateVector = new Vector2f(mc.player.rotationYaw, mc.player.rotationPitch);
    }

    private boolean rotatePacket(LivingEntity tgt, boolean isAttacking) {
        Vector3d vec = quantumTargetVec(tgt);
        isRotated = true;

        float yawToTarget = quantumYaw(vec);
        float pitchToTarget = quantumPitch(vec);

        float randomYaw = (random.nextFloat() - 0.1F) * 0.2F;
        float randomPitch = (random.nextFloat() - 0.2F) * 0.3F;
        yawToTarget += randomYaw;
        pitchToTarget += randomPitch;

        float yawDelta = MathHelper.wrapDegrees(yawToTarget - rotateVector.x);
        float pitchDelta = MathHelper.wrapDegrees(pitchToTarget - rotateVector.y);

        float clampedYaw = MathHelper.clamp(Math.abs(yawDelta), 1.0f, 180F);
        float clampedPitch = MathHelper.clamp(Math.abs(pitchDelta), 1.0f, 90F);

        if (quantumSelected != tgt) {
            clampedPitch = Math.max(Math.abs(pitchDelta), 1.0f);
        } else {
            clampedPitch /= 3f;
        }

        if (Math.abs(clampedYaw - quantumLastYaw) <= 3.0f) {
            clampedYaw = quantumLastYaw + 3.1f;
        }

        float rY = rotateVector.x + (yawDelta > 0 ? clampedYaw : -clampedYaw);
        float rP = MathHelper.clamp(rotateVector.y + (pitchDelta > 0 ? clampedPitch : -clampedPitch), -89.0F, 89.0F);

        quantumApplyGcd(new Vector2f(rY, rP));
        quantumLastYaw = clampedYaw;
        quantumLastPitch = clampedPitch;
        quantumSelected = tgt;

        if (option(OPTION_MOVE_FIX_INDEX)) {
            mc.player.rotationYawOffset = rY;
        }
        return true;
    }

    private boolean rotateTrack(LivingEntity tgt, boolean isAttacking) {
        Vector3d vec = quantumTargetVec(tgt);
        isRotated = true;

        float yawToTarget = quantumYaw(vec);
        float pitchToTarget = quantumPitch(vec);

        float yawDelta = MathHelper.wrapDegrees(yawToTarget - rotateVector.x);
        float pitchDelta = MathHelper.wrapDegrees(pitchToTarget - rotateVector.y);

        float distanceFactor = (float) Math.min(1.0, mc.player.getDistanceEyePos(tgt) / attackDistance());
        float dynamicSpeed = rotationSpeed.get() * (0.5f + distanceFactor * 0.5f);

        float clampedYaw = MathHelper.clamp(Math.abs(yawDelta), 1.0f, dynamicSpeed);
        float clampedPitch = MathHelper.clamp(Math.abs(pitchDelta), 1.0f, dynamicSpeed / 2f);

        if (Math.abs(yawDelta) < 30) {
            clampedYaw *= 1.5f;
        }

        float rY = rotateVector.x + (yawDelta > 0 ? clampedYaw : -clampedYaw);
        float rP = MathHelper.clamp(rotateVector.y + (pitchDelta > 0 ? clampedPitch : -clampedPitch), -89.0F, 89.0F);

        quantumApplyGcd(new Vector2f(rY, rP));
        quantumLastYaw = clampedYaw;
        quantumLastPitch = clampedPitch;

        if (option(OPTION_MOVE_FIX_INDEX)) {
            mc.player.rotationYawOffset = rY;
        }
        return true;
    }

    private boolean rotateInteract(LivingEntity tgt, boolean isAttacking) {
        Vector3d vec = quantumTargetVec(tgt);
        isRotated = true;

        float yawToTarget = quantumYaw(vec);
        float pitchToTarget = quantumPitch(vec);

        float yawDelta = MathHelper.wrapDegrees(yawToTarget - rotateVector.x);
        float pitchDelta = MathHelper.wrapDegrees(pitchToTarget - rotateVector.y);

        float clampedYaw = MathHelper.clamp(Math.abs(yawDelta), 1.0f, rotationSpeed.get());
        float clampedPitch = MathHelper.clamp(Math.abs(pitchDelta), 1.0f, rotationSpeed.get() / 2f);

        float rY = rotateVector.x + (yawDelta > 0 ? clampedYaw : -clampedYaw);
        float rP = MathHelper.clamp(rotateVector.y + (pitchDelta > 0 ? clampedPitch : -clampedPitch), -89.0F, 89.0F);

        quantumApplyGcd(new Vector2f(rY, rP));
        return true;
    }

    private boolean rotateLegit(LivingEntity tgt, boolean isAttacking) {
        Vector3d vec = quantumTargetVec(tgt);
        isRotated = true;

        float yawToTarget = quantumYaw(vec);
        float pitchToTarget = quantumPitch(vec);

        float yawDelta = MathHelper.wrapDegrees(yawToTarget - rotateVector.x);
        float pitchDelta = MathHelper.wrapDegrees(pitchToTarget - rotateVector.y);

        float clampedYaw = MathHelper.clamp(Math.abs(yawDelta), 1.0f, 30f);
        float clampedPitch = MathHelper.clamp(Math.abs(pitchDelta), 1.0f, 25f);

        if (quantumSelected != tgt) {
            clampedPitch = Math.max(Math.abs(pitchDelta), 1.0f);
        } else {
            clampedPitch /= 3f;
        }

        if (Math.abs(clampedYaw - quantumLastYaw) <= 3.0f) {
            clampedYaw = quantumLastYaw + 3.1f;
        }

        float rY = rotateVector.x + (yawDelta > 0 ? clampedYaw : -clampedYaw);
        float rP = MathHelper.clamp(rotateVector.y + (pitchDelta > 0 ? clampedPitch : -clampedPitch), -89.0F, 89.0F);

        quantumApplyGcd(new Vector2f(rY, rP));
        quantumLastYaw = clampedYaw;
        quantumLastPitch = clampedPitch;
        quantumSelected = tgt;

        if (option(OPTION_MOVE_FIX_INDEX)) {
            mc.player.rotationYawOffset = rY;
        }
        return true;
    }

    private boolean rotateMatrix(LivingEntity tgt, boolean isAttacking) {
        Vector3d vec = quantumTargetVec(tgt);
        isRotated = true;

        float yawToTarget = quantumYaw(vec);
        float pitchToTarget = quantumPitch(vec);

        float yawDelta = MathHelper.wrapDegrees(yawToTarget - rotateVector.x);
        float pitchDelta = MathHelper.wrapDegrees(pitchToTarget - rotateVector.y);

        quantumMatrixIndex++;

        double absYaw = Math.abs(yawDelta);
        float factor;
        if (absYaw >= 90.0F && quantumMatrixIndex >= 3) {
            factor = 0.45F + random.nextFloat() * 0.01F;
            quantumMatrixIndex = 0;
        } else if (quantumMatrixIndex == 1) {
            factor = 0.75F + random.nextFloat() * 0.01F;
        } else if (quantumMatrixIndex == 2) {
            factor = 0.9F + random.nextFloat() * 0.01F;
        } else {
            factor = 0.6F + random.nextFloat() * 0.02F;
        }

        float yawStep = yawDelta * factor;
        float pitchStep = pitchDelta * factor;

        float rY = rotateVector.x + yawStep;
        float rP = MathHelper.clamp(rotateVector.y + pitchStep, -89.0F, 89.0F);

        quantumApplyGcd(new Vector2f(rY, rP));
        quantumLastYaw = yawStep;
        quantumLastPitch = pitchStep;

        if (option(OPTION_MOVE_FIX_INDEX)) {
            mc.player.rotationYawOffset = rY;
        }
        return true;
    }

    // ===== SPOOKY (wasteland-style) =====
    private boolean rotateSpooky(LivingEntity tgt, boolean isAttacking) {
        long now = System.currentTimeMillis();

        if (this.spookyTrackedTarget != tgt) {
            this.spookyTrackedTarget = tgt;
            this.spookyLastAimPointUpdate = 0L;
            this.spookyLastRotateUpdate = now - 50L;
            this.spookyYawVelocity = 0.0F;
            this.spookyPitchVelocity = 0.0F;
            this.spookyBurstPower = 0.0F;
            this.spookyBurstTime = 0L;
            this.spookyBurstHoldTime = 0L;
            this.spookyBurstCounter = 0;
            this.spookyHeldYaw = rotateVector.x;
            this.spookyHeldPitch = rotateVector.y;
            this.spookyWaveYaw = 0.0F;
            this.spookyWavePitch = 0.0F;
            this.spookyWaveLastUpdate = now;
        }

        Vector3d eyePos = mc.player.getEyePosition(mc.getRenderPartialTicks());
        float zWave = (float) Math.cos((double) now / 250.0D) * 0.2F;
        float xWave = (float) Math.cos((double) now / 340.0D) * 0.3F;
        float yWave = (float) Math.cos((double) now / 120.0D) * 0.5F;

        Vector3d baseAim = tgt.getPositionVec().add(
                xWave,
                MathHelper.clamp(eyePos.y - tgt.getPosY(), 0.0D, 1.5D) + yWave,
                zWave
        );
        Vector3d direction = baseAim.subtract(eyePos);
        if (direction.lengthSquared() < 1.0E-6D) return false;
        direction = direction.normalize();

        float desiredYaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        float desiredPitch = (float) MathHelper.clamp(
                -Math.toDegrees(Math.atan2(direction.y, Math.hypot(direction.x, direction.z))),
                -90.0D,
                90.0D
        );
        float yawSpeed = this.spookyBurstCounter % 3 == 0 ? randBetween(12.0F, 15.0F) : randBetween(6.0F, 10.0F);
        float pitchSpeed = randBetween(8.0F, 15.0F);
        boolean burstAim = false;
        boolean multipointSmooth = true;

        if (isAttacking && mc.player.getDistance(tgt) < this.attackDistance() && !this.isSpookyUseBlocked()) {
            this.spookyBurstPower = randBetween(1.3F, 1.6F);
            this.spookyBurstTime = now;
            ++this.spookyBurstCounter;
            this.spookyBurstHoldTime = this.spookyBurstCounter % 2 == 0
                    ? ThreadLocalRandom.current().nextLong(95L, 126L)
                    : ThreadLocalRandom.current().nextLong(138L, 151L);
            this.spookyAimPointX = 0.0F;
            this.spookyAimPointZ = 0.0F;
        }

        if (this.spookyBurstPower > randBetween(1.0F, 1.1F)) {
            burstAim = true;
            this.spookyBurstPower = randBetween(0.1F, 0.6F);
        }

        if (burstAim) {
            Vector3d burstAimPos = tgt.getPositionVec().add(
                    this.spookyAimPointX,
                    MathHelper.clamp(eyePos.y - tgt.getPosY(), 0.0D, 1.0D) + yWave / 2.0F,
                    this.spookyAimPointZ
            );
            float[] burstRotations = this.getSpookyRotations(eyePos, burstAimPos);
            desiredYaw = burstRotations[0];
            desiredPitch = burstRotations[1];
            yawSpeed = randBetween(10.0F, 15.0F);
            pitchSpeed = randBetween(10.0F, 15.0F);
            this.spookyHeldYaw = desiredYaw;
            this.spookyHeldPitch = desiredPitch;
        } else if (now - this.spookyBurstTime < this.spookyBurstHoldTime) {
            desiredYaw = this.spookyHeldYaw;
            desiredPitch = this.spookyHeldPitch;
            yawSpeed = randBetween(10.0F, 20.0F);
            pitchSpeed = randBetween(10.0F, 20.0F);
        } else if (multipointSmooth) {
            yawSpeed = randBetween(16.0F, 24.0F);
            pitchSpeed = randBetween(16.0F, 24.0F);
        }

        float targetWaveYaw = (float) Math.cos((double) now / 60.0D) * randBetween(8.0F, 12.0F);
        float targetWavePitch = (float) Math.sin((double) now / 65.0D) * randBetween(3.0F, 6.0F);
        if (multipointSmooth) {
            long waveDt = now - this.spookyWaveLastUpdate;
            if (waveDt < 1L || waveDt > 250L) waveDt = 50L;
            this.spookyWaveLastUpdate = now;
            this.spookyWaveYaw = this.stepSpookyValue(this.spookyWaveYaw, targetWaveYaw, (float) waveDt / 240.0F);
            this.spookyWavePitch = this.stepSpookyValue(this.spookyWavePitch, targetWavePitch, (float) waveDt / 240.0F);
        } else {
            this.spookyWaveYaw = targetWaveYaw;
            this.spookyWavePitch = targetWavePitch;
        }

        desiredYaw += this.spookyWaveYaw;
        desiredPitch = MathHelper.clamp(desiredPitch + this.spookyWavePitch, -90.0F, 90.0F);
        this.spookyControlYawSpeed = randBetween(27.0F, 32.0F);
        this.spookyControlPitchSpeed = randBetween(15.0F, 20.0F);

        float activeYawSpeed = isAttacking ? Math.max(yawSpeed, this.spookyControlYawSpeed) : yawSpeed;
        float activePitchSpeed = isAttacking ? Math.max(pitchSpeed, this.spookyControlPitchSpeed) : pitchSpeed;
        this.applySpookyRotationStep(desiredYaw, desiredPitch, activeYawSpeed, activePitchSpeed);
        return true;
    }

    private void alignSpookyBeforeAttack(LivingEntity tgt) {
        if (tgt == null || mc.player == null) {
            return;
        }

        Vector3d eyePos = mc.player.getEyePosition(mc.getRenderPartialTicks());
        Vector3d aimPos = tgt.getPositionVec().add(
                0.0D,
                MathHelper.clamp(eyePos.y - tgt.getPosY(), 0.0D, 1.0D),
                0.0D
        );
        float[] rotations = this.getSpookyRotations(eyePos, aimPos);
        float yawDelta = Math.abs(MathHelper.wrapDegrees(rotations[0] - rotateVector.x));
        float pitchDelta = Math.abs(rotations[1] - rotateVector.y);
        float yawSpeed = MathHelper.clamp(yawDelta, 27.0F, 48.0F);
        float pitchSpeed = MathHelper.clamp(pitchDelta, 15.0F, 28.0F);

        this.applySpookyRotationStep(rotations[0], rotations[1], yawSpeed, pitchSpeed);
        this.spookyHeldYaw = rotations[0];
        this.spookyHeldPitch = rotations[1];
        this.spookyBurstTime = System.currentTimeMillis();
        this.spookyBurstHoldTime = ThreadLocalRandom.current().nextLong(95L, 126L);
    }

    private void applySpookyRotationStep(float desiredYaw, float desiredPitch, float yawSpeed, float pitchSpeed) {
        float currentYaw = rotateVector.x;
        float currentPitch = rotateVector.y;
        float yawDelta = MathHelper.wrapDegrees(desiredYaw - currentYaw);
        float pitchDelta = desiredPitch - currentPitch;
        float distanceFactor = Math.min(1.0F, Math.max(0.1F, (float) Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta)) / 45.0F));
        float yawStep = MathHelper.clamp(yawDelta, -yawSpeed * distanceFactor, yawSpeed * distanceFactor);
        float pitchStep = MathHelper.clamp(pitchDelta, -pitchSpeed * distanceFactor, pitchSpeed * distanceFactor);

        float newYaw = currentYaw;
        float newPitch = currentPitch;
        if (Math.abs(yawStep) > 0.0001F) {
            newYaw += yawStep;
        }
        if (Math.abs(pitchStep) > 0.0001F) {
            newPitch = MathHelper.clamp(newPitch + pitchStep, -90.0F, 90.0F);
        }

        float gcd = SensUtils.getGCDValue();
        if (gcd > 0.0F) {
            newYaw -= (newYaw - currentYaw) % gcd;
            newPitch -= (newPitch - currentPitch) % gcd;
        }
        rotateVector = new Vector2f(newYaw, newPitch);
    }

    private float stepSpookyValue(float current, float target, float step) {
        if (Math.abs(target - current) <= step) {
            return target;
        }
        return current + Math.signum(target - current) * step;
    }

    private boolean isSpookyUseBlocked() {
        return moreOption(MORE_OPTION_NO_EAT_INDEX) && this.isPlayerEating();
    }

    private void refreshSpookyAimPoint() {
        this.spookyAimPointX = 0.0F;
        this.spookyAimPointY = 0.0F;
        this.spookyAimPointZ = 0.0F;
    }

        // Шум каждые 80мс с 35% шанса

    private float[] getSpookyRotations(Vector3d eyePos, Vector3d point) {
        Vector3d direction = point.subtract(eyePos).normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        float pitch = (float) MathHelper.clamp(-Math.toDegrees(Math.atan2(direction.y, Math.hypot(direction.x, direction.z))), -90.0D, 90.0D);
        return new float[]{yaw, pitch};
    }

    private boolean rotateFunTime(LivingEntity tgt, boolean isAttacking) {
        long now = System.currentTimeMillis();
        if (!this.funTimeIsFlickPhase && !this.funTimeIsLookingDownPhase && now - this.funTimeLastPhaseChange >= this.funTimeNextPhaseChangeDelay) this.funTimeIsLookingDownPhase = true;
        int swingTicks = mc.player.ticksSinceLastSwing;
        if (this.funTimeIsLookingDownPhase && swingTicks < this.funTimeLastSwingTicks) {
            this.funTimeIsFlickPhase = true; this.funTimeIsLookingDownPhase = false;
            this.funTimeFlickStartTime = now; this.funTimeFlickDurationTicks = ThreadLocalRandom.current().nextInt(231, 300);
            this.funTimeLastPhaseChange = now; this.funTimeNextPhaseChangeDelay = ThreadLocalRandom.current().nextLong(60000L, 180000L);
        }
        this.funTimeLastSwingTicks = swingTicks;
        if (this.funTimeIsFlickPhase && now - this.funTimeFlickStartTime >= (long) this.funTimeFlickDurationTicks * 50L) this.funTimeIsFlickPhase = false;
        Vector3d eyePos = mc.player.getEyePosition(mc.getRenderPartialTicks());
        double hr = (double) tgt.getHeight() * 0.85 / Math.max(3.0, (double) attackDistance());
        Vector3d aimPos = tgt.getPositionVec().add(0.0, MathHelper.clamp(eyePos.y - tgt.getPosY(), 0.0, (double) tgt.getHeight() * hr), 0.0);
        double lf = getLeadFactor();
        if (lf > 0.0) aimPos = aimPos.add((tgt.getPosX() - tgt.prevPosX) * lf, 0.0, (tgt.getPosZ() - tgt.prevPosZ) * lf);
        Vector3d dir = aimPos.subtract(eyePos).normalize();
        float tYaw = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
        float tPitch = (float) MathHelper.clamp(-Math.toDegrees(Math.atan2(dir.y, Math.hypot(dir.x, dir.z))), -90.0, 90.0);
        float cY = rotateVector.x, cP = rotateVector.y;
        float ys = randBetween(0.0F, 2.0F), ps = randBetween(0.0F, 2.0F);
        boolean snap = false;
        if (isAttacking) this.funTimeAttackSnapTimer = 4.0F;
        if (this.funTimeAttackSnapTimer > 1.0F) { snap = true; --this.funTimeAttackSnapTimer; }
        if (snap) { cP = tPitch; ps = randBetween(60.0F, 80.0F); cY = tYaw; ys = randBetween(70.0F, 95.0F); }
        float t = (float) now / 1000.0F; float am = snap ? 0.3F : 1.0F;
        float mY = (float) Math.sin(t * 0.6) * 0.2F; float mP = (float) Math.cos(t * 0.8) * 0.8F * am;
        this.funTimeYawMicroShake += (mY - this.funTimeYawMicroShake) * 0.75F;
        this.funTimePitchMicroShake += (mP - this.funTimePitchMicroShake) * 0.75F;
        float dY = MathHelper.wrapDegrees(cY - rotateVector.x);
        float dP = MathHelper.wrapDegrees((this.funTimeIsLookingDownPhase ? cP : (this.funTimeIsFlickPhase ? randBetween(-75.0F, -66.0F) : cP)) - rotateVector.y);
        float fPS = this.funTimeIsLookingDownPhase ? ps : (this.funTimeIsFlickPhase ? 360.0F : ps);
        float rY = rotateVector.x + dY * Math.min(1.0F, ys / Math.max(1.0F, Math.abs(dY)));
        float rP = rotateVector.y + dP * Math.min(1.0F, fPS / Math.max(1.0F, Math.abs(dP)));
        rY = MathHelper.wrapDegrees(rY + this.funTimeYawMicroShake);
        rP = this.funTimeIsFlickPhase ? MathHelper.clamp(randBetween(-75.0F, -66.0F) + this.funTimePitchMicroShake, -90.0F, 90.0F) : MathHelper.clamp(rP + this.funTimePitchMicroShake, -90.0F, 90.0F);
        float gcd = SensUtils.getGCDValue();
        rY -= (rY - rotateVector.x) % gcd; rP -= (rP - rotateVector.y) % gcd;
        rotateVector = new Vector2f(rY, rP);
        return true;
    }

    private boolean rotateRilliWorld(LivingEntity tgt, boolean isAttacking) {
        if (shouldPlayerFalling()) this.rwSnapJitterAmount = randBetween(0.05F, 0.3F);
        double radius = 0.3, hh = (double) tgt.getHeight() / 2.0, speed = 0.15;
        double time = (double) System.currentTimeMillis() / 1000.0, angle = time * speed * 2.0 * Math.PI;
        double aX = tgt.getPosX() + Math.cos(angle) * radius, aZ = tgt.getPosZ() + Math.sin(angle) * radius, aY = tgt.getPosY() + hh;
        Vector3d aimPos = new Vector3d(aX, aY, aZ);
        double yawRad = Math.toRadians(tgt.rotationYaw), rpD = (double) (Float) this.runPastBlocks.get();
        double rpX = -Math.sin(yawRad) * rpD, rpZ = Math.cos(yawRad) * rpD;
        boolean rp = isRunPastEnabled();
        double pS = Math.sqrt(Math.pow(mc.player.getPosX() - mc.player.prevPosX, 2) + Math.pow(mc.player.getPosZ() - mc.player.prevPosZ, 2)) * 20.0;
        double tS = Math.sqrt(Math.pow(tgt.getPosX() - tgt.prevPosX, 2) + Math.pow(tgt.getPosZ() - tgt.prevPosZ, 2)) * 20.0;
        Vector3d toT = aimPos.subtract(mc.player.getEyePosition(1.0F)).add(rp && pS >= tS ? rpX : 0.0, 0.0, rp ? rpZ : 0.0);
        float dY = (float) (Math.toDegrees(Math.atan2(toT.z, toT.x)) - 90.0);
        float dP = (float) (-Math.toDegrees(Math.atan2(toT.y, Math.hypot(toT.x, toT.z))));
        AxisAlignedBB bb = tgt.getBoundingBox(); Vector3d eye = mc.player.getEyePosition(1.0F);
        float minY = Float.POSITIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, minP2 = Float.POSITIVE_INFINITY, maxP2 = Float.NEGATIVE_INFINITY;
        for (double x : new double[]{bb.minX, bb.maxX}) for (double y : new double[]{bb.minY, bb.maxY}) for (double z : new double[]{bb.minZ, bb.maxZ}) {
            Vector3d c = new Vector3d(x, y, z).subtract(eye);
            float cy = (float) (Math.toDegrees(Math.atan2(c.z, c.x)) - 90.0), cp = (float) (-Math.toDegrees(Math.atan2(c.y, Math.hypot(c.x, c.z))));
            minY = Math.min(minY, cy); maxY = Math.max(maxY, cy); minP2 = Math.min(minP2, cp); maxP2 = Math.max(maxP2, cp);
        }
        dY = MathHelper.clamp(dY, minY, maxY); dP = MathHelper.clamp(dP, minP2, maxP2);
        float ddY = MathHelper.wrapDegrees(dY - rotateVector.x), ddP = MathHelper.wrapDegrees(dP - rotateVector.y);
        float yS = Math.min(Math.max(Math.abs(ddY), 1.0F), randBetween(5000.0F, 9999.0F)), pSt = Math.max(Math.abs(ddP), 1.0F);
        float rY = rotateVector.x + (ddY > 0 ? yS : -yS);
        float rP = MathHelper.clamp(rotateVector.y + (ddP > 0 ? pSt : -pSt), -89.0F, 89.0F);
        float gcd = SensUtils.getGCDValue();
        rY -= (rY - rotateVector.x) % gcd; rP -= (rP - rotateVector.y) % gcd;
        rotateVector = new Vector2f(rY, rP);
        return true;
    }

    private boolean rotateTraining(LivingEntity tgt, boolean isAttacking) {
        long now = System.currentTimeMillis();
        if (now - this.trainingLastSpeedRefresh >= 600L) { this.trainingBaseYawSpeed = randBetween(18.0F, 28.0F); this.trainingBasePitchSpeed = randBetween(8.0F, 14.0F); this.trainingLastSpeedRefresh = now; }
        Vector3d eyePos = mc.player.getEyePosition(mc.getRenderPartialTicks());
        double ah = MathHelper.clamp((double) tgt.getHeight() * 0.62, 0.45, Math.max(0.45, (double) tgt.getHeight() - 0.2));
        Vector3d aimPos = tgt.getPositionVec().add(0.0, ah, 0.0);
        double dx = tgt.getPosX() - tgt.prevPosX, dz = tgt.getPosZ() - tgt.prevPosZ;
        double lf = getLeadFactor(), lead = lf > 0.0 ? lf * 0.35 : 0.08;
        aimPos = aimPos.add(dx * lead, 0.0, dz * lead);
        Vector3d dir = aimPos.subtract(eyePos).normalize();
        float tY = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
        float tP = (float) MathHelper.clamp(-Math.toDegrees(Math.atan2(dir.y, Math.hypot(dir.x, dir.z))), -90.0, 90.0);
        if (this.trainingTrackedTarget != tgt) { this.trainingTrackedTarget = tgt; this.trainingLockedPitch = tP; this.trainingPitchLocked = true; }
        float cY = rotateVector.x, cP = rotateVector.y;
        float yDiff = MathHelper.wrapDegrees(tY - cY);
        if (now - this.trainingLastMicroUpdate >= 120L) { this.trainingYawMicroJitter = randBetween(-0.16F, 0.16F); this.trainingPitchMicroJitter = 0.0F; this.trainingLastMicroUpdate = now; }
        float os = 0.0F;
        if (Math.abs(yDiff) > 35.0F) os = Math.signum(yDiff) * randBetween(0.35F, 1.15F);
        else if (Math.abs(yDiff) < 4.5F) os = Math.signum(yDiff) * randBetween(-0.08F, 0.18F);
        float desY = tY + this.trainingYawMicroJitter + os, desP = this.trainingLockedPitch;
        long dt2 = Math.max(1L, now - this.trainingLastRotateUpdate);
        float ySpd = (this.trainingBaseYawSpeed + (isAttacking ? randBetween(7.0F, 12.0F) : randBetween(0.0F, 3.0F))) * (float) dt2 / 50.0F;
        float pSpd = (this.trainingBasePitchSpeed + (isAttacking ? randBetween(4.0F, 8.0F) : randBetween(0.0F, 2.0F))) * (float) dt2 / 50.0F;
        float rY = approachAngle(cY, desY, ySpd), rP = MathHelper.clamp(approachAngle(cP, desP, pSpd), -90.0F, 90.0F);
        float gcd = SensUtils.getGCDValue();
        rY -= (rY - cY) % gcd; rP -= (rP - cP) % gcd;
        rotateVector = new Vector2f(rY, rP); this.trainingLastRotateUpdate = now;
        return true;
    }

    private boolean rotateGrim(LivingEntity tgt, boolean isAttacking) {
        if (isAttacking) this.updateAttack();
        Vector3d eyePos = mc.player.getEyePosition(mc.getRenderPartialTicks());
        Vector3d aimPos = tgt.getPositionVec().add(0.0, MathHelper.clamp(eyePos.y - tgt.getPosY(), 0.0, (double) tgt.getHeight() / 2.0), 0.0);
        Vector3d dir = aimPos.subtract(eyePos).normalize();
        float tY = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
        float tP = (float) MathHelper.clamp(-Math.toDegrees(Math.atan2(dir.y, Math.hypot(dir.x, dir.z))), -90.0, 90.0);
        float dY = MathHelper.wrapDegrees(tY - rotateVector.x), dP = MathHelper.wrapDegrees(tP - rotateVector.y);
        float rY = rotateVector.x + dY, rP = MathHelper.clamp(rotateVector.y + dP, -89.0F, 89.0F);
        float gcd = SensUtils.getGCDValue();
        rY -= (rY - rotateVector.x) % gcd; rP -= (rP - rotateVector.y) % gcd;
        rotateVector = new Vector2f(rY, rP);
        return true;
    }

    private boolean rotateRecording(LivingEntity tgt, boolean isAttacking) {
        RecordingManager rm = Harmony.getInstance().getRecordingManager();
        Vector3d eyePos = mc.player.getEyePosition(1.0F);
        float offY = rm != null ? rm.getAimOffsetY() : 0, offX = rm != null ? rm.getAimOffsetX() : 0;
        double clY = MathHelper.clamp(eyePos.y - tgt.getPosY(), 0.2, (double) tgt.getHeight() * 0.9) + offY;
        Vector3d aimPos = tgt.getPositionVec().add(offX, clY, 0.0);
        double dx = tgt.getPosX() - tgt.prevPosX, dz = tgt.getPosZ() - tgt.prevPosZ;
        double lf = getLeadFactor(), lead = lf > 0.0 ? lf : 1.5 + ThreadLocalRandom.current().nextDouble() * 1.5;
        aimPos = aimPos.add(dx * lead, 0.0, dz * lead);
        Vector3d dir = aimPos.subtract(eyePos).normalize();
        float tY = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
        float tP = (float) MathHelper.clamp(-Math.toDegrees(Math.atan2(dir.y, Math.hypot(dir.x, dir.z))), -90.0, 90.0);
        float dYaw = MathHelper.wrapDegrees(tY - rotateVector.x), dPitch = MathHelper.wrapDegrees(tP - rotateVector.y);
        float dist = mc.player.getDistance(tgt), cd = mc.player.getCooledAttackStrength(0.0F);
        float sY, sP;
        if (rm != null && rm.isModelLoaded()) {
            float[] l = rm.queryLearnedRotation(dYaw, dPitch, dist, cd); sY = l[0]; sP = l[1];
            float[] sh = rm.getRecordedShake(); float m = Math.abs(dYaw) < 8.0F ? 1.0F : 0.3F; sY += sh[0] * m; sP += sh[1] * m;
        } else {
            float sp = 15.0F + ThreadLocalRandom.current().nextFloat() * 15.0F;
            sY = dYaw * Math.min(1.0F, sp / Math.max(1.0F, Math.abs(dYaw))); sP = dPitch * Math.min(1.0F, sp / Math.max(1.0F, Math.abs(dPitch)));
            sY += (ThreadLocalRandom.current().nextFloat() - 0.5F) * 0.4F; sP += (ThreadLocalRandom.current().nextFloat() - 0.5F) * 0.3F;
        }
        float rY = rotateVector.x + sY, rP = MathHelper.clamp(rotateVector.y + sP, -90.0F, 90.0F);
        float gcd = SensUtils.getGCDValue();
        rY -= (rY - rotateVector.x) % gcd; rP -= (rP - rotateVector.y) % gcd;
        rotateVector = new Vector2f(rY, rP);
        return true;
    }

    private boolean rotateSlotAc(LivingEntity tgt, boolean isAttacking) {
        Vector3d eyePos = mc.player.getEyePosition(1.0F);
        Vector3d center = tgt.getBoundingBox().getCenter();
        Vector3d toAim = center.subtract(eyePos);
        float dY = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(toAim.z, toAim.x)) - 90.0);
        float dP = (float) (-Math.toDegrees(Math.atan2(toAim.y, Math.hypot(toAim.x, toAim.z))));
        float diffY = MathHelper.wrapDegrees(dY - rotateVector.x);
        float diffP = dP - rotateVector.y;
        float speed = 0.18F + (float) Math.random() * 0.04F;
        float damping = 0.34F;
        this.slotAcVelocityYaw = this.slotAcVelocityYaw * (1.0F - damping) + diffY * speed;
        this.slotAcVelocityPitch = this.slotAcVelocityPitch * (1.0F - damping) + diffP * speed * 0.87F;
        this.slotAcVelocityYaw = MathHelper.clamp(this.slotAcVelocityYaw, -30.0F, 30.0F);
        this.slotAcVelocityPitch = MathHelper.clamp(this.slotAcVelocityPitch, -20.0F, 20.0F);
        float rY = rotateVector.x + this.slotAcVelocityYaw;
        float rP = MathHelper.clamp(rotateVector.y + this.slotAcVelocityPitch, -89.0F, 89.0F);
        float gcd = SensUtils.getGCDValue();
        rY -= (rY - rotateVector.x) % gcd; rP -= (rP - rotateVector.y) % gcd;
        rotateVector = new Vector2f(rY, rP);
        return true;
    }

    private void resetSlotAcState() {
        this.slotAcTrackedTarget = null; this.slotAcVelocityYaw = 0; this.slotAcVelocityPitch = 0;
        this.slotAcAimPointX = 0; this.slotAcAimPointY = 0; this.slotAcAimPointZ = 0;
        this.slotAcNoiseAngle = 0; this.slotAcHitPhase = 0; this.slotAcHitTimer = 0;
        this.slotAcFirstSeenTime = 0; this.slotAcReactionMs = 0; this.slotAcReactionComplete = false;
        this.slotAcElytraSpeed = 0; this.slotAcElytraReturning = false;
        this.slotAcAdjYaw = 1.0F; this.slotAcAdjPitch = 1.0F; this.slotAcLastAimPointChangeTime = 0;
        this.slotAcCurrentYaw = rotateVector != null ? rotateVector.x : 0;
        this.slotAcCurrentPitch = rotateVector != null ? rotateVector.y : 0;
        this.slotAcLastSentYaw = this.slotAcCurrentYaw; this.slotAcLastSentPitch = this.slotAcCurrentPitch;
    }

    // ===== Rotation utilities =====
    private static float randBetween(float min, float max) { return min + ThreadLocalRandom.current().nextFloat() * (max - min); }
    private static boolean isSlotAcMode(String m) { if (m == null) return false; String n = m.trim().toLowerCase(Locale.ROOT).replace(" ","").replace("_","").replace("-",""); return "slotac".equals(n)||"slot".equals(n)||"sloth".equals(n)||"slothrotation".equals(n)||"слотас".equals(n); }
    private static float approachAngle(float c, float t, float s) { float d = MathHelper.wrapDegrees(t - c); return Math.abs(d) <= s ? t : c + Math.signum(d) * s; }

    private static boolean canUseElytraLead() {
        ElytraTarget et = Harmony.getInstance().getModuleManager().getElytraTarget();
        return mc.player != null && mc.player.isElytraFlying()
                && et != null && et.isState() && ElytraTarget.shouldElytraTarget;
    }

    private double getLeadFactor() {
        if (canUseElytraLead()) {
            ElytraTarget et = Harmony.getInstance().getModuleManager().getElytraTarget();
            return et != null ? (double) (Float) et.elytraForward.get() : 0.0;
        }
        return this.predict != null && Boolean.TRUE.equals(this.predict.get()) && this.predictStrength != null
                ? (double) (Float) this.predictStrength.get() * 10.0 : 0.0;
    }

    private boolean isRunPastEnabled() {
        ModeListSetting mo = this.moreOptions;
        if (mo == null) return false;
        List<?> l = (List<?>) mo.get();
        if (l != null && l.size() > MORE_OPTION_RUN_PAST_INDEX) {
            BooleanSetting s = (BooleanSetting) l.get(MORE_OPTION_RUN_PAST_INDEX);
            return s != null && Boolean.TRUE.equals(s.get());
        }
        return false;
    }

    private void rotationAnglesElytra(LivingEntity tgt, ElytraPredict ep) {
        double pd = ep.getDistance(tgt);
        Vector3d off = new Vector3d(0, MathHelper.clamp(tgt.getPosY() - (double) tgt.getHeight(), 0, (double) (tgt.getHeight() / 2.0F)), 0);
        Vector3d aim = tgt.getPositionVec().add(off);
        Vector3d eye = mc.player.getEyePosition(1.0F);
        Vector3d mot = tgt.getMotion().mul(pd, pd, pd);
        Vector3d toT = aim.subtract(eye);
        if (ep.canPredict(tgt)) toT = toT.add(mot);
        float y = (float) (((Math.toDegrees(Math.atan2(toT.z, toT.x)) - 90.0 - mc.player.rotationYaw) % 360.0 + 540.0) % 360.0 - 180.0);
        float p = (float) Math.min(90.0, -Math.toDegrees(Math.atan2(toT.y, Math.hypot(toT.x, toT.z))));
        float rY = mc.player.rotationYaw + y, rP = MathHelper.clamp(p, -90.0F, 90.0F);
        float gcd = SensUtils.getGCDValue();
        rY -= rY % gcd;
        rP -= rP % gcd;
        rotateVector = new Vector2f(rY, rP);
    }
}
