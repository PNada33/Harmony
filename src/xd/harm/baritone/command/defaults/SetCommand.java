package xd.harm.baritone.command.defaults;

import xd.harm.baritone.Baritone;
import xd.harm.baritone.api.IBaritone;
import xd.harm.baritone.api.Settings;
import xd.harm.baritone.api.command.Command;
import xd.harm.baritone.api.command.argument.IArgConsumer;
import xd.harm.baritone.api.command.exception.CommandException;
import xd.harm.baritone.api.command.exception.CommandInvalidStateException;
import xd.harm.baritone.api.command.exception.CommandInvalidTypeException;
import xd.harm.baritone.api.command.helpers.Paginator;
import xd.harm.baritone.api.command.helpers.TabCompleteHelper;
import xd.harm.baritone.api.utils.SettingsUtil;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static xd.harm.baritone.api.command.IBaritoneChatControl.FORCE_COMMAND_PREFIX;
import static xd.harm.baritone.api.utils.SettingsUtil.*;

public class SetCommand extends Command {

    public SetCommand(IBaritone baritone) {
        super(baritone, "set", "setting", "settings");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        String arg = args.hasAny() ? args.getString().toLowerCase(Locale.US) : "list";
        if (Arrays.asList("s", "save").contains(arg)) {
            SettingsUtil.save(Baritone.settings());
            logDirect("Настройки сохранены");
            return;
        }
        boolean viewModified = Arrays.asList("m", "mod", "modified").contains(arg);
        boolean viewAll = Arrays.asList("all", "l", "list").contains(arg);
        boolean paginate = viewModified || viewAll;
        if (paginate) {
            String search = args.hasAny() && args.peekAsOrNull(Integer.class) == null ? args.getString() : "";
            args.requireMax(1);
            List<? extends Settings.Setting> toPaginate =
                    (viewModified ? SettingsUtil.modifiedSettings(Baritone.settings()) : Baritone.settings().allSettings).stream()
                            .filter(s -> !javaOnlySetting(s))
                            .filter(s -> s.getName().toLowerCase(Locale.US).contains(search.toLowerCase(Locale.US)))
                            .sorted((s1, s2) -> String.CASE_INSENSITIVE_ORDER.compare(s1.getName(), s2.getName()))
                            .collect(Collectors.toList());
            Paginator.paginate(
                    args,
                    new Paginator<>(toPaginate),
                    () -> logDirect(
                            !search.isEmpty()
                                    ? String.format("Все %sнастройки, содержащие строку '%s':", viewModified ? "измененные " : "", search)
                                    : String.format("Все %sнастройки:", viewModified ? "измененные " : "")
                    ),
                    setting -> {
                        TextComponent typeComponent = new StringTextComponent(String.format(
                                " (%s)",
                                settingTypeToString(setting)
                        ));
                        typeComponent.setStyle(typeComponent.getStyle().setFormatting(TextFormatting.DARK_GRAY));
                        TextComponent hoverComponent = new StringTextComponent("");
                        hoverComponent.setStyle(hoverComponent.getStyle().setFormatting(TextFormatting.GRAY));
                        hoverComponent.appendString(setting.getName());
                        hoverComponent.appendString(String.format("\nТип: %s", settingTypeToString(setting)));
                        hoverComponent.appendString(String.format("\n\nЗначение:\n%s", settingValueToString(setting)));
                        hoverComponent.appendString(String.format("\n\nЗначение по умолчанию:\n%s", settingDefaultToString(setting)));
                        String commandSuggestion = Baritone.settings().prefix.value + String.format("set %s ", setting.getName());
                        TextComponent component = new StringTextComponent(setting.getName());
                        component.setStyle(component.getStyle().setFormatting(TextFormatting.GRAY));
                        component.append(typeComponent);
                        component.setStyle(component.getStyle()
                                .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hoverComponent))
                                .setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, commandSuggestion)));
                        return component;
                    },
                    FORCE_COMMAND_PREFIX + "set " + arg + " " + search
            );
            return;
        }
        args.requireMax(1);
        boolean resetting = arg.equalsIgnoreCase("reset");
        boolean toggling = arg.equalsIgnoreCase("toggle");
        boolean doingSomething = resetting || toggling;
        if (resetting) {
            if (!args.hasAny()) {
                logDirect("Пожалуйста, укажите 'all' в качестве аргумента для сброса, чтобы подтвердить, что вы действительно хотите это сделать");
                logDirect("ВСЕ настройки будут сброшены. Используйте команды 'set modified' или 'modified', чтобы увидеть, что будет сброшено.");
                logDirect("Укажите имя настройки вместо 'all', чтобы сбросить только одну настройку");
            } else if (args.peekString().equalsIgnoreCase("all")) {
                SettingsUtil.modifiedSettings(Baritone.settings()).forEach(Settings.Setting::reset);
                logDirect("Все настройки сброшены на значения по умолчанию");
                SettingsUtil.save(Baritone.settings());
                return;
            }
        }
        if (toggling) {
            args.requireMin(1);
        }
        String settingName = doingSomething ? args.getString() : arg;
        Settings.Setting<?> setting = Baritone.settings().allSettings.stream()
                .filter(s -> s.getName().equalsIgnoreCase(settingName))
                .findFirst()
                .orElse(null);
        if (setting == null) {
            throw new CommandInvalidTypeException(args.consumed(), "действительная настройка");
        }
        if (javaOnlySetting(setting)) {
            throw new CommandInvalidStateException(String.format("Настройка %s может использоваться только через API.", setting.getName()));
        }
        if (!doingSomething && !args.hasAny()) {
            logDirect(String.format("Значение настройки %s:", setting.getName()));
            logDirect(settingValueToString(setting));
        } else {
            String oldValue = settingValueToString(setting);
            if (resetting) {
                setting.reset();
            } else if (toggling) {
                if (setting.getValueClass() != Boolean.class) {
                    throw new CommandInvalidTypeException(args.consumed(), "переключаемая настройка", "другая настройка");
                }
                Settings.Setting<Boolean> asBoolSetting = (Settings.Setting<Boolean>) setting;
                asBoolSetting.value ^= true;
                logDirect(String.format(
                        "Переключена настройка %s на %s",
                        setting.getName(),
                        Boolean.toString((Boolean) setting.value)
                ));
            } else {
                String newValue = args.getString();
                try {
                    SettingsUtil.parseAndApply(Baritone.settings(), arg, newValue);
                } catch (Throwable t) {
                    t.printStackTrace();
                    throw new CommandInvalidTypeException(args.consumed(), "действительное значение", t);
                }
            }
            if (!toggling) {
                logDirect(String.format(
                        "Успешно %s настройка %s на %s",
                        resetting ? "сброшена" : "установлена",
                        setting.getName(),
                        settingValueToString(setting)
                ));
            }
            TextComponent oldValueComponent = new StringTextComponent(String.format("Старое значение: %s", oldValue));
            oldValueComponent.setStyle(oldValueComponent.getStyle()
                    .setFormatting(TextFormatting.GRAY)
                    .setHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            new StringTextComponent("Нажмите, чтобы вернуть настройку к этому значению")
                    ))
                    .setClickEvent(new ClickEvent(
                            ClickEvent.Action.RUN_COMMAND,
                            FORCE_COMMAND_PREFIX + String.format("set %s %s", setting.getName(), oldValue)
                    )));
            logDirect(oldValueComponent);
            if ((setting.getName().equals("chatControl") && !(Boolean) setting.value && !Baritone.settings().chatControlAnyway.value) ||
                    setting.getName().equals("chatControlAnyway") && !(Boolean) setting.value && !Baritone.settings().chatControl.value) {
                logDirect("Предупреждение: Команды в чате больше не будут работать. Если вы хотите отменить это изменение, используйте управление префиксом (если включено) или нажмите на старое значение выше.", TextFormatting.RED);
            } else if (setting.getName().equals("prefixControl") && !(Boolean) setting.value) {
                logDirect("Предупреждение: Команды с префиксом больше не будут работать. Если вы хотите отменить это изменение, используйте управление чатом (если включено) или нажмите на старое значение выше.", TextFormatting.RED);
            }
        }
        SettingsUtil.save(Baritone.settings());
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (args.hasAny()) {
            String arg = args.getString();
            if (args.hasExactlyOne() && !Arrays.asList("s", "save").contains(args.peekString().toLowerCase(Locale.US))) {
                if (arg.equalsIgnoreCase("reset")) {
                    return new TabCompleteHelper()
                            .addModifiedSettings()
                            .prepend("all")
                            .filterPrefix(args.getString())
                            .stream();
                } else if (arg.equalsIgnoreCase("toggle")) {
                    return new TabCompleteHelper()
                            .addToggleableSettings()
                            .filterPrefix(args.getString())
                            .stream();
                }
                Settings.Setting setting = Baritone.settings().byLowerName.get(arg.toLowerCase(Locale.US));
                if (setting != null) {
                    if (setting.getType() == Boolean.class) {
                        TabCompleteHelper helper = new TabCompleteHelper();
                        if ((Boolean) setting.value) {
                            helper.append("true", "false");
                        } else {
                            helper.append("false", "true");
                        }
                        return helper.filterPrefix(args.getString()).stream();
                    } else {
                        return Stream.of(settingValueToString(setting));
                    }
                }
            } else if (!args.hasAny()) {
                return new TabCompleteHelper()
                        .addSettings()
                        .sortAlphabetically()
                        .prepend("list", "modified", "reset", "toggle", "save")
                        .filterPrefix(arg)
                        .stream();
            }
        }
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Просмотр или изменение настроек";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "С помощью команды set вы можете управлять всеми настройками Baritone. Почти каждый аспект контролируется этими настройками — экспериментируйте!",
                "",
                "Использование:",
                "> set - То же, что `set list`",
                "> set list [page] - Просмотреть все настройки",
                "> set modified [page] - Просмотреть измененные настройки",
                "> set <setting> - Просмотреть текущее значение настройки",
                "> set <setting> <value> - Установить значение настройки",
                "> set reset all - Сбросить ВСЕ НАСТРОЙКИ на значения по умолчанию",
                "> set reset <setting> - Сбросить настройку на значение по умолчанию",
                "> set toggle <setting> - Переключить булеву настройку",
                "> set save - Сохранить все настройки (хотя это делается автоматически)"
        );
    }
}