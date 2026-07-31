package net.minecraft.client.settings;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.InputMappings;
import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.client.settings.IForgeKeybinding;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyBindingMap;
import net.minecraftforge.client.settings.KeyModifier;

public class KeyBinding implements Comparable<KeyBinding>, IForgeKeybinding
{
    private static final Map<String, KeyBinding> KEYBIND_ARRAY = Maps.newHashMap();
    private static final Map<InputMappings.Input, KeyBinding> HASH = Maps.newHashMap();
    private static final KeyBindingMap MAP = new KeyBindingMap();
    private static final Set<String> KEYBIND_SET = Sets.newHashSet();
    private static final Map<String, Integer> CATEGORY_ORDER = Util.make(Maps.newHashMap(), (p_205215_0_) ->
    {
        p_205215_0_.put("key.categories.movement", 1);
        p_205215_0_.put("key.categories.gameplay", 2);
        p_205215_0_.put("key.categories.inventory", 3);
        p_205215_0_.put("key.categories.creative", 4);
        p_205215_0_.put("key.categories.multiplayer", 5);
        p_205215_0_.put("key.categories.ui", 6);
        p_205215_0_.put("key.categories.misc", 7);
    });
    private final String keyDescription;
    private final InputMappings.Input keyCodeDefault;
    private final String keyCategory;
    private InputMappings.Input keyCode;
    public boolean pressed;
    private int pressTime;
    private KeyModifier keyModifierDefault = KeyModifier.NONE;
    private KeyModifier keyModifier = KeyModifier.NONE;
    private IKeyConflictContext keyConflictContext = KeyModifier.KeyConflictContext.UNIVERSAL;

    public static void onTick(InputMappings.Input key)
    {
        KeyBinding keybinding = HASH.get(key);

        if (keybinding != null)
        {
            ++keybinding.pressTime;
        }
    }

    public static void setKeyBindState(InputMappings.Input key, boolean held)
    {
        KeyBinding keybinding = HASH.get(key);

        if (keybinding != null)
        {
            keybinding.setPressed(held);
        }
    }

    public static void updateKeyBindState()
    {
        for (KeyBinding keybinding : KEYBIND_ARRAY.values())
        {
            if (keybinding.keyCode.getType() == InputMappings.Type.KEYSYM && keybinding.keyCode.getKeyCode() != InputMappings.INPUT_INVALID.getKeyCode())
            {
                keybinding.setPressed(InputMappings.isKeyDown(Minecraft.getInstance().getMainWindow().getHandle(), keybinding.keyCode.getKeyCode()));
            }
        }
    }

    public static void unPressAllKeys()
    {
        for (KeyBinding keybinding : KEYBIND_ARRAY.values())
        {
            keybinding.unpressKey();
        }
    }

    public static void resetKeyBindingArrayAndHash()
    {
        HASH.clear();
        MAP.clearMap();

        for (KeyBinding keybinding : KEYBIND_ARRAY.values())
        {
            HASH.put(keybinding.keyCode, keybinding);
            MAP.addKey(keybinding.keyCode, keybinding);
        }
    }

    public KeyBinding(String description, int keyCode, String category)
    {
        this(description, InputMappings.Type.KEYSYM, keyCode, category);
    }

    public KeyBinding(String description, InputMappings.Type type, int code, String category)
    {
        this.keyDescription = description;
        this.keyCode = type.getOrMakeInput(code);
        this.keyCodeDefault = this.keyCode;
        this.keyCategory = category;
        KEYBIND_ARRAY.put(description, this);
        HASH.put(this.keyCode, this);
        MAP.addKey(this.keyCode, this);
        KEYBIND_SET.add(category);
    }

    public KeyBinding(String description, IKeyConflictContext conflictContext, KeyModifier modifier, InputMappings.Input keyInput, String category)
    {
        this.keyDescription = description;
        this.keyCode = keyInput;
        this.keyCodeDefault = keyInput;
        this.keyCategory = category;
        this.keyConflictContext = conflictContext;
        this.keyModifier = modifier;
        this.keyModifierDefault = modifier;
        if (this.keyModifier.matches(keyInput))
        {
            this.keyModifier = KeyModifier.NONE;
        }
        KEYBIND_ARRAY.put(description, this);
        MAP.addKey(keyInput, this);
        KEYBIND_SET.add(category);
    }

