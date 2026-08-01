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
 * Настройки конкретного пака косметики: выбранный вид, где сидит пет,
 * где висит оружие. Хранится в &lt;gameDir&gt;/figura/harmony_pack_settings.txt
 * строками вида  variant|01a_shizuko=01b_shizuko_swimsuit
 */
public final class FiguraPackSettings {

    /** Где сидит пет. */
    public enum PetPlacement {
        GROUND("У ног"),
        HEAD("На голове");

        public final String title;

        PetPlacement(String title) {
            this.title = title;
        }
    }

    /** Как считается высота/рост пака. */
    public enum HeightMode {
        AUTO("Авто"),
        CUSTOM("Менять");

        public final String title;

        HeightMode(String title) {
            this.title = title;
        }
    }

    /** Где висит оружие. */
    public enum WeaponPlacement {
        BACK("На спине"),
        HAND("В руке"),
        AUTO("Авто");

        public final String title;

        WeaponPlacement(String title) {
            this.title = title;
        }
    }

    private static final Object LOCK = new Object();
    private static final Map<String, String> VARIANT = new LinkedHashMap<String, String>();
    private static final Map<String, String> PET = new LinkedHashMap<String, String>();
    private static final Map<String, String> WEAPON = new LinkedHashMap<String, String>();
    private static final Map<String, String> SCALE = new LinkedHashMap<String, String>();
    private static final Map<String, String> ROTATE = new LinkedHashMap<String, String>();
    private static final Map<String, String> HEIGHT = new LinkedHashMap<String, String>();
    private static final Map<String, String> HEIGHT_MODE = new LinkedHashMap<String, String>();
    private static final Map<String, String> TPOSE = new LinkedHashMap<String, String>();

    public static final float SCALE_MIN = 25f;
    public static final float SCALE_MAX = 300f;
    public static final float SCALE_DEFAULT = 100f;

    public static final float ROTATE_MIN = -180f;
    public static final float ROTATE_MAX = 180f;
    public static final float ROTATE_DEFAULT = 0f;

    public static final float HEIGHT_MIN = -100f;
    public static final float HEIGHT_MAX = 100f;
    public static final float HEIGHT_DEFAULT = 0f;

    private static volatile boolean loaded;

    private FiguraPackSettings() {
    }

    // ------------------------------------------------------------------ API

    /** Фолдер выбранного вида или null. */
    public static String getVariant(String headFolder) {
        ensureLoaded();
        if (headFolder == null) {
            return null;
        }
        synchronized (LOCK) {
            return VARIANT.get(key(headFolder));
        }
    }

    public static void setVariant(String headFolder, String variantFolder) {
        ensureLoaded();
        if (headFolder == null) {
            return;
        }
        synchronized (LOCK) {
            if (variantFolder == null || variantFolder.equalsIgnoreCase(headFolder)) {
                VARIANT.remove(key(headFolder));
            } else {
                VARIANT.put(key(headFolder), variantFolder);
            }
        }
        save();
    }

    public static PetPlacement getPet(String folder) {
        ensureLoaded();
        String raw;
        synchronized (LOCK) {
            raw = PET.get(key(FiguraAvatarLibrary.headFolder(folder)));
        }
        if (raw != null) {
            try {
                return PetPlacement.valueOf(raw);
            } catch (Exception ignored) {
            }
        }
        return PetPlacement.GROUND;
    }

    public static void setPet(String folder, PetPlacement placement) {
        ensureLoaded();
        if (folder == null || placement == null) {
            return;
        }
        synchronized (LOCK) {
            PET.put(key(FiguraAvatarLibrary.headFolder(folder)), placement.name());
        }
        save();
    }

    public static WeaponPlacement getWeapon(String folder) {
        ensureLoaded();
        String raw;
        synchronized (LOCK) {
            raw = WEAPON.get(key(FiguraAvatarLibrary.headFolder(folder)));
        }
        if (raw != null) {
            try {
                return WeaponPlacement.valueOf(raw);
            } catch (Exception ignored) {
            }
        }
        return WeaponPlacement.AUTO;
    }

    public static void setWeapon(String folder, WeaponPlacement placement) {
        ensureLoaded();
        if (folder == null || placement == null) {
            return;
        }
        synchronized (LOCK) {
            WEAPON.put(key(FiguraAvatarLibrary.headFolder(folder)), placement.name());
        }
        save();
    }

    // ------------------------------------------- Масштаб / поворот / высота

    /** Масштаб пака в процентах (25..300). */
    public static float getScale(String folder) {
        return num(SCALE, folder, SCALE_DEFAULT, SCALE_MIN, SCALE_MAX);
    }

    public static void setScale(String folder, float value) {
        putNum(SCALE, folder, value, SCALE_MIN, SCALE_MAX);
    }

    /** Поворот пака в градусах (-180..180). */
    public static float getRotate(String folder) {
        return num(ROTATE, folder, ROTATE_DEFAULT, ROTATE_MIN, ROTATE_MAX);
    }

    public static void setRotate(String folder, float value) {
        putNum(ROTATE, folder, value, ROTATE_MIN, ROTATE_MAX);
    }

    /** Смещение по высоте в процентах блока (-100..100). */
    public static float getHeight(String folder) {
        return num(HEIGHT, folder, HEIGHT_DEFAULT, HEIGHT_MIN, HEIGHT_MAX);
    }

    public static void setHeight(String folder, float value) {
        putNum(HEIGHT, folder, value, HEIGHT_MIN, HEIGHT_MAX);
    }

