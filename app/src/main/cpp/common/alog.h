//
// Created by beich on 2020/11/6.
//
#pragma once

#include <android/log.h>

#define LOG_TAG "HookLog"

extern int g_log_level;

#define _PRINT(v, format, ...) \
    do {                \
        if(g_log_level <= (v)) __android_log_print(v, LOG_TAG, format, ##__VA_ARGS__); \
    }while(0)

#define LOGV(format, ...) _PRINT(ANDROID_LOG_VERBOSE, format, ##__VA_ARGS__)
#define LOGD(format, ...) _PRINT(ANDROID_LOG_DEBUG, format, ##__VA_ARGS__)
#define LOGI(format, ...) _PRINT(ANDROID_LOG_INFO, format, ##__VA_ARGS__)
#define LOGW(format, ...) _PRINT(ANDROID_LOG_WARN, format, ##__VA_ARGS__)
#define LOGE(format, ...) _PRINT(ANDROID_LOG_ERROR, format, ##__VA_ARGS__)