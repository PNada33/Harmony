package xd.harm.baritone.command.defaults;

import xd.harm.baritone.api.IBaritone;
import xd.harm.baritone.api.command.Command;
import xd.harm.baritone.api.command.argument.IArgConsumer;
import xd.harm.baritone.api.command.exception.CommandException;
import xd.harm.baritone.api.pathing.goals.Goal;
import xd.harm.baritone.api.pathing.goals.GoalStrictDirection;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class TunnelCommand extends Command {

    public TunnelCommand(IBaritone baritone) {
        super(baritone, "tunnel");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        args.requireMax(3);
        if (args.hasExactly(3)) {
            boolean cont = true;
            int height = Integer.parseInt(args.getArgs().get(0).getValue());
            int width = Integer.parseInt(args.getArgs().get(1).getValue());
            int depth = Integer.parseInt(args.getArgs().get(2).getValue());

            if (width < 1 || height < 2 || depth < 1 || height > 255) {
                logDirect("Ширина и глубина должны быть не менее 1 блока; высота должна быть не менее 2 блоков и не может превышать лимит строительства.");
                cont = false;
            }

            if (cont) {
                height--;
                width--;
                BlockPos corner1;
                BlockPos corner2;
                Direction enumFacing = ctx.player().getHorizontalFacing();
                int addition = ((width % 2 == 0) ? 0 : 1);
                switch (enumFacing) {
                    case EAST:
                        corner1 = new BlockPos(ctx.playerFeet().x, ctx.playerFeet().y, ctx.playerFeet().z - width / 2);
                        corner2 = new BlockPos(ctx.playerFeet().x + depth, ctx.playerFeet().y + height, ctx.playerFeet().z + width / 2 + addition);
                        break;
                    case WEST:
                        corner1 = new BlockPos(ctx.playerFeet().x, ctx.playerFeet().y, ctx.playerFeet().z + width / 2 + addition);
                        corner2 = new BlockPos(ctx.playerFeet().x - depth, ctx.playerFeet().y + height, ctx.playerFeet().z - width / 2);
                        break;
                    case NORTH:
                        corner1 = new BlockPos(ctx.playerFeet().x - width / 2, ctx.playerFeet().y, ctx.playerFeet().z);
                        corner2 = new BlockPos(ctx.playerFeet().x + width / 2 + addition, ctx.playerFeet().y + height, ctx.playerFeet().z - depth);
                        break;
                    case SOUTH:
                        corner1 = new BlockPos(ctx.playerFeet().x + width / 2 + addition, ctx.playerFeet().y, ctx.playerFeet().z);
                        corner2 = new BlockPos(ctx.playerFeet().x - width / 2, ctx.playerFeet().y + height, ctx.playerFeet().z + depth);
                        break;
                    default:
                        throw new IllegalStateException("Неожиданное значение: " + enumFacing);
                }
                logDirect(String.format("Создание туннеля высотой %s блока(-ов), шириной %s блока(-ов) и глубиной %s блока(-ов)", height + 1, width + 1, depth));
                baritone.getBuilderProcess().clearArea(corner1, corner2);
            }
        } else {
            Goal goal = new GoalStrictDirection(
                    ctx.playerFeet(),
                    ctx.player().getHorizontalFacing()
            );
            baritone.getCustomGoalProcess().setGoalAndPath(goal);
            logDirect(String.format("Цель: %s", goal.toString()));
        }
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) {
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Установить цель для прокладки туннеля в текущем направлении";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Команда tunnel задает цель, которая указывает Baritone копать прямо в направлении, куда вы смотрите.",
                "",
                "Использование:",
                "> tunnel - Без аргументов, копает в радиусе 1x2.",
                "> tunnel <высота> <ширина> <глубина> - Прокладывает туннель с заданными пользователем высотой, шириной и глубиной."
        );
    }
}