//
// Created by jmsmf on 2022/6/2.
//

#include <jni.h>
#include <string>


extern "C" {
#include "common/debug.h"
#include "ft8Decoder.h"
#include "ft8Encoder.h"
}

//
////将char类型转换成jstring类型
//jstring CStr2Jstring(JNIEnv *env, const char *pat) {
//    // 定义java String类 strClass
//    jclass strClass = (env)->FindClass("java/lang/String");
//    // 获取java String类方法String(byte[],String)的构造器,用于将本地byte[]数组转换为一个新String
//    jmethodID ctorID = (env)->GetMethodID(strClass, "<init>", "([BLjava/lang/String;)V");
//    // 建立byte数组
//    jbyteArray bytes = (env)->NewByteArray((jsize) strlen(pat));
//    // 将char* 转换为byte数组
//    (env)->SetByteArrayRegion(bytes, 0, (jsize) strlen(pat), (jbyte *) pat);
//    //设置String, 保存语言类型,用于byte数组转换至String时的参数
//    jstring encoding = (env)->NewStringUTF("GB2312");
//    //将byte数组转换为java String,并输出
//    return (jstring) (env)->NewObject(strClass, ctorID, bytes, encoding);
//
//}
//
//char *Jstring2CStr(JNIEnv *env, jstring jstr) {
//    char *rtn = NULL;
//    jclass clsstring = env->FindClass("java/lang/String");
//    jstring strencode = env->NewStringUTF("GB2312");
//    jmethodID mid = env->GetMethodID(clsstring, "getBytes", "(Ljava/lang/String;)[B");
//    jbyteArray barr = (jbyteArray) env->CallObjectMethod(jstr, mid, strencode);
//    jsize alen = env->GetArrayLength(barr);
//    jbyte *ba = env->GetByteArrayElements(barr, JNI_FALSE);
//    if (alen > 0) {
//        rtn = (char *) malloc(alen + 1); //new char[alen+1];
//        memcpy(rtn, ba, alen);
//        rtn[alen] = 0;
//    }
//    env->ReleaseByteArrayElements(barr, ba, 0);
//
//    return rtn;
//}

