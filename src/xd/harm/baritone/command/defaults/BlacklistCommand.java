package xd.harm.baritone.command.defaults;

import xd.harm.baritone.api.IBaritone;
import xd.harm.baritone.api.command.Command;
import xd.harm.baritone.api.command.argument.IArgConsumer;
import xd.harm.baritone.api.command.exception.CommandException;
import xd.harm.baritone.api.command.exception.CommandInvalidStateException;
import xd.harm.baritone.api.process.IGetToBlockProcess;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class BlacklistCommand extends Command {

    public BlacklistCommand(IBaritone baritone) {
        super(baritone, "blacklist");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        args.requireMax(0);
        IGetToBlockProcess proc = baritone.getGetToBlockProcess();
        if (!proc.isActive()) {
            throw new CommandInvalidStateException("Процесс GetToBlockProcess в данный момент не активен");
        }
        if (proc.blacklistClosest()) {
            logDirect("Занесены в черный список ближайшие экземпляры");
        } else {
            throw new CommandInvalidStateException("Нет известных местоположений, невозможно занести в черный список");
        }
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) {
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Занести ближайший блок в черный список";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Во время движения к блоку эта команда заносит ближайший блок в черный список, чтобы Baritone не пытался к нему добраться.",
                "",
                "Использование:",
                "> blacklist"
        );
    }
}