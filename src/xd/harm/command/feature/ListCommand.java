package xd.harm.command.feature;

import xd.harm.command.interfaces.Command;
import xd.harm.command.interfaces.Logger;
import xd.harm.command.interfaces.MultiNamedCommand;
import xd.harm.command.interfaces.Parameters;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.minecraft.util.text.TextFormatting;
import java.util.List;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ListCommand implements Command, MultiNamedCommand {

    final List<Command> commands;
    final Logger logger;

    @Override
    public void execute(Parameters parameters) {
        logger.log(TextFormatting.DARK_GRAY + "┌ " + TextFormatting.WHITE + "Доступные команды " + TextFormatting.DARK_GRAY + "(" + (commands.size() - 1) + ")");
        for (Command command : commands) {
            if (command == this) continue;
            logger.log(TextFormatting.DARK_GRAY + "│ " + TextFormatting.WHITE + command.name() + TextFormatting.DARK_GRAY + " - " + TextFormatting.GRAY + command.description());
        }
        logger.log(TextFormatting.DARK_GRAY + "└───────────────────────");
    }

    @Override
    public String name() {
        return "list";
    }

    @Override
    public String description() {
        return "Список всех команд";
    }

    @Override
    public List<String> aliases() {
        return List.of("", "help", "?");
    }
}