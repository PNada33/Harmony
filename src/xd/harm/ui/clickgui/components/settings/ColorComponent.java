package xd.harm.ui.clickgui.components.settings;

import xd.harm.modules.impl.render.Theme;
import xd.harm.modules.settings.impl.ColorSetting;
import xd.harm.ui.clickgui.components.builder.Component;
import xd.harm.utils.math.MathUtil;
import xd.harm.utils.math.Vector4i;
import xd.harm.utils.render.Cursors;
import xd.harm.utils.render.color.ColorUtils;
import xd.harm.utils.render.font.Fonts;
import xd.harm.utils.render.rect.RenderUtility;
import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector4f;
import org.lwjgl.glfw.GLFW;
import ru.hogoshi.Animation;
import ru.hogoshi.util.Easings;

import java.awt.*;

public class ColorComponent extends Component {

    final ColorSetting colorSetting;
    private final int initialColor;

    @Getter
    private static ColorComponent opened;

    float colorRectX, colorRectY, colorRectWidth, colorRectHeight;
    float pickerX, pickerY, pickerWidth, pickerHeight;
    float sliderX, sliderY, sliderWidth, sliderHeight;
    float alphaX, alphaY, alphaWidth, alphaHeight;
    float buttonsY, buttonHeight;
    float copyX, copyW, pasteX, pasteW, resetX, resetW;
    float floatingX, floatingY, floatingWidth, floatingHeight;
    float targetFloatingX = -1000f, targetFloatingY = -1000f;
    float infoHexX, infoHexY, infoHexW, infoHexH;
    float infoAlphaX, infoAlphaY, infoAlphaW, infoAlphaH;
    float previewX, previewY, previewW, previewH;

    final float padding = 4;
    private float[] hsb = new float[3];
    private float alphaValue = 1.0f;
    private static Integer copiedColor = null;

    boolean panelOpened;
    boolean draggingHue, draggingPicker, draggingAlpha, draggingWindow;
    float draggingOffsetX, draggingOffsetY;
    boolean floatingPositionInitialized;
    private boolean hexEditing = false;
    private String hexInput = "";
    private int hexCursorPos = 0;
    private int hexSelectionPos = 0;
    private boolean hexSelectionDragging = false;
    private boolean hovered = false;

    private Animation openAnimation = new Animation();
    private boolean headerVisible = true;
    private boolean actionButtonsVisible = true;
    private float pickerPanelHeight = 50f;
    private final Vector4i svGradient = new Vector4i(0, 0, 0, 0);
    private final Vector4f pickerRounding = new Vector4f(2.7f, 2.7f, 2.7f, 2.7f);
    private int cachedHeaderRgb = Integer.MIN_VALUE;
    private String cachedHeaderHex = "";
    private float cachedHeaderHexWidth;
    private int cachedBodyColor = Integer.MIN_VALUE;
    private String cachedBodyHex = "";
    private float cachedHashWidth = -1f;

    public ColorComponent(ColorSetting colorSetting) {
        this.colorSetting = colorSetting;
        this.initialColor = colorSetting.get();
        hsb = Color.RGBtoHSB(
                ColorUtils.IntColor.getRed(colorSetting.get()),
                ColorUtils.IntColor.getGreen(colorSetting.get()),
                ColorUtils.IntColor.getBlue(colorSetting.get()),
                null
        );
        alphaValue = ColorUtils.IntColor.getAlpha(colorSetting.get()) / 255.0f;
        setHeight(16);
        openAnimation = openAnimation.animate(0, 0.3f, Easings.EXPO_OUT);
    }

    public static void closeAll() {
        if (opened != null) {
            opened.finishHexEdit(true);
            opened.panelOpened = false;
            opened.draggingHue = false;
            opened.draggingPicker = false;
            opened.draggingAlpha = false;
            opened.draggingWindow = false;
            opened.openAnimation.setValue(0);
            opened.openAnimation.setToValue(0);
            opened = null;
        }
    }

    public ColorComponent setHeaderVisible(boolean visible) {
        this.headerVisible = visible;
        return this;
    }

    public ColorComponent setActionButtonsVisible(boolean visible) {
        this.actionButtonsVisible = visible;
        return this;
    }

    public ColorComponent setPickerPanelHeight(float height) {
        this.pickerPanelHeight = MathHelper.clamp(height, 32f, 140f);
        return this;
    }

    @Override
    public void render(MatrixStack stack, float mouseX, float mouseY) {
        openAnimation.update();
        renderTextAndColorRect(stack, mouseX, mouseY);

        if (!headerVisible) {
            renderEmbeddedWindow(stack, mouseX, mouseY);
            setHeight(floatingHeight);
        } else {
            setHeight(16.0f);
        }

        super.render(stack, mouseX, mouseY);
    }

    private void renderTextAndColorRect(MatrixStack stack, float mouseX, float mouseY) {
        int colorValue = colorSetting.get();

        if (!headerVisible) {
            this.colorRectWidth = 0f;
            this.colorRectHeight = 0f;
            this.colorRectX = -10000f;
            this.colorRectY = -10000f;

            this.pickerX = getX();
            this.pickerY = getY();
            this.pickerWidth = Math.max(18f, getWidth() - padding - 8);
            this.pickerHeight = pickerPanelHeight;

            this.sliderX = pickerX + pickerWidth + padding;
            this.sliderY = pickerY;
            this.sliderWidth = 7;
            this.sliderHeight = pickerHeight;

            if (hovered) {
                GLFW.glfwSetCursor(Minecraft.getInstance().getMainWindow().getHandle(), Cursors.ARROW);
                hovered = false;
            }
            return;
        }

        String settingName = colorSetting.getName();

        this.colorRectWidth = 9;
        this.colorRectHeight = 9;
        this.colorRectX = getX() + getWidth() - colorRectWidth - 4;
        this.colorRectY = getY() + 3;

        this.pickerX = getX() + padding;
        this.pickerY = getY() + 18;
        this.pickerWidth = getWidth() - padding * 3 - 8;
        this.pickerHeight = pickerPanelHeight;

        this.sliderX = pickerX + pickerWidth + padding;
        this.sliderY = pickerY;
        this.sliderWidth = 7;
        this.sliderHeight = pickerHeight;

        Fonts.sfuy.drawText(stack, settingName, getX() + 4, getY() + 4, ColorUtils.rgba(212, 212, 212, 255), 6f);
        String hex = getHeaderHex(colorValue);
        Fonts.sfuy.drawText(
                stack,
                hex,
                colorRectX - cachedHeaderHexWidth - 4f,
                getY() + 4.2f,
                ColorUtils.rgba(150, 150, 150, 255),
                5.8f
        );

        boolean rectHovered = MathUtil.isHovered(mouseX, mouseY, colorRectX, colorRectY, colorRectWidth, colorRectHeight);
        RenderUtility.drawRoundedRect(colorRectX, colorRectY, colorRectWidth, colorRectHeight, 2.0f, colorValue);

        if (rectHovered) {
            if (!hovered) {
                GLFW.glfwSetCursor(Minecraft.getInstance().getMainWindow().getHandle(), Cursors.HAND);
                hovered = true;
            }
        } else if (hovered && !panelOpened) {
            GLFW.glfwSetCursor(Minecraft.getInstance().getMainWindow().getHandle(), Cursors.ARROW);
            hovered = false;
        }
    }

