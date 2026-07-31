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
import xd.harm.baritone.api.command.datatypes.RelativeFile;
import xd.harm.baritone.api.command.exception.CommandException;
import xd.harm.baritone.api.command.exception.CommandInvalidStateException;
import xd.harm.baritone.api.command.exception.CommandInvalidTypeException;
import com.google.gson.JsonSyntaxException;

import java.io.File;
import java.nio.file.NoSuchFileException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class ExploreFilterCommand extends Command {

    public ExploreFilterCommand(IBaritone baritone) {
        super(baritone, "explorefilter");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        args.requireMax(2);
        File file = args.getDatatypePost(RelativeFile.INSTANCE, mc.gameDir.getAbsoluteFile().getParentFile());
        boolean invert = false;
        if (args.hasAny()) {
            if (args.getString().equalsIgnoreCase("invert")) {
                invert = true;
            } else {
                throw new CommandInvalidTypeException(args.consumed(), "либо \"invert\", либо ничего");
            }
        }
        try {
            baritone.getExploreProcess().applyJsonFilter(file.toPath().toAbsolutePath(), invert);
        } catch (NoSuchFileException e) {
            throw new CommandInvalidStateException("Файл не найден");
        } catch (JsonSyntaxException e) {
            throw new CommandInvalidStateException("Неверный синтаксис JSON");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        logDirect(String.format("Фильтр исследования применен. Инвертирован: %s", Boolean.toString(invert)));
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (args.hasExactlyOne()) {
            return RelativeFile.tabComplete(args, RelativeFile.gameDir());
        }
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Исследование чанков из JSON";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Применяет фильтр исследования перед использованием команды explore, указывая, какие чанки исследованы/не исследованы.",
                "",
                "JSON-файл должен соответствовать формату: [{\"x\":0,\"z\":0},...]",
                "",
                "Если указано 'invert', перечисленные чанки будут считаться НЕ исследованными, а не исследованными.",
                "",
                "Использование:",
                "> explorefilter <путь> [invert] - Загружает JSON-файл по указанному пути. Если указано invert, это должно быть слово 'invert'."
        );
    }
}