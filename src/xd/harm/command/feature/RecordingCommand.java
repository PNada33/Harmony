package xd.harm.command.feature;

import net.minecraft.util.text.TextFormatting;
import xd.harm.Harmony;
import xd.harm.command.api.CommandException;
import xd.harm.command.interfaces.Command;
import xd.harm.command.interfaces.Logger;
import xd.harm.command.interfaces.MultiNamedCommand;
import xd.harm.command.interfaces.Parameters;
import xd.harm.command.interfaces.Prefix;
import xd.harm.utils.recording.RecordingManager;

import java.io.File;
import java.util.List;
import java.util.Map;

public class RecordingCommand implements Command, MultiNamedCommand {

    private final Prefix prefix;
    private final Logger logger;

    public RecordingCommand(Prefix prefix, Logger logger) {
        this.prefix = prefix;
        this.logger = logger;
    }

    @Override
    public void execute(Parameters parameters) {
        RecordingManager rec = Harmony.getInstance().getRecordingManager();
        String raw0 = parameters.asString(0).orElse("");
        if (raw0.isEmpty()) {
            showHelp();
            return;
        }

        String action = raw0.toLowerCase();

        switch (action) {
            case "start", "on" -> {
                start(rec);
            }
            case "stop", "off" -> {
                stop(rec);
            }
            case "status" -> {
                if (rec.isRecording()) {
                    logger.log(TextFormatting.DARK_GRAY + "┌ " + TextFormatting.WHITE + "Статус записи");
                    logger.log(TextFormatting.DARK_GRAY + "│ " + TextFormatting.GRAY + "Запись: " + TextFormatting.GREEN + "ВКЛ");
                    File file = rec.getFile();
                    if (file != null) {
                        logger.log(TextFormatting.DARK_GRAY + "│ " + TextFormatting.GRAY + "Файл: " + TextFormatting.WHITE + file.getName());
                    }
                    logger.log(TextFormatting.DARK_GRAY + "│ " + TextFormatting.GRAY + "Кадры: " + TextFormatting.WHITE + rec.getFrames());
                } else {
                    logger.log(TextFormatting.DARK_GRAY + "┌ " + TextFormatting.WHITE + "Статус записи");
                    logger.log(TextFormatting.DARK_GRAY + "│ " + TextFormatting.GRAY + "Запись: " + TextFormatting.RED + "ВЫКЛ");
                }
                logger.log(TextFormatting.DARK_GRAY + "│ " + TextFormatting.GRAY + "Ротация: " + (rec.isModelLoaded() ? TextFormatting.GREEN + "загружена (" + rec.getSampleCount() + " сэмплов)" : TextFormatting.RED + "не загружена"));
                if (rec.isModelLoaded()) {
                    logger.log(TextFormatting.DARK_GRAY + "│ " + TextFormatting.GRAY + "Источник: " + TextFormatting.WHITE + rec.getLoadedFileName());
                }
                logger.log(TextFormatting.DARK_GRAY + "└───────────────");
            }
            case "list" -> {
                List<String> files = rec.listRecordings();
                if (files.isEmpty()) {
                    logger.log(TextFormatting.RED + "○ " + TextFormatting.GRAY + "Записей пока нет");
                    return;
                }
                logger.log(TextFormatting.DARK_GRAY + "┌ " + TextFormatting.WHITE + "Записи " + TextFormatting.DARK_GRAY + "(" + files.size() + ")");
                for (String name : files) {
                    String marker = name.equals(rec.getLoadedFileName()) ? TextFormatting.GREEN + " ✓" : "";
                    Map<String, Object> info = rec.getFileInfo(name);
                    long frameCount = ((Number) info.getOrDefault("frames", 0L)).longValue();
                    long attackCount = ((Number) info.getOrDefault("attacks", 0L)).longValue();
                    long sizeBytes = ((Number) info.getOrDefault("size", 0L)).longValue();
                    long durationMs = ((Number) info.getOrDefault("durationMs", 0L)).longValue();
                    boolean hasTargets = (boolean) info.getOrDefault("hasTargets", false);
                    logger.log(TextFormatting.DARK_GRAY + "│ " + TextFormatting.WHITE + name + marker);
                    logger.log(TextFormatting.DARK_GRAY + "│   " + TextFormatting.GRAY + frameCount + " кадров · " + formatDuration(durationMs) + " · " + formatSize(sizeBytes));
                    logger.log(TextFormatting.DARK_GRAY + "│   " + TextFormatting.GRAY + "Атаки: " + TextFormatting.WHITE + attackCount + TextFormatting.DARK_GRAY + " · " + TextFormatting.GRAY + "Цели: " + (hasTargets ? TextFormatting.GREEN + "да" : TextFormatting.RED + "нет"));
                }
                logger.log(TextFormatting.DARK_GRAY + "└───────────────");
            }
            case "load" -> {
                String fileName = parameters.asString(1).orElse("");
                if (fileName.isEmpty()) {
                    int count = rec.loadAllRecordings();
                    if (count == 0) {
                        logger.log(TextFormatting.RED + "× " + TextFormatting.RED + "Сэмплы не найдены ни в одной записи");
                    } else {
                        logger.log(TextFormatting.DARK_GRAY + "↓ " + TextFormatting.WHITE + "Загружено: " + TextFormatting.GREEN + count + " сэмплов" + TextFormatting.GRAY + " из всех файлов");
                        logger.log(TextFormatting.DARK_GRAY + "  " + TextFormatting.GRAY + "Выберите тип 'Recording' в KillAura");
                    }
                } else {
                    int count = rec.loadFile(fileName);
                    if (count == -1) {
                        logger.log(TextFormatting.RED + "× " + TextFormatting.RED + "Файл " + TextFormatting.WHITE + fileName + TextFormatting.RED + " не найден");
                    } else if (count == 0) {
                        logger.log(TextFormatting.RED + "× " + TextFormatting.RED + "Нет сэмплов в " + TextFormatting.WHITE + fileName);
                    } else {
                        logger.log(TextFormatting.DARK_GRAY + "↓ " + TextFormatting.WHITE + "Загружено: " + TextFormatting.GREEN + count + " сэмплов" + TextFormatting.GRAY + " из " + TextFormatting.WHITE + fileName);
                        logger.log(TextFormatting.DARK_GRAY + "  " + TextFormatting.GRAY + "Выберите тип 'Recording' в KillAura");
                    }
                }
            }
            case "unload" -> {
                rec.unload();
                logger.log(TextFormatting.GREEN + "✓ " + TextFormatting.WHITE + "Ротация выгружена");
            }
            case "delete", "del" -> {
                String fileName = parameters.asString(1).orElse("");
                if (fileName.isEmpty()) {
                    throw new CommandException(TextFormatting.RED + "× " + TextFormatting.RED + "Укажите имя файла");
                }
                if (rec.deleteRecording(fileName)) {
                    logger.log(TextFormatting.DARK_GRAY + "✓ " + TextFormatting.WHITE + "Удалено: " + TextFormatting.RED + fileName);
                } else {
                    logger.log(TextFormatting.RED + "× " + TextFormatting.RED + "Файл " + TextFormatting.WHITE + fileName + TextFormatting.RED + " не найден");
                }
            }
            case "info" -> {
                String fileName = parameters.asString(1).orElse("");
                if (fileName.isEmpty()) {
                    throw new CommandException(TextFormatting.RED + "× " + TextFormatting.RED + "Укажите имя файла");
                }
                Map<String, Object> info = rec.getFileInfo(fileName);
                if (info.isEmpty()) {
                    logger.log(TextFormatting.RED + "× " + TextFormatting.RED + "Файл " + TextFormatting.WHITE + fileName + TextFormatting.RED + " не найден");
                    return;
                }
                long frameCount = ((Number) info.getOrDefault("frames", 0L)).longValue();
                long attackCount = ((Number) info.getOrDefault("attacks", 0L)).longValue();
                long sizeBytes = ((Number) info.getOrDefault("size", 0L)).longValue();
                long durationMs = ((Number) info.getOrDefault("durationMs", 0L)).longValue();
                long framesWithTarget = ((Number) info.getOrDefault("framesWithTarget", 0L)).longValue();
                boolean hasTargets = (boolean) info.getOrDefault("hasTargets", false);
                logger.log(TextFormatting.DARK_GRAY + "┌ " + TextFormatting.WHITE + "Информация: " + TextFormatting.GREEN + fileName);
                logger.log(TextFormatting.DARK_GRAY + "│ " + TextFormatting.GRAY + "Кадры: " + TextFormatting.WHITE + frameCount);
                logger.log(TextFormatting.DARK_GRAY + "│ " + TextFormatting.GRAY + "Длительность: " + TextFormatting.WHITE + formatDuration(durationMs));
                logger.log(TextFormatting.DARK_GRAY + "│ " + TextFormatting.GRAY + "Размер: " + TextFormatting.WHITE + formatSize(sizeBytes));
                logger.log(TextFormatting.DARK_GRAY + "│ " + TextFormatting.GRAY + "Атаки: " + TextFormatting.WHITE + attackCount);
                logger.log(TextFormatting.DARK_GRAY + "│ " + TextFormatting.GRAY + "С целью: " + TextFormatting.WHITE + framesWithTarget + "/" + frameCount);
                logger.log(TextFormatting.DARK_GRAY + "│ " + TextFormatting.GRAY + "Цели: " + (hasTargets ? TextFormatting.GREEN + "да" : TextFormatting.RED + "нет"));
                if (frameCount > 0) {
                    double targetPercent = (double) framesWithTarget / frameCount * 100.0;
                    logger.log(TextFormatting.DARK_GRAY + "│ " + TextFormatting.GRAY + "Покрытие: " + TextFormatting.WHITE + String.format("%.1f%%", targetPercent));
                }
                logger.log(TextFormatting.DARK_GRAY + "└───────────────");
            }
            default -> {
                showHelp();
            }
        }
    }

