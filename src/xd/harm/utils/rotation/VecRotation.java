




package xd.harm.utils.rotation;

public class VecRotation {
    private float yaw;
    private float pitch;

    public boolean equals(VecRotation target) {
        return target.yaw == this.yaw && target.pitch == this.pitch;
    }

    public String toString() {
        return "yaw=" + this.yaw + " pitch=" + this.pitch;
    }

    public VecRotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public float getYaw() {
        return this.yaw;
    }

    public float getPitch() {
        return this.pitch;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }
}
