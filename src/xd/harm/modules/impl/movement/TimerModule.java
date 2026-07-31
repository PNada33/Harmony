package xd.harm.modules.impl.movement;

import com.google.common.eventbus.Subscribe;
import net.minecraft.network.play.client.CPlayerPacket;
import net.minecraft.network.play.server.SPlayerPositionLookPacket;
import xd.harm.events.movement.EventMotion;
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

@ModuleRegister(name = "Timer", category = Category.Movement, desc = "Изменяет скорость игры / обход NCP")
public class TimerModule extends Module {

    // ===== Настройки базовых режимов =====
    private final ModeSetting mode = new ModeSetting("Режим", "Постоянный",
            "Постоянный", "Прерывистый", "Движение", "Прыжок", "NCPBypass");
    private final SliderSetting speed = new SliderSetting("Скорость", 2f, 0.1f, 12f, 0.1f);
    private final SliderSetting slowSpeed = new SliderSetting("Медленная скорость", 0.5f, 0.1f, 1f, 0.05f)
            .setVisible(() -> mode.is("Прерывистый"));
    private final SliderSetting fastTime = new SliderSetting("Время ускорения", 200f, 50f, 1000f, 50f)
            .setVisible(() -> mode.is("Прерывистый"));
    private final SliderSetting slowTime = new SliderSetting("Время замедления", 200f, 50f, 1000f, 50f)
            .setVisible(() -> mode.is("Прерывистый"));
    private final BooleanSetting onlyOnGround = new BooleanSetting("Только на земле", false)
            .setVisible(() -> mode.is("Прыжок"));
    private final BooleanSetting stopOnHurt = new BooleanSetting("Стоп при уроне", false);
    private final BooleanSetting stopInWater = new BooleanSetting("Стоп в воде", false);

    // ===== Настройки NCPBypass =====
    private final BooleanSetting NCPBypass = new BooleanSetting("NCPBypass", false).setVisible(() -> mode.is("NCPBypass"));
    private final ModeSetting NCPBypassVer = new ModeSetting("Bypass Version", "1.9+", "1.9+", "1.8.8-")
            .setVisible(() -> mode.is("NCPBypass") && NCPBypass.get());

    // ===== Состояние =====
    private final TimerUtility pulseTimer = TimerUtility.create();
    private boolean pulseFast = true;
    private int index1 = 0;
    private int flagscount = 0;
    private float lastYaw = 0, lastPitch = 0;

    public TimerModule() {
        addSettings(mode, speed, slowSpeed, fastTime, slowTime, onlyOnGround, stopOnHurt, stopInWater, NCPBypass, NCPBypassVer);
    }

    @Override
    public boolean onEnable() {
        super.onEnable();
        pulseTimer.reset();
        pulseFast = true;
        mc.timer.timerSpeed = 1.0f;
        index1 = 0;
        flagscount = 0;
        lastYaw = mc.player != null ? mc.player.rotationYaw : 0;
        lastPitch = mc.player != null ? mc.player.rotationPitch : 0;
        return false;
    }

    @Override
    public boolean onDisable() {
        mc.timer.timerSpeed = 1.0f;
        return super.onDisable();
    }

    @Subscribe
    public void onUpdate(EventUpdate e) {
        if (mc.player == null) return;

        if (shouldStop()) {
            mc.timer.timerSpeed = 1.0f;
            return;
        }

        switch (mode.get()) {
            case "Постоянный":
                mc.timer.timerSpeed = speed.get();
                break;
            case "Прерывистый":
                handlePulse();
                break;
            case "Движение":
                if (MoveUtils.isMoving()) mc.timer.timerSpeed = speed.get();
                else mc.timer.timerSpeed = 1.0f;
                break;
            case "Прыжок":
                boolean condition = onlyOnGround.get() ? mc.player.isOnGround() : !mc.player.isOnGround();
                mc.timer.timerSpeed = condition ? speed.get() : 1.0f;
                break;
            case "NCPBypass":
                handleNCPBypass();
                break;
        }
    }

    private void handlePulse() {
        long fast = (long) (float) fastTime.get();
        long slow = (long) (float) slowTime.get();
        if (pulseFast) {
            mc.timer.timerSpeed = speed.get();
            if (pulseTimer.isReached(fast)) {
                pulseFast = false;
                pulseTimer.reset();
            }
        } else {
            mc.timer.timerSpeed = slowSpeed.get();
            if (pulseTimer.isReached(slow)) {
                pulseFast = true;
                pulseTimer.reset();
            }
        }
    }

    // Порт Quantum TimerModule NCPBypass
    private void handleNCPBypass() {
        String ver = NCPBypassVer.get();
        if (!NCPBypass.get()) {
            mc.timer.timerSpeed = speed.get();
            return;
        }

        if (ver.equals("1.8.8-")) {
            int tick = mc.player.ticksExisted % 30;
            if (tick > 0 && tick < 16) mc.timer.timerSpeed = 3.2F;
            else if (tick >= 16 && tick < 26) mc.timer.timerSpeed = 2.4F;
            else mc.timer.timerSpeed = 2.8F;
        } else { // 1.9+
            if (index1 < 6) {
                multiplyMotion(0.99);
                mc.timer.timerSpeed = 1.5F;
            } else if (index1 > 6 && index1 < 11) {
                multiplyMotion(0.999);
                mc.timer.timerSpeed = 1.4F;
            } else if (index1 > 11) {
                multiplyMotion(0.9995);
                mc.timer.timerSpeed = 1.2F;
                index1 = 0;
            }
            index1++;
        }
    }

    private void multiplyMotion(double f) {
        mc.player.setMotion(mc.player.motion.x * f, mc.player.motion.y, mc.player.motion.z * f);
    }

    @Subscribe
    public void onMotion(EventMotion e) {
        if (mode.is("NCPBypass") && NCPBypass.get() && NCPBypassVer.get().equals("1.9+")) {
            e.setYaw(lastYaw);
            e.setPitch(lastPitch);
        }
    }

    @Subscribe
    public void onPacket(EventPacket e) {
        if (e.isSend() && mode.is("NCPBypass") && NCPBypass.get()) {
            String ver = NCPBypassVer.get();

            if (ver.equals("1.9+")) {
                // Отменяем все CPlayer, кроме Position-пакетов (NCP-безопасно)
                if (e.getPacket() instanceof CPlayerPacket && !(e.getPacket() instanceof CPlayerPacket.PositionPacket)) {
                    e.cancel();
                }
            }

            // Трекаем последние yaw/pitch из отправленных CPlayer
            if (e.getPacket() instanceof CPlayerPacket) {
                CPlayerPacket cp = (CPlayerPacket) e.getPacket();
                if (cp.rotating) {
                    lastYaw = cp.yaw;
                    lastPitch = cp.pitch;
                }
            }
        }

        // Сброс flagscount при телепорт-коррекции от античита
        if (e.isReceive() && e.getPacket() instanceof SPlayerPositionLookPacket) {
            SPlayerPositionLookPacket s08 = (SPlayerPositionLookPacket) e.getPacket();
            if (mc.player != null && mc.player.getDistanceSq(s08.getX(), s08.getY(), s08.getZ()) < 400.0 && flagscount > 0) {
                flagscount--;
            }
        }
    }

    private boolean shouldStop() {
        if (stopOnHurt.get() && mc.player.hurtTime > 0) return true;
        if (stopInWater.get() && (mc.player.isInWater() || mc.player.isInLava())) return true;
        return false;
    }
}