    private void updateFloatingLayout() {
        float panelPadding = 5f;
        float squareSize = 60f;
        float barGap = 3f;
        float barWidth = 6f;
        float previewGap = 2.5f;
        float previewHeight = 10f;
        float infoGap = 4f;
        float infoHeight = 11f;

        floatingWidth = panelPadding * 2f + squareSize + barGap + barWidth + barGap + barWidth;
        floatingHeight = panelPadding * 2f + squareSize + infoGap + infoHeight;

        float screenW = Minecraft.getInstance().getMainWindow().getScaledWidth();
        float screenH = Minecraft.getInstance().getMainWindow().getScaledHeight();

        if (!floatingPositionInitialized) {
            float desiredX = colorRectX + colorRectWidth + 6f;
            float desiredY = colorRectY + colorRectHeight + 4f;

            if (desiredX + floatingWidth > screenW - 4f) {
                desiredX = colorRectX - floatingWidth - 6f;
            }
            if (desiredY + floatingHeight > screenH - 4f) {
                desiredY = colorRectY - floatingHeight - 4f;
            }

            floatingX = MathHelper.clamp(desiredX, 4f, Math.max(4f, screenW - floatingWidth - 4f));
            floatingY = MathHelper.clamp(desiredY, 4f, Math.max(4f, screenH - floatingHeight - 4f));
            floatingPositionInitialized = true;
        } else {
            floatingX = MathHelper.clamp(floatingX, 4f, Math.max(4f, screenW - floatingWidth - 4f));
            floatingY = MathHelper.clamp(floatingY, 4f, Math.max(4f, screenH - floatingHeight - 4f));
        }

        pickerX = floatingX + panelPadding;
        pickerY = floatingY + panelPadding;
        pickerWidth = squareSize;
        pickerHeight = squareSize;

        sliderX = pickerX + pickerWidth + barGap;
        sliderWidth = barWidth;
        alphaX = sliderX + sliderWidth + barGap;
        alphaWidth = barWidth;

        previewX = sliderX;
        previewY = pickerY;
        previewW = sliderWidth + barGap + alphaWidth;
        previewH = previewHeight;

        sliderY = previewY + previewH + previewGap;
        alphaY = sliderY;
        sliderHeight = Math.max(8f, pickerHeight - previewH - previewGap);
        alphaHeight = sliderHeight;

        float infoWidth = floatingWidth - panelPadding * 2f;
        float alphaBoxWidth = 21f;
        infoHexX = pickerX;
        infoHexY = pickerY + pickerHeight + infoGap;
        infoHexW = infoWidth - alphaBoxWidth - 3f;
        infoHexH = infoHeight;

        infoAlphaX = infoHexX + infoHexW + 3f;
        infoAlphaY = infoHexY;
        infoAlphaW = alphaBoxWidth;
        infoAlphaH = infoHeight;
    }

    private void updateEmbeddedLayout() {
        float panelPadding = 4f;
        float barGap = 3f;
        float barWidth = 6f;
        float previewGap = 2.5f;
        float previewHeight = 10f;
        float infoGap = 4f;
        float infoHeight = 11f;

        float sideWidth = barWidth + barGap + barWidth;
        float widthBudget = Math.max(56f, getWidth());
        float heightBudget = Math.max(56f, pickerPanelHeight);
        float squareByWidth = widthBudget - panelPadding * 2f - barGap - sideWidth;
        float squareByHeight = heightBudget - panelPadding * 2f - infoGap - infoHeight;
        float squareSize = MathHelper.clamp(Math.min(squareByWidth, squareByHeight), 38f, 60f);

        floatingWidth = panelPadding * 2f + squareSize + barGap + sideWidth;
        floatingHeight = panelPadding * 2f + squareSize + infoGap + infoHeight;

        floatingX = getX() + Math.max(0f, (getWidth() - floatingWidth) / 2f);
        floatingY = getY() + Math.max(0f, (pickerPanelHeight - floatingHeight) / 2f);

        pickerX = floatingX + panelPadding;
        pickerY = floatingY + panelPadding;
        pickerWidth = squareSize;
        pickerHeight = squareSize;

        sliderX = pickerX + pickerWidth + barGap;
        sliderWidth = barWidth;
        alphaX = sliderX + sliderWidth + barGap;
        alphaWidth = barWidth;

        previewX = sliderX;
        previewY = pickerY;
        previewW = sliderWidth + barGap + alphaWidth;
        previewH = previewHeight;

        sliderY = previewY + previewH + previewGap;
        alphaY = sliderY;
        sliderHeight = Math.max(8f, pickerHeight - previewH - previewGap);
        alphaHeight = sliderHeight;

        float infoWidth = floatingWidth - panelPadding * 2f;
        float alphaBoxWidth = 21f;
        infoHexX = pickerX;
        infoHexY = pickerY + pickerHeight + infoGap;
        infoHexW = infoWidth - alphaBoxWidth - 3f;
        infoHexH = infoHeight;

        infoAlphaX = infoHexX + infoHexW + 3f;
        infoAlphaY = infoHexY;
        infoAlphaW = alphaBoxWidth;
        infoAlphaH = infoHeight;
    }

