package xd.harm.ui.clickgui.minidrop.components.settings;

import com.mojang.blaze3d.matrix.MatrixStack;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.ModeListSetting;
import xd.harm.ui.clickgui.minidrop.components.builder.Component;
import xd.harm.ui.clickgui.minidrop.utils.MathUtil;
import xd.harm.ui.clickgui.minidrop.utils.ColorUtils;
import xd.harm.ui.clickgui.minidrop.utils.RenderHelper;
import xd.harm.utils.render.font.Fonts;

import java.util.HashMap;
import java.util.Map;

public class MultiBoxComponent extends Component {

    final ModeListSetting setting;
    private final Map<BooleanSetting, Float> animations = new HashMap<>();
    private final Map<BooleanSetting, Float> textWidthCache = new HashMap<>();
    float width = 0;
    float heightPadding = 0;
    float spacing = 3;
    private float textHeight = 0;
    private boolean initialized = false;

    public MultiBoxComponent(ModeListSetting setting) {
        this.setting = setting;
        setHeight(22);
        for (BooleanSetting checkBoxSetting : setting.get()) {
            animations.put(checkBoxSetting, 0f);
        }
    }

    private void initializeCache() {
        if (!initialized && Fonts.sfui != null) {
            textHeight = Fonts.sfui.getHeight(5.5f) + 1;
            for (BooleanSetting checkBoxSetting : setting.get()) {
                textWidthCache.put(checkBoxSetting, Fonts.sfui.getWidth(checkBoxSetting.getName(), 5.5f) + 2);
            }
            initialized = true;
        }
    }

    @Override
    public void render(MatrixStack stack, float mouseX, float mouseY) {
        super.render(stack, mouseX, mouseY);
        initializeCache();
        Fonts.sfui.drawText(stack, setting.getName(), Math.round(getX() + 5), Math.round(getY() + 2), -1, 5.5f);
        float offset = 0;
        float heightoff = 0;
        float cachedTextHeight = initialized ? textHeight : Fonts.sfui.getHeight(5.5f) + 1;

        for (BooleanSetting option : setting.get()) {
            float anim = animations.get(option);
            anim = MathUtil.fast(anim, option.get() ? 1 : 0, 10);
            animations.put(option, anim);

            int backgroundColor = ColorUtils.rgba(25, 25, 25, 180);
            int interpolateColor = ColorUtils.interpolate(ColorUtils.getClickGuiColor(0), backgroundColor, anim);

            float textWidth = initialized ? textWidthCache.get(option) : Fonts.sfui.getWidth(option.getName(), 5.5f) + 2;

            if (offset + textWidth + spacing >= (getWidth() - 10)) {
                offset = 0;
                heightoff += cachedTextHeight + spacing;
            }

            float rectX = getX() + 7 + offset;
            float rectY = getY() + 10 + heightoff;
            float rectWidth = textWidth + 1;
            float rectHeight = cachedTextHeight + 2;

            RenderHelper.drawRoundedRect(rectX, rectY, rectWidth, rectHeight, 2, interpolateColor);

            float actualTextWidth = Fonts.sfui.getWidth(option.getName(), 5.5f);
            float actualTextHeight = Fonts.sfui.getHeight(5.5f);
            float textX = rectX + (rectWidth - actualTextWidth) / 2;
            float textY = rectY + (rectHeight - actualTextHeight) / 2;

            Fonts.sfui.drawText(stack, option.getName(), Math.round(textX), Math.round(textY), -1, 5.5f);

            offset += textWidth + spacing;
        }

        width = getWidth() - 15;
        setHeight(22 + heightoff);
        heightPadding = heightoff;
    }

    @Override
    public void mouseClick(float mouseX, float mouseY, int mouse) {
        float offset = 0;
        float heightoff = 0;
        float cachedTextHeight = initialized ? textHeight : Fonts.sfui.getHeight(5.5f) + 1;
        for (BooleanSetting option : setting.get()) {
            float textWidth = initialized ? textWidthCache.get(option) : Fonts.sfui.getWidth(option.getName(), 5.5f) + 2;

            if (offset + textWidth + spacing >= (getWidth() - 10)) {
                offset = 0;
                heightoff += cachedTextHeight + spacing;
            }
            if (mouse == 0 && MathUtil.isHovered(mouseX, mouseY, getX() + 8 + offset,
                    getY() + 10 + heightoff, textWidth, cachedTextHeight)) {
                option.set(!option.get());
            }
            offset += textWidth + spacing;
        }

        super.mouseClick(mouseX, mouseY, mouse);
    }

    @Override
    public boolean isVisible() {
        return setting.visible.get();
    }
}
