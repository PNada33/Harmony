package xd.harm.modules.impl.player.autobuy;

import net.minecraft.item.ItemStack;

import java.time.LocalDateTime;

public class BuyedItem {
    public BuyedItem(ItemStack ahItem, ItemStack parsedItem, int price, int count, AutoBuyItem abItem, boolean buyed, LocalDateTime buyTime) {
        this.ahItem = ahItem;
        this.parsedItem = parsedItem;
        this.price = price;
        this.count = count;
        this.abItem = abItem;
        this.buyed = buyed;
        this.buyTime = buyTime;
    }

    public ItemStack ahItem;
    public ItemStack parsedItem;
    public int price;
    public int count;
    public AutoBuyItem abItem;
    public boolean buyed;
    public LocalDateTime buyTime;
    public boolean sold = false;
}
