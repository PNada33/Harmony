package xd.harm.ui.clickgui.components.settings;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import ru.hogoshi.Animation;
import ru.hogoshi.util.Easings;
import xd.harm.modules.impl.render.Theme;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.ui.clickgui.components.builder.Component;
import xd.harm.utils.SoundUtil;
import xd.harm.utils.math.MathUtil;
import xd.harm.utils.render.Cursors;
import xd.harm.utils.render.color.ColorUtils;
import xd.harm.utils.render.font.Fonts;
import xd.harm.utils.render.rect.RenderUtility;

public class BooleanComponent extends Component {

    private final BooleanSetting setting;
    private final Animation animation = new Animation();
    private boolean hovered = false;

    private static final float BOX_SIZE = 8f;

    public BooleanComponent(BooleanSetting setting) {
        this.setting = setting;
        setHeight(14);
        animation.animate(setting.get() ? 1 : 0, 0.35f, Easings.CIRC_OUT);
    }

    @Override
    public void render(MatrixStack stack, float mouseX, float mouseY) {
        super.render(stack, mouseX, mouseY);
        animation.update();

        float boxX = getX() + getWidth() - BOX_SIZE - 6f;
        float boxY = getY() + getHeight() / 2f - BOX_SIZE / 2f;
        boolean boxHovered = MathUtil.isHovered(mouseX, mouseY, boxX, boxY, BOX_SIZE, BOX_SIZE);

        RenderUtility.drawRoundedRect(
                getX() + 2f,
                getY() + 1f,
                getWidth() - 4f,
                getHeight() - 2f,
                3f,
                ColorUtils.rgba(8, 8, 8, 120)
        );

        Fonts.sfuy.drawText(
                stack,
                setting.getName(),
                getX() + 4,
                getY() + getHeight() / 2f - 3.5f,
                ColorUtils.rgba(205, 205, 205, 255),
                6.3f,
                0.05f
        );

        float enabledAnimation = (float) animation.getValue();

        RenderUtility.drawRoundedRect(boxX, boxY, BOX_SIZE, BOX_SIZE, 2f, ColorUtils.rgba(20, 20, 20, 235));
        int outline = ColorUtils.interpolateColor(
                ColorUtils.rgba(85, 85, 85, 170),
                Theme.MainColor(0),
                Math.min(1f, enabledAnimation + (boxHovered ? 0.2f : 0f))
        );
        RenderUtility.drawRoundedRectOutline(boxX, boxY, BOX_SIZE, BOX_SIZE, 2f, 0.7f, outline);

        if (enabledAnimation > 0.01f) {
            float fillSize = (BOX_SIZE - 3f) * enabledAnimation;
            float fillX = boxX + (BOX_SIZE - fillSize) / 2f;
            float fillY = boxY + (BOX_SIZE - fillSize) / 2f;
            RenderUtility.drawRoundedRect(
                    fillX,
                    fillY,
                    fillSize,
                    fillSize,
                    1.3f,
                    ColorUtils.setAlpha(Theme.MainColor(0), (int) (190 * enabledAnimation))
            );
        }

        if (isHovered(mouseX, mouseY)) {
            if (boxHovered) {
                if (!hovered) {
                    GLFW.glfwSetCursor(Minecraft.getInstance().getMainWindow().getHandle(), Cursors.HAND);
                    hovered = true;
                }
            } else {
                if (hovered) {
                    GLFW.glfwSetCursor(Minecraft.getInstance().getMainWindow().getHandle(), Cursors.ARROW);
                    hovered = false;
                }
            }
        } else if (hovered) {
            GLFW.glfwSetCursor(Minecraft.getInstance().getMainWindow().getHandle(), Cursors.ARROW);
            hovered = false;
        }
    }

    @Override
    public void mouseClick(float mouseX, float mouseY, int mouse) {
        if (mouse == 0 && isHovered(mouseX, mouseY)) {
            setting.set(!setting.get());
            animation.animate(setting.get() ? 1 : 0, 0.2f, Easings.CIRC_OUT);
            SoundUtil.playSound(setting.get() ? "closeselectsettings.wav" : "openselectsettings.wav");
        }
        super.mouseClick(mouseX, mouseY, mouse);
    }

    @Override
    public boolean isVisible() {
        return setting.visible.get();
    }
}
