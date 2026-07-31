@echo off
echo === Проверка всех аватарок ===

REM Проверяем общее количество папок аватаров
echo.
echo Общее количество папок аватаров:
dir /b "figura_avatars" | find /c /v "" 

echo.
echo Список всех аватарок:
dir /b "figura_avatars"

echo.
echo Проверка структуры нескольких аватарок:
echo.

REM Проверяем несколько аватарок
if exist "figura_avatars\01a_Shizuko\avatar.json" (
    echo [OK] 01a_Shizuko - avatar.json найден
) else (
    echo [FAIL] 01a_Shizuko - avatar.json отсутствует
)

if exist "figura_avatars\06a_Shiroko\avatar.png" (
    echo [OK] 06a_Shiroko - avatar.png найден
) else (
    echo [FAIL] 06a_Shiroko - avatar.png отсутствует
)

if exist "figura_avatars\13a_Aris\models\" (
    echo [OK] 13a_Aris - папка models найдена
) else (
    echo [FAIL] 13a_Aris - папка models отсутствует
)

if exist "figura_avatars\20a_Hina\scripts\" (
    echo [OK] 20a_Hina - папка scripts найдена
) else (
    echo [FAIL] 20a_Hina - папка scripts отсутствует
)

echo.
echo === Проверка завершена ===
pause