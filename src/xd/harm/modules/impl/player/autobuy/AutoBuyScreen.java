package xd.harm.modules.impl.player.autobuy;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import org.lwjgl.opengl.GL11;
import xd.harm.modules.impl.player.AutoBuy;
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static xd.harm.utils.client.IMinecraft.mc;

public class AutoBuyScreen extends Screen {

    private final AutoBuyManager manager;
    private final AutoBuy module;

    private float posX, posY;
    private float windowWidth = 550;
    private float windowHeight = 330;

    private int selectedIndex = 0;
    private ExtendedTextField buyField, sellField;

    private Animation openAnim = new Animation();
    private final Animation[] itemAnims;
    private Animation saveBtnAnim = new Animation();
    private Animation parseBtnAnim = new Animation();
    private Animation logsBtnAnim = new Animation();
    private Animation telegramBtnAnim = new Animation();
    private Animation closeBtnAnim = new Animation();

    private Animation savePressAnim = new Animation();
    private Animation parsePressAnim = new Animation();
    private Animation logsPressAnim = new Animation();
    private Animation telegramPressAnim = new Animation();
    private Animation closePressAnim = new Animation();

    private ButtonRipple saveRipple = null;
    private ButtonRipple parseRipple = null;
    private ButtonRipple logsRipple = null;
    private ButtonRipple telegramRipple = null;
    private ButtonRipple closeRipple = null;

    private float scroll = 0;
    private float animatedScroll = 0;
    private float itemRotation = 0;
    private final List<Ripple> ripples = new ArrayList<>();

    private static final float ITEM_SIZE = 34;
    private static final float ITEM_GAP = 8;
    private static final int COLS = 7;

    private static final float BTN_SIZE = 26;
    private static final float BTN_GAP = 6;

    public AutoBuyScreen(AutoBuyManager manager, AutoBuy module) {
        super(new StringTextComponent("AutoBuy"));
        this.manager = manager;
        this.module = module;

        itemAnims = new Animation[manager.getItems().size()];
        for (int i = 0; i < itemAnims.length; i++) {
            itemAnims[i] = new Animation();
        }
    }

    @Override
    protected void init() {
        buyField = null;
        sellField = null;
        openAnim = new Animation().animate(1, 0.5f, Easings.CIRC_OUT);
        saveBtnAnim = new Animation();
        parseBtnAnim = new Animation();
        logsBtnAnim = new Animation();
        telegramBtnAnim = new Animation();
        closeBtnAnim = new Animation();
        savePressAnim = new Animation();
        parsePressAnim = new Animation();
        logsPressAnim = new Animation();
        telegramPressAnim = new Animation();
        closePressAnim = new Animation();
        ripples.clear();
        if (selectedIndex == -1 && !manager.getItems().isEmpty()) selectedIndex = 0;
    }

    @Override
    public void tick() {
        posX = (width - windowWidth) / 2f;
        posY = (height - windowHeight) / 2f;

        if (selectedIndex >= 0 && selectedIndex < manager.getItems().size()) {
            AutoBuyItem item = manager.getItems().get(selectedIndex);

            float leftPanelW = 320;
            float rightPanelX = posX + leftPanelW;
            float rightPanelW = windowWidth - leftPanelW;
            float bottomAreaY = posY + windowHeight - 95;

            float padding = 15;
            float gap = 10;
            float elementW = (rightPanelW - (padding * 2) - gap) / 2;

            float x1 = rightPanelX + padding;
            float x2 = rightPanelX + padding + elementW + gap;

            float iconOffset = 22;

            if (buyField == null) {
                buyField = new ExtendedTextField(font, (int)(x1 + iconOffset), (int)bottomAreaY + 20, (int)(elementW - iconOffset - 4), 18, StringTextComponent.EMPTY);
                buyField.setMaxStringLength(15);
                buyField.setEnableBackgroundDrawing(false);
                int hiddenInputText = 0x01000000;
                buyField.setTextColor(hiddenInputText);
                buyField.setDisabledTextColour(hiddenInputText);
                buyField.setText(item.buyPrice > 0 ? String.valueOf(item.buyPrice) : "");
            }
            if (sellField == null) {
                sellField = new ExtendedTextField(font, (int)(x2 + iconOffset), (int)bottomAreaY + 20, (int)(elementW - iconOffset - 4), 18, StringTextComponent.EMPTY);
                sellField.setMaxStringLength(15);
                sellField.setEnableBackgroundDrawing(false);
                int hiddenInputText = 0x01000000;
                sellField.setTextColor(hiddenInputText);
                sellField.setDisabledTextColour(hiddenInputText);
                sellField.setText(item.sellPrice > 0 ? String.valueOf(item.sellPrice) : "");
            }

            buyField.x = (int)(x1 + iconOffset);
            buyField.y = (int)bottomAreaY + 20;
            buyField.setWidth((int) (elementW - iconOffset - 4));

            sellField.x = (int)(x2 + iconOffset);
            sellField.y = (int)bottomAreaY + 20;
            sellField.setWidth((int) (elementW - iconOffset - 4));

            buyField.tick();
            sellField.tick();
        } else {
            buyField = null;
            sellField = null;
        }
    }

