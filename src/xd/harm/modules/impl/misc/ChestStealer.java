package xd.harm.modules.impl.misc;

import com.google.common.eventbus.Subscribe;

import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.ModeListSetting;
import xd.harm.modules.settings.impl.SliderSetting;
import xd.harm.utils.math.StopWatch;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.ChestContainer;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@FieldDefaults(level = AccessLevel.PRIVATE)
@ModuleRegister(name = "ChestStealer", category = Category.Misc, desc = "Автоматически забирает предметы с сундука")
public class ChestStealer extends Module {

    private final BooleanSetting chestClose = new BooleanSetting("Закрывать при пустом", false);
    private final SliderSetting stealDelay = new SliderSetting("Задержка", 100, 0, 1000, 1);
    private final BooleanSetting filterLootToggle = new BooleanSetting("Фильтр лута", false);
    private final ModeListSetting filterLoot = new ModeListSetting("Лут",
            new BooleanSetting("Руды", true),
            new BooleanSetting("Головы", false),
            new BooleanSetting("Незеритовый слиток", false),
            new BooleanSetting("Зачарованная книга", false),
            new BooleanSetting("Тотемы", false),
            new BooleanSetting("Зелья", false)
    ).setVisible(filterLootToggle::get);
    private final SliderSetting itemLimit = new SliderSetting("Лимит кол", 12, 1, 64, 1);
    private final SliderSetting missPercent = new SliderSetting("Миссать", 50, 0, 100, 1);
    private final StopWatch timerUtil = new StopWatch();

    public ChestStealer() {
        addSettings(chestClose, stealDelay, filterLootToggle, filterLoot, itemLimit, missPercent);
    }

    private boolean filterItem(Item item) {
        if (!filterLootToggle.get()) {
            return true;
        }

        boolean filterOres = filterLoot.get(0).get();
        boolean filterHeads = filterLoot.get(1).get();
        boolean filterNetherite = filterLoot.get(2).get();
        boolean filterEnchantedBooks = filterLoot.get(3).get();
        boolean filterTotems = filterLoot.get(4).get();
        boolean filterPotions = filterLoot.get(5).get();

        if (filterOres && (
                item == Items.DIAMOND_ORE ||
                        item == Items.EMERALD_ORE ||
                        item == Items.IRON_ORE ||
                        item == Items.GOLD_ORE ||
                        item == Items.COAL_ORE
        )) {
            return true;
        }


        if (filterHeads && item == Items.PLAYER_HEAD) {
            return true;
        }

        if (filterNetherite && item == Items.NETHERITE_INGOT) {
            return true;
        }

        if (filterEnchantedBooks && item == Items.ENCHANTED_BOOK) {
            return true;
        }

        if (filterTotems && item == Items.TOTEM_OF_UNDYING) {
            return true;
        }

        if (filterPotions && (
                item == Items.POTION ||
                        item == Items.SPLASH_POTION
        )) {
            return true;
        }

        return false;
    }

    @Subscribe
    public void onEvent(final EventUpdate event) {
        if (mc.player.openContainer instanceof ChestContainer) {
            ChestContainer container = (ChestContainer) mc.player.openContainer;
            IInventory inventory = container.getLowerChestInventory();
            List<Integer> validSlots = new ArrayList<>();

            for (int i = 0; i < inventory.getSizeInventory(); i++) {
                if (inventory.getStackInSlot(i).getItem() != Item.getItemById(0)
                        && inventory.getStackInSlot(i).getCount() <= itemLimit.get()
                        && filterItem(inventory.getStackInSlot(i).getItem())) {
                    validSlots.add(i);
                }
            }

            if (!validSlots.isEmpty() && timerUtil.isReached(Math.round(stealDelay.get()))) {
                int randomIndex = new Random().nextInt(validSlots.size());
                int slotToSteal = validSlots.get(randomIndex);

                if (new Random().nextInt(100) >= missPercent.get()) {
                    mc.playerController.windowClick(container.windowId, slotToSteal, 0, ClickType.QUICK_MOVE, mc.player);
                }

                timerUtil.reset();
            }

            if (inventory.isEmpty() && chestClose.get()) {
                mc.player.closeScreen();
            }
        }
    }
}
