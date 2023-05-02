package com.bg7yoz.ft8cn.grid_tracker;
/**
 * 通联日志的提示窗口。
 * @author BGY70Z
 * @date 2023-03-20
 */

import android.annotation.SuppressLint;
import android.graphics.Paint;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.bg7yoz.ft8cn.Ft8Message;
import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.R;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.OverlayWithIW;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

public class GridRecordInfoWindow extends InfoWindow {
    public static final int UNDEFINED_RES_ID = 0;
    //    static int mTitleId = 0;
//    static int mDescriptionId = 0;
//    static int mSubDescriptionId = 0;
//    static int fromDxccImageId = 0;
//    static int fromItuImageId = 0;
//    static int fromCqImageId = 0;
//    static int toDxccImageId = 0;
//    static int toItuImageId = 0;
//    static int toCqImageId = 0;
    private final TextView titleView;
    private final TextView descriptionView;
    private final TextView subDescriptionView;


    @SuppressLint("UseCompatLoadingForDrawables")
    public GridRecordInfoWindow(int layoutResId, MapView mapView) {
        super(layoutResId, mapView);
        //setResIds(mapView.getContext());
        titleView = (TextView) this.mView.findViewById(R.id.tracker_rec_info_bubble_title);
        descriptionView = (TextView) this.mView.findViewById(R.id.tracker_rec_info_bubble_description);
        subDescriptionView = (TextView) this.mView.findViewById(R.id.tracker_rec_info_bubble_subdescription);

        ConstraintLayout layout=(ConstraintLayout) mView.findViewById(R.id.trackerGridRecInfoConstraintLayout);


        this.mView.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction() == 1) {
                    GridRecordInfoWindow.this.close();
                }
                return true;
            }
        });
    }


    @Override
    public void onOpen(Object item) {
        OverlayWithIW overlay = (OverlayWithIW) item;
        String title = overlay.getTitle();
        if (title == null) {
            title = "";
        }

        if (this.mView == null) {
            Log.w("OsmDroid", "Error trapped, BasicInfoWindow.open, mView is null!");
        } else {
            titleView.setText(title);
            String snippet = overlay.getSnippet();
            //Spanned snippetHtml = Html.fromHtml(snippet);
            descriptionView.setText(snippet);
            String subDesc = overlay.getSubDescription();
            subDescriptionView.setText(subDesc);

        }
    }

    @Override
    public void onClose() {

    }
}
