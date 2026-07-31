package xd.harm.utils.render;

import xd.harm.utils.shaderharmony.ShaderUtility;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.IRenderCall;
import lombok.Getter;
import net.minecraft.client.MainWindow;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import xd.harm.utils.client.IMinecraft;
import xd.harm.utils.render.gl.GLUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.ARBShaderObjects.glUniform1fARB;
import static org.lwjgl.opengl.ARBShaderObjects.glUniform2fARB;
import static org.lwjgl.opengl.GL20.glUniform1i;

@Getter
public class ESPShader implements IMinecraft {
    private final ShaderUtility upSample = new ShaderUtility("kawaseUpBloom");
    private final ShaderUtility downSample = new ShaderUtility("kawaseDownBloom");
    public List<IRenderCall> displayBlurQueue = new LinkedList<>();
    public List<IRenderCall> cameraBlurQueue = new LinkedList<>();
    private Framebuffer inFramebuffer = new Framebuffer(mc.getMainWindow().getFramebufferWidth(), mc.getMainWindow().getFramebufferHeight(), true);
    private Framebuffer outFramebuffer = new Framebuffer(mc.getMainWindow().getFramebufferWidth(), mc.getMainWindow().getFramebufferHeight(), true);
    private int iterations;
    private final List<Framebuffer> framebufferList = new ArrayList<>();
    private final ShaderUniforms upSampleUniforms = new ShaderUniforms();
    private final ShaderUniforms downSampleUniforms = new ShaderUniforms();
    private int quadWidth = 1;
    private int quadHeight = 1;

    private void initFramebuffers(int iterations, int framebufferWidth, int framebufferHeight) {

        for (Framebuffer framebuffer : framebufferList) {
            framebuffer.deleteFramebuffer();
        }
        framebufferList.clear();

        framebufferList.add(outFramebuffer = new Framebuffer(framebufferWidth, framebufferHeight, false));

        for (int i = 0; i <= iterations; i++) {
            int width = (int) (framebufferWidth / Math.pow(2, i));
            int height = (int) (framebufferHeight / Math.pow(2, i));
            Framebuffer currentBuffer = new Framebuffer(width, height, false);

            currentBuffer.setFramebufferFilter(GL_LINEAR);
            GlStateManager.bindTexture(currentBuffer.framebufferTexture);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL14.GL_MIRRORED_REPEAT);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL14.GL_MIRRORED_REPEAT);
            GlStateManager.bindTexture(0);

