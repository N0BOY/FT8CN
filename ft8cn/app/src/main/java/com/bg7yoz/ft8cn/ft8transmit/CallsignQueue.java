package com.bg7yoz.ft8cn.ft8transmit;

import com.bg7yoz.ft8cn.Ft8Message;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Queue for managing multiple callsigns responding to our callsign.
 * By default, responds to oldest first, then newest.
 * 
 * @author BGY70Z
 */
public class CallsignQueue {
    private static final String TAG = "CallsignQueue";
    
    /**
     * Represents a queued callsign with timestamp
     */
    public static class QueuedCallsign {
        public String callsign;
        public long timestamp; // When this callsign was added to queue
        public Ft8Message initialMessage; // The message that triggered the queue entry
        
        public QueuedCallsign(String callsign, long timestamp, Ft8Message initialMessage) {
            this.callsign = callsign;
            this.timestamp = timestamp;
            this.initialMessage = initialMessage;
        }
    }
    
    private ArrayList<QueuedCallsign> queue = new ArrayList<>();
    
    /**
     * Add a callsign to the queue if not already present
     * @param callsign The callsign to add
     * @param initialMessage The message that triggered this queue entry
     */
    public synchronized void addCallsign(String callsign, Ft8Message initialMessage) {
        // Check if already in queue
        for (QueuedCallsign qc : queue) {
            if (qc.callsign.equals(callsign)) {
                return; // Already in queue
            }
        }
        
        // Add to queue with current timestamp
        queue.add(new QueuedCallsign(callsign, System.currentTimeMillis(), initialMessage));
    }
    
    /**
     * Remove a callsign from the queue
     * @param callsign The callsign to remove
     */
    public synchronized void removeCallsign(String callsign) {
        Iterator<QueuedCallsign> iterator = queue.iterator();
        while (iterator.hasNext()) {
            QueuedCallsign qc = iterator.next();
            if (qc.callsign.equals(callsign)) {
                iterator.remove();
                break;
            }
        }
    }
    
    /**
     * Get the next callsign from the queue (oldest first, then newest)
     * @return The next queued callsign, or null if queue is empty
     */
    public synchronized QueuedCallsign getNext() {
        if (queue.isEmpty()) {
            return null;
        }
        
        // Sort by timestamp: oldest first
        queue.sort((a, b) -> Long.compare(a.timestamp, b.timestamp));
        
        // Return the oldest (first) entry
        return queue.get(0);
    }
    
    /**
     * Get all queued callsigns in order (oldest first, then newest)
     * @return List of queued callsigns
     */
    public synchronized ArrayList<QueuedCallsign> getAll() {
        // Sort by timestamp: oldest first
        queue.sort((a, b) -> Long.compare(a.timestamp, b.timestamp));
        return new ArrayList<>(queue);
    }
    
    /**
     * Check if a callsign is in the queue
     * @param callsign The callsign to check
     * @return true if in queue
     */
    public synchronized boolean contains(String callsign) {
        for (QueuedCallsign qc : queue) {
            if (qc.callsign.equals(callsign)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Clear the queue
     */
    public synchronized void clear() {
        queue.clear();
    }
    
    /**
     * Get queue size
     * @return Number of callsigns in queue
     */
    public synchronized int size() {
        return queue.size();
    }
    
    /**
     * Check if queue is empty
     * @return true if queue is empty
     */
    public synchronized boolean isEmpty() {
        return queue.isEmpty();
    }
    
    /**
     * Remove a callsign from the queue by index
     * @param index The index to remove
     * @return The removed QueuedCallsign, or null if index is invalid
     */
    public synchronized QueuedCallsign removeByIndex(int index) {
        if (index >= 0 && index < queue.size()) {
            return queue.remove(index);
        }
        return null;
    }
    
    /**
     * Move a callsign from one position to another in the queue
     * @param fromPosition The source position
     * @param toPosition The destination position
     * @return true if the move was successful
     */
    public synchronized boolean moveItem(int fromPosition, int toPosition) {
        if (fromPosition < 0 || fromPosition >= queue.size() ||
            toPosition < 0 || toPosition >= queue.size() ||
            fromPosition == toPosition) {
            return false;
        }
        
        QueuedCallsign item = queue.remove(fromPosition);
        queue.add(toPosition, item);
        return true;
    }
    
    /**
     * Set the queue order directly (used after manual reordering)
     * @param newOrder The new ordered list
     */
    public synchronized void setOrder(ArrayList<QueuedCallsign> newOrder) {
        if (newOrder != null) {
            queue = new ArrayList<>(newOrder);
        }
    }
}
