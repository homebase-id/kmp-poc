#!/bin/bash

# Exit on error
set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Running KMP Tests for All Platforms${NC}"
echo -e "${BLUE}========================================${NC}\n"

# Desktop/JVM Tests
echo -e "${BLUE}[1/3] Running Desktop/JVM Tests...${NC}"
./gradlew desktopTest
echo -e "${GREEN}✓ Desktop tests completed${NC}\n"

# Android Unit Tests
echo -e "${BLUE}[2/3] Running Android Unit Tests...${NC}"
./gradlew testDebugUnitTest
echo -e "${GREEN}✓ Android tests completed${NC}\n"

# iOS Simulator Tests (only on macOS)
echo -e "${BLUE}[3/3] Running iOS Simulator Tests...${NC}"
if [[ "$OSTYPE" == "darwin"* ]]; then
    ./gradlew iosSimulatorArm64Test
    echo -e "${GREEN}✓ iOS tests completed${NC}\n"
else
    ./gradlew compileTestKotlinIosSimulatorArm64
    echo -e "${GREEN}✓ iOS tests compiled (execution skipped - requires macOS)${NC}\n"
fi

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}All platform tests passed!${NC}"
echo -e "${GREEN}========================================${NC}"
