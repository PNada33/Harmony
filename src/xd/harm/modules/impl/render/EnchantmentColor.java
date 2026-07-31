package xd.harm.modules.impl.render;

import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.ColorSetting;
import xd.harm.modules.settings.impl.ModeSetting;
import xd.harm.utils.render.color.ColorUtils;

import java.awt.Color;

@ModuleRegister(name = "EnchantmentColor", category = Category.Render, desc = "Изменяет цвет зачарований")
public class EnchantmentColor extends Module {
    private static EnchantmentColor instance;

    public static final ModeSetting colorMode = new ModeSetting("Цвет", "Радужный", "Радужный", "Клиент", "Кастом");
    public static final ColorSetting customColor = new ColorSetting("Свой цвет", ColorUtils.rgb(255, 255, 255)).setVisible(() -> colorMode.is("Кастом"));

    public EnchantmentColor() {
        instance = this;
        addSettings(colorMode, customColor);
    }

    public static boolean enabled() {
        try {
            EnchantmentColor module = instance;
            return module != null && module.isState();
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static int getColor() {
        if (colorMode.is("Клиент")) {
            return Theme.MainColor(0);
        }

        if (colorMode.is("Кастом")) {
            return customColor.get();
        }

        float hue = (System.currentTimeMillis() % 4500L) / 4500.0f;
        return Color.HSBtoRGB(hue, 0.85f, 1.0f);
    }
}
