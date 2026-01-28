package com.bg7yoz.ft8cn.ui;
/**
 * Radio model list adapter for spinner, filtered by make.
 * @author BGY70Z
 * @date 2025-01-27
 */

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.database.RigNameList;

import java.util.ArrayList;

public class RigModelSpinnerAdapter extends BaseAdapter {
    private final Context mContext;
    private final RigNameList rigNameList;
    private String selectedMake = "";
    private ArrayList<Integer> filteredIndices = new ArrayList<>();

    public RigModelSpinnerAdapter(Context context) {
        rigNameList = RigNameList.getInstance(context);
        mContext = context;
        updateFilter("");
    }

    /**
     * Update the filter based on selected make.
     * @param make The make to filter by (empty string shows all)
     */
    public void updateFilter(String make) {
        selectedMake = make != null ? make : "";
        filteredIndices = rigNameList.getRigIndicesByMake(selectedMake);
        notifyDataSetChanged();
    }

    /**
     * Get the full rig list index for a position in the filtered list.
     * @param position Position in the filtered spinner
     * @return Index in the full rigList
     */
    public int getFullListIndex(int position) {
        if (position < 0 || position >= filteredIndices.size()) {
            return 0;
        }
        return filteredIndices.get(position);
    }

    /**
     * Find the position in the filtered list for a full rig list index.
     * @param fullListIndex Index in the full rigList
     * @return Position in the filtered list, or 0 if not found
     */
    public int getPositionForFullIndex(int fullListIndex) {
        for (int i = 0; i < filteredIndices.size(); i++) {
            if (filteredIndices.get(i) == fullListIndex) {
                return i;
            }
        }
        return 0;
    }

    /**
     * Get the RigName at the given position in the filtered list.
     * @param position Position in the filtered spinner
     * @return RigName object
     */
    public RigNameList.RigName getRigName(int position) {
        int fullIndex = getFullListIndex(position);
        return rigNameList.getRigNameByIndex(fullIndex);
    }

    @Override
    public int getCount() {
        return filteredIndices.size();
    }

    @Override
    public Object getItem(int i) {
        return getRigName(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @SuppressLint({"ViewHolder", "InflateParams", "UseCompatLoadingForDrawables"})
    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        LayoutInflater layoutInflater = LayoutInflater.from(mContext);
        view = layoutInflater.inflate(R.layout.rig_name_spinner_item, null);
        if (view != null) {
            TextView textView = view.findViewById(R.id.rigNameItemTextView);
            RigNameList.RigName rigName = getRigName(i);
            String displayName = rigNameList.getRigNameInfo(filteredIndices.get(i));
            
            // Show model name without make if make is selected
            if (!selectedMake.isEmpty() && !rigName.modelName.isEmpty()) {
                String modelOnly = RigNameList.extractModel(rigName.modelName);
                textView.setText(modelOnly);
            } else {
                textView.setText(displayName);
            }
            
            // Handle visibility for entries starting with "#"
            if (displayName.startsWith("#")) {
                view.setVisibility(View.GONE);
            }
            
            // Show logos for specific brands
            ImageView imageView = view.findViewById(R.id.rigLogoImageView);
            String modelNameUpper = displayName.toUpperCase();
            if (modelNameUpper.contains("GUOHE")) {
                imageView.setImageDrawable(mContext.getDrawable(R.drawable.guohe_logo));
                imageView.setVisibility(View.VISIBLE);
            } else if (modelNameUpper.contains("XIEGU")) {
                imageView.setImageDrawable(mContext.getDrawable(R.drawable.xiegulogo));
                imageView.setVisibility(View.VISIBLE);
            } else {
                imageView.setVisibility(View.GONE);
                imageView.setImageDrawable(null);
            }
        }
        return view;
    }
}
