package net.minecraft.client.renderer.model.multipart;

import malte0811.ferritecore.impl.Deduplicator;
import malte0811.ferritecore.util.PredicateHelper;
import java.util.function.Predicate;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateContainer;

public class AndCondition implements ICondition
{
    private final Iterable <? extends ICondition > conditions;

    public AndCondition(Iterable <? extends ICondition > conditionsIn)
    {
        this.conditions = conditionsIn;
    }

    public Predicate<BlockState> getPredicate(StateContainer<Block, BlockState> p_getPredicate_1_)
    {
        return Deduplicator.and(PredicateHelper.toCanonicalList(this.conditions, p_getPredicate_1_));
    }
}
