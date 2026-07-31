package xd.harm.ui.display.impl.hud2;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.MainWindow;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.PotionSpriteUploader;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.math.vector.Vector4f;
import org.lwjgl.opengl.GL11;
import xd.harm.events.render.EventDisplay;
import xd.harm.ui.display.ElementRenderer;
import xd.harm.modules.impl.render.Theme;
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

import java.util.HashMap;
import java.util.Map;

@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class PotionRenderer2 implements ElementRenderer {

    final Dragging dragging;
    final CompactAnimation widthAnimation = new CompactAnimation(Easing.EASE_OUT_QUART, 100);
    final Animation alphaAnimation = new DecelerateAnimation(300, 1, Direction.FORWARDS);
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

    final Map<Effect, PotionAppearAnim> appearAnimations = new HashMap<>();

    private static class PotionAppearAnim {
        Boolean lastState = null;
        float fadeAlpha = 0f;

        void update(boolean isActive, float delta) {
            float d60 = delta * 60f;
            if (isActive) {
                fadeAlpha += (1f - fadeAlpha) * (1f - (float) Math.pow(1f - 0.15f, d60));
            } else {
                fadeAlpha += (0f - fadeAlpha) * (1f - (float) Math.pow(1f - 0.15f, d60));
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

    private PotionAppearAnim getOrCreateAnim(Effect effect) {
        return appearAnimations.computeIfAbsent(effect, k -> new PotionAppearAnim());
    }

    private void renderBlurMask(Runnable draw) {
        GL11.glEnable(0x809D);
        GL11.glEnable(0x809E);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.01f);
        try {
            draw.run();
        } finally {
            GL11.glDisable(0x809E);
            GL11.glDisable(0x809D);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.1f);
            RenderSystem.enableAlphaTest();
        }
    }

    private void restoreRenderState() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glDisable(GL11.GL_STENCIL_TEST);
        GL11.glDisable(0x809E);
        GL11.glDisable(0x809D);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.1f);
        RenderSystem.enableTexture();
        RenderSystem.enableAlphaTest();
        RenderSystem.defaultAlphaFunc();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private String formatPotionDuration(EffectInstance effect) {
        int duration = effect.getDuration();
        int totalSeconds = duration / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        if (totalSeconds >= 3600) {
            return "**:**";
        }
        return minutes + ":" + (seconds < 10 ? "0" : "") + seconds;
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
            float lerpFactor = 1f - (float) Math.pow(1f - dragSpeed, delta);
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
        float itemHeight = 12f;
        float headerFontSize = 6.5f;
        float itemFontSize = 5.5f;
        float padding = 5f;
        float radius = 3.5f;
        float effectIconSize = 7f;
        float iconPadding = 2.5f;

        boolean isAnyPotionActive = false;
        boolean isChatOpen = mc.currentScreen instanceof ChatScreen;

        for (EffectInstance ef : mc.player.getActivePotionEffects()) {
            if (ef.getDuration() > 0) {
                PotionAppearAnim anim = getOrCreateAnim(ef.getPotion());
                anim.checkAndUpdate(true, delta);
            }
        }

        for (Map.Entry<Effect, PotionAppearAnim> entry : appearAnimations.entrySet()) {
            boolean stillActive = false;
            for (EffectInstance ef : mc.player.getActivePotionEffects()) {
                if (ef.getPotion() == entry.getKey() && ef.getDuration() > 0) {
                    stillActive = true;
                    break;
                }
            }
            if (!stillActive) {
                entry.getValue().checkAndUpdate(false, delta);
            }
        }

        float totalPotionHeight = 0;
        int potionIndex = 0;
        for (EffectInstance ef : mc.player.getActivePotionEffects()) {
            if (ef.getDuration() > 0) {
                PotionAppearAnim anim = getOrCreateAnim(ef.getPotion());
                if (anim.fadeAlpha > 0.01f) {
                    isAnyPotionActive = true;
                    totalPotionHeight += itemHeight * anim.fadeAlpha;
                    potionIndex++;
                }
            }
        }

        for (Map.Entry<Effect, PotionAppearAnim> entry : appearAnimations.entrySet()) {
            PotionAppearAnim anim = entry.getValue();
            if (anim.fadeAlpha > 0.01f && anim.lastState != null && !anim.lastState) {
                isAnyPotionActive = true;
                totalPotionHeight += itemHeight * anim.fadeAlpha;
            }
        }

        isAnyPotionActive = isAnyPotionActive || isChatOpen;

        Direction targetDirection = isAnyPotionActive ? Direction.FORWARDS : Direction.BACKWARDS;
        if (alphaAnimation.getDirection() != targetDirection) {
            alphaAnimation.setDirection(targetDirection);
            if (targetDirection == Direction.FORWARDS) {
                alphaAnimation.reset();
            }
        }
        alphaAnimation.isDone();

        float finalAlpha = (float) alphaAnimation.getOutput();
        if (finalAlpha <= 0.01) {
            sizeInitialized = false;
            return;
        }

        String headerText = "potions";
        float iconWidth = 8f;
        float headerTextWidth = Fonts.sfuy.getWidth(headerText, headerFontSize);
        float totalHeaderContentWidth = iconWidth + 3f + headerTextWidth;
        float minWidth = totalHeaderContentWidth + 20f;

        float maxPotionWidth = 0;
        for (EffectInstance ef : mc.player.getActivePotionEffects()) {
            if (ef.getDuration() > 0) {
                PotionAppearAnim anim = getOrCreateAnim(ef.getPotion());
                if (anim.fadeAlpha > 0.01f) {
                    int amp = ef.getAmplifier();
                    String ampStr = amp >= 1 && amp <= 9 ? " " + I18n.format("enchantment.level." + (amp + 1)) : "";
                    String nameText = I18n.format(ef.getEffectName()) + ampStr;
                    String timerText = formatPotionDuration(ef);

                    float nameWidth = Fonts.sfuy.getWidth(nameText, itemFontSize);
                    float timerWidth = Fonts.sfuy.getWidth(timerText, itemFontSize);
                    float localWidth = padding + effectIconSize + iconPadding + nameWidth + 8f + timerWidth + padding;
                    if (localWidth > maxPotionWidth) {
                        maxPotionWidth = localWidth;
                    }
                }
            }
        }

        targetWidth = Math.max(minWidth, maxPotionWidth);
        boolean hasActivePotions = potionIndex > 0;
        targetHeight = headerHeight + (hasActivePotions ? totalPotionHeight : (isChatOpen ? 0 : totalPotionHeight));

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

        if (currentHeight > headerHeight && hasActivePotions) {
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

        RenderUtility.drawRoundedRectOutline(0, 0, currentWidth, currentHeight, radius, 0.5f, ColorUtils.rgba(255, 255, 255, (int) (30 * finalAlpha)));

        int textColor = ColorUtils.rgba(255, 255, 255, (int) (255 * finalAlpha));
        float headerContentX = (currentWidth - totalHeaderContentWidth) / 2f;

        Fonts.ico.drawText(ms, "f", headerContentX, (headerHeight - 7.5f) / 2 + 0.5f, ColorUtils.setAlpha(Theme.MainColor(0), (int) (255 * finalAlpha)), 8.5f);
        Fonts.sfuy.drawText(ms, headerText, headerContentX + iconWidth + 1f, (headerHeight - headerFontSize) / 2f + 0.5f, textColor, headerFontSize);

        if (hasActivePotions) {
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

            float potionPosY = headerHeight;
            PotionSpriteUploader potionSpriteUploader = mc.getPotionSpriteUploader();

            for (EffectInstance ef : mc.player.getActivePotionEffects()) {
                if (ef.getDuration() <= 0) continue;

                PotionAppearAnim anim = getOrCreateAnim(ef.getPotion());

                if (anim.fadeAlpha <= 0.01f) continue;

                float animAlpha = anim.fadeAlpha * finalAlpha;
                if (animAlpha < 0.01f) {
                    potionPosY += itemHeight * anim.fadeAlpha;
                    continue;
                }

                int amp = ef.getAmplifier();
                String ampStr = amp >= 1 && amp <= 9 ? " " + I18n.format("enchantment.level." + (amp + 1)) : "";
                String nameText = I18n.format(ef.getEffectName()) + ampStr;
                String timerText = formatPotionDuration(ef);

                float iconX = padding;
                float iconY = potionPosY + (itemHeight - effectIconSize) / 2;

                Effect effect = ef.getPotion();
                TextureAtlasSprite sprite = potionSpriteUploader.getSprite(effect);
                RenderSystem.color4f(1.0f, 1.0f, 1.0f, animAlpha);
                drawIcon(sprite, iconX, iconY, effectIconSize, effectIconSize);

                float textX = iconX + effectIconSize + iconPadding;
                float textY = potionPosY + (itemHeight - itemFontSize) / 2f;

                float blinkAlpha = 1f;
                if (ef.getDuration() < 200) {
                    blinkAlpha = (float) (Math.sin(System.currentTimeMillis() / 200.0) * 0.3 + 0.7);
                }

                int nameColor;
                int timerColor;
                if (!ef.getPotion().isBeneficial()) {
                    nameColor = ColorUtils.rgba(255, 120, 120, (int) (255 * animAlpha * blinkAlpha));
                    timerColor = ColorUtils.rgba(255, 100, 100, (int) (255 * animAlpha * blinkAlpha));
                } else {
                    nameColor = ColorUtils.rgba(255, 255, 255, (int) (255 * animAlpha * blinkAlpha));
                    timerColor = ColorUtils.rgba(160, 160, 160, (int) (255 * animAlpha * blinkAlpha));
                }

                Fonts.sfuy.drawText(ms, nameText, textX, textY, nameColor, itemFontSize);

                float timerWidth = Fonts.sfuy.getWidth(timerText, itemFontSize);
                Fonts.sfuy.drawText(ms, timerText, currentWidth - padding - timerWidth, textY, timerColor, itemFontSize);

                potionPosY += itemHeight * anim.fadeAlpha;
            }

            if (useScissor) {
                GL11.glDisable(GL11.GL_SCISSOR_TEST);
            }
        }

        GL11.glPopMatrix();

        if (grab) {
            GL11.glPopMatrix();
        }

        restoreRenderState();

        widthAnimation.run(targetWidth);
        currentWidth = (float) widthAnimation.getValue();
        dragging.setWidth(currentWidth);
        dragging.setHeight(currentHeight);
    }

    private void drawIcon(TextureAtlasSprite sprite, float x, float y, float width, float height) {
        if (sprite == null) return;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        mc.getTextureManager().bindTexture(sprite.getAtlasTexture().getTextureLocation());
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        bufferBuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
        bufferBuilder.pos(x, y + height, 0).tex(sprite.getMinU(), sprite.getMaxV()).endVertex();
        bufferBuilder.pos(x + width, y + height, 0).tex(sprite.getMaxU(), sprite.getMaxV()).endVertex();
        bufferBuilder.pos(x + width, y, 0).tex(sprite.getMaxU(), sprite.getMinV()).endVertex();
        bufferBuilder.pos(x, y, 0).tex(sprite.getMinU(), sprite.getMinV()).endVertex();
        tessellator.draw();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableTexture();
        RenderSystem.enableAlphaTest();
        RenderSystem.disableBlend();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private float animateFastDelta(float current, float target, float speed, float delta) {
        float diff = target - current;
        if (Math.abs(diff) < 0.1f) return target;
        return current + diff / speed * delta;
    }
}

