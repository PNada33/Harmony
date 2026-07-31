package xd.harm.ui.clickgui.minidrop.components.settings;

import com.mojang.blaze3d.matrix.MatrixStack;
import xd.harm.modules.settings.impl.ModeSetting;
import xd.harm.ui.clickgui.minidrop.components.builder.Component;
import xd.harm.ui.clickgui.minidrop.utils.MathUtil;
import xd.harm.ui.clickgui.minidrop.utils.ColorUtils;
import xd.harm.ui.clickgui.minidrop.utils.RenderHelper;
import xd.harm.utils.render.font.Fonts;

import java.util.HashMap;
import java.util.Map;

public class ModeComponent extends Component {

    final ModeSetting setting;
    private final Map<String, Float> animations = new HashMap<>();
    private final Map<String, Float> textWidthCache = new HashMap<>();
    float width = 0;
    float heightplus = 0;
    float spacing = 5;
    private float textHeight = 0;
    private boolean initialized = false;

    public ModeComponent(ModeSetting setting) {
        this.setting = setting;
        setHeight(22);
        for (String option : setting.strings) {
            animations.put(option, 0f);
        }
    }

    private void initializeCache() {
        if (!initialized && Fonts.sfui != null) {
            textHeight = Fonts.sfui.getHeight(5.5f) + 1;
            for (String option : setting.strings) {
                textWidthCache.put(option, Fonts.sfui.getWidth(option, 5.5f) + 3);
            }
            initialized = true;
        }
    }

    @Override
    public void render(MatrixStack stack, float mouseX, float mouseY) {
        super.render(stack, mouseX, mouseY);
        initializeCache();
        spacing = 5;
        Fonts.sfui.drawText(stack, setting.getName(), Math.round(getX() + 5), Math.round(getY() + 2), -1, 5.5f);

        float offset = 0;
        float heightoff = 0;

        float cachedTextHeight = initialized ? textHeight : Fonts.sfui.getHeight(5.5f) + 1;

        for (String text : setting.strings) {
            float anim = animations.get(text);
            anim = MathUtil.fast(anim, text.equals(setting.get()) ? 1 : 0, 10);
            animations.put(text, anim);

            float textWidth = initialized ? textWidthCache.get(text) : Fonts.sfui.getWidth(text, 5.5f) + 3;

            if (offset + textWidth + spacing >= (getWidth() - 10)) {
                offset = 0;
                heightoff += cachedTextHeight + spacing / 2;
            }

            int backgroundColor = ColorUtils.rgba(25, 25, 25, 180);
            int interpolateColor = ColorUtils.interpolate(ColorUtils.getClickGuiColor(0), backgroundColor, anim);

            float rectX = getX() + 7 + offset;
            float rectY = getY() + 10 + heightoff;
            float rectWidth = textWidth + 1;
            float rectHeight = cachedTextHeight + 2;

            RenderHelper.drawRoundedRect(rectX, rectY, rectWidth, rectHeight, 2, interpolateColor);

            float actualTextWidth = Fonts.sfui.getWidth(text, 5.6f);
            float actualTextHeight = Fonts.sfui.getHeight(5.6f);
            float textX = rectX + (rectWidth - actualTextWidth) / 3;
            float textY = rectY + (rectHeight - actualTextHeight) / 2;

            Fonts.sfui.drawText(stack, text, Math.round(textX), Math.round(textY), -1, 5.5f);

            offset += textWidth + spacing / 2;
        }

        width = getWidth() - 15;
        setHeight(22 + heightoff);
        heightplus = heightoff;
    }

    @Override
    public void mouseClick(float mouseX, float mouseY, int mouse) {
        float offset = 0;
        float heightoff = 0;
        float cachedTextHeight = initialized ? textHeight : Fonts.sfui.getHeight(5.5f) + 1;
        for (String text : setting.strings) {
            float textWidth = initialized ? textWidthCache.get(text) : Fonts.sfui.getWidth(text, 5.5f) + 3;

            if (offset + textWidth + spacing >= (getWidth() - 10)) {
                offset = 0;
                heightoff += cachedTextHeight + spacing / 2;
            }
            if (mouse == 0 && !text.equals(setting.get()) &&
                    MathUtil.isHovered(mouseX, mouseY, getX() + 8 + offset, getY() + 10 + heightoff, textWidth, cachedTextHeight)) {
                setting.set(text);
            }
            offset += textWidth + spacing / 2;
        }

        super.mouseClick(mouseX, mouseY, mouse);
    }

    @Override
    public boolean isVisible() {
        return setting.visible.get();
    }
}
