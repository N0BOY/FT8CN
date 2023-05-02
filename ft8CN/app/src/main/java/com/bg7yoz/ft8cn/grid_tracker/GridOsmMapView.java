package com.bg7yoz.ft8cn.grid_tracker;
/**
 * OsmMapView中画通联线、画网格等操作。地图是sqlite模式，采用离线方式（nightUSGS4Layer）。
 * @author BGY70Z
 * @date 2023-03-20
 */

import static java.lang.Math.PI;
import static java.lang.Math.asin;
import static java.lang.Math.atan;
import static java.lang.Math.cos;
import static java.lang.Math.floor;
import static java.lang.Math.sin;
import static java.lang.Math.tan;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.bg7yoz.ft8cn.Ft8Message;
import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.MainViewModel;
import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.database.DatabaseOpr;
import com.bg7yoz.ft8cn.log.QSLRecordStr;
import com.bg7yoz.ft8cn.maidenhead.MaidenheadGrid;
import com.google.android.gms.maps.model.LatLng;

import org.osmdroid.tileprovider.IRegisterReceiver;
import org.osmdroid.tileprovider.modules.IArchiveFile;
import org.osmdroid.tileprovider.modules.OfflineTileProvider;
import org.osmdroid.tileprovider.tilesource.FileBasedTileSource;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsDisplay;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.milestones.MilestoneLineDisplayer;
import org.osmdroid.views.overlay.milestones.MilestoneLister;
import org.osmdroid.views.overlay.milestones.MilestoneManager;
import org.osmdroid.views.overlay.milestones.MilestoneMeterDistanceSliceLister;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GridOsmMapView {
    private static final String TAG = "GridOsmMapView";

    public enum GridMode {//网格的模式
        QSX, QSO, QSL
    }

    public enum ShowTipsMode {
        ALL, NEW, NONE
    }

    private final MainViewModel mainViewModel;
    //public static int COLOR_QSX = 0x7f0000ff;//红色50%，未通联过
    //public static int COLOR_QSO = 0x7fffff00;//黄色50%，通联过
    //public static int COLOR_QSL = 0x7fff0000;//红色50%，确认过
    private boolean showCQ = true;
    private boolean showQSX = false;

    public final MapView gridMapView;
    private final Context context;
    // public ItemizedIconOverlay<OverlayItem> markerOverlay;
    // private final ArrayList<OverlayItem> markerItems = new ArrayList<>();
    private final ArrayList<GridPolyLine> gridLines = new ArrayList<>();
    private GridPolyLine selectedLine = null;
    private static final int TIME_OUT = 3;
    private int selectLineTimeOut = TIME_OUT;//被选择的画线，停留的周期数
    private final ArrayList<GridPolygon> gridPolygons = new ArrayList<>();
    private final ArrayList<GridMarker> gridMarkers = new ArrayList<>();

    private ShowTipsMode showTipsMode = ShowTipsMode.NEW;

    public GridOsmMapView(Context context, MapView gridMapView, MainViewModel mainViewModel) {
        this.gridMapView = gridMapView;
        this.context = context;
        this.mainViewModel = mainViewModel;
    }


    public void initMap(String grid, boolean offset) {
        mapViewOtherData(gridMapView);//设置内部源
        gridMapView.setMultiTouchControls(true);
        gridMapView.setBuiltInZoomControls(true);//显示缩放按钮
        gridMapView.getZoomController().getDisplay().setPositions(true
                , CustomZoomButtonsDisplay.HorizontalPosition.RIGHT
                , CustomZoomButtonsDisplay.VerticalPosition.BOTTOM);
        gridMapView.setTilesScaledToDpi(true);

        gridMapView.setMaxZoomLevel(6.0);
        gridMapView.setMinZoomLevel(1.0);
        gridMapView.getController().setZoom(1.6);

        gridMapView.setUseDataConnection(true);
        gridMapView.setMultiTouchControls(true);
        gridMapView.getOverlayManager().getTilesOverlay().setEnabled(true);
        gridMapView.setSelected(true);
        setGrayLine();

        //addMarkerOverlay();//添加Marker图层


        //[A-Ra-r]{2}[0-9]{2}[A-Xa-x]{2}，六位梅登海德正则
        // [A-Ra-r]{2}[0-9]{2}，四位梅登海德正则
        LatLng latLng = MaidenheadGrid.gridToLatLng(grid);//做一下判断是不是网格
        if (latLng != null) {
            if (offset) {
                gridMapView.getController().setCenter(new GeoPoint(latLng.latitude
                        , latLng.longitude - 90f));
            } else {
                gridMapView.getController().setCenter(new GeoPoint(latLng.latitude
                        , latLng.longitude));
            }
        }
    }

    /**
     * 缩放到线路的范围之内
     *
     * @param line 线
     */
    public void zoomToLineBound(GridPolyLine line) {
        BoundingBox boundingBox = new BoundingBox();
        selectedLine = line;
        selectLineTimeOut = TIME_OUT;
        line.getOutlinePaint().setColor(gridMapView.getResources().getColor(
                R.color.tracker_select_line_color));
        line.getOutlinePaint().setStrokeWidth(6);
        //mOutlinePaint = getStrokePaint(0xffFF1E27, 3);

        GeoPoint eastNorthPoint = new GeoPoint(line.getActualPoints().get(0).getLatitude()
                , line.getActualPoints().get(0).getLongitude());
        GeoPoint westSouthPoint = new GeoPoint(line.getActualPoints().get(1).getLatitude()
                , line.getActualPoints().get(1).getLongitude());

        if (Math.abs(westSouthPoint.getLongitude() - eastNorthPoint.getLongitude()) > 180) {
            if (eastNorthPoint.getLongitude() > westSouthPoint.getLongitude()) {
                double temp = westSouthPoint.getLongitude();
                westSouthPoint.setLongitude(eastNorthPoint.getLongitude());
                eastNorthPoint.setLongitude(temp);

            }
        } else {
            if (eastNorthPoint.getLongitude() < westSouthPoint.getLongitude()) {
                double temp = westSouthPoint.getLongitude();
                westSouthPoint.setLongitude(eastNorthPoint.getLongitude());
                eastNorthPoint.setLongitude(temp);

            }
        }
        if (eastNorthPoint.getLatitude() < westSouthPoint.getLatitude()) {
            double temp = westSouthPoint.getLatitude();
            westSouthPoint.setLatitude(eastNorthPoint.getLatitude());
            eastNorthPoint.setLatitude(temp);
        }

        boundingBox.set(eastNorthPoint.getLatitude(), eastNorthPoint.getLongitude()
                , westSouthPoint.getLatitude(), westSouthPoint.getLongitude());

        gridMapView.zoomToBoundingBox(boundingBox, true, 100);
    }


    /**
     * 显示CQ的位置
     *
     * @param marker CQ的标记
     * @param offset 是否偏移
     */
    public void gotoCqGrid(GridMarker marker, boolean offset) {
        GeoPoint geoPoint = new GeoPoint(marker.getPosition());
        if (offset) {
            geoPoint.setLongitude(geoPoint.getLongitude() - 40f);
        }
        gridMapView.getController().animateTo(geoPoint, 2.5, 500L);
    }


    public synchronized GridMarker addGridMarker(String grid, Ft8Message msg) {
        //todo 对于4.0的CQ消息，是没有网格信息的，可以以国家的地理位置代替
        if (LatLng2GeoPoint(MaidenheadGrid.gridToLatLng(grid)) == null) return null;
        GridMarker marker = new GridMarker(context, mainViewModel, gridMapView, grid, msg);
        gridMarkers.add(marker);
        return marker;
    }

    /**
     * 清除标记marker
     */
    public synchronized void clearMarkers() {
        for (GridMarker marker : gridMarkers) {
            marker.closeInfoWindow();
            gridMapView.getOverlays().remove(marker);
        }
        gridMarkers.clear();
        gridMapView.invalidate();
    }

    public GridPolyLine getSelectedLine() {
        return selectedLine;
    }

    public void clearSelectedLines() {
        if (selectedLine != null) {
            selectedLine.closeInfoWindow();
            gridMapView.getOverlays().remove(selectedLine);
            selectedLine = null;

        }
    }

    /**
     * 清除线条
     */
    public synchronized void clearLines() {

        boolean isOpening = false;

        if (selectedLine != null) {
            selectLineTimeOut--;
            isOpening = selectedLine.isInfoWindowOpen();
            selectedLine.closeInfoWindow();
            gridMapView.getOverlays().remove(selectedLine);
        }
        for (GridPolyLine line : gridLines) {
            line.closeInfoWindow();
            gridMapView.getOverlays().remove(line);
        }
        gridLines.clear();
        if (selectedLine != null && selectLineTimeOut > 0) {
            gridMapView.getOverlays().add(selectedLine);
            if (isOpening) selectedLine.showInfoWindow();
        }
        gridMapView.invalidate();
    }


    /**
     * 清除网格瓦片
     */
    public synchronized void clearGridPolygon() {
        for (GridPolygon polygon : gridPolygons) {
            gridMapView.getOverlays().remove(polygon);
        }
        gridPolygons.clear();
        gridMapView.invalidate();
    }

    /**
     * 清除全部图层
     */
    public void clearAll() {
        clearMarkers();
        clearLines();
        clearGridPolygon();
    }

    /**
     * 按照网格，查找网格图层，如果没有返回null
     *
     * @param grid 网格
     * @return 图层
     */
    public GridPolygon getGridPolygon(String grid) {
        for (GridPolygon polygon : gridPolygons) {
            if (polygon.grid.equals(grid)) return polygon;
        }
        return null;
    }

    /**
     * 标记、更新新发生消息的网格
     *
     * @param grid      网格
     * @param msg       消息内容
     * @param subDetail 细节
     * @return 网格对象
     */
    public GridPolygon upgradeGridInfo(String grid, String msg, String subDetail) {
        GridPolygon gridPolygon = getGridPolygon(grid);
        if (gridPolygon == null) {
            gridPolygon = addGridPolygon(grid, GridMode.QSX);
        }
        gridPolygon.setSnippet(msg);
        gridPolygon.setSubDescription(subDetail);
        //gridPolygon.showInfoWindow();
        return gridPolygon;
    }

    /**
     * 标记、更新新发生消息的网格
     *
     * @param recordStr 历史记录
     * @return 网格对象
     */
    public GridPolygon upgradeGridInfo(QSLRecordStr recordStr) {
        GridPolygon gridPolygon = getGridPolygon(recordStr.getGridsquare());
        if (gridPolygon == null) {
            if (recordStr.isQSL) {
                gridPolygon = addGridPolygon(recordStr.getGridsquare(), GridMode.QSL);
            } else {
                gridPolygon = addGridPolygon(recordStr.getGridsquare(), GridMode.QSO);
            }
        }

        gridPolygon.setSnippet(String.format(String.format("%s %s",
                String.format(GeneralVariables.getStringFromResource(R.string.qsl_freq)
                        , recordStr.getFreq()),
                String.format(GeneralVariables.getStringFromResource(R.string.qsl_band)
                        , recordStr.getBand()))));

        gridPolygon.setSubDescription(String.format("%s\n%s\n%s  %s\n%s %s",
                String.format(GeneralVariables.getStringFromResource(R.string.qsl_start_time)
                        , recordStr.getTime_on()),
                String.format(GeneralVariables.getStringFromResource(R.string.qsl_end_time)
                        , recordStr.getTime_off()),
                String.format(GeneralVariables.getStringFromResource(R.string.qsl_rst_rcvd)
                        , recordStr.getRst_rcvd()),
                String.format(GeneralVariables.getStringFromResource(R.string.qsl_rst_sent)
                        , recordStr.getRst_sent()),

                String.format(GeneralVariables.getStringFromResource(R.string.qsl_mode)
                        , recordStr.getMode()),
                recordStr.getComment()
        ));
        gridPolygon.setTitle(String.format("%s--%s", recordStr.getCall(), recordStr.getStation_callsign()));//显示消息内容
        gridPolygon.setInfoWindow(new GridRecordInfoWindow(R.layout.tracker_record_info_win, gridMapView));
        return gridPolygon;
    }

    /**
     * 更新地图
     */
    public void mapUpdate(){
        gridMapView.invalidate();
    }

    /**
     * 升级网格状态，如果没有，说明是新的，就添加网格。返回false。如果有，返回true。
     *
     * @param grid     网格
     * @param gridMode 模式
     * @return 发现
     */
    public boolean upgradeGridMode(String grid, GridMode gridMode) {
        GridPolygon polygon = getGridPolygon(grid);
        if (polygon != null) {
            polygon.upgradeGridMode(gridMode);
            return true;
        } else {
            addGridPolygon(grid, gridMode);
            return false;
        }
    }

    /**
     * 添加网格图层
     *
     * @param grid     网格
     * @param gridMode 网格类型
     * @return 返回一个网格图层对象
     */
    public synchronized GridPolygon addGridPolygon(String grid, GridMode gridMode) {
        if (gridMapView == null) return null;
        //here, we create a polygon using polygon class, note that you need 4 points in order to make a rectangle
        GridPolygon polygon = new GridPolygon(context, gridMapView, grid, gridMode);

        gridPolygons.add(polygon);
        gridMapView.getOverlays().add(polygon);

        return polygon;
    }

    /**
     * 查找有没有符合的CQ Marker
     *
     * @param message 消息
     * @return Marker
     */
    public GridMarker getMarker(Ft8Message message) {
        for (GridMarker marker : gridMarkers) {
            if (marker.msg == message) {
                return marker;
            }
        }
        return null;
    }

    /**
     * 查找有没有符合消息的线
     *
     * @param message 消息
     * @return 线
     */
    public GridPolyLine getLine(Ft8Message message) {
        for (GridPolyLine line : gridLines) {
            if (line.msg == message) {
                return line;
            }
        }
        return null;
    }

    /**
     * 在两个网格之间画线。
     *
     * @param message 消息
     * @param db      数据库
     */
    public synchronized GridPolyLine drawLine(Ft8Message message, DatabaseOpr db) {
        LatLng fromLatLng = MaidenheadGrid.gridToLatLng(message.getMaidenheadGrid(db));
        LatLng toLatLng = MaidenheadGrid.gridToLatLng(message.getToMaidenheadGrid(db));
        if (fromLatLng == null) {
            fromLatLng = message.fromLatLng;
        }

        if (toLatLng == null) {
            toLatLng = message.toLatLng;
        }
        if (fromLatLng == null || toLatLng == null) {
            return null;
        }
        final GridPolyLine line = new GridPolyLine(gridMapView, fromLatLng, toLatLng, message);
        gridLines.add(line);
        return line;
    }

    public synchronized GridPolyLine drawLine(QSLRecordStr recordStr) {
        LatLng fromLatLng = MaidenheadGrid.gridToLatLng(recordStr.getGridsquare());
        LatLng toLatLng = MaidenheadGrid.gridToLatLng(recordStr.getMy_gridsquare());
        if (fromLatLng == null) {
            //todo 把呼号转为国家的经纬度
            return null;
            //fromLatLng = message.fromLatLng;
        }

        if (toLatLng == null) {
            //todo 把呼号转为国家的经纬度
            return null;
            //toLatLng = message.toLatLng;
        }
        final GridPolyLine line = new GridPolyLine(gridMapView, fromLatLng, toLatLng, recordStr);
        return line;
    }

    /**
     * 设定地图的离线来源
     *
     * @param mapView osmMap
     */
    public void mapViewOtherData(MapView mapView) {
        //可以根据时间不同，显示不同的地图
        String strFilepath = getAssetsCacheFile(context, context.getString(R.string.map_name));
        File exitFile = new File(strFilepath);
        if (!exitFile.exists()) {
            mapView.setTileSource(TileSourceFactory.USGS_SAT);
        } else {
            OfflineTileProvider tileProvider = new OfflineTileProvider(
                    (IRegisterReceiver) new SimpleRegisterReceiver(context), new File[]{exitFile});
            mapView.setTileProvider(tileProvider);
            String source = "";
            IArchiveFile[] archives = tileProvider.getArchives();
            if (archives.length > 0) {
                Set<String> tileSources = archives[0].getTileSources();
                if (!tileSources.isEmpty()) {
                    source = tileSources.iterator().next();
                    mapView.setTileSource(FileBasedTileSource.getSource(source));
                } else {
                    mapView.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
                }
            } else
                mapView.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
            mapView.invalidate();

        }
    }

    /**
     * 获取Assets目录，这里面保存着地图文件
     *
     * @param context  context
     * @param fileName 地图文件名，sqlite格式
     * @return 包含全路径的文件名
     */
    public String getAssetsCacheFile(Context context, String fileName) {
        File cacheFile = new File(context.getCacheDir(), fileName);
        try {
            InputStream inputStream = context.getAssets().open(fileName);
            try {
                FileOutputStream outputStream = new FileOutputStream(cacheFile);
                try {
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = inputStream.read(buf)) > 0) {
                        outputStream.write(buf, 0, len);
                    }
                } finally {
                    outputStream.close();
                }
            } finally {
                inputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return cacheFile.getAbsolutePath();
    }

    public static GeoPoint LatLng2GeoPoint(LatLng latLng) {
        if (latLng == null) return null;
        return new GeoPoint(latLng.latitude, latLng.longitude);
    }

    public static ArrayList<GeoPoint> LatLngs2GeoPoints(LatLng[] latLngs) {
        ArrayList<GeoPoint> geoPoints = new ArrayList<>();
        if (latLngs != null) {
            for (int i = 0; i < latLngs.length; i++) {
                geoPoints.add(LatLng2GeoPoint(latLngs[i]));
            }
        }
        return geoPoints;
    }

    public static class GridPolyLine extends Polyline {
        //public String fromGrid;
        //public String toGrid;
        public Ft8Message msg;
        public QSLRecordStr recorder;
        //public boolean marked = false;

        @SuppressLint("DefaultLocale")
        public GridPolyLine(MapView mapView, LatLng fromLatLng, LatLng toLatLng, QSLRecordStr recordStr) {
            super(mapView);
            this.recorder = recordStr;
            setSnippet(String.format(String.format("%s %s",
                    String.format(GeneralVariables.getStringFromResource(R.string.qsl_freq)
                            , recordStr.getFreq()),
                    String.format(GeneralVariables.getStringFromResource(R.string.qsl_band)
                            , recordStr.getBand()))));

            setSubDescription(String.format("%s\n%s\n%s  %s\n%s %s",
                    String.format(GeneralVariables.getStringFromResource(R.string.qsl_start_time)
                            , recordStr.getTime_on()),
                    String.format(GeneralVariables.getStringFromResource(R.string.qsl_end_time)
                            , recordStr.getTime_off()),
                    String.format(GeneralVariables.getStringFromResource(R.string.qsl_rst_rcvd)
                            , recordStr.getRst_rcvd()),
                    String.format(GeneralVariables.getStringFromResource(R.string.qsl_rst_sent)
                            , recordStr.getRst_sent()),

                    String.format(GeneralVariables.getStringFromResource(R.string.qsl_mode)
                            , recordStr.getMode()),
                    recordStr.getComment()
            ));
            setTitle(String.format("%s--%s", recordStr.getCall(), recordStr.getStation_callsign()));//显示消息内容
            this.mOutlinePaint = getStrokePaint(
                    mapView.getResources().getColor(
                            R.color.tracker_history_line_color), 3);
            List<GeoPoint> pts = new ArrayList<>();
            pts.add(GridOsmMapView.LatLng2GeoPoint(fromLatLng));
            pts.add(GridOsmMapView.LatLng2GeoPoint(toLatLng));


            setPoints(pts);
            setGeodesic(true);
            setInfoWindow(new GridRecordInfoWindow(R.layout.tracker_record_info_win, mapView));
            mapView.getOverlayManager().add(this);
        }

        @SuppressLint("DefaultLocale")
        public GridPolyLine(MapView mapView, LatLng fromLatLng, LatLng toLatLng, Ft8Message msg) {
            super(mapView);
            this.msg = msg;

            setSnippet(String.format("%s<--%s", msg.toWhere, msg.fromWhere));//表示距离
            setSubDescription(String.format("%dBm , %.1f ms , %s"
                    , msg.snr, msg.time_sec
                    , MaidenheadGrid.getDistLatLngStr(fromLatLng, toLatLng)));
            setTitle(msg.getMessageText());//显示消息内容
            if (msg.inMyCall()) {
                this.mOutlinePaint = getStrokePaint(
                        mapView.getResources().getColor(
                                R.color.tracker_in_my_line_color), 3);
            } else {
                this.mOutlinePaint = getStrokePaint(mapView.getResources().getColor(
                        R.color.tracker_line_color), 3);
            }

            List<GeoPoint> pts = new ArrayList<>();
            pts.add(GridOsmMapView.LatLng2GeoPoint(fromLatLng));
            pts.add(GridOsmMapView.LatLng2GeoPoint(toLatLng));


            setPoints(pts);
            setGeodesic(true);
            setInfoWindow(new GridInfoWindow(R.layout.tracker_grid_info_win, mapView, msg));
            mapView.getOverlayManager().add(this);
            //showInfoWindow();

            final float lineLen = (float) getDistance();
            final float pointLen = lineLen / 10f > 200000 ? 200000f : lineLen / 10f;

            final List<MilestoneManager> managers = new ArrayList<>();

            final MilestoneMeterDistanceSliceLister slicerForPath = new MilestoneMeterDistanceSliceLister();
            managers.add(getAnimatedPathManager(slicerForPath));

            setMilestoneManagers(managers);


            //设置方向动画
            final ValueAnimator percentageCompletion = ValueAnimator.ofFloat(0, 1); // 10 kilometers

            percentageCompletion.setRepeatCount(ValueAnimator.INFINITE);
            percentageCompletion.setDuration(1000); // 1 seconds
            percentageCompletion.setStartDelay(0); // 1 second

            percentageCompletion.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    double dist = ((float) animation.getAnimatedValue()) * lineLen;
                    double distStart = dist - pointLen;
                    if (distStart < 0) distStart = 0;
                    slicerForPath.setMeterDistanceSlice(distStart, dist);
                    mapView.invalidate();
                }
            });
            percentageCompletion.start();
        }

        /**
         * 线条的动画点，设置：绿色，10f宽度
         */
        private MilestoneManager getAnimatedPathManager(final MilestoneLister pMilestoneLister) {
            final Paint slicePaint = getStrokePaint(Color.GREEN, 15f);
            return new MilestoneManager(pMilestoneLister, new MilestoneLineDisplayer(slicePaint));
        }

        private Paint getStrokePaint(final int pColor, final float pWidth) {
            Paint paint = new Paint();
            paint.setStrokeWidth(pWidth);
            paint.setStyle(Paint.Style.STROKE);
            paint.setAntiAlias(true);
            paint.setColor(pColor);
            //paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setPathEffect(new DashPathEffect(new float[]{20, 10}, 0));
            return paint;
        }

        public void showNewInfo() {
            if (msg != null) {
                if ((msg.fromDxcc || msg.fromItu || msg.fromCq)
                        && !GeneralVariables.checkQSLCallsign(msg.callsignFrom)) {
                    showInfoWindow();
                }
            }
            if (recorder != null) {
                showInfoWindow();
            }

        }
    }

    public static class GridPolygon extends Polygon {
        public String grid;
        public GridMode gridMode;
        private final Context context;
        //private BasicInfoWindow infoWindow;
        //public String details;

        public GridPolygon(Context context, MapView mapView, String grid, GridMode gridMode) {
            super(mapView);
            this.grid = grid;
            this.gridMode = gridMode;
            this.context = context;
            //infoWindow=new BasicInfoWindow(R.layout.tracker_grid_info_win,mapView);
            //this.details = details;

            setTitle(grid);
            //setSubDescription(details);
            setStrokeWidth(3f);
            setStrokeColor(this.context.getColor(R.color.osm_grid_out_line_color));
            //setSnippet("445534343");

            updateGridMode();

            ArrayList<GeoPoint> pts = LatLngs2GeoPoints(MaidenheadGrid.gridToPolygon(grid));
            setPoints(pts);


            setVisible(true);

        }

        public void updateGridMode() {
            switch (gridMode) {
                case QSL:
                    setFillColor(this.context.getColor(R.color.tracker_sample_qsl_color));
                    break;
                case QSO:
                    setFillColor(this.context.getColor(R.color.tracker_sample_qso_color));
                    break;
                case QSX:
                    setFillColor(this.context.getColor(R.color.tracker_sample_qsx_color));
                    break;
            }
        }

        public void upgradeGridMode(GridMode mode) {
            if (mode.ordinal() > gridMode.ordinal()) {
                gridMode = mode;
                updateGridMode();
            }
        }
    }

    public static class GridMarker extends Marker {
        public String grid;
        private final Context context;
        private final Ft8Message msg;

        @SuppressLint({"UseCompatLoadingForDrawables", "DefaultLocale"})
        public GridMarker(Context context, MainViewModel mainViewModel, MapView mapView
                , String grid, Ft8Message msg) {
            super(mapView);
            this.grid = grid;
            this.context = context;
            this.msg = msg;

            this.setPosition(LatLng2GeoPoint(MaidenheadGrid.gridToLatLng(grid)));
            this.setAnchor(ANCHOR_CENTER, ANCHOR_BOTTOM);
            this.setInfoWindow(new GridMarkerInfoWindow(mainViewModel
                    , R.layout.tracker_cq_marker_info_win, mapView, msg));
            setSnippet(String.format("%d dBm , %.1f ms", msg.snr, msg.time_sec));
            setSubDescription(String.format("%s , %s"
                    , MaidenheadGrid.getDistStr(grid, GeneralVariables.getMyMaidenheadGrid())
                    , msg.fromWhere));//表示距离
            setTitle(msg.getMessageText());//显示消息内容


            @SuppressLint("UseCompatLoadingForDrawables")
            Drawable d;
            if (GeneralVariables.checkQSLCallsign(msg.callsignFrom)) {
                d = context.getDrawable(R.drawable.ic_baseline_cq_qso_24).mutate();
                d.setColorFilter(context.getColor(R.color.tracker_cq_marker_is_qso_color)
                        , PorterDuff.Mode.SRC_ATOP);

            } else {
                d = context.getDrawable(R.drawable.ic_baseline_cq_24).mutate();
            }
            if (GeneralVariables.checkQSLCallsign_OtherBand(msg.callsignFrom)) {
                d.setColorFilter(context.getColor(R.color.tracker_cq_marker_other_is_qso_color)
                        , PorterDuff.Mode.SRC_ATOP);
            }
            setIcon(d);

            //this.showInfoWindow();
            mapView.getOverlays().add(this);
        }

        public void showNewInfo() {
            if ((msg.fromDxcc || msg.fromItu || msg.fromCq || (msg.checkIsCQ()))
                    && !GeneralVariables.checkQSLCallsign(msg.callsignFrom)) {
                showInfoWindow();
            }
        }
    }


    /**
     * 显示提示，根据显示模式来显示。
     */
    public void showInfoWindows() {
        setShowTipsMode(showTipsMode);
    }

    /**
     * 显示全部提示
     */
    public void showAllInfoWindows() {
        if (showQSX) {
            for (GridPolyLine line : gridLines) {
                line.showInfoWindow();
            }
        }
        if (showCQ) {
            for (GridMarker marker : gridMarkers) {
                marker.showInfoWindow();
            }
        }
    }

    /**
     * 只显示新的提示
     */
    public void showNewInfoWindows() {
        if (showQSX) {
            for (GridPolyLine line : gridLines) {
                line.showNewInfo();
            }
        }
        if (showCQ) {
            for (GridMarker marker : gridMarkers) {
                marker.showNewInfo();
            }
        }
    }

    /**
     * 关闭全部提示窗口
     */
    public void hideInfoWindows() {
        for (GridPolygon polygon : gridPolygons
        ) {
            polygon.closeInfoWindow();
        }
        for (GridPolyLine line : gridLines) {
            line.closeInfoWindow();
        }
        for (GridMarker marker : gridMarkers) {
            marker.closeInfoWindow();
        }
        gridMapView.invalidate();
    }


    public void setShowCQ(boolean showCQ) {
        this.showCQ = showCQ;
        showInfoWindows();
    }

    public void setShowQSX(boolean showQSX) {
        this.showQSX = showQSX;
        showInfoWindows();
    }

    public void setShowTipsMode(ShowTipsMode showTipsMode) {
        this.showTipsMode = showTipsMode;
        hideInfoWindows();
        switch (this.showTipsMode) {
            case ALL:
                showAllInfoWindows();
                break;
            case NEW:
                showNewInfoWindows();
                break;
            case NONE:
                break;
        }
    }


    private static double[] computeDayNightTerminator(long t) {
        // The nice thing about the java time standard is that converting it
        // to a julian date is trivial - unlike the gyrations the original
        // matlab code had to go through to convert the y/n/d/h/m/s parameters
        final double julianDate1970 = t / (double) (1000 * 60 * 60 * 24);
        // convert from the unix epoch to the astronomical epoch
        // (noon on January 1, 4713 BC, GMT/UT) (the .5 is noon versus midnight)
        final double juliandate = julianDate1970 + 2440587.500000;
        final double K = PI / 180;
        // here be dragons!
        final double T = (juliandate - 2451545.0) / 36525;
        double L = 280.46645 + 36000.76983 * T + 0.0003032 * T * T;
        L = L % 360;
        if (L < 0)
            L = L + 360;
        double M = 357.52910 + 35999.05030 * T - 0.0001559 * T * T -
                0.00000048 * T * T * T;
        M = M % 360;
        if (M < 0)
            M = M + 360;
        final double C = (1.914600 - 0.004817 * T - 0.000014 * T * T) * sin(K * M) +
                (0.019993 - 0.000101 * T) * sin(K * 2 * M) +
                0.000290 * sin(K * 3 * M);
        final double theta = L + C;
        final double LS = L;
        final double LM = 218.3165 + 481267.8813 * T;
        final double eps0 = 23.0 + 26.0 / 60.0 + 21.448 / 3600.0 -
                (46.8150 * T +
                        0.00059 * T * T - 0.001813 * T * T * T) / 3600;
        final double omega = 125.04452 - 1934.136261 * T + 0.0020708 * T * T +
                T * T *
                        T / 450000;
        final double deltaEps =
                (9.20 * cos(K * omega) + 0.57 * cos(K * 2 * LS) +
                        0.10 * cos(K * 2 * LM) - 0.09 * cos(K * 2 * omega)) / 3600;
        final double eps = eps0 + deltaEps + 0.00256 *
                cos(K * (125.04 - 1934.136 * T));
        final double lambda = theta - 0.00569 - 0.00478 * sin(K * (125.04 -
                1934.136 *
                        T));
        final double delta = asin(sin(K * eps) * sin(K * lambda));
        final double dec = delta / K;
        final double tau = (juliandate - floor(juliandate)) * 360;
        double[] coords = new double[361];
        for (int i = 0; i < 361; i++)
            coords[i] = atan(cos((i - 180 + tau) * K) / tan(dec * K)) / K + 90;
        return coords;
    }

    /**
     * 根据当前时间画灰线
     */
    public void setGrayLine() {
        double[] lats = computeDayNightTerminator(System.currentTimeMillis());
        LatLng[] grayLine = new LatLng[lats.length * 3];
        for (int i = 0; i < lats.length; i++) {
            grayLine[i] = new LatLng((lats[i] - 90), i);
            grayLine[lats.length + i] = new LatLng((lats[i] - 90), i);
            grayLine[lats.length * 2 + i] = new LatLng((lats[i] - 90), i);
        }

        Polyline line = new Polyline(gridMapView);
        line.setWidth(15f);
        line.setColor(context.getColor(R.color.tracker_gray_line_color));

        List<GeoPoint> pts = new ArrayList<>();
        for (int i = 0; i < grayLine.length; i++) {
            pts.add(GridOsmMapView.LatLng2GeoPoint(grayLine[i]));
        }
        line.setInfoWindow(null);
        line.setPoints(pts);
        gridMapView.getOverlays().add(line);

    }

}
