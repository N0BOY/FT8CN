package com.bg7yoz.ft8cn.ui;

import static android.graphics.Bitmap.Config.ARGB_8888;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * 柱状频谱图。
 * @author BGY70Z
 * @date 2023-03-20
 */
public class ColumnarView extends View {
    private static final String TAG = "ColumnarView";
    //每一个能量柱的宽度
    private int width;
    //每一个能量柱之间的间距
    private final int spacing = 1;
    //能量块高度
    private final int blockHeight = 5;
    //能量块下将速度
    private int blockSpeed = 5;
    //能量块与能量柱之间的距离
    private final int distance = 2;

    private boolean drawblock = false;
    private final Paint paint = new Paint();
    private final List<Rect> newData = new ArrayList<>();
    private final List<Rect> blockData = new ArrayList<>();

    private Bitmap lastBitMap=null;
    private Canvas _canvas;
    private Paint linePaint;
    private int touch_x = -1;
    private Paint touchPaint;
    private int freq_hz=-1;

    public void setBlockSpeed(int blockSpeed) {
        this.blockSpeed = blockSpeed;
    }

    public ColumnarView(Context context) {
        super(context);

    }

    public ColumnarView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ColumnarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setShowBlock(boolean showBlock) {
        drawblock = showBlock;
    }

    public void setWaveData(int[] data) {
        if (data == null) {
            return;
        }
        if (data.length <= 0) {
            return;
        }
        width = getWidth() / (data.length / 2);// 960/2=480，480比较合理，906无法显示了。
        if (drawblock) {//是否显示能量块
            if (newData.size() > 0) {
                if (blockData.size() == 0 || newData.size() != blockData.size()) {
                    blockData.clear();
                    for (int i = 0; i < data.length / 2; i++) {
                        Rect blockRect = new Rect();
                        blockRect.top =getHeight()- blockHeight;
                        blockRect.bottom = getHeight();
                        blockData.add(blockRect);
                    }
                }
                for (int i = 0; i < blockData.size(); i++) {
                    blockData.get(i).left = newData.get(i).left;
                    blockData.get(i).right = newData.get(i).right;
                    if (newData.get(i).top < blockData.get(i).top) {
                        blockData.get(i).top = newData.get(i).top - blockHeight - distance;
                    } else {
                        blockData.get(i).top = blockData.get(i).top + blockSpeed;
                    }
                    blockData.get(i).bottom = blockData.get(i).top + blockHeight;
                }
            }
        }
        newData.clear();
        float rateHeight =  0.95f * getHeight() / 256;//0.95是比率，柱形的最大高度不超过95%
        for (int i = 0; i < data.length / 2; i++) {
            Rect colRect = new Rect();
            if (newData.size() == 0) {
                colRect.left = 0;
            } else {
                colRect.left = i * getWidth() / (data.length / 2);
            }
            colRect.top = getHeight() - Math.round(Math.max(data[i], data[i + 1]) * rateHeight);
            colRect.right = colRect.left + width - spacing;
            colRect.bottom = getHeight();
            newData.add(colRect);
        }
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        setClickable(true);
        super.onSizeChanged(w, h, oldw, oldh);

        lastBitMap= Bitmap.createBitmap(w,h, ARGB_8888);
        _canvas=new Canvas(lastBitMap);
        LinearGradient linearGradient=new LinearGradient(0f, 0f, 0f, getHeight(),
                new int[]{0xff00ffff,0xff00ffff, Color.BLUE}
                , new float[]{0f, 0.6f, 1f}, Shader.TileMode.CLAMP);
        paint.setShader(linearGradient);
        linePaint = new Paint();
        linePaint.setColor(0xff990000);
        touchPaint = new Paint();
        touchPaint.setColor(0xff00ffff);
        touchPaint.setStrokeWidth(2);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        _canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        for (int i = 0; i < newData.size(); i++) {
            _canvas.drawRect(newData.get(i), paint);
        }
        if (drawblock) {
            for (int i = 0; i < blockData.size(); i++) {
                _canvas.drawRect(blockData.get(i), paint);
            }
        }
        canvas.drawBitmap(lastBitMap,0,0,null);
        if (touch_x>0) {
            //计算频率
            freq_hz = Math.round(3000f * (float) touch_x / (float) getWidth());
            canvas.drawLine(touch_x, 0, touch_x, getHeight(), touchPaint);
        }
        invalidate();
    }
    public void setTouch_x(int touch_x) {
        this.touch_x = touch_x;
    }

    public int getFreq_hz() {
        return freq_hz;
    }
}
