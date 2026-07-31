

package xd.harm.baritone.cache;

import xd.harm.baritone.Baritone;
import xd.harm.baritone.api.cache.IWorldProvider;
import xd.harm.baritone.api.utils.Helper;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.storage.FolderName;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author Brady
 * @since 8/4/2018
 */
public class WorldProvider implements IWorldProvider, Helper {

    private static final Map<Path, WorldData> worldCache = new HashMap<>();

    private WorldData currentWorld;
    private World mcWorld;

    @Override
    public final WorldData getCurrentWorld() {
        detectAndHandleBrokenLoading();
        return this.currentWorld;
    }

    /**
     * Called when a new world is initialized to discover the
     *
     * @param world The world's Registry Data
     */
    public final void initWorld(RegistryKey<World> world) {
        File directory;
        File readme;

        IntegratedServer integratedServer = mc.getIntegratedServer();


        if (mc.isSingleplayer()) {
            directory = DimensionType.getDimensionFolder(world, integratedServer.func_240776_a_(FolderName.DOT).toFile());


            if (directory.toPath().relativize(mc.gameDir.toPath()).getNameCount() != 2) {
                directory = directory.getParentFile();
            }

            directory = new File(directory, "baritone");
            readme = directory;
        } else {
            String folderName;
            if (mc.getCurrentServerData() != null) {
                folderName = mc.isConnectedToRealms() ? "realms" : mc.getCurrentServerData().serverIP;
            } else {

                System.out.println("World seems to be a replay. Not loading Baritone cache.");
                currentWorld = null;
                mcWorld = mc.world;
                return;
            }
            if (SystemUtils.IS_OS_WINDOWS) {
                folderName = folderName.replace(":", "_");
            }
            directory = new File(Baritone.getDir(), folderName);
            readme = Baritone.getDir();
        }



        Path dir = DimensionType.getDimensionFolder(world, directory).toPath();
        if (!Files.exists(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (IOException ignored) {}
        }

        synchronized (worldCache) {
            this.currentWorld = worldCache.computeIfAbsent(dir, d -> new WorldData(d, world));
        }
        this.mcWorld = mc.world;
    }

    public final void closeWorld() {
        WorldData world = this.currentWorld;
        this.currentWorld = null;
        this.mcWorld = null;
        if (world == null) {
            return;
        }
        world.onClose();
    }

    public final void ifWorldLoaded(Consumer<WorldData> currentWorldConsumer) {
        detectAndHandleBrokenLoading();
        if (this.currentWorld != null) {
            currentWorldConsumer.accept(this.currentWorld);
        }
    }

    private final void detectAndHandleBrokenLoading() {
        if (this.mcWorld != mc.world) {
            if (this.currentWorld != null) {
                System.out.println("mc.world unloaded unnoticed! Unloading Baritone cache now.");
                closeWorld();
            }
            if (mc.world != null) {
                System.out.println("mc.world loaded unnoticed! Loading Baritone cache now.");
                initWorld(mc.world.getDimensionKey());
            }
        } else if (currentWorld == null && mc.world != null && (mc.isSingleplayer() || mc.getCurrentServerData() != null)) {
            System.out.println("Retrying to load Baritone cache");
            initWorld(mc.world.getDimensionKey());
        }
    }
}
