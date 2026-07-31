package xd.harm.modules.impl.misc;

import com.google.common.eventbus.Subscribe;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import xd.harm.events.world.EventUpdate;
import xd.harm.events.input.EventKey;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.Setting;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.BindSetting;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.inventory.ChestScreen;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@ModuleRegister(name = "AhHelper", category = Category.Render, desc = "Помощник для аукциона")
public class AhHelper extends Module {
    public BooleanSetting three = new BooleanSetting("Подсвечивать 3 слота", true);
    public BindSetting ahSearchKey = new BindSetting("Ah search с рук", -1);

    float x = 0.0F;
    float y = 0.0F;
    float x2 = 0.0F;
    float y2 = 0.0F;
    float x3 = 0.0F;
    float y3 = 0.0F;

    private static final Map<String, String> EN_TO_RU = new HashMap<>();

    static {
        EN_TO_RU.put("bow", "лук");
        EN_TO_RU.put("crossbow", "арбалет");
        EN_TO_RU.put("trident", "трезубец");
        EN_TO_RU.put("sword", "еч");
        EN_TO_RU.put("axe", "топор");
        EN_TO_RU.put("pickaxe", "крка");
        EN_TO_RU.put("shovel", "лопата");
        EN_TO_RU.put("hoe", "отыга");
        EN_TO_RU.put("helmet", "шле");
        EN_TO_RU.put("chestplate", "нагрунк");
        EN_TO_RU.put("leggings", "понож");
        EN_TO_RU.put("boots", "ботнк");
        EN_TO_RU.put("shield", "щт");
        EN_TO_RU.put("elytra", "элтры");
        EN_TO_RU.put("totem of undying", "тоте бессертя");
        EN_TO_RU.put("diamond", "алазный");
        EN_TO_RU.put("iron", "железный");
        EN_TO_RU.put("golden", "золотой");
        EN_TO_RU.put("gold", "золотой");
        EN_TO_RU.put("stone", "каенный");
        EN_TO_RU.put("wooden", "еревянный");
        EN_TO_RU.put("wood", "еревянный");
        EN_TO_RU.put("netherite", "незертовый");
        EN_TO_RU.put("leather", "кожаный");
        EN_TO_RU.put("chainmail", "кольчужный");
        EN_TO_RU.put("apple", "яблоко");
        EN_TO_RU.put("golden apple", "золотое яблоко");
        EN_TO_RU.put("enchanted golden apple", "зачарованное золотое яблоко");
        EN_TO_RU.put("arrow", "стрела");
        EN_TO_RU.put("ender pearl", "энер-жечуг");
        EN_TO_RU.put("fishing rod", "уочка");
        EN_TO_RU.put("flint and steel", "огнво");
        EN_TO_RU.put("shears", "ножнцы");
        EN_TO_RU.put("bucket", "веро");
        EN_TO_RU.put("water bucket", "веро воы");
        EN_TO_RU.put("lava bucket", "веро лавы");
        EN_TO_RU.put("milk bucket", "веро олока");
        EN_TO_RU.put("potion", "зелье");
        EN_TO_RU.put("splash potion", "взрывное зелье");
        EN_TO_RU.put("lingering potion", "туанное зелье");
        EN_TO_RU.put("experience bottle", "пузырк опыта");
        EN_TO_RU.put("book", "кнга");
        EN_TO_RU.put("enchanted book", "зачарованная кнга");
        EN_TO_RU.put("name tag", "брка");
        EN_TO_RU.put("lead", "повоок");
        EN_TO_RU.put("saddle", "село");
        EN_TO_RU.put("bread", "хлеб");
        EN_TO_RU.put("carrot", "орковь");
        EN_TO_RU.put("golden carrot", "золотая орковь");
        EN_TO_RU.put("potato", "картофель");
        EN_TO_RU.put("baked potato", "печный картофель");
        EN_TO_RU.put("steak", "стейк");
        EN_TO_RU.put("cooked beef", "жареная говяна");
        EN_TO_RU.put("cooked porkchop", "жареная свнна");
        EN_TO_RU.put("cooked chicken", "жареная курятна");
        EN_TO_RU.put("coal", "уголь");
        EN_TO_RU.put("charcoal", "ревесный уголь");
        EN_TO_RU.put("emerald", "зуру");
        EN_TO_RU.put("redstone", "красная пыль");
        EN_TO_RU.put("lapis lazuli", "лазурт");
        EN_TO_RU.put("quartz", "кварц");
        EN_TO_RU.put("ingot", "слток");
        EN_TO_RU.put("nugget", "саороок");
        EN_TO_RU.put("block", "блок");
        EN_TO_RU.put("ore", "руа");
        EN_TO_RU.put("chest", "сунук");
        EN_TO_RU.put("ender chest", "энер-сунук");
        EN_TO_RU.put("shulker box", "ящк шалкера");
        EN_TO_RU.put("beacon", "аяк");
        EN_TO_RU.put("anvil", "наковальня");
        EN_TO_RU.put("enchanting table", "стол зачарованя");
        EN_TO_RU.put("brewing stand", "варочная стойка");
        EN_TO_RU.put("crafting table", "верстак");
        EN_TO_RU.put("furnace", "печь");
        EN_TO_RU.put("hopper", "воронка");
        EN_TO_RU.put("dispenser", "разатчк");
        EN_TO_RU.put("dropper", "выбрасыватель");
        EN_TO_RU.put("observer", "наблюатель");
        EN_TO_RU.put("piston", "поршень");
        EN_TO_RU.put("sticky piston", "лпкй поршень");
        EN_TO_RU.put("tnt", "нат");
        EN_TO_RU.put("obsidian", "обсан");
        EN_TO_RU.put("crying obsidian", "плачущй обсан");
        EN_TO_RU.put("netherrack", "незерак");
        EN_TO_RU.put("end stone", "энерняк");
        EN_TO_RU.put("glowstone", "светокаень");
        EN_TO_RU.put("nether star", "звеза незера");
        EN_TO_RU.put("dragon egg", "яйцо ракона");
        EN_TO_RU.put("end crystal", "крсталл Эна");
        EN_TO_RU.put("firework rocket", "фейерверк");
        EN_TO_RU.put("snowball", "снежок");
        EN_TO_RU.put("egg", "яйцо");
        EN_TO_RU.put("string", "нть");
        EN_TO_RU.put("feather", "перо");
        EN_TO_RU.put("gunpowder", "порох");
        EN_TO_RU.put("bone", "кость");
        EN_TO_RU.put("slime ball", "слзь");
        EN_TO_RU.put("blaze rod", "стержень фрта");
        EN_TO_RU.put("blaze powder", "огненный порошок");
        EN_TO_RU.put("ghast tear", "слеза гаста");
        EN_TO_RU.put("magma cream", "лавовый кре");
        EN_TO_RU.put("phantom membrane", "ебрана фантоа");
        EN_TO_RU.put("turtle helmet", "черепашй панцрь");
        EN_TO_RU.put("respawn anchor", "якорь возроженя");
        EN_TO_RU.put("lodestone", "агнетт");
        EN_TO_RU.put("compass", "копас");
        EN_TO_RU.put("clock", "часы");
        EN_TO_RU.put("map", "карта");
        EN_TO_RU.put("spyglass", "позорная труба");
        EN_TO_RU.put("minecart", "вагонетка");
        EN_TO_RU.put("boat", "лока");
        EN_TO_RU.put("rail", "рельсы");
        EN_TO_RU.put("torch", "факел");
        EN_TO_RU.put("lantern", "фонарь");
        EN_TO_RU.put("campfire", "костр");
        EN_TO_RU.put("bed", "кровать");
        EN_TO_RU.put("cake", "торт");
        EN_TO_RU.put("cookie", "печенье");
        EN_TO_RU.put("pumpkin pie", "тыквенный прог");
        EN_TO_RU.put("melon", "арбуз");
        EN_TO_RU.put("pumpkin", "тыква");
        EN_TO_RU.put("honey", "");
        EN_TO_RU.put("honeycomb", "соты");
        EN_TO_RU.put("wheat", "пшенца");
        EN_TO_RU.put("sugar", "сахар");
        EN_TO_RU.put("paper", "буага");
        EN_TO_RU.put("stick", "палка");
        EN_TO_RU.put("glass", "стекло");
        EN_TO_RU.put("cobblestone", "булыжнк");
        EN_TO_RU.put("dirt", "зеля");
        EN_TO_RU.put("sand", "песок");
        EN_TO_RU.put("gravel", "гравй");
        EN_TO_RU.put("clay", "глна");
        EN_TO_RU.put("brick", "крпч");
        EN_TO_RU.put("oak", "убовый");
        EN_TO_RU.put("spruce", "еловый");
        EN_TO_RU.put("birch", "берзовый");
        EN_TO_RU.put("jungle", "тропческй");
        EN_TO_RU.put("acacia", "акацевый");
        EN_TO_RU.put("dark oak", "з тного уба");
        EN_TO_RU.put("crimson", "багровый");
        EN_TO_RU.put("warped", "скажнный");
        EN_TO_RU.put("planks", "оск");
        EN_TO_RU.put("log", "бревно");
        EN_TO_RU.put("slab", "плта");
        EN_TO_RU.put("stairs", "ступеньк");
        EN_TO_RU.put("fence", "забор");
        EN_TO_RU.put("door", "верь");
        EN_TO_RU.put("trapdoor", "люк");
        EN_TO_RU.put("button", "кнопка");
        EN_TO_RU.put("pressure plate", "нажная плта");
        EN_TO_RU.put("lever", "рычаг");
        EN_TO_RU.put("sign", "таблчка");
        EN_TO_RU.put("white", "белый");
        EN_TO_RU.put("orange", "оранжевый");
        EN_TO_RU.put("magenta", "пурпурный");
        EN_TO_RU.put("light blue", "голубой");
        EN_TO_RU.put("yellow", "жлтый");
        EN_TO_RU.put("lime", "лайовый");
        EN_TO_RU.put("pink", "розовый");
        EN_TO_RU.put("gray", "серый");
        EN_TO_RU.put("light gray", "светло-серый");
        EN_TO_RU.put("cyan", "брюзовый");
        EN_TO_RU.put("purple", "фолетовый");
        EN_TO_RU.put("blue", "снй");
        EN_TO_RU.put("brown", "корчневый");
        EN_TO_RU.put("green", "зелный");
        EN_TO_RU.put("red", "красный");
        EN_TO_RU.put("black", "чрный");
        EN_TO_RU.put("wool", "шерсть");
        EN_TO_RU.put("carpet", "ковр");
        EN_TO_RU.put("terracotta", "терракота");
        EN_TO_RU.put("concrete", "бетон");
        EN_TO_RU.put("dye", "крастель");
    }

