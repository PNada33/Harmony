package xd.harm.ui.display.impl;

import xd.harm.Harmony;
import xd.harm.events.render.EventDisplay;
import xd.harm.modules.api.ModuleManager;
import xd.harm.modules.impl.render.HUD;
import xd.harm.modules.impl.render.Theme;
import xd.harm.ui.display.ElementRenderer;
import xd.harm.utils.animations.easing.CompactAnimation;
import xd.harm.utils.animations.easing.Easing;
import xd.harm.utils.drag.Dragging;
import xd.harm.utils.render.KawaseBlur;
import xd.harm.utils.render.rect.RenderUtility;
import xd.harm.utils.render.color.ColorUtils;
import xd.harm.utils.render.font.Fonts;
import xd.harm.utils.text.font.ClientFonts;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.util.math.vector.Vector4f;
import org.lwjgl.opengl.GL11;
import ru.hogoshi.Animation;

import java.text.DecimalFormat;

public class WatermarkRenderer implements ElementRenderer {

    private final Dragging dragging;

    private static final float V_SPACING = 3f;
    private static final float DOT_RADIUS = 0.9f;
    private static final Vector4f DOT_RADIUS_VEC = new Vector4f(DOT_RADIUS, DOT_RADIUS, DOT_RADIUS, DOT_RADIUS);
    private static final String[] MENU_ITEMS = {"\u041d\u0438\u043a", "\u0421\u0435\u0440\u0432\u0435\u0440", "\u0424\u043f\u0441", "\u041f\u0438\u043d\u0433", "\u041a\u043e\u043e\u0440\u0434\u0438\u043d\u0430\u0442\u044b", "\u0411\u043f\u0441"};
    private static final String[] MENU_ICONS = {"B", "p", "g", "K", "N", "L"};

    private float currentWatermarkWidth = 0;
    private float targetWatermarkWidth = 0;
    private float currentCoordsWidth = 0;
    private float targetCoordsWidth = 0;

    private float currentFPS = 0;
    private float currentPing = 0;

    private boolean isFirstRender = true;
    private boolean isFirstRenderCoords = true;
    private boolean isFirstRenderPos = true;

    private final Animation coordsSlideAnimation = new Animation();

    private final CompactAnimation xAnimation = new CompactAnimation(Easing.EASE_OUT_QUAD, 100);
    private final CompactAnimation yAnimation = new CompactAnimation(Easing.EASE_OUT_QUAD, 100);
    private final CompactAnimation zAnimation = new CompactAnimation(Easing.EASE_OUT_QUAD, 100);
    private final CompactAnimation bpsAnimation = new CompactAnimation(Easing.EASE_OUT_QUAD, 100);
    private final DecimalFormat bpsFormat = new DecimalFormat("0.00");

    private boolean contextMenuOpen = false;
    private float contextMenuX = 0;
    private float contextMenuY = 0;
    private float contextMenuAlpha = 0;

    private float currentBPS = 0;
    private float displayBPS = 0;

    private float animatedX = -1f;
    private float animatedY = -1f;
    private float dragSpeed = 0.15f;

    private float lastWatermarkX = 4f;
    private float lastWatermarkY = 4f;
    private float lastWatermarkWidth = 0f;
    private float lastWatermarkHeight = 16f;

    public WatermarkRenderer(Dragging dragging) {
        this.dragging = dragging;
        coordsSlideAnimation.setValue(0);
    }

