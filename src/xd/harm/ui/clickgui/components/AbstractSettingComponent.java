package xd.harm.ui.clickgui.components;

import com.mojang.blaze3d.matrix.MatrixStack;
import xd.harm.modules.settings.Setting;
import xd.harm.utils.client.IMinecraft;

import java.awt.Color;

public abstract class AbstractSettingComponent implements IMinecraft {
    protected final Setting<?> setting;
    protected float x, y, width, height;
    protected float alphaMultiplier = 1f;

    public AbstractSettingComponent(Setting<?> setting) {
        this.setting = setting;
        this.height = 16;
    }

    public abstract void render(MatrixStack context, int mouseX, int mouseY, float delta);

    public abstract void tick();

    public abstract boolean isHover(double mouseX, double mouseY);

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    public boolean charTyped(char chr, int modifiers) {
        return false;
    }

    public void position(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void size(float width, float height) {
        this.width = width;
        this.height = height;
    }

    public void setAlphaMultiplier(float alpha) {
        this.alphaMultiplier = alpha;
    }

    public Setting<?> getSetting() {
        return setting;
    }

    public float getHeight() {
        return height;
    }

    protected Color applyAlpha(Color color) {
        int newAlpha = Math.max(0, Math.min(255, (int) (color.getAlpha() * alphaMultiplier)));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), newAlpha);
    }

    protected int applyAlpha(int color) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int newAlpha = Math.max(0, Math.min(255, (int) (a * alphaMultiplier)));
        return (newAlpha << 24) | (r << 16) | (g << 8) | b;
    }

    protected float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    protected int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
