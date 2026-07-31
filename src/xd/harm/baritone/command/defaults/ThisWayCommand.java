package xd.harm.baritone.command.defaults;

import xd.harm.baritone.api.IBaritone;
import xd.harm.baritone.api.command.Command;
import xd.harm.baritone.api.command.argument.IArgConsumer;
import xd.harm.baritone.api.command.exception.CommandException;
import xd.harm.baritone.api.pathing.goals.GoalXZ;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class ThisWayCommand extends Command {

    public ThisWayCommand(IBaritone baritone) {
        super(baritone, "thisway", "forward");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        args.requireExactly(1);
        GoalXZ goal = GoalXZ.fromDirection(
                ctx.playerFeetAsVec(),
                ctx.player().rotationYawHead,
                args.getAs(Double.class)
        );
        baritone.getCustomGoalProcess().setGoal(goal);
        logDirect(String.format("Цель: %s", goal));
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) {
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Путешествие в текущем направлении";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Создает GoalXZ на заданное количество блоков в направлении, куда вы смотрите",
                "",
                "Использование:",
                "> thisway <расстояние> - создает GoalXZ на указанное расстояние блоков впереди вас"
        );
    }
}