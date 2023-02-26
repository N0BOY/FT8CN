package com.bg7yoz.ft8cn.grid_tracker;

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

public class GridInfoWindow extends InfoWindow {
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
    public GridInfoWindow(int layoutResId, MapView mapView, Ft8Message msg) {
        super(layoutResId, mapView);
        //setResIds(mapView.getContext());
        titleView = (TextView) this.mView.findViewById(R.id.tracker_info_bubble_title);
        descriptionView = (TextView) this.mView.findViewById(R.id.tracker_info_bubble_description);
        subDescriptionView = (TextView) this.mView.findViewById(R.id.tracker_info_bubble_subdescription);
        ImageView fromDxccImage = (ImageView) this.mView.findViewById(R.id.track_from_dxcc_image);
        ImageView fromItuImage = (ImageView) this.mView.findViewById(R.id.track_from_itu_image);
        ImageView fromCqImage = (ImageView) this.mView.findViewById(R.id.track_from_cq_image);
        ImageView toDxccImage = (ImageView) this.mView.findViewById(R.id.track_to_dxcc_image);
        ImageView toItuImage = (ImageView) this.mView.findViewById(R.id.track_to_itu_image);
        ImageView toCqImage = (ImageView) this.mView.findViewById(R.id.track_to_cq_image);
        ConstraintLayout layout=(ConstraintLayout) mView.findViewById(R.id.trackerGridInfoConstraintLayout);

        if (!msg.fromDxcc) fromDxccImage.setVisibility(View.GONE);
        if (!msg.fromItu) fromItuImage.setVisibility(View.GONE);
        if (!msg.fromCq) fromCqImage.setVisibility(View.GONE);
        if (!msg.toDxcc) toDxccImage.setVisibility(View.GONE);
        if (!msg.toItu) toItuImage.setVisibility(View.GONE);
        if (!msg.toCq) toCqImage.setVisibility(View.GONE);


        //查是不是在本波段内通联成功过的呼号
        if (GeneralVariables.checkQSLCallsign(msg.getCallsignFrom())) {//如果在数据库中，划线
            titleView.setPaintFlags(
                    titleView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {//如果不在数据库中，去掉划线
            titleView.setPaintFlags(
                    titleView.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        }
        boolean otherBandIsQso = GeneralVariables.checkQSLCallsign_OtherBand(msg.getCallsignFrom());

        //是否有与我呼号有关的消息
        if (msg.inMyCall(GeneralVariables.myCallsign)) {
            layout.setBackground(mView.getResources().getDrawable(R.drawable.tracker_new_cq_info_win_style));
            titleView.setTextColor(mapView.getResources().getColor(
                    R.color.message_in_my_call_text_color));
        } else if (otherBandIsQso) {
            //设置在别的波段通联过的消息颜色
            titleView.setTextColor(mapView.getResources().getColor(
                    R.color.fromcall_is_qso_text_color));
        } else {
            titleView.setTextColor(mapView.getResources().getColor(
                    R.color.message_text_color));
        }


        this.mView.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction() == 1) {
                    GridInfoWindow.this.close();
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
            Log.e("GridInfoWindow", "Error trapped, BasicInfoWindow.open, mView is null!");
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
