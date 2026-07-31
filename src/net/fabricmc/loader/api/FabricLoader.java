package net.fabricmc.loader.api;

import net.fabricmc.api.EnvType;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public interface FabricLoader {
    static FabricLoader getInstance() {
        return HarmonyFabricLoader.INSTANCE;
    }

    boolean isModLoaded(String modId);

    EnvType getEnvironmentType();

    Path getConfigDir();

    default File getConfigDirectory() {
        return this.getConfigDir().toFile();
    }

    default Path getGameDir() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft != null && minecraft.gameDir != null ? minecraft.gameDir.toPath() : Paths.get(".");
    }
}

final class HarmonyFabricLoader implements FabricLoader {
    static final HarmonyFabricLoader INSTANCE = new HarmonyFabricLoader();

    private static final Set<String> LOADED_MODS = new HashSet<>(Arrays.asList(
            "chesttracker",
            "whereisit",
            "libgui",
            "cloth-config",
            "cloth-config2",
            "cloth-client-events-v0",
            "fabric",
            "fabricloader"
    ));

    private HarmonyFabricLoader() {
    }

    @Override
    public boolean isModLoaded(String modId) {
        return modId != null && LOADED_MODS.contains(modId.toLowerCase(Locale.ROOT));
    }

    @Override
    public EnvType getEnvironmentType() {
        return EnvType.CLIENT;
    }

    @Override
    public Path getConfigDir() {
        Path configDir = this.getGameDir().resolve("config");
        File file = configDir.toFile();

        if (!file.exists()) {
            file.mkdirs();
        }

        return configDir;
    }
}
