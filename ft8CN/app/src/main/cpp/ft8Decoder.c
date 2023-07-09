//
// Created by jmsmf on 2022/4/24.
//

#include "ft8Decoder.h"

#define LOG_LEVEL LOG_INFO
//decoder_t decoder;

// Hanning窗（汉宁窗）适用于95%的情况。
static float hann_i(int i, int N) {
    float x = sinf((float) M_PI * i / N);
    return x * x;
}

//把信号FFT,在解码decoder中减去信号
void signalToFFT(decoder_t *decoder, float signal[], int sample_rate) {
    int nfft = kFreq_osr * (int) (sample_rate * FT8_SYMBOL_PERIOD);//nfft=一个FSK符号的样本数*频率过采样率
    float fft_norm = 2.0f / nfft;//< FFT归一化因子。FFT normalization factor
    float *window = (float *) malloc(
            nfft * sizeof(window[0]));// 申请窗空间，大小是fft块大小*sizeof(me->windows[0])
    for (int i = 0; i < nfft; ++i) {
        window[i] = hann_i(i, nfft);// 使用Hanning窗
        // window[i] = blackman_i(i, me->nfft);// Blackman-Harris窗
        // window[i] = hamming_i(i, me->nfft);// Hamming窗
        // window[i] = (i < len_window) ? hann_i(i, len_window) : 0;
    }

    // 申请当前STFT(短时傅氏变换)分析框架（nfft样本）的空间。
    //last_frame:申请傅里叶变换分析框架用的（nfft样本）。
    // 空间大小=时域数据量*数据类型占用的空间=一个FSK符号占用的采样数据量*频率过采样率=12000*0.16*2*SizeOf(float)
    float *last_frame = (float *) malloc(nfft * sizeof(last_frame[0]));

    size_t fft_work_size;
    // 第一步，获取可以用的FFT工作区域的大小到fft_work_size
    //nfft=一个FSK符号的样本数*频率过采样率=0.16*12000*2=3840
    kiss_fftr_alloc(nfft, 0, 0, &fft_work_size);

    // 申请FFT工作区域的内存，38676个
    void *fft_work = malloc(fft_work_size);
    //第二步，返回fft的设置信息
    kiss_fftr_cfg fft_cfg = kiss_fftr_alloc(nfft, 0, fft_work, &fft_work_size);


    free(fft_work);
    free(window);
    free(last_frame);

}

void *init_decoder(int64_t utcTime, int sample_rate, int num_samples, bool is_ft8) {

    //此处，改为一个变量，不是以指针，申请新内存的方式处理了。
    //其实这种方式要注意一个问题，在一个周期之内，必须解码完毕，否则新的解码又要开始了

    //此处不用申请内存的方式解决
//    decoder.utcTime = utcTime;
//    decoder.num_samples = num_samples;
//    decoder.mon_cfg = (monitor_config_t) {
//            .f_min = 100,//分析的最低频率边界
//            .f_max = 3000,//分析的最大频率边界
//            .sample_rate = sample_rate,//采样率12000Hz
//            .time_osr = kTime_osr,//时间过采样率=2
//            .freq_osr = kFreq_osr,//频率过采样率=2
//            .protocol = is_ft8 ? PROTO_FT8 : PROTO_FT4
//    };
//    //LOGD("Init decoder . address : %lld", decoder);
//    monitor_init(&decoder.mon, &decoder.mon_cfg);
//
//
//    return &decoder;

    //此部分，是老的解决方式，是动态申请内存。
    decoder_t *decoder;
    decoder = malloc(sizeof(decoder_t));
    decoder->utcTime = utcTime;
    decoder->num_samples = num_samples;
    decoder->mon_cfg = (monitor_config_t) {
            .f_min = 100,//分析的最低频率边界
            .f_max = 3000,//分析的最大频率边界
            .sample_rate = sample_rate,//采样率12000Hz
            .time_osr = kTime_osr,//时间过采样率=2
            .freq_osr = kFreq_osr,//频率过采样率=2
            .protocol = is_ft8 ? PROTO_FT8 : PROTO_FT4
    };

    decoder->kLDPC_iterations = fast_kLDPC_iterations;
    //LOGD("Init decoder . address : %lld", decoder);
    monitor_init(&decoder->mon, &decoder->mon_cfg);

    return decoder;
}

void delete_decoder(decoder_t *decoder) {
    //LOGD("Free decoder , address:%lld", decoder);
    monitor_free(&decoder->mon);
    free(decoder);
}

