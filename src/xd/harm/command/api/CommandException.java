package xd.harm.command.api;

import lombok.Value;

@Value
public class CommandException extends RuntimeException {
    String message;
}
