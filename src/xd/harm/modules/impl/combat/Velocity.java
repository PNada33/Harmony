package xd.harm.modules.impl.combat;

import com.google.common.eventbus.Subscribe;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.server.*;
import net.minecraft.potion.Effects;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import xd.harm.events.network.EventPacket;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.ModeSetting;
import xd.harm.modules.settings.impl.SliderSetting;
import xd.harm.utils.client.TimerUtility;
import xd.harm.utils.player.MoveUtils;

@ModuleRegister(name = "Velocity", category = Category.Combat, desc = "Уменьшает кнокбэк при ударе")
public class Velocity extends Module {

    // === Настройки ===

    private final ModeSetting mode = new ModeSetting("Режим", "Отмена",
            "Отмена", "Простой", "Легит", "Реверс", "Горизонталь", "Буст", "Матрикс", "Грим", "Полярный");

    // --- Sub-mode ---
    private final ModeSetting matrixMode = new ModeSetting("Матрикс Режим", "Мотион 1", "Мотион 1", "Мотион 2")
            .setVisible(() -> mode.is("Матрикс"));

    private final ModeSetting grimMode = new ModeSetting("Грим Режим", "2.3.71", "2.3.71", "2.3.72", "Симпле")
            .setVisible(() -> mode.is("Грим"));

    // --- Простой ---
    private final SliderSetting simpleHorizontal = new SliderSetting("Горизонталь %", 100f, 0f, 300f, 1f)
            .setVisible(() -> mode.is("Простой"));
    private final SliderSetting simpleVertical = new SliderSetting("Вертикаль %", 100f, 0f, 300f, 1f)
            .setVisible(() -> mode.is("Простой"));

    // --- Легит ---
    private final BooleanSetting legitJump = new BooleanSetting("Легит прыжок", true)
            .setVisible(() -> mode.is("Легит"));
    private final ModeSetting legitJumpMode = new ModeSetting("Режим прыжка", "Прыжки", "Прыжки", "Шанс")
            .setVisible(() -> mode.is("Легит") && legitJump.get());
    private final SliderSetting legitJumps = new SliderSetting("Кол-во прыжков", 2f, 1f, 10f, 1f)
            .setVisible(() -> mode.is("Легит") && legitJump.get() && legitJumpMode.is("Прыжки"));
    private final SliderSetting legitJumpChance = new SliderSetting("Шанс прыжка", 50f, 1f, 100f, 1f)
            .setVisible(() -> mode.is("Легит") && legitJump.get() && legitJumpMode.is("Шанс"));

    private final BooleanSetting doubleReduce = new BooleanSetting("Двойное уменьшение", false)
            .setVisible(() -> mode.is("Легит"));
    private final ModeSetting doubleReduceMode = new ModeSetting("Режим DR", "Simple1", "Симпле1", "Симпле2")
            .setVisible(() -> mode.is("Легит") && doubleReduce.get());
    private final SliderSetting legitReduceMult = new SliderSetting("Множитель снижения", 0.6f, 0f, 2f, 0.05f)
            .setVisible(() -> mode.is("Легит") && doubleReduce.get());

    private final BooleanSetting legitTimer = new BooleanSetting("Таймер", false)
            .setVisible(() -> mode.is("Легит"));
    private final SliderSetting legitTimerSpeed = new SliderSetting("Скорость таймера", 1.1f, 0.25f, 3f, 0.05f)
            .setVisible(() -> mode.is("Легит") && legitTimer.get());
    private final SliderSetting legitTimerTicks = new SliderSetting("Таймер тики", 3f, 1f, 10f, 1f)
            .setVisible(() -> mode.is("Легит") && legitTimer.get());

    // --- Реверс ---
    private final SliderSetting revTick = new SliderSetting("Тик реверса", 1f, 0f, 10f, 1f)
            .setVisible(() -> mode.is("Реверс"));
    private final SliderSetting revMult = new SliderSetting("Множитель реверса", 0.7f, 0f, 5f, 0.01f)
            .setVisible(() -> mode.is("Реверс"));
    private final BooleanSetting reverseStrafe = new BooleanSetting("Реверс стрейф", true)
            .setVisible(() -> mode.is("Реверс"));
    private final BooleanSetting downMR = new BooleanSetting("Снижение Y", true)
            .setVisible(() -> mode.is("Реверс"));

