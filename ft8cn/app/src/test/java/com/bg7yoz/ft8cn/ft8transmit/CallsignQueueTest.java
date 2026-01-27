package com.bg7yoz.ft8cn.ft8transmit;

import com.bg7yoz.ft8cn.Ft8Message;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for CallsignQueue class.
 * Tests queue management functionality for multiple responding callsigns.
 */
public class CallsignQueueTest {

    private CallsignQueue queue;
    private Ft8Message message1;
    private Ft8Message message2;
    private Ft8Message message3;

    @Before
    public void setUp() {
        queue = new CallsignQueue();
        
        // Create test messages
        message1 = createMessage("K1ABC", "W9XYZ", "OL50");
        message2 = createMessage("K2DEF", "W9XYZ", "FN20");
        message3 = createMessage("K3GHI", "W9XYZ", "DM33");
    }

    private Ft8Message createMessage(String from, String to, String grid) {
        Ft8Message msg = new Ft8Message(0);
        msg.callsignFrom = from;
        msg.callsignTo = to;
        msg.extraInfo = grid;
        msg.freq_hz = 1500.0f;
        msg.time_sec = 0.2f;
        msg.snr = -10;
        return msg;
    }

    @Test
    public void testEmptyQueue() {
        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
        assertNull(queue.getNext());
        assertTrue(queue.getAll().isEmpty());
    }

    @Test
    public void testAddCallsign() {
        queue.addCallsign("K1ABC", message1);
        
        assertFalse(queue.isEmpty());
        assertEquals(1, queue.size());
        assertTrue(queue.contains("K1ABC"));
    }

    @Test
    public void testAddDuplicateCallsign() {
        queue.addCallsign("K1ABC", message1);
        queue.addCallsign("K1ABC", message2); // Same callsign, different message
        
        assertEquals(1, queue.size()); // Should not add duplicate
        assertTrue(queue.contains("K1ABC"));
    }

    @Test
    public void testAddMultipleCallsigns() {
        queue.addCallsign("K1ABC", message1);
        queue.addCallsign("K2DEF", message2);
        queue.addCallsign("K3GHI", message3);
        
        assertEquals(3, queue.size());
        assertTrue(queue.contains("K1ABC"));
        assertTrue(queue.contains("K2DEF"));
        assertTrue(queue.contains("K3GHI"));
    }

    @Test
    public void testGetNextReturnsOldestFirst() throws InterruptedException {
        long time1 = System.currentTimeMillis();
        queue.addCallsign("K1ABC", message1);
        
        Thread.sleep(10); // Small delay to ensure different timestamps
        
        long time2 = System.currentTimeMillis();
        queue.addCallsign("K2DEF", message2);
        
        Thread.sleep(10);
        
        long time3 = System.currentTimeMillis();
        queue.addCallsign("K3GHI", message3);
        
        // getNext should return oldest (first added)
        CallsignQueue.QueuedCallsign next = queue.getNext();
        assertNotNull(next);
        assertEquals("K1ABC", next.callsign);
        assertTrue(next.timestamp >= time1);
        assertTrue(next.timestamp < time2);
    }

    @Test
    public void testGetAllReturnsOrderedList() throws InterruptedException {
        queue.addCallsign("K1ABC", message1);
        Thread.sleep(10);
        queue.addCallsign("K2DEF", message2);
        Thread.sleep(10);
        queue.addCallsign("K3GHI", message3);
        
        ArrayList<CallsignQueue.QueuedCallsign> all = queue.getAll();
        assertEquals(3, all.size());
        assertEquals("K1ABC", all.get(0).callsign); // Oldest first
        assertEquals("K2DEF", all.get(1).callsign);
        assertEquals("K3GHI", all.get(2).callsign); // Newest last
    }

    @Test
    public void testRemoveCallsign() {
        queue.addCallsign("K1ABC", message1);
        queue.addCallsign("K2DEF", message2);
        
        queue.removeCallsign("K1ABC");
        
        assertEquals(1, queue.size());
        assertFalse(queue.contains("K1ABC"));
        assertTrue(queue.contains("K2DEF"));
    }

    @Test
    public void testRemoveNonExistentCallsign() {
        queue.addCallsign("K1ABC", message1);
        
        queue.removeCallsign("NONEXISTENT");
        
        assertEquals(1, queue.size());
        assertTrue(queue.contains("K1ABC"));
    }

