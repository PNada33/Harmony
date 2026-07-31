package xd.harm.bot.brain;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * LLM-стратег: раз в несколько секунд отправляет текстовое состояние бота
 * в ЛОКАЛЬНУЮ нейросеть (LM Studio / llama.cpp, OpenAI-совместимый сервер на localhost)
 * и получает команду уровня стратегии. Мелкую моторику (движение, мосты, бой)
 * по-прежнему делает алгоритмика Legit — LLM только «думает».
 *
 * Адрес сервера можно поменять через JVM-флаг: -Dllm.url=http://127.0.0.1:1234/v1/chat/completions
 */
public class LlmStrategist {

    private static final String URL_STR = System.getProperty("llm.url", "http://127.0.0.1:1234/v1/chat/completions");

    private static final String SYSTEM_PROMPT =
            "Ты — стратег бота, играющего в Minecraft BedWars (карта BW-13). "
            + "Тебе дают текущее состояние бота. Выбери лучшую стратегию и ответь СТРОГО ОДНИМ словом из списка: "
            + "BALANCED, RUSH, DEFENSIVE, AGGRESSIVE, AGGRESSIVEMAX. "
            + "Подсказки: мало здоровья или враг близко — DEFENSIVE; всё спокойно и надо копить ресурсы — BALANCED; "
            + "полное здоровье и хочется давить — AGGRESSIVE; нужно срочно на центр за алмазами — RUSH; "
            + "враг почти добит и надо доломать все кровати — AGGRESSIVEMAX. Никаких пояснений, только одно слово.";

    private static final AtomicBoolean busy = new AtomicBoolean(false);
    private static final AtomicReference<String> lastStrategy = new AtomicReference<>(null);
    private static volatile boolean loggedError = false;

    /** Запустить запрос в фоне. Если предыдущий ещё не завершился — пропускаем. */
    public static void requestAsync(final String stateText) {
        if (!busy.compareAndSet(false, true)) return;
        Thread t = new Thread(() -> {
            try {
                String content = ask(stateText);
                if (content != null) {
                    String s = parseStrategy(content);
                    if (s != null) {
                        lastStrategy.set(s);
                        loggedError = false;
                    }
                }
            } catch (Throwable e) {
                if (!loggedError) {
                    loggedError = true;
                    System.out.println("[LLM] нет связи с локальным сервером (" + URL_STR + "): " + e.getMessage()
                            + " — проверь, что LM Studio запущен и сервер включён.");
                }
            } finally {
                busy.set(false);
            }
        }, "LLM-Strategist");
        t.setDaemon(true);
        t.start();
    }

    /** Забрать свежую стратегию (или null). После чтения сбрасывается. */
    public static String consumeStrategy() {
        return lastStrategy.getAndSet(null);
    }

    private static String ask(String stateText) throws Exception {
        String body = "{\"model\":\"local\",\"temperature\":0.2,\"max_tokens\":16,\"messages\":["
                + "{\"role\":\"system\",\"content\":\"" + esc(SYSTEM_PROMPT) + "\"},"
                + "{\"role\":\"user\",\"content\":\"" + esc(stateText) + "\"}]}";

        HttpURLConnection c = (HttpURLConnection) new URL(URL_STR).openConnection();
        c.setConnectTimeout(2000);
        c.setReadTimeout(20000);
        c.setRequestMethod("POST");
        c.setRequestProperty("Content-Type", "application/json");
        c.setDoOutput(true);
        try (OutputStream os = c.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        if (c.getResponseCode() != 200) return null;
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
        }
        return extractContent(sb.toString());
    }

    /** Достаём первое поле \"content\":\"...\" из JSON-ответа (без сторонних библиотек). */
    private static String extractContent(String resp) {
        int i = resp.indexOf("\"content\":\"");
        if (i < 0) return null;
        StringBuilder out = new StringBuilder();
        for (int j = i + 11; j < resp.length(); j++) {
            char ch = resp.charAt(j);
            if (ch == '\\') {
                j++;
                if (j < resp.length()) {
                    char e = resp.charAt(j);
                    if (e == 'n') out.append('\n');
                    else if (e == 't') out.append('\t');
                    else if (e == 'u' && j + 4 < resp.length()) {
                        out.append((char) Integer.parseInt(resp.substring(j + 1, j + 5), 16));
                        j += 4;
                    } else out.append(e);
                }
                continue;
            }
            if (ch == '"') break;
            out.append(ch);
        }
        return out.toString();
    }

    /** Превращаем ответ LLM в название стратегии BotAttack. */
    private static String parseStrategy(String content) {
        String u = content.toUpperCase();
        if (u.contains("AGGRESSIVEMAX") || u.contains("AGGRESSIVE MAX")) return "AggressiveMax";
        if (u.contains("AGGRESSIVE")) return "Aggressive";
        if (u.contains("RUSH")) return "Rush Mid";
        if (u.contains("DEFEN")) return "Defensive";
        if (u.contains("BALANC")) return "Balanced";
        return null;
    }

    private static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}
