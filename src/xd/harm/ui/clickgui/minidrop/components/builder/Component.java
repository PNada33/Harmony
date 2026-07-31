package xd.harm.ui.clickgui.minidrop.components.builder;

import com.mojang.blaze3d.matrix.MatrixStack;
import xd.harm.ui.clickgui.minidrop.Panel;

public class Component implements IBuilder {

    private float x, y, width, height;
    private Panel panel;

    public float getX() { return x; }
    public void setX(float x) { this.x = x; }
    public float getY() { return y; }
    public void setY(float y) { this.y = y; }
    public float getWidth() { return width; }
    public void setWidth(float width) { this.width = width; }
    public float getHeight() { return height; }
    public void setHeight(float height) { this.height = height; }
    public Panel getPanel() { return panel; }
    public void setPanel(Panel panel) { this.panel = panel; }

    public boolean isHovered(float mouseX, float mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public boolean isHovered(float mouseX, float mouseY, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public boolean isVisible() {
        return true;
    }
}
