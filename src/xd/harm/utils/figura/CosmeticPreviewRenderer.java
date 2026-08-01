package xd.harm.utils.figura;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3f;
import org.lwjgl.opengl.GL11;

import java.util.List;

public final class CosmeticPreviewRenderer {

    private static final float BASE_YAW = 180.0F;
    private static final float BASE_PITCH = 0.0F;

    private CosmeticPreviewRenderer() {
    }

    public static boolean render(MatrixStack ms, FiguraAvatarLibrary.Entry entry,
                                 float x, float y, float w, float h,
                                 float previewRotation,
                                 List<BbModelRenderer> petPreview) {
        if (entry == null || !entry.moduleCard) {
            return false;
        }

        if (entry.petName != null) {
            return renderPetPreview(ms, petPreview, x, y, w, h, previewRotation);
        }
        if (entry.moduleName == null) {
            return false;
        }
        return renderFeaturePreview(entry.moduleName, x, y, w, h, previewRotation);
    }

    private static boolean renderFeaturePreview(String moduleName,
                                                float x, float y, float w, float h,
                                                float previewRotation) {
        Minecraft mc = Minecraft.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null) {
            return false;
        }

        float scale = Math.max(34f, Math.min(w, h) * 0.50f);
        String normalized = normalize(moduleName);

        FrozenPlayerPose frozen = FrozenPlayerPose.capture(player);
        applyPreviewPose(player);

