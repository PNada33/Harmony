@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

set PROJECT_DIR=E:\Мои Сурсы\harmony
set LIB_DIR=%PROJECT_DIR%\libraries
set OUT_DIR=%PROJECT_DIR%\out\production\client
set NATIVES_DIR=%LIB_DIR%\natives

set CLASSPATH=%OUT_DIR%
for %%j in ("%LIB_DIR%\*.jar") do (
    echo %%j | find /i "voicechat" >nul
    if errorlevel 1 set CLASSPATH=!CLASSPATH!;%%j
)

if "%1"=="" (
    echo Usage: start_bot.bat ^<nickname^> [server] [port]
    echo Example: start_bot.bat Bot1 localhost 25565
    pause
    exit /b 1
)

set NICK=%1
set SERVER=%2
set PORT=%3
if "%SERVER%"=="" set SERVER=localhost
if "%PORT%"=="" set PORT=25565

start "Harmony-Bot-%NICK%" javaw.exe ^
    -cp "%CLASSPATH%" ^
    -Xmx1G -Xms512M ^
    -Djava.library.path="%NATIVES_DIR%" ^
    -Dbot.mode=true ^
    -Dvoicechat.disable=true ^
    -Dbot.width=400 ^
    -Dbot.height=300 ^
    -Dbot.nick=%NICK% ^
    Start ^
    --server %SERVER% --port %PORT%

echo Launched bot %NICK% connecting to %SERVER%:%PORT%
