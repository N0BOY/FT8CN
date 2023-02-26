package com.bg7yoz.ft8cn.callsign;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.Nullable;

import com.bg7yoz.ft8cn.Ft8Message;
import com.bg7yoz.ft8cn.GeneralVariables;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Set;

public class CallsignDatabase extends SQLiteOpenHelper {
    private static final String TAG = "CallsignDatabase";
    @SuppressLint("StaticFieldLeak")
    private static CallsignDatabase instance;
    private final Context context;
    private SQLiteDatabase db;

    public static CallsignDatabase getInstance(@Nullable Context context, @Nullable String databaseName, int version) {
        if (instance == null) {
            instance = new CallsignDatabase(context, databaseName, null, version);
        }
        return instance;
    }


    public CallsignDatabase(@Nullable Context context, @Nullable String name
            , @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
        this.context = context;

        //链接数据库，如果实体库不存在，就会调用onCreate方法，在onCreate方法中初始化数据库
        db = this.getWritableDatabase();
    }

    public SQLiteDatabase getDb() {
        return db;
    }

    /**
     * 当实体数据库不存在时，会调用该方法。可在这个地方创建数据，并添加文件
     *
     * @param sqLiteDatabase 需要连接的数据库
     */
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        Log.d(TAG, "Create database.");
        db = sqLiteDatabase;//把数据库链接保存下来
        createTables();//创建数据表
        new InitDatabase(context, db).execute();//导入数据
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }


    private void createTables() {
        try {
            db.execSQL("CREATE TABLE countries (\n" +
                    "id INTEGER NOT NULL PRIMARY KEY,\n" +
                    "CountryNameEn TEXT,\n" +
                    "CountryNameCN TEXT,\n" +
                    "CQZone INTEGER,\n" +
                    "ITUZone INTEGER,\n" +
                    "Continent TEXT,\n" +
                    "Latitude REAL,\n" +
                    "Longitude REAL,\n" +
                    "GMT_offset REAL,\n" +
                    "DXCC TEXT)");
            db.execSQL("CREATE INDEX countries_id_IDX ON countries (id)");
            db.execSQL("CREATE TABLE callsigns (countryId INTEGER NOT NULL,callsign TEXT)");
            db.execSQL("CREATE INDEX callsigns_callsign_IDX ON callsigns (callsign)");

        } catch (Exception e) {
            Log.e(TAG, e.getMessage());

        }
    }

    //查呼号的归属地
    public void getCallsignInformation(String callsign, OnAfterQueryCallsignLocation afterQueryCallsignLocation) {
        new QueryCallsignInformation(db, callsign, afterQueryCallsignLocation).execute();
    }

    public CallsignInfo getCallInfo(String callsign) {
        return getCallsignInfo(db, callsign);
    }

    /**
     * 更新消息中的位置及经纬度信息
     *
     * @param messages 消息列表
     */
    public static void getMessagesLocation(SQLiteDatabase db, ArrayList<Ft8Message> messages) {
        for (Ft8Message msg : messages) {
            if (msg.i3==0&&msg.n3==0) continue;//如果是自由文本，就不查了
            CallsignInfo fromCallsignInfo = getCallsignInfo(db,
                    msg.callsignFrom.replace("<","").replace(">",""));
            if (fromCallsignInfo != null) {
                    msg.fromDxcc = !GeneralVariables.getDxccByPrefix(fromCallsignInfo.DXCC);
                    msg.fromItu = !GeneralVariables.getItuZoneById(fromCallsignInfo.ITUZone);
                    msg.fromCq = !GeneralVariables.getCqZoneById(fromCallsignInfo.CQZone);
                    if (GeneralVariables.isChina) {
                        msg.fromWhere = fromCallsignInfo.CountryNameCN;
                    } else {
                        msg.fromWhere = fromCallsignInfo.CountryNameEn;
                    }
                    msg.fromLatLng = new LatLng(fromCallsignInfo.Latitude, fromCallsignInfo.Longitude * -1);
            }

            if (msg.checkIsCQ() || msg.getCallsignTo().contains("...")) {//CQ就不查了
                continue;
            }

            CallsignInfo toCallsignInfo = getCallsignInfo(db,
                    msg.callsignTo.replace("<","").replace(">",""));
            if (toCallsignInfo != null) {
                msg.toDxcc = !GeneralVariables.getDxccByPrefix(toCallsignInfo.DXCC);
                msg.toItu = !GeneralVariables.getItuZoneById(toCallsignInfo.ITUZone);
                msg.toCq = !GeneralVariables.getCqZoneById(toCallsignInfo.CQZone);

                if (GeneralVariables.isChina) {
                    msg.toWhere = toCallsignInfo.CountryNameCN;
                } else {
                    msg.toWhere = toCallsignInfo.CountryNameEn;
                }
                msg.toLatLng = new LatLng(toCallsignInfo.Latitude, toCallsignInfo.Longitude*-1);
            }

        }
    }

    @SuppressLint("Range")
    private static CallsignInfo getCallsignInfo(SQLiteDatabase db, String callsign) {
        CallsignInfo callsignInfo = null;

        String querySQL = "select a.*,b.* from callsigns as a left join countries as b on a.countryId =b.id \n" +
                "WHERE (SUBSTR(?,1,LENGTH(callsign))=callsign) OR (callsign=\"=\"||?)\n" +
                "order by LENGTH(callsign) desc\n" +
                "LIMIT 1";

        Cursor cursor = db.rawQuery(querySQL, new String[]{callsign.toUpperCase(), callsign.toUpperCase()});
        if (cursor.moveToFirst()) {
            callsignInfo = new CallsignInfo(callsign.toUpperCase()
                    , cursor.getString(cursor.getColumnIndex("CountryNameEn"))
                    , cursor.getString(cursor.getColumnIndex("CountryNameCN"))
                    , cursor.getInt(cursor.getColumnIndex("CQZone"))
                    , cursor.getInt(cursor.getColumnIndex("ITUZone"))
                    , cursor.getString(cursor.getColumnIndex("Continent"))
                    , cursor.getFloat(cursor.getColumnIndex("Latitude"))
                    , cursor.getFloat(cursor.getColumnIndex("Longitude"))
                    , cursor.getFloat(cursor.getColumnIndex("GMT_offset"))
                    , cursor.getString(cursor.getColumnIndex("DXCC")));
        }
        cursor.close();
        return callsignInfo;
    }


    static class QueryCallsignInformation extends AsyncTask<Void, Void, Void> {
        private final SQLiteDatabase db;
        private final String sqlParameter;
        private final OnAfterQueryCallsignLocation afterQueryCallsignLocation;

        public QueryCallsignInformation(SQLiteDatabase db, String sqlParameter, OnAfterQueryCallsignLocation afterQueryCallsignLocation) {
            this.db = db;
            this.sqlParameter = sqlParameter;
            this.afterQueryCallsignLocation = afterQueryCallsignLocation;
        }

        @SuppressLint("Range")
        @Override
        protected Void doInBackground(Void... voids) {
            CallsignInfo callsignInfo = getCallsignInfo(db, sqlParameter);
            if (callsignInfo != null && afterQueryCallsignLocation != null) {
                afterQueryCallsignLocation.doOnAfterQueryCallsignLocation(callsignInfo);
            }
            return null;
        }
    }


    static class InitDatabase extends AsyncTask<Void, Void, Void> {
        @SuppressLint("StaticFieldLeak")
        private final Context context;
        private final SQLiteDatabase db;

        public InitDatabase(Context context, SQLiteDatabase db) {
            this.context = context;
            this.db = db;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Log.d(TAG, "开始导入呼号位置数据...");
            String insertCountriesSQL = "INSERT INTO countries (id,CountryNameEn,CountryNameCN,CQZone" +
                    ",ITUZone,Continent,Latitude,Longitude,GMT_offset,DXCC)\n" +
                    "VALUES(?,?,?,?,?,?,?,?,?,?)";

            ArrayList<CallsignInfo> callsignInfos =
                    CallsignFileOperation.getCallSingInfoFromFile(context);
            ContentValues values = new ContentValues();
            for (int i = 0; i < callsignInfos.size(); i++) {
                try {
                    //把国家和地区数据写进表中，id用于关联呼号
                    db.execSQL(insertCountriesSQL, new Object[]{
                            i,//id号
                            callsignInfos.get(i).CountryNameEn,
                            callsignInfos.get(i).CountryNameCN,
                            callsignInfos.get(i).CQZone,
                            callsignInfos.get(i).ITUZone,
                            callsignInfos.get(i).Continent,
                            callsignInfos.get(i).Latitude,
                            callsignInfos.get(i).Longitude,
                            callsignInfos.get(i).GMT_offset,
                            callsignInfos.get(i).DXCC});
                    Set<String> calls = CallsignFileOperation.getCallsigns(callsignInfos.get(i).CallSign);

                    for (String s : calls
                    ) {
                        values.put("countryId", i);
                        values.put("callsign", s);
                        db.insert("callsigns", null, values);
                        values.clear();
                    }

                } catch (Exception e) {
                    Log.e(TAG, "错误：" + e.getMessage());
                }
            }
            Log.d(TAG, "呼号位置数据导入完毕！");
            return null;
        }
    }

}
