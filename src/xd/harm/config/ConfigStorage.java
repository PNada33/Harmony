package xd.harm.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import xd.harm.ui.notify.NotificationManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ConfigStorage {
    public final Logger logger = Logger.getLogger(ConfigStorage.class.getName());
    private static final JsonParser PARSER = new JsonParser();

    private final CloudConfigClient cloudClient = new CloudConfigClient();

    public void init() throws IOException {
        cloudClient.init();
        setupFolder();
    }

    private void diag(String msg) {
        try {
            Files.write(Paths.get("E:\\Мои Сурсы\\harmony\\config_diag.txt"),
                    (System.currentTimeMillis() + " [ConfigStorage] " + msg + "\n").getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ignored) {}
    }

    public void setupFolder() {
        NotificationManager.setLoadingConfig(true);
        try {
            diag("setupFolder: loading backup...");
            boolean loaded = loadConfiguration("backup");
            if (loaded) {
                logger.log(Level.SEVERE, "Loaded cloud backup configuration...");
                diag("setupFolder: backup loaded OK");
            } else if (saveConfiguration("backup")) {
                logger.log(Level.SEVERE, "Created cloud backup configuration...");
                diag("setupFolder: backup NOT found, CREATED NEW");
            } else {
                logger.log(Level.SEVERE, "Failed to initialize cloud backup configuration");
                diag("setupFolder: FAILED to load or create backup");
            }
        } finally {
            NotificationManager.setLoadingConfig(false);
        }
    }

    public boolean isEmpty() {
        return getConfigs().isEmpty();
    }

    public List<Config> getConfigs() {
        LinkedHashSet<String> configNames = new LinkedHashSet<>();

        try {
            configNames.addAll(cloudClient.listConfigNames());
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to fetch cloud config list", e);
        }

        return configNames.stream().map(Config::new).collect(Collectors.toList());
    }

    public boolean existsConfiguration(String configuration) {
        return findConfig(configuration) != null;
    }

    public boolean loadConfiguration(String configuration) {
        if (configuration == null || configuration.trim().isEmpty()) {
            return false;
        }

        String normalized = configuration.trim();
        NotificationManager.setLoadingConfig(true);

        try {
            diag("downloadConfig(" + normalized + ")...");
            JsonObject object = cloudClient.downloadConfig(normalized);
            if (object == null) {
                diag("downloadConfig(" + normalized + ") returned NULL");
                return false;
            }

            diag("downloadConfig(" + normalized + ") OK, loading into modules...");
            new Config(normalized).loadConfig(object);
            diag("loadConfig(" + normalized + ") applied to modules OK");
            return true;
        } catch (IOException e) {
            diag("downloadConfig IOException: " + e.getMessage());
            logger.log(Level.WARNING, "Failed to load config from cloud: " + normalized, e);
        } catch (NullPointerException pointerException) {
            diag("loadConfig NPE");
            logger.log(Level.WARNING, "Fatal error in Config!", pointerException);
        } catch (Exception e) {
            diag("loadConfig exception: " + e.getMessage());
            logger.log(Level.WARNING, "Failed to load configuration: " + normalized, e);
        } finally {
            NotificationManager.setLoadingConfig(false);
        }

        return false;
    }

    // Возвращает сырой payload конфига (без применения к модулям клиента).
    // Используется ботами, чтобы скопировать настройки в bot_module_config.json.
    public JsonObject getConfigPayload(String configuration) {
        if (configuration == null || configuration.trim().isEmpty()) {
            return null;
        }
        try {
            return cloudClient.downloadConfig(configuration.trim());
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to download config payload: " + configuration, e);
            return null;
        }
    }

    public boolean saveConfiguration(String configuration) {
        if (configuration == null || configuration.trim().isEmpty()) {
            return false;
        }

        String normalized = configuration.trim();
        Config config = new Config(normalized);

        JsonElement savedElement = config.saveConfig();
        if (!savedElement.isJsonObject()) {
            logger.log(Level.WARNING, "Failed to save configuration: not a JSON object");
            return false;
        }

        JsonObject jsonObject = savedElement.getAsJsonObject();

        try {
            cloudClient.uploadConfig(normalized, jsonObject);
            return true;
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to upload config to cloud: " + normalized, e);
            return false;
        }
    }

    public boolean removeConfiguration(String configuration) {
        if (configuration == null || configuration.trim().isEmpty()) {
            return false;
        }

        try {
            return cloudClient.deleteConfig(configuration.trim());
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to delete cloud config: " + configuration, e);
            return false;
        }
    }

    public boolean copyConfiguration(String sourceConfiguration, String targetConfiguration) {
        if (sourceConfiguration == null || sourceConfiguration.trim().isEmpty()
                || targetConfiguration == null || targetConfiguration.trim().isEmpty()) {
            return false;
        }

        String source = sourceConfiguration.trim();
        String target = targetConfiguration.trim();
        try {
            JsonObject payload = cloudClient.downloadConfig(source);
            if (payload == null) {
                return false;
            }
            cloudClient.uploadConfig(target, cloneJsonObject(payload));
            return true;
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to copy cloud config: " + source + " -> " + target, e);
            return false;
        }
    }

    public boolean importConfiguration(String configuration) {
        return importFromCode(configuration, "imported-config") > 0;
    }

    public boolean exportConfiguration(String configuration) {
        return exportConfigurationCode(configuration) != null;
    }

    public int importFromCode(String rawCode, String fallbackName) {
        String raw = rawCode == null ? "" : rawCode.trim();
        if (raw.isEmpty()) {
            return 0;
        }

        List<ImportEntry> entries;
        try {
            entries = parseImportEntries(raw, fallbackName);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to parse import code", e);
            return 0;
        }

        int imported = 0;
        for (ImportEntry entry : entries) {
            try {
                cloudClient.uploadConfig(entry.name, entry.payload);
                imported++;
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to import config from code: " + entry.name, e);
            }
        }
        return imported;
    }

    public String exportConfigurationCode(String configuration) {
        if (configuration == null || configuration.trim().isEmpty()) {
            return null;
        }

        String normalized = configuration.trim();
        try {
            JsonObject payload = cloudClient.downloadConfig(normalized);
            if (payload == null) {
                return null;
            }
            return cloudClient.encodeShareCode(normalized, payload);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to export config to share code: " + normalized, e);
            return null;
        }
    }

    public int clearConfigurations() {
        List<String> names = getConfigs().stream().map(Config::getName).collect(Collectors.toCollection(ArrayList::new));
        int deleted = 0;

        for (String name : names) {
            if ("backup".equalsIgnoreCase(name)) {
                continue;
            }
            if (removeConfiguration(name)) {
                deleted++;
            }
        }

        return deleted;
    }

    public Config findConfig(String configName) {
        if (configName == null || configName.trim().isEmpty()) {
            return null;
        }

        try {
            if (cloudClient.existsConfig(configName)) {
                return new Config(configName);
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to check cloud config existence: " + configName, e);
        }

        return null;
    }

    private List<ImportEntry> parseImportEntries(String rawCode, String fallbackName) {
        String fallback = (fallbackName == null || fallbackName.trim().isEmpty()) ? "imported-config" : fallbackName.trim();
        List<ImportEntry> result = new ArrayList<>();

        JsonObject shared = cloudClient.decodeShareCode(rawCode);
        if (shared != null) {
            result.add(normalizeImportEntry(shared, fallback));
        }

        // Server/share flow only: import accepts HCFG1 codes only.
        return result;
    }

    private ImportEntry normalizeImportEntry(JsonObject source, String fallbackName) {
        String name = pickImportName(source, fallbackName);
        JsonObject payload = extractImportPayload(source);
        return new ImportEntry(name, payload);
    }

    private JsonObject extractImportPayload(JsonObject source) {
        if (source == null) {
            return new JsonObject();
        }

        JsonElement encodedElement = source.get("enc_v1");
        if (encodedElement != null && encodedElement.isJsonPrimitive() && encodedElement.getAsJsonPrimitive().isString()) {
            JsonObject decoded = cloudClient.decodePayloadObject(source);
            if (decoded != null) {
                return decoded;
            }
        }

        JsonElement payloadElement = source.get("payload");
        if (payloadElement != null && payloadElement.isJsonObject()) {
            return cloneJsonObject(payloadElement.getAsJsonObject());
        }

        JsonElement configElement = source.get("config");
        if (configElement != null && configElement.isJsonObject()) {
            return cloneJsonObject(configElement.getAsJsonObject());
        }

        JsonElement dataElement = source.get("data");
        if (dataElement != null && dataElement.isJsonObject()) {
            JsonObject dataObject = dataElement.getAsJsonObject();
            JsonElement dataPayload = dataObject.get("payload");
            if (dataPayload != null && dataPayload.isJsonObject()) {
                return cloneJsonObject(dataPayload.getAsJsonObject());
            }
            JsonElement dataConfig = dataObject.get("config");
            if (dataConfig != null && dataConfig.isJsonObject()) {
                return cloneJsonObject(dataConfig.getAsJsonObject());
            }
            return cloneJsonObject(dataObject);
        }

        return cloneJsonObject(source);
    }

    private String pickImportName(JsonObject source, String fallbackName) {
        if (source != null) {
            String[] keys = {"name", "configName", "title"};
            for (String key : keys) {
                JsonElement element = source.get(key);
                if (element != null && element.isJsonPrimitive()) {
                    try {
                        String value = element.getAsString();
                        if (value != null && !value.trim().isEmpty()) {
                            return value.trim();
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        return fallbackName == null || fallbackName.trim().isEmpty() ? "imported-config" : fallbackName.trim();
    }

    private static final class ImportEntry {
        final String name;
        final JsonObject payload;

        ImportEntry(String name, JsonObject payload) {
            this.name = name;
            this.payload = payload == null ? new JsonObject() : payload;
        }
    }

    private JsonObject cloneJsonObject(JsonObject source) {
        if (source == null) {
            return new JsonObject();
        }

        try {
            JsonElement parsed = PARSER.parse(source.toString());
            if (parsed != null && parsed.isJsonObject()) {
                return parsed.getAsJsonObject();
            }
        } catch (Exception ignored) {
        }

        return new JsonObject();
    }
}
