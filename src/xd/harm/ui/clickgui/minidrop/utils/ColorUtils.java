package xd.harm.ui.clickgui.minidrop.utils;

import java.awt.Color;

/**
 * Утилиты цветов для MiniDropDown ClickGui (аналог Quantum ColorUtils)
 */
public class ColorUtils {

    private static final int OCEANIC_FIRST = new Color(5, 63, 111).getRGB();
    private static final int OCEANIC_SECOND = new Color(133, 183, 246).getRGB();

    public static int rgba(int r, int g, int b, int a) {
        return a << 24 | r << 16 | g << 8 | b;
    }

    public static int rgb(int r, int g, int b) {
        return 255 << 24 | r << 16 | g << 8 | b;
    }

    public static int getClickGuiColor(int index) {
        return gradient(OCEANIC_FIRST, OCEANIC_SECOND, index * 16, 10);
    }

    public static int getClickGuiColor(int index, int alpha) {
        return setAlpha(gradient(OCEANIC_FIRST, OCEANIC_SECOND, index * 16, 10), alpha);
    }

    /**
     * Анимированный градиент — точно как в Quantum
     */
    public static int gradient(int start, int end, int index, int speed) {
        int angle = (int) ((System.currentTimeMillis() / speed + index) % 360);
        angle = (angle > 180 ? 360 - angle : angle) + 180;
        int color = interpolate(start, end, Math.min(1f, Math.max(0f, angle / 180f - 1)));
        float[] hs = rgba(color);
        float[] hsb = Color.RGBtoHSB((int) (hs[0] * 255), (int) (hs[1] * 255), (int) (hs[2] * 255), null);

        hsb[1] *= 1.5F;
        hsb[1] = Math.min(hsb[1], 1.0f);

        return Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
    }

    /**
     * Интерполяция цветов — совместимая с Quantum.
     * Quantum: MathUtil.interpolate(current, old, scale) = old + (current - old) * scale
     * interpolate(start, end, value): value=0 → end, value=1 → start
     */
    public static int interpolate(int start, int end, float value) {
        float[] startColor = rgba(start);
        float[] endColor = rgba(end);

        // Quantum-совместимая интерполяция: value=0 → end, value=1 → start
        return rgba(
                (int) (endColor[0] * 255 + (startColor[0] * 255 - endColor[0] * 255) * value),
                (int) (endColor[1] * 255 + (startColor[1] * 255 - endColor[1] * 255) * value),
                (int) (endColor[2] * 255 + (startColor[2] * 255 - endColor[2] * 255) * value),
                (int) (endColor[3] * 255 + (startColor[3] * 255 - endColor[3] * 255) * value));
    }

    public static int setAlpha(int color, int alpha) {
        return (color & 0x00ffffff) | (alpha << 24);
    }

    public static float[] rgba(final int color) {
        return new float[]{
                (color >> 16 & 0xFF) / 255f,
                (color >> 8 & 0xFF) / 255f,
                (color & 0xFF) / 255f,
                (color >> 24 & 0xFF) / 255f
        };
    }
}
