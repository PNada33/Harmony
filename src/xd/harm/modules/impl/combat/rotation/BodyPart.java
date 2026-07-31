package xd.harm.modules.impl.combat.rotation;

public enum BodyPart {
    HEAD(0.0, 1.7, 0.0, 0.15f),
    NECK(0.0, 1.5, 0.0, 0.1f),
    CHEST(0.0, 1.3, 0.0, 0.2f),
    UPPER_CHEST(0.0, 1.4, 0.0, 0.15f),
    LST(0.0, 1.2, 0.0, 0.15f),
    LEFT_SHOULDER(-0.3, 1.45, 0.0, 0.1f),
    RIGHT_SHOULDER(0.3, 1.45, 0.0, 0.1f),
    LEFT_UPPER_ARM(-0.45, 1.3, 0.0, 0.08f),
    RIGHT_UPPER_ARM(0.45, 1.3, 0.0, 0.08f),
    LEFT_ELBOW(-0.5, 1.15, 0.0, 0.07f),
    RIGHT_ELBOW(0.5, 1.15, 0.0, 0.07f),
    LEFT_FOREARM(-0.45, 1.0, 0.0, 0.06f),
    RIGHT_FOREARM(0.45, 1.0, 0.0, 0.06f),
    LEFT_HAND(-0.35, 0.9, 0.0, 0.05f),
    RIGHT_HAND(0.35, 0.9, 0.0, 0.05f),
    LEFT_HIP(-0.15, 0.8, 0.0, 0.08f),
    RIGHT_HIP(0.15, 0.8, 0.0, 0.08f),
    LEFT_THIGH(-0.2, 0.6, 0.0, 0.07f),
    RIGHT_THIGH(0.2, 0.6, 0.0, 0.07f),
    LEFT_KNEE(-0.2, 0.4, 0.0, 0.06f),
    RIGHT_KNEE(0.2, 0.4, 0.0, 0.06f),
    LEFT_LOWER_LEG(-0.2, 0.2, 0.0, 0.05f),
    RIGHT_LOWER_LEG(0.2, 0.2, 0.0, 0.05f),
    LEFT_FOOT(-0.2, 0.05, 0.0, 0.04f),
    RIGHT_FOOT(0.2, 0.05, 0.0, 0.04f);

    public final double xOffset;
    public final double yOffset;
    public final double zOffset;
    public final float weight;

    BodyPart(double x, double y, double z, float w) {
        this.xOffset = x;
        this.yOffset = y;
        this.zOffset = z;
        this.weight = w;
    }
}
