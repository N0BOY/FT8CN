package com.bg7yoz.ft8cn.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;

public class RulerFrequencyView extends View {
    private static final String TAG = "RulerFrequencyView";
    private int rulerWidth = getWidth();
    private int freq = 1000;

    public RulerFrequencyView(Context context) {
        super(context);
    }

    public RulerFrequencyView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RulerFrequencyView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
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
        rulerWidth = w;
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @SuppressLint({"DefaultLocale", "ResourceAsColor"})
    public void drawRuler(Canvas canvas) {
        int top = 1;
        //rulerWidth=getRight();
        int width_rate = Math.round((float) rulerWidth / 30f);
        int lineWidth = (int) (getResources().getDisplayMetrics().density);
        int lineHeight = (int) (2 * getResources().getDisplayMetrics().density);
        Rect rect = new Rect();
        Paint paint = new Paint();
        paint.setColor(0xff00ffff);
        for (int i = 0; i <= 300; i++) {
            if (i % 1 == 0) {
                rect.top = top;
                rect.left = Math.round((float) i * width_rate);
                rect.right = rect.left + lineWidth;
                if (i % 5 == 0) {
                    rect.bottom = top + lineHeight * 3;
                    Paint fontPaint = new Paint();
                    fontPaint.setTextSize(dpToPixel(8));
                    fontPaint.setColor(0xff00ffff);
                    fontPaint.setAntiAlias(true);
                    fontPaint.setDither(true);
                    if (i == 0) {
                        fontPaint.setTextAlign(Paint.Align.LEFT);
                    } else if (i == 300) {
                        fontPaint.setTextAlign(Paint.Align.RIGHT);
                    } else {
                        fontPaint.setTextAlign(Paint.Align.CENTER);
                    }
                    canvas.drawText(String.format("%dHz", i * 100), rect.left
                            , rect.bottom + 8 * getResources().getDisplayMetrics().density
                            , fontPaint);

                } else {
                    rect.bottom = top + lineHeight;
                }
                canvas.drawRect(rect, paint);

            }
        }
        //主线
        rect.top = 1;
        rect.left = 0;
        rect.right = rulerWidth;
        rect.bottom = (int) (rect.top + 2*getResources().getDisplayMetrics().density);
        canvas.drawRect(rect, paint);

        //当前频率范围标记。红色块
        Rect mark = new Rect();
        paint.setColor(0xffff0000);
        mark.top = 1;
        mark.left = width_rate * (freq - 50) / 100;
        mark.right = width_rate * (freq + 50) / 100;
        mark.bottom = (int) (mark.top + 3*getResources().getDisplayMetrics().density);
        canvas.drawRect(mark, paint);

    }

    public void setFreq(int freq) {
        this.freq = freq;
        this.postInvalidate();
    }
}
