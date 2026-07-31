package xd.harm.modules.impl.player.autobuy;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.inventory.ChestScreen;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.inventory.container.ChestContainer;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import xd.harm.utils.client.ClientUtility;
import xd.harm.utils.client.IMinecraft;
import xd.harm.utils.render.color.ColorUtils;
import xd.harm.utils.render.rect.RenderUtility;

import java.lang.reflect.Field;

public class CheapestItemHighlighter implements IMinecraft {

    private boolean enabled = false;
    private int cheapestSlot = -1;
    private int cheapestPrice = Integer.MAX_VALUE;

    public void enable() {
        enabled = true;
    }

    public void disable() {
        enabled = false;
        cheapestSlot = -1;
        cheapestPrice = Integer.MAX_VALUE;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void update() {
        if (!enabled) return;
        if (!ClientUtility.isConnectedToServer("spookytime")) return;
        if (!(mc.currentScreen instanceof ChestScreen)) {
            cheapestSlot = -1;
            cheapestPrice = Integer.MAX_VALUE;
            return;
        }

        ChestScreen screen = (ChestScreen) mc.currentScreen;
        String title = screen.getTitle().getString();

        if (!title.contains("Аукцион") && !title.contains("Поиск:")) {
            cheapestSlot = -1;
            cheapestPrice = Integer.MAX_VALUE;
            return;
        }

        if (!(mc.player.openContainer instanceof ChestContainer)) return;
        ChestContainer container = (ChestContainer) mc.player.openContainer;

        cheapestSlot = -1;
        cheapestPrice = Integer.MAX_VALUE;

        for (Slot slot : container.inventorySlots) {
            if (slot.slotNumber > 53) continue;
            if (slot.slotNumber == 49) continue;
            if (!slot.getHasStack()) continue;
            if (slot.getStack().isEmpty()) continue;

            ItemStack stack = slot.getStack();
            int price = AutoBuyUtil.getPrice(stack);

            if (price > 0) {
                int itemCount = stack.getCount();
                int pricePerItem = price / itemCount;

                if (pricePerItem < cheapestPrice) {
                    cheapestPrice = pricePerItem;
                    cheapestSlot = slot.slotNumber;
                }
            }
        }
    }

    public void render(MatrixStack matrixStack) {
        if (!enabled) return;
        if (cheapestSlot == -1) return;
        if (!(mc.currentScreen instanceof ChestScreen)) return;

        if (!(mc.player.openContainer instanceof ChestContainer)) return;
        ChestContainer container = (ChestContainer) mc.player.openContainer;

        int guiLeft = getGuiLeft(mc.currentScreen);
        int guiTop = getGuiTop(mc.currentScreen);

        for (Slot slot : container.inventorySlots) {
            if (slot.slotNumber == cheapestSlot) {
                int x = guiLeft + slot.xPos;
                int y = guiTop + slot.yPos;
                RenderUtility.drawRoundedRectOutline(x, y, 16, 16, 2, 1f, ColorUtils.rgba(0, 255, 0, 180));
                break;
            }
        }
    }

    private int getGuiLeft(Object screen) {
        try {
            Field f = ContainerScreen.class.getDeclaredField("guiLeft");
            f.setAccessible(true);
            return f.getInt(screen);
        } catch (Exception ignored) {
        }
        try {
            Field f = ContainerScreen.class.getDeclaredField("field_147003_i");
            f.setAccessible(true);
            return f.getInt(screen);
        } catch (Exception ignored) {
        }
        return 0;
    }

    private int getGuiTop(Object screen) {
        try {
            Field f = ContainerScreen.class.getDeclaredField("guiTop");
            f.setAccessible(true);
            return f.getInt(screen);
        } catch (Exception ignored) {
        }
        try {
            Field f = ContainerScreen.class.getDeclaredField("field_147009_r");
            f.setAccessible(true);
            return f.getInt(screen);
        } catch (Exception ignored) {
        }
        return 0;
    }
}
