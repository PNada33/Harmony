package xd.harm.modules.models.dog;

import com.mojang.blaze3d.platform.GlStateManager;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import xd.harm.Harmony;
import xd.harm.utils.client.IMinecraft;

import java.util.concurrent.ThreadLocalRandom;

public class RotateUtility implements IMinecraft {

    @Getter
    private Vector3d targetVec;

    public static Vector2f get(Vector3d from, Vector3d target) {
        Vector3d vec = target;
        double posX = vec.getX() - from.getX();
        double posY = vec.getY() - from.getY();
        double posZ = vec.getZ() - from.getZ();
        double sqrt = MathHelper.sqrt(posX * posX + posZ * posZ);
        float yaw = (float) (Math.atan2(posZ, posX) * 180.0 / Math.PI) - 90.0f;
        float pitch = (float) (-(Math.atan2(posY, sqrt) * 180.0 / Math.PI));
        float sens = (float) (Math.pow(mc.gameSettings.mouseSensitivity, 1.5) * 0.05f + 0.1f);
        float pow = sens * sens * sens * 1.2F;
        yaw -= yaw % pow;
        pitch -= pitch % (pow * sens);
        return new Vector2f(yaw, pitch);
    }


    public Vector2f get(Vector3d target) {
        double posX = target.getX() - mc.player.getPosX();
        double posY = target.getY() - (mc.player.getPosY() + (double) mc.player.getEyeHeight());
        double posZ = target.getZ() - mc.player.getPosZ();
        double sqrt = MathHelper.sqrt(posX * posX + posZ * posZ);
        float yaw = (float) (Math.atan2(posZ, posX) * 180.0 / Math.PI) - 90.0f;
        float pitch = (float) (-(Math.atan2(posY, sqrt) * 180.0 / Math.PI));
        float sens = (float) (Math.pow(mc.gameSettings.mouseSensitivity, 1.5) * 0.05f + 0.1f);
        float pow = sens * sens * sens * 1.2F;
        yaw -= yaw % pow;
        pitch -= pitch % (pow * sens);
        return new Vector2f(yaw, pitch);
    }


    public Vector3d getPoint(LivingEntity target) {
        if (target == null) return Vector3d.ZERO;
        return getBestPoint(mc.player.getEyePosition(mc.timer.renderPartialTicks), target);
    }
    public Vector3d getBestPoint(Vector3d pos, LivingEntity entity) {
        if (entity == null) return Vector3d.ZERO;


        double safePoint = 0;
        Vector3d fastPoint = new Vector3d(
                MathHelper.clamp(pos.x,
                        entity.getBoundingBox().minX + safePoint,
                        entity.getBoundingBox().maxX - safePoint),

                MathHelper.clamp(pos.y,
                        entity.getBoundingBox().minY + safePoint,
                        entity.getBoundingBox().maxY - safePoint),

                MathHelper.clamp(pos.z,
                        entity.getBoundingBox().minZ + safePoint,
                        entity.getBoundingBox().maxZ - safePoint)
        );
        return fastPoint;
    }
    public Vector2f getRotationsToVec(Vector3d targetVec) {
        Vector3d eyePos = mc.player.getEyePosition(1.0f);
        Vector3d direction = targetVec.subtract(eyePos);

        double horiz = Math.sqrt(direction.x * direction.x + direction.z * direction.z);

        float yaw = (float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0f;
        float pitch = (float) -Math.toDegrees(Math.atan2(direction.y, horiz));

        return new Vector2f(MathHelper.wrapDegrees(yaw), MathHelper.clamp(pitch, -90.0f, 90.0f));
    }
}