    private float getLogsButtonX() {
        return posX + windowWidth - (BTN_SIZE * 3 + BTN_GAP * 2 + 14);
    }

    private float getTelegramButtonX() {
        return getLogsButtonX() + BTN_SIZE + BTN_GAP;
    }

    private float getHeaderButtonY() {
        return posY + 13;
    }

    @Override
    public void render(MatrixStack stack, int mx, int my, float pt) {
        itemRotation += pt * 0.5f;
        openAnim.update();
        saveBtnAnim.update();
        parseBtnAnim.update();
        logsBtnAnim.update();
        telegramBtnAnim.update();
        closeBtnAnim.update();
        savePressAnim.update();
        parsePressAnim.update();
        logsPressAnim.update();
        telegramPressAnim.update();
        closePressAnim.update();

        if (itemAnims != null) for (Animation a : itemAnims) if (a != null) a.update();
        animatedScroll = MathUtil.fast(animatedScroll, scroll, 15);

        float animValue = (float) openAnim.getValue();
        float alpha = MathHelper.clamp(animValue, 0, 1);

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

        renderBackgroundEffects(posX, posY, windowWidth, windowHeight, alpha);
        renderRipples(alpha);

        float leftPanelWidth = 320;

        renderHeader(stack, mx, my, alpha);

        float gridTop = posY + 45;
        float gridHeight = windowHeight - 60;

        Stencil.initStencilToWrite();
        RenderUtility.drawRoundedRect(posX + 10, gridTop, leftPanelWidth - 10, gridHeight, 4, -1);
        Stencil.readStencilBuffer(1);

        renderGrid(stack, mx, my, posX + 16, gridTop + 5 + animatedScroll, alpha);

        float scrollH = gridHeight - 10;
        float contentH = (float) Math.ceil(manager.getItems().size() / (float)COLS) * (ITEM_SIZE + ITEM_GAP);
        if (contentH > scrollH) {
            float scrollBarH = (scrollH / contentH) * scrollH;
            float scrollY = (-animatedScroll / contentH) * scrollH;
            RenderUtility.drawRoundedRect(posX + leftPanelWidth - 4, gridTop + 5 + scrollY, 2f, scrollBarH, 1f, ColorUtils.rgba(255, 255, 255, (int)(40 * alpha)));
        }

        Stencil.uninitStencilBuffer();

        float dividerX = posX + leftPanelWidth;
        RenderUtility.drawRectW(dividerX, posY + 15, 1, windowHeight - 30, ColorUtils.rgba(40, 40, 50, (int)(100 * alpha)));

        float rightPanelX = posX + leftPanelWidth;
        float rightPanelW = windowWidth - leftPanelWidth;

        if (selectedIndex >= 0 && selectedIndex < manager.getItems().size()) {
            renderInspector(stack, mx, my, rightPanelX, posY, rightPanelW, windowHeight, alpha);
        } else {
            Fonts.sfuy.drawCenteredText(stack, "Select an item to edit", rightPanelX + rightPanelW / 2, posY + windowHeight / 2, ColorUtils.rgba(80, 80, 90, (int)(150 * alpha)), 9f);
        }

        renderCloseButton(stack, mx, my, alpha);

        GlStateManager.popMatrix();

        renderTooltips(stack, mx, my, alpha);

        super.render(stack, mx, my, pt);
    }

    private void renderTooltips(MatrixStack stack, int mx, int my, float alpha) {
        float btnY = getHeaderButtonY();

        if (RenderUtility.isInRegion(mx, my, getLogsButtonX(), btnY, BTN_SIZE, BTN_SIZE)) {
            drawTooltip(stack, mx, my, "Логи транзакций", alpha);
        } else if (RenderUtility.isInRegion(mx, my, getTelegramButtonX(), btnY, BTN_SIZE, BTN_SIZE)) {
            drawTooltip(stack, mx, my, "Telegram бот", alpha);
        }
    }

    private void drawTooltip(MatrixStack stack, int mx, int my, String text, float alpha) {
        float tw = Fonts.sfuy.getWidth(text, 7f) + 12;
        float th = 16;
        float tx = mx - tw / 2f;
        float ty = my + 14;

        RenderUtility.drawShadow(tx, ty, tw, th, 8, ColorUtils.rgba(0, 0, 0, (int)(80 * alpha)));
        RenderUtility.drawRoundedRect(tx, ty, tw, th, 4, ColorUtils.rgba(22, 22, 28, (int)(230 * alpha)));
        RenderUtility.drawRoundedRectOutline(tx, ty, tw, th, 4, 1f, ColorUtils.setAlpha(Theme.MainColor(0), (int)(150 * alpha)));
        Fonts.sfuy.drawCenteredText(stack, text, tx + tw / 2f, ty + 4.5f, ColorUtils.rgba(210, 210, 220, (int)(230 * alpha)), 7f);
    }

