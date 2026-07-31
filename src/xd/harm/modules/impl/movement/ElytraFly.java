package xd.harm.modules.impl.movement;

import xd.harm.Harmony;
import xd.harm.events.movement.EventMotion;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.ModeSetting;
import xd.harm.modules.settings.impl.SliderSetting;
import xd.harm.utils.client.TimerUtility;
import com.google.common.eventbus.Subscribe;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.ElytraItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.play.client.CEntityActionPacket;

import java.util.ArrayList;
import java.util.Random;

@ModuleRegister(name = "ElytraFlight", category = Category.Movement, desc = "Взлёт на элитрах Grim")
public class ElytraFly extends Module {
    private final TimerUtility TimeCounterSetting = new TimerUtility();
    private final TimerUtility TimeCounterSetting1 = new TimerUtility();
    public static long lastStartFalling;
    private ItemStack currentStack = ItemStack.EMPTY;
    private int oldSlot = -1;
    private int bestSlot = -1;
    private float rotationCounter = 0.0f;

    public ModeSetting mode = new ModeSetting("Мод", "Грим Вверх", "Грим Вверх", "Матрикс", "Эксплойт", "Элитра");
    private SliderSetting motionY = new SliderSetting("Скорость Y", 0.2f, 0.1f, 0.5f, 0.01f).setVisible(() -> mode.is("Матрикс"));
    private SliderSetting motionX = new SliderSetting("Скорость XZ", 1.2f, 0.1f, 5f, 0.1f).setVisible(() -> mode.is("Матрикс"));
    private BooleanSetting autojump = new BooleanSetting("Авто прыжок", false).setVisible(() -> mode.is("Матрикс") || mode.is("Elytra"));
    private BooleanSetting saveMe = new BooleanSetting("Спасать", false).setVisible(() -> mode.is("Матрикс"));
    private SliderSetting timerStartFireWork = new SliderSetting("Таймер фейерверка", 400, 50, 1500, 10).setVisible(() -> mode.is("CatFly Mode"));
    private BooleanSetting onlyGrimBypass = new BooleanSetting("Обход Грим", true).setVisible(() -> mode.is("CatFly Mode"));
    private BooleanSetting controll = new BooleanSetting("Убрать дёргание", false).setVisible(() -> mode.is("Матрикс"));
    private BooleanSetting flightmatrix = new BooleanSetting("Дерзкий флай", false).setVisible(() -> mode.is("Матрикс"));
    private SliderSetting flyupvalue = new SliderSetting("Значение Y", 1.37f, 0.05f, 1.370f, 0.037f).setVisible(() -> mode.is("Матрикс") && !flightmatrix.get());
    private SliderSetting horizontal = new SliderSetting("Скорость XZ", 1.5f, 0.0f, 5.0f, 0.37f).setVisible(() -> mode.is("Матрикс") && flightmatrix.get());
    private SliderSetting vertical = new SliderSetting("Скорость Y", 1.5f, 0.0f, 5.0f, 0.37f).setVisible(() -> mode.is("Матрикс") && flightmatrix.get());
    private SliderSetting ElytraSpeed = new SliderSetting("Elytra Y Speed", 0.3f, 0.1f, 400F, 0.01f).setVisible(() -> mode.is("Элитра"));

    public static boolean shackingcontroll;
    boolean launchRocket = true;

    public ElytraFly() {
        addSettings(mode, motionX, motionY, autojump, saveMe, timerStartFireWork, onlyGrimBypass, controll, flightmatrix, flyupvalue, horizontal, vertical, ElytraSpeed);
    }

    @Override
    public boolean onEnable() {
        super.onEnable();
        TimeCounterSetting.reset();
        TimeCounterSetting1.reset();
        rotationCounter = 0.0f;
        return false;
    }

    @Override
    public boolean onDisable() {
        super.onDisable();
        TimeCounterSetting.reset();
        TimeCounterSetting1.reset();
        shackingcontroll = false;
        mc.player.rotationPitchHead = 0.0f;
        mc.player.renderYawOffset = mc.player.rotationYaw;
        return false;
    }

