package com.bg7yoz.ft8cn.database;

import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;

public class DxccObject {
    int id;
    int dxcc;
    String cc;
    String ccc;
    String name;
    String continent;
    String ituZone;
    String cqZone;
    int timeZone;
    int cCode;
    String aName;
    String pp;
    double lat;
    double lon;
    ArrayList<String> grid=new ArrayList<>();
    ArrayList<String> prefix=new ArrayList<>();

    public void insertToDb(SQLiteDatabase db){
        String insertSQL="INSERT INTO dxccList (id,dxcc,cc,ccc,name,continent" +
                ",ituzone,cqzone,timezone,ccode,aname,pp,lat,lon)"+
                "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

        db.execSQL(insertSQL,new Object[]{this.id,this.dxcc,this.cc,this.ccc,this.name,this.continent
                ,this.ituZone,this.cqZone,this.timeZone,this.cCode,this.aName,this.pp,this.lat,this.lon});

        String insertGrid="INSERT INTO dxcc_grid(dxcc,grid)VALUES(?,?)";
        for (int i = 0; i < this.grid.size(); i++) {
            db.execSQL(insertGrid,new Object[]{this.dxcc,this.grid.get(i)});
        }

        String insert_Prefix="INSERT INTO dxcc_prefix(dxcc,prefix)VALUES(?,?)";
        for (int i = 0; i <this.prefix.size() ; i++) {
            db.execSQL(insert_Prefix,new Object[]{this.dxcc,this.prefix.get(i)});
        }
    }
}
