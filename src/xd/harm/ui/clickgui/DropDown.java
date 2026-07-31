package xd.harm.ui.clickgui;

import xd.harm.Harmony;
import xd.harm.modules.api.Category;
import xd.harm.modules.impl.render.ClickGui;
import xd.harm.modules.impl.render.Theme;
import xd.harm.ui.clickgui.components.ModuleComponent;
import xd.harm.ui.clickgui.components.SearchField;
import xd.harm.ui.clickgui.components.settings.ColorComponent;
import xd.harm.ui.clickgui.components.settings.MultiBoxComponent;
import xd.harm.utils.client.ClientUtil;
import xd.harm.utils.client.IMinecraft;
import xd.harm.utils.client.Vec2i;
import xd.harm.utils.drag.DragManager;
import xd.harm.utils.drag.Dragging;
import xd.harm.utils.math.MathUtil;
import xd.harm.utils.math.Vector4i;
import xd.harm.utils.render.Cursors;
import xd.harm.utils.render.GaussianBlur;
import xd.harm.utils.render.color.ColorUtils;
import xd.harm.utils.render.font.Fonts;
import xd.harm.utils.render.rect.RenderUtility;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector4f;
import net.minecraft.util.text.ITextComponent;
import org.lwjgl.glfw.GLFW;
import ru.hogoshi.Animation;
import ru.hogoshi.util.Easings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DropDown extends Screen implements IMinecraft {
    private final ClickGui clickGui = Harmony.getInstance().getModuleManager().getClickGui();
    private SearchField searchField;
    private static Animation gradientAnimation = new Animation();
    private final List<Panel> panels = new ArrayList<>();
    private final List<Panel> test = new ArrayList<>();
    @Getter
    private static Animation animation = new Animation();
    @Getter
    private Animation searchFocusAnimation = new Animation();

    private static final Map<Category, Float> savedScrollPositions = new HashMap<>();
    private static final Map<Category, Set<String>> savedExpandedModules = new HashMap<>();
    private static boolean stateInitialized = false;

    private final Dragging themePickerDrag;
    private final ColorComponent themeVisualsPicker;
    private final ColorComponent themeTextPicker;
    private final ColorComponent themeRectPicker;
    private final ColorComponent[] themeCustomPickers;
    private boolean themePickerDragging;
    private float themePickerDragOffsetX;
    private float themePickerDragOffsetY;
    private boolean themePickerPositionInitialized;

    private static final int THEMES_PER_ROW = 4;
    private static final float THEME_CELL_SIZE = 12.8f;
    private static final float THEME_CELL_GAP = 3.5f;
    private static final float THEME_BLOCK_PADDING = 6.5f;
    private static final float THEME_HEADER_HEIGHT = 13.2f;
    private static final float THEME_EDGE_MARGIN = 8.0f;
    private static final float THEME_BLOCK_MIN_WIDTH = 76.0f;
    private static final float THEME_CUSTOM_GAP = 4.2f;
    private static final float THEME_CUSTOM_MIN_HEIGHT = 16.0f;
    private static final float THEME_CUSTOM_WIDTH_EXTRA = 20.0f;
    private static final float THEME_CUSTOM_PICKER_HEIGHT = 66.0f;
    private static final float AUTOBUY_BUTTON_WIDTH = 88.0f;
    private static final float AUTOBUY_BUTTON_HEIGHT = 18.0f;
    private final Animation autoBuyButtonAnimation = new Animation();
    private float autoBuyButtonX;
    private float autoBuyButtonY;

    public DropDown(ITextComponent titleIn) {
        super(titleIn);
        for (Category category : Category.values()) {
            panels.add(new Panel(category, this));
        }

        themeVisualsPicker = new ColorComponent(Theme.visualscolor)
                .setHeaderVisible(false)
                .setActionButtonsVisible(false)
                .setPickerPanelHeight(THEME_CUSTOM_PICKER_HEIGHT);
        themeTextPicker = new ColorComponent(Theme.textcolor);
        themeRectPicker = new ColorComponent(Theme.rectcolor);
        themeCustomPickers = new ColorComponent[]{themeVisualsPicker, themeTextPicker, themeRectPicker};

        themePickerDrag = Harmony.getInstance().createDrag(clickGui, "ClickGuiThemes", -1f, -1f);
        DragManager.load();

        searchFocusAnimation = searchFocusAnimation.animate(0, 0.3f, Easings.EXPO_OUT);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Getter
    private static float scale = 1.0f;

    public boolean isSearching() {
        return searchField != null && !searchField.isEmpty();
    }

    public String getSearchText() {
        return searchField != null ? searchField.getText() : "";
    }

    public boolean searchCheck(String text) {
        return isSearching() && !text
                .replaceAll(" ", "")
                .toLowerCase()
                .contains(getSearchText()
                        .replaceAll(" ", "")
                        .toLowerCase());
    }

    @Override
    public void closeScreen() {
        saveState();
        MultiBoxComponent.closeAllOpen();
        onClose();
        Minecraft.getInstance().displayGuiScreen(null);
        GLFW.glfwSetCursor(Minecraft.getInstance().getMainWindow().getHandle(), Cursors.ARROW);
    }

    private void saveState() {
        for (Panel panel : panels) {
            savedScrollPositions.put(panel.getCategory(), panel.getScrollOffset());

            Set<String> expandedNames = new HashSet<>();
            for (ModuleComponent module : panel.getModules()) {
                if (module.isOpen()) {
                    expandedNames.add(module.getModule().getName());
                }
            }
            savedExpandedModules.put(panel.getCategory(), expandedNames);
        }
        stateInitialized = true;
    }

    private void restoreState() {
        if (!stateInitialized) return;

        for (Panel panel : panels) {
            Float savedScroll = savedScrollPositions.get(panel.getCategory());
            if (savedScroll != null) {
                panel.setScrollOffset(savedScroll);
            }

            Set<String> savedModuleNames = savedExpandedModules.get(panel.getCategory());
            if (savedModuleNames != null && !savedModuleNames.isEmpty()) {
                for (ModuleComponent module : panel.getModules()) {
                    if (savedModuleNames.contains(module.getModule().getName())) {
                        module.setOpen(true);
                        module.getExpandAnim().setValue(1);
                    }
                }
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        Vec2i fixMouse = adjustMouseCoordinates((int) mouseX, (int) mouseY);
        mouseX = fixMouse.getX();
        mouseY = fixMouse.getY();

        for (Panel panel : panels) {
            if (panel.onScroll(mouseX, mouseY, delta)) {
                return true;
            }
        }
        for (Panel panel : test) {
            if (panel.onScroll(mouseX, mouseY, delta)) {
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (searchField != null && searchField.charTyped(codePoint, modifiers)) {
            return true;
        }
        dispatchThemeCustomPickersCharTyped(codePoint, modifiers);
        for (Panel panel : panels) {
            panel.charTyped(codePoint, modifiers);
        }
        for (Panel panel : test) {
            panel.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        animation.update();
        gradientAnimation.update();
        searchFocusAnimation.update();
        autoBuyButtonAnimation.update();

        int windowWidth = ClientUtil.calc(mc.getMainWindow().getScaledWidth());
        int windowHeight = ClientUtil.calc(mc.getMainWindow().getScaledHeight());

        Vec2i fixMouse = adjustMouseCoordinates(mouseX, mouseY);
        mouseX = fixMouse.getX();
        mouseY = fixMouse.getY();

        updateScaleBasedOnScreenWidth();

        boolean isSearchFocused = searchField != null && searchField.isFocused();
        searchFocusAnimation.animate(isSearchFocused ? 1 : 0, 0.3f, Easings.EXPO_OUT);
        float searchFocusValue = (float) searchFocusAnimation.getValue();

        int bgAlpha = (int) (80 * animation.getValue() + searchFocusValue * 100);
        RenderUtility.drawRect(0, 0, windowWidth, windowHeight, ColorUtils.rgba(0, 0, 0, bgAlpha));

        float off = 10;
        float width = panels.size() * (115 + off);

        float animValue = (float) animation.getValue();
        float yOffset = (1 - animValue) * windowHeight;
        boolean closingGui = animation.getToValue() == 0;

        if (closingGui) {
            ColorComponent.closeAll();
        }

        float scaleValue = Math.max(0.7f, animValue);

        for (int i = 0; i < panels.size(); i++) {
            Panel panel = panels.get(i);

            float panelDelay = i * 0.05f;
            float panelAnimation = (float) Math.max(0, Math.min(1, (animation.getValue() - panelDelay) * (1 / (1 - panelDelay))));
            if (panelAnimation <= 0) continue;

            panel.setY(windowHeight / 2f - (700 / 2) / 2f);
            panel.setX((windowWidth / 2f) - (width / 2f) + panel.getCategory().ordinal() * (115 + off) + off / 2f);

            GlStateManager.pushMatrix();
            GlStateManager.translatef(0, yOffset, 0);
            GlStateManager.translatef(panel.getX() + panel.getWidth() / 2f, panel.getY() + panel.getHeight() / 2f, 0);
            GlStateManager.scaled(scaleValue, scaleValue, 1);
            GlStateManager.translatef(-(panel.getX() + panel.getWidth() / 2f), -(panel.getY() + panel.getHeight() / 2f), 0);

            float panelAlpha = panelAnimation * (1.0f - searchFocusValue * 0.3f);

            panel.render(matrixStack, mouseX, mouseY - (int) yOffset, panelAlpha);

            GlStateManager.popMatrix();
        }

        if (!closingGui) {
            renderThemePicker(matrixStack, mouseX, mouseY, windowWidth, windowHeight, animValue, searchFocusValue);
            renderAutoBuyButton(matrixStack, mouseX, mouseY, windowWidth, windowHeight, animValue, searchFocusValue);
        }

        if (searchField != null) {
            float searchW = 250;
            float searchX = windowWidth / 2f - searchW / 2f;
            float searchY = 20;
            float searchH = 20;

            searchField.setX((int) searchX);
            searchField.setY((int) searchY);
            searchField.setWidth((int) searchW);
            searchField.setHeight((int) searchH);

            searchField.render(matrixStack, mouseX, mouseY, partialTicks, animValue);
        }

        if (searchFocusValue < 0.5f) {
            float hintsAlpha = 1.0f - searchFocusValue * 2;

            GlStateManager.pushMatrix();
            GlStateManager.color4f(1, 1, 1, hintsAlpha * animValue);

            float hintsY = windowHeight - 120;
            Fonts.sfuy.drawCenteredText(matrixStack, "CTRL + F - поиск", windowWidth / 2f, hintsY,
                    ColorUtils.rgba(200, 200, 200, (int)(225 * hintsAlpha * animValue)), 7);
            Fonts.sfuy.drawCenteredText(matrixStack, "RSHIFT/ESC - закрыть", windowWidth / 2f, hintsY + 10,
                    ColorUtils.rgba(200, 200, 200, (int)(225 * hintsAlpha * animValue)), 7);
            Fonts.sfuy.drawCenteredText(matrixStack, "СКМ - бинд • ПКМ - настройки", windowWidth / 2f, hintsY + 20,
                    ColorUtils.rgba(200, 200, 200, (int)(225 * hintsAlpha * animValue)), 7);

            GlStateManager.popMatrix();
        }

        if (searchFocusValue < 0.5f) {
            String moduleDescription = "";
            for (Panel panel : panels) {
                float panelTop = panel.getY() + 18;
                float panelBottom = panel.getY() + panel.getHeight() + 2;
                for (ModuleComponent component : panel.getModules()) {
                    if (searchCheck(component.getModule().getName())) {
                        continue;
                    }
                    float moduleY = component.getY();
                    float moduleHeight = component.getHeight();

                    if (moduleY < panelTop || moduleY + moduleHeight > panelBottom) {
                        continue;
                    }
                    if (RenderUtility.isInRegion(mouseX, mouseY - (int) yOffset, component.getX(), component.getY(), component.getWidth(), 20)) {
                        moduleDescription = component.getModule().getDesc();
                        break;
                    }
                }
                if (!moduleDescription.isEmpty()) {
                    break;
                }
            }

            if (!moduleDescription.isEmpty()) {
                float descAlpha = 1.0f - searchFocusValue * 2;
                float descWidth = Fonts.sfuy.getWidth(moduleDescription, 8) + 8;

                RenderUtility.drawShadow(
                        mouseX - descWidth / 2f - 2,
                        mouseY - 19,
                        descWidth + 4,
                        14,
                        6,
                        ColorUtils.rgba(0, 0, 0, (int) (60 * animation.getValue() * descAlpha))
                );

                RenderUtility.drawRoundedRect(
                        mouseX - descWidth / 2f,
                        mouseY - 16,
                        descWidth,
                        11,
                        new Vector4f(4, 4, 4, 4),
                        ColorUtils.rgba(20, 20, 20, (int) (200 * animation.getValue() * descAlpha))
                );

                Fonts.sfuy.drawCenteredText(
                        matrixStack,
                        moduleDescription,
                        mouseX,
                        mouseY - 14.5f,
                        ColorUtils.rgba(255, 255, 255, (int) (255 * animation.getValue() * descAlpha)),
                        8
                );
            }
        }

        for (Panel p : panels) {
            p.renderOverlays(matrixStack, mouseX, mouseY);
        }
        for (Panel p : test) {
            p.renderOverlays(matrixStack, mouseX, mouseY);
        }

        mc.gameRenderer.setupOverlayRendering();

        if (animation.getValue() <= 0.01 && animation.getToValue() == 0) {
            MultiBoxComponent.closeAllOpen();
            mc.displayGuiScreen(null);
            return;
        }
    }

    @Override
    protected void init() {
        animation.setValue(0.01);
        animation.animate(1, 0.4, Easings.EXPO_OUT);
        gradientAnimation.setValue(0.01);
        gradientAnimation.animate(1, 0.4, Easings.EXPO_OUT);

        int windowWidth = ClientUtil.calc(mc.getMainWindow().getScaledWidth());
        int windowHeight = ClientUtil.calc(mc.getMainWindow().getScaledHeight());

        float searchW = 250;
        float x = windowWidth / 2f - searchW / 2f;
        float y = 20;

        searchField = new SearchField((int) x, (int) y, (int) searchW, 20, "Поиск модуля...");

        restoreState();

        super.init();
    }

    @Override
    public void onClose() {
        saveState();
        MultiBoxComponent.closeAllOpen();

        animation.animate(0, 0.4, Easings.EXPO_OUT);
        gradientAnimation.animate(0, 0.4, Easings.EXPO_OUT);
        searchFocusAnimation.animate(0, 0.2f, Easings.EXPO_OUT);

        if (searchField != null) {
            searchField.setText("");
            searchField.setFocused(false);
        }

        for (Panel panel : panels) {
            panel.onClose();
        }
        for (Panel panel : test) {
            panel.onClose();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (searchField != null && searchField.isFocused()) {
                searchField.setFocused(false);
                searchField.setTyping(false);
                return true;
            }
            saveState();
            MultiBoxComponent.closeAllOpen();
            animation.animate(0, 0.5, Easings.EXPO_OUT);
            return true;
        }

        if (searchField != null && searchField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }

        for (Panel panel : panels) {
            panel.keyPressed(keyCode, scanCode, modifiers);
        }
        for (Panel panel : test) {
            panel.keyPressed(keyCode, scanCode, modifiers);
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (searchField != null) {
            searchField.keyReleased(keyCode, scanCode, modifiers);
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    private void updateScaleBasedOnScreenWidth() {
        final float PANEL_WIDTH = 115;
        final float MARGIN = 10;
        final float MIN_SCALE = 0.5f;

        float totalPanelWidth = panels.size() * (PANEL_WIDTH + MARGIN);
        float screenWidth = mc.getMainWindow().getScaledWidth();

        if (totalPanelWidth >= screenWidth && screenWidth > 0) {
            scale = screenWidth / totalPanelWidth;
            scale = MathHelper.clamp(scale, MIN_SCALE, 1.0f);
        } else {
            scale = 1f;
        }
    }

    private Vec2i adjustMouseCoordinates(int mouseX, int mouseY) {
        int windowWidth = mc.getMainWindow().getScaledWidth();
        int windowHeight = mc.getMainWindow().getScaledHeight();

        float animationValue = (float) animation.getValue();
        float adjustedScale = scale * animationValue;

        if (adjustedScale == 0) {
            adjustedScale = 1;
        }

        float adjustedMouseX = (mouseX - windowWidth / 2f) / adjustedScale + windowWidth / 2f;
        float adjustedMouseY = (mouseY - windowHeight / 2f) / adjustedScale + windowHeight / 2f;

        return new Vec2i((int) adjustedMouseX, (int) adjustedMouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Vec2i fixMouse = adjustMouseCoordinates((int) mouseX, (int) mouseY);
        mouseX = fixMouse.getX();
        mouseY = fixMouse.getY();

        if (handleThemePickerClick((float) mouseX, (float) mouseY, button)) {
            return true;
        }

        if (handleThemePickerDragStart((float) mouseX, (float) mouseY, button)) {
            return true;
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && MathUtil.isHovered((float) mouseX, (float) mouseY, autoBuyButtonX, autoBuyButtonY, AUTOBUY_BUTTON_WIDTH, AUTOBUY_BUTTON_HEIGHT)) {
            mc.displayGuiScreen(null);
            return true;
        }

        if (dispatchDropdownMouseClick((float) mouseX, (float) mouseY, button)) {
            return true;
        }

        if (searchField != null && searchField.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        for (Panel panel : panels) {
            if (MathUtil.isHovered((float) mouseX, (float) mouseY, panel.getX(), panel.getY(), panel.getWidth(), panel.getHeight())) {
                panel.mouseClick((float) mouseX, (float) mouseY, button);
            }
        }
        for (Panel panel : test) {
            if (MathUtil.isHovered((float) mouseX, (float) mouseY, panel.getX(), panel.getY(), panel.getWidth(), panel.getHeight())) {
                panel.mouseClick((float) mouseX, (float) mouseY, button);
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        Vec2i fixMouse = adjustMouseCoordinates((int) mouseX, (int) mouseY);
        mouseX = fixMouse.getX();
        mouseY = fixMouse.getY();

        if (handleThemePickerDragEnd(button)) {
            return true;
        }

        if (dispatchDropdownMouseRelease((float) mouseX, (float) mouseY, button)) {
            return true;
        }

        for (Panel panel : panels) {
            panel.mouseRelease((float) mouseX, (float) mouseY, button);
        }
        for (Panel panel : test) {
            panel.mouseRelease((float) mouseX, (float) mouseY, button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        Vec2i fixMouse = adjustMouseCoordinates((int) mouseX, (int) mouseY);
        mouseX = fixMouse.getX();
        mouseY = fixMouse.getY();

        if (handleThemePickerDragMove((float) mouseX, (float) mouseY)) {
            return true;
        }

        for (Panel panel : panels) {
            if (MathUtil.isHovered((float) mouseX, (float) mouseY, panel.getX(), panel.getY(), panel.getWidth(), panel.getHeight())) {
                panel.mouseDragged((float) mouseX, (float) mouseY, button);
            }
        }
        for (Panel panel : test) {
            if (MathUtil.isHovered((float) mouseX, (float) mouseY, panel.getX(), panel.getY(), panel.getWidth(), panel.getHeight())) {
                panel.mouseDragged((float) mouseX, (float) mouseY, button);
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    private boolean dispatchDropdownMouseClick(float mx, float my, int button) {
        MultiBoxComponent topMostOpen = null;

        for (int p = panels.size() - 1; p >= 0; p--) {
            Panel panel = panels.get(p);
            for (ModuleComponent mcpt : panel.getModules()) {
                for (xd.harm.ui.clickgui.components.builder.Component comp : mcpt.getComponents()) {
                    if (comp instanceof MultiBoxComponent) {
                        MultiBoxComponent mb = (MultiBoxComponent) comp;
                        if (mb.isDropdownOpen()) {
                            if (mb.isMouseOverDropdown(mx, my)) {
                                mb.mouseClick(mx, my, button);
                                return true;
                            }
                            if (topMostOpen == null) topMostOpen = mb;
                        }
                    }
                }
            }
        }
        if (topMostOpen != null) {
            topMostOpen.mouseClick(mx, my, button);
            return true;
        }
        return false;
    }

    private boolean dispatchDropdownMouseRelease(float mx, float my, int button) {
        for (int p = panels.size() - 1; p >= 0; p--) {
            Panel panel = panels.get(p);
            for (ModuleComponent mcpt : panel.getModules()) {
                for (xd.harm.ui.clickgui.components.builder.Component comp : mcpt.getComponents()) {
                    if (comp instanceof MultiBoxComponent) {
                        MultiBoxComponent mb = (MultiBoxComponent) comp;
                        if (mb.isDropdownOpen()) {
                            mb.mouseRelease(mx, my, button);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private void renderThemePicker(MatrixStack stack, float mouseX, float mouseY, int windowWidth, int windowHeight, float animValue, float searchFocusValue) {
        ThemePickerLayout layout = buildThemePickerLayout(windowWidth, windowHeight);
        if (layout == null) {
            return;
        }

        if (!isCustomThemeSelected()) {
            ColorComponent opened = ColorComponent.getOpened();
            if (opened == themeVisualsPicker || opened == themeTextPicker || opened == themeRectPicker) {
                ColorComponent.closeAll();
            }
        } else {
            syncCustomThemeColors();
        }

        float alphaMul = animValue * (1.0f - searchFocusValue * 0.35f);
        float panelRadius = 6.3f;
        int outerOutline = ColorUtils.rgba(255, 255, 255, (int) (6 * alphaMul));
        int innerOutline = ColorUtils.rgba(255, 255, 255, (int) (13 * alphaMul));
        int panelBg = ColorUtils.rgba(10, 10, 10, (int) (136 * alphaMul));

        if (alphaMul >= 0.9f) {
            GaussianBlur.startBlur();
            RenderUtility.drawRoundedRect(layout.x, layout.y, layout.width, layout.height, panelRadius, -1);
            GaussianBlur.endBlur(5, 2);
        }

        RenderUtility.drawRoundedRect(
                layout.x - 0.35f,
                layout.y - 0.35f,
                layout.width + 0.7f,
                layout.height + 0.7f,
                panelRadius + 0.35f,
                outerOutline
        );
        RenderUtility.drawRoundedRect(
                layout.x,
                layout.y,
                layout.width,
                layout.height,
                panelRadius,
                panelBg
        );
        RenderUtility.drawRoundedRectOutline(layout.x, layout.y, layout.width, layout.height, panelRadius, 0.35f, innerOutline);

        Fonts.sfuy.drawCenteredText(
                stack,
                "Themes",
                layout.x + layout.width / 2f,
                layout.y + THEME_BLOCK_PADDING + 0.9f,
                ColorUtils.rgba(235, 235, 235, (int) (215 * alphaMul)),
                6.6f
        );

        String currentTheme = Theme.THEME.get();
        float timeProgress = ((System.currentTimeMillis() % 5200L) / 5200.0f);
        float timeRadians = timeProgress * ((float) Math.PI * 2.0f);
        for (int i = 0; i < layout.themeCount; i++) {
            float cellX = layout.gridX + (i % layout.columns) * (THEME_CELL_SIZE + THEME_CELL_GAP);
            float cellY = layout.gridY + (i / layout.columns) * (THEME_CELL_SIZE + THEME_CELL_GAP);

            int[] colors = getThemeColors(i);
            int mainColor = colors[0];
            int rectColor = colors[1];
            float phaseBase = timeRadians + i * 0.55f;
            float waveX = (MathHelper.sin(phaseBase) + 1.0f) * 0.5f;
            float waveY = (MathHelper.sin(phaseBase + 1.57f) + 1.0f) * 0.5f;
            float m1 = 0.24f + waveX * 0.24f;
            float m2 = 0.76f - waveY * 0.24f;
            float m3 = 0.76f - waveX * 0.24f;
            float m4 = 0.24f + waveY * 0.24f;
            int alpha = (int) (232 * alphaMul);
            int c1 = ColorUtils.setAlpha(ColorUtils.interpolate(mainColor, rectColor, m1), alpha);
            int c2 = ColorUtils.setAlpha(ColorUtils.interpolate(mainColor, rectColor, m2), alpha);
            int c3 = ColorUtils.setAlpha(ColorUtils.interpolate(mainColor, rectColor, m3), alpha);
            int c4 = ColorUtils.setAlpha(ColorUtils.interpolate(mainColor, rectColor, m4), alpha);

            RenderUtility.drawRoundedRect(
                    cellX,
                    cellY,
                    THEME_CELL_SIZE,
                    THEME_CELL_SIZE,
                    new Vector4f(2.8f, 2.8f, 2.8f, 2.8f),
                    new Vector4i(c1, c2, c3, c4)
            );

            RenderUtility.drawRoundedRect(
                    cellX + 0.5f,
                    cellY + 0.5f,
                    THEME_CELL_SIZE - 1.0f,
                    THEME_CELL_SIZE - 1.0f,
                    2.1f,
                    ColorUtils.rgba(255, 255, 255, (int) (7 * alphaMul))
            );

            boolean selected = currentTheme != null && currentTheme.equalsIgnoreCase(layout.themeNames[i]);
            if (selected) {
                RenderUtility.drawRoundedRectOutline(
                        cellX - 0.55f,
                        cellY - 0.55f,
                        THEME_CELL_SIZE + 1.1f,
                        THEME_CELL_SIZE + 1.1f,
                        3.0f,
                        0.62f,
                        ColorUtils.rgba(245, 245, 245, (int) (205 * alphaMul))
                );
            } else if (MathUtil.isHovered(mouseX, mouseY, cellX, cellY, THEME_CELL_SIZE, THEME_CELL_SIZE)) {
                RenderUtility.drawRoundedRectOutline(
                        cellX - 0.28f,
                        cellY - 0.28f,
                        THEME_CELL_SIZE + 0.56f,
                        THEME_CELL_SIZE + 0.56f,
                        2.7f,
                        0.45f,
                        ColorUtils.rgba(235, 235, 235, (int) (105 * alphaMul))
                );
            }
        }

        if (isCustomThemeSelected()) {
            ThemeCustomPickerPanelLayout pickerPanel = buildThemeCustomPickerPanel(layout, windowWidth, windowHeight);
            if (pickerPanel != null) {
                layoutThemeCustomPicker(pickerPanel);
                renderThemeCustomPickerPanel(pickerPanel, alphaMul);
                themeVisualsPicker.render(stack, mouseX, mouseY);
            }
        }
    }

    private void renderAutoBuyButton(MatrixStack stack, float mouseX, float mouseY, int windowWidth, int windowHeight, float animValue, float searchFocusValue) {
        float alphaMul = animValue * (1.0f - searchFocusValue * 0.35f);
        if (alphaMul <= 0.01f) {
            return;
        }

        autoBuyButtonX = windowWidth - AUTOBUY_BUTTON_WIDTH - THEME_EDGE_MARGIN;
        autoBuyButtonY = windowHeight - AUTOBUY_BUTTON_HEIGHT - THEME_EDGE_MARGIN;

        boolean hovered = MathUtil.isHovered(mouseX, mouseY, autoBuyButtonX, autoBuyButtonY, AUTOBUY_BUTTON_WIDTH, AUTOBUY_BUTTON_HEIGHT);
        autoBuyButtonAnimation.animate(hovered ? 1 : 0, 0.25f, Easings.EXPO_OUT);
        float hover = (float) autoBuyButtonAnimation.getValue();

        int baseColor = ColorUtils.rgba(10, 10, 10, (int) (148 * alphaMul));
        int hoverColor = ColorUtils.setAlpha(Theme.MainColor(0), (int) (95 * alphaMul));
        int buttonColor = ColorUtils.interpolateColor(baseColor, hoverColor, hover);

        RenderUtility.drawRoundedRect(autoBuyButtonX, autoBuyButtonY, AUTOBUY_BUTTON_WIDTH, AUTOBUY_BUTTON_HEIGHT, 5.8f, buttonColor);

        Fonts.sfuy.drawCenteredText(
                stack,
                "AutoBuy",
                autoBuyButtonX + AUTOBUY_BUTTON_WIDTH / 2f,
                autoBuyButtonY + AUTOBUY_BUTTON_HEIGHT / 2f - 3.2f,
                ColorUtils.rgba(245, 245, 245, (int) (240 * alphaMul)),
                6.8f
        );
    }

    private boolean handleThemePickerClick(float mouseX, float mouseY, int button) {
        int windowWidth = ClientUtil.calc(mc.getMainWindow().getScaledWidth());
        int windowHeight = ClientUtil.calc(mc.getMainWindow().getScaledHeight());
        ThemePickerLayout layout = buildThemePickerLayout(windowWidth, windowHeight);
        if (layout == null) {
            return false;
        }

        if (isCustomThemeSelected()) {
            ThemeCustomPickerPanelLayout pickerPanel = buildThemeCustomPickerPanel(layout, windowWidth, windowHeight);
            if (pickerPanel != null && MathUtil.isHovered(mouseX, mouseY, pickerPanel.x, pickerPanel.y, pickerPanel.width, pickerPanel.height)) {
                layoutThemeCustomPicker(pickerPanel);
                themeVisualsPicker.mouseClick(mouseX, mouseY, button);
                return true;
            }
        }

        if (!MathUtil.isHovered(mouseX, mouseY, layout.x, layout.y, layout.width, layout.height)) {
            return false;
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            for (int i = 0; i < layout.themeCount; i++) {
                float cellX = layout.gridX + (i % layout.columns) * (THEME_CELL_SIZE + THEME_CELL_GAP);
                float cellY = layout.gridY + (i / layout.columns) * (THEME_CELL_SIZE + THEME_CELL_GAP);
                if (MathUtil.isHovered(mouseX, mouseY, cellX, cellY, THEME_CELL_SIZE, THEME_CELL_SIZE)) {
                    Theme.THEME.set(layout.themeNames[i]);
                    return true;
                }
            }
        }

        return button == GLFW.GLFW_MOUSE_BUTTON_LEFT;
    }

    private int[] getThemeColors(int index) {
        String[] names = Theme.THEME.strings;
        if (names == null || names.length == 0) {
            return new int[]{ColorUtils.rgb(130, 130, 255), ColorUtils.rgb(40, 40, 65)};
        }

        String original = Theme.THEME.get();
        String target = names[MathHelper.clamp(index, 0, names.length - 1)];
        Theme.THEME.set(target);
        int main = Theme.MainColor(0);
        int rect = Theme.RectColor(0);
        Theme.THEME.set(original);
        return new int[]{main, rect};
    }

    private ThemePickerLayout buildThemePickerLayout(int windowWidth, int windowHeight) {
        String[] themeNames = Theme.THEME.strings;
        if (themeNames == null || themeNames.length == 0) {
            return null;
        }

        int columns = Math.min(THEMES_PER_ROW, themeNames.length);
        int rows = (int) Math.ceil(themeNames.length / (float) columns);
        float gridWidth = columns * THEME_CELL_SIZE + (columns - 1) * THEME_CELL_GAP;
        float gridHeight = rows * THEME_CELL_SIZE + (rows - 1) * THEME_CELL_GAP;

        float width = Math.max(gridWidth + THEME_BLOCK_PADDING * 2f, THEME_BLOCK_MIN_WIDTH);
        float height = gridHeight + THEME_BLOCK_PADDING * 2f + THEME_HEADER_HEIGHT;

        float defaultX = windowWidth - width - THEME_EDGE_MARGIN;
        float defaultY = THEME_EDGE_MARGIN;

        if (!themePickerPositionInitialized) {
            if (themePickerDrag.getX() < 0 || themePickerDrag.getY() < 0) {
                themePickerDrag.setX(defaultX);
                themePickerDrag.setY(defaultY);
            }
            themePickerPositionInitialized = true;
        }

        float x = themePickerDrag.getX();
        float y = themePickerDrag.getY();
        x = MathHelper.clamp(x, THEME_EDGE_MARGIN, Math.max(THEME_EDGE_MARGIN, windowWidth - width - THEME_EDGE_MARGIN));
        y = MathHelper.clamp(y, THEME_EDGE_MARGIN, Math.max(THEME_EDGE_MARGIN, windowHeight - height - THEME_EDGE_MARGIN));

        themePickerDrag.setX(x);
        themePickerDrag.setY(y);
        themePickerDrag.setWidth(width);
        themePickerDrag.setHeight(height);

        float gridX = x + (width - gridWidth) / 2f;
        float gridY = y + THEME_BLOCK_PADDING + THEME_HEADER_HEIGHT;
        return new ThemePickerLayout(x, y, width, height, gridX, gridY, gridHeight, columns, themeNames.length, themeNames);
    }

    private ThemeCustomPickerPanelLayout buildThemeCustomPickerPanel(ThemePickerLayout layout, int windowWidth, int windowHeight) {
        if (!isCustomThemeSelected()) {
            return null;
        }

        float panelWidth = Math.min(windowWidth - THEME_EDGE_MARGIN * 2f, layout.width + THEME_CUSTOM_WIDTH_EXTRA);
        float pickerWidth = panelWidth - THEME_BLOCK_PADDING * 2f;
        themeVisualsPicker.setWidth(pickerWidth);
        float pickerHeight = Math.max(THEME_CUSTOM_MIN_HEIGHT, THEME_CUSTOM_PICKER_HEIGHT);
        float panelHeight = pickerHeight + THEME_BLOCK_PADDING * 2f;

        float panelX = layout.x + (layout.width - panelWidth) / 2f;
        float panelY = layout.y + layout.height + THEME_CUSTOM_GAP;
        if (panelY + panelHeight > windowHeight - THEME_EDGE_MARGIN) {
            panelY = layout.y - panelHeight - THEME_CUSTOM_GAP;
        }

        panelX = MathHelper.clamp(panelX, THEME_EDGE_MARGIN, Math.max(THEME_EDGE_MARGIN, windowWidth - panelWidth - THEME_EDGE_MARGIN));
        panelY = MathHelper.clamp(panelY, THEME_EDGE_MARGIN, Math.max(THEME_EDGE_MARGIN, windowHeight - panelHeight - THEME_EDGE_MARGIN));
        return new ThemeCustomPickerPanelLayout(panelX, panelY, panelWidth, panelHeight);
    }

    private void layoutThemeCustomPicker(ThemeCustomPickerPanelLayout panel) {
        float pickerX = panel.x + THEME_BLOCK_PADDING;
        float pickerY = panel.y + THEME_BLOCK_PADDING;
        float pickerWidth = panel.width - THEME_BLOCK_PADDING * 2f;
        themeVisualsPicker.setX(pickerX);
        themeVisualsPicker.setY(pickerY);
        themeVisualsPicker.setWidth(pickerWidth);
    }

    private void renderThemeCustomPickerPanel(ThemeCustomPickerPanelLayout panel, float alphaMul) {
        renderThemeGlassRect(panel.x, panel.y, panel.width, panel.height, 5.5f, alphaMul);
    }

    private void renderThemeGlassRect(float x, float y, float width, float height, float radius, float alphaMul) {
        int outer = ColorUtils.rgba(255, 255, 255, (int) (8 * alphaMul));
        int inner = ColorUtils.rgba(255, 255, 255, (int) (14 * alphaMul));
        int bg = ColorUtils.rgba(10, 10, 10, (int) (136 * alphaMul));

        if (alphaMul >= 0.9f) {
            GaussianBlur.startBlur();
            RenderUtility.drawRoundedRect(x, y, width, height, radius, -1);
            GaussianBlur.endBlur(5, 2);
        }

        RenderUtility.drawRoundedRect(x - 0.35f, y - 0.35f, width + 0.7f, height + 0.7f, radius + 0.35f, outer);
        RenderUtility.drawRoundedRect(x, y, width, height, radius, bg);
        RenderUtility.drawRoundedRectOutline(x, y, width, height, radius, 0.35f, inner);
    }

    private boolean isCustomThemeSelected() {
        String current = Theme.THEME.get();
        return current != null && current.equalsIgnoreCase("Свой");
    }

    private void syncCustomThemeColors() {
        int unified = Theme.visualscolor.get();
        if (Theme.textcolor.get() != unified) {
            Theme.textcolor.set(unified);
        }
        if (Theme.rectcolor.get() != unified) {
            Theme.rectcolor.set(unified);
        }
    }

    private boolean handleThemePickerDragStart(float mouseX, float mouseY, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }

        int windowWidth = ClientUtil.calc(mc.getMainWindow().getScaledWidth());
        int windowHeight = ClientUtil.calc(mc.getMainWindow().getScaledHeight());
        ThemePickerLayout layout = buildThemePickerLayout(windowWidth, windowHeight);
        if (layout == null) {
            return false;
        }

        float dragAreaHeight = THEME_BLOCK_PADDING + THEME_HEADER_HEIGHT + 1.2f;
        if (!MathUtil.isHovered(mouseX, mouseY, layout.x, layout.y, layout.width, dragAreaHeight)) {
            return false;
        }

        themePickerDragging = true;
        themePickerDragOffsetX = mouseX - layout.x;
        themePickerDragOffsetY = mouseY - layout.y;
        return true;
    }

    private boolean handleThemePickerDragMove(float mouseX, float mouseY) {
        if (!themePickerDragging) {
            return false;
        }

        int windowWidth = ClientUtil.calc(mc.getMainWindow().getScaledWidth());
        int windowHeight = ClientUtil.calc(mc.getMainWindow().getScaledHeight());
        ThemePickerLayout layout = buildThemePickerLayout(windowWidth, windowHeight);
        if (layout == null) {
            return false;
        }

        float newX = mouseX - themePickerDragOffsetX;
        float newY = mouseY - themePickerDragOffsetY;
        newX = MathHelper.clamp(newX, THEME_EDGE_MARGIN, Math.max(THEME_EDGE_MARGIN, windowWidth - layout.width - THEME_EDGE_MARGIN));
        newY = MathHelper.clamp(newY, THEME_EDGE_MARGIN, Math.max(THEME_EDGE_MARGIN, windowHeight - layout.height - THEME_EDGE_MARGIN));
        themePickerDrag.setX(newX);
        themePickerDrag.setY(newY);
        return true;
    }

    private boolean handleThemePickerDragEnd(int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || !themePickerDragging) {
            return false;
        }

        themePickerDragging = false;
        DragManager.save();
        return true;
    }

    private void dispatchThemeCustomPickersCharTyped(char codePoint, int modifiers) {
        if (isCustomThemeSelected()) {
            themeVisualsPicker.charTyped(codePoint, modifiers);
        }
    }

    private static class ThemePickerLayout {
        final float x;
        final float y;
        final float width;
        final float height;
        final float gridX;
        final float gridY;
        final float gridHeight;
        final int columns;
        final int themeCount;
        final String[] themeNames;

        ThemePickerLayout(float x, float y, float width, float height, float gridX, float gridY, float gridHeight, int columns, int themeCount, String[] themeNames) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.gridX = gridX;
            this.gridY = gridY;
            this.gridHeight = gridHeight;
            this.columns = columns;
            this.themeCount = themeCount;
            this.themeNames = themeNames;
        }
    }

    private static class ThemeCustomPickerPanelLayout {
        final float x;
        final float y;
        final float width;
        final float height;

        ThemeCustomPickerPanelLayout(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    public static void resetState() {
        savedScrollPositions.clear();
        savedExpandedModules.clear();
        stateInitialized = false;
    }
}
