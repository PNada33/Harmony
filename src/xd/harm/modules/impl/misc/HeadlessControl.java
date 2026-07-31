package xd.harm.modules.impl.misc;

import net.minecraft.client.gui.screen.Screen;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;

@ModuleRegister(name = "HeadlessControl", category = Category.Misc, desc = "Windowed-панель управления Headless ботами")
public class HeadlessControl extends Module {
    private final HeadlessBridgeClient api = new HeadlessBridgeClient();
    private Screen parent;
    public HeadlessBridgeClient getApi() { return api; }
    @Override public boolean isVisibleInArrayList() { return false; }
    @Override public boolean isVisibleInKeyBindHud() { return false; }
    @Override public boolean onEnable() {
        super.onEnable();
        parent = mc.currentScreen;
        mc.displayGuiScreen(new HeadlessControlScreen(this, parent));
        return false;
    }
    @Override public boolean onDisable() {
        if (mc.currentScreen instanceof HeadlessControlScreen) mc.displayGuiScreen(parent);
        return super.onDisable();
    }
}
