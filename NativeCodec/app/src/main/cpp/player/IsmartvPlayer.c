//
// Created by huibin on 10/08/2017.
//

#include <assert.h>
#include "IsmartvPlayer.h"
#include "IsmartvPlayerInternal.h"

void ismartv_mp_inc_ref(IsmartvMediaPlayer *mp) {
    assert(mp);
    __sync_fetch_and_add(&mp->ref_count, 1);
}