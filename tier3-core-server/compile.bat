@echo off
echo Compiling Tier 3 Core Server...
if not exist "bin" mkdir bin
javac -d bin -sourcepath src src\server\*.java src\management\*.java src\model\*.java src\streaming\*\*.java src\concurrency\*\*.java src\monitoring\*.java
if %ERRORLEVEL% EQU 0 (
    echo Compilation successful!
) else (
    echo Compilation failed!
)
pause

