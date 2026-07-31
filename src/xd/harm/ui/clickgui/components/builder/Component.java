package xd.harm.ui.clickgui.components.builder;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Component implements IBuilder {

    private float x, y, width, height;
    private boolean focused = false;
    private boolean sticky = false;

    public boolean isHovered(float mouseX, float mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public boolean isHovered(float mouseX, float mouseY, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public boolean isMouseOverComponent(float mouseX, float mouseY) {
        return isHovered(mouseX, mouseY);
    }

    public boolean isVisible() {
        return true;
    }
}
