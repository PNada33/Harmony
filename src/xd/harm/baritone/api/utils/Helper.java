

package xd.harm.baritone.api.utils;

import xd.harm.baritone.api.BaritoneAPI;
import xd.harm.utils.text.GradientUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.*;

import java.util.Arrays;
import java.util.Calendar;
import java.util.stream.Stream;


public interface Helper {


    Helper HELPER = new Helper() {};


    Minecraft mc = Minecraft.getInstance();

    static ITextComponent getPrefix() {
        final Calendar now = Calendar.getInstance();
        final boolean xd = now.get(Calendar.MONTH) == Calendar.APRIL && now.get(Calendar.DAY_OF_MONTH) <= 3;
        TextComponent baritone = new StringTextComponent(xd ? "Baritone" : BaritoneAPI.getSettings().shortBaritonePrefix.value ? "B" : "Baritone");

        baritone.setStyle(baritone.getStyle().setFormatting(TextFormatting.LIGHT_PURPLE));

        TextComponent prefix = new StringTextComponent("");
        prefix.append(GradientUtil.gradient("[Baritone] »"));
        return prefix;
    }


    default void logToast(ITextComponent title, ITextComponent message) {
        mc.execute(() -> BaritoneAPI.getSettings().toaster.value.accept(title, message));
    }


    default void logToast(String title, String message) {
        logToast(new StringTextComponent(title), new StringTextComponent(message));
    }

    default void logToast(String message) {
        logToast(Helper.getPrefix(), new StringTextComponent(message));
    }


    default void logNotification(String message) {
        logNotification(message, false);
    }


    default void logNotification(String message, boolean error) {
        if (BaritoneAPI.getSettings().desktopNotifications.value) {
            logNotificationDirect(message, error);
        }
    }


    default void logNotificationDirect(String message) {
        logNotificationDirect(message, false);
    }


    default void logNotificationDirect(String message, boolean error) {
        mc.execute(() -> BaritoneAPI.getSettings().notifier.value.accept(message, error));
    }

    default void logDebug(String message) {
        if (!BaritoneAPI.getSettings().chatDebug.value) {
            return;
        }
        logDirect(message, false);
    }


    default void logDirect(boolean logAsToast, ITextComponent... components) {
        TextComponent component = new StringTextComponent("");
        component.append(getPrefix());
        component.append(new StringTextComponent(" "));
        Arrays.asList(components).forEach(component::append);
        if (logAsToast) {
            logToast(getPrefix(), component);
        } else {
            mc.execute(() -> BaritoneAPI.getSettings().logger.value.accept(component));
        }
    }


    default void logDirect(ITextComponent... components) {
        logDirect(BaritoneAPI.getSettings().logAsToast.value, components);
    }


    default void logDirect(String message, TextFormatting color, boolean logAsToast) {
        Stream.of(message.split("\n")).forEach(line -> {
            TextComponent component = new StringTextComponent(line.replace("\t", "    "));
            component.setStyle(component.getStyle().setFormatting(color));
            logDirect(logAsToast, component);
        });
    }


    default void logDirect(String message, TextFormatting color) {
        logDirect(message, color, BaritoneAPI.getSettings().logAsToast.value);
    }

    default void logDirect(String message, boolean logAsToast) {
        logDirect(message, TextFormatting.GRAY, logAsToast);
    }


    default void logDirect(String message) {
        logDirect(message, BaritoneAPI.getSettings().logAsToast.value);
    }
}
