package com.bg7yoz.ft8cn.count;
/**
 * 用于通联日志统计的的数据库操作。
 * 注：目前归属地的统计，是基于网格的，如果基于呼号的前缀统计，虽然准确，但统计速度太慢，用户的交互效果不好。
 *
 * @author BGY70Z
 * @date 2023-03-20
 */

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;

import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.callsign.CallsignInfo;
import com.bg7yoz.ft8cn.maidenhead.MaidenheadGrid;

import java.util.ArrayList;

public class CountDbOpr {
    private static final String TAG="CountDbOpr";

    public enum ChartType {Bar, Pie, Line, None}
    public static final int ChartBar=0;
    public static final int ChartPie=1;
    public static final int ChartLine=2;
    public static final int ChartNone=3;
    public static void getQSLTotal(SQLiteDatabase db,AfterCount afterCount){
        //通联的呼号数量
        //SELECT count(DISTINCT callsign)  FROM QslCallsigns qc
        new GetTotal(db,afterCount).execute();
    }

    public static void getDxcc(SQLiteDatabase db,AfterCount afterCount){
        new GetDxccCount(db,afterCount).execute();
    }

    public static void getCQZoneCount(SQLiteDatabase db,AfterCount afterCount){
        new GetCqZoneCount(db,afterCount).execute();
    }
    public static void getItuCount(SQLiteDatabase db,AfterCount afterCount){
        new GetItuZoneCount(db,afterCount).execute();
    }
    public static void getBandCount(SQLiteDatabase db,AfterCount afterCount){
        new GetBandCount(db,afterCount).execute();
    }

    public static void getDistanceCount(SQLiteDatabase db,AfterCount afterCount){
        new DistanceCount(db,afterCount).execute();
    }


    static class DistanceCount extends AsyncTask<Void,Void,Void>{
        private final SQLiteDatabase db;
        private final AfterCount afterCount;

        public DistanceCount(SQLiteDatabase db, AfterCount afterCount) {
            this.db = db;
            this.afterCount = afterCount;
        }

        @SuppressLint("Range")
        @Override
        protected Void doInBackground(Void... voids) {
            String querySQL;
            Cursor cursor;
            double maxDistance=-1;
            String maxDistanceGrid="";
            double minDistance=6553500f;
            String minDistanceGrid="";

            ArrayList<CountValue> values=new ArrayList<>();

            ArrayList<String> bands=new ArrayList<>();
            querySQL="select DISTINCT band from QSLTable q WHERE gridsquare<>\"\"";
            cursor = db.rawQuery(querySQL,null);

            while (cursor.moveToNext()){
                bands.add(cursor.getString(cursor.getColumnIndex("band")));
            }

            for (String band:bands) {

                querySQL = "SELECT DISTINCT SUBSTR(gridsquare,1,4) as g FROM QSLTable q where (gridsquare <>\"\")and(band=?)";
                cursor = db.rawQuery(querySQL, new String[]{band});
                double max=-1;
                String maxGrid="";
                double min=6553500f;
                String minGrid="";

                while (cursor.moveToNext()) {
                    String grid = cursor.getString(cursor.getColumnIndex("g"));
                    double distance = MaidenheadGrid.getDist(grid, GeneralVariables.getMyMaidenheadGrid());
                    if (distance > maxDistance) {
                        maxDistance = distance;
                        maxDistanceGrid = grid;
                    }
                    if (distance < minDistance) {
                        minDistance = distance;
                        minDistanceGrid = grid;
                    }

                    if (distance > max) {
                        max = distance;
                        maxGrid = grid;
                    }
                    if (distance < min) {
                        min = distance;
                        minGrid = grid;
                    }

                }
                cursor.close();

                if ((max>-1)&&(min<6553500f)){
                    values.add(new CountValue((int) Math.round(max),String.format(
                            GeneralVariables.getStringFromResource(R.string.maximum_distance)
                            ,getQSLInfo(maxGrid))));
                    values.add(new CountValue((int) Math.round(min),String.format(
                            GeneralVariables.getStringFromResource(R.string.minimum_distance)
                            ,getQSLInfo(minGrid))));
                }
            }

           String info=String.format(GeneralVariables.getStringFromResource(R.string.count_distance_info)
                   ,maxDistance,maxDistanceGrid,minDistance,minDistanceGrid);

            if (afterCount!=null &&(maxDistance>0)&&(minDistance<6553500f)){
                afterCount.countInformation(new CountInfo(info
                        ,ChartType.None
                        ,GeneralVariables.getStringFromResource(R.string.distance_statistics)
                        ,values));
            }
            return null;
        }

