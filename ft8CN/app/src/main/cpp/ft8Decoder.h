//
// Created by jmsmf on 2022/4/24.
//
//
#include "ft8/decode.h"
#include "monitor_opr.h"
#include "ft8/constants.h"
#include <time.h>

//const int kMin_score = 10; // 候选人的最低同步分数阈值。Minimum sync score threshold for candidates
const int kMax_candidates = 120;//最大候选人数量

const int kMax_decoded_messages = 100;
//const int kMax_decoded_messages = 50;
const int kLDPC_iterations = 20;//LDPC（低密度奇偶校验）的迭代次数，数值越大，精度越高，速度越慢
const int deep_kLDPC_iterations = 200;//LDPC（低密度奇偶校验）的迭代次数
const int fast_kLDPC_iterations = 20;//LDPC（低密度奇偶校验）的迭代次数

typedef struct {
    long long utcTime;//UTC时间
    int num_samples;//采样率
    int num_candidates;
    int num_decoded;
    message_t decoded[kMax_decoded_messages];//kMax_decoded_messages=50
    // 哈希表指针列表（指针数组）
    message_t *decoded_hashtable[kMax_decoded_messages];

    // Find top candidates by Costas sync score and localize them in time and frequency
    // 从科斯塔斯阵列（Costas）寻找最佳候选，并在时间和频率上对其进行本地化。候选数组最大120个
    // candidate_t定义在decode.h
    candidate_t candidate_list[kMax_candidates];//kMax_candidates=120

    monitor_t mon;
    monitor_config_t mon_cfg;
    uint8_t a91[FTX_LDPC_K_BYTES];//用于生成减法代码的数据
    int kLDPC_iterations;//ldpc 迭代次数，数值越大，精度越高，速度越慢：20或100
} decoder_t;

typedef struct {
    int64_t utcTime;//消息的UTC时间
    bool isValid;//是否为有效消息
    int snr;//信噪比
    candidate_t candidate;//消息的原始信号数据
    float time_sec;//时间偏移值
    float freq_hz;//频率偏移值
    message_t message;//解码后的消息
    decode_status_t status;
} ft8_message;

static const int kFreq_osr = 2; // 频率过采样率。Frequency oversampling rate (bin subdivision)
static const int kTime_osr = 2; // 时间过采样率。Time oversampling rate (symbol subdivision)

//把信号FFT,在解码decoder中减去信号
void signalToFFT(decoder_t *decoder,float signal[], int sample_rate);
//初始化解码器所需要的参数，最后通过指针的方式传递给java
void *init_decoder(int64_t utcTime, int sample_rate, int num_samples, bool is_ft8);

void delete_decoder(decoder_t *decoder);

void decoder_monitor_press(float signal[], decoder_t *decoder);

int decoder_ft8_find_sync(decoder_t *decoder);

ft8_message decoder_ft8_analysis(int idx, decoder_t *decoder);

void decoder_ft8_reset(decoder_t *decoder,long utcTime,int num_samples);

void recode(int a174[],int a79[]);

