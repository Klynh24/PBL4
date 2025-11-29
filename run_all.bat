@echo off
echo ========================================
echo Starting 3-Tier Tutoring System
echo ========================================
echo.

REM Check if build outputs exist
if not exist "tier3-core-server\out\server\CoreServer.class" (
    echo [ERROR] Tier 3 Core Server not compiled!
    echo Please run build_all.bat first.
    pause
    exit /b 1
)

if not exist "tier2-proxy-server\target\proxy-server-1.0.0.jar" (
    echo [ERROR] Tier 2 Proxy Server JAR not found!
    echo Please run build_all.bat first.
    pause
    exit /b 1
)

echo Starting servers in separate console windows...
echo.

REM ============================================
REM Tier 3: Core Server (Start first)
REM ============================================
echo [1/3] Starting Tier 3 Core Server...
start "Tier 3 - Core Server" cmd /k "cd /d %~dp0tier3-core-server && java -cp out server.CoreServer"

REM Wait a bit for Core Server to initialize
timeout /t 3 /nobreak >nul

REM ============================================
REM Tier 2: Proxy Server (Start after Core)
REM ============================================
echo [2/3] Starting Tier 2 Proxy Server...
start "Tier 2 - Proxy Server" cmd /k "cd /d %~dp0tier2-proxy-server && java -jar target\proxy-server-1.0.0.jar"

REM Wait a bit for Proxy Server to initialize
timeout /t 2 /nobreak >nul

REM ============================================
REM Tier 1: Web Client (Node.js HTTP Server)
REM ============================================
echo [3/3] Starting Tier 1 Web Client Server (Node.js HTTP)...
echo Note: Requires Node.js installed. If Node.js is not available, skip this step.
echo.

REM Check if Node.js is available
node --version >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    start "Tier 1 - Web Client Server" cmd /k "cd /d %~dp0tier1-web-client && call npx http-server"
    echo [SUCCESS] Web Client Server started on http://localhost:8081
) else (
    echo [SKIP] Node.js not found. Web client server not started.
    echo You can manually start it with: cd tier1-web-client && npx http-server
)

echo.
echo ========================================
echo All servers started!
echo ========================================
pause

