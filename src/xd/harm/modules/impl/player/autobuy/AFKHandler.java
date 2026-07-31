package xd.harm.modules.impl.player.autobuy;

import net.minecraft.util.math.vector.Vector3d;
import xd.harm.utils.client.IMinecraft;
import xd.harm.utils.client.TimerUtility;

public class AFKHandler implements IMinecraft {
    private boolean isHandlingAFK = false;
    private final TimerUtility afkTimer = new TimerUtility();
    private int afkStep = 0;
    private float originalYaw;
    private float originalPitch;
    private Vector3d originalPos;

    public void handleAFKDetection() {
        if (mc.player == null || mc.world == null) return;

        isHandlingAFK = true;
        afkStep = 0;
        afkTimer.reset();

        originalYaw = mc.player.rotationYaw;
        originalPitch = mc.player.rotationPitch;
        originalPos = mc.player.getPositionVec();

        if (mc.currentScreen != null) {
            mc.player.closeScreen();
        }
    }

    public void tick() {
        if (!isHandlingAFK) return;

        if (mc.player == null || mc.world == null) {
            isHandlingAFK = false;
            return;
        }

        switch (afkStep) {
            case 0:
                if (afkTimer.hasTimeElapsed(300)) {
                    afkStep = 1;
                    afkTimer.reset();
                }
                break;
            case 1:
                mc.gameSettings.keyBindForward.setPressed(true);
                afkStep = 2;
                afkTimer.reset();
                break;
            case 2:
                if (afkTimer.hasTimeElapsed(500)) {
                    mc.gameSettings.keyBindForward.setPressed(false);
                    afkStep = 3;
                    afkTimer.reset();
                }
                break;
            case 3:
                mc.player.rotationYaw -= 15;
                afkStep = 4;
                afkTimer.reset();
                break;
            case 4:
                if (afkTimer.hasTimeElapsed(200)) {
                    afkStep = 5;
                    afkTimer.reset();
                }
                break;
            case 5:
                mc.player.rotationYaw += 30;
                afkStep = 6;
                afkTimer.reset();
                break;
            case 6:
                if (afkTimer.hasTimeElapsed(200)) {
                    afkStep = 7;
                    afkTimer.reset();
                }
                break;
            case 7:
                mc.player.jump();
                afkStep = 8;
                afkTimer.reset();
                break;
            case 8:
                if (afkTimer.hasTimeElapsed(400)) {
                    afkStep = 9;
                    afkTimer.reset();
                }
                break;
            case 9:
                mc.gameSettings.keyBindBack.setPressed(true);
                afkStep = 10;
                afkTimer.reset();
                break;
            case 10:
                if (afkTimer.hasTimeElapsed(500)) {
                    mc.gameSettings.keyBindBack.setPressed(false);
                    afkStep = 11;
                    afkTimer.reset();
                }
                break;
            case 11:
                mc.player.rotationYaw = originalYaw;
                mc.player.rotationPitch = originalPitch;
                afkStep = 12;
                afkTimer.reset();
                break;
            case 12:
                mc.player.jump();
                afkStep = 13;
                afkTimer.reset();
                break;
            case 13:
                if (afkTimer.hasTimeElapsed(500)) {
                    afkStep = 14;
                    afkTimer.reset();
                }
                break;
            case 14:
                mc.gameSettings.keyBindLeft.setPressed(true);
                afkStep = 15;
                afkTimer.reset();
                break;
            case 15:
                if (afkTimer.hasTimeElapsed(300)) {
                    mc.gameSettings.keyBindLeft.setPressed(false);
                    afkStep = 16;
                    afkTimer.reset();
                }
                break;
            case 16:
                mc.gameSettings.keyBindRight.setPressed(true);
                afkStep = 17;
                afkTimer.reset();
                break;
            case 17:
                if (afkTimer.hasTimeElapsed(300)) {
                    mc.gameSettings.keyBindRight.setPressed(false);
                    afkStep = 18;
                    afkTimer.reset();
                }
                break;
            case 18:
                mc.player.jump();
                afkStep = 19;
                afkTimer.reset();
                break;
            case 19:
                if (afkTimer.hasTimeElapsed(5000)) {
                    afkStep = 20;
                    afkTimer.reset();
                }
                break;
            case 20:
                mc.player.sendChatMessage("/ah");
                afkStep = 21;
                afkTimer.reset();
                break;
            case 21:
                if (afkTimer.hasTimeElapsed(1000)) {
                    mc.gameSettings.keyBindForward.setPressed(false);
                    mc.gameSettings.keyBindBack.setPressed(false);
                    mc.gameSettings.keyBindLeft.setPressed(false);
                    mc.gameSettings.keyBindRight.setPressed(false);
                    isHandlingAFK = false;
                    afkStep = 0;
                }
                break;
        }
    }

    public boolean isHandling() {
        return isHandlingAFK;
    }
}
