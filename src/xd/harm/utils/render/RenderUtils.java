package xd.harm.utils.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;
import xd.harm.utils.client.ScaledResolution;
import xd.harm.utils.render.color.ColorUtils;
import xd.harm.utils.math.MathUtil;

import java.awt.Color;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.List;

public class RenderUtils {
    protected static final Minecraft mc = Minecraft.getInstance();
    private static final ResourceLocation ITEM_WARN_DUR = new ResourceLocation("");
    public static final IntBuffer viewport = GLAllocation.createDirectByteBuffer(16 << 2).asIntBuffer();
    public static final FloatBuffer modelview = GLAllocation.createDirectFloatBuffer(16);
    public static final FloatBuffer projection = GLAllocation.createDirectFloatBuffer(16);
    public static final FloatBuffer vector = GLAllocation.createDirectFloatBuffer(4);
    public static final Tessellator tessellator = Tessellator.getInstance();
    public static final BufferBuilder buffer = tessellator.getBuffer();

    public static void drawBlockESP(BlockPos pos, Color color) {
        if (pos == null || mc.world == null) return;

        double x = pos.getX() - mc.getRenderManager().info.getProjectedView().getX();
        double y = pos.getY() - mc.getRenderManager().info.getProjectedView().getY();
        double z = pos.getZ() - mc.getRenderManager().info.getProjectedView().getZ();

        AxisAlignedBB box = new AxisAlignedBB(x, y, z, x + 1, y + 1, z + 1);

        drawFilledBox(box, color);
        drawOutlinedBox(box, color);
    }

    public static void drawFilledBox(AxisAlignedBB box, Color color) {
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);

