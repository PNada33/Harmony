
package xd.harm.utils.rotation;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import org.joml.Vector2f;
import xd.harm.Harmony;
import xd.harm.utils.client.IMinecraft;
import xd.harm.utils.math.VectorUtils;

public class RotationUtils implements IMinecraft {
    public RotationUtils() {
        Harmony.getInstance().getEventBus().register(this);
    }

    public Vector3d getClosestVec(Entity entity) {
        Minecraft var10000 = mc;
        Vector3d eyePosVec = Minecraft.player.getEyePosition(1.0F);
        return VectorUtils.getClosestVec(eyePosVec, entity).subtract(eyePosVec);
    }

    public Vector2f calculate(double x, double y, double z) {
        Minecraft var10000 = mc;
        Vector3d var8 = Minecraft.player.getPositionVec();
        Minecraft var10002 = mc;
        Vector3d pos = var8.add((double)0.0F, (double)Minecraft.player.getEyeHeight(), (double)0.0F);
        return calculate(new org.joml.Vector3d(pos.x, pos.y, pos.z), new org.joml.Vector3d(x, y, z));
    }

    public static float[] calculateAngle(Vector3d from, Vector3d to) {
        double difX = to.x - from.x;
        double difY = (to.y - from.y) * (double)-1.0F;
        double difZ = to.z - from.z;
        double dist = (double)MathHelper.sqrt((float)(difX * difX + difZ * difZ));
        float yD = (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(difZ, difX)) - (double)90.0F);
        float pD = (float)MathHelper.clamp(MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(difY, dist))), (double)-90.0F, (double)90.0F);
        return new float[]{yD, pD};
    }

    public static Vector2f calculate(org.joml.Vector3d to) {
        Minecraft var10000 = mc;
        Vector3d var3 = Minecraft.player.getPositionVec();
        Minecraft var10002 = mc;
        Vector3d pos = var3.add((double)0.0F, (double)Minecraft.player.getEyeHeight(), (double)0.0F);
        org.joml.Vector3d from = new org.joml.Vector3d(pos.x, pos.y, pos.z);
        return calculate(from, to);
    }

    public static Vector2f calculate(org.joml.Vector3d from, org.joml.Vector3d to) {
        org.joml.Vector3d diff = to.sub(from);
        double distance = Math.hypot(diff.x(), diff.z());
        float yaw = (float)(MathHelper.atan2(diff.z(), diff.x()) * (double)180.0F / Math.PI) - 90.0F;
        float pitch = (float)(-(MathHelper.atan2(diff.y(), distance) * (double)180.0F / Math.PI));
        yaw = normalize(yaw);
        pitch = org.joml.Math.clamp(-90.0F, 90.0F, pitch);
        return new Vector2f(yaw, pitch);
    }

    public static float normalize(float value) {
        value %= 360.0F;
        if (value > 180.0F) {
            value -= 360.0F;
        } else if (value < -180.0F) {
            value += 360.0F;
        }

        return value;
    }

    public static Vector2f calculate(Entity entity) {
        Vector3d var10000 = entity.getPositionVec();
        Minecraft var10003 = mc;
        double var3 = Minecraft.player.getPosY() - entity.getPosY();
        Minecraft var10004 = mc;
        Vector3d pos = var10000.add((double)0.0F, Math.max((double)0.0F, org.joml.Math.min(var3 + (double)Minecraft.player.getEyeHeight(), (entity.getBoundingBox().maxY - entity.getBoundingBox().minY) * (double)0.75F)), (double)0.0F);
        org.joml.Vector3d to = new org.joml.Vector3d(pos.x, pos.y, pos.z);
        return calculate(to);
    }

    public double getStrictDistance(Entity entity) {
        return this.getClosestVec(entity).length();
    }

    public static float[] getMatrixRots(Entity target) {
        double var10000 = target.getPosX();
        Minecraft var10001 = mc;
        double dX = var10000 - Minecraft.player.getPosX();
        var10000 = target.getPosZ();
        var10001 = mc;
        double dZ = var10000 - Minecraft.player.getPosZ();
        var10000 = target.getPosY() + (double)target.getEyeHeight();
        var10001 = mc;
        double var15 = Minecraft.player.getPosY();
        Minecraft var10002 = mc;
        double dY = var10000 - (var15 + (double)Minecraft.player.getEyeHeight());
        double dist = Math.sqrt(dX * dX + dZ * dZ);
        float yaw = (float)(Math.toDegrees(Math.atan2(dZ, dX)) - (double)90.0F);
        float pitch = (float)(-Math.toDegrees(Math.atan2(dY, dist)));
        return new float[]{yaw, pitch};
    }
}
