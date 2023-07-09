package com.bg7yoz.ft8cn.floatview;
/**
 * 自定义FloatButton
 * @author BGY70Z
 * @date 2023-03-20
 */

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageButton;

@SuppressLint("AppCompatCustomView")
public class FloatViewButton extends ImageButton {
    private String name;
    public FloatViewButton(Context context) {
        super(context);
    }

    public FloatViewButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FloatViewButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public String getName() {
        if (name==null){
            return "";
        }else {
            return name;
        }
    }

    public void setName(String name) {
        this.name = name;
    }
}
