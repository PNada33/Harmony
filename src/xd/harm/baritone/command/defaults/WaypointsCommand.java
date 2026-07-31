package xd.harm.baritone.command.defaults;

import xd.harm.baritone.Baritone;
import xd.harm.baritone.api.IBaritone;
import xd.harm.baritone.api.cache.IWaypoint;
import xd.harm.baritone.api.cache.IWorldData;
import xd.harm.baritone.api.cache.Waypoint;
import xd.harm.baritone.api.command.Command;
import xd.harm.baritone.api.command.argument.IArgConsumer;
import xd.harm.baritone.api.command.datatypes.ForWaypoints;
import xd.harm.baritone.api.command.datatypes.RelativeBlockPos;
import xd.harm.baritone.api.command.exception.CommandException;
import xd.harm.baritone.api.command.exception.CommandInvalidStateException;
import xd.harm.baritone.api.command.exception.CommandInvalidTypeException;
import xd.harm.baritone.api.command.helpers.Paginator;
import xd.harm.baritone.api.command.helpers.TabCompleteHelper;
import xd.harm.baritone.api.pathing.goals.Goal;
import xd.harm.baritone.api.pathing.goals.GoalBlock;
import xd.harm.baritone.api.utils.BetterBlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static xd.harm.baritone.api.command.IBaritoneChatControl.FORCE_COMMAND_PREFIX;

public class WaypointsCommand extends Command {

    private Map<IWorldData, List<IWaypoint>> deletedWaypoints = new HashMap<>();

