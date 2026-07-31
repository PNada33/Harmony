package xd.harm.ui.clickgui.components.settings;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import ru.hogoshi.Animation;
import ru.hogoshi.util.Easings;
import xd.harm.Harmony;
import xd.harm.modules.impl.render.HUD;
import xd.harm.modules.impl.render.Theme;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.ModeListSetting;
import xd.harm.ui.clickgui.components.builder.Component;
import xd.harm.utils.SoundUtil;
import xd.harm.utils.math.MathUtil;
import xd.harm.utils.render.Cursors;
import xd.harm.utils.render.GaussianBlur;
import xd.harm.utils.render.color.ColorUtils;
import xd.harm.utils.render.font.Fonts;
import xd.harm.utils.render.gl.Scissor;
import xd.harm.utils.render.rect.RenderUtility;

import java.util.ArrayList;
import java.util.List;

public class MultiBoxComponent extends Component {

    private static MultiBoxComponent CURRENT_OPEN;

    public static MultiBoxComponent getCurrentlyOpen() {
        return CURRENT_OPEN;
    }

    public static void closeAllOpen() {
        if (CURRENT_OPEN != null) {
            CURRENT_OPEN.forceClose();
            CURRENT_OPEN = null;
        }
    }

    public static boolean isAnyDropdownOpen() {
        return CURRENT_OPEN != null && CURRENT_OPEN.isOpen;
    }

    public static boolean isMouseOverAnyDropdown(float mouseX, float mouseY) {
        return CURRENT_OPEN != null && CURRENT_OPEN.isOpen && CURRENT_OPEN.isMouseOverDropdown(mouseX, mouseY);
    }

    private final ModeListSetting setting;
    private final List<OptionItem> options = new ArrayList<>();

    private Animation hoverAnimation = new Animation();
    private Animation dropdownAnimation = new Animation();
    private Animation scaleAnimation = new Animation();
    private Animation closeButtonAnimation = new Animation();

    private boolean isOpen;
    private boolean dragging;
    private boolean hovered;
    private boolean closeButtonHovered;
    private int hoveredOptionIndex = -1;
    private final TextCache titleCache = new TextCache();
    private final TextCache summaryCache = new TextCache();

    private float dropdownX = 60f;
    private float dropdownY = 0f;
    private float dragOffsetX;
    private float dragOffsetY;
    private long openedAtMs;

    private static final float DROPDOWN_WIDTH = 126f;
    private static final float DROPDOWN_HEADER_HEIGHT = 14f;
    private static final float DROPDOWN_RADIUS = 5.2f;
    private static final float CLOSE_BUTTON_SIZE = 10f;

    private static final int FIELD_HEIGHT = 18;
    private static final float FIELD_RADIUS = 3.2f;
    private static final float PREVIEW_BOX_WIDTH = 54f;
    private static final float PREVIEW_BOX_HEIGHT = 12f;
    private static final float PREVIEW_RADIUS = 3f;
    private static final int OPTION_HEIGHT = 18;
    private static final float OPTION_RADIUS = 3.2f;

    public MultiBoxComponent(ModeListSetting setting) {
        this.setting = setting;
        setHeight(FIELD_HEIGHT);

        hoverAnimation = hoverAnimation.animate(0, 0.25f, Easings.EXPO_OUT);
        dropdownAnimation = dropdownAnimation.animate(0, 0.22f, Easings.EXPO_OUT);
        scaleAnimation = scaleAnimation.animate(1, 0.28f, Easings.BACK_OUT);
        closeButtonAnimation = closeButtonAnimation.animate(0, 0.2f, Easings.EXPO_OUT);

        for (BooleanSetting bool : setting.get()) {
            options.add(new OptionItem(bool));
        }
    }

    public void forceClose() {
        if (isOpen) {
            SoundUtil.playSound("dropdowndis.wav");
            MultiBoxOverlayManager.hide(this);
        }
        isOpen = false;
        dragging = false;
        hoveredOptionIndex = -1;
        closeButtonHovered = false;
        dropdownAnimation.setValue(0);
        if (CURRENT_OPEN == this) {
            CURRENT_OPEN = null;
        }
    }

