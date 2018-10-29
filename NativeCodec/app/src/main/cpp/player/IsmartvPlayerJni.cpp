//
// Created by huibin on 09/08/2017.
//


#include <assert.h>
#include <pthread.h>
#include <media/NdkMediaCodec.h>
#include <media/NdkMediaExtractor.h>
#include <android/native_window_jni.h>
#include <unistd.h>

#include "IsmartvPlayerJni.h"
#include "IsmartvPlayerInternal.h"
#include "Mp4Extractor.h"

#define  JNI_CLASS_ISMARTVPLAYER "cn/ismartv/player/IsmartvPlayer"
#define  JNI_CLASS_FRAME_DATA "cn/ismartv/player/IsmartvPlayer$FrameData"
#define  JNI_CLASS_DATA "cn/ismartv/player/IsmartvPlayer$Data"


static JavaVM *g_jvm;
static float height[AV_NUM_DATA_POINTERS] = {1, 0.5, 0.5};
static player_fields_t g_clazz;
void doCodecWork(workerdata *d);

void printFrameData(AVFrame* frame) ;

int64_t system_nano_time() {
    timespec now;
    clock_gettime(CLOCK_MONOTONIC, &now);
    return now.tv_sec * 1000000000LL + now.tv_nsec;
}


void PlayerLooper::handle(int what, void *obj) {

    workerdata *workerdata1 = (workerdata *) obj;
    PlayerLooper *playerLooper = (PlayerLooper *) workerdata1->extra_data;

    switch (what) {
        case kMsgCodecBuffer: {
            doCodecWork((workerdata *) obj);
        }
            break;
        case kMsgDecodeDone: {
            workerdata *d = (workerdata *) obj;
            AMediaCodec_stop(d->codec);
            AMediaCodec_delete(d->codec);
            AMediaExtractor_delete(d->ex);
            d->sawInputEOS = true;
            d->sawOutputEOS = true;
        }
            break;
        case kMsgSeek: {
            workerdata *d = (workerdata *) obj;
            AMediaExtractor_seekTo(d->ex, d->positionMs * 1000, AMEDIAEXTRACTOR_SEEK_NEXT_SYNC);
            AMediaCodec_flush(d->codec);
            d->renderstart = -1;
            d->sawInputEOS = false;
            d->sawOutputEOS = false;
            if (!d->isPlaying) {
                d->renderonce = true;
                playerLooper->post(kMsgCodecBuffer, d);
            }
            ALOGV("seeked");
        }
            break;
        case kMsgPause: {
            workerdata *d = (workerdata *) obj;
            if (d->isPlaying) {
                d->isPlaying = false;
                playerLooper->post(kMsgPauseAck, NULL, true);
            }
        }
            break;
        case kMsgResume: {
            workerdata *d = (workerdata *) obj;
            if (!d->isPlaying) {
                d->renderstart = -1;
                d->isPlaying = true;
                playerLooper->post(kMsgCodecBuffer, d);
            }
        }
            break;
        case kMsgRewind: {
            workerdata *d = (workerdata *) obj;
            AMediaExtractor_seekTo(d->ex, 0, AMEDIAEXTRACTOR_SEEK_NEXT_SYNC);
            AMediaCodec_flush(d->codec);
            d->renderstart = -1;
            d->sawInputEOS = false;
            d->sawOutputEOS = false;
            if (!d->isPlaying) {
                d->renderonce = true;
                playerLooper->post(kMsgCodecBuffer, d);
            }
        }
            break;

    }
}


PlayerParams::PlayerParams() {
    playerLooper_ = new PlayerLooper();
    player_data_ = (workerdata*)malloc(sizeof(workerdata));//{-1, NULL, NULL, NULL, 0, false, false, false, false, NULL};
    memset(player_data_, 0, sizeof(workerdata));
    player_data_->fd = -1;
    frameQueue_ = new CycleQueue<AVFrame>(5);

    //source name size : FILENAME_MAX
    player_data_->source = (char*) malloc(sizeof(char) * FILENAME_LENGTH);
    memset(player_data_->source, 0, FILENAME_LENGTH);

}

PlayerParams::~PlayerParams() {
    delete playerLooper_;
    playerLooper_ = NULL;

    if (player_data_ != NULL) {
        if (player_data_->source != NULL) {
            free((void *) player_data_->source);
            player_data_->source = NULL;
        }

        free(player_data_);
        player_data_ = NULL;
    }

    if (frameQueue_ != NULL) {
        delete frameQueue_;
        frameQueue_ = NULL;
    }
}

