package xd.harm.modules.impl.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.settings.PointOfView;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.server.SEntityStatusPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import org.lwjgl.opengl.GL11;
import xd.harm.events.network.EventPacket;
import xd.harm.events.render.EventRender3D;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.CategorySetting;
import xd.harm.modules.settings.impl.ColorSetting;
import xd.harm.modules.settings.impl.ModeListSetting;
import xd.harm.modules.settings.impl.SliderSetting;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ModuleRegister(
        name = "WavePlayer",
        desc = "Сохраняет позу игрока и рисует призрак",
        category = Category.Render
)
public class WavePlayer extends Module {

    private final CategorySetting visualCategory = new CategorySetting("Визуал");
    private final SliderSetting riseHeight = new SliderSetting("Высота подъема", 4.0f, 0.2f, 5.0f, 0.1f);
    private final SliderSetting duration = new SliderSetting("Время жизни", 3.0f, 0.2f, 6.0f, 0.1f);
    private final BooleanSetting useThemeColor = new BooleanSetting("Брать цвет темы", false);
    private final ColorSetting color = new ColorSetting("Цвет", new Color(255, 255, 255).getRGB()).setVisible(() -> !useThemeColor.get());

    private final CategorySetting triggerCategory = new CategorySetting("Триггеры");
    private final BooleanSetting onTotem = new BooleanSetting("При тотеме", true);
    private final ModeListSetting totemView = new ModeListSetting("Тотем вид",
            new BooleanSetting("1 лицо", false),
            new BooleanSetting("3 лицо", true)
    ).setVisible(() -> onTotem.get());
    private final BooleanSetting onDeath = new BooleanSetting("При смерти", true);
    private final ModeListSetting deathView = new ModeListSetting("Смерть вид",
            new BooleanSetting("1 лицо", false),
            new BooleanSetting("3 лицо", true)
    ).setVisible(() -> onDeath.get());
    private final BooleanSetting onJump = new BooleanSetting("При прыжке", false);
    private final ModeListSetting jumpView = new ModeListSetting("Прыжок вид",
            new BooleanSetting("1 лицо", false),
            new BooleanSetting("3 лицо", true)
    ).setVisible(() -> onJump.get());
    private final BooleanSetting onWalk = new BooleanSetting("При ходьбе", false);
    private final ModeListSetting walkView = new ModeListSetting("Ходьба вид",
            new BooleanSetting("1 лицо", false),
            new BooleanSetting("3 лицо", true)
    ).setVisible(() -> onWalk.get());
    private final SliderSetting walkInterval = new SliderSetting("Интервал ходьбы", 0.6f, 0.1f, 3.0f, 0.1f)
            .setVisible(() -> onWalk.get());

    private final CategorySetting targetsCategory = new CategorySetting("Цели");
    private final BooleanSetting selfTarget = new BooleanSetting("У меня", true);
    private final BooleanSetting playersTarget = new BooleanSetting("У игроков", true);
    private final ModeListSetting targets = new ModeListSetting("Для кого",
            selfTarget,
            playersTarget
    );
    private final SliderSetting playersRadius = new SliderSetting("Радиус игроков", 8.0f, 1.0f, 64.0f, 1.0f)
            .setVisible(this::isOthersEnabled);

    private final List<GhostSnapshot> ghosts = new ArrayList<>();
    private final Map<UUID, TrackingData> tracking = new HashMap<>();

    private boolean wasOnGround;
    private long walkTimer;
    private long trackingPass;

    private static final float PX = 1.0f / 16.0f;

    public WavePlayer() {
        addSettings(
                visualCategory, riseHeight, duration, useThemeColor, color,
                triggerCategory, onTotem, totemView, onDeath, deathView, onJump, jumpView, onWalk, walkView, walkInterval,
                targetsCategory, targets, playersRadius
        );
    }

    @Override
    public boolean onDisable() {
        synchronized (ghosts) {
            ghosts.clear();
        }
        tracking.clear();
        wasOnGround = false;
        walkTimer = 0L;
        trackingPass = 0L;
        return super.onDisable();
    }