    @Override
    public void render(EventDisplay eventDisplay) {
        MatrixStack ms = eventDisplay.getMatrixStack();
        float posX = 4;
        float posY = 4;
        float fontSize = 6.5f;

        ModuleManager moduleManager = Harmony.getInstance().getModuleManager();
        HUD hud = moduleManager.getHud();

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        coordsSlideAnimation.update();

        if (dragging != null) {
            if (isFirstRenderPos) {
                animatedX = dragging.getX();
                animatedY = dragging.getY();
                isFirstRenderPos = false;
            }
            float targetX = dragging.getX();
            float targetY = dragging.getY();
            if (dragging.isDragging()) {
                animatedX = targetX;
                animatedY = targetY;
            } else if (Math.abs(targetX - animatedX) > 0.01f || Math.abs(targetY - animatedY) > 0.01f) {
                animatedX += (targetX - animatedX) * dragSpeed;
                animatedY += (targetY - animatedY) * dragSpeed;
                if (Math.abs(targetX - animatedX) < 0.1f) animatedX = targetX;
                if (Math.abs(targetY - animatedY) < 0.1f) animatedY = targetY;
            }
            posX = animatedX + dragging.getWobbleX();
            posY = animatedY + dragging.getWobbleY();
        }

        float padding = 4f;
        float elementSpacing = 2.5f;
        float iconSpacing = 2f;
        float radius = 5f;
        float height = 16f;
        int bgAlpha = 150;

        int whiteColor = ColorUtils.rgba(255, 255, 255, 255);
        int grayColor = ColorUtils.rgba(150, 150, 150, 255);
        int dotColor = ColorUtils.rgba(60, 60, 60, 255);
        int themeColor = Theme.MainColor(0);

        float separatorWidth = DOT_RADIUS * 2 + 2;

        String nickname = mc.player.getName().getString();
        float nicknameWidth = Fonts.sfuy.getWidth(nickname, fontSize);

        String serverIP = getServerIP();
        float serverIPWidth = Fonts.sfuy.getWidth(serverIP, fontSize);

        int fps = mc.debugFPS;
        if (isFirstRender) {
            currentFPS = fps;
        } else {
            currentFPS = animateFast(currentFPS, fps, 5);
        }
        String fpsNumber = String.valueOf(Math.round(currentFPS));
        String fpsLabel = "fps";
        String fpsText = fpsNumber + fpsLabel;
        float fpsWidth = Fonts.sfuy.getWidth(fpsText, fontSize);
        float fpsNumberWidth = Fonts.sfuy.getWidth(fpsNumber, fontSize);

        int ping = 0;
        if (mc.getConnection() != null) {
            NetworkPlayerInfo playerInfo = mc.getConnection().getPlayerInfo(mc.player.getUniqueID());
            if (playerInfo != null) {
                ping = playerInfo.getResponseTime();
            }
        }
        if (isFirstRender) {
            currentPing = ping;
        } else {
            currentPing = animateFast(currentPing, ping, 5);
        }
        String pingNumber = String.valueOf(Math.round(currentPing));
        String pingLabel = "ms";
        String pingText = pingNumber + pingLabel;
        float pingWidth = Fonts.sfuy.getWidth(pingText, fontSize);
        float pingNumberWidth = Fonts.sfuy.getWidth(pingNumber, fontSize);

        float logoWidth = 10f;
        float iconWidth = 8f;

        boolean hasAnyElements = hud.watermarkElements.getValueByName("\u041d\u0438\u043a").get() ||
                hud.watermarkElements.getValueByName("\u0421\u0435\u0440\u0432\u0435\u0440").get() ||
                hud.watermarkElements.getValueByName("\u0424\u043f\u0441").get() ||
                hud.watermarkElements.getValueByName("\u041f\u0438\u043d\u0433").get();

        targetWatermarkWidth = padding + logoWidth + padding + 3;

        if (hasAnyElements) {
            targetWatermarkWidth = padding + logoWidth + elementSpacing + separatorWidth + elementSpacing;
            boolean hasElements = false;

            if (hud.watermarkElements.getValueByName("\u041d\u0438\u043a").get()) {
                if (hasElements) targetWatermarkWidth += separatorWidth + elementSpacing;
                targetWatermarkWidth += iconWidth + iconSpacing + nicknameWidth + elementSpacing;
                hasElements = true;
            }

            if (hud.watermarkElements.getValueByName("\u0421\u0435\u0440\u0432\u0435\u0440").get()) {
                if (hasElements) targetWatermarkWidth += separatorWidth + elementSpacing;
                targetWatermarkWidth += iconWidth + iconSpacing + serverIPWidth + elementSpacing;
                hasElements = true;
            }

            if (hud.watermarkElements.getValueByName("\u0424\u043f\u0441").get()) {
                if (hasElements) targetWatermarkWidth += separatorWidth + elementSpacing;
                targetWatermarkWidth += iconWidth + iconSpacing + fpsWidth + elementSpacing;
                hasElements = true;
            }

            if (hud.watermarkElements.getValueByName("\u041f\u0438\u043d\u0433").get()) {
                if (hasElements) targetWatermarkWidth += separatorWidth + elementSpacing;
                targetWatermarkWidth += iconWidth + iconSpacing + pingWidth + elementSpacing;
                hasElements = true;
            }

            targetWatermarkWidth += padding;
        }

        if (isFirstRender) {
            currentWatermarkWidth = targetWatermarkWidth;
            isFirstRender = false;
        } else {
            currentWatermarkWidth = animateFast(currentWatermarkWidth, targetWatermarkWidth, 15);
        }

        boolean showCoordsBlock = hud.watermarkElements.getValueByName("\u041a\u043e\u043e\u0440\u0434\u0438\u043d\u0430\u0442\u044b").get()
                || hud.watermarkElements.getValueByName("\u0411\u043f\u0441").get();

        if (showCoordsBlock) {
            preCalcCoordsWidth(fontSize, hud);
        }

        float coordsBlockHeight = 16f;
        float totalWidth = showCoordsBlock ? Math.max(currentWatermarkWidth, currentCoordsWidth) : currentWatermarkWidth;
        float totalHeight = height + (showCoordsBlock ? (V_SPACING + coordsBlockHeight) : 0f);

        float grabScale = dragging != null ? dragging.getGrabScale() : 1.0f;
        float grabRot = dragging != null ? dragging.getWobbleAngle() : 0.0f;
        boolean isDragging = dragging != null && dragging.isDragging();
        boolean grab = dragging != null && (Math.abs(grabScale - 1.0f) > 0.001f || Math.abs(grabRot) > 0.001f);

        KawaseBlur.blur.updateBlur(3.0f, 3);

        if (currentWatermarkWidth > 2f && height > 2f) {
            final float blurRadius = radius + 0.75f;
            final float blurInset = 0.35f;
            final float bpx = posX;
            final float bpy = posY;
            final float bw = currentWatermarkWidth;
            final float bh = height;
            KawaseBlur.blur.render(() -> renderBlurMask(() ->
                    RenderUtility.drawRoundedRect(bpx + blurInset, bpy + blurInset,
                            bw - blurInset * 2f, bh - blurInset * 2f,
                            new Vector4f(blurRadius, blurRadius, blurRadius, blurRadius),
                            ColorUtils.rgba(255, 255, 255, 255))
            ));
        }

        if (showCoordsBlock && currentCoordsWidth > 2f) {
            final float blurRadius = radius + 0.75f;
            final float blurInset = 0.35f;
            final float bpx = posX;
            final float bpy = posY + height + V_SPACING;
            final float bw = currentCoordsWidth;
            final float bh = coordsBlockHeight;
            KawaseBlur.blur.render(() -> renderBlurMask(() ->
                    RenderUtility.drawRoundedRect(bpx + blurInset, bpy + blurInset,
                            bw - blurInset * 2f, bh - blurInset * 2f,
                            new Vector4f(blurRadius, blurRadius, blurRadius, blurRadius),
                            ColorUtils.rgba(255, 255, 255, 255))
            ));
        }

        if (grab) {
            GL11.glPushMatrix();
            RenderUtility.customScaledObject2D(posX, posY, totalWidth, totalHeight, grabScale);
        }

        GL11.glPushMatrix();
        GL11.glTranslatef(posX, posY, 0);
        if (Math.abs(grabRot) > 0.001f) {
            RenderUtility.customRotatedObject2D(0, 0, totalWidth, totalHeight, grabRot);
        }

        RenderUtility.drawRoundedRect(0, 0, currentWatermarkWidth, height,
                new Vector4f(radius, radius, radius, radius),
                ColorUtils.rgba(0, 0, 0, bgAlpha));

        boolean useScissor = !grab;

        if (useScissor) {
            MainWindow window = mc.getMainWindow();
            double guiScale = window.getGuiScaleFactor();
            int scissorX = (int) (posX * guiScale);
            int scissorY = (int) ((window.getScaledHeight() - posY - height) * guiScale);
            int scissorW = (int) (currentWatermarkWidth * guiScale);
            int scissorH = (int) (height * guiScale);
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor(scissorX, scissorY, Math.max(1, scissorW), Math.max(1, scissorH));
        }

        float currentX = padding;
        float textY = height / 2 - fontSize / 2 + 0.5f;
        float dotY = height / 2;
        float iconY = height / 2 - 1f;
        float logoY = height / 2 - 1f;

        int logoColor = ColorUtils.interpolateColor(Theme.MainColor(0), ColorUtils.rgb(255, 255, 255), 0.25f);
        float logoCenterX = currentX + logoWidth / 2f;
        ClientFonts.watermark[17].drawCenteredString(ms, "A", logoCenterX, logoY, logoColor);
        currentX += logoWidth;

        if (hasAnyElements) {
            currentX += elementSpacing;
            drawDot(currentX + separatorWidth / 2 - DOT_RADIUS, dotY, dotColor);
            currentX += separatorWidth + elementSpacing;

            boolean renderedElement = false;

            if (hud.watermarkElements.getValueByName("\u041d\u0438\u043a").get()) {
                if (renderedElement) {
                    drawDot(currentX + separatorWidth / 2 - DOT_RADIUS, dotY, dotColor);
                    currentX += separatorWidth + elementSpacing;
                }
                ClientFonts.iconsnew[16].drawString(ms, "d", currentX + 1.3f, iconY - 0.1f, themeColor);
                currentX += iconWidth + iconSpacing;
                Fonts.sfuy.drawText(ms, nickname, currentX, textY, whiteColor, fontSize);
                currentX += nicknameWidth + elementSpacing;
                renderedElement = true;
            }

            if (hud.watermarkElements.getValueByName("\u0421\u0435\u0440\u0432\u0435\u0440").get()) {
                if (renderedElement) {
                    drawDot(currentX + separatorWidth / 2 - DOT_RADIUS, dotY, dotColor);
                    currentX += separatorWidth + elementSpacing;
                }
                ClientFonts.iconsnew[35].drawString(ms, "p", currentX - 5, iconY - 3, themeColor);
                currentX += iconWidth + iconSpacing;
                Fonts.sfuy.drawText(ms, serverIP, currentX, textY, whiteColor, fontSize);
                currentX += serverIPWidth + elementSpacing;
                renderedElement = true;
            }

            if (hud.watermarkElements.getValueByName("\u0424\u043f\u0441").get()) {
                if (renderedElement) {
                    drawDot(currentX + separatorWidth / 2 - DOT_RADIUS, dotY, dotColor);
                    currentX += separatorWidth + elementSpacing;
                }
                ClientFonts.iconsnew[16].drawString(ms, "k", currentX - 1, iconY - 0.1f, themeColor);
                currentX += iconWidth + iconSpacing;
                Fonts.sfuy.drawText(ms, fpsNumber, currentX, textY, whiteColor, fontSize);
                Fonts.sfMedium.drawText(ms, fpsLabel, currentX + fpsNumberWidth, textY + 0.6f, grayColor, fontSize - 0.5f);
                currentX += fpsWidth + elementSpacing;
                renderedElement = true;
            }

            if (hud.watermarkElements.getValueByName("\u041f\u0438\u043d\u0433").get()) {
                if (renderedElement) {
                    drawDot(currentX + separatorWidth / 2 - DOT_RADIUS, dotY, dotColor);
                    currentX += separatorWidth + elementSpacing;
                }
                ClientFonts.iconsnew[24].drawString(ms, "q", currentX - 1, iconY - 1.0f, themeColor);
                currentX += iconWidth + iconSpacing;
                Fonts.sfuy.drawText(ms, pingNumber, currentX, textY, whiteColor, fontSize);
                Fonts.sfMedium.drawText(ms, pingLabel, currentX + pingNumberWidth, textY + 0.6f, grayColor, fontSize - 0.5f);
                renderedElement = true;
            }
        }

        if (useScissor) {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }

        if (showCoordsBlock) {
            renderCoordsAndSpeed(ms, 0, 0, height, radius, bgAlpha, fontSize, hud, useScissor, posX, posY);
        }

        GL11.glPopMatrix();
        if (grab) {
            GL11.glPopMatrix();
        }

        lastWatermarkX = posX;
        lastWatermarkY = posY;
        lastWatermarkWidth = currentWatermarkWidth;
        lastWatermarkHeight = height;

        if (dragging != null) {
            float dragWidth = showCoordsBlock ? Math.max(currentWatermarkWidth, currentCoordsWidth) : currentWatermarkWidth;
            float dragHeight = height + (showCoordsBlock ? (V_SPACING + coordsBlockHeight) : 0f);
            dragging.setWidth(dragWidth);
            dragging.setHeight(dragHeight);
        }

        if (mc.currentScreen instanceof ChatScreen) {
            if (contextMenuOpen) {
                contextMenuAlpha = animateFast(contextMenuAlpha, 1f, 10);
            } else {
                contextMenuAlpha = animateFast(contextMenuAlpha, 0f, 10);
            }
            if (contextMenuAlpha > 0.01f) {
                renderContextMenu(ms, posX, posY, currentWatermarkWidth, height, hud);
            }
        } else {
            if (contextMenuOpen) {
                contextMenuOpen = false;
            }
            contextMenuAlpha = 0;
        }
    }

