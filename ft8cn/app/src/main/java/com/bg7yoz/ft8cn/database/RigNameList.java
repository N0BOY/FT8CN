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
import java.util.Collections;
import java.util.Comparator;

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
            // Sort rigs by make then model (keep first empty entry at index 0)
            sortRigsByMakeAndModel();
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "从地址列表文件提取数据出错："+e.getMessage() );
        }
    }

    /**
     * Sort rigs by make then model. Keeps the first empty entry at index 0.
     */
    private void sortRigsByMakeAndModel() {
        if (rigList.size() <= 1) {
            return; // Nothing to sort
        }
        
        // Get the first entry (empty placeholder) and the rest
        RigName firstEntry = rigList.get(0);
        ArrayList<RigName> rigsToSort = new ArrayList<>(rigList.subList(1, rigList.size()));
        
        // Sort by make then model
        Collections.sort(rigsToSort, new Comparator<RigName>() {
            @Override
            public int compare(RigName r1, RigName r2) {
                String name1 = r1.modelName.trim();
                String name2 = r2.modelName.trim();
                
                // Extract make (first word) and model (rest)
                String[] parts1 = name1.split("\\s+", 2);
                String[] parts2 = name2.split("\\s+", 2);
                
                String make1 = parts1.length > 0 ? parts1[0] : "";
                String make2 = parts2.length > 0 ? parts2[0] : "";
                String model1 = parts1.length > 1 ? parts1[1] : "";
                String model2 = parts2.length > 1 ? parts2[1] : "";
                
                // Compare by make first
                int makeCompare = make1.compareToIgnoreCase(make2);
                if (makeCompare != 0) {
                    return makeCompare;
                }
                
                // If makes are equal, compare by model
                return model1.compareToIgnoreCase(model2);
            }
        });
        
        // Rebuild the list with first entry followed by sorted rigs
        rigList.clear();
        rigList.add(firstEntry);
        rigList.addAll(rigsToSort);
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

    /**
     * Extract make (brand) from a model name.
     * @param modelName Full model name (e.g., "ICOM IC-705")
     * @return Make/brand name (e.g., "ICOM") or empty string if not found
     */
    public static String extractMake(String modelName) {
        if (modelName == null || modelName.trim().isEmpty()) {
            return "";
        }
        String[] parts = modelName.trim().split("\\s+", 2);
        return parts.length > 0 ? parts[0] : "";
    }

    /**
     * Extract model name from a full model name (without make).
     * @param modelName Full model name (e.g., "ICOM IC-705")
     * @return Model name without make (e.g., "IC-705") or full name if no make found
     */
    public static String extractModel(String modelName) {
        if (modelName == null || modelName.trim().isEmpty()) {
            return "";
        }
        String[] parts = modelName.trim().split("\\s+", 2);
        return parts.length > 1 ? parts[1] : modelName.trim();
    }

    /**
     * Get list of unique makes (brands) from the rig list.
     * @return ArrayList of unique make names, with empty string as first entry
     */
    public ArrayList<String> getUniqueMakes() {
        ArrayList<String> makes = new ArrayList<>();
        makes.add(""); // Empty entry first
        
        for (int i = 1; i < rigList.size(); i++) {
            String make = extractMake(rigList.get(i).modelName);
            if (!make.isEmpty() && !makes.contains(make)) {
                makes.add(make);
            }
        }
        
        // Sort makes (keep empty at index 0)
        Collections.sort(makes.subList(1, makes.size()));
        return makes;
    }

    /**
     * Get list of rig indices that match the given make.
     * @param make The make/brand to filter by (empty string returns all)
     * @return ArrayList of indices in rigList that match the make
     */
    public ArrayList<Integer> getRigIndicesByMake(String make) {
        ArrayList<Integer> indices = new ArrayList<>();
        
        if (make == null || make.isEmpty()) {
            // Return all indices (including empty at 0)
            for (int i = 0; i < rigList.size(); i++) {
                indices.add(i);
            }
            return indices;
        }
        
        // Add empty entry first
        indices.add(0);
        
        // Add all rigs matching the make
        for (int i = 1; i < rigList.size(); i++) {
            String rigMake = extractMake(rigList.get(i).modelName);
            if (make.equals(rigMake)) {
                indices.add(i);
            }
        }
        
        return indices;
    }

    /**
     * Find the full rig list index for a model name within a filtered list.
     * @param make The make/brand
     * @param modelName The model name (without make, e.g., "IC-705")
     * @return The index in the full rigList, or -1 if not found
     */
    public int findRigIndexByMakeAndModel(String make, String modelName) {
        if (make == null || make.isEmpty() || modelName == null || modelName.isEmpty()) {
            return 0; // Return empty entry
        }
        
        String fullModelName = make + " " + modelName;
        
        for (int i = 0; i < rigList.size(); i++) {
            if (rigList.get(i).modelName.equals(fullModelName)) {
                return i;
            }
        }
        
        return -1; // Not found
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
