package xd.harm.ui.display.impl.hud2;

import xd.harm.Harmony;
import xd.harm.events.render.EventDisplay;
import xd.harm.modules.impl.render.HUD;
import xd.harm.modules.impl.render.Theme;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.ui.display.ElementRenderer;
import xd.harm.utils.drag.Dragging;
import xd.harm.utils.render.KawaseBlur;
import xd.harm.utils.render.color.ColorUtils;
import xd.harm.utils.render.font.Fonts;
import xd.harm.utils.render.rect.RenderUtility;
import xd.harm.utils.text.font.ClientFonts;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.util.math.vector.Vector4f;
import org.lwjgl.opengl.GL11;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;

public class WatermarkRenderer2 implements ElementRenderer {

    private static final Vector4f PANEL_RADIUS = new Vector4f(3.5f, 3.5f, 3.5f, 3.5f);
    private static final Vector4f DOT_RADIUS = new Vector4f(2.5f, 2.5f, 2.5f, 2.5f);

    private final Dragging dragging;
    private final List<String> activeElements = new ArrayList<>(7);
    private final List<String> values = new ArrayList<>(2);
    private final StringBuilder combinedValues = new StringBuilder(64);
    private final DecimalFormat oneDecimalFormat = new DecimalFormat("0.0");
    private final DecimalFormat coordFormat = new DecimalFormat("0");
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
    private final Date reusableDate = new Date();
    private float currentWidth = 0;
    private float currentLeftWidth = 0;
    private boolean isFirstRender = true;

    private float animatedDotAlpha = 0f;
    private float animatedSeparatorAlpha = 0f;
    private float animatedTextAlpha = 0f;
    private long cachedMinute = Long.MIN_VALUE;
    private String cachedTime = "";

    public WatermarkRenderer2(Dragging dragging) {
        this.dragging = dragging;
    }

    @Override
    public void render(EventDisplay eventDisplay) {
        MatrixStack ms = eventDisplay.getMatrixStack();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        HUD hud = Harmony.getInstance().getModuleManager().getHud();

        float posX = dragging.getX();
        float posY = dragging.getY();

        float height = 16f;
        float radius = 3.5f;
        float padding = 5f;
        float fontSize = 6.5f;

        activeElements.clear();
        addWatermarkElement(activeElements, hud, "Время", "Время");
        addWatermarkElement(activeElements, hud, "Пинг", "Пинг");
        addWatermarkElement(activeElements, hud, "Фпс", "FPS");
        addWatermarkElement(activeElements, hud, "Бпс", "BPS");
        addWatermarkElement(activeElements, hud, "Ник", "Ник");
        addWatermarkElement(activeElements, hud, "Координаты", "Координаты");
        addWatermarkElement(activeElements, hud, "Сервер", "Сервер");

        boolean hasRightElements = !activeElements.isEmpty();

        float iconWidth = 8f;
        String mainText = "Harmony (Beta)";
        float textWidth = Fonts.sfuy.getWidth(mainText, fontSize);
        float targetLeftWidth = padding + iconWidth + 3f + textWidth + padding;

        float dotSize = 5f;

        values.clear();
        long now = System.currentTimeMillis();
        for (String elem : activeElements) {
            switch (elem) {
                case "Время":
                    values.add(formatTime(now));
                    break;
                case "Пинг":
                    int ping = 0;
                    if (mc.getConnection() != null) {
                        NetworkPlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUniqueID());
                        if (info != null) ping = info.getResponseTime();
                    }
                    values.add(ping + "ms");
                    break;
                case "FPS":
                    values.add(mc.debugFPS + " fps");
                    break;
                case "BPS":
                    double bps = Math.hypot(mc.player.getPosX() - mc.player.lastTickPosX, mc.player.getPosZ() - mc.player.lastTickPosZ) * 20.0;
                    values.add(formatOneDecimal(bps, " bps"));
                    break;
                case "Ник":
                    values.add(mc.player.getName().getString());
                    break;
                case "Координаты":
                    values.add(formatCoordinates(mc));
                    break;
                case "Сервер":
                    String server = mc.getCurrentServerData() != null ? mc.getCurrentServerData().serverIP : "Одиночная игра";
                    values.add(server);
                    break;
            }
        }

        float targetRightWidth = 0;
        if (hasRightElements) {
            targetRightWidth = padding + dotSize + 4f;
            targetRightWidth += 1f + 4f;
            float elemsWidth = 0;
            for (int i = 0; i < values.size(); i++) {
                elemsWidth += Fonts.sfuy.getWidth(values.get(i), fontSize);
                if (i < values.size() - 1) elemsWidth += Fonts.sfuy.getWidth(" ", fontSize);
            }
            targetRightWidth += elemsWidth;
            targetRightWidth += padding;
        }

        float maxTotalWidth = Math.max(targetLeftWidth, mc.getMainWindow().getScaledWidth() - posX - 4f);
        float targetTotalWidth = Math.min(targetLeftWidth + targetRightWidth, maxTotalWidth);

        float speed = 0.1f;

