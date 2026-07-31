package xd.harm.modules.impl.player;

import com.google.common.eventbus.Subscribe;
import xd.harm.Harmony;
import xd.harm.modules.impl.combat.HitAura;
import xd.harm.modules.impl.movement.Scaffold;
import xd.harm.events.input.EventInput;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.ModeListSetting;
import xd.harm.modules.settings.impl.ModeSetting;
import xd.harm.modules.settings.impl.SliderSetting;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.*;

import java.util.*;

@ModuleRegister(name = "InvManager", category = Category.Player, desc = "Управление инвентарём — сортировка, выброс мусора, авто-броня")
public class InvManager extends Module {

    public final ModeSetting invMode = new ModeSetting("Мод", "Default", "Default", "Only Inventory");
    public final ModeSetting mode = new ModeSetting("Режим", "Мусор", "Мусор", "Сортировка", "Оба");
    private final SliderSetting minDelay = new SliderSetting("Мин. задержка (мс)", 50F, 0F, 500F, 10F);
    private final SliderSetting maxDelay = new SliderSetting("Макс. задержка (мс)", 150F, 0F, 500F, 10F);
    private final SliderSetting simulateDelay = new SliderSetting("Имитация задержки (мс)", 0F, 0F, 300F, 10F);
    public final BooleanSetting autoArmor = new BooleanSetting("Авто-броня", true);
    public final BooleanSetting autoArmorHotbar = new BooleanSetting("Авто-броня HotBar", false);
    public final BooleanSetting slowdown = new BooleanSetting("Замедление", false);
    public final BooleanSetting ignoreScaffold = new BooleanSetting("Ignore Scaffold", true);
    public final BooleanSetting ignoreHitAura = new BooleanSetting("Ignore HitAura", true);
    public final BooleanSetting dropStack = new BooleanSetting("Выброс по штучно", true);
    public final BooleanSetting garbageHotbar = new BooleanSetting("Выбрасывать HotBar", false);
    public final ModeSetting blockOrder = new ModeSetting("Раскладка блоков", "Уменьшение", "Увеличение", "Уменьшение");

    public final ModeListSetting garbageItems = new ModeListSetting("Выбрасывать",
            new BooleanSetting("Дерево", true),
            new BooleanSetting("Камень", true),
            new BooleanSetting("Глина", true),
            new BooleanSetting("Песок", true),
            new BooleanSetting("Стекло", true),
            new BooleanSetting("Бумага", true),
            new BooleanSetting("Рыба", false),
            new BooleanSetting("Палки", true),
            new BooleanSetting("Перо", true),
            new BooleanSetting("Краска", true),
            new BooleanSetting("Снежок", true),
            new BooleanSetting("Кости", false),
            new BooleanSetting("Компас", true),
            new BooleanSetting("Карта", true),
            new BooleanSetting("Стрелы", false)
    );

    private static final String SLOT_OPTIONS = "Меч,Лучший предмет,Кирка,Топор,Блоки,Еда,Ничего";

    public final ModeSetting sortSlot1 = new ModeSetting("Слот 1", "Меч", SLOT_OPTIONS.split(","));
    public final ModeSetting sortSlot2 = new ModeSetting("Слот 2", "Лучший предмет", SLOT_OPTIONS.split(","));
    public final ModeSetting sortSlot3 = new ModeSetting("Слот 3", "Кирка", SLOT_OPTIONS.split(","));
    public final ModeSetting sortSlot4 = new ModeSetting("Слот 4", "Топор", SLOT_OPTIONS.split(","));
    public final ModeSetting sortSlot5 = new ModeSetting("Слот 5", "Блоки", SLOT_OPTIONS.split(","));
    public final ModeSetting sortSlot6 = new ModeSetting("Слот 6", "Блоки", SLOT_OPTIONS.split(","));
    public final ModeSetting sortSlot7 = new ModeSetting("Слот 7", "Еда", SLOT_OPTIONS.split(","));
    public final ModeSetting sortSlot8 = new ModeSetting("Слот 8", "Ничего", SLOT_OPTIONS.split(","));
    public final ModeSetting sortSlot9 = new ModeSetting("Слот 9", "Ничего", SLOT_OPTIONS.split(","));

