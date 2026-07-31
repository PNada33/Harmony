package xd.harm.ui.display.impl;

import xd.harm.Harmony;
import xd.harm.events.render.EventDisplay;
import xd.harm.events.network.EventPacket;
import xd.harm.modules.impl.render.HUD;
import xd.harm.modules.impl.render.Theme;
import xd.harm.ui.display.ElementRenderer;
import xd.harm.utils.animations.Animation;
import xd.harm.utils.animations.Direction;
import xd.harm.utils.animations.impl.EaseInOutQuad;
import xd.harm.utils.drag.Dragging;
import xd.harm.utils.math.Vector4i;
import xd.harm.utils.projections.ProjectionUtility;
import xd.harm.utils.render.BlurUtils;
import xd.harm.utils.render.color.ColorUtils;
import xd.harm.utils.render.font.Fonts;
import xd.harm.utils.render.gl.Stencil;
import xd.harm.utils.render.rect.RenderUtility;
import xd.harm.utils.text.font.ClientFonts;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AirItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.UseAction;
import net.minecraft.network.play.server.SEntityStatusPacket;
import net.minecraft.scoreboard.Score;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector4f;
import org.joml.Vector2d;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

public class TargetInfoRenderer implements ElementRenderer {
    private final Minecraft mc = Minecraft.getInstance();
    private final Dragging drag;
    private final LegacyTargetHudRenderer legacyRenderer = new LegacyTargetHudRenderer();

    private LivingEntity entity = null;

    private final Animation fadeAnim = new EaseInOutQuad(300, 1);
    private final Animation itemsFadeAnim = new EaseInOutQuad(300, 1);

    private ResourceLocation lastSkin = null;
    private String lastName = "";
    private boolean lastWasPlayer = false;
    private LivingEntity heldTarget = null;
    private long lastTargetSeenMs = 0L;
    private float lastHurtPercent = 0.0f;
    private final List<ItemStack> lastItems = new ArrayList<>();
    private final List<ItemStack> renderItems = new ArrayList<>();
    private final List<ItemStack> reversedRenderItems = new ArrayList<>();
    private final List<Long> expiredRipples = new ArrayList<>();
    private final ItemStack totemStack = new ItemStack(Items.TOTEM_OF_UNDYING);

    private float animHpVal = 0f;
    private float hpNorm = 0f;
    private float hpNorm2 = 0f;
    private float absNorm = 0f;

    private float ax = 0f, ay = 0f;
    private float posX, posY;
    private final float dragSpeed = 0.2f;

    private final List<DamageParticle2D> damageParticles = new ArrayList<>();

    private static final float ROUND_PILL_MIN_W = 118f;
    private static final float ROUND_PILL_H = 34f;
    private static final float ROUND_PILL_R = 9f;
    private static final float ROUND_PAD_X = 8.5f;
    private static final float ROUND_AVATAR = 25f;
    private static final float ROUND_GAP_HEAD_TEXT = 5.5f;

    private static final float JUICY_WIDTH = 92F;
    private static final float JUICY_HEIGHT = 31F;
    private static final float JUICY_RADIUS = 7F;
    private static final float JUICY_HEAD_SIZE = 24F;

    private static final float ITEM_SCALE = 0.70f;
    private static final float ITEM_GAP = 11.5f;
    private static final int MAX_ITEMS = 6;
    private static final long TARGET_LOST_HOLD_MS = 550L;
    private static final float QUESTION_MARK_FONT_SIZE = 11f;
    private static final float QUESTION_MARK_CENTER_Y = -5.6f;

    private static final long RIPPLE_DUR = 500L;
    private static final int MAX_RIPPLES = 2;
    private final Deque<Long> rippleQueue = new ArrayDeque<>();
    private int prevHurtTime = 0;
    private float prevHealthSum = -1f;
    private boolean lastDamageActive = false;

    private static final long TOTEM_FX_DUR = 700L;
    private long totemFxStartMs = -1L;
    private long lastTotemAutoMs = 0L;
    private boolean isTotemAnimating = false;

    private float useUiAnim = 0f;
    private ItemStack lastUseStack = ItemStack.EMPTY;
    private float lastUseProg = 0f;
    private long useUiStartMs = 0L;

    private float headRingRotation = 0f;
    private long lastRenderTime = System.currentTimeMillis();

    private int lastEntityId = -1;
    private boolean blurEnabled = false;
    private boolean flowHudRects = false;

    private static final ResourceLocation FIREFLY_TEXTURE = new ResourceLocation("harmony/images/particles/bloom.png");
    private static final int DAMAGE_PARTICLES_COUNT = 8;

    public TargetInfoRenderer(Dragging drag) {
        this.drag = drag;
    }

    private static class DamageParticle2D {
        float x, y;
        float vx, vy;
        float lifetime;
        float maxLifetime;
        float size;
        float rotation;
        float rotationSpeed;
        int color;
        float startX, startY;

        DamageParticle2D(float x, float y, float angle, float speed, float lifetime, int color) {
            this.x = x;
            this.y = y;
            this.startX = x;
            this.startY = y;
            this.vx = (float) Math.cos(Math.toRadians(angle)) * speed;
            this.vy = (float) Math.sin(Math.toRadians(angle)) * speed;
            this.lifetime = lifetime;
            this.maxLifetime = lifetime;
            this.size = 8f + (float)(Math.random() * 4f);
            this.rotation = (float)(Math.random() * 360);
            this.rotationSpeed = (float)(Math.random() * 4f - 2f);
            this.color = color;
        }

        void update() {
            x += vx;
            y += vy;
            vx *= 0.96f;
            vy *= 0.96f;
            vy += 0.02f;
            rotation += rotationSpeed;
            lifetime -= 16.67f;
        }

        float getAlpha() {
            float distance = (float) Math.sqrt((x - startX) * (x - startX) + (y - startY) * (y - startY));
            float maxDistance = 60f;
            float distanceAlpha = 1f - Math.min(1f, distance / maxDistance);

            float progress = 1f - (lifetime / maxLifetime);
            float timeAlpha;
            if (progress < 0.1f) {
                timeAlpha = progress / 0.1f;
            } else if (progress > 0.6f) {
                timeAlpha = 1f - (progress - 0.6f) / 0.4f;
            } else {
                timeAlpha = 1f;
            }

            return distanceAlpha * timeAlpha;
        }
    }

    public void notifyDamage(LivingEntity who) {
        if (entity != null && who != null && entity.getEntityId() == who.getEntityId()) addRipple();
    }

    public void notifyTotemPop(LivingEntity who) {
        if (entity != null && who != null && entity.getEntityId() == who.getEntityId())
            startTotemAnimation(System.currentTimeMillis());
    }

    public void onPacket(EventPacket e) {
        if (mc.world == null) return;
        HUD hud = Harmony.getInstance().getModuleManager().getHud();
        if (hud != null && legacyRenderer.isLegacyMode(hud.targetHudMode.get())) {
            legacyRenderer.onPacket(e);
            return;
        }

        if (e.getPacket() instanceof SEntityStatusPacket) {
            SEntityStatusPacket packet = (SEntityStatusPacket) e.getPacket();

            if (packet.getOpCode() == 35) {
                Entity packetEntity = packet.getEntity(mc.world);

                if (packetEntity != null && packetEntity != mc.player && packetEntity instanceof LivingEntity) {
                    LivingEntity livingEntity = (LivingEntity) packetEntity;

                    if (entity != null && entity.getEntityId() == livingEntity.getEntityId()) {
                        startTotemAnimation(System.currentTimeMillis());
                    }
                }
            }
        }
    }

