package xd.harm.voicechat;

import com.mojang.brigadier.CommandDispatcher;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.intercompatibility.CommonCompatibilityManager;
import de.maxhenkel.voicechat.net.NetManager;
import de.maxhenkel.voicechat.permission.PermissionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class HarmonyCommonCompatibilityManager extends CommonCompatibilityManager {
    private final HarmonyNetManager netManager = new HarmonyNetManager();

    @Override
    public String getModVersion() {
        return "2.6.16";
    }

    @Override
    public String getModName() {
        return "Simple Voice Chat";
    }

    @Override
    public Path getGameDirectory() {
        return Minecraft.getInstance().gameDir.toPath();
    }

    @Override
    public void emitServerVoiceChatConnectedEvent(ServerPlayerEntity player) {
    }

    @Override
    public void emitServerVoiceChatDisconnectedEvent(UUID playerId) {
    }

    @Override
    public void emitPlayerCompatibilityCheckSucceeded(ServerPlayerEntity player) {
    }

    @Override
    public void onServerVoiceChatConnected(Consumer<ServerPlayerEntity> event) {
    }

    @Override
    public void onServerVoiceChatDisconnected(Consumer<UUID> event) {
    }

    @Override
    public void onServerStarting(Consumer<MinecraftServer> event) {
    }

    @Override
    public void onServerStopping(Consumer<MinecraftServer> event) {
    }

    @Override
    public void onPlayerLoggedIn(Consumer<ServerPlayerEntity> event) {
    }

    @Override
    public void onPlayerLoggedOut(Consumer<ServerPlayerEntity> event) {
    }

    @Override
    public void onPlayerHide(BiConsumer<ServerPlayerEntity, ServerPlayerEntity> event) {
    }

    @Override
    public void onPlayerShow(BiConsumer<ServerPlayerEntity, ServerPlayerEntity> event) {
    }

    @Override
    public void onPlayerCompatibilityCheckSucceeded(Consumer<ServerPlayerEntity> event) {
    }

    @Override
    public void onRegisterServerCommands(Consumer<CommandDispatcher<CommandSource>> event) {
    }

    @Override
    public NetManager getNetManager() {
        return netManager;
    }

    public HarmonyNetManager getHarmonyNetManager() {
        return netManager;
    }

    @Override
    public boolean isDevEnvironment() {
        return false;
    }

    @Override
    public boolean isDedicatedServer() {
        return false;
    }

    @Override
    public boolean isModLoaded(String modId) {
        return "voicechat".equals(modId);
    }

    @Override
    public List<VoicechatPlugin> loadPlugins() {
        return Collections.emptyList();
    }

    @Override
    public PermissionManager createPermissionManager() {
        return new HarmonyPermissionManager();
    }

    @Override
    public boolean canSee(ServerPlayerEntity observer, ServerPlayerEntity observed) {
        return true;
    }
}
