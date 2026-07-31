package xd.harm.utils.render.color;

import xd.harm.modules.impl.render.Theme;
import lombok.experimental.UtilityClass;
import net.minecraft.util.math.MathHelper;

import com.mojang.blaze3d.systems.RenderSystem;

import xd.harm.utils.math.MathUtil;

import java.awt.Color;

@UtilityClass
public class ColorUtils {
    public final int green = new Color(64, 255, 64).getRGB();
    public final int yellow = new Color(255, 255, 64).getRGB();
    public final int orange = new Color(255, 128, 32).getRGB();
    public final int blue = new Color(0, 180, 255).getRGB();
    public final int red = new Color(255, 64, 64).getRGB();
    public static int rgb(int r, int g, int b) {
        return 255 << 24 | r << 16 | g << 8 | b;
    }

    public int multDark(int c, float brpc) {
        return getColor((float) red(c) * brpc, (float) green(c) * brpc, (float) blue(c) * brpc, (float) alpha(c));
    }

    public static int getColor(int index) {
        return Theme.MainColor(index);
    }

    public static int getColor3(int index) {
        return ColorUtils.rgb(45,45,45);
    }

    public static int hsb(float hue, float saturation, float brightness) {
        return Color.HSBtoRGB(hue, saturation, brightness);
    }

    public static float[] getRGBAf(int c) {
        return new float[]{(float) red(c) / 255.F, (float) green(c) / 255.F, (float) blue(c) / 255.F, (float) alpha(c) / 255.F};
    }

    public static class IntColor {
        public static float[] rgb(final int color) {
            return new float[]{
                    (color >> 16 & 0xFF) / 255f,
                    (color >> 8 & 0xFF) / 255f,
                    (color & 0xFF) / 255f,
                    (color >> 24 & 0xFF) / 255f
            };
        }

        public static int rgba(final int r, final int g, final int b, final int a) {
            return a << 24 | r << 16 | g << 8 | b;
        }

        public static int rgb(int r, int g, int b) {
            return 255 << 24 | r << 16 | g << 8 | b;
        }

        public static int getRed(final int hex) {
            return hex >> 16 & 255;
        }

        public static int getGreen(final int hex) {
            return hex >> 8 & 255;
        }

        public static int getBlue(final int hex) {
            return hex & 255;
        }

        public static int getAlpha(final int hex) {
            return hex >> 24 & 255;
        }
    }

    public static int darker(int color, float factor) {
        int r = Math.max(0, (int)(getRed(color) * (1 - factor)));
        int g = Math.max(0, (int)(getGreen(color) * (1 - factor)));
        int b = Math.max(0, (int)(getBlue(color) * (1 - factor)));
        return rgba(r, g, b, getAlpha(color));
    }

    public static int brighter(int color, float factor) {
        int r = Math.min(255, (int)(getRed(color) + (255 - getRed(color)) * factor));
        int g = Math.min(255, (int)(getGreen(color) + (255 - getGreen(color)) * factor));
        int b = Math.min(255, (int)(getBlue(color) + (255 - getBlue(color)) * factor));
        return rgba(r, g, b, getAlpha(color));
    }

    public Color random(int index) {
        int angle = (int) ((System.currentTimeMillis() / 10 + index * 5) % 360);
        float hue = angle / 360f;
        float saturation = 0.7f;
        float brightness = 0.9f;
        return new Color(Color.HSBtoRGB(hue, saturation, brightness));
    }

    public int overCol(int c1, int c2, float pc01) {
        return getColor((float) red(c1) * (1 - pc01) + (float) red(c2) * pc01, (float) green(c1) * (1 - pc01) + (float) green(c2) * pc01, (float) blue(c1) * (1 - pc01) + (float) blue(c2) * pc01, (float) alpha(c1) * (1 - pc01) + (float) alpha(c2) * pc01);
    }

    public int overCol(int c1, int c2) {
        return overCol(c1, c2, 0.5f);
    }

    public static int rgba(int r, int g, int b, int a) {
        return a << 24 | r << 16 | g << 8 | b;
    }

