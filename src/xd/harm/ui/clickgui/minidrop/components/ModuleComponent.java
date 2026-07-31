package xd.harm.ui.clickgui.minidrop.components;

import com.mojang.blaze3d.matrix.MatrixStack;
import xd.harm.modules.api.Module;
import xd.harm.modules.settings.Setting;
import xd.harm.modules.settings.impl.*;
import xd.harm.ui.clickgui.minidrop.ClickGuiScreen;
import xd.harm.ui.clickgui.minidrop.components.builder.Component;
import xd.harm.ui.clickgui.minidrop.components.settings.*;
import xd.harm.ui.clickgui.minidrop.utils.MathUtil;
import xd.harm.ui.clickgui.minidrop.utils.ColorUtils;
import xd.harm.ui.clickgui.minidrop.utils.RenderHelper;
import xd.harm.utils.render.gl.Stencil;
import xd.harm.utils.render.font.Fonts;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ModuleComponent extends Component {
    private final Module module;
    private final List<Component> components = new ArrayList<>();
    private BindSetting bindSetting;
    public float animation = 0;
    public boolean open;
    public boolean bind;

    public Module getModule() { return module; }
    public List<Component> getComponents() { return components; }
    public boolean isOpen() { return open; }
    public boolean isBind() { return bind; }

    public ModuleComponent(Module module) {
        this.module = module;
        rebuildComponents();
    }

    public void rebuildComponents() {
        components.clear();
        for (Setting<?> setting : module.getSettings()) {
            if (setting instanceof BooleanSetting bool) {
                components.add(new BooleanComponent(bool));
            }
            if (setting instanceof SliderSetting slider) {
                components.add(new SliderComponent(slider));
            }
            if (setting instanceof BindSetting bindSetting) {
                this.bindSetting = bindSetting;
            }
            if (setting instanceof ModeSetting mode) {
                components.add(new ModeComponent(mode));
            }
            if (setting instanceof ModeListSetting mode) {
                components.add(new MultiBoxComponent(mode));
            }
            if (setting instanceof StringSetting string) {
                components.add(new StringComponent(string));
            }
            if (setting instanceof ColorSetting color) {
                components.add(new ColorComponent(color));
            }
        }
        if (getPanel() != null) {
            getPanel().updateHeight();
        }
    }

    @Override
    public void mouseRelease(float mouseX, float mouseY, int mouse) {
        for (Component component : components) {
            component.mouseRelease(mouseX, mouseY, mouse);
        }
        super.mouseRelease(mouseX, mouseY, mouse);
    }

    @Override
    public void render(MatrixStack stack, float mouseX, float mouseY) {
        super.render(stack, mouseX, mouseY);

        if (ClickGuiScreen.getInstance().getExpandedModule() != this) open = false;

        boolean hover = MathUtil.isHovered(mouseX, mouseY, getX() + 0.5f, getY() + 0.5f, getWidth() - 1, getHeight());

        animation = MathUtil.fast(animation, open ? 1 : 0, 10);

        int backgroundColor = module.isState() ?
                ColorUtils.rgba(35, 35, 40, 190) :
                ColorUtils.rgba(25, 25, 30, 135);

        RenderHelper.drawRoundedRect(getX() + 0.5f, getY() + 0.5f, getWidth() - 1, getHeight(), 3, backgroundColor);

        int textColor = module.isState() ? -1 : ColorUtils.rgba(161, 164, 177, 255);
        Fonts.sfui.drawText(stack, module.getName(), Math.round(getX() + 6), Math.round(getY() + 6.5F), textColor, 6.5f);

        boolean hasVisibleComponents = false;
        for (Component component : components) {
            if (component.isVisible()) {
                hasVisibleComponents = true;
                break;
            }
        }

        if (hasVisibleComponents) {
            RenderHelper.drawCircle(getX() + getWidth() - 8, getY() + 10, 3f,
                    module.isState() ? ColorUtils.getClickGuiColor(0) : ColorUtils.rgba(120, 120, 120, 255));
        }

        if (bind) {
            String keyName = module.getBind() == 0 ? "..." : GLFW.glfwGetKeyName(module.getBind(), 0);
            String bindText = "Key: " + (keyName != null ? keyName.toUpperCase() : "...");
            float bindTextWidth = Fonts.sfui.getWidth(bindText, 6f);
            float bindTextX = getX() + getWidth() - bindTextWidth - 12;
            float bindTextY = getY() + (getHeight() - Fonts.sfui.getHeight(6f)) / 2f;
            Fonts.sfui.drawText(stack, bindText, Math.round(bindTextX), Math.round(bindTextY), textColor, 6f);
        }

        if (animation > 0) {
            if (hasVisibleComponents) {
                RenderHelper.drawRectVerticalW(getX() + 5, getY() + 18, getWidth() - 10, 0.5f,
                        ColorUtils.rgba(60, 60, 60, (int) (200 * animation)),
                        ColorUtils.rgba(60, 60, 60, (int) (200 * animation)));
            }

            Stencil.initStencilToWrite();
            RenderHelper.drawRoundedRect(getX() + 0.5f, getY() + 0.5f, getWidth() - 1, getHeight() - 1, 3,
                    ColorUtils.rgba(23, 23, 23, (int) (255 * 0.33)));
            Stencil.readStencilBuffer(1);

            float y = getY() + 20;
            for (Component component : components) {
                if (component.isVisible()) {
                    component.setX(getX());
                    component.setY(y);
                    component.setWidth(getWidth());
                    component.render(stack, mouseX, mouseY);
                    y += component.getHeight();
                }
            }
            Stencil.uninitStencilBuffer();
        }
    }

    @Override
    public void mouseClick(float mouseX, float mouseY, int button) {
        if (MathUtil.isHovered(mouseX, mouseY, getX() + 1, getY() + 1, getWidth() - 2, 18)) {
            ModuleComponent openModule = ClickGuiScreen.getInstance().getExpandedModule();
            if (openModule != null && openModule != this && button == 1 && !module.getSettings().isEmpty()) {
                openModule.open = false;
            }
            if (button == 0 && !bind) {
                module.toggle();
            }
            if (button == 1 && !bind) {
                if (!module.getSettings().isEmpty()) {
                    open = !open;
                    if (open) {
                        ClickGuiScreen.getInstance().setExpandedModule(this);
                    }
                }
            }
            if (button == 2) {
                bind = !bind;
            }
        }
        if (isHovered(mouseX, mouseY)) {
            if (open) {
                for (Component component : components) {
                    if (component.isVisible()) component.mouseClick(mouseX, mouseY, button);
                }
            }
        }
        super.mouseClick(mouseX, mouseY, button);
    }

    @Override
    public void charTyped(char codePoint, int modifiers) {
        for (Component component : components) {
            if (component.isVisible()) component.charTyped(codePoint, modifiers);
        }
        super.charTyped(codePoint, modifiers);
    }

    @Override
    public void keyPressed(int key, int scanCode, int modifiers) {
        for (Component component : components) {
            if (component.isVisible()) component.keyPressed(key, scanCode, modifiers);
        }
        if (bind) {
            if (key == GLFW.GLFW_KEY_DELETE || key == GLFW.GLFW_KEY_ESCAPE) {
                module.setBind(0);
            } else {
                module.setBind(key);
            }
            bind = false;
        }
        super.keyPressed(key, scanCode, modifiers);
    }
}
