package xd.harm.modules.impl.player.autobuy;

import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.inventory.container.ChestContainer;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.text.TextFormatting;
import xd.harm.modules.impl.player.AutoBuy;
import xd.harm.utils.client.IMinecraft;
import xd.harm.utils.client.InvUtil;
import xd.harm.utils.client.TimerUtility;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

public class AutoBuySystem implements IMinecraft {
    public int curping = 0;
    private final AutoBuy module;
    private final AutoBuyManager manager;
    private final AutoBuyItems items;

    public final List<BuyedItem> rlyBuyedItems = new ArrayList<>();
    private final Object buyedItemsLock = new Object();
    public BuyedItem purchasingItem = null;
    private int lastAttemptedPrice = -1;
    private String lastAttemptedItemName = "";

    public final TimerUtility updater = new TimerUtility();
    public final TimerUtility sleep = new TimerUtility();
    public final TimerUtility buy = new TimerUtility();
    public final TimerUtility ping = new TimerUtility();

    private boolean enabled = false;
    public final AutoPriceParser priceParser;
    public final CheapestItemHighlighter cheapestHighlighter = new CheapestItemHighlighter();
    public final AFKHandler afkHandler = new AFKHandler();
    public final AutoSell autoSell;
    private boolean lastParserEnabled = false;
    private static final long AH_RECOVER_COOLDOWN = 1500;
    private static final long MIN_UPDATE_DELAY = 650;
    private static final long AH_UPDATE_CLICK_COOLDOWN = 650;
    private static final int STUCK_UPDATE_REOPEN_ATTEMPTS = 3;
    private long lastAhRecoverCommand = 0;
    private long lastAhUpdateClick = 0;
    private int lastAuctionFingerprint = 0;
    private boolean waitingForAuctionUpdate = false;
    private int stuckUpdateAttempts = 0;

    public AutoBuySystem(AutoBuy module, AutoBuyManager manager) {
        this.module = module;
        this.manager = manager;
        this.items = manager.getItemsModel();
        this.priceParser = new AutoPriceParser(module, manager);
        this.autoSell = new AutoSell(module, manager, rlyBuyedItems, buyedItemsLock);
    }

    public void enable() {
        if (!this.enabled) {
            this.enabled = true;
        }
    }

