package xd.harm.modules.impl.player;

import xd.harm.events.input.EventKey;
import xd.harm.events.network.EventPacket;
import xd.harm.events.world.EventUpdate;
import xd.harm.events.movement.EventMotion;
import xd.harm.events.movement.EventPostMotion;
import xd.harm.events.input.EventInput;
import xd.harm.events.movement.SprintEvent;
import com.google.common.eventbus.Subscribe;

import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BindSetting;
import xd.harm.modules.settings.impl.ModeSetting;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.utils.client.ServiceUtil;
import xd.harm.utils.math.StopWatch;
import xd.harm.utils.player.InventoryUtil;
import xd.harm.utils.rotation.FreeLookHandler;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.Items;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.network.play.client.CPlayerTryUseItemPacket;
import net.minecraft.network.play.client.CCloseWindowPacket;
import net.minecraft.util.Hand;

import java.util.Random;

@FieldDefaults(level = AccessLevel.PRIVATE)
@ModuleRegister(name = "ClickPearl", category = Category.Player, desc = "Автоматически бросает пёрл")
public class ClickPearl extends Module {
    final ModeSetting mode = new ModeSetting("Режим броска", "Легитный", "Легитный", "Рейджовый");
    final BooleanSetting syncHead = new BooleanSetting("Синхронизировать голову", false);
    final BindSetting pearlKey = new BindSetting("Кнопка", -98);
    final InventoryUtil.Hand handUtil = new InventoryUtil.Hand();
    final ItemCooldown itemCooldown;
    final StopWatch stopWatch = new StopWatch();
    final StopWatch legit = new StopWatch();
    final Random random = new Random();
    long delay;
    long pearlDelay = 350L;
    boolean throwPearl = false;
    int oldSlot = -1;
    int pearlSlot = -1;
    Hand activeHand = Hand.MAIN_HAND;
    long actionStartTime;
    ActionType actionType = ActionType.IDLE;
    public static boolean swapInProgress = false;

    public ClickPearl(ItemCooldown itemCooldown) {
        this.itemCooldown = itemCooldown;
        addSettings(mode, syncHead, pearlKey);
    }

    @Subscribe
    public void onKey(EventKey e) {
        if (mode.is("Легитный")) {
            if (mc.player.getCooldownTracker().hasCooldown(Items.ENDER_PEARL)) {
                return;
            }
            if (mc.player.isSpectator() || !mc.player.isAlive()) {
                return;
            }
            if (e.getKey() == pearlKey.get()) {
                throwPearl = true;
                if (actionType == ActionType.IDLE) {
                    pearlSlot = findPearlSlot();
                    if (pearlSlot == -1) return;
                    oldSlot = mc.player.inventory.currentItem;
                    activeHand = mc.player.getHeldItemOffhand().getItem() instanceof EnderPearlItem ? Hand.OFF_HAND : Hand.MAIN_HAND;
                    actionType = ActionType.SWITCH;
                    actionStartTime = System.currentTimeMillis();
                }
            }
        }
        if (mode.is("Рейджовый")) {
            throwPearl = e.getKey() == pearlKey.get();
        }
    }

    @Subscribe
    private void onMoveInput(EventInput e) {
        if (swapInProgress) {
            e.setForward(0.0F);
            e.setStrafe(0.0F);
        }
        if (!stopWatch.hasTimeElapsed()) {
            e.setForward(0.0F);
            e.setStrafe(0.0F);
        }
    }

    @Subscribe
    private void onSprint(SprintEvent e) {
        if (!stopWatch.hasTimeElapsed()) {
            e.cancel();
        }
    }

    @Subscribe
    private void onMotion(EventMotion e) {
        if (throwPearl || !legit.hasTimeElapsed()) {
            pearlDelay = mode.is("Рейджовый") ? 100L : 160L;
            stopWatch.reset();
            if (syncHead.get()) {
                e.setYaw(FreeLookHandler.getFreeYaw());
                e.setPitch(FreeLookHandler.getFreePitch());
                mc.player.rotationPitchHead = e.getPitch();
                mc.player.rotationYawHead = e.getYaw();
                mc.player.renderYawOffset = e.getYaw();
            }
        }
    }

