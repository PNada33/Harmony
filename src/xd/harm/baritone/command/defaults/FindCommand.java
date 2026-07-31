package xd.harm.baritone.command.defaults;

import xd.harm.baritone.api.IBaritone;
import xd.harm.baritone.api.command.Command;
import xd.harm.baritone.api.command.argument.IArgConsumer;
import xd.harm.baritone.api.command.datatypes.BlockById;
import xd.harm.baritone.api.command.exception.CommandException;
import xd.harm.baritone.api.command.helpers.TabCompleteHelper;
import xd.harm.baritone.api.utils.BetterBlockPos;
import xd.harm.baritone.cache.CachedChunk;
import net.minecraft.block.Block;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static xd.harm.baritone.api.command.IBaritoneChatControl.FORCE_COMMAND_PREFIX;

public class FindCommand extends Command {

    public FindCommand(IBaritone baritone) {
        super(baritone, "find");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        args.requireMin(1);
        List<Block> toFind = new ArrayList<>();
        while (args.hasAny()) {
            toFind.add(args.getDatatypeFor(BlockById.INSTANCE));
        }
        BetterBlockPos origin = ctx.playerFeet();
        ITextComponent[] components = toFind.stream()
                .flatMap(block ->
                        ctx.worldData().getCachedWorld().getLocationsOf(
                                Registry.BLOCK.getKey(block).getPath(),
                                Integer.MAX_VALUE,
                                origin.x,
                                origin.y,
                                4
                        ).stream()
                )
                .map(BetterBlockPos::new)
                .map(this::positionToComponent)
                .toArray(ITextComponent[]::new);
        if (components.length > 0) {
            Arrays.asList(components).forEach(this::logDirect);
        } else {
            logDirect("Позиции неизвестны, вы уверены, что блоки кэшированы?");
        }
    }

    private ITextComponent positionToComponent(BetterBlockPos pos) {
        String positionText = String.format("%s %s %s", pos.x, pos.y, pos.z);
        String command = String.format("%sgoal %s", FORCE_COMMAND_PREFIX, positionText);
        IFormattableTextComponent baseComponent = new StringTextComponent(pos.toString());
        IFormattableTextComponent hoverComponent = new StringTextComponent("Нажмите, чтобы установить цель в эту позицию");
        baseComponent.setStyle(baseComponent.getStyle()
                .setFormatting(TextFormatting.GRAY)
                .setInsertion(positionText)
                .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverComponent)));
        return baseComponent;
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        return new TabCompleteHelper()
                .append(
                        CachedChunk.BLOCKS_TO_KEEP_TRACK_OF.stream()
                                .map(Registry.BLOCK::getKey)
                                .map(Object::toString)
                )
                .filterPrefixNamespaced(args.getString())
                .sortAlphabetically()
                .stream();
    }

    @Override
    public String getShortDesc() {
        return "Найти позиции определенного блока";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Команда find просматривает кэш Baritone и пытается найти местоположение блока.",
                "Автодополнение предложит только кэшированные блоки, некэшированные блоки найти невозможно.",
                "",
                "Использование:",
                "> find <блок> [...] - Попытаться найти перечисленные блоки"
        );
    }
}