            framebufferList.add(currentBuffer);
        }
    }


    public void draw(int iterations, float offset, RenderType type, float saturation) {
        if (cameraBlurQueue.isEmpty() && displayBlurQueue.isEmpty()) return;

        MainWindow window = mc.getMainWindow();
        int framebufferWidth = window.getFramebufferWidth();
        int framebufferHeight = window.getFramebufferHeight();
        quadWidth = Math.max(window.getScaledWidth(), 1);
        quadHeight = Math.max(window.getScaledHeight(), 1);

        boolean framebufferSizeChanged = inFramebuffer.framebufferWidth != framebufferWidth ||
                inFramebuffer.framebufferHeight != framebufferHeight;
        if (framebufferSizeChanged) {
            inFramebuffer.deleteFramebuffer();
            inFramebuffer = new Framebuffer(framebufferWidth, framebufferHeight, false);
        }

        if (this.iterations != iterations || framebufferSizeChanged) {
            initFramebuffers(iterations, framebufferWidth, framebufferHeight);
            this.iterations = iterations;
        } else {
            inFramebuffer.framebufferClear();
        }

        inFramebuffer.bindFramebuffer(false);
        switch (type) {
            case CAMERA -> {
                if (!cameraBlurQueue.isEmpty()) {
                    processQueue(cameraBlurQueue);
                }
                inFramebuffer.unbindFramebuffer();
                mc.getFramebuffer().bindFramebuffer(true);
                RenderHelper.disableStandardItemLighting();
            }
            case DISPLAY -> {
                if (!displayBlurQueue.isEmpty()) {
                    processQueue(displayBlurQueue);
                }
                inFramebuffer.unbindFramebuffer();

                GL11.glClearColor(0, 0, 0, 0);
                GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

                renderFBO(framebufferList.get(1), inFramebuffer.framebufferTexture, downSample, downSampleUniforms, offset);
                renderFBO(framebufferList.get(1), inFramebuffer.framebufferTexture, upSample, upSampleUniforms, offset);

                for (int i = 1; i < iterations; i++) {
                    renderFBO(framebufferList.get(i + 1), framebufferList.get(i).framebufferTexture, downSample, downSampleUniforms, offset);
                }

                for (int i = iterations; i > 1; i--) {
                    renderFBO(framebufferList.get(i - 1), framebufferList.get(i).framebufferTexture, upSample, upSampleUniforms, offset);
                }

                Framebuffer lastBuffer = framebufferList.get(0);
                lastBuffer.framebufferClear();
                lastBuffer.bindFramebuffer(false);

                upSample.attach();
                setUpSampleUniforms(lastBuffer, offset, saturation);
                bindTextures();

                drawQuads();
                upSample.detach();

                mc.getFramebuffer().bindFramebuffer(false);
                GlStateManager.bindTexture(framebufferList.get(0).framebufferTexture);
                GLUtils.startBlend();
                drawQuads();
                GLUtils.endBlend();

                GlStateManager.bindTexture(0);
                reset();
            }
        }
    }

    private void setUpSampleUniforms(Framebuffer lastBuffer, float offset, float saturation) {
        upSampleUniforms.init(upSample);
        glUniform2fARB(upSampleUniforms.offset, offset, offset);
        glUniform1i(upSampleUniforms.inTexture, 0);
        glUniform1fARB(upSampleUniforms.saturation, saturation);
        glUniform1i(upSampleUniforms.check, 1);
        glUniform1i(upSampleUniforms.textureToCheck, 16);
        glUniform2fARB(upSampleUniforms.halfpixel, 1.0f / lastBuffer.framebufferWidth, 1.0f / lastBuffer.framebufferHeight);
        glUniform2fARB(upSampleUniforms.iResolution, lastBuffer.framebufferWidth, lastBuffer.framebufferHeight);
    }

    private void bindTextures() {
        GL13.glActiveTexture(GL13.GL_TEXTURE16);
        GlStateManager.bindTexture(inFramebuffer.framebufferTexture);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GlStateManager.bindTexture(framebufferList.get(1).framebufferTexture);
    }


    private void renderFBO(Framebuffer framebuffer, int framebufferTexture, ShaderUtility shader, ShaderUniforms uniforms, float offset) {
        framebuffer.framebufferClear();
        framebuffer.bindFramebuffer(false);

        GlStateManager.bindTexture(framebufferTexture);
        shader.attach();
        uniforms.init(shader);
        glUniform2fARB(uniforms.offset, offset, offset);
        glUniform1i(uniforms.inTexture, 0);
        if (uniforms.check >= 0) {
            glUniform1i(uniforms.check, 0);
        }
        glUniform2fARB(uniforms.halfpixel, 1.0f / framebuffer.framebufferWidth, 1.0f / framebuffer.framebufferHeight);
        glUniform2fARB(uniforms.iResolution, framebuffer.framebufferWidth, framebuffer.framebufferHeight);
        GlStateManager.disableBlend();
        drawQuads();
        shader.detach();
    }

    private void drawQuads() {
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(0, 0, 0).tex(0, 1).endVertex();
        buffer.pos(0, quadHeight, 0).tex(0, 0).endVertex();
        buffer.pos(quadWidth, quadHeight, 0).tex(1, 0).endVertex();
        buffer.pos(quadWidth, 0, 0).tex(1, 1).endVertex();
        tessellator.draw();
    }

    public void addTask2D(IRenderCall callback) {
        displayBlurQueue.add(callback);
    }

    public void addTask3D(IRenderCall callback) {
        cameraBlurQueue.add(callback);
    }

    public enum RenderType {
        CAMERA, DISPLAY
    }

    public void processQueue(List<IRenderCall> queue) {
        for (IRenderCall renderCall : queue) {
            renderCall.execute();
        }
    }

    public void reset() {
        cameraBlurQueue.clear();
        displayBlurQueue.clear();
    }

    private static final class ShaderUniforms {
        private boolean initialized;
        private int offset;
        private int inTexture;
        private int check;
        private int textureToCheck;
        private int halfpixel;
        private int iResolution;
        private int saturation;

        private void init(ShaderUtility shader) {
            if (initialized) {
                return;
            }

            offset = shader.getUniform("offset");
            inTexture = shader.getUniform("inTexture");
            check = shader.getUniform("check");
            textureToCheck = shader.getUniform("textureToCheck");
            halfpixel = shader.getUniform("halfpixel");
            iResolution = shader.getUniform("iResolution");
            saturation = shader.getUniform("saturation");
            initialized = true;
        }
    }
}
