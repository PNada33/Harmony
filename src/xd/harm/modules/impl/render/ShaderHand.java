package xd.harm.modules.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.settings.PointOfView;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import xd.harm.Harmony;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.ColorSetting;
import xd.harm.modules.settings.impl.ModeSetting;
import xd.harm.modules.settings.impl.SliderSetting;
import xd.harm.utils.render.color.ColorUtils;

import java.util.ArrayList;
import java.util.List;

@ModuleRegister(name = "ShaderHand", category = Category.Render, desc = "Показывает инвизок")
public class ShaderHand extends Module {

    private static final float INV_255 = 1.0f / 255.0f;
    private static final int MIN_BLOOM_BUFFER_SIZE = 2;
    private static final int BLOOM_OFFSET = 1;

    public static ShaderHand getInstance() {
        for (Module module : Harmony.getInstance().getModuleManager().getModules()) {
            if (module instanceof ShaderHand) {
                return (ShaderHand) module;
            }
        }
        return null;
    }

    public final BooleanSetting glass = new BooleanSetting("Стекло", true);
    public final SliderSetting glassAlpha =
            new SliderSetting("Прозрачность", 0.5F, 0.0F, 1.0F, 0.05F).setVisible(() -> glass.get());

    public final BooleanSetting glow = new BooleanSetting("Глов", true);
    public final SliderSetting glowRadius =
            new SliderSetting("Радиус Глова", 3, 1, 5, 1).setVisible(() -> glow.get());

    public final BooleanSetting outerGlow =
            new BooleanSetting("Внешний глов", true).setVisible(() -> glow.get());

    public final SliderSetting outerExposure =
            new SliderSetting("Яркость внешнего", 2.0f, 0.5f, 5.0f, 0.1f)
                    .setVisible(() -> glow.get() && outerGlow.get());

    public final BooleanSetting innerGlow =
            new BooleanSetting("Внутренний глов", false).setVisible(() -> glow.get());

    public final SliderSetting innerExposure =
            new SliderSetting("Яркость внутреннего", 2.0f, 0.5f, 5.0f, 0.1f)
                    .setVisible(() -> glow.get() && innerGlow.get());

    public final ModeSetting colorMode =
            new ModeSetting("Цвет", "Клиент", "Радужный", "Клиент", "Свой");

    public final ColorSetting color1 =
            new ColorSetting("Цвет", ColorUtils.rgb(100, 255, 100))
                    .setVisible(() -> colorMode.is("Свой"));

    private Framebuffer handsBuffer;
    private final List<Framebuffer> bloomBuffers = new ArrayList<>();

    private int kawaseDownProgram = -1;
    private int kawaseUpProgram = -1;
    private int innerGlowProgram = -1;
    private int outerGlowProgram = -1;

    private int kawaseDownTextureUniform = -1;
    private int kawaseDownSizeUniform = -1;
    private int kawaseDownOffsetUniform = -1;
    private int kawaseDownHalfPixelUniform = -1;

    private int kawaseUpTextureUniform = -1;
    private int kawaseUpSizeUniform = -1;
    private int kawaseUpOffsetUniform = -1;
    private int kawaseUpHalfPixelUniform = -1;
    private int kawaseUpColorUniform = -1;

    private int innerBloomTextureUniform = -1;
    private int innerMaskTextureUniform = -1;
    private int innerGlowColor1Uniform = -1;
    private int innerGlowColor2Uniform = -1;

    private int outerBloomTextureUniform = -1;
    private int outerMaskTextureUniform = -1;
    private int outerGlowColor1Uniform = -1;
    private int outerGlowColor2Uniform = -1;

    private float quadWidth;
    private float quadHeight;

    public ShaderHand() {
        addSettings(
                glass,
                glassAlpha,
                glow,
                glowRadius,
                outerGlow,
                outerExposure,
                innerGlow,
                innerExposure,
                colorMode,
                color1
        );
    }

    private int getShaderColor(int index) {
        String mode = colorMode.get();

        switch (mode) {
            case "Радужный":
                return ColorUtils.rainbow(8, index * 50, 0.85f, 1.0f, 1.0f);

            case "Клиент":
                return index == 0 ? Theme.MainColor(0) : Theme.RectColor(0);

            case "Свой":
                return color1.get();

            default:
                return -1;
        }
    }

    private int getTextureId(Framebuffer fb) {
        return fb.func_242996_f();
    }