    @Override
    public void render(MatrixStack stack, float mouseX, float mouseY) {
        super.render(stack, mouseX, mouseY);

        hoverAnimation.update();
        scaleAnimation.update();
        closeButtonAnimation.update();

        float previewX = getX() + getWidth() - PREVIEW_BOX_WIDTH - 6f;
        float previewY = getY() + (FIELD_HEIGHT - PREVIEW_BOX_HEIGHT) / 2f;
        boolean boxHovered = MathUtil.isHovered(mouseX, mouseY, previewX, previewY, PREVIEW_BOX_WIDTH, PREVIEW_BOX_HEIGHT);
        hoverAnimation.animate(boxHovered ? 1 : 0, 0.25f, Easings.EXPO_OUT);
        float hv = (float) hoverAnimation.getValue();
        int themeColor = Theme.MainColor(0);

        float rowX = getX() + 2f;
        float rowY = getY() + 1f;
        float rowW = getWidth() - 4f;
        float rowH = FIELD_HEIGHT - 2f;
        RenderUtility.drawRoundedRect(rowX, rowY, rowW, rowH, FIELD_RADIUS, ColorUtils.rgba(255, 255, 255, (int) (10 + 8 * hv)));
        RenderUtility.drawRoundedRect(rowX, rowY, rowW, rowH, FIELD_RADIUS, ColorUtils.rgba(11, 13, 17, (int) (90 + 12 * hv)));
        RenderUtility.drawRoundedRectOutline(
                rowX,
                rowY,
                rowW,
                rowH,
                FIELD_RADIUS,
                0.55f,
                ColorUtils.interpolateColor(ColorUtils.rgba(255, 255, 255, 20), ColorUtils.setAlpha(themeColor, 120), Math.min(1f, hv * 0.7f + (isOpen ? 0.4f : 0f)))
        );

        float nameX = getX() + 6f;
        float nameY = getY() + FIELD_HEIGHT / 2f - 3.5f;
        float maxNameW = Math.max(12f, previewX - nameX - 4f);
        String title = trimToWidthCached(setting.getName(), maxNameW, 6.3f, titleCache);
        Scissor.push();
        Scissor.setFromComponentCoordinates(nameX, getY() + 1f, maxNameW, FIELD_HEIGHT - 2f);
        Fonts.sfuy.drawText(
                stack,
                title,
                nameX,
                nameY,
                ColorUtils.interpolateColor(ColorUtils.rgba(158, 164, 174, 255), ColorUtils.rgba(226, 231, 240, 255), Math.min(1f, hv + (isOpen ? 0.2f : 0f))),
                6.2f,
                0.05f
        );
        Scissor.pop();

        float previewFocus = Math.min(1f, hv * 0.8f + (isOpen ? 0.45f : 0f));
        RenderUtility.drawRoundedRect(previewX, previewY, PREVIEW_BOX_WIDTH, PREVIEW_BOX_HEIGHT, PREVIEW_RADIUS, ColorUtils.rgba(255, 255, 255, (int) (12 + 12 * previewFocus)));
        RenderUtility.drawRoundedRect(previewX, previewY, PREVIEW_BOX_WIDTH, PREVIEW_BOX_HEIGHT, PREVIEW_RADIUS, ColorUtils.rgba(14, 16, 21, (int) (132 + 14 * previewFocus)));
        RenderUtility.drawRoundedRectOutline(
                previewX,
                previewY,
                PREVIEW_BOX_WIDTH,
                PREVIEW_BOX_HEIGHT,
                PREVIEW_RADIUS,
                0.6f,
                ColorUtils.interpolateColor(ColorUtils.rgba(255, 255, 255, 32), ColorUtils.setAlpha(themeColor, 145), previewFocus)
        );

        String summary = getSelectedSummary();
        int summaryColor = summary.equals("...")
                ? ColorUtils.rgba(122, 128, 138, 255)
                : ColorUtils.interpolateColor(ColorUtils.rgba(206, 211, 220, 255), ColorUtils.rgba(255, 255, 255, 255), previewFocus * 0.7f);
        Fonts.sfuy.drawCenteredText(stack, summary, previewX + PREVIEW_BOX_WIDTH / 2f - 2f, previewY + PREVIEW_BOX_HEIGHT / 2f - 3f, summaryColor, 5.7f, 0.04f);
        Fonts.sfuy.drawText(stack, ">", previewX + PREVIEW_BOX_WIDTH - 6f, previewY + PREVIEW_BOX_HEIGHT / 2f - 3.1f,
                ColorUtils.interpolateColor(ColorUtils.rgba(132, 138, 149, 255), ColorUtils.rgba(228, 233, 240, 255), previewFocus), 5.8f, 0.04f);

        handleCursor(boxHovered, mouseX, mouseY);
    }