    public static void setAlphaColor(final int color, final float alpha) {
        final float red = (float) (color >> 16 & 255) / 255.0F;
        final float green = (float) (color >> 8 & 255) / 255.0F;
        final float blue = (float) (color & 255) / 255.0F;
        RenderSystem.color4f(red, green, blue, alpha);
    }

    public int red(int c) {
        return c >> 16 & 0xFF;
    }

    public int green(int c) {
        return c >> 8 & 0xFF;
    }

    public int blue(int c) {
        return c & 0xFF;
    }

    public int alpha(int c) {
        return c >> 24 & 0xFF;
    }

    public float redf(int c) {
        return (float) red(c) / 255.F;
    }

    public float greenf(int c) {
        return (float) green(c) / 255.F;
    }

    public float bluef(int c) {
        return (float) blue(c) / 255.F;
    }

    public float alphaf(int c) {
        return (float) alpha(c) / 255.F;
    }

    public static void setColor(int color) {
        setAlphaColor(color, (float) (color >> 24 & 255) / 255.0F);
    }

    public static int toColor(String hexColor) {
        int argb = Integer.parseInt(hexColor.substring(1), 16);
        return setAlpha(argb, 255);
    }

    public static int setAlpha(int color, int alpha) {
        return (color & 0x00ffffff) | (alpha << 24);
    }

    public static float[] rgba(final int color) {
        return new float[] {
                (color >> 16 & 0xFF) / 255f,
                (color >> 8 & 0xFF) / 255f,
                (color & 0xFF) / 255f,
                (color >> 24 & 0xFF) / 255f
        };
    }

    public static int reAlphaInt(final int color, final int alpha) {
        return (MathHelper.clamp(alpha, 0, 255) << 24) | (color & 16777215);
    }

    public int getColor(double d, double e, double f, double g) {
        return new Color((int) d, (int) e, (int) f, (int) g).getRGB();
    }

    public int getColor(float r, float g, float b, float a) {
        return new Color((int) r, (int) g, (int) b, (int) a).getRGB();
    }

    public static int interpolateColor(int color1, int color2, float amount) {
        amount = Math.min(1, Math.max(0, amount));

        int red1 = getRed(color1);
        int green1 = getGreen(color1);
        int blue1 = getBlue(color1);
        int alpha1 = getAlpha(color1);

        int red2 = getRed(color2);
        int green2 = getGreen(color2);
        int blue2 = getBlue(color2);
        int alpha2 = getAlpha(color2);

        int interpolatedRed = interpolateInt(red1, red2, amount);
        int interpolatedGreen = interpolateInt(green1, green2, amount);
        int interpolatedBlue = interpolateInt(blue1, blue2, amount);
        int interpolatedAlpha = interpolateInt(alpha1, alpha2, amount);

        return (interpolatedAlpha << 24) | (interpolatedRed << 16) | (interpolatedGreen << 8) | interpolatedBlue;
    }

    public static int getOverallColorFrom(int color1, int color2) {
        int red1 = getRed(color1);
        int green1 = getGreen(color1);
        int blue1 = getBlue(color1);
        int alpha1 = getAlpha(color1);
        int red2 = getRed(color2);
        int green2 = getGreen(color2);
        int blue2 = getBlue(color2);
        int alpha2 = getAlpha(color2);
        int finalRed = (red1 + red2) / 2;
        int finalGreen = (green1 + green2) / 2;
        int finalBlue = (blue1 + blue2) / 2;
        int finalAlpha = (alpha1 + alpha2) / 2;
        return rgba(finalRed, finalGreen, finalBlue, finalAlpha);
    }

    public static int getOverallColorFrom(int color1, int color2, float percentTo2) {
        int finalRed = (int) MathHelper.lerp(percentTo2, (float) (color1 >> 16 & 255), (float) (color2 >> 16 & 255));
        int finalGreen = (int) MathHelper.lerp(percentTo2, (float) (color1 >> 8 & 255), (float) (color2 >> 8 & 255));
        int finalBlue = (int) MathHelper.lerp(percentTo2, (float) (color1 & 255), (float) (color2 & 255));
        int finalAlpha = (int) MathHelper.lerp(percentTo2, (float) (color1 >> 24 & 255), (float) (color2 >> 24 & 255));
        return rgba(finalRed, finalGreen, finalBlue, finalAlpha);
    }

