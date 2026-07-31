package xd.harm.command.feature;

import xd.harm.command.interfaces.Command;
import xd.harm.command.interfaces.Logger;
import xd.harm.command.interfaces.MultiNamedCommand;
import xd.harm.command.interfaces.Parameters;
import net.minecraft.util.text.TextFormatting;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MemoryCommand implements Command, MultiNamedCommand {

    private final Logger logger;
    private static ScheduledExecutorService scheduler;

    public MemoryCommand(Logger logger) {
        this.logger = logger;
        startAutoCleanup();
    }

    private void startAutoCleanup() {
        if (scheduler != null) return;

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Memory-Cleanup");
            t.setDaemon(true);
            return t;
        });

        scheduler.scheduleAtFixedRate(
                System::gc,
                1,
                1,
                TimeUnit.HOURS
        );
    }

    @Override
    public void execute(Parameters parameters) {
        Runtime runtime = Runtime.getRuntime();
        long before = runtime.totalMemory() - runtime.freeMemory();

        System.gc();

        long after = runtime.totalMemory() - runtime.freeMemory();
        long freed = (before - after) / 1024 / 1024;
        long total = runtime.totalMemory() / 1024 / 1024;
        long used = after / 1024 / 1024;

        logger.log(TextFormatting.GREEN + "Освобождено: " + freed + " MB");
        logger.log(TextFormatting.GRAY + "Используется: " + used + "/" + total + " MB");
    }

    public static void shutdown() {
        if (scheduler != null) {
            scheduler.shutdown();
            scheduler = null;
        }
    }

    @Override
    public String name() {
        return "memory";
    }

    @Override
    public String description() {
        return "Очистка памяти";
    }

    @Override
    public List<String> aliases() {
        return List.of("gc", "ram");
    }
}