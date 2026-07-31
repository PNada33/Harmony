package xd.harm.utils.player;

import xd.harm.Harmony;
import xd.harm.events.input.EventInput;
import xd.harm.events.movement.MovingEvent;
import xd.harm.modules.impl.combat.HitAura;
import xd.harm.utils.client.IMinecraft;
import lombok.experimental.UtilityClass;
import net.minecraft.block.AirBlock;
import net.minecraft.block.FlowingFluidBlock;
import net.minecraft.potion.Effects;
import net.minecraft.util.MovementInput;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import java.util.Objects;

@UtilityClass
public class MoveUtils implements IMinecraft {

    public static boolean isMoving() {
        return mc.player.movementInput.moveForward != 0f || mc.player.movementInput.moveStrafe != 0f;
    }

    public static boolean reason(boolean water) {
        boolean critWater = water && mc.world.getBlockState(new BlockPos(mc.player.getPosX(), mc.player.getPosY(), mc.player.getPosZ())).getBlock()
                instanceof FlowingFluidBlock && mc.world.getBlockState(new BlockPos(mc.player.getPosX(), mc.player.getPosY() + 1,
                mc.player.getPosZ())).getBlock() instanceof AirBlock;
        return mc.player.isPotionActive(Effects.BLINDNESS) || mc.player.isOnLadder()
                || mc.player.isInWater() && !critWater || mc.player.abilities.isFlying;
    }

    public static void fixMovement(EventInput event, float yaw) {
        float forward = event.getForward();
        float strafe = event.getStrafe();
        double angle = MathHelper.wrapDegrees(Math.toDegrees(direction(mc.player.isElytraFlying() ? yaw : mc.player.rotationYaw, (double)forward, (double)strafe)));
        if (forward != 0.0F || strafe != 0.0F) {
            float closestForward = 0.0F;
            float closestStrafe = 0.0F;
            float closestDifference = Float.MAX_VALUE;
            for(float predictedForward = -1.0F; predictedForward <= 1.0F; ++predictedForward) {
                for(float predictedStrafe = -1.0F; predictedStrafe <= 1.0F; ++predictedStrafe) {
                    if (predictedStrafe != 0.0F || predictedForward != 0.0F) {
                        double predictedAngle = MathHelper.wrapDegrees(Math.toDegrees(direction(yaw, (double)predictedForward, (double)predictedStrafe)));
                        double difference = Math.abs(angle - predictedAngle);
                        if (difference < (double)closestDifference) {
                            closestDifference = (float)difference;
                            closestForward = predictedForward;
                            closestStrafe = predictedStrafe;
                        }
                    }
                }
            }
            event.setForward(closestForward);
            event.setStrafe(closestStrafe);
        }
    }

    public static void fixMovementNoBack(EventInput event, float yaw) {
        float forward = event.getForward();
        float strafe = event.getStrafe();
        double angle = MathHelper.wrapDegrees(Math.toDegrees(direction(mc.player.isElytraFlying() ? yaw : mc.player.rotationYaw, (double) forward, (double) strafe)));
        if (forward == 0.0F && strafe == 0.0F) {
            return;
        }

        float closestForward = forward;
        float closestStrafe = strafe;
        float closestDifference = Float.MAX_VALUE;

        for (float predictedForward = -1.0F; predictedForward <= 1.0F; ++predictedForward) {
            if (forward > 0.0F && predictedForward < 0.0F) {
                continue;
            }
            if (forward < 0.0F && predictedForward > 0.0F) {
                continue;
            }
            if (forward == 0.0F && predictedForward != 0.0F) {
                continue;
            }

            for (float predictedStrafe = -1.0F; predictedStrafe <= 1.0F; ++predictedStrafe) {
                if (strafe > 0.0F && predictedStrafe < 0.0F) {
                    continue;
                }
                if (strafe < 0.0F && predictedStrafe > 0.0F) {
                    continue;
                }
                if (strafe == 0.0F && predictedStrafe != 0.0F) {
                    continue;
                }
                if (predictedStrafe == 0.0F && predictedForward == 0.0F) {
                    continue;
                }

                double predictedAngle = MathHelper.wrapDegrees(Math.toDegrees(direction(yaw, (double) predictedForward, (double) predictedStrafe)));
                double difference = Math.abs(angle - predictedAngle);
                if (difference < (double) closestDifference) {
                    closestDifference = (float) difference;
                    closestForward = predictedForward;
                    closestStrafe = predictedStrafe;
                }
            }
        }

        event.setForward(closestForward);
        event.setStrafe(closestStrafe);
    }

