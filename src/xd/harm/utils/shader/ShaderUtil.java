package xd.harm.utils.shader;

import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.GL20;

import xd.harm.utils.client.IMinecraft;
import xd.harm.utils.shader.exception.UndefinedShader;
import net.minecraft.client.MainWindow;
import net.minecraft.client.shader.Framebuffer;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.lwjgl.opengl.ARBShaderObjects.*;
import static org.lwjgl.opengl.GL11.GL_QUADS;
import static org.lwjgl.opengl.GL11.glBegin;
import static org.lwjgl.opengl.GL11.glEnd;
import static org.lwjgl.opengl.GL11.glTexCoord2f;
import static org.lwjgl.opengl.GL11.glVertex2f;
import static org.lwjgl.opengl.GL20.*;

public class ShaderUtil implements IMinecraft {
    private final int programID;
    private final Map<String, Integer> uniformLocationCache = new HashMap<>();

    public static ShaderUtil textShader = new ShaderUtil("textShader");
    public static ShaderUtil rounded = new ShaderUtil("rounded");
    public static ShaderUtil roundedout = new ShaderUtil("roundedout");
    public static ShaderUtil smooth = new ShaderUtil("smooth");
    public static ShaderUtil white = new ShaderUtil("white");
    public static ShaderUtil alpha = new ShaderUtil("alpha");
    public static ShaderUtil kawaseUp = new ShaderUtil("kawaseUp");
    public static ShaderUtil kawaseDown = new ShaderUtil("kawaseDown");
    public static ShaderUtil outline = new ShaderUtil("outline");
    public static ShaderUtil mask = new ShaderUtil("mask");
    public static ShaderUtil head = new ShaderUtil("round-head");
    public static ShaderUtil ringArc = new ShaderUtil("ring-arc-aa");
    public static ShaderUtil ringArcGradient = new ShaderUtil("ring-arc-rotating-grad");
    public static ShaderUtil gradient = new ShaderUtil("gradient");
    public static ShaderUtil gradientBar = new ShaderUtil("gradientBar");
    public static ShaderUtil dashedOutline = new ShaderUtil("dashedOutline");
    public static ShaderUtil menuButton = new ShaderUtil("menuButton");
    public static ShaderUtil mainMenuGrid = new ShaderUtil("mainMenuGrid");
    public static ShaderUtil flame = new ShaderUtil("flame");
    public static ShaderUtil radialFill = new ShaderUtil("radialFill");

