package xd.harm.modules.impl.render;

import xd.harm.events.render.DEngineEvent;
import xd.harm.events.world.EventChangeWorld;
import xd.harm.events.movement.JumpEvent;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.ModeSetting;
import xd.harm.modules.settings.impl.SliderSetting;
import xd.harm.utils.render.color.ColorUtils;
import xd.harm.utils.render.rect.RenderUtility;
import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.Blocks;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14C;
import org.lwjgl.opengl.GL20;

import java.util.ArrayList;

@ModuleRegister(name = "JumpCircle", category = Category.Render, desc = "Круг при прыжке")
public class JumpCircle extends Module {

    private final ModeSetting texture = new ModeSetting("Моды", "1", "1", "2", "3", "4");
    private final SliderSetting maxRadius = new SliderSetting("Радиус", 2, 1, 6, 0.1f);
    private final SliderSetting speed = new SliderSetting("Скорость", 1.0f, 0.5f, 3.0f, 0.1f);
    private final SliderSetting intensity = new SliderSetting("Интенсивность", 1.0f, 0.1f, 2.0f, 0.1f);
    private final SliderSetting lifeTime = new SliderSetting("Время жизни", 1.5f, 0.5f, 5.0f, 0.1f);

    private static final ResourceLocation TEXTURE_1 = new ResourceLocation("harmony/images/gui/jumpcircle1.png");
    private static final ResourceLocation TEXTURE_2 = new ResourceLocation("harmony/images/gui/jumpcircle2.png");

    private final ArrayList<Circle> circles = new ArrayList<>();
    private final Tessellator tessellator = Tessellator.getInstance();
    private final BufferBuilder buffer = tessellator.getBuffer();

    private String lastMode = "";
    private int shaderProgram = -1;
    private int vertexShader = -1;
    private int fragmentShader = -1;
    private int timeUniform;
    private int modeUniform;
    private int intensityUniform;
    private int alphaUniform;
    private int color1Uniform;
    private int color2Uniform;
    private int progressUniform;
    private boolean shaderUniformCacheValid;
    private int cachedModeValue;
    private float cachedIntensityValue;
    private float cachedAlphaValue;
    private float cachedTimeValue;
    private float cachedProgressValue;
    private int cachedColor1Value;
    private int cachedColor2Value;

    private static final String VERTEX_SHADER =
            "#version 120\n" +
                    "varying vec2 uv;\n" +
                    "void main() {\n" +
                    "    uv = gl_MultiTexCoord0.xy;\n" +
                    "    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;\n" +
                    "}\n";

