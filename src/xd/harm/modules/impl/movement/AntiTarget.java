package xd.harm.modules.impl.movement;

import com.google.common.eventbus.Subscribe;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.impl.combat.HitAura;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.ModeSetting;
import xd.harm.modules.settings.impl.SliderSetting;

@ModuleRegister(name = "AntiTarget", category = Category.Movement, desc = "Не даёт вас за таргетить на элитрах")
public class AntiTarget extends Module {
    public ModeSetting mode = new ModeSetting("Режим", "Обычный", new String[]{"Обычный", "Быстрый"});
    public SliderSetting gradus = new SliderSetting("Наклон", 35f, 30f, 50f, 1f).setVisible(() -> mode.is("Обычный"));
    public SliderSetting speed = new SliderSetting("Скорость", 1.95f, 1.9f, 2.7f, 0.01f).setVisible(() -> mode.is("Обычный"));

    public AntiTarget() {
        addSettings(mode, gradus, speed);
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (HitAura.target != null) return;
        
        if (mc.player.isElytraFlying()) {
            float targetPitch = -gradus.get();
            if (mode.is("Обычный")) {
                mc.player.rotationPitch = targetPitch;
            } else {
                mc.player.rotationPitch = -42.5f;
                mc.player.rotationYaw = 45;
            }
        }
    }
}
