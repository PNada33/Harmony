package xd.harm.modules.impl.player;

import com.google.common.eventbus.Subscribe;
import xd.harm.config.FriendStorage;
import xd.harm.events.network.EventPacket;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.ModeListSetting;
import xd.harm.utils.math.StopWatch;
import net.minecraft.block.Block;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.network.play.client.CPlayerTryUseItemPacket;
import net.minecraft.network.play.server.SChatPacket;
import net.minecraft.network.play.server.SPlaySoundEffectPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.TextFormatting;

import java.util.Arrays;
import java.util.Locale;

@ModuleRegister(name = "AutoActions", category = Category.Player, desc = "Автоинструмент")
public class AutoActions extends Module {

    public static ModeListSetting actions = new ModeListSetting("Действия",
        new BooleanSetting("Авто принятие тп", false),
        new BooleanSetting("Авто предметы", false),
        new BooleanSetting("Авто возврождение", true)
    );

    private final BooleanSetting onlyFriend = new BooleanSetting("Только друзья", true).setVisible(() -> actions.getValueByName("Авто принятие тп").get());
    public final BooleanSetting silent = new BooleanSetting("Незаметный", true).setVisible(() -> actions.getValueByName("Авто предметы").get());
    private final String[] teleportMessages = new String[]{"has requested teleport", "просит телепортироваться", "хочет телепортироваться к вам", "просит к вам телепортироваться"};
    private final StopWatch delay = new StopWatch();
    private boolean isHooked = false;
    private boolean needToHook = false;
    public int itemIndex = -1, oldSlot = -1;
    boolean status;

    public AutoActions() {
        addSettings(actions, onlyFriend);
    }

    @Subscribe
    private void onUpdate(EventUpdate e) {
        if (mc.player == null || mc.world == null) {
            return;
        }
        if (actions.getValueByName("Авто предметы").get() && !mc.player.isCreative()) {
            if (isMousePressed()) {
                itemIndex = findBestToolSlotInHotBar();
                if (itemIndex != -1) {
                    status = true;

                    if (oldSlot == -1) {
                        oldSlot = mc.player.inventory.currentItem;
                    }

                    if (silent.get()) {
                        mc.player.connection.sendPacket(new CHeldItemChangePacket(itemIndex));
                    } else {
                        mc.player.inventory.currentItem = itemIndex;
                    }
                }
            } else if (status && oldSlot != -1) {
                if (silent.get()) {
                    mc.player.connection.sendPacket(new CHeldItemChangePacket(oldSlot));
                } else {
                    mc.player.inventory.currentItem = oldSlot;
                }

                itemIndex = oldSlot;
                status = false;
                oldSlot = -1;
            }
        }

        if (actions.getValueByName("Авто возврождение").get()) {
            if (mc.currentScreen instanceof DeathScreen && mc.player.deathTime > 5) {
                mc.player.respawnPlayer();
                mc.displayGuiScreen(null);
            }
        }
    }

    @Subscribe
    public void onPacket(EventPacket e) {

        if (actions.getValueByName("Авто принятие тп").get()) {
            if (e.getPacket() instanceof SChatPacket p) {
                String raw = p.getChatComponent().getString().toLowerCase(Locale.ROOT);
                String message = TextFormatting.getTextWithoutFormattingCodes(p.getChatComponent().getString());
                if (isTeleportMessage(message)) {
                    if (onlyFriend.get()) {
                        boolean yes = false;
                        for (String friend : FriendStorage.getFriends()) {
                            if (raw.contains(friend.toLowerCase(Locale.ROOT))) {
                                yes = true;
                                break;
                            }
                        }
                        if (!yes) return;
                    }
                    mc.player.sendChatMessage("/tpaccept");
                }
            }
        }
    }

    private boolean isTeleportMessage(String message) {
        return Arrays.stream(this.teleportMessages).map(String::toLowerCase).anyMatch(message::contains);
    }

    private int findBestToolSlotInHotBar() {
        if (mc.objectMouseOver instanceof BlockRayTraceResult blockRayTraceResult) {
            Block block = mc.world.getBlockState(blockRayTraceResult.getPos()).getBlock();

            int bestSlot = -1;
            float bestSpeed = 2.0f;

            for (int slot = 0; slot < 9; slot++) {
                float speed = mc.player.inventory.getStackInSlot(slot)
                        .getDestroySpeed(block.getDefaultState());

                if (speed > bestSpeed) {
                    bestSpeed = speed;
                    bestSlot = slot;
                }
            }
            return bestSlot;
        }
        return -1;
    }


    private boolean isMousePressed() {
        return mc.objectMouseOver != null && mc.gameSettings.keyBindAttack.isKeyDown();
    }

    @Override
    public boolean onDisable() {
        status = false;
        itemIndex = -1;
        oldSlot = -1;
        super.onDisable();
        return false;
    }

}