    public Framebuffer getHandsBuffer() {
        int width = mc.getMainWindow().getFramebufferWidth();
        int height = mc.getMainWindow().getFramebufferHeight();

        if (handsBuffer == null) {
            handsBuffer = new Framebuffer(width, height, true, Minecraft.IS_RUNNING_ON_MAC);
            handsBuffer.setFramebufferFilter(GL11.GL_LINEAR);
            handsBuffer.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
        }

        if (handsBuffer.framebufferWidth != width || handsBuffer.framebufferHeight != height) {
            handsBuffer.resize(width, height, Minecraft.IS_RUNNING_ON_MAC);
            handsBuffer.setFramebufferFilter(GL11.GL_LINEAR);
        }

        return handsBuffer;
    }

    public void draw() {
        if (mc.gameSettings.getPointOfView() != PointOfView.FIRST_PERSON) return;
        if (handsBuffer == null) return;

        if (kawaseDownProgram == -1) initShaders();

        setup2D();

        boolean glowEnabled = glow.get();
        boolean outerGlowEnabled = glowEnabled && outerGlow.get();
        boolean innerGlowEnabled = glowEnabled && innerGlow.get();

        boolean bloomNeeded = outerGlowEnabled || innerGlowEnabled;

        int bloomTex = -1;

        if (bloomNeeded) {
            int iterations = glowEnabled ? glowRadius.get().intValue() : 5;
            bloomTex = generateKawaseBloom(iterations, getTextureId(handsBuffer));
        }

        if (outerGlowEnabled || innerGlowEnabled) {
            renderGlow(bloomTex, outerGlowEnabled, innerGlowEnabled);
        }

        RenderSystem.enableBlend();

        if (glass.get()) {
            RenderSystem.blendFunc(
                    GlStateManager.SourceFactor.SRC_ALPHA,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
            );

            float alpha = glassAlpha.getFloat();
            RenderSystem.color4f(1f, 1f, 1f, alpha);
        } else {
            RenderSystem.blendFunc(
                    GlStateManager.SourceFactor.ONE,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
            );

            RenderSystem.color4f(1f, 1f, 1f, 1f);
        }

        handsBuffer.bindFramebufferTexture();
        drawQuads();

        restore3D();
    }

    private int generateKawaseBloom(int iterations, int sourceTexture) {
        int offset = BLOOM_OFFSET;

        setupBloomBuffers(iterations);

        int currentTexture = sourceTexture;

        GL20.glUseProgram(kawaseDownProgram);
        GlStateManager.activeTexture(GL13.GL_TEXTURE0);

        for (int i = 0; i < iterations; i++) {
            Framebuffer buffer = bloomBuffers.get(i);

            buffer.framebufferClear(Minecraft.IS_RUNNING_ON_MAC);
            buffer.bindFramebuffer(true);

            setUniform2f(
                    kawaseDownSizeUniform,
                    (float) buffer.framebufferWidth,
                    (float) buffer.framebufferHeight
            );

            setUniform2f(
                    kawaseDownOffsetUniform,
                    (float) (offset + i),
                    (float) (offset + i)
            );

            setUniform2f(
                    kawaseDownHalfPixelUniform,
                    0.5f / buffer.framebufferWidth,
                    0.5f / buffer.framebufferHeight
            );

            GlStateManager.bindTexture(currentTexture);

            drawQuads();

            currentTexture = getTextureId(buffer);
        }

        GL20.glUseProgram(kawaseUpProgram);
        GlStateManager.activeTexture(GL13.GL_TEXTURE0);

        for (int i = iterations - 1; i > 0; i--) {
            Framebuffer buffer = bloomBuffers.get(i - 1);

            buffer.bindFramebuffer(true);

            setUniform2f(
                    kawaseUpSizeUniform,
                    (float) buffer.framebufferWidth,
                    (float) buffer.framebufferHeight
            );

            setUniform2f(
                    kawaseUpOffsetUniform,
                    (float) (offset + i),
                    (float) (offset + i)
            );

            setUniform2f(
                    kawaseUpHalfPixelUniform,
                    0.5f / buffer.framebufferWidth,
                    0.5f / buffer.framebufferHeight
            );

            GlStateManager.bindTexture(currentTexture);

            drawQuads();

            currentTexture = getTextureId(buffer);
        }

        GL20.glUseProgram(0);

        mc.getFramebuffer().bindFramebuffer(true);

        return currentTexture;
    }

