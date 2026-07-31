package net.minecraft.world.chunk;

import ca.spottedleaf.starlight.common.blockstate.ExtendedAbstractBlockState;
import ca.spottedleaf.starlight.common.chunk.ExtendedChunkSection;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.FluidState;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.palette.IPalette;
import net.minecraft.util.palette.IdentityPalette;
import net.minecraft.util.palette.PalettedContainer;
import net.optifine.ChunkDataOF;
import net.optifine.ChunkSectionDataOF;

public class ChunkSection implements ExtendedChunkSection
{
    private static final IPalette<BlockState> REGISTRY_PALETTE = new IdentityPalette<>(Block.BLOCK_STATE_IDS, Blocks.AIR.getDefaultState());
    private final int yBase;
    private short blockRefCount;
    private short blockTickRefCount;
    private short fluidRefCount;
    private final PalettedContainer<BlockState> data;
    private int transparentBlockCount = 16 * 16 * 16;
    private final long[] knownBlockTransparencies = new long[16 * 16 * 16 * 2 / Long.SIZE];
    public static final ThreadLocal<ChunkDataOF> THREAD_CHUNK_DATA_OF = new ThreadLocal<>();

    public ChunkSection(int yBaseIn)
    {
        this(yBaseIn, (short)0, (short)0, (short)0);
    }

    public ChunkSection(int yBaseIn, short blockRefCountIn, short blockTickRefCountIn, short fluidRefCountIn)
    {
        this.yBase = yBaseIn;
        this.blockRefCount = blockRefCountIn;
        this.blockTickRefCount = blockTickRefCountIn;
        this.fluidRefCount = fluidRefCountIn;
        this.data = new PalettedContainer<>(REGISTRY_PALETTE, Block.BLOCK_STATE_IDS, NBTUtil::readBlockState, NBTUtil::writeBlockState, Blocks.AIR.getDefaultState());
    }

    public BlockState getBlockState(int x, int y, int z)
    {
        return this.data.get(x, y, z);
    }

    public FluidState getFluidState(int x, int y, int z)
    {
        return this.data.get(x, y, z).getFluidState();
    }

    public void lock()
    {
        this.data.lock();
    }

    public void unlock()
    {
        this.data.unlock();
    }

    public BlockState setBlockState(int x, int y, int z, BlockState blockStateIn)
    {
        return this.setBlockState(x, y, z, blockStateIn, true);
    }

    public BlockState setBlockState(int x, int y, int z, BlockState state, boolean useLocks)
    {
        BlockState blockstate;

        if (useLocks)
        {
            blockstate = this.data.lockedSwap(x, y, z, state);
        }
        else
        {
            blockstate = this.data.swap(x, y, z, state);
        }

        FluidState fluidstate = blockstate.getFluidState();
        FluidState fluidstate1 = state.getFluidState();

        if (!blockstate.isAir())
        {
            --this.blockRefCount;

            if (blockstate.ticksRandomly())
            {
                --this.blockTickRefCount;
            }
        }

        if (!fluidstate.isEmpty())
        {
            --this.fluidRefCount;
        }

        if (!state.isAir())
        {
            ++this.blockRefCount;

            if (state.ticksRandomly())
            {
                ++this.blockTickRefCount;
            }
        }

        if (!fluidstate1.isEmpty())
        {
            ++this.fluidRefCount;
        }

        long oldTransparency = getKnownTransparency(blockstate);
        long newTransparency = getKnownTransparency(state);

        if (oldTransparency == ExtendedChunkSection.BLOCK_IS_TRANSPARENT)
        {
            --this.transparentBlockCount;
        }

        if (newTransparency == ExtendedChunkSection.BLOCK_IS_TRANSPARENT)
        {
            ++this.transparentBlockCount;
        }

        this.updateTransparencyInfo(y | (x << 4) | (z << 8), newTransparency);
        return blockstate;
    }

    /**
     * Returns whether or not this block storage's Chunk is fully empty, based on its internal reference count.
     */
    public boolean isEmpty()
    {
        return this.blockRefCount == 0;
    }

    public static boolean isEmpty(@Nullable ChunkSection section)
    {
        return section == Chunk.EMPTY_SECTION || section.isEmpty();
    }

    public boolean needsRandomTickAny()
    {
        return this.needsRandomTick() || this.needsRandomTickFluid();
    }

    /**
     * Returns whether or not this block storage's Chunk will require random ticking, used to avoid looping through
     * random block ticks when there are no blocks that would randomly tick.
     */
    public boolean needsRandomTick()
    {
        return this.blockTickRefCount > 0;
    }

    public boolean needsRandomTickFluid()
    {
        return this.fluidRefCount > 0;
    }

    /**
     * Gets the y coordinate that this chunk section starts at (which is a multiple of 16). To get the y number, use
     * <code>section.getYLocation() >> 4</code>. Note that there is a section below the world for lighting purposes.
     */
    public int getYLocation()
    {
        return this.yBase;
    }

