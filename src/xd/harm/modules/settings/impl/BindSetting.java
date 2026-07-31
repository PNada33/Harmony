package xd.harm.modules.settings.impl;

import java.util.function.Supplier;
import org.lwjgl.glfw.GLFW;
import xd.harm.modules.settings.Setting;
import xd.harm.utils.client.IMinecraft;
import xd.harm.utils.client.KeyStorage;

public class BindSetting extends Setting<Integer> implements IMinecraft {

    private boolean wasPressed = false;

    public BindSetting(String name, Integer defaultVal) {
        super(name, defaultVal);
    }

    @Override
    public BindSetting setVisible(Supplier<Boolean> bool) {
        return (BindSetting) super.setVisible(bool);
    }

    public boolean isPressed() {
        if (mc.currentScreen != null) return false;
        if (!isBound()) return false;

        boolean pressed = isInputPressed();

        if (pressed && !wasPressed) {
            wasPressed = true;
            return true;
        }

        if (!pressed) {
            wasPressed = false;
        }

        return false;
    }

    public boolean isKeyDown() {
        if (mc.currentScreen != null) return false;
        if (!isBound()) return false;
        return isInputPressed();
    }

    public String getKeyName() {
        int key = get();
        if (key == 0 || key == -1 || key == GLFW.GLFW_KEY_UNKNOWN) return "None";
        return KeyStorage.getKey(key);
    }

    private boolean isBound() {
        int bind = get();
        return bind != 0 && bind != GLFW.GLFW_KEY_UNKNOWN;
    }

    private boolean isInputPressed() {
        int bind = get();
        long windowHandle = mc.getMainWindow().getHandle();

        if (bind < 0) {
            int mouseButton = bind + 100;
            if (mouseButton < 0) {
                return false;
            }
            return GLFW.glfwGetMouseButton(windowHandle, mouseButton) == GLFW.GLFW_PRESS;
        }

        return GLFW.glfwGetKey(windowHandle, bind) == GLFW.GLFW_PRESS;
    }
}