    private boolean isInCornerDragRegion(float mouseX, float mouseY) {
        if (!RenderUtility.isInRegion(mouseX, mouseY, floatingX, floatingY, floatingWidth, floatingHeight)) {
            return false;
        }

        float cornerSize = MathHelper.clamp(Math.min(floatingWidth, floatingHeight) * 0.14f, 4f, 8f);
        boolean left = mouseX <= floatingX + cornerSize;
        boolean right = mouseX >= floatingX + floatingWidth - cornerSize;
        boolean top = mouseY <= floatingY + cornerSize;
        boolean bottom = mouseY >= floatingY + floatingHeight - cornerSize;
        return (left || right) && (top || bottom);
    }

    private boolean updateFloatingDrag(float mouseX, float mouseY) {
        float beforeX = floatingX;
        float beforeY = floatingY;
        float screenW = Minecraft.getInstance().getMainWindow().getScaledWidth();
        float screenH = Minecraft.getInstance().getMainWindow().getScaledHeight();

        if (draggingWindow) {
            targetFloatingX = MathHelper.clamp(mouseX - draggingOffsetX, 4f, Math.max(4f, screenW - floatingWidth - 4f));
            targetFloatingY = MathHelper.clamp(mouseY - draggingOffsetY, 4f, Math.max(4f, screenH - floatingHeight - 4f));
        }

        if (targetFloatingX != -1000f) {
            float dx = targetFloatingX - floatingX;
            float dy = targetFloatingY - floatingY;
            if (draggingWindow || Math.abs(dx) > 0.05f || Math.abs(dy) > 0.05f) {
                floatingX += dx * 0.15f;
                floatingY += dy * 0.15f;
            } else {
                floatingX = targetFloatingX;
                floatingY = targetFloatingY;
            }
        } else {
            targetFloatingX = floatingX;
            targetFloatingY = floatingY;
        }

        if (!headerVisible) {
            setX(floatingX);
            setY(floatingY);
        }
        if (draggingPicker) {
            hsb[1] = MathHelper.clamp((mouseX - pickerX) / Math.max(1f, pickerWidth), 0f, 1f);
            hsb[2] = 1.0f - MathHelper.clamp((mouseY - pickerY) / Math.max(1f, pickerHeight), 0f, 1f);
        }
        if (draggingHue) {
            hsb[0] = MathHelper.clamp((mouseY - sliderY) / Math.max(1f, sliderHeight), 0f, 1f);
        }
        if (draggingAlpha) {
            alphaValue = 1.0f - MathHelper.clamp((mouseY - alphaY) / Math.max(1f, alphaHeight), 0f, 1f);
        }
        updateHexSelectionDrag(mouseX);
        return Math.abs(beforeX - floatingX) > 0.001f || Math.abs(beforeY - floatingY) > 0.001f;
    }

    public void renderFloatingWindow(MatrixStack stack, float mouseX, float mouseY) {
        if (!headerVisible) {
            return;
        }

        openAnimation.update();
        float alpha = (float) openAnimation.getValue();
        if (alpha <= 0.01f) {
            return;
        }

        updateFloatingLayout();
        if (updateFloatingDrag(mouseX, mouseY)) {
            updateFloatingLayout();
        }
        renderModernLayout(stack, alpha, true);
    }

    private void renderEmbeddedWindow(MatrixStack stack, float mouseX, float mouseY) {
        updateEmbeddedLayout();
        if (updateFloatingDrag(mouseX, mouseY)) {
            updateEmbeddedLayout();
        }
        renderModernLayout(stack, 1.0f, true);
    }

    private void drawCheckerboard(float x, float y, float width, float height, float cellSize, float alpha) {
        int c1 = ColorUtils.rgba(65, 65, 65, (int) (200 * alpha));
        int c2 = ColorUtils.rgba(36, 36, 36, (int) (200 * alpha));
        for (float yy = 0; yy < height; yy += cellSize) {
            for (float xx = 0; xx < width; xx += cellSize) {
                int col = (((int) (xx / cellSize) + (int) (yy / cellSize)) & 1) == 0 ? c1 : c2;
                RenderUtility.drawRectW(x + xx, y + yy, Math.min(cellSize, width - xx), Math.min(cellSize, height - yy), col);
            }
        }
    }

