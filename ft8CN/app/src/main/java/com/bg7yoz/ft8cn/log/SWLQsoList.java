package com.bg7yoz.ft8cn.log;

import com.bg7yoz.ft8cn.Ft8Message;
import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.timer.UtcTimer;

import java.util.ArrayList;

/**
 * 用于计算和处理SWL消息中的QSO记录。
 * QSO的计算方法：把FT8通联的6个阶段分成3部分：
 * 1.CQ C1 grid
 * 2.C1 C2 grid
 * ---------第一部分---
 * 3.C2 C1 report
 * 4.C1 C2 r-report
 * --------第二部分----
 * 5.C2 C1 RR73(RRR)
 * 6.C1 C2 73
 * --------第三部分----
 * <p>
 * 一个基本的QSO，必须有自己的结束点（第三部分），双方的信号报告（在第二部分判断），网格报告可有可无（第一部分）
 * 以RR73、RRR、73为检查点，符合以上第一、二部分
 * swlQsoList是个双key的HashMap,用于防止重复记录QSO。
 * C1与C2顺序不同，代表不同的呼叫方。体现在station_callsign和call字段上
 *
 * @author BG7YOZ
 * @date 2023-03-07
 */
public class SWLQsoList {
    private static final String TAG = "SWLQsoList";
    //通联成功的列表，防止重复，两个KEY顺序分别是：station_callsign和call，Boolean=true,已经QSO
    private final HashTable qsoList =new HashTable();

    public SWLQsoList() {
    }

    /**
     * 检查有没有QSO消息
     *
     * @param newMessages   新的FT8消息
     * @param allMessages   全部的FT8消息
     * @param onFoundSwlQso 当有发现的回调
     */
    public void findSwlQso(ArrayList<Ft8Message> newMessages, ArrayList<Ft8Message> allMessages
            , OnFoundSwlQso onFoundSwlQso) {
        for (int i = 0; i < newMessages.size(); i++) {
            Ft8Message msg = newMessages.get(i);
            if (msg.inMyCall()) continue;//对包含我自己的消息不处理

            if (GeneralVariables.checkFun4_5(msg.extraInfo)//结束标识RRR、RR73、73
                    && !qsoList.contains(msg.callsignFrom, msg.callsignTo)) {//没有QSO记录

                QSLRecord qslRecord = new QSLRecord(msg);

                if (checkPart2(allMessages, qslRecord)) {//找双方的信号报告，一个基本的QSO，必须有双方的信号报告

                    checkPart1(allMessages, qslRecord);//找双方的网格报告，顺便更新time_on的时间

                    if (onFoundSwlQso != null) {//触发回调，用于记录到数据库
                        qsoList.put(msg.callsignFrom, msg.callsignTo, true);//把QSO记录保存下来
                        onFoundSwlQso.doFound(qslRecord);//触发找到QSO的动作
                    }
                }
            }
        }
    }

    /**
     * 查第2部分是否存在，顺便把信号报告保存到QSLRecord中
     *
     * @param allMessages 消息列表
     * @param record      QSLRecord
     * @return 返回值 没有发现：0，存在：2
     */
    private boolean checkPart2(ArrayList<Ft8Message> allMessages, QSLRecord record) {
        boolean foundFromReport = false;
        boolean foundToReport = false;
        long time_on = System.currentTimeMillis();//先把当前的时间作为最早时间
        for (int i = allMessages.size() - 1; i >= 0; i--) {
            Ft8Message msg = allMessages.get(i);
            if (msg.callsignFrom.equals(record.getMyCallsign())
                    && msg.callsignTo.equals(record.getToCallsign())
                    && !foundFromReport) {//callsignFrom发出的信号报告
                int report = GeneralVariables.checkFun2_3(msg.extraInfo);

                if (time_on > msg.utcTime) time_on = msg.utcTime;//取最早的时间
                if (report != -100) {
                    record.setSendReport(report);
                    foundFromReport = true;
                }
            }

            if (msg.callsignFrom.equals(record.getToCallsign())
                    && msg.callsignTo.equals(record.getMyCallsign())
                    && !foundToReport) {//callsignTo发出的信号报告
                int report = GeneralVariables.checkFun2_3(msg.extraInfo);
                if (time_on > msg.utcTime) time_on = msg.utcTime;//取最早的时间
                if (report != -100) {
                    record.setReceivedReport(report);
                    foundToReport = true;
                }
            }
            if (foundToReport && foundFromReport) {//如果双方的信号报告都找到了，就退出循环
                record.setQso_date(UtcTimer.getYYYYMMDD(time_on));
                record.setTime_on(UtcTimer.getTimeHHMMSS(time_on));
                break;
            }
        }
        return foundToReport && foundFromReport;//双方的信号报告都有，才算一个QSO
    }

    /**
     * 查第2部分是否存在，顺便把网格报告保存到QSLRecord中
     *
     * @param allMessages 消息列表
     * @param record      QSLRecord
     */
    private void checkPart1(ArrayList<Ft8Message> allMessages, QSLRecord record) {
        boolean foundFromGrid = false;
        boolean foundToGrid = false;
        long time_on = System.currentTimeMillis();//先把当前的时间作为最早时间
        for (int i = allMessages.size() - 1; i >= 0; i--) {
            Ft8Message msg = allMessages.get(i);
            if (!foundFromGrid
                    && msg.callsignFrom.equals(record.getMyCallsign())
                    && (msg.callsignTo.equals(record.getToCallsign()) || msg.checkIsCQ())) {//callsignFrom的网格报告

                if (GeneralVariables.checkFun1_6(msg.extraInfo)) {
                    record.setMyMaidenGrid(msg.extraInfo.trim());
                    foundFromGrid = true;
                }
                if (time_on > msg.utcTime) time_on = msg.utcTime;//取最早的时间
            }

            if (!foundToGrid
                    && msg.callsignFrom.equals(record.getToCallsign())
                    && (msg.callsignTo.equals(record.getMyCallsign())|| msg.checkIsCQ())) {//callsignTo发出的信号报告
                if (GeneralVariables.checkFun1_6(msg.extraInfo)) {
                    record.setToMaidenGrid(msg.extraInfo.trim());
                    foundToGrid = true;
                }
                if (time_on > msg.utcTime) time_on = msg.utcTime;//取最早的时间
            }
            if (foundToGrid && foundFromGrid) {//如果双方的信号报告都找到了，就退出循环
                break;
            }
        }

        if (foundFromGrid || foundToGrid) {//发现网格报告，至少一个方向的
            record.setQso_date(UtcTimer.getYYYYMMDD(time_on));
            record.setTime_on(UtcTimer.getTimeHHMMSS(time_on));
        }
    }

    public interface OnFoundSwlQso {
        void doFound(QSLRecord record);
    }
}
