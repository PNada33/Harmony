package xd.harm.modules.impl.combat;

import com.google.common.eventbus.Subscribe;
import net.minecraft.item.*;
import net.minecraft.network.play.client.CCloseWindowPacket;
import org.lwjgl.glfw.GLFW;
import xd.harm.events.input.EventInput;
import xd.harm.events.input.EventKey;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.api.Notify;
import xd.harm.modules.settings.impl.BindSetting;
import xd.harm.modules.settings.impl.ModeSetting;
import xd.harm.ui.display.impl.SwapRenderer;
import xd.harm.utils.math.StopWatch;
import xd.harm.utils.player.InventoryUtil;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ModuleRegister(name = "AutoSwap", category = Category.Combat, desc = "Меняет предметы в левой руке")
public class AutoSwap extends Module {

    public final ModeSetting swapMode = new ModeSetting("Мод", "Два предмета", "Два предмета", "Три предмета");
    private final BindSetting keyToSwap = new BindSetting("Кнопка", -1).setVisible(() -> swapMode.is("Два предмета"));
    private final BindSetting wheelKey = new BindSetting("Кнопка меню", -1).setVisible(() -> swapMode.is("Три предмета"));
    private final ModeSetting changeType = new ModeSetting("Режим свапа", "Старый", "Старый", "Новый").setVisible(() -> swapMode.is("Два предмета"));
    private final ModeSetting itemType = new ModeSetting("Предмет", "Щит", "Талисман", "Щит", "Геплы", "Тотем", "Шар/Сфера", "Фейерверки", "Чарки", "Еда").setVisible(() -> swapMode.is("Два предмета"));
    private final ModeSetting swapType = new ModeSetting("Свапать на", "Геплы", "Талисман", "Щит", "Геплы", "Тотем", "Шар/Сфера", "Фейерверки", "Чарки", "Еда").setVisible(() -> swapMode.is("Два предмета"));

    private final StopWatch stopWatch = new StopWatch();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private static final List<Item> EXCLUDED_FOOD = Arrays.asList(
            Items.GOLDEN_APPLE, Items.ENCHANTED_GOLDEN_APPLE,
            Items.TOTEM_OF_UNDYING, Items.PLAYER_HEAD, Items.FIREWORK_ROCKET
    );

    public static boolean swapInProgress = false;
    public static ItemStack[] threeItems = {ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
    public static boolean wheelMenuOpen = false;
    public static int hoveredSlot = -1;
    public static boolean selectingItem = false;
    public static int selectingSlotIndex = -1;

    private boolean wasKeyPressed = false;

    public AutoSwap() {
        addSettings(swapMode, changeType, itemType, swapType, keyToSwap, wheelKey);
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;

        if (swapMode.is("Три предмета")) {
            handleThreeItemMode();
        }
    }

    private void handleThreeItemMode() {
        if (wheelMenuOpen || mc.currentScreen != null) return;

        int key = wheelKey.get();
        if (key == -1) return;

        boolean isKeyPressed = GLFW.glfwGetKey(mc.getMainWindow().getHandle(), key) == GLFW.GLFW_PRESS;

        if (isKeyPressed && !wasKeyPressed) {
            wasKeyPressed = true;
            SwapRenderer.open(key);
        }

        if (!isKeyPressed) {
            wasKeyPressed = false;
        }
    }

    @Subscribe
    public void onEventKey(EventKey e) {
        if (mc.player == null) return;

        if (swapMode.is("Два предмета")) {
            handleTwoItemMode(e);
        }
    }

    private void handleTwoItemMode(EventKey e) {
        if (!e.isKeyDown(keyToSwap.get()) || !stopWatch.isReached(200L)) return;

        ItemStack offhandStack = mc.player.getHeldItemOffhand();
        ItemStack selectedStack = getItemStackByType(itemType.get());
        ItemStack swapStack = getItemStackByType(swapType.get());

        int targetSlot = -1;

        if (!selectedStack.isEmpty() && !stacksMatch(offhandStack, selectedStack)) {
            targetSlot = findSlotForStack(selectedStack);
        } else if (!swapStack.isEmpty() && !stacksMatch(offhandStack, swapStack)) {
            targetSlot = findSlotForStack(swapStack);
        }

        if (targetSlot >= 0) {
            performSwap(targetSlot, !offhandStack.isEmpty());
        }
    }

    private void performSwap(int slot, boolean offhandNotEmpty) {
        swapInProgress = true;

        executor.schedule(() -> mc.enqueue(() -> {
            if (changeType.is("Новый")) {
                InventoryUtil.swapToOffhand(slot);
            } else {
                InventoryUtil.moveItem(slot, 45, offhandNotEmpty);
            }

            if (mc.currentScreen == null && mc.player.inventory.getItemStack().isEmpty()) {
                mc.player.connection.sendPacket(new CCloseWindowPacket(0));
            }

            stopWatch.reset();
            showSwapNotification();

            executor.schedule(() -> swapInProgress = false, 65, TimeUnit.MILLISECONDS);
        }), 50, TimeUnit.MILLISECONDS);
    }

    public void performThreeItemSwap(ItemStack targetStack) {
        if (targetStack.isEmpty()) return;

        int slot = findSlotForStack(targetStack);
        if (slot < 0) return;

        swapInProgress = true;

        executor.schedule(() -> mc.enqueue(() -> {
            InventoryUtil.swapToOffhand(slot);

            if (mc.currentScreen == null && mc.player.inventory.getItemStack().isEmpty()) {
                mc.player.connection.sendPacket(new CCloseWindowPacket(0));
            }

            stopWatch.reset();
            showSwapNotification();

            executor.schedule(() -> swapInProgress = false, 65, TimeUnit.MILLISECONDS);
        }), 50, TimeUnit.MILLISECONDS);
    }

    private void showSwapNotification() {
        try {
            xd.harm.modules.impl.misc.Notifications miscNotification = xd.harm.Harmony.getInstance().getModuleManager().getNotifications();
            if (miscNotification == null || !miscNotification.isState()) {
                return;
            }
        } catch (Exception ignored) {
            return;
        }

        ItemStack offhand = mc.player.getHeldItemOffhand();
        if (offhand.isEmpty()) return;

        String name = offhand.getDisplayName().getString();
        Notify.NOTIFICATION_MANAGER.add("Свапнул на " + name, "", 3);
    }

    private int findSlotForStack(ItemStack target) {
        if (target.isEmpty()) return -1;

        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (stacksMatch(stack, target)) {
                return i < 9 ? i + 36 : i;
            }
        }
        return -1;
    }