        @SuppressLint("Range")
        private String getQSLInfo(String grid){
            String querySQL="SELECT call,band,freq,qso_date,time_on,gridsquare  FROM QSLTable q " +
                    "where SUBSTR(gridsquare,1,4) =? LIMIT 1";
            Cursor cursor = db.rawQuery(querySQL,new String[]{grid});
            StringBuilder result=new StringBuilder();
            int breakLine=0;
            while (cursor.moveToNext()){
                 String call=cursor.getString(cursor.getColumnIndex("call"));
                 String band=cursor.getString(cursor.getColumnIndex("band"));
                 String freq=cursor.getString(cursor.getColumnIndex("freq"));
                 //String qso_date=cursor.getString(cursor.getColumnIndex("qso_date"));
                 //String time_on=cursor.getString(cursor.getColumnIndex("time_on"));
                 String gridsquare=cursor.getString(cursor.getColumnIndex("gridsquare"));

                 //获取呼号的位置
                CallsignInfo callsignInfo= GeneralVariables.callsignDatabase.getCallInfo(call);

                 if (breakLine>0) result.append("\n");
                 result.append(String.format("%s %s(%s) %s\n %s", call, freq, band
                         ,gridsquare,callsignInfo.toString()));
                 breakLine++;
            }
            return  result.toString();
        }


    }


    /**
     * 统计各波段比例
     */
    static class GetBandCount extends AsyncTask<Void, Void, Void> {
        private final SQLiteDatabase db;
        private final AfterCount afterCount;

        public GetBandCount(SQLiteDatabase db, AfterCount afterCount) {
            this.db = db;
            this.afterCount = afterCount;
        }

        @SuppressLint({"Range", "DefaultLocale"})
        @Override
        protected Void doInBackground(Void... voids) {
            int successCount=0;
            ArrayList<CountValue> values=new ArrayList<>();
            String querySQL;
            Cursor cursor;
            querySQL="SELECT UPPER( band) as band ,count(*) as c FROM QSLTable q \n" +
                    "GROUP BY UPPER( band) ORDER BY COUNT(*) desc ";
            cursor = db.rawQuery(querySQL,null);
            while (cursor.moveToNext()){
                int count=cursor.getInt(cursor.getColumnIndex("c"));
                successCount=successCount+count;
                values.add(new CountValue(count
                        ,String.format("%s",cursor.getString(cursor.getColumnIndex("band")))));
            }
            StringBuilder stringBuilder=new StringBuilder();
            stringBuilder.append(String.format(GeneralVariables.getStringFromResource(R.string.count_total),successCount));
            for (int i = 0; i < values.size(); i++) {
                stringBuilder.append(String.format("\n%s:\t%d",values.get(i).name,values.get(i).value));
            }
            cursor.close();
            if (afterCount!=null){
                afterCount.countInformation(new CountInfo(
                        stringBuilder.toString()
                        ,ChartType.Pie,GeneralVariables.getStringFromResource(R.string.band_statistics)
                        ,values));
            }

            return null;
        }
    }
    /**
     * 统计通联数量
     */
    static class GetTotal extends AsyncTask<Void, Void, Void>{
        private final SQLiteDatabase db;
        private final AfterCount afterCount;

        public GetTotal(SQLiteDatabase db, AfterCount afterCount) {
            this.db = db;
            this.afterCount = afterCount;
        }

