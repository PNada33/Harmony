package xd.harm.command.api;

import xd.harm.command.interfaces.*;

import lombok.AccessLevel;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import net.minecraft.util.text.TextFormatting;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class StandaloneCommandDispatcher implements CommandDispatcher, CommandProvider {
    private static final String DELIMITER = " ";
    final Prefix prefix;
    final ParametersFactory parametersFactory;
    final Logger logger;
    final Map<String, Command> aliasToCommandMap;

    public StandaloneCommandDispatcher(List<Command> commands, Prefix prefix, ParametersFactory parametersFactory, Logger logger) {
        this.prefix = prefix;
        this.parametersFactory = parametersFactory;
        this.logger = logger;
        aliasToCommandMap = commandsToAliasToCommandMap(commands);
    }

    @Override
    public DispatchResult dispatch(String message) {
        String prefix = this.prefix.get();

        if (!message.startsWith(prefix)) {
            return DispatchResult.NOT_DISPATCHED;
        }

        String[] split = message.split(DELIMITER);
        String commandName = split[0].substring(prefix.length());
        Command command = aliasToCommandMap.get(commandName);

        try {
            String parameters = extractParametersFromMessage(message, split);
            command.execute(parametersFactory.createParameters(parameters, DELIMITER));
        } catch (Exception e) {
            handleCommandException(e, command, commandName);
        }
        return DispatchResult.DISPATCHED;
    }

    @Override
    public Command command(String alias) {
        return aliasToCommandMap.get(alias);
    }

    private Map<String, Command> commandsToAliasToCommandMap(List<Command> commands) {
        return commands.stream().flatMap(this::commandToWrappedCommandStream).collect(Collectors.toMap(FlatMapCommand::getAlias, FlatMapCommand::getCommand));
    }

    private Stream<FlatMapCommand> commandToWrappedCommandStream(Command command) {
        Stream<FlatMapCommand> wrappedCommandStream = Stream.of(new FlatMapCommand(command.name(), command));
        if (command instanceof MultiNamedCommand multiNamedCommand) {
            return Stream.concat(wrappedCommandStream, multiNamedCommand.aliases().stream().map(alias -> new FlatMapCommand(alias, command)));
        }
        return wrappedCommandStream;
    }

    private void handleCommandException(Exception e, Command command, String commandName) {
        if (e instanceof CommandException) {
            logger.log(e.getMessage());
        } else {
            if (e instanceof NullPointerException) {
                logger.log(TextFormatting.RED + "× " + TextFormatting.RED + "Команда " + TextFormatting.WHITE + commandName + TextFormatting.RED + " не найдена");
                logger.log(TextFormatting.DARK_GRAY + "  " + TextFormatting.GRAY + "Используйте " + TextFormatting.WHITE + prefix.get() + "help" + TextFormatting.GRAY + " для списка команд");
            } else {
                logger.log(TextFormatting.DARK_GRAY + "⚠ " + TextFormatting.RED + "Ошибка выполнения команды");
                if (e.getMessage() != null && !e.getMessage().isEmpty()) {
                    logger.log(TextFormatting.DARK_GRAY + "  " + TextFormatting.GRAY + "Причина: " + TextFormatting.WHITE + e.getMessage());
                }
            }
        }
    }

    private String extractParametersFromMessage(String message, String[] split) {
        return message.substring((split.length != 1 ? DELIMITER.length() : 0) + split[0].length());
    }

    @Value
    private static class FlatMapCommand {
        String alias;
        Command command;
    }
}