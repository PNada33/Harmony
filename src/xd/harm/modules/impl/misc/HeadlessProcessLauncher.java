package xd.harm.modules.impl.misc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class HeadlessProcessLauncher {
    private static final String DEFAULT_DIR = "E:\\Мои Сурсы\\HarmonyHeadless";
    private volatile String message = "Готов к запуску";
    private volatile Path logFile = Paths.get(DEFAULT_DIR, "embedded-console.log");

    public String message() { return message; }

    public boolean start(String nickname,String serverAddress){return start(nickname,serverAddress,0,0);}
    public boolean start(String nickname,String serverAddress,long connectDelayMs,long followRefreshMs) {
        String name = nickname == null ? "" : nickname.trim();
        if (!name.matches("[A-Za-z0-9_]{3,16}")) {
            message = "Ник: 3–16 символов A-Z, 0-9 или _";
            return false;
        }
        String address = serverAddress == null ? "" : serverAddress.trim();
        int split = address.lastIndexOf(':');
        if (split <= 0 || split == address.length() - 1) {
            message = "Сервер укажи как IP:PORT";
            return false;
        }
        String host = address.substring(0, split).trim();
        String portText = address.substring(split + 1).trim();
        int port;
        try { port = Integer.parseInt(portText); } catch (NumberFormatException error) { port = -1; }
        if (!host.matches("[A-Za-z0-9._-]{1,253}") || port < 1 || port > 65535) {
            message = "Некорректный IP или порт";
            return false;
        }
        if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win")) {
            message = "Встроенный запуск доступен только в Windows";
            return false;
        }
        Path dir = findDirectory();
        Path packageJson = dir.resolve("package.json");
        if (!Files.isRegularFile(packageJson)) {
            message = "Не найден HarmonyHeadless: " + dir;
            return false;
        }
        logFile = dir.resolve("embedded-console.log");
        try {
            Files.deleteIfExists(logFile);
            String psDir = powerShellQuote(dir.toAbsolutePath().toString());
            String psLog = powerShellQuote(logFile.toAbsolutePath().toString());
            String psName = powerShellQuote(name);
            String psHost = powerShellQuote(host);
            String psPort = powerShellQuote(String.valueOf(port));
            String psAddress = powerShellQuote(host + ":" + port);
            String script =
                    "$env:HARMONY_BOT_NAME='" + psName + "';" +
                    "$env:HARMONY_SERVER_HOST='" + psHost + "';" +
                    "$env:HARMONY_SERVER_PORT='" + psPort + "';" +
                    "Set-Location -LiteralPath '" + psDir + "';" +
                    "'[BotController] запуск: " + psName + " -> " + psAddress + "' | Out-File -FilePath '" + psLog + "' -Encoding utf8;" +
                    "& node.exe 'src/index.js' '--harmony-bot-name' '" + psName + "' '--harmony-server' '" + psAddress + "' '--harmony-connect-delay' '" + Math.max(0,connectDelayMs) + "' '--harmony-follow-refresh' '" + Math.max(0,followRefreshMs) + "' 2>&1 | ForEach-Object { " +
                    "$_ | Out-File -FilePath '" + psLog + "' -Append -Encoding utf8 }";
            ProcessBuilder builder = new ProcessBuilder(
                    "powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass",
                    "-WindowStyle", "Hidden", "-Command", script
            );
            builder.directory(dir.toFile());
            builder.redirectErrorStream(true);
            builder.start();
            message = "Headless скрыто запущен: " + name + " → " + host + ":" + port;
            return true;
        } catch (IOException error) {
            message = "Ошибка запуска: " + error.getMessage();
            return false;
        }
    }

    public void clearConsole(){try{Files.write(logFile,new byte[0]);}catch(Exception ignored){}}

    public List<String> consoleLines() {
        Path file = logFile;
        if (file == null || !Files.isRegularFile(file)) return Collections.emptyList();
        try {
            List<String> all = Files.readAllLines(file, StandardCharsets.UTF_8);
            int from = Math.max(0, all.size() - 180);
            List<String> result = new ArrayList<>();
            for (int i = from; i < all.size(); i++) {
                String line = all.get(i);
                if (!line.isEmpty() && line.charAt(0) == '\uFEFF') line = line.substring(1);
                result.add(line);
            }
            return result;
        } catch (IOException ignored) {
            return Collections.emptyList();
        }
    }

    private String powerShellQuote(String value) { return value.replace("'", "''"); }

    private Path findDirectory() {
        String property = System.getProperty("harmony.headless.dir", "").trim();
        if (!property.isEmpty() && Files.isDirectory(Paths.get(property))) return Paths.get(property);
        String env = System.getenv("HARMONY_HEADLESS_DIR");
        if (env != null && !env.trim().isEmpty() && Files.isDirectory(Paths.get(env.trim()))) return Paths.get(env.trim());
        Path nearby = Paths.get(System.getProperty("user.dir", "."), "HarmonyHeadless");
        if (Files.isDirectory(nearby)) return nearby;
        return Paths.get(DEFAULT_DIR);
    }
}
