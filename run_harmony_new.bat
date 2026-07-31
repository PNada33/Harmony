@echo off
echo === Harmony Figura Cosmetic - Запуск с исправлениями ===

REM Запускаем Java с правильным classpath
java -Dfile.encoding=UTF-8 -cp "src;libraries/*" xd.harm.Start

echo.
echo Запуск завершен.
pause