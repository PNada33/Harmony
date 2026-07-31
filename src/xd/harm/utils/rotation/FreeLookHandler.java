




package xd.harm.utils.rotation;

import com.google.common.eventbus.Subscribe;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.MathHelper;
import xd.harm.Harmony;
import xd.harm.events.movement.CameraEvent;
import xd.harm.events.movement.EventRotate;
import xd.harm.utils.client.IMinecraft;

public class FreeLookHandler implements IMinecraft {
    private static boolean active;
    private static float freeYaw;
    private static float freePitch;

    public FreeLookHandler() {
        Harmony.getInstance().getEventBus().register(this);
    }

    @Subscribe
    public void onLook(EventRotate e) {
        if (active) {
            this.rotateTowards(e.getYaw(), e.getPitch());
            e.cancel();
        }

    }

    @Subscribe
    public void onCamera(CameraEvent e) {
        if (active) {
            e.yaw = freeYaw;
            e.pitch = freePitch;
        } else {
            freeYaw = e.yaw;
            freePitch = e.pitch;
        }

    }

    public static void setActive(boolean state) {
        if (active != state) {
            active = state;
            resetRotation();
        }

    }

    private void rotateTowards(double yaw, double pitch) {
        double d0 = pitch * 0.15;
        double d1 = yaw * 0.15;
        freePitch = (float)((double)freePitch + d0);
        freeYaw = (float)((double)freeYaw + d1);
        freePitch = MathHelper.clamp(freePitch, -90.0F, 90.0F);
    }

    private static void resetRotation() {
        if (mc.player == null) {
            return;
        }
        Minecraft var10000 = mc;
        Minecraft.player.rotationYaw = freeYaw;
        var10000 = mc;
        Minecraft.player.rotationPitch = freePitch;
    }

    public static boolean isActive() {
        return active;
    }

    public static float getFreeYaw() {
        return freeYaw;
    }

    public static float getFreePitch() {
        return freePitch;
    }
}

