package xd.harm.voicechat;

import de.maxhenkel.voicechat.intercompatibility.ClientCompatibilityManager;
import de.maxhenkel.voicechat.voice.client.ClientVoicechatConnection;
import de.maxhenkel.voicechat.voice.client.KeyEvents;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.entity.Entity;
import net.minecraft.network.NetworkManager;
import net.minecraft.resources.IPackFinder;
import net.minecraft.resources.ResourcePackList;
import net.minecraft.util.text.ITextComponent;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.ArrayUtils;

import java.io.BufferedReader;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.net.SocketAddress;
import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class HarmonyClientCompatibilityManager extends ClientCompatibilityManager {
    public static HarmonyClientCompatibilityManager INSTANCE;

    private final List<Runnable> clientTickEvents = new CopyOnWriteArrayList<>();
    private final List<Runnable> keyBindEvents = new CopyOnWriteArrayList<>();
    private final List<Runnable> disconnectEvents = new CopyOnWriteArrayList<>();
    private final List<Runnable> joinWorldEvents = new CopyOnWriteArrayList<>();
    private final List<RenderNameplateEvent> renderNameplateEvents = new CopyOnWriteArrayList<>();
    private final List<RenderHUDEvent> renderHUDEvents = new CopyOnWriteArrayList<>();
    private final List<KeyboardEvent> keyboardEvents = new CopyOnWriteArrayList<>();
    private final List<MouseEvent> mouseEvents = new CopyOnWriteArrayList<>();
    private final List<Consumer<Integer>> publishServerEvents = new CopyOnWriteArrayList<>();
    private final List<Consumer<ClientVoicechatConnection>> voiceChatConnectedEvents = new CopyOnWriteArrayList<>();
    private final List<Runnable> voiceChatDisconnectedEvents = new CopyOnWriteArrayList<>();

    public HarmonyClientCompatibilityManager() {
        INSTANCE = this;
    }

    @Override
    public void onRenderNamePlate(RenderNameplateEvent event) {
        renderNameplateEvents.add(event);
    }

    @Override
    public void onRenderHUD(RenderHUDEvent event) {
        renderHUDEvents.add(event);
    }

    @Override
    public void onKeyboardEvent(KeyboardEvent event) {
        keyboardEvents.add(event);
    }

    @Override
    public void onMouseEvent(MouseEvent event) {
        mouseEvents.add(event);
    }

    @Override
    public void onClientTick(Runnable event) {
        clientTickEvents.add(event);
    }

    @Override
    public InputMappings.Input getBoundKeyOf(KeyBinding keyBinding) {
        return keyBinding.getKey();
    }

    @Override
    public void onHandleKeyBinds(Runnable event) {
        keyBindEvents.add(event);
    }

    @Override
    public KeyBinding registerKeyBinding(KeyBinding keyBinding) {
        Minecraft minecraft = Minecraft.getInstance();

        boolean loadedSavedKey = loadSavedKeyBinding(minecraft, keyBinding);
        if (!loadedSavedKey && "key.push_to_talk".equals(keyBinding.getKeyDescription()) && keyBinding.isInvalid()) {
            keyBinding.bind(InputMappings.Type.KEYSYM.getOrMakeInput(280));
        }

        if (minecraft != null && minecraft.gameSettings != null && !ArrayUtils.contains(minecraft.gameSettings.keyBindings, keyBinding)) {
            minecraft.gameSettings.keyBindings = ArrayUtils.add(minecraft.gameSettings.keyBindings, keyBinding);
            KeyBinding.resetKeyBindingArrayAndHash();
        }

        return keyBinding;
    }

    private boolean loadSavedKeyBinding(Minecraft minecraft, KeyBinding keyBinding) {
        if (minecraft == null || minecraft.gameDir == null || keyBinding == null) {
            return false;
        }

        File optionsFile = new File(minecraft.gameDir, "options.txt");
        if (!optionsFile.isFile()) {
            return false;
        }

        String optionName = "key_" + keyBinding.getKeyDescription();
        String savedInputName = findSavedInputName(optionsFile, optionName);
        if (savedInputName == null || savedInputName.isEmpty()) {
            return false;
        }

        try {
            keyBinding.bind(InputMappings.getInputByName(savedInputName));
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private String findSavedInputName(File optionsFile, String optionName) {
        String prefix = optionName + ":";
        try (BufferedReader reader = Files.newBufferedReader(optionsFile.toPath(), StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith(prefix)) {
                    continue;
                }

                String value = line.substring(prefix.length()).trim();
                int modifierSeparator = value.indexOf(':');
                return modifierSeparator >= 0 ? value.substring(0, modifierSeparator) : value;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    @Override
    public void emitVoiceChatConnectedEvent(ClientVoicechatConnection connection) {
        voiceChatConnectedEvents.forEach(event -> event.accept(connection));
    }

    @Override
    public void emitVoiceChatDisconnectedEvent() {
        voiceChatDisconnectedEvents.forEach(Runnable::run);
    }

    @Override
    public void onVoiceChatConnected(Consumer<ClientVoicechatConnection> event) {
        voiceChatConnectedEvents.add(event);
    }

    @Override
    public void onVoiceChatDisconnected(Runnable event) {
        voiceChatDisconnectedEvents.add(event);
    }

    @Override
    public void emitDisconnectedEvent() {
        disconnectEvents.forEach(Runnable::run);
    }

    @Override
    public void onDisconnect(Runnable event) {
        disconnectEvents.add(event);
    }

    @Override
    public void onJoinWorld(Runnable event) {
        joinWorldEvents.add(event);
    }

    @Override
    public void onPublishServer(Consumer<Integer> event) {
        publishServerEvents.add(event);
    }

    @Override
    public SocketAddress getSocketAddress(NetworkManager networkManager) {
        return networkManager.getRemoteAddress();
    }

    @Override
    public void addResourcePackSource(ResourcePackList resourcePacks, IPackFinder packFinder) {
        try {
            Field field = ResourcePackList.class.getDeclaredField("packFinders");
            field.setAccessible(true);

            Set<IPackFinder> packFinders = new LinkedHashSet<>((Set<IPackFinder>) field.get(resourcePacks));
            packFinders.add(packFinder);
            field.set(resourcePacks, ImmutableSet.copyOf(packFinders));
            resourcePacks.reloadPacksFromFinders();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void fireClientTick() {
        clientTickEvents.forEach(Runnable::run);
    }

    void fireHandleKeyBinds() {
        keyBindEvents.forEach(Runnable::run);
    }

    void fireKeyStatePoll() {
        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft == null || minecraft.getMainWindow() == null || keyboardEvents.isEmpty() && mouseEvents.isEmpty()) {
            return;
        }

        fireInputState(minecraft.getMainWindow().getHandle(), KeyEvents.KEY_PTT);
        fireInputState(minecraft.getMainWindow().getHandle(), KeyEvents.KEY_WHISPER);
    }

    void fireRenderHud(MatrixStack matrixStack, float partialTicks) {
        renderHUDEvents.forEach(event -> event.render(matrixStack, partialTicks));
    }

    void fireRenderNameplate(Entity entity, ITextComponent displayName, MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight) {
        renderNameplateEvents.forEach(event -> event.render(entity, displayName, matrixStack, buffer, packedLight));
    }

    void fireJoinWorld() {
        joinWorldEvents.forEach(Runnable::run);
    }

    void fireDisconnect() {
        disconnectEvents.forEach(Runnable::run);
    }

    private void fireInputState(long windowHandle, KeyBinding keyBinding) {
        if (keyBinding == null) {
            return;
        }

        InputMappings.Input input = getBoundKeyOf(keyBinding);

        if (input == null || input.getKeyCode() == InputMappings.INPUT_INVALID.getKeyCode()) {
            return;
        }

        if (input.getType() == InputMappings.Type.MOUSE) {
            mouseEvents.forEach(event -> event.onMouseEvent(windowHandle, input.getKeyCode(), 0, 0));
        } else {
            keyboardEvents.forEach(event -> event.onKeyboardEvent(windowHandle, input.getKeyCode(), 0));
        }
    }
}
