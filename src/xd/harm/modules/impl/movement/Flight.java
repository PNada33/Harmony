package xd.harm.modules.impl.movement;

import com.google.common.eventbus.Subscribe;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.ModeSetting;
import xd.harm.modules.settings.impl.SliderSetting;
import xd.harm.utils.client.TimerUtility;
import xd.harm.events.network.EventPacket;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.client.CConfirmTeleportPacket;
import net.minecraft.network.play.client.CPlayerPacket;
import net.minecraft.network.play.server.SEntityVelocityPacket;
import net.minecraft.network.play.server.SPlayerPositionLookPacket;

@ModuleRegister(name = "Flight", category = Category.Movement, desc = "Flight module with Vanilla and New modes")
public class Flight extends Module {

    // === Настройки ===

    private final ModeSetting mode = new ModeSetting("Режим", "Vanilla", "Vanilla", "New");
    private final SliderSetting speed = new SliderSetting("Скорость", 2f, 0.1f, 10f, 0.1f);

    // === Состояние для режима New (flag-flight) ===

    private int index1 = 0;
    private int index3 = -1;
    private int index5 = 0;
    private int index8 = 0;
    private int indexd1 = 0;
    private double indexd2 = 0.0D;
    private double indexd4 = 0.0D;
    private double indexd5 = 0.0D;
    private double indexd6 = 0.0D;
    private double indexd7 = 0.0D;
    private double indexd8 = 0.0D;
    private boolean startfly = false;
    private final TimerUtility timerr = TimerUtility.create();
    private final TimerUtility timerr2 = TimerUtility.create();

    public Flight() {
        addSettings(mode, speed);
    }

    @Override
    public boolean onEnable() {
        super.onEnable();
        resetState();
        return false;
    }

    @Override
    public boolean onDisable() {
        super.onDisable();
        if (mc.player != null) {
            mc.player.setMotion(0, 0, 0);
            mc.player.onGround = true;
        }
        mc.timer.timerSpeed = 1.0F;
        resetState();
        return false;
    }

    private void resetState() {
        index1 = 0;
        index3 = -1;
        index5 = 0;
        index8 = 0;
        indexd1 = 0;
        indexd2 = 0.0D;
        indexd4 = 0.0D;
        indexd5 = 0.0D;
        indexd6 = 0.0D;
        indexd7 = 0.0D;
        indexd8 = 0.0D;
        startfly = false;
        timerr.reset();
        timerr2.reset();
    }

    @Subscribe
    public void onUpdate(EventUpdate e) {
        if (mc.player == null) return;

        if (mode.is("Vanilla")) {
            handleVanillaFlight();
        } else if (mode.is("New")) {
            handleNewFlight();
        }
    }

    @Subscribe
    public void onPacket(EventPacket eventPacket) {
        if (mc.player == null || !mode.is("New")) return;

        if (eventPacket.isReceive()) {
            IPacket<?> packet = eventPacket.getPacket();
            if (packet instanceof SEntityVelocityPacket vel) {
                if (vel.getEntityID() == mc.player.getEntityId()) {
                    eventPacket.cancel();
                }
            } else if (packet instanceof SPlayerPositionLookPacket p) {
                handleTeleport(p, eventPacket);
            }
        } else if (eventPacket.isSend()) {
            if (eventPacket.getPacket() instanceof CPlayerPacket.RotationPacket) {
                eventPacket.cancel();
            }
        }
    }

    // === Режим Vanilla ===

    private void handleVanillaFlight() {
        double motionX = 0;
        double motionY = 0;
        double motionZ = 0;

        if (mc.gameSettings.keyBindJump.isKeyDown()) {
            motionY = speed.get();
        } else if (mc.gameSettings.keyBindSneak.isKeyDown()) {
            motionY = -speed.get();
        } else {
            motionY = 0;
        }

        if (mc.player.moveForward != 0 || mc.player.moveStrafing != 0) {
            float yawRad = (float) Math.toRadians(mc.player.rotationYaw);
            float moveForward = mc.player.moveForward;
            float moveStrafing = mc.player.moveStrafing;

            motionX = -Math.sin(yawRad) * moveForward * speed.get() + Math.cos(yawRad) * moveStrafing * speed.get();
            motionZ = Math.cos(yawRad) * moveForward * speed.get() + Math.sin(yawRad) * moveStrafing * speed.get();
        }

        mc.player.setMotion(motionX, motionY, motionZ);
    }

    // === Режим New (flag-flight из flightnew.txt) ===

