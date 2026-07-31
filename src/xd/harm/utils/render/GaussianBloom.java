package xd.harm.utils.render;

import java.nio.FloatBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL30;

import com.google.common.collect.Queues;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.IRenderCall;

import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;

import com.mojang.blaze3d.systems.RenderSystem;

import xd.harm.utils.client.IMinecraft;
import xd.harm.utils.shader.ShaderUtil;

import static org.lwjgl.opengl.ARBShaderObjects.glUniform1fARB;
import static org.lwjgl.opengl.ARBShaderObjects.glUniform1iARB;
import static org.lwjgl.opengl.ARBShaderObjects.glUniform2fARB;

public class GaussianBloom implements IMinecraft {

    public static GaussianBloom INGAME = new GaussianBloom();
    public static GaussianBloom GUI = new GaussianBloom();
    private final ShaderUtil bloom = new ShaderUtil("bloom");
    private final ConcurrentLinkedQueue<IRenderCall> renderQueue = Queues.newConcurrentLinkedQueue();
    private final FloatBuffer weightBuffer = BufferUtils.createFloatBuffer(128);

    private final Framebuffer inFrameBuffer = new Framebuffer(1, 1, true, false);
    private final Framebuffer outFrameBuffer = new Framebuffer(1, 1, true, false);
    private boolean uniformsInitialized;
    private int radiusUniform;
    private int exposureUniform;
    private int textureInUniform;
    private int textureToCheckUniform;
    private int avoidTextureUniform;
    private int weightsUniform;
    private int texelSizeUniform;
    private int directionUniform;
    private int cachedWindowWidth = -1;
    private int cachedWindowHeight = -1;
    private float cachedInvWindowWidth;
    private float cachedInvWindowHeight;
    private int cachedWeightsRadius = -1;

    public void registerRenderCall(IRenderCall rc) {
        renderQueue.add(rc);
    }

    public void draw(int radius, float exp, boolean fill, float direction) {
        if (renderQueue.isEmpty())
            return;
        
        MainWindow window = Minecraft.getInstance().getMainWindow();
        int windowWidth = Math.max(1, window.getWidth());
        int windowHeight = Math.max(1, window.getHeight());
        updateWindowSize(windowWidth, windowHeight);

        setupBuffer(inFrameBuffer, windowWidth, windowHeight);
        setupBuffer(outFrameBuffer, windowWidth, windowHeight);

        inFrameBuffer.bindFramebuffer(true);
        IRenderCall renderCall;
        while ((renderCall = renderQueue.poll()) != null) {
            renderCall.execute();
        }
        inFrameBuffer.unbindFramebuffer();

        outFrameBuffer.bindFramebuffer(true);

        bloom.attach();
        initUniforms();
        glUniform1fARB(radiusUniform, radius);
        glUniform1fARB(exposureUniform, exp);
        glUniform1iARB(textureInUniform, 0);
        glUniform1iARB(textureToCheckUniform, 20);
        glUniform1iARB(avoidTextureUniform, fill ? 1 : 0);
        uploadWeights(radius);
        glUniform2fARB(texelSizeUniform, cachedInvWindowWidth, cachedInvWindowHeight);
        glUniform2fARB(directionUniform, direction, 0.0F);

        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL30.GL_ONE, GL30.GL_SRC_ALPHA);
        GL30.glAlphaFunc(GL30.GL_GREATER, 0.0001f);

        inFrameBuffer.bindFramebufferTexture();
        ShaderUtil.drawQuads();

        mc.getFramebuffer().bindFramebuffer(false);
        GlStateManager.blendFunc(GL30.GL_SRC_ALPHA, GL30.GL_ONE_MINUS_SRC_ALPHA);

        glUniform2fARB(directionUniform, 0.0F, direction);

        outFrameBuffer.bindFramebufferTexture();
        GL30.glActiveTexture(GL30.GL_TEXTURE20);
        inFrameBuffer.bindFramebufferTexture();
        GL30.glActiveTexture(GL30.GL_TEXTURE0);
        ShaderUtil.drawQuads();

        bloom.detach();
        GlStateManager.bindTexture(0);
        GlStateManager.disableBlend();
        mc.getFramebuffer().bindFramebuffer(false);
    }

    private void initUniforms() {
        if (uniformsInitialized) {
            return;
        }

        radiusUniform = bloom.getUniform("radius");
        exposureUniform = bloom.getUniform("exposure");
        textureInUniform = bloom.getUniform("textureIn");
        textureToCheckUniform = bloom.getUniform("textureToCheck");
        avoidTextureUniform = bloom.getUniform("avoidTexture");
        weightsUniform = bloom.getUniform("weights");
        texelSizeUniform = bloom.getUniform("texelSize");
        directionUniform = bloom.getUniform("direction");
        uniformsInitialized = true;
    }

    private void updateWindowSize(int width, int height) {
        if (cachedWindowWidth == width && cachedWindowHeight == height) {
            return;
        }

        cachedWindowWidth = width;
        cachedWindowHeight = height;
        cachedInvWindowWidth = 1.0F / width;
        cachedInvWindowHeight = 1.0F / height;
    }

    private void uploadWeights(int radius) {
        if (cachedWeightsRadius == radius) {
            return;
        }

        weightBuffer.clear();
        for (int i = 0; i <= radius; i++) {
            weightBuffer.put(calculateGaussianValue(i, radius / 2));
        }
        weightBuffer.flip();
        RenderSystem.glUniform1(weightsUniform, weightBuffer);
        cachedWeightsRadius = radius;
    }

    private Framebuffer setupBuffer(Framebuffer frameBuffer, int width, int height) {
        if (frameBuffer.framebufferWidth != width || frameBuffer.framebufferHeight != height)
            frameBuffer.resize(width, height, false);
        else
            frameBuffer.framebufferClear(false);
        frameBuffer.setFramebufferColor(0.0f, 0.0f, 0.0f, 0.0f);

        return frameBuffer;
    }

    private float calculateGaussianValue(float x, float sigma) {
        double PI = 3.141592653;
        double output = 1.0 / Math.sqrt(2.0 * PI * (sigma * sigma));
        return (float) (output * Math.exp(-(x * x) / (2.0 * (sigma * sigma))));
    }

}
