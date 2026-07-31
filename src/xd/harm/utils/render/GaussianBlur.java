package xd.harm.utils.render;

import com.mojang.blaze3d.platform.GlStateManager;
import xd.harm.utils.client.IMinecraft;
import xd.harm.utils.render.gl.Stencil;
import xd.harm.utils.shaderbydobser.old.ShaderUtils;
import net.minecraft.client.MainWindow;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

import static com.mojang.blaze3d.systems.RenderSystem.glUniform1;
import static org.lwjgl.opengl.ARBShaderObjects.glUniform1fARB;
import static org.lwjgl.opengl.ARBShaderObjects.glUniform1iARB;
import static org.lwjgl.opengl.ARBShaderObjects.glUniform2fARB;


public class GaussianBlur  {

    private static final ShaderUtils gaussianBlur = new ShaderUtils("blur");
    private static final FloatBuffer WEIGHT_BUFFER = BufferUtils.createFloatBuffer(256);
    private static Framebuffer framebuffer = new Framebuffer(1, 1, false, false);
    private static boolean uniformsInitialized;
    private static int textureInUniform;
    private static int texelSizeUniform;
    private static int directionUniform;
    private static int radiusUniform;
    private static int weightsUniform;
    private static int cachedWindowWidth = -1;
    private static int cachedWindowHeight = -1;
    private static float cachedInvWindowWidth;
    private static float cachedInvWindowHeight;
    private static float cachedWeightsRadius = -1.0f;

    private static void setupUniforms(float dir1, float dir2, float radius) {
        initUniforms();
        updateWindowSize();

        glUniform1iARB(textureInUniform, 0);
        glUniform2fARB(texelSizeUniform, cachedInvWindowWidth, cachedInvWindowHeight);
        setDirection(dir1, dir2);
        glUniform1fARB(radiusUniform, radius);
        uploadWeights(radius);
    }

    private static void initUniforms() {
        if (uniformsInitialized) {
            return;
        }

        textureInUniform = gaussianBlur.getUniform("textureIn");
        texelSizeUniform = gaussianBlur.getUniform("texelSize");
        directionUniform = gaussianBlur.getUniform("direction");
        radiusUniform = gaussianBlur.getUniform("radius");
        weightsUniform = gaussianBlur.getUniform("weights");
        uniformsInitialized = true;
    }

    private static void updateWindowSize() {
        MainWindow window = IMinecraft.mc.getMainWindow();
        int width = Math.max(1, window.getWidth());
        int height = Math.max(1, window.getHeight());

        if (cachedWindowWidth == width && cachedWindowHeight == height) {
            return;
        }

        cachedWindowWidth = width;
        cachedWindowHeight = height;
        cachedInvWindowWidth = 1.0F / width;
        cachedInvWindowHeight = 1.0F / height;
    }

    private static void setDirection(float dir1, float dir2) {
        glUniform2fARB(directionUniform, dir1, dir2);
    }

    private static void uploadWeights(float radius) {
        if (Float.compare(cachedWeightsRadius, radius) == 0) {
            return;
        }

        WEIGHT_BUFFER.clear();
        for (int i = 0; i <= radius; i++) {
            WEIGHT_BUFFER.put(calculateGaussianValue(i, radius / 2));
        }

        WEIGHT_BUFFER.flip();
        glUniform1(weightsUniform, WEIGHT_BUFFER);
        cachedWeightsRadius = radius;
    }

    public static void startBlur(){
        GlStateManager.clearColor(0.0f, 0.0f, 0.0f, 0.0f);
        Stencil.initStencilToWrite();
    }

    public static void endBlur(float radius, float compression) {
        Stencil.readStencilBuffer(1);

        framebuffer = createFrameBuffer(framebuffer);

        framebuffer.framebufferClear(false);
        framebuffer.bindFramebuffer(false);
        gaussianBlur.attach();
        setupUniforms(compression, 0, radius);

        GlStateManager.bindTexture(IMinecraft.mc.getFramebuffer().framebufferTexture);
        ShaderUtils.drawQuads();
        framebuffer.unbindFramebuffer();
        gaussianBlur.detach();

        IMinecraft.mc.getFramebuffer().bindFramebuffer(false);
        gaussianBlur.attach();
        setDirection(0, compression);

        GlStateManager.bindTexture(framebuffer.framebufferTexture);
        ShaderUtils.drawQuads();
        gaussianBlur.detach();

        Stencil.uninitStencilBuffer();
        GlStateManager.color4f(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.bindTexture(0);
        GlStateManager.clearColor(0.0f, 0.0f, 0.0f, 1.0f);
    }

    public static void blur(float radius, float compression) {
        framebuffer = createFrameBuffer(framebuffer);

        framebuffer.framebufferClear(false);
        framebuffer.bindFramebuffer(false);
        gaussianBlur.attach();
        setupUniforms(compression, 0, radius);

        GlStateManager.bindTexture(IMinecraft.mc.getFramebuffer().framebufferTexture);
        ShaderUtils.drawQuads();
        framebuffer.unbindFramebuffer();
        gaussianBlur.detach();

        IMinecraft.mc.getFramebuffer().bindFramebuffer(false);
        gaussianBlur.attach();
        setDirection(0, compression);

        GlStateManager.bindTexture(framebuffer.framebufferTexture);
        ShaderUtils.drawQuads();
        gaussianBlur.detach();

        GlStateManager.color4f(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.bindTexture(0);
    }

    private static Framebuffer createFrameBuffer(Framebuffer framebuffer) {
        MainWindow window = IMinecraft.mc.getMainWindow();
        int width = Math.max(1, window.getWidth());
        int height = Math.max(1, window.getHeight());

        if (framebuffer == null || framebuffer.framebufferWidth != width || framebuffer.framebufferHeight != height) {
            if (framebuffer != null) {
                framebuffer.deleteFramebuffer();
            }
            return new Framebuffer(width, height, false, false);
        }

        return framebuffer;
    }

    public static float calculateGaussianValue(float x, float sigma) {
        double output = 1.0 / Math.sqrt(2.0 * Math.PI * (sigma * sigma));
        return (float) (output * Math.exp(-(x * x) / (2.0 * (sigma * sigma))));
    }
}
