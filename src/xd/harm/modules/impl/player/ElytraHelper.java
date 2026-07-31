package xd.harm.modules.impl.player;

import com.google.common.eventbus.Subscribe;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.play.client.CEntityActionPacket;
import net.minecraft.network.play.client.CPlayerTryUseItemPacket;
import net.minecraft.util.Hand;
import net.minecraft.item.UseAction;
import xd.harm.events.input.EventInput;
import xd.harm.events.input.EventKey;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BindSetting;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.SliderSetting;
import xd.harm.utils.math.StopWatch;
import xd.harm.utils.player.InventoryUtil;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ModuleRegister(name = "ElytraHelper", category = Category.Player, desc = "Помогает свапать элитры и использовать фейерверки")
public class ElytraHelper extends Module {
    private static final int CHEST_ARMOR_SLOT = 6;
    private static final long CHEST_SWAP_DELAY_MS = 90L;
    private static final long CHEST_SWAP_STOP_AFTER_MS = 120L;

    private final BindSetting swapChestKey = new BindSetting("Элитры", -1);
    private final BindSetting fireWorkKey = new BindSetting("Фейерверк", -1);
    private final BooleanSetting autoFly = new BooleanSetting("Авто взлёт", true);
    private final BooleanSetting autoJump = new BooleanSetting("Авто прыжок", true);
    private final BooleanSetting autoFireWork = new BooleanSetting("Авто фейерверк", false);
    private final BooleanSetting skipFireworkWhileEating = new BooleanSetting("Не кидать когда ешь", true).setVisible(() -> autoFireWork.get());
    private final BooleanSetting swapToOffhand = new BooleanSetting("Феир в левую руку", true);
    private final BooleanSetting autoFireWorkStart = new BooleanSetting("Только при взлёте", false).setVisible(() -> autoFireWork.get());
    private final SliderSetting autoFireWorkDelay = new SliderSetting("Таймер феера", 570.0f, 100.0f, 2000.0f, 10.0f).setVisible(() -> autoFireWork.get() && !autoFireWorkStart.get());
    private final BooleanSetting autoStop = new BooleanSetting("Авто-стоп", true);

    private final StopWatch stopWatch = new StopWatch();
    private final StopWatch autoFireworkTimer = new StopWatch();
    private final StopWatch swapCooldownTimer = new StopWatch();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private ItemStack currentStack = ItemStack.EMPTY;
    private boolean fireworkQueued;
    private boolean recentlySwapped;
    private boolean hasFiredOnStart;
    private boolean swapInProgress;

    public ElytraHelper() {
        addSettings(swapChestKey, fireWorkKey, autoJump, autoFly, autoFireWork, skipFireworkWhileEating, autoFireWorkStart, autoFireWorkDelay, swapToOffhand, autoStop);
    }

    @Subscribe
    private void onUpdate(EventUpdate event) {
        if (mc.player == null) {
            return;
        }

        currentStack = mc.player.getItemStackFromSlot(EquipmentSlotType.CHEST);

        if (autoJump.get()
                && !mc.player.abilities.isFlying
                && mc.player.isOnGround()
                && currentStack.getItem() == Items.ELYTRA
                && !mc.gameSettings.keyBindJump.isKeyDown()
                && !mc.player.isInWater()
                && !mc.player.isInLava()) {
            mc.player.jump();
        }

        if (autoFly.get()
                && !mc.player.abilities.isFlying
                && !mc.player.isInWater()
                && !mc.player.isOnGround()
                && !mc.player.isElytraFlying()
                && currentStack.getItem() == Items.ELYTRA) {
            mc.player.startFallFlying();
            mc.player.connection.sendPacket(new CEntityActionPacket(mc.player, CEntityActionPacket.Action.START_FALL_FLYING));
            if (autoFireWork.get() && autoFireWorkStart.get() && !hasFiredOnStart) {
                if (useFirework()) {
                    hasFiredOnStart = true;
                }
            }
        }

        if (mc.player.isOnGround() || mc.player.isInWater() || mc.player.isInLava()) {
            hasFiredOnStart = false;
        }

        if (mc.player.isElytraFlying()
                && autoFireWork.get()
                && !autoFireWorkStart.get()
                && autoFireworkTimer.isReached((long) autoFireWorkDelay.get().floatValue())) {
            useFirework();
            autoFireworkTimer.reset();
        }

        if (recentlySwapped && swapCooldownTimer.isReached(2000L)) {
            recentlySwapped = false;
        }

        if (fireworkQueued) {
            useFirework();
            fireworkQueued = false;
        }
    }

