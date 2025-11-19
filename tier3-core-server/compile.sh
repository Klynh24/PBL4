#!/bin/bash

echo "Compiling Tier 3 Core Server..."

# Create bin directory if it doesn't exist
mkdir -p bin

# Compile Java files
javac -d bin -sourcepath src src/com/tutoring/core/*.java

if [ $? -eq 0 ]; then
    echo "Compilation successful!"
else
    echo "Compilation failed!"
    exit 1
fi

