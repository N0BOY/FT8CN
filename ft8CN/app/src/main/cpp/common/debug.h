#pragma once

#include <stdio.h>

#include<android/log.h>

#define LOG_DEBUG   0
#define LOG_INFO    1
#define LOG_WARN    2
#define LOG_ERROR   3
#define LOG_FATAL   4
#define LOG_LEVEL LOG_DEBUG
//#define LOG_LEVEL LOG_INFO

//#define LOG(level, ...)     if (level >= LOG_LEVEL) fprintf(stderr, __VA_ARGS__)
#define TAG "FT8_DECODER" // 这个是自定义的LOG的标识
#define LOG(level, ...)     if (level >= LOG_LEVEL) __android_log_print(ANDROID_LOG_INFO,TAG ,__VA_ARGS__)
#define LOG_PRINTF(...)     __android_log_print(ANDROID_LOG_INFO,TAG ,__VA_ARGS__)

#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG,TAG ,__VA_ARGS__) // 定义LOGD类型
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,TAG ,__VA_ARGS__) // 定义LOGI类型
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,TAG ,__VA_ARGS__) // 定义LOGW类型
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,TAG ,__VA_ARGS__) // 定义LOGE类型
#define LOGF(...) __android_log_print(ANDROID_LOG_FATAL,TAG ,__VA_ARGS__) // 定义LOGF类型