package com.bg7yoz.ft8cn.ft8listener;

import com.bg7yoz.ft8cn.ft8transmit.GenerateFT8;

import java.util.ArrayList;

public class A91List {

    public ArrayList<A91> list=new ArrayList<>();
    public void clear(){
        list.clear();
    }
    public void add(byte[] data,float freq,float sec){
        A91 a91=new A91(data,sec,freq);
        list.add(a91);
    }
    public int size(){
        return list.size();
    }
    public static class A91{
        public byte[] a91 ;//= new byte[GenerateFT8.FTX_LDPC_K_BYTES];
        public float time_sec = 0;//时间偏移(秒)
        public float freq_hz = 0;//频率

        public A91(byte[] a91, float time_sec, float freq_hz) {
            this.a91 = a91;
            this.time_sec = time_sec;
            this.freq_hz = freq_hz;
        }
    }
}
