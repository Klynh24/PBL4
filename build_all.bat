@echo off
echo ========================================
echo Building 3-Tier Tutoring System
echo ========================================
echo.

REM ============================================
REM Tier 3: Core Server (Pure Java)
REM ============================================
echo [1/2] Building Tier 3 Core Server...
echo.

cd tier3-core-server

REM Clean output directories
if exist "bin" (
    echo Cleaning bin directory...
    rmdir /s /q bin
)
if exist "out" (
    echo Cleaning out directory...
    rmdir /s /q out
)

REM Create output directory
if not exist "out" mkdir out
if not exist "bin" mkdir bin

REM Compile all Java files
echo Compiling Java source files...
echo Note: Using new package structure (server, management, streaming, etc.)
echo.

javac -d out -sourcepath src src\server\*.java src\management\*.java src\model\*.java src\streaming\protocol\*.java src\streaming\encoding\*.java src\streaming\quality\*.java src\streaming\network\*.java src\streaming\retransmission\*.java src\streaming\broadcast\*.java src\concurrency\pool\*.java src\concurrency\worker\*.java src\monitoring\*.java

if %ERRORLEVEL% EQU 0 (
    echo.
    echo [SUCCESS] Tier 3 Core Server compiled successfully!
    echo Output: tier3-core-server\out\
) else (
    echo.
    echo [ERROR] Tier 3 Core Server compilation failed!
    echo Please check the errors above.
    cd ..
    echo.
    echo ========================================
    echo BUILD FAILED - Check errors above
    echo ========================================
    pause
    exit /b 1
)

cd ..

echo.
echo ========================================
echo.

REM ============================================
REM Tier 2: Proxy Server (Maven)
REM ============================================
echo [2/2] Building Tier 2 Proxy Server...
echo.

cd tier2-proxy-server

REM Run Maven clean package
echo Running Maven clean package...
call mvn clean package -q

if %ERRORLEVEL% EQU 0 (
    echo.
    echo [SUCCESS] Tier 2 Proxy Server built successfully!
    echo Output: tier2-proxy-server\target\proxy-server-1.0.0.jar
) else (
    echo.
    echo [ERROR] Tier 2 Proxy Server build failed!
    cd ..
    echo.
    echo ========================================
    echo BUILD FAILED - Check errors above
    echo ========================================
    pause
    exit /b 1
)

cd ..

echo.
echo ========================================
echo BUILD COMPLETE - All tiers built successfully!
echo ========================================
echo.
echo Tier 3 Core Server: tier3-core-server\out\
echo Tier 2 Proxy Server: tier2-proxy-server\target\proxy-server-1.0.0.jar
echo.
pause

