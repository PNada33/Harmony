

package xd.harm.baritone.command;

import xd.harm.baritone.api.command.ICommandSystem;
import xd.harm.baritone.api.command.argparser.IArgParserManager;
import xd.harm.baritone.command.argparser.ArgParserManager;

/**
 * @author Brady
 * @since 10/4/2019
 */
public enum CommandSystem implements ICommandSystem {
    INSTANCE;

    @Override
    public IArgParserManager getParserManager() {
        return ArgParserManager.INSTANCE;
    }
}
