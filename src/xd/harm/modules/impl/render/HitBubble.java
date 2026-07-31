package xd.harm.modules.impl.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import org.lwjgl.opengl.GL11;
import xd.harm.Harmony;
import xd.harm.events.combat.AttackEvent;
import xd.harm.events.render.EventRender3D;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.impl.combat.HitAura;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.ColorSetting;
import xd.harm.modules.settings.impl.ModeSetting;
import xd.harm.utils.render.color.ColorUtils;
import xd.harm.utils.voronoi.UVoronoiIntegration;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@ModuleRegister(name = "HitBubble", category = Category.Render, desc = "Хитбабл все дела")
public class HitBubble extends Module {

    public static HitBubble get;

    private static final List<Bubble> BUBBLES = new ArrayList<>();
    private static final int ROTATE_SPEED = 4;
    private static final long ROTATION_PERIOD_MS = 3600L / ROTATE_SPEED;
    private static final int COLOR_MODE_CLIENT = 0;
    private static final int COLOR_MODE_RAINBOW = 1;
    private static final int COLOR_MODE_CUSTOM = 2;
    private static final int COLOR_MODE_CUSTOM_TWO = 3;

    private final ModeSetting colorMode = new ModeSetting("Цвет", "Клиент",
            "Радужный", "Клиент", "Свой", "Свой 2 цвета");
    private final ColorSetting pickColor1 = new ColorSetting("Цвет 1", ColorUtils.rgb(100, 255, 100))
            .setVisible(() -> colorMode.get().contains("Свой"));
    private final ColorSetting pickColor2 = new ColorSetting("Цвет 2", ColorUtils.rgb(60, 60, 255))
            .setVisible(() -> colorMode.get().equalsIgnoreCase("Свой 2 цвета"));
    private final BooleanSetting voronoiTechnology = new BooleanSetting("Осколки", false);
    private final ModeSetting voronoiRenderPrio = new ModeSetting("Режим осколков", "Сбалансированное",
            "Производительность", "Сбалансированное", "Множество", "Ультра")
            .setVisible(() -> voronoiTechnology.get());

    private final ResourceLocation bubbleTexture = new ResourceLocation("harmony/images/particles/bubble.png");

    public HitBubble() {
        addSettings(colorMode, pickColor1, pickColor2, voronoiTechnology, voronoiRenderPrio);
        get = this;
    }

    @Override
    public boolean onEnable() {
        BUBBLES.clear();
        return super.onEnable();
    }

    @Override
    public boolean onDisable() {
        BUBBLES.clear();
        return super.onDisable();
    }

    @Subscribe
    public void onAttack(AttackEvent event) {
        onAttackEntity(event.entity);
    }

    @Subscribe
    public void onRender(EventRender3D event) {
        if (BUBBLES.isEmpty()) {
            return;
        }
        if (mc.player == null || mc.world == null) {
            return;
        }
        ActiveRenderInfo renderInfo = mc.getRenderManager().info;
        if (renderInfo == null) {
            return;
        }
        float moduleAlpha = getModuleAlpha();
        if (moduleAlpha < 0.05f) {
            return;
        }

        try {
            long now = System.currentTimeMillis();
            MatrixStack matrixStack = event.getStack() == null ? new MatrixStack() : event.getStack();
            Vector3d cameraPos = renderInfo.getProjectedView();
            boolean voronoiActive = isVoronoiTechnologyActive();
            int voronoiPoints = voronoiActive ? getVoronoiPointsCreateCount() : 0;
            int colorModeId = getColorModeId();
            int customColor1 = pickColor1.get();
            int customColor2 = pickColor2.get();

            RenderSystem.enableBlend();
            RenderSystem.disableCull();
            RenderSystem.disableAlphaTest();
            RenderSystem.depthMask(false);
            RenderSystem.shadeModel(GL11.GL_SMOOTH);
            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            mc.getTextureManager().bindTexture(bubbleTexture);
            RenderSystem.enableTexture();
            RenderSystem.disableDepthTest();

            Iterator<Bubble> it = BUBBLES.iterator();
            while (it.hasNext()) {
                Bubble bubble = it.next();
                float delta = bubble.getDeltaTime(now);
                if (delta >= 1.0f) {
                    it.remove();
                    continue;
                }

                drawBubble(matrixStack, bubble, moduleAlpha, cameraPos, voronoiActive, voronoiPoints,
                        colorModeId, customColor1, customColor2, now, delta);
            }
        } finally {
            restoreRenderState();
        }
    }

    private static void restoreRenderState() {
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.enableTexture();
        RenderSystem.enableAlphaTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.shadeModel(GL11.GL_FLAT);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glLineWidth(1.0F);
        GL11.glPointSize(1.0F);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glDisable(GL11.GL_POINT_SMOOTH);
    }