    @Subscribe
    public void onPacket(EventPacket event) {
        if (mc.world == null || !event.isReceivePacket()) return;
        if (!(event.getPacket() instanceof SEntityStatusPacket packet)) return;

        byte code = packet.getOpCode();
        if (code == 35 && !onTotem.get()) return;
        if (code == 3 && !onDeath.get()) return;
        if (code != 35 && code != 3) return;

        Entity raw = packet.getEntity(mc.world);
        if (!(raw instanceof AbstractClientPlayerEntity player)) return;
        if (!canTrack(player)) return;

        spawnGhost(player, true, code == 35 ? GhostCause.TOTEM : GhostCause.DEATH);
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;

        long now = System.currentTimeMillis();
        boolean selfEnabled = isSelfEnabled();
        boolean othersEnabled = isOthersEnabled();
        boolean jumpEnabled = onJump.get();
        boolean walkEnabled = onWalk.get();
        long cooldown = walkEnabled ? Math.max((long) (walkInterval.get() * 1000.0f), 1L) : 1L;

        if (selfEnabled) {
            handleSelf(now, cooldown, jumpEnabled, walkEnabled);
        }

        if (othersEnabled) {
            handleOthers(now, cooldown, jumpEnabled, walkEnabled);
        } else {
            tracking.clear();
        }
    }

    @Subscribe
    public void onRender(EventRender3D event) {
        if (mc.world == null) return;

        synchronized (ghosts) {
            if (ghosts.isEmpty()) return;

            long now = System.currentTimeMillis();
            float lifetimeMs = duration.get() * 1000.0f;
            float riseHeightValue = riseHeight.get();
            boolean themeColor = useThemeColor.get();
            int fixedColor = color.get();
            int fixedR = (fixedColor >> 16) & 0xFF;
            int fixedG = (fixedColor >> 8) & 0xFF;
            int fixedB = fixedColor & 0xFF;
            boolean firstPersonView = mc.gameSettings.getPointOfView() == PointOfView.FIRST_PERSON;
            Vector3d cam = mc.gameRenderer.getActiveRenderInfo().getProjectedView();
            MatrixStack ms = event.getMatrixStack();

            RenderSystem.pushMatrix();
            RenderSystem.disableTexture();
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            RenderSystem.disableCull();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            mc.gameRenderer.getLightTexture().disableLightmap();

            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buf = tess.getBuffer();
            boolean batchStarted = false;

            for (int i = 0; i < ghosts.size(); ) {
                GhostSnapshot ghost = ghosts.get(i);
                float t = (now - ghost.spawnTime) / lifetimeMs;
                if (t >= 1.0f) {
                    ghosts.remove(i);
                    continue;
                }
                i++;

                if (ghost.sneaking) continue;
                if (!canRenderInCurrentView(ghost, firstPersonView)) continue;

                double yOff = ghost.rising ? riseHeightValue * rise(t) : 0.0;
                float alpha = MathHelper.clamp((float) fade(t), 0.0f, 0.75f);

                int cr = fixedR;
                int cg = fixedG;
                int cb = fixedB;
                if (themeColor) {
                    int col = Theme.MainColor((int) (t * 360.0f));
                    cr = (col >> 16) & 0xFF;
                    cg = (col >> 8) & 0xFF;
                    cb = col & 0xFF;
                }
                int ca = MathHelper.clamp((int) (alpha * 255.0f), 0, 255);

                if (!batchStarted) {
                    buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
                    batchStarted = true;
                }

                ms.push();
                ms.translate(ghost.pos.x - cam.x, ghost.pos.y - cam.y + yOff, ghost.pos.z - cam.z);
                ms.rotate(Vector3f.YP.rotationDegrees(180.0f - ghost.bodyYaw));

                drawBody(ms, buf, cr, cg, cb, ca, ghost);

                ms.pop();
            }

            if (batchStarted) {
                tess.draw();
            }

            mc.gameRenderer.getLightTexture().enableLightmap();
            RenderSystem.enableTexture();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.popMatrix();
        }
    }

