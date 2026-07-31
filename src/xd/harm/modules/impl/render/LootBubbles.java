package xd.harm.modules.impl.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import org.lwjgl.opengl.GL11;
import xd.harm.events.render.DEngineEvent;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.utils.math.MathUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ModuleRegister(name = "LootBubbles", category = Category.Render, desc = "Пузыри вокруг предметов")
public class LootBubbles extends Module {
    private static final int BUBBLE_SLICES = 16;
    private static final int BUBBLE_STACKS = 16;
    private static final int BUBBLE_VERTEX_COUNT = BUBBLE_SLICES * BUBBLE_STACKS * 4;
    private static final int POP_PARTICLES = 30;
    private static final float TWO_PI = (float) (Math.PI * 2.0);
    private static final float[] BUBBLE_NX = new float[BUBBLE_VERTEX_COUNT];
    private static final float[] BUBBLE_NY = new float[BUBBLE_VERTEX_COUNT];
    private static final float[] BUBBLE_NZ = new float[BUBBLE_VERTEX_COUNT];
    private static final float[] BUBBLE_WIGGLE_PHASE = new float[BUBBLE_VERTEX_COUNT];
    private static final float[] BUBBLE_WIGGLE_SIN = new float[BUBBLE_VERTEX_COUNT];
    private static final float[] BUBBLE_WIGGLE_COS = new float[BUBBLE_VERTEX_COUNT];
    private static final float[] BUBBLE_WAVE_SIN = new float[BUBBLE_VERTEX_COUNT];
    private static final float[] BUBBLE_WAVE_COS = new float[BUBBLE_VERTEX_COUNT];
    private static final float[] BUBBLE_BASE_R = new float[BUBBLE_VERTEX_COUNT];
    private static final float[] BUBBLE_BASE_G = new float[BUBBLE_VERTEX_COUNT];
    private static final float[] BUBBLE_BASE_B = new float[BUBBLE_VERTEX_COUNT];
    private static final int[] BUBBLE_ALPHA = new int[BUBBLE_VERTEX_COUNT];

    static {
        int index = 0;
        for (int i = 0; i < BUBBLE_STACKS; i++) {
            float lat0 = (float) Math.PI * (-0.5f + (float) i / BUBBLE_STACKS);
            float y0 = (float) Math.sin(lat0);
            float zr0 = (float) Math.cos(lat0);

            float lat1 = (float) Math.PI * (-0.5f + (float) (i + 1) / BUBBLE_STACKS);
            float y1 = (float) Math.sin(lat1);
            float zr1 = (float) Math.cos(lat1);

            for (int j = 0; j < BUBBLE_SLICES; j++) {
                float lng0 = TWO_PI * (float) j / BUBBLE_SLICES;
                float x0_0 = (float) Math.cos(lng0) * zr0;
                float z0_0 = (float) Math.sin(lng0) * zr0;

                float x1_0 = (float) Math.cos(lng0) * zr1;
                float z1_0 = (float) Math.sin(lng0) * zr1;

                float lng1 = TWO_PI * (float) (j + 1) / BUBBLE_SLICES;
                float x0_1 = (float) Math.cos(lng1) * zr0;
                float z0_1 = (float) Math.sin(lng1) * zr0;

                float x1_1 = (float) Math.cos(lng1) * zr1;
                float z1_1 = (float) Math.sin(lng1) * zr1;

                index = putPrecomputedBubbleVertex(index, x0_0, y0, z0_0);
                index = putPrecomputedBubbleVertex(index, x0_1, y0, z0_1);
                index = putPrecomputedBubbleVertex(index, x1_1, y1, z1_1);
                index = putPrecomputedBubbleVertex(index, x1_0, y1, z1_0);
            }
        }
    }

    private final Map<Integer, TrackedItem> knownItems = new HashMap<>();
    private final List<BubblePop> pops = new ArrayList<>();
    private final List<ItemEntity> cachedItems = new ArrayList<>();
    private long updateSequence;