        if (isFirstRender) {
            currentLeftWidth = targetLeftWidth;
            currentWidth = targetTotalWidth;
            if (hasRightElements) {
                animatedDotAlpha = 255f;
                animatedSeparatorAlpha = 255f;
                animatedTextAlpha = 255f;
            } else {
                animatedDotAlpha = 0f;
                animatedSeparatorAlpha = 0f;
                animatedTextAlpha = 0f;
            }
            isFirstRender = false;
        } else {
            currentLeftWidth += (targetLeftWidth - currentLeftWidth) * speed;
            currentWidth += (targetTotalWidth - currentWidth) * speed;

            float targetDotAlpha = hasRightElements ? 255f : 0f;
            float targetSepAlpha = hasRightElements ? 255f : 0f;
            float targetTextAlpha = hasRightElements ? 255f : 0f;

            animatedDotAlpha += (targetDotAlpha - animatedDotAlpha) * speed;
            animatedSeparatorAlpha += (targetSepAlpha - animatedSeparatorAlpha) * speed;
            animatedTextAlpha += (targetTextAlpha - animatedTextAlpha) * speed;
        }

        if (Math.abs(currentWidth - targetTotalWidth) < 0.5f) {
            currentWidth = targetTotalWidth;
        }
        if (Math.abs(currentLeftWidth - targetLeftWidth) < 0.5f) {
            currentLeftWidth = targetLeftWidth;
        }

        currentWidth = Math.max(currentLeftWidth, currentWidth);
        float rightWidth = Math.max(0f, currentWidth - currentLeftWidth);

        KawaseBlur.blur.updateBlur(3.0f, 3);
        KawaseBlur.blur.render(() -> {
            RenderSystem.enableAlphaTest();
            RenderSystem.alphaFunc(GL11.GL_GREATER, 0.01f);
            RenderUtility.drawRoundedRect(posX, posY, currentWidth, height, PANEL_RADIUS, ColorUtils.rgba(255, 255, 255, 255));
            RenderSystem.disableAlphaTest();
        });

        RenderUtility.drawRoundedRect(posX, posY, currentWidth, height, PANEL_RADIUS, ColorUtils.rgba(25, 25, 30, 160));

        if (rightWidth > 1f) {
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            RenderUtility.scissor(posX + currentLeftWidth, posY, rightWidth, height);
            RenderUtility.drawRoundedRect(posX, posY, currentWidth, height, PANEL_RADIUS, ColorUtils.rgba(0, 0, 0, 120));
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }

        RenderUtility.drawRoundedRectOutline(posX, posY, currentWidth, height, radius, 0.5f, ColorUtils.rgba(255, 255, 255, 30));

        float currentX = posX + padding;

        ClientFonts.watermark[17].drawString(ms, "A", currentX, posY + height / 2f - 1, Theme.MainColor(0));
        currentX += iconWidth + 3f;

        Fonts.sfuy.drawText(ms, mainText, currentX, posY + height / 2f - fontSize / 2f + 0.5f, -1, fontSize);

        if (rightWidth > 1f && animatedDotAlpha > 1f) {
            currentX = posX + currentLeftWidth + padding;

            int dotAlpha = (int) Math.min(255, Math.max(0, animatedDotAlpha));
            int sepAlpha = (int) Math.min(255, Math.max(0, animatedSeparatorAlpha));
            int textAlpha = (int) Math.min(255, Math.max(0, animatedTextAlpha));

            float dotY = posY + height / 2f - dotSize / 2f;

            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            RenderUtility.scissor(posX + currentLeftWidth, posY, rightWidth, height);

            RenderUtility.drawShadow(currentX, dotY, dotSize, dotSize, 6, ColorUtils.rgba(255, 255, 255, dotAlpha));
            RenderUtility.drawRoundedRect(currentX, dotY, dotSize, dotSize, DOT_RADIUS, ColorUtils.rgba(255, 255, 255, dotAlpha));
            currentX += dotSize + 4f;

            if (!values.isEmpty()) {
                RenderUtility.drawRect(currentX, posY + 4, currentX + 0.5f, posY + height - 4, ColorUtils.rgba(255, 255, 255, (int) (sepAlpha * 0.235f)));
                currentX += 1f + 4f;

                combinedValues.setLength(0);
                for (int i = 0; i < values.size(); i++) {
                    combinedValues.append(values.get(i));
                    if (i < values.size() - 1) combinedValues.append(" ");
                }
                Fonts.sfuy.drawText(ms, combinedValues.toString(), currentX, posY + height / 2f - fontSize / 2f + 0.5f, ColorUtils.rgba(255, 255, 255, textAlpha), fontSize);
            }

            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }

        dragging.setWidth(currentWidth);
        dragging.setHeight(height);
    }

    private void addWatermarkElement(List<String> activeElements, HUD hud, String settingName, String elementName) {
        if (activeElements.size() >= 2) {
            return;
        }

        try {
            BooleanSetting setting = hud.watermarkElements.getValueByName(settingName);
            if (setting != null && setting.get()) {
                activeElements.add(elementName);
            }
        } catch (Exception ignored) {
        }
    }

    private String formatTime(long now) {
        long minute = now / 60000L;
        if (minute != cachedMinute) {
            cachedMinute = minute;
            reusableDate.setTime(now);
            cachedTime = timeFormat.format(reusableDate);
        }
        return cachedTime;
    }

    private String formatOneDecimal(double value, String suffix) {
        return oneDecimalFormat.format(value) + suffix;
    }

    private String formatCoordinates(Minecraft mc) {
        return coordFormat.format(mc.player.getPosX()) + " " +
                coordFormat.format(mc.player.getPosY()) + " " +
                coordFormat.format(mc.player.getPosZ());
    }
}
