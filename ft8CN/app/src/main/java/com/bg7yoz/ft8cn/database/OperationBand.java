package com.bg7yoz.ft8cn.database;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.bg7yoz.ft8cn.rigs.BaseRigOperation;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * 用于读取可用的载波波段列表，文件保存在assets/bands.txt中
 */

public class OperationBand {
    private static final String TAG="OperationBand";
    private final Context context;
    private static OperationBand operationBand = null;

    public static long getDefaultBand() {
        return 14074000;
    }

    public static String getDefaultWaveLength() {
        return "20m";
    }

    public static ArrayList<Band> bandList = new ArrayList<>();
    public OperationBand(Context context) {
        this.context = context;
        //把波段数据导入到内存
        getBandsFromFile();
    }

    public static OperationBand getInstance(Context context) {
        if (operationBand == null) {
            operationBand=new OperationBand(context);
            return operationBand;
        } else {
            return operationBand;
        }
    }

    /**
     * 获取操作波段的数据，以列表的索引值查找，如果没有返回默认值14.074，20m
     * @param index 索引
     * @return
     */
    public Band getBandByIndex(int index){
        if (index==-1||index>=bandList.size()){
            return new Band(getDefaultBand(),getDefaultWaveLength());
        }else {
            return bandList.get(index);
        }
    }

    /**
     * 检查频率是不是在频率列表中，如果不在，把这个频率加到频段中
     * @param freq
     * @return
     */
    public static int getIndexByFreq(long freq){
        int result=-1;
        for (int i = 0; i < bandList.size(); i++) {
            if (bandList.get(i).band==freq){
                result=i;
                break;
            }
        }
        if (result==-1){
            bandList.add(new Band(freq, BaseRigOperation.getMeterFromFreq(freq)));
            result=bandList.size()-1;
        }
        return result;
    }
    /**
     * 从bands.txt文件中读出FT8信号列表。
     */
    public void getBandsFromFile(){
        AssetManager assetManager = context.getAssets();
        try {
            bandList.clear();
            InputStream inputStream= assetManager.open("bands.txt");
            String[] st=getLinesFromInputStream(inputStream,"\n");
            for (int i = 0; i <st.length ; i++) {
                if (!st[i].contains(":")){
                    continue;
                }
               bandList.add(new Band(st[i]));
            }
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "从波段列表文件提取数据出错："+e.getMessage() );
        }
    }
    public static String getBandInfo(int index){
        if (index>=bandList.size()){
            return bandList.get(0).getBandInfo();
        }else {
            return bandList.get(index).getBandInfo();
        }
    }

    /**
     * 从InputStream中读出字符串
     * @param inputStream 输入流
     * @param deLimited 每行数据的分隔符。
     * @return String 返回字符串,如果失败，返回null
     */
    public static String[] getLinesFromInputStream(InputStream inputStream, String deLimited) {
        try {
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes);
            return (new String(bytes)).split(deLimited);
        }catch (IOException e){
            return null;
        }
    }
    public static long getBandFreq(int index){
        if (index>bandList.size()){
            return 14074000;
        }
        return bandList.get(index).band;
    }

    public static class Band {
        public long band;
        public String waveLength;
        public boolean marked=false;

        public Band(long band, String waveLength) {
            this.band = band;
            this.waveLength = waveLength;
        }

        public Band(String s) {
            String[] info=s.split(":");
            marked= (info[0].equals("*"));
            band=Long.parseLong(info[1]);
            waveLength=info[info.length-1];
        }
        @SuppressLint("DefaultLocale")
        public String getBandInfo(){
                return String.format("%s %.3f Mhz (%s)"
                        ,marked?"*":" "
                        ,(float)(band/1000000f)
                        ,waveLength);
        }
    }


}
