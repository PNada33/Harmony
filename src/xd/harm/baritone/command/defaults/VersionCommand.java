package xd.harm.baritone.command.defaults;

import xd.harm.baritone.api.IBaritone;
import xd.harm.baritone.api.command.Command;
import xd.harm.baritone.api.command.argument.IArgConsumer;
import xd.harm.baritone.api.command.exception.CommandException;
import xd.harm.baritone.api.command.exception.CommandInvalidStateException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class VersionCommand extends Command {

    public VersionCommand(IBaritone baritone) {
        super(baritone, "version");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        args.requireMax(0);
        String version = getClass().getPackage().getImplementationVersion();
        if (version == null) {
            throw new CommandInvalidStateException("Version -> harmony.baritone.version 1.0");
        } else {
            logDirect(String.format("Вы используете Baritone v%s", version));
        }
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) {
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Просмотр версии Baritone";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Команда version выводит версию Baritone, которую вы сейчас используете.",
                "",
                "Использование:",
                "> version - Просмотр информации о версии, если она доступна"
        );
    }
}