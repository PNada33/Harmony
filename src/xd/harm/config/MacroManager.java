package xd.harm.config;

import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.Value;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import xd.harm.utils.client.IMinecraft;

public class MacroManager implements IMinecraft {
    public List<Macro> macroList = new ArrayList<>();
    public File macroFile = new File(mc.gameDir, "\\harmony\\files\\other\\macro.cfg");

    public void init() throws IOException {
        File parentDir = macroFile.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }
        if (!macroFile.exists()) {
            macroFile.createNewFile();
        } else {
            readFile();
        }
    }

    public boolean isEmpty() {
        return macroList.isEmpty();
    }

    public void addMacro(String name, String message, int key) {
        macroList.add(new Macro(name, message, key));
        writeFile();
    }

    public boolean hasMacro(String macroName) {
        for (Macro macro : macroList) {
            if (macro.getName().equalsIgnoreCase(macroName)) {
                return true;
            }
        }
        return false;
    }

    public void deleteMacro(String name) {
        if (macroList.stream()
                .anyMatch(macro -> macro.getName().equalsIgnoreCase(name))) {
            macroList.removeIf(macro -> macro.getName().equalsIgnoreCase(name));
            writeFile();
        }
    }

    public void clearList() {
        if (!macroList.isEmpty()) {
            macroList.clear();
        }
        writeFile();
    }

    public void onKeyPressed(int key) {
        if (mc.player == null || mc.currentScreen != null) {
            return;
        }
        macroList.stream()
                .filter(macro -> macro.getKey() == key)
                .findFirst()
                .ifPresent(macro -> {
                    try {
                        mc.player.sendChatMessage(macro.getMessage());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }

    @SneakyThrows
    public void writeFile() {
        StringBuilder builder = new StringBuilder();
        macroList.forEach(macro -> builder.append(macro.getName())
                .append(":").append(macro.getMessage())
                .append(":").append(macro.getKey())
                .append("\n"));
        Files.write(macroFile.toPath(), builder.toString().getBytes());
    }

    @SneakyThrows
    private void readFile() {
        if (!macroFile.exists() || macroFile.length() == 0) {
            return;
        }
        FileInputStream fileInputStream = new FileInputStream(macroFile.getAbsolutePath());
        @Cleanup
        BufferedReader reader = new BufferedReader(new InputStreamReader(new DataInputStream(fileInputStream)));
        String line;
        while ((line = reader.readLine()) != null) {
            String curLine = line.trim();
            if (curLine.isEmpty() || !curLine.contains(":")) continue;
            String[] parts = curLine.split(":");
            if (parts.length >= 3) {
                String name = parts[0];
                String command = parts[1];
                String key = parts[2];
                try {
                    macroList.add(new Macro(name, command, Integer.parseInt(key)));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Value
    public static class Macro {
        String name;
        String message;
        int key;
    }
}
