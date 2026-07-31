package xd.harm.command.api;

import xd.harm.command.interfaces.Parameters;
import xd.harm.command.interfaces.ParametersFactory;

public class ParametersFactoryImpl implements ParametersFactory {

    @Override
    public Parameters createParameters(String message, String delimiter) {
        return new ParametersImpl(message.split(delimiter));
    }
}
