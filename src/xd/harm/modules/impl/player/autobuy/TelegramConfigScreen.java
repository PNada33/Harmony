package xd.harm.modules.impl.player.autobuy;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import org.lwjgl.opengl.GL11;
import xd.harm.modules.impl.render.Theme;
import xd.harm.utils.render.GaussianBlur;
import xd.harm.utils.render.color.ColorUtils;
import xd.harm.utils.render.font.Fonts;
import xd.harm.utils.render.gl.Stencil;
import xd.harm.utils.render.rect.RenderUtility;
import xd.harm.utils.text.GradientUtil;
import ru.hogoshi.Animation;
import ru.hogoshi.util.Easings;

public class TelegramConfigScreen extends Screen {
    private final AutoBuyManager manager;

    private float posX, posY;
    private float windowWidth = 500;
    private float windowHeight = 270;

    private static final float INPUT_X_OFFSET = 20;
    private static final float INPUT_Y_OFFSET = 72;
    private static final float INPUT_H = 34;
    private static final float ICON_W = 28;

    private Animation openAnim = new Animation();
    private Animation saveBtnAnim = new Animation();
    private Animation testBtnAnim = new Animation();
    private Animation closeBtnAnim = new Animation();
    private Animation savePressAnim = new Animation();
    private Animation testPressAnim = new Animation();
    private Animation closePressAnim = new Animation();

    private ButtonRipple saveRipple = null;
    private ButtonRipple testRipple = null;
    private ButtonRipple closeRipple = null;

    private ExtendedTextField tokenField;

    public TelegramConfigScreen(AutoBuyManager manager) {
        super(new StringTextComponent("Telegram Config"));
        this.manager = manager;
    }

    private float getFieldX() { return posX + INPUT_X_OFFSET + ICON_W + 6; }
    private float getFieldY() { return posY + INPUT_Y_OFFSET + 12; }
    private float getFieldW() { return windowWidth - INPUT_X_OFFSET * 2 - ICON_W - 10; }
    private float getFieldH() { return INPUT_H; }

    private void syncFieldPosition() {
        if (tokenField == null) return;
        tokenField.x = (int) getFieldX();
        tokenField.y = (int) getFieldY();
        tokenField.setWidth((int) getFieldW());
    }

    @Override
    protected void init() {
        openAnim = new Animation().animate(1, 0.5f, Easings.CIRC_OUT);
        saveBtnAnim = new Animation();
        testBtnAnim = new Animation();
        closeBtnAnim = new Animation();
        savePressAnim = new Animation();
        testPressAnim = new Animation();
        closePressAnim = new Animation();

        posX = (width - windowWidth) / 2f;
        posY = (height - windowHeight) / 2f;

        int hiddenColor = 0x01000000;
        tokenField = new ExtendedTextField(
                font,
                (int) getFieldX(),
                (int) getFieldY(),
                (int) getFieldW(),
                (int) getFieldH(),
                StringTextComponent.EMPTY
        );
        tokenField.setMaxStringLength(200);
        tokenField.setEnableBackgroundDrawing(false);
        tokenField.setTextColor(hiddenColor);
        tokenField.setDisabledTextColour(hiddenColor);
        tokenField.setText(manager.getTelegramNotifier().getToken());
    }

    @Override
    public void tick() {
        posX = (width - windowWidth) / 2f;
        posY = (height - windowHeight) / 2f;
        syncFieldPosition();
        if (tokenField != null) tokenField.tick();
    }

    @Override
    public void render(MatrixStack stack, int mx, int my, float pt) {
        openAnim.update();
        saveBtnAnim.update();
        testBtnAnim.update();
        closeBtnAnim.update();
        savePressAnim.update();
        testPressAnim.update();
        closePressAnim.update();

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
        renderFields(stack, alpha);
        renderButtons(stack, mx, my, alpha);
        renderCloseButton(stack, mx, my, alpha);

        GlStateManager.popMatrix();
        super.render(stack, mx, my, pt);
    }

    private void renderBackgroundEffects(float alpha) {
        float time = System.currentTimeMillis() / 2000f;
        int tgColor = ColorUtils.rgba(40, 160, 240, 255);

        Stencil.initStencilToWrite();
        RenderUtility.drawRoundedRect(posX, posY, windowWidth, windowHeight, 14, -1);
        Stencil.readStencilBuffer(1);

        float orbX = posX + windowWidth * 0.85f + (float) Math.sin(time) * 25;
        float orbY = posY + windowHeight * 0.1f + (float) Math.cos(time * 0.8) * 20;
        drawRadialGradient(orbX, orbY, 160, ColorUtils.setAlpha(tgColor, (int)(30 * alpha)), ColorUtils.rgba(0, 0, 0, 0));

        Stencil.uninitStencilBuffer();
    }

