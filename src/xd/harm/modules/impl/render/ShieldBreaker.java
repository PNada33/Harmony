package xd.harm.modules.impl.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import org.lwjgl.opengl.GL11;
import xd.harm.events.render.EventRender3D;
import xd.harm.events.world.EventUpdate;
import xd.harm.events.network.EventPacket;
import net.minecraft.network.play.server.SEntityStatusPacket;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.utils.math.MathUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@ModuleRegister(name = "ShieldBreaker", category = Category.Render, desc = "Красивый купол и стекло при ударе в щит")
public class ShieldBreaker extends Module {

    private final List<Shard> shards = new ArrayList<>();
    private final Map<PlayerEntity, DomeState> domes = new HashMap<>();

    private static final int DOME_STACKS = 24;
    private static final int DOME_SLICES = 24;
    private static final int DOME_RING_SIZE = DOME_SLICES + 1;
    private static final int SHARD_COUNT = 100;

    private static final float TWO_PI = (float) (Math.PI * 2.0);
    private static final float DOME_RADIUS_X = 0.95f;
    private static final float DOME_RADIUS_Y = 1.25f;
    private static final float DOME_RADIUS_Z = 0.95f;
    private static final float DOME_Y_OFFSET = 0.9f;

    private static final int CORE_RED = 50;
    private static final int CORE_GREEN = 150;
    private static final int CORE_BLUE = 255;
    private static final int WIRE_RED = 100;
    private static final int WIRE_GREEN = 200;
    private static final int WIRE_BLUE = 255;

    private static final double SHARD_GRAVITY = 0.03;
    private static final double SHARD_DRAG = 0.88;

    private static final float[] DOME_Y = new float[DOME_STACKS + 1];
    private static final float[] DOME_ALPHA_FACTOR = new float[DOME_STACKS + 1];
    private static final float[] DOME_X = new float[(DOME_STACKS + 1) * DOME_RING_SIZE];
    private static final float[] DOME_Z = new float[(DOME_STACKS + 1) * DOME_RING_SIZE];

    static {
        for (int i = 0; i <= DOME_STACKS; i++) {
            float lat = (float) Math.PI * (-0.5f + (float) i / DOME_STACKS);
            float sinLat = (float) Math.sin(lat);
            float cosLat = (float) Math.cos(lat);
            int row = i * DOME_RING_SIZE;

            DOME_Y[i] = sinLat * DOME_RADIUS_Y;
            DOME_ALPHA_FACTOR[i] = 0.5f + 0.5f * (float) Math.sin(lat * 2.0f);

            for (int j = 0; j <= DOME_SLICES; j++) {
                float lng = TWO_PI * (float) j / DOME_SLICES;
                DOME_X[row + j] = (float) Math.cos(lng) * cosLat * DOME_RADIUS_X;
                DOME_Z[row + j] = (float) Math.sin(lng) * cosLat * DOME_RADIUS_Z;
            }
        }
    }

    @Subscribe
    public void onPacket(EventPacket e) {
        if (!e.isReceive()) return;
        
        if (e.getPacket() instanceof SEntityStatusPacket) {
            SEntityStatusPacket packet = (SEntityStatusPacket) e.getPacket();
            if (packet.getOpCode() == 30) {
                net.minecraft.entity.Entity entity = packet.getEntity(mc.world);
                if (entity instanceof PlayerEntity) {
                    PlayerEntity target = (PlayerEntity) entity;

                    DomeState state = domes.get(target);
                    if (state != null) {
                        state.alpha = 0f;
                        state.brokenTime = System.currentTimeMillis();
                    }

                    for (int i = 0; i < SHARD_COUNT; i++) {
                        float theta = MathUtil.random(0, TWO_PI);
                        float phi = (float) Math.acos(MathUtil.random(-1f, 1f));
                        double sinPhi = Math.sin(phi);
                        double cosPhi = Math.cos(phi);
                        double cosTheta = Math.cos(theta);
                        double sinTheta = Math.sin(theta);
                        
                        double dX = DOME_RADIUS_X * sinPhi * cosTheta;
                        double dY = DOME_RADIUS_Y * cosPhi;
                        double dZ = DOME_RADIUS_Z * sinPhi * sinTheta;
                        
                        double sx = target.getPosX() + dX;
                        double sy = target.getPosY() + DOME_Y_OFFSET + dY;
                        double sz = target.getPosZ() + dZ;
                        
                        shards.add(new Shard(sx, sy, sz, dX, dY, dZ));
                    }
                }
            }
        }
    }

