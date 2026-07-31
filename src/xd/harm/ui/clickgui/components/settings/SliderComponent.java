package xd.harm.ui.clickgui.components.settings;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector4f;
import org.lwjgl.glfw.GLFW;
import ru.hogoshi.Animation;
import ru.hogoshi.util.Easings;
import xd.harm.modules.impl.render.Theme;
import xd.harm.modules.settings.impl.SliderSetting;
import xd.harm.ui.clickgui.components.builder.Component;
import xd.harm.utils.SoundUtil;
import xd.harm.utils.math.MathUtil;
import xd.harm.utils.math.Vector4i;
import xd.harm.utils.render.Cursors;
import xd.harm.utils.render.color.ColorUtils;
import xd.harm.utils.render.font.Fonts;
import xd.harm.utils.render.rect.RenderUtility;

public class SliderComponent extends Component {

    private static final float TRACK_PAD_X = 5f;
    private static final float TRACK_Y_OFFSET = 12.2f;
    private static final float TRACK_HEIGHT = 4.8f;

    private final SliderSetting setting;
    private float animatedValue;
    private float lastHandleX = Float.NaN;
    private float lastComponentX = Float.NaN;
    private float lastComponentY = Float.NaN;
    private float smoothedVelocity;
    private final Vector4i baseGradient = new Vector4i(0, 0, 0, 0);
    private final Vector4i fillGradient = new Vector4i(0, 0, 0, 0);
    private final Vector4f innerRounding = new Vector4f(0, 0, 0, 0);
    private final Vector4f fillRounding = new Vector4f(0, 0, 0, 0);
    private float cachedFormattedValue = Float.NaN;
    private float cachedFormattedIncrement = Float.NaN;
    private String cachedFormattedText = "";
    private float cachedFormattedWidth;

    private Animation hoverAnimation = new Animation();
    private Animation slideAnimation = new Animation();
    private Animation bubbleAnimation = new Animation();

    private boolean sliding;
    private boolean hovered;

    public SliderComponent(SliderSetting setting) {
        this.setting = setting;
        setHeight(20);
        this.animatedValue = ((Number) setting.get()).floatValue();

        hoverAnimation = hoverAnimation.animate(0, 0.22f, Easings.EXPO_OUT);
        slideAnimation = slideAnimation.animate(0, 0.28f, Easings.EXPO_OUT);
        bubbleAnimation = bubbleAnimation.animate(0, 0.24f, Easings.EXPO_OUT);
    }

