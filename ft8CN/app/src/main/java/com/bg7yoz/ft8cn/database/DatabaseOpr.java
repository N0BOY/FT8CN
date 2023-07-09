package com.bg7yoz.ft8cn.database;
/**
 * 用于数据库操作的类。绝大多数的操作都是采用异步方式（于HTTP有关的除外）。
 * 数据库已经经历的多个版本，所以有onUpgrade方法。
 * 配置信息也保存在数据库中
 *
 * @author BGY70Z
 * @date 2023-03-20
 *
 */

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
import android.util.Log;

import com.bg7yoz.ft8cn.FT8Common;
import com.bg7yoz.ft8cn.Ft8Message;
import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.ft8signal.FT8Package;
import com.bg7yoz.ft8cn.log.OnQueryQSLCallsign;
import com.bg7yoz.ft8cn.log.OnQueryQSLRecordCallsign;
import com.bg7yoz.ft8cn.log.QSLCallsignRecord;
import com.bg7yoz.ft8cn.log.QSLRecord;
import com.bg7yoz.ft8cn.log.QSLRecordStr;
import com.bg7yoz.ft8cn.rigs.BaseRigOperation;
import com.bg7yoz.ft8cn.timer.UtcTimer;

import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class DatabaseOpr extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseOpr";
    @SuppressLint("StaticFieldLeak")
    private static DatabaseOpr instance;
    private final Context context;
    private SQLiteDatabase db;


    public static DatabaseOpr getInstance(@Nullable Context context, @Nullable String databaseName) {
        if (instance == null) {
            instance = new DatabaseOpr(context, databaseName, null, 13);
        }
        return instance;
    }

    public DatabaseOpr(@Nullable Context context, @Nullable String name,
                       @androidx.annotation.Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
        this.context = context;

        //链接数据库，如果实体库不存在，就会调用onCreate方法，在onCreate方法中初始化数据库
        db = this.getWritableDatabase();
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
        createTables(sqLiteDatabase);//创建数据表
        //创建通联日志表
        createQSLTable(sqLiteDatabase);

        //创建DXCC表
        createDxccTables(sqLiteDatabase);

        //创建ITU表
        createItuTables(sqLiteDatabase);

        //创建CQZONE表
        createCqZoneTables(sqLiteDatabase);

        //创建呼号与网格对应关系表
        createCallsignQTHTables(sqLiteDatabase);

        //创建SWL相关的表
        createSWLTables(sqLiteDatabase);


    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        //创建通联日志表 版本2
        createQSLTable(sqLiteDatabase);

        //创建DXCC表
        createDxccTables(sqLiteDatabase);

        //创建ITU表
        createItuTables(sqLiteDatabase);

        //创建CQZONE表
        createCqZoneTables(sqLiteDatabase);

        //创建呼号与网格对应关系表
        createCallsignQTHTables(sqLiteDatabase);

        //创建SWL相关的表
        createSWLTables(sqLiteDatabase);

        //删除DXCC呼号列表中的等号
        //deleteDxccPrefixEqual(sqLiteDatabase);
    }


    public SQLiteDatabase getDb() {
        return db;
    }

    private void createTables(SQLiteDatabase sqLiteDatabase) {
        try {
            //创建配置信息表
            sqLiteDatabase.execSQL("CREATE TABLE config (KeyName TEXT,Value TEXT,\n" +
                    "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT)");

            //创建关注的呼号表,UNIQUE是指内容不重复，insert OR IGNORE  into
            sqLiteDatabase.execSQL("CREATE TABLE followCallsigns (callsign  TEXT UNIQUE)");

        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    /**
     * 给表添加列
     *
     * @param db        数据库
     * @param tableName 表名
     * @param fieldName 列名
     * @param sql       列的语句
     */
    private void alterTable(SQLiteDatabase db, String tableName, String fieldName, String sql) {
        Cursor cursor = db.rawQuery("select * from sqlite_master where name=? and sql like ?"
                , new String[]{tableName, "%" + fieldName + "%"});
        if (!cursor.moveToNext()) {
            db.execSQL(String.format("ALTER TABLE %s ADD COLUMN %s", tableName, sql));
        }
        cursor.close();
    }

    /**
     * 检查表是不是存在
     *
     * @param db        数据库
     * @param tableName 表名
     * @return 是否存在
     */
    private boolean checkTableExists(SQLiteDatabase db, String tableName) {
        Cursor cursor = db.rawQuery("select * from sqlite_master where type = 'table' and name = ?"
                , new String[]{tableName});
        if (cursor.moveToNext()) {
            cursor.close();
            return true;
        }
        return false;
    }

    private void deleteDxccPrefixEqual(SQLiteDatabase db) {
        db.execSQL("DELETE from dxcc_prefix where prefix LIKE \"=%\"");
    }

    /**
     * 创建通联日志表
     */
    private void createQSLTable(SQLiteDatabase sqLiteDatabase) {
        if (checkTableExists(sqLiteDatabase, "QSLTable")) {
            alterTable(sqLiteDatabase, "QSLTable", "isQSL"
                    , "isQSL INTEGER DEFAULT 0");
            alterTable(sqLiteDatabase, "QSLTable", "isLotW_import"
                    , "isLotW_import INTEGER DEFAULT 0");
            alterTable(sqLiteDatabase, "QSLTable", "isLotW_QSL"
                    , "isLotW_QSL INTEGER DEFAULT 0");

        } else {
            sqLiteDatabase.execSQL("CREATE TABLE QSLTable (\n" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                    "isQSL INTEGER DEFAULT 0,\n" +//是否确认QSL
                    "isLotW_import INTEGER DEFAULT 0,\n" +//是否是lotw导入
                    "isLotW_QSL INTEGER DEFAULT 0,\n" +


                    "call TEXT,\n" +
                    "gridsquare TEXT,\n" +
                    "mode TEXT,\n" +
                    "rst_sent TEXT,\n" +
                    "rst_rcvd TEXT,\n" +
                    "qso_date TEXT,\n" +
                    "time_on TEXT,\n" +
                    "qso_date_off TEXT,\n" +
                    "time_off TEXT,\n" +
                    "band TEXT,\n" +
                    "freq TEXT,\n" +
                    "station_callsign TEXT,\n" +
                    "my_gridsquare TEXT,\n" +
                    "comment TEXT)");
        }


        if (checkTableExists(sqLiteDatabase, "QslCallsigns")) {
            alterTable(sqLiteDatabase, "QslCallsigns", "isQSL"
                    , "isQSL INTEGER DEFAULT 0");
            alterTable(sqLiteDatabase, "QslCallsigns", "isLotW_import"
                    , "isLotW_import INTEGER DEFAULT 0");
            alterTable(sqLiteDatabase, "QslCallsigns", "isLotW_QSL"
                    , "isLotW_QSL INTEGER DEFAULT 0");
            alterTable(sqLiteDatabase, "QslCallsigns", "startTime"
                    , "startTime TEXT DEFAULT \"0\"");
        } else {
            sqLiteDatabase.execSQL("CREATE TABLE QslCallsigns (" +
                    "ID INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                    "isQSL INTEGER DEFAULT 0,\n" +
                    "isLotW_import INTEGER DEFAULT 0,\n" +
                    "isLotW_QSL INTEGER DEFAULT 0,\n" +

                    "callsign TEXT, startTime TEXT," +
                    "finishTime TEXT, mode TEXT," +
                    "grid TEXT,\n" +
                    "band TEXT,band_i INTEGER)");
        }

        if (!checkTableExists(sqLiteDatabase, "Messages")) {
            sqLiteDatabase.execSQL("CREATE TABLE Messages (\n" +
                    "ID INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                    "I3 INTEGER,\n" +
                    "N3 INTEGER,\n" +
                    "Protocol TEXT,\n" +
                    "UTC INTEGER,\n" +
                    "SNR INTEGER,\n" +
                    "TIME_SEC REAL,\n" +
                    "FREQ INTEGER,\n" +
                    "CALL_TO TEXT,\n" +
                    "CALL_FROM TEXT,\n" +
                    "EXTRAL TEXT,\n" +
                    "REPORT INTEGER,\n" +
                    "BAND INTEGER)");
        }
    }


    /**
     * 创建与DXCC有关的数据表：dxccList,dxcc_prefix,dxcc_grid
     */
    private void createDxccTables(SQLiteDatabase sqLiteDatabase) {
        if (!checkTableExists(sqLiteDatabase, "dxccList")) {
            sqLiteDatabase.execSQL("CREATE TABLE dxccList (\n" +
                    "id INTEGER ," +
                    "\tdxcc INTEGER,\n" +
                    "\tcc TEXT,\n" +
                    "\tccc TEXT,\n" +
                    "\tname TEXT,\n" +
                    "\tcontinent TEXT,\n" +
                    "\tituzone TEXT,\n" +
                    "\tcqzone TEXT,\n" +
                    "\ttimezone INTEGER,\n" +
                    "\tccode INTEGER,\n" +
                    "\taname TEXT,\n" +
                    "\tpp TEXT,\n" +
                    "\tlat REAL,\n" +
                    "\tlon REAL\n" +
                    ");");

            sqLiteDatabase.execSQL("CREATE TABLE dxcc_prefix (\n" +
                    "\tdxcc INTEGER,\n" +
                    "\tprefix TEXT\n" +
                    ");");

            sqLiteDatabase.execSQL("CREATE TABLE dxcc_grid (\n" +
                    "\tdxcc INTEGER,\n" +
                    "\tgrid TEXT\n" +
                    ");");


            //把DXCC对应表数据导入到数据库中
            new Thread(new Runnable() {
                @Override
                public void run() {
                    ArrayList<DxccObject> dxccObjects = loadDxccDataFromFile();
                    for (DxccObject obj : dxccObjects) {
                        obj.insertToDb(sqLiteDatabase);
                    }
                }
            }).start();
        }

    }

    /**
     * 把ITU分区的对应表导入数据库
     *
     * @param sqLiteDatabase 数据库
     */
    private void createItuTables(SQLiteDatabase sqLiteDatabase) {
        if (!checkTableExists(sqLiteDatabase, "ituList")) {
            sqLiteDatabase.execSQL("CREATE TABLE ituList (itu INTEGER,grid TEXT)");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    loadItuDataFromFile(sqLiteDatabase);
                }
            }).start();
        }
    }

    private void createCqZoneTables(SQLiteDatabase sqLiteDatabase) {
        if (!checkTableExists(sqLiteDatabase, "cqzoneList")) {
            sqLiteDatabase.execSQL("CREATE TABLE cqzoneList (cqzone INTEGER,grid TEXT)");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    loadICqZoneDataFromFile(sqLiteDatabase);
                }
            }).start();
        }
    }

    /**
     * 创建呼号与网格对应关系表
     *
     * @param sqLiteDatabase db
     */
    private void createCallsignQTHTables(SQLiteDatabase sqLiteDatabase) {
        if (!checkTableExists(sqLiteDatabase, "CallsignQTH")) {
            sqLiteDatabase.execSQL("CREATE TABLE CallsignQTH(callsign text, grid text" +
                    ",updateTime Int ,PRIMARY KEY(callsign))");
        }
    }

    private void createSWLTables(SQLiteDatabase sqLiteDatabase) {
        if (!checkTableExists(sqLiteDatabase, "SWLMessages")) {
            sqLiteDatabase.execSQL("CREATE TABLE SWLMessages (\n" +
                    "\tID INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                    "\tI3 INTEGER,\n" +
                    "\tN3 INTEGER,\n" +
                    "\tProtocol TEXT,\n" +
                    "\tUTC TEXT,\n" +
                    "\tSNR INTEGER,\n" +
                    "\tTIME_SEC REAL,\n" +
                    "\tFREQ INTEGER,\n" +
                    "\tCALL_TO TEXT,\n" +
                    "\tCALL_FROM TEXT,\n" +
                    "\tEXTRAL TEXT,\n" +
                    "\tREPORT INTEGER,\n" +
                    "\tBAND INTEGER\n" +
                    ")");
            sqLiteDatabase.execSQL("CREATE INDEX SWLMessages_CALL_TO_IDX " +
                    "ON SWLMessages (CALL_TO,CALL_FROM)");
            sqLiteDatabase.execSQL("CREATE INDEX SWLMessages_UTC_IDX ON SWLMessages (UTC)");
        }
        if (!checkTableExists(sqLiteDatabase, "SWLQSOTable")) {
            sqLiteDatabase.execSQL("CREATE TABLE SWLQSOTable (\n" +
                    "\tid INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                    "\t\"call\" TEXT,\n" +
                    "\tgridsquare TEXT,\n" +
                    "\tmode TEXT,\n" +
                    "\trst_sent TEXT,\n" +
                    "\trst_rcvd TEXT,\n" +
                    "\tqso_date TEXT,\n" +
                    "\ttime_on TEXT,\n" +
                    "\tqso_date_off TEXT,\n" +
                    "\ttime_off TEXT,\n" +
                    "\tband TEXT,\n" +
                    "\tfreq TEXT,\n" +
                    "\tstation_callsign TEXT,\n" +
                    "\tmy_gridsquare TEXT,\n" +
                    "\tcomment TEXT)");
        }
    }


    public void loadItuDataFromFile(SQLiteDatabase db) {
        AssetManager assetManager = context.getAssets();
        InputStream inputStream;
        db.execSQL("delete from ituList");

        String insertSQL = "INSERT INTO ituList (itu,grid)" +
                "VALUES(?,?)";
        try {
            inputStream = assetManager.open("ituzone.json");
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes);
            JSONObject jsonObject = new JSONObject(new String(bytes));
            JSONArray array = jsonObject.names();
            for (int i = 0; i < array.length(); i++) {
                JSONObject ituObject = new JSONObject(jsonObject.getString(array.getString(i)));
                JSONArray mh = ituObject.getJSONArray("mh");
                for (int j = 0; j < mh.length(); j++) {
                    db.execSQL(insertSQL, new Object[]{array.getString(i), mh.getString(j)});
                }
            }
            inputStream.close();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "loadDataFromFile: " + e.getMessage());
        }
    }

    public void loadICqZoneDataFromFile(SQLiteDatabase db) {
        AssetManager assetManager = context.getAssets();
        InputStream inputStream;
        db.execSQL("delete from cqzoneList");
        String insertSQL = "INSERT INTO cqzoneList (cqzone,grid)" +
                "VALUES(?,?)";
        try {
            inputStream = assetManager.open("cqzone.json");
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes);
            JSONObject jsonObject = new JSONObject(new String(bytes));
            JSONArray array = jsonObject.names();
            for (int i = 0; i < array.length(); i++) {
                JSONObject ituObject = new JSONObject(jsonObject.getString(array.getString(i)));
                JSONArray mh = ituObject.getJSONArray("mh");
                for (int j = 0; j < mh.length(); j++) {
                    db.execSQL(insertSQL, new Object[]{array.getString(i), mh.getString(j)});
                }
            }
            inputStream.close();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "loadDataFromFile: " + e.getMessage());
        }
    }


    public ArrayList<DxccObject> loadDxccDataFromFile() {
        AssetManager assetManager = context.getAssets();
        InputStream inputStream;
        ArrayList<DxccObject> dxccObjects = new ArrayList<>();
        try {
            inputStream = assetManager.open("dxcc_list.json");
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes);
            JSONObject jsonObject = new JSONObject(new String(bytes));
            JSONArray array = jsonObject.names();

            for (int i = 0; i < array.length(); i++) {
                if (array.getString(i).equals("-1")) continue;
                JSONObject dxccObject = new JSONObject(jsonObject.getString(array.getString(i)));
                DxccObject dxcc = new DxccObject();
                dxcc.id = Integer.parseInt(array.getString(i));
                dxcc.dxcc = dxccObject.getInt("dxcc");
                dxcc.cc = dxccObject.getString("cc");
                dxcc.ccc = dxccObject.getString("ccc");
                dxcc.name = dxccObject.getString("name");
                dxcc.continent = dxccObject.getString("continent");
                dxcc.ituZone = dxccObject.getString("ituzone")
                        .replace("[", "")
                        .replace("]", "")
                        .replace("\"", "");
                dxcc.cqZone = dxccObject.getString("cqzone")
                        .replace("[", "")
                        .replace("]", "")
                        .replace("\"", "");
                dxcc.timeZone = dxccObject.getInt("timezone");
                dxcc.cCode = dxccObject.getInt("ccode");
                dxcc.aName = dxccObject.getString("aname");
                dxcc.pp = dxccObject.getString("pp");
                dxcc.lat = dxccObject.getDouble("lat");
                dxcc.lon = dxccObject.getDouble("lon");

                JSONArray mh = dxccObject.getJSONArray("mh");
                for (int j = 0; j < mh.length(); j++) {
                    dxcc.grid.add(mh.getString(j));
                }
                JSONArray prefix = dxccObject.getJSONArray("prefix");
                for (int j = 0; j < prefix.length(); j++) {
                    dxcc.prefix.add(prefix.getString(j));
                }
                dxccObjects.add(dxcc);
                //Log.e(TAG, "loadDataFromFile: id:" + dxcc.id + " dxcc:" + dxcc.dxcc);
            }

            inputStream.close();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "loadDataFromFile: " + e.getMessage());
        }
        return dxccObjects;
    }


    /**
     * 把呼号和网格对应关系写入表中
     *
     * @param callsign 呼号
     * @param grid     网格
     */
    public void addCallsignQTH(String callsign, String grid) {
        if (grid.trim().length() < 4) return;
        new AddCallsignQTH(db).execute(callsign, grid);
        Log.d(TAG, String.format("addCallsignQTH: callsign:%s,grid:%s", callsign, grid));
    }

    //查询配置信息。
    public void getConfigByKey(String KeyName, OnAfterQueryConfig onAfterQueryConfig) {
        new QueryConfig(db, KeyName, onAfterQueryConfig).execute();
    }

    public void getCallSign(String callsign, String fieldName, String tableName, OnGetCallsign getCallsign) {
        new QueryCallsign(db, tableName, fieldName, callsign, getCallsign).execute();
    }

    /**
     * 写配置信息，异步操作
     */
    public void writeConfig(String KeyName, String Value, OnAfterWriteConfig onAfterWriteConfig) {
        Log.d(TAG, "writeConfig: Value:" + Value);
        new WriteConfig(db, KeyName, Value, onAfterWriteConfig).execute();
    }

    public void writeMessage(ArrayList<Ft8Message> messages) {
        new WriteMessages(db, messages).execute();
    }

    /**
     * 读取关注的呼号列表
     *
     * @param onAffterQueryFollowCallsigns 回调函数
     */
    public void getFollowCallsigns(OnAfterQueryFollowCallsigns onAffterQueryFollowCallsigns) {
        new GetFollowCallSigns(db, onAffterQueryFollowCallsigns).execute();
    }

    /**
     * 查询SWL MESSAGE各BAND的数量
     * @param onAfterQueryFollowCallsigns 回调
     */
    public void getMessageLogTotal(OnAfterQueryFollowCallsigns onAfterQueryFollowCallsigns) {
        new GetMessageLogTotal(db, onAfterQueryFollowCallsigns).execute();
    }

    /**
     * 查询SWL QSO的在各个月的数量
     * @param onAfterQueryFollowCallsigns 回调
     */
    public void getSWLQsoLogTotal(OnAfterQueryFollowCallsigns onAfterQueryFollowCallsigns) {
        new GetSWLQsoTotal(db, onAfterQueryFollowCallsigns).execute();
    }


    /**
     * 向数据库中添加关注的呼号
     *
     * @param callsign 呼号
     */
    public void addFollowCallsign(String callsign) {
        new AddFollowCallSign(db, callsign).execute();
    }

    /**
     * 清空关注的呼号
     */
    public void clearFollowCallsigns() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                db.execSQL("delete from followCallsigns ");
            }
        }).start();
    }

    /**
     * 删除通联的日志
     */
    public void clearLogCacheData() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                db.execSQL("delete from SWLMessages ");
            }
        }).start();
    }

    /**
     * 删除SWL QSO日志
     */
    public void clearSWLQsoData() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                db.execSQL("delete from SWLQSOTable ");
            }
        }).start();
    }
    /**
     * 把通联成功的日志和呼号写到数据库中
     *
     * @param qslRecord 通联记录
     */
    public void addQSL_Callsign(QSLRecord qslRecord) {
        new AddQSL_Info(this, qslRecord).execute();
    }

    /**
     * 把SWL的QSO保存到数据库，SWL的QSO标准：至少要有双方的信号报告。不包含自己的呼号。
     * @param qslRecord 通联日志记录
     */
    public void addSWL_QSO(QSLRecord qslRecord) {
        new Add_SWL_QSO_Info(this, qslRecord).execute();
    }

    //删除数据库中关注的呼号
    public void deleteFollowCallsign(String callsign) {
        new DeleteFollowCallsign(db, callsign).execute();
    }

    //获取所有配置参数
    public void getAllConfigParameter(OnAfterQueryConfig onAfterQueryConfig) {
        new GetAllConfigParameter(db, onAfterQueryConfig).execute();
    }

    /**
     * 查询全部成功通联的呼号，能通联的频率为条件
     */
    public void getAllQSLCallsigns() {
        new LoadAllQSLCallsigns(db).execute();
    }


    /**
     * 按呼号查找QSL的呼号记录
     *
     * @param callsign           呼号
     * @param onQueryQSLCallsign 回调
     */
    public void getQSLCallsignsByCallsign(boolean showAll,int offset,String callsign, int filter, OnQueryQSLCallsign onQueryQSLCallsign) {
        new GetQLSCallsignByCallsign(showAll,offset,db, callsign, filter, onQueryQSLCallsign).execute();
    }

    /**
     * 查询已经QSO的网格，这个主要用在GridTracker上
     * 可以知道哪些网格是QSO，哪些是QSL
     *
     * @param onGetQsoGrids 当查询结束之后的事件。
     */
    public void getQsoGridQuery(OnGetQsoGrids onGetQsoGrids) {
        new GetQsoGrids(db, onGetQsoGrids).execute();
    }

    /**
     * 按呼号查询QSL记录
     *
     * @param callsign                 呼号
     * @param onQueryQSLRecordCallsign 回调
     */
    public void getQSLRecordByCallsign(boolean showAll,int offset,String callsign, int filter, OnQueryQSLRecordCallsign onQueryQSLRecordCallsign) {
        new GetQSLByCallsign(showAll,offset,db, callsign, filter, onQueryQSLRecordCallsign).execute();
    }

    /**
     * 删除通联呼号
     *
     * @param id id号
     */
    public void deleteQSLCallsign(int id) {
        new DeleteQSLCallsignByID(db, id).execute();
    }

    /**
     * 删除日志
     *
     * @param id id号
     */
    public void deleteQSLByID(int id) {
        new DeleteQSLByID(db, id).execute();
    }

    /**
     * 修改日志的手工确认
     *
     * @param isQSL 是否确认
     * @param id    ID号
     */
    public void setQSLTableIsQSL(boolean isQSL, int id) {
        new SetQSLTableIsQSL(db, id, isQSL).execute();
    }

    public void setQSLCallsignIsQSL(boolean isQSL, int id) {
        new SetQSLCallsignIsQSL(db, id, isQSL).execute();
    }

    /**
     * 到数据库中查呼号和网格的对应关系，查出后，会把数据写入到GeneralVariables的callsignAndGrids中
     *
     * @param callsign 呼号
     */
    public void getCallsignQTH(String callsign) {
        new GetCallsignQTH(db).execute(callsign);
    }

    /**
     * 把已经通联的DXCC分区列出来
     */
    @SuppressLint("Range")
    public void getQslDxccToMap() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String querySQL;
                Cursor cursor;
                Log.d(TAG, "run: 开始导入分区...");

                //导入已经通联的dxcc
                querySQL = "SELECT DISTINCT dl.pp FROM   dxcc_grid dg\n" +
                        "inner join  QSLTable q\n" +
                        "on  dg.grid =UPPER(SUBSTR(q.gridsquare,1,4))  LEFT JOIN dxccList dl on dg.dxcc =dl.dxcc";
                cursor = db.rawQuery(querySQL, null);
                while (cursor.moveToNext()) {
                    GeneralVariables.addDxcc(cursor.getString(cursor.getColumnIndex("pp")));
                }
                cursor.close();

                //导入已经通联的CQ分区
                querySQL = "SELECT DISTINCT  cl.cqzone  as cq FROM   cqzoneList cl\n" +
                        "inner join  QSLTable q\n" +
                        "on  cl.grid =UPPER(SUBSTR(q.gridsquare,1,4)) ";
                cursor = db.rawQuery(querySQL, null);
                while (cursor.moveToNext()) {
                    GeneralVariables.addCqZone(cursor.getInt(cursor.getColumnIndex("cq")));
                }
                cursor.close();

                //导入已经通联的itu分区
                querySQL = "SELECT DISTINCT il.itu   FROM   ituList il\n" +
                        "inner join  QSLTable q\n" +
                        "on  il.grid =UPPER(SUBSTR(q.gridsquare,1,4))";
                cursor = db.rawQuery(querySQL, null);
                while (cursor.moveToNext()) {
                    GeneralVariables.addItuZone(cursor.getInt(cursor.getColumnIndex("itu")));
                }
                cursor.close();

                Log.d(TAG, "run: 分区导入完毕...");
            }
        }).start();

    }


    /**
     * 检查通联的呼号是不是存在，如果存在，返回TRUE，并且更新isLotW_QSL，
     *
     * @param record 记录
     * @return 是否存在
     */
    @SuppressLint("Range")
    public boolean checkQSLCallsign(QSLRecord record) {
        QSLRecord newRecord = record;
        newRecord.id = -1;
        //检查是不是已经存在呼号了
        String querySQL = "select * from QslCallsigns WHERE (callsign=?)" +
                "and (startTime=?) and(finishTime=?)" +
                "and(mode=?)";

        Cursor cursor = db.rawQuery(querySQL, new String[]{
                record.getToCallsign()
                , record.getStartTime()
                , record.getEndTime()
                , record.getMode()});
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            newRecord.isLotW_QSL = cursor.getInt(cursor.getColumnIndex("isLotW_QSL")) == 1
                    || record.isLotW_QSL;
            newRecord.id = cursor.getLong(cursor.getColumnIndex("ID"));
        }
        cursor.close();
