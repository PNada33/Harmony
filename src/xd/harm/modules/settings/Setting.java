package xd.harm.modules.settings;

import java.util.function.Supplier;

public class Setting<Value> implements ISetting {

    public Value defaultVal;

    String settingName;
    String configKey;
    public Supplier<Boolean> visible = () -> true;
    public boolean sticky = false;

    public Setting<Value> setSticky(boolean sticky) {
        this.sticky = sticky;
        return this;
    }
    public boolean isSticky() {
        return sticky;
    }

    public Setting(String name, Value defaultVal) {
        this.settingName = name;
        this.configKey = name;
        this.defaultVal = defaultVal;
    }

    public String getName() {
        return settingName;
    }

    public String getConfigKey() {
        return configKey;
    }

    public Setting<Value> setConfigKey(String configKey) {
        this.configKey = configKey;
        return this;
    }

    public void set(Value value) {
        defaultVal = value;
    }

    @Override
    public Setting<?> setVisible(Supplier<Boolean> bool) {
        visible = bool;
        return this;
    }

    public Value get() {
        return defaultVal;
    }

    public boolean getBool() {
        return (boolean) defaultVal;
    }

    public float getFloat() {
        return (float) defaultVal;
    }

    public int getInt() {
        return ((Float) defaultVal).intValue();
    }

    public void set(int i, boolean b) {
    }
}