    private void renderModernLayout(MatrixStack stack, float alpha, boolean drawContainer) {
        int rgb = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
        int alphaInt = MathHelper.clamp((int) (alphaValue * 255f), 0, 255);
        colorSetting.set(ColorUtils.setAlpha(rgb, alphaInt));

        int outer = ColorUtils.rgba(0, 0, 0, (int) (145 * alpha));
        int bg = ColorUtils.rgba(11, 11, 13, (int) (236 * alpha));
        int outline = ColorUtils.rgba(255, 255, 255, (int) (34 * alpha));
        int innerOutline = ColorUtils.rgba(255, 255, 255, (int) (18 * alpha));

        if (drawContainer) {
            if (isDraggingFloating()) {
                com.mojang.blaze3d.systems.RenderSystem.pushMatrix();
                com.mojang.blaze3d.systems.RenderSystem.enableBlend();
                com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
                
                xd.harm.utils.shader.ShaderUtil.dashedOutline.attach();
                xd.harm.utils.shader.ShaderUtil.dashedOutline.setUniform("quadSize", (floatingWidth + 10f) * 2f, (floatingHeight + 10f) * 2f);
                xd.harm.utils.shader.ShaderUtil.dashedOutline.setUniform("innerSize", floatingWidth * 2f, floatingHeight * 2f);
                xd.harm.utils.shader.ShaderUtil.dashedOutline.setUniform("round", 4.8f * 2f, 4.8f * 2f, 4.8f * 2f, 4.8f * 2f);
                xd.harm.utils.shader.ShaderUtil.dashedOutline.setUniform("smoothness", 0.0f, 1.5f);
                xd.harm.utils.shader.ShaderUtil.dashedOutline.setUniform("outlineColor", 1.0f, 1.0f, 1.0f, 1.0f);
                xd.harm.utils.shader.ShaderUtil.dashedOutline.setUniform("outlineThickness", 0.5f * 2f);
                xd.harm.utils.shader.ShaderUtil.dashedOutline.setUniform("time", (System.currentTimeMillis() % 2500L) / 2500f);
                xd.harm.utils.shader.ShaderUtil.dashedOutline.setUniform("dashLength", 12.0f * 2f);
                xd.harm.utils.shader.ShaderUtil.dashedOutline.setUniform("gapLength", 8.0f * 2f);
                xd.harm.utils.shader.ShaderUtil.dashedOutline.setUniform("glareSize", 30.0f * 2f);
                xd.harm.utils.shader.ShaderUtil.dashedOutline.setUniform("globalAlpha", alpha);

                org.lwjgl.opengl.GL11.glColor4f(1f, 1f, 1f, alpha);
                xd.harm.utils.render.rect.RenderUtility.drawQuads(floatingX - 5f, floatingY - 5f, floatingWidth + 10f, floatingHeight + 10f, 7);
                
                xd.harm.utils.shader.ShaderUtil.dashedOutline.detach();
                com.mojang.blaze3d.systems.RenderSystem.disableBlend();
                com.mojang.blaze3d.systems.RenderSystem.popMatrix();
            }

            RenderUtility.drawRoundedRect(floatingX - 0.5f, floatingY - 0.5f, floatingWidth + 1f, floatingHeight + 1f, 4.8f, outer);
            RenderUtility.drawRoundedRect(floatingX, floatingY, floatingWidth, floatingHeight, 4.4f, bg);
            RenderUtility.drawRoundedRectOutline(floatingX, floatingY, floatingWidth, floatingHeight, 4.4f, 0.55f, outline);
        }

        int hueColor = ColorUtils.setAlpha(Color.HSBtoRGB(hsb[0], 1f, 1f), (int) (255 * alpha));
        svGradient.x = ColorUtils.rgba(255, 255, 255, (int) (255 * alpha));
        svGradient.y = ColorUtils.rgba(0, 0, 0, (int) (255 * alpha));
        svGradient.z = svGradient.y;
        svGradient.w = hueColor;
        RenderUtility.drawRoundedRect(
                pickerX,
                pickerY,
                pickerWidth,
                pickerHeight,
                pickerRounding,
                svGradient
        );
        RenderUtility.drawRoundedRectOutline(pickerX, pickerY, pickerWidth, pickerHeight, 2.7f, 0.45f, innerOutline);

        int previewAlpha = MathHelper.clamp((int) (alphaInt * alpha), 0, 255);
        RenderUtility.drawRoundedRect(previewX, previewY, previewW, previewH, 1.9f, ColorUtils.setAlpha(rgb, previewAlpha));
        RenderUtility.drawRoundedRectOutline(previewX, previewY, previewW, previewH, 1.9f, 0.45f, innerOutline);

        RenderUtility.drawRoundedRect(sliderX, sliderY, sliderWidth, sliderHeight, 2.0f, ColorUtils.rgba(6, 6, 6, (int) (220 * alpha)));
        float sliderInnerY = sliderY + 1f;
        float sliderInnerH = Math.max(1f, sliderHeight - 2f);
        for (int i = 0; i < sliderInnerH; i++) {
            float hue = i / Math.max(1f, sliderInnerH - 1f);
            RenderUtility.drawRectW(
                    sliderX + 0.6f,
                    sliderInnerY + i,
                    sliderWidth - 1.2f,
                    1f,
                    ColorUtils.setAlpha(Color.HSBtoRGB(hue, 1, 1), (int) (255 * alpha))
            );
        }
        RenderUtility.drawRoundedRectOutline(sliderX, sliderY, sliderWidth, sliderHeight, 2.0f, 0.45f, innerOutline);

        float alphaInnerY = alphaY;
        float alphaInnerH = Math.max(1f, alphaHeight);
        drawCheckerboard(alphaX, alphaInnerY, alphaWidth, alphaInnerH, 2f, alpha);
        for (int i = 0; i < alphaInnerH; i++) {
            float rowAlpha = 1.0f - (i / Math.max(1f, alphaInnerH - 1f));
            RenderUtility.drawRectW(
                    alphaX,
                    alphaInnerY + i,
                    alphaWidth,
                    1f,
                    ColorUtils.setAlpha(rgb, (int) (rowAlpha * 255f * alpha))
            );
        }

        float markerX = pickerX + hsb[1] * pickerWidth;
        float markerY = pickerY + (1f - hsb[2]) * pickerHeight;
        markerX = MathHelper.clamp(markerX, pickerX + 1.2f, pickerX + pickerWidth - 1.2f);
        markerY = MathHelper.clamp(markerY, pickerY + 1.2f, pickerY + pickerHeight - 1.2f);

        RenderUtility.drawRoundedRect(markerX - 2.2f, markerY - 2.2f, 4.4f, 4.4f, 2.2f, ColorUtils.rgba(0, 0, 0, (int) (230 * alpha)));
        RenderUtility.drawRoundedRect(markerX - 1.4f, markerY - 1.4f, 2.8f, 2.8f, 1.4f, ColorUtils.rgba(255, 255, 255, (int) (245 * alpha)));

        float hueY = MathHelper.clamp(sliderY + hsb[0] * sliderHeight, sliderY + 1f, sliderY + sliderHeight - 2f);
        RenderUtility.drawRoundedRect(
                sliderX + 0.45f,
                hueY - 0.55f,
                Math.max(1f, sliderWidth - 0.9f),
                1.1f,
                0.55f,
                ColorUtils.rgba(228, 228, 228, (int) (245 * alpha))
        );

        float alphaYPos = MathHelper.clamp(alphaY + (1f - alphaValue) * alphaHeight, alphaY + 1f, alphaY + alphaHeight - 2f);
        RenderUtility.drawRoundedRect(
                alphaX + 0.45f,
                alphaYPos - 0.55f,
                Math.max(1f, alphaWidth - 0.9f),
                1.1f,
                0.55f,
                ColorUtils.rgba(170, 170, 170, (int) (245 * alpha))
        );

        int currentColor = ColorUtils.setAlpha(rgb, alphaInt);
        String alphaText = (int) (alphaValue * 100f) + "%";

        int infoBg = ColorUtils.rgba(8, 8, 10, (int) (215 * alpha));
        int infoOut = ColorUtils.rgba(255, 255, 255, (int) (24 * alpha));
        int hexOut = hexEditing
                ? ColorUtils.setAlpha(Theme.MainColor(0), (int) (165 * alpha))
                : infoOut;
        String hexBody = hexEditing ? hexInput : getBodyHex(currentColor);
        int hexTextColor = ColorUtils.rgba(228, 228, 228, (int) (245 * alpha));
        float hexTextSize = 5.8f;
        float hexDrawX = infoHexX + 2.2f;
        float hexDrawY = infoHexY + 2.5f;
        float hashWidth = getHashWidth(hexTextSize);

        RenderUtility.drawRoundedRect(infoHexX, infoHexY, infoHexW, infoHexH, 2.1f, infoBg);
        RenderUtility.drawRoundedRectOutline(infoHexX, infoHexY, infoHexW, infoHexH, 2.1f, 0.45f, hexOut);
        Fonts.sfuy.drawText(stack, "#", hexDrawX, hexDrawY, hexTextColor, hexTextSize);

        float hexValueX = hexDrawX + hashWidth;
        if (hexEditing && hasHexSelection()) {
            int selStart = getHexSelectionStart();
            int selEnd = getHexSelectionEnd();
            float selX = hexValueX + Fonts.sfuy.getWidth(hexInput.substring(0, selStart), hexTextSize);
            float selW = Fonts.sfuy.getWidth(hexInput.substring(selStart, selEnd), hexTextSize);
            if (selW > 0.01f) {
                RenderUtility.drawRectW(
                        selX,
                        infoHexY + 1.8f,
                        selW,
                        Math.max(1f, infoHexH - 3.6f),
                        ColorUtils.setAlpha(Theme.MainColor(0), (int) (125 * alpha))
                );
            }
        }

        Fonts.sfuy.drawText(stack, hexBody, hexValueX, hexDrawY, hexTextColor, hexTextSize);
        if (hexEditing && ((System.currentTimeMillis() / 450L) % 2L == 0L)) {
            float caretX = hexValueX + Fonts.sfuy.getWidth(hexInput.substring(0, MathHelper.clamp(hexCursorPos, 0, hexInput.length())), hexTextSize);
            RenderUtility.drawRectW(
                    caretX,
                    infoHexY + 2.1f,
                    0.75f,
                    Math.max(1f, infoHexH - 4.2f),
                    ColorUtils.rgba(245, 245, 245, (int) (245 * alpha))
            );
        }

        Fonts.sfuy.drawCenteredText(
                stack,
                alphaText,
                infoAlphaX + infoAlphaW / 2f,
                infoAlphaY + 2.5f,
                ColorUtils.rgba(228, 228, 228, (int) (245 * alpha)),
                5.8f
        );
    }