//        if (newRecord.id != -1) {//说明已经存在记录了
//            querySQL = "UPDATE   QslCallsigns set isLotW_QSL=? WHERE ID=?";
//            db.execSQL(querySQL, new Object[]{newRecord.isLotW_QSL ? "1" : "0", newRecord.id});
//        }
        return newRecord.id != -1;//
    }

    @SuppressLint("Range")
    public boolean checkIsQSL(QSLRecord record) {
        QSLRecord newRecord = record;
        newRecord.id = -1;
        //检查是不是已经存在日志记录了
        String querySQL = "select * from QSLTable WHERE (call=?)" +
                "and (qso_date=?) and(time_on=?)" +
                "and(mode=?)";

        Cursor cursor = db.rawQuery(querySQL, new String[]{
                record.getToCallsign()
                , record.getQso_date()
                , record.getTime_on()
                , record.getMode()});
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            newRecord.isLotW_QSL = cursor.getInt(cursor.getColumnIndex("isLotW_QSL")) == 1
                    || record.isLotW_QSL;
            newRecord.id = cursor.getLong(cursor.getColumnIndex("id"));
        }
        cursor.close();

//        if (newRecord.id != -1) {//说明已经存在记录了
//            querySQL = "UPDATE   QSLTable set isLotW_QSL=? WHERE ID=?";
//            db.execSQL(querySQL, new Object[]{newRecord.isLotW_QSL ? "1" : "0", newRecord.id});
//        }
        return newRecord.id != -1;//
    }

    @SuppressLint("Range")
    public boolean doInsertQSLData(QSLRecord record) {
        if (record.getToCallsign() == null) {
            return false;
        }

        String querySQL;
        if (!checkQSLCallsign(record)) {//如果不存在记录，就添加
            querySQL = "INSERT INTO  QslCallsigns (callsign" +
                    ",isQSL,isLotW_import,isLotW_QSL" +
                    ",startTime,finishTime,mode,grid,band,band_i)" +
                    "values(?,?,?,?,?,?,?,?,?,?)";
            db.execSQL(querySQL, new Object[]{record.getToCallsign()
                    , record.isQSL ? 1 : 0//是否手工确认
                    , record.isLotW_import ? 1 : 0//是否lotw导入
                    , record.isLotW_QSL ? 1 : 0//是否lotw确认
                    , record.getStartTime()
                    , record.getEndTime()
                    , record.getMode()
                    , record.getToMaidenGrid()
                    , BaseRigOperation.getFrequencyAllInfo(record.getBandFreq())
                    , record.getBandFreq()});
        } else {
            if (record.isQSL) {
                db.execSQL("UPDATE  QslCallsigns  SET isQSL=? " +
                                "WHERE  (callsign=?)AND(startTime=?)AND(finishTime=?)AND(mode=?)"
                        , new Object[]{1, record.getToCallsign(), record.getStartTime()
                                , record.getEndTime(), record.getMode()});
            }
            if (record.isLotW_import) {
                db.execSQL("UPDATE  QslCallsigns  SET isLotW_import=? " +
                                "WHERE  (callsign=?)AND(startTime=?)AND(finishTime=?)AND(mode=?)"
                        , new Object[]{1, record.getToCallsign(), record.getStartTime()
                                , record.getEndTime(), record.getMode()});
            }

            if (record.isLotW_QSL) {
                db.execSQL("UPDATE  QslCallsigns  SET isLotW_QSL=? " +
                                "WHERE  (callsign=?)AND(startTime=?)AND(finishTime=?)AND(mode=?)"
                        , new Object[]{1, record.getToCallsign(), record.getStartTime()
                                , record.getEndTime(), record.getMode()});
            }
            if (record.getToMaidenGrid().length() >= 4) {
                db.execSQL("UPDATE  QslCallsigns  SET grid=? " +
                                "WHERE  (callsign=?)AND(startTime=?)AND(finishTime=?)AND(mode=?)"
                        , new Object[]{record.getToMaidenGrid(), record.getToCallsign(), record.getStartTime()
                                , record.getEndTime(), record.getMode()});
            }

        }


        if (!checkIsQSL(record)) {//如果不存在日志数据就添加
            querySQL = "INSERT INTO QSLTable(call, isQSL,isLotW_import,isLotW_QSL,gridsquare, mode, rst_sent, rst_rcvd, qso_date, " +
                    "time_on, qso_date_off, time_off, band, freq, station_callsign, my_gridsquare," +
                    "comment)VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

            db.execSQL(querySQL, new String[]{record.getToCallsign()
                    , String.valueOf(record.isQSL ? 1 : 0)
                    , String.valueOf(record.isLotW_import ? 1 : 0)
                    , String.valueOf(record.isLotW_QSL ? 1 : 0)
                    , record.getToMaidenGrid()
                    , record.getMode()
                    , String.valueOf(record.getSendReport())
                    , String.valueOf(record.getReceivedReport())
                    , record.getQso_date()
                    , record.getTime_on()

                    , record.getQso_date_off()
                    , record.getTime_off()
                    , record.getBandLength()//波长//RigOperationConstant.getMeterFromFreq(qslRecord.getBandFreq())
                    , BaseRigOperation.getFrequencyFloat(record.getBandFreq())
                    , record.getMyCallsign()
                    , record.getMyMaidenGrid()
                    , record.getComment()});
        } else {
            if (record.isQSL) {
                db.execSQL("UPDATE  QSLTable  SET isQSL=? " +
                                " WHERE (call=?) and (qso_date=?) and(time_on=?) and(mode=?)"
                        , new Object[]{1, record.getToCallsign()
                                , record.getQso_date()
                                , record.getTime_on()
                                , record.getMode()});
            }
            if (record.isLotW_import) {
                db.execSQL("UPDATE  QSLTable  SET isLotW_import=? " +
                                " WHERE (call=?) and (qso_date=?) and(time_on=?) and(mode=?)"
                        , new Object[]{1, record.getToCallsign()
                                , record.getQso_date()
                                , record.getTime_on()
                                , record.getMode()});
            }
            if (record.isLotW_QSL) {
                db.execSQL("UPDATE  QSLTable  SET isLotW_QSL=? " +
                                " WHERE (call=?) and (qso_date=?) and(time_on=?) and(mode=?)"
                        , new Object[]{1, record.getToCallsign()
                                , record.getQso_date()
                                , record.getTime_on()
                                , record.getMode()});
            }
            if (record.getToMaidenGrid().length() >= 4) {
                db.execSQL("UPDATE  QSLTable  SET gridsquare=? " +
                                " WHERE (call=?) and (qso_date=?) and(time_on=?) and(mode=?)"
                        , new Object[]{record.getToMaidenGrid(), record.getToCallsign()
                                , record.getQso_date()
                                , record.getTime_on()
                                , record.getMode()});
            }
            if (record.getMyMaidenGrid().length() >= 4) {
                db.execSQL("UPDATE  QSLTable  SET my_gridsquare=? " +
                                " WHERE (call=?) and (qso_date=?) and(time_on=?) and(mode=?)"
                        , new Object[]{record.getMyMaidenGrid(), record.getToCallsign()
                                , record.getQso_date()
                                , record.getTime_on()
                                , record.getMode()});
            }
            if (record.getSendReport() > -100) {
                db.execSQL("UPDATE  QSLTable  SET rst_sent=? " +
                                " WHERE (call=?) and (qso_date=?) and(time_on=?) and(mode=?)"
                        , new Object[]{record.getSendReport(), record.getToCallsign()
                                , record.getQso_date()
                                , record.getTime_on()
                                , record.getMode()});
            }
            if (record.getReceivedReport() > -100) {
                db.execSQL("UPDATE  QSLTable  SET rst_rcvd=? " +
                                " WHERE (call=?) and (qso_date=?) and(time_on=?) and(mode=?)"
                        , new Object[]{record.getReceivedReport(), record.getToCallsign()
                                , record.getQso_date()
                                , record.getTime_on()
                                , record.getMode()});
            }
        }
        return true;
    }


    /**
     * 查询配置信息的类
     */
    static class QueryConfig extends AsyncTask<Void, Void, Void> {
        private final SQLiteDatabase db;
        private final String KeyName;
        private final OnAfterQueryConfig afterQueryConfig;

        public QueryConfig(SQLiteDatabase db, String keyName, OnAfterQueryConfig afterQueryConfig) {
            this.db = db;
            KeyName = keyName;
            this.afterQueryConfig = afterQueryConfig;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (afterQueryConfig != null) {
                afterQueryConfig.doOnBeforeQueryConfig(KeyName);
            }
        }

        @SuppressLint("Range")
        @Override
        protected Void doInBackground(Void... voids) {
            String querySQL = "select keyName,Value from config where KeyName =?";
            Cursor cursor = db.rawQuery(querySQL, new String[]{KeyName.toString()});
            if (cursor.moveToFirst()) {
                if (afterQueryConfig != null) {
                    afterQueryConfig.doOnAfterQueryConfig(KeyName, cursor.getString(cursor.getColumnIndex("Value")));
                }
            } else {
                if (afterQueryConfig != null) {
                    afterQueryConfig.doOnAfterQueryConfig(KeyName, "");
                }
            }
            cursor.close();
            return null;
        }
    }

    static class QueryCallsign extends AsyncTask<Void, Void, Void> {
        private final SQLiteDatabase db;
        private final String tableName;
        private final String fieldName;
        private final String callSign;
        private OnGetCallsign onGetCallsign;

        public QueryCallsign(SQLiteDatabase db, String tableName, String fieldName
                , String callSign, OnGetCallsign onGetCallsign) {
            this.db = db;
            this.tableName = tableName;
            this.fieldName = fieldName;
            this.callSign = callSign;
            this.onGetCallsign = onGetCallsign;
        }

        @SuppressLint("Range")
        @Override
        protected Void doInBackground(Void... voids) {
            String sql = String.format("select count(%s) as a FROM %s where %s=\"%s\" limit 1"
                    , fieldName, tableName, fieldName, callSign);
            Cursor cursor = db.rawQuery(sql, null);
            if (cursor.moveToFirst()) {
                if (onGetCallsign != null) {
                    onGetCallsign.doOnAfterGetCallSign(cursor.getInt(cursor.getColumnIndex("a")) > 0);
                }
            } else {
                if (onGetCallsign != null) {
                    onGetCallsign.doOnAfterGetCallSign(false);
                }

            }
            cursor.close();
            return null;
        }
    }

    /**
     * 写配置信息的类
     */
    static class WriteConfig extends AsyncTask<Void, Void, Void> {
        private final SQLiteDatabase db;
        private final String KeyName;
        private final String Value;
        private final OnAfterWriteConfig afterWriteConfig;

        public WriteConfig(SQLiteDatabase db, String keyName, String Value, OnAfterWriteConfig afterWriteConfig) {
            this.db = db;
            this.KeyName = keyName;
            this.afterWriteConfig = afterWriteConfig;
            this.Value = Value;
        }

        @SuppressLint("Range")
        @Override
        protected Void doInBackground(Void... voids) {
            String querySQL = "DELETE FROM config where KeyName =?";
            db.execSQL(querySQL, new String[]{KeyName.toString()});
            querySQL = "INSERT INTO config (KeyName,Value)Values(?,?)";
            db.execSQL(querySQL, new String[]{KeyName.toString(), Value.toString()});
            if (afterWriteConfig != null) {
                afterWriteConfig.doOnAfterWriteConfig(true);
            }
            return null;
        }
    }

    /**
     * 把消息写到数据库
     */
    static class WriteMessages extends AsyncTask<Void, Void, Void> {
        private final SQLiteDatabase db;
        private ArrayList<Ft8Message> messages;

        public WriteMessages(SQLiteDatabase db, ArrayList<Ft8Message> messages) {
            this.db = db;
            this.messages = messages;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            String sql = "INSERT INTO SWLMessages(I3,N3,Protocol,UTC,SNR,TIME_SEC,FREQ,CALL_FROM" +
                    ",CALL_TO,EXTRAL,REPORT,BAND)\n" +
                    "VALUES(?,?,?,?,?,?,?,?,?,?,?,?)";
            for (Ft8Message message : messages) {//只对与我有关的消息做保存
                db.execSQL(sql, new Object[]{message.i3, message.n3, "FT8"
                        ,UtcTimer.getDatetimeYYYYMMDD_HHMMSS(message.utcTime)
                        , message.snr, message.time_sec, Math.round(message.freq_hz)
                        , message.callsignFrom, message.callsignTo, message.extraInfo
                        , message.report, message.band});

            }
            return null;
        }
    }

    /**
     * 把关注的呼号写到数据库
     */
    static class AddFollowCallSign extends AsyncTask<Void, Void, Void> {
        private final SQLiteDatabase db;
        private final String callSign;

        public AddFollowCallSign(SQLiteDatabase db, String callSign) {
            this.db = db;
            this.callSign = callSign;
        }

        @SuppressLint("Range")
        @Override
        protected Void doInBackground(Void... voids) {
            String querySQL = "INSERT OR IGNORE INTO  followCallsigns (callsign)values(?)";
            db.execSQL(querySQL, new String[]{callSign});
            return null;
        }
    }

    /**
     * 向呼号网格对应表中写数据，AsyncTask中的String，是多参数，以数组形式给doInBackground
     * 所以，写入数据第一个元素是呼号，第二个是网格
     */
    static class AddCallsignQTH extends AsyncTask<String, Void, Void> {
        private final SQLiteDatabase db;

        public AddCallsignQTH(SQLiteDatabase db) {
            this.db = db;
        }

        @Override
        protected Void doInBackground(String... strings) {
            if (strings.length == 2) {
                String querySQL = "INSERT OR REPLACE  INTO  CallsignQTH  (callsign,grid,updateTime)" +
                        "VALUES (Upper(?),?,?)";
                db.execSQL(querySQL, new Object[]{strings[0], strings[1], System.currentTimeMillis()});
            }
            return null;
        }
    }

    static class Add_SWL_QSO_Info extends AsyncTask<Void, Void, Void>{
        private final DatabaseOpr databaseOpr;
        private QSLRecord qslRecord;
        public Add_SWL_QSO_Info(DatabaseOpr opr, QSLRecord qslRecord) {
            this.databaseOpr = opr;
            this.qslRecord = qslRecord;
        }
        @SuppressLint("Range")
        @Override
        protected Void doInBackground(Void... voids) {
            String querySQL;
            //删除之前重复的记录
            querySQL = "DELETE FROM  SWLQSOTable where ([call]=?) and (station_callsign=?) and (qso_date=?) and(time_on=?) and (freq=?)";
            databaseOpr.db.execSQL(querySQL, new String[]{
                             qslRecord.getToCallsign()
                            , qslRecord.getMyCallsign()
                            , qslRecord.getQso_date()
                            , qslRecord.getTime_on()
                            , BaseRigOperation.getFrequencyFloat(qslRecord.getBandFreq())
                    });
            //添加记录
            querySQL = "INSERT INTO SWLQSOTable([call], gridsquare, mode, rst_sent, rst_rcvd, qso_date, " +
                    "time_on, qso_date_off, time_off, band, freq, station_callsign, my_gridsquare,comment)\n" +
                    "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

            databaseOpr.db.execSQL(querySQL, new String[]{qslRecord.getToCallsign()
                    , qslRecord.getToMaidenGrid()
                    , qslRecord.getMode()
                    , String.valueOf(qslRecord.getSendReport())
                    , String.valueOf(qslRecord.getReceivedReport())
                    , qslRecord.getQso_date()
                    , qslRecord.getTime_on()

                    , qslRecord.getQso_date_off()
                    , qslRecord.getTime_off()
                    , qslRecord.getBandLength()//波长//RigOperationConstant.getMeterFromFreq(qslRecord.getBandFreq())
                    , BaseRigOperation.getFrequencyFloat(qslRecord.getBandFreq())
                    , qslRecord.getMyCallsign()
                    , qslRecord.getMyMaidenGrid()
                    , qslRecord.getComment()});


            return null;
        }

    }

    /**
     * 把QSL成功的呼号写到库中
     */
    static class AddQSL_Info extends AsyncTask<Void, Void, Void> {
        //private final SQLiteDatabase db;
        private final DatabaseOpr databaseOpr;
        private QSLRecord qslRecord;

        public AddQSL_Info(DatabaseOpr opr, QSLRecord qslRecord) {
            this.databaseOpr = opr;
            this.qslRecord = qslRecord;
        }


        @SuppressLint("Range")
        @Override
        protected Void doInBackground(Void... voids) {
            databaseOpr.doInsertQSLData(qslRecord);//添加日志和通联成功的呼号
            return null;
        }
    }


    /**
     * 从数据库中删除关注的呼号
     */
    static class DeleteFollowCallsign extends AsyncTask<Void, Void, Void> {
        private final SQLiteDatabase db;
        private final String callSign;

        public DeleteFollowCallsign(SQLiteDatabase db, String callSign) {
            this.db = db;
            this.callSign = callSign;
        }

        @SuppressLint("Range")
        @Override
        protected Void doInBackground(Void... voids) {
            String querySQL = "DELETE  from followCallsigns  WHERE callsign=?";
            db.execSQL(querySQL, new String[]{callSign});
            return null;
        }
    }

    /**
     * 向呼号与网格对应关系表中查网格，参数是呼号
     */
    static class GetCallsignQTH extends AsyncTask<String, Void, Void> {
        private final SQLiteDatabase db;

        GetCallsignQTH(SQLiteDatabase db) {
            this.db = db;
        }

        @SuppressLint("Range")
        @Override
        protected Void doInBackground(String... strings) {
            if (strings.length == 0) return null;
            String querySQL = "select grid from CallsignQTH cq \n" +
                    "WHERE callsign =?";
            Cursor cursor = db.rawQuery(querySQL, new String[]{strings[0]});
            if (cursor.moveToFirst()) {
                GeneralVariables.addCallsignAndGrid(strings[0]
                        , cursor.getString(cursor.getColumnIndex("grid")));
            }
            cursor.close();

            return null;
        }
    }

    static class GetMessageLogTotal extends AsyncTask<Void, Void, Void> {
        private final SQLiteDatabase db;
        private final OnAfterQueryFollowCallsigns onAffterQueryFollowCallsigns;

        public GetMessageLogTotal(SQLiteDatabase db, OnAfterQueryFollowCallsigns onAffterQueryFollowCallsigns) {
            this.db = db;
            this.onAffterQueryFollowCallsigns = onAffterQueryFollowCallsigns;
        }

        @Override
        @SuppressLint({"Range", "DefaultLocale"})
        protected Void doInBackground(Void... voids) {
            String querySQL = "SELECT BAND ,count(*) as c from SWLMessages m group by BAND order by BAND ";
            Cursor cursor = db.rawQuery(querySQL, new String[]{});
            ArrayList<String> callsigns = new ArrayList<>();
            callsigns.add(GeneralVariables.getStringFromResource(R.string.band_total));
            callsigns.add("---------------------------------------");
            int sum = 0;
            while (cursor.moveToNext()) {
                long s = cursor.getLong(cursor.getColumnIndex("BAND")); //获取频段
                int total = cursor.getInt(cursor.getColumnIndex("c")); //获取数量
                callsigns.add(String.format("%.3fMhz \t %d", s / 1000000f, total));
                sum = sum + total;
            }
            callsigns.add(String.format("-----------Total %d -----------", sum));
            cursor.close();
            if (onAffterQueryFollowCallsigns != null) {
                onAffterQueryFollowCallsigns.doOnAfterQueryFollowCallsigns(callsigns);
            }
            return null;
        }
    }


    static class GetSWLQsoTotal extends AsyncTask<Void, Void, Void> {
        private final SQLiteDatabase db;
        private final OnAfterQueryFollowCallsigns onAffterQueryFollowCallsigns;

        public GetSWLQsoTotal(SQLiteDatabase db, OnAfterQueryFollowCallsigns onAffterQueryFollowCallsigns) {
            this.db = db;
            this.onAffterQueryFollowCallsigns = onAffterQueryFollowCallsigns;
        }

        @Override
        @SuppressLint({"Range", "DefaultLocale"})
        protected Void doInBackground(Void... voids) {
            String querySQL = "select count(*) as c,substr(qso_date_off,1,6) as t \n" +
                    "from SWLQSOTable s\n" +
                    "group by substr(qso_date_off,1,6)";
            Cursor cursor = db.rawQuery(querySQL, new String[]{});
            ArrayList<String> callsigns = new ArrayList<>();
            //callsigns.add(GeneralVariables.getStringFromResource(R.string.band_total));
            callsigns.add("---------------------------------------");
            int sum = 0;
            while (cursor.moveToNext()) {
                String date = cursor.getString(cursor.getColumnIndex("t")); //获取频段
                int total = cursor.getInt(cursor.getColumnIndex("c")); //获取数量
                callsigns.add(String.format("%s \t %d ", date, total));
                sum = sum + total;
            }
            callsigns.add(String.format("-----------Total %d -----------", sum));
            cursor.close();
            if (onAffterQueryFollowCallsigns != null) {
                onAffterQueryFollowCallsigns.doOnAfterQueryFollowCallsigns(callsigns);
            }
            return null;
        }
    }



    /**
     * 从数据库中获取关注的呼号类
     */
    static class GetFollowCallSigns extends AsyncTask<Void, Void, Void> {
        private final SQLiteDatabase db;
        private final OnAfterQueryFollowCallsigns onAffterQueryFollowCallsigns;

        public GetFollowCallSigns(SQLiteDatabase db, OnAfterQueryFollowCallsigns onAffterQueryFollowCallsigns) {
            this.db = db;
            this.onAffterQueryFollowCallsigns = onAffterQueryFollowCallsigns;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            String querySQL = "select callsign from followCallsigns";
            Cursor cursor = db.rawQuery(querySQL, new String[]{});
            ArrayList<String> callsigns = new ArrayList<>();
            while (cursor.moveToNext()) {
                @SuppressLint("Range")
                String s = cursor.getString(cursor.getColumnIndex("callsign")); //获取第一列的值,第一列的索引从0开始
                if (s != null) {
                    callsigns.add(s);
                }
            }
            cursor.close();
            if (onAffterQueryFollowCallsigns != null) {
                onAffterQueryFollowCallsigns.doOnAfterQueryFollowCallsigns(callsigns);
            }
            return null;
        }
    }

    public static class GetCallsignMapGrid extends AsyncTask<Void, Void, Void> {
        SQLiteDatabase db;

        public GetCallsignMapGrid(SQLiteDatabase db) {
            this.db = db;
        }

        @SuppressLint("Range")
        @Override
        protected Void doInBackground(Void... voids) {

            String querySQL = "select DISTINCT callsign,grid from QslCallsigns qc \n" +
                    "where LENGTH(grid)>3\n" +
                    "order by ID ";
            Cursor cursor = db.rawQuery(querySQL, null);
            while (cursor.moveToNext()) {
                GeneralVariables.addCallsignAndGrid(cursor.getString(cursor.getColumnIndex("callsign"))
                        , cursor.getString(cursor.getColumnIndex("grid")));

            }
            cursor.close();
            return null;
        }
    }

    public interface OnGetQsoGrids {
        void onAfterQuery(HashMap<String, Boolean> grids);
    }


    static class GetQsoGrids extends AsyncTask<Void, Void, Void> {
        SQLiteDatabase db;

        HashMap<String, Boolean> grids = new HashMap<>();
        OnGetQsoGrids onGetQsoGrids;

        public GetQsoGrids(SQLiteDatabase db, OnGetQsoGrids onGetQsoGrids) {
            this.db = db;
            this.onGetQsoGrids = onGetQsoGrids;
        }

        @SuppressLint("Range")
        @Override
        protected Void doInBackground(Void... voids) {

            String querySQL = "select qc.gridsquare ,count(*) as cc,SUM(isQSL)+SUM(isLotW_QSL)as isQSL\n" +
                    "from QSLTable  qc\n" +
                    "WHERE LENGTH (qc.gridsquare)>2 \n" +
                    "group by qc.gridsquare\n" +
                    "ORDER by SUM(isQSL)+SUM(isLotW_QSL) desc";
            Cursor cursor = db.rawQuery(querySQL, null);

            while (cursor.moveToNext()) {
                grids.put(cursor.getString(cursor.getColumnIndex("gridsquare"))
                        , cursor.getInt(cursor.getColumnIndex("isQSL")) != 0);

            }
            cursor.close();
            if (onGetQsoGrids != null) {
                onGetQsoGrids.onAfterQuery(grids);
            }
            return null;
        }
    }

    static class GetQSLByCallsign extends AsyncTask<Void, Void, Void> {
        boolean showAll;
        int offset;
        SQLiteDatabase db;
        String callsign;
        int filter;
        OnQueryQSLRecordCallsign onQueryQSLRecordCallsign;

        public GetQSLByCallsign(boolean showAll,int offset,SQLiteDatabase db, String callsign, int queryFilter, OnQueryQSLRecordCallsign onQueryQSLRecordCallsign) {
            this.showAll=showAll;
            this.offset=offset;
            this.db = db;
            this.callsign = callsign;
            this.filter = queryFilter;
            this.onQueryQSLRecordCallsign = onQueryQSLRecordCallsign;
        }

        @SuppressLint("Range")
        @Override
        protected Void doInBackground(Void... voids) {
            String filterStr;
            switch (filter) {
                case 1:
                    filterStr = "and((isQSL =1)or(isLotW_QSL =1))\n";
                    break;
                case 2:
                    filterStr = "and((isQSL =0)and(isLotW_QSL =0))\n";
                    break;
                default:
                    filterStr = "";
            }
            String limitStr="";
            if (!showAll){
                limitStr="limit 100 offset "+offset;
            }
            String querySQL = "select * from QSLTable where ([call] like ?) \n" +
                    filterStr +
                    " ORDER BY qso_date DESC, time_off DESC\n"+
                    //" order by ID desc\n"+
                    limitStr;
            Cursor cursor = db.rawQuery(querySQL, new String[]{"%" + callsign + "%"});
            ArrayList<QSLRecordStr> records = new ArrayList<>();
            while (cursor.moveToNext()) {
                QSLRecordStr record = new QSLRecordStr();
                record.id = cursor.getInt(cursor.getColumnIndex("id"));
                record.setCall(cursor.getString(cursor.getColumnIndex("call")));
                record.isQSL = cursor.getInt(cursor.getColumnIndex("isQSL")) == 1;
                record.isLotW_import = cursor.getInt(cursor.getColumnIndex("isLotW_import")) == 1;
                record.isLotW_QSL = cursor.getInt(cursor.getColumnIndex("isLotW_QSL")) == 1;
                record.setGridsquare(cursor.getString(cursor.getColumnIndex("gridsquare")));
                record.setMode(cursor.getString(cursor.getColumnIndex("mode")));
                record.setRst_sent(cursor.getString(cursor.getColumnIndex("rst_sent")));
                record.setRst_rcvd(cursor.getString(cursor.getColumnIndex("rst_rcvd")));
                record.setTime_on(String.format("%s-%s"
                        , cursor.getString(cursor.getColumnIndex("qso_date"))
                        , cursor.getString(cursor.getColumnIndex("time_on"))));

                record.setTime_off(String.format("%s-%s"
                        , cursor.getString(cursor.getColumnIndex("qso_date_off"))
                        , cursor.getString(cursor.getColumnIndex("time_off"))));
                record.setBand(cursor.getString(cursor.getColumnIndex("band")));//波长
                record.setFreq(cursor.getString(cursor.getColumnIndex("freq")));//频率
                record.setStation_callsign(cursor.getString(cursor.getColumnIndex("station_callsign")));
                record.setMy_gridsquare(cursor.getString(cursor.getColumnIndex("my_gridsquare")));
                record.setComment(cursor.getString(cursor.getColumnIndex("comment")));
                records.add(record);
            }
            cursor.close();
            if (onQueryQSLRecordCallsign != null) {
                onQueryQSLRecordCallsign.afterQuery(records);
            }
            return null;
        }
    }

    /**
     * 通过呼号查询联通成功的呼号
     */
    static class GetQLSCallsignByCallsign extends AsyncTask<Void, Void, Void> {
        SQLiteDatabase db;
        String callsign;
        int filter;
        OnQueryQSLCallsign onQueryQSLCallsign;
        int offset;
        boolean showAll;

        public GetQLSCallsignByCallsign(boolean showAll,int offset,SQLiteDatabase db, String callsign, int queryFilter, OnQueryQSLCallsign onQueryQSLCallsign) {
            this.showAll=showAll;
            this.offset=offset;
            this.db = db;
            this.callsign = callsign;
            this.filter = queryFilter;
            this.onQueryQSLCallsign = onQueryQSLCallsign;
        }

        @SuppressLint("Range")
        @Override
        protected Void doInBackground(Void... voids) {
            String filterStr;
            switch (filter) {
                case 1:
                    filterStr = "and((q.isQSL =1)or(q.isLotW_QSL =1))\n";
                    break;
                case 2:
                    filterStr = "and((q.isQSL =0)and(q.isLotW_QSL =0))\n";
                    break;
                default:
                    filterStr = "";
            }
            String limitStr="";
            if (!showAll){
                limitStr="limit 100 offset "+offset;
            }
            String querySQL = "select q.[call] as callsign ,q.gridsquare as grid" +
                    ",q.band||\"(\"||q.freq||\" Mhz)\" as band \n" +
                    ",q.qso_date as last_time ,q.mode ,q.isQSL,q.isLotW_QSL\n" +
                    "from QSLTable q inner join QSLTable q2 ON q.id =q2.id \n" +
                    "where (q.[call] like ?)\n" +
                    filterStr +
                    "group by q.[call] ,q.gridsquare,q.freq ,q.qso_date,q.band\n" +
                    ",q.mode,q.isQSL,q.isLotW_QSL\n" +
                    "HAVING q.qso_date =MAX(q2.qso_date) \n" +
                    "order by q.qso_date desc\n"+
                    limitStr;


            Cursor cursor = db.rawQuery(querySQL, new String[]{"%" + callsign + "%"});
            ArrayList<QSLCallsignRecord> records = new ArrayList<>();
            while (cursor.moveToNext()) {
                QSLCallsignRecord record = new QSLCallsignRecord();
                record.setCallsign(cursor.getString(cursor.getColumnIndex("callsign")));
                record.isQSL = cursor.getInt(cursor.getColumnIndex("isQSL")) == 1;
                record.isLotW_QSL = cursor.getInt(cursor.getColumnIndex("isLotW_QSL")) == 1;
                record.setLastTime(cursor.getString(cursor.getColumnIndex("last_time")));
                record.setMode(cursor.getString(cursor.getColumnIndex("mode")));
                record.setGrid(cursor.getString(cursor.getColumnIndex("grid")));
                record.setBand(cursor.getString(cursor.getColumnIndex("band")));
                records.add(record);
            }
            cursor.close();
            if (onQueryQSLCallsign != null) {
                onQueryQSLCallsign.afterQuery(records);
            }
            return null;
        }
    }


    /**
     * 获取通联过的呼号
     */
    @SuppressLint("DefaultLocale")
    static class GetAllQSLCallsign {
        public static void get(SQLiteDatabase db) {

            //String querySQL = "select distinct [call] from QSLTable where freq=?";
            //改为以波长BAND取通联过的呼号
            String querySQL = "select distinct [call] from QSLTable where band=?";
            Cursor cursor = db.rawQuery(querySQL, new String[]{
                    BaseRigOperation.getMeterFromFreq(GeneralVariables.band)});
            ArrayList<String> callsigns = new ArrayList<>();
            while (cursor.moveToNext()) {
                @SuppressLint("Range")
                String s = cursor.getString(cursor.getColumnIndex("call"));
                if (s != null) {
                    callsigns.add(s);
                }
            }
            cursor.close();
            GeneralVariables.QSL_Callsign_list = callsigns;

            querySQL = "select distinct [call] from QSLTable where band<>?";
            cursor = db.rawQuery(querySQL, new String[]{
                    BaseRigOperation.getMeterFromFreq(GeneralVariables.band)});

            ArrayList<String> other_callsigns = new ArrayList<>();
            while (cursor.moveToNext()) {
                @SuppressLint("Range")
                String s = cursor.getString(cursor.getColumnIndex("call"));
                if (s != null) {
                    other_callsigns.add(s);
                }
            }
            cursor.close();
            GeneralVariables.QSL_Callsign_list_other_band = other_callsigns;
        }

    }


    /**
     * 通过ID删除通联呼号
     */
    static class DeleteQSLCallsignByID extends AsyncTask<Void, Void, Void> {
        private final SQLiteDatabase db;
        private final int id;

        public DeleteQSLCallsignByID(SQLiteDatabase db, int id) {
            this.db = db;
            this.id = id;
        }


        @Override
        protected Void doInBackground(Void... voids) {
            db.execSQL("delete from QslCallsigns where id=?", new Object[]{id});
            return null;
        }
    }


    /**
     * 通过ID删除日志
     */
    static class DeleteQSLByID extends AsyncTask<Void, Void, Void> {
        private final SQLiteDatabase db;
        private final int id;

        public DeleteQSLByID(SQLiteDatabase db, int id) {
            this.db = db;
            this.id = id;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            db.execSQL("delete from QSLTable where id=?", new Object[]{id});
            return null;
        }
    }

    static class SetQSLCallsignIsQSL extends AsyncTask<Void, Void, Void> {
        private final SQLiteDatabase db;
        private final int id;
        private final boolean isQSL;

        public SetQSLCallsignIsQSL(SQLiteDatabase db, int id, boolean isQSL) {
            this.db = db;
            this.id = id;
            this.isQSL = isQSL;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            db.execSQL("UPDATE QslCallsigns SET isQSL=? where id=?", new Object[]{isQSL ? "1" : "0", id});
            return null;
        }
    }

    /**
     * 设置日志手工确认
     */
    static class SetQSLTableIsQSL extends AsyncTask<Void, Void, Void> {
        private final SQLiteDatabase db;
        private final int id;
        private final boolean isQSL;

        public SetQSLTableIsQSL(SQLiteDatabase db, int id, boolean isQSL) {
            this.db = db;
            this.id = id;
            this.isQSL = isQSL;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            db.execSQL("UPDATE QSLTable SET isQSL=? where id=?", new Object[]{isQSL ? "1" : "0", id});
            return null;
        }
    }


    /**
     * 查询全部通联成功的呼号，以通联时的频段为条件
     */
    static class LoadAllQSLCallsigns extends AsyncTask<Void, Void, Void> {
        private final SQLiteDatabase db;

        public LoadAllQSLCallsigns(SQLiteDatabase db) {
            this.db = db;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            GetAllQSLCallsign.get(db);//获取通联过的呼号
            return null;
        }
    }

    static class GetAllConfigParameter extends AsyncTask<Void, Void, Void> {
        private final SQLiteDatabase db;
        private OnAfterQueryConfig onAfterQueryConfig;

        public GetAllConfigParameter(SQLiteDatabase db, OnAfterQueryConfig onAfterQueryConfig) {
            this.db = db;
            this.onAfterQueryConfig = onAfterQueryConfig;
        }

        @SuppressLint("Range")
        private String getConfigByKey(String KeyName) {
            String querySQL = "select keyName,Value from config where KeyName =?";
            Cursor cursor = db.rawQuery(querySQL, new String[]{KeyName});
            String result = "";
            if (cursor.moveToFirst()) {
                result = cursor.getString(cursor.getColumnIndex("Value"));
            }
            cursor.close();
            return result;
        }

        @SuppressLint("Range")
        @Override
        protected Void doInBackground(Void... voids) {

            String querySQL = "select keyName,Value from config ";
            Cursor cursor = db.rawQuery(querySQL, null);
            while (cursor.moveToNext()) {
                @SuppressLint("Range")
                //String result = "";
                String result = cursor.getString(cursor.getColumnIndex("Value"));
                String name = cursor.getString(cursor.getColumnIndex("KeyName"));

                if (name.equalsIgnoreCase("grid")) {
                    GeneralVariables.setMyMaidenheadGrid(result);
                }
                if (name.equalsIgnoreCase("callsign")) {
                    GeneralVariables.myCallsign = result;
                    String callsign = GeneralVariables.myCallsign;
                    if (callsign.length() > 0) {
                        Ft8Message.hashList.addHash(FT8Package.getHash22(callsign), callsign);
                        Ft8Message.hashList.addHash(FT8Package.getHash12(callsign), callsign);
                        Ft8Message.hashList.addHash(FT8Package.getHash10(callsign), callsign);
                    }
                }
                if (name.equalsIgnoreCase("toModifier")) {
                    GeneralVariables.toModifier = result;
                }
                if (name.equalsIgnoreCase("freq")) {
                    float freq = 1000;
                    try {
                        freq = Float.parseFloat(result);
                    } catch (Exception e) {
                        Log.e(TAG, "doInBackground: " + e.getMessage());
                    }
                    //GeneralVariables.setBaseFrequency(result.equals("") ? 1000 : Float.parseFloat(result));
                    GeneralVariables.setBaseFrequency(freq);
                }
                if (name.equalsIgnoreCase("synFreq")) {
                    GeneralVariables.synFrequency = !(result.equals("") || result.equals("0"));
                }
                if (name.equalsIgnoreCase("transDelay")) {
                    if (result.matches("^\\d{1,4}$")) {//正则表达式，1-4位长度的数字
                        GeneralVariables.transmitDelay = Integer.parseInt(result);
                    } else {
                        GeneralVariables.transmitDelay = FT8Common.FT8_TRANSMIT_DELAY;
                    }
                }

                if (name.equalsIgnoreCase("civ")) {
                    GeneralVariables.civAddress = result.equals("") ? 0xa4 : Integer.parseInt(result, 16);
                }
                if (name.equalsIgnoreCase("baudRate")) {
                    GeneralVariables.baudRate = result.equals("") ? 19200 : Integer.parseInt(result);
                }
                if (name.equalsIgnoreCase("bandFreq")) {
                    GeneralVariables.band = result.equals("") ? 14074000 : Long.parseLong(result);
                    GeneralVariables.bandListIndex = OperationBand.getIndexByFreq(GeneralVariables.band);
                }
                if (name.equalsIgnoreCase("ctrMode")) {
                    GeneralVariables.controlMode = result.equals("") ? ControlMode.VOX : Integer.parseInt(result);
                }
                if (name.equalsIgnoreCase("model")) {//电台型号
                    GeneralVariables.modelNo = result.equals("") ? 0 : Integer.parseInt(result);
                }
                if (name.equalsIgnoreCase("instruction")) {//指令集
                    GeneralVariables.instructionSet = result.equals("") ? 0 : Integer.parseInt(result);
                }
                if (name.equalsIgnoreCase("launchSupervision")) {//发射监管
                    GeneralVariables.launchSupervision = result.equals("") ?
                            GeneralVariables.DEFAULT_LAUNCH_SUPERVISION : Integer.parseInt(result);
                }
                if (name.equalsIgnoreCase("noReplyLimit")) {//
                    GeneralVariables.noReplyLimit = result.equals("") ? 0 : Integer.parseInt(result);
                }
                if (name.equalsIgnoreCase("autoFollowCQ")) {//自动关注CQ
                    GeneralVariables.autoFollowCQ = (result.equals("") || result.equals("1"));
                }
                if (name.equalsIgnoreCase("autoCallFollow")) {//自动呼叫关注
                    GeneralVariables.autoCallFollow = (result.equals("") || result.equals("1"));
                }
                if (name.equalsIgnoreCase("pttDelay")) {//ptt延时设置
                    GeneralVariables.pttDelay = result.equals("") ? 100 : Integer.parseInt(result);
                }
                if (name.equalsIgnoreCase("icomIp")) {//IcomIp地址
                    GeneralVariables.icomIp = result.equals("") ? "255.255.255.255" : result;
                }
                if (name.equalsIgnoreCase("icomPort")) {//Icom端口
                    GeneralVariables.icomUdpPort = result.equals("") ? 50001 : Integer.parseInt(result);
                }
                if (name.equalsIgnoreCase("icomUserName")) {//Icom用户名
                    GeneralVariables.icomUserName = result.equals("") ? "ic705" : result;
                }
                if (name.equalsIgnoreCase("icomPassword")) {//Icom密码
                    GeneralVariables.icomPassword = result;
                }
                if (name.equalsIgnoreCase("volumeValue")) {//输出音量大小
                    GeneralVariables.volumePercent = result.equals("") ? 1.0f : Float.parseFloat(result) / 100f;
                }
                if (name.equalsIgnoreCase("excludedCallsigns")) {//排除的呼号
                    GeneralVariables.addExcludedCallsigns(result);
                }
                if (name.equalsIgnoreCase("flexMaxRfPower")) {//指令集
                    GeneralVariables.flexMaxRfPower = result.equals("") ? 10 : Integer.parseInt(result);
                }
                if (name.equalsIgnoreCase("flexMaxTunePower")) {//指令集
                    GeneralVariables.flexMaxTunePower = result.equals("") ? 10 : Integer.parseInt(result);
                }
                if (name.equalsIgnoreCase("saveSWL")) {//保存解码信息
                    GeneralVariables.saveSWLMessage = result.equals("1");
                }
                if (name.equalsIgnoreCase("saveSWLQSO")) {//保存解码信息
                    GeneralVariables.saveSWL_QSO = result.equals("1");
                }
                if (name.equalsIgnoreCase("audioBits")) {//输出音频是否32位浮点
                    GeneralVariables.audioOutput32Bit = result.equals("1");
                }
                if (name.equalsIgnoreCase("audioRate")) {//输出音频是否32位浮点
                    GeneralVariables.audioSampleRate =Integer.parseInt( result);
                }
                if (name.equalsIgnoreCase("deepMode")) {//是不是深度解码模式
                    GeneralVariables.deepDecodeMode =result.equals("1");
                }
            }

            cursor.close();

            GetAllQSLCallsign.get(db);//获取通联过的呼号

            if (onAfterQueryConfig != null) {
                onAfterQueryConfig.doOnAfterQueryConfig(null, null);
            }

            return null;
        }
    }


}
