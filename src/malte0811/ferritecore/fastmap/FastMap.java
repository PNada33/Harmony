package malte0811.ferritecore.fastmap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.state.Property;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class FastMap<Value>
{
    private final List<FastMapKey<?>> keys;
    private final List<Property<?>> rawKeys;
    private final List<Value> values;
    private final Map<Property<?>, Integer> toKeyIndex;

    public FastMap(Collection<Property<?>> properties, Map<Map<Property<?>, Comparable<?>>, Value> valuesMap)
    {
        this.rawKeys = ImmutableList.copyOf(properties);
        List<FastMapKey<?>> list = new ArrayList<>(this.rawKeys.size());
        int i = 1;
        ImmutableMap.Builder<Property<?>, Integer> builder = ImmutableMap.builder();

        for (Property<?> property : this.rawKeys)
        {
            builder.put(property, list.size());
            list.add(new FastMapKey<>(property, i));
            i *= property.getAllowedValues().size();
        }

        this.keys = ImmutableList.copyOf(list);
        this.toKeyIndex = builder.build();
        List<Value> list1 = new ArrayList<>(i);

        for (int j = 0; j < i; ++j)
        {
            list1.add(null);
        }

        for (Map.Entry<Map<Property<?>, Comparable<?>>, Value> entry : valuesMap.entrySet())
        {
            list1.set(this.getIndexOf(entry.getKey()), entry.getValue());
        }

        this.values = ImmutableList.copyOf(list1);
    }

    @Nullable
    public <T extends Comparable<T>> Value with(int last, Property<T> prop, T value)
    {
        FastMapKey<T> fastmapkey = this.getKeyFor(prop);

        if (fastmapkey == null)
        {
            return null;
        }
        else
        {
            int i = fastmapkey.replaceIn(last, value);
            return i < 0 ? null : this.values.get(i);
        }
    }

    public int getIndexOf(Map<Property<?>, Comparable<?>> state)
    {
        int i = 0;

        for (FastMapKey<?> fastmapkey : this.keys)
        {
            i += fastmapkey.toPartialMapIndex(state.get(fastmapkey.getProperty()));
        }

        return i;
    }

    @Nullable
    public <T extends Comparable<T>> T getValue(int stateIndex, Property<T> property)
    {
        FastMapKey<T> fastmapkey = this.getKeyFor(property);
        return fastmapkey == null ? null : fastmapkey.getValue(stateIndex);
    }

    public List<Property<?>> getProperties()
    {
        return this.rawKeys;
    }

    public ImmutableMap<Property<?>, Comparable<?>> makeValuesFor(int index)
    {
        ImmutableMap.Builder<Property<?>, Comparable<?>> builder = ImmutableMap.builder();

        for (Property<?> property : this.getProperties())
        {
            builder.put(property, this.getValue(index, property));
        }

        return builder.build();
    }

    public <T extends Comparable<T>> Value withUnsafe(int globalTableIndex, Property<T> rowKey, Object columnKey)
    {
        return this.with(globalTableIndex, rowKey, (T)columnKey);
    }

    public int numProperties()
    {
        return this.keys.size();
    }

    FastMapKey<?> getKey(int keyIndex)
    {
        return this.keys.get(keyIndex);
    }

    @Nullable
    private <T extends Comparable<T>> FastMapKey<T> getKeyFor(Property<T> prop)
    {
        Integer integer = this.toKeyIndex.get(prop);
        return integer == null ? null : (FastMapKey<T>)this.getKey(integer);
    }
}
