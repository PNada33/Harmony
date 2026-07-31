package xd.harm.ui.clickgui.minidrop.components.settings;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.util.math.MathHelper;
import xd.harm.modules.settings.impl.ColorSetting;
import xd.harm.ui.clickgui.minidrop.components.builder.Component;
import xd.harm.ui.clickgui.minidrop.utils.MathUtil;
import xd.harm.ui.clickgui.minidrop.utils.ColorUtils;
import xd.harm.ui.clickgui.minidrop.utils.RenderHelper;
import xd.harm.utils.render.font.Fonts;

import java.awt.*;

public class ColorComponent extends Component {

    final ColorSetting setting;
    final float padding = 5.5f;
    float colorRectX, colorRectY, colorRectWidth, colorRectHeight;
    float pickerX, pickerY, pickerWidth, pickerHeight;
    float sliderX, sliderY, sliderWidth, sliderHeight;
    boolean panelOpened;
    boolean draggingHue, draggingPicker;
    private float[] hsb = new float[3];
    private int alpha = 255;
    private float openAnimation = 0;

    public ColorComponent(ColorSetting setting) {
        this.setting = setting;
        int color = setting.get();
        alpha = (color >> 24) & 0xFF;
        if (alpha == 0) alpha = 255;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        hsb = Color.RGBtoHSB(r, g, b, null);
        setHeight(14);
    }

    @Override
    public void render(MatrixStack stack, float mouseX, float mouseY) {
        renderTextAndColorRect(stack);

        openAnimation = MathUtil.fast(openAnimation, panelOpened ? 1 : 0, 10);

        if (openAnimation > 0.01f) {
            int rgb = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;
            // ColorSetting в Harmony хранит int (ARGB), устанавливаем напрямую
            int newColor = (alpha << 24) | (r << 16) | (g << 8) | b;
            setting.set(newColor);

            renderSlider(stack, mouseX, mouseY);
            renderPickerPanel(stack, mouseX, mouseY);

            setHeight(20 + (pickerHeight + padding) * openAnimation);
        } else {
            setHeight(14);
        }

        super.render(stack, mouseX, mouseY);
    }

    private void renderTextAndColorRect(MatrixStack stack) {
        String settingName = setting.getName();
        int colorValue = setting.get();

        float textX = this.getX() + padding;
        float textY = this.getY() + 3;

        this.colorRectWidth = padding * 3f;
        this.colorRectHeight = padding * 1.5f;
        this.colorRectX = this.getX() + getWidth() - colorRectWidth - padding;
        this.colorRectY = this.getY() + 2;

        this.pickerX = this.getX() + padding;
        this.pickerY = this.getY() + (padding) + 8;
        this.pickerWidth = this.getWidth() - (padding * 2);
        this.pickerHeight = 30;

        this.sliderX = pickerX;
        this.sliderY = pickerY + pickerHeight + padding;
        this.sliderWidth = pickerWidth;
        this.sliderHeight = 3;

        Fonts.sfui.drawText(stack, settingName, Math.round(textX), Math.round(textY), -1, 6);
        RenderHelper.drawRoundedRect(this.colorRectX, this.colorRectY, this.colorRectWidth,
                this.colorRectHeight, 2f, colorValue);
        RenderHelper.drawShadow(this.colorRectX, this.colorRectY, this.colorRectWidth,
                this.colorRectHeight, 5, colorValue);
    }

    private void renderPickerPanel(MatrixStack stack, float mouseX, float mouseY) {
        if (openAnimation < 0.01f) return;

        float scale = openAnimation;
        float scaledHeight = pickerHeight * scale;

        RenderHelper.drawRoundedRect(pickerX, pickerY, pickerWidth, scaledHeight, 2, -1);

        for (int i = 0; i < pickerWidth; i++) {
            float saturation = i / pickerWidth;
            for (int j = 0; j < scaledHeight; j++) {
                float brightness = 1 - (j / scaledHeight);
                int color = Color.HSBtoRGB(hsb[0], saturation, brightness);
                RenderHelper.drawRectW(pickerX + i, pickerY + j, 1, 1, color);
            }
        }

        float pickerXPos = pickerX + (hsb[1] * pickerWidth);
        float pickerYPos = pickerY + ((1 - hsb[2]) * scaledHeight);
        RenderHelper.drawCircle(pickerXPos, pickerYPos, 3, -1);
        RenderHelper.drawCircle(pickerXPos, pickerYPos, 2, ColorUtils.rgba(0, 0, 0, 255));

        if (draggingPicker) {
            hsb[1] = MathHelper.clamp((mouseX - pickerX) / pickerWidth, 0, 1);
            hsb[2] = 1 - MathHelper.clamp((mouseY - pickerY) / scaledHeight, 0, 1);
        }
    }

    private void renderSlider(MatrixStack stack, float mouseX, float mouseY) {
        if (openAnimation < 0.01f) return;

        RenderHelper.drawRoundedRect(sliderX, sliderY, sliderWidth, sliderHeight, 1.5f,
                ColorUtils.rgba(40, 40, 45, 255));

        for (int i = 0; i < sliderWidth; i++) {
            float hue = i / sliderWidth;
            int color = Color.HSBtoRGB(hue, 1, 1);
            RenderHelper.drawRectW(sliderX + i, sliderY, 1, sliderHeight, color);
        }

        float sliderPos = sliderX + (hsb[0] * sliderWidth);
        RenderHelper.drawCircle(sliderPos, sliderY + sliderHeight / 2, 4,
                Color.HSBtoRGB(hsb[0], 1, 1));

        if (draggingHue) {
            hsb[0] = MathHelper.clamp((mouseX - sliderX) / sliderWidth, 0, 1);
        }
    }

    @Override
    public void mouseClick(float mouseX, float mouseY, int mouse) {
        if (MathUtil.isHovered(mouseX, mouseY, colorRectX, colorRectY, colorRectWidth, colorRectHeight)) {
            panelOpened = !panelOpened;
        }

        if (panelOpened && openAnimation > 0.5f) {
            if (MathUtil.isHovered(mouseX, mouseY, pickerX, pickerY, pickerWidth, pickerHeight * openAnimation)) {
                draggingPicker = true;
            }
            if (MathUtil.isHovered(mouseX, mouseY, sliderX, sliderY, sliderWidth, sliderHeight)) {
                draggingHue = true;
            }
        }

        super.mouseClick(mouseX, mouseY, mouse);
    }

    @Override
    public void mouseRelease(float mouseX, float mouseY, int mouse) {
        draggingPicker = false;
        draggingHue = false;
        super.mouseRelease(mouseX, mouseY, mouse);
    }

    @Override
    public boolean isVisible() {
        return setting.visible.get();
    }
}
