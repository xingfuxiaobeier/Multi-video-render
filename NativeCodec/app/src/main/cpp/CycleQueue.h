//
// Created by mtime on 2018/7/3.
//

#ifndef NATIVECODEC_CYCLEQUEUE_H
#define NATIVECODEC_CYCLEQUEUE_H


#include <pthread.h>

template <class T>
class CycleQueue {
public:
    CycleQueue();
    CycleQueue(int num);
    CycleQueue& operator=(const CycleQueue&) = delete;
    CycleQueue(CycleQueue&) = delete;


    ~CycleQueue();
    T* queue_push();
    T* queue_pop();
    void lock();
    void unlock();


private:
    int size;
    T ** tab;
    int next_to_write;
    int next_to_read;
    pthread_mutex_t mutex;
    pthread_cond_t cond;
    int get_next_index(int current);
};


#endif //NATIVECODEC_CYCLEQUEUE_H
