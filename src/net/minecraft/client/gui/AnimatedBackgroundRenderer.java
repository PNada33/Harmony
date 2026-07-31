package net.minecraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.MathHelper;
import xd.harm.utils.render.color.ColorUtils;
import xd.harm.utils.render.rect.RenderUtility;

import static org.lwjgl.opengl.GL20.*;

public final class AnimatedBackgroundRenderer {
    public static final float MAIN_MENU_PROGRESS = 0.18f;
    public static final float MAIN_MENU_EXIT_PROGRESS = 0.88f;

    private static int program = -1;
    private static boolean compileAttempted = false;
    private static int sizeUniform = -1;
    private static int timeUniform = -1;
    private static int alphaUniform = -1;
    private static int progressUniform = -1;
    private static int exitProgressUniform = -1;
    private static int themeColorUniform = -1;

    private static final String VERTEX_SHADER =
            "#version 120\n" +
                    "varying vec2 texCoord;\n" +
                    "void main() {\n" +
                    "    texCoord = gl_MultiTexCoord0.xy;\n" +
                    "    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;\n" +
                    "}\n";

    private static final String FRAGMENT_SHADER =
            "#version 120\n" +
                    "varying vec2 texCoord;\n" +
                    "uniform vec2 size;\n" +
                    "uniform float time;\n" +
                    "uniform float alpha;\n" +
                    "uniform float progress;\n" +
                    "uniform float exitProgress;\n" +
                    "uniform vec3 themeColor;\n" +
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
                    "void main() {\n" +
                    "    vec2 uv = texCoord;\n" +
                    "    vec2 p = (uv - 0.5) * vec2(size.x / max(size.y, 1.0), 1.0);\n" +
                    "    float t = time * 0.35;\n" +
                    "\n" +
                    "    float n1 = fbm(p * 2.3 + vec2(t * 0.8, -t * 0.4));\n" +
                    "    float n2 = fbm(p * 4.1 + vec2(-t * 1.2, t * 0.7));\n" +
                    "\n" +
                    "    float glowA = exp(-length(p - vec2(-0.30 + 0.16 * sin(time * 0.50), 0.02)) * 2.7);\n" +
                    "    float glowB = exp(-length(p - vec2(0.28 + 0.14 * cos(time * 0.42), -0.18)) * 3.0);\n" +
                    "\n" +
                    "    vec3 base = vec3(0.035, 0.040, 0.055);\n" +
                    "    vec3 cTheme = themeColor;\n" +
                    "    vec3 cThemeDim = themeColor * 0.6;\n" +
                    "\n" +
                    "    vec3 haze = cTheme * glowA * 0.22;\n" +
                    "    haze += cThemeDim * glowB * 0.20;\n" +
                    "    haze += cTheme * (n1 * 0.09 + n2 * 0.06);\n" +
                    "\n" +
                    "    float centerLift = exp(-pow((uv.y - 0.5) * 4.6, 2.0)) * 0.06;\n" +
                    "    float breathe = 0.90 + 0.10 * sin(time * 0.75);\n" +
                    "\n" +
                    "    vec3 color = base + haze * breathe + vec3(centerLift);\n" +
                    "    color += cTheme * (0.02 + progress * 0.02);\n" +
                    "\n" +
                    "    float vignette = smoothstep(1.20, 0.16, length((uv - 0.5) * vec2(1.35, 1.0)));\n" +
                    "    color *= 0.75 + 0.25 * vignette;\n" +
                    "\n" +
                    "    float grain = (hash21(uv * size + vec2(time * 73.0, time * 41.0)) - 0.5) * 0.02;\n" +
                    "    color += grain;\n" +
                    "    color *= 1.0 - exitProgress * 0.08;\n" +
                    "\n" +
                    "    gl_FragColor = vec4(color, alpha);\n" +
                    "}\n";

    private AnimatedBackgroundRenderer() {
    }

