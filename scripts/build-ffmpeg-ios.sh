#!/bin/bash
# Build FFmpeg for iOS (arm64 device + arm64 simulator) and create xcframeworks
# Usage: ./build-ffmpeg-ios.sh [--clean]
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
BUILD_DIR="$PROJECT_ROOT/.ffmpeg-build-ios"
OUTPUT_DIR="$PROJECT_ROOT/composeApp/libs/ffmpeg.xcframework"
FFMPEG_VERSION="n7.1"
MIN_IOS_VERSION="16.0"

# FFmpeg libraries to build
LIBS=("libavcodec" "libavformat" "libavutil" "libswresample" "libswscale" "libavfilter")

echo "=============================================="
echo "FFmpeg iOS Build Script"
echo "=============================================="
echo "Build dir: $BUILD_DIR"
echo "Output dir: $OUTPUT_DIR"
echo "FFmpeg version: $FFMPEG_VERSION"
echo ""

# Clean if requested
if [ "$1" == "--clean" ]; then
    echo "Cleaning previous builds..."
    rm -rf "$BUILD_DIR"
    rm -rf "$OUTPUT_DIR"
fi

# Check prerequisites
command -v yasm >/dev/null 2>&1 || { echo "Error: yasm not found. Install with: brew install yasm"; exit 1; }
command -v nasm >/dev/null 2>&1 || { echo "Error: nasm not found. Install with: brew install nasm"; exit 1; }
command -v pkg-config >/dev/null 2>&1 || { echo "Error: pkg-config not found. Install with: brew install pkg-config"; exit 1; }

mkdir -p "$BUILD_DIR"
cd "$BUILD_DIR"

# Use shared FFmpeg source location (shared between iOS and Android builds)
SHARED_FFMPEG_SRC="$PROJECT_ROOT/.ffmpeg-source"

# Clone FFmpeg to shared location if not present
if [ -d "$SHARED_FFMPEG_SRC" ]; then
    echo "Using existing FFmpeg source at $SHARED_FFMPEG_SRC"
else
    echo "Cloning FFmpeg $FFMPEG_VERSION to shared location..."
    git clone --depth 1 --branch "$FFMPEG_VERSION" https://git.ffmpeg.org/ffmpeg.git "$SHARED_FFMPEG_SRC"
fi

FFMPEG_SRC="$SHARED_FFMPEG_SRC"

# Build function for each architecture
build_ffmpeg() {
    local ARCH=$1
    local SDK=$2
    # Use SDK in directory names to differentiate device from simulator
    local PLATFORM_SUFFIX=$([ "$SDK" = "iphonesimulator" ] && echo "-simulator" || echo "")
    local BUILD_SUBDIR="$BUILD_DIR/build-$ARCH$PLATFORM_SUFFIX"
    local INSTALL_DIR="$BUILD_DIR/install-$ARCH$PLATFORM_SUFFIX"
    
    echo ""
    echo "=============================================="
    echo "Building FFmpeg for $ARCH ($SDK)"
    echo "=============================================="
    
    rm -rf "$BUILD_SUBDIR"
    mkdir -p "$BUILD_SUBDIR"
    cd "$BUILD_SUBDIR"
    
    SYSROOT=$(xcrun --sdk "$SDK" --show-sdk-path)
    CC="xcrun --sdk $SDK clang"
    
    if [ "$SDK" = "iphonesimulator" ]; then
        EXTRA_CFLAGS="-arch $ARCH -mios-simulator-version-min=$MIN_IOS_VERSION -isysroot $SYSROOT"
        EXTRA_LDFLAGS="-arch $ARCH -mios-simulator-version-min=$MIN_IOS_VERSION -isysroot $SYSROOT"
    else
        EXTRA_CFLAGS="-arch $ARCH -mios-version-min=$MIN_IOS_VERSION -isysroot $SYSROOT -fembed-bitcode"
        EXTRA_LDFLAGS="-arch $ARCH -mios-version-min=$MIN_IOS_VERSION -isysroot $SYSROOT"
    fi
    
    "$FFMPEG_SRC/configure" \
        --prefix="$INSTALL_DIR" \
        --enable-cross-compile \
        --arch="$ARCH" \
        --target-os=darwin \
        --sysroot="$SYSROOT" \
        --cc="$CC" \
        --extra-cflags="$EXTRA_CFLAGS" \
        --extra-ldflags="$EXTRA_LDFLAGS" \
        --enable-gpl \
        --enable-nonfree \
        --disable-programs \
        --disable-doc \
        --disable-debug \
        --disable-avdevice \
        --disable-postproc \
        --enable-pic \
        --enable-static \
        --disable-shared \
        --disable-asm \
        --disable-x86asm \
        --enable-videotoolbox \
        --enable-audiotoolbox
    
    make clean 2>/dev/null || true
    make -j$(sysctl -n hw.ncpu)
    make install
    
    echo "Installed to $INSTALL_DIR"
}

# Build for device and simulator
build_ffmpeg "arm64" "iphoneos"
build_ffmpeg "arm64" "iphonesimulator"

echo ""
echo "=============================================="
echo "Creating XCFrameworks"
echo "=============================================="

rm -rf "$OUTPUT_DIR"
mkdir -p "$OUTPUT_DIR"

# Create xcframework for each library
for LIB in "${LIBS[@]}"; do
    echo "Creating $LIB.xcframework..."
    
    # Build xcframework from static libraries
    # Note: Headers are in include/libavcodec/, include/libavutil/, etc.
    xcodebuild -create-xcframework \
        -library "$BUILD_DIR/install-arm64/lib/$LIB.a" \
        -headers "$BUILD_DIR/install-arm64/include/$LIB" \
        -library "$BUILD_DIR/install-arm64-simulator/lib/$LIB.a" \
        -headers "$BUILD_DIR/install-arm64-simulator/include/$LIB" \
        -output "$OUTPUT_DIR/$LIB.xcframework"
done

echo ""
echo "=============================================="
echo "Build Complete!"
echo "=============================================="
echo "XCFrameworks created in: $OUTPUT_DIR"
ls -la "$OUTPUT_DIR"
echo ""
echo "Next steps:"
echo "1. Update composeApp/src/nativeInterop/cinterop/ffmpeg.def"
echo "2. Update composeApp/build.gradle.kts to use new paths"
echo "3. Run: ./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64"
