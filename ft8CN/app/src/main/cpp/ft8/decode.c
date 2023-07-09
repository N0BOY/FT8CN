#include "decode.h"
#include "constants.h"
#include "crc.h"
#include "ldpc.h"
#include "unpack.h"

#include <stdbool.h>
#include <math.h>
#include "../common/debug.h"
#include "hash22.h"

/// Compute log likelihood log(p(1) / p(0)) of 174 message bits for later use in soft-decision LDPC decoding
/// @param[in] wf Waterfall data collected during message slot
/// @param[in] cand Candidate to extract the message from
/// @param[in] code_map Symbol encoding map
/// @param[out] log174 Output of decoded log likelihoods for each of the 174 message bits
static void ft4_extract_likelihood(const waterfall_t *wf, const candidate_t *cand, float *log174);

static void ft8_extract_likelihood(const waterfall_t *wf, candidate_t *cand, float *log174);

/// Packs a string of bits each represented as a zero/non-zero byte in bit_array[],
/// as a string of packed bits starting from the MSB of the first byte of packed[]
/// @param[in] plain Array of bits (0 and nonzero values) with num_bits entires
/// @param[in] num_bits Number of bits (entries) passed in bit_array
/// @param[out] packed Byte-packed bits representing the data in bit_array
static void pack_bits(const uint8_t bit_array[], int num_bits, uint8_t packed[]);

static float max2(float a, float b);

static float max4(float a, float b, float c, float d);

static void heapify_down(candidate_t heap[], int heap_size);

static void heapify_up(candidate_t heap[], int heap_size);

static void ftx_normalize_logl(float *log174);

static void ft4_extract_symbol(const uint8_t *wf, float *logl);

static void ft8_extract_symbol(const uint8_t *wf, float *logl);

static void
ft8_decode_multi_symbols(const uint8_t *wf, int num_bins, int n_syms, int bit_idx, float *log174);

static int get_index(const waterfall_t *wf, const candidate_t *candidate) {
    int offset = candidate->time_offset;//time_offset:-12 ~ 23,(costas阵列7个符号+29个数据符号=36)
    offset = (offset * wf->time_osr) + candidate->time_sub;//time_sub:0~1
    offset = (offset * wf->freq_osr) + candidate->freq_sub;//freq_sub:0~1
    offset = (offset * wf->num_bins) +
             candidate->freq_offset;//num_bins:960，freq_offset:0~ 960(-1) -7
    return offset;
}


static int ft8_sync_score(const waterfall_t *wf, candidate_t *candidate) {
    /*
     * ft8本应有58个符号，但在开始（0-7）、中间（36-43）、结尾（72-79）加了科斯塔阵列，所以共有79个符号，
     *此函数在4层循环中执行。时间采样率2*频率采样率2*时间偏移（-12~24=36）*频率偏移（num_bins:960-7）
     */
    int score = 0;
    int num_average = 0;

    float signal = 0;
    float noise = 0;

    // Get the pointer to symbol 0 of the candidate
    //获取指向候选符号0的指针，在mag数组中取candidate对应的mag数据。
    const uint8_t *mag_cand = wf->mag + get_index(wf, candidate);
    //用于信号量和噪音的计算，暂时注释掉
    // const float *mag_signal = wf->mag2 + get_index(wf, candidate);


    // Compute average score over sync symbols (m+k = 0-7, 36-43, 72-79)
    //计算同步符号的平均分数（m+k=0-7、36-43、72-79）
    //m=0~2(3组),k=0~6(7个符号)
    for (int m = 0; m < FT8_NUM_SYNC; ++m) {
        for (int k = 0; k < FT8_LENGTH_SYNC; ++k) {
            //FT8_SYNC_OFFSET=36，block=0..6，36~43，72~79，这是costas阵列在符号序列中的索引
            int block = (FT8_SYNC_OFFSET * m) + k;   // 相对于消息relative to the message
            int block_abs =
                    //time_offset=-12.。23（36个）
                    candidate->time_offset + block; // 相对于捕获的信号relative to the captured signal
            // Check for time boundaries
            //检查时间界限
            if (block_abs < 0)
                continue;
            if (block_abs >= wf->num_blocks)
                break;

            // Get the pointer to symbol 'block' of the candidate
            //获取指向候选人符号“block”的指针
            const uint8_t *p8 = mag_cand + (block * wf->block_stride);

            // Weighted difference between the expected and all other symbols
            //预期符号和所有其他符号之间的加权差
            // Does not work as well as the alternative score below
            //效果不如下面的备选分数
            // score += 8 * p8[kFT8CostasPattern[k]] -
            //          p8[0] - p8[1] - p8[2] - p8[3] -
            //          p8[4] - p8[5] - p8[6] - p8[7];
            // ++num_average;

            // Check only the neighbors of the expected symbol frequency- and time-wise
            //仅检查预期符号频率和时间的相邻项，k=0..6
            int sm = kFT8CostasPattern[k]; //预期数据的索引 Index of the expected bin


            //此处计算信号量和噪音，可能不正确，暂时注释掉
            // const float *p8Signal = mag_signal + (block * wf->block_stride);



            //通过sm判断相邻频率的信号量是否小于本位置的信号量，小于就加分
            if (sm > 0) {
                // look at one frequency bin lower
                //信号量的差值。
                score += p8[sm] - p8[sm - 1];
                ++num_average;
            }
            if (sm < 7) {
                // look at one frequency bin higher
                score += p8[sm] - p8[sm + 1];
                ++num_average;
            }
            //判断前后符号时间频率信号量是否小于本位置的信号量，小于就加分
            if ((k > 0) && (block_abs > 0)) {
                // look one symbol back in time
                score += p8[sm] - p8[sm - wf->block_stride];

                ++num_average;
            }
            if (((k + 1) < FT8_LENGTH_SYNC) && ((block_abs + 1) < wf->num_blocks)) {
                // look one symbol forward in time
                score += p8[sm] - p8[sm + wf->block_stride];
                ++num_average;
            }
        }
    }

    if (num_average > 0) {
        score /= num_average;
    }

    return score;
}

