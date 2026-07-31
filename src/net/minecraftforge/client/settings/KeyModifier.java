package net.minecraftforge.client.settings;

import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputMappings;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public enum KeyModifier {
    CONTROL{

        @Override
        public boolean matches(InputMappings.Input key) {
            int keyCode = key.getKeyCode();
            if (Minecraft.IS_RUNNING_ON_MAC) {
                return keyCode == 342 || keyCode == 346;
            }
            return keyCode == 341 || keyCode == 345;
        }

        @Override
        public boolean isActive(@Nullable IKeyConflictContext conflictContext) {
            return Screen.hasControlDown();
        }

        @Override
        public ITextComponent getCombinedName(InputMappings.Input key, Supplier<ITextComponent> defaultLogic) {
            String localizationFormatKey = Minecraft.IS_RUNNING_ON_MAC ? "forge.controlsgui.control.mac" : "forge.controlsgui.control";
            return new TranslationTextComponent(localizationFormatKey, defaultLogic.get());
        }
    }
    ,
    SHIFT{

        @Override
        public boolean matches(InputMappings.Input key) {
            return key.getKeyCode() == 340 || key.getKeyCode() == 344;
        }

        @Override
        public boolean isActive(@Nullable IKeyConflictContext conflictContext) {
            return Screen.hasShiftDown();
        }

        @Override
        public ITextComponent getCombinedName(InputMappings.Input key, Supplier<ITextComponent> defaultLogic) {
            return new TranslationTextComponent("forge.controlsgui.shift", defaultLogic.get());
        }
    }
    ,
    ALT{

        @Override
        public boolean matches(InputMappings.Input key) {
            return key.getKeyCode() == 342 || key.getKeyCode() == 346;
        }

        @Override
        public boolean isActive(@Nullable IKeyConflictContext conflictContext) {
            return Screen.hasAltDown();
        }

        @Override
        public ITextComponent getCombinedName(InputMappings.Input keyCode, Supplier<ITextComponent> defaultLogic) {
            return new TranslationTextComponent("forge.controlsgui.alt", defaultLogic.get());
        }
    }
    ,
    NONE{

        @Override
        public boolean matches(InputMappings.Input key) {
            return false;
        }

        @Override
        public boolean isActive(@Nullable IKeyConflictContext conflictContext) {
            if (conflictContext != null && !conflictContext.conflicts(KeyConflictContext.IN_GAME)) {
                for (KeyModifier keyModifier : MODIFIER_VALUES) {
                    if (!keyModifier.isActive(conflictContext)) continue;
                    return false;
                }
            }
            return true;
        }

        @Override
        public ITextComponent getCombinedName(InputMappings.Input key, Supplier<ITextComponent> defaultLogic) {
            return defaultLogic.get();
        }
    };

    public static final KeyModifier[] MODIFIER_VALUES;

    public static KeyModifier getActiveModifier() {
        for (KeyModifier keyModifier : MODIFIER_VALUES) {
            if (!keyModifier.isActive(null)) continue;
            return keyModifier;
        }
        return NONE;
    }

    public static boolean isKeyCodeModifier(InputMappings.Input key) {
        for (KeyModifier keyModifier : MODIFIER_VALUES) {
            if (!keyModifier.matches(key)) continue;
            return true;
        }
        return false;
    }

    public static KeyModifier valueFromString(String stringValue) {
        try {
            return KeyModifier.valueOf(stringValue);
        }
        catch (IllegalArgumentException | NullPointerException ignored) {
            return NONE;
        }
    }

    public abstract boolean matches(InputMappings.Input var1);

    public abstract boolean isActive(@Nullable IKeyConflictContext var1);

    public abstract ITextComponent getCombinedName(InputMappings.Input var1, Supplier<ITextComponent> var2);

    static {
        MODIFIER_VALUES = new KeyModifier[]{SHIFT, CONTROL, ALT};
    }
    public enum KeyConflictContext implements IKeyConflictContext
    {
        UNIVERSAL{

            @Override
            public boolean isActive() {
                return true;
            }

            @Override
            public boolean conflicts(IKeyConflictContext other) {
                return true;
            }
        }
        ,
        GUI{

            @Override
            public boolean isActive() {
                return Minecraft.getInstance().currentScreen != null;
            }

            @Override
            public boolean conflicts(IKeyConflictContext other) {
                return this == other;
            }
        }
        ,
        IN_GAME{

            @Override
            public boolean isActive() {
                return !GUI.isActive();
            }

            @Override
            public boolean conflicts(IKeyConflictContext other) {
                return this == other;
            }
        };

    }
}
