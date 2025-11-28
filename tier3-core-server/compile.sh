#!/bin/bash

echo "Compiling Tier 3 Core Server..."

# Create bin directory if it doesn't exist
mkdir -p bin

# Compile Java files
javac -d bin -sourcepath src src/com/tutoring/core/server/*.java src/com/tutoring/core/management/*.java src/com/tutoring/core/model/*.java src/com/tutoring/core/streaming/*/*.java src/com/tutoring/core/concurrency/*/*.java src/com/tutoring/core/monitoring/*.java

if [ $? -eq 0 ]; then
    echo "Compilation successful!"
else
    echo "Compilation failed!"
    exit 1
fi

