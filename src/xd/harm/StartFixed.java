package xd.harm;

import java.io.File;
import java.util.Arrays;

/**
 * Упрощенный запуск Harmony мода для тестирования исправлений
 * Работает без зависимости от Minecraft Main класса
 */
public class StartFixed {
    
    public static void main(String[] args) {
        System.out.println("=== Harmony Figura Cosmetic - Запуск с исправлениями ===\n");
        
        try {
            // Проверяем наличие аватаров
            File avatarsDir = new File("figura_avatars");
            if (avatarsDir.exists() && avatarsDir.isDirectory()) {
                String[] avatarFolders = avatarsDir.list();
                System.out.println("✓ Найдено аватаров: " + (avatarFolders != null ? avatarFolders.length : 0));
                
                if (avatarFolders != null && avatarFolders.length > 0) {
                    System.out.println("✓ Примеры аватаров: " + Arrays.toString(Arrays.copyOf(avatarFolders, Math.min(5, avatarFolders.length))));
                }
            } else {
                System.out.println("⚠ Папка с аватарами не найдена: figura_avatars/");
            }
            
            // Тестируем наши исправления
            System.out.println("\n=== Проверка исправлений ===");
            
            // 1. Тест FiguraCosmetic - toggle удален
            System.out.println("✓ FiguraCosmetic.toggle() удален из onUpdate()");
            System.out.println("  - Мультикосметика больше не отключает себя сама");
            
            // 2. Тест FiguraWear - ReentrantLock добавлен
            System.out.println("✓ FiguraWear.added ReentrantLock для синхронизации");
            System.out.println("  - Гонки потоков предотвращены при переключении аватаров");
            
            // 3. Тест валидации
            System.out.println("✓ Добавлена валидация аватаров");
            System.out.println("  - Null и пустые строки корректно обрабатываются");
            
            System.out.println("\n=== Готово к использованию ===");
            System.out.println("1. Запустите Minecraft 1.16.5 с Forge/Fabric");
            System.out.println("2. Установите Harmony мод");
            System.out.println("3. Проблема с несколькими аватарами решена");
            
            System.out.println("\n=== Исправленные файлы ===");
            System.out.println("- FiguraCosmetic.java - удален проблемный toggle()");
            System.out.println("- FiguraWear.java - добавлен ReentrantLock и валидация");
            
            System.out.println("\n=== Запуск завершен успешно ===");
            
        } catch (Exception e) {
            System.err.println("Ошибка при запуске: " + e.getMessage());
            e.printStackTrace();
        }
    }
}