    private void showHelp() {
        logger.log(TextFormatting.DARK_GRAY + "┌ " + TextFormatting.WHITE + "Recording " + TextFormatting.DARK_GRAY + "· " + TextFormatting.GRAY + "команды");
        logger.log(TextFormatting.DARK_GRAY + "│ " + TextFormatting.WHITE + prefix.get() + "rec " + TextFormatting.GRAY + "start" + TextFormatting.DARK_GRAY + " · " + TextFormatting.GRAY + "stop");
        logger.log(TextFormatting.DARK_GRAY + "│ " + TextFormatting.WHITE + prefix.get() + "rec " + TextFormatting.GRAY + "status");
        logger.log(TextFormatting.DARK_GRAY + "│ " + TextFormatting.WHITE + prefix.get() + "rec " + TextFormatting.GRAY + "list");
        logger.log(TextFormatting.DARK_GRAY + "│ " + TextFormatting.WHITE + prefix.get() + "rec " + TextFormatting.GRAY + "info " + TextFormatting.DARK_GRAY + "<файл>");
        logger.log(TextFormatting.DARK_GRAY + "│ " + TextFormatting.WHITE + prefix.get() + "rec " + TextFormatting.GRAY + "load " + TextFormatting.DARK_GRAY + "<файл>");
        logger.log(TextFormatting.DARK_GRAY + "│ " + TextFormatting.WHITE + prefix.get() + "rec " + TextFormatting.GRAY + "unload");
        logger.log(TextFormatting.DARK_GRAY + "│ " + TextFormatting.WHITE + prefix.get() + "rec " + TextFormatting.GRAY + "delete " + TextFormatting.DARK_GRAY + "<файл>");
        logger.log(TextFormatting.DARK_GRAY + "└───────────────");
    }

