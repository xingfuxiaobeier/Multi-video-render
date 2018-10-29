//
// Created by huibin on 09/08/2017.
//

#include "IsmartvBase.h"

bool ExceptionCheck__catchAll(JNIEnv *env) {
    if ((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
        return true;
    }

    return false;
}
