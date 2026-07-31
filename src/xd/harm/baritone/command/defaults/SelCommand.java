package xd.harm.baritone.command.defaults;

import xd.harm.baritone.Baritone;
import xd.harm.baritone.api.IBaritone;
import xd.harm.baritone.api.command.Command;
import xd.harm.baritone.api.command.argument.IArgConsumer;
import xd.harm.baritone.api.command.datatypes.ForBlockOptionalMeta;
import xd.harm.baritone.api.command.datatypes.ForDirection;
import xd.harm.baritone.api.command.datatypes.RelativeBlockPos;
import xd.harm.baritone.api.command.exception.CommandException;
import xd.harm.baritone.api.command.exception.CommandInvalidStateException;
import xd.harm.baritone.api.command.exception.CommandInvalidTypeException;
import xd.harm.baritone.api.command.helpers.TabCompleteHelper;
import xd.harm.baritone.api.event.events.RenderEvent;
import xd.harm.baritone.api.event.listener.AbstractGameEventListener;
import xd.harm.baritone.api.schematic.*;
import xd.harm.baritone.api.selection.ISelection;
import xd.harm.baritone.api.selection.ISelectionManager;
import xd.harm.baritone.api.utils.BetterBlockPos;
import xd.harm.baritone.api.utils.BlockOptionalMeta;
import xd.harm.baritone.api.utils.BlockOptionalMetaLookup;
import xd.harm.baritone.utils.BlockStateInterface;
import xd.harm.baritone.utils.IRenderer;
import xd.harm.baritone.utils.schematic.StaticSchematic;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3i;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

public class SelCommand extends Command {

    private ISelectionManager manager = baritone.getSelectionManager();
    private BetterBlockPos pos1 = null;
    private ISchematic clipboard = null;
    private Vector3i clipboardOffset = null;