    private void renderHeader(MatrixStack stack, float alpha) {
        Fonts.sfuy.drawText(stack, GradientUtil.gradient("\uD83D\uDCE8  Настройка Telegram"), posX + 16, posY + 15, 13.8f, (int)(255 * alpha));
        Fonts.sfuy.drawText(stack, "Получайте уведомления о покупках и продажах", posX + 17, posY + 32, ColorUtils.rgba(145, 145, 158, (int)(205 * alpha)), 6.5f);
    }

    private void renderFields(MatrixStack stack, float alpha) {
        int themeColor = Theme.MainColor(0);
        float x = posX + INPUT_X_OFFSET;
        float y = posY + INPUT_Y_OFFSET;
        float w = windowWidth - INPUT_X_OFFSET * 2;
        drawStyledInput(stack, tokenField, "Bot Token", "\uD83D\uDD11", x, y, w, alpha, themeColor);

        float hintY = posY + 158;
        String[] lines = {
                "\u2460  Создайте бота через @BotFather и получите токен",
                "\u2461  Напишите боту /start чтобы активировать уведомления"
        };
        for (String line : lines) {
            Fonts.sfuy.drawText(stack, line, posX + 20, hintY, ColorUtils.rgba(115, 115, 128, (int)(190 * alpha)), 6.5f);
            hintY += 12;
        }
    }

    private void drawStyledInput(MatrixStack stack, ExtendedTextField f, String label, String icon,
                                 float x, float y, float w, float alpha, int themeColor) {
        if (f == null) return;
        boolean focused = f.isFocused();
        float h = INPUT_H;
        float boxY = y + 12;

        Fonts.sfuy.drawText(stack, label, x, y, ColorUtils.rgba(175, 175, 188, (int)(220 * alpha)), 7f);

        int bg = focused
                ? ColorUtils.rgba(25, 25, 32, (int)(255 * alpha))
                : ColorUtils.rgba(20, 20, 26, (int)(200 * alpha));
        RenderUtility.drawRoundedRect(x, boxY, w, h, 7, bg);

        int border = focused ? themeColor : ColorUtils.rgba(55, 55, 68, 255);
        RenderUtility.drawRoundedRectOutline(x, boxY, w, h, 7, 1f, ColorUtils.setAlpha(border, (int)(160 * alpha)));

        if (focused) {
            RenderUtility.drawShadow(x, boxY, w, h, 8, ColorUtils.setAlpha(themeColor, (int)(60 * alpha)));
        }

        RenderUtility.drawRectW(x + ICON_W, boxY + 4, 1, h - 8, ColorUtils.rgba(255, 255, 255, (int)(18 * alpha)));
        Fonts.sfuy.drawCenteredText(stack, icon, x + ICON_W / 2, boxY + (h - 8) / 2 - 3,
                focused ? themeColor : ColorUtils.rgba(120, 120, 135, 255), 9f);

        String value = f.getText();
        if (value == null) value = "";

        float valueY = boxY + (h - 7f) / 2f;
        float textX = x + ICON_W + 6;

        int color = focused
                ? ColorUtils.rgba(240, 240, 248, (int)(255 * alpha))
                : ColorUtils.rgba(175, 175, 188, (int)(220 * alpha));
        if (focused) color = ColorUtils.interpolateColor(color, themeColor, 0.12f);

        if (!value.isEmpty()) {
            Fonts.sfuy.drawText(stack, value, textX, valueY, color, 7f);
        } else if (!focused) {
            Fonts.sfuy.drawText(stack, "Вставьте токен сюда...", textX, valueY,
                    ColorUtils.rgba(80, 80, 95, (int)(180 * alpha)), 7f);
        }

        if (focused && (System.currentTimeMillis() / 450L) % 2L == 0L) {
            float caretX = textX + (value.isEmpty() ? 0 : Fonts.sfuy.getWidth(value, 7f)) + 0.8f;
            RenderUtility.drawRectW(caretX, valueY - 0.2f, 0.85f, 9f,
                    ColorUtils.setAlpha(themeColor, (int)(220 * alpha)));
        }
    }