    public boolean handleClick(int mouseX, int mouseY) {
        if (draggingWindow || draggingPicker || draggingHue || draggingAlpha) {
            return true;
        }

        if (headerVisible) {
            if (!panelOpened) {
                return false;
            }
            updateFloatingLayout();
            if (RenderUtility.isInRegion(mouseX, mouseY, sliderX, sliderY, sliderWidth, sliderHeight)) {
                draggingHue = true;
                return true;
            }
            if (RenderUtility.isInRegion(mouseX, mouseY, alphaX, alphaY, alphaWidth, alphaHeight)) {
                draggingAlpha = true;
                return true;
            }
            if (RenderUtility.isInRegion(mouseX, mouseY, pickerX, pickerY, pickerWidth, pickerHeight)) {
                draggingPicker = true;
                return true;
            }
            return false;
        } else {
            updateEmbeddedLayout();
            if (RenderUtility.isInRegion(mouseX, mouseY, sliderX, sliderY, sliderWidth, sliderHeight)) {
                draggingHue = true;
                return true;
            }
            if (RenderUtility.isInRegion(mouseX, mouseY, alphaX, alphaY, alphaWidth, alphaHeight)) {
                draggingAlpha = true;
                return true;
            }
            if (RenderUtility.isInRegion(mouseX, mouseY, pickerX, pickerY, pickerWidth, pickerHeight)) {
                draggingPicker = true;
                return true;
            }
            if (isInCornerDragRegion(mouseX, mouseY)) {
                draggingWindow = true;
                draggingOffsetX = mouseX - floatingX;
                draggingOffsetY = mouseY - floatingY;
                return true;
            }
            return false;
        }
    }


    public boolean isPanelOpened() {
        return panelOpened;
    }

    public boolean isMouseOverFloatingWindow(float mouseX, float mouseY) {
        if (!headerVisible) {
            return false;
        }
        if (!panelOpened && openAnimation.getValue() <= 0.01f) {
            return false;
        }
        updateFloatingLayout();
        return RenderUtility.isInRegion(mouseX, mouseY, floatingX, floatingY, floatingWidth, floatingHeight);
    }

    public boolean isMouseOverColorRect(float mouseX, float mouseY) {
        return headerVisible && RenderUtility.isInRegion(mouseX, mouseY, colorRectX, colorRectY, colorRectWidth, colorRectHeight);
    }

    @Override
    public boolean isMouseOverComponent(float mouseX, float mouseY) {
        if (super.isMouseOverComponent(mouseX, mouseY)) {
            return true;
        }
        return isMouseOverFloatingWindow(mouseX, mouseY);
    }

    public boolean isDraggingFloating() {
        return draggingPicker || draggingHue || draggingAlpha || draggingWindow;
    }

    private void applyColor(int color) {
        colorSetting.set(color);
        hsb = Color.RGBtoHSB(
                ColorUtils.IntColor.getRed(color),
                ColorUtils.IntColor.getGreen(color),
                ColorUtils.IntColor.getBlue(color),
                null
        );
        alphaValue = ColorUtils.IntColor.getAlpha(color) / 255.0f;
    }

    private Integer parseColorString(String raw) {
        if (raw == null) {
            return null;
        }

        String text = raw.trim();
        if (text.isEmpty()) {
            return null;
        }

        if (text.startsWith("#")) {
            text = text.substring(1);
        }
        if (text.startsWith("0x") || text.startsWith("0X")) {
            text = text.substring(2);
        }

        try {
            if (text.matches("(?i)[0-9a-f]{6}")) {
                int rgb = Integer.parseUnsignedInt(text, 16);
                return 0xFF000000 | rgb;
            }

            if (text.matches("(?i)[0-9a-f]{8}")) {
                return (int) Long.parseLong(text, 16);
            }

            if (text.matches("-?\\d+")) {
                return Integer.parseInt(text);
            }
        } catch (NumberFormatException ignored) {
            return null;
        }

        return null;
    }

