package xd.harm.baritone.api.command.datatypes;

import xd.harm.baritone.api.command.exception.CommandException;
import xd.harm.baritone.api.command.helpers.TabCompleteHelper;
import net.minecraft.block.Block;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;

import java.util.stream.Stream;

public enum BlockById implements IDatatypeFor<Block> {
    INSTANCE;

    @Override
    public Block get(IDatatypeContext ctx) throws CommandException {
        String input = ctx.getConsumer().getString();
        if (!input.contains(":")) {
            input = "minecraft:" + input;
        }
        ResourceLocation id = ResourceLocation.tryCreate(input);
        if (id == null) {
            throw new IllegalArgumentException("Invalid block id: " + input);
        }
        Block block = Registry.BLOCK.getOptional(id).orElse(null);
        if (block == null) {
            throw new IllegalArgumentException("No block found by id: " + input);
        }
        return block;
    }

    @Override
    public Stream<String> tabComplete(IDatatypeContext ctx) throws CommandException {
        return new TabCompleteHelper()
                .append(
                        Registry.BLOCK.keySet()
                                .stream()
                                .map(Object::toString)
                )
                .filterPrefixNamespaced(ctx.getConsumer().getString())
                .sortAlphabetically()
                .stream();
    }
}