#include "text.h"

#include <string.h>

const char* trim_front(const char* str)
{
    // Skip leading whitespace
    while (*str == ' ')
    {
        str++;
    }
    return str;
}

void trim_back(char* str)
{
    // Skip trailing whitespace by replacing it with '\0' characters
    int idx = strlen(str) - 1;
    while (idx >= 0 && str[idx] == ' ')
    {
        str[idx--] = '\0';
    }
}

// 1) trims a string from the back by changing whitespaces to '\0'
// 2) trims a string from the front by skipping whitespaces
char* trim(char* str)
{
    str = (char*)trim_front(str);
    trim_back(str);
    // return a pointer to the first non-whitespace character
    return str;
}

char to_upper(char c)
{
    return (c >= 'a' && c <= 'z') ? (c - 'a' + 'A') : c;
}

bool is_digit(char c)
{
    return (c >= '0') && (c <= '9');
}

bool is_letter(char c)
{
    return ((c >= 'A') && (c <= 'Z')) || ((c >= 'a') && (c <= 'z'));
}

bool is_space(char c)
{
    return (c == ' ');
}

bool in_range(char c, char min, char max)
{
    return (c >= min) && (c <= max);
}

bool starts_with(const char* string, const char* prefix)
{
    return 0 == memcmp(string, prefix, strlen(prefix));
}

bool equals(const char* string1, const char* string2)
{
    return 0 == strcmp(string1, string2);
}

int char_index(const char* string, char c)
{
    for (int i = 0; *string; ++i, ++string)
    {
        if (c == *string)
        {
            return i;
        }
    }
    return -1; // Not found
}

// Text message formatting:
//   - replaces lowercase letters with uppercase
//   - merges consecutive spaces into single space
void fmtmsg(char* msg_out, const char* msg_in)
{
    char c;
    char last_out = 0;
    while ((c = *msg_in))
    {
        if (c != ' ' || last_out != ' ')
        {
            last_out = to_upper(c);
            *msg_out = last_out;
            ++msg_out;
        }
        ++msg_in;
    }
    *msg_out = 0; // Add zero termination
}

// Parse a 2 digit integer from string
int dd_to_int(const char* str, int length)
{
    int result = 0;
    bool negative;
    int i;
    if (str[0] == '-')
    {
        negative = true;
        i = 1; // Consume the - sign
    }
    else
    {
        negative = false;
        i = (str[0] == '+') ? 1 : 0; // Consume a + sign if found
    }

    while (i < length)
    {
        if (str[i] == 0)
            break;
        if (!is_digit(str[i]))
            break;
        result *= 10;
        result += (str[i] - '0');
        ++i;
    }

    return negative ? -result : result;
}

// Convert a 2 digit integer to string
void int_to_dd(char* str, int value, int width, bool full_sign)
{
    if (value < 0)
    {
        *str = '-';
        ++str;
        value = -value;
    }
    else if (full_sign)
    {
        *str = '+';
        ++str;
    }

    int divisor = 1;
    for (int i = 0; i < width - 1; ++i)
    {
        divisor *= 10;
    }

    while (divisor >= 1)
    {
        int digit = value / divisor;

        *str = '0' + digit;
        ++str;

        value -= digit * divisor;
        divisor /= 10;
    }
    *str = 0; // Add zero terminator
}

// convert integer index to ASCII character according to one of 6 tables:
// table 0: " 0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ+-./?"
// table 1: " 0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
// table 2: "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
// table 3: "0123456789"
// table 4: " ABCDEFGHIJKLMNOPQRSTUVWXYZ"
// table 5: " 0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ/"
char charn(int c, int table_idx)
{
    if (table_idx != 2 && table_idx != 3)
    {
        if (c == 0)
            return ' ';
        c -= 1;
    }
    if (table_idx != 4)
    {
        if (c < 10)
            return '0' + c;
        c -= 10;
    }
    if (table_idx != 3)
    {
        if (c < 26)
            return 'A' + c;
        c -= 26;
    }

    if (table_idx == 0)
    {
        if (c < 5)
            return "+-./?"[c];
    }
    else if (table_idx == 5)
    {
        if (c == 0)
            return '/';
    }

    return '_'; // unknown character, should never get here
}

// Convert character to its index (charn in reverse) according to a table
int nchar(char c, int table_idx)
{
    int n = 0;
    if (table_idx != 2 && table_idx != 3)
    {
        if (c == ' ')
            return n + 0;
        n += 1;
    }
    if (table_idx != 4)
    {
        if (c >= '0' && c <= '9')
            return n + (c - '0');
        n += 10;
    }
    if (table_idx != 3)
    {
        if (c >= 'A' && c <= 'Z')
            return n + (c - 'A');
        n += 26;
    }

    if (table_idx == 0)
    {
        if (c == '+')
            return n + 0;
        if (c == '-')
            return n + 1;
        if (c == '.')
            return n + 2;
        if (c == '/')
            return n + 3;
        if (c == '?')
            return n + 4;
    }
    else if (table_idx == 5)
    {
        if (c == '/')
            return n + 0;
    }

    // Character not found
    return -1;
}
