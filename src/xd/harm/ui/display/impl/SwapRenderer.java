package xd.harm.ui.display.impl;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.vector.Vector4f;
import net.minecraft.util.text.StringTextComponent;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import xd.harm.Harmony;
import xd.harm.modules.impl.combat.AutoSwap;
import xd.harm.modules.impl.render.Theme;
import xd.harm.utils.animations.Animation;
import xd.harm.utils.animations.Direction;
import xd.harm.utils.animations.impl.DecelerateAnimation;
import xd.harm.utils.animations.impl.EaseBackIn;
import xd.harm.utils.animations.impl.EaseInOutQuad;
import xd.harm.utils.client.IMinecraft;
import xd.harm.utils.render.BlurUtils;
import xd.harm.utils.render.color.ColorUtils;
import xd.harm.utils.render.font.Fonts;
import xd.harm.utils.render.rect.RenderUtility;
import xd.harm.utils.text.font.ClientFonts;

public class SwapRenderer extends Screen implements IMinecraft {

    private static final int RADIUS = 70;
    private static final int INNER_RADIUS = 25;
    private static final int SEGMENT_COUNT = 3;
    private static final int MOUSE_LIMIT_RADIUS = RADIUS + 17;

    private final Animation openAnimation;
    private final Animation closeAnimation;
    private final Animation[] slotAnimations = new Animation[3];
    private final Animation inventoryAnimation;

    private boolean inventoryMode = false;
    private boolean closing = false;
    private boolean closingAll = false;
    private boolean forceClose = false;
    private static final int INV_SLOT_SIZE = 18;
    private static final int INV_COLS = 9;
    private static final int INV_ROWS = 4;

    private int bindKey = -1;
    private boolean keyWasReleased = false;

    private float smoothWheelAlpha = 0f;
    private float smoothInvAlpha = 0f;
    private float smoothCloseAlpha = 1f;
    private float[] smoothSlotAnim = new float[]{0f, 0f, 0f};
    private long lastRenderTime = System.currentTimeMillis();

    private int clampedMouseX;
    private int clampedMouseY;

    private boolean showDeleteEffect = false;
    private ItemStack deleteItem = ItemStack.EMPTY;
    private float deleteAnimProgress = 0f;
    private float deleteX = 0f;
    private float deleteY = 0f;
    private int deleteSlotIndex = -1;

    public SwapRenderer(int key) {
        super(new StringTextComponent(""));
        this.bindKey = key;
        this.keyWasReleased = false;

        openAnimation = new EaseBackIn(350, 1, 1.2f);
        openAnimation.reset();
        openAnimation.setDirection(Direction.FORWARDS);

        closeAnimation = new EaseInOutQuad(250, 1);
        closeAnimation.reset();
        closeAnimation.setDirection(Direction.FORWARDS);

        for (int i = 0; i < 3; i++) {
            slotAnimations[i] = new DecelerateAnimation(200, 1);
            slotAnimations[i].reset();
            slotAnimations[i].setDirection(Direction.BACKWARDS);
        }

        inventoryAnimation = new EaseInOutQuad(250, 1);
        inventoryAnimation.reset();
        inventoryAnimation.setDirection(Direction.BACKWARDS);
    }

