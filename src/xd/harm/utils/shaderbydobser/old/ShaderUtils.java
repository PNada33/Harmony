package xd.harm.utils.shaderbydobser.old;

import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import xd.harm.utils.render.rect.RenderUtility;
import xd.harm.utils.client.IMinecraft;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.ARBShaderObjects.*;
import static org.lwjgl.opengl.GL20.*;

public class ShaderUtils implements IMinecraft {

    private final int programID;
    private final Map<String, Integer> uniformLocationCache = new HashMap<>();

    public static ShaderUtils
            CORNER_ROUND_SHADER,
            CORNER_ROUND_SHADER_TEXTURE,
            GLOW_ROUND_SHADER,
            GRADIENT_ROUND_SHADER,
            TEXTURE_ROUND_SHADER,
            ROUND_SHADER_OUTLINE,
            GRADIENT_MASK_SHADER,
            ROUND_SHADER;

    public ShaderUtils(String fragmentShaderLoc) {
        programID = ARBShaderObjects.glCreateProgramObjectARB();

        try {
            int fragmentShaderID;
            switch (fragmentShaderLoc) {
                case "corner":
                    fragmentShaderID = createShader(new ByteArrayInputStream(roundedCornerRect.getBytes()), GL_FRAGMENT_SHADER);
                    break;
                case "cornerGradient":
                    fragmentShaderID = createShader(new ByteArrayInputStream(roundedCornerRectGradient.getBytes()), GL_FRAGMENT_SHADER);
                    break;
                case "noise":
                    fragmentShaderID = createShader(new ByteArrayInputStream(("uniform sampler2D u_texture;\n" +
                            "uniform float u_value;\n" +
                            "#define NOISE .5/255.0\n" +
                            "\n" +
                            "float random(vec2 st) {\n" +
                            "    return fract(sin(dot(st.xy, vec2(12.9898, 78.233))) * 43758.5453);\n" +
                            "}\n" +
                            "\n" +
                            "void main() {\n" +
                            "    vec2 st = gl_TexCoord[0].st;\n" +
                            "\n" +
                            "    vec4 color = texture2D(u_texture, st);\n" +
                            "\n" +
                            "    float noise = (sin(st.x) * cos(st.y)) * random(st);\n" +
                            "\n" +
                            "    color.rgb -= vec3(noise / u_value);\n" +
                            "    gl_FragColor = color;\n" +
                            "}\n").getBytes()), GL_FRAGMENT_SHADER);
                    break;
                case "blur2":
                    fragmentShaderID = createShader(new ByteArrayInputStream(("#version 120\n" +
                            "\n" +
                            " uniform sampler2D textureIn, textureToCheck;\n" +
                            " uniform vec2 texelSize, direction;\n" +
                            " uniform float exposure, radius;\n" +
                            " uniform float weights[256];\n" +
                            " uniform bool avoidTexture;\n" +
                            "\n" +
                            " #define offset direction * texelSize\n" +
                            "\n" +
                            " void main() {\n" +
                            "     if (direction.y >= 1 && avoidTexture) {\n" +
                            "         if (texture2D(textureToCheck, gl_TexCoord[0].st).a != 0.0) discard;\n" +
                            "     }\n" +
                            "     vec4 innerAlpha = texture2D(textureIn, gl_TexCoord[0].st);\n" +
                            "     innerAlpha *= innerAlpha.a;\n" +
                            "     innerAlpha *= weights[0];\n" +
                            "\n" +
                            "\n" +
                            "\n" +
                            "     for (float r = 1.0; r <= radius; r ++) {\n" +
                            "         vec4 colorCurrent1 = texture2D(textureIn, gl_TexCoord[0].st + offset * r);\n" +
                            "         vec4 colorCurrent2 = texture2D(textureIn, gl_TexCoord[0].st - offset * r);\n" +
                            "\n" +
                            "         colorCurrent1.rgb *= colorCurrent1.a;\n" +
                            "         colorCurrent2.rgb *= colorCurrent2.a;\n" +
                            "\n" +
                            "         innerAlpha += (colorCurrent1 + colorCurrent2) * weights[int(r)];\n" +
                            "\n" +
                            "     }\n" +
                            "\n" +
                            "     gl_FragColor = vec4(innerAlpha.rgb / innerAlpha.a, mix(innerAlpha.a, 1.0 - exp(-innerAlpha.a * exposure), step(0.0, direction.y)));\n" +
                            " }\n").getBytes()), GL_FRAGMENT_SHADER);
                    break;
                case "blur2c":
                    fragmentShaderID = createShader(new ByteArrayInputStream(("#version 120\n" +
                            "\n" +
                            " uniform vec4 color;\n" +
                            " uniform sampler2D textureIn, textureToCheck;\n" +
                            " uniform vec2 texelSize, direction;\n" +
                            " uniform float exposure, radius;\n" +
                            " uniform float weights[256];\n" +
                            " uniform bool avoidTexture;\n" +
                            "\n" +
                            " #define offset direction * texelSize\n" +
                            "\n" +
                            " void main() {\n" +
                            "     if (direction.y >= 1 && avoidTexture) {\n" +
                            "         if (texture2D(textureToCheck, gl_TexCoord[0].st).a != 0.0) discard;\n" +
                            "     }\n" +
                            "     vec4 innerAlpha = texture2D(textureIn, gl_TexCoord[0].st);\n" +
                            "     innerAlpha *= innerAlpha.a;\n" +
                            "     innerAlpha *= weights[0];\n" +
                            "\n" +
                            "\n" +
                            "\n" +
                            "     for (float r = 1.0; r <= radius; r ++) {\n" +
                            "         vec4 colorCurrent1 = texture2D(textureIn, gl_TexCoord[0].st + offset * r);\n" +
                            "         vec4 colorCurrent2 = texture2D(textureIn, gl_TexCoord[0].st - offset * r);\n" +
                            "\n" +
                            "         colorCurrent1.rgb *= colorCurrent1.a;\n" +
                            "         colorCurrent2.rgb *= colorCurrent2.a;\n" +
                            "\n" +
                            "         innerAlpha += (colorCurrent1 + colorCurrent2) * weights[int(r)];\n" +
                            "\n" +
                            "     }\n" +
                            "\n" +
                            "     gl_FragColor = vec4(color.rgb, mix(innerAlpha.a, 1.0 - exp(-innerAlpha.a * exposure), step(0.0, direction.y)));\n" +
                            " }\n").getBytes()), GL_FRAGMENT_SHADER);
                    break;
                case "cornerTex":
                    fragmentShaderID = createShader(new ByteArrayInputStream(roundedCornerRectTexture.getBytes()), GL_FRAGMENT_SHADER);
                    break;
                case "round":
                    fragmentShaderID = createShader(new ByteArrayInputStream(roundedRect.getBytes()), GL_FRAGMENT_SHADER);
                    break;
                case "fog":
                    fragmentShaderID = createShader(new ByteArrayInputStream(fog.getBytes()), GL_FRAGMENT_SHADER);
                    break;
                case "glow":
                    fragmentShaderID = createShader(new ByteArrayInputStream(glowRect.getBytes()), GL_FRAGMENT_SHADER);
                    break;
                case "out":
                    fragmentShaderID = createShader(new ByteArrayInputStream(roundedOut.getBytes()), GL_FRAGMENT_SHADER);
                    break;
                case "up":
                    fragmentShaderID = createShader(new ByteArrayInputStream(kawaseUpBloom.getBytes()), GL_FRAGMENT_SHADER);
                    break;
                case "down":
                    fragmentShaderID = createShader(new ByteArrayInputStream(kawaseDownBloom.getBytes()), GL_FRAGMENT_SHADER);
                    break;
                case "bloom":
                    fragmentShaderID = createShader(new ByteArrayInputStream(bloom.getBytes()), GL_FRAGMENT_SHADER);
                    break;
                case "shadow":
                    fragmentShaderID = createShader(new ByteArrayInputStream(("#version 120\n" +
                            "\n" +
                            "uniform sampler2D sampler1;\n" +
                            "uniform sampler2D sampler2;\n" +
                            "uniform vec2 texelSize;\n" +
                            "uniform vec2 direction;\n" +
                            "uniform float radius;\n" +
                            "uniform float kernel[256];\n" +
                            "\n" +
                            "void main(void)\n" +
                            "{\n" +
                            "    vec2 uv = gl_TexCoord[0].st;\n" +
                            "    vec4 pixel_color = texture2D(sampler1, uv);\n" +
                            "    pixel_color.rgb *= pixel_color.a;\n" +
                            "    pixel_color *= kernel[0];\n" +
                            "\n" +
                            "    for (float f = 1; f <= radius; f++) {\n" +
                            "        vec2 offset = f * texelSize * direction;\n" +
                            "        vec4 left = texture2D(sampler1, uv - offset);\n" +
                            "        vec4 right = texture2D(sampler1, uv + offset);\n" +
                            "        left.rgb *= left.a;\n" +
                            "        right.rgb *= right.a;\n" +
                            "        pixel_color += (left + right) * kernel[int(f)];\n" +
                            "    }\n" +
                            "\n" +
                            "    gl_FragColor = vec4(pixel_color.rgb / pixel_color.a, pixel_color.a);\n" +
                            "}\n").getBytes()), GL_FRAGMENT_SHADER);
                    break;
                case "texture":
                    fragmentShaderID = createShader(new ByteArrayInputStream(texture.getBytes()), GL_FRAGMENT_SHADER);
                    break;
                case "outline":
                    fragmentShaderID = createShader(new ByteArrayInputStream(outline.getBytes()), GL_FRAGMENT_SHADER);
                    break;
                case "outlineC":
                    fragmentShaderID = createShader(new ByteArrayInputStream(colorOut.getBytes()), GL_FRAGMENT_SHADER);
                    break;
                case "gradientMask":
                    fragmentShaderID = createShader(new ByteArrayInputStream(gradientMask.getBytes()), GL_FRAGMENT_SHADER);
                    break;
                case "blur":
                    fragmentShaderID = createShader(new ByteArrayInputStream(("#version 120\n" +
                            "\n" +
                            "uniform sampler2D textureIn;\n" +
                            "uniform sampler2D textureOut;\n" +
                            "uniform vec2 texelSize, direction;\n" +
                            "uniform float radius, weights[256];\n" +
                            "\n" +
                            "#define offset texelSize * direction\n" +
                            "\n" +
                            "void main() {\n" +
                            "    vec2 uv = gl_TexCoord[0].st;\n" +
                            "    uv.y = 1.0f - uv.y;\n" +
                            "\n" +
                            "    float alpha = texture2D(textureOut, uv).a;\n" +
                            "    if (direction.x == 0.0 && alpha == 0.0) {\n" +
                            "        discard;\n" +
                            "    }\n" +
                            "\n" +
                            "    vec3 color = texture2D(textureIn, gl_TexCoord[0].st).rgb * weights[0];\n" +
                            "    float totalWeight = weights[0];\n" +
                            "\n" +
                            "    for (float f = 1.0; f <= radius; f++) {\n" +
                            "        color += texture2D(textureIn, gl_TexCoord[0].st + f * offset).rgb * (weights[int(abs(f))]);\n" +
                            "        color += texture2D(textureIn, gl_TexCoord[0].st - f * offset).rgb * (weights[int(abs(f))]);\n" +
                            "\n" +
                            "        totalWeight += (weights[int(abs(f))]) * 2.0;\n" +
                            "    }\n" +
                            "\n" +
                            "    gl_FragColor = vec4(color / totalWeight, 1.0);\n" +
                            "}\n").getBytes()), GL_FRAGMENT_SHADER);
                    break;
                case "alphaMask":
                    fragmentShaderID = createShader(new ByteArrayInputStream(alphaMask.getBytes()), GL_FRAGMENT_SHADER);
                    break;
                case "roundRectOutline":
                    fragmentShaderID = createShader(new ByteArrayInputStream(roundRectOutline.getBytes()), GL_FRAGMENT_SHADER);
                    break;
                case "gradientRoundRect":
                    fragmentShaderID = createShader(new ByteArrayInputStream(gradientRoundRect.getBytes()), GL_FRAGMENT_SHADER);
                    break;
                default:
                    fragmentShaderID = createShader(mc.getResourceManager().getResource(new ResourceLocation("cordelia/shader/" + fragmentShaderLoc)).getInputStream(), GL_FRAGMENT_SHADER);
                    break;
            }
            ARBShaderObjects.glAttachObjectARB(programID, fragmentShaderID);
            ARBShaderObjects.glAttachObjectARB(programID, createShader(new ByteArrayInputStream(vertex.getBytes()), GL_VERTEX_SHADER));
            ARBShaderObjects.glLinkProgramARB(programID);
        } catch (IOException exception) {
            exception.fillInStackTrace();
            System.out.println("Ошибка при загрузке: " + fragmentShaderLoc);
        }
    }

