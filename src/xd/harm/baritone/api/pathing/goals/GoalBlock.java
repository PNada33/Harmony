

package xd.harm.baritone.api.pathing.goals;

import xd.harm.baritone.api.utils.SettingsUtil;
import xd.harm.baritone.api.utils.interfaces.IGoalRenderPos;
import net.minecraft.util.math.BlockPos;

/**
 * A specific BlockPos goal
 *
 * @author leijurv
 */
public class GoalBlock implements Goal, IGoalRenderPos {

    /**
     * The X block position of this goal
     */
    public final int x;

    /**
     * The Y block position of this goal
     */
    public final int y;

    /**
     * The Z block position of this goal
     */
    public final int z;

    public GoalBlock(BlockPos pos) {
        this(pos.getX(), pos.getY(), pos.getZ());
    }

    public GoalBlock(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public boolean isInGoal(int x, int y, int z) {
        return x == this.x && y == this.y && z == this.z;
    }

    @Override
    public double heuristic(int x, int y, int z) {
        int xDiff = x - this.x;
        int yDiff = y - this.y;
        int zDiff = z - this.z;
        return calculate(xDiff, yDiff, zDiff);
    }

    @Override
    public String toString() {
        return String.format(
                "GoalBlock{x=%s,y=%s,z=%s}",
                SettingsUtil.maybeCensor(x),
                SettingsUtil.maybeCensor(y),
                SettingsUtil.maybeCensor(z)
        );
    }

    /**
     * @return The position of this goal as a {@link BlockPos}
     */
    @Override
    public BlockPos getGoalPos() {
        return new BlockPos(x, y, z);
    }

    public static double calculate(double xDiff, int yDiff, double zDiff) {
        double heuristic = 0;

        heuristic += GoalYLevel.calculate(0, yDiff);

        heuristic += GoalXZ.calculate(xDiff, zDiff);
        return heuristic;
    }
}
