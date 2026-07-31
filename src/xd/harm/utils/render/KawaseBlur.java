package xd.harm.utils.render;

import xd.harm.utils.render.gl.Stencil;
import xd.harm.utils.shader.ShaderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MainWindow;
import net.minecraft.client.shader.Framebuffer;

import static org.lwjgl.opengl.ARBShaderObjects.glUniform1fARB;
import static org.lwjgl.opengl.ARBShaderObjects.glUniform2fARB;

public class KawaseBlur {

    public static KawaseBlur blur = new KawaseBlur();

    public final CustomFramebuffer BLURRED;
    public final CustomFramebuffer ADDITIONAL;
    private final CustomFramebuffer[] buffers;
    private final int kawaseDownOffset;
    private final int kawaseDownResolution;
    private final int kawaseUpOffset;
    private final int kawaseUpResolution;

    public KawaseBlur() {
        BLURRED = new CustomFramebuffer(false).setLinear();
        ADDITIONAL = new CustomFramebuffer(false).setLinear();
        buffers = new CustomFramebuffer[] {ADDITIONAL, BLURRED};
        kawaseDownOffset = ShaderUtil.kawaseDown.getUniform("offset");
        kawaseDownResolution = ShaderUtil.kawaseDown.getUniform("resolution");
        kawaseUpOffset = ShaderUtil.kawaseUp.getUniform("offset");
        kawaseUpResolution = ShaderUtil.kawaseUp.getUniform("resolution");

    }

    public void render(Runnable run) {

        Stencil.initStencilToWrite();
        run.run();
        Stencil.readStencilBuffer(1);
        BLURRED.draw();
        Stencil.uninitStencilBuffer();
    }
    public void updateBlur(float offset, int steps) {

        Minecraft mc = Minecraft.getInstance();
        MainWindow mainWindow = mc.getMainWindow();
        Framebuffer mcFramebuffer = mc.getFramebuffer();
        float width = Math.max(mainWindow.getScaledWidth(), 1);
        float height = Math.max(mainWindow.getScaledHeight(), 1);
        float invWidth = 1f / mainWindow.getWidth();
        float invHeight = 1f / mainWindow.getHeight();

        ADDITIONAL.setup();
        mcFramebuffer.bindFramebufferTexture();
        ShaderUtil.kawaseDown.attach();
        glUniform1fARB(kawaseDownOffset, offset);
        glUniform2fARB(kawaseDownResolution, invWidth, invHeight);
        CustomFramebuffer.drawTexture(width, height);

        for (int i = 1; i < steps; ++i) {
            int step = i % 2;
            buffers[step].setup();
            buffers[(step + 1) % 2].draw(width, height);
        }

        ShaderUtil.kawaseUp.attach();
        glUniform1fARB(kawaseUpOffset, offset);
        glUniform2fARB(kawaseUpResolution, invWidth, invHeight);

        for (int i = 0; i < steps; ++i) {
            int step = i % 2;
            buffers[(step + 1) % 2].setup();
            buffers[step].draw(width, height);
        }

        ShaderUtil.kawaseUp.detach();
        mcFramebuffer.bindFramebuffer(false);
    }

}