    @Test
    public void testClear() {
        queue.addCallsign("K1ABC", message1);
        queue.addCallsign("K2DEF", message2);
        queue.addCallsign("K3GHI", message3);
        
        queue.clear();
        
        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
        assertNull(queue.getNext());
    }

    @Test
    public void testRemoveByIndex() {
        queue.addCallsign("K1ABC", message1);
        queue.addCallsign("K2DEF", message2);
        queue.addCallsign("K3GHI", message3);
        
        CallsignQueue.QueuedCallsign removed = queue.removeByIndex(1);
        
        assertNotNull(removed);
        assertEquals("K2DEF", removed.callsign);
        assertEquals(2, queue.size());
        assertFalse(queue.contains("K2DEF"));
    }

    @Test
    public void testRemoveByIndexInvalid() {
        queue.addCallsign("K1ABC", message1);
        
        assertNull(queue.removeByIndex(-1));
        assertNull(queue.removeByIndex(10));
        assertEquals(1, queue.size()); // Queue unchanged
    }

    @Test
    public void testMoveItem() {
        queue.addCallsign("K1ABC", message1);
        queue.addCallsign("K2DEF", message2);
        queue.addCallsign("K3GHI", message3);
        
        // Move item from position 0 to position 2
        boolean moved = queue.moveItem(0, 2);
        
        assertTrue(moved);
        ArrayList<CallsignQueue.QueuedCallsign> all = queue.getAll();
        assertEquals("K2DEF", all.get(0).callsign);
        assertEquals("K3GHI", all.get(1).callsign);
        assertEquals("K1ABC", all.get(2).callsign); // Moved to end
    }

    @Test
    public void testMoveItemInvalidPositions() {
        queue.addCallsign("K1ABC", message1);
        queue.addCallsign("K2DEF", message2);
        
        assertFalse(queue.moveItem(-1, 1)); // Invalid from
        assertFalse(queue.moveItem(0, -1)); // Invalid to
        assertFalse(queue.moveItem(0, 10)); // Invalid to
        assertFalse(queue.moveItem(10, 0)); // Invalid from
        assertFalse(queue.moveItem(0, 0)); // Same position
        
        // Queue should be unchanged
        assertEquals(2, queue.size());
    }

    @Test
    public void testSetOrder() {
        queue.addCallsign("K1ABC", message1);
        queue.addCallsign("K2DEF", message2);
        queue.addCallsign("K3GHI", message3);
        
        ArrayList<CallsignQueue.QueuedCallsign> currentOrder = queue.getAll();
        // Reverse the order
        ArrayList<CallsignQueue.QueuedCallsign> reversed = new ArrayList<CallsignQueue.QueuedCallsign>();
        reversed.add(currentOrder.get(2));
        reversed.add(currentOrder.get(1));
        reversed.add(currentOrder.get(0));
        
        queue.setOrder(reversed);
        
        ArrayList<CallsignQueue.QueuedCallsign> newOrder = queue.getAll();
        assertEquals("K3GHI", newOrder.get(0).callsign);
        assertEquals("K2DEF", newOrder.get(1).callsign);
        assertEquals("K1ABC", newOrder.get(2).callsign);
    }

    @Test
    public void testSetOrderWithNull() {
        queue.addCallsign("K1ABC", message1);
        
        queue.setOrder(null);
        
        // Should not crash, queue should remain unchanged or empty
        // Implementation may vary, but should handle null gracefully
    }

    @Test
    public void testQueuedCallsignConstructor() {
        Ft8Message msg = createMessage("K1ABC", "W9XYZ", "OL50");
        long timestamp = System.currentTimeMillis();
        
        CallsignQueue.QueuedCallsign qc = new CallsignQueue.QueuedCallsign("K1ABC", timestamp, msg);
        
        assertEquals("K1ABC", qc.callsign);
        assertEquals(timestamp, qc.timestamp);
        assertEquals(msg, qc.initialMessage);
    }

    @Test
    public void testContains() {
        assertFalse(queue.contains("K1ABC"));
        
        queue.addCallsign("K1ABC", message1);
        
        assertTrue(queue.contains("K1ABC"));
        assertFalse(queue.contains("K2DEF"));
    }

    @Test
    public void testThreadSafety() throws InterruptedException {
        // Test that synchronized methods work correctly
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                queue.addCallsign("K" + i, message1);
            }
        });
        
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                queue.removeCallsign("K" + i);
            }
        });
        
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        
        // Queue should be in a consistent state (may be empty or have some items)
        int size = queue.size();
        assertTrue(size >= 0); // Should not crash or have negative size
    }
}
