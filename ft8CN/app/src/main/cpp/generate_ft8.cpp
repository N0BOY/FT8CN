//
// Created by jmsmf on 2022/6/1.
//
#include <jni.h>
#include <string>

extern "C" {
#include "common/debug.h"
#include "ft8Encoder.h"
#include "ft8/pack.h"
#include "ft8/encode.h"
#include "ft8/hash22.h"
}
#define GFSK_CONST_K 5.336446f ///< == pi * sqrt(2 / log(2))

char *Jstring2CStr(JNIEnv *env, jstring jstr) {
    char *rtn = nullptr;
    jclass clsstring = env->FindClass("java/lang/String");
    jstring strencode = env->NewStringUTF("GB2312");
    jmethodID mid = env->GetMethodID(clsstring, "getBytes", "(Ljava/lang/String;)[B");
    auto barr = (jbyteArray) env->CallObjectMethod(jstr, mid, strencode);
    int alen = env->GetArrayLength(barr);
    jbyte *ba = env->GetByteArrayElements(barr, JNI_FALSE);
    if (alen > 0) {
        rtn = (char *) malloc(alen + 1); //new char[alen+1];
        memcpy(rtn, ba, alen);
        rtn[alen] = 0;
    }
    env->ReleaseByteArrayElements(barr, ba, 0);

    return rtn;
}


extern "C"
JNIEXPORT void JNICALL
Java_com_bg7yoz_ft8cn_ft8transmit_FT8TransmitSignal_GenerateFt8(JNIEnv *env, jobject,
                                                                jstring message,
                                                                jfloat frequency,
                                                                jshortArray buffer) {
    jshort *_buffer;
    _buffer = (*env).GetShortArrayElements(buffer, nullptr);
    char *str=Jstring2CStr(env, message);
    generateFt8ToBuffer(str, frequency, _buffer);
    (*env).ReleaseShortArrayElements(buffer,_buffer,JNI_COMMIT);
    free(str);
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_bg7yoz_ft8cn_ft8transmit_GenerateFT8_pack77(JNIEnv *env, jclass, jstring msg,
                                                     jbyteArray c77) {
    jbyte *_buffer;
    _buffer = (*env).GetByteArrayElements(c77, nullptr);
    char *str=Jstring2CStr(env, msg);
    int result=pack77(str,(uint8_t *)_buffer);
    (*env).ReleaseByteArrayElements(c77,_buffer,JNI_COMMIT);
    free(str);
    return result;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_bg7yoz_ft8cn_ft8transmit_GenerateFT8_ft8_1encode(JNIEnv *env, jclass clazz,
                                                          jbyteArray payload, jbyteArray tones) {
    jbyte *_payload;
    jbyte *_tones;
    _payload = (*env).GetByteArrayElements(payload, nullptr);
    _tones = (*env).GetByteArrayElements(tones, nullptr);
    ft8_encode((uint8_t *)_payload,(uint8_t *)_tones);
    (*env).ReleaseByteArrayElements(payload,_payload,JNI_COMMIT);
    (*env).ReleaseByteArrayElements(tones,_tones,JNI_COMMIT);


}
extern "C"
JNIEXPORT void JNICALL
Java_com_bg7yoz_ft8cn_ft8transmit_GenerateFT8_gfsk_1pulse(JNIEnv *env, jclass clazz, jint n_spsym,
                                                          jfloat symbol_bt, jfloatArray pulse) {
    jfloat *_pulse;
    _pulse=(*env).GetFloatArrayElements(pulse, nullptr);

    for (int i = 0; i < 3 * n_spsym; ++i)
    {
        float t = i / (float)n_spsym - 1.5f;
        float arg1 = GFSK_CONST_K * symbol_bt * (t + 0.5f);
        float arg2 = GFSK_CONST_K * symbol_bt * (t - 0.5f);
        _pulse[i] = (erff(arg1) - erff(arg2)) / 2;
    }
    (*env).ReleaseFloatArrayElements(pulse,_pulse,JNI_COMMIT);

}
extern "C"
JNIEXPORT void JNICALL
Java_com_bg7yoz_ft8cn_ft8transmit_GenerateFT8_synth_1gfsk(JNIEnv *env, jclass clazz,
                                                          jbyteArray symbols, jint n_sym, jfloat f0,
                                                          jfloat symbol_bt, jfloat symbol_period,
                                                          jint signal_rate, jfloatArray signal,
                                                          jint offset) {
    jbyte *_symbols;
    jfloat *_signal;
    _symbols = (*env).GetByteArrayElements(symbols, nullptr);
    _signal = (*env).GetFloatArrayElements(signal, nullptr);
    synth_gfsk((uint8_t *)_symbols,n_sym,f0,symbol_bt,symbol_period,signal_rate,_signal+offset);

    (*env).ReleaseByteArrayElements(symbols,_symbols,JNI_COMMIT);
    (*env).ReleaseFloatArrayElements(signal,_signal,JNI_COMMIT);

}
extern "C"
JNIEXPORT jint JNICALL
Java_com_bg7yoz_ft8cn_ft8signal_FT8Package_getHash12(JNIEnv *env, jclass clazz, jstring callsign) {
    char *str=Jstring2CStr(env, callsign);
    uint32_t hash=hashcall_12(str);
    free(str);
    return hash;
}
extern "C"


JNIEXPORT jint JNICALL
Java_com_bg7yoz_ft8cn_ft8transmit_GenerateFT8_packFreeTextTo77(JNIEnv *env, jclass clazz,
                                                               jstring msg, jbyteArray c77) {

    jbyte *_buffer;
    _buffer = (*env).GetByteArrayElements(c77, nullptr);
    char *str=Jstring2CStr(env, msg);
    packtext77(str,(uint8_t *)_buffer);
    (*env).ReleaseByteArrayElements(c77,_buffer,JNI_COMMIT);
    free(str);
    return 0;
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_bg7yoz_ft8cn_ft8signal_FT8Package_getHash10(JNIEnv *env, jclass clazz, jstring callsign) {
    char *str=Jstring2CStr(env, callsign);
    uint32_t hash=(hashcall_10(str));
    free(str);
    return hash;
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_bg7yoz_ft8cn_ft8signal_FT8Package_getHash22(JNIEnv *env, jclass clazz, jstring callsign) {
    char *str=Jstring2CStr(env, callsign);
    u_int32_t hash=hashcall_22(str);
    free(str);
    return hash;
}