package xd.harm.baritone.api.utils;

import xd.harm.baritone.api.utils.accessor.IItemStack;
import com.google.common.collect.ImmutableSet;
import io.netty.util.concurrent.ThreadPerTaskExecutor;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.util.registry.Registry;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.*;
import net.minecraft.resources.*;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Unit;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.registry.Registry;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class BlockOptionalMeta {

    private final Block block;
    private final Set<BlockState> blockstates;
    private final ImmutableSet<Integer> stateHashes;
    private final ImmutableSet<Integer> stackHashes;
    private static final Pattern pattern = Pattern.compile("^(.+?)(?::(\\d+))?$");
    private static LootTableManager manager;
    private static LootPredicateManager predicate = new LootPredicateManager();
    private static Map<Block, List<Item>> drops = new HashMap<>();

    public BlockOptionalMeta(@Nonnull Block block) {
        this.block = block;
        this.blockstates = getStates(block);
        this.stateHashes = getStateHashes(blockstates);
        this.stackHashes = getStackHashes(blockstates);
    }

    public BlockOptionalMeta(@Nonnull String selector) {
        Matcher matcher = pattern.matcher(selector);

        if (!matcher.find()) {
            throw new IllegalArgumentException("invalid block selector: " + selector);
        }

        MatchResult matchResult = matcher.toMatchResult();
        String blockName = matchResult.group(1);

        if (!blockName.contains(":")) {
            blockName = "minecraft:" + blockName;
        }

        ResourceLocation resourceLocation = ResourceLocation.tryCreate(blockName);
        if (resourceLocation == null) {
            throw new IllegalArgumentException("Invalid resource location: " + blockName);
        }

        block = Registry.BLOCK.getOptional(resourceLocation).orElse(null);
        if (block == null) {
            throw new IllegalArgumentException("Unknown block: " + blockName);
        }

        blockstates = getStates(block);
        stateHashes = getStateHashes(blockstates);
        stackHashes = getStackHashes(blockstates);
    }

    private static Set<BlockState> getStates(@Nonnull Block block) {
        return new HashSet<>(block.getStateContainer().getValidStates());
    }

    private static ImmutableSet<Integer> getStateHashes(Set<BlockState> blockstates) {
        return ImmutableSet.copyOf(
                blockstates.stream()
                        .map(BlockState::hashCode)
                        .toArray(Integer[]::new)
        );
    }

    private static ImmutableSet<Integer> getStackHashes(Set<BlockState> blockstates) {
        try {
            return ImmutableSet.copyOf(
                    blockstates.stream()
                            .flatMap(state -> drops(state.getBlock())
                                    .stream()
                                    .map(item -> new ItemStack(item, 1))
                            )
                            .map(stack -> ((IItemStack) (Object) stack).getBaritoneHash())
                            .toArray(Integer[]::new)
            );
        } catch (Exception e) {
            return ImmutableSet.of();
        }
    }

    public Block getBlock() {
        return block;
    }

    public boolean matches(@Nonnull Block block) {
        return block == this.block;
    }

    public boolean matches(@Nonnull BlockState blockstate) {
        Block block = blockstate.getBlock();
        return block == this.block && stateHashes.contains(blockstate.hashCode());
    }

    public boolean matches(ItemStack stack) {
        int hash = ((IItemStack) (Object) stack).getBaritoneHash();
        hash -= stack.getDamage();
        return stackHashes.contains(hash);
    }

    @Override
    public String toString() {
        return String.format("BlockOptionalMeta{block=%s}", block);
    }

    public BlockState getAnyBlockState() {
        if (blockstates.size() > 0) {
            return blockstates.iterator().next();
        }
        return null;
    }

    public static LootTableManager getManager() {
        if (manager == null) {
            ResourcePackList rpl = new ResourcePackList(ResourcePackInfo::new, new ServerPackFinder());
            rpl.reloadPacksFromFinders();
            IResourcePack thePack = rpl.getAllPacks().iterator().next().getResourcePack();
            IReloadableResourceManager resourceManager = new SimpleReloadableResourceManager(ResourcePackType.SERVER_DATA);
            manager = new LootTableManager(predicate);
            resourceManager.addReloadListener(manager);
            try {
                resourceManager.reloadResourcesAndThen(new ThreadPerTaskExecutor(Thread::new), new ThreadPerTaskExecutor(Thread::new), Collections.singletonList(thePack), CompletableFuture.completedFuture(Unit.INSTANCE)).get();
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }
        return manager;
    }

    public static LootPredicateManager getPredicateManager() {
        return predicate;
    }

    private static synchronized List<Item> drops(Block b) {
        return drops.computeIfAbsent(b, block -> {
            ResourceLocation lootTableLocation = block.getLootTable();
            if (lootTableLocation == LootTables.EMPTY) {
                return Collections.emptyList();
            } else {
                List<Item> items = new ArrayList<>();
                getManager().getLootTableFromLocation(lootTableLocation).generate(
                        new LootContext.Builder(null)
                                .withRandom(new Random())
                                .withParameter(LootParameters.field_237457_g_, Vector3d.copy(BlockPos.NULL_VECTOR))
                                .withParameter(LootParameters.TOOL, ItemStack.EMPTY)
                                .withNullableParameter(LootParameters.BLOCK_ENTITY, null)
                                .withParameter(LootParameters.BLOCK_STATE, block.getDefaultState())
                                .build(LootParameterSets.BLOCK),
                        stack -> items.add(stack.getItem())
                );
                return items;
            }
        });
    }
}