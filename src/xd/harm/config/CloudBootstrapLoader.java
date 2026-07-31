package xd.harm.config;

import com.google.gson.JsonObject;
import xd.harm.config.rotation.HitAuraRotationRuntime;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public final class CloudBootstrapLoader {

    private static void diag(String msg) {
        try {
            Files.write(Paths.get("E:\\Мои Сурсы\\harmony\\config_diag.txt"),
                    (System.currentTimeMillis() + " [Bootstrap] " + msg + "\n").getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ignored) {}
    }

    private static final Object LOCK = new Object();
    private static volatile boolean attempted;
    private static volatile boolean loaded;
    private static volatile RuntimeException failure;

    private CloudBootstrapLoader() {
    }

    public static void preload() {
        if (loaded) {
            return;
        }
        if (failure != null) {
            throw failure;
        }

        synchronized (LOCK) {
            if (loaded) {
                return;
            }
            if (failure != null) {
                throw failure;
            }
            attempted = true;
        }

        try {
            CloudSettingsLoader.CloudSettings settings = CloudSettingsLoader.load();
            if (!settings.isWebSocketConfigured()) {
                throw new IllegalStateException("Cloud bootstrap failed: websocketUrl or anonKey is not configured");
            }

            CloudWebSocketClient client = new CloudWebSocketClient(settings);
            JsonObject response = client.request("bootstrap", new JsonObject());
            validateBootstrap(response);
            CloudBootstrapState.applyBootstrap(response);
            int configCount = response.has("configs") ? response.getAsJsonArray("configs").size() : 0;
            diag("preload OK, received " + configCount + " configs");
            HitAuraRotationCloud.loadFromPayload(CloudBootstrapState.copyRotationPayload());
            HitAuraRotationRuntime.loadFromPayload(CloudBootstrapState.copyRuntimePayload());
            loaded = true;
            failure = null;
        } catch (Exception exception) {
            RuntimeException wrapped = exception instanceof RuntimeException
                    ? (RuntimeException) exception
                    : new IllegalStateException("Cloud bootstrap failed", exception);
            failure = wrapped;
            throw wrapped;
        }
    }

    private static void validateBootstrap(JsonObject response) {
        if (response == null || response.entrySet().isEmpty()) {
            throw new IllegalStateException("Cloud bootstrap failed: empty bootstrap response");
        }

        JsonObject rotations = getObject(response, "rotations");
        if (rotations.entrySet().isEmpty()) {
            throw new IllegalStateException("Cloud bootstrap failed: rotations payload is empty");
        }

        JsonObject runtime = getObject(response, "runtime");
        if (runtime.entrySet().isEmpty()) {
            throw new IllegalStateException("Cloud bootstrap failed: runtime payload is empty");
        }
    }

    private static JsonObject getObject(JsonObject object, String key) {
        if (object == null) {
            return new JsonObject();
        }
        if (!object.has(key) || !object.get(key).isJsonObject()) {
            return new JsonObject();
        }
        return object.getAsJsonObject(key);
    }
}
