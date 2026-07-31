package xd.harm.modules.impl.player.autobuy;

import net.minecraft.item.ItemStack;
import java.time.LocalDateTime;

public class PurchasedItem {

    public final ItemStack stack;
    public final LocalDateTime purchaseTime;
    public final long price;
    public final boolean success;

    public PurchasedItem(ItemStack stack, LocalDateTime time, long price, boolean success) {
        this.stack = stack;
        this.purchaseTime = time;
        this.price = price;
        this.success = success;
    }
}