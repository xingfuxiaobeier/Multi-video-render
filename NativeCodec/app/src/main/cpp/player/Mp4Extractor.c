//
// Created by huibin on 15/08/2017.
//


#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <math.h>

#include "Mp4Extractor.h"
#include "IsmartvBase.h"

char *videoinfo_mem(char *haystack, unsigned int sizehaystack, char *needle, unsigned int sizeneedle) {
    int i = 0;
    int end = sizehaystack - sizeneedle;
    for (i = 0; i < end; ++i) {
        if (memcmp(haystack + i, needle, sizeneedle) == 0) {
            return haystack + i;
        }
    }
    return NULL;
}

void *videoinfo_find(char *filename, void *find, int size, int resultSize) {
    FILE *fp = NULL;
    char *buffer = NULL;
    char *result = NULL;
    char *pos = NULL;
    unsigned int bufferSize = 2048;
    double filesize = 0;
    double split = 16;
    double splitsize = 0;
    double start = 0;
    double end = 0;
    double i = 0;
    unsigned int read = 0;

    if (resultSize > bufferSize) {
        resultSize = bufferSize;
    }

    buffer = malloc(bufferSize);
    if (buffer == NULL) {
        return NULL;
    }

    fp = fopen(filename, "rb");
    if (fp == NULL) {
        free(buffer);
        return NULL;
    }

    fseek(fp, 0, SEEK_END);
    filesize = ftell(fp);
    rewind(fp);

    split = ceil(filesize / 100000);
    splitsize = ceil(filesize / split);

    for (i = split - 1; i >= 0; --i) {
        start = (i * splitsize);
        end = start + splitsize;
        fseek(fp, start, SEEK_SET);

        while ((read = fread(buffer, 1, bufferSize, fp)) != 0) {
            if ((pos = videoinfo_mem(buffer, bufferSize, find, size)) != NULL) {
                result = malloc(resultSize);
                memcpy(result, pos, resultSize);
                i = -1;
                break;
            }

            if (read != bufferSize || ftell(fp) >= end) {
                break; // go onto next split
            }
        }

    }

    fclose(fp);
    free(buffer);
    return result;
}

int64_t videoinfo_flip(int64_t val) {
    int64_t new = 0;
    new += (val & 0x000000FF) << 24;
    new += (val & 0xFF000000) >> 24;
    new += (val & 0x0000FF00) << 8;
    new += (val & 0x00FF0000) >> 8;

    return new;
}

int64_t videoinfo_duration(const char *filename) {
    int64_t duration = 0;
    char version = 0;
    void *data = NULL;
    char *pos = NULL;
    int64_t timescale = 0;
    int64_t timeunits = 0;
    size_t bytesize = 4;

    data = videoinfo_find(filename, "mvhd", 4, 64);
    if (data == NULL) {
        goto clean;
    }

    pos = (char *) data;
    pos += 4; // skip mvhd

    version = *pos++;
    pos += 3; //skip flags

    if (version == 1) {
        bytesize = 8;
    } else {
        bytesize = 4;
    }

    pos += bytesize; // skip created date
    pos += bytesize; // skip modified date

    memcpy(&timescale, pos, 4);
    memcpy(&timeunits, pos + 4, bytesize);

    timescale = videoinfo_flip(timescale);
    timeunits = videoinfo_flip(timeunits);

    if (timescale > 0 && timeunits > 0) {
        duration = (timeunits * 1000) / timescale;
    }

    clean:
    free(data);
    return duration;
}

int64_t Mp4Extractor_getDuration(const char *source) {
    return videoinfo_duration(source);
}
