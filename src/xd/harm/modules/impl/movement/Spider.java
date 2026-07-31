package xd.harm.modules.impl.movement;

import com.google.common.eventbus.Subscribe;
import xd.harm.events.movement.EventMotion;
import xd.harm.events.world.EventUpdate;
import xd.harm.events.network.EventPacket;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.ModeSetting;
import xd.harm.modules.settings.impl.SliderSetting;
import xd.harm.modules.settings.impl.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.server.*;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.inventory.container.ClickType;
import xd.harm.utils.client.TimerUtility;
import xd.harm.utils.player.MoveUtils;

import java.util.Timer;
import java.util.TimerTask;

@ModuleRegister(name = "Spider", category = Category.Movement, desc = "Добавляет возможность ползать по стене")
public class Spider extends Module {

    public ModeSetting mode = new ModeSetting("Mode", "Грим", "Грим", "Грим 2", "Матрикс", "Элитра");

    private final SliderSetting matrixSpeed = new SliderSetting("Скорость", 2.0F, 1.0F, 10.0F, 0.05F)
            .setVisible(() -> mode.is("Матрикс"));

    private final BooleanSetting secondBypass = new BooleanSetting("Второй обход", true)
            .setVisible(() -> mode.is("Элитра"));

//    private final BooleanSetting holdShift = new BooleanSetting("Зажать Shift", true)
//            .setVisible(() -> mode.is("СпукиТайм Вода"));
//
//    private final BooleanSetting silentUse = new BooleanSetting("Скрытый свап", true)
//            .setVisible(() -> mode.is("СпукиТайм Вода"));
//
//    private final BooleanSetting jumpAtStart = new BooleanSetting("Прыгать в начале", true)
//            .setVisible(() -> mode.is("СпукиТайм Вода"));
//
//    private final SliderSetting maxHeight = new SliderSetting("Макс Высота", 10.0F, 3.0F, 75.0F, 1.0F)
//            .setVisible(() -> mode.is("СпукиТайм Вода"));

    private TimerUtility timerUtil = new TimerUtility();
    private Timer timer = new Timer();
    private boolean canUse = true;
    private boolean sneakEnabled = false;
    private int elytraHotbarSlot = -1;
    private int elytraArmorSlot = -1;
    private int savedSlot = -1;
    private long delayTime;

    private int previousSlot = -1;
    private long lastUseTime = 0L;
    private long lastWallJumpMs = 0L;
    private long lastGroundTime = 0L;
    private double startY = 0.0;
    private float originalPitch = 0.0F;
    private double tridentNextPauseY = 0.0D;
    private long tridentPauseUntilMs = 0L;

    public Spider() {
        addSettings(mode, secondBypass, matrixSpeed);
    }

//    @Override
//    public boolean onEnable() {
//        super.onEnable();
//        savedSlot = Minecraft.player.inventory.currentItem;
//        if (mode.is("СпукиТайм Трезубец")) {
//            previousSlot = Minecraft.player.inventory.currentItem;
//            tridentNextPauseY = Minecraft.player.getPosY() + 6.0D;
//            tridentPauseUntilMs = 0L;
//        }
//
//
//        if (mode.is("СпукиТайм Вода")) {
//            previousSlot = Minecraft.player.inventory.currentItem;
//            lastUseTime = 0L;
//            lastGroundTime = System.currentTimeMillis();
//            startY = Minecraft.player.getPosY();
//            originalPitch = Minecraft.player.rotationPitch;
//
//            if (!silentUse.get()) {
//                for (int i = 0; i < 9; i++) {
//                    if (Minecraft.player.inventory.getStackInSlot(i) != null) {
//                        if (Minecraft.player.inventory.getStackInSlot(i).getItem() == Items.LAVA_BUCKET) {
//                            Minecraft.player.inventory.currentItem = i;
//                            break;
//                        }
//                    }
//                }
//            }
//        }
//        return false;
//    }

//    @Override
//    public boolean onDisable() {
//        if (mode.is("СпукиТайм Вода") || mode.is("СпукиТайм Трезубец")) {
//            if (previousSlot != -1) {
//                Minecraft.player.inventory.currentItem = previousSlot;
//                previousSlot = -1;
//            }
//            tridentPauseUntilMs = 0L;
//            tridentNextPauseY = 0.0D;
//            if (mc.gameSettings != null) {
//                mc.gameSettings.keyBindSneak.setPressed(false);
//                mc.gameSettings.keyBindJump.setPressed(false);
//            }
//        }
//
//        super.onDisable();
//        return false;
//    }