    private void renderRipples(float alpha) {
        Stencil.initStencilToWrite();
        RenderUtility.drawRoundedRect(posX, posY, windowWidth, windowHeight, 12, -1);
        Stencil.readStencilBuffer(1);
        Iterator<Ripple> iterator = ripples.iterator();
        while (iterator.hasNext()) {
            Ripple r = iterator.next();
            r.update();
            if (r.isFinished()) { iterator.remove(); continue; }
            float rVal = (float) r.animation.getValue();
            float radius = r.radius * rVal;
            int color = ColorUtils.setAlpha(Theme.MainColor(0), (int)(100 * (1 - rVal) * alpha));
            drawRadialGradient(r.x, r.y, radius, color, ColorUtils.setAlpha(color, 0));
        }
        Stencil.uninitStencilBuffer();
    }

    private void renderBackgroundEffects(float x, float y, float w, float h, float alpha) {
        float time = System.currentTimeMillis() / 2000f;
        Stencil.initStencilToWrite();
        RenderUtility.drawRoundedRect(x, y, w, h, 14, -1);
        Stencil.readStencilBuffer(1);

        float orbX = x + w * 0.85f + (float)Math.sin(time) * 40;
        float orbY = y + h * 0.15f + (float)Math.cos(time * 0.8) * 30;
        drawRadialGradient(orbX, orbY, 200, ColorUtils.setAlpha(Theme.MainColor(0), (int)(35 * alpha)), ColorUtils.rgba(0, 0, 0, 0));

        float orb2X = x + w * 0.15f - (float)Math.cos(time * 0.5) * 50;
        float orb2Y = y + h * 0.85f - (float)Math.sin(time * 0.7) * 40;
        drawRadialGradient(orb2X, orb2Y, 220, ColorUtils.setAlpha(ColorUtils.getOppositeColor(Theme.MainColor(0)), (int)(30 * alpha)), ColorUtils.rgba(0, 0, 0, 0));

        Stencil.uninitStencilBuffer();
    }

    private void renderHeader(MatrixStack stack, int mx, int my, float alpha) {
        Fonts.sfuy.drawText(stack, GradientUtil.gradient("AutoBuy"), posX + 16, posY + 15, 13.8f, (int)(255 * alpha));
        Fonts.sfuy.drawText(stack, manager.getItems().size() + " Items loaded", posX + 17, posY + 32, ColorUtils.rgba(145, 145, 158, (int)(205 * alpha)), 6.5f);

        float btnY = getHeaderButtonY();

        renderHeaderButton(
                stack, mx, my,
                "\u2630",
                "Логи",
                getLogsButtonX(), btnY, BTN_SIZE,
                logsBtnAnim, logsPressAnim, logsRipple,
                alpha,
                Theme.MainColor(0)
        );

        renderHeaderButton(
                stack, mx, my,
                "\u2709",
                "TG",
                getTelegramButtonX(), btnY, BTN_SIZE,
                telegramBtnAnim, telegramPressAnim, telegramRipple,
                alpha,
                ColorUtils.rgba(40, 160, 240, 255)
        );
    }

    private void renderHeaderButton(MatrixStack stack, int mx, int my,
                                    String icon, String label,
                                    float x, float y, float size,
                                    Animation hoverAnim, Animation pressAnim,
                                    ButtonRipple ripple, float alpha, int accentColor) {
        boolean hover = RenderUtility.isInRegion(mx, my, x, y, size, size);
        hoverAnim.animate(hover ? 1 : 0, 0.2f, Easings.EXPO_OUT);
        float anim = (float) hoverAnim.getValue();
        float press = (float) pressAnim.getValue();

        float cx = x + size / 2;
        float cy = y + size / 2;

        GlStateManager.pushMatrix();
        GlStateManager.translatef(cx, cy, 0);
        float scale = 1.0f + (anim * 0.1f) - (press * 0.06f);
        GlStateManager.scalef(scale, scale, 1);
        GlStateManager.translatef(-cx, -cy, 0);

        float time = System.currentTimeMillis() / 1000f;

        int bgBase = ColorUtils.rgba(20, 20, 26, (int)(255 * alpha));
        int bgHover = ColorUtils.rgba(26, 26, 34, (int)(255 * alpha));
        RenderUtility.drawShadow(x, y, size, size, 8, ColorUtils.rgba(0, 0, 0, (int)(80 * alpha)));
        RenderUtility.drawRoundedRect(x, y, size, size, 7, ColorUtils.interpolateColor(bgBase, bgHover, anim));

        Stencil.initStencilToWrite();
        RenderUtility.drawRoundedRect(x, y, size, size, 7, -1);
        Stencil.readStencilBuffer(1);

        if (anim > 0.01f) {
            for (float i = 0; i < size; i += 2) {
                float perc = i / size;
                float wave = (float) Math.sin(perc * 4.0 + time * 2.0) * 0.5f + 0.5f;
                int c1 = ColorUtils.setAlpha(accentColor, (int)(255 * alpha));
                int c2 = ColorUtils.setAlpha(ColorUtils.getOppositeColor(accentColor), (int)(180 * alpha));
                int mid = ColorUtils.interpolateColor(c1, c2, wave);
                float alphaMod = anim * 0.22f;
                RenderUtility.drawRectW(x + i, y, 2, size, ColorUtils.setAlpha(mid, (int)(255 * alphaMod * alpha)));
            }
        }

        if (ripple != null && !ripple.isFinished()) {
            float p = ripple.getProgress();
            float rad = size * 1.5f * p;
            drawRadialGradient(ripple.startX, ripple.startY, rad,
                    ColorUtils.rgba(255, 255, 255, (int)(180 * (1 - p) * alpha)),
                    ColorUtils.rgba(255, 255, 255, 0));
        }

        Stencil.uninitStencilBuffer();

        int borderBase = ColorUtils.rgba(48, 48, 58, (int)(255 * alpha));
        RenderUtility.drawRoundedRectOutline(x, y, size, size, 7, 1f,
                ColorUtils.interpolateColor(borderBase, ColorUtils.setAlpha(accentColor, (int)(255 * alpha)), anim));

        if (anim > 0.01f) {
            RenderUtility.drawShadow(x, y, size, size, 10, ColorUtils.setAlpha(accentColor, (int)(90 * anim * alpha)));
        }

        int iconColorBase = ColorUtils.rgba(155, 155, 168, (int)(255 * alpha));
        int iconColorHover = ColorUtils.rgba(255, 255, 255, (int)(255 * alpha));
        int iconColor = ColorUtils.interpolateColor(iconColorBase, iconColorHover, anim);

        Fonts.sfuy.drawCenteredText(stack, icon, cx, cy - 4, iconColor, 12f);

        GlStateManager.popMatrix();
    }

