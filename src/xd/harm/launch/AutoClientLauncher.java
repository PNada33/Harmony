package xd.harm.launch;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Auto-detecting Minecraft client launcher for IntelliJ.
 * Tries common entrypoints for MCP/Forge/Fabric stacks and runs the first available.
 * Produces clear logs about which main class was used or why detection failed.
 */
public final class AutoClientLauncher {
    private static final List<String> CANDIDATE_MAIN_CLASSES = Arrays.asList(
        // Vanilla / Forge MCP-era
        "net.minecraft.client.main.Main",
        // Modern Forge bootstrap (might require additional module args, but try)
        "cpw.mods.bootstraplauncher.BootstrapLauncher",
        // Fabric dev launcher
        "net.fabricmc.devlaunchinjector.Main",
        // Fabric loader direct knot client
        "net.fabricmc.loader.impl.launch.knot.KnotClient"
    );

    public static void main(String[] args) {
        System.out.println("=== Harmony AutoClientLauncher: autodetecting Minecraft entrypoint ===");

        List<String> baseArgs = new ArrayList<>(Arrays.asList(
            "--version", "mcp",
            "--accessToken", "0",
            "--assetsDir", System.getenv().getOrDefault("assetDirectory", "assets"),
            "--assetIndex", "1.16",
            "--userProperties", "{}"
        ));

        // Optional window size via JVM props -Dbot.width/-Dbot.height
        String w = System.getProperty("bot.width");
        String h = System.getProperty("bot.height");
        if (w != null && h != null) {
            baseArgs.add("--width");
            baseArgs.add(w);
            baseArgs.add("--height");
            baseArgs.add(h);
        }

        String[] fullArgs = concat(baseArgs.toArray(new String[0]), args);

        for (String mainClassName : CANDIDATE_MAIN_CLASSES) {
            try {
                Class<?> mainClass = Class.forName(mainClassName);
                Method main = mainClass.getMethod("main", String[].class);
                System.out.println("→ Trying: " + mainClassName);
                main.invoke(null, (Object) fullArgs);
                System.out.println("=== Launched via: " + mainClassName + " ===");
                return;
            } catch (ClassNotFoundException cnf) {
                System.out.println("✗ Not found: " + mainClassName);
            } catch (Throwable t) {
                System.out.println("! Failed to launch via: " + mainClassName);
                t.printStackTrace(System.out);
                return; // Stop on first found-but-failed to avoid cascading errors
            }
        }

        System.out.println("=== No known Minecraft entrypoint found on classpath. ===");
        System.out.println("Checked: " + CANDIDATE_MAIN_CLASSES);
        System.out.println("Hints:");
        System.out.println("- If you're on Forge 1.16.5 MCP, ensure net.minecraft.client.main.Main is on the classpath.");
        System.out.println("- If you're on modern Forge, cpw.mods.bootstraplauncher.BootstrapLauncher may need additional module args.");
        System.out.println("- If you're on Fabric, ensure devlaunchinjector or KnotClient is present in libraries.");
    }

    private static <T> T[] concat(T[] first, T[] second) {
        T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
}
