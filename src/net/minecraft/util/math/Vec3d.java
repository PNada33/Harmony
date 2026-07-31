package net.minecraft.util.math;

import javax.annotation.Nullable;
import net.minecraft.entity.Entity;

public class Vec3d {
    public static final Vec3d ZERO = new Vec3d(0.0D, 0.0D, 0.0D);
    public double xCoord;
    public double yCoord;
    public double zCoord;

    public Vec3d(double x, double y, double z) {
        if (x == -0.0D) {
            x = 0.0D;
        }
        if (y == -0.0D) {
            y = 0.0D;
        }
        if (z == -0.0D) {
            z = 0.0D;
        }
        this.xCoord = x;
        this.yCoord = y;
        this.zCoord = z;
    }

    public Vec3d(Vec3i vector) {
        this((double) vector.getX(), (double) vector.getY(), (double) vector.getZ());
    }

    public Vec3d subtractReverse(Vec3d vec) {
        return new Vec3d(vec.xCoord - this.xCoord, vec.yCoord - this.yCoord, vec.zCoord - this.zCoord);
    }

    public double getDistanceAtEyeByVec(Entity self, double x, double y, double z) {
        double d0 = this.xCoord - x;
        double d1 = this.yCoord + (self == null ? 0.0F : self.getEyeHeight()) - y;
        double d2 = this.zCoord - z;
        return (double) MathHelper.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
    }

    public Vec3d normalize() {
        double d0 = (double) MathHelper.sqrt(this.xCoord * this.xCoord + this.yCoord * this.yCoord + this.zCoord * this.zCoord);
        return d0 < 1.0E-4D ? ZERO : new Vec3d(this.xCoord / d0, this.yCoord / d0, this.zCoord / d0);
    }

    public double dotProduct(Vec3d vec) {
        return this.xCoord * vec.xCoord + this.yCoord * vec.yCoord + this.zCoord * vec.zCoord;
    }

    public Vec3d crossProduct(Vec3d vec) {
        return new Vec3d(this.yCoord * vec.zCoord - this.zCoord * vec.yCoord,
                this.zCoord * vec.xCoord - this.xCoord * vec.zCoord,
                this.xCoord * vec.yCoord - this.yCoord * vec.xCoord);
    }

    public Vec3d subtract(Vec3d vec) {
        return this.subtract(vec.xCoord, vec.yCoord, vec.zCoord);
    }

    public Vec3d subtract(double x, double y, double z) {
        return this.addVector(-x, -y, -z);
    }

    public Vec3d add(Vec3d vec) {
        return this.addVector(vec.xCoord, vec.yCoord, vec.zCoord);
    }

    public Vec3d addVector(double x, double y, double z) {
        return new Vec3d(this.xCoord + x, this.yCoord + y, this.zCoord + z);
    }

    public double distanceTo(Vec3d vec) {
        double d0 = vec.xCoord - this.xCoord;
        double d1 = vec.yCoord - this.yCoord;
        double d2 = vec.zCoord - this.zCoord;
        return (double) MathHelper.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
    }

    public double distanceXZTo(Vec3d vec) {
        double d0 = vec.xCoord - this.xCoord;
        double d1 = vec.zCoord - this.zCoord;
        return (double) MathHelper.sqrt(d0 * d0 + d1 * d1);
    }

    public double squareDistanceTo(Vec3d vec) {
        double d0 = vec.xCoord - this.xCoord;
        double d1 = vec.yCoord - this.yCoord;
        double d2 = vec.zCoord - this.zCoord;
        return d0 * d0 + d1 * d1 + d2 * d2;
    }

    public double squareDistanceTo(double xIn, double yIn, double zIn) {
        double d0 = xIn - this.xCoord;
        double d1 = yIn - this.yCoord;
        double d2 = zIn - this.zCoord;
        return d0 * d0 + d1 * d1 + d2 * d2;
    }

    public Vec3d scale(double scale) {
        return new Vec3d(this.xCoord * scale, this.yCoord * scale, this.zCoord * scale);
    }

    public Vec3i scaled(double scale) {
        return new Vec3i(this.xCoord * scale, this.yCoord * scale, this.zCoord * scale);
    }

    public double lengthVector() {
        return (double) MathHelper.sqrt(this.xCoord * this.xCoord + this.yCoord * this.yCoord + this.zCoord * this.zCoord);
    }

    public double lengthSquared() {
        return this.xCoord * this.xCoord + this.yCoord * this.yCoord + this.zCoord * this.zCoord;
    }

