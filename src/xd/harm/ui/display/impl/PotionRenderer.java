package xd.harm.ui.display.impl;

import xd.harm.events.render.EventDisplay;
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
import xd.harm.utils.text.GradientUtil;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.MainWindow;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.client.renderer.texture.PotionSpriteUploader;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.math.vector.Vector4f;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.ITextComponent;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;

@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class PotionRenderer implements ElementRenderer {

    final Dragging dragging;
    private final CompactAnimation widthAnimation = new CompactAnimation(Easing.EASE_OUT_QUART, 100);
    final Animation alfaAnimation = new DecelerateAnimation(300, 1, Direction.FORWARDS);
    float currentWidth;
    float targetWidth;
    float currentHeight;
    float targetHeight;
    boolean sizeInitialized = false;

    float[] colorShiftOffsets = new float[4];
    float[] colorShiftSpeeds = {0.00025f, 0.00018f, 0.00022f, 0.00015f};

    float animatedX = -1;
    float animatedY = -1;
    float dragSpeed = 0.15f;

    float[] potionAnimValues = new float[100];
    Animation[] nameTextAnimations = new Animation[100];
    Animation[] timerTextAnimations = new Animation[100];
    boolean[] potionWasActive = new boolean[100];
    boolean[] animationsInitialized = new boolean[100];
    boolean isFirstRender = true;
    long lastUpdateTime = -1;

    private final Map<Effect, PotionAppearAnim> appearAnimations = new HashMap<>();

    private static class PotionAppearAnim {
        long startTime = 0;
        Boolean lastState = null;
        float glowIntensity = 0f;
        float bounceScale = 0f;
        float typewriterProgress = 0f;
        float nameSlideX = -30f;
        float timerSlideX = 30f;
        float colorFlash = 0f;
        float lineProgress = 0f;
        float lineGlow = 0f;
        float fadeAlpha = 0f;
        float scaleY = 0f;
        float rotationAngle = 0f;
        float colorDesaturate = 0f;
        boolean initialized = false;

        void reset() {
            startTime = System.currentTimeMillis();
            glowIntensity = 1.5f;
            bounceScale = 0f;
            typewriterProgress = 0f;
            nameSlideX = -30f;
            timerSlideX = 30f;
            colorFlash = 1f;
            lineProgress = 0f;
            lineGlow = 1.5f;
            fadeAlpha = 0f;
            scaleY = 0f;
            rotationAngle = -3f;
            colorDesaturate = 0f;
        }

        void update(boolean isActive, float delta) {
            float d60 = delta * 60f;
            if (isActive) {
                glowIntensity = Math.max(0f, glowIntensity - 0.04f * d60);
                lineGlow = Math.max(0f, lineGlow - 0.04f * d60);
                if (lineProgress < 1f && lineGlow > 1f) {
                    lineProgress = Math.min(1f, lineProgress + 0.1f * d60);
                } else {
                    lineProgress = Math.max(0f, lineProgress - 0.03f * d60);
                }
                bounceScale += (1f - bounceScale) * (1f - (float) Math.pow(1f - 0.12f, d60));
                typewriterProgress = Math.min(1f, typewriterProgress + 0.05f * d60);
                nameSlideX += (0f - nameSlideX) * (1f - (float) Math.pow(1f - 0.12f, d60));
                timerSlideX += (0f - timerSlideX) * (1f - (float) Math.pow(1f - 0.12f, d60));
                colorFlash = Math.max(0f, colorFlash - 0.025f * d60);
                fadeAlpha += (1f - fadeAlpha) * (1f - (float) Math.pow(1f - 0.1f, d60));
                scaleY += (1f - scaleY) * (1f - (float) Math.pow(1f - 0.15f, d60));
                rotationAngle += (0f - rotationAngle) * (1f - (float) Math.pow(1f - 0.12f, d60));
                colorDesaturate = Math.max(0f, colorDesaturate - 0.05f * d60);
            } else {
                fadeAlpha += (0f - fadeAlpha) * (1f - (float) Math.pow(1f - 0.04f, d60));
                glowIntensity = Math.max(0f, glowIntensity - 0.05f * d60);
                lineGlow = Math.max(0f, lineGlow - 0.02f * d60);
                lineProgress = Math.max(0f, lineProgress - 0.025f * d60);
                bounceScale += (0.8f - bounceScale) * (1f - (float) Math.pow(1f - 0.03f, d60));
                if (fadeAlpha < 0.5f) {
                    bounceScale += (0f - bounceScale) * (1f - (float) Math.pow(1f - 0.025f, d60));
                }
                scaleY += (0.3f - scaleY) * (1f - (float) Math.pow(1f - 0.03f, d60));
                if (fadeAlpha < 0.3f) {
                    scaleY += (0f - scaleY) * (1f - (float) Math.pow(1f - 0.02f, d60));
                }
                nameSlideX += (-50f - nameSlideX) * (1f - (float) Math.pow(1f - 0.035f, d60));
                timerSlideX += (50f - timerSlideX) * (1f - (float) Math.pow(1f - 0.035f, d60));
                typewriterProgress = Math.max(0f, typewriterProgress - 0.02f * d60);
                colorFlash = 0f;
                colorDesaturate = Math.min(1f, colorDesaturate + 0.025f * d60);
                rotationAngle += (4f - rotationAngle) * (1f - (float) Math.pow(1f - 0.025f, d60));
            }
        }

        boolean isFullyHidden() {
            return fadeAlpha < 0.01f && !lastState;
        }

        void checkAndUpdate(boolean currentState, float delta) {
            if (lastState == null) {
                lastState = false;
                if (currentState) {
                    reset();
                }
            } else if (currentState && !lastState) {
                reset();
            }
            lastState = currentState;
            update(currentState, delta);
        }
    }

    private PotionAppearAnim getOrCreateAnim(Effect effect) {
        return appearAnimations.computeIfAbsent(effect, k -> new PotionAppearAnim());
    }

    private int desaturateColor(int color, float amount) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int gray = (int) (r * 0.3f + g * 0.59f + b * 0.11f);
        r = (int) (r + (gray - r) * amount);
        g = (int) (g + (gray - g) * amount);
        b = (int) (b + (gray - b) * amount);
        return (a << 24) | (r << 16) | (g << 8) | b;
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

        float wobbleX = dragging.getWobbleX();
        float wobbleY = dragging.getWobbleY();
        float grabScale = dragging.getGrabScale();
        float grabRot = dragging.getWobbleAngle();

        float posX = animatedX + wobbleX;
        float posY = animatedY + wobbleY;

        float titleFontSize = 7f;
        float potionFontSize = 6f;
        float timerFontSize = 6f;
        float padding = 0.8f;
        float iconWidth = 14;
        float iconPadding = 1;
        float sidePadding = 3.5f;
        float rectHeight = 13;
        float moduleRectHeight = 11;
        float cornerRadius = 3.5f;
        float effectIconSize = 8;
        float headPadding = 2.5f;
        float iconGap = 3.5f;

        ITextComponent name = GradientUtil.gradient("Potions");
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
                isAnyPotionActive = true;
                float effectiveAnim = anim.fadeAlpha;
                if (potionIndex < potionAnimValues.length) {
                    potionAnimValues[potionIndex] = effectiveAnim;
                }
                potionIndex++;
                totalPotionHeight += (moduleRectHeight + padding) * effectiveAnim;
            }
        }

        for (Map.Entry<Effect, PotionAppearAnim> entry : appearAnimations.entrySet()) {
            PotionAppearAnim anim = entry.getValue();
            if (anim.fadeAlpha > 0.01f && anim.lastState != null && !anim.lastState) {
                isAnyPotionActive = true;
                totalPotionHeight += (moduleRectHeight + padding) * anim.fadeAlpha;
            }
        }

        isAnyPotionActive = isAnyPotionActive || isChatOpen;

        Direction targetDirection = isAnyPotionActive ? Direction.FORWARDS : Direction.BACKWARDS;
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

        float textWidth = Fonts.sfuy.getWidth("Potions", titleFontSize);
        float headerWidth = iconWidth + iconPadding + textWidth + sidePadding * 2;

        float maxPotionWidth = 0;
        for (EffectInstance ef : mc.player.getActivePotionEffects()) {
            if (ef.getDuration() > 0) {
                int amp = ef.getAmplifier();
                String ampStr = amp >= 1 && amp <= 9 ? " " + I18n.format("enchantment.level." + (amp + 1)) : "";
                String nameText = I18n.format(ef.getEffectName()) + ampStr;
                String timerText = formatPotionDuration(ef);
                float nameWidth = Fonts.sfuy.getWidth(nameText, potionFontSize);
                float timerWidth = Fonts.sfuy.getWidth(timerText, timerFontSize);
                float localWidth = sidePadding + effectIconSize + headPadding + nameWidth + iconGap + timerWidth + sidePadding;
                if (localWidth > maxPotionWidth) {
                    maxPotionWidth = localWidth;
                }
            }
        }

        targetWidth = Math.max(headerWidth, maxPotionWidth);

        boolean hasActivePotions = potionIndex > 0;
        targetHeight = rectHeight + (hasActivePotions ? totalPotionHeight : (isChatOpen ? 0 : totalPotionHeight));

        if (!sizeInitialized) {
            currentWidth = targetWidth;
            currentHeight = targetHeight;
            sizeInitialized = true;
        } else {
            currentWidth = animateFastDelta(currentWidth, targetWidth, 8, delta);
            currentHeight = animateFastDelta(currentHeight, targetHeight, 8, delta);
        }

        updateColorAnimation(delta);

        int themeColor1 = Theme.MainColor(0);
        int themeColor2 = Theme.MainColor(1);

        boolean grab = Math.abs(grabScale - 1.0f) > 0.001f || Math.abs(grabRot) > 0.001f;

        if (currentWidth > 2f && currentHeight > 2f && !grab) {
            float blurRadius = cornerRadius + 0.75f;
            float blurInset = 0.35f;
            KawaseBlur.blur.render(() -> renderBlurMask(() ->
                    RenderUtility.drawRoundedRect(posX + blurInset, posY + blurInset,
                            currentWidth - blurInset * 2f, currentHeight - blurInset * 2f,
                            new Vector4f(blurRadius, blurRadius, blurRadius, blurRadius),
                            ColorUtils.rgba(255, 255, 255, 255))
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

        int wmBgAlpha = Math.max(1, (int) (150 * finalAlpha));
        RenderUtility.drawRoundedRect(0, 0, currentWidth, currentHeight,
                new Vector4f(cornerRadius, cornerRadius, cornerRadius, cornerRadius),
                ColorUtils.rgba(0, 0, 0, wmBgAlpha));

        int logoColor = ColorUtils.interpolateColor(themeColor1, ColorUtils.rgb(255, 255, 255), 0.2f);
        logoColor = ColorUtils.setAlpha(logoColor, (int) (255 * finalAlpha));
        Fonts.ico.drawText(ms, "f", sidePadding, (rectHeight - 7.5f) / 2 + 0.5f, logoColor, 8.5f);

        float xOffset = iconWidth + iconPadding;
        for (ITextComponent sibling : name.getSiblings()) {
            String charText = sibling.getString();
            Color color = sibling.getStyle().getColor();
            int colorInt = color != null ? color.getColor() : ColorUtils.rgb(255, 255, 255);
            colorInt = ColorUtils.interpolateColor(colorInt, ColorUtils.rgb(255, 255, 255), 0.15f);
            colorInt = ColorUtils.setAlpha(colorInt, (int) (255 * finalAlpha));
            Fonts.sfuy.drawText(ms, charText, xOffset, (rectHeight - titleFontSize) / 2, colorInt, titleFontSize);
            xOffset += Fonts.sfuy.getWidth(charText, titleFontSize);
        }

        if (hasActivePotions) {
            boolean useScissor = !grab;

            if (useScissor) {
                MainWindow window = mc.getMainWindow();
                double scale = window.getGuiScaleFactor();
                int scissorX = (int) (posX * scale);
                int scissorY = (int) ((window.getScaledHeight() - posY - currentHeight) * scale);
                int scissorW = (int) (currentWidth * scale);
                int scissorH = (int) (currentHeight * scale);
                GL11.glEnable(GL11.GL_SCISSOR_TEST);
                GL11.glScissor(scissorX, scissorY, Math.max(1, scissorW), Math.max(1, scissorH));
            }

            float effectY = rectHeight;
            potionIndex = 0;
            PotionSpriteUploader potionspriteuploader = mc.getPotionSpriteUploader();

            for (EffectInstance ef : mc.player.getActivePotionEffects()) {
                if (ef.getDuration() <= 0) continue;

                PotionAppearAnim anim = getOrCreateAnim(ef.getPotion());

                int amp = ef.getAmplifier();
                String ampStr = amp >= 1 && amp <= 9 ? " " + I18n.format("enchantment.level." + (amp + 1)) : "";
                String nameText = I18n.format(ef.getEffectName()) + ampStr;
                String timerText = formatPotionDuration(ef);

                float animValue = anim.fadeAlpha;
                boolean isPotionActive = ef.getDuration() > 0;

                if (nameTextAnimations[potionIndex] == null) {
                    nameTextAnimations[potionIndex] = new DecelerateAnimation(300, 1, Direction.FORWARDS);
                }
                if (timerTextAnimations[potionIndex] == null) {
                    timerTextAnimations[potionIndex] = new DecelerateAnimation(300, 1, Direction.FORWARDS);
                }

                if (!animationsInitialized[potionIndex]) {
                    if (isPotionActive) {
                        nameTextAnimations[potionIndex].timerUtil.setTime(System.currentTimeMillis() - 300);
                        timerTextAnimations[potionIndex].timerUtil.setTime(System.currentTimeMillis() - 300);
                        potionWasActive[potionIndex] = true;
                    }
                    animationsInitialized[potionIndex] = true;
                }

                if (isPotionActive != potionWasActive[potionIndex]) {
                    if (isPotionActive) {
                        nameTextAnimations[potionIndex].reset();
                        timerTextAnimations[potionIndex].reset();
                        nameTextAnimations[potionIndex].setDirection(Direction.FORWARDS);
                        timerTextAnimations[potionIndex].setDirection(Direction.FORWARDS);
                    } else {
                        nameTextAnimations[potionIndex].setDirection(Direction.BACKWARDS);
                        timerTextAnimations[potionIndex].setDirection(Direction.BACKWARDS);
                    }
                    potionWasActive[potionIndex] = isPotionActive;
                }

                nameTextAnimations[potionIndex].isDone();
                timerTextAnimations[potionIndex].isDone();

                float nameAnimValue = (float) nameTextAnimations[potionIndex].getOutput();
                float timerAnimValue = (float) timerTextAnimations[potionIndex].getOutput();

                float fadeMultiplier = anim.fadeAlpha;
                float combinedAlpha = animValue * finalAlpha * fadeMultiplier;

                if (combinedAlpha < 0.01f) {
                    effectY += (moduleRectHeight + padding) * animValue;
                    potionIndex++;
                    continue;
                }

                if (anim.glowIntensity > 0.01f) {
                    int glowColor = ColorUtils.setAlpha(themeColor1, (int) (100 * anim.glowIntensity * combinedAlpha));
                    RenderUtility.drawShadow(-2, effectY - 1, currentWidth + 4, moduleRectHeight + 2, 10, glowColor);
                }

                if (anim.lineProgress > 0.01f) {
                    float lineFullWidth = currentWidth - 4;
                    float lineWidth = lineFullWidth * anim.lineProgress;
                    float lineX = 2 + (lineFullWidth - lineWidth) / 2;
                    float lineY = effectY + moduleRectHeight - 1.5f;

                    if (anim.lineGlow > 0.01f) {
                        int lineGlowColor = ColorUtils.interpolateColor(themeColor1, themeColor2, 0.5f);
                        int lineGlowAlpha = (int) (80 * anim.lineGlow * combinedAlpha);
                        if (lineGlowAlpha > 3) {
                            lineGlowColor = ColorUtils.setAlpha(lineGlowColor, lineGlowAlpha);
                            RenderUtility.drawShadow(lineX - 2, lineY - 2, lineWidth + 4, 5.5f, 8, lineGlowColor);
                        }
                    }

                    int lineColor = ColorUtils.interpolateColor(themeColor1, themeColor2, 0.5f);
                    lineColor = desaturateColor(lineColor, anim.colorDesaturate);
                    int lineAlpha = (int) (255 * anim.lineProgress * combinedAlpha * 0.8f);
                    if (lineAlpha > 5) {
                        lineColor = ColorUtils.setAlpha(lineColor, lineAlpha);
                        RenderUtility.drawRoundedRect(lineX, lineY, lineWidth, 1.5f, 0.75f, lineColor);
                    }
                }

                float moduleCenterX = currentWidth / 2;
                float moduleCenterY = effectY + moduleRectHeight / 2;

                float displayScaleX = Math.max(0.01f, Math.min(anim.bounceScale, 1f));
                float displayScaleY = Math.max(0.01f, Math.min(anim.scaleY, 1f));

                GL11.glPushMatrix();
                GL11.glTranslatef(moduleCenterX, moduleCenterY, 0);
                GL11.glRotatef(anim.rotationAngle, 0, 0, 1);
                GL11.glScalef(displayScaleX, displayScaleY, 1.0f);
                GL11.glTranslatef(-moduleCenterX, -moduleCenterY, 0);

                float iconX = sidePadding;
                float iconY = effectY + (moduleRectHeight - effectIconSize) / 2;
                float nameOffset = -35 * (1 - nameAnimValue);

                Effect effect = ef.getPotion();
                TextureAtlasSprite sprite = potionspriteuploader.getSprite(effect);
                RenderSystem.color4f(1.0f, 1.0f, 1.0f, combinedAlpha * nameAnimValue);
                drawIcon(sprite, iconX + nameOffset + anim.nameSlideX, iconY, effectIconSize, effectIconSize);

                float prefixXOffset = iconX + effectIconSize + headPadding;

                int totalChars = nameText.length();
                int charsToShow;
                if (isPotionActive) {
                    charsToShow = (int) Math.ceil(totalChars * anim.typewriterProgress);
                } else {
                    float reverseProgress = 1f - anim.typewriterProgress;
                    int charsToHide = (int) Math.floor(totalChars * reverseProgress * 0.5f);
                    charsToShow = Math.max(0, totalChars - charsToHide);
                }
                charsToShow = Math.min(charsToShow, totalChars);
                String visibleName = nameText.substring(0, charsToShow);

                float timerWidth = Fonts.sfuy.getWidth(timerText, timerFontSize);
                float timerTextX = currentWidth - sidePadding - timerWidth;

                int timerTotalChars = timerText.length();
                int timerCharsToShow;
                if (isPotionActive) {
                    timerCharsToShow = (int) Math.ceil(timerTotalChars * anim.typewriterProgress);
                } else {
                    timerCharsToShow = timerTotalChars;
                }
                timerCharsToShow = Math.min(timerCharsToShow, timerTotalChars);
                String visibleTimer = timerText.substring(0, timerCharsToShow);

                float blinkAlpha = 1f;
                if (ef.getDuration() < 200) {
                    blinkAlpha = (float) (Math.sin(System.currentTimeMillis() / 200.0) * 0.3 + 0.7);
                }

                if (!ef.getPotion().isBeneficial()) {
                    if (charsToShow > 0) {
                        ITextComponent gradientNameText = GradientUtil.redGradient(visibleName);
                        float nameXOffset = prefixXOffset;
                        for (ITextComponent sibling : gradientNameText.getSiblings()) {
                            String charText = sibling.getString();
                            Color color = sibling.getStyle().getColor();
                            int colorInt = color != null ? color.getColor() : ColorUtils.rgb(255, 255, 255);
                            if (anim.colorFlash > 0.01f) {
                                colorInt = ColorUtils.interpolateColor(colorInt, themeColor1, anim.colorFlash);
                            }
                            colorInt = desaturateColor(colorInt, anim.colorDesaturate * 0.7f);
                            colorInt = ColorUtils.setAlpha(colorInt, (int) (255 * combinedAlpha * nameAnimValue * blinkAlpha));
                            Fonts.sfuy.drawText(ms, charText, nameXOffset + nameOffset + anim.nameSlideX, effectY + (moduleRectHeight - potionFontSize) / 2, colorInt, potionFontSize);
                            nameXOffset += Fonts.sfuy.getWidth(charText, potionFontSize);
                        }
                    }

                    if (anim.typewriterProgress > 0.01f && anim.typewriterProgress < 0.99f && isPotionActive) {
                        float cursorX = prefixXOffset + nameOffset + anim.nameSlideX + Fonts.sfuy.getWidth(visibleName, potionFontSize);
                        float cursorBlink = (System.currentTimeMillis() % 400 < 200) ? 1f : 0.4f;
                        int cursorColor = ColorUtils.setAlpha(themeColor1, (int) (255 * cursorBlink * combinedAlpha));
                        RenderUtility.drawRoundedRect(cursorX + 1, effectY + 2, 1.5f, moduleRectHeight - 4, 0.5f, cursorColor);
                    }

                    int timerColor = ColorUtils.rgba(255, 102, 102, (int) (255 * combinedAlpha * timerAnimValue * blinkAlpha));
                    timerColor = desaturateColor(timerColor, anim.colorDesaturate * 0.7f);
                    if (timerCharsToShow > 0) {
                        Fonts.sfuy.drawText(ms, visibleTimer, timerTextX + anim.timerSlideX, effectY + (moduleRectHeight - potionFontSize) / 2, timerColor, potionFontSize);
                    }
                } else {
                    int nameColor;
                    if (anim.colorFlash > 0.01f) {
                        nameColor = ColorUtils.interpolateColor(
                                ColorUtils.rgba(255, 255, 255, (int) (255 * combinedAlpha * nameAnimValue * blinkAlpha)),
                                ColorUtils.setAlpha(themeColor1, (int) (255 * combinedAlpha)),
                                anim.colorFlash
                        );
                    } else {
                        nameColor = ColorUtils.rgba(255, 255, 255, (int) (255 * combinedAlpha * nameAnimValue * blinkAlpha));
                    }
                    nameColor = desaturateColor(nameColor, anim.colorDesaturate * 0.7f);

                    if (charsToShow > 0) {
                        Fonts.sfuy.drawText(ms, visibleName, prefixXOffset + nameOffset + anim.nameSlideX, effectY + (moduleRectHeight - potionFontSize) / 2, nameColor, potionFontSize);
                    }

                    if (anim.typewriterProgress > 0.01f && anim.typewriterProgress < 0.99f && isPotionActive) {
                        float cursorX = prefixXOffset + nameOffset + anim.nameSlideX + Fonts.sfuy.getWidth(visibleName, potionFontSize);
                        float cursorBlink = (System.currentTimeMillis() % 400 < 200) ? 1f : 0.4f;
                        int cursorColor = ColorUtils.setAlpha(themeColor1, (int) (255 * cursorBlink * combinedAlpha));
                        RenderUtility.drawRoundedRect(cursorX + 1, effectY + 2, 1.5f, moduleRectHeight - 4, 0.5f, cursorColor);
                    }

                    int timerColor;
                    if (anim.colorFlash > 0.01f) {
                        timerColor = ColorUtils.interpolateColor(
                                ColorUtils.rgba(255, 255, 255, (int) (255 * combinedAlpha * timerAnimValue * blinkAlpha)),
                                ColorUtils.setAlpha(themeColor2, (int) (255 * combinedAlpha)),
                                anim.colorFlash
                        );
                    } else {
                        timerColor = ColorUtils.setAlpha(-1, (int) (255 * combinedAlpha * timerAnimValue * blinkAlpha));
                    }
                    timerColor = desaturateColor(timerColor, anim.colorDesaturate * 0.7f);

                    if (timerCharsToShow > 0) {
                        Fonts.sfuy.drawText(ms, visibleTimer, timerTextX + anim.timerSlideX, effectY + (moduleRectHeight - potionFontSize) / 2, timerColor, potionFontSize);
                    }
                }

                GL11.glPopMatrix();

                effectY += (moduleRectHeight + padding) * animValue;
                potionIndex++;
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

    private void updateColorAnimation(float delta) {
        for (int i = 0; i < 4; i++) {
            colorShiftOffsets[i] = (colorShiftOffsets[i] + colorShiftSpeeds[i] * delta) % 1.0f;
        }
    }
}
