package xd.harm.ui.clickgui.minidrop.components.settings;

import com.mojang.blaze3d.matrix.MatrixStack;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.ui.clickgui.minidrop.components.builder.Component;
import xd.harm.ui.clickgui.minidrop.utils.MathUtil;
import xd.harm.ui.clickgui.minidrop.utils.ColorUtils;
import xd.harm.ui.clickgui.minidrop.utils.RenderHelper;
import xd.harm.utils.render.font.Fonts;

public class BooleanComponent extends Component {

    private final BooleanSetting setting;
    private float animation = 0;
    private float width, height;

    public BooleanComponent(BooleanSetting setting) {
        this.setting = setting;
        setHeight(16);
    }

    @Override
    public void render(MatrixStack stack, float mouseX, float mouseY) {
        super.render(stack, mouseX, mouseY);

        animation = MathUtil.fast(animation, setting.get() ? 1 : 0, 10);

        Fonts.sfui.drawText(stack, setting.getName(), Math.round(getX() + 20f), Math.round(getY() + 4), -1, 6);

        width = 12;
        height = 7;

        int color = ColorUtils.interpolate(ColorUtils.getClickGuiColor(0), ColorUtils.rgba(80, 80, 80, 255), 1 - animation);

        RenderHelper.drawRoundedRect(getX() + 6, getY() - 1.5f + getHeight() / 2f - height / 2f, width, height, 3f,
                ColorUtils.rgba(40, 40, 40, 255));
        RenderHelper.drawCircle(getX() + 6 + 4 + (4 * animation),
                getY() - 1.5f + getHeight() / 2f - height / 2f + 3.5f, 5f, color);
        RenderHelper.drawShadow(getX() + 6 + 4 + (4 * animation),
                getY() - 1.5f + getHeight() / 2f - height / 2f + 3.5f, 7f, 7f, 10,
                ColorUtils.setAlpha(color, (int) (128 * animation)));
    }

    @Override
    public void mouseClick(float mouseX, float mouseY, int mouse) {
        if (mouse == 0 && MathUtil.isHovered(mouseX, mouseY, getX() + 6, getY() - 1.5f + getHeight() / 2f - height / 2f, width, height)) {
            setting.set(!setting.get());
        }
        super.mouseClick(mouseX, mouseY, mouse);
    }

    @Override
    public boolean isVisible() {
        return setting.visible.get();
    }
}
