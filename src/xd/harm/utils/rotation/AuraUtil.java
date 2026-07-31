




package xd.harm.utils.rotation;

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
import net.minecraft.util.math.vector.Vector3d;
import xd.harm.utils.client.IMinecraft;

public class AuraUtil implements IMinecraft {
    private static Vector3d calculateVector(LivingEntity target) {
        Minecraft var10000 = mc;
        double yOffset = MathHelper.clamp(Minecraft.player.getPosYEye() - target.getPosYEye(), 0.2, (double)target.getEyeHeight());
        return target.getPositionVec().add((double)0.0F, yOffset, (double)0.0F);
    }

    public static Vector3d getClosestVec(Vector3d vec, AxisAlignedBB AABB) {
        return new Vector3d(MathHelper.clamp(vec.getX(), AABB.minX, AABB.maxX), MathHelper.clamp(vec.getY(), AABB.minY, AABB.maxY), MathHelper.clamp(vec.getZ(), AABB.minZ, AABB.maxZ));
    }

    public static Vector3d getClosestVec(Entity entity) {
        Minecraft var10000 = mc;
        Vector3d eyePosVec = Minecraft.player.getEyePosition(1.0F);
        return getClosestVec(eyePosVec, entity).subtract(eyePosVec);
    }

    public static double getStrictDistance(Entity entity) {
        return getClosestVec(entity).length();
    }

    public static Vector3d getClosestVec(Vector3d vec, Entity entity) {
        return getClosestVec(vec, entity.getBoundingBox());
    }

    public static Vector3d getBestVec(Vector3d pos, AxisAlignedBB axisAlignedBB) {
        double lastDistance = Double.MAX_VALUE;
        Vector3d bestVec = null;
        double xWidth = axisAlignedBB.maxX - axisAlignedBB.minX;
        double zWidth = axisAlignedBB.maxZ - axisAlignedBB.minZ;
        double height = axisAlignedBB.maxY - axisAlignedBB.minY;

        for(float x = 0.0F; x < 1.0F; x += 0.1F) {
            for(float y = 0.0F; y < 1.0F; y += 0.1F) {
                for(float z = 0.0F; z < 1.0F; z += 0.1F) {
                    Vector3d hitVec = new Vector3d(axisAlignedBB.minX + xWidth * (double)x, axisAlignedBB.minY + height * (double)y, axisAlignedBB.minZ + zWidth * (double)z);
                    double distance = pos.distanceTo(hitVec);
                    if (isHitBoxNotVisible(hitVec) && distance < lastDistance) {
                        bestVec = hitVec;
                        lastDistance = distance;
                    }
                }
            }
        }

        return bestVec;
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

    public static Vector3d getVector(LivingEntity target) {
        double wHalf = (double)target.getWidth() / (double)3.0F;
        double yExpand = MathHelper.clamp(target.getPosYEye() - target.getPosY(), (double)0.0F, (double)target.getHeight());
        Minecraft var10000 = mc;
        double xExpand = MathHelper.clamp(Minecraft.player.getPosX() - target.getPosX(), -wHalf, wHalf);
        var10000 = mc;
        double zExpand = MathHelper.clamp(Minecraft.player.getPosZ() - target.getPosZ(), -wHalf, wHalf);
        double var10002 = target.getPosX();
        Minecraft var10003 = mc;
        var10002 = var10002 - Minecraft.player.getPosX() + xExpand;
        double var11 = target.getPosY();
        Minecraft var10004 = mc;
        var11 = var11 - Minecraft.player.getPosYEye() + yExpand;
        double var13 = target.getPosZ();
        Minecraft var10005 = mc;
        return new Vector3d(var10002, var11, var13 - Minecraft.player.getPosZ() + zExpand);
    }
}