    @Subscribe
    public void onUpdate(EventUpdate e) {
        if (mc.world == null) {
            knownItems.clear();
            cachedItems.clear();
            pops.clear();
            return;
        }

        long seenAt = ++updateSequence;
        cachedItems.clear();
        
        for (Entity entity : mc.world.getAllEntities()) {
            if (entity instanceof ItemEntity) {
                ItemEntity item = (ItemEntity) entity;
                int id = item.getEntityId();
                TrackedItem tracked = knownItems.get(id);
                if (tracked == null) {
                    knownItems.put(id, new TrackedItem(item.getPosX(), item.getPosY(), item.getPosZ(), seenAt));
                } else {
                    tracked.set(item.getPosX(), item.getPosY(), item.getPosZ(), seenAt);
                }
                cachedItems.add(item);
            }
        }
        
        java.util.Iterator<Map.Entry<Integer, TrackedItem>> it = knownItems.entrySet().iterator();
        while (it.hasNext()) {
            TrackedItem item = it.next().getValue();
            if (item.seenAt != seenAt) {
                if (mc.player != null && mc.player.getDistanceSq(item.x, item.y, item.z) < 9.0) {
                    for (int i = 0; i < POP_PARTICLES; i++) {
                        pops.add(new BubblePop(item.x, item.y + 0.25, item.z));
                    }
                }
                it.remove();
            }
        }

        for (int i = pops.size() - 1; i >= 0; i--) {
            BubblePop p = pops.get(i);
            p.update();
            if (p.age > p.maxAge) {
                pops.remove(i);
            }
        }
    }

    @Subscribe
    public void onRender(DEngineEvent e) {
        if (cachedItems.isEmpty() && pops.isEmpty()) {
            return;
        }

        RenderSystem.pushMatrix();
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.disableCull();
        RenderSystem.disableAlphaTest();
        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        RenderSystem.depthMask(false);
        GL11.glShadeModel(GL11.GL_SMOOTH);

        try {
            MatrixStack ms = e.getMatrix();
            Vector3d cam = e.getActiveRenderInfo().getProjectedView();
            Matrix4f baseMatrix = ms.getLast().getMatrix();
            float pt = e.getPartialTicks();
            long time = System.currentTimeMillis();
            float waveTime = (time % 3000L) / 3000f;
            double wiggleTime = time / 200.0;
            float waveAngle = waveTime * TWO_PI;
            float waveSin = (float) Math.sin(waveAngle);
            float waveCos = (float) Math.cos(waveAngle);

            Tessellator tes = Tessellator.getInstance();
            BufferBuilder buf = tes.getBuffer();

            buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

            for (int i = 0, size = cachedItems.size(); i < size; i++) {
                ItemEntity entity = cachedItems.get(i);
                double x = MathHelper.lerp(pt, entity.lastTickPosX, entity.getPosX()) - cam.x;
                double y = MathHelper.lerp(pt, entity.lastTickPosY, entity.getPosY()) - cam.y;
                double z = MathHelper.lerp(pt, entity.lastTickPosZ, entity.getPosZ()) - cam.z;

                drawBubble(buf, baseMatrix, x, y + 0.25, z, 0.4f, entity.getEntityId(), wiggleTime, waveSin, waveCos);
            }

            for (int i = 0, size = pops.size(); i < size; i++) {
                BubblePop p = pops.get(i);
                float alpha = 1.0f - p.age * p.invMaxAge;
                if (alpha < 0) {
                    alpha = 0;
                }

                double x = MathHelper.lerp(pt, p.prevX, p.x) - cam.x;
                double y = MathHelper.lerp(pt, p.prevY, p.y) - cam.y;
                double z = MathHelper.lerp(pt, p.prevZ, p.z) - cam.z;

                ms.push();
                ms.translate(x, y, z);
                ms.rotate(Vector3f.YP.rotationDegrees(p.rotY + p.spinY * pt));
                ms.rotate(Vector3f.XP.rotationDegrees(p.rotX + p.spinX * pt));

                Matrix4f mat = ms.getLast().getMatrix();
                int cr = 200;
                int cg = 230;
                int cb = 255;
                int ca = (int) (200 * alpha);

                float s = p.size;
                buf.pos(mat, -s, -s, 0).color(cr, cg, cb, ca).endVertex();
                buf.pos(mat, -s, s, 0).color(cr, cg, cb, ca).endVertex();
                buf.pos(mat, s, s, 0).color(cr, cg, cb, ca).endVertex();
                buf.pos(mat, s, -s, 0).color(cr, cg, cb, ca).endVertex();

                ms.pop();
            }

            tes.draw();
        } finally {
            GL11.glShadeModel(GL11.GL_FLAT);
            RenderSystem.depthMask(true);
            RenderSystem.enableAlphaTest();
            RenderSystem.enableCull();
            RenderSystem.enableTexture();
            RenderSystem.disableBlend();
            RenderSystem.popMatrix();
        }
    }