    public static void open(int key) {
        AutoSwap.wheelMenuOpen = true;
        mc.displayGuiScreen(new SwapRenderer(key));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void startDeleteEffect(ItemStack item, int slotIndex, float x, float y) {
        deleteItem = item.copy();
        deleteSlotIndex = slotIndex;
        deleteX = x;
        deleteY = y;
        deleteAnimProgress = 0f;
        showDeleteEffect = true;
    }

    private void updateDeleteEffect(float deltaTime) {
        if (showDeleteEffect) {
            deleteAnimProgress += deltaTime * 0.03f;
            if (deleteAnimProgress >= 1f) {
                deleteAnimProgress = 1f;
            }
            if (deleteAnimProgress >= 1f && deleteItem != ItemStack.EMPTY) {
                deleteAnimProgress += deltaTime * 0.05f;
                if (deleteAnimProgress >= 1.2f) {
                    showDeleteEffect = false;
                    deleteItem = ItemStack.EMPTY;
                    deleteSlotIndex = -1;
                }
            }
        }
    }

    private void checkKeyHoldState() {
        if (forceClose || inventoryMode || closing) return;
        if (bindKey == -1) return;

        boolean keyPressed = GLFW.glfwGetKey(mc.getMainWindow().getHandle(), bindKey) == GLFW.GLFW_PRESS;

        if (!keyPressed && !keyWasReleased) {
            keyWasReleased = true;
            onKeyReleased();
        }
    }

    private void onKeyReleased() {
        if (AutoSwap.hoveredSlot >= 0 && AutoSwap.hoveredSlot < 3) {
            if (!AutoSwap.threeItems[AutoSwap.hoveredSlot].isEmpty()) {
                AutoSwap autoSwap = Harmony.getInstance().getModuleManager().getAutoSwap();
                if (autoSwap != null) {
                    autoSwap.performThreeItemSwap(AutoSwap.threeItems[AutoSwap.hoveredSlot]);
                }
            }
        }
        initiateCloseAll();
    }

    @Override
    public void tick() {
    }

    private void initiateCloseAll() {
        if (closing || forceClose) return;

        closing = true;
        closingAll = true;
        closeAnimation.reset();
        closeAnimation.setDirection(Direction.BACKWARDS);
        openAnimation.setDirection(Direction.BACKWARDS);
        inventoryAnimation.setDirection(Direction.BACKWARDS);

        for (Animation anim : slotAnimations) {
            anim.setDirection(Direction.BACKWARDS);
        }
    }

    private void initiateCloseInventoryOnly() {
        if (closing) return;
        inventoryAnimation.setDirection(Direction.BACKWARDS);
    }

    public void closeScreen() {
        initiateCloseAll();
    }

    private void finalizeClose() {
        forceClose = true;
        AutoSwap.wheelMenuOpen = false;
        AutoSwap.hoveredSlot = -1;
        AutoSwap.selectingItem = false;
        AutoSwap.selectingSlotIndex = -1;
        inventoryMode = false;
        bindKey = -1;
        keyWasReleased = false;
        mc.displayGuiScreen(null);
    }

    private float lerp(float current, float target, float speed) {
        if (Math.abs(current - target) < 0.001f) return target;
        return current + (target - current) * speed;
    }

    private void clampMouseToCircle(int mouseX, int mouseY, int centerX, int centerY) {
        if (inventoryMode) {
            clampedMouseX = mouseX;
            clampedMouseY = mouseY;
            return;
        }

        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance > MOUSE_LIMIT_RADIUS) {
            double angle = Math.atan2(dy, dx);
            clampedMouseX = (int) (centerX + Math.cos(angle) * MOUSE_LIMIT_RADIUS);
            clampedMouseY = (int) (centerY + Math.sin(angle) * MOUSE_LIMIT_RADIUS);

            double scaleFactor = mc.getMainWindow().getGuiScaleFactor();
            GLFW.glfwSetCursorPos(
                    mc.getMainWindow().getHandle(),
                    clampedMouseX * scaleFactor,
                    clampedMouseY * scaleFactor
            );
        } else {
            clampedMouseX = mouseX;
            clampedMouseY = mouseY;
        }
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        checkKeyHoldState();

        int centerX = width / 2;
        int centerY = height / 2;

        clampMouseToCircle(mouseX, mouseY, centerX, centerY);

        long currentTime = System.currentTimeMillis();
        float deltaTime = Math.min((currentTime - lastRenderTime) / 16.67f, 3f);
        lastRenderTime = currentTime;

        updateDeleteEffect(deltaTime);

        float targetWheelAlpha = (float) openAnimation.getOutput();
        float targetCloseAlpha = closing ? (float) closeAnimation.getOutput() : 1f;
        float targetInvAlpha = (float) inventoryAnimation.getOutput();

        smoothWheelAlpha = lerp(smoothWheelAlpha, targetWheelAlpha, 0.2f * deltaTime);
        smoothCloseAlpha = lerp(smoothCloseAlpha, targetCloseAlpha, 0.25f * deltaTime);
        smoothInvAlpha = lerp(smoothInvAlpha, targetInvAlpha, 0.2f * deltaTime);

        if (closing && closingAll && smoothCloseAlpha < 0.01f && closeAnimation.finished(Direction.BACKWARDS)) {
            finalizeClose();
            return;
        }

        if (inventoryMode && smoothInvAlpha < 0.01f && inventoryAnimation.getDirection() == Direction.BACKWARDS) {
            inventoryMode = false;
            AutoSwap.selectingItem = false;
            AutoSwap.selectingSlotIndex = -1;
        }

        for (int i = 0; i < 3; i++) {
            float target = (float) slotAnimations[i].getOutput();
            smoothSlotAnim[i] = lerp(smoothSlotAnim[i], target, 0.25f * deltaTime);
        }

        float finalAlpha = smoothCloseAlpha;
        float rotationOffset = closing ? (1f - smoothCloseAlpha) * 120f : (1f - smoothWheelAlpha) * -120f;

        if (!closingAll || smoothCloseAlpha > 0.01f) {
            float wheelAlpha = smoothWheelAlpha * finalAlpha;
            if (inventoryMode || smoothInvAlpha > 0.01f) {
                wheelAlpha *= (1f - smoothInvAlpha * 0.5f);
            }
            renderWheelMode(matrixStack, clampedMouseX, clampedMouseY, centerX, centerY, wheelAlpha, rotationOffset);
        }

        if ((inventoryMode || smoothInvAlpha > 0.01f) && !closingAll) {
            renderInventoryMode(matrixStack, mouseX, mouseY, smoothInvAlpha);
        }

        if (showDeleteEffect && !deleteItem.isEmpty()) {
            renderDeleteEffect(matrixStack, centerX, centerY);
        }
    }