    private void beginHexEdit() {
        hexEditing = true;
        hexInput = toPaddedHex(colorSetting.get(), 8);
        hexCursorPos = hexInput.length();
        hexSelectionPos = hexCursorPos;
        hexSelectionDragging = false;
        setFocused(true);
    }

    private void finishHexEdit(boolean apply) {
        if (!hexEditing) {
            return;
        }
        if (apply) {
            Integer parsed = parseColorString(hexInput);
            if (parsed != null) {
                applyColor(parsed);
            }
        }
        hexEditing = false;
        setFocused(false);
        hexInput = "";
        hexCursorPos = 0;
        hexSelectionPos = 0;
        hexSelectionDragging = false;
    }

    private void copyCurrentColor() {
        int color = colorSetting.get();
        copiedColor = color;
        String hex = "#" + toPaddedHex(color, 8);
        try {
            Minecraft.getInstance().keyboardListener.setClipboardString(hex);
        } catch (Exception ignored) {
        }
    }

    private void pasteColor() {
        String raw = getClipboardText();
        Integer color = parseColorString(raw);
        if (color == null) {
            if (hexEditing) {
                insertHexCharacters(raw);
                return;
            }
            color = copiedColor;
        }
        if (color == null) {
            return;
        }
        applyColor(color);
        if (hexEditing) {
            hexInput = toPaddedHex(color, 8);
            hexCursorPos = hexInput.length();
            hexSelectionPos = hexCursorPos;
        }
    }

    private void resetColor() {
        applyColor(initialColor);
        if (hexEditing) {
            hexInput = toPaddedHex(initialColor, 8);
            hexCursorPos = hexInput.length();
            hexSelectionPos = hexCursorPos;
        }
    }

    private String getClipboardText() {
        try {
            String raw = Minecraft.getInstance().keyboardListener.getClipboardString();
            return raw == null ? "" : raw;
        } catch (Exception ignored) {
            return "";
        }
    }

    private String getHeaderHex(int color) {
        int rgb = color & 0xFFFFFF;
        if (rgb != cachedHeaderRgb) {
            cachedHeaderRgb = rgb;
            cachedHeaderHex = "#" + toPaddedHex(rgb, 6);
            cachedHeaderHexWidth = Fonts.sfuy.getWidth(cachedHeaderHex, 5.8f);
        }
        return cachedHeaderHex;
    }

    private String getBodyHex(int color) {
        if (color != cachedBodyColor) {
            cachedBodyColor = color;
            cachedBodyHex = toPaddedHex(color, 8);
        }
        return cachedBodyHex;
    }

    private float getHashWidth(float textSize) {
        if (cachedHashWidth < 0f) {
            cachedHashWidth = Fonts.sfuy.getWidth("#", textSize);
        }
        return cachedHashWidth;
    }

    private String toPaddedHex(int color, int digits) {
        String hex = Integer.toHexString(color).toUpperCase();
        if (hex.length() > digits) {
            return hex.substring(hex.length() - digits);
        }
        if (hex.length() == digits) {
            return hex;
        }

        StringBuilder builder = new StringBuilder(digits);
        for (int i = hex.length(); i < digits; i++) {
            builder.append('0');
        }
        builder.append(hex);
        return builder.toString();
    }

