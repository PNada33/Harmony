package xd.harm.command.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ConnectingScreen;
import net.minecraft.client.gui.screen.DirtMessageScreen;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import xd.harm.command.interfaces.Command;
import xd.harm.command.interfaces.Logger;
import xd.harm.command.interfaces.MultiNamedCommand;
import xd.harm.command.interfaces.Parameters;
import xd.harm.ui.mainmenu.MainScreen;

import java.util.List;

public class ReconnectCommand implements Command, MultiNamedCommand {
    private final Logger logger;

    public ReconnectCommand(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void execute(Parameters parameters) {
        Minecraft mc = Minecraft.getInstance();
        String ip = ConnectingScreen.IP;
        int port = ConnectingScreen.PORT;

        if (mc.isSingleplayer() || ip == null || ip.isEmpty() || port <= 0) {
            logger.log(TextFormatting.RED + "Нет сервера для перезахода");
            return;
        }

        if (mc.world != null) {
            mc.world.sendQuittingDisconnectingPacket();
        }

        if (mc.isIntegratedServerRunning()) {
            mc.unloadWorld(new DirtMessageScreen(new TranslationTextComponent("menu.savingLevel")));
        } else {
            mc.unloadWorld();
        }

        mc.displayGuiScreen(new ConnectingScreen(new MainScreen(), mc, ip, port));
    }

    @Override
    public String name() {
        return "reconnect";
    }

    @Override
    public String description() {
        return "Перезаход на последний сервер";
    }

    @Override
    public List<String> aliases() {
        return List.of("recon", "rejoin", "rct");
    }
}