    @Subscribe
    private void onKey(EventKey event) {
        if (mc.currentScreen != null || mc.player == null) {
            return;
        }

        if (event.getKey() == swapChestKey.get() && stopWatch.isReached(200L)) {
            performChestSwap();
        }

        if (event.getKey() == fireWorkKey.get()) {
            fireworkQueued = true;
        }
    }

    private void performChestSwap() {
        swapInProgress = true;
        requestMovementStop();

        executor.schedule(() -> mc.enqueue(() -> {
            if (mc.player == null || mc.playerController == null) {
                swapInProgress = false;
                return;
            }

            currentStack = mc.player.getItemStackFromSlot(EquipmentSlotType.CHEST);
            changeChestPlate(currentStack);

            stopWatch.reset();
            recentlySwapped = true;
            swapCooldownTimer.reset();

            executor.schedule(() -> swapInProgress = false, CHEST_SWAP_STOP_AFTER_MS, TimeUnit.MILLISECONDS);
        }), CHEST_SWAP_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    private void changeChestPlate(ItemStack stack) {
        if (stack.getItem() != Items.ELYTRA) {
            int elytraSlot = getItemSlot(Items.ELYTRA);
            if (elytraSlot >= 0) {
                if (!equipElytra(elytraSlot)) {
                    print("Не удалось свапнуть элитру!");
                }
            } else {
                print("Элитра не найдена!");
            }
            requestMovementStop();
            return;
        }

        int armorSlot = getChestPlateSlot();
        int freeSlot = findFreeInventorySlot();
        if (armorSlot >= 0) {
            if (!equipChestPlate(armorSlot)) {
                print("Не удалось свапнуть нагрудник!");
            }
        } else if (freeSlot >= 0) {
            if (!quickMoveSlot(CHEST_ARMOR_SLOT)) {
                print("Не удалось снять элитру!");
            }
        }
        requestMovementStop();
    }

    private boolean useFirework() {
        if (!canUseFireworkNow()) {
            return false;
        }

        if (getItemSlot(Items.FIREWORK_ROCKET) == -1) {
            if (mc.player != null && mc.player.isElytraFlying()) {
                print("Фейерверки не найдены!");
            }
            return false;
        }

        if (swapToOffhand.get() && useFireworkFromOffhand()) {
            return true;
        }

        requestMovementStop();
        InventoryUtil.inventorySwapClick(Items.FIREWORK_ROCKET, false);
        return true;
    }

    private boolean canUseFireworkNow() {
        if (!skipFireworkWhileEating.get() || mc.player == null) {
            return true;
        }
        if (!mc.player.isHandActive()) {
            return true;
        }
        UseAction useAction = mc.player.getActiveItemStack().getUseAction();
        return useAction != UseAction.EAT && useAction != UseAction.DRINK;
    }

    private boolean useFireworkFromOffhand() {
        int fireworkSlot = getItemSlot(Items.FIREWORK_ROCKET);
        if (fireworkSlot < 0 || mc.player == null) {
            return false;
        }

        requestMovementStop();
        mc.playerController.windowClick(0, fireworkSlot, 40, ClickType.SWAP, mc.player);
        mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.OFF_HAND));
        mc.playerController.windowClick(0, fireworkSlot, 40, ClickType.SWAP, mc.player);
        return true;
    }

