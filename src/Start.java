import net.minecraft.client.main.Main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class Start
{
    public static void main(String[] args)
    {
        List<String> baseArgs = new ArrayList<>(Arrays.asList(
                "--version", "mcp",
                "--accessToken", "0",
                "--assetsDir", System.getenv().containsKey("assetDirectory") ? System.getenv("assetDirectory") : "assets",
                "--assetIndex", "1.16",
                "--userProperties", "{}"
        ));

        if (System.getProperty("bot.mode") != null) {
            String w = System.getProperty("bot.width", "400");
            String h = System.getProperty("bot.height", "300");
            baseArgs.add("--width");
            baseArgs.add(w);
            baseArgs.add("--height");
            baseArgs.add(h);
        }

        Main.main(concat(baseArgs.toArray(new String[0]), args));
    }

    public static <T> T[] concat(T[] first, T[] second)
    {
        T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
}
