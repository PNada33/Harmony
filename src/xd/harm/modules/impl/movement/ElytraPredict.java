package xd.harm.modules.impl.movement;

import net.minecraft.entity.LivingEntity;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.SliderSetting;
import xd.harm.utils.math.StopWatch;

@ModuleRegister(name = "ElytraPredict", category = Category.Movement, desc = "Смещает хитбокс противника во время полёта на элитрах для перегона на элитрах")
public class ElytraPredict extends Module {
    public SliderSetting distance = new SliderSetting("Дистанция обгона", 3.0F, 0.0F, 4.25F, 0.05F);
    
    private final StopWatch timer = new StopWatch();
    private boolean disabled = false;

    public ElytraPredict() {
        addSettings(distance);
    }

    public double getDistance(LivingEntity target) {
        return distance.get();
    }

    public boolean canPredict(LivingEntity target) {
        if (mc.player.hurtTime > 0 && target.swingProgress < 0.5f) {
            disabled = true;
            timer.reset();
        }
        if (timer.hasTimeElapsed(500)) {
            disabled = false;
        }
        return !disabled;
    }
}
