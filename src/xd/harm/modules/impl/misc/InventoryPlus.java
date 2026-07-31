package xd.harm.modules.impl.misc;

import xd.harm.modules.impl.player.AutoActions;
import com.google.common.eventbus.Subscribe;

import xd.harm.events.network.EventPacket;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.ColorSetting;
import xd.harm.modules.settings.impl.ModeListSetting;
import xd.harm.modules.settings.impl.SliderSetting;
import xd.harm.utils.math.StopWatch;
import xd.harm.utils.player.MoveUtils;
import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.*;
import net.minecraft.network.play.client.CCloseWindowPacket;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.potion.PotionUtils;
import net.minecraft.potion.Potions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@ModuleRegister(name = "InventoryPlus", category = Category.Misc, desc = "Помощник в инвентаре")
public class InventoryPlus extends Module {

    public BooleanSetting xcarry = new BooleanSetting("Сохранять предметы в слотах крафта", false);
    public BooleanSetting itemScroller = new BooleanSetting("Быстро складывать предметы", true);
    public BooleanSetting itemHelper = new BooleanSetting("Подсвечивание предметов", false);

    public ModeListSetting itemsToHighlight = new ModeListSetting("Подсвечивать",
            new BooleanSetting("Зелье исцеления", true),
            new BooleanSetting("Зачарованное яблоко", true),
            new BooleanSetting("Золотое яблоко", true)).setVisible(() -> itemHelper.get());

    public SliderSetting animationSpeed = new SliderSetting("Скорость анимации", 3.0f, 1.0f, 5.0f, 0.5f).setVisible(() -> itemHelper.get());

    public ColorSetting healingPotionColor = new ColorSetting("Цвет исцела", 0xFF00FF00).setVisible(() -> itemHelper.get() && itemsToHighlight.getValueByName("Зелье исцеления").get());
    public ColorSetting enchantedAppleColor = new ColorSetting("Цвет зачар. яблока", 0xFFFFD700).setVisible(() -> itemHelper.get() && itemsToHighlight.getValueByName("Зачарованное яблоко").get());
    public ColorSetting goldenAppleColor = new ColorSetting("Цвет золотого яблока", 0xFFFFAA00).setVisible(() -> itemHelper.get() && itemsToHighlight.getValueByName("Золотое яблоко").get());

    public BooleanSetting autoArmor = new BooleanSetting("AutoArmor", true);
    final SliderSetting delay = new SliderSetting("Задержка", 100.0f, 0.0f, 1000.0f, 1.0f).setVisible(() -> autoArmor.get());
    final BooleanSetting onlyInv = new BooleanSetting("Только в инве", false).setVisible(() -> autoArmor.get());
    final BooleanSetting workInMove = new BooleanSetting("Работать в движении", true).setVisible(() -> autoArmor.get());
    final BooleanSetting autoStop = new BooleanSetting("Авто-стоп", true).setVisible(() -> autoArmor.get());
    final BooleanSetting ignoreElytra = new BooleanSetting("Игнорировать нагрудник с элитрами", false).setVisible(() -> autoArmor.get());
    final SliderSetting stopDelay = new SliderSetting("Задержка стопа", 80.0f, 80.0f, 400.0f, 1.0f).setVisible(() -> autoArmor.get() && autoStop.get());

    final StopWatch stopWatchAutoArmor = new StopWatch();
    public static boolean swapInProgress = false;
    private long swapDelay;

    private static final int MAX_ALPHA = 100;

    public InventoryPlus() {
        addSettings(xcarry, itemScroller, itemHelper, itemsToHighlight, animationSpeed,
                healingPotionColor, enchantedAppleColor, goldenAppleColor, autoArmor, delay, onlyInv,
                workInMove, autoStop, ignoreElytra, stopDelay);
    }

