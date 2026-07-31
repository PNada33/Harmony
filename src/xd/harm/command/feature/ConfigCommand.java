package xd.harm.command.feature;

import xd.harm.command.interfaces.*;
import xd.harm.utils.SoundUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.minecraft.util.text.TextFormatting;

import java.awt.Desktop;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import xd.harm.Harmony;
import xd.harm.command.api.CommandException;
import xd.harm.config.ConfigStorage;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConfigCommand implements Command, MultiNamedCommand {

    final ConfigStorage configStorage;
    final Prefix prefix;
    final Logger logger;

    @Override
    public void execute(Parameters parameters) {
        String action = parameters.asString(0).orElse("").toLowerCase();

        Map<String, Runnable> actions = Map.of(
                "list", this::configList,
                "clear", this::clearConfig,
                "reset", this::resetConfig,
                "folder", this::openConfigFolder
        );

        Map<String, Consumer<Parameters>> paramActions = Map.of(
                "load", this::loadConfig,
                "save", this::saveConfig,
                "remove", this::removeConfig
        );

        if (paramActions.containsKey(action)) {
            if (parameters.asString(1).isEmpty()) {
                throw new CommandException(TextFormatting.RED + "x " + TextFormatting.WHITE + "Укажите имя конфига");
            }
            paramActions.get(action).accept(parameters);
        } else if (actions.containsKey(action)) {
            actions.get(action).run();
        } else {
            throw new CommandException(TextFormatting.RED + "x " + TextFormatting.WHITE + "Действия: " + TextFormatting.GRAY + "load | save | remove | list | clear | reset | folder");
        }
    }

    @Override
    public String name() {
        return "cfg";
    }

    @Override
    public String description() {
        return "Система конфигураций";
    }

    @Override
    public List<String> aliases() {
        return List.of("config", "configuration");
    }

    private void resetConfig() {
        Harmony.getInstance().getModuleManager().getModules().forEach(module -> {
            if (module.isState()) {
                module.setState(false, false);
            }
            module.setBind(0);
        });
        logger.log(TextFormatting.DARK_GRAY + "* " + TextFormatting.WHITE + "Конфигурация сброшена до стандартных");
        SoundUtil.playSound("r.wav");
    }

    private void clearConfig() {
        int deleted = configStorage.clearConfigurations();
        if (deleted <= 0) {
            logger.log(TextFormatting.DARK_GRAY + "* " + TextFormatting.GRAY + "Нет конфигов для удаления");
            return;
        }

        logger.log(TextFormatting.DARK_GRAY + "* " + TextFormatting.WHITE + "Удалено: " + deleted + TextFormatting.GRAY + " конфигов");
        SoundUtil.playSound("r.wav");
    }

    private void loadConfig(Parameters parameters) {
        String configName = parameters.asString(1).get();
        if (configStorage.loadConfiguration(configName)) {
            logger.log(TextFormatting.DARK_GRAY + "* " + TextFormatting.WHITE + "Загружен: " + configName);
            SoundUtil.playSound("s.wav");
        } else {
            logger.log(TextFormatting.DARK_GRAY + "* " + TextFormatting.RED + "Конфиг не найден: " + configName);
        }
    }

    private void saveConfig(Parameters parameters) {
        String configName = parameters.asString(1).get();
        if (configStorage.saveConfiguration(configName)) {
            logger.log(TextFormatting.DARK_GRAY + "* " + TextFormatting.WHITE + "Сохранён: " + configName);
            SoundUtil.playSound("s.wav");
        } else {
            logger.log(TextFormatting.DARK_GRAY + "* " + TextFormatting.RED + "Не удалось сохранить: " + configName);
        }
    }

    private void removeConfig(Parameters parameters) {
        String configName = parameters.asString(1).get();
        if (configStorage.removeConfiguration(configName)) {
            logger.log(TextFormatting.DARK_GRAY + "* " + TextFormatting.WHITE + "Удалён: " + configName);
            SoundUtil.playSound("s.wav");
        } else {
            logger.log(TextFormatting.DARK_GRAY + "* " + TextFormatting.RED + "Не удалось удалить: " + configName);
        }
    }

    private void configList() {
        if (configStorage.isEmpty()) {
            logger.log(TextFormatting.DARK_GRAY + "* " + TextFormatting.GRAY + "Нет конфигов");
            return;
        }

        String configs = configStorage.getConfigs().stream()
                .map(cfg -> TextFormatting.WHITE + cfg.getName())
                .collect(Collectors.joining(TextFormatting.DARK_GRAY + " | "));

        logger.log(TextFormatting.DARK_GRAY + "* " + TextFormatting.GRAY + "Конфиги: " + configs);
    }

    private void openConfigFolder() {
        try {
            File folder = new File("cloud-config-site/local-server-data");
            if (!folder.exists()) {
                folder.mkdirs();
            }
            Desktop.getDesktop().open(folder);
            logger.log(TextFormatting.DARK_GRAY + "* " + TextFormatting.WHITE + "Открытая папка конфигов");
        } catch (Exception e) {
            throw new CommandException(TextFormatting.RED + "Не удалось открыть папку: " + e.getMessage());
        }
    }
}
