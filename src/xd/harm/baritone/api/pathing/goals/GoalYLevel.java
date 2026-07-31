

package xd.harm.baritone.api.pathing.goals;

import xd.harm.baritone.api.pathing.movement.ActionCosts;
import xd.harm.baritone.api.utils.SettingsUtil;

/**
 * Useful for mining (getting to diamond / iron level)
 *
 * @author leijurv
 */
public class GoalYLevel implements Goal, ActionCosts {

    /**
     * The target Y level
     */
    public final int level;

    public GoalYLevel(int level) {
        this.level = level;
    }

    @Override
    public boolean isInGoal(int x, int y, int z) {
        return y == level;
    }

    @Override
    public double heuristic(int x, int y, int z) {
        return calculate(level, y);
    }

    public static double calculate(int goalY, int currentY) {
        if (currentY > goalY) {
            return FALL_N_BLOCKS_COST[2] / 2 * (currentY - goalY);
        }
        if (currentY < goalY) {
            return (goalY - currentY) * JUMP_ONE_BLOCK_COST;
        }
        return 0;
    }

    @Override
    public String toString() {
        return String.format(
                "GoalYLevel{y=%s}",
                SettingsUtil.maybeCensor(level)
        );
    }
}
