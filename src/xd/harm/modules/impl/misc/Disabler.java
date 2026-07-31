package xd.harm.modules.impl.misc;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.client.CConfirmTransactionPacket;
import net.minecraft.network.play.client.CEntityActionPacket;
import net.minecraft.network.play.client.CKeepAlivePacket;
import net.minecraft.network.play.client.CPlayerPacket;
import net.minecraft.network.play.client.CUseEntityPacket;
import net.minecraft.network.play.server.SEntityVelocityPacket;
import net.minecraft.network.play.server.SJoinGamePacket;
import net.minecraft.network.play.server.SPlayerPositionLookPacket;
import xd.harm.events.network.EventPacket;
import xd.harm.events.render.EventDisplay;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.ModeSetting;
import xd.harm.modules.settings.impl.SliderSetting;
import xd.harm.utils.client.TimerUtility;
import xd.harm.utils.player.MoveUtils;
import xd.harm.utils.render.color.ColorUtils;
import xd.harm.utils.render.font.Fonts;

import java.util.ArrayDeque;

@ModuleRegister(name = "Disabler", category = Category.Misc, desc = "Ослабляет античиты (режимы Matrix&Ncp / MatrixTimer)")
public class Disabler extends Module {

    // ===== Таймеры =====
    public TimerUtility lastC00timer = TimerUtility.create();
    public TimerUtility lastPackettimer = TimerUtility.create();
    public TimerUtility lastFlagtimer = TimerUtility.create();
    public TimerUtility timerr = TimerUtility.create();

    // ===== Состояние =====
    ArrayDeque<LatePacket> C0Fs = new ArrayDeque<>();
    ArrayDeque<CKeepAlivePacket> C00s = new ArrayDeque<>();
    double posxc03 = 0, posyc03 = 0, poszc03 = 0;
    float yawc03 = 0, pitchc03 = 0;
    int flagskip = 0;
    int flagscount = 0;
    int packets = 0;

    // ===== Настройки =====
    public ModeSetting mode = new ModeSetting("Disabler Mode", "Matrix&Ncp", "Matrix&Ncp", "MatrixTimer").setVisible(() -> true);
    public BooleanSetting MatrixAllDir = new BooleanSetting("MatrixAllDir", true).setVisible(() -> mode.is("Matrix&Ncp") || mode.is("MatrixTimer"));
    public BooleanSetting onlyC04 = new BooleanSetting("Только C04", false).setVisible(() -> mode.is("MatrixTimer"));
    public BooleanSetting InvBypass = new BooleanSetting("InvBypass", false).setVisible(() -> mode.is("Matrix&Ncp"));
    public SliderSetting MatrixTPackets = new SliderSetting("Packet for 1 tick", 20f, 1f, 50f, 1f).setVisible(() -> mode.is("MatrixTimer"));

    private boolean noEvent = false;

    public Disabler() {
        addSettings(mode, MatrixAllDir, onlyC04, InvBypass, MatrixTPackets);
    }

    public boolean onEnable() {
        C0Fs.clear();
        C00s.clear();
        posxc03 = 0;
        posyc03 = 0;
        poszc03 = 0;
        yawc03 = 0;
        pitchc03 = 0;
        flagskip = 0;
        flagscount = 0;
        packets = 0;
        timerr.reset();
        lastFlagtimer.reset();
        lastPackettimer.reset();
        lastC00timer.reset();
        return super.onEnable();
    }

    public boolean onDisable() {
        flushAllC0Fs();
        while (C00s.size() > 0) {
            CKeepAlivePacket c = C00s.pollFirst();
            if (c != null) sendNoEvent(c);
        }
        C00s.clear();
        return super.onDisable();
    }