    private void handleSelf(long now, long cooldown, boolean jumpEnabled, boolean walkEnabled) {
        boolean grounded = mc.player.isOnGround();

        if (jumpEnabled && wasOnGround && !grounded && mc.player.getMotion().y > 0.0) {
            spawnGhost(mc.player, true, GhostCause.JUMP);
        }

        if (walkEnabled) {
            if (jumpEnabled && !grounded) {
                walkTimer = 0L;
            } else {
                Vector3d vel = mc.player.getMotion();
                double speed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
                if (speed > 0.02) {
                    if (walkTimer == 0L) walkTimer = now;
                    if (now - walkTimer >= cooldown) {
                        walkTimer = now;
                        spawnGhost(mc.player, false, GhostCause.WALK);
                    }
                } else {
                    walkTimer = 0L;
                }
            }
        }

        wasOnGround = grounded;
    }

    private void handleOthers(long now, long cooldown, boolean jumpEnabled, boolean walkEnabled) {
        float radius = playersRadius.get();
        double radiusSq = radius * radius;
        long pass = ++trackingPass;

        for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player || player.removed) continue;
            if (mc.player.getDistanceSq(player) > radiusSq) continue;

            UUID id = player.getUniqueID();
            TrackingData data = tracking.get(id);
            if (data == null) {
                data = new TrackingData();
                tracking.put(id, data);
            }
            data.seenPass = pass;
            boolean grounded = player.isOnGround();
            double x = player.getPosX();
            double z = player.getPosZ();

            if (jumpEnabled && data.wasOnGround && !grounded && player.getMotion().y > 0.0) {
                spawnGhost(player, true, GhostCause.JUMP);
            }

            if (walkEnabled) {
                if (jumpEnabled && !grounded) {
                    data.walkTimer = 0L;
                } else {
                    double dx = x - data.prevX;
                    double dz = z - data.prevZ;
                    if (Math.sqrt(dx * dx + dz * dz) > 0.003) {
                        if (data.walkTimer == 0L) data.walkTimer = now;
                        if (now - data.walkTimer >= cooldown) {
                            data.walkTimer = now;
                            spawnGhost(player, false, GhostCause.WALK);
                        }
                    } else {
                        data.walkTimer = 0L;
                    }
                }
            }

