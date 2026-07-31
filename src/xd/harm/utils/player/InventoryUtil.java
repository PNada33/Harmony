package xd.harm.utils.player;

import net.minecraft.block.Block;
import net.minecraft.block.SlabBlock;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.AxeItem;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.network.play.client.CCloseWindowPacket;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.network.play.client.CPlayerTryUseItemPacket;
import net.minecraft.network.play.server.SHeldItemChangePacket;
import xd.harm.events.input.EventInput;
import xd.harm.events.network.EventPacket;
import xd.harm.utils.client.IMinecraft;

public class InventoryUtil implements IMinecraft {
    private static InventoryUtil instance = new InventoryUtil();
    private static final long OFFHAND_SWAP_STOP_MS = 260L;
    private static boolean offhandSwapInProgress;
    private static long offhandSwapStopUntil;

    public static int findEmptySlot(boolean inHotBar) {
        int start = inHotBar ? 0 : 9;
        int end = inHotBar ? 9 : 45;
        for (int i = start; i < end; ++i) {
            if (!InventoryUtil.mc.player.inventory.getStackInSlot(i).isEmpty()) continue;
            return i;
        }
        return -1;
    }

    public static int findBlockInHotbar() {
        for (int i = 0; i < 9; ++i) {
            ItemStack stack = InventoryUtil.mc.player.inventory.getStackInSlot(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) continue;
            return i;
        }
        return -1;
    }

    public static int getHotbarSlotOfItem() {
        for (ItemStack stack : InventoryUtil.mc.player.getArmorInventoryList()) {
            if (stack.getItem() != Items.ELYTRA) continue;
            return -2;
        }
        int slot = -1;
        for (int i = 0; i < 36; ++i) {
            ItemStack s = InventoryUtil.mc.player.inventory.getStackInSlot(i);
            if (s.getItem() != Items.ELYTRA) continue;
            slot = i;
            break;
        }
        if (slot < 9 && slot != -1) {
            slot += 36;
        }
        return slot;
    }

    public static int getSlotIDFromItem(Item item) {
        for (ItemStack stack : InventoryUtil.mc.player.getArmorInventoryList()) {
            if (stack.getItem() != item) continue;
            return -2;
        }
        int slot = -1;
        for (int i = 0; i < 36; ++i) {
            ItemStack s = InventoryUtil.mc.player.inventory.getStackInSlot(i);
            if (s.getItem() != item) continue;
            slot = i;
            break;
        }
        if (slot < 9 && slot != -1) {
            slot += 36;
        }
        return slot;
    }

    public static void moveItem(int from, int to, boolean air) {
        if (from == to) return;
        
        from = from < 9 ? from + 36 : from;
        
        if (to == 45) {
            swapToOffhand(from);
        } else {
            InventoryUtil.pickupItem(from, 0);
            InventoryUtil.pickupItem(to, 0);
            if (air) {
                InventoryUtil.pickupItem(from, 0);
            }
        }
    }

    public static void moveItemTime(int from, int to, boolean air, int time) {
        if (from == to) return;
        
        from = from < 9 ? from + 36 : from;
        
        if (to == 45) {
            swapToOffhand(from);
        } else {
            InventoryUtil.pickupItem(from, 0, time);
            InventoryUtil.pickupItem(to, 0, time);
            if (air) {
                InventoryUtil.pickupItem(from, 0, time);
            }
        }
    }

    public static void moveItem(int from, int to) {
        if (from == to || from == -1) return;
        
        from = from < 9 ? from + 36 : from;
        
        if (to == 45) {
            swapToOffhand(from);
        } else {
            InventoryUtil.pickupItem(from, 0);
            InventoryUtil.pickupItem(to, 0);
            InventoryUtil.pickupItem(from, 0);
        }
    }

    public static void swapToOffhand(int slot) {
        if (mc.player == null || mc.playerController == null) {
            return;
        }

        int containerSlot = slot < 9 ? slot + 36 : slot;
        if (containerSlot < 0 || containerSlot > 45 || containerSlot == 45) {
            return;
        }

        startOffhandSwapStop();
        mc.playerController.windowClick(0, containerSlot, 40, ClickType.SWAP, mc.player);
        mc.player.connection.sendPacket(new CCloseWindowPacket(0));
        startOffhandSwapStop();
    }

    public static void startOffhandSwapStop() {
        offhandSwapInProgress = true;
        offhandSwapStopUntil = System.currentTimeMillis() + OFFHAND_SWAP_STOP_MS;
    }