    // --- Буст / Горизонталь ---
    private final BooleanSetting boostMultiply = new BooleanSetting("Буст умножение", true)
            .setVisible(() -> mode.is("Буст") || mode.is("Горизонталь"));
    private final SliderSetting boostMult = new SliderSetting("Множитель буста", 1.5f, 1f, 3f, 0.1f)
            .setVisible(() -> (mode.is("Буст") || mode.is("Горизонталь")) && boostMultiply.get());
    private final SliderSetting boostMotion = new SliderSetting("Движение буста", 0.3f, 0f, 1f, 0.01f)
            .setVisible(() -> (mode.is("Буст") || mode.is("Горизонталь")) && !boostMultiply.get());
    private final SliderSetting boostTicks = new SliderSetting("Тики буста", 5f, 1f, 10f, 1f)
            .setVisible(() -> mode.is("Буст") || mode.is("Горизонталь"));

    // --- Грим ---
    private final BooleanSetting grimSwingArm = new BooleanSetting("Размах руки", true)
            .setVisible(() -> mode.is("Грим"));
    private final BooleanSetting grimStopMotion = new BooleanSetting("Стоп движения", true)
            .setVisible(() -> mode.is("Грим"));

    // --- Matrix ---
    private final SliderSetting matrixStopTicks = new SliderSetting("Тики стопа", 20f, 1f, 40f, 1f)
            .setVisible(() -> mode.is("Matrix") && matrixMode.is("Motion 1"));

    // --- Общие ---
    private final BooleanSetting cancelExplosion = new BooleanSetting("Отмена взрыва", true);

    // === Состояние ===

    private int ticks;
    private int polarTicks;
    private int ticksFromKB;
    private int jumpCounter;
    private double prevMotX, prevMotY, prevMotZ;
    private boolean modifyNextS12;
    private boolean prevMotSaved;

    private final TimerUtility velTimer = TimerUtility.create();

    // === Конструктор ===

    public Velocity() {
        addSettings(mode,
                matrixMode, grimMode,
                simpleHorizontal, simpleVertical,
                legitJump, legitJumpMode, legitJumps, legitJumpChance,
                doubleReduce, doubleReduceMode, legitReduceMult,
                legitTimer, legitTimerSpeed, legitTimerTicks,
                revTick, revMult, reverseStrafe, downMR,
                boostMultiply, boostMult, boostMotion, boostTicks,
                grimSwingArm, grimStopMotion,
                matrixStopTicks,
                cancelExplosion);
    }

    // === Утилиты направления движения ===

    /**
     * Возвращает направление движения в радианах, -1 если нет ввода.
     */
    private float getMoveDirection() {
        return MoveUtils.getDirection();
    }

    /**
     * Возвращает вектор движения (направление движения).
     */
    private double[] getMoveDirVec() {
        float dir = getMoveDirection();
        if (mc.player.movementInput.moveForward == 0 && mc.player.movementInput.moveStrafe == 0) {
            return new double[]{0, 0};
        }
        return new double[]{-Math.sin(dir), Math.cos(dir)};
    }

    /**
     * Возвращает направление движения по motion (в радианах).
     */
    private double getMotionDir() {
        double motionX = mc.player.getMotion().x;
        double motionZ = mc.player.getMotion().z;
        return Math.atan2(motionX, motionZ);
    }

    /**
     * Проверяет, хорошо ли направление движения совпадает с движением.
     */
    private boolean isGoodMotion() {
        if (mc.player.movementInput.moveForward == 0 && mc.player.movementInput.moveStrafe == 0) {
            return true;
        }
        float moveDir = getMoveDirection();
        double motionDir = getMotionDir();
        // Нормализуем разницу в радианах
        double yawDiff = MathHelper.wrapDegrees(Math.toDegrees(motionDir) - Math.toDegrees(moveDir));
        return Math.abs(yawDiff) < 45;
    }