    private boolean hasHealingEffect(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof PotionItem && !(item instanceof SplashPotionItem) && !(item instanceof LingeringPotionItem)) {
            List<EffectInstance> effects = PotionUtils.getEffectsFromStack(stack);
            for (EffectInstance effect : effects) {
                if (effect.getPotion() == Effects.INSTANT_HEALTH) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean shouldHighlight(ItemStack stack) {
        if (!itemHelper.get() || stack.isEmpty()) return false;

        Item item = stack.getItem();

        if (itemsToHighlight.getValueByName("Зелье исцеления").get() && hasHealingEffect(stack)) {
            return true;
        }

        if (itemsToHighlight.getValueByName("Зачарованное яблоко").get() && item == Items.ENCHANTED_GOLDEN_APPLE) {
            return true;
        }

        if (itemsToHighlight.getValueByName("Золотое яблоко").get() && item == Items.GOLDEN_APPLE) {
            return true;
        }

        return false;
    }

    public int getHighlightColor(ItemStack stack) {
        if (!itemHelper.get() || stack.isEmpty()) return 0;

        Item item = stack.getItem();

        if (hasHealingEffect(stack)) {
            return healingPotionColor.get();
        }

        if (item == Items.ENCHANTED_GOLDEN_APPLE) {
            return enchantedAppleColor.get();
        }

        if (item == Items.GOLDEN_APPLE) {
            return goldenAppleColor.get();
        }

        return 0;
    }

    public float getAnimationAlpha() {
        float speed = animationSpeed.get();
        float time = (System.currentTimeMillis() % (long)(1000 / speed)) / (1000f / speed);
        return (float)(Math.sin(time * Math.PI * 2) * 0.5 + 0.5) * (MAX_ALPHA / 255.0f);
    }

    @Subscribe
    public void onUpdate(EventUpdate e) {
        if (autoArmor.get()) {
            if (!workInMove.get() && MoveUtils.isMoving()) {
                return;
            }

            if (onlyInv.get() && !(mc.currentScreen instanceof InventoryScreen)) {
                return;
            }

            PlayerInventory inventory = mc.player.inventory;
            int[] bestArmorSlots = new int[4];
            int[] armorValues = new int[4];
            boolean hasElytra = mc.player.getItemStackFromSlot(EquipmentSlotType.CHEST).getItem() instanceof ElytraItem;

            if (ignoreElytra.get() && hasElytra) {
                bestArmorSlots[EquipmentSlotType.CHEST.getIndex()] = -1;
            }

            for (int slot = 0; slot < 4; ++slot) {
                bestArmorSlots[slot] = -1;
                ItemStack stack = inventory.armorItemInSlot(slot);
                if (isItemValid(stack)) {
                    Item item = stack.getItem();
                    if (item instanceof ArmorItem) {
                        ArmorItem armor = (ArmorItem) item;
                        armorValues[slot] = calculateArmorValue(armor, stack);
                    }
                }
            }

            for (int slot = 0; slot < 36; ++slot) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (isItemValid(stack)) {
                    Item item = stack.getItem();
                    if (item instanceof ArmorItem) {
                        ArmorItem armor = (ArmorItem) item;
                        EquipmentSlotType slotType = armor.getSlot();
                        int slotIndex = slotType.getIndex();
                        if (slotType != EquipmentSlotType.CHEST || !ignoreElytra.get() || !hasElytra) {
                            int value = calculateArmorValue(armor, stack);
                            if (value > armorValues[slotIndex]) {
                                bestArmorSlots[slotIndex] = slot;
                                armorValues[slotIndex] = value;
                            }
                        }
                    }
                }
            }

            Integer[] slots = new Integer[]{0, 1, 2, 3};
            ArrayList<Integer> slotList = new ArrayList<>(Arrays.asList(slots));
            Collections.shuffle(slotList);

            for (int slotIndex : slotList) {
                int targetSlot = bestArmorSlots[slotIndex];
                if (targetSlot != -1 && (!isItemValid(inventory.armorItemInSlot(slotIndex)) || inventory.getFirstEmptyStack() != -1)) {
                    if (targetSlot < 9) {
                        targetSlot += 36;
                    }

                    if (stopWatchAutoArmor.isReached(delay.get().longValue())) {
                        ItemStack currentArmor = inventory.armorItemInSlot(slotIndex);
                        int finalTargetSlot = targetSlot;
                        initiateSwap(() -> {
                            if (isItemValid(currentArmor)) {
                                mc.playerController.windowClick(0, 8 - slotIndex, 0, ClickType.QUICK_MOVE, mc.player);
                            }
                            mc.playerController.windowClick(0, finalTargetSlot, 0, ClickType.QUICK_MOVE, mc.player);
                        });
                        stopWatchAutoArmor.reset();
                    }
                    break;
                }
            }
        }
    }

    private void initiateSwap(Runnable action) {
        swapInProgress = true;
        if (autoStop.get()) {
            this.swapDelay = stopDelay.get().longValue();
        } else {
            this.swapDelay = 0L;
        }

        new Thread(() -> {
            try {
                Thread.sleep(this.swapDelay);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }

            action.run();

            try {
                Thread.sleep(100L);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }

            swapInProgress = false;
        }).start();
    }

    @Subscribe
    public void onPacket(EventPacket e) {
        if (mc.player == null) return;
        if (e.getPacket() instanceof CCloseWindowPacket && xcarry.get()) {
            e.cancel();
        }
    }

    private boolean isItemValid(ItemStack stack) {
        return stack != null && !stack.isEmpty();
    }

    private int calculateArmorValue(ArmorItem armor, ItemStack stack) {
        int protection = EnchantmentHelper.getEnchantmentLevel(Enchantments.PROTECTION, stack);
        IArmorMaterial material = armor.getArmorMaterial();
        int damageReduction = material.getDamageReductionAmount(armor.getEquipmentSlot());
        return armor.getDamageReduceAmount() * 20 + protection * 12 + (int) (armor.getToughness() * 2.0F) + damageReduction * 5 >> 3;
    }
}