    public boolean isKeyDown()
    {
        return this.pressed;
    }

    public String getKeyCategory()
    {
        return this.keyCategory;
    }

    public boolean isPressed()
    {
        if (this.pressTime == 0)
        {
            return false;
        }
        else
        {
            --this.pressTime;
            return true;
        }
    }

    private void unpressKey()
    {
        this.pressTime = 0;
        this.setPressed(false);
    }

    public String getKeyDescription()
    {
        return this.keyDescription;
    }

    public InputMappings.Input getDefault()
    {
        return this.keyCodeDefault;
    }

    @Override
    public InputMappings.Input getKey()
    {
        return this.keyCode;
    }

    public void bind(InputMappings.Input key)
    {
        this.keyCode = key;
    }

    public int compareTo(KeyBinding p_compareTo_1_)
    {
        if (this.keyCategory.equals(p_compareTo_1_.keyCategory))
        {
            return I18n.format(this.keyDescription).compareTo(I18n.format(p_compareTo_1_.keyDescription));
        }
        else
        {
            Integer tCat = CATEGORY_ORDER.get(this.keyCategory);
            Integer oCat = CATEGORY_ORDER.get(p_compareTo_1_.keyCategory);
            if (tCat == null && oCat != null)
            {
                return 1;
            }
            else if (tCat != null && oCat == null)
            {
                return -1;
            }
            else
            {
                return tCat == null && oCat == null ? I18n.format(this.keyCategory).compareTo(I18n.format(p_compareTo_1_.keyCategory)) : tCat.compareTo(oCat);
            }
        }
    }

    public static Supplier<ITextComponent> getDisplayString(String key)
    {
        KeyBinding keybinding = KEYBIND_ARRAY.get(key);
        return keybinding == null ? () ->
        {
            return new TranslationTextComponent(key);
        } : keybinding::func_238171_j_;
    }

    public boolean conflicts(KeyBinding binding)
    {
        return this.keyCode.equals(binding.keyCode);
    }

    public boolean isInvalid()
    {
        return this.keyCode.equals(InputMappings.INPUT_INVALID);
    }

    public boolean matchesKey(int keysym, int scancode)
    {
        if (keysym == InputMappings.INPUT_INVALID.getKeyCode())
        {
            return this.keyCode.getType() == InputMappings.Type.SCANCODE && this.keyCode.getKeyCode() == scancode;
        }
        else
        {
            return this.keyCode.getType() == InputMappings.Type.KEYSYM && this.keyCode.getKeyCode() == keysym;
        }
    }

    public ITextComponent getTranslatedKeyMessage()
    {
        return this.keyModifier.getCombinedName(this.keyCode, () -> this.keyCode.func_237520_d_());
    }

    public boolean matchesMouseKey(int key)
    {
        return this.keyCode.getType() == InputMappings.Type.MOUSE && this.keyCode.getKeyCode() == key;
    }

    public ITextComponent func_238171_j_()
    {
        return this.keyCode.func_237520_d_();
    }

    public boolean isDefault()
    {
        return this.keyCode.equals(this.keyCodeDefault) && this.keyModifier == this.keyModifierDefault;
    }

    public String getTranslationKey()
    {
        return this.keyCode.getTranslationKey();
    }

    public void setPressed(boolean valueIn)
    {
        this.pressed = valueIn;
    }

    @Override
    public void setKeyConflictContext(IKeyConflictContext conflictContext)
    {
        this.keyConflictContext = conflictContext;
    }

    @Override
    public IKeyConflictContext getKeyConflictContext()
    {
        return this.keyConflictContext;
    }

    @Override
    public KeyModifier getKeyModifierDefault()
    {
        return this.keyModifierDefault;
    }

    @Override
    public KeyModifier getKeyModifier()
    {
        return this.keyModifier;
    }

    @Override
    public void setKeyModifierAndCode(KeyModifier modifier, InputMappings.Input keyInput)
    {
        this.keyCode = keyInput;
        if (modifier.matches(keyInput))
        {
            modifier = KeyModifier.NONE;
        }
        MAP.removeKey(this);
        this.keyModifier = modifier;
        MAP.addKey(keyInput, this);
    }
}