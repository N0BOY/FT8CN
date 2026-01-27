package com.bg7yoz.ft8cn.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bg7yoz.ft8cn.GeneralVariables;
import com.bg7yoz.ft8cn.MainViewModel;
import com.bg7yoz.ft8cn.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying and managing the follow callsign list
 */
public class FollowCallsignAdapter extends RecyclerView.Adapter<FollowCallsignAdapter.FollowCallsignViewHolder> {
    private List<String> followCallsigns = new ArrayList<>();
    private MainViewModel mainViewModel;
    private Runnable onListUpdateListener;

    public FollowCallsignAdapter(MainViewModel mainViewModel) {
        this.mainViewModel = mainViewModel;
    }

    @NonNull
    @Override
    public FollowCallsignViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.follow_callsign_item, parent, false);
        return new FollowCallsignViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FollowCallsignViewHolder holder, int position) {
        String callsign = followCallsigns.get(position);
        holder.callsignText.setText(callsign);
        holder.removeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Remove from list
                GeneralVariables.followCallsign.remove(callsign);
                // Remove from database
                mainViewModel.databaseOpr.deleteFollowCallsign(callsign);
                // Update the adapter
                updateList();
                // Notify listener if set
                if (onListUpdateListener != null) {
                    onListUpdateListener.run();
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return followCallsigns.size();
    }

    public void updateList() {
        followCallsigns.clear();
        followCallsigns.addAll(GeneralVariables.followCallsign);
        notifyDataSetChanged();
    }

    public void setOnListUpdateListener(Runnable listener) {
        this.onListUpdateListener = listener;
    }

    static class FollowCallsignViewHolder extends RecyclerView.ViewHolder {
        TextView callsignText;
        Button removeButton;

        FollowCallsignViewHolder(@NonNull View itemView) {
            super(itemView);
            callsignText = itemView.findViewById(R.id.followCallsignText);
            removeButton = itemView.findViewById(R.id.removeFollowButton);
        }
    }
}
