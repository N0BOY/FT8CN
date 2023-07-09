#ifndef _INCLUDE_LDPC_H_
#define _INCLUDE_LDPC_H_

#include <stdint.h>
#include "../common/debug.h"

//// codeword is 174 log-likelihoods.
//// plain is a return value, 174 ints, to be 0 or 1.
//// iters is how hard to try.
//// ok == 87 means success.
//// 码字是174个对数可能性。
//// plain是一个返回值，174 整数，为0或1。
//// max_iters是迭代次数。
//// ok==87表示成功。??好像不是哦，==0才是

void ldpc_decode(float codeword[], int max_iters, uint8_t plain[], int* ok);

void bp_decode(float codeword[], int max_iters, uint8_t plain[], int* ok);

#endif // _INCLUDE_LDPC_H_
