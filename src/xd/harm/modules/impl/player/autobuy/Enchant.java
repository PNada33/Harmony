package xd.harm.modules.impl.player.autobuy;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;

import java.util.Map;

public class Enchant {
    public final String enchantmentId;
    public final int level;
    public final String enchantmentName;
    public final boolean isCustom;
    public transient Enchantment enchantment;

    public Enchant(Enchantment enchantment, int level) {
        this.enchantment = enchantment;
        this.level = level;
        this.enchantmentId = Registry.ENCHANTMENT.getKey(enchantment).toString();
        this.enchantmentName = null;
        this.isCustom = false;
    }

    public Enchant(String enchantmentName, int level) {
        this.enchantment = null;
        this.level = level;
        this.enchantmentName = enchantmentName;
        this.enchantmentId = null;
        this.isCustom = true;
    }

    public Enchant(String enchantmentId, int level, boolean isCustom) {
        this.enchantmentId = enchantmentId;
        this.level = level;
        this.enchantmentName = null;
        this.isCustom = isCustom;
        this.enchantment = (enchantmentId != null && !isCustom) ? Registry.ENCHANTMENT.getOrDefault(new ResourceLocation(enchantmentId)) : null;
    }

    public boolean has(ItemStack item) {
        if (item == null) {
            return false;
        }
        if (isCustom) {
            CompoundNBT tag = item.getTag();
            if (tag == null) {
                return false;
            }
            String levelStr = (level + "")
                    .replace("1", "I")
                    .replace("2", "II")
                    .replace("3", "III")
                    .replace("-1", "");
            return tag.toString().contains(enchantmentName + (levelStr.isEmpty() ? "" : " " ) + levelStr);
        }
        if (item.getItem() == Items.ENCHANTED_BOOK && enchantment != null) {
            ListNBT enchList = EnchantedBookItem.getEnchantments(item);
            for (int i = 0; i < enchList.size(); i++) {
                CompoundNBT ench = enchList.getCompound(i);
                String enchId = ench.getString("id");
                int enchLvl = ench.getShort("lvl");
                if (enchantmentId != null && enchId.equals(enchantmentId)) {
                    if (level == -1 || enchLvl >= level) {
                        return true;
                    }
                }
            }
            return false;
        }
        if (item.isEnchanted() && enchantment != null) {
            Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(item);
            if (level == -1) {
                return enchantments.containsKey(enchantment);
            }
            return enchantments.getOrDefault(enchantment, 0) >= level;
        }
        return false;
    }
}
