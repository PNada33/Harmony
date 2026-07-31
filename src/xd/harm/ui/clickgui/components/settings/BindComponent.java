package xd.harm.ui.clickgui.components.settings;

import xd.harm.modules.impl.render.Theme;
import xd.harm.modules.settings.impl.BindSetting;
import xd.harm.ui.clickgui.components.builder.Component;
import xd.harm.utils.SoundUtil;
import xd.harm.utils.client.KeyStorage;
import xd.harm.utils.math.MathUtil;
import xd.harm.utils.render.Cursors;
import xd.harm.utils.render.color.ColorUtils;
import xd.harm.utils.render.font.Fonts;
import xd.harm.utils.render.rect.RenderUtility;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class BindComponent extends Component {

    final BindSetting setting;
    private boolean hidden = false;

    public BindComponent(BindSetting setting) {
        this.setting = setting;
        this.setHeight(12);
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    boolean activated;
    boolean hovered = false;

    public boolean isActivated() {
        return activated;
    }

    @Override
    public void render(MatrixStack stack, float mouseX, float mouseY) {
        super.render(stack, mouseX, mouseY);
        Fonts.sfuy.drawText(stack, setting.getName(), getX() + 4, getY() + 4f / 2f + 1, Theme.MainColor(0), 5.5f, 0.05f);
        String bind = KeyStorage.getKey(setting.get());

        if (bind == null || setting.get() == -1) {
            bind = "Нету";
        }
        float spacing = activated ? 0.1f : 0.05f;
        float bindWidth = Fonts.sfuy.getWidth(bind, 4.5f, spacing);
        float x = getX() + getWidth() - 5 - bindWidth;
        float y = getY() + 3.5f / 2f + (3.5f / 2f);
        RenderUtility.drawRoundedRect(x - 1.5f + 0.5F, y - 1.5f, bindWidth + 3, 4.5f + 3, 1.5f, ColorUtils.rgba(0,0,0,90));
        Fonts.sfuy.drawText(stack, bind, x, y, activated ? Theme.MainColor(0) : Theme.MainColor(0), 4.5f, spacing);

        if (isHovered(mouseX, mouseY)) {
            if (MathUtil.isHovered(mouseX, mouseY, x - 1.5f + 0.5F, y - 1.5f, bindWidth + 3, 4.5f + 3)) {
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
        }
        setHeight(12);
    }

    @Override
    public void keyPressed(int key, int scanCode, int modifiers) {
        if (activated) {
            if (key == GLFW.GLFW_KEY_DELETE) {
                setting.set(-1);
                SoundUtil.playSound("guibindreset.wav");
                activated = false;
                return;
            }
            setting.set(key);
            SoundUtil.playSound("guibinding.wav");
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
            System.out.println(-100 + mouse);
            setting.set(-100 + mouse);
            activated = false;
        }

        super.mouseClick(mouseX, mouseY, mouse);
    }

    @Override
    public void mouseRelease(float mouseX, float mouseY, int mouse) {
        super.mouseRelease(mouseX, mouseY, mouse);
    }

    @Override
    public boolean isVisible() {
        return !hidden && setting.visible.get();
    }
}
