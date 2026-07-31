package xd.harm.modules.impl.movement;

import xd.harm.events.network.EventPacket;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.ModeSetting;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.utils.client.TimerUtility;
import com.google.common.eventbus.Subscribe;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.EditSignScreen;
import net.minecraft.client.gui.screen.inventory.ChestScreen;
import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import net.minecraft.client.gui.screen.inventory.MerchantScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.item.ItemStack;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.client.CClickWindowPacket;
import net.minecraft.network.play.server.SCloseWindowPacket;
import net.minecraft.util.MovementInput;

@ModuleRegister(name = "GuiMove", category = Category.Movement, desc = "Позволяет двигаться в инвентаре")
public class GuiMove extends Module {
    private final ModeSetting mode = new ModeSetting("Режим", "Обход", "Обычный", "Обход", "Легитный", "Безопасный");
    private final BooleanSetting exploitPortal = new BooleanSetting("Не дать закрывать порталу", false);
    private final BooleanSetting slowInInventory = new BooleanSetting("Замедляться в инвентаре", false);
    private final BooleanSetting cancelClosePacket = new BooleanSetting("Отменить пакет закрытия", false);
    private final BooleanSetting onlyInventory = new BooleanSetting("Только в инвентаре", false);
    public static final BooleanSetting bounceAnimation = new BooleanSetting("Прыгающая анимация", false);
    private final TimerUtility delayTimer = new TimerUtility();
    public static final List<IPacket<?>> packetQueue = new ArrayList<>();
    public static boolean exploitPortalValue = false;
    public static boolean stopMovement = false;
    public static boolean needToSlow = false;
    public static final KeyBinding[] moveKeys;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final TimerUtility clickTimer = new TimerUtility();
    private final TimerUtility waitTimer = new TimerUtility();
    private final TimerUtility sprintTimer = new TimerUtility();
    public static boolean isVarenik;

    public GuiMove() {
        addSettings(mode, exploitPortal, slowInInventory, cancelClosePacket, onlyInventory, bounceAnimation);
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        isVarenik = false;
        if (slowInInventory.get()) {
            if (Minecraft.getInstance().currentScreen instanceof InventoryScreen) {
                Minecraft.getInstance().player.setSprinting(false);
                needToSlow = true;
            } else {
                needToSlow = false;
            }
        } else {
            needToSlow = false;
        }

        exploitPortalValue = exploitPortal.get();
        if ((mode.is("Grim") || mode.is("Advanced") || mode.is("Обход") || mode.is("Grim/Spooky")) && Minecraft.getInstance().player != null) {
            KeyBinding[] keys = new KeyBinding[]{
                    Minecraft.getInstance().gameSettings.keyBindForward,
                    Minecraft.getInstance().gameSettings.keyBindBack,
                    Minecraft.getInstance().gameSettings.keyBindLeft,
                    Minecraft.getInstance().gameSettings.keyBindRight,
                    Minecraft.getInstance().gameSettings.keyBindJump
            };
            int delay = mode.is("Grim") ? 400 : (mode.is("Advanced") ? 170 : 1000);
            if (!packetQueue.isEmpty() && !waitTimer.hasReached(delay)) {
                for (KeyBinding key : keys) {
                    key.setPressed(false);
                }
                return;
            }

            if (Minecraft.getInstance().currentScreen instanceof ChatScreen || Minecraft.getInstance().currentScreen instanceof EditSignScreen) {
                return;
            }

            this.updateKeyBindingState(keys);
        }

        if (mode.is("Легитный") && Minecraft.getInstance().player != null) {
            if (Minecraft.getInstance().currentScreen instanceof ChatScreen || Minecraft.getInstance().currentScreen instanceof EditSignScreen) {
                return;
            }

            KeyBinding[] keys = new KeyBinding[]{
                    Minecraft.getInstance().gameSettings.keyBindForward,
                    Minecraft.getInstance().gameSettings.keyBindBack,
                    Minecraft.getInstance().gameSettings.keyBindLeft,
                    Minecraft.getInstance().gameSettings.keyBindRight,
                    Minecraft.getInstance().gameSettings.keyBindJump
            };
            if (stopMovement) {
                for (KeyBinding key : keys) {
                    key.setPressed(false);
                }
            } else {
                this.updateKeyBindingState(keys);
            }
        }

        if (mode.is("Обычный") || mode.is("Безопасный")) {
            this.handleInventoryMove();
        }
    }