    public static int toDark(int color, float dark) {
        return rgba(
                (int) ((float) getRed(color) * dark),
                (int) ((float) getGreen(color) * dark),
                (int) ((float) getBlue(color) * dark),
                getAlpha(color)
        );
    }

    public static int fadeColor(int color1, int color2, float speed) {
        if (speed <= 0.0f) {
            return color1;
        }
        float t = (float) ((Math.sin(System.currentTimeMillis() / (double) (200.0f / speed)) + 1.0f) * 0.5f);
        return getOverallColorFrom(color1, color2, t);
    }

    public static Color getProgressColor(float val) {
        float[] fractions = new float[]{0.0F, 0.15F, 0.55F, 0.7F, 0.9F};
        Color[] colors = new Color[]{new Color(133, 0, 0), Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN};
        return val >= 0.0F ? blendColors(fractions, colors, val).brighter() : colors[0];
    }

    private static Color blendColors(float[] fractions, Color[] colors, float progress) {
        if (fractions == null || colors == null) {
            throw new IllegalArgumentException("Fractions and colors can't be null");
        }
        if (fractions.length != colors.length) {
            throw new IllegalArgumentException("Fractions and colors must have equal number of elements");
        }
        int[] indices = getFractionIndices(fractions, progress);
        float[] range = new float[]{fractions[indices[0]], fractions[indices[1]]};
        Color[] colorRange = new Color[]{colors[indices[0]], colors[indices[1]]};
        float max = range[1] - range[0];
        float value = progress - range[0];
        float weight = max == 0.0f ? 0.0f : value / max;
        return blend(colorRange[0], colorRange[1], 1.0 - weight);
    }

    private static int[] getFractionIndices(float[] fractions, float progress) {
        int startPoint = 0;
        while (startPoint < fractions.length && fractions[startPoint] <= progress) {
            startPoint++;
        }
        if (startPoint >= fractions.length) {
            startPoint = fractions.length - 1;
        }
        return new int[]{Math.max(0, startPoint - 1), startPoint};
    }

    private static Color blend(Color color1, Color color2, double ratio) {
        float r = (float) ratio;
        float ir = 1.0f - r;
        float[] rgb1 = color1.getRGBComponents(null);
        float[] rgb2 = color2.getRGBComponents(null);
        float red = rgb1[0] * r + rgb2[0] * ir;
        float green = rgb1[1] * r + rgb2[1] * ir;
        float blue = rgb1[2] * r + rgb2[2] * ir;
        return new Color(red, green, blue);
    }

    public int multAlpha(int c, float apc) {
        return getColor(red(c), green(c), blue(c), (float) alpha(c) * apc);
    }

    public int astolfo(int speed, int index) {
        double angle = (int) ((System.currentTimeMillis() / speed + index) % 360);
        return Color.getHSBColor(
                ((angle %= 360) / 360.0) < 0.5 ? -((float) (angle / 360.0)) : (float) (angle / 360.0),
                0.5F,
                1.0F
        ).hashCode();
    }

    public int rainbow(int speed, int index, float saturation, float brightness, float opacity) {
        int angle = (int) ((System.currentTimeMillis() / speed + index) % 360);
        float hue = angle / 360f;
        int color = Color.HSBtoRGB(hue, saturation, brightness);
        return getColor(
                red(color),
                green(color),
                blue(color),
                Math.max(0, Math.min(255, (int) (opacity * 255)))
        );
    }

    private static int calculateHueDegrees(int divisor, int offset) {
        long currentTime = System.currentTimeMillis();
        long calculatedValue = (currentTime / divisor + offset) % 360L;
        return (int) calculatedValue;
    }

    public static int gradient(int start, int end, int index, int speed) {
        int angle = (int) ((System.currentTimeMillis() / speed + index) % 360);
        angle = (angle > 180 ? 360 - angle : angle) + 180;
        int color = interpolate(start, end, MathHelper.clamp(angle / 180f - 1, 0, 1));
        float[] hs = rgba(color);
        float[] hsb = Color.RGBtoHSB((int) (hs[0] * 255), (int) (hs[1] * 255), (int) (hs[2] * 255), null);

        hsb[1] *= 1.5F;
        hsb[1] = Math.min(hsb[1], 1.0f);

        return Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
    }

