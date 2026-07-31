

package xd.harm.baritone.api.utils;

import xd.harm.baritone.api.cache.IWorldData;
import net.minecraft.block.SlabBlock;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author Brady
 * @since 11/12/2018
 */
public interface IPlayerContext {

    ClientPlayerEntity player();

    IPlayerController playerController();

    World world();

    default Iterable<Entity> entities() {
        return ((ClientWorld) world()).getAllEntities();
    }

    default Stream<Entity> entitiesStream() {
        return StreamSupport.stream(entities().spliterator(), false);
    }


    IWorldData worldData();

    RayTraceResult objectMouseOver();

    default BetterBlockPos playerFeet() {
        BetterBlockPos feet = new BetterBlockPos(player().getPositionVec().x, player().getPositionVec().y + 0.1251, player().getPositionVec().z);
        try {
            if (world().getBlockState(feet).getBlock() instanceof SlabBlock) {
                return feet.up();
            }
        } catch (NullPointerException ignored) {}

        return feet;
    }

    default Vector3d playerFeetAsVec() {
        return new Vector3d(player().getPositionVec().x, player().getPositionVec().y, player().getPositionVec().z);
    }

    default Vector3d playerHead() {
        return new Vector3d(player().getPositionVec().x, player().getPositionVec().y + player().getEyeHeight(), player().getPositionVec().z);
    }

    default Rotation playerRotations() {
        return new Rotation(player().rotationYaw, player().rotationPitch);
    }

    static double eyeHeight(boolean ifSneaking) {
        return ifSneaking ? 1.27 : 1.62;
    }

    /**
     * Returns the block that the crosshair is currently placed over. Updated once per tick.
     *
     * @return The position of the highlighted block
     */
    default Optional<BlockPos> getSelectedBlock() {
        RayTraceResult result = objectMouseOver();
        if (result != null && result.getType() == RayTraceResult.Type.BLOCK) {
            return Optional.of(((BlockRayTraceResult) result).getPos());
        }
        return Optional.empty();
    }

    default boolean isLookingAt(BlockPos pos) {
        return getSelectedBlock().equals(Optional.of(pos));
    }
}
