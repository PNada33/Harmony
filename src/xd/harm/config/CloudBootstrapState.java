package xd.harm.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.LinkedHashMap;
import java.util.Map;

public final class CloudBootstrapState {

    private static final JsonParser PARSER = new JsonParser();
    private static final Object LOCK = new Object();

    private static Map<String, ConfigEntry> configsById = new LinkedHashMap<>();
    private static JsonObject rotationPayload = new JsonObject();
    private static JsonObject runtimePayload = new JsonObject();

    private CloudBootstrapState() {
    }

    static void applyBootstrap(JsonObject bootstrap) {
        Map<String, ConfigEntry> loadedConfigs = new LinkedHashMap<>();
        JsonArray configsArray = getArray(bootstrap, "configs");
        for (JsonElement element : configsArray) {
            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject record = element.getAsJsonObject();
            String id = getString(record, "id");
            String name = getString(record, "name");
            JsonObject payload = getObject(record, "payload");
            if (id.isEmpty() || name.isEmpty()) {
                continue;
            }

            loadedConfigs.put(id, new ConfigEntry(id, name, cloneJsonObject(payload)));
        }

        synchronized (LOCK) {
            configsById = loadedConfigs;
            rotationPayload = cloneJsonObject(getObject(bootstrap, "rotations"));
            runtimePayload = cloneJsonObject(getObject(bootstrap, "runtime"));
        }
    }

    static Map<String, ConfigEntry> copyConfigs() {
        synchronized (LOCK) {
            Map<String, ConfigEntry> snapshot = new LinkedHashMap<>();
            for (Map.Entry<String, ConfigEntry> entry : configsById.entrySet()) {
                snapshot.put(entry.getKey(), entry.getValue().copy());
            }
            return snapshot;
        }
    }

    public static JsonObject copyRotationPayload() {
        synchronized (LOCK) {
            return cloneJsonObject(rotationPayload);
        }
    }

    public static JsonObject copyRuntimePayload() {
        synchronized (LOCK) {
            return cloneJsonObject(runtimePayload);
        }
    }

    static void putConfig(String id, String name, JsonObject payload) {
        synchronized (LOCK) {
            Map<String, ConfigEntry> updated = new LinkedHashMap<>(configsById);
            updated.put(id, new ConfigEntry(id, name, cloneJsonObject(payload)));
            configsById = updated;
        }
    }

    static void removeConfig(String id) {
        synchronized (LOCK) {
            if (!configsById.containsKey(id)) {
                return;
            }
            Map<String, ConfigEntry> updated = new LinkedHashMap<>(configsById);
            updated.remove(id);
            configsById = updated;
        }
    }

    private static JsonArray getArray(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : new JsonArray();
    }

    private static JsonObject getObject(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
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

    static JsonObject cloneJsonObject(JsonObject source) {
        if (source == null) {
            return new JsonObject();
        }

        try {
            JsonElement parsed = PARSER.parse(source.toString());
            return parsed != null && parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
        } catch (Exception ignored) {
            return new JsonObject();
        }
    }

    static final class ConfigEntry {
        final String id;
        final String name;
        final JsonObject payload;

        ConfigEntry(String id, String name, JsonObject payload) {
            this.id = id;
            this.name = name;
            this.payload = payload == null ? new JsonObject() : payload;
        }

        ConfigEntry copy() {
            return new ConfigEntry(id, name, cloneJsonObject(payload));
        }
    }
}
