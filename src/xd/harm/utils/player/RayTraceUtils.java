package xd.harm.utils.player;

import xd.harm.utils.client.IMinecraft;
import lombok.experimental.UtilityClass;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;

import java.util.Optional;
import java.util.function.Predicate;

import static xd.harm.utils.player.MouseUtil.getVectorForRotation;

@UtilityClass
public class RayTraceUtils implements IMinecraft {

    public boolean rayTraceSingleEntity(float yaw, float pitch, double distance, Entity entity) {
        Vector3d eyeVec = mc.player.getEyePosition(1.0F);
        Vector3d lookVec = mc.player.getVectorForRotation(pitch, yaw);
        Vector3d extendedVec = eyeVec.add(lookVec.scale(distance));

        AxisAlignedBB AABB = entity.getBoundingBox();

        return AABB.contains(eyeVec) || AABB.rayTrace(eyeVec, extendedVec).isPresent();
    }

    public static RayTraceResult rayTrace(double rayTraceDistance,
                                          float yaw,
                                          float pitch,
                                          Entity entity) {
        Vector3d startVec = mc.player.getEyePosition(1.0F);
        Vector3d directionVec = getVectorForRotation(pitch, yaw);
        Vector3d endVec = startVec.add(
                directionVec.x * rayTraceDistance,
                directionVec.y * rayTraceDistance,
                directionVec.z * rayTraceDistance
        );

        return mc.world.rayTraceBlocks(new RayTraceContext(
                startVec,
                endVec,
                RayTraceContext.BlockMode.OUTLINE,
                RayTraceContext.FluidMode.NONE,
                entity)
        );
    }

    public boolean isHitBoxNotVisible(final Vector3d vec3d) {
        final RayTraceContext rayTraceContext = new RayTraceContext(
                mc.player.getEyePosition(1F),
                vec3d,
                RayTraceContext.BlockMode.COLLIDER,
                RayTraceContext.FluidMode.NONE,
                mc.player
        );
        final BlockRayTraceResult blockHitResult = mc.world.rayTraceBlocks(rayTraceContext);
        return blockHitResult.getType() == RayTraceResult.Type.MISS;
    }

    public static EntityRayTraceResult rayTraceEntity(Entity shooter, Vector3d startVec, Vector3d endVec,
                                                      AxisAlignedBB boundingBox, Predicate<Entity> filter, double maxDistance) {
        double closestDistance = maxDistance;
        Entity closestEntity = null;
        Vector3d closestHitVec = null;

        for (Entity entity : mc.world.getEntitiesInAABBexcluding(shooter, boundingBox, filter::test)) {
            AxisAlignedBB entityBB = entity.getBoundingBox().grow(0.3);
            Optional<Vector3d> rayTraceResult = entityBB.rayTrace(startVec, endVec);

            if (rayTraceResult.isPresent()) {
                double distance = startVec.distanceTo(rayTraceResult.get());
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestEntity = entity;
                    closestHitVec = rayTraceResult.get();
                }
            }
        }

        return closestEntity != null ? new EntityRayTraceResult(closestEntity, closestHitVec) : null;
    }


    public static boolean rayTraceEntity(float yaw, float pitch, double distance, Entity entity) {
        return rayTraceSingleEntity(yaw, pitch, distance, entity);
    }

    public static EntityRayTraceResult rayTraceEntity(double distance, float yaw, float pitch) {
        Vector3d startVec = mc.player.getEyePosition(1.0F);
        Vector3d directionVec = getVectorForRotation(pitch, yaw);
        Vector3d endVec = startVec.add(directionVec.scale(distance));

        AxisAlignedBB boundingBox = mc.player.getBoundingBox().expand(directionVec.scale(distance)).grow(1.0D);

        Predicate<Entity> filter = entity -> !entity.isSpectator() && entity.canBeCollidedWith() && entity != mc.player;

        return rayTraceEntity(mc.player, startVec, endVec, boundingBox, filter, distance);
    }

    public static Optional<Entity> getEntityInCrosshair(double distance, float yaw, float pitch) {
        EntityRayTraceResult result = rayTraceEntity(distance, yaw, pitch);
        return result != null ? Optional.of(result.getEntity()) : Optional.empty();
    }

    public static RayTraceResult rayTraceWithEntities(double distance, float yaw, float pitch) {
        Vector3d startVec = mc.player.getEyePosition(1.0F);
        Vector3d directionVec = getVectorForRotation(pitch, yaw);
        Vector3d endVec = startVec.add(directionVec.scale(distance));

        RayTraceContext context = new RayTraceContext(
                startVec,
                endVec,
                RayTraceContext.BlockMode.OUTLINE,
                RayTraceContext.FluidMode.NONE,
                mc.player
        );

        BlockRayTraceResult blockResult = mc.world.rayTraceBlocks(context);

        double blockDistance = blockResult.getType() != RayTraceResult.Type.MISS ?
                startVec.distanceTo(blockResult.getHitVec()) : distance;

        EntityRayTraceResult entityResult = rayTraceEntity(blockDistance, yaw, pitch);

        if (entityResult != null) {
            double entityDistance = startVec.distanceTo(entityResult.getHitVec());
            if (entityDistance < blockDistance) {
                return entityResult;
            }
        }

        return blockResult;
    }
}