package xd.harm.modules.impl.misc;

import com.google.common.eventbus.Subscribe;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.ModeSetting;
import xd.harm.modules.settings.impl.SliderSetting;
import xd.harm.utils.client.InvUtil;
import xd.harm.utils.client.TimerUtility;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@ModuleRegister(name = "AutoBuyMLegacy", category = Category.Misc, desc = "Автопокупка в магазине MineLegacy (BedWars). Открой магазин ПКМ по жителю.")
public class AutoBuyMLegacy extends Module {

    // Категории магазина MineLegacy: 0=Блоки, 1=Оружие, 2=Броня, 3=Инструменты.
    private static final int CAT_BLOCKS = 0;
    private static final int CAT_WEAPONS = 1;
    private static final int CAT_ARMOR = 2;
    private static final int CAT_TOOLS = 3;

    private static final int STEP_SELECT_CATEGORY = 0;
    private static final int STEP_BUY_ITEM = 1;
    private static final int MAX_SEARCH_ATTEMPTS = 15;

    private final TimerUtility buyTimer = TimerUtility.create();

    private final SliderSetting buyDelay = new SliderSetting("BuyDelay (мс)", 200f, 50f, 2000f, 50f);
    private final ModeSetting resourceMode = new ModeSetting("Resource Mode", "30i 6g", "30i 6g", "35i 7g");
    private final BooleanSetting buySword = new BooleanSetting("Buy Sword", true);
    private final BooleanSetting buyArmor = new BooleanSetting("Buy Armor", true);
    private final BooleanSetting buyPickaxe = new BooleanSetting("Buy Pickaxe", true);
    private final BooleanSetting buyBlocks = new BooleanSetting("Buy Blocks", true);
    private final BooleanSetting onlyOneBlock = new BooleanSetting("Только 1 Блок", false)
            .setVisible(() -> buyBlocks.get());

    private final List<Purchase> purchasePlan = new ArrayList<>();

    private boolean shopSessionActive;
    private int activeWindowId = -1;
    private int purchaseIndex;
    private int step = STEP_SELECT_CATEGORY;
    private int searchAttempts;
    private boolean waitingForBlockPurchase;
    private int ironBeforeBlockPurchase;
    private int detectedBlockCost;
    private long blockPurchaseClickTime;

    public AutoBuyMLegacy() {
        addSettings(buyDelay, resourceMode, buySword, buyArmor, buyPickaxe, buyBlocks, onlyOneBlock);
    }

    @Override
    public boolean onEnable() {
        super.onEnable();
        resetShopSession();
        return false;
    }

    @Override
    public boolean onDisable() {
        resetShopSession();
        super.onDisable();
        return false;
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (!isState() || mc.player == null || mc.playerController == null) {
            return;
        }

        if (!isMineLegacyShopScreen()) {
            // Игрок сам ходит и фармит. Пока GUI магазина не открыт, модуль ничего не делает.
            resetShopSession();
            return;
        }

        Container container = mc.player.openContainer;
        if (container == null || container.windowId == 0) {
            return;
        }

        // ПКМ по жителю открыл новое окно магазина — начинаем отдельную закупочную сессию.
        if (!shopSessionActive || activeWindowId != container.windowId) {
            beginShopSession(container.windowId);
            return;
        }

        long delay = (long) (float) buyDelay.get();
        if (!buyTimer.hasTimeElapsed(delay, true)) {
            return;
        }

        skipUnavailablePurchases();
        if (purchaseIndex >= purchasePlan.size()) {
            finishShopSession();
            return;
        }

        Purchase purchase = purchasePlan.get(purchaseIndex);

        if (step == STEP_SELECT_CATEGORY) {
            // Как у BotMode: категории всегда доступны в слотах 0-6, поэтому кнопку назад нажимать не нужно.
            mc.playerController.windowClick(
                    container.windowId,
                    purchase.categorySlot,
                    0,
                    ClickType.PICKUP,
                    mc.player
            );
            step = STEP_BUY_ITEM;
            searchAttempts = 0;
            return;
        }

        if (step == STEP_BUY_ITEM) {
            if (purchase.repeatWhileIronAvailable && waitingForBlockPurchase) {
                handleBlockPurchaseResult();
                return;
            }

            boolean bought = buyItemInCategory(container, purchase);
            searchAttempts++;

            if (bought && purchase.repeatWhileIronAvailable) {
                // Не завершаем пункт после первого стака. Ждём подтверждение сервера по уменьшению железа,
                // затем снова кликаем шерсть, пока железа хватает на следующую покупку.
                waitingForBlockPurchase = true;
                ironBeforeBlockPurchase = InvUtil.getInventoryCount(Items.IRON_INGOT);
                blockPurchaseClickTime = System.currentTimeMillis();
                searchAttempts = 0;
                return;
            }

            // После покупки переходим к следующему пункту. Если товар не найден, ждём обновления GUI
            // до MAX_SEARCH_ATTEMPTS, как это делает закупка ботов.
            if (bought || searchAttempts > MAX_SEARCH_ATTEMPTS) {
                purchaseIndex++;
                step = STEP_SELECT_CATEGORY;
                searchAttempts = 0;
            }
        }
    }

