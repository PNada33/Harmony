package xd.harm.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class CloudBackendClient {

    private static final JsonParser PARSER = new JsonParser();

    private CloudBackendClient() {
    }

    public static JsonObject downloadJson(String backendPath) {
        CloudSettingsLoader.CloudSettings settings = loadSettings();
        if (!settings.isConfigured()) {
            return null;
        }

        try {
            URL url = new URL(buildUrl(settings.projectUrl, backendPath));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(settings.timeoutMs);
            connection.setReadTimeout(settings.timeoutMs);
            connection.setUseCaches(false);
            connection.setDoInput(true);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("apikey", settings.anonKey);
            connection.setRequestProperty("Authorization", "Bearer " + settings.anonKey);

            try {
                int statusCode = connection.getResponseCode();
                if (statusCode >= 400) {
                    return null;
                }
                String raw = readStream(connection.getInputStream());
                if (raw == null || raw.trim().isEmpty()) {
                    return null;
                }
                JsonElement parsed = PARSER.parse(raw);
                if (parsed != null && parsed.isJsonObject()) {
                    return parsed.getAsJsonObject();
                }
                return null;
            } finally {
                connection.disconnect();
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String buildUrl(String baseUrl, String backendPath) {
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String path = backendPath.startsWith("/") ? backendPath : "/" + backendPath;
        return base + path;
    }

    private static CloudSettingsLoader.CloudSettings loadSettings() {
        return CloudSettingsLoader.load();
    }

    private static String readStream(InputStream stream) {
        if (stream == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        } catch (Exception ignored) {
            return "";
        }
        return builder.toString();
    }
}
