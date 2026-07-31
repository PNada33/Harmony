package xd.harm.modules.settings.impl;

import xd.harm.modules.settings.Setting;

import java.util.function.Supplier;

public class CategorySetting extends Setting<String> {

    public CategorySetting(String name) {
        super(name, name);
    }

    @Override
    public CategorySetting setVisible(Supplier<Boolean> bool) {
        return (CategorySetting) super.setVisible(bool);
    }
}