    private void onAttackEntity(Entity entity) {
        if (!(entity instanceof LivingEntity)) {
            return;
        }
        LivingEntity target = (LivingEntity) entity;
        if (!target.isAlive()) {
            return;
        }
        Vector3d pos = computeSurfaceHitPos(target);
        addBubble(pos);
    }

    private Vector3d computeSurfaceHitPos(LivingEntity target) {
        if (mc.player == null) {
            return target.getBoundingBox().getCenter();
        }

        Vector3d eye = mc.player.getEyePosition(1.0F);
        Vector3d look = mc.player.getLook(1.0F);
        double range = Math.max(3.0D, mc.player.getDistance(target) + 2.0D);
        Vector3d rayEnd = eye.add(look.scale(range));

        try {
            java.util.Optional<Vector3d> hit = target.getBoundingBox().rayTrace(eye, rayEnd);
            if (hit.isPresent()) {
                return hit.get().add(look.normalize().scale(0.02));
            }
        } catch (Exception ignored) {
        }

        Vector3d center = target.getBoundingBox().getCenter();
        Vector3d dir = eye.subtract(center);
        if (dir.lengthSquared() < 1.0E-4) {
            dir = new Vector3d(0.0, 0.0, 1.0);
        } else {
            dir = dir.normalize();
        }

        double hx = (target.getBoundingBox().maxX - target.getBoundingBox().minX) * 0.5;
        double hy = (target.getBoundingBox().maxY - target.getBoundingBox().minY) * 0.5;
        double hz = (target.getBoundingBox().maxZ - target.getBoundingBox().minZ) * 0.5;

        double t = Double.POSITIVE_INFINITY;
        if (Math.abs(dir.x) > 1.0E-6) {
            t = Math.min(t, hx / Math.abs(dir.x));
        }
        if (Math.abs(dir.y) > 1.0E-6) {
            t = Math.min(t, hy / Math.abs(dir.y));
        }
        if (Math.abs(dir.z) > 1.0E-6) {
            t = Math.min(t, hz / Math.abs(dir.z));
        }
        if (!Double.isFinite(t)) {
            t = Math.max(hx, Math.max(hy, hz));
        }

        Vector3d pos = center.add(dir.scale(t));
        return pos.add(dir.scale(0.02));
    }