    public void recalculateRefCounts()
    {
        ChunkDataOF chunkdataof = THREAD_CHUNK_DATA_OF.get();

        if (chunkdataof != null)
        {
            ChunkSectionDataOF[] achunksectiondataof = chunkdataof.getChunkSectionDatas();

            if (achunksectiondataof != null)
            {
                int i = this.yBase >> 4;

                if (i >= 0 && i < achunksectiondataof.length)
                {
                    ChunkSectionDataOF chunksectiondataof = achunksectiondataof[i];

                    if (chunksectiondataof != null)
                    {
                        this.blockRefCount = chunksectiondataof.getBlockRefCount();
                        this.blockTickRefCount = chunksectiondataof.getTickRefCount();
                        this.fluidRefCount = chunksectiondataof.getFluidRefCount();
                        this.initKnownTransparenciesData();
                        achunksectiondataof[i] = null;
                        return;
                    }
                }
            }
        }

        this.blockRefCount = 0;
        this.blockTickRefCount = 0;
        this.fluidRefCount = 0;
        this.data.count((p_lambda$recalculateRefCounts$0_1_, p_lambda$recalculateRefCounts$0_2_) ->
        {
            FluidState fluidstate = p_lambda$recalculateRefCounts$0_1_.getFluidState();

            if (!p_lambda$recalculateRefCounts$0_1_.isAir())
            {
                this.blockRefCount = (short)(this.blockRefCount + p_lambda$recalculateRefCounts$0_2_);

                if (p_lambda$recalculateRefCounts$0_1_.ticksRandomly())
                {
                    this.blockTickRefCount = (short)(this.blockTickRefCount + p_lambda$recalculateRefCounts$0_2_);
                }
            }

            if (!fluidstate.isEmpty())
            {
                this.blockRefCount = (short)(this.blockRefCount + p_lambda$recalculateRefCounts$0_2_);

                if (fluidstate.ticksRandomly())
                {
                    this.fluidRefCount = (short)(this.fluidRefCount + p_lambda$recalculateRefCounts$0_2_);
                }
            }
        });
        this.initKnownTransparenciesData();
    }

    public PalettedContainer<BlockState> getData()
    {
        return this.data;
    }

    public void read(PacketBuffer packetBufferIn)
    {
        this.blockRefCount = packetBufferIn.readShort();
        this.data.read(packetBufferIn);
        this.initKnownTransparenciesData();
    }

    public void write(PacketBuffer packetBufferIn)
    {
        packetBufferIn.writeShort(this.blockRefCount);
        this.data.write(packetBufferIn);
    }

    public int getSize()
    {
        return 2 + this.data.getSerializedSize();
    }

    public boolean isValidPOIState(Predicate<BlockState> predicate)
    {
        return this.data.func_235963_a_(predicate);
    }

    public short getBlockRefCount()
    {
        return this.blockRefCount;
    }

    public short getTickRefCount()
    {
        return this.blockTickRefCount;
    }

    public short getFluidRefCount()
    {
        return this.fluidRefCount;
    }

    private static long getKnownTransparency(BlockState state)
    {
        int opacityIfCached = ((ExtendedAbstractBlockState)state).getOpacityIfCached();

        if (opacityIfCached == 0)
        {
            return ExtendedChunkSection.BLOCK_IS_TRANSPARENT;
        }
        else if (opacityIfCached == 15)
        {
            return ExtendedChunkSection.BLOCK_IS_FULL_OPAQUE;
        }
        else
        {
            return opacityIfCached == -1 ? ExtendedChunkSection.BLOCK_SPECIAL_TRANSPARENCY : ExtendedChunkSection.BLOCK_UNKNOWN_TRANSPARENCY;
        }
    }

    private void updateTransparencyInfo(int blockIndex, long transparency)
    {
        int arrayIndex = blockIndex >>> 5;
        int valueShift = (blockIndex & 31) << 1;
        long value = this.knownBlockTransparencies[arrayIndex];
        value &= ~(0b11L << valueShift);
        value |= transparency << valueShift;
        this.knownBlockTransparencies[arrayIndex] = value;
    }

    private void initKnownTransparenciesData()
    {
        this.transparentBlockCount = 0;

        for (int y = 0; y <= 15; ++y)
        {
            for (int z = 0; z <= 15; ++z)
            {
                for (int x = 0; x <= 15; ++x)
                {
                    long transparency = getKnownTransparency(this.data.get(x, y, z));

                    if (transparency == ExtendedChunkSection.BLOCK_IS_TRANSPARENT)
                    {
                        ++this.transparentBlockCount;
                    }

                    this.updateTransparencyInfo(y | (x << 4) | (z << 8), transparency);
                }
            }
        }
    }

    @Override
    public boolean hasOpaqueBlocks()
    {
        return this.transparentBlockCount != 4096;
    }

    @Override
    public long getKnownTransparency(int blockIndex)
    {
        int arrayIndex = blockIndex >>> 5;
        int valueShift = (blockIndex & 31) << 1;
        return this.knownBlockTransparencies[arrayIndex] >>> valueShift & 0b11L;
    }

    @Override
    public long getBitsetForColumn(int columnX, int columnZ)
    {
        int columnIndex = (columnX << 4) | (columnZ << 8);
        long value = this.knownBlockTransparencies[columnIndex >>> 5];
        int startIndex = (columnIndex & 31) << 1;
        return value >>> startIndex & ((1L << 32) - 1L);
    }
}