void decoder_monitor_press(float signal[], decoder_t *decoder) {

    // 以每一个FSK符号占用的数据量为单位循环。
    //block_size每个符号的样本数12000*0.16=1920

    for (int frame_pos = 0;
         frame_pos + decoder->mon.block_size <=
         decoder->num_samples; frame_pos += decoder->mon.block_size) {
        // Process the waveform data frame by frame - you could have a live loop here with data from an audio device
        // 逐帧处理波形数据，这个位置可以使用音频设备的数据环。
        //以每一个符号时间长度（0.16）内的数据做瀑布数据，最后会形成一个信号量mag数组。
        // mag数组的总长度是：最大符号块数93*时间过采样率2*频率过采样率2*分析块960，也就是Waterfall size = 357120
        //一次调用monitor_process，处理的是一个符号长度的信号量数据，生成2*2*960=3840个mag数据。
        //一共有93个符号的循环，mag数组的大小：2*2*960*93=357120
        //mag数据保存在monitor.wf.mag中。
        monitor_process(&decoder->mon, signal + frame_pos);
    }

    //  /data/user/0/com.bg7yoz.ft8cn/cache/

    //把fft数据保存下来
    //FILE * fp2 = fopen("/data/user/0/com.bg7yoz.ft8cn/cache/fft001.txt", "w");//打开输出文件
    //for (int i = 0; i < 3840; ++i) {
    //    for (int j = 0; j < 93; ++j) {
    //        fprintf (fp2,"%d\n", decoder->mon.wf.mag[i*j]);//把数组a逆序写入到输出文件当中
    //    }
    //}
    //fclose(fp2);//关闭输出文件，相当于保存



    LOG(LOG_DEBUG, "Waterfall accumulated %d symbols\n", decoder->mon.wf.num_blocks);//积累的信号
    LOG(LOG_INFO, "Max magnitude: %.1f dB\n", decoder->mon.max_mag);//最大信号值dB

}

int decoder_ft8_find_sync(decoder_t *decoder) {
    //检测ft8信号，kMax_candidates最大候选人数量=120，candidate_list候选人列表(size=120)，kMin_score候选人的最低同步分数阈值=10
    decoder->num_candidates = ft8_find_sync(&decoder->mon.wf, kMax_candidates,
                                            decoder->candidate_list, kMin_score);
    LOG(LOG_DEBUG, "ft8_find_sync finished. %d candidates\n", decoder->num_candidates);


    // Hash table for decoded messages (to check for duplicates)
    // 解码消息的哈希表（用于检查重复项）
    //int num_decoded = 0;
    //message_t decoded[kMax_decoded_messages];//kMax_decoded_messages=50
    // 哈希表指针列表（指针数组）
    //message_t *decoded_hashtable[kMax_decoded_messages];

    // Initialize hash table pointers
    // 初始化哈希表指针列表
    for (int i = 0; i < kMax_decoded_messages; ++i) {
        decoder->decoded_hashtable[i] = NULL;
    }
    return decoder->num_candidates;
}