//static PlayerLooper *playerLooper = NULL;

static IsmartvMediaPlayer *jni_get_media_player(JNIEnv *env, jobject thiz) {
//    pthread_mutex_lock(&g_clazz.mutex);
//
//    IsmartvMediaPlayer *mp = (IsmartvMediaPlayer *) (intptr_t) cn_ismartv_player_IsmartvPlayer__mIsmartvMediaPlayer__get__catchAll(
//            env, thiz);
//    if (mp) {
//        ismartv_mp_inc_ref(mp);
//    }
//
//    pthread_mutex_unlock(&g_clazz.mutex);
//    return mp;
    return NULL;
}

//static IjkMediaPlayer *jni_set_media_player(JNIEnv* env, jobject thiz, IjkMediaPlayer *mp)
//{
//    pthread_mutex_lock(&g_clazz.mutex);
//
//    IjkMediaPlayer *old = (IjkMediaPlayer*) (intptr_t) J4AC_IjkMediaPlayer__mNativeMediaPlayer__get__catchAll(env, thiz);
//    if (mp) {
//        ijkmp_inc_ref(mp);
//    }
//    J4AC_IjkMediaPlayer__mNativeMediaPlayer__set__catchAll(env, thiz, (intptr_t) mp);
//
//    pthread_mutex_unlock(&g_clazz.mutex);
//
//    // NOTE: ijkmp_dec_ref may block thread
//    if (old != NULL ) {
//        ijkmp_dec_ref_p(&old);
//    }
//
//    return old;
//}

static void IsmartvPlayer_init(JNIEnv *env, jobject thiz) {
    PlayerParams *playerParams = new PlayerParams();
    ALOGI("create player_params addr : %p, player_loop addr : %p, player_data_addr : %p", playerParams, playerParams->playerLooper(), playerParams->player_data());
    workerdata *workerdata1 = playerParams->player_data();
    workerdata1->extra_data = playerParams->playerLooper();
    jclass IsmartvPlayer_cls = env->GetObjectClass(thiz);
    jfieldID obj_index = env->GetFieldID(IsmartvPlayer_cls, "objId", "J");
    env->SetLongField(thiz, obj_index, (jlong) playerParams);
}

static void IsmartvPlayer_uninit(JNIEnv *env, jobject thiz) {
    PlayerParams *playerParams = jni_getPlayerParam(env, thiz);
    PlayerParams **params = &playerParams;
    PlayerParams *nullParams = *(params);
    if (nullParams != NULL) {
        delete nullParams;
    }
    *(params) = NULL;

    jclass IsmartvPlayer_cls = env->GetObjectClass(thiz);
    jfieldID obj_index = env->GetFieldID(IsmartvPlayer_cls, "objId", "J");
    env->SetLongField(thiz, obj_index, -1);
}

static PlayerParams* jni_getPlayerParam(JNIEnv *env, jobject thiz) {
    jclass IsmartvPlayer_cls = env->GetObjectClass(thiz);
    jfieldID obj_index = env->GetFieldID(IsmartvPlayer_cls, "objId", "J");
    jlong tmp_index = env->GetLongField(thiz, obj_index);
    if (tmp_index == -1) {
        return NULL;
    }
    return (PlayerParams *) tmp_index;
}

static void IsmartvPlayer_setup(JNIEnv *env, jobject thiz, jobject weak_this) {

}

static void IsmartvPlayer_setSurface(JNIEnv *env, jobject thiz, jobject jsurface) {
    ALOGD("IsmartvPlayer_setSurface");

    PlayerParams *playerParams = jni_getPlayerParam(env, thiz);
    if (playerParams == NULL) {
        ALOGD("IsmartvPlayer_setSurface, playerParams == NULL, recreate it.");
        IsmartvPlayer_init(env, thiz);
        playerParams = jni_getPlayerParam(env, thiz);
        if (playerParams == NULL) {
            ALOGD("IsmartvPlayer_setSurface, recreate playerParams == NULL, return.");
            return;
        }
    }
    workerdata* data = playerParams->player_data();
//    PlayerLooper *playerLooper = playerParams->playerLooper();

    if (data->window) {
        ANativeWindow_release(data->window);
        data->window = NULL;
    }

    data->window = ANativeWindow_fromSurface(env, jsurface);
    ALOGD("@@@ setsurface %p", data->window);

}

