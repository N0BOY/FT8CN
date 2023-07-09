#include <jni.h>

//
// Created by jmsmf on 2022/6/11.
//

extern "C" {
#include "common/debug.h"
#include "spectrum_data.h"
}
extern "C"
JNIEXPORT void JNICALL
Java_com_bg7yoz_ft8cn_ui_SpectrumFragment_getFFTData(JNIEnv *env, jobject thiz, jintArray data,
                                                     jintArray fft_data) {

    int arr_len=env->GetArrayLength(data);
    //将java数组复制到c数组中
    auto c_array=(jint *) malloc(arr_len * sizeof(arr_len));

    env->GetIntArrayRegion(data,0,arr_len,c_array);
    auto *raw_data = (float *) malloc(sizeof(float) * arr_len);
    for (int i = 0; i < arr_len; i++) {
        raw_data[i] = c_array[i] / 32768.0f;
    }


    jint temp[arr_len/2];

    do_fftr(raw_data,arr_len,temp);
    (*env).SetIntArrayRegion(fft_data,0,arr_len/2,temp);

    free(c_array);
    free(raw_data);

//
//    jint *arr;
//    jint length;
//    arr = (*env).GetIntArrayElements(data, NULL);
//    length = (*env).GetArrayLength(data);
//    float *raw_data = (float *) malloc(sizeof(float) * length);
//    for (int i = 0; i < length; i++) {
//        raw_data[i] = arr[i] / 32768.0f;
//    }
//
//    jint temp[length/2];
//
//    do_fftr(raw_data,length,temp);
//
//    (*env).SetIntArrayRegion(fft_data,0,length/2,temp);
//    free(raw_data);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_bg7yoz_ft8cn_ui_SpectrumFragment_getFFTDataRaw(JNIEnv *env, jobject thiz, jintArray data,
                                                        jintArray fft_data) {

    int arr_len=env->GetArrayLength(data);
    //将java数组复制到c数组中
    auto c_array=(jint *) malloc(arr_len * sizeof(arr_len));

    env->GetIntArrayRegion(data,0,arr_len,c_array);
    auto *raw_data = (float *) malloc(sizeof(float) * arr_len);
    for (int i = 0; i < arr_len; i++) {
        raw_data[i] = c_array[i] / 32768.0f;
    }


    jint temp[arr_len/2];

    do_fftr_raw(raw_data,arr_len,temp);
    (*env).SetIntArrayRegion(fft_data,0,arr_len/2,temp);

    free(c_array);
    free(raw_data);


//
//    jint *arr;
//    jint length;
//    arr = (*env).GetIntArrayElements(data, NULL);
//    length = (*env).GetArrayLength(data);
//    float *raw_data = (float *) malloc(sizeof(float) * length);
//    for (int i = 0; i < length; i++) {
//        raw_data[i] = arr[i] / 32768.0f;
//    }
//
//    jint temp[length/2];
//
//    do_fftr_raw(raw_data,length,temp);
//
//    (*env).SetIntArrayRegion(fft_data,0,length/2,temp);
//    free(raw_data);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_bg7yoz_ft8cn_ui_SpectrumView_getFFTData(JNIEnv *env, jobject thiz, jintArray data,
                                                 jintArray fft_data) {
    int arr_len=env->GetArrayLength(data);
    //将java数组复制到c数组中
    auto c_array=(jint *) malloc(arr_len * sizeof(arr_len));

    env->GetIntArrayRegion(data,0,arr_len,c_array);
    auto *raw_data = (float *) malloc(sizeof(float) * arr_len);
    for (int i = 0; i < arr_len; i++) {
        raw_data[i] = c_array[i] / 32768.0f;
    }


    jint temp[arr_len/2];

    do_fftr(raw_data,arr_len,temp);
    (*env).SetIntArrayRegion(fft_data,0,arr_len/2,temp);

    free(c_array);
    free(raw_data);


//    jint *arr;
//    jint length;
//    arr = (*env).GetIntArrayElements(data, NULL);
//    length = (*env).GetArrayLength(data);
//    float *raw_data = (float *) malloc(sizeof(float) * length);
//    for (int i = 0; i < length; i++) {
//        raw_data[i] = arr[i] / 32768.0f;
//    }
//
//    jint temp[length/2];
//
//    do_fftr(raw_data,length,temp);
//
//    (*env).SetIntArrayRegion(fft_data,0,length/2,temp);
//    free(raw_data);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_bg7yoz_ft8cn_ui_SpectrumView_getFFTDataRaw(JNIEnv *env, jobject thiz, jintArray data,
                                                    jintArray fft_data) {
    int arr_len=env->GetArrayLength(data);
    //将java数组复制到c数组中
    auto c_array=(jint *) malloc(arr_len * sizeof(arr_len));

    env->GetIntArrayRegion(data,0,arr_len,c_array);
    auto *raw_data = (float *) malloc(sizeof(float) * arr_len);
    for (int i = 0; i < arr_len; i++) {
        raw_data[i] = c_array[i] / 32768.0f;
    }


    jint temp[arr_len/2];

    do_fftr_raw(raw_data,arr_len,temp);
    (*env).SetIntArrayRegion(fft_data,0,arr_len/2,temp);

    free(c_array);
    free(raw_data);

//    jint *arr;
//    jint length;
//    arr = (*env).GetIntArrayElements(data, NULL);
//    length = (*env).GetArrayLength(data);
//    float *raw_data = (float *) malloc(sizeof(float) * length);
//    for (int i = 0; i < length; i++) {
//        raw_data[i] = arr[i] / 32768.0f;
//    }
//
//    jint temp[length/2];
//    //jint *fftdata;
//    //fftdata=(*env).GetIntArrayElements(fft_data, NULL);
//
//    do_fftr_raw(raw_data,length,temp);
//
//    (*env).SetIntArrayRegion(fft_data,0,length/2,temp);
//    free(raw_data);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_bg7yoz_ft8cn_ui_SpectrumFragment_getFFTDataFloat(JNIEnv *env, jobject thiz,
                                                          jfloatArray data, jintArray fft_data) {


    int arr_len=env->GetArrayLength(data);
    //将java数组复制到c数组中
    auto *c_array=(jfloat *) malloc(arr_len * sizeof(arr_len));

    env->GetFloatArrayRegion(data,0,arr_len,c_array);
    jint temp[arr_len/2];

    do_fftr(c_array,arr_len,temp);
    (*env).SetIntArrayRegion(fft_data,0,arr_len/2,temp);

    free(c_array);



//    jfloat *arr;
//    jint length;
//    arr = (*env).GetFloatArrayElements(data, NULL);
//    length = (*env).GetArrayLength(data);
//
//    jint temp[length/2];
//
//    do_fftr(arr,length,temp);
//
//    (*env).SetIntArrayRegion(fft_data,0,length/2,temp);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_bg7yoz_ft8cn_ui_SpectrumFragment_getFFTDataRawFloat(JNIEnv *env, jobject thiz,
                                                             jfloatArray data, jintArray fft_data) {

    int arr_len=env->GetArrayLength(data);
    //将java数组复制到c数组中
    auto *c_array=(jfloat *) malloc(arr_len * sizeof(arr_len));

    env->GetFloatArrayRegion(data,0,arr_len,c_array);
    jint temp[arr_len/2];

    do_fftr_raw(c_array,arr_len,temp);
    (*env).SetIntArrayRegion(fft_data,0,arr_len/2,temp);

    free(c_array);



//    jfloat *arr;
//    jint length;
//    arr = (*env).GetFloatArrayElements(data, NULL);
//    length = (*env).GetArrayLength(data);
//
//    jint temp[length/2];
//    do_fftr_raw(arr,length,temp);

//    (*env).SetIntArrayRegion(fft_data,0,length/2,temp);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_bg7yoz_ft8cn_ui_SpectrumView_getFFTDataFloat(JNIEnv *env, jobject thiz, jfloatArray data,
                                                      jintArray fft_data) {

    int arr_len=env->GetArrayLength(data);
    //将java数组复制到c数组中
    auto *c_array=(jfloat *) malloc(arr_len * sizeof(arr_len));

    //env->GetFloatArrayRegion(data,0,arr_len,c_array);
    (*env).GetFloatArrayRegion(data,0,arr_len,c_array);
    jint temp[arr_len/2];

    do_fftr(c_array,arr_len,temp);
    (*env).SetIntArrayRegion(fft_data,0,arr_len/2,temp);
    free(c_array);

//    jfloat *arr;
//    jint length;
//    arr = (*env).GetFloatArrayElements(data, NULL);
//    length = (*env).GetArrayLength(data);
//
//    jint temp[length/2];
//
//    do_fftr(arr,length,temp);
//
//    (*env).SetIntArrayRegion(fft_data,0,length/2,temp);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_bg7yoz_ft8cn_ui_SpectrumView_getFFTDataRawFloat(JNIEnv *env, jobject thiz,
                                                         jfloatArray data, jintArray fft_data) {


    int arr_len=env->GetArrayLength(data);
    //将java数组复制到c数组中
    auto *c_array=(jfloat *) malloc(arr_len * sizeof(arr_len));

    //env->GetFloatArrayRegion(data,0,arr_len,c_array);
    (*env).GetFloatArrayRegion(data,0,arr_len,c_array);
    jint temp[arr_len/2];

    do_fftr_raw(c_array,arr_len,temp);
    (*env).SetIntArrayRegion(fft_data,0,arr_len/2,temp);

    free(c_array);

//    jfloat *arr;
//    jint length;
//    arr = (*env).GetFloatArrayElements(data, NULL);
//    length = (*env).GetArrayLength(data);
//
//    jint temp[length/2];
//
//    do_fftr_raw(arr,length,temp);
//
//    (*env).SetIntArrayRegion(fft_data,0,length/2,temp);
}
