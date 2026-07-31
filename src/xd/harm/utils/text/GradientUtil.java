package xd.harm.utils.text;

import xd.harm.modules.impl.render.Theme;
import xd.harm.utils.render.color.ColorUtils;
import net.minecraft.util.text.*;

public class GradientUtil {

    public static IFormattableTextComponent gradient(String message) {
        StringTextComponent text = new StringTextComponent("");

        int accentColor = Theme.MainColor(0);
        int darkAccentColor = ColorUtils.darker(accentColor, 0.5f);

        for (int i = 0; i < message.length(); i++) {

            int speed = 5;
            int index = i * 20;

            int angle = (int) ((System.currentTimeMillis() / speed + index) % 360);
            angle = angle >= 180 ? 360 - angle : angle;

            float factor = angle / 180f;

            int finalColor = ColorUtils.interpolate(accentColor, darkAccentColor, factor);

            text.append(new StringTextComponent(String.valueOf(message.charAt(i)))
                    .setStyle(Style.EMPTY.setColor(new Color(finalColor))));
        }
        return text;
    }

    public static IFormattableTextComponent whiteToLightGrayGradient(String message) {
        StringTextComponent text = new StringTextComponent("");

        long time = System.currentTimeMillis();
        float timeOffset = (time % 3400) / 3400.0f;

        for (int i = 0; i < message.length(); i++) {

            float position = ((float) i / message.length() + timeOffset) % 1.0f;

            float factor = (float) (Math.sin(position * Math.PI * 2) * 0.5 + 0.5);


            int value;
            if (factor < 0.33) {

                float localFactor = factor / 0.33f;
                value = (int) (255 * (1 - localFactor) + 220 * localFactor);
            } else if (factor < 0.66) {

                float localFactor = (factor - 0.33f) / 0.33f;
                value = (int) (220 * (1 - localFactor) + 190 * localFactor);
            } else {

                float localFactor = (factor - 0.66f) / 0.34f;
                value = (int) (190 * (1 - localFactor) + 160 * localFactor);
            }

            text.append(new StringTextComponent(String.valueOf(message.charAt(i)))
                    .setStyle(Style.EMPTY.setColor(new Color(ColorUtils.rgb(value, value, value)))));
        }
        return text;
    }

    public static IFormattableTextComponent goldGradient(String message) {
        StringTextComponent text = new StringTextComponent("");

        long time = System.currentTimeMillis();
        float timeOffset = (time % 3400) / 3400.0f;

        for (int i = 0; i < message.length(); i++) {

            float position = ((float) i / message.length() + timeOffset) % 1.0f;

            float factor = (float) (Math.sin(position * Math.PI * 2) * 0.5 + 0.5);

            int r = (int) (184 * (1 - factor) + 255 * factor);
            int g = (int) (134 * (1 - factor) + 215 * factor);
            int b = (int) (11 * (1 - factor) + 0 * factor);

            text.append(new StringTextComponent(String.valueOf(message.charAt(i)))
                    .setStyle(Style.EMPTY.setColor(new Color(ColorUtils.rgb(r, g, b)))));
        }
        return text;
    }



    public static IFormattableTextComponent redGradient(String message) {
        StringTextComponent text = new StringTextComponent("");

        long time = System.currentTimeMillis();
        float timeOffset = (time % 3400) / 3400.0f;

        for (int i = 0; i < message.length(); i++) {

            float position = ((float) i / message.length() + timeOffset) % 1.0f;

            float factor = (float) (Math.sin(position * Math.PI * 2) * 0.5 + 0.5);

            int r = (int) (178 * (1 - factor) + 255 * factor);
            int g = (int) (34 * (1 - factor) + 102 * factor);
            int b = (int) (34 * (1 - factor) + 102 * factor);

            text.append(new StringTextComponent(String.valueOf(message.charAt(i)))
                    .setStyle(Style.EMPTY.setColor(new Color(ColorUtils.rgb(r, g, b)))));
        }
        return text;
    }

    public static StringTextComponent gradient1(String message) {
        StringTextComponent text = new StringTextComponent("");
        for (int i = 0; i < message.length(); i++) {
            text.append(new StringTextComponent(String.valueOf(message.charAt(i))).setStyle(Style.EMPTY.setColor(new Color(Theme.MainColor(i)))));
        }
        return text;
    }

    public static StringTextComponent gradienmainmenu(String message) {
        StringTextComponent text = new StringTextComponent("");
        for (int i = 0; i < message.length(); i++) {
            text.append(new StringTextComponent(String.valueOf(message.charAt(i))).setStyle(Style.EMPTY.setColor(new Color(Theme.getMainMenu(i,i)))));
        }
        return text;
    }

    public static IFormattableTextComponent white(String message) {
        return (new StringTextComponent(message)).setStyle(Style.EMPTY.setColor(Color.fromHex("#FFFFFF")));
    }

    public static IFormattableTextComponent blue(String message) {
        return (new StringTextComponent(message)).setStyle(Style.EMPTY.setColor(Color.fromHex("#6495ED")));
    }

    public static IFormattableTextComponent red(String message) {
        return (new StringTextComponent(message)).setStyle(Style.EMPTY.setColor(Color.fromHex("#FF0000")));
    }

    public static IFormattableTextComponent green(String message) {
        return (new StringTextComponent(message)).setStyle(Style.EMPTY.setColor(Color.fromHex("#00FF00")));
    }

    public static IFormattableTextComponent black(String message) {
        return (new StringTextComponent(message)).setStyle(Style.EMPTY.setColor(Color.fromHex("#000000")));
    }
}