    private void renderGlow(int bloomTex, boolean renderOuterGlow, boolean renderInnerGlow) {
        int c1 = getShaderColor(0);
        int c2 = getShaderColor(1);
        int maskTex = getTextureId(handsBuffer);

        RenderSystem.enableBlend();

        boolean usedProgram = false;

        if (renderOuterGlow) {
            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

            float exposure = outerExposure.get();

            GL20.glUseProgram(outerGlowProgram);
            usedProgram = true;

            setUniformColor3f(outerGlowColor1Uniform, c1);
            setUniformColor3f(outerGlowColor2Uniform, c2);

            RenderSystem.color4f(1f, 1f, 1f, exposure);

            bindBloomAndMask(bloomTex, maskTex);

            drawQuads();
        }

        if (renderInnerGlow) {
            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            float innerExp = innerExposure.get();

            GL20.glUseProgram(innerGlowProgram);
            usedProgram = true;

            setUniformColor3f(innerGlowColor1Uniform, c1);
            setUniformColor3f(innerGlowColor2Uniform, c2);

            RenderSystem.color4f(1f, 1f, 1f, innerExp);

            bindBloomAndMask(bloomTex, maskTex);

            drawQuads();
        }

        if (usedProgram) {
            GL20.glUseProgram(0);
        }

        RenderSystem.defaultBlendFunc();
        RenderSystem.color4f(1f, 1f, 1f, 1f);
    }

    private void setupBloomBuffers(int iterations) {
        int framebufferWidth = mc.getMainWindow().getFramebufferWidth();
        int framebufferHeight = mc.getMainWindow().getFramebufferHeight();

        for (int i = bloomBuffers.size(); i < iterations; i++) {
            bloomBuffers.add(createBloomBuffer(framebufferWidth, framebufferHeight, i));
        }

        for (int i = 0; i < iterations; i++) {
            int w = getBloomBufferSize(framebufferWidth, i);
            int h = getBloomBufferSize(framebufferHeight, i);

            Framebuffer buffer = bloomBuffers.get(i);

            if (buffer.framebufferWidth != w || buffer.framebufferHeight != h) {
                buffer.resize(w, h, Minecraft.IS_RUNNING_ON_MAC);
                buffer.setFramebufferFilter(GL11.GL_LINEAR);
            }
        }
    }

    private Framebuffer createBloomBuffer(int framebufferWidth, int framebufferHeight, int iteration) {
        Framebuffer fbo = new Framebuffer(
                getBloomBufferSize(framebufferWidth, iteration),
                getBloomBufferSize(framebufferHeight, iteration),
                false,
                Minecraft.IS_RUNNING_ON_MAC
        );

        fbo.setFramebufferFilter(GL11.GL_LINEAR);
        fbo.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);

