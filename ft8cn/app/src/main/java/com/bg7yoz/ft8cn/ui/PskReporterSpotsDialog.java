package com.bg7yoz.ft8cn.ui;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.MainViewModel;
import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.psk.PskReporterMqtt;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Dialog that shows PSK Reporter reception reports (spots) for the user's callsign
 * via MQTT (similar to GridTracker2). Displays receiver callsign, grid, frequency,
 * and time of last heard. Spots are received in real-time via MQTT.
 */
public class PskReporterSpotsDialog extends Dialog {

    private final Activity activity;
    private TextView titleText;
    private TextView spotsText;
    private Button refreshButton;
    private PskReporterMqtt mqtt;
    private MainViewModel mainViewModel;
    private boolean usingMainViewModelMqtt = false;
    private final List<PskReporterMqtt.Spot> spots = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable spotUpdateRunnable = new Runnable() {
        @Override
        public void run() {
            if (usingMainViewModelMqtt && mqtt != null) {
                // Poll spots from MainViewModel's MQTT connection
                List<PskReporterMqtt.Spot> receivedSpots = mqtt.getReceivedSpots();
                synchronized (spots) {
                    spots.clear();
                    spots.addAll(receivedSpots);
                }
                updateSpotsDisplay();
            }
            // Schedule next update
            handler.postDelayed(this, 2000); // Update every 2 seconds
        }
    };
    private static final SimpleDateFormat TIME_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US);

    static {
        TIME_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public PskReporterSpotsDialog(@NonNull android.content.Context context, Activity activity) {
        super(context, R.style.HelpDialog);
        this.activity = activity;
        
        // Get MainViewModel from activity if it's a FragmentActivity
        if (activity instanceof androidx.fragment.app.FragmentActivity) {
            this.mainViewModel = MainViewModel.getInstance((androidx.lifecycle.ViewModelStoreOwner) activity);
        }
        
        // Check if MainViewModel already has an active MQTT connection
        if (mainViewModel != null && mainViewModel.pskReporterMqtt != null && 
            mainViewModel.pskReporterMqtt.isConnected()) {
            // Use existing connection from MainViewModel
            this.mqtt = mainViewModel.pskReporterMqtt;
            this.usingMainViewModelMqtt = true;
        } else {
            // Create new MQTT instance for dialog-only use
            this.mqtt = new PskReporterMqtt();
            this.usingMainViewModelMqtt = false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.psk_reporter_spots_dialog);
        titleText = findViewById(R.id.pskSpotsTitle);
        spotsText = findViewById(R.id.pskSpotsText);
        refreshButton = findViewById(R.id.pskSpotsRefreshButton);

        String call = GeneralVariables.myCallsign != null ? GeneralVariables.myCallsign : "";
        titleText.setText(getContext().getString(R.string.psk_reporter_spots_title, call.isEmpty() ? "?" : call));
        spotsText.setText(getContext().getString(R.string.psk_reporter_spots_loading));

        // Change button text to "Clear" for MQTT mode
        refreshButton.setText("Clear");
        refreshButton.setOnClickListener(v -> {
            synchronized (spots) {
                spots.clear();
                if (mqtt != null) {
                    mqtt.clearSpots();
                }
            }
            updateSpotsDisplay();
        });

        // Start MQTT connection only if we're not using MainViewModel's connection
        if (!usingMainViewModelMqtt) {
            startMqtt();
        } else {
            // If using MainViewModel's connection, load existing spots and start polling
            if (mqtt != null) {
                synchronized (spots) {
                    spots.clear();
                    spots.addAll(mqtt.getReceivedSpots());
                }
                spotsText.setText("Using existing connection. Waiting for spots...");
                updateSpotsDisplay();
                // Start polling for new spots
                handler.postDelayed(spotUpdateRunnable, 2000);
            }
        }
    }

    private void startMqtt() {
        spotsText.setText("Connecting to MQTT...");
        
        mqtt.start(new PskReporterMqtt.Callback() {
            @Override
            public void onSpotReceived(PskReporterMqtt.Spot spot) {
                activity.runOnUiThread(() -> {
                    synchronized (spots) {
                        // Add spot if not already present (deduplicate by callsign+freq)
                        boolean found = false;
                        for (PskReporterMqtt.Spot s : spots) {
                            if (s.receiverCallsign.equals(spot.receiverCallsign) && 
                                s.frequency == spot.frequency) {
                                // Update existing spot with newer timestamp
                                if (spot.flowStartSeconds > s.flowStartSeconds) {
                                    spots.remove(s);
                                    spots.add(spot);
                                }
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            spots.add(spot);
                        }
                        
                        // Keep only most recent 100 spots
                        if (spots.size() > 100) {
                            spots.sort((a, b) -> Long.compare(b.flowStartSeconds, a.flowStartSeconds));
                            spots.subList(100, spots.size()).clear();
                        }
                    }
                    updateSpotsDisplay();
                });
            }

            @Override
            public void onConnected() {
                activity.runOnUiThread(() -> {
                    spotsText.setText("Connected. Waiting for spots...");
                    updateSpotsDisplay();
                });
            }

            @Override
            public void onDisconnected() {
                activity.runOnUiThread(() -> {
                    spotsText.setText("Disconnected from MQTT");
                });
            }

            @Override
            public void onError(String message) {
                activity.runOnUiThread(() -> {
                    spotsText.setText(getContext().getString(R.string.psk_reporter_spots_error, message));
                });
            }
        });
    }

    private void updateSpotsDisplay() {
        synchronized (spots) {
            if (spots.isEmpty()) {
                if (mqtt.isConnected()) {
                    spotsText.setText("Connected. No spots received yet...");
                } else {
                    spotsText.setText("Not connected");
                }
                return;
            }

            // Sort by time (most recent first)
            List<PskReporterMqtt.Spot> sortedSpots = new ArrayList<>(spots);
            sortedSpots.sort((a, b) -> Long.compare(b.flowStartSeconds, a.flowStartSeconds));

            StringBuilder sb = new StringBuilder();
            sb.append("Connected via MQTT - ").append(sortedSpots.size()).append(" spot(s)\n\n");
            sb.append(getContext().getString(R.string.psk_reporter_spots_header)).append("\n\n");
            
            // Show most recent 50 spots
            int count = Math.min(50, sortedSpots.size());
            for (int i = 0; i < count; i++) {
                PskReporterMqtt.Spot s = sortedSpots.get(i);
                String timeStr = formatLastHeard(s.flowStartSeconds);
                sb.append(String.format(Locale.US, "%-10s %-6s %7d Hz   Last heard: %s\n",
                        s.receiverCallsign,
                        s.receiverLocator.isEmpty() ? "-" : s.receiverLocator,
                        s.frequency,
                        timeStr));
            }
            if (sortedSpots.size() > 50) {
                sb.append(String.format("\n... and %d more spots", sortedSpots.size() - 50));
            }
            spotsText.setText(sb.toString());
        }
    }

    private static String formatLastHeard(long flowStartSeconds) {
        if (flowStartSeconds <= 0) return "-";
        long nowSec = System.currentTimeMillis() / 1000;
        long diffSec = nowSec - flowStartSeconds;
        if (diffSec < 60) return diffSec + " s ago";
        if (diffSec < 3600) return (diffSec / 60) + " min ago";
        if (diffSec < 86400) return (diffSec / 3600) + " h ago";
        return TIME_FORMAT.format(new Date(flowStartSeconds * 1000));
    }

    @Override
    public void show() {
        super.show();
        WindowManager.LayoutParams params = getWindow() != null ? getWindow().getAttributes() : null;
        if (params != null && getWindow() != null) {
            int height = getWindow().getWindowManager().getDefaultDisplay().getHeight();
            int width = getWindow().getWindowManager().getDefaultDisplay().getWidth();
            params.width = (int) (width * 0.85);
            params.height = (int) (height * 0.6);
            getWindow().setAttributes(params);
        }
    }

    @Override
    public void dismiss() {
        // Stop polling if we were using MainViewModel's connection
        if (usingMainViewModelMqtt) {
            handler.removeCallbacks(spotUpdateRunnable);
        }
        // Only stop MQTT if we created our own instance (not using MainViewModel's)
        if (!usingMainViewModelMqtt && mqtt != null) {
            mqtt.stop();
        }
        super.dismiss();
    }
}
