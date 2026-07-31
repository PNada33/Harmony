package xd.harm.bot;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SDisconnectPacket;
import net.minecraft.util.text.ITextComponent;

public class ForegroundBotPlayNetHandler extends ClientPlayNetHandler {
    private final BotSessionManager manager;

    public ForegroundBotPlayNetHandler(
            BotSessionManager manager,
            Minecraft mcIn,
            Screen previousGuiScreen,
            NetworkManager networkManagerIn,
            GameProfile profileIn
    ) {
        super(mcIn, previousGuiScreen, networkManagerIn, profileIn);
        this.manager = manager;
    }

    @Override
    public void handleDisconnect(SDisconnectPacket packetIn) {
        this.getNetworkManager().closeChannel(packetIn.getReason());
        manager.onForegroundPlayDisconnected(packetIn.getReason());
    }

    @Override
    public void onDisconnect(ITextComponent reason) {
        manager.onForegroundPlayDisconnected(reason);
    }
}
