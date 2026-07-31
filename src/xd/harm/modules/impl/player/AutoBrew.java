package xd.harm.modules.impl.player;

import xd.harm.events.movement.EventMotion;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.BindSetting;
import xd.harm.modules.settings.impl.SliderSetting;
import xd.harm.utils.player.InventoryUtil;
import com.google.common.eventbus.Subscribe;
import net.minecraft.client.gui.screen.inventory.BrewingStandScreen;
import net.minecraft.client.gui.screen.inventory.ChestScreen;
import net.minecraft.client.util.InputMappings;
import net.minecraft.inventory.container.BrewingStandContainer;
import net.minecraft.inventory.container.ChestContainer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionUtils;
import net.minecraft.potion.Potions;
import net.minecraft.tileentity.BrewingStandTileEntity;
import net.minecraft.tileentity.ChestTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ModuleRegister(name = "AutoBrew", category = Category.Player, desc = "Автоматически варит зелья")
public class AutoBrew extends Module {

    private final BooleanSetting strengthPotion = new BooleanSetting("Зелье силы", true);
    private final BooleanSetting speedPotion = new BooleanSetting("Зелье скорости", false);
    private final BooleanSetting fireResPotion = new BooleanSetting("Зелье огнестойкости", false);
    private final BooleanSetting invisPotion = new BooleanSetting("Зелье невидимости", false);
    private final BooleanSetting enhance = new BooleanSetting("Усиливать зелья", true);
    private final SliderSetting delay = new SliderSetting("Задержка", 150f, 50f, 500f, 10f);

    private final BooleanSetting useCustomChests = new BooleanSetting("Свои сундуки", false);
    private final BindSetting setIngredientChest = new BindSetting("Сундук ингредиентов", GLFW.GLFW_KEY_G).setVisible(() -> useCustomChests.get());
    private final BindSetting setStorageChest = new BindSetting("Сундук хранения", GLFW.GLFW_KEY_H).setVisible(() -> useCustomChests.get());
    private final BindSetting readyKey = new BindSetting("Готов", GLFW.GLFW_KEY_Y).setVisible(() -> useCustomChests.get());

    private BrewState currentState = BrewState.WAITING;
    private BrewingStandTileEntity currentBrewingStand;
    private ChestTileEntity currentChest;
    private List<BlockPos> usedStands = new ArrayList<>();
    private List<BlockPos> usedChests = new ArrayList<>();
    private long lastActionTime = 0;
    private Item neededItem = null;
    private boolean needBottles = false;
    private Set<Item> collectedIngredients = new HashSet<>();
    private boolean bottlesCollected = false;

    private BlockPos ingredientChestPos = null;
    private BlockPos storageChestPos = null;
    private boolean isReady = false;

    public AutoBrew() {
        addSettings(strengthPotion, speedPotion, fireResPotion, invisPotion, enhance, delay, useCustomChests, setIngredientChest, setStorageChest, readyKey);
    }

    private String getKeyName(BindSetting bind) {
        int key = bind.get();
        return InputMappings.getInputByCode(key, 0).func_237520_d_().getString().toUpperCase();
    }

    @Override
    public boolean onEnable() {
        currentBrewingStand = null;
        currentChest = null;
        usedStands.clear();
        usedChests.clear();
        lastActionTime = 0;
        neededItem = null;
        needBottles = false;
        collectedIngredients.clear();
        bottlesCollected = false;
        ingredientChestPos = null;
        storageChestPos = null;

        if (useCustomChests.get()) {
            currentState = BrewState.WAITING;
            isReady = false;
            print("Установите сундуки и нажмите [" + getKeyName(readyKey) + "] чтобы начать");
        } else {
            currentState = BrewState.SEARCH;
            isReady = true;
        }

        return super.onEnable();
    }

    @Override
    public boolean onDisable() {
        if (mc.player != null && mc.currentScreen != null) {
            mc.player.closeScreen();
        }
        isReady = false;
        return super.onDisable();
    }