    private static final String FRAGMENT_SHADER =
            "#version 120\n" +
                    "varying vec2 uv;\n" +
                    "uniform float time;\n" +
                    "uniform int mode;\n" +
                    "uniform float intensity;\n" +
                    "uniform float alpha;\n" +
                    "uniform float progress;\n" +
                    "uniform vec3 color1;\n" +
                    "uniform vec3 color2;\n" +
                    "\n" +
                    "float hash(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }\n" +
                    "\n" +
                    "float noise(vec2 p) {\n" +
                    "    vec2 i = floor(p), f = fract(p);\n" +
                    "    f = f * f * (3.0 - 2.0 * f);\n" +
                    "    return mix(mix(hash(i), hash(i + vec2(1,0)), f.x), mix(hash(i + vec2(0,1)), hash(i + vec2(1,1)), f.x), f.y);\n" +
                    "}\n" +
                    "\n" +
                    "float fbm(vec2 p) {\n" +
                    "    float v = 0.0, a = 0.5;\n" +
                    "    mat2 rot = mat2(cos(0.5), sin(0.5), -sin(0.5), cos(0.5));\n" +
                    "    for(int i = 0; i < 6; i++) { v += a * noise(p); p = rot * p * 2.0; a *= 0.5; }\n" +
                    "    return v;\n" +
                    "}\n" +
                    "\n" +
                    "vec3 fire(vec2 p, float t, float prog) {\n" +
                    "    vec2 c = p - 0.5;\n" +
                    "    float d = length(c);\n" +
                    "    float appear = smoothstep(0.0, 0.15, prog);\n" +
                    "    vec2 q = p;\n" +
                    "    q.y -= t * 0.8;\n" +
                    "    q.x += sin(t * 2.0 + p.y * 5.0) * 0.05;\n" +
                    "    float n1 = fbm(q * 4.0 + t * 0.5);\n" +
                    "    float n2 = fbm(q * 8.0 - t * 0.3);\n" +
                    "    float n3 = fbm(q * 16.0 + t * 0.7);\n" +
                    "    float fire = n1 * 0.5 + n2 * 0.3 + n3 * 0.2;\n" +
                    "    fire *= (1.0 - d * 1.8);\n" +
                    "    fire = pow(clamp(fire, 0.0, 1.0), 1.5) * appear;\n" +
                    "    float spark = pow(hash(p * 100.0 + t), 20.0) * step(0.3, fire);\n" +
                    "    float wave = sin(d * 30.0 - t * 8.0) * 0.5 + 0.5;\n" +
                    "    wave *= smoothstep(0.5, 0.2, d);\n" +
                    "    vec3 col1 = color1 * 1.5;\n" +
                    "    vec3 col2 = color2;\n" +
                    "    vec3 col3 = mix(color1, vec3(1.0), 0.8);\n" +
                    "    vec3 col = mix(col2, col1, pow(fire, 0.6));\n" +
                    "    col = mix(col, col3, pow(fire, 2.0));\n" +
                    "    col += spark * vec3(1.0, 0.9, 0.7) * 3.0;\n" +
                    "    col += wave * color1 * 0.3 * fire;\n" +
                    "    col += exp(-d * 5.0) * color1 * 0.4 * appear;\n" +
                    "    return col * intensity * (fire + 0.1);\n" +
                    "}\n" +
                    "\n" +
                    "vec3 cyber(vec2 p, float t, float prog) {\n" +
                    "    vec2 c = p - 0.5;\n" +
                    "    float d = length(c);\n" +
                    "    float r = prog * 3.0;\n" +
                    "    float width = 0.3;\n" +
                    "    float mask = step(r - width, d) * step(d, r);\n" +
                    "    if (mask < 0.1) return vec3(0.0);\n" +
                    "    float ring = smoothstep(r - 0.05, r, d);\n" +
                    "    float hex = max(abs(fract(p.x*10.0)-0.5), abs(fract(p.y*10.0)-0.5));\n" +
                    "    float grid = smoothstep(0.48, 0.5, hex);\n" +
                    "    vec3 col = vec3(0.0);\n" +
                    "    col += grid * mix(color1, color2, 0.5);\n" +
                    "    col += ring * color1 * 4.0;\n" +
                    "    float scan = smoothstep(0.02, 0.0, abs(fract(p.y*5.0-t)-0.5));\n" +
                    "    col += scan * color2 * 0.5;\n" +
                    "    return col * intensity;\n" +
                    "}\n" +
                    "\n" +
                    "void main() {\n" +
                    "    float d = length(uv - 0.5) * 2.0;\n" +
                    "    if(d > 1.0) discard;\n" +
                    "    vec3 col;\n" +
                    "    if(mode == 0) col = fire(uv, time, progress);\n" +
                    "    else col = cyber(uv, time, progress);\n" +
                    "    float edge = 1.0 - smoothstep(0.7, 1.0, d);\n" +
                    "    float disappear = 1.0 - smoothstep(0.8, 1.0, progress);\n" +
                    "    float a = alpha * edge * disappear;\n" +
                    "    if(a < 0.01) discard;\n" +
                    "    gl_FragColor = vec4(col, a);\n" +
                    "}\n";

    public JumpCircle() {
        addSettings(texture, maxRadius, speed, intensity, lifeTime);
    }

