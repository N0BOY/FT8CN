package com.bg7yoz.ft8cn.log;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import androidx.core.content.FileProvider;

import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.ui.ToastMessage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class ShareLogs {
    private static final String TAG = "ShareLogs";
    private boolean isCancel=false;

    private String makeSQL(int queryFilter) {
        String filterStr;
        switch (queryFilter) {
            case 1:
                filterStr = "and((q.isQSL =1)or(q.isLotW_QSL =1))\n";
                break;
            case 2:
                filterStr = "and((q.isQSL =0)and(q.isLotW_QSL =0))\n";
                break;
            default:
                filterStr = "";
        }
        return " FROM QSLTable AS q \n" +
                "WHERE ((CALL LIKE ?)OR(station_callsign LIKE ?))\n" +
                filterStr;

    }


    @SuppressLint("Range")
    private int getCount(SQLiteDatabase db, String queryKey, int queryFilter) {
        String sql = makeSQL(queryFilter);
        String key = "%" + queryKey + "%";
        Cursor cursor = db.rawQuery("SELECT COUNT(*) AS C " + sql, new String[]{key, key});
        cursor.moveToFirst();
        int count = cursor.getInt(cursor.getColumnIndex("C"));

        cursor.close();
        return count;
    }

    private Cursor getData(SQLiteDatabase db, String queryKey, int queryFilter) {
        String sql = makeSQL(queryFilter);
        String key = "%" + queryKey + "%";
        return db.rawQuery("SELECT * " + sql, new String[]{key, key});
    }

    /**
     * 把日志数据写入到文件中，用于分享等处理
     *
     * @param db             数据库
     * @param queryKey       关键词
     * @param queryFilter    过滤条件
     * @param adiFile        临时文件
     * @param isSWL          是否是swl模式
     * @param onGetShareLogs 回调
     */
    @SuppressLint({"DefaultLocale", "Range"})
    private void downQSLTableToFile(SQLiteDatabase db, String queryKey, int queryFilter, File adiFile
            , boolean isSWL
            , OnShareLogEvents onGetShareLogs) {
        final int count = getCount(db, queryKey, queryFilter);

        if (onGetShareLogs != null) {
            onGetShareLogs.onShareStart(count, String.format(
                    GeneralVariables.getStringFromResource(R.string.total_logs)
                    , count));
        }
        Cursor cursor = getData(db, queryKey, queryFilter);
        FileOutputStream fileOutputStream = null;
        int position = 0;
        try {
            fileOutputStream = new FileOutputStream(adiFile, true);
            fileOutputStream.write("FT8CN ADIF Export<eoh>\n".getBytes());
            cursor.moveToPosition(-1);
            while (cursor.moveToNext()) {
                position++;
                if (onGetShareLogs != null) {
                   if (!onGetShareLogs.onShareProgress(count, position
                            , String.format(GeneralVariables.getStringFromResource(R.string.get_log_no)
                                    , position))){
                       break;
                   };
                }
                fileOutputStream.write(String.format("<call:%d>%s "
                        , cursor.getString(cursor.getColumnIndex("call")).length()
                        , cursor.getString(cursor.getColumnIndex("call"))).getBytes());
                if (!isSWL) {
                    if (cursor.getInt(cursor.getColumnIndex("isLotW_QSL")) == 1) {
                        fileOutputStream.write("<QSL_RCVD:1>Y ".getBytes());
                    } else {
                        fileOutputStream.write("<QSL_RCVD:1>N ".getBytes());
                    }
                    if (cursor.getInt(cursor.getColumnIndex("isQSL")) == 1) {
                        fileOutputStream.write("<QSL_MANUAL:1>Y ".getBytes());
                    } else {
                        fileOutputStream.write("<QSL_MANUAL:1>N ".getBytes());
                    }
                } else {
                    fileOutputStream.write("<swl:1>Y ".getBytes());
                }

                if (cursor.getString(cursor.getColumnIndex("gridsquare")) != null) {
                    fileOutputStream.write(String.format("<gridsquare:%d>%s "
                            , cursor.getString(cursor.getColumnIndex("gridsquare")).length()
                            , cursor.getString(cursor.getColumnIndex("gridsquare"))).getBytes());
                }

                if (cursor.getString(cursor.getColumnIndex("mode")) != null) {
                    fileOutputStream.write(String.format("<mode:%d>%s "
                            , cursor.getString(cursor.getColumnIndex("mode")).length()
                            , cursor.getString(cursor.getColumnIndex("mode"))).getBytes());
                }

                if (cursor.getString(cursor.getColumnIndex("rst_sent")) != null) {
                    fileOutputStream.write(String.format("<rst_sent:%d>%s "
                            , cursor.getString(cursor.getColumnIndex("rst_sent")).length()
                            , cursor.getString(cursor.getColumnIndex("rst_sent"))).getBytes());
                }

                if (cursor.getString(cursor.getColumnIndex("rst_rcvd")) != null) {
                    fileOutputStream.write(String.format("<rst_rcvd:%d>%s "
                            , cursor.getString(cursor.getColumnIndex("rst_rcvd")).length()
                            , cursor.getString(cursor.getColumnIndex("rst_rcvd"))).getBytes());
                }

                if (cursor.getString(cursor.getColumnIndex("qso_date")) != null) {
                    fileOutputStream.write(String.format("<qso_date:%d>%s "
                            , cursor.getString(cursor.getColumnIndex("qso_date")).length()
                            , cursor.getString(cursor.getColumnIndex("qso_date"))).getBytes());
                }

                if (cursor.getString(cursor.getColumnIndex("time_on")) != null) {
                    fileOutputStream.write(String.format("<time_on:%d>%s "
                            , cursor.getString(cursor.getColumnIndex("time_on")).length()
                            , cursor.getString(cursor.getColumnIndex("time_on"))).getBytes());
                }

                if (cursor.getString(cursor.getColumnIndex("qso_date_off")) != null) {
                    fileOutputStream.write( String.format("<qso_date_off:%d>%s "
                            , cursor.getString(cursor.getColumnIndex("qso_date_off")).length()
                            , cursor.getString(cursor.getColumnIndex("qso_date_off"))).getBytes());
                }

                if (cursor.getString(cursor.getColumnIndex("time_off")) != null) {
                    fileOutputStream.write(String.format("<time_off:%d>%s "
                            , cursor.getString(cursor.getColumnIndex("time_off")).length()
                            , cursor.getString(cursor.getColumnIndex("time_off"))).getBytes());
                }

                if (cursor.getString(cursor.getColumnIndex("band")) != null) {
                    fileOutputStream.write(String.format("<band:%d>%s "
                            , cursor.getString(cursor.getColumnIndex("band")).length()
                            , cursor.getString(cursor.getColumnIndex("band"))).getBytes());
                }

                if (cursor.getString(cursor.getColumnIndex("freq")) != null) {
                    fileOutputStream.write(String.format("<freq:%d>%s "
                            , cursor.getString(cursor.getColumnIndex("freq")).length()
                            , cursor.getString(cursor.getColumnIndex("freq"))).getBytes());
                }

                if (cursor.getString(cursor.getColumnIndex("station_callsign")) != null) {
                    fileOutputStream.write(String.format("<station_callsign:%d>%s "
                            , cursor.getString(cursor.getColumnIndex("station_callsign")).length()
                            , cursor.getString(cursor.getColumnIndex("station_callsign"))).getBytes());
                }

                if (cursor.getString(cursor.getColumnIndex("my_gridsquare")) != null) {
                    fileOutputStream.write(String.format("<my_gridsquare:%d>%s "
                            , cursor.getString(cursor.getColumnIndex("my_gridsquare")).length()
                            , cursor.getString(cursor.getColumnIndex("my_gridsquare"))).getBytes());
                }

                if (cursor.getColumnIndex("operator") != -1) {
                    if (cursor.getString(cursor.getColumnIndex("operator")) != null) {
                        fileOutputStream.write(String.format("<operator:%d>%s "
                                , cursor.getString(cursor.getColumnIndex("operator")).length()
                                , cursor.getString(cursor.getColumnIndex("operator"))).getBytes());
                    }
                }
                String comment = cursor.getString(cursor.getColumnIndex("comment"));

                //<comment:15>Distance: 99 mi <eor>
                //在写库的时候，一定要加" mi"
                fileOutputStream.write(String.format("<comment:%d>%s <eor>\n"
                        , comment.length()
                        , comment).getBytes());
            }


        } catch (IOException e) {
            Log.e(TAG,String.format("写文件出错：%s",e.getMessage()));
            ToastMessage.show(String.format(GeneralVariables
                    .getStringFromResource(R.string.write_file_error), e.getMessage()));
        } finally {
            try {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (IOException e) {
                Log.e(TAG, String.format("关闭写文件出错：%s", e.getMessage()));
                ToastMessage.show(String.format(GeneralVariables
                        .getStringFromResource(R.string.write_file_error), e.getMessage()));
            }
        }

        if (onGetShareLogs != null) {
            onGetShareLogs.afterGet(count, String.format(
                    GeneralVariables.getStringFromResource(R.string.total_logs)
                    , position));
        }
        Log.d(TAG, String.format("写入数据%d条", position));

        cursor.close();
    }

    /**
     * 分享文件
     *
     * @param context Context
     * @param file    文件对象
     * @param title   标题
     */
    public void doShareLogs(Context context, File file, String title
            , SQLiteDatabase db, String queryKey, int queryFilter, File adiFile
            , boolean isSWL
            , OnShareLogEvents onGetShareLogs) {

        isCancel=false;

        downQSLTableToFile(db, queryKey, queryFilter, adiFile, false, new OnShareLogEvents() {
            @Override
            public void onPreparing(String info) {
                if (onGetShareLogs!=null){
                    onGetShareLogs.onPreparing(info);
                }
            }

            @Override
            public void onShareStart(int count, String info) {
                if (onGetShareLogs != null) {
                    onGetShareLogs.onShareStart(count, info);
                }
            }

            @Override
            public boolean onShareProgress(int count, int position, String info) {
                if (onGetShareLogs != null) {
                    boolean temp=onGetShareLogs.onShareProgress(count, position, info);
                    isCancel=!temp;
                   return temp;
                }
                return true;
            }

            @Override
            public void afterGet( int count, String info) {
                if (onGetShareLogs != null) {
                    onGetShareLogs.afterGet( count, info);
                }
                if (!isCancel) {
                    Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                    Uri fileUri = FileProvider.getUriForFile(context.getApplicationContext()
                            , "com.bg7yoz.ft8cn.fileprovider", file);
                    sharingIntent.setType("text/plain");
                    sharingIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                    sharingIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    context.startActivity(Intent.createChooser(sharingIntent, title));
                }
            }

            @Override
            public void onShareFailed(String info) {
                if (onGetShareLogs!=null){
                    onGetShareLogs.onShareFailed(info);
                }
            }
        });


    }



}