    @Nullable
    public Vec3d getIntermediateWithXValue(Vec3d vec, double x) {
        double d0 = vec.xCoord - this.xCoord;
        double d1 = vec.yCoord - this.yCoord;
        double d2 = vec.zCoord - this.zCoord;
        if (d0 * d0 < 1.0E-7D) {
            return null;
        }
        double d3 = (x - this.xCoord) / d0;
        return d3 >= 0.0D && d3 <= 1.0D ? new Vec3d(this.xCoord + d0 * d3, this.yCoord + d1 * d3, this.zCoord + d2 * d3) : null;
    }

    @Nullable
    public Vec3d getIntermediateWithYValue(Vec3d vec, double y) {
        double d0 = vec.xCoord - this.xCoord;
        double d1 = vec.yCoord - this.yCoord;
        double d2 = vec.zCoord - this.zCoord;
        if (d1 * d1 < 1.0E-7D) {
            return null;
        }
        double d3 = (y - this.yCoord) / d1;
        return d3 >= 0.0D && d3 <= 1.0D ? new Vec3d(this.xCoord + d0 * d3, this.yCoord + d1 * d3, this.zCoord + d2 * d3) : null;
    }

    @Nullable
    public Vec3d getIntermediateWithZValue(Vec3d vec, double z) {
        double d0 = vec.xCoord - this.xCoord;
        double d1 = vec.yCoord - this.yCoord;
        double d2 = vec.zCoord - this.zCoord;
        if (d2 * d2 < 1.0E-7D) {
            return null;
        }
        double d3 = (z - this.zCoord) / d2;
        return d3 >= 0.0D && d3 <= 1.0D ? new Vec3d(this.xCoord + d0 * d3, this.yCoord + d1 * d3, this.zCoord + d2 * d3) : null;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Vec3d)) {
            return false;
        }
        Vec3d vec3d = (Vec3d) other;
        if (Double.compare(vec3d.xCoord, this.xCoord) != 0) {
            return false;
        }
        if (Double.compare(vec3d.yCoord, this.yCoord) != 0) {
            return false;
        }
        return Double.compare(vec3d.zCoord, this.zCoord) == 0;
    }

    public void Vec3ds(double x, double y, double z) {
        if (x == -0.0D) {
            x = 0.0D;
        }
        if (y == -0.0D) {
            y = 0.0D;
        }
        if (z == -0.0D) {
            z = 0.0D;
        }
        this.xCoord = x;
        this.yCoord = y;
        this.zCoord = z;
    }

    @Override
    public int hashCode() {
        long j = Double.doubleToLongBits(this.xCoord);
        int i = (int) (j ^ j >>> 32);
        j = Double.doubleToLongBits(this.yCoord);
        i = 31 * i + (int) (j ^ j >>> 32);
        j = Double.doubleToLongBits(this.zCoord);
        i = 31 * i + (int) (j ^ j >>> 32);
        return i;
    }

    @Override
    public String toString() {
        return "(" + this.xCoord + ", " + this.yCoord + ", " + this.zCoord + ")";
    }

    public Vec3d rotatePitch(float pitch) {
        float f = MathHelper.cos(pitch);
        float f1 = MathHelper.sin(pitch);
        double d0 = this.xCoord;
        double d1 = this.yCoord * (double) f + this.zCoord * (double) f1;
        double d2 = this.zCoord * (double) f - this.yCoord * (double) f1;
        return new Vec3d(d0, d1, d2);
    }

    public Vec3d rotateYaw(float yaw) {
        float f = MathHelper.cos(yaw);
        float f1 = MathHelper.sin(yaw);
        double d0 = this.xCoord * (double) f + this.zCoord * (double) f1;
        double d1 = this.yCoord;
        double d2 = this.zCoord * (double) f - this.xCoord * (double) f1;
        return new Vec3d(d0, d1, d2);
    }

    public static Vec3d fromPitchYawVector(Vec2f vec) {
        return fromPitchYaw(vec.x, vec.y);
    }

    public static Vec3d fromPitchYaw(float pitch, float yaw) {
        float f = MathHelper.cos(-yaw * ((float) Math.PI / 180F) - (float) Math.PI);
        float f1 = MathHelper.sin(-yaw * ((float) Math.PI / 180F) - (float) Math.PI);
        float f2 = -MathHelper.cos(-pitch * ((float) Math.PI / 180F));
        float f3 = MathHelper.sin(-pitch * ((float) Math.PI / 180F));
        return new Vec3d((double) (f1 * f2), (double) f3, (double) (f * f2));
    }

    public Vec3d addr(Vec3d vec) {
        return this.addVector(vec.xCoord, vec.yCoord, vec.zCoord);
    }

    public BlockPos addr(double d, double e, double f) {
        return new BlockPos(this.xCoord + d, this.yCoord + e, this.zCoord + f);
    }
}