    public static int getSpeedEffect() {
        if (mc.player.isPotionActive(Effects.SPEED)) {
            return Objects.requireNonNull(mc.player.getActivePotionEffect(Effects.SPEED)).getAmplifier() + 1;
        }
        return 0;
    }

    public static double getBaseSpeed() {
        double baseSpeed = 0.2873;
        if (mc.player.isPotionActive(Effects.SPEED)) {
            int amplifier = mc.player.getActivePotionEffect(Effects.SPEED).getAmplifier();
            baseSpeed *= 1.0 + 0.2 * (double) (amplifier + 1);
        }
        return baseSpeed;
    }

    public static double direction(float rotationYaw, final double moveForward, final double moveStrafing) {
        if (moveForward < 0F) rotationYaw += 180F;
        float forward = 1F;
        if (moveForward < 0F) forward = -0.5F;
        else if (moveForward > 0F) forward = 0.5F;
        if (moveStrafing > 0F) rotationYaw -= 90F * forward;
        if (moveStrafing < 0F) rotationYaw += 90F * forward;
        return Math.toRadians(rotationYaw);
    }

    public static double getMotion() {
        return Math.hypot(mc.player.getMotion().x, mc.player.getMotion().z);
    }

    public static double getSpeed() {
        return Math.sqrt(mc.player.motion.x * mc.player.motion.x + mc.player.motion.z * mc.player.motion.z);
    }

    public static double[] getSpeed(double speed) {
        HitAura hitAura = Harmony.getInstance().getModuleManager().getHitAura();
        boolean check = (hitAura.isState() && hitAura.getTarget() != null && hitAura.getOptions().getValueByName("Коррекция движения").get());
        float yaw = check ? hitAura.rotateVector.x : mc.player.rotationYaw;
        float forward = mc.player.movementInput.moveForward;
        float strafe = mc.player.movementInput.moveStrafe;
        if (forward != 0) {
            if (strafe > 0) {
                yaw += (forward > 0 ? -45 : 45);
            } else if (strafe < 0) {
                yaw += (forward > 0 ? 45 : -45);
            }
            strafe = 0;
            if (forward > 0) {
                forward = 1;
            } else if (forward < 0) {
                forward = -1;
            }
        }
        return new double[] {
                (forward * speed * Math.cos(Math.toRadians(yaw + 90))
                        + strafe * speed * Math.sin(Math.toRadians(yaw + 90))),
                (forward * speed * Math.sin(Math.toRadians(yaw + 90))
                        - strafe * speed * Math.cos(Math.toRadians(yaw + 90))),
                yaw };
    }

    public static void setMotion(final double motion) {
        double forward = mc.player.movementInput.moveForward;
        double strafe = mc.player.movementInput.moveStrafe;
        HitAura hitAura = Harmony.getInstance().getModuleManager().getHitAura();
        boolean check = (hitAura.isState() && hitAura.getTarget() != null && hitAura.getOptions().getValueByName("Коррекция движения").get());
        float yaw = check ? hitAura.rotateVector.x : mc.player.rotationYaw;
        if (forward == 0 && strafe == 0) {
            mc.player.motion.x = 0;
            mc.player.motion.z = 0;
        } else {
            if (forward != 0) {
                if (strafe > 0) {
                    yaw += (float) (forward > 0 ? -45 : 45);
                } else if (strafe < 0) {
                    yaw += (float) (forward > 0 ? 45 : -45);
                }
                strafe = 0;
                if (forward > 0) {
                    forward = 1;
                } else if (forward < 0) {
                    forward = -1;
                }
            }
            mc.player.motion.x = forward * motion * MathHelper.cos(Math.toRadians(yaw + 90.0f))
                    + strafe * motion * MathHelper.sin(Math.toRadians(yaw + 90.0f));
            mc.player.motion.z = forward * motion * MathHelper.sin(Math.toRadians(yaw + 90.0f))
                    - strafe * motion * MathHelper.cos(Math.toRadians(yaw + 90.0f));
        }
    }

