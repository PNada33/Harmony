package xd.harm.modules.impl.movement;

import xd.harm.events.movement.EventMotion;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.ModeSetting;
import com.google.common.eventbus.Subscribe;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Effects;
import net.minecraft.util.math.BlockPos;

@ModuleRegister(name = "WaterSpeed", category = Category.Movement, desc = "Увеличивает скорость в воде")
public class WaterSpeed extends Module {
    private final ModeSetting mode = new ModeSetting("Мод", "ФанТайм", "ФанТайм", "Обычный");
    private final BooleanSetting autoSwap = new BooleanSetting("Авто свап ботинок", false).setVisible(() -> mode.is("ФанТайм"));

    public WaterSpeed() {
        addSettings(mode, autoSwap);
    }

    @Subscribe
    public void onMotion(EventMotion event) {
        if (!mc.player.isInWater()) return;

        if (mode.is("ФанТайм")) {
            BlockPos posUp1 = mc.player.getPosition().up(1);
            BlockPos posUp2 = mc.player.getPosition().up(2);
            boolean isUnderwater = mc.world.getBlockState(posUp1).getBlock() == Blocks.WATER ||
                    mc.world.getBlockState(posUp2).getBlock() == Blocks.WATER;

            if (!isUnderwater) {
                boolean hasDolphins = mc.player.isPotionActive(Effects.DOLPHINS_GRACE);
                double speed = hasDolphins ? 1.01D : 1.05D;
                mc.player.setMotion(
                        mc.player.getMotion().x * speed,
                        mc.player.getMotion().y * 0.8D,
                        mc.player.getMotion().z * speed
                );
            } else {
                boolean hasDepthStrider = getDepthStriderLevel() > 0;
                boolean hasDolphins = mc.player.isPotionActive(Effects.DOLPHINS_GRACE);
                float multiplier;

                if (hasDepthStrider) {
                    if (hasDolphins) {
                        multiplier = 1.021F;
                    } else {
                        multiplier = mc.player.ticksExisted % 7 == 0 ? 1.12F : 1.11F;
                    }
                } else {
                    multiplier = mc.player.ticksExisted % 8 == 0 ? 1.074F : 1.065F;
                }

                mc.player.setMotion(
                        mc.player.getMotion().x * multiplier,
                        mc.player.getMotion().y,
                        mc.player.getMotion().z * multiplier
                );
            }
        } else if (mode.is("Обычный")) {
            boolean hasDolphins = mc.player.isPotionActive(Effects.DOLPHINS_GRACE);
            double speed = hasDolphins ? 1.02D : 1.06D;
            mc.player.setMotion(
                    mc.player.getMotion().x * speed,
                    mc.player.getMotion().y,
                    mc.player.getMotion().z * speed
            );
        }

        if (autoSwap.get()) {
            trySwapBoots();
        }
    }

    private void trySwapBoots() {
        ItemStack currentBoots = mc.player.getItemStackFromSlot(EquipmentSlotType.FEET);
        boolean currentHasDS = EnchantmentHelper.getEnchantmentLevel(Enchantments.DEPTH_STRIDER, currentBoots) > 0;

        if (mc.player.isInWater() && !currentHasDS) {
            int slot = findBootsWithDepthStrider();
            if (slot != -1) {
                swapBoots(slot);
            }
        }
    }

    private int findBootsWithDepthStrider() {
        for (int i = 9; i < 45; i++) {
            ItemStack stack = mc.player.container.getSlot(i).getStack();
            if (stack.getItem() instanceof ArmorItem) {
                ArmorItem armor = (ArmorItem) stack.getItem();
                if (armor.getEquipmentSlot() == EquipmentSlotType.FEET) {
                    if (EnchantmentHelper.getEnchantmentLevel(Enchantments.DEPTH_STRIDER, stack) > 0) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    private void swapBoots(int slot) {
        mc.playerController.windowClick(mc.player.container.windowId, slot, 0, ClickType.PICKUP, mc.player);
        mc.playerController.windowClick(mc.player.container.windowId, 8, 0, ClickType.PICKUP, mc.player);
        mc.playerController.windowClick(mc.player.container.windowId, slot, 0, ClickType.PICKUP, mc.player);
    }

    private int getDepthStriderLevel() {
        ItemStack boots = mc.player.getItemStackFromSlot(EquipmentSlotType.FEET);
        return EnchantmentHelper.getEnchantmentLevel(Enchantments.DEPTH_STRIDER, boots);
    }
}
