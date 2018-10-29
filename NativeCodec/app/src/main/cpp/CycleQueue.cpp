//
// Created by dhb on 2018/7/3.
//

#include "CycleQueue.h"
#include "player/IsmartvBase.h"

template <typename T>
CycleQueue<T>::CycleQueue() {

}
template <typename T>
CycleQueue<T>::CycleQueue(int num)
        : size(num), next_to_read(0), next_to_write(0){
    pthread_mutex_init(&mutex, NULL);
    pthread_cond_init(&cond, NULL);
    tab = (T**) malloc(sizeof((*tab)) * size);
    for (int i = 0; i < size; ++i) {
        tab[i] = (T*)malloc(sizeof(T));
        memset(tab[i], 0, sizeof(T));
    }
}

template <typename T>
CycleQueue<T>::~CycleQueue() {
    pthread_mutex_destroy(&mutex);
    pthread_cond_destroy(&cond);
    for (int i = 0; i < size; ++i) {
        free(tab[i]);
    }
    free(tab);
}

template <typename T>
T *CycleQueue<T>::queue_push() {

    int current = next_to_write;

    int next_write;
    for(;;){
        //下一个要读的位置等于下一个要写的，等我写完，在读
        //不等于，就继续
        next_write = get_next_index(current);
        if(next_write != next_to_read){
            break;
        }
        //阻塞
        pthread_cond_wait(&cond,&mutex);
    }
    ALOGI("queue --- queue push data for current index : %d, pop data index : %d ", current, next_to_read);

    next_to_write = next_write;
    //通知
    pthread_cond_broadcast(&cond);

    return tab[current];
}

template <typename T>
T *CycleQueue<T>::queue_pop() {
    int current = next_to_read;

    for(;;){
        if(next_to_read != next_to_write){
            break;
        }
        pthread_cond_wait(&cond,&mutex);
    }
    ALOGI("queue --- queue pop data for current index : %d, push data index : %d ", current, next_to_write);
    next_to_read = get_next_index(current);

    pthread_cond_broadcast(&cond);
    return tab[current];
}

template <typename T>
int CycleQueue<T>::get_next_index(int current) {
    return (current + 1) % size;;
}

template <typename T>
void CycleQueue<T>::lock() {
    pthread_mutex_lock(&mutex);
}

template <typename T>
void CycleQueue<T>::unlock() {
    pthread_mutex_unlock(&mutex);
}





