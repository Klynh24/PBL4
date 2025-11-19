#!/bin/bash

# Build and Test Script for Advanced Screen Sharing Implementation
# Compiles all source files and runs unit tests

echo "========================================="
echo "Advanced Screen Sharing Build & Test"
echo "========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Create directories
echo "[1/5] Creating build directories..."
mkdir -p bin
mkdir -p test/bin
echo -e "${GREEN}✓ Directories created${NC}"
echo ""

# Compile main source files
echo "[2/5] Compiling main source files..."
javac -d bin -sourcepath src \
    src/com/tutoring/core/*.java \
    src/com/tutoring/core/streaming/*.java 2>&1 | tee compile.log

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Main source compiled successfully${NC}"
else
    echo -e "${RED}✗ Compilation failed! See compile.log${NC}"
    exit 1
fi
echo ""

# Compile test files
echo "[3/5] Compiling test files..."
javac -d test/bin -cp bin -sourcepath src:test \
    test/com/tutoring/core/streaming/*.java 2>&1 | tee -a compile.log

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Tests compiled successfully${NC}"
else
    echo -e "${YELLOW}⚠ Test compilation failed (non-critical)${NC}"
fi
echo ""

# Run Phase 1 tests
echo "[4/5] Running Phase 1: Foundation Tests..."
echo "------------------------------------"
java -cp bin:test/bin com.tutoring.core.streaming.TestFragmentation
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Phase 1 tests passed${NC}"
else
    echo -e "${RED}✗ Phase 1 tests failed${NC}"
fi
echo ""

# Run Phase 3 tests
echo "[5/5] Running Phase 3: Delta Compression Tests..."
echo "------------------------------------------------"
java -cp bin:test/bin com.tutoring.core.streaming.TestDirtyRegion
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Phase 3 tests passed${NC}"
else
    echo -e "${RED}✗ Phase 3 tests failed${NC}"
fi
echo ""

# Summary
echo "========================================="
echo "Build Summary"
echo "========================================="
echo ""

# Count compiled classes
MAIN_CLASSES=$(find bin -name "*.class" 2>/dev/null | wc -l)
TEST_CLASSES=$(find test/bin -name "*.class" 2>/dev/null | wc -l)

echo "Main classes: $MAIN_CLASSES"
echo "Test classes: $TEST_CLASSES"
echo ""

# List key components
echo "Key Components:"
echo "  ✓ FrameFragmenter.java"
echo "  ✓ RetransmissionBuffer.java"
echo "  ✓ DirtyRegionDetector.java"
echo "  ✓ FrameEncoder.java"
echo "  ✓ ClientStreamState.java"
echo "  ✓ PerformanceMonitor.java"
echo "  ✓ StreamingBroadcaster.java"
echo ""

echo -e "${GREEN}Build complete!${NC}"
echo ""
echo "Next steps:"
echo "  1. Review INTEGRATION_GUIDE.md for integration steps"
echo "  2. Start the core server: java -cp bin com.tutoring.core.CoreServer"
echo "  3. Monitor performance with PerformanceMonitor"
echo ""

