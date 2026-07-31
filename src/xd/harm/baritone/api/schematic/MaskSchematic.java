

package xd.harm.baritone.api.schematic;

import net.minecraft.block.BlockState;

import java.util.List;

public abstract class MaskSchematic extends AbstractSchematic {

    private final ISchematic schematic;

    public MaskSchematic(ISchematic schematic) {
        super(schematic.widthX(), schematic.heightY(), schematic.lengthZ());
        this.schematic = schematic;
    }

    protected abstract boolean partOfMask(int x, int y, int z, BlockState currentState);

    @Override
    public boolean inSchematic(int x, int y, int z, BlockState currentState) {
        return schematic.inSchematic(x, y, z, currentState) && partOfMask(x, y, z, currentState);
    }

    @Override
    public BlockState desiredState(int x, int y, int z, BlockState current, List<BlockState> approxPlaceable) {
        return schematic.desiredState(x, y, z, current, approxPlaceable);
    }
}