    public static boolean isOffhandSwapInProgress() {
        if (!offhandSwapInProgress) {
            return false;
        }
        if (System.currentTimeMillis() > offhandSwapStopUntil) {
            offhandSwapInProgress = false;
            return false;
        }
        return true;
    }

    public static void handleOffhandSwapStop(EventInput event) {
        if (!isOffhandSwapInProgress()) {
            return;
        }

        event.setForward(0.0F);
        event.setStrafe(0.0F);
        event.setSprintState(false);
        if (mc.player != null) {
            mc.player.setSprinting(false);
        }
    }

    public static void pickupItem(int slot, int button) {
        InventoryUtil.mc.playerController.windowClick(0, slot, button, ClickType.PICKUP, InventoryUtil.mc.player);
    }

    public static void pickupItem(int slot, int button, int time) {
        InventoryUtil.mc.playerController.windowClickFixed(0, slot, button, ClickType.PICKUP, InventoryUtil.mc.player, time);
    }

    public static void windowClick(int windowId, int slot, int button, ClickType type) {
        InventoryUtil.mc.playerController.windowClick(windowId, slot, button, type, InventoryUtil.mc.player);
    }

    public static void shiftClick(int windowId, int slot) {
        InventoryUtil.mc.playerController.windowClick(windowId, slot, 0, ClickType.QUICK_MOVE, InventoryUtil.mc.player);
    }

    public static void clickSlot(int windowId, int slot, int button) {
        InventoryUtil.mc.playerController.windowClick(windowId, slot, button, ClickType.PICKUP, InventoryUtil.mc.player);
    }

    public static void putSingleItem(int windowId, int fromSlot, int toSlot) {
        mc.playerController.windowClick(windowId, fromSlot, 0, ClickType.PICKUP, mc.player);
        mc.playerController.windowClick(windowId, toSlot, 1, ClickType.PICKUP, mc.player);
        mc.playerController.windowClick(windowId, fromSlot, 0, ClickType.PICKUP, mc.player);
    }

    public int getAxeInInventory(boolean inHotBar) {
        int firstSlot = inHotBar ? 0 : 9;
        int lastSlot = inHotBar ? 9 : 36;
        for (int i = firstSlot; i < lastSlot; ++i) {
            if (!(InventoryUtil.mc.player.inventory.getStackInSlot(i).getItem() instanceof AxeItem)) continue;
            return i;
        }
        return -1;
    }

    public int getItemSlotNew(Item input) {
        for (int i = 0; i < 36; ++i) {
            ItemStack s = mc.player.inventory.getStackInSlot(i);
            if (s.getItem() == input) {
                return i;
            }
        }
        return -1;
    }

    public int findBestSlotInHotBar() {
        int emptySlot = this.findEmptySlot();
        return emptySlot != -1 ? emptySlot : this.findNonSwordSlot();
    }

    private int findEmptySlot() {
        for (int i = 0; i < 9; ++i) {
            if (!InventoryUtil.mc.player.inventory.getStackInSlot(i).isEmpty() || InventoryUtil.mc.player.inventory.currentItem == i) continue;
            return i;
        }
        return -1;
    }

    private int findNonSwordSlot() {
        for (int i = 0; i < 9; ++i) {
            if (InventoryUtil.mc.player.inventory.getStackInSlot(i).getItem() instanceof SwordItem || InventoryUtil.mc.player.inventory.getStackInSlot(i).getItem() instanceof ElytraItem || InventoryUtil.mc.player.inventory.currentItem == i) continue;
            return i;
        }
        return -1;
    }

    public int getSlotInInventory(Item item) {
        int finalSlot = -1;
        for (int i = 0; i < 36; ++i) {
            if (InventoryUtil.mc.player.inventory.getStackInSlot(i).getItem() != item) continue;
            finalSlot = i;
        }
        return finalSlot;
    }

    public int getSlotInInventoryOrHotbar(Item item, boolean inHotBar) {
        int firstSlot = inHotBar ? 0 : 9;
        int lastSlot = inHotBar ? 9 : 36;
        int finalSlot = -1;
        for (int i = firstSlot; i < lastSlot; ++i) {
            if (InventoryUtil.mc.player.inventory.getStackInSlot(i).getItem() != item) continue;
            finalSlot = i;
        }
        return finalSlot;
    }

