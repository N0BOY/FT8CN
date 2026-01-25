package com.bg7yoz.ft8cn.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bg7yoz.ft8cn.R;
import com.bg7yoz.ft8cn.ft8transmit.CallsignQueue;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying the callsign queue
 */
public class CallsignQueueAdapter extends RecyclerView.Adapter<CallsignQueueAdapter.QueueViewHolder> {
    private List<CallsignQueue.QueuedCallsign> queue = new ArrayList<>();
    private OnQueueItemActionListener listener;

    /**
     * Interface for handling queue item actions (delete, reorder)
     */
    public interface OnQueueItemActionListener {
        void onItemDeleted(int position, CallsignQueue.QueuedCallsign item);
        void onItemMoved(int fromPosition, int toPosition);
    }

    public void setOnQueueItemActionListener(OnQueueItemActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public QueueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.callsign_queue_item, parent, false);
        return new QueueViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QueueViewHolder holder, int position) {
        if (position < 0 || position >= queue.size()) {
            return;
        }
        CallsignQueue.QueuedCallsign item = queue.get(position);
        holder.callsignText.setText(item.callsign);
        
        // Show position in queue (1-based)
        holder.positionText.setText(String.valueOf(position + 1));
    }

    @Override
    public int getItemCount() {
        return queue.size();
    }

    public void updateQueue(List<CallsignQueue.QueuedCallsign> newQueue) {
        this.queue = newQueue != null ? new ArrayList<>(newQueue) : new ArrayList<>();
        notifyDataSetChanged();
    }

    /**
     * Remove an item from the adapter and notify
     */
    public void removeItem(int position) {
        if (position >= 0 && position < queue.size()) {
            CallsignQueue.QueuedCallsign item = queue.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, queue.size());
            if (listener != null) {
                listener.onItemDeleted(position, item);
            }
        }
    }

    /**
     * Move an item in the adapter and notify
     */
    public void moveItem(int fromPosition, int toPosition) {
        if (fromPosition >= 0 && fromPosition < queue.size() &&
            toPosition >= 0 && toPosition < queue.size() &&
            fromPosition != toPosition) {
            CallsignQueue.QueuedCallsign item = queue.remove(fromPosition);
            queue.add(toPosition, item);
            notifyItemMoved(fromPosition, toPosition);
            notifyItemRangeChanged(Math.min(fromPosition, toPosition), 
                    Math.abs(fromPosition - toPosition) + 1);
            if (listener != null) {
                listener.onItemMoved(fromPosition, toPosition);
            }
        }
    }

    /**
     * Get the item at a specific position
     */
    public CallsignQueue.QueuedCallsign getItem(int position) {
        if (position >= 0 && position < queue.size()) {
            return queue.get(position);
        }
        return null;
    }

    /**
     * Get the current queue order
     */
    public List<CallsignQueue.QueuedCallsign> getQueue() {
        return new ArrayList<>(queue);
    }

    static class QueueViewHolder extends RecyclerView.ViewHolder {
        TextView callsignText;
        TextView positionText;

        QueueViewHolder(@NonNull View itemView) {
            super(itemView);
            callsignText = itemView.findViewById(R.id.queueCallsignText);
            positionText = itemView.findViewById(R.id.queuePositionText);
        }
    }
}
