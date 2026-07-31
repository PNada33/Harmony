package xd.harm.utils.math;

import xd.harm.utils.client.IMinecraft;

public final class SensUtils implements IMinecraft {
    public static float getSensitivity(float rot) {
        return getDeltaMouse(rot) * getGCDValue();
    }

    public static float getGCDValue() {
        return (float)((double)getGCD() * 0.15);
    }

    public static float getGCD() {
        float f1;
        return (f1 = (float)(mc.gameSettings.mouseSensitivity * 0.6 + 0.2)) * f1 * f1 * 8.0F;
    }


    public static float correctRotation(float rot) {
        float gcd = SensUtils.getGCDValue();
        rot -= rot % gcd;
        return SensUtils.getFixedRotation(rot);
    }



    public static float getFixedRotation(float rot) {
        return SensUtils.getDeltaMouse(rot) * SensUtils.getGCDValue();
    }


    public static float getDeltaMouse(float delta) {
        return (float)Math.round(delta / getGCDValue());
    }

    private SensUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
