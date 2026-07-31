package xd.harm.utils.rotation;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceContext.BlockMode;
import net.minecraft.util.math.RayTraceContext.FluidMode;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import xd.harm.utils.client.IMinecraft;

public class RotationUtility implements IMinecraft {
    static float lastYaw = 0.0F;

    public static Vector3d getClosestVec(Entity entity) {
        double wHalf = (double)(entity.getWidth() / 2.0F);
        double yExpand = MathHelper.clamp(entity.getPosYEye() - entity.getPosY(), (double)0.0F, (double)entity.getHeight());
        Minecraft var10000 = mc;
        double xExpand = MathHelper.clamp(Minecraft.player.getPosX() - entity.getPosX(), -wHalf, wHalf);
        var10000 = mc;
        double zExpand = MathHelper.clamp(Minecraft.player.getPosZ() - entity.getPosZ(), -wHalf, wHalf);
        double var10002 = entity.getPosX();
        Minecraft var10003 = mc;
        var10002 = var10002 - Minecraft.player.getPosX() + xExpand;
        double var11 = entity.getPosY();
        Minecraft var10004 = mc;
        var11 = var11 - Minecraft.player.getPosYEye() + yExpand;
        double var13 = entity.getPosZ();
        Minecraft var10005 = mc;
        return new Vector3d(var10002, var11, var13 - Minecraft.player.getPosZ() + zExpand);
    }

