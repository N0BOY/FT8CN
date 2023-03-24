package com.bg7yoz.ft8cn.database;
/**
 * 各电台信号的列表。文件在rigaddress.txt中
 * @author BGY70Z
 * @date 2023-03-20
 */

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.R;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class RigNameList {
    private static final String TAG="RigNameList";
    private Context context;
    private static RigNameList rigNameList = null;


    public ArrayList<RigName> rigList = new ArrayList<>();

    public RigNameList(Context context) {
        this.context = context;
        //电台数据导入到内存
        getRigNamesFromFile();
    }

    public static RigNameList getInstance(Context context) {
        if (rigNameList == null) {
            return new RigNameList(context);
        } else {
            return rigNameList;
        }
    }

    /**
     * 获取各电台参数数据，以列表的索引值查找，如果没有返回默认值以空
     * @param index 索引
     * @return 电台参数
     */
    public RigName getRigNameByIndex(int index){
        if (index==-1||index>=rigList.size()){
            return new RigName("",0xA4,19200,0);
        }else {
            return rigList.get(index);
        }
    }

    /**
     * rigaddress.txt文件中读出各电台参数列表。
     */
    public void getRigNamesFromFile(){
        AssetManager assetManager = context.getAssets();
        try {
            InputStream inputStream= assetManager.open("rigaddress.txt");
            String[] st=getLinesFromInputStream(inputStream,"\n");
            rigList.add(new RigName("",0xA4,19200,0));
            for (int i = 0; i <st.length ; i++) {
                if (!st[i].contains(",")){
                    continue;
                }
               rigList.add(new RigName(st[i]));
            }
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "从地址列表文件提取数据出错："+e.getMessage() );
        }
    }
    public String getRigNameInfo(int index){
        return rigList.get(index).getName();
    }
    public int getIndexByAddress(int addr){
        int index=-1;
        for (int i = 1; i <rigList.size() ; i++) {
            if (rigList.get(i).address==addr){
                index=i;
                break;
            }
        }
        if (index==-1){//如果没找到，就返回第一个，“空”
            return 0;
        }else {
            return index;
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


    public static class RigName {
        public String modelName;
        public int address;//地址
        public int bauRate;//波特率
        public int instructionSet;//指令集0:icom,1:yaesu 2代,2:yaesu 3代

        public RigName(String modelName, int address, int bauRate,int instructionSet) {
            this.modelName = modelName;
            this.address = address;
            this.bauRate = bauRate;
            this.instructionSet=instructionSet;
        }

        /**
         * 把String格式数据转换成电台型号ICOM IC-705,A4,19200
         * @param s
         */
        public RigName(String s) {
            String[] info=s.split(",");
            if (info.length<4){
                modelName="";
                address=0xA4;
                bauRate=19200;
                instructionSet=0;
                return;
            }
            modelName= info[0].trim();
            address=Integer.parseInt(info[1].trim(),16);
            bauRate=Integer.parseInt(info[2].trim());
            instructionSet=Integer.parseInt(info[3].trim());
        }

        public String getName(){
            if (modelName.equals("")) {
                return GeneralVariables.getStringFromResource(R.string.none);
            }else {
                return modelName;
            }
        }
    }
}
