package com.bg7yoz.ft8cn.ft8transmit;
/**
 * 通联记录的列表
 * @author BGY70Z
 * @date 2023-03-20
 */

import com.bg7yoz.ft8cn.log.QSLRecord;

import java.util.ArrayList;

public class QslRecordList extends ArrayList<QSLRecord> {

    /**
     * 根据呼号查是否有通联记录
     * @param callsign 呼号
     * @return 记录，没有则为空
     */
    public QSLRecord getRecordByCallsign(String callsign){
        for (int i = this.size()-1; i >=0 ; i--) {
            if (this.get(i).getToCallsign().equals(callsign)){
                return this.get(i);
            }
        }
        return null;
    }

    /**
     * 按照呼号查找，是否有通联记录，且保存过。如果没有记录，视作没保存过。
     * @param callsign 呼号
     * @return 是否保存过
     */
    public boolean getSavedRecByCallsign(String callsign){
        QSLRecord record=getRecordByCallsign(callsign);
        if (record==null){
            return false;
        }else {
            return record.saved;
        }
    }

    /**
     * 添加通联过的记录，如果已经存在，就更新记录
     * @param record 通联记录
     * @return 通联记录
     */
    public QSLRecord addQSLRecord(QSLRecord record){
        if (record.getToCallsign().equals("CQ")) return null;
        //清除已经保存过的通联记录
        //for (int i = this.size()-1; i >=0 ; i--) {
        //    if (this.get(i).getToCallsign().equals(record.getToCallsign())){
        //        if (this.get(i).saved){
        //            this.remove(i);
        //        }
        //    }
        //}
        //找一下看有没有已经在列表中，但还没有保存的记录
        QSLRecord oldRecord= getRecordByCallsign(record.getToCallsign());
        if (oldRecord==null){
            this.add(record);
            return record;
        }else {
            oldRecord.update(record);
        }
        return oldRecord;
    }

    /**
     * 删除已经保存过的呼号
     * @param record
     */
    public void deleteIfSaved(QSLRecord record){
        //清除已经保存过的通联记录
        for (int i = this.size()-1; i >=0 ; i--) {
            if (this.get(i).getToCallsign().equals(record.getToCallsign())){
                if (this.get(i).saved){
                    this.remove(i);
                }
            }
        }
    }

    public String toHTML(){
        StringBuilder html=new StringBuilder();
        for (int i = 0; i < this.size(); i++) {
            if (i%2==0) {
                html.append("<tr>");
                html.append(String.format("<td class=\"default\" >%s</td>", this.get(i).toHtmlString()));
                html.append("<br>\n</tr>\n");
            }else {
                html.append("<tr  class=\"bbb\">>");
                html.append(String.format("<td class=\"default\" >%s</td>", this.get(i).toHtmlString()));
                html.append("<br>\n</tr>\n");
            }

        }
        return html.toString();
    }

}
