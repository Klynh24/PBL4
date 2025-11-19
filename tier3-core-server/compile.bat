@echo off
echo Compiling Tier 3 Core Server...
if not exist "bin" mkdir bin
javac -d bin -sourcepath src src\com\tutoring\core\*.java
if %ERRORLEVEL% EQU 0 (
    echo Compilation successful!
) else (
    echo Compilation failed!
)
pause

