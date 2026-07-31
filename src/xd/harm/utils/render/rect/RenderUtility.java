package xd.harm.utils.render.rect;

import net.minecraft.util.math.vector.Vector3d;
import org.lwjgl.opengl.GL12;
import com.jhlabs.image.GaussianFilter;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import xd.harm.utils.client.IMinecraft;
import xd.harm.utils.client.ScaledResolution;
import xd.harm.utils.math.Vector4i;
import xd.harm.utils.math.MathUtil;
import xd.harm.utils.render.color.ColorUtils;
import xd.harm.utils.shader.ShaderUtil;
import net.minecraft.client.MainWindow;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.Texture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.math.vector.Vector4f;
import net.optifine.util.TextureUtils;
import org.joml.Vector2d;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;

import static com.mojang.blaze3d.platform.GlStateManager.*;
import static com.mojang.blaze3d.platform.GlStateManager.GL_QUADS;
import static com.mojang.blaze3d.systems.RenderSystem.enableBlend;
import static net.minecraft.client.renderer.vertex.DefaultVertexFormats.*;
import static org.lwjgl.opengl.GL11.*;

public class RenderUtility implements IMinecraft {

    public static final RenderUtility instance = new RenderUtility();
    private static final ResourceLocation ITEM_WARN_DUR = new ResourceLocation("");
    public static final IntBuffer viewport = GLAllocation.createDirectByteBuffer(16 << 2).asIntBuffer();
    public static final FloatBuffer modelview = GLAllocation.createDirectFloatBuffer(16);
    public static final FloatBuffer projection = GLAllocation.createDirectFloatBuffer(16);
    public static final FloatBuffer vector = GLAllocation.createDirectFloatBuffer(4);
    public static final List<Vec2fColored> VERTEXES_COLORED = new ArrayList<>();
    public final List<Vector2f> VERTEXES = new ArrayList<>();
    private final int[] LEFT_UP = new int[]{-90, 0};
    private final int[] RIGHT_UP = new int[]{0, 90};
    private final int[] RIGHT_DOWN = new int[]{90, 180};
    private final int[] LEFT_DOWN = new int[]{180, 270};
    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0D);
    private static final double DEG_TO_RAD_D = Math.PI / 180.0D;
    private static final float POLYGON_ANGLE_STEP = (float) (Math.PI * 2F) / 360.0F;
    private static final float RAD_30 = 30.0F * DEG_TO_RAD;
    private static final float RAD_180 = 180.0F * DEG_TO_RAD;
    private static final int DEGREE_CACHE_STEP = 5;
    private static final float[] SIN_5_DEGREES = new float[360 / DEGREE_CACHE_STEP + 1];
    private static final float[] COS_5_DEGREES = new float[360 / DEGREE_CACHE_STEP + 1];

    static {
        for (int degrees = 0; degrees <= 360; degrees += DEGREE_CACHE_STEP) {
            float radians = degrees * DEG_TO_RAD;
            int index = degrees / DEGREE_CACHE_STEP;
            SIN_5_DEGREES[index] = (float) Math.sin(radians);
            COS_5_DEGREES[index] = (float) Math.cos(radians);
        }
    }

    private static float sinDeg5(int degrees) {
        return SIN_5_DEGREES[degrees / DEGREE_CACHE_STEP];
    }

    private static float cosDeg5(int degrees) {
        return COS_5_DEGREES[degrees / DEGREE_CACHE_STEP];
    }

    private static void putBufferColor(BufferBuilder bufferBuilder, int color) {
        bufferBuilder.color(
                (color >> 16 & 255) / 255.0F,
                (color >> 8 & 255) / 255.0F,
                (color & 255) / 255.0F,
                (color >> 24 & 255) / 255.0F
        );
    }

    private static void putMatrixVertex(BufferBuilder bufferBuilder, Matrix4f matrix, float x, float y, int color) {
        bufferBuilder.pos(matrix, x, y, 0.0F);
        putBufferColor(bufferBuilder, color);
        bufferBuilder.endVertex();
    }

    private static void putTexturedMatrixVertex(BufferBuilder bufferBuilder, Matrix4f matrix, float x, float y, float u, float v, int color) {
        bufferBuilder.pos(matrix, x, y, 0.0F).tex(u, v);
        putBufferColor(bufferBuilder, color);
        bufferBuilder.endVertex();
    }

    private static void putSolidRect(BufferBuilder bufferBuilder, double x, double y, double x2, double y2, int color) {
        bufferBuilder.pos(x, y, 0.0).color(color).endVertex();
        bufferBuilder.pos(x2, y, 0.0).color(color).endVertex();
        bufferBuilder.pos(x2, y2, 0.0).color(color).endVertex();
        bufferBuilder.pos(x, y2, 0.0).color(color).endVertex();
    }

    private static void putSidewaysRect(BufferBuilder bufferBuilder, double left, double top, double right, double bottom, int col1, int col2) {
        bufferBuilder.pos(left, top, 0.0).color(col1).endVertex();
        bufferBuilder.pos(left, bottom, 0.0).color(col1).endVertex();
        bufferBuilder.pos(right, bottom, 0.0).color(col2).endVertex();
        bufferBuilder.pos(right, top, 0.0).color(col2).endVertex();
    }

    private static int shadowCacheKey(float width, float height, int radius) {
        int result = Float.floatToIntBits(width);
        result = 31 * result + Float.floatToIntBits(height);
        return 31 * result + radius;
    }

    public static void quads(float x, float y, float width, float height, int glQuads, int color) {
        buffer.begin(glQuads, POSITION_TEX_COLOR);
        {
            buffer.pos(x, y, 0).tex(0, 0).color(color).endVertex();
            buffer.pos(x, y + height, 0).tex(0, 1).color(color).endVertex();
            buffer.pos(x + width, y + height, 0).tex(1, 1).color(color).endVertex();
            buffer.pos(x + width, y, 0).tex(1, 0).color(color).endVertex();
        }
        tessellator.draw();
    }

    public static int reAlphaInt(final int color,
                                 final int alpha) {
        return (MathHelper.clamp(alpha, 0, 255) << 24) | (color & 16777215);
    }

    public static Vector3d cameraPos() {
        return mc.gameRenderer.getActiveRenderInfo().getProjectedView();
    }

    public static void drawBlockESP(BlockPos pos, Color color) {
        if (pos == null || mc.world == null) {
            return;
        }

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
            float scaleFactor = ScaledResolution.getScaleFactor();
            GL11.glPointSize(radius * scaleFactor * 2.0F);
            renderObj(GL11.GL_POINTS, () -> GL11.glVertex2d(x, y));
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            if (bloom) {
                RenderSystem.defaultBlendFunc();
            }
        });
    }

    public static void drawLightContureRect(double x, double y, double x2, double y2, int color) {
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        putSolidRect(buffer, x - 0.5D, y - 0.5D, x2 + 0.5D, y, color);
        putSolidRect(buffer, x - 0.5D, y2, x2 + 0.5D, y2 + 0.5D, color);
        putSolidRect(buffer, x - 0.5D, y, x, y2, color);
        putSolidRect(buffer, x2, y, x2 + 0.5D, y2, color);
        tessellator.draw();
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    public static void drawLightContureRectSmooth(double x, double y, double x2, double y2, int color) {
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        putSolidRect(buffer, x, y - 0.5D, x2, y, color);
        putSolidRect(buffer, x, y2, x2, y2 + 0.5D, color);
        putSolidRect(buffer, x - 0.5D, y, x, y2, color);
        putSolidRect(buffer, x2, y, x2 + 0.5D, y2, color);
        tessellator.draw();
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    public static void drawLightContureRectSidewaysSmooth(double x, double y, double x2, double y2, int color, int color2) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        putSidewaysRect(buffer, x, y - 0.5D, x2, y, color, color2);
        putSidewaysRect(buffer, x, y2, x2, y2 + 0.5D, color, color2);
        putSolidRect(buffer, x - 0.5D, y, x, y2, color);
        putSolidRect(buffer, x2, y, x2 + 0.5D, y2, color2);
        tessellator.draw();
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glShadeModel(GL11.GL_FLAT);
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
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
            float angle = POLYGON_ANGLE_STEP * i + RAD_30;
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
            float angle = POLYGON_ANGLE_STEP * i + RAD_180;
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
            float angle = POLYGON_ANGLE_STEP * i + RAD_180;
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

    public static void bindTexture(ResourceLocation location) {
        mc.getTextureManager().bindTexture(location);
    }

    public Vec2fColored getOfVec3f(Vector2f vec2f, int color) {
        return new Vec2fColored(vec2f.x, vec2f.y, color);
    }

    public static void drawRect(MatrixStack matrix, float x, float y, float x2, float y2, int color) {
        drawRect(matrix, x, y, x2, y2, color, false);
    }

    public static void drawRect(MatrixStack matrix, float x, float y, float x2, float y2, int color, boolean bloom) {
        drawRect(matrix, x, y, x2, y2, color, color, color, color, bloom, false);
    }

    public static void drawSmoothRect(MatrixStack matrixStack, double left, double top, double right, double bottom, int color) {
        drawRect(left, top, right, bottom, color, matrixStack);

        glScalef(0.5f, 0.5f, 0.5f);
        drawRect(left * 2.0f - 1.0f, top * 2.0f, left * 2.0f, bottom * 2.0f - 1.0f, color, matrixStack);
        drawRect(left * 2.0f, top * 2.0f - 1.0f, right * 2.0f, top * 2.0f, color, matrixStack);
        drawRect(right * 2.0f, top * 2.0f, right * 2.0f + 1.0f, bottom * 2.0f - 1.0f, color, matrixStack);
        glScalef(2.0f, 2.0f, 2.0f);
    }

    public static void drawRect(double left, double top, double right, double bottom, int color, MatrixStack matrixStack) {
        if (left < right) {
            double i = left;
            left = right;
            right = i;
        }
        if (top < bottom) {
            double j = top;
            top = bottom;
            bottom = j;
        }

        double finalLeft = left;
        double finalTop = top;
        double finalRight = right;
        double finalBottom = bottom;
        start2Draw(() -> {
            setColor(color);
            BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
            bufferBuilder.begin(GL_QUADS, DefaultVertexFormats.POSITION);
            bufferBuilder.pos(matrixStack, finalLeft, finalBottom).endVertex();
            bufferBuilder.pos(matrixStack, finalRight, finalBottom).endVertex();
            bufferBuilder.pos(matrixStack, finalRight, finalTop).endVertex();
            bufferBuilder.pos(matrixStack, finalLeft, finalTop).endVertex();
            bufferBuilder.finishDrawing();
            WorldVertexBufferUploader.draw(bufferBuilder);
        });
    }

    public static void start2Draw(Runnable runnable) {
        boolean isEnabled = glIsEnabled(GL_BLEND);
        glEnable(GL_BLEND);
        glDisable(GL_TEXTURE_2D);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_ALPHA_TEST);

        runnable.run();

        if (!isEnabled) {
            glDisable(GL_BLEND);
        }
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_ALPHA_TEST);
    }

    public static void setColor(int color) {
        glColor4ub((byte) (color >> 16 & 0xFF), (byte) (color >> 8 & 0xFF), (byte) (color & 0xFF), (byte) (color >> 24 & 0xFF));
    }

    public static void setupOrientationMatrix(MatrixStack matrix, float x, float y, float z) {
        setupOrientationMatrix(matrix, (double) x, y, z);
    }

    public static void setupOrientationMatrix(MatrixStack matrix, double x, double y, double z) {
        EntityRendererManager rendererManager = mc.getRenderManager();
        final Vector3d renderPos = rendererManager.info.getProjectedView();
        matrix.translate(x - renderPos.x, y - renderPos.y, z - renderPos.z);
    }

    public static void drawRect(MatrixStack matrix, float x, float y, float x2, float y2, int color1, int color2, int color3, int color4, boolean bloom, boolean texture) {
        setupRenderRect(texture, bloom);
        buffer.begin(GL11.GL_POLYGON, texture ? DefaultVertexFormats.POSITION_TEX_COLOR : DefaultVertexFormats.POSITION_COLOR);
        Matrix4f matrix4f = matrix.getLast().getMatrix();
        if (texture) {
            putTexturedMatrixVertex(buffer, matrix4f, x, y, 0.0F, 0.0F, color1);
            putTexturedMatrixVertex(buffer, matrix4f, x2, y, 1.0F, 0.0F, color2);
            putTexturedMatrixVertex(buffer, matrix4f, x2, y2, 1.0F, 1.0F, color3);
            putTexturedMatrixVertex(buffer, matrix4f, x, y2, 0.0F, 1.0F, color4);
        } else {
            putMatrixVertex(buffer, matrix4f, x, y, color1);
            putMatrixVertex(buffer, matrix4f, x2, y, color2);
            putMatrixVertex(buffer, matrix4f, x2, y2, color3);
            putMatrixVertex(buffer, matrix4f, x, y2, color4);
        }
        tessellator.draw();
        endRenderRect(bloom);
    }

    public void drawRoundedRectShadowed(MatrixStack matrix, float x, float y, float x2, float y2, float round, float shadowSize, int color1, int color2, int color3, int color4, boolean bloom, boolean sageColor, boolean rect, boolean shadow) {
        float roundMax = Math.max(x2 - x, y2 - y);
        round = Math.max(Math.min(round, roundMax), 0);
        shadowSize = Math.max(shadowSize, 0);

        x += round;
        y += round;
        x2 -= round;
        y2 -= round;
        if (rect) {
            drawRect(matrix, x, y, x2, y2, color1, color2, color3, color4, bloom, false);
            if (round != 0) {
                drawLimitersSegments(matrix, x, y, x2, y2, round, 0, color1, color2, color3, color4, false, false, bloom);
                drawRoundSegments(matrix, x, y, x2, y2, round, color1, color2, color3, color4, bloom);
            }
        }
        if (shadow && shadowSize > 0) {
            drawLimitersSegments(matrix, x - round, y - round, x2 + round, y2 + round, shadowSize, round, color1, color2, color3, color4, sageColor, true, bloom);
            drawShadowSegmentsExtract(matrix, x, y, x2, y2, round, shadowSize, color1, color2, color3, color4, sageColor, bloom);
        }
    }

    public void drawShadowSegment(MatrixStack matrix, float x, float y, double radius, int color, boolean sageColor, int[] side, boolean bloom) {
        int color2 = sageColor ? 0 : ColorUtils.reAlphaInt(color, 0);
        drawDuadsSegment(matrix, x, y, 0, radius, color, color2, bloom, side);
    }

    public void drawShadowSegment(MatrixStack matrix, float x, float y, double radiusRound, double radiusShadow, int color, boolean sageColor, int[] side, boolean bloom) {
        int color2 = sageColor ? 0 : ColorUtils.reAlphaInt(color, 0);
        drawDuadsSegment(matrix, x, y, radiusRound, radiusShadow, color, color2, bloom, side);
    }

    public void drawShadowSegment(MatrixStack matrix, float x, float y, double radius, int color, boolean sageColor, int[] side) {
        drawShadowSegment(matrix, x, y, radius, color, sageColor, side, false);
    }

    public void drawShadowSegments(MatrixStack matrix, float x, float y, float x2, float y2, double radius, int color1, int color2, int color3, int color4, boolean sageColor, boolean bloom) {
        drawShadowSegment(matrix, x, y, radius, color1, sageColor, LEFT_UP, bloom);
        drawShadowSegment(matrix, x2, y, radius, color2, sageColor, RIGHT_UP, bloom);
        drawShadowSegment(matrix, x2, y2, radius, color3, sageColor, RIGHT_DOWN, bloom);
        drawShadowSegment(matrix, x, y2, radius, color4, sageColor, LEFT_DOWN, bloom);
    }

    public void drawShadowSegmentsExtract(MatrixStack matrix, float x, float y, float x2, float y2, double radiusStart, double radiusEnd, int color1, int color2, int color3, int color4, boolean sageColor, boolean bloom) {
        drawShadowSegment(matrix, x, y, radiusStart, radiusEnd, color1, sageColor, LEFT_UP, bloom);
        drawShadowSegment(matrix, x2, y, radiusStart, radiusEnd, color2, sageColor, RIGHT_UP, bloom);
        drawShadowSegment(matrix, x2, y2, radiusStart, radiusEnd, color3, sageColor, RIGHT_DOWN, bloom);
        drawShadowSegment(matrix, x, y2, radiusStart, radiusEnd, color4, sageColor, LEFT_DOWN, bloom);
    }

    public void drawShadowSegments(MatrixStack matrix, float x, float y, float x2, float y2, double radius, int color1, int color2, int color3, int color4, boolean sageColor) {
        drawShadowSegments(matrix, x, y, x2, y2, radius, color1, color2, color3, color4, sageColor, false);
    }

    public void drawShadowSegmentsExtract(MatrixStack matrix, float x, float y, float x2, float y2, double radiusStart, double radiusEnd, int color1, int color2, int color3, int color4, boolean sageColor) {
        drawShadowSegmentsExtract(matrix, x, y, x2, y2, radiusStart, radiusEnd, color1, color2, color3, color4, sageColor, false);
    }

    public void drawLimitersSegments(MatrixStack matrix, float x, float y, float x2, float y2, float radius, float appendOffsets, int color1, int color2, int color3, int color4, boolean sageColor, boolean retainZero, boolean bloom) {
        int c5 = retainZero ? sageColor ? 0 : ColorUtils.reAlphaInt(color1, 0) : color1;
        int c6 = retainZero ? sageColor ? 0 : ColorUtils.reAlphaInt(color2, 0) : color2;
        int c7 = retainZero ? sageColor ? 0 : ColorUtils.reAlphaInt(color3, 0) : color3;
        int c8 = retainZero ? sageColor ? 0 : ColorUtils.reAlphaInt(color4, 0) : color4;

        drawRect(matrix, x + appendOffsets, y - radius, x2 - appendOffsets, y, c5, c6, color2, color1, bloom, false);
        drawRect(matrix, x + appendOffsets, y2, x2 - appendOffsets, y2 + radius, color4, color3, c7, c8, bloom, false);
        drawRect(matrix, x - radius, y + appendOffsets, x, y2 - appendOffsets, c5, color1, color4, c8, bloom, false);
        drawRect(matrix, x2, y + appendOffsets, x2 + radius, y2 - appendOffsets, color2, c6, c7, color3, bloom, false);
    }

    public void drawRoundSegments(MatrixStack matrix, float x, float y, float x2, float y2, double radius, int color1, int color2, int color3, int color4, boolean bloom) {
        drawRoundSegment(matrix, x, y, radius, color1, LEFT_UP, bloom);
        drawRoundSegment(matrix, x2, y, radius, color2, RIGHT_UP, bloom);
        drawRoundSegment(matrix, x2, y2, radius, color3, RIGHT_DOWN, bloom);
        drawRoundSegment(matrix, x, y2, radius, color4, LEFT_DOWN, bloom);
    }

    public void drawRoundSegment(MatrixStack matrix, float x, float y, double radius, int color, int[] side, boolean bloom) {
        drawDuadsSegment(matrix, x, y, 0, radius, color, color, bloom, side);
    }

    public void drawDuadsSegment(MatrixStack matrix, float x, float y, double radius, double expand, int color, int color2, boolean bloom, int[] side) {
        VERTEXES_COLORED.clear();
        int index = 0;
        for (Vector2f vec2f : generateRadiusCircledVertexes(matrix, x, y, radius, radius + expand, side[0], side[1], 9, true)) {
            VERTEXES_COLORED.add(getOfVec3f(vec2f, index % 2 == 1 ? color2 : color));
            ++index;
        }
        drawVertexesList(matrix, VERTEXES_COLORED, GL12.GL_TRIANGLE_STRIP, false, bloom);
    }

    public List<Vector2f> generateRadiusCircledVertexes(MatrixStack matrix, float x, float y, double radius1, double radius2, double startRadius, double endRadius, double step, boolean doublepart) {
        VERTEXES.clear();
        double radius = startRadius;
        while (radius <= endRadius) {
            if (radius > endRadius) {
                radius = endRadius;
            }
            double angle = radius * DEG_TO_RAD_D;
            double sin = Math.sin(angle);
            double cos = Math.cos(angle);
            float x1 = (float) (sin * radius1);
            float y1 = (float) (-cos * radius1);
            VERTEXES.add(new Vector2f(x + x1, y + y1));
            if (doublepart) {
                x1 = (float) (sin * radius2);
                y1 = (float) (-cos * radius2);
                VERTEXES.add(new Vector2f(x + x1, y + y1));
            }
            radius += step;
        }
        return VERTEXES;
    }

    public static void drawVertexesList(MatrixStack matrix, List<Vec2fColored> vec2c, int begin, boolean texture, boolean bloom) {
        setupRenderRect(texture, bloom);
        buffer.begin(begin, texture ? DefaultVertexFormats.POSITION_TEX_COLOR : DefaultVertexFormats.POSITION_COLOR);
        Matrix4f matrix4f = matrix.getLast().getMatrix();
        int counter = 0;
        for (final Vec2fColored vec : vec2c) {
            buffer.pos(matrix4f, vec.x, vec.y, 0);
            if (texture) {
                buffer.tex(counter == 0 || counter == 3 ? 0 : 1, counter == 0 || counter == 1 ? 0 : 1);
            }
            putBufferColor(buffer, vec.color);
            buffer.endVertex();
            counter++;
        }
        tessellator.draw();
        endRenderRect(bloom);
    }

    public static void setupRenderRect(boolean texture, boolean bloom) {
        if (texture) {
            RenderSystem.enableTexture();
        } else {
            RenderSystem.disableTexture();
        }
        RenderSystem.enableBlend();
        RenderSystem.disableAlphaTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.shadeModel(7425);
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, bloom ? GlStateManager.DestFactor.ONE_MINUS_CONSTANT_ALPHA : GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.disableAlphaTest();
        GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glEnable(GL11.GL_POINT_SMOOTH);
    }

    public static void endRenderRect(boolean bloom) {
        RenderSystem.enableAlphaTest();
        if (bloom) {
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        }
        RenderSystem.shadeModel(7424);
        RenderSystem.enableCull();
        RenderSystem.enableAlphaTest();
        RenderSystem.enableTexture();
        RenderSystem.clearCurrentColor();
    }

    public static void drawRoundedRectOutline(float x, float y, float width, float height, float radius, float outlineWidth, int borderColor) {
        float x2 = x + width;
        float y2 = y + height;

        radius = Math.min(radius, Math.min(width, height) / 2f);

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glLineWidth(outlineWidth);

        float alpha = (borderColor >> 24 & 0xFF) / 255.0f;
        float red = (borderColor >> 16 & 0xFF) / 255.0f;
        float green = (borderColor >> 8 & 0xFF) / 255.0f;
        float blue = (borderColor & 0xFF) / 255.0f;

        GL11.glColor4f(red, green, blue, alpha);

        int segments = Math.max(8, (int)(radius * 0.8f));
        double segmentStep = 90.0D / segments;

        GL11.glBegin(GL11.GL_LINE_LOOP);

        GL11.glVertex2f(x + radius, y);
        GL11.glVertex2f(x2 - radius, y);

        for (int i = 0; i <= segments; i++) {
            double angle = (270.0D + segmentStep * i) * DEG_TO_RAD_D;
            GL11.glVertex2f(
                    (float)(x2 - radius + Math.cos(angle) * radius),
                    (float)(y + radius + Math.sin(angle) * radius)
            );
        }

        GL11.glVertex2f(x2, y + radius);
        GL11.glVertex2f(x2, y2 - radius);

        for (int i = 0; i <= segments; i++) {
            double angle = segmentStep * i * DEG_TO_RAD_D;
            GL11.glVertex2f(
                    (float)(x2 - radius + Math.cos(angle) * radius),
                    (float)(y2 - radius + Math.sin(angle) * radius)
            );
        }

        GL11.glVertex2f(x2 - radius, y2);
        GL11.glVertex2f(x + radius, y2);

        for (int i = 0; i <= segments; i++) {
            double angle = (90.0D + segmentStep * i) * DEG_TO_RAD_D;
            GL11.glVertex2f(
                    (float)(x + radius + Math.cos(angle) * radius),
                    (float)(y2 - radius + Math.sin(angle) * radius)
            );
        }

        GL11.glVertex2f(x, y2 - radius);
        GL11.glVertex2f(x, y + radius);

        for (int i = 0; i <= segments; i++) {
            double angle = (180.0D + segmentStep * i) * DEG_TO_RAD_D;
            GL11.glVertex2f(
                    (float)(x + radius + Math.cos(angle) * radius),
                    (float)(y + radius + Math.sin(angle) * radius)
            );
        }

        GL11.glEnd();

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.enableTexture();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }


    public static void quadsBegin(float x, float y, float width, float height, int glQuads) {
        buffer.begin(glQuads, POSITION_TEX);
        {
            buffer.pos(x, y, 0).tex(0, 0).endVertex();
            buffer.pos(x, y + height, 0).tex(0, 1).endVertex();
            buffer.pos(x + width, y + height, 0).tex(1, 1).endVertex();
            buffer.pos(x + width, y, 0).tex(1, 0).endVertex();
        }
        tessellator.draw();
    }

    static ShaderUtil head = new ShaderUtil("round-head");
    public static void drawHead(ResourceLocation skin, float x, float y, float width, float height, float radius, float alpha, float hurtPercent) {
        mc.getTextureManager().bindTexture(skin);
        pushMatrix();
        enableBlend();
        head.attach();
        head.setUniformf("size", width, height);
        head.setUniformf("radius", radius);
        head.setUniformf("hurt_time", hurtPercent);
        head.setUniformf("alpha", alpha);

        head.setUniformf("startX", 4);
        head.setUniformf("startY", 4);
        head.setUniformf("endX", 8);
        head.setUniformf("endY", 8);

        head.setUniformf("texXSize", 32);
        head.setUniformf("texYSize", 32);
        head.drawQuads(x+2, y+2, width, height);

        head.setUniformf("startX", 20);
        head.setUniformf("startY", 4);
        head.setUniformf("endX", 24);
        head.setUniformf("endY", 8);
        head.drawQuads(x+2, y+2, width, height);
        head.detach();
        disableBlend();
        popMatrix();
    }

    public static void drawAccurateHead(ResourceLocation skin, float x, float y, float width, float height, float radius, float alpha, float hurtPercent) {
        mc.getTextureManager().bindTexture(skin);
        pushMatrix();
        enableBlend();
        head.attach();
        head.setUniformf("texXSize", 32);
        head.setUniformf("texYSize", 32);

        drawAccurateHeadLayer(x, y, width, height, radius, alpha, hurtPercent, 4, 4, 8, 8);

        float layerExpand = Math.max(0.35f, Math.min(width, height) * 0.035f);
        drawAccurateHeadLayer(
                x - layerExpand * 0.5f,
                y - layerExpand * 0.5f,
                width + layerExpand,
                height + layerExpand,
                radius + layerExpand * 0.45f,
                alpha,
                hurtPercent,
                20,
                4,
                24,
                8
        );

        head.detach();
        disableBlend();
        popMatrix();
    }

    private static void drawAccurateHeadLayer(float x, float y, float width, float height, float radius, float alpha, float hurtPercent,
                                              float startX, float startY, float endX, float endY) {
        head.setUniformf("size", width, height);
        head.setUniformf("radius", radius);
        head.setUniformf("hurt_time", hurtPercent);
        head.setUniformf("alpha", alpha);
        head.setUniformf("startX", startX);
        head.setUniformf("startY", startY);
        head.setUniformf("endX", endX);
        head.setUniformf("endY", endY);
        head.drawQuads(x, y, width, height);
    }

    public static Vector2d project2D(net.minecraft.util.math.vector.Vector3d vec) {
        return project2D(vec.x, vec.y, vec.z);
    }

    public static Vector2d project2D(double x, double y, double z) {
        if (mc.getRenderManager().info == null) return new Vector2d();
        net.minecraft.util.math.vector.Vector3d cameraPosition = mc.getRenderManager().info.getProjectedView();
        Quaternion cameraRotation = mc.getRenderManager().getCameraOrientation().copy();
        cameraRotation.conjugate();

        Vector3f relativePosition = new Vector3f((float) (cameraPosition.x - x), (float) (cameraPosition.y - y), (float) (cameraPosition.z - z));
        relativePosition.transform(cameraRotation);

        if (mc.gameSettings.viewBobbing) {
            Entity renderViewEntity = mc.getRenderViewEntity();
            if (renderViewEntity instanceof PlayerEntity playerEntity) {
                float walkedDistance = playerEntity.distanceWalkedModified;

                float deltaDistance = walkedDistance - playerEntity.prevDistanceWalkedModified;
                float interpolatedDistance = -(walkedDistance + deltaDistance * mc.getRenderPartialTicks());
                float cameraYaw = MathHelper.lerp(mc.getRenderPartialTicks(), playerEntity.prevCameraYaw, playerEntity.cameraYaw);

                Quaternion bobQuaternionX = new Quaternion(Vector3f.XP, Math.abs(MathHelper.cos(interpolatedDistance * (float) Math.PI - 0.2F) * cameraYaw) * 5.0F, true);
                bobQuaternionX.conjugate();
                relativePosition.transform(bobQuaternionX);

                Quaternion bobQuaternionZ = new Quaternion(Vector3f.ZP, MathHelper.sin(interpolatedDistance * (float) Math.PI) * cameraYaw * 3.0F, true);
                bobQuaternionZ.conjugate();
                relativePosition.transform(bobQuaternionZ);

                Vector3f bobTranslation = new Vector3f((MathHelper.sin(interpolatedDistance * (float) Math.PI) * cameraYaw * 0.5F), (-Math.abs(MathHelper.cos(interpolatedDistance * (float) Math.PI) * cameraYaw)), 0.0f);
                bobTranslation.setY(-bobTranslation.getY());
                relativePosition.add(bobTranslation);
            }
        }

        double fieldOfView = (float) mc.gameRenderer.getFOVModifier(mc.getRenderManager().info, mc.getRenderPartialTicks(), true);

        MainWindow mainWindow = mc.getMainWindow();
        int scaledWidth = mainWindow.getScaledWidth();
        int scaledHeight = mainWindow.getScaledHeight();
        float halfHeight = (float) scaledHeight / 2.0F;
        float scaleFactor = halfHeight / (relativePosition.getZ() * (float) Math.tan(fieldOfView * 0.5D * DEG_TO_RAD_D));

        if (relativePosition.getZ() < 0.0F) {
            return new Vector2d(-relativePosition.getX() * scaleFactor + (float) (scaledWidth / 2), (float) (scaledHeight / 2) - relativePosition.getY() * scaleFactor);
        }
        return null;
    }

    public static void drawGradientRoundedOutlineHorizontal(float x, float y, float x2, float y2, float radius, float lineWidth, int colorLeft, int colorRight) {
        float width = x2 - x;
        float height = y2 - y;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glLineWidth(lineWidth);
        GL11.glShadeModel(GL11.GL_SMOOTH);

        float alphaL = (colorLeft >> 24 & 0xFF) / 255.0f;
        float redL = (colorLeft >> 16 & 0xFF) / 255.0f;
        float greenL = (colorLeft >> 8 & 0xFF) / 255.0f;
        float blueL = (colorLeft & 0xFF) / 255.0f;

        float alphaR = (colorRight >> 24 & 0xFF) / 255.0f;
        float redR = (colorRight >> 16 & 0xFF) / 255.0f;
        float greenR = (colorRight >> 8 & 0xFF) / 255.0f;
        float blueR = (colorRight & 0xFF) / 255.0f;

        float glowBoost = 4f;

        GL11.glBegin(GL11.GL_LINE_LOOP);

        for (int i = 180; i <= 270; i += 5) {
            float px = x + radius + cosDeg5(i) * radius;
            float py = y + radius + sinDeg5(i) * radius;
            setColorForXGlow(redL, greenL, blueL, alphaL, 1.0f);
            GL11.glVertex2f(px, py);
        }

        setColorForXGlow(redL, greenL, blueL, alphaL, 1.0f);
        GL11.glVertex2f(x + radius, y);
        setColorForXGlow(redR, greenR, blueR, alphaR, 1.0f);
        GL11.glVertex2f(x2 - radius, y);

        for (int i = 270; i <= 360; i += 5) {
            float px = x2 - radius + cosDeg5(i) * radius;
            float py = y + radius + sinDeg5(i) * radius;
            setColorForXGlow(redR, greenR, blueR, alphaR, glowBoost);
            GL11.glVertex2f(px, py);
        }

        setColorForXGlow(redR, greenR, blueR, alphaR, glowBoost);
        GL11.glVertex2f(x2, y + radius);
        GL11.glVertex2f(x2, y2 - radius);

        for (int i = 0; i <= 90; i += 5) {
            float px = x2 - radius + cosDeg5(i) * radius;
            float py = y2 - radius + sinDeg5(i) * radius;
            setColorForXGlow(redR, greenR, blueR, alphaR, glowBoost);
            GL11.glVertex2f(px, py);
        }

        setColorForXGlow(redR, greenR, blueR, alphaR, 1.0f);
        GL11.glVertex2f(x2 - radius, y2);
        setColorForXGlow(redL, greenL, blueL, alphaL, 1.0f);
        GL11.glVertex2f(x + radius, y2);

        for (int i = 90; i <= 180; i += 5) {
            float px = x + radius + cosDeg5(i) * radius;
            float py = y2 - radius + sinDeg5(i) * radius;
            setColorForXGlow(redL, greenL, blueL, alphaL, 1.0f);
            GL11.glVertex2f(px, py);
        }

        setColorForXGlow(redL, greenL, blueL, alphaL, 1.0f);
        GL11.glVertex2f(x, y2 - radius);
        GL11.glVertex2f(x, y + radius);

        GL11.glEnd();

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.enableTexture();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }


    private static void setColorForXGlow(float r, float g, float b, float a, float alphaBoost) {
        float finalAlpha = MathHelper.clamp(a * alphaBoost, 0.0f, 1.0f);
        GL11.glColor4f(r, g, b, finalAlpha);
    }

    public static void drawCircleWithFill(float x, float y, float start, float end, float radius, float width, boolean filled, int color) {
        float sin;
        float cos;
        float i;
        if (start > end) {
            float endOffset = end;
            end = start;
            start = endOffset;
        }
        GlStateManager.enableBlend();
        GL11.glDisable(3553);
        RenderSystem.blendFuncSeparate(770, 771, 1, 0);
        GL11.glEnable(2848);
        GL11.glLineWidth(width);
        GL11.glBegin(3);
        for (i = end; i >= start; i -= 1.0f) {
            ColorUtils.setColor(color);
            float angle = i * DEG_TO_RAD;
            cos = MathHelper.cos(angle) * radius;
            sin = MathHelper.sin(angle) * radius;
            GL11.glVertex2f(x + cos, y + sin);
        }
        GL11.glEnd();
        GL11.glDisable(2848);
        if (filled) {
            GL11.glBegin(6);
            for (i = end; i >= start; i -= 1.0f) {
                ColorUtils.setColor(color);
                float angle = i * DEG_TO_RAD;
                cos = MathHelper.cos(angle) * radius;
                sin = MathHelper.sin(angle) * radius;
                GL11.glVertex2f(x + cos, y + sin);
            }
            GL11.glEnd();
        }
        GL11.glEnable(3553);
        disableBlend();
    }

    public static boolean isInRegion(double mouseX, double mouseY, float x, float y, float width, float height) {
        return mouseX >= (double)x && mouseX <= (double)(x + width) && mouseY >= (double)y && mouseY <= (double)(y + height);
    }


    public static void scissor(double x, double y, double width, double height) {

        MainWindow mainWindow = mc.getMainWindow();
        final double scale = mainWindow.getGuiScaleFactor();

        y = mainWindow.getScaledHeight() - y;

        x *= scale;
        y *= scale;
        width *= scale;
        height *= scale;

        GL11.glScissor((int) x, (int) (y - height), (int) width, (int) height);
    }

    private static final HashMap<Integer, Integer> shadowCache = new HashMap<Integer, Integer>();

    public static void drawShadow(float x, float y, float width, float height, int radius, int color) {
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.01f);
        GlStateManager.disableAlphaTest();

        x -= radius;
        y -= radius;
        width = width + radius * 2;
        height = height + radius * 2;
        x -= 0.25f;
        y += 0.25f;

        int identifier = shadowCacheKey(width, height, radius);
        int textureId;

        Integer cachedTextureId = shadowCache.get(identifier);
        if (cachedTextureId != null) {
            textureId = cachedTextureId;
            GlStateManager.bindTexture(textureId);
        } else {
            if (width <= 0) {
                width = 1;
            }

            if (height <= 0) {
                height = 1;
            }

            BufferedImage originalImage = new BufferedImage((int) width, (int) height, BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D graphics = originalImage.createGraphics();
            graphics.setColor(Color.WHITE);
            graphics.fillRect(radius, radius, (int) (width - radius * 2), (int) (height - radius * 2));
            graphics.dispose();

            GaussianFilter filter = new GaussianFilter(radius);
            BufferedImage blurredImage = filter.filter(originalImage, null);
            DynamicTexture texture = new DynamicTexture(TextureUtils.toNativeImage(blurredImage));
            texture.setBlurMipmap(true, true);
            try {
                textureId = texture.getGlTextureId();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            shadowCache.put(identifier, textureId);
        }

        float colorRed = (color >> 16 & 255) / 255.0F;
        float colorGreen = (color >> 8 & 255) / 255.0F;
        float colorBlue = (color & 255) / 255.0F;
        float colorAlpha = (color >> 24 & 255) / 255.0F;

        buffer.begin(GL11.GL_QUADS, POSITION_COLOR_TEX);
        buffer.pos(x, y, 0.0f)
                .color(colorRed, colorGreen, colorBlue, colorAlpha)
                .tex(0.0f, 0.0f)
                .endVertex();

        buffer.pos(x, y + (float) ((int) height), 0.0f)
                .color(colorRed, colorGreen, colorBlue, colorAlpha)
                .tex(0.0f, 1.0f)
                .endVertex();

        buffer.pos(x + (float) ((int) width), y + (float) ((int) height), 0.0f)
                .color(colorRed, colorGreen, colorBlue, colorAlpha)
                .tex(1.0f, 1.0f)
                .endVertex();

        buffer.pos(x + (float) ((int) width), y, 0.0f)
                .color(colorRed, colorGreen, colorBlue, colorAlpha)
                .tex(1.0f, 0.0f)
                .endVertex();

        tessellator.draw();
        GlStateManager.enableAlphaTest();
        GlStateManager.bindTexture(0);
        disableBlend();
    }

    public static void drawImage(ResourceLocation resourceLocation, float x, float y, float width, float height,
                                 int color) {
        RenderSystem.pushMatrix();
        RenderSystem.enableBlend();
        RenderSystem.disableAlphaTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.shadeModel(7425);
        mc.getTextureManager().bindTexture(resourceLocation);
        quads(x, y, width, height, 7, color);
        RenderSystem.shadeModel(7424);
        RenderSystem.color4f(1, 1, 1, 1);
        RenderSystem.popMatrix();

    }

    public static void drawImage(ResourceLocation resourceLocation, float x, float y, float width, float height,
                                 Vector4i color) {
        RenderSystem.pushMatrix();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.shadeModel(7425);
        mc.getTextureManager().bindTexture(resourceLocation);
        buffer.begin(7, POSITION_TEX_COLOR);
        {
            buffer.pos(x, y, 0).tex(0, 0).color(color.x).endVertex();
            buffer.pos(x, y + height, 0).tex(0, 1).color(color.y).endVertex();
            buffer.pos(x + width, y + height, 0).tex(1, 1).color(color.z).endVertex();
            buffer.pos(x + width, y, 0).tex(1, 0).color(color.w).endVertex();
        }
        tessellator.draw();
        RenderSystem.shadeModel(7424);
        RenderSystem.color4f(1, 1, 1, 1);
        RenderSystem.popMatrix();

    }

    public static void drawRectBuilding(
            double left,
            double top,
            double right,
            double bottom,
            int color) {
        if (left < right) {
            double i = left;
            left = right;
            right = i;
        }

        if (top < bottom) {
            double j = top;
            top = bottom;
            bottom = j;
        }

        float f3 = (float) (color >> 24 & 255) / 255.0F;
        float f = (float) (color >> 16 & 255) / 255.0F;
        float f1 = (float) (color >> 8 & 255) / 255.0F;
        float f2 = (float) (color & 255) / 255.0F;
        BufferBuilder bufferbuilder = Tessellator.getInstance().getBuffer();
        bufferbuilder.pos(left, bottom, 0.0F).color(f, f1, f2, f3).endVertex();
        bufferbuilder.pos(right, bottom, 0.0F).color(f, f1, f2, f3).endVertex();
        bufferbuilder.pos(right, top, 0.0F).color(f, f1, f2, f3).endVertex();
        bufferbuilder.pos(left, top, 0.0F).color(f, f1, f2, f3).endVertex();
    }

    public static void drawMCVerticalBuilding(double x,
                                              double y,
                                              double width,
                                              double height,
                                              int start,
                                              int end) {

        float f = (float) (start >> 24 & 255) / 255.0F;
        float f1 = (float) (start >> 16 & 255) / 255.0F;
        float f2 = (float) (start >> 8 & 255) / 255.0F;
        float f3 = (float) (start & 255) / 255.0F;
        float f4 = (float) (end >> 24 & 255) / 255.0F;
        float f5 = (float) (end >> 16 & 255) / 255.0F;
        float f6 = (float) (end >> 8 & 255) / 255.0F;
        float f7 = (float) (end & 255) / 255.0F;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();

        bufferbuilder.pos(x, height, 0f).color(f1, f2, f3, f).endVertex();
        bufferbuilder.pos(width, height, 0f).color(f1, f2, f3, f).endVertex();
        bufferbuilder.pos(width, y, 0f).color(f5, f6, f7, f4).endVertex();
        bufferbuilder.pos(x, y, 0f).color(f5, f6, f7, f4).endVertex();
    }

    public static void drawMCHorizontalBuilding(double x,
                                                double y,
                                                double width,
                                                double height,
                                                int start,
                                                int end) {

        float f = (float) (start >> 24 & 255) / 255.0F;
        float f1 = (float) (start >> 16 & 255) / 255.0F;
        float f2 = (float) (start >> 8 & 255) / 255.0F;
        float f3 = (float) (start & 255) / 255.0F;
        float f4 = (float) (end >> 24 & 255) / 255.0F;
        float f5 = (float) (end >> 16 & 255) / 255.0F;
        float f6 = (float) (end >> 8 & 255) / 255.0F;
        float f7 = (float) (end & 255) / 255.0F;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.pos(x, height, 0f).color(f1, f2, f3, f).endVertex();
        bufferbuilder.pos(width, height, 0f).color(f5, f6, f7, f4).endVertex();
        bufferbuilder.pos(width, y, 0f).color(f5, f6, f7, f4).endVertex();
        bufferbuilder.pos(x, y, 0f).color(f1, f2, f3, f).endVertex();
    }

    public static void drawRect(
            double left,
            double top,
            double right,
            double bottom,
            int color) {
        if (left < right) {
            double i = left;
            left = right;
            right = i;
        }

        if (top < bottom) {
            double j = top;
            top = bottom;
            bottom = j;
        }

        float f3 = (float) (color >> 24 & 255) / 255.0F;
        float f = (float) (color >> 16 & 255) / 255.0F;
        float f1 = (float) (color >> 8 & 255) / 255.0F;
        float f2 = (float) (color & 255) / 255.0F;
        BufferBuilder bufferbuilder = Tessellator.getInstance().getBuffer();
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.defaultBlendFunc();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
        bufferbuilder.pos(left, bottom, 0.0F).color(f, f1, f2, f3).endVertex();
        bufferbuilder.pos(right, bottom, 0.0F).color(f, f1, f2, f3).endVertex();
        bufferbuilder.pos(right, top, 0.0F).color(f, f1, f2, f3).endVertex();
        bufferbuilder.pos(left, top, 0.0F).color(f, f1, f2, f3).endVertex();
        bufferbuilder.finishDrawing();
        WorldVertexBufferUploader.draw(bufferbuilder);
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    public static void drawRectW(
            double x,
            double y,
            double w,
            double h,
            int color) {

        w = x + w;
        h = y + h;

        if (x < w) {
            double i = x;
            x = w;
            w = i;
        }

        if (y < h) {
            double j = y;
            y = h;
            h = j;
        }

        float f3 = (float) (color >> 24 & 255) / 255.0F;
        float f = (float) (color >> 16 & 255) / 255.0F;
        float f1 = (float) (color >> 8 & 255) / 255.0F;
        float f2 = (float) (color & 255) / 255.0F;
        BufferBuilder bufferbuilder = Tessellator.getInstance().getBuffer();
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.defaultBlendFunc();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
        bufferbuilder.pos(x, h, 0.0F).color(f, f1, f2, f3).endVertex();
        bufferbuilder.pos(w, h, 0.0F).color(f, f1, f2, f3).endVertex();
        bufferbuilder.pos(w, y, 0.0F).color(f, f1, f2, f3).endVertex();
        bufferbuilder.pos(x, y, 0.0F).color(f, f1, f2, f3).endVertex();
        bufferbuilder.finishDrawing();
        WorldVertexBufferUploader.draw(bufferbuilder);
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    public static void drawRectVerticalW(double x, double y, double w, double h, int color, int color1) {
        w = x + w;
        h = y + h;

        if (x < w) {
            double i = x;
            x = w;
            w = i;
        }

        if (y < h) {
            double j = y;
            y = h;
            h = j;
        }

        float[] colorOne = ColorUtils.rgba(color);
        float[] colorTwo = ColorUtils.rgba(color1);
        BufferBuilder bufferbuilder = Tessellator.getInstance().getBuffer();
        RenderSystem.enableBlend();
        RenderSystem.shadeModel(7425);
        RenderSystem.disableTexture();
        RenderSystem.defaultBlendFunc();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_COLOR);
        bufferbuilder.pos(x, h, 0.0F).color(colorOne[0], colorOne[1], colorOne[2], colorOne[3]).endVertex();
        bufferbuilder.pos(w, h, 0.0F).color(colorTwo[0], colorTwo[1], colorTwo[2], colorTwo[3]).endVertex();
        bufferbuilder.pos(w, y, 0.0F).color(colorTwo[0], colorTwo[1], colorTwo[2], colorTwo[3]).endVertex();
        bufferbuilder.pos(x, y, 0.0F).color(colorOne[0], colorOne[1], colorOne[2], colorOne[3]).endVertex();
        bufferbuilder.finishDrawing();
        WorldVertexBufferUploader.draw(bufferbuilder);
        RenderSystem.enableTexture();
        RenderSystem.shadeModel(7424);
        RenderSystem.disableBlend();
    }

    public static void drawRoundedRect(float x, float y, float width, float height, Vector4f vector4f, int color) {
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();

        ShaderUtil.rounded.attach();
        ShaderUtil.rounded.setUniform("size", width * 2, height * 2);
        ShaderUtil.rounded.setUniform("round", vector4f.x * 2, vector4f.y * 2, vector4f.z * 2, vector4f.w * 2);
        ShaderUtil.rounded.setUniform("smoothness", 0.f, 1.5f);
        ShaderUtil.rounded.setUniform("color1", ColorUtils.rgba(color));
        ShaderUtil.rounded.setUniform("color2", ColorUtils.rgba(color));
        ShaderUtil.rounded.setUniform("color3", ColorUtils.rgba(color));
        ShaderUtil.rounded.setUniform("color4", ColorUtils.rgba(color));
        drawQuads(x, y, width, height, 7);

        ShaderUtil.rounded.detach();
        disableBlend();

        GlStateManager.popMatrix();
    }

    public static void drawRoundedRect(float x, float y, float width, float height, Vector4f vector4f, Vector4i color) {
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        ShaderUtil.rounded.attach();

        ShaderUtil.rounded.setUniform("size", width, height);

        float maxRadiusX = width / 2.0f;
        float maxRadiusY = height / 2.0f;
        float maxRadius = Math.min(maxRadiusX, maxRadiusY);

        float topRight = Math.min(vector4f.x, maxRadius);
        float bottomRight = Math.min(vector4f.y, maxRadius);
        float topLeft = Math.min(vector4f.z, maxRadius);
        float bottomLeft = Math.min(vector4f.w, maxRadius);

        ShaderUtil.rounded.setUniform("round", bottomLeft, topLeft, bottomRight, topRight);

        ShaderUtil.rounded.setUniform("smoothness", 0.0f, 0.5f);
        ShaderUtil.rounded.setUniform("color1", ColorUtils.rgba(color.getX()));
        ShaderUtil.rounded.setUniform("color2", ColorUtils.rgba(color.getY()));
        ShaderUtil.rounded.setUniform("color3", ColorUtils.rgba(color.getZ()));
        ShaderUtil.rounded.setUniform("color4", ColorUtils.rgba(color.getW()));
        drawQuads(x, y, width, height, 7);

        ShaderUtil.rounded.detach();
        disableBlend();
        GlStateManager.popMatrix();
    }

    public static void drawRoundedRect(float x, float y, float width, float height, float outline, int color1, Vector4f vector4f, Vector4i color) {
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        ShaderUtil.roundedout.attach();

        ShaderUtil.roundedout.setUniform("size", width * 2, height * 2);
        ShaderUtil.roundedout.setUniform("round", vector4f.x * 2, vector4f.y * 2, vector4f.z * 2, vector4f.w * 2);

        ShaderUtil.roundedout.setUniform("smoothness", 0.f, 1.5f);
        ShaderUtil.roundedout.setUniform("outlineColor", ColorUtils.rgba(color.getX()));
        ShaderUtil.roundedout.setUniform("outlineColor1", ColorUtils.rgba(color.getY()));
        ShaderUtil.roundedout.setUniform("outlineColor2", ColorUtils.rgba(color.getZ()));
        ShaderUtil.roundedout.setUniform("outlineColor3", ColorUtils.rgba(color.getW()));
        ShaderUtil.roundedout.setUniform("color", ColorUtils.rgba(color1));
        ShaderUtil.roundedout.setUniform("outline", outline);
        drawQuads(x, y, width, height, 7);

        ShaderUtil.rounded.detach();
        disableBlend();
        GlStateManager.popMatrix();
    }

    public static void drawMenuButton(float x, float y, float w, float h, float radius,
                                        int bgTL, int bgTR, int bgBL, int bgBR,
                                        int borderColor, float borderWidth,
                                        float glowIntensity, float glowRadius) {
        drawMenuButton(x, y, w, h, radius, bgTL, bgTR, bgBL, bgBR, borderColor, borderWidth,
                glowIntensity, glowRadius, -10000f, -10000f, 0f, 1f, 0, 0f);
    }

    public static void drawMenuButton(float x, float y, float w, float h, float radius,
                                        int bgTL, int bgTR, int bgBL, int bgBR,
                                        int borderColor, float borderWidth,
                                        float glowIntensity, float glowRadius,
                                        float clickX, float clickY,
                                        float revealRadius, float revealSoftness,
                                        int revealColor, float revealAlpha) {
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        ShaderUtil.menuButton.attach();
        ShaderUtil.menuButton.setUniform("size", w, h);
        ShaderUtil.menuButton.setUniform("round", radius, radius, radius, radius);
        ShaderUtil.menuButton.setUniform("smoothness", 0.0f, 1.0f);
        ShaderUtil.menuButton.setUniform("bgColor1", ColorUtils.rgba(bgTL));
        ShaderUtil.menuButton.setUniform("bgColor2", ColorUtils.rgba(bgBL));
        ShaderUtil.menuButton.setUniform("bgColor3", ColorUtils.rgba(bgBR));
        ShaderUtil.menuButton.setUniform("bgColor4", ColorUtils.rgba(bgTR));
        ShaderUtil.menuButton.setUniform("borderColor", ColorUtils.rgba(borderColor));
        ShaderUtil.menuButton.setUniform("borderWidth", borderWidth);
        ShaderUtil.menuButton.setUniform("glowIntensity", glowIntensity);
        ShaderUtil.menuButton.setUniform("glowRadius", glowRadius);
        ShaderUtil.menuButton.setUniform("clickPoint", clickX, clickY);
        ShaderUtil.menuButton.setUniform("revealRadius", revealRadius);
        ShaderUtil.menuButton.setUniform("revealSoftness", revealSoftness);
        ShaderUtil.menuButton.setUniform("revealColor", ColorUtils.rgba(revealColor));
        ShaderUtil.menuButton.setUniform("revealAlpha", revealAlpha);
        drawQuads(x, y, w, h, 7);

        ShaderUtil.menuButton.detach();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    public static void drawRadialFillButton(float x, float y, float w, float h, float radius,
                                            int bgTL, int bgTR, int bgBL, int bgBR,
                                            int activeTL, int activeTR, int activeBL, int activeBR,
                                            int borderColor, float borderWidth,
                                            float glowIntensity, float glowRadius,
                                            float clickX, float clickY,
                                            float progress, float state) {
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        ShaderUtil.radialFill.attach();
        ShaderUtil.radialFill.setUniform("size", w, h);
        ShaderUtil.radialFill.setUniform("round", radius, radius, radius, radius);
        ShaderUtil.radialFill.setUniform("bgColor1", ColorUtils.rgba(bgTL));
        ShaderUtil.radialFill.setUniform("bgColor2", ColorUtils.rgba(bgBL));
        ShaderUtil.radialFill.setUniform("bgColor3", ColorUtils.rgba(bgBR));
        ShaderUtil.radialFill.setUniform("bgColor4", ColorUtils.rgba(bgTR));
        ShaderUtil.radialFill.setUniform("activeColor1", ColorUtils.rgba(activeTL));
        ShaderUtil.radialFill.setUniform("activeColor2", ColorUtils.rgba(activeBL));
        ShaderUtil.radialFill.setUniform("activeColor3", ColorUtils.rgba(activeBR));
        ShaderUtil.radialFill.setUniform("activeColor4", ColorUtils.rgba(activeTR));
        ShaderUtil.radialFill.setUniform("borderColor", ColorUtils.rgba(borderColor));
        ShaderUtil.radialFill.setUniform("borderWidth", borderWidth);
        ShaderUtil.radialFill.setUniform("glowIntensity", glowIntensity);
        ShaderUtil.radialFill.setUniform("glowRadius", glowRadius);
        ShaderUtil.radialFill.setUniform("clickPoint", clickX, clickY);
        ShaderUtil.radialFill.setUniform("progress", progress);
        ShaderUtil.radialFill.setUniform("state", state);
        drawQuads(x, y, w, h, 7);

        ShaderUtil.radialFill.detach();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private static Framebuffer whiteCache = new Framebuffer(1, 1, false, true);

    public static void drawWhite(float state) {
        state = MathHelper.clamp(state, 0, 1);
        GlStateManager.enableBlend();
        GlStateManager.color4f(1, 1, 1, 1);
        GlStateManager.glBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

        whiteCache = ShaderUtil.createFrameBuffer(whiteCache);

        whiteCache.framebufferClear(false);
        whiteCache.bindFramebuffer(true);

        ShaderUtil.white.attach();
        ShaderUtil.white.setUniform("texture", 0);
        ShaderUtil.white.setUniformf("state", state);
        GlStateManager.bindTexture(mc.getFramebuffer().framebufferTexture);

        ShaderUtil.drawQuads();
        whiteCache.unbindFramebuffer();
        ShaderUtil.white.detach();
        mc.getFramebuffer().bindFramebuffer(true);

        ShaderUtil.white.attach();
        ShaderUtil.white.setUniform("texture", 0);
        ShaderUtil.white.setUniformf("state", state);
        GlStateManager.bindTexture(whiteCache.framebufferTexture);
        ShaderUtil.drawQuads();
        ShaderUtil.white.detach();

        GlStateManager.color4f(1, 1, 1, 1);
        GlStateManager.bindTexture(0);
    }

    public static void drawRoundedRect(float x, float y, float width, float height, float radius, int color) {
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        ShaderUtil.smooth.attach();

        MainWindow mainWindow = mc.getMainWindow();
        float scaleFactor = (float) mainWindow.getGuiScaleFactor();
        ShaderUtil.smooth.setUniformf("location", x * scaleFactor, (mainWindow.getHeight() - (height * scaleFactor)) - (y * scaleFactor));
        ShaderUtil.smooth.setUniformf("rectSize", width * scaleFactor, height * scaleFactor);
        ShaderUtil.smooth.setUniformf("radius", radius * scaleFactor);
        ShaderUtil.smooth.setUniform("blur", 0);
        ShaderUtil.smooth.setUniform("color", ColorUtils.rgba(color));
        drawQuads(x, y, width, height, 7);

        ShaderUtil.smooth.detach();
        disableBlend();
        GlStateManager.popMatrix();
    }

    public static void drawCircle(float x, float y, float radius, int color) {
        drawRoundedRect(x - radius / 2f, y - radius / 2f, radius, radius, radius / 2f, color);
    }

    public static void drawQuads(float x, float y, float width, float height, int glQuads) {
        buffer.begin(glQuads, POSITION_TEX);
        {
            buffer.pos(x, y, 0).tex(0, 0).endVertex();
            buffer.pos(x, y + height, 0).tex(0, 1).endVertex();
            buffer.pos(x + width, y + height, 0).tex(1, 1).endVertex();
            buffer.pos(x + width, y, 0).tex(1, 0).endVertex();
        }
        Tessellator.getInstance().draw();
    }

    public static void drawBox(double x, double y, double width, double height, double size, int color) {
        drawRectBuilding(x + size, y, width - size, y + size, color);
        drawRectBuilding(x, y, x + size, height, color);

        drawRectBuilding(width - size, y, width, height, color);
        drawRectBuilding(x + size, height - size, width - size, height, color);
    }

    public static void drawBoxTest(double x, double y, double width, double height, double size, Vector4i colors) {
        drawMCHorizontalBuilding(x + size, y, width - size, y + size, colors.x, colors.z);
        drawMCVerticalBuilding(x, y, x + size, height, colors.z, colors.x);

        drawMCVerticalBuilding(width - size, y, width, height, colors.x, colors.z);
        drawMCHorizontalBuilding(x + size, height - size, width - size, height, colors.z, colors.x);
    }

    public static void drawImageAlpha(ResourceLocation resourceLocation, float x, float y, float width, float height,
                                      int color) {
        RenderSystem.pushMatrix();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.shadeModel(7425);
        mc.getTextureManager().bindTexture(resourceLocation);
        quads(x, y, width, height, 7, color);
        RenderSystem.shadeModel(7424);
        RenderSystem.color4f(1, 1, 1, 1);
        RenderSystem.popMatrix();
    }

    public static void drawImageAlphaSmooth(ResourceLocation resourceLocation, float x, float y, float width, float height,
                                            int color) {
        RenderSystem.pushMatrix();
        RenderSystem.enableBlend();
        RenderSystem.disableAlphaTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.shadeModel(7425);
        mc.getTextureManager().bindTexture(resourceLocation);

        Texture texture = mc.getTextureManager().getTexture(resourceLocation);
        if (texture != null) {
            texture.setBlurMipmap(true, false);
        }

        try {
            quads(x, y, width, height, 7, color);
        } finally {
            if (texture != null) {
                texture.restoreLastBlurMipmap();
            }
            RenderSystem.shadeModel(7424);
            RenderSystem.color4f(1, 1, 1, 1);
            RenderSystem.enableAlphaTest();
            RenderSystem.popMatrix();
        }
    }

    public static void drawImageAlphaSmooth(MatrixStack matrix, ResourceLocation resourceLocation, float x, float y, float width, float height,
                                            int color) {
        RenderSystem.pushMatrix();
        RenderSystem.enableBlend();
        RenderSystem.disableAlphaTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.shadeModel(7425);
        mc.getTextureManager().bindTexture(resourceLocation);

        Texture texture = mc.getTextureManager().getTexture(resourceLocation);
        if (texture != null) {
            texture.setBlurMipmap(true, false);
        }

        try {
            buffer.begin(7, POSITION_TEX_COLOR);
            buffer.pos(matrix.getLast().getMatrix(), x, y, 0).tex(0, 0).color(color).endVertex();
            buffer.pos(matrix.getLast().getMatrix(), x, y + height, 0).tex(0, 1).color(color).endVertex();
            buffer.pos(matrix.getLast().getMatrix(), x + width, y + height, 0).tex(1, 1).color(color).endVertex();
            buffer.pos(matrix.getLast().getMatrix(), x + width, y, 0).tex(1, 0).color(color).endVertex();
            tessellator.draw();
        } finally {
            if (texture != null) {
                texture.restoreLastBlurMipmap();
            }
            RenderSystem.shadeModel(7424);
            RenderSystem.color4f(1, 1, 1, 1);
            RenderSystem.enableAlphaTest();
            RenderSystem.popMatrix();
        }
    }

    public static void drawImageAlpha(ResourceLocation resourceLocation, float x, float y, float width, float height, Vector4i color) {
        RenderSystem.pushMatrix();
        RenderSystem.disableLighting();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.shadeModel(7425);
        RenderSystem.disableCull();
        RenderSystem.disableAlphaTest();
        RenderSystem.blendFuncSeparate(770, 1, 0, 1);
        mc.getTextureManager().bindTexture(resourceLocation);
        buffer.begin(7, POSITION_TEX_COLOR);
        {
            buffer.pos(x, y, 0).tex(0, 1 - 0.01f).lightmap(0, 240).color(color.x).endVertex();
            buffer.pos(x, y + height, 0).tex(1, 1 - 0.01f).lightmap(0, 240).color(color.y).endVertex();
            buffer.pos(x + width, y + height, 0).tex(1, 0).lightmap(0, 240).color(color.z).endVertex();
            buffer.pos(x + width, y, 0).tex(0, 0).lightmap(0, 240).color(color.w).endVertex();

        }
        tessellator.draw();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.enableAlphaTest();
        RenderSystem.depthMask(true);
        RenderSystem.popMatrix();
    }

    public static void drawRoundedOutline(float x, float y, float x2, float y2, float radius, float lineWidth, int color) {
        float width = x2 - x;
        float height = y2 - y;

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glLineWidth(lineWidth);

        float alpha = (color >> 24 & 0xFF) / 255.0f;
        float red = (color >> 16 & 0xFF) / 255.0f;
        float green = (color >> 8 & 0xFF) / 255.0f;
        float blue = (color & 0xFF) / 255.0f;

        GL11.glColor4f(red, green, blue, alpha);

        GL11.glBegin(GL11.GL_LINE_LOOP);

        GL11.glVertex2f(x + radius, y);
        GL11.glVertex2f(x2 - radius, y);

        for (int i = 0; i <= 90; i += 5) {
            float dx = cosDeg5(i) * radius;
            float dy = sinDeg5(i) * radius;
            GL11.glVertex2f(x2 - radius + dx, y + radius - dy);
        }

        GL11.glVertex2f(x2, y + radius);
        GL11.glVertex2f(x2, y2 - radius);

        for (int i = 90; i <= 180; i += 5) {
            float dx = cosDeg5(i) * radius;
            float dy = sinDeg5(i) * radius;
            GL11.glVertex2f(x2 - radius + dx, y2 - radius - dy);
        }

        GL11.glVertex2f(x2 - radius, y2);
        GL11.glVertex2f(x + radius, y2);

        for (int i = 180; i <= 270; i += 5) {
            float dx = cosDeg5(i) * radius;
            float dy = sinDeg5(i) * radius;
            GL11.glVertex2f(x + radius + dx, y2 - radius - dy);
        }

        GL11.glVertex2f(x, y2 - radius);
        GL11.glVertex2f(x, y + radius);

        for (int i = 270; i <= 360; i += 5) {
            float dx = cosDeg5(i) * radius;
            float dy = sinDeg5(i) * radius;
            GL11.glVertex2f(x + radius + dx, y + radius - dy);
        }

        GL11.glEnd();

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.enableTexture();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    public static void drawCircleWithRotatingGradient(float x, float y, float start, float end, float radius, float width, boolean filled, int color, float rotation) {
        if (start > end) {
            float endOffset = end;
            end = start;
            start = endOffset;
        }

        GlStateManager.enableBlend();
        RenderSystem.disableAlphaTest();
        GL11.glDisable(3553);
        RenderSystem.blendFuncSeparate(770, 771, 1, 0);
        RenderSystem.shadeModel(7425);
        GL11.glEnable(2848);
        GL11.glLineWidth(width);
        int endColor = ColorUtils.brighter(color, 0.7f);

        GL11.glBegin(3);
        for (float i = start; i <= end; i += 1.0f) {

            float rotatedAngle = (i + rotation) % 360;

            float progress = (float) ((Math.sin(rotatedAngle * 2.0D * DEG_TO_RAD_D) + 1.0D) * 0.5D);

            int gradientColor = ColorUtils.interpolate(color, endColor, progress);
            ColorUtils.setColor(gradientColor);

            float angle = i * DEG_TO_RAD;
            float cos = MathHelper.cos(angle) * radius;
            float sin = MathHelper.sin(angle) * radius;
            GL11.glVertex2f(x + cos, y + sin);
        }
        GL11.glEnd();
        GL11.glDisable(2848);

        if (filled) {
            GL11.glBegin(6);
            GL11.glVertex2f(x, y);

            for (float i = start; i <= end; i += 1.0f) {
                float rotatedAngle = (i + rotation) % 360;
                float progress = (float) ((Math.sin(rotatedAngle * 2.0D * DEG_TO_RAD_D) + 1.0D) * 0.5D);

                int gradientColor = ColorUtils.interpolate(color, endColor, progress);
                ColorUtils.setColor(gradientColor);

                float angle = i * DEG_TO_RAD;
                float cos = MathHelper.cos(angle) * radius;
                float sin = MathHelper.sin(angle) * radius;
                GL11.glVertex2f(x + cos, y + sin);
            }
            GL11.glEnd();
        }

        RenderSystem.enableAlphaTest();
        RenderSystem.shadeModel(7424);
        GL11.glEnable(3553);
        disableBlend();
    }

    public static void drawRingArcAAWithRotatingGradient(float cx, float cy,
                                                         float radius, float thickness,
                                                         float startDeg, float endDeg,
                                                         int rgba, float featherPx,
                                                         float rotationDeg) {
        float w = radius * 2f + featherPx * 2f;
        float h = w;
        float x = cx - w / 2f;
        float y = cy - h / 2f;

        RenderSystem.pushMatrix();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        ShaderUtil.ringArcGradient.attach();

        int brightColor = ColorUtils.brighter(rgba, 0.35f);
        int darkColor = ColorUtils.darker(rgba, 0.25f);

        ShaderUtil.ringArcGradient.setUniform("color", ColorUtils.rgba(darkColor));
        ShaderUtil.ringArcGradient.setUniform("brightColor", ColorUtils.rgba(brightColor));

        ShaderUtil.ringArcGradient.setUniformf("size", w, h);
        ShaderUtil.ringArcGradient.setUniformf("center", cx - x, cy - y);
        ShaderUtil.ringArcGradient.setUniformf("radius", radius);
        ShaderUtil.ringArcGradient.setUniformf("thickness", thickness);
        ShaderUtil.ringArcGradient.setUniformf("feather", featherPx);
        ShaderUtil.ringArcGradient.setUniformf("startAngle", startDeg * DEG_TO_RAD);
        ShaderUtil.ringArcGradient.setUniformf("endAngle", endDeg * DEG_TO_RAD);
        ShaderUtil.ringArcGradient.setUniformf("rotation", rotationDeg * DEG_TO_RAD);

        ShaderUtil.ringArcGradient.drawQuads(x, y, w, h);
        ShaderUtil.ringArcGradient.detach();

        RenderSystem.disableBlend();
        RenderSystem.popMatrix();
    }

    public static void drawRingArcAA(float cx, float cy,
                                     float radius, float thickness,
                                     float startDeg, float endDeg,
                                     int rgba, float featherPx) {
        float w = radius * 2f + featherPx * 2f;
        float h = w;
        float x = cx - w / 2f;
        float y = cy - h / 2f;

        RenderSystem.pushMatrix();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        ShaderUtil.ringArc.attach();
        ShaderUtil.ringArc.setUniform("color", ColorUtils.rgba(rgba));
        ShaderUtil.ringArc.setUniformf("size",   w, h);
        ShaderUtil.ringArc.setUniformf("center", cx - x, cy - y);
        ShaderUtil.ringArc.setUniformf("radius",    radius);
        ShaderUtil.ringArc.setUniformf("thickness", thickness);
        ShaderUtil.ringArc.setUniformf("feather",   featherPx);
        ShaderUtil.ringArc.setUniformf("startAngle", startDeg * DEG_TO_RAD);
        ShaderUtil.ringArc.setUniformf("endAngle",   endDeg * DEG_TO_RAD);
        ShaderUtil.ringArc.drawQuads(x, y, w, h);
        ShaderUtil.ringArc.detach();

        RenderSystem.disableBlend();
        RenderSystem.popMatrix();
    }

    public static void drawRingAA(float cx, float cy,
                                  float radius, float thickness,
                                  int rgba, float featherPx) {
        drawRingArcAA(cx, cy, radius, thickness, 0f, 360f, rgba, featherPx);
    }

    public static void drawCircleProgressWithRotatingGradient(float x, float y, float start, float end, float radius, float width, boolean filled, int color, float rotation) {
        if (start > end) {
            float endOffset = end;
            end = start;
            start = endOffset;
        }

        GlStateManager.enableBlend();
        RenderSystem.disableAlphaTest();
        GL11.glDisable(3553);
        RenderSystem.blendFuncSeparate(770, 771, 1, 0);
        RenderSystem.shadeModel(7425);
        GL11.glEnable(2848);
        GL11.glLineWidth(width);

        int brightColor = ColorUtils.brighter(color, 0.35f);
        int darkColor = ColorUtils.darker(color, 0.25f);

        GL11.glBegin(3);
        for (float i = start; i <= end; i += 1.0f) {

            float rotatedAngle = (i + rotation) % 360;

            float progress = (float) ((Math.sin(rotatedAngle * 2.0D * DEG_TO_RAD_D) + 1.0D) * 0.5D);

            int gradientColor = ColorUtils.interpolate(brightColor, darkColor, progress);
            ColorUtils.setColor(gradientColor);

            float angle = i * DEG_TO_RAD;
            float cos = MathHelper.cos(angle) * radius;
            float sin = MathHelper.sin(angle) * radius;
            GL11.glVertex2f(x + cos, y + sin);
        }
        GL11.glEnd();
        GL11.glDisable(2848);

        if (filled) {
            GL11.glBegin(6);
            GL11.glVertex2f(x, y);
            for (float i = start; i <= end; i += 1.0f) {
                float rotatedAngle = (i + rotation) % 360;
                float progress = (float) ((Math.sin(rotatedAngle * 2.0D * DEG_TO_RAD_D) + 1.0D) * 0.5D);

                int gradientColor = ColorUtils.interpolate(brightColor, darkColor, progress);
                ColorUtils.setColor(gradientColor);

                float angle = i * DEG_TO_RAD;
                float cos = MathHelper.cos(angle) * radius;
                float sin = MathHelper.sin(angle) * radius;
                GL11.glVertex2f(x + cos, y + sin);
            }
            GL11.glEnd();
        }

        RenderSystem.enableAlphaTest();
        RenderSystem.shadeModel(7424);
        GL11.glEnable(3553);
        disableBlend();
    }

    public static void drawRoundedRectWithRotatingGradient(float x, float y, float w, float h, float radius,
                                                           int baseColor, float rotation, float alpha) {
        RenderSystem.pushMatrix();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        int brightColor = ColorUtils.brighter(baseColor, 0.35f);
        int darkColor = ColorUtils.darker(baseColor, 0.25f);

        ShaderUtil.gradientBar.attach();
        ShaderUtil.gradientBar.setUniform("colorA", ColorUtils.rgba(ColorUtils.reAlphaInt(darkColor, (int)(255 * alpha))));
        ShaderUtil.gradientBar.setUniform("colorB", ColorUtils.rgba(ColorUtils.reAlphaInt(brightColor, (int)(255 * alpha))));
        ShaderUtil.gradientBar.setUniformf("size", w, h);
        ShaderUtil.gradientBar.setUniformf("radius", radius);
        ShaderUtil.gradientBar.setUniformf("rotation", rotation * DEG_TO_RAD);
        ShaderUtil.gradientBar.setUniformf("position", x, y);

        ShaderUtil.gradientBar.drawQuads(x, y, w, h);
        ShaderUtil.gradientBar.detach();

        RenderSystem.disableBlend();
        RenderSystem.popMatrix();
    }

    public static final class FrameBuffer {
        private FrameBuffer() {
            throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
        }

        public static Framebuffer createFrameBuffer(Framebuffer framebuffer) {
            return createFrameBuffer(framebuffer, false);
        }

        public static Framebuffer createFrameBuffer(Framebuffer framebuffer, boolean depth) {
            if (needsNewFramebuffer(framebuffer)) {
                if (framebuffer != null) {
                    framebuffer.deleteFramebuffer();
                }
                MainWindow mainWindow = mc.getMainWindow();
                int frameBufferWidth = mainWindow.getFramebufferWidth();
                int frameBufferHeight = mainWindow.getFramebufferHeight();
                return new Framebuffer(frameBufferWidth, frameBufferHeight, depth);
            }
            return framebuffer;
        }

        public static boolean needsNewFramebuffer(Framebuffer framebuffer) {
            MainWindow mainWindow = mc.getMainWindow();
            return framebuffer == null || framebuffer.framebufferWidth != mainWindow.getFramebufferWidth() || framebuffer.framebufferHeight != mainWindow.getFramebufferHeight();
        }
    }

    public static float[] rgb(final int color) {
        return new float[]{
                (color >> 16 & 0xFF) / 255f,
                (color >> 8 & 0xFF) / 255f,
                (color & 0xFF) / 255f,
                (color >> 24 & 0xFF) / 255f
        };
    }

    public static class Vec2fColored {
        float x;
        float y;
        int color;

        public Vec2fColored(float x, float y, int color) {
            this.x = x;
            this.y = y;
            this.color = color;
        }

        public float getX() {
            return x;
        }

        public float getY() {
            return y;
        }

        public int getColor() {
            return color;
        }
    }

    public static class Render2D {
        public static void drawRect(float x, float y, float width, float height, int color) {
            RenderUtility.drawRect(x, y, x + width, y + height, color);
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
    }
}

