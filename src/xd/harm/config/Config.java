package xd.harm.config;

import xd.harm.modules.settings.impl.*;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import xd.harm.Harmony;
import xd.harm.modules.impl.combat.AutoSwap;
import xd.harm.modules.settings.Setting;

import xd.harm.utils.client.IMinecraft;
import lombok.Getter;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.JsonToNBT;

import java.util.function.Consumer;

@Getter
public class Config implements IMinecraft {
    private final String name;

    public Config(String name) {
        this.name = name;
    }

    public void loadConfig(JsonObject jsonObject) {
        if (jsonObject == null) {
            return;
        }

        if (jsonObject.has("modules")) {
            loadFunctionSettings(jsonObject.getAsJsonObject("modules"));
        }

        if (jsonObject.has("swapItems")) {
            loadSwapItems(jsonObject.getAsJsonObject("swapItems"));
        }
    }

    private void loadSwapItems(JsonObject swapItemsObject) {
        for (int i = 0; i < 3; i++) {
            String key = "slot" + i;
            if (swapItemsObject.has(key)) {
                try {
                    String nbtString = swapItemsObject.get(key).getAsString();
                    CompoundNBT nbt = JsonToNBT.getTagFromJson(nbtString);
                    AutoSwap.threeItems[i] = ItemStack.read(nbt);
                } catch (Exception e) {
                    AutoSwap.threeItems[i] = ItemStack.EMPTY;
                }
            } else {
                AutoSwap.threeItems[i] = ItemStack.EMPTY;
            }
        }
    }

    private void loadFunctionSettings(JsonObject functionsObject) {
        Harmony.getInstance().getModuleManager().getModules().forEach(f -> {
            try {
                JsonObject moduleObject = resolveModuleObject(functionsObject, f.getName());
                if (moduleObject == null) {
                    return;
                }

                f.setState(false, true);
                loadSettingFromJson(moduleObject, "bind", value -> f.setBind(value.getAsInt()));
                loadSettingFromJson(moduleObject, "state", value -> f.setState(value.getAsBoolean(), true));
                f.getSettings().forEach(setting -> loadIndividualSetting(moduleObject, setting));
            } catch (Exception e) {
                System.err.println("Ошибка при загрузке модуля " + f.getName() + ": " + e.getMessage());
            }
        });
    }

    private JsonObject resolveModuleObject(JsonObject functionsObject, String moduleName) {
        JsonElement moduleElement = functionsObject.get(moduleName.toLowerCase());
        if ((moduleElement == null || !moduleElement.isJsonObject()) && "visuality".equalsIgnoreCase(moduleName)) {
            moduleElement = functionsObject.get("betterminecraft");
        }
        return moduleElement != null && moduleElement.isJsonObject() ? moduleElement.getAsJsonObject() : null;
    }

    private void loadIndividualSetting(JsonObject moduleObject, Setting<?> setting) {
        try {
            JsonElement settingElement = moduleObject.get(setting.getConfigKey());

            if (settingElement == null || settingElement.isJsonNull()) {
                return;
            }

            if (setting instanceof SliderSetting) {
                if (settingElement.isJsonPrimitive() && settingElement.getAsJsonPrimitive().isNumber()) {
                    ((SliderSetting) setting).set(settingElement.getAsFloat());
                }
            } else if (setting instanceof BooleanSetting) {
                if (settingElement.isJsonPrimitive() && settingElement.getAsJsonPrimitive().isBoolean()) {
                    ((BooleanSetting) setting).set(settingElement.getAsBoolean());
                }
            } else if (setting instanceof ColorSetting) {
                if (settingElement.isJsonPrimitive() && settingElement.getAsJsonPrimitive().isNumber()) {
                    ((ColorSetting) setting).set(settingElement.getAsInt());
                }
            } else if (setting instanceof ModeSetting) {
                if (settingElement.isJsonPrimitive() && settingElement.getAsJsonPrimitive().isString()) {
                    ((ModeSetting) setting).set(settingElement.getAsString());
                }
            } else if (setting instanceof BindSetting) {
                if (settingElement.isJsonPrimitive() && settingElement.getAsJsonPrimitive().isNumber()) {
                    ((BindSetting) setting).set(settingElement.getAsInt());
                }
            } else if (setting instanceof StringSetting) {
                if (settingElement.isJsonPrimitive() && settingElement.getAsJsonPrimitive().isString()) {
                    ((StringSetting) setting).set(settingElement.getAsString());
                }
            } else if (setting instanceof ModeListSetting) {
                loadModeListSetting((ModeListSetting) setting, moduleObject);
            }
        } catch (Exception e) {
            System.err.println("Ошибка при загрузке настройки " + setting.getName() + ": " + e.getMessage());
        }
    }

