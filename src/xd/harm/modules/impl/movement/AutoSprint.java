package xd.harm.modules.impl.movement;

import xd.harm.Harmony;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.impl.combat.HitAura;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.utils.player.MoveUtils;
import com.google.common.eventbus.Subscribe;
import net.minecraft.potion.Effects;

@ModuleRegister(name = "AutoSprint", category = Category.Movement, desc = "Автоматически даёт спринт")
public class AutoSprint extends Module {
    private final BooleanSetting omnidirectional = new BooleanSetting("Все направления", false);
    private final BooleanSetting keepSprint = new BooleanSetting("Сохранять спринт", true);

    public AutoSprint() {
        addSettings(omnidirectional, keepSprint);
    }

    public boolean isKeepSprint() {
        return keepSprint.get();
    }

    public boolean isOmnidirectional() {
        return isState() && omnidirectional.get();
    }

    @Subscribe
    public void onUpdate(EventUpdate e) {
        HitAura hitAura = Harmony.getInstance().getModuleManager().getHitAura();
        if (hitAura.isState()) {
            mc.player.setSprinting(false);
            return;
        }

        if (!MoveUtils.isMoving()) {
            mc.player.setSprinting(false);
            return;
        }

        if (omnidirectional.get()) {
            mc.gameSettings.keyBindSprint.setPressed(true);
        }

        mc.player.setSprinting(true);
    }
}