    private void renderDeleteEffect(MatrixStack matrixStack, int centerX, int centerY) {
        float progress = Math.min(deleteAnimProgress, 1f);

        float easeOut = progress * progress * (3.0f - 2.0f * progress);

        float scale;
        if (progress < 0.7f) {
            scale = 1f - easeOut * 0.3f;
        } else {
            float endProgress = (progress - 0.7f) / 0.3f;
            scale = 0.7f - endProgress * 0.65f;
        }

        float alpha;
        if (progress < 0.5f) {
            alpha = 1f;
        } else if (progress < 0.9f) {
            float fadeProgress = (progress - 0.5f) / 0.4f;
            alpha = 1f - fadeProgress * 0.7f;
        } else {
            float endFade = (progress - 0.9f) / 0.1f;
            alpha = 0.3f - endFade * 0.3f;
        }

        float rotation = easeOut * 270f;

        double segmentAngle = (Math.PI * 2) / SEGMENT_COUNT;
        double startAngle = -Math.PI / 2 + deleteSlotIndex * segmentAngle - segmentAngle / 2;
        double midAngle = startAngle + segmentAngle / 2;

        float pullX = (float) (centerX + Math.cos(midAngle) * (INNER_RADIUS - 10));
        float pullY = (float) (centerY + Math.sin(midAngle) * (INNER_RADIUS - 10));

        float smoothProgress = (float) Math.sin(easeOut * Math.PI / 2);
        float currentX = deleteX + (pullX - deleteX) * smoothProgress;
        float currentY = deleteY + (pullY - deleteY) * smoothProgress;

        if (alpha > 0.001f && scale > 0.001f) {
            GL11.glPushMatrix();
            GL11.glTranslatef(currentX, currentY, 0);
            GL11.glRotatef(rotation, 0, 0, 1);
            GL11.glScalef(scale, scale, 1f);
            GL11.glTranslatef(-8, -8, 0);

            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            float redTint = 1f;
            float greenTint = 1f - easeOut * 0.7f;
            float blueTint = 1f - easeOut * 0.7f;

            GlStateManager.color4f(redTint, greenTint, blueTint, alpha);
            mc.getItemRenderer().renderItemAndEffectIntoGUI(deleteItem, 0, 0);
            GlStateManager.color4f(1f, 1f, 1f, 1f);
            GlStateManager.disableBlend();

            GL11.glPopMatrix();
        }
    }

