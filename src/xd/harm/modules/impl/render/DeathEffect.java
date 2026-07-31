package xd.harm.modules.impl.render;

import xd.harm.events.combat.AttackEvent;
import com.google.common.eventbus.Subscribe;
import xd.harm.events.movement.CameraEvent;
import xd.harm.events.movement.EventMotion;
import xd.harm.events.network.EventPacket;
import xd.harm.events.render.EventDisplay;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.ModeSetting;
import xd.harm.modules.settings.impl.SliderSetting;
import xd.harm.utils.math.MathUtil;
import xd.harm.utils.math.StopWatch;
import xd.harm.utils.render.rect.RenderUtility;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.play.server.SDestroyEntitiesPacket;
import net.minecraft.network.play.server.SEntityStatusPacket;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import ru.hogoshi.Animation;
import ru.hogoshi.util.Easings;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.matrix.MatrixStack;
import org.lwjgl.opengl.GL11;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.awt.Color;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

@ModuleRegister(name = "DeathEffect", category = Category.Render, desc = "Эффект при убийстве игрока")
public class DeathEffect extends Module {
    private static final int MAX_POSITIONS = 50;
    private static final long EFFECT_LIFESPAN = 2500L;
    private static final long SOUND_COOLDOWN = 500L;
    private static final float INV_255 = 1.0F / 255.0F;
    private static final int RING_SEGMENTS = 36;
    private static final int SPARK_COUNT = 12;
    private static final int RUNE_COUNT = 12;
    private static final int PARTICLE_COUNT = 40;
    private static final int LIGHTNING_ARC_SEGMENTS = 4;
    private static final int WIRE_SPHERE_STACKS = 12;
    private static final int WIRE_SPHERE_SLICES = 12;
    private static final float TWO_PI = (float) (Math.PI * 2.0);
    private static final float[] RING_COS = createDegreeTrig(RING_SEGMENTS + 1, 10.0F, true);
    private static final float[] RING_SIN = createDegreeTrig(RING_SEGMENTS + 1, 10.0F, false);
    private static final float[] RUNE_COS = createCircularTrig(RUNE_COUNT, true);
    private static final float[] RUNE_SIN = createCircularTrig(RUNE_COUNT, false);
    private static final float[] SPARK_SEED_ABS_SIN = createLinearTrig(SPARK_COUNT, 13.0F, false, true);
    private static final float[] SPARK_SEED_COS = createLinearTrig(SPARK_COUNT, 13.0F, true, false);
    private static final float[] SPARK_DIR_COS = createCircularTrig(SPARK_COUNT, true);
    private static final float[] SPARK_DIR_SIN = createCircularTrig(SPARK_COUNT, false);
    private static final float[] PARTICLE_PHASE_COS = createLinearTrig(PARTICLE_COUNT, 25.0F, true, false);
    private static final float[] PARTICLE_PHASE_SIN = createLinearTrig(PARTICLE_COUNT, 25.0F, false, false);
    private static final float[] LIGHTNING_X = createLightningAxis(true);
    private static final float[] LIGHTNING_Z = createLightningAxis(false);
    private static final float[] STAR_COS = createDegreeTrig(new float[]{0.0F, 144.0F, 288.0F, 72.0F, 216.0F, 0.0F}, true);
    private static final float[] STAR_SIN = createDegreeTrig(new float[]{0.0F, 144.0F, 288.0F, 72.0F, 216.0F, 0.0F}, false);
    private static final float[] STAR_NORMAL_X = createStarNormals(true);
    private static final float[] STAR_NORMAL_Z = createStarNormals(false);
    private static final float[] WIRE_SPHERE_VERTICES = createWireSphereVertices();

    private Animation animate = new Animation();
    private boolean useAnimation;

    private final ModeSetting effectMode = new ModeSetting("Режим", "Тайм-стоп", "Тайм-стоп", "Девочка", "Прикольные");
    private final BooleanSetting onlyPlayer = new BooleanSetting("Только на игроков", true);
    private final SliderSetting volume = new SliderSetting("Громкость", 100.0f, 1.0f, 300.0f, 1.0f);

    private LivingEntity target;
    private long time;
    private final StopWatch stopWatch = new StopWatch();

    private float yaw, pitch;

    private final List<Vector3d> position = new ArrayList<>(MAX_POSITIONS);

    private int current;
    private Vector3d setPosition;

