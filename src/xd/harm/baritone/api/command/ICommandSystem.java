

package xd.harm.baritone.api.command;

import xd.harm.baritone.api.command.argparser.IArgParserManager;

/**
 * @author Brady
 * @since 10/4/2019
 */
public interface ICommandSystem {

    IArgParserManager getParserManager();
}
