

#include "hash22.h"
#include "stdlib.h"
#include "string.h"
//m为hash的长度，12，22
//call的长度是12（包括'\0'）
uint32_t hashcall(char* call, int m)
{
    const char *chars = " 0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ/";
    char callsign[11]="           ";

    char *temp=call;

    int j=0;
    while(temp[0] == ' '){
        ++temp;
        if (temp[0]!=' ')
        {
           break;
        }
        j++;
    }

    for (int i = 0; i < 11-j; i++)
    {
       if (temp[i]=='\0')
       {
         break;
       }else{
           callsign[i]=temp[i];
       }
       
    }
    
    uint64_t x = 0;
    for(int i = 0; i < 11; i++){
       
        int c = (int)callsign[i];
        const char *p = strchr(chars, c);
        if (p==NULL)
        {
            return 0;
        }

        int j = p - chars;
        x = 38*x + j;
    }

    x = x * 47055833459LL;
    x = x >> (64 - m);

    return x;

}

uint32_t hashcall_10(char* call){
    return hashcall(call,HASH_10);
}
uint32_t hashcall_12(char* call){
    return hashcall(call,HASH_12);
}
uint32_t hashcall_22(char* call){
    return hashcall(call,HASH_22);
}