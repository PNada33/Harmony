

package xd.harm.baritone;

import xd.harm.baritone.api.BaritoneAPI;
import xd.harm.baritone.api.IBaritone;
import xd.harm.baritone.api.Settings;
import xd.harm.baritone.api.event.listener.IEventBus;
import xd.harm.baritone.api.utils.Helper;
import xd.harm.baritone.api.utils.IPlayerContext;
import xd.harm.baritone.behavior.*;
import xd.harm.baritone.cache.WorldProvider;
import xd.harm.baritone.command.manager.CommandManager;
import xd.harm.baritone.event.GameEventHandler;
import xd.harm.baritone.process.*;
import xd.harm.baritone.selection.SelectionManager;
import xd.harm.baritone.utils.BlockStateInterface;
import xd.harm.baritone.utils.GuiClick;
import xd.harm.baritone.utils.InputOverrideHandler;
import xd.harm.baritone.utils.PathingControlManager;
import xd.harm.baritone.utils.player.PrimaryPlayerContext;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Brady
 * @since 7/31/2018
 */
public class Baritone implements IBaritone {

    private static ThreadPoolExecutor threadPool;
    private static File dir;

    static {
        threadPool = new ThreadPoolExecutor(4, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<>());

        dir = new File(Minecraft.getInstance().gameDir + "\\harmony\\", "baritone");
        if (!Files.exists(dir.toPath())) {
            try {
                Files.createDirectories(dir.toPath());
            } catch (IOException ignored) {}
        }
    }

    private GameEventHandler gameEventHandler;

    private PathingBehavior pathingBehavior;
    private LookBehavior lookBehavior;
    private InventoryBehavior inventoryBehavior;
    private WaypointBehavior waypointBehavior;
    private InputOverrideHandler inputOverrideHandler;

    private FollowProcess followProcess;
    private MineProcess mineProcess;
    private GetToBlockProcess getToBlockProcess;
    private CustomGoalProcess customGoalProcess;
    private BuilderProcess builderProcess;
    private ExploreProcess exploreProcess;
    private BackfillProcess backfillProcess;
    private FarmProcess farmProcess;

    private PathingControlManager pathingControlManager;
    private SelectionManager selectionManager;
    private CommandManager commandManager;

    private IPlayerContext playerContext;
    private WorldProvider worldProvider;

    public BlockStateInterface bsi;

    Baritone() {
        this.gameEventHandler = new GameEventHandler(this);

        // Define this before behaviors try and get it, or else it will be null and the builds will fail!
        this.playerContext = PrimaryPlayerContext.INSTANCE;

        {
            // the Behavior constructor calls baritone.registerBehavior(this) so this populates the behaviors arraylist
            pathingBehavior = new PathingBehavior(this);
            lookBehavior = new LookBehavior(this);
            inventoryBehavior = new InventoryBehavior(this);
            inputOverrideHandler = new InputOverrideHandler(this);
            waypointBehavior = new WaypointBehavior(this);
        }

        this.pathingControlManager = new PathingControlManager(this);
        {
            this.pathingControlManager.registerProcess(followProcess = new FollowProcess(this));
            this.pathingControlManager.registerProcess(mineProcess = new MineProcess(this));
            this.pathingControlManager.registerProcess(customGoalProcess = new CustomGoalProcess(this)); // very high iq
            this.pathingControlManager.registerProcess(getToBlockProcess = new GetToBlockProcess(this));
            this.pathingControlManager.registerProcess(builderProcess = new BuilderProcess(this));
            this.pathingControlManager.registerProcess(exploreProcess = new ExploreProcess(this));
            this.pathingControlManager.registerProcess(backfillProcess = new BackfillProcess(this));
            this.pathingControlManager.registerProcess(farmProcess = new FarmProcess(this));
        }

        this.worldProvider = new WorldProvider();
        this.selectionManager = new SelectionManager(this);
        this.commandManager = new CommandManager(this);
    }

    @Override
    public PathingControlManager getPathingControlManager() {
        return this.pathingControlManager;
    }

    public void registerBehavior(Behavior behavior) {
        this.gameEventHandler.registerEventListener(behavior);
    }

    @Override
    public InputOverrideHandler getInputOverrideHandler() {
        return this.inputOverrideHandler;
    }

    @Override
    public CustomGoalProcess getCustomGoalProcess() {
        return this.customGoalProcess;
    }

    @Override
    public GetToBlockProcess getGetToBlockProcess() {
        return this.getToBlockProcess;
    }

    @Override
    public IPlayerContext getPlayerContext() {
        return this.playerContext;
    }

    @Override
    public FollowProcess getFollowProcess() {
        return this.followProcess;
    }

    @Override
    public BuilderProcess getBuilderProcess() {
        return this.builderProcess;
    }

    public InventoryBehavior getInventoryBehavior() {
        return this.inventoryBehavior;
    }

    @Override
    public LookBehavior getLookBehavior() {
        return this.lookBehavior;
    }

    public ExploreProcess getExploreProcess() {
        return this.exploreProcess;
    }

    @Override
    public MineProcess getMineProcess() {
        return this.mineProcess;
    }

    public FarmProcess getFarmProcess() {
        return this.farmProcess;
    }

    @Override
    public PathingBehavior getPathingBehavior() {
        return this.pathingBehavior;
    }

    @Override
    public SelectionManager getSelectionManager() {
        return selectionManager;
    }

    @Override
    public WorldProvider getWorldProvider() {
        return this.worldProvider;
    }

    @Override
    public IEventBus getGameEventHandler() {
        return this.gameEventHandler;
    }

    @Override
    public CommandManager getCommandManager() {
        return this.commandManager;
    }

    @Override
    public void openClick() {
        new Thread(() -> {
            try {
                Thread.sleep(100);
                Helper.mc.execute(() -> Helper.mc.displayGuiScreen(new GuiClick()));
            } catch (Exception ignored) {}
        }).start();
    }

    public static Settings settings() {
        return BaritoneAPI.getSettings();
    }

    public static File getDir() {
        return dir;
    }

    public static Executor getExecutor() {
        return threadPool;
    }
}
