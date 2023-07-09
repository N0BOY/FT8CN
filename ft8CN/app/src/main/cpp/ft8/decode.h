#ifndef _INCLUDE_DECODE_H_
#define _INCLUDE_DECODE_H_

#include <stdint.h>
#include <stdbool.h>

#include "constants.h"
#include "../fft/kiss_fft.h"
#include "../common/debug.h"
/// Input structure to ft8_find_sync() function. This structure describes stored waterfall data over the whole message slot.
/// Fields time_osr and freq_osr specify additional oversampling rate for time and frequency resolution.
/// If time_osr=1, FFT magnitude data is collected once for every symbol transmitted, i.e. every 1/6.25 = 0.16 seconds.
/// Values time_osr > 1 mean each symbol is further subdivided in time.
/// If freq_osr=1, each bin in the FFT magnitude data corresponds to 6.25 Hz, which is the tone spacing.
/// Values freq_osr > 1 mean the tone spacing is further subdivided by FFT analysis.

/// ft8_find_sync（）函数的输入结构。这种结构描述了整个消息槽中存储的瀑布数据。
/// time_osr和freq_osr字段指定时间和频率分辨率的额外过采样率。
/// 如果time_osr=1，则针对每个传输的符号收集一次FFT幅度数据，即每1/6.25=0.16秒收集一次。FSK符号的时间长度是0.16秒。
/// 如果time_osr>1表示每个符号在时间上进一步过采样。
/// 如果freq_osr=1，FFT幅度数据中的每个单元对应于6.25 Hz，这是音调间隔（FSK的符号时长）。
/// 如果freq_osr>1意味着通过FFT分析进一步对音调间隔过采样。

typedef struct
{
    int max_blocks;          ///< mag阵列中分配的块（符号）数。number of blocks (symbols) allocated in the mag array
    int num_blocks;          ///< mag阵列中存储的块（符号）编号，时域序列号。number of blocks (symbols) stored in the mag array
    //num_bins = 12000 * 0.16 / 2 = 960
    int num_bins;            ///< 以6.25 Hz为单位的FFT箱数量（960）。number of FFT bins in terms of 6.25 Hz
    int time_osr;            ///< 时间过采样率（时间细分数）。number of time subdivisions
    int freq_osr;            ///< 频率过采样率（频率细分数）。number of frequency subdivisions
    uint8_t* mag;            ///< FFT的magnitudes（量级）存储。FFT magnitudes stored as uint8_t[blocks][time_osr][freq_osr][num_bins]
    int block_stride;        ///< 块的步态？没搞懂。Helper value = time_osr * freq_osr * num_bins
    ftx_protocol_t protocol; ///< 协议。Indicate if using FT4 or FT8
    float* mag2;///用于存储准确信号量的数组，这是个平方值，真正的值需要开方，为了速度，就不开方了，等后续再计算。

} waterfall_t;

/// Output structure of ft8_find_sync() and input structure of ft8_decode().
/// Holds the position of potential start of a message in time and frequency.
// 此结构是ft8_find_sync()的输出结构，ft8_decode()的输入
// 在时间和频率上，保持消息的潜在起始位置。
typedef struct
{
    int16_t score;       ///< score 候选分数（非负数；分数越高表示可能性越大）。Candidate score (non-negative number; higher score means higher likelihood)
    int16_t time_offset; ///< 时间段索引。Index of the time block
    int16_t freq_offset; ///< 频率段索引。Index of the frequency bin
    uint8_t time_sub;    ///< 所用时间细分的索引。Index of the time subdivision used
    uint8_t freq_sub;    ///< 所用频率细分的索引。Index of the frequency subdivision used
    int  snr;//信噪比
} candidate_t;

/// Structure that holds the decoded message
typedef struct {
    uint32_t hash22;
    uint32_t hash12;
    uint32_t hash10;
} hashCode;
// 保存已解码消息的结构
typedef struct
{
    //2022-05-13增加i3和n3
    uint8_t i3;
    uint8_t n3;

    // TODO: check again that this size is enough
    //char text[25]; ///< 纯文本，Plain text，原文是25，
    char text[48]; ///<但在在unpack.c中，unpack77函数的最大可能是14+14+19=
    uint16_t hash; ///用于对消息hash，防止消息重复< Hash value to be used in hash table and quick checking for duplicates

    //2022-05-26新增以下内容
    char call_to[14];//被呼叫的呼号
    char call_de[14];//发起的呼号
    char extra[19];//扩展内容

    //---TODO-------------
    char maidenGrid[5];//梅登海德
    int report;//信号报告

    hashCode call_to_hash;//22位长度的哈希码
    hashCode call_de_hash;//22位长度的哈希码
    //TO HASH , FROM HASH

    uint8_t a91[FTX_LDPC_K_BYTES];//用于生成减法代码的数据

} message_t;

/// Structure that contains the status of various steps during decoding of a message
/// 包含消息解码过程中各个步骤的状态的结构
typedef struct
{
    int ldpc_errors;         ///< 解码期间的LDPC（稀疏校验矩阵）错误数。Number of LDPC errors during decoding
    uint16_t crc_extracted;  ///< 从消息中恢复的CRC值。CRC value recovered from the message
    uint16_t crc_calculated; ///< 在有效负载上计算的CRC值。CRC value calculated over the payload
    int unpack_status;       ///< 解包例程的返回值。Return value of the unpack routine
} decode_status_t;

/// Localize top N candidates in frequency and time according to their sync strength (looking at Costas symbols)
/// 根据同步强度（查看Costas符号），在频率和时间上对前N名候选人进行本地化。
/// We treat and organize the candidate list as a min-heap (empty initially).
/// 我们将候选列表视为一个最小堆（最初为空）。

/// @param[in] power 在消息槽期间收集的瀑布数据。Waterfall data collected during message slot 
/// @param[in] sync_pattern 同步模式。Synchronization pattern
/// @param[in] num_candidates 最大候选数量（堆数组大小）。Number of maximum candidates (size of heap array)
/// @param[in,out] heap 候选项类型的数组（分配了num个候选项）。Array of candidate_t type entries (with num_candidates allocated entries)
/// @param[in] min_score 删减不太可能的候选项所允许的最低分数（可以为零，没有效果）。Minimal score allowed for pruning unlikely candidates (can be zero for no effect)
/// @return 堆中填写的候选人数。Number of candidates filled in the heap
int ft8_find_sync(const waterfall_t* power, int num_candidates, candidate_t heap[], int min_score);

/// Attempt to decode a message candidate. Extracts the bit probabilities, runs LDPC decoder, checks CRC and unpacks the message in plain text.
/// 尝试解码候选消息。提取比特概率，运行LDPC解码器，检查CRC，并将消息解压为纯文本。
/// @param[in] power 在消息槽期间收集的瀑布数据。Waterfall data collected during message slot
/// @param[in] cand 要解码的候选人。Candidate to decode
/// @param[out] message 将接收解码消息的message_t结构。message_t structure that will receive the decoded message
/// @param[in] max_iterations 允许的最大LDPC迭代次数（数字越小，解码速度越快，但精度越低）。Maximum allowed LDPC iterations (lower number means faster decode, but less precise)
/// @param[out] status decode_status_t结构，该结构将填充各种解码步骤的状态。decode_status_t structure that will be filled with the status of various decoding steps
/// @return 如果解码成功，则为True，否则为false（查看状态了解详细信息）。True if the decoding was successful, false otherwise (check status for details)
bool ft8_decode(waterfall_t* power, candidate_t* cand, message_t* message, int max_iterations, decode_status_t* status);

#endif // _INCLUDE_DECODE_H_
