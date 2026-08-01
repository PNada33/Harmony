package xd.harm.utils.figura;

import xd.harm.Harmony;

/**
 * Состояние косметических функций Figura Cosmetic.
 *
 * Раньше это были обычные модули ClickGUI (Katana, ChinaHat, Raincoat,
 * PatPatPat). Теперь функций-модулей нет — есть карточки в Figura Cosmetic,
 * а их «включено/выключено» и настройки живут здесь и сохраняются в
 * {@link CosmeticSettings}.
 */
public final class CosmeticFeatures {

    /** Идентификаторы функций: совпадают с moduleName у карточек. */
    public static final String KATANA = "Katana";
    public static final String CHINA_HAT = "ChinaHat";
    public static final String RAINCOAT = "Raincoat";
    public static final String PAT = "PatPatPat";

    /** Все функции, которые показываем карточками. */
    public static final String[] ALL = new String[]{KATANA, CHINA_HAT, RAINCOAT, PAT};

    private static volatile boolean registered;

    private CosmeticFeatures() {
    }

    // ------------------------------------------------------------ Состояние

    /** Включена ли функция. */
    public static boolean isEnabled(String feature) {
        if (feature == null) {
            return false;
        }
        return CosmeticSettings.getBool(key(feature) + ".enabled", false);
    }

    /** Включает или выключает функцию. */
    public static void setEnabled(String feature, boolean enabled) {
        if (feature == null || isEnabled(feature) == enabled) {
            return;
        }
        CosmeticSettings.setBool(key(feature) + ".enabled", enabled);
        if (PAT.equals(feature) && !enabled) {
            CosmeticPat.get().reset();
        }
        bootstrap();
    }

    public static void toggle(String feature) {
        setEnabled(feature, !isEnabled(feature));
    }

    /** Есть ли такая функция вообще. */
    public static boolean available(String feature) {
        if (feature == null) {
            return false;
        }
        for (int i = 0; i < ALL.length; i++) {
            if (ALL[i].equalsIgnoreCase(feature)) {
                return true;
            }
        }
        return false;
    }

    /** Человеческое имя функции для GUI. */
    public static String title(String feature) {
        if (KATANA.equals(feature)) {
            return "Катана";
        }
        if (CHINA_HAT.equals(feature)) {
            return "Китайская шляпа";
        }
        if (RAINCOAT.equals(feature)) {
            return "Плащ";
        }
        if (PAT.equals(feature)) {
            return "Погладить";
        }
        return feature == null ? "" : feature;
    }

    // ------------------------------------------------------------- Настройки

    public static boolean getBool(String feature, String name, boolean def) {
        return CosmeticSettings.getBool(key(feature) + "." + name, def);
    }

    public static void setBool(String feature, String name, boolean value) {
        CosmeticSettings.setBool(key(feature) + "." + name, value);
    }

    public static float getFloat(String feature, String name, float def, float min, float max) {
        return CosmeticSettings.getFloat(key(feature) + "." + name, def, min, max);
    }

    public static void setFloat(String feature, String name, float value, float min, float max) {
        CosmeticSettings.setFloat(key(feature) + "." + name, value, min, max);
    }

    public static int getColor(String feature, String name, int def) {
        return CosmeticSettings.getInt(key(feature) + "." + name, def);
    }

    public static void setColor(String feature, String name, int value) {
        CosmeticSettings.setInt(key(feature) + "." + name, value);
    }

    public static String getMode(String feature, String name, String def) {
        return CosmeticSettings.getString(key(feature) + "." + name, def);
    }

    public static void setMode(String feature, String name, String value) {
        CosmeticSettings.setString(key(feature) + "." + name, value);
    }

    /** Режим цвета: true — берём цвет темы клиента, false — свой цвет. */
    public static boolean usesThemeColor(String feature) {
        return "Тема".equalsIgnoreCase(getMode(feature, "colorMode", "Тема"));
    }

    // ---------------------------------------------------------- Регистрация

    /**
     * Подписывает рендер косметики на шину событий. Вызывается из
     * FiguraWear.bootstrap(), поэтому отдельного модуля больше не нужно.
     */
    public static void bootstrap() {
        if (registered) {
            return;
        }
        try {
            Harmony harmony = Harmony.getInstance();
            if (harmony == null || harmony.getEventBus() == null) {
                return;
            }
            harmony.getEventBus().register(CosmeticRenderer.get());
            harmony.getEventBus().register(CosmeticPat.get());
            registered = true;
        } catch (Throwable ignored) {
        }
    }

    private static String key(String feature) {
        return feature == null ? "" : feature.toLowerCase();
    }
}
