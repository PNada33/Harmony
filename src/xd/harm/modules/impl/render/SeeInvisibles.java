package xd.harm.modules.impl.render;

import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.SliderSetting;
import com.google.common.eventbus.Subscribe;

import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import net.minecraft.entity.player.PlayerEntity;

@ModuleRegister(name = "SeeInvisibles", category = Category.Render, desc = "Показывает инвизок")
public class SeeInvisibles extends Module {


    public final SliderSetting alpha = new SliderSetting("Прозрачность", 0.5f, 0.3F, 1F, 0.1F);


public SeeInvisibles() {
        addSettings(alpha);
    }
}

