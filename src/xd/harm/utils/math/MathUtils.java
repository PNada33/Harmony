package xd.harm.utils.math;

import net.minecraft.util.math.MathHelper;

public class MathUtils {
    public static double easeOutCubic(double t) {
        t = clamp01(t);
        double inv = 1.0 - t;
        return 1.0 - inv * inv * inv;
    }

    public static double easeInCircle(double t) {
        t = clamp01(t);
        return 1.0 - Math.sqrt(1.0 - t * t);
    }

    private static double clamp01(double t) {
        return MathHelper.clamp((float) t, 0.0f, 1.0f);
    }
}
