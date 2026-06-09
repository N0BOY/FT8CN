package com.bg7yoz.ft8cn.log;
import android.util.Log;

import com.bg7yoz.ft8cn.GeneralVariables;

import org.json.JSONStringer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

enum ServiceType{
    Cloudlog,
    QRZ
}

public class ThirdPartyService {
    public static String TAG = "ThirdPartyService";
    private static final String[] CLOUDLOG_API_PATHS = {
            "index.php/api/qso",
            "api/qso/",
            "api/qso"
    };

    private static String QSLRecordToADIF(QSLRecord qslRecord, ServiceType serv){
        StringBuilder logStr = new StringBuilder();
        logStr.append(String.format("<call:%d>%s "
                , qslRecord.getToCallsign().length()
                , qslRecord.getToCallsign()));

        if (qslRecord.getToMaidenGrid() != null) {
            logStr.append(String.format("<gridsquare:%d>%s "
                    , qslRecord.getToMaidenGrid().length()
                    , qslRecord.getToMaidenGrid()));
        }

        if (qslRecord.getMode() != null) {
            logStr.append(String.format("<mode:%d>%s "
                    , qslRecord.getMode().length()
                    , qslRecord.getMode()));
        }

        if (String.valueOf(qslRecord.getSendReport()) != null) {
            logStr.append(String.format("<rst_sent:%d>%s "
                    , String.valueOf(qslRecord.getSendReport()).length()
                    , String.valueOf(qslRecord.getSendReport())));
        }

        if (String.valueOf(qslRecord.getReceivedReport()) != null) {
            logStr.append(String.format("<rst_rcvd:%d>%s "
                    , String.valueOf(qslRecord.getReceivedReport()).length()
                    , String.valueOf(qslRecord.getReceivedReport())));
        }

        if (qslRecord.getQso_date() != null) {
            logStr.append(String.format("<qso_date:%d>%s "
                    , qslRecord.getQso_date().length()
                    , qslRecord.getQso_date()));
        }

        if (qslRecord.getTime_on() != null) {
            logStr.append(String.format("<time_on:%d>%s "
                    , qslRecord.getTime_on().length()
                    , qslRecord.getTime_on()));
        }
        if (qslRecord.getBandLength() != null) {
            logStr.append(String.format("<band:%d>%s "
                    , qslRecord.getBandLength().length()
                    , qslRecord.getBandLength()));
        }

        if (qslRecord.getQso_date_off() != null) {
            logStr.append(String.format("<qso_date_off:%d>%s "
                    , qslRecord.getQso_date_off().length()
                    , qslRecord.getQso_date_off()));
        }

        if (qslRecord.getTime_off() != null) {
            logStr.append(String.format("<time_off:%d>%s "
                    , qslRecord.getTime_off().length()
                    , qslRecord.getTime_off()));
        }

        if (String.valueOf(qslRecord.getBandFreq()) != null) {
            String freq = "";
            Log.d(TAG,String.valueOf(qslRecord.getBandFreq()));
            if (serv == ServiceType.Cloudlog || serv == ServiceType.QRZ){
                double i = (double)qslRecord.getBandFreq() / 1000000;
                freq = String.valueOf(i);
            }

            logStr.append(String.format("<freq:%d>%s "
                    , freq.length()
                    , freq));
        }

        if (qslRecord.getMyCallsign() != null) {
            logStr.append(String.format("<station_callsign:%d>%s "
                    , qslRecord.getMyCallsign().length()
                    , qslRecord.getMyCallsign()));
        }

        if (qslRecord.getMyMaidenGrid() != null) {
            logStr.append(String.format("<my_gridsquare:%d>%s "
                    , qslRecord.getMyMaidenGrid().length()
                    , qslRecord.getMyMaidenGrid()));
        }

        String comment = qslRecord.getComment();

        //<comment:15>Distance: 99 km <eor>
        //在写库的时候，一定要加" km"
        logStr.append(String.format("<comment:%d>%s <eor>\n"
                , comment.length()
                , comment));
        return logStr.toString();
    }
    public static void UploadToCloudLog(QSLRecord qslRecord){
        // 转换为adif格式
        String logStr = QSLRecordToADIF(qslRecord,ServiceType.Cloudlog);
        Log.d(TAG,logStr);
        String address = GeneralVariables.getCloudlogServerAddress();
        if (!address.endsWith("/")){
            address+="/";
        }
        try {
            JSONStringer js = new JSONStringer();
            String result = js.object()
                    .key("key").value(GeneralVariables.getCloudlogServerApiKey())
                    .key("station_profile_id").value(GeneralVariables.getCloudlogStationID())
                    .key("type").value("adif").key("string").value(logStr).endObject().toString();

            HttpResult cloudlogResult = postCloudlog(address, result);
            if (cloudlogResult.isSuccess()) {
                Log.d(TAG,"Updated to Cloudlog successfully. result:"+cloudlogResult.body);
            } else {
                Log.d(TAG,"Cloudlog upload failed. code:" + cloudlogResult.responseCode
                        + " result:" + cloudlogResult.body);
            }
        }catch (Exception k){
            Log.d(TAG, k.toString());
        }
    }
    public static boolean CheckCloudlogConnection(){
        String address = GeneralVariables.getCloudlogServerAddress();
        String apiKey = GeneralVariables.getCloudlogServerApiKey();
        // 检查地址末尾是否含有 /
        if (!address.endsWith("/")){
            address+="/";
        }
        try{
            String stationId = GeneralVariables.getCloudlogStationID();
            if (apiKey == null || apiKey.trim().isEmpty()) {
                return false;
            }
            if (stationId == null || stationId.trim().isEmpty()) {
                return false;
            }
            String testAdif = "<CALL:6>TEST01 <BAND:3>20m <MODE:4>FT8 <QSO_DATE:8>20260101 <TIME_ON:6>000000 <RST_SENT:3>599 <RST_RCVD:3>599 <EOR>";
            JSONStringer js = new JSONStringer();
            String json = js.object()
                    .key("key").value(apiKey)
                    .key("station_profile_id").value(stationId)
                    .key("type").value("adif")
                    .key("string").value(testAdif)
                    .endObject().toString();
            HttpResult result = postCloudlog(address, json);
            Log.d(TAG, "Cloudlog test result code:" + result.responseCode + " body:" + result.body);
            return result.responseCode >= 200 && result.responseCode < 300;
        }catch (Exception e){
            Log.d(TAG, e.toString());
            return false;
        }
    }

