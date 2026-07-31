package xd.harm;

public final class HarmonyStartupException extends RuntimeException {

    public HarmonyStartupException(String message) {
        super(message, null, false, false);
    }

    public HarmonyStartupException(String message, Throwable cause) {
        super(message, cause, false, false);
    }
}
