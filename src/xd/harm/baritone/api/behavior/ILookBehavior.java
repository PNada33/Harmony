

package xd.harm.baritone.api.behavior;

import xd.harm.baritone.api.utils.Rotation;

/**
 * @author Brady
 * @since 9/23/2018
 */
public interface ILookBehavior extends IBehavior {

    /**
     * Updates the current {@link ILookBehavior} target to target
     * the specified rotations on the next tick. If force is {@code true},
     * then freeLook will be overriden and angles will be set regardless.
     * If any sort of block interaction is required, force should be {@code true},
     * otherwise, it should be {@code false};
     *
     * @param rotation The target rotations
     * @param force    Whether or not to "force" the rotations
     */
    void updateTarget(Rotation rotation, boolean force);
}
