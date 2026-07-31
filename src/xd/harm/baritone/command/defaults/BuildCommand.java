package xd.harm.baritone.command.defaults;

import xd.harm.baritone.Baritone;
import xd.harm.baritone.api.IBaritone;
import xd.harm.baritone.api.command.Command;
import xd.harm.baritone.api.command.argument.IArgConsumer;
import xd.harm.baritone.api.command.datatypes.RelativeBlockPos;
import xd.harm.baritone.api.command.datatypes.RelativeFile;
import xd.harm.baritone.api.command.exception.CommandException;
import xd.harm.baritone.api.command.exception.CommandInvalidStateException;
import xd.harm.baritone.api.utils.BetterBlockPos;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class BuildCommand extends Command {

    private static final File schematicsDir = new File(mc.gameDir, "schematics");

    public BuildCommand(IBaritone baritone) {
        super(baritone, "build");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        File file = args.getDatatypePost(RelativeFile.INSTANCE, schematicsDir).getAbsoluteFile();
        if (FilenameUtils.getExtension(file.getAbsolutePath()).isEmpty()) {
            file = new File(file.getAbsolutePath() + "." + Baritone.settings().schematicFallbackExtension.value);
        }
        BetterBlockPos origin = ctx.playerFeet();
        BetterBlockPos buildOrigin;
        if (args.hasAny()) {
            args.requireMax(3);
            buildOrigin = args.getDatatypePost(RelativeBlockPos.INSTANCE, origin);
        } else {
            args.requireMax(0);
            buildOrigin = origin;
        }
        boolean success = baritone.getBuilderProcess().build(file.getName(), file, buildOrigin);
        if (!success) {
            throw new CommandInvalidStateException("Не удалось загрузить схему. Убедитесь, что используется ПОЛНОЕ имя файла, включая расширение (например, blah.schematic).");
        }
        logDirect(String.format("Успешно загружена схема для строительства\nНачало: %s", buildOrigin));
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (args.hasExactlyOne()) {
            return RelativeFile.tabComplete(args, schematicsDir);
        } else if (args.has(2)) {
            args.get();
            return args.tabCompleteDatatype(RelativeBlockPos.INSTANCE);
        }
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Построить схему";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Построить схему из файла.",
                "",
                "Использование:",
                "> build <имя_файла> - Загружает и строит '<имя_файла>.schematic'",
                "> build <имя_файла> <x> <y> <z> - Пользовательская позиция"
        );
    }
}