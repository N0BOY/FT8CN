package com.bg7yoz.ft8cn.flex;

//时间戳的类型
//时间戳共有两部分，小数部分和整数部分，整数部分以秒为分辨率，32位， 主要传递UTC时间或者 GPS 时间，
//小数部分主要有三种，一种是sample-count ，以采样周期为最小分辨率，一种是real-time以ps为最小单位，第三种是以任意选择的时间进行累加得出的，前面两种时间戳可以直接与整数部分叠加，第三种则不能保证与整数部分保持恒定关系，前两种与整数部分叠加来操作的可以在覆盖的时间范围为年
//小数部分的时间戳共有64位，小数部分可以在没有整数部分的情况下使用，
//所有的时间带来都是在以一个采样数据为该reference-point 时间
public enum VitaTSI {
    TSI_NONE,//No Integer-seconds Timestamp field included
    TSI_UTC,//Coordinated Universal Time(UTC)
    TSI_GPS,//GPS time
    TSI_OTHER//Other
}
