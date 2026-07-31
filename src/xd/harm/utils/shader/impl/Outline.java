package xd.harm.utils.shader.impl;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.lwjgl.opengl.GL30;

import com.google.common.collect.Queues;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.IRenderCall;

import xd.harm.utils.client.IMinecraft;
import xd.harm.utils.shader.ShaderUtil;
import net.minecraft.client.MainWindow;
import net.minecraft.client.shader.Framebuffer;

import static org.lwjgl.opengl.ARBShaderObjects.glUniform1fARB;
import static org.lwjgl.opengl.ARBShaderObjects.glUniform1iARB;
import static org.lwjgl.opengl.ARBShaderObjects.glUniform2fARB;
import static org.lwjgl.opengl.ARBShaderObjects.glUniform3fARB;

public class Outline implements IMinecraft {

    private static final ConcurrentLinkedQueue<IRenderCall> renderQueue = Queues.newConcurrentLinkedQueue();
    private static final Framebuffer inFrameBuffer = new Framebuffer(1, 1, true, false);
    private static final Framebuffer outFrameBuffer = new Framebuffer(1, 1, true, false);
    private static boolean uniformsInitialized;
    private static int sizeUniform;
    private static int textureInUniform;
    private static int textureToCheckUniform;
    private static int texelSizeUniform;
    private static int directionUniform;
    private static int colorUniform;

    public static void registerRenderCall(IRenderCall rc) {
        renderQueue.add(rc);
    }

    public static void draw(int radius, int color) {
        if (renderQueue.isEmpty())
            return;

        MainWindow window = mc.getMainWindow();
        int windowWidth = Math.max(1, window.getWidth());
        int windowHeight = Math.max(1, window.getHeight());
        float invWindowWidth = 1.0F / windowWidth;
        float invWindowHeight = 1.0F / windowHeight;

        setupBuffer(inFrameBuffer, windowWidth, windowHeight);
        setupBuffer(outFrameBuffer, windowWidth, windowHeight);

        inFrameBuffer.bindFramebuffer(true);

        IRenderCall renderCall;
        while ((renderCall = renderQueue.poll()) != null) {
            renderCall.execute();
        }

        outFrameBuffer.bindFramebuffer(true);

        ShaderUtil.outline.attach();
        initUniforms();
        glUniform1fARB(sizeUniform, radius);
        glUniform1iARB(textureInUniform, 0);
        glUniform1iARB(textureToCheckUniform, 20);
        glUniform2fARB(texelSizeUniform, invWindowWidth, invWindowHeight);
        glUniform2fARB(directionUniform, 1.0F, 0.0F);
        glUniform3fARB(colorUniform, (color >> 16 & 0xFF) / 255f, (color >> 8 & 0xFF) / 255f, (color & 0xFF) / 255f);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL30.GL_ONE, GL30.GL_SRC_ALPHA);
        GL30.glAlphaFunc(GL30.GL_GREATER, 0.0001f);

        inFrameBuffer.bindFramebufferTexture();
        ShaderUtil.drawQuads();

        mc.getFramebuffer().bindFramebuffer(false);
        GlStateManager.blendFunc(GL30.GL_SRC_ALPHA, GL30.GL_ONE_MINUS_SRC_ALPHA);

        glUniform2fARB(directionUniform, 0.0F, 1.0F);

        outFrameBuffer.bindFramebufferTexture();
        GL30.glActiveTexture(GL30.GL_TEXTURE20);
        inFrameBuffer.bindFramebufferTexture();
        GL30.glActiveTexture(GL30.GL_TEXTURE0);
        ShaderUtil.drawQuads();

        ShaderUtil.outline.detach();
        GlStateManager.bindTexture(0);
        GlStateManager.disableBlend();
    }

    private static void initUniforms() {
        if (uniformsInitialized) {
            return;
        }

        sizeUniform = ShaderUtil.outline.getUniform("size");
        textureInUniform = ShaderUtil.outline.getUniform("textureIn");
        textureToCheckUniform = ShaderUtil.outline.getUniform("textureToCheck");
        texelSizeUniform = ShaderUtil.outline.getUniform("texelSize");
        directionUniform = ShaderUtil.outline.getUniform("direction");
        colorUniform = ShaderUtil.outline.getUniform("color");
        uniformsInitialized = true;
    }

    public static Framebuffer setupBuffer(Framebuffer frameBuffer, int width, int height) {
        if (frameBuffer.framebufferWidth != width || frameBuffer.framebufferHeight != height)
            frameBuffer.resize(width, height, false);
        else
            frameBuffer.framebufferClear(false);
        frameBuffer.setFramebufferColor(0.0f, 0.0f, 0.0f, 0.0f);

        return frameBuffer;
    }

}