    @Subscribe
    private void onUpdate(EventUpdate e) {
        if (mode.is("Рейджовый")) {
            handUtil.handleItemChange(System.currentTimeMillis() - delay > 111L);
        }
        if (actionType != ActionType.IDLE && mode.is("Легитный")) {
            handleLegitMode();
        }
    }

    private void handleLegitMode() {
        long currentTime = System.currentTimeMillis();
        switch (actionType) {
            case SWITCH:
                if (activeHand != Hand.OFF_HAND && pearlSlot != mc.player.inventory.currentItem) {
                    mc.player.inventory.currentItem = pearlSlot;
                }
                if (currentTime - actionStartTime >= 40 + random.nextInt(20)) {
                    actionType = ActionType.USE;
                }
                break;
            case USE:
                usePearl(activeHand);
                actionType = ActionType.SWAP_BACK;
                actionStartTime = currentTime;
                break;
            case SWAP_BACK:
                if (currentTime - actionStartTime >= 60 + random.nextInt(20)) {
                    if (activeHand != Hand.OFF_HAND && oldSlot != mc.player.inventory.currentItem) {
                        mc.player.inventory.currentItem = oldSlot;
                    }
                    resetState();
                }
                break;
        }
    }

    private void usePearl(Hand hand) {
        mc.playerController.processRightClick(mc.player, mc.world, hand);
        mc.player.swingArm(hand);
    }

    private int findPearlSlot() {
        return InventoryUtil.getInstance().getSlotInInventoryOrHotbar(Items.ENDER_PEARL, true);
    }

    @Subscribe
    private void onPostMotion(EventPostMotion e) {
        if (mode.is("Рейджовый")) {
            if (throwPearl) {
                if (!mc.player.getCooldownTracker().hasCooldown(Items.ENDER_PEARL)) {
                    boolean isOffhandEnderPearl = mc.player.getHeldItemOffhand().getItem() instanceof EnderPearlItem;
                    if (isOffhandEnderPearl) {
                        mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
                    } else {
                        int slot = findPearlAndThrow();
                        if (slot > 8) {
                            mc.playerController.pickItem(slot);
                            mc.getConnection().sendPacket(new CCloseWindowPacket(0));
                        }
                    }
                }
                throwPearl = false;
            }
        }
    }

    @Subscribe
    private void onPacket(EventPacket e) {
        if (mode.is("Рейджовый")) {
            handUtil.onEventPacket(e);
        }
    }

    private int findPearlAndThrow() {
        int hbSlot = InventoryUtil.getInstance().getSlotInInventoryOrHotbar(Items.ENDER_PEARL, true);
        if (hbSlot != -1) {
            handUtil.setOriginalSlot(mc.player.inventory.currentItem);
            if (hbSlot != mc.player.inventory.currentItem) {
                mc.player.connection.sendPacket(new CHeldItemChangePacket(hbSlot));
            }
            mc.player.swing(Hand.MAIN_HAND, false);
            mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
            if (hbSlot != mc.player.inventory.currentItem) {
                mc.player.connection.sendPacket(new CHeldItemChangePacket(mc.player.inventory.currentItem));
            }
            delay = System.currentTimeMillis();
            return hbSlot;
        } else {
            int invSlot = InventoryUtil.getInstance().getSlotInInventoryOrHotbar(Items.ENDER_PEARL, false);
            if (invSlot != -1) {
                handUtil.setOriginalSlot(mc.player.inventory.currentItem);
                mc.playerController.pickItem(invSlot);
                mc.getConnection().sendPacket(new CCloseWindowPacket(0));
                mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
                mc.player.swingArm(Hand.MAIN_HAND);
                delay = System.currentTimeMillis();
                return invSlot;
            } else {
                return -1;
            }
        }
    }

    private int findPearlInHotbar() {
        for(int i = 0; i < 9; ++i) {
            if (mc.player.inventory.getStackInSlot(i).getItem() == Items.ENDER_PEARL) {
                return i;
            }
        }
        return -1;
    }

    private void resetState() {
        actionType = ActionType.IDLE;
        pearlSlot = -1;
        oldSlot = -1;
        activeHand = Hand.MAIN_HAND;
        throwPearl = false;
    }

    @Override
    public boolean onDisable() {
        resetState();
        delay = 0L;
        super.onDisable();
        return false;
    }

    public enum ActionType {
        IDLE, SWITCH, USE, SWAP_BACK
    }
}
