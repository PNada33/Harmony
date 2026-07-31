package xd.harm.bot.team;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Общий межпроцессный канал команды BedWars.
 * v32: все операции с диском выполняются в ФОНОВОМ потоке.
 * Игровой тик читает только готовые значения из памяти,
 * поэтому Scaffold, мост и тайминги блоков не замедляются.
 */
public final class TeamCoordinator {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final long ACTIVE_MS = 7000L;
    private static final long LOOP_MS = 1000L;

    private static volatile boolean enabled;
    private static volatile String teamId = "LionsTempleTeam";
    private static final String botId = safe(System.getProperty("bot.id", System.getProperty("bot.nick", "bot1")));
    private static volatile String requestedRole = System.getProperty("bot.role", "Auto");
    private static final int botIndex = intProperty("bot.index", 9999);
    private static final String roleLayout = System.getProperty("bot.role.layout", "Bridger,Defender,Collector,Fighter");
    private static final Path root = Paths.get(System.getProperty("bot.team.dir", "E:\\Мои Сурсы\\harmony\\team_ai"));

    // Кэш для игрового потока: только чтение памяти, никакого диска.
    private static volatile String cachedRole = "Auto";
    private static volatile boolean cachedCommander = false;
    private static volatile String cachedSummary = "";
    private static final AtomicReference<String> incomingStrategy = new AtomicReference<>(null);
    private static volatile String lastSeenStrategy = null;

    // Данные от игрового потока для фоновой записи.
    private static final AtomicReference<JsonObject> pendingState = new AtomicReference<>(null);
    private static final AtomicReference<String> pendingStrategy = new AtomicReference<>(null);

    private static volatile Thread worker;

    private TeamCoordinator() {}

    public static void configure(boolean on, String team, String role) {
        enabled = on;
        if (team != null && !team.trim().isEmpty()) teamId = safe(team.trim());
        String propRole = System.getProperty("bot.role", "").trim();
        requestedRole = !propRole.isEmpty() ? propRole : (role == null ? "Auto" : role.trim());
        if (on) startWorker();
    }

    /** Мгновенно: только сохраняет снимок в памяти. Запись делает фоновый поток. */
    public static void publishState(String nick, String map, int phase, double x, double y, double z,
            float health, int armor, int iron, int gold, int emerald, int diamond, int blocks, boolean bedKnown) {
        if (!enabled) return;
        JsonObject o = new JsonObject();
        o.addProperty("time", System.currentTimeMillis()); o.addProperty("botId", botId); o.addProperty("nick", nick);
        o.addProperty("launchIndex", botIndex); o.addProperty("teamId", teamId);
        o.addProperty("role", cachedRole); o.addProperty("map", map);
        o.addProperty("phase", phase); o.addProperty("x", x); o.addProperty("y", y); o.addProperty("z", z);
        o.addProperty("health", health); o.addProperty("armor", armor); o.addProperty("iron", iron);
        o.addProperty("gold", gold); o.addProperty("emerald", emerald); o.addProperty("diamond", diamond);
        o.addProperty("blocks", blocks); o.addProperty("bedKnown", bedKnown);
        pendingState.set(o);
    }

    /** Мгновенно: читает кэш. */
    public static boolean isCommander() {
        return enabled && cachedCommander;
    }

    /** Мгновенно: читает кэш. */
    public static String getAssignedRole() {
        if (!requestedRole.isEmpty() && !requestedRole.equalsIgnoreCase("Auto")) return requestedRole;
        return cachedRole;
    }

    /** Мгновенно: читает кэш. */
    public static String buildTeamSummary() {
        return cachedSummary;
    }

    /** Мгновенно: кладёт стратегию в очередь на фоновую запись. */
    public static void publishStrategy(String strategy) {
        if (!enabled || strategy == null || !cachedCommander) return;
        pendingStrategy.set(strategy);
        lastSeenStrategy = strategy;
    }

    /** Мгновенно: забирает новую общую стратегию из кэша (или null). */
    public static String readSharedStrategy() {
        if (!enabled) return null;
        return incomingStrategy.getAndSet(null);
    }

    private static synchronized void startWorker() {
        if (worker != null && worker.isAlive()) return;
        worker = new Thread(TeamCoordinator::workerLoop, "TeamAI-IO");
        worker.setDaemon(true);
        worker.start();
    }

    private static void workerLoop() {
        while (true) {
            try {
                if (enabled) {
                    Files.createDirectories(statesDir());

                    JsonObject state = pendingState.getAndSet(null);
                    if (state != null) writeAtomic(statesDir().resolve(botId + ".json"), GSON.toJson(state));

                    List<JsonObject> members = activeStates();
                    refreshRoleAndCommander(members);
                    refreshSummary(members);

                    String outgoing = pendingStrategy.getAndSet(null);
                    if (outgoing != null && cachedCommander) {
                        JsonObject o = new JsonObject();
                        o.addProperty("time", System.currentTimeMillis());
                        o.addProperty("commander", botId);
                        o.addProperty("strategy", outgoing);
                        writeAtomic(teamDir().resolve("team_strategy.json"), GSON.toJson(o));
                    }

                    readStrategyFile();
                }
            } catch (Exception e) {
                System.out.println("[TeamAI] worker error: " + e.getMessage());
            }
            try { Thread.sleep(LOOP_MS); } catch (InterruptedException e) { return; }
        }
    }