static jboolean IsmartvPlayer_prepare(JNIEnv *env, jobject thiz, jstring jsource) {
    ALOGD("IsmartvPlayer_prepare");

    PlayerParams *playerParams = jni_getPlayerParam(env, thiz);
    if (playerParams == NULL) {
        ALOGD("IsmartvPlayer_prepare, playerParams == NULL, return.");
        return false;
    }
    workerdata* data = playerParams->player_data();
    PlayerLooper *playerLooper = playerParams->playerLooper();

    workerdata *d = data;

    const char * str = env->GetStringUTFChars(jsource, 0);
    strcpy(data->source, str);
//    d->source = (char *) env->GetStringUTFChars(jsource, 0);

    AMediaExtractor *extractor = AMediaExtractor_new();

    media_status_t mediaStatus = AMediaExtractor_setDataSource(extractor, d->source);

//    free(d.data)

    if (mediaStatus != AMEDIA_OK) {
        ALOGE("setDataSource error: %d", mediaStatus);
        return JNI_FALSE;
    }

    int numberTracks = AMediaExtractor_getTrackCount(extractor);
    ALOGD("input has %d tracks", numberTracks);

    AMediaCodec *mediaCodec = NULL;

    for (int i = 0; i < numberTracks; ++i) {
        AMediaFormat *mediaFormat = AMediaExtractor_getTrackFormat(extractor, i);
        const char *format = AMediaFormat_toString(mediaFormat);
        ALOGV("track %d format: %s", i, format);

        const char *mime;
        if (!AMediaFormat_getString(mediaFormat, AMEDIAFORMAT_KEY_MIME, &mime)) {
            ALOGE("no mime type");
            return JNI_FALSE;
        } else if (!strncmp(mime, "video/", 6)) {
            AMediaExtractor_selectTrack(extractor, i);
            mediaCodec = AMediaCodec_createDecoderByType(mime);
            AMediaCodec_configure(mediaCodec, mediaFormat, d->window, NULL, 0);

            d->ex = extractor;
            d->codec = mediaCodec;
            d->renderstart = -1;
            d->sawInputEOS = false;
            d->sawOutputEOS = false;
            d->isPlaying = false;
            d->renderonce = true;
            AMediaCodec_start(mediaCodec);
        }
        AMediaFormat_delete(mediaFormat);
    }

    print_worker_data(data);

//    playerLooper = new PlayerLooper();
    playerLooper->post(kMsgSeek, data);

    return JNI_TRUE;
}