    private void loadModeListSetting(ModeListSetting setting, JsonObject moduleObject) {
        try {
            JsonElement settingElement = moduleObject.get(setting.getConfigKey());

            if (settingElement == null || !settingElement.isJsonObject()) {
                System.err.println("ModeListSetting " + setting.getName() + " не является JsonObject или отсутствует");
                return;
            }

            JsonObject elements = settingElement.getAsJsonObject();

            setting.get().forEach(option -> {
                try {
                    JsonElement optionElement = elements.get(option.getName());
                    if (optionElement != null && !optionElement.isJsonNull() &&
                            optionElement.isJsonPrimitive() && optionElement.getAsJsonPrimitive().isBoolean()) {
                        option.set(optionElement.getAsBoolean());
                    }
                } catch (Exception e) {
                    System.err.println("Ошибка при загрузке опции " + option.getName() + ": " + e.getMessage());
                }
            });
        } catch (Exception e) {
            System.err.println("Ошибка при загрузке ModeListSetting " + setting.getName() + ": " + e.getMessage());
        }
    }

    private void loadSettingFromJson(JsonObject jsonObject, String key, Consumer<JsonElement> consumer) {
        try {
            JsonElement element = jsonObject.get(key);
            if (element != null && !element.isJsonNull()) {
                consumer.accept(element);
            }
        } catch (Exception e) {
            System.err.println("Ошибка при загрузке настройки " + key + ": " + e.getMessage());
        }
    }

    public JsonElement saveConfig() {
        JsonObject functionsObject = new JsonObject();

        saveFunctionSettings(functionsObject);

        JsonObject newObject = new JsonObject();
        newObject.add("modules", functionsObject);

        JsonObject swapItemsObject = new JsonObject();
        saveSwapItems(swapItemsObject);
        newObject.add("swapItems", swapItemsObject);

        return newObject;
    }

    private void saveSwapItems(JsonObject swapItemsObject) {
        for (int i = 0; i < AutoSwap.threeItems.length; i++) {
            ItemStack stack = AutoSwap.threeItems[i];
            if (stack != null && !stack.isEmpty()) {
                CompoundNBT nbt = stack.write(new CompoundNBT());
                swapItemsObject.addProperty("slot" + i, nbt.toString());
            }
        }
    }

    private void saveFunctionSettings(JsonObject functionsObject) {
        Harmony.getInstance().getModuleManager().getModules().forEach(module -> {
            JsonObject moduleObject = new JsonObject();

            moduleObject.addProperty("bind", module.getBind());
            moduleObject.addProperty("state", module.isState());

            module.getSettings().forEach(setting -> saveIndividualSetting(moduleObject, setting));

            functionsObject.add(module.getName().toLowerCase(), moduleObject);
        });
    }

    private void saveIndividualSetting(JsonObject moduleObject, Setting<?> setting) {
        if (setting instanceof BooleanSetting) {
            moduleObject.addProperty(setting.getConfigKey(), ((BooleanSetting) setting).get());
        } else if (setting instanceof SliderSetting) {
            moduleObject.addProperty(setting.getConfigKey(), ((SliderSetting) setting).get());
        } else if (setting instanceof ModeSetting) {
            moduleObject.addProperty(setting.getConfigKey(), ((ModeSetting) setting).get());
        } else if (setting instanceof ColorSetting) {
            moduleObject.addProperty(setting.getConfigKey(), ((ColorSetting) setting).get());
        } else if (setting instanceof BindSetting) {
            moduleObject.addProperty(setting.getConfigKey(), ((BindSetting) setting).get());
        } else if (setting instanceof StringSetting) {
            moduleObject.addProperty(setting.getConfigKey(), ((StringSetting) setting).get());
        } else if (setting instanceof ModeListSetting) {
            saveModeListSetting(moduleObject, (ModeListSetting) setting);
        }
    }

    private void saveModeListSetting(JsonObject moduleObject, ModeListSetting setting) {
        JsonObject elements = new JsonObject();
        setting.get().forEach(option -> elements.addProperty(option.getName(), option.get()));
        moduleObject.add(setting.getConfigKey(), elements);
    }
}