    public static int getSlotInInventoryOrHotbar() {
        int firstSlot = 0;
        int lastSlot = 9;
        int finalSlot = -1;
        for (int i = firstSlot; i < lastSlot; ++i) {
            if (!(Block.getBlockFromItem(InventoryUtil.mc.player.inventory.getStackInSlot(i).getItem()) instanceof SlabBlock)) continue;
            finalSlot = i;
        }
        return finalSlot;
    }

    public static boolean doesHotbarHaveItem(Item item) {
        for (int i = 0; i < 9; ++i) {
            InventoryUtil.mc.player.inventory.getStackInSlot(i);
            if (InventoryUtil.mc.player.inventory.getStackInSlot(i).getItem() != item) continue;
            return true;
        }
        return false;
    }

    public static int getItemIndex(Item item) {
        for (int i = 0; i < 36; ++i) {
            ItemStack stack = InventoryUtil.mc.player.inventory.getStackInSlot(i);
            if (stack.getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    public static void inventorySwapClick(Item item, boolean rotation) {
        if (getItemIndex(item) != -1) {
            int i;
            if (InventoryUtil.doesHotbarHaveItem(item)) {
                for (i = 0; i < 9; ++i) {
                    if (InventoryUtil.mc.player.inventory.getStackInSlot(i).getItem() != item) continue;
                    if (i != InventoryUtil.mc.player.inventory.currentItem) {
                        InventoryUtil.mc.player.connection.sendPacket(new CHeldItemChangePacket(i));
                    }
                    InventoryUtil.mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(net.minecraft.util.Hand.MAIN_HAND));
                    if (i == InventoryUtil.mc.player.inventory.currentItem) break;
                    InventoryUtil.mc.player.connection.sendPacket(new CHeldItemChangePacket(InventoryUtil.mc.player.inventory.currentItem));
                    break;
                }
            }
            if (!InventoryUtil.doesHotbarHaveItem(item)) {
                for (i = 0; i < 36; ++i) {
                    if (InventoryUtil.mc.player.inventory.getStackInSlot(i).getItem() != item) continue;
                    InventoryUtil.mc.playerController.windowClick(0, i, InventoryUtil.mc.player.inventory.currentItem % 8 + 1, ClickType.SWAP, InventoryUtil.mc.player);
                    InventoryUtil.mc.player.connection.sendPacket(new CHeldItemChangePacket(InventoryUtil.mc.player.inventory.currentItem % 8 + 1));
                    InventoryUtil.mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(net.minecraft.util.Hand.MAIN_HAND));
                    InventoryUtil.mc.player.connection.sendPacket(new CHeldItemChangePacket(InventoryUtil.mc.player.inventory.currentItem));
                    InventoryUtil.mc.playerController.windowClick(0, i, InventoryUtil.mc.player.inventory.currentItem % 8 + 1, ClickType.SWAP, InventoryUtil.mc.player);
                    break;
                }
            }
        }
    }

    public static int getItemSlot(Item input) {
        for (ItemStack stack : InventoryUtil.mc.player.getArmorInventoryList()) {
            if (stack.getItem() != input) continue;
            return -2;
        }
        int slot = -1;
        for (int i = 0; i < 36; ++i) {
            ItemStack s = InventoryUtil.mc.player.inventory.getStackInSlot(i);
            if (s.getItem() != input) continue;
            slot = i;
            break;
        }
        if (slot < 9 && slot != -1) {
            slot += 36;
        }
        return slot;
    }

    public static InventoryUtil getInstance() {
        return instance;
    }

    public static int getHotBarSlot(Item var0) {
        for (int var1 = 0; var1 < 9; ++var1) {
            if (InventoryUtil.mc.player.inventory.getStackInSlot(var1).getItem() != var0) continue;
            return var1;
        }
        return -1;
    }

    public static class Hand {
        public static boolean isEnabled;
        private boolean isChangingItem;
        private int originalSlot = -1;

        public void onEventPacket(EventPacket eventPacket) {
            if (eventPacket.isReceive() && eventPacket.getPacket() instanceof SHeldItemChangePacket) {
                this.isChangingItem = true;
            }
        }

        public void handleItemChange(boolean resetItem) {
            if (this.isChangingItem && this.originalSlot != -1) {
                isEnabled = true;
                IMinecraft.mc.player.inventory.currentItem = this.originalSlot;
                if (resetItem) {
                    this.isChangingItem = false;
                    this.originalSlot = -1;
                    isEnabled = false;
                }
            }
        }

        public void setOriginalSlot(int slot) {
            this.originalSlot = slot;
        }
    }
}