    private static int putPrecomputedBubbleVertex(int index, float nx, float ny, float nz) {
        BUBBLE_NX[index] = nx;
        BUBBLE_NY[index] = ny;
        BUBBLE_NZ[index] = nz;
        float wigglePhase = nx * 2.0f + ny * 2.5f;
        float wavePhase = ny * TWO_PI;
        BUBBLE_WIGGLE_PHASE[index] = wigglePhase;
        BUBBLE_WIGGLE_SIN[index] = (float) Math.sin(wigglePhase);
        BUBBLE_WIGGLE_COS[index] = (float) Math.cos(wigglePhase);
        BUBBLE_WAVE_SIN[index] = (float) Math.sin(wavePhase);
        BUBBLE_WAVE_COS[index] = (float) Math.cos(wavePhase);
        BUBBLE_BASE_R[index] = MathHelper.lerp(0.4f, 0.5f + 0.5f * nx, 0.7f);
        BUBBLE_BASE_G[index] = MathHelper.lerp(0.5f, 0.5f + 0.5f * Math.abs(ny), 0.8f);
        BUBBLE_BASE_B[index] = MathHelper.lerp(0.6f, 0.5f + 0.5f * nz, 0.9f);
        BUBBLE_ALPHA[index] = 40 + (int) (60 * (1.0f - Math.abs(nz)));
        return index + 1;
    }

    private void drawBubble(BufferBuilder buf, Matrix4f mat, double x, double y, double z, float radius, int id, double wiggleTime, float waveSin, float waveCos) {
        double wiggleBase = wiggleTime + id;
        float wiggleSin = (float) Math.sin(wiggleBase);
        float wiggleCos = (float) Math.cos(wiggleBase);
        for (int i = 0; i < BUBBLE_VERTEX_COUNT; i++) {
            putBubbleVertex(buf, mat, x, y, z, radius, wiggleSin, wiggleCos, waveSin, waveCos, i);
        }
    }

    private void putBubbleVertex(BufferBuilder buf, Matrix4f mat, double x, double y, double z, float radius,
                                 float wiggleSin, float wiggleCos, float waveSin, float waveCos, int vertex) {
        float nx = BUBBLE_NX[vertex];
        float ny = BUBBLE_NY[vertex];
        float nz = BUBBLE_NZ[vertex];
        float wiggleWave = wiggleSin * BUBBLE_WIGGLE_COS[vertex] + wiggleCos * BUBBLE_WIGGLE_SIN[vertex];
        float wiggle = 1.0f + 0.04f * wiggleWave;
        float r = radius * wiggle;

        float wave = BUBBLE_WAVE_SIN[vertex] * waveCos + BUBBLE_WAVE_COS[vertex] * waveSin;

        int cr = (int) (MathHelper.clamp(BUBBLE_BASE_R[vertex] + wave * 0.1f, 0, 1) * 255);
        int cg = (int) (MathHelper.clamp(BUBBLE_BASE_G[vertex] - wave * 0.1f, 0, 1) * 255);
        int cb = (int) (MathHelper.clamp(BUBBLE_BASE_B[vertex] + wave * 0.1f, 0, 1) * 255);

        buf.pos(mat, (float) (x + nx * r), (float) (y + ny * r), (float) (z + nz * r)).color(cr, cg, cb, BUBBLE_ALPHA[vertex]).endVertex();
    }

    @Override
    public boolean onDisable() {
        knownItems.clear();
        pops.clear();
        cachedItems.clear();
        return super.onDisable();
    }

    private static class TrackedItem {
        double x, y, z;
        long seenAt;

        TrackedItem(double x, double y, double z, long seenAt) {
            set(x, y, z, seenAt);
        }

        void set(double x, double y, double z, long seenAt) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.seenAt = seenAt;
        }
    }

    private static class BubblePop {
        double x, y, z;
        double prevX, prevY, prevZ;
        double vx, vy, vz;
        float rotX, rotY;
        float spinX, spinY;
        float size;
        float invMaxAge;
        int age;
        int maxAge;

        BubblePop(double sx, double sy, double sz) {
            x = prevX = sx;
            y = prevY = sy;
            z = prevZ = sz;
            
            float speed = MathUtil.random(0.05f, 0.15f);
            float theta = MathUtil.random(0, TWO_PI);
            float phi = (float) Math.acos(MathUtil.random(-1f, 1f));
            double sinPhi = Math.sin(phi);
            
            vx = speed * sinPhi * Math.cos(theta);
            vy = speed * Math.cos(phi);
            vz = speed * sinPhi * Math.sin(theta);
            
            rotX = MathUtil.random(0, 360);
            rotY = MathUtil.random(0, 360);
            spinX = MathUtil.random(-30, 30);
            spinY = MathUtil.random(-30, 30);
            
            size = MathUtil.random(0.015f, 0.04f);
            age = 0;
            maxAge = (int) MathUtil.random(15, 30);
            invMaxAge = 1.0f / maxAge;
        }

        void update() {
            prevX = x; prevY = y; prevZ = z;
            x += vx; y += vy; z += vz;
            vy -= 0.005;
            vx *= 0.85; vz *= 0.85;
            rotX += spinX;
            rotY += spinY;
            age++;
        }
    }
}
