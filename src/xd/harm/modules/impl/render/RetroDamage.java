package xd.harm.modules.impl.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.potion.Effects;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import xd.harm.events.combat.AttackEvent;
import xd.harm.events.render.EventRender3D;
import xd.harm.events.world.EventChangeWorld;
import xd.harm.events.world.TickEvent;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.ColorSetting;
import xd.harm.modules.settings.impl.SliderSetting;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

@ModuleRegister(name = "RetroDamage", category = Category.Render, desc = "Ретро показ урона")
public class RetroDamage extends Module {

    private static final long HIT_TRACK_TIMEOUT_MS = 1500L;
    private static final float MIN_DAMAGE = 0.05F;
    private static final float TWO_PI = (float) Math.PI * 2.0F;
    private static final float DIGIT_CHAR_WIDTH = 5.1F;
    private static final float DIGIT_CHAR_HEIGHT = 7.0F;
    private static final float DIGIT_GAP = 1.15F;
    private static final double STACK_THRESHOLD = 0.45D;
    private static final float SPARK_POINT_SIZE = 2.8F;
    private static final float SHADOW_ALPHA = 0.62F;
    private static final int UNSET_UNIFORM_INT = Integer.MIN_VALUE;

    private final SliderSetting lifeTime = new SliderSetting("Время Жизни", 1.20F, 0.60F, 2.50F, 0.05F);
    private final SliderSetting rise = new SliderSetting("Подъём", 0.95F, 0.30F, 2.00F, 0.05F);
    private final SliderSetting size = new SliderSetting("Размер", 0.052F, 0.025F, 0.120F, 0.002F);
    private final SliderSetting spread = new SliderSetting("Разброс", 0.24F, 0.00F, 0.80F, 0.01F);;
    private final BooleanSetting themeColor = new BooleanSetting("Тема Клиента", false);
    private final BooleanSetting sparks = new BooleanSetting("Искры", true);
    private final ColorSetting color = new ColorSetting("Цвет", new Color(255, 194, 64).getRGB());

    private final Map<Integer, DamageTrack> trackedTargets = new HashMap<>();
    private final List<Popup> popups = new ArrayList<>();
    private final List<Spark> sparkParticles = new ArrayList<>();
    private final Random random = new Random();

    private int shaderProgram = -1;
    private int vertexShader = -1;
    private int fragmentShader = -1;
    private int timeUniform = -1;
    private int digitUniform = -1;
    private int alphaUniform = -1;
    private int colorTopUniform = -1;
    private int colorBottomUniform = -1;
    private boolean shaderFailed = false;
    private float cachedTimeUniform = Float.NaN;
    private int cachedDigitUniform = UNSET_UNIFORM_INT;
    private float cachedAlphaUniform = Float.NaN;
    private float cachedColorTopR = Float.NaN;
    private float cachedColorTopG = Float.NaN;
    private float cachedColorTopB = Float.NaN;
    private float cachedColorBottomR = Float.NaN;
    private float cachedColorBottomG = Float.NaN;
    private float cachedColorBottomB = Float.NaN;

    private static final String VERTEX_SHADER =
            "#version 120\n" +
                    "varying vec2 v_uv;\n" +
                    "void main() {\n" +
                    "    v_uv = gl_MultiTexCoord0.xy;\n" +
                    "    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;\n" +
                    "}\n";

