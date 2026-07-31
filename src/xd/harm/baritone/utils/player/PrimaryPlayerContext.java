

package xd.harm.baritone.utils.player;

import xd.harm.baritone.api.BaritoneAPI;
import xd.harm.baritone.api.cache.IWorldData;
import xd.harm.baritone.api.utils.Helper;
import xd.harm.baritone.api.utils.IPlayerContext;
import xd.harm.baritone.api.utils.IPlayerController;
import xd.harm.baritone.api.utils.RayTraceUtils;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

/**
 * Implementation of {@link IPlayerContext} that provides information about the primary player.
 *
 * @author Brady
 * @since 11/12/2018
 */
public enum PrimaryPlayerContext implements IPlayerContext, Helper {

    INSTANCE;

    @Override
    public ClientPlayerEntity player() {
        return mc.player;
    }

    @Override
    public IPlayerController playerController() {
        return PrimaryPlayerController.INSTANCE;
    }

    @Override
    public World world() {
        return mc.world;
    }

    @Override
    public IWorldData worldData() {
        return BaritoneAPI.getProvider().getPrimaryBaritone().getWorldProvider().getCurrentWorld();
    }

    @Override
    public RayTraceResult objectMouseOver() {
        return RayTraceUtils.rayTraceTowards(player(), playerRotations(), playerController().getBlockReachDistance());
    }
}