static int ft4_sync_score(const waterfall_t *wf, const candidate_t *candidate) {
    int score = 0;
    int num_average = 0;

    // Get the pointer to symbol 0 of the candidate
    const uint8_t *mag_cand = wf->mag + get_index(wf, candidate);

    // Compute average score over sync symbols (block = 1-4, 34-37, 67-70, 100-103)
    for (int m = 0; m < FT4_NUM_SYNC; ++m) {
        for (int k = 0; k < FT4_LENGTH_SYNC; ++k) {
            int block = 1 + (FT4_SYNC_OFFSET * m) + k;
            int block_abs = candidate->time_offset + block;
            // Check for time boundaries
            if (block_abs < 0)
                continue;
            if (block_abs >= wf->num_blocks)
                break;

            // Get the pointer to symbol 'block' of the candidate
            const uint8_t *p4 = mag_cand + (block * wf->block_stride);

            int sm = kFT4CostasPattern[m][k]; // Index of the expected bin

            // score += (4 * p4[sm]) - p4[0] - p4[1] - p4[2] - p4[3];
            // num_average += 4;

            // Check only the neighbors of the expected symbol frequency- and time-wise
            if (sm > 0) {
                // look at one frequency bin lower
                score += p4[sm] - p4[sm - 1];
                ++num_average;
            }
            if (sm < 3) {
                // look at one frequency bin higher
                score += p4[sm] - p4[sm + 1];
                ++num_average;
            }
            if ((k > 0) && (block_abs > 0)) {
                // look one symbol back in time
                score += p4[sm] - p4[sm - wf->block_stride];
                ++num_average;
            }
            if (((k + 1) < FT4_LENGTH_SYNC) && ((block_abs + 1) < wf->num_blocks)) {
                // look one symbol forward in time
                score += p4[sm] - p4[sm + wf->block_stride];
                ++num_average;
            }
        }
    }

    if (num_average > 0)
        score /= num_average;

    return score;
}

