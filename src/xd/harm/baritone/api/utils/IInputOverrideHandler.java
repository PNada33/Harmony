

package xd.harm.baritone.api.utils;

import xd.harm.baritone.api.behavior.IBehavior;
import xd.harm.baritone.api.utils.input.Input;

/**
 * @author Brady
 * @since 11/12/2018
 */
public interface IInputOverrideHandler extends IBehavior {

    boolean isInputForcedDown(Input input);

    void setInputForceState(Input input, boolean forced);

    void clearAllKeys();
}