    @Subscribe
    private void onMotion(EventMotion motion) {
        switch (mode.get()) {
            case "Матрикс":
                handleMatrix(motion);
                break;
            case "Грим":
                handleGrim(motion);
                break;
            case "Грим 2":
                handleGrim2(motion);
                break;
            case "Элитра":
                handleElytraMotion(motion);
                break;
        }
    }

//    private void handleBucketSpider(EventMotion motion) {
//        Minecraft.player.rotationPitch = 75.0F;
//        Minecraft.player.rotationPitchHead = 75.0F;
//        Minecraft.player.prevRotationPitch = 75.0F;
//        handleBucketMode();
//    }
//
//    private void handleTridentSpider(EventMotion motion) {
//        Minecraft.player.rotationPitch = 72.0F;
//        Minecraft.player.rotationPitchHead = 72.0F;
//        Minecraft.player.prevRotationPitch = 72.0F;
//        handleTridentMode();
//    }
//
//    private void handleTridentMode() {
//        long now = System.currentTimeMillis();
//        if (tridentPauseUntilMs > now) {
//            mc.gameSettings.keyBindSneak.setPressed(false);
//            mc.gameSettings.keyBindJump.setPressed(false);
//            return;
//        }
//        if (tridentPauseUntilMs != 0L) {
//            tridentPauseUntilMs = 0L;
//            tridentNextPauseY = Minecraft.player.getPosY() + 6.0D;
//        }
//
//        if (!Minecraft.player.collidedHorizontally) {
//            if (mc.gameSettings.keyBindSneak.isKeyDown()) {
//                mc.gameSettings.keyBindSneak.setPressed(false);
//            }
//            if (mc.gameSettings.keyBindJump.isKeyDown()) {
//                mc.gameSettings.keyBindJump.setPressed(false);
//            }
//            if (previousSlot != -1) {
//                Minecraft.player.inventory.currentItem = previousSlot;
//            }
//            tridentNextPauseY = Minecraft.player.getPosY() + 6.0D;
//            return;
//        }
//
//        if (Minecraft.player.getPosY() >= tridentNextPauseY) {
//            tridentPauseUntilMs = now + 100L;
//            mc.gameSettings.keyBindSneak.setPressed(false);
//            mc.gameSettings.keyBindJump.setPressed(false);
//            return;
//        }
//
//        int waterSlot = locateItemInHotbar(Items.LAVA_BUCKET);
//        if (waterSlot == -1) {
//            print("Положи Ведро воды в хотбар");
//            return;
//        }
//
//        int tridentSlot = locateItemInHotbar(Items.TRIDENT);
//        if (tridentSlot == -1) {
//            print("Положи Трезубец в хотбар");
//            return;
//        }
//
//        mc.gameSettings.keyBindSneak.setPressed(true);
//        mc.gameSettings.keyBindJump.setPressed(true);
//
//        Minecraft.player.connection.sendPacket(new CHeldItemChangePacket(waterSlot));
//        Minecraft.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
//        Minecraft.player.inventory.currentItem = tridentSlot;
//        Minecraft.player.connection.sendPacket(new CHeldItemChangePacket(tridentSlot));
//        Minecraft.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
//        Minecraft.player.motion.y = 0.42F;
//    }
//
//    private void handleBucketMode() {
//        if (Minecraft.player.isInWater()) {
//            Minecraft.player.motion.y = 0.45;
//        } else if (Minecraft.player.onGround) {
//            lastGroundTime = System.currentTimeMillis();
//            startY = Minecraft.player.getPosY();
//        } else {
//            long timeInAir = System.currentTimeMillis() - lastGroundTime;
//            if (timeInAir >= 120L) {
//                if (silentUse.get()) {
//                    handleSilentBucket();
//                } else {
//                    handleVisibleBucket();
//                }
//
//                if (!Minecraft.player.collidedHorizontally) {
//                    if (mc.gameSettings.keyBindSneak.isKeyDown() && holdShift.get()) {
//                        mc.gameSettings.keyBindSneak.setPressed(false);
//                    }
//                    if (Minecraft.player.motion.y <= 0.0 && mc.gameSettings.keyBindJump.isKeyDown() && jumpAtStart.get()) {
//                        mc.gameSettings.keyBindJump.setPressed(false);
//                    }
//                }
//            }
//        }
//    }

