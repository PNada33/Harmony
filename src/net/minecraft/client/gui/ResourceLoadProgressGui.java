package net.minecraft.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.function.Consumer;

import xd.harm.modules.impl.render.Theme;
import xd.harm.utils.render.color.ColorUtils;
import xd.harm.utils.render.rect.RenderUtility;
import xd.harm.utils.text.font.ClientFonts;
import xd.harm.utils.text.font.styled.StyledFont;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.resources.data.TextureMetadataSection;
import net.minecraft.resources.IAsyncReloader;
import net.minecraft.resources.IResourceManager;
import net.minecraft.resources.ResourcePackType;
import net.minecraft.resources.VanillaPack;
import net.minecraft.util.ColorHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

import static org.lwjgl.opengl.GL20.*;

public class ResourceLoadProgressGui extends LoadingGui {

    private static final ResourceLocation MOJANG_LOGO_TEXTURE = new ResourceLocation("textures/gui/title/mojangstudios.png");
    private static final int FIELD_COLOR_DEFAULT = ColorHelper.PackedColor.packColor(255, 239, 50, 61);
    private static final int FIELD_COLOR_DEFAULT_NO_ALPHA = FIELD_COLOR_DEFAULT & 16777215;

    private final Minecraft mc;
    private final IAsyncReloader asyncReloader;
    private final Consumer<Optional<Throwable>> completedCallback;
    private final boolean reloading;

    private float progress;
    private long fadeOutStart = -1L;
    private long fadeInStart = -1L;

    private int colorBackground = FIELD_COLOR_DEFAULT_NO_ALPHA;
    private boolean fadeOut = false;

    private final StyledFont fontPct;
    private final StyledFont fontTitle;

    private float smoothProgress = 0f;
    private float renderProgress = 0f;

    private long firstRenderTime = -1L;
    private boolean firstFrameRendered = false;
    private boolean loadingComplete = false;

    private int barProgram = -1;
    private boolean shadersCompiled = false;

    private static final String VERTEX_SHADER =
            "#version 120\n" +
                    "varying vec2 texCoord;\n" +
                    "void main() {\n" +
                    "    texCoord = gl_MultiTexCoord0.xy;\n" +
                    "    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;\n" +
                    "}\n";

