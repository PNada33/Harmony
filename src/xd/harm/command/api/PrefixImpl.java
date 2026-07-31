package xd.harm.command.api;

import xd.harm.command.interfaces.Prefix;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class PrefixImpl implements Prefix {
    public String prefix = ".";

    @Override
    public void set(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public String get() {
        return prefix;
    }
}