    private final List<KillEffect> killEffects = new ArrayList<>();
    private final Object killEffectsLock = new Object();
    private final Map<Integer, Long> recentAttacks = new HashMap<>();
    private final Set<Integer> deadEntityIds = new HashSet<>();
    private final Random random = new Random();

    private long lastSoundTime = 0;
    private Clip currentClip = null;
    private Clip currentSecondClip = null;

    public float back;

    private static float[] createDegreeTrig(int count, float stepDegrees, boolean cosine) {
        float[] result = new float[count];
        for (int i = 0; i < count; ++i) {
            double angle = Math.toRadians(i * stepDegrees);
            result[i] = (float) (cosine ? Math.cos(angle) : Math.sin(angle));
        }
        return result;
    }

    private static float[] createDegreeTrig(float[] degrees, boolean cosine) {
        float[] result = new float[degrees.length];
        for (int i = 0; i < degrees.length; ++i) {
            double angle = Math.toRadians(degrees[i]);
            result[i] = (float) (cosine ? Math.cos(angle) : Math.sin(angle));
        }
        return result;
    }

    private static float[] createCircularTrig(int count, boolean cosine) {
        float[] result = new float[count];
        for (int i = 0; i < count; ++i) {
            double angle = TWO_PI * i / count;
            result[i] = (float) (cosine ? Math.cos(angle) : Math.sin(angle));
        }
        return result;
    }

    private static float[] createLinearTrig(int count, float step, boolean cosine, boolean absolute) {
        float[] result = new float[count];
        for (int i = 0; i < count; ++i) {
            double value = cosine ? Math.cos(i * step) : Math.sin(i * step);
            result[i] = (float) (absolute ? Math.abs(value) : value);
        }
        return result;
    }

    private static float[] createLightningAxis(boolean xAxis) {
        float[] result = new float[5];
        for (int i = 0; i < result.length; ++i) {
            double angle = TWO_PI * i / result.length;
            result[i] = (float) (xAxis ? Math.cos(angle) : Math.sin(angle)) * 1.8F;
        }
        return result;
    }

    private static float[] createStarNormals(boolean xAxis) {
        float[] result = new float[5];
        for (int i = 0; i < result.length; ++i) {
            float dx = STAR_COS[i + 1] - STAR_COS[i];
            float dz = STAR_SIN[i + 1] - STAR_SIN[i];
            float len = (float) Math.sqrt(dx * dx + dz * dz);
            result[i] = xAxis ? -dz / len : dx / len;
        }
        return result;
    }

    private static float[] createWireSphereVertices() {
        float[] vertices = new float[WIRE_SPHERE_STACKS * WIRE_SPHERE_SLICES * 2 * 3];
        int index = 0;
        for (int i = 0; i < WIRE_SPHERE_STACKS; ++i) {
            double lat0 = Math.PI * (-0.5 + (double) i / WIRE_SPHERE_STACKS);
            double z0 = Math.sin(lat0);
            double zr0 = Math.cos(lat0);
            double lat1 = Math.PI * (-0.5 + (double) (i + 1) / WIRE_SPHERE_STACKS);
            double z1 = Math.sin(lat1);
            double zr1 = Math.cos(lat1);
            for (int j = 0; j < WIRE_SPHERE_SLICES; ++j) {
                double lng0 = TWO_PI * j / WIRE_SPHERE_SLICES;
                double x0 = Math.cos(lng0);
                double y0 = Math.sin(lng0);
                vertices[index++] = (float) (x0 * zr0);
                vertices[index++] = (float) (y0 * zr0);
                vertices[index++] = (float) z0;
                vertices[index++] = (float) (x0 * zr1);
                vertices[index++] = (float) (y0 * zr1);
                vertices[index++] = (float) z1;
            }
        }
        return vertices;
    }

    public DeathEffect() {
        addSettings(effectMode, onlyPlayer, volume);
    }

    @Subscribe
    public void onAttack(AttackEvent e) {
        if (mc.player == null || mc.world == null)
            return;

        if (!(e.entity instanceof LivingEntity))
            return;

        if (onlyPlayer.get() && !(e.entity instanceof PlayerEntity))
            return;

        long now = System.currentTimeMillis();
        target = (LivingEntity) e.entity;
        time = now;
        recentAttacks.put(e.entity.getEntityId(), now);
    }

