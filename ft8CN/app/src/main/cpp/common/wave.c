#include "wave.h"

#include <stdlib.h>
#include <string.h>
#include <stdio.h>

#include <stdint.h>

// Save signal in floating point format (-1 .. +1) as a WAVE file using 16-bit signed integers.
void save_wav(const float* signal, int num_samples, int sample_rate, const char* path)
{
    char subChunk1ID[4] = { 'f', 'm', 't', ' ' };
    uint32_t subChunk1Size = 16; // 16 for PCM
    uint16_t audioFormat = 1; // PCM = 1
    uint16_t numChannels = 1;
    uint16_t bitsPerSample = 16;
    uint32_t sampleRate = sample_rate;
    uint16_t blockAlign = numChannels * bitsPerSample / 8;
    uint32_t byteRate = sampleRate * blockAlign;

    char subChunk2ID[4] = { 'd', 'a', 't', 'a' };
    uint32_t subChunk2Size = num_samples * blockAlign;

    char chunkID[4] = { 'R', 'I', 'F', 'F' };
    uint32_t chunkSize = 4 + (8 + subChunk1Size) + (8 + subChunk2Size);
    char format[4] = { 'W', 'A', 'V', 'E' };

    int16_t* raw_data = (int16_t*)malloc(num_samples * blockAlign);
    for (int i = 0; i < num_samples; i++)
    {
        float x = signal[i];
        if (x > 1.0)
            x = 1.0;
        else if (x < -1.0)
            x = -1.0;
        raw_data[i] = (int)(x * 32767.0);
    }

    FILE* f = fopen(path, "wb");

    // NOTE: works only on little-endian architecture
    fwrite(chunkID, sizeof(chunkID), 1, f);
    fwrite(&chunkSize, sizeof(chunkSize), 1, f);
    fwrite(format, sizeof(format), 1, f);

    fwrite(subChunk1ID, sizeof(subChunk1ID), 1, f);
    fwrite(&subChunk1Size, sizeof(subChunk1Size), 1, f);
    fwrite(&audioFormat, sizeof(audioFormat), 1, f);
    fwrite(&numChannels, sizeof(numChannels), 1, f);
    fwrite(&sampleRate, sizeof(sampleRate), 1, f);
    fwrite(&byteRate, sizeof(byteRate), 1, f);
    fwrite(&blockAlign, sizeof(blockAlign), 1, f);
    fwrite(&bitsPerSample, sizeof(bitsPerSample), 1, f);

    fwrite(subChunk2ID, sizeof(subChunk2ID), 1, f);
    fwrite(&subChunk2Size, sizeof(subChunk2Size), 1, f);

    fwrite(raw_data, blockAlign, num_samples, f);

    fclose(f);

    free(raw_data);
}

// Load signal in floating point format (-1 .. +1) as a WAVE file using 16-bit signed integers.
int load_wav(float* signal, int* num_samples, int* sample_rate, const char* path)
{
    char subChunk1ID[4]; // = {'f', 'm', 't', ' '};
    uint32_t subChunk1Size; // = 16;    // 16 for PCM
    uint16_t audioFormat; // = 1;     // PCM = 1
    uint16_t numChannels; // = 1;
    uint16_t bitsPerSample; // = 16;
    uint32_t sampleRate;
    uint16_t blockAlign; // = numChannels * bitsPerSample / 8;
    uint32_t byteRate; // = sampleRate * blockAlign;

    char subChunk2ID[4]; // = {'d', 'a', 't', 'a'};
    uint32_t subChunk2Size; // = num_samples * blockAlign;

    char chunkID[4]; // = {'R', 'I', 'F', 'F'};
    uint32_t chunkSize; // = 4 + (8 + subChunk1Size) + (8 + subChunk2Size);
    char format[4]; // = {'W', 'A', 'V', 'E'};

    FILE* f = fopen(path, "rb");
    if (f==NULL){
        return -1;
    }

    // NOTE: works only on little-endian architecture
    fread((void*)chunkID, sizeof(chunkID), 1, f);
    fread((void*)&chunkSize, sizeof(chunkSize), 1, f);
    fread((void*)format, sizeof(format), 1, f);

    fread((void*)subChunk1ID, sizeof(subChunk1ID), 1, f);
    fread((void*)&subChunk1Size, sizeof(subChunk1Size), 1, f);
    if (subChunk1Size != 16)
        return -1;

    fread((void*)&audioFormat, sizeof(audioFormat), 1, f);
    fread((void*)&numChannels, sizeof(numChannels), 1, f);
    fread((void*)&sampleRate, sizeof(sampleRate), 1, f);
    fread((void*)&byteRate, sizeof(byteRate), 1, f);
    fread((void*)&blockAlign, sizeof(blockAlign), 1, f);
    fread((void*)&bitsPerSample, sizeof(bitsPerSample), 1, f);

    if (audioFormat != 1 || numChannels != 1 || bitsPerSample != 16)
        return -1;

    fread((void*)subChunk2ID, sizeof(subChunk2ID), 1, f);
    fread((void*)&subChunk2Size, sizeof(subChunk2Size), 1, f);

    if (subChunk2Size / blockAlign > *num_samples)
        return -2;

    *num_samples = subChunk2Size / blockAlign;
    *sample_rate = sampleRate;

    int16_t* raw_data = (int16_t*)malloc(*num_samples * blockAlign);

    fread((void*)raw_data, blockAlign, *num_samples, f);
    for (int i = 0; i < *num_samples; i++)
    {
        signal[i] = raw_data[i] / 32768.0f;
    }

    free(raw_data);

    fclose(f);

    return 0;
}
