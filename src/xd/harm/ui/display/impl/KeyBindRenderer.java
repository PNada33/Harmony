package xd.harm.ui.display.impl;

import xd.harm.Harmony;
import xd.harm.events.render.EventDisplay;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.Category;
import xd.harm.modules.impl.render.Theme;
import xd.harm.ui.display.ElementRenderer;
import xd.harm.utils.animations.Animation;
import xd.harm.utils.animations.Direction;
import xd.harm.utils.animations.easing.CompactAnimation;
import xd.harm.utils.animations.easing.Easing;
import xd.harm.utils.animations.impl.DecelerateAnimation;
import xd.harm.utils.client.KeyStorage;
import xd.harm.utils.drag.Dragging;
import xd.harm.utils.render.KawaseBlur;
import xd.harm.utils.render.rect.RenderUtility;
import xd.harm.utils.render.color.ColorUtils;
import xd.harm.utils.render.font.Fonts;
import xd.harm.utils.text.GradientUtil;
import xd.harm.utils.text.font.ClientFonts;
import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.MainWindow;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.util.math.vector.Vector4f;
import net.minecraft.util.text.ITextComponent;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class KeyBindRenderer implements ElementRenderer {

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

    float[] moduleAnimValues = new float[100];
    Animation[] nameTextAnimations = new Animation[100];
    Animation[] bindTextAnimations = new Animation[100];
    boolean[] moduleWasEnabled = new boolean[100];
    boolean[] animationsInitialized = new boolean[100];
    boolean isFirstRender = true;
    long lastUpdateTime = -1;

    private final Map<Module, ModuleAppearAnim> appearAnimations = new HashMap<>();

    private static class ModuleAppearAnim {
        long startTime = 0;
        Boolean lastState = null;
        float glowIntensity = 0f;
        float bounceScale = 0f;
        float typewriterProgress = 0f;
        float nameSlideX = -30f;
        float bindSlideX = 30f;
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
            bindSlideX = 30f;
            colorFlash = 1f;
            lineProgress = 0f;
            lineGlow = 1.5f;
            fadeAlpha = 0f;
            scaleY = 0f;
            rotationAngle = -3f;
            colorDesaturate = 0f;
        }

        void update(boolean isEnabled, float delta) {
            float d60 = delta * 60f;
            if (isEnabled) {
                glowIntensity = Math.max(0f, glowIntensity - 0.04f * d60);
                lineGlow = Math.max(0f, lineGlow - 0.04f * d60);
                if (lineProgress < 1f && lineGlow > 1f) {
                    lineProgress = Math.min(1f, lineProgress + 0.1f * d60);
                } else {
                    lineProgress = Math.max(0f, lineProgress - 0.03f * d60);
                }
                bounceScale += (1f - bounceScale) * (1f - (float)Math.pow(1f - 0.12f, d60));
                typewriterProgress = Math.min(1f, typewriterProgress + 0.05f * d60);
                nameSlideX += (0f - nameSlideX) * (1f - (float)Math.pow(1f - 0.12f, d60));
                bindSlideX += (0f - bindSlideX) * (1f - (float)Math.pow(1f - 0.12f, d60));
                colorFlash = Math.max(0f, colorFlash - 0.025f * d60);
                fadeAlpha += (1f - fadeAlpha) * (1f - (float)Math.pow(1f - 0.1f, d60));
                scaleY += (1f - scaleY) * (1f - (float)Math.pow(1f - 0.15f, d60));
                rotationAngle += (0f - rotationAngle) * (1f - (float)Math.pow(1f - 0.12f, d60));
                colorDesaturate = Math.max(0f, colorDesaturate - 0.05f * d60);
            } else {
                fadeAlpha += (0f - fadeAlpha) * (1f - (float)Math.pow(1f - 0.04f, d60));
                glowIntensity = Math.max(0f, glowIntensity - 0.05f * d60);
                lineGlow = Math.max(0f, lineGlow - 0.02f * d60);
                lineProgress = Math.max(0f, lineProgress - 0.025f * d60);
                bounceScale += (0.8f - bounceScale) * (1f - (float)Math.pow(1f - 0.03f, d60));
                if (fadeAlpha < 0.5f) {
                    bounceScale += (0f - bounceScale) * (1f - (float)Math.pow(1f - 0.025f, d60));
                }
                scaleY += (0.3f - scaleY) * (1f - (float)Math.pow(1f - 0.03f, d60));
                if (fadeAlpha < 0.3f) {
                    scaleY += (0f - scaleY) * (1f - (float)Math.pow(1f - 0.02f, d60));
                }
                nameSlideX += (-50f - nameSlideX) * (1f - (float)Math.pow(1f - 0.035f, d60));
                bindSlideX += (50f - bindSlideX) * (1f - (float)Math.pow(1f - 0.035f, d60));
                typewriterProgress = Math.max(0f, typewriterProgress - 0.02f * d60);
                colorFlash = 0f;
                colorDesaturate = Math.min(1f, colorDesaturate + 0.025f * d60);
                rotationAngle += (4f - rotationAngle) * (1f - (float)Math.pow(1f - 0.025f, d60));
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

    private ModuleAppearAnim getOrCreateAnim(Module m) {
        return appearAnimations.computeIfAbsent(m, k -> new ModuleAppearAnim());
    }

    private String getCategoryIcon(Category cat) {
        if (cat == null) return "B";
        return cat.getIcon();
    }

    private String shortenKey(String key) {
        if (key.length() <= 2) return key;
        switch(key.toLowerCase()) {
            case "caps_lock": return "CL";
            case "capslock": return "CL";
            case "numpad_0": return "N0";
            case "numpad_1": return "N1";
            case "numpad_2": return "N2";
            case "numpad_3": return "N3";
            case "numpad_4": return "N4";
            case "numpad_5": return "N5";
            case "numpad_6": return "N6";
            case "numpad_7": return "N7";
            case "numpad_8": return "N8";
            case "numpad_9": return "N9";
            case "printscreen": return "PS";
            case "print_screen": return "PS";
            case "scroll_lock": return "SL";
            case "scrolllock": return "SL";
            case "num_lock": return "NL";
            case "numlock": return "NL";
            case "page_up": return "PU";
            case "pageup": return "PU";
            case "page_down": return "PD";
            case "pagedown": return "PD";
            case "insert": return "IN";
            case "delete": return "DL";
            case "home": return "HM";
            case "end": return "EN";
            case "escape": return "ES";
            case "backspace": return "BS";
            case "enter": return "EN";
            case "return": return "RT";
            case "space": return "SP";
            case "left": return "LF";
            case "right": return "RG";
            case "up": return "UP";
            case "down": return "DN";
            case "shift": return "SH";
            case "control": return "CT";
            case "ctrl": return "CT";
            case "alt": return "AL";
            case "tab": return "TB";
            case "f1": return "F1";
            case "f2": return "F2";
            case "f3": return "F3";
            case "f4": return "F4";
            case "f5": return "F5";
            case "f6": return "F6";
            case "f7": return "F7";
            case "f8": return "F8";
            case "f9": return "F9";
            case "f10": return "10";
            case "f11": return "11";
            case "f12": return "12";
            default:
                if (key.length() > 2) {
                    return key.substring(0, 2).toUpperCase();
                }
                return key;
        }
    }

    private int desaturateColor(int color, float amount) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int gray = (int)(r * 0.3f + g * 0.59f + b * 0.11f);
        r = (int)(r + (gray - r) * amount);
        g = (int)(g + (gray - g) * amount);
        b = (int)(b + (gray - b) * amount);
        return (a << 24) | (r << 16) | (g << 8) | b;
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

        float wobbleX = dragging.getWobbleX();
        float wobbleY = dragging.getWobbleY();
        float grabScale = dragging.getGrabScale();
        float grabRot = dragging.getWobbleAngle();

        float posX = animatedX + wobbleX;
        float posY = animatedY + wobbleY;

        float titleFontSize = 7f;
        float functionFontSize = 6f;
        float bindFontSize = 6f;
        float numberFontSize = 5.5f;
        int categoryIconFontSize = 20;
        float padding = 0.8f;
        float iconWidth = 14;
        float iconPadding = 1;
        float sidePadding = 2f;
        float rectHeight = 13;
        float moduleRectHeight = 11;
        float cornerRadius = 3.5f;
        float iconGap = 2.5f;
        float numberAreaWidth = 7f;
        float dotAreaWidth = 3.5f;
        float bindRectPadding = 1.5f;
        float bindRectExtraPad = 2.5f;
        float categoryIconAreaWidth = 12f;
        float extraWidth = 1f;

        ITextComponent name = GradientUtil.gradient("HotKeys");
        boolean isAnyModuleEnabled = false;
        boolean isChatOpen = mc.currentScreen instanceof ChatScreen;
        List<Module> modules = Harmony.getInstance().getModuleManager().getModules();

        for (Module f : modules) {
            if (f.isVisibleInKeyBindHud() && f.getBind() != 0) {
                ModuleAppearAnim anim = getOrCreateAnim(f);
                anim.checkAndUpdate(f.isState(), delta);
            }
        }

        if (xd.harm.modules.impl.movement.ElytraTarget.shouldElytraTarget) {
            ModuleAppearAnim elytraAnim = appearAnimations.computeIfAbsent(null, k -> new ModuleAppearAnim());
            elytraAnim.checkAndUpdate(true, delta);
        }

        float totalModuleHeight = 0;
        int moduleIndex = 0;
        for (Module f : modules) {
            f.getAnimation().update();
            boolean isOn = f.isState();
            double animVal = f.getAnimation().getValue();
            ModuleAppearAnim anim = getOrCreateAnim(f);

            if (f.isVisibleInKeyBindHud() && (isOn || animVal > 0 || anim.fadeAlpha > 0.01f) && f.getBind() != 0) {
                isAnyModuleEnabled = true;
                float effectiveAnim = anim.fadeAlpha;
                if (moduleIndex < moduleAnimValues.length) {
                    moduleAnimValues[moduleIndex] = effectiveAnim;
                }
                moduleIndex++;
                totalModuleHeight += (moduleRectHeight + padding) * effectiveAnim;
            }
        }

        if (xd.harm.modules.impl.movement.ElytraTarget.shouldElytraTarget) {
            ModuleAppearAnim elytraAnim = appearAnimations.get(null);
            if (elytraAnim != null && elytraAnim.fadeAlpha > 0.01f) {
                isAnyModuleEnabled = true;
                totalModuleHeight += (moduleRectHeight + padding) * elytraAnim.fadeAlpha;
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

        float textWidth = Fonts.sfuy.getWidth("HotKeys", titleFontSize);
        float headerWidth = iconWidth + iconPadding + textWidth + sidePadding * 2;

        float maxModuleWidth = 0;
        for (Module f : modules) {
            f.getAnimation().update();
            boolean isOn = f.isState();
            double animVal = f.getAnimation().getValue();
            ModuleAppearAnim anim = getOrCreateAnim(f);

            if (f.isVisibleInKeyBindHud() && (isOn || animVal > 0 || anim.fadeAlpha > 0.01f) && f.getBind() != 0) {
                String nameText = f.getName();
                String bindText = shortenKey(KeyStorage.getKey(f.getBind()));
                float nameWidth = Fonts.sfuy.getWidth(nameText, functionFontSize);
                float bindWidth = Fonts.sfuy.getWidth(bindText, bindFontSize);
                float localWidth = sidePadding + numberAreaWidth + dotAreaWidth + nameWidth + iconGap + bindWidth + bindRectExtraPad * 2 + bindRectPadding + categoryIconAreaWidth + sidePadding + extraWidth;
                if (localWidth > maxModuleWidth) {
                    maxModuleWidth = localWidth;
                }
            }
        }

        if (xd.harm.modules.impl.movement.ElytraTarget.shouldElytraTarget) {
            ModuleAppearAnim elytraAnim = appearAnimations.get(null);
            if (elytraAnim != null && elytraAnim.fadeAlpha > 0.01f) {
                String nameText = "РџРµСЂРµРіРѕРЅ";
                xd.harm.modules.impl.movement.ElytraTarget elytraTarget = xd.harm.Harmony.getInstance().getModuleManager().getElytraTarget();
                String bindText = elytraTarget != null && elytraTarget.forwardKey.get() != -1 ?
                        shortenKey(xd.harm.utils.client.KeyStorage.getKey(elytraTarget.forwardKey.get())) : "ON";
                float nameWidth = Fonts.sfuy.getWidth(nameText, functionFontSize);
                float bindWidth = Fonts.sfuy.getWidth(bindText, bindFontSize);
                float localWidth = sidePadding + numberAreaWidth + dotAreaWidth + nameWidth + iconGap + bindWidth + bindRectExtraPad * 2 + bindRectPadding + categoryIconAreaWidth + sidePadding + extraWidth;
                if (localWidth > maxModuleWidth) {
                    maxModuleWidth = localWidth;
                }
            }
        }

        targetWidth = Math.max(headerWidth, maxModuleWidth);

        boolean hasActiveModules = moduleIndex > 0;
        targetHeight = rectHeight + (hasActiveModules ? totalModuleHeight : (isChatOpen ? 0 : totalModuleHeight));

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

        boolean isDragging = dragging.isDragging();
        boolean grab = Math.abs(grabScale - 1.0f) > 0.001f || Math.abs(grabRot) > 0.001f;

        KawaseBlur.blur.updateBlur(3.0f, 3);

        if (currentWidth > 2f && currentHeight > 2f) {
            final float blurRadius = cornerRadius + 0.75f;
            final float blurInset = 0.35f;
            final float bpx = posX;
            final float bpy = posY;
            final float bw = currentWidth;
            final float bh = currentHeight;
            KawaseBlur.blur.render(() -> renderBlurMask(() ->
                    RenderUtility.drawRoundedRect(bpx + blurInset, bpy + blurInset,
                            bw - blurInset * 2f, bh - blurInset * 2f,
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
        Fonts.ico.drawText(ms, "a", sidePadding, (rectHeight - 7.5f) / 2 + 0.5f, logoColor, 8.5f);

        float xOffset = iconWidth + iconPadding;
        for (ITextComponent sibling : name.getSiblings()) {
            String charText = sibling.getString();
            net.minecraft.util.text.Color color = sibling.getStyle().getColor();
            int colorInt = color != null ? color.getColor() : ColorUtils.rgb(255, 255, 255);
            colorInt = ColorUtils.interpolateColor(colorInt, ColorUtils.rgb(255, 255, 255), 0.15f);
            colorInt = ColorUtils.setAlpha(colorInt, (int) (255 * finalAlpha));
            Fonts.sfuy.drawText(ms, charText, xOffset, (rectHeight - titleFontSize) / 2, colorInt, titleFontSize);
            xOffset += Fonts.sfuy.getWidth(charText, titleFontSize);
        }

        if (hasActiveModules) {
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

            float modulePosY = rectHeight;
            moduleIndex = 0;
            int displayNumber = 0;
            for (Module f : modules) {
                f.getAnimation().update();
                boolean isOn = f.isState();
                double animVal = f.getAnimation().getValue();
                ModuleAppearAnim anim = getOrCreateAnim(f);

                if (!f.isVisibleInKeyBindHud() || (!isOn && animVal <= 0 && anim.fadeAlpha <= 0.01f) || f.getBind() == 0) continue;

                displayNumber++;
                String nameText = f.getName();
                String bindText = shortenKey(KeyStorage.getKey(f.getBind()));

                float animValue = anim.fadeAlpha;
                boolean isModuleEnabled = isOn || animVal > 0 || anim.fadeAlpha > 0.01f;

                if (nameTextAnimations[moduleIndex] == null) {
                    nameTextAnimations[moduleIndex] = new DecelerateAnimation(300, 1, Direction.FORWARDS);
                }
                if (bindTextAnimations[moduleIndex] == null) {
                    bindTextAnimations[moduleIndex] = new DecelerateAnimation(300, 1, Direction.FORWARDS);
                }

                if (!animationsInitialized[moduleIndex]) {
                    if (isModuleEnabled) {
                        nameTextAnimations[moduleIndex].timerUtil.setTime(System.currentTimeMillis() - 300);
                        bindTextAnimations[moduleIndex].timerUtil.setTime(System.currentTimeMillis() - 300);
                        moduleWasEnabled[moduleIndex] = true;
                    }
                    animationsInitialized[moduleIndex] = true;
                }

                if (isModuleEnabled != moduleWasEnabled[moduleIndex]) {
                    if (isModuleEnabled) {
                        nameTextAnimations[moduleIndex].reset();
                        bindTextAnimations[moduleIndex].reset();
                        nameTextAnimations[moduleIndex].setDirection(Direction.FORWARDS);
                        bindTextAnimations[moduleIndex].setDirection(Direction.FORWARDS);
                    } else {
                        nameTextAnimations[moduleIndex].setDirection(Direction.BACKWARDS);
                        bindTextAnimations[moduleIndex].setDirection(Direction.BACKWARDS);
                    }
                    moduleWasEnabled[moduleIndex] = isModuleEnabled;
                }

                nameTextAnimations[moduleIndex].isDone();
                bindTextAnimations[moduleIndex].isDone();

                float nameAnimValue = (float) nameTextAnimations[moduleIndex].getOutput();
                float bindAnimValue = (float) bindTextAnimations[moduleIndex].getOutput();

                float fadeMultiplier = anim.fadeAlpha;
                float combinedAlpha = animValue * finalAlpha * fadeMultiplier;

                if (combinedAlpha < 0.01f) {
                    modulePosY += (moduleRectHeight + padding) * animValue;
                    moduleIndex++;
                    continue;
                }

                if (anim.glowIntensity > 0.01f) {
                    int glowColor = ColorUtils.setAlpha(themeColor1, (int)(100 * anim.glowIntensity * combinedAlpha));
                    RenderUtility.drawShadow(-2, modulePosY - 1, currentWidth + 4, moduleRectHeight + 2, 10, glowColor);
                }

                if (anim.lineProgress > 0.01f) {
                    float lineFullWidth = currentWidth - 4;
                    float lineWidth = lineFullWidth * anim.lineProgress;
                    float lineX = 2 + (lineFullWidth - lineWidth) / 2;
                    float lineY = modulePosY + moduleRectHeight - 1.5f;

                    if (anim.lineGlow > 0.01f) {
                        int lineGlowColor = ColorUtils.interpolateColor(themeColor1, themeColor2, 0.5f);
                        int lineGlowAlpha = (int)(80 * anim.lineGlow * combinedAlpha);
                        if (lineGlowAlpha > 3) {
                            lineGlowColor = ColorUtils.setAlpha(lineGlowColor, lineGlowAlpha);
                            RenderUtility.drawShadow(lineX - 2, lineY - 2, lineWidth + 4, 5.5f, 8, lineGlowColor);
                        }
                    }

                    int lineColor = ColorUtils.interpolateColor(themeColor1, themeColor2, 0.5f);
                    lineColor = desaturateColor(lineColor, anim.colorDesaturate);
                    int lineAlpha = (int)(255 * anim.lineProgress * combinedAlpha * 0.8f);
                    if (lineAlpha > 5) {
                        lineColor = ColorUtils.setAlpha(lineColor, lineAlpha);
                        RenderUtility.drawRoundedRect(lineX, lineY, lineWidth, 1.5f, 0.75f, lineColor);
                    }
                }

                float moduleCenterX = currentWidth / 2;
                float moduleCenterY = modulePosY + moduleRectHeight / 2;

                float displayScaleX = Math.max(0.01f, Math.min(anim.bounceScale, 1f));
                float displayScaleY = Math.max(0.01f, Math.min(anim.scaleY, 1f));

                GL11.glPushMatrix();
                GL11.glTranslatef(moduleCenterX, moduleCenterY, 0);
                GL11.glRotatef(anim.rotationAngle, 0, 0, 1);
                GL11.glScalef(displayScaleX, displayScaleY, 1.0f);
                GL11.glTranslatef(-moduleCenterX, -moduleCenterY, 0);

                String numberStr = String.valueOf(displayNumber);
                int numberColor = ColorUtils.rgba(180, 180, 190, (int)(255 * combinedAlpha * nameAnimValue));
                numberColor = desaturateColor(numberColor, anim.colorDesaturate * 0.7f);
                float numberWidth = Fonts.sfuy.getWidth(numberStr, numberFontSize);
                Fonts.sfuy.drawText(ms, numberStr,
                        sidePadding + (numberAreaWidth - numberWidth) / 2,
                        modulePosY + (moduleRectHeight - numberFontSize) / 2,
                        numberColor,
                        numberFontSize);

                float dotX = sidePadding + numberAreaWidth + (dotAreaWidth - 3.5f);
                float dotY = modulePosY + (moduleRectHeight - 2f) / 2;
                int dotColor = ColorUtils.rgba(180, 180, 190, (int)(255 * combinedAlpha * nameAnimValue));
                dotColor = desaturateColor(dotColor, anim.colorDesaturate * 0.7f);
                RenderUtility.drawRoundedRect(dotX, dotY, 2f, 2f, 1f, dotColor);

                float nameStartX = sidePadding + numberAreaWidth + dotAreaWidth;

                int totalChars = nameText.length();
                int charsToShow;
                if (isOn) {
                    charsToShow = (int) Math.ceil(totalChars * anim.typewriterProgress);
                } else {
                    float reverseProgress = 1f - anim.typewriterProgress;
                    int charsToHide = (int) Math.floor(totalChars * reverseProgress * 0.5f);
                    charsToShow = Math.max(0, totalChars - charsToHide);
                }
                charsToShow = Math.min(charsToShow, totalChars);
                String visibleName = nameText.substring(0, charsToShow);

                int nameColor;
                if (anim.colorFlash > 0.01f) {
                    nameColor = ColorUtils.interpolateColor(
                            ColorUtils.rgba(255, 255, 255, (int) (255 * combinedAlpha * nameAnimValue)),
                            ColorUtils.setAlpha(themeColor1, (int) (255 * combinedAlpha)),
                            anim.colorFlash
                    );
                } else {
                    nameColor = ColorUtils.rgba(255, 255, 255, (int) (255 * combinedAlpha * nameAnimValue));
                }
                nameColor = desaturateColor(nameColor, anim.colorDesaturate * 0.7f);

                float nameOffset = -35 * (1 - nameAnimValue);

                if (charsToShow > 0) {
                    Fonts.sfuy.drawText(ms, visibleName,
                            nameStartX + nameOffset,
                            modulePosY + (moduleRectHeight - functionFontSize) / 2,
                            nameColor,
                            functionFontSize);
                }

                if (anim.typewriterProgress > 0.01f && anim.typewriterProgress < 0.99f && isOn) {
                    float cursorX = nameStartX + nameOffset + Fonts.sfuy.getWidth(visibleName, functionFontSize);
                    float cursorBlink = (System.currentTimeMillis() % 400 < 200) ? 1f : 0.4f;
                    int cursorColor = ColorUtils.setAlpha(themeColor1, (int)(255 * cursorBlink * combinedAlpha));
                    RenderUtility.drawRoundedRect(cursorX + 1, modulePosY + 2, 1.5f, moduleRectHeight - 4, 0.5f, cursorColor);
                }

                float bindWidth = Fonts.sfuy.getWidth(bindText, bindFontSize);
                float bindRectWidth = bindWidth + bindRectExtraPad * 2;
                float bindRectX = currentWidth - sidePadding - categoryIconAreaWidth - bindRectPadding - bindRectWidth + 2f;
                float bindRectY = modulePosY + 1.5f;
                float bindRectH = moduleRectHeight - 3f;

                int bindBgAlpha = (int)(180 * combinedAlpha);
                int bindBgColor = ColorUtils.rgba(0, 0, 0, Math.max(1, (int)(bindBgAlpha * 0.7f)));
                RenderUtility.drawRoundedRect(bindRectX, bindRectY, bindRectWidth, bindRectH, 2f, bindBgColor);

                float bindTextX = bindRectX + bindRectExtraPad;

                int bindTotalChars = bindText.length();
                int bindCharsToShow;
                if (isOn) {
                    bindCharsToShow = (int) Math.ceil(bindTotalChars * anim.typewriterProgress);
                } else {
                    bindCharsToShow = bindTotalChars;
                }
                bindCharsToShow = Math.min(bindCharsToShow, bindTotalChars);
                String visibleBind = bindText.substring(0, bindCharsToShow);

                int bindColor;
                if (anim.colorFlash > 0.01f) {
                    bindColor = ColorUtils.interpolateColor(
                            ColorUtils.rgba(255, 255, 255, (int) (255 * combinedAlpha * bindAnimValue)),
                            ColorUtils.setAlpha(themeColor2, (int) (255 * combinedAlpha)),
                            anim.colorFlash
                    );
                } else {
                    bindColor = ColorUtils.rgba(255, 255, 255, (int) (255 * combinedAlpha * bindAnimValue));
                }
                bindColor = desaturateColor(bindColor, anim.colorDesaturate * 0.7f);

                if (bindCharsToShow > 0) {
                    Fonts.sfuy.drawText(ms, visibleBind,
                            bindTextX,
                            modulePosY + (moduleRectHeight - bindFontSize) / 2,
                            bindColor,
                            bindFontSize);
                }

                String catIcon = getCategoryIcon(f.getCategory());
                int catIconColor = ColorUtils.interpolateColor(themeColor1, themeColor2, 0.5f);
                catIconColor = ColorUtils.setAlpha(catIconColor, (int)(220 * combinedAlpha));
                catIconColor = desaturateColor(catIconColor, anim.colorDesaturate * 0.5f);
                float catIconX = currentWidth - sidePadding - categoryIconAreaWidth + 1f;
                float catIconY = modulePosY + (moduleRectHeight - categoryIconFontSize / 2.4f);
                int hiResSize = Math.min(categoryIconFontSize * 2, 49);
                GL11.glPushMatrix();
                GL11.glTranslatef(catIconX, catIconY, 0);
                GL11.glScalef(0.5f, 0.5f, 1.0f);
                ClientFonts.keybind[hiResSize].drawString(ms, catIcon, 0, 0, catIconColor);
                GL11.glPopMatrix();

                GL11.glPopMatrix();

                modulePosY += (moduleRectHeight + padding) * animValue;
                moduleIndex++;
            }

            if (xd.harm.modules.impl.movement.ElytraTarget.shouldElytraTarget) {
                ModuleAppearAnim elytraAnim = appearAnimations.get(null);
                if (elytraAnim != null && elytraAnim.fadeAlpha > 0.01f) {
                    displayNumber++;
                    String nameText = "РџРµСЂРµРіРѕРЅ";
                    xd.harm.modules.impl.movement.ElytraTarget elytraTarget = xd.harm.Harmony.getInstance().getModuleManager().getElytraTarget();
                    String bindText = elytraTarget != null && elytraTarget.forwardKey.get() != -1 ?
                            shortenKey(xd.harm.utils.client.KeyStorage.getKey(elytraTarget.forwardKey.get())) : "ON";

                    float animValue = elytraAnim.fadeAlpha;
                    float combinedAlpha = animValue * finalAlpha;

                    if (combinedAlpha >= 0.01f) {
                        if (elytraAnim.glowIntensity > 0.01f) {
                            int glowColor = ColorUtils.setAlpha(themeColor1, (int)(100 * elytraAnim.glowIntensity * combinedAlpha));
                            RenderUtility.drawShadow(-2, modulePosY - 1, currentWidth + 4, moduleRectHeight + 2, 10, glowColor);
                        }

                        if (elytraAnim.lineProgress > 0.01f) {
                            float lineFullWidth = currentWidth - 4;
                            float lineWidth = lineFullWidth * elytraAnim.lineProgress;
                            float lineX = 2 + (lineFullWidth - lineWidth) / 2;
                            float lineY = modulePosY + moduleRectHeight - 1.5f;

                            if (elytraAnim.lineGlow > 0.01f) {
                                int lineGlowColor = ColorUtils.interpolateColor(themeColor1, themeColor2, 0.5f);
                                int lineGlowAlpha = (int)(80 * elytraAnim.lineGlow * combinedAlpha);
                                if (lineGlowAlpha > 3) {
                                    lineGlowColor = ColorUtils.setAlpha(lineGlowColor, lineGlowAlpha);
                                    RenderUtility.drawShadow(lineX - 2, lineY - 2, lineWidth + 4, 5.5f, 8, lineGlowColor);
                                }
                            }

                            int lineColor = ColorUtils.interpolateColor(themeColor1, themeColor2, 0.5f);
                            lineColor = desaturateColor(lineColor, elytraAnim.colorDesaturate);
                            int lineAlpha = (int)(255 * elytraAnim.lineProgress * combinedAlpha * 0.8f);
                            if (lineAlpha > 5) {
                                lineColor = ColorUtils.setAlpha(lineColor, lineAlpha);
                                RenderUtility.drawRoundedRect(lineX, lineY, lineWidth, 1.5f, 0.75f, lineColor);
                            }
                        }

                        float moduleCenterX = currentWidth / 2;
                        float moduleCenterY = modulePosY + moduleRectHeight / 2;

                        float displayScaleX = Math.max(0.01f, Math.min(elytraAnim.bounceScale, 1f));
                        float displayScaleY = Math.max(0.01f, Math.min(elytraAnim.scaleY, 1f));

                        GL11.glPushMatrix();
                        GL11.glTranslatef(moduleCenterX, moduleCenterY, 0);
                        GL11.glRotatef(elytraAnim.rotationAngle, 0, 0, 1);
                        GL11.glScalef(displayScaleX, displayScaleY, 1.0f);
                        GL11.glTranslatef(-moduleCenterX, -moduleCenterY, 0);

                        String numberStr = String.valueOf(displayNumber);
                        int numberColor = ColorUtils.rgba(180, 180, 190, (int)(255 * combinedAlpha));
                        numberColor = desaturateColor(numberColor, elytraAnim.colorDesaturate * 0.7f);
                        float numberWidth = Fonts.sfuy.getWidth(numberStr, numberFontSize);
                        Fonts.sfuy.drawText(ms, numberStr,
                                sidePadding + (numberAreaWidth - numberWidth) / 2,
                                modulePosY + (moduleRectHeight - numberFontSize) / 2,
                                numberColor,
                                numberFontSize);

                        float dotX = sidePadding + numberAreaWidth + (dotAreaWidth - 3.5f);
                        float dotY = modulePosY + (moduleRectHeight - 2f) / 2;
                        int dotColor = ColorUtils.rgba(180, 180, 190, (int)(255 * combinedAlpha));
                        dotColor = desaturateColor(dotColor, elytraAnim.colorDesaturate * 0.7f);
                        RenderUtility.drawRoundedRect(dotX, dotY, 2f, 2f, 1f, dotColor);

                        float nameStartX = sidePadding + numberAreaWidth + dotAreaWidth;

                        int totalChars = nameText.length();
                        int charsToShow = (int) Math.ceil(totalChars * elytraAnim.typewriterProgress);
                        charsToShow = Math.min(charsToShow, totalChars);
                        String visibleName = nameText.substring(0, charsToShow);

                        int nameColor;
                        if (elytraAnim.colorFlash > 0.01f) {
                            nameColor = ColorUtils.interpolateColor(
                                    ColorUtils.rgba(255, 255, 255, (int) (255 * combinedAlpha)),
                                    ColorUtils.setAlpha(themeColor1, (int) (255 * combinedAlpha)),
                                    elytraAnim.colorFlash
                            );
                        } else {
                            nameColor = ColorUtils.rgba(255, 255, 255, (int) (255 * combinedAlpha));
                        }
                        nameColor = desaturateColor(nameColor, elytraAnim.colorDesaturate * 0.7f);

                        if (charsToShow > 0) {
                            Fonts.sfuy.drawText(ms, visibleName,
                                    nameStartX,
                                    modulePosY + (moduleRectHeight - functionFontSize) / 2,
                                    nameColor,
                                    functionFontSize);
                        }

                        if (elytraAnim.typewriterProgress > 0.01f && elytraAnim.typewriterProgress < 0.99f) {
                            float cursorX = nameStartX + Fonts.sfuy.getWidth(visibleName, functionFontSize);
                            float cursorBlink = (System.currentTimeMillis() % 400 < 200) ? 1f : 0.4f;
                            int cursorColor = ColorUtils.setAlpha(themeColor1, (int)(255 * cursorBlink * combinedAlpha));
                            RenderUtility.drawRoundedRect(cursorX + 1, modulePosY + 2, 1.5f, moduleRectHeight - 4, 0.5f, cursorColor);
                        }

                        float bindWidth = Fonts.sfuy.getWidth(bindText, bindFontSize);
                        float bindRectWidth = bindWidth + bindRectExtraPad * 2;
                        float bindRectX = currentWidth - sidePadding - categoryIconAreaWidth - bindRectPadding - bindRectWidth + 2f;
                        float bindRectY = modulePosY + 1.5f;
                        float bindRectH = moduleRectHeight - 3f;

                        int bindBgAlpha = (int)(180 * combinedAlpha);
                        int bindBgColor = ColorUtils.rgba(0, 0, 0, Math.max(1, (int)(bindBgAlpha * 0.7f)));
                        RenderUtility.drawRoundedRect(bindRectX, bindRectY, bindRectWidth, bindRectH, 2f, bindBgColor);

                        float bindTextX = bindRectX + bindRectExtraPad;

                        int bindTotalChars = bindText.length();
                        int bindCharsToShow = (int) Math.ceil(bindTotalChars * elytraAnim.typewriterProgress);
                        bindCharsToShow = Math.min(bindCharsToShow, bindTotalChars);
                        String visibleBind = bindText.substring(0, bindCharsToShow);

                        int bindColor;
                        if (elytraAnim.colorFlash > 0.01f) {
                            bindColor = ColorUtils.interpolateColor(
                                    ColorUtils.rgba(255, 255, 255, (int) (255 * combinedAlpha)),
                                    ColorUtils.setAlpha(themeColor2, (int) (255 * combinedAlpha)),
                                    elytraAnim.colorFlash
                            );
                        } else {
                            bindColor = ColorUtils.rgba(255, 255, 255, (int) (255 * combinedAlpha));
                        }
                        bindColor = desaturateColor(bindColor, elytraAnim.colorDesaturate * 0.7f);

                        if (bindCharsToShow > 0) {
                            Fonts.sfuy.drawText(ms, visibleBind,
                                    bindTextX,
                                    modulePosY + (moduleRectHeight - bindFontSize) / 2,
                                    bindColor,
                                    bindFontSize);
                        }

                        String catIcon = "D";
                        int catIconColor = ColorUtils.interpolateColor(themeColor1, themeColor2, 0.5f);
                        catIconColor = ColorUtils.setAlpha(catIconColor, (int)(220 * combinedAlpha));
                        catIconColor = desaturateColor(catIconColor, elytraAnim.colorDesaturate * 0.5f);
                        float catIconX = currentWidth - sidePadding - categoryIconAreaWidth + 1f;
                        float catIconY = modulePosY + (moduleRectHeight - categoryIconFontSize / 2.4f);
                        int hiResSize = Math.min(categoryIconFontSize * 2, 49);
                        GL11.glPushMatrix();
                        GL11.glTranslatef(catIconX, catIconY, 0);
                        GL11.glScalef(0.5f, 0.5f, 1.0f);
                        ClientFonts.keybind[hiResSize].drawString(ms, catIcon, 0, 0, catIconColor);
                        GL11.glPopMatrix();

                        GL11.glPopMatrix();

                        modulePosY += (moduleRectHeight + padding) * animValue;
                    }
                }
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

    private void updateColorAnimation(float delta) {
        for (int i = 0; i < 4; i++) {
            colorShiftOffsets[i] = (colorShiftOffsets[i] + colorShiftSpeeds[i] * delta) % 1.0f;
        }
    }
}
