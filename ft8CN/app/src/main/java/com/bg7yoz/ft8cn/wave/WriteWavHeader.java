package com.bg7yoz.ft8cn.wave;

import android.media.AudioFormat;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

public class WriteWavHeader {
    private int samplesBits;
    private final int channels;
    private final int totalAudioLen;
    private final int longSampleRate;
    String TAG = "HamAudioRecorder";//调试用标志

    public WriteWavHeader( int totalAudioLen, int longSampleRate, int channels, int samplesBits) {
        if (samplesBits == AudioFormat.ENCODING_PCM_16BIT)
            this.samplesBits = 16;
        else if (samplesBits == AudioFormat.ENCODING_PCM_8BIT)
            this.samplesBits = 8;

        if (channels == AudioFormat.CHANNEL_IN_STEREO)
            this.channels = 2;
        else
            this.channels = 1;
        this.totalAudioLen = totalAudioLen;
        this.longSampleRate = longSampleRate;

    }

    private byte[] makeWaveHeader(){
        int file_size = totalAudioLen + 44 - 8;//文件大小，刨除前面RIFF和file_size
        byte[] header = new byte[44];
        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (file_size & 0xff);
        header[5] = (byte) ((file_size >> 8) & 0xff);
        header[6] = (byte) ((file_size >> 16) & 0xff);
        header[7] = (byte) ((file_size >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1表示PCM编码
        header[21] = 0;
        header[22] = (byte) channels;//1为单声道，2是双声道
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);

        header[28] = (byte) (samplesBits & 0xff);
        header[29] = (byte) ((samplesBits >> 8) & 0xff);
        header[30] = (byte) ((samplesBits >> 16) & 0xff);
        header[31] = (byte) ((samplesBits >> 24) & 0xff);

        //2字节数据块长度(每个样本的字节数=通道数*每次采样得到的样本位数/8)
        if (samplesBits==AudioFormat.ENCODING_PCM_16BIT) {
            header[32] = (byte) (channels * samplesBits);
            header[33] = 0;
            header[34] = 16; // 每个采样点的位数
            header[35] = 0;
        }else if (samplesBits==AudioFormat.ENCODING_PCM_8BIT){
            header[32] = (byte) (channels );
            header[33] = 0;
            header[34] = 8; // 每个采样点的位数
            header[35] = 0;
        }else {
            header[32] = (byte) (channels * samplesBits / 8);
            header[33] = 0;
            header[34] = (byte) samplesBits; //每个采样点的位数
            header[35] = 0;
        }


        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        //pcm音频数据大小
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        return header;
    }

    public void writeHeader(DataOutputStream dos) {

        try {
            dos.write(makeWaveHeader());
        } catch (IOException e) {
            Log.e(TAG, String.format("创建wav文件头（WriteWavHeader）错误！%s", e.getMessage()));
        }

    }
    public void modifyHeader(String fileName) {

        try {
            RandomAccessFile raf=new RandomAccessFile(fileName,"rw");
            raf.seek(0);
            raf.write(makeWaveHeader());
            raf.close();
        } catch (IOException e) {
            Log.e(TAG, String.format("修改wav文件头（modifyHeader）错误！%s", e.getMessage()));
        }

    }

}
