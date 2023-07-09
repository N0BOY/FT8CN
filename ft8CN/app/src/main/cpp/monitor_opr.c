//
// Created by jmsmf on 2022/4/22.
//

#include "monitor_opr.h"
#include "ft8/constants.h"
//#define LOG_LEVEL LOG_DEBUG
#define LOG_LEVEL LOG_FATAL


// FFT转换，采用加窗函数，减少频谱泄露的问题。
// Hanning窗（汉宁窗）适用于95%的情况。
static float hann_i(int i, int N) {
    float x = sinf((float) M_PI * i / N);
    return x * x;
}

static float hamming_i(int i, int N) {
    const float a0 = (float) 25 / 46;
    const float a1 = 1 - a0;

    float x1 = cosf(2 * (float) M_PI * i / N);
    return a0 - a1 * x1;
}

static float blackman_i(int i, int N) {
    const float alpha = 0.16f; // or 2860/18608
    const float a0 = (1 - alpha) / 2;
    const float a1 = 1.0f / 2;
    const float a2 = alpha / 2;

    float x1 = cosf(2 * (float) M_PI * i / N);
    float x2 = 2 * x1 * x1 - 1; // Use double angle formula

    return a0 - a1 * x1 + a2 * x2;
}

static void
waterfall_init(waterfall_t *me, int max_blocks, int num_bins, int time_osr, int freq_osr) {
    //mag_size，信号量数组的大小。最大块数93*时间过采样率2*频率过采样率2*分析块960*sizeOf(U_int8)
    size_t mag_size = max_blocks * time_osr * freq_osr * num_bins * sizeof(me->mag[0]);
    me->max_blocks = max_blocks;
    me->num_blocks = 0;
    me->num_bins = num_bins;//num_bins = 12000 * 0.16 / 2 = 960
    me->time_osr = time_osr;
    me->freq_osr = freq_osr;
    me->block_stride = (time_osr * freq_osr * num_bins);
    me->mag = (uint8_t *) malloc(mag_size);//申请信号量数组，用于计算得分，357120个
    me->mag2 = (float *) malloc(mag_size * sizeof(float));//申请信号量数组，用于计算信噪比
    LOG(LOG_DEBUG, "Waterfall size = %zu\n", mag_size);
}

static void waterfall_free(waterfall_t *me) {
    free(me->mag);
    free(me->mag2);
}


