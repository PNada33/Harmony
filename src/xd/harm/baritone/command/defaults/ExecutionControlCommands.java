package xd.harm.baritone.command.defaults;

import xd.harm.baritone.api.IBaritone;
import xd.harm.baritone.api.command.Command;
import xd.harm.baritone.api.command.argument.IArgConsumer;
import xd.harm.baritone.api.command.exception.CommandException;
import xd.harm.baritone.api.command.exception.CommandInvalidStateException;
import xd.harm.baritone.api.process.IBaritoneProcess;
import xd.harm.baritone.api.process.PathingCommand;
import xd.harm.baritone.api.process.PathingCommandType;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class ExecutionControlCommands {

    Command pauseCommand;
    Command resumeCommand;
    Command pausedCommand;
    Command cancelCommand;

    public ExecutionControlCommands(IBaritone baritone) {
        final boolean[] paused = {false};
        baritone.getPathingControlManager().registerProcess(
                new IBaritoneProcess() {
                    @Override
                    public boolean isActive() {
                        return paused[0];
                    }

                    @Override
                    public PathingCommand onTick(boolean calcFailed, boolean isSafeToCancel) {
                        baritone.getInputOverrideHandler().clearAllKeys();
                        return new PathingCommand(null, PathingCommandType.REQUEST_PAUSE);
                    }

                    @Override
                    public boolean isTemporary() {
                        return true;
                    }

                    @Override
                    public void onLostControl() {
                    }

                    @Override
                    public double priority() {
                        return DEFAULT_PRIORITY + 1;
                    }

                    @Override
                    public String displayName0() {
                        return "Команды паузы/возобновления";
                    }
                }
        );
        pauseCommand = new Command(baritone, "pause", "p", "paws") {
            @Override
            public void execute(String label, IArgConsumer args) throws CommandException {
                args.requireMax(0);
                if (paused[0]) {
                    throw new CommandInvalidStateException("Уже на паузе");
                }
                paused[0] = true;
                logDirect("На паузе");
            }

            @Override
            public Stream<String> tabComplete(String label, IArgConsumer args) {
                return Stream.empty();
            }

            @Override
            public String getShortDesc() {
                return "Приостанавливает Baritone до использования resume";
            }

            @Override
            public List<String> getLongDesc() {
                return Arrays.asList(
                        "Команда pause указывает Baritone временно прекратить то, что он делает.",
                        "",
                        "Это можно использовать для приостановки пути, строительства, следования и т.д. Однократное использование команды resume возобновит работу!",
                        "",
                        "Использование:",
                        "> pause"
                );
            }
        };
        resumeCommand = new Command(baritone, "resume", "r", "unpause", "unpaws") {
            @Override
            public void execute(String label, IArgConsumer args) throws CommandException {
                args.requireMax(0);
                baritone.getBuilderProcess().resume();
                if (!paused[0]) {
                    throw new CommandInvalidStateException("Не на паузе");
                }
                paused[0] = false;
                logDirect("Возобновлено");
            }

            @Override
            public Stream<String> tabComplete(String label, IArgConsumer args) {
                return Stream.empty();
            }

            @Override
            public String getShortDesc() {
                return "Возобновляет Baritone после паузы";
            }

            @Override
            public List<String> getLongDesc() {
                return Arrays.asList(
                        "Команда resume указывает Baritone возобновить то, что он делал, когда вы последний раз использовали pause.",
                        "",
                        "Использование:",
                        "> resume"
                );
            }
        };
        pausedCommand = new Command(baritone, "paused") {
            @Override
            public void execute(String label, IArgConsumer args) throws CommandException {
                args.requireMax(0);
                logDirect(String.format("Baritone %sна паузе", paused[0] ? "" : "не "));
            }

            @Override
            public Stream<String> tabComplete(String label, IArgConsumer args) {
                return Stream.empty();
            }

            @Override
            public String getShortDesc() {
                return "Сообщает, находится ли Baritone на паузе";
            }

            @Override
            public List<String> getLongDesc() {
                return Arrays.asList(
                        "Команда paused сообщает, находится ли Baritone в данный момент на паузе из-за использования команды pause.",
                        "",
                        "Использование:",
                        "> paused"
                );
            }
        };
        cancelCommand = new Command(baritone, "cancel", "c", "stop") {
            @Override
            public void execute(String label, IArgConsumer args) throws CommandException {
                args.requireMax(0);
                if (paused[0]) {
                    paused[0] = false;
                }
                baritone.getPathingBehavior().cancelEverything();
                logDirect("ок, отменено");
            }

            @Override
            public Stream<String> tabComplete(String label, IArgConsumer args) {
                return Stream.empty();
            }

            @Override
            public String getShortDesc() {
                return "Отменить то, что Baritone сейчас делает";
            }

            @Override
            public List<String> getLongDesc() {
                return Arrays.asList(
                        "Команда cancel указывает Baritone прекратить то, что он сейчас делает.",
                        "",
                        "Использование:",
                        "> cancel"
                );
            }
        };
    }
}