extern "C"
JNIEXPORT void JNICALL
Java_com_bg7yoz_ft8cn_ft8listener_FT8SignalListener_DecoderFt8Reset(JNIEnv *env, jobject thiz,
                                                                    jlong decoder, jlong utcTime,
                                                                    jint num_samples) {
    decoder_t *dd;
    dd = (decoder_t *) decoder;
    decoder_ft8_reset(dd, utcTime, num_samples);
    //dd->utcTime=utcTime;
}
extern "C"
JNIEXPORT void JNICALL
Java_com_bg7yoz_ft8cn_ft8listener_FT8SignalListener_DeleteDecoder(JNIEnv *env, jobject,
                                                                  jlong decoder) {
    decoder_t *dd;
    dd = (decoder_t *) decoder;
    delete_decoder(dd);
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_bg7yoz_ft8cn_ft8listener_FT8SignalListener_DecoderFt8Analysis(JNIEnv *env, jobject,
                                                                       jint idx,
                                                                       jlong decoder,
                                                                       jobject ft8Message) {
    decoder_t *dd;
    dd = (decoder_t *) decoder;


    ft8_message message = decoder_ft8_analysis(idx, dd);


    jclass objectClass = env->FindClass("com/bg7yoz/ft8cn/Ft8Message");

    jfieldID utcTime = env->GetFieldID(objectClass, "utcTime", "J");
    jfieldID isValid = env->GetFieldID(objectClass, "isValid", "Z");
    jfieldID time_sec = env->GetFieldID(objectClass, "time_sec", "F");
    jfieldID freq_hz = env->GetFieldID(objectClass, "freq_hz", "F");
    jfieldID score = env->GetFieldID(objectClass, "score", "I");
    jfieldID snr = env->GetFieldID(objectClass, "snr", "I");
    jfieldID messageHash = env->GetFieldID(objectClass, "messageHash", "I");
    env->SetBooleanField(ft8Message, isValid, message.isValid);

    jfieldID i3 = env->GetFieldID(objectClass, "i3", "I");
    jfieldID n3 = env->GetFieldID(objectClass, "n3", "I");
    jfieldID callsignFrom = env->GetFieldID(objectClass, "callsignFrom", "Ljava/lang/String;");
    jfieldID callsignTo = env->GetFieldID(objectClass, "callsignTo", "Ljava/lang/String;");
    jfieldID extraInfo = env->GetFieldID(objectClass, "extraInfo", "Ljava/lang/String;");
    jfieldID maidenGrid = env->GetFieldID(objectClass, "maidenGrid", "Ljava/lang/String;");
    jfieldID report = env->GetFieldID(objectClass, "report", "I");
    jfieldID callFromHash10 = env->GetFieldID(objectClass, "callFromHash10", "J");
    jfieldID callFromHash12 = env->GetFieldID(objectClass, "callFromHash12", "J");
    jfieldID callFromHash22 = env->GetFieldID(objectClass, "callFromHash22", "J");
    jfieldID callToHash10 = env->GetFieldID(objectClass, "callToHash10", "J");
    jfieldID callToHash12 = env->GetFieldID(objectClass, "callToHash12", "J");
    jfieldID callToHash22 = env->GetFieldID(objectClass, "callToHash22", "J");


    if (message.isValid) {

        env->SetLongField(ft8Message, utcTime, message.utcTime);
        env->SetFloatField(ft8Message, time_sec, message.time_sec);
        env->SetFloatField(ft8Message, freq_hz, message.freq_hz);
        env->SetIntField(ft8Message, score, message.candidate.score);
        env->SetIntField(ft8Message, snr, message.snr);
        //env->SetObjectField(ft8Message,messageText,env->NewStringUTF(message.message.text));
        env->SetIntField(ft8Message, messageHash, message.message.hash);

        env->SetIntField(ft8Message, i3, message.message.i3);
        env->SetIntField(ft8Message, n3, message.message.n3);
        env->SetObjectField(ft8Message, callsignFrom, env->NewStringUTF(message.message.call_de));
        env->SetObjectField(ft8Message, callsignTo, env->NewStringUTF(message.message.call_to));
        env->SetObjectField(ft8Message, extraInfo, env->NewStringUTF(message.message.extra));
        env->SetObjectField(ft8Message, maidenGrid, env->NewStringUTF(message.message.maidenGrid));
        env->SetIntField(ft8Message, report, message.message.report);
        env->SetLongField(ft8Message, callFromHash10,
                          (long long) message.message.call_de_hash.hash10);
        env->SetLongField(ft8Message, callFromHash12,
                          (long long) message.message.call_de_hash.hash12);
        env->SetLongField(ft8Message, callFromHash22,
                          (long long) message.message.call_de_hash.hash22);
        env->SetLongField(ft8Message, callToHash10,
                          (long long) message.message.call_to_hash.hash10);
        env->SetLongField(ft8Message, callToHash12,
                          (long long) message.message.call_to_hash.hash12);
        env->SetLongField(ft8Message, callToHash22,
                          (long long) message.message.call_to_hash.hash22);

    }
    return message.isValid;
}
extern "C"
JNIEXPORT jint JNICALL
Java_com_bg7yoz_ft8cn_ft8listener_FT8SignalListener_DecoderFt8FindSync(JNIEnv *env, jobject,
                                                                       jlong decoder) {
    decoder_t *dd;
    dd = (decoder_t *) decoder;
    return decoder_ft8_find_sync(dd);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_bg7yoz_ft8cn_ft8listener_FT8SignalListener_DecoderMonitorPress(JNIEnv *env, jobject,
                                                                        jintArray buffer,
                                                                        jlong decoder) {
    decoder_t *dd;
    dd = (decoder_t *) decoder;


    //    jfloat *arr;
//    jint length;
//    arr = (*env).GetFloatArrayElements(buffer, NULL);
//    decoder_monitor_press(arr, dd);
    int arr_len = env->GetArrayLength(buffer);
    //将java数组复制到c数组中
    auto *c_array = (jint *) malloc(arr_len * sizeof(arr_len));

    //env->GetFloatArrayRegion(buffer,0,arr_len,c_array);
    (*env).GetIntArrayRegion(buffer, 0, arr_len, c_array);
    auto *raw_data = (float_t *) malloc(sizeof(float_t) * arr_len);

    for (int i = 0; i < arr_len; i++) {
        raw_data[i] = c_array[i] / 32768.0f;
    }

    decoder_monitor_press(raw_data, dd);
    free(raw_data);
    free(c_array);



//    jint *arr;
//    jint length;
//    arr = (*env).GetIntArrayElements(buffer, nullptr);
//    length = (*env).GetArrayLength(buffer);
//    auto *raw_data = (float_t *) malloc(sizeof(float_t) * length);
//
//    for (int i = 0; i < length; i++) {
//        raw_data[i] = arr[i] / 32768.0f;
//    }
//
//    decoder_monitor_press(raw_data, dd);
//
//    free(raw_data);

}
extern "C"
JNIEXPORT jlong JNICALL
Java_com_bg7yoz_ft8cn_ft8listener_FT8SignalListener_InitDecoder(JNIEnv *env, jobject, jlong utcTime,
                                                                jint sampleRate, jint num_samples,
                                                                jboolean isFt8) {
    return (long) init_decoder(utcTime, sampleRate, num_samples, isFt8);
}


extern "C"
JNIEXPORT void JNICALL
Java_com_bg7yoz_ft8cn_ft8listener_FT8SignalListener_DecoderMonitorPressFloat(JNIEnv *env,
                                                                             jobject thiz,
                                                                             jfloatArray buffer,
                                                                             jlong decoder) {
    decoder_t *dd;
    dd = (decoder_t *) decoder;


//    jfloat *arr;
//    jint length;
//    arr = (*env).GetFloatArrayElements(buffer, NULL);
//    decoder_monitor_press(arr, dd);
    int arr_len = env->GetArrayLength(buffer);
    //将java数组复制到c数组中
    auto *c_array = (jfloat *) malloc(arr_len * sizeof(arr_len));

    //env->GetFloatArrayRegion(buffer,0,arr_len,c_array);
    (*env).GetFloatArrayRegion(buffer, 0, arr_len, c_array);
    decoder_monitor_press(c_array, dd);
    free(c_array);

}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_com_bg7yoz_ft8cn_ft8listener_FT8SignalListener_DecoderGetA91(JNIEnv *env, jobject thiz,
                                                                  jlong decoder) {
    decoder_t *dd;
    dd = (decoder_t *) decoder;

    jbyteArray array;
    array = env->NewByteArray(FTX_LDPC_K_BYTES);

    jbyte buf[FTX_LDPC_K_BYTES];
    memcpy(buf, dd->a91, FTX_LDPC_K_BYTES);

    // 使用 setIntArrayRegion 来赋值
    env->SetByteArrayRegion(array, 0, FTX_LDPC_K_BYTES, buf);
    return array;
}
extern "C"
JNIEXPORT void JNICALL
Java_com_bg7yoz_ft8cn_ft8listener_FT8SignalListener_setDecodeMode(JNIEnv *env, jobject thiz,
                                                                  jlong decoder, jboolean is_deep) {
    decoder_t *dd;
    dd = (decoder_t *) decoder;
    if (is_deep) {
        dd->kLDPC_iterations = deep_kLDPC_iterations;
    } else {
        dd->kLDPC_iterations = fast_kLDPC_iterations;
    }
}

/**
 * 把频率减去
 * @param dd
 * @param index
 * @param max_block_size
 */
void setMagToZero(decoder_t * dd ,int index,int max_block_size){
    if (index>0 && index<max_block_size){
        dd->mon.wf.mag[index]=0;
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_bg7yoz_ft8cn_ft8listener_ReBuildSignal_doSubtractSignal(JNIEnv *env, jclass clazz,
                                                                 jlong decoder,
                                                                 jbyteArray payload,
                                                                 jint sample_rate,
                                                                 jfloat frequency,
                                                                 jfloat time_sec) {
    decoder_t *dd;
    dd = (decoder_t *) decoder;

    int arr_len = env->GetArrayLength(payload);
    //将java数组复制到c数组中
    auto *c_array = (jbyte *) malloc(arr_len * sizeof(arr_len));


    //env->GetFloatArrayRegion(buffer,0,arr_len,c_array);
    (*env).GetByteArrayRegion(payload, 0, arr_len, c_array);

    uint8_t tones[FT8_NN];// 79音调（符号）数组,
    //此处是12个字节（91+7）/8，可以使用a91生成音频
    ft8_encode((uint8_t *) c_array, tones);

    //相当于二维数组，freq优先
    int max_block_size=(int) (FT8_SLOT_TIME / FT8_SYMBOL_PERIOD) * kTime_osr * kFreq_osr
            * (int) (sample_rate * FT8_SYMBOL_PERIOD / 2);
    LOG_PRINTF("max_block_size：%d",max_block_size);
    int block_size = FT8_SYMBOL_PERIOD * dd->mon_cfg.sample_rate;//1920,一个0.08秒的数据块大小，x轴的总长度
    LOG_PRINTF("block_size +++:%d", block_size);
    int freq_offset = (int) (frequency * FT8_SYMBOL_PERIOD) * kFreq_osr;//频率的偏移量,x轴
    int time_offset = (int) ((time_sec / FT8_SYMBOL_PERIOD) * kTime_osr+0.5f);// + 0.5);,y轴
    LOG_PRINTF("freq_offset +++:%f,%d", (frequency * FT8_SYMBOL_PERIOD) * kFreq_osr, freq_offset);
    LOG_PRINTF("time_offset +++:%f ,%d,time_offset:%d, time_sec:%f, freq_offset:%d, freq:%f",
               (time_sec / 0.08),
               (int) (time_sec / 0.08 + 0.5), time_offset, time_sec, freq_offset, frequency);
    for (int i = 0; i < FT8_NN; ++i) {//y轴自增循环
        int index = (i + time_offset) * 2;
        int index1 = index * block_size + freq_offset+tones[i];
        int index2 = (index + 1) * block_size + freq_offset+tones[i];
        int index3 =index1+1;
        int index4 =index2+1;
        int index5 =index1-1;
        int index6 =index2-1;
        int index7 =index1-2;
        int index8 =index2-2;
        int index9 =index1+2;
        int index10 =index2+2;

        setMagToZero(dd,index1,max_block_size);
        setMagToZero(dd,index2,max_block_size);
        setMagToZero(dd,index3,max_block_size);
        setMagToZero(dd,index4,max_block_size);
        setMagToZero(dd,index5,max_block_size);
        setMagToZero(dd,index6,max_block_size);
        setMagToZero(dd,index7,max_block_size);
        setMagToZero(dd,index8,max_block_size);
        setMagToZero(dd,index9,max_block_size);
        setMagToZero(dd,index10,max_block_size);

    }
    free(c_array);

}