    private void renderWheelMode(MatrixStack matrixStack, int mouseX, int mouseY, int centerX, int centerY, float alpha, float rotation) {
        if (alpha <= 0.001f) return;

        float scale = 0.85f + alpha * 0.15f;
        float itemAlphaThreshold = 0.15f;

        double dx = mouseX - centerX;
        double dy = mouseY - centerY;
        double distance = Math.sqrt(dx * dx + dy * dy);
        double angle = Math.atan2(dy, dx);
        if (angle < 0) angle += Math.PI * 2;

        if (!closing && !inventoryMode) {
            AutoSwap.hoveredSlot = -1;
            if (distance > INNER_RADIUS * scale && distance < (RADIUS + 20) * scale) {
                double segmentAngle = (Math.PI * 2) / SEGMENT_COUNT;
                double startOffset = -Math.PI / 2 - segmentAngle / 2;
                double adjustedAngle = angle - startOffset;
                if (adjustedAngle < 0) adjustedAngle += Math.PI * 2;
                if (adjustedAngle >= Math.PI * 2) adjustedAngle -= Math.PI * 2;
                AutoSwap.hoveredSlot = (int) (adjustedAngle / segmentAngle);
                if (AutoSwap.hoveredSlot >= SEGMENT_COUNT) AutoSwap.hoveredSlot = 0;
            }

            for (int i = 0; i < 3; i++) {
                slotAnimations[i].setDirection(AutoSwap.hoveredSlot == i ? Direction.FORWARDS : Direction.BACKWARDS);
            }
        }

        GL11.glPushMatrix();
        GL11.glTranslatef(centerX, centerY, 0);
        GL11.glRotatef(rotation, 0, 0, 1);
        GL11.glScalef(scale, scale, 1f);
        GL11.glTranslatef(-centerX, -centerY, 0);

        RenderUtility.drawCircle(centerX, centerY, (RADIUS + 18) * 2,
                ColorUtils.rgba(30, 30, 38, (int)(235 * alpha)));

        RenderUtility.drawRingArcAA(centerX, centerY, RADIUS + 18, 1.5f, 0f, 360f,
                ColorUtils.rgba(70, 70, 85, (int)(200 * alpha)), 1.5f);

        RenderUtility.drawCircle(centerX, centerY, (INNER_RADIUS + 2) * 2,
                ColorUtils.rgba(40, 40, 50, (int)(240 * alpha)));

        RenderUtility.drawRingArcAA(centerX, centerY, INNER_RADIUS + 2, 1.5f, 0f, 360f,
                ColorUtils.rgba(80, 80, 100, (int)(180 * alpha)), 1.5f);

        for (int i = 0; i < SEGMENT_COUNT; i++) {
            double segmentAngle = (Math.PI * 2) / SEGMENT_COUNT;
            double startAngle = -Math.PI / 2 + i * segmentAngle - segmentAngle / 2;
            double endAngle = startAngle + segmentAngle;

            boolean isEmpty = AutoSwap.threeItems[i].isEmpty();
            float hoverAnim = smoothSlotAnim[i];

            int baseColor;
            int borderColor;

            if (isEmpty) {
                baseColor = ColorUtils.interpolateColor(
                        ColorUtils.rgba(45, 45, 55, (int)(180 * alpha)),
                        ColorUtils.rgba(60, 55, 80, (int)(210 * alpha)),
                        hoverAnim
                );
                borderColor = ColorUtils.interpolateColor(
                        ColorUtils.rgba(80, 80, 95, (int)(180 * alpha)),
                        ColorUtils.reAlphaInt(Theme.MainColor(0), (int)(220 * alpha)),
                        hoverAnim
                );
            } else {
                baseColor = ColorUtils.interpolateColor(
                        ColorUtils.rgba(50, 50, 65, (int)(190 * alpha)),
                        ColorUtils.rgba(65, 60, 90, (int)(220 * alpha)),
                        hoverAnim
                );
                borderColor = ColorUtils.interpolateColor(
                        ColorUtils.rgba(90, 90, 110, (int)(190 * alpha)),
                        ColorUtils.reAlphaInt(Theme.MainColor(0), (int)(230 * alpha)),
                        hoverAnim
                );
            }

            int innerR = INNER_RADIUS + 5 + (int)(3 * hoverAnim);
            int outerR = RADIUS + 12 + (int)(4 * hoverAnim);

            float startDeg = (float) Math.toDegrees(startAngle + 0.04);
            float endDeg = (float) Math.toDegrees(endAngle - 0.04);

            RenderUtility.drawRingArcAA(centerX, centerY, (innerR + outerR) / 2f,
                    (outerR - innerR) / 2f, startDeg, endDeg, baseColor, 1.2f);

            float borderThickness = 1.2f + hoverAnim * 0.5f;
            RenderUtility.drawRingArcAA(centerX, centerY, innerR, borderThickness,
                    startDeg, endDeg, borderColor, 1.5f);
            RenderUtility.drawRingArcAA(centerX, centerY, outerR, borderThickness,
                    startDeg, endDeg, borderColor, 1.5f);

            double midAngle = startAngle + segmentAngle / 2;
            double itemRadius = (innerR + outerR) / 2.0;
            int itemX = (int) (centerX + Math.cos(midAngle) * itemRadius - 8);
            int itemY = (int) (centerY + Math.sin(midAngle) * itemRadius - 8);

            boolean isBeingDeleted = showDeleteEffect && deleteSlotIndex == i;

            if (!AutoSwap.threeItems[i].isEmpty() && alpha > itemAlphaThreshold && !isBeingDeleted) {
                float itemScale = 1f + 0.1f * hoverAnim;
                float itemAlpha = (alpha - itemAlphaThreshold) / (1f - itemAlphaThreshold);
                itemAlpha = Math.min(itemAlpha, 1f);

                GL11.glPushMatrix();
                GL11.glTranslatef(itemX + 8, itemY + 8, 0);
                GL11.glScalef(itemScale, itemScale, 1f);
                GL11.glTranslatef(-8, -8, 0);

                GlStateManager.enableBlend();
                GlStateManager.color4f(1f, 1f, 1f, itemAlpha);
                mc.getItemRenderer().renderItemAndEffectIntoGUI(AutoSwap.threeItems[i], 0, 0);
                mc.getItemRenderer().renderItemOverlays(mc.fontRenderer, AutoSwap.threeItems[i], 0, 0);
                GlStateManager.color4f(1f, 1f, 1f, 1f);
                GL11.glPopMatrix();
            } else if (isEmpty || isBeingDeleted) {
                int plusX = (int) (centerX + Math.cos(midAngle) * itemRadius);
                int plusY = (int) (centerY + Math.sin(midAngle) * itemRadius);
                float s = 5 + 2 * hoverAnim;
                float thickness = 2f + hoverAnim * 0.5f;

                float plusAlpha = 1f;
                if (isBeingDeleted) {
                    plusAlpha = Math.min(deleteAnimProgress * 2f, 1f);
                }

                int plusColor = ColorUtils.interpolateColor(
                        ColorUtils.rgba(100, 100, 115, (int)(180 * alpha * plusAlpha)),
                        ColorUtils.reAlphaInt(Theme.MainColor(0), (int)(230 * alpha * plusAlpha)),
                        hoverAnim
                );

                GL11.glPushMatrix();
                GL11.glTranslatef(plusX, plusY, 0);
                GL11.glRotatef(45f * hoverAnim, 0, 0, 1);

                RenderUtility.drawRoundedRect(-s, -thickness/2, s * 2, thickness, thickness/2, plusColor);
                RenderUtility.drawRoundedRect(-thickness/2, -s, thickness, s * 2, thickness/2, plusColor);

                GL11.glPopMatrix();
            }

            String label = String.valueOf(i + 1);
            int labelColor = ColorUtils.interpolateColor(
                    ColorUtils.rgba(130, 130, 145, (int)(220 * alpha)),
                    ColorUtils.reAlphaInt(Theme.MainColor(0), (int)(255 * alpha)),
                    hoverAnim * 0.6f
            );
            int labelX = (int) (centerX + Math.cos(midAngle) * (RADIUS + 32));
            int labelY = (int) (centerY + Math.sin(midAngle) * (RADIUS + 32));

            ClientFonts.sf_medium[14].drawCenteredString(matrixStack, label, labelX, labelY - 3, labelColor);
        }

        GL11.glPopMatrix();

        if (AutoSwap.hoveredSlot >= 0 && AutoSwap.hoveredSlot < 3 && !closing && !inventoryMode) {
            String hint;
            if (AutoSwap.threeItems[AutoSwap.hoveredSlot].isEmpty()) {
                hint = "ЛКМ - выбрать";
            } else {
                hint = "ПКМ - удалить | Отпустить - свап";
            }
            int hintColor = ColorUtils.rgba(180, 180, 195, (int)(240 * alpha));
            float hintWidth = Fonts.sfui.getWidth(hint, 7);
            Fonts.sfui.drawText(matrixStack, hint, centerX - hintWidth / 2, centerY + (int)((RADIUS + 42) * scale), hintColor, 7);

            if (!AutoSwap.threeItems[AutoSwap.hoveredSlot].isEmpty()) {
                String itemName = AutoSwap.threeItems[AutoSwap.hoveredSlot].getDisplayName().getString();
                int nameColor = ColorUtils.reAlphaInt(Theme.MainColor(0), (int)(240 * alpha));
                float nameWidth = Fonts.sfui.getWidth(itemName, 8);
                Fonts.sfui.drawText(matrixStack, itemName, centerX - nameWidth / 2, centerY + (int)((RADIUS + 55) * scale), nameColor, 8);
            }
        }
    }

