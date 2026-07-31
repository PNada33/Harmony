package xd.harm.utils.shader.impl;

import org.lwjgl.opengl.GL30;

import com.mojang.blaze3d.platform.GlStateManager;

import xd.harm.utils.client.IMinecraft;
import xd.harm.utils.shader.ShaderUtil;
import lombok.experimental.UtilityClass;
import net.minecraft.client.MainWindow;
import net.minecraft.client.shader.Framebuffer;

import static org.lwjgl.opengl.ARBShaderObjects.glUniform2fARB;

@UtilityClass
public class Mask implements IMinecraft {

    private final Framebuffer in = new Framebuffer(1, 1, true, false);
    private final Framebuffer out = new Framebuffer(1, 1, true, false);
    private boolean uniformsInitialized;
    private int locationUniform;
    private int rectSizeUniform;

    public void renderMask(float x, float y, float width, float height, Runnable mask) {
        MainWindow window = mc.getMainWindow();
        int windowWidth = Math.max(1, window.getWidth());
        int windowHeight = Math.max(1, window.getHeight());
        float scale = (float) window.getGuiScaleFactor();

        setupBuffer(in, windowWidth, windowHeight);
        setupBuffer(out, windowWidth, windowHeight);

        in.bindFramebuffer(true);

        mask.run();

        out.bindFramebuffer(true);

        ShaderUtil.mask.attach();
        initUniforms();
        glUniform2fARB(locationUniform, x * scale, (windowHeight - (height * scale)) - (y * scale));
        glUniform2fARB(rectSizeUniform, width * scale, height * scale);
       

        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL30.GL_ONE, GL30.GL_SRC_ALPHA);
        GL30.glAlphaFunc(GL30.GL_GREATER, 0.0001f);

        in.bindFramebufferTexture();
        ShaderUtil.drawQuads();

        mc.getFramebuffer().bindFramebuffer(false);
        GlStateManager.blendFunc(GL30.GL_SRC_ALPHA, GL30.GL_ONE_MINUS_SRC_ALPHA);

        out.bindFramebufferTexture();
        GL30.glActiveTexture(GL30.GL_TEXTURE20);
        in.bindFramebufferTexture();
        GL30.glActiveTexture(GL30.GL_TEXTURE0);
        ShaderUtil.drawQuads();
        ShaderUtil.mask.detach();
        GlStateManager.bindTexture(0);
        GlStateManager.disableBlend();
    }

    private void initUniforms() {
        if (uniformsInitialized) {
            return;
        }

        locationUniform = ShaderUtil.mask.getUniform("location");
        rectSizeUniform = ShaderUtil.mask.getUniform("rectSize");
        uniformsInitialized = true;
    }

    private Framebuffer setupBuffer(Framebuffer frameBuffer, int width, int height) {
        if (frameBuffer.framebufferWidth != width || frameBuffer.framebufferHeight != height)
            frameBuffer.resize(width, height, false);
        else
            frameBuffer.framebufferClear(false);
        frameBuffer.setFramebufferColor(0.0f, 0.0f, 0.0f, 0.0f);

        return frameBuffer;
    }

}