        float r = color.getRed() / 255.0F;
        float g = color.getGreen() / 255.0F;
        float b = color.getBlue() / 255.0F;
        float a = 0.3F;

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        buffer.pos(box.minX, box.minY, box.minZ).color(r, g, b, a).endVertex();
        buffer.pos(box.maxX, box.minY, box.minZ).color(r, g, b, a).endVertex();
        buffer.pos(box.maxX, box.minY, box.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(box.minX, box.minY, box.maxZ).color(r, g, b, a).endVertex();

        buffer.pos(box.minX, box.maxY, box.minZ).color(r, g, b, a).endVertex();
        buffer.pos(box.minX, box.maxY, box.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(box.maxX, box.maxY, box.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(box.maxX, box.maxY, box.minZ).color(r, g, b, a).endVertex();

        buffer.pos(box.minX, box.minY, box.minZ).color(r, g, b, a).endVertex();
        buffer.pos(box.minX, box.maxY, box.minZ).color(r, g, b, a).endVertex();
        buffer.pos(box.maxX, box.maxY, box.minZ).color(r, g, b, a).endVertex();
        buffer.pos(box.maxX, box.minY, box.minZ).color(r, g, b, a).endVertex();

        buffer.pos(box.minX, box.minY, box.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(box.maxX, box.minY, box.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(box.maxX, box.maxY, box.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(box.minX, box.maxY, box.maxZ).color(r, g, b, a).endVertex();

        buffer.pos(box.minX, box.minY, box.minZ).color(r, g, b, a).endVertex();
        buffer.pos(box.minX, box.minY, box.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(box.minX, box.maxY, box.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(box.minX, box.maxY, box.minZ).color(r, g, b, a).endVertex();

        buffer.pos(box.maxX, box.minY, box.minZ).color(r, g, b, a).endVertex();
        buffer.pos(box.maxX, box.maxY, box.minZ).color(r, g, b, a).endVertex();
        buffer.pos(box.maxX, box.maxY, box.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(box.maxX, box.minY, box.maxZ).color(r, g, b, a).endVertex();

        tessellator.draw();

        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    public static void drawOutlinedBox(AxisAlignedBB box, Color color) {
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glLineWidth(2.0F);

        float r = color.getRed() / 255.0F;
        float g = color.getGreen() / 255.0F;
        float b = color.getBlue() / 255.0F;
        float a = 1.0F;

        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);

        buffer.pos(box.minX, box.minY, box.minZ).color(r, g, b, a).endVertex();
        buffer.pos(box.maxX, box.minY, box.minZ).color(r, g, b, a).endVertex();
        buffer.pos(box.maxX, box.minY, box.minZ).color(r, g, b, a).endVertex();
        buffer.pos(box.maxX, box.minY, box.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(box.maxX, box.minY, box.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(box.minX, box.minY, box.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(box.minX, box.minY, box.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(box.minX, box.minY, box.minZ).color(r, g, b, a).endVertex();

        buffer.pos(box.minX, box.maxY, box.minZ).color(r, g, b, a).endVertex();
        buffer.pos(box.maxX, box.maxY, box.minZ).color(r, g, b, a).endVertex();
        buffer.pos(box.maxX, box.maxY, box.minZ).color(r, g, b, a).endVertex();
        buffer.pos(box.maxX, box.maxY, box.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(box.maxX, box.maxY, box.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(box.minX, box.maxY, box.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(box.minX, box.maxY, box.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(box.minX, box.maxY, box.minZ).color(r, g, b, a).endVertex();

        buffer.pos(box.minX, box.minY, box.minZ).color(r, g, b, a).endVertex();
        buffer.pos(box.minX, box.maxY, box.minZ).color(r, g, b, a).endVertex();
        buffer.pos(box.maxX, box.minY, box.minZ).color(r, g, b, a).endVertex();
        buffer.pos(box.maxX, box.maxY, box.minZ).color(r, g, b, a).endVertex();
        buffer.pos(box.maxX, box.minY, box.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(box.maxX, box.maxY, box.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(box.minX, box.minY, box.maxZ).color(r, g, b, a).endVertex();
        buffer.pos(box.minX, box.maxY, box.maxZ).color(r, g, b, a).endVertex();

        tessellator.draw();

        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    public static void glRenderStart() {
        GL11.glPushMatrix();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
    }

    public static void glRenderStop() {
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        GL11.glPopMatrix();
    }

    public static void anialisON(boolean line, boolean polygon, boolean point) {
        if (line) {
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        }
        if (polygon) {
            GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
            GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT, GL11.GL_NICEST);
        }
        if (point) {
            GL11.glEnable(GL11.GL_POINT_SMOOTH);
            GL11.glHint(GL11.GL_POINT_SMOOTH_HINT, GL11.GL_NICEST);
        }
    }

    public static void anialisOFF(boolean line, boolean polygon, boolean point) {
        if (line) {
            GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_DONT_CARE);
            GL11.glDisable(GL11.GL_LINE_SMOOTH);
        }
        if (polygon) {
            GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT, GL11.GL_DONT_CARE);
            GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
        }
        if (point) {
            GL11.glHint(GL11.GL_POINT_SMOOTH_HINT, GL11.GL_DONT_CARE);
            GL11.glDisable(GL11.GL_POINT_SMOOTH);
        }
    }

    public static int red(int color) {
        return color >> 16 & 255;
    }

    public static int green(int color) {
        return color >> 8 & 255;
    }

    public static int blue(int color) {
        return color & 255;
    }

    public static int alpha(int color) {
        return color >> 24 & 255;
    }

    public static void drawRect(double x, double y, double x2, double y2, int color) {
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.defaultBlendFunc();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(x2, y, 0.0).color(color).endVertex();
        buffer.pos(x, y, 0.0).color(color).endVertex();
        buffer.pos(x, y2, 0.0).color(color).endVertex();
        buffer.pos(x2, y2, 0.0).color(color).endVertex();
        tessellator.draw();
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    public static void drawAlphedRect(double x, double y, double x2, double y2, int color) {
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(x, y, 0.0).color(color).endVertex();
        buffer.pos(x2, y, 0.0).color(color).endVertex();
        buffer.pos(x2, y2, 0.0).color(color).endVertex();
        buffer.pos(x, y2, 0.0).color(color).endVertex();
        tessellator.draw();
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    public static void drawSmoothCircle(double x, double y, float radius, int color) {
        runGLColor(color);
        setup2D(() -> {
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glEnable(GL11.GL_POINT_SMOOTH);
            GL11.glPointSize(radius * ScaledResolution.getScaleFactor() * 2.0F);
            renderObj(GL11.GL_POINTS, () -> GL11.glVertex2d(x, y));
            GL11.glEnable(GL11.GL_ALPHA_TEST);
        });
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void drawSmoothCircle(double x, double y, float radius, int color, boolean bloom) {
        runGLColor(color);
        setup2D(() -> {
            if (bloom) {
                RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            }
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glEnable(GL11.GL_POINT_SMOOTH);
            GL11.glHint(GL11.GL_POINT_SMOOTH_HINT, GL11.GL_NICEST);
            float scale = (float) (ScaledResolution.getScaleFactor() / Math.pow(ScaledResolution.getScaleFactor(), 2.0D));
            GL11.glPointSize(radius / scale * 2.0F);
            renderObj(GL11.GL_POINTS, () -> GL11.glVertex2d(x, y));
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            if (bloom) {
                RenderSystem.defaultBlendFunc();
            }
        });
    }

    public static void drawLightContureRect(double x, double y, double x2, double y2, int color) {
        drawAlphedRect(x - 0.5D, y - 0.5D, x2 + 0.5D, y, color);
        drawAlphedRect(x - 0.5D, y2, x2 + 0.5D, y2 + 0.5D, color);
        drawAlphedRect(x - 0.5D, y, x, y2, color);
        drawAlphedRect(x2, y, x2 + 0.5D, y2, color);
    }

    public static void drawLightContureRectSmooth(double x, double y, double x2, double y2, int color) {
        drawAlphedRect(x, y - 0.5D, x2, y, color);
        drawAlphedRect(x, y2, x2, y2 + 0.5D, color);
        drawAlphedRect(x - 0.5D, y, x, y2, color);
        drawAlphedRect(x2, y, x2 + 0.5D, y2, color);
    }

    public static void drawLightContureRectSidewaysSmooth(double x, double y, double x2, double y2, int color, int color2) {
        drawAlphedSideways(x, y - 0.5D, x2, y, color, color2);
        drawAlphedSideways(x, y2, x2, y2 + 0.5D, color, color2);
        drawAlphedRect(x - 0.5D, y, x, y2, color);
        drawAlphedRect(x2, y, x2 + 0.5D, y2, color2);
    }

    public static void drawFullGradientRectPro(float x, float y, float x2, float y2, int color, int color2, int color3, int color4, boolean blend) {
        RenderSystem.enableBlend();
        if (blend) {
            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        }
        RenderSystem.disableTexture();
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(x2, y, 0.0).color(color3).endVertex();
        buffer.pos(x, y, 0.0).color(color4).endVertex();
        buffer.pos(x, y2, 0.0).color(color).endVertex();
        buffer.pos(x2, y2, 0.0).color(color2).endVertex();
        tessellator.draw();
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glShadeModel(GL11.GL_FLAT);
        RenderSystem.enableTexture();
        if (blend) {
            RenderSystem.defaultBlendFunc();
        }
        RenderSystem.disableBlend();
    }

    public static void drawAlphedSideways(double left, double top, double right, double bottom, int col1, int col2, boolean bloom) {
        if (bloom) {
            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        }
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(left, top, 0.0).color(col1).endVertex();
        buffer.pos(left, bottom, 0.0).color(col1).endVertex();
        buffer.pos(right, bottom, 0.0).color(col2).endVertex();
        buffer.pos(right, top, 0.0).color(col2).endVertex();
        tessellator.draw();
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glShadeModel(GL11.GL_FLAT);
        RenderSystem.enableTexture();
        if (bloom) {
            RenderSystem.defaultBlendFunc();
        }
        RenderSystem.disableBlend();
    }

    public static void drawAlphedSideways(double left, double top, double right, double bottom, int col1, int col2) {
        drawAlphedSideways(left < right ? left : right, top, left >= right ? left : right, bottom, col1, col2, false);
    }

    public static void drawAlphedGradient(double x, double y, double x2, double y2, int col1, int col2) {
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(x2, y, 0.0).color(col1).endVertex();
        buffer.pos(x, y, 0.0).color(col1).endVertex();
        buffer.pos(x, y2, 0.0).color(col2).endVertex();
        buffer.pos(x2, y2, 0.0).color(col2).endVertex();
        tessellator.draw();
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glShadeModel(GL11.GL_FLAT);
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    public static void drawVec2Colored(List<Vec2fColored> pos) {
        drawVec2Colored(pos, GL11.GL_POLYGON, 1.0F);
    }

    public static void drawVec2Colored(List<Vec2fColored> pos, int begin) {
        drawVec2Colored(pos, begin, 1.0F);
    }

    public static void drawVec2Colored(List<Vec2fColored> pos, int begin, float alphaPC) {
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        if (!blend) {
            GL11.glEnable(GL11.GL_BLEND);
        }
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_LIGHTING);
        if (begin != 0) {
            GL11.glShadeModel(GL11.GL_SMOOTH);
        }
        anialisON(begin == GL11.GL_LINE_STRIP || begin == GL11.GL_LINES || begin == GL11.GL_LINE_LOOP, begin != 0, begin == 0);
        if (!pos.isEmpty()) {
            buffer.begin(begin, DefaultVertexFormats.POSITION_COLOR);
            for (Vec2fColored vec : pos) {
                int c = ColorUtils.swapAlpha(vec.color, (float) ColorUtils.getAlphaFromColor(vec.color) * alphaPC);
                buffer.pos(vec.x, vec.y, 0.0).color(c).endVertex();
            }
            tessellator.draw();
        }
        anialisOFF(begin == GL11.GL_LINE_STRIP || begin == GL11.GL_LINES || begin == GL11.GL_LINE_LOOP, begin != 0, false);
        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        if (!blend) {
            GL11.glDisable(GL11.GL_BLEND);
        }
    }

    public static void drawVec2ColoredNoSmooth(List<Vec2fColored> pos, int begin) {
        drawVec2ColoredNoSmooth(pos, begin, 1.0F);
    }

    public static void drawVec2ColoredNoSmooth(List<Vec2fColored> pos, int begin, float alphaPC) {
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        if (!blend) {
            GL11.glEnable(GL11.GL_BLEND);
        }
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glShadeModel(GL11.GL_FLAT);
        if (!pos.isEmpty()) {
            buffer.begin(begin, DefaultVertexFormats.POSITION_COLOR);
            for (Vec2fColored vec : pos) {
                int c = ColorUtils.swapAlpha(vec.color, Math.min((float) ColorUtils.getAlphaFromColor(vec.color) * alphaPC, 255.0F));
                buffer.pos(vec.x, vec.y, 0.0).color(c).endVertex();
            }
            tessellator.draw();
        }
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        if (!blend) {
            GL11.glDisable(GL11.GL_BLEND);
        }
    }

    public static void drawPolygonPartsGlowBackSAlpha(double x, double y, float radius, int part, int color, int endcolor, float alpha, boolean bloom) {
        float red = (float) (color >> 16 & 255) / 255.0F;
        float green = (float) (color >> 8 & 255) / 255.0F;
        float blue = (float) (color & 255) / 255.0F;
        float alpha1 = (float) (endcolor >> 24 & 255) / 255.0F;
        float red1 = (float) (endcolor >> 16 & 255) / 255.0F;
        float green1 = (float) (endcolor >> 8 & 255) / 255.0F;
        float blue1 = (float) (endcolor & 255) / 255.0F;
        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.disableAlphaTest();
        if (bloom) {
            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        } else {
            RenderSystem.defaultBlendFunc();
        }
        GL11.glShadeModel(GL11.GL_SMOOTH);
        buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(x, y, 0.0).color(red, green, blue, alpha).endVertex();
        for (int i = part * 90; i <= part * 90 + 90; i += 6) {
            float angle = (float) (Math.PI * 2F) * i / 360.0F + (float) Math.toRadians(30.0F);
            buffer.pos(x + MathHelper.sin(angle) * radius, y + MathHelper.cos(angle) * radius, 0.0).color(red1, green1, blue1, alpha1).endVertex();
        }
        tessellator.draw();
        if (bloom) {
            RenderSystem.defaultBlendFunc();
        }
        GL11.glShadeModel(GL11.GL_FLAT);
        RenderSystem.enableAlphaTest();
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    public static void drawPolygonParts(double x, double y, float radius, int part, int color, int endcolor, boolean bloom) {
        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.disableAlphaTest();
        if (bloom) {
            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        } else {
            RenderSystem.defaultBlendFunc();
        }
        GL11.glShadeModel(GL11.GL_SMOOTH);
        buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(x, y, 0.0).color(color).endVertex();
        for (int i = part * 90; i <= part * 90 + 90; i += 18) {
            float angle = (float) (Math.PI * 2F) * i / 360.0F + (float) Math.toRadians(180.0F);
            buffer.pos(x + MathHelper.sin(angle) * radius, y + MathHelper.cos(angle) * radius, 0.0).color(endcolor).endVertex();
        }
        tessellator.draw();
        if (bloom) {
            RenderSystem.defaultBlendFunc();
        }
        GL11.glShadeModel(GL11.GL_FLAT);
        RenderSystem.enableAlphaTest();
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    public static void drawPolygonPart(double x, double y, int radius, int part, int color, int endcolor) {
        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.disableAlphaTest();
        RenderSystem.defaultBlendFunc();
        GL11.glShadeModel(GL11.GL_SMOOTH);
        buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(x, y, 0.0).color(color).endVertex();
        for (int i = part * 90; i <= part * 90 + 90; ++i) {
            float angle = (float) (Math.PI * 2F) * i / 360.0F + (float) Math.toRadians(180.0F);
            buffer.pos(x + MathHelper.sin(angle) * radius, y + MathHelper.cos(angle) * radius, 0.0).color(endcolor).endVertex();
        }
        tessellator.draw();
        GL11.glShadeModel(GL11.GL_FLAT);
        RenderSystem.enableAlphaTest();
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    public static void drawCircledTHud(float cx, double cy, float r, float percent, int color, float alpha, float lineWidth) {
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        cx *= 2.0F;
        cy *= 2.0;
        float theta = 0.0175F;
        float p = MathHelper.cos(theta);
        float s = MathHelper.sin(theta);
        float x = r * 2.0F;
        float y = 0.0F;
        enableGL2D();
        GL11.glScalef(0.5F, 0.5F, 0.5F);
        GL11.glLineWidth(lineWidth);
        buffer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (float ii = 0.0F; ii < 360.0F * percent; ++ii) {
            buffer.pos(x + cx, y + cy, 0.0).color(ColorUtils.swapAlpha(color, alpha)).endVertex();
            float t = x;
            x = p * x - s * y;
            y = s * t + p * y;
        }
        tessellator.draw();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glScalef(2.0F, 2.0F, 2.0F);
        disableGL2D();
        RenderSystem.enableBlend();
        GL11.glPopMatrix();
    }

    public static void drawCircledTHudWithOverallColor(float cx, double cy, float r, float percent, int color, float alpha, float lineWidth, int color2, float pcColor2) {
        int c = blendColor(color, color2, pcColor2);
        drawCircledTHud(cx, cy, r, percent, c, alpha, lineWidth);
    }

    public static void customScaledObject2D(float oXpos, float oYpos, float oWidth, float oHeight, float oScale) {
        GL11.glTranslated(oWidth / 2.0F, oHeight / 2.0F, 1.0F);
        GL11.glTranslated(-oXpos * oScale + oXpos + oWidth / 2.0F * -oScale, -oYpos * oScale + oYpos + oHeight / 2.0F * -oScale, 0.0F);
        GL11.glScaled(oScale, oScale, 1.0F);
    }

    public static void customScaledObject2DCoords(float oXpos, float oYpos, float oXpos2, float oYpos2, float oScale) {
        customScaledObject2D(oXpos, oYpos, oXpos2 - oXpos, oYpos2 - oYpos, oScale);
    }

    public static void customScaledObject2DCoordsPro(float oXpos, float oYpos, float oXpos2, float oYpos2, float oScaleX, float oScaleY) {
        customScaledObject2DPro(oXpos, oYpos, oXpos2 - oXpos, oYpos2 - oYpos, oScaleX, oScaleY);
    }

    public static void customScaledObject2DPro(float oXpos, float oYpos, float oWidth, float oHeight, float oScaleX, float oScaleY) {
        GL11.glTranslated(oWidth / 2.0F, oHeight / 2.0F, 1.0F);
        GL11.glTranslated(-oXpos * oScaleX + oXpos + oWidth / 2.0F * -oScaleX, -oYpos * oScaleY + oYpos + oHeight / 2.0F * -oScaleY, 1.0F);
        GL11.glScaled(oScaleX, oScaleY, 0.0F);
    }

    public static void customRotatedObject2D(float oXpos, float oYpos, float oWidth, float oHeight, double rotate) {
        GL11.glTranslated(oXpos + oWidth / 2.0F, oYpos + oHeight / 2.0F, 0.0F);
        GL11.glRotated(rotate, 0.0F, 0.0F, 1.0F);
        GL11.glTranslated(-oXpos - oWidth / 2.0F, -oYpos - oHeight / 2.0F, 0.0F);
    }

    public static void resetBlender() {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableTexture();
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glShadeModel(GL11.GL_FLAT);
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void resetColor() {
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public static void enableGL2D() {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT, GL11.GL_NICEST);
    }

    public static void disableGL2D() {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_DONT_CARE);
        GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT, GL11.GL_DONT_CARE);
    }

    public static void setup2D(Runnable f) {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        f.run();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
    }

    public static void renderObj(int mode, Runnable render) {
        GL11.glBegin(mode);
        render.run();
        GL11.glEnd();
    }

    public static void runGLColor(int orRGB) {
        float c1 = (float) (orRGB >> 16 & 255) / 255.0F;
        float c2 = (float) (orRGB >> 8 & 255) / 255.0F;
        float c3 = (float) (orRGB & 255) / 255.0F;
        float c4 = (float) (orRGB >> 24 & 255) / 255.0F;
        GL11.glColor4f(c1, c2, c3, c4);
    }

    public static void setupColor(int color, float alpha) {
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;
        GL11.glColor4f(r, g, b, alpha / 255.0F);
    }

    public static void color(int argb) {
        float a = (float) (argb >> 24 & 255) / 255.0F;
        float r = (float) (argb >> 16 & 255) / 255.0F;
        float g = (float) (argb >> 8 & 255) / 255.0F;
        float b = (float) (argb & 255) / 255.0F;
        GL11.glColor4f(r, g, b, a);
    }

    public static int glColor(int color) {
        color(color);
        return color;
    }

    public static void drawItemWarnIfLowDur(ItemStack stack, float x, float y, float alphaPC, float scale) {
        drawItemWarnIfLowDur(stack, x, y, alphaPC, scale, 1);
    }

    public static void drawItemWarnIfLowDur(ItemStack stack, float x, float y, float alphaPC, float scale, int count) {
        if (stack == null || stack.isEmpty() || !stack.isDamaged()) {
            return;
        }
        float dmgPC = (float) stack.getDamage() / (float) stack.getMaxDamage();
        if (dmgPC < 0.7F) {
            return;
        }
        long timeDelay = (long) (1000.0F - 650.0F * (dmgPC - 0.9F) * 10.0F);
        if (timeDelay <= 0) {
            timeDelay = 200;
        }
        float timePC = (float) (System.currentTimeMillis() % timeDelay) / (float) timeDelay;
        timePC = ((timePC > 0.5F) ? 1.0F - timePC : timePC) * 2.0F;
        if (timePC * alphaPC < 0.02F) {
            return;
        }
        int colorVal = ColorUtils.getColor(255, 40, 0, (int) MathUtil.clamp(510.0F * timePC * alphaPC, 0.0F, 255.0F));
        mc.getTextureManager().bindTexture(ITEM_WARN_DUR);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        if (x != 0.0F || y != 0.0F) {
            GL11.glTranslated(x, y, 0.0D);
        }
        glColor(colorVal);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        for (int i = 0; i < count; ++i) {
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
            buffer.pos(-2.0D, 18.0D, 0.0D).tex(0.0F, 1.0F).color(colorVal).endVertex();
            buffer.pos(18.0D, 18.0D, 0.0D).tex(1.0F, 1.0F).color(colorVal).endVertex();
            buffer.pos(18.0D, -2.0D, 0.0D).tex(1.0F, 0.0F).color(colorVal).endVertex();
            buffer.pos(-2.0D, -2.0D, 0.0D).tex(0.0F, 0.0F).color(colorVal).endVertex();
            tessellator.draw();
        }
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        glColor(-1);
        if (x != 0.0F || y != 0.0F) {
            GL11.glTranslated(-x, -y, 0.0D);
        }
        RenderSystem.defaultBlendFunc();
    }

    public static void setAlphaLimit(float limit) {
        RenderSystem.enableAlphaTest();
        RenderSystem.alphaFunc(GL11.GL_GREATER, (float) (limit * 0.01));
    }

    private static int blendColor(int c1, int c2, float t) {
        return ColorUtils.interpolateColor(c1, c2, t);
    }

    public static class Vec2fColored {
        public final float x;
        public final float y;
        public final int color;

        public Vec2fColored(float x, float y, int color) {
            this.x = x;
            this.y = y;
            this.color = color;
        }
    }

    public static class Render2D {

        public static void drawRect(float x, float y, float width, float height, int color) {
            RenderUtils.drawRect(x, y, x + width, y + height, color);
        }

        public static void drawGradientRound(float x, float y, float width, float height, float radius, int color1, int color2, int color3, int color4) {
            RenderSystem.enableBlend();
            RenderSystem.disableTexture();
            RenderSystem.defaultBlendFunc();
            GL11.glShadeModel(GL11.GL_SMOOTH);
            GL11.glDisable(GL11.GL_ALPHA_TEST);

            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            buffer.pos(x + width, y, 0.0).color(color2).endVertex();
            buffer.pos(x, y, 0.0).color(color1).endVertex();
            buffer.pos(x, y + height, 0.0).color(color3).endVertex();
            buffer.pos(x + width, y + height, 0.0).color(color4).endVertex();
            tessellator.draw();

            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glShadeModel(GL11.GL_FLAT);
            RenderSystem.enableTexture();
            RenderSystem.disableBlend();
        }

        public static void drawGradientHorizontal(float x, float y, float width, float height, int color1, int color2) {
            RenderUtils.drawAlphedSideways(x, y, x + width, y + height, color1, color2);
        }

        public static void drawGradientVertical(float x, float y, float width, float height, int color1, int color2) {
            RenderUtils.drawAlphedGradient(x, y, x + width, y + height, color1, color2);
        }
    }
}