    @Override
    public boolean onEnable() {
        circles.clear();
        lastMode = texture.get();
        if (isShaderMode()) {
            initShaders();
        }
        super.onEnable();
        return false;
    }

    @Override
    public boolean onDisable() {
        circles.clear();
        cleanupShaders();
        super.onDisable();
        return false;
    }

    @Subscribe
    public void onJump(JumpEvent e) {
        Vector3d pos = getEntityPosition(mc.player);
        BlockPos blockPos = new BlockPos(pos);
        if (mc.world.getBlockState(blockPos).getBlock() == Blocks.SNOW) {
            pos = pos.add(0, 0.125, 0);
        }
        circles.add(new Circle(pos, circles.size()));
    }

    @Subscribe
    public void onRender(DEngineEvent event) {
        String currentMode = texture.get();
        if (!currentMode.equals(lastMode)) {
            circles.clear();
            lastMode = currentMode;
        }

        float life = lifeTime.get();
        boolean shaderMode = isShaderMode(currentMode);
        boolean isCyber = "4".equals(currentMode);
        long now = System.currentTimeMillis();
        removeExpiredCircles(life, isCyber, now);

        if (circles.isEmpty()) {
            return;
        }

        if (shaderMode && shaderProgram == -1) {
            initShaders();
        }

        renderCircles(event.getMatrix(), currentMode, shaderMode, now, life, maxRadius.get(), speed.get(), intensity.get());
    }

    @Subscribe
    public void onChange(EventChangeWorld e) {
        circles.clear();
    }

    private void renderCircles(MatrixStack matrix, String currentMode, boolean shaderMode, long now, float life,
                               float maxRadiusValue, float speedValue, float intensityValue) {
        boolean lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);

        matrix.push();
        RenderSystem.enableBlend();
        RenderSystem.enableAlphaTest();
        RenderSystem.alphaFunc(GL14C.GL_GREATER, 0);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        if (lighting) RenderSystem.disableLighting();
        RenderSystem.shadeModel(GL11.GL_SMOOTH);
        RenderSystem.blendFunc(GL14C.GL_SRC_ALPHA, GL14C.GL_ONE_MINUS_CONSTANT_ALPHA);
        RenderUtility.setupOrientationMatrix(matrix, 0, 0, 0);

        if (shaderMode) {
            GL20.glUseProgram(shaderProgram);
            setShaderStaticUniforms("3".equals(currentMode) ? 0 : 1, intensityValue);

            for (int i = 0, size = circles.size(); i < size; i++) {
                renderShaderCircle(matrix, circles.get(i), now, life, maxRadiusValue, speedValue);
            }

            GL20.glUseProgram(0);
        } else {
            mc.getTextureManager().bindTexture("2".equals(currentMode) ? TEXTURE_2 : TEXTURE_1);

            for (int i = 0, size = circles.size(); i < size; i++) {
                renderTextureCircle(matrix, circles.get(i), now, life, maxRadiusValue);
            }
        }

        RenderSystem.blendFunc(GL14C.GL_SRC_ALPHA, GL14C.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.color3f(1f, 1f, 1f);
        RenderSystem.shadeModel(GL11.GL_FLAT);
        if (lighting) RenderSystem.enableLighting();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.alphaFunc(GL14C.GL_GREATER, 0.1f);
        RenderSystem.enableAlphaTest();
        matrix.pop();
    }

    private void renderShaderCircle(MatrixStack matrix, Circle circle, long now, float life, float maxRadiusValue, float speedValue) {
        float elapsed = circle.getTime(now);
        float progress = elapsed / life;
        float time = elapsed * speedValue;
        float radius = easeOutBack(Math.min(1.0f, progress * 4.0f)) * maxRadiusValue;
        float halfRadius = radius * 0.5f;

        int c1 = Theme.MainColor(circle.index * 30);
        int c2 = Theme.MainColor(circle.index * 30 + 90);
        setShaderCircleUniforms(time, progress, c1, c2);

        matrix.push();
        matrix.translate(circle.pos.x - halfRadius, circle.pos.y, circle.pos.z - halfRadius);
        matrix.rotate(Vector3f.XP.rotationDegrees(90));

        Matrix4f transform = matrix.getLast().getMatrix();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(transform, 0, 0, 0).tex(0, 0).endVertex();
        buffer.pos(transform, 0, radius, 0).tex(0, 1).endVertex();
        buffer.pos(transform, radius, radius, 0).tex(1, 1).endVertex();
        buffer.pos(transform, radius, 0, 0).tex(1, 0).endVertex();
        tessellator.draw();

        matrix.pop();
    }

