package com.bg7yoz.ft8cn.ui;
/**
 * 自定义音频强度的图形控件。
 * @author BGY70Z
 * @date 2023-03-20
 */

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class VolumeProgress extends View {
    private Paint mRadarPaint;
    private Paint mValuePaint;

    private Path mLinePath;//外部容器形状
    private Path mValuePath;//内部填充的形状
    private float mPercent=0.45f;
    private int width,high;

    private int radarColor=Color.WHITE;
    private int valueColor=Color.WHITE;
    private int alarmColor=Color.RED;
    private float alarmValue=0.5f;

    public VolumeProgress(Context context) {
        super(context);
    }

    public VolumeProgress(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public VolumeProgress(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private int dpToPixel(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        width=w;
        high=h;
        postInvalidate();
        super.onSizeChanged(w, h, oldw, oldh);
    }

    public void setPercent(float percent) {
        mPercent = percent;

        init();
        invalidate();
    }
    public void reDraw(){
        setPercent(mPercent);
    }

    public void init() {
        //绘制外部容器
        mRadarPaint = new Paint();
        mRadarPaint.setAntiAlias(true);
        mRadarPaint.setStrokeWidth(dpToPixel(2));
        mRadarPaint.setStyle(Paint.Style.STROKE);
        mRadarPaint.setColor(radarColor);

        //绘制填充的内容
        mValuePaint = new Paint();
        mValuePaint.setStrokeWidth(dpToPixel(2));
        mValuePaint.setStyle(Paint.Style.FILL_AND_STROKE);


        //绘制外部容器路径
        mLinePath = new Path();
        mValuePath = new Path();

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //绘制外部容器
        drawLines(canvas);
        //绘制每部填充
        drawRegion(canvas,mPercent);
    }

    //绘制外部容器，顺时针，O-A-B-C
    public void drawLines(Canvas canvas) {
        mRadarPaint.setColor(radarColor);
        mLinePath.reset();

        float xa = 0;//(float) (mCenterX + width);
        float ya = 0;//(float) (mCenterY);
        mLinePath.moveTo(xa,ya);

        float xb = width;
        float yb = high;
        mLinePath.lineTo(xb, yb);

        float xc =0;
        float yc = high;
        mLinePath.lineTo(xc, yc);

        mLinePath.close();
        canvas.drawPath(mLinePath, mRadarPaint);
    }

    //绘制覆盖图层，顺时针，O-Q-P-C
    public void drawRegion(Canvas canvas, float percent) {
        if (alarmValue>mPercent) {
            mValuePaint.setColor(valueColor);
        }else {
            mValuePaint.setColor(alarmColor);
        }

        //直线CB与QH，求出交点坐标P，
        //其中H点=（是以B点的Y坐标，Q点的X坐标），起始就是QP的延长线
       float xa=(1f-percent)*width;
       float ya=(1f-percent)*high;
       float xb=width;
       float yb=high;
       float xc=(1f-percent)*width;
       float yc=high;

        mValuePath.moveTo(xa, ya);
        mValuePath.lineTo(xb, yb);
        mValuePath.lineTo(xc, yc);


        mValuePath.close();
        canvas.drawPath(mValuePath, mValuePaint);
    }

    //求两直线相交的坐标

    public void setRadarColor(int radarColor) {
        this.radarColor = radarColor;
    }

    public void setValueColor(int valueColor) {
        this.valueColor = valueColor;
    }

    public void setAlarmColor(int alarmColor) {
        this.alarmColor = alarmColor;
    }

    public void setAlarmValue(float alarmValue) {
        this.alarmValue = alarmValue;
    }
}
