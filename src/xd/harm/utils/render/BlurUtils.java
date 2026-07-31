package xd.harm.utils.render;

import net.minecraft.client.Minecraft;
import net.minecraft.util.math.vector.Vector4f;
import org.lwjgl.opengl.GL11;
import xd.harm.utils.client.IMinecraft;
import xd.harm.utils.render.color.ColorUtils;
import xd.harm.utils.render.rect.RenderUtility;

public class BlurUtils implements IMinecraft {

    private static int lastFrame = -1;

    public static void renderRoundedBlur(float x, float y, float width, float height, float radius) {
        renderRoundedBlur(x, y, width, height, radius, 1f);
    }

    public static void renderRoundedBlur(float x, float y, float width, float height, float radius, float alpha) {
        if (width <= 0 || height <= 0 || alpha <= 0.01f) {
            return;
        }

        updateBlurOnce(3.0f, 3);

        float blurRadius = radius + 0.75f;
        float blurInset = 0.35f;
        float w = width - blurInset * 2f;
        float h = height - blurInset * 2f;
        if (w <= 0 || h <= 0) {
            return;
        }

        int a = Math.max(1, Math.min(255, (int)(255 * alpha)));

        KawaseBlur.blur.render(() -> renderBlurMask(() ->
                RenderUtility.drawRoundedRect(x + blurInset, y + blurInset, w, h,
                        new Vector4f(blurRadius, blurRadius, blurRadius, blurRadius),
                        ColorUtils.rgba(255, 255, 255, a))
        ));
    }

    private static void updateBlurOnce(float offset, int steps) {
        int frame = Minecraft.getInstance().getFrameTimer().getIndex();
        if (frame != lastFrame) {
            KawaseBlur.blur.updateBlur(offset, steps);
            lastFrame = frame;
        }
    }

    private static void renderBlurMask(Runnable draw) {
        GL11.glEnable(0x809D);
        GL11.glEnable(0x809E);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.01f);
        draw.run();
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(0x809E);
        GL11.glDisable(0x809D);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.1f);
    }
}
