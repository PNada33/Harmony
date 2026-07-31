

package xd.harm.baritone.utils;

import xd.harm.baritone.Baritone;
import xd.harm.baritone.api.process.IBaritoneProcess;
import xd.harm.baritone.api.utils.Helper;
import xd.harm.baritone.api.utils.IPlayerContext;

public abstract class BaritoneProcessHelper implements IBaritoneProcess, Helper {

    protected final Baritone baritone;
    protected final IPlayerContext ctx;

    public BaritoneProcessHelper(Baritone baritone) {
        this.baritone = baritone;
        this.ctx = baritone.getPlayerContext();
    }

    @Override
    public boolean isTemporary() {
        return false;
    }
}
