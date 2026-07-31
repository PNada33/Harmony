package net.fabricmc.fabric.api.resource;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.IReloadableResourceManager;
import net.minecraft.resources.ResourcePackType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public interface ResourceManagerHelper {
    Map<ResourcePackType, HarmonyResourceManagerHelper> HELPERS = new EnumMap<>(ResourcePackType.class);

    static ResourceManagerHelper get(ResourcePackType type) {
        synchronized (HELPERS) {
            return HELPERS.computeIfAbsent(type, HarmonyResourceManagerHelper::new);
        }
    }

    void registerReloadListener(IdentifiableResourceReloadListener listener);
}

final class HarmonyResourceManagerHelper implements ResourceManagerHelper {
    private final ResourcePackType type;
    private final List<IdentifiableResourceReloadListener> listeners = new ArrayList<>();

    HarmonyResourceManagerHelper(ResourcePackType type) {
        this.type = type;
    }

    @Override
    public void registerReloadListener(IdentifiableResourceReloadListener listener) {
        if (listener == null || this.listeners.contains(listener)) {
            return;
        }

        this.listeners.add(listener);
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft != null
                && this.type == ResourcePackType.CLIENT_RESOURCES
                && minecraft.getResourceManager() instanceof IReloadableResourceManager) {
            ((IReloadableResourceManager) minecraft.getResourceManager()).addReloadListener(listener);
        }
    }

    List<IdentifiableResourceReloadListener> getListeners() {
        return Collections.unmodifiableList(this.listeners);
    }
}
