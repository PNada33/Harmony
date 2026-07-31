

package xd.harm.baritone.api.command.datatypes;

import xd.harm.baritone.api.command.exception.CommandException;

/**
 * @author Brady
 * @since 9/26/2019
 */
public interface IDatatypePostFunction<T, O> {

    T apply(O original) throws CommandException;
}