            data.wasOnGround = grounded;
            data.prevX = x;
            data.prevZ = z;
        }

        Iterator<Map.Entry<UUID, TrackingData>> iterator = tracking.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().seenPass != pass) {
                iterator.remove();
            }
        }
    }

    private void spawnGhost(AbstractClientPlayerEntity player, boolean rise, GhostCause cause) {
        if (player.isCrouching()) return;

        float limbAmt = player.limbSwingAmount;
        float limbPhase = player.limbSwing;

        float rArmP = 0, rArmY = 0, lArmP = 0, lArmY = 0;
        float rLegP = 0, lLegP = 0;

        if (limbAmt > 0.01f) {
            float cycle = MathHelper.cos(limbPhase * 0.6662f) * 1.4f * limbAmt;
            rArmP = -cycle;
            lArmP = cycle;
            rLegP = cycle;
            lLegP = -cycle;
        }

        if (player.isSwingInProgress && player.swingProgress > 0.0f) {
            float sw = MathHelper.sin(MathHelper.sqrt(player.swingProgress) * (float) Math.PI * 2.0f) * 0.5f;
            rArmP -= sw * 1.2f;
            rArmY = -sw * 0.5f;
        }

        ModeListSetting viewSetting = viewSettingFor(cause);
        float renderScale = WorldTweaks.getSmallPlayerRenderScale(player);

        GhostSnapshot snapshot = new GhostSnapshot(
                player.getPositionVec(), player.renderYawOffset, player.isCrouching(),
                player == mc.player, rise, System.currentTimeMillis(),
                player.rotationYawHead, player.rotationPitch,
                rArmP, rArmY, lArmP, lArmY, rLegP, lLegP,
                player.isSwimming(), player.isElytraFlying(),
                viewSetting.get(0).get(), viewSetting.get(1).get(),
                renderScale, 0.0f
        );

        synchronized (ghosts) {
            ghosts.add(snapshot);
        }
    }

    private void drawBody(MatrixStack ms, BufferBuilder buf, int r, int g, int b, int a, GhostSnapshot ghost) {
        ms.push();
        if (ghost.renderScale != 1.0f || ghost.renderYOffset != 0.0f) {
            ms.translate(0.0, ghost.renderYOffset, 0.0);
            ms.scale(ghost.renderScale, ghost.renderScale, ghost.renderScale);
        }

        ms.scale(-1.0f, -1.0f, 1.0f);
        ms.translate(0.0, -1.501, 0.0);

        if (ghost.swimming || ghost.elytra) {
            ms.translate(0.0, 0.9, -0.5);
            ms.rotate(Vector3f.XP.rotationDegrees(90.0f));
        }

        float crouchYOffset = 0.0f;
        float crouchLegShrink = 0.0f;

        box(buf, ms.getLast().getMatrix(), -4 * PX, crouchYOffset, -2 * PX, 8 * PX, 12 * PX, 4 * PX, r, g, b, a);

        drawHead(ms, buf, r, g, b, a, ghost, crouchYOffset);
        drawArm(ms, buf, r, g, b, a, true, ghost.lArmP, ghost.lArmY, ghost, crouchYOffset);
        drawArm(ms, buf, r, g, b, a, false, ghost.rArmP, ghost.rArmY, ghost, crouchYOffset);
        drawLeg(ms, buf, r, g, b, a, true, ghost.lLegP, crouchYOffset, crouchLegShrink);
        drawLeg(ms, buf, r, g, b, a, false, ghost.rLegP, crouchYOffset, crouchLegShrink);

        ms.pop();
    }

    private void drawHead(MatrixStack ms, BufferBuilder buf, int r, int g, int b, int a, GhostSnapshot ghost, float yOffset) {
        ms.push();
        float yaw = MathHelper.clamp(MathHelper.wrapDegrees(ghost.headYaw - ghost.bodyYaw), -50.0f, 50.0f);
        float pitch = MathHelper.clamp(ghost.headPitch, -90.0f, 90.0f);
        ms.rotate(Vector3f.YP.rotationDegrees(yaw));
        ms.rotate(Vector3f.XP.rotationDegrees(pitch));
        box(buf, ms.getLast().getMatrix(), -4 * PX, -8 * PX + yOffset, -4 * PX, 8 * PX, 8 * PX, 8 * PX, r, g, b, a);
        ms.pop();
    }

    private void drawArm(MatrixStack ms, BufferBuilder buf, int r, int g, int b, int a,
                         boolean left, float pitch, float yaw, GhostSnapshot ghost, float yOffset) {
        ms.push();
        float px = (left ? -6.0f : 6.0f) * PX;
        float py = 2.0f * PX + yOffset;
        ms.translate(px, py, 0.0f);

        if (ghost.swimming || ghost.elytra) {
            ms.rotate(Vector3f.XP.rotationDegrees(-10.0f));
            ms.rotate(Vector3f.ZP.rotationDegrees(left ? -5.0f : 5.0f));
        } else {
            ms.rotate(Vector3f.YP.rotation(yaw));
            ms.rotate(Vector3f.XP.rotation(pitch));
        }

        ms.translate(-px, -py, 0.0f);
        float bx = (left ? -8.0f : 4.0f) * PX;
        box(buf, ms.getLast().getMatrix(), bx, -2 * PX + yOffset, -2 * PX, 4 * PX, 12 * PX, 4 * PX, r, g, b, a);
        ms.pop();
    }

    private void drawLeg(MatrixStack ms, BufferBuilder buf, int r, int g, int b, int a,
                         boolean left, float pitch, float yOffset, float shrink) {
        ms.push();
        float px = (left ? -2.0f : 2.0f) * PX;
        float py = 12.0f * PX + yOffset;
        ms.translate(px, py, 0.0f);
        ms.rotate(Vector3f.XP.rotation(pitch + (shrink > 0.0f ? 0.22f : 0.0f)));
        ms.translate(-px, -py, 0.0f);
        float bx = (left ? -4.0f : 0.0f) * PX;
        box(buf, ms.getLast().getMatrix(), bx, 12 * PX + yOffset, -2 * PX, 4 * PX, 12 * PX - shrink, 4 * PX, r, g, b, a);
        ms.pop();
    }

    private static void box(BufferBuilder buf, Matrix4f m, float x, float y, float z,
                            float w, float h, float d, int r, int g, int b, int a) {
        float ex = x + w, ey = y + h, ez = z + d;

        quad(buf, m, x, y, ez, ex, y, ez, ex, ey, ez, x, ey, ez, r, g, b, a);
        quad(buf, m, ex, y, z, x, y, z, x, ey, z, ex, ey, z, r, g, b, a);
        quad(buf, m, x, y, z, x, y, ez, x, ey, ez, x, ey, z, r, g, b, a);
        quad(buf, m, ex, y, ez, ex, y, z, ex, ey, z, ex, ey, ez, r, g, b, a);
        quad(buf, m, x, ey, ez, ex, ey, ez, ex, ey, z, x, ey, z, r, g, b, a);
        quad(buf, m, x, y, z, ex, y, z, ex, y, ez, x, y, ez, r, g, b, a);
    }

    private static void quad(BufferBuilder buf, Matrix4f m,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float x3, float y3, float z3, float x4, float y4, float z4,
                             int r, int g, int b, int a) {
        buf.pos(m, x1, y1, z1).color(r, g, b, a).endVertex();
        buf.pos(m, x2, y2, z2).color(r, g, b, a).endVertex();
        buf.pos(m, x3, y3, z3).color(r, g, b, a).endVertex();
        buf.pos(m, x4, y4, z4).color(r, g, b, a).endVertex();
    }

    private double rise(double t) {
        t = MathHelper.clamp(t, 0.0, 0.75);
        double inv = 1.0 - t;
        return 1.0 - inv * inv * inv;
    }

    private double fade(double t) {
        t = MathHelper.clamp(t, 0.0, 1.0);
        double inv = 1.0 - t;
        return 0.75 * inv * inv * inv;
    }

    private boolean canTrack(AbstractClientPlayerEntity player) {
        if (mc.player == null) return false;
        return player == mc.player ? isSelfEnabled() : isOthersEnabled();
    }

    private boolean canRenderInCurrentView(GhostSnapshot ghost, boolean firstPersonView) {
        return firstPersonView ? ghost.renderFirstPerson : ghost.renderThirdPerson;
    }

    private ModeListSetting viewSettingFor(GhostCause cause) {
        switch (cause) {
            case TOTEM:
                return totemView;
            case DEATH:
                return deathView;
            case JUMP:
                return jumpView;
            case WALK:
            default:
                return walkView;
        }
    }

    private boolean isSelfEnabled() {
        return selfTarget.get();
    }

    private boolean isOthersEnabled() {
        return playersTarget.get();
    }

    private enum GhostCause {
        TOTEM,
        DEATH,
        JUMP,
        WALK
    }

    private static class TrackingData {
        boolean wasOnGround;
        long walkTimer;
        long seenPass;
        double prevX, prevZ;
    }

    private static class GhostSnapshot {
        final Vector3d pos;
        final float bodyYaw;
        final boolean sneaking;
        final boolean self;
        final boolean rising;
        final long spawnTime;
        final float headYaw, headPitch;
        final float rArmP, rArmY, lArmP, lArmY;
        final float rLegP, lLegP;
        final boolean swimming, elytra;
        final boolean renderFirstPerson, renderThirdPerson;
        final float renderScale, renderYOffset;

        GhostSnapshot(Vector3d pos, float bodyYaw, boolean sneaking,
                      boolean self, boolean rising, long spawnTime,
                      float headYaw, float headPitch,
                      float rArmP, float rArmY, float lArmP, float lArmY,
                      float rLegP, float lLegP,
                      boolean swimming, boolean elytra,
                      boolean renderFirstPerson, boolean renderThirdPerson,
                      float renderScale, float renderYOffset) {
            this.pos = pos;
            this.bodyYaw = bodyYaw;
            this.sneaking = sneaking;
            this.self = self;
            this.rising = rising;
            this.spawnTime = spawnTime;
            this.headYaw = headYaw;
            this.headPitch = headPitch;
            this.rArmP = rArmP;
            this.rArmY = rArmY;
            this.lArmP = lArmP;
            this.lArmY = lArmY;
            this.rLegP = rLegP;
            this.lLegP = lLegP;
            this.swimming = swimming;
            this.elytra = elytra;
            this.renderFirstPerson = renderFirstPerson;
            this.renderThirdPerson = renderThirdPerson;
            this.renderScale = renderScale;
            this.renderYOffset = renderYOffset;
        }
    }
}