    public boolean renderFloatingWindows(MatrixStack stack, float mouseX, float mouseY) {
        dropdownAnimation.update();
        dropdownAnimation.animate(isOpen ? 1 : 0, 0.2f, Easings.EXPO_OUT);

        float alpha = (float) dropdownAnimation.getValue();
        boolean shouldRender = isOpen || alpha > 0.01f;
        if (shouldRender) {
            renderFloatingDropdown(stack, mouseX, mouseY, alpha);
        }
        return shouldRender;
    }

    private void renderFloatingDropdown(MatrixStack stack, float mouseX, float mouseY, float alpha) {
        float totalHeight = getDropdownHeight();
        float animatedHeight = Math.max(DROPDOWN_HEADER_HEIGHT + 6f, totalHeight * alpha);
        float scale = 0.90f + 0.10f * (float) scaleAnimation.getValue();
        int themeColor = Theme.MainColor(0);

        if (dragging) {
            dropdownX = mouseX - dragOffsetX;
            dropdownY = mouseY - dragOffsetY;
            clampToScreen(totalHeight);
        }

        hoveredOptionIndex = -1;

        float closeX = getCloseButtonX();
        float closeY = getCloseButtonY();
        boolean closeHovered = MathUtil.isHovered(mouseX, mouseY, closeX, closeY, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE);
        closeButtonAnimation.animate(closeHovered ? 1 : 0, 0.2f, Easings.EXPO_OUT);
        closeButtonHovered = closeHovered;
        float closeAnim = (float) closeButtonAnimation.getValue();

        stack.push();
        float cx = dropdownX + DROPDOWN_WIDTH / 2f;
        float cy = dropdownY + animatedHeight / 2f;
        stack.translate(cx, cy, 0);
        stack.scale(scale, scale, 1);
        stack.translate(-cx, -cy, 0);

        RenderUtility.drawShadow(dropdownX + 1f, dropdownY + 1f, DROPDOWN_WIDTH - 2f, animatedHeight - 2f, 10, ColorUtils.rgba(0, 0, 0, (int) (35 * alpha)));
        GaussianBlur.startBlur();
        RenderUtility.drawRoundedRect(dropdownX, dropdownY, DROPDOWN_WIDTH, animatedHeight, DROPDOWN_RADIUS, ColorUtils.rgba(0, 0, 0, (int) (26 * alpha)));
        GaussianBlur.endBlur(12, 1);

        RenderUtility.drawRoundedRect(dropdownX, dropdownY, DROPDOWN_WIDTH, animatedHeight, DROPDOWN_RADIUS, ColorUtils.rgba(255, 255, 255, (int) (17 * alpha)));
        RenderUtility.drawRoundedRect(dropdownX, dropdownY, DROPDOWN_WIDTH, animatedHeight, DROPDOWN_RADIUS, ColorUtils.rgba(12, 14, 19, (int) (145 * alpha)));
        RenderUtility.drawRoundedRectOutline(dropdownX, dropdownY, DROPDOWN_WIDTH, animatedHeight, DROPDOWN_RADIUS, 0.65f, ColorUtils.rgba(255, 255, 255, (int) (34 * alpha)));

        Fonts.sfuy.drawText(stack, setting.getName(), dropdownX + 7f, dropdownY + DROPDOWN_HEADER_HEIGHT / 2f - 3.2f, ColorUtils.rgba(226, 232, 241, (int) (240 * alpha)), 6.4f, 0.05f);
        String countText = getEnabledCount() + "/" + options.size();
        float countW = Fonts.sfuy.getWidth(countText, 5.6f, 0.04f);
        Fonts.sfuy.drawText(stack, countText, dropdownX + DROPDOWN_WIDTH - CLOSE_BUTTON_SIZE - 6f - countW, dropdownY + DROPDOWN_HEADER_HEIGHT / 2f - 2.8f, ColorUtils.rgba(162, 170, 186, (int) (220 * alpha)), 5.6f, 0.04f);
        Fonts.sfuy.drawCenteredText(
                stack,
                "x",
                closeX + CLOSE_BUTTON_SIZE / 2f + 0.2f,
                closeY + CLOSE_BUTTON_SIZE / 2f - 2.8f,
                ColorUtils.interpolateColor(ColorUtils.rgba(145, 152, 166, (int) (200 * alpha)), ColorUtils.rgba(255, 255, 255, (int) (245 * alpha)), closeAnim),
                5.8f,
                0.04f
        );

        RenderUtility.drawRectW(dropdownX + 6f, dropdownY + DROPDOWN_HEADER_HEIGHT + 0.8f, DROPDOWN_WIDTH - 12f, 0.5f, ColorUtils.rgba(255, 255, 255, (int) (24 * alpha)));

        float itemX = dropdownX + 6f;
        float itemW = DROPDOWN_WIDTH - 12f;
        float y = dropdownY + DROPDOWN_HEADER_HEIGHT + 3.4f;
        int visibleIndex = 0;
        for (int i = 0; i < options.size(); i++) {
            OptionItem option = options.get(i);
            if (!option.setting.visible.get()) continue;
            boolean selected = option.setting.get();
            float itemY = y + visibleIndex * OPTION_HEIGHT;
            float itemH = OPTION_HEIGHT - 2f;
            boolean itemHovered = MathUtil.isHovered(mouseX, mouseY, itemX, itemY, itemW, itemH) && !closeHovered;
            if (itemHovered) hoveredOptionIndex = i;

            option.hoverAnimation.update();
            option.checkAnimation.update();
            option.hoverAnimation.animate(itemHovered ? 1 : 0, 0.2f, Easings.EXPO_OUT);
            option.checkAnimation.animate(selected ? 1 : 0, 0.25f, Easings.EXPO_OUT);
            float hv = (float) option.hoverAnimation.getValue();
            float ck = (float) option.checkAnimation.getValue();
            float active = Math.max(ck, hv * 0.75f);

            RenderUtility.drawRoundedRect(itemX, itemY, itemW, itemH, OPTION_RADIUS, ColorUtils.rgba(255, 255, 255, (int) ((7 + 7 * hv) * alpha)));
            RenderUtility.drawRoundedRect(itemX, itemY, itemW, itemH, OPTION_RADIUS, ColorUtils.rgba(14, 16, 22, (int) ((96 + 14 * hv) * alpha)));
            if (active > 0.01f) {
                RenderUtility.drawRoundedRect(itemX, itemY, itemW, itemH, OPTION_RADIUS, ColorUtils.setAlpha(themeColor, (int) (26 * active * alpha)));
            }
            RenderUtility.drawRoundedRectOutline(
                    itemX,
                    itemY,
                    itemW,
                    itemH,
                    OPTION_RADIUS,
                    0.6f,
                    ColorUtils.interpolateColor(ColorUtils.rgba(255, 255, 255, (int) (28 * alpha)), ColorUtils.setAlpha(themeColor, (int) (155 * alpha)), active)
            );

            if (selected || ck > 0.08f) {
                RenderUtility.drawRoundedRect(itemX + 2f, itemY + 2f, 1.4f, itemH - 4f, 0.7f, ColorUtils.setAlpha(themeColor, (int) (180 * Math.max(0.4f, ck) * alpha)));
            }

            String name = trimToWidthCached(option.setting.getName(), itemW - 24f, 6.2f, option.nameCache);
            int textColor = ColorUtils.interpolateColor(ColorUtils.rgba(162, 168, 178, (int) (255 * alpha)), ColorUtils.rgba(246, 248, 255, (int) (255 * alpha)), Math.min(1f, ck * 0.85f + hv * 0.35f));
            Fonts.sfuy.drawText(stack, name, itemX + 6f, itemY + itemH / 2f - 3.1f, textColor, 6.2f, 0.04f);

            float checkSize = 7.3f;
            float checkX = itemX + itemW - checkSize - 6f;
            float checkY = itemY + itemH / 2f - checkSize / 2f;
            RenderUtility.drawRoundedRect(
                    checkX,
                    checkY,
                    checkSize,
                    checkSize,
                    1.7f,
                    ColorUtils.rgba(10, 12, 16, (int) ((112 + 18 * hv) * alpha))
            );
            RenderUtility.drawRoundedRectOutline(
                    checkX,
                    checkY,
                    checkSize,
                    checkSize,
                    1.7f,
                    0.55f,
                    ColorUtils.interpolateColor(
                            ColorUtils.rgba(255, 255, 255, (int) (30 * alpha)),
                            ColorUtils.setAlpha(themeColor, (int) (165 * alpha)),
                            Math.min(1f, ck + hv * 0.18f)
                    )
            );

            if (ck > 0.01f) {
                float innerBase = checkSize - 3.0f;
                float innerSize = innerBase * ck;
                float innerX = checkX + (checkSize - innerSize) / 2f;
                float innerY = checkY + (checkSize - innerSize) / 2f;
                RenderUtility.drawRoundedRect(
                        innerX,
                        innerY,
                        innerSize,
                        innerSize,
                        1.1f,
                        ColorUtils.setAlpha(themeColor, (int) (205 * ck * alpha))
                );
            }
            visibleIndex++;
        }

        stack.pop();
    }

