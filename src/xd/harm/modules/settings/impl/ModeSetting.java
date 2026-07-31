package xd.harm.modules.settings.impl;


import java.util.function.Supplier;

import xd.harm.modules.settings.Setting;

public class ModeSetting extends Setting<String> {

    public String[] strings;
    public int optionGap = 0;

    public ModeSetting(String name, String defaultVal, String... strings) {
        super(name, defaultVal);
        this.strings = strings;
    }

    public ModeSetting setOptionGap(int gap) {
        this.optionGap = gap;
        return this;
    }

    public int getIndex() {
        int index = 0;
        for (String val : strings) {
            if (val.equalsIgnoreCase(get())) {
                return index;
            }
            index++;
        }
        return 0;
    }

    public boolean is(String s) {
        return get().equalsIgnoreCase(s);
    }
    @Override
    public ModeSetting setVisible(Supplier<Boolean> bool) {
        return (ModeSetting) super.setVisible(bool);
    }

}