//检测ft8信号，num_candidates最大候选人数量=120，heap[]候选人列表（size=120），kMin_score候选人的最低同步分数阈值=10
int ft8_find_sync(const waterfall_t *wf, int num_candidates, candidate_t heap[], int min_score) {
    int heap_size = 0;
    candidate_t candidate;//候选人
    // Here we allow time offsets that exceed signal boundaries, as long as we still have all data bits.
    // I.e. we can afford to skip the first 7 or the last 7 Costas symbols, as long as we track how many
    // sync symbols we included in the score, so the score is averaged.
    //在这里，我们允许超过信号边界的时间偏移，只要我们仍然拥有所有数据位。
    //也就是说，我们可以跳过前7个或最后7个Costas符号，只要我们跟踪有多少个
    //我们在分数中包含了同步符号，所以分数是平均值。
    //循环：时间过采样*频率过采样*前36个符号（7同步+29信息）*fft频率偏移=2*2*36*960=3840*36=138240
    for (candidate.time_sub = 0; candidate.time_sub < wf->time_osr; ++candidate.time_sub) {
        for (candidate.freq_sub = 0; candidate.freq_sub < wf->freq_osr; ++candidate.freq_sub) {
            for (candidate.time_offset = -12; candidate.time_offset < 24; ++candidate.time_offset) {
                for (candidate.freq_offset = 0;
                     (candidate.freq_offset + 7) < wf->num_bins; ++candidate.freq_offset) {
                    if (wf->protocol == PROTO_FT4) {
                        candidate.score = ft4_sync_score(wf, &candidate);
                    } else {
                        candidate.score = ft8_sync_score(wf, &candidate);
                    }

                    if (candidate.score < min_score)
                        continue;

                    // If the heap is full AND the current candidate is better than
                    // the worst in the heap, we remove the worst and make space
                    //如果堆已满且当前候选堆优于在堆中最坏的，我们移除最坏的，并创造空间
                    if (heap_size == num_candidates && candidate.score > heap[0].score) {
                        heap[0] = heap[heap_size - 1];
                        --heap_size;
                        //降序？
                        heapify_down(heap, heap_size);
                    }

                    // If there's free space in the heap, we add the current candidate
                    //如果堆中有可用空间，我们将添加当前候选堆
                    if (heap_size < num_candidates) {
                        heap[heap_size] = candidate;
                        ++heap_size;
                        //升序？
                        heapify_up(heap, heap_size);
                    }
                }
            }
        }
    }



    // Sort the candidates by sync strength - here we benefit from the heap structure
    int len_unsorted = heap_size;
    while (len_unsorted > 1) {
        candidate_t tmp = heap[len_unsorted - 1];
        heap[len_unsorted - 1] = heap[0];
        heap[0] = tmp;
        len_unsorted--;
        heapify_down(heap, len_unsorted);
    }

    return heap_size;
}

static void ft4_extract_likelihood(const waterfall_t *wf, const candidate_t *cand, float *log174) {
    const uint8_t *mag_cand = wf->mag + get_index(wf, cand);

    // Go over FSK tones and skip Costas sync symbols
    for (int k = 0; k < FT4_ND; ++k) {
        // Skip either 5, 9 or 13 sync symbols
        // TODO: replace magic numbers with constants
        int sym_idx = k + ((k < 29) ? 5 : ((k < 58) ? 9 : 13));
        int bit_idx = 2 * k;

        // Check for time boundaries
        int block = cand->time_offset + sym_idx;
        if ((block < 0) || (block >= wf->num_blocks)) {
            log174[bit_idx + 0] = 0;
            log174[bit_idx + 1] = 0;
        } else {
            // Pointer to 4 bins of the current symbol
            const uint8_t *ps = mag_cand + (sym_idx * wf->block_stride);

            ft4_extract_symbol(ps, log174 + bit_idx);
        }
    }
}

//解开可能的FT8信号
static void ft8_extract_likelihood(const waterfall_t *wf, candidate_t *cand, float *log174) {
    const uint8_t *mag_cand = wf->mag + get_index(wf, cand);

    ////FT8总消息的为174位，符号是174/3=58个，加上同步costas阵列的7*3=21个符号，共计58+21=79个符号。
    //log174数组的大小是174
    // Go over FSK tones and skip Costas sync symbols
    //浏览FSK音调并跳过Costas同步符号，所以log174
    //FT8_ND=58，k=0..57
    for (int k = 0; k < FT8_ND; ++k) {
        // Skip either 7 or 14 sync symbols
        // TODO: replace magic numbers with constants
        //sym_idx=7..35,43..71
        int sym_idx = k + ((k < 29) ? 7 : 14);
        //bit_idx符号位的索引
        int bit_idx = 3 * k;

        // Check for time boundaries
        //检测时间边界
        int block = cand->time_offset + sym_idx;
        if ((block < 0) || (block >= wf->num_blocks)) {
            log174[bit_idx + 0] = 0;
            log174[bit_idx + 1] = 0;
            log174[bit_idx + 2] = 0;
        } else {
            // Pointer to 8 bins of the current symbol
            //指向当前符号信号量的8个箱子的指针
            //block_stride=960*2*2=3840
            const uint8_t *ps = mag_cand + (sym_idx * wf->block_stride);

            //每个符号，bit_idx是符号的3倍
            ft8_extract_symbol(ps, log174 + bit_idx);
        }
    }
}

