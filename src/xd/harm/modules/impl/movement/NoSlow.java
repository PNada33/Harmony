package xd.harm.modules.impl.movement;

import com.google.common.eventbus.Subscribe;
import net.minecraft.item.Items;
import net.minecraft.item.UseAction;
import net.minecraft.network.play.client.CEntityActionPacket;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.network.play.client.CPlayerDiggingPacket;
import net.minecraft.network.play.client.CPlayerTryUseItemPacket;
import net.minecraft.network.play.server.SHeldItemChangePacket;
import net.minecraft.potion.Effects;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import xd.harm.events.movement.EventNoSlow;
import xd.harm.events.movement.NoSlowEvent;
import xd.harm.events.network.EventPacket;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.ModeSetting;
import xd.harm.utils.math.StopWatch;
import xd.harm.utils.player.MoveUtils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ModuleRegister(name = "NoSlow", category = Category.Movement, desc = "Позволяет бегать при действиях")
public class NoSlow extends Module {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final ModeSetting mode = new ModeSetting("Мод", "Матрикс", "Матрикс", "Грим", "ФанАЧ", "Рилик", "СпукиТайм");
    private final BooleanSetting rightHand = new BooleanSetting("Во второстепенной руке", false).setVisible(() -> this.mode.is("Грим"));
    private final BooleanSetting jumpFalse = new BooleanSetting("Отключение прыжка", true);
    private final StopWatch stopWatch = new StopWatch();
    private int spookyUseTicks;
    public static Stopper stopper = new Stopper();

    public NoSlow() {
        this.addSettings(this.mode, this.rightHand, this.jumpFalse);
    }

    @Override
    public boolean onDisable() {
        spookyUseTicks = 0;
        stopWatch.reset();
        return super.onDisable();
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (!this.mode.is("СпукиТайм")) {
            return;
        }
        if (mc.player == null || mc.player.isElytraFlying()) {
            return;
        }
        spookyUseTicks = mc.player.isHandActive() ? spookyUseTicks + 1 : 0;
    }

    @Subscribe
    public void onPacket(EventPacket event) {
        if (!this.mode.is("СпукиТайм")) {
            return;
        }
        if (mc.player == null) {
            return;
        }
        if (event.isReceive() && event.getPacket() instanceof SHeldItemChangePacket) {
            if (stopWatch.isReached(100L)) {
                mc.player.connection.sendPacket(new CHeldItemChangePacket(mc.player.inventory.currentItem % 8 + 1));
                mc.player.connection.sendPacket(new CHeldItemChangePacket(mc.player.inventory.currentItem));
                stopWatch.reset();
            }
            event.cancel();
        }
    }

    @Subscribe
    public void onSlowWalking(EventNoSlow event) {
        if (!this.mode.is("СпукиТайм")) {
            return;
        }
        if (mc.player == null || mc.player.isElytraFlying()) {
            return;
        }
        if (spookyUseTicks >= 2
                && mc.player.getActiveItemStack().getItem() != Items.GOLDEN_APPLE
                && mc.player.getActiveItemStack().getItem() != Items.ENCHANTED_GOLDEN_APPLE) {
            event.cancel();
            spookyUseTicks = 0;
        }
    }

    @Subscribe
    public void onEating(NoSlowEvent event) {
        if (mc.player == null || !mc.player.isHandActive()) {
            return;
        }
        switch (this.mode.get()) {
            case "Грим":
                handleGrimACMode(event);
                break;
            case "Матрикс":
                handleMatrixMode(event);
                break;
            case "ФанАЧ":
                handleFtMode(event);
                break;
            case "Рилик":
                handleReallyWorldMode(event);
                break;
        }
    }

