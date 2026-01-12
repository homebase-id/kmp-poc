# CMakeLists.txt for FFmpeg JNI wrapper
cmake_minimum_required(VERSION 3.22.1)
project(ffmpeg_jni)

# Set C standard
set(CMAKE_C_STANDARD 11)

# FFmpeg library paths (relative to this CMakeLists.txt)
set(FFMPEG_DIR "${CMAKE_SOURCE_DIR}/../../../libs/ffmpeg-android/${ANDROID_ABI}")
set(FFMPEG_INCLUDE_DIR "${FFMPEG_DIR}/include")
set(FFMPEG_LIB_DIR "${FFMPEG_DIR}/lib")

# Include FFmpeg headers
include_directories(${FFMPEG_INCLUDE_DIR})

# Define the shared library
add_library(ffmpeg_jni SHARED
    ffmpeg_jni.c
)

# Link against Android log library
find_library(log-lib log)

# Add FFmpeg libraries as imported shared libraries
add_library(avcodec SHARED IMPORTED)
set_target_properties(avcodec PROPERTIES IMPORTED_LOCATION ${FFMPEG_LIB_DIR}/libavcodec.so)

add_library(avformat SHARED IMPORTED)
set_target_properties(avformat PROPERTIES IMPORTED_LOCATION ${FFMPEG_LIB_DIR}/libavformat.so)

add_library(avutil SHARED IMPORTED)
set_target_properties(avutil PROPERTIES IMPORTED_LOCATION ${FFMPEG_LIB_DIR}/libavutil.so)

add_library(swscale SHARED IMPORTED)
set_target_properties(swscale PROPERTIES IMPORTED_LOCATION ${FFMPEG_LIB_DIR}/libswscale.so)

add_library(swresample SHARED IMPORTED)
set_target_properties(swresample PROPERTIES IMPORTED_LOCATION ${FFMPEG_LIB_DIR}/libswresample.so)

add_library(avfilter SHARED IMPORTED)
set_target_properties(avfilter PROPERTIES IMPORTED_LOCATION ${FFMPEG_LIB_DIR}/libavfilter.so)

# Link all libraries
target_link_libraries(ffmpeg_jni
    ${log-lib}
    avformat
    avcodec
    avutil
    swscale
    swresample
    avfilter
)