static void ftx_normalize_logl(float *log174) {
    // Compute the variance of log174
    //计算log174的方差
    float sum = 0;
    float sum2 = 0;
    //FTX_LDPC_N=174
    for (int i = 0; i < FTX_LDPC_N; ++i) {
        sum += log174[i];//取和
        sum2 += log174[i] * log174[i];//取平方和
    }
    float inv_n = 1.0f / FTX_LDPC_N;
    //variance方差
    float variance = (sum2 - (sum * sum * inv_n)) * inv_n;

    // Normalize log174 distribution and scale it with experimentally found coefficient
    ////规范化log174分布，并用实验发现的系数对其进行缩放
    float norm_factor = sqrtf(24.0f / variance);
    for (int i = 0; i < FTX_LDPC_N; ++i) {
        log174[i] *= norm_factor;
    }
}

//推算snr
static void ft8_guess_snr(const waterfall_t *wf, candidate_t *cand) {
    const float *mag_signal = wf->mag2 + get_index(wf, cand);


    float signal = 0, noise = 0;
    for (int i = 0; i < 7; ++i) {
        if ((cand->time_offset + i >= 0) && (cand->time_offset + i < wf->num_blocks + 8)) {
            //LOG_PRINTF("End guess SNR 0...");
            signal += mag_signal[(i) * wf->block_stride + kFT8CostasPattern[i]];
            noise += mag_signal[(i) * wf->block_stride + ((kFT8CostasPattern[i] + 4) % 8)];
            //LOG_PRINTF("End guess SNR 0... done");
        }
        if ((cand->time_offset + i + 36 >= 0) && (cand->time_offset + i < wf->num_blocks + 8)) {
            //LOG_PRINTF("End guess SNR 36...");
            signal += mag_signal[(i + 36) * wf->block_stride + kFT8CostasPattern[i]];
            noise += mag_signal[(i + 36) * wf->block_stride + ((kFT8CostasPattern[i] + 4) % 8)];
            //LOG_PRINTF("End guess SNR 36... done");
        }
        //此处容易产生数组下标越界的问题
//        if ((cand->time_offset+i+72>=0)&&(cand->time_offset+i<wf->num_blocks+8)) {
//            LOG_PRINTF("End guess SNR 72...");
//            signal += mag_signal[(i + 72) * wf->block_stride + kFT8CostasPattern[i]];
//            noise += mag_signal[(i + 72) * wf->block_stride + ((kFT8CostasPattern[i] + 4) % 8)];
//            LOG_PRINTF("End guess SNR 72... done");
//        }
    }
    //LOG(LOG_INFO, "Max magnitude:ft8_guess_snr 002\n");
    if (noise != 0) {
        float raw = signal / noise;
        cand->snr = floor(10 * log10f(1E-12f + raw) - 24 + 0.5);
        if (cand->snr < -30) {//-30是最小值了。
            cand->snr = -30;
        }
    } else {
        cand->snr = -100;
    }
}