    private void renderInventoryMode(MatrixStack matrixStack, int mouseX, int mouseY, float alpha) {
        if (alpha <= 0.001f) return;

        float itemAlphaThreshold = 0.15f;

        int invWidth = INV_COLS * INV_SLOT_SIZE + 16;
        int invHeight = INV_ROWS * INV_SLOT_SIZE + 16;
        int invX = (width - invWidth) / 2;
        int invY = (height - invHeight) / 2;

        float scale = 0.7f + alpha * 0.3f;
        float rotation = (1f - alpha) * -15f;

        GL11.glPushMatrix();
        GL11.glTranslatef(width / 2f, height / 2f, 0);
        GL11.glRotatef(rotation, 0, 0, 1);
        GL11.glScalef(scale, scale, 1f);
        GL11.glTranslatef(-width / 2f, -height / 2f, 0);

        BlurUtils.renderRoundedBlur(invX - 3, invY - 3, invWidth + 6, invHeight + 6, 6f);

        int bgColor = ColorUtils.rgba(35, 35, 45, (int)(240 * alpha));
        RenderUtility.drawRoundedRect(invX - 3, invY - 3, invWidth + 6, invHeight + 6,
                new Vector4f(6, 6, 6, 6), bgColor);

        RenderUtility.drawRoundedOutline(invX - 3, invY - 3, invX + invWidth + 3, invY + invHeight + 3, 6, 1f,
                ColorUtils.rgba(70, 70, 85, (int)(180 * alpha)));

        int startX = invX + 8;
        int startY = invY + 8;

        int hoveredSlotIndex = -1;
        boolean hoveredOffhand = false;

        float itemAlpha = alpha > itemAlphaThreshold ? (alpha - itemAlphaThreshold) / (1f - itemAlphaThreshold) : 0f;
        itemAlpha = Math.min(itemAlpha, 1f);

        for (int row = 0; row < INV_ROWS; row++) {
            for (int col = 0; col < INV_COLS; col++) {
                int slotIndex = row == 3 ? col : 9 + row * 9 + col;
                int slotX = startX + col * INV_SLOT_SIZE;
                int slotY = startY + row * INV_SLOT_SIZE;

                ItemStack stack = mc.player.inventory.getStackInSlot(slotIndex);
                boolean isHovered = mouseX >= slotX && mouseX < slotX + INV_SLOT_SIZE &&
                        mouseY >= slotY && mouseY < slotY + INV_SLOT_SIZE;

                if (isHovered) hoveredSlotIndex = slotIndex;

                int slotColor = isHovered ?
                        ColorUtils.rgba(60, 55, 80, (int)(220 * alpha)) :
                        ColorUtils.rgba(45, 45, 55, (int)(190 * alpha));

                RenderUtility.drawRoundedRect(slotX, slotY, INV_SLOT_SIZE - 2, INV_SLOT_SIZE - 2,
                        new Vector4f(2, 2, 2, 2), slotColor);

                if (isHovered) {
                    RenderUtility.drawRoundedOutline(slotX - 0.5f, slotY - 0.5f,
                            slotX + INV_SLOT_SIZE - 1.5f, slotY + INV_SLOT_SIZE - 1.5f, 2, 1f,
                            ColorUtils.reAlphaInt(Theme.MainColor(0), (int)(220 * alpha)));
                }

                if (!stack.isEmpty() && itemAlpha > 0.01f) {
                    GlStateManager.enableBlend();
                    GlStateManager.color4f(1f, 1f, 1f, itemAlpha);
                    mc.getItemRenderer().renderItemAndEffectIntoGUI(stack, slotX + 1, slotY + 1);
                    mc.getItemRenderer().renderItemOverlays(mc.fontRenderer, stack, slotX + 1, slotY + 1);
                    GlStateManager.color4f(1f, 1f, 1f, 1f);
                }
            }
        }

        int offhandX = invX - INV_SLOT_SIZE - 8;
        int offhandY = invY + (invHeight - INV_SLOT_SIZE) / 2;

        ItemStack offhandStack = mc.player.getHeldItemOffhand();
        boolean isOffhandHovered = mouseX >= offhandX && mouseX < offhandX + INV_SLOT_SIZE &&
                mouseY >= offhandY && mouseY < offhandY + INV_SLOT_SIZE;

        if (isOffhandHovered) hoveredOffhand = true;

        int offhandSlotColor = isOffhandHovered ?
                ColorUtils.rgba(60, 55, 80, (int)(220 * alpha)) :
                ColorUtils.rgba(50, 45, 60, (int)(200 * alpha));

        RenderUtility.drawRoundedRect(offhandX, offhandY, INV_SLOT_SIZE - 2, INV_SLOT_SIZE - 2,
                new Vector4f(2, 2, 2, 2), offhandSlotColor);

        int offhandBorderColor = isOffhandHovered ?
                ColorUtils.reAlphaInt(Theme.MainColor(0), (int)(220 * alpha)) :
                ColorUtils.rgba(80, 75, 100, (int)(160 * alpha));

        RenderUtility.drawRoundedOutline(offhandX - 0.5f, offhandY - 0.5f,
                offhandX + INV_SLOT_SIZE - 1.5f, offhandY + INV_SLOT_SIZE - 1.5f, 2, 1f, offhandBorderColor);

        if (!offhandStack.isEmpty() && itemAlpha > 0.01f) {
            GlStateManager.enableBlend();
            GlStateManager.color4f(1f, 1f, 1f, itemAlpha);
            mc.getItemRenderer().renderItemAndEffectIntoGUI(offhandStack, offhandX + 1, offhandY + 1);
            mc.getItemRenderer().renderItemOverlays(mc.fontRenderer, offhandStack, offhandX + 1, offhandY + 1);
            GlStateManager.color4f(1f, 1f, 1f, 1f);
        }

        int labelColor = ColorUtils.rgba(100, 100, 115, (int)(180 * alpha));
        String offLabel = "OFF";
        float offWidth = Fonts.sfui.getWidth(offLabel, 6);
        Fonts.sfui.drawText(matrixStack, offLabel, offhandX + (INV_SLOT_SIZE - 2) / 2f - offWidth / 2, offhandY + INV_SLOT_SIZE + 2, labelColor, 6);

        if (hoveredSlotIndex >= 0) {
            ItemStack hoveredStack = mc.player.inventory.getStackInSlot(hoveredSlotIndex);
            if (!hoveredStack.isEmpty()) {
                renderTooltip(matrixStack, hoveredStack, mouseX, mouseY);
            }
        } else if (hoveredOffhand && !offhandStack.isEmpty()) {
            renderTooltip(matrixStack, offhandStack, mouseX, mouseY);
        }

        String hint = "ЛКМ - выбрать | ESC - назад";
        int hintColor = ColorUtils.rgba(170, 170, 185, (int)(240 * alpha));
        float hintWidth = Fonts.sfui.getWidth(hint, 7);
        Fonts.sfui.drawText(matrixStack, hint, width / 2f - hintWidth / 2, invY + invHeight + 15, hintColor, 7);

        GL11.glPopMatrix();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (closing || forceClose) return false;

        if (inventoryMode) {
            return handleInventoryClick((int) mouseX, (int) mouseY, button);
        } else {
            return handleWheelClick(button);
        }
    }

