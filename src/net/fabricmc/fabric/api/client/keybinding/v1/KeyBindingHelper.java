package net.fabricmc.fabric.api.client.keybinding.v1;

import net.minecraft.client.settings.KeyBinding;

public final class KeyBindingHelper {
    private KeyBindingHelper() {
    }

    public static KeyBinding registerKeyBinding(KeyBinding keyBinding) {
        return keyBinding;
    }
}