    public void disable() {
        if (this.enabled) {
            this.enabled = false;
        }
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public List<BuyedItem> getBuyedItems() {
        synchronized (this.buyedItemsLock) {
            return new ArrayList<>(this.rlyBuyedItems);
        }
    }

    private void addBuyedItem(BuyedItem item) {
        if (item != null) {
            synchronized (this.buyedItemsLock) {
                if (!this.rlyBuyedItems.isEmpty() && this.rlyBuyedItems.get(0).equals(item)) {
                } else {
                    item.buyed = false;
                    this.rlyBuyedItems.add(0, item);
                }
            }
        }
    }


    public void onUpdate() {
        if (this.enabled) {
            afkHandler.tick();
            if (afkHandler.isHandling()) {
                return;
            }

            if (priceParser.isParsing()) {
                return;
            }

            long updateDelayMs = (long) (float) module.getUpdateDelay().get();
            long delay = Math.max(MIN_UPDATE_DELAY, updateDelayMs);
            if (this.updater.hasTimeElapsed(delay)) {
                this.pushUpdatePage();
                this.updater.reset();
            }

            autoSell.onUpdate();
        }
    }

    public boolean onChatMessage(String text) {
        if (!this.enabled) {
            return false;
        } else {
            tryLogAuctionSale(text);

            if (text.contains("Недопустимо") && text.contains("AFK")) {
                print("[AutoBuy] Обнаружен AFK режим, выполняю анти-AFK действия...");
                afkHandler.handleAFKDetection();
                return true;
            }

            autoSell.onChatMessage(text);
            AutoBuyUtil.MessageType type = AutoBuyUtil.getMessageType(text);
            if (type == AutoBuyUtil.MessageType.No) {
                if (text.startsWith("Ваш пинг: ")) {
                    try {
                        this.curping = Integer.parseInt(text.replace("Ваш пинг: ", ""));
                    } catch (Exception ignored) {
                    }

                    return true;
                } else {
                    return false;
                }
            } else if (type == AutoBuyUtil.MessageType.Buy) {
                String info = AutoBuyUtil.extractBuyInformation(text);
                if (info.isEmpty()) {
                    return false;
                } else {
                    int price = Integer.parseInt(info.split("[|]")[0].replace(",", ""));
                    String item = info.split("[|]")[1];
                    if (price == this.lastAttemptedPrice && !this.rlyBuyedItems.isEmpty()) {
                        synchronized (this.buyedItemsLock) {
                            BuyedItem lastItem = this.rlyBuyedItems.get(0);
                            lastItem.buyed = true;
                        }
                        if (purchasingItem != null && purchasingItem.ahItem != null) {
                            String server = mc.getCurrentServerData() != null ? mc.getCurrentServerData().serverIP : "Unknown";
                            String account = mc.getSession().getUsername();
                            manager.addTransactionLog(new TransactionLog(
                                    TransactionLog.Type.BUY,
                                    purchasingItem.ahItem.copy(),
                                    purchasingItem.ahItem.getDisplayName().getString(),
                                    purchasingItem.count,
                                    price,
                                    LocalDateTime.now(),
                                    server,
                                    account
                            ));
                        }
                    }

                    String message = "Куплен предмет \"" + TextFormatting.GREEN + item + TextFormatting.WHITE + "\" за " + TextFormatting.YELLOW + price + " монет" + TextFormatting.WHITE + ".";
                    print(message);
                    return true;
                }
            } else {
                if (type == AutoBuyUtil.MessageType.Wait) {
                    this.sleep.reset();
                    this.updater.reset();
                    this.lastAhUpdateClick = System.currentTimeMillis();
                } else {
                    if (type == AutoBuyUtil.MessageType.NoMoney) {
                        print("Не хватило денег на покупку товара.");
                        return true;
                    }

                    if (type == AutoBuyUtil.MessageType.Purchased) {
                        print("К сожалению, товар уже куплен.");
                        return true;
                    }
                }

                return false;
            }
        }
    }

    public void processBuy() {
        if (this.enabled) {
            if (afkHandler.isHandling()) {
                return;
            }
            if (priceParser.isParsing()) {
                return;
            }

            if (AutoBuyUtil.isAuctionOpened()) {
                long buyDelayMs = (long) (float) module.getBuyDelay().get();
                boolean canBuy = this.sleep.hasTimeElapsed(buyDelayMs);
                if (canBuy) {
                    ChestContainer ah = (ChestContainer) mc.player.openContainer;
                    boolean scaryPrice = mc.currentScreen.getTitle().getString().toLowerCase().contains("подозрительная цена") ||
                            mc.currentScreen.getTitle().getString().toLowerCase().contains("подтверждение покупки");
                    if (scaryPrice) {
                        print("Подтверждение покупки...");
                        InvUtil.clickSlotId(0, 0, ClickType.QUICK_MOVE, false);
                        this.sleep.reset();
                        this.buy.reset();
                        return;
                    } else {
                        for (Slot slot : ah.inventorySlots) {
                            if (slot.slotNumber <= 44 && slot.getHasStack() && !slot.getStack().isEmpty()) {
                                int price = AutoBuyUtil.getPrice(slot.getStack());
                                if (price != -1) {
                                    int finalPrice = price / slot.getStack().getCount();
                                    AutoBuyItem itemToBuy = null;
                                    HashMap<Attribute, Map.Entry<Float, AttributeModifier.Operation>> attributes = AutoBuyUtil.getAttributes(slot.getStack());
                                    if (attributes != null && !attributes.isEmpty()) {
                                        itemToBuy = items.isNeedToBuy(slot.getStack(), attributes);
                                        if (itemToBuy == null) {
                                            continue;
                                        }
                                    } else {
                                        if (slot.getStack().getItem() == Items.ENCHANTED_BOOK) {
                                            itemToBuy = items.isNeedToBuyEnchanted(slot.getStack());
                                        } else {
                                            itemToBuy = items.isNeedToBuy(slot.getStack());
                                        }

                                        if (itemToBuy == null && slot.getStack().isEnchanted()) {
                                            itemToBuy = items.isNeedToBuyEnchanted(slot.getStack());
                                        }

                                        if (itemToBuy == null) {
                                            String itemType = AutoBuyUtil.getSpookyItemType(slot.getStack());
                                            if (itemType != null) {
                                                itemToBuy = items.isNeedToBuy(slot.getStack(), itemType);
                                            }
                                        }

                                        if (itemToBuy == null) {
                                            itemToBuy = items.isNeedToBuyPotion(slot.getStack());
                                        }
                                    }

                                    if (itemToBuy != null) {
                                        if (itemToBuy.buyPrice <= 0) {
                                            continue;
                                        }
                                        int limitPrice = (int) itemToBuy.buyPrice;
                                        if (finalPrice <= Math.max(10, limitPrice)) {
                                            this.lastAttemptedPrice = price;
                                            this.lastAttemptedItemName = slot.getStack().getDisplayName().getString();
                                            BuyedItem buyedItem = new BuyedItem(slot.getStack(), slot.getStack().copy(), price, slot.getStack().getCount(), itemToBuy, false, LocalDateTime.now());
                                            this.addBuyedItem(buyedItem);
                                            this.purchasingItem = buyedItem;
                                            InvUtil.clickSlotId(slot.slotNumber, 0, ClickType.QUICK_MOVE, false);
                                            this.sleep.reset();
                                            this.buy.reset();
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void pushUpdatePage() {
        if (AutoBuyUtil.isAuctionOpened()) {
            if (hasUpdateSlot()) {
                long now = System.currentTimeMillis();
                if (now - lastAhUpdateClick >= AH_UPDATE_CLICK_COOLDOWN) {
                    int currentFingerprint = getAuctionFingerprint();
                    if (waitingForAuctionUpdate) {
                        if (currentFingerprint == lastAuctionFingerprint) {
                            stuckUpdateAttempts++;
                        } else {
                            stuckUpdateAttempts = 0;
                        }
                    }
                    if (stuckUpdateAttempts >= STUCK_UPDATE_REOPEN_ATTEMPTS) {
                        waitingForAuctionUpdate = false;
                        stuckUpdateAttempts = 0;
                        reopenAuction();
                        return;
                    }
                    lastAuctionFingerprint = currentFingerprint;
                    InvUtil.clickSlotId(49, 0, ClickType.PICKUP, false);
                    lastAhUpdateClick = now;
                    waitingForAuctionUpdate = true;
                }
            } else {
                reopenAuction();
            }
        } else {
            reopenAuction();
        }
    }

    private int getAuctionFingerprint() {
        if (!(mc.player.openContainer instanceof ChestContainer)) {
            return 0;
        }
        ChestContainer container = (ChestContainer) mc.player.openContainer;
        int result = 1;
        for (Slot slot : container.inventorySlots) {
            if (slot.slotNumber > 44) continue;
            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) {
                result = 31 * result + slot.slotNumber;
                continue;
            }
            result = 31 * result + slot.slotNumber;
            result = 31 * result + Item.getIdFromItem(stack.getItem());
            result = 31 * result + stack.getCount();
            result = 31 * result + stack.getDisplayName().getString().hashCode();
            result = 31 * result + AutoBuyUtil.getPrice(stack);
        }
        return result;
    }

    private boolean hasUpdateSlot() {
        if (!(mc.player.openContainer instanceof ChestContainer)) {
            return false;
        }
        ChestContainer container = (ChestContainer) mc.player.openContainer;
        if (container.inventorySlots.size() <= 49) {
            return false;
        }
        Slot slot = container.inventorySlots.get(49);
        return slot != null && slot.getHasStack() && !slot.getStack().isEmpty();
    }

    private void reopenAuction() {
        long now = System.currentTimeMillis();
        if (mc.player != null && now - lastAhRecoverCommand >= AH_RECOVER_COOLDOWN) {
            mc.player.closeScreen();
            mc.player.sendChatMessage("/ah");
            lastAhRecoverCommand = now;
            lastAhUpdateClick = now;
            waitingForAuctionUpdate = false;
            stuckUpdateAttempts = 0;
            updater.reset();
        }
    }

    public void savePrices() {
        manager.saveConfig();
    }

    public void startPriceParsing() {
        this.priceParser.startPriceParsing();
    }

    public void stopPriceParsing() {
        this.priceParser.stopPriceParsing();
    }

    public boolean isParsingPrices() {
        return this.priceParser.isParsing();
    }

    public void checkParsingDelay() {
        this.priceParser.checkParsingDelay();
    }

    public void up() {
        boolean parserEnabled = module.getParser().get();
        if (parserEnabled && !lastParserEnabled && !priceParser.isParsing()) {
            priceParser.startPriceParsing();
        }
        lastParserEnabled = parserEnabled;
        this.priceParser.up();
    }

    public void resetState() {
        lastAttemptedPrice = -1;
        lastAttemptedItemName = "";
        purchasingItem = null;
        updater.reset();
        sleep.reset();
        ping.reset();
        buy.reset();
        autoSell.reset();
        lastParserEnabled = module.getParser().get();
        lastAhRecoverCommand = 0;
        lastAhUpdateClick = 0;
        lastAuctionFingerprint = 0;
        waitingForAuctionUpdate = false;
        stuckUpdateAttempts = 0;
    }

    private void tryLogAuctionSale(String text) {
        String clean = TextFormatting.getTextWithoutFormattingCodes(text);
        if (clean == null) clean = text;
        String lower = clean.toLowerCase(Locale.ROOT);
        if (!lower.contains("у вас купили предмет")) {
            return;
        }

        int start = lower.indexOf("у вас купили предмет");
        if (start < 0) return;

        String tail = clean.substring(start + "У вас купили предмет".length()).trim();
        String tailLower = tail.toLowerCase(Locale.ROOT);
        String itemName = "";
        int zaIdx = tailLower.lastIndexOf(" за ");
        String pricePart = tail;
        if (zaIdx >= 0) {
            itemName = tail.substring(0, zaIdx).trim();
            pricePart = tail.substring(zaIdx + 4);
        }

        String digits = pricePart.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return;

        long price;
        try {
            price = Long.parseLong(digits);
        } catch (Exception ignored) {
            return;
        }
        if (price <= 0) return;

        if (itemName.isEmpty()) {
            itemName = "Продажа с аукциона";
        }

        String server = mc.getCurrentServerData() != null ? mc.getCurrentServerData().serverIP : "Unknown";
        String account = mc.getSession().getUsername();
        manager.addTransactionLog(new TransactionLog(
                TransactionLog.Type.SELL,
                ItemStack.EMPTY,
                itemName,
                1,
                price,
                LocalDateTime.now(),
                server,
                account
        ));
    }
}
