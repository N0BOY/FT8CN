package com.bg7yoz.ft8cn.rigs;

public class InstructionSet {
    public  static final int ICOM=0;
    public  static final int YAESU_2=1;//5字节数据 817
    public  static final int YAESU_3_9=2;//9字节频率
    public  static final int YAESU_3_8=3;//8字节频率
    public  static final int YAESU_3_450=4;//PTT发射不通
    public  static final int KENWOOD_TK90=5;//11字节频率，VFO模式下才可用
    public  static final int YAESU_DX10=6;//9字节，DATA-U模式
    public  static final int KENWOOD_TS590=7;//11字节频率，VFO模式
    public  static final int GUOHE_Q900=8;//国赫Q900，5字节一次性接收，与YAESU2代不同
    public  static final int XIEGUG90S=9;//协谷G90S，指令与ICOM兼容，但不会主动发送频率的变化，需要定期获取
    public  static final int ELECRAFT=10;//Elecraft k3
    public  static final int FLEX_CABLE=11;//FLEX6000;
    public  static final int FLEX_NETWORK=12;//FLEX网络模式连接;
    public  static final int XIEGU_6100=13;//协谷X6100;
    public  static final int KENWOOD_TS2000=14;//建武TS2000,发射指令是TX0;
    public  static final int TRUSDX=15;//(tr)USDX;
}