    private static final String FRAGMENT_SHADER =
            "#version 120\n" +
                    "varying vec2 v_uv;\n" +
                    "uniform float u_time;\n" +
                    "uniform int u_digit;\n" +
                    "uniform float u_alpha;\n" +
                    "uniform vec3 u_colorTop;\n" +
                    "uniform vec3 u_colorBottom;\n" +
                    "\n" +
                    "float bitAt(int mask, int x) {\n" +
                    "    float v = floor(float(mask) / pow(2.0, float(4 - x)));\n" +
                    "    return mod(v, 2.0);\n" +
                    "}\n" +
                    "\n" +
                    "int rowMask(int d, int y) {\n" +
                    "    if (d == 0) {\n" +
                    "        if (y == 6) return 14;\n" +
                    "        if (y == 5) return 17;\n" +
                    "        if (y == 4) return 19;\n" +
                    "        if (y == 3) return 21;\n" +
                    "        if (y == 2) return 25;\n" +
                    "        if (y == 1) return 17;\n" +
                    "        if (y == 0) return 14;\n" +
                    "    }\n" +
                    "    if (d == 1) {\n" +
                    "        if (y == 6) return 4;\n" +
                    "        if (y == 5) return 12;\n" +
                    "        if (y == 4) return 4;\n" +
                    "        if (y == 3) return 4;\n" +
                    "        if (y == 2) return 4;\n" +
                    "        if (y == 1) return 4;\n" +
                    "        if (y == 0) return 14;\n" +
                    "    }\n" +
                    "    if (d == 2) {\n" +
                    "        if (y == 6) return 14;\n" +
                    "        if (y == 5) return 17;\n" +
                    "        if (y == 4) return 1;\n" +
                    "        if (y == 3) return 2;\n" +
                    "        if (y == 2) return 4;\n" +
                    "        if (y == 1) return 8;\n" +
                    "        if (y == 0) return 31;\n" +
                    "    }\n" +
                    "    if (d == 3) {\n" +
                    "        if (y == 6) return 30;\n" +
                    "        if (y == 5) return 1;\n" +
                    "        if (y == 4) return 1;\n" +
                    "        if (y == 3) return 14;\n" +
                    "        if (y == 2) return 1;\n" +
                    "        if (y == 1) return 1;\n" +
                    "        if (y == 0) return 30;\n" +
                    "    }\n" +
                    "    if (d == 4) {\n" +
                    "        if (y == 6) return 2;\n" +
                    "        if (y == 5) return 6;\n" +
                    "        if (y == 4) return 10;\n" +
                    "        if (y == 3) return 18;\n" +
                    "        if (y == 2) return 31;\n" +
                    "        if (y == 1) return 2;\n" +
                    "        if (y == 0) return 2;\n" +
                    "    }\n" +
                    "    if (d == 5) {\n" +
                    "        if (y == 6) return 31;\n" +
                    "        if (y == 5) return 16;\n" +
                    "        if (y == 4) return 16;\n" +
                    "        if (y == 3) return 30;\n" +
                    "        if (y == 2) return 1;\n" +
                    "        if (y == 1) return 1;\n" +
                    "        if (y == 0) return 30;\n" +
                    "    }\n" +
                    "    if (d == 6) {\n" +
                    "        if (y == 6) return 14;\n" +
                    "        if (y == 5) return 16;\n" +
                    "        if (y == 4) return 16;\n" +
                    "        if (y == 3) return 30;\n" +
                    "        if (y == 2) return 17;\n" +
                    "        if (y == 1) return 17;\n" +
                    "        if (y == 0) return 14;\n" +
                    "    }\n" +
                    "    if (d == 7) {\n" +
                    "        if (y == 6) return 31;\n" +
                    "        if (y == 5) return 1;\n" +
                    "        if (y == 4) return 2;\n" +
                    "        if (y == 3) return 4;\n" +
                    "        if (y == 2) return 8;\n" +
                    "        if (y == 1) return 8;\n" +
                    "        if (y == 0) return 8;\n" +
                    "    }\n" +
                    "    if (d == 8) {\n" +
                    "        if (y == 6) return 14;\n" +
                    "        if (y == 5) return 17;\n" +
                    "        if (y == 4) return 17;\n" +
                    "        if (y == 3) return 14;\n" +
                    "        if (y == 2) return 17;\n" +
                    "        if (y == 1) return 17;\n" +
                    "        if (y == 0) return 14;\n" +
                    "    }\n" +
                    "    if (d == 9) {\n" +
                    "        if (y == 6) return 14;\n" +
                    "        if (y == 5) return 17;\n" +
                    "        if (y == 4) return 17;\n" +
                    "        if (y == 3) return 15;\n" +
                    "        if (y == 2) return 1;\n" +
                    "        if (y == 1) return 1;\n" +
                    "        if (y == 0) return 14;\n" +
                    "    }\n" +
                    "    return 0;\n" +
                    "}\n" +
                    "\n" +
                    "float glyph(int d, int x, int y) {\n" +
                    "    if (x < 0 || x > 4 || y < 0 || y > 6) return 0.0;\n" +
                    "    return bitAt(rowMask(d, y), x);\n" +
                    "}\n" +
                    "\n" +
                    "void main() {\n" +
                    "    vec2 uv = vec2(min(v_uv.x, 0.9999), min(v_uv.y, 0.9999));\n" +
                    "    vec2 grid = vec2(5.0, 7.0);\n" +
                    "    vec2 gp = uv * grid;\n" +
                    "    int x = int(floor(gp.x));\n" +
                    "    int y = 6 - int(floor(gp.y));\n" +
                    "\n" +
                    "    float on = glyph(u_digit, x, y);\n" +
                    "    if (on < 0.5) discard;\n" +
                    "\n" +
                    "    vec2 cell = fract(gp);\n" +
                    "    float pixelBody = step(0.08, cell.x) * step(0.08, cell.y) * step(cell.x, 0.92) * step(cell.y, 0.92);\n" +
                    "    float shimmer = 0.90 + 0.10 * sin(u_time * 10.0 + float(y) * 1.7);\n" +
                    "    vec3 base = mix(u_colorBottom, u_colorTop, uv.y);\n" +
                    "    float alpha = u_alpha * pixelBody;\n" +
                    "    if (alpha < 0.02) discard;\n" +
                    "\n" +
                    "    gl_FragColor = vec4(base * shimmer, alpha);\n" +
                    "}\n";

