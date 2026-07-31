package ca.fxco.moreculling.utils;

import ca.fxco.moreculling.config.MoreCullingConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.client.GameSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.settings.GraphicsFanciness;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;

import java.util.Optional;

public final class CullingUtils
{
    private static final Direction[] DIRECTIONS = Direction.values();

    private CullingUtils()
    {
    }

    public static boolean areLeavesOpaque()
    {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft == null)
        {
            return false;
        }

        GameSettings gamesettings = minecraft.gameSettings;
        return gamesettings != null && gamesettings.graphicFanciness == GraphicsFanciness.FAST;
    }

    public static boolean hasModelTranslucency(BlockState state)
    {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft == null)
        {
            return true;
        }

        BlockRendererDispatcher blockrendererdispatcher = minecraft.getBlockRendererDispatcher();

        if (blockrendererdispatcher == null)
        {
            return true;
        }

        IBakedModel ibakedmodel = blockrendererdispatcher.getModelForState(state);
        return ibakedmodel == null || ibakedmodel.hasTextureTranslucency(state);
    }

    public static Optional<Boolean> shouldDrawFaceCheck(IBlockReader view, BlockState sideState, BlockPos thisPos, BlockPos sidePos, Direction side)
    {
        if (sideState.getBlock() instanceof LeavesBlock || sideState.isSolidSide(view, sidePos, side))
        {
            boolean flag = true;

            for (Direction direction : DIRECTIONS)
            {
                if (direction != side)
                {
                    BlockPos blockpos = thisPos.offset(direction);
                    BlockState blockstate = view.getBlockState(blockpos);
                    flag &= blockstate.getBlock() instanceof LeavesBlock || blockstate.isSolidSide(view, blockpos, direction);
                }
            }

            return flag ? Optional.of(false) : Optional.empty();
        }

        return Optional.of(true);
    }

    public static Optional<Boolean> shouldDrawFaceDepth(IBlockReader view, BlockState sideState, BlockPos sidePos, Direction side)
    {
        if (sideState.getBlock() instanceof LeavesBlock || sideState.isSolidSide(view, sidePos, side))
        {
            for (int i = 1; i < MoreCullingConfig.leavesCullingDepth + 1; ++i)
            {
                BlockPos blockpos = sidePos.offset(side, i);
                BlockState blockstate = view.getBlockState(blockpos);

                if (!(blockstate.getBlock() instanceof LeavesBlock) && !blockstate.isSolidSide(view, blockpos, side))
                {
                    return Optional.of(false);
                }
            }
        }

        return Optional.of(true);
    }
}