    private long lastAction = 0;
    private final Random random = new Random();
    private long freezeUntil = 0;

    public InvManager() {
        addSettings(invMode, mode, minDelay, maxDelay, simulateDelay, autoArmor, autoArmorHotbar,
                slowdown, ignoreScaffold, ignoreHitAura, dropStack, garbageHotbar, blockOrder, garbageItems, sortSlot1, sortSlot2, sortSlot3, sortSlot4, sortSlot5,
                sortSlot6, sortSlot7, sortSlot8, sortSlot9);
    }

    private static final Set<Item> WOOD_LOGS = new HashSet<>(Arrays.asList(
        Items.OAK_LOG, Items.SPRUCE_LOG, Items.BIRCH_LOG, Items.JUNGLE_LOG,
        Items.ACACIA_LOG, Items.DARK_OAK_LOG
    ));

    private static final Set<Item> FISH_ITEMS = new HashSet<>(Arrays.asList(
        Items.COD, Items.SALMON, Items.TROPICAL_FISH, Items.PUFFERFISH
    ));

    private boolean pendingOperation = false;

    @Subscribe
    public void onUpdate(EventUpdate e) {
        if (mc.player == null || mc.world == null) return;

        var moduleManager = Harmony.getInstance().getModuleManager();
        if (ignoreScaffold.get() && moduleManager.getScaffold().isState()) { toggle(); return; }
        if (ignoreHitAura.get() && moduleManager.getHitAura().isState()) { toggle(); return; }

        long now = System.currentTimeMillis();
        float simD = simulateDelay.getFloat();
        boolean doSlowdown = slowdown.get() && "Default".equals(invMode.get());

        // Ждём пока onInput остановит движение (freeze активен)
        if (doSlowdown && now < freezeUntil) {
            // Делаем операцию ТОЛЬКО если pendingOperation установлен
            // и задержка прошла
            if (pendingOperation) {
                float minD = minDelay.getFloat();
                float maxD = maxDelay.getFloat();
                long delay = (long) minD + (long) (random.nextDouble() * (maxD - minD));
                if (now - lastAction < delay) return;

                boolean screenOpen = mc.currentScreen != null;
                boolean onlyInv = "Only Inventory".equals(invMode.get());
                if (onlyInv ? !screenOpen : screenOpen) return;

                String modeVal = mode.get();
                boolean didWork = false;

                if ("Мусор".equals(modeVal) || "Оба".equals(modeVal)) {
                    didWork = handleDropGarbageExecute();
                }
                if (!didWork && ("Сортировка".equals(modeVal) || "Оба".equals(modeVal))) {
                    didWork = handleSortHotbarExecute();
                }
                if (!didWork && autoArmor.get()) {
                    didWork = handleAutoArmorExecute();
                }
                if (!didWork && ("Сортировка".equals(modeVal) || "Оба".equals(modeVal))) {
                    didWork = reorderBlocks();
                }

                pendingOperation = false;
                if (didWork) lastAction = now + (long) simD;
            }
            return;
        }

        // Обычный путь (без замедления, или freeze уже прошёл)
        float minD = minDelay.getFloat();
        float maxD = maxDelay.getFloat();
        long delay = (long) minD + (long) (random.nextDouble() * (maxD - minD));
        if (now - lastAction < delay) return;

        boolean screenOpen = mc.currentScreen != null;
        boolean onlyInv = "Only Inventory".equals(invMode.get());
        if (onlyInv ? !screenOpen : screenOpen) return;

        String modeVal = mode.get();

        if (doSlowdown) {
            // Проверяем, есть ли работа. Если да — запускаем freeze
            if (hasWork(modeVal)) {
                freezeUntil = now + 200;
                pendingOperation = true;
                return;
            }
            return;
        }

        if ("Мусор".equals(modeVal) || "Оба".equals(modeVal)) {
            if (handleDropGarbageExecute()) {
                lastAction = now + (long) simD;
                return;
            }
        }

        if ("Сортировка".equals(modeVal) || "Оба".equals(modeVal)) {
            if (handleSortHotbarExecute()) {
                lastAction = now + (long) simD;
                return;
            }
        }

        if (autoArmor.get() && handleAutoArmorExecute()) {
            lastAction = now + (long) simD;
            return;
        }

        // Переупорядочивание блоков в слотах (по одному свапу за тик)
        if (("Сортировка".equals(modeVal) || "Оба".equals(modeVal))) {
            if (reorderBlocks()) {
                lastAction = now + (long) simD;
                return;
            }
        }
    }