    public static Rotation getRotation(Vector3d vec) {
        return new Rotation((float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(vec.z, vec.x)) - (double)90.0F), (float)MathHelper.wrapDegrees(Math.toDegrees(-Math.atan2(vec.y, Math.hypot(vec.x, vec.z)))));
    }

    public static VecRotation createRotation(Vector3d vector) {
        return new VecRotation((float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(vector.z, vector.x)) - (double)90.0F), (float)MathHelper.wrapDegrees(Math.toDegrees(-Math.atan2(vector.y, Math.hypot(vector.x, vector.z)))));
    }

    public static Rotation resetRotation(Rotation rotation) {
        if (rotation == null) {
            return null;
        } else {
            float var10000 = rotation.getYaw();
            Minecraft var10001 = mc;
            float yaw = var10000 + MathHelper.wrapDegrees(Minecraft.player.rotationYaw - rotation.getYaw());
            Minecraft var3 = mc;
            float pitch = Minecraft.player.rotationPitch;
            return new Rotation(yaw, pitch);
        }
    }

    public static Vector3d raytraceBox(Vector3d vector, AxisAlignedBB box, double rangeSquared) {
        Vector3d vector3d = null;
        if (!isHitBoxNotVisible(vector)) {
            for(double x = (double)0.0F; x <= (double)1.0F; x += 0.1) {
                for(double y = (double)0.0F; y <= (double)1.0F; y += 0.1) {
                    for(double z = (double)0.0F; z <= (double)1.0F; z += 0.1) {
                        Vector3d spot = new Vector3d(box.minX + (box.maxX - box.minX) * x, box.minY + (box.maxY - box.minY) * y, box.minZ + (box.maxZ - box.minZ) * z);
                        double distance = vector.squareDistanceTo(spot);
                        if (!(distance > rangeSquared) && isHitBoxNotVisible(spot)) {
                            vector3d = spot;
                            break;
                        }
                    }
                }
            }
        }

        return vector3d;
    }

    public static float squaredDistanceFromEyes(Vector3d vec) {
        Minecraft var10001 = mc;
        double d0 = vec.x - Minecraft.player.getPosX();
        var10001 = mc;
        double d1 = vec.z - Minecraft.player.getPosZ();
        double var10000 = vec.y;
        var10001 = mc;
        double var9 = Minecraft.player.getPosY();
        Minecraft var10002 = mc;
        Minecraft var10003 = mc;
        double d2 = var10000 - (var9 + (double)Minecraft.player.getEyeHeight(Minecraft.player.getPose()));
        return (float)(d0 * d0 + d1 * d1 + d2 * d2);
    }

    public static boolean isHitBoxNotVisible(Vector3d vec3d) {
        Minecraft var10002 = mc;
        Vector3d var3 = Minecraft.player.getEyePosition(1.0F);
        Minecraft var10006 = mc;
        RayTraceContext rayTraceContext = new RayTraceContext(var3, vec3d, BlockMode.COLLIDER, FluidMode.NONE, Minecraft.player);
        Minecraft var10000 = mc;
        BlockRayTraceResult blockHitResult = Minecraft.world.rayTraceBlocks(rayTraceContext);
        return blockHitResult.getType() == Type.MISS;
    }

    public static Vector3d getNearestPoint(Vector3d eyes, AxisAlignedBB box) {
        double[] origin = new double[]{eyes.x, eyes.y, eyes.z};
        double[] destMins = new double[]{box.minX, box.minY, box.minZ};
        double[] destMaxs = new double[]{box.maxX, box.maxY, box.maxZ};

        for(int i = 0; i <= 2; ++i) {
            origin[i] = Math.max(destMins[i], Math.min(destMaxs[i], origin[i]));
        }

        return new Vector3d(origin[0], origin[1], origin[2]);
    }

    public static Rotation getRotation(Rotation lastRotation, Rotation targetRotation, double speed) {
        float yaw = targetRotation.getYaw();
        float pitch = targetRotation.getPitch();
        float lastYaw = lastRotation.getYaw();
        float lastPitch = lastRotation.getPitch();
        float rotationSpeed = (float)speed;
        if (speed > (double)0.0F) {
            double deltaYaw = (double)MathHelper.wrapDegrees(targetRotation.getYaw() - lastRotation.getYaw());
            double deltaPitch = (double)(pitch - lastPitch);
            double distance = Math.sqrt(deltaYaw * deltaYaw + deltaPitch * deltaPitch);
            double distributionYaw = Math.abs(deltaYaw / distance);
            double distributionPitch = Math.abs(deltaPitch / distance);
            double maxYaw = (double)rotationSpeed * distributionYaw;
            double maxPitch = (double)rotationSpeed * distributionPitch;
            float moveYaw = (float)Math.max(Math.min(deltaYaw, maxYaw), -maxYaw);
            float movePitch = (float)Math.max(Math.min(deltaPitch, maxPitch), -maxPitch);
            yaw = lastYaw + moveYaw;
            pitch = lastPitch + movePitch;
        }

        yaw += ThreadLocalRandom.current().nextFloat(-0.5F, 0.5F);
        pitch -= ThreadLocalRandom.current().nextFloat(-0.5F, 0.5F);
        Rotation rotations = new Rotation(yaw, pitch);
        yaw = rotations.getYaw();
        pitch = Math.max(-90.0F, Math.min(90.0F, rotations.getPitch()));
        return new Rotation(yaw, pitch);
    }

    public static Vector2f getFakeRotation(LivingEntity target, float attackRange, Vector2f previousRotation) {
        Vector3d targetPosition = calculateVectorToTarget(target, (double)attackRange);
        float targetYaw = (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(targetPosition.z, targetPosition.x)) - (double)90.0F);
        float targetPitch = (float)Math.toDegrees(-Math.atan2(targetPosition.y, Math.hypot(targetPosition.x, targetPosition.z)));
        float yawDifference = MathHelper.wrapDegrees(targetYaw - previousRotation.x);
        float pitchDifference = targetPitch - previousRotation.y;
        double distance = Math.sqrt((double)(yawDifference * yawDifference + pitchDifference * pitchDifference));
        double distributionYaw = Math.abs((double)yawDifference / distance);
        double distributionPitch = Math.abs((double)pitchDifference / distance);
        float maxYaw = (float)((double)90.0F * distributionYaw);
        float maxPitch = (float)((double)30.0F * distributionPitch);
        float clampedYaw = MathHelper.clamp(yawDifference, -maxYaw, maxYaw);
        float clampedPitch = MathHelper.clamp(pitchDifference, -maxPitch, maxPitch);
        float newYaw = previousRotation.x + clampedYaw;
        float newPitch = previousRotation.y + clampedPitch;
        return new Vector2f(newYaw, MathHelper.clamp(newPitch, -90.0F, 90.0F));
    }

    public static Vector3d calculateVectorToTarget(Entity target, double attackRange) {
        Minecraft var10000 = mc;
        double eyePositionYOffset = Minecraft.player.getEyePosition(1.0F).y - target.getPosY();
        double maxOffset = (double)target.getHeight() * (getDistanceEyePos(target) / attackRange);
        double clampedYOffset = MathHelper.clamp(eyePositionYOffset, (double)0.0F, maxOffset);
        var10000 = mc;
        Vector3d playerEyePosition = Minecraft.player.getEyePosition(1.0F);
        return target.getPositionVec().add((double)0.0F, clampedYOffset, (double)0.0F).subtract(playerEyePosition);
    }

    public static double getDistanceEyePos(Entity target) {
        double wHalf = (double)(target.getWidth() / 2.0F);
        Minecraft var10000 = mc;
        double x = MathHelper.clamp(Minecraft.player.getPosX() - target.getPosX(), -wHalf, wHalf);
        var10000 = mc;
        double var10 = Minecraft.player.getPosY();
        Minecraft var10001 = mc;
        double y = MathHelper.clamp(var10 + (double)Minecraft.player.getEyeHeight() - target.getPosY(), (double)0.0F, (double)target.getHeight());
        Minecraft var11 = mc;
        double z = MathHelper.clamp(Minecraft.player.getPosZ() - target.getPosZ(), -wHalf, wHalf);
        var11 = mc;
        return Minecraft.player.getEyePosition(1.0F).distanceTo(target.getPositionVec().add(x, y, z));
    }

    public static double getStrictDistance(Entity entity) {
        return getClosestVec(entity).length();
    }
}
