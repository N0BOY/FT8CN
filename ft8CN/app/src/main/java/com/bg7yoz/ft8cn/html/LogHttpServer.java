package com.bg7yoz.ft8cn.html;

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
            msg = HtmlContext.HTML_STRING(getConfig());
        } else if (uri.equalsIgnoreCase("showQSLCallsigns")) {//显示通联过的呼号，包括最后的时间
            msg = HtmlContext.HTML_STRING(showQslCallsigns());
        } else if (uri.equalsIgnoreCase("DEBUG")) {//查通联过的呼号
            msg = HtmlContext.HTML_STRING(showDebug());
        } else if (uri.equalsIgnoreCase("SHOWHASH")) {//查通呼号的哈希表
            msg = HtmlContext.HTML_STRING(showCallsignHash());
        } else if (uri.equalsIgnoreCase("NEWMESSAGE")) {//查通消息表
            msg = HtmlContext.HTML_STRING(getNewMessages());
        } else if (uri.equalsIgnoreCase("MESSAGE")) {//查通联消息表
            msg = HtmlContext.HTML_STRING(getMessages());
        } else if (uri.equalsIgnoreCase("CALLSIGNGRID")) {//查所有的表
            msg = HtmlContext.HTML_STRING(showCallGridList());
        } else if (uri.equalsIgnoreCase("GETCALLSIGNQTH")) {
            msg = HtmlContext.HTML_STRING(getCallsignQTH());
        } else if (uri.equalsIgnoreCase("ALLTABLE")) {//查所有的表
            msg = HtmlContext.HTML_STRING(getAllTableName());
        } else if (uri.equalsIgnoreCase("FOLLOWCALLSIGNS")) {//查关注的呼号
            msg = HtmlContext.HTML_STRING(getFollowCallsigns());
        } else if (uri.equalsIgnoreCase("DELFOLLOW")) {//删除关注的呼号
            if (uriList.length >= 3) {
                deleteFollowCallSign(uriList[2].replace("_", "/"));
            }
            msg = HtmlContext.HTML_STRING(getFollowCallsigns());
        } else if (uri.equalsIgnoreCase("DELQSL")) {
            if (uriList.length >= 3) {
                deleteQSLByMonth(uriList[2].replace("_", "/"));
            }
            msg = HtmlContext.HTML_STRING(showQSLTable());
        } else if (uri.equalsIgnoreCase("QSLCALLSIGNS")) {//查通联过的呼号
            msg = HtmlContext.HTML_STRING(getQSLCallsigns());
        } else if (uri.equalsIgnoreCase("QSLTABLE")) {
            msg = HtmlContext.HTML_STRING(showQSLTable());
        } else if (uri.equalsIgnoreCase("IMPORTLOG")) {
            msg = HtmlContext.HTML_STRING(showImportLog());
        } else if (uri.equalsIgnoreCase("IMPORTLOGDATA")) {
            msg = HtmlContext.HTML_STRING(doImportLogFile(session));
        } else if (uri.equalsIgnoreCase("SHOWALLQSL")) {
            msg = HtmlContext.HTML_STRING(showAllQSL());
        } else if (uri.equalsIgnoreCase("SHOWQSL")) {
            msg = HtmlContext.HTML_STRING(showQSLByMonth(uriList[2]));
        } else if (uri.equalsIgnoreCase("DELQSLCALLSIGN")) {//删除通联过的呼号
            if (uriList.length >= 3) {
                deleteQSLCallSign(uriList[2].replace("_", "/"));
            }
            msg = HtmlContext.HTML_STRING(getQSLCallsigns());
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
            //Log.e(TAG, "serve uri: --------->" + session.getUri());
            Map<String, String> files = new HashMap<String, String>();
            Map<String, String> header = session.getHeaders();
            try {
                session.parseBody(files);
                String param = files.get("file1");//这个是post或put文件的key
                LogFileImport logFileImport = new LogFileImport(param);

                ArrayList<HashMap<String, String>> recordList = logFileImport.getLogRecords();
                int importCount = 0;
                int recordCount=0;
                for (HashMap<String, String> record : recordList) {
                    QSLRecord qslRecord = new QSLRecord(record);
                    recordCount++;
                    if (mainViewModel.databaseOpr.doInsertQSLData(qslRecord)) {
                        importCount++;
                    }
                }

                StringBuilder temp=new StringBuilder();
                temp.append(String.format(GeneralVariables.getStringFromResource(R.string.html_import_count) + "<br>"
                        , recordCount, importCount,logFileImport.getErrorCount()));
                if (logFileImport.getErrorCount()>0) {
                    temp.append("<table>");
                    temp.append(String.format("<tr><th></th><th>%d malformed logs</th></tr>\n",logFileImport.getErrorCount()));
                    for (int key : logFileImport.getErrorLines().keySet()) {
                        temp.append(String.format("<tr><td><pre>%d</pre></td><td><pre >%s</pre></td></tr>\n"
                                , key, logFileImport.getErrorLines().get(key)));
                    }

                    temp.append("</table>");
                }
                return temp.toString();
//                return String.format(GeneralVariables.getStringFromResource(R.string.html_import_count) + "<br>"
//                        , recordList.size(), importCount,logFileImport.getErrorCount());
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
                .rawQuery("select * from config", null);
        return HtmlContext.ListTableContext(cursor);
    }

    /**
     * 获取通联过的呼号，包括：呼号、最后时间、频段，波长、网格
     *
     * @return config表内容
     */
    private String showQslCallsigns() {
        Cursor cursor = mainViewModel.databaseOpr.getDb()
                .rawQuery("select q.[call] as callsign ,q.gridsquare,q.band||\"(\"||q.freq||\")\" as band \n" +
                        ",q.qso_date||\"-\"||q.time_on as last_time from QSLTable q \n" +
                        "inner join QSLTable q2 ON q.id =q2.id \n" +
                        "group by q.[call] ,q.gridsquare,q.freq ,q.qso_date,q.time_on,q.band\n" +
                        "HAVING q.qso_date||q.time_on =MAX(q2.qso_date||q2.time_on) \n", null);
        return HtmlContext.ListTableContext(cursor);
    }

    /**
     * 获取全部的表名
     *
     * @return html
     */
    private String getAllTableName() {
        Cursor cursor = mainViewModel.databaseOpr.getDb()
                .rawQuery("select * from sqlite_master where type='table'", null);
        return HtmlContext.ListTableContext(cursor);
    }

    @SuppressLint("Range")
    private String getCallsignQTH() {
        Cursor cursor = mainViewModel.databaseOpr.getDb().rawQuery("select callsign ,grid,updateTime from CallsignQTH", null);
        StringBuilder result = new StringBuilder();
        result.append("<table bgcolor=\"#a1a1a1\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">");

        //写字段名
        result.append("<tr>");


        result.append(String.format("<th>%s</th>", GeneralVariables.getStringFromResource(R.string.html_callsign)));
        result.append(String.format("<th>%s</th>", GeneralVariables.getStringFromResource(R.string.html_qsl_grid)));
        result.append(String.format("<th>%s</th>", GeneralVariables.getStringFromResource(R.string.html_distance)));
        result.append(String.format("<th>%s</th>", GeneralVariables.getStringFromResource(R.string.html_update_time)));
        result.append("</tr>\n");
        @SuppressLint("SimpleDateFormat") SimpleDateFormat formatTime= new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        int order = 0;
        while (cursor.moveToNext()) {
            if (order % 2 == 0) {
                result.append("<tr align=center>");
            } else {
                result.append("<tr align=center class=\"bbb\">");
            }

            result.append("<td class=\"default\" >");
            result.append(String.format("%s</td><td class=\"default\" align=\"center\">%s</td>\n"
                    , cursor.getString(cursor.getColumnIndex("callsign"))
                    , cursor.getString(cursor.getColumnIndex("grid"))));
            result.append(String.format("<td class=\"default\" align=right>%s</td>",
                    MaidenheadGrid.getDistStr(GeneralVariables.myCallsign
                            , cursor.getString(cursor.getColumnIndex("grid")))));

            Date date =new Date(cursor.getLong(cursor.getColumnIndex("updateTime")));

            result.append(String.format("<td class=\"default\" align=center>%s</td>",
                    formatTime.format(date) ));

            result.append("</tr>\n");
            order++;
        }
        result.append("</table>");
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
        result.append("<table bgcolor=\"#a1a1a1\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">");

        //写字段名
        result.append("<tr>");
        for (int i = 0; i < cursor.getColumnCount(); i++) {
            result.append(String.format("<th>%s</th>", GeneralVariables.getStringFromResource(R.string.html_callsign)));
            result.append(String.format("<th>%s</th>", GeneralVariables.getStringFromResource(R.string.html_operation)));
        }
        result.append("</tr>\n");
        int order = 0;
        while (cursor.moveToNext()) {
            if (order % 2 == 0) {
                result.append("<tr align=center>");
            } else {
                result.append("<tr align=center class=\"bbb\">");
            }
            for (int i = 0; i < cursor.getColumnCount(); i++) {
                result.append("<td class=\"default\" >");
                result.append(String.format("%s</td><td class=\"default\" align=\"center\"><a href=/delfollow/%s>%s</a></td>\n"
                        , cursor.getString(i), cursor.getString(i).replace("/", "_")
                        , GeneralVariables.getStringFromResource(R.string.html_delete)));

            }
            result.append("</tr>\n");
            order++;
        }
        result.append("</table>");
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
        result.append("<table bgcolor=\"#a1a1a1\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">\n");

        //写字段名
        result.append("<tr>");
        result.append(String.format("<th>%s</th>", GeneralVariables.getStringFromResource(R.string.html_qsl_start_time)));
        result.append(String.format("<th>%s</th>", GeneralVariables.getStringFromResource(R.string.html_qsl_end_time)));
        result.append(String.format("<th>%s</th>", GeneralVariables.getStringFromResource(R.string.html_callsign)));
        result.append(String.format("<th>%s</th>", GeneralVariables.getStringFromResource(R.string.html_qsl_mode)));
        result.append(String.format("<th>%s</th>", GeneralVariables.getStringFromResource(R.string.html_qsl_grid)));
        result.append(String.format("<th>%s</th>", GeneralVariables.getStringFromResource(R.string.html_qsl_band)));
        result.append(String.format("<th>%s</th>", GeneralVariables.getStringFromResource(R.string.html_qsl_freq)));
        result.append(String.format("<th>%s</th>", GeneralVariables.getStringFromResource(R.string.html_qsl_manual_confirmation)));
        result.append(String.format("<th>%s</th>", GeneralVariables.getStringFromResource(R.string.html_qsl_lotw_confirmation)));
        result.append(String.format("<th>%s</th>", GeneralVariables.getStringFromResource(R.string.html_qsl_data_source)));
        result.append(String.format("<th>%s</th>", GeneralVariables.getStringFromResource(R.string.html_operation)));
        result.append("</tr>\n");
        int order = 0;
        while (cursor.moveToNext()) {
            if (order % 2 == 0) {
                result.append("<tr align=center >");
            } else {
                result.append("<tr align=center class=\"bbb\">");
            }
            result.append("<td  class=\"default\" >");
            result.append(cursor.getString(cursor.getColumnIndex("startTime")));
            result.append("</td>");

            result.append("<td  class=\"default\" >");
            result.append(cursor.getString(cursor.getColumnIndex("finishTime")));
            result.append("</td>");

            result.append("<td  class=\"default\" >");
            result.append(cursor.getString(cursor.getColumnIndex("callsign")));
            result.append("</td>\n");

            result.append("<td   class=\"default\" >");
            result.append(cursor.getString(cursor.getColumnIndex("mode")));
            result.append("</td>");
            result.append("<td class=\"default\" >");
            result.append(cursor.getString(cursor.getColumnIndex("grid")));
            result.append("</td>");
            result.append("<td  class=\"default\" >");
            result.append(cursor.getString(cursor.getColumnIndex("band")));
            result.append("</td>");
            result.append("<td  class=\"default\" >");
            result.append(cursor.getString(cursor.getColumnIndex("band_i")));
            result.append("Hz</td>");
            result.append("<td  class=\"default\" >");
            if (cursor.getInt(cursor.getColumnIndex("isQSL")) == 1) {
                result.append("<font color=green>√</font>");
            } else {
                result.append("<font color=red>×</font>");
            }
            result.append("</td>");
            result.append("<td  class=\"default\" >");
            if (cursor.getInt(cursor.getColumnIndex("isLotW_QSL")) == 1) {
                result.append("<font color=green>√</font>");
            } else {
                result.append("<font color=red>×</font>");
            }
            result.append("</td>");


            result.append("<td  class=\"default\" >");
            if (cursor.getInt(cursor.getColumnIndex("isLotW_import")) == 1) {
                result.append(GeneralVariables.getStringFromResource(R.string.html_qsl_import_data_from_external));
            } else {
                result.append(GeneralVariables.getStringFromResource(R.string.html_qsl_native_data));
            }
            result.append("</td>");

            result.append(String.format("<td class=\"default\" align=\"center\"><a href=/delQslCallsign/%s>%s</a></td>\n"
                    , cursor.getString(cursor.getColumnIndex("ID"))
                    , GeneralVariables.getStringFromResource(R.string.html_delete)));
            result.append("</tr>\n");
            order++;
        }
        result.append("</table>");
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
    private String showCallGridList() {
        StringBuilder result = new StringBuilder();
        result.append("<script language=\"JavaScript\">\n" +
                "function myrefresh(){\n" +
                "window.location.reload();\n" +
                "}\n" +
                "setTimeout('myrefresh()',5000); //指定5秒刷新一次，5000处可自定义设置，1000为1秒\n" +
                "</script>");
        result.append("<table bgcolor=\"#a1a1a1\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">\n");
        result.append("<tr>");
        result.append(String.format("<th>%s</th>", GeneralVariables.getStringFromResource(R.string.html_callsign)));
        result.append(String.format("<th>%s</th>", GeneralVariables.getStringFromResource(R.string.html_qsl_grid)));
        result.append("</tr>\n");
        result.append(GeneralVariables.getCallsignAndGridToHTML());
        result.append("</table><br>\n");
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
                "setTimeout('myrefresh()',5000); //指定5秒刷新一次，5000处可自定义设置，1000为1秒\n" +
                "</script>");
        result.append("<table bgcolor=\"#a1a1a1\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">\n");
        result.append("<tr>");
        result.append(String.format("<th>%s</th>", GeneralVariables.getStringFromResource(R.string.html_variable)));
        result.append(String.format("<th>%s</th>", GeneralVariables.getStringFromResource(R.string.html_value)));
        result.append("</tr>\n");

        result.append("<tr class=\"default\">");
        result.append("<td class=\"default\" >");
        result.append("UTC");
        result.append("</td>\n");
        result.append("<td class=\"default\" >");
        result.append(String.format("%s</td>\n", UtcTimer.getTimeStr(mainViewModel.timerSec.getValue())));
        result.append("</tr>\n");


        result.append("<tr class=\"bbb\">");
        result.append("<td class=\"default\" >");
        result.append(GeneralVariables.getStringFromResource(R.string.html_my_callsign));
        result.append("</td>\n");
        result.append("<td class=\"default\" >");
        result.append(String.format("%s</td>\n", GeneralVariables.myCallsign));
        result.append("</tr>\n");


        result.append("<tr class=\"default\">");
        result.append("<td class=\"default\" >");
        result.append(GeneralVariables.getStringFromResource(R.string.html_my_grid));
        result.append("</td>\n");
        result.append("<td class=\"default\" >");
        result.append(String.format("%s</td>\n", GeneralVariables.getMyMaidenheadGrid()));
        result.append("</tr>\n");


        result.append("<tr class=\"bbb\">");
        result.append("<td class=\"default\" >");
        result.append(GeneralVariables.getStringFromResource(R.string.html_max_message_cache));
        result.append("</td>\n");
        result.append("<td class=\"default\" >");
        result.append(String.format("%d</td>\n", GeneralVariables.MESSAGE_COUNT));//消息最大缓存条数
        result.append("</tr>\n");

        result.append("<tr class=\"default\">");
        result.append("<td class=\"default\" >");
        result.append(GeneralVariables.getStringFromResource(R.string.signal_strength));
        result.append("</td>\n");
        result.append("<td class=\"default\" >");
        result.append(String.format("%.0f%%</td>\n", GeneralVariables.volumePercent * 100f));//音量大小
        result.append("</tr>\n");


        result.append("<tr class=\"bbb\">");
        result.append("<td class=\"default\" >");
        result.append(GeneralVariables.getStringFromResource(R.string.html_decodes_in_this_cycle));
        result.append("</td>\n");
        result.append("<td class=\"default\" >");
        result.append(String.format("%d</td>\n", mainViewModel.currentDecodeCount));
        result.append("</tr>\n");

        result.append("<tr>");
        result.append("<td class=\"default\" >");
        result.append(GeneralVariables.getStringFromResource(R.string.html_total_number_of_decodes));
        result.append("</td>\n");
        result.append("<td class=\"default\" >");
        result.append(String.format("%d</td>\n", mainViewModel.ft8Messages.size()));
        result.append("</tr>\n");

        result.append("<tr class=\"bbb\">");
        result.append("<td class=\"default\" >");
        result.append(GeneralVariables.getStringFromResource(R.string.html_in_recording_state));
        result.append("</td>\n");
        result.append("<td class=\"default\" >");
        result.append(String.format("%s</td>\n",
                Boolean.TRUE.equals(mainViewModel.mutableIsRecording.getValue())
                        ? GeneralVariables.getStringFromResource(R.string.html_recording)
                        : GeneralVariables.getStringFromResource(R.string.html_no_recording)));
        result.append("</tr>\n");

        result.append("<tr>");
        result.append("<td class=\"default\" >");
        result.append(GeneralVariables.getStringFromResource(R.string.html_time_consuming_for_this_decoding));
        result.append("</td>\n");
        result.append("<td class=\"default\" >");
        result.append(String.format(GeneralVariables.getStringFromResource(R.string.html_milliseconds)
                , mainViewModel.ft8SignalListener.decodeTimeSec.getValue()));
        result.append("</td>\n</tr>\n");

        result.append("<tr class=\"bbb\">");
        result.append("<td class=\"default\" >");
        result.append(GeneralVariables.getStringFromResource(R.string.html_average_delay_time_of_this_cycle));
        result.append("</td>\n");
        result.append("<td class=\"default\" >");
        result.append(String.format(GeneralVariables.getStringFromResource(R.string.html_seconds)
                , mainViewModel.mutableTimerOffset.getValue()));
        result.append("</td>\n</tr>\n");

        result.append("<tr >");
        result.append("<td class=\"default\" >");
        result.append(GeneralVariables.getStringFromResource(R.string.html_sound_frequency));
        result.append("</td>\n");
        result.append("<td class=\"default\" >");
        result.append(String.format("%.0fHz", GeneralVariables.getBaseFrequency()));
        result.append("</td>\n</tr>\n");

        result.append("<tr class=\"bbb\">");
        result.append("<td class=\"default\" >");
        result.append(GeneralVariables.getStringFromResource(R.string.html_transmission_delay_time));
        result.append("</td>\n");
        result.append("<td class=\"default\" >");
        result.append(String.format(GeneralVariables.getStringFromResource(R.string.html_milliseconds)
                , GeneralVariables.transmitDelay));
        result.append("</td>\n</tr>\n");

        result.append("<tr>");
        result.append("<td class=\"default\" >");
        result.append(GeneralVariables.getStringFromResource(R.string.html_launch_supervision));
        result.append("</td>\n");
        result.append("<td class=\"default\" >");
        result.append(String.format(GeneralVariables.getStringFromResource(R.string.html_milliseconds)
                , GeneralVariables.launchSupervision));
        result.append("</td>\n</tr>\n");


        result.append("<tr  class=\"bbb\">");
        result.append("<td class=\"default\" >");
        result.append(GeneralVariables.getStringFromResource(R.string.html_automatic_program_run_time));
        result.append("</td>\n");
        result.append("<td class=\"default\" >");
        result.append(String.format(GeneralVariables.getStringFromResource(R.string.html_milliseconds)
                , GeneralVariables.launchSupervisionCount()));
        result.append("</td>\n</tr>\n");

        result.append("<tr>");
        result.append("<td class=\"default\" >");
        result.append(GeneralVariables.getStringFromResource(R.string.html_no_reply_limit));
        result.append("</td>\n");
        result.append("<td class=\"default\" >");
        result.append(GeneralVariables.noReplyLimit);
        result.append("</td>\n</tr>\n");

        result.append("<tr  class=\"bbb\">");
        result.append("<td class=\"default\" >");
        result.append(GeneralVariables.getStringFromResource(R.string.html_no_reply_count));
        result.append("</td>\n");
        result.append("<td class=\"default\" >");
        result.append(GeneralVariables.noReplyCount);
        result.append("</td>\n</tr>\n");


        result.append("<tr>");
        result.append("<td class=\"default\" >");
        result.append(GeneralVariables.getStringFromResource(R.string.follow_cq));
        result.append("</td>\n");
        result.append("<td class=\"default\" >");
        result.append(GeneralVariables.autoFollowCQ);
        result.append("</td>\n</tr>\n");

        result.append("<tr  class=\"bbb\">");
        result.append("<td class=\"default\" >");
        result.append(GeneralVariables.getStringFromResource(R.string.auto_call_follow));
        result.append("</td>\n");
        result.append("<td class=\"default\" >");
        result.append(GeneralVariables.autoCallFollow);
        result.append("</td>\n</tr>\n");


        result.append("<tr>");
        result.append("<td class=\"default\" >");
        result.append(GeneralVariables.getStringFromResource(R.string.html_target_callsign));
        result.append("</td>\n");
        result.append("<td class=\"default\" >");
        if (mainViewModel.ft8TransmitSignal.mutableToCallsign.getValue() != null) {
            result.append(mainViewModel.ft8TransmitSignal.mutableToCallsign.getValue().callsign);
        } else {
            result.append("-");
        }
        result.append("</td>\n</tr>\n");

        result.append("<tr  class=\"bbb\">");
        result.append("<td class=\"default\" >");
        result.append(GeneralVariables.getStringFromResource(R.string.html_sequential));
        result.append("</td>\n");
        result.append("<td class=\"default\" >");
        result.append(mainViewModel.ft8TransmitSignal.sequential);
        result.append("</td>\n</tr>\n");


        result.append("<tr>");
        result.append("<td class=\"default\" >");
        result.append(GeneralVariables.getStringFromResource(R.string.synFrequency));
        result.append("</td>\n");
        result.append("<td class=\"default\" >");
        result.append(GeneralVariables.synFrequency);
        result.append("</td>\n</tr>\n");

        result.append("<tr  class=\"bbb\">");
        result.append("<td class=\"default\" >");
        result.append(GeneralVariables.getStringFromResource(R.string.tran_delay));
        result.append("</td>\n");
        result.append("<td class=\"default\" >");
        result.append(GeneralVariables.transmitDelay);
        result.append("</td>\n</tr>\n");

        result.append("<tr>");
        result.append("<td class=\"default\" >");
        result.append(GeneralVariables.getStringFromResource(R.string.ptt_delay));
        result.append("</td>\n");
        result.append("<td class=\"default\" >");
        result.append(GeneralVariables.pttDelay);
        result.append("</td>\n</tr>\n");

        result.append("<tr  class=\"bbb\">");
        result.append("<td class=\"default\" >");
        result.append(GeneralVariables.getStringFromResource(R.string.rig_name));
        result.append("</td>\n");
        result.append("<td class=\"default\" >");
        result.append(RigNameList.getInstance(
                GeneralVariables.getMainContext()).getRigNameByIndex(GeneralVariables.modelNo).modelName);
        result.append("</td>\n</tr>\n");


        result.append("<tr>");
        result.append("<td class=\"default\" >");
        result.append(GeneralVariables.getStringFromResource(R.string.html_mark_message));
        result.append("</td>\n");
        result.append("<td class=\"default\" >");
        result.append(String.format("%s", mainViewModel.markMessage
                ? GeneralVariables.getStringFromResource(R.string.html_marking_message)
                : GeneralVariables.getStringFromResource(R.string.html_do_not_mark_message)));
        result.append("</td>\n</tr>\n");

        result.append("<tr class=\"bbb\">");
        result.append("<td class=\"default\" >");
        result.append(GeneralVariables.getStringFromResource(R.string.html_operation_mode));
        result.append("</td>\n");
        result.append(String.format("<td class=\"default\" >%s</td>"
                , ControlMode.getControlModeStr(GeneralVariables.controlMode)));
        result.append("</tr>\n");

        result.append("<tr>");
        result.append("<td class=\"default\" >");
        result.append(GeneralVariables.getStringFromResource(R.string.html_civ_address));
        result.append("</td>\n");
        result.append(String.format("<td class=\"default\" >0x%2X</td>", GeneralVariables.civAddress));
        result.append("</tr>\n");

        result.append("<tr  class=\"bbb\">>");
        result.append("<td class=\"default\" >");
        result.append(GeneralVariables.getStringFromResource(R.string.html_baud_rate));
        result.append("</td>\n");
        result.append(String.format("<td class=\"default\" >%d</td>", GeneralVariables.baudRate));
        result.append("</tr>\n");

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
            result.append(serialPort.information() + "<br>\n");
        }
        result.append("</td>");
        result.append("</tr>\n");

        result.append("<tr  class=\"bbb\">>");
        result.append("<td class=\"default\" >");
        result.append(GeneralVariables.getStringFromResource(R.string.html_instruction_set));
        result.append("</td>\n");

        result.append("<td class=\"default\" >");
        if (mainViewModel.baseRig != null) {
            result.append(mainViewModel.baseRig.getName());
        } else {
            result.append("-");
        }
        result.append("</td>");
        result.append("</tr>\n");

        result.append("<tr>");
        result.append("<td class=\"default\" >");
        result.append(GeneralVariables.getStringFromResource(R.string.html_connect_mode));
        result.append("</td>\n");
        if (GeneralVariables.controlMode == ControlMode.VOX) {
            result.append("<td class=\"default\" >-</td>");
        } else {
            result.append(String.format("<td class=\"default\" >%s</td>"
                    , ConnectMode.getModeStr(GeneralVariables.connectMode)));
        }
        result.append("</tr>\n");


        result.append("<tr>");
        result.append("<td class=\"default\" >");
        result.append(GeneralVariables.getStringFromResource(R.string.html_baud_rate));
        result.append("</td>\n");
        result.append(String.format("<td class=\"default\" >%d</td>", GeneralVariables.baudRate));
        result.append("</tr>\n");


        result.append("<tr class=\"bbb\">>");
        result.append("<td class=\"default\" >");
        result.append(GeneralVariables.getStringFromResource(R.string.html_carrier_frequency_band));
        result.append("</td>\n");
        result.append("<td class=\"default\" >");
        result.append(GeneralVariables.getBandString());
        result.append("</td>");
        result.append("</tr>\n");


        result.append("<tr>");
        result.append("<td class=\"default\" >");
        result.append(GeneralVariables.getStringFromResource(R.string.html_radio_frequency));
        result.append("</td>\n");
        result.append("<td class=\"default\" >");
        if (mainViewModel.baseRig != null) {
            result.append(BaseRigOperation.getFrequencyStr(mainViewModel.baseRig.getFreq()));
        } else {
            result.append("-");
        }
        result.append("</td>");
        result.append("</tr>\n");

        result.append("<tr class=\"bbb\">>");
        result.append("<td class=\"default\" >");
        result.append(GeneralVariables.getStringFromResource(R.string.be_excluded_callsigns));
        result.append("</td>\n");
        result.append("<td class=\"default\" >");
        result.append(GeneralVariables.getExcludeCallsigns());
        result.append("</td>");
        result.append("</tr>\n");



        result.append("</table><br>");


        result.append("<table bgcolor=\"#a1a1a1\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">\n");
        result.append(String.format("<tr><th>%s</th></tr>\n"
                , String.format(GeneralVariables.getStringFromResource(R.string.html_successful_callsign)
                        , GeneralVariables.getBandString())));

        result.append("<tr><td class=\"default\" >");
        for (int i = 0; i < GeneralVariables.QSL_Callsign_list.size(); i++) {
            result.append(GeneralVariables.QSL_Callsign_list.get(i));
            result.append(",&nbsp;");
            if (((i + 1) % 10) == 0) {
                result.append("</td></tr><tr></td><td class=\"default\" >");
            }
        }
        result.append("</td></tr></table><br>\n");


        result.append("<table bgcolor=\"#a1a1a1\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">\n");
        result.append(String.format("<tr><th>%s</th></tr>\n"
                , GeneralVariables.getStringFromResource(R.string.html_tracking_callsign)));

        result.append("<tr><td class=\"default\" >");
        for (int i = 0; i < GeneralVariables.followCallsign.size(); i++) {
            result.append(GeneralVariables.followCallsign.get(i));
            result.append(",&nbsp;");
            if (((i + 1) % 10) == 0) {
                result.append("</td></tr><tr></td><td class=\"default\" >");
            }
        }
        result.append("</td></tr></table>\n");

        result.append("<table bgcolor=\"#a1a1a1\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">\n");
        result.append(String.format("<tr><th>%s</th></tr>\n"
                , GeneralVariables.getStringFromResource(R.string.html_tracking_qso_information)));
//
//
//        result.append(GeneralVariables.qslRecordList.toHTML());
        result.append("<tr><td class=\"default\" >");
        result.append(GeneralVariables.qslRecordList.toHTML());
        result.append("</td></tr></table>\n");
        // result.append("</table>\n");

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
        result.append("<table bgcolor=\"#a1a1a1\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">\n");
        result.append("<tr>");
        result.append(String.format("<th>%s</th>"
                , GeneralVariables.getStringFromResource(R.string.html_callsign)));
        result.append(String.format("<th>%s</th>"
                , GeneralVariables.getStringFromResource(R.string.html_hash_value)));
        result.append("</tr>\n");

        int order = 0;
        for (Map.Entry<Long,String> entry: Ft8Message.hashList.entrySet()) {
            if ((order / 3) % 2 == 0) {
                result.append("<tr>");
            } else {
                result.append("<tr class=\"bbb\">");
            }
            result.append(String.format("<td class=\"default\" >%s</td>", entry.getValue()));
            result.append(String.format("<td class=\"default\" >0x%x</td>", entry.getKey()));
            result.append("</tr>\n");
            order++;
        }
//        for (HashStruct hash : Ft8Message.hashList) {
//            if ((order / 3) % 2 == 0) {
//                result.append("<tr>");
//            } else {
//                result.append("<tr class=\"bbb\">");
//            }
//            result.append(String.format("<td class=\"default\" >%s</td>", hash.callsign));
//            result.append(String.format("<td class=\"default\" >0x%x</td>", hash.hashCode));
//            result.append("</tr>\n");
//            order++;
//        }

        result.append("</table>");

        return result.toString();
    }


    /**
     * 查消息表
     *
     * @return html
     */
    private String getMessages() {
        Cursor cursor = mainViewModel.databaseOpr.getDb().rawQuery(
                "select * from Messages order by ID DESC ", null);
        return HtmlContext.ListTableContext(cursor);
    }

    /**
     * 获取全部通联日志
     *
     * @return HTML
     */
    private String showAllQSL() {
        Cursor cursor = mainViewModel.databaseOpr.getDb().rawQuery(
                "select * from QSLTable order by ID DESC ", null);
        return HtmlContext.ListTableContext(cursor);
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
        return HtmlContext.ListTableContext(cursor);
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
        result.append("<table bgcolor=\"#a1a1a1\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">\n");
        result.append("<tr>");
        result.append("<th>UTC</th>");
        result.append("<th>dB</th>");
        result.append("<th>Dt</th>");
        result.append(String.format("<th>%s</th>"
                , GeneralVariables.getStringFromResource(R.string.html_qsl_freq)));
        result.append(String.format("<th>%s</th>"
                , GeneralVariables.getStringFromResource(R.string.message)));
        result.append(String.format("<th>%s</th>"
                , GeneralVariables.getStringFromResource(R.string.html_carrier_frequency_band)));
        result.append("</tr>\n");
        int order = 0;
        if (mainViewModel.currentMessages != null) {
            for (Ft8Message message : mainViewModel.currentMessages) {
                if (order % 2 == 0) {
                    result.append("<tr align=center>");
                } else {
                    result.append("<tr align=center class=\"bbb\">");
                }
                result.append(message.toHtml());
                result.append("<tr>");
                order++;
            }
        }
        result.append("</table><br>");
        return result.toString();
    }

    /**
     * 显示导入FT8CN日志文件的HTML
     *
     * @return HTML
     */
    private String showImportLog() {
        StringBuilder result = new StringBuilder();
        result.append("<table bgcolor=\"#a1a1a1\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">\n");
        result.append(String.format("<tr class=\"bbb\"><td class=\"default\">" +
                        "%s<font size=5 color=red>%s</font>%s</td></tr>"
                , GeneralVariables.getStringFromResource(R.string.html_please_select)
                , GeneralVariables.getStringFromResource(R.string.html_adi_format)
                , GeneralVariables.getStringFromResource(R.string.html_file_in_other_format)));
        result.append("<tr><td class=\"default\"><br><form action=\"importLogData\" method=\"post\"\n" +
                "            enctype=\"multipart/form-data\">\n" +
                "            <input type=\"file\" name=\"file1\" id=\"file1\" title=\"上传ADI文件\" accept=\".adi,.txt\" />\n" +
                "            <input type=\"submit\" value=\"上传\" />\n" +
                "        </form></td></tr>");
        result.append("</table>");
        return result.toString();
    }

    @SuppressLint("Range")
    private String showQSLTable() {

        StringBuilder result = new StringBuilder();
        result.append("<table bgcolor=\"#a1a1a1\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">\n");
        result.append(String.format("<tr><th>%s</th>"
                , GeneralVariables.getStringFromResource(R.string.html_time)));
        result.append(String.format("<th>%s</th>"
                , GeneralVariables.getStringFromResource(R.string.html_total)));
        result.append(String.format("<th>%s</th>"
                , GeneralVariables.getStringFromResource(R.string.html_operation)));
        result.append(String.format("<th>%s</th>"
                , GeneralVariables.getStringFromResource(R.string.html_operation)));
        result.append(String.format("<th>%s</th></tr>"
                , GeneralVariables.getStringFromResource(R.string.html_operation)));

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

        result.append("<tr align=center class=\"bbb\">");

        result.append(String.format("<td  class=\"default\"><a href=\"/showQsl/%s\">%s</a></td>"
                , UtcTimer.getYYYYMMDD(UtcTimer.getSystemTime())
                , GeneralVariables.getStringFromResource(R.string.html_today_log)));
        result.append(String.format("<td  class=\"default\">%s</td>"
                , cursor.getString(cursor.getColumnIndex("b"))));
        result.append(String.format("<td  class=\"default\"><a href=\"/downQsl/%s\">%s</a></td>"
                , UtcTimer.getYYYYMMDD(UtcTimer.getSystemTime())
                , GeneralVariables.getStringFromResource(R.string.html_download_all)));
        result.append(String.format("<td  class=\"default\"><a href=\"/downQslNoQSL/%s\">%s</a></td>"
                , UtcTimer.getYYYYMMDD(UtcTimer.getSystemTime())
                , GeneralVariables.getStringFromResource(R.string.html_download_unconfirmed)));

        result.append(String.format("<td  class=\"default\"><a href=\"/delQsl/%s\">%s</a></td></tr>"
                , UtcTimer.getYYYYMMDD(UtcTimer.getSystemTime())
                , GeneralVariables.getStringFromResource(R.string.html_delete)));
        cursor.close();

        int order = 1;
        cursor = mainViewModel.databaseOpr.getDb().rawQuery("select SUBSTR(qso_date,1,6) as a,count(*) as b from QSLTable\n" +
                "group by SUBSTR(qso_date,1,6)", null);
        while (cursor.moveToNext()) {
            if (order % 2 == 0) {
                result.append("<tr align=center class=\"bbb\">");
            } else {
                result.append("<tr align=center>");
            }
            result.append(String.format("<td  class=\"default\"><a href=\"/showQsl/%s\">%s</a></td>"
                    , cursor.getString(cursor.getColumnIndex("a"))
                    , cursor.getString(cursor.getColumnIndex("a"))));
            result.append(String.format("<td  class=\"default\">%s</td>"
                    , cursor.getString(cursor.getColumnIndex("b"))));
            result.append(String.format("<td  class=\"default\"><a href=\"/downQsl/%s\">%s</a></td>"
                    , cursor.getString(cursor.getColumnIndex("a"))
                    , GeneralVariables.getStringFromResource(R.string.html_download_all)));
            result.append(String.format("<td  class=\"default\"><a href=\"/downQslNoQSL/%s\">%s</a></td>"
                    , cursor.getString(cursor.getColumnIndex("a"))
                    , GeneralVariables.getStringFromResource(R.string.html_download_unconfirmed)));

            result.append(String.format("<td  class=\"default\"><a href=\"/delQsl/%s\">%s</a></td></tr>"
                    , cursor.getString(cursor.getColumnIndex("a"))
                    , GeneralVariables.getStringFromResource(R.string.html_delete)));
            order++;
        }
        result.append("</table>");
        //cursor.close();
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
        return downQSLTable(cursor);
    }

    /**
     * 下载全部日志
     *
     * @return
     */
    private String downAllQSl() {
        Cursor cursor = mainViewModel.databaseOpr.getDb().rawQuery("select * from QSLTable", null);
        return downQSLTable(cursor);
    }

    /**
     * 生成QSL记录文本
     *
     * @return 日志内容
     */
    @SuppressLint({"Range", "DefaultLocale"})
    private String downQSLTable(Cursor cursor) {
        //Cursor cursor = mainViewModel.databaseOpr.getDb().rawQuery("select * from QSLTable", null);
        StringBuilder logStr = new StringBuilder();

        logStr.append("FT8CN ADIF Export<eoh>\n");
        while (cursor.moveToNext()) {
            logStr.append(String.format("<call:%d>%s "
                    , cursor.getString(cursor.getColumnIndex("call")).length()
                    , cursor.getString(cursor.getColumnIndex("call"))));

            if (cursor.getInt(cursor.getColumnIndex("isLotW_QSL"))==1){
                logStr.append("<QSL_RCVD:1>Y ");
            }else {
                logStr.append("<QSL_RCVD:1>N ");
            }
            if (cursor.getInt(cursor.getColumnIndex("isQSL"))==1){
                logStr.append("<QSL_MANUAL:1>Y ");
            }else {
                logStr.append("<QSL_MANUAL:1>N ");
            }
            logStr.append(String.format("<gridsquare:%d>%s "
                    , cursor.getString(cursor.getColumnIndex("gridsquare")).length()
                    , cursor.getString(cursor.getColumnIndex("gridsquare"))));

            logStr.append(String.format("<mode:%d>%s "
                    , cursor.getString(cursor.getColumnIndex("mode")).length()
                    , cursor.getString(cursor.getColumnIndex("mode"))));

            logStr.append(String.format("<rst_sent:%d>%s "
                    , cursor.getString(cursor.getColumnIndex("rst_sent")).length()
                    , cursor.getString(cursor.getColumnIndex("rst_sent"))));

            logStr.append(String.format("<rst_rcvd:%d>%s "
                    , cursor.getString(cursor.getColumnIndex("rst_rcvd")).length()
                    , cursor.getString(cursor.getColumnIndex("rst_rcvd"))));

            logStr.append(String.format("<qso_date:%d>%s "
                    , cursor.getString(cursor.getColumnIndex("qso_date")).length()
                    , cursor.getString(cursor.getColumnIndex("qso_date"))));

            logStr.append(String.format("<time_on:%d>%s "
                    , cursor.getString(cursor.getColumnIndex("time_on")).length()
                    , cursor.getString(cursor.getColumnIndex("time_on"))));

            logStr.append(String.format("<qso_date_off:%d>%s "
                    , cursor.getString(cursor.getColumnIndex("qso_date_off")).length()
                    , cursor.getString(cursor.getColumnIndex("qso_date_off"))));

            logStr.append(String.format("<time_off:%d>%s "
                    , cursor.getString(cursor.getColumnIndex("time_off")).length()
                    , cursor.getString(cursor.getColumnIndex("time_off"))));

            logStr.append(String.format("<band:%d>%s "
                    , cursor.getString(cursor.getColumnIndex("band")).length()
                    , cursor.getString(cursor.getColumnIndex("band"))));

            logStr.append(String.format("<freq:%d>%s "
                    , cursor.getString(cursor.getColumnIndex("freq")).length()
                    , cursor.getString(cursor.getColumnIndex("freq"))));

            logStr.append(String.format("<station_callsign:%d>%s "
                    , cursor.getString(cursor.getColumnIndex("station_callsign")).length()
                    , cursor.getString(cursor.getColumnIndex("station_callsign"))));

            logStr.append(String.format("<my_gridsquare:%d>%s "
                    , cursor.getString(cursor.getColumnIndex("my_gridsquare")).length()
                    , cursor.getString(cursor.getColumnIndex("my_gridsquare"))));

            String comment = cursor.getString(cursor.getColumnIndex("comment"));

            //<comment:15>Distance: 99 km <eor>
            //在写库的时候，一定要加" km"
            logStr.append(String.format("<comment:%d>%s <eor>\n"
                    , comment.length()
                    , comment));
        }

        //Log.e(TAG, "getQSLTable: " + logStr.toString());
        return logStr.toString();
    }

}
