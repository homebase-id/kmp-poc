#!/bin/bash
# Build FFmpeg for Android with NDK and create shared libraries
# Usage: ./build-ffmpeg-android.sh [--clean]
# Requires: Android NDK (set ANDROID_NDK_HOME or NDK_HOME environment variable)
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
BUILD_DIR="$PROJECT_ROOT/.ffmpeg-build-android"
OUTPUT_DIR="$PROJECT_ROOT/composeApp/libs/ffmpeg-android"
JNI_OUTPUT="$PROJECT_ROOT/composeApp/src/androidMain/jniLibs"
FFMPEG_VERSION="n7.1"
MIN_SDK=24

# Android ABIs to build
ABIS=("arm64-v8a" "armeabi-v7a" "x86_64")

echo "=============================================="
echo "FFmpeg Android Build Script"
echo "=============================================="

# Find NDK
NDK=""
if [ -n "$ANDROID_NDK_HOME" ] && [ -d "$ANDROID_NDK_HOME" ]; then
    NDK="$ANDROID_NDK_HOME"
elif [ -n "$NDK_HOME" ] && [ -d "$NDK_HOME" ]; then
    NDK="$NDK_HOME"
elif [ -d "$HOME/Library/Android/sdk/ndk" ]; then
    # Find latest NDK version by sorting numerically
    LATEST_NDK=$(ls "$HOME/Library/Android/sdk/ndk" | sort -V | tail -1)
    if [ -n "$LATEST_NDK" ]; then
        NDK="$HOME/Library/Android/sdk/ndk/$LATEST_NDK"
    fi
fi

if [ -z "$NDK" ] || [ ! -d "$NDK" ]; then
    echo "Error: Android NDK not found."
    echo "Available NDKs:"
    ls "$HOME/Library/Android/sdk/ndk" 2>/dev/null || echo "  None found"
    echo ""
    echo "Set ANDROID_NDK_HOME environment variable to the NDK path, e.g.:"
    echo "  export ANDROID_NDK_HOME=~/Library/Android/sdk/ndk/29.0.13113456"
    exit 1
fi

echo "Using NDK: $NDK"

# Detect host platform for toolchain
HOST_TAG=""
if [[ "$(uname)" == "Darwin" ]]; then
    HOST_TAG="darwin-x86_64"
elif [[ "$(uname)" == "Linux" ]]; then
    HOST_TAG="linux-x86_64"
else
    echo "Error: Unsupported host platform: $(uname)"
    exit 1
fi

TOOLCHAIN="$NDK/toolchains/llvm/prebuilt/$HOST_TAG"

if [ ! -d "$TOOLCHAIN" ]; then
    echo "Error: NDK toolchain not found at $TOOLCHAIN"
    echo "Make sure NDK is properly installed."
    exit 1
fi

echo "Toolchain: $TOOLCHAIN"
echo "Build dir: $BUILD_DIR"
echo "Output dir: $OUTPUT_DIR"
echo ""

# Clean if requested
if [ "$1" == "--clean" ]; then
    echo "Cleaning previous builds..."
    rm -rf "$BUILD_DIR"
    rm -rf "$OUTPUT_DIR"
    rm -rf "$JNI_OUTPUT"
fi

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

# Map ABI to architecture and target
get_arch() {
    case $1 in
        arm64-v8a) echo "aarch64" ;;
        armeabi-v7a) echo "arm" ;;
        x86_64) echo "x86_64" ;;
    esac
}

get_target() {
    case $1 in
        arm64-v8a) echo "aarch64-linux-android" ;;
        armeabi-v7a) echo "armv7a-linux-androideabi" ;;
        x86_64) echo "x86_64-linux-android" ;;
    esac
}

get_cpu() {
    case $1 in
        arm64-v8a) echo "armv8-a" ;;
        armeabi-v7a) echo "armv7-a" ;;
        x86_64) echo "x86-64" ;;
    esac
}