    private float smoothPosX = 0f;
    private float smoothPosY = 0f;
    private float smoothPosZ = 0f;
    private boolean smoothInitialized = false;
    private LivingEntity lastSmoothTarget = null;

    @Override
    public void render(EventDisplay e) {
        HUD hud = Harmony.getInstance().getModuleManager().getHud();
        flowHudRects = isFlowHudModeSafe(hud);
        if (!hud.elements.getValueByName("Активный таргет").get()) {
            fadeAnim.setDirection(Direction.BACKWARDS);
            itemsFadeAnim.setDirection(Direction.BACKWARDS);
            entity = null;
            clearHeldTarget();
            rippleQueue.clear();
            smoothInitialized = false;
            headRingRotation = 0f;

            if (itemsFadeAnim.finished(Direction.BACKWARDS)) {
                lastItems.clear();
            }
            return;
        }

        if (legacyRenderer.isLegacyMode(hud.targetHudMode.get())) {
            legacyRenderer.render(e, hud, drag);
            return;
        }

        String mode = hud.targetHudMode.get();
        boolean isCompactTargetHud = isCompactTargetHudMode(mode);

        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastRenderTime) / 16.67f;
        lastRenderTime = currentTime;

        headRingRotation += 2.0f * deltaTime;
        while (headRingRotation >= 360) {
            headRingRotation -= 360;
        }

        if (drag.isDragging()) {
            ax = drag.getX();
            ay = drag.getY();
        } else {
            ax += (drag.getX() - ax) * dragSpeed;
            ay += (drag.getY() - ay) * dragSpeed;
        }

        LivingEntity prev = entity;
        entity = resolveTarget(entity);
        boolean showTargetItems = shouldShowTargetItems(hud, entity);

        if (entity != null) {
            if (entity instanceof AbstractClientPlayerEntity) {
                lastSkin = ((AbstractClientPlayerEntity) entity).getLocationSkin();
                lastWasPlayer = true;
            } else {
                lastSkin = null;
                lastWasPlayer = false;
            }
            lastName = entity.getName().getString();
            lastHurtPercent = (entity.hurtTime - (entity.hurtTime != 0 ? mc.timer.renderPartialTicks : 0.0f)) / 10.0f;

            if (showTargetItems) {
                lastItems.clear();
                lastItems.add(entity.getHeldItemMainhand().copy());
                lastItems.add(entity.getHeldItemOffhand().copy());
                entity.getArmorInventoryList().forEach(s -> lastItems.add(s.copy()));
            }
        }

        boolean targetChanged = entity != null && (prev == null || entity.getEntityId() != lastEntityId);
        if (targetChanged) {
            lastEntityId = entity.getEntityId();
            prevHurtTime = 0;
            rippleQueue.clear();
            damageParticles.clear();
            prevHealthSum = -1f;
            lastDamageActive = false;
            smoothInitialized = false;
        }

        if (entity == null) {
            fadeAnim.setDirection(Direction.BACKWARDS);
            itemsFadeAnim.setDirection(Direction.BACKWARDS);

            if (itemsFadeAnim.finished(Direction.BACKWARDS)) {
                lastItems.clear();
            }
            if (fadeAnim.finished(Direction.BACKWARDS)) {
                smoothInitialized = false;
                clearHeldTarget();
                return;
            }
        } else {
            fadeAnim.setDirection(Direction.FORWARDS);
            itemsFadeAnim.setDirection(showTargetItems ? Direction.FORWARDS : Direction.BACKWARDS);
            if (!showTargetItems && itemsFadeAnim.finished(Direction.BACKWARDS)) {
                lastItems.clear();
            }
        }

        boolean projectedTargetHud = false;
        if (hud.d3.get() && entity != null && entity != mc.player) {
            float partialTicks = e.getPartialTicks();
            float targetX = (float)(entity.lastTickPosX + (entity.getPosX() - entity.lastTickPosX) * partialTicks);
            float targetY = (float)(entity.lastTickPosY + (entity.getPosY() - entity.lastTickPosY) * partialTicks) + entity.getHeight() / 2f;
            float targetZ = (float)(entity.lastTickPosZ + (entity.getPosZ() - entity.lastTickPosZ) * partialTicks);

            if (!smoothInitialized || lastSmoothTarget != entity) {
                smoothPosX = targetX;
                smoothPosY = targetY;
                smoothPosZ = targetZ;
                smoothInitialized = true;
                lastSmoothTarget = entity;
            }

            float smoothingFactor = 0.18f;
            smoothPosX = smoothPosX + (targetX - smoothPosX) * smoothingFactor;
            smoothPosY = smoothPosY + (targetY - smoothPosY) * smoothingFactor;
            smoothPosZ = smoothPosZ + (targetZ - smoothPosZ) * smoothingFactor;

            Vector2d v = ProjectionUtility.project2D(smoothPosX, smoothPosY, smoothPosZ);
            if (isProjectedTargetHudVisible(v)) {
                float pillH = getHudHeight(mode);
                posX = (float) v.x - 3;
                posY = (float) v.y - pillH - 3;
                projectedTargetHud = true;
            }
        }

        if (!projectedTargetHud) {
            posX = ax;
            posY = ay;
            if (!hud.d3.get() || entity == null || entity == mc.player) {
                smoothInitialized = false;
            }
        }

        posX += drag.getWobbleX();
        posY += drag.getWobbleY();

        if (entity != null && entity != mc.player && fixHealth(entity.getHealth()) <= 0f) {
            entity = null;
            clearHeldTarget();
            fadeAnim.setDirection(Direction.BACKWARDS);
            itemsFadeAnim.setDirection(Direction.BACKWARDS);
            smoothInitialized = false;
            return;
        }

        float curHp = entity != null ? fixHealth(entity.getHealth()) : animHpVal;
        float maxHp = entity != null ? entity.getMaxHealth() : 20f;
        float absorb = entity != null ? entity.getAbsorptionAmount() : 0f;
        if (mc.getCurrentServerData() != null && mc.getCurrentServerData().serverIP.contains("funtime") && entity != mc.player)
            absorb = 0f;

        if (entity != null) {
            float hpRatio = MathHelper.clamp(curHp / Math.max(1f, maxHp), 0f, 1f);
            float absRatio = MathHelper.clamp(absorb / Math.max(1f, maxHp), 0f, 1f);
            if (targetChanged) {
                hpNorm = hpRatio;
                hpNorm2 = hpRatio;
                absNorm = absRatio;
                animHpVal = curHp;
            } else {
                hpNorm = lerp(hpNorm, hpRatio, 6f);
                hpNorm2 = lerp(hpNorm2, hpNorm, 2.0f);
                absNorm = lerp(absNorm, absRatio, 6f);
                animHpVal = lerp(animHpVal, curHp, 6f);
            }
        }

        updateDamageParticles();

        if (isCompactTargetHud) {
            renderRoundStyle(e);
        } else {
            renderJuicyStyle(e, absorb);
        }

