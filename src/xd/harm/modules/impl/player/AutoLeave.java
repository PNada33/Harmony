package xd.harm.modules.impl.player;

import com.google.common.eventbus.Subscribe;

import xd.harm.config.FriendStorage;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.ModeSetting;
import xd.harm.modules.settings.impl.SliderSetting;
import xd.harm.utils.player.PlayerUtils;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.StringTextComponent;

@FieldDefaults(level = AccessLevel.PRIVATE)
@ModuleRegister(name = "AutoLeave", category = Category.Player, desc = "Автоматически ливает с сервер при нахождении игрок в радиусе")
public class AutoLeave extends Module {

    final ModeSetting action = new ModeSetting("Действие", "Кик", "Кик", "/hub", "/spawn", "/home");
    final SliderSetting distance = new SliderSetting("Дистанция", 50.0f, 1.0f, 100.0f, 1.0f);

    public AutoLeave() {
        addSettings(action, distance);
    }
    @Subscribe
    private void onUpdate(EventUpdate event) {
        mc.world.getPlayers().stream()
                .filter(this::isValidPlayer)
                .findFirst()
                .ifPresent(this::performAction);
    }

    private boolean isValidPlayer(PlayerEntity player) {
        return player.isAlive()
                && player.getHealth() > 0.0f
                && player.getDistance(mc.player) <= distance.get()
                && player != mc.player
                && PlayerUtils.isNameValid(player.getName().getString())
                && !FriendStorage.isFriend(player.getName().getString());
    }

    private void performAction(PlayerEntity player) {
        if (!action.get().equalsIgnoreCase("Кик")) {
            mc.player.sendChatMessage(action.get());
            mc.ingameGUI.func_238452_a_(new StringTextComponent("[AutoLeave] " + player.getGameProfile().getName()),
                    new StringTextComponent("test"), -1,
                    -1, -1);
        } else {
            mc.player.connection.getNetworkManager().closeChannel(new StringTextComponent("Ввышли с сервера! \n" + player.getGameProfile().getName()));
        }
        toggle();
    }
}

