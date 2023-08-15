package com.bg7yoz.ft8cn.ui;
/**
 * flexRadio仪表的自定义控件。
 * @author BGY70Z
 * @date 2023-03-20
 */

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;

public class FlexMeterRulerView extends View {
    private static final String TAG = "FlexMeterRulerView";

    private String label="S/Po";
    private String unit="dBm";
    private String valueLabel="5dBm";
    private float lowVal=-150f;
    private float highVal=-72f;
    private float maxVal=10f;
    private int normalCount=9;
    private int highCount=3;
    private String[] normalLabels=new String[]{"0","1","2","3","4","5","6","7","8","9"};
    private String[] highLabels=new String[]{"20","40","50"};
    private float value=5f;


    private final int labelDp=12;//标签字体大小dp
    private Rect rulerRect = new Rect();
    private Rect valueRect =new Rect();
    private Paint fontPaint = new Paint();
    private Paint rulerPaint = new Paint();
    private Paint valuePaint = new Paint();
    private Paint labelPaint=new Paint();
    private int rulerWidth = getWidth();
    private int labelWidth=dpToPixel(40);//标签宽度
    private int valueWidth=dpToPixel(65);//值标签宽度

    @SuppressLint("DefaultLocale")
    public void setValue(float value) {
        valueLabel=String.format("%.1f%s",value,unit);
        if (value>maxVal) {
            this.value = maxVal;
        }else if(value<lowVal){
            this.value = lowVal;
        }else {
            this.value = value;
        }
    }

    public void initVal(float low,float high,float max,int normal_count,int high_count){
        this.lowVal=low;
        this.highVal=high;
        this.maxVal=max;
        this.normalCount=normal_count;
        this.highCount=high_count;
    }
    public void initLabels(String label,String unit,String[] normal,String[] high){
        this.label=label;
        this.unit=unit;
        this.normalLabels=normal;
        this.highLabels=high;
    }
    public void setLabel(String label) {
        this.label = label;
    }

    public FlexMeterRulerView(Context context) {
        super(context);
    }

    public FlexMeterRulerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public FlexMeterRulerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //Log.d(TAG, String.format("onDraw: rulerWidth:%d,getWidth:%d", rulerWidth, getWidth()));
        drawRuler(canvas);
        super.onDraw(canvas);
    }

    /**
     * 把dp值转换为像素点
     *
     * @param dp dp值
     * @return 像素点
     */
    private int dpToPixel(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp
                , getResources().getDisplayMetrics());
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        //Log.d(TAG, String.format("onSizeChanged: rulerWidth:%d,getWidth:%d", w, getWidth()));
        rulerWidth = w-labelWidth-valueWidth;
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @SuppressLint({"DefaultLocale", "ResourceAsColor"})
    public void drawRuler(Canvas canvas) {

        int width_rate = Math.round((float) rulerWidth / (normalCount+highCount));//最大得长度比率

        labelPaint.setColor(0xff00ffff);
        labelPaint.setTextSize(dpToPixel(labelDp));
        labelPaint.setAntiAlias(true);
        labelPaint.setDither(true);
        labelPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(label, labelWidth-5,  dpToPixel(labelDp)+5, labelPaint);
        labelPaint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(valueLabel, rulerWidth+labelWidth+5
                ,  dpToPixel(labelDp)+5, labelPaint);

        rulerPaint.setColor(0xff00FF00);

        valueRect.top=5;
        valueRect.left=labelWidth;
        valueRect.bottom=20;
        if (value<highVal){
            valueRect.right =Math.round((value-lowVal)*(width_rate*normalCount)/(highVal-lowVal))+labelWidth;
            valuePaint.setColor(0xff00FF00);
            canvas.drawRect(valueRect, rulerPaint);
        }else {
            valueRect.right =width_rate*normalCount+labelWidth
                    +Math.round((value-highVal)*width_rate*highCount/(maxVal-highVal));
            valuePaint.setColor(0xffff0000);
            canvas.drawRect(valueRect, valuePaint);
        }
        //valueRect.right=30;




        //主线
        rulerRect.top = valueRect.bottom+5;
        rulerRect.left = labelWidth;
        rulerRect.right = width_rate*normalCount+labelWidth;
        rulerRect.bottom = (int) (rulerRect.top + 2*getResources().getDisplayMetrics().density);
        canvas.drawRect(rulerRect, rulerPaint);
        rulerPaint.setColor(0xffff0000);
        rulerRect.left=rulerRect.right;
        rulerRect.right=width_rate*highCount+rulerRect.left;
        rulerPaint.setColor(0xffff0000);
        canvas.drawRect(rulerRect, rulerPaint);

        //Paint fontPaint = new Paint();
        fontPaint.setTextSize(dpToPixel(8));
        fontPaint.setColor(0xff00ffff);
        fontPaint.setAntiAlias(true);
        fontPaint.setDither(true);

        for (int i = 0; i <normalLabels.length ; i++) {
            if (i==0){
                fontPaint.setTextAlign(Paint.Align.LEFT);
            }else {
                fontPaint.setTextAlign(Paint.Align.CENTER);
            }
            canvas.drawText(normalLabels[i], i*width_rate+labelWidth
                    , rulerRect.bottom + 8 * getResources().getDisplayMetrics().density
                    , fontPaint);
        }

        fontPaint.setColor(0xffff0000);
        for (int i = 0; i < highLabels.length; i++) {
            if (i<highCount-1){
                fontPaint.setTextAlign(Paint.Align.CENTER);
            }else {
                fontPaint.setTextAlign(Paint.Align.RIGHT);
            }
            canvas.drawText(highLabels[i], (i+normalCount+1)*width_rate+labelWidth
                    , rulerRect.bottom + 8 * getResources().getDisplayMetrics().density
                    , fontPaint);
        }

        invalidate();
    }

}