    public ShaderUtil(String fragmentShaderLoc) {
        programID = ARBShaderObjects.glCreateProgramObjectARB();

        try {

            int fragmentShaderID = switch (fragmentShaderLoc) {
                case "textShader" -> createShader(Shaders.getInstance().getFont(), GL_FRAGMENT_SHADER);
                case "smooth" -> createShader(Shaders.getInstance().getSmooth(), GL_FRAGMENT_SHADER);
                case "white" -> createShader(Shaders.getInstance().getWhite(), GL_FRAGMENT_SHADER);
                case "rounded" -> createShader(Shaders.getInstance().getRounded(), GL_FRAGMENT_SHADER);
                case "round" -> createShader(Shaders.getInstance().getRound(), GL_FRAGMENT_SHADER);
                case "roundedout" -> createShader(Shaders.getInstance().getRoundedout(), GL_FRAGMENT_SHADER);
                case "bloom" -> createShader(Shaders.getInstance().getGaussianbloom(), GL_FRAGMENT_SHADER);
                case "kawaseUp" -> createShader(Shaders.getInstance().getKawaseUp(), GL_FRAGMENT_SHADER);
                case "kawaseDown" -> createShader(Shaders.getInstance().getKawaseDown(), GL_FRAGMENT_SHADER);
                case "alpha" -> createShader(Shaders.getInstance().getAlpha(), GL_FRAGMENT_SHADER);
                case "outline" -> createShader(Shaders.getInstance().getOutline(), GL_FRAGMENT_SHADER);
                case "contrast" -> createShader(Shaders.getInstance().getContrast(), GL_FRAGMENT_SHADER);
                case "mask" -> createShader(Shaders.getInstance().getMask(), GL_FRAGMENT_SHADER);
                case "round-head" -> createShader(Shaders.getInstance().getHead(), GL_FRAGMENT_SHADER);
                case "ring-arc-aa" -> createShader(Shaders.getInstance().getRingArc(), GL_FRAGMENT_SHADER);
                case "ring-arc-grad-aa" -> createShader(Shaders.getInstance().getRingArcGrad(), GL_FRAGMENT_SHADER);
                case "ring-arc-rotating-grad" -> createShader(Shaders.getInstance().getRingArcGradientGlsl(), GL_FRAGMENT_SHADER);
                case "circle-arc-aa" -> createShader(Shaders.getInstance().getCircleArc(), GL_FRAGMENT_SHADER);
                case "circle-arc-grad-aa" -> createShader(Shaders.getInstance().getCircleArcGrad(), GL_FRAGMENT_SHADER);
                case "hud-holo" -> createShader(Shaders.getInstance().getHudHolo(), GL_FRAGMENT_SHADER);
                case "hud-rays" -> createShader(Shaders.getInstance().getHudRays(), GL_FRAGMENT_SHADER);
                case "infinity-loader" -> createShader(Shaders.getInstance().getInfinityLoader(), GL_FRAGMENT_SHADER);
                case "exit-shader" -> createShader(Shaders.getInstance().getExitShader(), GL_FRAGMENT_SHADER);
                case "MainMenuShader" -> createShader(Shaders.getInstance().getMainMenuShader(), GL_FRAGMENT_SHADER);
                case "gradient" -> createShader(Shaders.getInstance().getGradient(), GL_FRAGMENT_SHADER);
                case "gradientBar" -> createShader(Shaders.getInstance().getGradientBar(), GL_FRAGMENT_SHADER);
                case "dashedOutline" -> createShader(Shaders.getInstance().getDashedOutline(), GL_FRAGMENT_SHADER);
                case "menuButton" -> createShader(Shaders.getInstance().getMenuButton(), GL_FRAGMENT_SHADER);
                case "mainMenuGrid" -> createShader(Shaders.getInstance().getMainMenuGrid(), GL_FRAGMENT_SHADER);
                case "radialFill" -> createShader(Shaders.getInstance().getRadialFill(), GL_FRAGMENT_SHADER);

                default ->
                        throw new UndefinedShader(fragmentShaderLoc);
            };
            ARBShaderObjects.glAttachObjectARB(programID, fragmentShaderID);

            ARBShaderObjects.glAttachObjectARB(programID,
                    createShader(Shaders.getInstance().getVertex(), GL_VERTEX_SHADER));

            ARBShaderObjects.glLinkProgramARB(programID);
        } catch (UndefinedShader exception) {
            exception.fillInStackTrace();
            System.out.println("Ошибка при загрузке: " + fragmentShaderLoc);
        }
    }

    public static Framebuffer createFrameBuffer(Framebuffer framebuffer) {
        return createFrameBuffer(framebuffer, false);
    }

    public static boolean needsNewFramebuffer(Framebuffer framebuffer) {
        return framebuffer == null || framebuffer.framebufferWidth != mc.getMainWindow().getWidth()
                || framebuffer.framebufferHeight != mc.getMainWindow().getHeight();
    }

    public static void setupRoundedRectUniforms(float x, float y, float width, float height, float radius, ShaderUtil roundedTexturedShader) {
        MainWindow mainWindow = Minecraft.getInstance().getMainWindow();
        roundedTexturedShader.setUniform("location", (float) (x * 2),
                (float) ((mainWindow.getHeight() - (height * 2)) - (y * 2)));
        roundedTexturedShader.setUniform("rectSize", (float) (width * 2), (float) (height * 2));
        roundedTexturedShader.setUniform("radius", (float) (radius * 2));
    }

    public int getUniform(String name) {
        Integer location = uniformLocationCache.get(name);

        if (location == null) {
            location = ARBShaderObjects.glGetUniformLocationARB(programID, name);
            uniformLocationCache.put(name, location);
        }

        return location;
    }

    public static Framebuffer createFrameBuffer(Framebuffer framebuffer, boolean depth) {
        if (needsNewFramebuffer(framebuffer)) {
            if (framebuffer != null) {
                framebuffer.deleteFramebuffer();
            }
            return new Framebuffer(mc.getMainWindow().getWidth(), mc.getMainWindow().getHeight(), depth, false);
        }
        return framebuffer;
    }

