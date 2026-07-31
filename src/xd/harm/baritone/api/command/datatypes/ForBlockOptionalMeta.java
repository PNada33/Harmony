package xd.harm.baritone.api.command.datatypes;

import xd.harm.baritone.api.command.exception.CommandException;
import xd.harm.baritone.api.utils.BlockOptionalMeta;

import java.util.stream.Stream;

public enum ForBlockOptionalMeta implements IDatatypeFor<BlockOptionalMeta> {
    INSTANCE;

    @Override
    public BlockOptionalMeta get(IDatatypeContext ctx) throws CommandException {
        String blockName = ctx.getConsumer().getString();
        return new BlockOptionalMeta(blockName);
    }

    @Override
    public Stream<String> tabComplete(IDatatypeContext ctx) {
        return ctx.getConsumer().tabCompleteDatatype(BlockById.INSTANCE);
    }
}