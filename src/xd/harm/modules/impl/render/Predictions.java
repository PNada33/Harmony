package xd.harm.modules.impl.render;

import com.google.common.eventbus.Subscribe;
import net.minecraft.client.MainWindow;
import xd.harm.events.render.EventDisplay;
import xd.harm.events.render.WorldEvent;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.Setting;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.ModeSetting;
import xd.harm.utils.render.color.ColorUtils;
import xd.harm.utils.render.font.Fonts;
import xd.harm.utils.render.rect.RenderUtility;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.EnderPearlEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3i;

import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.util.*;
import xd.harm.events.world.EventUpdate;

@ModuleRegister(name = "Predictions", category = Category.Render, desc = "Рисует предикшн предметов")
public class Predictions extends Module {

    private final List<ProjectilePoint> projectilePoints = new ArrayList<>();

    private final BooleanSetting траектория = new BooleanSetting("Траектория", false);
    private final ModeSetting тип = new ModeSetting("Тип", "На конце", "На сущности", "На конце");
    private final BooleanSetting эндерЖемчуг = new BooleanSetting("Эндер жемчуг", true);
    private final BooleanSetting трезубец = new BooleanSetting("Трезубец", true);

    private final List<Vector3d> points = new ArrayList<>();
    private Vector3d endHitPos = null;
    private Vector3i endHitNormal = null;
    private boolean hitEntity = false;
    private final List<Vector3d> crossHitPositions = new ArrayList<>();
    private final List<Entity> cachedProjectiles = new ArrayList<>();
    private final List<Vector3i> crossHitNormals = new ArrayList<>();
    private final List<Boolean> crossHitEntities = new ArrayList<>();
    private final List<List<Vector3d>> shotPoints = new ArrayList<>();
    private boolean wasTargeting = false;
    private long lastSoundTime = 0;
    private float animato;
    private float crossAnimato;

    private final Map<Integer, CooldownEntry> cooldowns = new HashMap<>();

    private static final ItemStack TRIDENT_ICON_STACK = new ItemStack(Items.TRIDENT);
    private static final ItemStack ENDER_PEARL_ICON_STACK = new ItemStack(Items.ENDER_PEARL);

    private int shaderProgram = -1;
    private int circleShaderProgram = -1;
    private int trailShaderProgram = -1;
    private boolean shadersInitialized = false;
    private int circleCenterUniform = -1;
    private int circleRadiusUniform = -1;
    private int circleThicknessUniform = -1;
    private int circleProgressUniform = -1;
    private int circleTimeUniform = -1;
    private int circleFeatherUniform = -1;
    private int circleResolutionUniform = -1;
    private int circleColor1Uniform = -1;
    private int circleColor2Uniform = -1;
    private int circleTrackColorUniform = -1;
    private int circleGlowColorUniform = -1;
    private int trailTimeUniform = -1;
    private int trailColor1Uniform = -1;
    private int trailColor2Uniform = -1;
    private int hitTimeUniform = -1;
    private int hitEntityUniform = -1;
    private int hitColorUniform = -1;
    private int hitInnerRadiusUniform = -1;
    private int hitOuterRadiusUniform = -1;
    private int hitSoftnessUniform = -1;

    public Predictions() {
        this.addSettings(new Setting[]{траектория, тип, эндерЖемчуг, трезубец});
    }

    private static class CooldownEntry {
        final int startTicks;
        float smoothRemain;
        long lastSeenMs;
        long lastNs;
        float pulsePhase;
        float spawnTime;
        float fadeAlpha;
        float scaleAnim;
        boolean seenThisFrame;
        boolean fadingOut;
        float lastCx;
        float lastCy;
        float lastAngleDeg;
        boolean lastOnScreen;
        ItemStack lastIconStack = ItemStack.EMPTY;
        String lastTimeText;
        float lastTimeWidth;

        CooldownEntry(int startTicks) {
            this.startTicks = Math.max(1, startTicks);
            this.smoothRemain = 1f;
            this.lastSeenMs = System.currentTimeMillis();
            this.lastNs = System.nanoTime();
            this.pulsePhase = 0f;
            this.spawnTime = (float)(System.currentTimeMillis() / 1000.0);
            this.fadeAlpha = 0f;
            this.scaleAnim = 0.82f;
            this.seenThisFrame = false;
            this.fadingOut = false;
        }
    }

    private static class ProjectilePoint {
        Vector3d position;
        int ticks;
        Entity entity;
        ProjectilePoint(Vector3d position, int ticks, Entity entity) {
            this.position = position;
            this.ticks = ticks;
            this.entity = entity;
        }
    }

    private static class Indicator {
        float x, y;
        float angleDeg;
        boolean onScreen;
        Indicator(float x, float y, float angleDeg, boolean onScreen) {
            this.x = x; this.y = y; this.angleDeg = angleDeg; this.onScreen = onScreen;
        }
    }