//max_iterations=20 LDPC的迭代次数。
bool
ft8_decode(waterfall_t *wf, candidate_t *cand, message_t *message, int max_iterations,
           decode_status_t *status) {
    //FT8总消息的为174位，符号是174/3=58个，加上同步costas阵列的7*3=21个符号，共计58+21=79个符号。
    //FTX_LDPC_N=174，是把7*3个符号的位去掉后的数组，
    float log174[FTX_LDPC_N]; //编码为似然的消息位 message bits encoded as likelihood
    if (wf->protocol == PROTO_FT4) {
        ft4_extract_likelihood(wf, cand, log174);
    } else {
        //检测可能的FT8信号,结果在log174中,每3个为一组，与8个格雷码为索引的信号量的平方差的值
        ft8_extract_likelihood(wf, cand, log174);
    }

    //规范化
    ftx_normalize_logl(log174);

    //FTX_LDPC_N=174
    uint8_t plain174[FTX_LDPC_N]; // message bits (0/1)

    //bp_decode是原作者采用的，ldpc_decode经测试也是可以用的。
    //结果在plain174中，以0和1为值。包括77位信息+14位冗余校验+83位前向纠错=174位。
    //max_iterations是最大迭代次数，越大速度越慢，精度越高
    bp_decode(log174, max_iterations, plain174, &status->ldpc_errors);
    //ldpc_decode(log174, max_iterations, plain174, &status->ldpc_errors);

    if (status->ldpc_errors > 0) {
        return false;
    }

    // Extract payload + CRC (first FTX_LDPC_K bits) packed into a byte array
    ////提取压缩到字节数组中的有效负载+CRC（第一个FTX\U LDPC\U K位）
    ////FTX_LDPC_K_BYTES：存储91位所需的整字节数（仅限有效负载+CRC）
    ////FTX_LDPC_K有效负载位数（包括CRC）
    uint8_t a91[FTX_LDPC_K_BYTES];
    //提取出91个位，77位信息+14位冗余校验
    pack_bits(plain174, FTX_LDPC_K, a91);

    // Extract CRC and check it
    ////提取CRC并进行检查，后面crc_extracted又作为hash值保存下来
    status->crc_extracted = ftx_extract_crc(a91);

    // [1]: 'The CRC is calculated on the source-encoded message, zero-extended from 77 to 82 bits.'
    a91[9] &= 0xF8;
    a91[10] &= 0x00;
    status->crc_calculated = ftx_compute_crc(a91, 96 - 14);

    if (status->crc_extracted != status->crc_calculated) {
        return false;
    }

    if (wf->protocol == PROTO_FT4) {
        // '[..] for FT4 only, in order to avoid transmitting a long string of zeros when sending CQ messages,
        // the assembled 77-bit message is bitwise exclusive-OR’ed with [a] pseudorandom sequence before computing the CRC and FEC parity bits'
        for (int i = 0; i < 10; ++i) {
            a91[i] ^= kFT4XORSequence[i];
        }
    }

    //从91位解包77位信息，然后返回消息的文本内容。
    //status->unpack_status = unpack77(a91, message->text);
    message->call_to[0] = message->call_de[0] = message->maidenGrid[0] = message->extra[0] = '\0';
    message->call_de_hash.hash10 = message->call_de_hash.hash12 = message->call_de_hash.hash22 = 0;
    message->call_to_hash.hash10 = message->call_to_hash.hash12 = message->call_to_hash.hash22 = 0;
    memcpy(message->a91, a91, FTX_LDPC_K_BYTES);//把数据包保存下来，用于音频相减

    //LOG_PRINTF("hex:%0x %0x %0x %0x %0x %0x %0x %0x %0x %0x"
    //           ,a91[0],a91[1],a91[2],a91[3],a91[4],a91[5],a91[6],a91[7],a91[8],a91[9]);


    status->unpack_status = unpackToMessage_t(a91, message);
    //message->call_de_hash.hash12=hashcall(message->call_de,HASH_12)  ;

    if (status->unpack_status < 0) {
        return false;
    }

    // Reuse binary message CRC as hash value for the message
    //重用二进制消息CRC作为消息的哈希值
    message->hash = status->crc_extracted;


    //2022-05-13增加解析i3,n3
    //解出i3和n3
    // Extract i3 (bits 74..76)
    //message->i3 = (a91[9] >> 3) & 0x07;
    // Extract n3 (bits 71..73)
    //message->n3 = ((a91[8] << 2) & 0x04) | ((a91[9] >> 6) & 0x03);
    //推算信噪比
    ft8_guess_snr(wf, cand);

    return true;
}

static float max2(float a, float b) {
    return (a >= b) ? a : b;
}

static float max4(float a, float b, float c, float d) {
    return max2(max2(a, b), max2(c, d));
}

static void heapify_down(candidate_t heap[], int heap_size) {
    // heapify from the root down
    int current = 0;
    while (true) {
        int largest = current;
        int left = 2 * current + 1;
        int right = left + 1;

        if (left < heap_size && heap[left].score < heap[largest].score) {
            largest = left;
        }
        if (right < heap_size && heap[right].score < heap[largest].score) {
            largest = right;
        }
        if (largest == current) {
            break;
        }

        candidate_t tmp = heap[largest];
        heap[largest] = heap[current];
        heap[current] = tmp;
        current = largest;
    }
}

static void heapify_up(candidate_t heap[], int heap_size) {
    // heapify from the last node up
    int current = heap_size - 1;
    while (current > 0) {
        int parent = (current - 1) / 2;
        if (heap[current].score >= heap[parent].score) {
            break;
        }

        candidate_t tmp = heap[parent];
        heap[parent] = heap[current];
        heap[current] = tmp;
        current = parent;
    }
}