    public static void init() {
        CORNER_ROUND_SHADER = new ShaderUtils("corner");
        CORNER_ROUND_SHADER_TEXTURE = new ShaderUtils("cornerTex");
        GLOW_ROUND_SHADER = new ShaderUtils("glow");
        TEXTURE_ROUND_SHADER = new ShaderUtils("texture");
        ROUND_SHADER = new ShaderUtils("round");
        GRADIENT_ROUND_SHADER = new ShaderUtils("gradientRoundRect");
        ROUND_SHADER_OUTLINE = new ShaderUtils("roundRectOutline");
        GRADIENT_MASK_SHADER = new ShaderUtils("gradientMask");
    }

    public int getUniform(String name) {
        Integer location = uniformLocationCache.get(name);

        if (location == null) {
            location = ARBShaderObjects.glGetUniformLocationARB(programID, name);
            uniformLocationCache.put(name, location);
        }

        return location;
    }

    public static float calculateGaussianValue(float x, float sigma) {
        double PI = 3.141592653;
        double output = 1.0 / Math.sqrt(2.0 * PI * (sigma * sigma));
        return (float) (output * Math.exp(-(x * x) / (2.0 * (sigma * sigma))));
    }

    public void attach() {
        ARBShaderObjects.glUseProgramObjectARB(programID);
    }