    private void initShaders() {
        if (shadersInitialized) return;
        shadersInitialized = true;

        String circleVert =
                "#version 120\n" +
                        "varying vec2 v_texCoord;\n" +
                        "void main() {\n" +
                        "    v_texCoord = gl_MultiTexCoord0.xy;\n" +
                        "    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;\n" +
                        "}\n";

        String circleFrag =
                "#version 120\n" +
                        "uniform vec2 u_center;\n" +
                        "uniform float u_radius;\n" +
                        "uniform float u_thickness;\n" +
                        "uniform float u_progress;\n" +
                        "uniform float u_time;\n" +
                        "uniform vec4 u_color1;\n" +
                        "uniform vec4 u_color2;\n" +
                        "uniform vec4 u_trackColor;\n" +
                        "uniform vec4 u_glowColor;\n" +
                        "uniform float u_feather;\n" +
                        "uniform vec2 u_resolution;\n" +
                        "\n" +
                        "#define PI 3.14159265359\n" +
                        "#define TWO_PI 6.28318530718\n" +
                        "\n" +
                        "void main() {\n" +
                        "    vec2 fragCoord = gl_FragCoord.xy;\n" +
                        "    fragCoord.y = u_resolution.y - fragCoord.y;\n" +
                        "    vec2 uv = fragCoord - u_center;\n" +
                        "    float dist = length(uv);\n" +
                        "    float angle = atan(uv.y, uv.x);\n" +
                        "    angle = angle + PI * 0.5;\n" +
                        "    if (angle < 0.0) angle += TWO_PI;\n" +
                        "    float normalizedAngle = angle / TWO_PI;\n" +
                        "    \n" +
                        "    float innerR = u_radius - u_thickness * 0.5;\n" +
                        "    float outerR = u_radius + u_thickness * 0.5;\n" +
                        "    \n" +
                        "    float ringAlpha = smoothstep(innerR - u_feather, innerR + u_feather * 0.3, dist) * \n" +
                        "                      (1.0 - smoothstep(outerR - u_feather * 0.3, outerR + u_feather, dist));\n" +
                        "    \n" +
                        "    vec4 trackCol = u_trackColor * ringAlpha;\n" +
                        "    \n" +
                        "    float prog = clamp(u_progress, 0.0, 1.0);\n" +
                        "    float edgeSoft = 0.015;\n" +
                        "    float arcAlpha = smoothstep(prog + edgeSoft, prog - edgeSoft, normalizedAngle);\n" +
                        "    \n" +
                        "    float t = normalizedAngle / max(prog, 0.001);\n" +
                        "    vec4 arcColor = mix(u_color1, u_color2, t);\n" +
                        "    \n" +
                        "    float pulse = 0.92 + 0.08 * sin(u_time * 3.0 + normalizedAngle * 8.0);\n" +
                        "    arcColor.rgb *= pulse;\n" +
                        "    \n" +
                        "    float headGlow = 0.0;\n" +
                        "    if (prog > 0.01) {\n" +
                        "        float headAngle = prog * TWO_PI - PI * 0.5;\n" +
                        "        if (headAngle < 0.0) headAngle += TWO_PI;\n" +
                        "        vec2 headPos = vec2(cos(headAngle - PI * 0.5 + TWO_PI), sin(headAngle - PI * 0.5 + TWO_PI)) * u_radius;\n" +
                        "        float headDist = length(uv - headPos);\n" +
                        "        headGlow = exp(-headDist * headDist / (u_thickness * u_thickness * 3.6)) * arcAlpha * ringAlpha;\n" +
                        "    }\n" +
                        "    \n" +
                        "    float glowDist = abs(dist - u_radius);\n" +
                        "    float outerGlow = exp(-glowDist * glowDist / (u_thickness * u_thickness * 4.0)) * arcAlpha;\n" +
                        "    \n" +
                        "    vec4 result = trackCol;\n" +
                        "    vec4 arcFinal = arcColor * ringAlpha * arcAlpha;\n" +
                        "    result = mix(result, arcFinal, arcAlpha * ringAlpha);\n" +
                        "    \n" +
                        "    vec4 glowFinal = u_glowColor * outerGlow * 0.5;\n" +
                        "    result.rgb += glowFinal.rgb * glowFinal.a;\n" +
                        "    result.a = max(result.a, glowFinal.a * 0.3);\n" +
                        "    \n" +
                        "    vec4 headCol = mix(u_color2, u_glowColor, 0.35) * (headGlow * 0.45);\n" +
                        "    result.rgb += headCol.rgb * headCol.a;\n" +
                        "    result.a = max(result.a, headCol.a * 0.8);\n" +
                        "    \n" +
                        "    gl_FragColor = result;\n" +
                        "}\n";

        circleShaderProgram = createShaderProgram(circleVert, circleFrag);

        String trailVert =
                "#version 120\n" +
                        "varying vec3 v_worldPos;\n" +
                        "void main() {\n" +
                        "    v_worldPos = gl_Vertex.xyz;\n" +
                        "    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;\n" +
                        "    gl_FrontColor = gl_Color;\n" +
                        "}\n";

        String trailFrag =
                "#version 120\n" +
                        "uniform float u_time;\n" +
                        "uniform vec4 u_color1;\n" +
                        "uniform vec4 u_color2;\n" +
                        "varying vec3 v_worldPos;\n" +
                        "\n" +
                        "void main() {\n" +
                        "    float flow = fract(v_worldPos.x * 0.5 + v_worldPos.z * 0.5 - u_time * 2.0);\n" +
                        "    vec4 col = mix(u_color1, u_color2, flow);\n" +
                        "    float pulse = 0.85 + 0.15 * sin(u_time * 4.0 + v_worldPos.y * 3.0);\n" +
                        "    col.rgb *= pulse;\n" +
                        "    col.a *= gl_Color.a;\n" +
                        "    gl_FragColor = col;\n" +
                        "}\n";

        trailShaderProgram = createShaderProgram(trailVert, trailFrag);

        String hitVert =
                "#version 120\n" +
                        "varying vec2 v_uv;\n" +
                        "void main() {\n" +
                        "    v_uv = gl_MultiTexCoord0.xy;\n" +
                        "    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;\n" +
                        "}\n";

        String hitFrag =
                "#version 120\n" +
                        "uniform float u_time;\n" +
                        "uniform float u_hitEntity;\n" +
                        "uniform vec4 u_color;\n" +
                        "uniform float u_innerRadius;\n" +
                        "uniform float u_outerRadius;\n" +
                        "uniform float u_softness;\n" +
                        "varying vec2 v_uv;\n" +
                        "\n" +
                        "void main() {\n" +
                        "    vec2 uv = v_uv * 2.0 - 1.0;\n" +
                        "    float dist = length(uv);\n" +
                        "    float ring = smoothstep(u_innerRadius - u_softness, u_innerRadius + u_softness, dist) *\n" +
                        "                 (1.0 - smoothstep(u_outerRadius - u_softness, u_outerRadius + u_softness, dist));\n" +
                        "    float ringMid = (u_innerRadius + u_outerRadius) * 0.5;\n" +
                        "    float glow = exp(-pow((dist - ringMid) * 6.0, 2.0)) * (0.42 + u_hitEntity * 0.18);\n" +
                        "    float pulse = 0.90 + 0.10 * sin(u_time * (4.0 + u_hitEntity));\n" +
                        "    float alpha = max(ring, glow * 0.60) * u_color.a * pulse;\n" +
                        "    vec3 rgb = u_color.rgb * (ring * 1.10 + glow * 0.72);\n" +
                        "    gl_FragColor = vec4(rgb, alpha);\n" +
                        "}\n";

        shaderProgram = createShaderProgram(hitVert, hitFrag);
        cachePredictionUniforms();
    }

    private void cachePredictionUniforms() {
        if (circleShaderProgram > 0) {
            circleCenterUniform = GL20.glGetUniformLocation(circleShaderProgram, "u_center");
            circleRadiusUniform = GL20.glGetUniformLocation(circleShaderProgram, "u_radius");
            circleThicknessUniform = GL20.glGetUniformLocation(circleShaderProgram, "u_thickness");
            circleProgressUniform = GL20.glGetUniformLocation(circleShaderProgram, "u_progress");
            circleTimeUniform = GL20.glGetUniformLocation(circleShaderProgram, "u_time");
            circleFeatherUniform = GL20.glGetUniformLocation(circleShaderProgram, "u_feather");
            circleResolutionUniform = GL20.glGetUniformLocation(circleShaderProgram, "u_resolution");
            circleColor1Uniform = GL20.glGetUniformLocation(circleShaderProgram, "u_color1");
            circleColor2Uniform = GL20.glGetUniformLocation(circleShaderProgram, "u_color2");
            circleTrackColorUniform = GL20.glGetUniformLocation(circleShaderProgram, "u_trackColor");
            circleGlowColorUniform = GL20.glGetUniformLocation(circleShaderProgram, "u_glowColor");
        }
        if (trailShaderProgram > 0) {
            trailTimeUniform = GL20.glGetUniformLocation(trailShaderProgram, "u_time");
            trailColor1Uniform = GL20.glGetUniformLocation(trailShaderProgram, "u_color1");
            trailColor2Uniform = GL20.glGetUniformLocation(trailShaderProgram, "u_color2");
        }
        if (shaderProgram > 0) {
            hitTimeUniform = GL20.glGetUniformLocation(shaderProgram, "u_time");
            hitEntityUniform = GL20.glGetUniformLocation(shaderProgram, "u_hitEntity");
            hitColorUniform = GL20.glGetUniformLocation(shaderProgram, "u_color");
            hitInnerRadiusUniform = GL20.glGetUniformLocation(shaderProgram, "u_innerRadius");
            hitOuterRadiusUniform = GL20.glGetUniformLocation(shaderProgram, "u_outerRadius");
            hitSoftnessUniform = GL20.glGetUniformLocation(shaderProgram, "u_softness");
        }
    }

