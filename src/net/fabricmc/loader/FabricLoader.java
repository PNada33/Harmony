package net.fabricmc.loader;

public final class FabricLoader {
    private FabricLoader() {
    }

    public static net.fabricmc.loader.api.FabricLoader getInstance() {
        return net.fabricmc.loader.api.FabricLoader.getInstance();
    }
}