    private void renderGrid(MatrixStack stack, int mx, int my, float startX, float startY, float alpha) {
        int themeColor = Theme.MainColor(0);
        float visibleTop = posY + 45;
        float visibleBottom = posY + windowHeight - 15;

        for (int i = 0; i < manager.getItems().size(); i++) {
            AutoBuyItem item = manager.getItems().get(i);
            int col = i % COLS;
            int row = i / COLS;
            float itemX = startX + col * (ITEM_SIZE + ITEM_GAP);
            float itemY = startY + row * (ITEM_SIZE + ITEM_GAP);

            if (itemY + ITEM_SIZE < visibleTop || itemY > visibleBottom) continue;

            boolean selected = selectedIndex == i;
            boolean hovered = RenderUtility.isInRegion(mx, my, itemX, itemY, ITEM_SIZE, ITEM_SIZE);

            if (itemAnims[i] != null) itemAnims[i].animate(hovered || selected ? 1 : 0, 0.25f, Easings.EXPO_OUT);
            float anim = (float) (itemAnims[i] != null ? itemAnims[i].getValue() : 0);

            int bgCol = ColorUtils.rgba(25, 25, 30, (int)(180 * alpha));
            int selCol = ColorUtils.setAlpha(themeColor, (int)(40 * alpha));

            GlStateManager.pushMatrix();
            float scale = 1f + anim * 0.08f;
            GlStateManager.translatef(itemX + ITEM_SIZE / 2f, itemY + ITEM_SIZE / 2f, 0);
            GlStateManager.scalef(scale, scale, 1);
            GlStateManager.translatef(-(itemX + ITEM_SIZE / 2f), -(itemY + ITEM_SIZE / 2f), 0);

            if (selected) {
                RenderUtility.drawShadow(itemX, itemY, ITEM_SIZE, ITEM_SIZE, 10, ColorUtils.setAlpha(themeColor, (int)(80 * alpha)));
                RenderUtility.drawRoundedRect(itemX, itemY, ITEM_SIZE, ITEM_SIZE, 8, selCol);
                RenderUtility.drawRoundedRectOutline(itemX, itemY, ITEM_SIZE, ITEM_SIZE, 8, 1f, ColorUtils.setAlpha(themeColor, (int)(200 * alpha)));
            } else {
                RenderUtility.drawRoundedRect(itemX, itemY, ITEM_SIZE, ITEM_SIZE, 8, bgCol);
                if (anim > 0.01f) RenderUtility.drawRoundedRectOutline(itemX, itemY, ITEM_SIZE, ITEM_SIZE, 8, 1f, ColorUtils.rgba(255, 255, 255, (int)(100 * anim * alpha)));
            }

            if (item.itemStack != null) {
                RenderSystem.pushMatrix();
                RenderSystem.translatef(itemX + ITEM_SIZE / 2, itemY + ITEM_SIZE / 2, 0);
                RenderSystem.scalef(1.1f, 1.1f, 1);
                RenderSystem.translatef(-(itemX + ITEM_SIZE / 2), -(itemY + ITEM_SIZE / 2), 0);
                minecraft.getItemRenderer().renderItemIntoGUI(item.itemStack, (int)(itemX + (ITEM_SIZE - 16) / 2), (int)(itemY + (ITEM_SIZE - 16) / 2));
                RenderSystem.popMatrix();
            }

            renderItemStateMarker(itemX, itemY, i, item, selected, alpha);
            GlStateManager.popMatrix();
        }
    }