    private void handleCursor(boolean boxHovered, float mouseX, float mouseY) {
        boolean onDropdown = isOpen && isMouseOverDropdown(mouseX, mouseY);
        if (boxHovered || hoveredOptionIndex != -1 || onDropdown || closeButtonHovered) {
            GLFW.glfwSetCursor(Minecraft.getInstance().getMainWindow().getHandle(), Cursors.HAND);
            if (!hovered) {
                hovered = true;
                SoundUtil.playSound("Hovered.wav");
            }
        } else if (hovered) {
            GLFW.glfwSetCursor(Minecraft.getInstance().getMainWindow().getHandle(), Cursors.ARROW);
            hovered = false;
        }
    }

    private String getSelectedSummary() {
        int count = 0;
        String first = null;
        for (OptionItem option : options) {
            if (!option.setting.visible.get()) continue;
            if (option.setting.get()) {
                count++;
                if (first == null) {
                    first = option.setting.getName();
                }
            }
        }
        if (count == 0) return "...";
        if (count == 1 && first != null) return trimToWidthCached(first, PREVIEW_BOX_WIDTH - 8f, 5.8f, summaryCache);
        return trimToWidth(count + " выбрано", PREVIEW_BOX_WIDTH - 8f, 5.8f);
    }

    private String trimToWidthCached(String text, float maxWidth, float size, TextCache cache) {
        if (text == null || text.isEmpty()) return "";
        if (text.equals(cache.source) && Float.compare(maxWidth, cache.maxWidth) == 0 && Float.compare(size, cache.size) == 0) {
            return cache.value;
        }

        cache.source = text;
        cache.maxWidth = maxWidth;
        cache.size = size;
        cache.value = trimToWidth(text, maxWidth, size);
        return cache.value;
    }

