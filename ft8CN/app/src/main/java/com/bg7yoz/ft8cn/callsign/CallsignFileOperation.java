package com.bg7yoz.ft8cn.callsign;
/**
 * 预处理呼号数据库的文件操作，呼号的来源是CTY.DAT
 * @author BG7YOZ
 * @date 2023-03-20
 */

import android.content.Context;
import android.content.res.AssetManager;

import com.bg7yoz.ft8cn.GeneralVariables;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class CallsignFileOperation {
    public static String TAG="CallsignFileOperation";
    public static String[][] countries;

    /**
     * 从assets目录中的cty.dat中读出呼号分配国家和地区的列表。呼号字符串中包括多个字符串，以逗号分割，
     * @param context 用于调用getAssets()方法。
     * @return ArrayList<CallsignInfo> 返回CallsignInfo数组列表
     */
    public static ArrayList<CallsignInfo> getCallSingInfoFromFile(Context context){
        ArrayList<CallsignInfo> callsignInfos=new ArrayList<>();

        //读出国家和地区的中英文对应翻译表。保存到countries二维数组中。
        countries=getCountryNameToCN(context);

        AssetManager assetManager = context.getAssets();
        try {
            InputStream inputStream= assetManager.open("cty.dat");
            String[] st=getLinesFromInputStream(inputStream,";");
            for (int i = 0; i <st.length ; i++) {
                if (!st[i].contains(":")){
                    continue;
                }
                CallsignInfo callsignInfo=new CallsignInfo(st[i]);
                //查找对用的中文名字
                callsignInfo.CountryNameCN=searchForCountryName(callsignInfo.CountryNameEn);
                callsignInfos.add(callsignInfo);
            }

            inputStream.close();
            //Log.d(TAG,String.format("size:%d",st.length));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return callsignInfos;
    }

    /**
     * 查找对应的国家和地区的中文名字，对应关系在countries二维数组中，一维的0列是英文，1列是中文。
     * @param country 国际和地区的英文名
     * @return String 返回对应的中文，没有就返回null。
     */
    public static String searchForCountryName(String country){
        for (int i = 0; i < countries[0].length; i++) {
            if (countries[0][i].equals(country)){
                return countries[1][i];
            }
        }
        return null;
    }

    /**
     * 从assets目录中的country_en2cn.dat文件中读出国家和地区的英文与中文对应翻译，以冒号分割。
     * @param context 用于调用getAssets。
     * @return 返回一个对应关系的二维数组，一维的0列是英文，1列是中文。
     */
    public static String[][] getCountryNameToCN(Context context){
        AssetManager assetManager = context.getAssets();
        try {
            InputStream inputStream;
            if (GeneralVariables.isTraditionalChinese) {
                inputStream = assetManager.open("country_en2hk.dat");//繁体中文
            }else {
                inputStream = assetManager.open("country_en2cn.dat");//简体中文
            }

            String[] st=getLinesFromInputStream(inputStream,"\n");
            String[][] countries=new String[2][st.length];
            for (int i = 0; i <st.length ; i++) {
                if (!st[i].contains(":")){
                    continue;
                }
                String[] cc=st[i].split(":");
                countries[0][i]=cc[0];
                countries[1][i]=cc[1];
            }
            inputStream.close();
            return countries;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
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

    public static Set<String> getCallsigns(String s){
        String[] ls=s.replace("\n","").split(",");
        Set<String> callsigns=new HashSet<>();
        for (int i = 0; i < ls.length ; i++) {
            if (ls[i].contains(")")) {
                //Log.d(TAG,ls[i]);
                ls[i] = ls[i].substring(0, ls[i].indexOf("("));
                //Log.d(TAG,ls[i]+"     (((");
            }
            if (ls[i].contains("[")) {
                //Log.d(TAG,ls[i]);
                ls[i] = ls[i].substring(0, ls[i].indexOf("["));
                //Log.d(TAG,ls[i]+"     【【【");
            }
            callsigns.add(ls[i].trim());
        }

        return callsigns;
    }
}