    public void detach() {
        glUseProgram(0);
    }

    public void setUniform(String name, float... args) {
        int loc = getUniform(name);
        switch (args.length) {
            case 1: {
                ARBShaderObjects.glUniform1fARB(loc, args[0]);
                break;
            }
            case 2: {
                ARBShaderObjects.glUniform2fARB(loc, args[0], args[1]);
                break;
            }
            case 3: {
                ARBShaderObjects.glUniform3fARB(loc, args[0], args[1], args[2]);
                break;
            }
            case 4: {
                ARBShaderObjects.glUniform4fARB(loc, args[0], args[1], args[2], args[3]);
                break;
            }
            default: {
                throw new IllegalArgumentException("Недопустимое количество аргументов для uniform '" + name + "'");
            }
        }
    }
    public void setUniformi(String name, int... args) {
        int loc = getUniform(name);
        switch (args.length) {
            case 1 -> glUniform1i(loc, args[0]);
            case 2 -> glUniform2i(loc, args[0], args[1]);
            case 3 -> glUniform3i(loc, args[0], args[1], args[2]);
            case 4 -> glUniform4i(loc, args[0], args[1], args[2], args[3]);
        }
    }

    public void setUniformf(String var1, double... var2) {
        int var3 = getUniform(var1);
        switch (var2.length) {
            case 1 -> ARBShaderObjects.glUniform1fARB(var3, (float) var2[0]);
            case 2 -> ARBShaderObjects.glUniform2fARB(var3, (float) var2[0], (float) var2[1]);
            case 3 -> ARBShaderObjects.glUniform3fARB(var3, (float) var2[0], (float) var2[1], (float) var2[2]);
            case 4 -> ARBShaderObjects.glUniform4fARB(var3, (float) var2[0], (float) var2[1], (float) var2[2],
                    (float) var2[3]);
        }
    }
    public void setUniform(String name, int... args) {
        int loc = getUniform(name);
        switch (args.length) {
            case 1: {
                glUniform1iARB(loc, args[0]);
                break;
            }
            case 2: {
                glUniform2iARB(loc, args[0], args[1]);
            }
            case 3: {
                glUniform3iARB(loc, args[0], args[1], args[2]);
                break;
            }
            case 4: {
                glUniform4iARB(loc, args[0], args[1], args[2], args[3]);
            }
            default: {
                throw new IllegalArgumentException("Недопустимое количество аргументов для uniform '" + name + "'");
            }
        }
    }

    public void setUniformf(String var1, float... var2) {
        int var3 = getUniform(var1);
        switch (var2.length) {
            case 1: {
                ARBShaderObjects.glUniform1fARB(var3, var2[0]);
                break;
            }
            case 2: {
                ARBShaderObjects.glUniform2fARB(var3, var2[0], var2[1]);
                break;
            }
            case 3: {
                ARBShaderObjects.glUniform3fARB(var3, var2[0], var2[1], var2[2]);
                break;
            }
            case 4: {
                ARBShaderObjects.glUniform4fARB(var3, var2[0], var2[1], var2[2], var2[3]);
                break;
            }
        }
    }