        @SuppressLint({"DefaultLocale", "Range"})
        @Override
        protected Void doInBackground(Void... voids) {

            int logCount=0;//日志的数量
            int callsignCount=0;//呼号的数量
            int isQslCount=0;//确认的数量
            int isLotwQslCount=0;//三方平台确认的数量

            //通联的呼号数量
            String querySQL;
            Cursor cursor;
            querySQL="SELECT count(*) AS C FROM QSLTable q";
            cursor = db.rawQuery(querySQL,null);
            cursor.moveToFirst();
            logCount=cursor.getInt(cursor.getColumnIndex("C"));
            cursor.close();

            querySQL="SELECT count(DISTINCT \"call\") AS C FROM QSLTable q ";
            cursor = db.rawQuery(querySQL,null);
            cursor.moveToFirst();
            callsignCount=cursor.getInt(cursor.getColumnIndex("C"));
            cursor.close();

            querySQL="SELECT count(*) AS C FROM QSLTable q   WHERE isQSL =1 or isLotW_QSL =1";
            cursor = db.rawQuery(querySQL,null);
            cursor.moveToFirst();
            isQslCount=cursor.getInt(cursor.getColumnIndex("C"));
            cursor.close();

            querySQL="SELECT count(*) AS C FROM QSLTable q   WHERE isLotW_QSL =1";
            cursor = db.rawQuery(querySQL,null);
            cursor.moveToFirst();
            isLotwQslCount=cursor.getInt(cursor.getColumnIndex("C"));
            cursor.close();

            float qslPercent=0;
            if (logCount>0){
                qslPercent=100f*(float) isQslCount/(float) logCount;
            }
            //result.append(String.format("通联的呼号数量:%d",cursor.getInt(cursor.getColumnIndex("C"))));

            if (afterCount!=null){
                ArrayList<CountValue> values=new ArrayList<>();
                values.add(new CountValue(isQslCount,GeneralVariables.getStringFromResource(R.string.count_confirmed)));
                values.add(new CountValue(logCount-isQslCount,GeneralVariables.getStringFromResource(R.string.count_unconfirmed)));
                StringBuilder stringBuilder=new StringBuilder();
                stringBuilder.append(GeneralVariables.getStringFromResource(R.string.count_total_logs));
                stringBuilder.append("\n"+GeneralVariables.getStringFromResource(R.string.count_confirmed_log));
                stringBuilder.append("\n"+GeneralVariables.getStringFromResource(R.string.count_lotw_confirmed));
                stringBuilder.append("\n"+GeneralVariables.getStringFromResource(R.string.count_manually_confirmed));
                stringBuilder.append("\n"+GeneralVariables.getStringFromResource(R.string.count_confirmed_proportion));
                stringBuilder.append("\n"+GeneralVariables.getStringFromResource(R.string.count_tota_callsigns));
                afterCount.countInformation(new CountInfo(
                        //String.format("日志数:%d\n确认的日志数:%d\n平台确认的日志数:%d" +
                        //        "\n手动确认的日志数:%d\n日志确认比例:%.1f%%\n呼号数量:%d"
                        String.format(stringBuilder.toString()
                        ,logCount,isQslCount,isLotwQslCount,isQslCount-isLotwQslCount,qslPercent,callsignCount)
                        ,ChartType.Pie,GeneralVariables.getStringFromResource(R.string.confirmation_statistics)
                        ,values));
            }

            return null;
        }
    }

    /**
     * 统计ITU分区数量
     */
    static class GetItuZoneCount extends AsyncTask<Void, Void, Void> {
        private static final String TAG = "GetItuZoneCount";
        private final SQLiteDatabase db;
        private final AfterCount afterCount;

        public GetItuZoneCount(SQLiteDatabase db, AfterCount afterCount) {
            this.db = db;
            this.afterCount = afterCount;
        }

        @SuppressLint({"Range", "DefaultLocale"})
        @Override
        protected Void doInBackground(Void... voids) {
            ArrayList<CountValue> countValues=new ArrayList<>();
            int ituZoneCount=0;
            int successCount=0;
            String querySQL;
            Cursor cursor;


            querySQL="SELECT count(DISTINCT itu) as c From ituList il ";

            cursor = db.rawQuery(querySQL,null);
            cursor.moveToFirst();
            ituZoneCount=cursor.getInt(cursor.getColumnIndex("c"));
            cursor.close();

            querySQL="SELECT il.itu  ,count(*) as c  FROM   ituList il \n" +
                    "inner join  QSLTable q\n" +
                    "on  il.grid =UPPER(SUBSTR(q.gridsquare,1,4))\n" +
                    "GROUP BY il.itu ORDER BY COUNT(*) DESC";

            cursor = db.rawQuery(querySQL,null);
            while (cursor.moveToNext()){
                successCount++;
                countValues.add(new CountValue(cursor.getInt(cursor.getColumnIndex("c"))
                        ,String.format(GeneralVariables.getStringFromResource(R.string.count_zone)
                                ,cursor.getString(cursor.getColumnIndex("itu")))));
            }

            if (afterCount!=null){
                afterCount.countInformation(new CountInfo(""
                        ,ChartType.Bar
                        ,GeneralVariables.getStringFromResource(R.string.itu_completion_statistics)
                        ,countValues));
            }
            if (afterCount!=null&&ituZoneCount!=0){
                ArrayList<CountValue> values=new ArrayList<>();
                values.add(new CountValue(successCount
                        ,GeneralVariables.getStringFromResource(R.string.count_completed)));
                values.add(new CountValue(ituZoneCount-successCount
                        ,GeneralVariables.getStringFromResource(R.string.count_incomplete)));
                afterCount.countInformation(new CountInfo(
                        String.format(GeneralVariables.getStringFromResource(R.string.count_total_itu)
                                ,ituZoneCount,successCount,100f*(float)successCount/(float) ituZoneCount)
                        ,ChartType.Pie
                        ,GeneralVariables.getStringFromResource(R.string.count_itu_completed_scale)
                        ,values));
            }
            return null;
        }
    }



