package xd.harm.modules.impl.player;

import net.minecraft.client.gui.screen.Screen;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.voicechat.HarmonyVoicechatBootstrap;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

@ModuleRegister(name = "VoiceChat", category = Category.Player, desc = "Simple Voice Chat")
public class VoiceChat extends Module {
    private static final String VOICECHAT_SCREEN = "de.maxhenkel.voicechat.gui.VoiceChatScreen";

    @Override
    public boolean onEnable() {
        super.onEnable();
        openVoiceChat();
        setState(false, true);
        return false;
    }

    private void openVoiceChat() {
        try {
            HarmonyVoicechatBootstrap.init();

            Class<?> screenClass = Class.forName(VOICECHAT_SCREEN);

            if (!isMcpRemapped(screenClass)) {
                print("VoiceChat jar is not remapped for this client.");
                return;
            }

            Constructor<?> constructor = screenClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            Object screen = constructor.newInstance();

            if (!(screen instanceof Screen)) {
                print("VoiceChat screen is not compatible with this client screen class.");
                return;
            }

            mc.displayGuiScreen((Screen) screen);
        } catch (ClassNotFoundException e) {
            print("VoiceChat classes are not in the client jar.");
        } catch (Throwable throwable) {
            print("VoiceChat failed to open: " + throwable.getClass().getSimpleName() + " - " + throwable.getMessage());
            throwable.printStackTrace();
        }
    }

    private boolean isMcpRemapped(Class<?> screenClass) {
        boolean hasMcpInit = hasDeclaredMethod(screenClass, "init");
        boolean hasSrgInit = hasDeclaredMethod(screenClass, "func_231023_e_");
        return hasMcpInit || !hasSrgInit;
    }

    private boolean hasDeclaredMethod(Class<?> clazz, String name) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }
}
