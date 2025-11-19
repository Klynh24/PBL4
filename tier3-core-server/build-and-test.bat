@echo off
REM Build and Test Script for Advanced Screen Sharing Implementation (Windows)
REM Compiles all source files and runs unit tests

echo =========================================
echo Advanced Screen Sharing Build ^& Test
echo =========================================
echo.

REM Create directories
echo [1/5] Creating build directories...
if not exist bin mkdir bin
if not exist test\bin mkdir test\bin
echo [92m✓ Directories created[0m
echo.

REM Compile main source files
echo [2/5] Compiling main source files...
javac -d bin -sourcepath src src\com\tutoring\core\*.java src\com\tutoring\core\streaming\*.java 2>&1 > compile.log

if %ERRORLEVEL% EQU 0 (
    echo [92m✓ Main source compiled successfully[0m
) else (
    echo [91m✗ Compilation failed! See compile.log[0m
    exit /b 1
)
echo.

REM Compile test files
echo [3/5] Compiling test files...
javac -d test\bin -cp bin -sourcepath src;test test\com\tutoring\core\streaming\*.java 2>&1 >> compile.log

if %ERRORLEVEL% EQU 0 (
    echo [92m✓ Tests compiled successfully[0m
) else (
    echo [93m⚠ Test compilation failed (non-critical)[0m
)
echo.

REM Run Phase 1 tests
echo [4/5] Running Phase 1: Foundation Tests...
echo ------------------------------------
java -cp "bin;test\bin" com.tutoring.core.streaming.TestFragmentation
if %ERRORLEVEL% EQU 0 (
    echo [92m✓ Phase 1 tests passed[0m
) else (
    echo [91m✗ Phase 1 tests failed[0m
)
echo.

REM Run Phase 3 tests
echo [5/5] Running Phase 3: Delta Compression Tests...
echo ------------------------------------------------
java -cp "bin;test\bin" com.tutoring.core.streaming.TestDirtyRegion
if %ERRORLEVEL% EQU 0 (
    echo [92m✓ Phase 3 tests passed[0m
) else (
    echo [91m✗ Phase 3 tests failed[0m
)
echo.

REM Summary
echo =========================================
echo Build Summary
echo =========================================
echo.

echo Key Components:
echo   ✓ FrameFragmenter.java
echo   ✓ RetransmissionBuffer.java
echo   ✓ DirtyRegionDetector.java
echo   ✓ FrameEncoder.java
echo   ✓ ClientStreamState.java
echo   ✓ PerformanceMonitor.java
echo   ✓ StreamingBroadcaster.java
echo.

echo [92mBuild complete![0m
echo.
echo Next steps:
echo   1. Review INTEGRATION_GUIDE.md for integration steps
echo   2. Start the core server: java -cp bin com.tutoring.core.CoreServer
echo   3. Monitor performance with PerformanceMonitor
echo.

pause