    /**
     * 统计CQ分区
     */
    static class GetCqZoneCount extends AsyncTask<Void, Void, Void> {
        private static final String TAG = "GetCqZoneCount";
        private final SQLiteDatabase db;
        private final AfterCount afterCount;

        public GetCqZoneCount(SQLiteDatabase db, AfterCount afterCount) {
            this.db = db;
            this.afterCount = afterCount;
        }

        @SuppressLint({"Range", "DefaultLocale"})
        @Override
        protected Void doInBackground(Void... voids) {
            ArrayList<CountValue> countValues=new ArrayList<>();
            int cqZoneCount=0;
            int successCount=0;
            String querySQL;
            Cursor cursor;


            querySQL="SELECT count(DISTINCT cqzone) as c From cqzoneList cl ";

            cursor = db.rawQuery(querySQL,null);
            cursor.moveToFirst();
            cqZoneCount=cursor.getInt(cursor.getColumnIndex("c"));
            cursor.close();

            querySQL="SELECT cl.cqzone ,count(*) as c  FROM   cqzoneList cl \n" +
                    "inner join  QSLTable q \n" +
                    "on  cl.grid =UPPER(SUBSTR(q.gridsquare,1,4))   \n" +
                    "GROUP BY cl.cqzone ORDER BY COUNT(*) DESC";

            cursor = db.rawQuery(querySQL,null);
            while (cursor.moveToNext()){
                successCount++;
                countValues.add(new CountValue(cursor.getInt(cursor.getColumnIndex("c"))
                        ,String.format(GeneralVariables.getStringFromResource(R.string.count_zone)
                                ,cursor.getString(cursor.getColumnIndex("cqzone")))));
            }

            if (afterCount!=null){
                afterCount.countInformation(new CountInfo(""
                        ,ChartType.Bar
                        ,GeneralVariables.getStringFromResource(R.string.count_cqzone_completed)
                        ,countValues));
            }
            if (afterCount!=null&&cqZoneCount!=0){
                ArrayList<CountValue> values=new ArrayList<>();
                values.add(new CountValue(successCount
                        ,GeneralVariables.getStringFromResource(R.string.count_completed)));
                values.add(new CountValue(cqZoneCount-successCount
                        ,GeneralVariables.getStringFromResource(R.string.count_incomplete)));
                afterCount.countInformation(new CountInfo(
                        String.format(GeneralVariables.getStringFromResource(R.string.count_total_cqzone)
                                ,cqZoneCount,successCount,100f*(float)successCount/(float) cqZoneCount)
                        ,ChartType.Pie
                        ,GeneralVariables.getStringFromResource(R.string.count_cqzone_proportion)
                        ,values));
            }


            return null;
        }
    }
    /**
     * 统计DXCC分区的数据
     */
    static class GetDxccCount extends AsyncTask<Void, Void, Void>{
        private static final String TAG="GetDxccCount";
        private final SQLiteDatabase db;
        private final AfterCount afterCount;

        class DxccInfo{
            String name;
            int dxcc;
            int count=0;

            public DxccInfo(String name, int dxcc, int count) {
                this.name = name;
                this.dxcc = dxcc;
                this.count = count;
            }
        }

        public GetDxccCount(SQLiteDatabase db, AfterCount afterCount) {
            this.db = db;
            this.afterCount = afterCount;
        }

