#include <stdlib.h>
#include "spectrum_data.h"

static float hann_i(int i, int N) {
    float x = sinf((float) M_PI * i / N);
    return x * x;
}

void do_fftr(float *voiceData, int dataSize, int *fftData) {
//    int block_size = FT8_SYMBOL_PERIOD * 12000; //=1920
    int fftSize = FT8_SYMBOL_PERIOD * 12000; //=1920
    float *window = (float *) malloc(
            fftSize * sizeof(window[0])); // 申请窗空间，大小是fft块大小*sizeof(windows[0])
    for (int i = 0; i < fftSize; ++i)                            //汉宁窗
    {
        window[i] = hann_i(i, fftSize);
    }
    // 申请当前STFT分析框架（nfft样本）的空间。
    //float *last_frame = (float *) malloc(fftSize * sizeof(last_frame[0]));

    size_t fft_work_size;

    // 获取FFT工作区域的大小到fft_work_size
    kiss_fftr_alloc(fftSize, 0, 0, &fft_work_size);

    // 申请FFT工作区域的内存
    void *fft_work = malloc(fft_work_size);
    kiss_fftr_cfg fft_cfg = kiss_fftr_alloc(fftSize, 0, fft_work, &fft_work_size);

    // 最大块数，FT8的周期时长/每个符号时长，FT8:15/0.16 =93
    const int max_blocks = (int) (FT8_SLOT_TIME / FT8_SYMBOL_PERIOD);
    const int num_bins = (int) (12000 * FT8_SYMBOL_PERIOD / 2);
    int fftOffset = 0;
    int offset = 0;
    float maxMag = 0;
    float minMag = 65535.0f;
    float mags[dataSize / 2];
    for (int pos = 0; pos < dataSize / fftSize; pos++) {
        kiss_fft_scalar timedata[fftSize];
        kiss_fft_cpx freqdata[fftSize / 2 + 1];
        for (int i = 0; i < fftSize; i++) {//fftSize=3840
            timedata[i] = window[i] * voiceData[offset];//window[i] *
            offset++;
        }
        kiss_fftr(fft_cfg, timedata, freqdata);

        for (int i = 1; i < fftSize / 2 + 1; i++) {
            float mag2 = sqrtf(freqdata[i].i * freqdata[i].i + freqdata[i].r * freqdata[i].r);
            mags[fftOffset] = mag2;
            if (maxMag < mag2) {
                maxMag = mag2;
            }
            if (minMag > mag2) {
                minMag = mag2;
            }
            fftOffset++;
        }

        float normal = (maxMag - minMag) / 256;
        for (int i = 0; i < dataSize / 2; ++i) {
                fftData[i] = roundf((mags[i] - minMag) / normal);
        }
    }
    free(fft_work);
    free(window);
    //free(last_frame);
    //free(fft_cfg);
}

void do_fftr_raw(float *voiceData, int dataSize, int *fftData) {
    //int block_size = FT8_SYMBOL_PERIOD * 12000; //=1920
    int fftSize = FT8_SYMBOL_PERIOD * 12000; //=1920
    float *window = (float *) malloc(
            fftSize * sizeof(window[0])); // 申请窗空间，大小是fft块大小*sizeof(windows[0])
    for (int i = 0; i < fftSize; ++i)                            //汉宁窗
    {
        window[i] = hann_i(i, fftSize);
    }
    // 申请当前STFT分析框架（nfft样本）的空间。
    //float *last_frame = (float *) malloc(fftSize * sizeof(last_frame[0]));

    size_t fft_work_size;

    // 获取FFT工作区域的大小到fft_work_size
    kiss_fftr_alloc(fftSize, 0, 0, &fft_work_size);

    // 申请FFT工作区域的内存
    void *fft_work = malloc(fft_work_size);
    kiss_fftr_cfg fft_cfg = kiss_fftr_alloc(fftSize, 0, fft_work, &fft_work_size);

    // 最大块数，FT8的周期时长/每个符号时长，FT8:15/0.16 =93
    const int max_blocks = (int) (FT8_SLOT_TIME / FT8_SYMBOL_PERIOD);
    const int num_bins = (int) (12000 * FT8_SYMBOL_PERIOD / 2);
    int fftOffset = 0;
    int offset = 0;

    //float mags[dataSize / 2];
    for (int pos = 0; pos < dataSize / fftSize; pos++) {
        kiss_fft_scalar timedata[fftSize];
        kiss_fft_cpx freqdata[fftSize / 2 + 1];
        for (int i = 0; i < fftSize; i++) {//fftSize=3840
            timedata[i] = window[i] * voiceData[offset];//window[i] *
            offset++;
        }
        kiss_fftr(fft_cfg, timedata, freqdata);

        for (int i = 1; i < fftSize / 2 + 1; i++) {
            float mag2 =(freqdata[i].i * freqdata[i].i + freqdata[i].r * freqdata[i].r);
            mag2= 10.0f * log10f(1E-12f + mag2);
            int scaled = (int) (mag2 +20)*4;

            //0~255之间
            fftData[fftOffset] = (scaled < 0) ? 0 : ((scaled > 255) ? 255 : scaled);

            fftOffset++;
        }

//        float normal = (maxMag - minMag) / 256;
//        for (int i = 0; i < dataSize / 2; ++i) {
//            fftData[i] = roundf((mags[i] - minMag) / normal);
//        }
    }
    free(fft_work);
    free(window);
}