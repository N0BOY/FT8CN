package com.bg7yoz.ft8cn.ui;
/**
 * NTP服务器列表适配器
 * NTP Server Spinner Adapter
 * @author BGY70Z
 * @date 2026-01-28
 */

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.bg7yoz.ft8cn.R;

import java.util.ArrayList;
import java.util.List;

public class NtpServerSpinnerAdapter extends BaseAdapter {
    private List<String> ntpServers = new ArrayList<>();
    private Context mContext;

    public NtpServerSpinnerAdapter(Context context) {
        mContext = context;
        // Add common NTP servers
        ntpServers.add("time.windows.com"); // Microsoft (default)
        ntpServers.add("pool.ntp.org"); // NTP Pool Project
        ntpServers.add("time.nist.gov"); // NIST
        ntpServers.add("time.google.com"); // Google
        ntpServers.add("time.cloudflare.com"); // Cloudflare
        ntpServers.add("0.pool.ntp.org"); // NTP Pool 0
        ntpServers.add("1.pool.ntp.org"); // NTP Pool 1
        ntpServers.add("2.pool.ntp.org"); // NTP Pool 2
        ntpServers.add("3.pool.ntp.org"); // NTP Pool 3
        ntpServers.add("time.apple.com"); // Apple
        ntpServers.add("time1.google.com"); // Google Time 1
        ntpServers.add("time2.google.com"); // Google Time 2
        ntpServers.add("time3.google.com"); // Google Time 3
        ntpServers.add("time4.google.com"); // Google Time 4
        ntpServers.add(""); // Empty string for "Custom" option
    }

    @Override
    public int getCount() {
        return ntpServers.size();
    }

    @Override
    public Object getItem(int i) {
        return ntpServers.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @SuppressLint({"ViewHolder", "InflateParams"})
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        LayoutInflater layoutInflater = LayoutInflater.from(mContext);
        view = layoutInflater.inflate(R.layout.ntp_server_spinner_item, null);
        if (view != null) {
            TextView textView = (TextView) view.findViewById(R.id.ntpServerItemTextView);
            String server = ntpServers.get(i);
            if (server.isEmpty()) {
                textView.setText(mContext.getString(R.string.ntp_server_custom));
            } else {
                textView.setText(server);
            }
        }
        return view;
    }

    /**
     * Get the server address at the given position
     * @param position Position in the list
     * @return Server address, or empty string for custom
     */
    public String getServerAddress(int position) {
        if (position >= 0 && position < ntpServers.size()) {
            return ntpServers.get(position);
        }
        return "";
    }

    /**
     * Find the position of a server address in the list
     * @param serverAddress Server address to find
     * @return Position, or -1 if not found
     */
    public int findServerPosition(String serverAddress) {
        if (serverAddress == null || serverAddress.isEmpty()) {
            // Return position of custom option (last item)
            return ntpServers.size() - 1;
        }
        for (int i = 0; i < ntpServers.size(); i++) {
            if (ntpServers.get(i).equals(serverAddress)) {
                return i;
            }
        }
        // Not found in list, return custom position
        return ntpServers.size() - 1;
    }
}