    @Subscribe
    public void onMotion(EventMotion event) {
        if (mc.player == null || mc.world == null) return;

        handleKeyBinds();

        switch (currentState) {
            case WAITING:
                break;
            case SEARCH:
                handleSearch();
                break;
            case GATHER_INGREDIENTS:
                handleGatherIngredients();
                break;
            case OPEN_STAND:
                handleOpenStand();
                break;
            case BREWING:
                handleBrewing();
                break;
            case STORE_POTIONS:
                handleStorePotions();
                break;
            case TAKE_FROM_CHEST:
                handleTakeFromChest();
                break;
        }
    }

    private void handleKeyBinds() {
        if (useCustomChests.get()) {
            if (setIngredientChest.isPressed()) {
                BlockPos lookingAt = getLookingAtBlock();
                if (lookingAt != null && mc.world.getTileEntity(lookingAt) instanceof ChestTileEntity) {
                    ingredientChestPos = lookingAt;
                    print("Сундук ингредиентов установлен");
                    checkReady();
                } else {
                    print("Смотрите на сундук!");
                }
            }

            if (setStorageChest.isPressed()) {
                BlockPos lookingAt = getLookingAtBlock();
                if (lookingAt != null && mc.world.getTileEntity(lookingAt) instanceof ChestTileEntity) {
                    storageChestPos = lookingAt;
                    print("Сундук хранения установлен");
                    checkReady();
                } else {
                    print("Смотрите на сундук!");
                }
            }

            if (readyKey.isPressed()) {
                if (currentState == BrewState.WAITING) {
                    if (ingredientChestPos != null && storageChestPos != null) {
                        isReady = true;
                        collectedIngredients.clear();
                        bottlesCollected = false;
                        currentState = BrewState.SEARCH;
                        print("Начинаем варку!");
                    } else {
                        StringBuilder missing = new StringBuilder("§cНе установлены: ");
                        if (ingredientChestPos == null) missing.append("сундук ингредиентов ");
                        if (storageChestPos == null) missing.append("сундук хранения");
                        print(missing.toString());
                    }
                } else {
                    isReady = false;
                    currentState = BrewState.WAITING;
                    currentChest = null;
                    currentBrewingStand = null;
                    neededItem = null;
                    needBottles = false;
                    if (mc.currentScreen != null) {
                        mc.player.closeScreen();
                    }
                    print("Остановлено. Нажмите [" + getKeyName(readyKey) + "] чтобы продолжить");
                }
            }
        }
    }

    private void checkReady() {
        if (ingredientChestPos != null && storageChestPos != null) {
            print("Всё готово! Нажмите [" + getKeyName(readyKey) + "] чтобы начать");
        }
    }

    private BlockPos getLookingAtBlock() {
        if (mc.objectMouseOver != null && mc.objectMouseOver.getType() == net.minecraft.util.math.RayTraceResult.Type.BLOCK) {
            return ((BlockRayTraceResult) mc.objectMouseOver).getPos();
        }
        return null;
    }


    private List<Item> getRequiredIngredients() {
        List<Item> required = new ArrayList<>();

        required.add(Items.BLAZE_POWDER);
        required.add(Items.NETHER_WART);

        if (strengthPotion.get()) {
            if (enhance.get()) {
                required.add(Items.GLOWSTONE_DUST);
            }
        }

        if (speedPotion.get()) {
            required.add(Items.SUGAR);
            if (enhance.get() && !required.contains(Items.GLOWSTONE_DUST)) {
                required.add(Items.GLOWSTONE_DUST);
            }
        }

        if (fireResPotion.get()) {
            required.add(Items.MAGMA_CREAM);
            if (enhance.get()) {
                required.add(Items.REDSTONE);
            }
        }

        if (invisPotion.get()) {
            required.add(Items.GOLDEN_CARROT);
            required.add(Items.FERMENTED_SPIDER_EYE);
            if (enhance.get() && !required.contains(Items.REDSTONE)) {
                required.add(Items.REDSTONE);
            }
        }

        return required;
    }