    public static HeightMode getHeightMode(String folder) {
        ensureLoaded();
        String raw;
        synchronized (LOCK) {
            raw = HEIGHT_MODE.get(key(FiguraAvatarLibrary.headFolder(folder)));
        }
        if (raw != null) {
            try {
                return HeightMode.valueOf(raw);
            } catch (Exception ignored) {
            }
        }
        return HeightMode.AUTO;
    }

    public static void setHeightMode(String folder, HeightMode value) {
        ensureLoaded();
        if (folder == null || value == null) {
            return;
        }
        synchronized (LOCK) {
            HEIGHT_MODE.put(key(FiguraAvatarLibrary.headFolder(folder)), value.name());
        }
        save();
    }

    /** Исправлять ли T-позу у этого пака. */
    public static boolean getFixTPose(String folder) {
        ensureLoaded();
        String raw;
        synchronized (LOCK) {
            raw = TPOSE.get(key(FiguraAvatarLibrary.headFolder(folder)));
        }
        return raw == null || !raw.equalsIgnoreCase("false");
    }

    public static void setFixTPose(String folder, boolean value) {
        ensureLoaded();
        if (folder == null) {
            return;
        }
        synchronized (LOCK) {
            TPOSE.put(key(FiguraAvatarLibrary.headFolder(folder)), value ? "true" : "false");
        }
        save();
    }

    /**
     * Нужно ли править T-позу у пака, который сейчас собирается/рисуется.
     * BbModelRenderer должен спрашивать именно этот метод вместо
     * FiguraCosmetic.INSTANCE.fixArms.get().
     */
    public static boolean isFixTPoseActive() {
        String folder = BUILDING;
        if (folder == null) {
            try {
                folder = FiguraWear.getWornFolder();
            } catch (Throwable ignored) {
            }
        }
        return folder == null || getFixTPose(folder);
    }

    /** Фолдер, модели которого сейчас собираются (для превью и загрузки). */
    private static volatile String BUILDING;

    public static void beginBuild(String folder) {
        BUILDING = folder;
    }

    public static void endBuild() {
        BUILDING = null;
    }

    private static float num(Map<String, String> map, String folder, float def, float min, float max) {
        ensureLoaded();
        String raw;
        synchronized (LOCK) {
            raw = map.get(key(FiguraAvatarLibrary.headFolder(folder)));
        }
        if (raw == null) {
            return def;
        }
        try {
            float value = Float.parseFloat(raw);
            if (value < min) value = min;
            if (value > max) value = max;
            return value;
        } catch (Exception ignored) {
            return def;
        }
    }

    private static void putNum(Map<String, String> map, String folder, float value, float min, float max) {
        ensureLoaded();
        if (folder == null) {
            return;
        }
        if (value < min) value = min;
        if (value > max) value = max;
        synchronized (LOCK) {
            map.put(key(FiguraAvatarLibrary.headFolder(folder)), String.valueOf(Math.round(value)));
        }
        save();
    }

    // ------------------------------------------------------------- Хранение

    private static String key(String folder) {
        return folder == null ? "" : folder.toLowerCase(Locale.ROOT);
    }

    private static File gameDir() {
        Minecraft mc = Minecraft.getInstance();
        return mc != null && mc.gameDir != null ? mc.gameDir : new File(".");
    }

    private static Path file() {
        return gameDir().toPath().resolve("figura").resolve("harmony_pack_settings.txt");
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
                    int bar = line.indexOf('|');
                    int eq = line.indexOf('=');
                    if (bar <= 0 || eq <= bar + 1) {
                        continue;
                    }
                    String kind = line.substring(0, bar).trim().toLowerCase(Locale.ROOT);
                    String folder = line.substring(bar + 1, eq).trim().toLowerCase(Locale.ROOT);
                    String value = line.substring(eq + 1).trim();
                    if (folder.isEmpty() || value.isEmpty()) {
                        continue;
                    }
                    if (kind.equals("variant")) {
                        VARIANT.put(folder, value);
                    } else if (kind.equals("pet")) {
                        PET.put(folder, value.toUpperCase(Locale.ROOT));
                    } else if (kind.equals("weapon")) {
                        WEAPON.put(folder, value.toUpperCase(Locale.ROOT));
                    } else if (kind.equals("scale")) {
                        SCALE.put(folder, value);
                    } else if (kind.equals("rotate")) {
                        ROTATE.put(folder, value);
                    } else if (kind.equals("height")) {
                        HEIGHT.put(folder, value);
                    } else if (kind.equals("heightmode")) {
                        HEIGHT_MODE.put(folder, value.toUpperCase(Locale.ROOT));
                    } else if (kind.equals("tpose")) {
                        TPOSE.put(folder, value.toLowerCase(Locale.ROOT));
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
            sb.append("# Harmony FiguraCosmetic: настройки паков\n");
            synchronized (LOCK) {
                appendAll(sb, "variant", VARIANT);
                appendAll(sb, "pet", PET);
                appendAll(sb, "weapon", WEAPON);
                appendAll(sb, "scale", SCALE);
                appendAll(sb, "rotate", ROTATE);
                appendAll(sb, "height", HEIGHT);
                appendAll(sb, "heightmode", HEIGHT_MODE);
                appendAll(sb, "tpose", TPOSE);
            }
            Files.write(path, sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
    }

    private static void appendAll(StringBuilder sb, String kind, Map<String, String> map) {
        for (Map.Entry<String, String> e : map.entrySet()) {
            if (e.getKey() == null || e.getValue() == null) {
                continue;
            }
            sb.append(kind).append('|').append(e.getKey()).append('=').append(e.getValue()).append('\n');
        }
    }
}
