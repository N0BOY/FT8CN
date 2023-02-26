package com.bg7yoz.ft8cn.log;

import android.util.Log;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * 日志文件导入。
 * 构建方法需要日志文件名，此处的文件是由NanoHTTPd的session中的post过来的。
 * getFileContext是获取全部文件内容。
 * getLogBody是获取日志文件中全部的原始记录内容，也就是全部以<eoh>后面的数据
 * getLogRecords是获取拆解后的全部记录列表，记录是以HashMap方式保存的，其中HashMap的Key是字段名（大写），value是实际的值
 * @author  BG7YOZ
 */

public class LogFileImport {
    private static final String TAG = "LogFileImport";
    private final String fileContext;
    private final HashMap<Integer,String> errorLines=new HashMap<>();

    /**
     * 构建函数，需要文件名，如果在读取文件时出错，会回抛异常
     *
     * @param logFileName 日志文件名
     * @throws IOException 回抛异常
     */
    public LogFileImport(String logFileName) throws IOException {
        FileInputStream logFileStream = new FileInputStream(logFileName);
        byte[] bytes = new byte[logFileStream.available()];
        logFileStream.read(bytes);
        fileContext = new String(bytes);
    }

    /**
     * 获取日志文件的全部内容
     *
     * @return 全部文本
     */
    public String getFileContext() {
        return fileContext;
    }

    public String getLogBody() {
        String[] temp = fileContext.split("[<][Ee][Oo][Hh][>]");
        if (temp.length > 1) {
            return temp[temp.length - 1];
        } else {
            return "";
        }
    }

    /**
     * 获取日志文件中全部的记录，每条记录是以HashMap保存的。HashMap的Key是字段名（大写），Value是值。
     *
     * @return 记录列表。ArrayList
     */
    public ArrayList<HashMap<String, String>> getLogRecords() {
        String[] temp = getLogBody().split("[<][Ee][Oo][Rr][>]");//拆解出每个记录的原始内容
        ArrayList<HashMap<String, String>> records = new ArrayList<>();
        int count=0;//解析计数器
        for (String s : temp) {//对每一个原始记录内容做拆解
            count++;
            if (!s.contains("<")) {
                continue;
            }//说明没有标签，不做拆解
            try {
                HashMap<String, String> record = new HashMap<>();//创建一个记录
                String[] fields = s.split("<");//拆解记录的每一个字段

                for (String field : fields) {//对每一个原始记录做拆解

                    if (field.length() > 1) {//如果是可拆解的
                        String[] values = field.split(">");//拆解记录的字段名和值

                        if (values.length > 1) {//如果是可拆解的
                            if (values[0].contains(":")) {//拆解字段名和字段的长度，冒号前的字段名，后面是长度
                                String[] ttt = values[0].split(":");
                                if (ttt.length > 1) {
                                    String name = ttt[0];//字段名
                                    int valueLen = Integer.parseInt(ttt[1]);//字段长度
                                    if (valueLen > 0) {
                                        if (values[1].length() < valueLen) {
                                            valueLen = values[1].length() - 1;
                                        }
                                        String value = values[1].substring(0, valueLen);//字段值
                                        record.put(name.toUpperCase(), value);//保存字段,key要大写
                                    }
                                }

                            }
                        }
                    }
                }
                records.add(record);//保存记录
            }catch (Exception e){
                errorLines.put(count,s.replace("<","&lt;"));//把错误的内容保存下来。
            }
        }
        return records;
    }
    public int getErrorCount(){
        return errorLines.size();
    }
    public HashMap<Integer,String> getErrorLines(){
        return errorLines;
    }
}
