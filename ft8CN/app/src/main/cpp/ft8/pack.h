#ifndef _INCLUDE_PACK_H_
#define _INCLUDE_PACK_H_

#include <stdint.h>

// Pack FT8 text message into 72 bits
// [IN] msg      - FT8 message (e.g. "CQ TE5T KN01")
// [OUT] c77     - 10 byte array to store the 77 bit payload (MSB first)
int pack77(const char* msg, uint8_t* c77);
void packtext77(const char* text, uint8_t* b77);

#endif // _INCLUDE_PACK_H_
