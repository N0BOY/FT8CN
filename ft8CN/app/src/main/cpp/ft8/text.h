#ifndef _INCLUDE_TEXT_H_
#define _INCLUDE_TEXT_H_

#include <stdbool.h>
#include <stdint.h>

// Utility functions for characters and strings

const char* trim_front(const char* str);
void trim_back(char* str);
char* trim(char* str);

char to_upper(char c);
bool is_digit(char c);
bool is_letter(char c);
bool is_space(char c);
bool in_range(char c, char min, char max);
bool starts_with(const char* string, const char* prefix);
bool equals(const char* string1, const char* string2);

int char_index(const char* string, char c);

// Text message formatting:
//   - replaces lowercase letters with uppercase
//   - merges consecutive spaces into single space
void fmtmsg(char* msg_out, const char* msg_in);

// Parse a 2 digit integer from string
int dd_to_int(const char* str, int length);

// Convert a 2 digit integer to string
void int_to_dd(char* str, int value, int width, bool full_sign);

char charn(int c, int table_idx);
int nchar(char c, int table_idx);

#endif // _INCLUDE_TEXT_H_
