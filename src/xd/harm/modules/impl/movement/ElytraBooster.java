package xd.harm.modules.impl.movement;

import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.ModeSetting;
import xd.harm.modules.settings.impl.SliderSetting;

@ModuleRegister(name = "ElytraBooster", category = Category.Movement, desc = "Ускоряет полет на элитрах")
public class ElytraBooster extends Module {
    public final ModeSetting mode = new ModeSetting("Режим", "Кастомный", "Кастомный", "БравоХВХ", "РиллиВорлд");
    public final BooleanSetting maxspeed = new BooleanSetting("Скорость по углам", false).setVisible(() -> mode.is("Кастомный"));
    public final SliderSetting speedxz = new SliderSetting("Скорость XZ", 1.65f, 1.5f, 2.5f, 0.01f).setVisible(() -> mode.is("Кастомный") && !maxspeed.get());
    public final SliderSetting speedy = new SliderSetting("Скорость Y", 1.59f, 1.5f, 2.5f, 0.01f).setVisible(() -> mode.is("Кастомный") && !maxspeed.get());
    public final SliderSetting speed5 = new SliderSetting("Угол 0-5", 1.6f, 1.5f, 2.5f, 0.01f).setVisible(() -> mode.is("Кастомный") && maxspeed.get());
    public final SliderSetting speed10 = new SliderSetting("Угол 5-10", 1.62f, 1.5f, 2.5f, 0.01f).setVisible(() -> mode.is("Кастомный") && maxspeed.get());
    public final SliderSetting speed15 = new SliderSetting("Угол 10-15", 1.65f, 1.5f, 2.5f, 0.01f).setVisible(() -> mode.is("Кастомный") && maxspeed.get());
    public final SliderSetting speed20 = new SliderSetting("Угол 15-20", 1.68f, 1.5f, 2.5f, 0.01f).setVisible(() -> mode.is("Кастомный") && maxspeed.get());
    public final SliderSetting speed25 = new SliderSetting("Угол 20-25", 1.74f, 1.5f, 2.5f, 0.01f).setVisible(() -> mode.is("Кастомный") && maxspeed.get());
    public final SliderSetting speed30 = new SliderSetting("Угол 25-30", 1.8f, 1.5f, 2.5f, 0.01f).setVisible(() -> mode.is("Кастомный") && maxspeed.get());
    public final SliderSetting speed35 = new SliderSetting("Угол 30-35", 1.8f, 1.5f, 2.5f, 0.01f).setVisible(() -> mode.is("Кастомный") && maxspeed.get());
    public final SliderSetting speed40 = new SliderSetting("Угол 35-40", 1.8f, 1.5f, 2.5f, 0.01f).setVisible(() -> mode.is("Кастомный") && maxspeed.get());
    public final SliderSetting speed5y = new SliderSetting("УголY 0-5", 1.59f, 1.5f, 2.5f, 0.01f).setVisible(() -> mode.is("Кастомный") && maxspeed.get());
    public final SliderSetting speed10y = new SliderSetting("УголY 5-10", 1.6f, 1.5f, 2.5f, 0.01f).setVisible(() -> mode.is("Кастомный") && maxspeed.get());
    public final SliderSetting speed15y = new SliderSetting("УголY 10-15", 1.61f, 1.5f, 2.5f, 0.01f).setVisible(() -> mode.is("Кастомный") && maxspeed.get());
    public final SliderSetting speed20y = new SliderSetting("УголY 15-20", 1.62f, 1.5f, 2.5f, 0.01f).setVisible(() -> mode.is("Кастомный") && maxspeed.get());
    public final SliderSetting speed25y = new SliderSetting("УголY 20-25", 1.68f, 1.5f, 2.5f, 0.01f).setVisible(() -> mode.is("Кастомный") && maxspeed.get());
    public final SliderSetting speed30y = new SliderSetting("УголY 25-30", 1.74f, 1.5f, 2.5f, 0.01f).setVisible(() -> mode.is("Кастомный") && maxspeed.get());
    public final SliderSetting speed35y = new SliderSetting("УголY 30-35", 1.95f, 1.5f, 2.5f, 0.01f).setVisible(() -> mode.is("Кастомный") && maxspeed.get());
    public final SliderSetting speed40y = new SliderSetting("УголY 35-40", 2.0f, 1.5f, 2.5f, 0.01f).setVisible(() -> mode.is("Кастомный") && maxspeed.get());

    public ElytraBooster() {
        addSettings(mode, maxspeed, speedxz, speedy, speed5, speed10, speed15, speed20, speed25, speed30, speed35, speed40, speed5y, speed10y, speed15y, speed20y, speed25y, speed30y, speed35y, speed40y);
    }
}