    private void renderItemStateMarker(float itemX, float itemY, int index, AutoBuyItem item, boolean selected, float alpha) {
        if (!item.parsingEnabled && item.buyPrice <= 0) return;

        float pulse = (float) ((Math.sin(System.currentTimeMillis() / 210.0 + index * 0.45) * 0.5 + 0.5) * 0.35 + 0.65);
        int glowAlpha = (int) ((selected ? 160 : 110) * alpha * pulse);

        int accentLeft, accentRight, badgeColor;
        if (item.parsingEnabled) {
            accentLeft = ColorUtils.rgba(255, 220, 120, (int)(230 * alpha * pulse));
            accentRight = ColorUtils.rgba(255, 150, 60, (int)(230 * alpha * pulse));
            badgeColor = ColorUtils.rgba(255, 205, 90, (int)(240 * alpha));
        } else {
            accentLeft = ColorUtils.setAlpha(Theme.MainColor(index * 16), (int)(220 * alpha * pulse));
            accentRight = ColorUtils.setAlpha(Theme.MainColor(index * 16 + 130), (int)(220 * alpha * pulse));
            badgeColor = ColorUtils.setAlpha(Theme.MainColor(index * 24 + 80), (int)(230 * alpha));
        }

        RenderUtility.drawGradientRoundedOutlineHorizontal(itemX + 1.5f, itemY + 1.5f, itemX + ITEM_SIZE - 1.5f, itemY + ITEM_SIZE - 1.5f, 6.5f, 1.0f, accentLeft, accentRight);
        float bSize = 6.2f;
        float bx = itemX + ITEM_SIZE - bSize - 3f;
        float by = itemY + 3f;
        RenderUtility.drawShadow(bx, by, bSize, bSize, 6, ColorUtils.setAlpha(badgeColor, glowAlpha));
        RenderUtility.drawRoundedRect(bx, by, bSize, bSize, 2.3f, badgeColor);
    }

    private void renderInspector(MatrixStack stack, int mx, int my, float x, float y, float w, float h, float alpha) {
        AutoBuyItem item = manager.getItems().get(selectedIndex);
        int themeColor = Theme.MainColor(0);
        float centerX = x + w / 2;
        float centerY = y + 85;

        float glowAnim = (float) (Math.sin(System.currentTimeMillis() / 600f) * 0.5 + 0.5);
        drawRadialGradient(centerX, centerY, 70,
                ColorUtils.setAlpha(themeColor, (int)((30 + 20 * glowAnim) * alpha)),
                ColorUtils.rgba(0, 0, 0, 0));

        renderRotatingItem(item.itemStack, centerX, centerY, 75, itemRotation);

        String name = item.itemName;
        if (name.length() > 22) name = name.substring(0, 20) + "...";
        Fonts.sfuy.drawCenteredText(stack, name, centerX, centerY + 50, -1, 11f);

        List<String> lore = getLoreStrings(item.itemStack);
        float loreY = centerY + 65;
        for (int i = 0; i < Math.min(lore.size(), 3); i++) {
            String line = lore.get(i);
            if (line.length() > 30) line = line.substring(0, 28) + "..";
            Fonts.sfuy.drawCenteredText(stack, line, centerX, loreY, ColorUtils.rgba(180, 180, 190, (int)(180 * alpha)), 7f);
            loreY += 9;
        }

        float bottomAreaY = y + h - 100;
        float padding = 15;
        float gap = 10;
        float availableW = w - (padding * 2);
        float elementW = (availableW - gap) / 2;

        drawStyledInput(stack, buyField, "Purchase Price", "$", x + padding, bottomAreaY + 15, elementW, alpha, themeColor);
        drawStyledInput(stack, sellField, "Selling Price", "$", x + padding + elementW + gap, bottomAreaY + 15, elementW, alpha, themeColor);

        float btnY = bottomAreaY + 58;
        float btnH = 26;

        boolean saveHover = RenderUtility.isInRegion(mx, my, x + padding, btnY, elementW, btnH);
        saveBtnAnim = saveBtnAnim.animate(saveHover ? 1 : 0, 0.2f, Easings.EXPO_OUT);
        drawAbstractButton(stack, "\uD83D\uDCBE  SAVE CONFIG", x + padding, btnY, elementW, btnH,
                (float) saveBtnAnim.getValue(), (float) savePressAnim.getValue(), alpha, themeColor, saveRipple);

        boolean parseHover = RenderUtility.isInRegion(mx, my, x + padding + elementW + gap, btnY, elementW, btnH);
        parseBtnAnim = parseBtnAnim.animate(parseHover ? 1 : 0, 0.2f, Easings.EXPO_OUT);
        int stateColor = item.parsingEnabled ? themeColor : ColorUtils.rgba(140, 140, 145, 255);
        String parserIcon = item.parsingEnabled ? "\u25CF" : "\u25CB";
        drawAbstractButton(stack, parserIcon + "  PARSER: " + (item.parsingEnabled ? "ON" : "OFF"),
                x + padding + elementW + gap, btnY, elementW, btnH,
                (float) parseBtnAnim.getValue(), (float) parsePressAnim.getValue(), alpha, stateColor, parseRipple);
    }