    public AhHelper() {
        this.addSettings(new Setting[]{this.three, this.ahSearchKey});
    }

    @Subscribe
    public void onUpdate(EventUpdate update) {
        Screen var3 = mc.currentScreen;
        if (var3 instanceof ChestScreen e) {
            String title = e.getTitle().getString().toLowerCase();
            if (!title.contains("аукцион") && !title.contains("аукционы") && !title.contains("поиск") && !title.contains("auction")) {
                this.resetHighlights();
            } else {
                Container container = e.getContainer();
                Slot slot1 = null;
                Slot slot2 = null;
                Slot slot3 = null;
                int fsPrice = Integer.MAX_VALUE;
                int medPrice = Integer.MAX_VALUE;
                int thPrice = Integer.MAX_VALUE;

                for (Slot slot : container.inventorySlots) {
                    if (slot.slotNumber <= 44) {
                        int currentPrice = this.extractPriceFromStack(slot.getStack());
                        if (currentPrice != -1 && currentPrice < fsPrice) {
                            thPrice = medPrice;
                            slot3 = slot2;
                            medPrice = fsPrice;
                            slot2 = slot1;
                            fsPrice = currentPrice;
                            slot1 = slot;
                        } else if (this.three.get() && currentPrice != -1 && currentPrice < medPrice && currentPrice > fsPrice) {
                            thPrice = medPrice;
                            slot3 = slot2;
                            medPrice = currentPrice;
                            slot2 = slot;
                        } else if (this.three.get() && currentPrice != -1 && currentPrice < thPrice && currentPrice > medPrice) {
                            thPrice = currentPrice;
                            slot3 = slot;
                        }
                    }
                }

                this.updateSlotPositions(slot1, slot2, slot3);
            }
        } else {
            this.resetHighlights();
        }
    }