    public static void setupRoundedRectUniforms(float x, float y, float width, float height, float radius, ShaderUtils roundedTexturedShader) {
        roundedTexturedShader.setUniform("location", (float) (x * 2), (float) ((window.getHeight() - (height * 2)) - (y * 2)));
        roundedTexturedShader.setUniform("rectSize", (float) (width * 2), (float) (height * 2));
        roundedTexturedShader.setUniform("radius", (float) (radius * 2));
    }

    public static Framebuffer createFrameBuffer(Framebuffer framebuffer) {
        if (framebuffer == null || framebuffer.framebufferWidth != mc.getMainWindow().getWidth() || framebuffer.framebufferHeight != mc.getMainWindow().getHeight()) {
            if (framebuffer != null) {
                framebuffer.deleteFramebuffer();
            }
            return new Framebuffer(Math.max(mc.getMainWindow().getWidth(), 1), Math.max(mc.getMainWindow().getHeight(), 1), true, false);
        }
        return framebuffer;
    }

    public static void update(Framebuffer framebuffer) {
        if (framebuffer.framebufferWidth != mc.getMainWindow().getWidth() || framebuffer.framebufferHeight != mc.getMainWindow().getHeight()) {
            framebuffer.createBuffers(mc.getMainWindow().getWidth(), mc.getMainWindow().getHeight(), false);
        }
    }

    public void drawQuads(final float x, final float y, final float width, final float height) {
        RenderUtility.quadsBegin(x, y, width, height, GL_QUADS);
    }