    /**
     * Проверяет, есть ли у игрока плохие эффекты (яд, увядание).
     */
    private boolean isBadEffects() {
        if (mc.player == null) return false;
        return mc.player.isPotionActive(Effects.POISON) || mc.player.isPotionActive(Effects.WITHER)
                || mc.player.isPotionActive(Effects.INSTANT_DAMAGE);
    }

    /**
     * Легит прыжок логика.
     */
    private boolean legitJump() {
        if (legitJumpMode.is("Прыжки")) {
            jumpCounter++;
            if (jumpCounter >= legitJumps.get()) {
                mc.player.jump();
                jumpCounter = 0;
                return true;
            }
        } else {
            if (Math.random() * 100 > legitJumpChance.get()) {
                mc.player.jump();
                return true;
            }
            jumpCounter = 0;
        }
        return false;
    }

    // === Packet Handler ===

    @Subscribe
    public void onPacket(EventPacket e) {
        if (mc.player == null) return;

        if (e.isReceive()) {
            handleReceivePacket(e);
        } else if (e.isSend()) {
            handleSendPacket(e);
        }
    }

    private void handleReceivePacket(EventPacket e) {
        IPacket<?> packet = e.getPacket();

        // SExplosionPacket — отмена взрывов для всех режимов
        if (packet instanceof SExplosionPacket) {
            if (cancelExplosion.get()) {
                e.cancel();
            }
        }

        // SEntityVelocityPacket — основная обработка
        if (packet instanceof SEntityVelocityPacket s12) {
            if (s12.getEntityID() != mc.player.getEntityId()) return;

            // Отмена при плохих эффектах
            if (isBadEffects()) {
                e.cancel();
                return;
            }

            // Отмена при огне или в void
            if (mc.player.isBurning() || mc.player.getPosY() < 0) {
                e.cancel();
                return;
            }

            velTimer.reset();
            ticksFromKB = 0;

            switch (mode.get()) {
                case "Отмена" -> {
                    e.cancel();
                }
                case "Простой" -> {
                    // Модифицируем velocity с процентами (0% = полный KB, 100% = нет KB)
                    double motX = s12.getMotionX() / 8000.0;
                    double motY = s12.getMotionY() / 8000.0;
                    double motZ = s12.getMotionZ() / 8000.0;
                    double newMotX = motX * (1.0 - simpleHorizontal.get() / 100.0);
                    double newMotY = motY * (1.0 - simpleVertical.get() / 100.0);
                    double newMotZ = motZ * (1.0 - simpleHorizontal.get() / 100.0);
                    mc.player.setMotion(newMotX, newMotY, newMotZ);
                    e.cancel();
                }
                case "Легит" -> {
                    handleLegitPacket(e, s12);
                }
                case "Реверс" -> {
                    // Не отменяем — просто запускаем таймер
                }
                case "Горизонталь" -> {
                    // Сохраняем только Y, отменяем горизонталь
                    double motY = s12.getMotionY() / 8000.0;
                    mc.player.setMotion(mc.player.getMotion().x, motY, mc.player.getMotion().z);
                    e.cancel();
                }
                case "Буст" -> {
                    // Не отменяем — буст в move
                }
                case "Matrix" -> {
                    handleMatrixPacket(e, s12);
                }
                case "Грим" -> {
                    handleGrimPacket(e, s12);
                }
                case "Полярный" -> {
                    handlePolarPacket(e, s12);
                }
            }
        }
    }

    // --- Легит packet ---
    private void handleLegitPacket(EventPacket e, SEntityVelocityPacket s12) {
        if (doubleReduce.get() && doubleReduceMode.is("Simple2")) {
            double lastMotX = mc.player.getMotion().x;
            double lastMotY = mc.player.getMotion().y;
            double lastMotZ = mc.player.getMotion().z;
            mc.player.setMotion(lastMotX, lastMotY, lastMotZ);
            if (!isGoodMotion()) {
                modifyNextS12 = true;
            }
        }
        // В режиме Legit не отменяем пакет (оно легит!)
    }