    @Subscribe
    public void onPacket(EventPacket e) {
        IPacket<?> packet = e.getPacket();

        // ===================== RECEIVE =====================
        if (e.isReceive()) {
            if (packet instanceof SJoinGamePacket) {
                C0Fs.clear();
                C00s.clear();
                flagskip = 0;
                flagscount = 0;
                packets = 0;
                timerr.reset();
                lastFlagtimer.reset();
                lastPackettimer.reset();
                lastC00timer.reset();
                posxc03 = 0;
                posyc03 = 0;
                poszc03 = 0;
                yawc03 = 0;
                pitchc03 = 0;
            } else if (packet instanceof SEntityVelocityPacket) {
                SEntityVelocityPacket s12 = (SEntityVelocityPacket) packet;
                if (mc.player != null && s12.getEntityID() == mc.player.getEntityId()) {
                    flushAllC0Fs();
                }
            } else if (packet instanceof SPlayerPositionLookPacket) {
                SPlayerPositionLookPacket s08 = (SPlayerPositionLookPacket) packet;
                // Близкая телепорт-коррекция (<20 блоков) = флаг от античита
                if (mc.player != null && mc.player.getDistanceSq(s08.getX(), s08.getY(), s08.getZ()) < 400.0) {
                    flagscount++;
                    if (flagscount >= 4) {
                        flagscount = 0;
                        flagskip = 5; // отменяем следующие коррекции, чтобы не было резкого возврата
                    }
                }
                if (flagskip > 0) {
                    flagskip--;
                    e.cancel();
                }
                lastFlagtimer.reset();
            }
            return;
        }

        // ===================== SEND =====================
        if (noEvent) return;
        if (e.isCancel()) return;
        if (!mode.is("Matrix&Ncp") && !mode.is("MatrixTimer")) return;

        String m = mode.get();

        // Только C04 (только для MatrixTimer): блокируем всё, кроме C00 и C0F
        if (m.equals("MatrixTimer") && onlyC04.get()) {
            if (packet instanceof CKeepAlivePacket) {
                // обрабатывается ниже
            } else if (packet instanceof CConfirmTransactionPacket) {
                // обрабатывается ниже
            } else {
                e.cancel();
                return;
            }
        }

        if (packet instanceof CConfirmTransactionPacket) {
            long delay = m.equals("Matrix&Ncp") ? 14000L : 24000L;
            C0Fs.add(new LatePacket(packet, System.currentTimeMillis() + delay));
            e.cancel();
        } else if (packet instanceof CUseEntityPacket) {
            CUseEntityPacket c02 = (CUseEntityPacket) packet;
            if (c02.getAction() == CUseEntityPacket.Action.ATTACK) {
                flushAllC0Fs();
            }
        } else if (packet instanceof CPlayerPacket) {
            handleCPlayer((CPlayerPacket) packet, e);
        } else if (packet instanceof CKeepAlivePacket) {
            CKeepAlivePacket c00 = (CKeepAlivePacket) packet;
            if (C00s.size() == 0) lastC00timer.reset();
            C00s.add(c00);
            e.cancel();
        } else if (packet instanceof CEntityActionPacket) {
            CEntityActionPacket c0b = (CEntityActionPacket) packet;
            if (MatrixAllDir.get() &&
                    (c0b.getAction() == CEntityActionPacket.Action.START_SPRINTING ||
                     c0b.getAction() == CEntityActionPacket.Action.STOP_SPRINTING)) {
                e.cancel();
            }
        }
    }

    private void handleCPlayer(CPlayerPacket cp, EventPacket e) {
        double dev = 1.0e-6;
        if (cp instanceof CPlayerPacket.PositionPacket) {
            CPlayerPacket.PositionPacket c04 = (CPlayerPacket.PositionPacket) cp;
            if (Math.abs(c04.x - posxc03) < dev && Math.abs(c04.y - posyc03) < dev && Math.abs(c04.z - poszc03) < dev) {
                e.cancel();
            }
            posxc03 = c04.x;
            posyc03 = c04.y;
            poszc03 = c04.z;
        } else if (cp instanceof CPlayerPacket.RotationPacket) {
            CPlayerPacket.RotationPacket c05 = (CPlayerPacket.RotationPacket) cp;
            if (yawdiff(yawc03, c05.yaw) < dev && pitchdiff(pitchc03, c05.pitch) < dev) {
                e.cancel();
            }
            yawc03 = c05.yaw;
            pitchc03 = c05.pitch;
        } else if (cp instanceof CPlayerPacket.PositionRotationPacket) {
            CPlayerPacket.PositionRotationPacket c06 = (CPlayerPacket.PositionRotationPacket) cp;
            if (yawdiff(yawc03, c06.yaw) < dev && pitchdiff(pitchc03, c06.pitch) < dev) {
                e.cancel();
                sendNoEvent(new CPlayerPacket.PositionPacket(c06.x, c06.y, c06.z, c06.onGround));
            }
            if (Math.abs(c06.x - posxc03) < dev && Math.abs(c06.y - posyc03) < dev && Math.abs(c06.z - poszc03) < dev) {
                e.cancel();
            }
            posxc03 = c06.x;
            posyc03 = c06.y;
            poszc03 = c06.z;
            yawc03 = c06.yaw;
            pitchc03 = c06.pitch;
        }
        // plain CPlayerPacket (только onGround) — отправляем как есть
    }

