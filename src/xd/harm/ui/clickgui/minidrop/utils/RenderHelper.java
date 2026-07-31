package xd.harm.ui.clickgui.minidrop.utils;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import static net.minecraft.client.renderer.vertex.DefaultVertexFormats.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * Утилиты рендеринга для MiniDropDown ClickGui (аналог Quantum RenderHelper)
 * Имена методов совпадают с Quantum: drawRectHorizontalW = вертикальный градиент, drawRectVerticalW = горизонтальный
 */
public class RenderHelper {

    private static final Minecraft mc = Minecraft.getInstance();
    private static final BufferBuilder buffer = Tessellator.getInstance().getBuffer();
    private static final Tessellator tessellator = Tessellator.getInstance();

    // ==================== Прямоугольники ====================

    public static void drawRectW(float x, float y, float width, float height, int color) {
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.defaultBlendFunc();

        buffer.begin(GL_QUADS, POSITION_COLOR);
        buffer.pos(x, y + height, 0).color(color).endVertex();
        buffer.pos(x + width, y + height, 0).color(color).endVertex();
        buffer.pos(x + width, y, 0).color(color).endVertex();
        buffer.pos(x, y, 0).color(color).endVertex();
        tessellator.draw();

        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    /**
     * Вертикальный градиент (сверху color1 → снизу color2) — как в Quantum drawRectHorizontalW
     */
    public static void drawRectHorizontalW(double x, double y, double w, double h, int color1, int color2) {
        w = x + w;
        h = y + h;

        if (x < w) { double i = x; x = w; w = i; }
        if (y < h) { double j = y; y = h; h = j; }

        float[] c1 = ColorUtils.rgba(color1);
        float[] c2 = ColorUtils.rgba(color2);

        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.shadeModel(7425);
        RenderSystem.defaultBlendFunc();
        buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(x, h, 0.0F).color(c2[0], c2[1], c2[2], c2[3]).endVertex();
        buffer.pos(w, h, 0.0F).color(c2[0], c2[1], c2[2], c2[3]).endVertex();
        buffer.pos(w, y, 0.0F).color(c1[0], c1[1], c1[2], c1[3]).endVertex();
        buffer.pos(x, y, 0.0F).color(c1[0], c1[1], c1[2], c1[3]).endVertex();
        buffer.finishDrawing();
        WorldVertexBufferUploader.draw(buffer);
        RenderSystem.shadeModel(7424);
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    /**
     * Горизонтальный градиент (слева color1 → справа color2) — как в Quantum drawRectVerticalW
     */
    public static void drawRectVerticalW(double x, double y, double w, double h, int color1, int color2) {
        w = x + w;
        h = y + h;

        if (x < w) { double i = x; x = w; w = i; }
        if (y < h) { double j = y; y = h; h = j; }

        float[] c1 = ColorUtils.rgba(color1);
        float[] c2 = ColorUtils.rgba(color2);

        RenderSystem.enableBlend();
        RenderSystem.shadeModel(7425);
        RenderSystem.disableTexture();
        RenderSystem.defaultBlendFunc();
        buffer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(x, h, 0.0F).color(c1[0], c1[1], c1[2], c1[3]).endVertex();
        buffer.pos(w, h, 0.0F).color(c2[0], c2[1], c2[2], c2[3]).endVertex();
        buffer.pos(w, y, 0.0F).color(c2[0], c2[1], c2[2], c2[3]).endVertex();
        buffer.pos(x, y, 0.0F).color(c1[0], c1[1], c1[2], c1[3]).endVertex();
        buffer.finishDrawing();
        WorldVertexBufferUploader.draw(buffer);
        RenderSystem.enableTexture();
        RenderSystem.shadeModel(7424);
        RenderSystem.disableBlend();
    }

    // ==================== Скруглённые прямоугольники ====================

    public static void drawRoundedRect(float x, float y, float width, float height, float radius, int color) {
        xd.harm.utils.render.rect.RenderUtility.drawRoundedRect(x, y, width, height, radius, color);
    }

    // ==================== Круги ====================

    public static void drawCircle(float x, float y, float radius, int color) {
        xd.harm.utils.render.rect.RenderUtility.drawCircle(x, y, radius, color);
    }

    // ==================== Тени ====================

    public static void drawShadow(float x, float y, float width, float height, int radius, int color) {
        xd.harm.utils.render.rect.RenderUtility.drawShadow(x, y, width, height, radius, color);
    }

    // ==================== Масштабирование ====================

    public static void scaleStart(float x, float y, float scale) {
        RenderSystem.pushMatrix();
        RenderSystem.translatef(x, y, 0);
        RenderSystem.scalef(scale, scale, 1);
        RenderSystem.translatef(-x, -y, 0);
    }

    public static void scaleEnd() {
        RenderSystem.popMatrix();
    }

    // ==================== Утилиты ====================

    public static void scissor(double x, double y, double width, double height) {
        final double scale = mc.getMainWindow().getGuiScaleFactor();
        y = mc.getMainWindow().getScaledHeight() - y;
        x *= scale;
        y *= scale;
        width *= scale;
        height *= scale;
        GL11.glScissor((int) x, (int) (y - height), (int) width, (int) height);
    }
}