    public RetroDamage() {
        color.setVisible(() -> !themeColor.get());
        addSettings(lifeTime, rise, size, spread, themeColor, sparks, color);
    }

    @Override
    public boolean onEnable() {
        popups.clear();
        trackedTargets.clear();
        sparkParticles.clear();
        return super.onEnable();
    }

    @Override
    public boolean onDisable() {
        popups.clear();
        trackedTargets.clear();
        sparkParticles.clear();
        return super.onDisable();
    }

    @Subscribe
    public void onChangeWorld(EventChangeWorld e) {
        popups.clear();
        trackedTargets.clear();
        sparkParticles.clear();
    }

    @Subscribe
    public void onAttack(AttackEvent e) {
        if (mc.player == null || mc.world == null) {
            return;
        }
        if (!(e.entity instanceof LivingEntity)) {
            return;
        }

        LivingEntity target = (LivingEntity) e.entity;
        if (!target.isAlive() || target == mc.player || target.removed) {
            return;
        }

        long now = System.currentTimeMillis();

        Vector3d hitPos = null;
        if (mc.objectMouseOver != null && mc.objectMouseOver.getType() == RayTraceResult.Type.ENTITY
                && mc.objectMouseOver instanceof EntityRayTraceResult) {
            hitPos = mc.objectMouseOver.getHitVec();
        }
        if (hitPos == null) {
            hitPos = new Vector3d(target.getPosX(), target.getPosY() + target.getHeight() * 0.5D, target.getPosZ());
        }

        int entityId = target.getEntityId();
        DamageTrack existing = trackedTargets.get(entityId);
        if (existing == null) {
            trackedTargets.put(entityId, new DamageTrack(combinedHealth(target), now, resolveStyle(target), hitPos));
        } else {
            existing.time = now;
            existing.style = resolveStyle(target);
            existing.hitPos = hitPos;
        }
    }

