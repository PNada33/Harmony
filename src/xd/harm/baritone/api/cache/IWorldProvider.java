

package xd.harm.baritone.api.cache;

/**
 * @author Brady
 * @since 9/24/2018
 */
public interface IWorldProvider {

    /**
     * Returns the data of the currently loaded world
     *
     * @return The current world data
     */
    IWorldData getCurrentWorld();
}
