package xd.harm.ui.clickgui.minidrop.components.settings;

import com.mojang.blaze3d.matrix.MatrixStack;
import xd.harm.modules.settings.impl.BindSetting;
import xd.harm.ui.clickgui.minidrop.components.builder.Component;
import xd.harm.ui.clickgui.minidrop.utils.ColorUtils;
import xd.harm.ui.clickgui.minidrop.utils.RenderHelper;
import xd.harm.utils.render.font.Fonts;
import org.lwjgl.glfw.GLFW;

public class BindComponent extends Component {

    final BindSetting setting;
    boolean activated;

    public BindComponent(BindSetting setting) {
        this.setting = setting;
        this.setHeight(16);
    }

    @Override
    public void render(MatrixStack stack, float mouseX, float mouseY) {
        super.render(stack, mouseX, mouseY);

        Fonts.sfui.drawText(stack, setting.getName(), Math.round(getX() + 5), Math.round(getY() + 2.5f), -1, 6.5f);

        String bind = getKeyName(setting.get());
        if (bind == null || setting.get() == -1) {
            bind = "None";
        }

        float bindWidth = Fonts.sfui.getWidth(bind, 5.5f);
        float x = getX() + getWidth() - 7 - bindWidth;
        float y = getY() + 4f;

        RenderHelper.drawRoundedRect(x - 2, y - 2, bindWidth + 4, 5.5f + 4, 2,
                ColorUtils.getClickGuiColor(0));
        Fonts.sfui.drawText(stack, bind, Math.round(x), Math.round(y),
                activated ? -1 : ColorUtils.rgba(180, 180, 180, 255), 5.5f);
    }

    private String getKeyName(int key) {
        if (key < 0) return null;
        String keyName = GLFW.glfwGetKeyName(key, 0);
        return keyName != null ? keyName.toUpperCase() : "MOUSE" + (key + 100);
    }

    @Override
    public void keyPressed(int key, int scanCode, int modifiers) {
        if (activated) {
            if (key == GLFW.GLFW_KEY_DELETE || key == GLFW.GLFW_KEY_ESCAPE) {
                setting.set(-1);
                activated = false;
                return;
            }
            setting.set(key);
            activated = false;
        }
        super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public void mouseClick(float mouseX, float mouseY, int mouse) {
        if (isHovered(mouseX, mouseY) && mouse == 0) {
            activated = !activated;
        }

        if (activated && mouse >= 1) {
            setting.set(-100 + mouse);
            activated = false;
        }

        super.mouseClick(mouseX, mouseY, mouse);
    }

    @Override
    public boolean isVisible() {
        return setting.visible.get();
    }
}
