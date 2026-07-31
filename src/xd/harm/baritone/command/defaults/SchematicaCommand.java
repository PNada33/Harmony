package xd.harm.baritone.command.defaults;

import xd.harm.baritone.api.IBaritone;
import xd.harm.baritone.api.command.Command;
import xd.harm.baritone.api.command.argument.IArgConsumer;
import xd.harm.baritone.api.command.exception.CommandException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class SchematicaCommand extends Command {

    public SchematicaCommand(IBaritone baritone) {
        super(baritone, "schematica");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        args.requireMax(0);
        baritone.getBuilderProcess().buildOpenSchematic();
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) {
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Строит загруженную схему";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Строит схему, открытую в Schematica.",
                "",
                "Использование:",
                "> schematica"
        );
    }
}