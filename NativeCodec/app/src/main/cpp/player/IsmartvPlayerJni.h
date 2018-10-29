//
// Created by huibin on 09/08/2017.
//

#include <jni.h>
#include "IsmartvBase.h"
#include "../looper.h"
#include "../CycleQueue.cpp"

#ifdef __cplusplus
extern "C" {
#endif
#include <sys/stat.h>
#include <stdio.h>
#include "libavformat/avformat.h"
#include "libavcodec/avcodec.h"
#include "libswscale/swscale.h"
#include "libswresample/swresample.h"
#include "libavutil/imgutils.h"

#ifndef NATIVE_CODEC_ISMARTVPLAYERJNI_H
#define NATIVE_CODEC_ISMARTVPLAYERJNI_H

//define
#define FILENAME_LENGTH 128
#define var
#define method

#define PROPERTY(varType, varName, funName) \
protected var : varType varName##_; \
public method : varType varName(void) const { return varName##_;} \
public method : void set##funName(varType value) { varName##_ = value;} \

typedef struct player_fields_t {
    pthread_mutex_t mutex;
    jclass clazz;
    jclass inner_class_frame_data;
    jobject gl_inner_class_frame_data_obj;
    jfieldID inner_class_frame_data_datas;

    jclass inner_class_data;
    jfieldID inner_class_data_buf;
    jfieldID inner_class_data_linesize;
    jfieldID inner_class_data_format;
    jfieldID inner_class_data_width;
    jfieldID inner_class_data_height;

    jclass array_list;
    jmethodID list_construct;
    jmethodID list_add;
} player_fields_t;

typedef struct data {
    int fd;
    char *source;
    ANativeWindow *window;
    AMediaExtractor *ex;
    AMediaCodec *codec;
    int64_t renderstart;
    bool sawInputEOS;
    bool sawOutputEOS;
    bool isPlaying;
    bool renderonce;
    int64_t positionMs;
    void* extra_data;
//    void (*set_extra_data)(void*);
} workerdata;

void print_worker_data(workerdata *data) {
    if (data == NULL) {
        return;
    }
    ALOGI("print data : source = %s, mediacodec = %p, extra_data = %p", data->source, data->codec, data->extra_data);
}

//workerdata data = {-1, NULL, NULL, NULL, 0, false, false, false, false, NULL};

enum {
    kMsgCodecBuffer,
    kMsgPause,
    kMsgResume,
    kMsgPauseAck,
    kMsgDecodeDone,
    kMsgSeek,
    kMsgRewind
};

class PlayerParams;
//static void IsmartvPlayer_init(JNIEnv *env);

//static void IsmartvPlayer_setup(JNIEnv *env, jobject thiz, jobject weak_this);

static PlayerParams *jni_getPlayerParam(JNIEnv *env, jobject thiz);

class PlayerLooper : public looper {
    virtual void handle(int what, void *obj);
};

class PlayerParams {
public:
    PlayerParams();
    ~PlayerParams();
    PlayerParams& operator=(const PlayerParams& ) = delete;
    PlayerParams(PlayerParams&) = delete;
    pthread_t readThread;

private:

    PROPERTY(PlayerLooper*, playerLooper, PlayerLooper)

    PROPERTY(workerdata*, player_data, Data)

    PROPERTY(CycleQueue<AVFrame>*, frameQueue, FrameQueue)

};

#ifdef __cplusplus
}
#endif
#endif //NATIVE_CODEC_ISMARTVPLAYERJNI_H
