package xd.harm.voicechat;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import de.maxhenkel.voicechat.voice.client.ClientManager;
import de.maxhenkel.voicechat.voice.client.ClientVoicechat;
import de.maxhenkel.voicechat.voice.client.ClientVoicechatConnection;
import de.maxhenkel.voicechat.voice.client.TalkCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix3f;
import net.minecraft.util.math.vector.Matrix4f;

final class HarmonyVoicechatHud {
    private static final ResourceLocation MICROPHONE_ICON = new ResourceLocation("voicechat", "textures/icons/microphone.png");

    private HarmonyVoicechatHud() {
    }

    static void renderSpeakingPlayerIcon(Entity entity, MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight) {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft == null || minecraft.world == null || minecraft.player == null || minecraft.gameSettings.hideGUI) {
            return;
        }

        if (!(entity instanceof PlayerEntity) || entity == minecraft.player) {
            return;
        }

        ClientVoicechat client = ClientManager.getClient();
        if (!isVoicechatReady(client)) {
            return;
        }
        TalkCache talkCache = client.getTalkCache();

        if (talkCache == null || !talkCache.isTalking(entity)) {
            return;
        }

        renderMicrophoneAboveHead((PlayerEntity) entity, matrixStack, buffer, packedLight, minecraft);
    }

    private static boolean isVoicechatReady(ClientVoicechat client) {
        if (client == null) {
            return false;
        }

        ClientVoicechatConnection connection = client.getConnection();
        return connection != null && connection.isInitialized();
    }

    private static void renderMicrophoneAboveHead(PlayerEntity player, MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight, Minecraft minecraft) {
        float iconSize = 12.0F;
        float iconX = -iconSize + 5F;
        float iconY = -iconSize - 25.0F;

        matrixStack.push();
        matrixStack.translate(0.0D, player.getHeight() + 0.18D, 0.0D);
        matrixStack.rotate(minecraft.getRenderManager().getCameraOrientation());
        matrixStack.scale(-0.025F, -0.025F, 0.025F);

        IVertexBuilder builder = buffer.getBuffer(RenderType.getText(MICROPHONE_ICON));
        vertex(builder, matrixStack, iconX, iconY + iconSize, 0.0F, 1.0F, packedLight);
        vertex(builder, matrixStack, iconX + iconSize, iconY + iconSize, 1.0F, 1.0F, packedLight);
        vertex(builder, matrixStack, iconX + iconSize, iconY, 1.0F, 0.0F, packedLight);
        vertex(builder, matrixStack, iconX, iconY, 0.0F, 0.0F, packedLight);

        matrixStack.pop();
    }

    private static void vertex(IVertexBuilder builder, MatrixStack matrixStack, float x, float y, float u, float v, int packedLight) {
        MatrixStack.Entry entry = matrixStack.getLast();
        Matrix4f matrix = entry.getMatrix();
        Matrix3f normal = entry.getNormal();

        builder.pos(matrix, x, y, 0.0F)
                .color(255, 255, 255, 255)
                .tex(u, v)
                .overlay(OverlayTexture.NO_OVERLAY)
                .lightmap(packedLight)
                .normal(normal, 0.0F, 0.0F, -1.0F)
                .endVertex();
    }
}