    public WaypointsCommand(IBaritone baritone) {
        super(baritone, "waypoints", "waypoint", "wp");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        Action action = args.hasAny() ? Action.getByName(args.getString()) : Action.LIST;
        if (action == null) {
            throw new CommandInvalidTypeException(args.consumed(), "действие");
        }
        BiFunction<IWaypoint, Action, ITextComponent> toComponent = (waypoint, _action) -> {
            TextComponent component = new StringTextComponent("");
            TextComponent tagComponent = new StringTextComponent(waypoint.getTag().name() + " ");
            tagComponent.setStyle(tagComponent.getStyle().setFormatting(TextFormatting.GRAY));
            String name = waypoint.getName();
            TextComponent nameComponent = new StringTextComponent(!name.isEmpty() ? name : "<пусто>");
            nameComponent.setStyle(nameComponent.getStyle().setFormatting(!name.isEmpty() ? TextFormatting.GRAY : TextFormatting.DARK_GRAY));
            TextComponent timestamp = new StringTextComponent(" @ " + new Date(waypoint.getCreationTimestamp()));
            timestamp.setStyle(timestamp.getStyle().setFormatting(TextFormatting.DARK_GRAY));
            component.append(tagComponent);
            component.append(nameComponent);
            component.append(timestamp);
            component.setStyle(component.getStyle()
                    .setHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            new StringTextComponent("Нажмите для выбора")
                    ))
                    .setClickEvent(new ClickEvent(
                            ClickEvent.Action.RUN_COMMAND,
                            String.format(
                                    "%s%s %s %s @ %d",
                                    FORCE_COMMAND_PREFIX,
                                    label,
                                    _action.names[0],
                                    waypoint.getTag().getName(),
                                    waypoint.getCreationTimestamp()
                            ))
                    ));
            return component;
        };
        Function<IWaypoint, ITextComponent> transform = waypoint ->
                toComponent.apply(waypoint, action == Action.LIST ? Action.INFO : action);
        if (action == Action.LIST) {
            IWaypoint.Tag tag = args.hasAny() ? IWaypoint.Tag.getByName(args.peekString()) : null;
            if (tag != null) {
                args.get();
            }
            IWaypoint[] waypoints = tag != null
                    ? ForWaypoints.getWaypointsByTag(this.baritone, tag)
                    : ForWaypoints.getWaypoints(this.baritone);
            if (waypoints.length > 0) {
                args.requireMax(1);
                Paginator.paginate(
                        args,
                        waypoints,
                        () -> logDirect(
                                tag != null
                                        ? String.format("Все путевые точки по тегу %s:", tag.name())
                                        : "Все путевые точки:"
                        ),
                        transform,
                        String.format(
                                "%s%s %s%s",
                                FORCE_COMMAND_PREFIX,
                                label,
                                action.names[0],
                                tag != null ? " " + tag.getName() : ""
                        )
                );
            } else {
                args.requireMax(0);
                throw new CommandInvalidStateException(
                        tag != null
                                ? "Путевые точки с таким тегом не найдены"
                                : "Путевые точки не найдены"
                );
            }
        } else if (action == Action.SAVE) {
            IWaypoint.Tag tag = args.hasAny() ? IWaypoint.Tag.getByName(args.peekString()) : null;
            if (tag == null) {
                tag = IWaypoint.Tag.USER;
            } else {
                args.get();
            }
            String name = (args.hasExactlyOne() || args.hasExactly(4)) ? args.getString() : "";
            BetterBlockPos pos = args.hasAny()
                    ? args.getDatatypePost(RelativeBlockPos.INSTANCE, ctx.playerFeet())
                    : ctx.playerFeet();
            args.requireMax(0);
            IWaypoint waypoint = new Waypoint(name, tag, pos);
            ForWaypoints.waypoints(this.baritone).addWaypoint(waypoint);
            TextComponent component = new StringTextComponent("Путевая точка добавлена: ");
            component.setStyle(component.getStyle().setFormatting(TextFormatting.GRAY));
            component.append(toComponent.apply(waypoint, Action.INFO));
            logDirect(component);
        } else if (action == Action.CLEAR) {
            args.requireMax(1);
            IWaypoint.Tag tag = IWaypoint.Tag.getByName(args.getString());
            IWaypoint[] waypoints = ForWaypoints.getWaypointsByTag(this.baritone, tag);
            for (IWaypoint waypoint : waypoints) {
                ForWaypoints.waypoints(this.baritone).removeWaypoint(waypoint);
            }
            deletedWaypoints.computeIfAbsent(baritone.getWorldProvider().getCurrentWorld(), k -> new ArrayList<>()).addAll(Arrays.<IWaypoint>asList(waypoints));
            TextComponent textComponent = new StringTextComponent(String.format("Удалено %d путевых точек, нажмите для восстановления", waypoints.length));
            textComponent.setStyle(textComponent.getStyle().setClickEvent(new ClickEvent(
                    ClickEvent.Action.RUN_COMMAND,
                    String.format(
                            "%s%s restore @ %s",
                            FORCE_COMMAND_PREFIX,
                            label,
                            Stream.of(waypoints).map(wp -> Long.toString(wp.getCreationTimestamp())).collect(Collectors.joining(" "))
                    )
            )));
            logDirect(textComponent);
        } else if (action == Action.RESTORE) {
            List<IWaypoint> waypoints = new ArrayList<>();
            List<IWaypoint> deletedWaypoints = this.deletedWaypoints.getOrDefault(baritone.getWorldProvider().getCurrentWorld(), Collections.emptyList());
            if (args.peekString().equals("@")) {
                args.get();
                while (args.hasAny()) {
                    long timestamp = args.getAs(Long.class);
                    for (IWaypoint waypoint : deletedWaypoints) {
                        if (waypoint.getCreationTimestamp() == timestamp) {
                            waypoints.add(waypoint);
                            break;
                        }
                    }
                }
            } else {
                args.requireExactly(1);
                int size = deletedWaypoints.size();
                int amount = Math.min(size, args.getAs(Integer.class));
                waypoints = new ArrayList<>(deletedWaypoints.subList(size - amount, size));
            }
            waypoints.forEach(ForWaypoints.waypoints(this.baritone)::addWaypoint);
            deletedWaypoints.removeIf(waypoints::contains);
            logDirect(String.format("Восстановлено %d путевых точек", waypoints.size()));
        } else {
            IWaypoint[] waypoints = args.getDatatypeFor(ForWaypoints.INSTANCE);
            IWaypoint waypoint = null;
            if (args.hasAny() && args.peekString().equals("@")) {
                args.requireExactly(2);
                args.get();
                long timestamp = args.getAs(Long.class);
                for (IWaypoint iWaypoint : waypoints) {
                    if (iWaypoint.getCreationTimestamp() == timestamp) {
                        waypoint = iWaypoint;
                        break;
                    }
                }
                if (waypoint == null) {
                    throw new CommandInvalidStateException("Указана метка времени, но путевая точка не найдена");
                }
            } else {
                switch (waypoints.length) {
                    case 0:
                        throw new CommandInvalidStateException("Путевые точки не найдены");
                    case 1:
                        waypoint = waypoints[0];
                        break;
                    default:
                        break;
                }
            }
            if (waypoint == null) {
                args.requireMax(1);
                Paginator.paginate(
                        args,
                        waypoints,
                        () -> logDirect("Найдено несколько путевых точек:"),
                        transform,
                        String.format(
                                "%s%s %s %s",
                                FORCE_COMMAND_PREFIX,
                                label,
                                action.names[0],
                                args.consumedString()
                        )
                );
            } else {
                if (action == Action.INFO) {
                    logDirect(transform.apply(waypoint));
                    logDirect(String.format("Позиция: %s", waypoint.getLocation()));
                    TextComponent deleteComponent = new StringTextComponent("Нажмите, чтобы удалить эту путевую точку");
                    deleteComponent.setStyle(deleteComponent.getStyle().setClickEvent(new ClickEvent(
                            ClickEvent.Action.RUN_COMMAND,
                            String.format(
                                    "%s%s delete %s @ %d",
                                    FORCE_COMMAND_PREFIX,
                                    label,
                                    waypoint.getTag().getName(),
                                    waypoint.getCreationTimestamp()
                            )
                    )));
                    TextComponent goalComponent = new StringTextComponent("Нажмите, чтобы установить цель на эту путевую точку");
                    goalComponent.setStyle(goalComponent.getStyle().setClickEvent(new ClickEvent(
                            ClickEvent.Action.RUN_COMMAND,
                            String.format(
                                    "%s%s goal %s @ %d",
                                    FORCE_COMMAND_PREFIX,
                                    label,
                                    waypoint.getTag().getName(),
                                    waypoint.getCreationTimestamp()
                            )
                    )));
                    TextComponent recreateComponent = new StringTextComponent("Нажмите, чтобы показать команду для воссоздания этой путевой точки");
                    recreateComponent.setStyle(recreateComponent.getStyle().setClickEvent(new ClickEvent(
                            ClickEvent.Action.SUGGEST_COMMAND,
                            String.format(
                                    "%s%s save %s %s %s %s %s",
                                    Baritone.settings().prefix.value,
                                    label,
                                    waypoint.getTag().getName(),
                                    waypoint.getName(),
                                    waypoint.getLocation().x,
                                    waypoint.getLocation().y,
                                    waypoint.getLocation().z
                            )
                    )));
                    TextComponent backComponent = new StringTextComponent("Нажмите, чтобы вернуться к списку путевых точек");
                    backComponent.setStyle(backComponent.getStyle().setClickEvent(new ClickEvent(
                            ClickEvent.Action.RUN_COMMAND,
                            String.format(
                                    "%s%s list",
                                    FORCE_COMMAND_PREFIX,
                                    label
                            )
                    )));
                    logDirect(deleteComponent);
                    logDirect(goalComponent);
                    logDirect(recreateComponent);
                    logDirect(backComponent);
                } else if (action == Action.DELETE) {
                    ForWaypoints.waypoints(this.baritone).removeWaypoint(waypoint);
                    deletedWaypoints.computeIfAbsent(baritone.getWorldProvider().getCurrentWorld(), k -> new ArrayList<>()).add(waypoint);
                    TextComponent textComponent = new StringTextComponent("Путевая точка успешно удалена, нажмите для восстановления");
                    textComponent.setStyle(textComponent.getStyle().setClickEvent(new ClickEvent(
                            ClickEvent.Action.RUN_COMMAND,
                            String.format(
                                    "%s%s restore @ %s",
                                    FORCE_COMMAND_PREFIX,
                                    label,
                                    waypoint.getCreationTimestamp()
                            )
                    )));
                    logDirect(textComponent);
                } else if (action == Action.GOAL) {
                    Goal goal = new GoalBlock(waypoint.getLocation());
                    baritone.getCustomGoalProcess().setGoal(goal);
                    logDirect(String.format("Цель: %s", goal));
                } else if (action == Action.GOTO) {
                    Goal goal = new GoalBlock(waypoint.getLocation());
                    baritone.getCustomGoalProcess().setGoalAndPath(goal);
                    logDirect(String.format("Иду к: %s", goal));
                }
            }
        }
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (args.hasAny()) {
            if (args.hasExactlyOne()) {
                return new TabCompleteHelper()
                        .append(Action.getAllNames())
                        .sortAlphabetically()
                        .filterPrefix(args.getString())
                        .stream();
            } else {
                Action action = Action.getByName(args.getString());
                if (args.hasExactlyOne()) {
                    if (action == Action.LIST || action == Action.SAVE || action == Action.CLEAR) {
                        return new TabCompleteHelper()
                                .append(IWaypoint.Tag.getAllNames())
                                .sortAlphabetically()
                                .filterPrefix(args.getString())
                                .stream();
                    } else if (action == Action.RESTORE) {
                        return Stream.empty();
                    } else {
                        return args.tabCompleteDatatype(ForWaypoints.INSTANCE);
                    }
                } else if (args.has(3) && action == Action.SAVE) {
                    args.get();
                    args.get();
                    return args.tabCompleteDatatype(RelativeBlockPos.INSTANCE);
                }
            }
        }
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Управление путевыми точками";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Команда waypoint позволяет управлять путевыми точками Baritone.",
                "",
                "Путевые точки можно использовать для отметки позиций на будущее. Каждая путевая точка имеет тег и необязательное имя.",
                "",
                "Обратите внимание, что команды info, delete и goal позволяют указать путевую точку по тегу. Если с определенным тегом связано несколько путевых точек, вы сможете выбрать нужную.",
                "",
                "Отсутствующие аргументы для команды save используют тег USER, создавая точку без имени и используя вашу текущую позицию по умолчанию.",
                "",
                "Использование:",
                "> wp [l/list] - Список всех путевых точек.",
                "> wp <l/list> <тег> - Список всех путевых точек по тегу.",
                "> wp <s/save> - Сохранение неназванной точки USER в вашей текущей позиции.",
                "> wp <s/save> [тег] [имя] [позиция] - Сохранение точки с указанным тегом, именем и позицией.",
                "> wp <i/info/show> <тег/имя> - Показать информацию о путевой точке по тегу или имени.",
                "> wp <d/delete> <тег/имя> - Удалить путевую точку по тегу или имени.",
                "> wp <restore> <n> - Восстановить последние n удаленных путевых точек.",
                "> wp <c/clear> <тег> - Удалить все путевые точки с указанным тегом.",
                "> wp <g/goal> <тег/имя> - Установить цель на путевую точку по тегу или имени.",
                "> wp <goto> <тег/имя> - Установить цель на путевую точку по тегу или имени и начать движение."
        );
    }

    private enum Action {
        LIST("list", "get", "l"),
        CLEAR("clear", "c"),
        SAVE("save", "s"),
        INFO("info", "show", "i"),
        DELETE("delete", "d"),
        RESTORE("restore"),
        GOAL("goal", "g"),
        GOTO("goto");
        private final String[] names;

        Action(String... names) {
            this.names = names;
        }

        public static Action getByName(String name) {
            for (Action action : Action.values()) {
                for (String alias : action.names) {
                    if (alias.equalsIgnoreCase(name)) {
                        return action;
                    }
                }
            }
            return null;
        }

        public static String[] getAllNames() {
            Set<String> names = new HashSet<>();
            for (Action action : Action.values()) {
                names.addAll(Arrays.asList(action.names));
            }
            return names.toArray(new String[0]);
        }
    }
}