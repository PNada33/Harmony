package xd.harm.modules.impl.player.autobuy;

import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.registry.Registry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AutoBuyItem {

    public String itemName;
    public long buyPrice;
    public long sellPrice;
    public ItemStack itemStack;
    public String itemId;
    public boolean parsingEnabled;

    public String spookyItemType;
    public HashMap<Attribute, Map.Entry<Float, AttributeModifier.Operation>> attributes;
    public List<Enchant> enchants;
    public List<PotionEffectMatcher> potionEffects;
    public String texture;

    public float selectionAnim = 0.0F;
    public float priceAnim = 0.0F;
    public float sellPriceAnim = 0.0F;
    public float parsingAnim = 0.0F;

    public AutoBuyItem(long buyPrice, long sellPrice, ItemStack stack, String itemName) {
        this(buyPrice, sellPrice, stack, itemName, "");
    }

    public AutoBuyItem(long buyPrice, long sellPrice, ItemStack stack, String itemName, String itemId) {
        this.buyPrice = buyPrice;
        this.sellPrice = sellPrice;
        this.itemStack = stack;
        this.itemName = itemName != null ? itemName : "";
        this.itemId = itemId != null ? itemId : "";
        this.parsingEnabled = true;
        this.spookyItemType = null;
        this.texture = "";
        this.attributes = new HashMap<>();
        this.enchants = new ArrayList<>();
        this.potionEffects = new ArrayList<>();
    }

    public AutoBuyItem(String itemName, long buyPrice, Item item) {
        this(itemName, buyPrice, item, new HashMap<>(), null, "", new ArrayList<>(), new ArrayList<>());
    }

    public AutoBuyItem(String itemName, long buyPrice, Item item, HashMap<Attribute, Map.Entry<Float, AttributeModifier.Operation>> attributes) {
        this(itemName, buyPrice, item, attributes, null, "", new ArrayList<>(), new ArrayList<>());
    }

    public AutoBuyItem(String itemName, long buyPrice, Item item,
                       HashMap<Attribute, Map.Entry<Float, AttributeModifier.Operation>> attributes,
                       String spookyItemType,
                       String texture) {
        this(itemName, buyPrice, item, attributes, spookyItemType, texture, new ArrayList<>(), new ArrayList<>());
    }

    public AutoBuyItem(String itemName, long buyPrice, Item item, String spookyItemType, String texture) {
        this(itemName, buyPrice, item, new HashMap<>(), spookyItemType, texture, new ArrayList<>(), new ArrayList<>());
    }

    public AutoBuyItem(String itemName, long buyPrice, Item item, List<PotionEffectMatcher> potionEffects) {
        this(itemName, buyPrice, item, new HashMap<>(), null, "", new ArrayList<>(), potionEffects);
    }

    public AutoBuyItem(String itemName, long buyPrice, Item item, List<Enchant> enchants, String spookyItemType, String texture) {
        this(itemName, buyPrice, item, new HashMap<>(), spookyItemType, texture, enchants, new ArrayList<>());
    }

    public AutoBuyItem(String itemName, long buyPrice, Item item,
                       HashMap<Attribute, Map.Entry<Float, AttributeModifier.Operation>> attributes,
                       String spookyItemType,
                       String texture,
                       List<Enchant> enchants,
                       List<PotionEffectMatcher> potionEffects) {
        this.itemName = itemName != null ? itemName : "";
        this.buyPrice = buyPrice;
        this.sellPrice = 0;
        this.itemStack = createStack(item, this.itemName, texture);
        this.itemId = item != null && Registry.ITEM.getKey(item) != null ? Registry.ITEM.getKey(item).toString() : "";
        this.parsingEnabled = true;
        this.spookyItemType = spookyItemType;
        this.texture = texture != null ? texture : "";
        this.attributes = attributes != null ? attributes : new HashMap<>();
        this.enchants = enchants != null ? enchants : new ArrayList<>();
        this.potionEffects = potionEffects != null ? potionEffects : new ArrayList<>();
    }

    private static ItemStack createStack(Item item, String name, String texture) {
        ItemStack stack = new ItemStack(item);
        if (name != null && !name.isEmpty()) {
            stack.setDisplayName(new StringTextComponent(name));
        }
        if (texture != null && !texture.isEmpty()) {
            try {
                stack.setTag(JsonToNBT.getTagFromJson(String.format(
                        "{SkullOwner:{Properties:{textures:[{Value:\"%s\"}]},Name:\"%s\"}}",
                        texture, name == null ? "" : name)));
            } catch (Exception ignored) {
            }
        }
        return stack;
    }

    public boolean hasBuyPrice() {
        return this.buyPrice > 0;
    }

    public boolean hasSellPrice() {
        return this.sellPrice > 0;
    }
}