    private long getUseCooldown() {
        return 450L + (long)(Math.random() * 50.0);
    }


//    private void handleSilentBucket() {
//        int bucketSlot = -1;
//        for (int i = 0; i < 9; i++) {
//            if (Minecraft.player.inventory.getStackInSlot(i) != null) {
//                if (Minecraft.player.inventory.getStackInSlot(i).getItem() == Items.LAVA_BUCKET) {
//                    bucketSlot = i;
//                    break;
//                }
//            }
//        }
//        if (bucketSlot != -1 && Minecraft.player.collidedHorizontally) {
//            handleWallClimb(bucketSlot);
//        }
//    }
//
//    private void handleVisibleBucket() {
//        if (Minecraft.player.getHeldItemMainhand().getItem() == Items.LAVA_BUCKET) {
//            if (Minecraft.player.collidedHorizontally) {
//                handleWallClimb(Minecraft.player.inventory.currentItem);
//            }
//        }
//    }
//
//    private void handleWallClimb(int waterSlot) {
//        if (!Minecraft.player.onGround) {
//            double currentHeight = Minecraft.player.getPosY() - startY;
//            if (currentHeight >= (double)(float) maxHeight.get()) {
//                return;
//            }
//
//            long timeInAir = System.currentTimeMillis() - lastGroundTime;
//            if (timeInAir >= 120L) {
//                if (Minecraft.player.collidedHorizontally) {
//                    Minecraft.player.rotationPitch = 75.0F;
//                    if (holdShift.get()) {
//                        mc.gameSettings.keyBindSneak.setPressed(true);
//                    }
//                } else if (mc.gameSettings.keyBindSneak.isKeyDown() && holdShift.get()) {
//                    mc.gameSettings.keyBindSneak.setPressed(false);
//                }
//
//                if (jumpAtStart.get()) {
//                    if (Minecraft.player.isOnGround()) {
//                        long now = System.currentTimeMillis();
//                        if (now - lastWallJumpMs > 350L) {
//                            Minecraft.player.jump();
//                            lastWallJumpMs = now;
//                        }
//                    }
//                    mc.gameSettings.keyBindJump.setPressed(true);
//                }
//
//                long currentTime = System.currentTimeMillis();
//                long cooldown = getUseCooldown();
//                if (currentTime - lastUseTime >= cooldown) {
//                    int clientSlot = Minecraft.player.inventory.currentItem;
//                    if (silentUse.get() && waterSlot != clientSlot) {
//                        Minecraft.player.connection.sendPacket(new CHeldItemChangePacket(waterSlot));
//                    }
//
//                    Minecraft.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
//                    Minecraft.player.motion.y = 0.45;
//
//                    if (silentUse.get() && waterSlot != clientSlot) {
//                        Minecraft.player.connection.sendPacket(new CHeldItemChangePacket(clientSlot));
//                    }
//
//                    lastUseTime = currentTime;
//                }
//            }
//        }
//    }


    private void handleMatrix(EventMotion motion) {
        if (!Minecraft.player.collidedHorizontally) return;

        long speed = (long) Math.max(0, Math.min(500 - (long)(matrixSpeed.get() / 2.0F * 100.0F), 500));

        if (timerUtil.hasTimeElapsed(speed)) {
            motion.setOnGround(true);
            Minecraft.player.setOnGround(true);
            Minecraft.player.onGround = true;
            Minecraft.player.collidedHorizontally = true;
            Minecraft.player.collidedVertically = true;
            Minecraft.player.jump();
            timerUtil.reset();
        }
    }

    private void handleGrim(EventMotion motion) {
        int blockSlot = findBlockSlot(true);
        if (blockSlot == -1) {
            print("Блоки не найдены!");
            toggle();
            return;
        }

        if (!Minecraft.player.collidedHorizontally) return;

        if (Minecraft.player.isOnGround()) {
            motion.setOnGround(true);
            Minecraft.player.setOnGround(true);
            Minecraft.player.jump();
        }

        if (Minecraft.player.fallDistance > 0.0F && Minecraft.player.fallDistance < 2.0F) {
            placeBlockBelow(motion, blockSlot);
        }
    }

    private void handleGrim2(EventMotion motion) {
        if (Minecraft.player.isOnGround()) {
            delayTime = (long) Math.max(0, Math.min(500 - matrixSpeed.get() / 2.0F * 100.0F, 500));

            if (timerUtil.hasTimeElapsed(delayTime)) {
                mc.gameSettings.keyBindSneak.setPressed(true);
                motion.setOnGround(true);
                Minecraft.player.setOnGround(true);
                Minecraft.player.onGround = true;
                Minecraft.player.collidedHorizontally = true;
                Minecraft.player.collidedVertically = true;

                if (Minecraft.player.fallDistance != 0.0F) {
                    mc.gameSettings.keyBindJump.setPressed(true);
                    mc.gameSettings.keyBindJump.setPressed(false);
                }

                Minecraft.player.jump();
                mc.gameSettings.keyBindSneak.setPressed(false);
                timerUtil.reset();
            }
        }
    }

    private void handleElytraMotion(EventMotion motion) {
        motion.setPitch(0.0F);
        Minecraft.player.rotationPitch = 0.0F;
    }

    @Subscribe
    public void onUpdate(EventUpdate e) {
        if (mode.is("Элитра")) {
            if (!secondBypass.get()) {
                handleElytraSimple();
            } else {
                handleElytraAdvanced();
            }
        }
    }

