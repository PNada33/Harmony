/*
 * Этот файл является частью Baritone.
 *
 * Baritone — это свободное программное обеспечение: вы можете распространять его и/или изменять
 * в соответствии с условиями GNU Lesser General Public License, опубликованной
 * Free Software Foundation, версия 3 лицензии или (по вашему выбору) любая последующая версия.
 *
 * Baritone распространяется в надежде, что он будет полезен,
 * но БЕЗ КАКИХ-ЛИБО ГАРАНТИЙ; даже без подразумеваемых гарантий
 * КОММЕРЧЕСКОЙ ЦЕННОСТИ или ПРИГОДНОСТИ ДЛЯ ОПРЕДЕЛЕННОЙ ЦЕЛИ. Подробности
 * см. в GNU Lesser General Public License.
 *
 * Вы должны были получить копию GNU Lesser General Public License
 * вместе с Baritone. Если нет, см. <https://www.gnu.org/licenses/>.
 */

package xd.harm.baritone.command.defaults;

import xd.harm.baritone.api.IBaritone;
import xd.harm.baritone.api.command.Command;
import xd.harm.baritone.api.command.argument.IArgConsumer;
import xd.harm.baritone.api.command.datatypes.BlockById;
import xd.harm.baritone.api.command.datatypes.ForBlockOptionalMeta;
import xd.harm.baritone.api.command.datatypes.RelativeCoordinate;
import xd.harm.baritone.api.command.datatypes.RelativeGoal;
import xd.harm.baritone.api.command.exception.CommandException;
import xd.harm.baritone.api.pathing.goals.Goal;
import xd.harm.baritone.api.utils.BetterBlockPos;
import xd.harm.baritone.api.utils.BlockOptionalMeta;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class GotoCommand extends Command {

    protected GotoCommand(IBaritone baritone) {
        super(baritone, "goto");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        if (args.peekDatatypeOrNull(RelativeCoordinate.INSTANCE) != null) {
            args.requireMax(3);
            BetterBlockPos origin = baritone.getPlayerContext().playerFeet();
            Goal goal = args.getDatatypePost(RelativeGoal.INSTANCE, origin);
            logDirect(String.format("Иду к: %s", goal.toString()));
            baritone.getCustomGoalProcess().setGoalAndPath(goal);
            return;
        }
        args.requireMax(1);
        BlockOptionalMeta destination = args.getDatatypeFor(ForBlockOptionalMeta.INSTANCE);
        baritone.getGetToBlockProcess().getToBlock(destination);
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        return args.tabCompleteDatatype(BlockById.INSTANCE);
    }

    @Override
    public String getShortDesc() {
        return "Перейти к координатам или блоку";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Команда goto указывает Baritone двигаться к заданной цели или блоку.",
                "",
                "Везде, где ожидается координата, можно использовать ~, как в обычных командах Minecraft. Или просто указать числа.",
                "",
                "Использование:",
                "> goto <блок> - Перейти к блоку, где бы он ни находился в мире",
                "> goto <y> - Перейти на уровень Y",
                "> goto <x> <z> - Перейти к позиции X,Z",
                "> goto <x> <y> <z> - Перейти к позиции X,Y,Z"
        );
    }
}