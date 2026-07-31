package xd.harm.modules.impl.movement;

import com.google.common.eventbus.Subscribe;
import xd.harm.events.input.EventInput;
import xd.harm.events.movement.EventMotion;
import xd.harm.events.movement.JumpEvent;
import xd.harm.events.movement.SprintEvent;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.ModeSetting;
import xd.harm.utils.player.MoveUtils;
import net.minecraft.util.math.MathHelper;

@ModuleRegister(name = "MovementFix", category = Category.Movement, desc = "Фиксирует движение под ваш поворот")
public class MovementFix extends Module {

    private final ModeSetting mode = new ModeSetting("Режим", "Нормальный", "Нормальный", "Только спринт");
    private final BooleanSetting packetOnly = new BooleanSetting("Packet only", false)
            .setVisible(() -> mode.is("Только спринт"));
    private final BooleanSetting backwardFix = new BooleanSetting("Backward fix", false);

    public MovementFix() {
        addSettings(mode, packetOnly, backwardFix);
    }

    public boolean isBackwardFix() {
        return isState() && backwardFix.get();
    }

    @Subscribe
    public void onInput(EventInput event) {
        if (!mode.is("Нормальный")) return;

        float forward = event.getForward();
        float strafe = event.getStrafe();
        if (forward == 0 && strafe == 0) return;

        MoveUtils.fixMovement(event, mc.player.rotationYaw);
    }

    @Subscribe
    public void onSprint(SprintEvent event) {
        if (!mode.is("Только спринт") || packetOnly.get()) return;

        if (isResetSprint()) {
            event.cancel();
        }
    }

    @Subscribe
    public void onJump(JumpEvent event) {
        if (!mode.is("Только спринт") || packetOnly.get()) return;

        if (isResetSprint()) {
            event.cancel();
        }
    }

    @Subscribe
    public void onMotion(EventMotion event) {
        if (!mode.is("Только спринт") || !packetOnly.get()) return;

        if (isResetSprint()) {
            mc.player.setSprinting(false);
        }
    }

    private boolean isResetSprint() {
        float rotationYaw = mc.player.rotationYaw;
        float movementYaw = getMovementYaw();
        float diff = Math.abs(MathHelper.wrapDegrees(rotationYaw - movementYaw));
        return diff > 45.0F + 0.005F;
    }

    private float getMovementYaw() {
        float yaw = mc.player.rotationYaw;
        float forward = mc.player.movementInput.moveForward;
        float strafe = mc.player.movementInput.moveStrafe;

        if (forward < 0) yaw += 180;
        float forwardFactor = 1;
        if (forward < 0) forwardFactor = -0.5F;
        else if (forward > 0) forwardFactor = 0.5F;
        if (strafe > 0) yaw -= 90 * forwardFactor;
        if (strafe < 0) yaw += 90 * forwardFactor;
        return yaw;
    }
}
