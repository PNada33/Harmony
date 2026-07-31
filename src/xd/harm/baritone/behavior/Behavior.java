

package xd.harm.baritone.behavior;

import xd.harm.baritone.Baritone;
import xd.harm.baritone.api.behavior.IBehavior;
import xd.harm.baritone.api.utils.IPlayerContext;

/**
 * A type of game event listener that is given {@link Baritone} instance context.
 *
 * @author Brady
 * @since 8/1/2018
 */
public class Behavior implements IBehavior {

    public final Baritone baritone;
    public final IPlayerContext ctx;

    protected Behavior(Baritone baritone) {
        this.baritone = baritone;
        this.ctx = baritone.getPlayerContext();
        baritone.registerBehavior(this);
    }
}