void monitor_init(monitor_t *me, const monitor_config_t *cfg) {
    LOG(LOG_DEBUG, "Monitor is initializing...");
    //协议的时长，FT8_SLOT_TIME=15.0f，FT4_SLOT_TIME=7.5f
    float slot_time = (cfg->protocol == PROTO_FT4) ? FT4_SLOT_TIME : FT8_SLOT_TIME;
    //协议每个符号的时长，FT8_SYMBOL_PERIOD=0.160f，FT4_SYMBOL_PERIOD=0.048f
    float symbol_period = (cfg->protocol == PROTO_FT4) ? FT4_SYMBOL_PERIOD : FT8_SYMBOL_PERIOD;

    //**************************************************
    // Compute DSP parameters that depend on the sample rate
    // 根据采样率计算DSP参数
    // block_size：每一个FSK符号占用的样本数，FT8:12000*0.16=1920个
    // subblock_size：分析移动的大小（样本数）,每符号样本数/时间过采样率。FT8:1920/2=960
    // nfft：fft size。fft大小=每个FSK符号占用的样本数*频率过采样率=1920*2
    // fft_norm：FFT归一化因子。2/fft size。
    me->block_size = (int) (cfg->sample_rate *
                            symbol_period); //12000*0.16 对应于一个FSK符号的样本=1920
    me->subblock_size = me->block_size / cfg->time_osr;//移动的样本数。一个FSK符号的样本数/过采样率
    //nfft是傅里叶变换前，时域的实数序列的数量。目前是一个FSK符号的样本数*频率过采样率
    me->nfft = me->block_size * cfg->freq_osr;//nfft=一个FSK符号的样本数*频率过采样率
    me->fft_norm = 2.0f / me->nfft;//< FFT归一化因子。FFT normalization factor
    // const int len_window = 1.8f * me->block_size; // hand-picked and optimized
    //**************************************************

    // 申请窗空间，大小是fft块大小*me->windows[0]的大小
    // 采集周期如果是实际信号的非整数时，端点是不连续的。
    // 这些不连续片段在FFT中显示为高频成分。这些高频成分不存在于原信号中。
    // 这些频率可能远高于奈奎斯特频率，在0～ 采样率的一半的频率区间内产生混叠。
    // 使用FFT获得的频率，不是原信号的实际频率，而是一个改变过的频率。
    // 类似于某个频率的能量泄漏至其他频率。 这种现象叫做频谱泄漏。
    // 频率泄漏使好的频谱线扩散到更宽的信号范围中。这些不连续片段在FFT中显示为高频成分。
    // 通过加窗来尽可能减少在非整数个周期上进行FFT产生的误差。
    // 加窗可减少这些不连续部分的幅值。
    // 加窗包括将时间记录乘以有限长度的窗，窗的幅值逐渐变小，在边沿处为0。
    // 加窗的结果是尽可能呈现出一个连续的波形，减少剧烈的变化。 这种方法也叫应用一个加窗。
    // 窗函数分很多种，常见的的如：Hamming窗、Hanning窗、Blackman-Harris窗、Kaiser-Bessel窗、Flat top窗。
    // Hanning窗适用于95%的情况。
    me->window = (float *) malloc(
            me->nfft * sizeof(me->window[0]));// 申请窗空间，大小是fft块大小*sizeof(me->windows[0])
    //此处是窗函数的设置，使用常用的hanning窗
    for (int i = 0; i < me->nfft; ++i) {
        // window[i] = 1;
        me->window[i] = hann_i(i, me->nfft);// 使用Hanning窗
        // me->window[i] = blackman_i(i, me->nfft);// Blackman-Harris窗
        // me->window[i] = hamming_i(i, me->nfft);// Hamming窗
        // me->window[i] = (i < len_window) ? hann_i(i, len_window) : 0;
    }


    // 申请当前STFT(短时傅氏变换)分析框架（nfft样本）的空间。
    //last_frame:申请傅里叶变换分析框架用的（nfft样本）。
    // 空间大小=时域数据量*数据类型占用的空间=一个FSK符号占用的采样数据量*频率过采样率=12000*0.16*2*SizeOf(float)
    me->last_frame = (float *) malloc(me->nfft * sizeof(me->last_frame[0]));

    // size_t 类型定义在cstddef头文件中，该文件是C标准库的头文件stddef.h的C++版。
    // 它是一个与机器相关的unsigned类型，其大小足以保证存储内存中对象的大小。
    size_t fft_work_size;
    // 第一步，获取可以用的FFT工作区域的大小到fft_work_size
    //nfft=一个FSK符号的样本数*频率过采样率=0.16*12000*2=3840
    kiss_fftr_alloc(me->nfft, 0, 0, &fft_work_size);
    //fft_work_size


    // 申请FFT工作区域的内存，38676个
    me->fft_work = malloc(fft_work_size);
    //第二步，返回fft的设置信息
    me->fft_cfg = kiss_fftr_alloc(me->nfft, 0, me->fft_work, &fft_work_size);

    // 最大块数，FT8的周期时长/每个符号时长，FT8:15/0.16 =93
    const int max_blocks = (int) (slot_time / symbol_period);

    // num_bins：如果以6.25 Hz为单位的FFT箱数量。6.25：每秒6.25个FSK符号，0.16*6.25=1。
    // num_bins = 12000 * 0.16 / 2 = 960。2是啥？是生成的频域数据量是采集数据量的一半
    const int num_bins = (int) (cfg->sample_rate * symbol_period / 2);

    //初始化瀑布图。
    waterfall_init(&me->wf, max_blocks, num_bins, cfg->time_osr, cfg->freq_osr);
    me->wf.protocol = cfg->protocol;
    me->symbol_period = symbol_period;

    me->max_mag = -120.0f;


    LOG(LOG_INFO, "Block size = %d\n", me->block_size);
    LOG(LOG_INFO, "Subblock size = %d\n", me->subblock_size);
    LOG(LOG_INFO, "N_FFT = %d\n", me->nfft);
    LOG(LOG_DEBUG, "FFT work area = %zu\n", fft_work_size);
    // 瀑布图中最多能申请max_blocks个块数
    LOG(LOG_DEBUG, "Waterfall allocated %d symbols\n", me->wf.max_blocks);
}

void monitor_free(monitor_t *me) {

    waterfall_free(&me->wf);
    free(me->fft_work);
    free(me->last_frame);
    free(me->window);
    LOG(LOG_DEBUG, "Monitor is free .");
}


