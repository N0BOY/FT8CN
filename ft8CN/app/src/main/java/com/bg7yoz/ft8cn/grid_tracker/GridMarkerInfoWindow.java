package com.bg7yoz.ft8cn.grid_tracker;

import android.annotation.SuppressLint;
import android.graphics.Paint;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.bg7yoz.ft8cn.Ft8Message;
import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.MainViewModel;
import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.ui.ToastMessage;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.OverlayWithIW;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

public class GridMarkerInfoWindow extends InfoWindow {
    public static final int UNDEFINED_RES_ID = 0;

    private final TextView titleView;
    private final TextView descriptionView;
    private final TextView subDescriptionView;
    private final MainViewModel mainViewModel;
    private Ft8Message msg;


    @SuppressLint("UseCompatLoadingForDrawables")
    public GridMarkerInfoWindow(MainViewModel mainViewModel,int layoutResId, MapView mapView, Ft8Message msg) {
        super(layoutResId, mapView);
        this.mainViewModel=mainViewModel;
        this.msg=msg;
        //setResIds(mapView.getContext());
        titleView = (TextView) this.mView.findViewById(R.id.tracker_marker_info_bubble_title);
        descriptionView = (TextView) this.mView.findViewById(R.id.tracker_marker_info_bubble_description);
        subDescriptionView = (TextView) this.mView.findViewById(R.id.tracker_marker_info_bubble_subdescription);
        ImageView fromDxccImage = (ImageView) this.mView.findViewById(R.id.track_marker_from_dxcc_image);
        ImageView fromItuImage = (ImageView) this.mView.findViewById(R.id.track_marker_from_itu_image);
        ImageView fromCqImage = (ImageView) this.mView.findViewById(R.id.track_marker_from_cq_image);
        if (!msg.fromDxcc) fromDxccImage.setVisibility(View.GONE);
        if (!msg.fromItu) fromItuImage.setVisibility(View.GONE);
        if (!msg.fromCq) fromCqImage.setVisibility(View.GONE);

        ConstraintLayout layout=(ConstraintLayout) mView.findViewById(R.id.trackerMarkerConstraintLayout);
        if (msg.fromCq||msg.fromItu||msg.fromDxcc){//如果是没有通联过的区域，把颜色改成红色
            layout.setBackground(mView.getResources().getDrawable(R.drawable.tracker_new_cq_info_win_style));
            ToastMessage.show(String.format(GeneralVariables.getStringFromResource(
                    (R.string.tracker_new_zone_found)),msg.getMessageText()));
        }



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






        ImageButton imageButton=(ImageButton) this.mView.findViewById(R.id.callThisImageButton);
        if (GeneralVariables.myCallsign.equals(msg.getCallsignFrom())){
            imageButton.setVisibility(View.GONE);
        }

        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doCallNow();
            }
        });
        this.mView.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction() == 1) {
                    GridMarkerInfoWindow.this.close();
                }
                return true;
            }
        });
    }
    /**
     * 马上对发起者呼叫
     *
     */
    //@RequiresApi(api = Build.VERSION_CODES.N)
    private void doCallNow() {
        mainViewModel.addFollowCallsign(msg.getCallsignFrom());
        if (!mainViewModel.ft8TransmitSignal.isActivated()) {
            mainViewModel.ft8TransmitSignal.setActivated(true);
            GeneralVariables.transmitMessages.add(msg);//把消息添加到关注列表中
        }
        //呼叫发启者
        mainViewModel.ft8TransmitSignal.setTransmit(msg.getFromCallTransmitCallsign()
                , 1, msg.extraInfo);
        mainViewModel.ft8TransmitSignal.transmitNow();

        GeneralVariables.resetLaunchSupervision();//复位自动监管
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
