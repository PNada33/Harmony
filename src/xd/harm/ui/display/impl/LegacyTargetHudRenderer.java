package xd.harm.ui.display.impl;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AirItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.math.vector.Vector4f;
import net.minecraft.network.play.server.SEntityStatusPacket;
import org.joml.Vector2d;
import org.lwjgl.opengl.GL11;
import xd.harm.Harmony;
import xd.harm.events.render.EventDisplay;
import xd.harm.events.network.EventPacket;
import xd.harm.modules.impl.render.HUD;
import xd.harm.modules.impl.render.Theme;
import xd.harm.utils.SoundUtil;
import xd.harm.utils.client.IMinecraft;
import xd.harm.utils.client.ScaledResolution;
import xd.harm.utils.drag.Dragging;
import xd.harm.utils.math.MathUtil;
import xd.harm.utils.render.AnimationUtils;
import xd.harm.utils.render.BlurUtils;
import xd.harm.utils.render.rect.RenderUtility;
import xd.harm.utils.render.StencilUtil;
import xd.harm.utils.render.color.ColorUtils;
import xd.harm.utils.render.gl.Stencil;
import xd.harm.utils.text.GradientUtil;
import xd.harm.utils.text.font.ClientFonts;
import xd.harm.utils.text.font.styled.StyledFont;
import xd.harm.utils.voronoi.VoronoiOfCopyRenderTemp;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class LegacyTargetHudRenderer implements IMinecraft {

    private static final Set<String> LEGACY_MODES = new HashSet<>();
    private static final boolean PRE_RANGED_TARGET = true;
    private static final boolean TARGETTING_SFX = true;
    private static final long TARGET_LOST_HOLD_MS = 550L;

    static {
        Collections.addAll(LEGACY_MODES,
                "Маленький"
        );
    }

    private final AnimationUtils scale = new AnimationUtils(1.0F, 1.0F, 0.07F);
    private final AnimationUtils alphaHp = new AnimationUtils(0.0F, 0.0F, 0.075F);
    private final AnimationUtils hpAnim = new AnimationUtils(1.0F, 1.0F, 0.05F);
    private final AnimationUtils absorbAnim = new AnimationUtils(0.0F, 0.0F, 0.05F);
    private final AnimationUtils hurtHpAnim = new AnimationUtils(1.0F, 1.0F, 0.05F);

    private LivingEntity curTarget;
    private LivingEntity soundTarget;
    private LivingEntity heldTarget;
    private long lastTargetSeenMs;
    private ResourceLocation skin;
    private ResourceLocation oldSkin;

    private final List<Particle> particles = new ArrayList<>();
    private final List<VoronoiEvent> voronoiEvents = new ArrayList<>();
    private final List<ItemStack> topItems = new ArrayList<>();
    private final List<ItemStack> renderItems = new ArrayList<>();
    private final ItemStack totemStack = new ItemStack(Items.TOTEM_OF_UNDYING);
    private final NoiseAnimation noiser = new NoiseAnimation();

    private boolean posInit;
    private float xPosHud;
    private float yPosHud;
    private float widthHud = 120.0F;
    private float heightHud = 40.0F;

    private float hpRectAnim = 0.0F;
    private float hpHurtAnim = 0.0F;
    private float absRectAnim = 0.0F;
    private float armorHealth;
    private float prevHp;
    private float prevAbs;
    private int targetHurt;

    private static final long TOTEM_FX_DUR = 700L;
    private long totemFxStartMs = -1L;
    private long lastTotemAutoMs = 0L;
    private boolean isTotemAnimating = false;
    private static final float MODERN_TOTEM_R = 4f;
    private static final float MODERN_TOTEM_SCALE_MULT = 0.85f;

    private LivingEntity prevFrameTargetEntity;
    private boolean hasLastDispatchTick = false;

    private final StringIntsAnimator hpStringAnim = new StringIntsAnimator();
    private final int[] lastUpdatedResColors = new int[4];

    private String targetName = "";
    private boolean blurEnabled;
    private boolean flowHudRects;
    private float dragScale = 1.0F;
    private float dragWobbleX = 0.0F;
    private float dragWobbleY = 0.0F;
    private float dragWobbleAngle = 0.0F;

    public boolean isLegacyMode(String mode) {
        if (mode == null) {
            return false;
        }
        for (String key : LEGACY_MODES) {
            if (key.equalsIgnoreCase(mode)) {
                return true;
            }
        }
        return false;
    }

    private boolean isFlowHudModeSafe(HUD hud) {
        if (hud == null) {
            return false;
        }
        String hudModeName = hud.hudMode.get();
        return hudModeName != null && !hudModeName.equalsIgnoreCase("\u041E\u0431\u044B\u0447\u043D\u044B\u0439");
    }

    public void onPacket(EventPacket e) {
        if (mc.world == null) return;
        if (!(e.getPacket() instanceof SEntityStatusPacket)) return;
        SEntityStatusPacket packet = (SEntityStatusPacket) e.getPacket();
        if (packet.getOpCode() != 35) return;
        Entity packetEntity = packet.getEntity(mc.world);
        if (!(packetEntity instanceof LivingEntity)) return;
        LivingEntity livingEntity = (LivingEntity) packetEntity;
        if (curTarget != null && livingEntity.getEntityId() == curTarget.getEntityId()) {
            startTotemAnimation(System.currentTimeMillis());
        }
    }

    public void render(EventDisplay e, HUD hud, Dragging drag) {
        if (mc.player == null || mc.world == null) {
            return;
        }
        flowHudRects = hud != null && !hud.hudMode.is("Р С›Р В±РЎвЂ№РЎвЂЎР Р…РЎвЂ№Р в„–");

        flowHudRects = isFlowHudModeSafe(hud);

        LivingEntity target = resolveTarget(hud);
        updateScale(target);

        if (scale.getAnim() < 0.01F && target == null) {
            curTarget = null;
            clearHeldTarget();
            return;
        }

        if (target != null) {
            if (curTarget == null || curTarget.getEntityId() != target.getEntityId()) {
                particles.clear();
                voronoiEvents.clear();
                prevFrameTargetEntity = null;
                hasLastDispatchTick = false;
                prevHp = 0.0F;
                prevAbs = 0.0F;
                hpRectAnim = getHpRatio(target);
                hpHurtAnim = hpRectAnim;
                absRectAnim = getAbsRatio(target);
                hpStringAnim.reset();
                totemFxStartMs = -1L;
                isTotemAnimating = false;
                lastTotemAutoMs = 0L;
            }
            curTarget = target;
        }

        if (curTarget == null && mc.currentScreen instanceof ChatScreen) {
            curTarget = mc.player;
        }

        if (curTarget == null) {
            return;
        }

        ResourceLocation resolvedSkin = null;
        if (curTarget instanceof AbstractClientPlayerEntity) {
            resolvedSkin = ((AbstractClientPlayerEntity) curTarget).getLocationSkin();
        } else {
            try {
                EntityRenderer<? super Entity> renderer = mc.getRenderManager().getRenderer(curTarget);
                if (renderer != null) {
                    resolvedSkin = renderer.getEntityTexture(curTarget);
                }
            } catch (Exception ignored) {
            }
        }
        skin = resolvedSkin;
        if (skin != null && (oldSkin == null || oldSkin != skin)) {
            oldSkin = skin;
        }

        if (TARGETTING_SFX && soundTarget != target) {
            if (target != null && target != mc.player) {
                SoundUtil.playSound("targetselect.wav");
            }
            soundTarget = target;
        }

        updatePosition(e, hud, drag, curTarget);

        targetName = curTarget.getName().getString();

        if (drag != null) {
            dragScale = drag.getGrabScale();
            dragWobbleX = drag.getWobbleX();
            dragWobbleY = drag.getWobbleY();
            dragWobbleAngle = drag.getWobbleAngle();
        } else {
            dragScale = 1.0F;
            dragWobbleX = 0.0F;
            dragWobbleY = 0.0F;
            dragWobbleAngle = 0.0F;
        }

        renderModern(e, curTarget);

        drag.setWidth(widthHud);
        drag.setHeight(heightHud);

        restoreHudState();
    }

    private LivingEntity resolveTarget(HUD hud) {
        if (mc.currentScreen instanceof ChatScreen) {
            rememberResolvedTarget(mc.player);
            return mc.player;
        }

        LivingEntity aura = PRE_RANGED_TARGET ? Harmony.getInstance().getModuleManager().getHitAura().getTarget() : null;
        if (isUsableTarget(aura)) {
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
        return getHeldTarget();
    }

    private void rememberResolvedTarget(LivingEntity target) {
        if (target == null) {
            return;
        }
        heldTarget = target;
        lastTargetSeenMs = System.currentTimeMillis();
    }

    private LivingEntity getHeldTarget() {
        if (heldTarget == null || heldTarget == mc.player || !isUsableTarget(heldTarget)) {
            return null;
        }
        if (System.currentTimeMillis() - lastTargetSeenMs > TARGET_LOST_HOLD_MS) {
            return null;
        }
        return heldTarget;
    }

    private boolean isUsableTarget(LivingEntity target) {
        return target != null && target.isAlive() && target.getHealth() > 0.0F;
    }

    private void clearHeldTarget() {
        heldTarget = null;
        lastTargetSeenMs = 0L;
    }

    private void updateScale(LivingEntity target) {
        if (target == null && !(mc.currentScreen instanceof ChatScreen)) {
            scale.to = 0.0F;
        } else {
            if (scale.getAnim() < 0.75F) {
                scale.setAnim(0.75F);
            }
            scale.to = 1.0F;
        }
    }

    private float scaleValue() {
        return MathUtil.clamp(scale.getAnim(), 0.0F, 1.0F);
    }

    private void updatePosition(EventDisplay e, HUD hud, Dragging drag, LivingEntity target) {
        float animSpeed = 0.15F;
        float animSpeedCast = 0.08F;

        float baseX = drag.getX();
        float baseY = drag.getY();

        if (!posInit) {
            xPosHud = baseX;
            yPosHud = baseY;
            posInit = true;
        }

        if (drag.isDragging()) {
            xPosHud = baseX;
            yPosHud = baseY;
            return;
        }

        float[] casted = new float[]{-1.0F, -1.0F};
        if (hud.d3.get() && target != null && target != mc.player) {
            casted = castPosition(e, target);
        }

        boolean validCast = casted[0] >= 0.0F && casted[1] >= 0.0F;
        ScaledResolution sr = new ScaledResolution(mc);
        if (validCast
                && casted[0] + widthHud < sr.getScaledWidth()
                && casted[1] + heightHud < sr.getScaledHeight()) {
            xPosHud = lerp(xPosHud, casted[0], animSpeedCast);
            yPosHud = lerp(yPosHud, casted[1], animSpeedCast);
        } else {
            xPosHud = lerp(xPosHud, baseX, animSpeed);
            yPosHud = lerp(yPosHud, baseY, animSpeed);
        }
    }

    private float[] castPosition(EventDisplay e, LivingEntity entity) {
        float xn = -1.0F;
        float yn = -1.0F;
        if (entity == null || entity == mc.player) {
            return new float[]{xn, yn};
        }

        double x = MathUtil.lerp(entity.lastTickPosX, entity.getPosX(), e.getPartialTicks());
        double y = MathUtil.lerp(entity.lastTickPosY, entity.getPosY(), e.getPartialTicks());
        double z = MathUtil.lerp(entity.lastTickPosZ, entity.getPosZ(), e.getPartialTicks());
        double w = entity.getWidth() / 2.0D;
        AxisAlignedBB aabb = new AxisAlignedBB(x - w, y, z - w, x + w, y + entity.getHeight(), z + w);

        Vector2d[] vectors = new Vector2d[]{
                RenderUtility.project2D(aabb.minX, aabb.minY, aabb.minZ),
                RenderUtility.project2D(aabb.minX, aabb.maxY, aabb.minZ),
                RenderUtility.project2D(aabb.maxX, aabb.minY, aabb.minZ),
                RenderUtility.project2D(aabb.maxX, aabb.maxY, aabb.minZ),
                RenderUtility.project2D(aabb.minX, aabb.minY, aabb.maxZ),
                RenderUtility.project2D(aabb.minX, aabb.maxY, aabb.maxZ),
                RenderUtility.project2D(aabb.maxX, aabb.minY, aabb.maxZ),
                RenderUtility.project2D(aabb.maxX, aabb.maxY, aabb.maxZ)
        };

        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -1;
        double maxY = -1;

        for (Vector2d v : vectors) {
            if (v == null) {
                continue;
            }
            if (v.x < minX) minX = v.x;
            if (v.y < minY) minY = v.y;
            if (v.x > maxX) maxX = v.x;
            if (v.y > maxY) maxY = v.y;
        }

        if (minX == Double.MAX_VALUE || maxX < 0) {
            return new float[]{xn, yn};
        }

        xn = (float) (minX + (maxX - minX) / 2.0D) - widthHud / 2.0F;
        yn = (float) minY + heightHud / 2.0F;

        return new float[]{xn, yn};
    }


    private void renderModern(EventDisplay e, LivingEntity target) {
        float a = scaleValue();
        if (a < 0.01F) {
            return;
        }

        MatrixStack ms = e.getMatrixStack();
        float h = 22.0F;
        float headSize = 14.0F;
        float headYOffset = -2.0F;

        float hpValue = Math.max(0.0F, target.getHealth());
        float absValue = Math.max(0.0F, target.getAbsorptionAmount());
        String hpBaseStr = formatHpValue(hpValue) + "hp";
        String hpGoldStr = absValue > 0.0F ? formatHpValue(absValue) : "";
        StyledFont hpFont = ClientFonts.sf_medium[12];
        float nameW = ClientFonts.sf_medium[14].getWidth(targetName);
        float hpTextWBase = hpFont.getWidth(hpBaseStr);
        float hpTextWFull = hpTextWBase
                + (hpGoldStr.isEmpty() ? 0.0F : hpFont.getWidth(" +") + hpFont.getWidth(hpGoldStr));
        float w = MathUtil.clamp(headSize + 12.0F + nameW + hpTextWBase + 16.0F, 110.0F, 240.0F);

        topItems.clear();
        for (ItemStack st : target.getArmorInventoryList()) {
            if (!st.isEmpty()) {
                topItems.add(st);
            }
        }
        ItemStack main = target.getHeldItemMainhand();
        ItemStack off = target.getHeldItemOffhand();
        if (!main.isEmpty()) {
            topItems.add(main);
        }
        if (!off.isEmpty()) {
            topItems.add(off);
        }

        float itemsScale = 0.6F;
        float itemSize = 16.0F * itemsScale;
        float itemGap = 1.5F * itemsScale;
        float itemsRowH = topItems.isEmpty() ? 0.0F : itemSize + 2.0F;

        widthHud = w;
        heightHud = h;

        float x = xPosHud + dragWobbleX;
        float y = yPosHud + dragWobbleY;
        float rectY = y;

        float scaleHud = (0.45F + a * 0.55F) * dragScale;
        float centerX = x + w / 2.0F;
        float centerY = rectY + h / 2.0F;

        GL11.glPushMatrix();
        RenderUtility.customScaledObject2D(x, y, widthHud, heightHud, scaleHud);
        if (Math.abs(dragWobbleAngle) > 0.001F) {
            RenderUtility.customRotatedObject2D(x, y, widthHud, heightHud, dragWobbleAngle);
        }

        if (!topItems.isEmpty() && a > 0.05F) {
            float rowW = topItems.size() * itemSize + Math.max(0, topItems.size() - 1) * itemGap;
            float rowX = x + w - rowW - 4.0F;
            float rowY = rectY - itemsRowH;
            RenderSystem.enableBlend();
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, a);
            drawStacksRow(topItems, rowX, rowY, itemsScale, a);
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        }

        if (blurEnabled) {
            BlurUtils.renderRoundedBlur(x, rectY, w, h, 4.0F);
        }

        if (flowHudRects) {
            int base = ColorUtils.rgba(25, 25, 30, (int) (160 * a));
            int darkLayer = ColorUtils.rgba(0, 0, 0, (int) (120 * a * 0.72f));
            int outline = ColorUtils.rgba(255, 255, 255, (int) (30 * a));
            RenderUtility.drawRoundedRect(x, rectY, w, h, 4.0F, base);
            RenderUtility.drawRoundedRect(x, rectY, w, h, 4.0F, darkLayer);
            RenderUtility.drawRoundedRectOutline(x, rectY, w, h, 4.0F, 0.5F, outline);
        } else {
            int bg = ColorUtils.rgba(0, 0, 0, (int) (120 * a));
            RenderUtility.drawShadow(x, rectY, w, h, 6, ColorUtils.rgba(0, 0, 0, (int) (70 * a)));
            RenderUtility.drawRoundedRect(x, rectY, w, h, 4.0F, bg);
        }

        if (scale.to != 0.0F) {
            float hpRatio = getHpRatio(target);
            hpRectAnim = lerp(hpRectAnim, hpRatio, 0.08F);
            hpHurtAnim = lerp(hpHurtAnim, hpRatio, 0.03F);

            float absRatio = getAbsRatio(target);
            absRectAnim = lerp(absRectAnim, absRatio, 0.06F);
        }

        float barX = x + 3.0F;
        float barY = rectY + h - 5.0F;
        float barW = w - 6.0F;
        RenderUtility.drawRoundedRect(barX, barY, barW, 2.5F, 1.5F, ColorUtils.rgba(10, 10, 10, (int) (120 * a)));

        float hurtBarW = Math.max(2.0F, barW * hpHurtAnim);
        RenderUtility.drawRoundedRect(barX, barY, hurtBarW, 2.5F, 1.5F,
                ColorUtils.reAlphaInt(ColorUtils.rgba(255, 80, 80, 255), (int) (150 * a)));

        float hpBarW = Math.max(2.0F, barW * hpRectAnim);
        RenderUtility.drawRoundedRect(barX, barY, hpBarW, 2.5F, 1.5F,
                ColorUtils.reAlphaInt(Theme.MainColor(0), (int) (220 * a)));

        if (absRectAnim > 0.005F) {
            float absW = Math.max(2.0F, barW * absRectAnim);
            int goldColor = ColorUtils.rgb(255, 220, 80);
            float rotation = (System.currentTimeMillis() % 3600L) * 0.05F;
            RenderUtility.drawRoundedRectWithRotatingGradient(barX, barY, absW, 2.5F, 1.5F,
                    goldColor, rotation, a);
            RenderUtility.drawRoundedRect(barX, barY, absW, 2.5F * 0.3F,
                    new Vector4f(1.5F, 1.5F, 0.0F, 0.0F),
                    ColorUtils.reAlphaInt(ColorUtils.brighter(goldColor, 0.3F), (int) (50 * a)));
        }

        int nameCol = ColorUtils.rgba(255, 255, 255, (int) (255 * a));
        float headX = x + 2.0F;
        float headY = rectY + (h - headSize) / 3F + headYOffset;
        boolean headDrawn = drawHead(headX, headY - 0.7f, headSize, 3.0F, a, target);

        ms.push();
        ms.translate(centerX, centerY, 0.0D);
        ms.scale(scaleHud, scaleHud, 1.0F);
        ms.translate(-centerX, -centerY, 0.0D);

        if (!headDrawn) {
            int qCol = ColorUtils.rgba(255, 255, 255, (int) (255 * a));
            ClientFonts.sf_medium[15].drawString(ms, "?", headX + headSize / 2.0F, headY + headSize / 2.0F - 0.7F, qCol);
        }

        float nameX = headX + headSize + 6.0F;
        float nameY = rectY + 5.0F;
        float hpX = x + w - hpTextWFull - 6.0F;
        float maxNameW = Math.max(0.0F, hpX - nameX - 4.0F);
        drawTextWithFadeTail(ms, ClientFonts.sf_medium[14], targetName, nameX, nameY, nameCol, maxNameW);
        hpFont.drawString(ms, hpBaseStr, hpX, nameY + 1.0F, nameCol);
        if (!hpGoldStr.isEmpty()) {
            float goldY = nameY + 1.0F;
            float goldX = hpX + hpFont.getWidth(hpBaseStr);
            hpFont.drawString(ms, " +", goldX, goldY, nameCol);
            drawGoldGradientText(ms, hpFont, hpGoldStr, goldX + hpFont.getWidth(" +"), goldY, a);
        }
        ms.pop();

        if (totemFxStartMs > 0) {
            long now = System.currentTimeMillis();
            long elapsed = now - totemFxStartMs;
            if (elapsed < TOTEM_FX_DUR) {
                isTotemAnimating = true;
                float t = MathHelper.clamp(elapsed / (float) TOTEM_FX_DUR, 0f, 1f);
                GL11.glEnable(GL11.GL_BLEND);
                drawTotemOverlay(x, rectY, w, h, MODERN_TOTEM_R, a, t, false);
            } else {
                totemFxStartMs = -1L;
                isTotemAnimating = false;
                lastTotemAutoMs = now;
            }
        }

        GL11.glPopMatrix();

        restoreHudState();
    }

    private void startTotemAnimation(long now) {
        if (!isTotemAnimating && now - lastTotemAutoMs >= 2000) {
            totemFxStartMs = now;
            lastTotemAutoMs = now;
            isTotemAnimating = true;
        }
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
            totemScale = (0.3f + easeOutBack(phase1) * 0.8f) * MODERN_TOTEM_SCALE_MULT;
            totemY = easeOutQuad(phase1) * 5f;
            glowIntensity = phase1 * 0.8f;
        } else if (t <= 0.75f) {
            float phase2 = (t - 0.25f) / 0.5f;
            overlayAlpha = 0.85f;
            float pulse = 1f + 0.03f * (float) Math.sin(phase2 * Math.PI * 4);
            totemScale = 1.1f * pulse * MODERN_TOTEM_SCALE_MULT;
            totemY = 5f + (float) Math.sin(phase2 * Math.PI * 2) * 1.5f;
            glowIntensity = 0.8f + 0.2f * (float) Math.sin(phase2 * Math.PI * 3);
        } else {
            float phase3 = (t - 0.75f) / 0.25f;
            overlayAlpha = (1f - easeInQuad(phase3)) * 0.85f;
            totemScale = (1.1f - easeInBack(phase3) * 0.9f) * MODERN_TOTEM_SCALE_MULT;
            totemY = 5f - easeOutCubic(phase3) * 30f;
            glowIntensity = (1f - phase3) * 0.8f;
        }

        float cx = x + w / 2f;
        float cy = y + h / 2f;

        int bgColor = ColorUtils.rgba(12, 12, 16, (int) (overlayAlpha * 240f * a));
        int tone = Theme.RectColor(0);
        bgColor = ColorUtils.interpolateColor(bgColor, tone, 0.06f);

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

    private void restoreHudState() {
        RenderSystem.disableBlend();
        RenderSystem.enableTexture();
        RenderSystem.enableAlphaTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glDisable(GL11.GL_POINT_SMOOTH);
        GL11.glLineWidth(1.0F);
        GL11.glPointSize(1.0F);
    }

    private String formatHpValue(float value) {
        int tenths = Math.round(Math.max(0.0F, value) * 10.0F);
        if (tenths % 10 == 0) {
            return String.valueOf(tenths / 10);
        }
        return (tenths / 10) + "." + (tenths % 10);
    }

    private void drawTextWithFadeTail(MatrixStack ms, StyledFont font, String text, float x, float y, int color, float maxWidth) {
        if (maxWidth <= 0.5F || text == null || text.isEmpty()) {
            return;
        }
        if (font.getWidth(text) <= maxWidth) {
            font.drawString(ms, text, x, y, color);
            return;
        }

        int fadeChars = Math.min(4, text.length());
        float fadeCharsWidth = 0.0F;
        for (int i = text.length() - fadeChars; i < text.length(); i++) {
            fadeCharsWidth += font.getWidth(String.valueOf(text.charAt(i)));
        }
        float fadeWidth = MathHelper.clamp(fadeCharsWidth, 7.0F, Math.max(7.0F, maxWidth - 0.5F));
        float solidWidth = Math.max(0.0F, maxWidth - fadeWidth);
        float currentX = x;
        int baseAlpha = (color >>> 24) & 255;
        if (baseAlpha <= 0) {
            baseAlpha = 255;
        }
        int rgb = color & 0x00FFFFFF;

        for (int i = 0; i < text.length(); i++) {
            String ch = text.substring(i, i + 1);
            float charWidth = font.getWidth(ch);
            float offset = currentX - x;
            if (offset + charWidth > maxWidth + 0.01F) {
                break;
            }

            float center = offset + charWidth * 0.5F;
            float alphaMul = 1.0F;
            if (center > solidWidth) {
                float t = MathHelper.clamp((center - solidWidth) / Math.max(0.01F, fadeWidth), 0.0F, 1.0F);
                alphaMul = 1.0F - t;
            }

            int charAlpha = Math.max(0, Math.min(255, (int) (baseAlpha * alphaMul)));
            if (charAlpha <= 2) {
                break;
            }

            font.drawString(ms, ch, currentX, y, (charAlpha << 24) | rgb);
            currentX += charWidth;
        }
    }

    private float drawGoldGradientText(MatrixStack ms, StyledFont font, String text, float x, float y, float alpha) {
        if (text == null || text.isEmpty()) {
            return 0.0F;
        }
        IFormattableTextComponent gradient = GradientUtil.goldGradient(text);
        float posX = x;
        List<ITextComponent> parts = gradient.getSiblings();
        if (parts == null || parts.isEmpty()) {
            int col = ColorUtils.swapAlpha(ColorUtils.rgb(255, 215, 0), (int) (255 * alpha));
            font.drawString(ms, text, posX, y, col);
            return font.getWidth(text);
        }

        for (ITextComponent part : parts) {
            String ch = part.getString();
            if (ch.isEmpty()) {
                continue;
            }
            int col = 0xFFFFFF;
            if (part.getStyle().getColor() != null) {
                col = part.getStyle().getColor().getColor();
            }
            col = ColorUtils.swapAlpha(col, (int) (255 * alpha));
            font.drawString(ms, ch, posX, y, col);
            posX += font.getWidth(ch);
        }
        return posX - x;
    }


    private boolean drawHead(float x, float y, float size, float radius, float alpha, LivingEntity target) {
        if (target == null) {
            return false;
        }

        RenderSystem.enableTexture();
        RenderSystem.enableBlend();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        float hurtPercent = target.hurtTime > 0 ? 0.4F : 0.0F;
        if (target instanceof PlayerEntity && oldSkin != null) {
            RenderUtility.drawHead(oldSkin, x, y, size, size, radius, alpha, hurtPercent);
            return true;
        }

        return false;
    }

    private void drawItemsRow(LivingEntity target, float x, float y, float available, float alpha, float scale) {
        if (target == null || alpha < 0.02F) {
            return;
        }

        collectItems(target, renderItems);
        if (renderItems.isEmpty()) {
            return;
        }

        float size = 16.0F * scale;
        float gap = 2.0F * scale;
        int max = Math.max(1, (int) Math.floor(available / (size + gap)));
        int count = Math.min(renderItems.size(), max);

        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0.0F);
        GL11.glScalef(scale, scale, 1.0F);

        for (int i = 0; i < count; i++) {
            ItemStack st = renderItems.get(i);
            mc.getItemRenderer().renderItemIntoGUI(st, (int) ((size + gap) * i), 0);
        }

        GL11.glPopMatrix();
    }

    private void drawStacksRow(List<ItemStack> items, float x, float y, float scale, float alpha) {
        if (alpha < 0.02F || items.isEmpty()) {
            return;
        }

        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0.0F);
        GL11.glScalef(scale, scale, 1.0F);
        float step = 16.0F + 1.0F;
        for (int i = 0; i < items.size(); i++) {
            ItemStack st = items.get(i);
            if (st.isEmpty()) {
                continue;
            }
            mc.getItemRenderer().renderItemIntoGUI(st, (int) (step * i), 0);
        }
        GL11.glPopMatrix();
    }

    private void collectItems(LivingEntity target, List<ItemStack> stacks) {
        stacks.clear();
        if (target == null) {
            return;
        }
        ItemStack off = target.getHeldItemOffhand();
        ItemStack main = target.getHeldItemMainhand();
        addRenderableItem(stacks, off);
        addRenderableItem(stacks, main);
        for (ItemStack stack : target.getArmorInventoryList()) {
            addRenderableItem(stacks, stack);
        }
    }

    private void addRenderableItem(List<ItemStack> stacks, ItemStack stack) {
        if (!stack.isEmpty() && !(stack.getItem() instanceof AirItem)) {
            stacks.add(stack);
        }
    }

    private void updateParticles(float x, float y, float alpha, LivingEntity target) {
        if (target == null) {
            particles.clear();
            return;
        }

        if (target.hurtTime > 6 && targetHurt != target.hurtTime) {
            for (int i = 0; i < 3; i++) {
                particles.add(new Particle(x, y));
            }
        }
        targetHurt = target.hurtTime;

        particles.removeIf(p -> p.life <= 0);
        for (Particle p : particles) {
            p.update();
            p.render(alpha);
        }
    }

    private float getHpRatio(LivingEntity target) {
        float hp = Math.max(0.0F, target.getHealth());
        float max = Math.max(1.0F, target.getMaxHealth());
        return MathHelper.clamp(hp / max, 0.0F, 1.0F);
    }

    private float getAbsRatio(LivingEntity target) {
        float abs = Math.max(0.0F, target.getAbsorptionAmount());
        float max = Math.max(1.0F, target.getMaxHealth());
        return MathHelper.clamp(abs / max, 0.0F, 1.0F);
    }

    private float lerp(float from, float to, float speed) {
        return from + (to - from) * speed;
    }

    private float easeOutCubic(float t) {
        t = MathHelper.clamp(t, 0.0F, 1.0F);
        float inv = 1.0F - t;
        return 1.0F - inv * inv * inv;
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

    private float easeInOutQuad(float t) {
        t = MathHelper.clamp(t, 0.0F, 1.0F);
        return t < 0.5F ? 2.0F * t * t : 1.0F - (float) Math.pow(-2.0F * t + 2.0F, 2.0F) / 2.0F;
    }

    private float getArmorPercent01(LivingEntity entity) {
        float armPC = 0.0F;
        for (ItemStack armorElement : entity.getArmorInventoryList()) {
            if (!armorElement.isEmpty()) {
                float maxDurable = (float) armorElement.getMaxDamage();
                if (maxDurable <= 0.0F) {
                    continue;
                }
                float armorHealth = maxDurable - (float) armorElement.getDamage();
                float durablePC = armorHealth / maxDurable;
                armPC += durablePC;
            }
        }
        return MathUtil.clamp(armPC / 4.0F, 0.0F, 1.0F);
    }

    private BufferedImage getBufferedImage(ResourceLocation res) {
        if (res == null) {
            return null;
        }
        try (InputStream inputStream = mc.getResourceManager().getResource(res).getInputStream()) {
            return ImageIO.read(inputStream);
        } catch (Exception ignored) {
            return null;
        }
    }

    private int getColorFromTextureCoord(BufferedImage img, int texX, int texY) {
        return img == null ? 0 : img.getRGB(texX, texY);
    }

    private int[] getColorsFromLastResource(ResourceLocation res, float alphaPC) {
        int[] colors = getColorsFromLastResource(res);
        colors[0] = ColorUtils.swapAlpha(colors[0], (float) ColorUtils.getAlphaFromColor(colors[0]) * alphaPC);
        colors[1] = ColorUtils.swapAlpha(colors[1], (float) ColorUtils.getAlphaFromColor(colors[1]) * alphaPC);
        colors[2] = ColorUtils.swapAlpha(colors[2], (float) ColorUtils.getAlphaFromColor(colors[2]) * alphaPC);
        colors[3] = ColorUtils.swapAlpha(colors[3], (float) ColorUtils.getAlphaFromColor(colors[3]) * alphaPC);
        return colors;
    }

    private int[] getColorsFromLastResource(ResourceLocation res) {
        if (res == null) {
            return new int[]{Theme.MainColor(0), Theme.RectColor(0), Theme.MainColor(90), Theme.RectColor(90)};
        }
        try {
            BufferedImage bufferedImage = getBufferedImage(res);
            if (bufferedImage != null) {
                float texW = bufferedImage.getWidth();
                float texH = bufferedImage.getHeight();
                float defS = 64.0F;
                int[] uPix = new int[]{(int) (8.0F / defS * texW), (int) (15.0F / defS * texW), (int) (15.0F / defS * texW), (int) (8.0F / defS * texW)};
                int[] vPix = new int[]{(int) (8.0F / defS * texH), (int) (8.0F / defS * texH), (int) (15.0F / defS * texH), (int) (15.0F / defS * texH)};
                lastUpdatedResColors[0] = getColorFromTextureCoord(bufferedImage, uPix[0], vPix[0]);
                lastUpdatedResColors[1] = getColorFromTextureCoord(bufferedImage, uPix[1], vPix[1]);
                lastUpdatedResColors[2] = getColorFromTextureCoord(bufferedImage, uPix[2], vPix[2]);
                lastUpdatedResColors[3] = getColorFromTextureCoord(bufferedImage, uPix[3], vPix[3]);
            }
        } catch (Exception ignored) {
        }
        return lastUpdatedResColors;
    }
    private static class Particle {
        float x;
        float y;
        float vx;
        float vy;
        float life = 1.0F;

        Particle(float x, float y) {
            this.x = x;
            this.y = y;
            double angle = Math.random() * Math.PI * 2.0;
            float speed = 0.5F + (float) Math.random() * 0.8F;
            this.vx = (float) Math.cos(angle) * speed;
            this.vy = (float) Math.sin(angle) * speed;
        }

        void update() {
            x += vx;
            y += vy;
            vx *= 0.96F;
            vy *= 0.96F;
            vy += 0.01F;
            life -= 0.025F;
        }

        void render(float alpha) {
            if (life <= 0) {
                return;
            }
            int col = ColorUtils.rgba(255, 140, 140, (int) (180 * alpha * life));
            RenderUtility.drawShadow(x - 2, y - 2, 4, 4, 6, ColorUtils.rgba(255, 80, 80, (int) (60 * alpha * life)));
            RenderUtility.drawCircle(x, y, 1.5F, col);
        }
    }

    private static class NoiseAnimation {
        private final AnimationUtils noiseAnimation = new AnimationUtils(1.0F, 1.0F, 0.0F);
        private float noiseProgress;
        private ResourceLocation resolvedNoise;

        void update(float animationSpeed, boolean toShow) {
            noiseAnimation.to = toShow ? 0.0F : 1.0F;
            if (noiseAnimation.to == 0.0F && noiseAnimation.anim > 0.0F || noiseAnimation.to == 1.0F && noiseAnimation.anim < 1.0F) {
                noiseAnimation.getAnim();
            }
            if (noiseAnimation.to == 1.0F && noiseAnimation.anim > 0.9980392F) {
                noiseAnimation.setAnim(1.0F);
            } else if (noiseAnimation.to == 0.0F && noiseAnimation.anim < 0.003921569F) {
                noiseAnimation.setAnim(0.0F);
            }
            noiseAnimation.speed = animationSpeed;
            noiseProgress = noiseAnimation.anim;
        }

        void insertRender2D(Runnable drawable, ScaledResolution sr, int reduction) {
            if (noiseProgress == 1.0F) {
                return;
            }
            if (noiseProgress == 0.0F) {
                drawable.run();
                return;
            }

            ResourceLocation texture = resolveNoiseTexture();
            if (texture == null) {
                drawable.run();
                return;
            }

            Runnable stencilShape = () -> {
                Minecraft mc = Minecraft.getInstance();
                mc.getTextureManager().bindTexture(texture);
                Tessellator tessellator = Tessellator.getInstance();
                BufferBuilder buffer = tessellator.getBuffer();
                float w = (float) (sr.getScaledWidth() * reduction);
                float h = (float) (sr.getScaledHeight() * reduction);
                int densityIterations = 3;
                h /= (float) densityIterations;
                w /= (float) densityIterations;
                w *= 1.7777778F * (h / w);
                buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
                for (int x = 0; x < densityIterations; ++x) {
                    for (int y = 0; y < densityIterations; ++y) {
                        quad(buffer, w * x, h * y, w * x + w, h * y + h);
                    }
                }

                RenderUtility.glRenderStart();
                GL11.glDisable(GL11.GL_DEPTH_TEST);
                GL11.glDepthRange(0.0D, 0.01D);
                RenderUtility.resetBlender();
                RenderUtility.resetColor();
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GL11.glDisable(GL11.GL_LIGHTING);
                GL11.glEnable(GL11.GL_TEXTURE_2D);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, 32772);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
                GL11.glEnable(GL11.GL_ALPHA_TEST);
                float anim = 1.0F - noiseProgress;
                anim *= anim * anim;
                anim = MathHelper.clamp(anim, 0.0F, 1.0F);
                GL11.glAlphaFunc(GL11.GL_GREATER, anim);
                tessellator.draw();
                GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GL11.glEnable(GL11.GL_DEPTH_TEST);
                GL11.glDepthRange(0.0D, 1.0D);
                RenderUtility.glRenderStop();
            };

            StencilUtil.renderInStencil(stencilShape, drawable, 0);
        }

        private void quad(BufferBuilder buffer, float x, float y, float x2, float y2) {
            buffer.pos(x, y, 0.0D).tex(0.0F, 0.0F).endVertex();
            buffer.pos(x2, y, 0.0D).tex(1.0F, 0.0F).endVertex();
            buffer.pos(x2, y2, 0.0D).tex(1.0F, 1.0F).endVertex();
            buffer.pos(x, y2, 0.0D).tex(0.0F, 1.0F).endVertex();
        }

        private ResourceLocation resolveNoiseTexture() {
            if (resolvedNoise != null) {
                return resolvedNoise;
            }
            Minecraft mc = Minecraft.getInstance();
            try {
                return resolvedNoise;
            } catch (Exception ignored) {
            }

            try {
                int size = 64;
                NativeImage img = new NativeImage(size, size, true);
                Random rnd = new Random(1337L);
                for (int y = 0; y < size; y++) {
                    for (int x = 0; x < size; x++) {
                        int gray = 50 + rnd.nextInt(205);
                        int rgba = (0xFF << 24) | (gray << 16) | (gray << 8) | gray;
                        img.setPixelRGBA(x, y, rgba);
                    }
                }
                DynamicTexture dyn = new DynamicTexture(img);
                resolvedNoise = mc.getTextureManager().getDynamicTextureLocation("legacy_noise", dyn);
                return resolvedNoise;
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    private static class StringIntsAnimator {
        private final List<AnimationUtils> animations = new ArrayList<>();

        void reset() {
            animations.clear();
        }

        void controlAnimations(int numericToString, float animationSpeed) {
            String numericString = Integer.toString(numericToString);
            int lengthSet = numericString.length();
            int[] digits = new int[lengthSet];
            for (int i = 0; i < numericString.length(); ++i) {
                char c = numericString.toCharArray()[i];
                try {
                    digits[i] = Integer.parseInt(String.valueOf(c));
                } catch (Exception ignored) {
                    digits[i] = 0;
                }
            }

            while (animations.size() > lengthSet) {
                animations.remove(animations.size() - 1);
            }

            for (int i = 0; i < lengthSet; ++i) {
                int value = digits[i];
                if (animations.size() < lengthSet) {
                    animations.add(new AnimationUtils(value, value, animationSpeed));
                }
                AnimationUtils current = animations.get(i);
                current.speed = Math.min(animationSpeed * (1.0F + Math.max(Math.abs(current.anim - current.to) - 1.0F, 1.0F)), 1.0F);
                current.to = value;
            }
        }

        void drawString(xd.harm.utils.text.font.styled.StyledFont font, float x, float y, int color, boolean centeredX, float numericPaddingPix) {
            if (animations.isEmpty()) {
                String text = "0";
                if (centeredX) {
                    x -= font.getWidth(text) / 2.0F;
                }
                font.drawString(new MatrixStack(), text, x, y, color);
                return;
            }

            if (centeredX) {
                for (AnimationUtils animation : animations) {
                    String ch = String.valueOf((int) animation.to);
                    x -= font.getWidth(ch) / 2.0F;
                }
            }

            float stepY = font.getFontHeight() + numericPaddingPix;
            float textX = x;
            MatrixStack ms = new MatrixStack();

            for (AnimationUtils animation : animations) {
                int numberCurrent = (int) animation.to;
                float numberCurrentAnim = animation.getAnim();
                if (Math.abs(numberCurrentAnim - numberCurrent) < 0.1F) {
                    animation.setAnim(numberCurrent);
                    numberCurrentAnim = numberCurrent;
                }
                for (int numberOffset = 0; numberOffset < 10; ++numberOffset) {
                    float diff = Math.abs(numberCurrentAnim - numberOffset);
                    if (diff > 0.99F) {
                        continue;
                    }
                    int alphaSet = (int) ((1.0F - diff) * (1.0F - diff) * ColorUtils.getAlphaFromColor(color));
                    float textY = y - stepY * (numberCurrentAnim - numberOffset);
                    if (alphaSet >= 33) {
                        font.drawString(ms, String.valueOf(numberOffset), textX, textY, ColorUtils.swapAlpha(color, alphaSet));
                    }
                }
                float diff = Math.abs(numberCurrentAnim - numberCurrent);
                float widthTo = font.getWidth(String.valueOf(numberCurrent));
                float widthAnim = font.getWidth(String.valueOf((int) numberCurrentAnim));
                textX += lerp(widthTo, widthAnim, Math.min(diff, 1.0F));
            }
        }

        private float lerp(float from, float to, float t) {
            return from + (to - from) * t;
        }
    }

    private static class VoronoiEvent {
        private final VoronoiOfCopyRenderTemp voronoi;
        private final long startMs;
        private final long lifeMs;
        private final float x1;
        private final float y1;
        private final float x2;
        private final float y2;
        private final int points;

        VoronoiEvent(float x1, float y1, float x2, float y2, int points, long lifeMs) {
            this.voronoi = new VoronoiOfCopyRenderTemp(x1, y1, x2, y2, points, true);
            this.lifeMs = lifeMs;
            this.startMs = System.currentTimeMillis();
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.points = points;
        }

        void capture(Runnable render, int repeats) {
            voronoi.genFromCaptureRender2d(x1, y1, x2, y2, render, points, true, repeats);
        }

        float getTimePC01() {
            return MathHelper.clamp((System.currentTimeMillis() - startMs) / (float) lifeMs, 0.0F, 1.0F);
        }

        boolean removeIf() {
            return getTimePC01() >= 1.0F;
        }

        VoronoiOfCopyRenderTemp setupVLAARender(boolean aa, boolean blur) {
            return voronoi.setupVLAARender(aa, blur);
        }

        void renderCapturedSegments2d(boolean temporalMode, int begin, float trans, float rot, float aPC, int color, int tryDraws) {
            voronoi.renderCapturedSegments2d(temporalMode, begin, trans, rot, aPC, color, tryDraws);
        }
    }
}
