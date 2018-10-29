//
// Created by huibin on 09/08/2017.
//

#ifndef NATIVE_CODEC_ISMARTVBASE_H
#define NATIVE_CODEC_ISMARTVBASE_H

#ifdef __cplusplus
extern "C" {
#endif

#include <string.h>
#include <stdbool.h>
#include <stdint.h>
#include <jni.h>
#include <android/log.h>

#define LOG_TAG "Ismartv-Player"
#define VLOGV(...)  __android_log_vprint(ANDROID_LOG_VERBOSE,   LOG_TAG, __VA_ARGS__)
#define VLOGD(...)  __android_log_vprint(ANDROID_LOG_DEBUG,     LOG_TAG, __VA_ARGS__)
#define VLOGI(...)  __android_log_vprint(ANDROID_LOG_INFO,      LOG_TAG, __VA_ARGS__)
#define VLOGW(...)  __android_log_vprint(ANDROID_LOG_WARN,      LOG_TAG, __VA_ARGS__)
#define VLOGE(...)  __android_log_vprint(ANDROID_LOG_ERROR,     LOG_TAG, __VA_ARGS__)

#define ALOGV(...)  __android_log_print(ANDROID_LOG_VERBOSE,    LOG_TAG, __VA_ARGS__)
#define ALOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,      LOG_TAG, __VA_ARGS__)
#define ALOGI(...)  __android_log_print(ANDROID_LOG_INFO,       LOG_TAG, __VA_ARGS__)
#define ALOGW(...)  __android_log_print(ANDROID_LOG_WARN,       LOG_TAG, __VA_ARGS__)
#define ALOGE(...)  __android_log_print(ANDROID_LOG_ERROR,      LOG_TAG, __VA_ARGS__)
//#define LOGI(...) ALOGI(...)
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO,       LOG_TAG, __VA_ARGS__)


#ifndef NELEM
#define NELEM(x) ((int) (sizeof(x) / sizeof((x)[0])))
#endif

bool ExceptionCheck__catchAll(JNIEnv *env);

#define ISMARTV_FIND_JAVA_CLASS(env__, var__, classsign__) \
    do { \
        jclass clazz = env__->FindClass(classsign__); \
        if (ExceptionCheck__catchAll(env) || !(clazz)) { \
            ALOGE("FindClass failed: %s", classsign__); \
            return -1; \
        } \
        var__ = env__->NewGlobalRef(clazz); \
        if (ExceptionCheck__catchAll(env) || !(var__)) { \
            ALOGE("FindClass::NewGlobalRef failed: %s", classsign__); \
            env__->DeleteLocalRef( clazz); \
            return -1; \
        } \
        env__->DeleteLocalRef(clazz); \
    } while(0);

#ifdef __cplusplus
}
#endif
#endif //NATIVE_CODEC_ISMARTVBASE_H
