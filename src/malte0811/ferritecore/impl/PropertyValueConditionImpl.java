package malte0811.ferritecore.impl;

import com.google.common.base.Splitter;
import malte0811.ferritecore.util.PredicateHelper;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.Property;
import net.minecraft.state.StateContainer;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class PropertyValueConditionImpl
{
    private static final Map<Pair<Property<?>, Comparable<?>>, Predicate<BlockState>> STATE_HAS_PROPERTY_CACHE = new ConcurrentHashMap<>();

    private PropertyValueConditionImpl()
    {
    }

    public static Predicate<BlockState> getPredicate(StateContainer<Block, BlockState> stateContainer, String key, String value, Splitter splitter)
    {
        Property<?> property = stateContainer.getProperty(key);

        if (property == null)
        {
            throw new RuntimeException(String.format("Unknown property '%s' on '%s'", key, stateContainer.getOwner().toString()));
        }
        else
        {
            String s = value;
            boolean flag = !s.isEmpty() && s.charAt(0) == '!';

            if (flag)
            {
                s = s.substring(1);
            }

            List<String> list = splitter.splitToList(s);

            if (list.isEmpty())
            {
                throw new RuntimeException(String.format("Empty value '%s' for property '%s' on '%s'", value, key, stateContainer.getOwner().toString()));
            }
            else
            {
                Predicate<BlockState> predicate;

                if (list.size() == 1)
                {
                    predicate = makePropertyPredicate(stateContainer, property, s, key, value);
                }
                else
                {
                    List<Predicate<BlockState>> list1 = list.stream().map((subValue) ->
                    {
                        return makePropertyPredicate(stateContainer, property, subValue, key, value);
                    }).collect(Collectors.toList());
                    predicate = Deduplicator.or(PredicateHelper.canonize(list1));
                }

                return flag ? predicate.negate() : predicate;
            }
        }
    }

    private static <T extends Comparable<T>> Predicate<BlockState> makePropertyPredicate(StateContainer<Block, BlockState> container, Property<T> property, String subValue, String key, String value)
    {
        Optional<T> optional = property.parseValue(subValue);

        if (!optional.isPresent())
        {
            throw new RuntimeException(String.format("Unknown value '%s' for property '%s' on '%s' in '%s'", subValue, key, container.getOwner().toString(), value));
        }
        else
        {
            T t = optional.get();
            return STATE_HAS_PROPERTY_CACHE.computeIfAbsent(Pair.of(property, t), (pair) ->
            {
                Comparable<?> comparable = pair.getRight();
                Property<?> property1 = pair.getLeft();
                return (state) ->
                {
                    return state.get(property1).equals(comparable);
                };
            });
        }
    }
}