    @Subscribe
    public void onKey(EventKey e) {
        if (e.getKey() == this.ahSearchKey.get()) {
            if (mc.player == null) return;

            ItemStack heldItem = mc.player.getHeldItemMainhand();
            if (!heldItem.isEmpty()) {
                String itemName = getItemName(heldItem);

                if (itemName != null && !itemName.isEmpty()) {
                    mc.player.sendChatMessage("/ah search " + itemName);
                } else {
                    print(TextFormatting.GREEN + "Название предмета пустое!");
                }
            } else {
                print(TextFormatting.GREEN + "В руке ничего нет!");
            }
        }
    }

    private String getItemName(ItemStack stack) {
        String name;

        if (stack.hasDisplayName()) {
            name = stack.getDisplayName().getString();
            name = cleanName(name);
            if (isEnglish(name)) {
                name = translateToRussian(name);
            }
        } else {
            ITextComponent translatedName = stack.getItem().getName();
            name = translatedName.getString();
            name = cleanName(name);
            if (isEnglish(name)) {
                name = translateToRussian(name);
            }
        }

        return name;
    }

    private String cleanName(String name) {
        if (name == null) return null;

        name = TextFormatting.getTextWithoutFormattingCodes(name);
        if (name == null) return null;

        name = name.replaceAll("(?i)[\\u00A7&][0-9a-fk-or]", "");
        name = name.replaceAll("[^\\p{IsLatin}\\p{IsCyrillic}0-9\\s\\-\']", "");
        name = name.replaceAll("\\s+", " ");
        name = name.trim();

        return name;
    }

