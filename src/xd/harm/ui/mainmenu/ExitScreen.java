package xd.harm.ui.mainmenu;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.ITextComponent;
import xd.harm.utils.render.color.ColorUtils;
import xd.harm.utils.text.font.ClientFonts;
import org.lwjgl.opengl.GL11;

import static xd.harm.utils.client.IMinecraft.mc;

public class ExitScreen extends Screen {
    private final long startTime;


    private static final float ENTRANCE_DURATION = 0.8f;
    private static final float HOLD_DURATION = 0.6f;
    private static final float EXIT_DURATION = 1.5f;

    private float currentPhase = 0f;
    private boolean isExiting = false;

    public ExitScreen() {
        super(ITextComponent.getTextComponentOrEmpty(""));
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {

        GL11.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

        long elapsedTime = System.currentTimeMillis() - startTime;
        float totalElapsed = elapsedTime / 1000.0f;


        float entranceProgress = 0f;
        float exitProgress = 0f;

        if (totalElapsed < ENTRANCE_DURATION) {
            entranceProgress = totalElapsed / ENTRANCE_DURATION;
            currentPhase = entranceProgress;
            isExiting = false;
        } else if (totalElapsed < ENTRANCE_DURATION + HOLD_DURATION) {
            entranceProgress = 1.0f;
            currentPhase = 1.0f;
            isExiting = false;
        } else {
            entranceProgress = 1.0f;
            float exitStart = totalElapsed - ENTRANCE_DURATION - HOLD_DURATION;
            exitProgress = Math.min(1.0f, exitStart / EXIT_DURATION);
            currentPhase = 1.0f - exitProgress;
            isExiting = true;

            if (exitProgress >= 1.0f) {
                Minecraft.getInstance().shutdownMinecraftApplet();
                return;
            }
        }

        float centerX = this.width / 2f;
        float centerY = this.height / 2f;


        float textAlpha, textScale;

        if (isExiting) {
            textScale = 1.0f + exitProgress * 0.4f;
            textAlpha = easeInCubic(1.0f - exitProgress);
        } else {
            textScale = 0.8f + easeOutCubic(entranceProgress) * 0.2f;
            textAlpha = easeOutCubic(entranceProgress);
        }

        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        matrixStack.push();
        matrixStack.translate(centerX, centerY, 0);
        matrixStack.scale(textScale, textScale, 1.0f);
        matrixStack.translate(-centerX, -centerY, 0);

        ClientFonts.interMedium[32].drawCenteredString(
                matrixStack,
                "До встречи.",
                centerX,
                centerY - 16,
                ColorUtils.rgba(255, 255, 255, (int) (255 * textAlpha))
        );

        matrixStack.pop();


        if (entranceProgress > 0.3f && textAlpha > 0.1f) {
            float lineProgress = Math.min(1.0f, (entranceProgress - 0.3f) / 0.7f);
            float lineWidth = 150 * easeOutCubic(lineProgress) * textAlpha;
            float lineY = centerY + 10;

            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glShadeModel(GL11.GL_SMOOTH);

            GL11.glBegin(GL11.GL_QUADS);
            GL11.glColor4f(0.3f, 0.6f, 1.0f, 0.0f);
            GL11.glVertex2f(centerX - lineWidth / 2, lineY);
            GL11.glVertex2f(centerX - lineWidth / 2, lineY + 2);
            GL11.glColor4f(0.3f, 0.6f, 1.0f, textAlpha * 0.8f);
            GL11.glVertex2f(centerX, lineY + 2);
            GL11.glVertex2f(centerX, lineY);

            GL11.glColor4f(0.3f, 0.6f, 1.0f, textAlpha * 0.8f);
            GL11.glVertex2f(centerX, lineY);
            GL11.glVertex2f(centerX, lineY + 2);
            GL11.glColor4f(0.3f, 0.6f, 1.0f, 0.0f);
            GL11.glVertex2f(centerX + lineWidth / 2, lineY + 2);
            GL11.glVertex2f(centerX + lineWidth / 2, lineY);
            GL11.glEnd();

            GL11.glShadeModel(GL11.GL_FLAT);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
        }


        if (isExiting && exitProgress > 0.6f) {
            float fadeAlpha = (exitProgress - 0.6f) / 0.4f;
            fadeAlpha = easeInCubic(fadeAlpha) * 0.95f;

            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glColor4f(0.0f, 0.0f, 0.0f, fadeAlpha);
            GL11.glVertex2f(0, 0);
            GL11.glVertex2f(0, height);
            GL11.glVertex2f(width, height);
            GL11.glVertex2f(width, 0);
            GL11.glEnd();
            GL11.glEnable(GL11.GL_TEXTURE_2D);
        }

        GlStateManager.disableBlend();
    }

    private float easeOutCubic(float t) {
        return 1 - (float) Math.pow(1 - t, 3);
    }

    private float easeInCubic(float t) {
        return t * t * t;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return true;
    }
}