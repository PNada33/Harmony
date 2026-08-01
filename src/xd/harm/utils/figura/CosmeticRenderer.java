package xd.harm.utils.figura;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.settings.PointOfView;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3f;
import org.lwjgl.opengl.GL11;
import xd.harm.events.render.EventRender3D;
import xd.harm.modules.impl.render.Theme;
import xd.harm.utils.render.color.ColorUtils;

/**
 * Рисует косметику Figura Cosmetic в мире: катану на спине и китайскую шляпу.
 *
 * Код перенесён один в один из бывших модулей Katana и ChinaHat, только
 * настройки берутся из {@link CosmeticFeatures}, а не из Setting-ов модуля.
 */
public final class CosmeticRenderer {

    private static final CosmeticRenderer INSTANCE = new CosmeticRenderer();

    private CosmeticRenderer() {
    }

    public static CosmeticRenderer get() {
        return INSTANCE;
    }

    // ------------------------------------------------------------ Настройки

    /** Цвет функции: либо цвет темы клиента, либо свой. */
    public static int color(String feature) {
        if (CosmeticFeatures.usesThemeColor(feature)) {
            return Theme.MainColor(0);
        }
        return CosmeticFeatures.getColor(feature, "color", ColorUtils.rgb(255, 255, 255));
    }

    public static float katanaFillAlpha() {
        return CosmeticFeatures.getFloat(CosmeticFeatures.KATANA, "fillAlpha", 20f, 0f, 100f);
    }

    public static float katanaOutlineAlpha() {
        return CosmeticFeatures.getFloat(CosmeticFeatures.KATANA, "outlineAlpha", 85f, 0f, 100f);
    }

    public static boolean katanaGlow() {
        return CosmeticFeatures.getBool(CosmeticFeatures.KATANA, "glow", true);
    }

    public static float katanaGlowLevel() {
        return CosmeticFeatures.getFloat(CosmeticFeatures.KATANA, "glowLevel", 50f, 0f, 100f);
    }

    public static float hatRadius() {
        return CosmeticFeatures.getFloat(CosmeticFeatures.CHINA_HAT, "radius", 6f, 2f, 12f);
    }

    public static float hatHeight() {
        return CosmeticFeatures.getFloat(CosmeticFeatures.CHINA_HAT, "height", 4f, 1f, 10f);
    }

    public static float hatSegments() {
        return CosmeticFeatures.getFloat(CosmeticFeatures.CHINA_HAT, "segments", 48f, 12f, 96f);
    }

    public static boolean hatOutline() {
        return CosmeticFeatures.getBool(CosmeticFeatures.CHINA_HAT, "outline", true);
    }

    // --------------------------------------------------------------- Рендер

    @Subscribe
    public void onRender(EventRender3D event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.world == null) {
            return;
        }
        if (mc.gameSettings.getPointOfView() == PointOfView.FIRST_PERSON) {
            return;
        }

        boolean katana = CosmeticFeatures.isEnabled(CosmeticFeatures.KATANA);
        if (!katana) {
            return;
        }

        float pt = event.getPartialTicks();
        double x = MathHelper.lerp(pt, mc.player.lastTickPosX, mc.player.getPosX()) - mc.getRenderManager().renderPosX();
        double y = MathHelper.lerp(pt, mc.player.lastTickPosY, mc.player.getPosY()) - mc.getRenderManager().renderPosY();
        double z = MathHelper.lerp(pt, mc.player.lastTickPosZ, mc.player.getPosZ()) - mc.getRenderManager().renderPosZ();
        float yaw = MathHelper.lerp(pt, mc.player.prevRenderYawOffset, mc.player.renderYawOffset);

