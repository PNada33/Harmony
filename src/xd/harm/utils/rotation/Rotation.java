




package xd.harm.utils.rotation;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import xd.harm.utils.client.IMinecraft;

public class Rotation implements IMinecraft {
    private float yaw;
    private float pitch;

    public Rotation(Entity entity) {
        this.yaw = entity.rotationYaw;
        this.pitch = entity.rotationPitch;
    }

    public double getDelta(Rotation targetRotation) {
        double yawDelta = (double)MathHelper.wrapDegrees(targetRotation.getYaw() - this.yaw);
        double pitchDelta = (double)MathHelper.wrapDegrees(targetRotation.getPitch() - this.pitch);
        return Math.hypot(Math.abs(yawDelta), Math.abs(pitchDelta));
    }

    public static Rotation getReal() {
        return new Rotation(FreeLookHandler.getFreeYaw(), FreeLookHandler.getFreePitch());
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public float getYaw() {
        return this.yaw;
    }

    public float getPitch() {
        return this.pitch;
    }

    public Rotation() {
    }

    public Rotation(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }
}
