package xd.harm.ui.clickgui.minidrop.components.settings;

import com.mojang.blaze3d.matrix.MatrixStack;
import xd.harm.modules.settings.impl.StringSetting;
import xd.harm.ui.clickgui.minidrop.components.builder.Component;
import xd.harm.ui.clickgui.minidrop.utils.MathUtil;
import xd.harm.ui.clickgui.minidrop.utils.ColorUtils;
import xd.harm.ui.clickgui.minidrop.utils.RenderHelper;
import xd.harm.utils.render.font.Fonts;
import org.lwjgl.glfw.GLFW;

public class StringComponent extends Component {

    final StringSetting setting;
    boolean typing;
    String text = "";

    public StringComponent(StringSetting setting) {
        this.setting = setting;
        this.setHeight(24);
    }

    @Override
    public void render(MatrixStack stack, float mouseX, float mouseY) {
        super.render(stack, mouseX, mouseY);
        text = setting.get();

        float x = getX() + 5;
        float y = getY() + 10;
        float width = getWidth() - 10;

        Fonts.sfui.drawText(stack, setting.getName(), Math.round(x), Math.round(getY() + 2), -1, 5.5f);

        String textToDraw = typing || !setting.get().isEmpty() ? text : "Введите текст...";
        String displayText = textToDraw + (typing && System.currentTimeMillis() % 1000 > 500 ? "_" : "");

        RenderHelper.drawRoundedRect(x, y, width, 12, 2, ColorUtils.rgba(30, 30, 35, 255));
        Fonts.sfui.drawText(stack, displayText, Math.round(x + 2), Math.round(y + 3),
                typing || !setting.get().isEmpty() ? -1 : ColorUtils.rgba(120, 120, 120, 255), 5.5f);

        setHeight(24);
    }

    @Override
    public void mouseClick(float mouseX, float mouseY, int mouse) {
        float x = getX() + 5;
        float y = getY() + 10;
        float width = getWidth() - 10;

        if (MathUtil.isHovered(mouseX, mouseY, x, y, width, 12)) {
            typing = !typing;
        } else {
            typing = false;
        }
        super.mouseClick(mouseX, mouseY, mouse);
    }

    @Override
    public void keyPressed(int key, int scanCode, int modifiers) {
        if (typing) {
            if (key == GLFW.GLFW_KEY_BACKSPACE && !text.isEmpty()) {
                text = text.substring(0, text.length() - 1);
                setting.set(text);
            } else if (key == GLFW.GLFW_KEY_ENTER) {
                typing = false;
            } else if (key == GLFW.GLFW_KEY_ESCAPE) {
                typing = false;
            }
        }
        super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public void charTyped(char codePoint, int modifiers) {
        if (typing && text.length() < 60) {
            text += codePoint;
            setting.set(text);
        }
        super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean isVisible() {
        return setting.visible.get();
    }
}
