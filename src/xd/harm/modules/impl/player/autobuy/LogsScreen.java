package xd.harm.modules.impl.player.autobuy;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;
import org.lwjgl.opengl.GL11;
import xd.harm.modules.impl.render.Theme;
import xd.harm.utils.math.MathUtil;
import xd.harm.utils.render.GaussianBlur;
import xd.harm.utils.render.color.ColorUtils;
import xd.harm.utils.render.font.Fonts;
import xd.harm.utils.render.gl.Stencil;
import xd.harm.utils.render.rect.RenderUtility;
import xd.harm.utils.text.GradientUtil;
import ru.hogoshi.Animation;
import ru.hogoshi.util.Easings;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class LogsScreen extends Screen {
    private final AutoBuyManager manager;
    private final String server;
    private final String account;

    private float posX, posY;
    private float windowWidth = 620;
    private float windowHeight = 420;
    private float scroll = 0;
    private float animatedScroll = 0;

    private Animation openAnim = new Animation();
    private Animation closeBtnAnim = new Animation();
    private Animation closePressAnim = new Animation();
    private Animation clearBtnAnim = new Animation();
    private Animation clearPressAnim = new Animation();

    private ButtonRipple closeRipple = null;
    private ButtonRipple clearRipple = null;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    public LogsScreen(AutoBuyManager manager, String server, String account) {
        super(new StringTextComponent("Transaction Logs"));
        this.manager = manager;
        this.server = server;
        this.account = account;
    }

    @Override
    protected void init() {
        openAnim = new Animation().animate(1, 0.5f, Easings.CIRC_OUT);
        closeBtnAnim = new Animation();
        closePressAnim = new Animation();
        clearBtnAnim = new Animation();
        clearPressAnim = new Animation();
    }

    @Override
    public void tick() {
        posX = (width - windowWidth) / 2f;
        posY = (height - windowHeight) / 2f;
    }

    @Override
    public void render(MatrixStack stack, int mx, int my, float pt) {
        openAnim.update();
        closeBtnAnim.update();
        closePressAnim.update();
        animatedScroll = MathUtil.fast(animatedScroll, scroll, 15);

        float animValue = (float) openAnim.getValue();
        float alpha = Math.min(animValue, 1);

        fill(stack, 0, 0, width, height, ColorUtils.rgba(0, 0, 0, (int)(160 * alpha)));

        if (animValue < 0.01f) return;

        GlStateManager.pushMatrix();
        float cx = posX + windowWidth / 2f;
        float cy = posY + windowHeight / 2f;
        GlStateManager.translatef(cx, cy, 0);
        GlStateManager.scalef(animValue, animValue, 1);
        GlStateManager.translatef(-cx, -cy, 0);

        float corner = 14;
        GaussianBlur.startBlur();
        RenderUtility.drawRoundedRect(posX, posY, windowWidth, windowHeight, corner, -1);
        GaussianBlur.endBlur(18, 2);

        RenderUtility.drawRoundedRect(posX, posY, windowWidth, windowHeight, corner, ColorUtils.rgba(16, 16, 21, (int)(250 * alpha)));

        renderBackgroundEffects(alpha);
        renderHeader(stack, alpha);
        renderStats(stack, alpha);
        renderLogs(stack, mx, my, alpha);
        renderClearButton(stack, mx, my, alpha);
        renderCloseButton(stack, mx, my, alpha);

        GlStateManager.popMatrix();
        super.render(stack, mx, my, pt);
    }

    private void renderBackgroundEffects(float alpha) {
        float time = System.currentTimeMillis() / 2000f;
        Stencil.initStencilToWrite();
        RenderUtility.drawRoundedRect(posX, posY, windowWidth, windowHeight, 14, -1);
        Stencil.readStencilBuffer(1);

        float orbX = posX + windowWidth * 0.85f + (float)Math.sin(time) * 30;
        float orbY = posY + windowHeight * 0.1f + (float)Math.cos(time * 0.8) * 20;
        drawRadialGradient(orbX, orbY, 180, ColorUtils.setAlpha(Theme.MainColor(0), (int)(25 * alpha)), ColorUtils.rgba(0, 0, 0, 0));

        float orb2X = posX + windowWidth * 0.1f - (float)Math.cos(time * 0.5) * 40;
        float orb2Y = posY + windowHeight * 0.9f - (float)Math.sin(time * 0.7) * 30;
        drawRadialGradient(orb2X, orb2Y, 200, ColorUtils.setAlpha(ColorUtils.rgba(100, 255, 150, 255), (int)(15 * alpha)), ColorUtils.rgba(0, 0, 0, 0));

        Stencil.uninitStencilBuffer();
    }

    private void renderHeader(MatrixStack stack, float alpha) {
        Fonts.sfuy.drawText(stack, GradientUtil.gradient("Логи транзакций"), posX + 16, posY + 15, 13.8f, (int)(255 * alpha));

        List<TransactionLog> logs = manager.getTransactionLogs();
        Fonts.sfuy.drawText(stack, logs.size() + " транзакций", posX + 17, posY + 32, ColorUtils.rgba(145, 145, 158, (int)(205 * alpha)), 6.5f);
    }

    private void renderStats(MatrixStack stack, float alpha) {
        List<TransactionLog> logs = manager.getTransactionLogs();
        long totalBuy = 0, totalSell = 0;

        for (TransactionLog log : logs) {
            if (!log.server.equals(server) || !log.account.equals(account)) continue;
            if (log.type == TransactionLog.Type.BUY) totalBuy += log.price;
            else totalSell += log.price;
        }

        long totalProfit = totalSell - totalBuy;

        float statsY = posY + 50;
        float statsX = posX + 16;
        float statW = (windowWidth - 48) / 3;
        int themeColor = Theme.MainColor(0);

        drawStatBox(stack, "\u2193 Куплено", String.format("%,d $", totalBuy), statsX, statsY, statW, ColorUtils.rgba(255, 100, 100, 255), alpha);
        drawStatBox(stack, "\u2191 Продано", String.format("%,d $", totalSell), statsX + statW + 8, statsY, statW, ColorUtils.rgba(100, 255, 130, 255), alpha);
        drawStatBox(stack, "\u25B2 Прибыль", String.format("%,d $", totalProfit), statsX + (statW + 8) * 2, statsY, statW,
                totalProfit >= 0 ? ColorUtils.setAlpha(themeColor, 255) : ColorUtils.rgba(255, 80, 80, 255), alpha);
    }

    private void drawStatBox(MatrixStack stack, String label, String value, float x, float y, float w, int color, float alpha) {
        float h = 55;
        float pulse = (float)(Math.sin(System.currentTimeMillis() / 800.0) * 0.5 + 0.5);

        RenderUtility.drawShadow(x, y, w, h, 10, ColorUtils.setAlpha(color, (int)(30 * alpha)));
        RenderUtility.drawRoundedRectOutline(x, y, w, h, 9, 1f, ColorUtils.setAlpha(color, (int)((60 + 30 * pulse) * alpha)));


        Fonts.sfuy.drawCenteredText(stack, label, x + w / 2, y + 10, ColorUtils.rgba(170, 170, 180, (int)(210 * alpha)), 6.5f);
        Fonts.sfuy.drawCenteredText(stack, value, x + w / 2, y + 25, ColorUtils.setAlpha(color, (int)(255 * alpha)), 9.5f);
    }

    private void renderLogs(MatrixStack stack, int mx, int my, float alpha) {
        float logsTop = posY + 122;
        float logsHeight = windowHeight - 138;
        float logsX = posX + 16;
        float logsW = windowWidth - 32;

        Stencil.initStencilToWrite();
        RenderUtility.drawRoundedRect(logsX, logsTop, logsW, logsHeight, 4, -1);
        Stencil.readStencilBuffer(1);

        List<TransactionLog> logs = manager.getTransactionLogs();
        float itemH = 64;
        float gap = 7;
        float currentY = logsTop + 8 + animatedScroll;

        for (int i = logs.size() - 1; i >= 0; i--) {
            TransactionLog log = logs.get(i);

            if (currentY + itemH < logsTop || currentY > logsTop + logsHeight) {
                currentY += itemH + gap;
                continue;
            }

            boolean isBuy = log.type == TransactionLog.Type.BUY;
            int accentColor = isBuy ? ColorUtils.rgba(255, 100, 100, (int)(220 * alpha)) : ColorUtils.rgba(100, 255, 150, (int)(220 * alpha));
            String typeLabel = isBuy ? "\u2193 ПОКУПКА" : "\u2191 ПРОДАЖА";

            boolean hovered = RenderUtility.isInRegion(mx, my, logsX, currentY, logsW, itemH);
            float hoverAlpha = hovered ? 1.1f : 1.0f;

            RenderUtility.drawShadow(logsX, currentY, logsW, itemH, 8, ColorUtils.setAlpha(accentColor, (int)(20 * alpha)));
            RenderUtility.drawRoundedRectOutline(logsX, currentY, logsW, itemH, 8, 1f, ColorUtils.setAlpha(accentColor, (int)(70 * alpha)));
            

            float iconX = logsX + 14;
            float iconY = currentY + itemH / 2 - 8;
            if (log.stack != null && !log.stack.isEmpty()) {
                minecraft.getItemRenderer().renderItemIntoGUI(log.stack, (int) iconX, (int) iconY);
            }

            float textX = logsX + 38;

            Fonts.sfuy.drawText(stack, typeLabel, textX, currentY + 8, accentColor, 6.5f);

            Fonts.sfuy.drawText(stack, log.itemName, textX, currentY + 19, ColorUtils.rgba(225, 225, 235, (int)(255 * alpha)), 9f);

            String priceStr = String.format("%,d $", log.price);
            float priceX = logsX + logsW - Fonts.sfuy.getWidth(priceStr, 9f) - 14;
            Fonts.sfuy.drawText(stack, priceStr, priceX, currentY + 19, ColorUtils.setAlpha(accentColor, (int)(255 * alpha)), 9f);

            String details = String.format("x%d  •  %s", log.quantity, log.timestamp.format(formatter));
            Fonts.sfuy.drawText(stack, details, textX, currentY + 33, ColorUtils.rgba(150, 150, 160, (int)(190 * alpha)), 6.5f);

            String serverInfo = "\uD83D\uDCCC " + log.server + "  •  \uD83D\uDC64 " + log.account;
            Fonts.sfuy.drawText(stack, serverInfo, textX, currentY + 46, ColorUtils.rgba(110, 110, 125, (int)(160 * alpha)), 6f);

            currentY += itemH + gap;
        }

        float contentH = logs.size() * (itemH + gap);
        if (contentH > logsHeight) {
            float scrollBarH = (logsHeight / contentH) * logsHeight;
            float scrollY = (-animatedScroll / contentH) * logsHeight;
            RenderUtility.drawRoundedRect(logsX + logsW - 5, logsTop + 5 + scrollY, 3f, scrollBarH - 10, 1.5f,
                    ColorUtils.rgba(255, 255, 255, (int)(50 * alpha)));
        }

        Stencil.uninitStencilBuffer();
    }

    private void renderCloseButton(MatrixStack stack, int mx, int my, float alpha) {
        float size = 24;
        float x = posX + windowWidth - size - 14;
        float y = posY + 13;
        boolean hover = RenderUtility.isInRegion(mx, my, x, y, size, size);
        closeBtnAnim.animate(hover ? 1 : 0, 0.25f, Easings.EXPO_OUT);
        float anim = (float) closeBtnAnim.getValue();
        float press = (float) closePressAnim.getValue();

        float cx = x + size / 2;
        float cy = y + size / 2;

        GlStateManager.pushMatrix();
        GlStateManager.translatef(cx, cy, 0);
        float scale = 1.0f + (anim * 0.15f) - (press * 0.1f);
        GlStateManager.scalef(scale, scale, 1);
        GlStateManager.translatef(-cx, -cy, 0);

        int baseColor = ColorUtils.rgba(28, 28, 34, (int)(255 * alpha));
        int hoverColor = ColorUtils.rgba(220, 50, 50, (int)(255 * alpha));
        int activeColor = ColorUtils.interpolateColor(baseColor, hoverColor, anim);

        if (anim > 0.01f) {
            RenderUtility.drawShadow(x, y, size, size, 14, ColorUtils.setAlpha(hoverColor, (int)(160 * anim * alpha)));
        }
        RenderUtility.drawRoundedRect(x, y, size, size, 7, activeColor);

        int borderBase = ColorUtils.rgba(50, 50, 60, (int)(255 * alpha));
        int borderHover = ColorUtils.rgba(220, 50, 50, (int)(255 * alpha));
        RenderUtility.drawRoundedRectOutline(x, y, size, size, 7, 1f, ColorUtils.interpolateColor(borderBase, borderHover, anim));

        if (closeRipple != null && !closeRipple.isFinished()) {
            Stencil.initStencilToWrite();
            RenderUtility.drawRoundedRect(x, y, size, size, 7, -1);
            Stencil.readStencilBuffer(1);
            float p = closeRipple.getProgress();
            float rad = size * 1.5f * p;
            drawRadialGradient(closeRipple.startX, closeRipple.startY, rad,
                    ColorUtils.rgba(255, 255, 255, (int)(180 * (1 - p) * alpha)),
                    ColorUtils.rgba(255, 255, 255, 0));
            Stencil.uninitStencilBuffer();
        }

        float iconSize = 6f;
        float rot = anim * 90;
        GlStateManager.pushMatrix();
        GlStateManager.translatef(cx, cy, 0);
        GlStateManager.rotatef(rot, 0, 0, 1);
        int xColor = ColorUtils.interpolateColor(ColorUtils.rgba(170, 170, 180, 255), ColorUtils.rgba(255, 255, 255, 255), anim);
        float r = (float)(xColor >> 16 & 255) / 255.0F;
        float g = (float)(xColor >> 8 & 255) / 255.0F;
        float b = (float)(xColor & 255) / 255.0F;
        float a = (float)(xColor >> 24 & 255) / 255.0F * alpha;
        GlStateManager.disableTexture();
        GlStateManager.enableBlend();
        GL11.glColor4f(r, g, b, a);
        GL11.glLineWidth(1.8f);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(-iconSize / 2, -iconSize / 2);
        GL11.glVertex2f(iconSize / 2, iconSize / 2);
        GL11.glVertex2f(-iconSize / 2, iconSize / 2);
        GL11.glVertex2f(iconSize / 2, -iconSize / 2);
        GL11.glEnd();
        GlStateManager.enableTexture();
        GlStateManager.popMatrix();
        GlStateManager.popMatrix();
    }

    private void renderClearButton(MatrixStack stack, int mx, int my, float alpha) {
        float size = 24;
        float gap = 8;
        float x = posX + windowWidth - size - 14 - size - gap;
        float y = posY + 13;
        boolean hover = RenderUtility.isInRegion(mx, my, x, y, size, size);
        clearBtnAnim.animate(hover ? 1 : 0, 0.25f, Easings.EXPO_OUT);
        float anim = (float) clearBtnAnim.getValue();
        float press = (float) clearPressAnim.getValue();

        float cx = x + size / 2;
        float cy = y + size / 2;

        GlStateManager.pushMatrix();
        GlStateManager.translatef(cx, cy, 0);
        float scale = 1.0f + (anim * 0.15f) - (press * 0.1f);
        GlStateManager.scalef(scale, scale, 1);
        GlStateManager.translatef(-cx, -cy, 0);

        int baseColor = ColorUtils.rgba(28, 28, 34, (int)(255 * alpha));
        int hoverColor = ColorUtils.rgba(255, 140, 60, (int)(255 * alpha));
        int activeColor = ColorUtils.interpolateColor(baseColor, hoverColor, anim);

        if (anim > 0.01f) {
            RenderUtility.drawShadow(x, y, size, size, 14, ColorUtils.setAlpha(hoverColor, (int)(140 * anim * alpha)));
        }
        RenderUtility.drawRoundedRect(x, y, size, size, 7, activeColor);

        int borderBase = ColorUtils.rgba(50, 50, 60, (int)(255 * alpha));
        RenderUtility.drawRoundedRectOutline(x, y, size, size, 7, 1f, ColorUtils.interpolateColor(borderBase, hoverColor, anim));

        if (clearRipple != null && !clearRipple.isFinished()) {
            Stencil.initStencilToWrite();
            RenderUtility.drawRoundedRect(x, y, size, size, 7, -1);
            Stencil.readStencilBuffer(1);
            float p = clearRipple.getProgress();
            float rad = size * 1.5f * p;
            drawRadialGradient(clearRipple.startX, clearRipple.startY, rad,
                    ColorUtils.rgba(255, 255, 255, (int)(180 * (1 - p) * alpha)),
                    ColorUtils.rgba(255, 255, 255, 0));
            Stencil.uninitStencilBuffer();
        }

        int iconColor = ColorUtils.interpolateColor(ColorUtils.rgba(170, 170, 180, 255), ColorUtils.rgba(255, 255, 255, 255), anim);
        Fonts.sfuy.drawCenteredText(stack, "\uD83D\uDDD1", cx, cy - 4, iconColor, 9f);

        GlStateManager.popMatrix();
    }

    public void drawRadialGradient(float x, float y, float radius, int startColor, int endColor) {
        float f = (float)(startColor >> 16 & 255) / 255.0F;
        float f1 = (float)(startColor >> 8 & 255) / 255.0F;
        float f2 = (float)(startColor & 255) / 255.0F;
        float f3 = (float)(startColor >> 24 & 255) / 255.0F;
        float f4 = (float)(endColor >> 16 & 255) / 255.0F;
        float f5 = (float)(endColor >> 8 & 255) / 255.0F;
        float f6 = (float)(endColor & 255) / 255.0F;
        float f7 = (float)(endColor >> 24 & 255) / 255.0F;
        GlStateManager.disableTexture();
        GlStateManager.enableBlend();
        GlStateManager.disableAlphaTest();
        GlStateManager.blendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(7425);
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glColor4f(f, f1, f2, f3);
        GL11.glVertex2f(x, y);
        GL11.glColor4f(f4, f5, f6, f7);
        for (int i = 0; i <= 360; i += 10) {
            double angle = Math.toRadians(i);
            GL11.glVertex2d(x + Math.sin(angle) * radius, y + Math.cos(angle) * radius);
        }
        GL11.glEnd();
        GlStateManager.shadeModel(7424);
        GlStateManager.disableBlend();
        GlStateManager.enableAlphaTest();
        GlStateManager.enableTexture();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        float size = 24;
        float x = posX + windowWidth - size - 14;
        float y = posY + 13;
        if (RenderUtility.isInRegion(mx, my, x, y, size, size)) {
            closePressAnim = new Animation().animate(1, 0.1f, Easings.QUAD_OUT);
            closeRipple = new ButtonRipple((float) mx, (float) my);
            minecraft.displayGuiScreen(null);
            return true;
        }
        float gap = 8;
        float clearX = posX + windowWidth - size - 14 - size - gap;
        float clearY = posY + 13;
        if (RenderUtility.isInRegion(mx, my, clearX, clearY, size, size)) {
            clearPressAnim = new Animation().animate(1, 0.1f, Easings.QUAD_OUT);
            clearRipple = new ButtonRipple((float) mx, (float) my);
            manager.clearTransactionLogs();
            scroll = 0;
            return true;
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        closePressAnim = closePressAnim.animate(0, 0.35f, Easings.EXPO_OUT);
        clearPressAnim = clearPressAnim.animate(0, 0.35f, Easings.EXPO_OUT);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        float logsTop = posY + 122;
        float logsHeight = windowHeight - 138;
        float logsX = posX + 16;
        float logsW = windowWidth - 32;

        if (RenderUtility.isInRegion(mx, my, logsX, logsTop, logsW, logsHeight)) {
            List<TransactionLog> logs = manager.getTransactionLogs();
            float itemH = 64;
            float gap = 7;
            float contentH = logs.size() * (itemH + gap);
            float maxScroll = Math.max(0, contentH - logsHeight + 16);
            scroll = Math.max(-maxScroll, Math.min(0, scroll + (float)(delta * 30)));
            return true;
        }
        return false;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private static class ButtonRipple {
        float startX, startY;
        long startTime;
        public ButtonRipple(float x, float y) { this.startX = x; this.startY = y; this.startTime = System.currentTimeMillis(); }
        public float getProgress() { return Math.min((System.currentTimeMillis() - startTime) / 600f, 1f); }
        public boolean isFinished() { return System.currentTimeMillis() - startTime > 600; }
    }
}
