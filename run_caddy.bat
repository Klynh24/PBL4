@echo off
echo ========================================
echo Starting Caddy Web Server
echo ========================================
echo.

REM Check if Caddyfile exists
if not exist "Caddyfile" (
    echo [ERROR] Caddyfile not found!
    echo Please make sure Caddyfile exists in the current directory.
    pause
    exit /b 1
)

REM Check if caddy.exe exists
if not exist "caddy.exe" (
    echo [ERROR] caddy.exe not found!
    echo.
    echo Please download Caddy from: https://caddyserver.com/download
    echo Or use: winget install Caddy.Caddy
    echo.
    pause
    exit /b 1
)

REM Check if required directories exist
if not exist "D:\Ki1_Nam3\PBL4\Realtime\caddy_data" (
    echo [INFO] Creating Caddy data directory...
    mkdir "D:\Ki1_Nam3\PBL4\Realtime\caddy_data" 2>nul
)

if not exist "D:\Ki1_Nam3\PBL4\Realtime\logs" (
    echo [INFO] Creating logs directory...
    mkdir "D:\Ki1_Nam3\PBL4\Realtime\logs" 2>nul
)

echo [INFO] Starting Caddy server...
echo [INFO] Using Caddyfile: %CD%\Caddyfile
echo.
echo Caddy will:
echo   - Listen on https://192.168.98.93:443 (LAN)
echo   - Listen on https://toan-webrtc.ddns.net (Internet)
echo   - Reverse proxy WebSocket connections to localhost:8089
echo   - Serve static files from tier1-web-client
echo.
echo Press Ctrl+C to stop the server.
echo.

REM Run Caddy with Caddyfile
caddy run --config Caddyfile

pause