    @Subscribe
    public void onPacket(EventPacket e) {
        if (mc.player == null || mc.world == null)
            return;

        long now = System.currentTimeMillis();

        if (e.getPacket() instanceof SEntityStatusPacket packet) {
            if (packet.getOpCode() == 3 && target != null && packet.getEntity(mc.world) == target) {
                if (time + 1000 >= now) {
                    onKill(target);
                    target = null;
                }
            }
        }

        if (e.getPacket() instanceof SDestroyEntitiesPacket p) {
            for (int ids : p.getEntityIDs()) {
                if (target != null) {
                    if (ids == mc.player.getEntityId())
                        continue;

                    if (time + 400 >= now && target.getEntityId() == ids) {
                        if (mc.world.getEntityByID(ids) instanceof LivingEntity living) {
                            if (living.getHealth() < 5) {
                                onKill(target);
                                target = null;
                            }
                        }
                    }
                }
            }
        }
    }

    @Subscribe
    public void onUpdate(EventMotion e) {
        if (mc.player == null || mc.world == null)
            return;

        long now = System.currentTimeMillis();
        float partialTicks = mc.getRenderPartialTicks();

        if (useAnimation && effectMode.is("Тайм-стоп")) {
            if (mc.player.ticksExisted % 5 == 0 && current < position.size() - 1)
                current++;
            Vector3d player = new Vector3d(
                    MathUtil.interpolate(mc.player.getPosX(), mc.player.lastTickPosX, partialTicks),
                    MathUtil.interpolate(mc.player.getPosY(), mc.player.lastTickPosY, partialTicks),
                    MathUtil.interpolate(mc.player.getPosZ(), mc.player.lastTickPosZ, partialTicks))
                    .add(0, mc.player.getEyeHeight(), 0);

            if (position.size() < MAX_POSITIONS) {
                position.add(player);
            } else {
                position.remove(0);
                position.add(player);
            }
        }

        if (target != null) {
            if (time + 1000 >= now && target.getHealth() <= 0f) {
                onKill(target);
                target = null;
            }
        }

        if (effectMode.is("Тайм-стоп")) {
            if (stopWatch.isReached(500)) {
                animate = animate.animate(0, 1f, Easings.CIRC_OUT);
            }
            if (stopWatch.isReached(2000)) {
                useAnimation = false;
                position.clear();
                current = 0;
            }
        }

        Iterator<Map.Entry<Integer, Long>> attackIterator = recentAttacks.entrySet().iterator();
        while (attackIterator.hasNext()) {
            if (now - attackIterator.next().getValue() > 10000L) {
                attackIterator.remove();
            }
        }

        if (!recentAttacks.isEmpty()) {
            boolean onlyPlayers = onlyPlayer.get();
            for (LivingEntity entity : mc.world.getEntitiesWithinAABB(LivingEntity.class, mc.player.getBoundingBox().grow(64))) {
                if (entity == mc.player) continue;

                if (onlyPlayers && !(entity instanceof PlayerEntity)) continue;

                int entityId = entity.getEntityId();
                if ((entity.getHealth() <= 0 || entity.deathTime > 0) && deadEntityIds.add(entityId)) {
                    if (recentAttacks.remove(entityId) != null) {
                        onKill(entity);
                    }
                }
            }
        }

        if (mc.player.ticksExisted % 200 == 0) {
            deadEntityIds.clear();
        }

        synchronized (killEffectsLock) {
            for (int i = killEffects.size() - 1; i >= 0; --i) {
                if (now - killEffects.get(i).startTime > EFFECT_LIFESPAN) {
                    killEffects.remove(i);
                }
            }
        }
    }

    @Subscribe
    public void onCameraController(CameraEvent e) {
        if (useAnimation && effectMode.is("Тайм-стоп")) {
            float shake = (float) (animate.getValue() * 6);
            mc.getRenderManager().info.setDirection(
                    (float) (yaw + shake),
                    (float) (pitch + shake));

            back = MathUtil.fast(back, stopWatch.isReached(1000) ? 1 : 0, 10);
            float partialTicks = mc.getRenderPartialTicks();
            Vector3d player = new Vector3d(
                    MathUtil.interpolate(mc.player.getPosX(), mc.player.lastTickPosX, partialTicks),
                    MathUtil.interpolate(mc.player.getPosY(), mc.player.lastTickPosY, partialTicks),
                    MathUtil.interpolate(mc.player.getPosZ(), mc.player.lastTickPosZ, partialTicks))
                    .add(0, mc.player.getEyeHeight(), 0);

            if (setPosition != null) {
                mc.getRenderManager().info.setDirection(
                        (float) MathUtil.interpolate((float) (yaw + shake), mc.player.getYaw(e.partialTicks), 1 - back),
                        (float) MathUtil.interpolate((float) (pitch + shake), mc.player.getPitch(e.partialTicks), 1 - back));
                mc.getRenderManager().info.setPosition(MathUtil.interpolate(setPosition, player, 1 - back));
            }
            mc.getRenderManager().info.moveForward(2f * (float) animate.getValue());
        }
    }

