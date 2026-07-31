package net.minecraft.client.renderer.model.multipart;

import com.google.common.base.MoreObjects;
import com.google.common.base.Splitter;
import malte0811.ferritecore.impl.PropertyValueConditionImpl;
import java.util.function.Predicate;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.StateContainer;

public class PropertyValueCondition implements ICondition
{
    private static final Splitter SPLITTER = Splitter.on('|').omitEmptyStrings();
    private final String key;
    private final String value;

    public PropertyValueCondition(String keyIn, String valueIn)
    {
        this.key = keyIn;
        this.value = valueIn;
    }

    public Predicate<BlockState> getPredicate(StateContainer<Block, BlockState> p_getPredicate_1_)
    {
        return PropertyValueConditionImpl.getPredicate(p_getPredicate_1_, this.key, this.value, SPLITTER);
    }

    public String toString()
    {
        return MoreObjects.toStringHelper(this).add("key", this.key).add("value", this.value).toString();
    }
}