    private void renderRotatingItem(ItemStack itemStack, float x, float y, float scale, float rotation) {
        if (itemStack == null || itemStack.isEmpty()) return;
        RenderSystem.pushMatrix();
        RenderSystem.translatef(x, y, 300);
        RenderSystem.scalef(scale, -scale, scale);
        RenderSystem.rotatef(rotation, 0, 1, 0);
        RenderSystem.rotatef(15, 1, 0, 0);
        RenderSystem.enableDepthTest();
        IRenderTypeBuffer.Impl buffer = minecraft.getRenderTypeBuffers().getBufferSource();
        IBakedModel model = minecraft.getItemRenderer().getItemModelWithOverrides(itemStack, null, null);
        minecraft.getItemRenderer().renderItem(itemStack, ItemCameraTransforms.TransformType.GUI, false, new MatrixStack(), buffer, 15728880, OverlayTexture.NO_OVERLAY, model);
        buffer.finish();
        RenderSystem.disableDepthTest();
        RenderSystem.popMatrix();
    }

    private List<String> getLoreStrings(ItemStack itemStack) {
        List<String> lore = new ArrayList<>();
        if (!itemStack.hasTag()) return lore;
        CompoundNBT tag = itemStack.getTag();
        if (!tag.contains("display")) return lore;
        CompoundNBT display = tag.getCompound("display");
        if (!display.contains("Lore")) return lore;
        ListNBT loreList = display.getList("Lore", 8);
        for (int i = 0; i < Math.min(loreList.size(), 6); i++) {
            try {
                String text = ITextComponent.Serializer.getComponentFromJson(loreList.getString(i)).getString().replaceAll("§.", "").trim();
                if (!text.isEmpty()) lore.add(text);
            } catch (Exception ignored) {}
        }
        return lore;
    }

