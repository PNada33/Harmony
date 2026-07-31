package xd.harm.ui.mainmenu;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import lombok.AllArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@AllArgsConstructor
public class Alt {

    public String name;
    public ResourceLocation skin;
    public String uuid;
    private boolean isPremium = false;

    @Override
    public String toString() {
        return name;
    }

    public Alt(String accountName) {
        this.name = accountName;

        this.skin = DefaultPlayerSkin.getDefaultSkin(UUID.randomUUID());
        CompletableFuture.runAsync(() -> {
            try {

                UUID playerUUID = resolveUUID(accountName);
                if (playerUUID != null) {
                    this.uuid = playerUUID.toString();
                    this.isPremium = true;

                    loadSkin(playerUUID, accountName);
                } else {
                    UUID offlineUUID = UUID.nameUUIDFromBytes(("OfflinePlayer:" + accountName).getBytes(StandardCharsets.UTF_8));
                    this.uuid = offlineUUID.toString();
                    this.isPremium = false;
                    this.skin = DefaultPlayerSkin.getDefaultSkin(offlineUUID);
                }
            } catch (Exception e) {
                UUID offlineUUID = UUID.nameUUIDFromBytes(("OfflinePlayer:" + accountName).getBytes(StandardCharsets.UTF_8));
                this.uuid = offlineUUID.toString();
                this.skin = DefaultPlayerSkin.getDefaultSkin(offlineUUID);
                this.isPremium = false;
            }
        });
    }

    private void loadSkin(UUID uuid, String accountName) {
        GameProfile profile = new GameProfile(uuid, accountName);
        Minecraft.getInstance().getSkinManager().loadProfileTextures(profile, (type, loc, tex) -> {
            if (type == MinecraftProfileTexture.Type.SKIN) {
                this.skin = loc;
            }
        }, true);
    }

    public static UUID resolveUUID(String name) {
        try {
            URLConnection connection = new URL("https://api.mojang.com/users/profiles/minecraft/" + name).openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            InputStreamReader in = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8);
            try {
                JsonObject jsonObject = new Gson().fromJson(in, JsonObject.class);
                if (jsonObject != null && jsonObject.has("id")) {
                    String id = jsonObject.get("id").getAsString();
                    return UUID.fromString(id.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
                }
            } catch (Exception e) {

            } finally {
                in.close();
            }
        } catch (Exception e) {
        }
        return null;
    }

    public boolean isPremium() {
        return isPremium;
    }
}
