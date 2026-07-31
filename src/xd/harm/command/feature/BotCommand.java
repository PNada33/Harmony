package xd.harm.command.feature;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerAddress;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import xd.harm.bot.BotSession;
import xd.harm.bot.BotSessionManager;
import xd.harm.command.api.CommandException;
import xd.harm.command.interfaces.Command;
import xd.harm.command.interfaces.Logger;
import xd.harm.command.interfaces.MultiNamedCommand;
import xd.harm.command.interfaces.Parameters;
import xd.harm.command.interfaces.Prefix;
import xd.harm.config.Config;
import xd.harm.config.ConfigStorage;
import xd.harm.Harmony;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BotCommand implements Command, MultiNamedCommand {
    Prefix prefix;
    Logger logger;

    private static final Path CONTROL_FILE = Paths.get("E:\\Мои Сурсы\\harmony\\bot_control.txt");
    private static final Path MODULE_CONFIG_FILE = Paths.get("E:\\Мои Сурсы\\harmony\\bot_module_config.json");
    private static final Path BOT_CONFIGS_DIR = Paths.get("E:\\Мои Сурсы\\harmony\\bot_configs");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public void execute(Parameters parameters) {
        String sub = parameters.asString(0).orElse("");
        BotSessionManager manager = BotSessionManager.getInstance();

        switch (sub.toLowerCase()) {
            case "connect":
                connect(parameters, manager);
                break;
            case "control":
                control(parameters, manager);
                break;
            case "return":
                returnToMain(manager);
                break;
            case "disconnect":
                disconnect(parameters, manager);
                break;
            case "list":
                list(manager);
                break;
            case "settings":
                settings(parameters);
                break;
            case "cfg":
            case "config":
                cfg(parameters);
                break;
            default:
                help();
                break;
        }
    }

    private void connect(Parameters parameters, BotSessionManager manager) {
        String nick = parameters.asString(1).orElseThrow(() -> new CommandException(usageLine("connect <nick> <host[:port]> [port]")));
        String hostRaw = parameters.asString(2).orElseThrow(() -> new CommandException(usageLine("connect <nick> <host[:port]> [port]")));

        ServerAddress parsed = ServerAddress.fromString(hostRaw);
        String host = parsed != null ? parsed.getIP() : hostRaw;
        int port = parsed != null ? parsed.getPort() : 25565;
        Integer overridePort = parameters.asInt(3).orElse(null);
        if (overridePort != null) {
            port = overridePort;
        }

        String error = manager.connectBot(nick, host, port);
        if (error != null) {
            throw new CommandException(TextFormatting.RED + error);
        }

        logger.log(TextFormatting.GREEN + "Присоединяюсь к сессие  " + TextFormatting.WHITE + nick + TextFormatting.GRAY + " -> " + host + ":" + port);
    }

    private void control(Parameters parameters, BotSessionManager manager) {
        String nick = parameters.asString(1).orElseThrow(() -> new CommandException(usageLine("control <nick>")));
        String error = manager.controlBot(nick);
        if (error != null) {
            throw new CommandException(TextFormatting.RED + error);
        }

        logger.log(TextFormatting.GREEN + "Переключение для полного контроля сессией " + TextFormatting.WHITE + nick);
        logger.log(TextFormatting.GRAY + "Использование " + TextFormatting.WHITE + prefix.get() + "bot return" + TextFormatting.GRAY + " чтобы вернутся в основную сессию.");
    }

    private void returnToMain(BotSessionManager manager) {
        String error = manager.rerunMainSession();
        if (error != null) {
            throw new CommandException(TextFormatting.RED + error);
        }
        logger.log(TextFormatting.GREEN + "Вырнул в основную сессию");
    }

    private void disconnect(Parameters parameters, BotSessionManager manager) {
        String target = parameters.asString(1).orElse(null);
        String error = manager.disconnectBot(target);
        if (error != null) {
            throw new CommandException(TextFormatting.RED + error);
        }
        logger.log(TextFormatting.GREEN + "Сессия сэбалась");
    }

    private void list(BotSessionManager manager) {
        List<BotSession> sessions = manager.snapshotSessions().stream()
                .sorted(Comparator.comparing(BotSession::getNick, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());

        if (manager.isForegroundMode()) {
        }

        if (sessions.isEmpty()) {
            logger.log(TextFormatting.GRAY + "Пусто нах");
            return;
        }

        logger.log(TextFormatting.WHITE + "Сесси: " + sessions.size());
        for (BotSession session : sessions) {
            logger.log(TextFormatting.DARK_GRAY + " - " + TextFormatting.WHITE + session.getNick()
                    + TextFormatting.GRAY + " [" + session.getState() + "] "
                    + TextFormatting.DARK_GRAY + session.getHost() + ":" + session.getPort()
                    + TextFormatting.GRAY + " :: " + session.getStatus());
        }
    }

    private void settings(Parameters parameters) {
        String settingKey = parameters.asString(1).orElse(null);

        if (settingKey == null) {
            logger.log(TextFormatting.GRAY + "Текущие настройки ботов читай в bot_control.txt");
            logger.log(usageLine("settings <key> <value>"));
            logger.log(TextFormatting.GRAY + "Пример: " + prefix.get() + "bot settings HitAura Range 4.5");
            logger.log(TextFormatting.GRAY + "Пример: " + prefix.get() + "bot settings HitAura enable");
            logger.log(TextFormatting.GRAY + "Пример: " + prefix.get() + "bot settings Sprint disable");
            return;
        }

        String key;
        String value;

        switch (settingKey.toLowerCase()) {
            case "hitaura":
            case "hutaura":
                String sub = parameters.asString(2).orElse("");
                if (sub.equalsIgnoreCase("range")) {
                    value = parameters.asString(3).orElseThrow(() -> new CommandException("Укажи число для Range"));
                    key = "attackRange";
                } else {
                    value = sub.equalsIgnoreCase("enable") || sub.equalsIgnoreCase("true") ? "true" : "false";
                    key = "attack";
                }
                break;
            case "sprint":
                value = parameters.asString(2).orElse("false");
                value = value.equalsIgnoreCase("enable") || value.equalsIgnoreCase("true") ? "true" : "false";
                key = "sprint";
                break;
            case "scaffold":
                value = parameters.asString(2).orElse("false");
                value = value.equalsIgnoreCase("enable") || value.equalsIgnoreCase("true") ? "true" : "false";
                key = "scaffold";
                break;
            case "velocity":
                value = parameters.asString(2).orElse("false");
                value = value.equalsIgnoreCase("enable") || value.equalsIgnoreCase("true") ? "true" : "false";
                key = "velocity";
                break;
            case "autoskin":
                value = parameters.asString(2).orElse("false");
                value = value.equalsIgnoreCase("enable") || value.equalsIgnoreCase("true") ? "true" : "false";
                key = "autoSkin";
                break;
            case "autoregister":
                value = parameters.asString(2).orElse("false");
                value = value.equalsIgnoreCase("enable") || value.equalsIgnoreCase("true") ? "true" : "false";
                key = "autoRegister";
                break;
            case "skinname":
                value = parameters.asString(2).orElseThrow(() -> new CommandException("Укажи имя скина, например Notch"));
                key = "skinName";
                break;
            case "registerpassword":
                value = parameters.asString(2).orElseThrow(() -> new CommandException("Укажи пароль для регистрации"));
                key = "registerPassword";
                break;
            case "follow":
                value = parameters.asString(2).orElseThrow(() -> new CommandException("Укажи имя игрока для follow, например PlayerName:2.5"));
                key = "follow";
                break;
            default:
                value = parameters.asString(2).orElse("");
                key = settingKey.toLowerCase();
                break;
        }

        writeSetting(key, value);
        persistToModuleConfig(key, value);
        logger.log(TextFormatting.GREEN + "Боты: " + TextFormatting.WHITE + key + TextFormatting.GRAY + " = " + TextFormatting.WHITE + value);
    }

    private void writeSetting(String key, String value) {
        try {
            List<String> lines = new ArrayList<>();
            if (Files.exists(CONTROL_FILE)) {
                lines = Files.readAllLines(CONTROL_FILE);
            }

            boolean found = false;
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.startsWith(key + "=")) {
                    lines.set(i, key + "=" + value);
                    found = true;
                    break;
                }
            }
            if (!found) {
                lines.add(key + "=" + value);
            }

            Files.write(CONTROL_FILE, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new CommandException(TextFormatting.RED + "Ошибка записи bot_control.txt: " + e.getMessage());
        }
    }

    private void help() {
        logger.log(TextFormatting.WHITE + "Команды для использование бота:");
        logger.log(TextFormatting.GRAY + prefix.get() + "bot connect <nick> <host[:port]> [port]");
        logger.log(TextFormatting.GRAY + prefix.get() + "bot control <nick>");
        logger.log(TextFormatting.GRAY + prefix.get() + "bot return");
        logger.log(TextFormatting.GRAY + prefix.get() + "bot disconnect [nick|all]");
        logger.log(TextFormatting.GRAY + prefix.get() + "bot list");
        logger.log(TextFormatting.GRAY + prefix.get() + "bot settings <key> <value>");
        logger.log(TextFormatting.GRAY + prefix.get() + "bot settings HitAura Range <N>");
        logger.log(TextFormatting.GRAY + prefix.get() + "bot settings HitAura enable/disable");
        logger.log(TextFormatting.GRAY + prefix.get() + "bot settings Sprint enable/disable");
        logger.log(TextFormatting.GRAY + prefix.get() + "bot settings Scaffold enable/disable");
        logger.log(TextFormatting.GRAY + prefix.get() + "bot settings Velocity enable/disable");
        logger.log(TextFormatting.GRAY + prefix.get() + "bot settings AutoSkin enable/disable");
        logger.log(TextFormatting.GRAY + prefix.get() + "bot settings AutoRegister enable/disable");
        logger.log(TextFormatting.GRAY + prefix.get() + "bot settings SkinName <name>");
        logger.log(TextFormatting.GRAY + prefix.get() + "bot settings RegisterPassword <pass>");
        logger.log(TextFormatting.WHITE + "Управление конфигами ботов:");
        logger.log(TextFormatting.GRAY + prefix.get() + "bot cfg load <name>");
        logger.log(TextFormatting.GRAY + prefix.get() + "bot cfg list");
        logger.log(TextFormatting.GRAY + prefix.get() + "bot cfg folder");
    }

    private String usageLine(String usage) {
        return TextFormatting.RED + "Usage: " + TextFormatting.WHITE + prefix.get() + "bot " + usage;
    }

    @Override
    public String name() {
        return "bot";
    }

    @Override
    public String description() {
        return "Перемещение между сессиями (БЕТА)";
    }

    @Override
    public List<String> aliases() {
        return List.of("bots");
    }

    private void cfg(Parameters parameters) {
        String action = parameters.asString(1).orElseThrow(() -> new CommandException(
            TextFormatting.RED + "Usage: " + TextFormatting.WHITE + prefix.get() + "bot cfg <load|list> [name]"));

        switch (action.toLowerCase()) {
            case "load":
                cfgLoad(parameters);
                break;
            case "list":
                cfgList();
                break;
            case "folder":
                openBotConfigsFolder();
                break;
            default:
                throw new CommandException(TextFormatting.RED + "Unknown action: " + action + ". Valid: load, list, folder");
        }
    }

    private void cfgSave(Parameters parameters) {
        String name = parameters.asString(2).orElseThrow(() -> new CommandException(
            usageLine("cfg save <name>")));
        try {
            Files.createDirectories(BOT_CONFIGS_DIR);
            Path src = MODULE_CONFIG_FILE;
            Path dst = BOT_CONFIGS_DIR.resolve(name + ".json");
            Files.copy(src, dst, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            logger.log(TextFormatting.GREEN + "Config saved as " + TextFormatting.WHITE + name);
        } catch (IOException e) {
            throw new CommandException(TextFormatting.RED + "Failed to save config: " + e.getMessage());
        }
    }

    private void cfgLoad(Parameters parameters) {
        String name = parameters.asString(2).orElseThrow(() -> new CommandException(
            usageLine("cfg load <name>")));
        String err = applyBotConfig(name);
        if (err != null) {
            throw new CommandException(TextFormatting.RED + err);
        }
        logger.log(TextFormatting.GREEN + "Bot config loaded: " + TextFormatting.WHITE + name);
    }

    // Применяет конфиг к ботам: копирует настройки HitAura/Scaffold/Velocity/AutoSprint
    // из облачного конфига в bot_module_config.json. Не трогает модули основного клиента.
    // Возвращает null при успехе или текст ошибки.
    public static String applyBotConfig(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "Укажи имя конфига";
        }
        try {
            ConfigStorage cs = Harmony.getInstance().getConfigStorage();
            if (cs == null) {
                return "Config storage unavailable";
            }
            JsonObject payload = cs.getConfigPayload(name);
            if (payload == null) {
                return "Config not found: " + name;
            }
            JsonObject cfgModules = payload.has("modules") ? payload.getAsJsonObject("modules") : null;
            if (cfgModules == null) {
                return "Config has no modules";
            }

            Path moduleConfigPath = MODULE_CONFIG_FILE;
            JsonObject json;
            if (Files.exists(moduleConfigPath)) {
                String content = Files.readString(moduleConfigPath, StandardCharsets.UTF_8);
                json = new JsonParser().parse(content).getAsJsonObject();
            } else {
                json = new JsonObject();
            }
            JsonObject modules = json.has("modules") ? json.getAsJsonObject("modules") : new JsonObject();

            String[] botModules = {"hitaura", "scaffold", "velocity", "autosprint"};
            for (String mn : botModules) {
                JsonObject src = cfgModules.getAsJsonObject(mn);
                if (src != null) {
                    modules.add(mn, new JsonParser().parse(src.toString()).getAsJsonObject());
                }
            }
            json.add("modules", modules);
            Files.writeString(moduleConfigPath, GSON.toJson(json), StandardCharsets.UTF_8);
            notifyConfigApplied(name);
            return null;
        } catch (Exception e) {
            return "Failed to apply config: " + e.getMessage();
        }
    }

    // Уведомляет основной клиент и всех запущенных ботов о применении конфига.
    private static void notifyConfigApplied(String name) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;

        String mainMsg = TextFormatting.GREEN + "Config " + TextFormatting.WHITE + name
                + TextFormatting.GREEN + " Complete";
        String botMsg = "Config " + name + " Complete";

        Runnable task = () -> {
            // Сообщение в чат основного клиента
            if (mc.ingameGUI != null && mc.ingameGUI.getChatGUI() != null) {
                ITextComponent component = new StringTextComponent(mainMsg);
                mc.ingameGUI.getChatGUI().printChatMessage(component);
            }
            // Сообщение в чат каждого запущенного бота
            try {
                BotSessionManager mgr = BotSessionManager.getInstance();
                for (BotSession session : mgr.snapshotSessions()) {
                    session.sendChat(botMsg);
                }
            } catch (Exception ignored) {
            }
        };

        if (mc.isOnExecutionThread()) {
            task.run();
        } else {
            mc.execute(task);
        }
    }

    private void cfgList() {
        ConfigStorage cs = Harmony.getInstance().getConfigStorage();
        if (cs == null || cs.isEmpty()) {
            logger.log(TextFormatting.GRAY + "Нет конфигов");
            return;
        }
        String configs = cs.getConfigs().stream()
            .map(cfg -> TextFormatting.WHITE + cfg.getName())
            .collect(Collectors.joining(TextFormatting.DARK_GRAY + " | "));
        logger.log(TextFormatting.DARK_GRAY + "* " + TextFormatting.GRAY + "Конфиги: " + configs);
    }

    private void openBotConfigsFolder() {
        try {
            Files.createDirectories(BOT_CONFIGS_DIR);
            File folder = BOT_CONFIGS_DIR.toFile();
            Desktop.getDesktop().open(folder);
            logger.log(TextFormatting.GREEN + "Открыта папка конфигов ботов");
        } catch (Exception e) {
            throw new CommandException(TextFormatting.RED + "Не удалось открыть папку: " + e.getMessage());
        }
    }

    private void cfgRemove(Parameters parameters) {
        String name = parameters.asString(2).orElseThrow(() -> new CommandException(
            usageLine("cfg remove <name>")));
        try {
            Path config = BOT_CONFIGS_DIR.resolve(name + ".json");
            if (!Files.exists(config)) {
                throw new CommandException(TextFormatting.RED + "Config not found: " + name);
            }
            Files.delete(config);
            logger.log(TextFormatting.GREEN + "Config removed: " + TextFormatting.WHITE + name);
        } catch (CommandException e) {
            throw e;
        } catch (IOException e) {
            throw new CommandException(TextFormatting.RED + "Failed to remove config: " + e.getMessage());
        }
    }

    private void persistToModuleConfig(String key, String value) {
        try {
            if (!Files.exists(MODULE_CONFIG_FILE)) return;

            String content = Files.readString(MODULE_CONFIG_FILE, StandardCharsets.UTF_8);
            JsonObject json = new JsonParser().parse(content).getAsJsonObject();
            JsonObject modules = json.getAsJsonObject("modules");
            if (modules == null) return;

            boolean modified = false;

            switch (key) {
                case "attack": {
                    JsonObject m = modules.getAsJsonObject("hitaura");
                    if (m != null) { m.addProperty("state", Boolean.parseBoolean(value)); modified = true; }
                    break;
                }
                case "attackRange": {
                    JsonObject m = modules.getAsJsonObject("hitaura");
                    if (m != null) {
                        try { m.addProperty("Дистанция аттаки", Float.parseFloat(value)); modified = true; }
                        catch (NumberFormatException ignored) {}
                    }
                    break;
                }
                case "sprint": {
                    JsonObject m = modules.getAsJsonObject("autosprint");
                    if (m != null) { m.addProperty("state", Boolean.parseBoolean(value)); modified = true; }
                    break;
                }
                case "scaffold": {
                    JsonObject m = modules.getAsJsonObject("scaffold");
                    if (m != null) { m.addProperty("state", Boolean.parseBoolean(value)); modified = true; }
                    break;
                }
                case "velocity": {
                    JsonObject m = modules.getAsJsonObject("velocity");
                    if (m != null) { m.addProperty("state", Boolean.parseBoolean(value)); modified = true; }
                    break;
                }
                case "autoSkin": {
                    JsonObject m = modules.getAsJsonObject("autoskin");
                    if (m != null) { m.addProperty("state", Boolean.parseBoolean(value)); modified = true; }
                    break;
                }
                case "autoRegister": {
                    JsonObject m = modules.getAsJsonObject("autoregister");
                    if (m != null) { m.addProperty("state", Boolean.parseBoolean(value)); modified = true; }
                    break;
                }
            }

            if (modified) {
                Files.writeString(MODULE_CONFIG_FILE, GSON.toJson(json), StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {}
    }
}