    private void drawAbstractButton(MatrixStack stack, String text, float x, float y, float w, float h,
                                    float hover, float press, float alpha, int color, ButtonRipple ripple) {
        float time = System.currentTimeMillis() / 1000f;
        float cx = x + w / 2;
        float cy = y + h / 2;
        float scale = 1.0f - press * 0.03f;

        GlStateManager.pushMatrix();
        GlStateManager.translatef(cx, cy, 0);
        GlStateManager.scalef(scale, scale, 1);
        GlStateManager.translatef(-cx, -cy, 0);

        if (hover > 0.01f) {
            float glowOp = hover * 0.6f * alpha;
            for (int i = 1; i <= 3; i++) {
                RenderUtility.drawRoundedRect(x - i * 2, y - i * 2, w + i * 4, h + i * 4, 8 + i, ColorUtils.setAlpha(color, (int)(40 * glowOp / i)));
            }
        }

        RenderUtility.drawShadow(x, y + 2, w, h, 8, ColorUtils.rgba(0, 0, 0, (int)(100 * alpha)));
        RenderUtility.drawRoundedRect(x, y, w, h, 6, ColorUtils.rgba(20, 20, 24, (int)(255 * alpha)));

        Stencil.initStencilToWrite();
        RenderUtility.drawRoundedRect(x, y, w, h, 6, -1);
        Stencil.readStencilBuffer(1);

        for (float i = 0; i < w; i += 2) {
            float perc = i / w;
            float wave = (float) Math.sin(perc * 4.0 + time * 2.0) * 0.5f + 0.5f;
            int c1 = ColorUtils.setAlpha(color, (int)(255 * alpha));
            int c2 = ColorUtils.setAlpha(ColorUtils.getOppositeColor(color), (int)(180 * alpha));
            int mid = ColorUtils.interpolateColor(c1, c2, wave);
            float alphaMod = 0.1f + hover * 0.2f;
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
        int borderColor = ColorUtils.interpolateColor(ColorUtils.rgba(60, 60, 70, (int)(255 * alpha)), color, hover);
        RenderUtility.drawRoundedRectOutline(x, y, w, h, 6, 1.0f, borderColor);

        GlStateManager.enableTexture();
        Fonts.sfuy.drawCenteredText(stack, text, cx, cy - 3.5f, -1, 8.5f);
        if (hover > 0.1f) Fonts.sfuy.drawCenteredText(stack, text, cx, cy - 3.5f, ColorUtils.setAlpha(color, (int)(150 * hover * alpha)), 8.5f);
        GlStateManager.popMatrix();
    }

    private void drawStyledInput(MatrixStack stack, ExtendedTextField f, String label, String icon, float x, float y, float w, float alpha, int themeColor) {
        if (f == null) return;
        boolean focused = f.isFocused();
        float h = 26;

        int bg = focused ? ColorUtils.rgba(25, 25, 30, (int)(255 * alpha)) : ColorUtils.rgba(20, 20, 24, (int)(200 * alpha));
        RenderUtility.drawRoundedRect(x, y, w, h, 5, bg);

        int border = focused ? themeColor : ColorUtils.rgba(60, 60, 70, 255);
        RenderUtility.drawRoundedRectOutline(x, y, w, h, 5, 1f, ColorUtils.setAlpha(border, (int)(150 * alpha)));

        float iconW = 20;
        RenderUtility.drawRectW(x + iconW, y + 4, 1, h - 8, ColorUtils.rgba(255, 255, 255, (int)(20 * alpha)));
        Fonts.sfuy.drawCenteredText(stack, icon, x + iconW / 2, y + 9, focused ? themeColor : ColorUtils.rgba(120, 120, 130, 255), 10f);
        Fonts.sfuy.drawText(stack, label, x, y - 9, ColorUtils.rgba(160, 160, 170, (int)(200 * alpha)), 6f);
        renderInputValue(stack, f, x + iconW + 4, y, h, focused, alpha, themeColor);
    }

    private void renderInputValue(MatrixStack stack, ExtendedTextField f, float x, float y, float h, boolean focused, float alpha, int themeColor) {
        String value = f.getText();
        if (value == null) value = "";
        int color = focused ? ColorUtils.rgba(240, 240, 248, (int)(255 * alpha)) : ColorUtils.rgba(180, 180, 190, (int)(220 * alpha));
        if (focused) color = ColorUtils.interpolateColor(color, themeColor, 0.12f);
        float valueSize = 7.1f;
        float valueY = y + (h - valueSize) / 2f + 0.7f;
        if (!value.isEmpty()) Fonts.sfuy.drawText(stack, value, x, valueY, color, valueSize);
        renderInputCaret(f, value, x, valueY, valueSize, focused, alpha, themeColor);
    }

    private void renderInputCaret(ExtendedTextField f, String value, float x, float valueY, float valueSize, boolean focused, float alpha, int themeColor) {
        if (!focused || (System.currentTimeMillis() / 450L) % 2L == 0L) return;
        int cursor = MathHelper.clamp(f.getCursorPosition(), 0, value.length());
        String beforeCursor = value.substring(0, cursor);
        float caretX = x + Fonts.sfuy.getWidth(beforeCursor, valueSize) + 0.8f;
        float caretY = valueY - 0.2f;
        float caretH = valueSize + 1.8f;
        RenderUtility.drawRectW(caretX, caretY, 0.85f, caretH, ColorUtils.setAlpha(themeColor, (int)(220 * alpha)));
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

    private void renderCloseButton(MatrixStack stack, int mx, int my, float alpha) {
        float size = BTN_SIZE;
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

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        ripples.add(new Ripple((float) mx, (float) my));
        if (buyField != null) buyField.setFocused2(buyField.isMouseOver(mx, my));
        if (sellField != null) sellField.setFocused2(sellField.isMouseOver(mx, my));

        float btnY = getHeaderButtonY();

        if (RenderUtility.isInRegion(mx, my, getLogsButtonX(), btnY, BTN_SIZE, BTN_SIZE)) {
            logsPressAnim = new Animation().animate(1, 0.1f, Easings.QUAD_OUT);
            logsRipple = new ButtonRipple((float) mx, (float) my);
            String server = mc.getCurrentServerData() != null ? mc.getCurrentServerData().serverIP : "Unknown";
            String account = mc.getSession().getUsername();
            mc.displayGuiScreen(new LogsScreen(manager, server, account));
            return true;
        }

        if (RenderUtility.isInRegion(mx, my, getTelegramButtonX(), btnY, BTN_SIZE, BTN_SIZE)) {
            telegramPressAnim = new Animation().animate(1, 0.1f, Easings.QUAD_OUT);
            telegramRipple = new ButtonRipple((float) mx, (float) my);
            mc.displayGuiScreen(new TelegramConfigScreen(manager));
            return true;
        }

        float closeX = posX + windowWidth - BTN_SIZE - 14;
        float closeY = posY + 13;
        if (RenderUtility.isInRegion(mx, my, closeX, closeY, BTN_SIZE, BTN_SIZE)) {
            closePressAnim = new Animation().animate(1, 0.1f, Easings.QUAD_OUT);
            closeRipple = new ButtonRipple((float) mx, (float) my);
            minecraft.displayGuiScreen(null);
            return true;
        }

        if (selectedIndex >= 0 && selectedIndex < manager.getItems().size()) {
            AutoBuyItem item = manager.getItems().get(selectedIndex);
            float rx = posX + 320;
            float rw = windowWidth - 320;
            float bottomAreaY = posY + windowHeight - 95;
            float padding = 15;
            float gap = 10;
            float elementW = (rw - (padding * 2) - gap) / 2;
            float btnY2 = bottomAreaY + 58;
            float btnH = 26;

            if (RenderUtility.isInRegion(mx, my, rx + padding, btnY2, elementW, btnH)) {
                savePressAnim = new Animation().animate(1, 0.1f, Easings.QUAD_OUT);
                saveRipple = new ButtonRipple((float) mx, (float) my);
                saveItem(item);
                return true;
            }
            if (RenderUtility.isInRegion(mx, my, rx + padding + elementW + gap, btnY2, elementW, btnH)) {
                parsePressAnim = new Animation().animate(1, 0.1f, Easings.QUAD_OUT);
                parseRipple = new ButtonRipple((float) mx, (float) my);
                item.parsingEnabled = !item.parsingEnabled;
                manager.saveConfig();
                return true;
            }
        }

        float gridTop = posY + 45;
        float gridHeight = windowHeight - 60;
        if (RenderUtility.isInRegion(mx, my, posX + 10, gridTop, 310, gridHeight)) {
            for (int i = 0; i < manager.getItems().size(); i++) {
                int col = i % COLS;
                int row = i / COLS;
                float ix = posX + 16 + col * (ITEM_SIZE + ITEM_GAP);
                float iy = gridTop + 5 + animatedScroll + row * (ITEM_SIZE + ITEM_GAP);
                if (iy + ITEM_SIZE < gridTop || iy > gridTop + gridHeight) continue;
                if (RenderUtility.isInRegion(mx, my, ix, iy, ITEM_SIZE, ITEM_SIZE)) {
                    if (btn == 0) { selectedIndex = i; buyField = null; }
                    else if (btn == 1) { manager.getItems().get(i).buyPrice = 0; manager.getItems().get(i).sellPrice = 0; manager.saveConfig(); if (selectedIndex == i) buyField = null; }
                    else if (btn == 2) { manager.getItems().get(i).parsingEnabled = !manager.getItems().get(i).parsingEnabled; manager.saveConfig(); }
                    return true;
                }
            }
        }

        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        savePressAnim = savePressAnim.animate(0, 0.35f, Easings.EXPO_OUT);
        parsePressAnim = parsePressAnim.animate(0, 0.35f, Easings.EXPO_OUT);
        logsPressAnim = logsPressAnim.animate(0, 0.35f, Easings.EXPO_OUT);
        telegramPressAnim = telegramPressAnim.animate(0, 0.35f, Easings.EXPO_OUT);
        closePressAnim = closePressAnim.animate(0, 0.35f, Easings.EXPO_OUT);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (RenderUtility.isInRegion(mouseX, mouseY, posX, posY, 320, windowHeight)) {
            int rows = (int) Math.ceil(manager.getItems().size() / (float) COLS);
            float itemsHeight = rows * (ITEM_SIZE + ITEM_GAP);
            float maxScroll = Math.max(0, itemsHeight - (windowHeight - 60));
            scroll = MathHelper.clamp(scroll + (float)(delta * 22), -maxScroll, 0);
            return true;
        }
        return false;
    }

    private void saveItem(AutoBuyItem item) {
        try {
            if (buyField != null) item.buyPrice = parseLongSafe(buyField.getText());
            if (sellField != null) item.sellPrice = parseLongSafe(sellField.getText());
        } catch (Exception ignored) {}
        manager.saveConfig();
    }

    private long parseLongSafe(String s) {
        try { return Long.parseLong(s.replaceAll("\\D", "")); } catch (Exception e) { return 0; }
    }

    @Override
    public boolean keyPressed(int key, int scan, int mod) {
        if (key == 256) { minecraft.displayGuiScreen(null); return true; }
        if (buyField != null && buyField.isFocused()) return buyField.keyPressed(key, scan, mod);
        if (sellField != null && sellField.isFocused()) return sellField.keyPressed(key, scan, mod);
        return super.keyPressed(key, scan, mod);
    }

    @Override
    public boolean charTyped(char c, int mod) {
        if (!Character.isDigit(c)) return false;
        if (buyField != null && buyField.isFocused()) return buyField.charTyped(c, mod);
        if (sellField != null && sellField.isFocused()) return sellField.charTyped(c, mod);
        return false;
    }

    @Override
    public void onClose() { manager.saveConfig(); }

    @Override
    public boolean isPauseScreen() { return false; }

    private static class ExtendedTextField extends TextFieldWidget {
        public ExtendedTextField(FontRenderer font, int x, int y, int width, int height, ITextComponent title) {
            super(font, x, y, width, height, title);
        }
        public int getRealHeight() { return this.height; }
    }

    private static class Ripple {
        float x, y, radius = 250;
        Animation animation = new Animation().animate(1, 0.6f, Easings.QUAD_OUT);
        public Ripple(float x, float y) { this.x = x; this.y = y; }
        public void update() { animation.update(); }
        public boolean isFinished() { return animation.getValue() >= 0.99f; }
    }

    private static class ButtonRipple {
        float startX, startY;
        long startTime;
        public ButtonRipple(float x, float y) { this.startX = x; this.startY = y; this.startTime = System.currentTimeMillis(); }
        public float getProgress() { return Math.min((System.currentTimeMillis() - startTime) / 600f, 1f); }
        public boolean isFinished() { return System.currentTimeMillis() - startTime > 600; }
    }
}
