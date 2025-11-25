#!/bin/bash
# Run iOS Simulator tests for CI/CD (captures exit codes and failures)
# This script compiles the test executable with Gradle, then runs it directly on the iOS Simulator

set -e

echo "Building iOS test executable..."
./gradlew :composeApp:linkDebugTestIosSimulatorArm64 --no-configuration-cache --quiet

echo ""
echo "Running tests on iOS Simulator..."
echo "================================="

# Run the test executable and capture output + exit code
TEST_OUTPUT=$(mktemp)
if xcrun simctl spawn booted \
  composeApp/build/bin/iosSimulatorArm64/debugTest/test.kexe \
  2>&1 | tee "$TEST_OUTPUT"; then

  # Check if all tests passed
  if grep -q "\[  PASSED  \]" "$TEST_OUTPUT" && ! grep -q "\[  FAILED  \]" "$TEST_OUTPUT"; then
    echo ""
    echo "================================="
    echo "✅ All iOS tests PASSED!"
    rm "$TEST_OUTPUT"
    exit 0
  fi
fi

# Tests failed
echo ""
echo "================================="
echo "❌ iOS tests FAILED!"
rm "$TEST_OUTPUT"
exit 1
