package xd.harm.config.rotation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.entity.LivingEntity;
import xd.harm.config.CloudBootstrapState;
import xd.harm.modules.impl.combat.HitAura;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public final class HitAuraRotationRuntime {

    private static final HitAuraRotationProvider NO_OP = new NoOpHitAuraRotationProvider();
    private static final String CLASSPATH_FALLBACK_PROVIDER = "server.runtime.ServerHitAuraRotationProvider";
    private static volatile HitAuraRotationProvider provider = NO_OP;

    private HitAuraRotationRuntime() {
    }

    public static void loadFromCloud() {
        loadFromPayload(CloudBootstrapState.copyRuntimePayload());
    }

    public static void loadFromPayload(JsonObject payload) {
        HitAuraRotationProvider loadedProvider = NO_OP;

        try {
            if (payload == null) {
                provider = loadedProvider;
                return;
            }

            JsonObject runtimeObject = resolveRuntimeObject(payload);
            if (runtimeObject == null) {
                provider = loadedProvider;
                return;
            }

            String entryClass = getString(runtimeObject, "entryClass");
            String jarBase64 = getString(runtimeObject, "jarBase64");
            if (entryClass.isEmpty() || jarBase64.isEmpty()) {
                provider = loadedProvider;
                return;
            }

            byte[] jarBytes = Base64.getDecoder().decode(jarBase64);
            String expectedSha256 = getString(runtimeObject, "sha256");
            if (!expectedSha256.isEmpty()) {
                String actualSha256 = sha256Hex(jarBytes);
                if (!expectedSha256.equalsIgnoreCase(actualSha256)) {
                    provider = loadedProvider;
                    return;
                }
            }

            InMemoryJarClassLoader classLoader = new InMemoryJarClassLoader(jarBytes, HitAuraRotationRuntime.class.getClassLoader());
            Class<?> loadedClass = Class.forName(entryClass, true, classLoader);
            if (!HitAuraRotationProvider.class.isAssignableFrom(loadedClass)) {
                provider = loadedProvider;
                return;
            }

            Object instance = loadedClass.getDeclaredConstructor().newInstance();
            loadedProvider = (HitAuraRotationProvider) instance;
        } catch (Throwable ignored) {
            loadedProvider = NO_OP;
        }

        if (loadedProvider == NO_OP) {
            loadedProvider = tryLoadClasspathFallback();
        }

        provider = loadedProvider;
    }

    public static HitAuraRotationProvider getProvider() {
        return provider;
    }

    private static JsonObject resolveRuntimeObject(JsonObject payload) {
        JsonElement runtime = payload.get("runtime");
        if (runtime != null && runtime.isJsonObject()) {
            return runtime.getAsJsonObject();
        }
        return payload;
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

    private static String sha256Hex(byte[] bytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(bytes);
        StringBuilder builder = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            builder.append(String.format(Locale.ROOT, "%02x", b));
        }
        return builder.toString();
    }

    private static HitAuraRotationProvider tryLoadClasspathFallback() {
        try {
            Class<?> loadedClass = Class.forName(CLASSPATH_FALLBACK_PROVIDER, true, HitAuraRotationRuntime.class.getClassLoader());
            if (!HitAuraRotationProvider.class.isAssignableFrom(loadedClass)) {
                return NO_OP;
            }

            Object instance = loadedClass.getDeclaredConstructor().newInstance();
            return (HitAuraRotationProvider) instance;
        } catch (Throwable ignored) {
            return NO_OP;
        }
    }

    private static final class NoOpHitAuraRotationProvider implements HitAuraRotationProvider {
        @Override
        public void onEnable(HitAura hitAura) {
        }

        @Override
        public void onDisable(HitAura hitAura) {
        }

        @Override
        public boolean rotate(HitAura hitAura, LivingEntity target, boolean isAttacking) {
            return false;
        }
    }

    private static final class InMemoryJarClassLoader extends ClassLoader {
        private final Map<String, byte[]> classBytesByName;

        InMemoryJarClassLoader(byte[] jarBytes, ClassLoader parent) throws IOException {
            super(parent);
            this.classBytesByName = extractClassBytes(jarBytes);
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            byte[] bytes = classBytesByName.get(name);
            if (bytes == null) {
                throw new ClassNotFoundException(name);
            }
            return defineClass(name, bytes, 0, bytes.length);
        }

        private static Map<String, byte[]> extractClassBytes(byte[] jarBytes) throws IOException {
            Map<String, byte[]> classes = new HashMap<>();
            try (JarInputStream stream = new JarInputStream(new ByteArrayInputStream(jarBytes))) {
                JarEntry entry;
                while ((entry = stream.getNextJarEntry()) != null) {
                    if (entry.isDirectory()) {
                        continue;
                    }
                    String name = entry.getName();
                    if (!name.endsWith(".class")) {
                        continue;
                    }
                    String className = name.substring(0, name.length() - 6).replace('/', '.');
                    classes.put(className, stream.readAllBytes());
                }
            }
            return classes;
        }
    }
}