void* read_thread(void* args) {

    LOGI("dhb test, test read packet begin ... ");

    PlayerParams *playerParams = (PlayerParams *) args;
    workerdata* workerdata1 = playerParams->player_data();
    CycleQueue<AVFrame>* queue = playerParams->frameQueue();
    if (queue == NULL) {
        ALOGE("dhb test, in read_thread, get player queue error!!!");
        return false;
    }
    av_register_all();
    AVFormatContext *pFormatCtx = avformat_alloc_context();

    LOGI("dhb test, open input file : %s", workerdata1->source);
    if (avformat_open_input(&pFormatCtx, workerdata1->source, NULL, NULL) != 0) {
        LOGI("Couldn't open file:%s\n", workerdata1->source);
        return false;
    }

    // Retrieve stream information
    LOGI("find stream begin ... ");
    if (avformat_find_stream_info(pFormatCtx, NULL) < 0) {
        LOGI("Couldn't find stream information.");
        return false;
    }


    // Find the first video stream
    int videoStream = -1, i;
    for (i = 0; i < pFormatCtx->nb_streams; i++) {
        if (pFormatCtx->streams[i]->codec->codec_type == AVMEDIA_TYPE_VIDEO
            && videoStream < 0) {
            videoStream = i;
        }
    }
    if (videoStream == -1) {
        LOGI("Didn't find a video stream.");
        return false;
    }

    //AVStream
    AVStream *stream = pFormatCtx->streams[videoStream];
    int frame_rate = stream->avg_frame_rate.num / stream->avg_frame_rate.den;
    LOGI("frame rate : %d", frame_rate);

    // Get a pointer to the codec context for the video stream
    AVCodecContext *pCodecCtx = pFormatCtx->streams[videoStream]->codec;

    // Find the decoder for the video stream
    LOGI("find decoder, codec id : %d", pCodecCtx->codec_id);
    AVCodec *pCodec = avcodec_find_decoder(pCodecCtx->codec_id);
    if (pCodec == NULL) {
        LOGI("Codec not found.");
        return false;
    }

    LOGI("open decoder begin ... ");
    if (avcodec_open2(pCodecCtx, pCodec, NULL) < 0) {
        LOGI("Could not open codec.");
        return false;
    }

    // 获取视频宽高
    int videoWidth = pCodecCtx->width;
    int videoHeight = pCodecCtx->height;

    if (avcodec_open2(pCodecCtx, pCodec, NULL) < 0) {
        LOGI("Could not open codec.");
        return false;
    }

    // Allocate video frame
    AVFrame *pFrame = av_frame_alloc();


//根据实际解码格式天添加到队列中
//    // 用于渲染
//    AVFrame *pFrameRGBA = av_frame_alloc();
//    if (pFrameRGBA == NULL || pFrame == NULL) {
//        LOGI("Could not allocate video frame.");
//        return false;
//    }

    // Determine required buffer size and allocate buffer
    // buffer中数据就是用于渲染的,且格式为RGBA
//    int numBytes = av_image_get_buffer_size(AV_PIX_FMT_RGBA, pCodecCtx->width, pCodecCtx->height,
//                                            1);
//    uint8_t *buffer = (uint8_t *) av_malloc(numBytes * sizeof(uint8_t));
//    av_image_fill_arrays(pFrameRGBA->data, pFrameRGBA->linesize, buffer, AV_PIX_FMT_RGBA,
//                         pCodecCtx->width, pCodecCtx->height, 1);

    // 由于解码出来的帧格式不是RGBA的,在渲染之前需要进行格式转换
//    struct SwsContext *sws_ctx = sws_getContext(pCodecCtx->width,
//                                                pCodecCtx->height,
//                                                pCodecCtx->pix_fmt,
//                                                pCodecCtx->width,
//                                                pCodecCtx->height,
//                                                AV_PIX_FMT_RGBA,
//                                                SWS_BILINEAR,
//                                                NULL,
//                                                NULL,
//                                                NULL);

    int frameFinished;
    AVPacket packet, *pkt;
    pkt = &packet;
    LOGI("before av_read_frame.");
    while (av_read_frame(pFormatCtx, pkt) >= 0) {
        if (pkt->stream_index == videoStream) {
            LOGI("av_read_frame for video, context duration : %lld, current pkt pts (%lld)(%llf), dts : (%lld)(%llf), keyframe : %d",
                 pFormatCtx->duration, pkt->pts,
                 pkt->pts * av_q2d(pFormatCtx->streams[pkt->stream_index]->time_base), pkt->dts,
                 pkt->dts * av_q2d(pFormatCtx->streams[pkt->stream_index]->time_base),
                 (pkt->flags));
            avcodec_decode_video2(pCodecCtx, pFrame, &frameFinished, pkt);
            // 并不是decode一次就可解码出一帧
            if (frameFinished) {
                ALOGI("dhb test, print origin frame ... ");
//                printFrameData(pFrame);
                ALOGI("dhb test, after one frame finished, push it into frame queue, frame pts : %lld, frame format : %d", pFrame->pts, pFrame->format);
                queue->lock();
                AVFrame* frame = queue->queue_push();
                frame->format = pFrame->format;
                frame->width = pFrame->width;
                frame->height = pFrame->height;
                for (int i = 0; i < AV_NUM_DATA_POINTERS; ++i) {
                    if (pFrame->linesize[i] <= 0) {
                        break;
                    }
                    int len = pFrame->linesize[i] * (pFrame->height * height[i]);
                    ALOGI("malloc mem size : %d", len);
                    frame->data[i] = (uint8_t*) malloc((size_t) len);

                    memset(frame->data[i], 0, pFrame->linesize[i] * (pFrame->height * height[i]));
                    frame->linesize[i] = pFrame->linesize[i];
                    memcpy(frame->data[i], pFrame->data[i], pFrame->linesize[i] * ((pFrame->height * height[i])));
                }

                ALOGI("dhb test, print copied frame ... ");
//                printFrameData(frame);
                queue->unlock();
                // 格式转换
//                sws_scale(sws_ctx, (uint8_t const *const *) pFrame->data,
//                          pFrame->linesize, 0, pCodecCtx->height,
//                          pFrameRGBA->data, pFrameRGBA->linesize);


                // 由于window的stride和帧的stride不同,因此需要逐行复制
//                int h;
//                for (h = 0; h < videoHeight; h++) {
//                    memcpy(dst + h * dstStride, src + h * srcStride, srcStride);
//                }
//            }

                //
            }
            av_packet_unref(&packet);
        }
    }

//    av_free(buffer);
//    av_free(pFrameRGBA);

    // Free the YUV frame
    av_free(pFrame);

    // Close the codecs
    avcodec_close(pCodecCtx);

    // Close the video file
    avformat_close_input(&pFormatCtx);
    LOGI("dhb test, test read packet end ... ");
}