    private boolean handleWheelClick(int button) {
        if (AutoSwap.hoveredSlot < 0 || AutoSwap.hoveredSlot >= 3) return false;

        if (showDeleteEffect && deleteSlotIndex == AutoSwap.hoveredSlot && !AutoSwap.threeItems[AutoSwap.hoveredSlot].isEmpty()) {
            return false;
        }

        if (button == 0 && AutoSwap.threeItems[AutoSwap.hoveredSlot].isEmpty()) {
            AutoSwap.selectingItem = true;
            AutoSwap.selectingSlotIndex = AutoSwap.hoveredSlot;
            inventoryMode = true;
            inventoryAnimation.reset();
            inventoryAnimation.setDirection(Direction.FORWARDS);
            return true;
        } else if (button == 1 && !AutoSwap.threeItems[AutoSwap.hoveredSlot].isEmpty()) {
            ItemStack clearedItem = AutoSwap.threeItems[AutoSwap.hoveredSlot].copy();
            int clearedSlot = AutoSwap.hoveredSlot;

            int centerX = width / 2;
            int centerY = height / 2;
            double segmentAngle = (Math.PI * 2) / SEGMENT_COUNT;
            double startAngle = -Math.PI / 2 + clearedSlot * segmentAngle - segmentAngle / 2;
            double midAngle = startAngle + segmentAngle / 2;
            int innerR = INNER_RADIUS + 5;
            int outerR = RADIUS + 12;
            double itemRadius = (innerR + outerR) / 2.0;
            float itemX = (float) (centerX + Math.cos(midAngle) * itemRadius);
            float itemY = (float) (centerY + Math.sin(midAngle) * itemRadius);

            startDeleteEffect(clearedItem, clearedSlot, itemX, itemY);
            AutoSwap.clearThreeItem(AutoSwap.hoveredSlot);
            return true;
        }

        return false;
    }

