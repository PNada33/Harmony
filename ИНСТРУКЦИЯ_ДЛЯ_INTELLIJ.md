# Конфигурация запуска для IntelliJ IDEA

## Основной класс для запуска: xd.harm.Start

### Настройки запуска:
- **Main class:** `xd.harm.Start`
- **VM options:** `-Dfile.encoding=UTF-8`
- **Program arguments:** (пусто)
- **Working directory:** `E:\Мои Сурсы\harmony`
- **Use classpath of module:** `client`

### Альтернативные варианты:
1. **Тестовый запуск:** `xd.harm.StartFixed` - для проверки исправлений
2. **Упрощенный запуск:** `xd.harm.StartSimple` - минимальная версия

### Classpath должен включать:
- `src/` - исходники
- `libraries/*` - все библиотеки

### Команда для ручного запуска:
```bash
"C:\Users\Mishka\.jdks\ms-17.0.19\bin\java.exe" -Dfile.encoding=UTF-8 -classpath "src;libraries/*" xd.harm.Start
```

### Что делает Start.java:
1. Проверяет наличие папки с аватарами
2. Подтверждает исправления в FiguraCosmetic и FiguraWear
3. Готовит мод к использованию в Minecraft

### Важно:
- Удалите все дублирующиеся файлы StartFixed.java
- Используйте только один основной класс xd.harm.Start
- Обновите конфигурацию в IntelliJ IDEA