    public static void drawMainMenuStyle(Minecraft minecraft, int width, int height, float alpha, float time) {
        draw(minecraft, width, height, alpha, time, MAIN_MENU_PROGRESS, MAIN_MENU_EXIT_PROGRESS);
        int overlayAlpha = MathHelper.clamp((int) (176.0f * clamp01(alpha)), 0, 176);
        RenderUtility.drawRectW(0, 0, width, height, ColorUtils.rgba(4, 4, 5, overlayAlpha));
    }

    public static void draw(Minecraft minecraft, int width, int height, float alpha, float time, float progress, float exitProgress) {
        if (alpha <= 0.001f) {
            return;
        }

        ensureProgram();

        if (program == -1) {
            RenderUtility.drawRectW(0, 0, width, height, ColorUtils.rgba(8, 8, 12, MathHelper.clamp((int) (alpha * 255.0f), 0, 255)));
            return;
        }

        float scaleFactor = (float) minecraft.getMainWindow().getGuiScaleFactor();
        float pixelW = width * scaleFactor;
        float pixelH = height * scaleFactor;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        glUseProgram(program);
        glUniform2f(sizeUniform, pixelW, pixelH);
        glUniform1f(timeUniform, time);
        glUniform1f(alphaUniform, clamp01(alpha));
        glUniform1f(progressUniform, clamp01(progress));
        glUniform1f(exitProgressUniform, clamp01(exitProgress));

        int themeColor = ColorUtils.getColor(0);
        float tr = ColorUtils.getRed(themeColor) / 255.0f;
        float tg = ColorUtils.getGreen(themeColor) / 255.0f;
        float tb = ColorUtils.getBlue(themeColor) / 255.0f;
        glUniform3f(themeColorUniform, tr, tg, tb);

        org.lwjgl.opengl.GL11.glBegin(org.lwjgl.opengl.GL11.GL_QUADS);
        org.lwjgl.opengl.GL11.glTexCoord2f(0, 0);
        org.lwjgl.opengl.GL11.glVertex2f(0.0f, 0.0f);
        org.lwjgl.opengl.GL11.glTexCoord2f(0, 1);
        org.lwjgl.opengl.GL11.glVertex2f(0.0f, height);
        org.lwjgl.opengl.GL11.glTexCoord2f(1, 1);
        org.lwjgl.opengl.GL11.glVertex2f(width, height);
        org.lwjgl.opengl.GL11.glTexCoord2f(1, 0);
        org.lwjgl.opengl.GL11.glVertex2f(width, 0.0f);
        org.lwjgl.opengl.GL11.glEnd();

        glUseProgram(0);
    }

    private static void ensureProgram() {
        if (compileAttempted) {
            return;
        }

        compileAttempted = true;
        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        if (program != -1) {
            sizeUniform = glGetUniformLocation(program, "size");
            timeUniform = glGetUniformLocation(program, "time");
            alphaUniform = glGetUniformLocation(program, "alpha");
            progressUniform = glGetUniformLocation(program, "progress");
            exitProgressUniform = glGetUniformLocation(program, "exitProgress");
            themeColorUniform = glGetUniformLocation(program, "themeColor");
        }
    }

    private static int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexShader, vertexSource);
        glCompileShader(vertexShader);
        if (glGetShaderi(vertexShader, GL_COMPILE_STATUS) == 0) {
            glDeleteShader(vertexShader);
            return -1;
        }

        int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentShader, fragmentSource);
        glCompileShader(fragmentShader);
        if (glGetShaderi(fragmentShader, GL_COMPILE_STATUS) == 0) {
            glDeleteShader(vertexShader);
            glDeleteShader(fragmentShader);
            return -1;
        }

        int shaderProgram = glCreateProgram();
        glAttachShader(shaderProgram, vertexShader);
        glAttachShader(shaderProgram, fragmentShader);
        glLinkProgram(shaderProgram);

        if (glGetProgrami(shaderProgram, GL_LINK_STATUS) == 0) {
            glDeleteShader(vertexShader);
            glDeleteShader(fragmentShader);
            glDeleteProgram(shaderProgram);
            return -1;
        }

        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
        return shaderProgram;
    }

    private static float clamp01(float value) {
        return value < 0.0f ? 0.0f : Math.min(value, 1.0f);
    }
}
