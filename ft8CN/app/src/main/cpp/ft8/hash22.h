#include <stdint.h>


#define HASH_10          (10)  ///哈希码长度为10
#define HASH_12          (12)  ///哈希码长度为12
#define HASH_22          (22)  ///哈希码长度为12
uint32_t hashcall(char* call, int m);

uint32_t hashcall_10(char* call);//返回长度是10的哈希码
uint32_t hashcall_12(char* call);//返回长度是12的哈希码
uint32_t hashcall_22(char* call);//返回长度是22的哈希码