    @Subscribe
    public void onUpdate(EventUpdate e) {
        if (mc.player == null) return;

        // Слив просроченных C0F (Matrix&Ncp держит 14с, MatrixTimer 24с)
        boolean find = false;
        while (C0Fs.size() > 0 && !find) {
            if (C0Fs.peekFirst().requiredMs <= System.currentTimeMillis()) {
                LatePacket lp = C0Fs.pollFirst();
                sendNoEvent(lp.packet);
            } else {
                find = true;
            }
        }

        // Слив C00 (keepalive) каждые 400мс — иначе сервер кикнет
        if (lastC00timer.hasTimeElapsed(400L, true)) {
            while (C00s.size() > 0) {
                CKeepAlivePacket c = C00s.pollFirst();
                if (c != null) sendNoEvent(c);
            }
        }

        // Idle-keepalive если стоим >4с
        if (lastPackettimer.hasTimeElapsed(4000L, true) && MoveUtils.getSpeed() < 0.001) {
            sendNoEvent(new CPlayerPacket(false));
        }

        // Ограничение скорости для MatrixTimer
        if (mode.is("MatrixTimer") && mc.timer.timerSpeed > 1.0F && C0Fs.size() < 10) {
            limit2speed(0.199);
        }
    }

    @Subscribe
    public void onDisplay(EventDisplay e) {
        if (e.getType() != EventDisplay.Type.HIGH) return;
        if (mc.player == null) return;
        MatrixStack ms = e.getMatrixStack();
        int h = mc.getMainWindow().getScaledHeight();
        String text = mode.get() + " | C0F:" + C0Fs.size() + " | C00:" + C00s.size();
        Fonts.sfbold.drawText(ms, text, 22.5f, h * 0.95f, ColorUtils.rgb(139, 0, 255), 21f);
    }

    // ===================== Утилиты =====================

    public void flushAllC0Fs() {
        while (C0Fs.size() > 0) {
            LatePacket lp = C0Fs.pollFirst();
            if (lp != null && lp.packet != null) sendNoEvent(lp.packet);
        }
        C0Fs.clear();
    }

    private void limit2speed(double speedd) {
        while (MoveUtils.getSpeed() > speedd) {
            mc.player.setMotion(mc.player.motion.x * 0.999, mc.player.motion.y, mc.player.motion.z * 0.999);
        }
    }

    private void sendNoEvent(IPacket<?> packet) {
        if (mc.player == null || mc.player.connection == null || packet == null) return;
        noEvent = true;
        mc.player.connection.sendPacket(packet);
        noEvent = false;
    }

    private static float pitchdiff(float pitch1, float pitch2) {
        return Math.abs(pitch1 - pitch2);
    }

    private static float yawdiff(float yaw1, float yaw2) {
        float yaw360 = (float) (Math.abs(yaw1 - yaw2) % 360.0D);
        if (yaw360 > 180.0F) yaw360 = 360.0F - yaw360;
        return yaw360;
    }

    static class LatePacket {
        IPacket<?> packet;
        long requiredMs;

        LatePacket(IPacket<?> packet2, long requiredMs2) {
            this.packet = packet2;
            this.requiredMs = requiredMs2;
        }
    }
}
