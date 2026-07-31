/*
 * Этот файл является частью Baritone.
 *
 * Baritone является свободным программным обеспечением: вы можете распространять его и/или изменять
 * в соответствии с условиями GNU Lesser General Public License, опубликованной
 * Free Software Foundation, либо версии 3 лицензии, либо
 * (по вашему выбору) любой более поздней версии.
 *
 * Baritone распространяется в надежде, что он будет полезен,
 * но БЕЗ КАКИХ-ЛИБО ГАРАНТИЙ; даже без подразумеваемых гарантий
 * КОММЕРЧЕСКОЙ ЦЕННОСТИ или ПРИГОДНОСТИ ДЛЯ ОПРЕДЕЛЕННОЙ ЦЕЛИ. См.
 * GNU Lesser General Public License для получения дополнительной информации.
 *
 * Вы должны были получить копию GNU Lesser General Public License
 * вместе с Baritone. Если нет, см. <https://www.gnu.org/licenses/>.
 */

package xd.harm.baritone.command.defaults;

import xd.harm.baritone.api.IBaritone;
import xd.harm.baritone.api.command.Command;
import xd.harm.baritone.api.command.argument.IArgConsumer;
import xd.harm.baritone.api.command.exception.CommandException;
import xd.harm.baritone.api.pathing.goals.Goal;
import xd.harm.baritone.api.pathing.goals.GoalAxis;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class AxisCommand extends Command {

    public AxisCommand(IBaritone baritone) {
        super(baritone, "axis", "highway");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        args.requireMax(0);
        Goal goal = new GoalAxis();
        baritone.getCustomGoalProcess().setGoal(goal);
        logDirect(String.format("Цель: %s", goal.toString()));
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) {
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Установить цель на оси";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Команда axis устанавливает цель, которая указывает Baritone двигаться к ближайшей оси. То есть X=0 или Z=0.",
                "",
                "Использование:",
                "> axis"
        );
    }
}