package xd.harm.ui.clickgui.figura;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.util.ResourceLocation;
import xd.harm.utils.figura.FiguraAvatarInstaller;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Кэш превью (avatar.png) для встроенных Figura-аватаров. */
public final class FiguraAvatarPreviews {

    private static final Map<String, ResourceLocation> TEXTURES = new HashMap<>();
    private static final Set<String> FAILED = new HashSet<>();

    private FiguraAvatarPreviews() {
    }

    /** Возвращает текстуру превью или null. Загружает лениво в главном потоке. */
    public static ResourceLocation get(String folder) {
        ResourceLocation cached = TEXTURES.get(folder);
        if (cached != null) {
            return cached;
        }
        if (FAILED.contains(folder)) {
            return null;
        }
        java.nio.file.Path file = FiguraAvatarInstaller.avatarsDir().resolve(folder).resolve("avatar.png");
        if (!java.nio.file.Files.isRegularFile(file)) {
            FAILED.add(folder);
            return null;
        }
        try (InputStream input = java.nio.file.Files.newInputStream(file)) {            if (input == null) {
                FAILED.add(folder);
                return null;
            }
            NativeImage image = NativeImage.read(input);
            DynamicTexture texture = new DynamicTexture(image);
            ResourceLocation id = Minecraft.getInstance().getTextureManager()
                    .getDynamicTextureLocation("figura_preview_" + Integer.toHexString(folder.hashCode()), texture);
            TEXTURES.put(folder, id);
            return id;
        } catch (Throwable t) {
            FAILED.add(folder);
            return null;
        }
    }
}