    public SelCommand(IBaritone baritone) {
        super(baritone, "sel", "selection", "s");
        baritone.getGameEventHandler().registerEventListener(new AbstractGameEventListener() {
            @Override
            public void onRenderPass(RenderEvent event) {
                if (!Baritone.settings().renderSelectionCorners.value || pos1 == null) {
                    return;
                }
                Color color = Baritone.settings().colorSelectionPos1.value;
                float opacity = Baritone.settings().selectionOpacity.value;
                float lineWidth = Baritone.settings().selectionLineWidth.value;
                boolean ignoreDepth = Baritone.settings().renderSelectionIgnoreDepth.value;
                IRenderer.startLines(color, opacity, lineWidth, ignoreDepth);
                IRenderer.drawAABB(event.getModelViewStack(), new AxisAlignedBB(pos1, pos1.add(1, 1, 1)));
                IRenderer.endLines(ignoreDepth);
            }
        });
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        Action action = Action.getByName(args.getString());
        if (action == null) {
            throw new CommandInvalidTypeException(args.consumed(), "действие");
        }
        if (action == Action.POS1 || action == Action.POS2) {
            if (action == Action.POS2 && pos1 == null) {
                throw new CommandInvalidStateException("Сначала установите pos1 перед использованием pos2");
            }
            BetterBlockPos playerPos = mc.getRenderViewEntity() != null ? BetterBlockPos.from(mc.getRenderViewEntity().getPosition()) : ctx.playerFeet();
            BetterBlockPos pos = args.hasAny() ? args.getDatatypePost(RelativeBlockPos.INSTANCE, playerPos) : playerPos;
            args.requireMax(0);
            if (action == Action.POS1) {
                pos1 = pos;
                logDirect("Позиция 1 установлена");
            } else {
                manager.addSelection(pos1, pos);
                pos1 = null;
                logDirect("Выделение добавлено");
            }
        } else if (action == Action.CLEAR) {
            args.requireMax(0);
            pos1 = null;
            logDirect(String.format("Удалено %d выделений", manager.removeAllSelections().length));
        } else if (action == Action.UNDO) {
            args.requireMax(0);
            if (pos1 != null) {
                pos1 = null;
                logDirect("Отменено pos1");
            } else {
                ISelection[] selections = manager.getSelections();
                if (selections.length < 1) {
                    throw new CommandInvalidStateException("Нечего отменять!");
                } else {
                    pos1 = manager.removeSelection(selections[selections.length - 1]).pos1();
                    logDirect("Отменено pos2");
                }
            }
        } else if (action == Action.SET || action == Action.WALLS || action == Action.SHELL || action == Action.CLEARAREA || action == Action.REPLACE) {
            BlockOptionalMeta type = action == Action.CLEARAREA
                    ? new BlockOptionalMeta(Blocks.AIR)
                    : args.getDatatypeFor(ForBlockOptionalMeta.INSTANCE);
            BlockOptionalMetaLookup replaces = null;
            if (action == Action.REPLACE) {
                args.requireMin(1);
                List<BlockOptionalMeta> replacesList = new ArrayList<>();
                replacesList.add(type);
                while (args.has(2)) {
                    replacesList.add(args.getDatatypeFor(ForBlockOptionalMeta.INSTANCE));
                }
                type = args.getDatatypeFor(ForBlockOptionalMeta.INSTANCE);
                replaces = new BlockOptionalMetaLookup(replacesList.toArray(new BlockOptionalMeta[0]));
            } else {
                args.requireMax(0);
            }
            ISelection[] selections = manager.getSelections();
            if (selections.length == 0) {
                throw new CommandInvalidStateException("Нет выделений");
            }
            BetterBlockPos origin = selections[0].min();
            CompositeSchematic composite = new CompositeSchematic(0, 0, 0);
            for (ISelection selection : selections) {
                BetterBlockPos min = selection.min();
                origin = new BetterBlockPos(
                        Math.min(origin.x, min.x),
                        Math.min(origin.y, min.y),
                        Math.min(origin.z, min.z)
                );
            }
            for (ISelection selection : selections) {
                Vector3i size = selection.size();
                BetterBlockPos min = selection.min();
                ISchematic schematic = new FillSchematic(size.getX(), size.getY(), size.getZ(), type);
                if (action == Action.WALLS) {
                    schematic = new WallsSchematic(schematic);
                } else if (action == Action.SHELL) {
                    schematic = new ShellSchematic(schematic);
                } else if (action == Action.REPLACE) {
                    schematic = new ReplaceSchematic(schematic, replaces);
                }
                composite.put(schematic, min.x - origin.x, min.y - origin.y, min.z - origin.z);
            }
            baritone.getBuilderProcess().build("Fill", composite, origin);
            logDirect("Теперь заполняется");
        } else if (action == Action.COPY) {
            BetterBlockPos playerPos = mc.getRenderViewEntity() != null ? BetterBlockPos.from(mc.getRenderViewEntity().getPosition()) : ctx.playerFeet();
            BetterBlockPos pos = args.hasAny() ? args.getDatatypePost(RelativeBlockPos.INSTANCE, playerPos) : playerPos;
            args.requireMax(0);
            ISelection[] selections = manager.getSelections();
            if (selections.length < 1) {
                throw new CommandInvalidStateException("Нет выделений");
            }
            BlockStateInterface bsi = new BlockStateInterface(ctx);
            BetterBlockPos origin = selections[0].min();
            CompositeSchematic composite = new CompositeSchematic(0, 0, 0);
            for (ISelection selection : selections) {
                BetterBlockPos min = selection.min();
                origin = new BetterBlockPos(
                        Math.min(origin.x, min.x),
                        Math.min(origin.y, min.y),
                        Math.min(origin.z, min.z)
                );
            }
            for (ISelection selection : selections) {
                Vector3i size = selection.size();
                BetterBlockPos min = selection.min();
                BlockState[][][] blockstates = new BlockState[size.getX()][size.getZ()][size.getY()];
                for (int x = 0; x < size.getX(); x++) {
                    for (int y = 0; y < size.getY(); y++) {
                        for (int z = 0; z < size.getZ(); z++) {
                            blockstates[x][z][y] = bsi.get0(min.x + x, min.y + y, min.z + z);
                        }
                    }
                }
                ISchematic schematic = new StaticSchematic() {{
                    states = blockstates;
                    x = size.getX();
                    y = size.getY();
                    z = size.getZ();
                }};
                composite.put(schematic, min.x - origin.x, min.y - origin.y, min.z - origin.z);
            }
            clipboard = composite;
            clipboardOffset = origin.subtract(pos);
            logDirect("Выделение скопировано");
        } else if (action == Action.PASTE) {
            BetterBlockPos playerPos = mc.getRenderViewEntity() != null ? BetterBlockPos.from(mc.getRenderViewEntity().getPosition()) : ctx.playerFeet();
            BetterBlockPos pos = args.hasAny() ? args.getDatatypePost(RelativeBlockPos.INSTANCE, playerPos) : playerPos;
            args.requireMax(0);
            if (clipboard == null) {
                throw new CommandInvalidStateException("Сначала нужно скопировать выделение");
            }
            baritone.getBuilderProcess().build("Fill", clipboard, pos.add(clipboardOffset));
            logDirect("Теперь строится");
        } else if (action == Action.EXPAND || action == Action.CONTRACT || action == Action.SHIFT) {
            args.requireExactly(3);
            TransformTarget transformTarget = TransformTarget.getByName(args.getString());
            if (transformTarget == null) {
                throw new CommandInvalidStateException("Недопустимый тип трансформации");
            }
            Direction direction = args.getDatatypeFor(ForDirection.INSTANCE);
            int blocks = args.getAs(Integer.class);
            ISelection[] selections = manager.getSelections();
            if (selections.length < 1) {
                throw new CommandInvalidStateException("Выделения не найдены");
            }
            selections = transformTarget.transform(selections);
            for (ISelection selection : selections) {
                if (action == Action.EXPAND) {
                    manager.expand(selection, direction, blocks);
                } else if (action == Action.CONTRACT) {
                    manager.contract(selection, direction, blocks);
                } else {
                    manager.shift(selection, direction, blocks);
                }
            }
            logDirect(String.format("Трансформировано %d выделений", selections.length));
        }
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (args.hasExactlyOne()) {
            return new TabCompleteHelper()
                    .append(Action.getAllNames())
                    .filterPrefix(args.getString())
                    .sortAlphabetically()
                    .stream();
        } else {
            Action action = Action.getByName(args.getString());
            if (action != null) {
                if (action == Action.POS1 || action == Action.POS2) {
                    if (args.hasAtMost(3)) {
                        return args.tabCompleteDatatype(RelativeBlockPos.INSTANCE);
                    }
                } else if (action == Action.SET || action == Action.WALLS || action == Action.CLEARAREA || action == Action.REPLACE) {
                    if (args.hasExactlyOne() || action == Action.REPLACE) {
                        while (args.has(2)) {
                            args.get();
                        }
                        return args.tabCompleteDatatype(ForBlockOptionalMeta.INSTANCE);
                    }
                } else if (action == Action.EXPAND || action == Action.CONTRACT || action == Action.SHIFT) {
                    if (args.hasExactlyOne()) {
                        return new TabCompleteHelper()
                                .append(TransformTarget.getAllNames())
                                .filterPrefix(args.getString())
                                .sortAlphabetically()
                                .stream();
                    } else {
                        TransformTarget target = TransformTarget.getByName(args.getString());
                        if (target != null && args.hasExactlyOne()) {
                            return args.tabCompleteDatatype(ForDirection.INSTANCE);
                        }
                    }
                }
            }
        }
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Команды в стиле WorldEdit";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Команда sel позволяет манипулировать выделениями Baritone, подобно WorldEdit.",
                "",
                "С помощью этих выделений вы можете очищать области, заполнять их блоками или выполнять другие действия.",
                "",
                "Команды expand/contract/shift используют своего рода селектор для выбора целевых выделений. Поддерживаемые: a/all, n/newest и o/oldest.",
                "",
                "Использование:",
                "> sel pos1/p1/1 - Установить позицию 1 на вашу текущую позицию.",
                "> sel pos1/p1/1 <x> <y> <z> - Установить позицию 1 на относительную позицию.",
                "> sel pos2/p2/2 - Установить позицию 2 на вашу текущую позицию.",
                "> sel pos2/p2/2 <x> <y> <z> - Установить позицию 2 на относительную позицию.",
                "",
                "> sel clear/c - Очистить выделение.",
                "> sel undo/u - Отменить последнее действие (установка позиций, создание выделений и т.д.).",
                "> sel set/fill/s/f [block] - Полностью заполнить все выделения блоком.",
                "> sel walls/w [block] - Заполнить стены выделения указанным блоком.",
                "> sel shell/shl [block] - То же, что walls, но также заполняет потолок и пол.",
                "> sel cleararea/ca - По сути, 'set air'.",
                "> sel replace/r <blocks...> <with> - Заменяет блоки другим блоком.",
                "> sel copy/cp <x> <y> <z> - Скопировать выделенную область относительно указанной или вашей позиции.",
                "> sel paste/p <x> <y> <z> - Построить скопированную область относительно указанной или вашей позиции.",
                "",
                "> sel expand <target> <direction> <blocks> - Расширить цели.",
                "> sel contract <target> <direction> <blocks> - Сжать цели.",
                "> sel shift <target> <direction> <blocks> - Сдвинуть цели (не изменяет размер)."
        );
    }

    enum Action {
        POS1("pos1", "p1", "1"),
        POS2("pos2", "p2", "2"),
        CLEAR("clear", "c"),
        UNDO("undo", "u"),
        SET("set", "fill", "s", "f"),
        WALLS("walls", "w"),
        SHELL("shell", "shl"),
        CLEARAREA("cleararea", "ca"),
        REPLACE("replace", "r"),
        EXPAND("expand", "ex"),
        COPY("copy", "cp"),
        PASTE("paste", "p"),
        CONTRACT("contract", "ct"),
        SHIFT("shift", "sh");
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

    enum TransformTarget {
        ALL(sels -> sels, "all", "a"),
        NEWEST(sels -> new ISelection[]{sels[sels.length - 1]}, "newest", "n"),
        OLDEST(sels -> new ISelection[]{sels[0]}, "oldest", "o");
        private final Function<ISelection[], ISelection[]> transform;
        private final String[] names;

        TransformTarget(Function<ISelection[], ISelection[]> transform, String... names) {
            this.transform = transform;
            this.names = names;
        }

        public ISelection[] transform(ISelection[] selections) {
            return transform.apply(selections);
        }

        public static TransformTarget getByName(String name) {
            for (TransformTarget target : TransformTarget.values()) {
                for (String alias : target.names) {
                    if (alias.equalsIgnoreCase(name)) {
                        return target;
                    }
                }
            }
            return null;
        }

        public static String[] getAllNames() {
            Set<String> names = new HashSet<>();
            for (TransformTarget target : TransformTarget.values()) {
                names.addAll(Arrays.asList(target.names));
            }
            return names.toArray(new String[0]);
        }
    }
}