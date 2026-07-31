package xd.harm.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CloudConfigClient {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final JsonParser PARSER = new JsonParser();
    private static final String PAYLOAD_KEY = "enc_v1";
    private static final String SHARE_PREFIX = "HCFG1:";
    private static final byte[] MASK_KEY = "HarmonyCloudMaskV1".getBytes(StandardCharsets.UTF_8);

    private final Map<String, ConfigRecord> recordsById = new LinkedHashMap<>();
    private CloudSettingsLoader.CloudSettings settings = CloudSettingsLoader.load();

    public void init() throws IOException {
        loadSettings();
        ensureConfigured();
        try {
            CloudBootstrapLoader.preload();
        } catch (RuntimeException exception) {
            throw new IOException("Cloud bootstrap unavailable", exception);
        }
        hydrateFromBootstrap();
    }

    public List<String> listConfigNames() throws IOException {
        ensureConfigured();
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (ConfigRecord record : recordsById.values()) {
            names.add(record.name);
        }
        return new ArrayList<>(names);
    }

    public boolean existsConfig(String configName) throws IOException {
        ensureConfigured();
        return resolveRecord(configName) != null;
    }

    public JsonObject downloadConfig(String configName) throws IOException {
        ensureConfigured();
        ConfigRecord record = resolveRecord(configName);
        if (record == null) {
            return null;
        }
        return unwrapEncryptedPayload(CloudBootstrapState.cloneJsonObject(record.payload));
    }

    public void uploadConfig(String configName, JsonObject payload) throws IOException {
        ensureConfigured();

        String normalizedName = normalizeName(configName);
        String recordId = buildConfigId(normalizedName);
        JsonObject wrappedPayload = wrapEncryptedPayload(payload);

        JsonObject request = new JsonObject();
        request.addProperty("name", normalizedName);
        request.addProperty("id", recordId);
        request.add("payload", wrappedPayload);

        new CloudWebSocketClient(settings).request("save_config", request);
        ConfigRecord record = new ConfigRecord(recordId, normalizedName, wrappedPayload);
        recordsById.put(recordId, record);
        CloudBootstrapState.putConfig(recordId, normalizedName, wrappedPayload);
    }

    public boolean deleteConfig(String configName) throws IOException {
        ensureConfigured();

        ConfigRecord record = resolveRecord(configName);
        if (record == null) {
            return false;
        }

        JsonObject request = new JsonObject();
        request.addProperty("name", record.name);
        request.addProperty("id", record.id);

        new CloudWebSocketClient(settings).request("delete_config", request);
        recordsById.remove(record.id);
        CloudBootstrapState.removeConfig(record.id);
        return true;
    }

    public String encodeShareCode(String name, JsonObject payload) {
        JsonObject shareObject = new JsonObject();
        shareObject.addProperty("name", normalizeName(name));
        shareObject.add("payload", cloneJsonObject(payload));
        return SHARE_PREFIX + encryptText(GSON.toJson(shareObject));
    }

    public JsonObject decodeShareCode(String code) {
        if (code == null) {
            return null;
        }

        String value = code.trim();
        if (!value.startsWith(SHARE_PREFIX)) {
            return null;
        }

        try {
            JsonElement parsed = PARSER.parse(decryptText(value.substring(SHARE_PREFIX.length())));
            if (parsed != null && parsed.isJsonObject()) {
                return parsed.getAsJsonObject();
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    public JsonObject decodePayloadObject(JsonObject payload) {
        if (payload == null) {
            return null;
        }

        JsonObject decoded = unwrapEncryptedPayload(payload);
        return decoded != null ? decoded : cloneJsonObject(payload);
    }

    private void hydrateFromBootstrap() {
        recordsById.clear();
        for (CloudBootstrapState.ConfigEntry entry : CloudBootstrapState.copyConfigs().values()) {
            recordsById.put(entry.id, new ConfigRecord(entry.id, entry.name, CloudBootstrapState.cloneJsonObject(entry.payload)));
        }
    }

    private JsonObject wrapEncryptedPayload(JsonObject payload) {
        JsonObject wrapper = new JsonObject();
        wrapper.addProperty(PAYLOAD_KEY, encryptText(GSON.toJson(payload)));
        return wrapper;
    }

    private JsonObject unwrapEncryptedPayload(JsonObject payload) {
        JsonElement encryptedElement = payload.get(PAYLOAD_KEY);
        if (encryptedElement == null || !encryptedElement.isJsonPrimitive()) {
            return payload;
        }

        try {
            String decrypted = decryptText(encryptedElement.getAsString());
            JsonElement parsed = PARSER.parse(decrypted);
            if (parsed != null && parsed.isJsonObject()) {
                return parsed.getAsJsonObject();
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    private void loadSettings() {
        settings = CloudSettingsLoader.load();
    }

    private void ensureConfigured() throws IOException {
        if (!settings.isWebSocketConfigured()) {
            throw new IOException("Cloud settings are invalid: set websocketUrl and anonKey");
        }
    }

    private ConfigRecord resolveRecord(String configName) {
        String value = configName == null ? "" : configName.trim();
        if (value.isEmpty()) {
            return null;
        }

        ConfigRecord byId = recordsById.get(buildConfigId(value));
        if (byId != null) {
            return byId;
        }

        for (ConfigRecord record : recordsById.values()) {
            if (record.name.equalsIgnoreCase(value)) {
                return record;
            }
        }
        return null;
    }

    private String buildConfigId(String configName) {
        String normalized = configName == null ? "" : configName.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            normalized = "unnamed";
        }
        return "cfg:" + normalized;
    }

    private String normalizeName(String name) {
        String value = name == null ? "" : name.trim();
        return value.isEmpty() ? "untitled" : value;
    }

    private JsonObject cloneJsonObject(JsonObject source) {
        return CloudBootstrapState.cloneJsonObject(source);
    }

    private String encryptText(String plainText) {
        byte[] input = plainText.getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = xorWithMask(input);
        return Base64.getEncoder().encodeToString(encrypted);
    }

    private String decryptText(String cipherText) {
        byte[] encrypted = Base64.getDecoder().decode(cipherText);
        byte[] plain = xorWithMask(encrypted);
        return new String(plain, StandardCharsets.UTF_8);
    }

    private byte[] xorWithMask(byte[] input) {
        byte[] output = new byte[input.length];
        for (int i = 0; i < input.length; i++) {
            output[i] = (byte) (input[i] ^ MASK_KEY[i % MASK_KEY.length]);
        }
        return output;
    }

    private static final class ConfigRecord {
        final String id;
        final String name;
        final JsonObject payload;

        ConfigRecord(String id, String name, JsonObject payload) {
            this.id = id;
            this.name = name;
            this.payload = payload == null ? new JsonObject() : payload;
        }
    }
}
