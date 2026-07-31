package xd.harm.utils.render;

import com.mojang.blaze3d.platform.GlStateManager;
import xd.harm.utils.client.IMinecraft;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;

import static org.lwjgl.opengl.GL11.GL_QUADS;

public class CustomFramebuffer extends Framebuffer implements IMinecraft {

    private boolean linear;

    public CustomFramebuffer(boolean useDepthIn) {
        super(1, 1, useDepthIn, Minecraft.IS_RUNNING_ON_MAC);
    }

    public void resizeFramebuffer(Framebuffer framebuffer) {
        Minecraft mc = Minecraft.getInstance();
        if (framebuffer.framebufferWidth != mc.getMainWindow().getWidth() || framebuffer.framebufferHeight != mc.getMainWindow().getFramebufferHeight()) {
            framebuffer.createBuffers(Math.max(mc.getMainWindow().getWidth(), 1), Math.max(mc.getMainWindow().getFramebufferHeight(), 1), Minecraft.IS_RUNNING_ON_MAC);
        }
    }


    public static void drawTexture() {
        drawTexture(Math.max(mc.getMainWindow().getScaledWidth(), 1), Math.max(mc.getMainWindow().getScaledHeight(), 1));
    }

    public static void drawTexture(float width, float height) {
        final BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
        final Tessellator tessellator = Tessellator.getInstance();
        bufferBuilder.begin(GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        bufferBuilder.pos(0, 0, 0).tex(0, 1).endVertex();
        bufferBuilder.pos(0, height, 0).tex(0, 0).endVertex();
        bufferBuilder.pos(width, height, 0).tex(1, 0).endVertex();
        bufferBuilder.pos(width, 0, 0).tex(1, 1).endVertex();
        tessellator.draw();
    }

    public static void flipQuads() {
        double width = mc.getMainWindow().getScaledWidth();
        double height = mc.getMainWindow().getScaledHeight();
        flipQuads(width, height);
    }

    public static void flipQuads(double width, double height) {
        flipQuads(0, 0, width, height);
    }

    public static void flipQuads(double x, double y, double width, double height) {
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        Tessellator tessellator = Tessellator.getInstance();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        buffer.pos((float) x, (float) y, 0).tex(0, 1).endVertex();
        buffer.pos((float) x, (float) (y + height), 0).tex(0, 0).endVertex();
        buffer.pos((float) (x + width), (float) (y + height), 0).tex(1, 0).endVertex();
        buffer.pos((float) (x + width), (float) y, 0).tex(1, 1).endVertex();
        tessellator.draw();
    }

    public void setup(boolean clear) {
        resizeFramebuffer(this);
        if (clear)
            this.framebufferClear(Minecraft.IS_RUNNING_ON_MAC);
        this.bindFramebuffer(true);
    }

    public void stop() {
        unbindFramebuffer();
        Minecraft.getInstance().getFramebuffer().bindFramebuffer(true);
    }


    public void draw() {
        this.bindFramebufferTexture();
        drawTexture();
    }

    public void draw(float width, float height) {
        this.bindFramebufferTexture();
        drawTexture(width, height);
    }

    public void draw(int color) {
        this.bindFramebufferTexture();
        drawTexture(color);
    }

    public void draw(Framebuffer bFramebuffer) {
        bFramebuffer.bindFramebufferTexture();
        drawTexture();
    }

    public static void drawTexture(int color) {
        Minecraft mc = Minecraft.getInstance();
        MainWindow sr = mc.getMainWindow();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        float width = (float) sr.getScaledWidth();
        float height = (float) sr.getScaledHeight();


        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
        bufferBuilder.pos(0, 0, 0).color(color).tex(0, 1).endVertex();
        bufferBuilder.pos(0, height, 0).color(color).tex(0, 0).endVertex();
        bufferBuilder.pos(width, height, 0).color(color).tex(1, 0).endVertex();
        bufferBuilder.pos(width, 0, 0).color(color).tex(1, 1).endVertex();
        tessellator.draw();
    }

    public CustomFramebuffer setLinear() {
        this.linear = true;
        return this;
    }

    public void setFramebufferFilter(int framebufferFilterIn) {
        super.setFramebufferFilter(this.linear ? 9729 : framebufferFilterIn);
    }

    public void setup() {
        resizeFramebuffer(this);
        this.bindFramebuffer(true);
        GlStateManager.clearColor(this.framebufferColor[0], this.framebufferColor[1], this.framebufferColor[2], this.framebufferColor[3]);
        int mask = 16384;

        if (this.useDepth) {
            GlStateManager.clearDepth(1.0D);
            mask |= 256;
        }

        GlStateManager.clear(mask, Minecraft.IS_RUNNING_ON_MAC);
    }

}
