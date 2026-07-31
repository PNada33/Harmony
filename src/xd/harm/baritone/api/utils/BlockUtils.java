package xd.harm.baritone.api.utils;

import net.minecraft.block.Block;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;

import java.util.HashMap;
import java.util.Map;

public class BlockUtils {

    private static transient Map<String, Block> resourceCache = new HashMap<>();

    public static String blockToString(Block block) {
        ResourceLocation loc = Registry.BLOCK.getKey(block);
        String name = loc.getPath();
        if (!loc.getNamespace().equals("minecraft")) {
            name = loc.toString();
        }
        return name;
    }

    public static Block stringToBlockRequired(String name) {
        Block block = stringToBlockNullable(name);

        if (block == null) {
            throw new IllegalArgumentException(String.format("Invalid block name %s", name));
        }

        return block;
    }

    public static Block stringToBlockNullable(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }

        name = name.trim().toLowerCase();

        Block block = resourceCache.get(name);
        if (block != null) {
            return block;
        }
        if (resourceCache.containsKey(name)) {
            return null;
        }

        String fullName = name.contains(":") ? name : "minecraft:" + name;
        ResourceLocation resourceLocation = ResourceLocation.tryCreate(fullName);

        if (resourceLocation == null) {
            resourceLocation = new ResourceLocation("minecraft", name.replace("minecraft:", ""));
        }

        block = Registry.BLOCK.getOptional(resourceLocation).orElse(null);

        Map<String, Block> copy = new HashMap<>(resourceCache);
        copy.put(name, block);
        resourceCache = copy;
        return block;
    }

    private BlockUtils() {}
}