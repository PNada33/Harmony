package net.minecraft.client;

import org.lwjgl.glfw.GLFWNativeWin32;
import com.sun.jna.*;
import com.sun.jna.platform.win32.WinDef.*;
import com.sun.jna.win32.*;

public class WindowStyle {
    public interface DwmApi extends StdCallLibrary {
        DwmApi INSTANCE = Native.loadLibrary("dwmapi", DwmApi.class);
        int DwmSetWindowAttribute(HWND hwnd, int dwAttribute, Pointer pvAttribute, int cbAttribute);
    }

    public static void setDarkMode(long windowHandle) {
        long hwnd = GLFWNativeWin32.glfwGetWin32Window(windowHandle);
        HWND hwndJna = new HWND(new Pointer(hwnd));


        int DWMWA_USE_IMMERSIVE_DARK_MODE = 20;
        Memory darkModeEnabled = new Memory(4);
        darkModeEnabled.setInt(0, 1);
        int result = DwmApi.INSTANCE.DwmSetWindowAttribute(hwndJna, DWMWA_USE_IMMERSIVE_DARK_MODE, darkModeEnabled, 4);
        if (result != 0) {
        }


        int DWMWA_BORDER_COLOR = 34;
        Memory borderColor = new Memory(4);
        borderColor.setInt(0, 0x000000);
        result = DwmApi.INSTANCE.DwmSetWindowAttribute(hwndJna, DWMWA_BORDER_COLOR, borderColor, 4);
        if (result != 0) {
        }


        int DWMWA_CAPTION_COLOR = 35;
        Memory captionColor = new Memory(4);
        captionColor.setInt(0, 0x000000);
        result = DwmApi.INSTANCE.DwmSetWindowAttribute(hwndJna, DWMWA_CAPTION_COLOR, captionColor, 4);
        if (result != 0) {
        }
    }
}