static jobject IsmartvPlayer_getFrameByIndex(JNIEnv *env, jobject instance, jstring source, jobject surface, jint index) {

    ALOGI("dhb test, get frame begin ... ");
    if (g_clazz.inner_class_frame_data == NULL
        || g_clazz.inner_class_data == NULL) {
        ALOGE("get frame by index failed, inner class == NULL");
        return NULL;
    }

    ALOGI("dhb test, get frame before get player params ... ");

    PlayerParams *playerParams = jni_getPlayerParam(env, instance);
    if (playerParams == NULL) {
        ALOGE("get frame by index(%d) failed, player params == NULL");
        return NULL;
    }

    ALOGI("dhb test, get frame before get player queue ... ");
    CycleQueue<AVFrame>* queue = playerParams->frameQueue();
    if (queue == NULL) {
        ALOGE("get frame by index(%d) failed, frame queue == NULL");
        return NULL;
    }


    //add list
//    ALOGI("get frame by index(%d), array class addr : %p, construct addr : %p", index, g_clazz.array_list, g_clazz.list_construct);
    jobject obj_ArrayList = env->NewObject(g_clazz.array_list, g_clazz.list_construct,"");
    ALOGI("dhb test, get frame before pop ... ");
    queue->lock();
    AVFrame *frame = queue->queue_pop();
    ALOGI("dhb test, get frame after pop ... ");
//    printFrameData(frame);

    for (int i = 0; i < AV_NUM_DATA_POINTERS; ++i) {
        if (frame->linesize[i] <= 0) {
            break;
        }
        jobject obj = env->AllocObject(g_clazz.inner_class_data);
        jbyteArray array = env->NewByteArray(frame->linesize[i] * frame->height * height[i]);
        env->SetByteArrayRegion(array, 0, frame->linesize[i]* frame->height * height[i], (const jbyte *) frame->data[i]);

        env->SetObjectField(obj, g_clazz.inner_class_data_buf, array);
        env->SetIntField(obj, g_clazz.inner_class_data_linesize, frame->linesize[i]);
        env->SetIntField(obj, g_clazz.inner_class_data_format, frame->format);
        env->SetIntField(obj, g_clazz.inner_class_data_width, frame->width);
        env->SetIntField(obj, g_clazz.inner_class_data_height, frame->height);
        env->CallBooleanMethod(obj_ArrayList, g_clazz.list_add, obj);

        env->DeleteLocalRef(obj);
    }
    env->SetObjectField(g_clazz.gl_inner_class_frame_data_obj, g_clazz.inner_class_frame_data_datas, obj_ArrayList);

    //release data in frame
    ALOGI("dhb test, get frame before release frame data ... ");

    for (int i = 0; i < AV_NUM_DATA_POINTERS; ++i) {
        if (frame->linesize[i] <= 0) {
            break;
        }
        if (frame->data[i] != NULL) {
            free(frame->data[i]);
        }

    }
    ALOGI("dhb test, get frame after release frame data ... ");
    queue->unlock();

    env->DeleteLocalRef(obj_ArrayList);
    ALOGI("dhb test, get frame end ... ");

    return g_clazz.gl_inner_class_frame_data_obj;
}


static jboolean IsmartvPlayer_prepare2(JNIEnv *env, jobject thiz, jstring source) {
    LOGI("dhb test, test prepare2 begin ... ");

    PlayerParams *playerParams = jni_getPlayerParam(env, thiz);
    if (playerParams == NULL) {
        ALOGE("dhb test, in player prepared2, get player params error!!!");
        return false;
    }
    workerdata* work_data = playerParams->player_data();
//    work_data->source = dir;
    const char * str = env->GetStringUTFChars(source, 0);
    strcpy(work_data->source, str);
    pthread_create(&(playerParams->readThread), NULL, read_thread, (void*) playerParams);
//    pthread_join(playerParams->readThread, NULL);
    LOGI("dhb test, test prepare2 end ... ");
    return true;

}

static void IsmartvPlayer_setPlayWhenReady(JNIEnv *env, jobject thiz, jboolean playWhenReady) {
    ALOGD("IsmartvPlayer_setPlayingStreamingMediaPlayer");

    PlayerParams *playerParams = jni_getPlayerParam(env, thiz);
    if (playerParams == NULL) {
        ALOGD("IsmartvPlayer_setPlayingStreamingMediaPlayer, playerParams == NULL, return.");
        return;
    }
    workerdata* data = playerParams->player_data();
    PlayerLooper *playerLooper = playerParams->playerLooper();

    if (playerLooper) {
        if (playWhenReady) {
            playerLooper->post(kMsgResume, data);
        } else {
            playerLooper->post(kMsgPause, data);
        }
    }
}

