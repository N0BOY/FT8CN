

#include <string.h>
#include <stdio.h>
#include <math.h>
#include <stdbool.h>

#include "ft8/constants.h"
#include "common/debug.h"
#include "fft/kiss_fftr.h"

/**
 * 对符号进行快速傅里叶变换，以1920块数据变换，生成960块。0~3000Hz
 * normalization=1对数据归一化处理，方便显示FT8信号的频率
 * @param voiceData 声音数据
 * @param dataSize 声音数据的大小
 * @param fftData fft数据
 * @param normalization 是否是归一化处理
 */
void do_fftr(float* voiceData, int dataSize, int* fftData);
void do_fftr_raw(float *voiceData, int dataSize, int *fftData);
