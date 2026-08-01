package xd.harm.utils.figura;

/**
 * Совместимость: раньше этот класс через рефлексию дёргал модуль Pet
 * из ClickGUI. Модуля больше нет — всё живёт в FiguraPetController,
 * а этот класс оставлен как тонкая обёртка для Figura Cosmetic.
 */
public final class PetModuleBridge {

    private PetModuleBridge() {
    }

    private static FiguraPetController pets() {
        return FiguraPetController.get();
    }

    /** Питомцы всегда доступны: они часть Figura Cosmetic. */
    public static boolean available() {
        return true;
    }

    public static boolean isEnabled() {
        try {
            return pets().isActive();
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void toggle() {
        try {
            pets().toggle();
        } catch (Throwable ignored) {
        }
    }

    public static void setEnabled(boolean state) {
        try {
            pets().setActive(state);
        } catch (Throwable ignored) {
        }
    }

    /** Список питомцев для карточек во вкладке «Петы». */
    public static String[] petNames() {
        try {
            return pets().names();
        } catch (Throwable ignored) {
            return new String[0];
        }
    }

    public static String currentPet() {
        try {
            return pets().current();
        } catch (Throwable ignored) {
            return "";
        }
    }

    public static void selectPet(String name) {
        try {
            pets().select(name);
        } catch (Throwable ignored) {
        }
    }

    /** Вызвать конкретного питомца одним действием. */
    public static void summon(String name) {
        try {
            pets().summon(name);
        } catch (Throwable ignored) {
        }
    }

    public static boolean isSummoned(String name) {
        try {
            return pets().isSummoned(name);
        } catch (Throwable ignored) {
            return false;
        }
    }
}