    private void handleElytraSimple() {
        for (int i = 0; i < 9; i++) {
            if (Minecraft.player.inventory.getStackInSlot(i).getItem() == Items.ELYTRA) {
                if (!Minecraft.player.isOnGround() && Minecraft.player.collidedHorizontally && Minecraft.player.fallDistance == 0.0F) {
                    mc.playerController.windowClick(0, 6, i, ClickType.SWAP, Minecraft.player);
                    Minecraft.player.connection.sendPacket(new CEntityActionPacket(Minecraft.player, CEntityActionPacket.Action.START_FALL_FLYING));
                    MoveUtils.setMotion(0.06);
                    Minecraft.player.motion.y = 0.366D;
                    mc.playerController.windowClick(0, 6, i, ClickType.SWAP, Minecraft.player);
                    elytraHotbarSlot = i;
                }
            }
        }
    }

    private void handleElytraAdvanced() {
        ItemStack chestSlot = Minecraft.player.inventory.armorInventory.get(2);

        if (chestSlot.getItem() != Items.ELYTRA && Minecraft.player.collidedHorizontally) {
            for (int i = 0; i < 9; i++) {
                if (Minecraft.player.inventory.getStackInSlot(i).getItem() == Items.ELYTRA) {
                    mc.playerController.windowClick(0, 6, i, ClickType.SWAP, Minecraft.player);
                    elytraArmorSlot = i;
                    timerUtil.reset();
                }
            }
        }

        if (Minecraft.player.collidedHorizontally) {
            mc.gameSettings.keyBindUseItem.setPressed(false);
            if (timerUtil.hasTimeElapsed(180L)) {
                mc.gameSettings.keyBindUseItem.setPressed(true);
            }
        }

        if (chestSlot.getItem() == Items.ELYTRA && !Minecraft.player.collidedHorizontally && elytraArmorSlot != -1) {
            mc.playerController.windowClick(0, 6, elytraArmorSlot, ClickType.SWAP, Minecraft.player);
            elytraArmorSlot = -1;
        }

        if (chestSlot.getItem() == Items.ELYTRA && !Minecraft.player.isOnGround() && Minecraft.player.collidedHorizontally && Minecraft.player.fallDistance == 0.0F) {
            Minecraft.player.connection.sendPacket(new CEntityActionPacket(Minecraft.player, CEntityActionPacket.Action.START_FALL_FLYING));
            MoveUtils.setMotion(0.02);
            Minecraft.player.motion.y = 0.36D;
        }
    }

    @Subscribe
    public void onPacket(EventPacket e) {
        if (mode.is("Элитра")) {
            Object packet = e.getPacket();

            if (!secondBypass.get()) {
                if (packet instanceof SPlayerPositionLookPacket) {
                    SPlayerPositionLookPacket p = (SPlayerPositionLookPacket) packet;
                    Minecraft.player.setPositionAndUpdate(p.getX(), p.getY(), p.getZ());
                    return;
                }
            }

            if (packet instanceof SEntityVelocityPacket) {
                if (((SEntityVelocityPacket) packet).getEntityID() == Minecraft.player.getEntityId()) {
                    e.cancel();
                }
            }
        }
    }

    private void placeBlockBelow(EventMotion motion, int blockSlot) {
        int lastSlot = Minecraft.player.inventory.currentItem;
        Minecraft.player.inventory.currentItem = blockSlot;

        motion.setPitch(80.0F);
        motion.setYaw(Minecraft.player.getPosition().getY());

        float yaw = motion.getYaw();
        float pitch = motion.getPitch();

        Minecraft.player.swingArm(Hand.MAIN_HAND);
        mc.playerController.processRightClick(Minecraft.player, Minecraft.world, Hand.MAIN_HAND);

        Minecraft.player.inventory.currentItem = lastSlot;
        Minecraft.player.fallDistance = 0.0F;
    }

    public int findBlockSlot(boolean inHotBar) {
        int firstSlot = inHotBar ? 0 : 9;
        int lastSlot = inHotBar ? 9 : 36;
        int finalSlot = -1;

        for (int i = firstSlot; i < lastSlot; i++) {
            ItemStack stack = Minecraft.player.inventory.getStackInSlot(i);
            if (stack.getItem() != Items.AIR &&
                    !(stack.getItem() instanceof net.minecraft.item.BlockItem) &&
                    stack.getItem() != Items.LAVA_BUCKET) {
                continue;
            }
            if (stack.getItem() instanceof net.minecraft.item.BlockItem) {
                finalSlot = i;
            }
        }
        return finalSlot;
    }

    private int locateItemInHotbar(Item item) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = Minecraft.player.inventory.getStackInSlot(i);
            if (stack.getItem() == item) {
                return i;
            }
        }
        return -1;
    }
}