    private String trimToWidth(String text, float maxWidth, float size) {
        if (text == null || text.isEmpty()) return "";
        if (Fonts.sfuy.getWidth(text, size) <= maxWidth) return text;
        int low = 1;
        int high = text.length() - 1;
        while (low < high) {
            int mid = (low + high + 1) >>> 1;
            if (Fonts.sfuy.getWidth(text.substring(0, mid) + "..", size) <= maxWidth) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        return text.substring(0, low) + "..";
    }

    private void openDropdownAt(float mouseX, float mouseY) {
        float totalHeight = getDropdownHeight();
        float x = mouseX + 5f;
        float y = mouseY - DROPDOWN_HEADER_HEIGHT / 2f;

        int sw = Minecraft.getInstance().getMainWindow().getScaledWidth();
        int sh = Minecraft.getInstance().getMainWindow().getScaledHeight();

        if (x + DROPDOWN_WIDTH > sw - 5f) x = mouseX - DROPDOWN_WIDTH - 5f;
        if (x < 5f) x = 5f;
        if (y + totalHeight > sh - 5f) y = sh - totalHeight - 5f;
        if (y < 5f) y = 5f;

        if (CURRENT_OPEN != null && CURRENT_OPEN != this) {
            CURRENT_OPEN.forceClose();
        }

        dropdownX = x;
        dropdownY = y;
        isOpen = true;
        dragging = false;
        openedAtMs = System.currentTimeMillis();

        dropdownAnimation.setValue(0);
        scaleAnimation.setValue(0);
        scaleAnimation.animate(1, 0.3f, Easings.BACK_OUT);
        SoundUtil.playSound("dropdownen.wav");

        CURRENT_OPEN = this;
        MultiBoxOverlayManager.show(this);
    }

    private void openDropdownAtCursor() {
        Minecraft mc = Minecraft.getInstance();
        var window = mc.getMainWindow();
        double rawX = mc.mouseHelper.getMouseX();
        double rawY = mc.mouseHelper.getMouseY();
        float mx = (float) (rawX * window.getScaledWidth() / (double) window.getWidth());
        float my = (float) (rawY * window.getScaledHeight() / (double) window.getHeight());
        openDropdownAt(mx, my);
    }

    private void clampToScreen(float totalHeight) {
        int sw = Minecraft.getInstance().getMainWindow().getScaledWidth();
        int sh = Minecraft.getInstance().getMainWindow().getScaledHeight();
        dropdownX = Math.max(2f, Math.min(sw - DROPDOWN_WIDTH - 2f, dropdownX));
        dropdownY = Math.max(2f, Math.min(sh - totalHeight - 2f, dropdownY));
    }

    private float getDropdownHeight() {
        int visibleCount = 0;
        for (OptionItem option : options) {
            if (option.setting.visible.get()) visibleCount++;
        }
        return DROPDOWN_HEADER_HEIGHT + visibleCount * OPTION_HEIGHT + 8f;
    }

    private float getCloseButtonX() {
        return dropdownX + DROPDOWN_WIDTH - CLOSE_BUTTON_SIZE - 4f;
    }

    private float getCloseButtonY() {
        return dropdownY + 2.5f;
    }

    @Override
    public void mouseClick(float mouseX, float mouseY, int mouse) {
        if (isAnyDropdownOpen() && !isMouseOverAnyDropdown(mouseX, mouseY) && CURRENT_OPEN != this) {
            return;
        }

        float previewX = getX() + getWidth() - PREVIEW_BOX_WIDTH - 6f;
        float previewY = getY() + (FIELD_HEIGHT - PREVIEW_BOX_HEIGHT) / 2f;
        boolean previewHovered = MathUtil.isHovered(mouseX, mouseY, previewX, previewY, PREVIEW_BOX_WIDTH, PREVIEW_BOX_HEIGHT);
        boolean rowHovered = isHovered(mouseX, mouseY);
        if (previewHovered || rowHovered) {
            if (!isOpen && (mouse == GLFW.GLFW_MOUSE_BUTTON_RIGHT || mouse == GLFW.GLFW_MOUSE_BUTTON_LEFT)) {
                openDropdownAt(mouseX, mouseY);
                return;
            }

            if (isOpen && mouse == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                closeDropdown();
                return;
            }
        }

        if (isOpen && mouse == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            float totalHeight = getDropdownHeight();
            float closeX = getCloseButtonX();
            float closeY = getCloseButtonY();
            if (MathUtil.isHovered(mouseX, mouseY, closeX, closeY, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE)) {
                closeDropdown();
                return;
            }

            boolean inWindow = MathUtil.isHovered(mouseX, mouseY, dropdownX, dropdownY, DROPDOWN_WIDTH, totalHeight);
            if (inWindow) {
                float itemX = dropdownX + 6f;
                float itemW = DROPDOWN_WIDTH - 12f;
                float y = dropdownY + DROPDOWN_HEADER_HEIGHT + 4.5f;
                int vi = 0;
                for (int i = 0; i < options.size(); i++) {
                    if (!options.get(i).setting.visible.get()) continue;
                    float itemY = y + vi * OPTION_HEIGHT;
                    if (MathUtil.isHovered(mouseX, mouseY, itemX, itemY, itemW, OPTION_HEIGHT - 3f)) {
                        BooleanSetting clicked = options.get(i).setting;
                        if (shouldLimitToTwoEnabled() && !clicked.get() && getEnabledCount() >= 2) {
                            for (OptionItem opt : options) {
                                if (opt.setting.get()) {
                                    opt.setting.set(false);
                                    break;
                                }
                            }
                        }
                        clicked.set(!clicked.get());
                        SoundUtil.playSound("click.wav");
                        return;
                    }
                    vi++;
                }

                if (MathUtil.isHovered(mouseX, mouseY, dropdownX, dropdownY, DROPDOWN_WIDTH - CLOSE_BUTTON_SIZE - 6f, DROPDOWN_HEADER_HEIGHT)) {
                    dragging = true;
                    dragOffsetX = mouseX - dropdownX;
                    dragOffsetY = mouseY - dropdownY;
                }
                return;
            }

            closeDropdown();
            return;
        }

        super.mouseClick(mouseX, mouseY, mouse);
    }

    @Override
    public void mouseRelease(float mouseX, float mouseY, int mouse) {
        if (mouse == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            dragging = false;
        }
        super.mouseRelease(mouseX, mouseY, mouse);
    }

    @Override
    public boolean isVisible() {
        return setting.visible.get();
    }

    public boolean isDropdownOpen() {
        return isOpen;
    }

    public boolean isMouseOverDropdown(float mx, float my) {
        if (!isOpen) return false;
        return MathUtil.isHovered(mx, my, dropdownX, dropdownY, DROPDOWN_WIDTH, getDropdownHeight());
    }

    @Override
    public boolean isMouseOverComponent(float mouseX, float mouseY) {
        if (super.isMouseOverComponent(mouseX, mouseY)) {
            return true;
        }
        return isMouseOverDropdown(mouseX, mouseY);
    }

    public void closeDropdown() {
        if (!isOpen) return;
        if (System.currentTimeMillis() - openedAtMs < 120L) return;
        SoundUtil.playSound("dropdowndis.wav");
        isOpen = false;
        dragging = false;
        closeButtonHovered = false;
        scaleAnimation.animate(0, 0.22f, Easings.BACK_IN);
        if (CURRENT_OPEN == this) {
            CURRENT_OPEN = null;
        }
    }

    private int getEnabledCount() {
        int enabledCount = 0;
        for (OptionItem option : options) {
            if (option.setting.get()) {
                enabledCount++;
            }
        }
        return enabledCount;
    }

    private boolean shouldLimitToTwoEnabled() {
        HUD hud = Harmony.getInstance().getModuleManager().getHud();
        return hud != null && setting == hud.watermarkElements && !hud.hudMode.is("\u041E\u0431\u044B\u0447\u043D\u044B\u0439");
    }

    private static class OptionItem {
        private final BooleanSetting setting;
        private Animation hoverAnimation = new Animation();
        private Animation checkAnimation = new Animation();
        private final TextCache nameCache = new TextCache();

        private OptionItem(BooleanSetting setting) {
            this.setting = setting;
            hoverAnimation = hoverAnimation.animate(0, 0.2f, Easings.EXPO_OUT);
            checkAnimation = checkAnimation.animate(0, 0.22f, Easings.EXPO_OUT);
        }
    }

    private static class TextCache {
        private String source = "";
        private String value = "";
        private float maxWidth = Float.NaN;
        private float size = Float.NaN;
    }
}