    public static void setCuttingSpeed(double speed) {
        boolean tickTime = mc.player.ticksExisted % 2 == 0;
        double forward = mc.player.movementInput.moveForward;
        double strafe = mc.player.movementInput.moveStrafe;
        float yaw = mc.player.rotationYaw - (mc.player.lastReportedYaw - mc.player.rotationYaw) * 2.0F;
        HitAura hitAura = Harmony.getInstance().getModuleManager().getHitAura();
        boolean check = (hitAura.isState() && hitAura.getTarget() != null && hitAura.getOptions().getValueByName("Коррекция движения").get());
        if (check) {
            yaw = hitAura.rotateVector.x;
        }
        if (!mc.gameSettings.keyBindForward.isKeyDown() && !mc.gameSettings.keyBindBack.isKeyDown() && !mc.gameSettings.keyBindLeft.isKeyDown() && !mc.gameSettings.keyBindRight.isKeyDown()) {
            mc.player.motion.x = tickTime ? 1.0E-10 : -1.0E-10;
            mc.player.motion.z = tickTime ? 1.0E-10 : -1.0E-10;
        } else {
            if (forward != 0.0) {
                if (strafe > 0.0) {
                    yaw += (float)(forward > 0.0 ? -45 : 45);
                } else if (strafe < 0.0) {
                    yaw += (float)(forward > 0.0 ? 45 : -45);
                }
                strafe = 0.0;
                if (forward > 0.0) {
                    forward = 1.0;
                } else if (forward < 0.0) {
                    forward = -1.0;
                }
            }
            double cos = Math.cos(Math.toRadians(yaw + 89.5F));
            double sin = Math.sin(Math.toRadians(yaw + 89.5F));
            mc.player.motion.x = forward * speed * cos + strafe * speed * sin;
            mc.player.motion.z = forward * speed * sin - strafe * speed * cos;
        }
    }

    public static void setSpeed(float speed) {
        HitAura hitAura = Harmony.getInstance().getModuleManager().getHitAura();
        boolean check = (hitAura.isState() && hitAura.getTarget() != null && hitAura.getOptions().getValueByName("Коррекция движения").get());
        float yaw = check ? hitAura.rotateVector.x : mc.player.rotationYaw;
        float forward = mc.player.movementInput.moveForward;
        float strafe = mc.player.movementInput.moveStrafe;
        if (forward != 0) {
            if (strafe > 0) {
                yaw += (forward > 0 ? -45 : 45);
            } else if (strafe < 0) {
                yaw += (forward > 0 ? 45 : -45);
            }
            strafe = 0;
            if (forward > 0) {
                forward = 1;
            } else if (forward < 0) {
                forward = -1;
            }
        }
        mc.player.motion.x = (forward * speed * Math.cos(Math.toRadians(yaw + 90)) + strafe * speed * Math.sin(Math.toRadians(yaw + 90)));
        mc.player.motion.z = (forward * speed * Math.sin(Math.toRadians(yaw + 90)) - strafe * speed * Math.cos(Math.toRadians(yaw + 90)));
    }

    public static void setSpeed(float speed, float noMoveSpeed) {
        HitAura hitAura = Harmony.getInstance().getModuleManager().getHitAura();
        boolean check = (hitAura.isState() && hitAura.getTarget() != null && hitAura.getOptions().getValueByName("Коррекция движения").get());
        float yaw = check ? hitAura.rotateVector.x : mc.player.rotationYaw;
        float forward = mc.player.movementInput.moveForward;
        float strafe = mc.player.movementInput.moveStrafe;
        if (isMoving()) {
            if (forward != 0) {
                if (strafe > 0) {
                    yaw += (forward > 0 ? -45 : 45);
                } else if (strafe < 0) {
                    yaw += (forward > 0 ? 45 : -45);
                }
                strafe = 0;
                if (forward > 0) {
                    forward = 1;
                } else if (forward < 0) {
                    forward = -1;
                }
            }
            mc.player.motion.x = (forward * speed * Math.cos(Math.toRadians(yaw + 90)) + strafe * speed * Math.sin(Math.toRadians(yaw + 90)));
            mc.player.motion.z = (forward * speed * Math.sin(Math.toRadians(yaw + 90)) - strafe * speed * Math.cos(Math.toRadians(yaw + 90)));
        } else {
            mc.player.motion.x *= noMoveSpeed;
            mc.player.motion.z *= noMoveSpeed;
        }
    }