    private static void refreshRoleAndCommander(List<JsonObject> members) {
        boolean selfFound = false;
        String commander = botId;
        for (JsonObject o : members) {
            String id = text(o, "botId", "");
            if (botId.equalsIgnoreCase(id)) selfFound = true;
            if (!id.isEmpty() && id.compareToIgnoreCase(commander) < 0) commander = id;
        }
        cachedCommander = botId.equalsIgnoreCase(commander);
        if (!selfFound) {
            JsonObject self = new JsonObject();
            self.addProperty("botId", botId); self.addProperty("launchIndex", botIndex);
            members.add(self);
        }
        members.sort(Comparator
                .comparingInt((JsonObject o) -> integer(o, "launchIndex", 9999))
                .thenComparing(o -> text(o, "botId", ""), String.CASE_INSENSITIVE_ORDER));

        int position = 0;
        for (int i = 0; i < members.size(); i++)
            if (botId.equalsIgnoreCase(text(members.get(i), "botId", ""))) { position = i; break; }

        String[] r = normalizedRoles();
        int count = members.size();
        String role;
        if (count <= 1) role = join(r[0], r[1], r[2], r[3]);
        else if (count == 2) role = position == 0 ? join(r[0], r[3]) : join(r[1], r[2]);
        else if (count == 3) {
            if (position == 0) role = join(r[0], r[3]);
            else if (position == 1) role = r[1];
            else role = join(r[2], r[3]);
        }
        else if (position < 4) role = r[position];
        else role = r[3];
        cachedRole = role;
    }

    private static void refreshSummary(List<JsonObject> members) {
        StringBuilder s = new StringBuilder("team=").append(teamId).append('\n');
        for (JsonObject o : members) {
            s.append(text(o,"botId","?")).append(" role=").append(text(o,"role","Auto"))
             .append(" phase=").append(text(o,"phase","0")).append(" hp=").append(text(o,"health","0"))
             .append(" iron=").append(text(o,"iron","0")).append(" gold=").append(text(o,"gold","0"))
             .append(" emerald=").append(text(o,"emerald","0")).append(" diamond=").append(text(o,"diamond","0"))
             .append(" blocks=").append(text(o,"blocks","0")).append('\n');
        }
        cachedSummary = s.toString();
    }

    private static void readStrategyFile() {
        try {
            Path p = teamDir().resolve("team_strategy.json");
            if (!Files.exists(p)) return;
            JsonObject o = new JsonParser().parse(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
            if (System.currentTimeMillis() - o.get("time").getAsLong() > 30000L) return;
            String s = text(o, "strategy", "");
            if (s.isEmpty() || s.equals(lastSeenStrategy)) return;
            lastSeenStrategy = s;
            incomingStrategy.set(s);
        } catch (Exception ignored) {}
    }

    private static List<JsonObject> activeStates() {
        List<JsonObject> out = new ArrayList<>(); long now = System.currentTimeMillis();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(statesDir(), "*.json")) {
            for (Path p : ds) try {
                JsonObject o = new JsonParser().parse(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
                if (now - o.get("time").getAsLong() <= ACTIVE_MS) out.add(o);
            } catch (Exception ignored) {}
        } catch (Exception ignored) {}
        return out;
    }

    private static String[] normalizedRoles() {
        String[] defaults = {"Bridger", "Defender", "Collector", "Fighter"};
        String[] input = roleLayout.split(",");
        for (int i = 0; i < 4; i++)
            if (i < input.length && !input[i].trim().isEmpty()) defaults[i] = input[i].trim();
        return defaults;
    }

    private static String join(String... roles) { return String.join("+", roles); }
    private static Path teamDir() { return root.resolve(teamId); }
    private static Path statesDir() { return teamDir().resolve("states"); }
    private static void writeAtomic(Path p, String s) {
        try { Files.createDirectories(p.getParent()); Path t=p.resolveSibling(p.getFileName()+".tmp."+botId);
            Files.writeString(t,s,StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING);
            try{Files.move(t,p,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(Exception e){Files.move(t,p,StandardCopyOption.REPLACE_EXISTING);} }
        catch(Exception e){System.out.println("[TeamAI] write failed: "+e.getMessage());}
    }
    private static String text(JsonObject o,String k,String d){try{return o.has(k)?o.get(k).getAsString():d;}catch(Exception e){return d;}}
    private static int integer(JsonObject o,String k,int d){try{return o.has(k)?o.get(k).getAsInt():d;}catch(Exception e){return d;}}
    private static int intProperty(String k,int d){try{return Integer.parseInt(System.getProperty(k,String.valueOf(d)));}catch(Exception e){return d;}}
    private static String safe(String s){return s==null||s.isEmpty()?"default":s.replaceAll("[^a-zA-Z0-9_\\-а-яА-Я]","_");}
}
