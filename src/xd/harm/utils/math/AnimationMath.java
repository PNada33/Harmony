package xd.harm.utils.math;

import net.minecraft.util.math.vector.Vector3d;

public class AnimationMath {

    public static Vector3d fast(Vector3d from, Vector3d to, float speed) {
        double deltaTime = 0.016;
        double factor = 1.0 - Math.exp(-speed * deltaTime);

        double x = lerp(from.x, to.x, factor);
        double y = lerp(from.y, to.y, factor);
        double z = lerp(from.z, to.z, factor);

        return new Vector3d(x, y, z);
    }

    public static double fast(double from, double to, float speed) {
        double deltaTime = 0.016;
        double factor = 1.0 - Math.exp(-speed * deltaTime);
        return lerp(from, to, factor);
    }

    public static float fast(float from, float to, float speed) {
        double deltaTime = 0.016;
        double factor = 1.0 - Math.exp(-speed * deltaTime);
        return (float) lerp(from, to, factor);
    }

    private static double lerp(double start, double end, double factor) {
        return start + (end - start) * factor;
    }
}