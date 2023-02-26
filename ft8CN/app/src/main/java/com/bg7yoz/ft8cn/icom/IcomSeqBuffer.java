package com.bg7yoz.ft8cn.icom;

import java.util.ArrayList;

public class IcomSeqBuffer {
    private long last=System.currentTimeMillis();

    public static class SeqBufEntry {
        public short seq;//序号
        public byte[] data;//数据
        public long addedAt;//添加的时间,最多保存10秒钟

        public SeqBufEntry(short seq, byte[] data) {
            this.seq = seq;
            this.data = data;
            addedAt = System.currentTimeMillis();
        }
    }

    public ArrayList<SeqBufEntry> entries = new ArrayList<>();

    /**
     * 添加指令号缓存
     *
     * @param seq  序号
     * @param data 指令
     */
    public synchronized void add(short seq, byte[] data) {
        entries.add(new SeqBufEntry(seq, data));
        last=System.currentTimeMillis();
        purgeOldEntries();//要删除多余的历史记录
    }

    /**
     * 弹出旧的指令，保证指令在缓存的限定范围内
     */
    public void purgeOldEntries() {
        if (entries.size() == 0) return;
        long now=System.currentTimeMillis();
        for (int i = entries.size()-1; i >=0 ; i--) {//删除超过10秒的历史记录
            if (now-entries.get(i).addedAt>IComPacketTypes.PURGE_MILLISECONDS){
                entries.remove(i);
            }
        }

        //while (entries.size() > MaxBufferCount) {
        //    entries.remove(0);
        //}
    }

    /**
     * 按序号查找缓存中是否有历史指令
     *
     * @param seqNum 序号
     * @return 指令数据，如果没有为NULL。
     */
    public synchronized byte[] get(int seqNum) {
        int founded = -1;
        for (int i = entries.size() - 1; i >= 0; i--) {
            if (entries.get(i).seq == seqNum) {
                founded = i;
            }
        }
        if (founded != -1) {
            return entries.get(founded).data;
        } else {
            return null;
        }
    }
    public long getTimeOut(){
        return System.currentTimeMillis()-last;
    }
}
