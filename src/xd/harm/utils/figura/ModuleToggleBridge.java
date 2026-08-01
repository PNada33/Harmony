package xd.harm.utils.figura;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import xd.harm.Harmony;

/**
 * Мост к обычным модулям Harmony для карточек Figura Cosmetic.
 *
 * Ищет модуль по его имени ({@code @ModuleRegister(name = ...)}) в ModuleManager
 * и умеет включать/выключать его. Всё через рефлексию, чтобы не зависеть от
 * конкретных имён методов в базовом классе Module.
 */
public final class ModuleToggleBridge {

    private static final Map<String, Object> CACHE = new HashMap<String, Object>();

    private ModuleToggleBridge() {
    }

    /** Находит модуль по имени. */
    public static Object module(String name) {
        if (name == null) {
            return null;
        }
        Object cached = CACHE.get(name.toLowerCase());
        if (cached != null) {
            return cached;
        }
        try {
            Object manager = Harmony.getInstance().getModuleManager();
            if (manager == null) {
                return null;
            }
            Object raw = manager.getClass().getMethod("getModules").invoke(manager);
            if (!(raw instanceof List)) {
                return null;
            }
            List<?> modules = (List<?>) raw;
            for (int i = 0; i < modules.size(); i++) {
                Object module = modules.get(i);
                if (module == null) {
                    continue;
                }
                String moduleName = null;
                try {
                    Object value = module.getClass().getMethod("getName").invoke(module);
                    moduleName = value == null ? null : value.toString();
                } catch (Throwable ignored) {
                }
                if (moduleName == null) {
                    moduleName = module.getClass().getSimpleName();
                }
                if (moduleName.equalsIgnoreCase(name)) {
                    CACHE.put(name.toLowerCase(), module);
                    return module;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /** Есть ли такой модуль в сборке. */
    public static boolean available(String name) {
        return module(name) != null;
    }

    /** Включён ли модуль. */
    public static boolean isEnabled(String name) {
        Object module = module(name);
        if (module == null) {
            return false;
        }
        try {
            Object value = module.getClass().getMethod("isState").invoke(module);
            return value instanceof Boolean && ((Boolean) value).booleanValue();
        } catch (Throwable ignored) {
        }
        Boolean field = readBooleanField(module);
        return field != null && field.booleanValue();
    }

    /** Включает или выключает модуль. */
    public static void setEnabled(String name, boolean enabled) {
        Object module = module(name);
        if (module == null || isEnabled(name) == enabled) {
            return;
        }

        // Сначала пробуем явные сеттеры состояния.
        String[] setters = {"setState", "setEnabled", "setToggled"};
        for (int i = 0; i < setters.length; i++) {
            if (invokeBoolean(module, setters[i], enabled) && isEnabled(name) == enabled) {
                return;
            }
        }

        // Потом — enable()/disable().
        if (invokeVoid(module, enabled ? "enable" : "disable") && isEnabled(name) == enabled) {
            return;
        }

        // Потом — обычный toggle().
        if (invokeVoid(module, "toggle") && isEnabled(name) == enabled) {
            return;
        }

        // В крайнем случае — пишем поле состояния и дёргаем onEnable/onDisable.
        writeBooleanField(module, enabled);
        invokeVoid(module, enabled ? "onEnable" : "onDisable");
    }

    /** Переключает модуль. */
    public static void toggle(String name) {
        setEnabled(name, !isEnabled(name));
    }

    private static boolean invokeBoolean(Object target, String method, boolean value) {
        try {
            Method m = target.getClass().getMethod(method, boolean.class);
            m.setAccessible(true);
            m.invoke(target, Boolean.valueOf(value));
            return true;
        } catch (Throwable ignored) {
        }
        try {
            Method m = target.getClass().getMethod(method, Boolean.class);
            m.setAccessible(true);
            m.invoke(target, Boolean.valueOf(value));
            return true;
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static boolean invokeVoid(Object target, String method) {
        Class<?> type = target.getClass();
        while (type != null && type != Object.class) {
            try {
                Method m = type.getDeclaredMethod(method);
                m.setAccessible(true);
                m.invoke(target);
                return true;
            } catch (Throwable ignored) {
            }
            type = type.getSuperclass();
        }
        return false;
    }

    private static Field stateField(Object target) {
        String[] names = {"state", "enabled", "toggled", "active"};
        Class<?> type = target.getClass();
        while (type != null && type != Object.class) {
            for (int i = 0; i < names.length; i++) {
                try {
                    Field field = type.getDeclaredField(names[i]);
                    if (field.getType() == boolean.class || field.getType() == Boolean.class) {
                        field.setAccessible(true);
                        return field;
                    }
                } catch (Throwable ignored) {
                }
            }
            type = type.getSuperclass();
        }
        return null;
    }

    private static Boolean readBooleanField(Object target) {
        Field field = stateField(target);
        if (field == null) {
            return null;
        }
        try {
            Object value = field.get(target);
            return value instanceof Boolean ? (Boolean) value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void writeBooleanField(Object target, boolean value) {
        Field field = stateField(target);
        if (field == null) {
            return;
        }
        try {
            field.set(target, Boolean.valueOf(value));
        } catch (Throwable ignored) {
        }
    }
}