    private int findFreeInventorySlot() {
        for (int i = 9; i < 36; i++) {
            if (mc.player.inventory.getStackInSlot(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private int getChestPlateSlot() {
        Item[] items = {
                Items.NETHERITE_CHESTPLATE,
                Items.DIAMOND_CHESTPLATE,
                Items.GOLDEN_CHESTPLATE,
                Items.IRON_CHESTPLATE,
                Items.LEATHER_CHESTPLATE,
                Items.CHAINMAIL_CHESTPLATE
        };

        for (Item item : items) {
            for (int i = 0; i < 36; ++i) {
                if (mc.player.inventory.getStackInSlot(i).getItem() == item) {
                    if (i < 9) {
                        i += 36;
                    }
                    return i;
                }
            }
        }
        return -1;
    }

    private int getItemSlot(Item input) {
        int slot = -1;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (stack.getItem() == input) {
                slot = i;
                break;
            }
        }
        if (slot < 9 && slot != -1) {
            slot += 36;
        }
        return slot;
    }

    private boolean equipElytra(int elytraSlot) {
        if (mc.player == null || mc.playerController == null) {
            return false;
        }

        if (!mc.player.getItemStackFromSlot(EquipmentSlotType.CHEST).isEmpty()) {
            if (findFreeInventorySlot() == -1) {
                return swapSlotsSafely(elytraSlot, CHEST_ARMOR_SLOT);
            }
            if (!quickMoveSlot(CHEST_ARMOR_SLOT)) {
                return false;
            }
            elytraSlot = getItemSlot(Items.ELYTRA);
        }

        return elytraSlot >= 0 && quickMoveSlot(elytraSlot);
    }

    private boolean equipChestPlate(int armorSlot) {
        if (mc.player == null || mc.playerController == null) {
            return false;
        }

        if (findFreeInventorySlot() == -1) {
            return swapSlotsSafely(armorSlot, CHEST_ARMOR_SLOT);
        }

        if (!mc.player.getItemStackFromSlot(EquipmentSlotType.CHEST).isEmpty() && !quickMoveSlot(CHEST_ARMOR_SLOT)) {
            return false;
        }

        armorSlot = getChestPlateSlot();
        return armorSlot >= 0 && quickMoveSlot(armorSlot);
    }

    private boolean quickMoveSlot(int slot) {
        if (slot < 0 || mc.player == null || mc.playerController == null) {
            return false;
        }

        requestMovementStop();
        mc.playerController.windowClick(0, slot, 0, ClickType.QUICK_MOVE, mc.player);
        return true;
    }

    private boolean swapSlotsSafely(int fromSlot, int toSlot) {
        if (fromSlot == toSlot || mc.player == null || mc.playerController == null) {
            return true;
        }

        requestMovementStop();

        int fromHotbar = toHotbarButton(fromSlot);
        if (fromHotbar != -1) {
            mc.playerController.windowClick(0, toSlot, fromHotbar, ClickType.SWAP, mc.player);
            return true;
        }

        int toHotbar = toHotbarButton(toSlot);
        if (toHotbar != -1) {
            mc.playerController.windowClick(0, fromSlot, toHotbar, ClickType.SWAP, mc.player);
            return true;
        }

        int tempHotbar = findTempHotbarButton();
        if (tempHotbar == -1) {
            return false;
        }

        mc.playerController.windowClick(0, fromSlot, tempHotbar, ClickType.SWAP, mc.player);
        mc.playerController.windowClick(0, toSlot, tempHotbar, ClickType.SWAP, mc.player);
        mc.playerController.windowClick(0, fromSlot, tempHotbar, ClickType.SWAP, mc.player);
        return true;
    }

    private int toHotbarButton(int slot) {
        if (slot >= 36 && slot <= 44) {
            return slot - 36;
        }
        return -1;
    }

    private int findTempHotbarButton() {
        for (int i = 0; i < 9; i++) {
            if (i != mc.player.inventory.currentItem && mc.player.inventory.getStackInSlot(i).isEmpty()) {
                return i;
            }
        }
        for (int i = 0; i < 9; i++) {
            if (i != mc.player.inventory.currentItem) {
                return i;
            }
        }
        return mc.player.inventory.currentItem;
    }

    @Subscribe
    private void onMoveInput(EventInput event) {
        if (!autoStop.get()) {
            return;
        }

        InventoryUtil.handleOffhandSwapStop(event);

        if (swapInProgress || InventoryUtil.isOffhandSwapInProgress()) {
            event.setForward(0.0F);
            event.setStrafe(0.0F);
        }
    }

    private void requestMovementStop() {
        if (!autoStop.get()) {
            return;
        }
        InventoryUtil.startOffhandSwapStop();
    }

    @Override
    public boolean onDisable() {
        stopWatch.reset();
        autoFireworkTimer.reset();
        swapCooldownTimer.reset();
        fireworkQueued = false;
        recentlySwapped = false;
        hasFiredOnStart = false;
        swapInProgress = false;
        return super.onDisable();
    }
}
