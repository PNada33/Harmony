package xd.harm.utils.client;

import lombok.experimental.UtilityClass;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CClickWindowPacket;

import java.util.List;
import java.util.stream.IntStream;

import static xd.harm.utils.client.IMinecraft.mc;

@UtilityClass
public class InvUtil {

    public Slot getInventorySlot(Item item) {
        return mc.player.openContainer.inventorySlots.stream().filter(s -> s.getStack().getItem().equals(item) && s.slotNumber >= mc.player.openContainer.inventorySlots.size() - 36).findFirst().orElse(null);
    }

    public Slot getInventorySlot(List<Item> item) {
        return mc.player.openContainer.inventorySlots.stream().filter(s -> item.contains(s.getStack().getItem()) && s.slotNumber >= mc.player.openContainer.inventorySlots.size() - 36).findFirst().orElse(null);
    }

    public int getInventoryCount(Item item) {
        return IntStream.range(0, 45).filter(i -> mc.player.inventory.getStackInSlot(i).getItem().equals(item)).map(i -> mc.player.inventory.getStackInSlot(i).getCount()).sum();
    }

    public void clickSlot(Slot slot, int button, ClickType clickType, boolean packet) {
        if (slot != null) clickSlotId(slot.slotNumber, button, clickType, packet);
    }
    public int findEmptySlot(boolean hotbar) {
        int start = hotbar ? 0 : 9;
        int end = hotbar ? 9 : 36;

        for (int i = start; i < end; i++) {
            if (mc.player.inventory.getStackInSlot(i).isEmpty()) {
                return i + (hotbar ? 36 : 9);
            }
        }
        return -1;
    }

    public void clickSlotId(int slot, int button, ClickType clickType, boolean packet) {
        if (slot == -1) return;

        if (packet) {
            mc.player.connection.sendPacket(new CClickWindowPacket(mc.player.openContainer.windowId, slot, button, clickType, ItemStack.EMPTY, mc.player.openContainer.getNextTransactionID(mc.player.inventory)));
        } else {
            mc.playerController.windowClick(mc.player.openContainer.windowId, slot, button, clickType, mc.player);
        }
    }

}