package xd.harm.utils.client;

import lombok.Getter;

public class TimerUtility {
    @Getter
    public long lastMS = System.currentTimeMillis();

    public static TimerUtility create() {
        return new TimerUtility();
    }

    public void reset() {
        lastMS = System.currentTimeMillis();
    }

    public boolean isReached(long time) {
        return System.currentTimeMillis() - lastMS > time;
    }

    public boolean hasReached(long time) {
        return System.currentTimeMillis() - lastMS > time;
    }

    public void setLastMS(long newValue) {
        lastMS = System.currentTimeMillis() + newValue;
    }

    public void setTime(long time) {
        lastMS = time;
    }

    public long getTime() {
        return System.currentTimeMillis() - lastMS;
    }

    public boolean isRunning() {
        return System.currentTimeMillis() - lastMS <= 0;
    }

    public boolean hasTimeElapsed(long time) {
        return System.currentTimeMillis() - lastMS > time;
    }

    public boolean hasTimeElapsed() {
        return lastMS < System.currentTimeMillis();
    }

    public boolean hasTimeElapsed(long time, boolean reset) {
        if (System.currentTimeMillis() - lastMS > time) {
            if (reset) reset();
            return true;
        }
        return false;
    }

    public boolean hasTimeElapsed2(long time) {
        return System.currentTimeMillis() - this.lastMS > time;
    }
}