    @Override
    public String name() {
        return "recording";
    }

    @Override
    public String description() {
        return "Запись и загрузка паттернов ротации";
    }

    @Override
    public List<String> aliases() {
        return List.of("rec", "record");
    }

    private void start(RecordingManager rec) {
        try {
            File file = rec.start();
            logger.log(TextFormatting.DARK_GRAY + "● " + TextFormatting.GREEN + "Запись начата");
            logger.log(TextFormatting.DARK_GRAY + "  " + TextFormatting.GRAY + "Файл: " + TextFormatting.WHITE + file.getName());
            logger.log(TextFormatting.DARK_GRAY + "  " + TextFormatting.GRAY + "Сразитесь с кем-нибудь, затем " + TextFormatting.WHITE + prefix.get() + "rec stop");
        } catch (IllegalStateException e) {
            throw new CommandException(TextFormatting.RED + "× " + TextFormatting.RED + e.getMessage());
        } catch (Exception e) {
            throw new CommandException(TextFormatting.RED + "× " + TextFormatting.RED + "Ошибка: " + e.getMessage());
        }
    }

    private void stop(RecordingManager rec) {
        if (!rec.isRecording()) {
            logger.log(TextFormatting.DARK_GRAY + "○ " + TextFormatting.GRAY + "Запись не ведётся");
            return;
        }
        File file = rec.getFile();
        long frames = rec.getFrames();
        rec.stop();
        logger.log(TextFormatting.DARK_GRAY + "■ " + TextFormatting.WHITE + "Запись остановлена");
        if (file != null) {
            logger.log(TextFormatting.DARK_GRAY + "  " + TextFormatting.GRAY + "Файл: " + TextFormatting.WHITE + file.getName());
        }
        logger.log(TextFormatting.DARK_GRAY + "  " + TextFormatting.GRAY + "Кадры: " + TextFormatting.WHITE + frames);
        logger.log(TextFormatting.DARK_GRAY + "  " + TextFormatting.GRAY + "Сэмплов: " + TextFormatting.WHITE + rec.getSampleCount());
        if (rec.isModelLoaded()) {
            logger.log(TextFormatting.DARK_GRAY + "  " + TextFormatting.GREEN + "Выберите тип 'Recording' в NiggAura");
        } else {
            logger.log(TextFormatting.DARK_GRAY + "  " + TextFormatting.RED + "Нет сэмплов — вы дрались с кем-нибудь?");
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " Б";
        if (bytes < 1024 * 1024) return String.format("%.1f КБ", bytes / 1024.0);
        return String.format("%.1f МБ", bytes / (1024.0 * 1024.0));
    }

    private String formatDuration(long ms) {
        if (ms <= 0) return "0с";
        long totalSec = ms / 1000;
        long min = totalSec / 60;
        long sec = totalSec % 60;
        if (min == 0) return sec + "с";
        return min + "м " + sec + "с";
    }
}