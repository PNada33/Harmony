package xd.harm.ui.display.impl.hud2;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ChatScreen;
import org.lwjgl.opengl.GL11;
import net.minecraft.util.math.vector.Vector4f;
import xd.harm.Harmony;
import xd.harm.events.render.EventDisplay;
import xd.harm.modules.api.Module;
import xd.harm.modules.impl.render.Theme;
import xd.harm.ui.display.ElementRenderer;
import xd.harm.utils.animations.Animation;
import xd.harm.utils.animations.Direction;
import xd.harm.utils.animations.easing.CompactAnimation;
import xd.harm.utils.animations.easing.Easing;
import xd.harm.utils.animations.impl.DecelerateAnimation;
import xd.harm.utils.drag.Dragging;
import xd.harm.utils.render.KawaseBlur;
import xd.harm.utils.render.rect.RenderUtility;
import xd.harm.utils.render.color.ColorUtils;
import xd.harm.utils.render.font.Fonts;
import xd.harm.utils.text.font.ClientFonts;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class KeyBindRenderer2 implements ElementRenderer {

    final Dragging dragging;
    final CompactAnimation widthAnimation = new CompactAnimation(Easing.EASE_OUT_QUART, 100);
    final Animation alfaAnimation = new DecelerateAnimation(300, 1, Direction.FORWARDS);
    float currentWidth;
    float targetWidth;
    float currentHeight;
    float targetHeight;
    boolean sizeInitialized = false;

    float animatedX = -1;
    float animatedY = -1;
    float dragSpeed = 0.15f;

    boolean isFirstRender = true;
    long lastUpdateTime = -1;

    final Map<Module, ModuleAppearAnim> appearAnimations = new HashMap<>();

    private static class ModuleAppearAnim {
        Boolean lastState = null;
        float fadeAlpha = 0f;

        void update(boolean isEnabled, float delta) {
            float d60 = delta * 60f;
            if (isEnabled) {
                fadeAlpha += (1f - fadeAlpha) * (1f - (float)Math.pow(1f - 0.15f, d60));
            } else {
                fadeAlpha += (0f - fadeAlpha) * (1f - (float)Math.pow(1f - 0.15f, d60));
            }
        }

        void checkAndUpdate(boolean currentState, float delta) {
            if (lastState == null) {
                lastState = false;
            }
            lastState = currentState;
            update(currentState, delta);
        }
    }

    private ModuleAppearAnim getOrCreateAnim(Module m) {
        return appearAnimations.computeIfAbsent(m, k -> new ModuleAppearAnim());
    }

    private void renderBlurMask(Runnable draw) {
        GL11.glEnable(0x809D);
        GL11.glEnable(0x809E);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.01f);
        draw.run();
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(0x809E);
        GL11.glDisable(0x809D);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.1f);
    }

    @Override
    public void render(EventDisplay eventDisplay) {
        MatrixStack ms = eventDisplay.getMatrixStack();

        long currentTime = System.currentTimeMillis();
        if (lastUpdateTime == -1) {
            lastUpdateTime = currentTime;
        }
        float deltaMs = currentTime - lastUpdateTime;
        lastUpdateTime = currentTime;
        float delta = deltaMs / 16.666f;
        delta = Math.max(0.001f, Math.min(delta, 5f));

        if (isFirstRender) {
            animatedX = dragging.getX();
            animatedY = dragging.getY();
            isFirstRender = false;
        }

        float targetX = dragging.getX();
        float targetY = dragging.getY();

        if (dragging.isDragging()) {
            animatedX = targetX;
            animatedY = targetY;
        } else if (Math.abs(targetX - animatedX) > 0.01f || Math.abs(targetY - animatedY) > 0.01f) {
            float lerpFactor = 1f - (float)Math.pow(1f - dragSpeed, delta);
            animatedX += (targetX - animatedX) * lerpFactor;
            animatedY += (targetY - animatedY) * lerpFactor;
            if (Math.abs(targetX - animatedX) < 0.1f) animatedX = targetX;
            if (Math.abs(targetY - animatedY) < 0.1f) animatedY = targetY;
        }

        float posX = animatedX + dragging.getWobbleX();
        float posY = animatedY + dragging.getWobbleY();
        float grabScale = dragging.getGrabScale();
        float grabRot = dragging.getWobbleAngle();

        float headerHeight = 16f;
        float itemHeight = 14f;
        float headerFontSize = 6.5f;
        float itemFontSize = 6.5f;
        float padding = 5f;
        float radius = 3.5f;

        boolean isAnyModuleEnabled = false;
        boolean isChatOpen = mc.currentScreen instanceof ChatScreen;
        List<Module> modules = Harmony.getInstance().getModuleManager().getModules();

        for (Module f : modules) {
            if (f.isVisibleInKeyBindHud() && f.getBind() != 0) {
                ModuleAppearAnim anim = getOrCreateAnim(f);
                anim.checkAndUpdate(f.isState(), delta);
            }
        }

        float totalModuleHeight = 0;
        int moduleIndex = 0;
        for (Module f : modules) {
            boolean isOn = f.isState();
            ModuleAppearAnim anim = getOrCreateAnim(f);

            if (f.isVisibleInKeyBindHud() && (isOn || anim.fadeAlpha > 0.01f) && f.getBind() != 0) {
                isAnyModuleEnabled = true;
                totalModuleHeight += itemHeight * anim.fadeAlpha;
                moduleIndex++;
            }
        }

        isAnyModuleEnabled = isAnyModuleEnabled || isChatOpen;

        Direction targetDirection = isAnyModuleEnabled ? Direction.FORWARDS : Direction.BACKWARDS;
        if (alfaAnimation.getDirection() != targetDirection) {
            alfaAnimation.setDirection(targetDirection);
            if (targetDirection == Direction.FORWARDS) {
                alfaAnimation.reset();
            }
        }
        alfaAnimation.isDone();

        float finalAlpha = (float) alfaAnimation.getOutput();
        if (finalAlpha <= 0.01) {
            sizeInitialized = false;
            return;
        }

        String headerText = "keybinds";
        float iconWidth = 8f;
        float headerTextWidth = Fonts.sfuy.getWidth(headerText, headerFontSize);
        float totalHeaderContentWidth = iconWidth + 3f + headerTextWidth;
        float minWidth = totalHeaderContentWidth + 20f;

        float maxModuleWidth = 0;
        String stateText = "[ toggled ]";
        float stateWidth = Fonts.sfuy.getWidth(stateText, itemFontSize);

        for (Module f : modules) {
            boolean isOn = f.isState();
            ModuleAppearAnim anim = getOrCreateAnim(f);

            if (f.isVisibleInKeyBindHud() && (isOn || anim.fadeAlpha > 0.01f) && f.getBind() != 0) {
                float nameWidth = Fonts.sfuy.getWidth(f.getName(), itemFontSize);
                float localWidth = padding + nameWidth + 15f + stateWidth + padding;
                if (localWidth > maxModuleWidth) {
                    maxModuleWidth = localWidth;
                }
            }
        }

        targetWidth = Math.max(minWidth, maxModuleWidth);
        boolean hasActiveModules = moduleIndex > 0;
        targetHeight = headerHeight + (hasActiveModules ? totalModuleHeight : (isChatOpen ? 0 : totalModuleHeight));

        if (!sizeInitialized) {
            currentWidth = targetWidth;
            currentHeight = targetHeight;
            sizeInitialized = true;
        } else {
            currentWidth = animateFastDelta(currentWidth, targetWidth, 8, delta);
            currentHeight = animateFastDelta(currentHeight, targetHeight, 8, delta);
        }

        boolean grab = Math.abs(grabScale - 1.0f) > 0.001f || Math.abs(grabRot) > 0.001f;

        KawaseBlur.blur.updateBlur(3.0f, 3);

        if (currentWidth > 2f && headerHeight > 2f) {
            KawaseBlur.blur.render(() -> renderBlurMask(() ->
                    RenderUtility.drawRoundedRect(posX, posY, currentWidth, currentHeight, new Vector4f(radius, radius, radius, radius), ColorUtils.rgba(255, 255, 255, 255))
            ));
        }

        if (grab) {
            GL11.glPushMatrix();
            RenderUtility.customScaledObject2D(posX, posY, currentWidth, currentHeight, grabScale);
        }

        GL11.glPushMatrix();
        GL11.glTranslatef(posX, posY, 0);
        if (Math.abs(grabRot) > 0.001f) {
            RenderUtility.customRotatedObject2D(0, 0, currentWidth, currentHeight, grabRot);
        }

        int bgAlpha = (int) (160 * finalAlpha);
        RenderUtility.drawRoundedRect(0, 0, currentWidth, currentHeight, new Vector4f(radius, radius, radius, radius), ColorUtils.rgba(25, 25, 30, bgAlpha));

        if (currentHeight > headerHeight && hasActiveModules) {
            int bodyAlpha = (int) (120 * finalAlpha);
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            MainWindow window = mc.getMainWindow();
            double scale = window.getGuiScaleFactor();
            int scissorX = (int) (posX * scale);
            int scissorY = (int) ((window.getScaledHeight() - posY - currentHeight) * scale);
            int scissorW = (int) (currentWidth * scale);
            int scissorH = (int) ((currentHeight - headerHeight) * scale);
            GL11.glScissor(scissorX, scissorY, Math.max(1, scissorW), Math.max(1, scissorH));
            RenderUtility.drawRoundedRect(0, 0, currentWidth, currentHeight, new Vector4f(radius, radius, radius, radius), ColorUtils.rgba(0, 0, 0, bodyAlpha));
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }

        RenderUtility.drawRoundedRectOutline(0, 0, currentWidth, currentHeight, radius, 0.5f, ColorUtils.rgba(255, 255, 255, (int)(30 * finalAlpha)));

        int textColor = ColorUtils.rgba(255, 255, 255, (int) (255 * finalAlpha));
        float headerContentX = (currentWidth - totalHeaderContentWidth) / 2f;

        Fonts.ico.drawText(ms, "a", headerContentX, (headerHeight - 7.5f) / 2 + 0.5f, ColorUtils.setAlpha(Theme.MainColor(0), (int)(255 * finalAlpha)), 8.5f);
        Fonts.sfuy.drawText(ms, headerText, headerContentX + iconWidth + 3f, (headerHeight - headerFontSize) / 2f + 0.5f, textColor, headerFontSize);

        if (hasActiveModules) {
            boolean useScissor = !grab;

            if (useScissor) {
                MainWindow window = mc.getMainWindow();
                double scale = window.getGuiScaleFactor();
                int scissorX = (int) (posX * scale);
                int scissorY = (int) ((window.getScaledHeight() - posY - currentHeight) * scale);
                int scissorW = (int) (currentWidth * scale);
                int scissorH = (int) ((currentHeight - headerHeight) * scale);
                GL11.glEnable(GL11.GL_SCISSOR_TEST);
                GL11.glScissor(scissorX, scissorY, Math.max(1, scissorW), Math.max(1, scissorH));
            }

            float modulePosY = headerHeight;

            for (Module f : modules) {
                boolean isOn = f.isState();
                ModuleAppearAnim anim = getOrCreateAnim(f);

                if (!f.isVisibleInKeyBindHud() || (!isOn && anim.fadeAlpha <= 0.01f) || f.getBind() == 0) continue;

                float animAlpha = anim.fadeAlpha * finalAlpha;
                if (animAlpha < 0.01f) {
                    modulePosY += itemHeight * anim.fadeAlpha;
                    continue;
                }

                String nameText = f.getName();

                int nameColor = ColorUtils.rgba(255, 255, 255, (int) (255 * animAlpha));
                int stateColor = ColorUtils.rgba(160, 160, 160, (int) (255 * animAlpha));

                float textY = modulePosY + (itemHeight - itemFontSize) / 2f;

                Fonts.sfuy.drawText(ms, nameText, padding, textY, nameColor, itemFontSize);
                Fonts.sfuy.drawText(ms, stateText, currentWidth - padding - stateWidth, textY, stateColor, itemFontSize);

                modulePosY += itemHeight * anim.fadeAlpha;
            }

            if (useScissor) {
                GL11.glDisable(GL11.GL_SCISSOR_TEST);
            }
        }

        GL11.glPopMatrix();

        if (grab) {
            GL11.glPopMatrix();
        }

        widthAnimation.run(targetWidth);
        currentWidth = (float) widthAnimation.getValue();
        dragging.setWidth(currentWidth);
        dragging.setHeight(currentHeight);
    }

    private float animateFastDelta(float current, float target, float speed, float delta) {
        float diff = target - current;
        if (Math.abs(diff) < 0.1f) return target;
        return current + diff / speed * delta;
    }
}
