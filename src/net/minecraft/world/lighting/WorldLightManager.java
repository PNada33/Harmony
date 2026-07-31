package net.minecraft.world.lighting;

import ca.spottedleaf.starlight.common.chunk.ExtendedChunk;
import ca.spottedleaf.starlight.common.light.SWMRNibbleArray;
import ca.spottedleaf.starlight.common.light.StarLightEngine;
import ca.spottedleaf.starlight.common.light.StarLightInterface;
import ca.spottedleaf.starlight.common.light.StarLightLightingProvider;
import ca.spottedleaf.starlight.common.util.CoordinateUtils;
import ca.spottedleaf.starlight.common.util.WorldUtil;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import javax.annotation.Nullable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.chunk.IChunkLightProvider;
import net.minecraft.world.chunk.NibbleArray;

public class WorldLightManager implements ILightListener, StarLightLightingProvider
{
    protected final StarLightInterface lightEngine;
    protected final Long2ObjectOpenHashMap<SWMRNibbleArray[]> blockLightMap = new Long2ObjectOpenHashMap<>();
    protected final Long2ObjectOpenHashMap<SWMRNibbleArray[]> skyLightMap = new Long2ObjectOpenHashMap<>();

    public WorldLightManager(IChunkLightProvider provider, boolean hasBlockLight, boolean hasSkyLight)
    {
        this.lightEngine = new StarLightInterface(provider, hasSkyLight, hasBlockLight);
    }

    @Override
    public StarLightInterface getLightEngine()
    {
        return this.lightEngine;
    }

    public void checkBlock(BlockPos blockPosIn)
    {
        this.lightEngine.blockChange(blockPosIn.toImmutable());
    }

    public void onBlockEmissionIncrease(BlockPos blockPosIn, int p_215573_2_)
    {
    }

    public boolean hasLightWork()
    {
        return this.lightEngine.hasUpdates();
    }

    public int tick(int toUpdateCount, boolean updateSkyLight, boolean updateBlockLight)
    {
        boolean hadUpdates = this.hasLightWork();
        this.lightEngine.propagateChanges();
        return hadUpdates ? 1 : 0;
    }

    public void updateSectionStatus(SectionPos pos, boolean isEmpty)
    {
        this.lightEngine.sectionChange(pos, isEmpty);
    }

    public void enableLightSources(ChunkPos p_215571_1_, boolean p_215571_2_)
    {
    }

    public IWorldLightListener getLightEngine(LightType type)
    {
        return type == LightType.BLOCK ? this.lightEngine.getBlockReader() : this.lightEngine.getSkyReader();
    }

    public String getDebugInfo(LightType p_215572_1_, SectionPos p_215572_2_)
    {
        return "starlight";
    }

    public void setData(LightType type, SectionPos pos, @Nullable NibbleArray array, boolean p_215574_4_)
    {
    }

    public void retainData(ChunkPos pos, boolean retain)
    {
    }

    public int getLightSubtracted(BlockPos blockPosIn, int amount)
    {
        int i = this.lightEngine.getSkyReader().getLightFor(blockPosIn) - amount;
        int j = this.lightEngine.getBlockReader().getLightFor(blockPosIn);
        return Math.max(j, i);
    }

    @Override
    public void clientUpdateLight(LightType lightType, SectionPos pos, @Nullable NibbleArray nibble, boolean trustEdges)
    {
        if (!this.lightEngine.isClientSide())
        {
            return;
        }

        IChunk chunk = this.lightEngine.getAnyChunkNow(pos.getX(), pos.getZ());

        if (lightType == LightType.BLOCK)
        {
            SWMRNibbleArray[] blockNibbles = this.blockLightMap.computeIfAbsent(CoordinateUtils.getChunkKey(pos), (keyInMap) ->
            {
                return StarLightEngine.getFilledEmptyLight(this.lightEngine.getWorld());
            });
            blockNibbles[pos.getY() - WorldUtil.getMinLightSection(this.lightEngine.getWorld())] = SWMRNibbleArray.fromVanilla(nibble);

            if (chunk != null)
            {
                ((ExtendedChunk)chunk).setBlockNibbles(blockNibbles);
                this.lightEngine.getLightAccess().markLightChanged(LightType.BLOCK, pos);
            }
        }
        else
        {
            SWMRNibbleArray[] skyNibbles = this.skyLightMap.computeIfAbsent(CoordinateUtils.getChunkKey(pos), (keyInMap) ->
            {
                return StarLightEngine.getFilledEmptyLight(this.lightEngine.getWorld());
            });
            skyNibbles[pos.getY() - WorldUtil.getMinLightSection(this.lightEngine.getWorld())] = SWMRNibbleArray.fromVanilla(nibble);

            if (chunk != null)
            {
                ((ExtendedChunk)chunk).setSkyNibbles(skyNibbles);
                this.lightEngine.getLightAccess().markLightChanged(LightType.SKY, pos);
            }
        }
    }

    @Override
    public void clientRemoveLightData(ChunkPos chunkPos)
    {
        if (!this.lightEngine.isClientSide())
        {
            return;
        }

        long key = CoordinateUtils.getChunkKey(chunkPos);
        this.blockLightMap.remove(key);
        this.skyLightMap.remove(key);
    }

    @Override
    public void clientChunkLoad(ChunkPos pos, Chunk chunk)
    {
        if (!this.lightEngine.isClientSide())
        {
            return;
        }

        long key = CoordinateUtils.getChunkKey(pos);
        SWMRNibbleArray[] blockNibbles = this.blockLightMap.get(key);
        SWMRNibbleArray[] skyNibbles = this.skyLightMap.get(key);

        if (blockNibbles != null)
        {
            ((ExtendedChunk)chunk).setBlockNibbles(blockNibbles);
        }

        if (skyNibbles != null)
        {
            ((ExtendedChunk)chunk).setSkyNibbles(skyNibbles);
        }
    }
}
