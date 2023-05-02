package com.bg7yoz.ft8cn.maidenhead;
/**
 * 梅登海德网格的处理。包括经纬度换算、距离计算。
 * @author BGY70Z
 * @date 2023-03-20
 */

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;

import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.R;
import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public class MaidenheadGrid {
    private static final String TAG = "MaidenheadGrid";
    private static final double EARTH_RADIUS = 6371393; // 平均半径,单位：m；不是赤道半径。赤道为6378左右

    /**
     * 计算梅登海德网格的经纬度，4字符或6字符。如果网格数据不正确，返回null。如果是四字符的，尾部加ll,取中间的位置。
     *
     * @param grid 梅登海德网格数据
     * @return LatLng 返回经纬度，如果数据不正确，返回null
     */
    public static LatLng gridToLatLng(String grid) {
        if (grid==null) return null;
        if (grid.length()==0) return null;
        //判断是不是符合梅登海德网格的规则
        if (grid.length() != 2&&grid.length() != 4 && grid.length() != 6) {
            return null;
        }
        if (grid.equalsIgnoreCase("RR73")) return null;
        if (grid.equalsIgnoreCase("RR")) return null;
        double x=0;
        double y=0;
        double z=0;
        //纬度
        double lat=0;
        if (grid.length()==2){
            x=grid.toUpperCase().getBytes()[1]-'A'+0.5f;
        }else {
            x=grid.toUpperCase().getBytes()[1]-'A';
        }
        x*=10;

        if (grid.length()==4){
            y=grid.getBytes()[3]-'0'+0.5f;
        }else if (grid.length()==6){
            y=grid.getBytes()[3]-'0';
        }

        if (grid.length()==6){
            z=grid.toUpperCase().getBytes()[5]-'A'+0.5f;
            z=z*(1/18f);
        }
        lat=x+y+z-90;

        //经度
        x=0;
        y=0;
        z=0;
        double lng=0;
        if (grid.length()==2){
            x=grid.toUpperCase().getBytes()[0]-'A'+0.5;
        }else {
            x=grid.toUpperCase().getBytes()[0]-'A';
        }
        x*=20;
        if (grid.length()==4){
            y=grid.getBytes()[2]-'0'+0.5;
        }else if (grid.length()==6){
            y=grid.getBytes()[2]-'0';
        }
        y*=2;
        if (grid.length()==6){
            z=grid.toUpperCase().getBytes()[4]-'A'+0.5;
            z=z*(2/18f);
        }
        lng=x+y+z-180;
        if (lat>85) lat=85;//防止在地图上越界
        if (lat<-85) lat=-85;//防止在地图上越界


        return new LatLng(lat,lng);



//        if (grid.length() == 4) {
//            grid = grid + "ll";
//        }
//        //计算纬度
//        double two = grid.getBytes()[1];
//        two -= 65;
//        two *= 10;
//        double four = grid.getBytes()[3] - '0';
//        double six = grid.getBytes()[5];
//        six -= 97;
//        six /= 24;
//        six += 1.0 / 48;
//        six -= 90;
//
//        //计算经度
//        double one = grid.getBytes()[0];
//        one -= 65;
//        one *= 20;
//        double three = grid.getBytes()[2] - '0';
//        three *= 2;
//        double five = grid.getBytes()[4];
//        five -= 97;
//        five /= 12;
//        five += 1.0 / 24;
        //return new LatLng(two + four + six, one + three + five - 180);
    }






    public static LatLng[] gridToPolygon(String grid) {
        if (grid.length() != 2 && grid.length() != 4 && grid.length() != 6) {
            return null;
        }
        LatLng[] latLngs = new LatLng[4];

        //纬度1
        double x;
        double y = 0;
        double z = 0;
        double lat1;
        x = grid.toUpperCase().getBytes()[1] - 'A';
        x *= 10;
        if (grid.length() > 2) {
            y = grid.getBytes()[3] - '0';
        }
        if (grid.length() > 4) {
            z = grid.toUpperCase().getBytes()[5] - 'A';
            z = z * (1f / 18f);
        }
        lat1 = x + y + z - 90;
        if (lat1<-85.0){
            lat1=-85.0;
        }
        if (lat1>85.0){
            lat1=85.0;
        }

        //纬度2
        x = 0;
        y = 0;
        z = 0;
        double lat2;
        if (grid.length() == 2) {
            x = grid.toUpperCase().getBytes()[1] - 'A' + 1;
        } else {
            x = grid.toUpperCase().getBytes()[1] - 'A';
        }
        x *= 10;
        if (grid.length() == 4) {
            y = grid.getBytes()[3] - '0' + 1;
        } else if (grid.length() == 6) {
            y = grid.getBytes()[3] - '0';
        }
        if (grid.length() == 6) {
            z = grid.toUpperCase().getBytes()[5] - 'A' + 1;
            z = z * (1f / 18f);
        }
        lat2 = x + y + z - 90;
        if (lat2<-85.0){
            lat2=-85.0;
        }
        if (lat2>85.0){
            lat2=85.0;
        }


        //经度1
        x=0;y=0;z=0;
        double lng1;
        x=grid.toUpperCase().getBytes()[0]-'A';
        x*=20;

        if (grid.length()>2){
            y=grid.getBytes()[2]-'0';
            y*=2;
        }
        if (grid.length()>4){
            z=grid.toUpperCase().getBytes()[4]-'A';
            z=z*2/18f;
        }
        lng1=x+y+z-180;

        //经度2
        x=0;y=0;z=0;
        double lng2;
        if (grid.length()==2){
            x=grid.toUpperCase().getBytes()[0]-'A'+1;
        }else {
            x=grid.toUpperCase().getBytes()[0]-'A';
        }
        x*=20;
        if (grid.length()==4){
            y=grid.getBytes()[2]-'0'+1;
        }else if (grid.length()==6){
            y=grid.getBytes()[2]-'0';
        }
        y*=2;
        if (grid.length()==6){
            z=grid.toUpperCase().getBytes()[4]-'A'+1;
            z=z*2/18f;
        }
        lng2=x+y+z-180;

        latLngs[0] = new LatLng(lat1,lng1);
        latLngs[1] = new LatLng(lat1,lng2);
        latLngs[2] = new LatLng(lat2,lng2);
        latLngs[3] = new LatLng(lat2,lng1);

//        Log.e(TAG, "gridToPolygon: latLng0"+latLngs[0].toString() );
//        Log.e(TAG, "gridToPolygon: latLng1"+latLngs[1].toString() );
//        Log.e(TAG, "gridToPolygon: latLng2"+latLngs[2].toString() );
//        Log.e(TAG, "gridToPolygon: latLng3"+latLngs[3].toString() );
        return latLngs;


    }

    /**
     * 此函数根据纬度计算 6 字符 Maidenhead网格。
     * 经纬度采用 NMEA 格式。换句话说，西经和南纬度为负数。它们被指定为double类型
     *
     * @param location 经纬度
     * @return String 梅登海德字符
     */
    public static String getGridSquare(LatLng location) {
        double tempNumber;//用于中间计算
        int index;//确定要显示的字符
        double _long = location.longitude;
        double _lat = location.latitude;
        StringBuilder buff = new StringBuilder();

        /*
         *	计算第一对两个字符
         */
        _long += 180;                    // 从太平洋中部开始
        tempNumber = _long / 20;            // 每个主要正方形都是 20 度宽
        index = (int) tempNumber;            // 大写字母的索引
        buff.append(String.valueOf((char) (index + 'A')));  // 设置第一个字符
        _long = _long - (index * 20);            // 第 2 步的剩余部分

        _lat += 90;                    //从南极开始 180 度
        tempNumber = _lat / 10;                // 每个大正方形高 10 度
        index = (int) tempNumber;            // 大写字母的索引
        buff.append(String.valueOf((char) (index + 'A')));//设置第二个字符
        _lat = _lat - (index * 10);            // 第 2 步的剩余部分

        /*
         *	现在是第二对两数字：
         */
        tempNumber = _long / 2;                // 步骤 1 的余数除以 2
        index = (int) tempNumber;            // 数字索引
        buff.append(String.valueOf((char) (index + '0')));//设置第三个字符
        _long = _long - (index * 2);            //第 3 步的剩余部分

        tempNumber = _lat;                // 步骤 1 的余数除以 1
        index = (int) tempNumber;            // 数字索引
        buff.append(String.valueOf((char) (index + '0')));//设置第四个字符
        _lat = _lat - index;                //第 3 步的剩余部分

        /*
         *现在是第三对两个小写字符：
         */
        tempNumber = _long / 0.083333;            //步骤 2 的余数除以 0.083333
        index = (int) tempNumber;            // 小写字母的索引
        buff.append(String.valueOf((char) (index + 'a')));//设置第五个字符

        tempNumber = _lat / 0.0416665;            // 步骤 2 的余数除以 0.0416665
        index = (int) tempNumber;            // 小写字母的索引
        buff.append(String.valueOf((char) (index + 'a')));//设置第五个字符

        return buff.toString().substring(0, 4);
    }

    /**
     * 计算经纬度之间的距离
     *
     * @param latLng1 经纬度
     * @param latLng2 经纬度
     * @return 距离，公里。
     */
    public static double getDist(LatLng latLng1, LatLng latLng2) {
        double radiansAX = Math.toRadians(latLng1.longitude); // A经弧度
        double radiansAY = Math.toRadians(latLng1.latitude); // A纬弧度
        double radiansBX = Math.toRadians(latLng2.longitude); // B经弧度
        double radiansBY = Math.toRadians(latLng2.latitude); // B纬弧度

        // 公式中“cosβ1cosβ2cos（α1-α2）+sinβ1sinβ2”的部分，得到∠AOB的cos值
        double cos = Math.cos(radiansAY) * Math.cos(radiansBY) * Math.cos(radiansAX - radiansBX)
                + Math.sin(radiansAY) * Math.sin(radiansBY);
        double acos = Math.acos(cos); // 反余弦值
        return EARTH_RADIUS * acos / 1000; // 最终结果km
    }

    /**
     * 计算梅登海德网格之间的距离
     *
     * @param mGrid1 梅登海德网格
     * @param mGrid2 梅登海德网格2
     * @return double 两个网格之间的距离
     */
    public static double getDist(String mGrid1, String mGrid2) {
        LatLng latLng1 = gridToLatLng(mGrid1);
        LatLng latLng2 = gridToLatLng(mGrid2);
        if (latLng1 != null && latLng2 != null) {
            return getDist(latLng1, latLng2);
        } else {
            return 0;
        }
    }

    /**
     * 计算两个网格之间的距离
     *
     * @param mGrid1 网格
     * @param mGrid2 网格
     * @return 距离
     */
    @SuppressLint("DefaultLocale")
    public static String getDistStr(String mGrid1, String mGrid2) {
        double dist = getDist(mGrid1, mGrid2);
        if (dist == 0) {
            return "";
        } else {
            return String.format(GeneralVariables.getStringFromResource(R.string.distance), dist);
        }
    }
    public static String getDistLatLngStr(LatLng latLng1,LatLng latLng2){
        return String.format(GeneralVariables.getStringFromResource(R.string.distance), getDist(latLng1,latLng2));

    }

    /**
     * 计算两个网格之间的距离，以英文显示公里数
     *
     * @param mGrid1 网格
     * @param mGrid2 网格
     * @return 距离
     */
    @SuppressLint("DefaultLocale")
    public static String getDistStrEN(String mGrid1, String mGrid2) {
        double dist = getDist(mGrid1, mGrid2);
        if (dist == 0) {
            return "";
        } else {
            return String.format("%.0f km", dist);
        }
    }

    /**
     * 获取本设备的经纬度
     *
     * @param context context
     * @return 经纬度
     */
    public static LatLng getLocalLocation(Context context) {
        // 获取位置服务
        String serviceName = Context.LOCATION_SERVICE;
        // 调用getSystemService()方法来获取LocationManager对象
        LocationManager locationManager = (LocationManager) context.getSystemService(serviceName);
        // 指定LocationManager的定位方法
        //String provider = LocationManager.GPS_PROVIDER;
        // 调用getLastKnownLocation()方法获取当前的位置信息

        List<String> providers = locationManager.getProviders(true);
        Location location = null;
        for (String s : providers) {
            @SuppressLint("MissingPermission") Location l = locationManager.getLastKnownLocation(s);
            if (l == null) {
                continue;
            }
            if (location == null || l.getAccuracy() < location.getAccuracy()) {
                // Found best last known location: %s", l);
                location = l;
            }
        }

        if (location != null) {
            return new LatLng(location.getLatitude(), location.getLongitude());
        } else {
            return null;
        }
    }


    /**
     * 获取本机的梅登海德网格数据。需要定位的权限。
     *
     * @param context context
     * @return String 返回6字符的梅登海德网格。
     */
    public static String getMyMaidenheadGrid(Context context) {
        LatLng latLng = getLocalLocation(context);

        if (latLng != null) {
            return getGridSquare(latLng);
        } else {
            //ToastMessage.show("无法定位，请确认是否有定位的权限。");
            return "";
        }
    }

    /**
     * 检查是不是梅登海德网格。如果不是返回false。
     *
     * @param s 梅登海德网格
     * @return boolean 是否是梅登海德网格。
     */
    public static boolean checkMaidenhead(String s) {
        if (s.length() != 4 && s.length() != 6) {
            return false;
        } else {
            if (s.equals("RR73")) {
                return false;
            }
            return Character.isAlphabetic(s.charAt(0))
                    && Character.isAlphabetic(s.charAt(1))
                    && Character.isDigit(s.charAt(2))
                    && Character.isDigit(s.charAt(3));
        }
    }

}