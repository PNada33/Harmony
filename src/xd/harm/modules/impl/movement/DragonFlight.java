package xd.harm.modules.impl.movement;

import xd.harm.events.movement.EventMotion;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.SliderSetting;
import xd.harm.modules.settings.impl.ModeSetting;
import xd.harm.utils.player.MoveUtils;
import com.google.common.eventbus.Subscribe;

@ModuleRegister(name = "DragonFlight", category = Category.Movement, desc = "Улучшает полёт в творческом режиме или на элитрах")
public class DragonFlight extends Module {
    private final ModeSetting mode = new ModeSetting("Мод", "Рилик / Шторм", "Рилик / Шторм", "Кастомный");
    private final SliderSetting speedX = new SliderSetting("Скорость XZ", 2.0f, 1.0f, 5.0f, 0.2f).setVisible(() -> mode.is("Кастомный"));
    private final SliderSetting speedY = new SliderSetting("Скорость Y", 1.0f, 1.0f, 5.0f, 0.2f).setVisible(() -> mode.is("Кастомный"));

    public DragonFlight() {
        addSettings(mode, speedX, speedY);
    }

    @Override
    public boolean onDisable() {
        super.onDisable();

        if (mc.player != null) {
            mc.player.setVelocity(0.0, 0.0, 0.0);
        }
        return false;
    }

    @Subscribe
    public void onMotion(EventMotion event) {

        if (mc.player.abilities.isFlying || mc.player.isElytraFlying()) {
            if (mode.is("Рилик / Шторм")) {
                if (MoveUtils.isMoving()) {
                    boolean isStrafingOrJumping = mc.gameSettings.keyBindLeft.isKeyDown() ||
                            mc.gameSettings.keyBindRight.isKeyDown() ||
                            mc.gameSettings.keyBindJump.isKeyDown();
                    double verticalMotion = mc.gameSettings.keyBindSneak.isKeyDown() ? -0.25 :
                            mc.gameSettings.keyBindJump.isKeyDown() ? 0.25 : 0.0;
                    float speed = isStrafingOrJumping ? 1.03f : 1.1f;
                    MoveUtils.setMotion(speed);
                    mc.player.setVelocity(mc.player.getMotion().x, verticalMotion, mc.player.getMotion().z);
                } else {
                    mc.player.setVelocity(0.0, 0.0, 0.0);
                }
            } else if (mode.is("Кастомный")) {
                double verticalMotion = mc.gameSettings.keyBindSneak.isKeyDown() ? -speedY.get() :
                        mc.gameSettings.keyBindJump.isKeyDown() ? speedY.get() : 0.0;
                if (MoveUtils.isMoving()) {
                    float speed = speedX.get();
                    MoveUtils.setMotion(speed);
                    mc.player.setVelocity(mc.player.getMotion().x, verticalMotion, mc.player.getMotion().z);
                } else {
                    mc.player.setVelocity(0.0, verticalMotion, 0.0);
                }
            }
        }
    }
}
