package xd.harm.baritone.command.defaults;

import xd.harm.baritone.api.IBaritone;
import xd.harm.baritone.api.command.Command;
import xd.harm.baritone.api.command.argument.IArgConsumer;
import xd.harm.baritone.api.command.exception.CommandException;
import xd.harm.baritone.api.pathing.goals.Goal;
import xd.harm.baritone.api.pathing.goals.GoalBlock;
import xd.harm.baritone.api.utils.BetterBlockPos;
import net.minecraft.block.AirBlock;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class SurfaceCommand extends Command {

    protected SurfaceCommand(IBaritone baritone) {
        super(baritone, "surface", "top");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        final BetterBlockPos playerPos = baritone.getPlayerContext().playerFeet();
        final int surfaceLevel = baritone.getPlayerContext().world().getSeaLevel();
        final int worldHeight = baritone.getPlayerContext().world().getHeight();

        if (playerPos.getY() > surfaceLevel && mc.world.getBlockState(playerPos.up()).getBlock() instanceof AirBlock) {
            logDirect("Уже на поверхности");
            return;
        }

        final int startingYPos = Math.max(playerPos.getY(), surfaceLevel);

        for (int currentIteratedY = startingYPos; currentIteratedY < worldHeight; currentIteratedY++) {
            final BetterBlockPos newPos = new BetterBlockPos(playerPos.getX(), currentIteratedY, playerPos.getZ());

            if (!(mc.world.getBlockState(newPos).getBlock() instanceof AirBlock) && newPos.getY() > playerPos.getY()) {
                Goal goal = new GoalBlock(newPos.up());
                logDirect(String.format("Иду к: %s", goal.toString()));
                baritone.getCustomGoalProcess().setGoalAndPath(goal);
                return;
            }
        }
        logDirect("Более высокое место не найдено");
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) {
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Используется для выхода из пещер, шахт, ...";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Команда surface/top указывает Baritone двигаться к ближайшей поверхности.",
                "",
                "Это может быть поверхность или самое высокое доступное воздушное пространство, в зависимости от обстоятельств.",
                "",
                "Использование:",
                "> surface - Используется для выхода из пещер, шахт, ...",
                "> top - Используется для выхода из пещер, шахт, ..."
        );
    }
}