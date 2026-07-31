package xd.harm.utils.math;

public class TimerHelper {
    private long lastMs;

    public TimerHelper() {
        reset();
    }

    public static TimerHelper TimerHelperReseted() {
        return new TimerHelper();
    }

    public void reset() {
        lastMs = System.currentTimeMillis();
    }

    public long getTime() {
        return System.currentTimeMillis() - lastMs;
    }

    public boolean hasReached(double ms) {
        return getTime() >= ms;
    }
}
