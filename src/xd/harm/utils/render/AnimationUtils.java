package xd.harm.utils.render;

import net.minecraft.client.Minecraft;
import net.minecraft.util.math.MathHelper;

public class AnimationUtils {
    public long mc;
    public float anim;
    public float to;
    public float speed;

    public AnimationUtils(float anim, float to, float speed) {
        this.anim = anim;
        this.to = to;
        this.speed = speed;
        this.mc = System.nanoTime();
    }

    public float getAnim() {
        if (this.to == this.anim) {
            this.mc = System.nanoTime();
            return this.anim;
        }
        if (Math.abs(this.to - this.anim) < 1.0E-4F) {
            this.setAnim(this.to);
            return this.anim;
        }
        float msFinished = (float) (System.nanoTime() - this.mc) / 1000000.0F;
        if (msFinished >= 1000.0F / getRefreshRateSafe()) {
            this.anim = lerp(this.anim, this.to, Math.min(this.speed * msFinished * 0.125F, 1.0F));
            this.mc = System.nanoTime();
        }
        return this.anim;
    }

    public float getAngleAnim() {
        if (Math.abs(this.to - this.anim) < 1.0E-4F) {
            this.setAnim(this.to);
            return this.anim;
        }
        float msFinished = (float) (System.nanoTime() - this.mc) / 1000000.0F;
        if (msFinished >= 1000.0F / getRefreshRateSafe()) {
            this.anim = (float) this.lerpAngle(this.anim, this.to, Math.min(this.speed * msFinished * 0.125F, 1.0F));
            this.mc = System.nanoTime();
        }
        return MathHelper.wrapDegrees(this.anim);
    }

    public void setAnim(float anim) {
        this.anim = anim;
        this.mc = System.nanoTime();
    }

    double lerpAngle(float start, float end, float amount) {
        float minAngle = (end - start + 180.0F) % 360.0F - 180.0F;
        return minAngle * amount + start;
    }

    private static float lerp(float start, float end, float amount) {
        return start + (end - start) * amount;
    }

    private static float getRefreshRateSafe() {
        int rate = 0;
        try {
            rate = Minecraft.getInstance().getMainWindow().getRefreshRate();
        } catch (Throwable ignored) {
        }
        return rate > 0 ? rate : 60.0F;
    }
}
