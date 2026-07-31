package xd.harm.modules.impl.movement;

import com.google.common.eventbus.Subscribe;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BindSetting;
import xd.harm.modules.settings.impl.SliderSetting;

@ModuleRegister(name = "ElytraTarget", category = Category.Movement, desc = "Наводка на цель в элитре")
public class ElytraTarget extends Module {
    public final SliderSetting elytraFindRange = new SliderSetting("Дистанция наводки", 32.0f, 6.0f, 64.0f, 0.5f);
    public final SliderSetting elytraForward = new SliderSetting("Значение перегона", 3.0f, 0.0f, 6.0f, 0.1f);
    public final BindSetting forwardKey = new BindSetting("Кнопка перегона", -1);
    public static boolean shouldElytraTarget = false;

    public ElytraTarget() {
        addSettings(elytraFindRange, elytraForward, forwardKey);
    }

    @Subscribe
    public void onUpdate(EventUpdate eventUpdate) {
        if (forwardKey.isPressed()) {
            shouldElytraTarget = !shouldElytraTarget;
            String message = shouldElytraTarget ? "Перегон включен." : "Перегон выключен.";
            xd.harm.modules.api.Notify.NOTIFICATION_MANAGER.add(message, "", 2);
        }
    }

    @Override
    public boolean onDisable() {
        shouldElytraTarget = false;
        return super.onDisable();
    }
}