    public void drawQuads(float x, float y, float width, float height) {
        glBegin(GL_QUADS);
        glTexCoord2f(0, 0);
        glVertex2f(x, y);
        glTexCoord2f(0, 1);
        glVertex2f(x, y + height);
        glTexCoord2f(1, 1);
        glVertex2f(x + width, y + height);
        glTexCoord2f(1, 0);
        glVertex2f(x + width, y);
        glEnd();
    }

    public static void drawQuads() {
        MainWindow sr = mc.getMainWindow();
        float width = (float) sr.getScaledWidth();
        float height = (float) sr.getScaledHeight();
        glBegin(GL_QUADS);
        glTexCoord2f(0, 1);
        glVertex2f(0, 0);
        glTexCoord2f(0, 0);
        glVertex2f(0, height);
        glTexCoord2f(1, 0);
        glVertex2f(width, height);
        glTexCoord2f(1, 1);
        glVertex2f(width, 0);
        glEnd();
    }

    public Framebuffer setupBuffer(Framebuffer frameBuffer) {
        if (frameBuffer.framebufferWidth != mc.getMainWindow().getWidth()
                || frameBuffer.framebufferHeight != mc.getMainWindow().getHeight())
            frameBuffer.resize(Math.max(1, mc.getMainWindow().getWidth()), Math.max(1, mc.getMainWindow().getHeight()),
                    false);
        else
            frameBuffer.framebufferClear(false);
        frameBuffer.setFramebufferColor(0.0f, 0.0f, 0.0f, 0.0f);

        return frameBuffer;
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
            case 1 -> ARBShaderObjects.glUniform1fARB(loc, args[0]);
            case 2 -> ARBShaderObjects.glUniform2fARB(loc, args[0], args[1]);
            case 3 -> ARBShaderObjects.glUniform3fARB(loc, args[0], args[1], args[2]);
            case 4 -> ARBShaderObjects.glUniform4fARB(loc, args[0], args[1], args[2], args[3]);
            default ->
                    throw new IllegalArgumentException("Недопустимое количество аргументов для uniform '" + name + "'");
        }
    }

    public void setUniform(String name, int... args) {
        int loc = getUniform(name);
        switch (args.length) {
            case 1 -> glUniform1iARB(loc, args[0]);
            case 2 -> glUniform2iARB(loc, args[0], args[1]);
            case 3 -> glUniform3iARB(loc, args[0], args[1], args[2]);
            case 4 -> glUniform4iARB(loc, args[0], args[1], args[2], args[3]);
            default ->
                    throw new IllegalArgumentException("Недопустимое количество аргументов для uniform '" + name + "'");
        }
    }

    public void setUniformf(String var1, float... var2) {
        int var3 = getUniform(var1);
        switch (var2.length) {
            case 1 -> ARBShaderObjects.glUniform1fARB(var3, var2[0]);
            case 2 -> ARBShaderObjects.glUniform2fARB(var3, var2[0], var2[1]);
            case 3 -> ARBShaderObjects.glUniform3fARB(var3, var2[0], var2[1], var2[2]);
            case 4 -> ARBShaderObjects.glUniform4fARB(var3, var2[0], var2[1], var2[2], var2[3]);
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

    private int createShader(IShader glsl, int shaderType) {
        int shader = ARBShaderObjects.glCreateShaderObjectARB(shaderType);
        ARBShaderObjects.glShaderSourceARB(shader, readInputStream(new ByteArrayInputStream(glsl.glsl().getBytes())));
        ARBShaderObjects.glCompileShaderARB(shader);
        if (GL20.glGetShaderi(shader, 35713) == 0) {
            System.out.println(GL20.glGetShaderInfoLog(shader, 4096));
            throw new IllegalStateException(String.format("Shader (%s) failed to compile!", shaderType));
        }
        return shader;
    }

    public String readInputStream(InputStream inputStream) {
        return new BufferedReader(new InputStreamReader(inputStream)).lines()
                .map(line -> line + '\n')
                .collect(Collectors.joining());
    }
}
