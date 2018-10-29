LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_LIB_PATH := $(shell cd .. && pwd)
LOCAL_LIB_PATH := $(LOCAL_LIB_PATH)/app/src/main
$(info libs dir : $(LOCAL_LIB_PATH))
LOCAL_MODULE := libffmpeg
LOCAL_SRC_FILES := $(LOCAL_LIB_PATH)/jniLibs/$(TARGET_ARCH_ABI)/libijkffmpeg.so
FFMPEG_INCLUDE_PATH := $(LOCAL_LIB_PATH)/jniLibs/$(TARGET_ARCH_ABI)/include
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)
$(info local path : $(LOCAL_PATH))
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_SRC_FILES := \
                looper.cpp \
                player/IsmartvBase.c \
                player/IsmartvMediaPlayer.c \
                player/IsmartvPlayer.c \
                player/IsmartvPlayerJni.cpp \
                player/Mp4Extractor.c \
LOCAL_CFLAGS += -std=c99
LOCAL_LDLIBS := -ldl -llog -landroid -lmediandk -lOpenMAXAL
LOCAL_SHARED_LIBRARIES := libffmpeg
LOCAL_MODULE := native-codec-jni
LOCAL_C_INCLUDES += $(FFMPEG_INCLUDE_PATH)
$(info LOCAL_C_INCLUDES : $(LOCAL_C_INCLUDES))
include $(BUILD_SHARED_LIBRARY)
