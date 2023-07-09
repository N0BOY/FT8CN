
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <math.h>
#include <stdbool.h>
#include "ft8/decode.h"
#include "fft/kiss_fftr.h"
#include "common/debug.h"

#define LOG_LEVEL LOG_INFO



/// Configuration options for FT4/FT8 monitor
typedef struct {
    float f_min;             ///< 最低频率界限，Lower frequency bound for analysis
    float f_max;             ///< 最高频率界限，Upper frequency bound for analysis
    int sample_rate;         ///< 采样率，Sample rate in Hertz
    int time_osr;            ///< 时间过采样率，Number of time subdivisions
    int freq_osr;            ///< 频率过采样率，Number of frequency subdivisions
    ftx_protocol_t protocol; ///< Protocol: FT4 or FT8
} monitor_config_t;

/// FT4/FT8 monitor object that manages DSP processing of incoming audio data
/// and prepares a waterfall object
typedef struct {
    float symbol_period; ///< FT4/FT8符号周期（秒）。FT4/FT8 symbol period in seconds
    int block_size;      ///< 每个符号（FSK）的样本数。Number of samples per symbol (block)
    int subblock_size;   ///< 分析移动的大小（样本数）。Analysis shift size (number of samples)
    int nfft;            ///< FFT size
    float fft_norm;      ///< FFT归一化因子。FFT normalization factor
    float *window;       ///< STFT分析的窗口函数（nfft样本）。Window function for STFT analysis (nfft samples)
    float *last_frame;   ///< 当前STFT分析框架（nfft样本）。Current STFT analysis frame (nfft samples)
    waterfall_t wf;      ///< 瀑布对象。Waterfall object
    float max_mag;       ///< 最大检测量（调试统计）。Maximum detected magnitude (debug stats)

    // KISS FFT housekeeping variables
    void *fft_work;        ///< FFT需要的工作区域。Work area required by Kiss FFT
    kiss_fftr_cfg fft_cfg; ///< Kiss FFT需要的设置信息。Kiss FFT housekeeping object
} monitor_t;


void monitor_init(monitor_t *me, const monitor_config_t *cfg);
void monitor_free(monitor_t* me);
void monitor_process(monitor_t *me, const float *frame);
void monitor_reset(monitor_t *me);