    public boolean moveKeysPressed() {
        return mc.gameSettings.keyBindForward.isKeyDown() || mc.gameSettings.keyBindBack.isKeyDown() || mc.gameSettings.keyBindLeft.isKeyDown() || mc.gameSettings.keyBindRight.isKeyDown();
    }

    public double getCuttingSpeed() {
        return Math.sqrt(mc.player.getMotion().x * mc.player.getMotion().x + mc.player.getMotion().z * mc.player.getMotion().z);
    }

    public double[] forward(final double d) {
        float f = mc.player.movementInput.moveForward;
        float f2 = mc.player.movementInput.moveStrafe;
        HitAura hitAura = Harmony.getInstance().getModuleManager().getHitAura();
        boolean check = (hitAura.isState() && hitAura.getTarget() != null && hitAura.getOptions().getValueByName("Коррекция движения").get());
        float f3 = check ? hitAura.rotateVector.x : mc.player.rotationYaw;
        if (f != 0.0f) {
            if (f2 > 0.0f) {
                f3 += ((f > 0.0f) ? -45 : 45);
            } else if (f2 < 0.0f) {
                f3 += ((f > 0.0f) ? 45 : -45);
            }
            f2 = 0.0f;
            if (f > 0.0f) {
                f = 1.0f;
            } else if (f < 0.0f) {
                f = -1.0f;
            }
        }
        final double d2 = Math.sin(Math.toRadians(f3 + 90.0f));
        final double d3 = Math.cos(Math.toRadians(f3 + 90.0f));
        final double d4 = f * d * d3 + f2 * d * d2;
        final double d5 = f * d * d2 - f2 * d * d3;
        return new double[]{d4, d5};
    }

    public boolean isBlockUnder(float under) {
        if (mc.player.getPosY() < 0.0) {
            return false;
        } else {
            AxisAlignedBB aab = mc.player.getBoundingBox().offset(0.0, -under, 0.0);
            return mc.world.getCollisionShapes(mc.player, aab).toList().isEmpty();
        }
    }

    public boolean isBlockUnder() {
        for(int i = (int)(mc.player.getPosY() - 1.0); i > 0; --i) {
            BlockPos pos = new BlockPos(mc.player.getPosX(), (double)i, mc.player.getPosZ());
            if (!(mc.world.getBlockState(pos).getBlock() instanceof AirBlock)) {
                return true;
            }
        }
        return false;
    }

    public double getDirection(final boolean toRadians) {
        HitAura hitAura = Harmony.getInstance().getModuleManager().getHitAura();
        boolean check = (hitAura.isState() && hitAura.getTarget() != null && hitAura.getOptions().getValueByName("Коррекция движения").get());
        float rotationYaw = check ? hitAura.rotateVector.x : mc.player.rotationYaw;
        if (mc.player.moveForward < 0F)
            rotationYaw += 180F;
        float forward = 1F;
        if (mc.player.moveForward < 0F)
            forward = -0.5F;
        else if (mc.player.moveForward > 0F)
            forward = 0.5F;
        if (mc.player.moveStrafing > 0F)
            rotationYaw -= 90F * forward;
        if (mc.player.moveStrafing < 0F)
            rotationYaw += 90F * forward;
        return toRadians ? Math.toRadians(rotationYaw) : rotationYaw;
    }

    public float getDirection() {
        HitAura hitAura = Harmony.getInstance().getModuleManager().getHitAura();
        boolean check = (hitAura.isState() && hitAura.getTarget() != null && hitAura.getOptions().getValueByName("Коррекция движения").get());
        float rotationYaw = check ? hitAura.rotateVector.x : mc.player.rotationYaw;
        float strafeFactor = 0f;
        if (mc.player.movementInput.moveForward > 0)
            strafeFactor = 1;
        if (mc.player.movementInput.moveForward < 0)
            strafeFactor = -1;
        if (strafeFactor == 0) {
            if (mc.player.movementInput.moveStrafe > 0)
                rotationYaw -= 90;
            if (mc.player.movementInput.moveStrafe < 0)
                rotationYaw += 90;
        } else {
            if (mc.player.movementInput.moveStrafe > 0)
                rotationYaw -= 45 * strafeFactor;
            if (mc.player.movementInput.moveStrafe < 0)
                rotationYaw += 45 * strafeFactor;
        }
        if (strafeFactor < 0)
            rotationYaw -= 180;
        return (float) Math.toRadians(rotationYaw);
    }

