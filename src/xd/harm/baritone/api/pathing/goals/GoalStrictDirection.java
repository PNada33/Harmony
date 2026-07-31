

package xd.harm.baritone.api.pathing.goals;

import xd.harm.baritone.api.utils.SettingsUtil;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

/**
 * Dig a tunnel in a certain direction, but if you have to deviate from the path, go back to where you started
 */
public class GoalStrictDirection implements Goal {

    public final int x;
    public final int y;
    public final int z;
    public final int dx;
    public final int dz;

    public GoalStrictDirection(BlockPos origin, Direction direction) {
        x = origin.getX();
        y = origin.getY();
        z = origin.getZ();
        dx = direction.getXOffset();
        dz = direction.getZOffset();
        if (dx == 0 && dz == 0) {
            throw new IllegalArgumentException(direction + "");
        }
    }

    @Override
    public boolean isInGoal(int x, int y, int z) {
        return false;
    }

    @Override
    public double heuristic(int x, int y, int z) {
        int distanceFromStartInDesiredDirection = (x - this.x) * dx + (z - this.z) * dz;

        int distanceFromStartInIncorrectDirection = Math.abs((x - this.x) * dz) + Math.abs((z - this.z) * dx);

        int verticalDistanceFromStart = Math.abs(y - this.y);

        double heuristic = -distanceFromStartInDesiredDirection * 100;

        heuristic += distanceFromStartInIncorrectDirection * 1000;
        heuristic += verticalDistanceFromStart * 1000;
        return heuristic;
    }

    @Override
    public double heuristic() {
        return Double.NEGATIVE_INFINITY;
    }

    @Override
    public String toString() {
        return String.format(
                "GoalStrictDirection{x=%s, y=%s, z=%s, dx=%s, dz=%s}",
                SettingsUtil.maybeCensor(x),
                SettingsUtil.maybeCensor(y),
                SettingsUtil.maybeCensor(z),
                SettingsUtil.maybeCensor(dx),
                SettingsUtil.maybeCensor(dz)
        );
    }
}
