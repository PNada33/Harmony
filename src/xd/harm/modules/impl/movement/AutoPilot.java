package xd.harm.modules.impl.movement;

import xd.harm.Harmony;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.StringSetting;
import xd.harm.utils.client.TimerUtility;
import xd.harm.utils.player.InventoryUtil;
import com.google.common.eventbus.Subscribe;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.play.client.CAnimateHandPacket;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.network.play.client.CPlayerTryUseItemPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;

@ModuleRegister(name = "AutoPilot", category = Category.Movement, desc = "Автоматически летит на элитрах к указанным координатам")
public class AutoPilot extends Module {
    private final StringSetting xPos = new StringSetting("X Координата", "0", "Введите X координату для полета");
    private final StringSetting zPos = new StringSetting("Z Координата", "0", "Введите Z координату для полета");
    private final StringSetting fireworkTimer = new StringSetting("Таймер фейерверка", "400", "Введите таймер фейерверка (мс)");
    private final BooleanSetting grimBypass = new BooleanSetting("Обход Грим", true);
    private final TimerUtility timer = new TimerUtility();
    private ItemStack currentStack = ItemStack.EMPTY;
    private double targetX;
    private double targetZ;
    private long fireworkDelay;
    private boolean initialized = false;
    private long lastFirework;

    public AutoPilot() {
        addSettings(xPos, zPos, fireworkTimer, grimBypass);
    }

    @Override
    public boolean onDisable() {
        initialized = false;
        timer.reset();
        super.onDisable();
        return false;
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (Harmony.getInstance().getModuleManager().getFreeCam().isState()) return;

        if (!initialized) {
            try {
                targetX = Double.parseDouble(xPos.get());
                targetZ = Double.parseDouble(zPos.get());
                fireworkDelay = Long.parseLong(fireworkTimer.get());
                initialized = true;
            } catch (NumberFormatException e) {
                print("Неверный формат координат или таймера!");
                toggle();
                return;
            }
        }

        if (mc.player.isElytraFlying()) {
            double playerX = mc.player.getPosX();
            double playerY = mc.player.getPosY();
            double playerZ = mc.player.getPosZ();

            double deltaX = targetX - playerX;
            double deltaZ = targetZ - playerZ;

            float yaw = (float) (MathHelper.atan2(deltaZ, deltaX) * 57.29577951308232) - 90.0f;

            float pitch = playerY >= 250.0 && playerY <= 345.0
                    ? MathHelper.lerp(0.042f, mc.player.rotationPitch, 5.0f)
                    : MathHelper.lerp(0.042f, mc.player.rotationPitch, -55.0f);

            mc.player.rotationYaw = MathHelper.wrapDegrees(yaw);
            mc.player.rotationPitch = MathHelper.clamp(pitch, -90.0f, 90.0f);

            if (mc.player.getMotion().length() <= 0.83 && !mc.player.isOnGround() && timer.hasTimeElapsed(fireworkDelay, true)) {
                int hotbarSlot = InventoryUtil.getInstance().getSlotInInventoryOrHotbar(Items.FIREWORK_ROCKET, true);
                int inventorySlot = InventoryUtil.getInstance().getSlotInInventoryOrHotbar(Items.FIREWORK_ROCKET, false);

                if (inventorySlot == -1 && hotbarSlot == -1) {
                    print("Нет фейерверков, полёт невозможен!");
                    toggle();
                    return;
                }

                if (!mc.player.getCooldownTracker().hasCooldown(Items.FIREWORK_ROCKET)) {
                    int slot = useFirework(hotbarSlot, inventorySlot);
                    if (slot > 8 && grimBypass.get()) {
                        mc.playerController.windowClick(0, slot, 0, ClickType.PICKUP, mc.player);
                    }
                }
            }

            double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
            if (distance < 50.0) {
                print("Прибыли в пункт назначения");
                toggle();
            }
        }
    }

    private int useFirework(int hotbarSlot, int inventorySlot) {
        int originalSlot = mc.player.inventory.currentItem;

        if (hotbarSlot != -1) {
            if (hotbarSlot != originalSlot) {
                mc.player.connection.sendPacket(new CHeldItemChangePacket(hotbarSlot));
            }
            mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
            if (hotbarSlot != originalSlot) {
                mc.player.connection.sendPacket(new CHeldItemChangePacket(originalSlot));
            }
            mc.player.connection.sendPacket(new CAnimateHandPacket(Hand.MAIN_HAND));
            lastFirework = System.currentTimeMillis();
            timer.reset();
            return hotbarSlot;
        }

        if (inventorySlot != -1) {
            mc.playerController.windowClick(0, inventorySlot, 0, ClickType.PICKUP, mc.player);
            mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
            mc.player.connection.sendPacket(new CAnimateHandPacket(Hand.MAIN_HAND));
            lastFirework = System.currentTimeMillis();
            timer.reset();
            return inventorySlot;
        }

        return -1;
    }
}
