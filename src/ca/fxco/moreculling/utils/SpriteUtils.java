package ca.fxco.moreculling.utils;

import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public final class SpriteUtils
{
    private SpriteUtils()
    {
    }

    public static boolean doesHaveTransparency(TextureAtlasSprite sprite)
    {
        if (sprite == null)
        {
            return true;
        }

        for (int frame = 0; frame < sprite.getFrameCount(); ++frame)
        {
            for (int y = 0; y < sprite.getHeight(); ++y)
            {
                for (int x = 0; x < sprite.getWidth(); ++x)
                {
                    if (NativeImage.getAlpha(sprite.getPixelRGBA(frame, x, y)) == 0)
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static boolean doesHaveTranslucency(TextureAtlasSprite sprite)
    {
        if (sprite == null)
        {
            return true;
        }

        for (int frame = 0; frame < sprite.getFrameCount(); ++frame)
        {
            for (int y = 0; y < sprite.getHeight(); ++y)
            {
                for (int x = 0; x < sprite.getWidth(); ++x)
                {
                    int alpha = NativeImage.getAlpha(sprite.getPixelRGBA(frame, x, y));

                    if (alpha != 0 && alpha != 255)
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