static void IsmartvPlayer_stop(JNIEnv *env, jobject thiz) {
    ALOGD("IsmartvPlayer_stop");

    PlayerParams *playerParams = jni_getPlayerParam(env, thiz);
    if (playerParams == NULL) {
        return;
    }
    workerdata* data = playerParams->player_data();
    PlayerLooper *playerLooper = playerParams->playerLooper();
    if (playerLooper) {
        playerLooper->post(kMsgDecodeDone, data, true);
        playerLooper->quit();
//        delete playerLooper;
//        playerLooper = NULL;
    }

    if (data->window) {
        ANativeWindow_release(data->window);
        data->window = NULL;
    }

    //release
    IsmartvPlayer_uninit(env, thiz);
}

static void IsmartvPlayer_stop2(JNIEnv *env, jobject thiz) {
    ALOGD("IsmartvPlayer_stop");

    PlayerParams *playerParams = jni_getPlayerParam(env, thiz);
    if (playerParams == NULL) {
        return;
    }
    workerdata* data = playerParams->player_data();
    PlayerLooper *playerLooper = playerParams->playerLooper();


    IsmartvPlayer_uninit(env, thiz);
}

static void IsmartvPlayer_rewind(JNIEnv *env, jobject thiz) {
    ALOGD("IsmartvPlayer_rewind");

    PlayerParams *playerParams = jni_getPlayerParam(env, thiz);
    if (playerParams == NULL) {
        return;
    }
    workerdata* data = playerParams->player_data();
    PlayerLooper *playerLooper = playerParams->playerLooper();
    if (playerLooper) {
        playerLooper->post(kMsgRewind, data);
    }
}

static jlong IsmartvPlayer_getDuration(JNIEnv *env, jobject instance) {
    ALOGD("IsmartvPlayer_getDuration begin");

    PlayerParams *playerParams = jni_getPlayerParam(env, instance);
    if (playerParams == NULL) {
        ALOGD("IsmartvPlayer_getDuration, playerParams == NULL, return");
        return -1;
    }
    workerdata* data = playerParams->player_data();
    print_worker_data(data);
//    PlayerLooper *playerLooper = playerParams->playerLooper();
    int64_t duration = Mp4Extractor_getDuration(data->source);
    return (jlong)duration;
}

static long IsmartvPlayer_getCurrentPosition(JNIEnv *env, jobject instance) {
    ALOGD("IsmartvPlayer_getCurrentPosition");

}

