package xd.harm.command.feature;

import xd.harm.command.interfaces.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.minecraft.util.text.TextFormatting;
import java.util.List;
import xd.harm.config.MacroManager;
import xd.harm.command.api.CommandException;
import xd.harm.utils.client.KeyStorage;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MacroCommand implements Command, MultiNamedCommand {

    final MacroManager macroManager;
    final Prefix prefix;
    final Logger logger;

    @Override
    public void execute(Parameters parameters) {
        String commandType = parameters.asString(0).orElseThrow(() ->
                new CommandException(TextFormatting.RED + "× " + TextFormatting.WHITE + "Действия: " + TextFormatting.GRAY + "add · remove · clear · list"));
        switch (commandType) {
            case "add" -> addMacro(parameters);
            case "remove" -> removeMacro(parameters);
            case "clear" -> clearMacros();
            case "list" -> printMacrosList();
            default -> throw new CommandException(TextFormatting.RED + "× " + TextFormatting.WHITE + "Действия: " + TextFormatting.GRAY + "add · remove · clear · list");
        }
    }

    @Override
    public String name() {
        return "macro";
    }

    @Override
    public String description() {
        return "Система макросов";
    }

    @Override
    public List<String> aliases() {
        return List.of("macros", "m");
    }

    private void addMacro(Parameters parameters) {
        String macroName = parameters.asString(1)
                .orElseThrow(() -> new CommandException(TextFormatting.RED + "× " + TextFormatting.RED + "Укажите имя макроса"));
        String macroKey = parameters.asString(2)
                .orElseThrow(() -> new CommandException(TextFormatting.RED + "× " + TextFormatting.RED + "Укажите клавишу"));
        String macroMessage = parameters.collectMessage(3);
        if (macroMessage.isEmpty()) {
            throw new CommandException(TextFormatting.RED + "× " + TextFormatting.RED + "Укажите команду/сообщение");
        }
        Integer key = KeyStorage.getKey(macroKey);
        if (key == null) {
            logger.log(TextFormatting.DARK_GRAY + "" + TextFormatting.RED + "× " + TextFormatting.WHITE + "Клавиша " + macroKey + TextFormatting.GRAY + " не найдена");
            return;
        }
        if (macroManager.hasMacro(macroName)) {
            throw new CommandException(TextFormatting.RED + "× " + TextFormatting.WHITE + "Макрос " + macroName + TextFormatting.GRAY + " уже существует");
        }
        macroManager.addMacro(macroName, macroMessage, key);
        logger.log(TextFormatting.DARK_GRAY + "" + TextFormatting.GREEN + "✓ " + TextFormatting.WHITE + macroName + TextFormatting.GRAY + " → [" + KeyStorage.getKey(key) + "] → " + macroMessage);
    }

    private void removeMacro(Parameters parameters) {
        String macroName = parameters.asString(1)
                .orElseThrow(() -> new CommandException(TextFormatting.RED + "× " + TextFormatting.WHITE + "Укажите имя макроса"));
        if (!macroManager.hasMacro(macroName)) {
            logger.log(TextFormatting.DARK_GRAY + "" + TextFormatting.RED + "× " + TextFormatting.WHITE + "Макрос " + macroName + TextFormatting.GRAY + " не найден");
            return;
        }
        macroManager.deleteMacro(macroName);
        logger.log(TextFormatting.DARK_GRAY + "" + TextFormatting.GREEN + "✓ " + TextFormatting.WHITE + "Макрос " + macroName + TextFormatting.GRAY + " удалён");
    }

    private void clearMacros() {
        int count = macroManager.macroList.size();
        macroManager.clearList();
        logger.log(TextFormatting.DARK_GRAY + "" + TextFormatting.GREEN + "✓ " + TextFormatting.WHITE + "Удалено макросов: " + count);
    }

    private void printMacrosList() {
        if (macroManager.isEmpty()) {
            logger.log(TextFormatting.DARK_GRAY + "○ " + TextFormatting.GRAY + "Макросов пока нет");
            return;
        }
        logger.log(TextFormatting.DARK_GRAY + "┌ " + TextFormatting.WHITE + "Макросы " + TextFormatting.DARK_GRAY + "(" + macroManager.macroList.size() + ")");
        macroManager.macroList.forEach(macro -> {
            String keyName = KeyStorage.getKey(macro.getKey());
            logger.log(TextFormatting.DARK_GRAY + "│ " + TextFormatting.WHITE + macro.getName() + TextFormatting.DARK_GRAY + " [" + TextFormatting.GRAY + keyName + TextFormatting.DARK_GRAY + "] → " + TextFormatting.GRAY + macro.getMessage());
        });
        logger.log(TextFormatting.DARK_GRAY + "└───────────────");
    }
}