# Build function for each ABI
build_ffmpeg() {
    local ABI=$1
    local ARCH=$(get_arch "$ABI")
    local TARGET=$(get_target "$ABI")
    local CPU=$(get_cpu "$ABI")
    local BUILD_SUBDIR="$BUILD_DIR/build-$ABI"
    local INSTALL_DIR="$BUILD_DIR/install-$ABI"
    
    echo ""
    echo "=============================================="
    echo "Building FFmpeg for $ABI (arch=$ARCH)"
    echo "=============================================="
    
    rm -rf "$BUILD_SUBDIR"
    mkdir -p "$BUILD_SUBDIR"
    cd "$BUILD_SUBDIR"
    
    local CC="$TOOLCHAIN/bin/${TARGET}${MIN_SDK}-clang"
    local CXX="$TOOLCHAIN/bin/${TARGET}${MIN_SDK}-clang++"
    local AR="$TOOLCHAIN/bin/llvm-ar"
    local STRIP="$TOOLCHAIN/bin/llvm-strip"
    
    # Fix for armv7
    if [ "$ABI" == "armeabi-v7a" ]; then
        CC="$TOOLCHAIN/bin/armv7a-linux-androideabi${MIN_SDK}-clang"
        CXX="$TOOLCHAIN/bin/armv7a-linux-androideabi${MIN_SDK}-clang++"
    fi
    
    local EXTRA_CFLAGS="-O3 -fPIC"
    # 16KB page size support for Android 15+ (required for arm64)
    # See: https://developer.android.com/guide/practices/page-sizes
    local EXTRA_LDFLAGS="-Wl,-z,max-page-size=16384"
    
    "$FFMPEG_SRC/configure" \
        --prefix="$INSTALL_DIR" \
        --enable-cross-compile \
        --arch="$ARCH" \
        --cpu="$CPU" \
        --target-os=android \
        --cc="$CC" \
        --cxx="$CXX" \
        --ar="$AR" \
        --strip="$STRIP" \
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
        --enable-shared \
        --disable-static \
        --disable-asm \
        --enable-jni \
        --enable-mediacodec \
        --enable-neon
    
    make clean 2>/dev/null || true
    make -j$(nproc 2>/dev/null || sysctl -n hw.ncpu)
    make install
    
    echo "Installed to $INSTALL_DIR"
}

# Build for all ABIs
for ABI in "${ABIS[@]}"; do
    build_ffmpeg "$ABI"
done

echo ""
echo "=============================================="
echo "Copying libraries to jniLibs"
echo "=============================================="

rm -rf "$JNI_OUTPUT"
mkdir -p "$JNI_OUTPUT"

# Copy shared libraries for each ABI
for ABI in "${ABIS[@]}"; do
    mkdir -p "$JNI_OUTPUT/$ABI"
    cp "$BUILD_DIR/install-$ABI/lib/"*.so "$JNI_OUTPUT/$ABI/"
    echo "Copied .so files to $JNI_OUTPUT/$ABI/"
done

# Also copy to output dir with headers for JNI compilation
rm -rf "$OUTPUT_DIR"
mkdir -p "$OUTPUT_DIR"

for ABI in "${ABIS[@]}"; do
    mkdir -p "$OUTPUT_DIR/$ABI"
    cp -R "$BUILD_DIR/install-$ABI/lib" "$OUTPUT_DIR/$ABI/"
    cp -R "$BUILD_DIR/install-$ABI/include" "$OUTPUT_DIR/$ABI/"
done

echo ""
echo "=============================================="
echo "Build Complete!"
echo "=============================================="
echo "Shared libraries in: $JNI_OUTPUT"
echo "Full output (with headers) in: $OUTPUT_DIR"
ls -la "$JNI_OUTPUT"
echo ""
echo "JNI wrapper and CMakeLists.txt already exist in:"
echo "  composeApp/src/androidMain/jni/"
echo ""
echo "To verify: ./gradlew :composeApp:assembleDebug"
