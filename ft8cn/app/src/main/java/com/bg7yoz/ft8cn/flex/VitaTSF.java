package com.bg7yoz.ft8cn.flex;

//时间戳小数部分类型
//小数部分主要有三种：
// 一种是sample-count ，以采样周期为最小分辨率，
// 一种是real-time以ps为最小单位，
// 第三种是以任意选择的时间进行累加得出的，
// 前面两种时间戳可以直接与整数部分叠加，
// 第三种则不能保证与整数部分保持恒定关系，前两种与整数部分叠加来操作的可以在覆盖的时间范围为年
// 小数部分的时间戳共有64位，小数部分可以在没有整数部分的情况下使用，
//  所有的时间带来都是在以一个采样数据为该参考点（reference-point）的时间。
public enum VitaTSF {
    TSF_NONE,//No Fractional-seconds Timestamp field included. 不包括分数秒时间戳字段
    TSF_SAMPLE_COUNT,//Sample Count Timestamp. 样本计数时间戳
    TSF_REALTIME,//Real Time(Picoseconds) Timestamp. 实时（皮秒）时间戳
    TSF_FREERUN,//Free Running Count Timestamp. 自由运行计数时间戳
}