    public static boolean CheckQRZConnection(){
        String apiKey = GeneralVariables.getQrzApiKey();
        try{
            String url = "https://logbook.qrz.com/api?KEY="+apiKey+"&ACTION=STATUS";
            String result = sendGetRequest(url);
            HashMap<String,String> status = new HashMap<>();
            for (String s : result.split("&")) {
                String[] split = s.split("=");
                if (split.length>1){
                    status.put(split[0],split[1]);
                }
            }
            Log.d(TAG, status.toString());
            if (!status.get("RESULT").equals("OK")){
                return false;
            }
            return true;
        }catch (Exception e){
            Log.d(TAG, e.toString());
            return false;
        }
    }

    public static void UploadToQRZ(QSLRecord qslRecord){
        // 转换为adif格式
        String logStr = QSLRecordToADIF(qslRecord, ServiceType.QRZ);
        Log.d(TAG,logStr);
        String apikey = GeneralVariables.getQrzApiKey();
        HashMap<String,String> json = new HashMap<>();

        String url = String.format("https://logbook.qrz.com/api/KEY=%s&ACTION=INSERT&ADIF=%s",apikey,logStr);

        try {
            String result = sendGetRequest(url);
            Log.d(TAG,"Updated to QRZ successfully. result:" + result);
        }catch (Exception k){
            Log.d(TAG, k.toString());
        }
    }

    public static String sendPostRequest(String url, String json) throws IOException {
        return sendPostRequestWithCode(url, json).body;
    }

    private static HttpResult postCloudlog(String address, String json) throws IOException {
        HttpResult lastResult = null;
        for (String path : CLOUDLOG_API_PATHS) {
            String url = address + path;
            Log.d(TAG, "Cloudlog POST URL: " + url);
            HttpResult result = sendPostRequestWithCode(url, json);
            lastResult = result;
            if (result.responseCode != HttpURLConnection.HTTP_NOT_FOUND) {
                return result;
            }
        }
        return lastResult == null ? new HttpResult(-1, "") : lastResult;
    }

    private static HttpResult sendPostRequestWithCode(String url, String json) throws IOException {
        HttpURLConnection conn = null;
        BufferedReader reader = null;

        try {
            URL urlObj = new URL(url);
            conn = (HttpURLConnection) urlObj.openConnection();

            // 设置请求方法为POST
            conn.setRequestMethod("POST");
            // 设置请求的头部信息
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setDoOutput(true);

            // 获取OutputStream，将请求的数据写入流中
            OutputStream os = conn.getOutputStream();
            os.write(json.getBytes("UTF-8"));
            os.flush();
            os.close();

            // 获取服务器的响应结果
            int responseCode = conn.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            } else {
                InputStream errorStream = conn.getErrorStream();
                if (errorStream != null) {
                    reader = new BufferedReader(new InputStreamReader(errorStream));
                }
            }

            StringBuilder response = new StringBuilder();
            if (reader != null) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }
            return new HttpResult(responseCode, response.toString());
        } finally {
            if (reader != null) {
                reader.close();
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
    public static String sendGetRequest(String url) throws IOException {
        HttpURLConnection conn = null;
        BufferedReader reader = null;

        try {
            URL urlObj = new URL(url);
            conn = (HttpURLConnection) urlObj.openConnection();

            // 设置请求方法为POST
            conn.setRequestMethod("GET");
            // 设置请求的头部信息
            conn.setRequestProperty("Content-Type", "application/json");

            // 获取服务器的响应结果
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                return response.toString();
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
            if (reader != null) {
                reader.close();
            }
        }
        return null;
    }

    private static class HttpResult {
        final int responseCode;
        final String body;

        HttpResult(int responseCode, String body) {
            this.responseCode = responseCode;
            this.body = body == null ? "" : body;
        }

        boolean isSuccess() {
            return responseCode >= 200 && responseCode < 300;
        }
    }
}