        return fbo;
    }

    private int getBloomBufferSize(int size, int iteration) {
        return Math.max(MIN_BLOOM_BUFFER_SIZE, size >> (iteration + 1));
    }

    private void bindBloomAndMask(int bloomTex, int maskTex) {
        GlStateManager.activeTexture(GL13.GL_TEXTURE1);
        GlStateManager.bindTexture(maskTex);

        GlStateManager.activeTexture(GL13.GL_TEXTURE0);
        GlStateManager.bindTexture(bloomTex);
    }

    private static void setUniform1i(int location, int value) {
        if (location >= 0) {
            GL20.glUniform1i(location, value);
        }
    }

    private static void setUniform1f(int location, float value) {
        if (location >= 0) {
            GL20.glUniform1f(location, value);
        }
    }

    private static void setUniform2f(int location, float x, float y) {
        if (location >= 0) {
            GL20.glUniform2f(location, x, y);
        }
    }

    private static void setUniform3f(int location, float x, float y, float z) {
        if (location >= 0) {
            GL20.glUniform3f(location, x, y, z);
        }
    }

    private static void setUniformColor3f(int location, int color) {
        setUniform3f(
                location,
                ((color >> 16) & 255) * INV_255,
                ((color >> 8) & 255) * INV_255,
                (color & 255) * INV_255
        );
    }

    private void drawQuads() {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        float w = quadWidth;
        float h = quadHeight;

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);

        buffer.pos(0, h, 0).tex(0, 0).endVertex();
        buffer.pos(w, h, 0).tex(1, 0).endVertex();
        buffer.pos(w, 0, 0).tex(1, 1).endVertex();
        buffer.pos(0, 0, 0).tex(0, 1).endVertex();

        tessellator.draw();
    }

    private void setup2D() {
        quadWidth = mc.getMainWindow().getScaledWidth();
        quadHeight = mc.getMainWindow().getScaledHeight();

        RenderSystem.matrixMode(GL11.GL_PROJECTION);
        RenderSystem.pushMatrix();
        RenderSystem.loadIdentity();

        RenderSystem.ortho(
                0.0D,
                quadWidth,
                quadHeight,
                0.0D,
                1000.0D,
                3000.0D
        );

        RenderSystem.matrixMode(GL11.GL_MODELVIEW);
        RenderSystem.pushMatrix();
        RenderSystem.loadIdentity();

        RenderSystem.translatef(0.0F, 0.0F, -2000.0F);

        RenderSystem.disableDepthTest();
        RenderSystem.disableAlphaTest();
    }

    private void restore3D() {
        RenderSystem.enableDepthTest();
        RenderSystem.enableAlphaTest();

        RenderSystem.color4f(1f, 1f, 1f, 1f);

        RenderSystem.bindTexture(0);

        RenderSystem.matrixMode(GL11.GL_PROJECTION);
        RenderSystem.popMatrix();

        RenderSystem.matrixMode(GL11.GL_MODELVIEW);
        RenderSystem.popMatrix();
    }

    private void initShaders() {
        String vert =
                "#version 120\n" +
                        "void main() {\n" +
                        "    gl_TexCoord[0] = gl_MultiTexCoord0;\n" +
                        "    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;\n" +
                        "}";

        String kawaseDown =
                "#version 120\n" +
                        "uniform sampler2D inTexture;\n" +
                        "uniform vec2 uOffset, uHalfPixel, uSize;\n" +
                        "void main() {\n" +
                        "    vec2 uv = gl_TexCoord[0].xy;\n" +
                        "    vec2 halfPixel = uHalfPixel * uOffset;\n" +
                        "    vec4 sum = texture2D(inTexture, uv) * 4.0;\n" +
                        "    sum += texture2D(inTexture, uv - halfPixel);\n" +
                        "    sum += texture2D(inTexture, uv + halfPixel);\n" +
                        "    sum += texture2D(inTexture, uv + vec2(halfPixel.x, -halfPixel.y));\n" +
                        "    sum += texture2D(inTexture, uv - vec2(halfPixel.x, -halfPixel.y));\n" +
                        "    gl_FragColor = sum / 8.0;\n" +
                        "}";

        String kawaseUp =
                "#version 120\n" +
                        "uniform sampler2D inTexture;\n" +
                        "uniform vec2 uOffset, uHalfPixel, uSize;\n" +
                        "uniform vec3 color;\n" +
                        "void main() {\n" +
                        "    vec2 uv = gl_TexCoord[0].xy;\n" +
                        "    vec2 halfPixel = uHalfPixel * uOffset;\n" +
                        "    vec4 sum = texture2D(inTexture, uv + vec2(-halfPixel.x * 2.0, 0.0));\n" +
                        "    sum += texture2D(inTexture, uv + vec2(-halfPixel.x, halfPixel.y)) * 2.0;\n" +
                        "    sum += texture2D(inTexture, uv + vec2(0.0, halfPixel.y * 2.0));\n" +
                        "    sum += texture2D(inTexture, uv + vec2(halfPixel.x, halfPixel.y)) * 2.0;\n" +
                        "    sum += texture2D(inTexture, uv + vec2(halfPixel.x * 2.0, 0.0));\n" +
                        "    sum += texture2D(inTexture, uv + vec2(halfPixel.x, -halfPixel.y)) * 2.0;\n" +
                        "    sum += texture2D(inTexture, uv + vec2(0.0, -halfPixel.y * 2.0));\n" +
                        "    sum += texture2D(inTexture, uv + vec2(-halfPixel.x, -halfPixel.y)) * 2.0;\n" +
                        "    vec4 result = sum / 12.0;\n" +
                        "    gl_FragColor = vec4(result.rgb * color, result.a);\n" +
                        "}";

        String innerGlowFrag =
                "#version 120\n" +
                        "uniform sampler2D bloomTexture;\n" +
                        "uniform sampler2D maskTexture;\n" +
                        "uniform vec3 glowColor1;\n" +
                        "uniform vec3 glowColor2;\n" +
                        "void main() {\n" +
                        "    vec2 uv = gl_TexCoord[0].xy;\n" +
                        "    vec4 bloom = texture2D(bloomTexture, uv);\n" +
                        "    vec4 mask = texture2D(maskTexture, uv);\n" +
                        "    if (mask.a < 0.01) discard;\n" +
                        "    vec3 gradientColor = mix(glowColor1, glowColor2, uv.y);\n" +
                        "    float edgeGlow = 1.0 - bloom.a;\n" +
                        "    float baseIntensity = 0.5;\n" +
                        "    float edgeIntensity = edgeGlow * 0.5;\n" +
                        "    float totalIntensity = (baseIntensity + edgeIntensity) * mask.a;\n" +
                        "    gl_FragColor = vec4(gradientColor, totalIntensity * gl_Color.a);\n" +
                        "}";

        String outerGlowFrag =
                "#version 120\n" +
                        "uniform sampler2D bloomTexture;\n" +
                        "uniform sampler2D maskTexture;\n" +
                        "uniform vec3 glowColor1;\n" +
                        "uniform vec3 glowColor2;\n" +
                        "void main() {\n" +
                        "    vec2 uv = gl_TexCoord[0].xy;\n" +
                        "    vec4 bloom = texture2D(bloomTexture, uv);\n" +
                        "    vec4 mask = texture2D(maskTexture, uv);\n" +
                        "    float intensity = bloom.a * (1.0 - mask.a);\n" +
                        "    vec3 gradientColor = mix(glowColor1, glowColor2, uv.y);\n" +
                        "    gl_FragColor = vec4(gradientColor, intensity * gl_Color.a);\n" +
                        "}";

        kawaseDownProgram = createProgram(vert, kawaseDown);
        kawaseUpProgram = createProgram(vert, kawaseUp);
        innerGlowProgram = createProgram(vert, innerGlowFrag);
        outerGlowProgram = createProgram(vert, outerGlowFrag);

        cacheShaderUniforms();
    }

    private void cacheShaderUniforms() {
        kawaseDownTextureUniform =
                GL20.glGetUniformLocation(kawaseDownProgram, "inTexture");

        kawaseDownSizeUniform =
                GL20.glGetUniformLocation(kawaseDownProgram, "uSize");

        kawaseDownOffsetUniform =
                GL20.glGetUniformLocation(kawaseDownProgram, "uOffset");

        kawaseDownHalfPixelUniform =
                GL20.glGetUniformLocation(kawaseDownProgram, "uHalfPixel");

        kawaseUpTextureUniform =
                GL20.glGetUniformLocation(kawaseUpProgram, "inTexture");

        kawaseUpSizeUniform =
                GL20.glGetUniformLocation(kawaseUpProgram, "uSize");

        kawaseUpOffsetUniform =
                GL20.glGetUniformLocation(kawaseUpProgram, "uOffset");

        kawaseUpHalfPixelUniform =
                GL20.glGetUniformLocation(kawaseUpProgram, "uHalfPixel");

        kawaseUpColorUniform =
                GL20.glGetUniformLocation(kawaseUpProgram, "color");

        innerBloomTextureUniform =
                GL20.glGetUniformLocation(innerGlowProgram, "bloomTexture");

        innerMaskTextureUniform =
                GL20.glGetUniformLocation(innerGlowProgram, "maskTexture");

        innerGlowColor1Uniform =
                GL20.glGetUniformLocation(innerGlowProgram, "glowColor1");

        innerGlowColor2Uniform =
                GL20.glGetUniformLocation(innerGlowProgram, "glowColor2");

        outerBloomTextureUniform =
                GL20.glGetUniformLocation(outerGlowProgram, "bloomTexture");

        outerMaskTextureUniform =
                GL20.glGetUniformLocation(outerGlowProgram, "maskTexture");

        outerGlowColor1Uniform =
                GL20.glGetUniformLocation(outerGlowProgram, "glowColor1");

        outerGlowColor2Uniform =
                GL20.glGetUniformLocation(outerGlowProgram, "glowColor2");

        GL20.glUseProgram(kawaseDownProgram);
        setUniform1i(kawaseDownTextureUniform, 0);

        GL20.glUseProgram(kawaseUpProgram);
        setUniform1i(kawaseUpTextureUniform, 0);
        setUniform3f(kawaseUpColorUniform, 1.0f, 1.0f, 1.0f);

        GL20.glUseProgram(innerGlowProgram);
        setUniform1i(innerBloomTextureUniform, 0);
        setUniform1i(innerMaskTextureUniform, 1);

        GL20.glUseProgram(outerGlowProgram);
        setUniform1i(outerBloomTextureUniform, 0);
        setUniform1i(outerMaskTextureUniform, 1);

        GL20.glUseProgram(0);
    }

    private int createProgram(String vert, String frag) {
        int v = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(v, vert);
        GL20.glCompileShader(v);

        int f = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(f, frag);
        GL20.glCompileShader(f);

        int p = GL20.glCreateProgram();

        GL20.glAttachShader(p, v);
        GL20.glAttachShader(p, f);

        GL20.glLinkProgram(p);

        GL20.glDetachShader(p, v);
        GL20.glDetachShader(p, f);

        GL20.glDeleteShader(v);
        GL20.glDeleteShader(f);

        return p;
    }
}