    private static final String BAR_FRAGMENT_SHADER =
            "#version 120\n" +
                    "varying vec2 texCoord;\n" +
                    "uniform vec2 size;\n" +
                    "uniform float time;\n" +
                    "uniform float alpha;\n" +
                    "uniform float progress;\n" +
                    "uniform float radius;\n" +
                    "uniform float barHeight;\n" +
                    "uniform float exitProgress;\n" +
                    "\n" +
                    "float roundedBoxSdf(vec2 p, vec2 b, float r) {\n" +
                    "    vec2 d = abs(p) - b + vec2(r);\n" +
                    "    return length(max(d, 0.0)) - r;\n" +
                    "}\n" +
                    "\n" +
                    "float hash21(vec2 p) {\n" +
                    "    p = fract(p * vec2(123.34, 345.45));\n" +
                    "    p += dot(p, p + 34.345);\n" +
                    "    return fract(p.x * p.y);\n" +
                    "}\n" +
                    "\n" +
                    "float noise(vec2 p) {\n" +
                    "    vec2 i = floor(p);\n" +
                    "    vec2 f = fract(p);\n" +
                    "    float a = hash21(i);\n" +
                    "    float b = hash21(i + vec2(1.0, 0.0));\n" +
                    "    float c = hash21(i + vec2(0.0, 1.0));\n" +
                    "    float d = hash21(i + vec2(1.0, 1.0));\n" +
                    "    vec2 u = f * f * (3.0 - 2.0 * f);\n" +
                    "    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;\n" +
                    "}\n" +
                    "\n" +
                    "float fbm(vec2 p) {\n" +
                    "    float v = 0.0;\n" +
                    "    float a = 0.5;\n" +
                    "    for (int i = 0; i < 3; i++) {\n" +
                    "        v += noise(p) * a;\n" +
                    "        p = p * 2.0 + vec2(17.0, 31.0);\n" +
                    "        a *= 0.5;\n" +
                    "    }\n" +
                    "    return v;\n" +
                    "}\n" +
                    "\n" +
                    "vec3 helloPalette(float t) {\n" +
                    "    vec3 c0 = vec3(1.00, 0.64, 0.48);\n" +
                    "    vec3 c1 = vec3(1.00, 0.84, 0.44);\n" +
                    "    vec3 c2 = vec3(0.46, 0.86, 1.00);\n" +
                    "    vec3 c3 = vec3(0.78, 0.62, 1.00);\n" +
                    "    vec3 c01 = mix(c0, c1, smoothstep(0.00, 0.35, t));\n" +
                    "    vec3 c12 = mix(c1, c2, smoothstep(0.25, 0.70, t));\n" +
                    "    vec3 c23 = mix(c2, c3, smoothstep(0.60, 1.00, t));\n" +
                    "    vec3 blend = mix(c01, c12, smoothstep(0.15, 0.75, t));\n" +
                    "    return mix(blend, c23, smoothstep(0.55, 1.00, t));\n" +
                    "}\n" +
                    "\n" +
                    "void main() {\n" +
                    "    vec2 uv = texCoord;\n" +
                    "    vec2 pixel = uv * size;\n" +
                    "    float centerY = size.y * 0.5;\n" +
                    "    float halfBar = barHeight * 0.5;\n" +
                    "\n" +
                    "    float collapseX = mix(1.0, 0.0, exitProgress * exitProgress);\n" +
                    "    float barWidthNow = max(size.x * collapseX, 1.0);\n" +
                    "    float barStartX = (size.x - barWidthNow) * 0.5;\n" +
                    "    float fillEndX = barStartX + barWidthNow * clamp(progress, 0.0, 1.0);\n" +
                    "\n" +
                    "    vec2 local = vec2(pixel.x - size.x * 0.5, pixel.y - centerY);\n" +
                    "    float dist = roundedBoxSdf(local, vec2(barWidthNow * 0.5, halfBar), radius);\n" +
                    "    float barMask = smoothstep(0.7, -0.7, dist);\n" +
                    "\n" +
                    "    float fillMask = smoothstep(fillEndX + 0.8, fillEndX - 0.8, pixel.x) * step(barStartX, pixel.x);\n" +
                    "    fillMask *= barMask;\n" +
                    "\n" +
                    "    float localX = clamp((pixel.x - barStartX) / barWidthNow, 0.0, 1.0);\n" +
                    "    float localY = clamp((pixel.y - (centerY - halfBar)) / max(barHeight, 1.0), 0.0, 1.0);\n" +
                    "\n" +
                    "    vec3 track = mix(vec3(0.09, 0.10, 0.13), vec3(0.14, 0.15, 0.19), localY);\n" +
                    "    float trackSpec = exp(-pow((localY - 0.12) * 8.0, 2.0)) * 0.22;\n" +
                    "    track += vec3(trackSpec);\n" +
                    "\n" +
                    "    float flowNoise = fbm(vec2(localX * 3.6 - time * 0.52, localY * 2.7 + time * 0.21));\n" +
                    "    float wave1 = sin((localX * 11.0 + localY * 4.0) - time * 2.9) * 0.5 + 0.5;\n" +
                    "    float wave2 = sin((localX * 1.8 - localY * 0.6) * 13.0 + time * 1.7) * 0.5 + 0.5;\n" +
                    "    float paletteT = clamp(localX * 0.75 + flowNoise * 0.15 + wave1 * 0.08 + wave2 * 0.08, 0.0, 1.0);\n" +
                    "    vec3 fill = helloPalette(paletteT);\n" +
                    "\n" +
                    "    float glassTop = exp(-pow((localY - 0.15) * 8.5, 2.0)) * 0.36;\n" +
                    "    fill += vec3(1.0, 0.98, 0.96) * glassTop;\n" +
                    "\n" +
                    "    float sweepPos = fract(time * 0.24);\n" +
                    "    float sweep = exp(-pow((localX - sweepPos) / 0.09, 2.0));\n" +
                    "    fill += vec3(1.0, 0.98, 0.94) * sweep * fillMask * (1.0 - exitProgress) * 0.58;\n" +
                    "\n" +
                    "    float edgeDist = abs(pixel.x - fillEndX);\n" +
                    "    float edgeVertical = exp(-abs(pixel.y - centerY) / max(barHeight, 1.0) * 1.25);\n" +
                    "    float edgeGlow = exp(-edgeDist * 0.23) * edgeVertical;\n" +
                    "    edgeGlow *= step(0.01, progress) * step(progress, 0.995) * (1.0 - exitProgress);\n" +
                    "    vec3 edgeColor = vec3(1.0, 0.97, 0.88);\n" +
                    "\n" +
                    "    float outsideMask = 1.0 - barMask;\n" +
                    "    float aura = exp(-max(dist, 0.0) * 0.34);\n" +
                    "    aura *= smoothstep(barStartX - 10.0, barStartX + 12.0, pixel.x);\n" +
                    "    aura *= 1.0 - smoothstep(fillEndX + 3.0, fillEndX + 16.0, pixel.x);\n" +
                    "    float auraPulse = 0.82 + 0.18 * sin(time * 2.3 + localX * 6.2831);\n" +
                    "    vec3 auraColor = mix(vec3(0.64, 0.90, 1.0), vec3(1.0, 0.72, 0.58), 0.45 + 0.35 * sin(time * 0.65));\n" +
                    "    float auraAlpha = aura * auraPulse * 0.18 * alpha * outsideMask;\n" +
                    "\n" +
                    "    vec3 barColor = mix(track, fill, fillMask);\n" +
                    "    float grain = (hash21(pixel + vec2(time * 67.0, time * 47.0)) - 0.5) * 0.02;\n" +
                    "    barColor += grain;\n" +
                    "    float barAlpha = barMask * alpha;\n" +
                    "\n" +
                    "    float edgeAlpha = edgeGlow * 0.46 * alpha;\n" +
                    "\n" +
                    "    vec3 finalColor = barColor * barAlpha + edgeColor * edgeAlpha + auraColor * auraAlpha;\n" +
                    "    float finalAlpha = barAlpha + edgeAlpha + auraAlpha;\n" +
                    "    finalAlpha = clamp(finalAlpha, 0.0, 1.0);\n" +
                    "\n" +
                    "    gl_FragColor = vec4(finalColor / max(finalAlpha, 0.001), finalAlpha);\n" +
                    "}\n";