    private void preCalcCoordsWidth(float fontSize, HUD hud) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        xAnimation.run(mc.player.getPosX());
        yAnimation.run(mc.player.getPosY());
        zAnimation.run(mc.player.getPosZ());

        float targetBPS = (float) calculateBPS();
        currentBPS = currentBPS + (targetBPS - currentBPS) * 0.1f;
        displayBPS = displayBPS + (currentBPS - displayBPS) * 0.05f;
        if (displayBPS < 0.01f) displayBPS = 0;

        float localPadding = 4f;
        float elementSpacing = 2.5f;
        float iconSpacing = 2f;
        float iconWidth = 8f;
        float separatorWidth = DOT_RADIUS * 2 + 2;

        boolean hasCoords = hud.watermarkElements.getValueByName("\u041a\u043e\u043e\u0440\u0434\u0438\u043d\u0430\u0442\u044b").get();
        boolean hasBPS = hud.watermarkElements.getValueByName("\u0411\u043f\u0441").get();

        int xCoord = (int) xAnimation.getValue();
        int yCoord = (int) yAnimation.getValue();
        int zCoord = (int) zAnimation.getValue();

        String xLbl = "x";
        String xVal = String.valueOf(xCoord);
        String yLbl = " y";
        String yVal = String.valueOf(yCoord);
        String zLbl = " z";
        String zVal = String.valueOf(zCoord);
        String coordsFullText = xLbl + xVal + yLbl + yVal + zLbl + zVal;
        float coordsTextWidth = Fonts.sfuy.getWidth(coordsFullText, fontSize);