    public void setStrafe(double motion) {
        if (!isMoving()) return;
        double radians = getDirection();
        mc.player.motion.x = -Math.sin(radians) * motion;
        mc.player.motion.z = Math.cos(radians) * motion;
    }

    public boolean moveKeyPressed(int keyNumber) {
        boolean w = mc.gameSettings.keyBindForward.isKeyDown();
        boolean a = mc.gameSettings.keyBindLeft.isKeyDown();
        boolean s = mc.gameSettings.keyBindBack.isKeyDown();
        boolean d = mc.gameSettings.keyBindRight.isKeyDown();
        return keyNumber == 0 ? w : (keyNumber == 1 ? a : (keyNumber == 2 ? s : keyNumber == 3 && d));
    }

    public boolean w() {
        return moveKeyPressed(0);
    }

    public boolean a() {
        return moveKeyPressed(1);
    }

    public boolean s() {
        return moveKeyPressed(2);
    }

    public boolean d() {
        return moveKeyPressed(3);
    }

    public float moveYaw(float entityYaw) {
        return entityYaw + (float)(!a() || !d() || w() && s() || !w() && !s() ? (w() && s() && (!a() || !d()) && (a() || d()) ? (a() ? -90 : (d() ? 90 : 0)) : (a() && d() && (!w() || !s()) || w() && s() && (!a() || !d()) ? 0 : (!a() && !d() && !s() ? 0 : (w() && !s() ? 45 : (s() && !w() ? (!a() && !d() ? 180 : 135) : ((w() || s()) && (!w() || !s()) ? 0 : 90))) * (a() ? -1 : 1)))) : (w() ? 0 : (s() ? 180 : 0)));
    }

    public void update(MovementInput input, float speed, int ticks, int priority) {
        if (mc.player == null) return;

        float yaw = input.getYaw();
        float pitch = input.getPitch();

        mc.player.rotationYaw = yaw;
        mc.player.rotationPitch = pitch;

        double[] motion = getSpeed(speed / 20.0);
        mc.player.motion.x = motion[0];
        mc.player.motion.z = motion[1];
    }

    public float getFreePitch() {
        return mc.player != null ? mc.player.rotationPitch : 0.0f;
    }

    public static class MovementInput {
        private final float yaw;
        private final float pitch;

        public MovementInput(float yaw, float pitch) {
            this.yaw = yaw;
            this.pitch = pitch;
        }

        public float getYaw() {
            return yaw;
        }

        public float getPitch() {
            return pitch;
        }
    }

    public class MoveEvent {
        public static void setMoveMotion(final MovingEvent move, final double motion) {
            double forward = mc.player.movementInput.moveForward;
            double strafe = mc.player.movementInput.moveStrafe;
            HitAura hitAura = Harmony.getInstance().getModuleManager().getHitAura();
            boolean check = (hitAura.isState() && hitAura.getTarget() != null && hitAura.getOptions().getValueByName("Коррекция движения").get());
            float yaw = check ? hitAura.rotateVector.x : mc.player.rotationYaw;
            if (forward == 0 && strafe == 0) {
                move.getMotion().x = 0;
                move.getMotion().z = 0;
            } else {
                if (forward != 0) {
                    if (strafe > 0) {
                        yaw += (float) (forward > 0 ? -45 : 45);
                    } else if (strafe < 0) {
                        yaw += (float) (forward > 0 ? 45 : -45);
                    }
                    strafe = 0;
                    if (forward > 0) {
                        forward = 1;
                    } else if (forward < 0) {
                        forward = -1;
                    }
                }
                move.getMotion().x = forward * motion * MathHelper.cos((float) Math.toRadians(yaw + 90.0f))
                        + strafe * motion * MathHelper.sin((float) Math.toRadians(yaw + 90.0f));
                move.getMotion().z = forward * motion * MathHelper.sin((float) Math.toRadians(yaw + 90.0f))
                        - strafe * motion * MathHelper.cos((float) Math.toRadians(yaw + 90.0f));
            }
        }
    }
}
