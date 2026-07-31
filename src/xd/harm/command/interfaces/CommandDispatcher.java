package xd.harm.command.interfaces;

import xd.harm.command.api.DispatchResult;

public interface CommandDispatcher {
    DispatchResult dispatch(String command);
}
