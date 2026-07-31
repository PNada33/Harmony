package xd.harm.command.feature;

import xd.harm.command.interfaces.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.minecraft.util.text.TextFormatting;
import xd.harm.Harmony;
import xd.harm.command.api.CommandException;
import xd.harm.modules.api.Module;
import xd.harm.utils.client.KeyStorage;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BindCommand implements Command {

    final Prefix prefix;
    final Logger logger;

    @Override
    public void execute(Parameters parameters) {
        String commandType = parameters.asString(0).orElse("");

        switch (commandType) {
            case "add" -> addBindToFunction(parameters);
            case "remove" -> removeBindFromFunction(parameters);
            case "clear" -> clearAllBindings();
            case "list" -> listBoundKeys();
            default -> throw new CommandException(TextFormatting.RED + "× " + TextFormatting.WHITE + "Действия: " + TextFormatting.GRAY + "add · remove · clear · list");
        }
    }

    @Override
    public String name() {
        return "bind";
    }

    @Override
    public String description() {
        return "Управление привязками клавиш";
    }

    private void addBindToFunction(Parameters parameters) {
        String functionName = parameters.asString(1)
                .orElseThrow(() -> new CommandException(TextFormatting.RED + "× " + TextFormatting.RED + "Не указано имя модуля"));
        String keyName = parameters.asString(2)
                .orElseThrow(() -> new CommandException(TextFormatting.RED + "× " + TextFormatting.RED + "Не указана клавиша"));

        Module module = null;
        for (Module func : Harmony.getInstance().getModuleManager().getModules()) {
            if (func.getName().toLowerCase(Locale.ROOT).equals(functionName.toLowerCase(Locale.ROOT))) {
                module = func;
                break;
            }
        }

        Integer key = KeyStorage.getKey(keyName);

        if (module == null) {
            logger.log(TextFormatting.DARK_GRAY + "" + TextFormatting.RED + "× " + TextFormatting.WHITE + "Модуль " + functionName + TextFormatting.GRAY + " не найден");
            return;
        }

        if (key == null) {
            logger.log(TextFormatting.DARK_GRAY + "" + TextFormatting.RED + "× " + TextFormatting.WHITE + "Клавиша " + keyName + TextFormatting.GRAY + " не существует");
            return;
        }

        module.setBind(key);
        logger.log(TextFormatting.DARK_GRAY + "" + TextFormatting.GREEN + "✓ " + TextFormatting.WHITE + module.getName() + TextFormatting.GRAY + " → " + "[" + KeyStorage.getKey(key) + "]");
    }

    private void removeBindFromFunction(Parameters parameters) {
        String functionName = parameters.asString(1)
                .orElseThrow(() -> new CommandException(TextFormatting.RED + "× " + TextFormatting.WHITE + "Не указано имя модуля"));

        Harmony.getInstance().getModuleManager().getModules().stream()
                .filter(f -> f.getName().equalsIgnoreCase(functionName))
                .forEach(f -> {
                    f.setBind(0);
                    logger.log(TextFormatting.DARK_GRAY + "" + TextFormatting.GREEN + "✓ " + TextFormatting.WHITE + f.getName() + TextFormatting.GRAY + " → [отвязан]");
                });
    }

    private void clearAllBindings() {
        long count = Harmony.getInstance().getModuleManager().getModules().stream()
                .filter(f -> f.getBind() != 0)
                .count();
        Harmony.getInstance().getModuleManager().getModules().forEach(function -> function.setBind(0));
        logger.log(TextFormatting.DARK_GRAY + "" + TextFormatting.GREEN + "✓ " + TextFormatting.WHITE + "Очищено биндов: " + count);
    }

    private void listBoundKeys() {
        List<Module> bound = Harmony.getInstance().getModuleManager().getModules().stream()
                .filter(f -> f.getBind() != 0)
                .toList();

        if (bound.isEmpty()) {
            logger.log(TextFormatting.DARK_GRAY + "○ " + TextFormatting.GRAY + "Нет активных биндов");
            return;
        }

        logger.log(TextFormatting.DARK_GRAY + "┌ " + TextFormatting.WHITE + "Активные бинды " + TextFormatting.DARK_GRAY + "(" + bound.size() + ")");
        bound.forEach(f -> {
            String keyName = KeyStorage.getKey(f.getBind());
            logger.log(TextFormatting.DARK_GRAY + "│ " + TextFormatting.GRAY + f.getName() + TextFormatting.DARK_GRAY + " → " + TextFormatting.WHITE + "[" + keyName + "]");
        });
        logger.log(TextFormatting.DARK_GRAY + "└───────────────");
    }
}