    private void renderButtons(MatrixStack stack, int mx, int my, float alpha) {
        float btnW = 145;
        float btnH = 34;
        float gap = 10;
        float totalW = btnW * 2 + gap;
        float startX = posX + (windowWidth - totalW) / 2;
        float btnY = posY + windowHeight - btnH - 18;

        int themeColor = Theme.MainColor(0);
        int tgColor = ColorUtils.rgba(40, 160, 240, 255);

        boolean saveHover = RenderUtility.isInRegion(mx, my, startX, btnY, btnW, btnH);
        saveBtnAnim.animate(saveHover ? 1 : 0, 0.2f, Easings.EXPO_OUT);
        drawStyledButton(stack, "\uD83D\uDCBE  СОХРАНИТЬ", startX, btnY, btnW, btnH,
                (float) saveBtnAnim.getValue(), (float) savePressAnim.getValue(), alpha, themeColor, saveRipple);

        boolean testHover = RenderUtility.isInRegion(mx, my, startX + btnW + gap, btnY, btnW, btnH);
        testBtnAnim.animate(testHover ? 1 : 0, 0.2f, Easings.EXPO_OUT);
        drawStyledButton(stack, "\uD83D\uDCE4  ТЕСТ", startX + btnW + gap, btnY, btnW, btnH,
                (float) testBtnAnim.getValue(), (float) testPressAnim.getValue(), alpha, tgColor, testRipple);
    }

