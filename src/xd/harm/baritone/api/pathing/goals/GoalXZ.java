

package xd.harm.baritone.api.pathing.goals;

import xd.harm.baritone.api.BaritoneAPI;
import xd.harm.baritone.api.utils.BetterBlockPos;
import xd.harm.baritone.api.utils.SettingsUtil;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;

/**
 * Useful for long-range goals that don't have a specific Y level.
 *
 * @author leijurv
 */
public class GoalXZ implements Goal {

    private static final double SQRT_2 = Math.sqrt(2);

    /**
     * The X block position of this goal
     */
    private final int x;

    /**
     * The Z block position of this goal
     */
    private final int z;

    public GoalXZ(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public GoalXZ(BetterBlockPos pos) {
        this.x = pos.x;
        this.z = pos.z;
    }

    @Override
    public boolean isInGoal(int x, int y, int z) {
        return x == this.x && z == this.z;
    }

    @Override
    public double heuristic(int x, int y, int z) {
        int xDiff = x - this.x;
        int zDiff = z - this.z;
        return calculate(xDiff, zDiff);
    }

    @Override
    public String toString() {
        return String.format(
                "GoalXZ{x=%s,z=%s}",
                SettingsUtil.maybeCensor(x),
                SettingsUtil.maybeCensor(z)
        );
    }

    public static double calculate(double xDiff, double zDiff) {
        double x = Math.abs(xDiff);
        double z = Math.abs(zDiff);
        double straight;
        double diagonal;
        if (x < z) {
            straight = z - x;
            diagonal = x;
        } else {
            straight = x - z;
            diagonal = z;
        }
        diagonal *= SQRT_2;
        return (diagonal + straight) * BaritoneAPI.getSettings().costHeuristic.value;
    }

    public static GoalXZ fromDirection(Vector3d origin, float yaw, double distance) {
        float theta = (float) Math.toRadians(yaw);
        double x = origin.x - MathHelper.sin(theta) * distance;
        double z = origin.z + MathHelper.cos(theta) * distance;
        return new GoalXZ(MathHelper.floor(x), MathHelper.floor(z));
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }
}
