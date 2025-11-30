#!/bin/bash

# Script to run Android instrumented tests for ThumbnailGenerator
# These tests run on a real Android device/emulator with full BitmapFactory support

set -e

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║  Android Instrumented Tests for ThumbnailGenerator          ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""

# Check if adb is available
if ! command -v adb &> /dev/null; then
    echo "❌ Error: adb not found!"
    echo "   Please install Android SDK and add adb to your PATH"
    echo "   export ANDROID_HOME=\$HOME/Library/Android/sdk"
    echo "   export PATH=\$PATH:\$ANDROID_HOME/platform-tools"
    exit 1
fi

# Check for connected devices
echo "🔍 Checking for connected devices..."
DEVICES=$(adb devices | grep -v "List" | grep "device$" | wc -l | tr -d ' ')

if [ "$DEVICES" -eq 0 ]; then
    echo "❌ No Android devices/emulators detected!"
    echo ""
    echo "Please start an emulator or connect a device:"
    echo "  1. Using Android Studio: Device Manager → Start Emulator"
    echo "  2. Using command line: emulator -avd <avd-name>"
    echo ""
    echo "Available AVDs:"
    if command -v emulator &> /dev/null; then
        emulator -list-avds
    else
        echo "  (emulator command not found)"
    fi
    exit 1
fi

echo "✅ Found $DEVICES device(s)"
adb devices | grep "device$"
echo ""

# Run the tests
echo "🧪 Running instrumented tests..."
echo "   This will build and install the test APK on the device"
echo ""

./gradlew :composeApp:connectedAndroidTest

# Check result
if [ $? -eq 0 ]; then
    echo ""
    echo "╔══════════════════════════════════════════════════════════════╗"
    echo "║  ✅ All tests passed!                                        ║"
    echo "╚══════════════════════════════════════════════════════════════╝"
    echo ""
    echo "📊 Test report: composeApp/build/reports/androidTests/connected/index.html"
else
    echo ""
    echo "╔══════════════════════════════════════════════════════════════╗"
    echo "║  ❌ Some tests failed!                                       ║"
    echo "╚══════════════════════════════════════════════════════════════╝"
    echo ""
    echo "📊 Test report: composeApp/build/reports/androidTests/connected/index.html"
    exit 1
fi

