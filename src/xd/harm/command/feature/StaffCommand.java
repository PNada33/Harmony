package xd.harm.command.feature;

import xd.harm.command.interfaces.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextFormatting;
import xd.harm.command.api.CommandException;
import xd.harm.config.StaffStorage;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StaffCommand implements Command {

    final Prefix prefix;
    final Logger logger;

    @Override
    public void execute(Parameters parameters) {
        String commandType = parameters.asString(0).orElseThrow(() ->
                new CommandException(TextFormatting.RED + "× " + TextFormatting.WHITE + "Действия: " + TextFormatting.GRAY + "add · remove · clear · list"));
        switch (commandType) {
            case "add" -> addStaffToList(parameters);
            case "remove" -> removeStaffFromList(parameters);
            case "clear" -> clearStaffList();
            case "list" -> getStaffList();
            default -> throw new CommandException(TextFormatting.RED + "× " + TextFormatting.WHITE + "Действия: " + TextFormatting.GRAY + "add · remove · clear · list");
        }
    }

    private void addStaffToList(Parameters parameters) {
        String staffName = parameters.asString(1)
                .orElseThrow(() -> new CommandException(TextFormatting.RED + "× " + TextFormatting.RED + "Укажите никнейм"));

        if (staffName.equalsIgnoreCase(Minecraft.getInstance().player.getName().getString())) {
            logger.log(TextFormatting.DARK_GRAY + "" + TextFormatting.RED + "× " + TextFormatting.GRAY + "Ты не можешь быть модером :)");
            return;
        }

        if (StaffStorage.isStaff(staffName)) {
            logger.log(TextFormatting.DARK_GRAY + "○ " + TextFormatting.WHITE + staffName + TextFormatting.GRAY + " уже в списке");
            return;
        }

        StaffStorage.add(staffName);
        logger.log(TextFormatting.DARK_GRAY + "" + TextFormatting.GREEN + "⚠ " + TextFormatting.WHITE + staffName + TextFormatting.GRAY + " добавлен в стафф-лист");
    }

    private void removeStaffFromList(Parameters parameters) {
        String staff = parameters.asString(1)
                .orElseThrow(() -> new CommandException(TextFormatting.RED + "× " + TextFormatting.RED + "Укажите никнейм"));
        if (StaffStorage.isStaff(staff)) {
            StaffStorage.remove(staff);
            logger.log(TextFormatting.DARK_GRAY + "" + TextFormatting.GREEN + "✓ " + TextFormatting.WHITE + staff + TextFormatting.GRAY + " удалён из стафф-листа");
            return;
        }
        logger.log(TextFormatting.DARK_GRAY + "" + TextFormatting.RED + "× " + TextFormatting.WHITE + staff + TextFormatting.GRAY + " не найден в списке");
    }

    private void getStaffList() {
        if (StaffStorage.getStaffs().isEmpty()) {
            logger.log(TextFormatting.DARK_GRAY + "○ " + TextFormatting.GRAY + "Стафф-лист пуст");
            return;
        }
        logger.log(TextFormatting.DARK_GRAY + "┌ " + TextFormatting.WHITE + "Стафф " + TextFormatting.DARK_GRAY + "(" + StaffStorage.getStaffs().size() + ")");
        for (String staff : StaffStorage.getStaffs()) {
            logger.log(TextFormatting.DARK_GRAY + "│ " + TextFormatting.YELLOW + "⚠ " + TextFormatting.WHITE + staff);
        }
        logger.log(TextFormatting.DARK_GRAY + "└───────────────");
    }

    private void clearStaffList() {
        if (StaffStorage.getStaffs().isEmpty()) {
            logger.log(TextFormatting.DARK_GRAY + "○ " + TextFormatting.GRAY + "Список уже пуст");
            return;
        }
        int count = StaffStorage.getStaffs().size();
        StaffStorage.clear();
        logger.log(TextFormatting.DARK_GRAY + "" + TextFormatting.GREEN + "✓ " + TextFormatting.WHITE + "Удалено из стаффа: " + count);
    }

    @Override
    public String name() {
        return "staff";
    }

    @Override
    public String description() {
        return "Управление стафф-листом";
    }
}