    @Subscribe
    public void onUpdate(EventUpdate e) {
        long now = System.currentTimeMillis();
        List<? extends PlayerEntity> players = mc.world.getPlayers();
        for (int i = 0, size = players.size(); i < size; i++) {
            PlayerEntity player = players.get(i);
            if (player == mc.player || !player.isAlive()) continue;

            DomeState state = domes.get(player);
            if (state == null) {
                state = new DomeState(now);
                domes.put(player, state);
            }

            long delta = now - state.lastUpdate;
            state.lastUpdate = now;

            if (player.isActiveItemStackBlocking()) {
                if (now - state.brokenTime > 500) {
                    state.alpha += delta * 0.003f;
                    if (state.alpha > 1f) state.alpha = 1f;
                }
            } else {
                state.alpha -= delta * 0.005f;
                if (state.alpha < 0f) state.alpha = 0f;
            }
        }

        Iterator<Map.Entry<PlayerEntity, DomeState>> domeIterator = domes.entrySet().iterator();
        while (domeIterator.hasNext()) {
            Map.Entry<PlayerEntity, DomeState> entry = domeIterator.next();
            PlayerEntity player = entry.getKey();
            DomeState state = entry.getValue();
            if (!player.isAlive() || (!player.isActiveItemStackBlocking() && state.alpha <= 0)) {
                domeIterator.remove();
            }
        }

        for (int i = shards.size() - 1; i >= 0; i--) {
            Shard shard = shards.get(i);
            shard.update();
            if (shard.age > shard.maxAge) {
                shards.remove(i);
            }
        }
    }

    @Subscribe
    public void onRender(EventRender3D e) {
        boolean lineWidthChanged = false;
        RenderSystem.pushMatrix();
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.disableCull();
        RenderSystem.disableAlphaTest();
        RenderSystem.enableDepthTest();
        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ZERO);
        RenderSystem.depthMask(false);
        GL11.glShadeModel(GL11.GL_SMOOTH);

