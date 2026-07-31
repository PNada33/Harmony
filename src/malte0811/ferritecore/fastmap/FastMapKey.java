package malte0811.ferritecore.fastmap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.state.Property;

import java.util.List;
import java.util.Map;

class FastMapKey<T extends Comparable<T>>
{
    private final Property<T> property;
    private final List<T> values;
    private final int mapFactor;
    private final Map<Comparable<?>, Integer> toValueIndex;

    FastMapKey(Property<T> property, int mapFactor)
    {
        this.property = property;
        this.values = ImmutableList.copyOf(property.getAllowedValues());
        this.mapFactor = mapFactor;
        ImmutableMap.Builder<Comparable<?>, Integer> builder = ImmutableMap.builder();

        for (int i = 0; i < this.values.size(); ++i)
        {
            builder.put(this.values.get(i), i);
        }

        this.toValueIndex = builder.build();
    }

    T getValue(int mapIndex)
    {
        int i = mapIndex / this.mapFactor % this.values.size();
        return this.values.get(i);
    }

    int replaceIn(int mapIndex, T newValue)
    {
        int i = mapIndex % this.mapFactor;
        int j = this.mapFactor * this.values.size();
        int k = mapIndex - mapIndex % j;
        int l = this.getInternalIndex(newValue);
        return l < 0 ? -1 : i + this.mapFactor * l + k;
    }

    Property<T> getProperty()
    {
        return this.property;
    }

    int toPartialMapIndex(Comparable<?> value)
    {
        return this.mapFactor * this.getInternalIndex(value);
    }

    private int getInternalIndex(Comparable<?> value)
    {
        Integer integer = this.toValueIndex.get(value);

        if (integer != null)
        {
            return integer;
        }
        else
        {
            throw new IllegalStateException("Unknown value: " + value + " in " + this.property);
        }
    }
}
