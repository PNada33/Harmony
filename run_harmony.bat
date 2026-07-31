@echo off
echo === Harmony Figura Cosmetic - Запуск с исправлениями ===

REM Запускаем Java с правильным classpath
"C:\Users\Mishka\.jdks\ms-17.0.19\bin\java.exe" -Dfile.encoding=UTF-8 -classpath "src;libraries/*" xd.harm.Start

echo.
echo Запуск завершен.
pause