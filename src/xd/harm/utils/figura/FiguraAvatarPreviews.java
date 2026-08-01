package xd.harm.utils.figura;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.util.ResourceLocation;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Ленивый кэш превью (avatar.png) из <gameDir>/figura/avatars/<folder>.
 * Вызывать только из render-потока.
 */
public final class FiguraAvatarPreviews {

    private static final Map<String, ResourceLocation> CACHE = new HashMap<String, ResourceLocation>();

    private FiguraAvatarPreviews() {
    }

    public static ResourceLocation get(FiguraAvatarLibrary.Entry entry) {
        if (entry == null) {
            return null;
        }
        String key = entry.key();
        if (CACHE.containsKey(key)) {
            return CACHE.get(key);
        }
        ResourceLocation loc = null;
        if (entry.preview != null) {
            loc = load(key, entry.preview);
        }
        if (loc == null && entry.path != null) {
            loc = load(key, entry.path.resolve("avatar.png"));
        }
        if (loc == null && entry.path != null) {
            loc = load(key, entry.path.resolve("texture.png"));
        }
        CACHE.put(key, loc);
        return loc;
    }

    public static ResourceLocation get(String folder) {
        return get(FiguraAvatarLibrary.byFolder(folder));
    }

    private static ResourceLocation load(String key, Path file) {
        if (file == null || !Files.isRegularFile(file)) {
            return null;
        }
        InputStream in = null;
        try {
            in = Files.newInputStream(file);
            NativeImage image = NativeImage.read(in);
            DynamicTexture texture = new DynamicTexture(image);
            String id = "figura_preview_" + key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
            return Minecraft.getInstance().getTextureManager().getDynamicTextureLocation(id, texture);
        } catch (Exception e) {
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    /** Сброс кэша после переустановки набора аватаров. */
    public static void invalidate() {
        try {
            for (ResourceLocation loc : CACHE.values()) {
                if (loc != null) {
                    Minecraft.getInstance().getTextureManager().deleteTexture(loc);
                }
            }
        } catch (Exception ignored) {
        }
        CACHE.clear();
    }
}
