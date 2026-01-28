package com.bg7yoz.ft8cn.ui;
/**
 * Radio make (brand) list adapter for spinner.
 * @author BGY70Z
 * @date 2025-01-27
 */

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.database.RigNameList;

import java.util.ArrayList;

public class RigMakeSpinnerAdapter extends BaseAdapter {
    private final Context mContext;
    private final RigNameList rigNameList;
    private final ArrayList<String> makes;

    public RigMakeSpinnerAdapter(Context context) {
        rigNameList = RigNameList.getInstance(context);
        mContext = context;
        makes = rigNameList.getUniqueMakes();
    }

    @Override
    public int getCount() {
        return makes.size();
    }

    @Override
    public Object getItem(int i) {
        return makes.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    /**
     * Get the make name at the given position.
     * @param position Position in the spinner
     * @return Make name (empty string for "None")
     */
    public String getMake(int position) {
        if (position < 0 || position >= makes.size()) {
            return "";
        }
        return makes.get(position);
    }

    /**
     * Find the position of a make in the list.
     * @param make Make name to find
     * @return Position in the list, or 0 if not found
     */
    public int getPosition(String make) {
        if (make == null || make.isEmpty()) {
            return 0;
        }
        for (int i = 0; i < makes.size(); i++) {
            if (makes.get(i).equals(make)) {
                return i;
            }
        }
        return 0;
    }

    @SuppressLint({"ViewHolder", "InflateParams"})
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        LayoutInflater layoutInflater = LayoutInflater.from(mContext);
        view = layoutInflater.inflate(R.layout.rig_name_spinner_item, null);
        if (view != null) {
            TextView textView = view.findViewById(R.id.rigNameItemTextView);
            String make = makes.get(i);
            if (make.isEmpty()) {
                textView.setText(GeneralVariables.getStringFromResource(R.string.none));
            } else {
                textView.setText(make);
            }
            // Hide logo for make spinner
            view.findViewById(R.id.rigLogoImageView).setVisibility(View.GONE);
        }
        return view;
    }
}
