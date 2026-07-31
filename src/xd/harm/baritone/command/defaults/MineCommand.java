package xd.harm.baritone.command.defaults;

import xd.harm.baritone.api.IBaritone;
import xd.harm.baritone.api.command.Command;
import xd.harm.baritone.api.command.argument.IArgConsumer;
import xd.harm.baritone.api.command.datatypes.BlockById;
import xd.harm.baritone.api.command.datatypes.ForBlockOptionalMeta;
import xd.harm.baritone.api.command.exception.CommandException;
import xd.harm.baritone.api.utils.BlockOptionalMeta;
import xd.harm.baritone.cache.WorldScanner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class MineCommand extends Command {

    public MineCommand(IBaritone baritone) {
        super(baritone, "mine");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        args.requireMin(1);

        int quantity = 0;
        if (args.hasAny() && args.peekAsOrNull(Integer.class) != null) {
            quantity = args.getAs(Integer.class);
            args.requireMin(1);
        }

        List<BlockOptionalMeta> boms = new ArrayList<>();
        while (args.hasAny()) {
            boms.add(args.getDatatypeFor(ForBlockOptionalMeta.INSTANCE));
        }
        WorldScanner.INSTANCE.repack(ctx);
        logDirect(String.format("Добыча %s", boms.toString()));
        baritone.getMineProcess().mine(quantity, boms.toArray(new BlockOptionalMeta[0]));
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) {
        return args.tabCompleteDatatype(BlockById.INSTANCE);
    }

    @Override
    public String getShortDesc() {
        return "Добыть некоторые блоки";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Команда mine позволяет указать Baritone искать и добывать отдельные блоки.",
                "",
                "Указанные блоки могут быть рудами или любыми другими блоками.",
                "",
                "Также см. настройки legitMine (см. #set l legitMine).",
                "",
                "Использование:",
                "> mine diamond_ore - Добывает все алмазы, которые может найти.",
                "> mine 64 iron_ore - Добывает 64 железной руды."
        );
    }
}