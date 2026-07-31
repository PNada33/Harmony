package xd.harm.ui.clickgui.minidrop;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.util.math.MathHelper;
import xd.harm.Harmony;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.impl.render.FiguraCosmetic;
import xd.harm.ui.clickgui.minidrop.components.ModuleComponent;
import xd.harm.ui.clickgui.minidrop.components.builder.Component;
import xd.harm.ui.clickgui.minidrop.components.builder.IBuilder;
import xd.harm.ui.clickgui.minidrop.utils.MathUtil;
import xd.harm.ui.clickgui.minidrop.utils.ColorUtils;
import xd.harm.ui.clickgui.minidrop.utils.RenderHelper;
import xd.harm.utils.render.font.Fonts;

import java.util.ArrayList;
import java.util.List;

public class Panel implements IBuilder {

    protected final float width = 115;
    private final Category category;
    public boolean binding;
    protected float x;
    protected float y;
    protected float height;
    double base = 20;
    double biba = 28.5;
    double boba = 8.5;
    float max = 0;
    private List<ModuleComponent> modules = new ArrayList<>();
    private float scroll, animatedScroll;

    public Category getCategory() { return category; }
    public float getX() { return x; }
    public void setX(float x) { this.x = x; }
    public float getY() { return y; }
    public void setY(float y) { this.y = y; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public void setHeight(float height) { this.height = height; }
    public boolean isBinding() { return binding; }
    public List<ModuleComponent> getModules() { return modules; }

    public Panel(Category category) {
        this.category = category;

        for (Module module : Harmony.getInstance().getModuleManager().getModules()) {
            if (module.getCategory() == category && module.isVisibleInClickGui()) {
                ModuleComponent component = new ModuleComponent(module);
                component.setPanel(this);
                modules.add(component);
            }
        }

        updateHeight();
    }

    public void updateHeight() {
        final double additionalHeight = modules.stream()
                .filter(ModuleComponent::isOpen)
                .mapToDouble(Component::getHeight)
                .sum();

        this.height = (float) Math.max(biba, base + additionalHeight + boba);
    }

    @Override
    public void render(MatrixStack stack, float mouseX, float mouseY) {
        animatedScroll = MathUtil.fast(animatedScroll, scroll, 10);
        float headerFont = 9;

        float totalHeight = 0;
        for (ModuleComponent module : modules) {
            totalHeight += module.getHeight();
        }
        height = (float) Math.max(biba, totalHeight + base + boba);

        int backgroundColor = ColorUtils.rgba(23, 23, 25, 150);

        RenderHelper.drawRoundedRect(x, y, width, height, 4, backgroundColor);

        Fonts.sfui.drawText(stack, category.getName(), Math.round(x + 9), Math.round(y + 6), -1, 7);

        RenderHelper.drawRectHorizontalW(x + 0.5f, y + 18 + headerFont / 2f, width - 1, 2.5f,
                ColorUtils.rgba(0, 0, 0, 0), ColorUtils.rgba(0, 0, 0, 65));

        drawComponents(stack, mouseX, mouseY);
    }

    private void drawComponents(MatrixStack stack, float mouseX, float mouseY) {
        float offset = -1;
        float header = 25;

        if (max > height - header - 10) {
            scroll = MathHelper.clamp(scroll, -max + height - header - 10, 0);
            animatedScroll = MathHelper.clamp(animatedScroll, -max + height - header - 10, 0);
        } else {
            scroll = 0;
            animatedScroll = 0;
        }

        for (ModuleComponent component : modules) {
            component.setX(getX() + 0.5f);
            component.setY(getY() + header + offset + 0.5f + animatedScroll);
            component.setWidth(getWidth() - 1);
            component.setHeight(20);

            binding = component.isBind();

            if (component.animation > 0 && ClickGuiScreen.getInstance().getExpandedModule() == component) {
                float componentOffset = 0;
                for (Component component2 : component.getComponents()) {
                    if (component2.isVisible())
                        componentOffset += component2.getHeight();
                }
                componentOffset *= component.animation;
                component.setHeight(component.getHeight() + componentOffset);
            }

            component.render(stack, mouseX, mouseY);
            offset += component.getHeight() + 0.1f;
        }

        max = offset;
    }

    @Override
    public void mouseClick(float mouseX, float mouseY, int button) {
        for (ModuleComponent component : modules) {
            component.mouseClick(mouseX, mouseY, button);
        }
    }

    @Override
    public void keyPressed(int key, int scanCode, int modifiers) {
        for (ModuleComponent component : modules) {
            component.keyPressed(key, scanCode, modifiers);
        }
    }

    @Override
    public void charTyped(char codePoint, int modifiers) {
        for (ModuleComponent component : modules) {
            component.charTyped(codePoint, modifiers);
        }
    }

    @Override
    public void mouseRelease(float mouseX, float mouseY, int button) {
        for (ModuleComponent component : modules) {
            component.mouseRelease(mouseX, mouseY, button);
        }
    }
}