    // --- Matrix packet ---
    private void handleMatrixPacket(EventPacket e, SEntityVelocityPacket s12) {
        switch (matrixMode.get()) {
            case "Motion 1" -> {
                e.cancel();
                prevMotX = mc.player.getMotion().x;
                prevMotY = mc.player.getMotion().y;
                prevMotZ = mc.player.getMotion().z;
                prevMotSaved = true;
                ticks = matrixStopTicks.getInt();
            }
            case "Motion 2" -> {
                double ya = s12.getMotionY() / 8000.0;
                double xa = s12.getMotionX() / 8000.0;
                double za = s12.getMotionZ() / 8000.0;
                if ((ya < 0.1 || ya > 0.5 || Math.abs(xa) > 1 || Math.abs(za) > 1)
                        && !(mc.player.isOnGround() && mc.player.collidedVertically)) {
                    e.cancel();
                } else {
                    mc.player.setMotion(mc.player.getMotion().x, ya, mc.player.getMotion().z);
                    e.cancel();
                }
            }
        }
    }

    // --- Грим packet ---
    private void handleGrimPacket(EventPacket e, SEntityVelocityPacket s12) {
        switch (grimMode.get()) {
            case "2.3.71" -> {
                ticks = 2;
                e.cancel();
            }
            case "2.3.72" -> {
                double speed = MoveUtils.getSpeed();
                ticks = speed < 0.01 ? 1 : 3;
                e.cancel();
                if (grimSwingArm.get()) {
                    mc.player.swingArm(Hand.MAIN_HAND);
                }
                if (speed > 0.01) {
                    // Отправляем Rotation-only пакет
                    mc.player.connection.sendPacket(new CPlayerPacket.RotationPacket(
                            mc.player.rotationYaw, mc.player.rotationPitch, mc.player.isOnGround()));
                    mc.timer.timerSpeed = 0.4984f;
                }
                // STOP_DESTROY_BLOCK (без BlockStatePredictionHandler, просто отправляем)
                mc.player.connection.sendPacket(new CPlayerDiggingPacket(
                        CPlayerDiggingPacket.Action.STOP_DESTROY_BLOCK,
                        new BlockPos(mc.player.getPosX(), mc.player.getPosY(), mc.player.getPosZ()),
                        Direction.UP));
                if (grimSwingArm.get()) {
                    mc.player.swingArm(Hand.MAIN_HAND);
                }
            }
            case "Simple" -> {
                ticks = 2;
                e.cancel();
            }
        }
    }

    // --- Полярный packet ---
    private void handlePolarPacket(EventPacket e, SEntityVelocityPacket s12) {
        Vector3d savedMot = mc.player.getMotion();
        double xa = s12.getMotionX() / 8000.0;
        double ya = s12.getMotionY() / 8000.0;
        double za = s12.getMotionZ() / 8000.0;
        // Временно применяем velocity для проверки направления
        mc.player.setMotion(xa, ya, za);

        if (!isGoodMotion() && MoveUtils.getSpeed() > 0.1) {
            if (polarTicks > 70 && !mc.player.isOnGround()) {
                polarTicks = 0;
                e.cancel();
            }
        }
        // Всегда восстанавливаем motion (сервер применит его сам, если не отменено)
        mc.player.setMotion(savedMot.x, savedMot.y, savedMot.z);
    }

    // --- Send packet handler ---
    private void handleSendPacket(EventPacket e) {
        // Грим: уменьшаем ticks при отправке move-пакета
        if (mode.is("Грим")) {
            if (e.getPacket() instanceof CPlayerPacket) {
                if (ticks > 0) {
                    ticks--;
                }
            }
        }
    }

    // === Update Handler ===

    @Subscribe
    public void onUpdate(EventUpdate e) {
        if (mc.player == null) return;

        ticksFromKB++;

        switch (mode.get()) {
            case "Легит" -> updateLegit();
            case "Грим" -> updateGrim();
            case "Matrix" -> updateMatrix();
            case "Реверс" -> updateReverse();
            case "Буст" -> updateBoost();
            case "Горизонталь" -> updateHorizontal();
            case "Полярный" -> updatePolar();
        }
    }

