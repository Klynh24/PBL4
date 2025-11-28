@echo off
echo Compiling Tier 3 Core Server...
if not exist "bin" mkdir bin
javac -d bin -sourcepath src src\com\tutoring\core\server\*.java src\com\tutoring\core\management\*.java src\com\tutoring\core\model\*.java src\com\tutoring\core\streaming\*\*.java src\com\tutoring\core\concurrency\*\*.java src\com\tutoring\core\monitoring\*.java
if %ERRORLEVEL% EQU 0 (
    echo Compilation successful!
) else (
    echo Compilation failed!
)
pause

