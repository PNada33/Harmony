package xd.harm.command.api;

import xd.harm.command.interfaces.Logger;

public class ConsoleLogger implements Logger {
    @Override
    public void log(String message) {
        System.out.println("message = " + message);
    }
}