    public ResourceLoadProgressGui(Minecraft mc, IAsyncReloader asyncReloader, Consumer<Optional<Throwable>> completedCallback, boolean reloading) {
        this.mc = mc;
        this.asyncReloader = asyncReloader;
        this.completedCallback = completedCallback;
        this.reloading = reloading;
        this.fontPct = ClientFonts.interMedium[12];
        this.fontTitle = ClientFonts.interMedium[18];
    }

    public static void loadLogoTexture(Minecraft mc) {
        mc.getTextureManager().loadTexture(MOJANG_LOGO_TEXTURE, new MojangLogoTexture());
    }

    private void compileShaders() {
        if (shadersCompiled) return;
        shadersCompiled = true;
        barProgram = createProgram(VERTEX_SHADER, BAR_FRAGMENT_SHADER);
    }

    private int createProgram(String vertexSource, String fragmentSource) {
        int vert = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vert, vertexSource);
        glCompileShader(vert);
        if (glGetShaderi(vert, GL_COMPILE_STATUS) == 0) {
            glDeleteShader(vert);
            return -1;
        }

        int frag = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(frag, fragmentSource);
        glCompileShader(frag);
        if (glGetShaderi(frag, GL_COMPILE_STATUS) == 0) {
            glDeleteShader(vert);
            glDeleteShader(frag);
            return -1;
        }

