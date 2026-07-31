package xd.harm.utils.client;

import com.google.common.eventbus.Subscribe;
import net.minecraft.network.play.client.CCloseWindowPacket;
import xd.harm.Harmony;
import xd.harm.events.input.EventInput;

public class ServiceUtil implements IMinecraft {
    public static boolean swag;

    public ServiceUtil() {
        Harmony.getInstance().getEventBus().register(this);
    }

    public static void initAutoStop(Runnable clickTasker, int delay, int postdelay) {
        swag = true;
        (new Thread(() -> {
            try {
                Thread.sleep((long)delay);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            clickTasker.run();

            try {
                Thread.sleep((long)postdelay);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            if (mc.currentScreen == null) {
                mc.player.connection.sendPacket(new CCloseWindowPacket(0));
            }

            swag = false;
        })).start();
    }

    @Subscribe
    private void onMoveInput(EventInput e) {
        if (swag) {
            e.setForward(0.0F);
            e.setStrafe(0.0F);
        }

    }
}