    public static void drawQuads() {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 1);
        GL11.glVertex2f(0, 0);
        GL11.glTexCoord2f(0, 0);
        GL11.glVertex2f(0, Math.max(window.getScaledHeight(), 1));
        GL11.glTexCoord2f(1, 0);
        GL11.glVertex2f(Math.max(window.getScaledWidth(), 1), Math.max(window.getScaledHeight(), 1));
        GL11.glTexCoord2f(1, 1);
        GL11.glVertex2f(Math.max(window.getScaledWidth(), 1), 0);
        GL11.glEnd();
    }

    private int createShader(InputStream inputStream, int shaderType) {
        int shader = ARBShaderObjects.glCreateShaderObjectARB(shaderType);
        glShaderSourceARB(shader, FileUtil.readInputStream(inputStream));
        ARBShaderObjects.glCompileShaderARB(shader);
        if (GL20.glGetShaderi(shader, 35713) == 0) {
            System.out.println(GL20.glGetShaderInfoLog(shader, 4096));
            throw new IllegalStateException(String.format("Shader (%s) failed to compile!", shaderType));
        }
        return shader;
    }

    private final String roundedOut = "#version 120\n" +
            "\n" +
            "uniform vec2 size;\n" +
            "uniform vec4 round\n;" +
            "uniform vec2 smoothness\n;" +
            "uniform float value\n;" +
            "uniform vec4 color\n;" +
            "uniform float outlineSize\n;" +
            "uniform vec4 outlineColor\n;" +
            "\n" +

            "float test(vec2 vec_1, vec2 vec_2, vec4 vec_4) {\n" +
            "vec_4.xy = (vec_1.x > 0.0) ? vec_4.xy : vec_4.zw\n;" +
            "vec_4.x = (vec_1.y > 0.0) ? vec_4.x : vec_4.y\n;" +
            "vec2 coords = abs(vec_1) - vec_2 + vec_4.x\n;" +
            "   return min(max(coords.x, coords.y), 0.0) + length(max(coords, vec2(0.0f))) - vec_4.x\n;" +
            "}\n" +

            "void main() {\n" +
            "vec2 st = gl_TexCoord[0].st * size;\n" +
            "vec2 halfSize = 0.5 * size;\n" +
            "float sa = 1.0 - smoothstep(smoothness.x, smoothness.y, test(halfSize - st, halfSize - value, round));\n" +
            "gl_FragColor = mix(vec4(color.rgb, 0.0), vec4(color.rgb, color.a), sa);\n" +
            " \n" +
            "vec2 outlineSizeVec = size + vec2(outlineSize);\n" +
            "float outlineDist = test(halfSize - st, halfSize - value, round);\n" +
            "float outline = smoothstep(smoothness.x, smoothness.y, outlineDist) - smoothstep(smoothness.x, smoothness.y, outlineDist - outlineSize);\n" +
            "if (outlineDist < outlineSize)\n" +
            "   gl_FragColor = mix(gl_FragColor, outlineColor, outline);\n" +
            "}";

    private final String fog = "#version 120\n" +
            "\n" +
            "uniform float BLUR_AMOUNT = 1.6;\n" +
            "uniform sampler2D depthtex0;\n" +
            "uniform float near;\n" +
            "uniform float far;\n" +
            "uniform sampler2D textureIn;\n" +
            "uniform vec2 texelSize, direction;\n" +
            "uniform float startRadius;\n" +
            "uniform float endRadius;\n" +
            "uniform float depthStart;\n" +
            "uniform float weights[256];\n" +
            "\n" +
            "\n" +
            "#define offset texelSize * direction\n" +
            "#define clipping far\n" +
            "#define NOISE .5/255.0\n" +
            "\n" +
            "\n" +
            "float getDepth(vec2 coord) {\n" +
            "    return 2.0 * near * far / (far + near - (2.0 * texture2D(depthtex0, coord).x - 1.0) * (far - near)) / clipping;\n" +
            "}\n" +
            "\n" +
            "void main() {\n" +
            "    float depth = getDepth(gl_TexCoord[0].st);\n" +
            "    vec4 finalColor;\n" +
            "    if (depth >= depthStart) {\n" +
            "        float totalWeight = weights[0];\n" +
            "        vec3 blr = texture2D(textureIn, gl_TexCoord[0].st).rgb * weights[0];\n" +
            "\n" +
            "        for (float f = 0f; f <= mix(startRadius, endRadius, depth - depthStart); f++) {\n" +
            "            blr += texture2D(textureIn, gl_TexCoord[0].st + f * offset).rgb * (weights[int(abs(f))]);\n" +
            "            blr += texture2D(textureIn, gl_TexCoord[0].st - f * offset).rgb * (weights[int(abs(f))]);\n" +
            "            totalWeight += (weights[int(abs(f))]) * 2.0;\n" +
            "        }\n" +
            "\n" +
            "        finalColor = vec4(blr / totalWeight, 1);\n" +
            "\n" +
            "    }\n" +
            "    else {\n" +
            "        finalColor = vec4(texture2D(textureIn, gl_TexCoord[0].st).rgb, 1.0);\n" +
            "    }\n" +
            "\n" +
            "    gl_FragColor = finalColor;\n" +
            "}\n";

    private final String bloom = "#version 120\n" +
            "\n" +
            "uniform sampler2D textureIn, textureToCheck;\n" +
            "uniform vec2 texelSize, direction;\n" +
            "uniform float exposure, radius;\n" +
            "uniform float weights[64];\n" +
            "uniform bool avoidTexture;\n" +
            "\n" +
            "#define offset direction * texelSize\n" +
            "\n" +
            "void main() {\n" +
            "    if (direction.y == 1 && avoidTexture) {\n" +
            "        if (texture2D(textureToCheck, gl_TexCoord[0].st).a != 0.0) discard;\n" +
            "    }\n" +
            "    vec4 innerAlpha = texture2D(textureIn, gl_TexCoord[0].st);\n" +
            "    innerAlpha *= innerAlpha.a;\n" +
            "    innerAlpha *= weights[0];\n" +
            "\n" +
            "\n" +
            "\n" +
            "    for (float r = 1.0; r <= radius; r ++) {\n" +
            "        vec4 colorCurrent1 = texture2D(textureIn, gl_TexCoord[0].st + offset * r);\n" +
            "        vec4 colorCurrent2 = texture2D(textureIn, gl_TexCoord[0].st - offset * r);\n" +
            "\n" +
            "        colorCurrent1.rgb *= colorCurrent1.a;\n" +
            "        colorCurrent2.rgb *= colorCurrent2.a;\n" +
            "\n" +
            "        innerAlpha += (colorCurrent1 + colorCurrent2) * weights[int(r)];\n" +
            "\n" +
            "    }\n" +
            "\n" +
            "    gl_FragColor = vec4(innerAlpha.rgb / innerAlpha.a, mix(innerAlpha.a, 1.0 - exp(-innerAlpha.a * exposure), step(0.0, direction.y)));\n" +
            "}";

    String glowRect = "uniform vec2 rectSize; \n" +
            "uniform vec4 color1, color2, color3, color4; \n" +
            "uniform float radius, soft; \n" +
            "\n" +
            "/* Функция, вычисляющая расстояние от точки до круга с заданным радиусом и центром */\n" +
            "float roundSDF(vec2 p, vec2 b, float r) {\n" +
            "    return length(max(abs(p) - b , 0.0)) - r;\n" +
            "}\n" +
            "\n" +
            "/* Функция, создающая градиент по заданным цветам */\n" +
            "vec3 createGradient(vec2 coords, vec3 color1, vec3 color2, vec3 color3, vec3 color4){\n" +
            "    vec3 color = mix(mix(color1.rgb, color2.rgb, coords.y), mix(color3.rgb, color4.rgb, coords.y), coords.x);\n" +
            "    return color;\n" +
            "}\n" +
            "\n" +
            "void main() {\n" +
            "    vec2 st = gl_TexCoord[0].st; \n" +
            "    vec2 halfSize = rectSize * .5; \n" +
            "    float dist = roundSDF(halfSize - (gl_TexCoord[0].st * rectSize), halfSize - (soft + (radius * 0.75)), radius); \n" +
            "    float smoothedAlpha = (1. - smoothstep(-soft, soft, dist)) * color1.a;\n" +
            "    gl_FragColor = mix(vec4(createGradient(st, color1.rgb, color2.rgb, color3.rgb, color4.rgb), 0.0), vec4(createGradient(st, color1.rgb, color2.rgb, color3.rgb, color4.rgb), smoothedAlpha), smoothedAlpha);\n" +
            "}";

    private final String roundRectOutline = "#version 120\n" +
            "            \n" +
            "uniform vec2 location, rectSize;\n" +
            "uniform vec4 color, outlineColor1,outlineColor2,outlineColor3,outlineColor4;\n" +
            "uniform float radius, outlineThickness;\n" +
            "#define NOISE .5/255.0\n" +
            "\n" +
            "float roundedSDF(vec2 centerPos, vec2 size, float radius) {\n" +
            "    return length(max(abs(centerPos) - size + radius, 0.0)) - radius;\n" +
            "}\n" +
            "\n" +
            "vec3 createGradient(vec2 coords, vec3 color1, vec3 color2, vec3 color3, vec3 color4)\n" +
            "{\n" +
            "    vec3 color = mix(mix(color1.rgb, color2.rgb, coords.y), mix(color3.rgb, color4.rgb, coords.y), coords.x);\n" +
            "    color += mix(NOISE, -NOISE, fract(sin(dot(coords.xy, vec2(12.9898, 78.233))) * 43758.5453));\n" +
            "    return color;\n" +
            "}\n" +
            "\n" +
            "void main() {\n" +
            "    float distance = roundedSDF(gl_FragCoord.xy - location - (rectSize * .5), (rectSize * .5) + (outlineThickness * 0.5) - 1.0, radius);\n" +
            "\n" +
            "    float blendAmount = smoothstep(0., 2., abs(distance) - (outlineThickness * 0.5));\n" +
            "    vec4 outlineColor = vec4(createGradient(gl_TexCoord[0].st, outlineColor1.rgb, outlineColor2.rgb, outlineColor3.rgb, outlineColor4.rgb), outlineColor1.a);\n" +
            "    vec4 insideColor = (distance < 0.) ? color : vec4(outlineColor.rgb,  0.0);\n" +
            "    gl_FragColor = mix(outlineColor, insideColor, blendAmount);\n" +
            "}\n";

    String gradientRoundRect = "#version 120\n" +
            "\n" +
            "uniform vec2 location, rectSize;\n" +
            "uniform vec4 color1, color2, color3, color4;\n" +
            "uniform float radius;\n" +
            "\n" +
            "#define NOISE .5/255.0\n" +
            "\n" +
            "float roundSDF(vec2 p, vec2 b, float r) {\n" +
            "    return length(max(abs(p) - b , 0.0)) - r;\n" +
            "}\n" +
            "\n" +
            "vec3 createGradient(vec2 coords, vec3 color1, vec3 color2, vec3 color3, vec3 color4){\n" +
            "    vec3 color = mix(mix(color1.rgb, color2.rgb, coords.y), mix(color3.rgb, color4.rgb, coords.y), coords.x);\n" +
            "    color += mix(NOISE, -NOISE, fract(sin(dot(coords.xy, vec2(12.9898, 78.233))) * 43758.5453));\n" +
            "    return color;\n" +
            "}\n" +
            "\n" +
            "void main() {\n" +
            "    vec2 st = gl_TexCoord[0].st;\n" +
            "    vec2 halfSize = rectSize * .5;\n" +
            "    \n" +
            "    float smoothedAlpha =  (1.0-smoothstep(0.0, 2., roundSDF(halfSize - (gl_TexCoord[0].st * rectSize), halfSize - radius - 1., radius))) * color1.a;\n" +
            "    gl_FragColor = vec4(createGradient(st, color1.rgb, color2.rgb, color3.rgb, color4.rgb), smoothedAlpha);\n" +
            "}";

    String roundedRect = "#version 120\n" +
            "\n" +
            "uniform vec2 location, rectSize;\n" +
            "uniform vec4 color;\n" +
            "uniform float radius;\n" +
            "uniform bool blur;\n" +
            "\n" +
            "float roundSDF(vec2 p, vec2 b, float r) {\n" +
            "    return length(max(abs(p) - b, 0.0)) - r;\n" +
            "}\n" +
            "\n" +
            "\n" +
            "void main() {\n" +
            "    vec2 rectHalf = rectSize * 0.5;\n" +
            "    float smoothedAlpha =  (1.0-smoothstep(0.0, 1.0, roundSDF(rectHalf - (gl_TexCoord[0].st * rectSize), rectHalf - radius - 1., radius))) * color.a;\n" +
            "    gl_FragColor = vec4(color.rgb, smoothedAlpha);// mix(quadColor, shadowColor, 0.0);\n" +
            "\n" +
            "}";

    String vertex = "#version 120       \n" +
            "void main() {\n" +
            "    gl_TexCoord[0] = gl_MultiTexCoord0;\n" +
            "    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;\n" +
            "}\n";

    String texture = "uniform vec2 rectSize;\n" +
            "uniform sampler2D textureIn;\n" +
            "uniform float radius, alpha; /\n" +
            "\n" +
            "float roundedSDF(vec2 centerPos, vec2 size, float radius) {\n" +
            "    return length(max(abs(centerPos) - size, 0.)) - radius;\n" +
            "}\n" +
            "\n" +
            "void main() {\n" +
            "    float distance = roundedSDF((rectSize * .5) - (gl_TexCoord[0].st * rectSize), (rectSize * .5) - radius - 1., radius);\n" +
            "    \n" +
            "    float smoothedAlpha = (1.0 - smoothstep(0.0, 2.0, distance)) * alpha;\n" +
            "\n" +
            "    gl_FragColor = vec4(texture2D(textureIn, gl_TexCoord[0].st).rgb, smoothedAlpha);\n" +
            "}\n";

    String roundedCornerRect = "#version 120\n" +
            "    uniform vec2 size;\n" +
            "    uniform vec4 round;\n" +
            "    uniform vec2 smoothness;\n" +
            "    uniform float value;\n" +
            "    uniform vec4 color;\n" +
            "\n" +
            "    float test(vec2 vec_1, vec2 vec_2, vec4 vec_4) {\n" +
            "        vec_4.xy = (vec_1.x > 0.0) ? vec_4.xy : vec_4.zw;\n" +
            "        vec_4.x = (vec_1.y > 0.0) ? vec_4.x : vec_4.y;\n" +
            "        vec2 coords = abs(vec_1) - vec_2 + vec_4.x;\n" +
            "        return min(max(coords.x, coords.y), 0.0) + length(max(coords, vec2(0.0f))) - vec_4.x;\n" +
            "    }\n" +
            "\n" +
            "\n" +
            "    void main() {\n" +
            "        vec2 st = gl_TexCoord[0].st * size;\n" +
            "        vec2 halfSize = 0.5 * size;\n" +
            "        float sa = 1.0 - smoothstep(smoothness.x, smoothness.y, test(halfSize - st, halfSize - value, round));\n" +
            "        gl_FragColor = mix(vec4(color.rgb, 0.0), vec4(color.rgb, color.a), sa);\n" +
            "    }";

    String roundedCornerRectGradient = "#version 120\n" +
            "    uniform vec2 size;\n" +
            "    uniform vec4 round;\n" +
            "    uniform vec2 smoothness;\n" +
            "    uniform float value;\n" +
            "    uniform vec4 color1;\n" +
            "    uniform vec4 color2;\n" +
            "    uniform vec4 color3;\n" +
            "    uniform vec4 color4;\n" +
            "    #define NOISE .5/255.0\n" +
            "    float test(vec2 vec_1, vec2 vec_2, vec4 vec_4) {\n" +
            "        vec_4.xy = (vec_1.x > 0.0) ? vec_4.xy : vec_4.zw;\n" +
            "        vec_4.x = (vec_1.y > 0.0) ? vec_4.x : vec_4.y;\n" +
            "        vec2 coords = abs(vec_1) - vec_2 + vec_4.x;\n" +
            "        return min(max(coords.x, coords.y), 0.0) + length(max(coords, vec2(0.0f))) - vec_4.x;\n" +
            "    }\n" +
            "\n" +
            "    vec4 createGradient(vec2 coords, vec4 color1, vec4 color2, vec4 color3, vec4 color4)\n" +
            "{\n" +
            "    vec4 color = mix(mix(color1, color2, coords.y), mix(color3, color4, coords.y), coords.x);\n" +
            "    color += mix(NOISE, -NOISE, fract(sin(dot(coords.xy, vec2(12.9898, 78.233))) * 43758.5453));\n" +
            "    return color;\n" +
            "}\n" +
            "\n" +
            "    void main() {\n" +
            "        vec2 st = gl_TexCoord[0].st * size;\n" +
            "        vec2 halfSize = 0.5 * size;\n" +
            "        float sa = 1.0 - smoothstep(smoothness.x, smoothness.y, test(halfSize - st, halfSize - value, round));\n" +
            "        vec4 color = createGradient(gl_TexCoord[0].st, color1, color2,color3,color4);\n" +
            "        gl_FragColor = mix(vec4(color.rgb, 0.0), vec4(color.rgb, color.a), sa);\n" +
            "    }";

    String roundedCornerRectTexture = "#version 120\n" +
            "     uniform vec2 size;\n" +
            "     uniform vec4 round;\n" +
            "     uniform vec2 smoothness;\n" +
            "     uniform float value;\n" +
            "     uniform sampler2D textureIn;\n" +
            "     uniform float alpha;\n" +
            "\n" +
            "     float test(vec2 vec_1, vec2 vec_2, vec4 vec_4) {\n" +
            "         vec_4.xy = (vec_1.x > 0.0) ? vec_4.xy : vec_4.zw;\n" +
            "         vec_4.x = (vec_1.y > 0.0) ? vec_4.x : vec_4.y;\n" +
            "         vec2 coords = abs(vec_1) - vec_2 + vec_4.x;\n" +
            "         return min(max(coords.x, coords.y), 0.0) + length(max(coords, vec2(0.0f))) - vec_4.x;\n" +
            "     }\n" +
            "\n" +
            "     void main() {\n" +
            "         vec4 color = texture2D(textureIn, gl_TexCoord[0].st);\n" +
            "         vec2 st = gl_TexCoord[0].st * size;\n" +
            "         vec2 halfSize = 0.5 * size;\n" +
            "         float sa = 1.0 - smoothstep(smoothness.x, smoothness.y, test(halfSize - st, halfSize - value, round));\n" +
            "         gl_FragColor = mix(vec4(color.rgb, 0.0), vec4(color.rgb, alpha), sa);\n" +
            "     }";

    private String gradientMask = "#version 120\n" +
            "\n" +
            "uniform vec2 location, rectSize;\n" +
            "uniform sampler2D tex;\n" +
            "uniform vec3 color1, color2, color3, color4;\n" +
            "uniform float alpha;\n" +
            "\n" +
            "#define NOISE .5/255.0\n" +
            "\n" +
            "vec3 createGradient(vec2 coords, vec3 color1, vec3 color2, vec3 color3, vec3 color4){\n" +
            "    vec3 color = mix(mix(color1.rgb, color2.rgb, coords.y), mix(color3.rgb, color4.rgb, coords.y), coords.x);\n" +
            "    color += mix(NOISE, -NOISE, fract(sin(dot(coords.xy, vec2(12.9898,78.233))) * 43758.5453));\n" +
            "    return color;\n" +
            "}\n" +
            "\n" +
            "void main() {\n" +
            "    vec2 coords = (gl_FragCoord.xy - location) / rectSize;\n" +
            "    float texColorAlpha = texture2D(tex, gl_TexCoord[0].st).a;\n" +
            "    gl_FragColor = vec4(createGradient(coords, color1, color2, color3, color4), texColorAlpha * alpha);\n" +
            "}";

    private String alphaMask = "#version 120\n" +
            "uniform sampler2D tex;\n" +
            "uniform float alpha;\n" +
            "\n" +
            "\n" +
            "void main() {\n" +
            "    float texColorAlpha = texture2D(tex, gl_TexCoord[0].st).a;\n" +
            "    gl_FragColor = vec4(texture2D(tex, gl_TexCoord[0].st).rgb, texColorAlpha * alpha);\n" +
            "}";

    private String kawaseUpBloom = "#version 120\n" +
            "\n" +
            "uniform sampler2D inTexture, textureToCheck;\n" +
            "uniform vec2 halfpixel, offset, iResolution;\n" +
            "uniform int check;\n" +
            "\n" +
            "void main() {\n" +
            "    vec2 uv = vec2(gl_FragCoord.xy / iResolution);\n" +
            "\n" +
            "    vec4 sum = texture2D(inTexture, uv + vec2(-halfpixel.x * 2.0, 0.0) * offset);\n" +
            "    sum.rgb *= sum.a;\n" +
            "    vec4 smpl1 =  texture2D(inTexture, uv + vec2(-halfpixel.x, halfpixel.y) * offset);\n" +
            "    smpl1.rgb *= smpl1.a;\n" +
            "    sum += smpl1 * 2.0;\n" +
            "    vec4 smp2 = texture2D(inTexture, uv + vec2(0.0, halfpixel.y * 2.0) * offset);\n" +
            "    smp2.rgb *= smp2.a;\n" +
            "    sum += smp2;\n" +
            "    vec4 smp3 = texture2D(inTexture, uv + vec2(halfpixel.x, halfpixel.y) * offset);\n" +
            "    smp3.rgb *= smp3.a;\n" +
            "    sum += smp3 * 2.0;\n" +
            "    vec4 smp4 = texture2D(inTexture, uv + vec2(halfpixel.x * 2.0, 0.0) * offset);\n" +
            "    smp4.rgb *= smp4.a;\n" +
            "    sum += smp4;\n" +
            "    vec4 smp5 = texture2D(inTexture, uv + vec2(halfpixel.x, -halfpixel.y) * offset);\n" +
            "    smp5.rgb *= smp5.a;\n" +
            "    sum += smp5 * 2.0;\n" +
            "    vec4 smp6 = texture2D(inTexture, uv + vec2(0.0, -halfpixel.y * 2.0) * offset);\n" +
            "    smp6.rgb *= smp6.a;\n" +
            "    sum += smp6;\n" +
            "    vec4 smp7 = texture2D(inTexture, uv + vec2(-halfpixel.x, -halfpixel.y) * offset);\n" +
            "    smp7.rgb *= smp7.a;\n" +
            "    sum += smp7 * 2.0;\n" +
            "    vec4 result = sum / 12.0;\n" +
            "    gl_FragColor = vec4(result.rgb / result.a, mix(result.a, result.a * (1.0 - texture2D(textureToCheck, gl_TexCoord[0].st).a),check));\n" +
            "}";

    private String outline = "#version 120\n" +
            "\n" +
            "uniform vec4 color;\n" +
            "uniform sampler2D textureIn, textureToCheck;\n" +
            "uniform vec2 texelSize, direction;\n" +
            "uniform float size;\n" +
            "\n" +
            "#define offset direction * texelSize\n" +
            "\n" +
            "void main() {\n" +
            "    if (direction.y == 1) {\n" +
            "        if (texture2D(textureToCheck, gl_TexCoord[0].st).a != 0.0) discard;\n" +
            "    }\n" +
            "\n" +
            "    vec4 innerAlpha = texture2D(textureIn, gl_TexCoord[0].st);\n" +
            "    innerAlpha *= innerAlpha.a;\n" +
            "    for (float r = 1.0; r <= size; r ++) {\n" +
            "        vec4 colorCurrent1 = texture2D(textureIn, gl_TexCoord[0].st + offset * r);\n" +
            "        vec4 colorCurrent2 = texture2D(textureIn, gl_TexCoord[0].st - offset * r);\n" +
            "        colorCurrent1.rgb *= colorCurrent1.a;\n" +
            "        colorCurrent2.rgb *= colorCurrent2.a;\n" +
            "        innerAlpha += (colorCurrent1 + colorCurrent2) * r;\n" +
            "    }\n" +
            "    gl_FragColor = vec4(color.rgb, mix(innerAlpha.a, 1.0 - exp(-innerAlpha.a), step(0.0, direction.y)));\n" +
            "}";

    private String colorOut = "#version 120\n" +
            "\n" +
            "uniform vec4 color;\n" +
            "uniform sampler2D textureIn, textureToCheck;\n" +
            "uniform vec2 texelSize, direction;\n" +
            "uniform float size;\n" +
            "\n" +
            "#define offset direction * texelSize\n" +
            "\n" +
            "void main() {\n" +
            "    if (direction.y == 1) {\n" +
            "        if (texture2D(textureToCheck, gl_TexCoord[0].st).a != 0.0) discard;\n" +
            "    }\n" +
            "\n" +
            "    vec4 innerAlpha = texture2D(textureIn, gl_TexCoord[0].st);\n" +
            "    innerAlpha *= innerAlpha.a;\n" +
            "    for (float r = 1.0; r <= size; r ++) {\n" +
            "        vec4 colorCurrent1 = texture2D(textureIn, gl_TexCoord[0].st + offset * r);\n" +
            "        vec4 colorCurrent2 = texture2D(textureIn, gl_TexCoord[0].st - offset * r);\n" +
            "        colorCurrent1.rgb *= colorCurrent1.a;\n" +
            "        colorCurrent2.rgb *= colorCurrent2.a;\n" +
            "        innerAlpha += (colorCurrent1 + colorCurrent2) * r;\n" +
            "    }\n" +
            "    gl_FragColor = vec4(innerAlpha.rgb / innerAlpha.a, mix(innerAlpha.a, 1.0 - exp(-innerAlpha.a), step(0.0, direction.y)));\n" +
            "}";

    private String kawaseDownBloom = "#version 120\n" +
            "\n" +
            "uniform sampler2D inTexture;\n" +
            "uniform vec2 offset, halfpixel, iResolution;\n" +
            "\n" +
            "void main() {\n" +
            "    vec2 uv = vec2(gl_FragCoord.xy / iResolution);\n" +
            "    vec4 sum = texture2D(inTexture, gl_TexCoord[0].st);\n" +
            "    sum.rgb *= sum.a;\n" +
            "    sum *= 4.0;\n" +
            "    vec4 smp1 = texture2D(inTexture, uv - halfpixel.xy * offset);\n" +
            "    smp1.rgb *= smp1.a;\n" +
            "    sum += smp1;\n" +
            "    vec4 smp2 = texture2D(inTexture, uv + halfpixel.xy * offset);\n" +
            "    smp2.rgb *= smp2.a;\n" +
            "    sum += smp2;\n" +
            "    vec4 smp3 = texture2D(inTexture, uv + vec2(halfpixel.x, -halfpixel.y) * offset);\n" +
            "    smp3.rgb *= smp3.a;\n" +
            "    sum += smp3;\n" +
            "    vec4 smp4 = texture2D(inTexture, uv - vec2(halfpixel.x, -halfpixel.y) * offset);\n" +
            "    smp4.rgb *= smp4.a;\n" +
            "    sum += smp4;\n" +
            "    vec4 result = sum / 8.0;\n" +
            "    gl_FragColor = vec4(result.rgb / result.a, result.a);\n" +
            "}";
}
