package xd.harm.bot;

import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.exceptions.InsufficientPrivilegesException;
import com.mojang.authlib.exceptions.InvalidCredentialsException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.login.IClientLoginNetHandler;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.ProtocolType;
import net.minecraft.network.login.client.CCustomPayloadLoginPacket;
import net.minecraft.network.login.client.CEncryptionResponsePacket;
import net.minecraft.network.login.server.SCustomPayloadLoginPacket;
import net.minecraft.network.login.server.SDisconnectLoginPacket;
import net.minecraft.network.login.server.SEnableCompressionPacket;
import net.minecraft.network.login.server.SEncryptionRequestPacket;
import net.minecraft.network.login.server.SLoginSuccessPacket;
import net.minecraft.util.CryptException;
import net.minecraft.util.CryptManager;
import net.minecraft.util.HTTPUtil;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.math.BigInteger;
import java.security.PublicKey;

public class ForegroundBotLoginNetHandler implements IClientLoginNetHandler {
    private final BotSessionManager manager;
    private final NetworkManager networkManager;
    private final Screen previousScreen;
    private final String nick;
    private final Minecraft mc = Minecraft.getInstance();

    public ForegroundBotLoginNetHandler(BotSessionManager manager, NetworkManager networkManager, Screen previousScreen, String nick) {
        this.manager = manager;
        this.networkManager = networkManager;
        this.previousScreen = previousScreen;
        this.nick = nick;
    }

    @Override
    public void handleEncryptionRequest(SEncryptionRequestPacket packetIn) {
        if (!nick.equalsIgnoreCase(mc.getSession().getUsername())) {
            ITextComponent reason = new StringTextComponent("Online-mode requires nick = current account");
            this.networkManager.closeChannel(reason);
            manager.onForegroundConnectFailed(reason.getString());
            return;
        }

        Cipher decryptCipher;
        Cipher encryptCipher;
        String serverHash;
        CEncryptionResponsePacket responsePacket;

        try {
            SecretKey secretKey = CryptManager.createNewSharedKey();
            PublicKey publicKey = packetIn.getPublicKey();
            serverHash = (new BigInteger(CryptManager.getServerIdHash(packetIn.getServerId(), publicKey, secretKey))).toString(16);
            decryptCipher = CryptManager.createNetCipherInstance(2, secretKey);
            encryptCipher = CryptManager.createNetCipherInstance(1, secretKey);
            responsePacket = new CEncryptionResponsePacket(secretKey, publicKey, packetIn.getVerifyToken());
        } catch (CryptException e) {
            ITextComponent reason = new StringTextComponent("Encryption setup failed");
            this.networkManager.closeChannel(reason);
            manager.onForegroundConnectFailed(reason.getString());
            return;
        }

        HTTPUtil.DOWNLOADER_EXECUTOR.submit(() -> {
            ITextComponent authError = this.joinServer(serverHash);
            if (authError != null) {
                this.networkManager.closeChannel(authError);
                manager.onForegroundConnectFailed(authError.getString());
                return;
            }

            this.networkManager.sendPacket(responsePacket, future -> this.networkManager.func_244777_a(decryptCipher, encryptCipher));
        });
    }

    private ITextComponent joinServer(String serverHash) {
        try {
            mc.getSessionService().joinServer(mc.getSession().getProfile(), mc.getSession().getToken(), serverHash);
            return null;
        } catch (AuthenticationUnavailableException e) {
            return new TranslationTextComponent("disconnect.loginFailedInfo", new TranslationTextComponent("disconnect.loginFailedInfo.serversUnavailable"));
        } catch (InvalidCredentialsException e) {
            return new TranslationTextComponent("disconnect.loginFailedInfo", new TranslationTextComponent("disconnect.loginFailedInfo.invalidSession"));
        } catch (InsufficientPrivilegesException e) {
            return new TranslationTextComponent("disconnect.loginFailedInfo", new TranslationTextComponent("disconnect.loginFailedInfo.insufficientPrivileges"));
        } catch (AuthenticationException e) {
            return new TranslationTextComponent("disconnect.loginFailedInfo", e.getMessage());
        }
    }

    @Override
    public void handleLoginSuccess(SLoginSuccessPacket packetIn) {
        this.networkManager.setConnectionState(ProtocolType.PLAY);
        this.networkManager.setNetHandler(new ForegroundBotPlayNetHandler(this.manager, this.mc, this.previousScreen, this.networkManager, packetIn.getProfile()));
        manager.onForegroundJoinedPlay(this.networkManager);
    }

    @Override
    public void handleDisconnect(SDisconnectLoginPacket packetIn) {
        this.networkManager.closeChannel(packetIn.getReason());
        this.mc.execute(() -> this.mc.displayGuiScreen(new DisconnectedScreen(this.previousScreen, net.minecraft.client.gui.DialogTexts.CONNECTION_FAILED, packetIn.getReason())));
        manager.onForegroundConnectFailed(packetIn.getReason().getString());
    }

    @Override
    public void handleEnableCompression(SEnableCompressionPacket packetIn) {
        if (!this.networkManager.isLocalChannel()) {
            this.networkManager.setCompressionThreshold(packetIn.getCompressionThreshold());
        }
    }

    @Override
    public void handleCustomPayloadLogin(SCustomPayloadLoginPacket packetIn) {
        this.networkManager.sendPacket(new CCustomPayloadLoginPacket(packetIn.getTransaction(), (PacketBuffer) null));
    }

    @Override
    public void onDisconnect(ITextComponent reason) {
        manager.onForegroundConnectFailed(reason.getString());
    }

    @Override
    public NetworkManager getNetworkManager() {
        return networkManager;
    }
}
