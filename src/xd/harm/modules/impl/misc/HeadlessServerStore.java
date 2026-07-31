package xd.harm.modules.impl.misc;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class HeadlessServerStore {
    public static final String LOCAL_NAME = "LocalHost";
    private final LinkedHashMap<String,String> servers = new LinkedHashMap<>();
    private final File file = new File("config", "harmony_headless_servers.properties");

    public HeadlessServerStore() { load(); }

    private synchronized void load() {
        servers.clear();
        servers.put(LOCAL_NAME, "");
        if (!file.isFile()) return;
        Properties props = new Properties();
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            props.load(reader);
            List<String> names = new ArrayList<>(props.stringPropertyNames());
            names.sort(String.CASE_INSENSITIVE_ORDER);
            for (String name : names) if (!name.equalsIgnoreCase(LOCAL_NAME)) {
                String address = normalizeAddress(props.getProperty(name, ""));
                if (!name.trim().isEmpty() && address != null) servers.put(name.trim(), address);
            }
        } catch (Exception ignored) {}
    }

    private synchronized void save() {
        try {
            File parent = file.getParentFile();
            if (parent != null) parent.mkdirs();
            Properties props = new Properties();
            for (Map.Entry<String,String> entry : servers.entrySet()) if (!entry.getKey().equalsIgnoreCase(LOCAL_NAME)) props.setProperty(entry.getKey(), entry.getValue());
            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
                props.store(writer, "Harmony Headless saved servers");
            }
        } catch (Exception ignored) {}
    }

    public synchronized List<String> names() { return new ArrayList<>(servers.keySet()); }
    public synchronized String address(String name) { return servers.getOrDefault(name, ""); }
    public synchronized String nameForAddress(String address) {
        String normalized = normalizeAddress(address);
        for (Map.Entry<String,String> entry : servers.entrySet()) if (normalized != null && !entry.getValue().isEmpty() && entry.getValue().equalsIgnoreCase(normalized)) return entry.getKey();
        return null;
    }
    public synchronized boolean put(String name, String address) {
        name = String.valueOf(name).trim(); address = normalizeAddress(address);
        if (name.isEmpty() || name.length() > 32 || name.equalsIgnoreCase(LOCAL_NAME) || address == null) return false;
        String old = null;
        for (String existing : servers.keySet()) if (existing.equalsIgnoreCase(name)) { old = existing; break; }
        if (old != null) servers.remove(old);
        servers.put(name, address); save(); return true;
    }
    public synchronized void remove(String name) {
        if (name == null || name.equalsIgnoreCase(LOCAL_NAME)) return;
        servers.keySet().removeIf(existing -> existing.equalsIgnoreCase(name)); save();
    }
    public synchronized void setLocalPort(int port) { servers.put(LOCAL_NAME, port > 0 && port <= 65535 ? "127.0.0.1:" + port : ""); }
    public static String normalizeAddress(String address) {
        if (address == null) return null;
        String value = address.trim();
        if (value.regionMatches(true, 0, "minecraft://", 0, 12)) value = value.substring(12).trim();
        else if (value.regionMatches(true, 0, "http://", 0, 7)) value = value.substring(7).trim();
        else if (value.regionMatches(true, 0, "https://", 0, 8)) value = value.substring(8).trim();
        while (value.endsWith("/")) value = value.substring(0, value.length() - 1).trim();
        if (value.isEmpty() || value.contains("/") || value.contains(" ")) return null;
        int split = value.lastIndexOf(':');
        if (split < 0) {
            if (!value.matches("[A-Za-z0-9._-]+")) return null;
            return value + ":25565";
        }
        if (split < 1 || split == value.length() - 1) return null;
        String host = value.substring(0, split).trim(), portText = value.substring(split + 1).trim();
        if (host.isEmpty() || !host.matches("[A-Za-z0-9._-]+")) return null;
        try { int port = Integer.parseInt(portText); return port > 0 && port <= 65535 ? host + ":" + port : null; }
        catch (Exception ignored) { return null; }
    }
    public static boolean validAddress(String address) { return normalizeAddress(address) != null; }
}