static void IsmartvPlayer_seekTo(JNIEnv *env, jobject instance, jlong positionMs) {
    ALOGD("IsmartvPlayer_seekTo");

    PlayerParams *playerParams = jni_getPlayerParam(env, instance);
    if (playerParams == NULL) {
        return;
    }
    workerdata* data = playerParams->player_data();
    PlayerLooper *playerLooper = playerParams->playerLooper();
    data->positionMs = (size_t)positionMs;
    if (playerLooper){
        playerLooper->post(kMsgSeek, data);
    }
}

    void printFrameData(AVFrame* frame) {
//        ALOGI("frame data y : \n");
//        char y[frame->linesize[0] * 3 * (int)(frame->height * height[0]) + 1];
//        char u[frame->linesize[1] * 3 * (int)(frame->height * height[1]) + 1];
//        char v[frame->linesize[2] * 3 * (int)(frame->height * height[2]) + 1];
//
//        for (int i = 0; i < frame->linesize[0] * frame->height * height[0]; ++i) {
//            sprintf(y + i * 3, "%x ", frame->data[0][i]);
//        }
//        ALOGI("%s", y);
//
//        ALOGI("frame data u : \n");
//        for (int i = 0; i < frame->linesize[1] * frame->height * height[1]; ++i) {
//            sprintf(u + i * 3, "%x ", frame->data[1][i]);
//        }
//        ALOGI("%s", u);
//
//        ALOGI("frame data v : \n");
//        for (int i = 0; i < frame->linesize[2] * frame->height * height[2]; ++i) {
//            sprintf(v + i * 3, "%x ", frame->data[2][i]);
//        }
//        ALOGI("%s", v);

    }

    void doCodecWork(workerdata *d) {
        ssize_t bufidx = -1;
        PlayerLooper *playerLooper = (PlayerLooper *) d->extra_data;

        if (!d->sawInputEOS) {
            bufidx = AMediaCodec_dequeueInputBuffer(d->codec, 2000);
            ALOGV("input buffer %zd", bufidx);
            if (bufidx >= 0) {
                size_t bufsize;
                auto buf = AMediaCodec_getInputBuffer(d->codec, bufidx, &bufsize);
                auto sampleSize = AMediaExtractor_readSampleData(d->ex, buf, bufsize);
                if (sampleSize < 0) {
                    sampleSize = 0;
                    d->sawInputEOS = true;
                    ALOGV("EOS");
                }

                auto presentationTimeUs = AMediaExtractor_getSampleTime(d->ex);

                AMediaCodec_queueInputBuffer(d->codec, bufidx, 0, sampleSize, presentationTimeUs,
                                             d->sawInputEOS ? AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM
                                                            : 0);
                AMediaExtractor_advance(d->ex);
            }
        }

        if (!d->sawOutputEOS) {
            AMediaCodecBufferInfo info;
            auto status = AMediaCodec_dequeueOutputBuffer(d->codec, &info, 0);
            if (status >= 0) {
                if (info.flags & AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM) {
                    ALOGV("output EOS");
                    d->sawOutputEOS = true;
                }

                int64_t presentationNano = info.presentationTimeUs * 1000;

                if (d->renderstart < 0) {
                    d->renderstart = system_nano_time() - presentationNano;
                }

                int64_t delay = (d->renderstart + presentationNano) - system_nano_time();

                if (delay > 0) {
                    usleep(delay / 1000);
                }

                AMediaCodec_releaseOutputBuffer(d->codec, status, info.size != 0);

                if (d->renderonce) {
                    d->renderonce = false;
                    return;
                }
            } else if (status == AMEDIACODEC_INFO_OUTPUT_BUFFERS_CHANGED) {
                ALOGV("output buffers changed");
            } else if (status == AMEDIACODEC_INFO_OUTPUT_FORMAT_CHANGED) {
                auto format = AMediaCodec_getOutputFormat(d->codec);
                ALOGV("format changed to: %s", AMediaFormat_toString(format));
                AMediaFormat_delete(format);
            } else if (status == AMEDIACODEC_INFO_TRY_AGAIN_LATER) {
                ALOGV("no output buffer right now");
            } else {
                ALOGV("unexpected info code: %zd", status);
            }
        }

        if (!d->sawInputEOS || !d->sawOutputEOS) {
            playerLooper->post(kMsgCodecBuffer, d);
        }
    }


    static JNINativeMethod g_methods[] = {

            {
                    "_init",
                    "()V",
                    (void *) IsmartvPlayer_init
            },

            {
                    "_prepare",
                    "(Ljava/lang/String;)Z",
                    (void *) IsmartvPlayer_prepare
            },

            {
                    "_prepare2",
                    "(Ljava/lang/String;)Z",
                    (void *) IsmartvPlayer_prepare2
            },

            {       "_setSurface",
                    "(Landroid/view/Surface;)V",
                    (void *) IsmartvPlayer_setSurface
            },

            {
                    "_setPlayWhenReady",
                    "(Z)V",
                    (void *) IsmartvPlayer_setPlayWhenReady
            },

            {
                    "_stop",
                    "()V",
                    (void *) IsmartvPlayer_stop
            },

            {
                    "_stop2",
                    "()V",
                    (void *) IsmartvPlayer_stop2
            },

            {
                    "_rewind",
                    "()V",
                    (void *) IsmartvPlayer_rewind
            },

            {
                    "_getDuration",
                    "()J",
                    (void *) IsmartvPlayer_getDuration
            },

            {
                    "_getCurrentPosition",
                    "()J",
                    (void *) IsmartvPlayer_getCurrentPosition

            },

            {
                    "_seekTo",
                    "(J)V",
                    (void *) IsmartvPlayer_seekTo
            },
//
            {
                    "_getVideoFrameBySort",
                    "(Ljava/lang/String;Landroid/view/Surface;I)Lcn/ismartv/player/IsmartvPlayer$FrameData;",
                    (void *) IsmartvPlayer_getFrameByIndex
            }

    };

