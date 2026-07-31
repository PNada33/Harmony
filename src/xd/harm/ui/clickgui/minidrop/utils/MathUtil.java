package xd.harm.ui.clickgui.minidrop.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

import net.minecraft.util.math.MathHelper;

/**
 * Утилиты для MiniDropDown ClickGui (аналог Quantum MathUtil)
 */
public class MathUtil {

    public static float fast(float end, float start, float multiple) {
        float clamped = MathHelper.clamp((float) (deltaTime() * multiple), 0, 1);
        return (1 - clamped) * end + clamped * start;
    }

    public static boolean isHovered(float mouseX, float mouseY, float x, float y, float width, float height) {
        return mouseX > x && mouseX < x + width && mouseY > y && mouseY < y + height;
    }

    public static double round(double num, double increment) {
        double v = Math.round(num / increment) * increment;
        BigDecimal bd = new BigDecimal(v);
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    private static double deltaTime() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        return mc.debugFPS > 0 ? (1.0 / mc.debugFPS) : 1;
    }
}