        RenderContext context = beginStaticPlayerPreview(x, y, w, h, scale, player, previewRotation);
        boolean rendered = renderAttachedFeature(context.stack, normalized);
        endStaticPlayerPreview(context, player, frozen);
        return rendered;
    }

    private static RenderContext beginStaticPlayerPreview(float x, float y, float w, float h,
                                                          float scale, ClientPlayerEntity player,
                                                          float previewRotation) {
        Minecraft mc = Minecraft.getInstance();

        RenderSystem.pushMatrix();
        RenderSystem.translatef(x + w / 2f, y + h * 0.88f, 1050.0F);
        RenderSystem.scalef(1.0F, 1.0F, -1.0F);

        MatrixStack previewStack = new MatrixStack();
        previewStack.translate(0.0D, 0.0D, 1000.0D);
        previewStack.scale(scale, scale, scale);

        Quaternion bodyRotation = Vector3f.ZP.rotationDegrees(180.0F);
        previewStack.rotate(bodyRotation);
        previewStack.rotate(Vector3f.YP.rotationDegrees(previewRotation));

        EntityRendererManager renderManager = mc.getRenderManager();
        Quaternion cameraOrientation = Vector3f.XP.rotationDegrees(0.0F).copy();
        cameraOrientation.conjugate();
        renderManager.setCameraOrientation(cameraOrientation);
        renderManager.setRenderShadow(false);

        IRenderTypeBuffer.Impl buffer = mc.getRenderTypeBuffers().getBufferSource();
        RenderSystem.runAsFancy(() -> renderManager.renderEntityStatic(
                player,
                0.0D,
                0.0D,
                0.0D,
                0.0F,
                1.0F,
                previewStack,
                buffer,
                15728880
        ));
        buffer.finish();

        return new RenderContext(previewStack, renderManager);
    }

    private static void endStaticPlayerPreview(RenderContext context, ClientPlayerEntity player, FrozenPlayerPose frozen) {
        context.renderManager.setRenderShadow(true);
        frozen.restore(player);
        RenderSystem.popMatrix();
        RenderSystem.color4f(1f, 1f, 1f, 1f);
    }

    private static void applyPreviewPose(ClientPlayerEntity player) {
        player.prevRenderYawOffset = BASE_YAW;
        player.renderYawOffset = BASE_YAW;
        player.prevRotationYaw = BASE_YAW;
        player.rotationYaw = BASE_YAW;
        player.prevRotationYawHead = BASE_YAW;
        player.rotationYawHead = BASE_YAW;
        player.prevRotationPitch = BASE_PITCH;
        player.rotationPitch = BASE_PITCH;
        player.limbSwing = 0.0F;
        player.prevLimbSwingAmount = 0.0F;
        player.limbSwingAmount = 0.0F;
        player.prevDistanceWalkedModified = player.distanceWalkedModified;
        player.distanceWalkedModified = 0.0F;
        player.prevCameraYaw = player.cameraYaw;
        player.cameraYaw = 0.0F;
        player.rotationPitch = 0.0F;
    }

    private static boolean renderAttachedFeature(MatrixStack ms, String normalized) {
        if (isKatana(normalized)) {
            renderKatana(ms);
            return true;
        }
        if (isHat(normalized)) {
            renderHat(ms);
            return true;
        }
        if (isCape(normalized)) {
            renderCape(ms);
            return true;
        }
        if (isPat(normalized)) {
            renderPat(ms);
            return true;
        }
        return false;
    }

    private static boolean renderPetPreview(MatrixStack ms, List<BbModelRenderer> list,
                                            float x, float y, float w, float h,
                                            float previewRotation) {
        if (list == null || list.isEmpty()) {
            return false;
        }

        float minY = Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        float minX = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float minZ = Float.MAX_VALUE;
        float maxZ = -Float.MAX_VALUE;
        for (int i = 0; i < list.size(); i++) {
            BbModelRenderer renderer = list.get(i);
            minY = Math.min(minY, renderer.getMinY());
            maxY = Math.max(maxY, renderer.getMaxY());
            minX = Math.min(minX, renderer.getMinX());
            maxX = Math.max(maxX, renderer.getMaxX());
            minZ = Math.min(minZ, renderer.getMinZ());
            maxZ = Math.max(maxZ, renderer.getMaxZ());
        }

        float height = maxY - minY;
        if (height <= 0.001f) {
            return false;
        }

        float spanX = Math.max(0f, maxX - minX);
        float spanZ = Math.max(0f, maxZ - minZ);
        float radius = (float) Math.sqrt(spanX * spanX + spanZ * spanZ);
        float centerX = (minX + maxX) / 2f;
        float centerZ = (minZ + maxZ) / 2f;

        float byHeight = (h * 0.72f) / height;
        float byWidth = radius > 0.001f ? (w * 0.78f) / radius : byHeight;
        float size = Math.min(byHeight, byWidth);
        BbModelRenderer.Pose pose = new BbModelRenderer.Pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableAlphaTest();
        RenderSystem.defaultAlphaFunc();
        RenderSystem.enableTexture();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.color4f(1f, 1f, 1f, 1f);

        ms.push();
        ms.translate(x + w / 2f, y + h * 0.88f, 150f);
        ms.scale(size, -size, size);
        ms.rotate(Vector3f.XP.rotationDegrees(10f));
        ms.rotate(Vector3f.YP.rotationDegrees(previewRotation));
        ms.translate(-centerX, -minY, -centerZ);
        for (int i = 0; i < list.size(); i++) {
            try {
                list.get(i).render(ms, pose);
            } catch (Throwable ignored) {
            }
        }
        ms.pop();

        RenderSystem.enableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.disableAlphaTest();
        RenderSystem.disableBlend();
        RenderSystem.color4f(1f, 1f, 1f, 1f);
        return true;
    }

    private static void renderKatana(MatrixStack ms) {
        ms.push();
        ms.translate(-0.15f, 1.32f, -0.28f);
        ms.rotate(Vector3f.ZP.rotationDegrees(-45f));
        ms.rotate(Vector3f.XP.rotationDegrees(-3f));
        ms.scale(1.15f, 1.15f, 1.15f);
        CosmeticRenderer.renderKatanaGeometry(ms);
        ms.pop();
    }

    private static void renderHat(MatrixStack ms) {
        int color = CosmeticRenderer.color(CosmeticFeatures.CHINA_HAT);
        int r = color >> 16 & 255;
        int g = color >> 8 & 255;
        int b = color & 255;

        float radius = Math.max(0.34f, CosmeticRenderer.hatRadius() / 10.5f);
        float coneHeight = Math.max(0.18f, CosmeticRenderer.hatHeight() / 14.0f);
        int segments = Math.max(16, Math.round(CosmeticRenderer.hatSegments()));

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);

        ms.push();
        ms.translate(0f, 1.96f, 0f);

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        Matrix4f matrix = ms.getLast().getMatrix();

        if (CosmeticRenderer.hatOutline()) {
            RenderSystem.lineWidth(2.0f);
            buf.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
            for (int i = 0; i < segments; i++) {
                float angle = (float) (Math.PI * 2.0 * i / segments);
                float px = -MathHelper.sin(angle) * radius;
                float pz = MathHelper.cos(angle) * radius;
                buf.pos(matrix, px, 0f, pz).color(r, g, b, 135).endVertex();
            }
            tess.draw();
        }

        buf.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        buf.pos(matrix, 0f, coneHeight, 0f).color(10, 10, 10, 250).endVertex();
        for (int i = 0; i <= segments; i++) {
            float angle = (float) (Math.PI * 2.0 * i / segments);
            float px = -MathHelper.sin(angle) * radius;
            float pz = MathHelper.cos(angle) * radius;
            buf.pos(matrix, px, 0f, pz).color(r, g, b, 255).endVertex();
        }
        tess.draw();

        ms.pop();
        RenderSystem.enableTexture();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.color4f(1f, 1f, 1f, 1f);
    }

    private static void renderCape(MatrixStack ms) {
        int color = CosmeticRenderer.capeColor();
        int r = color >> 16 & 255;
        int g = color >> 8 & 255;
        int b = color & 255;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);

        ms.push();
        ms.translate(0f, 1.62f, 0.16f);
        ms.rotate(Vector3f.XP.rotationDegrees(3f));

        float halfWidth = 0.24f;
        float thickness = 0.02f;
        float segmentHeight = 0.10f;
        float z = -0.01f;
        for (int i = 0; i < 6; i++) {
            float y0 = -i * segmentHeight;
            float y1 = y0 - segmentHeight;
            box(ms, -halfWidth, y0, z, halfWidth, y1, z + thickness,
                    r, g, b, 220, false, 0f);
            box(ms, -halfWidth, y0, z, halfWidth, y1, z + thickness,
                    r, g, b, 255, true, 0f);
        }

        ms.pop();
        RenderSystem.enableTexture();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.color4f(1f, 1f, 1f, 1f);
    }

    private static void renderPat(MatrixStack ms) {
        int color = CosmeticRenderer.color("PatPatPat");
        int r = color >> 16 & 255;
        int g = color >> 8 & 255;
        int b = color & 255;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableTexture();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);

        ms.push();
        ms.translate(0f, 1.62f, 0f);
        ms.translate(0.42f, -0.10f, -0.02f);
        ms.rotate(Vector3f.ZP.rotationDegrees(-18f));
        ms.rotate(Vector3f.XP.rotationDegrees(14f));
        ms.scale(1.18f, 1.18f, 1.18f);

        box(ms, -0.11f, -0.04f, -0.08f, 0.13f, 0.10f, 0.08f, r, g, b, 215, false, 0f);
        box(ms, -0.11f, -0.04f, -0.08f, 0.13f, 0.10f, 0.08f, r, g, b, 255, true, 0f);
        for (int i = 0; i < 4; i++) {
            float fx = -0.09f + i * 0.055f;
            box(ms, fx, -0.16f, -0.05f, fx + 0.035f, -0.02f, 0.05f, r, g, b, 220, false, 0f);
        }
        box(ms, 0.11f, -0.01f, -0.04f, 0.18f, 0.07f, 0.04f, r, g, b, 210, false, 0f);

        ms.pop();
        RenderSystem.enableTexture();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.color4f(1f, 1f, 1f, 1f);
    }

    private static void box(MatrixStack ms, float x0, float y0, float z0,
                            float x1, float y1, float z1,
                            int r, int g, int b, int a,
                            boolean edges, float ex) {
        x0 -= ex; y0 -= ex; z0 -= ex;
        x1 += ex; y1 += ex; z1 += ex;
        Matrix4f m = ms.getLast().getMatrix();
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        if (edges) {
            RenderSystem.lineWidth(1.25f);
            buf.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
            edge(buf, m, x0, y0, z0, x1, y0, z0, r, g, b, a);
            edge(buf, m, x1, y0, z0, x1, y1, z0, r, g, b, a);
            edge(buf, m, x1, y1, z0, x0, y1, z0, r, g, b, a);
            edge(buf, m, x0, y1, z0, x0, y0, z0, r, g, b, a);
            edge(buf, m, x0, y0, z1, x1, y0, z1, r, g, b, a);
            edge(buf, m, x1, y0, z1, x1, y1, z1, r, g, b, a);
            edge(buf, m, x1, y1, z1, x0, y1, z1, r, g, b, a);
            edge(buf, m, x0, y1, z1, x0, y0, z1, r, g, b, a);
            edge(buf, m, x0, y0, z0, x0, y0, z1, r, g, b, a);
            edge(buf, m, x1, y0, z0, x1, y0, z1, r, g, b, a);
            edge(buf, m, x1, y1, z0, x1, y1, z1, r, g, b, a);
            edge(buf, m, x0, y1, z0, x0, y1, z1, r, g, b, a);
        } else {
            buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            face(buf, m, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, r, g, b, a);
            face(buf, m, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, r, g, b, a);
            face(buf, m, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, r, g, b, a);
            face(buf, m, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, r, g, b, a);
            face(buf, m, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, r, g, b, a);
            face(buf, m, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, r, g, b, a);
        }
        tess.draw();
    }

    private static void edge(BufferBuilder b, Matrix4f m,
                             float x0, float y0, float z0,
                             float x1, float y1, float z1,
                             int r, int g, int bl, int a) {
        vertex(b, m, x0, y0, z0, r, g, bl, a);
        vertex(b, m, x1, y1, z1, r, g, bl, a);
    }

    private static void face(BufferBuilder b, Matrix4f m,
                             float x0, float y0, float z0,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             int r, int g, int bl, int a) {
        vertex(b, m, x0, y0, z0, r, g, bl, a);
        vertex(b, m, x1, y1, z1, r, g, bl, a);
        vertex(b, m, x2, y2, z2, r, g, bl, a);
        vertex(b, m, x3, y3, z3, r, g, bl, a);
    }

    private static void vertex(BufferBuilder b, Matrix4f m,
                               float x, float y, float z,
                               int r, int g, int bl, int a) {
        b.pos(m, x, y, z).color(r, g, bl, a).endVertex();
    }

    private static boolean isKatana(String name) {
        return CosmeticFeatures.KATANA.equals(name) || name.contains("katana");
    }

    private static boolean isHat(String name) {
        return CosmeticFeatures.CHINA_HAT.equals(name) || name.contains("hat") || name.contains("china");
    }

    private static boolean isCape(String name) {
        return CosmeticFeatures.RAINCOAT.equals(name) || name.contains("raincoat") || name.contains("cape");
    }

    private static boolean isPat(String name) {
        return name.contains("patpat") || name.contains("pat_pat") || name.contains("pat");
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private static final class RenderContext {
        private final MatrixStack stack;
        private final EntityRendererManager renderManager;

        private RenderContext(MatrixStack stack, EntityRendererManager renderManager) {
            this.stack = stack;
            this.renderManager = renderManager;
        }
    }

    private static final class FrozenPlayerPose {
        private final float renderYawOffset;
        private final float prevRenderYawOffset;
        private final float rotationYaw;
        private final float prevRotationYaw;
        private float rotationPitch;
        private final float prevRotationPitch;
        private final float prevRotationYawHead;
        private final float rotationYawHead;
        private final float limbSwing;
        private final float prevLimbSwingAmount;
        private final float limbSwingAmount;
        private final float prevDistanceWalkedModified;
        private final float distanceWalkedModified;
        private final float prevCameraYaw;
        private final float cameraYaw;

        private FrozenPlayerPose(ClientPlayerEntity player) {
            this.renderYawOffset = player.renderYawOffset;
            this.prevRenderYawOffset = player.prevRenderYawOffset;
            this.rotationYaw = player.rotationYaw;
            this.prevRotationYaw = player.prevRotationYaw;
            this.rotationPitch = player.rotationPitch;
            this.prevRotationPitch = player.prevRotationPitch;
            this.prevRotationYawHead = player.prevRotationYawHead;
            this.rotationYawHead = player.rotationYawHead;
            this.limbSwing = player.limbSwing;
            this.prevLimbSwingAmount = player.prevLimbSwingAmount;
            this.limbSwingAmount = player.limbSwingAmount;
            this.prevDistanceWalkedModified = player.prevDistanceWalkedModified;
            this.distanceWalkedModified = player.distanceWalkedModified;
            this.prevCameraYaw = player.prevCameraYaw;
            this.cameraYaw = player.cameraYaw;
            this.rotationPitch = player.rotationPitch;

        }

        private static FrozenPlayerPose capture(ClientPlayerEntity player) {
            return new FrozenPlayerPose(player);
        }

        private void restore(ClientPlayerEntity player) {
            player.renderYawOffset = this.renderYawOffset;
            player.prevRenderYawOffset = this.prevRenderYawOffset;
            player.rotationYaw = this.rotationYaw;
            player.prevRotationYaw = this.prevRotationYaw;
            player.rotationPitch = this.rotationPitch;
            player.prevRotationPitch = this.prevRotationPitch;
            player.prevRotationYawHead = this.prevRotationYawHead;
            player.rotationYawHead = this.rotationYawHead;
            player.limbSwing = this.limbSwing;
            player.prevLimbSwingAmount = this.prevLimbSwingAmount;
            player.limbSwingAmount = this.limbSwingAmount;
            player.prevDistanceWalkedModified = this.prevDistanceWalkedModified;
            player.distanceWalkedModified = this.distanceWalkedModified;
            player.prevCameraYaw = this.prevCameraYaw;
            player.cameraYaw = this.cameraYaw;
            player.rotationPitch = this.rotationPitch;
        }
    }
}