    private int createShaderProgram(String vertSrc, String fragSrc) {
        int vert = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vert, vertSrc);
        GL20.glCompileShader(vert);
        if (GL20.glGetShaderi(vert, GL20.GL_COMPILE_STATUS) == 0) {
            GL20.glDeleteShader(vert);
            return -1;
        }

        int frag = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(frag, fragSrc);
        GL20.glCompileShader(frag);
        if (GL20.glGetShaderi(frag, GL20.GL_COMPILE_STATUS) == 0) {
            GL20.glDeleteShader(vert);
            GL20.glDeleteShader(frag);
            return -1;
        }

        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vert);
        GL20.glAttachShader(program, frag);
        GL20.glLinkProgram(program);

        GL20.glDeleteShader(vert);
        GL20.glDeleteShader(frag);

        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == 0) {
            GL20.glDeleteProgram(program);
            return -1;
        }

        return program;
    }

    @Subscribe
    public void onUpdate(EventUpdate e) {
        if (mc.world == null) return;
        boolean trackPearls = эндерЖемчуг.get();
        boolean trackTridents = трезубец.get();
        if (!trackPearls && !trackTridents) {
            cachedProjectiles.clear();
            return;
        }
        cachedProjectiles.clear();
        for (Entity entity : mc.world.getAllEntities()) {
            if ((trackPearls && entity instanceof EnderPearlEntity) || (trackTridents && entity instanceof TridentEntity)) {
                cachedProjectiles.add(entity);
            }
        }
    }

    @Subscribe
    public void aa(EventDisplay e) {
        if (e.getType() != EventDisplay.Type.HIGH) return;

        initShaders();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        MainWindow window = mc.getMainWindow();
        float scaleFactor = (float) window.getGuiScaleFactor();
        float resW = window.getWidth();
        float resH = window.getHeight();
        float time = (float)(System.currentTimeMillis() % 100000L) / 1000.0f;
        long nowMs = System.currentTimeMillis();

        for (CooldownEntry entry : cooldowns.values()) {
            entry.seenThisFrame = false;
        }

        for (ProjectilePoint point : projectilePoints) {
            boolean show = (point.entity instanceof TridentEntity && трезубец.get())
                    || (point.entity instanceof EnderPearlEntity && эндерЖемчуг.get());
            if (!show) continue;

            Vector3d worldPos = тип.is("На сущности")
                    ? new Vector3d(
                    point.entity.lastTickPosX + (point.entity.getPosX() - point.entity.lastTickPosX) * mc.getRenderPartialTicks(),
                    point.entity.lastTickPosY + (point.entity.getPosY() - point.entity.lastTickPosY) * mc.getRenderPartialTicks() - 0.30F,
                    point.entity.lastTickPosZ + (point.entity.getPosZ() - point.entity.lastTickPosZ) * mc.getRenderPartialTicks()
            )
                    : new Vector3d(point.position.x, point.position.y - 0.30F, point.position.z);

            float radius     = 14.0F;
            float thickness  = 3.0F;
            float feather    = 1.6F;
            float iconSize   = 12.0F;
            float safePad    = radius + 32.0F;

            int accent       = Theme.MainColor(0);
            int arcStartCol  = ColorUtils.setAlpha(accent, 245);
            int arcEndCol    = ColorUtils.setAlpha(ColorUtils.brighter(accent, 0.6f), 245);
            int glowCol      = ColorUtils.setAlpha(accent, 110);
            int trackColor   = ColorUtils.rgba(20, 20, 20, 210);
            int textColor    = -1;

            Indicator ind = projectClampedFixed(worldPos, safePad);
            float cx = ind.x, cy = ind.y;

            double timeSec = (point.ticks * 50) / 1000.0;
            String timeText = String.format("%.1fс", timeSec);
            float timeWidth = Fonts.sfuy.getWidth(timeText, 7.0F);

            ItemStack iconStack = (point.entity instanceof TridentEntity)
                    ? TRIDENT_ICON_STACK
                    : ENDER_PEARL_ICON_STACK;

            int id = point.entity.getEntityId();
            CooldownEntry entry = cooldowns.get(id);
            if (entry == null || point.ticks > entry.startTicks) {
                entry = new CooldownEntry(point.ticks);
                cooldowns.put(id, entry);
            }
            entry.lastSeenMs = nowMs;
            entry.seenThisFrame = true;
            entry.fadingOut = false;

            float targetRemain = Math.max(0f, Math.min(1f, point.ticks / (float) entry.startTicks));
            long nowNs = System.nanoTime();
            float dt = (nowNs - entry.lastNs) / 1_000_000_000f;
            entry.lastNs = nowNs;
            float lambda = 12f;
            float k = 1f - (float) Math.exp(-lambda * dt);
            entry.smoothRemain += (targetRemain - entry.smoothRemain) * k;
            entry.fadeAlpha += (1.0f - entry.fadeAlpha) * (1.0f - (float)Math.exp(-18.0f * dt));
            entry.scaleAnim += (1.0f - entry.scaleAnim) * (1.0f - (float)Math.exp(-14.0f * dt));
            entry.lastCx = cx;
            entry.lastCy = cy;
            entry.lastAngleDeg = ind.angleDeg;
            entry.lastOnScreen = ind.onScreen;
            entry.lastIconStack = iconStack;
            entry.lastTimeText = timeText;
            entry.lastTimeWidth = timeWidth;

            renderCooldownIndicator(e, cx, cy, radius, thickness, feather, iconSize, time,
                    entry.smoothRemain, entry.fadeAlpha, entry.scaleAnim,
                    arcStartCol, arcEndCol, trackColor, glowCol, textColor,
                    iconStack, timeText, timeWidth, ind.onScreen, ind.angleDeg,
                    scaleFactor, resW, resH);
        }

        RenderSystem.disableBlend();

        int accent = Theme.MainColor(0);
        int arcStartCol = ColorUtils.setAlpha(accent, 245);
        int arcEndCol = ColorUtils.setAlpha(ColorUtils.brighter(accent, 0.6f), 245);
        int glowCol = ColorUtils.setAlpha(accent, 110);
        int trackColor = ColorUtils.rgba(20, 20, 20, 210);

        Iterator<Map.Entry<Integer, CooldownEntry>> iterator = cooldowns.entrySet().iterator();
        while (iterator.hasNext()) {
            CooldownEntry entry = iterator.next().getValue();
            if (entry.seenThisFrame) continue;

            long nowNs = System.nanoTime();
            float dt = (nowNs - entry.lastNs) / 1_000_000_000f;
            entry.lastNs = nowNs;

            if (nowMs - entry.lastSeenMs > 40L) {
                entry.fadingOut = true;
            }

            if (entry.fadingOut) {
                entry.fadeAlpha += (0.0f - entry.fadeAlpha) * (1.0f - (float)Math.exp(-11.0f * dt));
                entry.scaleAnim += (0.72f - entry.scaleAnim) * (1.0f - (float)Math.exp(-12.0f * dt));
            }

            if (entry.fadeAlpha <= 0.035f || nowMs - entry.lastSeenMs > 900L) {
                iterator.remove();
                continue;
            }

            if (!entry.lastIconStack.isEmpty()) {
                renderCooldownIndicator(e, entry.lastCx, entry.lastCy, 14.0F, 3.0F, 1.6F, 12.0F, time,
                        entry.smoothRemain, entry.fadeAlpha, entry.scaleAnim,
                        arcStartCol, arcEndCol, trackColor, glowCol, -1,
                        entry.lastIconStack,
                        entry.lastTimeText == null ? "" : entry.lastTimeText,
                        entry.lastTimeWidth,
                        entry.lastOnScreen, entry.lastAngleDeg,
                        scaleFactor, resW, resH);
            }
        }

        RenderSystem.disableBlend();
    }

    private void drawShaderCircle(float cx, float cy, float radius, float thickness, float feather,
                                  float progress, float time,
                                  int col1, int col2, int trackCol, int glowCol,
                                  float alphaMul, float scaleMul,
                                  float scale, float resW, float resH) {

        GL20.glUseProgram(circleShaderProgram);

        GL20.glUniform2f(circleCenterUniform, cx * scale, cy * scale);
        GL20.glUniform1f(circleRadiusUniform, radius * scaleMul * scale);
        GL20.glUniform1f(circleThicknessUniform, thickness * scaleMul * scale);
        GL20.glUniform1f(circleProgressUniform, progress);
        GL20.glUniform1f(circleTimeUniform, time);
        GL20.glUniform1f(circleFeatherUniform, feather * scale);
        GL20.glUniform2f(circleResolutionUniform, resW, resH);
        uploadColor4f(circleColor1Uniform, col1, alphaMul);
        uploadColor4f(circleColor2Uniform, col2, alphaMul);
        uploadColor4f(circleTrackColorUniform, trackCol, alphaMul);
        uploadColor4f(circleGlowColorUniform, glowCol, alphaMul);

        RenderSystem.disableTexture();

        float scaledRadius = radius * scaleMul;
        float scaledThickness = thickness * scaleMul;
        float ext = (scaledRadius + scaledThickness + feather * 4.0f);
        float x1 = cx - ext;
        float y1 = cy - ext;
        float x2 = cx + ext;
        float y2 = cy + ext;

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(x1, y2, 0).tex(0, 0).endVertex();
        buffer.pos(x2, y2, 0).tex(1, 0).endVertex();
        buffer.pos(x2, y1, 0).tex(1, 1).endVertex();
        buffer.pos(x1, y1, 0).tex(0, 1).endVertex();
        Tessellator.getInstance().draw();

        GL20.glUseProgram(0);
        RenderSystem.enableTexture();
    }

    private void renderCooldownIndicator(EventDisplay e, float cx, float cy, float radius, float thickness, float feather,
                                         float iconSize, float time, float progress, float alphaMul, float scaleMul,
                                         int arcStartCol, int arcEndCol, int trackColor, int glowCol, int textColor,
                                         ItemStack iconStack, String timeText, float timeWidth,
                                         boolean onScreen, float angleDeg,
                                         float scaleFactor, float resW, float resH) {
        float scaledIcon = iconSize * scaleMul;
        float scaledRadius = radius * scaleMul;
        float scaledThickness = thickness * scaleMul;

        RenderSystem.pushMatrix();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, alphaMul);

        if (circleShaderProgram > 0) {
            drawShaderCircle(cx, cy, radius, thickness, feather,
                    progress, time, arcStartCol, arcEndCol, trackColor, glowCol,
                    alphaMul, scaleMul, scaleFactor, resW, resH);
        } else {
            RenderUtility.drawRingAA(cx, cy, scaledRadius, scaledThickness, multiplyAlpha(trackColor, alphaMul), feather);
            drawArcGlowAA(cx, cy, scaledRadius, scaledThickness, -90f, -90f + Math.max(0.5f, 360f * progress), multiplyAlpha(glowCol, alphaMul), feather, 3);
            drawArcGradientAA(cx, cy, scaledRadius, scaledThickness, -90f, -90f + Math.max(0.5f, 360f * progress), multiplyAlpha(arcStartCol, alphaMul), multiplyAlpha(arcEndCol, alphaMul), feather, 18);
        }

        renderIndicatorItem(iconStack, cx - scaledIcon / 2f, cy - scaledIcon / 2f, scaledIcon, alphaMul);
        Fonts.sfuy.drawText(e.getMatrixStack(), timeText, cx - timeWidth / 2.0F, cy + scaledRadius + 3.5F, multiplyAlpha(textColor, alphaMul), 7.0F);

        if (!onScreen) {
            drawDirectionArrowSmooth(cx, cy, angleDeg, scaledRadius + 5.0F, 8.0F * scaleMul, 4.0F * scaleMul, multiplyAlpha(arcEndCol, alphaMul), time);
        }

        RenderSystem.popMatrix();
    }

    private void renderIndicatorItem(ItemStack iconStack, float x, float y, float size, float alphaMul) {
        if (iconStack == null || iconStack.isEmpty()) {
            return;
        }

        float scale = size / 16.0f;
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0.0f);
        GL11.glScalef(scale, scale, 1.0f);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, alphaMul);
        mc.getItemRenderer().renderItemIntoGUI(iconStack, 0, 0);
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glPopMatrix();
    }

    private void uploadColor4f(int uniform, int color, float alphaMul) {
        int alpha = MathHelper.clamp((int)(((color >> 24) & 0xFF) * Math.max(0.0f, Math.min(1.0f, alphaMul))), 0, 255);
        GL20.glUniform4f(uniform,
                ((color >> 16) & 0xFF) / 255f,
                ((color >> 8) & 0xFF) / 255f,
                (color & 0xFF) / 255f,
                alpha / 255f);
    }

    private int multiplyAlpha(int color, float mul) {
        int alpha = MathHelper.clamp((int)(((color >> 24) & 0xFF) * Math.max(0.0f, Math.min(1.0f, mul))), 0, 255);
        return ColorUtils.setAlpha(color, alpha);
    }

    @Subscribe
    public void onRender(WorldEvent event) {
        if (эндерЖемчуг.get() || трезубец.get()) {
            handleProjectileTrajectories();
        }
        if (траектория.get()) {
            handleTrajectories();
        }
    }

    private Indicator projectClampedFixed(Vector3d worldPos, float padding) {
        int w = mc.getMainWindow().getScaledWidth();
        int h = mc.getMainWindow().getScaledHeight();
        float cx = w / 2f, cy = h / 2f;

        Vector3d camPos = mc.getRenderManager().info.getProjectedView();
        float partialTicks = mc.getRenderPartialTicks();

        double dx = worldPos.x - camPos.x;
        double dy = worldPos.y - camPos.y;
        double dz = worldPos.z - camPos.z;

        float yaw = mc.player.prevRotationYaw + (mc.player.rotationYaw - mc.player.prevRotationYaw) * partialTicks;
        float pitch = mc.player.prevRotationPitch + (mc.player.rotationPitch - mc.player.prevRotationPitch) * partialTicks;

        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);

        float cosYaw = MathHelper.cos(-yawRad);
        float sinYaw = MathHelper.sin(-yawRad);
        float cosPitch = MathHelper.cos(pitchRad);
        float sinPitch = MathHelper.sin(pitchRad);

        double tempX = dx * cosYaw - dz * sinYaw;
        double tempZ = dx * sinYaw + dz * cosYaw;

        double localX = tempX;
        double localY = dy * cosPitch + tempZ * sinPitch;
        double localZ = -dy * sinPitch + tempZ * cosPitch;

        boolean inFront = localZ > 0;

        if (inFront) {
            float fov = (float) mc.gameRenderer.getFOVModifier(mc.getRenderManager().info, partialTicks, true);
            float halfFovRad = (float) Math.toRadians(fov / 2f);
            float scale = (h / 2f) / (float) Math.tan(halfFovRad);

            float screenX = (float) (cx - (localX / localZ) * scale);
            float screenY = (float) (cy - (localY / localZ) * scale);

            float minX = padding, maxX = w - padding;
            float minY = padding, maxY = h - padding;

            if (screenX >= minX && screenX <= maxX && screenY >= minY && screenY <= maxY) {
                return new Indicator(screenX, screenY, 0, true);
            }

            float dirX = screenX - cx;
            float dirY = screenY - cy;
            float len = (float) Math.sqrt(dirX * dirX + dirY * dirY);
            if (len > 0) {
                dirX /= len;
                dirY /= len;
            }

            float tHoriz = dirX > 0 ? (maxX - cx) / dirX : (dirX < 0 ? (minX - cx) / dirX : Float.POSITIVE_INFINITY);
            float tVert = dirY > 0 ? (maxY - cy) / dirY : (dirY < 0 ? (minY - cy) / dirY : Float.POSITIVE_INFINITY);
            float t = Math.min(tHoriz, tVert);

            float clampedX = cx + dirX * t;
            float clampedY = cy + dirY * t;
            float angle = (float) Math.toDegrees(Math.atan2(dirY, dirX));

            return new Indicator(clampedX, clampedY, angle, false);
        } else {
            float screenDirX = (float) -localX;
            float screenDirY = (float) -localY;

            float len = (float) Math.sqrt(screenDirX * screenDirX + screenDirY * screenDirY);
            if (len > 0) {
                screenDirX /= len;
                screenDirY /= len;
            }

            float angle = (float) Math.toDegrees(Math.atan2(screenDirY, screenDirX));

            float minX = padding, maxX = w - padding;
            float minY = padding, maxY = h - padding;

            float tHoriz = screenDirX > 0 ? (maxX - cx) / screenDirX : (screenDirX < 0 ? (minX - cx) / screenDirX : Float.POSITIVE_INFINITY);
            float tVert = screenDirY > 0 ? (maxY - cy) / screenDirY : (screenDirY < 0 ? (minY - cy) / screenDirY : Float.POSITIVE_INFINITY);
            float t = Math.min(tHoriz, tVert);

            float clampedX = cx + screenDirX * t;
            float clampedY = cy + screenDirY * t;

            return new Indicator(clampedX, clampedY, angle, false);
        }
    }

    private void drawArcGradientAA(float cx, float cy, float radius, float thickness,
                                   float startDeg, float endDeg,
                                   int colorStart, int colorEnd, float feather, int segments) {
        if (endDeg <= startDeg) return;
        segments = Math.max(2, segments);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);

        for (int i = 0; i < segments; i++) {
            float t0 = i / (float) segments;
            float t1 = (i + 1) / (float) segments;
            float a0 = startDeg + (endDeg - startDeg) * t0;
            float a1 = startDeg + (endDeg - startDeg) * t1;
            int col = ColorUtils.interpolate(colorStart, colorEnd, (t0 + t1) * 0.5f);
            RenderUtility.drawRingArcAA(cx, cy, radius, thickness, a0, a1, col, feather);
        }
    }

    private void drawArcGlowAA(float cx, float cy, float radius, float thickness,
                               float startDeg, float endDeg, int baseColor, float feather, int passes) {
        int a = (baseColor >> 24) & 0xFF;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);

        for (int i = passes; i >= 1; i--) {
            float mul = i;
            int alpha = Math.max(10, (int) (a * (0.20f * i / passes)));
            int col = ColorUtils.setAlpha(baseColor, alpha);
            RenderUtility.drawRingArcAA(cx, cy, radius, thickness, startDeg, endDeg, col, feather * mul);
        }
    }

    private void handleProjectileTrajectories() {
        if (mc.world == null) return;

        projectilePoints.clear();

        for (Entity entity : cachedProjectiles) {
            if (entity.lastTickPosX != entity.getPosX()) {
                boolean shouldTrack = (entity instanceof EnderPearlEntity && эндерЖемчуг.get())
                        || (entity instanceof TridentEntity && трезубец.get());
                if (!shouldTrack) continue;

                Entity projectile = entity;
                Vector3d motion = projectile.getMotion();
                Vector3d position = projectile.getPositionVec();
                int ticks = 0;

                for (int i = 0; i < 150; i++) {
                    Vector3d prevPos = position;
                    position = position.add(motion);
                    motion = getNextProjectileMotion(projectile, motion);

                    RayTraceContext context = new RayTraceContext(prevPos, position, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, projectile);
                    BlockRayTraceResult result = mc.world.rayTraceBlocks(context);
                    if (result.getType() == RayTraceResult.Type.BLOCK) {
                        position = result.getHitVec();
                    }

                    if (result.getType() == RayTraceResult.Type.BLOCK || position.y < -128.0F) {
                        projectilePoints.add(new ProjectilePoint(position, ticks, projectile));
                        break;
                    }
                    ticks++;
                }
            }
        }
    }

    private Vector3d getNextProjectileMotion(Entity entity, Vector3d motion) {
        motion = entity.isInWater() ? motion.scale(0.8) : motion.scale(0.99);
        if (!entity.hasNoGravity()) {
            if (entity instanceof EnderPearlEntity) motion = motion.add(0.0F, -0.03, 0.0F);
            else if (entity instanceof TridentEntity) motion = motion.add(0.0F, -0.05, 0.0F);
        }
        return motion;
    }

    private void drawDirectionArrowSmooth(float cx, float cy, float angleDeg, float distance, float length, float width, int color, float time) {
        double rad = Math.toRadians(angleDeg);

        float breathe = 1.0f + 0.15f * (float) Math.sin(time * 3.5);
        float actualDist = distance * breathe;

        float tipX = (float) (cx + Math.cos(rad) * actualDist);
        float tipY = (float) (cy + Math.sin(rad) * actualDist);

        float bx = (float) (tipX - Math.cos(rad) * length);
        float by = (float) (tipY - Math.sin(rad) * length);

        double leftRad = rad + Math.PI / 2.0;
        double rightRad = rad - Math.PI / 2.0;

        float leftX = (float) (bx + Math.cos(leftRad) * width);
        float leftY = (float) (by + Math.sin(leftRad) * width);
        float rightX = (float) (bx + Math.cos(rightRad) * width);
        float rightY = (float) (by + Math.sin(rightRad) * width);

        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;

        float pulse = 0.7f + 0.3f * (float) Math.sin(time * 4.0);
        a *= pulse;

        RenderSystem.pushMatrix();
        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
        GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT, GL11.GL_NICEST);

        RenderSystem.color4f(r, g, b, a);

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION);
        buffer.pos(tipX, tipY, 0).endVertex();
        buffer.pos(leftX, leftY, 0).endVertex();
        buffer.pos(rightX, rightY, 0).endVertex();
        Tessellator.getInstance().draw();

        RenderSystem.color4f(r, g, b, a * 0.3f);
        float glowScale = 1.4f;
        float gtipX = (float) (cx + Math.cos(rad) * (actualDist + length * 0.2f));
        float gtipY = (float) (cy + Math.sin(rad) * (actualDist + length * 0.2f));
        float gbx = (float) (gtipX - Math.cos(rad) * length * glowScale);
        float gby = (float) (gtipY - Math.sin(rad) * length * glowScale);
        float gleftX = (float) (gbx + Math.cos(leftRad) * width * glowScale);
        float gleftY = (float) (gby + Math.sin(leftRad) * width * glowScale);
        float grightX = (float) (gbx + Math.cos(rightRad) * width * glowScale);
        float grightY = (float) (gby + Math.sin(rightRad) * width * glowScale);

        buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION);
        buffer.pos(gtipX, gtipY, 0).endVertex();
        buffer.pos(gleftX, gleftY, 0).endVertex();
        buffer.pos(grightX, grightY, 0).endVertex();
        Tessellator.getInstance().draw();

        GL11.glDisable(GL11.GL_POLYGON_SMOOTH);

        RenderSystem.disableBlend();
        RenderSystem.enableTexture();
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.popMatrix();
    }

    private float lerp(float start, float end, float speed) {
        return start + (end - start) * speed * 0.1F;
    }

    private void handleTrajectories() {
        if (mc.player == null || mc.world == null) return;

        ItemStack mainHand = mc.player.getHeldItemMainhand();
        ItemStack offHand  = mc.player.getHeldItemOffhand();

        boolean isBowActive = mc.player.isHandActive() && mc.player.getActiveItemStack().getItem() instanceof BowItem;

        boolean isMainCrossbow = mainHand.getItem() instanceof CrossbowItem && (
                CrossbowItem.hasChargedProjectile(mainHand, Items.ARROW) ||
                        CrossbowItem.hasChargedProjectile(mainHand, Items.SPECTRAL_ARROW) ||
                        CrossbowItem.hasChargedProjectile(mainHand, Items.TIPPED_ARROW)
        );
        boolean isOffCrossbow = offHand.getItem() instanceof CrossbowItem && (
                CrossbowItem.hasChargedProjectile(offHand, Items.ARROW) ||
                        CrossbowItem.hasChargedProjectile(offHand, Items.SPECTRAL_ARROW) ||
                        CrossbowItem.hasChargedProjectile(offHand, Items.TIPPED_ARROW)
        );

        boolean isMainCrossbowActive = mc.player.isHandActive() && mc.player.getActiveItemStack().getItem() instanceof CrossbowItem && mc.player.getActiveHand() == Hand.MAIN_HAND;
        boolean isOffCrossbowActive  = mc.player.isHandActive() && mc.player.getActiveItemStack().getItem() instanceof CrossbowItem && mc.player.getActiveHand() == Hand.OFF_HAND;

        boolean isCrossbow = isMainCrossbow || isOffCrossbow || isMainCrossbowActive || isOffCrossbowActive;
        boolean isTridentActive = mc.player.isHandActive() && mc.player.getActiveItemStack().getItem() == Items.TRIDENT;

        if (!(isBowActive || isTridentActive || isCrossbow)) return;

        RenderSystem.pushMatrix();
        RenderSystem.disableTexture();
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        Vector3d viewPos = mc.getRenderManager().info.getProjectedView();
        RenderSystem.translated(-viewPos.x, -viewPos.y, -viewPos.z);

        points.clear();
        endHitPos = null;
        endHitNormal = null;
        hitEntity = false;
        crossHitPositions.clear();
        crossHitNormals.clear();
        crossHitEntities.clear();
        shotPoints.clear();

        float time = (float)(System.currentTimeMillis() % 100000L) / 1000.0f;
        int accent = Theme.MainColor(0);
        int accentEnd = ColorUtils.brighter(accent, 0.6f);

        if (isBowActive) {
            simulateBow(points);
            if (!points.isEmpty()) {
                drawTrailWithShader(points, accent, accentEnd, time, 0.8f);
                if (endHitPos != null) drawHitMarkerShader(endHitPos, endHitNormal, hitEntity, time);
            }
        } else if (isCrossbow) {
            if (isMainCrossbow || isMainCrossbowActive) {
                simulateCrossbow(mainHand, isMainCrossbowActive, false);
                for (int i = 0; i < shotPoints.size() && i < crossHitEntities.size(); i++) {
                    List<Vector3d> shot = shotPoints.get(i);
                    if (!shot.isEmpty()) {
                        drawTrailWithShader(shot, accent, accentEnd, time, 0.8f);
                    }
                }
                for (int i = 0; i < crossHitPositions.size() && i < crossHitNormals.size() && i < crossHitEntities.size(); i++) {
                    drawHitMarkerShader(crossHitPositions.get(i), crossHitNormals.get(i), crossHitEntities.get(i), time);
                }
            }
            if (isOffCrossbow || isOffCrossbowActive) {
                shotPoints.clear();
                crossHitPositions.clear();
                crossHitNormals.clear();
                crossHitEntities.clear();
                simulateCrossbow(offHand, isOffCrossbowActive, true);
                for (int i = 0; i < shotPoints.size() && i < crossHitEntities.size(); i++) {
                    List<Vector3d> shot = shotPoints.get(i);
                    if (!shot.isEmpty()) {
                        drawTrailWithShader(shot, accent, accentEnd, time, 0.8f);
                    }
                }
                for (int i = 0; i < crossHitPositions.size() && i < crossHitNormals.size() && i < crossHitEntities.size(); i++) {
                    drawHitMarkerShader(crossHitPositions.get(i), crossHitNormals.get(i), crossHitEntities.get(i), time);
                }
            }
        } else if (isTridentActive) {
            simulateTrident(points);
            if (!points.isEmpty()) {
                drawTrailWithShader(points, accent, accentEnd, time, 0.8f);
                if (endHitPos != null) drawHitMarkerShader(endHitPos, endHitNormal, hitEntity, time);
            }
        }

        RenderSystem.disableBlend();
        RenderSystem.enableTexture();
        RenderSystem.enableDepthTest();
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.popMatrix();
    }

    private void drawTrailWithShader(List<Vector3d> pts, int col1, int col2, float time, float alpha) {
        if (pts.size() < 2) return;

        if (trailShaderProgram > 0) {
            GL20.glUseProgram(trailShaderProgram);
            GL20.glUniform1f(trailTimeUniform, time);
            uploadColor4f(trailColor1Uniform, col1, alpha);
            uploadColor4f(trailColor2Uniform, col2, alpha);
        }

        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glLineWidth(2.5f);

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);

        int totalPts = pts.size();
        for (int i = 0; i < totalPts; i++) {
            Vector3d p = pts.get(i);
            float t = (float) i / (float) Math.max(1, totalPts - 1);
            float fadeAlpha = alpha * (1.0f - t * 0.6f);
            int r = (int)(((col1 >> 16) & 0xFF) * (1 - t) + ((col2 >> 16) & 0xFF) * t);
            int g = (int)(((col1 >> 8) & 0xFF) * (1 - t) + ((col2 >> 8) & 0xFF) * t);
            int b = (int)((col1 & 0xFF) * (1 - t) + (col2 & 0xFF) * t);
            buffer.pos(p.x, p.y, p.z).color(r, g, b, (int)(fadeAlpha * 255)).endVertex();
        }
        Tessellator.getInstance().draw();

        GL11.glLineWidth(5.0f);
        buffer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < totalPts; i++) {
            Vector3d p = pts.get(i);
            float t = (float) i / (float) Math.max(1, totalPts - 1);
            float fadeAlpha = alpha * 0.15f * (1.0f - t * 0.8f);
            int r = (int)(((col1 >> 16) & 0xFF) * (1 - t) + ((col2 >> 16) & 0xFF) * t);
            int g = (int)(((col1 >> 8) & 0xFF) * (1 - t) + ((col2 >> 8) & 0xFF) * t);
            int b2 = (int)((col1 & 0xFF) * (1 - t) + (col2 & 0xFF) * t);
            buffer.pos(p.x, p.y, p.z).color(r, g, b2, (int)(fadeAlpha * 255)).endVertex();
        }
        Tessellator.getInstance().draw();

        GL11.glLineWidth(1.0f);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);

        if (trailShaderProgram > 0) {
            GL20.glUseProgram(0);
        }
    }

    private void drawHitMarkerShader(Vector3d pos, Vector3i normal, boolean hitEnt, float time) {
        long currentTime = System.currentTimeMillis();

        if (hitEnt && !wasTargeting && currentTime - lastSoundTime > 500) {
            mc.player.playSound(net.minecraft.util.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5F, 2.0F);
            lastSoundTime = currentTime;
            wasTargeting = true;
        } else if (!hitEnt) {
            wasTargeting = false;
        }

        RenderSystem.pushMatrix();

        int accent = Theme.MainColor(0);
        float pulse = 0.7f + 0.3f * (float) Math.sin(time * 5.0);

        float r, g, b, a;
        if (hitEnt) {
            r = 1.0f; g = 0.2f; b = 0.2f;
        } else {
            r = ((accent >> 16) & 0xFF) / 255f;
            g = ((accent >> 8) & 0xFF) / 255f;
            b = (accent & 0xFF) / 255f;
        }
        a = 0.92f * pulse;

        if (shaderProgram <= 0) {
            RenderSystem.popMatrix();
            return;
        }

        GL20.glUseProgram(shaderProgram);
        GL20.glUniform1f(hitTimeUniform, time);
        GL20.glUniform1f(hitEntityUniform, hitEnt ? 1.0f : 0.0f);
        GL20.glUniform4f(hitColorUniform, r, g, b, a);
        GL20.glUniform1f(hitInnerRadiusUniform, 0.56f);
        GL20.glUniform1f(hitOuterRadiusUniform, 0.84f);
        GL20.glUniform1f(hitSoftnessUniform, 0.08f);

        RenderSystem.translated(pos.x, pos.y, pos.z);
        RenderSystem.disableTexture();
        RenderSystem.disableDepthTest();

        if (hitEnt || normal == null) {
            orientMarkerToCamera();
        } else if (normal != null) {
            if (normal.getY() != 0) {
                RenderSystem.translated(0.0F, normal.getY() * 0.01, 0.0F);
                RenderSystem.rotatef(90.0F, 1.0F, 0.0F, 0.0F);
            } else if (normal.getX() != 0) {
                RenderSystem.translated(normal.getX() * 0.01, 0.0F, 0.0F);
                RenderSystem.rotatef(90.0F, 0.0F, 1.0F, 0.0F);
            } else if (normal.getZ() != 0) {
                RenderSystem.translated(0.0F, 0.0F, normal.getZ() * 0.01);
            }
        }

        float size = 0.33F * (1.0f + 0.08f * (float)Math.sin(time * 3.0));
        float ext = size * 1.45f;
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(-ext,  ext, 0.0F).tex(0.0F, 0.0F).endVertex();
        buffer.pos( ext,  ext, 0.0F).tex(1.0F, 0.0F).endVertex();
        buffer.pos( ext, -ext, 0.0F).tex(1.0F, 1.0F).endVertex();
        buffer.pos(-ext, -ext, 0.0F).tex(0.0F, 1.0F).endVertex();
        Tessellator.getInstance().draw();

        GL20.glUseProgram(0);

        RenderSystem.enableTexture();
        RenderSystem.enableDepthTest();
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.popMatrix();
    }

    private void orientMarkerToCamera() {
        if (mc.getRenderManager() == null || mc.getRenderManager().info == null) {
            return;
        }

        float cameraYaw = mc.getRenderManager().info.getYaw();
        float cameraPitch = mc.getRenderManager().info.getPitch();
        RenderSystem.rotatef(-cameraYaw, 0.0F, 1.0F, 0.0F);
        RenderSystem.rotatef(cameraPitch, 1.0F, 0.0F, 0.0F);
        RenderSystem.rotatef(180.0F, 0.0F, 0.0F, 1.0F);
    }

    private Vector3d getCrosshairOrigin(Vector3d direction, float partialTicks) {
        Vector3d cameraPos = mc.getRenderManager() != null && mc.getRenderManager().info != null
                ? mc.getRenderManager().info.getProjectedView()
                : mc.player.getEyePosition(partialTicks);
        return cameraPos.add(direction.normalize().scale(0.15F));
    }

    private void simulateBow(List<Vector3d> points) {
        float partialTicks = mc.getRenderPartialTicks();
        float useTime = (float) mc.player.getItemInUseMaxCount();
        float velocity = BowItem.getArrowVelocity((int) useTime);
        animato = lerp(animato, velocity, 15.0F);
        if (animato < 0.1F) return;

        float pitch = mc.player.prevRotationPitch + (mc.player.rotationPitch - mc.player.prevRotationPitch) * partialTicks;
        float yaw   = mc.player.prevRotationYaw   + (mc.player.rotationYaw   - mc.player.prevRotationYaw)   * partialTicks;
        Vector3d direction = Vector3d.fromPitchYaw(pitch, yaw).normalize();
        Vector3d startPos = getCrosshairOrigin(direction, partialTicks);
        Vector3d velocityVec = direction.scale(animato * 3.0F);
        Vector3d currentPos = startPos;
        Vector3d currentVelocity = velocityVec;

        for (int i = 0; i < 150; i++) {
            Vector3d prevPos = currentPos;
            currentPos = currentPos.add(currentVelocity);
            currentVelocity = applyDragAndGravity(currentVelocity, 0.99, 0.05);

            int steps = 50;
            for (int j = 1; j <= steps; j++) {
                double t = (double) j / steps;
                double x = prevPos.x + (currentPos.x - prevPos.x) * t;
                double y = prevPos.y + (currentPos.y - prevPos.y) * t;
                double z = prevPos.z + (currentPos.z - prevPos.z) * t;
                points.add(new Vector3d(x, y, z));
            }

            for (LivingEntity entity : mc.world.getEntitiesWithinAABB(LivingEntity.class, new AxisAlignedBB(prevPos, currentPos).grow(0.3))) {
                if (entity != mc.player) {
                    Optional<Vector3d> hit = entity.getBoundingBox().rayTrace(prevPos, currentPos);
                    if (hit.isPresent()) {
                        Vector3d hitPos = hit.get();
                        points.add(hitPos);
                        endHitPos = hitPos;
                        endHitNormal = null;
                        hitEntity = true;
                        return;
                    }
                }
            }

            RayTraceContext context = new RayTraceContext(prevPos, currentPos, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, mc.player);
            BlockRayTraceResult result = mc.world.rayTraceBlocks(context);
            if (result.getType() == RayTraceResult.Type.BLOCK || currentPos.y < -128.0F) {
                Vector3d hitPos = result.getHitVec();
                points.add(hitPos);
                endHitPos = hitPos;
                endHitNormal = result.getFace().getDirectionVec();
                hitEntity = false;
                return;
            }
        }
    }

    private void simulateCrossbow(ItemStack stack, boolean isActive, boolean isOffHand) {
        float partialTicks = mc.getRenderPartialTicks();
        int multishotLevel = EnchantmentHelper.getEnchantmentLevel(Enchantments.MULTISHOT, stack);
        float velocity = 3.0F;

        float pitch = mc.player.prevRotationPitch + (mc.player.rotationPitch - mc.player.prevRotationPitch) * partialTicks;
        float yaw   = mc.player.prevRotationYaw   + (mc.player.rotationYaw   - mc.player.prevRotationYaw)   * partialTicks;
        Vector3d direction = Vector3d.fromPitchYaw(pitch, yaw).normalize();
        Vector3d startPos = getCrosshairOrigin(direction, partialTicks);

        if (isActive) {
            int useTime = mc.player.getItemInUseMaxCount();
            int chargeTime = CrossbowItem.getChargeTime(stack);
            float charge = Math.min(Math.max((float) useTime / chargeTime, 0.0F), 1.0F);
            crossAnimato = lerp(crossAnimato, charge, 10.0F);
            velocity = crossAnimato * velocity;
        }

        if (multishotLevel > 0) {
            float spread = 10.0F;
            shotPoints.add(simulateSingleCrossShot(startPos, pitch, yaw, 0.0F, velocity));
            shotPoints.add(simulateSingleCrossShot(startPos, pitch, yaw, spread, velocity));
            shotPoints.add(simulateSingleCrossShot(startPos, pitch, yaw, -spread, velocity));
        } else {
            shotPoints.add(simulateSingleCrossShot(startPos, pitch, yaw, 0.0F, velocity));
        }

        hitEntity = crossHitEntities.contains(true);
    }

    private List<Vector3d> simulateSingleCrossShot(Vector3d startPos, float pitch, float yaw, float yawOffset, float velocity) {
        List<Vector3d> shot = new ArrayList<>();
        Vector3d direction = Vector3d.fromPitchYaw(pitch, yaw + yawOffset).normalize().scale(velocity);
        Vector3d currentPos = startPos;
        Vector3d currentVelocity = direction;

        for (int i = 0; i < 150; i++) {
            Vector3d prevPos = currentPos;
            currentPos = currentPos.add(currentVelocity);
            currentVelocity = applyDragAndGravity(currentVelocity, 0.99, 0.05);

            int steps = 50;
            for (int j = 1; j <= steps; j++) {
                double t = (double) j / steps;
                double x = prevPos.x + (currentPos.x - prevPos.x) * t;
                double y = prevPos.y + (currentPos.y - prevPos.y) * t;
                double z = prevPos.z + (currentPos.z - prevPos.z) * t;
                shot.add(new Vector3d(x, y, z));
            }

            Vector3d hitPos = null;
            Vector3i hitNormal = null;
            boolean hitEntity = false;

            for (LivingEntity entity : mc.world.getEntitiesWithinAABB(LivingEntity.class, new AxisAlignedBB(prevPos, currentPos).grow(0.3))) {
                if (entity != mc.player) {
                    Optional<Vector3d> hit = entity.getBoundingBox().rayTrace(prevPos, currentPos);
                    if (hit.isPresent()) {
                        hitPos = hit.get();
                        shot.add(hitPos);
                        hitNormal = null;
                        hitEntity = true;
                        break;
                    }
                }
            }

            if (hitPos != null) {
                crossHitPositions.add(hitPos);
                crossHitNormals.add(hitNormal);
                crossHitEntities.add(hitEntity);
                break;
            }

            RayTraceContext context = new RayTraceContext(prevPos, currentPos, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, mc.player);
            BlockRayTraceResult result = mc.world.rayTraceBlocks(context);
            if (result.getType() == RayTraceResult.Type.BLOCK || currentPos.y < -128.0F) {
                hitPos = result.getHitVec();
                hitNormal = result.getFace().getDirectionVec();
                shot.add(hitPos);
                crossHitPositions.add(hitPos);
                crossHitNormals.add(hitNormal);
                crossHitEntities.add(false);
                break;
            }
        }

        return shot;
    }

    private void simulateTrident(List<Vector3d> points) {
        float partialTicks = mc.getRenderPartialTicks();
        float yaw = mc.player.prevRotationYaw + (mc.player.rotationYaw - mc.player.prevRotationYaw) * partialTicks;
        float pitch = mc.player.prevRotationPitch + (mc.player.rotationPitch - mc.player.prevRotationPitch) * partialTicks;
        Vector3d direction = Vector3d.fromPitchYaw(pitch, yaw);
        Vector3d startPos = getCrosshairOrigin(direction, partialTicks);
        Vector3d velocity = direction.normalize().scale(2.5F);
        Vector3d currentPos = startPos;
        Vector3d currentVelocity = velocity;

        for (int i = 0; i < 150; i++) {
            points.add(currentPos);
            Vector3d prevPos = currentPos;
            currentPos = currentPos.add(currentVelocity);
            currentVelocity = applyDragAndGravity(currentVelocity, 0.99, 0.05);

            for (LivingEntity entity : mc.world.getEntitiesWithinAABB(LivingEntity.class, new AxisAlignedBB(prevPos, currentPos).grow(0.3))) {
                if (entity != mc.player) {
                    Optional<Vector3d> hit = entity.getBoundingBox().rayTrace(prevPos, currentPos);
                    if (hit.isPresent()) {
                        Vector3d hitPos = hit.get();
                        points.add(hitPos);
                        endHitPos = hitPos;
                        endHitNormal = null;
                        hitEntity = true;
                        return;
                    }
                }
            }

            RayTraceContext context = new RayTraceContext(prevPos, currentPos, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, mc.player);
            BlockRayTraceResult result = mc.world.rayTraceBlocks(context);
            if (result.getType() == RayTraceResult.Type.BLOCK || currentPos.y < -128.0F) {
                Vector3d hitPos = result.getHitVec();
                points.add(hitPos);
                endHitPos = hitPos;
                endHitNormal = result.getFace().getDirectionVec();
                hitEntity = false;
                return;
            }
        }
    }

    private Vector3d applyDragAndGravity(Vector3d velocity, double drag, double gravity) {
        Vector3d dragged = velocity.scale(drag);
        return dragged.add(0.0F, -gravity, 0.0F);
    }

    @Override
    public boolean onDisable() {
        super.onDisable();
        if (circleShaderProgram > 0) {
            GL20.glDeleteProgram(circleShaderProgram);
            circleShaderProgram = -1;
        }
        if (trailShaderProgram > 0) {
            GL20.glDeleteProgram(trailShaderProgram);
            trailShaderProgram = -1;
        }
        if (shaderProgram > 0) {
            GL20.glDeleteProgram(shaderProgram);
            shaderProgram = -1;
        }
        shadersInitialized = false;
        cooldowns.clear();
        return false;
    }
}
