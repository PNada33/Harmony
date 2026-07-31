package xd.harm.baritone.command.argument;

import xd.harm.baritone.api.command.argument.ICommandArgument;
import xd.harm.baritone.api.command.exception.CommandInvalidArgumentException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CommandArguments {

    private CommandArguments() {}

    private static final Pattern ARG_PATTERN = Pattern.compile("\\S+");

    public static List<ICommandArgument> from(String string, boolean preserveEmptyLast) {
        System.out.println("[DEBUG] CommandArguments.from() input: '" + string + "'");
        List<ICommandArgument> args = new ArrayList<>();
        Matcher argMatcher = ARG_PATTERN.matcher(string);
        int lastEnd = -1;
        while (argMatcher.find()) {
            String value = argMatcher.group();
            String rawRest = string.substring(argMatcher.start());
            System.out.println("[DEBUG] Found arg[" + args.size() + "]: value='" + value + "', rawRest='" + rawRest + "'");
            args.add(new CommandArgument(
                    args.size(),
                    value,
                    rawRest
            ));
            lastEnd = argMatcher.end();
        }
        if (preserveEmptyLast && lastEnd < string.length()) {
            args.add(new CommandArgument(args.size(), "", ""));
        }
        System.out.println("[DEBUG] CommandArguments.from() result: " + args.size() + " arguments");
        return args;
    }

    public static List<ICommandArgument> from(String string) {
        return from(string, false);
    }

    public static CommandArgument unknown() {
        return new CommandArgument(-1, "<unknown>", "");
    }
}