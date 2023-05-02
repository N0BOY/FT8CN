package com.bg7yoz.ft8cn.html;
/**
 * Http服务的具体内容。数据库访问不需要异步方式。
 * @author BGY70Z
 * @date 2023-03-20
 */

import static com.bg7yoz.ft8cn.html.HtmlContext.HTML_STRING;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.util.Log;

import com.bg7yoz.ft8cn.Ft8Message;
import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.MainViewModel;
import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.connector.CableSerialPort;
import com.bg7yoz.ft8cn.connector.ConnectMode;
import com.bg7yoz.ft8cn.database.ControlMode;
import com.bg7yoz.ft8cn.database.RigNameList;
import com.bg7yoz.ft8cn.log.LogFileImport;
import com.bg7yoz.ft8cn.log.QSLRecord;
import com.bg7yoz.ft8cn.maidenhead.MaidenheadGrid;
import com.bg7yoz.ft8cn.rigs.BaseRigOperation;
import com.bg7yoz.ft8cn.timer.UtcTimer;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import fi.iki.elonen.NanoHTTPD;

@SuppressWarnings("ConstantConditions")
public class LogHttpServer extends NanoHTTPD {
    private final MainViewModel mainViewModel;
    public static int DEFAULT_PORT = 7050;
    private static final String TAG = "LOG HTTP";


    public LogHttpServer(MainViewModel viewModel, int port) {
        super(port);
        this.mainViewModel = viewModel;

    }

