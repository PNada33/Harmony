@echo off
setlocal
cd /d "%~dp0"

set HOST=0.0.0.0
set PORT=54321
set API_KEY=harmony-local-dev-key

for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":54321"') do (
  taskkill /PID %%p /F >nul 2>nul
)

start "Harmony Cloud Server" /D "%~dp0" cmd /k python local_cloud_server.py --host %HOST% --port %PORT% --api-key %API_KEY% --require-auth 1 --data-dir "%~dp0local-server-data"
timeout /t 1 /nobreak >nul
start "" "http://localhost:%PORT%/?v=%RANDOM%"
