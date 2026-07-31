package xd.harm.baritone.command.defaults;

import xd.harm.baritone.api.IBaritone;
import xd.harm.baritone.api.command.Command;
import xd.harm.baritone.api.command.argument.IArgConsumer;
import xd.harm.baritone.api.command.exception.CommandException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class ReloadAllCommand extends Command {

    public ReloadAllCommand(IBaritone baritone) {
        super(baritone, "reloadall");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        args.requireMax(0);
        ctx.worldData().getCachedWorld().reloadAllFromDisk();
        logDirect("Перезагружено");
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) {
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Перезагружает кэш Baritone для этого мира";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Команда reloadall перезагружает кэш мира Baritone.",
                "",
                "Использование:",
                "> reloadall"
        );
    }
}