    private void drawStyledButton(MatrixStack stack, String text, float x, float y, float w, float h,
                                  float hover, float press, float alpha, int color, ButtonRipple ripple) {
        float time = System.currentTimeMillis() / 1000f;
        float cx = x + w / 2;
        float cy = y + h / 2;
        float scale = 1.0f + (hover * 0.04f) - (press * 0.03f);

        GlStateManager.pushMatrix();
        GlStateManager.translatef(cx, cy, 0);
        GlStateManager.scalef(scale, scale, 1);
        GlStateManager.translatef(-cx, -cy, 0);

        if (hover > 0.01f) {
            float glowOp = hover * 0.5f * alpha;
            for (int i = 1; i <= 3; i++) {
                RenderUtility.drawRoundedRect(x - i * 2, y - i * 2, w + i * 4, h + i * 4, 8 + i,
                        ColorUtils.setAlpha(color, (int)(35 * glowOp / i)));
            }
        }

        RenderUtility.drawShadow(x, y + 2, w, h, 8, ColorUtils.rgba(0, 0, 0, (int)(100 * alpha)));
        RenderUtility.drawRoundedRect(x, y, w, h, 7, ColorUtils.rgba(20, 20, 26, (int)(255 * alpha)));

        Stencil.initStencilToWrite();
        RenderUtility.drawRoundedRect(x, y, w, h, 7, -1);
        Stencil.readStencilBuffer(1);

        for (float i = 0; i < w; i += 2) {
            float perc = i / w;
            float wave = (float) Math.sin(perc * 4.0 + time * 2.0) * 0.5f + 0.5f;
            int c1 = ColorUtils.setAlpha(color, (int)(255 * alpha));
            int c2 = ColorUtils.setAlpha(ColorUtils.getOppositeColor(color), (int)(180 * alpha));
            int mid = ColorUtils.interpolateColor(c1, c2, wave);
            float alphaMod = 0.08f + hover * 0.18f;
            RenderUtility.drawRectW(x + i, y, 2, h, ColorUtils.setAlpha(mid, (int)(255 * alphaMod * alpha)));
        }

        if (ripple != null && !ripple.isFinished()) {
            float p = ripple.getProgress();
            float rad = w * 1.2f * p;
            drawRadialGradient(ripple.startX, ripple.startY, rad,
                    ColorUtils.rgba(255, 255, 255, (int)(150 * (1 - p) * alpha)),
                    ColorUtils.rgba(255, 255, 255, 0));
        }

        Stencil.uninitStencilBuffer();

        int borderBase = ColorUtils.rgba(55, 55, 68, (int)(255 * alpha));
        int borderHover = ColorUtils.setAlpha(color, (int)(255 * alpha));
        RenderUtility.drawRoundedRectOutline(x, y, w, h, 7, 1f, ColorUtils.interpolateColor(borderBase, borderHover, hover));

        GlStateManager.enableTexture();
        Fonts.sfuy.drawCenteredText(stack, text, cx, cy - 3.5f, -1, 8.5f);
        if (hover > 0.1f) {
            Fonts.sfuy.drawCenteredText(stack, text, cx, cy - 3.5f,
                    ColorUtils.setAlpha(color, (int)(140 * hover * alpha)), 8.5f);
        }
        GlStateManager.popMatrix();
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
        RenderUtility.drawRoundedRectOutline(x, y, size, size, 7, 1f,
                ColorUtils.interpolateColor(borderBase, borderHover, anim));

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

    private boolean isOverInputBox(double mx, double my) {
        float x = posX + INPUT_X_OFFSET;
        float y = posY + INPUT_Y_OFFSET + 12;
        float w = windowWidth - INPUT_X_OFFSET * 2;
        return mx >= x && mx <= x + w && my >= y && my <= y + INPUT_H;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (tokenField != null) {
            tokenField.setFocused2(isOverInputBox(mx, my));
        }

        float btnW = 145;
        float btnH = 34;
        float gap = 10;
        float totalW = btnW * 2 + gap;
        float startX = posX + (windowWidth - totalW) / 2;
        float btnY = posY + windowHeight - btnH - 18;

        if (RenderUtility.isInRegion(mx, my, startX, btnY, btnW, btnH)) {
            savePressAnim = new Animation().animate(1, 0.1f, Easings.QUAD_OUT);
            saveRipple = new ButtonRipple((float) mx, (float) my);
            saveConfig();
            minecraft.displayGuiScreen(null);
            return true;
        }

        if (RenderUtility.isInRegion(mx, my, startX + btnW + gap, btnY, btnW, btnH)) {
            testPressAnim = new Animation().animate(1, 0.1f, Easings.QUAD_OUT);
            testRipple = new ButtonRipple((float) mx, (float) my);
            testTelegram();
            return true;
        }

        float size = 24;
        float x = posX + windowWidth - size - 14;
        float y = posY + 13;
        if (RenderUtility.isInRegion(mx, my, x, y, size, size)) {
            closePressAnim = new Animation().animate(1, 0.1f, Easings.QUAD_OUT);
            closeRipple = new ButtonRipple((float) mx, (float) my);
            minecraft.displayGuiScreen(null);
            return true;
        }

        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        savePressAnim = savePressAnim.animate(0, 0.35f, Easings.EXPO_OUT);
        testPressAnim = testPressAnim.animate(0, 0.35f, Easings.EXPO_OUT);
        closePressAnim = closePressAnim.animate(0, 0.35f, Easings.EXPO_OUT);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void testTelegram() {
        if (tokenField == null || tokenField.getText().isEmpty()) return;
        manager.getTelegramNotifier().setToken(tokenField.getText());
        manager.saveTelegramConfig();

        TransactionLog testLog = new TransactionLog(
                TransactionLog.Type.BUY,
                net.minecraft.item.ItemStack.EMPTY,
                "Тестовый предмет",
                1,
                1000,
                java.time.LocalDateTime.now(),
                "test.server",
                "TestPlayer"
        );
        manager.getTelegramNotifier().sendTransaction(testLog);
    }

    private void saveConfig() {
        if (tokenField != null) manager.getTelegramNotifier().setToken(tokenField.getText());
        manager.saveTelegramConfig();
    }

    @Override
    public boolean keyPressed(int key, int scan, int mod) {
        if (key == 256) { minecraft.displayGuiScreen(null); return true; }
        if (tokenField != null && tokenField.isFocused()) return tokenField.keyPressed(key, scan, mod);
        return super.keyPressed(key, scan, mod);
    }

    @Override
    public boolean charTyped(char c, int mod) {
        if (tokenField != null && tokenField.isFocused()) return tokenField.charTyped(c, mod);
        return false;
    }

    @Override
    public void onClose() { saveConfig(); }

    @Override
    public boolean isPauseScreen() { return false; }

    private static class ExtendedTextField extends TextFieldWidget {
        public ExtendedTextField(FontRenderer font, int x, int y, int width, int height, ITextComponent title) {
            super(font, x, y, width, height, title);
        }
    }

    private static class ButtonRipple {
        float startX, startY;
        long startTime;
        public ButtonRipple(float x, float y) {
            this.startX = x;
            this.startY = y;
            this.startTime = System.currentTimeMillis();
        }
        public float getProgress() { return Math.min((System.currentTimeMillis() - startTime) / 600f, 1f); }
        public boolean isFinished() { return System.currentTimeMillis() - startTime > 600; }
    }
}
