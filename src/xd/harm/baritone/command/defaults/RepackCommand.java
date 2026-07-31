package xd.harm.baritone.command.defaults;

import xd.harm.baritone.api.IBaritone;
import xd.harm.baritone.api.command.Command;
import xd.harm.baritone.api.command.argument.IArgConsumer;
import xd.harm.baritone.api.command.exception.CommandException;
import xd.harm.baritone.cache.WorldScanner;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class RepackCommand extends Command {

    public RepackCommand(IBaritone baritone) {
        super(baritone, "repack", "rescan");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        args.requireMax(0);
        logDirect(String.format("Поставлено в очередь %d чанков для перепаковки", WorldScanner.INSTANCE.repack(ctx)));
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) {
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Перекэшировать чанки";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Перепаковка чанков вокруг вас. Это, по сути, их перекэширование.",
                "",
                "Использование:",
                "> repack - Перепаковать чанки."
        );
    }
}