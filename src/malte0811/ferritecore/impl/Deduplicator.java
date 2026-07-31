package malte0811.ferritecore.impl;

import com.google.common.collect.ImmutableList;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.MultipartBakedModel;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

public final class Deduplicator
{
    private static final Map<String, String> VARIANT_IDENTITIES = new ConcurrentHashMap<>();
    private static final Map<List<Pair<Predicate<BlockState>, IBakedModel>>, MultipartBakedModel> KNOWN_MULTIPART_MODELS = new ConcurrentHashMap<>();
    private static final Map<List<Predicate<BlockState>>, Predicate<BlockState>> OR_PREDICATE_CACHE = new ConcurrentHashMap<>();
    private static final Map<List<Predicate<BlockState>>, Predicate<BlockState>> AND_PREDICATE_CACHE = new ConcurrentHashMap<>();

    private Deduplicator()
    {
    }

    public static void clearCaches()
    {
        VARIANT_IDENTITIES.clear();
        KNOWN_MULTIPART_MODELS.clear();
        OR_PREDICATE_CACHE.clear();
        AND_PREDICATE_CACHE.clear();
    }

    public static String deduplicateVariant(String variant)
    {
        return VARIANT_IDENTITIES.computeIfAbsent(variant, Function.identity());
    }

    public static MultipartBakedModel makeMultipartModel(List<Pair<Predicate<BlockState>, IBakedModel>> selectors)
    {
        List<Pair<Predicate<BlockState>, IBakedModel>> list = ImmutableList.copyOf(selectors);
        return KNOWN_MULTIPART_MODELS.computeIfAbsent(list, MultipartBakedModel::new);
    }

    public static Predicate<BlockState> or(List<Predicate<BlockState>> list)
    {
        List<Predicate<BlockState>> list1 = ImmutableList.copyOf(list);
        return OR_PREDICATE_CACHE.computeIfAbsent(list1, (predicates) ->
        {
            return (state) ->
            {
                return predicates.stream().anyMatch((predicate) ->
                {
                    return predicate.test(state);
                });
            };
        });
    }

    public static Predicate<BlockState> and(List<Predicate<BlockState>> list)
    {
        List<Predicate<BlockState>> list1 = ImmutableList.copyOf(list);
        return AND_PREDICATE_CACHE.computeIfAbsent(list1, (predicates) ->
        {
            return (state) ->
            {
                return predicates.stream().allMatch((predicate) ->
                {
                    return predicate.test(state);
                });
            };
        });
    }
}