    private void handleNewFlight() {
        if (indexd8 == 0.0D) {
            mc.player.moveForward = 0.0F;
            mc.player.moveStrafing = 0.0F;
            strafe(0.0D);

            if (index1 == 0) {
                if (index3 == 0 && timerr2.hasTimeElapsed(140L, false)) {
                    indexd7++;
                    index8 = 0;
                    sendPacketNoEvent(new CPlayerPacket.PositionPacket(
                            mc.player.getPosX(),
                            mc.player.getPosY() + 11.0D + Math.random() * 10.0D,
                            mc.player.getPosZ(), false));
                    index5 = 0;
                }
                if (index3 == 1 && index8 >= 5) {
                    indexd7++;
                    index8 = 0;
                    sendPacketNoEvent(new CPlayerPacket.PositionPacket(
                            mc.player.getPosX(),
                            mc.player.getPosY() + 11.0D + Math.random() * 10.0D,
                            mc.player.getPosZ(), false));
                    index5 = 0;
                }

                int timeee = 7000;
                if (timerr.hasTimeElapsed(timeee, false) && indexd5 == 0.0D) {
                    print("flyhor");
                    indexd5++;
                }

                if (index3 == 2) {
                    if (timerr.hasTimeElapsed(timeee, false)) {
                        timerr.reset();
                        strafe(1.996D);
                        indexd5 = 0.0D;
                        indexd7++;
                        index8 = 0;
                        indexd4 = 0.0D;
                        startfly = true;
                        index5 = 0;
                    } else if (!startfly) {
                        mc.player.motion.y = 0.0D;
                        mc.player.onGround = false;
                    }
                } else if (!startfly) {
                    mc.player.motion.y = 0.0D;
                    mc.player.onGround = false;
                }

                indexd4++;
                index8++;
            }

            if (mc.gameSettings.keyBindJump.isKeyDown()) index3 = 0;
            if (mc.gameSettings.keyBindSneak.isKeyDown()) index3 = 1;
            if (mc.gameSettings.keyBindForward.isKeyDown()) index3 = 2;
            return;
        }

        // Фаза разгона (indexd8 != 0)
        if (indexd6 == 0.0D) {
            mc.player.motion.x *= 1.003D;
            mc.player.motion.z *= 1.003D;
        }
        if (indexd6 > 6.0D) {
            mc.timer.timerSpeed = 1.0F;
            strafe(0.0D);
            indexd8 = 0.0D;
            startfly = false;
            mc.player.moveForward = 0.0F;
            index3 = -1;
        } else if (indexd6 > 0.0D) {
            mc.player.moveForward = 1.0F;
            mc.player.motion.y += 0.0032D;
        }
        indexd6++;
    }

    private void handleTeleport(SPlayerPositionLookPacket p, EventPacket eventPacket) {
        if (index5 >= 1) return;

        double x = p.getX() - mc.player.getPosX();
        double y = p.getY() - mc.player.getPosY();
        double z = p.getZ() - mc.player.getPosZ();
        double diff = Math.sqrt(x * x + y * y + z * z);

        if (index3 == 0) {
            mc.player.setPosition(p.getX(), p.getY(), p.getZ());
            sendPacketNoEvent(new CConfirmTeleportPacket(p.getTeleportId()));
            timerr2.reset();
            if (diff > 0.55D) {
                mc.player.motion.y = 0.01D;
                timerr2.setLastMS(-100L);
            } else {
                mc.player.motion.y = 0.0624D;
            }
            eventPacket.cancel();
            mc.player.onGround = false;
            index5++;
        } else if (index3 == 1) {
            mc.player.setPosition(p.getX(), p.getY(), p.getZ());
            sendPacketNoEvent(new CConfirmTeleportPacket(p.getTeleportId()));
            mc.player.motion.y = -0.06D;
            eventPacket.cancel();
            mc.player.onGround = false;
            index5++;
        } else if (index3 == 2) {
            mc.player.setPosition(p.getX(), p.getY(), p.getZ());
            sendPacketNoEvent(new CConfirmTeleportPacket(p.getTeleportId()));
            if (indexd2 < 1.0D) {
                strafe(1.992D);
                indexd2++;
            } else {
                strafe(1.992D);
            }
            mc.timer.timerSpeed = 1.0F;
            startfly = true;
            eventPacket.cancel();
            indexd4 = 0.0D;
            index5++;
            indexd6 = 0.0D;
            indexd8 = 1.0D;
        } else {
            startfly = false;
        }

        indexd1 = index3;
        index8 = 0;
        index3 = -1;
    }

    // === Утилиты ===

    private void strafe(double speed) {
        double yaw = Math.toRadians(mc.player.rotationYaw);
        mc.player.motion.x = -Math.sin(yaw) * speed;
        mc.player.motion.z = Math.cos(yaw) * speed;
    }

    private void sendPacketNoEvent(IPacket<?> packet) {
        if (mc.player == null || mc.player.connection == null) return;
        mc.player.connection.sendPacket(packet);
    }
}