    private void handleInventoryMove() {
        KeyBinding[] keys = new KeyBinding[]{
                Minecraft.getInstance().gameSettings.keyBindForward,
                Minecraft.getInstance().gameSettings.keyBindBack,
                Minecraft.getInstance().gameSettings.keyBindLeft,
                Minecraft.getInstance().gameSettings.keyBindRight,
                Minecraft.getInstance().gameSettings.keyBindJump
        };
        if (!(Minecraft.getInstance().currentScreen instanceof ChatScreen) && !(Minecraft.getInstance().currentScreen instanceof EditSignScreen)) {
            for (KeyBinding key : keys) {
                key.setPressed(InputMappings.isKeyDown(Minecraft.getInstance().getMainWindow().getHandle(), key.getDefault().getKeyCode()));
            }
        }
    }

    @Subscribe
    public void onPacket(EventPacket event) {
        if (event.getPacket() instanceof SCloseWindowPacket && cancelClosePacket.get()) {
            event.cancel();
        }

        if (!(Minecraft.getInstance().currentScreen instanceof InventoryScreen) && !(Minecraft.getInstance().currentScreen instanceof ChestScreen) && !(Minecraft.getInstance().currentScreen instanceof MerchantScreen) && event.getPacket() instanceof SCloseWindowPacket) {
            event.cancel();
        }

        IPacket<?> packet = event.getPacket();
        if (packet instanceof CClickWindowPacket) {
            CClickWindowPacket clickPacket = (CClickWindowPacket) packet;
            if (isPlayerMoving() && Minecraft.getInstance().currentScreen instanceof InventoryScreen) {
                if (mode.is("Grim") || mode.is("Обход") || mode.is("Grim/Spooky")) {
                    packetQueue.add(clickPacket);
                    event.cancel();
                }

                if (mode.is("Легитный")) {
                    event.cancel();
                    stopMovement = true;
                    new Thread(() -> {
                        stopMovement = true;
                        waitTimer.reset();
                        try {
                            Thread.sleep(100L);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        Minecraft.getInstance().player.connection.sendPacket(clickPacket);
                        stopMovement = false;
                    }).start();
                    stopMovement = false;
                }
            }
        }

        if (event.getPacket() instanceof SCloseWindowPacket && mode.is("Легитный")) {
            event.cancel();
        }
    }

    @Subscribe
    public void onMoveInput(MovementInput event) {
        ItemStack heldItem = Minecraft.getInstance().player.inventory.getItemStack();
        if (mode.is("Легитный") && !heldItem.isEmpty()) {
            event.sneaking = false;
            event.jump = false;
            event.moveForward = 0.0F;
            event.moveStrafe = 0.0F;
        }

        if (stopMovement) {
            event.moveForward = 0.0F;
            event.moveStrafe = 0.0F;
        }

        if (onlyInventory.get() && Minecraft.getInstance().currentScreen instanceof ChestScreen && Minecraft.getInstance().currentScreen != null) {
            event.sneaking = false;
            event.jump = false;
            event.moveForward = 0.0F;
            event.moveStrafe = 0.0F;
        }

        if (!clickTimer.hasReached(100)) {
            event.sneaking = false;
            event.jump = false;
            event.moveForward = 0.0F;
            event.moveStrafe = 0.0F;
        }
    }

    private void updateKeyBindingState(KeyBinding[] keys) {
        for (KeyBinding key : keys) {
            boolean isPressed = InputMappings.isKeyDown(Minecraft.getInstance().getMainWindow().getHandle(), key.getDefault().getKeyCode());
            key.setPressed(isPressed);
        }
    }

    private boolean isPlayerMoving() {
        if (Minecraft.getInstance().player == null) return false;
        return Minecraft.getInstance().player.movementInput.moveForward != 0.0F ||
                Minecraft.getInstance().player.movementInput.moveStrafe != 0.0F;
    }

    @Override
    public boolean onEnable() {
        super.onEnable();
        return false;
    }

    @Override
    public boolean onDisable() {
        super.onDisable();
        exploitPortalValue = false;
        stopMovement = false;
        needToSlow = false;
        return false;
    }

    static {
        moveKeys = new KeyBinding[]{
                Minecraft.getInstance().gameSettings.keyBindForward,
                Minecraft.getInstance().gameSettings.keyBindBack,
                Minecraft.getInstance().gameSettings.keyBindLeft,
                Minecraft.getInstance().gameSettings.keyBindRight,
                Minecraft.getInstance().gameSettings.keyBindJump
        };
    }
}