    private boolean isControlDown(int modifiers) {
        if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            return true;
        }
        long window = Minecraft.getInstance().getMainWindow().getHandle();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
    }

    private boolean isShiftDown(int modifiers) {
        if ((modifiers & GLFW.GLFW_MOD_SHIFT) != 0) {
            return true;
        }
        long window = Minecraft.getInstance().getMainWindow().getHandle();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }

    private boolean hasHexSelection() {
        return hexCursorPos != hexSelectionPos;
    }

    private int getHexSelectionStart() {
        return Math.min(hexCursorPos, hexSelectionPos);
    }

    private int getHexSelectionEnd() {
        return Math.max(hexCursorPos, hexSelectionPos);
    }

    private void setHexCursorPosition(int position, boolean keepSelection) {
        int clamped = MathHelper.clamp(position, 0, hexInput.length());
        hexCursorPos = clamped;
        if (!keepSelection) {
            hexSelectionPos = clamped;
        }
    }

    private boolean isHexWordChar(char ch) {
        return Character.isLetterOrDigit(ch) || ch == '_';
    }

    private int getPreviousHexWordBoundary(int fromPos) {
        int pos = MathHelper.clamp(fromPos, 0, hexInput.length());
        if (pos <= 0) {
            return 0;
        }
        pos--;
        while (pos > 0 && !isHexWordChar(hexInput.charAt(pos))) {
            pos--;
        }
        while (pos > 0 && isHexWordChar(hexInput.charAt(pos - 1))) {
            pos--;
        }
        return pos;
    }

    private int getNextHexWordBoundary(int fromPos) {
        int len = hexInput.length();
        int pos = MathHelper.clamp(fromPos, 0, len);
        if (pos >= len) {
            return len;
        }
        while (pos < len && !isHexWordChar(hexInput.charAt(pos))) {
            pos++;
        }
        while (pos < len && isHexWordChar(hexInput.charAt(pos))) {
            pos++;
        }
        return pos;
    }

    private int getHexCursorIndexByMouse(float mouseX) {
        float textSize = 5.8f;
        float valueStart = infoHexX + 2.2f + Fonts.sfuy.getWidth("#", textSize);
        float localX = mouseX - valueStart;
        if (localX <= 0f) {
            return 0;
        }
        int len = hexInput.length();
        float prevWidth = 0f;
        for (int i = 1; i <= len; i++) {
            float currentWidth = Fonts.sfuy.getWidth(hexInput.substring(0, i), textSize);
            float midpoint = prevWidth + (currentWidth - prevWidth) * 0.5f;
            if (localX < midpoint) {
                return i - 1;
            }
            prevWidth = currentWidth;
        }
        return len;
    }

    private void startHexSelectionDrag(float mouseX) {
        int cursor = getHexCursorIndexByMouse(mouseX);
        hexCursorPos = cursor;
        hexSelectionPos = cursor;
        hexSelectionDragging = true;
    }

    private void updateHexSelectionDrag(float mouseX) {
        if (!hexSelectionDragging || !hexEditing) {
            return;
        }
        long window = Minecraft.getInstance().getMainWindow().getHandle();
        if (GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) {
            hexSelectionDragging = false;
            return;
        }
        hexCursorPos = getHexCursorIndexByMouse(mouseX);
    }

    private void deleteHexSelection() {
        if (!hasHexSelection()) {
            return;
        }
        int start = getHexSelectionStart();
        int end = getHexSelectionEnd();
        hexInput = hexInput.substring(0, start) + hexInput.substring(end);
        hexCursorPos = start;
        hexSelectionPos = start;
    }

    private void insertHexCharacters(String rawText) {
        if (rawText == null || rawText.isEmpty()) {
            return;
        }
        StringBuilder filtered = new StringBuilder();
        for (int i = 0; i < rawText.length(); i++) {
            char ch = Character.toUpperCase(rawText.charAt(i));
            if ((ch >= '0' && ch <= '9') || (ch >= 'A' && ch <= 'F')) {
                filtered.append(ch);
            }
        }
        if (filtered.length() == 0) {
            return;
        }
        if (hasHexSelection()) {
            deleteHexSelection();
        }
        int available = 8 - hexInput.length();
        if (available <= 0) {
            return;
        }
        String toInsert = filtered.substring(0, Math.min(filtered.length(), available));
        hexInput = hexInput.substring(0, hexCursorPos) + toInsert + hexInput.substring(hexCursorPos);
        hexCursorPos += toInsert.length();
        hexSelectionPos = hexCursorPos;
    }

    private void backspaceHexCharacter() {
        if (hasHexSelection()) {
            deleteHexSelection();
            return;
        }
        if (hexCursorPos <= 0) {
            return;
        }
        hexInput = hexInput.substring(0, hexCursorPos - 1) + hexInput.substring(hexCursorPos);
        hexCursorPos--;
        hexSelectionPos = hexCursorPos;
    }

    private void deleteHexCharacter() {
        if (hasHexSelection()) {
            deleteHexSelection();
            return;
        }
        if (hexCursorPos >= hexInput.length()) {
            return;
        }
        hexInput = hexInput.substring(0, hexCursorPos) + hexInput.substring(hexCursorPos + 1);
        hexSelectionPos = hexCursorPos;
    }

    private void copySelectedHexText() {
        if (!hasHexSelection()) {
            return;
        }
        int start = getHexSelectionStart();
        int end = getHexSelectionEnd();
        if (end <= start) {
            return;
        }
        try {
            Minecraft.getInstance().keyboardListener.setClipboardString(hexInput.substring(start, end));
        } catch (Exception ignored) {
        }
    }

    @Override
    public void keyPressed(int key, int scanCode, int modifiers) {
        if (!hexEditing) {
            super.keyPressed(key, scanCode, modifiers);
            return;
        }

        boolean ctrlDown = isControlDown(modifiers);
        boolean shiftDown = isShiftDown(modifiers);

        if (ctrlDown && key == GLFW.GLFW_KEY_A) {
            hexSelectionPos = 0;
            hexCursorPos = hexInput.length();
            super.keyPressed(key, scanCode, modifiers);
            return;
        }

        if (ctrlDown && key == GLFW.GLFW_KEY_C) {
            if (hasHexSelection()) {
                copySelectedHexText();
            } else {
                copyCurrentColor();
            }
            super.keyPressed(key, scanCode, modifiers);
            return;
        }

        if (ctrlDown && key == GLFW.GLFW_KEY_V) {
            pasteColor();
            super.keyPressed(key, scanCode, modifiers);
            return;
        }

        if (ctrlDown && key == GLFW.GLFW_KEY_LEFT) {
            setHexCursorPosition(getPreviousHexWordBoundary(hexCursorPos), shiftDown);
            super.keyPressed(key, scanCode, modifiers);
            return;
        }

        if (ctrlDown && key == GLFW.GLFW_KEY_RIGHT) {
            setHexCursorPosition(getNextHexWordBoundary(hexCursorPos), shiftDown);
            super.keyPressed(key, scanCode, modifiers);
            return;
        }

        if (key == GLFW.GLFW_KEY_LEFT) {
            setHexCursorPosition(hexCursorPos - 1, shiftDown);
            super.keyPressed(key, scanCode, modifiers);
            return;
        }

        if (key == GLFW.GLFW_KEY_RIGHT) {
            setHexCursorPosition(hexCursorPos + 1, shiftDown);
            super.keyPressed(key, scanCode, modifiers);
            return;
        }

        if (key == GLFW.GLFW_KEY_HOME) {
            setHexCursorPosition(0, shiftDown);
            super.keyPressed(key, scanCode, modifiers);
            return;
        }

        if (key == GLFW.GLFW_KEY_END) {
            setHexCursorPosition(hexInput.length(), shiftDown);
            super.keyPressed(key, scanCode, modifiers);
            return;
        }

        if (key == GLFW.GLFW_KEY_BACKSPACE) {
            backspaceHexCharacter();
            super.keyPressed(key, scanCode, modifiers);
            return;
        }

        if (key == GLFW.GLFW_KEY_DELETE) {
            deleteHexCharacter();
            super.keyPressed(key, scanCode, modifiers);
            return;
        }

        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            finishHexEdit(true);
            super.keyPressed(key, scanCode, modifiers);
            return;
        }

        if (key == GLFW.GLFW_KEY_ESCAPE) {
            finishHexEdit(false);
            super.keyPressed(key, scanCode, modifiers);
            return;
        }

        super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public void charTyped(char codePoint, int modifiers) {
        if (!hexEditing) {
            super.charTyped(codePoint, modifiers);
            return;
        }

        insertHexCharacters(String.valueOf(codePoint));

        super.charTyped(codePoint, modifiers);
    }

    @Override
    public void mouseClick(float mouseX, float mouseY, int mouse) {
        if (headerVisible && RenderUtility.isInRegion(mouseX, mouseY, colorRectX, colorRectY, colorRectWidth, colorRectHeight) && (mouse == 0 || mouse == 1)) {
            if (panelOpened) {
                panelOpened = false;
                draggingHue = false;
                draggingPicker = false;
                draggingAlpha = false;
                draggingWindow = false;
                openAnimation.animate(0, 0.2f, Easings.EXPO_OUT);
                if (opened == this) opened = null;
            } else {
                if (opened != null && opened != this) {
                    opened.panelOpened = false;
                    opened.draggingHue = false;
                    opened.draggingPicker = false;
                    opened.draggingAlpha = false;
                    opened.draggingWindow = false;
                    opened.openAnimation.animate(0, 0.2f, Easings.EXPO_OUT);
                }
                panelOpened = true;
                floatingPositionInitialized = false;
                draggingWindow = false;
                openAnimation.animate(1, 0.3f, Easings.EXPO_OUT);
                opened = this;
            }
            super.mouseClick(mouseX, mouseY, mouse);
            return;
        }

        if (!headerVisible && (panelOpened || !headerVisible)) {
            updateEmbeddedLayout();
            if (hexEditing && !RenderUtility.isInRegion(mouseX, mouseY, infoHexX, infoHexY, infoHexW, infoHexH)) {
                finishHexEdit(true);
            }
            if (mouse == 0) {
                if (RenderUtility.isInRegion(mouseX, mouseY, infoHexX, infoHexY, infoHexW, infoHexH)) {
                    if (!hexEditing) {
                        beginHexEdit();
                    }
                    startHexSelectionDrag(mouseX);
                    super.mouseClick(mouseX, mouseY, mouse);
                    return;
                }
                if (actionButtonsVisible && RenderUtility.isInRegion(mouseX, mouseY, copyX, buttonsY, copyW, buttonHeight)) {
                    copyCurrentColor();
                    super.mouseClick(mouseX, mouseY, mouse);
                    return;
                }
                if (actionButtonsVisible && RenderUtility.isInRegion(mouseX, mouseY, pasteX, buttonsY, pasteW, buttonHeight)) {
                    pasteColor();
                    super.mouseClick(mouseX, mouseY, mouse);
                    return;
                }
                if (actionButtonsVisible && RenderUtility.isInRegion(mouseX, mouseY, resetX, buttonsY, resetW, buttonHeight)) {
                    resetColor();
                    super.mouseClick(mouseX, mouseY, mouse);
                    return;
                }
            } else if (mouse == 1 && RenderUtility.isInRegion(mouseX, mouseY, infoHexX, infoHexY, infoHexW, infoHexH)) {
                pasteColor();
                if (!hexEditing) {
                    beginHexEdit();
                }
                super.mouseClick(mouseX, mouseY, mouse);
                return;
            } else if (mouse == 2 && RenderUtility.isInRegion(mouseX, mouseY, infoHexX, infoHexY, infoHexW, infoHexH)) {
                if (hexEditing && hasHexSelection()) {
                    copySelectedHexText();
                } else {
                    copyCurrentColor();
                }
                super.mouseClick(mouseX, mouseY, mouse);
                return;
            }

            if (RenderUtility.isInRegion(mouseX, mouseY, sliderX, sliderY, sliderWidth, sliderHeight)) {
                draggingHue = true;
            } else if (RenderUtility.isInRegion(mouseX, mouseY, alphaX, alphaY, alphaWidth, alphaHeight)) {
                draggingAlpha = true;
            } else if (RenderUtility.isInRegion(mouseX, mouseY, pickerX, pickerY, pickerWidth, pickerHeight)) {
                draggingPicker = true;
            }
        } else if (headerVisible && (panelOpened || openAnimation.getValue() > 0.01f)) {
            updateFloatingLayout();
            if (hexEditing && !RenderUtility.isInRegion(mouseX, mouseY, infoHexX, infoHexY, infoHexW, infoHexH)) {
                finishHexEdit(true);
            }
            if (mouse == 0 && RenderUtility.isInRegion(mouseX, mouseY, infoHexX, infoHexY, infoHexW, infoHexH)) {
                if (!hexEditing) {
                    beginHexEdit();
                }
                startHexSelectionDrag(mouseX);
                super.mouseClick(mouseX, mouseY, mouse);
                return;
            }
            if (mouse == 1 && RenderUtility.isInRegion(mouseX, mouseY, infoHexX, infoHexY, infoHexW, infoHexH)) {
                pasteColor();
                if (!hexEditing) {
                    beginHexEdit();
                }
                super.mouseClick(mouseX, mouseY, mouse);
                return;
            }
            if (mouse == 2 && RenderUtility.isInRegion(mouseX, mouseY, infoHexX, infoHexY, infoHexW, infoHexH)) {
                if (hexEditing && hasHexSelection()) {
                    copySelectedHexText();
                } else {
                    copyCurrentColor();
                }
                super.mouseClick(mouseX, mouseY, mouse);
                return;
            }
            if (mouse == 0 && RenderUtility.isInRegion(mouseX, mouseY, sliderX, sliderY, sliderWidth, sliderHeight)) {
                draggingHue = true;
                super.mouseClick(mouseX, mouseY, mouse);
                return;
            }
            if (mouse == 0 && RenderUtility.isInRegion(mouseX, mouseY, alphaX, alphaY, alphaWidth, alphaHeight)) {
                draggingAlpha = true;
                super.mouseClick(mouseX, mouseY, mouse);
                return;
            }
            if (mouse == 0 && RenderUtility.isInRegion(mouseX, mouseY, pickerX, pickerY, pickerWidth, pickerHeight)) {
                draggingPicker = true;
                super.mouseClick(mouseX, mouseY, mouse);
                return;
            }
            if (mouse == 0 && RenderUtility.isInRegion(mouseX, mouseY, floatingX, floatingY, floatingWidth, floatingHeight)) {
                draggingWindow = true;
                draggingOffsetX = mouseX - floatingX;
                draggingOffsetY = mouseY - floatingY;
                super.mouseClick(mouseX, mouseY, mouse);
                return;
            }
        }

        super.mouseClick(mouseX, mouseY, mouse);
    }

    @Override
    public void mouseRelease(float mouseX, float mouseY, int mouse) {
        draggingHue = false;
        draggingPicker = false;
        draggingAlpha = false;
        draggingWindow = false;
        if (mouse == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            hexSelectionDragging = false;
        }
        super.mouseRelease(mouseX, mouseY, mouse);
    }

    @Override
    public boolean isVisible() {
        return colorSetting.visible.get();
    }
}
