package xd.harm.modules.impl.player.autobuy;

import net.minecraft.item.ItemStack;
import java.time.LocalDateTime;

public class TransactionLog {
    public enum Type { BUY, SELL }
    
    public final Type type;
    public final ItemStack stack;
    public final String itemName;
    public final int quantity;
    public final long price;
    public final LocalDateTime timestamp;
    public final String server;
    public final String account;
    
    public TransactionLog(Type type, ItemStack stack, String itemName, int quantity, long price, LocalDateTime timestamp, String server, String account) {
        this.type = type;
        this.stack = stack;
        this.itemName = itemName;
        this.quantity = quantity;
        this.price = price;
        this.timestamp = timestamp;
        this.server = server;
        this.account = account;
    }
}
