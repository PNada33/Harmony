package xd.harm.modules.impl.misc;

import com.google.common.eventbus.Subscribe;
import xd.harm.events.network.EventPacket;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.ModeSetting;
import net.minecraft.network.play.client.CChatMessagePacket;

@ModuleRegister(name = "BWJoinHelper", category = Category.Misc, desc = "Помогает просто написав номер арены зайти на неё")
public class BWJoinHelper extends Module {

    private boolean skipMessage = false;
    private final ModeSetting serverMode = new ModeSetting("Server Mode", "ForsCraft", "ForsCraft");
    private final BooleanSetting cancelMessage = new BooleanSetting("Cancel Message", false);

    public BWJoinHelper() {
        addSettings(serverMode, cancelMessage);
    }

    @Subscribe
    public void onPacket(EventPacket event) {
        if (!isState()) return;
        if (!event.isSend()) return;
        if (skipMessage) return;

        if (event.getPacket() instanceof CChatMessagePacket chatPacket) {
            String rawMessage = chatPacket.getMessage();

            if (rawMessage.startsWith("/") || rawMessage.length() > 6) return;

            String number = "";
            for (int i = rawMessage.length() - 1; i >= 0; i--) {
                if (Character.isDigit(rawMessage.charAt(i))) {
                    number = rawMessage.charAt(i) + number;
                } else {
                    break;
                }
            }

            if (number.length() > 0 && number.length() < 3) {
                skipMessage = true;
                mc.player.connection.sendPacket(new CChatMessagePacket("/bw rjoin BW-" + number));
                skipMessage = false;
                if (cancelMessage.get()) {
                    event.cancel();
                }
            }
        }
    }
}
