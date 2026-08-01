package xd.harm.utils.figura;

import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Harmony 1.16.5 (Java 8) port of the RichClient Figura avatar installer.
 *
 * Раскладывает содержимое ресурса figura_avatars (или папки figura_assets
 * рядом с игрой) в <gameDir>/figura/avatars.
 *
 * Никаких зависимостей от остального Harmony — только Minecraft.getInstance().
 */
public final class FiguraAvatarInstaller {

    /** Имя папки-ресурса внутри classpath / jar. */
    private static final String RESOURCE_ROOT = "figura_avatars";

    /** Запасные папки рядом с .minecraft, если ресурса в classpath нет. */
    private static final String[] FALLBACK_DIRS = {"figura_assets", "figura_avatars"};

    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);

    private static final AtomicInteger INSTALLED_FILES = new AtomicInteger();
    private static final AtomicInteger SKIPPED_FILES = new AtomicInteger();
    private static final AtomicInteger REPLACED_FILES = new AtomicInteger();

    private static volatile boolean finished;
    private static volatile Throwable lastError;
    private static volatile String status = "";

    private FiguraAvatarInstaller() {
    }

    // ------------------------------------------------------------------ API

    public static void installAsync() {
        if (!RUNNING.compareAndSet(false, true)) {
            return;
        }

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    prepareCounters();
                    installNow();
                    finished = true;
                    lastError = null;
                    status = "Готово: " + INSTALLED_FILES.get() + " новых, "
                            + REPLACED_FILES.get() + " обновлено, "
                            + SKIPPED_FILES.get() + " пропущено";
                } catch (Throwable t) {
                    finished = false;
                    lastError = t;
                    status = "Ошибка установки: " + t;
                } finally {
                    RUNNING.set(false);
                }
            }
        }, "Harmony-Figura-Avatar-Installer");

        thread.setDaemon(true);
        thread.start();
    }

    public static void installBlocking() throws Exception {
        while (RUNNING.get()) {
            Thread.sleep(10L);
        }
        if (!RUNNING.compareAndSet(false, true)) {
            return;
        }
        try {
            prepareCounters();
            installNow();
            finished = true;
            lastError = null;
        } catch (Throwable t) {
            finished = false;
            lastError = t;
            if (t instanceof Exception) {
                throw (Exception) t;
            }
            if (t instanceof Error) {
                throw (Error) t;
            }
            throw new Exception(t);
        } finally {
            RUNNING.set(false);
        }
    }

    public static boolean isRunning() {
        return RUNNING.get();
    }

    public static boolean isFinished() {
        return finished;
    }

    public static int getInstalledFiles() {
        return INSTALLED_FILES.get();
    }

    public static int getSkippedFiles() {
        return SKIPPED_FILES.get();
    }

    public static int getReplacedFiles() {
        return REPLACED_FILES.get();
    }

    public static Throwable getLastError() {
        return lastError;
    }

    public static String getStatus() {
        return status;
    }

    /** <gameDir>/figura/avatars */
    public static Path avatarsDir() {
        Minecraft mc = Minecraft.getInstance();
        File gameDir = mc != null && mc.gameDir != null ? mc.gameDir : new File(".");
        return gameDir.toPath().resolve("figura").resolve("avatars").normalize();
    }

    public static boolean isInstalled(String folder) {
        if (folder == null || folder.isEmpty()) {
            return false;
        }
        Path dir = avatarsDir().resolve(folder);
        return Files.isDirectory(dir);
    }

    /**
     * Сносит все установленные аватары. Нужно при переезде со старого набора
     * Obb_* на новый набор из figura_assets.
     */
    public static int wipeInstalled() {
        Path root = avatarsDir();
        if (!Files.isDirectory(root)) {
            return 0;
        }
        int removed = 0;
        try {
            java.util.List<Path> children = new java.util.ArrayList<Path>();
            java.util.stream.Stream<Path> stream = Files.list(root);
            try {
                java.util.Iterator<Path> it = stream.iterator();
                while (it.hasNext()) {
                    children.add(it.next());
                }
            } finally {
                stream.close();
            }
            for (Path child : children) {
                if (deleteRecursively(child)) {
                    removed++;
                }
            }
        } catch (IOException ignored) {
        }
        return removed;
    }

    // -------------------------------------------------------------- Установка

    private static void prepareCounters() {
        finished = false;
        INSTALLED_FILES.set(0);
        SKIPPED_FILES.set(0);
        REPLACED_FILES.set(0);
        lastError = null;
        status = "Установка аватаров...";
    }

    private static void installNow() throws Exception {
        Path avatarsDir = avatarsDir();
        Files.createDirectories(avatarsDir);

        ClassLoader loader = FiguraAvatarInstaller.class.getClassLoader();
        Enumeration<URL> roots = loader.getResources(RESOURCE_ROOT);

        boolean found = false;
        while (roots.hasMoreElements()) {
            URL root = roots.nextElement();
            found = true;
            copyResourceRoot(root, avatarsDir);
        }

        if (!found) {
            found = copyFromCodeSource(avatarsDir);
        }

        if (!found) {
            copyFromGameDirFallback(avatarsDir);
        }
    }

    private static void copyResourceRoot(URL root, Path avatarsDir) throws Exception {
        String protocol = root.getProtocol();

        if ("file".equalsIgnoreCase(protocol)) {
            Path rootPath = Paths.get(root.toURI()).normalize();
            copyDirectory(rootPath, avatarsDir);
            return;
        }

        if ("jar".equalsIgnoreCase(protocol)) {
            JarURLConnection connection = (JarURLConnection) root.openConnection();
            String entryName = connection.getEntryName();

            if (entryName == null || entryName.isEmpty()) {
                entryName = RESOURCE_ROOT;
            }

            JarFile jar = connection.getJarFile();
            try {
                copyFromJar(jar, entryName, avatarsDir);
            } finally {
                // JarURLConnection может кэшировать jar, поэтому не закрываем
                // его принудительно, если он общий для classpath.
                if (!connection.getUseCaches()) {
                    jar.close();
                }
            }
        }
    }

    private static boolean copyFromCodeSource(Path avatarsDir) throws Exception {
        URL location = FiguraAvatarInstaller.class.getProtectionDomain()
                .getCodeSource() != null
                ? FiguraAvatarInstaller.class.getProtectionDomain().getCodeSource().getLocation()
                : null;
        if (location == null) {
            return false;
        }

        URI uri = location.toURI();
        Path path;
        try {
            path = Paths.get(uri).normalize();
        } catch (Exception e) {
            return false;
        }

        if (Files.isDirectory(path)) {
            Path root = path.resolve(RESOURCE_ROOT).normalize();
            if (Files.isDirectory(root)) {
                copyDirectory(root, avatarsDir);
                return true;
            }
            return false;
        }

        if (Files.isRegularFile(path)) {
            JarFile jar = new JarFile(path.toFile());
            try {
                copyFromJar(jar, RESOURCE_ROOT, avatarsDir);
            } finally {
                jar.close();
            }
            return true;
        }

        return false;
    }

    /**
     * Запасной путь для запуска из IntelliJ: папка figura_assets/figura_avatars
     * лежит просто в корне игры или в корне проекта.
     */
    private static void copyFromGameDirFallback(Path avatarsDir) throws IOException {
        Minecraft mc = Minecraft.getInstance();
        File gameDir = mc != null && mc.gameDir != null ? mc.gameDir : new File(".");

        Path[] bases = {
                gameDir.toPath().normalize(),
                gameDir.toPath().getParent() == null ? gameDir.toPath() : gameDir.toPath().getParent(),
                Paths.get("").toAbsolutePath().normalize()
        };

        for (Path base : bases) {
            if (base == null) {
                continue;
            }
            for (String name : FALLBACK_DIRS) {
                Path candidate = base.resolve(name).normalize();
                if (Files.isDirectory(candidate) && !candidate.equals(avatarsDir)) {
                    copyDirectory(candidate, avatarsDir);
                    return;
                }
            }
        }
    }

    private static void copyFromJar(JarFile jar, String entryRoot, Path avatarsDir) throws IOException {
        String prefix = entryRoot.endsWith("/") ? entryRoot : entryRoot + "/";

        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();

            if (!name.startsWith(prefix) || entry.isDirectory()) {
                continue;
            }

            String relative = name.substring(prefix.length());
            if (relative.isEmpty()) {
                continue;
            }

            Path target = avatarsDir.resolve(relative).normalize();
            if (!target.startsWith(avatarsDir)) {
                continue;
            }

            Files.createDirectories(target.getParent());

            boolean existed = Files.exists(target);
            if (existed && sameSize(target, entry.getSize())) {
                SKIPPED_FILES.incrementAndGet();
                continue;
            }

            InputStream in = jar.getInputStream(entry);
            try {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                in.close();
            }

            if (existed) {
                REPLACED_FILES.incrementAndGet();
            } else {
                INSTALLED_FILES.incrementAndGet();
            }
        }
    }

    private static void copyDirectory(final Path source, final Path avatarsDir) throws IOException {
        if (!Files.isDirectory(source)) {
            return;
        }

        java.util.Deque<Path> stack = new java.util.ArrayDeque<Path>();
        stack.push(source);

        while (!stack.isEmpty()) {
            Path dir = stack.pop();
            java.util.stream.Stream<Path> stream = Files.list(dir);
            try {
                java.util.Iterator<Path> it = stream.iterator();
                while (it.hasNext()) {
                    Path child = it.next();
                    if (Files.isDirectory(child)) {
                        stack.push(child);
                        continue;
                    }

                    Path relative = source.relativize(child);
                    Path target = avatarsDir.resolve(relative.toString()).normalize();
                    if (!target.startsWith(avatarsDir)) {
                        continue;
                    }

                    Files.createDirectories(target.getParent());

                    boolean existed = Files.exists(target);
                    if (existed && sameSize(target, Files.size(child))) {
                        SKIPPED_FILES.incrementAndGet();
                        continue;
                    }

                    Files.copy(child, target, StandardCopyOption.REPLACE_EXISTING);

                    if (existed) {
                        REPLACED_FILES.incrementAndGet();
                    } else {
                        INSTALLED_FILES.incrementAndGet();
                    }
                }
            } finally {
                stream.close();
            }
        }
    }

    private static boolean sameSize(Path target, long size) {
        if (size < 0L) {
            return false;
        }
        try {
            return Files.size(target) == size;
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean deleteRecursively(Path path) {
        try {
            if (Files.isDirectory(path)) {
                java.util.List<Path> children = new java.util.ArrayList<Path>();
                java.util.stream.Stream<Path> stream = Files.list(path);
                try {
                    java.util.Iterator<Path> it = stream.iterator();
                    while (it.hasNext()) {
                        children.add(it.next());
                    }
                } finally {
                    stream.close();
                }
                for (Path child : children) {
                    deleteRecursively(child);
                }
            }
            Files.deleteIfExists(path);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