    @Override
    public void render(MatrixStack stack, float mouseX, float mouseY) {
        super.render(stack, mouseX, mouseY);

        if (sliding && GLFW.glfwGetMouseButton(Minecraft.getInstance().getMainWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) {
            sliding = false;
        }

        hoverAnimation.update();
        slideAnimation.update();
        bubbleAnimation.update();

        int themeColor = Theme.MainColor(0);
        float currentValue = ((Number) setting.get()).floatValue();

        float trackX = getX() + TRACK_PAD_X;
        float trackY = getY() + TRACK_Y_OFFSET;
        float trackWidth = getWidth() - TRACK_PAD_X * 2f;
        boolean componentMoved = Float.isNaN(lastComponentX) || Float.isNaN(lastComponentY)
                || Math.abs(lastComponentX - getX()) > 0.01f
                || Math.abs(lastComponentY - getY()) > 0.01f;
        lastComponentX = getX();
        lastComponentY = getY();

        boolean sliderHovered = MathUtil.isHovered(
                mouseX,
                mouseY,
                trackX,
                trackY - 3.0f,
                trackWidth,
                TRACK_HEIGHT + 6.0f
        );

        hoverAnimation.animate(sliderHovered || sliding ? 1 : 0, 0.22f, Easings.EXPO_OUT);
        slideAnimation.animate(sliding ? 1 : 0, 0.28f, Easings.EXPO_OUT);
        bubbleAnimation.animate(sliderHovered || sliding ? 1 : 0, 0.24f, Easings.EXPO_OUT);

        float hoverValue = (float) hoverAnimation.getValue();
        float slideValue = (float) slideAnimation.getValue();
        float bubbleValue = (float) bubbleAnimation.getValue();

        if (sliding) {
            float newValue = (float) MathHelper.clamp(
                    MathUtil.round(
                            (mouseX - trackX) / Math.max(1f, trackWidth) * (setting.max - setting.min) + setting.min,
                            setting.increment
                    ),
                    setting.min,
                    setting.max
            );

            if (newValue != currentValue) {
                setting.set(newValue);
                SoundUtil.playSound("slider.wav");
                currentValue = newValue;
            }
        }

        if (componentMoved) {
            animatedValue = currentValue;
        } else {
            animatedValue = MathUtil.lerp(animatedValue, currentValue, 10f);
        }
        float progress = getProgress(animatedValue);
        float fillWidth = MathHelper.clamp(trackWidth * progress, 0f, trackWidth);

        float handleX = trackX + fillWidth;
        float handleY = trackY + TRACK_HEIGHT / 2f;

        if (Float.isNaN(lastHandleX) || componentMoved) {
            lastHandleX = handleX;
            smoothedVelocity = 0.0f;
        }
        float rawVelocity = handleX - lastHandleX;
        lastHandleX = handleX;
        smoothedVelocity = MathUtil.lerp(smoothedVelocity, rawVelocity, 8f);

        float dragEnergy = MathHelper.clamp(Math.abs(smoothedVelocity) * 8.5f + slideValue * 1.8f, 0f, 3f);
        float direction = smoothedVelocity == 0f ? 1f : Math.signum(smoothedVelocity);
        float liquidStrength = MathHelper.clamp(0.35f + hoverValue * 0.4f + slideValue * 0.55f, 0f, 1f);

        drawHeader(stack);
        drawTrack(trackX, trackY, trackWidth, themeColor, hoverValue, slideValue);

        if (fillWidth > 0.2f) {
            drawLiquidTail(trackX, trackY, trackWidth, fillWidth, themeColor, liquidStrength, dragEnergy, slideValue);
        }

        drawThumb(trackX, trackY, fillWidth, themeColor, hoverValue, slideValue, dragEnergy, direction);
        drawValueBubble(stack, currentValue, handleX, handleY, themeColor, bubbleValue, slideValue);

        handleCursor(mouseX, mouseY, sliderHovered);
    }

    private void drawHeader(MatrixStack stack) {
        Fonts.sfuy.drawText(
                stack,
                setting.getName(),
                getX() + 4f,
                getY() + 2.2f,
                ColorUtils.rgba(188, 194, 204, 255),
                5.8f,
                0.05f
        );
    }

    private void drawTrack(float trackX, float trackY, float trackWidth, int themeColor, float hoverValue, float slideValue) {
        int tr = (themeColor >> 16) & 0xFF;
        int tg = (themeColor >> 8) & 0xFF;
        int tb = themeColor & 0xFF;

        float baseRadius = TRACK_HEIGHT / 2f;

        RenderUtility.drawShadow(
                trackX,
                trackY - 0.7f,
                trackWidth,
                TRACK_HEIGHT + 1.4f,
                10,
                ColorUtils.rgba(0, 0, 0, (int) (30 + 22 * Math.max(hoverValue, slideValue)))
        );

        RenderUtility.drawRoundedRect(
                trackX,
                trackY,
                trackWidth,
                TRACK_HEIGHT,
                baseRadius,
                ColorUtils.rgba(255, 255, 255, (int) (14 + 10 * hoverValue))
        );
        RenderUtility.drawRoundedRect(
                trackX,
                trackY,
                trackWidth,
                TRACK_HEIGHT,
                baseRadius,
                ColorUtils.rgba(9, 11, 15, (int) (136 + 18 * hoverValue))
        );

        float innerX = trackX + 0.45f;
        float innerY = trackY + 0.45f;
        float innerW = Math.max(0.4f, trackWidth - 0.9f);
        float innerH = Math.max(0.4f, TRACK_HEIGHT - 0.9f);
        float innerRadius = innerH / 2f;
        baseGradient.x = ColorUtils.rgba(7, 8, 12, 232);
        baseGradient.y = ColorUtils.rgba(4, 5, 9, 236);
        baseGradient.z = ColorUtils.rgba(Math.max(8, (int) (tr * 0.24f)), Math.max(8, (int) (tg * 0.24f)), Math.max(10, (int) (tb * 0.24f)), 234);
        baseGradient.w = ColorUtils.rgba(Math.max(10, (int) (tr * 0.34f)), Math.max(10, (int) (tg * 0.34f)), Math.max(12, (int) (tb * 0.34f)), 230);
        setRounding(innerRounding, innerRadius);
        RenderUtility.drawRoundedRect(
                innerX,
                innerY,
                innerW,
                innerH,
                innerRounding,
                baseGradient
        );
        RenderUtility.drawRoundedRectOutline(
                trackX,
                trackY,
                trackWidth,
                TRACK_HEIGHT,
                baseRadius,
                0.5f,
                ColorUtils.interpolateColor(
                        ColorUtils.rgba(255, 255, 255, 22),
                        ColorUtils.setAlpha(themeColor, 105),
                        Math.min(1f, hoverValue * 0.6f + slideValue * 0.55f)
                )
        );
    }

    private void drawLiquidTail(float trackX, float trackY, float trackWidth, float fillWidth, int themeColor, float intensity, float dragEnergy, float slideValue) {
        int tr = (themeColor >> 16) & 0xFF;
        int tg = (themeColor >> 8) & 0xFF;
        int tb = themeColor & 0xFF;

        float headInset = 1.9f + dragEnergy * 0.72f + slideValue * 0.55f;
        float tailWidth = MathHelper.clamp(fillWidth - headInset, 0f, trackWidth);
        if (tailWidth <= 0.08f) {
            return;
        }

        float fillX = trackX + 0.45f;
        float fillY = trackY + 0.45f;
        float fillW = Math.max(0.25f, tailWidth - 0.9f);
        float fillH = Math.max(0.4f, TRACK_HEIGHT - 0.9f);
        float fillRadius = fillH / 2f;

        fillGradient.x = ColorUtils.rgba(6, 7, 10, (int) (180 * intensity));
        fillGradient.y = ColorUtils.rgba(9, 10, 14, (int) (190 * intensity));
        fillGradient.z = ColorUtils.rgba(Math.max(18, (int) (tr * 0.70f)), Math.max(18, (int) (tg * 0.70f)), Math.max(20, (int) (tb * 0.70f)), (int) (220 * intensity));
        fillGradient.w = ColorUtils.rgba(Math.max(24, (int) (tr * 0.90f)), Math.max(24, (int) (tg * 0.90f)), Math.max(26, (int) (tb * 0.90f)), (int) (235 * intensity));
        setRounding(fillRounding, fillRadius);
        RenderUtility.drawRoundedRect(
                fillX,
                fillY,
                fillW,
                fillH,
                fillRounding,
                fillGradient
        );

    }

    private void drawThumb(float trackX, float trackY, float fillWidth, int themeColor, float hoverValue, float slideValue, float dragEnergy, float direction) {
        float handleX = trackX + fillWidth;
        float handleY = trackY + TRACK_HEIGHT / 2f;

        float thumbW = 7.8f + dragEnergy;
        float thumbH = 7.8f - dragEnergy * 0.26f;
        thumbH = Math.max(6.3f, thumbH);

        float thumbX = handleX - thumbW / 2f + direction * dragEnergy * 0.24f;
        float thumbY = handleY - thumbH / 2f;

        float glow = Math.max(hoverValue * 0.5f, slideValue);
        float shellRadius = thumbH / 2f;
        float shellGlow = 0.35f + glow * 0.65f;
        RenderUtility.drawShadow(
                thumbX - 1.0f,
                thumbY - 1.0f,
                thumbW + 2.0f,
                thumbH + 2.0f,
                11,
                ColorUtils.setAlpha(themeColor, (int) (70 * shellGlow))
        );
        if (glow > 0.01f) {
            RenderUtility.drawShadow(
                    thumbX - 0.2f,
                    thumbY - 0.2f,
                    thumbW + 0.4f,
                    thumbH + 0.4f,
                    8,
                    ColorUtils.rgba(255, 255, 255, (int) (24 * glow))
            );
        }

        RenderUtility.drawRoundedRect(
                thumbX - 0.4f,
                thumbY - 0.4f,
                thumbW + 0.8f,
                thumbH + 0.8f,
                shellRadius + 0.4f,
                ColorUtils.rgba(255, 255, 255, (int) (15 + 18 * shellGlow))
        );
        RenderUtility.drawRoundedRect(
                thumbX,
                thumbY,
                thumbW,
                thumbH,
                shellRadius,
                ColorUtils.rgba(16, 20, 27, (int) (150 + 25 * shellGlow))
        );
        RenderUtility.drawRoundedRectOutline(
                thumbX,
                thumbY,
                thumbW,
                thumbH,
                shellRadius,
                0.62f,
                ColorUtils.interpolateColor(
                        ColorUtils.rgba(255, 255, 255, 46),
                        ColorUtils.setAlpha(themeColor, (int) (175 * shellGlow)),
                        0.65f
                )
        );
        RenderUtility.drawRoundedRect(
                thumbX + 1.3f,
                thumbY + 1.3f,
                Math.max(1.4f, thumbW - 2.6f),
                Math.max(1.4f, thumbH - 2.6f),
                Math.max(0.9f, (thumbH - 2.6f) / 2f),
                ColorUtils.setAlpha(themeColor, (int) (92 + 84 * shellGlow))
        );

        if (dragEnergy > 0.15f && direction > 0f) {
            float tailW = Math.min(3.6f, 1.8f + dragEnergy * 0.75f);
            float tailH = thumbH * 0.5f;
            float tailX = thumbX - tailW * 0.55f;
            float tailY = handleY - tailH / 2f;
            RenderUtility.drawRoundedRect(
                    tailX,
                    tailY,
                    tailW,
                    tailH,
                    tailH / 2f,
                    ColorUtils.setAlpha(themeColor, (int) (115 + 45 * slideValue))
            );
        }
    }

    private void drawValueBubble(MatrixStack stack, float currentValue, float handleX, float handleY, int themeColor, float bubbleValue, float slideValue) {
        if (bubbleValue < 0.02f) {
            return;
        }

        String bubbleText = formatValue(currentValue);
        float textSize = 5.8f;
        float bubbleW = cachedFormattedWidth + 8f;
        float bubbleH = 7.8f;
        float wobble = (float) Math.sin(System.currentTimeMillis() * 0.009 + handleX * 0.06) * (0.35f + 0.45f * slideValue);

        float bubbleX = handleX - bubbleW / 2f;
        float bubbleY = handleY - 8.9f - bubbleH + wobble;

        RenderUtility.drawShadow(
                bubbleX + 0.3f,
                bubbleY + 0.3f,
                bubbleW - 0.6f,
                bubbleH - 0.6f,
                8,
                ColorUtils.rgba(0, 0, 0, (int) (48 * bubbleValue))
        );
        RenderUtility.drawRoundedRect(bubbleX, bubbleY, bubbleW, bubbleH, 3.2f, ColorUtils.rgba(255, 255, 255, (int) (14 + 12 * bubbleValue)));
        RenderUtility.drawRoundedRect(bubbleX, bubbleY, bubbleW, bubbleH, 3.2f, ColorUtils.rgba(13, 16, 22, (int) (150 + 16 * bubbleValue)));
        RenderUtility.drawRoundedRectOutline(
                bubbleX,
                bubbleY,
                bubbleW,
                bubbleH,
                3.2f,
                0.6f,
                ColorUtils.interpolateColor(
                        ColorUtils.rgba(255, 255, 255, (int) (30 * bubbleValue)),
                        ColorUtils.setAlpha(themeColor, (int) (145 * bubbleValue)),
                        0.65f
                )
        );
        RenderUtility.drawRoundedRect(handleX - 0.7f, bubbleY + bubbleH - 0.1f, 1.4f, 1.9f, 0.7f, ColorUtils.rgba(13, 16, 22, (int) (145 * bubbleValue)));

        Fonts.sfuy.drawCenteredText(
                stack,
                bubbleText,
                bubbleX + bubbleW / 2f,
                bubbleY + bubbleH / 2f - 2.9f,
                ColorUtils.rgba(238, 242, 250, (int) (245 * bubbleValue)),
                textSize,
                0.04f
        );
    }

    private float getProgress(float value) {
        float range = setting.max - setting.min;
        if (range <= 0f) {
            return 0f;
        }
        return MathHelper.clamp((value - setting.min) / range, 0f, 1f);
    }

    private String formatValue(float value) {
        if (Float.compare(value, cachedFormattedValue) == 0
                && Float.compare(setting.increment, cachedFormattedIncrement) == 0) {
            return cachedFormattedText;
        }

        cachedFormattedValue = value;
        cachedFormattedIncrement = setting.increment;
        if (setting.increment >= 1f) {
            cachedFormattedText = String.valueOf((int) value);
        } else if (setting.increment >= 0.1f) {
            cachedFormattedText = String.format("%.1f", value);
        } else {
            cachedFormattedText = String.format("%.2f", value);
        }
        cachedFormattedWidth = Fonts.sfuy.getWidth(cachedFormattedText, 5.8f, 0.04f);
        return cachedFormattedText;
    }

    private void setRounding(Vector4f vector, float radius) {
        vector.x = radius;
        vector.y = radius;
        vector.z = radius;
        vector.w = radius;
    }

    private void handleCursor(float mouseX, float mouseY, boolean sliderHovered) {
        if (isHovered(mouseX, mouseY)) {
            if (sliderHovered || sliding) {
                if (!hovered) {
                    GLFW.glfwSetCursor(Minecraft.getInstance().getMainWindow().getHandle(), Cursors.RESIZEH);
                    hovered = true;
                }
            } else if (hovered) {
                GLFW.glfwSetCursor(Minecraft.getInstance().getMainWindow().getHandle(), Cursors.ARROW);
                hovered = false;
            }
        } else if (hovered) {
            GLFW.glfwSetCursor(Minecraft.getInstance().getMainWindow().getHandle(), Cursors.ARROW);
            hovered = false;
        }
    }

    @Override
    public void mouseClick(float mouseX, float mouseY, int mouse) {
        if (mouse == GLFW.GLFW_MOUSE_BUTTON_LEFT && MathUtil.isHovered(
                mouseX,
                mouseY,
                getX() + TRACK_PAD_X,
                getY() + TRACK_Y_OFFSET - 3.0f,
                getWidth() - TRACK_PAD_X * 2f,
                TRACK_HEIGHT + 6.0f
        )) {
            sliding = true;
        }
        super.mouseClick(mouseX, mouseY, mouse);
    }

    @Override
    public void mouseRelease(float mouseX, float mouseY, int mouse) {
        if (mouse == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            sliding = false;
        }
        super.mouseRelease(mouseX, mouseY, mouse);
    }

    @Override
    public boolean isVisible() {
        return setting.visible.get();
    }
}
