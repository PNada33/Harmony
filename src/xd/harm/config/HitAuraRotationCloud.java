package xd.harm.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class HitAuraRotationCloud {

    public static final String MODE_SPOOKY = "spooky";
    public static final String MODE_FUNTIME = "funtime";
    public static final String MODE_RILLIWORLD = "rilliworld";
    public static final String MODE_TRAINING = "training";
    public static final String MODE_GRIM = "grim";
    public static final String MODE_SLOTAC = "slotac";
    public static final String MODE_RECORDING = "recording";
    public static final String MODE_PACKET = "packet";
    public static final String MODE_TRACK = "track";
    public static final String MODE_INTERACT = "interact";
    public static final String MODE_LEGIT = "legit";
    public static final String MODE_MATRIX = "matrix";

    private static final String[] MODE_ORDER = new String[]{
            MODE_SPOOKY,
            MODE_FUNTIME,
            MODE_RILLIWORLD,
            MODE_TRAINING,
            MODE_GRIM,
            MODE_SLOTAC,
            MODE_RECORDING,
            MODE_PACKET,
            MODE_TRACK,
            MODE_INTERACT,
            MODE_LEGIT,
            MODE_MATRIX
    };

    private static final Map<String, String> DEFAULT_LABELS = new LinkedHashMap<>();
    private static final Object LOCK = new Object();

    private static Map<String, String> labelsById = new LinkedHashMap<>();
    private static Map<String, String> idByLabel = new HashMap<>();

    static {
        DEFAULT_LABELS.put(MODE_SPOOKY, "СпукиТайм");
        DEFAULT_LABELS.put(MODE_FUNTIME, "ФанТайм");
        DEFAULT_LABELS.put(MODE_RILLIWORLD, "РиллиВорлд");
        DEFAULT_LABELS.put(MODE_GRIM, "Грим");
        DEFAULT_LABELS.put(MODE_SLOTAC, "СлотАС");
        DEFAULT_LABELS.put(MODE_TRAINING, "Тренировка");
        DEFAULT_LABELS.put(MODE_RECORDING, "Запись");
        DEFAULT_LABELS.put(MODE_PACKET, "Пакет");
        DEFAULT_LABELS.put(MODE_TRACK, "Трек");
        DEFAULT_LABELS.put(MODE_INTERACT, "Интеракт");
        DEFAULT_LABELS.put(MODE_LEGIT, "Легит");
        DEFAULT_LABELS.put(MODE_MATRIX, "Матрица");
        resetToDefaults();
    }

    private HitAuraRotationCloud() {
    }

    public static void loadFromCloud() {
        loadFromPayload(CloudBootstrapState.copyRotationPayload());
    }

    public static void loadFromPayload(JsonObject payload) {
        if (payload == null || payload.entrySet().isEmpty()) {
            resetToDefaults();
            return;
        }

        Map<String, String> loaded = parsePayload(payload);
        if (loaded.isEmpty()) {
            resetToDefaults();
            return;
        }

        Map<String, String> merged = new LinkedHashMap<>(DEFAULT_LABELS);
        merged.putAll(loaded);

        synchronized (LOCK) {
            labelsById = merged;
            rebuildLookup();
        }
    }

    public static String[] getModeLabels() {
        synchronized (LOCK) {
            String[] labels = new String[MODE_ORDER.length];
            for (int i = 0; i < MODE_ORDER.length; i++) {
                labels[i] = labelsById.get(MODE_ORDER[i]);
            }
            return labels;
        }
    }

    public static String getDefaultLabel() {
        synchronized (LOCK) {
            String value = labelsById.get(MODE_SPOOKY);
            return value == null ? DEFAULT_LABELS.get(MODE_SPOOKY) : value;
        }
    }

    public static String normalizeSelectedLabel(String value) {
        String modeId = resolveModeId(value);
        synchronized (LOCK) {
            String resolved = labelsById.get(modeId);
            if (resolved != null && !resolved.trim().isEmpty()) {
                return resolved;
            }
            return DEFAULT_LABELS.get(MODE_SPOOKY);
        }
    }

    public static boolean isMode(String selectedValue, String modeId) {
        return modeId.equals(resolveModeId(selectedValue));
    }

    public static String resolveModeId(String selectedValue) {
        if (selectedValue == null) {
            return MODE_SPOOKY;
        }

        String normalized = selectedValue.trim();
        if (normalized.isEmpty()) {
            return MODE_SPOOKY;
        }

        String compactId = normalizeModeId(normalized);
        if (!compactId.isEmpty()) {
            return compactId;
        }

        String key = normalized.toLowerCase(Locale.ROOT);
        synchronized (LOCK) {
            String byLabel = idByLabel.get(key);
            return byLabel == null ? MODE_SPOOKY : byLabel;
        }
    }

    private static void resetToDefaults() {
        synchronized (LOCK) {
            labelsById = new LinkedHashMap<>(DEFAULT_LABELS);
            rebuildLookup();
        }
    }

    private static void rebuildLookup() {
        idByLabel = new HashMap<>();
        for (String modeId : MODE_ORDER) {
            putLookup(modeId, modeId);
            putLookup(labelsById.get(modeId), modeId);
            putLookup(DEFAULT_LABELS.get(modeId), modeId);
        }
    }

    private static void putLookup(String value, String modeId) {
        if (value == null) {
            return;
        }
        String key = value.trim().toLowerCase(Locale.ROOT);
        if (!key.isEmpty()) {
            idByLabel.put(key, modeId);
        }
    }

    private static Map<String, String> parsePayload(JsonObject payload) {
        Map<String, String> fromObject = parseObjectModes(payload);
        if (!fromObject.isEmpty()) {
            return fromObject;
        }

        JsonElement modesElement = payload.get("modes");
        if (modesElement == null || modesElement.isJsonNull()) {
            return new LinkedHashMap<>();
        }

        if (modesElement.isJsonObject()) {
            return parseObjectModes(modesElement.getAsJsonObject());
        }

        if (modesElement.isJsonArray()) {
            return parseArrayModes(modesElement.getAsJsonArray());
        }

        return new LinkedHashMap<>();
    }

    private static Map<String, String> parseObjectModes(JsonObject object) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String modeId : MODE_ORDER) {
            String label = getString(object, modeId);
            if (!label.isEmpty()) {
                result.put(modeId, label);
            }
        }
        return result;
    }

    private static Map<String, String> parseArrayModes(JsonArray array) {
        Map<String, String> result = new LinkedHashMap<>();

        for (int i = 0; i < array.size(); i++) {
            JsonElement element = array.get(i);
            if (element == null || element.isJsonNull()) {
                continue;
            }

            if (element.isJsonPrimitive()) {
                if (i >= MODE_ORDER.length) {
                    continue;
                }
                String label = element.getAsString().trim();
                if (!label.isEmpty()) {
                    result.put(MODE_ORDER[i], label);
                }
                continue;
            }

            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject modeObject = element.getAsJsonObject();
            String rawId = getString(modeObject, "id");
            if (rawId.isEmpty()) {
                rawId = getString(modeObject, "mode");
            }

            String modeId = normalizeModeId(rawId);
            if (modeId.isEmpty()) {
                continue;
            }

            String label = getString(modeObject, "label");
            if (label.isEmpty()) {
                label = getString(modeObject, "name");
            }
            if (label.isEmpty()) {
                label = getString(modeObject, "value");
            }

            if (!label.isEmpty()) {
                result.put(modeId, label);
            }
        }

        return result;
    }

    private static String normalizeModeId(String value) {
        if (value == null) {
            return "";
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT)
                .replace(" ", "")
                .replace("_", "")
                .replace("-", "");

        switch (normalized) {
            case "spooky":
            case "spookytime":
                return MODE_SPOOKY;
            case "funtime":
                return MODE_FUNTIME;
            case "rilliworld":
            case "rilli":
                return MODE_RILLIWORLD;
            case "training":
            case "train":
                return MODE_TRAINING;
            case "grim":
                return MODE_GRIM;
            case "slotac":
            case "slot":
            case "sloth":
            case "slothrotation":
            case "слотас":
                return MODE_SLOTAC;
            case "recording":
                return MODE_RECORDING;
            case "packet":
                return MODE_PACKET;
            case "track":
                return MODE_TRACK;
            case "interact":
                return MODE_INTERACT;
            case "legit":
                return MODE_LEGIT;
            case "matrix":
                return MODE_MATRIX;
            default:
                return "";
        }
    }

    private static String getString(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return "";
        }

        try {
            String value = element.getAsString();
            return value == null ? "" : value.trim();
        } catch (Exception ignored) {
            return "";
        }
    }
}