    private void handleGrimACMode(NoSlowEvent event) {
        if (mc.player.getHeldItemOffhand().getUseAction() == UseAction.BLOCK && mc.player.getActiveHand() == Hand.MAIN_HAND
                || mc.player.getHeldItemOffhand().getUseAction() == UseAction.EAT && mc.player.getActiveHand() == Hand.MAIN_HAND) {
            return;
        }
        if (mc.player.getActiveHand() == Hand.MAIN_HAND) {
            mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.OFF_HAND));
            event.cancel();
            return;
        }
        event.cancel();
        sendItemChangePacket();
    }

    private void handleMatrixMode(NoSlowEvent event) {
        boolean isFalling = mc.player.fallDistance > 0.725F;
        event.cancel();
        if (mc.player.isOnGround() && !mc.player.movementInput.jump) {
            if (mc.player.ticksExisted % 2 == 0) {
                boolean isNotStrafing = mc.player.moveStrafing == 0.0F;
                float speedMultiplier = isNotStrafing ? 0.5F : 0.4F;
                mc.player.motion.x *= speedMultiplier;
                mc.player.motion.z *= speedMultiplier;
            }
        } else if (isFalling) {
            boolean isVeryFastFalling = mc.player.fallDistance > 1.4F;
            float speedMultiplier = isVeryFastFalling ? 0.95F : 0.97F;
            mc.player.motion.x *= speedMultiplier;
            mc.player.motion.z *= speedMultiplier;
        }
    }

    private void handleFtMode(NoSlowEvent event) {
        if (jumpFalse.get() && mc.player.movementInput.jump) {
            mc.player.movementInput.jump = false;
        }
        if (mc.player.isElytraFlying()) {
            return;
        }
        if (!isBlockUnderWithMotion() && mc.player.isOnGround() && !mc.player.movementInput.jump && !mc.player.isPotionActive(Effects.SLOWNESS)) {
            float boost = mc.player.moveStrafing == 0.0F || mc.player.moveForward == 0.0F ? 0.015F : 0.0F;
            float speed = mc.player.isPotionActive(Effects.SPEED) ? 0.35F : 0.3F;
            MoveUtils.setMotion(speed + boost);
            if (stopWatch.isReached(120L)) {
                BlockPos.getAllInBox(mc.player.getBoundingBox().offset(0.0, -0.1, 0.0))
                        .filter(pos -> !mc.world.getBlockState(pos).isAir())
                        .forEach(pos -> mc.player.connection.sendPacket(new CPlayerDiggingPacket(CPlayerDiggingPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.UP)));
                mc.player.connection.sendPacket(new CEntityActionPacket(mc.player, CEntityActionPacket.Action.START_SNEAKING));
                scheduler.schedule(() -> mc.player.connection.sendPacket(new CEntityActionPacket(mc.player, CEntityActionPacket.Action.STOP_SNEAKING)), 1L, TimeUnit.MILLISECONDS);
                stopWatch.reset();
            }
        }
    }

    private void handleReallyWorldMode(NoSlowEvent event) {
        if (mc.player == null || mc.player.isElytraFlying()) {
            return;
        }
        mc.player.connection.sendPacket(new CPlayerDiggingPacket(CPlayerDiggingPacket.Action.ABORT_DESTROY_BLOCK, mc.player.getPosition().up(), Direction.NORTH));
        event.cancel();
    }

    private void sendItemChangePacket() {
        if (MoveUtils.isMoving()) {
            mc.player.connection.sendPacket(new CHeldItemChangePacket(mc.player.inventory.currentItem % 8 + 1));
            mc.player.connection.sendPacket(new CHeldItemChangePacket(mc.player.inventory.currentItem));
        }
    }

    public boolean isBlockUnderWithMotion() {
        AxisAlignedBB aabb = mc.player.getBoundingBox().offset(mc.player.getMotion().x, -0.1, mc.player.getMotion().z);
        return mc.world.getCollisionShapes(mc.player, aabb).toList().isEmpty();
    }

    public static class Stopper {
        private long lastMS = System.currentTimeMillis();

        public void reset() {
            lastMS = System.currentTimeMillis();
        }

        public boolean hasTimeElapsed(long time, boolean reset) {
            if (System.currentTimeMillis() - lastMS > time) {
                if (reset) {
                    reset();
                }
                return true;
            }
            return false;
        }

        public void setLastMS(long lastMS) {
            this.lastMS = lastMS;
        }

        public long getLastMS() {
            return lastMS;
        }
    }
}
