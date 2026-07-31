package net.minecraftforge.client.settings;

import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;

import javax.annotation.Nonnull;

public interface IForgeKeybinding {
    default public KeyBinding getKeyBinding() {
        return (KeyBinding)this;
    }

    @Nonnull
    public InputMappings.Input getKey();

    default public boolean isActiveAndMatches(InputMappings.Input keyCode) {
        return keyCode != InputMappings.INPUT_INVALID && keyCode.equals(this.getKey()) && this.getKeyConflictContext().isActive() && this.getKeyModifier().isActive(this.getKeyConflictContext());
    }

    default public void setToDefault() {
        this.setKeyModifierAndCode(this.getKeyModifierDefault(), this.getKeyBinding().getDefault());
    }

    public void setKeyConflictContext(IKeyConflictContext var1);

    public IKeyConflictContext getKeyConflictContext();

    public KeyModifier getKeyModifierDefault();

    public KeyModifier getKeyModifier();

    public void setKeyModifierAndCode(KeyModifier var1, InputMappings.Input var2);

    default public boolean isConflictContextAndModifierActive() {
        return this.getKeyConflictContext().isActive() && this.getKeyModifier().isActive(this.getKeyConflictContext());
    }

    default public boolean hasKeyCodeModifierConflict(KeyBinding other) {
        return !(!this.getKeyConflictContext().conflicts(other.getKeyConflictContext()) && !other.getKeyConflictContext().conflicts(this.getKeyConflictContext()) || !this.getKeyModifier().matches(other.getKey()) && !other.getKeyModifier().matches(this.getKey()));
    }
}

