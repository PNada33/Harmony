package xd.harm.utils.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MainWindow;

public class ScaledResolution {
    private final MainWindow window;

    public ScaledResolution(Minecraft mc) {
        this.window = mc.getMainWindow();
    }

    public int getScaledWidth() {
        return window.getScaledWidth();
    }

    public int getScaledHeight() {
        return window.getScaledHeight();
    }

    public static int getScaleFactor() {
        MainWindow window = Minecraft.getInstance().getMainWindow();
        double factor = window.getGuiScaleFactor();
        if (factor < 1.0) {
            factor = window.getScaleFactor();
        }
        if (factor < 1.0) {
            factor = 1.0;
        }
        return (int) Math.round(factor);
    }

    public static float lpSCFactor() {
        MainWindow window = Minecraft.getInstance().getMainWindow();
        double factor = window.getGuiScaleFactor();
        if (factor < 1.0) {
            factor = window.getScaleFactor();
        }
        if (factor < 1.0) {
            factor = 1.0;
        }
        return (float) factor;
    }
}
