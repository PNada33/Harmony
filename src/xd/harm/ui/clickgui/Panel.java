package xd.harm.ui.clickgui;

import com.mojang.blaze3d.platform.GlStateManager;
import xd.harm.Harmony;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.impl.render.ClickGui;
import xd.harm.modules.impl.render.FiguraCosmetic;
import xd.harm.modules.impl.render.Theme;
import xd.harm.ui.clickgui.components.ModuleComponent;
import xd.harm.ui.clickgui.components.builder.Component;
import xd.harm.ui.clickgui.components.builder.IBuilder;
import xd.harm.ui.clickgui.components.settings.MultiBoxComponent;
import xd.harm.utils.math.MathUtil;
import xd.harm.utils.render.GaussianBlur;
import xd.harm.utils.render.color.ColorUtils;
import xd.harm.utils.render.font.Fonts;
import xd.harm.utils.render.gl.Stencil;
import xd.harm.utils.render.rect.RenderUtility;
import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.Getter;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector4f;
import org.joml.Vector2d;
import ru.hogoshi.Animation;
import ru.hogoshi.util.Easings;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Panel implements IBuilder {
    public Category getCategory() { return category; }
    public float getX() { return x; }
    public float getY() { return y; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public List<ModuleComponent> getModules() { return modules; }

    private final Vector4f ROUNDING_VECTOR = new Vector4f(8, 8, 8, 8);
    private final Category category;
    protected float x;
    protected float y;
    private float scroll, animatedScroll, scrollbarHeight, scrollbarY;
    private float animatedScrollbarY;
    private float scrollbarAlpha = 0;
    protected final float width = 120;
    protected final float height = 600 / 2f;
    private boolean draggingScrollbar = false;
    private float lastMouseY;
    private float max = 0;
    private final DropDown dropDown;

    private List<ModuleComponent> modules = new ArrayList<>();

    private Animation scrollbarHoverAnim = new Animation();
    private Animation headerLineAnim = new Animation();

    private static final float HEADER_HEIGHT = 22;
    private static final float CONTENT_PADDING = 5;
    private static final float MODULE_SPACING = 3.5f;

    public Panel(Category category, DropDown dropDown) {
        this.category = category;
        this.dropDown = dropDown;

        for (Module function : Harmony.getInstance().getModuleManager().getModules()) {
            if (function.getCategory() == category && function.isVisibleInClickGui()) {
                ModuleComponent component = new ModuleComponent(function);
                component.setPanel(this);
                modules.add(component);
            }
        }

        scrollbarHoverAnim = scrollbarHoverAnim.animate(0, 0.25f, Easings.EXPO_OUT);
        headerLineAnim = headerLineAnim.animate(0, 1.5f, Easings.SINE_BOTH);
    }

    @Override
    public void render(MatrixStack stack, float mouseX, float mouseY) {
        render(stack, mouseX, mouseY, 1.0f);
    }

    public void render(MatrixStack stack, float mouseX, float mouseY, float alpha) {
        ClickGui clickGui = Harmony.getInstance().getModuleManager().getClickGui();

        updateMaxScroll();
        clampScroll();

        animatedScroll = MathUtil.fast(animatedScroll, scroll, 15);
        animatedScrollbarY = MathUtil.fast(animatedScrollbarY, scrollbarY, 12);
        scrollbarHoverAnim.update();
        headerLineAnim.update();

        float panelX = this.x;
        float panelY = this.y;
        float panelWidth = this.width;
        float panelHeight = this.height;
        float panelRadius = 8;

        int themeColor = Theme.MainColor(0);

        GlStateManager.color4f(1, 1, 1, 1);

        if (alpha >= 0.95f) {
            GaussianBlur.startBlur();
            RenderUtility.drawRoundedRect(
                    panelX,
                    panelY,
                    panelWidth,
                    panelHeight,
                    panelRadius,
                    -1
            );
            GaussianBlur.endBlur(5, 2);
        }

        GlStateManager.color4f(1, 1, 1, alpha);

        RenderUtility.drawRoundedRect(
                panelX - 0.5f,
                panelY - 0.5f,
                panelWidth + 1f,
                panelHeight + 1f,
                panelRadius + 0.5f,
                ColorUtils.rgba(255, 255, 255, (int)(8 * alpha))
        );

        int bgAlpha = alpha >= 0.95f ? 160 : 200;
        RenderUtility.drawRoundedRect(
                panelX,
                panelY,
                panelWidth,
                panelHeight,
                panelRadius,
                ColorUtils.rgba(10, 10, 10, (int)(bgAlpha * alpha))
        );

        String categoryName = category.name().toUpperCase();
        float textY = panelY + HEADER_HEIGHT / 2f - 4;

        Fonts.sfuy.drawCenteredText(
                stack,
                categoryName,
                panelX + panelWidth / 2f,
                textY,
                ColorUtils.rgba(255, 255, 255, (int)(220 * alpha)),
                7.5f
        );

        headerLineAnim = headerLineAnim.animate(1, 1.5f, Easings.SINE_BOTH);
        float lineProgress = (float) (Math.sin(System.currentTimeMillis() / 1500.0) * 0.15 + 0.85);

        float lineWidth = (panelWidth - 16) * lineProgress;
        float lineX = panelX + (panelWidth - lineWidth) / 2;

        RenderUtility.drawRectW(
                lineX,
                panelY + HEADER_HEIGHT - 0.5f,
                lineWidth,
                0.5f,
                ColorUtils.setAlpha(themeColor, (int)(35 * alpha))
        );

        float contentTop = panelY + HEADER_HEIGHT + 2;
        float contentBottom = panelY + panelHeight - 10.5f;;
        float contentHeight = contentBottom - contentTop ;

        Stencil.initStencilToWrite();
        RenderUtility.drawRoundedRect(
                panelX + 4,
                contentTop,
                panelWidth - 8,
                contentHeight,
                5,
                -1
        );
        Stencil.readStencilBuffer(1);

        float moduleY = contentTop + CONTENT_PADDING + animatedScroll;

        for (ModuleComponent component : modules) {
            if (searchCheck(component.getModule().getName())) {
                continue;
            }

            component.setX(panelX + 4);
            component.setY(moduleY);
            component.setWidth(panelWidth - 8);
            component.setHeight(20);

            component.getExpandAnim().update();
            if (component.getExpandAnim().getValue() > 0) {
                float componentOffset = 0;
                for (int ci = 0; ci < component.getComponents().size(); ci++) {
                    Component component2 = component.getComponents().get(ci);
                    if (component2.isVisible() && !component.isHiddenByCollapsedCategory(ci)) {
                        componentOffset += component2.getHeight();
                    }
                }
                componentOffset *= component.getExpandAnim().getValue();
                component.setHeight(component.getHeight() + componentOffset);
            }

            float moduleBottom = moduleY + component.getHeight();
            if (moduleBottom > contentTop && moduleY < contentBottom) {
                component.render(stack, mouseX, mouseY);
            }

            moduleY += component.getHeight() + MODULE_SPACING;
        }

        Stencil.uninitStencilBuffer();

        if (modules.stream().noneMatch(c -> !searchCheck(c.getModule().getName()))) {
            Fonts.sfuy.drawCenteredText(
                    stack,
                    "Empty",
                    panelX + panelWidth / 2f,
                    panelY + HEADER_HEIGHT + 25,
                    ColorUtils.rgba(255, 255, 255, (int)(60 * alpha)),
                    7
            );
        }

        float visibleHeight = contentHeight;
        if (max > visibleHeight) {
            scrollbarHeight = (visibleHeight * visibleHeight) / max;
            scrollbarHeight = MathHelper.clamp(scrollbarHeight, 15, visibleHeight - 10);

            float maxScroll = max - visibleHeight;
            float scrollProgress = maxScroll > 0 ? (-scroll / maxScroll) : 0;
            scrollbarY = contentTop + 2 + scrollProgress * (visibleHeight - scrollbarHeight - 4);

            scrollbarY = MathHelper.clamp(scrollbarY, contentTop + 2, contentBottom - scrollbarHeight - 2);

            float scrollbarX = panelX + panelWidth - 5;
            boolean scrollbarHovered = RenderUtility.isInRegion(
                    mouseX, mouseY,
                    scrollbarX - 2, scrollbarY - 2,
                    6, scrollbarHeight + 4
            );

            scrollbarHoverAnim = scrollbarHoverAnim.animate(
                    scrollbarHovered || draggingScrollbar ? 1 : 0,
                    0.25f,
                    Easings.EXPO_OUT
            );
            float hoverValue = (float) scrollbarHoverAnim.getValue();

            boolean shouldShow =  hoverValue > 0.01f;
            float targetAlpha = shouldShow ? 1.0f : 0.0f;
            scrollbarAlpha = MathUtil.fast(scrollbarAlpha, targetAlpha, 12);

            if (scrollbarAlpha > 0.01f) {
                float thumbWidth = 2f + hoverValue * 0.5f;
                float thumbX = scrollbarX - hoverValue * 0.25f;

                RenderUtility.drawRoundedRect(
                        thumbX,
                        animatedScrollbarY,
                        thumbWidth,
                        scrollbarHeight,
                        thumbWidth / 2,
                        ColorUtils.setAlpha(
                                ColorUtils.interpolateColor(
                                        ColorUtils.rgba(255, 255, 255, 40),
                                        themeColor,
                                        hoverValue * 0.3f
                                ),
                                (int)(255 * scrollbarAlpha * (0.4f + hoverValue * 0.3f) * alpha)
                        )
                );
            }
        }
    }

    public void renderOverlays(MatrixStack stack, float mouseX, float mouseY) {
        Stencil.uninitStencilBuffer();
        for (ModuleComponent module : modules) {
            for (int ci = 0; ci < module.getComponents().size(); ci++) {
                Component comp = module.getComponents().get(ci);
                if (module.isHiddenByCollapsedCategory(ci)) continue;
                if (comp instanceof MultiBoxComponent) {
                    ((MultiBoxComponent) comp).renderFloatingWindows(stack, mouseX, mouseY);
                }
            }
        }
    }

    public void setX(float x) {
        this.x = x;
    }

    public void setY(float y) {
        this.y = y;
    }

    public void setScroll(float scroll) {
        this.scroll = scroll;
    }

    public void setAnimatedScroll(float animatedScroll) {
        this.animatedScroll = animatedScroll;
    }

    public float getScrollOffset() {
        return this.scroll;
    }

    public void setScrollOffset(float offset) {
        this.scroll = offset;
        this.animatedScroll = offset;
    }

    public boolean searchCheck(String moduleName) {
        return dropDown != null && dropDown.searchCheck(moduleName);
    }

    public void onClose() {
        draggingScrollbar = false;
        scrollbarAlpha = 0;

        for (ModuleComponent component : modules) {
            component.setBind(false);
            component.getHoverAnim().animate(0, 0.2f, Easings.EXPO_OUT);
        }
    }

    public void onCloseWithReset() {
        setScroll(0);
        setAnimatedScroll(0);
        draggingScrollbar = false;
        scrollbarAlpha = 0;

        for (ModuleComponent component : modules) {
            component.setOpen(false);
            component.setBind(false);
            component.getExpandAnim().animate(0, component.getNoOpenAnimValue(), Easings.EXPO_OUT);
            component.getHoverAnim().animate(0, 0.2f, Easings.EXPO_OUT);
        }
    }

    private void clampScroll() {
        float visibleHeight = height - HEADER_HEIGHT - 6;
        float maxScroll = Math.max(0, max - visibleHeight);

        if (maxScroll <= 0) {
            scroll = 0;
        } else {
            scroll = MathHelper.clamp(scroll, -maxScroll, 0);
        }
    }

    public boolean onScroll(double mouseX, double mouseY, double delta) {
        Vector2d m = MathUtil.getMouse((int) mouseX, (int) mouseY);
        mouseX = m.x();
        mouseY = m.y();

        if (RenderUtility.isInRegion(mouseX, mouseY, x, y, width, height)) {
            updateMaxScroll();

            float visibleHeight = height - HEADER_HEIGHT - 6;
            float maxScroll = Math.max(0, max - visibleHeight);

            if (maxScroll <= 0) {
                scroll = 0;
                animatedScroll = 0;
                return true;
            }

            float newScroll = scroll + (float) (delta * 15);
            newScroll = MathHelper.clamp(newScroll, -maxScroll, 0);
            scroll = newScroll;
            return true;
        }
        return false;
    }

    private void updateMaxScroll() {
        float totalHeight = 0;
        for (ModuleComponent component : modules) {
            if (searchCheck(component.getModule().getName())) {
                continue;
            }

            float componentHeight = 20;
            if (component.isOpen() || component.getExpandAnim().getValue() > 0) {
                float componentOffset = 0;
                for (int ci = 0; ci < component.getComponents().size(); ci++) {
                    Component component2 = component.getComponents().get(ci);
                    if (component2.isVisible() && !component.isHiddenByCollapsedCategory(ci)) {
                        componentOffset += component2.getHeight();
                    }
                }
                if (component.getExpandAnim().getValue() > 0) {
                    componentOffset *= component.getExpandAnim().getValue();
                }
                componentHeight += componentOffset;
            }

            totalHeight += componentHeight + MODULE_SPACING;
        }

        max = totalHeight + CONTENT_PADDING;
    }

    @Override
    public void mouseClick(float mouseX, float mouseY, int button) {
        boolean handledByDropdown = false;
        for (ModuleComponent module : modules) {
            for (int ci = 0; ci < module.getComponents().size(); ci++) {
                Component comp = module.getComponents().get(ci);
                if (module.isHiddenByCollapsedCategory(ci)) continue;
                if (comp instanceof MultiBoxComponent) {
                    MultiBoxComponent mb = (MultiBoxComponent) comp;
                    if (mb.isDropdownOpen()) {
                        mb.mouseClick(mouseX, mouseY, button);
                        handledByDropdown = true;
                    }
                }
            }
        }
        if (handledByDropdown) return;

        float contentTop = y + HEADER_HEIGHT + 2;
        float contentBottom = y + height - 4;

        if (!RenderUtility.isInRegion(mouseX, mouseY, x + 4, contentTop, width - 8, contentBottom - contentTop))
            return;

        for (ModuleComponent component : modules) {
            if (searchCheck(component.getModule().getName())) continue;
            component.mouseClick(mouseX, mouseY, button);
        }

        float scrollx = getX() + getWidth() - 5;
        if (button == 0 && MathUtil.isHovered(mouseX, mouseY, scrollx - 2, scrollbarY - 2, 6, scrollbarHeight + 4)) {
            draggingScrollbar = true;
            lastMouseY = mouseY;
        }
    }

    @Override
    public void keyPressed(int key, int scanCode, int modifiers) {
        for (ModuleComponent component : modules) {
            if (searchCheck(component.getModule().getName())) {
                continue;
            }
            component.keyPressed(key, scanCode, modifiers);
        }
    }

    @Override
    public void charTyped(char codePoint, int modifiers) {
        for (ModuleComponent component : modules) {
            if (searchCheck(component.getModule().getName())) {
                continue;
            }
            component.charTyped(codePoint, modifiers);
        }
    }

    @Override
    public void mouseRelease(float mouseX, float mouseY, int button) {
        for (ModuleComponent module : modules) {
            for (int ci = 0; ci < module.getComponents().size(); ci++) {
                Component comp = module.getComponents().get(ci);
                if (module.isHiddenByCollapsedCategory(ci)) continue;
                if (comp instanceof MultiBoxComponent) {
                    ((MultiBoxComponent) comp).mouseRelease(mouseX, mouseY, button);
                }
            }
        }

        float contentTop = y + HEADER_HEIGHT + 2;
        float contentBottom = y + height - 4;

        if (!RenderUtility.isInRegion(mouseX, mouseY, x + 4, contentTop, width - 8, contentBottom - contentTop))
            return;

        for (ModuleComponent component : modules) {
            if (searchCheck(component.getModule().getName())) continue;
            component.mouseRelease(mouseX, mouseY, button);
        }

        if (button == 0) draggingScrollbar = false;
    }

    public void mouseDragged(float mouseX, float mouseY, int button) {
        if (draggingScrollbar && button == 0) {
            float deltaY = mouseY - lastMouseY;

            float visibleHeight = height - HEADER_HEIGHT - 6;
            float maxScroll = Math.max(0, max - visibleHeight);

            if (maxScroll <= 0) {
                scroll = 0;
                animatedScroll = 0;
                return;
            }

            float scrollbarRange = visibleHeight - scrollbarHeight - 4;
            if (scrollbarRange <= 0) return;

            float scrollPerPixel = maxScroll / scrollbarRange;

            float newScroll = scroll - deltaY * scrollPerPixel;
            newScroll = MathHelper.clamp(newScroll, -maxScroll, 0);
            setScroll(newScroll);

            lastMouseY = mouseY;
        }
    }
}
