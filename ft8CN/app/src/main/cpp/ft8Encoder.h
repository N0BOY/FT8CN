//
// Created by jmsmf on 2022/6/1.
//

#ifndef FT8CN_FT8ENCODER_H
#define FT8CN_FT8ENCODER_H

#endif //FT8CN_FT8ENCODER_H
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <math.h>
#include <stdbool.h>

#include "common/common.h"
//#include "common/wave.h"
#include "common/debug.h"
#include "ft8/pack.h"
#include "ft8/encode.h"
#include "ft8/constants.h"

const int Ft8num_samples = 15*12000;//FT8采样数，不是字节数，16bit,字节数要乘以2
//void generateFt8ToFile(char* message,float frequency,char* wav_path,bool is_ft4);
void generateFt8ToBuffer(char* message,float frequency,short * buffer);
void synth_gfsk(const uint8_t* symbols, int n_sym, float f0, float symbol_bt, float symbol_period, int signal_rate, float* signal);