    private boolean handleInventoryClick(int mouseX, int mouseY, int button) {
        if (button != 0) return false;

        int invWidth = INV_COLS * INV_SLOT_SIZE + 16;
        int invHeight = INV_ROWS * INV_SLOT_SIZE + 16;
        int invX = (width - invWidth) / 2;
        int invY = (height - invHeight) / 2;
        int startX = invX + 8;
        int startY = invY + 8;

        for (int row = 0; row < INV_ROWS; row++) {
            for (int col = 0; col < INV_COLS; col++) {
                int slotIndex = row == 3 ? col : 9 + row * 9 + col;
                int slotX = startX + col * INV_SLOT_SIZE;
                int slotY = startY + row * INV_SLOT_SIZE;

                if (mouseX >= slotX && mouseX < slotX + INV_SLOT_SIZE &&
                        mouseY >= slotY && mouseY < slotY + INV_SLOT_SIZE) {
                    ItemStack stack = mc.player.inventory.getStackInSlot(slotIndex);
                    if (!stack.isEmpty()) {
                        int targetSlot = AutoSwap.selectingSlotIndex;

                        if (showDeleteEffect && deleteSlotIndex == targetSlot) {
                            showDeleteEffect = false;
                            deleteItem = ItemStack.EMPTY;
                            deleteSlotIndex = -1;
                            deleteAnimProgress = 0f;
                        }

                        AutoSwap.setThreeItem(targetSlot, stack);
                        AutoSwap.selectingItem = false;
                        AutoSwap.selectingSlotIndex = -1;
                        initiateCloseInventoryOnly();
                        return true;
                    }
                }
            }
        }

        int offhandX = invX - INV_SLOT_SIZE - 8;
        int offhandY = invY + (invHeight - INV_SLOT_SIZE) / 2;

        if (mouseX >= offhandX && mouseX < offhandX + INV_SLOT_SIZE &&
                mouseY >= offhandY && mouseY < offhandY + INV_SLOT_SIZE) {
            ItemStack offhandStack = mc.player.getHeldItemOffhand();
            if (!offhandStack.isEmpty()) {
                int targetSlot = AutoSwap.selectingSlotIndex;
                if (showDeleteEffect && deleteSlotIndex == targetSlot) {
                    showDeleteEffect = false;
                    deleteItem = ItemStack.EMPTY;
                    deleteSlotIndex = -1;
                    deleteAnimProgress = 0f;
                }

                AutoSwap.setThreeItem(targetSlot, offhandStack);
                AutoSwap.selectingItem = false;
                AutoSwap.selectingSlotIndex = -1;
                initiateCloseInventoryOnly();
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (closing || forceClose) return false;

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (inventoryMode) {
                initiateCloseInventoryOnly();
                return true;
            } else {
                initiateCloseAll();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        if (!forceClose && !closing) {
            initiateCloseAll();
        } else if (forceClose) {
            AutoSwap.wheelMenuOpen = false;
            AutoSwap.hoveredSlot = -1;
            AutoSwap.selectingItem = false;
            AutoSwap.selectingSlotIndex = -1;
            inventoryMode = false;
            bindKey = -1;
            keyWasReleased = false;
            super.onClose();
        }
    }
}
