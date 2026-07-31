package net.minecraft.state;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Table;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import malte0811.ferritecore.fastmap.FastMap;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public abstract class StateHolder<O, S>
{
    private static final Function < Entry < Property<?>, Comparable<? >> , String > field_235890_a_ = new Function < Entry < Property<?>, Comparable<? >> , String > ()
    {
        public String apply(@Nullable Entry < Property<?>, Comparable<? >> p_apply_1_)
        {
            if (p_apply_1_ == null)
            {
                return "<NULL>";
            }
            else
            {
                Property<?> property = p_apply_1_.getKey();
                return property.getName() + "=" + this.func_235905_a_(property, p_apply_1_.getValue());
            }
        }
        private <T extends Comparable<T>> String func_235905_a_(Property<T> p_235905_1_, Comparable<?> p_235905_2_)
        {
            return p_235905_1_.getName((T)p_235905_2_);
        }
    };
    private static final ThreadLocal<Map<Map<Property<?>, Comparable<?>>, ?>> FERRITECORE_LAST_STATE_MAP = new ThreadLocal<>();
    private static final ThreadLocal<FastMap<?>> FERRITECORE_LAST_FAST_STATE_MAP = new ThreadLocal<>();
    protected final O instance;
    @Nullable
    private ImmutableMap < Property<?>, Comparable<? >> properties;
    private Table < Property<?>, Comparable<?>, S > field_235894_e_;
    @Nullable
    private FastMap<S> ferriteCoreGlobalTable;
    private int ferriteCoreGlobalTableIndex;
    protected final MapCodec<S> field_235893_d_;

    protected StateHolder(O p_i231879_1_, ImmutableMap < Property<?>, Comparable<? >> p_i231879_2_, MapCodec<S> p_i231879_3_)
    {
        this.instance = p_i231879_1_;
        this.properties = p_i231879_2_;
        this.field_235893_d_ = p_i231879_3_;
    }

    public <T extends Comparable<T>> S func_235896_a_(Property<T> p_235896_1_)
    {
        return this.with(p_235896_1_, func_235898_a_(p_235896_1_.getAllowedValues(), this.get(p_235896_1_)));
    }

    protected static <T> T func_235898_a_(Collection<T> p_235898_0_, T p_235898_1_)
    {
        Iterator<T> iterator = p_235898_0_.iterator();

        while (iterator.hasNext())
        {
            if (iterator.next().equals(p_235898_1_))
            {
                if (iterator.hasNext())
                {
                    return iterator.next();
                }

                return p_235898_0_.iterator().next();
            }
        }

        return iterator.next();
    }

    public String toString()
    {
        StringBuilder stringbuilder = new StringBuilder();
        stringbuilder.append(this.instance);

        if (!this.getValues().isEmpty())
        {
            stringbuilder.append('[');
            stringbuilder.append(this.getValues().entrySet().stream().map(field_235890_a_).collect(Collectors.joining(",")));
            stringbuilder.append(']');
        }

        return stringbuilder.toString();
    }

    public Collection < Property> getProperties()
    {
        return Collections.unmodifiableCollection((Collection)this.ferriteCoreGetProperties());
    }

    public <T extends Comparable<T>> boolean hasProperty(Property<T> property)
    {
        return this.ferriteCoreGetProperties().contains(property);
    }

    public <T extends Comparable<T>> T get(Property<T> property)
    {
        Comparable<?> comparable = this.ferriteCoreGetValue(property);

        if (comparable == null)
        {
            throw new IllegalArgumentException("Cannot get property " + property + " as it does not exist in " + this.instance);
        }
        else
        {
            return property.getValueClass().cast(comparable);
        }
    }

    public <T extends Comparable<T>> Optional<T> func_235903_d_(Property<T> p_235903_1_)
    {
        Comparable<?> comparable = this.ferriteCoreGetValue(p_235903_1_);
        return comparable == null ? Optional.empty() : Optional.of(p_235903_1_.getValueClass().cast(comparable));
    }

    public <T extends Comparable<T>, V extends T> S with(Property<T> property, V value)
    {
        Comparable<?> comparable = this.ferriteCoreGetValue(property);

        if (comparable == null)
        {
            throw new IllegalArgumentException("Cannot set property " + property + " as it does not exist in " + this.instance);
        }
        else if (comparable == value)
        {
            return (S)this;
        }
        else
        {
            S s = this.ferriteCoreGlobalTable != null ? this.ferriteCoreGlobalTable.with(this.ferriteCoreGlobalTableIndex, property, value) : this.field_235894_e_.get(property, value);

            if (s == null)
            {
                throw new IllegalArgumentException("Cannot set property " + property + " to " + value + " on " + this.instance + ", it is not an allowed value");
            }
            else
            {
                return s;
            }
        }
    }

    public void func_235899_a_(Map < Map < Property<?>, Comparable<? >> , S > p_235899_1_)
    {
        if (this.ferriteCoreGlobalTable != null || this.field_235894_e_ != null)
        {
            throw new IllegalStateException();
        }
        else
        {
            FastMap<S> fastmap;

            if (p_235899_1_ == FERRITECORE_LAST_STATE_MAP.get())
            {
                fastmap = (FastMap<S>)FERRITECORE_LAST_FAST_STATE_MAP.get();
            }
            else
            {
                FERRITECORE_LAST_STATE_MAP.set(p_235899_1_);
                fastmap = new FastMap<>(this.properties.keySet(), p_235899_1_);
                FERRITECORE_LAST_FAST_STATE_MAP.set(fastmap);
            }

            this.ferriteCoreGlobalTable = fastmap;
            this.ferriteCoreGlobalTableIndex = fastmap.getIndexOf(this.properties);
            this.properties = null;
        }
    }

    public ImmutableMap < Property<?>, Comparable<? >> getValues()
    {
        if (this.properties != null)
        {
            return this.properties;
        }
        else
        {
            return this.ferriteCoreGlobalTable != null ? this.ferriteCoreGlobalTable.makeValuesFor(this.ferriteCoreGlobalTableIndex) : ImmutableMap.of();
        }
    }

    protected static <O, S extends StateHolder<O, S>> Codec<S> func_235897_a_(Codec<O> p_235897_0_, Function<O, S> p_235897_1_)
    {
        return p_235897_0_.dispatch("Name", (p_235895_0_) ->
        {
            return p_235895_0_.instance;
        }, (p_235900_1_) ->
        {
            S s = p_235897_1_.apply(p_235900_1_);
            return s.getValues().isEmpty() ? Codec.unit(s) : s.field_235893_d_.fieldOf("Properties").codec();
        });
    }

    private Collection<Property<?>> ferriteCoreGetProperties()
    {
        if (this.ferriteCoreGlobalTable != null)
        {
            return this.ferriteCoreGlobalTable.getProperties();
        }
        else
        {
            return this.properties != null ? this.properties.keySet() : Collections.emptyList();
        }
    }

    @Nullable
    private Comparable<?> ferriteCoreGetValue(Property<?> property)
    {
        if (this.ferriteCoreGlobalTable != null)
        {
            return this.ferriteCoreGlobalTable.getValue(this.ferriteCoreGlobalTableIndex, property);
        }
        else
        {
            return this.properties != null ? this.properties.get(property) : null;
        }
    }
}