    private void beginShopSession(int windowId) {
        activeWindowId = windowId;
        shopSessionActive = true;
        purchaseIndex = 0;
        step = STEP_SELECT_CATEGORY;
        searchAttempts = 0;
        waitingForBlockPurchase = false;
        ironBeforeBlockPurchase = 0;
        detectedBlockCost = 0;
        blockPurchaseClickTime = 0L;
        buildPurchasePlan();
        buyTimer.reset();
    }

    private void finishShopSession() {
        // Закрываем только GUI магазина. Сам модуль остаётся включённым и сработает при следующем ПКМ.
        if (mc.player != null) {
            mc.player.closeScreen();
        }
        resetShopSession();
    }

    private void resetShopSession() {
        shopSessionActive = false;
        activeWindowId = -1;
        purchaseIndex = 0;
        step = STEP_SELECT_CATEGORY;
        searchAttempts = 0;
        waitingForBlockPurchase = false;
        ironBeforeBlockPurchase = 0;
        detectedBlockCost = 0;
        blockPurchaseClickTime = 0L;
        purchasePlan.clear();
        buyTimer.reset();
    }

    /**
     * Повторяет таблицу AggressiveMax из BotMode.
     * 30i 6g: железные нагрудник/поножи, кольчужные шлем/ботинки, каменный меч, кирка, шерсть.
     * 35i 7g: железный меч, полный комплект кольчужной брони, кирка, шерсть.
     */
    private void buildPurchasePlan() {
        purchasePlan.clear();

        if (resourceMode.is("35i 7g")) {
            if (buySword.get()) {
                purchasePlan.add(Purchase.item(CAT_WEAPONS, "меч", "sword", "железн", "iron", Items.GOLD_INGOT, 7, true));
            }
            if (buyArmor.get()) {
                purchasePlan.add(Purchase.item(CAT_ARMOR, "шлем", "helmet", "кольчужн", "chain", Items.IRON_INGOT, 5, true));
                purchasePlan.add(Purchase.item(CAT_ARMOR, "понож", "legging", "кольчужн", "chain", Items.IRON_INGOT, 5, true));
                purchasePlan.add(Purchase.item(CAT_ARMOR, "нагрудник", "chest", "кольчужн", "chain", Items.IRON_INGOT, 5, true));
                purchasePlan.add(Purchase.item(CAT_ARMOR, "ботин", "boot", "кольчужн", "chain", Items.IRON_INGOT, 5, true));
            }
        } else {
            if (buyArmor.get()) {
                purchasePlan.add(Purchase.item(CAT_ARMOR, "нагрудник", "chest", "железн", "iron", Items.GOLD_INGOT, 3, true));
                purchasePlan.add(Purchase.item(CAT_ARMOR, "понож", "legging", "железн", "iron", Items.GOLD_INGOT, 3, true));
                purchasePlan.add(Purchase.item(CAT_ARMOR, "шлем", "helmet", "кольчужн", "chain", Items.IRON_INGOT, 5, true));
                purchasePlan.add(Purchase.item(CAT_ARMOR, "ботин", "boot", "кольчужн", "chain", Items.IRON_INGOT, 5, true));
            }
            if (buySword.get()) {
                purchasePlan.add(Purchase.item(CAT_WEAPONS, "меч", "sword", "камен", "stone", Items.IRON_INGOT, 10, true));
            }
        }

        if (buyPickaxe.get()) {
            // Цена кирки может отличаться в зависимости от текущего улучшения, поэтому магазин решает,
            // доступна ли покупка. Повторно уже имеющуюся кирку не покупаем.
            purchasePlan.add(Purchase.item(CAT_TOOLS, "кирк", "pickaxe", null, null, null, 0, true));
        }

        if (buyBlocks.get()) {
            if (onlyOneBlock.get()) {
                // Старое поведение: покупаем только один комплект шерсти.
                purchasePlan.add(Purchase.item(CAT_BLOCKS, "шерст", "wool", null, null, null, 0, false));
            } else {
                // Покупаем шерсть повторно, пока магазин не перестанет списывать железо.
                purchasePlan.add(Purchase.repeatedBlocks(CAT_BLOCKS, "шерст", "wool"));
            }
        }
    }

    private void handleBlockPurchaseResult() {
        int currentIron = InvUtil.getInventoryCount(Items.IRON_INGOT);

        if (currentIron < ironBeforeBlockPurchase) {
            int spent = ironBeforeBlockPurchase - currentIron;
            if (detectedBlockCost <= 0) {
                detectedBlockCost = spent;
            }

            waitingForBlockPurchase = false;
            blockPurchaseClickTime = 0L;

            // Стоимость определяем по первой успешной покупке, поэтому логика работает и при изменении цен.
            if (detectedBlockCost > 0 && currentIron < detectedBlockCost) {
                purchaseIndex++;
                step = STEP_SELECT_CATEGORY;
            }
            return;
        }

        // Если железо не списалось за 1.5 секунды, сервер отклонил покупку — ресурсов больше не хватает.
        if (System.currentTimeMillis() - blockPurchaseClickTime >= 1500L) {
            waitingForBlockPurchase = false;
            blockPurchaseClickTime = 0L;
            purchaseIndex++;
            step = STEP_SELECT_CATEGORY;
        }
    }

