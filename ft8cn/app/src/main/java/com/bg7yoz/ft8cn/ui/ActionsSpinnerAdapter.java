package com.bg7yoz.ft8cn.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.bg7yoz.ft8cn.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for the Actions dropdown spinner on QSO logs page
 */
public class ActionsSpinnerAdapter extends BaseAdapter {
    public enum Action {
        EXPORT(0, R.string.export),
        SHARE_LOGS(1, R.string.share_logs),
        MAP_LOCATION(2, R.string.map_location_image),
        UPLOAD_QRZ(3, R.string.upload_qrz_unuploaded),
        FILTER(4, R.string.filter),
        STATISTICS(5, R.string.statistical);

        private final int id;
        private final int stringResource;

        Action(int id, int stringResource) {
            this.id = id;
            this.stringResource = stringResource;
        }

        public int getId() {
            return id;
        }

        public int getStringResource() {
            return stringResource;
        }
    }

    private List<Action> actions = new ArrayList<>();
    private Context mContext;

    public ActionsSpinnerAdapter(Context context) {
        mContext = context;
        // Add prompt as first item (will show "Actions")
        actions.add(null); // Placeholder for prompt
        // Add all actions in order
        actions.add(Action.EXPORT);
        actions.add(Action.SHARE_LOGS);
        actions.add(Action.MAP_LOCATION);
        actions.add(Action.UPLOAD_QRZ);
        actions.add(Action.FILTER);
        actions.add(Action.STATISTICS);
    }

    @Override
    public int getCount() {
        return actions.size();
    }

    @Override
    public Object getItem(int position) {
        return actions.get(position);
    }

    @Override
    public long getItemId(int position) {
        Action action = actions.get(position);
        if (action == null) {
            return -1; // Return -1 for prompt item
        }
        return action.getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.actions_spinner_item, parent, false);
        }

        TextView textView = convertView.findViewById(R.id.actionItemTextView);
        Action action = actions.get(position);
        if (action == null) {
            // Show "Actions" prompt
            textView.setText(mContext.getString(R.string.actions));
        } else {
            textView.setText(mContext.getString(action.getStringResource()));
        }

        return convertView;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getView(position, convertView, parent);
    }

    public Action getAction(int position) {
        if (position < 0 || position >= actions.size()) {
            return null;
        }
        return actions.get(position);
    }
}