    private void renderTextureCircle(MatrixStack matrix, Circle circle, long now, float life, float maxRadiusValue) {
        float delta = 1f - circle.getProgress(life, now);
        float wave = (delta > 0.5f ? 1f - delta : delta) * 2f;
        float alpha = clamp01(wave);
        float radius = easeOutBack(clamp01(delta)) * maxRadiusValue;
        float halfRadius = radius * 0.5f;

        matrix.push();
        matrix.translate(circle.pos.x - halfRadius, circle.pos.y, circle.pos.z - halfRadius);
        matrix.rotate(Vector3f.XP.rotationDegrees(90));

        Matrix4f transform = matrix.getLast().getMatrix();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        buffer.pos(transform, 0, 0, 0).tex(0, 0).color(getColor(circle.index, alpha)).endVertex();
        buffer.pos(transform, 0, radius, 0).tex(0, 1).color(getColor(circle.index + 90, alpha)).endVertex();
        buffer.pos(transform, radius, radius, 0).tex(1, 1).color(getColor(circle.index + 180, alpha)).endVertex();
        buffer.pos(transform, radius, 0, 0).tex(1, 0).color(getColor(circle.index + 270, alpha)).endVertex();
        tessellator.draw();

        matrix.pop();
    }

    private boolean isShaderMode() {
        return isShaderMode(texture.get());
    }

    private boolean isShaderMode(String mode) {
        return "3".equals(mode) || "4".equals(mode);
    }

    private int getColor(int index, float alpha) {
        return ColorUtils.multAlpha(Theme.MainColor(index), alpha);
    }

    private Vector3d getEntityPosition(Entity entity) {
        float pt = mc.getRenderPartialTicks();
        double x = entity.lastTickPosX + (entity.getPosX() - entity.lastTickPosX) * pt;
        double y = entity.lastTickPosY + (entity.getPosY() - entity.lastTickPosY) * pt;
        double z = entity.lastTickPosZ + (entity.getPosZ() - entity.lastTickPosZ) * pt;
        return new Vector3d(x + (entity.getPosX() - entity.lastTickPosX) * 2, y + 0.005, z + (entity.getPosZ() - entity.lastTickPosZ) * 2);
    }

    private float easeOutBack(float x) {
        double c1 = 1.70158;
        double c3 = c1 + 1;
        double t = x - 1;
        return (float) (1 + c3 * t * t * t + c1 * t * t);
    }

    private float clamp01(float value) {
        return Math.max(0, Math.min(1, value));
    }

    private void removeExpiredCircles(float life, boolean isCyber, long now) {
        int writeIndex = 0;
        int size = circles.size();
        float maxProgress = isCyber ? 0.4f : 1.0f;

        for (int readIndex = 0; readIndex < size; readIndex++) {
            Circle circle = circles.get(readIndex);
            if (circle.getProgress(life, now) < maxProgress) {
                if (writeIndex != readIndex) {
                    circles.set(writeIndex, circle);
                }
                writeIndex++;
            }
        }

        if (writeIndex < size) {
            circles.subList(writeIndex, size).clear();
        }
    }