    // --- Легит update ---
    private void updateLegit() {
        // Легит прыжок
        boolean wasJump = false;
        if (legitJump.get() && mc.player.collidedVertically && mc.player.isOnGround()
                && mc.player.isSprinting() && !velTimer.hasTimeElapsed(60)
                && legitJump()) {
            wasJump = true;
        }

        // Double Reduce Simple1
        if (!isGoodMotion()) {
            if (!wasJump && doubleReduce.get() && doubleReduceMode.is("Simple1")
                    && !velTimer.hasTimeElapsed(100)) {
                double speed = MoveUtils.getSpeed();
                mc.player.motion.x *= legitReduceMult.get();
                mc.player.motion.z *= legitReduceMult.get();
            }

            // Таймер
            if (legitTimer.get()) {
                if (ticksFromKB <= (legitTimerTicks.get() - 1)) {
                    mc.timer.timerSpeed = legitTimerSpeed.get();
                } else if (ticksFromKB > (legitTimerTicks.get() - 1)
                        && mc.timer.timerSpeed == legitTimerSpeed.get()) {
                    mc.timer.timerSpeed = 1.0f;
                }
            }
        }

        // Сброс таймера
        if (legitTimer.get()) {
            if (ticksFromKB > (legitTimerTicks.get() - 1)
                    && mc.timer.timerSpeed == legitTimerSpeed.get()) {
                mc.timer.timerSpeed = 1.0f;
            }
        }

        // Double Reduce Simple2 (post-packet speed reduction)
        if (modifyNextS12) {
            double speed = MoveUtils.getSpeed();
            mc.player.motion.x *= 0.6;
            mc.player.motion.z *= 0.6;
            modifyNextS12 = false;
        }
    }

    // --- Грим update ---
    private void updateGrim() {
        // Пока ticks > 0 — сохраняем motion и останавливаем
        if (ticks > 0) {
            if (!prevMotSaved) {
                prevMotX = mc.player.getMotion().x;
                prevMotY = mc.player.getMotion().y;
                prevMotZ = mc.player.getMotion().z;
                prevMotSaved = true;
            }
            // Останавливаем движение если включено
            if (grimStopMotion.get()) {
                mc.player.setMotion(0, 0, 0);
            }
        } else if (prevMotSaved) {
            // ticks = 0 — восстанавливаем motion
            if (grimMode.is("2.3.71") || grimMode.is("Simple")) {
                mc.player.setMotion(prevMotX, prevMotY, prevMotZ);
            } else if (grimMode.is("2.3.72")) {
                mc.player.setMotion(0, 0, 0);
                mc.timer.timerSpeed = 1.0f;
            }
            prevMotSaved = false;
        }

        // Грим 2.3.72 — отправка STOP_DESTROY_BLOCK каждый тик пока ticks > 0
        if (grimMode.is("2.3.72") && ticks > 0) {
            if (grimSwingArm.get()) {
                mc.player.swingArm(Hand.MAIN_HAND);
            }
            mc.player.connection.sendPacket(new CPlayerDiggingPacket(
                    CPlayerDiggingPacket.Action.STOP_DESTROY_BLOCK,
                    new BlockPos(mc.player.getPosX(), mc.player.getPosY(), mc.player.getPosZ()),
                    Direction.UP));
            if (grimSwingArm.get()) {
                mc.player.swingArm(Hand.MAIN_HAND);
            }
        }

        // Грим 2.3.71 — отправка UseItemOn пакетов пока ticks > 0
        if (grimMode.is("2.3.71") && ticks > 0) {
            Hand hand = Hand.MAIN_HAND;
            if (mc.player.getHeldItem(Hand.MAIN_HAND).isEmpty() && !mc.player.getHeldItem(Hand.OFF_HAND).isEmpty()) {
                hand = Hand.OFF_HAND;
            }

            Vector3d look = mc.player.getLook(1.0f);
            BlockRayTraceResult blockHitResult = new BlockRayTraceResult(
                    look, mc.player.getHorizontalFacing(),
                    new BlockPos(mc.player.getPosX(), mc.player.getPosY(), mc.player.getPosZ()),
                    true);
            mc.player.connection.sendPacket(new CPlayerTryUseItemOnBlockPacket(hand, blockHitResult));

            // Второй пакет с pitch 90 (взгляд вниз)
            BlockRayTraceResult blockHitResult2 = new BlockRayTraceResult(
                    new Vector3d(0, -1, 0), Direction.DOWN,
                    new BlockPos(mc.player.getPosX(), mc.player.getPosY(), mc.player.getPosZ()),
                    true);
            mc.player.connection.sendPacket(new CPlayerTryUseItemOnBlockPacket(hand, blockHitResult2));
        }
    }

