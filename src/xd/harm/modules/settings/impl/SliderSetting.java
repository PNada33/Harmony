package xd.harm.modules.settings.impl;


import java.util.function.Supplier;

import xd.harm.modules.settings.Setting;

public class SliderSetting extends Setting<Float> {

    public float min;
    public float max;
    public float increment;

    public SliderSetting(String name, float defaultVal, float min, float max, float increment) {
        super(name, defaultVal);
        this.min = min;
        this.max = max;
        this.increment = increment;
    }

    @Override
    public SliderSetting setVisible(Supplier<Boolean> bool) {
        return (SliderSetting) super.setVisible(bool);
    }
}