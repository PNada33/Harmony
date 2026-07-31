package xd.harm.ui.clickgui.minidrop.components.settings;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.util.math.MathHelper;
import xd.harm.modules.settings.impl.SliderSetting;
import xd.harm.ui.clickgui.minidrop.components.builder.Component;
import xd.harm.ui.clickgui.minidrop.utils.MathUtil;
import xd.harm.ui.clickgui.minidrop.utils.ColorUtils;
import xd.harm.ui.clickgui.minidrop.utils.RenderHelper;
import xd.harm.utils.render.font.Fonts;

public class SliderComponent extends Component {

    private final SliderSetting setting;
    private float animation;
    private boolean drag;

    public SliderComponent(SliderSetting setting) {
        this.setting = setting;
        this.setHeight(18);
    }

    @Override
    public void render(MatrixStack stack, float mouseX, float mouseY) {
        super.render(stack, mouseX, mouseY);

        Fonts.sfui.drawText(stack, setting.getName(), Math.round(getX() + 5), Math.round(getY() + 2.5f), -1, 5.5f);
        Fonts.sfui.drawText(stack, String.valueOf(setting.get()),
                Math.round(getX() + getWidth() - 5 - Fonts.sfui.getWidth(String.valueOf(setting.get()), 5.5f)),
                Math.round(getY() + 2.5f), -1, 5.5f);

        RenderHelper.drawRoundedRect(getX() + 5, getY() + 11, getWidth() - 10, 2, 0.6f,
                ColorUtils.rgba(60, 60, 60, 255));

        animation = MathUtil.fast(animation,
                (getWidth() - 10) * (setting.get() - setting.min) / (setting.max - setting.min), 20);
        float sliderWidth = animation;

        RenderHelper.drawRoundedRect(getX() + 5, getY() + 11, sliderWidth, 2, 0.6f,
                ColorUtils.getClickGuiColor(0));
        RenderHelper.drawCircle(getX() + 5 + sliderWidth, getY() + 12, 5,
                ColorUtils.getClickGuiColor(0));

        if (drag) {
            float newValue = (float) MathHelper.clamp(
                    MathUtil.round((mouseX - getX() - 5) / (getWidth() - 10) * (setting.max - setting.min) + setting.min,
                            setting.increment), setting.min, setting.max);
            setting.set(newValue);
        }
    }

    @Override
    public void mouseClick(float mouseX, float mouseY, int mouse) {
        if (MathUtil.isHovered(mouseX, mouseY, getX() + 5, getY() + 10, getWidth() - 10, 3)) {
            drag = true;
        }
        super.mouseClick(mouseX, mouseY, mouse);
    }

    @Override
    public void mouseRelease(float mouseX, float mouseY, int mouse) {
        drag = false;
        super.mouseRelease(mouseX, mouseY, mouse);
    }

    @Override
    public boolean isVisible() {
        return setting.visible.get();
    }
}