    private void handleSearch() {
        if (!isReady && useCustomChests.get()) {
            currentState = BrewState.WAITING;
            return;
        }

        if (!hasTimePassed(500)) return;

        if (mc.currentScreen != null) {
            mc.player.closeScreen();
            resetTimer();
            return;
        }

        List<Item> required = getRequiredIngredients();
        boolean needsIngredients = false;
        for (Item item : required) {
            if (!collectedIngredients.contains(item) && countItemInInventory(item) == 0) {
                needsIngredients = true;
                break;
            }
        }

        if (needsIngredients) {
            currentChest = null;
            currentState = BrewState.GATHER_INGREDIENTS;
            resetTimer();
            return;
        }

        if (!bottlesCollected && countPotionInInventory(Potions.WATER) < 3) {
            needBottles = true;
            currentChest = null;
            currentState = BrewState.GATHER_INGREDIENTS;
            resetTimer();
            return;
        }

        usedStands.clear();
        List<BrewingStandTileEntity> stands = findBrewingStands();
        if (!stands.isEmpty()) {
            currentBrewingStand = stands.get(0);
            currentState = BrewState.OPEN_STAND;
            resetTimer();
        }
    }

    private int countItemInInventory(Item item) {
        int count = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private int countPotionInInventory(Potion potionType) {
        int count = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() == Items.POTION) {
                Potion potion = PotionUtils.getPotionFromItem(stack);
                if (potion == potionType) {
                    count += stack.getCount();
                }
            }
        }
        return count;
    }

    private void handleGatherIngredients() {
        if (!isReady && useCustomChests.get()) {
            currentState = BrewState.WAITING;
            if (mc.currentScreen != null) {
                mc.player.closeScreen();
            }
            return;
        }

        if (currentChest == null) {
            if (useCustomChests.get()) {
                if (ingredientChestPos != null) {
                    TileEntity te = mc.world.getTileEntity(ingredientChestPos);
                    if (te instanceof ChestTileEntity) {
                        currentChest = (ChestTileEntity) te;
                    } else {
                        print("Сундук ингредиентов не найден! [" + getKeyName(setIngredientChest) + "]");
                        toggle();
                        return;
                    }
                } else {
                    print("Установите сундук ингредиентов! [" + getKeyName(setIngredientChest) + "]");
                    toggle();
                    return;
                }
            } else {
                List<ChestTileEntity> chests = findChests();
                if (chests.isEmpty()) {
                    print("Сундуки не найдены!");
                    toggle();
                    return;
                }
                currentChest = chests.get(0);
            }
        }

        if (!(mc.currentScreen instanceof ChestScreen)) {
            if (hasTimePassed(300)) {
                openChest(currentChest);
                resetTimer();
            }
            return;
        }

        if (!hasTimePassed(delay.get().longValue())) return;

        ChestScreen chestScreen = (ChestScreen) mc.currentScreen;
        ChestContainer chestContainer = chestScreen.getContainer();
        int windowId = chestContainer.windowId;

        if (needBottles) {
            int bottleSlot = findWaterBottleInChest(chestContainer);
            if (bottleSlot != -1) {
                InventoryUtil.shiftClick(windowId, bottleSlot);
                resetTimer();

                if (findWaterBottleInChest(chestContainer) == -1 || countPotionInInventory(Potions.WATER) >= 27) {
                    needBottles = false;
                    bottlesCollected = true;
                    mc.player.closeScreen();
                    currentChest = null;
                    currentState = BrewState.SEARCH;
                }
            } else {
                if (countPotionInInventory(Potions.WATER) >= 3) {
                    needBottles = false;
                    bottlesCollected = true;
                    mc.player.closeScreen();
                    currentChest = null;
                    currentState = BrewState.SEARCH;
                    resetTimer();
                } else {
                    print("Бутылки с водой не найдены!");
                    mc.player.closeScreen();
                    toggle();
                }
            }
            return;
        }

        List<Item> required = getRequiredIngredients();
        Item needed = null;
        for (Item item : required) {
            if (!collectedIngredients.contains(item) && countItemInInventory(item) == 0) {
                needed = item;
                break;
            }
        }

        if (needed == null) {
            mc.player.closeScreen();
            currentChest = null;
            currentState = BrewState.SEARCH;
            resetTimer();
            return;
        }

        int itemSlot = findItemInChest(chestContainer, needed);
        if (itemSlot != -1) {
            InventoryUtil.shiftClick(windowId, itemSlot);
            collectedIngredients.add(needed);
            resetTimer();
        } else {
            print("Не найден: " + needed.getName().getString());
            mc.player.closeScreen();
            toggle();
        }
    }

    private int findItemInChest(ChestContainer container, Item item) {
        int chestSize = container.getLowerChestInventory().getSizeInventory();
        for (int i = 0; i < chestSize; i++) {
            ItemStack stack = container.getSlot(i).getStack();
            if (!stack.isEmpty() && stack.getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    private int findWaterBottleInChest(ChestContainer container) {
        int chestSize = container.getLowerChestInventory().getSizeInventory();
        for (int i = 0; i < chestSize; i++) {
            ItemStack stack = container.getSlot(i).getStack();
            if (!stack.isEmpty() && stack.getItem() == Items.POTION) {
                Potion potion = PotionUtils.getPotionFromItem(stack);
                if (potion == Potions.WATER) {
                    return i;
                }
            }
        }
        return -1;
    }

    private void handleOpenStand() {
        if (mc.currentScreen instanceof BrewingStandScreen) {
            currentState = BrewState.BREWING;
            resetTimer();
            return;
        }

        if (currentBrewingStand == null) {
            currentState = BrewState.SEARCH;
            resetTimer();
            return;
        }

        if (hasTimePassed(300)) {
            BlockPos pos = currentBrewingStand.getPos();

            double dist = mc.player.getDistanceSq(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (dist > 25) {
                currentBrewingStand = null;
                currentState = BrewState.SEARCH;
                resetTimer();
                return;
            }

            Vector3d hitVec = new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            BlockRayTraceResult result = new BlockRayTraceResult(hitVec, Direction.UP, pos, false);
            mc.playerController.processRightClickBlock(mc.player, mc.world, Hand.MAIN_HAND, result);
            resetTimer();
        }
    }

    private void handleBrewing() {
        if (!(mc.currentScreen instanceof BrewingStandScreen)) {
            currentState = BrewState.SEARCH;
            return;
        }

        BrewingStandScreen screen = (BrewingStandScreen) mc.currentScreen;
        BrewingStandContainer container = screen.getContainer();
        int windowId = container.windowId;

        if (!hasTimePassed(delay.get().longValue())) return;

        if (container.getBrewTime() > 0) {
            return;
        }

        if (isInventoryFull()) {
            mc.player.closeScreen();
            currentState = BrewState.STORE_POTIONS;
            resetTimer();
            return;
        }

        if (hasFinishedPotions(container)) {
            takeAllPotions(windowId, container);
            resetTimer();
            return;
        }

        Potion currentPotion = getCurrentPotion(container);

        if (currentPotion != null && !isWorkablePotion(currentPotion)) {
            takeAllPotions(windowId, container);
            resetTimer();
            return;
        }

        int fuel = container.getFuel();
        if (fuel <= 0) {
            int blazeSlot = findItemInContainer(container, Items.BLAZE_POWDER);
            if (blazeSlot != -1) {
                InventoryUtil.putSingleItem(windowId, blazeSlot, 4);
                resetTimer();
                return;
            } else {
                neededItem = Items.BLAZE_POWDER;
                mc.player.closeScreen();
                currentChest = null;
                currentState = BrewState.TAKE_FROM_CHEST;
                resetTimer();
                return;
            }
        }

        int filledSlots = 0;
        for (int i = 0; i < 3; i++) {
            if (!container.getSlot(i).getStack().isEmpty()) {
                filledSlots++;
            }
        }

        if (filledSlots == 0) {
            int waterSlot = findWaterBottle(container);
            if (waterSlot != -1) {
                InventoryUtil.shiftClick(windowId, waterSlot);
                resetTimer();
                return;
            }

            int awkwardSlot = findAwkwardPotion(container);
            if (awkwardSlot != -1) {
                InventoryUtil.shiftClick(windowId, awkwardSlot);
                resetTimer();
                return;
            }

            mc.player.closeScreen();
            needBottles = true;
            bottlesCollected = false;
            currentChest = null;
            currentState = BrewState.GATHER_INGREDIENTS;
            resetTimer();
            return;
        }

        if (filledSlots < 3) {
            Potion slotPotion = getCurrentPotion(container);
            if (slotPotion == Potions.WATER) {
                int waterSlot = findWaterBottle(container);
                if (waterSlot != -1) {
                    InventoryUtil.shiftClick(windowId, waterSlot);
                    resetTimer();
                    return;
                }
            } else if (slotPotion == Potions.AWKWARD) {
                int awkwardSlot = findAwkwardPotion(container);
                if (awkwardSlot != -1) {
                    InventoryUtil.shiftClick(windowId, awkwardSlot);
                    resetTimer();
                    return;
                }
            }
        }

        Item neededIngredient = getNeededIngredient(container);
        if (neededIngredient == null) {
            return;
        }

        ItemStack ingredientStack = container.getSlot(3).getStack();
        if (!ingredientStack.isEmpty()) {
            if (ingredientStack.getItem() != neededIngredient) {
                InventoryUtil.shiftClick(windowId, 3);
                resetTimer();
            }
            return;
        }

        int ingredientSlot = findItemInContainer(container, neededIngredient);
        if (ingredientSlot != -1) {
            InventoryUtil.putSingleItem(windowId, ingredientSlot, 3);
            resetTimer();
        } else {
            neededItem = neededIngredient;
            mc.player.closeScreen();
            currentChest = null;
            currentState = BrewState.TAKE_FROM_CHEST;
            resetTimer();
        }
    }

    private boolean isInventoryFull() {
        for (int i = 9; i < 45; i++) {
            if (mc.player.container.getSlot(i).getStack().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private boolean hasFinishedPotions(BrewingStandContainer container) {
        for (int i = 0; i < 3; i++) {
            ItemStack stack = container.getSlot(i).getStack();
            if (!stack.isEmpty() && stack.getItem() == Items.POTION) {
                Potion potion = PotionUtils.getPotionFromItem(stack);
                if (isFinishedPotion(potion)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isFinishedPotion(Potion potion) {
        if (potion == null) return false;

        if (enhance.get()) {
            if (strengthPotion.get() && potion == Potions.STRONG_STRENGTH) return true;
            if (speedPotion.get() && potion == Potions.STRONG_SWIFTNESS) return true;
            if (fireResPotion.get() && potion == Potions.LONG_FIRE_RESISTANCE) return true;
            if (invisPotion.get() && potion == Potions.LONG_INVISIBILITY) return true;
        } else {
            if (strengthPotion.get() && potion == Potions.STRENGTH) return true;
            if (speedPotion.get() && potion == Potions.SWIFTNESS) return true;
            if (fireResPotion.get() && potion == Potions.FIRE_RESISTANCE) return true;
            if (invisPotion.get() && potion == Potions.INVISIBILITY) return true;
        }

        return false;
    }

    private boolean isWorkablePotion(Potion potion) {
        if (potion == null) return false;
        if (potion == Potions.WATER) return true;
        if (potion == Potions.AWKWARD) return true;
        if (potion == Potions.NIGHT_VISION && invisPotion.get()) return true;
        if (potion == Potions.STRENGTH && strengthPotion.get()) return true;
        if (potion == Potions.SWIFTNESS && speedPotion.get()) return true;
        if (potion == Potions.FIRE_RESISTANCE && fireResPotion.get()) return true;
        if (potion == Potions.INVISIBILITY && invisPotion.get()) return true;
        return false;
    }

    private void takeAllPotions(int windowId, BrewingStandContainer container) {
        for (int i = 0; i < 3; i++) {
            ItemStack stack = container.getSlot(i).getStack();
            if (!stack.isEmpty()) {
                InventoryUtil.shiftClick(windowId, i);
            }
        }
    }

    private void handleTakeFromChest() {
        if (neededItem == null) {
            currentState = BrewState.OPEN_STAND;
            resetTimer();
            return;
        }

        if (currentChest == null) {
            if (useCustomChests.get()) {
                if (ingredientChestPos != null) {
                    TileEntity te = mc.world.getTileEntity(ingredientChestPos);
                    if (te instanceof ChestTileEntity) {
                        currentChest = (ChestTileEntity) te;
                    } else {
                        print("Сундук ингредиентов не найден!");
                        toggle();
                        return;
                    }
                } else {
                    print("Установите сундук ингредиентов! [" + getKeyName(setIngredientChest) + "]");
                    toggle();
                    return;
                }
            } else {
                List<ChestTileEntity> chests = findChests();
                if (chests.isEmpty()) {
                    print("Сундуки не найдены!");
                    toggle();
                    return;
                }
                currentChest = chests.get(0);
            }
        }

        if (!(mc.currentScreen instanceof ChestScreen)) {
            if (hasTimePassed(300)) {
                openChest(currentChest);
                resetTimer();
            }
            return;
        }

        if (!hasTimePassed(delay.get().longValue())) return;

        ChestScreen chestScreen = (ChestScreen) mc.currentScreen;
        ChestContainer chestContainer = chestScreen.getContainer();
        int windowId = chestContainer.windowId;

        int itemSlot = findItemInChest(chestContainer, neededItem);
        if (itemSlot != -1) {
            InventoryUtil.shiftClick(windowId, itemSlot);
            collectedIngredients.add(neededItem);
            neededItem = null;
            mc.player.closeScreen();
            currentChest = null;
            currentState = BrewState.OPEN_STAND;
            resetTimer();
        } else {
            print("Не найден: " + neededItem.getName().getString());
            mc.player.closeScreen();
            toggle();
        }
    }

    private int findItemInContainer(BrewingStandContainer container, Item item) {
        for (int i = 5; i < 41; i++) {
            ItemStack stack = container.getSlot(i).getStack();
            if (!stack.isEmpty() && stack.getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    private int findWaterBottle(BrewingStandContainer container) {
        for (int i = 5; i < 41; i++) {
            ItemStack stack = container.getSlot(i).getStack();
            if (!stack.isEmpty() && stack.getItem() == Items.POTION) {
                Potion potion = PotionUtils.getPotionFromItem(stack);
                if (potion == Potions.WATER) {
                    return i;
                }
            }
        }
        return -1;
    }

    private int findAwkwardPotion(BrewingStandContainer container) {
        for (int i = 5; i < 41; i++) {
            ItemStack stack = container.getSlot(i).getStack();
            if (!stack.isEmpty() && stack.getItem() == Items.POTION) {
                Potion potion = PotionUtils.getPotionFromItem(stack);
                if (potion == Potions.AWKWARD) {
                    return i;
                }
            }
        }
        return -1;
    }

    private Item getNeededIngredient(BrewingStandContainer container) {
        Potion currentPotion = getCurrentPotion(container);
        if (currentPotion == null) return null;

        if (currentPotion == Potions.WATER) {
            return Items.NETHER_WART;
        }

        if (currentPotion == Potions.AWKWARD) {
            if (strengthPotion.get()) return Items.BLAZE_POWDER;
            if (speedPotion.get()) return Items.SUGAR;
            if (fireResPotion.get()) return Items.MAGMA_CREAM;
            if (invisPotion.get()) return Items.GOLDEN_CARROT;
        }

        if (currentPotion == Potions.NIGHT_VISION) {
            if (invisPotion.get()) return Items.FERMENTED_SPIDER_EYE;
        }

        if (enhance.get()) {
            if (strengthPotion.get() && currentPotion == Potions.STRENGTH) {
                return Items.GLOWSTONE_DUST;
            }
            if (speedPotion.get() && currentPotion == Potions.SWIFTNESS) {
                return Items.GLOWSTONE_DUST;
            }
            if (fireResPotion.get() && currentPotion == Potions.FIRE_RESISTANCE) {
                return Items.REDSTONE;
            }
            if (invisPotion.get() && currentPotion == Potions.INVISIBILITY) {
                return Items.REDSTONE;
            }
        }

        return null;
    }

    private Potion getCurrentPotion(BrewingStandContainer container) {
        for (int i = 0; i < 3; i++) {
            ItemStack stack = container.getSlot(i).getStack();
            if (!stack.isEmpty() && stack.getItem() == Items.POTION) {
                return PotionUtils.getPotionFromItem(stack);
            }
        }
        return null;
    }

    private void handleStorePotions() {
        if (mc.currentScreen instanceof BrewingStandScreen) {
            if (hasTimePassed(300)) {
                mc.player.closeScreen();
                resetTimer();
            }
            return;
        }

        if (!hasFinishedPotionsInInventory()) {
            currentState = BrewState.SEARCH;
            currentChest = null;
            resetTimer();
            return;
        }

        if (currentChest == null) {
            if (useCustomChests.get()) {
                if (storageChestPos != null) {
                    TileEntity te = mc.world.getTileEntity(storageChestPos);
                    if (te instanceof ChestTileEntity) {
                        currentChest = (ChestTileEntity) te;
                    } else {
                        currentState = BrewState.SEARCH;
                        resetTimer();
                        return;
                    }
                } else {
                    currentState = BrewState.SEARCH;
                    resetTimer();
                    return;
                }
            } else {
                List<ChestTileEntity> chests = findChests();
                if (chests.isEmpty()) {
                    currentState = BrewState.SEARCH;
                    resetTimer();
                    return;
                }
                currentChest = chests.get(0);
            }
        }

        if (!(mc.currentScreen instanceof ChestScreen)) {
            if (hasTimePassed(300)) {
                openChest(currentChest);
                resetTimer();
            }
            return;
        }

        if (!hasTimePassed(delay.get().longValue())) return;

        ChestScreen chestScreen = (ChestScreen) mc.currentScreen;
        ChestContainer chestContainer = chestScreen.getContainer();
        int windowId = chestContainer.windowId;
        int chestSize = chestContainer.getLowerChestInventory().getSizeInventory();

        boolean foundPotion = false;
        for (int i = chestSize; i < chestContainer.inventorySlots.size(); i++) {
            ItemStack stack = chestContainer.getSlot(i).getStack();
            if (!stack.isEmpty() && stack.getItem() == Items.POTION) {
                Potion potion = PotionUtils.getPotionFromItem(stack);
                if (isFinishedPotion(potion)) {
                    InventoryUtil.shiftClick(windowId, i);
                    foundPotion = true;
                    resetTimer();
                    break;
                }
            }
        }

        if (!foundPotion) {
            mc.player.closeScreen();
            currentChest = null;
            currentState = BrewState.SEARCH;
            resetTimer();
        }
    }

    private boolean hasFinishedPotionsInInventory() {
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() == Items.POTION) {
                Potion potion = PotionUtils.getPotionFromItem(stack);
                if (isFinishedPotion(potion)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<BrewingStandTileEntity> findBrewingStands() {
        List<BrewingStandTileEntity> stands = new ArrayList<>();
        BlockPos playerPos = mc.player.getPosition();

        for (TileEntity te : mc.world.loadedTileEntityList) {
            if (te instanceof BrewingStandTileEntity) {
                BlockPos pos = te.getPos();
                double dist = playerPos.distanceSq(pos);
                if (dist <= 100 && !usedStands.contains(pos)) {
                    stands.add((BrewingStandTileEntity) te);
                }
            }
        }

        stands.sort(Comparator.comparingDouble(s -> s.getPos().distanceSq(playerPos)));
        return stands;
    }

    private List<ChestTileEntity> findChests() {
        List<ChestTileEntity> chests = new ArrayList<>();
        BlockPos playerPos = mc.player.getPosition();

        for (TileEntity te : mc.world.loadedTileEntityList) {
            if (te instanceof ChestTileEntity) {
                BlockPos pos = te.getPos();
                double dist = playerPos.distanceSq(pos);
                if (dist <= 100 && !usedChests.contains(pos)) {
                    chests.add((ChestTileEntity) te);
                }
            }
        }

        chests.sort(Comparator.comparingDouble(c -> c.getPos().distanceSq(playerPos)));
        return chests;
    }

    private void openChest(ChestTileEntity chest) {
        BlockPos pos = chest.getPos();
        Vector3d hitVec = new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        BlockRayTraceResult result = new BlockRayTraceResult(hitVec, Direction.UP, pos, false);
        mc.playerController.processRightClickBlock(mc.player, mc.world, Hand.MAIN_HAND, result);
    }

    private boolean hasTimePassed(long ms) {
        return System.currentTimeMillis() - lastActionTime >= ms;
    }

    private void resetTimer() {
        lastActionTime = System.currentTimeMillis();
    }

    private enum BrewState {
        WAITING,
        SEARCH,
        GATHER_INGREDIENTS,
        OPEN_STAND,
        BREWING,
        STORE_POTIONS,
        TAKE_FROM_CHEST
    }
}
