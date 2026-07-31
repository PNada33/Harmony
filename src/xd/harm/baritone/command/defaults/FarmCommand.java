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
import xd.harm.baritone.api.cache.IWaypoint;
import xd.harm.baritone.api.command.Command;
import xd.harm.baritone.api.command.argument.IArgConsumer;
import xd.harm.baritone.api.command.datatypes.ForWaypoints;
import xd.harm.baritone.api.command.exception.CommandException;
import xd.harm.baritone.api.command.exception.CommandInvalidStateException;
import xd.harm.baritone.api.utils.BetterBlockPos;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class FarmCommand extends Command {

    public FarmCommand(IBaritone baritone) {
        super(baritone, "farm");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        args.requireMax(2);
        int range = 0;
        BetterBlockPos origin = null;
        if (args.has(1)) {
            range = args.getAs(Integer.class);
        }
        if (args.has(1)) {
            IWaypoint[] waypoints = args.getDatatypeFor(ForWaypoints.INSTANCE);
            IWaypoint waypoint = null;
            switch (waypoints.length) {
                case 0:
                    throw new CommandInvalidStateException("Путевые точки не найдены");
                case 1:
                    waypoint = waypoints[0];
                    break;
                default:
                    throw new CommandInvalidStateException("Найдено несколько путевых точек");
            }
            origin = waypoint.getLocation();
        }

        baritone.getFarmProcess().farm(range, origin);
        logDirect("Сбор урожая");
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) {
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Сбор урожая поблизости";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Команда farm начинает сбор урожая поблизости. Она собирает созревшие культуры и сажает новые.",
                "",
                "Использование:",
                "> farm - собирает все доступные культуры.",
                "> farm <диапазон> - собирает культуры в указанном диапазоне от начальной позиции.",
                "> farm <диапазон> <путевая точка> - собирает культуры в указанном диапазоне от путевой точки."
        );
    }
}