    public Color interpolate(Color color1, Color color2, double amount) {
        amount = 1F - amount;
        amount = (float) MathHelper.clamp(0, 1, amount);
        return new Color(
                (int) MathUtil.lerp(color1.getRed(), color2.getRed(), amount),
                (int) MathUtil.lerp(color1.getGreen(), color2.getGreen(), amount),
                (int) MathUtil.lerp(color1.getBlue(), color2.getBlue(), amount),
                (int) MathUtil.lerp(color1.getAlpha(), color2.getAlpha(), amount)
        );
    }

    public int getColor(int r, int g, int b, int a) {
        return new Color(r, g, b, a).getRGB();
    }

    public int getColor(int r, int g, int b) {
        return new Color(r, g, b, 255).getRGB();
    }

    public int getColor(int br, int a) {
        return new Color(br, br, br, a).getRGB();
    }

    public int interpolate(int color1, int color2, double amount) {
        amount = (float) MathHelper.clamp(0, 1, amount);
        return getColor(
                MathUtil.lerp(red(color1), red(color2), amount),
                MathUtil.lerp(green(color1), green(color2), amount),
                MathUtil.lerp(blue(color1), blue(color2), amount),
                MathUtil.lerp(alpha(color1), alpha(color2), amount)
        );
    }

    public static int interpolate(int start, int end, float value) {
        float[] startColor = rgba(start);
        float[] endColor = rgba(end);

        return rgba((int) MathUtil.interpolate(startColor[0] * 255, endColor[0] * 255, value),
                (int) MathUtil.interpolate(startColor[1] * 255, endColor[1] * 255, value),
                (int) MathUtil.interpolate(startColor[2] * 255, endColor[2] * 255, value),
                (int) MathUtil.interpolate(startColor[3] * 255, endColor[3] * 255, value));
    }

    public static Color lerp(int speed, int index, Color start, Color end) {
        int angle = (int) (((System.currentTimeMillis()) / speed + index) % 360);
        angle = (angle >= 180 ? 360 - angle : angle) * 2;
        return interpolate(start, end, angle / 360f);
    }

    public static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) MathHelper.clamp(0, 255, alpha));
    }

    public static Double interpolateD(double oldValue, double newValue, double interpolationValue) {
        return (oldValue + (newValue - oldValue) * interpolationValue);
    }

    public static int interpolateInt(int oldValue, int newValue, double interpolationValue) {
        return interpolateD(oldValue, newValue, (float) interpolationValue).intValue();
    }

    public static int getRed(final int hex) {
        return hex >> 16 & 255;
    }

    public static int getGreen(final int hex) {
        return hex >> 8 & 255;
    }

    public static int getBlue(final int hex) {
        return hex & 255;
    }

    public static int getAlpha(final int hex) {
        return hex >> 24 & 255;
    }

    public static int getAlphaFromColor(int color) {
        return getAlpha(color);
    }

    public static int swapAlpha(int color, float alpha) {
        return setAlpha(color, MathHelper.clamp((int) alpha, 0, 255));
    }

    public static int blendColors(int color1, int color2) {
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        float alphaRatio = a2 / 255.0f;
        int a = (int) (a1 + (a2 - a1) * alphaRatio);
        int r = (int) (r1 + (r2 - r1) * alphaRatio);
        int g = (int) (g1 + (g2 - g1) * alphaRatio);
        int b = (int) (b1 + (b2 - b1) * alphaRatio);

        return (MathHelper.clamp(a, 0, 255) << 24) |
                (MathHelper.clamp(r, 0, 255) << 16) |
                (MathHelper.clamp(g, 0, 255) << 8) |
                MathHelper.clamp(b, 0, 255);
    }

    public static int getOppositeColor(int color) {
        return rgba(255 - getRed(color), 255 - getGreen(color), 255 - getBlue(color), getAlpha(color));
    }

}
