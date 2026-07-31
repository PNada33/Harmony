

package xd.harm.baritone;

import xd.harm.baritone.api.IBaritone;
import xd.harm.baritone.api.IBaritoneProvider;
import xd.harm.baritone.api.cache.IWorldScanner;
import xd.harm.baritone.api.command.ICommandSystem;
import xd.harm.baritone.api.schematic.ISchematicSystem;
import xd.harm.baritone.cache.WorldScanner;
import xd.harm.baritone.command.CommandSystem;
import xd.harm.baritone.command.ExampleBaritoneControl;
import xd.harm.baritone.utils.schematic.SchematicSystem;

import java.util.Collections;
import java.util.List;

/**
 * @author Brady
 * @since 9/29/2018
 */
public final class BaritoneProvider implements IBaritoneProvider {

    private final Baritone primary;
    private final List<IBaritone> all;

    {
        this.primary = new Baritone();
        this.all = Collections.singletonList(this.primary);

        // Setup chat control, just for the primary instance
        new ExampleBaritoneControl(this.primary);
    }

    @Override
    public IBaritone getPrimaryBaritone() {
        return primary;
    }

    @Override
    public List<IBaritone> getAllBaritones() {
        return all;
    }

    @Override
    public IWorldScanner getWorldScanner() {
        return WorldScanner.INSTANCE;
    }

    @Override
    public ICommandSystem getCommandSystem() {
        return CommandSystem.INSTANCE;
    }

    @Override
    public ISchematicSystem getSchematicSystem() {
        return SchematicSystem.INSTANCE;
    }
}