        restoreHudState();
    }

    private void restoreHudState() {
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        com.mojang.blaze3d.systems.RenderSystem.enableTexture();
        com.mojang.blaze3d.systems.RenderSystem.enableAlphaTest();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        com.mojang.blaze3d.systems.RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        org.lwjgl.opengl.GL11.glShadeModel(org.lwjgl.opengl.GL11.GL_FLAT);
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_LINE_SMOOTH);
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_POINT_SMOOTH);
        org.lwjgl.opengl.GL11.glLineWidth(1.0F);
        org.lwjgl.opengl.GL11.glPointSize(1.0F);
        org.lwjgl.opengl.GL20.glUseProgram(0);
        com.mojang.blaze3d.platform.GlStateManager.activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
    }


    private void renderRoundStyle(EventDisplay e) {
        MatrixStack ms = e.getMatrixStack();
        String name = entity != null ? entity.getName().getString() : lastName;
        String hpText = ((int) Math.ceil(animHpVal)) + "HP";

        float nameMaxWidth = 52f;
        float nameW = Math.min(ClientFonts.sf_medium[15].getWidth(name), nameMaxWidth);
        float hpW = ClientFonts.sf_medium[12].getWidth(hpText);
        float pillW = Math.max(ROUND_PILL_MIN_W, ROUND_PAD_X + ROUND_AVATAR + ROUND_GAP_HEAD_TEXT + Math.max(52f, nameW) + 6f + hpW + ROUND_PAD_X);
        float pillH = ROUND_PILL_H;
        drag.setWidth(pillW);
        drag.setHeight(pillH);

        float a = (float) fadeAnim.getOutput();
        float itemsAlpha = getItemsRenderAlpha(a);

        float grabScale = drag.getGrabScale();
        float grabRot = drag.getWobbleAngle();
        GL11.glPushMatrix();
        if (Math.abs(grabScale - 1.0f) > 0.001f) {
            RenderUtility.customScaledObject2D(posX, posY, pillW, pillH, grabScale);
        }
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glTranslatef(posX, posY, 0);
        if (Math.abs(grabRot) > 0.001f) {
            RenderUtility.customRotatedObject2D(0, 0, pillW, pillH, grabRot);
        }

        if (blurEnabled) {
            BlurUtils.renderRoundedBlur(3f, 0f, pillW - 3f, pillH, ROUND_PILL_R + 4f);
        }

        drawRoundPill(0, 0, pillW, pillH, ROUND_PILL_R, a);

        float headX = ROUND_PAD_X, headY = pillH / 2f - ROUND_AVATAR / 2f;
        float hurtPct = entity != null ? lastHurtPercent : 0f;
        if (lastWasPlayer && lastSkin != null) {
            Stencil.initStencilToWrite();
            RenderUtility.drawRoundedRect(headX, headY, ROUND_AVATAR, ROUND_AVATAR, 11,
                    ColorUtils.rgba(22, 22, 30, (int) (210 * a)));
            Stencil.readStencilBuffer(1);
            RenderUtility.drawHead(lastSkin, headX - 2, headY - 2, ROUND_AVATAR, ROUND_AVATAR, 11, a, hurtPct * 0.5f);
            Stencil.uninitStencilBuffer();
        } else {
            MatrixStack tmp = new MatrixStack();
            tmp.push();
            tmp.translate(headX + ROUND_AVATAR / 2f, headY + ROUND_AVATAR / 2f, 0);
            tmp.scale(1.5f, 1.5f, 1);
            Fonts.sfMedium.drawCenteredText(tmp, "?", 0.0f, QUESTION_MARK_CENTER_Y,
                    ColorUtils.reAlphaInt(Color.WHITE.getRGB(), (int) (255 * a)), QUESTION_MARK_FONT_SIZE);
            tmp.pop();
        }

        drawHeadRings(headX, headY, ROUND_AVATAR, a, hpNorm2, absNorm, Theme.MainColor(0));

        float nameX = headX + ROUND_AVATAR + ROUND_GAP_HEAD_TEXT;
        float titleY = 6.2f;
        int nameCol = ColorUtils.rgba(255, 255, 255, (int) (255 * a));
        drawSfMediumNameWithFade(ms, 15, name, nameX, titleY, nameCol, nameMaxWidth);

        float hpX = pillW - ROUND_PAD_X - hpW;
        int hpCol = ColorUtils.rgba(255, 255, 255, (int) (242 * a));
        ClientFonts.sf_medium[12].drawString(ms, hpText, hpX, titleY + 0.5f, hpCol);

        drawItemsRow(nameX, pillH - 10.2f, pillW - nameX - ROUND_PAD_X, itemsAlpha);

        long now = renderConsumableChip(pillW, headY, a);
        handleDamageEffects(headX + ROUND_AVATAR / 2f, headY + ROUND_AVATAR + 5f);

        draw2DParticles(a);
        renderTotemEffect(0, 0, pillW, pillH, ROUND_PILL_R, a, now, true);

        drawRipplesTop(headX, headY, ROUND_AVATAR, a, now, Theme.MainColor(0));

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    private void renderJuicyStyle(EventDisplay e, float absorb) {
        MatrixStack ms = e.getMatrixStack();

        drag.setWidth(JUICY_WIDTH);
        drag.setHeight(JUICY_HEIGHT);

        float alpha = (float) fadeAnim.getOutput();
        float itemsAlpha = getItemsRenderAlpha(alpha);
        float grabScale = drag.getGrabScale();
        float grabRot = drag.getWobbleAngle();

        GL11.glPushMatrix();
        if (Math.abs(grabScale - 1.0f) > 0.001f) {
            RenderUtility.customScaledObject2D(posX, posY, JUICY_WIDTH, JUICY_HEIGHT, grabScale);
        }
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glTranslatef(posX, posY, 0);
        if (Math.abs(grabRot) > 0.001f) {
            RenderUtility.customRotatedObject2D(0, 0, JUICY_WIDTH, JUICY_HEIGHT, grabRot);
        }

        if (blurEnabled) {
            BlurUtils.renderRoundedBlur(0, 0, JUICY_WIDTH, JUICY_HEIGHT, JUICY_RADIUS, alpha);
        }

        drawJuicyCard(alpha);

        float headX = 3.5f;
        float headY = 3.5f;
        float hurtPct = entity != null ? lastHurtPercent : 0f;

        drawJuicyHead(headX, headY, alpha, hurtPct);

        String targetName = entity != null ? entity.getName().getString() : lastName;
        float juicyNameMaxWidth = 34f;

        int themeColor = Theme.MainColor(0);
        int textColor = ColorUtils.rgba(255, 255, 255, (int) (245 * alpha));
        int nameColor = ColorUtils.rgba(255, 255, 255, (int) (255 * alpha));

        drawSfMediumNameClipped(ms, 13, targetName, 31f, 6.2f, nameColor, juicyNameMaxWidth);

        String hpText = String.valueOf((int) Math.ceil(animHpVal));
        String goldHpText = absorb > 0.05f ? "+" + (int) Math.ceil(absorb) : "";
        float hpTextW = ClientFonts.sf_medium[13].getWidth(hpText);
        float goldHpTextW = goldHpText.isEmpty() ? 0f : ClientFonts.sf_medium[13].getWidth(goldHpText);
        float goldGap = goldHpText.isEmpty() ? 0f : 1.2f;
        float hpX = JUICY_WIDTH - 5.5f - hpTextW - goldHpTextW - goldGap;

        ClientFonts.sf_medium[13].drawString(ms, hpText, hpX, 6.2f, textColor);
        if (!goldHpText.isEmpty()) {
            drawJuicyGoldHpText(ms, goldHpText, hpX + hpTextW + goldGap, 6.2f, alpha);
        }

        float barX = 31f;
        float barY = 22.5f;
        float barW = 57f;
        float barH = 4.5f;
        float hpWidth = MathHelper.clamp(barW * hpNorm, 0f, barW);
        float absorbWidth = MathHelper.clamp(barW * absNorm, 0f, barW);

        int goldColor = ColorUtils.rgba(255, 214, 92, 255);

        if (hpWidth > 0.5f) {
            float hpR = getJuicyBarRadius(hpWidth, barH);
            RenderUtility.drawShadow(barX, barY - 0.3f, hpWidth, barH + 0.6f, 9,
                    ColorUtils.reAlphaInt(themeColor, (int) (55 * alpha)));
            RenderUtility.drawRoundedRectWithRotatingGradient(barX, barY, hpWidth, barH, hpR, themeColor, headRingRotation, alpha);
            RenderUtility.drawRoundedRect(barX, barY, hpWidth, barH * 0f,
                    new Vector4f(hpR, hpR, 0f, 0f),
                    ColorUtils.reAlphaInt(ColorUtils.brighter(themeColor, 0.22f), (int) (38 * alpha)));
        }

        if (absorbWidth > 0.5f) {
            float absorbX = barX;
            float absorbR = getJuicyBarRadius(absorbWidth, barH);
            RenderUtility.drawShadow(absorbX, barY - 0.25f, absorbWidth, barH + 0.5f, 8,
                    ColorUtils.reAlphaInt(goldColor, (int) (64 * alpha)));
            RenderUtility.drawRoundedRectWithRotatingGradient(absorbX, barY, absorbWidth, barH, absorbR, goldColor, headRingRotation * 0.55f, alpha);
            RenderUtility.drawRoundedRectOutline(absorbX, barY, absorbWidth, barH, absorbR, 0.45f,
                    ColorUtils.reAlphaInt(ColorUtils.brighter(goldColor, 0.12f), (int) (44 * alpha)));

            float goldLight = MathHelper.clamp(absNorm, 0f, 1f);
            RenderUtility.drawShadow(
                    JUICY_WIDTH - 30.5f,
                    1.2f,
                    25.5f,
                    JUICY_HEIGHT - 2.4f,
                    10,
                    ColorUtils.reAlphaInt(goldColor, (int) (17f * alpha * goldLight))
            );
            RenderUtility.drawShadow(
                    barX - 1.0f,
                    barY - 1.6f,
                    barW + 2.0f,
                    barH + 2.5f,
                    9,
                    ColorUtils.reAlphaInt(goldColor, (int) (19f * alpha * goldLight))
            );
            if (!goldHpText.isEmpty()) {
                RenderUtility.drawShadow(
                        hpX + hpTextW + goldGap - 0.8f,
                        4.9f,
                        goldHpTextW + 1.8f,
                        7.1f,
                        7,
                        ColorUtils.reAlphaInt(goldColor, (int) (23f * alpha * goldLight))
                );
            }
        }

        drawJuicyItems(30.5f, 11.7f, itemsAlpha);

        long now = renderConsumableChip(JUICY_WIDTH, headY, alpha);
        handleDamageEffects(headX + JUICY_HEAD_SIZE / 2f, headY + JUICY_HEAD_SIZE / 2f);

        draw2DParticles(alpha);
        renderTotemEffect(0, 0, JUICY_WIDTH, JUICY_HEIGHT, JUICY_RADIUS, alpha, now, false);

        drawRipplesTop(headX, headY, JUICY_HEAD_SIZE, alpha, now, themeColor);

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    private void drawJuicyCard(float alpha) {
        int themeColor1 = Theme.MainColor(0);
        int themeColor2 = Theme.MainColor(1);
        int base = ColorUtils.rgba(25, 25, 30, (int) (160 * alpha));
        int darkLayer = ColorUtils.rgba(0, 0, 0, (int) (120 * alpha * 0.72f));
        Vector4f round = new Vector4f(JUICY_RADIUS, JUICY_RADIUS, JUICY_RADIUS, JUICY_RADIUS);
        RenderUtility.drawRoundedRect(0, 0, JUICY_WIDTH, JUICY_HEIGHT, round, base);
        RenderUtility.drawRoundedRect(0, 0, JUICY_WIDTH, JUICY_HEIGHT, round, darkLayer);

        drawJuicyBloomOutline(0f, 0f, JUICY_WIDTH, JUICY_HEIGHT, JUICY_RADIUS, alpha, themeColor1, themeColor2, 1.0f);
    }

    private void drawJuicyHead(float headX, float headY, float alpha, float hurtPct) {
        int headBg = ColorUtils.rgba(25, 25, 30, (int) (160 * alpha));
        int headDark = ColorUtils.rgba(0, 0, 0, (int) (120 * alpha * 0.65f));
        RenderUtility.drawRoundedRect(headX, headY, JUICY_HEAD_SIZE, JUICY_HEAD_SIZE, 4f, headBg);
        RenderUtility.drawRoundedRect(headX, headY, JUICY_HEAD_SIZE, JUICY_HEAD_SIZE, 4f, headDark);

        if (lastWasPlayer && lastSkin != null) {
            RenderUtility.drawAccurateHead(lastSkin, headX, headY, JUICY_HEAD_SIZE, JUICY_HEAD_SIZE, 4f, alpha, hurtPct * 0.5f);
        } else {
            MatrixStack tmp = new MatrixStack();
            tmp.push();
            tmp.translate(headX + JUICY_HEAD_SIZE / 2f, headY + JUICY_HEAD_SIZE / 2f, 0);
            tmp.scale(1.8f, 1.8f, 1f);
            Fonts.sfMedium.drawCenteredText(tmp, "?", 0f, QUESTION_MARK_CENTER_Y,
                    ColorUtils.reAlphaInt(Color.WHITE.getRGB(), (int) (255 * alpha)), QUESTION_MARK_FONT_SIZE);
            tmp.pop();
        }

        if (entity != null && entity.hurtTime > 0) {
            float hitAlpha = MathHelper.clamp(hurtPct, 0f, 1f) * alpha;
            RenderUtility.drawRoundedRect(headX, headY, JUICY_HEAD_SIZE, JUICY_HEAD_SIZE, 4f,
                    ColorUtils.rgba(255, 70, 70, (int) (72 * hitAlpha)));
        }

        drawJuicyBloomOutline(headX, headY, JUICY_HEAD_SIZE, JUICY_HEAD_SIZE, 4f, alpha, Theme.MainColor(0), Theme.MainColor(1), 0.55f);
    }

    private void drawJuicyBloomOutline(float x, float y, float width, float height, float radius, float alpha, int colorLeft, int colorRight, float strength) {
        float k = MathHelper.clamp(strength, 0.45f, 1.25f);
        int outline = ColorUtils.rgba(255, 255, 255, (int) (30f * alpha * k));
        RenderUtility.drawRoundedRectOutline(x, y, width, height, radius, 0.5f, outline);
    }

    private void drawJuicyGoldHpText(MatrixStack ms, String text, float x, float y, float alpha) {
        if (text == null || text.isEmpty()) {
            return;
        }

        int glowOuter = ColorUtils.reAlphaInt(ColorUtils.rgba(255, 184, 72, 255), (int) (42 * alpha));
        int glowInner = ColorUtils.reAlphaInt(ColorUtils.rgba(255, 233, 156, 255), (int) (30 * alpha));

        ClientFonts.sf_medium[13].drawString(ms, text, x - 0.24f, y, glowOuter);
        ClientFonts.sf_medium[13].drawString(ms, text, x + 0.24f, y, glowOuter);
        ClientFonts.sf_medium[13].drawString(ms, text, x, y - 0.24f, glowOuter);
        ClientFonts.sf_medium[13].drawString(ms, text, x, y + 0.24f, glowOuter);
        ClientFonts.sf_medium[13].drawString(ms, text, x - 0.36f, y, glowInner);
        ClientFonts.sf_medium[13].drawString(ms, text, x + 0.36f, y, glowInner);

        drawJuicyGoldHpGradientText(ms, text, x, y, alpha);
    }

    private float getJuicyBarRadius(float width, float height) {
        float minSide = Math.max(0.01f, Math.min(width, height));
        return MathHelper.clamp(minSide * 0.5f, 0.35f, 2.5f);
    }

    private void drawJuicyGoldHpGradientText(MatrixStack ms, String text, float x, float y, float alpha) {
        float clampedAlpha = MathHelper.clamp(alpha, 0f, 1f);
        float cx = x;
        int len = text.length();
        for (int i = 0; i < len; i++) {
            String ch = String.valueOf(text.charAt(i));
            float t = len > 1 ? i / (float) (len - 1) : 0f;
            int cA = ColorUtils.reAlphaInt(ColorUtils.rgba(255, 245, 183, 255), (int) (255 * clampedAlpha));
            int cB = ColorUtils.reAlphaInt(ColorUtils.rgba(255, 179, 58, 255), (int) (255 * clampedAlpha));
            ClientFonts.sf_medium[13].drawString(ms, ch, cx, y, ColorUtils.interpolateColor(cA, cB, t));
            cx += ClientFonts.sf_medium[13].getWidth(ch);
        }
    }

    private void drawJuicyItems(float x, float y, float alpha) {
        if (alpha <= 0.01f) {
            return;
        }

        renderItems.clear();
        for (ItemStack stack : lastItems) {
            if (!stack.isEmpty() && !(stack.getItem() instanceof AirItem)) {
                renderItems.add(stack);
            }
        }

        int count = Math.min(MAX_ITEMS, renderItems.size());
        for (int i = 0; i < count; i++) {
            ItemStack stack = renderItems.get(i);
            GL11.glPushMatrix();
            GL11.glTranslatef(x + i * 8f, y, 0);
            GL11.glScalef(0.5f, 0.5f, 1f);
            GlStateManager.color4f(1f, 1f, 1f, alpha);
            mc.getItemRenderer().renderItemIntoGUI(stack, 0, 0);
            mc.getItemRenderer().renderItemOverlayIntoGUI(mc.fontRenderer, stack, 0, 0, null);
            GlStateManager.color4f(1f, 1f, 1f, 1f);
            GL11.glPopMatrix();
        }
    }

    private boolean isCompactTargetHudMode(String mode) {
        return mode != null && (
                mode.equalsIgnoreCase("Маленький")
                        || mode.equalsIgnoreCase("\u041C\u0430\u043B\u0435\u043D\u044C\u043A\u0438\u0439")
                        || mode.equalsIgnoreCase("Круглый")
                        || mode.equalsIgnoreCase("\u041A\u0440\u0443\u0433\u043B\u044B\u0439")
        );
    }


    private float getHudHeight(String mode) {
        return isCompactTargetHudMode(mode) ? ROUND_PILL_H : JUICY_HEIGHT;
    }

    private float getItemsRenderAlpha(float hudAlpha) {
        float alpha = Math.min(hudAlpha, (float) itemsFadeAnim.getOutput());
        if (entity == null) {
            alpha *= hudAlpha;
            if (hudAlpha <= 0.2f) {
                return 0f;
            }
        }
        return alpha;
    }

    private boolean isFlowHudModeSafe(HUD hud) {
        return hud != null && hud.isFlowHudMode();
    }

    private boolean isProjectedTargetHudVisible(Vector2d projectedPos) {
        if (projectedPos == null || mc.getMainWindow() == null) {
            return false;
        }

        return projectedPos.x >= 0.0
                && projectedPos.x <= mc.getMainWindow().getScaledWidth()
                && projectedPos.y >= 0.0
                && projectedPos.y <= mc.getMainWindow().getScaledHeight();
    }

    private void drawSfMediumNameClipped(MatrixStack matrixStack, int fontIndex, String text, float x, float y, int color, float maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 0.5f) {
            return;
        }

        if (ClientFonts.sf_medium[fontIndex].getWidth(text) <= maxWidth) {
            ClientFonts.sf_medium[fontIndex].drawString(matrixStack, text, x, y, color);
            return;
        }

        String ellipsis = "...";
        float ellipsisWidth = ClientFonts.sf_medium[fontIndex].getWidth(ellipsis);
        if (ellipsisWidth >= maxWidth) {
            String clipped = text;
            while (!clipped.isEmpty() && ClientFonts.sf_medium[fontIndex].getWidth(clipped) > maxWidth) {
                clipped = clipped.substring(0, clipped.length() - 1);
            }
            if (!clipped.isEmpty()) {
                ClientFonts.sf_medium[fontIndex].drawString(matrixStack, clipped, x, y, color);
            }
            return;
        }

        String clipped = text;
        while (!clipped.isEmpty() && ClientFonts.sf_medium[fontIndex].getWidth(clipped + ellipsis) > maxWidth) {
            clipped = clipped.substring(0, clipped.length() - 1);
        }

        if (clipped.isEmpty()) {
            return;
        }

        ClientFonts.sf_medium[fontIndex].drawString(matrixStack, clipped + ellipsis, x, y, color);
    }

    private void drawSfMediumNameWithFade(MatrixStack matrixStack, int fontIndex, String text, float x, float y, int color, float maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 0.5f) {
            return;
        }

        if (ClientFonts.sf_medium[fontIndex].getWidth(text) <= maxWidth) {
            ClientFonts.sf_medium[fontIndex].drawString(matrixStack, text, x, y, color);
            return;
        }

        int fadeChars = Math.min(4, text.length());
        float fadeCharsWidth = 0f;
        for (int i = text.length() - fadeChars; i < text.length(); i++) {
            fadeCharsWidth += ClientFonts.sf_medium[fontIndex].getWidth(String.valueOf(text.charAt(i)));
        }
        float fadeWidth = MathHelper.clamp(fadeCharsWidth, 7f, Math.max(7f, maxWidth - 0.5f));
        float solidWidth = Math.max(0f, maxWidth - fadeWidth);
        float currentX = x;
        int baseAlpha = (color >>> 24) & 255;
        if (baseAlpha <= 0) {
            baseAlpha = 255;
        }
        int rgb = color & 0x00FFFFFF;

        for (int i = 0; i < text.length(); i++) {
            String character = text.substring(i, i + 1);
            float charWidth = ClientFonts.sf_medium[fontIndex].getWidth(character);
            float offset = currentX - x;
            if (offset + charWidth > maxWidth + 0.01f) {
                break;
            }

            float center = offset + charWidth * 0.5f;
            float alphaMul = 1f;
            if (center > solidWidth) {
                float t = MathHelper.clamp((center - solidWidth) / Math.max(0.01f, fadeWidth), 0f, 1f);
                alphaMul = 1f - t;
            }

            int charAlpha = Math.max(0, Math.min(255, (int) (baseAlpha * alphaMul)));
            if (charAlpha <= 2) {
                break;
            }

            ClientFonts.sf_medium[fontIndex].drawString(matrixStack, character, currentX, y, (charAlpha << 24) | rgb);
            currentX += charWidth;
        }
    }

    private void drawRoundPill(float x, float y, float w, float h, float r, float a) {
        if (flowHudRects) {
            float px = x + 3f;
            float pw = w - 3f;
            float pr = r + 4f;
            Vector4f round = new Vector4f(pr, pr, pr, pr);
            int base = ColorUtils.rgba(25, 25, 30, (int) (160 * a));
            int dark = ColorUtils.rgba(0, 0, 0, (int) (120 * a * 0.72f));
            int outline = ColorUtils.rgba(255, 255, 255, (int) (30 * a));
            RenderUtility.drawRoundedRect(px, y, pw, h, round, base);
            RenderUtility.drawRoundedRect(px, y, pw, h, round, dark);
            RenderUtility.drawRoundedRectOutline(px, y, pw, h, pr, 0.5f, outline);
            return;
        }

        RenderUtility.drawShadow(x + 3, y + 1, w - 3, h, 13, ColorUtils.rgba(0, 0, 0, (int) (56 * a)));
        int c1 = ColorUtils.rgba(12, 12, 18, (int) (205 * a));
        int c2 = ColorUtils.rgba(14, 14, 20, (int) (205 * a));
        int c3 = ColorUtils.rgba(8, 8, 12, (int) (205 * a));
        int c4 = ColorUtils.rgba(10, 10, 14, (int) (205 * a));
        int tone = Theme.RectColor(0);
        c1 = ColorUtils.interpolateColor(c1, tone, 0.05f);
        c2 = ColorUtils.interpolateColor(c2, tone, 0.05f);
        c3 = ColorUtils.interpolateColor(c3, tone, 0.07f);
        c4 = ColorUtils.interpolateColor(c4, tone, 0.07f);
        RenderUtility.drawRoundedRect(x + 3, y, w - 3, h, new Vector4f(r + 4, r + 4, r + 4, r + 4), new Vector4i(c1, c2, c3, c4));
    }

    private void drawHeadRings(float headX, float headY, float avatarSize, float a, float hp, float absorb, int theme) {
        float cx = headX + avatarSize / 2f;
        float cy = headY + avatarSize / 2f;

        float baseR = avatarSize / 2f + 1.6f;
        float trackTh = 0.45f;
        float hpTh = 0.60f;
        float feather = 1.2f;

        int track = ColorUtils.rgba(255, 255, 255, (int) (24 * a));
        RenderUtility.drawRingArcAA(cx, cy, baseR, trackTh, 0f, 360f, track, feather);

        float start = -90f;
        float hpAngle = Math.max(0.001f, Math.min(hp, 0.9999f)) * 359.8f;
        float endHp = start + hpAngle;

        int hpColor = ColorUtils.reAlphaInt(theme, (int) (228 * a));
        RenderUtility.drawRingArcAAWithRotatingGradient(
                cx, cy,
                baseR,
                hpTh,
                start,
                endHp,
                hpColor,
                feather,
                headRingRotation
        );

        if (absorb > 0.001f) {
            float absR = baseR + 1.2f;
            float absAngle = Math.max(0.001f, Math.min(absorb, 0.9995f)) * 359.8f;
            float endAbs = start + absAngle;

            int yellowColor = ColorUtils.rgba(255, 220, 80, (int) (255 * a));

            RenderUtility.drawRingArcAAWithRotatingGradient(
                    cx, cy,
                    absR,
                    0.35f,
                    start,
                    endAbs,
                    yellowColor,
                    1.0f,
                    headRingRotation * 0.5f
            );
        }
    }

    private void addRipple() {
        long now = System.currentTimeMillis();
        rippleQueue.addLast(now);
        while (rippleQueue.size() > MAX_RIPPLES) rippleQueue.removeFirst();
    }

    private void drawRipplesTop(float headX, float headY, float avatarSize, float a, long now, int theme) {
        float cx = headX + avatarSize / 2f;
        float cy = headY + avatarSize / 2f;

        Iterator<Long> it = rippleQueue.iterator();
        expiredRipples.clear();
        int idx = 0;
        while (it.hasNext()) {
            long t0 = it.next();
            float t = (now - t0) / (float) RIPPLE_DUR;
            if (t >= 1f) {
                expiredRipples.add(t0);
                continue;
            }
            t = MathHelper.clamp(t, 0f, 1f);

            float startR = avatarSize / 2f + 3.0f + idx * 1.2f;
            float endR = startR + 15.0f;
            float r = MathHelper.lerp(t, startR, endR);

            float th = 0.85f;
            float alphaMul = (1.0f - t) * (1.0f - t);
            int col = ColorUtils.reAlphaInt(ColorUtils.brighter(theme, 0.55f), (int) (208 * alphaMul * a));

            RenderUtility.drawRingArcAAWithRotatingGradient(cx, cy, r, th, 0f, 360f, col, 1.4f, headRingRotation + t * 360f);
            idx++;
        }
        for (Long k : expiredRipples) rippleQueue.remove(k);
    }

    private void drawItemsRow(float startX, float baselineY, float availableW, float alpha) {
        if (alpha < 0.01f) return;

        reversedRenderItems.clear();
        for (int i = lastItems.size() - 1; i >= 0; i--) {
            ItemStack stack = lastItems.get(i);
            if (!stack.isEmpty() && !(stack.getItem() instanceof AirItem)) {
                reversedRenderItems.add(stack);
            }
        }

        if (reversedRenderItems.isEmpty()) return;

        int maxByWidth = Math.max(1, (int) Math.floor((availableW + (ITEM_GAP - 16 * ITEM_SCALE)) / ITEM_GAP));
        int max = Math.min(Math.min(MAX_ITEMS, reversedRenderItems.size()), maxByWidth);

        float x = startX;
        for (int i = 0; i < max; i++) {
            ItemStack st = reversedRenderItems.get(i);

            GL11.glPushMatrix();
            GL11.glTranslatef(x, baselineY - 10.4f, 0);
            GL11.glScalef(ITEM_SCALE, ITEM_SCALE, 1);

            GlStateManager.color4f(1f, 1f, 1f, alpha);
            mc.getItemRenderer().renderItemIntoGUI(st, 0, 0);
            GlStateManager.color4f(1f, 1f, 1f, 1f);

            GL11.glPopMatrix();
            x += ITEM_GAP;
        }
    }

    private long renderConsumableChip(float width, float headY, float alpha) {
        long now = System.currentTimeMillis();
        float progress = getConsumableProgress(entity);

        if (progress > 0f && lastUseProg == 0f) {
            useUiStartMs = now;
        }

        lastUseProg = progress;
        useUiAnim += ((progress > 0f ? 1f : 0f) - useUiAnim) * 0.25f;

        if (progress > 0f && entity != null) {
            lastUseStack = entity.getActiveItemStack().copy();
        }

        if (useUiAnim > 0.01f && !lastUseStack.isEmpty()) {
            drawConsumableChip(width, headY, alpha, useUiAnim, progress, lastUseStack, now - useUiStartMs);
        } else if (useUiAnim <= 0.01f) {
            lastUseStack = ItemStack.EMPTY;
        }

        return now;
    }

    private void drawConsumableChip(float pillW, float headY, float a, float ui, float prog, ItemStack icon, long sinceStart) {
        float slide = easeOutBack(ui) * 26f;
        float x = pillW + 6f + slide;
        float y = headY - 1.5f;
        float w = 24f, h = 24f, r = 12f;

        float pulse = prog > 0.3f ? 0.95f + 0.05f * (float) Math.sin(sinceStart * 0.008f) : 1f;
        float scale = 0.9f + 0.1f * pulse;

        int linkA = ColorUtils.rgba(255, 255, 255, (int) (15 * a * ui));
        int linkB = ColorUtils.rgba(255, 255, 255, (int) (5 * a * ui));
        RenderUtility.drawRectVerticalW(pillW + 2f, y + h / 2f - 1f, 4f + slide, 2f, linkA, linkB);

        if (prog > 0.2f) {
            int glowColor = ColorUtils.reAlphaInt(Theme.MainColor(0), (int) (60 * a * ui * prog));
            RenderUtility.drawShadow(x - 2, y - 2, w + 4, h + 4, 8, glowColor);
        }

        int bg = ColorUtils.rgba(20, 20, 26, (int) (200 * a));
        RenderUtility.drawRoundedRect(x, y, w, h, new Vector4f(r, r, r, r), bg);

        RenderUtility.drawRoundedOutline(x, y, x + w, y + h, r, 0.5f,
                ColorUtils.reAlphaInt(Theme.MainColor(0), (int) (30 * a)));

        GL11.glPushMatrix();
        GL11.glTranslatef(x + w / 2f, y + h / 2f, 0);
        GL11.glScalef(scale, scale, 1);
        GL11.glTranslatef(-8, -8, 0);
        GlStateManager.color4f(1f, 1f, 1f, a);
        mc.getItemRenderer().renderItemIntoGUI(icon, 0, 0);
        GlStateManager.color4f(1f, 1f, 1f, 1f);
        GL11.glPopMatrix();

        if (prog > 0.01f) {
            float cx = x + w / 2f;
            float cy = y + h / 2f;
            float radius = 15.5f;

            RenderUtility.drawRingArcAA(cx, cy, radius, 0.6f, 0f, 360f,
                    ColorUtils.rgba(255, 255, 255, (int) (20 * a)), 1.0f);

            int ringColor = ColorUtils.reAlphaInt(Theme.MainColor(0), (int) (240 * a));
            float endAngle = -90f + MathHelper.clamp(prog, 0f, 1f) * 360f;
            RenderUtility.drawRingArcAAWithRotatingGradient(cx, cy, radius, 1.0f, -90f, endAngle, ringColor, 1.2f, headRingRotation);
        }
    }

    private void handleDamageEffects(float particleX, float particleY) {
        if (entity == null) {
            return;
        }

        boolean triggered = false;
        if (entity.hurtTime > 0 && prevHurtTime == 0) {
            triggered = true;
            spawn2DDamageParticles(particleX, particleY);
        }
        prevHurtTime = entity.hurtTime;

        float healthSum = entity.getHealth() + entity.getAbsorptionAmount();
        if (prevHealthSum < 0f) {
            prevHealthSum = healthSum;
        }
        if (healthSum < prevHealthSum - 0.05f) {
            triggered = true;
        }
        prevHealthSum = healthSum;

        boolean damageActive = entity.getLastDamageSource() != null;
        if (damageActive && !lastDamageActive) {
            triggered = true;
        }
        lastDamageActive = damageActive;

        if (triggered) {
            addRipple();
        }
    }

    private void renderTotemEffect(float x, float y, float width, float height, float radius, float alpha, long now, boolean round) {
        if (totemFxStartMs <= 0) {
            return;
        }

        long elapsed = now - totemFxStartMs;
        if (elapsed < TOTEM_FX_DUR) {
            isTotemAnimating = true;
            float progress = MathHelper.clamp(elapsed / (float) TOTEM_FX_DUR, 0f, 1f);
            drawTotemOverlay(x, y, width, height, radius, alpha, progress, round);
            return;
        }

        totemFxStartMs = -1L;
        isTotemAnimating = false;
        lastTotemAutoMs = now;
    }

    private void drawTotemOverlay(float x, float y, float w, float h, float r, float a, float t, boolean isRound) {
        if (a < 0.01f) return;

        float overlayAlpha;
        float totemScale;
        float totemY;
        float glowIntensity = 0f;

        if (t <= 0.25f) {
            float phase1 = t / 0.25f;
            overlayAlpha = easeOutCubic(phase1) * 0.85f;
            totemScale = 0.3f + easeOutBack(phase1) * 0.8f;
            totemY = easeOutQuad(phase1) * 5f;
            glowIntensity = phase1 * 0.8f;
        } else if (t <= 0.75f) {
            float phase2 = (t - 0.25f) / 0.5f;
            overlayAlpha = 0.85f;
            float pulse = 1f + 0.03f * (float) Math.sin(phase2 * Math.PI * 4);
            totemScale = 1.1f * pulse;
            totemY = 5f + (float) Math.sin(phase2 * Math.PI * 2) * 1.5f;
            glowIntensity = 0.8f + 0.2f * (float) Math.sin(phase2 * Math.PI * 3);
        } else {
            float phase3 = (t - 0.75f) / 0.25f;
            overlayAlpha = (1f - easeInQuad(phase3)) * 0.85f;
            totemScale = 1.1f - easeInBack(phase3) * 0.9f;
            totemY = 5f - easeOutCubic(phase3) * 30f;
            glowIntensity = (1f - phase3) * 0.8f;
        }

        float cx = x + w / 2f;
        float cy = y + h / 2f;

        int bgColor;
        if (flowHudRects) {
            bgColor = ColorUtils.rgba(25, 25, 30, (int) (overlayAlpha * 210f * a));
        } else {
            bgColor = ColorUtils.rgba(12, 12, 16, (int) (overlayAlpha * 240f * a));
            int tone = Theme.RectColor(0);
            bgColor = ColorUtils.interpolateColor(bgColor, tone, 0.06f);
        }

        Stencil.initStencilToWrite();
        if (isRound) {
            RenderUtility.drawRoundedRect(x + 3, y, w - 3, h, new Vector4f(r + 4, r + 4, r + 4, r + 4), -1);
        } else {
            RenderUtility.drawRoundedRect(x, y, w, h, new Vector4f(r, r, r, r), -1);
        }
        Stencil.readStencilBuffer(1);

        if (isRound) {
            RenderUtility.drawRoundedRect(x + 3, y, w - 3, h, new Vector4f(r + 4, r + 4, r + 4, r + 4), bgColor);
        } else {
            RenderUtility.drawRoundedRect(x, y, w, h, new Vector4f(r, r, r, r), bgColor);
        }

        if (flowHudRects) {
            int outline = ColorUtils.rgba(255, 255, 255, (int) (30 * overlayAlpha * a));
            if (isRound) {
                RenderUtility.drawRoundedRectOutline(x + 3, y, w - 3, h, r + 4, 0.5f, outline);
            } else {
                RenderUtility.drawRoundedRectOutline(x, y, w, h, r, 0.5f, outline);
            }
        }

        if (glowIntensity > 0.01f && totemScale > 0.01f) {
            int glowColor = ColorUtils.rgba(255, 215, 0, (int) (glowIntensity * 100f * overlayAlpha * a));
            float glowSize = 35f * totemScale;
            RenderUtility.drawShadow(cx - glowSize / 2f, cy + totemY - glowSize / 2f, glowSize, glowSize, 18, glowColor);
        }

        if (totemScale > 0.01f && overlayAlpha > 0.01f) {
            GL11.glPushMatrix();
            GL11.glTranslatef(cx, cy + totemY, 0);
            GL11.glScalef(totemScale, totemScale, 1);
            GL11.glTranslatef(-8, -8, 0);
            GlStateManager.color4f(1f, 1f, 1f, overlayAlpha * a);
            mc.getItemRenderer().renderItemIntoGUI(totemStack, 0, 0);
            GlStateManager.color4f(1f, 1f, 1f, 1f);
            GL11.glPopMatrix();
        }

        Stencil.uninitStencilBuffer();
    }

    private void spawn2DDamageParticles(float centerX, float centerY) {
        int themeColor = Theme.MainColor(0);

        for (int i = 0; i < DAMAGE_PARTICLES_COUNT; i++) {
            float angle = (float)(Math.random() * 360f);
            float speed = 1.5f + (float)(Math.random() * 2f);
            float lifetime = 800f + (float)(Math.random() * 400f);

            damageParticles.add(new DamageParticle2D(
                    centerX,
                    centerY,
                    angle,
                    speed,
                    lifetime,
                    themeColor
            ));
        }
    }

    private void updateDamageParticles() {
        damageParticles.removeIf(p -> p.lifetime <= 0);
        for (DamageParticle2D p : damageParticles) {
            p.update();
        }
    }

    private void draw2DParticles(float globalAlpha) {
        if (damageParticles.isEmpty()) return;

        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        for (DamageParticle2D p : damageParticles) {
            float alpha = p.getAlpha() * globalAlpha;
            if (alpha < 0.01f) continue;

            int color = ColorUtils.reAlphaInt(p.color, (int)(255 * alpha));

            GL11.glPushMatrix();
            GL11.glTranslatef(p.x, p.y, 0);
            GL11.glRotatef(p.rotation, 0, 0, 1);

            float glowSize = p.size * 2f;
            int glowColor = ColorUtils.reAlphaInt(p.color, (int)(48 * alpha));
            RenderUtility.drawImageAlphaSmooth(FIREFLY_TEXTURE, -glowSize / 2f, -glowSize / 2f, glowSize, glowSize, glowColor);
            RenderUtility.drawImageAlphaSmooth(FIREFLY_TEXTURE, -p.size / 2f, -p.size / 2f, p.size, p.size, color);

            GL11.glPopMatrix();
        }

        GL11.glColor4f(1f, 1f, 1f, 1f);
        com.mojang.blaze3d.systems.RenderSystem.enableTexture();
        com.mojang.blaze3d.systems.RenderSystem.enableAlphaTest();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    private void startTotemAnimation(long now) {
        if (!isTotemAnimating && now - lastTotemAutoMs >= 2000) {
            totemFxStartMs = now;
            lastTotemAutoMs = now;
            isTotemAnimating = true;
        }
    }

    private boolean shouldShowTargetItems(HUD hud, LivingEntity target) {
        if (target == null) {
            return false;
        }

        if (mc.currentScreen instanceof ChatScreen) {
            return target == mc.player;
        }

        LivingEntity auraTarget = Harmony.getInstance().getModuleManager().getHitAura().getTarget();
        if (auraTarget != null && fixHealth(auraTarget.getHealth()) > 0.0f) {
            return auraTarget.getEntityId() == target.getEntityId();
        }

        if (hud != null && hud.targetHudRaycast.get() && mc.objectMouseOver instanceof net.minecraft.util.math.EntityRayTraceResult) {
            Entity pointedEntity = ((net.minecraft.util.math.EntityRayTraceResult) mc.objectMouseOver).getEntity();
            return pointedEntity instanceof LivingEntity && pointedEntity.getEntityId() == target.getEntityId();
        }

        return false;
    }

    private LivingEntity resolveTarget(LivingEntity current) {
        if (mc.currentScreen instanceof ChatScreen) {
            rememberResolvedTarget(mc.player);
            return mc.player;
        }

        HUD hud = Harmony.getInstance().getModuleManager().getHud();
        var aura = Harmony.getInstance().getModuleManager().getHitAura().getTarget();

        if (aura != null && fixHealth(aura.getHealth()) > 0.0f) {
            rememberResolvedTarget(aura);
            return aura;
        }

        if (hud != null && hud.targetHudRaycast.get()
                && mc.objectMouseOver instanceof net.minecraft.util.math.EntityRayTraceResult) {
            Entity ent = ((net.minecraft.util.math.EntityRayTraceResult) mc.objectMouseOver).getEntity();
            if (ent instanceof LivingEntity && ent != mc.player) {
                LivingEntity raycastTarget = (LivingEntity) ent;
                if (isUsableTarget(raycastTarget)) {
                    rememberResolvedTarget(raycastTarget);
                    return raycastTarget;
                }
            }
        }

        return getHeldTarget(current);
    }

    private void rememberResolvedTarget(LivingEntity target) {
        if (target == null) {
            return;
        }
        heldTarget = target;
        lastTargetSeenMs = System.currentTimeMillis();
    }

    private LivingEntity getHeldTarget(LivingEntity current) {
        LivingEntity candidate = current != null ? current : heldTarget;
        if (candidate == null || candidate == mc.player || !isUsableTarget(candidate)) {
            return null;
        }
        if (System.currentTimeMillis() - lastTargetSeenMs > TARGET_LOST_HOLD_MS) {
            return null;
        }
        return candidate;
    }

    private boolean isUsableTarget(LivingEntity target) {
        return target != null && target.isAlive() && target.getHealth() > 0.0f;
    }

    private void clearHeldTarget() {
        heldTarget = null;
        lastTargetSeenMs = 0L;
    }

    private float fixHealth(float original) {
        if (entity == null) return original;
        try {
            if (mc.world == null) return original;
            var obj = mc.world.getScoreboard().getObjectiveInDisplaySlot(2);
            if (obj == null) return original;
            Score score = mc.world.getScoreboard().getOrCreateScore(entity.getScoreboardName(), obj);
            String header = mc.ingameGUI.getTabList().header == null ? " " : mc.ingameGUI.getTabList().header.getString().toLowerCase();
            boolean fun = mc.getCurrentServerData() != null
                    && mc.getCurrentServerData().serverIP.contains("funtime")
                    && (header.contains("анархия") || header.contains("гриферский"))
                    && entity instanceof PlayerEntity;
            return fun ? score.getScorePoints() : original;
        } catch (Throwable ignored) {
        }
        return original;
    }

    private float getConsumableProgress(LivingEntity ent) {
        if (ent == null || !ent.isHandActive()) return 0f;
        ItemStack st = ent.getActiveItemStack();
        if (st == null || st.isEmpty()) return 0f;
        UseAction act = st.getUseAction();
        boolean isConsumable = (act == UseAction.EAT || act == UseAction.DRINK) || st.isFood();
        if (!isConsumable) return 0f;

        int dur = st.getUseDuration();
        if (dur <= 0) dur = 32;
        int left = ent.getItemInUseCount();
        float used = Math.max(0f, dur - left);
        return MathHelper.clamp(used / (float) dur, 0f, 1f);
    }

    private float lerp(float c, float t, float s) {
        return c + (t - c) / (s + 1f);
    }

    private float easeOutCubic(float x) {
        x = MathHelper.clamp(x, 0f, 1f);
        return 1f - (float) Math.pow(1f - x, 3);
    }

    private float easeOutQuad(float x) {
        x = MathHelper.clamp(x, 0f, 1f);
        return 1f - (1f - x) * (1f - x);
    }

    private float easeInQuad(float x) {
        x = MathHelper.clamp(x, 0f, 1f);
        return x * x;
    }

    private float easeOutBack(float x) {
        float c1 = 1.70158f, c3 = c1 + 1f;
        x = MathHelper.clamp(x, 0f, 1f);
        return 1 + c3 * (float) Math.pow(x - 1, 3) + c1 * (float) Math.pow(x - 1, 2);
    }

    private float easeInBack(float x) {
        float c1 = 1.70158f, c3 = c1 + 1f;
        x = MathHelper.clamp(x, 0f, 1f);
        return c3 * x * x * x - c1 * x * x;
    }
}


