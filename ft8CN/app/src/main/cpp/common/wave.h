#ifndef _INCLUDE_WAVE_H_
#define _INCLUDE_WAVE_H_

// Save signal in floating point format (-1 .. +1) as a WAVE file using 16-bit signed integers.
void save_wav(const float* signal, int num_samples, int sample_rate, const char* path);

// Load signal in floating point format (-1 .. +1) as a WAVE file using 16-bit signed integers.
int load_wav(float* signal, int* num_samples, int* sample_rate, const char* path);

#endif // _INCLUDE_WAVE_H_
