package xd.harm.modules.impl.misc;

import net.minecraft.client.Minecraft;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.regex.*;

public final class LocalLanPortResolver {
    private static final Pattern SERVING = Pattern.compile("Started serving on\\s+(\\d{1,5})", Pattern.CASE_INSENSITIVE);
    private LocalLanPortResolver() {}

    public static int resolve(Minecraft minecraft) {
        try {
            if (minecraft == null || minecraft.getIntegratedServer() == null) return -1;
            File log = new File("logs", "latest.log");
            if (!log.isFile()) return -1;
            long length = log.length(), start = Math.max(0, length - 1_500_000);
            byte[] data = new byte[(int)(length - start)];
            try (RandomAccessFile file = new RandomAccessFile(log, "r")) { file.seek(start); file.readFully(data); }
            String text = new String(data, StandardCharsets.UTF_8);
            int worldStart = Math.max(text.lastIndexOf("Starting integrated minecraft server"), text.lastIndexOf("Preparing start region"));
            Matcher matcher = SERVING.matcher(text); int port = -1, position = -1;
            while (matcher.find()) { port = Integer.parseInt(matcher.group(1)); position = matcher.start(); }
            return port > 0 && port <= 65535 && position > worldStart ? port : -1;
        } catch (Exception ignored) { return -1; }
    }
}