        renderKatana(event.getStack(), x, y, z, yaw);
    }

    // --------------------------------------------------------------- Катана

    private void renderKatana(MatrixStack ms, double x, double y, double z, float yaw) {
        ms.push();
        ms.translate(x, y + 1.32, z);
        ms.rotate(Vector3f.YP.rotationDegrees(-yaw));
        ms.translate(-0.15, 0.10, -0.28);
        ms.rotate(Vector3f.ZP.rotationDegrees(-45f));
        ms.rotate(Vector3f.XP.rotationDegrees(-3f));
        ms.scale(1.15f, 1.15f, 1.15f);

        renderKatanaGeometry(ms);
        ms.pop();
    }

    /**
     * Общая геометрия катаны: используется и в мире, и в 3D-превью GUI.
     */
    public static void renderKatanaGeometry(MatrixStack ms) {
        int color = color(CosmeticFeatures.KATANA);
        int r = color >> 16 & 255;
        int g = color >> 8 & 255;
        int b = color & 255;
        int fill = MathHelper.clamp((int) (katanaFillAlpha() / 100f * 255f), 0, 255);
        int outline = MathHelper.clamp((int) (katanaOutlineAlpha() / 100f * 255f), 0, 255);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);

        katanaParts(ms, r, g, b, fill, false, 0f);
        katanaParts(ms, r, g, b, outline, true, 0f);

        if (katanaGlow() && katanaGlowLevel() > 0f) {
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            float power = katanaGlowLevel() / 100f;
            for (int i = 0; i < 7; i++) {
                float t = i / 6f;
                int a = (int) (power * (1f - t) * (1f - t) * 70f);
                if (a > 0) {
                    katanaParts(ms, r, g, b, a, false, 0.008f + t * 0.038f);
                }
            }
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
        }

        RenderSystem.enableTexture();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.color4f(1f, 1f, 1f, 1f);
    }

    private static void katanaParts(MatrixStack ms, int r, int g, int b, int a, boolean edges, float ex) {
        part(ms, -0.042f, -0.48f, -0.024f, 0.042f, -0.42f, 0.024f, r, g, b, a, edges, ex);
        part(ms, -0.032f, -0.42f, -0.018f, 0.032f, 0.00f, 0.018f, r, g, b, a, edges, ex);
        part(ms, -0.085f, 0.00f, -0.034f, 0.085f, 0.05f, 0.034f, r, g, b, a, edges, ex);
        part(ms, -0.022f, 0.05f, -0.010f, 0.022f, 0.96f, 0.010f, r, g, b, a, edges, ex);
        part(ms, -0.012f, 0.96f, -0.007f, 0.012f, 1.07f, 0.007f, r, g, b, a, edges, ex);
    }

    private static void part(MatrixStack ms, float x0, float y0, float z0, float x1, float y1, float z1,
                      int r, int g, int b, int a, boolean edges, float ex) {
        x0 -= ex; y0 -= ex; z0 -= ex; x1 += ex; y1 += ex; z1 += ex;
        Matrix4f m = ms.getLast().getMatrix();
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        if (edges) {
            RenderSystem.lineWidth(1.35f);
            buf.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
            edge(buf,m,x0,y0,z0,x1,y0,z0,r,g,b,a); edge(buf,m,x1,y0,z0,x1,y1,z0,r,g,b,a);
            edge(buf,m,x1,y1,z0,x0,y1,z0,r,g,b,a); edge(buf,m,x0,y1,z0,x0,y0,z0,r,g,b,a);
            edge(buf,m,x0,y0,z1,x1,y0,z1,r,g,b,a); edge(buf,m,x1,y0,z1,x1,y1,z1,r,g,b,a);
            edge(buf,m,x1,y1,z1,x0,y1,z1,r,g,b,a); edge(buf,m,x0,y1,z1,x0,y0,z1,r,g,b,a);
            edge(buf,m,x0,y0,z0,x0,y0,z1,r,g,b,a); edge(buf,m,x1,y0,z0,x1,y0,z1,r,g,b,a);
            edge(buf,m,x1,y1,z0,x1,y1,z1,r,g,b,a); edge(buf,m,x0,y1,z0,x0,y1,z1,r,g,b,a);
        } else {
            buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            face(buf,m,x0,y0,z0, x1,y0,z0, x1,y0,z1, x0,y0,z1,r,g,b,a);
            face(buf,m,x0,y1,z0, x0,y1,z1, x1,y1,z1, x1,y1,z0,r,g,b,a);
            face(buf,m,x0,y0,z0, x0,y1,z0, x1,y1,z0, x1,y0,z0,r,g,b,a);
            face(buf,m,x0,y0,z1, x1,y0,z1, x1,y1,z1, x0,y1,z1,r,g,b,a);
            face(buf,m,x0,y0,z0, x0,y0,z1, x0,y1,z1, x0,y1,z0,r,g,b,a);
            face(buf,m,x1,y0,z0, x1,y1,z0, x1,y1,z1, x1,y0,z1,r,g,b,a);
        }
        tess.draw();
    }

    private static void edge(BufferBuilder b, Matrix4f m, float x0,float y0,float z0,float x1,float y1,float z1,int r,int g,int bl,int a) {
        v(b,m,x0,y0,z0,r,g,bl,a); v(b,m,x1,y1,z1,r,g,bl,a);
    }

    private static void face(BufferBuilder b, Matrix4f m, float x0,float y0,float z0,float x1,float y1,float z1,
                      float x2,float y2,float z2,float x3,float y3,float z3,int r,int g,int bl,int a) {
        v(b,m,x0,y0,z0,r,g,bl,a); v(b,m,x1,y1,z1,r,g,bl,a); v(b,m,x2,y2,z2,r,g,bl,a); v(b,m,x3,y3,z3,r,g,bl,a);
    }

    private static void v(BufferBuilder b, Matrix4f m, float x,float y,float z,int r,int g,int bl,int a) {
        b.pos(m,x,y,z).color(r,g,bl,a).endVertex();
    }

    // ----------------------------------------------------------------- Плащ

    /**
     * Цвет плаща. Отдельный метод, потому что у плаща своя палитра тем,
     * унаследованная от старого модуля Raincoat.
     */
    public static int capeColor() {
        if (!CosmeticFeatures.usesThemeColor(CosmeticFeatures.RAINCOAT)) {
            return CosmeticFeatures.getColor(CosmeticFeatures.RAINCOAT, "color", ColorUtils.rgb(255, 255, 255));
        }
        String theme = Theme.THEME.get();
        int stable = themeCapeColor(theme);
        return stable != 0 ? stable : Theme.MainColor(0);
    }

    private static int themeCapeColor(String theme) {
        if (theme == null) {
            return 0;
        }
        switch (theme) {
            case "Aubergine": return ColorUtils.rgb(170, 7, 107);
            case "Aqua": return ColorUtils.rgb(185, 250, 255);
            case "Banana": return ColorUtils.rgb(253, 236, 177);
            case "Blend": return ColorUtils.rgb(71, 148, 253);
            case "Blossom": return ColorUtils.rgb(226, 208, 249);
            case "Bubblegum": return ColorUtils.rgb(243, 145, 216);
            case "Candy Cane": return ColorUtils.rgb(255, 255, 255);
            case "Cherry": return ColorUtils.rgb(187, 55, 125);
            case "Christmas": return ColorUtils.rgb(255, 64, 64);
            case "Coral": return ColorUtils.rgb(244, 168, 150);
            case "Digital Horizon": return ColorUtils.rgb(95, 195, 228);
            case "Express": return ColorUtils.rgb(173, 83, 137);
            case "Lime Water": return ColorUtils.rgb(18, 255, 247);
            case "Lush": return ColorUtils.rgb(168, 224, 99);
            case "Halogen": return ColorUtils.rgb(255, 65, 108);
            case "Hyper": return ColorUtils.rgb(236, 110, 173);
            case "Magic": return ColorUtils.rgb(74, 0, 224);
            case "May": return ColorUtils.rgb(253, 219, 245);
            case "Orange Juice": return ColorUtils.rgb(252, 74, 26);
            case "Pastel": return ColorUtils.rgb(243, 155, 178);
            case "Pumpkin": return ColorUtils.rgb(241, 166, 98);
            case "Satin": return ColorUtils.rgb(215, 60, 67);
            case "Snowy Sky": return ColorUtils.rgb(1, 171, 179);
            case "Steel Fade": return ColorUtils.rgb(66, 134, 244);
            case "Sundae": return ColorUtils.rgb(206, 74, 126);
            case "Sunkist": return ColorUtils.rgb(242, 201, 76);
            case "Water": return ColorUtils.rgb(12, 232, 199);
            case "Winter": return ColorUtils.rgb(255, 255, 255);
            case "Wood": return ColorUtils.rgb(79, 109, 81);
            default: return 0;
        }
    }
}
