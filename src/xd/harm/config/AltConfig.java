package xd.harm.config;

import com.google.gson.*;
import xd.harm.Harmony;
import xd.harm.ui.mainmenu.AltScreen;
import xd.harm.utils.client.IMinecraft;
import net.minecraft.util.Session;

import java.io.*;
import java.util.UUID;

public class AltConfig implements IMinecraft {

    static Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final File file = new File(mc.gameDir, "harmony/files/other/alts.cfg");

    public void init() {
        try {
            if (!file.exists()) {

                file.getParentFile().mkdirs();
                file.createNewFile();
                System.out.println("Создан новый файл alts.cfg");
            } else {
                loadAlts();
            }
        } catch (IOException e) {
            System.err.println("Ошибка при инициализации AltConfig: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void updateFile() {
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("last", mc.session.getUsername());

        JsonArray altsArray = new JsonArray();
        for (AltScreen.Account account : Harmony.getInstance().getAltScreen().accounts) {
            JsonObject altObject = new JsonObject();
            altObject.addProperty("name", account.name);
            if (account.uuid != null) {
                altObject.addProperty("uuid", account.uuid);
            }
            altObject.addProperty("creationTime", account.creationTime);
            altsArray.add(altObject);
        }

        jsonObject.add("alts", altsArray);

        try (PrintWriter printWriter = new PrintWriter(new FileWriter(file))) {
            printWriter.println(gson.toJson(jsonObject));
        } catch (IOException e) {
            System.err.println("Ошибка при сохранении alts.cfg: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void loadAlts() {
        if (!file.exists()) {
            System.out.println("Файл alts.cfg не найден, пропускаем загрузку.");
            return;
        }

        try (FileReader reader = new FileReader(file)) {

            JsonElement jsonElement = new JsonParser().parse(reader);

            if (jsonElement.isJsonNull()) {
                System.out.println("Файл alts.cfg пуст или содержит null.");
                return;
            }

            JsonObject jsonObject = jsonElement.getAsJsonObject();

            Harmony.getInstance().getAltScreen().accounts.clear();

            if (jsonObject.has("last")) {
                String lastUsername = jsonObject.get("last").getAsString();
                mc.session = new Session(lastUsername, UUID.randomUUID().toString(), "", "mojang");
            }

            if (jsonObject.has("alts")) {
                JsonArray altsArray = jsonObject.getAsJsonArray("alts");
                for (JsonElement element : altsArray) {
                    if (element.isJsonObject()) {
                        JsonObject altObject = element.getAsJsonObject();
                        if (altObject.has("name")) {
                            String name = altObject.get("name").getAsString();
                            long creationTime = altObject.has("creationTime") ?
                                    altObject.get("creationTime").getAsLong() :
                                    System.currentTimeMillis();

                            AltScreen.Account account = Harmony.getInstance().getAltScreen().new Account(name, creationTime);

                            if (altObject.has("uuid")) {
                                account.uuid = altObject.get("uuid").getAsString();
                            }

                            Harmony.getInstance().getAltScreen().accounts.add(account);
                        }
                    } else if (element.isJsonPrimitive()) {

                        String name = element.getAsString();
                        Harmony.getInstance().getAltScreen().accounts.add(
                                Harmony.getInstance().getAltScreen().new Account(name)
                        );
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Ошибка при чтении alts.cfg: " + e.getMessage());
            e.printStackTrace();
        } catch (JsonParseException e) {
            System.err.println("Ошибка при парсинге JSON в alts.cfg: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
