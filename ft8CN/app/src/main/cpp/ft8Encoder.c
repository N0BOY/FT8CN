//
// Created by jmsmf on 2022/6/1.
//

#include "ft8Encoder.h"

//#define LOG_LEVEL LOG_INFO

#define FT8_SYMBOL_BT 2.0f /// 符号平滑滤波器带宽因子（BT）
#define FT4_SYMBOL_BT 1.0f /// 符号平滑滤波器带宽因子（BT）
#define GFSK_CONST_K 5.336446f ///< == pi * sqrt(2 / log(2))


/// 生成高斯平滑脉冲
/// 脉冲理论上是无限长的，然而，这里它被截断为符号长度的3倍。
/// 这意味着脉冲阵列必须有空间容纳3*n_spsym元素。
/// @param[in] n_spsym 每个符号的样本数 Number of samples per symbol
/// @param[in] b 形状参数（为FT8/FT4定义的值）
/// @param[out] pulse 脉冲采样输出阵列
///
void gfsk_pulse(int n_spsym, float symbol_bt, float *pulse) {
    for (int i = 0; i < 3 * n_spsym; ++i) {
        float t = i / (float) n_spsym - 1.5f;
        float arg1 = GFSK_CONST_K * symbol_bt * (t + 0.5f);
        float arg2 = GFSK_CONST_K * symbol_bt * (t - 0.5f);
        pulse[i] = (erff(arg1) - erff(arg2)) / 2;
    }
}


/// 使用GFSK相位整形合成波形数据。
/// 输出波形将包含n_sym个符号。
/// @param[in] symbols 符号（音调）数组 (0-7 for FT8)
/// @param[in] n_sym 符号数组中的符号数
/// @param[in] f0 符号0的音频频率（赫兹） (载波频率)
/// @param[in] symbol_bt 符号平滑滤波器带宽 (2 for FT8, 1 for FT4)
/// @param[in] symbol_period 符号周期（持续时间），秒
/// @param[in] signal_rate 合成信号的采样率，赫兹
/// @param[out] signal 信号波形样本的输出阵列（应为n_sym*n_spsym样本留出空间）
///
void synth_gfsk(const uint8_t *symbols, int n_sym, float f0, float symbol_bt, float symbol_period,
                int signal_rate, float *signal) {
    int n_spsym = (int) (0.5f + (float)signal_rate * symbol_period); // 每个符号的样本数12000*0.16=1920
    int n_wave = n_sym * n_spsym;                            // 输出样本数79*1920=151680
    float hmod = 1.0f;


    // 计算平滑的频率波形。
    // Length = (nsym+2)*n_spsym samples, 首个和最后一个扩展符号
    float dphi_peak = 2 * M_PI * hmod / n_spsym;

    //此处是与采样率有关，采样率提高后，可能会有闪退的问题
    float *dphi;//此处使用内存申请的方式，而不是原来数组的方式，因为是采样率过高时，会报内存出错。
    dphi = malloc(sizeof(float) * (n_wave + 2 * n_spsym));
    if (dphi==0) return;//内存申请失败
    //float dphi[n_wave + 2 * n_spsym];//原来的方式

    // 频率上移f0
    for (int i = 0; i < n_wave + 2 * n_spsym; ++i) {
        dphi[i] = 2 * M_PI * f0 / signal_rate;
    }

    //float pulse[3 * n_spsym];
    float *pulse=(float *) malloc(sizeof(float)*3 * n_spsym);
    gfsk_pulse(n_spsym, symbol_bt, pulse);

    for (int i = 0; i < n_sym; ++i) {
        int ib = i * n_spsym;
        for (int j = 0; j < 3 * n_spsym; ++j) {
            dphi[j + ib] += dphi_peak * symbols[i] * pulse[j];
        }
    }
    // 在开头和结尾添加伪符号，音调值分别等于第一个符号和最后一个符号
    for (int j = 0; j < 2 * n_spsym; ++j) {
        dphi[j] += dphi_peak * pulse[j + n_spsym] * symbols[0];
        dphi[j + n_sym * n_spsym] += dphi_peak * pulse[j] * symbols[n_sym - 1];
    }
    // 计算并插入音频波形
    float phi = 0;
    for (int k = 0; k < n_wave; ++k) { // 不包括虚拟符号
        signal[k] = sinf(phi);
        phi = fmodf(phi + dphi[k + n_spsym], 2 * M_PI);
    }
    // 对第一个和最后一个符号应用封套成形，此处是前后增加斜坡函数，
    int n_ramp = n_spsym / 8;//240个样本，20毫秒，T/8
    for (int i = 0; i < n_ramp; ++i) {
        float env = (1 - cosf(2 * M_PI * i / (2 * n_ramp))) / 2;
        signal[i] *= env;
        signal[n_wave - 1 - i] *= env;
    }
    free(pulse);
    free(dphi);//要释放掉内存
}

//此代码已经弃用
void generateFt8ToBuffer(char *message, float frequency, short *buffer) {
// 首先，将文本数据打包为二进制消息
    uint8_t packed[FTX_LDPC_K_BYTES];//91位，包括CRC。
    int rc = pack77(message, packed);//生成数据
    if (rc < 0) {
        //LOGE("Cannot parse message!\n");
        //LOGE("RC = %d\n", rc);
        return;
    }


    //int num_tones = FT8_NN;//符号数量：FT8是79个，FT4是105个。
    //float symbol_period = FT8_SYMBOL_PERIOD;//FT8_SYMBOL_PERIOD=0.160f
    float symbol_bt = FT8_SYMBOL_BT;//FT8_SYMBOL_BT=2.0f
    float slot_time = FT8_SLOT_TIME;//FT8_SLOT_TIME=15f

    // 其次，将二进制消息编码为FSK音调序列
    uint8_t tones[FT8_NN]; // 79音调（符号）数组
    ft8_encode(packed, tones);



    // 第三，将FSK音调转换为音频信号b
    //int sample_rate = FT8_SAMPLE_RATE;//采样率
    int num_samples = (int) (0.5f + FT8_NN * FT8_SYMBOL_PERIOD *
                                    FT8_SAMPLE_RATE); // 数据信号中的采样数0.5+79*0.16*12000
    //int num_silence = (slot_time * sample_rate - num_samples) / 2;           // 两端填充静音到15秒（15*12000-num_samples）/2（1.18秒的样本数）
    int num_silence = 20;//把前面的静音时长缩短为20毫秒，留出时间给解码
    //int num_total_samples = num_silence + num_samples + num_silence;         // 填充信号中的样本数2.36秒+12.64秒=15秒的样本数
    float signal[Ft8num_samples];
    //Ft8num_sampleFT8声音的总采样数，不是字节数。15*12000
    for (int i = 0; i < Ft8num_samples; i++)//把数据全部静音。
    {
        signal[i] = 0;
        //buffer[i + num_samples + num_silence] = 0;
    }

    // 合成波形数据（信号）并将其保存为WAV文件
    synth_gfsk(tones, FT8_NN, frequency, symbol_bt, FT8_SYMBOL_PERIOD, FT8_SAMPLE_RATE,
               signal + num_silence);


    for (int i = 0; i < Ft8num_samples; i++) {
        float x = signal[i];
        if (x > 1.0)
            x = 1.0;
        else if (x < -1.0)
            x = -1.0;
        buffer[i] = (short) (0.5 + (x * 32767.0));
    }


    //save_wav(signal, num_total_samples, sample_rate, wav_path);

}
