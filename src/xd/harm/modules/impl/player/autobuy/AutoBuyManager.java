package xd.harm.modules.impl.player.autobuy;

import com.google.gson.*;
import net.minecraft.item.ItemStack;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class AutoBuyManager {

    private final AutoBuyItems items = new AutoBuyItems();
    private final List<PurchasedItem> purchasedItems = new ArrayList<>();
    private final List<TransactionLog> transactionLogs = new ArrayList<>();
    private final TelegramNotifier telegramNotifier = new TelegramNotifier();
    private final File configFile;
    private final File purchasesFile;
    private final File logsFile;
    private final File telegramFile;

    public AutoBuyManager() {
        File configDir = new File("harmony/autobuy");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        this.configFile = new File(configDir, "items.json");
        this.purchasesFile = new File(configDir, "purchases.json");
        this.logsFile = new File(configDir, "logs.json");
        this.telegramFile = new File(configDir, "telegram.json");
        loadConfig();
        loadLogs();
        loadTelegramConfig();
    }

    public List<AutoBuyItem> getItems() {
        return items.list;
    }

    public AutoBuyItems getItemsModel() {
        return items;
    }

    public List<PurchasedItem> getPurchasedItems() {
        return purchasedItems;
    }
    
    public List<TransactionLog> getTransactionLogs() {
        return transactionLogs;
    }
    
    public TelegramNotifier getTelegramNotifier() {
        return telegramNotifier;
    }

    public void addPurchasedItem(PurchasedItem item) {
        purchasedItems.add(item);
        savePurchases();
    }

    public void addPurchasedItem(ItemStack stack, long price) {
        purchasedItems.add(new PurchasedItem(stack.copy(), LocalDateTime.now(), price, true));
        savePurchases();
    }

    public void clearPurchasedItems() {
        purchasedItems.clear();
        savePurchases();
    }
    
    public void addTransactionLog(TransactionLog log) {
        transactionLogs.add(log);
        saveLogs();
        telegramNotifier.sendTransaction(log);
    }
    
    public void clearTransactionLogs() {
        transactionLogs.clear();
        saveLogs();
    }

    public void updateItemPrice(AutoBuyItem item, String buyPriceStr) {
        try {
            String clean = buyPriceStr.replaceAll("[^\\d]", "");
            if (!clean.isEmpty()) {
                item.buyPrice = Long.parseLong(clean);
            } else {
                item.buyPrice = 0;
            }
        } catch (Exception ignored) {
            item.buyPrice = 0;
        }
    }

    public void updateItemSellPrice(AutoBuyItem item, String sellPriceStr) {
        try {
            String clean = sellPriceStr.replaceAll("[^\\d]", "");
            if (!clean.isEmpty()) {
                item.sellPrice = Long.parseLong(clean);
            } else {
                item.sellPrice = 0;
            }
        } catch (Exception ignored) {
            item.sellPrice = 0;
        }
    }

    public void saveConfig() {
        try {
            JsonArray array = new JsonArray();
            for (AutoBuyItem item : items.list) {
                JsonObject obj = new JsonObject();
                obj.addProperty("name", item.itemName);
                obj.addProperty("buyPrice", item.buyPrice);
                obj.addProperty("sellPrice", item.sellPrice);
                obj.addProperty("itemId", item.itemId);
                obj.addProperty("parsingEnabled", item.parsingEnabled);
                array.add(obj);
            }

            try (Writer writer = new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(array, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void loadConfig() {
        if (!configFile.exists()) return;

        try (Reader reader = new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8)) {
            JsonArray array = new JsonParser().parse(reader).getAsJsonArray();
            Map<String, Integer> orderById = new HashMap<>();
            Map<String, Integer> orderByName = new HashMap<>();
            int order = 0;

            for (JsonElement element : array) {
                JsonObject obj = element.getAsJsonObject();
                String name = obj.get("name").getAsString();
                String itemId = obj.has("itemId") ? obj.get("itemId").getAsString() : "";
                long buyPrice = obj.has("buyPrice") ? obj.get("buyPrice").getAsLong() : 0;
                long sellPrice = obj.has("sellPrice") ? obj.get("sellPrice").getAsLong() : 0;
                boolean parsing = obj.has("parsingEnabled") && obj.get("parsingEnabled").getAsBoolean();

                AutoBuyItem matchedItem = findItem(name, itemId);
                if (matchedItem != null) {
                    matchedItem.buyPrice = buyPrice;
                    matchedItem.sellPrice = sellPrice;
                    matchedItem.parsingEnabled = parsing;
                }

                if (!itemId.isEmpty()) {
                    orderById.put(itemId, order);
                }
                orderByName.put(name, order);
                order++;
            }

            sortItemsByOrder(orderById, orderByName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private AutoBuyItem findItem(String name, String itemId) {
        if (name != null && !name.isEmpty()) {
            for (AutoBuyItem item : items.list) {
                if (name.equals(item.itemName)) {
                    return item;
                }
            }
        }
        if (itemId != null && !itemId.isEmpty()) {
            for (AutoBuyItem item : items.list) {
                if (itemId.equals(item.itemId)) {
                    return item;
                }
            }
        }
        return null;
    }

    private void sortItemsByOrder(Map<String, Integer> orderById, Map<String, Integer> orderByName) {
        Map<AutoBuyItem, Integer> originalIndex = new IdentityHashMap<>();
        for (int i = 0; i < items.list.size(); i++) {
            originalIndex.put(items.list.get(i), i);
        }

        items.list.sort((a, b) -> {
            int orderA = resolveOrder(a, orderById, orderByName);
            int orderB = resolveOrder(b, orderById, orderByName);
            if (orderA != orderB) {
                return Integer.compare(orderA, orderB);
            }
            return Integer.compare(originalIndex.get(a), originalIndex.get(b));
        });
    }

    private int resolveOrder(AutoBuyItem item, Map<String, Integer> orderById, Map<String, Integer> orderByName) {
        Integer byName = orderByName.get(item.itemName);
        if (byName != null) {
            return byName;
        }
        Integer byId = item.itemId != null ? orderById.get(item.itemId) : null;
        if (byId != null) {
            return byId;
        }
        return Integer.MAX_VALUE;
    }

    private void savePurchases() {
        try {
            JsonArray array = new JsonArray();

            for (PurchasedItem item : purchasedItems) {
                JsonObject obj = new JsonObject();
                obj.addProperty("name", item.stack.getDisplayName().getString());
                obj.addProperty("price", item.price);
                obj.addProperty("time", item.purchaseTime.toString());
                obj.addProperty("success", item.success);
                array.add(obj);
            }

            try (Writer writer = new OutputStreamWriter(new FileOutputStream(purchasesFile), StandardCharsets.UTF_8)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(array, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void saveLogs() {
        try {
            JsonArray array = new JsonArray();
            for (TransactionLog log : transactionLogs) {
                JsonObject obj = new JsonObject();
                obj.addProperty("type", log.type.name());
                obj.addProperty("itemName", log.itemName);
                obj.addProperty("quantity", log.quantity);
                obj.addProperty("price", log.price);
                obj.addProperty("timestamp", log.timestamp.toString());
                obj.addProperty("server", log.server);
                obj.addProperty("account", log.account);
                array.add(obj);
            }
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(logsFile), StandardCharsets.UTF_8)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(array, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void loadLogs() {
        if (!logsFile.exists()) return;
        try (Reader reader = new InputStreamReader(new FileInputStream(logsFile), StandardCharsets.UTF_8)) {
            JsonArray array = new JsonParser().parse(reader).getAsJsonArray();
            for (JsonElement element : array) {
                JsonObject obj = element.getAsJsonObject();
                TransactionLog.Type type = TransactionLog.Type.valueOf(obj.get("type").getAsString());
                String itemName = obj.get("itemName").getAsString();
                int quantity = obj.get("quantity").getAsInt();
                long price = obj.get("price").getAsLong();
                LocalDateTime timestamp = LocalDateTime.parse(obj.get("timestamp").getAsString());
                String server = obj.get("server").getAsString();
                String account = obj.get("account").getAsString();
                transactionLogs.add(new TransactionLog(type, ItemStack.EMPTY, itemName, quantity, price, timestamp, server, account));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void saveTelegramConfig() {
        try {
            JsonObject obj = new JsonObject();
            obj.addProperty("botToken", telegramNotifier.getToken());
            obj.addProperty("chatId", telegramNotifier.getChatId());
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(telegramFile), StandardCharsets.UTF_8)) {
                new GsonBuilder().setPrettyPrinting().create().toJson(obj, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void loadTelegramConfig() {
        if (!telegramFile.exists()) return;
        try (Reader reader = new InputStreamReader(new FileInputStream(telegramFile), StandardCharsets.UTF_8)) {
            JsonObject obj = new JsonParser().parse(reader).getAsJsonObject();
            if (obj.has("botToken")) telegramNotifier.setToken(obj.get("botToken").getAsString());
            if (obj.has("chatId")) telegramNotifier.setChatId(obj.get("chatId").getAsLong());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