    @Subscribe
    public void onDisplay(EventDisplay e) {
        if (mc.player == null || mc.world == null || e.getType() != EventDisplay.Type.POST) {
            return;
        }
        animate.update();

        if (useAnimation && setPosition != null && position.size() > 1 && effectMode.is("Тайм-стоп")) {
            setPosition = MathUtil.fast(setPosition, position.get(current), 1);
            RenderUtility.drawWhite((float) animate.getValue());
        }

        if (effectMode.is("Прикольные")) {
            renderKillEffects(e.getMatrixStack(), mc.getRenderPartialTicks());
        }
    }

    private void renderKillEffects(MatrixStack matrixStack, float partialTicks) {
        synchronized (killEffectsLock) {
            renderKillEffectsLocked(matrixStack, partialTicks);
        }
    }

    private void renderKillEffectsLocked(MatrixStack matrixStack, float partialTicks) {
        int effectCount = killEffects.size();
        if (effectCount == 0) return;

        long currentTime = System.currentTimeMillis();
        boolean hasActiveEffect = false;
        for (int i = 0; i < effectCount; ++i) {
            KillEffect effect = killEffects.get(i);
            if (currentTime - effect.startTime <= EFFECT_LIFESPAN) {
                hasActiveEffect = true;
                break;
            }
        }

        if (!hasActiveEffect) return;

        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        int rgb = getThemeColor();
        float r = ((rgb >> 16) & 255) * INV_255;
        float g = ((rgb >> 8) & 255) * INV_255;
        float b = (rgb & 255) * INV_255;

        double camX = MathUtil.interpolate(mc.player.getPosX(), mc.player.lastTickPosX, partialTicks);
        double camY = MathUtil.interpolate(mc.player.getPosY(), mc.player.lastTickPosY, partialTicks);
        double camZ = MathUtil.interpolate(mc.player.getPosZ(), mc.player.lastTickPosZ, partialTicks);

        boolean lineWidthWide = false;
        for (int effectIndex = 0; effectIndex < effectCount; ++effectIndex) {
            KillEffect effect = killEffects.get(effectIndex);
            long age = currentTime - effect.startTime;
            if (age > EFFECT_LIFESPAN) continue;

            float progress = (float) age / EFFECT_LIFESPAN;
            float alpha = 1.0F;
            if (progress > 0.75F) {
                alpha = 1.0F - (progress - 0.75F) / 0.25F;
            }

            if (alpha <= 0.01F) continue;

            matrixStack.push();
            matrixStack.translate(
                    effect.pos.x - camX,
                    effect.pos.y - camY + 0.02,
                    effect.pos.z - camZ
            );

            if (progress < 0.2F) {
                float blastProg = progress / 0.2F;
                float invBlast = 1.0F - blastProg;
                float blastScale = (1.0F - invBlast * invBlast * invBlast) * 8.0F;
                float blastAlpha = (1.0F - blastProg) * alpha;

                matrixStack.push();
                matrixStack.scale(blastScale, 1.0F, blastScale);
                buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
                drawRing(matrixStack, buffer, 1.0F, 0.3F, r, g, b, blastAlpha * 0.8F);
                tessellator.draw();
                matrixStack.pop();

                matrixStack.push();
                matrixStack.translate(0.0, 1.0, 0.0);
                matrixStack.scale(blastScale * 0.6F, blastScale * 0.6F, blastScale * 0.6F);
                buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
                drawWireSphere(matrixStack, buffer, 1.0F, WIRE_SPHERE_STACKS, WIRE_SPHERE_SLICES, r, g, b, blastAlpha * 0.4F);
                tessellator.draw();
                matrixStack.pop();

                buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
                Matrix4f sparkMatrix = matrixStack.getLast().getMatrix();
                float spkSpeed = 6.0F * blastProg;
                for (int i = 0; i < SPARK_COUNT; ++i) {
                    float spkY = SPARK_SEED_ABS_SIN[i] * spkSpeed;
                    float spkRad = SPARK_SEED_COS[i] * spkSpeed;
                    float sx = SPARK_DIR_COS[i] * spkRad;
                    float sz = SPARK_DIR_SIN[i] * spkRad;
                    drawParticle(sparkMatrix, buffer, sx, spkY, sz, 0.1F, r, g, b, blastAlpha);
                }
                tessellator.draw();
            }

            matrixStack.push();
            float heartbeat = 1.0F + (float) Math.sin(age * 0.01F) * 0.05F;
            matrixStack.scale(heartbeat, 1.0F, heartbeat);
            float ritualBase = 1.0F - Math.min(1.0F, progress * 5.0F);
            float ritualPop = 1.0F - ritualBase * ritualBase * ritualBase;
            matrixStack.scale(ritualPop, 1.0F, ritualPop);

            buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
            drawThickStar(matrixStack, buffer, 1.8F, 0.1F, r, g, b, alpha);

            matrixStack.push();
            matrixStack.rotate(Vector3f.YP.rotationDegrees(age * 0.1F));
            drawRunesRing(matrixStack, buffer, 1.2F, 0.1F, r, g, b, alpha * 0.7F);
            matrixStack.pop();

            matrixStack.push();
            matrixStack.rotate(Vector3f.YP.rotationDegrees(-age * 0.05F));
            drawRunesRing(matrixStack, buffer, 2.5F, 0.15F, r, g, b, alpha * 0.5F);
            matrixStack.pop();
            tessellator.draw();
            matrixStack.pop();

            float soulY = 0.5F + progress * 4.5F;
            soulY = (float) (soulY + Math.sin(age * 0.005F) * 0.2F);

            matrixStack.push();
            matrixStack.translate(0.0F, soulY, 0.0F);
            float spin = age * 0.2F;
            matrixStack.rotate(Vector3f.YP.rotationDegrees(spin));
            float progressSq = progress * progress;
            float soulSize = 0.6F * (1.0F - progressSq * progressSq);
            matrixStack.scale(soulSize, soulSize, soulSize);

            matrixStack.push();
            matrixStack.rotate(Vector3f.XP.rotationDegrees(spin));
            matrixStack.rotate(Vector3f.ZP.rotationDegrees(spin));
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            draw3DCube(matrixStack, buffer, 0.3F, 1.0F, 1.0F, 1.0F, alpha);
            tessellator.draw();
            matrixStack.pop();

            matrixStack.push();
            matrixStack.rotate(Vector3f.XP.rotationDegrees(-spin * 0.5F));
            buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
            draw3DDiamond(matrixStack, buffer, 0.8F, r, g, b, alpha * 0.4F);
            tessellator.draw();
            matrixStack.pop();

            matrixStack.push();
            matrixStack.scale(1.5F, 1.5F, 1.5F);
            matrixStack.rotate(Vector3f.ZP.rotationDegrees(45.0F));
            matrixStack.rotate(Vector3f.YP.rotationDegrees(spin * 2.0F));
            buffer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
            drawRing(matrixStack, buffer, 1.0F, 0.05F, r, g, b, alpha * 0.6F);
            tessellator.draw();
            matrixStack.pop();
            matrixStack.pop();

            if (alpha > 0.1F) {
                if (!lineWidthWide) {
                    GL11.glLineWidth(2.0F);
                    lineWidthWide = true;
                }
                buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
                for (int i = 0; i < LIGHTNING_X.length; ++i) {
                    if (random.nextFloat() <= 0.6F) {
                        drawLightningArc(matrixStack, buffer, LIGHTNING_X[i], 0.1F, LIGHTNING_Z[i], 0.0F, soulY, 0.0F, r, g, b, alpha * 0.8F);
                    }
                }
                tessellator.draw();
            }

            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            Matrix4f particleMatrix = matrixStack.getLast().getMatrix();
            float particleBase = age * 0.01F;
            float particleBaseCos = (float) Math.cos(particleBase);
            float particleBaseSin = (float) Math.sin(particleBase);
            for (int i = 0; i < PARTICLE_COUNT; ++i) {
                float pAge = (age + i * 80L) % 1200L;
                float pProg = pAge / 1200.0F;
                float pY = pProg * 5.0F;
                float pRad = 2.0F * (1.0F - pProg * 0.8F);
                float pX = (particleBaseCos * PARTICLE_PHASE_COS[i] - particleBaseSin * PARTICLE_PHASE_SIN[i]) * pRad;
                float pZ = (particleBaseSin * PARTICLE_PHASE_COS[i] + particleBaseCos * PARTICLE_PHASE_SIN[i]) * pRad;
                drawParticle(particleMatrix, buffer, pX, pY, pZ, 0.06F, r, g, b, alpha * (1.0F - pProg));
            }
            tessellator.draw();

            matrixStack.pop();
        }

        GL11.glLineWidth(1.0F);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    private void drawRing(MatrixStack stack, BufferBuilder buffer, float radius, float width, float r, float g, float b, float a) {
        Matrix4f m = stack.getLast().getMatrix();
        float innerRadius = radius - width;
        for (int i = 0; i < RING_COS.length; ++i) {
            float c = RING_COS[i];
            float s = RING_SIN[i];
            buffer.pos(m, c * innerRadius, 0.0F, s * innerRadius).color(r, g, b, 0.0F).endVertex();
            buffer.pos(m, c * radius, 0.0F, s * radius).color(r, g, b, a).endVertex();
        }
    }

    private void drawThickStar(MatrixStack stack, BufferBuilder buffer, float radius, float width, float r, float g, float b, float a) {
        Matrix4f m = stack.getLast().getMatrix();
        for (int i = 0; i < 5; ++i) {
            float x1 = STAR_COS[i] * radius;
            float z1 = STAR_SIN[i] * radius;
            float x2 = STAR_COS[i + 1] * radius;
            float z2 = STAR_SIN[i + 1] * radius;
            float nx = STAR_NORMAL_X[i] * width;
            float nz = STAR_NORMAL_Z[i] * width;
            buffer.pos(m, x1 - nx, 0.0F, z1 - nz).color(r, g, b, 0.0F).endVertex();
            buffer.pos(m, x1 + nx, 0.0F, z1 + nz).color(r, g, b, a).endVertex();
            buffer.pos(m, x2 + nx, 0.0F, z2 + nz).color(r, g, b, a).endVertex();
            buffer.pos(m, x2 - nx, 0.0F, z2 - nz).color(r, g, b, 0.0F).endVertex();
        }
    }

    private void drawRunesRing(MatrixStack stack, BufferBuilder buffer, float radius, float size, float r, float g, float b, float a) {
        Matrix4f m = stack.getLast().getMatrix();
        for (int i = 0; i < RUNE_COUNT; ++i) {
            float x = RUNE_COS[i] * radius;
            float z = RUNE_SIN[i] * radius;
            buffer.pos(m, x - size, 0.0F, z - size).color(r, g, b, a).endVertex();
            buffer.pos(m, x + size, 0.0F, z - size).color(r, g, b, 0.0F).endVertex();
            buffer.pos(m, x + size, 0.0F, z + size).color(r, g, b, a).endVertex();
            buffer.pos(m, x - size, 0.0F, z + size).color(r, g, b, 0.0F).endVertex();
        }
    }

    private void drawLightningArc(MatrixStack stack, BufferBuilder buffer, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b, float a) {
        Matrix4f m = stack.getLast().getMatrix();
        float px = x1;
        float py = y1;
        float pz = z1;
        for (int i = 1; i <= LIGHTNING_ARC_SEGMENTS; ++i) {
            float progress = (float) i / LIGHTNING_ARC_SEGMENTS;
            float tx = x1 + (x2 - x1) * progress;
            float ty = y1 + (y2 - y1) * progress;
            float tz = z1 + (z2 - z1) * progress;
            if (i < LIGHTNING_ARC_SEGMENTS) {
                tx += (random.nextFloat() - 0.5F) * 0.4F;
                ty += (random.nextFloat() - 0.5F) * 0.4F;
                tz += (random.nextFloat() - 0.5F) * 0.4F;
            }
            buffer.pos(m, px, py, pz).color(r, g, b, a).endVertex();
            buffer.pos(m, tx, ty, tz).color(r, g, b, a).endVertex();
            px = tx;
            py = ty;
            pz = tz;
        }
    }

    private void draw3DDiamond(MatrixStack stack, BufferBuilder buffer, float size, float r, float g, float b, float a) {
        Matrix4f m = stack.getLast().getMatrix();
        float h = size * 1.6F;
        drawTri(m, buffer, 0.0F, h, 0.0F, -size, 0.0F, -size, size, 0.0F, -size, r, g, b, a);
        drawTri(m, buffer, 0.0F, h, 0.0F, size, 0.0F, -size, size, 0.0F, size, r, g, b, a);
        drawTri(m, buffer, 0.0F, h, 0.0F, size, 0.0F, size, -size, 0.0F, size, r, g, b, a);
        drawTri(m, buffer, 0.0F, h, 0.0F, -size, 0.0F, size, -size, 0.0F, -size, r, g, b, a);
        drawTri(m, buffer, 0.0F, -h, 0.0F, size, 0.0F, -size, -size, 0.0F, -size, r, g, b, a);
        drawTri(m, buffer, 0.0F, -h, 0.0F, size, 0.0F, size, size, 0.0F, -size, r, g, b, a);
        drawTri(m, buffer, 0.0F, -h, 0.0F, -size, 0.0F, size, size, 0.0F, size, r, g, b, a);
        drawTri(m, buffer, 0.0F, -h, 0.0F, -size, 0.0F, -size, -size, 0.0F, size, r, g, b, a);
    }

    private void draw3DCube(MatrixStack stack, BufferBuilder buffer, float size, float r, float g, float b, float a) {
        Matrix4f m = stack.getLast().getMatrix();
        buffer.pos(m, -size, size, -size).color(r, g, b, a).endVertex();
        buffer.pos(m, -size, size, size).color(r, g, b, a).endVertex();
        buffer.pos(m, size, size, size).color(r, g, b, a).endVertex();
        buffer.pos(m, size, size, -size).color(r, g, b, a).endVertex();
        buffer.pos(m, -size, -size, -size).color(r, g, b, a).endVertex();
        buffer.pos(m, size, -size, -size).color(r, g, b, a).endVertex();
        buffer.pos(m, size, -size, size).color(r, g, b, a).endVertex();
        buffer.pos(m, -size, -size, size).color(r, g, b, a).endVertex();
        buffer.pos(m, -size, -size, -size).color(r, g, b, a).endVertex();
        buffer.pos(m, -size, size, -size).color(r, g, b, a).endVertex();
        buffer.pos(m, size, size, -size).color(r, g, b, a).endVertex();
        buffer.pos(m, size, -size, -size).color(r, g, b, a).endVertex();
    }

    private void drawTri(Matrix4f m, BufferBuilder b, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float r, float g, float bl, float a) {
        b.pos(m, x1, y1, z1).color(r, g, bl, a).endVertex();
        b.pos(m, x2, y2, z2).color(r, g, bl, a).endVertex();
        b.pos(m, x3, y3, z3).color(r, g, bl, a).endVertex();
    }

    private void drawWireSphere(MatrixStack stack, BufferBuilder buffer, float radius, int stacks, int slices, float r, float g, float b, float a) {
        Matrix4f m = stack.getLast().getMatrix();
        if (stacks == WIRE_SPHERE_STACKS && slices == WIRE_SPHERE_SLICES) {
            for (int i = 0; i < WIRE_SPHERE_VERTICES.length; i += 3) {
                buffer.pos(m, WIRE_SPHERE_VERTICES[i] * radius, WIRE_SPHERE_VERTICES[i + 1] * radius, WIRE_SPHERE_VERTICES[i + 2] * radius).color(r, g, b, a).endVertex();
            }
            return;
        }

        for (int i = 0; i < stacks; ++i) {
            double lat0 = Math.PI * (-0.5 + (double) i / stacks);
            double z0 = Math.sin(lat0);
            double zr0 = Math.cos(lat0);
            double lat1 = Math.PI * (-0.5 + (double) (i + 1) / stacks);
            double z1 = Math.sin(lat1);
            double zr1 = Math.cos(lat1);
            for (int j = 0; j < slices; ++j) {
                double lng0 = Math.PI * 2.0 * j / slices;
                double x0 = Math.cos(lng0);
                double y0 = Math.sin(lng0);
                buffer.pos(m, (float) (x0 * zr0 * radius), (float) (y0 * zr0 * radius), (float) (z0 * radius)).color(r, g, b, a).endVertex();
                buffer.pos(m, (float) (x0 * zr1 * radius), (float) (y0 * zr1 * radius), (float) (z1 * radius)).color(r, g, b, a).endVertex();
            }
        }
    }

    private void drawParticle(MatrixStack stack, BufferBuilder buffer, float x, float y, float z, float size, float r, float g, float b, float a) {
        drawParticle(stack.getLast().getMatrix(), buffer, x, y, z, size, r, g, b, a);
    }

    private void drawParticle(Matrix4f m, BufferBuilder buffer, float x, float y, float z, float size, float r, float g, float b, float a) {
        buffer.pos(m, x, y + size, z).color(r, g, b, a).endVertex();
        buffer.pos(m, x + size, y, z).color(r, g, b, a).endVertex();
        buffer.pos(m, x, y - size, z).color(r, g, b, a).endVertex();
        buffer.pos(m, x - size, y, z).color(r, g, b, a).endVertex();
    }

    private int getThemeColor() {
        float hue = (System.currentTimeMillis() % 5000) / 5000.0F;
        return Color.HSBtoRGB(hue, 0.8F, 1.0F);
    }

    public void onKill(LivingEntity entity) {
        long now = System.currentTimeMillis();
        if (now - lastSoundTime < SOUND_COOLDOWN) {
            return;
        }
        lastSoundTime = now;

        if (effectMode.is("Тайм-стоп")) {
            position.clear();
            current = 0;
            animate = animate.animate(1, 1f, Easings.CIRC_OUT);
            useAnimation = true;
            stopWatch.reset();
            float partialTicks = mc.getRenderPartialTicks();
            Vector3d player = new Vector3d(
                    MathUtil.interpolate(mc.player.getPosX(), mc.player.lastTickPosX, partialTicks),
                    MathUtil.interpolate(mc.player.getPosY(), mc.player.lastTickPosY, partialTicks),
                    MathUtil.interpolate(mc.player.getPosZ(), mc.player.lastTickPosZ, partialTicks))
                    .add(0, mc.player.getEyeHeight(), 0);

            setPosition = player;
            yaw = mc.player.getYaw(partialTicks);
            pitch = mc.player.getPitch(partialTicks);

            stopCurrentSounds();
            playSound("harmony/sounds/fragsfx.wav", volume.get() / 100.0f, true);
            createSound();
        }

        if (effectMode.is("Девочка")) {
            stopCurrentSounds();
            String[] girlSoundFiles = {"girl_1.wav", "girl_2.wav", "girl_3.wav", "girl_4.wav", "girl_5.wav"};
            int randomIndex = MathUtil.randomInt(0, girlSoundFiles.length - 1);
            playSound("harmony/sounds/" + girlSoundFiles[randomIndex], volume.get() / 100.0f, true);
        }

        if (effectMode.is("Прикольные")) {
            Vector3d entityPos = entity.getPositionVec();
            synchronized (killEffectsLock) {
                killEffects.add(new KillEffect(entityPos, System.currentTimeMillis()));
            }
        }
    }

    private void stopCurrentSounds() {
        if (currentClip != null) {
            if (currentClip.isRunning()) {
                currentClip.stop();
            }
            currentClip.close();
            currentClip = null;
        }
        if (currentSecondClip != null) {
            if (currentSecondClip.isRunning()) {
                currentSecondClip.stop();
            }
            currentSecondClip.close();
            currentSecondClip = null;
        }
    }

    private void playSound(String resourcePath, float volume, boolean isPrimary) {
        try {
            Clip clip = AudioSystem.getClip();
            InputStream is = mc.getResourceManager().getResource(new ResourceLocation(resourcePath)).getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(bis);
            if (audioInputStream == null) {
                return;
            }
            clip.open(audioInputStream);
            FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float gain = (float) (20.0f * Math.log10(volume));
            gainControl.setValue(Math.max(-80.0f, Math.min(gain, 6.0f)));
            clip.start();
            if (isPrimary) {
                currentClip = clip;
            } else {
                currentSecondClip = clip;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void createSound() {
        String[] soundFiles = {"strikesf-1.wav", "strikesf-2.wav", "strikesf-3.wav", "strikesf-4.wav"};
        int randomIndex = MathUtil.randomInt(0, soundFiles.length - 1);
        playSound("harmony/sounds/" + soundFiles[randomIndex], volume.get() / 100.0f, false);
    }

    private static class KillEffect {
        public final Vector3d pos;
        public final long startTime;

        public KillEffect(Vector3d pos, long startTime) {
            this.pos = pos;
            this.startTime = startTime;
        }
    }

}