    private boolean isEnglish(String text) {
        if (text == null || text.isEmpty()) return false;

        boolean hasLatin = false;
        for (char c : text.toCharArray()) {
            if (isCyrillicChar(c)) {
                return false;
            }
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                hasLatin = true;
            }
        }
        return hasLatin;
    }

    private boolean isCyrillicChar(char c) {
        return (c >= 0x0400 && c <= 0x052F)
                || (c >= 0x2DE0 && c <= 0x2DFF)
                || (c >= 0xA640 && c <= 0xA69F)
                || c == 0x1C80;
    }

    private String translateToRussian(String englishName) {
        if (englishName == null) return null;

        String lower = englishName.toLowerCase().trim();

        if (EN_TO_RU.containsKey(lower)) {
            return EN_TO_RU.get(lower);
        }

        String result = lower;
        for (Map.Entry<String, String> entry : EN_TO_RU.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }

        if (!result.equals(lower)) {
            return result.trim();
        }

        return englishName;
    }

    private void resetHighlights() {
        this.setX(0.0F);
        this.setY(0.0F);
        this.setX2(0.0F);
        this.setY2(0.0F);
        this.setX3(0.0F);
        this.setY3(0.0F);
    }

    private void updateSlotPositions(Slot slot1, Slot slot2, Slot slot3) {
        if (slot1 != null) {
            this.setX((float)slot1.xPos);
            this.setY((float)slot1.yPos);
        } else {
            this.setX(0.0F);
            this.setY(0.0F);
        }

        if (slot2 != null && this.three.get()) {
            this.setX2((float)slot2.xPos);
            this.setY2((float)slot2.yPos);
        } else {
            this.setX2(0.0F);
            this.setY2(0.0F);
        }

        if (slot3 != null && this.three.get()) {
            this.setX3((float)slot3.xPos);
            this.setY3((float)slot3.yPos);
        } else {
            this.setX3(0.0F);
            this.setY3(0.0F);
        }
    }

    protected int extractPriceFromStack(ItemStack stack) {
        CompoundNBT tag = stack.getTag();
        if (tag != null && tag.contains("display", 10)) {
            CompoundNBT display = tag.getCompound("display");
            if (display.contains("Lore", 9)) {
                ListNBT lore = display.getList("Lore", 8);

                for (int j = 0; j < lore.size(); ++j) {
                    String line = lore.getString(j);

                    try {
                        JsonObject object = new JsonParser().parse(line).getAsJsonObject();
                        if (object.has("extra")) {
                            JsonArray array = object.getAsJsonArray("extra");

                            for (int i = 0; i < array.size(); ++i) {
                                String text = array.get(i).getAsJsonObject().get("text").getAsString().toLowerCase();
                                if (text.contains("цена") || text.contains("price") || text.contains("cost") || text.contains("bid")) {
                                    if (i + 1 < array.size()) {
                                        String priceStr = array.get(i + 1).getAsJsonObject().get("text").getAsString().replaceAll("[^\\d]", "");
                                        if (!priceStr.isEmpty()) {
                                            return Integer.parseInt(priceStr);
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception ex) {
                        String stripped = line.replaceAll("(?i)[\u00A7&].", "").toLowerCase();
                        if (stripped.contains("цена") || stripped.contains("price") || stripped.contains("cost") || stripped.contains("bid")) {
                            String[] parts = stripped.split(":");
                            if (parts.length > 1) {
                                try {
                                    String priceStr = parts[1].replaceAll("[^\\d]", "");
                                    if (!priceStr.isEmpty()) {
                                        return Integer.parseInt(priceStr);
                                    }
                                } catch (NumberFormatException ignored) {
                                }
                            }
                        }
                    }
                }
            }
        }
        return -1;
    }
}