    private boolean hasWork(String modeVal) {
        if ("Мусор".equals(modeVal) || "Оба".equals(modeVal)) {
            if (handleDropGarbageCheck()) return true;
        }
        if ("Сортировка".equals(modeVal) || "Оба".equals(modeVal)) {
            if (handleSortHotbarCheck()) return true;
            if (reorderBlocksCheck()) return true;
        }
        if (autoArmor.get() && handleAutoArmorCheck()) return true;
        return false;
    }

    @Subscribe
    public void onInput(EventInput event) {
        if (System.currentTimeMillis() < freezeUntil) {
            event.setForward(0.0F);
            event.setStrafe(0.0F);
            event.setSprintState(false);
        }
    }

    private boolean handleAutoArmorCheck() {
        PlayerEntity player = mc.player;
        int searchStart = autoArmorHotbar.get() ? 0 : 9;
        for (int armorSlot = 0; armorSlot < 4; armorSlot++) {
            int currentProt = getArmorProtection(player.inventory.getStackInSlot(36 + armorSlot));
            for (int i = searchStart; i < 36; i++) {
                ItemStack stack = player.inventory.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() instanceof ArmorItem) {
                    ArmorItem armor = (ArmorItem) stack.getItem();
                    int slot = armor.getEquipmentSlot().getIndex();
                    if (slot == armorSlot && getArmorProtection(stack) > currentProt) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean handleAutoArmorExecute() {
        PlayerEntity player = mc.player;
        int searchStart = autoArmorHotbar.get() ? 0 : 9;
        for (int armorSlot = 0; armorSlot < 4; armorSlot++) {
            int bestSlot = -1;
            int bestProtection = getArmorProtection(player.inventory.getStackInSlot(36 + armorSlot));
            for (int i = searchStart; i < 36; i++) {
                ItemStack stack = player.inventory.getStackInSlot(i);
                if (!stack.isEmpty() && stack.getItem() instanceof ArmorItem) {
                    ArmorItem armor = (ArmorItem) stack.getItem();
                    int slot = armor.getEquipmentSlot().getIndex();
                    if (slot == armorSlot) {
                        int prot = getArmorProtection(stack);
                        if (prot > bestProtection) {
                            bestProtection = prot;
                            bestSlot = i;
                        }
                    }
                }
            }
            if (bestSlot != -1) {
                int containerSlot = bestSlot < 9 ? bestSlot + 36 : bestSlot;
                mc.playerController.windowClick(0, containerSlot, 0, ClickType.QUICK_MOVE, player);
                return true;
            }
        }
        return false;
    }

    private int getArmorProtection(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ArmorItem)) return -1;
        Item item = stack.getItem();
        int base = 0;
        if (item == Items.NETHERITE_CHESTPLATE || item == Items.NETHERITE_LEGGINGS || item == Items.NETHERITE_HELMET || item == Items.NETHERITE_BOOTS) base = 4;
        else if (item == Items.DIAMOND_CHESTPLATE || item == Items.DIAMOND_LEGGINGS || item == Items.DIAMOND_HELMET || item == Items.DIAMOND_BOOTS) base = 3;
        else if (item == Items.IRON_CHESTPLATE || item == Items.IRON_LEGGINGS || item == Items.IRON_HELMET || item == Items.IRON_BOOTS) base = 2;
        else if (item == Items.CHAINMAIL_CHESTPLATE || item == Items.CHAINMAIL_LEGGINGS || item == Items.CHAINMAIL_HELMET || item == Items.CHAINMAIL_BOOTS) base = 1;
        if (stack.isEnchanted()) base += 2;
        return base;
    }

    private boolean handleDropGarbageCheck() {
        PlayerEntity player = mc.player;
        int searchStart = garbageHotbar.get() ? 0 : 9;
        for (int i = searchStart; i < 36; i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && isGarbage(stack)) return true;
        }
        return false;
    }

