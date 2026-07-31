package xd.harm.voicechat;

import com.google.common.eventbus.Subscribe;
import de.maxhenkel.voicechat.Voicechat;
import de.maxhenkel.voicechat.VoicechatClient;
import de.maxhenkel.voicechat.intercompatibility.CommonCompatibilityManager;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.entity.Entity;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.client.CCustomPayloadPacket;
import net.minecraft.network.play.server.SCustomPayloadPlayPacket;
import net.minecraft.util.text.ITextComponent;
import xd.harm.Harmony;
import xd.harm.events.network.EventPacket;
import xd.harm.events.world.TickEvent;

public class HarmonyVoicechatBootstrap {
    private static final HarmonyVoicechatBootstrap EVENT_BRIDGE = new HarmonyVoicechatBootstrap();
    private static boolean initialized;
    private static boolean registered;
    private static boolean wasInWorld;

    public static synchronized void init() {
        if (initialized) {
            return;
        }

        new CommonBootstrap().initialize();
        new ClientBootstrap().initializeClient();

        if (!registered && Harmony.getInstance() != null) {
            Harmony.getInstance().getEventBus().register(EVENT_BRIDGE);
            registered = true;
        }

        initialized = true;
    }

    @Subscribe
    public void onTick(TickEvent event) {
        HarmonyClientCompatibilityManager manager = HarmonyClientCompatibilityManager.INSTANCE;

        if (manager == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        boolean inWorld = minecraft.player != null && minecraft.world != null && minecraft.getConnection() != null;

        if (inWorld && !wasInWorld) {
            if (CommonCompatibilityManager.INSTANCE instanceof HarmonyCommonCompatibilityManager) {
                ((HarmonyCommonCompatibilityManager) CommonCompatibilityManager.INSTANCE).getHarmonyNetManager().sendChannelRegistration();
            }
            manager.fireJoinWorld();
        } else if (!inWorld && wasInWorld) {
            manager.fireDisconnect();
        }

        wasInWorld = inWorld;
        manager.fireClientTick();
        manager.fireKeyStatePoll();
        manager.fireHandleKeyBinds();
    }

    public static void fireRenderHud(MatrixStack matrixStack, float partialTicks) {
        if (Boolean.getBoolean("bot.mode")) return;

        HarmonyClientCompatibilityManager manager = HarmonyClientCompatibilityManager.INSTANCE;

        if (manager != null) {
            manager.fireRenderHud(matrixStack, partialTicks);
        }
    }

    public static void fireRenderNameplate(Entity entity, ITextComponent displayName, MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight) {
    }

    public static void fireRenderPlayerVoiceIcon(Entity entity, MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight) {
        if (Boolean.getBoolean("bot.mode")) return;
        HarmonyVoicechatHud.renderSpeakingPlayerIcon(entity, matrixStack, buffer, packedLight);
    }

    @Subscribe
    public void onPacket(EventPacket event) {
        if (event.isSend() && event.getPacket() instanceof CCustomPayloadPacket) {
            CCustomPayloadPacket packet = (CCustomPayloadPacket) event.getPacket();

            if ("voicechat".equals(packet.getChannelName().getNamespace())) {
                System.out.println("[voicechat] Sending custom payload: " + packet.getChannelName());
            }
            return;
        }

        if (!event.isReceive()) {
            return;
        }

        IPacket<?> packet = event.getPacket();

        if (!(packet instanceof SCustomPayloadPlayPacket)) {
            return;
        }

        CommonCompatibilityManager compatibilityManager = CommonCompatibilityManager.INSTANCE;

        if (!(compatibilityManager instanceof HarmonyCommonCompatibilityManager)) {
            return;
        }

        if (((HarmonyCommonCompatibilityManager) compatibilityManager).getHarmonyNetManager().handleClientbound((SCustomPayloadPlayPacket) packet)) {
            event.setCancel(true);
        }
    }

    private static class CommonBootstrap extends Voicechat {
    }

    private static class ClientBootstrap extends VoicechatClient {
    }
}
