//
// Created by huibin on 10/08/2017.
//

#include "IsmartvMediaPlayer.h"
#include "IsmartvBase.h"

typedef struct cn_ismartv_player_IsmartvMediaPlayer {
    jclass id;

    jfieldID filed_mIsmartvPlayer;

} cn_ismartv_player_IsmartvMediaPlayer;

static cn_ismartv_player_IsmartvMediaPlayer class_cn_ismartv_player_IsmartvMediaPlayer;


jlong cn_ismartv_player_IsmartvPlayer__mIsmartvMediaPlayer__get(JNIEnv *env, jobject thiz) {
    return (*env)->GetLongField(env, thiz,
                                class_cn_ismartv_player_IsmartvMediaPlayer.filed_mIsmartvPlayer);
}

jlong
cn_ismartv_player_IsmartvPlayer__mIsmartvMediaPlayer__get__catchAll(JNIEnv *env, jobject thiz) {
    jlong ret_value = cn_ismartv_player_IsmartvPlayer__mIsmartvMediaPlayer__get(env, thiz);
    if (ExceptionCheck__catchAll(env)) {
        return 0;
    }
    return ret_value;
}


void test(JNIEnv *env, jobject thiz) {
    cn_ismartv_player_IsmartvPlayer__mIsmartvMediaPlayer__get__catchAll(env, thiz);
}