    private void setShaderStaticUniforms(int modeValue, float intensityValue) {
        if (!shaderUniformCacheValid || cachedModeValue != modeValue) {
            GL20.glUniform1i(modeUniform, modeValue);
            cachedModeValue = modeValue;
        }

        if (!shaderUniformCacheValid || Float.compare(cachedIntensityValue, intensityValue) != 0) {
            GL20.glUniform1f(intensityUniform, intensityValue);
            cachedIntensityValue = intensityValue;
        }

        if (!shaderUniformCacheValid || Float.compare(cachedAlphaValue, 1.0f) != 0) {
            GL20.glUniform1f(alphaUniform, 1.0f);
            cachedAlphaValue = 1.0f;
        }
    }

    private void setShaderCircleUniforms(float time, float progress, int color1, int color2) {
        if (!shaderUniformCacheValid || Float.compare(cachedTimeValue, time) != 0) {
            GL20.glUniform1f(timeUniform, time);
            cachedTimeValue = time;
        }

        if (!shaderUniformCacheValid || Float.compare(cachedProgressValue, progress) != 0) {
            GL20.glUniform1f(progressUniform, progress);
            cachedProgressValue = progress;
        }

        if (!shaderUniformCacheValid || cachedColor1Value != color1) {
            GL20.glUniform3f(color1Uniform, ((color1 >> 16) & 0xFF) / 255f, ((color1 >> 8) & 0xFF) / 255f, (color1 & 0xFF) / 255f);
            cachedColor1Value = color1;
        }

        if (!shaderUniformCacheValid || cachedColor2Value != color2) {
            GL20.glUniform3f(color2Uniform, ((color2 >> 16) & 0xFF) / 255f, ((color2 >> 8) & 0xFF) / 255f, (color2 & 0xFF) / 255f);
            cachedColor2Value = color2;
        }

        shaderUniformCacheValid = true;
    }

    private void initShaders() {
        if (shaderProgram != -1) return;

        vertexShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vertexShader, VERTEX_SHADER);
        GL20.glCompileShader(vertexShader);

        fragmentShader = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(fragmentShader, FRAGMENT_SHADER);
        GL20.glCompileShader(fragmentShader);

        shaderProgram = GL20.glCreateProgram();
        GL20.glAttachShader(shaderProgram, vertexShader);
        GL20.glAttachShader(shaderProgram, fragmentShader);
        GL20.glLinkProgram(shaderProgram);

        timeUniform = GL20.glGetUniformLocation(shaderProgram, "time");
        modeUniform = GL20.glGetUniformLocation(shaderProgram, "mode");
        intensityUniform = GL20.glGetUniformLocation(shaderProgram, "intensity");
        alphaUniform = GL20.glGetUniformLocation(shaderProgram, "alpha");
        color1Uniform = GL20.glGetUniformLocation(shaderProgram, "color1");
        color2Uniform = GL20.glGetUniformLocation(shaderProgram, "color2");
        progressUniform = GL20.glGetUniformLocation(shaderProgram, "progress");
        invalidateShaderUniformCache();
    }

    private void cleanupShaders() {
        if (shaderProgram != -1) {
            GL20.glDeleteProgram(shaderProgram);
            GL20.glDeleteShader(vertexShader);
            GL20.glDeleteShader(fragmentShader);
            shaderProgram = -1;
            vertexShader = -1;
            fragmentShader = -1;
            invalidateShaderUniformCache();
        }
    }

    private void invalidateShaderUniformCache() {
        shaderUniformCacheValid = false;
        cachedModeValue = 0;
        cachedIntensityValue = Float.NaN;
        cachedAlphaValue = Float.NaN;
        cachedTimeValue = Float.NaN;
        cachedProgressValue = Float.NaN;
        cachedColor1Value = 0;
        cachedColor2Value = 0;
    }

    private class Circle {
        final Vector3d pos;
        final int index;
        final long startTime;

        Circle(Vector3d pos, int index) {
            this.pos = pos;
            this.index = index;
            this.startTime = System.currentTimeMillis();
        }

        float getTime(long now) {
            return (now - startTime) / 1000f;
        }

        float getProgress(float lifeTime, long now) {
            return getTime(now) / lifeTime;
        }
    }
}