    private boolean stacksMatch(ItemStack first, ItemStack second) {
        if (first.isEmpty() || second.isEmpty()) return false;
        if (first.getItem() != second.getItem()) return false;
        if (first.hasTag() != second.hasTag()) return false;
        return !first.hasTag() || first.getTag().equals(second.getTag());
    }

    @Subscribe
    private void onMoveInput(EventInput e) {
        InventoryUtil.handleOffhandSwapStop(e);
        if (swapInProgress || InventoryUtil.isOffhandSwapInProgress()) {
            e.setForward(0.0F);
            e.setStrafe(0.0F);
        }
    }

    private ItemStack getItemStackByType(String type) {
        return switch (type) {
            case "Талисман" -> findEnchantedTotem();
            case "Щит" -> findItem(Items.SHIELD);
            case "Тотем" -> findDefaultTotem();
            case "Геплы" -> findItem(Items.GOLDEN_APPLE);
            case "Шар/Сфера" -> findSphere();
            case "Фейерверки" -> findItem(Items.FIREWORK_ROCKET);
            case "Чарки" -> findItem(Items.ENCHANTED_GOLDEN_APPLE);
            case "Еда" -> findFood();
            default -> ItemStack.EMPTY;
        };
    }

    private ItemStack findItem(Item item) {
        for (int i = 0; i < mc.player.inventory.getSizeInventory(); i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (stack.getItem() == item) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private ItemStack findSphere() {
        for (int i = 0; i < mc.player.inventory.getSizeInventory(); i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (stack.getItem() == Items.PLAYER_HEAD && stack.hasTag()) {
                String name = stack.getDisplayName().getString().toLowerCase();
                if (!name.contains("голова")) {
                    return stack;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    private ItemStack findEnchantedTotem() {
        for (int i = 0; i < mc.player.inventory.getSizeInventory(); i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (stack.getItem() == Items.TOTEM_OF_UNDYING && stack.isEnchanted()) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private ItemStack findDefaultTotem() {
        for (int i = 0; i < mc.player.inventory.getSizeInventory(); i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (stack.getItem() == Items.TOTEM_OF_UNDYING && !stack.isEnchanted()) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private ItemStack findFood() {
        for (int i = 0; i < mc.player.inventory.getSizeInventory(); i++) {
            if (i == 45) continue;
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (stack.getItem() == Items.GOLDEN_CARROT) {
                return stack;
            }
        }

        for (int i = 0; i < mc.player.inventory.getSizeInventory(); i++) {
            if (i == 45) continue;
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (stack.getItem().isFood() && !EXCLUDED_FOOD.contains(stack.getItem())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public static void setThreeItem(int index, ItemStack stack) {
        if (index >= 0 && index < 3) {
            threeItems[index] = stack.copy();
        }
    }

    public static void clearThreeItem(int index) {
        if (index >= 0 && index < 3) {
            threeItems[index] = ItemStack.EMPTY;
        }
    }

    @Override
    public boolean onDisable() {
        super.onDisable();
        wheelMenuOpen = false;
        selectingItem = false;
        selectingSlotIndex = -1;
        hoveredSlot = -1;
        wasKeyPressed = false;
        return false;
    }
}