        try {
            MatrixStack ms = e.getStack();
            Vector3d cam = mc.gameRenderer.getActiveRenderInfo().getProjectedView();
            float pt = e.getPartialTicks();
            long now = System.currentTimeMillis();
            float time = (now % 10000L) / 10000f;
            float pulse = 0.8f + 0.2f * (float)Math.sin(now / 200.0);
            Tessellator tes = Tessellator.getInstance();
            BufferBuilder buf = tes.getBuffer();
            
            Iterator<Map.Entry<PlayerEntity, DomeState>> domeIterator = domes.entrySet().iterator();
            while (domeIterator.hasNext()) {
                Map.Entry<PlayerEntity, DomeState> entry = domeIterator.next();
                PlayerEntity entity = entry.getKey();
                DomeState state = entry.getValue();
                if (state.alpha <= 0.01f) continue;

                double x = MathHelper.lerp(pt, entity.lastTickPosX, entity.getPosX()) - cam.x;
                double y = MathHelper.lerp(pt, entity.lastTickPosY, entity.getPosY()) - cam.y;
                double z = MathHelper.lerp(pt, entity.lastTickPosZ, entity.getPosZ()) - cam.z;
                
                float currentAlpha = state.alpha * pulse;

                int caCore = (int)(40 * currentAlpha);
                int caWire = (int)(150 * currentAlpha);
                
                if (!lineWidthChanged) {
                    GL11.glLineWidth(2.0f);
                    lineWidthChanged = true;
                }

                ms.push();
                ms.translate(x, y + DOME_Y_OFFSET, z);
                ms.rotate(Vector3f.YP.rotationDegrees(time * 360f * 4f));
                
                drawDomeAdvanced(tes, buf, ms, caCore, caWire);
                
                ms.pop();
            }

            if (!shards.isEmpty()) {
                buf.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);

                for (int i = 0, size = shards.size(); i < size; i++) {
                    Shard s = shards.get(i);
                    float alpha = 1.0f - s.age * s.invMaxAge;
                    if (alpha <= 0) continue;
                    if (alpha > 1) alpha = 1;

                    double x = MathHelper.lerp(pt, s.prevX, s.x) - cam.x;
                    double y = MathHelper.lerp(pt, s.prevY, s.y) - cam.y;
                    double z = MathHelper.lerp(pt, s.prevZ, s.z) - cam.z;
                    
                    ms.push();
                    ms.translate(x, y, z);
                    ms.rotate(Vector3f.YP.rotationDegrees(s.rotY + s.spinY * pt));
                    ms.rotate(Vector3f.XP.rotationDegrees(s.rotX + s.spinX * pt));
                    ms.rotate(Vector3f.ZP.rotationDegrees(s.rotZ + s.spinZ * pt));
                    
                    addShard(buf, ms.getLast().getMatrix(), s.size, (int)(255 * alpha));
                    
                    ms.pop();
                }
                
                tes.draw();
            }
        } finally {
            if (lineWidthChanged) {
                GL11.glLineWidth(1.0f);
            }
            GL11.glShadeModel(GL11.GL_FLAT);
            RenderSystem.depthMask(true);
            RenderSystem.enableAlphaTest();
            RenderSystem.enableCull();
            RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
            RenderSystem.enableTexture();
            RenderSystem.disableBlend();
            RenderSystem.popMatrix();
        }
    }

    private void drawDomeAdvanced(Tessellator tes, BufferBuilder buf, MatrixStack ms, int coreAlpha, int wireAlpha) {
        Matrix4f mat = ms.getLast().getMatrix();

        buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < DOME_STACKS; i++) {
            int row0 = i * DOME_RING_SIZE;
            int row1 = row0 + DOME_RING_SIZE;
            float y0 = DOME_Y[i];
            float y1 = DOME_Y[i + 1];
            int a0 = (int)(coreAlpha * DOME_ALPHA_FACTOR[i]);
            int a1 = (int)(coreAlpha * DOME_ALPHA_FACTOR[i + 1]);

            for (int j = 0; j < DOME_SLICES; j++) {
                int p00 = row0 + j;
                int p01 = p00 + 1;
                int p10 = row1 + j;
                int p11 = p10 + 1;

                buf.pos(mat, DOME_X[p00], y0, DOME_Z[p00]).color(CORE_RED, CORE_GREEN, CORE_BLUE, a0).endVertex();
                buf.pos(mat, DOME_X[p01], y0, DOME_Z[p01]).color(CORE_RED, CORE_GREEN, CORE_BLUE, a0).endVertex();
                buf.pos(mat, DOME_X[p11], y1, DOME_Z[p11]).color(CORE_RED, CORE_GREEN, CORE_BLUE, a1).endVertex();
                buf.pos(mat, DOME_X[p10], y1, DOME_Z[p10]).color(CORE_RED, CORE_GREEN, CORE_BLUE, a1).endVertex();
            }
        }
        tes.draw();

        buf.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < DOME_STACKS; i += 2) {
            int row = i * DOME_RING_SIZE;
            float y0 = DOME_Y[i];

            for (int j = 0; j < DOME_SLICES; j += 2) {
                int p0 = row + j;
                int p1 = p0 + 1;

                buf.pos(mat, DOME_X[p0], y0, DOME_Z[p0]).color(WIRE_RED, WIRE_GREEN, WIRE_BLUE, wireAlpha).endVertex();
                buf.pos(mat, DOME_X[p1], y0, DOME_Z[p1]).color(WIRE_RED, WIRE_GREEN, WIRE_BLUE, wireAlpha).endVertex();
            }
        }
        for (int j = 0; j < DOME_SLICES; j += 2) {
            for (int i = 0; i < DOME_STACKS; i++) {
                int p0 = i * DOME_RING_SIZE + j;
                int p1 = p0 + DOME_RING_SIZE;

                buf.pos(mat, DOME_X[p0], DOME_Y[i], DOME_Z[p0]).color(WIRE_RED, WIRE_GREEN, WIRE_BLUE, wireAlpha).endVertex();
                buf.pos(mat, DOME_X[p1], DOME_Y[i + 1], DOME_Z[p1]).color(WIRE_RED, WIRE_GREEN, WIRE_BLUE, wireAlpha).endVertex();
            }
        }
        tes.draw();
    }

    private void addShard(BufferBuilder buf, Matrix4f mat, float size, int alpha) {
        float topY = size * 1.5f;
        float bottom = -size;

        addTri(buf, mat, 0, topY, 0, -size, bottom, size, size, bottom, size, alpha);
        addTri(buf, mat, 0, topY, 0, size, bottom, size, size, bottom, -size, alpha);
        addTri(buf, mat, 0, topY, 0, size, bottom, -size, -size, bottom, -size, alpha);
        addTri(buf, mat, 0, topY, 0, -size, bottom, -size, -size, bottom, size, alpha);
        addTri(buf, mat, -size, bottom, size, -size, bottom, -size, size, bottom, -size, alpha);
        addTri(buf, mat, -size, bottom, size, size, bottom, -size, size, bottom, size, alpha);
    }

    private void addTri(BufferBuilder buf, Matrix4f mat,
                        float x1, float y1, float z1,
                        float x2, float y2, float z2,
                        float x3, float y3, float z3,
                        int alpha) {
        buf.pos(mat, x1, y1, z1).color(WIRE_RED, WIRE_GREEN, WIRE_BLUE, alpha).endVertex();
        buf.pos(mat, x2, y2, z2).color(WIRE_RED, WIRE_GREEN, WIRE_BLUE, alpha).endVertex();
        buf.pos(mat, x3, y3, z3).color(WIRE_RED, WIRE_GREEN, WIRE_BLUE, alpha).endVertex();
    }

    @Override
    public boolean onDisable() {
        shards.clear();
        domes.clear();
        return super.onDisable();
    }

    private static class DomeState {
        float alpha = 0f;
        long lastUpdate;
        long brokenTime = 0;

        DomeState(long now) {
            lastUpdate = now;
        }
    }

    private static class Shard {
        double x, y, z;
        double prevX, prevY, prevZ;
        double vx, vy, vz;
        float rotX, rotY, rotZ;
        float spinX, spinY, spinZ;
        float size;
        float invMaxAge;
        int age;
        int maxAge;

        Shard(double sx, double sy, double sz, double dirX, double dirY, double dirZ) {
            x = prevX = sx;
            y = prevY = sy;
            z = prevZ = sz;
            
            double len = Math.sqrt(dirX*dirX + dirY*dirY + dirZ*dirZ);
            if (len > 0) {
                dirX /= len;
                dirY /= len;
                dirZ /= len;
            } else {
                dirX = 0; dirY = 1; dirZ = 0;
            }
            
            float speed = MathUtil.random(0.3f, 0.7f);
            vx = dirX * speed + MathUtil.random(-0.1f, 0.1f);
            vy = dirY * speed + MathUtil.random(0.1f, 0.3f);
            vz = dirZ * speed + MathUtil.random(-0.1f, 0.1f);
            
            rotX = MathUtil.random(0, 360);
            rotY = MathUtil.random(0, 360);
            rotZ = MathUtil.random(0, 360);
            
            spinX = MathUtil.random(-60, 60);
            spinY = MathUtil.random(-60, 60);
            spinZ = MathUtil.random(-60, 60);
            
            size = MathUtil.random(0.05f, 0.15f);
            age = 0;
            maxAge = (int) MathUtil.random(20, 50);
            invMaxAge = 1.0f / maxAge;
        }

        void update() {
            prevX = x;
            prevY = y;
            prevZ = z;
            
            x += vx;
            y += vy;
            z += vz;
            
            vy -= SHARD_GRAVITY;
            vx *= SHARD_DRAG;
            vz *= SHARD_DRAG;
            
            rotX += spinX;
            rotY += spinY;
            rotZ += spinZ;
            
            age++;
        }
    }
}
