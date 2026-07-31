package xd.harm.modules.impl.player;

import com.google.common.eventbus.Subscribe;
import xd.harm.events.world.TickEvent;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import net.minecraft.item.Items;

@ModuleRegister(name = "FastEXP", category = Category.Player, desc = "Позволяет кидать пузырёк опыта без кд")
public class FastEXP extends Module {
    @Subscribe
    public void onEvent(TickEvent e) {
        if (mc.player != null && mc.player.getHeldItemMainhand().getItem() == Items.EXPERIENCE_BOTTLE) {
            mc.rightClickDelayTimer = 1;
        }
    }
}