        String bpsText = formatTwoDecimals(displayBPS);
        float bpsTextWidth = Fonts.sfuy.getWidth(bpsText, fontSize);

        targetCoordsWidth = localPadding;
        boolean hasCE = false;
        if (hasCoords) {
            targetCoordsWidth += iconWidth + iconSpacing + coordsTextWidth + elementSpacing;
            hasCE = true;
        }
        if (hasBPS) {
            if (hasCE) targetCoordsWidth += separatorWidth + elementSpacing;
            targetCoordsWidth += iconWidth + iconSpacing + bpsTextWidth + elementSpacing;
        }
        targetCoordsWidth += localPadding;

        if (isFirstRenderCoords) {
            currentCoordsWidth = targetCoordsWidth;
            isFirstRenderCoords = false;
        } else {
            currentCoordsWidth = animateFast(currentCoordsWidth, targetCoordsWidth, 15);
        }
    }

    private void drawDot(float x, float y, int color) {
        RenderUtility.drawRoundedRect(x, y - DOT_RADIUS, DOT_RADIUS * 2, DOT_RADIUS * 2,
                DOT_RADIUS_VEC, color);
    }

    private void renderCoordsAndSpeed(MatrixStack ms, float posX, float posY, float watermarkHeight, float radius,
                                      int bgAlpha, float fontSize, HUD hud, boolean useScissor,
                                      float globalPosX, float globalPosY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        float blockHeight = 16f;
        float localPadding = 4f;
        float elementSpacing = 2.5f;
        float iconSpacing = 2f;
        float iconWidth = 8f;

        int whiteColor = ColorUtils.rgba(255, 255, 255, 255);
        int grayColor = ColorUtils.rgba(150, 150, 150, 255);
        int dotColor = ColorUtils.rgba(60, 60, 60, 255);
        int themeColor = Theme.MainColor(0);

        float separatorWidth = DOT_RADIUS * 2 + 2;

        boolean hasCoords = hud.watermarkElements.getValueByName("\u041a\u043e\u043e\u0440\u0434\u0438\u043d\u0430\u0442\u044b").get();
        boolean hasBPS = hud.watermarkElements.getValueByName("\u0411\u043f\u0441").get();

        int xCoord = (int) xAnimation.getValue();
        int yCoord = (int) yAnimation.getValue();
        int zCoord = (int) zAnimation.getValue();

        String xLbl = "x";
        String xVal = String.valueOf(xCoord);
        String yLbl = " y";
        String yVal = String.valueOf(yCoord);
        String zLbl = " z";
        String zVal = String.valueOf(zCoord);
        String coordsFullText = xLbl + xVal + yLbl + yVal + zLbl + zVal;
        float coordsTextWidth = Fonts.sfuy.getWidth(coordsFullText, fontSize);

        String bpsText = formatTwoDecimals(displayBPS);

        GL11.glPushMatrix();
        GL11.glTranslatef(posX, posY + watermarkHeight + V_SPACING, 0);

        RenderUtility.drawRoundedRect(0, 0, currentCoordsWidth, blockHeight,
                new Vector4f(radius, radius, radius, radius),
                ColorUtils.rgba(0, 0, 0, bgAlpha));

        if (useScissor) {
            float coordsGlobalY = globalPosY + watermarkHeight + V_SPACING;
            MainWindow window = mc.getMainWindow();
            double guiScale = window.getGuiScaleFactor();
            int scissorX = (int) (globalPosX * guiScale);
            int scissorY = (int) ((window.getScaledHeight() - coordsGlobalY - blockHeight) * guiScale);
            int scissorW = (int) (currentCoordsWidth * guiScale);
            int scissorH = (int) (blockHeight * guiScale);
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor(scissorX, scissorY, Math.max(1, scissorW), Math.max(1, scissorH));
        }

        float currentX = localPadding;
        float textY = blockHeight / 2 - fontSize / 2 + 0.5f;
        float dotY = blockHeight / 2;
        float iconY = blockHeight / 2 - 1f;

        boolean renderedCoordElement = false;

        if (hasCoords) {
            ClientFonts.iconsnew[16].drawString(ms, "b", currentX + 1f, iconY + 0.1f, themeColor);
            currentX += iconWidth + iconSpacing;

            float tx = currentX - 1.5f;
            Fonts.sfuy.drawText(ms, xLbl, tx + 0.2f, textY + 1, grayColor, fontSize - 1);
            tx += Fonts.sfuy.getWidth(xLbl, fontSize);
            Fonts.sfuy.drawText(ms, xVal, tx, textY, whiteColor, fontSize);
            tx += Fonts.sfuy.getWidth(xVal, fontSize);
            Fonts.sfuy.drawText(ms, yLbl, tx + 0.2f, textY + 1, grayColor, fontSize - 1);
            tx += Fonts.sfuy.getWidth(yLbl, fontSize);
            Fonts.sfuy.drawText(ms, yVal, tx, textY, whiteColor, fontSize);
            tx += Fonts.sfuy.getWidth(yVal, fontSize);
            Fonts.sfuy.drawText(ms, zLbl, tx + 0.2f, textY + 1, grayColor, fontSize - 1);
            tx += Fonts.sfuy.getWidth(zLbl, fontSize);
            Fonts.sfuy.drawText(ms, zVal, tx, textY, whiteColor, fontSize);

            currentX += coordsTextWidth + elementSpacing;
            renderedCoordElement = true;
        }

        if (hasBPS) {
            if (renderedCoordElement) {
                drawDot(currentX + separatorWidth / 2 - DOT_RADIUS, dotY, dotColor);
                currentX += separatorWidth + elementSpacing;
            }
            ClientFonts.icons_nur[18].drawString(ms, "L", currentX, iconY, themeColor);
            currentX += iconWidth + iconSpacing;
            Fonts.sfuy.drawText(ms, bpsText, currentX, textY, whiteColor, fontSize);
        }

        if (useScissor) {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }

        GL11.glPopMatrix();
    }

    private void renderContextMenu(MatrixStack ms, float watermarkX, float watermarkY, float watermarkWidth, float watermarkHeight, HUD hud) {
        float menuWidth = 120;
        float menuItemHeight = 18;
        float menuHeight = menuItemHeight * 6 + 4;
        float menuX = contextMenuX;
        float menuY = contextMenuY;

        Minecraft mc = Minecraft.getInstance();
        if (menuX + menuWidth > mc.getMainWindow().getScaledWidth()) {
            menuX = mc.getMainWindow().getScaledWidth() - menuWidth - 5;
        }
        if (menuY + menuHeight > mc.getMainWindow().getScaledHeight()) {
            menuY = mc.getMainWindow().getScaledHeight() - menuHeight - 5;
        }

        int menuBgAlpha = (int) (180 * contextMenuAlpha);

        GL11.glPushMatrix();
        GL11.glTranslatef(0, (1 - contextMenuAlpha) * -5, 0);

        RenderUtility.drawRoundedRect(menuX, menuY, menuWidth, menuHeight,
                new Vector4f(4, 4, 4, 4),
                ColorUtils.rgba(0, 0, 0, menuBgAlpha));

        float itemY = menuY + 2;
        for (int i = 0; i < MENU_ITEMS.length; i++) {
            boolean enabled = hud.watermarkElements.getValueByName(MENU_ITEMS[i]).get();

            int mouseX = (int) (mc.mouseHelper.getMouseX() * mc.getMainWindow().getScaledWidth() / (double) mc.getMainWindow().getWidth());
            int mouseY = (int) (mc.mouseHelper.getMouseY() * mc.getMainWindow().getScaledHeight() / (double) mc.getMainWindow().getHeight());

            boolean hovered = mouseX >= menuX && mouseX <= menuX + menuWidth &&
                    mouseY >= itemY && mouseY <= itemY + menuItemHeight;

            if (hovered) {
                RenderUtility.drawRoundedRect(menuX + 2, itemY, menuWidth - 4, menuItemHeight - 2,
                        new Vector4f(3, 3, 3, 3),
                        ColorUtils.setAlpha(ColorUtils.rgb(255, 255, 255), (int) (20 * contextMenuAlpha)));
            }

            int textColor = enabled ?
                    ColorUtils.setAlpha(ColorUtils.rgb(255, 255, 255), (int) (255 * contextMenuAlpha)) :
                    ColorUtils.setAlpha(ColorUtils.rgb(130, 130, 140), (int) (200 * contextMenuAlpha));

            int iconColor = ColorUtils.setAlpha(Theme.MainColor(0), (int) (200 * contextMenuAlpha));

            float iconX = menuX + 8;
            float iconYPos = itemY + menuItemHeight / 2 - 1;

            switch (i) {
                case 0:
                    ClientFonts.icons_wex[20].drawString(ms, MENU_ICONS[i], iconX, iconYPos, iconColor);
                    break;
                case 1:
                    ClientFonts.iconsnew[35].drawString(ms, MENU_ICONS[i], iconX - 4, iconYPos - 3, iconColor);
                    break;
                case 2:
                    Fonts.ico.drawText(ms, MENU_ICONS[i], iconX, itemY + menuItemHeight / 2 - 3, iconColor, 8f);
                    break;
                case 3:
                    ClientFonts.icons_nur[18].drawString(ms, MENU_ICONS[i], iconX, iconYPos, iconColor);
                    break;
                case 4:
                    ClientFonts.icons_nur[18].drawString(ms, MENU_ICONS[i], iconX, iconYPos, iconColor);
                    break;
                case 5:
                    ClientFonts.icons_nur[18].drawString(ms, MENU_ICONS[i], iconX, iconYPos, iconColor);
                    break;
            }

            Fonts.sfuy.drawText(ms, MENU_ITEMS[i], menuX + 25, itemY + menuItemHeight / 2 - 3, textColor, 6.5f);

            float checkboxX = menuX + menuWidth - 22;
            float checkboxY = itemY + menuItemHeight / 2 - 5;
            float checkboxSize = 10;

            int checkboxBg = enabled ?
                    ColorUtils.setAlpha(Theme.MainColor(0), (int) (220 * contextMenuAlpha)) :
                    ColorUtils.setAlpha(ColorUtils.rgb(40, 40, 50), (int) (180 * contextMenuAlpha));

            RenderUtility.drawRoundedRect(checkboxX, checkboxY, checkboxSize, checkboxSize,
                    new Vector4f(2, 2, 2, 2), checkboxBg);

            if (enabled) {
                Fonts.sfuy.drawText(ms, "\u2713", checkboxX + 1.5f, checkboxY + 0.5f,
                        ColorUtils.setAlpha(ColorUtils.rgb(255, 255, 255), (int) (255 * contextMenuAlpha)), 7f);
            }

            itemY += menuItemHeight;
        }

        GL11.glPopMatrix();
    }

    public void handleMouseClick(int mouseX, int mouseY, int button) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.currentScreen instanceof ChatScreen)) return;

        HUD hud = Harmony.getInstance().getModuleManager().getHud();

        float watermarkX = lastWatermarkX;
        float watermarkY = lastWatermarkY;
        float watermarkWidth = lastWatermarkWidth;
        float watermarkHeight = lastWatermarkHeight;

        if (button == 1) {
            if (mouseX >= watermarkX && mouseX <= watermarkX + watermarkWidth &&
                    mouseY >= watermarkY && mouseY <= watermarkY + watermarkHeight) {
                contextMenuOpen = !contextMenuOpen;
                contextMenuX = mouseX;
                contextMenuY = mouseY;
                return;
            }
        }

        if (button == 0 && contextMenuOpen) {
            float menuWidth = 120;
            float menuItemHeight = 18;
            float menuX = contextMenuX;
            float menuY = contextMenuY + 2;

            if (mouseX >= menuX && mouseX <= menuX + menuWidth) {
                for (int i = 0; i < 6; i++) {
                    float itemY = menuY + i * menuItemHeight;
                    if (mouseY >= itemY && mouseY <= itemY + menuItemHeight) {
                        hud.watermarkElements.getValueByName(MENU_ITEMS[i]).set(!hud.watermarkElements.getValueByName(MENU_ITEMS[i]).get());
                        return;
                    }
                }
            }
            contextMenuOpen = false;
        }
    }

    private String getServerIP() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getCurrentServerData() != null) {
            return mc.getCurrentServerData().serverIP;
        } else if (mc.isIntegratedServerRunning()) {
            return "Singleplayer";
        }
        return "Unknown";
    }

    private double calculateBPS() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return 0.0;
        double dx = mc.player.getPosX() - mc.player.lastTickPosX;
        double dz = mc.player.getPosZ() - mc.player.lastTickPosZ;
        double distancePerTick = Math.hypot(dx, dz);
        return distancePerTick * 20.0;
    }

    private String formatTwoDecimals(float value) {
        return bpsFormat.format(value);
    }

    private float animateFast(float current, float target, float speed) {
        float diff = target - current;
        if (Math.abs(diff) < 0.1f) return target;
        return current + diff / speed;
    }

    private void renderBlurMask(Runnable draw) {
        GL11.glEnable(0x809D);
        GL11.glEnable(0x809E);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.01f);
        draw.run();
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(0x809E);
        GL11.glDisable(0x809D);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.1f);
    }
}
