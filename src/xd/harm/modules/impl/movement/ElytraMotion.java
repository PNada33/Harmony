package xd.harm.modules.impl.movement;

import com.google.common.eventbus.Subscribe;
import net.minecraft.entity.LivingEntity;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.impl.combat.HitAura;
import xd.harm.modules.settings.impl.SliderSetting;

@ModuleRegister(name = "ElytraMotion", category = Category.Movement, desc = "Позволяет зависнуть возле цели на элитрах")
public class ElytraMotion extends Module {
    public SliderSetting distance = new SliderSetting("Дист до цели", 2.5F, 1.5F, 3F, 0.1F);

    public ElytraMotion() {
        addSettings(distance);
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        LivingEntity target = HitAura.target;
        if (target == null) return;
        
        if (mc.player.isElytraFlying() && mc.player.getDistance(target) <= distance.get()) {
            ElytraPredict elytraPredict = mc.player.connection != null ? 
                xd.harm.Harmony.getInstance().getModuleManager().getElytraPredict() : null;
            
            boolean shouldPredictElytra = target.isElytraFlying() && 
                elytraPredict != null && 
                elytraPredict.isState() && 
                elytraPredict.canPredict(target);
            
            if (!shouldPredictElytra) {
                mc.player.setVelocity(0, 0, 0);
            }
        }
    }
}