    private void addBubble(Vector3d pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.world == null) {
            return;
        }
        ActiveRenderInfo renderInfo = mc.getRenderManager().info;
        float yaw = renderInfo != null ? renderInfo.getYaw() : 0.0f;
        float pitch = renderInfo != null ? renderInfo.getPitch() : 0.0f;
        BUBBLES.add(new Bubble(pos, yaw, pitch, getMaxTime(), System.currentTimeMillis()));
    }

    private void drawBubble(MatrixStack ms, Bubble bubble, float alphaPC, Vector3d cameraPos,
                            boolean voronoiActive, int voronoiPoints, int colorModeId,
                            int customColor1, int customColor2, long now, float delta) {
        if (delta <= 0.0f) {
            return;
        }

        float aPC = (float) easeInOutQuadWave(MathHelper.clamp((delta + 0.1f) * alphaPC, 0.0f, 1.0f)) * 2.0f;
        if (aPC > 1.0f) {
            aPC = 1.0f;
        }
        if (delta > 0.5f) {
            aPC *= aPC;
        }
        aPC *= alphaPC;
        if (aPC <= 0.01f) {
            return;
        }

        double x = bubble.pos.x - cameraPos.x;
        double y = bubble.pos.y - cameraPos.y;
        double z = bubble.pos.z - cameraPos.z;

        ms.push();
        ms.translate(x, y, z);

        if (!voronoiActive) {
            ms.translate(-bubble.pitchSin * delta / 3.0f, bubble.yawSin * delta / 2.0f, -bubble.pitchCos * delta / 3.0f);
        }

        ms.rotate(bubble.yawRotation);
        ms.rotate(bubble.pitchRotation);
        ms.scale(-0.1f, -0.1f, 0.1f);

        float rBase = 12.5f;
        float r = rBase * aPC;
        float rotation = (now % ROTATION_PERIOD_MS) / 10.0f * ROTATE_SPEED;

        if (voronoiActive) {
            UVoronoiIntegration integration = bubble.getVoronoiIntegration(voronoiPoints);
            if (integration != null) {
                ms.push();
                ms.scale(rBase, rBase, 1.0f);
                integration.setMatrix(ms.getLast().getMatrix());
                float timePCBase = delta;
                float timePC = Math.min(delta / 0.75f, 1.0f);
                timePC = (float) easeInOutExpo(0.25f + timePC);
                int fill0 = stateColor(0, 1.0f, colorModeId, customColor1, customColor2, now);
                int fill1 = stateColor(270, 1.0f, colorModeId, customColor1, customColor2, now);
                int fill2 = stateColor(540, 1.0f, colorModeId, customColor1, customColor2, now);
                int fill3 = stateColor(810, 1.0f, colorModeId, customColor1, customColor2, now);
                float voronoiAPC = Math.max(
                        1.0f - Math.min(timePCBase * 1.5f, 1.0f) - (1.0f - Math.min(timePCBase / 0.2f, 1.0f)),
                        0.0f
                );
                integration.renderBindTextureSegments(true, GL11.GL_POLYGON,
                        timePC * 0.8f + timePCBase * 0.2f,
                        60.0f * timePC + 40.0f * timePCBase,
                        voronoiAPC, fill0, fill1, fill2, fill3, 1);

                double dst = cameraPos.distanceTo(bubble.pos);
                float lineW = 0.025f + 3.5f * (float) MathHelper.clamp(1.0 - dst / 7.0, 0.0, 1.0);
                GL11.glLineWidth(lineW);
                fill0 = blendWithWhite(fill0, 0.35f);
                fill1 = blendWithWhite(fill1, 0.35f);
                fill2 = blendWithWhite(fill2, 0.35f);
                fill3 = blendWithWhite(fill3, 0.35f);
                integration.renderBindTextureSegments(true, GL11.GL_LINE_LOOP,
                        timePC * 0.8f + timePCBase * 0.2f,
                        60.0f * timePC + 40.0f * timePCBase,
                        voronoiAPC * timePC, fill0, fill1, fill2, fill3, 2);
                GL11.glLineWidth(1.0f);
                ms.pop();
                ms.pop();
                return;
            }
        }

        ms.push();
        ms.rotate(Vector3f.ZP.rotationDegrees(rotation));
        Matrix4f quadMatrix = ms.getLast().getMatrix();
        drawBubbleQuad(quadMatrix, r, aPC, colorModeId, customColor1, customColor2, now);
        ms.pop();

        ms.pop();
    }

    private void drawBubbleQuad(Matrix4f matrix, float size, float alphaPC,
                                int colorModeId, int customColor1, int customColor2, long now) {
        float half = size / 2.0f;
        int c1 = stateColor(0, alphaPC, colorModeId, customColor1, customColor2, now);
        int c2 = stateColor(90, alphaPC, colorModeId, customColor1, customColor2, now);
        int c3 = stateColor(180, alphaPC, colorModeId, customColor1, customColor2, now);
        int c4 = stateColor(270, alphaPC, colorModeId, customColor1, customColor2, now);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        putVertex(buffer, matrix, -half, -half, 0.0f, 0.0f, 0.0f, c1);
        putVertex(buffer, matrix, -half, half, 0.0f, 0.0f, 1.0f, c2);
        putVertex(buffer, matrix, half, half, 0.0f, 1.0f, 1.0f, c3);
        putVertex(buffer, matrix, half, -half, 0.0f, 1.0f, 0.0f, c4);
        tessellator.draw();
    }

    private int stateColor(int index, float alphaPC, int colorModeId, int customColor1, int customColor2, long now) {
        int color;
        switch (colorModeId) {
            case COLOR_MODE_RAINBOW:
                color = ColorUtils.rainbow(8, index, 0.85f, 1.0f, 1.0f);
                break;
            case COLOR_MODE_CUSTOM:
                color = customColor1;
                break;
            case COLOR_MODE_CUSTOM_TWO:
                float t = (float) ((Math.sin((now / 300.0) + index * 0.05) + 1.0) * 0.5);
                color = ColorUtils.interpolateColor(customColor1, customColor2, t);
                break;
            case COLOR_MODE_CLIENT:
            default:
                color = Theme.MainColor(index);
                break;
        }
        int alpha = (int) (ColorUtils.getAlpha(color) * alphaPC);
        return ColorUtils.setAlpha(color, MathHelper.clamp(alpha, 0, 255));
    }

    private int getColorModeId() {
        String mode = colorMode.get();
        String[] modes = colorMode.strings;
        for (int i = 0; i < modes.length; i++) {
            if (modes[i].equalsIgnoreCase(mode)) {
                if (i == 0) {
                    return COLOR_MODE_RAINBOW;
                }
                if (i == 2) {
                    return COLOR_MODE_CUSTOM;
                }
                if (i == 3) {
                    return COLOR_MODE_CUSTOM_TWO;
                }
                return COLOR_MODE_CLIENT;
            }
        }
        return COLOR_MODE_CLIENT;
    }

    private int stateColor(int index, float alphaPC) {
        int color;
        if (colorMode.is("Радужный")) {
            color = ColorUtils.rainbow(8, index, 0.85f, 1.0f, 1.0f);
        } else if (colorMode.is("Свой")) {
            color = pickColor1.get();
        } else if (colorMode.is("Свой 2 цвета")) {
            float t = (float) ((Math.sin((System.currentTimeMillis() / 300.0) + index * 0.05) + 1.0) * 0.5);
            color = ColorUtils.interpolateColor(pickColor1.get(), pickColor2.get(), t);
        } else {
            color = Theme.MainColor(index);
        }
        int alpha = (int) (ColorUtils.getAlpha(color) * alphaPC);
        return ColorUtils.setAlpha(color, MathHelper.clamp(alpha, 0, 255));
    }

    private int blendWithWhite(int color, float amount) {
        int white = ColorUtils.setAlpha(0xFFFFFFFF, ColorUtils.getAlpha(color));
        return ColorUtils.interpolateColor(color, white, amount);
    }

    private boolean isVoronoiTechnologyActive() {
        return voronoiTechnology.get();
    }

    private int getVoronoiPointsCreateCount() {
        if (voronoiRenderPrio.is("Производительность")) {
            return 4;
        }
        if (voronoiRenderPrio.is("Сбалансированное")) {
            return 16;
        }
        if (voronoiRenderPrio.is("Множество")) {
            return 45;
        }
        if (voronoiRenderPrio.is("Ультра")) {
            return 170;
        }
        return 2;
    }

    private float getModuleAlpha() {
        getAnimation().update();
        double value = getAnimation().getValue();
        if (Double.isNaN(value)) {
            return 1.0f;
        }
        return (float) MathHelper.clamp(value, 0.0, 1.0);
    }

    private static float getMaxTime() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return 700.0f;
        }
        try {
            HitAura hitAura = Harmony.getInstance().getModuleManager().getHitAura();
            if (hitAura != null && hitAura.isState()) {
                return 3000.0f;
            }
        } catch (Exception ignored) {
        }
        float cooled = mc.player.getCooledAttackStrength(1.0f);
        return Math.max(cooled * cooled * 2200.0f, 700.0f);
    }

    private static void putVertex(BufferBuilder buffer, Matrix4f matrix,
                                  float x, float y, float z,
                                  float u, float v, int color) {
        buffer.pos(matrix, x, y, z)
                .tex(u, v)
                .color(ColorUtils.getRed(color), ColorUtils.getGreen(color), ColorUtils.getBlue(color), ColorUtils.getAlpha(color))
                .endVertex();
    }

    private static double easeInOutQuadWave(double t) {
        t = MathHelper.clamp((float) t, 0.0f, 1.0f);
        double s = Math.sin(Math.PI * t);
        return s * s;
    }

    private static double easeInOutExpo(double t) {
        t = MathHelper.clamp((float) t, 0.0f, 1.0f);
        if (t == 0.0) {
            return 0.0;
        }
        if (t == 1.0) {
            return 1.0;
        }
        return t < 0.5 ? Math.pow(2.0, 20.0 * t - 10.0) / 2.0 : (2.0 - Math.pow(2.0, -20.0 * t + 10.0)) / 2.0;
    }

    private static class Bubble {
        private final Vector3d pos;
        private final float yawSin;
        private final float pitchSin;
        private final float pitchCos;
        private final Quaternion yawRotation;
        private final Quaternion pitchRotation;
        private final long startTime;
        private final float maxTime;
        private UVoronoiIntegration voronoi;
        private int voronoiPoints;

        private Bubble(Vector3d pos, float viewYaw, float viewPitch, float maxTime, long startTime) {
            this.pos = pos;
            this.maxTime = maxTime;
            this.startTime = startTime;
            float yawRad = (float) Math.toRadians(viewYaw);
            float pitchRad = (float) Math.toRadians(viewPitch);
            this.yawSin = MathHelper.sin(yawRad);
            this.pitchSin = MathHelper.sin(pitchRad);
            this.pitchCos = MathHelper.cos(pitchRad);
            this.yawRotation = Vector3f.YP.rotationDegrees(-viewYaw);
            this.pitchRotation = Vector3f.XP.rotationDegrees(viewPitch);
        }

        private float getDeltaTime(long now) {
            return (now - startTime) / maxTime;
        }

        private UVoronoiIntegration getVoronoiIntegration(int points) {
            if (voronoi == null || voronoiPoints != points) {
                voronoi = UVoronoiIntegration.generateDefault(points, true);
                voronoiPoints = points;
            }
            return voronoi;
        }
    }

}