    @Subscribe
    public void onUpdate(EventUpdate e) {
        if (Harmony.getInstance().getModuleManager().getFreeCam().isState()) return;
        shackingcontroll = controll.get();

        if (mode.is("Грим Вверх")) {
            shackingcontroll = true;
            this.currentStack = mc.player.getItemStackFromSlot(EquipmentSlotType.CHEST);
            if (this.currentStack.getItem() == Items.ELYTRA) {
                if (mc.player.isOnGround()) {
                    mc.player.jump();
                    mc.player.rotationPitchHead = -90.0f;
                } else if (ElytraItem.isUsable(this.currentStack) && !mc.player.isElytraFlying()) {
                    mc.player.startFallFlying();
                    mc.player.connection.sendPacket(new CEntityActionPacket(mc.player, CEntityActionPacket.Action.START_FALL_FLYING));
                    mc.player.rotationPitchHead = -90.0f;
                }
                mc.player.rotationPitch = 0.0f;
                mc.player.motion.y *= 1.08f;
            }
        } else if (mode.is("Эксплойт")) {
            if (!mc.player.isOnGround()) {
                float yaw = mc.player.rotationYaw;
                double rad = Math.toRadians(yaw);
                double motionX = -Math.sin(rad) * 1.89;
                double motionZ = Math.cos(rad) * 1.89;
                double motionY = 0.42;
                mc.player.setMotion(motionX, motionY, motionZ);
            }
        } else if (mode.is("Матрикс")) {
            this.currentStack = mc.player.getItemStackFromSlot(EquipmentSlotType.CHEST);
            if (this.currentStack.getItem() != Items.ELYTRA) {
                int elytraSlot = findElytraSlotInInventory();
                if (elytraSlot != -1) {
                    swapArmorSlot(elytraSlot);
                } else {
                    print("Элитры не найдены!");
                    this.toggle();
                    return;
                }
            }
            if (!mc.player.isElytraFlying() && !mc.player.isOnGround()) {
                mc.player.startFallFlying();
                mc.player.connection.sendPacket(new CEntityActionPacket(mc.player, CEntityActionPacket.Action.START_FALL_FLYING));
            }
            float pitch = mc.player.rotationPitch;
            if ((pitch > 50.0f || pitch < -50.0f) && !flightmatrix.get()) {
                mc.player.rotationPitch = 0.0f;
            }
            if (!flightmatrix.get()) {
                mc.player.motion.y = flyupvalue.get();
            } else {
                updatePlayerMotion();
            }
            if (mc.player.isOnGround() && autojump.get()) {
                mc.player.jump();
            }
            if (mc.player.isElytraFlying()) {
                mc.player.stopFallFlying();
            }
        } else if (mode.is("Элитра")) {
            this.currentStack = mc.player.getItemStackFromSlot(EquipmentSlotType.CHEST);
            if (this.currentStack.getItem() != Items.ELYTRA) {
                int elytraSlot = findElytraSlotInInventory();
                if (elytraSlot != -1) {
                    swapArmorSlot(elytraSlot);
                } else {
                    print("Элитры не найдены!");
                    this.toggle();
                    return;
                }
            }
            if (mc.player.isOnGround() && !mc.player.isInWater() && !mc.player.isSwimming()) {
                mc.player.jump();
                mc.player.rotationPitchHead = -90.0f;
            } else if (ElytraItem.isUsable(this.currentStack) && !mc.player.isElytraFlying() && !mc.player.isInWater() && !mc.player.isSwimming()) {
                mc.player.startFallFlying();
                mc.player.connection.sendPacket(new CEntityActionPacket(mc.player, CEntityActionPacket.Action.START_FALL_FLYING));
                mc.player.rotationPitchHead = -90.0f;
            }
            if (mc.player.isElytraFlying()) {
                double maxYSpeed = ElytraSpeed.get();
                if (mc.player.getMotion().y < maxYSpeed) {
                    mc.player.setMotion(
                            mc.player.getMotion().x,
                            mc.player.getMotion().y + 0.05,
                            mc.player.getMotion().z
                    );
                }
                rotationCounter += 0.05f;
                mc.player.renderYawOffset = (rotationCounter * 360.0f) % 360.0f;
                mc.player.prevRenderYawOffset = mc.player.renderYawOffset;
                mc.player.rotationYawHead = mc.player.rotationYaw;
            } else {
                mc.player.renderYawOffset = mc.player.rotationYaw;
                mc.player.prevRenderYawOffset = mc.player.rotationYaw;
            }
            if (!mc.player.isOnGround() && !mc.player.isInWater() && mc.gameSettings.keyBindJump.isKeyDown() && mc.player.fallDistance == 0.0f)     {
                if (ElytraItem.isUsable(this.currentStack) && !mc.player.isElytraFlying()) {
                    mc.player.startFallFlying();
                    mc.player.connection.sendPacket(new CEntityActionPacket(mc.player, CEntityActionPacket.Action.START_FALL_FLYING));
                }
            }
            if (mc.player.isOnGround() && autojump.get()) {
                mc.player.jump();
            }
        }
    }

    @Subscribe
    public void onMotion(EventMotion eventMotion) {
        if (mode.is("Матрикс")) {
            shackingcontroll = false;
        }
    }

    private void updatePlayerMotion() {
        if (mode.is("Матрикс") && flightmatrix.get()) {
            float yaw = mc.player.rotationYaw;
            double rad = Math.toRadians(yaw);
            double motionX = -Math.sin(rad) * horizontal.get();
            double motionZ = Math.cos(rad) * horizontal.get();
            double motionY = vertical.get();
            mc.player.setMotion(motionX, motionY, motionZ);
        }
    }

    private int findElytraSlotInInventory() {
        for (int i = 9; i <= 35; ++i) {
            if (mc.player.inventory.getStackInSlot(i).getItem() == Items.ELYTRA) {
                return i;
            }
        }
        return -1;
    }

    private void swapArmorSlot(int slot) {
        mc.playerController.windowClick(0, 6, slot, ClickType.SWAP, mc.player);
    }

    private int findRandomEmptySlot() {
        ArrayList<Integer> emptySlots = new ArrayList<>();
        for (int i = 9; i <= 35; ++i) {
            if (mc.player.inventory.getStackInSlot(i).isEmpty()) {
                emptySlots.add(i);
            }
        }
        if (emptySlots.isEmpty()) {
            return -1;
        }
        return emptySlots.get(new Random().nextInt(emptySlots.size()));
    }
}
