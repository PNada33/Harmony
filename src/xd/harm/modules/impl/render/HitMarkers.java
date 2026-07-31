package xd.harm.modules.impl.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import org.lwjgl.opengl.GL11;
import xd.harm.events.combat.AttackEvent;
import xd.harm.events.render.EventRender3D;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.ColorSetting;
import xd.harm.modules.settings.impl.SliderSetting;
import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@ModuleRegister(name = "HitMarkers", category = Category.Render, desc = "Показывает место попадания по игроку")
public class HitMarkers extends Module {

    private final SliderSetting size = new SliderSetting("Размер", 0.08F, 0.02F, 0.25F, 0.01F);
    private final SliderSetting duration = new SliderSetting("Длительность", 1.0F, 0.1F, 5.0F, 0.1F);
    private final BooleanSetting clientTheme = new BooleanSetting("Цвет клиента", true);
    private final ColorSetting color = new ColorSetting("Цвет", new Color(255, 50, 50).getRGB());
    private final BooleanSetting outline = new BooleanSetting("Обводка", true);
    private final BooleanSetting followTarget = new BooleanSetting("Следовать за целью", true);

    private final List<HitMark> marks = new ArrayList<>();

    public HitMarkers() {
        color.setVisible(() -> !clientTheme.get());
        addSettings(size, duration, clientTheme, color, outline, followTarget);
    }

    @Subscribe
    public void onAttack(AttackEvent e) {
        if (mc.player == null || !(e.entity instanceof LivingEntity)) return;

        LivingEntity target = (LivingEntity) e.entity;
        Vector3d eye = mc.player.getEyePosition(1.0F);
        Vector3d look = mc.player.getLookVec();
        Vector3d end = eye.add(look.scale(mc.player.getDistance(target) + 1.0));
        AxisAlignedBB box = target.getBoundingBox().grow(0.1);

        Vector3d hit = box.rayTrace(eye, end).orElse(target.getPositionVec().add(0, target.getHeight() * 0.5, 0));
        marks.add(new HitMark(target, hit, target.getPositionVec(), System.currentTimeMillis()));
    }

    @Subscribe
    public void onRender(EventRender3D e) {
        if (marks.isEmpty()) return;

        long now = System.currentTimeMillis();
        float dur = duration.get() * 1000;
        int idx = 0;
        Vector3d cam = mc.gameRenderer.getActiveRenderInfo().getProjectedView();

        Iterator<HitMark> it = marks.iterator();
        while (it.hasNext()) {
            HitMark m = it.next();
            long age = now - m.time;

            if (age > dur || m.target == null || !m.target.isAlive() || m.target.removed) {
                it.remove();
                continue;
            }

            float alpha = 1.0F - (float) age / dur;
            if (alpha <= 0.01F) {
                it.remove();
                continue;
            }

            Vector3d pos = m.hit;
            if (followTarget.get() && m.target.isAlive()) {
                Vector3d offset = m.hit.subtract(m.targetPos);
                float pt = e.getPartialTicks();
                Vector3d current = new Vector3d(
                        lerp(m.target.lastTickPosX, m.target.getPosX(), pt),
                        lerp(m.target.lastTickPosY, m.target.getPosY(), pt),
                        lerp(m.target.lastTickPosZ, m.target.getPosZ(), pt)
                );
                pos = current.add(offset);
            }

            drawCube(e.getStack(), pos, cam, alpha, idx++);
        }
    }

    private double lerp(double prev, double curr, float pt) {
        return prev + (curr - prev) * pt;
    }

    private int getColor(int idx) {
        return clientTheme.get() ? Theme.MainColor(idx) : color.get();
    }

    private void drawCube(MatrixStack ms, Vector3d pos, Vector3d cam, float alpha, int idx) {
        double x = pos.x - cam.x;
        double y = pos.y - cam.y;
        double z = pos.z - cam.z;

        float s = size.get();
        int c = getColor(idx);
        int r = (c >> 16) & 0xFF;
        int g = (c >> 8) & 0xFF;
        int b = c & 0xFF;
        int a = (int) (255 * alpha);

        ms.push();
        ms.translate(x, y, z);

        RenderSystem.disableTexture();
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.defaultBlendFunc();

        Matrix4f mat = ms.getLast().getMatrix();
        Tessellator tes = Tessellator.getInstance();
        BufferBuilder buf = tes.getBuffer();

        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        quad(buf, mat, -s, -s, -s, -s, s, -s, s, s, -s, s, -s, -s, r, g, b, a);
        quad(buf, mat, -s, -s, s, s, -s, s, s, s, s, -s, s, s, r, g, b, a);
        quad(buf, mat, -s, s, -s, -s, s, s, s, s, s, s, s, -s, r, g, b, a);
        quad(buf, mat, -s, -s, -s, s, -s, -s, s, -s, s, -s, -s, s, r, g, b, a);
        quad(buf, mat, -s, -s, -s, -s, -s, s, -s, s, s, -s, s, -s, r, g, b, a);
        quad(buf, mat, s, -s, -s, s, s, -s, s, s, s, s, -s, s, r, g, b, a);
        tes.draw();

        if (outline.get()) {
            RenderSystem.lineWidth(2.0F);
            buf.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
            drawEdges(buf, mat, s, a);
            tes.draw();
        }

        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.enableTexture();
        ms.pop();
    }

    private void quad(BufferBuilder buf, Matrix4f mat,
                      float x1, float y1, float z1, float x2, float y2, float z2,
                      float x3, float y3, float z3, float x4, float y4, float z4,
                      int r, int g, int b, int a) {
        buf.pos(mat, x1, y1, z1).color(r, g, b, a).endVertex();
        buf.pos(mat, x2, y2, z2).color(r, g, b, a).endVertex();
        buf.pos(mat, x3, y3, z3).color(r, g, b, a).endVertex();
        buf.pos(mat, x4, y4, z4).color(r, g, b, a).endVertex();
    }

    private void drawEdges(BufferBuilder buf, Matrix4f mat, float s, int a) {
        edge(buf, mat, -s, -s, -s, s, -s, -s, a);
        edge(buf, mat, s, -s, -s, s, s, -s, a);
        edge(buf, mat, s, s, -s, -s, s, -s, a);
        edge(buf, mat, -s, s, -s, -s, -s, -s, a);
        edge(buf, mat, -s, -s, s, s, -s, s, a);
        edge(buf, mat, s, -s, s, s, s, s, a);
        edge(buf, mat, s, s, s, -s, s, s, a);
        edge(buf, mat, -s, s, s, -s, -s, s, a);
        edge(buf, mat, -s, -s, -s, -s, -s, s, a);
        edge(buf, mat, s, -s, -s, s, -s, s, a);
        edge(buf, mat, s, s, -s, s, s, s, a);
        edge(buf, mat, -s, s, -s, -s, s, s, a);
    }

    private void edge(BufferBuilder buf, Matrix4f mat, float x1, float y1, float z1, float x2, float y2, float z2, int a) {
        buf.pos(mat, x1, y1, z1).color(0, 0, 0, a).endVertex();
        buf.pos(mat, x2, y2, z2).color(0, 0, 0, a).endVertex();
    }

    @Override
    public boolean onDisable() {
        marks.clear();
        return super.onDisable();
    }

    private static class HitMark {
        LivingEntity target;
        Vector3d hit;
        Vector3d targetPos;
        long time;

        HitMark(LivingEntity target, Vector3d hit, Vector3d targetPos, long time) {
            this.target = target;
            this.hit = hit;
            this.targetPos = targetPos;
            this.time = time;
        }
    }
}
