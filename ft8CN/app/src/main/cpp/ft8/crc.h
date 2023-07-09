#ifndef _INCLUDE_CRC_H_
#define _INCLUDE_CRC_H_

#include <stdint.h>
#include <stdbool.h>

// Compute 14-bit CRC for a sequence of given number of bits using FT8/FT4 CRC polynomial
// [IN] message  - byte sequence (MSB first)
// [IN] num_bits - number of bits in the sequence
uint16_t ftx_compute_crc(const uint8_t message[], int num_bits);

/// Extract the FT8/FT4 CRC of a packed message (during decoding)
/// 提取压缩消息的FT8/FT4 CRC（解码期间）
/// @param[in] a91 77 bits of payload data + CRC
/// @return Extracted CRC
uint16_t ftx_extract_crc(const uint8_t a91[]);

/// Add FT8/FT4 CRC to a packed message (during encoding)
/// @param[in] payload 77 bits of payload data
/// @param[out] a91 91 bits of payload data + CRC
void ftx_add_crc(const uint8_t payload[], uint8_t a91[]);

#endif // _INCLUDE_CRC_H_