// Compute unnormalized log likelihood log(p(1) / p(0)) of 2 message bits (1 FSK symbol)
static void ft4_extract_symbol(const uint8_t *wf, float *logl) {
    // Cleaned up code for the simple case of n_syms==1
    float s2[4];

    for (int j = 0; j < 4; ++j) {
        s2[j] = (float) wf[kFT4GrayMap[j]];
    }

    logl[0] = max2(s2[2], s2[3]) - max2(s2[0], s2[1]);
    logl[1] = max2(s2[1], s2[3]) - max2(s2[0], s2[2]);
}

// Compute unnormalized log likelihood log(p(1) / p(0)) of 3 message bits (1 FSK symbol)
//计算3个消息位（1个FSK符号）的非规范化对数似然对数log(（p（1）/p（0）)
//wf当前符号的信号量的地址，logl当前符号的位数组的地址。
static void ft8_extract_symbol(const uint8_t *wf, float *logl) {
    // Cleaned up code for the simple case of n_syms==1
    //清理了n_syms==1简单案例的代码
    float s2[8];//信号强度数组，格雷码数组内容做偏移索引：{ 0, 1, 3, 2, 5, 6, 4, 7 }

    for (int j = 0; j < 8; ++j) {
        s2[j] = (float) wf[kFT8GrayMap[j]];//格雷码值作索引，对应信号的强度保存到
    }
    //信号量的值在之前已经是平方过的了，相减实际上是log(p(1)/p(0))。
    logl[0] = max4(s2[4], s2[5], s2[6], s2[7]) - max4(s2[0], s2[1], s2[2], s2[3]);
    logl[1] = max4(s2[2], s2[3], s2[6], s2[7]) - max4(s2[0], s2[1], s2[4], s2[5]);
    logl[2] = max4(s2[1], s2[3], s2[5], s2[7]) - max4(s2[0], s2[2], s2[4], s2[6]);
}

// Compute unnormalized log likelihood log(p(1) / p(0)) of bits corresponding to several FSK symbols at once
static void
ft8_decode_multi_symbols(const uint8_t *wf, int num_bins, int n_syms, int bit_idx, float *log174) {
    const int n_bits = 3 * n_syms;
    const int n_tones = (1 << n_bits);

    float s2[n_tones];

    for (int j = 0; j < n_tones; ++j) {
        int j1 = j & 0x07;
        if (n_syms == 1) {
            s2[j] = (float) wf[kFT8GrayMap[j1]];
            continue;
        }
        int j2 = (j >> 3) & 0x07;
        if (n_syms == 2) {
            s2[j] = (float) wf[kFT8GrayMap[j2]];
            s2[j] += (float) wf[kFT8GrayMap[j1] + 4 * num_bins];
            continue;
        }
        int j3 = (j >> 6) & 0x07;
        s2[j] = (float) wf[kFT8GrayMap[j3]];
        s2[j] += (float) wf[kFT8GrayMap[j2] + 4 * num_bins];
        s2[j] += (float) wf[kFT8GrayMap[j1] + 8 * num_bins];
    }

    // Extract bit significance (and convert them to float)
    // 8 FSK tones = 3 bits
    for (int i = 0; i < n_bits; ++i) {
        if (bit_idx + i >= FTX_LDPC_N) {
            // Respect array size
            break;
        }

        uint16_t mask = (n_tones >> (i + 1));
        float max_zero = -1000, max_one = -1000;
        for (int n = 0; n < n_tones; ++n) {
            if (n & mask) {
                max_one = max2(max_one, s2[n]);
            } else {
                max_zero = max2(max_zero, s2[n]);
            }
        }

        log174[bit_idx + i] = max_one - max_zero;
    }
}

// Packs a string of bits each represented as a zero/non-zero byte in plain[],
// as a string of packed bits starting from the MSB of the first byte of packed[]
static void pack_bits(const uint8_t bit_array[], int num_bits, uint8_t packed[]) {
    int num_bytes = (num_bits + 7) / 8;
    for (int i = 0; i < num_bytes; ++i) {
        packed[i] = 0;
    }

    uint8_t mask = 0x80;
    int byte_idx = 0;
    for (int i = 0; i < num_bits; ++i) {
        if (bit_array[i]) {
            packed[byte_idx] |= mask;
        }
        mask >>= 1;
        if (!mask) {
            mask = 0x80;
            ++byte_idx;
        }
    }
}