ft8_message decoder_ft8_analysis(int idx, decoder_t *decoder) {

    ft8_message ft8Message;
    ft8Message.isValid = false;
    ft8Message.utcTime = decoder->utcTime;
    // 候选列表candidate_list，已经从ft8_fing_sync获得。
    ft8Message.candidate = decoder->candidate_list[idx];


    if (ft8Message.candidate.score < kMin_score) {
        //ft8Message.isValid = false;
        return ft8Message;
    }

    ft8Message.freq_hz =
            (ft8Message.candidate.freq_offset +
             (float) ft8Message.candidate.freq_sub / decoder->mon.wf.freq_osr) /
            decoder->mon.symbol_period;
    ft8Message.time_sec =
            ((ft8Message.candidate.time_offset + (float) ft8Message.candidate.time_sub)
             * decoder->mon.symbol_period) / decoder->mon.wf.time_osr;

    //ft8Message.snr=ft8Message.candidate.snr;
    //这是原来代码的时间偏移，同样的数据与JTDX的时间差异很大，改用上面的代码，稍微接近一些
    //(ft8Message.candidate.time_offset +
    //(float) ft8Message.candidate.time_sub / decoder->mon.wf.time_osr) *
    //decoder->mon.symbol_period;


    // 如果解码失败，跳到下一次循环 kLDPC_iterations=20 LDPC（低密度奇偶校验）的迭代次数。
    if (!ft8_decode(&decoder->mon.wf, &ft8Message.candidate
            //, &ft8Message.message, kLDPC_iterations,
            , &ft8Message.message, decoder->kLDPC_iterations,
                    &ft8Message.status)) {
        // printf("000000 %3d %+4.2f %4.0f ~  ---\n", cand->score, time_sec, freq_hz);
        if (ft8Message.status.ldpc_errors > 0) {
            // LDPC:低密度奇偶校验
            LOG(LOG_DEBUG, "LDPC decode: %d errors\n", ft8Message.status.ldpc_errors);
        } else if (ft8Message.status.crc_calculated != ft8Message.status.crc_extracted) {
            LOG(LOG_DEBUG, "CRC mismatch!\n");
        } else if (ft8Message.status.unpack_status != 0) {
            LOG(LOG_DEBUG, "Error while unpacking!\n");
        }
        //ft8Message.isValid = false;
        return ft8Message;
    }

    ft8Message.snr = ft8Message.candidate.snr;

    LOG(LOG_DEBUG, "Checking hash table for %4.1fs / %4.1fHz [%d]...\n", ft8Message.time_sec,
        ft8Message.freq_hz,
        ft8Message.candidate.score);

    int idx_hash =
            ft8Message.message.hash % kMax_decoded_messages;//为啥是取模？稍后研究kMax_decoded_messages=50

    bool found_empty_slot = false;
    bool found_duplicate = false;
    //检查哈希表，只有空插槽，或重复插槽（哈希值相同，并且消息相同）
    do {
        if (decoder->decoded_hashtable[idx_hash] == NULL) {
            LOG(LOG_DEBUG, "Found an empty slot\n");
            found_empty_slot = true;
        } else if ((decoder->decoded_hashtable[idx_hash]->hash == ft8Message.message.hash) &&
                   (0 ==
                    strcmp(decoder->decoded_hashtable[idx_hash]->text, ft8Message.message.text))) {
            LOG(LOG_DEBUG, "Found a duplicate [%s]\n", ft8Message.message.text);
            found_duplicate = true;
        } else {
            LOG(LOG_DEBUG, "Hash table clash!\n");
            // Move on to check the next entry in hash table
            idx_hash = (idx_hash + 1) % kMax_decoded_messages;
        }
    } while (!found_empty_slot && !found_duplicate);


    if (found_empty_slot) {
        // Fill the empty hashtable slot
        memcpy(&decoder->decoded[idx_hash], &ft8Message.message, sizeof(ft8Message.message));
        decoder->decoded_hashtable[idx_hash] = &decoder->decoded[idx_hash];
        ++decoder->num_decoded;


        ft8Message.isValid = true;

        LOG_PRINTF("%3d %+4.2f %4.0f ~  %s report:%d grid:%s,toHash:%x,fromHash:%x",
                   ft8Message.snr,
                   ft8Message.time_sec, ft8Message.freq_hz, ft8Message.message.text,
                   ft8Message.message.report, ft8Message.message.maidenGrid,
                   ft8Message.message.call_to_hash.hash12, ft8Message.message.call_de_hash.hash12);
    }
    memcpy(decoder->a91, ft8Message.message.a91, FTX_LDPC_K_BYTES);
    return ft8Message;
}

void decoder_ft8_reset(decoder_t *decoder, long utcTime, int num_samples) {
    LOG(LOG_DEBUG, "Monitor is resetting...");
    decoder->mon.wf.num_blocks = 0;
    //decoder->mon.max_mag = 0;
    decoder->mon.max_mag = -120.0f;
    decoder->utcTime = utcTime;
    decoder->num_samples = num_samples;
}

/**
 * 对174码，重新编生成79码
 * @param a174 174个int
 * @param a79 79个int
 */
void recode(int a174[], int a79[]) {
    int i174 = 0;
    //int costas[] = { 3, 1, 4, 0, 6, 5, 2 };
    //std::vector<int> out79;
    for (int i79 = 0; i79 < 79; i79++) {
        if (i79 < 7) {
            //out79.push_back(costas[i79]);
            a79[i79] = kFT8CostasPattern[i79];
        } else if (i79 >= 36 && i79 < 36 + 7) {
            //out79.push_back(costas[i79-36]);
            a79[i79] = kFT8CostasPattern[i79 - 36];
        } else if (i79 >= 72) {
            //out79.push_back(costas[i79-72]);
            a79[i79] = kFT8CostasPattern[i79 - 72];
        } else {
            int sym = (a174[i174 + 0] << 2) | (a174[i174 + 1] << 1) | (a174[i174 + 2] << 0);
            i174 += 3;
            // gray code
            int map[] = {0, 1, 3, 2, 5, 6, 4, 7};
            sym = map[sym];
            //out79.push_back(sym);
            a79[i79] = sym;
        }
    }
};