    @Subscribe
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) {
            popups.clear();
            trackedTargets.clear();
            sparkParticles.clear();
            return;
        }

        long now = System.currentTimeMillis();
        Iterator<Map.Entry<Integer, DamageTrack>> iterator = trackedTargets.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, DamageTrack> entry = iterator.next();
            DamageTrack track = entry.getValue();

            Entity entity = mc.world.getEntityByID(entry.getKey());
            if (!(entity instanceof LivingEntity)) {
                spawnRemovedTargetDamage(track);
                iterator.remove();
                continue;
            }

            LivingEntity target = (LivingEntity) entity;
            if (target.removed) {
                spawnRemovedTargetDamage(track);
                iterator.remove();
                continue;
            }

            float currentHp = combinedHealth(target);
            float damage = track.beforeHp - currentHp;
            if (damage > MIN_DAMAGE) {
                spawnDamagePopup(target, damage, track.style, track.hitPos);
                iterator.remove();
                continue;
            }

            if (!target.isAlive()) {
                iterator.remove();
                continue;
            }

            if (currentHp > track.beforeHp) {
                track.beforeHp = currentHp;
            }

            if (now - track.time > HIT_TRACK_TIMEOUT_MS) {
                iterator.remove();
            }
        }
    }

    @Subscribe
    public void onRender(EventRender3D event) {
        if (mc.player == null || mc.world == null) {
            popups.clear();
            sparkParticles.clear();
            return;
        }

        ActiveRenderInfo renderInfo = mc.getRenderManager().info;
        if (renderInfo == null) {
            popups.clear();
            sparkParticles.clear();
            return;
        }

        if (popups.isEmpty() && sparkParticles.isEmpty()) {
            return;
        }

        MatrixStack ms = event.getMatrixStack() == null ? new MatrixStack() : event.getMatrixStack();
        long now = System.currentTimeMillis();

        Vector3d cameraPos = renderInfo.getProjectedView();
        float cameraYaw = renderInfo.getYaw();
        float cameraPitch = renderInfo.getPitch();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);

        try {
            if (!sparkParticles.isEmpty()) {
                RenderSystem.disableAlphaTest();
                RenderSystem.disableTexture();
                RenderSystem.enableDepthTest();
                renderSparks(ms, cameraPos, now);
            }

            if (!popups.isEmpty()) {
                float life = Math.max(0.20F, lifeTime.get());
                float riseValue = rise.get();
                float sizeValue = size.get();
                boolean useThemeColor = themeColor.get();
                int configuredColor = useThemeColor ? 0 : color.get();

                RenderSystem.enableTexture();
                RenderSystem.enableAlphaTest();
                RenderSystem.disableDepthTest();
                renderFallbackPopups(ms, cameraPos, cameraYaw, cameraPitch, now, life,
                        riseValue, sizeValue, useThemeColor, configuredColor, mc.player, mc.fontRenderer);
            }
        } finally {
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableTexture();
            RenderSystem.enableAlphaTest();
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private void renderSparks(MatrixStack ms, Vector3d cameraPos, long now) {
        if (sparkParticles.isEmpty()) {
            return;
        }

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        Matrix4f matrix = ms.getLast().getMatrix();
        GL11.glPointSize(SPARK_POINT_SIZE);
        try {
            buffer.begin(GL11.GL_POINTS, DefaultVertexFormats.POSITION_COLOR);

            int writeIndex = 0;
            int particleCount = sparkParticles.size();
            for (int readIndex = 0; readIndex < particleCount; readIndex++) {
                Spark spark = sparkParticles.get(readIndex);
                float t = (now - spark.startTime) / 1000.0F / spark.life;
                if (t >= 1.0F) {
                    continue;
                }

                if (writeIndex != readIndex) {
                    sparkParticles.set(writeIndex, spark);
                }
                writeIndex++;

                double gravity = -0.22D * t * t;
                double px = spark.origin.x + spark.velocityX * t;
                double py = spark.origin.y + spark.velocityY * t + gravity;
                double pz = spark.origin.z + spark.velocityZ * t;

                double rx = px - cameraPos.x;
                double ry = py - cameraPos.y;
                double rz = pz - cameraPos.z;

                float fade = (float) Math.pow(1.0F - t, 1.45F);
                int a = MathHelper.clamp((int) (255 * fade), 0, 255);
                buffer.pos(matrix, (float) rx, (float) ry, (float) rz).color(spark.red, spark.green, spark.blue, a).endVertex();
            }

            trimList(sparkParticles, writeIndex);
            tessellator.draw();
        } finally {
            GL11.glPointSize(1.0F);
        }
    }

    private void renderFallbackPopups(MatrixStack ms, Vector3d cameraPos, float cameraYaw, float cameraPitch,
                                      long now, float life, float riseValue, float sizeValue,
                                      boolean useThemeColor, int configuredColor,
                                      Entity player, FontRenderer fontRenderer) {
        int writeIndex = 0;
        int popupCount = popups.size();
        for (int readIndex = 0; readIndex < popupCount; readIndex++) {
            Popup popup = popups.get(readIndex);
            float t = (now - popup.startTime) / 1000.0F / life;
            if (t >= 1.0F) {
                continue;
            }

            float alpha = popupAlpha(t);
            if (alpha <= 0.01F) {
                continue;
            }

            if (writeIndex != readIndex) {
                popups.set(writeIndex, popup);
            }
            writeIndex++;

            float sideDrift = popup.sideSpeed * (t - 0.35F * t * t);
            float riseAmount = riseValue * easeOutCubic(t) + popup.extraRise * (1.0F - t) * 0.35F;

            Vector3d worldPos = popup.anchor.add(popup.sideX * sideDrift, riseAmount, popup.sideZ * sideDrift);
            double x = worldPos.x - cameraPos.x;
            double y = worldPos.y - cameraPos.y;
            double z = worldPos.z - cameraPos.z;

            ms.push();
            ms.translate(x, y, z);
            ms.rotate(Vector3f.YP.rotationDegrees(-cameraYaw));
            ms.rotate(Vector3f.XP.rotationDegrees(cameraPitch));

            float distance = (float) Math.sqrt(player.getDistanceSq(worldPos.x, worldPos.y, worldPos.z));
            float scaleValue = sizeValue * (1.0F + MathHelper.clamp(distance * 0.02F, 0.0F, 0.42F)) * bounceScale(t);
            ms.scale(-scaleValue, -scaleValue, scaleValue);

            float textX = -fontRenderer.getStringWidth(popup.text) / 2.0F;
            int a = MathHelper.clamp((int) (alpha * 255.0F), 0, 255);
            int rgb = fallbackTextRgb(popup.style, configuredColor, useThemeColor, popup.phase);
            fontRenderer.drawStringWithShadow(ms, popup.text, textX, -4.0F, (a << 24) | rgb);

            ms.pop();
        }

        trimList(popups, writeIndex);
    }

    private <T> void trimList(List<T> list, int size) {
        for (int i = list.size() - 1; i >= size; i--) {
            list.remove(i);
        }
    }

    private int fallbackTextRgb(HitStyle style, int configuredColor, boolean useThemeColor, float phase) {
        if (style == HitStyle.STRENGTH) {
            return 0xE6BAFF;
        }

        if (style == HitStyle.CRIT) {
            float pulse = 0.84F + 0.16F * (float) Math.sin(phase * 2.1F);
            return multiplyColor(0xFFE77A, pulse);
        }

        int baseColor = useThemeColor ? Theme.MainColor((int) (phase * 35.0F)) : configuredColor;
        return brighten(baseColor, useThemeColor ? 1.33F : 1.28F);
    }

    private void spawnDamagePopup(LivingEntity target, float rawDamage, HitStyle style, Vector3d hitPos) {
        int value = Math.max(1, Math.round(rawDamage));
        spawnPopup(target, value, style, hitPos);
        spawnSparks(target, style);
    }

    private void spawnRemovedTargetDamage(DamageTrack track) {
        if (System.currentTimeMillis() - track.time > HIT_TRACK_TIMEOUT_MS || track.beforeHp <= MIN_DAMAGE || track.hitPos == null) {
            return;
        }

        spawnPopupAt(Math.max(1, Math.round(track.beforeHp)), track.style, track.hitPos);
    }

    private void spawnPopup(LivingEntity target, int value, HitStyle style, Vector3d hitPos) {
        float spreadValue = spread.get();
        double xJitter = (random.nextDouble() - 0.5D) * spreadValue;
        double zJitter = (random.nextDouble() - 0.5D) * spreadValue;

        Vector3d anchor;
        if (hitPos != null) {
            anchor = hitPos.add(xJitter, 0.0D, zJitter);
        } else {
            float pt = mc.getRenderPartialTicks();
            double ix = lerp(target.lastTickPosX, target.getPosX(), pt);
            double iy = lerp(target.lastTickPosY, target.getPosY(), pt);
            double iz = lerp(target.lastTickPosZ, target.getPosZ(), pt);
            anchor = new Vector3d(ix + xJitter, iy + target.getHeight() * 0.5D, iz + zJitter);
        }

        addPopup(value, style, anchor);
    }

    private void spawnPopupAt(int value, HitStyle style, Vector3d pos) {
        float spreadValue = spread.get();
        double xJitter = (random.nextDouble() - 0.5D) * spreadValue;
        double zJitter = (random.nextDouble() - 0.5D) * spreadValue;
        addPopup(value, style, pos.add(xJitter, 0.0D, zJitter));
    }

    private void addPopup(int value, HitStyle style, Vector3d anchor) {
        boolean pushed = true;
        int maxIter = 20;
        while (pushed && maxIter-- > 0) {
            pushed = false;
            for (Popup existing : popups) {
                double dx = anchor.x - existing.anchor.x;
                double dz = anchor.z - existing.anchor.z;
                double horizontal = dx * dx + dz * dz;
                if (horizontal < 1.2D) {
                    double dy = anchor.y - existing.anchor.y;
                    if (dy >= 0.0D && dy < STACK_THRESHOLD) {
                        anchor = new Vector3d(anchor.x, existing.anchor.y + STACK_THRESHOLD, anchor.z);
                        pushed = true;
                    }
                }
            }
        }

        float angle = random.nextFloat() * TWO_PI;
        float sideX = MathHelper.cos(angle);
        float sideZ = MathHelper.sin(angle);
        float sideSpeed = 0.12F + random.nextFloat() * 0.20F;
        float phase = random.nextFloat() * TWO_PI;
        float extraRise = random.nextFloat() * 0.22F;

        popups.add(new Popup(value, style, anchor, sideX, sideZ, sideSpeed,
                phase, extraRise, System.currentTimeMillis()));
    }

    private void spawnSparks(LivingEntity target, HitStyle style) {
        if (!sparks.get()) {
            return;
        }

        int count = style == HitStyle.CRIT ? 12 : 8;
        int sparkColor;
        if (style == HitStyle.CRIT) {
            sparkColor = 0xFF5E39;
        } else if (style == HitStyle.STRENGTH) {
            sparkColor = 0xAE5CFF;
        } else {
            sparkColor = 0xFFD95A;
        }

        Vector3d origin = new Vector3d(target.getPosX(), target.getPosY() + target.getHeight() + 0.35D, target.getPosZ());
        long now = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            double vx = (random.nextDouble() - 0.5D) * 0.36D;
            double vy = 0.25D + random.nextDouble() * 0.45D;
            double vz = (random.nextDouble() - 0.5D) * 0.36D;
            float life = 0.40F + random.nextFloat() * 0.45F;
            sparkParticles.add(new Spark(origin, vx, vy, vz, sparkColor, life, now));
        }
    }

    private void drawValue(MatrixStack ms, int value, Palette palette, float alpha) {
        String text = Integer.toString(value);
        float totalWidth = text.length() * DIGIT_CHAR_WIDTH + Math.max(0, text.length() - 1) * DIGIT_GAP;
        float startX = -totalWidth / 2.0F;
        float baseY = -DIGIT_CHAR_HEIGHT / 2.0F;
        Matrix4f matrix = ms.getLast().getMatrix();

        setColorTopUniform(0.0F, 0.0F, 0.0F);
        setColorBottomUniform(0.0F, 0.0F, 0.0F);
        setAlphaUniform(alpha * SHADOW_ALPHA);
        float shadowX = startX + 0.75F;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= '0' && c <= '9') {
                setDigitUniform(c - '0');
                drawDigitQuad(matrix, shadowX, baseY + 0.75F, DIGIT_CHAR_WIDTH, DIGIT_CHAR_HEIGHT);
            }
            shadowX += DIGIT_CHAR_WIDTH + DIGIT_GAP;
        }

        setColorTopUniform(palette.topR, palette.topG, palette.topB);
        setColorBottomUniform(palette.bottomR, palette.bottomG, palette.bottomB);
        setAlphaUniform(alpha);
        float x = startX;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= '0' && c <= '9') {
                setDigitUniform(c - '0');
                drawDigitQuad(matrix, x, baseY, DIGIT_CHAR_WIDTH, DIGIT_CHAR_HEIGHT);
            }
            x += DIGIT_CHAR_WIDTH + DIGIT_GAP;
        }
    }

    private void drawDigitQuad(Matrix4f matrix, float x, float y, float width, float height) {
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(matrix, x, y, 0.0F).tex(0.0F, 0.0F).endVertex();
        buffer.pos(matrix, x, y + height, 0.0F).tex(0.0F, 1.0F).endVertex();
        buffer.pos(matrix, x + width, y + height, 0.0F).tex(1.0F, 1.0F).endVertex();
        buffer.pos(matrix, x + width, y, 0.0F).tex(1.0F, 0.0F).endVertex();
        Tessellator.getInstance().draw();
    }

    private void setTimeUniform(float time) {
        if (timeUniform == -1 || Float.compare(cachedTimeUniform, time) == 0) {
            return;
        }

        GL20.glUniform1f(timeUniform, time);
        cachedTimeUniform = time;
    }

    private void setDigitUniform(int digit) {
        if (digitUniform == -1 || cachedDigitUniform == digit) {
            return;
        }

        GL20.glUniform1i(digitUniform, digit);
        cachedDigitUniform = digit;
    }

    private void setAlphaUniform(float alpha) {
        if (alphaUniform == -1 || Float.compare(cachedAlphaUniform, alpha) == 0) {
            return;
        }

        GL20.glUniform1f(alphaUniform, alpha);
        cachedAlphaUniform = alpha;
    }

    private void setColorTopUniform(float r, float g, float b) {
        if (colorTopUniform == -1
                || (Float.compare(cachedColorTopR, r) == 0
                && Float.compare(cachedColorTopG, g) == 0
                && Float.compare(cachedColorTopB, b) == 0)) {
            return;
        }

        GL20.glUniform3f(colorTopUniform, r, g, b);
        cachedColorTopR = r;
        cachedColorTopG = g;
        cachedColorTopB = b;
    }

    private void setColorBottomUniform(float r, float g, float b) {
        if (colorBottomUniform == -1
                || (Float.compare(cachedColorBottomR, r) == 0
                && Float.compare(cachedColorBottomG, g) == 0
                && Float.compare(cachedColorBottomB, b) == 0)) {
            return;
        }

        GL20.glUniform3f(colorBottomUniform, r, g, b);
        cachedColorBottomR = r;
        cachedColorBottomG = g;
        cachedColorBottomB = b;
    }

    private void resetShaderUniformCache() {
        cachedTimeUniform = Float.NaN;
        cachedDigitUniform = UNSET_UNIFORM_INT;
        cachedAlphaUniform = Float.NaN;
        cachedColorTopR = Float.NaN;
        cachedColorTopG = Float.NaN;
        cachedColorTopB = Float.NaN;
        cachedColorBottomR = Float.NaN;
        cachedColorBottomG = Float.NaN;
        cachedColorBottomB = Float.NaN;
    }

    private void initShader() {
        if (shaderProgram != -1 || shaderFailed) {
            return;
        }

        vertexShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vertexShader, VERTEX_SHADER);
        GL20.glCompileShader(vertexShader);
        if (GL20.glGetShaderi(vertexShader, GL20.GL_COMPILE_STATUS) == 0) {
            print("DamagePopupRetro: vertex shader compile failed.");
            shaderFailed = true;
            cleanupShader();
            return;
        }

        fragmentShader = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(fragmentShader, FRAGMENT_SHADER);
        GL20.glCompileShader(fragmentShader);
        if (GL20.glGetShaderi(fragmentShader, GL20.GL_COMPILE_STATUS) == 0) {
            print("DamagePopupRetro: fragment shader compile failed.");
            shaderFailed = true;
            cleanupShader();
            return;
        }

        shaderProgram = GL20.glCreateProgram();
        GL20.glAttachShader(shaderProgram, vertexShader);
        GL20.glAttachShader(shaderProgram, fragmentShader);
        GL20.glLinkProgram(shaderProgram);
        if (GL20.glGetProgrami(shaderProgram, GL20.GL_LINK_STATUS) == 0) {
            print("DamagePopupRetro: shader link failed.");
            shaderFailed = true;
            cleanupShader();
            return;
        }

        shaderFailed = false;
        timeUniform = GL20.glGetUniformLocation(shaderProgram, "u_time");
        digitUniform = GL20.glGetUniformLocation(shaderProgram, "u_digit");
        alphaUniform = GL20.glGetUniformLocation(shaderProgram, "u_alpha");
        colorTopUniform = GL20.glGetUniformLocation(shaderProgram, "u_colorTop");
        colorBottomUniform = GL20.glGetUniformLocation(shaderProgram, "u_colorBottom");
        resetShaderUniformCache();
    }

    private void cleanupShader() {
        if (shaderProgram != -1) {
            GL20.glDeleteProgram(shaderProgram);
            shaderProgram = -1;
        }
        if (vertexShader != -1) {
            GL20.glDeleteShader(vertexShader);
            vertexShader = -1;
        }
        if (fragmentShader != -1) {
            GL20.glDeleteShader(fragmentShader);
            fragmentShader = -1;
        }

        timeUniform = -1;
        digitUniform = -1;
        alphaUniform = -1;
        colorTopUniform = -1;
        colorBottomUniform = -1;
        resetShaderUniformCache();
    }

    private float combinedHealth(LivingEntity entity) {
        return entity.getHealth() + entity.getAbsorptionAmount();
    }

    private HitStyle resolveStyle(LivingEntity target) {
        if (mc.player == null) {
            return HitStyle.NORMAL;
        }

        boolean critReady = mc.player.getCooledAttackStrength(0.5F) > 0.9F;
        boolean crit = critReady
                && mc.player.fallDistance > 0.0F
                && !mc.player.isOnGround()
                && !mc.player.isOnLadder()
                && !mc.player.isInWater()
                && !mc.player.isPotionActive(Effects.BLINDNESS)
                && !mc.player.isPassenger()
                && !mc.player.isSprinting()
                && target.isAlive();

        if (crit) {
            return HitStyle.CRIT;
        }

        if (mc.player.isPotionActive(Effects.STRENGTH)) {
            return HitStyle.STRENGTH;
        }

        return HitStyle.NORMAL;
    }

    private Palette paletteFor(HitStyle style, int baseColor, float time, float phase) {
        if (style == HitStyle.STRENGTH) {
            return Palette.ofRgb(0xE6BAFF, 0x8E3CFF);
        }

        if (style == HitStyle.CRIT) {
            float pulse = 0.84F + 0.16F * (float) Math.sin(time * 14.0F + phase * 2.1F);
            int top = multiplyColor(0xFFE77A, pulse);
            int bottom = multiplyColor(0xFF3F2B, pulse);
            return Palette.ofRgb(top, bottom);
        }

        if (themeColor.get()) {
            int c = Theme.MainColor((int) (time * 200.0F + phase * 35.0F));
            int top = brighten(c, 1.33F);
            int bottom = multiplyColor(c, 0.82F);
            return Palette.ofRgb(top, bottom);
        }

        int top = brighten(baseColor, 1.28F);
        int bottom = multiplyColor(baseColor, 0.82F);
        return Palette.ofRgb(top, bottom);
    }

    private int multiplyColor(int rgb, float mul) {
        int r = MathHelper.clamp((int) (((rgb >> 16) & 0xFF) * mul), 0, 255);
        int g = MathHelper.clamp((int) (((rgb >> 8) & 0xFF) * mul), 0, 255);
        int b = MathHelper.clamp((int) ((rgb & 0xFF) * mul), 0, 255);
        return (r << 16) | (g << 8) | b;
    }

    private int brighten(int rgb, float mul) {
        int r = MathHelper.clamp((int) (((rgb >> 16) & 0xFF) * mul), 0, 255);
        int g = MathHelper.clamp((int) (((rgb >> 8) & 0xFF) * mul), 0, 255);
        int b = MathHelper.clamp((int) ((rgb & 0xFF) * mul), 0, 255);
        return (r << 16) | (g << 8) | b;
    }

    private float popupAlpha(float t) {
        float in = MathHelper.clamp(t / 0.08F, 0.0F, 1.0F);
        float out = 1.0F - MathHelper.clamp((t - 0.62F) / 0.38F, 0.0F, 1.0F);
        return in * out;
    }

    private float bounceScale(float t) {
        float p = MathHelper.clamp(t / 0.22F, 0.0F, 1.0F);
        if (p < 0.6F) {
            float k = p / 0.6F;
            return 0.50F + (1.20F - 0.50F) * easeOutCubic(k);
        }
        float k = (p - 0.6F) / 0.4F;
        return 1.20F + (1.00F - 1.20F) * k;
    }

    private double lerp(double prev, double cur, float partialTicks) {
        return prev + (cur - prev) * partialTicks;
    }

    private float easeOutCubic(float x) {
        float t = 1.0F - MathHelper.clamp(x, 0.0F, 1.0F);
        return 1.0F - t * t * t;
    }

    private enum HitStyle {
        NORMAL,
        CRIT,
        STRENGTH
    }

    private static class Palette {
        private final float topR;
        private final float topG;
        private final float topB;
        private final float bottomR;
        private final float bottomG;
        private final float bottomB;

        private Palette(float topR, float topG, float topB, float bottomR, float bottomG, float bottomB) {
            this.topR = topR;
            this.topG = topG;
            this.topB = topB;
            this.bottomR = bottomR;
            this.bottomG = bottomG;
            this.bottomB = bottomB;
        }

        private static Palette ofRgb(int top, int bottom) {
            return new Palette(
                    ((top >> 16) & 0xFF) / 255.0F,
                    ((top >> 8) & 0xFF) / 255.0F,
                    (top & 0xFF) / 255.0F,
                    ((bottom >> 16) & 0xFF) / 255.0F,
                    ((bottom >> 8) & 0xFF) / 255.0F,
                    (bottom & 0xFF) / 255.0F);
        }

        private int toRgb() {
            int r = MathHelper.clamp((int) (topR * 255.0F), 0, 255);
            int g = MathHelper.clamp((int) (topG * 255.0F), 0, 255);
            int b = MathHelper.clamp((int) (topB * 255.0F), 0, 255);
            return (r << 16) | (g << 8) | b;
        }
    }

    private static class DamageTrack {
        private float beforeHp;
        private long time;
        private HitStyle style;
        private Vector3d hitPos;

        private DamageTrack(float beforeHp, long time, HitStyle style, Vector3d hitPos) {
            this.beforeHp = beforeHp;
            this.time = time;
            this.style = style;
            this.hitPos = hitPos;
        }
    }

    private static class Popup {
        private final String text;
        private final HitStyle style;
        private final Vector3d anchor;
        private final float sideX;
        private final float sideZ;
        private final float sideSpeed;
        private final float phase;
        private final float extraRise;
        private final long startTime;

        private Popup(int value, HitStyle style, Vector3d anchor,
                      float sideX, float sideZ, float sideSpeed, float phase, float extraRise, long startTime) {
            this.text = Integer.toString(value);
            this.style = style;
            this.anchor = anchor;
            this.sideX = sideX;
            this.sideZ = sideZ;
            this.sideSpeed = sideSpeed;
            this.phase = phase;
            this.extraRise = extraRise;
            this.startTime = startTime;
        }
    }

    private static class Spark {
        private final Vector3d origin;
        private final double velocityX;
        private final double velocityY;
        private final double velocityZ;
        private final int red;
        private final int green;
        private final int blue;
        private final float life;
        private final long startTime;

        private Spark(Vector3d origin, double velocityX, double velocityY, double velocityZ, int color, float life, long startTime) {
            this.origin = origin;
            this.velocityX = velocityX;
            this.velocityY = velocityY;
            this.velocityZ = velocityZ;
            this.red = (color >> 16) & 0xFF;
            this.green = (color >> 8) & 0xFF;
            this.blue = color & 0xFF;
            this.life = life;
            this.startTime = startTime;
        }
    }
}
