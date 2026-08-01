package xd.harm.utils.figura;

import net.minecraft.client.Minecraft;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Хранилище настроек косметических функций Figura Cosmetic
 * (Катана, Китайская шляпа, Плащ, Погладить).
 *
 * Раньше эти значения жили в настройках модулей и попадали в конфиг клиента.
 * Модулей больше нет, поэтому всё лежит рядом с остальными настройками
 * косметики: &lt;gameDir&gt;/figura/harmony_cosmetics.txt строками вида
 * {@code katana.glow=true}.
 */
public final class CosmeticSettings {

    private static final Object LOCK = new Object();
    private static final Map<String, String> VALUES = new LinkedHashMap<String, String>();
    private static volatile boolean loaded;

    private CosmeticSettings() {
    }

    // ------------------------------------------------------------------ API

    public static boolean getBool(String key, boolean def) {
        String raw = get(key);
        if (raw == null) {
            return def;
        }
        return raw.equalsIgnoreCase("true");
    }

    public static void setBool(String key, boolean value) {
        put(key, value ? "true" : "false");
    }

    public static float getFloat(String key, float def, float min, float max) {
        String raw = get(key);
        if (raw == null) {
            return clamp(def, min, max);
        }
        try {
            return clamp(Float.parseFloat(raw), min, max);
        } catch (Exception ignored) {
            return clamp(def, min, max);
        }
    }

    public static void setFloat(String key, float value, float min, float max) {
        put(key, String.valueOf(clamp(value, min, max)));
    }

    public static int getInt(String key, int def) {
        String raw = get(key);
        if (raw == null) {
            return def;
        }
        try {
            return Integer.parseInt(raw);
        } catch (Exception ignored) {
            return def;
        }
    }

    public static void setInt(String key, int value) {
        put(key, String.valueOf(value));
    }

    public static String getString(String key, String def) {
        String raw = get(key);
        return raw == null || raw.isEmpty() ? def : raw;
    }

    public static void setString(String key, String value) {
        put(key, value == null ? "" : value);
    }

    // -------------------------------------------------------------- Внутри

    private static float clamp(float value, float min, float max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private static String get(String key) {
        ensureLoaded();
        if (key == null) {
            return null;
        }
        synchronized (LOCK) {
            return VALUES.get(key.toLowerCase(Locale.ROOT));
        }
    }

    private static void put(String key, String value) {
        ensureLoaded();
        if (key == null) {
            return;
        }
        synchronized (LOCK) {
            VALUES.put(key.toLowerCase(Locale.ROOT), value);
        }
        save();
    }

    private static File gameDir() {
        Minecraft mc = Minecraft.getInstance();
        return mc != null && mc.gameDir != null ? mc.gameDir : new File(".");
    }

    private static Path file() {
        return gameDir().toPath().resolve("figura").resolve("harmony_cosmetics.txt");
    }

    private static void ensureLoaded() {
        if (loaded) {
            return;
        }
        loaded = true;
        Path path = file();
        if (!Files.isRegularFile(path)) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            synchronized (LOCK) {
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i).trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    int eq = line.indexOf('=');
                    if (eq <= 0) {
                        continue;
                    }
                    String key = line.substring(0, eq).trim().toLowerCase(Locale.ROOT);
                    String value = line.substring(eq + 1).trim();
                    if (!key.isEmpty()) {
                        VALUES.put(key, value);
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void save() {
        try {
            Path path = file();
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            StringBuilder sb = new StringBuilder();
            sb.append("# Harmony Figura Cosmetic: настройки косметических функций\n");
            synchronized (LOCK) {
                for (Map.Entry<String, String> e : VALUES.entrySet()) {
                    if (e.getKey() == null || e.getValue() == null) {
                        continue;
                    }
                    sb.append(e.getKey()).append('=').append(e.getValue()).append('\n');
                }
            }
            Files.write(path, sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
    }
}
