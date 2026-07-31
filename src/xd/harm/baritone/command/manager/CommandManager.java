package xd.harm.baritone.command.manager;

import xd.harm.baritone.Baritone;
import xd.harm.baritone.api.IBaritone;
import xd.harm.baritone.api.command.ICommand;
import xd.harm.baritone.api.command.argument.ICommandArgument;
import xd.harm.baritone.api.command.exception.CommandUnhandledException;
import xd.harm.baritone.api.command.exception.ICommandException;
import xd.harm.baritone.api.command.helpers.TabCompleteHelper;
import xd.harm.baritone.api.command.manager.ICommandManager;
import xd.harm.baritone.api.command.registry.Registry;
import xd.harm.baritone.command.argument.ArgConsumer;
import xd.harm.baritone.command.argument.CommandArguments;
import xd.harm.baritone.command.defaults.DefaultCommands;
import net.minecraft.util.Tuple;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public class CommandManager implements ICommandManager {

    private final Registry<ICommand> registry = new Registry<>();
    private final Baritone baritone;

    public CommandManager(Baritone baritone) {
        this.baritone = baritone;
        DefaultCommands.createAll(baritone).forEach(this.registry::register);
    }

    @Override
    public IBaritone getBaritone() {
        return this.baritone;
    }

    @Override
    public Registry<ICommand> getRegistry() {
        return this.registry;
    }

    @Override
    public ICommand getCommand(String name) {
        for (ICommand command : this.registry.entries) {
            if (command.getNames().contains(name.toLowerCase(Locale.US))) {
                return command;
            }
        }
        return null;
    }

    @Override
    public boolean execute(String string) {
        System.out.println("[DEBUG] CommandManager.execute() input: '" + string + "'");
        return this.execute(expand(string));
    }

    @Override
    public boolean execute(Tuple<String, List<ICommandArgument>> expanded) {
        ExecutionWrapper execution = this.from(expanded);
        if (execution != null) {
            execution.execute();
        }
        return execution != null;
    }

    @Override
    public Stream<String> tabComplete(Tuple<String, List<ICommandArgument>> expanded) {
        ExecutionWrapper execution = this.from(expanded);
        return execution == null ? Stream.empty() : execution.tabComplete();
    }

    @Override
    public Stream<String> tabComplete(String prefix) {
        Tuple<String, List<ICommandArgument>> pair = expand(prefix, true);
        String label = pair.getA();
        List<ICommandArgument> args = pair.getB();
        if (args.isEmpty()) {
            return new TabCompleteHelper()
                    .addCommands(this.baritone.getCommandManager())
                    .filterPrefix(label)
                    .stream();
        } else {
            return tabComplete(pair);
        }
    }

    private ExecutionWrapper from(Tuple<String, List<ICommandArgument>> expanded) {
        String label = expanded.getA();
        ArgConsumer args = new ArgConsumer(this, expanded.getB());

        ICommand command = this.getCommand(label);
        System.out.println("[DEBUG] CommandManager.from() label='" + label + "', command=" + (command != null ? command.getClass().getSimpleName() : "null"));
        return command == null ? null : new ExecutionWrapper(command, label, args);
    }

    private static Tuple<String, List<ICommandArgument>> expand(String string, boolean preserveEmptyLast) {
        System.out.println("[DEBUG] CommandManager.expand() input: '" + string + "'");
        String label = string.split("\\s", 2)[0];
        String argsString = string.substring(label.length());
        System.out.println("[DEBUG] CommandManager.expand() label='" + label + "', argsString='" + argsString + "'");
        List<ICommandArgument> args = CommandArguments.from(argsString, preserveEmptyLast);
        return new Tuple<>(label, args);
    }

    public static Tuple<String, List<ICommandArgument>> expand(String string) {
        return expand(string, false);
    }

    private static final class ExecutionWrapper {

        private ICommand command;
        private String label;
        private ArgConsumer args;

        private ExecutionWrapper(ICommand command, String label, ArgConsumer args) {
            this.command = command;
            this.label = label;
            this.args = args;
        }

        private void execute() {
            try {
                System.out.println("[DEBUG] ExecutionWrapper.execute() command=" + command.getClass().getSimpleName());
                System.out.println("[DEBUG] ExecutionWrapper.execute() args count=" + args.getArgs().size());
                for (int i = 0; i < args.getArgs().size(); i++) {
                    System.out.println("[DEBUG] ExecutionWrapper.execute() arg[" + i + "]='" + args.getArgs().get(i).getValue() + "'");
                }
                this.command.execute(this.label, this.args);
                System.out.println("[DEBUG] ExecutionWrapper.execute() completed successfully");
            } catch (Throwable t) {
                System.out.println("[DEBUG] ExecutionWrapper.execute() exception: " + t.getClass().getName() + ": " + t.getMessage());
                t.printStackTrace();
                ICommandException exception = t instanceof ICommandException
                        ? (ICommandException) t
                        : new CommandUnhandledException(t);

                exception.handle(command, args.getArgs());
            }
        }

        private Stream<String> tabComplete() {
            try {
                return this.command.tabComplete(this.label, this.args);
            } catch (Throwable t) {
                return Stream.empty();
            }
        }
    }
}