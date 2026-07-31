package xd.harm.baritone.command.defaults;

import xd.harm.baritone.api.IBaritone;
import xd.harm.baritone.api.command.Command;
import xd.harm.baritone.api.command.argument.IArgConsumer;
import xd.harm.baritone.api.command.exception.CommandException;
import xd.harm.baritone.api.process.ICustomGoalProcess;
import xd.harm.baritone.cache.WorldScanner;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class PathCommand extends Command {

    public PathCommand(IBaritone baritone) {
        super(baritone, "path");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        ICustomGoalProcess customGoalProcess = baritone.getCustomGoalProcess();
        args.requireMax(0);
        WorldScanner.INSTANCE.repack(ctx);
        customGoalProcess.path();
        logDirect("Теперь прокладывается путь");
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Начать движение к цели";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Команда path указывает Baritone двигаться к текущей цели.",
                "",
                "Использование:",
                "> path - Начать прокладку пути."
        );
    }
}