package xd.harm.command.interfaces;

import net.minecraft.util.text.IFormattableTextComponent;

public interface Logger2 {
    void log(String message);
    void log(IFormattableTextComponent message);
}