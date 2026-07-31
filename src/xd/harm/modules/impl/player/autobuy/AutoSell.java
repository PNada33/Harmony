package xd.harm.modules.impl.player.autobuy;

import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import xd.harm.modules.impl.player.AutoBuy;
import xd.harm.utils.client.IMinecraft;
import xd.harm.utils.client.InvUtil;

public class AutoSell implements IMinecraft {
    private final AutoBuy module;
    private final AutoBuyManager manager;
    private final Object buyedItemsLock;
    private final java.util.List<BuyedItem> buyedItems;

    private boolean autoSellBusy = false;
    private BuyedItem currentSellItem = null;
    private long lastSellAction = 0;
    private boolean waitingForStorage = false;
    private long lastStorageCheck = 0;
    private long lastSellPrice = 0;

    public AutoSell(AutoBuy module, AutoBuyManager manager, java.util.List<BuyedItem> buyedItems, Object buyedItemsLock) {
        this.module = module;
        this.manager = manager;
        this.buyedItems = buyedItems;
        this.buyedItemsLock = buyedItemsLock;
    }

    public void onUpdate() {
        if (!module.getAutoSell().get()) return;

        if (waitingForStorage) {
            if (System.currentTimeMillis() > lastSellAction && AutoBuyUtil.isAuctionOpened()) {
                InvUtil.clickSlotId(46, 0, ClickType.PICKUP, false);
                lastSellAction = System.currentTimeMillis() + 50;
            } else if (System.currentTimeMillis() > lastSellAction && mc.player.openContainer != null && mc.player.openContainer.getSlot(0).getHasStack()) {
                InvUtil.clickSlotId(0, 0, ClickType.PICKUP, false);
                lastSellAction = System.currentTimeMillis() + 50;
            } else if (System.currentTimeMillis() > lastSellAction && mc.player.openContainer != null && !mc.player.openContainer.getSlot(0).getHasStack()) {
                waitingForStorage = false;
                mc.player.closeScreen();
                mc.player.sendChatMessage("/ah");
                lastSellAction = System.currentTimeMillis() + 200;
            }
            return;
        }

        if (autoSellBusy) return;

        BuyedItem toSell = null;
        synchronized (buyedItemsLock) {
            for (BuyedItem item : buyedItems) {
                if (item.buyed && !item.sold) {
                    toSell = item;
                    break;
                }
            }
        }
        if (toSell == null) return;

        int slot = getItemInHotBar(toSell.ahItem.getItem());
        if (slot == -1) return;
        if (mc.player.inventory.currentItem != slot) {
            mc.player.inventory.currentItem = slot;
            mc.player.connection.sendPacket(new CHeldItemChangePacket(slot));
            return;
        }

        int price = (int) (toSell.price * (1 + module.getAutoSellPercent().get() / 100.0));
        if (toSell.abItem != null && toSell.abItem.sellPrice > 0) {
            price = (int) toSell.abItem.sellPrice;
        }
        lastSellPrice = price;
        mc.player.sendChatMessage("/ah sell " + price);
        autoSellBusy = true;
        currentSellItem = toSell;
        lastSellAction = System.currentTimeMillis() + 500;
    }

    public void onChatMessage(String text) {
        if (!module.getAutoSell().get()) return;
        if (text.contains("Освободите хранилище") || text.contains("Освободите хранилище или уберите предметы с продажи")) {
            waitingForStorage = true;
            lastStorageCheck = System.currentTimeMillis();
            mc.player.sendChatMessage("/ah");
            lastSellAction = System.currentTimeMillis() + 100;
        }
        if (text.contains("успешно выставили предмет на аукцион") || text.contains("Выставлен лот")) {
            if (currentSellItem != null) {
                currentSellItem.sold = true;
                autoSellBusy = false;
                currentSellItem = null;
            }
        }
    }

    public void reset() {
        autoSellBusy = false;
        currentSellItem = null;
        waitingForStorage = false;
        lastSellAction = 0;
        lastStorageCheck = 0;
        lastSellPrice = 0;
    }

    private int getItemInHotBar(Item item) {
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                return i;
            }
        }
        return -1;
    }
}