        @SuppressLint({"Range", "DefaultLocale"})
        @Override
        protected Void doInBackground(Void... voids) {
            //DXCC的数量
            ArrayList<CountValue> dxccValues=new ArrayList<>();
            int dxccCount=0;
            Cursor cursor;
            String querySQL;
            querySQL="SELECT count(*) as c FROM dxcclist";
            cursor = db.rawQuery(querySQL,null);
            cursor.moveToFirst();
            dxccCount=cursor.getInt(cursor.getColumnIndex("c"));
            cursor.close();

            if (GeneralVariables.isChina) {
                querySQL = "SELECT dg.dxcc,count(*) as c ,dl.name as dxccName FROM   dxcc_grid dg\n" +
                        "inner join  QSLTable q \n" +
                        "on  dg.grid =UPPER(SUBSTR(q.gridsquare,1,4))  LEFT JOIN dxccList dl on dg.dxcc =dl.dxcc \n" +
                        "GROUP BY dg.dxcc ,dl.name  ORDER BY count(*) DESC";
//                querySQL="SELECT dg.dxcc,count(*) as c ,dl.name as dxccName FROM   dxcc_prefix dg\n" +
//                        "inner join  QSLTable q\n" +
//                        //"on  (q.call like (dg.prefix||'%'))\n" +
//                        "on  ((SUBSTR( q.call,1,LENGTH( dg.prefix))  = (dg.prefix))) \n"+
//                        "LEFT JOIN dxccList dl on dg.dxcc =dl.dxcc \n" +
//                        "GROUP BY dg.dxcc ,dl.name  ORDER BY count(*) DESC";
            }else {
//                querySQL="SELECT dg.dxcc,count(*) as c ,dl.aname as dxccName FROM   dxcc_prefix dg\n" +
//                        "inner join  QSLTable q\n" +
//                        "on  (q.call like (dg.prefix||'%'))\n" +
//                        "LEFT JOIN dxccList dl on dg.dxcc =dl.dxcc \n" +
//                        "GROUP BY dg.dxcc ,dl.name  ORDER BY count(*) DESC";
                querySQL = "SELECT dg.dxcc,count(*) as c ,dl.aname as dxccName FROM   dxcc_grid dg\n" +
                        "inner join  QSLTable q \n" +
                        "on  dg.grid =UPPER(SUBSTR(q.gridsquare,1,4))  LEFT JOIN dxccList dl on dg.dxcc =dl.dxcc \n" +
                        "GROUP BY dg.dxcc ,dl.aname ORDER BY count(*) DESC";

            }
            cursor = db.rawQuery(querySQL,null);
            int successCount=0;
            while (cursor.moveToNext()){
                dxccValues.add(new CountValue(cursor.getInt(cursor.getColumnIndex("c"))
                ,cursor.getString(cursor.getColumnIndex("dxccName"))));
                successCount++;
            }

            if (afterCount!=null){
                afterCount.countInformation(new CountInfo(""
                        ,ChartType.Bar
                        ,GeneralVariables.getStringFromResource(R.string.dxcc_completion_statistics)
                        ,dxccValues));
            }


            if (afterCount!=null&&dxccCount!=0){
                ArrayList<CountValue> values=new ArrayList<>();
                values.add(new CountValue(successCount
                        ,GeneralVariables.getStringFromResource(R.string.count_completed)));
                values.add(new CountValue(dxccCount-successCount
                        ,GeneralVariables.getStringFromResource(R.string.count_incomplete)));
                afterCount.countInformation(new CountInfo(
                        String.format(GeneralVariables.getStringFromResource(R.string.count_total_dxcc)
                                ,dxccCount,successCount,100f*(float)successCount/(float) dxccCount)
                        ,ChartType.Pie
                        ,GeneralVariables.getStringFromResource(R.string.count_dxcc_proportion)
                        ,values));
            }

            return null;
        }
    }




    public interface  AfterCount{
        void countInformation(CountInfo countInfo);
    }

    static public class CountInfo{
        public String title;
        public String info;
        public ArrayList<CountValue> values=null;
        public ChartType chartType;

        public CountInfo(String info,ChartType chartType,String title
                , ArrayList<CountValue> values) {
            this.info = info;
            this.values = values;
            this.title=title;
            this.chartType=chartType;
        }
    }

    static public class CountValue{
        public int value;
        public String name;

        public CountValue(int value, String name) {
            this.value = value;
            this.name = name;
        }
    }


}
