package xd.harm.modules.models.dog;

import lombok.Getter;
import xd.harm.utils.animations.easing.Easing;
import xd.harm.utils.client.TimerUtility;

@Getter
public class Animation {
    private final TimerUtility timer = new TimerUtility();
    private int speed;
    private double size = 1;
    @Getter
    private boolean forward;
    private Easing easing;

    public boolean finished(boolean forward) {
        return timer.hasTimeElapsed(speed) && (forward ? this.forward : !this.forward);
    }

    public boolean finished() {
        return timer.hasTimeElapsed(speed) && this.forward;
    }

    public Animation setForward(boolean forward) {
        if (this.forward != forward) {
            this.forward = forward;
            timer.setTime((long) (System.currentTimeMillis() - (size - Math.min(size, timer.getTime()))));
        }
        return this;
    }

    public Animation finish() {
        timer.setTime((long) (System.currentTimeMillis()-speed));
        return this;
    }

    public Animation setEasing(Easing easing) {
        this.easing = easing;
        return this;
    }

    public Animation setSpeed(int speed) {
        this.speed = speed;
        return this;
    }

    public Animation setSize(float size) {
        this.size = size;
        return this;
    }

    public float getLinear() {
        if (forward) {
            if (timer.hasTimeElapsed(speed)) {
                return (float) size;
            }

            return (float) (timer.getTime() / (double) speed * size);
        } else {
            if (timer.hasTimeElapsed(speed)) {
                return 0.0f;
            }

            return (float) ((1 - timer.getTime() / (double) speed) * size);
        }
    }

    public float get() {
        if (forward) {
            if (timer.hasTimeElapsed(speed)) {
                return (float) size;
            }

            return (float) (easing.apply(timer.getTime() / (double) speed) * size);
        } else {
            if (timer.hasTimeElapsed(speed)) {
                return 0.0f;
            }

            return (float) ((1 - easing.apply(timer.getTime() / (double) speed)) * size);
        }
    }

    public float reversed() {
        return 1-get();
    }

    public void reset() {
        timer.reset();
    }
}