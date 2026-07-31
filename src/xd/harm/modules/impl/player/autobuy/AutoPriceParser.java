package xd.harm.modules.impl.player.autobuy;

import net.minecraft.inventory.container.ChestContainer;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.PotionUtils;
import xd.harm.modules.impl.player.AutoBuy;
import xd.harm.utils.client.IMinecraft;
import xd.harm.utils.client.InvUtil;
import xd.harm.utils.client.TimerUtility;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AutoPriceParser implements IMinecraft {
    private final AutoBuy module;
    private final AutoBuyManager manager;

    private static final long SEARCH_DELAY_BASE = 1000;
    private static final long SEARCH_DELAY_JITTER = 500;
    private static final long PAGE_UPDATE_BASE = 300;
    private static final long PAGE_UPDATE_JITTER = 200;
    private static final long ANALYSIS_BASE = 200;
    private static final long ANALYSIS_JITTER = 150;
    private static final long ANALYSIS_MIN_BASE = 300;
    private static final long ANALYSIS_MIN_JITTER = 200;
    private static final long CLOSE_SCREEN_DELAY = 400;
    private static final long AUTO_RESTART_MS = 180_000;
    private static final long AH_COMMAND_COOLDOWN = 1500;
    private static final long SEARCH_RESULT_TIMEOUT = 7000;
    private static final int SEARCH_RETRY_LIMIT = 1;
    private static final int POST_FINISH_AH_TRIES = 3;

    private boolean isParsingPrices = false;
    private int currentParsingIndex = 0;
    private final List<AutoBuyItem> itemsToParse = new ArrayList<>();
    private final TimerUtility parsingDelay = new TimerUtility();
    private final TimerUtility pageUpdateTimer = new TimerUtility();
    private final TimerUtility closeScreenTimer = new TimerUtility();
    private final TimerUtility analysisDelay = new TimerUtility();
    private boolean waitingForResults = false;
    private boolean needsPageUpdate = false;
    private long nextAutoRestartAt = -1;
    private boolean pendingAutoRestart = false;
    private long lastAutoAhCommand = 0;
    private long searchStartedAt = -1;
    private int searchRetryCount = 0;
    private int postFinishAhTries = 0;
    private long postFinishAhNextAt = -1;

    public AutoPriceParser(AutoBuy module, AutoBuyManager manager) {
        this.module = module;
        this.manager = manager;
    }

    public boolean isWaitingForResults() {
        return waitingForResults;
    }

    public void startPriceParsing() {
        if (!module.getParser().get()) {
            return;
        }

        itemsToParse.clear();
        for (AutoBuyItem item : manager.getItems()) {
            if (item.parsingEnabled) {
                itemsToParse.add(item);
            }
        }

        if (itemsToParse.isEmpty()) {
            return;
        }

        isParsingPrices = true;
        nextAutoRestartAt = -1;
        pendingAutoRestart = false;
        postFinishAhTries = 0;
        postFinishAhNextAt = -1;
        currentParsingIndex = 0;
        parsingDelay.setTime(0);
        closeScreenTimer.reset();
        waitingForResults = false;
        needsPageUpdate = false;
        pageUpdateTimer.reset();
        searchStartedAt = -1;
        searchRetryCount = 0;
    }

    public void stopPriceParsing() {
        if (isParsingPrices) {
            isParsingPrices = false;
            waitingForResults = false;
            needsPageUpdate = false;
            closeScreenTimer.reset();
            nextAutoRestartAt = -1;
            pendingAutoRestart = false;
            searchStartedAt = -1;
            searchRetryCount = 0;
            postFinishAhTries = 0;
            postFinishAhNextAt = -1;
        }
    }

    public boolean isParsing() {
        return isParsingPrices;
    }

    private void startNextSearch() {
        if (currentParsingIndex >= itemsToParse.size()) {
            finishParsing();
            return;
        }

        AutoBuyItem currentItem = itemsToParse.get(currentParsingIndex);

        if (!parsingDelay.hasTimeElapsed(SEARCH_DELAY_BASE + (long) (Math.random() * SEARCH_DELAY_JITTER))) {
            return;
        }

        sendSearch(currentItem, false);
    }

    public void updatePriceParsing() {
        if (!isParsingPrices) return;

        if (!module.getParser().get()) {
            return;
        }

        if (waitingForResults && searchStartedAt > 0 && System.currentTimeMillis() - searchStartedAt > SEARCH_RESULT_TIMEOUT) {
            retryOrSkipCurrentSearch();
            return;
        }

        if (needsPageUpdate && pageUpdateTimer.hasTimeElapsed(PAGE_UPDATE_BASE + (long) (Math.random() * PAGE_UPDATE_JITTER))) {
            if (AutoBuyUtil.isAuctionOpened()) {
                if (!hasUpdateSlot()) {
                    retryOrSkipCurrentSearch();
                    return;
                }
                InvUtil.clickSlotId(49, 0, ClickType.PICKUP, false);
                needsPageUpdate = false;
                waitingForResults = true;
                analysisDelay.reset();
            }
        }

        if (waitingForResults && analysisDelay.hasTimeElapsed(ANALYSIS_BASE + (long) (Math.random() * ANALYSIS_JITTER))) {
            if (AutoBuyUtil.isAuctionOpened()) {
                processCurrentSearch();
            }
        }
    }

    public void processCurrentSearch() {
        if (!isParsingPrices || !waitingForResults) return;

        if (!analysisDelay.hasTimeElapsed(ANALYSIS_MIN_BASE + (long) (Math.random() * ANALYSIS_MIN_JITTER))) {
            return;
        }

        waitingForResults = false;

        if (mc.player == null || !(mc.player.openContainer instanceof ChestContainer) || mc.currentScreen == null) {
            retryOrSkipCurrentSearch();
            return;
        }

        ChestContainer ah = (ChestContainer) mc.player.openContainer;

        AutoBuyItem currentItem = itemsToParse.get(currentParsingIndex);
        String title = mc.currentScreen.getTitle().getString();

        if (!isExpectedSearchTitle(title, currentItem) || !hasUpdateSlot()) {
            retryOrSkipCurrentSearch();
            return;
        }

        int lowestPrice = Integer.MAX_VALUE;

        boolean isPotion = currentItem.potionEffects != null && !currentItem.potionEffects.isEmpty();
        boolean isKrushElytra = currentItem.itemName.equalsIgnoreCase("Элитры Крушителя");

        for (Slot slot : ah.inventorySlots) {
            if (slot.slotNumber > 53) continue;
            if (slot.slotNumber == 49) continue;
            if (!slot.getHasStack()) continue;
            if (slot.getStack().isEmpty()) continue;

            boolean isCorrectItem = false;

            if (isPotion) {
                if (slot.getStack().getItem() == currentItem.itemStack.getItem()) {
                    List<EffectInstance> itemEffects = PotionUtils.getFullEffectsFromItem(slot.getStack());
                    if (itemEffects.size() == currentItem.potionEffects.size()) {
                        boolean allEffectsMatch = true;
                        for (PotionEffectMatcher requiredEffect : currentItem.potionEffects) {
                            boolean foundMatch = false;
                            for (EffectInstance itemEffect : itemEffects) {
                                int id = Effect.getId(itemEffect.getPotion());
                                int amplifier = itemEffect.getAmplifier();
                                int duration = itemEffect.getDuration() / 20;
                                if (id == requiredEffect.id && amplifier == requiredEffect.amplifier) {
                                    if (requiredEffect.duration != -1) {
                                        if (duration == requiredEffect.duration) {
                                            foundMatch = true;
                                            break;
                                        }
                                    } else {
                                        foundMatch = true;
                                        break;
                                    }
                                }
                            }
                            if (!foundMatch) {
                                allEffectsMatch = false;
                                break;
                            }
                        }
                        if (allEffectsMatch) isCorrectItem = true;
                    }
                }
            } else if (isKrushElytra) {
                if (slot.getStack().getItem() == currentItem.itemStack.getItem()) {
                    boolean allEnchantsMatch = true;
                    if (currentItem.enchants != null && !currentItem.enchants.isEmpty()) {
                        for (Enchant requiredEnchant : currentItem.enchants) {
                            if (!requiredEnchant.has(slot.getStack())) {
                                allEnchantsMatch = false;
                                break;
                            }
                        }
                    }
                    if (allEnchantsMatch) isCorrectItem = true;
                }
            } else {
                if (slot.getStack().getItem() == currentItem.itemStack.getItem()) {
                    isCorrectItem = true;
                }
            }

            if (isCorrectItem) {
                int price = AutoBuyUtil.getPrice(slot.getStack());
                int itemCount = slot.getStack().getCount();

                if (price > 0 && itemCount > 0) {
                    int finalPrice = price / itemCount;
                    if (finalPrice < 1) {
                        continue;
                    }
                    if (finalPrice < lowestPrice) {
                        lowestPrice = finalPrice;
                    }
                }
            }
        }

        if (lowestPrice != Integer.MAX_VALUE) {
            float discountPercent = module.getParserDiscount().get();
            int newPrice = (int) (lowestPrice * (1 - discountPercent / 100.0));
            currentItem.buyPrice = newPrice;
        }

        closeScreenTimer.reset();

        currentParsingIndex++;
        waitingForResults = false;
        searchStartedAt = -1;
        searchRetryCount = 0;
        parsingDelay.reset();

        if (currentParsingIndex >= itemsToParse.size()) {
            finishParsing();
        }
    }

    public void checkParsingDelay() {
        if (isParsingPrices && !waitingForResults && parsingDelay.hasTimeElapsed(SEARCH_DELAY_BASE + (long) (Math.random() * SEARCH_DELAY_JITTER)) && closeScreenTimer.hasTimeElapsed(CLOSE_SCREEN_DELAY)) {
            if (module.getParser().get()) {
                startNextSearch();
            }
        }
    }

    public void up() {
        if (!module.getParser().get() && isParsingPrices) {
            stopPriceParsing();
            return;
        }
        if (!module.getParser().get()) {
            nextAutoRestartAt = -1;
            pendingAutoRestart = false;
        }
        if (!isParsingPrices) {
            handlePostFinishAh();
        }
        handleAutoRestart();
        if (isParsingPrices) {
            updatePriceParsing();
            checkParsingDelay();
        }
    }

    private void scheduleAutoRestart() {
        nextAutoRestartAt = System.currentTimeMillis() + AUTO_RESTART_MS;
        pendingAutoRestart = false;
    }

    private void handleAutoRestart() {
        if (!module.getParser().get()) {
            return;
        }
        if (isParsingPrices) {
            return;
        }
        long now = System.currentTimeMillis();
        if (nextAutoRestartAt > 0 && now >= nextAutoRestartAt) {
            if (AutoBuyUtil.isAuctionOpened()) {
                startPriceParsing();
                pendingAutoRestart = false;
                nextAutoRestartAt = -1;
                return;
            }
            if (now - lastAutoAhCommand > AH_COMMAND_COOLDOWN) {
                if (mc.player != null) {
                    mc.player.sendChatMessage("/ah");
                }
                lastAutoAhCommand = now;
            }
            pendingAutoRestart = true;
        }
        if (pendingAutoRestart && AutoBuyUtil.isAuctionOpened()) {
            startPriceParsing();
            pendingAutoRestart = false;
            nextAutoRestartAt = -1;
        }
    }

    private void queuePostFinishAh() {
        postFinishAhTries = POST_FINISH_AH_TRIES;
        postFinishAhNextAt = System.currentTimeMillis();
    }

    private void handlePostFinishAh() {
        if (postFinishAhTries <= 0) return;
        long now = System.currentTimeMillis();
        if (now >= postFinishAhNextAt && now - lastAutoAhCommand >= AH_COMMAND_COOLDOWN) {
            if (mc.player != null) {
                mc.player.sendChatMessage("/ah");
            }
            lastAutoAhCommand = now;
            postFinishAhTries--;
            postFinishAhNextAt = now + AH_COMMAND_COOLDOWN;
        }
    }

    private void sendSearch(AutoBuyItem item, boolean retry) {
        if (mc.player == null) {
            skipCurrentSearch();
            return;
        }
        mc.player.sendChatMessage("/ah search " + getSearchQuery(item));
        parsingDelay.reset();
        waitingForResults = true;
        needsPageUpdate = true;
        pageUpdateTimer.reset();
        analysisDelay.reset();
        searchStartedAt = System.currentTimeMillis();
        searchRetryCount = retry ? searchRetryCount + 1 : 0;
    }

    private boolean hasUpdateSlot() {
        if (mc.player == null) {
            return false;
        }
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

    private void retryOrSkipCurrentSearch() {
        if (searchRetryCount < SEARCH_RETRY_LIMIT && currentParsingIndex >= 0 && currentParsingIndex < itemsToParse.size()) {
            sendSearch(itemsToParse.get(currentParsingIndex), true);
        } else {
            skipCurrentSearch();
        }
    }

    private void skipCurrentSearch() {
        currentParsingIndex++;
        waitingForResults = false;
        needsPageUpdate = false;
        searchStartedAt = -1;
        searchRetryCount = 0;
        closeScreenTimer.reset();
        parsingDelay.reset();
        if (currentParsingIndex >= itemsToParse.size()) {
            finishParsing();
        }
    }

    private void finishParsing() {
        isParsingPrices = false;
        waitingForResults = false;
        needsPageUpdate = false;
        searchStartedAt = -1;
        searchRetryCount = 0;
        manager.saveConfig();
        scheduleAutoRestart();
        queuePostFinishAh();
    }

    private String getSearchQuery(AutoBuyItem item) {
        if (item != null && item.itemName.equalsIgnoreCase("Элитры Крушителя")) {
            return "Элитры";
        }
        return item != null ? item.itemName : "";
    }

    private boolean isExpectedSearchTitle(String title, AutoBuyItem item) {
        String normalizedTitle = normalize(title);
        String itemName = normalize(item.itemName);
        String searchQuery = normalize(getSearchQuery(item));
        boolean searchTitle = normalizedTitle.contains("поиск") || normalizedTitle.startsWith("п:");
        return searchTitle
                && ((!itemName.isEmpty() && normalizedTitle.contains(itemName))
                || (!searchQuery.isEmpty() && normalizedTitle.contains(searchQuery)));
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
}
