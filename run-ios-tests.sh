#!/bin/bash
# Run iOS Simulator tests directly, bypassing Gradle's broken test result collector
# This script compiles the test executable with Gradle, then runs it directly on the iOS Simulator

set -e

echo "Building iOS test executable..."
./gradlew :composeApp:linkDebugTestIosSimulatorArm64 --no-configuration-cache --quiet

echo ""
echo "Running tests on iOS Simulator..."
echo "================================="

# Run the test executable directly on the simulator
xcrun simctl spawn booted \
  composeApp/build/bin/iosSimulatorArm64/debugTest/test.kexe

echo ""
echo "================================="
echo "iOS tests completed!"
