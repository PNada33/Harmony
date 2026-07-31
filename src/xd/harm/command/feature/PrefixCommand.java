package xd.harm.command.feature;

import xd.harm.command.api.CommandException;
import xd.harm.command.api.PrefixImpl;
import xd.harm.command.interfaces.*;
import xd.harm.config.PrefixStorage;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.minecraft.util.text.TextFormatting;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PrefixCommand implements Command {

    final Prefix prefix;
    final Logger logger;

    @Override
    public void execute(Parameters parameters) {
        String commandType = parameters.asString(0).orElse("");

        switch (commandType) {
            case "set" -> setPrefix(parameters);
            default -> throw new CommandException(TextFormatting.RED + "× " + TextFormatting.WHITE + "Используйте: " + TextFormatting.GRAY + "set <символ>");
        }
    }

    public void setPrefix(Parameters parameters) {
        String prefixSymbol = parameters.asString(1).orElseThrow(() ->
                new CommandException(TextFormatting.RED + "× " + TextFormatting.RED + "Укажите новый префикс"));
        PrefixImpl prefixSet = new PrefixImpl();
        prefixSet.set(prefixSymbol);
        PrefixStorage.updatePrefix(prefixSymbol);
        logger.log(TextFormatting.DARK_GRAY + "" + TextFormatting.GREEN + "✓ " + TextFormatting.WHITE + "Новый префикс: " + prefixSymbol);
    }

    @Override
    public String name() {
        return "prefix";
    }

    @Override
    public String description() {
        return "Смена префикса команд";
    }
}