    private void skipUnavailablePurchases() {
        while (purchaseIndex < purchasePlan.size()) {
            Purchase purchase = purchasePlan.get(purchaseIndex);
            if (purchase.skipWhenOwned && hasItemByName(purchase.ruKey, purchase.enKey)) {
                purchaseIndex++;
                step = STEP_SELECT_CATEGORY;
                searchAttempts = 0;
                continue;
            }
            if (!hasEnoughCurrency(purchase)) {
                purchaseIndex++;
                step = STEP_SELECT_CATEGORY;
                searchAttempts = 0;
                continue;
            }
            break;
        }
    }

    private boolean hasEnoughCurrency(Purchase purchase) {
        return purchase.currency == null
                || purchase.cost <= 0
                || InvUtil.getInventoryCount(purchase.currency) >= purchase.cost;
    }

    private boolean buyItemInCategory(Container container, Purchase purchase) {
        int maxSlot = Math.min(container.inventorySlots.size(), 54);
        for (int slotIndex = 9; slotIndex < maxSlot; slotIndex++) {
            ItemStack stack = container.getSlot(slotIndex).getStack();
            if (stack.isEmpty()) {
                continue;
            }

            String name = stack.getDisplayName().getString().toLowerCase(Locale.ROOT);
            boolean primaryMatch = containsEither(name, purchase.ruKey, purchase.enKey);
            boolean secondaryMatch = purchase.ruKey2 == null
                    || containsEither(name, purchase.ruKey2, purchase.enKey2);

            if (primaryMatch && secondaryMatch) {
                mc.playerController.windowClick(
                        container.windowId,
                        slotIndex,
                        0,
                        ClickType.PICKUP,
                        mc.player
                );
                return true;
            }
        }
        return false;
    }

    private boolean isMineLegacyShopScreen() {
        if (mc.currentScreen == null || mc.player == null || mc.player.openContainer == null) {
            return false;
        }
        String title = mc.currentScreen.getTitle().getString()
                .replaceAll("§[0-9a-fk-or]", "")
                .toLowerCase(Locale.ROOT);
        return title.contains("магазин") || title.contains("shop");
    }

    private boolean hasItemByName(String ruKey, String enKey) {
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(slot);
            if (matchesPrimaryName(stack, ruKey, enKey)) {
                return true;
            }
        }

        EquipmentSlotType[] armorSlots = {
                EquipmentSlotType.HEAD,
                EquipmentSlotType.CHEST,
                EquipmentSlotType.LEGS,
                EquipmentSlotType.FEET
        };
        for (EquipmentSlotType slot : armorSlots) {
            if (matchesPrimaryName(mc.player.getItemStackFromSlot(slot), ruKey, enKey)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesPrimaryName(ItemStack stack, String ruKey, String enKey) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        String name = stack.getDisplayName().getString().toLowerCase(Locale.ROOT);
        return containsEither(name, ruKey, enKey);
    }

    private boolean containsEither(String value, String first, String second) {
        return (first != null && value.contains(first))
                || (second != null && value.contains(second));
    }

    private static final class Purchase {
        private final int categorySlot;
        private final String ruKey;
        private final String enKey;
        private final String ruKey2;
        private final String enKey2;
        private final Item currency;
        private final int cost;
        private final boolean skipWhenOwned;
        private final boolean repeatWhileIronAvailable;

        private Purchase(int categorySlot,
                         String ruKey,
                         String enKey,
                         String ruKey2,
                         String enKey2,
                         Item currency,
                         int cost,
                         boolean skipWhenOwned,
                         boolean repeatWhileIronAvailable) {
            this.categorySlot = categorySlot;
            this.ruKey = ruKey;
            this.enKey = enKey;
            this.ruKey2 = ruKey2;
            this.enKey2 = enKey2;
            this.currency = currency;
            this.cost = cost;
            this.skipWhenOwned = skipWhenOwned;
            this.repeatWhileIronAvailable = repeatWhileIronAvailable;
        }

        private static Purchase item(int categorySlot,
                                     String ruKey,
                                     String enKey,
                                     String ruKey2,
                                     String enKey2,
                                     Item currency,
                                     int cost,
                                     boolean skipWhenOwned) {
            return new Purchase(categorySlot, ruKey, enKey, ruKey2, enKey2, currency, cost, skipWhenOwned, false);
        }

        private static Purchase repeatedBlocks(int categorySlot, String ruKey, String enKey) {
            return new Purchase(categorySlot, ruKey, enKey, null, null, null, 0, false, true);
        }
    }
}
