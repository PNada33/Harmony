package xd.harm.utils.rotation;

import com.google.common.eventbus.Subscribe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;
import xd.harm.Harmony;
import xd.harm.events.world.EventUpdate;
import xd.harm.utils.client.IMinecraft;
import xd.harm.utils.math.SensUtils;

public class RotationHandler implements IMinecraft {
    private static RotationTask currentTask;
    private static float currentTurnSpeed;
    private static int currentPriority;
    private static int currentTimeout;
    private static int idleTicks;

    public RotationHandler() {
        Harmony.getInstance().getEventBus().register(this);
    }

    public static void update(xd.harm.utils.aurautil.Rotation rotation, float turnSpeed, int timeout, int priority) {
        if (rotation == null) {
            return;
        }
        update(new Rotation(rotation.getYaw(), rotation.getPitch()), turnSpeed, timeout, priority);
    }

    @Subscribe
    public void onUpdate(EventUpdate e) {
        if (currentTask == RotationTask.IDLE) {
            return;
        }

        idleTicks++;
        int timeout = Math.max(1, currentTimeout);
        if (idleTicks >= timeout) {
            reset();
        }
    }

    public static void update(Rotation rotation, float turnSpeed, int timeout, int priority) {
        if (rotation == null || mc.player == null) {
            return;
        }

        if (currentPriority <= priority) {
            if (currentTask == RotationTask.IDLE) {
                FreeLookHandler.setActive(true);
            }

            currentTurnSpeed = turnSpeed;
            currentTimeout = Math.max(1, timeout);
            currentPriority = priority;
            currentTask = RotationTask.AIM;
            updateRotation(rotation, turnSpeed);
        }
    }

    private static boolean updateRotation(Rotation rotation, float turnSpeed) {
        if (rotation == null || mc.player == null) {
            return false;
        }
        Minecraft var10002 = mc;
        Rotation currentRotation = new Rotation(Minecraft.player);
        float yawDelta = MathHelper.wrapDegrees(rotation.getYaw() - currentRotation.getYaw());
        float pitchDelta = rotation.getPitch() - currentRotation.getPitch();
        float totalDelta = Math.abs(yawDelta) + Math.abs(pitchDelta);
        float yawSpeed = totalDelta == 0.0F ? 0.0F : Math.abs(yawDelta / totalDelta) * turnSpeed;
        float pitchSpeed = totalDelta == 0.0F ? 0.0F : Math.abs(pitchDelta / totalDelta) * turnSpeed;
        Minecraft var10000 = mc;
        ClientPlayerEntity var9 = Minecraft.player;
        var9.rotationYaw += SensUtils.getSensitivity(MathHelper.clamp(yawDelta, -yawSpeed, yawSpeed));
        Minecraft var10 = mc;
        Minecraft var10001 = mc;
        Minecraft.player.rotationPitch = MathHelper.clamp(Minecraft.player.rotationPitch + SensUtils.getSensitivity(MathHelper.clamp(pitchDelta, -pitchSpeed, pitchSpeed)), -90.0F, 90.0F);
        var10002 = mc;
        Rotation finalRotation = new Rotation(Minecraft.player);
        idleTicks = 0;
        return finalRotation.getDelta(rotation) < (double)currentTurnSpeed;
    }

    private static void reset() {
        currentTask = RotationTask.IDLE;
        currentTurnSpeed = 0.0F;
        currentPriority = 0;
        currentTimeout = 0;
        idleTicks = 0;
        FreeLookHandler.setActive(false);
    }

    static {
        currentTask = RotationTask.IDLE;
    }

    public enum RotationTask {
        IDLE,
        AIM,
        RESET
    }
}
