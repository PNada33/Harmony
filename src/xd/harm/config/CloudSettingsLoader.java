package xd.harm.config;

import java.nio.charset.StandardCharsets;

final class CloudSettingsLoader {

    private static final int[] OBFUSCATION_MASK_DATA = {
            72, 97, 114, 109, 111, 110, 121, 67, 108, 111, 117, 100, 83, 101, 101, 100, 86, 50
    };
    private static final byte[] OBFUSCATION_MASK = toByteArray(OBFUSCATION_MASK_DATA);
    private static final int[] DEFAULT_PROJECT_URL_DATA = {
            32, 21, 6, 29, 85, 65, 86, 114, 94, 88, 91, 84, 125, 85, 75, 85, 108, 7, 124, 82, 64, 92
    };
    private static final int[] DEFAULT_ANON_KEY_DATA = {
            32, 0, 0, 0, 0, 0, 0, 110, 0, 0, 22, 5, 63, 72, 1, 1, 32, 31, 35, 4, 11
    };
    private static final int[] DEFAULT_TABLE_DATA = {
            43, 14, 28, 11, 6, 9, 10
    };

    static final String DEFAULT_PROJECT_URL = decodeDefault(DEFAULT_PROJECT_URL_DATA);
    static final String DEFAULT_ANON_KEY = decodeDefault(DEFAULT_ANON_KEY_DATA);
    static final String DEFAULT_TABLE = decodeDefault(DEFAULT_TABLE_DATA);
    static final String DEFAULT_WEBSOCKET_URL = "ws://127.0.0.1:54321/ws/cloud";
    static final int DEFAULT_TIMEOUT_MS = 4000;

    private CloudSettingsLoader() {
    }

    static CloudSettings load() {
        CloudSettings settings = new CloudSettings();
        applyOverrideSettings(settings);
        sanitize(settings);
        return settings;
    }

    private static void applyOverrideSettings(CloudSettings settings) {
        settings.projectUrl = readSetting("harmony.cloud.url", "HARMONY_CLOUD_PROJECT_URL", settings.projectUrl);
        settings.websocketUrl = readSetting("harmony.cloud.wsUrl", "HARMONY_CLOUD_WS_URL", settings.websocketUrl);
        settings.anonKey = readSetting("harmony.cloud.anonKey", "HARMONY_CLOUD_ANON_KEY", settings.anonKey);
        settings.table = readSetting("harmony.cloud.table", "HARMONY_CLOUD_TABLE", settings.table);
        settings.timeoutMs = readSettingInt("harmony.cloud.timeoutMs", "HARMONY_CLOUD_TIMEOUT_MS", settings.timeoutMs);
    }

    private static void sanitize(CloudSettings settings) {
        if (settings.projectUrl != null) {
            settings.projectUrl = settings.projectUrl.trim();
        }
        if (settings.websocketUrl == null || settings.websocketUrl.trim().isEmpty()) {
            settings.websocketUrl = DEFAULT_WEBSOCKET_URL;
        } else {
            settings.websocketUrl = settings.websocketUrl.trim();
        }
        if (settings.anonKey != null) {
            settings.anonKey = settings.anonKey.trim();
        }
        if (settings.table == null || settings.table.trim().isEmpty()) {
            settings.table = DEFAULT_TABLE;
        } else {
            settings.table = settings.table.trim();
        }
        if (settings.timeoutMs <= 0) {
            settings.timeoutMs = DEFAULT_TIMEOUT_MS;
        }
    }

    private static String readSetting(String propertyKey, String envKey, String defaultValue) {
        String propertyValue = System.getProperty(propertyKey);
        if (propertyValue != null && !propertyValue.trim().isEmpty()) {
            return propertyValue.trim();
        }

        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.trim().isEmpty()) {
            return envValue.trim();
        }

        return defaultValue;
    }

    private static int readSettingInt(String propertyKey, String envKey, int defaultValue) {
        String propertyValue = System.getProperty(propertyKey);
        if (propertyValue != null && !propertyValue.trim().isEmpty()) {
            try {
                return Integer.parseInt(propertyValue.trim());
            } catch (Exception ignored) {
            }
        }

        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.trim().isEmpty()) {
            try {
                return Integer.parseInt(envValue.trim());
            } catch (Exception ignored) {
            }
        }

        return defaultValue;
    }

    private static String decodeDefault(int[] data) {
        return xorToString(toByteArray(data));
    }

    private static String xorToString(byte[] data) {
        byte[] decoded = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            decoded[i] = (byte) (data[i] ^ OBFUSCATION_MASK[i % OBFUSCATION_MASK.length]);
        }
        return new String(decoded, StandardCharsets.UTF_8);
    }

    private static byte[] toByteArray(int[] values) {
        byte[] bytes = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            bytes[i] = (byte) values[i];
        }
        return bytes;
    }

    static final class CloudSettings {
        String projectUrl = DEFAULT_PROJECT_URL;
        String websocketUrl = DEFAULT_WEBSOCKET_URL;
        String anonKey = DEFAULT_ANON_KEY;
        String table = DEFAULT_TABLE;
        int timeoutMs = DEFAULT_TIMEOUT_MS;

        boolean isConfigured() {
            return projectUrl != null
                    && !projectUrl.trim().isEmpty()
                    && anonKey != null
                    && !anonKey.trim().isEmpty();
        }

        boolean isWebSocketConfigured() {
            return websocketUrl != null
                    && !websocketUrl.trim().isEmpty()
                    && anonKey != null
                    && !anonKey.trim().isEmpty();
        }
    }
}
