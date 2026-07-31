package xd.harm.modules.impl.render;

import com.google.common.eventbus.Subscribe;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.ModeSetting;
import xd.harm.modules.settings.impl.SliderSetting;
import xd.harm.utils.animations.easing.CompactAnimation;
import xd.harm.utils.animations.easing.Easing;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;

@ModuleRegister(name = "FullBright", category = Category.Render, desc = "Способность видеть в темноте")
public class FullBright extends Module {
    private final ModeSetting mode = new ModeSetting("Мод", "Гамма", "Гамма", "Зелье");
    private final CompactAnimation animation = new CompactAnimation(Easing.EASE_OUT_QUART, 500);
    private final BooleanSetting dynamic = new BooleanSetting("Динамический", false).setVisible(() -> mode.is("Гамма"));
    final SliderSetting elytraRange = new SliderSetting("Диномичиская Яркость", 1f, 0.1f, 1f, 0.1f).setVisible(() ->dynamic.get());
    private final SliderSetting bright = new SliderSetting("Яркость", 2.5f, 0.1f, 5, 0.1f).setVisible(() -> !dynamic.get() && mode.is("Гамма"));

    private float originalGamma;
    private boolean isGammaChanged = false;

    public FullBright() {
        addSettings(mode, dynamic, bright,elytraRange);
    }

    @Override
    public boolean onEnable() {
        super.onEnable();
        saveGamma();
        return false;
    }

    @Override
    public boolean onDisable() {
        super.onDisable();
        restoreGamma();
        if (mc.player != null) {
            mc.player.removeActivePotionEffect(Effects.NIGHT_VISION);
        }
        return false;
    }

    @Subscribe
    public void onUpdate(EventUpdate e) {
        if (mc.player == null || mc.gameSettings == null) {
            return;
        }
        if (mode.is("Гамма")) {
            mc.player.removeActivePotionEffect(Effects.NIGHT_VISION);
            if (dynamic.get()) {
                float lightLevel = mc.player.getBrightness();
                animation.run(calculateGamma(lightLevel));
                float gamma = (float) animation.getValue();
                setGamma(gamma);
            } else {
                setGamma(bright.get());
            }
        } else {
            EffectInstance activeNightVision = mc.player.getActivePotionEffect(Effects.NIGHT_VISION);
            if (activeNightVision == null || activeNightVision.getDuration() < 200) {
                mc.player.addPotionEffect(new EffectInstance(Effects.NIGHT_VISION, 16360, 0));
            }
        }
    }

    private float calculateGamma(float lightLevel) {
        float minGamma = elytraRange.get();
        return minGamma + ((5.0f - minGamma) * (1.0f - lightLevel));
    }

    public void saveGamma() {
        originalGamma = (float) mc.gameSettings.gamma;
    }

    public void setGamma(float newGamma) {
        if (Float.compare((float) mc.gameSettings.gamma, newGamma) == 0) {
            return;
        }
        mc.gameSettings.gamma = newGamma;
        isGammaChanged = true;
    }

    public void restoreGamma() {
        if (isGammaChanged) {
            mc.gameSettings.gamma = originalGamma;
            isGammaChanged = false;
        }
    }
}