        int program = glCreateProgram();
        glAttachShader(program, vert);
        glAttachShader(program, frag);
        glLinkProgram(program);

        if (glGetProgrami(program, GL_LINK_STATUS) == 0) {
            glDeleteShader(vert);
            glDeleteShader(frag);
            glDeleteProgram(program);
            return -1;
        }

        glDeleteShader(vert);
        glDeleteShader(frag);
        return program;
    }

    private long getAnimTime(long now) {
        if (firstRenderTime == -1L) return 0;
        return now - firstRenderTime;
    }

    @Override
    public void render(MatrixStack ms, int mx, int my, float pt) {
        int sw = mc.getMainWindow().getScaledWidth();
        int sh = mc.getMainWindow().getScaledHeight();
        long now = Util.milliTime();

        if (!firstFrameRendered) {
            firstFrameRendered = true;
            firstRenderTime = now;
        }

        float spd = asyncReloader.estimateExecutionSpeed();
        progress = MathHelper.clamp(progress * 0.95F + spd * 0.05F, 0.0F, 1.0F);
        smoothProgress += (progress - smoothProgress) * 0.025f;
        renderProgress += (smoothProgress - renderProgress) * 0.04f;

        if (reloading && (asyncReloader.asyncPartDone() || mc.currentScreen != null) && fadeInStart == -1L)
            fadeInStart = now;

        float fiP = fadeInStart > -1L ? (now - fadeInStart) / 500f : -1f;

        long animTime = getAnimTime(now);
        float t = animTime / 1000f;
        float entAlpha = easeOutCubic(clamp01(animTime / 400f));

        float exitProgress = fadeOutStart > -1L ? clamp01((now - fadeOutStart) / 1000f) : 0f;
        float bgExitProgress = fadeOutStart > -1L ? clamp01((now - fadeOutStart - 400f) / 800f) : 0f;

        float extAlpha = 1f - easeInOutQuart(exitProgress);
        float bgAlpha = 1f - easeInOutQuart(bgExitProgress);

        drawBg(sw, sh, bgAlpha, t, renderProgress, exitProgress);

        float contentAlpha = entAlpha * extAlpha;
        drawContent(ms, sw, sh, t, contentAlpha, exitProgress);

        if (fadeOutStart > -1L && bgExitProgress >= 1.0f) {
            fadeOut = true;
            mc.setLoadingGui(null);
        }

        if (fadeOutStart == -1L && !loadingComplete && asyncReloader.fullyDone() && (!reloading || fiP >= 2.0F)) {
            loadingComplete = true;

            try {
                asyncReloader.join();
                completedCallback.accept(Optional.empty());
            } catch (Throwable error) {
                completedCallback.accept(Optional.of(error));
            }

            if (mc.currentScreen != null) {
                mc.currentScreen.init(mc, sw, sh);
            }

            fadeOutStart = Util.milliTime();
        }
    }

    private void drawBg(int w, int h, float alpha, float t, float progress, float exitProgress) {
        AnimatedBackgroundRenderer.drawMainMenuStyle(mc, w, h, alpha, t);
    }

    private void drawContent(MatrixStack ms, int sw, int sh, float t, float alpha, float exitProgress) {
        if (alpha <= 0.001f) return;

        float cx = sw / 2f;
        float cy = sh / 2f;

        float entOff = (1f - easeOutCubic(clamp01(t / 0.5f))) * 22f;
        float exitEased = easeInOutQuart(exitProgress);
        float extScale = 1f + 0.16f * exitEased;
        float ringCy = cy + entOff - 28f * exitEased;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        RenderSystem.pushMatrix();
        RenderSystem.translatef(cx, ringCy, 0.0F);
        RenderSystem.scalef(extScale, extScale, 1.0F);
        RenderSystem.translatef(-cx, -ringCy, 0.0F);

        drawDonutProgress(ms, cx, ringCy, alpha, t, exitEased);
        RenderSystem.popMatrix();

        RenderSystem.disableBlend();
    }

    private void drawDonutProgress(MatrixStack ms, float cx, float cy, float alpha, float t, float exitEased) {
        float enter = easeOutCubic(clamp01(t / 0.45f));
        float pctAlpha = alpha * (1f - exitEased);
        if (pctAlpha <= 0.01f) {
            return;
        }

        float radius = 43.0f * enter;
        float thickness = 1.0f * enter;
        if (radius < 2.0f || thickness < 1.0f) {
            return;
        }

        float progressValue = clamp01(renderProgress);
        float endAngle = -90.0f + 360.0f * progressValue;
        float rotate = t * 56.0f;
        int baseAlpha = (int) (pctAlpha * 255.0f);
        int themeColor = ColorUtils.setAlpha(Theme.MainColor((int) (t * 120.0f)), baseAlpha);
        int themeTrackColor = ColorUtils.setAlpha(ColorUtils.darker(themeColor, 0.46f), (int) (baseAlpha * 0.72f));

        RenderUtility.drawRingArcAA(cx, cy, radius + 3.0f, thickness + 5.0f, 0.0f, 360.0f,
                ColorUtils.rgba(15, 18, 27, (int) (baseAlpha * 0.38f)), 1.4f);
        RenderUtility.drawRingArcAA(cx, cy, radius, thickness, 0.0f, 360.0f,
                themeTrackColor, 1.1f);

        if (progressValue > 0.001f) {
            RenderUtility.drawRingArcAAWithRotatingGradient(cx, cy, radius, thickness,
                    -90.0f, endAngle, themeColor, 1.2f, rotate);
        }

        if (fontTitle == null) return;
        int percent = MathHelper.clamp((int) (progressValue * 100.0f), 0, 100);
        String percentText = percent + "%";
        float textY = cy - fontTitle.getFontHeight() / 4.0f;
        fontTitle.drawCenteredString(ms, percentText, cx, textY,
                ColorUtils.rgba(245, 248, 255, (int) (baseAlpha * 0.94f)));
    }

    private float clamp01(float v) {
        return v < 0 ? 0 : Math.min(v, 1);
    }

    private float easeOutCubic(float t) {
        return 1f - (1f - t) * (1f - t) * (1f - t);
    }

    private float easeInCubic(float t) {
        return t * t * t;
    }

    private float easeInOutQuart(float t) {
        return t < 0.5f ? 8f * t * t * t * t : 1f - (float)Math.pow(-2f * t + 2f, 4) / 2f;
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    public void update() {
        colorBackground = FIELD_COLOR_DEFAULT_NO_ALPHA;
    }

    public boolean isFadeOut() {
        return fadeOut;
    }

    @Override
    protected void resetProgressAndMessage() {
        progress = 0.0F;
        fadeOut = false;
        firstRenderTime = -1L;
        firstFrameRendered = false;
        fadeOutStart = -1L;
        fadeInStart = -1L;
        smoothProgress = 0f;
        renderProgress = 0f;
        loadingComplete = false;
    }

    static class MojangLogoTexture extends SimpleTexture {
        public MojangLogoTexture() {
            super(MOJANG_LOGO_TEXTURE);
        }

        @Override
        protected TextureData getTextureData(IResourceManager resourceManager) {
            Minecraft mc = Minecraft.getInstance();
            VanillaPack vanillaPack = mc.getPackFinder().getVanillaPack();
            try (InputStream is = getLogoInputStream(resourceManager, vanillaPack)) {
                return new TextureData(new TextureMetadataSection(true, true), NativeImage.read(is));
            } catch (IOException e) {
                return new TextureData(e);
            }
        }

        private static InputStream getLogoInputStream(IResourceManager rm, VanillaPack vp) throws IOException {
            return rm.hasResource(MOJANG_LOGO_TEXTURE)
                    ? rm.getResource(MOJANG_LOGO_TEXTURE).getInputStream()
                    : vp.getResourceStream(ResourcePackType.CLIENT_RESOURCES, MOJANG_LOGO_TEXTURE);
        }
    }
}
