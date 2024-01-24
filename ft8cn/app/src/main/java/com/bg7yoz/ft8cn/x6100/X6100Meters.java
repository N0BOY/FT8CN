package com.bg7yoz.ft8cn.x6100;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.annotation.NonNull;

import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.flex.VITA;

public class X6100Meters {
    private final String TAG="X6100Meters";
    public float sMeter;
    public float power;
    public float swr;
    public float alc;
    public float volt;
    public float max_power;
    public short tx_volume;
    public short af_level;//电台的音量

    public X6100Meters() {

    }
    public synchronized void update(byte[] meterData){
        for (int i = 0; i < meterData.length/4; i++) {
            short index = VITA.readShortDataBigEnd(meterData,i*4);
            short value= VITA.readShortDataBigEnd(meterData,i*4+2);
            setValues(index,value);
        }
    }

    private void setValues(short index,short value){
        switch (index){
            case 0://sMeter
                sMeter = (100.0f/255.0f)*value -130;
                //Log.e(TAG,String.format("s.Meter:%.1f",sMeter));
                break;
            case 1://power
                power = (25/255f)*value*10;
                //Log.e(TAG,String.format("power:%.1f",power));
                break;
            case 2://swr
                swr= value *1.0f;
                //Log.e(TAG,String.format("swr:%d",value));
                //ToastMessage.show(String.format("swr:%d",value));
                break;
            case 3://alc
                alc = (100.0f/255f)*value;
                //ToastMessage.show(String.format("alc:%d",value));
                //Log.e(TAG,String.format("alc:%.1f",alc));
                break;
            case 4:
                volt = value *1.0f;
                //Log.e(TAG,String.format("volt:%.1f",volt));
                break;
            case 5:
                max_power = value /25.5f;
                //GeneralVariables.flexMaxRfPower=(int)max_power;
                //Log.e(TAG,String.format("max_power:%.1f,val:%d",max_power,value));
                break;
            case 6:
                tx_volume = value;
                //Log.e(TAG,String.format("tx_volume:%d",tx_volume));
                break;
            case 7:
                af_level = value;
                //Log.e(TAG,String.format("tx_volume:%d",tx_volume));
                break;
            default:
        }
    }

    @SuppressLint("DefaultLocale")
    @NonNull
    @Override
    public String toString() {
        //return String.format("S.Meter: %.1f dBm\nSWR: %s\nALC: %.1f\nVolt: %.1fV\nTX power: %.1f W\nMax tx power: %.1f\nTX volume:%d%%"
        return String.format(GeneralVariables.getStringFromResource(R.string.xiegu_meter_info)
                ,sMeter
                ,swr > 8 ? "∞" : String.format("%.1f",swr)
                ,alc
                ,volt
                ,power
                ,max_power
                ,tx_volume
        );
        //"信号强度: %.1f dBm\n驻波: %s\nALC: %.1f\n电压: %.1fV\n发射功率: %.1f W\n最大发射功率: %.1f\n发射音量:%d%%"
    }
}
