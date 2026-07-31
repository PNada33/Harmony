package xd.harm.ui.clickgui.components.settings;

import com.mojang.blaze3d.matrix.MatrixStack;
import xd.harm.ui.clickgui.components.settings.MultiBoxComponent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class MultiBoxOverlayManager {
    private static final List<MultiBoxComponent> opened = new ArrayList<>();

    public static void show(MultiBoxComponent box) {
        opened.remove(box);
        opened.add(box);
    }

    public static void hide(MultiBoxComponent box) {
        opened.remove(box);
    }

    public static boolean hasOpen() {
        return !opened.isEmpty();
    }

    public static void bringToFront(MultiBoxComponent box) {
        if (opened.remove(box)) opened.add(box);
    }

    public static void renderAll(MatrixStack stack, float mouseX, float mouseY) {
        Iterator<MultiBoxComponent> iterator = opened.iterator();
        while (iterator.hasNext()) {
            MultiBoxComponent box = iterator.next();
            if (!box.renderFloatingWindows(stack, mouseX, mouseY)) {
                iterator.remove();
            }
        }
    }

    public static boolean mouseClick(float mouseX, float mouseY, int button) {
        if (opened.isEmpty()) return false;

        MultiBoxComponent topOpen = null;
        for (int i = opened.size() - 1; i >= 0; i--) {
            MultiBoxComponent box = opened.get(i);
            if (!box.isDropdownOpen()) continue;
            if (topOpen == null) {
                topOpen = box;
            }
            if (box.isMouseOverDropdown(mouseX, mouseY)) {
                bringToFront(box);
                box.mouseClick(mouseX, mouseY, button);
                return true;
            }
        }

        if (topOpen == null) {
            return false;
        }

        bringToFront(topOpen);
        topOpen.mouseClick(mouseX, mouseY, button);
        return true;
    }

    public static boolean mouseRelease(float mouseX, float mouseY, int button) {
        if (opened.isEmpty()) return false;

        MultiBoxComponent topOpen = null;
        for (int i = opened.size() - 1; i >= 0; i--) {
            MultiBoxComponent box = opened.get(i);
            if (box.isDropdownOpen()) {
                topOpen = box;
                break;
            }
        }
        if (topOpen == null) {
            return false;
        }

        topOpen.mouseRelease(mouseX, mouseY, button);
        return true;
    }
}