// Compute FFT magnitudes (log wf) for a frame in the signal and update waterfall data
// 计算信号中一帧的FFT幅度（log wf），并更新瀑布数据
void monitor_process(monitor_t *me, const float *frame) {
    // Check if we can still store more waterfall data
    //防止溢出
    //mag阵列中存储的块（符号）编号
    if (me->wf.num_blocks >= me->wf.max_blocks)
        return;

    //num_bins 的值是 12000 * 0.16 / 2 = 960
    //wf.block_strid= (time_osr * freq_osr * num_bins)=2*2*960
    //offset是在mag数组中的偏移量。wf.num_blocks是当前符号的块编号，以num_bins(数据片段)*时间过采样*频率过采样为单位。
    //mag的数组大小实际上是时间过采样*频率过采样*符号的最大量（93）*每符号真实采样数据（过采样的数据，960）.
    int offset = me->wf.num_blocks * me->wf.block_stride;
    int frame_pos = 0;

    // Loop over block subdivisions
    //循环块细分，wf.time_osr=2 时间过采样率
    for (int time_sub = 0; time_sub < me->wf.time_osr; ++time_sub) {
        kiss_fft_scalar timedata[me->nfft];
        kiss_fft_cpx freqdata[me->nfft / 2 + 1];

        // Shift the new data into analysis frame
        //将新数据转移到分析框架中。
        // subblock_size：分析移动的大小（样本数）blocksize/2 每秒块数/时间过采样率=FT8:12000*0.16/2=1920/2=960个
        //last_frame的空间已经申请好了。空间大小=时域数据量*数据类型占用的空间=一个FSK符号占用的采样数据量*频率过采样率=12000*0.16*2*SizeOf(float)=3840
        //nfft=一个FSK符号的样本数*频率过采样率=1920*2=3840
        //subblock_size。移动的样本数。一个FSK符号的样本数/过采样率,1920/2=960
        //第一个循环，把过采样的后半段数据向前移960个数据，
        //第二个循环，把新的声音数据导入到last_frame的后半部分，新的声音数据960个。外面的时间过采样率循环2遍，正好960*2=1920,一个符号
        //这样就可以对一个符号周期的时域数据做傅里叶变换了
        for (int pos = 0; pos < me->nfft - me->subblock_size; ++pos) {
            me->last_frame[pos] = me->last_frame[pos + me->subblock_size];
        }
        for (int pos = me->nfft - me->subblock_size; pos < me->nfft; ++pos) {
            me->last_frame[pos] = frame[frame_pos];
            ++frame_pos;
        }


        // Compute windowed analysis frame
        //用窗函数做一次转换，汉宁窗。
        for (int pos = 0; pos < me->nfft; ++pos) {
            //把last_frame中的数据赋值到timedata中来，timedata是时域数据，要做一次归一化、窗函数处理
            timedata[pos] = me->fft_norm * me->window[pos] * me->last_frame[pos];
            //timedata[pos] =me->window[pos] *  me->last_frame[pos];
        }

        //傅里叶变换把timedata的时域数据（长度是nfft）转换到频域数据上来。频域数据是复数数组，数组长度是nfft/2+1
        //nfft=一个FSK符号的样本数*频率过采样率=12000*0.16*2=3840
        kiss_fftr(me->fft_cfg, timedata, freqdata);

        // Loop over two possible frequency bin offsets (for averaging)
        //在两个可能的频率单元偏移上循环（用于平均）
        //两个循环的意义是：在一个符号采样数据的范围内（12000*0.16）对freqdata的能量做计算
        for (int freq_sub = 0; freq_sub < me->wf.freq_osr; ++freq_sub) {
            for (int bin = 0; bin < me->wf.num_bins; ++bin) { //num_bins 的值是 12000 * 0.16 / 2 = 960

                //循环次数=2*960=1920
                //信号量位置src_bin,
                int src_bin = (bin * me->wf.freq_osr) + freq_sub;

                ////此位置可能是计算信噪比的位置
                //求各频率点上的信号强度是傅里叶之后的平方？少了开方，mag2应当是信号量的平方
                float mag2 = (freqdata[src_bin].i * freqdata[src_bin].i) +
                             (freqdata[src_bin].r * freqdata[src_bin].r);
                float db = 10.0f * log10f(1E-12f + mag2);

                //把信号量保存下来
                //offset=me->wf.num_blocks * me->wf.block_stride;
                //wf.block_strid= (time_osr * freq_osr * num_bins)=2*2*960
                //偏移量就是当前块编号*每个符号ftt数据量

                me->wf.mag2[offset] = mag2;

                //每循环一次，偏移量移一位。共移动time_osr*freq_osr*num_bins=2*2*960=3840

                // Scale decibels to unsigned 8-bit range and clamp the value
                //将分贝缩放到无符号8位范围，并钳制该值
                // Range 0-240 covers -120..0 dB in 0.5 dB steps
                int scaled = (int) (2 * db + 240);

                //0~255之间
                me->wf.mag[offset] = (scaled < 0) ? 0 : ((scaled > 255) ? 255 : scaled);
                ++offset;

                if (db > me->max_mag)
                    me->max_mag = db;
            }
        }
    }
    ++me->wf.num_blocks;//mag阵列中存储的块（符号）编号，块大小：2*2*960
}

void monitor_reset(monitor_t *me) {
    LOG(LOG_DEBUG, "Monitor is resetting...");
    me->wf.num_blocks = 0;
    me->max_mag = -120.0f;
}