//
//static int ismartv_find_java_class(JNIEnv *env, jobject jclazz, char *class_sign) {
//    do {
//        jclass clazz = env->FindClass(class_sign);
//        if (ExceptionCheck__catchAll(env) || !(clazz)) {
//            ALOGE("FindClass failed: %s", class_sign);
//            return -1;
//
//        }
//        jclazz = env->NewGlobalRef(clazz);
//        if (ExceptionCheck__catchAll(env) || !(jclazz)) {
//
//            ALOGE("FindClass::NewGlobalRef failed: %s", class_sign);
//            env->DeleteLocalRef(clazz);
//            return -1;
//
//        }
//        env->DeleteLocalRef(clazz);
//
//    } while (0);
//}
//

    JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
        ALOGD("JNI_OnLoad");

        JNIEnv *env = NULL;

        g_jvm = vm;

        if (vm->GetEnv((void **) &env, JNI_VERSION_1_4) != JNI_OK) {
            return -1;
        }

        assert(env != NULL);

        pthread_mutex_init(&g_clazz.mutex, NULL);

        // FindClass returns LocalReference
//    ISMARTV_FIND_JAVA_CLASS(env, g_clazz.clazz, JNI_CLASS_ISMARTVPLAYER);
//    do {
        g_clazz.clazz = env->FindClass(JNI_CLASS_ISMARTVPLAYER);
        g_clazz.clazz = (jclass) env->NewGlobalRef(g_clazz.clazz);
        ALOGI("JNI_OnLoad before load inner FrameData. ");
        //inner class FrameData
        g_clazz.inner_class_frame_data = env->FindClass(JNI_CLASS_FRAME_DATA);
        g_clazz.inner_class_frame_data = (jclass) env->NewGlobalRef(g_clazz.inner_class_frame_data);
        g_clazz.inner_class_frame_data_datas = env->GetFieldID(g_clazz.inner_class_frame_data, "datas", "Ljava/util/ArrayList;");
        jobject frame_data_obj = env->AllocObject(g_clazz.inner_class_frame_data);
        jobject gl_frame_data = env->NewGlobalRef(frame_data_obj);
        g_clazz.gl_inner_class_frame_data_obj = gl_frame_data;

        ALOGI("JNI_OnLoad before load inner Data. ");
        //inner class Data
        g_clazz.inner_class_data = env->FindClass(JNI_CLASS_DATA);
        g_clazz.inner_class_data = (jclass) env->NewGlobalRef(g_clazz.inner_class_data);
        g_clazz.inner_class_data_buf = env->GetFieldID(g_clazz.inner_class_data, "data", "[B");
        g_clazz.inner_class_data_linesize = env->GetFieldID(g_clazz.inner_class_data, "linesize", "I");
        g_clazz.inner_class_data_format = env->GetFieldID(g_clazz.inner_class_data, "format", "I");
        g_clazz.inner_class_data_width = env->GetFieldID(g_clazz.inner_class_data, "width", "I");
        g_clazz.inner_class_data_height = env->GetFieldID(g_clazz.inner_class_data, "height", "I");

        ALOGI("JNI_OnLoad before load inner ArrayList. ");
        //ArrayList
        g_clazz.array_list = env->FindClass("java/util/ArrayList");
        g_clazz.array_list = (jclass) env->NewGlobalRef(g_clazz.array_list);
        g_clazz.list_construct = env->GetMethodID(g_clazz.array_list,"<init>","()V");
        g_clazz.list_add = env->GetMethodID(g_clazz.array_list,"add","(Ljava/lang/Object;)Z");


        if (ExceptionCheck__catchAll(env) || !(g_clazz.clazz)) {
            ALOGE("FindClass failed: %s", JNI_CLASS_ISMARTVPLAYER);
            return -1;
        }

//        jobject  object = env->NewGlobalRef()
//    }while (0);

        env->RegisterNatives(g_clazz.clazz, g_methods, NELEM(g_methods));

        return JNI_VERSION_1_4;
    }


    JNIEXPORT void JNI_OnUnload(JavaVM *vm, void *reserved) {
        ALOGD("JNI_OnUnLoad");

        pthread_mutex_destroy(&g_clazz.mutex);
        JNIEnv *env = NULL;
        if (vm->GetEnv((void **) &env, JNI_VERSION_1_4) != JNI_OK) {
            ALOGE("jni on unload get env failed.");
            return;
        }
        env->DeleteGlobalRef(g_clazz.gl_inner_class_frame_data_obj);
        env->DeleteGlobalRef(g_clazz.clazz);
        env->DeleteGlobalRef(g_clazz.inner_class_frame_data);
        env->DeleteGlobalRef(g_clazz.inner_class_data);
        env->DeleteGlobalRef(g_clazz.array_list);
    }





