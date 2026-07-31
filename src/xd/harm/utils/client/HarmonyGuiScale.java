package xd.harm.utils.client;

import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;

public final class HarmonyGuiScale {
    public static final int FIXED_GUI_SCALE = 2;

    private static final String HARMONY_PACKAGE = "xd.harm.";

    private HarmonyGuiScale() {
    }

    public static boolean isHarmonyScreen(Screen screen) {
        return screen != null && screen.getClass().getName().startsWith(HARMONY_PACKAGE);
    }

    public static int getScreenWidth(Screen screen, MainWindow window) {
        return isHarmonyScreen(screen) ? getFixedScaledWidth(window) : window.getScaledWidth();
    }

    public static int getScreenHeight(Screen screen, MainWindow window) {
        return isHarmonyScreen(screen) ? getFixedScaledHeight(window) : window.getScaledHeight();
    }

    public static int getScreenWidth(Minecraft minecraft, Screen screen) {
        return isHarmonyScreen(screen) ? getFixedScaledWidth(minecraft.getMainWindow()) : getVanillaScaledWidth(minecraft);
    }

    public static int getScreenHeight(Minecraft minecraft, Screen screen) {
        return isHarmonyScreen(screen) ? getFixedScaledHeight(minecraft.getMainWindow()) : getVanillaScaledHeight(minecraft);
    }

    public static int getFixedScaledWidth(MainWindow window) {
        return getScaledWidth(window, FIXED_GUI_SCALE);
    }

    public static int getFixedScaledHeight(MainWindow window) {
        return getScaledHeight(window, FIXED_GUI_SCALE);
    }

    public static int getVanillaScaledWidth(Minecraft minecraft) {
        return getScaledWidth(minecraft.getMainWindow(), getVanillaGuiScale(minecraft));
    }

    public static int getVanillaScaledHeight(Minecraft minecraft) {
        return getScaledHeight(minecraft.getMainWindow(), getVanillaGuiScale(minecraft));
    }

    public static int getFixedMouseX(Minecraft minecraft) {
        return (int) getScaledMouseX(minecraft, null, minecraft.mouseHelper.getMouseX(), true);
    }

    public static int getFixedMouseY(Minecraft minecraft) {
        return (int) getScaledMouseY(minecraft, null, minecraft.mouseHelper.getMouseY(), true);
    }

    public static double getScaledMouseX(Minecraft minecraft, Screen screen, double mouseX) {
        return getScaledMouseX(minecraft, screen, mouseX, false);
    }

    public static double getScaledMouseY(Minecraft minecraft, Screen screen, double mouseY) {
        return getScaledMouseY(minecraft, screen, mouseY, false);
    }

    public static double getScaledMouseDeltaX(Minecraft minecraft, Screen screen, double deltaX) {
        return deltaX * getMouseScaleX(minecraft, screen, false);
    }

    public static double getScaledMouseDeltaY(Minecraft minecraft, Screen screen, double deltaY) {
        return deltaY * getMouseScaleY(minecraft, screen, false);
    }

    private static double getScaledMouseX(Minecraft minecraft, Screen screen, double mouseX, boolean forceFixed) {
        return mouseX * getMouseScaleX(minecraft, screen, forceFixed);
    }

    private static double getScaledMouseY(Minecraft minecraft, Screen screen, double mouseY, boolean forceFixed) {
        return mouseY * getMouseScaleY(minecraft, screen, forceFixed);
    }

    private static double getMouseScaleX(Minecraft minecraft, Screen screen, boolean forceFixed) {
        MainWindow window = minecraft.getMainWindow();
        int scaledWidth = forceFixed || isHarmonyScreen(screen) ? getFixedScaledWidth(window) : getVanillaScaledWidth(minecraft);
        return (double) scaledWidth / (double) window.getWidth();
    }

    private static double getMouseScaleY(Minecraft minecraft, Screen screen, boolean forceFixed) {
        MainWindow window = minecraft.getMainWindow();
        int scaledHeight = forceFixed || isHarmonyScreen(screen) ? getFixedScaledHeight(window) : getVanillaScaledHeight(minecraft);
        return (double) scaledHeight / (double) window.getHeight();
    }

    private static int getVanillaGuiScale(Minecraft minecraft) {
        return minecraft.getMainWindow().calcGuiScale(minecraft.gameSettings.guiScale, minecraft.getForceUnicodeFont());
    }

    private static int getScaledWidth(MainWindow window, int guiScale) {
        return Math.max(1, (int) Math.ceil((double) window.getFramebufferWidth() / (double) Math.max(1, guiScale)));
    }

    private static int getScaledHeight(MainWindow window, int guiScale) {
        return Math.max(1, (int) Math.ceil((double) window.getFramebufferHeight() / (double) Math.max(1, guiScale)));
    }
}
