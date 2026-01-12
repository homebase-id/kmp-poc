---
description: Build FFmpeg iOS xcframeworks from source for KMP project
---

# Build FFmpeg iOS XCFrameworks from Source

This workflow builds FFmpeg and creates xcframeworks compatible with your Kotlin Multiplatform project.

## Prerequisites

```bash
# Install Xcode Command Line Tools
xcode-select --install

# Install dependencies via Homebrew
brew install yasm nasm pkg-config automake libtool
```

// turbo-all

## Build iOS XCFrameworks

```bash
cd /Users/biswa/Documents/GitHub/kmp-poc
./scripts/build-ffmpeg-ios.sh
```

Build takes 15-30 minutes. Output: `composeApp/libs/ffmpeg.xcframework/`

## Build Android Libraries (requires NDK)

```bash
# Set NDK path if not auto-detected
export ANDROID_NDK_HOME=~/Library/Android/sdk/ndk/<version>

cd /Users/biswa/Documents/GitHub/kmp-poc
./scripts/build-ffmpeg-android.sh
```

Build takes 20-40 minutes. Output: `composeApp/src/androidMain/jniLibs/`

## Verify Builds

```bash
# Check iOS xcframeworks
ls -la composeApp/libs/ffmpeg.xcframework/

# Check Android .so files
ls -la composeApp/src/androidMain/jniLibs/arm64-v8a/

# Build KMP project
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
./gradlew :composeApp:assembleDebug
```

## Clean Rebuild

```bash
./scripts/build-ffmpeg-ios.sh --clean
./scripts/build-ffmpeg-android.sh --clean
```

## Files Created

- `scripts/build-ffmpeg-ios.sh` - iOS build script
- `scripts/build-ffmpeg-android.sh` - Android build script
- `composeApp/src/androidMain/jni/ffmpeg_jni.c` - JNI C wrapper
- `composeApp/src/androidMain/jni/CMakeLists.txt` - CMake config
- `composeApp/src/androidMain/kotlin/.../FFmpegNative.kt` - Kotlin JNI bindings
- `composeApp/src/nativeInterop/cinterop/ffmpeg.def` - iOS cinterop definition