    // --- Matrix update ---
    private void updateMatrix() {
        if (matrixMode.is("Motion 1")) {
            // На 20-м тике — обработка Y подавления (в оригинале закомментировано, оставляем пустым)

            // При airpackets > 1 и высоком тике — добавляем небольшое движение
            // (упрощённая версия без PacketHelper.airpackets — используем !onGround)
            if (!mc.player.isOnGround()) {
                if (ticks > 18 && MoveUtils.getSpeed() > 0.25) {
                    double[] dir = getMoveDirVec();
                    mc.player.setMotion(
                            mc.player.getMotion().x + dir[0] * 0.009,
                            mc.player.getMotion().y,
                            mc.player.getMotion().z + dir[1] * 0.009);
                }
            }

            if (ticks > 0) {
                ticks--;
            }
        }
    }

    // --- Реверс update ---
    private void updateReverse() {
        if (ticksFromKB == revTick.getInt()) {
            double mult = revMult.get();
            // Сначала умножаем горизонтальную скорость
            mc.player.motion.x *= mult;
            mc.player.motion.z *= mult;

            if (reverseStrafe.get()) {
                // Smart strafe: пересчитывает движение из ввода
                double speed = MoveUtils.getSpeed();
                MoveUtils.setMotion(speed);
            } else {
                // Полный реверс
                mc.player.motion.x *= -1;
                mc.player.motion.z *= -1;
            }

            // Снижение Y если падаем вверх
            if (mc.player.getMotion().y > 0.1 && mc.player.fallDistance > 0 && downMR.get()) {
                mc.player.setMotion(mc.player.getMotion().x, -0.001, mc.player.getMotion().z);
            }
        }
    }

    // --- Буст update ---
    private void updateBoost() {
        if (ticksFromKB < boostTicks.getInt()) {
            applyBoost();
        }
    }

    // --- Горизонталь update ---
    private void updateHorizontal() {
        if (ticksFromKB < boostTicks.getInt()) {
            applyBoost();
        }
    }

    private void applyBoost() {
        double[] dir = getMoveDirVec();
        if (boostMultiply.get()) {
            double speed = MoveUtils.getSpeed();
            mc.player.motion.x *= boostMult.get();
            mc.player.motion.z *= boostMult.get();
        } else {
            double mult = boostMotion.get();
            mc.player.setMotion(
                    mc.player.getMotion().x + dir[0] * mult,
                    mc.player.getMotion().y,
                    mc.player.getMotion().z + dir[1] * mult);
        }
    }

    // --- Полярный update ---
    private void updatePolar() {
        // Прыжок при спринте на земле с шансом 50%
        if (mc.player.collidedVertically && mc.player.isOnGround()
                && mc.player.isSprinting() && !velTimer.hasTimeElapsed(60)
                && Math.random() > 0.5) {
            mc.player.jump();
        }
        polarTicks++;
    }

    // === OnEnable / OnDisable ===

    @Override
    public boolean onEnable() {
        super.onEnable();
        ticks = 0;
        polarTicks = 0;
        ticksFromKB = 0;
        jumpCounter = 0;
        prevMotX = 0;
        prevMotY = 0;
        prevMotZ = 0;
        modifyNextS12 = false;
        prevMotSaved = false;
        mc.timer.timerSpeed = 1.0f;
        return false;
    }

    @Override
    public boolean onDisable() {
        mc.timer.timerSpeed = 1.0f;
        return super.onDisable();
    }
}
