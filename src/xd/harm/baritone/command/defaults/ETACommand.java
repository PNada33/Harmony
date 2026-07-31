package xd.harm.baritone.command.defaults;

import xd.harm.baritone.api.IBaritone;
import xd.harm.baritone.api.behavior.IPathingBehavior;
import xd.harm.baritone.api.command.Command;
import xd.harm.baritone.api.command.argument.IArgConsumer;
import xd.harm.baritone.api.command.exception.CommandException;
import xd.harm.baritone.api.command.exception.CommandInvalidStateException;
import xd.harm.baritone.api.pathing.calc.IPathingControlManager;
import xd.harm.baritone.api.process.IBaritoneProcess;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class ETACommand extends Command {

    public ETACommand(IBaritone baritone) {
        super(baritone, "eta");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        args.requireMax(0);
        IPathingControlManager pathingControlManager = baritone.getPathingControlManager();
        IBaritoneProcess process = pathingControlManager.mostRecentInControl().orElse(null);
        if (process == null) {
            throw new CommandInvalidStateException("Нет процесса в управлении");
        }
        IPathingBehavior pathingBehavior = baritone.getPathingBehavior();

        double ticksRemainingInSegment = pathingBehavior.ticksRemainingInSegment().orElse(Double.NaN);
        double ticksRemainingInGoal = pathingBehavior.estimatedTicksToGoal().orElse(Double.NaN);

        logDirect(String.format(
                "Следующий сегмент: %.1fс (%.0f тиков)\n" +
                        "Цель: %.1fс (%.0f тиков)",
                ticksRemainingInSegment / 20, // мы просто предполагаем, что TPS равен 20, не стоит усилий для точного расчета
                ticksRemainingInSegment,
                ticksRemainingInGoal / 20,
                ticksRemainingInGoal
        ));
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) {
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Просмотр текущего ETA";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Команда ETA предоставляет информацию об оценочном времени до следующего сегмента.",
                "и до цели",
                "",
                "Имейте в виду, что ETA до вашей цели действительно неточен",
                "",
                "Использование:",
                "> eta - Просмотр ETA, если присутствует"
        );
    }
}