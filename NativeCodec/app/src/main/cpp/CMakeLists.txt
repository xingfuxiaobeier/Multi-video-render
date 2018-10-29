cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -Wall -UNDEBUG")
set(FFMPEG_LIB_DIR ${PROJECT_SOURCE_DIR}/../../../src/main/jniLibs/${ANDROID_ABI})
set(FFMPEG_INC_DIR ${PROJECT_SOURCE_DIR}/../../../src/main/jniLibs/${ANDROID_ABI}/include)


#MESSAGE(STATUS "ffmpeg lib Directory: ${FFMPEG_LIB_DIR}")

# create new lib
add_library(native-codec-jni SHARED
            looper.cpp
            player/IsmartvBase.c
            player/IsmartvMediaPlayer.c
            player/IsmartvPlayer.c
            player/IsmartvPlayerJni.cpp
            player/Mp4Extractor.c
            CycleQueue.cpp
            )

# find lib
find_library( ijkffmpeg-lib ijkffmpeg PATHS ${FFMPEG_LIB_DIR}/ NO_CMAKE_FIND_ROOT_PATH)

# include header files
include_directories(
    ${FFMPEG_INC_DIR}
    ${FFMPEG_INC_DIR}/libffmpeg
)

# Include libraries needed for native-codec-jni lib
target_link_libraries(native-codec-jni

                      ${ijkffmpeg-lib}

                      android
                      log
                      mediandk
                      OpenMAXAL)