    @Override
    public Response serve(IHTTPSession session) {
        String[] uriList = session.getUri().split("/");
        String uri = "";
        String msg;
        Log.i(TAG, "serve uri: " + session.getUri());

        if (uriList.length >= 2) {
            uri = uriList[1];
        }

        if (uri.equalsIgnoreCase("CONFIG")) {//查配置信息
            msg = HTML_STRING(getConfig());
        } else if (uri.equalsIgnoreCase("showQSLCallsigns")) {//显示通联过的呼号，包括最后的时间
            msg = HTML_STRING(showQslCallsigns(session));
        } else if (uri.equalsIgnoreCase("DEBUG")) {//查通联过的呼号
            msg = HTML_STRING(showDebug());
        } else if (uri.equalsIgnoreCase("SHOWHASH")) {//查通呼号的哈希表
            msg = HTML_STRING(showCallsignHash());
        } else if (uri.equalsIgnoreCase("NEWMESSAGE")) {//查本周期通联消息表
            msg = HTML_STRING(getNewMessages());
        } else if (uri.equalsIgnoreCase("MESSAGE")) {//查保存的SWL通联消息表
            return getMessages(session);
        }else if (uri.equalsIgnoreCase("QSOSWLMSG")) {//查SWL QSO通联消息表
            return getSWLQsoMessages(session);
        }else if (uri.equalsIgnoreCase("QSOLogs")) {//查QSO日志
            return getQsoLogs(session);
        } else if (uri.equalsIgnoreCase("CALLSIGNGRID")) {//查呼号与网格的对应关系
            msg = HTML_STRING(showCallGridList());
        } else if (uri.equalsIgnoreCase("GETCALLSIGNQTH")) {
            msg = HTML_STRING(getCallsignQTH(session));
        } else if (uri.equalsIgnoreCase("ALLTABLE")) {//查所有的表
            msg = HTML_STRING(getAllTableName());
        } else if (uri.equalsIgnoreCase("FOLLOWCALLSIGNS")) {//查关注的呼号
            msg = HTML_STRING(getFollowCallsigns());
        } else if (uri.equalsIgnoreCase("DELFOLLOW")) {//删除关注的呼号
            if (uriList.length >= 3) {
                deleteFollowCallSign(uriList[2].replace("_", "/"));
            }
            msg = HTML_STRING(getFollowCallsigns());
        } else if (uri.equalsIgnoreCase("DELQSL")) {
            if (uriList.length >= 3) {
                deleteQSLByMonth(uriList[2].replace("_", "/"));
            }
            msg = HTML_STRING(showQSLTable());
        } else if (uri.equalsIgnoreCase("QSLCALLSIGNS")) {//查通联过的呼号
            msg = HTML_STRING(getQSLCallsigns());
        } else if (uri.equalsIgnoreCase("QSLTABLE")) {
            msg = HTML_STRING(showQSLTable());
        } else if (uri.equalsIgnoreCase("IMPORTLOG")) {
            msg = HTML_STRING(showImportLog());
        } else if (uri.equalsIgnoreCase("IMPORTLOGDATA")) {
            msg = HTML_STRING(doImportLogFile(session));
        } else if (uri.equalsIgnoreCase("SHOWALLQSL")) {
            msg = HTML_STRING(showAllQSL());
        } else if (uri.equalsIgnoreCase("SHOWQSL")) {
            msg = HTML_STRING(showQSLByMonth(uriList[2]));
        } else if (uri.equalsIgnoreCase("DELQSLCALLSIGN")) {//删除通联过的呼号
            if (uriList.length >= 3) {
                deleteQSLCallSign(uriList[2].replace("_", "/"));
            }
            msg = HTML_STRING(getQSLCallsigns());
        } else {
            msg = HtmlContext.DEFAULT_HTML();
        }
        //return newFixedLengthResponse(msg);

        try {
            Response response;
            if (uri.equalsIgnoreCase("DOWNALLQSL")) {//下载日志
                msg = downAllQSl();
                response = newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "text/plain", msg);
                response.addHeader("Content-Disposition", "attachment;filename=All_log.adi");
            } else if (uri.equalsIgnoreCase("DOWNQSL")) {
                if (uriList.length >= 3) {
                    msg = downQSLByMonth(uriList[2], true);
                } else {
                    msg = HtmlContext.DEFAULT_HTML();
                }
                response = newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "text/plain", msg);
                response.addHeader("Content-Disposition", String.format("attachment;filename=log%s.adi", uriList[2]));

            } else if (uri.equalsIgnoreCase("DOWNQSLNOQSL")) {
                if (uriList.length >= 3) {
                    msg = downQSLByMonth(uriList[2], false);
                } else {
                    msg = HtmlContext.DEFAULT_HTML();
                }
                response = newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "text/plain", msg);
                response.addHeader("Content-Disposition", String.format("attachment;filename=log%s.adi", uriList[2]));

            } else {
                response = newFixedLengthResponse(msg);
            }
            return response;//
        } catch (Exception exception) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, exception.getMessage());
        }
    }


    @SuppressLint("DefaultLocale")
    private String doImportLogFile(IHTTPSession session) {
        //判断是不是POST日志文件
        if (session.getMethod().equals(Method.POST)
                || session.getMethod().equals(Method.PUT)) {
            Map<String, String> files = new HashMap<>();
            //Map<String, String> header = session.getHeaders();
            try {
                session.parseBody(files);
                String param = files.get("file1");//这个是post或put文件的key
                LogFileImport logFileImport = new LogFileImport(param);

                ArrayList<HashMap<String, String>> recordList = logFileImport.getLogRecords();
                int importCount = 0;
                int recordCount = 0;
                for (HashMap<String, String> record : recordList) {
                    QSLRecord qslRecord = new QSLRecord(record);
                    recordCount++;
                    if (mainViewModel.databaseOpr.doInsertQSLData(qslRecord)) {
                        importCount++;
                    }
                }

                StringBuilder temp = new StringBuilder();
                temp.append(String.format(GeneralVariables.getStringFromResource(R.string.html_import_count) + "<br>"
                        , recordCount, importCount, logFileImport.getErrorCount()));
                if (logFileImport.getErrorCount() > 0) {
                    temp.append("<table>");
                    temp.append(String.format("<tr><th></th><th>%d malformed logs</th></tr>\n", logFileImport.getErrorCount()));
                    for (int key : logFileImport.getErrorLines().keySet()) {
                        temp.append(String.format("<tr><td><pre>%d</pre></td><td><pre >%s</pre></td></tr>\n"
                                , key, logFileImport.getErrorLines().get(key)));
                    }

                    temp.append("</table>");
                }
                return temp.toString();
            } catch (IOException | ResponseException e) {
                e.printStackTrace();
                return String.format(GeneralVariables.getStringFromResource(R.string.html_import_failed)
                        , e.getMessage());
            }
        }
        return GeneralVariables.getStringFromResource(R.string.html_illegal_command);
    }

    /**
     * 获取配置信息
     *
     * @return config表内容
     */
    private String getConfig() {
        Cursor cursor = mainViewModel.databaseOpr.getDb()
                .rawQuery("select KeyName,Value from config", null);
        return HtmlContext.ListTableContext(cursor,true,4,false);
    }

    /**
     * 获取通联过的呼号，包括：呼号、最后时间、频段，波长、网格
     *
     * @return config表内容
     */
    private String showQslCallsigns(IHTTPSession session) {
        String callsign = "";
        //读取查询的参数
        Map<String, String> pars = session.getParms();
        if (pars.get("callsign") != null) {
            callsign = Objects.requireNonNull(pars.get("callsign"));
        }
        String where = String.format("%%%s%%", callsign);

        String html = String.format("<form >%s<input type=text name=callsign value=\"%s\">" +
                        "<input type=submit value=\"%s\"></form><br>\n"
                , GeneralVariables.getStringFromResource(R.string.html_callsign)
                , callsign
                , GeneralVariables.getStringFromResource(R.string.html_message_query));

        Cursor cursor = mainViewModel.databaseOpr.getDb()
                .rawQuery("select q.[call] as callsign ,q.gridsquare,q.band||\"(\"||q.freq||\")\" as band \n" +
                        ",q.qso_date||\"-\"||q.time_on as last_time from QSLTable q \n" +
                        "inner join QSLTable q2 ON q.id =q2.id \n" +
                        "where q.[call] like ?\n" +
                        "group by q.[call] ,q.gridsquare,q.freq ,q.qso_date,q.time_on,q.band\n" +
                        "HAVING q.qso_date||q.time_on =MAX(q2.qso_date||q2.time_on) \n", new String[]{where});
        return html + HtmlContext.ListTableContext(cursor,true,3,false);

    }

    /**
     * 获取全部的表名
     *
     * @return html
     */
    private String getAllTableName() {
        Cursor cursor = mainViewModel.databaseOpr.getDb()
                .rawQuery("select * from sqlite_master where type='table'", null);
        return HtmlContext.ListTableContext(cursor,true,4,true);
    }

    @SuppressLint({"Range", "DefaultLocale"})
    private String getCallsignQTH(IHTTPSession session) {
        String callsign="";
        String grid="";
        //读取查询的参数
        Map<String, String> pars = session.getParms();

        if (pars.get("callsign") != null) {
            callsign = Objects.requireNonNull(pars.get("callsign"));
        }
        if (pars.get("grid") != null) {
            grid = Objects.requireNonNull(pars.get("grid"));
        }
        String whereCallsign = String.format("%%%s%%", callsign.toUpperCase());
        String whereGrid = String.format("%%%s%%", grid.toUpperCase());
        Cursor cursor = mainViewModel.databaseOpr.getDb()
                .rawQuery("select callsign ,grid,updateTime from CallsignQTH where (callsign like ?) and (grid like ?)"
                        , new String[]{whereCallsign,whereGrid});
        StringBuilder result = new StringBuilder();
        HtmlContext.tableBegin(result,true,2,false).append("\n");

        result.append(String.format("<form >%s<input type=text name=callsign value=\"%s\"><br>\n" +
                "%s<input type=text name=grid value=\"%s\">\n"+
                        "<input type=submit value=\"%s\"></form><br><br>\n"
                , GeneralVariables.getStringFromResource(R.string.html_callsign)
                , callsign
                , GeneralVariables.getStringFromResource(R.string.html_qsl_grid)
                , grid
                , GeneralVariables.getStringFromResource(R.string.html_message_query)));
        //写字段名
        HtmlContext.tableRowBegin(result).append("\n");
        HtmlContext.tableCellHeader(result
                ,GeneralVariables.getStringFromResource(R.string.html_callsign)
                ,GeneralVariables.getStringFromResource(R.string.html_qsl_grid)
                ,GeneralVariables.getStringFromResource(R.string.html_distance)
                ,GeneralVariables.getStringFromResource(R.string.html_update_time)
                ).append("\n");

        HtmlContext.tableRowEnd(result).append("\n");

        @SuppressLint("SimpleDateFormat") SimpleDateFormat formatTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        int order = 0;
        while (cursor.moveToNext()) {
            HtmlContext.tableRowBegin(result,true,order % 2 != 0);
            Date date = new Date(cursor.getLong(cursor.getColumnIndex("updateTime")));

            HtmlContext.tableCell(result
                    ,cursor.getString(cursor.getColumnIndex("callsign"))
                    ,cursor.getString(cursor.getColumnIndex("grid"))
                    ,MaidenheadGrid.getDistStr(GeneralVariables.getMyMaidenhead4Grid()
                            , cursor.getString(cursor.getColumnIndex("grid")))
                    ,formatTime.format(date)
                    ).append("\n");
            HtmlContext.tableRowEnd(result).append("\n");
            order++;
        }
        HtmlContext.tableEnd(result).append("<br>\n");
        result.append(String.format("%d", order));
        cursor.close();
        return result.toString();
    }


    /**
     * 获取关注的呼号
     *
     * @return HTML
     */
    private String getFollowCallsigns() {
        Cursor cursor = mainViewModel.databaseOpr.getDb().rawQuery("select * from followCallsigns", null);
        StringBuilder result = new StringBuilder();
        HtmlContext.tableBegin(result,true,3,false).append("\n");

        //写字段名
        HtmlContext.tableRowBegin(result).append("\n");
        for (int i = 0; i < cursor.getColumnCount(); i++) {
            HtmlContext.tableCellHeader(result
                    ,GeneralVariables.getStringFromResource(R.string.html_callsign)
                    ,GeneralVariables.getStringFromResource(R.string.html_operation)).append("\n");
        }
        HtmlContext.tableRowEnd(result).append("\n");
        int order = 0;
        while (cursor.moveToNext()) {
            HtmlContext.tableRowBegin(result,true,order % 2 !=0).append("\n");
            for (int i = 0; i < cursor.getColumnCount(); i++) {
                HtmlContext.tableCell(result
                        ,cursor.getString(i)
                        ,String.format("<a href=/delfollow/%s>%s</a>"
                                ,cursor.getString(i).replace("/", "_")
                                , GeneralVariables.getStringFromResource(R.string.html_delete))
                        ).append("\n");
            }
            HtmlContext.tableRowEnd(result).append("\n");
            order++;
        }
        HtmlContext.tableEnd(result).append("\n");
        cursor.close();
        return result.toString();
    }

    /**
     * 删除关注的呼号
     *
     * @param callsign 关注的呼号
     */
    private void deleteFollowCallSign(String callsign) {
        mainViewModel.databaseOpr.getDb().execSQL("delete from followCallsigns where callsign=?", new String[]{callsign});
    }

    private void deleteQSLByMonth(String month) {
        mainViewModel.databaseOpr.getDb().execSQL("delete from QSLTable where SUBSTR(qso_date,1,6)=? \n"
                , new String[]{month});
    }


    /**
     * 查询通联过的呼号
     *
     * @return HTML
     */
    @SuppressLint("Range")
    private String getQSLCallsigns() {
        Cursor cursor = mainViewModel.databaseOpr.getDb().rawQuery(
                "select * from QslCallsigns order by ID desc", null);
        StringBuilder result = new StringBuilder();
        HtmlContext.tableBegin(result,false,0,true).append("\n");

        //写字段名
        HtmlContext.tableRowBegin(result).append("\n");
        HtmlContext.tableCellHeader(result
                ,GeneralVariables.getStringFromResource(R.string.html_qsl_start_time)
                ,GeneralVariables.getStringFromResource(R.string.html_qsl_end_time)
                ,GeneralVariables.getStringFromResource(R.string.html_callsign)
                ,GeneralVariables.getStringFromResource(R.string.html_qsl_mode)
                ,GeneralVariables.getStringFromResource(R.string.html_qsl_grid)
                ,GeneralVariables.getStringFromResource(R.string.html_qsl_band)
                ,GeneralVariables.getStringFromResource(R.string.html_qsl_freq)
                ,GeneralVariables.getStringFromResource(R.string.html_qsl_manual_confirmation)
                ,GeneralVariables.getStringFromResource(R.string.html_qsl_lotw_confirmation)
                ,GeneralVariables.getStringFromResource(R.string.html_qsl_data_source)
                ,GeneralVariables.getStringFromResource(R.string.html_operation)).append("\n");
        HtmlContext.tableRowEnd(result).append("\n");
        int order = 0;
        while (cursor.moveToNext()) {
            HtmlContext.tableRowBegin(result,true,order % 2 != 0);
            HtmlContext.tableCell(result
                    ,cursor.getString(cursor.getColumnIndex("startTime"))
                    ,cursor.getString(cursor.getColumnIndex("finishTime"))
                    ,cursor.getString(cursor.getColumnIndex("callsign"))
                    ,cursor.getString(cursor.getColumnIndex("mode"))
                    ,cursor.getString(cursor.getColumnIndex("grid"))
                    ,cursor.getString(cursor.getColumnIndex("band"))
                    ,cursor.getString(cursor.getColumnIndex("band_i"))+"Hz"
                    ,(cursor.getInt(cursor.getColumnIndex("isQSL")) == 1)
                            ? "<font color=green>√</font>" : "<font color=red>×</font>"
                    ,(cursor.getInt(cursor.getColumnIndex("isLotW_QSL")) == 1)
                            ? "<font color=green>√</font>" : "<font color=red>×</font>"
                    ,(cursor.getInt(cursor.getColumnIndex("isLotW_import")) == 1)
                            ? GeneralVariables.getStringFromResource(R.string.html_qsl_import_data_from_external)
                            : GeneralVariables.getStringFromResource(R.string.html_qsl_native_data)
                    ,String.format("<a href=/delQslCallsign/%s>%s</a>"
                            , cursor.getString(cursor.getColumnIndex("ID"))
                            , GeneralVariables.getStringFromResource(R.string.html_delete))).append("\n");
            HtmlContext.tableRowEnd(result).append("\n");
            order++;
        }
        HtmlContext.tableEnd(result).append("\n");
        cursor.close();
        return result.toString();
    }

    private void deleteQSLCallSign(String callsign) {
        mainViewModel.databaseOpr.getDb().execSQL("delete from QslCallsigns where id=?", new String[]{callsign});
    }

    /**
     * 显示呼号与网格的对应关系
     *
     * @return html
     */
    @SuppressLint("DefaultLocale")
    private String showCallGridList() {
        StringBuilder result = new StringBuilder();
        result.append("<script language=\"JavaScript\">\n" +
                "function myrefresh(){\n" +
                "window.location.reload();\n" +
                "}\n" +
                "setTimeout('myrefresh()',5000); //指定5秒刷新一次，5000处可自定义设置，1000为1秒\n" +
                "</script>");
        result.append(String.format(GeneralVariables.getStringFromResource(R.string.html_callsign_grid_total)
                , GeneralVariables.callsignAndGrids.size()));
        HtmlContext.tableBegin(result,true,1,false);
        HtmlContext.tableRowBegin(result);
        result.append(String.format("<th>%s</th>", GeneralVariables.getStringFromResource(R.string.html_callsign)));
        result.append(String.format("<th>%s</th>", GeneralVariables.getStringFromResource(R.string.html_qsl_grid)));
        HtmlContext.tableRowEnd(result).append("\n");

        result.append(GeneralVariables.getCallsignAndGridToHTML());
        HtmlContext.tableEnd(result).append("<br>\n");

        return result.toString();
    }

    /**
     * 显示调试信息
     *
     * @return html
     */
    @SuppressLint("DefaultLocale")
    private String showDebug() {
        StringBuilder result = new StringBuilder();
        result.append("<script language=\"JavaScript\">\n" +
                "function myrefresh(){\n" +
                "window.location.reload();\n" +
                "}\n" +
                "setTimeout('myrefresh()',5000);\n "+////指定5秒刷新一次，5000处可自定义设置，1000为1秒\n" +
                "</script>");

        HtmlContext.tableBegin(result,true,5,false).append("\n");

        HtmlContext.tableRowBegin(result);
        HtmlContext.tableCellHeader(result
                ,GeneralVariables.getStringFromResource(R.string.html_variable)
                ,GeneralVariables.getStringFromResource(R.string.html_value)).append("\n");

        HtmlContext.tableKeyRow(result,false,"UTC"
                ,UtcTimer.getTimeStr(mainViewModel.timerSec.getValue()));

        HtmlContext.tableKeyRow(result,true
                ,GeneralVariables.getStringFromResource(R.string.html_my_callsign)
                ,GeneralVariables.myCallsign);

        HtmlContext.tableKeyRow(result,false
                ,GeneralVariables.getStringFromResource(R.string.html_my_grid)
                ,GeneralVariables.getMyMaidenheadGrid());

        HtmlContext.tableKeyRow(result,true//消息最大缓存条数
                ,GeneralVariables.getStringFromResource(R.string.html_max_message_cache)
                ,String.format("%d", GeneralVariables.MESSAGE_COUNT));

        HtmlContext.tableKeyRow(result,false//音量大小
                ,GeneralVariables.getStringFromResource(R.string.signal_strength)
                ,String.format("%.0f%%\n", GeneralVariables.volumePercent * 100f));


        HtmlContext.tableKeyRow(result,true
                ,GeneralVariables.getStringFromResource(R.string.html_audio_bits)
                ,GeneralVariables.audioOutput32Bit ?
                        GeneralVariables.getStringFromResource(R.string.audio32_bit)
                        : GeneralVariables.getStringFromResource(R.string.audio16_bit));

        HtmlContext.tableKeyRow(result,false
                ,GeneralVariables.getStringFromResource(R.string.html_audio_rate)
                ,String.format("%dHz", GeneralVariables.audioSampleRate));

        HtmlContext.tableKeyRow(result,true
                ,GeneralVariables.getStringFromResource(R.string.html_decodes_in_this_cycle)
                ,String.format("%d", mainViewModel.currentDecodeCount));

        HtmlContext.tableKeyRow(result,false
                ,GeneralVariables.getStringFromResource(R.string.html_total_number_of_decodes)
                ,String.format("%d", mainViewModel.ft8Messages.size()));

        HtmlContext.tableKeyRow(result,true
                ,GeneralVariables.getStringFromResource(R.string.html_in_recording_state)
                ,Boolean.TRUE.equals(mainViewModel.mutableIsRecording.getValue())
                                ? GeneralVariables.getStringFromResource(R.string.html_recording)
                                : GeneralVariables.getStringFromResource(R.string.html_no_recording));

        HtmlContext.tableKeyRow(result,false
                ,GeneralVariables.getStringFromResource(R.string.html_time_consuming_for_this_decoding)
                ,String.format(GeneralVariables.getStringFromResource(R.string.html_milliseconds)
                        , mainViewModel.ft8SignalListener.decodeTimeSec.getValue()));

        HtmlContext.tableKeyRow(result,true
                ,GeneralVariables.getStringFromResource(R.string.html_average_delay_time_of_this_cycle)
                ,String.format(GeneralVariables.getStringFromResource(R.string.html_seconds)
                        , mainViewModel.mutableTimerOffset.getValue()));

        HtmlContext.tableKeyRow(result,false
                ,GeneralVariables.getStringFromResource(R.string.html_sound_frequency)
                ,String.format("%.0fHz", GeneralVariables.getBaseFrequency()));

        HtmlContext.tableKeyRow(result,true
                ,GeneralVariables.getStringFromResource(R.string.html_transmission_delay_time)
                ,String.format(GeneralVariables.getStringFromResource(R.string.html_milliseconds)
                        , GeneralVariables.transmitDelay));

        HtmlContext.tableKeyRow(result,false
                ,GeneralVariables.getStringFromResource(R.string.html_launch_supervision)
                ,String.format(GeneralVariables.getStringFromResource(R.string.html_milliseconds)
                        , GeneralVariables.launchSupervision));

        HtmlContext.tableKeyRow(result,true
                ,GeneralVariables.getStringFromResource(R.string.html_automatic_program_run_time)
                ,String.format(GeneralVariables.getStringFromResource(R.string.html_milliseconds)
                        , GeneralVariables.launchSupervisionCount()));

        HtmlContext.tableKeyRow(result,false
                ,GeneralVariables.getStringFromResource(R.string.html_no_reply_limit)
                ,GeneralVariables.noReplyLimit);

        HtmlContext.tableKeyRow(result,true
                ,GeneralVariables.getStringFromResource(R.string.html_no_reply_count)
                ,GeneralVariables.noReplyCount);

        HtmlContext.tableKeyRow(result,false
                ,GeneralVariables.getStringFromResource(R.string.follow_cq)
                ,String.valueOf(GeneralVariables.autoFollowCQ));

        HtmlContext.tableKeyRow(result,true
                ,GeneralVariables.getStringFromResource(R.string.auto_call_follow)
                ,String.valueOf(GeneralVariables.autoCallFollow));

        HtmlContext.tableKeyRow(result,false
                ,GeneralVariables.getStringFromResource(R.string.html_target_callsign)
                ,(mainViewModel.ft8TransmitSignal.mutableToCallsign.getValue() != null)
                ? mainViewModel.ft8TransmitSignal.mutableToCallsign.getValue().callsign :"");

        HtmlContext.tableKeyRow(result,true
                ,GeneralVariables.getStringFromResource(R.string.html_sequential)
                ,mainViewModel.ft8TransmitSignal.sequential);

        HtmlContext.tableKeyRow(result,false
                ,GeneralVariables.getStringFromResource(R.string.synFrequency)
                ,String.valueOf(GeneralVariables.synFrequency));

        HtmlContext.tableKeyRow(result,true
                ,GeneralVariables.getStringFromResource(R.string.tran_delay)
                ,GeneralVariables.transmitDelay);

        HtmlContext.tableKeyRow(result,false
                ,GeneralVariables.getStringFromResource(R.string.ptt_delay)
                ,GeneralVariables.pttDelay);

        HtmlContext.tableKeyRow(result,true
                ,GeneralVariables.getStringFromResource(R.string.rig_name)
                ,RigNameList.getInstance(
                        GeneralVariables.getMainContext()).getRigNameByIndex(GeneralVariables.modelNo).modelName);

        HtmlContext.tableKeyRow(result,false
                ,GeneralVariables.getStringFromResource(R.string.html_mark_message)
                ,String.format("%s", mainViewModel.markMessage
                        ? GeneralVariables.getStringFromResource(R.string.html_marking_message)
                        : GeneralVariables.getStringFromResource(R.string.html_do_not_mark_message)));

        HtmlContext.tableKeyRow(result,true
                ,GeneralVariables.getStringFromResource(R.string.html_operation_mode)
                ,ControlMode.getControlModeStr(GeneralVariables.controlMode));

        HtmlContext.tableKeyRow(result,false
                ,GeneralVariables.getStringFromResource(R.string.html_civ_address)
                ,String.format("0x%2X", GeneralVariables.civAddress));

        HtmlContext.tableKeyRow(result,true
                ,GeneralVariables.getStringFromResource(R.string.html_baud_rate)
                ,GeneralVariables.baudRate);

        result.append("<tr>");
        result.append("<td class=\"default\" >");
        result.append(GeneralVariables.getStringFromResource(R.string.html_available_serial_ports));
        result.append("</td>\n");
        result.append("<td class=\"default\" >");
        if (mainViewModel.mutableSerialPorts != null) {
            if (mainViewModel.mutableSerialPorts.getValue().size() == 0) {
                result.append("-");
            }
        }
        for (CableSerialPort.SerialPort serialPort : Objects.requireNonNull(mainViewModel.mutableSerialPorts.getValue())) {
            result.append(serialPort.information()).append("<br>\n");
        }
        result.append("</td>");
        result.append("</tr>\n");


        HtmlContext.tableKeyRow(result,true
                ,GeneralVariables.getStringFromResource(R.string.html_instruction_set)
                ,(mainViewModel.baseRig != null) ? mainViewModel.baseRig.getName() : "-");

        HtmlContext.tableKeyRow(result,false
                ,GeneralVariables.getStringFromResource(R.string.html_connect_mode)
                ,(GeneralVariables.controlMode == ControlMode.VOX) ? "-"
                        :ConnectMode.getModeStr(GeneralVariables.connectMode));

        HtmlContext.tableKeyRow(result,true
                ,GeneralVariables.getStringFromResource(R.string.html_baud_rate)
                ,GeneralVariables.baudRate);

        HtmlContext.tableKeyRow(result,false
                ,GeneralVariables.getStringFromResource(R.string.html_carrier_frequency_band)
                ,GeneralVariables.getBandString());

        HtmlContext.tableKeyRow(result,true
                ,GeneralVariables.getStringFromResource(R.string.html_radio_frequency)
                ,(mainViewModel.baseRig != null)
                        ? BaseRigOperation.getFrequencyStr(mainViewModel.baseRig.getFreq()) : "-");

        HtmlContext.tableKeyRow(result,false
                ,GeneralVariables.getStringFromResource(R.string.html_flex_max_rf_power)
                ,String.format("%d W", GeneralVariables.flexMaxRfPower));

        HtmlContext.tableKeyRow(result,true
                ,GeneralVariables.getStringFromResource(R.string.html_atu_tune_power)
                ,String.format("%d W", GeneralVariables.flexMaxTunePower));

        HtmlContext.tableKeyRow(result,false
                ,GeneralVariables.getStringFromResource(R.string.be_excluded_callsigns)
                ,GeneralVariables.getExcludeCallsigns());

        HtmlContext.tableKeyRow(result,true
                ,GeneralVariables.getStringFromResource(R.string.config_save_swl)
                ,String.valueOf(GeneralVariables.saveSWLMessage));

        HtmlContext.tableKeyRow(result,false
                ,GeneralVariables.getStringFromResource(R.string.config_save_swl_qso)
                ,String.valueOf(GeneralVariables.saveSWL_QSO));

        HtmlContext.tableEnd(result).append("<br>\n");

        HtmlContext.tableBegin(result,false,0,true).append("\n");
        result.append(String.format("<tr><th>%s</th></tr>\n"
                , String.format(GeneralVariables.getStringFromResource(R.string.html_successful_callsign)
                        , GeneralVariables.getBandString())));

        result.append("<tr><td class=\"default\" >");
        for (int i = 0; i < GeneralVariables.QSL_Callsign_list.size(); i++) {
            result.append(GeneralVariables.QSL_Callsign_list.get(i));
            result.append(",&nbsp;");
            if (((i + 1) % 10) == 0) {
                result.append("</td></tr><tr><td class=\"default\" >\n");
            }
        }
        result.append("</td></tr>\n");
        HtmlContext.tableEnd(result).append("<br>\n");

        HtmlContext.tableBegin(result,false,0,true).append("\n");
        result.append(String.format("<tr><th>%s</th></tr>\n"
                , GeneralVariables.getStringFromResource(R.string.html_tracking_callsign)));

        result.append("<tr><td class=\"default\" >");
        for (int i = 0; i < GeneralVariables.followCallsign.size(); i++) {
            result.append(GeneralVariables.followCallsign.get(i));
            result.append(",&nbsp;");
            if (((i + 1) % 10) == 0) {
                result.append("</td></tr><tr><td class=\"default\" >\n");
            }
        }
        result.append("</td></tr>\n");
        HtmlContext.tableEnd(result).append("\n");

        HtmlContext.tableBegin(result,false,0,true).append("\n");
        result.append(String.format("<tr><th>%s</th></tr>\n"
                , GeneralVariables.getStringFromResource(R.string.html_tracking_qso_information)));

        result.append("<tr><td class=\"default\" >");
        result.append(GeneralVariables.qslRecordList.toHTML());
        result.append("</td></tr>\n");
        HtmlContext.tableEnd(result).append("\n");

        return result.toString();
    }

    /**
     * 显示呼号的HASH
     *
     * @return html
     */
    private String showCallsignHash() {
        StringBuilder result = new StringBuilder();
        result.append("<script language=\"JavaScript\">\n" +
                "function myrefresh(){\n" +
                "window.location.reload();\n" +
                "}\n" +
                "setTimeout('myrefresh()',5000); //指定5秒刷新一次，5000处可自定义设置，1000为1秒\n" +
                "</script>");
        HtmlContext.tableBegin(result,true,3,false).append("\n");
        HtmlContext.tableRowBegin(result);
        //表头
        HtmlContext.tableCellHeader(result,GeneralVariables.getStringFromResource(R.string.html_callsign)
                ,GeneralVariables.getStringFromResource(R.string.html_hash_value));

        HtmlContext.tableRowEnd(result).append("\n");


        int order = 0;
        for (Map.Entry<Long, String> entry : Ft8Message.hashList.entrySet()) {
            HtmlContext.tableRowBegin(result,false,(order / 3) % 2 != 0);

            HtmlContext.tableCell(result,entry.getValue());
            HtmlContext.tableCell(result,String.format(" 0x%x ", entry.getKey()));
            HtmlContext.tableRowEnd(result).append("\n");

            order++;
        }
        HtmlContext.tableEnd(result).append("\n");

        return result.toString();
    }

    @SuppressLint("Range")
    private Response exportSWLMessage(String exportFile, String callsign, String start_date, String end_date) {
        Response response;
        StringBuilder fileName = new StringBuilder();
        fileName.append("message");
        if (callsign.length() > 0) {
            fileName.append("_");
            fileName.append(callsign.replace("/", "_")
                    .replace("\\", "_")
                    .replace(":", "_")
                    .replace("?", "_")
                    .replace("*", "_")
                    .replace("|", "_")
                    .replace("\"", "_")
                    .replace("'", "_")
                    .replace("<", "_")
                    .replace(".", "_")
                    .replace(">", "_"));
        }
        if (start_date.length()>0){
            fileName.append(String.format("_%s",start_date));
        }
        if (end_date.length()>0){
            fileName.append(String.format("_%s",end_date));
        }
        fileName.append(".").append(exportFile);


        Cursor cursor;
        StringBuilder dateSql = new StringBuilder();
        if (!start_date.equals("")) {
            dateSql.append(String.format(" AND (SUBSTR(UTC,1,8)>=\"%s\") "
                    , start_date.replace("-", "")));
        }
        if (!end_date.equals("")) {
            dateSql.append(String.format(" AND (SUBSTR(UTC,1,8)<=\"%s\") "
                    , end_date.replace("-", "")));
        }
        String whereStr = String.format("%%%s%%", callsign);
        cursor = mainViewModel.databaseOpr.getDb().rawQuery(
                        "select * from SWLMessages where ((CALL_TO LIKE ?)OR(CALL_FROM LIKE ?)) " +
                                dateSql +
                                " order by ID "
                        , new String[]{whereStr, whereStr});

        StringBuilder result=new StringBuilder();

        String formatStr;
        if (exportFile.equalsIgnoreCase("CSV")){
            formatStr="%s,%.3f,Rx,%s,%d,%.1f,%d,%s\n";
        }else {
            formatStr="%s %12.3f Rx %s %6d %4.1f %4d %s\n";
        }

        while (cursor.moveToNext()) {
            String utcTime = cursor.getString(cursor.getColumnIndex("UTC"));
            int dB = cursor.getInt(cursor.getColumnIndex("SNR"));
            float dt = cursor.getFloat(cursor.getColumnIndex("TIME_SEC"));
            int freq = cursor.getInt(cursor.getColumnIndex("FREQ"));
            String callTo = cursor.getString(cursor.getColumnIndex("CALL_TO"));
            String protocol = cursor.getString(cursor.getColumnIndex("Protocol"));
            String callFrom = cursor.getString(cursor.getColumnIndex("CALL_FROM"));
            String extra = cursor.getString(cursor.getColumnIndex("EXTRAL"));
            long band = cursor.getLong(cursor.getColumnIndex("BAND"));

            result.append(String.format(formatStr
                    ,utcTime,(band/1000f/1000f),protocol,dB,dt,freq,String.format("%s %s %s",callTo,callFrom,extra)));
        }
        cursor.close();



        response = newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "text/plain", result.toString());
        response.addHeader("Content-Disposition"
                , String.format("attachment;filename=%s", fileName));

        return response;
    }

    /**
     * 查SWL消息表
     *
     * @return html
     */
    @SuppressLint({"DefaultLocale", "Range"})
    private Response getMessages(IHTTPSession session) {
        int pageSize = 100;
        String callsign = "";


        StringBuilder result = new StringBuilder();
        String startDate = "";
        String endDate = "";
        String exportFile = "";

        //读取查询的参数
        Map<String, String> pars = session.getParms();
        int pageIndex = 1;
        if (pars.get("page") != null) {
            pageIndex = Integer.parseInt(Objects.requireNonNull(pars.get("page")));
        }
        if (pars.get("pageSize") != null) {
            pageSize = Integer.parseInt(Objects.requireNonNull(pars.get("pageSize")));
        }
        if (pars.get("callsign") != null) {
            callsign = Objects.requireNonNull(pars.get("callsign"));
        }
        if (pars.get("start_date") != null) {
            startDate = Objects.requireNonNull(pars.get("start_date"));
        }
        if (pars.get("end_date") != null) {
            endDate = Objects.requireNonNull(pars.get("end_date"));
        }
        String whereStr = String.format("%%%s%%", callsign);

        if (pars.get("exportFile") != null) {
            exportFile = Objects.requireNonNull(pars.get("exportFile"));
        }

        //导出到文件中
        if (exportFile.equalsIgnoreCase("CSV")
                || exportFile.equalsIgnoreCase("TXT")) {
            return exportSWLMessage(exportFile, callsign, startDate, endDate);
        }

        HtmlContext.tableBegin(result,false,0,true).append("\n");
        HtmlContext.tableRowBegin(result).append("<td>");


        result.append(String.format("<a href=\"message?callsign=%s&start_date=%s&end_date=%s&exportFile=csv\">%s</a>" +
                        " , <a href=\"message?callsign=%s&start_date=%s&end_date=%s&exportFile=txt\">%s</a><br>"
                , callsign, startDate, endDate,GeneralVariables.getStringFromResource(R.string.html_export_csv)
                , callsign, startDate, endDate,GeneralVariables.getStringFromResource(R.string.html_export_text)));

        Cursor cursor;
        StringBuilder dateSql = new StringBuilder();
        if (!startDate.equals("")) {
            dateSql.append(String.format(" AND (SUBSTR(UTC,1,8)>=\"%s\") "
                    , startDate.replace("-", "")));
        }
        if (!endDate.equals("")) {
            dateSql.append(String.format(" AND (SUBSTR(UTC,1,8)<=\"%s\") "
                    , endDate.replace("-", "")));
        }
        //计算总的记录数
        cursor = mainViewModel.databaseOpr.getDb().rawQuery(
                "select count(*) as rc from SWLMessages " +
                        "where ((CALL_TO LIKE ?)OR(CALL_FROM LIKE ?))" + dateSql
                , new String[]{whereStr, whereStr});
        cursor.moveToFirst();
        int pageCount = Math.round(((float) cursor.getInt(cursor.getColumnIndex("rc")) / pageSize) + 0.5f);
        if (pageIndex > pageCount) pageIndex = pageCount;
        cursor.close();

        //查询、每页消息数设定
        result.append(String.format("<form >%s , %s" +
                        "<input type=number name=pageSize style=\"width:80px\" value=%d>" +//页码及页大小
                        "<br>\n%s&nbsp;<input type=text name=callsign value=\"%s\">" +//呼号
                        "&nbsp;&nbsp;<input type=submit value=\"%s\"><br>\n"+
                        "<br>\n%s&nbsp;<input type=date name=\"start_date\" value=\"%s\" onchange=\"javascript:form.submit();\">" +//起始时间
                        "&nbsp;\n%s&nbsp;<input type=date name=end_date value=\"%s\" onchange=\"javascript:form.submit();\"><br>" //结束时间

                , String.format(GeneralVariables.getStringFromResource(R.string.html_message_page_count), pageCount)
                , GeneralVariables.getStringFromResource(R.string.html_message_page_size)
                , pageSize
                , GeneralVariables.getStringFromResource(R.string.html_callsign)
                , callsign
                , GeneralVariables.getStringFromResource(R.string.html_message_query)
                , GeneralVariables.getStringFromResource(R.string.html_start_date_swl_message)
                , startDate
                , GeneralVariables.getStringFromResource(R.string.html_end_date_swl_message)
                , endDate ));


        //定位页，第一页、上一页、下一页，最后一页
        result.append(String.format("<a href=\"message?page=%d&pageSize=%d&callsign=%s&start_date=%s&end_date=%s\">|&lt;</a>" +
                        "&nbsp;&nbsp;<a href=\"message?page=%d&pageSize=%d&callsign=%s&start_date=%s&end_date=%s\">&lt;&lt;</a>" +
                        "<input type=\"number\" name=\"page\" value=%d style=\"width:50px\">" +
                        "<a href=\"message?page=%d&pageSize=%d&callsign=%s&start_date=%s&end_date=%s\">&gt;&gt;</a>" +
                        "&nbsp;&nbsp;<a href=\"message?page=%d&pageSize=%d&callsign=%s&start_date=%s&end_date=%s\">&gt;|</a></form>\n"
                , 1, pageSize, callsign, startDate, endDate
                , pageIndex - 1 == 0 ? 1 : pageIndex - 1, pageSize, callsign, startDate, endDate
                , pageIndex
                , pageIndex == pageCount ? pageCount : pageIndex + 1, pageSize, callsign, startDate, endDate
                , pageCount, pageSize, callsign, startDate, endDate));
        result.append("</td>");
        HtmlContext.tableRowEnd(result);
        HtmlContext.tableEnd(result).append("\n");



        cursor = mainViewModel.databaseOpr.getDb().rawQuery(
                String.format(
                        "select * from SWLMessages where ((CALL_TO LIKE ?)OR(CALL_FROM LIKE ?)) " +
                                dateSql +
                                " order by ID LIMIT(%d),%d "
                        , (pageIndex - 1) * pageSize, pageSize), new String[]{whereStr, whereStr});

        //result.append("<table bgcolor=#a1a1a1 border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">\n");
        HtmlContext.tableBegin(result,false,true).append("\n");
        HtmlContext.tableRowBegin(result).append("\n");
        HtmlContext.tableCellHeader(result,"No.");
        HtmlContext.tableCellHeader(result,GeneralVariables.getStringFromResource(R.string.html_protocol));
        HtmlContext.tableCellHeader(result,"i3.n3","UTC","dB","Δt");
        HtmlContext.tableCellHeader(result,GeneralVariables.getStringFromResource(R.string.html_qsl_freq));
        HtmlContext.tableCellHeader(result,GeneralVariables.getStringFromResource(R.string.message));
        HtmlContext.tableCellHeader(result,GeneralVariables.getStringFromResource(R.string.html_carrier_frequency_band)).append("\n");
        HtmlContext.tableRowEnd(result).append("\n");


        int order = 0;
        while (cursor.moveToNext()) {
            HtmlContext.tableRowBegin(result,true,order % 2 != 0);

            int i3 = cursor.getInt(cursor.getColumnIndex("I3"));
            int n3 = cursor.getInt(cursor.getColumnIndex("N3"));
            String utcTime = cursor.getString(cursor.getColumnIndex("UTC"));
            int dB = cursor.getInt(cursor.getColumnIndex("SNR"));
            float dt = cursor.getFloat(cursor.getColumnIndex("TIME_SEC"));
            int freq = cursor.getInt(cursor.getColumnIndex("FREQ"));
            String protocol = cursor.getString(cursor.getColumnIndex("Protocol"));
            String callTo = cursor.getString(cursor.getColumnIndex("CALL_TO"));
            String callFrom = cursor.getString(cursor.getColumnIndex("CALL_FROM"));
            String extra = cursor.getString(cursor.getColumnIndex("EXTRAL"));
            long band = cursor.getLong(cursor.getColumnIndex("BAND"));

            HtmlContext.tableCell(result,String.format("%d",order + 1 + pageSize * (pageIndex - 1)));
            HtmlContext.tableCell(result,protocol,Ft8Message.getCommandInfoByI3N3(i3, n3));
            HtmlContext.tableCell(result,utcTime);
            HtmlContext.tableCell(result,String.format("%d",dB));
            HtmlContext.tableCell(result, String.format("%.1f",dt));
            HtmlContext.tableCell(result,String.format("%dHz", freq));
            HtmlContext.tableCell(result,String.format("<b><a href=\"message?&pageSize=%d&callsign=%s\">" +
                    "%s</a>&nbsp;&nbsp;" +
                    "<a href=\"message?&pageSize=%d&callsign=%s\">%s</a>&nbsp;&nbsp;%s</b>", pageSize, callTo.replace("<", "")
                            .replace(">", "")
                    , callTo.replace("<", "&lt;")
                            .replace(">", "&gt;")
                    , pageSize, callFrom.replace("<", "")
                            .replace(">", "")
                    , callFrom.replace("<", "&lt;")
                            .replace(">", "&gt;"), extra));
            HtmlContext.tableCell(result,BaseRigOperation.getFrequencyStr(band)).append("\n");
            HtmlContext.tableRowEnd(result).append("\n");

            order++;
        }
        cursor.close();
        HtmlContext.tableEnd(result).append("<br>\n");
        //result.append("</table><br>");


        return newFixedLengthResponse(HtmlContext.HTML_STRING(result.toString()));
        //return result.toString();

    }


    /**
     * 把swo的QSO日志导出到文件
     * @param exportFile 文件名
     * @param callsign 呼号
     * @param start_date 起始日期
     * @param end_date 结束日期
     * @return 数据
     */
    @SuppressLint("Range")
    private Response exportSWLQSOMessage(String exportFile, String callsign, String start_date, String end_date) {
        Response response;
        StringBuilder fileName = new StringBuilder();
        fileName.append("swl_qso");
        if (callsign.length() > 0) {
            fileName.append("_");
            fileName.append(callsign.replace("/", "_")
                    .replace("\\", "_")
                    .replace(":", "_")
                    .replace("?", "_")
                    .replace("*", "_")
                    .replace("|", "_")
                    .replace("\"", "_")
                    .replace("'", "_")
                    .replace("<", "_")
                    .replace(".", "_")
                    .replace(">", "_"));
        }
        if (start_date.length()>0){
            fileName.append(String.format("_%s",start_date));
        }
        if (end_date.length()>0){
            fileName.append(String.format("_%s",end_date));
        }
        fileName.append(".").append(exportFile);


        Cursor cursor;
        StringBuilder dateSql = new StringBuilder();
        if (!start_date.equals("")) {
            dateSql.append(String.format(" AND (SUBSTR(qso_date_off,1,8)>=\"%s\") "
                    , start_date.replace("-", "")));
        }
        if (!end_date.equals("")) {
            dateSql.append(String.format(" AND (SUBSTR(qso_date_off,1,8)<=\"%s\") "
                    , end_date.replace("-", "")));
        }
        String whereStr = String.format("%%%s%%", callsign);

        cursor = mainViewModel.databaseOpr.getDb().rawQuery(
                String.format(
                        "select * from SWLQSOTable where (([call] LIKE ?)OR(station_callsign LIKE ?)) " +
                                dateSql +
                                " order by qso_date desc,time_on desc "),new String[]{whereStr, whereStr});


        response = newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "text/plain"
                , downQSLTable(cursor,true));
        response.addHeader("Content-Disposition"
                , String.format("attachment;filename=%s", fileName));

        return response;
    }


    /**
     * 查询SWL日志
     * @param session 会话
     * @return html
     */
    @SuppressLint({"DefaultLocale", "Range"})
    private Response getSWLQsoMessages(IHTTPSession session) {
        int pageSize = 100;
        String callsign = "";


        StringBuilder result = new StringBuilder();
        String startDate = "";
        String endDate = "";
        String exportFile = "";

        //读取查询的参数
        Map<String, String> pars = session.getParms();
        int pageIndex = 1;
        if (pars.get("page") != null) {
            pageIndex = Integer.parseInt(Objects.requireNonNull(pars.get("page")));
        }
        if (pars.get("pageSize") != null) {
            pageSize = Integer.parseInt(Objects.requireNonNull(pars.get("pageSize")));
        }
        if (pars.get("callsign") != null) {
            callsign = Objects.requireNonNull(pars.get("callsign"));
        }
        if (pars.get("start_date") != null) {
            startDate = Objects.requireNonNull(pars.get("start_date"));
        }
        if (pars.get("end_date") != null) {
            endDate = Objects.requireNonNull(pars.get("end_date"));
        }
        String whereStr = String.format("%%%s%%", callsign);

        if (pars.get("exportFile") != null) {
            exportFile = Objects.requireNonNull(pars.get("exportFile"));
        }

        //导出到文件中
        if (exportFile.equalsIgnoreCase("ADI")) {
            return exportSWLQSOMessage(exportFile, callsign, startDate, endDate);
        }

        HtmlContext.tableBegin(result,false,0,true).append("\n");
        HtmlContext.tableRowBegin(result).append("\n");
        result.append("<td>");

        result.append(String.format("<a href=\"QSOSWLMSG?callsign=%s&start_date=%s&end_date=%s&exportFile=adi\">%s</a>"
                , callsign, startDate, endDate,GeneralVariables.getStringFromResource(R.string.html_export_adi)));

        Cursor cursor;
        StringBuilder dateSql = new StringBuilder();
        if (!startDate.equals("")) {
            dateSql.append(String.format(" AND (SUBSTR(qso_date_off,1,8)>=\"%s\") "
                    , startDate.replace("-", "")));
        }
        if (!endDate.equals("")) {
            dateSql.append(String.format(" AND (SUBSTR(qso_date_off,1,8)<=\"%s\") "
                    , endDate.replace("-", "")));
        }
        //计算总的记录数
        cursor = mainViewModel.databaseOpr.getDb().rawQuery(
                "select count(*) as rc from SWLQSOTable " +
                        "where (([call] LIKE ?)OR(station_callsign LIKE ?))" + dateSql
                , new String[]{whereStr, whereStr});
        cursor.moveToFirst();
        int pageCount = Math.round(((float) cursor.getInt(cursor.getColumnIndex("rc")) / pageSize) + 0.5f);
        if (pageIndex > pageCount) pageIndex = pageCount;
        cursor.close();

        //查询、每页消息数设定
        result.append(String.format("<form >%s , %s" +
                        "<input type=number name=pageSize style=\"width:80px\" value=%d>" +//页码及页大小
                        "<br>\n%s&nbsp;<input type=text name=callsign value=\"%s\">" +//呼号
                        "&nbsp;&nbsp;<input type=submit value=\"%s\"><br>\n"+
                        "<br>\n%s&nbsp;<input type=date name=\"start_date\" value=\"%s\" onchange=\"javascript:form.submit();\">" +//起始时间
                        "&nbsp;\n%s&nbsp;<input type=date name=end_date value=\"%s\" onchange=\"javascript:form.submit();\"><br>" //结束时间

                , String.format(GeneralVariables.getStringFromResource(R.string.html_message_page_count), pageCount)
                , GeneralVariables.getStringFromResource(R.string.html_message_page_size)
                , pageSize
                , GeneralVariables.getStringFromResource(R.string.html_callsign)
                , callsign
                , GeneralVariables.getStringFromResource(R.string.html_message_query)
                , GeneralVariables.getStringFromResource(R.string.html_start_date_swl_message)
                , startDate
                , GeneralVariables.getStringFromResource(R.string.html_end_date_swl_message)
                , endDate     ));


        //定位页，第一页、上一页、下一页，最后一页
        result.append(String.format("<a href=\"QSOSWLMSG?page=%d&pageSize=%d&callsign=%s&start_date=%s&end_date=%s\">|&lt;</a>" +
                        "&nbsp;&nbsp;<a href=\"QSOSWLMSG?page=%d&pageSize=%d&callsign=%s&start_date=%s&end_date=%s\">&lt;&lt;</a>" +
                        "<input type=\"number\" name=\"page\" value=%d style=\"width:50px\">" +
                        "<a href=\"QSOSWLMSG?page=%d&pageSize=%d&callsign=%s&start_date=%s&end_date=%s\">&gt;&gt;</a>" +
                        "&nbsp;&nbsp;<a href=\"QSOSWLMSG?page=%d&pageSize=%d&callsign=%s&start_date=%s&end_date=%s\">&gt;|</a></form>\n"

                , 1, pageSize, callsign, startDate, endDate
                , pageIndex - 1 == 0 ? 1 : pageIndex - 1, pageSize, callsign, startDate, endDate
                , pageIndex
                , pageIndex == pageCount ? pageCount : pageIndex + 1, pageSize, callsign, startDate, endDate
                , pageCount, pageSize, callsign, startDate, endDate));

        result.append("</td>");
        HtmlContext.tableRowEnd(result);
        HtmlContext.tableEnd(result).append("\n");

        cursor = mainViewModel.databaseOpr.getDb().rawQuery(
                String.format(
                        "select * from SWLQSOTable where (([call] LIKE ?)OR(station_callsign LIKE ?)) " +
                                dateSql +
                                " order by qso_date desc,time_on desc LIMIT(%d),%d "
                        , (pageIndex - 1) * pageSize, pageSize), new String[]{whereStr, whereStr});

        HtmlContext.tableBegin(result,false,true).append("\n");

        HtmlContext.tableRowBegin(result);
        HtmlContext.tableCellHeader(result,"No."
                        ,GeneralVariables.getStringFromResource(R.string.html_callsign)//"call"
                        ,GeneralVariables.getStringFromResource(R.string.html_qsl_grid)//"gridsquare"
                        ,GeneralVariables.getStringFromResource(R.string.html_qsl_mode)//"mode"
                        ,GeneralVariables.getStringFromResource(R.string.html_rst_sent)//"rst_sent"
                        ,GeneralVariables.getStringFromResource(R.string.html_rst_rcvd)//"rst_rcvd"
                        ,GeneralVariables.getStringFromResource(R.string.html_qsl_start_day)//"qso date"
                        ,GeneralVariables.getStringFromResource(R.string.html_qsl_start_time)//"time_on"
                        ,GeneralVariables.getStringFromResource(R.string.html_qsl_end_date)//qso date off
                        ,GeneralVariables.getStringFromResource(R.string.html_qsl_end_time)//"time_off"
                        ,GeneralVariables.getStringFromResource(R.string.html_qsl_band)//"band"
                        ,GeneralVariables.getStringFromResource(R.string.html_qsl_freq)//"freq"
                        ,GeneralVariables.getStringFromResource(R.string.html_callsign)//"station_callsign"
                        ,GeneralVariables.getStringFromResource(R.string.html_qsl_grid)//"my_gridsquare"
                        ,GeneralVariables.getStringFromResource(R.string.html_comment))//"comment")
                .append("\n");
        HtmlContext.tableRowEnd(result).append("\n");
        int order = 0;
        while (cursor.moveToNext()) {
            HtmlContext.tableRowBegin(result,true,order % 2 != 0).append("\n");

            String call = cursor.getString(cursor.getColumnIndex("call"));
            String gridsquare = cursor.getString(cursor.getColumnIndex("gridsquare"));
            String mode = cursor.getString(cursor.getColumnIndex("mode"));
            String rst_sent = cursor.getString(cursor.getColumnIndex("rst_sent"));
            String rst_rcvd = cursor.getString(cursor.getColumnIndex("rst_rcvd"));
            String qso_date = cursor.getString(cursor.getColumnIndex("qso_date"));
            String time_on = cursor.getString(cursor.getColumnIndex("time_on"));
            String qso_date_off = cursor.getString(cursor.getColumnIndex("qso_date_off"));
            String time_off = cursor.getString(cursor.getColumnIndex("time_off"));
            String band = cursor.getString(cursor.getColumnIndex("band"));
            String freq = cursor.getString(cursor.getColumnIndex("freq"));
            String station_callsign = cursor.getString(cursor.getColumnIndex("station_callsign"));
            String my_gridsquare = cursor.getString(cursor.getColumnIndex("my_gridsquare"));
            String comment = cursor.getString(cursor.getColumnIndex("comment"));


            //生成数据表的一行
            HtmlContext.tableCell(result,String.format("%d",order + 1 + pageSize * (pageIndex - 1)));
            HtmlContext.tableCell(result,String.format("<a href=\"QSOSWLMSG?&pageSize=%d&callsign=%s\">%s</a>"
                    ,pageSize ,call.replace("<", "")
                            .replace(">", "")
                    , call.replace("<", "&lt;")
                            .replace(">", "&gt;")));
            HtmlContext.tableCell(result,gridsquare==null?"":gridsquare);
            HtmlContext.tableCell(result,mode,rst_sent,rst_rcvd,qso_date,time_on,qso_date_off,time_off);
            HtmlContext.tableCell(result,band,freq);
            HtmlContext.tableCell(result,String.format("<a href=\"QSOSWLMSG?&pageSize=%d&callsign=%s\">%s</a>"
                    ,pageSize
                    ,station_callsign.replace("<", "")
                            .replace(">", "")
                    , station_callsign.replace("<", "&lt;")
                            .replace(">", "&gt;")));
            HtmlContext.tableCell(result,my_gridsquare==null?"":my_gridsquare
                    ,comment).append("\n");

            HtmlContext.tableRowEnd(result).append("\n");
            order++;
        }
        cursor.close();
        HtmlContext.tableEnd(result).append("<br>\n");

        return newFixedLengthResponse(HtmlContext.HTML_STRING(result.toString()));
    }


    /**
     * 把QSO日志导出到文件
     * @param exportFile 文件名
     * @param callsign 呼号
     * @param start_date 起始日期
     * @param end_date 结束日期
     * @return 数据
     */
    @SuppressLint("Range")
    private Response exportQSOLogs(String exportFile, String callsign, String start_date, String end_date,String extWhere) {
        Response response;
        StringBuilder fileName = new StringBuilder();
        fileName.append("qso_log");
        if (callsign.length() > 0) {
            fileName.append("_");
            fileName.append(callsign.replace("/", "_")
                    .replace("\\", "_")
                    .replace(":", "_")
                    .replace("?", "_")
                    .replace("*", "_")
                    .replace("|", "_")
                    .replace("\"", "_")
                    .replace("'", "_")
                    .replace("<", "_")
                    .replace(".", "_")
                    .replace(">", "_"));
        }
        if (start_date.length()>0){
            fileName.append(String.format("_%s",start_date));
        }
        if (end_date.length()>0){
            fileName.append(String.format("_%s",end_date));
        }
        fileName.append(".").append(exportFile);


        Cursor cursor;
        String whereStr = String.format("%%%s%%", callsign);

        cursor = mainViewModel.databaseOpr.getDb().rawQuery(
                String.format(
                        "select * from QSLTable where (([call] LIKE ?)OR(station_callsign LIKE ?)) " +
                                extWhere +
                                " order by qso_date desc,time_on desc "),new String[]{whereStr, whereStr});


        response = newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "text/plain"
                , downQSLTable(cursor,false));
        response.addHeader("Content-Disposition"
                , String.format("attachment;filename=%s", fileName));

        return response;
    }

    /**
     * 查询QSO日志
     * @param session 会话
     * @return html
     */
    @SuppressLint({"DefaultLocale", "Range"})
    private Response getQsoLogs(IHTTPSession session) {
        int pageSize = 100;
        String callsign = "";


        StringBuilder result = new StringBuilder();
        String startDate = "";
        String endDate = "";
        String exportFile = "";
        String qIsQSL="";
        String qIsImported="";

        //读取查询的参数
        Map<String, String> pars = session.getParms();
        int pageIndex = 1;
        if (pars.get("page") != null) {
            pageIndex = Integer.parseInt(Objects.requireNonNull(pars.get("page")));
        }
        if (pars.get("pageSize") != null) {
            pageSize = Integer.parseInt(Objects.requireNonNull(pars.get("pageSize")));
        }
        if (pars.get("callsign") != null) {
            callsign = Objects.requireNonNull(pars.get("callsign"));
        }
        if (pars.get("start_date") != null) {
            startDate = Objects.requireNonNull(pars.get("start_date"));
        }
        if (pars.get("end_date") != null) {
            endDate = Objects.requireNonNull(pars.get("end_date"));
        }
        String whereStr = String.format("%%%s%%", callsign);

        if (pars.get("exportFile") != null) {
            exportFile = Objects.requireNonNull(pars.get("exportFile"));
        }
        if (pars.get("QSL") != null) {
            qIsQSL = Objects.requireNonNull(pars.get("QSL"));
        }
        if (pars.get("Imported") != null) {
            qIsImported = Objects.requireNonNull(pars.get("Imported"));
        }

        result.append("<form >\n");
        HtmlContext.tableBegin(result,false,0,true).append("\n");
        HtmlContext.tableRowBegin(result).append("<td>");
        result.append(String.format("<a href=\"QSOLogs?callsign=%s&start_date=%s&end_date=%s&QSL=%s&Imported=%s&exportFile=adi\">%s</a>\n"
                , callsign, startDate, endDate, qIsQSL,qIsImported
                , GeneralVariables.getStringFromResource(R.string.html_export_adi)));

        Cursor cursor;
        StringBuilder dateSql = new StringBuilder();
        if (!startDate.equals("")) {
            dateSql.append(String.format(" AND (SUBSTR(qso_date_off,1,8)>=\"%s\") "
                    , startDate.replace("-", "")));
        }
        if (!endDate.equals("")) {
            dateSql.append(String.format(" AND (SUBSTR(qso_date_off,1,8)<=\"%s\") "
                    , endDate.replace("-", "")));
        }
        if (!qIsQSL.equals("")){
            dateSql.append(String.format(" AND ((isQSl = %s) %s (isLotW_QSL = %s))"
                    ,qIsQSL,qIsQSL.equals("1")?"OR":"AND" ,qIsQSL));
        }
        if (!qIsImported.equals("")){
            dateSql.append(String.format(" AND (isLotW_import = %s)",qIsImported));
        }


        //导出到文件中
        if (exportFile.equalsIgnoreCase("ADI")) {
            return exportQSOLogs(exportFile, callsign, startDate,endDate, dateSql.toString());
        }

        //计算总的记录数
        cursor = mainViewModel.databaseOpr.getDb().rawQuery(
                "select count(*) as rc from QSLTable " +
                        "where (([call] LIKE ?)OR(station_callsign LIKE ?))" + dateSql
                , new String[]{whereStr, whereStr});
        cursor.moveToFirst();
        int pageCount = Math.round(((float) cursor.getInt(cursor.getColumnIndex("rc")) / pageSize) + 0.5f);
        if (pageIndex > pageCount) pageIndex = pageCount;
        cursor.close();

        //查询、每页消息数设定
        result.append("</td>");
        HtmlContext.tableRowEnd(result);
        HtmlContext.tableRowBegin(result).append("<td>\n");

        result.append(String.format("%s , %s" +
                        "<input type=number name=pageSize style=\"width:80px\" value=%d>" +//页码及页大小
                        "&nbsp;&nbsp;<input type=submit value=\"%s\"><br>\n"+
                        "<br>\n%s&nbsp;<input type=text name=callsign value=\"%s\">" +//呼号
                        "\n%s&nbsp;<input type=date name=\"start_date\" value=\"%s\" onchange=\"javascript:form.submit();\">" +//起始时间
                        "\n%s&nbsp;<input type=date name=end_date value=\"%s\" onchange=\"javascript:form.submit();\">\n" +//结束时间
                        " </td></tr></table>\n" +
                        " <table bgcolor=#a1a1a1 border=\"0\" cellpadding=\"0\" cellspacing=\"0\">\n" +
                        "<tr><td class=\"default\">\n"+

                        "<fieldset onclick=\"javascript:form.submit();\"> \n" +
                        "    <legend>QSL</legend>"+
                        "<div>\n" +
                        "<input type=\"radio\" name=\"QSL\" value=\"\" %s/>\n" +
                        "<label >"+GeneralVariables.getStringFromResource(R.string.html_qso_all)+"</label>\n" +
                        "<input type=\"radio\" name=\"QSL\" value=\"0\" %s />\n" +
                        "<label >"+GeneralVariables.getStringFromResource(R.string.html_qso_unconfirmed)+"</label>\n" +
                        "<input type=\"radio\" name=\"QSL\" value=\"1\" %s />\n" +
                        "<label >"+GeneralVariables.getStringFromResource(R.string.html_qso_confirmed)+"</label>\n" +
                        "</div>"+
                        "</fieldset>\n"+
                        "</td> <td class=\"default\">\n"+
                        "<fieldset onclick=\"javascript:form.submit();\"> \n" +
                        "    <legend>"+GeneralVariables.getStringFromResource(R.string.html_qso_source)+"</legend>"+
                        "<div>\n" +
                        "      <input type=\"radio\" name=\"Imported\" value=\"\" %s />\n" +
                        "      <label >"+GeneralVariables.getStringFromResource(R.string.html_qso_all)+"</label>\n" +
                        "      <input type=\"radio\" name=\"Imported\" value=\"0\" %s />\n" +
                        "      <label >"+GeneralVariables.getStringFromResource(R.string.html_qso_raw_log)+"</label>\n" +
                        "      <input type=\"radio\" name=\"Imported\" value=\"1\" %s />\n" +
                        "      <label >"+GeneralVariables.getStringFromResource(R.string.html_qso_external_logs)+"</label>\n" +
                        "    </div>"+
                        "</fieldset>"+
                        "</td></tr>  </table>"+
                        "<table bgcolor=#a1a1a1 border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%%\">\n" +
                        "        <tr><td class=\"default\">"


                , String.format(GeneralVariables.getStringFromResource(R.string.html_message_page_count), pageCount)
                , GeneralVariables.getStringFromResource(R.string.html_message_page_size)
                , pageSize
                , GeneralVariables.getStringFromResource(R.string.html_message_query)

                , GeneralVariables.getStringFromResource(R.string.html_callsign)
                , callsign
                , GeneralVariables.getStringFromResource(R.string.html_start_date_swl_message)
                , startDate
                , GeneralVariables.getStringFromResource(R.string.html_end_date_swl_message)
                , endDate

                , qIsQSL.equals("") ? "checked=\"true\"":""
                , qIsQSL.equals("0") ? "checked=\"true\"":""
                , qIsQSL.equals("1") ? "checked=\"true\"":""


                , qIsImported.equals("") ? "checked=\"true\"":""
                , qIsImported.equals("0") ? "checked=\"true\"":""
                , qIsImported.equals("1") ? "checked=\"true\"":""
                ));


        //定位页，第一页、上一页、下一页，最后一页
        result.append(String.format("<a href=\"QSOLogs?page=%d&pageSize=%d&callsign=%s&start_date=%s&end_date=%s&QSL=%s&Imported=%s\">|&lt;</a>" +
                        "&nbsp;&nbsp;<a href=\"QSOLogs?page=%d&pageSize=%d&callsign=%s&start_date=%s&end_date=%s&QSL=%s&Imported=%s\">&lt;&lt;</a>" +
                        "<input type=\"number\" name=\"page\" value=%d style=\"width:50px\">" +
                        "<a href=\"QSOLogs?page=%d&pageSize=%d&callsign=%s&start_date=%s&end_date=%s&QSL=%s&Imported=%s\">&gt;&gt;</a>\n" +
                        "&nbsp;&nbsp;<a href=\"QSOLogs?page=%d&pageSize=%d&callsign=%s&start_date=%s&end_date=%s&QSL=%s&Imported=%s\">&gt;|</a>\n"

                , 1, pageSize, callsign, startDate, endDate,qIsQSL,qIsImported
                , pageIndex - 1 == 0 ? 1 : pageIndex - 1, pageSize, callsign, startDate, endDate,qIsQSL,qIsImported
                , pageIndex
                , pageIndex == pageCount ? pageCount : pageIndex + 1, pageSize, callsign, startDate, endDate,qIsQSL,qIsImported
                , pageCount, pageSize, callsign, startDate, endDate,qIsQSL,qIsImported));
        result.append("</td>");
        HtmlContext.tableRowEnd(result);
        HtmlContext.tableEnd(result);
        result.append("</form>");
        //" </td></tr> </table></form>\n"


        cursor = mainViewModel.databaseOpr.getDb().rawQuery(
                String.format(
                        "select * from QSLTable where (([call] LIKE ?)OR(station_callsign LIKE ?)) " +
                                dateSql +
                                " order by qso_date desc,time_on desc LIMIT(%d),%d "
                        , (pageIndex - 1) * pageSize, pageSize), new String[]{whereStr, whereStr});

        HtmlContext.tableBegin(result,false,true).append("\n");

        //表头
        HtmlContext.tableRowBegin(result).append("\n");
        HtmlContext.tableCellHeader(result,"No.","QSL"
                ,GeneralVariables.getStringFromResource(R.string.html_qso_source)
                ,GeneralVariables.getStringFromResource(R.string.html_callsign)
                ,GeneralVariables.getStringFromResource(R.string.html_qsl_grid)
                ,GeneralVariables.getStringFromResource(R.string.html_qsl_mode)
                ,GeneralVariables.getStringFromResource(R.string.html_rst_sent)
                ,GeneralVariables.getStringFromResource(R.string.html_rst_rcvd)
                ,GeneralVariables.getStringFromResource(R.string.html_qsl_start_day)//"qso date"
                ,GeneralVariables.getStringFromResource(R.string.html_qsl_start_time)//"time_on"
                ,GeneralVariables.getStringFromResource(R.string.html_qsl_end_date)//qso date off
                ,GeneralVariables.getStringFromResource(R.string.html_qsl_end_time)//"time_off"
                ,GeneralVariables.getStringFromResource(R.string.html_qsl_band)
                ,GeneralVariables.getStringFromResource(R.string.html_qsl_freq)
                ,GeneralVariables.getStringFromResource(R.string.html_callsign)//"station_callsign"
                ,GeneralVariables.getStringFromResource(R.string.html_qsl_grid)//"my_gridsquare"
                ,GeneralVariables.getStringFromResource(R.string.html_comment))//"comment")
                .append("\n");
        HtmlContext.tableRowEnd(result).append("\n");

        //表内容
        int order = 0;
        while (cursor.moveToNext()) {
            HtmlContext.tableRowBegin(result,true,order % 2 != 0).append("\n");

            String call = cursor.getString(cursor.getColumnIndex("call"));
            boolean isQSL = cursor.getInt(cursor.getColumnIndex("isQSL"))==1;
            boolean isLotW_Import = cursor.getInt(cursor.getColumnIndex("isLotW_import"))==1;
            boolean isLotW_QSL = cursor.getInt(cursor.getColumnIndex("isLotW_QSL"))==1;
            String gridsquare = cursor.getString(cursor.getColumnIndex("gridsquare"));
            String mode = cursor.getString(cursor.getColumnIndex("mode"));
            String rst_sent = cursor.getString(cursor.getColumnIndex("rst_sent"));
            String rst_rcvd = cursor.getString(cursor.getColumnIndex("rst_rcvd"));
            String qso_date = cursor.getString(cursor.getColumnIndex("qso_date"));
            String time_on = cursor.getString(cursor.getColumnIndex("time_on"));
            String qso_date_off = cursor.getString(cursor.getColumnIndex("qso_date_off"));
            String time_off = cursor.getString(cursor.getColumnIndex("time_off"));
            String band = cursor.getString(cursor.getColumnIndex("band"));
            String freq = cursor.getString(cursor.getColumnIndex("freq"));
            String station_callsign = cursor.getString(cursor.getColumnIndex("station_callsign"));
            String my_gridsquare = cursor.getString(cursor.getColumnIndex("my_gridsquare"));
            String comment = cursor.getString(cursor.getColumnIndex("comment"));


            HtmlContext.tableCell(result,String.format("%d",order + 1 + pageSize * (pageIndex - 1)));
            HtmlContext.tableCell(result,(isQSL ||isLotW_QSL) ? "<font color=green>√</font>" :"<font color=red>✗</font>");
            HtmlContext.tableCell(result,isLotW_Import ?
                    String.format("<font color=red>%s</font>"
                            ,GeneralVariables.getStringFromResource(R.string.html_qso_external))
                    :  String.format("<font color=green>%s</font>"
                    ,GeneralVariables.getStringFromResource(R.string.html_qso_raw)));//是否是导入的
            HtmlContext.tableCell(result,String.format("<a href=\"QSOLogs?&pageSize=%d&callsign=%s\">%s</a>"
                    ,pageSize
                    ,call.replace("<", "")
                            .replace(">", "")
                    , call.replace("<", "&lt;")
                            .replace(">", "&gt;")));
            HtmlContext.tableCell(result,gridsquare==null?"":gridsquare,mode,rst_sent,rst_rcvd
                    ,qso_date,time_on,qso_date_off,time_off ,band,freq);
            HtmlContext.tableCell(result,String.format("<a href=\"QSOLogs?&pageSize=%d&callsign=%s\">%s</a>"
                    ,pageSize
                    ,station_callsign.replace("<", "")
                                    .replace(">", "")
                    ,station_callsign.replace("<", "&lt;")
                                    .replace(">", "&gt;")));
            HtmlContext.tableCell(result,my_gridsquare==null?"":my_gridsquare
                    ,comment).append("\n");

            HtmlContext.tableRowEnd(result).append("\n");

            order++;
        }
        cursor.close();
        HtmlContext.tableEnd(result).append("<br>\n");

        return newFixedLengthResponse(HtmlContext.HTML_STRING(result.toString()));
    }
    /**
     * 获取全部通联日志
     *
     * @return HTML
     */
    private String showAllQSL() {
        Cursor cursor = mainViewModel.databaseOpr.getDb().rawQuery(
                "select * from QSLTable order by ID DESC ", null);
        return HtmlContext.ListTableContext(cursor,true);
    }

    /**
     * 按月获取日志
     *
     * @param month 月份yyyymm
     * @return HTML
     */
    private String showQSLByMonth(String month) {
        Cursor cursor = mainViewModel.databaseOpr.getDb().rawQuery(
                "select * from QSLTable  WHERE SUBSTR(qso_date,1,?)=? \n" +
                        "order by ID DESC ", new String[]{String.valueOf(month.length()), month});
        return HtmlContext.ListTableContext(cursor,true);
    }

    /**
     * 查最新解码的消息
     *
     * @return html
     */
    private String getNewMessages() {
        StringBuilder result = new StringBuilder();
        result.append("<script language=\"JavaScript\">\n" +
                "function myrefresh(){\n" +
                "window.location.reload();\n" +
                "}\n" +
                "setTimeout('myrefresh()',5000); //指定5秒刷新一次，5000处可自定义设置，1000为1秒\n" +
                "</script>");
        HtmlContext.tableBegin(result,false,true).append("\n");

        HtmlContext.tableRowBegin(result);
        HtmlContext.tableCellHeader(result,"UTC","dB","Δt"
                ,GeneralVariables.getStringFromResource(R.string.html_qsl_freq)
                ,GeneralVariables.getStringFromResource(R.string.message)
                ,GeneralVariables.getStringFromResource(R.string.html_carrier_frequency_band));
        HtmlContext.tableRowEnd(result).append("\n");

        int order = 0;
        if (mainViewModel.currentMessages != null) {
            for (Ft8Message message : mainViewModel.currentMessages) {
                HtmlContext.tableRowBegin(result,true,order % 2 !=0)
                        .append("\n").append(message.toHtml());
                HtmlContext.tableRowEnd(result).append("\n");
                order++;
            }
        }
        HtmlContext.tableEnd(result).append("<br>\n");
        return result.toString();
    }

    /**
     * 显示导入FT8CN日志文件的HTML
     *
     * @return HTML
     */
    private String showImportLog() {
        StringBuilder result = new StringBuilder();
        HtmlContext.tableBegin(result,false,0,true).append("\n");
        HtmlContext.tableRowBegin(result,false,true);
        HtmlContext.tableCell(result,String.format("%s<font size=5 color=red>%s</font>%s"
                , GeneralVariables.getStringFromResource(R.string.html_please_select)
                , GeneralVariables.getStringFromResource(R.string.html_adi_format)
                , GeneralVariables.getStringFromResource(R.string.html_file_in_other_format)));

        HtmlContext.tableRowEnd(result).append("\n");
        HtmlContext.tableRowBegin(result).append("\n");

        result.append("<td class=\"default\"><br><form action=\"importLogData\" method=\"post\"\n" +
                "            enctype=\"multipart/form-data\">\n" +
                "            <input type=\"file\" name=\"file1\" id=\"file1\" title=\"select ADI file\" accept=\".adi,.txt\" />\n" +
                "            <input type=\"submit\" value=\"上传\" />\n" +
                "        </form></td>");
        HtmlContext.tableRowEnd(result).append("\n");
        HtmlContext.tableEnd(result).append("\n");
        return result.toString();
    }

    @SuppressLint("Range")
    private String showQSLTable() {

        StringBuilder result = new StringBuilder();
        HtmlContext.tableBegin(result,false,1,true).append("\n");
        HtmlContext.tableRowBegin(result).append("\n");

        HtmlContext.tableCellHeader(result
                , GeneralVariables.getStringFromResource(R.string.html_time)
                , GeneralVariables.getStringFromResource(R.string.html_total)
                , GeneralVariables.getStringFromResource(R.string.html_operation)
                , GeneralVariables.getStringFromResource(R.string.html_operation)
                , GeneralVariables.getStringFromResource(R.string.html_operation)
                ).append("\n");

        HtmlContext.tableRowEnd(result).append("\n");

        Cursor cursor = mainViewModel.databaseOpr.getDb().rawQuery("select count(*) as b from QSLTable"
                , null);
        cursor.moveToFirst();

        result.append(String.format("<tr><td align=center class=\"default\"><a href=\"/showAllQsl\">%s</a></td>"
                , GeneralVariables.getStringFromResource(R.string.html_all_logs)));
        result.append(String.format("<td align=center class=\"default\">%s</td>", cursor.getString(cursor.getColumnIndex("b"))));
        result.append(String.format("<td align=center class=\"default\"><a href=\"/downAllQsl\">%s</a></td>"
                , GeneralVariables.getStringFromResource(R.string.html_download)));
        result.append("<td align=center class=\"default\"></td><td></td></tr>");
        cursor.close();

        cursor = mainViewModel.databaseOpr.getDb().rawQuery("select count(*) as b from QSLTable\n" +
                "WHERE SUBSTR(qso_date,1,8)=?", new String[]{UtcTimer.getYYYYMMDD(UtcTimer.getSystemTime())});
        cursor.moveToFirst();

        HtmlContext.tableRowBegin(result,true,true).append("\n");
        HtmlContext.tableCell(result,String.format("<a href=\"/showQsl/%s\">%s</a>"
                , UtcTimer.getYYYYMMDD(UtcTimer.getSystemTime())
                , GeneralVariables.getStringFromResource(R.string.html_today_log)));
        HtmlContext.tableCell(result,cursor.getString(cursor.getColumnIndex("b")));
        HtmlContext.tableCell(result,String.format("<a href=\"/downQsl/%s\">%s</a>"
                , UtcTimer.getYYYYMMDD(UtcTimer.getSystemTime())
                , GeneralVariables.getStringFromResource(R.string.html_download_all)));
        HtmlContext.tableCell(result,String.format("<a href=\"/downQslNoQSL/%s\">%s</a>"
                , UtcTimer.getYYYYMMDD(UtcTimer.getSystemTime())
                , GeneralVariables.getStringFromResource(R.string.html_download_unconfirmed)));
        HtmlContext.tableCell(result,String.format("<a href=\"/delQsl/%s\">%s</a>"
                        , UtcTimer.getYYYYMMDD(UtcTimer.getSystemTime())
                        , GeneralVariables.getStringFromResource(R.string.html_delete))).append("\n");

        HtmlContext.tableRowEnd(result).append("\n");

        cursor.close();

        int order = 1;
        cursor = mainViewModel.databaseOpr.getDb()
                .rawQuery("select SUBSTR(qso_date,1,6) as a,count(*) as b from QSLTable\n" +
                "group by SUBSTR(qso_date,1,6)", null);
        while (cursor.moveToNext()) {
            HtmlContext.tableRowBegin(result,true,order % 2 == 0);

            HtmlContext.tableCell(result,String.format("<a href=\"/showQsl/%s\">%s</a>"
                    ,cursor.getString(cursor.getColumnIndex("a"))
                    ,cursor.getString(cursor.getColumnIndex("a"))));

            HtmlContext.tableCell(result,cursor.getString(cursor.getColumnIndex("b")));
            HtmlContext.tableCell(result,String.format("<a href=\"/downQsl/%s\">%s</a>"
                    , cursor.getString(cursor.getColumnIndex("a"))
                    , GeneralVariables.getStringFromResource(R.string.html_download_all)));
            HtmlContext.tableCell(result,String.format("<a href=\"/downQslNoQSL/%s\">%s</a>"
                    , cursor.getString(cursor.getColumnIndex("a"))
                    , GeneralVariables.getStringFromResource(R.string.html_download_unconfirmed)));
            HtmlContext.tableCell(result,String.format("<a href=\"/delQsl/%s\">%s</a>"
                    ,cursor.getString(cursor.getColumnIndex("a"))
                            , GeneralVariables.getStringFromResource(R.string.html_delete))).append("\n");

            HtmlContext.tableRowEnd(result).append("\n");
            order++;
        }
        HtmlContext.tableEnd(result).append("\n");
        cursor.close();
        return result.toString();
    }

    private String downQSLByMonth(String month, boolean downall) {
        Cursor cursor;
        if (downall) {
            cursor = mainViewModel.databaseOpr.getDb().rawQuery("select * from QSLTable \n" +
                            "WHERE (SUBSTR(qso_date,1,?)=?)"
                    , new String[]{String.valueOf(month.length()), month});
        } else {
            cursor = mainViewModel.databaseOpr.getDb().rawQuery("select * from QSLTable \n" +
                            "WHERE (SUBSTR(qso_date,1,?)=?)and(isLotW_QSL=0 and isQSL=0)"
                    , new String[]{String.valueOf(month.length()), month});

        }
        return downQSLTable(cursor,false);
    }

    /**
     * 下载全部日志
     *
     * @return String
     */
    private String downAllQSl() {
        Cursor cursor = mainViewModel.databaseOpr.getDb().rawQuery("select * from QSLTable", null);
        return downQSLTable(cursor,false);
    }

    /**
     * 生成QSL记录文本
     *
     * @return 日志内容
     */
    @SuppressLint({"Range", "DefaultLocale"})
    private String downQSLTable(Cursor cursor,boolean isSWL) {
        StringBuilder logStr = new StringBuilder();

        logStr.append("FT8CN ADIF Export<eoh>\n");
        while (cursor.moveToNext()) {
            logStr.append(String.format("<call:%d>%s "
                    , cursor.getString(cursor.getColumnIndex("call")).length()
                    , cursor.getString(cursor.getColumnIndex("call"))));
            if (!isSWL) {
                if (cursor.getInt(cursor.getColumnIndex("isLotW_QSL")) == 1) {
                    logStr.append("<QSL_RCVD:1>Y ");
                } else {
                    logStr.append("<QSL_RCVD:1>N ");
                }
                if (cursor.getInt(cursor.getColumnIndex("isQSL")) == 1) {
                    logStr.append("<QSL_MANUAL:1>Y ");
                } else {
                    logStr.append("<QSL_MANUAL:1>N ");
                }
            }else {
                logStr.append("<swl:1>Y ");
            }
            if (cursor.getString(cursor.getColumnIndex("gridsquare"))!=null) {
                logStr.append(String.format("<gridsquare:%d>%s "
                        , cursor.getString(cursor.getColumnIndex("gridsquare")).length()
                        , cursor.getString(cursor.getColumnIndex("gridsquare"))));
            }

            if (cursor.getString(cursor.getColumnIndex("mode"))!=null) {
                logStr.append(String.format("<mode:%d>%s "
                        , cursor.getString(cursor.getColumnIndex("mode")).length()
                        , cursor.getString(cursor.getColumnIndex("mode"))));
            }

            if (cursor.getString(cursor.getColumnIndex("rst_sent"))!=null) {
                logStr.append(String.format("<rst_sent:%d>%s "
                        , cursor.getString(cursor.getColumnIndex("rst_sent")).length()
                        , cursor.getString(cursor.getColumnIndex("rst_sent"))));
            }

            if (cursor.getString(cursor.getColumnIndex("rst_rcvd"))!=null) {
                logStr.append(String.format("<rst_rcvd:%d>%s "
                        , cursor.getString(cursor.getColumnIndex("rst_rcvd")).length()
                        , cursor.getString(cursor.getColumnIndex("rst_rcvd"))));
            }

            if (cursor.getString(cursor.getColumnIndex("qso_date"))!=null) {
                logStr.append(String.format("<qso_date:%d>%s "
                        , cursor.getString(cursor.getColumnIndex("qso_date")).length()
                        , cursor.getString(cursor.getColumnIndex("qso_date"))));
            }

            if (cursor.getString(cursor.getColumnIndex("time_on"))!=null) {
                logStr.append(String.format("<time_on:%d>%s "
                        , cursor.getString(cursor.getColumnIndex("time_on")).length()
                        , cursor.getString(cursor.getColumnIndex("time_on"))));
            }

            if (cursor.getString(cursor.getColumnIndex("qso_date_off"))!=null) {
                logStr.append(String.format("<qso_date_off:%d>%s "
                        , cursor.getString(cursor.getColumnIndex("qso_date_off")).length()
                        , cursor.getString(cursor.getColumnIndex("qso_date_off"))));
            }

            if (cursor.getString(cursor.getColumnIndex("time_off"))!=null) {
                logStr.append(String.format("<time_off:%d>%s "
                        , cursor.getString(cursor.getColumnIndex("time_off")).length()
                        , cursor.getString(cursor.getColumnIndex("time_off"))));
            }

            if (cursor.getString(cursor.getColumnIndex("band"))!=null) {
                logStr.append(String.format("<band:%d>%s "
                        , cursor.getString(cursor.getColumnIndex("band")).length()
                        , cursor.getString(cursor.getColumnIndex("band"))));
            }

            if (cursor.getString(cursor.getColumnIndex("freq"))!=null) {
                logStr.append(String.format("<freq:%d>%s "
                        , cursor.getString(cursor.getColumnIndex("freq")).length()
                        , cursor.getString(cursor.getColumnIndex("freq"))));
            }

            if (cursor.getString(cursor.getColumnIndex("station_callsign"))!=null) {
                logStr.append(String.format("<station_callsign:%d>%s "
                        , cursor.getString(cursor.getColumnIndex("station_callsign")).length()
                        , cursor.getString(cursor.getColumnIndex("station_callsign"))));
            }

            if (cursor.getString(cursor.getColumnIndex("my_gridsquare"))!=null) {
                logStr.append(String.format("<my_gridsquare:%d>%s "
                        , cursor.getString(cursor.getColumnIndex("my_gridsquare")).length()
                        , cursor.getString(cursor.getColumnIndex("my_gridsquare"))));
            }

            String comment = cursor.getString(cursor.getColumnIndex("comment"));

            //<comment:15>Distance: 99 km <eor>
            //在写库的时候，一定要加" km"
            logStr.append(String.format("<comment:%d>%s <eor>\n"
                    , comment.length()
                    , comment));
        }

        cursor.close();
        return logStr.toString();
    }

}
