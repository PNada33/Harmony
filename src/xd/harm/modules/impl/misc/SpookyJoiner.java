/*
 * Decompiled with CFR 0.153-SNAPSHOT (d6f6758-dirty).
 */
package xd.harm.modules.impl.misc;

import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import com.google.common.eventbus.Subscribe;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.inventory.ChestScreen;
import net.minecraft.inventory.container.ChestContainer;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.play.client.CClickWindowPacket;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.network.play.server.SChatPacket;
import net.minecraft.util.Hand;
import xd.harm.events.network.EventPacket;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Category;
import xd.harm.utils.player.InventoryUtil;
import java.util.Locale;

@ModuleRegister(name = "SpookyJoiner", category = Category.Misc, desc = "\u0411\u044b\u0441\u0442\u0440\u043e \u0437\u0430\u0445\u043e\u0434\u0438\u0442 \u043d\u0430 \u0434\u0443\u044d\u043b\u0438 SpookyTime")
public class SpookyJoiner extends Module {
    public static void selectCompass() {
        int n = InventoryUtil.getHotBarSlot(Items.COMPASS);
        if (n == -1) {
            return;
        }
        SpookyJoiner.mc.player.inventory.currentItem = n;
        SpookyJoiner.mc.player.connection.sendPacket(new CHeldItemChangePacket(n));
        SpookyJoiner.mc.playerController.processRightClick(SpookyJoiner.mc.player, SpookyJoiner.mc.world, Hand.MAIN_HAND);
    }

    @Subscribe
    private void onUpdate(EventUpdate eventUpdate) {
        Screen screen = SpookyJoiner.mc.currentScreen;
        if (screen instanceof ChestScreen) {
            ChestScreen chestScreen = (ChestScreen)screen;
            for (int i = 0; i < ((ChestContainer)chestScreen.getContainer()).inventorySlots.size(); ++i) {
                if (((ChestContainer)chestScreen.getContainer()).getSlot(i).getStack().getItem() != Items.RESPAWN_ANCHOR) continue;
                ItemStack itemStack = ((ChestContainer)chestScreen.getContainer()).getSlot(i).getStack();
                SpookyJoiner.mc.player.connection.sendPacket(new CClickWindowPacket(((ChestContainer)chestScreen.getContainer()).windowId, i, 0, ClickType.PICKUP, itemStack, ((ChestContainer)chestScreen.getContainer()).getNextTransactionID(SpookyJoiner.mc.player.inventory)));
                return;
            }
        } else {
            SpookyJoiner.selectCompass();
        }
    }

    @Subscribe
    private void onPacket(EventPacket eventPacket) {
        if (!eventPacket.isReceive()) {
            return;
        }
        if (!(eventPacket.getPacket() instanceof SChatPacket chatPacket)) {
            return;
        }

        String text = chatPacket.getChatComponent().getString().toLowerCase(Locale.ROOT);
        if (text.contains("\u0445\u0432\u0430\u0442\u0438\u0442 \u0441\u043f\u0430\u043c\u0438\u0442\u044c")
                || text.contains("\u043f\u0440\u0435\u043a\u0440\u0430\u0442\u0438\u0442\u0435 \u0441\u043f\u0430\u043c\u0438\u0442\u044c")) {
            this.toggle();
        }
    }
}