    private boolean handleDropGarbageExecute() {
        PlayerEntity player = mc.player;
        int searchStart = garbageHotbar.get() ? 0 : 9;
        int throwButton = dropStack.get() ? 0 : 1;
        for (int i = searchStart; i < 36; i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && isGarbage(stack)) {
                int containerSlot = i < 9 ? i + 36 : i;
                mc.playerController.windowClick(0, containerSlot, throwButton, ClickType.THROW, player);
                return true;
            }
        }
        return false;
    }

    private boolean isGarbage(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof SwordItem || item instanceof BowItem || item instanceof CrossbowItem) return false;
        if (item instanceof ArmorItem) return false;
        if (item instanceof PickaxeItem || item instanceof AxeItem || item instanceof ShovelItem) return false;
        if (item == Items.TOTEM_OF_UNDYING || item == Items.ENDER_PEARL || item == Items.ENDER_EYE) return false;
        if (item == Items.OBSIDIAN || item == Items.DIAMOND || item == Items.EMERALD) return false;
        if (item == Items.GOLD_INGOT || item == Items.IRON_INGOT) return false;
        if (item == Items.ELYTRA) return false;

        if (WOOD_LOGS.contains(item)) return garbageItems.getValueByName("Дерево").getBool();
        if (item == Items.COBBLESTONE || item == Items.STONE) return garbageItems.getValueByName("Камень").getBool();
        if (item == Items.CLAY_BALL) return garbageItems.getValueByName("Глина").getBool();
        if (item == Items.SAND || item == Items.RED_SAND) return garbageItems.getValueByName("Песок").getBool();
        if (item == Items.GLASS) return garbageItems.getValueByName("Стекло").getBool();
        if (item == Items.PAPER) return garbageItems.getValueByName("Бумага").getBool();
        if (FISH_ITEMS.contains(item)) return garbageItems.getValueByName("Рыба").getBool();
        if (item == Items.STICK) return garbageItems.getValueByName("Палки").getBool();
        if (item == Items.FEATHER) return garbageItems.getValueByName("Перо").getBool();
        if (item instanceof DyeItem) return garbageItems.getValueByName("Краска").getBool();
        if (item == Items.SNOWBALL) return garbageItems.getValueByName("Снежок").getBool();
        if (item == Items.BONE) return garbageItems.getValueByName("Кости").getBool();
        if (item == Items.COMPASS) return garbageItems.getValueByName("Компас").getBool();
        if (item == Items.MAP || item == Items.FILLED_MAP) return garbageItems.getValueByName("Карта").getBool();
        if (item == Items.ARROW) return garbageItems.getValueByName("Стрелы").getBool();
        return false;
    }

    private boolean handleSortHotbarCheck() {
        ModeSetting[] slots = {sortSlot1, sortSlot2, sortSlot3, sortSlot4, sortSlot5, sortSlot6, sortSlot7, sortSlot8, sortSlot9};
        for (int targetSlot = 0; targetSlot < 9; targetSlot++) {
            String desired = slots[targetSlot].get();
            if ("Ничего".equals(desired)) continue;
            ItemStack currentItem = mc.player.inventory.getStackInSlot(targetSlot);
            if (matchesType(currentItem, desired)) continue;
            int foundSlot = findItemForSlot(desired, targetSlot, slots);
            if (foundSlot != -1 && foundSlot != targetSlot) return true;
        }
        return false;
    }

    private boolean handleSortHotbarExecute() {
        ModeSetting[] slots = {sortSlot1, sortSlot2, sortSlot3, sortSlot4, sortSlot5, sortSlot6, sortSlot7, sortSlot8, sortSlot9};
        for (int targetSlot = 0; targetSlot < 9; targetSlot++) {
            String desired = slots[targetSlot].get();
            if ("Ничего".equals(desired)) continue;

            ItemStack currentItem = mc.player.inventory.getStackInSlot(targetSlot);
            if (matchesType(currentItem, desired)) continue;

            int foundSlot = findItemForSlot(desired, targetSlot, slots);
            if (foundSlot != -1 && foundSlot != targetSlot) {
                int containerSlot = foundSlot < 9 ? foundSlot + 36 : foundSlot;
                mc.playerController.windowClick(0, containerSlot, targetSlot, ClickType.SWAP, mc.player);
                return true;
            }
        }
        return false;
    }

    private boolean reorderBlocksCheck() {
        boolean ascending = "Увеличение".equals(blockOrder.get());
        for (int i = 0; i < 8; i++) {
            ItemStack a = mc.player.inventory.getStackInSlot(i);
            ItemStack b = mc.player.inventory.getStackInSlot(i + 1);
            if (a.isEmpty() || b.isEmpty()) continue;
            if (!(a.getItem() instanceof BlockItem) || !(b.getItem() instanceof BlockItem)) continue;
            boolean wrongOrder = ascending ? a.getCount() > b.getCount() : a.getCount() < b.getCount();
            if (wrongOrder) return true;
        }
        return false;
    }

    private boolean reorderBlocks() {
        boolean ascending = "Увеличение".equals(blockOrder.get());
        for (int i = 0; i < 8; i++) {
            ItemStack a = mc.player.inventory.getStackInSlot(i);
            ItemStack b = mc.player.inventory.getStackInSlot(i + 1);
            if (a.isEmpty() || b.isEmpty()) continue;
            if (!(a.getItem() instanceof BlockItem) || !(b.getItem() instanceof BlockItem)) continue;
            boolean wrongOrder = ascending ? a.getCount() > b.getCount() : a.getCount() < b.getCount();
            if (wrongOrder) {
                mc.playerController.windowClick(0, i + 36, i + 1, ClickType.SWAP, mc.player);
                return true;
            }
        }
        return false;
    }

    private int findItemForSlot(String type, int currentSlot, ModeSetting[] slots) {
        PlayerEntity player = mc.player;
        int bestSlot = -1;
        int bestScore = -1;

        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            if (!matchesType(stack, type)) continue;

            if (i < 9 && i != currentSlot) {
                String slotDesired = slots[i].get();
                if (type.equals(slotDesired)) continue;
            }

            int score = getItemScore(stack);
            if (score > bestScore) {
                bestScore = score;
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    private boolean matchesType(ItemStack stack, String type) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        switch (type) {
            case "Меч": return item instanceof SwordItem;
            case "Кирка": return item instanceof PickaxeItem;
            case "Топор": return item instanceof AxeItem;
            case "Лук": return item instanceof BowItem || item instanceof CrossbowItem;
            case "Блоки": return item instanceof BlockItem;
            case "Еда": return item.isFood();
            case "Лучший предмет": return true;
            default: return false;
        }
    }

    private int getItemScore(ItemStack stack) {
        Item item = stack.getItem();
        int score = 10;
        if (item == Items.NETHERITE_SWORD || item == Items.NETHERITE_AXE || item == Items.NETHERITE_PICKAXE) score = 100;
        else if (item == Items.DIAMOND_SWORD || item == Items.DIAMOND_AXE || item == Items.DIAMOND_PICKAXE) score = 80;
        else if (item == Items.IRON_SWORD || item == Items.IRON_AXE || item == Items.IRON_PICKAXE) score = 60;
        else if (item == Items.STONE_SWORD || item == Items.STONE_AXE || item == Items.STONE_PICKAXE) score = 40;
        else if (item == Items.WOODEN_SWORD || item == Items.WOODEN_AXE || item == Items.WOODEN_PICKAXE) score = 20;
        if (stack.isEnchanted()) score += 30;
        if (stack.isDamaged()) score -= stack.getDamage();
        return score;
    }
}
